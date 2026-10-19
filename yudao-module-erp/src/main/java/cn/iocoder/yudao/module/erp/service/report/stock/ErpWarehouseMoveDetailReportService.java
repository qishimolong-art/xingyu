package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpWarehouseMoveDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpWarehouseMoveDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpWarehouseMoveDetailReportModels.Summary;
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

/** 仓库移货逐明细报表服务。 */
@Service
public class ErpWarehouseMoveDetailReportService {

    private static final String MOVE_MODULE = "erp_warehouse_move";
    private static final String PRODUCT_MODULE = "erp_product";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;
    private static final List<String> EXPORT_FIELDS = Arrays.asList("productCode", "documentNo", "approveTime",
            "productName", "brand", "vehicleModel", "originPlace", "unitName", "count", "costPrice",
            "costAmount", "movePrice", "moveAmount", "fromWarehouseName", "fromShelf", "toWarehouseName",
            "toShelf", "creatorName", "approveUserName", "standard", "drawingNo", "featureCode", "itemRemark");

    @Resource
    private ErpWarehouseMoveDetailReportRepository repository;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private PermissionApi permissionApi;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        Visibility visibility = visibility();
        validate(filter, visibility);
        stripHiddenFilters(filter, visibility);
        ErpWarehouseMoveDetailReportRepository.Scope scope = scope(visibility);
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
        stripHiddenFilters(filter, visibility);
        Summary summary = repository.summary(filter, scope(visibility));
        if (summary == null) {
            summary = new Summary().setRowCount(0L).setTotalCount(BigDecimal.ZERO)
                    .setTotalCostAmount(BigDecimal.ZERO).setTotalMoveAmount(BigDecimal.ZERO);
        }
        summary.setCostVisible(visibility.costVisible).setMovePriceVisible(visibility.movePriceVisible)
                .setHiddenFields(visibility.hiddenFields);
        if (visibility.hiddenFields.contains("count")) summary.setTotalCount(null);
        if (!visibility.costVisible) summary.setTotalCostAmount(null);
        else summary.setTotalCostAmount(scale(summary.getTotalCostAmount()));
        if (!visibility.movePriceVisible) summary.setTotalMoveAmount(null);
        else summary.setTotalMoveAmount(scale(summary.getTotalMoveAmount()));
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        Visibility visibility = visibility();
        validate(filter, visibility);
        stripHiddenFilters(filter, visibility);
        ErpWarehouseMoveDetailReportRepository.Scope scope = scope(visibility);
        long total = repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("仓库移货明细报表.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(visibility)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("仓库移货明细").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                finishRows(rows, visibility);
                writer.write(rows, sheet);
            }
        }
    }

    ErpWarehouseMoveDetailReportRepository.Scope scope(Visibility visibility) {
        Long userId = getLoginUserId();
        DeptDataPermissionRespDTO permission = userId == null ? null
                : permissionApi.getDeptDataPermission(userId, MOVE_MODULE);
        boolean all = permission != null && Boolean.TRUE.equals(permission.getAll());
        Collection<Long> deptIds = permission == null ? Collections.emptySet() : permission.getDeptIds();
        boolean self = permission != null && Boolean.TRUE.equals(permission.getSelf());
        return new ErpWarehouseMoveDetailReportRepository.Scope(TenantContextHolder.getRequiredTenantId(),
                all, deptIds, self, userId, visibility.keywordColumns);
    }

    private Visibility visibility() {
        Set<String> moveHidden = normalize(fieldPermissionMasker.getHiddenFieldSet(MOVE_MODULE));
        Set<String> productHidden = normalize(permissionApi.getCurrentUserHiddenFields(PRODUCT_MODULE));
        Set<String> hidden = new LinkedHashSet<>();
        addIfHidden(hidden, moveHidden, "documentNo", "no");
        addIfHidden(hidden, moveHidden, "approveTime", "approveTime");
        addIfHidden(hidden, moveHidden, "unitName", "item_productUnitId");
        addIfHidden(hidden, moveHidden, "count", "item_count", "totalCount");
        addIfHidden(hidden, moveHidden, "fromWarehouseName", "item_fromWarehouseId", "fromWarehouseId",
                "fromWarehouseName", "query_fromWarehouseId");
        addIfHidden(hidden, moveHidden, "fromShelf", "item_fromShelf");
        addIfHidden(hidden, moveHidden, "toWarehouseName", "item_toWarehouseId", "toWarehouseId",
                "toWarehouseName", "query_toWarehouseId");
        addIfHidden(hidden, moveHidden, "toShelf", "item_toShelf");
        addIfHidden(hidden, moveHidden, "creatorName", "creator", "creatorName");
        addIfHidden(hidden, moveHidden, "approveUserName", "approveUserId", "approveUserName");
        addIfHidden(hidden, moveHidden, "itemRemark", "item_remark");
        addIfHidden(hidden, moveHidden, "productCode", "item_productCode", "productCodes");
        addIfHidden(hidden, moveHidden, "productName", "item_productId", "productNames", "items");
        addIfHidden(hidden, productHidden, "productCode", "code");
        addIfHidden(hidden, productHidden, "productName", "name");
        addIfHidden(hidden, productHidden, "brand", "brand");
        addIfHidden(hidden, productHidden, "vehicleModel", "vehicleModel");
        addIfHidden(hidden, productHidden, "originPlace", "originPlace");
        addIfHidden(hidden, productHidden, "standard", "standard");
        addIfHidden(hidden, productHidden, "drawingNo", "drawingNo");
        addIfHidden(hidden, productHidden, "featureCode", "featureCode");
        boolean costVisible = !containsAny(moveHidden, "item_costPrice", "item_costAmount",
                "totalCostAmount", "col_totalCostAmount");
        boolean movePriceVisible = !containsAny(moveHidden, "item_productPrice", "item_totalPrice",
                "totalPrice", "col_totalPrice");
        if (!costVisible) Collections.addAll(hidden, "costPrice", "costAmount");
        if (!movePriceVisible) Collections.addAll(hidden, "movePrice", "moveAmount");
        return new Visibility(hidden, costVisible, movePriceVisible, keywordColumns(hidden));
    }

    private List<String> keywordColumns(Set<String> hidden) {
        List<String> columns = new ArrayList<>();
        addKeyword(columns, hidden, "documentNo", "document_no");
        addKeyword(columns, hidden, "productCode", "product_code");
        addKeyword(columns, hidden, "productName", "product_name");
        addKeyword(columns, hidden, "brand", "brand");
        addKeyword(columns, hidden, "vehicleModel", "vehicle_model");
        addKeyword(columns, hidden, "originPlace", "origin_place");
        addKeyword(columns, hidden, "standard", "standard");
        addKeyword(columns, hidden, "drawingNo", "drawing_no");
        addKeyword(columns, hidden, "featureCode", "feature_code");
        addKeyword(columns, hidden, "fromShelf", "from_shelf");
        addKeyword(columns, hidden, "toShelf", "to_shelf");
        addKeyword(columns, hidden, "creatorName", "creator_name");
        addKeyword(columns, hidden, "approveUserName", "approve_user_name");
        return columns;
    }

    private void addKeyword(List<String> columns, Set<String> hidden, String field, String column) {
        if (!hidden.contains(field)) columns.add(column);
    }

    private void stripHiddenFilters(Filter filter, Visibility visibility) {
        if (visibility.hiddenFields.contains("approveTime")) filter.setApproveTime(null);
        if (visibility.hiddenFields.contains("fromWarehouseName")) filter.setFromWarehouseId(null);
        if (visibility.hiddenFields.contains("toWarehouseName")) filter.setToWarehouseId(null);
    }

    private void finishRows(List<Row> rows, Visibility visibility) {
        for (Row row : rows) {
            row.setCostVisible(visibility.costVisible).setMovePriceVisible(visibility.movePriceVisible)
                    .setHiddenFields(visibility.hiddenFields);
            row.setCostPrice(scale(row.getCostPrice())).setCostAmount(scale(row.getCostAmount()))
                    .setMovePrice(scale(row.getMovePrice())).setMoveAmount(scale(row.getMoveAmount()));
            mask(row, visibility.hiddenFields);
        }
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void mask(Row row, Set<String> hidden) {
        if (hidden.contains("productCode")) row.setProductCode(null).setProductId(null);
        if (hidden.contains("documentNo")) row.setDocumentNo(null).setMoveId(null);
        if (hidden.contains("approveTime")) row.setApproveTime(null);
        if (hidden.contains("productName")) row.setProductName(null);
        if (hidden.contains("brand")) row.setBrand(null);
        if (hidden.contains("vehicleModel")) row.setVehicleModel(null);
        if (hidden.contains("originPlace")) row.setOriginPlace(null);
        if (hidden.contains("unitName")) row.setUnitName(null);
        if (hidden.contains("count")) row.setCount(null);
        if (hidden.contains("costPrice")) row.setCostPrice(null);
        if (hidden.contains("costAmount")) row.setCostAmount(null);
        if (hidden.contains("movePrice")) row.setMovePrice(null);
        if (hidden.contains("moveAmount")) row.setMoveAmount(null);
        if (hidden.contains("fromWarehouseName")) row.setFromWarehouseName(null).setFromWarehouseId(null);
        if (hidden.contains("fromShelf")) row.setFromShelf(null);
        if (hidden.contains("toWarehouseName")) row.setToWarehouseName(null).setToWarehouseId(null);
        if (hidden.contains("toShelf")) row.setToShelf(null);
        if (hidden.contains("creatorName")) row.setCreatorName(null);
        if (hidden.contains("approveUserName")) row.setApproveUserName(null);
        if (hidden.contains("standard")) row.setStandard(null);
        if (hidden.contains("drawingNo")) row.setDrawingNo(null);
        if (hidden.contains("featureCode")) row.setFeatureCode(null);
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
        if (filter.getApproveTimeFrom() != null && filter.getApproveTimeTo() != null
                && filter.getApproveTimeFrom().isAfter(filter.getApproveTimeTo())) {
            throw new ServiceException(400, "审核日期范围不正确");
        }
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
        private final boolean costVisible;
        private final boolean movePriceVisible;
        private final List<String> keywordColumns;

        private Visibility(Set<String> hiddenFields, boolean costVisible, boolean movePriceVisible,
                           List<String> keywordColumns) {
            this.hiddenFields = hiddenFields;
            this.costVisible = costVisible;
            this.movePriceVisible = movePriceVisible;
            this.keywordColumns = keywordColumns;
        }
    }
}
