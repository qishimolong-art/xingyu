package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPendingInDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 库存盘点单项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockCheckItemMapper extends BaseMapperX<ErpStockCheckItemDO> {

    default List<ErpStockCheckItemDO> selectListByCheckId(Long checkId) {
        return selectList(buildSortedQuery(checkId, null, null, null));
    }

    default List<ErpStockCheckItemDO> selectListByCheckId(Long checkId, String orderField,
                                                          String orderDirection) {
        return selectList(buildSortedQuery(checkId, null, orderField, orderDirection));
    }

    default List<ErpStockCheckItemDO> selectListByCheckIdForUpdate(Long checkId) {
        return selectList(new LambdaQueryWrapperX<ErpStockCheckItemDO>()
                .eq(ErpStockCheckItemDO::getCheckId, checkId).last("FOR UPDATE"));
    }

    default PageResult<ErpStockCheckItemDO> selectPageByCheckId(ErpStockCheckItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getCheckId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpStockCheckItemDO> buildSortedQuery(Long checkId, Collection<Long> excludedItemIds,
                                                               String orderField, String orderDirection) {
        String item = "erp_stock_check_item";
        QueryWrapper<ErpStockCheckItemDO> query = new QueryWrapper<ErpStockCheckItemDO>()
                .eq("check_id", checkId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpStockItemSortUtils.applyOrder(query, item, orderField, orderDirection,
                getOrderSpec(orderField));
    }

    default List<ErpStockCheckItemDO> selectListByCheckIds(Collection<Long> checkIds) {
        return selectList(ErpStockCheckItemDO::getCheckId, checkIds);
    }

    default Map<Long, Map<String, Object>> selectSummaryMapByCheckIds(Collection<Long> checkIds) {
        if (CollUtil.isEmpty(checkIds)) {
            return Collections.emptyMap();
        }
        return convertMap(selectSummaryRowsByCheckIds(checkIds), row -> toLong(row.get("checkId")), row -> row);
    }

    @Select({
            "<script>",
            "SELECT sci.check_id AS checkId,",
            "       COUNT(*) AS itemCount,",
            "       COALESCE(SUM(sci.count), 0) AS totalCount,",
            "       COALESCE(SUM(sci.total_price), 0) AS totalPrice,",
            "       COALESCE(SUM(sci.total_weight), 0) AS totalWeight,",
            "       GROUP_CONCAT(DISTINCT p.name ORDER BY p.name SEPARATOR ', ') AS productNames,",
            "       GROUP_CONCAT(DISTINCT p.code ORDER BY p.code SEPARATOR ', ') AS productCodes,",
            "       GROUP_CONCAT(DISTINCT w.name ORDER BY w.name SEPARATOR ', ') AS warehouseNames",
            "  FROM erp_stock_check_item sci",
            "  LEFT JOIN erp_product p ON p.id = sci.product_id AND p.deleted = b'0'",
            "  LEFT JOIN erp_warehouse w ON w.id = sci.warehouse_id AND w.deleted = b'0'",
            " WHERE sci.deleted = b'0'",
            "   AND sci.check_id IN",
            " <foreach collection='checkIds' item='checkId' open='(' separator=',' close=')'>#{checkId}</foreach>",
            " GROUP BY sci.check_id",
            "</script>"
    })
    List<Map<String, Object>> selectSummaryRowsByCheckIds(@Param("checkIds") Collection<Long> checkIds);

    default int deleteByCheckId(Long checkId) {
        return delete(ErpStockCheckItemDO::getCheckId, checkId);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpStockCheckItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpStockCheckItemDO::getWarehouseId, warehouseId);
    }

    default ErpStockCheckItemDO selectFirstByWarehouseId(Long warehouseId) {
        return selectFirstOne(ErpStockCheckItemDO::getWarehouseId, warehouseId);
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpStockItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) {
            return null;
        }
        String item = "erp_stock_check_item";
        return switch (orderField.trim()) {
            case "id" -> spec(item + ".id", false);
            case "productId", "productName" -> spec(ErpStockItemSortUtils.productField(item, "name"), true);
            case "productCode" -> spec(ErpStockItemSortUtils.productField(item, "code"), true);
            case "productBarCode" -> spec(ErpStockItemSortUtils.productField(item, "bar_code"), true);
            case "productUnitName" -> spec(ErpStockItemSortUtils.itemUnitName(item), true);
            case "warehouseId" -> spec(ErpStockItemSortUtils.warehouseName(item, "warehouse_id"), true);
            case "warehouseDeptName" -> spec(ErpStockItemSortUtils.warehouseDeptName(item, "warehouse_id"), true);
            case "stockCount" -> spec(item + ".stock_count", false);
            case "actualCount" -> spec(item + ".actual_count", false);
            case "count" -> spec(item + ".count", false);
            case "productPrice" -> spec(item + ".product_price", false);
            case "totalPrice" -> spec(item + ".total_price", false);
            case "weight" -> spec(item + ".weight", false);
            case "packageQty" -> spec(item + ".package_qty", false);
            case "pieceCount" -> spec(ErpStockItemSortUtils.pieceCount(item), false);
            case "totalWeight" -> spec(item + ".total_weight", false);
            case "batchNo" -> spec(item + ".batch_no", true);
            case "remark" -> spec(item + ".remark", true);
            default -> null;
        };
    }

    private static ErpStockItemSortUtils.OrderSpec spec(String expression, boolean text) {
        return new ErpStockItemSortUtils.OrderSpec(expression, text);
    }

    static Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    @Select({
            "SELECT sc.id AS checkId,",
            "       sci.id AS itemId,",
            "       sc.no AS no,",
            "       sci.product_id AS productId,",
            "       p.code AS productCode,",
            "       p.name AS productName,",
            "       sci.warehouse_id AS warehouseId,",
            "       w.name AS warehouseName,",
            "       COALESCE(sci.count, 0) AS count,",
            "       p.vehicle_model AS vehicleModel,",
            "       p.origin_place AS originPlace,",
            "       p.drawing_no AS drawingNo,",
            "       p.standard AS standard,",
            "       sc.create_time AS createTime,",
            "       sc.creator AS creator",
            "  FROM erp_stock_check_item sci",
            " INNER JOIN erp_stock_check sc ON sc.id = sci.check_id",
            "   AND sc.deleted = 0 AND sc.status = #{status} AND sc.check_type = #{checkType}",
            "  LEFT JOIN erp_product p ON p.id = sci.product_id AND p.deleted = 0",
            "  LEFT JOIN erp_warehouse w ON w.id = sci.warehouse_id AND w.deleted = 0",
            " WHERE sci.deleted = 0 AND sci.count > 0",
            "   AND sci.product_id = #{productId} AND sci.warehouse_id = #{warehouseId}",
            " ORDER BY sc.create_time DESC, sc.id DESC, sci.id DESC"
    })
    List<ErpStockPendingInDetailRespVO> selectPendingInDetails(@Param("productId") Long productId,
                                                               @Param("warehouseId") Long warehouseId,
                                                               @Param("status") Integer status,
                                                               @Param("checkType") Integer checkType);

}
