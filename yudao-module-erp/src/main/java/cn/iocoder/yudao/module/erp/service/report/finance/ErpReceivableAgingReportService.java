package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.DetailFilter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.DetailRow;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_3_TO_6_MONTHS;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_6_TO_12_MONTHS;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_ALL;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_OVER_1_YEAR;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_WITHIN_1_MONTH;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_WITHIN_2_MONTHS;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_WITHIN_3_MONTHS;

/** 应收账龄分析服务。 */
@Service
public class ErpReceivableAgingReportService {

    private static final String ACCOUNT_MODULE = "erp_finance_receivable_account";
    private static final String CUSTOMER_MODULE = "erp_customer";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;
    private static final Set<String> BUCKETS = new HashSet<>(Arrays.asList(BUCKET_ALL, BUCKET_WITHIN_1_MONTH,
            BUCKET_WITHIN_2_MONTHS, BUCKET_WITHIN_3_MONTHS, BUCKET_3_TO_6_MONTHS,
            BUCKET_6_TO_12_MONTHS, BUCKET_OVER_1_YEAR));

    @Resource private ErpReceivableAgingReportRepository repository;
    @Resource private PermissionApi permissionApi;
    @Resource private ErpSaleFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validate(filter);
        Visibility visibility = visibility();
        validateFilterVisibility(filter, visibility);
        ErpReceivableAgingReportRepository.Scope scope = resolveScope();
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
        validate(filter);
        Visibility visibility = visibility();
        validateFilterVisibility(filter, visibility);
        ErpReceivableAgingReportRepository.Scope scope = resolveScope();
        Summary summary = scope == null ? emptySummary() : repository.summary(filter, scope);
        summary.setAmountVisible(visibility.amount).setCreditVisible(visibility.credit)
                .setCustomerNameVisible(visibility.customerName).setCustomerTypeVisible(visibility.customerType)
                .setAreaVisible(visibility.area).setSaleUserVisible(visibility.saleUser)
                .setDeptVisible(visibility.dept).setDeveloperVisible(visibility.developer);
        if (!visibility.amount) {
            summary.setBalance(null).setWithin1Month(null).setWithin2Months(null).setWithin3Months(null)
                    .setMonths3To6(null).setMonths6To12(null).setOver1Year(null);
        }
        if (!visibility.credit) summary.setCreditLimit(null).setCreditBalance(null);
        return summary;
    }

    @Transactional(readOnly = true)
    public PageResult<DetailRow> getDetailPage(DetailFilter filter) {
        validateDetail(filter);
        Visibility visibility = visibility();
        ErpReceivableAgingReportRepository.Scope scope = resolveScope();
        if (scope == null) return PageResult.empty();
        long total = repository.detailCount(filter, scope);
        if (total == 0) return PageResult.empty();
        List<DetailRow> rows = repository.detailPage(filter, scope,
                (long) (filter.getPageNo() - 1) * filter.getPageSize(), filter.getPageSize());
        applyDetailVisibility(rows, visibility);
        return new PageResult<>(rows, total);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validate(filter);
        Visibility visibility = visibility();
        validateFilterVisibility(filter, visibility);
        ErpReceivableAgingReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0 : repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("应收账龄分析报表.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields()).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("应收账龄分析").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                applyVisibility(rows, visibility);
                writer.write(rows, sheet);
            }
        }
    }

    private ErpReceivableAgingReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        ErpFinanceVisibleScope account = visibleScope(userId, ACCOUNT_MODULE);
        ErpFinanceVisibleScope customer = visibleScope(userId, CUSTOMER_MODULE);
        if (account == null || customer == null || !account.hasAccess() || !customer.hasAccess()) return null;
        return new ErpReceivableAgingReportRepository.Scope(TenantContextHolder.getRequiredTenantId(), account, customer);
    }

    private ErpFinanceVisibleScope visibleScope(Long userId, String module) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, module);
        return ErpFinanceVisibleScope.from(permission, userId);
    }

    private Visibility visibility() {
        Set<String> account = hiddenFields(ACCOUNT_MODULE);
        Set<String> customer = hiddenFields(CUSTOMER_MODULE);
        Visibility value = new Visibility();
        value.amount = isAmountVisible(account);
        value.credit = !containsAny(account, "creditLimit", "creditBalance") && !customer.contains("creditLimit");
        value.customerName = !customer.contains("name");
        value.customerType = !customer.contains("customerType");
        value.area = !customer.contains("areaId");
        value.saleUser = !customer.contains("saleUserId");
        value.developer = !customer.contains("developerUserId");
        value.dept = !containsAny(account, "deptId", "deptName");
        value.description = !account.contains("remark");
        return value;
    }

    private void applyVisibility(List<Row> rows, Visibility visible) {
        for (Row row : rows) {
            row.setAmountVisible(visible.amount);
            if (!visible.customerName) row.setCustomerName(null).setCustomerId(null);
            if (!visible.customerType) row.setCustomerTypeName(null).setCustomerTypeId(null);
            if (!visible.area) row.setAreaName(null).setAreaId(null);
            if (!visible.saleUser) row.setSaleUserName(null).setSaleUserId(null);
            if (!visible.developer) row.setDeveloperName(null).setDeveloperUserId(null);
            if (!visible.dept) row.setDeptName(null).setDeptId(null);
            if (!visible.amount) row.setBalance(null).setWithin1Month(null).setWithin2Months(null)
                    .setWithin3Months(null).setMonths3To6(null).setMonths6To12(null).setOver1Year(null);
            if (!visible.credit) row.setCreditLimit(null).setCreditBalance(null);
        }
    }

    private void applyDetailVisibility(List<DetailRow> rows, Visibility visible) {
        Map<String, Set<String>> sourceFields = new HashMap<>();
        sourceFields.put("SALE_OUT", hiddenFields("erp_sale_out"));
        sourceFields.put("SALE_RETURN", hiddenFields("erp_sale_return"));
        sourceFields.put("SALE_ADJUST", hiddenFields("erp_sale_price_adjust"));
        sourceFields.put("FINANCE_RECEIPT", hiddenFields("erp_finance_receipt"));
        sourceFields.put("RECEIVABLE_ADJUST", hiddenFields("erp_finance_receivable_other"));
        for (DetailRow row : rows) {
            Set<String> hidden = sourceFields.getOrDefault(row.getDocumentTypeCode(), Collections.emptySet());
            boolean rowAmount = visible.amount && sourceAmountVisible(row.getDocumentTypeCode(), hidden);
            row.setAmountVisible(rowAmount);
            if (!rowAmount) row.setAmount(null);
            if (!visible.customerName) row.setCustomerName(null).setCustomerId(null);
            if (!visible.dept) row.setDeptName(null).setDeptId(null);
            if (!visible.description || hidden.contains("remark")) row.setDescription(null);
            if (hidden.contains("no")) row.setDocumentNo(null).setBusinessId(null);
            if (hidden.contains(sourceUserField(row.getDocumentTypeCode()))) {
                row.setBusinessUserName(null).setBusinessUserId(null);
            }
        }
    }

    private boolean isAmountVisible(Set<String> account) {
        if (account.stream().anyMatch(field -> field.toLowerCase().contains("amount")
                || field.toLowerCase().contains("balance") || field.toLowerCase().contains("price"))) return false;
        return !fieldPermissionMasker.isFieldHidden("erp_sale_out", "receiptPrice")
                && !fieldPermissionMasker.isFieldHidden("erp_sale_return", "refundPrice")
                && !fieldPermissionMasker.isFieldHidden("erp_sale_price_adjust", "totalAdjustPrice")
                && !fieldPermissionMasker.isFieldHidden("erp_finance_receipt", "totalPrice")
                && !fieldPermissionMasker.isFieldHidden("erp_finance_receipt", "receiptPrice")
                && !fieldPermissionMasker.isFieldHidden("erp_finance_receivable_other", "receivableAmount");
    }

    private boolean sourceAmountVisible(String type, Set<String> hidden) {
        switch (type) {
            case "SALE_OUT": return !hidden.contains("receiptPrice");
            case "SALE_RETURN": return !hidden.contains("refundPrice") && !hidden.contains("totalPrice");
            case "SALE_ADJUST": return !hidden.contains("totalAdjustPrice");
            case "FINANCE_RECEIPT": return !hidden.contains("totalPrice") && !hidden.contains("receiptPrice");
            case "RECEIVABLE_ADJUST": return !hidden.contains("receivableAmount");
            default: return false;
        }
    }

    private String sourceUserField(String type) {
        if ("SALE_OUT".equals(type) || "SALE_RETURN".equals(type)) return "saleUserId";
        if ("SALE_ADJUST".equals(type)) return "adjustUserId";
        if ("FINANCE_RECEIPT".equals(type)) return "financeUserId";
        return "handlerId";
    }

    private Set<String> hiddenFields(String module) {
        Collection<String> fields = permissionApi.getCurrentUserHiddenFields(module);
        if (fields == null) return Collections.emptySet();
        return fields.stream().map(field -> field.startsWith("col_") ? field.substring(4) : field)
                .collect(Collectors.toSet());
    }

    private boolean containsAny(Set<String> values, String... fields) {
        for (String field : fields) if (values.contains(field)) return true;
        return false;
    }

    private void validateFilterVisibility(Filter filter, Visibility visible) {
        if (filter.getCustomerId() != null && !visible.customerName) hiddenFilter();
        if (filter.getCustomerType() != null && !visible.customerType) hiddenFilter();
        if (filter.getAreaId() != null && !visible.area) hiddenFilter();
        if (filter.getSaleUserId() != null && !visible.saleUser) hiddenFilter();
        if (filter.getDeveloperUserId() != null && !visible.developer) hiddenFilter();
        if (filter.getDeptId() != null && !visible.dept) hiddenFilter();
        String order = filter.getOrderField();
        if (!visible.amount && Arrays.asList("balance", "within1Month", "within2Months", "within3Months",
                "months3To6", "months6To12", "over1Year").contains(order)) hiddenFilter();
        if (!visible.credit && Arrays.asList("creditLimit", "creditBalance").contains(order)) hiddenFilter();
    }

    private void hiddenFilter() {
        throw new ServiceException(403, "无权使用当前筛选或排序字段");
    }

    void validate(Filter filter) {
        validatePage(filter);
        validateDate(filter.getAnalysisDate());
        validateDirection(filter.getOrderDirection());
    }

    void validateDetail(DetailFilter filter) {
        validatePage(filter);
        validateDate(filter.getAnalysisDate());
        validateDirection(filter.getOrderDirection());
        if (filter.getCustomerId() == null) throw new ServiceException(400, "客户不能为空");
        if (!Boolean.TRUE.equals(filter.getDeptUnassigned()) && filter.getDeptId() == null)
            throw new ServiceException(400, "部门不能为空");
        if (filter.getBucket() == null || !BUCKETS.contains(filter.getBucket()))
            throw new ServiceException(400, "账龄区间不正确");
    }

    private void validatePage(cn.iocoder.yudao.framework.common.pojo.PageParam filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1 || filter.getPageSize() == null
                || filter.getPageSize() < 1 || filter.getPageSize() > 500)
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
    }

    private void validateDate(LocalDate date) {
        if (date == null) throw new ServiceException(400, "分析日期不能为空");
        if (date.isAfter(LocalDate.now())) throw new ServiceException(400, "分析日期不能晚于今天");
    }

    private void validateDirection(String direction) {
        if (direction != null && !direction.trim().isEmpty() && !"asc".equalsIgnoreCase(direction)
                && !"desc".equalsIgnoreCase(direction)) throw new ServiceException(400, "排序方向必须为 asc 或 desc");
    }

    private Summary emptySummary() {
        return new Summary().setRowCount(0L).setBalance(BigDecimal.ZERO).setCreditLimit(BigDecimal.ZERO)
                .setCreditBalance(BigDecimal.ZERO).setWithin1Month(BigDecimal.ZERO).setWithin2Months(BigDecimal.ZERO)
                .setWithin3Months(BigDecimal.ZERO).setMonths3To6(BigDecimal.ZERO).setMonths6To12(BigDecimal.ZERO)
                .setOver1Year(BigDecimal.ZERO);
    }

    private Set<String> exportFields() {
        return new LinkedHashSet<>(Arrays.asList("customerName", "customerTypeName", "balance", "creditLimit",
                "creditBalance", "within1Month", "within2Months", "within3Months", "months3To6",
                "months6To12", "over1Year", "areaName", "saleUserName", "deptName", "developerName"));
    }

    private static class Visibility {
        private boolean amount;
        private boolean credit;
        private boolean customerName;
        private boolean customerType;
        private boolean area;
        private boolean saleUser;
        private boolean developer;
        private boolean dept;
        private boolean description;
    }
}
