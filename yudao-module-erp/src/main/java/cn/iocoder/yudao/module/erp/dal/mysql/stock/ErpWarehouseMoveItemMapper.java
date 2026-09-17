package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

@Mapper
public interface ErpWarehouseMoveItemMapper extends BaseMapperX<ErpWarehouseMoveItemDO> {

    default List<ErpWarehouseMoveItemDO> selectListByMoveId(Long moveId) {
        return selectList(ErpWarehouseMoveItemDO::getMoveId, moveId);
    }

    default List<ErpWarehouseMoveItemDO> selectListByMoveIdForUpdate(Long moveId) {
        return selectList(new LambdaQueryWrapperX<ErpWarehouseMoveItemDO>()
                .eq(ErpWarehouseMoveItemDO::getMoveId, moveId).last("FOR UPDATE"));
    }

    default PageResult<ErpWarehouseMoveItemDO> selectPageByMoveId(ErpWarehouseMoveItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpWarehouseMoveItemDO> query = new LambdaQueryWrapperX<ErpWarehouseMoveItemDO>()
                .eq(ErpWarehouseMoveItemDO::getMoveId, reqVO.getMoveId());
        SFunction<ErpWarehouseMoveItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpWarehouseMoveItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpWarehouseMoveItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpWarehouseMoveItemDO> selectListByMoveIds(Collection<Long> moveIds) {
        if (moveIds == null || moveIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(ErpWarehouseMoveItemDO::getMoveId, moveIds);
    }

    default Map<Long, Map<String, Object>> selectSummaryMapByMoveIds(Collection<Long> moveIds) {
        if (CollUtil.isEmpty(moveIds)) {
            return Collections.emptyMap();
        }
        return convertMap(selectSummaryRowsByMoveIds(moveIds), row -> toLong(row.get("moveId")), row -> row);
    }

    @Select({
            "<script>",
            "SELECT wmi.move_id AS moveId,",
            "       COUNT(*) AS itemCount,",
            "       GROUP_CONCAT(DISTINCT p.name ORDER BY p.name SEPARATOR ', ') AS productNames,",
            "       GROUP_CONCAT(DISTINCT p.code ORDER BY p.code SEPARATOR ', ') AS productCodes,",
            "       GROUP_CONCAT(DISTINCT fw.name ORDER BY fw.name SEPARATOR '、') AS fromWarehouseNames,",
            "       GROUP_CONCAT(DISTINCT tw.name ORDER BY tw.name SEPARATOR '、') AS toWarehouseNames",
            "  FROM erp_warehouse_move_item wmi",
            "  LEFT JOIN erp_product p ON p.id = wmi.product_id AND p.deleted = b'0'",
            "  LEFT JOIN erp_warehouse fw ON fw.id = wmi.from_warehouse_id AND fw.deleted = b'0'",
            "  LEFT JOIN erp_warehouse tw ON tw.id = wmi.to_warehouse_id AND tw.deleted = b'0'",
            " WHERE wmi.deleted = b'0'",
            "   AND wmi.move_id IN",
            " <foreach collection='moveIds' item='moveId' open='(' separator=',' close=')'>#{moveId}</foreach>",
            " GROUP BY wmi.move_id",
            "</script>"
    })
    List<Map<String, Object>> selectSummaryRowsByMoveIds(@Param("moveIds") Collection<Long> moveIds);

    default int deleteByMoveId(Long moveId) {
        return delete(ErpWarehouseMoveItemDO::getMoveId, moveId);
    }

    static SFunction<ErpWarehouseMoveItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpWarehouseMoveItemDO::getId;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpWarehouseMoveItemDO::getProductId;
            case "fromWarehouseId":
                return ErpWarehouseMoveItemDO::getFromWarehouseId;
            case "toWarehouseId":
                return ErpWarehouseMoveItemDO::getToWarehouseId;
            case "count":
                return ErpWarehouseMoveItemDO::getCount;
            case "productPrice":
                return ErpWarehouseMoveItemDO::getProductPrice;
            case "totalPrice":
                return ErpWarehouseMoveItemDO::getTotalPrice;
            case "costPrice":
                return ErpWarehouseMoveItemDO::getCostPrice;
            case "costAmount":
                return ErpWarehouseMoveItemDO::getCostAmount;
            case "weight":
                return ErpWarehouseMoveItemDO::getWeight;
            case "packageQty":
                return ErpWarehouseMoveItemDO::getPackageQty;
            case "totalWeight":
                return ErpWarehouseMoveItemDO::getTotalWeight;
            case "batchNo":
                return ErpWarehouseMoveItemDO::getBatchNo;
            case "fromShelf":
                return ErpWarehouseMoveItemDO::getFromShelf;
            case "toShelf":
                return ErpWarehouseMoveItemDO::getToShelf;
            case "remark":
                return ErpWarehouseMoveItemDO::getRemark;
            default:
                return null;
        }
    }

    static Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

}
