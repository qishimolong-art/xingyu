package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockProfitLossDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockProfitLossDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockProfitLossDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 配件盈亏逐明细报表服务。 */
@Service
public class ErpStockProfitLossDetailReportService {

    private static final String STOCK_CHECK_MODULE = "erp_stock_check";
    private static final String PRODUCT_MODULE = "erp_product";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;

    @Resource
    private ErpStockProfitLossDetailReportRepository repository;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private PermissionApi permissionApi;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validate(filter);
        Set<String> hiddenFields = reportHiddenFields();
        stripHiddenFilters(filter, hiddenFields);
        ErpStockProfitLossDetailReportRepository.Scope scope = resolveScope();
        long total = repository.count(filter, scope);
        if (total == 0) {
            return PageResult.empty();
        }
        List<Row> rows = repository.page(filter, scope,
                (long) (filter.getPageNo() - 1) * filter.getPageSize(), filter.getPageSize());
        finishRows(rows, hiddenFields);
        return new PageResult<>(rows, total);
    }

    @Transactional(readOnly = true)
    public Summary getSummary(Filter filter) {
        validate(filter);
        Set<String> hiddenFields = reportHiddenFields();
        stripHiddenFilters(filter, hiddenFields);
        Summary summary = repository.summary(filter, resolveScope());
        if (summary == null) {
            summary = new Summary().setRowCount(0L).setProfitLossCount(BigDecimal.ZERO)
                    .setProfitLossAmount(BigDecimal.ZERO);
        }
        boolean priceVisible = !hiddenFields.contains("costUnitPrice")
                && !hiddenFields.contains("adjustedUnitPrice")
                && !hiddenFields.contains("profitLossAmount");
        summary.setPriceVisible(priceVisible).setHiddenFields(hiddenFields);
        if (hiddenFields.contains("profitLossCount")) {
            summary.setProfitLossCount(null);
        }
        if (!priceVisible) {
            summary.setProfitLossAmount(null);
        }
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validate(filter);
        Set<String> hiddenFields = reportHiddenFields();
        stripHiddenFilters(filter, hiddenFields);
        ErpStockProfitLossDetailReportRepository.Scope scope = resolveScope();
        long total = repository.count(filter, scope);
        if (total > EXPORT_LIMIT) {
            throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("配件盈亏明细表.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(hiddenFields)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("配件盈亏明细").build();
            if (total == 0) {
                writer.write(Collections.emptyList(), sheet);
            }
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                finishRows(rows, hiddenFields);
                writer.write(rows, sheet);
            }
        }
    }

    ErpStockProfitLossDetailReportRepository.Scope resolveScope() {
        ErpProductStockPermissionScope scope = warehouseService.getCurrentUserProductStockPermissionScope();
        return new ErpStockProfitLossDetailReportRepository.Scope(TenantContextHolder.getRequiredTenantId(),
                scope.isAll(), scope.getDepartmentWarehouseIds(), scope.getSelfWarehouseIds(),
                scope.getUserId() == null ? null : String.valueOf(scope.getUserId()));
    }

    private void finishRows(List<Row> rows, Set<String> hiddenFields) {
        boolean priceVisible = !hiddenFields.contains("costUnitPrice")
                && !hiddenFields.contains("adjustedUnitPrice")
                && !hiddenFields.contains("profitLossAmount");
        for (Row row : rows) {
            row.setCheckTypeName(checkTypeName(row));
            row.setPriceVisible(priceVisible).setHiddenFields(hiddenFields);
            maskRow(row, hiddenFields);
        }
    }

    private String checkTypeName(Row row) {
        if (Integer.valueOf(1).equals(row.getCheckType())) return "盘数量";
        if (Integer.valueOf(2).equals(row.getCheckType())) return "盘成本";
        return "未知";
    }

    private void maskRow(Row row, Set<String> hiddenFields) {
        if (hiddenFields.contains("checkTypeName")) row.setCheckTypeName(null);
        if (hiddenFields.contains("documentNo")) row.setDocumentNo(null).setCheckId(null);
        if (hiddenFields.contains("checkDate")) row.setCheckDate(null);
        if (hiddenFields.contains("productCode")) row.setProductCode(null).setProductId(null);
        if (hiddenFields.contains("productName")) row.setProductName(null);
        if (hiddenFields.contains("warehouseName")) row.setWarehouseName(null).setWarehouseId(null);
        if (hiddenFields.contains("shelf")) row.setShelf(null);
        if (hiddenFields.contains("profitLossCount")) row.setProfitLossCount(null);
        if (hiddenFields.contains("costUnitPrice")) row.setCostUnitPrice(null);
        if (hiddenFields.contains("adjustedUnitPrice")) row.setAdjustedUnitPrice(null);
        if (hiddenFields.contains("actualCount")) row.setActualCount(null);
        if (hiddenFields.contains("profitLossAmount")) row.setProfitLossAmount(null);
        if (hiddenFields.contains("creatorName")) row.setCreatorName(null);
        if (hiddenFields.contains("vehicleModel")) row.setVehicleModel(null);
        if (hiddenFields.contains("reason")) row.setReason(null);
        if (hiddenFields.contains("originPlace")) row.setOriginPlace(null);
        if (hiddenFields.contains("standard")) row.setStandard(null);
        if (hiddenFields.contains("drawingNo")) row.setDrawingNo(null);
        if (hiddenFields.contains("relatedNo")) row.setRelatedNo(null);
        if (hiddenFields.contains("itemRemark")) row.setItemRemark(null);
    }

    private Set<String> reportHiddenFields() {
        Set<String> stockHidden = normalize(fieldPermissionMasker.getHiddenFieldSet(STOCK_CHECK_MODULE));
        Set<String> productHidden = normalize(permissionApi.getCurrentUserHiddenFields(PRODUCT_MODULE));
        Set<String> result = new LinkedHashSet<>();
        addIfHidden(result, stockHidden, "checkTypeName", "checkType");
        addIfHidden(result, stockHidden, "documentNo", "no");
        addIfHidden(result, stockHidden, "checkDate", "checkTime");
        addIfHidden(result, stockHidden, "warehouseName", "item_warehouseId", "item_warehouseName");
        addIfHidden(result, stockHidden, "profitLossCount", "item_count");
        addIfHidden(result, stockHidden, "adjustedUnitPrice", "item_productPrice");
        addIfHidden(result, stockHidden, "actualCount", "item_actualCount");
        addIfHidden(result, stockHidden, "profitLossAmount", "item_totalPrice");
        addIfHidden(result, stockHidden, "creatorName", "creator");
        addIfHidden(result, stockHidden, "reason", "remark");
        addIfHidden(result, stockHidden, "itemRemark", "item_remark");
        if (containsAny(stockHidden, "item_productId", "item_productCode")) result.add("productCode");
        if (containsAny(stockHidden, "item_productId", "item_productName")) result.add("productName");
        addIfHidden(result, productHidden, "productCode", "code");
        addIfHidden(result, productHidden, "productName", "name");
        addIfHidden(result, productHidden, "shelf", "shelf");
        addIfHidden(result, productHidden, "costUnitPrice", "purchasePrice");
        addIfHidden(result, productHidden, "vehicleModel", "vehicleModel");
        addIfHidden(result, productHidden, "originPlace", "originPlace");
        addIfHidden(result, productHidden, "standard", "standard");
        addIfHidden(result, productHidden, "drawingNo", "drawingNo");
        if (containsAny(result, "costUnitPrice", "adjustedUnitPrice", "profitLossAmount")) {
            result.add("costUnitPrice");
            result.add("adjustedUnitPrice");
            result.add("profitLossAmount");
        }
        return result;
    }

    private void stripHiddenFilters(Filter filter, Set<String> hiddenFields) {
        if (hiddenFields.contains("checkTypeName")) filter.setCheckType(null);
        if (hiddenFields.contains("documentNo")) filter.setDocumentNo(null);
        if (hiddenFields.contains("checkDate")) filter.setCheckDate(null);
        if (hiddenFields.contains("warehouseName")) filter.setWarehouseId(null);
        if (hiddenFields.contains("creatorName")) filter.setCreatorKeyword(null);
        if (containsAny(hiddenFields, "productCode", "productName", "shelf", "vehicleModel",
                "originPlace", "standard", "drawingNo")) {
            filter.setKeyword(null);
        }
    }

    private Set<String> exportFields(Set<String> hiddenFields) {
        Set<String> fields = new LinkedHashSet<>(Arrays.asList("checkTypeName", "documentNo", "checkDate",
                "productCode", "productName", "warehouseName", "shelf", "profitLossCount", "costUnitPrice",
                "adjustedUnitPrice", "actualCount", "profitLossAmount", "creatorName", "vehicleModel", "reason",
                "originPlace", "standard", "drawingNo", "relatedNo", "itemRemark"));
        fields.removeAll(hiddenFields);
        return fields;
    }

    private void validate(Filter filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1
                || filter.getPageSize() == null || filter.getPageSize() < 1 || filter.getPageSize() > 500) {
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        }
        if (filter.getCheckType() != null && filter.getCheckType() != 1 && filter.getCheckType() != 2) {
            throw new ServiceException(400, "盘点类型不正确");
        }
        if (filter.getCheckDateFrom() != null && filter.getCheckDateTo() != null
                && filter.getCheckDateFrom().isAfter(filter.getCheckDateTo())) {
            throw new ServiceException(400, "日期范围不正确");
        }
        if (filter.getOrderDirection() != null && !filter.getOrderDirection().trim().isEmpty()
                && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
        if (length(filter.getKeyword()) > 100 || length(filter.getDocumentNo()) > 100
                || length(filter.getCreatorKeyword()) > 100) {
            throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
        }
    }

    private Set<String> normalize(Collection<String> fields) {
        if (fields == null) return Collections.emptySet();
        return fields.stream().map(field -> field.startsWith("col_") ? field.substring(4) : field)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private void addIfHidden(Set<String> target, Set<String> source, String reportField, String... sourceFields) {
        if (containsAny(source, sourceFields)) target.add(reportField);
    }

    private boolean containsAny(Set<String> fields, String... candidates) {
        return Arrays.stream(candidates).anyMatch(fields::contains);
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
