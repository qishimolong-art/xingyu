package cn.iocoder.yudao.module.erp.service.report.sale;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSalePickDeliveryDetailReportModels.DeliveryExportRow;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSalePickDeliveryDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSalePickDeliveryDetailReportModels.PickExportRow;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSalePickDeliveryDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSalePickDeliveryDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleDeliveryStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePickStatusEnum;
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Service
public class ErpSalePickDeliveryDetailReportService {

    private static final String CART_MODULE = "erp_sale_cart";
    private static final String OUT_MODULE = "erp_sale_out";
    private static final int CART_SOURCE = 30;
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;

    @Resource private ErpSalePickDeliveryDetailReportRepository repository;
    @Resource private PermissionApi permissionApi;
    @Resource private ErpSaleFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter, boolean delivery) {
        stripHiddenFilters(filter);
        validate(filter, delivery);
        ErpSalePickDeliveryDetailReportRepository.Scope scope = resolveScope();
        if (scope == null) return PageResult.empty();
        long total = repository.count(filter, scope, delivery);
        if (total == 0) return PageResult.empty();
        long offset = (long) (filter.getPageNo() - 1) * filter.getPageSize();
        List<Row> rows = repository.page(filter, scope, delivery, offset, filter.getPageSize());
        finishRows(rows, delivery);
        return new PageResult<>(rows, total);
    }

    @Transactional(readOnly = true)
    public Summary getSummary(Filter filter, boolean delivery) {
        stripHiddenFilters(filter);
        validate(filter, delivery);
        Set<String> hidden = reportHiddenFields();
        ErpSalePickDeliveryDetailReportRepository.Scope scope = resolveScope();
        Summary summary = scope == null ? emptySummary() : repository.summary(filter, scope, delivery);
        summary.setHiddenFields(hidden);
        if (hidden.contains("count")) {
            summary.setTotalCount(null).setPickedCount(null).setRemainingPickCount(null)
                    .setDeliveredCount(null).setAvailableDeliveryCount(null).setRemainingDeliveryCount(null)
                    .setPieceCount(null);
        }
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, boolean delivery, HttpServletResponse response) throws IOException {
        stripHiddenFilters(filter);
        validate(filter, delivery);
        ErpSalePickDeliveryDetailReportRepository.Scope scope = resolveScope();
        long total = scope == null ? 0L : repository.count(filter, scope, delivery);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        Class<?> head = delivery ? DeliveryExportRow.class : PickExportRow.class;
        String fileName = delivery ? "送货明细查询.xlsx" : "拣货明细查询.xlsx";
        String sheetName = delivery ? "送货明细" : "拣货明细";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), head)
                .includeColumnFieldNames(exportFields(delivery)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet(sheetName).build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, delivery, offset, EXPORT_BATCH_SIZE);
                finishRows(rows, delivery);
                writer.write(delivery ? BeanUtils.toBean(rows, DeliveryExportRow.class)
                        : BeanUtils.toBean(rows, PickExportRow.class), sheet);
            }
        }
    }

    private void finishRows(List<Row> rows, boolean delivery) {
        Set<String> reportHidden = reportHiddenFields();
        for (Row row : rows) {
            row.setStatusText(delivery ? deliveryStatus(row.getStatus()) : pickStatus(row.getStatus()));
            String module = Integer.valueOf(CART_SOURCE).equals(row.getSourceType()) ? CART_MODULE : OUT_MODULE;
            maskRow(row, module);
            row.setHiddenFields(reportHidden);
        }
    }

    private void maskRow(Row row, String module) {
        if (hidden(CART_MODULE, "no")) row.setSourceCartNo(null).setSourceCartId(null);
        if (hidden(OUT_MODULE, "no")) row.setSaleOutNo(null).setSaleOutId(null);
        if (hidden(OUT_MODULE, "outTime")) row.setSaleOutDate(null);
        if (hidden(module, "customerId")) row.setCustomerName(null).setCustomerId(null);
        if (hidden(module, "deptId")) row.setDeptName(null).setDeptId(null);
        if (hidden(module, "saleUserId")) row.setSaleUserName(null).setSaleUserId(null);
        if (hidden(module, "item_warehouseId") || hidden(module, "item_warehouseName")) {
            row.setWarehouseName(null).setWarehouseId(null).setWarehousePosition(null);
        }
        if (hidden(module, "item_productId") || hidden(module, "item_productCode")) {
            row.setProductCode(null).setProductId(null);
        }
        if (hidden(module, "item_productId") || hidden(module, "item_productName")) row.setProductName(null);
        if (hidden(module, "item_standard")) row.setStandard(null);
        if (hidden(module, "item_count")) {
            row.setCount(null).setPickedCount(null).setRemainingPickCount(null).setDeliveredCount(null)
                    .setAvailableDeliveryCount(null).setRemainingDeliveryCount(null).setPackageQty(null).setPieceCount(null);
        }
    }

    private Set<String> reportHiddenFields() {
        Set<String> hidden = new LinkedHashSet<>();
        if (hidden(CART_MODULE, "no")) hidden.add("sourceCartNo");
        if (hidden(OUT_MODULE, "no")) hidden.add("saleOutNo");
        if (hidden(OUT_MODULE, "outTime")) hidden.add("saleOutDate");
        addIfBothHidden(hidden, "customerName", "customerId");
        addIfBothHidden(hidden, "deptName", "deptId");
        addIfBothHidden(hidden, "saleUserName", "saleUserId");
        addIfBothHidden(hidden, "warehouseName", "item_warehouseId");
        addIfBothHidden(hidden, "productCode", "item_productId");
        addIfBothHidden(hidden, "productName", "item_productId");
        addIfBothHidden(hidden, "standard", "item_standard");
        addIfEitherHidden(hidden, "count", "item_count");
        return hidden;
    }

    private void addIfBothHidden(Set<String> target, String reportField, String field) {
        if (hidden(CART_MODULE, field) && hidden(OUT_MODULE, field)) target.add(reportField);
    }
    private void addIfEitherHidden(Set<String> target, String reportField, String field) {
        if (hidden(CART_MODULE, field) || hidden(OUT_MODULE, field)) target.add(reportField);
    }
    private boolean hidden(String module, String field) { return fieldPermissionMasker.isFieldHidden(module, field); }

    private ErpSalePickDeliveryDetailReportRepository.Scope resolveScope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        ErpFinanceVisibleScope cartScope = visibleScope(userId, CART_MODULE);
        ErpFinanceVisibleScope outScope = visibleScope(userId, OUT_MODULE);
        if ((cartScope == null || !cartScope.hasAccess()) && (outScope == null || !outScope.hasAccess())) return null;
        return new ErpSalePickDeliveryDetailReportRepository.Scope(
                TenantContextHolder.getRequiredTenantId(), cartScope, outScope);
    }

    private ErpFinanceVisibleScope visibleScope(Long userId, String module) {
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, module);
        return ErpFinanceVisibleScope.from(permission, userId);
    }

    private String pickStatus(Integer status) {
        if (status == null) return "";
        for (ErpSalePickStatusEnum value : ErpSalePickStatusEnum.values()) if (value.getStatus().equals(status)) return value.getName();
        return "未知";
    }
    private String deliveryStatus(Integer status) {
        if (status == null) return "";
        for (ErpSaleDeliveryStatusEnum value : ErpSaleDeliveryStatusEnum.values()) if (value.getStatus().equals(status)) return value.getName();
        return "未知";
    }

    private Summary emptySummary() {
        return new Summary().setRowCount(0L).setTotalCount(BigDecimal.ZERO).setPickedCount(BigDecimal.ZERO)
                .setRemainingPickCount(BigDecimal.ZERO).setDeliveredCount(BigDecimal.ZERO)
                .setAvailableDeliveryCount(BigDecimal.ZERO).setRemainingDeliveryCount(BigDecimal.ZERO)
                .setPieceCount(BigDecimal.ZERO);
    }

    private Set<String> exportFields(boolean delivery) {
        Set<String> fields = new LinkedHashSet<>();
        Collections.addAll(fields, "sequence", "taskCreateDate", "saleOutDate", "sourceCartNo", "saleOutNo",
                "customerName", "deptName", "saleUserName", "warehouseName", "productCode", "productName",
                "standard", "warehousePosition", "count", "pickedCount");
        if (delivery) Collections.addAll(fields, "deliveredCount", "availableDeliveryCount", "remainingDeliveryCount");
        else fields.add("remainingPickCount");
        Collections.addAll(fields, "packageQty", "pieceCount", "statusText", "operationUserName", "operationTime",
                "completeTime");
        Set<String> hidden = reportHiddenFields();
        fields.removeAll(hidden);
        if (hidden.contains("count")) {
            fields.removeAll(Set.of("count", "pickedCount", "remainingPickCount", "deliveredCount",
                    "availableDeliveryCount", "remainingDeliveryCount", "packageQty", "pieceCount"));
        }
        return fields;
    }

    private void stripHiddenFilters(Filter filter) {
        if (filter == null) return;
        Set<String> hidden = reportHiddenFields();
        if (hidden.contains("sourceCartNo")) filter.setSourceCartNo(null);
        if (hidden.contains("saleOutNo")) filter.setSaleOutNo(null);
        if (hidden.contains("saleOutDate")) filter.setSaleOutDate(null);
        if (hidden.contains("customerName")) filter.setCustomerId(null);
        if (hidden.contains("deptName")) filter.setDeptId(null);
        if (hidden.contains("saleUserName")) filter.setSaleUserId(null);
        if (hidden.contains("warehouseName")) filter.setWarehouseId(null);
        if (hidden.contains("productCode") || hidden.contains("productName")) filter.setProductKeyword(null);
        if (hidden.contains("count")) {
            filter.setCountMin(null);
            filter.setCountMax(null);
            filter.setPickedCountMin(null);
            filter.setPickedCountMax(null);
            filter.setDeliveredCountMin(null);
            filter.setDeliveredCountMax(null);
            filter.setAvailableCountMin(null);
            filter.setAvailableCountMax(null);
            filter.setRemainingCountMin(null);
            filter.setRemainingCountMax(null);
        }
    }

    private void validate(Filter filter, boolean delivery) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1
                || filter.getPageSize() == null || filter.getPageSize() < 1 || filter.getPageSize() > 1000) {
            throw new ServiceException(400, "分页大小必须在 1 到 1000 之间");
        }
        range(filter.getTaskCreateDateFrom(), filter.getTaskCreateDateTo(), "任务创建日期");
        range(filter.getSaleOutDateFrom(), filter.getSaleOutDateTo(), "销售出库日期");
        range(filter.getOperationTimeFrom(), filter.getOperationTimeTo(), "最近操作时间");
        range(filter.getCompleteTimeFrom(), filter.getCompleteTimeTo(), "任务完成时间");
        numberRange(filter.getCountMin(), filter.getCountMax(), "应作业数量");
        numberRange(filter.getPickedCountMin(), filter.getPickedCountMax(), "已拣数量");
        numberRange(filter.getDeliveredCountMin(), filter.getDeliveredCountMax(), "已送数量");
        numberRange(filter.getAvailableCountMin(), filter.getAvailableCountMax(), "当前可送数量");
        numberRange(filter.getRemainingCountMin(), filter.getRemainingCountMax(), "未完成数量");
        Set<Integer> statuses = delivery ? Set.of(5, 10, 20, 30) : Set.of(10, 20, 30);
        if (filter.getStatus() != null && !statuses.contains(filter.getStatus())) throw new ServiceException(400, "状态不正确");
        if (hasText(filter.getOrderDirection()) && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        if (length(filter.getSourceCartNo()) > 100 || length(filter.getSaleOutNo()) > 100
                || length(filter.getProductKeyword()) > 100) throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
    }
    private void range(java.time.LocalDateTime from, java.time.LocalDateTime to, String name) {
        if (from != null && to != null && from.isAfter(to)) throw new ServiceException(400, name + "范围不正确");
    }
    private void numberRange(BigDecimal min, BigDecimal max, String name) {
        if (min != null && max != null && min.compareTo(max) > 0) throw new ServiceException(400, name + "范围不正确");
    }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private int length(String value) { return value == null ? 0 : value.length(); }
}
