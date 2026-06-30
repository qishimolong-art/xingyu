package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpStockOutBillMapper extends BaseMapperX<ErpStockOutBillDO> {

    default PageResult<ErpStockOutBillDO> selectPage(ErpStockOutBillPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpStockOutBillDO>()
                .likeIfPresent(ErpStockOutBillDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpStockOutBillDO::getBillDate, reqVO.getBillDate())
                .eqIfPresent(ErpStockOutBillDO::getWarehouseId, reqVO.getWarehouseId())
                .likeIfPresent(ErpStockOutBillDO::getShippingArea, reqVO.getShippingArea())
                .likeIfPresent(ErpStockOutBillDO::getSourceUnitName, reqVO.getSourceUnitName())
                .likeIfPresent(ErpStockOutBillDO::getSourceNo, reqVO.getSourceNo())
                .eqIfPresent(ErpStockOutBillDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpStockOutBillDO::getPickUserName, reqVO.getPickUserName())
                .eqIfPresent(ErpStockOutBillDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpStockOutBillDO::getAuditor, reqVO.getAuditor())
                .orderByDesc(ErpStockOutBillDO::getId));
    }

    default ErpStockOutBillDO selectBySource(Integer sourceBizType, Long sourceId) {
        return selectOne(ErpStockOutBillDO::getSourceBizType, sourceBizType,
                ErpStockOutBillDO::getSourceId, sourceId);
    }

    default List<ErpStockOutBillDO> selectListBySource(Integer sourceBizType, Long sourceId) {
        return selectList(new LambdaQueryWrapperX<ErpStockOutBillDO>()
                .eq(ErpStockOutBillDO::getSourceBizType, sourceBizType)
                .eq(ErpStockOutBillDO::getSourceId, sourceId)
                .orderByAsc(ErpStockOutBillDO::getId));
    }

    default ErpStockOutBillDO selectByNo(String no) {
        return selectOne(ErpStockOutBillDO::getNo, no);
    }

}
