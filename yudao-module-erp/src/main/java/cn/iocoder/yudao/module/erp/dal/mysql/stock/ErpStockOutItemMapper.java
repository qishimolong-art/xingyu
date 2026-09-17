package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 其它出库单项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockOutItemMapper extends BaseMapperX<ErpStockOutItemDO> {

    default List<ErpStockOutItemDO> selectListByOutId(Long outId) {
        return selectList(ErpStockOutItemDO::getOutId, outId);
    }

    default List<ErpStockOutItemDO> selectListByOutIdForUpdate(Long outId) {
        return selectList(new LambdaQueryWrapperX<ErpStockOutItemDO>()
                .eq(ErpStockOutItemDO::getOutId, outId).last("FOR UPDATE"));
    }

    default PageResult<ErpStockOutItemDO> selectPageByOutId(ErpStockOutItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpStockOutItemDO> query = new LambdaQueryWrapperX<ErpStockOutItemDO>()
                .eq(ErpStockOutItemDO::getOutId, reqVO.getOutId());
        SFunction<ErpStockOutItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpStockOutItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpStockOutItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpStockOutItemDO> selectListByOutIds(Collection<Long> outIds) {
        return selectList(ErpStockOutItemDO::getOutId, outIds);
    }

    default Map<Long, Map<String, Object>> selectSummaryMapByOutIds(Collection<Long> outIds) {
        if (CollUtil.isEmpty(outIds)) {
            return Collections.emptyMap();
        }
        return convertMap(selectSummaryRowsByOutIds(outIds), row -> toLong(row.get("outId")), row -> row);
    }

    @Select({
            "<script>",
            "SELECT soi.out_id AS outId,",
            "       COUNT(*) AS itemCount,",
            "       GROUP_CONCAT(DISTINCT p.name ORDER BY p.name SEPARATOR ', ') AS productNames,",
            "       GROUP_CONCAT(DISTINCT p.code ORDER BY p.code SEPARATOR ', ') AS productCodes,",
            "       GROUP_CONCAT(DISTINCT w.name ORDER BY w.name SEPARATOR ', ') AS warehouseNames",
            "  FROM erp_stock_out_item soi",
            "  LEFT JOIN erp_product p ON p.id = soi.product_id AND p.deleted = b'0'",
            "  LEFT JOIN erp_warehouse w ON w.id = soi.warehouse_id AND w.deleted = b'0'",
            " WHERE soi.deleted = b'0'",
            "   AND soi.out_id IN",
            " <foreach collection='outIds' item='outId' open='(' separator=',' close=')'>#{outId}</foreach>",
            " GROUP BY soi.out_id",
            "</script>"
    })
    List<Map<String, Object>> selectSummaryRowsByOutIds(@Param("outIds") Collection<Long> outIds);

    default int deleteByOutId(Long outId) {
        return delete(ErpStockOutItemDO::getOutId, outId);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpStockOutItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpStockOutItemDO::getWarehouseId, warehouseId);
    }

    default ErpStockOutItemDO selectFirstByWarehouseId(Long warehouseId) {
        return selectFirstOne(ErpStockOutItemDO::getWarehouseId, warehouseId);
    }

    static SFunction<ErpStockOutItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpStockOutItemDO::getId;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpStockOutItemDO::getProductId;
            case "warehouseId":
                return ErpStockOutItemDO::getWarehouseId;
            case "count":
                return ErpStockOutItemDO::getCount;
            case "productPrice":
                return ErpStockOutItemDO::getProductPrice;
            case "totalPrice":
                return ErpStockOutItemDO::getTotalPrice;
            case "weight":
                return ErpStockOutItemDO::getWeight;
            case "packageQty":
                return ErpStockOutItemDO::getPackageQty;
            case "totalWeight":
                return ErpStockOutItemDO::getTotalWeight;
            case "batchNo":
                return ErpStockOutItemDO::getBatchNo;
            case "remark":
                return ErpStockOutItemDO::getRemark;
            default:
                return null;
        }
    }

    static Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

}
