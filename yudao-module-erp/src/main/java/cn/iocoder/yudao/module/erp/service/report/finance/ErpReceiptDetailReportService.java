package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceiptDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceiptDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceiptDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceiptDetailReportModels.*;

@Service
public class ErpReceiptDetailReportService {

    private static final String RECEIPT_MODULE = "erp_finance_receipt";
    private static final String CUSTOMER_MODULE = "erp_customer";
    private static final String ACCOUNT_MODULE = "erp_account";
    private static final String VOUCHER_MODULE = "erp_voucher";
    private static final String SALE_OUT_MODULE = "erp_sale_out";
    private static final String SALE_RETURN_MODULE = "erp_sale_return";
    private static final String SALE_ADJUST_MODULE = "erp_sale_price_adjust";
    private static final String RECEIVABLE_MISC_MODULE = "erp_finance_receivable_misc";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;

    @Resource private ErpReceiptDetailReportRepository repository;
    @Resource private PermissionApi permissionApi;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validate(filter);
        ErpReceiptDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return PageResult.empty();
        long total = repository.count(filter, scope);
        if (total == 0) return PageResult.empty();
        List<Row> rows = repository.page(filter, scope,
                (long) (filter.getPageNo() - 1) * filter.getPageSize(), filter.getPageSize());
        applyVisibility(rows);
        return new PageResult<>(rows, total);
    }

    @Transactional(readOnly = true)
    public Summary getSummary(Filter filter) {
        validate(filter);
        boolean amountVisible = isAmountVisible();
        ErpReceiptDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return emptySummary(amountVisible);
        Summary summary = repository.summary(filter, scope).setAmountVisible(amountVisible);
        if (!amountVisible) summary.setSettlementAmount(null).setActualReceiptAmount(null);
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validate(filter);
        ErpReceiptDetailReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0 : repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        boolean amountVisible = isAmountVisible();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("收款明细查询.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(amountVisible)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("收款明细").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                applyVisibility(rows);
                writer.write(rows, sheet);
            }
        }
    }

    private ErpReceiptDetailReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        ErpFinanceVisibleScope receipt = visibleScope(userId, RECEIPT_MODULE);
        ErpFinanceVisibleScope customer = visibleScope(userId, CUSTOMER_MODULE);
        if (receipt == null || customer == null || !receipt.hasAccess() || !customer.hasAccess()) return null;
        return new ErpReceiptDetailReportRepository.Scope(
                TenantContextHolder.getRequiredTenantId(), receipt, customer);
    }

    private ErpFinanceVisibleScope visibleScope(Long userId, String module) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, module);
        return ErpFinanceVisibleScope.from(permission, userId);
    }

    private void applyVisibility(List<Row> rows) {
        boolean amountVisible = isAmountVisible();
        boolean voucherVisible = isVoucherVisible();
        Set<String> receiptHidden = hiddenFields(RECEIPT_MODULE);
        Set<String> customerHidden = hiddenFields(CUSTOMER_MODULE);
        Set<String> accountHidden = hiddenFields(ACCOUNT_MODULE);
        Map<String, Set<String>> sourceHidden = new HashMap<>();
        sourceHidden.put(TYPE_SALE_OUT, hiddenFields(SALE_OUT_MODULE));
        sourceHidden.put(TYPE_SALE_RETURN, hiddenFields(SALE_RETURN_MODULE));
        sourceHidden.put(TYPE_SALE_PRICE_ADJUST, hiddenFields(SALE_ADJUST_MODULE));
        sourceHidden.put(TYPE_RECEIVABLE_MISC, hiddenFields(RECEIVABLE_MISC_MODULE));
        for (Row row : rows) {
            row.setAmountVisible(amountVisible);
            if (!amountVisible) {
                row.setSettlementAmount(null).setActualReceiptAmount(null).setCostAmount(null)
                        .setGrossProfit(null).setGrossProfitRate(null);
            }
            if (!voucherVisible) row.setVoucherNo(null);
            if (receiptHidden.contains("paymentVoucherNo")) row.setPaymentVoucherNo(null);
            if (customerHidden.contains("name")) row.setCustomerName(null);
            if (customerHidden.contains("customerType")) row.setCustomerType(null);
            if (accountHidden.contains("name")) row.setAccountName(null);
            if (accountHidden.contains("accountType")) row.setAccountType(null);
            if (receiptHidden.contains("receiptTime")) row.setReceiptDate(null);
            if (receiptHidden.contains("no")) row.setReceiptNo(null);
            if (receiptHidden.contains("creator")) row.setReceiptUserName(null);
            Set<String> source = sourceHidden.getOrDefault(row.getBusinessTypeCode(), Collections.emptySet());
            if (source.contains("no") || receiptHidden.contains("items") || receiptHidden.contains("bizNo")) {
                row.setBusinessNo(null);
            }
            if (source.contains("creator")) row.setBusinessCreatorName(null);
            if (source.contains("deptId")) row.setDeptName(null);
            if (source.contains("creator") || source.contains(businessUserField(row.getBusinessTypeCode()))) {
                row.setBusinessUserName(null);
            }
            if (source.contains(businessDateField(row.getBusinessTypeCode()))) row.setBusinessDate(null);
            if (source.contains("settleMethod") || customerHidden.contains("settleMethod")) row.setSettleMethod(null);
            if (receiptHidden.contains("remark") || receiptHidden.contains("items")
                    || receiptHidden.contains("item_remark")) row.setRemark(null);
        }
    }

    private String businessUserField(String type) {
        if (TYPE_SALE_OUT.equals(type) || TYPE_SALE_RETURN.equals(type)) return "saleUserId";
        if (TYPE_SALE_PRICE_ADJUST.equals(type)) return "adjustUserId";
        return "handlerId";
    }

    private String businessDateField(String type) {
        if (TYPE_SALE_OUT.equals(type)) return "outTime";
        if (TYPE_SALE_RETURN.equals(type)) return "returnTime";
        if (TYPE_SALE_PRICE_ADJUST.equals(type)) return "adjustDate";
        return "bizTime";
    }

    private boolean isAmountVisible() {
        Set<String> hidden = hiddenFields(RECEIPT_MODULE);
        if (hidden.contains("items")) return false;
        return hidden.stream().noneMatch(field -> {
            String normalized = field.toLowerCase();
            return normalized.contains("price") || normalized.contains("amount") || normalized.contains("discount");
        });
    }

    private boolean isVoucherVisible() {
        Long userId = getLoginUserId();
        return userId != null && permissionApi.hasAnyPermissions(userId, "erp:voucher:query")
                && !hiddenFields(VOUCHER_MODULE).contains("voucherNo");
    }

    private Set<String> hiddenFields(String module) {
        Collection<String> fields = permissionApi.getCurrentUserHiddenFields(module);
        if (fields == null) return Collections.emptySet();
        return fields.stream().map(field -> field.startsWith("col_") ? field.substring(4) : field)
                .collect(Collectors.toSet());
    }

    private Summary emptySummary(boolean visible) {
        return new Summary().setRowCount(0L).setSettlementAmount(visible ? BigDecimal.ZERO : null)
                .setActualReceiptAmount(visible ? BigDecimal.ZERO : null).setAmountVisible(visible);
    }

    private Set<String> exportFields(boolean amountVisible) {
        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(Arrays.asList("customerName", "receiptDate", "receiptNo", "businessNo", "businessType"));
        if (amountVisible) fields.addAll(Arrays.asList("settlementAmount", "actualReceiptAmount"));
        fields.addAll(Arrays.asList("settleMethod", "accountType", "accountName", "paymentVoucherNo", "voucherNo", "receiptUserName",
                "businessUserName", "developerName", "businessCreatorName", "businessDate"));
        if (amountVisible) fields.addAll(Arrays.asList("costAmount", "grossProfit", "grossProfitRate"));
        fields.addAll(Arrays.asList("customerType", "deptName", "remark"));
        return fields;
    }

    private void validate(Filter filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1 || filter.getPageSize() == null
                || filter.getPageSize() < 1 || filter.getPageSize() > 500) {
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        }
        validateRange(filter.getReceiptDateFrom(), filter.getReceiptDateTo(), "收款日期");
        validateRange(filter.getBusinessDateFrom(), filter.getBusinessDateTo(), "业务日期");
        Set<String> types = new LinkedHashSet<>(Arrays.asList(
                TYPE_SALE_OUT, TYPE_SALE_RETURN, TYPE_SALE_PRICE_ADJUST, TYPE_RECEIVABLE_MISC));
        if (filter.getBusinessTypes() != null && !types.containsAll(filter.getBusinessTypes())) {
            throw new ServiceException(400, "业务类型不正确");
        }
        if (filter.getAccountType() != null && filter.getAccountType() != 1
                && filter.getAccountType() != 2 && filter.getAccountType() != 3) {
            throw new ServiceException(400, "账户类型不正确");
        }
        if (hasText(filter.getOrderDirection()) && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
        for (String value : Arrays.asList(filter.getReceiptNo(), filter.getBusinessNo(),
                filter.getSettleMethod(), filter.getPaymentVoucherNo(), filter.getVoucherNo())) {
            if (value != null && value.length() > 100) {
                throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
            }
        }
    }

    private void validateRange(java.time.LocalDateTime from, java.time.LocalDateTime to, String label) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ServiceException(400, label + "范围不正确");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
