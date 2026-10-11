package cn.iocoder.yudao.module.erp.service.report.purchase;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseOrderDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseOrderDetailReportModels.ProductWarehousePair;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseOrderDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseOrderDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Service
public class ErpPurchaseOrderDetailReportService {

    private static final String PURCHASE_ORDER_MODULE = "erp_purchase_order";
    private static final String SUPPLIER_MODULE = "erp_supplier";
    private static final int EXPORT_BATCH_SIZE = 1000;

    @Resource
    private ErpPurchaseOrderDetailReportRepository repository;
    @Resource
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validateFilter(filter);
        ErpPurchaseOrderDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return PageResult.empty();
        long total = repository.count(filter, scope);
        if (total == 0) return PageResult.empty();
        long offset = (long) (filter.getPageNo() - 1) * filter.getPageSize();
        List<Row> rows = repository.page(filter, scope, offset, filter.getPageSize());
        enrich(rows);
        maskPrices(rows);
        return new PageResult<>(rows, total);
    }

    @Transactional(readOnly = true)
    public Summary getSummary(Filter filter) {
        validateFilter(filter);
        boolean priceVisible = isPriceVisible();
        ErpPurchaseOrderDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return emptySummary(priceVisible);
        Summary summary = repository.summary(filter, scope);
        List<ProductWarehousePair> pairs = repository.distinctPairs(filter, scope);
        Map<String, BigDecimal> transitMap = getTransitMap(pairs);
        Map<String, BigDecimal> pendingMap = getPendingMap(pairs);
        summary.setInTransitCount(sumPairs(pairs, transitMap));
        summary.setPendingInCount(sumPairs(pairs, pendingMap));
        summary.setPriceVisible(priceVisible);
        if (!priceVisible) summary.setTotalPrice(null);
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validateFilter(filter);
        ErpPurchaseOrderDetailReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0L : repository.count(filter, scope);
        if (total > 1_000_000L) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        boolean priceVisible = isPriceVisible();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("配件订货明细表.xlsx", "UTF-8").replace("+", "%20"));
        Set<String> fields = exportFields(priceVisible);
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(fields).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("订货明细").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                enrich(rows);
                maskPrices(rows);
                writer.write(rows, sheet);
            }
        }
    }

    private void enrich(List<Row> rows) {
        if (rows == null || rows.isEmpty()) return;
        List<ProductWarehousePair> pairs = new ArrayList<>();
        for (Row row : rows) {
            pairs.add(new ProductWarehousePair().setProductId(row.getProductId()).setWarehouseId(row.getWarehouseId()));
        }
        Map<String, BigDecimal> transitMap = getTransitMap(pairs);
        Map<String, BigDecimal> pendingMap = getPendingMap(pairs);
        boolean priceVisible = isPriceVisible();
        for (Row row : rows) {
            String key = key(row.getProductId(), row.getWarehouseId());
            row.setInTransitCount(transitMap.getOrDefault(key, BigDecimal.ZERO));
            row.setPendingInCount(pendingMap.getOrDefault(key, BigDecimal.ZERO));
            row.setPriceVisible(priceVisible);
        }
    }

    private Map<String, BigDecimal> getTransitMap(Collection<ProductWarehousePair> pairs) {
        Set<Long> productIds = new LinkedHashSet<>();
        Set<Long> warehouseIds = new LinkedHashSet<>();
        collectPairIds(pairs, productIds, warehouseIds);
        if (productIds.isEmpty() || warehouseIds.isEmpty()) return Collections.emptyMap();
        return DataPermissionUtils.executeIgnore(() -> purchaseOrderItemMapper.selectInTransitCountMap(
                productIds, warehouseIds, Collections.singletonList(ErpAuditStatus.APPROVE.getStatus())));
    }

    private Map<String, BigDecimal> getPendingMap(Collection<ProductWarehousePair> pairs) {
        Set<Long> productIds = new LinkedHashSet<>();
        Set<Long> warehouseIds = new LinkedHashSet<>();
        collectPairIds(pairs, productIds, warehouseIds);
        if (productIds.isEmpty() || warehouseIds.isEmpty()) return Collections.emptyMap();
        return DataPermissionUtils.executeIgnore(() -> stockMapper.selectPendingInCountMap(productIds, warehouseIds));
    }

    private void collectPairIds(Collection<ProductWarehousePair> pairs, Set<Long> productIds,
                                Set<Long> warehouseIds) {
        for (ProductWarehousePair pair : pairs) {
            if (pair.getProductId() != null) productIds.add(pair.getProductId());
            if (pair.getWarehouseId() != null) warehouseIds.add(pair.getWarehouseId());
        }
    }

    private BigDecimal sumPairs(Collection<ProductWarehousePair> pairs, Map<String, BigDecimal> values) {
        Set<String> seen = new LinkedHashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ProductWarehousePair pair : pairs) {
            String key = key(pair.getProductId(), pair.getWarehouseId());
            if (seen.add(key)) total = total.add(values.getOrDefault(key, BigDecimal.ZERO));
        }
        return total;
    }

    private String key(Long productId, Long warehouseId) {
        return productId + "_" + warehouseId;
    }

    private ErpPurchaseOrderDetailReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        ErpFinanceVisibleScope documentScope = visibleScope(userId, PURCHASE_ORDER_MODULE);
        ErpFinanceVisibleScope supplierScope = visibleScope(userId, SUPPLIER_MODULE);
        if (documentScope == null || supplierScope == null
                || !documentScope.hasAccess() || !supplierScope.hasAccess()) return null;
        return new ErpPurchaseOrderDetailReportRepository.Scope(
                TenantContextHolder.getRequiredTenantId(), documentScope, supplierScope);
    }

    private ErpFinanceVisibleScope visibleScope(Long userId, String module) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, module);
        return ErpFinanceVisibleScope.from(permission, userId);
    }

    private void maskPrices(List<Row> rows) {
        if (isPriceVisible()) return;
        for (Row row : rows) row.setProductPrice(null).setTotalPrice(null).setPriceVisible(false);
    }

    private boolean isPriceVisible() {
        return !fieldPermissionMasker.isFieldHidden(PURCHASE_ORDER_MODULE, "item_productPrice")
                && !fieldPermissionMasker.isFieldHidden(PURCHASE_ORDER_MODULE, "item_totalPrice");
    }

    private Summary emptySummary(boolean priceVisible) {
        return new Summary().setRowCount(0L).setTotalCount(BigDecimal.ZERO)
                .setTotalPrice(priceVisible ? BigDecimal.ZERO : null)
                .setInTransitCount(BigDecimal.ZERO).setPendingInCount(BigDecimal.ZERO)
                .setOrderInTransitCount(BigDecimal.ZERO).setPriceVisible(priceVisible);
    }

    private Set<String> exportFields(boolean priceVisible) {
        Set<String> fields = new LinkedHashSet<>();
        Collections.addAll(fields, "orderDate", "factoryOrderNo", "productName", "supplierName", "vehicleModel",
                "productCode", "warehouseName", "warehousePosition", "drawingNo", "standard", "brand",
                "originPlace", "purchaserName", "creatorName", "orderNo", "count", "inTransitCount",
                "pendingInCount", "stockCountAtOrder", "stockMax", "stockMin", "stockStandard",
                "orderInTransitCount", "expectedArrivalDate", "documentRemark", "featureCode");
        if (priceVisible) {
            fields.add("productPrice");
            fields.add("totalPrice");
        }
        return fields;
    }

    private void validateFilter(Filter filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1
                || filter.getPageSize() == null || filter.getPageSize() < 1 || filter.getPageSize() > 500) {
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        }
        if (filter.getOrderDateFrom() != null && filter.getOrderDateTo() != null
                && filter.getOrderDateFrom().isAfter(filter.getOrderDateTo())) {
            throw new ServiceException(400, "订货日期范围不正确");
        }
        if (hasText(filter.getOrderDirection()) && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
        if (length(filter.getOrderNo()) > 100 || length(filter.getFactoryOrderNo()) > 100
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
