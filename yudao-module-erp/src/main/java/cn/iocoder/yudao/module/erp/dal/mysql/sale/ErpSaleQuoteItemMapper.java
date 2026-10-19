package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ERP 报价订单项 Mapper
 */
@Mapper
public interface ErpSaleQuoteItemMapper extends BaseMapperX<ErpSaleQuoteItemDO> {

    default List<ErpSaleQuoteItemDO> selectListByQuoteId(Long quoteId) {
        return selectList(buildSortedQuery(quoteId, null, null, null));
    }

    default List<ErpSaleQuoteItemDO> selectListByQuoteId(Long quoteId, String orderField,
                                                         String orderDirection) {
        return selectList(buildSortedQuery(quoteId, null, orderField, orderDirection));
    }

    default List<ErpSaleQuoteItemDO> selectListByQuoteIdForUpdate(Long quoteId) {
        return selectList(new LambdaQueryWrapperX<ErpSaleQuoteItemDO>()
                .eq(ErpSaleQuoteItemDO::getQuoteId, quoteId).last("FOR UPDATE"));
    }

    default PageResult<ErpSaleQuoteItemDO> selectPageByQuoteId(ErpSaleQuoteItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getQuoteId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpSaleQuoteItemDO> buildSortedQuery(Long quoteId, Collection<Long> excludedItemIds,
                                                              String orderField, String orderDirection) {
        QueryWrapper<ErpSaleQuoteItemDO> query = new QueryWrapper<ErpSaleQuoteItemDO>()
                .eq("quote_id", quoteId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpSaleItemSortUtils.applyOrder(query, "erp_sale_quote_items", orderField, orderDirection,
                getOrderSpec(orderField));
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpSaleItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) return null;
        String item = "erp_sale_quote_items";
        return switch (orderField.trim()) {
            case "id" -> new ErpSaleItemSortUtils.OrderSpec(item + ".id", false);
            case "productId", "productName" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.productField(item, "name"), true);
            case "productCode" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.productField(item, "code"), true);
            case "productBarCode" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.productField(item, "bar_code"), true);
            case "productUnitName" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.productUnitName(item), true);
            case "warehouseId" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.warehouseName(item, "warehouse_id"), true);
            case "deptId" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.deptName(item, "dept_id"), true);
            case "stockCount" -> new ErpSaleItemSortUtils.OrderSpec(ErpSaleItemSortUtils.stockCount(item), false);
            case "lastSalePrice" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.lastSalePrice(item), false);
            case "pieceCount" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.pieceCount(item, "count"), false);
            case "giftFlag" -> new ErpSaleItemSortUtils.OrderSpec(item + ".gift_flag", false);
            case "batchNo" -> new ErpSaleItemSortUtils.OrderSpec(item + ".batch_no", true);
            case "count" -> new ErpSaleItemSortUtils.OrderSpec(item + ".count", false);
            case "convertedCount" -> new ErpSaleItemSortUtils.OrderSpec(item + ".converted_count", false);
            case "productPrice", "salePrice" -> new ErpSaleItemSortUtils.OrderSpec(
                    item + ".product_price", false);
            case "totalPrice" -> new ErpSaleItemSortUtils.OrderSpec(item + ".total_price", false);
            case "taxPercent" -> new ErpSaleItemSortUtils.OrderSpec(item + ".tax_percent", false);
            case "taxPrice" -> new ErpSaleItemSortUtils.OrderSpec(item + ".tax_price", false);
            case "vehicleModel" -> new ErpSaleItemSortUtils.OrderSpec(item + ".vehicle_model", true);
            case "standard" -> new ErpSaleItemSortUtils.OrderSpec(item + ".standard", true);
            case "weight" -> new ErpSaleItemSortUtils.OrderSpec(item + ".weight", false);
            case "packageQty" -> new ErpSaleItemSortUtils.OrderSpec(item + ".package_qty", false);
            case "warehousePosition" -> new ErpSaleItemSortUtils.OrderSpec(item + ".warehouse_position", true);
            case "drawingNo" -> new ErpSaleItemSortUtils.OrderSpec(item + ".drawing_no", true);
            case "brand" -> new ErpSaleItemSortUtils.OrderSpec(item + ".brand", true);
            case "originPlace" -> new ErpSaleItemSortUtils.OrderSpec(item + ".origin_place", true);
            case "remark" -> new ErpSaleItemSortUtils.OrderSpec(item + ".remark", true);
            default -> null;
        };
    }

    default List<ErpSaleQuoteItemDO> selectListByQuoteIds(Collection<Long> quoteIds) {
        return selectList(ErpSaleQuoteItemDO::getQuoteId, quoteIds);
    }

    default int deleteByQuoteId(Long quoteId) {
        return delete(ErpSaleQuoteItemDO::getQuoteId, quoteId);
    }

    static SFunction<ErpSaleQuoteItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpSaleQuoteItemDO::getId;
            case "giftFlag":
                return ErpSaleQuoteItemDO::getGiftFlag;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
            case "salePrice":
            case "lastSalePrice":
                return ErpSaleQuoteItemDO::getProductId;
            case "warehouseId":
                return ErpSaleQuoteItemDO::getWarehouseId;
            case "deptId":
                return ErpSaleQuoteItemDO::getDeptId;
            case "count":
                return ErpSaleQuoteItemDO::getCount;
            case "convertedCount":
                return ErpSaleQuoteItemDO::getConvertedCount;
            case "productPrice":
                return ErpSaleQuoteItemDO::getProductPrice;
            case "totalPrice":
                return ErpSaleQuoteItemDO::getTotalPrice;
            case "taxPercent":
                return ErpSaleQuoteItemDO::getTaxPercent;
            case "taxPrice":
                return ErpSaleQuoteItemDO::getTaxPrice;
            case "vehicleModel":
                return ErpSaleQuoteItemDO::getVehicleModel;
            case "standard":
                return ErpSaleQuoteItemDO::getStandard;
            case "weight":
                return ErpSaleQuoteItemDO::getWeight;
            case "packageQty":
                return ErpSaleQuoteItemDO::getPackageQty;
            case "warehousePosition":
                return ErpSaleQuoteItemDO::getWarehousePosition;
            case "drawingNo":
                return ErpSaleQuoteItemDO::getDrawingNo;
            case "batchNo":
                return ErpSaleQuoteItemDO::getBatchNo;
            case "brand":
                return ErpSaleQuoteItemDO::getBrand;
            case "originPlace":
                return ErpSaleQuoteItemDO::getOriginPlace;
            case "remark":
                return ErpSaleQuoteItemDO::getRemark;
            default:
                return null;
        }
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpSaleQuoteItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpSaleQuoteItemDO::getWarehouseId, warehouseId);
    }

    default int updateWarehouseDeptByIds(Collection<Long> ids, Long warehouseId, Long deptId, boolean clearBatchNo) {
        LambdaUpdateWrapper<ErpSaleQuoteItemDO> wrapper = new LambdaUpdateWrapper<ErpSaleQuoteItemDO>()
                .in(ErpSaleQuoteItemDO::getId, ids)
                .set(warehouseId != null, ErpSaleQuoteItemDO::getWarehouseId, warehouseId)
                .set(ErpSaleQuoteItemDO::getDeptId, deptId);
        if (clearBatchNo) {
            wrapper.set(ErpSaleQuoteItemDO::getBatchNo, null);
        }
        return update(null, wrapper);
    }

    default int updateStockCountAtOrderIfNull(Long id, BigDecimal stockCount) {
        return update(null, new LambdaUpdateWrapper<ErpSaleQuoteItemDO>()
                .eq(ErpSaleQuoteItemDO::getId, id)
                .isNull(ErpSaleQuoteItemDO::getStockCountAtOrder)
                .set(ErpSaleQuoteItemDO::getStockCountAtOrder, stockCount));
    }

    default int clearStockCountAtOrder(Long id) {
        return update(null, new LambdaUpdateWrapper<ErpSaleQuoteItemDO>()
                .eq(ErpSaleQuoteItemDO::getId, id)
                .set(ErpSaleQuoteItemDO::getStockCountAtOrder, null));
    }

}
