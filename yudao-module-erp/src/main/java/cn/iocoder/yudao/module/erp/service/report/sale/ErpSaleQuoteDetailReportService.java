package cn.iocoder.yudao.module.erp.service.report.sale;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleQuoteDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleQuoteDetailReportModels.ItemProgress;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleQuoteDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleQuoteDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Service
public class ErpSaleQuoteDetailReportService {

    private static final String QUOTE_MODULE = "erp_sale_quote";
    private static final String CUSTOMER_MODULE = "erp_customer";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;

    @Resource
    private ErpSaleQuoteDetailReportRepository repository;
    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        validate(filter);
        ErpSaleQuoteDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return PageResult.empty();
        long total = repository.count(filter, scope);
        if (total == 0) return PageResult.empty();
        long offset = (long) (filter.getPageNo() - 1) * filter.getPageSize();
        List<Row> rows = repository.page(filter, scope, offset, filter.getPageSize());
        enrich(rows);
        applyPriceVisibility(rows);
        return new PageResult<>(rows, total);
    }

    @Transactional(readOnly = true)
    public Summary getSummary(Filter filter) {
        validate(filter);
        boolean visible = isPriceVisible();
        ErpSaleQuoteDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return emptySummary(visible);
        Summary summary = repository.summary(filter, scope);
        Summary progress = repository.progressSummary(filter, scope);
        summary.setActualOutCount(progress.getActualOutCount());
        summary.setPendingOutCount(progress.getPendingOutCount());
        summary.setPriceVisible(visible);
        if (!visible) summary.setTotalPrice(null);
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        validate(filter);
        ErpSaleQuoteDetailReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0L : repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        boolean visible = isPriceVisible();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("配件报价订单明细表.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(visible)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("报价订单明细").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                enrich(rows);
                applyPriceVisibility(rows);
                writer.write(rows, sheet);
            }
        }
    }

    private void enrich(List<Row> rows) {
        if (rows == null || rows.isEmpty()) return;
        Set<Long> itemIds = new LinkedHashSet<>();
        Set<Long> productIds = new LinkedHashSet<>();
        Set<Long> warehouseIds = new LinkedHashSet<>();
        for (Row row : rows) {
            if (row.getItemId() != null) itemIds.add(row.getItemId());
            if (row.getProductId() != null) productIds.add(row.getProductId());
            if (row.getWarehouseId() != null) warehouseIds.add(row.getWarehouseId());
        }
        long tenantId = TenantContextHolder.getRequiredTenantId();
        Map<Long, ItemProgress> progressMap = new HashMap<>();
        for (ItemProgress progress : repository.itemProgress(itemIds, tenantId)) {
            progressMap.put(progress.getItemId(), progress);
        }
        List<ErpStockDO> stocks = productIds.isEmpty() || warehouseIds.isEmpty() ? Collections.emptyList()
                : DataPermissionUtils.executeIgnore(() ->
                stockMapper.selectListByProductIdsAndWarehouseIds(productIds, warehouseIds));
        Map<String, ErpStockDO> stockMap = new HashMap<>();
        for (ErpStockDO stock : stocks) stockMap.put(key(stock.getProductId(), stock.getWarehouseId()), stock);
        Map<String, BigDecimal> occupiedMap = productIds.isEmpty() || warehouseIds.isEmpty()
                ? Collections.emptyMap() : DataPermissionUtils.executeIgnore(() ->
                stockMapper.selectOccupiedCountMap(productIds, warehouseIds));
        for (Row row : rows) {
            row.setCustomerRegion(ErpSaleReportAreaFormatter.format(row.getCustomerAreaId()));
            ItemProgress progress = progressMap.get(row.getItemId());
            BigDecimal converted = progress == null ? zero(row.getConvertedCount()) : zero(progress.getConvertedCount());
            BigDecimal actual = progress == null ? BigDecimal.ZERO : zero(progress.getActualOutCount());
            if (ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(row.getRawStatus()) && progress == null) {
                converted = zero(row.getCount());
                actual = zero(row.getCount());
            }
            row.setConvertedCount(converted).setActualOutCount(actual)
                    .setPendingOutCount(zero(row.getCount()).subtract(actual).max(BigDecimal.ZERO))
                    .setOrderStatus("已下订").setOutStatus(outStatusText(row.getRawStatus()));
            ErpStockDO stock = stockMap.get(key(row.getProductId(), row.getWarehouseId()));
            BigDecimal count = stock == null ? BigDecimal.ZERO : zero(stock.getCount());
            row.setStockCount(count).setAvailableCount(count.subtract(
                    occupiedMap.getOrDefault(key(row.getProductId(), row.getWarehouseId()), BigDecimal.ZERO)));
        }
    }

    private String outStatusText(Integer status) {
        if (ErpSaleQuoteStatusEnum.PART_CONVERTED_CART.getStatus().equals(status)) return "部分转手推车";
        if (ErpSaleQuoteStatusEnum.CONVERTED_CART.getStatus().equals(status)) return "全部转手推车";
        if (ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(status)) return "已出库";
        return "未转出";
    }

    private ErpSaleQuoteDetailReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        ErpFinanceVisibleScope quoteScope = visibleScope(userId, QUOTE_MODULE);
        ErpFinanceVisibleScope customerScope = visibleScope(userId, CUSTOMER_MODULE);
        if (!hasAccess(quoteScope) || !hasAccess(customerScope)) return null;
        return new ErpSaleQuoteDetailReportRepository.Scope(
                TenantContextHolder.getRequiredTenantId(), quoteScope, customerScope);
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
            if (!visible) row.setProductPrice(null).setTotalPrice(null);
        }
    }

    private boolean isPriceVisible() {
        return !fieldPermissionMasker.isFieldHidden(QUOTE_MODULE, "item_productPrice")
                && !fieldPermissionMasker.isFieldHidden(QUOTE_MODULE, "item_totalPrice");
    }

    private Summary emptySummary(boolean visible) {
        return new Summary().setRowCount(0L).setTotalCount(BigDecimal.ZERO)
                .setTotalPrice(visible ? BigDecimal.ZERO : null).setConvertedCount(BigDecimal.ZERO)
                .setActualOutCount(BigDecimal.ZERO).setPendingOutCount(BigDecimal.ZERO).setPriceVisible(visible);
    }

    private Set<String> exportFields(boolean visible) {
        Set<String> fields = new LinkedHashSet<>();
        Collections.addAll(fields, "quoteDate", "documentNo", "sourceNo", "orderStatus", "outStatus",
                "customerName", "customerCode", "customerRegion", "deptName", "saleUserName", "creatorName",
                "expectedDeliveryDate", "priceType", "productCode", "productName", "vehicleModel", "featureCode",
                "drawingNo", "standard", "brand", "originPlace", "unitName", "warehouseName", "warehousePosition",
                "giftFlagText", "count", "stockCountAtOrder", "stockCount", "availableCount", "stockMax", "stockMin",
                "stockStandard", "convertedCount", "actualOutCount", "pendingOutCount", "documentRemark");
        if (visible) Collections.addAll(fields, "productPrice", "totalPrice");
        return fields;
    }

    private void validate(Filter filter) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1
                || filter.getPageSize() == null || filter.getPageSize() < 1 || filter.getPageSize() > 500) {
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        }
        if (filter.getQuoteDateFrom() != null && filter.getQuoteDateTo() != null
                && filter.getQuoteDateFrom().isAfter(filter.getQuoteDateTo())) {
            throw new ServiceException(400, "报价日期范围不正确");
        }
        if (filter.getOrderStatus() != null && filter.getOrderStatus() != 20) {
            throw new ServiceException(400, "订单状态不正确");
        }
        if (filter.getOutStatus() != null && !Set.of(0, 30, 40, 50).contains(filter.getOutStatus())) {
            throw new ServiceException(400, "出库状态不正确");
        }
        if (hasText(filter.getOrderDirection()) && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
        if (length(filter.getDocumentNo()) > 100 || length(filter.getSourceNo()) > 100
                || length(filter.getProductKeyword()) > 100) {
            throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
        }
    }

    private String key(Long productId, Long warehouseId) {
        return productId + "_" + warehouseId;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
