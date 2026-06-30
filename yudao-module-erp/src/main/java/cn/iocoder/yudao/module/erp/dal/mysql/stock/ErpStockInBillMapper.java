package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpStockInBillMapper extends BaseMapperX<ErpStockInBillDO> {

    default PageResult<ErpStockInBillDO> selectPage(ErpStockInBillPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpStockInBillDO>()
                .likeIfPresent(ErpStockInBillDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpStockInBillDO::getBillDate, reqVO.getBillDate())
                .eqIfPresent(ErpStockInBillDO::getWarehouseId, reqVO.getWarehouseId())
                .likeIfPresent(ErpStockInBillDO::getSourceUnitName, reqVO.getSourceUnitName())
                .likeIfPresent(ErpStockInBillDO::getSourceNo, reqVO.getSourceNo())
                .eqIfPresent(ErpStockInBillDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpStockInBillDO::getPickupUserName, reqVO.getPickupUserName())
                .eqIfPresent(ErpStockInBillDO::getCreator, reqVO.getCreator())
                .orderByDesc(ErpStockInBillDO::getId));
    }

    default ErpStockInBillDO selectByNo(String no) {
        return selectOne(ErpStockInBillDO::getNo, no);
    }

}
