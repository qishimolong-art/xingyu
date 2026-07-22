package cn.iocoder.yudao.module.erp.service.common;

import cn.hutool.core.util.StrUtil;
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

import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.*;

/**
 * Minimal ERP business operation log writer.
 */
@Service
@Slf4j
public class ErpOperateLogService {

    private static final ThreadLocal<Integer> SIMPLE_RECORD_SUPPRESS = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> RECORD_CAPTURED = new ThreadLocal<>();

    @Resource
    private OperateLogCommonApi operateLogApi;

    public static void beginSuppressSimpleRecord() {
        Integer depth = SIMPLE_RECORD_SUPPRESS.get();
        SIMPLE_RECORD_SUPPRESS.set(depth == null ? 1 : depth + 1);
    }

    public static void endSuppressSimpleRecord() {
        Integer depth = SIMPLE_RECORD_SUPPRESS.get();
        if (depth == null || depth <= 1) {
            SIMPLE_RECORD_SUPPRESS.remove();
            return;
        }
        SIMPLE_RECORD_SUPPRESS.set(depth - 1);
    }

    public static void beginCaptureRecord() {
        RECORD_CAPTURED.set(false);
    }

    public static boolean hasCapturedRecord() {
        return Boolean.TRUE.equals(RECORD_CAPTURED.get());
    }

    public static void endCaptureRecord() {
        RECORD_CAPTURED.remove();
    }

    private static boolean isSuppressSimpleRecord() {
        return SIMPLE_RECORD_SUPPRESS.get() != null;
    }

    public void record(String type, String subType, Long bizId, String action, String no) {
        if (bizId == null) {
            return;
        }
        RECORD_CAPTURED.set(true);
        String normalizedType = normalizeType(type);
        String normalizedSubType = normalizeSubType(subType);
        String normalizedAction = OperateLogContentUtils.buildErpFormAction(normalizedSubType, null, bizId, no, action);
        OperateLogCreateReqDTO reqDTO = new OperateLogCreateReqDTO();
        try {
            reqDTO.setTraceId(TracerUtils.getTraceId());
            fillUserFields(reqDTO);
            fillModuleFields(reqDTO, normalizedType, normalizedSubType, bizId, normalizedAction, no);
            fillRequestFields(reqDTO);
            operateLogApi.createOperateLogAsync(reqDTO);
        } catch (Throwable ex) {
            log.error("[record][type({}) subType({}) bizId({}) action({}) error]",
                    normalizedType, normalizedSubType, bizId, normalizedAction, ex);
        }
    }

    public void recordCreate(String type, Long bizId, String no) {
        if (isSuppressSimpleRecord()) {
            return;
        }
        record(type, ERP_CREATE_SUB_TYPE, bizId,
                OperateLogContentUtils.buildErpFormCreateAction(null, bizId, no), no);
    }

    public void recordCreate(String type, Long bizId, Object newObj, String no) {
        record(type, ERP_CREATE_SUB_TYPE, bizId,
                OperateLogContentUtils.buildErpFormCreateAction(newObj, bizId, no), no);
    }

    public void recordUpdate(String type, Long bizId, String no) {
        if (isSuppressSimpleRecord()) {
            return;
        }
        record(type, ERP_UPDATE_SUB_TYPE, bizId,
                OperateLogContentUtils.buildErpFormUpdateAction(null, null, bizId, no), no);
    }

    public void recordUpdate(String type, Long bizId, Object oldObj, Object newObj, String no) {
        record(type, ERP_UPDATE_SUB_TYPE, bizId,
                OperateLogContentUtils.buildErpFormUpdateAction(oldObj, newObj, bizId, no), no);
    }

    public void recordDelete(String type, Long bizId, String no) {
        if (isSuppressSimpleRecord()) {
            return;
        }
        record(type, ERP_DELETE_SUB_TYPE, bizId,
                OperateLogContentUtils.buildErpFormDeleteAction(null, bizId, no), no);
    }

    public void recordDelete(String type, Long bizId, Object oldObj, String no) {
        record(type, ERP_DELETE_SUB_TYPE, bizId,
                OperateLogContentUtils.buildErpFormDeleteAction(oldObj, bizId, no), no);
    }

    public void recordStatus(String type, Long bizId, String no, boolean approve) {
        if (isSuppressSimpleRecord()) {
            return;
        }
        record(type, ERP_UPDATE_SUB_TYPE, bizId,
                OperateLogContentUtils.buildErpFormUpdateAction(null, null, bizId, no), no);
    }

    public void recordStatus(String type, String actionName, Long bizId, String no) {
        if (isSuppressSimpleRecord()) {
            return;
        }
        record(type, actionName, bizId,
                OperateLogContentUtils.buildErpFormAction(actionName, null, bizId, no, actionName), no);
    }

    public void recordStatus(String type, Long bizId, Object oldObj, Object newObj, String no, boolean approve) {
        record(type, ERP_UPDATE_SUB_TYPE, bizId,
                OperateLogContentUtils.buildErpFormUpdateAction(oldObj, newObj, bizId, no), no);
    }

    private static String normalizeType(String type) {
        if (type == null) {
            return null;
        }
        return type.replaceFirst("^(ERP|SYSTEM)\\s+", "").trim();
    }

    private static String normalizeSubType(String subType) {
        if (ERP_CREATE_SUB_TYPE.equals(subType) || "创建".equals(subType) || "新增".equals(subType)) {
            return ERP_CREATE_SUB_TYPE;
        }
        if (ERP_DELETE_SUB_TYPE.equals(subType) || "删除".equals(subType)) {
            return ERP_DELETE_SUB_TYPE;
        }
        if (StrUtil.isNotBlank(subType) && !ERP_UPDATE_SUB_TYPE.equals(subType) && !"更新".equals(subType)) {
            return subType;
        }
        return ERP_UPDATE_SUB_TYPE;
    }

    private static void fillUserFields(OperateLogCreateReqDTO reqDTO) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return;
        }
        reqDTO.setUserId(loginUser.getId());
        reqDTO.setUserType(loginUser.getUserType());
    }

    private static void fillModuleFields(OperateLogCreateReqDTO reqDTO, String type, String subType,
                                         Long bizId, String action, String no) {
        reqDTO.setType(type);
        reqDTO.setSubType(subType);
        reqDTO.setBizId(bizId);
        reqDTO.setAction(action);
        reqDTO.setExtra(JsonUtils.toJsonString(Collections.singletonMap("no", no)));
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
