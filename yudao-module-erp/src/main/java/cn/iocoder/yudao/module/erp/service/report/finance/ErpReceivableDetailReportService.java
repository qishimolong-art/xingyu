package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.Summary;
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
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.*;

@Service
public class ErpReceivableDetailReportService {

    private static final String ACCOUNT_MODULE = "erp_finance_receivable_account";
    private static final String CUSTOMER_MODULE = "erp_customer";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;

    @Resource private ErpReceivableDetailReportRepository repository;
    @Resource private PermissionApi permissionApi;
    @Resource private ErpSaleFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validate(filter);
        ErpReceivableDetailReportRepository.Scope scope = resolveScope();
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
        ErpReceivableDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return emptySummary(visible);
        Summary summary = repository.summary(filter, scope).setAmountVisible(visible);
        if (!visible) summary.setIncreaseAmount(null).setReceiptAmount(null).setWriteOffAmount(null).setEndingBalance(null);
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validate(filter);
        ErpReceivableDetailReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0 : repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        boolean visible = isAmountVisible();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("应收账明细查询.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(visible)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("应收账明细").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                applyVisibility(rows);
                writer.write(rows, sheet);
            }
        }
    }

    private ErpReceivableDetailReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        ErpFinanceVisibleScope account = visibleScope(userId, ACCOUNT_MODULE);
        ErpFinanceVisibleScope customer = visibleScope(userId, CUSTOMER_MODULE);
        if (account == null || customer == null || !account.hasAccess() || !customer.hasAccess()) return null;
        return new ErpReceivableDetailReportRepository.Scope(TenantContextHolder.getRequiredTenantId(), account, customer);
    }

    private ErpFinanceVisibleScope visibleScope(Long userId, String module) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, module);
        return ErpFinanceVisibleScope.from(permission, userId);
    }

    private void applyVisibility(List<Row> rows) {
        boolean amountVisible = isAmountVisible();
        boolean voucherVisible = isVoucherVisible();
        Map<String, Set<String>> sourceHiddenFields = new HashMap<>();
        sourceHiddenFields.put(TYPE_SALE_OUT, hiddenFields("erp_sale_out"));
        sourceHiddenFields.put(TYPE_SALE_RETURN, hiddenFields("erp_sale_return"));
        sourceHiddenFields.put(TYPE_SALE_ADJUST, hiddenFields("erp_sale_price_adjust"));
        sourceHiddenFields.put(TYPE_RECEIPT, hiddenFields("erp_finance_receipt"));
        sourceHiddenFields.put(TYPE_RECEIVABLE_ADJUST, hiddenFields("erp_finance_receivable_other"));
        Set<String> accountHidden = hiddenFields(ACCOUNT_MODULE);
        for (Row row : rows) {
            row.setAmountVisible(amountVisible);
            if (!amountVisible) row.setPreviousBalance(null).setIncreaseAmount(null).setReceiptAmount(null)
                    .setBalance(null).setUnconfirmedReceivable(null).setWriteOffAmount(null).setReceivedAmount(null);
            if (!voucherVisible) row.setVoucherNo(null);
            applySourceVisibility(row, sourceHiddenFields.getOrDefault(row.getDocumentTypeCode(), accountHidden),
                    accountHidden);
        }
    }

    private void applySourceVisibility(Row row, Set<String> sourceHidden, Set<String> accountHidden) {
        if (sourceHidden.contains("no")) row.setDocumentNo(null);
        if (isSourceNumberHidden(row.getDocumentTypeCode(), sourceHidden, accountHidden)) {
            row.setSourceNo(null).setSourceBusinessId(null).setSourceDocumentTypeCode(null);
        }
        if (accountHidden.contains("remark") || sourceHidden.contains("remark")) row.setDescription(null);
        if (sourceHidden.contains("settleMethod")) row.setSettleMethod(null);
        if (sourceHidden.contains("deliveryMethod")) row.setDeliveryMethod(null);
        if (sourceHidden.contains("creator")) row.setCreatorName(null);
        if (sourceHidden.contains("updater")) row.setUpdaterName(null);
        String businessUserField;
        switch (row.getDocumentTypeCode()) {
            case TYPE_SALE_OUT:
            case TYPE_SALE_RETURN:
                businessUserField = "saleUserId";
                break;
            case TYPE_SALE_ADJUST:
                businessUserField = "adjustUserId";
                break;
            case TYPE_RECEIPT:
                businessUserField = "financeUserId";
                break;
            case TYPE_RECEIVABLE_ADJUST:
                businessUserField = "handlerId";
                break;
            default:
                businessUserField = "operatorUserId";
        }
        if (sourceHidden.contains(businessUserField)) row.setBusinessUserName(null);
        if (TYPE_RECEIVABLE_ADJUST.equals(row.getDocumentTypeCode()) && sourceHidden.contains("voucherNo")) {
            row.setVoucherNo(null);
        }
    }

    private boolean isSourceNumberHidden(String documentType, Set<String> sourceHidden, Set<String> accountHidden) {
        if (accountHidden.contains("sourceNo") || accountHidden.contains("bizNo")
                || sourceHidden.contains("sourceNo")) return true;
        switch (documentType) {
            case TYPE_SALE_OUT:
                return sourceHidden.contains("orderNo");
            case TYPE_SALE_RETURN:
                return sourceHidden.contains("sourceOutNo");
            case TYPE_SALE_ADJUST:
                return sourceHidden.contains("originalSaleOutNo");
            case TYPE_RECEIPT:
                return sourceHidden.contains("items") || sourceHidden.contains("bizNo")
                        || sourceHidden.contains("item_bizNo");
            default:
                return false;
        }
    }

    private boolean isAmountVisible() {
        Set<String> hidden = hiddenFields(ACCOUNT_MODULE);
        if (hidden.stream().anyMatch(field -> field.toLowerCase().contains("amount")
                || field.toLowerCase().contains("balance") || field.toLowerCase().contains("price"))) return false;
        return !fieldPermissionMasker.isFieldHidden("erp_sale_out", "receiptPrice")
                && !fieldPermissionMasker.isFieldHidden("erp_sale_return", "refundPrice")
                && !fieldPermissionMasker.isFieldHidden("erp_sale_price_adjust", "totalAdjustPrice")
                && !fieldPermissionMasker.isFieldHidden("erp_finance_receipt", "totalPrice")
                && !fieldPermissionMasker.isFieldHidden("erp_finance_receipt", "receiptPrice")
                && !fieldPermissionMasker.isFieldHidden("erp_finance_receivable_other", "receivableAmount")
                && !fieldPermissionMasker.isFieldHidden("erp_finance_receivable_other", "settledAmount");
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
        return new Summary().setRowCount(0L).setIncreaseAmount(visible ? BigDecimal.ZERO : null)
                .setReceiptAmount(visible ? BigDecimal.ZERO : null).setWriteOffAmount(visible ? BigDecimal.ZERO : null)
                .setEndingBalance(visible ? BigDecimal.ZERO : null).setAmountVisible(visible);
    }

    private Set<String> exportFields(boolean visible) {
        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(Arrays.asList("businessDate", "customerName", "documentType", "settleMethod", "updaterName",
                "creatorName", "businessUserName", "deliveryMethod", "customerCode", "documentNo"));
        if (visible) fields.addAll(Arrays.asList("previousBalance", "increaseAmount", "receiptAmount", "balance",
                "unconfirmedReceivable"));
        fields.addAll(Arrays.asList("approveTime", "voucherNo"));
        if (visible) fields.addAll(Arrays.asList("writeOffAmount", "receivedAmount"));
        fields.addAll(Arrays.asList("sourceNo", "description", "settledStatus"));
        return fields;
    }

    private void validate(Filter filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1 || filter.getPageSize() == null
                || filter.getPageSize() < 1 || filter.getPageSize() > 500) throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        validateRange(filter.getBusinessDateFrom(), filter.getBusinessDateTo(), "业务日期");
        validateRange(filter.getApproveDateFrom(), filter.getApproveDateTo(), "审核时间");
        Set<String> types = new LinkedHashSet<>(Arrays.asList(TYPE_SALE_OUT, TYPE_SALE_RETURN, TYPE_SALE_ADJUST,
                TYPE_RECEIPT, TYPE_RECEIVABLE_ADJUST));
        if (filter.getDocumentTypes() != null && !types.containsAll(filter.getDocumentTypes()))
            throw new ServiceException(400, "单据类型不正确");
        if (hasText(filter.getSettledStatus()) && !Arrays.asList("是", "否", "-").contains(filter.getSettledStatus()))
            throw new ServiceException(400, "是否结算筛选值不正确");
        if (hasText(filter.getOrderDirection()) && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        for (String value : Arrays.asList(filter.getCustomerCode(), filter.getSettleMethod(), filter.getDeliveryMethod(),
                filter.getDocumentNo(), filter.getSourceNo(), filter.getVoucherNo())) if (value != null && value.length() > 100)
            throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
    }
    private void validateRange(java.time.LocalDateTime from, java.time.LocalDateTime to, String name) {
        if (from != null && to != null && from.isAfter(to)) throw new ServiceException(400, name + "范围不正确");
    }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
}
