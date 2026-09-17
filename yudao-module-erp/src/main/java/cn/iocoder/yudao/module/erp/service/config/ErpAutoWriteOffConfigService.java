package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpAutoWriteOffConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpAutoWriteOffConfigUpdateReqVO;

import javax.validation.Valid;

public interface ErpAutoWriteOffConfigService {

    ErpAutoWriteOffConfigRespVO getConfig();

    void updateConfig(@Valid ErpAutoWriteOffConfigUpdateReqVO reqVO);

    boolean isAutoWriteOffDisabled(Long deptId);

}
