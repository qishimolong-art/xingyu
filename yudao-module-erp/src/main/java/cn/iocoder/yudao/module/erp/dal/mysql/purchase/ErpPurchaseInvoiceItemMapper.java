package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
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

    default List<ErpPurchaseInvoiceItemDO> selectApprovedListBySourceInIds(Collection<Long> sourceInIds) {
        return selectJoinList(ErpPurchaseInvoiceItemDO.class,
                new MPJLambdaWrapperX<ErpPurchaseInvoiceItemDO>()
                        .in(ErpPurchaseInvoiceItemDO::getSourceInId, sourceInIds)
                        .innerJoin(ErpPurchaseInvoiceDO.class, ErpPurchaseInvoiceDO::getId,
                                ErpPurchaseInvoiceItemDO::getInvoiceId)
                        .eq(ErpPurchaseInvoiceDO::getStatus, ErpAuditStatus.APPROVE.getStatus()));
    }

    default int deleteByInvoiceId(Long invoiceId) {
        return delete(ErpPurchaseInvoiceItemDO::getInvoiceId, invoiceId);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpPurchaseInvoiceItemDO::getProductId, productId);
    }
}
