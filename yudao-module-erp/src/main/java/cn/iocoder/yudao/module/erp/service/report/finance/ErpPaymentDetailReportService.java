package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPaymentDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPaymentDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPaymentDetailReportModels.Summary;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPaymentDetailReportModels.*;

@Service
public class ErpPaymentDetailReportService {

    private static final String PAYMENT_MODULE = "erp_finance_payment";
    private static final String SUPPLIER_MODULE = "erp_supplier";
    private static final String ACCOUNT_MODULE = "erp_account";
    private static final String PURCHASE_IN_MODULE = "erp_purchase_in";
    private static final String PURCHASE_RETURN_MODULE = "erp_purchase_return";
    private static final String PURCHASE_ADJUST_MODULE = "erp_purchase_price_adjust";
    private static final String PAYABLE_MISC_MODULE = "erp_finance_payable_misc";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;

    @Resource private ErpPaymentDetailReportRepository repository;
    @Resource private PermissionApi permissionApi;
    @Resource private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validate(filter);
        clearHiddenVoucherFilters(filter);
        ErpPaymentDetailReportRepository.Scope scope = resolveScope();
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
        boolean visible = isAmountVisible();
        ErpPaymentDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return emptySummary(visible);
        Summary summary = repository.summary(filter, scope).setAmountVisible(visible);
        if (!visible) summary.setSettlementAmount(null).setActualPaymentAmount(null);
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validate(filter);
        clearHiddenVoucherFilters(filter);
        ErpPaymentDetailReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0 : repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        boolean amountVisible = isAmountVisible();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("付款明细查询.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(amountVisible)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("付款明细").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                applyVisibility(rows);
                writer.write(rows, sheet);
            }
        }
    }

    private ErpPaymentDetailReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, PAYMENT_MODULE);
        ErpFinanceVisibleScope paymentScope = ErpFinanceVisibleScope.from(permission, userId);
        if (!paymentScope.hasAccess()) return null;
        return new ErpPaymentDetailReportRepository.Scope(TenantContextHolder.getRequiredTenantId(), paymentScope);
    }

    private void applyVisibility(List<Row> rows) {
        boolean amountVisible = isAmountVisible();
        boolean voucherVisible = isVoucherVisible();
        Set<String> paymentHidden = hiddenFields(PAYMENT_MODULE);
        Set<String> supplierHidden = hiddenFields(SUPPLIER_MODULE);
        Set<String> accountHidden = hiddenFields(ACCOUNT_MODULE);
        Map<Integer, Set<String>> sourceHiddenByType = new java.util.HashMap<>();
        sourceHiddenByType.put(TYPE_PURCHASE_IN, hiddenFields(PURCHASE_IN_MODULE));
        sourceHiddenByType.put(TYPE_PURCHASE_RETURN, hiddenFields(PURCHASE_RETURN_MODULE));
        sourceHiddenByType.put(TYPE_PURCHASE_PRICE_ADJUST, hiddenFields(PURCHASE_ADJUST_MODULE));
        sourceHiddenByType.put(TYPE_PAYABLE_MISC, hiddenFields(PAYABLE_MISC_MODULE));
        for (Row row : rows) {
            row.setAmountVisible(amountVisible);
            if (!amountVisible) row.setSettlementAmount(null).setActualPaymentAmount(null);
            if (!voucherVisible) row.setVoucherNo(null);
            if (paymentHidden.contains("paymentVoucherNo")) row.setPaymentVoucherNo(null);
            if (paymentHidden.contains("no")) row.setPaymentNo(null);
            if (paymentHidden.contains("paymentTime")) row.setPaymentDate(null);
            if (paymentHidden.contains("creator")) row.setPayerName(null);
            if (paymentHidden.contains("accountId") || accountHidden.contains("name")) row.setAccountName(null);
            if (paymentHidden.contains("accountId") || accountHidden.contains("accountType")) {
                row.setPaymentMethod(null);
            }
            if (paymentHidden.contains("deptId")) row.setDeptName(null);
            if (paymentHidden.contains("supplierId")) row.setSupplierName(null).setSupplierType(null);
            if (paymentHidden.contains("bizNo") || paymentHidden.contains("items")) row.setBusinessNo(null);
            if (paymentHidden.contains("items")) row.setBusinessType(null);
            if (supplierHidden.contains("name")) row.setSupplierName(null);
            if (supplierHidden.contains("supplierType")) row.setSupplierType(null);
            Set<String> sourceHidden = sourceHiddenByType.getOrDefault(row.getBusinessTypeCode(),
                    Collections.emptySet());
            if (sourceHidden.contains("no")) row.setBusinessNo(null);
            if (sourceHidden.contains("creator")) row.setPurchaserName(null).setCreatorName(null);
            if (sourceHidden.contains("settleMethod")) row.setSettlementMethod(null);
        }
    }

    private boolean isAmountVisible() {
        Set<String> paymentHidden = hiddenFields(PAYMENT_MODULE);
        if (paymentHidden.contains("totalPrice") || paymentHidden.contains("paidPrice")
                || paymentHidden.contains("paymentPrice") || paymentHidden.contains("items")) {
            return false;
        }
        return !fieldPermissionMasker.isFieldHidden(PURCHASE_IN_MODULE, "totalPrice")
                && !fieldPermissionMasker.isFieldHidden(PURCHASE_RETURN_MODULE, "totalPrice")
                && !fieldPermissionMasker.isFieldHidden(PURCHASE_ADJUST_MODULE, "totalAdjustPrice")
                && !fieldPermissionMasker.isFieldHidden(PAYABLE_MISC_MODULE, "amount");
    }

    private boolean isVoucherVisible() {
        Long userId = getLoginUserId();
        return userId != null && permissionApi.hasAnyPermissions(userId, "erp:voucher:query")
                && !hiddenFields("erp_voucher").contains("voucherNo");
    }

    private void clearHiddenVoucherFilters(Filter filter) {
        if (hiddenFields(PAYMENT_MODULE).contains("paymentVoucherNo")) {
            filter.setPaymentVoucherNo(null);
        }
        if (!isVoucherVisible()) {
            filter.setVoucherNo(null);
        }
    }

    private Set<String> hiddenFields(String module) {
        Collection<String> fields = permissionApi.getCurrentUserHiddenFields(module);
        if (fields == null) return Collections.emptySet();
        return fields.stream().map(field -> field.startsWith("col_") ? field.substring(4) : field)
                .collect(Collectors.toSet());
    }

    private Summary emptySummary(boolean visible) {
        return new Summary().setRowCount(0L).setSettlementAmount(visible ? BigDecimal.ZERO : null)
                .setActualPaymentAmount(visible ? BigDecimal.ZERO : null).setAmountVisible(visible);
    }

    private Set<String> exportFields(boolean amountVisible) {
        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(Arrays.asList("supplierName", "paymentDate", "paymentNo", "businessNo", "businessType"));
        if (amountVisible) fields.addAll(Arrays.asList("settlementAmount", "actualPaymentAmount"));
        fields.addAll(Arrays.asList("accountName", "paymentVoucherNo", "voucherNo", "payerName", "purchaserName",
                "supplierType", "paymentMethod", "creatorName", "settlementMethod", "deptName"));
        return fields;
    }

    private void validate(Filter filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1 || filter.getPageSize() == null
                || filter.getPageSize() < 1 || filter.getPageSize() > 500) {
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        }
        if (filter.getPaymentDateFrom() != null && filter.getPaymentDateTo() != null
                && filter.getPaymentDateFrom().isAfter(filter.getPaymentDateTo())) {
            throw new ServiceException(400, "付款日期范围不正确");
        }
        Set<Integer> types = new LinkedHashSet<>(Arrays.asList(TYPE_PURCHASE_IN, TYPE_PURCHASE_RETURN,
                TYPE_PURCHASE_PRICE_ADJUST, TYPE_PAYABLE_MISC));
        if (filter.getBusinessTypes() != null && !types.containsAll(filter.getBusinessTypes())) {
            throw new ServiceException(400, "业务类型不正确");
        }
        if (hasText(filter.getOrderDirection()) && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
        for (String value : Arrays.asList(filter.getPaymentNo(), filter.getBusinessNo(), filter.getSupplierType(),
                filter.getSettlementMethod(), filter.getVoucherNo())) {
            if (value != null && value.length() > 100) {
                throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
