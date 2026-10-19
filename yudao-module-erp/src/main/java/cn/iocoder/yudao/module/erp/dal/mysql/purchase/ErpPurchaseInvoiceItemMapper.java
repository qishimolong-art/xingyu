package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpPurchaseInvoiceItemMapper extends BaseMapperX<ErpPurchaseInvoiceItemDO> {

    default List<ErpPurchaseInvoiceItemDO> selectListByInvoiceId(Long invoiceId) {
        return selectList(buildSortedQuery(invoiceId, null, null, null));
    }

    default List<ErpPurchaseInvoiceItemDO> selectListByInvoiceId(Long invoiceId, String orderField,
                                                                 String orderDirection) {
        return selectList(buildSortedQuery(invoiceId, null, orderField, orderDirection));
    }

    default List<ErpPurchaseInvoiceItemDO> selectListByInvoiceIdForUpdate(Long invoiceId) {
        return selectList(new LambdaQueryWrapperX<ErpPurchaseInvoiceItemDO>()
                .eq(ErpPurchaseInvoiceItemDO::getInvoiceId, invoiceId).last("FOR UPDATE"));
    }

    default List<ErpPurchaseInvoiceItemDO> selectSourceRefListByInvoiceId(Long invoiceId) {
        return selectList(new QueryWrapper<ErpPurchaseInvoiceItemDO>()
                .select("source_type", "source_doc_id", "source_in_id")
                .eq("invoice_id", invoiceId)
                .and(query -> query.isNotNull("source_doc_id").or().isNotNull("source_in_id"))
                .groupBy("source_type", "source_doc_id", "source_in_id")
                .orderByAsc("source_type", "source_doc_id", "source_in_id"));
    }

    default PageResult<ErpPurchaseInvoiceItemDO> selectPageByInvoiceId(ErpPurchaseInvoiceItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getInvoiceId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpPurchaseInvoiceItemDO> buildSortedQuery(Long invoiceId, Collection<Long> excludedItemIds,
                                                                    String orderField, String orderDirection) {
        QueryWrapper<ErpPurchaseInvoiceItemDO> query = new QueryWrapper<ErpPurchaseInvoiceItemDO>()
                .eq("invoice_id", invoiceId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpPurchaseItemSortUtils.applyOrder(query, "erp_purchase_invoice_item", orderField, orderDirection,
                getOrderSpec(orderField));
    }

    default List<ErpPurchaseInvoiceItemDO> selectListByInvoiceIds(Collection<Long> invoiceIds) {
        return selectList(ErpPurchaseInvoiceItemDO::getInvoiceId, invoiceIds);
    }

    default List<ErpPurchaseInvoiceItemDO> selectTaxPercentListByInvoiceIds(Collection<Long> invoiceIds) {
        if (CollUtil.isEmpty(invoiceIds)) {
            return Collections.emptyList();
        }
        return selectList(new QueryWrapper<ErpPurchaseInvoiceItemDO>()
                .select("invoice_id", "tax_percent")
                .in("invoice_id", invoiceIds));
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

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpPurchaseItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) {
            return null;
        }
        String item = "erp_purchase_invoice_item";
        return switch (orderField.trim()) {
            case "id" -> spec(item + ".id", false);
            case "sourceInId" -> spec(item + ".source_in_id", false);
            case "sourceInNo" -> spec(item + ".source_in_no", true);
            case "sourceType", "sourceTypeName" -> spec(item + ".source_type_name", true);
            case "sourceDocNo" -> spec("COALESCE(" + item + ".source_doc_no, " + item + ".source_in_no)", true);
            case "productId", "productName" -> spec(ErpPurchaseItemSortUtils.productField(item, "name"), true);
            case "productCode" -> spec(ErpPurchaseItemSortUtils.productField(item, "code"), true);
            case "productBarCode" -> spec("COALESCE(" + item + ".product_bar_code, "
                    + ErpPurchaseItemSortUtils.productField(item, "bar_code") + ")", true);
            case "productUnitName" -> spec(item + ".product_unit_name", true);
            case "warehouseId" -> spec(ErpPurchaseItemSortUtils.warehouseName(item), true);
            case "stockCount" -> spec(ErpPurchaseItemSortUtils.stockCount(item), false);
            case "count" -> spec(item + ".count", false);
            case "sourceRemainingCount" -> spec(item + ".source_remaining_count", false);
            case "packageQty" -> spec(item + ".package_qty", false);
            case "pieceCount" -> spec(ErpPurchaseItemSortUtils.pieceCount(item + ".count",
                    item + ".package_qty"), false);
            case "productPrice" -> spec(item + ".product_price", false);
            case "taxExclusivePrice" -> spec(item + ".tax_exclusive_price", false);
            case "taxPercent" -> spec(item + ".tax_percent", false);
            case "taxPrice" -> spec(item + ".tax_price", false);
            case "totalPrice" -> spec(item + ".total_price", false);
            case "remark" -> spec(item + ".remark", true);
            default -> null;
        };
    }

    private static ErpPurchaseItemSortUtils.OrderSpec spec(String expression, boolean text) {
        return new ErpPurchaseItemSortUtils.OrderSpec(expression, text);
    }
}
