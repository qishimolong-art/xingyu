package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockTransferInDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockTransferInDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockTransferInDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 调拨入库逐明细查询报表服务。 */
@Service
public class ErpStockTransferInDetailReportService {

    private static final String MOVE_MODULE = "erp_stock_transfer_in";
    private static final String PRODUCT_MODULE = "erp_product";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;
    private static final List<String> EXPORT_FIELDS = Arrays.asList("moveDate", "documentNo", "productCode",
            "productName", "vehicleModel", "originPlace", "toWarehouseName", "count", "productPrice",
            "totalAmount", "costUnitPrice", "costAmount", "creatorName", "fromWarehouseName", "standard",
            "drawingNo");

    @Resource
    private ErpStockTransferInDetailReportRepository repository;
    @Resource
    private ErpStockMoveService stockMoveService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        Visibility visibility = visibility();
        validate(filter, visibility);
        ErpStockTransferInDetailReportRepository.Scope scope = scope(visibility);
        long total = repository.count(filter, scope);
        if (total == 0) return PageResult.empty();
        long offset = (long) (filter.getPageNo() - 1) * filter.getPageSize();
        List<Row> rows = repository.page(filter, scope, offset, filter.getPageSize());
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
                    .setTotalAmount(BigDecimal.ZERO).setTotalCostAmount(BigDecimal.ZERO);
        }
        summary.setPriceVisible(visibility.priceVisible).setAmountVisible(visibility.amountVisible)
                .setHiddenFields(visibility.hiddenFields);
        if (visibility.hiddenFields.contains("count")) summary.setTotalCount(null);
        if (!visibility.amountVisible) {
            summary.setTotalAmount(null).setTotalCostAmount(null);
        } else {
            summary.setTotalAmount(scale(summary.getTotalAmount())).setTotalCostAmount(scale(summary.getTotalCostAmount()));
        }
        return summary;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        Visibility visibility = visibility();
        validate(filter, visibility);
        ErpStockTransferInDetailReportRepository.Scope scope = scope(visibility);
        long total = repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("调拨入库查询报表.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(visibility)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("调拨入库明细").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                finishRows(rows, visibility);
                writer.write(rows, sheet);
            }
        }
    }

    private ErpStockTransferInDetailReportRepository.Scope scope(Visibility visibility) {
        ErpStockTransferOutPermissionScope permissionScope = stockMoveService.getTransferInPermissionScope();
        return new ErpStockTransferInDetailReportRepository.Scope(TenantContextHolder.getRequiredTenantId(),
                permissionScope, visibility.productKeywordColumns);
    }

    private Visibility visibility() {
        Set<String> hidden = new LinkedHashSet<>();
        boolean itemsHidden = moveHidden("items");
        mapMove(hidden, "moveTime", "moveDate");
        mapMove(hidden, "no", "documentNo");
        mapMove(hidden, "item_fromWarehouseId", "fromWarehouseName");
        mapMove(hidden, "item_toWarehouseId", "toWarehouseName");
        mapMove(hidden, "item_count", "count");
        mapMove(hidden, "creatorName", "creatorName");
        if (itemsHidden) {
            Collections.addAll(hidden, "fromWarehouseName", "toWarehouseName", "count");
        }
        if (moveHidden("item_productId") || itemsHidden) {
            Collections.addAll(hidden, "productCode", "productName", "vehicleModel", "originPlace",
                    "standard", "drawingNo");
        }
        mapProduct(hidden, "code", "productCode");
        mapProduct(hidden, "name", "productName");
        mapProduct(hidden, "vehicleModel", "vehicleModel");
        mapProduct(hidden, "originPlace", "originPlace");
        mapProduct(hidden, "standard", "standard");
        mapProduct(hidden, "drawingNo", "drawingNo");
        boolean priceVisible = !itemsHidden && !moveHidden("item_productPrice");
        boolean amountVisible = priceVisible && !moveHidden("item_totalPrice") && !moveHidden("totalPrice");
        if (!priceVisible) Collections.addAll(hidden, "productPrice", "costUnitPrice", "totalAmount", "costAmount");
        else if (!amountVisible) Collections.addAll(hidden, "totalAmount", "costAmount");
        List<String> productColumns = new ArrayList<>();
        if (!hidden.contains("productCode")) productColumns.add("product_code");
        if (!hidden.contains("productName")) productColumns.add("product_name");
        if (!hidden.contains("vehicleModel")) productColumns.add("vehicle_model");
        if (!hidden.contains("originPlace")) productColumns.add("origin_place");
        if (!hidden.contains("standard")) productColumns.add("standard");
        if (!hidden.contains("drawingNo")) productColumns.add("drawing_no");
        return new Visibility(hidden, priceVisible, amountVisible, productColumns);
    }

    private void mapMove(Set<String> hidden, String source, String target) {
        if (moveHidden(source)) hidden.add(target);
    }

    private void mapProduct(Set<String> hidden, String source, String target) {
        if (fieldPermissionMasker.isFieldHidden(PRODUCT_MODULE, source)) hidden.add(target);
    }

    private boolean moveHidden(String field) {
        return fieldPermissionMasker.isFieldHidden(MOVE_MODULE, field);
    }

    private void validate(Filter filter, Visibility visibility) {
        if (filter.getPageNo() < 1 || filter.getPageSize() < 1 || filter.getPageSize() > 1000) {
            throw new ServiceException(400, "分页参数不正确");
        }
        if (!visibility.priceVisible && (filter.getPriceMin() != null || filter.getPriceMax() != null
                || filter.getAmountMin() != null || filter.getAmountMax() != null)) {
            throw new ServiceException(403, "当前账号无权按调拨价格或金额筛选");
        }
        if (!visibility.amountVisible && (filter.getAmountMin() != null || filter.getAmountMax() != null)) {
            throw new ServiceException(403, "当前账号无权按调拨金额筛选");
        }
        boolean hiddenFilter = hiddenValue(visibility, "moveDate", filter.getMoveDate())
                || hiddenText(visibility, "documentNo", filter.getDocumentNo())
                || hiddenValue(visibility, "fromWarehouseName", filter.getFromWarehouseId())
                || hiddenValue(visibility, "toWarehouseName", filter.getToWarehouseId())
                || hiddenRange(visibility, "count", filter.getCountMin(), filter.getCountMax())
                || hiddenValue(visibility, "creatorName", filter.getCreatorId())
                || hiddenText(visibility, "vehicleModel", filter.getVehicleModel())
                || hiddenText(visibility, "originPlace", filter.getOriginPlace())
                || hiddenText(visibility, "standard", filter.getStandard())
                || hiddenText(visibility, "drawingNo", filter.getDrawingNo());
        if (hiddenFilter) throw new ServiceException(403, "当前账号无权使用所选调拨入库筛选条件");
        if (StringUtils.hasText(filter.getProductKeyword()) && visibility.productKeywordColumns.isEmpty()) {
            throw new ServiceException(403, "当前账号无权按配件字段筛选");
        }
        validateRange("数量", filter.getCountMin(), filter.getCountMax());
        validateRange("单价", filter.getPriceMin(), filter.getPriceMax());
        validateRange("金额", filter.getAmountMin(), filter.getAmountMax());
    }

    private void validateRange(String name, BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new ServiceException(400, name + "下限不能大于上限");
        }
    }

    private boolean hiddenText(Visibility visibility, String field, String value) {
        return visibility.hiddenFields.contains(field) && StringUtils.hasText(value);
    }

    private boolean hiddenValue(Visibility visibility, String field, Object value) {
        return visibility.hiddenFields.contains(field) && value != null;
    }

    private boolean hiddenRange(Visibility visibility, String field, Object min, Object max) {
        return visibility.hiddenFields.contains(field) && (min != null || max != null);
    }

    private void finishRows(List<Row> rows, Visibility visibility) {
        for (Row row : rows) {
            row.setPriceVisible(visibility.priceVisible).setAmountVisible(visibility.amountVisible)
                    .setHiddenFields(visibility.hiddenFields);
            row.setCount(scale(row.getCount())).setProductPrice(scale(row.getProductPrice()))
                    .setTotalAmount(scale(row.getTotalAmount())).setCostUnitPrice(scale(row.getCostUnitPrice()))
                    .setCostAmount(scale(row.getCostAmount()));
            for (String field : visibility.hiddenFields) mask(row, field);
        }
    }

    private BigDecimal scale(BigDecimal value) {
        return value;
    }

    private void mask(Row row, String field) {
        switch (field) {
            case "moveDate": row.setMoveDate(null); break;
            case "documentNo": row.setDocumentNo(null); break;
            case "productCode": row.setProductCode(null); break;
            case "productName": row.setProductName(null); break;
            case "vehicleModel": row.setVehicleModel(null); break;
            case "originPlace": row.setOriginPlace(null); break;
            case "fromWarehouseName": row.setFromWarehouseName(null); break;
            case "count": row.setCount(null); break;
            case "productPrice": row.setProductPrice(null); break;
            case "totalAmount": row.setTotalAmount(null); break;
            case "costUnitPrice": row.setCostUnitPrice(null); break;
            case "costAmount": row.setCostAmount(null); break;
            case "creatorName": row.setCreatorName(null); break;
            case "toWarehouseName": row.setToWarehouseName(null); break;
            case "standard": row.setStandard(null); break;
            case "drawingNo": row.setDrawingNo(null); break;
            default: break;
        }
    }

    private Set<String> exportFields(Visibility visibility) {
        Set<String> fields = new LinkedHashSet<>(EXPORT_FIELDS);
        fields.removeAll(visibility.hiddenFields);
        return fields;
    }

    private static final class Visibility {
        private final Set<String> hiddenFields;
        private final boolean priceVisible;
        private final boolean amountVisible;
        private final List<String> productKeywordColumns;

        private Visibility(Set<String> hiddenFields, boolean priceVisible, boolean amountVisible,
                           List<String> productKeywordColumns) {
            this.hiddenFields = hiddenFields;
            this.priceVisible = priceVisible;
            this.amountVisible = amountVisible;
            this.productKeywordColumns = productKeywordColumns;
        }
    }
}
