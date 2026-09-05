package cn.iocoder.yudao.module.erp.dal.mysql.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpPayableExpenseItemMapper extends BaseMapperX<ErpPayableExpenseItemDO> {

    default List<ErpPayableExpenseItemDO> selectListByExpenseId(Long expenseId) {
        return selectList(new LambdaQueryWrapperX<ErpPayableExpenseItemDO>()
                .eq(ErpPayableExpenseItemDO::getExpenseId, expenseId)
                .orderByAsc(ErpPayableExpenseItemDO::getId));
    }

    default PageResult<ErpPayableExpenseItemDO> selectPageByExpenseId(ErpPayableExpenseItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPayableExpenseItemDO> query = new LambdaQueryWrapperX<ErpPayableExpenseItemDO>()
                .eq(ErpPayableExpenseItemDO::getExpenseId, reqVO.getExpenseId());
        SFunction<ErpPayableExpenseItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpPayableExpenseItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpPayableExpenseItemDO::getId);
        }
        return selectPage(reqVO, query);
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

    static SFunction<ErpPayableExpenseItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpPayableExpenseItemDO::getId;
            case "itemName":
                return ErpPayableExpenseItemDO::getItemName;
            case "amount":
                return ErpPayableExpenseItemDO::getAmount;
            case "invoiceNo":
                return ErpPayableExpenseItemDO::getInvoiceNo;
            case "party":
                return ErpPayableExpenseItemDO::getParty;
            case "deptId":
                return ErpPayableExpenseItemDO::getDeptId;
            case "bizDate":
                return ErpPayableExpenseItemDO::getBizDate;
            case "handlerId":
                return ErpPayableExpenseItemDO::getHandlerId;
            case "qty":
                return ErpPayableExpenseItemDO::getQty;
            case "expenseCategory":
                return ErpPayableExpenseItemDO::getExpenseCategory;
            case "remark":
                return ErpPayableExpenseItemDO::getRemark;
            default:
                return null;
        }
    }

}
