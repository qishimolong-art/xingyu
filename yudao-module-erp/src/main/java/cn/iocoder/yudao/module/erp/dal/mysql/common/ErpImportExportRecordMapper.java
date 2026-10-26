package cn.iocoder.yudao.module.erp.dal.mysql.common;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpImportExportRecordMapper extends BaseMapperX<ErpImportExportRecordDO> {

    default ErpImportExportRecordDO selectLatestProcessing(String operationType, String moduleKey, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<ErpImportExportRecordDO>()
                .eqIfPresent(ErpImportExportRecordDO::getTenantId, tenantId)
                .eq(ErpImportExportRecordDO::getOperationType, operationType)
                .eq(ErpImportExportRecordDO::getModuleKey, moduleKey)
                .eq(ErpImportExportRecordDO::getStatus, "PROCESSING")
                .orderByDesc(ErpImportExportRecordDO::getId)
                .last("LIMIT 1"));
    }

    default ErpImportExportRecordDO selectLatestProcessingByOperator(String operationType, String moduleKey,
                                                                     Long tenantId, Long operatorId) {
        return selectOne(new LambdaQueryWrapperX<ErpImportExportRecordDO>()
                .eqIfPresent(ErpImportExportRecordDO::getTenantId, tenantId)
                .eq(ErpImportExportRecordDO::getOperationType, operationType)
                .eq(ErpImportExportRecordDO::getModuleKey, moduleKey)
                .eq(ErpImportExportRecordDO::getOperatorId, operatorId)
                .eq(ErpImportExportRecordDO::getStatus, "PROCESSING")
                .orderByDesc(ErpImportExportRecordDO::getId)
                .last("LIMIT 1"));
    }

    default ErpImportExportRecordDO selectByIdAndTenant(Long id, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<ErpImportExportRecordDO>()
                .eq(ErpImportExportRecordDO::getId, id)
                .eqIfPresent(ErpImportExportRecordDO::getTenantId, tenantId));
    }

    default PageResult<ErpImportExportRecordDO> selectOwnedExportPage(PageParam pageParam, String moduleKey,
                                                                       Long tenantId, Long operatorId) {
        return selectPage(pageParam, new LambdaQueryWrapperX<ErpImportExportRecordDO>()
                .eq(ErpImportExportRecordDO::getTenantId, tenantId)
                .eq(ErpImportExportRecordDO::getOperationType, "EXPORT")
                .eq(ErpImportExportRecordDO::getModuleKey, moduleKey)
                .eq(ErpImportExportRecordDO::getOperatorId, operatorId)
                .orderByDesc(ErpImportExportRecordDO::getId));
    }

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
