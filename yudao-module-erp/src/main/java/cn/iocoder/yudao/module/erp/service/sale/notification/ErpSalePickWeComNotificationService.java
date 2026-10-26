package cn.iocoder.yudao.module.erp.service.sale.notification;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryPickTaskDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliveryPickTaskMapper;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.framework.wecom.config.ErpSalePickWeComNotificationProperties;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.wecom.WeComClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 拣货任务企业微信通知服务。
 */
@Service
@Slf4j
public class ErpSalePickWeComNotificationService {

    @Resource
    private ErpSalePickWeComNotificationProperties properties;
    @Resource
    private ErpSalePickDeliveryPickTaskMapper pickTaskMapper;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private WeComClientService weComClientService;

    /**
     * 向任务仓库的全部有效拣货员发送任务卡片。
     */
    public void sendTaskCreatedNotification(Long tenantId, Long taskId) {
        if (!properties.isEnabled()) {
            return;
        }
        if (tenantId == null || taskId == null) {
            log.warn("[sendTaskCreatedNotification][tenantId({}) taskId({}) 参数不完整]", tenantId, taskId);
            return;
        }
        if (StrUtil.isBlank(properties.getEntryUrl())) {
            log.error("[sendTaskCreatedNotification][taskId({}) 未配置企微拣货入口地址]", taskId);
            return;
        }
        TenantUtils.execute(tenantId, () -> sendWithinTenant(tenantId, taskId));
    }

    private void sendWithinTenant(Long tenantId, Long taskId) {
        ErpSalePickDeliveryPickTaskDO task = pickTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("[sendWithinTenant][tenantId({}) taskId({}) 拣货任务不存在]", tenantId, taskId);
            return;
        }
        List<Long> pickerUserIds = warehouseService.getWarehousePickerUserIds(task.getWarehouseId());
        if (pickerUserIds == null || pickerUserIds.isEmpty()) {
            log.info("[sendWithinTenant][taskId({}) warehouseId({}) 未配置拣货员，跳过企微通知]",
                    taskId, task.getWarehouseId());
            return;
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(pickerUserIds);
        String entryUrl = buildEntryUrl(tenantId, taskId);
        String description = buildDescription(task);
        Set<String> notifiedWeComUserIds = new HashSet<>();
        for (Long pickerUserId : pickerUserIds) {
            AdminUserRespDTO user = userMap.get(pickerUserId);
            if (user == null || !CommonStatusEnum.isEnable(user.getStatus()) || StrUtil.isBlank(user.getMobile())) {
                log.warn("[sendWithinTenant][taskId({}) pickerUserId({}) ERP 用户无效、已停用或未配置手机号]",
                        taskId, pickerUserId);
                continue;
            }
            try {
                String weComUserId = weComClientService.getUserIdByMobile(
                        normalizeMobile(user.getMobile()), properties.getClientKey());
                if (!notifiedWeComUserIds.add(weComUserId)) {
                    continue;
                }
                weComClientService.sendTextCardMessage(weComUserId, "新的拣货任务", description, entryUrl,
                        "进入拣货", properties.getClientKey());
            } catch (Exception exception) {
                // 通知属于事务提交后的附加动作，单个成员失败不能影响其它成员或业务单据。
                log.error("[sendWithinTenant][taskId({}) pickerUserId({}) 企业微信通知失败：{}]",
                        taskId, pickerUserId, exception.getMessage());
            }
        }
    }

    private String buildEntryUrl(Long tenantId, Long taskId) {
        return UriComponentsBuilder.fromHttpUrl(properties.getEntryUrl())
                .queryParam("tenantId", tenantId)
                .queryParam("workbench", "sale-pick")
                .queryParam("taskId", taskId)
                .build()
                .encode()
                .toUriString();
    }

    private String buildDescription(ErpSalePickDeliveryPickTaskDO task) {
        String sourceName = ErpSaleBizSourceTypeEnum.getPickSourceName(task.getSourceType());
        String displayNo = StrUtil.blankToDefault(task.getSourceNo(), task.getSaleOutNo());
        String customerName = StrUtil.blankToDefault(task.getCustomerName(), "-");
        String warehouseName = StrUtil.blankToDefault(task.getWarehouseName(), "-");
        return String.format("<div class=\"gray\">%s · %s</div>"
                        + "<div class=\"normal\">仓库：%s</div>"
                        + "<div class=\"normal\">往来单位：%s</div>"
                        + "<div class=\"normal\">明细：%s 种</div>",
                escape(sourceName), escape(StrUtil.blankToDefault(displayNo, "-")), escape(warehouseName),
                escape(customerName), task.getTotalItemCount() == null ? 0 : task.getTotalItemCount());
    }

    private String normalizeMobile(String mobile) {
        String normalized = StrUtil.removeAll(mobile, ' ', '-');
        return StrUtil.startWith(normalized, "+86") ? StrUtil.removePrefix(normalized, "+86") : normalized;
    }

    private String escape(String text) {
        return HtmlUtils.htmlEscape(StrUtil.nullToDefault(text, ""));
    }

}
