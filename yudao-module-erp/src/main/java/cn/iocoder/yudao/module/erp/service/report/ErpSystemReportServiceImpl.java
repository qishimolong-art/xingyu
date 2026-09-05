package cn.iocoder.yudao.module.erp.service.report;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportRankRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportTrendRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.report.ErpSystemReportMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class ErpSystemReportServiceImpl implements ErpSystemReportService {

    private static final String TYPE_PURCHASE = "purchase";
    private static final String TYPE_SALE = "sale";
    private static final String TYPE_STOCK = "stock";
    private static final String SYSTEM_REPORT_MODULE = "erp_system_report";
    private static final String SUPPLIER_MODULE = "erp_supplier";
    private static final String CUSTOMER_MODULE = "erp_customer";
    private static final String PRODUCT_PRICE_PERMISSION_MODULE = "erp_product";
    private static final int DEFAULT_RANK_LIMIT = 10;
    private static final int EXPORT_LIMIT = 50_000;

    @Resource
    private ErpSystemReportMapper systemReportMapper;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private ErpWarehouseService warehouseService;

    @Override
    public Map<String, Object> getOptions(String reportType, ErpSystemReportReqVO reqVO) {
        ErpFinanceVisibleScope reportScope = getVisibleScope(SYSTEM_REPORT_MODULE);
        if (!hasAccess(reportScope)) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("departments", dataPermissionDeptService.getDeptSimpleList(SYSTEM_REPORT_MODULE));
        result.put("users", getUserOptions(reqVO));
        if (TYPE_PURCHASE.equals(reportType)) {
            ErpFinanceVisibleScope supplierScope = getVisibleScope(SUPPLIER_MODULE);
            result.put("suppliers", hasAccess(supplierScope) ? DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectSupplierOptions(reqVO, supplierScope.getDeptIds(),
                            selfUserIdText(supplierScope), supplierScope.isAll())) : Collections.emptyList());
        } else if (TYPE_SALE.equals(reportType)) {
            ErpFinanceVisibleScope customerScope = getVisibleScope(CUSTOMER_MODULE);
            result.put("customers", hasAccess(customerScope) ? DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectCustomerOptions(reqVO, customerScope.getDeptIds(),
                            selfUserIdText(customerScope), customerScope.isAll())) : Collections.emptyList());
        } else if (TYPE_STOCK.equals(reportType)) {
            StockScope stockScope = getStockScope(reportScope);
            result.put("warehouses", stockScope.hasAccess() ? DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectWarehouseOptions(reqVO, stockScope.sqlWarehouseIds())) : Collections.emptyList());
            result.put("categories", stockScope.hasAccess() ? DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectCategoryOptions(reqVO, stockScope.sqlWarehouseIds())) : Collections.emptyList());
        }
        return result;
    }

    @Override
    public ErpSystemReportSummaryRespVO getSummary(String reportType, ErpSystemReportReqVO reqVO) {
        resolveTime(reqVO);
        if (TYPE_PURCHASE.equals(reportType)) {
            PermissionScope scope = getPartyDocumentScope(SUPPLIER_MODULE);
            if (!scope.hasAccess()) {
                return new ErpSystemReportSummaryRespVO();
            }
            ErpSystemReportSummaryRespVO summary = DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectPurchaseSummary(reqVO, scope.party.getDeptIds(), selfUserIdText(scope.party),
                            scope.party.isAll(), scope.document.getDeptIds(), selfUserIdText(scope.document),
                            scope.document.isAll()));
            maskPurchase(summary);
            return summary == null ? new ErpSystemReportSummaryRespVO() : summary;
        }
        if (TYPE_SALE.equals(reportType)) {
            PermissionScope scope = getPartyDocumentScope(CUSTOMER_MODULE);
            if (!scope.hasAccess()) {
                return new ErpSystemReportSummaryRespVO();
            }
            ErpSystemReportSummaryRespVO summary = DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectSaleSummary(reqVO, scope.party.getDeptIds(), selfUserIdText(scope.party),
                            scope.party.isAll(), scope.document.getDeptIds(), selfUserIdText(scope.document),
                            scope.document.isAll()));
            maskSale(summary);
            return summary == null ? new ErpSystemReportSummaryRespVO() : summary;
        }
        StockScope stockScope = getStockScope(getVisibleScope(SYSTEM_REPORT_MODULE));
        if (!stockScope.hasAccess()) {
            return new ErpSystemReportSummaryRespVO();
        }
        ErpSystemReportSummaryRespVO summary = DataPermissionUtils.executeIgnore(() ->
                systemReportMapper.selectStockSummary(reqVO, stockScope.sqlWarehouseIds()));
        maskStock(summary);
        return summary == null ? new ErpSystemReportSummaryRespVO() : summary;
    }

    @Override
    public List<ErpSystemReportTrendRespVO> getTrend(String reportType, ErpSystemReportReqVO reqVO) {
        resolveTime(reqVO);
        String periodExpr = periodExpression("stock".equals(reportType) ? "sr.biz_date" : "d.bizDate", reqVO.getGrain());
        if (TYPE_PURCHASE.equals(reportType)) {
            PermissionScope scope = getPartyDocumentScope(SUPPLIER_MODULE);
            if (!scope.hasAccess()) {
                return Collections.emptyList();
            }
            List<ErpSystemReportTrendRespVO> list = DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectPurchaseTrend(reqVO, periodExpr, scope.party.getDeptIds(),
                            selfUserIdText(scope.party), scope.party.isAll(), scope.document.getDeptIds(),
                            selfUserIdText(scope.document), scope.document.isAll()));
            maskPurchaseTrend(list);
            return list;
        }
        if (TYPE_SALE.equals(reportType)) {
            PermissionScope scope = getPartyDocumentScope(CUSTOMER_MODULE);
            if (!scope.hasAccess()) {
                return Collections.emptyList();
            }
            List<ErpSystemReportTrendRespVO> list = DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectSaleTrend(reqVO, periodExpr, scope.party.getDeptIds(),
                            selfUserIdText(scope.party), scope.party.isAll(), scope.document.getDeptIds(),
                            selfUserIdText(scope.document), scope.document.isAll()));
            maskSaleTrend(list);
            return list;
        }
        StockScope stockScope = getStockScope(getVisibleScope(SYSTEM_REPORT_MODULE));
        if (!stockScope.hasAccess()) {
            return Collections.emptyList();
        }
        List<ErpSystemReportTrendRespVO> list = DataPermissionUtils.executeIgnore(() ->
                systemReportMapper.selectStockTrend(reqVO, periodExpr, stockScope.sqlWarehouseIds()));
        maskStockTrend(list);
        return list;
    }

    @Override
    public List<ErpSystemReportRankRespVO> getRank(String reportType, ErpSystemReportReqVO reqVO) {
        resolveTime(reqVO);
        int limit = DEFAULT_RANK_LIMIT;
        if (TYPE_PURCHASE.equals(reportType)) {
            PermissionScope scope = getPartyDocumentScope(SUPPLIER_MODULE);
            if (!scope.hasAccess()) {
                return Collections.emptyList();
            }
            String orderBy = rankOrder(reqVO.getMetric(), TYPE_PURCHASE);
            List<ErpSystemReportRankRespVO> list = DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectPurchaseRank(reqVO, orderBy, limit, scope.party.getDeptIds(),
                            selfUserIdText(scope.party), scope.party.isAll(), scope.document.getDeptIds(),
                            selfUserIdText(scope.document), scope.document.isAll()));
            maskPurchaseRank(list);
            return list;
        }
        if (TYPE_SALE.equals(reportType)) {
            PermissionScope scope = getPartyDocumentScope(CUSTOMER_MODULE);
            if (!scope.hasAccess()) {
                return Collections.emptyList();
            }
            String orderBy = rankOrder(reqVO.getMetric(), TYPE_SALE);
            List<ErpSystemReportRankRespVO> list = DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectSaleRank(reqVO, orderBy, limit, scope.party.getDeptIds(),
                            selfUserIdText(scope.party), scope.party.isAll(), scope.document.getDeptIds(),
                            selfUserIdText(scope.document), scope.document.isAll()));
            maskSaleRank(list);
            return list;
        }
        StockScope stockScope = getStockScope(getVisibleScope(SYSTEM_REPORT_MODULE));
        if (!stockScope.hasAccess()) {
            return Collections.emptyList();
        }
        String orderBy = rankOrder(reqVO.getMetric(), TYPE_STOCK);
        List<ErpSystemReportRankRespVO> list = DataPermissionUtils.executeIgnore(() ->
                systemReportMapper.selectStockRank(reqVO, orderBy, limit, stockScope.sqlWarehouseIds()));
        maskStockRank(list);
        return list;
    }

    @Override
    public PageResult<Map<String, Object>> getPage(String reportType, ErpSystemReportReqVO reqVO) {
        resolveTime(reqVO);
        reqVO.setDimension(defaultDimension(reportType, reqVO.getDimension()));
        int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        int limit = PageParam.PAGE_SIZE_NONE.equals(pageSize) ? EXPORT_LIMIT : pageSize;
        int offset = PageParam.PAGE_SIZE_NONE.equals(pageSize) ? 0 : Math.max(0, (pageNo - 1) * pageSize);
        String orderBy = pageOrder(reportType, reqVO.getDimension(), reqVO.getOrderField(), reqVO.getOrderDirection());
        PageResult<Map<String, Object>> pageResult;
        if (TYPE_PURCHASE.equals(reportType)) {
            PermissionScope scope = getPartyDocumentScope(SUPPLIER_MODULE);
            if (!scope.hasAccess()) {
                return PageResult.empty(0L);
            }
            Long total = DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.countPurchasePage(reqVO, reqVO.getDimension(), scope.party.getDeptIds(),
                            selfUserIdText(scope.party), scope.party.isAll(), scope.document.getDeptIds(),
                            selfUserIdText(scope.document), scope.document.isAll()));
            List<Map<String, Object>> rows = DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectPurchasePage(reqVO, reqVO.getDimension(), orderBy, limit, offset,
                            scope.party.getDeptIds(), selfUserIdText(scope.party), scope.party.isAll(),
                            scope.document.getDeptIds(), selfUserIdText(scope.document), scope.document.isAll()));
            maskPurchaseRows(rows);
            pageResult = new PageResult<>(rows, total == null ? 0L : total);
        } else if (TYPE_SALE.equals(reportType)) {
            PermissionScope scope = getPartyDocumentScope(CUSTOMER_MODULE);
            if (!scope.hasAccess()) {
                return PageResult.empty(0L);
            }
            Long total = DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.countSalePage(reqVO, reqVO.getDimension(), scope.party.getDeptIds(),
                            selfUserIdText(scope.party), scope.party.isAll(), scope.document.getDeptIds(),
                            selfUserIdText(scope.document), scope.document.isAll()));
            List<Map<String, Object>> rows = DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectSalePage(reqVO, reqVO.getDimension(), orderBy, limit, offset,
                            scope.party.getDeptIds(), selfUserIdText(scope.party), scope.party.isAll(),
                            scope.document.getDeptIds(), selfUserIdText(scope.document), scope.document.isAll()));
            maskSaleRows(rows);
            pageResult = new PageResult<>(rows, total == null ? 0L : total);
        } else {
            StockScope stockScope = getStockScope(getVisibleScope(SYSTEM_REPORT_MODULE));
            if (!stockScope.hasAccess()) {
                return PageResult.empty(0L);
            }
            Long total = DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.countStockPage(reqVO, reqVO.getDimension(), stockScope.sqlWarehouseIds()));
            List<Map<String, Object>> rows = DataPermissionUtils.executeIgnore(() ->
                    systemReportMapper.selectStockPage(reqVO, reqVO.getDimension(), orderBy, limit, offset,
                            stockScope.sqlWarehouseIds()));
            maskStockRows(rows);
            pageResult = new PageResult<>(rows, total == null ? 0L : total);
        }
        return pageResult;
    }

    @Override
    public List<Map<String, Object>> getExportList(String reportType, ErpSystemReportReqVO reqVO) {
        reqVO.setPageNo(1);
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        return getPage(reportType, reqVO).getList();
    }

    private PermissionScope getPartyDocumentScope(String partyModule) {
        return new PermissionScope(getVisibleScope(partyModule), getVisibleScope(SYSTEM_REPORT_MODULE));
    }

    private ErpFinanceVisibleScope getVisibleScope(String module) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        DeptDataPermissionRespDTO permission = loginUserId == null ? null
                : permissionApi.getDeptDataPermission(loginUserId, module);
        return ErpFinanceVisibleScope.from(permission, loginUserId);
    }

    private boolean hasAccess(ErpFinanceVisibleScope scope) {
        return scope != null && scope.hasAccess();
    }

    private String selfUserIdText(ErpFinanceVisibleScope scope) {
        return scope.getSelfUserId() == null ? null : String.valueOf(scope.getSelfUserId());
    }

    private StockScope getStockScope(ErpFinanceVisibleScope reportScope) {
        if (!hasAccess(reportScope)) {
            return StockScope.empty();
        }
        ErpProductStockPermissionScope warehouseScope = warehouseService.getCurrentUserProductStockPermissionScope();
        if (warehouseScope == null || (!warehouseScope.isAll() && CollUtil.isEmpty(warehouseScope.getVisibleWarehouseIds()))) {
            return StockScope.empty();
        }
        if (reportScope.isAll() && warehouseScope.isAll()) {
            return StockScope.all();
        }
        Set<Long> warehouseIds = new LinkedHashSet<>(warehouseScope.getVisibleWarehouseIds());
        if (!reportScope.isAll()) {
            Set<Long> reportWarehouseIds = warehouseService.getCurrentUserStockVisibleWarehouseList().stream()
                    .filter(warehouse -> reportScope.getDeptIds().contains(warehouse.getDeptId())
                            || Objects.equals(warehouse.getCreator(), selfUserIdText(reportScope)))
                    .map(ErpWarehouseDO::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            warehouseIds.retainAll(reportWarehouseIds);
        }
        return warehouseIds.isEmpty() ? StockScope.empty() : new StockScope(false, warehouseIds);
    }

    private List<Map<String, Object>> getUserOptions(ErpSystemReportReqVO reqVO) {
        List<AdminUserRespDTO> users = DataPermissionUtils.executeIgnore(() -> {
            if (reqVO.getKeyword() != null && !reqVO.getKeyword().trim().isEmpty()) {
                return adminUserApi.getUserListByNickname(reqVO.getKeyword().trim());
            }
            return adminUserApi.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        if (CollUtil.isEmpty(users)) {
            return Collections.emptyList();
        }
        return users.stream().limit(100).map(user -> {
            Map<String, Object> item = new HashMap<>();
            item.put("value", user.getId());
            item.put("label", user.getNickname());
            return item;
        }).collect(Collectors.toList());
    }

    private void resolveTime(ErpSystemReportReqVO reqVO) {
        if ("custom".equals(reqVO.getPeriodType()) && reqVO.getStartTime() != null && reqVO.getEndTime() != null) {
            return;
        }
        LocalDate today = LocalDate.now();
        String periodType = reqVO.getPeriodType() == null ? "month" : reqVO.getPeriodType();
        LocalDate start;
        LocalDate end;
        switch (periodType) {
            case "today":
                start = today;
                end = today;
                break;
            case "week":
                start = today.with(DayOfWeek.MONDAY);
                end = today.with(DayOfWeek.SUNDAY);
                break;
            case "quarter":
                int firstMonth = ((today.getMonthValue() - 1) / 3) * 3 + 1;
                start = LocalDate.of(today.getYear(), firstMonth, 1);
                end = start.plusMonths(3).minusDays(1);
                break;
            case "year":
                start = today.with(TemporalAdjusters.firstDayOfYear());
                end = today.with(TemporalAdjusters.lastDayOfYear());
                break;
            case "month":
            default:
                start = today.with(TemporalAdjusters.firstDayOfMonth());
                end = today.with(TemporalAdjusters.lastDayOfMonth());
                break;
        }
        reqVO.setStartTime(start.atStartOfDay());
        reqVO.setEndTime(end.atTime(LocalTime.MAX));
    }

    private String periodExpression(String dateColumn, String grain) {
        if ("week".equals(grain)) {
            return "DATE_FORMAT(" + dateColumn + ", '%x-%v')";
        }
        if ("month".equals(grain)) {
            return "DATE_FORMAT(" + dateColumn + ", '%Y-%m')";
        }
        return "DATE_FORMAT(" + dateColumn + ", '%Y-%m-%d')";
    }

    private String defaultDimension(String reportType, String dimension) {
        if (dimension != null && !dimension.trim().isEmpty()) {
            return dimension;
        }
        if (TYPE_SALE.equals(reportType)) {
            return "customer";
        }
        if (TYPE_STOCK.equals(reportType)) {
            return "product";
        }
        return "supplier";
    }

    private String rankOrder(String metric, String reportType) {
        if (TYPE_STOCK.equals(reportType)) {
            if ("stockQty".equals(metric)) {
                return "SUM(r.stockQty)";
            }
            return "SUM(r.stockAmount)";
        }
        if ("inAmount".equals(metric) || "purchaseAmount".equals(metric) || "saleAmount".equals(metric)) {
            return TYPE_SALE.equals(reportType) ? "SUM(r.saleAmount)" : "SUM(r.inAmount)";
        }
        if ("returnAmount".equals(metric)) {
            return "SUM(r.returnAmount)";
        }
        if ("pendingQty".equals(metric) && TYPE_PURCHASE.equals(reportType)) {
            return "SUM(r.pendingQty)";
        }
        return "SUM(r.netAmount)";
    }

    private String pageOrder(String reportType, String dimension, String orderField, String orderDirection) {
        Map<String, String> fields = new HashMap<>();
        fields.put("name", "name");
        fields.put("code", "code");
        fields.put("userName", "userName");
        fields.put("deptName", "deptName");
        fields.put("docDate", "docDate");
        fields.put("docNo", "docNo");
        fields.put("inCount", "inCount");
        fields.put("inAmount", "inAmount");
        fields.put("purchaseAmount", "inAmount");
        fields.put("returnCount", "returnCount");
        fields.put("returnAmount", "returnAmount");
        fields.put("netAmount", "netAmount");
        fields.put("pendingQty", "pendingQty");
        fields.put("saleCount", "saleCount");
        fields.put("saleAmount", "saleAmount");
        fields.put("stockQty", "stockQty");
        fields.put("stockAmount", "stockAmount");
        fields.put("gapQty", "gapQty");
        fields.put("lowStockSku", "lowStockSku");
        String field = fields.get(orderField);
        if (field == null) {
            if ("document".equals(dimension)) {
                field = "docDate";
            } else if (TYPE_STOCK.equals(reportType)) {
                field = "stockAmount";
            } else {
                field = "netAmount";
            }
        }
        String direction = "asc".equalsIgnoreCase(orderDirection) ? "ASC" : "DESC";
        return field + " " + direction;
    }

    private boolean isPurchaseAmountHidden() {
        Set<String> fields = hiddenPriceFields();
        return isFieldHidden(fields, "lastPurchasePrice") || isFieldHidden(fields, "purchasePrice");
    }

    private boolean isSaleAmountHidden() {
        return isFieldHidden(hiddenPriceFields(), "salePrice");
    }

    private boolean isStockAmountHidden() {
        return isFieldHidden(hiddenPriceFields(), "lastPurchasePrice");
    }

    private Set<String> hiddenPriceFields() {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(PRODUCT_PRICE_PERMISSION_MODULE);
        return CollUtil.isEmpty(hiddenFields) ? Collections.emptySet() : new HashSet<>(hiddenFields);
    }

    private boolean isFieldHidden(Set<String> fields, String field) {
        return fields.contains(field) || fields.contains("col_" + field);
    }

    private void maskPurchase(ErpSystemReportSummaryRespVO summary) {
        if (summary != null && isPurchaseAmountHidden()) {
            summary.setPurchaseAmount(null).setInAmount(null).setReturnAmount(null).setNetAmount(null);
        }
    }

    private void maskSale(ErpSystemReportSummaryRespVO summary) {
        if (summary != null && isSaleAmountHidden()) {
            summary.setSaleAmount(null).setReturnAmount(null).setNetAmount(null);
        }
    }

    private void maskStock(ErpSystemReportSummaryRespVO summary) {
        if (summary != null && isStockAmountHidden()) {
            summary.setStockAmount(null);
        }
    }

    private void maskPurchaseTrend(List<ErpSystemReportTrendRespVO> list) {
        if (isPurchaseAmountHidden()) {
            list.forEach(item -> item.setPurchaseAmount(null).setInAmount(null).setReturnAmount(null).setNetAmount(null));
        }
    }

    private void maskSaleTrend(List<ErpSystemReportTrendRespVO> list) {
        if (isSaleAmountHidden()) {
            list.forEach(item -> item.setSaleAmount(null).setReturnAmount(null).setNetAmount(null));
        }
    }

    private void maskStockTrend(List<ErpSystemReportTrendRespVO> list) {
        if (isStockAmountHidden()) {
            list.forEach(item -> item.setInAmount(null).setOutAmount(null).setStockAmount(null));
        }
    }

    private void maskPurchaseRank(List<ErpSystemReportRankRespVO> list) {
        if (isPurchaseAmountHidden()) {
            list.forEach(item -> item.setValue(null).setInAmount(null).setReturnAmount(null).setNetAmount(null));
        }
    }

    private void maskSaleRank(List<ErpSystemReportRankRespVO> list) {
        if (isSaleAmountHidden()) {
            list.forEach(item -> item.setValue(null).setSaleAmount(null).setReturnAmount(null).setNetAmount(null));
        }
    }

    private void maskStockRank(List<ErpSystemReportRankRespVO> list) {
        if (isStockAmountHidden()) {
            list.forEach(item -> item.setValue(null).setStockAmount(null));
        }
    }

    private void maskPurchaseRows(List<Map<String, Object>> rows) {
        if (isPurchaseAmountHidden()) {
            rows.forEach(row -> maskKeys(row, "inAmount", "purchaseAmount", "returnAmount", "netAmount"));
        }
    }

    private void maskSaleRows(List<Map<String, Object>> rows) {
        if (isSaleAmountHidden()) {
            rows.forEach(row -> maskKeys(row, "saleAmount", "returnAmount", "netAmount"));
        }
    }

    private void maskStockRows(List<Map<String, Object>> rows) {
        if (isStockAmountHidden()) {
            rows.forEach(row -> maskKeys(row, "stockAmount"));
        }
    }

    private void maskKeys(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            row.put(key, null);
        }
    }

    private static class PermissionScope {
        private final ErpFinanceVisibleScope party;
        private final ErpFinanceVisibleScope document;

        private PermissionScope(ErpFinanceVisibleScope party, ErpFinanceVisibleScope document) {
            this.party = party;
            this.document = document;
        }

        private boolean hasAccess() {
            return party != null && party.hasAccess() && document != null && document.hasAccess();
        }
    }

    private static class StockScope {
        private final boolean all;
        private final Set<Long> warehouseIds;

        private StockScope(boolean all, Set<Long> warehouseIds) {
            this.all = all;
            this.warehouseIds = warehouseIds;
        }

        private static StockScope all() {
            return new StockScope(true, Collections.emptySet());
        }

        private static StockScope empty() {
            return new StockScope(false, Collections.emptySet());
        }

        private boolean hasAccess() {
            return all || CollUtil.isNotEmpty(warehouseIds);
        }

        private Set<Long> sqlWarehouseIds() {
            return all ? null : warehouseIds;
        }
    }

}
