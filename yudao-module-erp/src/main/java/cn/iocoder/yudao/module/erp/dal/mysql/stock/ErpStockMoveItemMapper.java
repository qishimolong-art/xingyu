package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 库存调拨单项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockMoveItemMapper extends BaseMapperX<ErpStockMoveItemDO> {

    default List<ErpStockMoveItemDO> selectListByMoveId(Long moveId) {
        return selectList(ErpStockMoveItemDO::getMoveId, moveId);
    }

    default PageResult<ErpStockMoveItemDO> selectPageByMoveId(ErpStockMoveItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpStockMoveItemDO> query = new LambdaQueryWrapperX<ErpStockMoveItemDO>()
                .eq(ErpStockMoveItemDO::getMoveId, reqVO.getMoveId());
        SFunction<ErpStockMoveItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpStockMoveItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpStockMoveItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpStockMoveItemDO> selectListByMoveIds(Collection<Long> moveIds) {
        return selectList(ErpStockMoveItemDO::getMoveId, moveIds);
    }

    default int deleteByMoveId(Long moveId) {
        return delete(ErpStockMoveItemDO::getMoveId, moveId);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpStockMoveItemDO::getProductId, productId);
    }

    default Long selectCountByFromWarehouseId(Long warehouseId) {
        return selectCount(ErpStockMoveItemDO::getFromWarehouseId, warehouseId);
    }

    default Long selectCountByToWarehouseId(Long warehouseId) {
        return selectCount(ErpStockMoveItemDO::getToWarehouseId, warehouseId);
    }

    default ErpStockMoveItemDO selectFirstByFromWarehouseId(Long warehouseId) {
        return selectFirstOne(ErpStockMoveItemDO::getFromWarehouseId, warehouseId);
    }

    default ErpStockMoveItemDO selectFirstByToWarehouseId(Long warehouseId) {
        return selectFirstOne(ErpStockMoveItemDO::getToWarehouseId, warehouseId);
    }

    static SFunction<ErpStockMoveItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpStockMoveItemDO::getId;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpStockMoveItemDO::getProductId;
            case "fromWarehouseId":
                return ErpStockMoveItemDO::getFromWarehouseId;
            case "toWarehouseId":
                return ErpStockMoveItemDO::getToWarehouseId;
            case "fromDeptId":
                return ErpStockMoveItemDO::getFromDeptId;
            case "toDeptId":
                return ErpStockMoveItemDO::getToDeptId;
            case "count":
                return ErpStockMoveItemDO::getCount;
            case "productPrice":
                return ErpStockMoveItemDO::getProductPrice;
            case "totalPrice":
                return ErpStockMoveItemDO::getTotalPrice;
            case "weight":
                return ErpStockMoveItemDO::getWeight;
            case "packageQty":
                return ErpStockMoveItemDO::getPackageQty;
            case "totalWeight":
                return ErpStockMoveItemDO::getTotalWeight;
            case "batchNo":
                return ErpStockMoveItemDO::getBatchNo;
            case "fromShelf":
                return ErpStockMoveItemDO::getFromShelf;
            case "sourceCount":
                return ErpStockMoveItemDO::getSourceCount;
            case "remark":
                return ErpStockMoveItemDO::getRemark;
            default:
                return null;
        }
    }

    default Map<Long, BigDecimal> selectMovedCountMapBySourceInItemIds(Collection<Long> sourceInItemIds) {
        return selectMovedCountMapBySourceInItemIds(sourceInItemIds, null);
    }

    default Map<Long, BigDecimal> selectMovedCountMapBySourceInItemIds(Collection<Long> sourceInItemIds,
                                                                       Long excludeMoveId) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(sourceInItemIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectMovedCountListBySourceInItemIds(sourceInItemIds, excludeMoveId);
        return convertMap(result,
                obj -> (Long) obj.get("source_in_item_id"),
                obj -> (BigDecimal) obj.get("sum_count"));
    }

    default Map<Long, BigDecimal> selectMovedCountMapBySourceSaleReturnItemIds(
            Collection<Long> sourceSaleReturnItemIds, Long excludeMoveId) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(sourceSaleReturnItemIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectMovedCountListBySourceSaleReturnItemIds(
                sourceSaleReturnItemIds, excludeMoveId);
        return convertMap(result,
                obj -> (Long) obj.get("source_sale_return_item_id"),
                obj -> (BigDecimal) obj.get("sum_count"));
    }

    @Select({
            "<script>",
            "SELECT smi.source_in_item_id, SUM(smi.count) AS sum_count",
            "  FROM erp_stock_move_item smi",
            " INNER JOIN erp_stock_move sm ON sm.id = smi.move_id",
            "   AND sm.deleted = 0",
            "   AND (sm.transfer_direction = 10 OR sm.transfer_direction IS NULL)",
            " WHERE smi.deleted = 0",
            "   AND smi.source_in_item_id IN",
            " <foreach collection='sourceInItemIds' item='sourceInItemId' open='(' separator=',' close=')'>",
            "   #{sourceInItemId}",
            " </foreach>",
            " <if test='excludeMoveId != null'>",
            "   AND smi.move_id &lt;&gt; #{excludeMoveId}",
            " </if>",
            " GROUP BY smi.source_in_item_id",
            "</script>"
    })
    List<Map<String, Object>> selectMovedCountListBySourceInItemIds(
            @Param("sourceInItemIds") Collection<Long> sourceInItemIds,
            @Param("excludeMoveId") Long excludeMoveId);

    @Select({
            "<script>",
            "SELECT smi.source_sale_return_item_id, SUM(smi.count) AS sum_count",
            "  FROM erp_stock_move_item smi",
            " INNER JOIN erp_stock_move sm ON sm.id = smi.move_id",
            "   AND sm.deleted = 0",
            "   AND (sm.transfer_direction = 10 OR sm.transfer_direction IS NULL)",
            " WHERE smi.deleted = 0",
            "   AND smi.source_sale_return_item_id IN",
            " <foreach collection='sourceSaleReturnItemIds' item='sourceSaleReturnItemId' open='(' separator=',' close=')'>",
            "   #{sourceSaleReturnItemId}",
            " </foreach>",
            " <if test='excludeMoveId != null'>",
            "   AND smi.move_id &lt;&gt; #{excludeMoveId}",
            " </if>",
            " GROUP BY smi.source_sale_return_item_id",
            "</script>"
    })
    List<Map<String, Object>> selectMovedCountListBySourceSaleReturnItemIds(
            @Param("sourceSaleReturnItemIds") Collection<Long> sourceSaleReturnItemIds,
            @Param("excludeMoveId") Long excludeMoveId);

}
