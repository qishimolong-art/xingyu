package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillItemDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpStockOutBillItemMapper extends BaseMapperX<ErpStockOutBillItemDO> {

    default List<ErpStockOutBillItemDO> selectListByBillId(Long billId) {
        return selectList(new LambdaQueryWrapperX<ErpStockOutBillItemDO>()
                .eq(ErpStockOutBillItemDO::getBillId, billId)
                .orderByAsc(ErpStockOutBillItemDO::getId));
    }

    default List<ErpStockOutBillItemDO> selectListByBillIds(Collection<Long> billIds) {
        return selectList(ErpStockOutBillItemDO::getBillId, billIds);
    }

    default int updatePickedCountByIdAndPickedCount(Long id, BigDecimal oldPickedCount,
                                                    BigDecimal newPickedCount, Integer status) {
        return update(new ErpStockOutBillItemDO()
                        .setPickedCount(newPickedCount)
                        .setStatus(status),
                new LambdaUpdateWrapper<ErpStockOutBillItemDO>()
                        .eq(ErpStockOutBillItemDO::getId, id)
                        .eq(ErpStockOutBillItemDO::getPickedCount, oldPickedCount));
    }

}
