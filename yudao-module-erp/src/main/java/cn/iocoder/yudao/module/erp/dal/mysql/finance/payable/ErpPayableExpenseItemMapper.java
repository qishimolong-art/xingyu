package cn.iocoder.yudao.module.erp.dal.mysql.finance.payable;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpPayableExpenseItemMapper extends BaseMapperX<ErpPayableExpenseItemDO> {

    default List<ErpPayableExpenseItemDO> selectListByExpenseId(Long expenseId) {
        return selectList(ErpPayableExpenseItemDO::getExpenseId, expenseId);
    }

    default List<ErpPayableExpenseItemDO> selectListByExpenseIds(Collection<Long> expenseIds) {
        return selectList(ErpPayableExpenseItemDO::getExpenseId, expenseIds);
    }

    default void deleteByExpenseId(Long expenseId) {
        delete(ErpPayableExpenseItemDO::getExpenseId, expenseId);
    }

    default List<ErpPayableExpenseItemDO> selectListByItemNameOrInvoiceNo(String itemName, String invoiceNo) {
        return selectList(new LambdaQueryWrapperX<ErpPayableExpenseItemDO>()
                .likeIfPresent(ErpPayableExpenseItemDO::getItemName, itemName)
                .likeIfPresent(ErpPayableExpenseItemDO::getInvoiceNo, invoiceNo));
    }

}
