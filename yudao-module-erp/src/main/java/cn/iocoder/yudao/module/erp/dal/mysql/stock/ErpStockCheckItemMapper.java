package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPendingInDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * ERP 库存盘点单项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockCheckItemMapper extends BaseMapperX<ErpStockCheckItemDO> {

    default List<ErpStockCheckItemDO> selectListByCheckId(Long checkId) {
        return selectList(ErpStockCheckItemDO::getCheckId, checkId);
    }

    default List<ErpStockCheckItemDO> selectListByCheckIds(Collection<Long> checkIds) {
        return selectList(ErpStockCheckItemDO::getCheckId, checkIds);
    }

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
