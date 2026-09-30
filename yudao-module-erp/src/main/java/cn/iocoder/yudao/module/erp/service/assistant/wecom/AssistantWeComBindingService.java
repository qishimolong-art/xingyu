package cn.iocoder.yudao.module.erp.service.assistant.wecom;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.assistant.ErpAssistantWeComBindingDO;
import cn.iocoder.yudao.module.erp.dal.mysql.assistant.ErpAssistantWeComBindingMapper;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantFailure;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.framework.wecom.config.WeComProperties;
import cn.iocoder.yudao.module.system.service.wecom.WeComClientService;
import cn.iocoder.yudao.module.system.service.wecom.WeComUserIdentity;
import lombok.Data;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class AssistantWeComBindingService {

    private static final String BIND_BRIDGE_FILE = "wecom-bot-bind.html";
    private static final String BIND_BRIDGE_PATH = "/" + BIND_BRIDGE_FILE;
    private static final String TICKET_KEY = "erp:assistant:wecom:bind-ticket:%s";
    private static final String STATE_KEY = "erp:assistant:wecom:bind-state:%s";
    private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value=redis.call('get',KEYS[1]); if value then redis.call('del',KEYS[1]); end; return value",
            String.class);

    @Resource private WeComProperties properties;
    @Resource private WeComClientService weComClientService;
    @Resource private AdminUserApi adminUserApi;
    @Resource private ErpAssistantWeComBindingMapper bindingMapper;
    @Resource(name = "stringRedisTemplate") private StringRedisTemplate redisTemplate;
    @Resource private ApplicationEventPublisher eventPublisher;

    public String createTicket(String wecomUserId, String question) {
        WeComProperties.AiBotProperties bot = requiredBotConfig();
        if (StrUtil.isBlank(wecomUserId) || StrUtil.isBlank(question)) {
            throw new AssistantFailure("INVALID_REQUEST", "企业微信用户或问题不能为空");
        }
        BindTicket value = new BindTicket();
        value.setTenantId(bot.getTenantId());
        value.setBotId(bot.getBotId());
        value.setWecomUserId(wecomUserId);
        value.setQuestion(question);
        String ticket = randomToken();
        Duration timeout = defaultDuration(bot.getTicketTimeout(), Duration.ofMinutes(5));
        redisTemplate.opsForValue().set(String.format(TICKET_KEY, ticket), JsonUtils.toJsonString(value),
                timeout.toMillis(), TimeUnit.MILLISECONDS);
        return ticket;
    }

    public String getAuthorizeUrl(String ticket) {
        BindTicket bindTicket = readTicket(ticket);
        String state = randomToken();
        BindState stateValue = new BindState();
        stateValue.setTicket(ticket);
        stateValue.setTenantId(bindTicket.getTenantId());
        Duration timeout = defaultDuration(requiredBotConfig().getTicketTimeout(), Duration.ofMinutes(5));
        redisTemplate.opsForValue().set(String.format(STATE_KEY, state), JsonUtils.toJsonString(stateValue),
                timeout.toMillis(), TimeUnit.MILLISECONDS);
        String redirectUri = getBindEntryUrl(ticket, bindTicket.getTenantId());
        return weComClientService.getAuthorizeUrl(redirectUri, state, emptyToNull(requiredBotConfig().getLoginClientKey()));
    }

    /**
     * 生成企微可校验的无 hash 绑定入口。兼容历史上误配的 /#/auth/wecom-bot-bind 地址，
     * 避免 fragment 被作为 OAuth redirect_uri 后触发“域名无效”。
     */
    public String getBindEntryUrl(String ticket, Long tenantId) {
        String configuredUrl = StrUtil.trim(requiredBotConfig().getBindPageUrl());
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(configuredUrl);
        String fragment = builder.build().getFragment();
        if (StrUtil.isNotBlank(fragment)) {
            String basePath = builder.build().getPath();
            String bridgePath;
            if (StrUtil.isBlank(basePath) || "/".equals(basePath)) {
                bridgePath = BIND_BRIDGE_PATH;
            } else {
                bridgePath = StrUtil.endWith(basePath, "/")
                        ? basePath + BIND_BRIDGE_FILE : basePath + "/" + BIND_BRIDGE_FILE;
            }
            builder.replacePath(bridgePath).fragment(null);
        }
        return builder.replaceQuery(null)
                .queryParam("ticket", ticket).queryParam("tenantId", tenantId)
                .build().encode().toUriString();
    }

    @Transactional(rollbackFor = Exception.class)
    public BindResult bind(String code, String state) {
        if (StrUtil.hasBlank(code, state)) throw new AssistantFailure("INVALID_REQUEST", "免登参数不完整");
        String stateKey = String.format(STATE_KEY, state);
        String rawState = consume(stateKey);
        BindState bindState = StrUtil.isBlank(rawState) ? null : JsonUtils.parseObject(rawState, BindState.class);
        if (bindState == null || StrUtil.isBlank(bindState.getTicket())) {
            throw new AssistantFailure("INVALID_STATE", "免登状态已失效，请返回机器人重新发起绑定");
        }
        String ticketKey = String.format(TICKET_KEY, bindState.getTicket());
        String rawTicket = consume(ticketKey);
        BindTicket ticket = StrUtil.isBlank(rawTicket) ? null : JsonUtils.parseObject(rawTicket, BindTicket.class);
        WeComProperties.AiBotProperties bot = requiredBotConfig();
        if (ticket == null || !bot.getTenantId().equals(ticket.getTenantId())
                || !bot.getTenantId().equals(bindState.getTenantId()) || !bot.getBotId().equals(ticket.getBotId())) {
            throw new AssistantFailure("INVALID_TICKET", "绑定链接已失效，请返回机器人重新发起绑定");
        }
        WeComUserIdentity identity = weComClientService.getUserIdentityByCode(code, emptyToNull(bot.getLoginClientKey()));
        if (!StrUtil.equals(identity.getUserId(), ticket.getWecomUserId())) {
            throw new AssistantFailure("IDENTITY_MISMATCH", "当前免登用户与提问用户不一致，禁止绑定");
        }
        AtomicReference<BindResult> result = new AtomicReference<>();
        TenantUtils.execute(ticket.getTenantId(), () -> result.set(bindInTenant(ticket, identity)));
        eventPublisher.publishEvent(new AssistantWeComBoundEvent(ticket.getTenantId(), result.get().getUserId(),
                ticket.getWecomUserId(), ticket.getQuestion()));
        return result.get();
    }

    protected BindResult bindInTenant(BindTicket ticket, WeComUserIdentity identity) {
        List<AdminUserRespDTO> users = adminUserApi.getUserListByMobile(StrUtil.trim(identity.getMobile()));
        if (users == null || users.isEmpty()) throw new AssistantFailure("USER_NOT_FOUND", "手机号未匹配到 ERP 用户");
        List<AdminUserRespDTO> enabledUsers = users.stream()
                .filter(user -> CommonStatusEnum.isEnable(user.getStatus())).collect(Collectors.toList());
        if (enabledUsers.isEmpty()) throw new AssistantFailure("USER_DISABLED", "ERP 用户已停用");
        if (enabledUsers.size() != 1) throw new AssistantFailure("USER_DUPLICATE", "手机号匹配到多个启用的 ERP 用户，请联系管理员");
        AdminUserRespDTO user = enabledUsers.get(0);

        ErpAssistantWeComBindingDO byWeCom = bindingMapper.selectByBotAndWeComUser(ticket.getBotId(), identity.getUserId());
        if (byWeCom != null && !user.getId().equals(byWeCom.getUserId())) {
            throw new AssistantFailure("BINDING_CONFLICT", "该企业微信账号已绑定其他 ERP 用户，请联系管理员");
        }
        LocalDateTime now = LocalDateTime.now();
        if (byWeCom != null) {
            byWeCom.setMobile(identity.getMobile()).setLastActiveTime(now);
            bindingMapper.updateById(byWeCom);
            return new BindResult(user.getId(), user.getNickname(), true);
        }
        ErpAssistantWeComBindingDO byUser = bindingMapper.selectByBotAndUser(ticket.getBotId(), user.getId());
        if (byUser != null) {
            byUser.setWecomUserId(identity.getUserId()).setMobile(identity.getMobile())
                    .setBindTime(now).setLastActiveTime(now);
            bindingMapper.updateById(byUser);
            return new BindResult(user.getId(), user.getNickname(), true);
        }
        ErpAssistantWeComBindingDO binding = new ErpAssistantWeComBindingDO()
                .setCorpId(properties.getCorpId()).setBotId(ticket.getBotId())
                .setWecomUserId(identity.getUserId()).setUserId(user.getId()).setMobile(identity.getMobile())
                .setBindTime(now).setLastActiveTime(now);
        try {
            bindingMapper.insert(binding);
        } catch (DuplicateKeyException ex) {
            throw new AssistantFailure("BINDING_CONFLICT", "绑定关系发生冲突，请联系管理员");
        }
        return new BindResult(user.getId(), user.getNickname(), false);
    }

    public ErpAssistantWeComBindingDO getBinding(String wecomUserId) {
        return bindingMapper.selectByBotAndWeComUser(requiredBotConfig().getBotId(), wecomUserId);
    }

    public void updateConversation(Long bindingId, String conversationId) {
        ErpAssistantWeComBindingDO update = new ErpAssistantWeComBindingDO().setId(bindingId)
                .setConversationId(conversationId).setLastActiveTime(LocalDateTime.now());
        bindingMapper.updateById(update);
    }

    private BindTicket readTicket(String ticket) {
        if (StrUtil.isBlank(ticket)) throw new AssistantFailure("INVALID_TICKET", "绑定链接无效");
        String raw = redisTemplate.opsForValue().get(String.format(TICKET_KEY, ticket));
        BindTicket value = StrUtil.isBlank(raw) ? null : JsonUtils.parseObject(raw, BindTicket.class);
        if (value == null) throw new AssistantFailure("INVALID_TICKET", "绑定链接已过期，请返回机器人重新提问");
        return value;
    }

    private WeComProperties.AiBotProperties requiredBotConfig() {
        WeComProperties.AiBotProperties bot = properties.getAiBot();
        if (bot == null || !bot.isEnabled() || StrUtil.hasBlank(bot.getBotId(), bot.getBindPageUrl()) || bot.getTenantId() == null) {
            throw new AssistantFailure("BOT_NOT_CONFIGURED", "企业微信问数机器人尚未完成配置");
        }
        return bot;
    }

    private static String randomToken() { return SecureUtil.sha256(UUID.randomUUID().toString()); }
    private String consume(String key) {
        return redisTemplate.execute(CONSUME_SCRIPT, java.util.Collections.singletonList(key));
    }
    private static Duration defaultDuration(Duration value, Duration fallback) { return value == null ? fallback : value; }
    private static String emptyToNull(String value) { return StrUtil.isBlank(value) ? null : value.trim(); }

    @Data private static class BindTicket { private Long tenantId; private String botId; private String wecomUserId; private String question; }
    @Data private static class BindState { private String ticket; private Long tenantId; }
    @Data
    public static class BindResult {
        private final Long userId;
        private final String nickname;
        private final boolean existing;
    }
}
