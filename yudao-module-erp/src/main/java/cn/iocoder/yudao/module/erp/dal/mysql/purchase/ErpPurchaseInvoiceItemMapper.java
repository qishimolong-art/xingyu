package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpPurchaseInvoiceItemMapper extends BaseMapperX<ErpPurchaseInvoiceItemDO> {

    default List<ErpPurchaseInvoiceItemDO> selectListByInvoiceId(Long invoiceId) {
        return selectList(ErpPurchaseInvoiceItemDO::getInvoiceId, invoiceId);
    }

    default PageResult<ErpPurchaseInvoiceItemDO> selectPageByInvoiceId(ErpPurchaseInvoiceItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPurchaseInvoiceItemDO> query = new LambdaQueryWrapperX<ErpPurchaseInvoiceItemDO>()
                .eq(ErpPurchaseInvoiceItemDO::getInvoiceId, reqVO.getInvoiceId());
        SFunction<ErpPurchaseInvoiceItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpPurchaseInvoiceItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpPurchaseInvoiceItemDO::getId);
        }
        return selectPage(reqVO, query);
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

    static SFunction<ErpPurchaseInvoiceItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpPurchaseInvoiceItemDO::getId;
            case "sourceInId":
            case "sourceInNo":
                return ErpPurchaseInvoiceItemDO::getSourceInId;
            case "productId":
            case "productCode":
            case "productName":
                return ErpPurchaseInvoiceItemDO::getProductId;
            case "count":
                return ErpPurchaseInvoiceItemDO::getCount;
            case "productPrice":
                return ErpPurchaseInvoiceItemDO::getProductPrice;
            case "taxPercent":
                return ErpPurchaseInvoiceItemDO::getTaxPercent;
            case "taxPrice":
                return ErpPurchaseInvoiceItemDO::getTaxPrice;
            case "totalPrice":
                return ErpPurchaseInvoiceItemDO::getTotalPrice;
            case "remark":
                return ErpPurchaseInvoiceItemDO::getRemark;
            default:
                return null;
        }
    }
}
