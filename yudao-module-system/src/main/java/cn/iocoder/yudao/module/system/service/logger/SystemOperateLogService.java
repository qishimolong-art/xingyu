package cn.iocoder.yudao.module.system.service.logger;

import cn.iocoder.yudao.framework.common.biz.system.logger.OperateLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.logger.OperateLogContentUtils;
import cn.iocoder.yudao.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * System 模块业务操作日志写入服务。
 */
@Service
@Slf4j
public class SystemOperateLogService {

    @Resource
    private OperateLogCommonApi operateLogApi;

    public void record(String type, String subType, Long bizId, String action, Object extra) {
        if (bizId == null) {
            return;
        }
        String normalizedType = normalizeType(type);
        String normalizedSubType = normalizeSubType(subType);
        String normalizedAction = OperateLogContentUtils.buildErpFormAction(normalizedSubType, null, bizId, null, action);
        OperateLogCreateReqDTO reqDTO = new OperateLogCreateReqDTO();
        try {
            reqDTO.setTraceId(TracerUtils.getTraceId());
            fillUserFields(reqDTO);
            reqDTO.setType(normalizedType);
            reqDTO.setSubType(normalizedSubType);
            reqDTO.setBizId(bizId);
            reqDTO.setAction(normalizedAction);
            reqDTO.setExtra(JsonUtils.toJsonString(extra == null ? Collections.emptyMap() : extra));
            fillRequestFields(reqDTO);
            operateLogApi.createOperateLogAsync(reqDTO);
        } catch (Throwable ex) {
            log.error("[record][type({}) subType({}) bizId({}) action({}) error]",
                    normalizedType, normalizedSubType, bizId, normalizedAction, ex);
        }
    }

    public void recordCreate(String type, String subType, Long bizId, Object newObj) {
        record(type, subType, bizId, OperateLogContentUtils.buildErpFormCreateAction(newObj, bizId, null),
                buildIdentityExtra(newObj, bizId));
    }

    public void recordUpdate(String type, String subType, Long bizId, Object oldObj, Object newObj) {
        record(type, subType, bizId, OperateLogContentUtils.buildErpFormUpdateAction(oldObj, newObj, bizId, null),
                buildIdentityExtra(newObj != null ? newObj : oldObj, bizId));
    }

    public void recordDelete(String type, String subType, Long bizId, Object oldObj) {
        record(type, subType, bizId, OperateLogContentUtils.buildErpFormDeleteAction(oldObj, bizId, null),
                buildIdentityExtra(oldObj, bizId));
    }

    public void recordBatch(String type, String subType, String action, Iterable<Long> ids) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("ids", ids);
        record(type, subType, 0L, action, extra);
    }

    private Map<String, Object> buildIdentityExtra(Object obj, Long bizId) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("id", bizId);
        extra.put("identity", OperateLogContentUtils.buildIdentity(obj, bizId, null));
        return extra;
    }

    private static String normalizeType(String type) {
        if (type == null) {
            return null;
        }
        return type.replaceFirst("^(ERP|SYSTEM)\\s+", "").trim();
    }

    private static String normalizeSubType(String subType) {
        if (subType != null && subType.contains("删除")) {
            return "删除";
        }
        if (subType != null && (subType.contains("创建") || subType.contains("新增"))) {
            return "新增";
        }
        return "修改";
    }

    private static void fillUserFields(OperateLogCreateReqDTO reqDTO) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return;
        }
        reqDTO.setUserId(loginUser.getId());
        reqDTO.setUserType(loginUser.getUserType());
    }

    private static void fillRequestFields(OperateLogCreateReqDTO reqDTO) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return;
        }
        reqDTO.setRequestMethod(request.getMethod());
        reqDTO.setRequestUrl(request.getRequestURI());
        reqDTO.setUserIp(ServletUtils.getClientIP(request));
        reqDTO.setUserAgent(ServletUtils.getUserAgent(request));
    }

}
