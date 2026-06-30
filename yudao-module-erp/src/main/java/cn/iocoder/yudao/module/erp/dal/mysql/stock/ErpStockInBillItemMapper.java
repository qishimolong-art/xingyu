package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpStockInBillItemMapper extends BaseMapperX<ErpStockInBillItemDO> {

    default List<ErpStockInBillItemDO> selectListByBillId(Long billId) {
        return selectList(new LambdaQueryWrapperX<ErpStockInBillItemDO>()
                .eq(ErpStockInBillItemDO::getBillId, billId)
                .orderByAsc(ErpStockInBillItemDO::getId));
    }

    default List<ErpStockInBillItemDO> selectListByBillIds(Collection<Long> billIds) {
        return selectList(ErpStockInBillItemDO::getBillId, billIds);
    }

}
