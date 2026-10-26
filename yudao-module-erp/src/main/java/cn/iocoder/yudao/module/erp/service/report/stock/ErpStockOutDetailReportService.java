package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockOutDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockOutDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockOutDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** 其他出库逐明细报表服务。 */
@Service
public class ErpStockOutDetailReportService {

    private static final String STOCK_OUT_MODULE = "erp_stock_out";
    private static final String STOCK_RECORD_MODULE = "erp_stock_record";
    private static final String PRODUCT_MODULE = "erp_product";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;
    private static final List<String> EXPORT_FIELDS = Arrays.asList("warehouseName", "documentNo",
            "relatedPartyName", "productCode", "drawingNo", "productName", "vehicleModel", "featureCode",
            "count", "unitPrice", "totalAmount", "costUnitPrice", "costAmount", "productPrice",
            "productAmount", "cancelCount", "afterCancelCount", "afterCancelAmount", "documentTime",
            "approveTime", "sourceNo", "brand", "originPlace", "standard", "productCategoryName", "shelf",
            "documentCategory", "businessUserName", "creatorName", "deptName", "itemRemark");

    @Resource
    private ErpStockOutDetailReportRepository repository;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private PermissionApi permissionApi;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        Visibility visibility = visibility();
        validate(filter, visibility);
        ErpStockOutDetailReportRepository.Scope scope = scope(visibility);
        long total = repository.count(filter, scope);
        if (total == 0) return PageResult.empty();
        List<Row> rows = repository.page(filter, scope,
                (long) (filter.getPageNo() - 1) * filter.getPageSize(), filter.getPageSize());
        finishRows(rows, visibility);
        return new PageResult<>(rows, total);
    }

    @Transactional(readOnly = true)
    public Summary getSummary(Filter filter) {
        Visibility visibility = visibility();
        validate(filter, visibility);
        Summary summary = repository.summary(filter, scope(visibility));
        if (summary == null) {
            summary = new Summary().setRowCount(0L).setTotalCount(BigDecimal.ZERO)
                    .setTotalAmount(BigDecimal.ZERO).setTotalCostAmount(BigDecimal.ZERO)
                    .setTotalProductAmount(BigDecimal.ZERO).setTotalCancelCount(BigDecimal.ZERO)
                    .setTotalAfterCancelCount(BigDecimal.ZERO).setTotalAfterCancelAmount(BigDecimal.ZERO)
                    .setMissingCostCount(0L);
        }
        summary.setPriceVisible(visibility.priceVisible).setCostVisible(visibility.costVisible)
                .setHiddenFields(visibility.hiddenFields);
        if (!visibility.countVisible()) {
            summary.setTotalCount(null).setTotalCancelCount(null).setTotalAfterCancelCount(null);
        }
        if (!visibility.priceVisible) {
            summary.setTotalAmount(null).setTotalProductAmount(null).setTotalAfterCancelAmount(null);
        } else {
            summary.setTotalAmount(scale(summary.getTotalAmount()))
                    .setTotalProductAmount(scale(summary.getTotalProductAmount()))
                    .setTotalAfterCancelAmount(scale(summary.getTotalAfterCancelAmount()));
        }
        if (!visibility.costVisible) {
            summary.setTotalCostAmount(null).setMissingCostCount(null);
        } else {
            summary.setTotalCostAmount(scale(summary.getTotalCostAmount()));
        }
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        Visibility visibility = visibility();
        validate(filter, visibility);
        ErpStockOutDetailReportRepository.Scope scope = scope(visibility);
        long total = repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("其他出库明细报表.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(visibility)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("其他出库明细").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                finishRows(rows, visibility);
                writer.write(rows, sheet);
            }
        }
    }

    ErpStockOutDetailReportRepository.Scope scope(Visibility visibility) {
        Long userId = getLoginUserId();
        DeptDataPermissionRespDTO permission = userId == null ? null
                : permissionApi.getDeptDataPermission(userId, STOCK_OUT_MODULE);
        boolean all = permission != null && Boolean.TRUE.equals(permission.getAll());
        Collection<Long> deptIds = permission == null ? Collections.emptySet() : permission.getDeptIds();
        boolean self = permission != null && Boolean.TRUE.equals(permission.getSelf());
        return new ErpStockOutDetailReportRepository.Scope(TenantContextHolder.getRequiredTenantId(),
                all, deptIds, self, userId, visibility.keywordColumns);
    }

    private Visibility visibility() {
        Set<String> stockOutHidden = normalize(fieldPermissionMasker.getHiddenFieldSet(STOCK_OUT_MODULE));
        Set<String> stockRecordHidden = normalize(fieldPermissionMasker.getHiddenFieldSet(STOCK_RECORD_MODULE));
        Set<String> productHidden = normalize(permissionApi.getCurrentUserHiddenFields(PRODUCT_MODULE));
        Set<String> hidden = new LinkedHashSet<>();
        addIfHidden(hidden, stockOutHidden, "warehouseName", "item_warehouseId", "warehouseNames");
        addIfHidden(hidden, stockOutHidden, "documentNo", "no");
        addIfHidden(hidden, stockOutHidden, "relatedPartyName", "customerId", "customerName");
        addIfHidden(hidden, stockOutHidden, "productCode", "item_productCode", "productCodes");
        addIfHidden(hidden, stockOutHidden, "productName", "item_productId", "productNames", "items");
        addIfHidden(hidden, stockOutHidden, "count", "item_count", "totalCount");
        addIfHidden(hidden, stockOutHidden, "approveTime", "approveTime");
        addIfHidden(hidden, stockOutHidden, "documentTime", "outTime");
        addIfHidden(hidden, stockOutHidden, "itemRemark", "item_remark");
        addIfHidden(hidden, stockOutHidden, "deptName", "deptId", "deptName");
        addIfHidden(hidden, stockOutHidden, "businessUserName", "creator", "creatorName");
        addIfHidden(hidden, stockOutHidden, "creatorName", "creator", "creatorName");
        addIfHidden(hidden, productHidden, "productCode", "code");
        addIfHidden(hidden, productHidden, "drawingNo", "drawingNo");
        addIfHidden(hidden, productHidden, "productName", "name");
        addIfHidden(hidden, productHidden, "vehicleModel", "vehicleModel");
        addIfHidden(hidden, productHidden, "featureCode", "featureCode");
        addIfHidden(hidden, productHidden, "brand", "brand");
        addIfHidden(hidden, productHidden, "originPlace", "originPlace");
        addIfHidden(hidden, productHidden, "standard", "standard");
        addIfHidden(hidden, productHidden, "productCategoryName", "categoryId");
        addIfHidden(hidden, productHidden, "shelf", "shelf");

        boolean priceVisible = !containsAny(stockOutHidden, "item_productPrice", "item_totalPrice",
                "productPrice", "totalPrice");
        if (!priceVisible) Collections.addAll(hidden, "unitPrice", "totalAmount", "productPrice",
                "productAmount", "afterCancelAmount");
        if (hidden.contains("count")) Collections.addAll(hidden, "cancelCount", "afterCancelCount");
        boolean costVisible = !containsAny(stockRecordHidden, "unitPrice", "totalPrice");
        if (!costVisible) Collections.addAll(hidden, "costUnitPrice", "costAmount");
        return new Visibility(hidden, priceVisible, costVisible, keywordColumns(hidden));
    }

    private List<String> keywordColumns(Set<String> hidden) {
        List<String> columns = new ArrayList<>();
        addKeyword(columns, hidden, "documentNo", "document_no");
        addKeyword(columns, hidden, "relatedPartyName", "related_party_name");
        addKeyword(columns, hidden, "productCode", "product_code");
        addKeyword(columns, hidden, "drawingNo", "drawing_no");
        addKeyword(columns, hidden, "productName", "product_name");
        addKeyword(columns, hidden, "vehicleModel", "vehicle_model");
        addKeyword(columns, hidden, "featureCode", "feature_code");
        addKeyword(columns, hidden, "brand", "brand");
        addKeyword(columns, hidden, "originPlace", "origin_place");
        addKeyword(columns, hidden, "standard", "standard");
        addKeyword(columns, hidden, "productCategoryName", "product_category_name");
        addKeyword(columns, hidden, "shelf", "shelf");
        addKeyword(columns, hidden, "deptName", "dept_name");
        addKeyword(columns, hidden, "businessUserName", "business_user_name");
        if (!hidden.contains("creatorName") && !columns.contains("creator_name")) columns.add("creator_name");
        return columns;
    }

    private void addKeyword(List<String> columns, Set<String> hidden, String field, String column) {
        if (!hidden.contains(field)) columns.add(column);
    }

    private void finishRows(List<Row> rows, Visibility visibility) {
        for (Row row : rows) {
            row.setPriceVisible(visibility.priceVisible).setCostVisible(visibility.costVisible)
                    .setHiddenFields(visibility.hiddenFields);
            row.setUnitPrice(scale(row.getUnitPrice())).setTotalAmount(scale(row.getTotalAmount()))
                    .setCostUnitPrice(scale(row.getCostUnitPrice())).setCostAmount(scale(row.getCostAmount()))
                    .setProductPrice(scale(row.getProductPrice())).setProductAmount(scale(row.getProductAmount()))
                    .setAfterCancelAmount(scale(row.getAfterCancelAmount()));
            mask(row, visibility.hiddenFields);
        }
    }

    private BigDecimal scale(BigDecimal value) {
        return value;
    }

    private void mask(Row row, Set<String> hidden) {
        if (hidden.contains("warehouseName")) row.setWarehouseName(null).setWarehouseId(null);
        if (hidden.contains("documentNo")) row.setDocumentNo(null).setStockOutId(null);
        if (hidden.contains("relatedPartyName")) row.setRelatedPartyName(null).setRelatedPartyId(null);
        if (hidden.contains("productCode")) row.setProductCode(null).setProductId(null);
        if (hidden.contains("drawingNo")) row.setDrawingNo(null);
        if (hidden.contains("productName")) row.setProductName(null);
        if (hidden.contains("vehicleModel")) row.setVehicleModel(null);
        if (hidden.contains("featureCode")) row.setFeatureCode(null);
        if (hidden.contains("count")) row.setCount(null);
        if (hidden.contains("unitPrice")) row.setUnitPrice(null);
        if (hidden.contains("totalAmount")) row.setTotalAmount(null);
        if (hidden.contains("costUnitPrice")) row.setCostUnitPrice(null);
        if (hidden.contains("costAmount")) row.setCostAmount(null);
        if (hidden.contains("productPrice")) row.setProductPrice(null);
        if (hidden.contains("productAmount")) row.setProductAmount(null);
        if (hidden.contains("cancelCount")) row.setCancelCount(null);
        if (hidden.contains("afterCancelCount")) row.setAfterCancelCount(null);
        if (hidden.contains("afterCancelAmount")) row.setAfterCancelAmount(null);
        if (hidden.contains("documentTime")) row.setDocumentTime(null);
        if (hidden.contains("approveTime")) row.setApproveTime(null);
        if (hidden.contains("brand")) row.setBrand(null);
        if (hidden.contains("originPlace")) row.setOriginPlace(null);
        if (hidden.contains("standard")) row.setStandard(null);
        if (hidden.contains("productCategoryName")) row.setProductCategoryName(null).setCategoryId(null);
        if (hidden.contains("shelf")) row.setShelf(null);
        if (hidden.contains("businessUserName")) row.setBusinessUserName(null).setBusinessUserId(null);
        if (hidden.contains("creatorName")) row.setCreatorName(null);
        if (hidden.contains("deptName")) row.setDeptName(null).setDeptId(null);
        if (hidden.contains("itemRemark")) row.setItemRemark(null);
    }

    private Set<String> exportFields(Visibility visibility) {
        Set<String> fields = new LinkedHashSet<>(EXPORT_FIELDS);
        fields.removeAll(visibility.hiddenFields);
        return fields;
    }

    private void validate(Filter filter, Visibility visibility) {
        if (filter == null || filter.getPageNo() == null || filter.getPageNo() < 1
                || filter.getPageSize() == null || filter.getPageSize() < 1 || filter.getPageSize() > 500) {
            throw new ServiceException(400, "分页大小必须在 1 到 500 之间");
        }
        validateRange("审核日期", filter.getApproveTimeFrom(), filter.getApproveTimeTo());
        validateRange("单据日期", filter.getDocumentTimeFrom(), filter.getDocumentTimeTo());
        if (filter.getOrderDirection() != null && !filter.getOrderDirection().trim().isEmpty()
                && !"asc".equalsIgnoreCase(filter.getOrderDirection())
                && !"desc".equalsIgnoreCase(filter.getOrderDirection())) {
            throw new ServiceException(400, "排序方向必须为 asc 或 desc");
        }
        if (filter.getKeyword() != null && filter.getKeyword().length() > 100) {
            throw new ServiceException(400, "查询关键字长度不能超过 100 个字符");
        }
        if (StringUtils.hasText(filter.getKeyword()) && visibility.keywordColumns.isEmpty()) {
            throw new ServiceException(403, "当前账号无权按报表字段搜索");
        }
        if (filter.getOrderField() != null && visibility.hiddenFields.contains(filter.getOrderField())) {
            throw new ServiceException(403, "当前账号无权使用当前排序字段");
        }
        if ((filter.getApproveTime() != null && visibility.hiddenFields.contains("approveTime"))
                || (filter.getDocumentTime() != null && visibility.hiddenFields.contains("documentTime"))
                || (filter.getWarehouseId() != null && visibility.hiddenFields.contains("warehouseName"))
                || (filter.getRelatedPartyId() != null && visibility.hiddenFields.contains("relatedPartyName"))
                || (filter.getCategoryId() != null && visibility.hiddenFields.contains("productCategoryName"))
                || (filter.getDeptId() != null && visibility.hiddenFields.contains("deptName"))
                || (filter.getBusinessUserId() != null
                    && visibility.hiddenFields.contains("businessUserName")
                    && visibility.hiddenFields.contains("creatorName"))) {
            throw new ServiceException(403, "当前账号无权使用所选报表筛选条件");
        }
    }

    private void validateRange(String name, java.time.LocalDateTime min, java.time.LocalDateTime max) {
        if (min != null && max != null && min.isAfter(max)) {
            throw new ServiceException(400, name + "范围不正确");
        }
    }

    private Set<String> normalize(Collection<String> fields) {
        if (fields == null) return Collections.emptySet();
        return fields.stream().map(field -> field.startsWith("col_") ? field.substring(4) : field)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private void addIfHidden(Set<String> target, Set<String> source, String reportField, String... sourceFields) {
        if (containsAny(source, sourceFields)) target.add(reportField);
    }

    private boolean containsAny(Set<String> fields, String... candidates) {
        return Arrays.stream(candidates).anyMatch(fields::contains);
    }

    static final class Visibility {
        private final Set<String> hiddenFields;
        private final boolean priceVisible;
        private final boolean costVisible;
        private final List<String> keywordColumns;

        private Visibility(Set<String> hiddenFields, boolean priceVisible, boolean costVisible,
                           List<String> keywordColumns) {
            this.hiddenFields = hiddenFields;
            this.priceVisible = priceVisible;
            this.costVisible = costVisible;
            this.keywordColumns = keywordColumns;
        }

        private boolean countVisible() {
            return !hiddenFields.contains("count");
        }
    }
}
