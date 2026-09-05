package cn.iocoder.yudao.module.erp.service.report;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportPageReqVO;
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
            for (ErpSaleReportDetailRespVO row : rows) {
                row.setSaleAmount(null);
                row.setReturnAmount(null);
                row.setNetAmount(null);
            }
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
