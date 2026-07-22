package cn.iocoder.yudao.module.erp.dal.mysql.common;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpImportExportRecordMapper extends BaseMapperX<ErpImportExportRecordDO> {

    default PageResult<ErpImportExportRecordDO> selectPage(ErpImportExportRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpImportExportRecordDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(ErpImportExportRecordDO::getOperationType, reqVO.getOperationType());
        wrapper.eqIfPresent(ErpImportExportRecordDO::getModuleKey, reqVO.getModuleKey());
        wrapper.eqIfPresent(ErpImportExportRecordDO::getStatus, reqVO.getStatus());
        wrapper.likeIfPresent(ErpImportExportRecordDO::getFileName, reqVO.getFileName());
        wrapper.eqIfPresent(ErpImportExportRecordDO::getOperatorId, reqVO.getOperatorId());
        wrapper.betweenIfPresent(ErpImportExportRecordDO::getCreateTime, reqVO.getCreateTime());
        wrapper.orderByDesc(ErpImportExportRecordDO::getId);
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpImportExportRecordDO::getModuleKey,
                ErpImportExportRecordDO::getModuleName,
                ErpImportExportRecordDO::getFileName,
                ErpImportExportRecordDO::getErrorMessage,
                ErpImportExportRecordDO::getOperatorName);
        return selectPage(reqVO, wrapper);
    }

}
