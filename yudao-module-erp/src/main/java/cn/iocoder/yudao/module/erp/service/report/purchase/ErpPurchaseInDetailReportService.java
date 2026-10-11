package cn.iocoder.yudao.module.erp.service.report.purchase;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.PriceEvent;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.Summary;
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
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.TYPE_ADJUST;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.TYPE_IN;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.TYPE_RETURN;

@Service
public class ErpPurchaseInDetailReportService {

    private static final String IN_MODULE = "erp_purchase_in";
    private static final String RETURN_MODULE = "erp_purchase_return";
    private static final String ADJUST_MODULE = "erp_purchase_price_adjust";
    private static final String PRODUCT_MODULE = "erp_product";
    private static final String SUPPLIER_MODULE = "erp_supplier";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;

    @Resource
    private ErpPurchaseInDetailReportRepository repository;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validate(filter);
        ErpPurchaseInDetailReportRepository.Scope scope = resolveScope();
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
        boolean priceVisible = isPriceVisible();
        ErpPurchaseInDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return emptySummary(priceVisible);
        Summary summary = repository.summary(filter, scope);
        summary.setPriceVisible(priceVisible);
        if (!priceVisible) {
            summary.setTotalAmount(null).setInvoiceAmount(null).setFreightAmount(null);
        }
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validate(filter);
        ErpPurchaseInDetailReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0L : repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        boolean priceVisible = isPriceVisible();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("配件入库明细查询报表.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(priceVisible)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("入库明细").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                enrichPreviousPrices(rows, scope);
                applyPriceVisibility(rows);
                writer.write(rows, sheet);
            }
        }
    }

    private void enrichPreviousPrices(List<Row> rows, ErpPurchaseInDetailReportRepository.Scope scope) {
        if (rows == null || rows.isEmpty()) return;
        Set<Long> productIds = new LinkedHashSet<>();
        for (Row row : rows) if (row.getProductId() != null) productIds.add(row.getProductId());
        Map<Long, List<PriceEvent>> eventsByProduct = new HashMap<>();
        for (PriceEvent event : repository.priceEvents(productIds, scope)) {
            eventsByProduct.computeIfAbsent(event.getProductId(), key -> new ArrayList<>()).add(event);
        }
        Comparator<PriceEvent> comparator = Comparator.comparing(PriceEvent::getEventTime)
                .thenComparing(PriceEvent::getDocumentTypeCode)
                .thenComparing(PriceEvent::getItemId);
        eventsByProduct.values().forEach(events -> events.sort(comparator));
        for (Row row : rows) {
            if (TYPE_ADJUST.equals(row.getDocumentTypeCode())) {
                row.setPreviousPurchasePrice(row.getProductPrice());
                continue;
            }
            row.setPreviousPurchasePrice(findPreviousPrice(
                    eventsByProduct.get(row.getProductId()), row.getApproveDate()));
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

    private ErpPurchaseInDetailReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        ErpFinanceVisibleScope inScope = visibleScope(userId, IN_MODULE);
        ErpFinanceVisibleScope returnScope = visibleScope(userId, RETURN_MODULE);
        ErpFinanceVisibleScope adjustScope = visibleScope(userId, ADJUST_MODULE);
        ErpFinanceVisibleScope supplierScope = visibleScope(userId, SUPPLIER_MODULE);
        if (!hasAccess(inScope) || !hasAccess(returnScope) || !hasAccess(adjustScope) || !hasAccess(supplierScope)) {
            return null;
        }
        return new ErpPurchaseInDetailReportRepository.Scope(TenantContextHolder.getRequiredTenantId(),
                inScope, returnScope, adjustScope, supplierScope);
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
                row.setProductPrice(null).setFreightIncludedPrice(null).setPreviousPurchasePrice(null)
                        .setTotalAmount(null).setInvoiceAmount(null).setFreight1(null);
            }
        }
    }

    private boolean isPriceVisible() {
        return !hidden(PRODUCT_MODULE, "purchasePrice")
                && !hidden(IN_MODULE, "item_productPrice") && !hidden(IN_MODULE, "item_totalPrice")
                && !hidden(IN_MODULE, "totalFreight1")
                && !hidden(RETURN_MODULE, "item_productPrice") && !hidden(RETURN_MODULE, "item_totalPrice")
                && !hidden(ADJUST_MODULE, "item_oldPrice") && !hidden(ADJUST_MODULE, "item_newPrice")
                && !hidden(ADJUST_MODULE, "item_adjustPrice");
    }

    private boolean hidden(String module, String field) {
        return fieldPermissionMasker.isFieldHidden(module, field);
    }

    private Summary emptySummary(boolean priceVisible) {
        return new Summary().setRowCount(0L).setTotalCount(BigDecimal.ZERO)
                .setTotalAmount(priceVisible ? BigDecimal.ZERO : null)
                .setInvoiceAmount(priceVisible ? BigDecimal.ZERO : null)
                .setFreightAmount(priceVisible ? BigDecimal.ZERO : null)
                .setPriceVisible(priceVisible);
    }

    private Set<String> exportFields(boolean priceVisible) {
        Set<String> fields = new LinkedHashSet<>();
        Collections.addAll(fields, "approveDate", "documentNo", "documentType", "sourceNo", "productCode",
                "productName", "vehicleModel", "featureCode", "count", "invoiceStatus", "originPlace",
                "factoryOrderNo", "brand", "supplierName", "supplierCode", "warehouseName", "warehousePosition",
                "purchaserName", "standard", "drawingNo", "unitName", "creatorName", "itemRemark",
                "documentRemark", "factoryCode", "factoryName", "factoryModel", "supplierRegion", "freightType1");
        if (priceVisible) {
            Collections.addAll(fields, "productPrice", "freightIncludedPrice", "previousPurchasePrice",
                    "totalAmount", "invoiceAmount", "freight1");
        }
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
        Set<String> types = Set.of(TYPE_IN, TYPE_RETURN, TYPE_ADJUST);
        if (filter.getDocumentTypes() != null && !types.containsAll(filter.getDocumentTypes())) {
            throw new ServiceException(400, "单据类型不正确");
        }
        if (hasText(filter.getInvoiceStatus()) && !Set.of("未开票", "部分开票", "已开票", "不适用")
                .contains(filter.getInvoiceStatus())) {
            throw new ServiceException(400, "开票状态不正确");
        }
        if (hasText(filter.getOrderDirection()) && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
        if (length(filter.getDocumentNo()) > 100 || length(filter.getSourceNo()) > 100
                || length(filter.getFactoryOrderNo()) > 100 || length(filter.getPurchaserKeyword()) > 100
                || length(filter.getProductKeyword()) > 100) {
            throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
