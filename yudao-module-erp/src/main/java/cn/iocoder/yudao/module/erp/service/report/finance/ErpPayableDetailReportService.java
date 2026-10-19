package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableDetailReportModels.Summary;
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
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableDetailReportModels.*;

@Service
public class ErpPayableDetailReportService {

    private static final String ACCOUNT_MODULE = "erp_finance_payable_account";
    private static final String SUPPLIER_MODULE = "erp_supplier";
    private static final String PURCHASE_IN_MODULE = "erp_purchase_in";
    private static final String PURCHASE_RETURN_MODULE = "erp_purchase_return";
    private static final String PURCHASE_ADJUST_MODULE = "erp_purchase_price_adjust";
    private static final String PAYABLE_ADJUST_MODULE = "erp_finance_payable_other";
    private static final String PAYMENT_MODULE = "erp_finance_payment";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;

    @Resource private ErpPayableDetailReportRepository repository;
    @Resource private PermissionApi permissionApi;
    @Resource private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validate(filter);
        ErpPayableDetailReportRepository.Scope scope = resolveScope();
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
        boolean visible = isAmountVisible();
        ErpPayableDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return emptySummary(visible);
        Summary summary = repository.summary(filter, scope).setAmountVisible(visible);
        if (!visible) {
            summary.setIncreasePayable(null).setPaymentAmount(null).setEndingPayable(null);
        }
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validate(filter);
        ErpPayableDetailReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0 : repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        boolean visible = isAmountVisible();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("应付账明细查询.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(visible)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("应付账明细").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                applyVisibility(rows);
                writer.write(rows, sheet);
            }
        }
    }

    private ErpPayableDetailReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        ErpFinanceVisibleScope account = visibleScope(userId, ACCOUNT_MODULE);
        ErpFinanceVisibleScope supplier = visibleScope(userId, SUPPLIER_MODULE);
        if (account == null || supplier == null || !account.hasAccess() || !supplier.hasAccess()) return null;
        return new ErpPayableDetailReportRepository.Scope(TenantContextHolder.getRequiredTenantId(), account, supplier);
    }

    private ErpFinanceVisibleScope visibleScope(Long userId, String module) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, module);
        return ErpFinanceVisibleScope.from(permission, userId);
    }

    private void applyVisibility(List<Row> rows) {
        boolean amountVisible = isAmountVisible();
        boolean voucherVisible = isVoucherVisible();
        Set<String> supplierHidden = hiddenFields(SUPPLIER_MODULE);
        Set<String> accountHidden = hiddenFields(ACCOUNT_MODULE);
        Map<String, Set<String>> sourceHiddenFields = new HashMap<>();
        sourceHiddenFields.put(TYPE_PURCHASE_IN, hiddenFields(PURCHASE_IN_MODULE));
        sourceHiddenFields.put(TYPE_PURCHASE_RETURN, hiddenFields(PURCHASE_RETURN_MODULE));
        sourceHiddenFields.put(TYPE_PURCHASE_ADJUST, hiddenFields(PURCHASE_ADJUST_MODULE));
        sourceHiddenFields.put(TYPE_PAYABLE_ADJUST, hiddenFields(PAYABLE_ADJUST_MODULE));
        sourceHiddenFields.put(TYPE_PAYMENT, hiddenFields(PAYMENT_MODULE));
        for (Row row : rows) {
            row.setAmountVisible(amountVisible);
            if (!amountVisible) {
                row.setIncreasePayable(null).setPaymentAmount(null).setPreviousBalance(null).setEndingPayable(null);
            }
            if (!voucherVisible) row.setVoucherNo(null);
            if (supplierHidden.contains("code")) row.setSupplierCode(null);
            if (supplierHidden.contains("name")) row.setSupplierName(null);
            if (supplierHidden.contains("supplierType")) row.setSupplierType(null);
            Set<String> sourceHidden = sourceHiddenFields.getOrDefault(row.getDocumentTypeCode(), accountHidden);
            if (accountHidden.contains("remark") || sourceHidden.contains("remark")) row.setRemark(null);
            if (sourceHidden.contains("creator")) row.setCreatorName(null);
            if (TYPE_PAYABLE_ADJUST.equals(row.getDocumentTypeCode()) && sourceHidden.contains("voucherNo")) {
                row.setVoucherNo(null);
            }
        }
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

    private boolean isVoucherVisible() {
        Long userId = getLoginUserId();
        return userId != null && permissionApi.hasAnyPermissions(userId, "erp:voucher:query")
                && !hiddenFields("erp_voucher").contains("voucherNo");
    }

    private Set<String> hiddenFields(String module) {
        Collection<String> fields = permissionApi.getCurrentUserHiddenFields(module);
        if (fields == null) return Collections.emptySet();
        return fields.stream().map(field -> field.startsWith("col_") ? field.substring(4) : field)
                .collect(Collectors.toSet());
    }

    private Summary emptySummary(boolean visible) {
        return new Summary().setRowCount(0L).setIncreasePayable(visible ? BigDecimal.ZERO : null)
                .setPaymentAmount(visible ? BigDecimal.ZERO : null)
                .setEndingPayable(visible ? BigDecimal.ZERO : null).setAmountVisible(visible);
    }

    private Set<String> exportFields(boolean visible) {
        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(Arrays.asList("supplierCode", "supplierName", "documentDate", "documentType", "documentNo",
                "creatorName"));
        if (visible) {
            fields.addAll(Arrays.asList("increasePayable", "paymentAmount", "previousBalance", "endingPayable"));
        }
        fields.addAll(Arrays.asList("voucherNo", "remark", "supplierType"));
        return fields;
    }

    private void validate(Filter filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1 || filter.getPageSize() == null
                || filter.getPageSize() < 1 || filter.getPageSize() > 500) {
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        }
        if (filter.getDocumentDateFrom() != null && filter.getDocumentDateTo() != null
                && filter.getDocumentDateFrom().isAfter(filter.getDocumentDateTo())) {
            throw new ServiceException(400, "单据日期范围不正确");
        }
        Set<String> types = new LinkedHashSet<>(Arrays.asList(TYPE_PURCHASE_IN, TYPE_PURCHASE_RETURN,
                TYPE_PURCHASE_ADJUST, TYPE_PAYABLE_ADJUST, TYPE_PAYMENT));
        if (filter.getDocumentTypes() != null && !types.containsAll(filter.getDocumentTypes())) {
            throw new ServiceException(400, "单据类型不正确");
        }
        if (hasText(filter.getOrderDirection()) && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
        for (String value : Arrays.asList(filter.getSupplierCode(), filter.getSupplierType(),
                filter.getDocumentNo(), filter.getVoucherNo())) {
            if (value != null && value.length() > 100) {
                throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
