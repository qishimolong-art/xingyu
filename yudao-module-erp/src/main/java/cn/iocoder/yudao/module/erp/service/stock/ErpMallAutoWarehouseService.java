package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallAutoWarehouseResultBO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商城按目的地自动分仓服务。
 */
public interface ErpMallAutoWarehouseService {

    /**
     * 为每一个商城商品选择距离最近且可用量足够的销售仓。
     *
     * @param deptId 销售部门编号
     * @param longitude 目的地经度（GCJ-02）
     * @param latitude 目的地纬度（GCJ-02）
     * @param items 商品项
     * @return 分仓结果
     */
    ErpMallAutoWarehouseResultBO allocate(Long deptId, BigDecimal longitude, BigDecimal latitude,
                                          List<ErpMallAutoWarehouseResultBO.Item> items);

}
