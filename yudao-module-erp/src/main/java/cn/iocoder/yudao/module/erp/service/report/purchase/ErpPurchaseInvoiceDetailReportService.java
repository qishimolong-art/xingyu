package cn.iocoder.yudao.module.erp.service.report.purchase;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInvoiceDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInvoiceDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInvoiceDetailReportModels.Summary;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Service
public class ErpPurchaseInvoiceDetailReportService {
    private static final String MODULE = "erp_purchase_invoice";
    private static final int EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_LIMIT = 1_000_000L;
    private static final List<String> ALL_EXPORT_FIELDS = Arrays.asList("sequence", "createDate", "documentNo",
            "supplierName", "invoiceDate", "invoiceType", "invoiceNo", "productCode", "productName", "unitName",
            "count", "productPrice", "totalAmount", "settlementStatus", "taxRate", "vehicleModel", "brand",
            "originPlace", "drawingNo", "standard", "volume", "weight", "taxCategoryCode", "taxProductName",
            "taxProductModel", "taxUnit", "approveDate", "itemRemark", "businessNo", "businessApproveDate");

    @Resource private ErpPurchaseInvoiceDetailReportRepository repository;
    @Resource private PermissionApi permissionApi;
    @Resource private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @Transactional(readOnly = true)
    public PageResult<Row> getPage(Filter filter) {
        Visibility visibility = visibility();
        validate(filter, visibility);
        ErpPurchaseInvoiceDetailReportRepository.Scope scope = scope();
        if (scope == null) return PageResult.empty();
        long total = repository.count(filter, scope);
        if (total == 0) return PageResult.empty();
        long offset = (long) (filter.getPageNo() - 1) * filter.getPageSize();
        List<Row> rows = repository.page(filter, scope, offset, filter.getPageSize());
        prepareRows(rows, offset, visibility);
        return new PageResult<>(rows, total);
    }

    @Transactional(readOnly = true)
    public Summary getSummary(Filter filter) {
        Visibility visibility = visibility();
        validate(filter, visibility);
        ErpPurchaseInvoiceDetailReportRepository.Scope scope = scope();
        Summary value = scope == null ? new Summary().setRowCount(0L).setTotalCount(BigDecimal.ZERO)
                .setTotalAmount(BigDecimal.ZERO) : repository.summary(filter, scope);
        value.setPriceVisible(visibility.priceVisible).setHiddenFields(new ArrayList<>(visibility.hiddenFields));
        if (visibility.hiddenFields.contains("count")) value.setTotalCount(null);
        if (!visibility.priceVisible) value.setTotalAmount(null);
        return value;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter, HttpServletResponse response) throws IOException {
        Visibility visibility = visibility();
        validate(filter, visibility);
        ErpPurchaseInvoiceDetailReportRepository.Scope scope = scope();
        long total = scope == null ? 0 : repository.count(filter, scope);
        if (total > EXPORT_LIMIT) throw new ServiceException(400, "单次导出最多 100 万行，请缩小查询范围");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''"
                + URLEncoder.encode("采购票据明细查询.xlsx", "UTF-8").replace("+", "%20"));
        try (ExcelWriter writer = FastExcelFactory.write(response.getOutputStream(), Row.class)
                .includeColumnFieldNames(exportFields(visibility)).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("采购票据明细").build();
            if (total == 0) writer.write(Collections.emptyList(), sheet);
            for (long offset = 0; offset < total; offset += EXPORT_BATCH_SIZE) {
                List<Row> rows = repository.page(filter, scope, offset, EXPORT_BATCH_SIZE);
                prepareRows(rows, offset, visibility);
                writer.write(rows, sheet);
            }
        }
    }

    private ErpPurchaseInvoiceDetailReportRepository.Scope scope() {
        Long userId = getLoginUserId();
        if (userId == null) return null;
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(userId, MODULE);
        ErpFinanceVisibleScope visible = ErpFinanceVisibleScope.from(permission, userId);
        if (visible == null || !visible.hasAccess()) return null;
        return new ErpPurchaseInvoiceDetailReportRepository.Scope(TenantContextHolder.getRequiredTenantId(), visible);
    }

    private void prepareRows(List<Row> rows, long offset, Visibility visibility) {
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i).setSequence(offset + i + 1);
            for (String field : visibility.hiddenFields) mask(row, field);
        }
    }

    private Visibility visibility() {
        Set<String> hidden = new LinkedHashSet<>();
        map(hidden, "no", "documentNo"); map(hidden, "supplierId", "supplierName");
        map(hidden, "invoiceDate", "invoiceDate"); map(hidden, "invoiceType", "invoiceType");
        map(hidden, "invoiceNo", "invoiceNo"); map(hidden, "item_productCode", "productCode");
        if (isHidden("item_productId")) Collections.addAll(hidden, "productName", "unitName", "vehicleModel",
                "brand", "originPlace", "drawingNo", "standard", "weight");
        map(hidden, "item_count", "count"); map(hidden, "item_productPrice", "productPrice");
        map(hidden, "item_totalPrice", "totalAmount"); map(hidden, "item_remark", "itemRemark");
        if (isHidden("item_sourceInNo")) Collections.addAll(hidden, "businessNo", "businessApproveDate", "settlementStatus");
        if (isHidden("items")) Collections.addAll(hidden, "productCode", "productName", "unitName", "count",
                "productPrice", "totalAmount", "settlementStatus", "vehicleModel", "brand", "originPlace",
                "drawingNo", "standard", "weight", "itemRemark", "businessNo", "businessApproveDate");
        boolean priceVisible = !hidden.contains("productPrice") && !hidden.contains("totalAmount")
                && !isHidden("totalAmount");
        if (!priceVisible) Collections.addAll(hidden, "productPrice", "totalAmount");
        return new Visibility(hidden, priceVisible);
    }

    private void map(Set<String> hidden, String permissionField, String rowField) {
        if (isHidden(permissionField)) hidden.add(rowField);
    }
    private boolean isHidden(String field) { return fieldPermissionMasker.isFieldHidden(MODULE, field); }

    private void validate(Filter filter, Visibility visibility) {
        if (filter.getPageNo() < 1 || filter.getPageSize() < 1 || filter.getPageSize() > 1000) {
            throw new ServiceException(400, "分页参数不正确");
        }
        if (!visibility.priceVisible && (filter.getPriceMin() != null || filter.getPriceMax() != null
                || filter.getAmountMin() != null || filter.getAmountMax() != null)) {
            throw new ServiceException(403, "当前账号无权按采购票据价格或金额筛选");
        }
        boolean hiddenFilter = hiddenText(visibility, "documentNo", filter.getDocumentNo())
                || hiddenValue(visibility, "supplierName", filter.getSupplierId())
                || hiddenValue(visibility, "invoiceDate", filter.getInvoiceDate())
                || hiddenText(visibility, "invoiceType", filter.getInvoiceType())
                || hiddenText(visibility, "invoiceNo", filter.getInvoiceNo())
                || hiddenProductKeyword(visibility, filter.getProductKeyword())
                || hiddenRange(visibility, "count", filter.getCountMin(), filter.getCountMax())
                || hiddenText(visibility, "settlementStatus", filter.getSettlementStatus())
                || hiddenText(visibility, "vehicleModel", filter.getVehicleModel())
                || hiddenText(visibility, "brand", filter.getBrand())
                || hiddenText(visibility, "originPlace", filter.getOriginPlace())
                || hiddenText(visibility, "drawingNo", filter.getDrawingNo())
                || hiddenText(visibility, "standard", filter.getStandard())
                || hiddenRange(visibility, "weight", filter.getWeightMin(), filter.getWeightMax())
                || hiddenText(visibility, "itemRemark", filter.getItemRemark())
                || hiddenText(visibility, "businessNo", filter.getBusinessNo())
                || hiddenValue(visibility, "businessApproveDate", filter.getBusinessApproveDate());
        if (hiddenFilter) throw new ServiceException(403, "当前账号无权使用所选采购票据明细筛选条件");
    }

    private boolean hiddenText(Visibility visibility, String field, String value) {
        return visibility.hiddenFields.contains(field) && value != null && !value.trim().isEmpty();
    }
    private boolean hiddenValue(Visibility visibility, String field, Object value) {
        return visibility.hiddenFields.contains(field) && value != null;
    }
    private boolean hiddenRange(Visibility visibility, String field, Object min, Object max) {
        return visibility.hiddenFields.contains(field) && (min != null || max != null);
    }
    private boolean hiddenProductKeyword(Visibility visibility, String value) {
        return visibility.hiddenFields.contains("productCode") && visibility.hiddenFields.contains("productName")
                && value != null && !value.trim().isEmpty();
    }

    private void mask(Row row, String field) {
        switch (field) {
            case "documentNo": row.setDocumentNo(null); break; case "supplierName": row.setSupplierName(null); break;
            case "invoiceDate": row.setInvoiceDate(null); break; case "invoiceType": row.setInvoiceType(null); break;
            case "invoiceNo": row.setInvoiceNo(null); break; case "productCode": row.setProductCode(null); break;
            case "productName": row.setProductName(null); break; case "unitName": row.setUnitName(null); break;
            case "count": row.setCount(null); break; case "productPrice": row.setProductPrice(null); break;
            case "totalAmount": row.setTotalAmount(null); break; case "settlementStatus": row.setSettlementStatus(null); break;
            case "vehicleModel": row.setVehicleModel(null); break; case "brand": row.setBrand(null); break;
            case "originPlace": row.setOriginPlace(null); break; case "drawingNo": row.setDrawingNo(null); break;
            case "standard": row.setStandard(null); break; case "weight": row.setWeight(null); break;
            case "itemRemark": row.setItemRemark(null); break; case "businessNo": row.setBusinessNo(null); break;
            case "businessApproveDate": row.setBusinessApproveDate(null); break; default: break;
        }
    }

    private Set<String> exportFields(Visibility visibility) {
        Set<String> fields = new LinkedHashSet<>(ALL_EXPORT_FIELDS); fields.removeAll(visibility.hiddenFields); return fields;
    }

    private static class Visibility {
        private final Set<String> hiddenFields; private final boolean priceVisible;
        private Visibility(Set<String> hiddenFields, boolean priceVisible) { this.hiddenFields = hiddenFields; this.priceVisible = priceVisible; }
    }
}
