package cn.iocoder.yudao.module.erp.service.report;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportDeptRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportTrendRespVO;
import cn.iocoder.yudao.module.erp.dal.mysql.report.ErpSaleReportMapper;
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
public class ErpSaleReportServiceImpl implements ErpSaleReportService {

    private static final String CUSTOMER_MODULE = "erp_customer";
    private static final String REPORT_MODULE = "erp_sale_report";
    private static final String PRODUCT_PRICE_PERMISSION_MODULE = "erp_product";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Resource
    private ErpSaleReportMapper saleReportMapper;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public ErpSaleReportSummaryRespVO getSaleReportSummary(ErpSaleReportPageReqVO reqVO) {
        List<ErpSaleReportDetailRespVO> rows = selectRows(reqVO);
        ErpSaleReportSummaryRespVO summary = new ErpSaleReportSummaryRespVO();
        Set<Long> customerIds = new HashSet<>();
        for (ErpSaleReportDetailRespVO row : rows) {
            if (row.getCustomerId() != null) {
                customerIds.add(row.getCustomerId());
            }
            summary.setDocCount(summary.getDocCount() + 1);
            if (Integer.valueOf(1).equals(row.getBizType())) {
                summary.setSaleCount(amount(summary.getSaleCount()).add(amount(row.getBizCount())));
                summary.setSaleAmount(amount(summary.getSaleAmount()).add(amount(row.getSaleAmount())));
            } else {
                summary.setReturnCount(amount(summary.getReturnCount()).add(amount(row.getBizCount())));
                summary.setReturnAmount(amount(summary.getReturnAmount()).add(amount(row.getReturnAmount())));
            }
            summary.setNetAmount(amount(summary.getNetAmount()).add(amount(row.getNetAmount())));
        }
        summary.setCustomerCount((long) customerIds.size());
        maskSummary(summary, isSaleAmountHidden());
        return summary;
    }

    @Override
    public List<ErpSaleReportTrendRespVO> getSaleReportTrend(ErpSaleReportPageReqVO reqVO) {
        Map<String, ErpSaleReportTrendRespVO> trendMap = new LinkedHashMap<>();
        for (ErpSaleReportDetailRespVO row : selectRows(reqVO)) {
            if (row.getDocDate() == null) {
                continue;
            }
            String period = MONTH_FORMATTER.format(row.getDocDate());
            ErpSaleReportTrendRespVO trend = trendMap.computeIfAbsent(period, key -> {
                ErpSaleReportTrendRespVO item = new ErpSaleReportTrendRespVO();
                item.setPeriod(key);
                return item;
            });
            trend.setDocCount(trend.getDocCount() + 1);
            if (Integer.valueOf(1).equals(row.getBizType())) {
                trend.setSaleCount(amount(trend.getSaleCount()).add(amount(row.getBizCount())));
                trend.setSaleAmount(amount(trend.getSaleAmount()).add(amount(row.getSaleAmount())));
            } else {
                trend.setReturnCount(amount(trend.getReturnCount()).add(amount(row.getBizCount())));
                trend.setReturnAmount(amount(trend.getReturnAmount()).add(amount(row.getReturnAmount())));
            }
            trend.setNetAmount(amount(trend.getNetAmount()).add(amount(row.getNetAmount())));
        }
        List<ErpSaleReportTrendRespVO> list = new ArrayList<>(trendMap.values());
        maskTrend(list, isSaleAmountHidden());
        return list;
    }

    @Override
    public PageResult<ErpSaleReportRespVO> getSaleReportPage(ErpSaleReportPageReqVO reqVO) {
        List<ErpSaleReportRespVO> list = buildReportList(selectRows(reqVO));
        sortReportList(list, reqVO.getOrderField(), reqVO.getOrderDirection());
        maskReportList(list, isSaleAmountHidden());
        return buildPageResult(list, reqVO);
    }

    @Override
    public PageResult<ErpSaleReportProductRespVO> getSaleReportProductPage(ErpSaleReportPageReqVO reqVO) {
        List<ErpSaleReportProductRespVO> list = selectProductRows(reqVO);
        sortProductList(list, reqVO.getOrderField(), reqVO.getOrderDirection());
        maskProductList(list, isSaleAmountHidden());
        return buildPageResult(list, reqVO);
    }

    @Override
    public PageResult<ErpSaleReportDeptRespVO> getSaleReportDeptPage(ErpSaleReportPageReqVO reqVO) {
        List<ErpSaleReportDeptRespVO> list = buildDeptReportList(selectRows(reqVO));
        sortDeptList(list, reqVO.getOrderField(), reqVO.getOrderDirection());
        maskDeptList(list, isSaleAmountHidden());
        return buildPageResult(list, reqVO);
    }

    @Override
    public PageResult<ErpSaleReportDetailRespVO> getSaleReportDetailPage(ErpSaleReportPageReqVO reqVO) {
        List<ErpSaleReportDetailRespVO> list = selectRows(reqVO);
        if (isSaleAmountHidden()) {
            maskDetailList(list);
        }
        return buildPageResult(list, reqVO);
    }

    private <T> PageResult<T> buildPageResult(List<T> list, ErpSaleReportPageReqVO reqVO) {
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
    public List<ErpSaleReportDetailRespVO> getSaleReportDetailList(ErpSaleReportDetailReqVO reqVO) {
        ErpSaleReportPageReqVO pageReqVO = new ErpSaleReportPageReqVO();
        pageReqVO.setCustomerId(reqVO.getCustomerId());
        pageReqVO.setBizTime(reqVO.getBizTime());
        List<ErpSaleReportDetailRespVO> rows = selectRows(pageReqVO);
        if (isSaleAmountHidden()) {
            maskDetailList(rows);
        }
        return rows;
    }

    private List<ErpSaleReportRespVO> buildReportList(List<ErpSaleReportDetailRespVO> rows) {
        Map<Long, ErpSaleReportRespVO> map = new LinkedHashMap<>();
        for (ErpSaleReportDetailRespVO row : rows) {
            Long customerId = row.getCustomerId();
            if (customerId == null) {
                continue;
            }
            ErpSaleReportRespVO item = map.computeIfAbsent(customerId, key -> buildReportItem(row));
            item.setDocCount(item.getDocCount() + 1);
            if (Integer.valueOf(1).equals(row.getBizType())) {
                item.setSaleCount(amount(item.getSaleCount()).add(amount(row.getBizCount())));
                item.setSaleAmount(amount(item.getSaleAmount()).add(amount(row.getSaleAmount())));
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

    private ErpSaleReportRespVO buildReportItem(ErpSaleReportDetailRespVO row) {
        ErpSaleReportRespVO item = new ErpSaleReportRespVO();
        item.setCustomerId(row.getCustomerId());
        item.setCustomerName(row.getCustomerName());
        item.setContact(row.getContact());
        item.setMobile(row.getMobile());
        item.setDeptId(row.getDeptId());
        item.setDeptName(row.getDeptName());
        item.setSaleUserId(row.getSaleUserId());
        item.setSaleUserName(row.getSaleUserName());
        item.setDocCount(0L);
        item.setSaleCount(BigDecimal.ZERO);
        item.setSaleAmount(BigDecimal.ZERO);
        item.setReturnCount(BigDecimal.ZERO);
        item.setReturnAmount(BigDecimal.ZERO);
        item.setNetAmount(BigDecimal.ZERO);
        return item;
    }

    private List<ErpSaleReportDeptRespVO> buildDeptReportList(List<ErpSaleReportDetailRespVO> rows) {
        Map<Long, ErpSaleReportDeptRespVO> map = new LinkedHashMap<>();
        Map<Long, Set<Long>> customerMap = new HashMap<>();
        for (ErpSaleReportDetailRespVO row : rows) {
            Long deptKey = row.getDeptId() == null ? 0L : row.getDeptId();
            ErpSaleReportDeptRespVO item = map.computeIfAbsent(deptKey, key -> buildDeptReportItem(row));
            if (row.getCustomerId() != null) {
                customerMap.computeIfAbsent(deptKey, key -> new HashSet<>()).add(row.getCustomerId());
                item.setCustomerCount((long) customerMap.get(deptKey).size());
            }
            item.setDocCount(item.getDocCount() + 1);
            if (Integer.valueOf(1).equals(row.getBizType())) {
                item.setSaleCount(amount(item.getSaleCount()).add(amount(row.getBizCount())));
                item.setSaleAmount(amount(item.getSaleAmount()).add(amount(row.getSaleAmount())));
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

    private ErpSaleReportDeptRespVO buildDeptReportItem(ErpSaleReportDetailRespVO row) {
        ErpSaleReportDeptRespVO item = new ErpSaleReportDeptRespVO();
        item.setDeptId(row.getDeptId());
        item.setDeptName(row.getDeptName() == null ? "未设置部门" : row.getDeptName());
        item.setCustomerCount(0L);
        item.setDocCount(0L);
        item.setSaleCount(BigDecimal.ZERO);
        item.setSaleAmount(BigDecimal.ZERO);
        item.setReturnCount(BigDecimal.ZERO);
        item.setReturnAmount(BigDecimal.ZERO);
        item.setNetAmount(BigDecimal.ZERO);
        return item;
    }

    private List<ErpSaleReportDetailRespVO> selectRows(ErpSaleReportPageReqVO reqVO) {
        ErpFinanceVisibleScope customerScope = getVisibleScope(CUSTOMER_MODULE);
        ErpFinanceVisibleScope documentScope = getVisibleScope(REPORT_MODULE);
        if (!hasAccess(customerScope) || !hasAccess(documentScope)) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> saleReportMapper.selectRows(reqVO,
                customerScope.getDeptIds(), selfUserIdText(customerScope), customerScope.isAll(),
                documentScope.getDeptIds(), selfUserIdText(documentScope), documentScope.isAll()));
    }

    private List<ErpSaleReportProductRespVO> selectProductRows(ErpSaleReportPageReqVO reqVO) {
        ErpFinanceVisibleScope customerScope = getVisibleScope(CUSTOMER_MODULE);
        ErpFinanceVisibleScope documentScope = getVisibleScope(REPORT_MODULE);
        if (!hasAccess(customerScope) || !hasAccess(documentScope)) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> saleReportMapper.selectProductRows(reqVO,
                customerScope.getDeptIds(), selfUserIdText(customerScope), customerScope.isAll(),
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

    private boolean isSaleAmountHidden() {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(PRODUCT_PRICE_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return false;
        }
        Set<String> fields = new HashSet<>(hiddenFields);
        return isProductPriceFieldHidden(fields, "salePrice");
    }

    private boolean isProductPriceFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey) || hiddenFields.contains("col_" + fieldKey);
    }

    private void sortReportList(List<ErpSaleReportRespVO> list, String orderField, String orderDirection) {
        Comparator<ErpSaleReportRespVO> comparator = getComparator(orderField);
        if (comparator == null) {
            comparator = getComparator("netAmount");
            orderDirection = "desc";
        }
        if ("desc".equals(orderDirection)) {
            comparator = comparator.reversed();
        }
        list.sort(comparator.thenComparing(item -> item.getCustomerId() == null ? 0L : item.getCustomerId()));
    }

    private Comparator<ErpSaleReportRespVO> getComparator(String orderField) {
        Map<String, Comparator<ErpSaleReportRespVO>> comparators = new HashMap<>();
        comparators.put("customerName", Comparator.comparing(ErpSaleReportRespVO::getCustomerName,
                Comparator.nullsLast(String::compareTo)));
        comparators.put("deptName", Comparator.comparing(ErpSaleReportRespVO::getDeptName,
                Comparator.nullsLast(String::compareTo)));
        comparators.put("saleUserName", Comparator.comparing(ErpSaleReportRespVO::getSaleUserName,
                Comparator.nullsLast(String::compareTo)));
        comparators.put("saleAmount", Comparator.comparing(ErpSaleReportRespVO::getSaleAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("returnAmount", Comparator.comparing(ErpSaleReportRespVO::getReturnAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("netAmount", Comparator.comparing(ErpSaleReportRespVO::getNetAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("lastBizTime", Comparator.comparing(ErpSaleReportRespVO::getLastBizTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return comparators.get(orderField);
    }

    private void sortDeptList(List<ErpSaleReportDeptRespVO> list, String orderField, String orderDirection) {
        Comparator<ErpSaleReportDeptRespVO> comparator = getDeptComparator(orderField);
        if (comparator == null) {
            comparator = getDeptComparator("netAmount");
            orderDirection = "desc";
        }
        if ("desc".equals(orderDirection)) {
            comparator = comparator.reversed();
        }
        list.sort(comparator.thenComparing(item -> item.getDeptId() == null ? 0L : item.getDeptId()));
    }

    private Comparator<ErpSaleReportDeptRespVO> getDeptComparator(String orderField) {
        Map<String, Comparator<ErpSaleReportDeptRespVO>> comparators = new HashMap<>();
        comparators.put("deptName", Comparator.comparing(ErpSaleReportDeptRespVO::getDeptName,
                Comparator.nullsLast(String::compareTo)));
        comparators.put("customerCount", Comparator.comparing(ErpSaleReportDeptRespVO::getCustomerCount,
                Comparator.nullsLast(Long::compareTo)));
        comparators.put("docCount", Comparator.comparing(ErpSaleReportDeptRespVO::getDocCount,
                Comparator.nullsLast(Long::compareTo)));
        comparators.put("saleCount", Comparator.comparing(ErpSaleReportDeptRespVO::getSaleCount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("saleAmount", Comparator.comparing(ErpSaleReportDeptRespVO::getSaleAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("returnCount", Comparator.comparing(ErpSaleReportDeptRespVO::getReturnCount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("returnAmount", Comparator.comparing(ErpSaleReportDeptRespVO::getReturnAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("netAmount", Comparator.comparing(ErpSaleReportDeptRespVO::getNetAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("lastBizTime", Comparator.comparing(ErpSaleReportDeptRespVO::getLastBizTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return comparators.get(orderField);
    }

    private void sortProductList(List<ErpSaleReportProductRespVO> list, String orderField, String orderDirection) {
        Comparator<ErpSaleReportProductRespVO> comparator = getProductComparator(orderField);
        if (comparator == null) {
            comparator = getProductComparator("netAmount");
            orderDirection = "desc";
        }
        if ("desc".equals(orderDirection)) {
            comparator = comparator.reversed();
        }
        list.sort(comparator.thenComparing(item -> item.getProductId() == null ? 0L : item.getProductId()));
    }

    private Comparator<ErpSaleReportProductRespVO> getProductComparator(String orderField) {
        Map<String, Comparator<ErpSaleReportProductRespVO>> comparators = new HashMap<>();
        comparators.put("productCode", Comparator.comparing(ErpSaleReportProductRespVO::getProductCode,
                Comparator.nullsLast(String::compareTo)));
        comparators.put("productName", Comparator.comparing(ErpSaleReportProductRespVO::getProductName,
                Comparator.nullsLast(String::compareTo)));
        comparators.put("customerCount", Comparator.comparing(ErpSaleReportProductRespVO::getCustomerCount,
                Comparator.nullsLast(Long::compareTo)));
        comparators.put("docCount", Comparator.comparing(ErpSaleReportProductRespVO::getDocCount,
                Comparator.nullsLast(Long::compareTo)));
        comparators.put("saleCount", Comparator.comparing(ErpSaleReportProductRespVO::getSaleCount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("saleAmount", Comparator.comparing(ErpSaleReportProductRespVO::getSaleAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("returnCount", Comparator.comparing(ErpSaleReportProductRespVO::getReturnCount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("returnAmount", Comparator.comparing(ErpSaleReportProductRespVO::getReturnAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("netAmount", Comparator.comparing(ErpSaleReportProductRespVO::getNetAmount,
                Comparator.nullsLast(BigDecimal::compareTo)));
        comparators.put("lastBizTime", Comparator.comparing(ErpSaleReportProductRespVO::getLastBizTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return comparators.get(orderField);
    }

    private void maskReportList(List<ErpSaleReportRespVO> list, boolean hidden) {
        if (!hidden) {
            return;
        }
        for (ErpSaleReportRespVO item : list) {
            item.setSaleAmount(null);
            item.setReturnAmount(null);
            item.setNetAmount(null);
        }
    }

    private void maskProductList(List<ErpSaleReportProductRespVO> list, boolean hidden) {
        if (!hidden) {
            return;
        }
        for (ErpSaleReportProductRespVO item : list) {
            item.setSaleAmount(null);
            item.setReturnAmount(null);
            item.setNetAmount(null);
        }
    }

    private void maskDeptList(List<ErpSaleReportDeptRespVO> list, boolean hidden) {
        if (!hidden) {
            return;
        }
        for (ErpSaleReportDeptRespVO item : list) {
            item.setSaleAmount(null);
            item.setReturnAmount(null);
            item.setNetAmount(null);
        }
    }

    private void maskDetailList(List<ErpSaleReportDetailRespVO> list) {
        for (ErpSaleReportDetailRespVO row : list) {
            row.setSaleAmount(null);
            row.setReturnAmount(null);
            row.setNetAmount(null);
        }
    }

    private void maskSummary(ErpSaleReportSummaryRespVO summary, boolean hidden) {
        if (!hidden) {
            return;
        }
        summary.setSaleAmount(null);
        summary.setReturnAmount(null);
        summary.setNetAmount(null);
    }

    private void maskTrend(List<ErpSaleReportTrendRespVO> list, boolean hidden) {
        if (!hidden) {
            return;
        }
        for (ErpSaleReportTrendRespVO item : list) {
            item.setSaleAmount(null);
            item.setReturnAmount(null);
            item.setNetAmount(null);
        }
    }

    private BigDecimal amount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
