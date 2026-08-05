package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpReceivableOtherIncomeItemMapper extends BaseMapperX<ErpReceivableOtherIncomeItemDO> {

    default List<ErpReceivableOtherIncomeItemDO> selectListByIncomeId(Long incomeId) {
        return selectList(ErpReceivableOtherIncomeItemDO::getIncomeId, incomeId);
    }

    default List<ErpReceivableOtherIncomeItemDO> selectListByIncomeIds(Collection<Long> incomeIds) {
        return selectList(ErpReceivableOtherIncomeItemDO::getIncomeId, incomeIds);
    }

    default int deleteByIncomeId(Long incomeId) {
        return delete(ErpReceivableOtherIncomeItemDO::getIncomeId, incomeId);
    }

    default List<ErpReceivableOtherIncomeItemDO> selectListByItemNameOrInvoiceNo(String itemName, String invoiceNo) {
        return selectList(new LambdaQueryWrapperX<ErpReceivableOtherIncomeItemDO>()
                .likeIfPresent(ErpReceivableOtherIncomeItemDO::getItemName, itemName)
                .likeIfPresent(ErpReceivableOtherIncomeItemDO::getInvoiceNo, invoiceNo));
    }
}
