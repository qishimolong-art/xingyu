package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.Summary;
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
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.*;

@Service
public class ErpAccountLedgerReportService {

    private static final String ACCOUNT_MODULE = "erp_account";
    private static final String RECEIPT_MODULE = "erp_finance_receipt";
    private static final String PAYMENT_MODULE = "erp_finance_payment";
    private static final String OTHER_INCOME_MODULE = "erp_finance_receivable_other_income";
    private static final String EXPENSE_MODULE = "erp_finance_payable_expense";
    private static final String TRANSFER_MODULE = "erp_finance_transfer";
    private static final String VOUCHER_MODULE = "erp_voucher";
    private static final String CUSTOMER_MODULE = "erp_customer";
    private static final String SUPPLIER_MODULE = "erp_supplier";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;

    @Resource private ErpAccountLedgerReportRepository repository;
    @Resource private PermissionApi permissionApi;
    @Resource private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validate(filter);
        clearHiddenVoucherFilters(filter);
        ErpAccountLedgerReportRepository.Scope scope = resolveScope();
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
        clearHiddenVoucherFilters(filter);
        boolean amountVisible = isAmountVisible();
        ErpAccountLedgerReportRepository.Scope scope = resolveScope();
        if (scope == null) return emptySummary(amountVisible);
        Summary summary = repository.summary(filter, scope).setAmountVisible(amountVisible);
        if (!amountVisible) {
            summary.setIncomeAmount(null).setExpenseAmount(null).setNetAmount(null);
        }
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validate(filter);
        clearHiddenVoucherFilters(filter);
        ErpAccountLedgerReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0 : repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        boolean amountVisible = isAmountVisible();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("账户流水报表.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(amountVisible)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("账户流水").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                applyVisibility(rows);
                writer.write(rows, sheet);
            }
        }
    }

    private ErpAccountLedgerReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, ACCOUNT_MODULE);
        ErpFinanceVisibleScope visibleScope = ErpFinanceVisibleScope.from(permission, userId);
        if (visibleScope == null || !visibleScope.hasAccess()) return null;
        return new ErpAccountLedgerReportRepository.Scope(
                TenantContextHolder.getRequiredTenantId(), visibleScope);
    }

    private void applyVisibility(List<Row> rows) {
        boolean amountVisible = isAmountVisible();
        boolean voucherVisible = isVoucherVisible();
        Set<String> accountHidden = hiddenFields(ACCOUNT_MODULE);
        Set<String> customerHidden = hiddenFields(CUSTOMER_MODULE);
        Set<String> supplierHidden = hiddenFields(SUPPLIER_MODULE);
        Map<String, Set<String>> sourceHiddenByType = new HashMap<>();
        for (String sourceType : Arrays.asList(TYPE_RECEIPT, TYPE_PAYMENT, TYPE_OTHER_INCOME,
                TYPE_EXPENSE, TYPE_TRANSFER)) {
            sourceHiddenByType.put(sourceType, hiddenFields(sourceModule(sourceType)));
        }
        for (Row row : rows) {
            row.setAmountVisible(amountVisible);
            if (!amountVisible) row.setIncomeAmount(null).setExpenseAmount(null).setBalance(null);
            if (!voucherVisible) row.setVoucherNo(null);
            if (hidden(accountHidden, "name")) row.setAccountName(null);
            Set<String> sourceHidden = sourceHiddenByType.getOrDefault(
                    row.getSourceType(), Collections.emptySet());
            if (hidden(sourceHidden, "no")) row.setBusinessNo(null);
            if (hidden(sourceHidden, "remark")) row.setRemark(null);
            if (hidden(sourceHidden, "creator", "creatorName")) {
                row.setReceiverName(null).setPayerName(null);
            }
            switch (row.getSourceType()) {
                case TYPE_RECEIPT:
                    if (hidden(sourceHidden, "receiptTime")) row.setBusinessDate(null);
                    if (hidden(sourceHidden, "accountId")) row.setAccountName(null);
                    if (hidden(sourceHidden, "customerId")) row.setPartyName(null).setPartyCode(null);
                    if (hidden(customerHidden, "name")) row.setPartyName(null);
                    if (hidden(customerHidden, "code")) row.setPartyCode(null);
                    if (hidden(sourceHidden, "paymentVoucherNo")) row.setPaymentVoucherNo(null);
                    break;
                case TYPE_PAYMENT:
                    if (hidden(sourceHidden, "paymentTime")) row.setBusinessDate(null);
                    if (hidden(sourceHidden, "accountId")) row.setAccountName(null);
                    if (hidden(sourceHidden, "supplierId")) row.setPartyName(null).setPartyCode(null);
                    if (hidden(supplierHidden, "name")) row.setPartyName(null);
                    if (hidden(supplierHidden, "code")) row.setPartyCode(null);
                    if (hidden(sourceHidden, "paymentVoucherNo")) row.setPaymentVoucherNo(null);
                    break;
                case TYPE_OTHER_INCOME:
                    if (hidden(sourceHidden, "bizTime")) row.setBusinessDate(null);
                    if (hidden(sourceHidden, "accountId")) row.setAccountName(null);
                    if (hidden(sourceHidden, "party")) row.setPartyName(null);
                    if (hidden(sourceHidden, "voucherNo")) row.setPaymentVoucherNo(null);
                    break;
                case TYPE_EXPENSE:
                    if (hidden(sourceHidden, "bizTime")) row.setBusinessDate(null);
                    if (hidden(sourceHidden, "accountId")) row.setAccountName(null);
                    if (hidden(sourceHidden, "party")) row.setPartyName(null);
                    if (hidden(sourceHidden, "voucherNo")) row.setPaymentVoucherNo(null);
                    break;
                case TYPE_TRANSFER:
                    if (hidden(sourceHidden, "transferTime")) row.setBusinessDate(null);
                    if (hidden(sourceHidden, "outAccountId", "inAccountId")) {
                        row.setAccountName(null).setPartyName(null).setPartyCode(null);
                    }
                    if (hidden(accountHidden, "name")) row.setPartyName(null);
                    if (hidden(accountHidden, "no")) row.setPartyCode(null);
                    break;
                default:
                    break;
            }
        }
    }

    private boolean isAmountVisible() {
        return !fieldPermissionMasker.isFieldHidden(ACCOUNT_MODULE, "currentBalance")
                && !fieldPermissionMasker.isFieldHidden(RECEIPT_MODULE, "receiptPrice")
                && !fieldPermissionMasker.isFieldHidden(PAYMENT_MODULE, "paymentPrice")
                && !fieldPermissionMasker.isFieldHidden(OTHER_INCOME_MODULE, "totalAmount")
                && !fieldPermissionMasker.isFieldHidden(EXPENSE_MODULE, "totalAmount")
                && !fieldPermissionMasker.isFieldHidden(TRANSFER_MODULE, "transferPrice")
                && !fieldPermissionMasker.isFieldHidden(TRANSFER_MODULE, "feePrice");
    }

    private boolean isVoucherVisible() {
        Long userId = getLoginUserId();
        return userId != null && permissionApi.hasAnyPermissions(userId, "erp:voucher:query")
                && !hiddenFields(VOUCHER_MODULE).contains("voucherNo");
    }

    private void clearHiddenVoucherFilters(Filter filter) {
        if (!isVoucherVisible()) {
            filter.setVoucherNo(null);
        }
        if (fieldPermissionMasker.isFieldHidden(RECEIPT_MODULE, "paymentVoucherNo")
                || fieldPermissionMasker.isFieldHidden(PAYMENT_MODULE, "paymentVoucherNo")
                || fieldPermissionMasker.isFieldHidden(OTHER_INCOME_MODULE, "voucherNo")
                || fieldPermissionMasker.isFieldHidden(EXPENSE_MODULE, "voucherNo")) {
            filter.setPaymentVoucherNo(null);
        }
    }

    private String sourceModule(String sourceType) {
        if (TYPE_RECEIPT.equals(sourceType)) return RECEIPT_MODULE;
        if (TYPE_PAYMENT.equals(sourceType)) return PAYMENT_MODULE;
        if (TYPE_OTHER_INCOME.equals(sourceType)) return OTHER_INCOME_MODULE;
        if (TYPE_EXPENSE.equals(sourceType)) return EXPENSE_MODULE;
        if (TYPE_TRANSFER.equals(sourceType)) return TRANSFER_MODULE;
        return ACCOUNT_MODULE;
    }

    private Set<String> hiddenFields(String module) {
        Collection<String> fields = permissionApi.getCurrentUserHiddenFields(module);
        if (fields == null) return Collections.emptySet();
        return fields.stream().map(field -> field.startsWith("col_") ? field.substring(4) : field)
                .collect(Collectors.toSet());
    }

    private boolean hidden(Set<String> hidden, String... fields) {
        return Arrays.stream(fields).anyMatch(hidden::contains);
    }

    private Summary emptySummary(boolean visible) {
        return new Summary().setRowCount(0L).setIncomeAmount(visible ? BigDecimal.ZERO : null)
                .setExpenseAmount(visible ? BigDecimal.ZERO : null).setNetAmount(visible ? BigDecimal.ZERO : null)
                .setAmountVisible(visible);
    }

    private Set<String> exportFields(boolean amountVisible) {
        Set<String> fields = new LinkedHashSet<>(Arrays.asList("partyName", "partyCode", "accountName",
                "businessDate", "businessNo", "documentType", "direction"));
        if (amountVisible) fields.addAll(Arrays.asList("incomeAmount", "expenseAmount", "balance"));
        fields.addAll(Arrays.asList("receiverName", "payerName", "remark", "paymentVoucherNo", "voucherNo",
                "onlinePaymentOrderNo", "onlinePaymentVoucherNo"));
        return fields;
    }

    private void validate(Filter filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1 || filter.getPageSize() == null
                || filter.getPageSize() < 1 || filter.getPageSize() > 500) {
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        }
        if (filter.getBusinessDateFrom() != null && filter.getBusinessDateTo() != null
                && filter.getBusinessDateFrom().isAfter(filter.getBusinessDateTo())) {
            throw new ServiceException(400, "业务日期范围不正确");
        }
        Set<String> types = new LinkedHashSet<>(Arrays.asList(TYPE_RECEIPT, TYPE_PAYMENT, TYPE_OTHER_INCOME,
                TYPE_EXPENSE, TYPE_TRANSFER));
        if (filter.getDocumentTypes() != null && !types.containsAll(filter.getDocumentTypes())) {
            throw new ServiceException(400, "单据类型不正确");
        }
        if (hasText(filter.getDirection()) && !DIRECTION_DEBIT.equals(filter.getDirection())
                && !DIRECTION_CREDIT.equals(filter.getDirection())) {
            throw new ServiceException(400, "方向不正确");
        }
        if (hasText(filter.getOrderDirection()) && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
        for (String value : Arrays.asList(filter.getPartyName(), filter.getPartyCode(), filter.getBusinessNo(),
                filter.getRemark(), filter.getPaymentVoucherNo(), filter.getVoucherNo())) {
            if (value != null && value.length() > 100) {
                throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
