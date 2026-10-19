package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpOtherIncomeReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpOtherIncomeReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpOtherIncomeReportModels.Summary;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Service
public class ErpOtherIncomeReportService {

    private static final String DATA_PERMISSION_MODULE = "erp_receivable_other_income";
    private static final String FIELD_PERMISSION_MODULE = "erp_finance_receivable_other_income";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;

    @Resource private ErpOtherIncomeReportRepository repository;
    @Resource private PermissionApi permissionApi;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validate(filter);
        ErpOtherIncomeReportRepository.Scope scope = resolveScope();
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
        ErpOtherIncomeReportRepository.Scope scope = resolveScope();
        if (scope == null) return emptySummary(amountVisible);
        Summary summary = repository.summary(filter, scope).setAmountVisible(amountVisible);
        if (!amountVisible) summary.setAmount(null);
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validate(filter);
        ErpOtherIncomeReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0 : repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("其他收入查询.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields()).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("其他收入查询").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                applyVisibility(rows);
                writer.write(rows, sheet);
            }
        }
    }

    private ErpOtherIncomeReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, DATA_PERMISSION_MODULE);
        ErpFinanceVisibleScope scope = ErpFinanceVisibleScope.from(permission, userId);
        if (scope == null || !scope.hasAccess()) return null;
        return new ErpOtherIncomeReportRepository.Scope(TenantContextHolder.getRequiredTenantId(), scope);
    }

    private void applyVisibility(List<Row> rows) {
        Set<String> hidden = hiddenFields();
        boolean amountVisible = isAmountVisible(hidden);
        for (Row row : rows) {
            row.setAmountVisible(amountVisible);
            if (isHidden(hidden, "approveTime", "auditTime", "updateTime", "approveDate")) {
                row.setApproveDate(null);
            }
            if (isHidden(hidden, "accountId", "accountName")) row.setAccountName(null);
            if (isHidden(hidden, "no", "documentNo")) row.setDocumentNo(null);
            if (isHidden(hidden, "items", "item_itemName", "itemName")) row.setItemName(null);
            if (!amountVisible) row.setAmount(null);
            if (hidden.contains("voucherNo")) row.setVoucherNo(null);
            if (isHidden(hidden, "items", "item_remark")) row.setSummary(null);
            if (isHidden(hidden, "deptId", "deptName", "billingDeptName")) row.setBillingDeptName(null);
            if (isHidden(hidden, "items", "item_deptId", "item_deptName")) row.setProjectDeptName(null);
            if (isHidden(hidden, "handlerId", "handlerName")) row.setHandlerName(null);
            if (hidden.contains("party")) row.setParty(null);
            if (isHidden(hidden, "items", "item_invoiceNo", "invoiceNo")) row.setInvoice(null);
            if (isHidden(hidden, "items", "item_bizDate", "bizDate")) row.setOccurrenceDate(null);
        }
    }

    private boolean isAmountVisible() {
        return isAmountVisible(hiddenFields());
    }

    private boolean isAmountVisible(Set<String> hidden) {
        return !hidden.contains("items") && !hidden.contains("amount") && !hidden.contains("item_amount")
                && !hidden.contains("totalAmount");
    }

    private Set<String> hiddenFields() {
        Collection<String> fields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        if (fields == null) return Collections.emptySet();
        Set<String> result = new LinkedHashSet<>();
        for (String field : fields) {
            result.add(field);
            if (field != null && field.startsWith("col_")) result.add(field.substring(4));
        }
        return result;
    }

    private boolean isHidden(Set<String> hidden, String... fields) {
        return Arrays.stream(fields).anyMatch(hidden::contains);
    }

    private Summary emptySummary(boolean visible) {
        return new Summary().setRowCount(0L).setAmount(visible ? BigDecimal.ZERO : null).setAmountVisible(visible);
    }

    private Set<String> exportFields() {
        return new LinkedHashSet<>(Arrays.asList("approveDate", "accountName", "documentNo", "itemName", "amount",
                "voucherNo", "summary", "type", "billingDeptName", "projectDeptName", "handlerName", "party",
                "invoice", "occurrenceDate"));
    }

    private void validate(Filter filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1 || filter.getPageSize() == null
                || filter.getPageSize() < 1 || filter.getPageSize() > 500) {
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        }
        validateRange(filter.getApproveDateFrom(), filter.getApproveDateTo(), "审核日期范围不正确");
        validateRange(filter.getOccurrenceDateFrom(), filter.getOccurrenceDateTo(), "发生日期范围不正确");
        if (filter.getAmountMin() != null && filter.getAmountMax() != null
                && filter.getAmountMin().compareTo(filter.getAmountMax()) > 0) {
            throw new ServiceException(400, "金额区间不正确");
        }
        if (hasText(filter.getOrderDirection()) && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
        for (String value : Arrays.asList(filter.getDocumentNo(), filter.getItemName(), filter.getVoucherNo(),
                filter.getSummary(), filter.getParty(), filter.getInvoice())) {
            if (value != null && value.length() > 100) {
                throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
            }
        }
    }

    private void validateRange(java.time.LocalDateTime from, java.time.LocalDateTime to, String message) {
        if (from != null && to != null && from.isAfter(to)) throw new ServiceException(400, message);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
