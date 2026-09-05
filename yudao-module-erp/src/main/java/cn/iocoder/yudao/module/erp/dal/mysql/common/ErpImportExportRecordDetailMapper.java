package cn.iocoder.yudao.module.erp.dal.mysql.common;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordDetailPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpImportExportRecordDetailMapper extends BaseMapperX<ErpImportExportRecordDetailDO> {

    default PageResult<ErpImportExportRecordDetailDO> selectPage(ErpImportExportRecordDetailPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpImportExportRecordDetailDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ErpImportExportRecordDetailDO::getRecordId, reqVO.getRecordId());
        wrapper.and(w -> w.isNull(ErpImportExportRecordDetailDO::getDetailType)
                .or().eq(ErpImportExportRecordDetailDO::getDetailType, "FAILURE"));
        wrapper.orderByAsc(ErpImportExportRecordDetailDO::getRowNo);
        wrapper.orderByAsc(ErpImportExportRecordDetailDO::getId);
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpImportExportRecordDetailDO::getBizKey,
                ErpImportExportRecordDetailDO::getBizName,
                ErpImportExportRecordDetailDO::getFailureReason);
        return selectPage(reqVO, wrapper);
    }

    default List<ErpImportExportRecordDetailDO> selectListByRecordId(Long recordId) {
        return selectList(new LambdaQueryWrapperX<ErpImportExportRecordDetailDO>()
                .eq(ErpImportExportRecordDetailDO::getRecordId, recordId)
                .orderByAsc(ErpImportExportRecordDetailDO::getGroupKey)
                .orderByAsc(ErpImportExportRecordDetailDO::getRowNo)
                .orderByAsc(ErpImportExportRecordDetailDO::getId));
    }

    default List<ErpImportExportRecordDetailDO> selectFailureListByRecordId(Long recordId) {
        return selectList(new LambdaQueryWrapperX<ErpImportExportRecordDetailDO>()
                .eq(ErpImportExportRecordDetailDO::getRecordId, recordId)
                .and(w -> w.isNull(ErpImportExportRecordDetailDO::getDetailType)
                        .or().eq(ErpImportExportRecordDetailDO::getDetailType, "FAILURE"))
                .orderByAsc(ErpImportExportRecordDetailDO::getRowNo)
                .orderByAsc(ErpImportExportRecordDetailDO::getId));
    }

}
