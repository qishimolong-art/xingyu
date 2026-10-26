package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckExportRespVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Export mapping and permission dependencies, isolated from business save/approval paths. */
final class ErpStockCheckExportSupport {
    private ErpStockCheckExportSupport() { }
    static final Map<String, String> GROUPS = new LinkedHashMap<>();
    private static final Map<String, List<String>> DEPENDENCIES = new LinkedHashMap<>();
    static {
        GROUPS.put("id", "main");
        DEPENDENCIES.put("id", Arrays.asList("id", "col_id"));
        GROUPS.put("no", "main");
        DEPENDENCIES.put("no", Arrays.asList("no", "col_no"));
        GROUPS.put("deptId", "main");
        DEPENDENCIES.put("deptId", Arrays.asList("deptId", "col_deptId"));
        GROUPS.put("deptName", "main");
        DEPENDENCIES.put("deptName", Arrays.asList("deptName", "col_deptName", "deptId", "col_deptId"));
        GROUPS.put("checkTime", "main");
        DEPENDENCIES.put("checkTime", Arrays.asList("checkTime", "col_checkTime"));
        GROUPS.put("checkType", "main");
        DEPENDENCIES.put("checkType", Arrays.asList("checkType", "col_checkType"));
        GROUPS.put("balanceType", "main");
        DEPENDENCIES.put("balanceType", Arrays.asList("balanceType", "col_balanceType", "totalCount", "col_totalCount", "totalPrice", "col_totalPrice", "item_count", "item_totalPrice"));
        GROUPS.put("totalCount", "main");
        DEPENDENCIES.put("totalCount", Arrays.asList("totalCount", "col_totalCount", "item_count"));
        GROUPS.put("totalPrice", "main");
        DEPENDENCIES.put("totalPrice", Arrays.asList("totalPrice", "col_totalPrice", "item_totalPrice"));
        GROUPS.put("totalWeight", "main");
        DEPENDENCIES.put("totalWeight", Arrays.asList("totalWeight", "col_totalWeight", "item_totalWeight"));
        GROUPS.put("status", "main");
        DEPENDENCIES.put("status", Arrays.asList("status", "col_status"));
        GROUPS.put("warehouseNames", "main");
        DEPENDENCIES.put("warehouseNames", Arrays.asList("warehouseNames", "col_warehouseNames", "item_warehouseId", "item_warehouseName"));
        GROUPS.put("itemCount", "main");
        DEPENDENCIES.put("itemCount", Arrays.asList("itemCount", "col_itemCount"));
        GROUPS.put("productNames", "main");
        DEPENDENCIES.put("productNames", Arrays.asList("productNames", "col_productNames", "item_productId", "item_productName"));
        GROUPS.put("productCodes", "main");
        DEPENDENCIES.put("productCodes", Arrays.asList("productCodes", "col_productCodes", "item_productId", "item_productCode"));
        GROUPS.put("remark", "main");
        DEPENDENCIES.put("remark", Arrays.asList("remark", "col_remark"));
        GROUPS.put("creatorName", "main");
        DEPENDENCIES.put("creatorName", Arrays.asList("creatorName", "col_creatorName", "creator", "col_creator"));
        GROUPS.put("createTime", "main");
        DEPENDENCIES.put("createTime", Arrays.asList("createTime", "col_createTime"));
        GROUPS.put("updaterName", "main");
        DEPENDENCIES.put("updaterName", Arrays.asList("updaterName", "col_updaterName", "updater", "col_updater"));
        GROUPS.put("updateTime", "main");
        DEPENDENCIES.put("updateTime", Arrays.asList("updateTime", "col_updateTime"));
        GROUPS.put("approveUserName", "main");
        DEPENDENCIES.put("approveUserName", Arrays.asList("approveUserName", "col_approveUserName", "updater", "col_updater", "col_updaterName"));
        GROUPS.put("approveTime", "main");
        DEPENDENCIES.put("approveTime", Arrays.asList("approveTime", "col_approveTime", "updateTime", "col_updateTime"));
        GROUPS.put("printCount", "main");
        DEPENDENCIES.put("printCount", Arrays.asList("printCount", "col_printCount"));
        GROUPS.put("printTime", "main");
        DEPENDENCIES.put("printTime", Arrays.asList("printTime", "col_printTime"));
        GROUPS.put("detailWarehouseName", "detail");
        DEPENDENCIES.put("detailWarehouseName", Arrays.asList("detailWarehouseName", "col_detailWarehouseName", "item_warehouseName", "item_warehouseId"));
        GROUPS.put("detailWarehouseDeptName", "detail");
        DEPENDENCIES.put("detailWarehouseDeptName", Arrays.asList("detailWarehouseDeptName", "col_detailWarehouseDeptName", "item_warehouseDeptName", "item_warehouseId"));
        GROUPS.put("detailProductCode", "detail");
        DEPENDENCIES.put("detailProductCode", Arrays.asList("detailProductCode", "col_detailProductCode", "item_productCode", "item_productId"));
        GROUPS.put("detailProductName", "detail");
        DEPENDENCIES.put("detailProductName", Arrays.asList("detailProductName", "col_detailProductName", "item_productName", "item_productId"));
        GROUPS.put("detailProductBarCode", "detail");
        DEPENDENCIES.put("detailProductBarCode", Arrays.asList("detailProductBarCode", "col_detailProductBarCode", "item_productBarCode", "item_productId"));
        GROUPS.put("detailProductUnitName", "detail");
        DEPENDENCIES.put("detailProductUnitName", Arrays.asList("detailProductUnitName", "col_detailProductUnitName", "item_productUnitName", "item_productUnitId"));
        GROUPS.put("detailBatchNo", "detail");
        DEPENDENCIES.put("detailBatchNo", Arrays.asList("detailBatchNo", "col_detailBatchNo", "item_batchNo"));
        GROUPS.put("detailPackageQty", "detail");
        DEPENDENCIES.put("detailPackageQty", Arrays.asList("detailPackageQty", "col_detailPackageQty", "item_packageQty"));
        GROUPS.put("detailPieceCount", "detail");
        DEPENDENCIES.put("detailPieceCount", Arrays.asList("detailPieceCount", "col_detailPieceCount", "item_pieceCount", "item_count", "item_packageQty"));
        GROUPS.put("detailWeight", "detail");
        DEPENDENCIES.put("detailWeight", Arrays.asList("detailWeight", "col_detailWeight", "item_weight"));
        GROUPS.put("detailTotalWeight", "detail");
        DEPENDENCIES.put("detailTotalWeight", Arrays.asList("detailTotalWeight", "col_detailTotalWeight", "item_totalWeight"));
        GROUPS.put("detailStockCount", "detail");
        DEPENDENCIES.put("detailStockCount", Arrays.asList("detailStockCount", "col_detailStockCount", "item_stockCount"));
        GROUPS.put("detailActualCount", "detail");
        DEPENDENCIES.put("detailActualCount", Arrays.asList("detailActualCount", "col_detailActualCount", "item_actualCount"));
        GROUPS.put("detailCount", "detail");
        DEPENDENCIES.put("detailCount", Arrays.asList("detailCount", "col_detailCount", "item_count"));
        GROUPS.put("detailProductPrice", "detail");
        DEPENDENCIES.put("detailProductPrice", Arrays.asList("detailProductPrice", "col_detailProductPrice", "item_productPrice"));
        GROUPS.put("detailTotalPrice", "detail");
        DEPENDENCIES.put("detailTotalPrice", Arrays.asList("detailTotalPrice", "col_detailTotalPrice", "item_totalPrice"));
        GROUPS.put("detailRemark", "detail");
        DEPENDENCIES.put("detailRemark", Arrays.asList("detailRemark", "col_detailRemark", "item_remark"));
    }
    static Set<String> hiddenFields(Collection<String> hidden) {
        Set<String> result = hidden == null ? new HashSet<>() : new HashSet<>(hidden);
        DEPENDENCIES.forEach((field, dependencies) -> {
            if (dependencies.stream().anyMatch(result::contains)) result.add(field);
        });
        return result;
    }
    static boolean hasDetail(Set<String> selected) {
        return selected == null || selected.stream().anyMatch(field -> "detail".equals(GROUPS.get(field)));
    }
    static ErpStockCheckExportRespVO row(ErpStockCheckRespVO main, ErpStockCheckRespVO.Item item) {
        ErpStockCheckExportRespVO row = new ErpStockCheckExportRespVO();
        row.setId(main.getId());
        row.setNo(main.getNo());
        row.setDeptId(main.getDeptId());
        row.setDeptName(main.getDeptName());
        row.setCheckTime(main.getCheckTime());
        row.setTotalCount(main.getTotalCount());
        row.setTotalPrice(main.getTotalPrice());
        row.setTotalWeight(main.getTotalWeight());
        row.setWarehouseNames(main.getWarehouseNames());
        row.setItemCount(main.getItemCount());
        row.setProductNames(main.getProductNames());
        row.setProductCodes(main.getProductCodes());
        row.setRemark(main.getRemark());
        row.setCreatorName(main.getCreatorName());
        row.setCreateTime(main.getCreateTime());
        row.setUpdaterName(main.getUpdaterName());
        row.setUpdateTime(main.getUpdateTime());
        row.setPrintCount(main.getPrintCount());
        row.setPrintTime(main.getPrintTime());
        row.setCheckType(Objects.equals(main.getCheckType(), 2) ? "盘成本" : "盘数量");
        row.setStatus(Objects.equals(main.getStatus(), 0) ? "草稿" : Objects.equals(main.getStatus(), 20) ? "已审核" : "待审核");
        BigDecimal count = main.getTotalCount() == null ? BigDecimal.ZERO : main.getTotalCount();
        BigDecimal amount = main.getTotalPrice() == null ? BigDecimal.ZERO : main.getTotalPrice();
        row.setBalanceType(count.signum() > 0 ? "盘盈" : count.signum() < 0 ? "盘亏" : amount.signum() == 0 ? "无差异" : "成本调整");
        if (Objects.equals(main.getStatus(), 20)) {
            row.setApproveUserName(main.getUpdaterName());
            row.setApproveTime(main.getUpdateTime());
        }
        if (item == null) return row;
        row.setDetailWarehouseName(item.getWarehouseName());
        row.setDetailWarehouseDeptName(item.getWarehouseDeptName());
        row.setDetailProductCode(item.getProductCode());
        row.setDetailProductName(item.getProductName());
        row.setDetailProductBarCode(item.getProductBarCode());
        row.setDetailProductUnitName(item.getProductUnitName());
        row.setDetailBatchNo(item.getBatchNo());
        row.setDetailPackageQty(item.getPackageQty());
        row.setDetailWeight(item.getWeight());
        row.setDetailTotalWeight(item.getTotalWeight());
        row.setDetailStockCount(item.getStockCount());
        row.setDetailActualCount(item.getActualCount());
        row.setDetailCount(item.getCount());
        row.setDetailProductPrice(item.getProductPrice());
        row.setDetailTotalPrice(item.getTotalPrice());
        row.setDetailRemark(item.getRemark());
        if (item.getCount() != null && item.getPackageQty() != null && item.getPackageQty() > 0) {
            row.setDetailPieceCount(item.getCount().divide(BigDecimal.valueOf(item.getPackageQty()), 3, RoundingMode.HALF_UP));
        }
        return row;
    }
}
