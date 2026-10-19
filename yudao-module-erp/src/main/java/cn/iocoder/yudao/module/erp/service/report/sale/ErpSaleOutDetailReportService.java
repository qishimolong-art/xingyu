package cn.iocoder.yudao.module.erp.service.report.sale;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.PriceEvent;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.Summary;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.TYPE_ADJUST;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.TYPE_OUT;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.TYPE_RETURN;

@Service
public class ErpSaleOutDetailReportService {

    private static final String OUT_MODULE = "erp_sale_out";
    private static final String RETURN_MODULE = "erp_sale_return";
    private static final String ADJUST_MODULE = "erp_sale_price_adjust";
    private static final String CUSTOMER_MODULE = "erp_customer";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;

    @Resource
    private ErpSaleOutDetailReportRepository repository;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validate(filter);
        ErpSaleOutDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return PageResult.empty();
        long total = repository.count(filter, scope);
        if (total == 0) return PageResult.empty();
        long offset = (long) (filter.getPageNo() - 1) * filter.getPageSize();
        List<Row> rows = repository.page(filter, scope, offset, filter.getPageSize());
        enrichPreviousPrices(rows, scope);
        applyPriceVisibility(rows);
        return new PageResult<>(rows, total);
    }

    @Transactional(readOnly = true)
    public Summary getSummary(Filter filter) {
        validate(filter);
        boolean visible = isPriceVisible();
        ErpSaleOutDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return emptySummary(visible);
        Summary summary = repository.summary(filter, scope).setPriceVisible(visible);
        if (!visible) summary.setTotalAmount(null).setInvoiceAmount(null).setFreightAmount(null);
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validate(filter);
        ErpSaleOutDetailReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0L : repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        boolean visible = isPriceVisible();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("配件出库明细查询报表.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(visible)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("出库明细").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                enrichPreviousPrices(rows, scope);
                applyPriceVisibility(rows);
                writer.write(rows, sheet);
            }
        }
    }

    private void enrichPreviousPrices(List<Row> rows, ErpSaleOutDetailReportRepository.Scope scope) {
        if (rows == null || rows.isEmpty()) return;
        Set<Long> customerIds = new LinkedHashSet<>();
        Set<Long> productIds = new LinkedHashSet<>();
        for (Row row : rows) {
            if (row.getCustomerId() != null) customerIds.add(row.getCustomerId());
            if (row.getProductId() != null) productIds.add(row.getProductId());
        }
        Map<String, List<PriceEvent>> eventsByKey = new HashMap<>();
        for (PriceEvent event : repository.priceEvents(customerIds, productIds, scope)) {
            eventsByKey.computeIfAbsent(key(event.getCustomerId(), event.getProductId()), value -> new ArrayList<>())
                    .add(event);
        }
        Comparator<PriceEvent> comparator = Comparator.comparing(PriceEvent::getEventTime)
                .thenComparing(PriceEvent::getDocumentTypeCode).thenComparing(PriceEvent::getItemId);
        eventsByKey.values().forEach(events -> events.sort(comparator));
        for (Row row : rows) {
            row.setCustomerRegion(ErpSaleReportAreaFormatter.format(row.getCustomerAreaId()));
            if (TYPE_ADJUST.equals(row.getDocumentTypeCode())) {
                row.setPreviousSalePrice(row.getProductPrice());
            } else {
                row.setPreviousSalePrice(findPreviousPrice(
                        eventsByKey.get(key(row.getCustomerId(), row.getProductId())), row.getApproveDate()));
            }
        }
    }

    static BigDecimal findPreviousPrice(List<PriceEvent> events, LocalDateTime currentApproveTime) {
        if (events == null || currentApproveTime == null) return null;
        BigDecimal value = null;
        for (PriceEvent event : events) {
            if (event.getEventTime() == null || !event.getEventTime().isBefore(currentApproveTime)) break;
            if (event.getPrice() != null) value = event.getPrice();
        }
        return value;
    }

    private ErpSaleOutDetailReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        ErpFinanceVisibleScope outScope = visibleScope(userId, OUT_MODULE);
        ErpFinanceVisibleScope returnScope = visibleScope(userId, RETURN_MODULE);
        ErpFinanceVisibleScope adjustScope = visibleScope(userId, ADJUST_MODULE);
        ErpFinanceVisibleScope customerScope = visibleScope(userId, CUSTOMER_MODULE);
        if (!hasAccess(outScope) || !hasAccess(returnScope) || !hasAccess(adjustScope)
                || !hasAccess(customerScope)) return null;
        return new ErpSaleOutDetailReportRepository.Scope(TenantContextHolder.getRequiredTenantId(),
                outScope, returnScope, adjustScope, customerScope);
    }

    private ErpFinanceVisibleScope visibleScope(Long userId, String module) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, module);
        return ErpFinanceVisibleScope.from(permission, userId);
    }

    private boolean hasAccess(ErpFinanceVisibleScope scope) {
        return scope != null && scope.hasAccess();
    }

    private void applyPriceVisibility(List<Row> rows) {
        boolean visible = isPriceVisible();
        for (Row row : rows) {
            row.setPriceVisible(visible);
            if (!visible) {
                row.setProductPrice(null).setFreightIncludedPrice(null).setPreviousSalePrice(null)
                        .setTotalAmount(null).setInvoiceAmount(null).setSaleCostPrice(null).setSaleCostAmount(null)
                        .setGrossProfitAmount(null).setGrossProfitRate(null).setFreightAmount(null);
            }
        }
    }

    private boolean isPriceVisible() {
        return !hidden(OUT_MODULE, "item_productPrice") && !hidden(OUT_MODULE, "item_totalPrice")
                && !hidden(OUT_MODULE, "freight") && !hidden(OUT_MODULE, "invoiceAmount")
                && !hidden(RETURN_MODULE, "item_productPrice") && !hidden(RETURN_MODULE, "item_totalPrice")
                && !hidden(ADJUST_MODULE, "item_oldPrice") && !hidden(ADJUST_MODULE, "item_newPrice")
                && !hidden(ADJUST_MODULE, "item_adjustPrice");
    }

    private boolean hidden(String module, String field) {
        return fieldPermissionMasker.isFieldHidden(module, field);
    }

    private Summary emptySummary(boolean visible) {
        return new Summary().setRowCount(0L).setTotalCount(BigDecimal.ZERO)
                .setTotalAmount(visible ? BigDecimal.ZERO : null)
                .setInvoiceAmount(visible ? BigDecimal.ZERO : null)
                .setFreightAmount(visible ? BigDecimal.ZERO : null).setPriceVisible(visible);
    }

    private Set<String> exportFields(boolean visible) {
        Set<String> fields = new LinkedHashSet<>();
        Collections.addAll(fields, "approveDate", "documentNo", "documentType", "sourceNo", "thirdPartyNo",
                "productCode", "productName", "vehicleModel", "featureCode", "count", "invoiceStatus",
                "originPlace", "brand", "customerName", "customerCode", "customerRegion", "deptName",
                "warehouseName", "warehousePosition", "saleUserName", "standard", "drawingNo", "unitName",
                "giftFlagText", "creatorName", "itemRemark", "documentRemark", "factoryCode", "factoryName",
                "factoryModel", "freightType");
        if (visible) Collections.addAll(fields, "productPrice", "freightIncludedPrice", "previousSalePrice",
                "totalAmount", "invoiceAmount", "saleCostPrice", "saleCostAmount", "grossProfitAmount",
                "grossProfitRate", "freightAmount");
        return fields;
    }

    private void validate(Filter filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1
                || filter.getPageSize() == null || filter.getPageSize() < 1 || filter.getPageSize() > 500) {
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        }
        if (filter.getApproveDateFrom() != null && filter.getApproveDateTo() != null
                && filter.getApproveDateFrom().isAfter(filter.getApproveDateTo())) {
            throw new ServiceException(400, "审核日期范围不正确");
        }
        if (filter.getDocumentTypes() != null
                && !Set.of(TYPE_OUT, TYPE_RETURN, TYPE_ADJUST).containsAll(filter.getDocumentTypes())) {
            throw new ServiceException(400, "单据类型不正确");
        }
        if (hasText(filter.getInvoiceStatus())
                && !Set.of("未开票", "部分开票", "已开票", "不适用").contains(filter.getInvoiceStatus())) {
            throw new ServiceException(400, "开票状态不正确");
        }
        if (hasText(filter.getOrderDirection()) && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
        if (length(filter.getDocumentNo()) > 100 || length(filter.getSourceNo()) > 100
                || length(filter.getThirdPartyNo()) > 100 || length(filter.getSaleUserKeyword()) > 100
                || length(filter.getProductKeyword()) > 100) {
            throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
        }
    }

    private String key(Long customerId, Long productId) {
        return customerId + "_" + productId;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
