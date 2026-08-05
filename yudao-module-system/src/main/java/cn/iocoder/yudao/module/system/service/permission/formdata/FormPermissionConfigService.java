package cn.iocoder.yudao.module.system.service.permission.formdata;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionCandidateColumnRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionCandidateTableRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionConfigRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionConfigSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata.FormPermissionPageReqVO;

import java.util.List;

public interface FormPermissionConfigService {

    PageResult<FormPermissionConfigRespVO> getConfigPage(FormPermissionPageReqVO pageReqVO);

    List<FormPermissionCandidateTableRespVO> getCandidateTables();

    List<FormPermissionCandidateColumnRespVO> getCandidateColumns(String tableName);

    FormPermissionConfigRespVO getConfig(String formType);

    void saveConfig(FormPermissionConfigSaveReqVO reqVO);

}
