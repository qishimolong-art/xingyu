package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpPurchaseInvoiceItemMapper extends BaseMapperX<ErpPurchaseInvoiceItemDO> {

    default List<ErpPurchaseInvoiceItemDO> selectListByInvoiceId(Long invoiceId) {
        return selectList(ErpPurchaseInvoiceItemDO::getInvoiceId, invoiceId);
    }

    default List<ErpPurchaseInvoiceItemDO> selectListByInvoiceIds(Collection<Long> invoiceIds) {
        return selectList(ErpPurchaseInvoiceItemDO::getInvoiceId, invoiceIds);
    }

    default List<ErpPurchaseInvoiceItemDO> selectListBySourceInId(Long sourceInId) {
        return selectList(ErpPurchaseInvoiceItemDO::getSourceInId, sourceInId);
    }

    default List<ErpPurchaseInvoiceItemDO> selectListBySourceInIds(Collection<Long> sourceInIds) {
        return selectList(ErpPurchaseInvoiceItemDO::getSourceInId, sourceInIds);
    }

    default int deleteByInvoiceId(Long invoiceId) {
        return delete(ErpPurchaseInvoiceItemDO::getInvoiceId, invoiceId);
    }
}
