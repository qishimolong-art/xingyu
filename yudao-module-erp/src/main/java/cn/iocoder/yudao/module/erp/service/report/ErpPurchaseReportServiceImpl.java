package cn.iocoder.yudao.module.erp.service.report;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportDeptRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportTrendRespVO;
import cn.iocoder.yudao.module.erp.dal.mysql.report.ErpPurchaseReportMapper;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Validated
public class ErpPurchaseReportServiceImpl implements ErpPurchaseReportService {

    private static final String SUPPLIER_MODULE = "erp_supplier";
    private static final String REPORT_MODULE = "erp_purchase_report";
    private static final String PRODUCT_PRICE_PERMISSION_MODULE = "erp_product";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Resource
    private ErpPurchaseReportMapper purchaseReportMapper;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public ErpPurchaseReportSummaryRespVO getPurchaseReportSummary(ErpPurchaseReportPageReqVO reqVO) {
        List<ErpPurchaseReportDetailRespVO> rows = selectRows(reqVO);
        ErpPurchaseReportSummaryRespVO summary = new ErpPurchaseReportSummaryRespVO();
        Set<Long> supplierIds = new HashSet<>();
        for (ErpPurchaseReportDetailRespVO row : rows) {
            if (row.getSupplierId() != null) {
                supplierIds.add(row.getSupplierId());
            }
            summary.setDocCount(summary.getDocCount() + 1);
            if (Integer.valueOf(1).equals(row.getBizType())) {
                summary.setPurchaseCount(amount(summary.getPurchaseCount()).add(amount(row.getBizCount())));
                summary.setPurchaseAmount(amount(summary.getPurchaseAmount()).add(amount(row.getPurchaseAmount())));
            } else {
                summary.setReturnCount(amount(summary.getReturnCount()).add(amount(row.getBizCount())));
                summary.setReturnAmount(amount(summary.getReturnAmount()).add(amount(row.getReturnAmount())));
            }
            summary.setNetAmount(amount(summary.getNetAmount()).add(amount(row.getNetAmount())));
        }
        summary.setSupplierCount((long) supplierIds.size());
        maskSummary(summary, isPurchaseAmountHidden());
        return summary;
    }

    @Override
    public List<ErpPurchaseReportTrendRespVO> getPurchaseReportTrend(ErpPurchaseReportPageReqVO reqVO) {
        Map<String, ErpPurchaseReportTrendRespVO> trendMap = new LinkedHashMap<>();
        for (ErpPurchaseReportDetailRespVO row : selectRows(reqVO)) {
            if (row.getDocDate() == null) {
                continue;
            }
            String period = MONTH_FORMATTER.format(row.getDocDate());
            ErpPurchaseReportTrendRespVO trend = trendMap.computeIfAbsent(period, key -> {
                ErpPurchaseReportTrendRespVO item = new ErpPurchaseReportTrendRespVO();
                item.setPeriod(key);
                return item;
            });
            trend.setDocCount(trend.getDocCount() + 1);
            if (Integer.valueOf(1).equals(row.getBizType())) {
                trend.setPurchaseCount(amount(trend.getPurchaseCount()).add(amount(row.getBizCount())));
                trend.setPurchaseAmount(amount(trend.getPurchaseAmount()).add(amount(row.getPurchaseAmount())));
            } else {
                trend.setReturnCount(amount(trend.getReturnCount()).add(amount(row.getBizCount())));
                trend.setReturnAmount(amount(trend.getReturnAmount()).add(amount(row.getReturnAmount())));
            }
            trend.setNetAmount(amount(trend.getNetAmount()).add(amount(row.getNetAmount())));
        }
        List<ErpPurchaseReportTrendRespVO> list = new ArrayList<>(trendMap.values());
        maskTrend(list, isPurchaseAmountHidden());
        return list;
    }

    @Override
    public PageResult<ErpPurchaseReportRespVO> getPurchaseReportPage(ErpPurchaseReportPageReqVO reqVO) {
        List<ErpPurchaseReportRespVO> list = buildReportList(selectRows(reqVO));
        sortReportList(list, reqVO.getOrderField(), reqVO.getOrderDirection());
        maskReportList(list, isPurchaseAmountHidden());
        return buildPageResult(list, reqVO);
    }

    @Override
    public PageResult<ErpPurchaseReportProductRespVO> getPurchaseReportProductPage(ErpPurchaseReportPageReqVO reqVO) {
        List<ErpPurchaseReportProductRespVO> list = selectProductRows(reqVO);
        sortProductList(list, reqVO.getOrderField(), reqVO.getOrderDirection());
        maskProductList(list, isPurchaseAmountHidden());
        return buildPageResult(list, reqVO);
    }

    @Override
    public PageResult<ErpPurchaseReportDeptRespVO> getPurchaseReportDeptPage(ErpPurchaseReportPageReqVO reqVO) {
        List<ErpPurchaseReportDeptRespVO> list = buildDeptReportList(selectRows(reqVO));
        sortDeptList(list, reqVO.getOrderField(), reqVO.getOrderDirection());
        maskDeptList(list, isPurchaseAmountHidden());
        return buildPageResult(list, reqVO);
    }

    @Override
    public PageResult<ErpPurchaseReportDetailRespVO> getPurchaseReportDetailPage(ErpPurchaseReportPageReqVO reqVO) {
        List<ErpPurchaseReportDetailRespVO> list = selectRows(reqVO);
        if (isPurchaseAmountHidden()) {
            maskDetailList(list);
        }
        return buildPageResult(list, reqVO);
    }

    private <T> PageResult<T> buildPageResult(List<T> list, ErpPurchaseReportPageReqVO reqVO) {
        if (PageParam.PAGE_SIZE_NONE.equals(reqVO.getPageSize())) {
            return new PageResult<>(list, (long) list.size());
        }
        int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        int fromIndex = Math.max(0, (pageNo - 1) * pageSize);
        int toIndex = Math.min(list.size(), fromIndex + pageSize);
        if (fromIndex >= list.size()) {
            return PageResult.empty((long) list.size());
        }
        return new PageResult<>(list.subList(fromIndex, toIndex), (long) list.size());
    }

    @Override
    public List<ErpPurchaseReportDetailRespVO> getPurchaseReportDetailList(ErpPurchaseReportDetailReqVO reqVO) {
        ErpPurchaseReportPageReqVO pageReqVO = new ErpPurchaseReportPageReqVO();
        pageReqVO.setSupplierId(reqVO.getSupplierId());
        pageReqVO.setBizTime(reqVO.getBizTime());
        List<ErpPurchaseReportDetailRespVO> rows = selectRows(pageReqVO);
        if (isPurchaseAmountHidden()) {
            maskDetailList(rows);
        }
        return rows;
    }

    private List<ErpPurchaseReportRespVO> buildReportList(List<ErpPurchaseReportDetailRespVO> rows) {
        Map<Long, ErpPurchaseReportRespVO> map = new LinkedHashMap<>();
        for (ErpPurchaseReportDetailRespVO row : rows) {
            Long supplierId = row.getSupplierId();
            if (supplierId == null) {
                continue;
            }
            ErpPurchaseReportRespVO item = map.computeIfAbsent(supplierId, key -> buildReportItem(row));
            mergePurchaser(item, row);
            item.setDocCount(item.getDocCount() + 1);
            if (Integer.valueOf(1).equals(row.getBizType())) {
                item.setPurchaseCount(amount(item.getPurchaseCount()).add(amount(row.getBizCount())));
                item.setPurchaseAmount(amount(item.getPurchaseAmount()).add(amount(row.getPurchaseAmount())));
            } else {
                item.setReturnCount(amount(item.getReturnCount()).add(amount(row.getBizCount())));
                item.setReturnAmount(amount(item.getReturnAmount()).add(amount(row.getReturnAmount())));
            }
            item.setNetAmount(amount(item.getNetAmount()).add(amount(row.getNetAmount())));
            if (row.getDocDate() != null && (item.getLastBizTime() == null || row.getDocDate().isAfter(item.getLastBizTime()))) {
                item.setLastBizTime(row.getDocDate());
            }
        }
        return new ArrayList<>(map.values());
    }

    private ErpPurchaseReportRespVO buildReportItem(ErpPurchaseReportDetailRespVO row) {
        ErpPurchaseReportRespVO item = new ErpPurchaseReportRespVO();
        item.setSupplierId(row.getSupplierId());
        item.setSupplierName(row.getSupplierName());
        item.setContact(row.getContact());
        item.setMobile(row.getMobile());
        item.setDeptId(row.getDeptId());
        item.setDeptName(row.getDeptName());
        item.setPurchaser(row.getPurchaser());
        item.setPurchaserName(row.getPurchaserName());
        item.setDocCount(0L);
        item.setPurchaseCount(BigDecimal.ZERO);
        item.setPurchaseAmount(BigDecimal.ZERO);
        item.setReturnCount(BigDecimal.ZERO);
        item.setReturnAmount(BigDecimal.ZERO);
        item.setNetAmount(BigDecimal.ZERO);
        return item;
    }

    private List<ErpPurchaseReportDeptRespVO> buildDeptReportList(List<ErpPurchaseReportDetailRespVO> rows) {
        Map<Long, ErpPurchaseReportDeptRespVO> map = new LinkedHashMap<>();
        Map<Long, Set<Long>> supplierMap = new HashMap<>();
        for (ErpPurchaseReportDetailRespVO row : rows) {
            Long deptKey = row.getDeptId() == null ? 0L : row.getDeptId();
            ErpPurchaseReportDeptRespVO item = map.computeIfAbsent(deptKey, key -> buildDeptReportItem(row));
            if (row.getSupplierId() != null) {
                supplierMap.computeIfAbsent(deptKey, key -> new HashSet<>()).add(row.getSupplierId());
                item.setSupplierCount((long) supplierMap.get(deptKey).size());
            }
            item.setDocCount(item.getDocCount() + 1);
            if (Integer.valueOf(1).equals(row.getBizType())) {
                item.setPurchaseCount(amount(item.getPurchaseCount()).add(amount(row.getBizCount())));
                item.setPurchaseAmount(amount(item.getPurchaseAmount()).add(amount(row.getPurchaseAmount())));
            } else {
                item.setReturnCount(amount(item.getReturnCount()).add(amount(row.getBizCount())));
                item.setReturnAmount(amount(item.getReturnAmount()).add(amount(row.getReturnAmount())));
            }
            item.setNetAmount(amount(item.getNetAmount()).add(amount(row.getNetAmount())));
            if (row.getDocDate() != null && (item.getLastBizTime() == null || row.getDocDate().isAfter(item.getLastBizTime()))) {
                item.setLastBizTime(row.getDocDate());
            }
        }
        return new ArrayList<>(map.values());
    }

    private ErpPurchaseReportDeptRespVO buildDeptReportItem(ErpPurchaseReportDetailRespVO row) {
        ErpPurchaseReportDeptRespVO item = new ErpPurchaseReportDeptRespVO();
        item.setDeptId(row.getDeptId());
        item.setDeptName(row.getDeptName() == null ? "未设置部门" : row.getDeptName());
        item.setSupplierCount(0L);
        item.setDocCount(0L);
        item.setPurchaseCount(BigDecimal.ZERO);
        item.setPurchaseAmount(BigDecimal.ZERO);
        item.setReturnCount(BigDecimal.ZERO);
        item.setReturnAmount(BigDecimal.ZERO);
        item.setNetAmount(BigDecimal.ZERO);
        return item;
    }

    private void mergePurchaser(ErpPurchaseReportRespVO item, ErpPurchaseReportDetailRespVO row) {
        if (row.getPurchaserName() == null) {
            return;
        }
        if (item.getPurchaserName() == null) {
            item.setPurchaser(row.getPurchaser());
            item.setPurchaserName(row.getPurchaserName());
            return;
        }
        if (!item.getPurchaserName().equals(row.getPurchaserName())) {
            item.setPurchaser(null);
            item.setPurchaserName("多个采购员");
        }
    }

    private List<ErpPurchaseReportDetailRespVO> selectRows(ErpPurchaseReportPageReqVO reqVO) {
        ErpFinanceVisibleScope supplierScope = getVisibleScope(SUPPLIER_MODULE);
        ErpFinanceVisibleScope documentScope = getVisibleScope(REPORT_MODULE);
        if (!hasAccess(supplierScope) || !hasAccess(documentScope)) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> purchaseReportMapper.selectRows(reqVO,
                supplierScope.getDeptIds(), selfUserIdText(supplierScope), supplierScope.isAll(),
                documentScope.getDeptIds(), selfUserIdText(documentScope), documentScope.isAll()));
    }

    private List<ErpPurchaseReportProductRespVO> selectProductRows(ErpPurchaseReportPageReqVO reqVO) {
        ErpFinanceVisibleScope supplierScope = getVisibleScope(SUPPLIER_MODULE);
        ErpFinanceVisibleScope documentScope = getVisibleScope(REPORT_MODULE);
        if (!hasAccess(supplierScope) || !hasAccess(documentScope)) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> purchaseReportMapper.selectProductRows(reqVO,
                supplierScope.getDeptIds(), selfUserIdText(supplierScope), supplierScope.isAll(),
                documentScope.getDeptIds(), selfUserIdText(documentScope), documentScope.isAll()));
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

    private boolean isPurchaseAmountHidden() {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(PRODUCT_PRICE_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return false;
        }
        Set<String> fields = new HashSet<>(hiddenFields);
        return isProductPriceFieldHidden(fields, "lastPurchasePrice") || isProductPriceFieldHidden(fields, "purchasePrice");
    }

    private boolean isProductPriceFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey) || hiddenFields.contains("col_" + fieldKey);
    }

    private void sortReportList(List<ErpPurchaseReportRespVO> list, String orderField, String orderDirection) {
        Comparator<ErpPurchaseReportRespVO> comparator = getComparator(orderField);
        if (comparator == null) {
            comparator = getComparator("netAmount");
            orderDirection = "desc";
        }
        if ("desc".equals(orderDirection)) {
            comparator = comparator.reversed();
        }
        list.sort(comparator.thenComparing(item -> item.getSupplierId() == null ? 0L : item.getSupplierId()));
    }

    private Comparator<ErpPurchaseReportRespVO> getComparator(String orderField) {
        Map<String, Comparator<ErpPurchaseReportRespVO>> comparators = new HashMap<>();
        comparators.put("supplierName", Comparator.comparing(ErpPurchaseReportRespVO::getSupplierName,
                Comparator.nullsLast(String::compareTo)));
        comparators.put("deptName", Comparator.comparing(ErpPurchaseReportRespVO::getDeptName,
                Comparator.nullsLast(String::compareTo)));
        comparators.put("purchaserName", Comparator.comparing(ErpPurchaseReportRespVO::getPurchaserName,
                Comparator.nullsLast(String::compareTo)));
        comparators.put("purchaseAmount", Comparator.comparing(ErpPurchaseReportRespVO::getPurchaseAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("returnAmount", Comparator.comparing(ErpPurchaseReportRespVO::getReturnAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("netAmount", Comparator.comparing(ErpPurchaseReportRespVO::getNetAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("lastBizTime", Comparator.comparing(ErpPurchaseReportRespVO::getLastBizTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return comparators.get(orderField);
    }

    private void sortDeptList(List<ErpPurchaseReportDeptRespVO> list, String orderField, String orderDirection) {
        Comparator<ErpPurchaseReportDeptRespVO> comparator = getDeptComparator(orderField);
        if (comparator == null) {
            comparator = getDeptComparator("netAmount");
            orderDirection = "desc";
        }
        if ("desc".equals(orderDirection)) {
            comparator = comparator.reversed();
        }
        list.sort(comparator.thenComparing(item -> item.getDeptId() == null ? 0L : item.getDeptId()));
    }

    private Comparator<ErpPurchaseReportDeptRespVO> getDeptComparator(String orderField) {
        Map<String, Comparator<ErpPurchaseReportDeptRespVO>> comparators = new HashMap<>();
        comparators.put("deptName", Comparator.comparing(ErpPurchaseReportDeptRespVO::getDeptName,
                Comparator.nullsLast(String::compareTo)));
        comparators.put("supplierCount", Comparator.comparing(ErpPurchaseReportDeptRespVO::getSupplierCount,
                Comparator.nullsLast(Long::compareTo)));
        comparators.put("docCount", Comparator.comparing(ErpPurchaseReportDeptRespVO::getDocCount,
                Comparator.nullsLast(Long::compareTo)));
        comparators.put("purchaseCount", Comparator.comparing(ErpPurchaseReportDeptRespVO::getPurchaseCount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("purchaseAmount", Comparator.comparing(ErpPurchaseReportDeptRespVO::getPurchaseAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("returnCount", Comparator.comparing(ErpPurchaseReportDeptRespVO::getReturnCount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("returnAmount", Comparator.comparing(ErpPurchaseReportDeptRespVO::getReturnAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("netAmount", Comparator.comparing(ErpPurchaseReportDeptRespVO::getNetAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("lastBizTime", Comparator.comparing(ErpPurchaseReportDeptRespVO::getLastBizTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return comparators.get(orderField);
    }

    private void sortProductList(List<ErpPurchaseReportProductRespVO> list, String orderField, String orderDirection) {
        Comparator<ErpPurchaseReportProductRespVO> comparator = getProductComparator(orderField);
        if (comparator == null) {
            comparator = getProductComparator("netAmount");
            orderDirection = "desc";
        }
        if ("desc".equals(orderDirection)) {
            comparator = comparator.reversed();
        }
        list.sort(comparator.thenComparing(item -> item.getProductId() == null ? 0L : item.getProductId()));
    }

    private Comparator<ErpPurchaseReportProductRespVO> getProductComparator(String orderField) {
        Map<String, Comparator<ErpPurchaseReportProductRespVO>> comparators = new HashMap<>();
        comparators.put("productCode", Comparator.comparing(ErpPurchaseReportProductRespVO::getProductCode,
                Comparator.nullsLast(String::compareTo)));
        comparators.put("productName", Comparator.comparing(ErpPurchaseReportProductRespVO::getProductName,
                Comparator.nullsLast(String::compareTo)));
        comparators.put("supplierCount", Comparator.comparing(ErpPurchaseReportProductRespVO::getSupplierCount,
                Comparator.nullsLast(Long::compareTo)));
        comparators.put("docCount", Comparator.comparing(ErpPurchaseReportProductRespVO::getDocCount,
                Comparator.nullsLast(Long::compareTo)));
        comparators.put("purchaseCount", Comparator.comparing(ErpPurchaseReportProductRespVO::getPurchaseCount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("purchaseAmount", Comparator.comparing(ErpPurchaseReportProductRespVO::getPurchaseAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("returnCount", Comparator.comparing(ErpPurchaseReportProductRespVO::getReturnCount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("returnAmount", Comparator.comparing(ErpPurchaseReportProductRespVO::getReturnAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("netAmount", Comparator.comparing(ErpPurchaseReportProductRespVO::getNetAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("lastBizTime", Comparator.comparing(ErpPurchaseReportProductRespVO::getLastBizTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return comparators.get(orderField);
    }

    private void maskReportList(List<ErpPurchaseReportRespVO> list, boolean hidden) {
        if (!hidden) {
            return;
        }
        for (ErpPurchaseReportRespVO item : list) {
            item.setPurchaseAmount(null);
            item.setReturnAmount(null);
            item.setNetAmount(null);
        }
    }

    private void maskProductList(List<ErpPurchaseReportProductRespVO> list, boolean hidden) {
        if (!hidden) {
            return;
        }
        for (ErpPurchaseReportProductRespVO item : list) {
            item.setPurchaseAmount(null);
            item.setReturnAmount(null);
            item.setNetAmount(null);
        }
    }

    private void maskDeptList(List<ErpPurchaseReportDeptRespVO> list, boolean hidden) {
        if (!hidden) {
            return;
        }
        for (ErpPurchaseReportDeptRespVO item : list) {
            item.setPurchaseAmount(null);
            item.setReturnAmount(null);
            item.setNetAmount(null);
        }
    }

    private void maskDetailList(List<ErpPurchaseReportDetailRespVO> list) {
        for (ErpPurchaseReportDetailRespVO row : list) {
            row.setPurchaseAmount(null);
            row.setReturnAmount(null);
            row.setNetAmount(null);
        }
    }

    private void maskSummary(ErpPurchaseReportSummaryRespVO summary, boolean hidden) {
        if (!hidden) {
            return;
        }
        summary.setPurchaseAmount(null);
        summary.setReturnAmount(null);
        summary.setNetAmount(null);
    }

    private void maskTrend(List<ErpPurchaseReportTrendRespVO> list, boolean hidden) {
        if (!hidden) {
            return;
        }
        for (ErpPurchaseReportTrendRespVO item : list) {
            item.setPurchaseAmount(null);
            item.setReturnAmount(null);
            item.setNetAmount(null);
        }
    }

    private BigDecimal amount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
