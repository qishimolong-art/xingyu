package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockAgeReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockAgeReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockAgeReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.config.ErpStockSelectPriceConfigService;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockAgeReportModels.*;

/** 库龄及长期未出库辅助报表服务。 */
@Service
public class ErpStockAgeReportService {

    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;
    private static final Set<String> BUCKETS = new HashSet<>(Arrays.asList(BUCKET_ALL,
            BUCKET_WITHIN_1_MONTH, BUCKET_MONTHS_2_TO_3, BUCKET_MONTHS_4_TO_6,
            BUCKET_MONTHS_7_TO_12, BUCKET_YEARS_1_TO_2, BUCKET_YEARS_2_TO_3,
            BUCKET_OVER_3_YEARS, BUCKET_UNCLASSIFIED));
    private static final Set<String> OUTBOUND_STATES = new HashSet<>(Arrays.asList(
            OUTBOUND_ALL, OUTBOUND_NEVER, OUTBOUND_HAS));

    @Resource
    private ErpStockAgeReportRepository repository;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpStockSelectPriceConfigService stockSelectPriceConfigService;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validate(filter);
        boolean amountVisible = isAmountVisible();
        validateSortVisibility(filter, amountVisible);
        ErpStockAgeReportRepository.Scope scope = resolveScope();
        LocalDate asOfDate = LocalDate.now();
        long total = repository.count(filter, scope, asOfDate);
        if (total == 0) {
            return PageResult.empty();
        }
        List<Row> rows = repository.page(filter, scope, asOfDate,
                (long) (filter.getPageNo() - 1) * filter.getPageSize(), filter.getPageSize());
        applyAmountVisibility(rows, amountVisible);
        return new PageResult<>(rows, total);
    }

    @Transactional(readOnly = true)
    public Summary getSummary(Filter filter) {
        validate(filter);
        boolean amountVisible = isAmountVisible();
        validateSortVisibility(filter, amountVisible);
        Summary summary = repository.summary(filter, resolveScope(), LocalDate.now());
        if (summary == null) {
            summary = emptySummary();
        }
        applyAmountVisibility(summary, amountVisible);
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validate(filter);
        boolean amountVisible = isAmountVisible();
        validateSortVisibility(filter, amountVisible);
        ErpStockAgeReportRepository.Scope scope = resolveScope();
        LocalDate asOfDate = LocalDate.now();
        long total = repository.count(filter, scope, asOfDate);
        if (total > EXPORT_LIMIT) {
            throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("库龄及长期未出库辅助报表.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(amountVisible)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("库龄分析").build();
            if (total == 0) {
                writer.write(Collections.emptyList(), sheet);
            }
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, asOfDate, offset, EXPORT_BATCH_SIZE);
                applyAmountVisibility(rows, amountVisible);
                writer.write(rows, sheet);
            }
            Summary summary = repository.summary(filter, scope, asOfDate);
            if (summary != null && summary.getRowCount() != null && summary.getRowCount() > 0) {
                applyAmountVisibility(summary, amountVisible);
                writer.write(Collections.singletonList(toTotalRow(summary, amountVisible)), sheet);
            }
        }
    }

    ErpStockAgeReportRepository.Scope resolveScope() {
        ErpProductStockPermissionScope scope = warehouseService.getCurrentUserProductStockPermissionScope();
        return new ErpStockAgeReportRepository.Scope(TenantContextHolder.getRequiredTenantId(), scope.isAll(),
                scope.getDepartmentWarehouseIds(), scope.getSelfWarehouseIds(),
                scope.getUserId() == null ? null : String.valueOf(scope.getUserId()));
    }

    boolean isAmountVisible() {
        Long deptId = getLoginUserDeptId();
        Collection<String> hidden = deptId == null
                ? permissionApi.getCurrentUserHiddenFields("erp_product")
                : permissionApi.getCurrentUserHiddenFields("erp_product", deptId);
        Set<String> hiddenFields = hidden == null ? new HashSet<>() : hidden.stream()
                .map(field -> field.startsWith("col_") ? field.substring(4) : field)
                .collect(Collectors.toSet());
        Collection<String> sceneHiddenFields = stockSelectPriceConfigService.getSceneHiddenPriceFields(null);
        hiddenFields.addAll((sceneHiddenFields == null ? Collections.<String>emptySet() : sceneHiddenFields).stream()
                .map(field -> field.startsWith("col_") ? field.substring(4) : field)
                .collect(Collectors.toSet()));
        return !hiddenFields.contains("lastPurchasePrice");
    }

    private void applyAmountVisibility(List<Row> rows, boolean visible) {
        for (Row row : rows) {
            row.setAmountVisible(visible);
            if (!visible) {
                clearAmounts(row);
            }
        }
    }

    private void applyAmountVisibility(Summary summary, boolean visible) {
        summary.setAmountVisible(visible);
        if (!visible) {
            summary.setTotalAmount(null)
                    .setWithin1MonthAmount(null).setMonths2To3Amount(null).setMonths4To6Amount(null)
                    .setMonths7To12Amount(null).setYears1To2Amount(null).setYears2To3Amount(null)
                    .setOver3YearsAmount(null).setUnclassifiedAmount(null);
        }
    }

    private void clearAmounts(Row row) {
        row.setCostUnitPrice(null).setCostAmount(null)
                .setWithin1MonthAmount(null).setMonths2To3Amount(null).setMonths4To6Amount(null)
                .setMonths7To12Amount(null).setYears1To2Amount(null).setYears2To3Amount(null)
                .setOver3YearsAmount(null);
    }

    private Row toTotalRow(Summary summary, boolean amountVisible) {
        Row row = new Row().setProductName("合计").setStockQuantity(summary.getTotalQuantity())
                .setWithin1MonthQuantity(summary.getWithin1MonthQuantity())
                .setMonths2To3Quantity(summary.getMonths2To3Quantity())
                .setMonths4To6Quantity(summary.getMonths4To6Quantity())
                .setMonths7To12Quantity(summary.getMonths7To12Quantity())
                .setYears1To2Quantity(summary.getYears1To2Quantity())
                .setYears2To3Quantity(summary.getYears2To3Quantity())
                .setOver3YearsQuantity(summary.getOver3YearsQuantity())
                .setAgingBasis("完整筛选范围合计").setAmountVisible(amountVisible);
        if (amountVisible) {
            row.setCostAmount(summary.getTotalAmount())
                    .setWithin1MonthAmount(summary.getWithin1MonthAmount())
                    .setMonths2To3Amount(summary.getMonths2To3Amount())
                    .setMonths4To6Amount(summary.getMonths4To6Amount())
                    .setMonths7To12Amount(summary.getMonths7To12Amount())
                    .setYears1To2Amount(summary.getYears1To2Amount())
                    .setYears2To3Amount(summary.getYears2To3Amount())
                    .setOver3YearsAmount(summary.getOver3YearsAmount());
        }
        return row;
    }

    private Set<String> exportFields(boolean amountVisible) {
        Set<String> fields = new LinkedHashSet<>(Arrays.asList("warehouseName", "deptName", "productCode",
                "productName", "stockQuantity", "within1MonthQuantity", "months2To3Quantity",
                "months4To6Quantity", "months7To12Quantity", "years1To2Quantity", "years2To3Quantity",
                "over3YearsQuantity", "lastInboundTime", "lastWarehouseMoveInboundTime", "lastOutboundTime",
                "ageDays", "agingBasis", "standard", "vehicleModel", "drawingNo", "originPlace",
                "categoryName", "featureCode", "brand"));
        if (amountVisible) {
            fields.addAll(Arrays.asList("costUnitPrice", "costAmount", "within1MonthAmount",
                    "months2To3Amount", "months4To6Amount", "months7To12Amount", "years1To2Amount",
                    "years2To3Amount", "over3YearsAmount"));
        }
        return fields;
    }

    private void validate(Filter filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1
                || filter.getPageSize() == null || filter.getPageSize() < 1 || filter.getPageSize() > 500) {
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        }
        if (filter.getAgeBucket() != null && !BUCKETS.contains(filter.getAgeBucket())) {
            throw new ServiceException(400, "库龄区间不正确");
        }
        if (filter.getOutboundState() != null && !OUTBOUND_STATES.contains(filter.getOutboundState())) {
            throw new ServiceException(400, "出库状态不正确");
        }
        if (filter.getOrderDirection() != null && !filter.getOrderDirection().trim().isEmpty()
                && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
    }

    private void validateSortVisibility(Filter filter, boolean amountVisible) {
        if (!amountVisible && Arrays.asList("costUnitPrice", "costAmount").contains(filter.getOrderField())) {
            throw new ServiceException(403, "无权使用当前排序字段");
        }
    }

    private Summary emptySummary() {
        BigDecimal zero = BigDecimal.ZERO;
        return new Summary().setRowCount(0L).setTotalQuantity(zero).setTotalAmount(zero)
                .setWithin1MonthQuantity(zero).setWithin1MonthAmount(zero)
                .setMonths2To3Quantity(zero).setMonths2To3Amount(zero)
                .setMonths4To6Quantity(zero).setMonths4To6Amount(zero)
                .setMonths7To12Quantity(zero).setMonths7To12Amount(zero)
                .setYears1To2Quantity(zero).setYears1To2Amount(zero)
                .setYears2To3Quantity(zero).setYears2To3Amount(zero)
                .setOver3YearsQuantity(zero).setOver3YearsAmount(zero)
                .setUnclassifiedRowCount(0L).setUnclassifiedQuantity(zero).setUnclassifiedAmount(zero);
    }
}
