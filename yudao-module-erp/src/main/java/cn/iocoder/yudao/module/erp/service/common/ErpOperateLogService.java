package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.common.biz.system.logger.OperateLogCommonApi;
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

    @Resource
    private OperateLogCommonApi operateLogApi;

    public void record(String type, String subType, Long bizId, String action, String no) {
        if (bizId == null) {
            return;
        }
        OperateLogCreateReqDTO reqDTO = new OperateLogCreateReqDTO();
        try {
            reqDTO.setTraceId(TracerUtils.getTraceId());
            fillUserFields(reqDTO);
            fillModuleFields(reqDTO, type, subType, bizId, action, no);
            fillRequestFields(reqDTO);
            operateLogApi.createOperateLogAsync(reqDTO);
        } catch (Throwable ex) {
            log.error("[record][type({}) subType({}) bizId({}) action({}) error]", type, subType, bizId, action, ex);
        }
    }

    public void recordCreate(String type, Long bizId, String no) {
        record(type, ERP_CREATE_SUB_TYPE, bizId, "创建单据，单据编号：" + no, no);
    }

    public void recordUpdate(String type, Long bizId, String no) {
        record(type, ERP_UPDATE_SUB_TYPE, bizId, "更新单据，单据编号：" + no, no);
    }

    public void recordDelete(String type, Long bizId, String no) {
        record(type, ERP_DELETE_SUB_TYPE, bizId, "删除单据，单据编号：" + no, no);
    }

    public void recordStatus(String type, Long bizId, String no, boolean approve) {
        record(type, approve ? ERP_APPROVE_SUB_TYPE : ERP_PROCESS_SUB_TYPE, bizId,
                (approve ? "审核单据，单据编号：" : "反审核单据，单据编号：") + no, no);
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
