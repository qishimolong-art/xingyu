package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderExportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.*;
import java.util.*;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Component
public class ErpPurchaseOrderExportAssembler {
    @Resource private ErpProductService productService;
    @Resource private ErpSupplierService supplierService;
    @Resource private ErpWarehouseService warehouseService;
    @Resource private ErpStockService stockService;
    @Resource private DeptApi deptApi;
    @Resource private AdminUserApi adminUserApi;

    public List<ErpPurchaseOrderExportRespVO> buildRows(ErpPurchaseOrderDO order,
            List<ErpPurchaseOrderItemDO> items, boolean fillMainOnFirstRow) {
        Map<Long, ErpProductRespVO> products = items.isEmpty() ? Collections.emptyMap() : DataPermissionUtils.executeIgnore(
                () -> productService.getProductVOMap(convertSet(items, ErpPurchaseOrderItemDO::getProductId)));
        Set<Long> warehouseIds = convertSet(items, ErpPurchaseOrderItemDO::getWarehouseId);
        Map<Long, ErpWarehouseDO> warehouses = warehouseIds.isEmpty() ? Collections.emptyMap() : DataPermissionUtils.executeIgnore(
                () -> warehouseService.getWarehouseMap(warehouseIds));
        Set<Long> deptIds = new HashSet<>(convertSet(items, ErpPurchaseOrderItemDO::getDeptId));
        if (order.getDeptId() != null) deptIds.add(order.getDeptId());
        Map<Long, DeptRespDTO> depts = deptIds.isEmpty() ? Collections.emptyMap() : DataPermissionUtils.executeIgnore(() -> deptApi.getDeptMap(deptIds));
        Set<Long> productIds = convertSet(items, ErpPurchaseOrderItemDO::getProductId);
        Map<Long, BigDecimal> stocks = productIds.isEmpty() ? Collections.emptyMap() : DataPermissionUtils.executeIgnore(
                () -> stockService.getStockCountMap(productIds));
        ErpSupplierDO supplier = order.getSupplierId() == null ? null : supplierService.getSupplier(order.getSupplierId());
        Set<Long> userIds = new HashSet<>(); Long creatorId = parseLong(order.getCreator());
        if (creatorId != null) userIds.add(creatorId); if (order.getPurchaser() != null) userIds.add(order.getPurchaser());
        Map<Long, AdminUserRespDTO> users = userIds.isEmpty() ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        if (items.isEmpty()) return Collections.singletonList(build(order, supplier, users.get(creatorId),
                users.get(order.getPurchaser()), depts.get(order.getDeptId()), null, null, null, null, null, fillMainOnFirstRow));
        List<ErpPurchaseOrderExportRespVO> rows = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            ErpPurchaseOrderItemDO item = items.get(i);
            rows.add(build(order, supplier, users.get(creatorId), users.get(order.getPurchaser()), depts.get(order.getDeptId()),
                    item, products.get(item.getProductId()), warehouses.get(item.getWarehouseId()), depts.get(item.getDeptId()),
                    stocks.get(item.getProductId()), fillMainOnFirstRow && i == 0));
        }
        return rows;
    }

    private ErpPurchaseOrderExportRespVO build(ErpPurchaseOrderDO order, ErpSupplierDO supplier,
            AdminUserRespDTO creator, AdminUserRespDTO purchaser, DeptRespDTO orderDept,
            ErpPurchaseOrderItemDO item, ErpProductRespVO product, ErpWarehouseDO warehouse,
            DeptRespDTO itemDept, BigDecimal stockCount, boolean main) {
        ErpPurchaseOrderExportRespVO row = main ? BeanUtils.toBean(order, ErpPurchaseOrderExportRespVO.class)
                : new ErpPurchaseOrderExportRespVO();
        row.setSupplierName(main && supplier != null ? supplier.getName() : null);
        row.setCreatorName(main && creator != null ? creator.getNickname() : null);
        row.setPurchaserName(main && purchaser != null ? purchaser.getNickname() : null);
        row.setDeptName(main && orderDept != null ? orderDept.getName() : null);
        if (item == null) return row;
        row.setProductCode(product == null ? null : product.getCode()); row.setProductName(product == null ? null : product.getName());
        row.setProductUnitName(product == null ? null : product.getUnitName()); row.setWarehouseName(warehouse == null ? null : warehouse.getName());
        row.setItemDeptName(itemDept == null ? null : itemDept.getName()); row.setStockCount(stockCount == null ? BigDecimal.ZERO : stockCount);
        row.setWeight(product == null ? null : product.getWeight()); row.setPackageQty(product == null ? null : product.getPackageQty());
        row.setItemCount(item.getCount()); row.setProductPrice(item.getProductPrice());
        row.setLastPurchasePrice(product == null ? null : product.getLastPurchasePrice()); row.setGift(Boolean.TRUE.equals(item.getGift()) ? "是" : "否");
        row.setItemTotalPrice(item.getProductPrice() == null || item.getCount() == null ? null : item.getProductPrice().multiply(item.getCount()));
        row.setItemTaxPercent(item.getTaxPercent()); row.setItemTaxPrice(item.getTaxPrice());
        row.setVehicleModel(first(item.getVehicleModel(), product == null ? null : product.getVehicleModel()));
        row.setStandard(first(item.getStandard(), product == null ? null : product.getStandard()));
        row.setFeatureCode(first(item.getFeatureCode(), product == null ? null : product.getFeatureCode()));
        row.setPieceCount(piece(item.getCount(), product == null ? null : product.getPackageQty())); row.setArrivalCount(item.getArrivalCount());
        row.setWarehousePosition(item.getWarehousePosition()); row.setDrawingNo(first(item.getDrawingNo(), product == null ? null : product.getDrawingNo()));
        row.setBatchNo(item.getBatchNo()); row.setFactoryCode(first(item.getFactoryCode(), product == null ? null : product.getFactoryCode()));
        row.setBrand(first(item.getBrand(), product == null ? null : product.getBrand()));
        row.setOriginPlace(first(item.getOriginPlace(), product == null ? null : product.getOriginPlace()));
        row.setItemInCount(item.getInCount()); row.setItemReturnCount(item.getReturnCount()); row.setItemRemark(item.getRemark());
        row.setItemInStatus(status(item.getInCount(), item.getCount())); return row;
    }
    private String status(BigDecimal done, BigDecimal total) { if (done == null || done.signum() <= 0) return "未入库";
        return total != null && done.compareTo(total) >= 0 ? "全部入库" : "部分入库"; }
    private BigDecimal piece(BigDecimal count, Integer qty) { return count == null || qty == null || qty <= 0 ? null
            : count.divide(BigDecimal.valueOf(qty), 3, RoundingMode.HALF_UP).stripTrailingZeros(); }
    private <T> T first(T a, T b) { return a == null ? b : a; }
    private Long parseLong(String value) { try { return value == null ? null : Long.valueOf(value); } catch (NumberFormatException ex) { return null; } }
}
