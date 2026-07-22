package cn.iocoder.yudao.module.erp.dal.mysql.common;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordDetailPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDetailDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpImportExportRecordDetailMapper extends BaseMapperX<ErpImportExportRecordDetailDO> {

    default PageResult<ErpImportExportRecordDetailDO> selectPage(ErpImportExportRecordDetailPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpImportExportRecordDetailDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ErpImportExportRecordDetailDO::getRecordId, reqVO.getRecordId());
        wrapper.orderByAsc(ErpImportExportRecordDetailDO::getRowNo);
        wrapper.orderByAsc(ErpImportExportRecordDetailDO::getId);
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpImportExportRecordDetailDO::getBizKey,
                ErpImportExportRecordDetailDO::getBizName,
                ErpImportExportRecordDetailDO::getFailureReason);
        return selectPage(reqVO, wrapper);
    }

}
