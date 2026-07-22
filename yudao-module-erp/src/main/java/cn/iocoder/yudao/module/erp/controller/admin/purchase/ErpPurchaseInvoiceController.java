package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoicePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInvoiceService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_PROCESS_NOT_SUPPORT;

@Tag(name = "管理后台 - ERP 采购票据")
@RestController
@RequestMapping("/erp/purchase-invoice")
@Validated
public class ErpPurchaseInvoiceController {

    private static final String FIELD_PERMISSION_MODULE = "erp_purchase_invoice";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();
    private static final Map<String, String> IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "supplierId", "supplierName",
            "supplierName", "supplierName",
            "invoiceDate", "invoiceDate",
            "invoiceType", "invoiceType",
            "invoiceNo", "invoiceNo",
            "invoiceCount", "invoiceCount",
            "remark", "remark",
            "sourceInNo", "sourceInNo",
            "sourceInItemId", "sourceInItemId",
            "productId", "productCode",
            "productCode", "productCode",
            "item_productId", "productCode",
            "count", "count",
            "item_count", "count",
            "productPrice", "productPrice",
            "item_productPrice", "productPrice",
            "item_remark", "itemRemark",
            "itemRemark", "itemRemark");

    @Resource
    private ErpPurchaseInvoiceService purchaseInvoiceService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpFieldConfigService fieldConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建采购票据")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:create')")
    public CommonResult<Long> createPurchaseInvoice(@Valid @RequestBody ErpPurchaseInvoiceSaveReqVO createReqVO) {
        return success(purchaseInvoiceService.createPurchaseInvoice(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购票据")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:update')")
    public CommonResult<Boolean> updatePurchaseInvoice(@Valid @RequestBody ErpPurchaseInvoiceSaveReqVO updateReqVO) {
        purchaseInvoiceService.updatePurchaseInvoice(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新采购票据状态")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:update-status')")
    public CommonResult<Boolean> updatePurchaseInvoiceStatus(@RequestParam("id") Long id,
                                                             @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        if (!Integer.valueOf(20).equals(status)) {
            throw exception(PURCHASE_INVOICE_PROCESS_NOT_SUPPORT);
        }
        purchaseInvoiceService.updatePurchaseInvoiceStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购票据")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:delete')")
    public CommonResult<Boolean> deletePurchaseInvoice(@RequestParam("ids") List<Long> ids) {
        purchaseInvoiceService.deletePurchaseInvoice(ids);
        return success(true);
    }

    @PostMapping("/import")
    @Operation(summary = "导入采购票据")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:import')")
    public CommonResult<ErpPurchaseImportResultRespVO> importPurchaseInvoice(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPurchaseInvoiceImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseInvoiceImportExcelVO.class);
        return success(purchaseInvoiceService.importPurchaseInvoiceList(list));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得采购票据导入模板")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchaseInvoiceImportExcelVO example = new ErpPurchaseInvoiceImportExcelVO();
        example.setNo("CGPJ-IMPORT-001");
        example.setSupplierName("Example Supplier");
        example.setInvoiceDate("2026-07-02");
        example.setInvoiceType("增值税专用发票");
        example.setInvoiceNo("INV-20260702-001");
        example.setInvoiceCount(1);
        example.setRemark("Invoice remark");
        example.setSourceInNo("CGRK202607020001");
        example.setSourceInItemId(10001L);
        example.setProductCode("P000001");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("10.00"));
        example.setItemRemark("Item remark");

        ErpPurchaseInvoiceImportExcelVO secondItem = new ErpPurchaseInvoiceImportExcelVO();
        secondItem.setSourceInNo("CGRK202607020001");
        secondItem.setSourceInItemId(10002L);
        secondItem.setProductCode("P000002");
        secondItem.setCount(new BigDecimal("2"));
        secondItem.setProductPrice(new BigDecimal("20.00"));

        ExcelUtils.writeImportTemplate(response, "采购票据导入模板.xls", "采购票据",
                ErpPurchaseInvoiceImportExcelVO.class, java.util.Arrays.asList(example, secondItem), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.PURCHASE_INVOICE, ErpPurchaseInvoiceImportExcelVO.class,
                        IMPORT_FIELD_ALIAS_MAP));
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购票据")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:query')")
    public CommonResult<ErpPurchaseInvoiceRespVO> getPurchaseInvoice(@RequestParam("id") Long id) {
        ErpPurchaseInvoiceDO purchaseInvoice = purchaseInvoiceService.getPurchaseInvoice(id);
        if (purchaseInvoice == null) {
            return success(null);
        }
        List<ErpPurchaseInvoiceItemDO> itemList = purchaseInvoiceService.getPurchaseInvoiceItemListByInvoiceId(id);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(itemList, ErpPurchaseInvoiceItemDO::getProductId)));
        Map<Long, ErpPurchaseInItemDO> sourceInItemMap = getSourceInItemMap(itemList);
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                Collections.singleton(purchaseInvoice.getSupplierId()));
        DeptRespDTO dept = purchaseInvoice.getDeptId() == null ? null : deptApi.getDept(purchaseInvoice.getDeptId());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                collectUserIds(Collections.singletonList(purchaseInvoice)));

        ErpPurchaseInvoiceRespVO respVO = BeanUtils.toBean(purchaseInvoice, ErpPurchaseInvoiceRespVO.class);
        fillInvoiceRespItems(respVO, itemList, productMap, sourceInItemMap);
        MapUtils.findAndThen(supplierMap, respVO.getSupplierId(), supplier -> {
            respVO.setSupplierName(supplier.getName());
            respVO.setSupplierType(supplier.getSupplierType());
        });
        if (dept != null) {
            respVO.setDeptName(dept.getName());
        }
        fillUserNames(respVO, userMap);
        fillAuditInfo(respVO);
        fieldPermissionMasker.mask("erp_purchase_invoice", respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购票据分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:query')")
    public CommonResult<PageResult<ErpPurchaseInvoiceRespVO>> getPurchaseInvoicePage(@Valid ErpPurchaseInvoicePageReqVO pageReqVO) {
        PageResult<ErpPurchaseInvoiceDO> pageResult = purchaseInvoiceService.getPurchaseInvoicePage(pageReqVO);
        return success(buildPurchaseInvoiceVOPageResult(pageResult));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购票据 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseInvoiceExcel(@Valid ErpPurchaseInvoicePageReqVO pageReqVO,
                                           @RequestParam(value = "fields", required = false) String fields,
                                           HttpServletResponse response) throws IOException {
        List<ErpPurchaseInvoiceRespVO> list;
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            list = buildPurchaseInvoiceVOPageResult(new PageResult<>(
                    purchaseInvoiceService.getPurchaseInvoiceList(pageReqVO.getIds()),
                    (long) pageReqVO.getIds().size())).getList();
        } else {
            pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
            list = buildPurchaseInvoiceVOPageResult(
                    purchaseInvoiceService.getPurchaseInvoicePage(pageReqVO)).getList();
        }
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpPurchaseInvoiceExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "采购票据.xls", "数据", ErpPurchaseInvoiceExportRespVO.class,
                buildPurchaseInvoiceExportList(list), includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "获得采购票据导出字段")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getPurchaseInvoiceExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpPurchaseInvoiceExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    private PageResult<ErpPurchaseInvoiceRespVO> buildPurchaseInvoiceVOPageResult(PageResult<ErpPurchaseInvoiceDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpPurchaseInvoiceItemDO> itemList = purchaseInvoiceService.getPurchaseInvoiceItemListByInvoiceIds(
                convertSet(pageResult.getList(), ErpPurchaseInvoiceDO::getId));
        Map<Long, List<ErpPurchaseInvoiceItemDO>> itemMap = convertMultiMap(itemList, ErpPurchaseInvoiceItemDO::getInvoiceId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(itemList, ErpPurchaseInvoiceItemDO::getProductId)));
        Map<Long, ErpPurchaseInItemDO> sourceInItemMap = getSourceInItemMap(itemList);
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseInvoiceDO::getSupplierId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpPurchaseInvoiceDO::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(collectUserIds(pageResult.getList()));

        PageResult<ErpPurchaseInvoiceRespVO> respPage = BeanUtils.toBean(pageResult, ErpPurchaseInvoiceRespVO.class);
        respPage.getList().forEach(invoice -> {
            fillInvoiceRespItems(invoice, itemMap.get(invoice.getId()), productMap, sourceInItemMap);
            MapUtils.findAndThen(supplierMap, invoice.getSupplierId(), supplier -> {
                invoice.setSupplierName(supplier.getName());
                invoice.setSupplierType(supplier.getSupplierType());
            });
            MapUtils.findAndThen(deptMap, invoice.getDeptId(), dept -> invoice.setDeptName(dept.getName()));
            fillUserNames(invoice, userMap);
            fillAuditInfo(invoice);
        });
        fieldPermissionMasker.maskList(FIELD_PERMISSION_MODULE, respPage.getList());
        return respPage;
    }

    private void fillInvoiceRespItems(ErpPurchaseInvoiceRespVO invoice, List<ErpPurchaseInvoiceItemDO> itemList,
                                      Map<Long, ErpProductRespVO> productMap,
                                      Map<Long, ErpPurchaseInItemDO> sourceInItemMap) {
        List<ErpPurchaseInvoiceRespVO.Item> items = BeanUtils.toBean(itemList, ErpPurchaseInvoiceRespVO.Item.class);
        if (items == null) {
            items = new ArrayList<>();
        }
        for (ErpPurchaseInvoiceRespVO.Item item : items) {
            Long resolvedWarehouseId = null;
            if (item.getSourceInItemId() != null) {
                ErpPurchaseInItemDO sourceInItem = sourceInItemMap.get(item.getSourceInItemId());
                resolvedWarehouseId = sourceInItem == null ? null : sourceInItem.getWarehouseId();
            }
            final Long warehouseId = resolvedWarehouseId;
            ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                    stockService.getStock(item.getProductId(), warehouseId));
            item.setStockCount(stock != null && stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO);
            ErpProductRespVO product = productMap.get(item.getProductId());
            if (product == null) {
                continue;
            }
            item.setProductName(product.getName());
            item.setProductCode(product.getCode());
            if (item.getProductUnitName() == null) {
                item.setProductUnitName(product.getUnitName());
            }
            if (item.getProductBarCode() == null) {
                item.setProductBarCode(product.getBarCode());
            }
        }
        invoice.setItems(items);
        invoice.setProductNames(CollUtil.join(items, ", ", ErpPurchaseInvoiceRespVO.Item::getProductName));
        invoice.setDisplayTaxPercent(buildDisplayTaxPercent(itemList));
    }

    private Map<Long, ErpPurchaseInItemDO> getSourceInItemMap(List<ErpPurchaseInvoiceItemDO> itemList) {
        Set<Long> sourceInItemIds = convertSet(itemList, ErpPurchaseInvoiceItemDO::getSourceInItemId);
        sourceInItemIds.remove(null);
        if (CollUtil.isEmpty(sourceInItemIds)) {
            return Collections.emptyMap();
        }
        return convertMap(purchaseInItemMapper.selectBatchIds(sourceInItemIds), ErpPurchaseInItemDO::getId);
    }

    private void fillAuditInfo(ErpPurchaseInvoiceRespVO invoice) {
        if (ErpAuditStatus.APPROVE.getStatus().equals(invoice.getStatus())) {
            invoice.setAuditUserName(invoice.getUpdaterName());
            invoice.setAuditTime(invoice.getUpdateTime());
            return;
        }
        invoice.setAuditUserName(null);
        invoice.setAuditTime(null);
    }

    private String buildDisplayTaxPercent(List<ErpPurchaseInvoiceItemDO> itemList) {
        if (CollUtil.isEmpty(itemList)) {
            return null;
        }
        Set<String> taxPercents = new LinkedHashSet<>();
        for (ErpPurchaseInvoiceItemDO item : itemList) {
            BigDecimal taxPercent = item.getTaxPercent();
            if (taxPercent != null) {
                taxPercents.add(taxPercent.stripTrailingZeros().toPlainString());
            }
        }
        if (taxPercents.isEmpty()) {
            return null;
        }
        if (taxPercents.size() > 1) {
            return "多税率";
        }
        return taxPercents.iterator().next();
    }

    private List<ErpPurchaseInvoiceExportRespVO> buildPurchaseInvoiceExportList(List<ErpPurchaseInvoiceRespVO> list) {
        List<ErpPurchaseInvoiceExportRespVO> rows = new ArrayList<>();
        for (ErpPurchaseInvoiceRespVO invoice : list) {
            if (CollUtil.isEmpty(invoice.getItems())) {
                rows.add(buildPurchaseInvoiceExportRow(invoice, null, true));
                continue;
            }
            for (int i = 0; i < invoice.getItems().size(); i++) {
                rows.add(buildPurchaseInvoiceExportRow(invoice, invoice.getItems().get(i), i == 0));
            }
        }
        return rows;
    }

    private ErpPurchaseInvoiceExportRespVO buildPurchaseInvoiceExportRow(ErpPurchaseInvoiceRespVO invoice,
                                                                         ErpPurchaseInvoiceRespVO.Item item,
                                                                         boolean fillMainFields) {
        ErpPurchaseInvoiceExportRespVO row = fillMainFields
                ? BeanUtils.toBean(invoice, ErpPurchaseInvoiceExportRespVO.class)
                : new ErpPurchaseInvoiceExportRespVO();
        if (fillMainFields) {
            row.setSupplierName(invoice.getSupplierName());
            row.setDeptName(invoice.getDeptName());
            row.setHandlerName(invoice.getHandlerName());
            row.setCreatorName(invoice.getCreatorName());
            row.setUpdaterName(invoice.getUpdaterName());
            row.setRemark(invoice.getRemark());
        }
        if (item == null) {
            return row;
        }
        row.setSourceInNo(item.getSourceInNo());
        row.setSourceInItemId(item.getSourceInItemId());
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setProductBarCode(item.getProductBarCode());
        row.setProductUnitName(item.getProductUnitName());
        row.setCount(item.getCount());
        row.setProductPrice(item.getProductPrice());
        row.setTaxExclusivePrice(item.getTaxExclusivePrice());
        row.setTaxPercent(item.getTaxPercent());
        row.setTaxPrice(item.getTaxPrice());
        row.setItemTotalPrice(item.getTotalPrice());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private Set<Long> collectUserIds(List<ErpPurchaseInvoiceDO> list) {
        Set<Long> userIds = new LinkedHashSet<>();
        list.forEach(invoice -> {
            addUserId(userIds, invoice.getCreator());
            addUserId(userIds, invoice.getUpdater());
            addLongUserId(userIds, invoice.getHandlerId());
        });
        return userIds;
    }

    private void fillUserNames(ErpPurchaseInvoiceRespVO invoice, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(invoice.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> invoice.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(invoice.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> invoice.setUpdaterName(user.getNickname()));
        }
        if (invoice.getHandlerId() != null) {
            MapUtils.findAndThen(userMap, invoice.getHandlerId(), user -> invoice.setHandlerName(user.getNickname()));
        }
    }

    private void addUserId(Set<Long> userIds, String userId) {
        Long parsed = parseUserId(userId);
        if (parsed != null) {
            userIds.add(parsed);
        }
    }

    private void addLongUserId(Set<Long> userIds, Long userId) {
        if (userId != null) {
            userIds.add(userId);
        }
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("no", "main");
        map.put("supplierName", "main");
        map.put("status", "main");
        map.put("invoiceDate", "main");
        map.put("invoiceType", "main");
        map.put("invoiceNo", "main");
        map.put("invoiceCount", "main");
        map.put("taxExclusiveAmount", "main");
        map.put("taxAmount", "main");
        map.put("totalAmount", "main");
        map.put("deptName", "main");
        map.put("handlerName", "main");
        map.put("fileUrl", "main");
        map.put("creatorName", "system");
        map.put("createTime", "system");
        map.put("updaterName", "system");
        map.put("updateTime", "system");
        map.put("remark", "main");
        map.put("sourceInNo", "detail");
        map.put("sourceInItemId", "detail");
        map.put("productCode", "detail");
        map.put("productName", "detail");
        map.put("productBarCode", "detail");
        map.put("productUnitName", "detail");
        map.put("count", "detail");
        map.put("productPrice", "detail");
        map.put("taxExclusivePrice", "detail");
        map.put("taxPercent", "detail");
        map.put("taxPrice", "detail");
        map.put("itemTotalPrice", "detail");
        map.put("itemRemark", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("supplierName", "supplierId");
        map.put("deptName", "deptId");
        map.put("handlerName", "handlerId");
        map.put("productCode", "item_productCode");
        map.put("productName", "item_productId");
        map.put("productBarCode", "item_productBarCode");
        map.put("productUnitName", "item_productUnitName");
        map.put("count", "item_count");
        map.put("productPrice", "item_productPrice");
        map.put("taxExclusivePrice", "item_taxExclusivePrice");
        map.put("taxPercent", "item_taxPercent");
        map.put("taxPrice", "item_taxPrice");
        map.put("itemTotalPrice", "item_totalPrice");
        map.put("itemRemark", "item_remark");
        return map;
    }

}
