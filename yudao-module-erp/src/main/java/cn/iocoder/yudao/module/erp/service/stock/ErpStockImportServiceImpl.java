package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockCheckTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ErpStockImportServiceImpl implements ErpStockImportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private ErpStockInService stockInService;
    @Resource
    private ErpStockOutService stockOutService;
    @Resource
    private ErpStockMoveService stockMoveService;
    @Resource
    private ErpStockCheckService stockCheckService;
    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpWarehouseMapper warehouseMapper;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private DeptApi deptApi;

    @Override
    public ErpStockImportResultRespVO importStockInList(List<ErpStockImportExcelVO> list) {
        return importGroups(list, "其它入库", group -> {
            ErpStockImportExcelVO mainRow = group.getMainRow();
            ErpStockInSaveReqVO reqVO = new ErpStockInSaveReqVO();
            reqVO.setSupplierId(resolveSupplierId(mainRow.getSupplierName()));
            reqVO.setInTime(parseBizTime(mainRow.getBizTime()));
            reqVO.setRemark(trimToNull(mainRow.getRemark()));
            List<ErpStockInSaveReqVO.Item> items = new ArrayList<>();
            for (ErpStockImportExcelVO row : group.getRows()) {
                ErpStockInSaveReqVO.Item item = new ErpStockInSaveReqVO.Item();
                item.setWarehouseId(resolveWarehouse(row.getWarehouseName(), "仓库名称").getId());
                item.setProductId(resolveProductId(row.getProductCode()));
                item.setCount(requirePositive(row.getCount(), "数量"));
                item.setProductPrice(defaultZero(row.getProductPrice()));
                item.setRemark(trimToNull(row.getItemRemark()));
                items.add(item);
            }
            reqVO.setItems(items);
            stockInService.createStockIn(reqVO);
        });
    }

    @Override
    public ErpStockImportResultRespVO importStockOutList(List<ErpStockImportExcelVO> list) {
        return importGroups(list, "其它出库", group -> {
            ErpStockImportExcelVO mainRow = group.getMainRow();
            ErpStockOutSaveReqVO reqVO = new ErpStockOutSaveReqVO();
            reqVO.setCustomerId(resolveCustomerId(mainRow.getCustomerName()));
            reqVO.setOutTime(parseBizTime(mainRow.getBizTime()));
            reqVO.setRemark(trimToNull(mainRow.getRemark()));
            List<ErpStockOutSaveReqVO.Item> items = new ArrayList<>();
            for (ErpStockImportExcelVO row : group.getRows()) {
                ErpStockOutSaveReqVO.Item item = new ErpStockOutSaveReqVO.Item();
                item.setWarehouseId(resolveWarehouse(row.getWarehouseName(), "仓库名称").getId());
                item.setProductId(resolveProductId(row.getProductCode()));
                item.setCount(requirePositive(row.getCount(), "数量"));
                item.setProductPrice(defaultZero(row.getProductPrice()));
                item.setRemark(trimToNull(row.getItemRemark()));
                items.add(item);
            }
            reqVO.setItems(items);
            stockOutService.createStockOut(reqVO);
        });
    }

    @Override
    public ErpStockImportResultRespVO importStockMoveList(List<ErpStockImportExcelVO> list) {
        return importStockMoveGroups(list, "库存调拨", false);
    }

    @Override
    public ErpStockImportResultRespVO importStockTransferOutList(List<ErpStockImportExcelVO> list) {
        return importStockMoveGroups(list, "调拨出库单", true);
    }

    private ErpStockImportResultRespVO importStockMoveGroups(List<ErpStockImportExcelVO> list, String moduleName,
                                                            boolean requireTransferOutFields) {
        return importGroups(list, moduleName, group -> {
            ErpStockImportExcelVO mainRow = group.getMainRow();
            ErpStockMoveSaveReqVO reqVO = new ErpStockMoveSaveReqVO();
            reqVO.setMoveTime(parseBizTimeOrNow(mainRow.getBizTime()));
            reqVO.setRemark(trimToNull(mainRow.getRemark()));
            List<ErpStockMoveSaveReqVO.Item> items = new ArrayList<>();
            for (ErpStockImportExcelVO row : group.getRows()) {
                ErpWarehouseDO fromWarehouse = resolveWarehouse(row.getFromWarehouseName(), "调出仓库名称");
                ErpWarehouseDO toWarehouse = resolveWarehouse(row.getToWarehouseName(), "调入仓库名称");
                if (requireTransferOutFields) {
                    validateToDept(row.getToDeptName(), toWarehouse);
                }
                ErpStockMoveSaveReqVO.Item item = new ErpStockMoveSaveReqVO.Item();
                item.setFromWarehouseId(fromWarehouse.getId());
                item.setToWarehouseId(toWarehouse.getId());
                item.setProductId(resolveProductId(row.getProductCode()));
                item.setCount(requirePositive(row.getCount(), "数量"));
                item.setProductPrice(requireTransferOutFields
                        ? requirePositive(row.getProductPrice(), "价格")
                        : defaultZero(row.getProductPrice()));
                item.setRemark(trimToNull(row.getItemRemark()));
                items.add(item);
            }
            reqVO.setItems(items);
            stockMoveService.createStockMove(reqVO);
        });
    }

    @Override
    public ErpStockImportResultRespVO importStockCheckList(List<ErpStockImportExcelVO> list) {
        return importGroups(list, "库存盘点", group -> {
            ErpStockImportExcelVO mainRow = group.getMainRow();
            ErpStockCheckSaveReqVO reqVO = new ErpStockCheckSaveReqVO();
            reqVO.setCheckTime(parseBizTime(mainRow.getBizTime()));
            Integer checkType = resolveCheckType(mainRow.getCheckTypeName());
            reqVO.setCheckType(checkType);
            reqVO.setRemark(trimToNull(mainRow.getRemark()));
            List<ErpStockCheckSaveReqVO.Item> items = new ArrayList<>();
            for (ErpStockImportExcelVO row : group.getRows()) {
                BigDecimal stockCount = requireNotNull(row.getStockCount(), "账面库存");
                BigDecimal actualCount = ErpStockCheckTypeEnum.isCost(checkType) ? stockCount : requireNotNull(row.getActualCount(), "实际库存");
                BigDecimal productPrice = requireNotNull(row.getProductPrice(), "单价");
                ErpStockCheckSaveReqVO.Item item = new ErpStockCheckSaveReqVO.Item();
                item.setWarehouseId(resolveWarehouse(row.getWarehouseName(), "仓库名称").getId());
                item.setProductId(resolveProductId(row.getProductCode()));
                item.setProductPrice(productPrice);
                item.setStockCount(stockCount);
                item.setActualCount(actualCount);
                item.setCount(actualCount.subtract(stockCount));
                if (ErpStockCheckTypeEnum.isCost(checkType)) {
                    item.setTotalPrice(MoneyUtils.priceMultiply(productPrice, stockCount));
                }
                item.setRemark(trimToNull(row.getItemRemark()));
                items.add(item);
            }
            reqVO.setItems(items);
            stockCheckService.createStockCheck(reqVO);
        });
    }

    private ErpStockImportResultRespVO importGroups(List<ErpStockImportExcelVO> list, String moduleName,
                                                   GroupImporter importer) {
        ErpStockImportResultRespVO result = new ErpStockImportResultRespVO();
        if (CollUtil.isEmpty(list)) {
            return result;
        }
        List<ImportGroup> groups = buildGroups(list, result);
        for (ImportGroup group : groups) {
            try {
                importer.importGroup(group);
                result.addSuccess();
            } catch (Exception ex) {
                result.addFailure(group.getRowNo(), group.getOrderNo(), group.getFirstProductCode(),
                        moduleName + "导入失败：" + resolveFailureReason(ex));
            }
        }
        return result;
    }

    private List<ImportGroup> buildGroups(List<ErpStockImportExcelVO> list, ErpStockImportResultRespVO result) {
        List<ImportGroup> groups = new ArrayList<>();
        ImportGroup currentBlankNoGroup = null;
        for (int i = 0; i < list.size(); i++) {
            ErpStockImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (isBlankRow(row)) {
                continue;
            }
            String orderNo = trimToNull(row.getOrderNo());
            ImportGroup group;
            if (orderNo != null) {
                group = findOrCreateGroup(groups, orderNo, rowNo, row);
                currentBlankNoGroup = null;
            } else if (hasMainFields(row) || currentBlankNoGroup == null) {
                group = new ImportGroup(null, rowNo, row);
                groups.add(group);
                currentBlankNoGroup = group;
            } else {
                group = currentBlankNoGroup;
            }
            group.addRow(row);
        }
        return groups;
    }

    private ImportGroup findOrCreateGroup(List<ImportGroup> groups, String orderNo, Integer rowNo,
                                          ErpStockImportExcelVO row) {
        for (ImportGroup group : groups) {
            if (orderNo.equals(group.getOrderNo())) {
                return group;
            }
        }
        ImportGroup group = new ImportGroup(orderNo, rowNo, row);
        groups.add(group);
        return group;
    }

    private boolean isBlankRow(ErpStockImportExcelVO row) {
        return row == null || StrUtil.isAllBlank(row.getOrderNo(), row.getSupplierName(), row.getCustomerName(),
                row.getBizTime(), row.getWarehouseName(), row.getFromWarehouseName(), row.getToWarehouseName(),
                row.getToDeptName(), row.getProductCode(), row.getCheckTypeName(), row.getRemark(), row.getItemRemark())
                && row.getCount() == null && row.getProductPrice() == null
                && row.getStockCount() == null && row.getActualCount() == null;
    }

    private boolean hasMainFields(ErpStockImportExcelVO row) {
        return StrUtil.isNotBlank(trimToNull(row.getBizTime()))
                || StrUtil.isNotBlank(trimToNull(row.getSupplierName()))
                || StrUtil.isNotBlank(trimToNull(row.getCustomerName()))
                || StrUtil.isNotBlank(trimToNull(row.getCheckTypeName()))
                || StrUtil.isNotBlank(trimToNull(row.getRemark()));
    }

    private Integer resolveCheckType(String checkTypeName) {
        String value = trimToNull(checkTypeName);
        if (value == null || "盘数量".equals(value) || "1".equals(value)) {
            return ErpStockCheckTypeEnum.COUNT.getType();
        }
        if ("盘成本".equals(value) || "2".equals(value)) {
            return ErpStockCheckTypeEnum.COST.getType();
        }
        throw new IllegalArgumentException("盘点类型只能填写盘数量或盘成本");
    }

    private Long resolveProductId(String productCode) {
        String code = trimToNull(productCode);
        if (code == null) {
            throw new IllegalArgumentException("产品编码不能为空");
        }
        ErpProductDO product = productMapper.selectByCode(code);
        if (product == null || Boolean.TRUE.equals(product.getMergedFlag())) {
            throw new IllegalArgumentException("产品不存在：" + code);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(product.getStatus())) {
            throw new IllegalArgumentException("产品未启用：" + code);
        }
        return product.getId();
    }

    private ErpWarehouseDO resolveWarehouse(String warehouseName, String label) {
        String name = trimToNull(warehouseName);
        if (name == null) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        ErpWarehouseDO warehouse = warehouseMapper.selectByName(name);
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在：" + name);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(warehouse.getStatus())) {
            throw new IllegalArgumentException("仓库未启用：" + name);
        }
        return warehouse;
    }

    private void validateToDept(String deptName, ErpWarehouseDO toWarehouse) {
        String name = trimToNull(deptName);
        if (name == null) {
            throw new IllegalArgumentException("调入部门不能为空");
        }
        List<DeptRespDTO> depts = deptApi.getDeptListByName(name);
        DeptRespDTO dept = depts.stream().filter(item -> name.equals(item.getName())).findFirst().orElse(null);
        if (dept == null) {
            throw new IllegalArgumentException("调入部门不存在：" + name);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus())) {
            throw new IllegalArgumentException("调入部门未启用：" + name);
        }
        if (toWarehouse.getDeptId() != null && !Objects.equals(toWarehouse.getDeptId(), dept.getId())) {
            throw new IllegalArgumentException("调入仓库不属于调入部门：" + toWarehouse.getName());
        }
    }

    private Long resolveSupplierId(String supplierName) {
        String name = trimToNull(supplierName);
        if (name == null) {
            return null;
        }
        List<ErpSupplierDO> suppliers = supplierService.getSupplierListByNameLike(name);
        ErpSupplierDO supplier = suppliers.stream().filter(item -> name.equals(item.getName())).findFirst()
                .orElse(null);
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在：" + name);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(supplier.getStatus())) {
            throw new IllegalArgumentException("供应商未启用：" + name);
        }
        return supplier.getId();
    }

    private Long resolveCustomerId(String customerName) {
        String name = trimToNull(customerName);
        if (name == null) {
            return null;
        }
        List<ErpCustomerDO> customers = customerService.getCustomerListByNameLike(name);
        ErpCustomerDO customer = customers.stream().filter(item -> name.equals(item.getName())).findFirst()
                .orElse(null);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在：" + name);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(customer.getStatus())) {
            throw new IllegalArgumentException("客户未启用：" + name);
        }
        return customer.getId();
    }

    private LocalDateTime parseBizTimeOrNow(String value) {
        String time = trimToNull(value);
        if (time == null) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(time, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // try next format
        }
        try {
            return LocalDateTime.parse(time, DATE_TIME_MINUTE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // try next format
        }
        try {
            return LocalDate.parse(time, DATE_FORMATTER).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            throw new IllegalArgumentException("业务时间格式不正确，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    private LocalDateTime parseBizTime(String value) {
        String time = trimToNull(value);
        if (time == null) {
            throw new IllegalArgumentException("业务时间不能为空");
        }
        try {
            return LocalDateTime.parse(time, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // try next format
        }
        try {
            return LocalDateTime.parse(time, DATE_TIME_MINUTE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // try next format
        }
        try {
            return LocalDate.parse(time, DATE_FORMATTER).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            throw new IllegalArgumentException("业务时间格式不正确，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    private BigDecimal requirePositive(BigDecimal value, String label) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(label + "必须大于 0");
        }
        return value;
    }

    private BigDecimal requireNotNull(BigDecimal value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private String resolveFailureReason(Exception ex) {
        if (ex.getMessage() == null || ex.getMessage().trim().isEmpty()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getMessage();
    }

    @FunctionalInterface
    private interface GroupImporter {
        void importGroup(ImportGroup group);
    }

    private static class ImportGroup {

        private final String orderNo;
        private final Integer rowNo;
        private final ErpStockImportExcelVO mainRow;
        private final List<ErpStockImportExcelVO> rows = new ArrayList<>();

        ImportGroup(String orderNo, Integer rowNo, ErpStockImportExcelVO mainRow) {
            this.orderNo = orderNo;
            this.rowNo = rowNo;
            this.mainRow = mainRow;
        }

        void addRow(ErpStockImportExcelVO row) {
            rows.add(row);
        }

        String getOrderNo() {
            return orderNo;
        }

        Integer getRowNo() {
            return rowNo;
        }

        ErpStockImportExcelVO getMainRow() {
            return mainRow;
        }

        List<ErpStockImportExcelVO> getRows() {
            return rows;
        }

        String getFirstProductCode() {
            return rows.isEmpty() ? null : rows.get(0).getProductCode();
        }

    }

}

