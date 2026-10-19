package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableAgingReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableAgingReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableAgingReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Service
public class ErpPayableAgingReportService {

    private static final String ACCOUNT_MODULE = "erp_finance_payable_account";
    private static final String SUPPLIER_MODULE = "erp_supplier";
    private static final String PURCHASE_IN_MODULE = "erp_purchase_in";
    private static final String PURCHASE_RETURN_MODULE = "erp_purchase_return";
    private static final String PURCHASE_ADJUST_MODULE = "erp_purchase_price_adjust";
    private static final String PAYABLE_ADJUST_MODULE = "erp_finance_payable_other";
    private static final String PAYMENT_MODULE = "erp_finance_payment";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;
    private static final List<String> AMOUNT_FIELDS = Arrays.asList("balance", "month01", "month02", "month03",
            "month04", "month05", "month06", "month07", "month08", "month09", "month10", "month11",
            "month12", "withinTwoYears", "withinThreeYears", "overThreeYears");

    @Resource
    private ErpPayableAgingReportRepository repository;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        normalizeAndValidate(filter);
        Visibility visibility = visibility();
        validateFilterVisibility(filter, visibility);
        ErpPayableAgingReportRepository.Scope scope = resolveScope();
        if (scope == null) return PageResult.empty();
        long total = repository.count(filter, scope);
        if (total == 0) return PageResult.empty();
        List<Row> rows = repository.page(filter, scope,
                (long) (filter.getPageNo() - 1) * filter.getPageSize(), filter.getPageSize());
        applyVisibility(rows, visibility);
        return new PageResult<>(rows, total);
    }

    @Transactional(readOnly = true)
    public Summary getSummary(Filter filter) {
        normalizeAndValidate(filter);
        Visibility visibility = visibility();
        validateFilterVisibility(filter, visibility);
        ErpPayableAgingReportRepository.Scope scope = resolveScope();
        Summary summary = scope == null ? emptySummary() : repository.summary(filter, scope);
        applyVisibility(summary, visibility);
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        normalizeAndValidate(filter);
        Visibility visibility = visibility();
        validateFilterVisibility(filter, visibility);
        ErpPayableAgingReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0 : repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("应付账龄分析.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(visibility)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("应付账龄分析").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                applyVisibility(rows, visibility);
                writer.write(rows, sheet);
            }
        }
    }

    private ErpPayableAgingReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        ErpFinanceVisibleScope account = visibleScope(userId, ACCOUNT_MODULE);
        ErpFinanceVisibleScope supplier = visibleScope(userId, SUPPLIER_MODULE);
        if (account == null || supplier == null || !account.hasAccess() || !supplier.hasAccess()) return null;
        return new ErpPayableAgingReportRepository.Scope(
                TenantContextHolder.getRequiredTenantId(), account, supplier);
    }

    private ErpFinanceVisibleScope visibleScope(Long userId, String module) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, module);
        return ErpFinanceVisibleScope.from(permission, userId);
    }

    private Visibility visibility() {
        Set<String> supplierHidden = hiddenFields(SUPPLIER_MODULE);
        return new Visibility(isAmountVisible(), !supplierHidden.contains("supplierType"),
                !supplierHidden.contains("purchaser"), !supplierHidden.contains("name"));
    }

    private void applyVisibility(List<Row> rows, Visibility visibility) {
        for (Row row : rows) {
            row.setAmountVisible(visibility.amountVisible);
            if (!visibility.amountVisible) clearAmounts(row);
            if (!visibility.supplierNameVisible) row.setSupplierName(null);
            if (!visibility.supplierTypeVisible) row.setSupplierType(null);
            if (!visibility.purchaserVisible) row.setPurchaser(null);
        }
    }

    private void applyVisibility(Summary summary, Visibility visibility) {
        summary.setAmountVisible(visibility.amountVisible)
                .setSupplierTypeVisible(visibility.supplierTypeVisible)
                .setPurchaserVisible(visibility.purchaserVisible);
        if (!visibility.amountVisible) clearAmounts(summary);
    }

    private boolean isAmountVisible() {
        Set<String> hidden = hiddenFields(ACCOUNT_MODULE);
        if (hidden.stream().anyMatch(field -> field.toLowerCase().contains("amount")
                || field.toLowerCase().contains("balance") || field.toLowerCase().contains("price"))) {
            return false;
        }
        return !fieldPermissionMasker.isFieldHidden(PURCHASE_IN_MODULE, "totalPrice")
                && !fieldPermissionMasker.isFieldHidden(PURCHASE_RETURN_MODULE, "totalPrice")
                && !fieldPermissionMasker.isFieldHidden(PURCHASE_ADJUST_MODULE, "totalAdjustPrice")
                && !fieldPermissionMasker.isFieldHidden(PAYABLE_ADJUST_MODULE, "payableAmount")
                && !fieldPermissionMasker.isFieldHidden(PAYMENT_MODULE, "totalPrice")
                && !fieldPermissionMasker.isFieldHidden(PAYMENT_MODULE, "paymentPrice");
    }

    private Set<String> hiddenFields(String module) {
        Collection<String> fields = permissionApi.getCurrentUserHiddenFields(module);
        if (fields == null) return Collections.emptySet();
        return fields.stream().map(field -> field.startsWith("col_") ? field.substring(4) : field)
                .collect(Collectors.toSet());
    }

    private void validateFilterVisibility(Filter filter, Visibility visibility) {
        if (hasText(filter.getSupplierType()) && !visibility.supplierTypeVisible) {
            throw new ServiceException(403, "无供应商类型查看权限");
        }
        if (hasText(filter.getPurchaser()) && !visibility.purchaserVisible) {
            throw new ServiceException(403, "无采购员查看权限");
        }
    }

    private void normalizeAndValidate(Filter filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1 || filter.getPageSize() == null
                || filter.getPageSize() < 1 || filter.getPageSize() > 500) {
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        }
        if (filter.getAsOfDate() == null) filter.setAsOfDate(LocalDate.now());
        if (hasText(filter.getOrderDirection()) && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
        for (String value : Arrays.asList(filter.getSupplierType(), filter.getPurchaser())) {
            if (value != null && value.length() > 100) {
                throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
            }
        }
    }

    private Set<String> exportFields(Visibility visibility) {
        Set<String> fields = new LinkedHashSet<>();
        if (visibility.supplierNameVisible) fields.add("supplierName");
        if (visibility.supplierTypeVisible) fields.add("supplierType");
        if (visibility.purchaserVisible) fields.add("purchaser");
        if (visibility.amountVisible) fields.addAll(AMOUNT_FIELDS);
        return fields;
    }

    private Summary emptySummary() {
        Summary summary = new Summary().setRowCount(0L);
        setZeroAmounts(summary);
        return summary;
    }

    private void setZeroAmounts(Summary summary) {
        BigDecimal zero = BigDecimal.ZERO;
        summary.setBalance(zero).setMonth01(zero).setMonth02(zero).setMonth03(zero).setMonth04(zero)
                .setMonth05(zero).setMonth06(zero).setMonth07(zero).setMonth08(zero).setMonth09(zero)
                .setMonth10(zero).setMonth11(zero).setMonth12(zero).setWithinTwoYears(zero)
                .setWithinThreeYears(zero).setOverThreeYears(zero);
    }

    private void clearAmounts(Row row) {
        row.setBalance(null).setMonth01(null).setMonth02(null).setMonth03(null).setMonth04(null)
                .setMonth05(null).setMonth06(null).setMonth07(null).setMonth08(null).setMonth09(null)
                .setMonth10(null).setMonth11(null).setMonth12(null).setWithinTwoYears(null)
                .setWithinThreeYears(null).setOverThreeYears(null);
    }

    private void clearAmounts(Summary summary) {
        summary.setBalance(null).setMonth01(null).setMonth02(null).setMonth03(null).setMonth04(null)
                .setMonth05(null).setMonth06(null).setMonth07(null).setMonth08(null).setMonth09(null)
                .setMonth10(null).setMonth11(null).setMonth12(null).setWithinTwoYears(null)
                .setWithinThreeYears(null).setOverThreeYears(null);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static class Visibility {
        private final boolean amountVisible;
        private final boolean supplierTypeVisible;
        private final boolean purchaserVisible;
        private final boolean supplierNameVisible;

        private Visibility(boolean amountVisible, boolean supplierTypeVisible,
                           boolean purchaserVisible, boolean supplierNameVisible) {
            this.amountVisible = amountVisible;
            this.supplierTypeVisible = supplierTypeVisible;
            this.purchaserVisible = purchaserVisible;
            this.supplierNameVisible = supplierNameVisible;
        }
    }
}
