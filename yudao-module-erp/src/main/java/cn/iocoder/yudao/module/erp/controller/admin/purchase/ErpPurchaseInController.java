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
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateSaleCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateSaleCartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateTransferOutReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateTransferOutRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaleCartableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInTransferOutableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseInFromOrderReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInvoiceService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "Admin - ERP Purchase In")
@RestController
@RequestMapping("/erp/purchase-in")
@Validated
public class ErpPurchaseInController {

    private static final Integer RETURN_STATUS_NONE = 0;
    private static final Integer RETURN_STATUS_PART = 1;
    private static final Integer RETURN_STATUS_ALL = 2;
    private static final String FIELD_PERMISSION_MODULE = "erp_purchase_in";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();
    private static final Map<String, String> DETAIL_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "productId", "productCode",
            "warehouseId", "warehouseName",
            "item_productId", "productCode",
            "item_warehouseId", "warehouseName",
            "count", "count",
            "item_count", "count",
            "itemCount", "count",
            "productPrice", "productPrice",
            "wholeQty", "wholeQty",
            "warehousePosition", "warehousePosition",
            "batchNo", "batchNo",
            "item_remark", "remark",
            "itemRemark", "remark");
    private static final Map<String, String> ORDER_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "supplierId", "supplierName",
            "supplierName", "supplierName",
            "inTime", "inTime",
            "factoryOrderNo", "factoryOrderNo",
            "remark", "remark",
            "productId", "productCode",
            "warehouseId", "warehouseName",
            "item_productId", "productCode",
            "item_warehouseId", "warehouseName",
            "count", "itemCount",
            "item_count", "itemCount",
            "itemCount", "itemCount",
            "productPrice", "productPrice",
            "wholeQty", "wholeQty",
            "warehousePosition", "warehousePosition",
            "batchNo", "batchNo",
            "item_remark", "itemRemark",
            "itemRemark", "itemRemark");

    @Resource
    private ErpPurchaseInService purchaseInService;
    @Resource
    private ErpPurchaseInvoiceService purchaseInvoiceService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockInBillService stockInBillService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpFieldConfigService fieldConfigService;

    @PostMapping("/create")
    @Operation(summary = "Create purchase in")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:create')")
    public CommonResult<Long> createPurchaseIn(@Valid @RequestBody ErpPurchaseInSaveReqVO createReqVO) {
        return success(purchaseInService.createPurchaseIn(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "Update purchase in")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:update')")
    public CommonResult<Boolean> updatePurchaseIn(@Valid @RequestBody ErpPurchaseInSaveReqVO updateReqVO) {
        purchaseInService.updatePurchaseIn(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "Update purchase in status")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:update-status')")
    public CommonResult<Boolean> updatePurchaseInStatus(@RequestParam("id") Long id,
                                                        @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        purchaseInService.updatePurchaseInStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete purchase in")
    @Parameter(name = "ids", description = "IDs", required = true)
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:delete')")
    public CommonResult<Boolean> deletePurchaseIn(@RequestParam("ids") List<Long> ids) {
        purchaseInService.deletePurchaseIn(ids);
        return success(true);
    }

    @PostMapping("/import")
    @Operation(summary = "解析采购入库明细导入 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:create')")
    public CommonResult<ErpPurchaseInImportRespVO> importPurchaseIn(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPurchaseInImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseInImportExcelVO.class);
        return success(purchaseInService.importPurchaseInItems(list));
    }

    @PostMapping("/import-order")
    @Operation(summary = "Import purchase in order")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:create')")
    public CommonResult<ErpPurchaseImportResultRespVO> importPurchaseInOrder(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPurchaseInOrderImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseInOrderImportExcelVO.class);
        return success(purchaseInService.importPurchaseInOrderList(list));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得采购入库明细导入模板")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:create')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchaseInImportExcelVO example = new ErpPurchaseInImportExcelVO();
        example.setProductCode("P000001");
        example.setWarehouseName("Main Warehouse");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("10.00"));
        example.setWholeQty(1);
        example.setWarehousePosition("A-01-01");
        example.setBatchNo("B20260526");
        example.setRemark("Example");
        ExcelUtils.writeImportTemplate(response, "采购入库明细导入模板.xls", "采购入库明细",
                ErpPurchaseInImportExcelVO.class, Collections.singletonList(example), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.PURCHASE_IN, ErpPurchaseInImportExcelVO.class,
                        DETAIL_IMPORT_FIELD_ALIAS_MAP));
    }

    @GetMapping("/get-order-import-template")
    @Operation(summary = "Get purchase in order import template")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:create')")
    public void getOrderImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchaseInOrderImportExcelVO example = new ErpPurchaseInOrderImportExcelVO();
        example.setNo("RK-IMPORT-001");
        example.setSupplierName("Example Supplier");
        example.setInTime("2026-06-05 09:00:00");
        example.setFactoryOrderNo("FACTORY-001");
        example.setRemark("Order remark");
        example.setProductCode("P000001");
        example.setWarehouseName("Main Warehouse");
        example.setItemCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("10.00"));
        example.setWholeQty(1);
        example.setWarehousePosition("A-01-01");
        example.setBatchNo("B20260605");
        example.setItemRemark("Item remark");

        ErpPurchaseInOrderImportExcelVO secondItem = new ErpPurchaseInOrderImportExcelVO();
        secondItem.setProductCode("P000002");
        secondItem.setWarehouseName("Main Warehouse");
        secondItem.setItemCount(new BigDecimal("2"));
        secondItem.setProductPrice(new BigDecimal("20.00"));

        ExcelUtils.writeImportTemplate(response, "采购入库导入模板.xls", "采购入库",
                ErpPurchaseInOrderImportExcelVO.class, java.util.Arrays.asList(example, secondItem), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.PURCHASE_IN, ErpPurchaseInOrderImportExcelVO.class,
                        ORDER_IMPORT_FIELD_ALIAS_MAP));
    }

    @GetMapping("/get")
    @Operation(summary = "Get purchase in")
    @Parameter(name = "id", description = "ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:query')")
    public CommonResult<ErpPurchaseInRespVO> getPurchaseIn(@RequestParam("id") Long id) {
        ErpPurchaseInDO purchaseIn = purchaseInService.getPurchaseIn(id);
        if (purchaseIn == null) {
            return success(null);
        }
        markHasInvoice(purchaseIn);
        List<ErpPurchaseInItemDO> purchaseInItemList = purchaseInService.getPurchaseInItemListByInId(id);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(purchaseInItemList, ErpPurchaseInItemDO::getProductId)));
        Map<Long, BigDecimal> returnCountMap = getApprovedReturnCountMap(purchaseInItemList);
        Map<Long, BigDecimal> transferOutCountMap = getTransferOutCountMap(purchaseInItemList);
        Set<Long> userIds = new HashSet<>();
        collectUserIds(userIds, purchaseIn);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        DeptRespDTO dept = purchaseIn.getDeptId() == null ? null : deptApi.getDept(purchaseIn.getDeptId());
        ErpSupplierDO supplier = purchaseIn.getSupplierId() == null
                ? null : supplierService.getSupplier(purchaseIn.getSupplierId());
        ErpPurchaseInRespVO respVO = BeanUtils.toBean(purchaseIn, ErpPurchaseInRespVO.class, purchaseInVO -> {
            purchaseInVO.setItems(BeanUtils.toBean(purchaseInItemList, ErpPurchaseInRespVO.Item.class, item -> {
                ErpStockDO stock = stockService.getStock(item.getProductId(), item.getWarehouseId());
                item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                        .setProductCode(product.getCode()).setBatchNoEnabled(product.getBatchNoEnabled()));
                fillPurchaseInItemReturnInfo(item, returnCountMap);
            }));
            purchaseInVO.setItemCount(CollUtil.size(purchaseInItemList));
            fillPurchaseInReturnInfo(purchaseInVO);
            fillPurchaseInTransferOutInfo(purchaseInVO, transferOutCountMap);
            fillUserNames(purchaseInVO, userMap);
            if (dept != null) {
                purchaseInVO.setDeptName(dept.getName());
            }
            if (supplier != null) {
                purchaseInVO.setSupplierName(supplier.getName());
            }
        });
        fillPurchaseInStockInBillInfo(respVO, id);
        fieldPermissionMasker.mask("erp_purchase_in", respVO);
        return success(respVO);
    }

    @GetMapping("/list-items")
    @Operation(summary = "Get purchase in items")
    @Parameter(name = "inId", description = "Purchase in ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:query')")
    public CommonResult<List<ErpPurchaseInRespVO.Item>> getPurchaseInItems(@RequestParam("inId") Long inId) {
        List<ErpPurchaseInItemDO> purchaseInItemList = purchaseInService.getPurchaseInItemListByInId(inId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(purchaseInItemList, ErpPurchaseInItemDO::getProductId)));
        List<ErpPurchaseInRespVO.Item> items = BeanUtils.toBean(purchaseInItemList, ErpPurchaseInRespVO.Item.class);
        if (items == null) {
            return success(Collections.emptyList());
        }
        Map<Long, BigDecimal> returnCountMap = getApprovedReturnCountMap(purchaseInItemList);
        items.forEach(item -> MapUtils.findAndThen(productMap, item.getProductId(),
                product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode())
                        .setProductUnitName(product.getUnitName())
                        .setProductCode(product.getCode())
                        .setBatchNoEnabled(product.getBatchNoEnabled())));
        items.forEach(item -> fillPurchaseInItemReturnInfo(item, returnCountMap));
        return success(items);
    }

    @GetMapping("/page")
    @Operation(summary = "Get purchase in page")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:query')")
    public CommonResult<PageResult<ErpPurchaseInRespVO>> getPurchaseInPage(@Valid ErpPurchaseInPageReqVO pageReqVO) {
        PageResult<ErpPurchaseInDO> pageResult = purchaseInService.getPurchaseInPage(pageReqVO);
        return success(buildPurchaseInVOPageResult(pageResult));
    }

    @GetMapping("/returnable-items")
    @Operation(summary = "Get returnable items by purchase in")
    @Parameter(name = "inId", description = "Purchase in ID", required = true, example = "17386")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public CommonResult<List<ErpPurchaseReturnableItemRespVO>> getReturnableItems(@RequestParam("inId") Long inId) {
        return success(purchaseInService.getReturnableItemsByInId(inId));
    }

    @GetMapping("/transfer-outable-items")
    @Operation(summary = "Get transfer-outable items by purchase in")
    @Parameter(name = "inId", description = "Purchase in ID", required = true, example = "17386")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:transfer-out')")
    public CommonResult<List<ErpPurchaseInTransferOutableItemRespVO>> getTransferOutableItems(
            @RequestParam("inId") Long inId) {
        return success(purchaseInService.getTransferOutableItemsByInId(inId));
    }

    @GetMapping("/sale-cartable-items")
    @Operation(summary = "Get sale-cartable items by purchase in")
    @Parameter(name = "inId", description = "Purchase in ID", required = true, example = "17386")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:to-sale-cart')")
    public CommonResult<List<ErpPurchaseInSaleCartableItemRespVO>> getSaleCartableItems(
            @RequestParam("inId") Long inId) {
        return success(purchaseInService.getSaleCartableItemsByInId(inId));
    }

    @PostMapping("/create-transfer-out")
    @Operation(summary = "Create stock transfer-out from purchase in")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:transfer-out')")
    public CommonResult<ErpPurchaseInCreateTransferOutRespVO> createTransferOutFromPurchaseIn(
            @Valid @RequestBody ErpPurchaseInCreateTransferOutReqVO reqVO) {
        return success(purchaseInService.createTransferOutFromPurchaseIn(reqVO));
    }

    @PostMapping("/create-sale-cart")
    @Operation(summary = "Create sale cart from purchase in")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:to-sale-cart')")
    public CommonResult<ErpPurchaseInCreateSaleCartRespVO> createSaleCartFromPurchaseIn(
            @Valid @RequestBody ErpPurchaseInCreateSaleCartReqVO reqVO) {
        return success(purchaseInService.createSaleCartFromPurchaseIn(reqVO));
    }

    @PostMapping("/create-from-order")
    @Operation(summary = "Create purchase in from order")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:create')")
    public CommonResult<Long> createPurchaseInFromOrder(@Valid @RequestBody ErpPurchaseInFromOrderReqVO reqVO) {
        return success(purchaseInService.createPurchaseInFromOrder(reqVO));
    }

    @GetMapping("/list-approved-for-adjust")
    @Operation(summary = "Get approved purchase ins by supplier")
    @Parameter(name = "supplierId", description = "Supplier ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:query')")
    public CommonResult<List<ErpPurchaseInForAdjustRespVO>> getApprovedPurchaseInsBySupplier(
            @RequestParam("supplierId") Long supplierId) {
        return success(purchaseInService.getApprovedPurchaseInsBySupplier(supplierId));
    }

    @GetMapping("/list-items-for-adjust")
    @Operation(summary = "Get approved purchase in items by supplier")
    @Parameter(name = "supplierId", description = "Supplier ID", required = true, example = "1024")
    @Parameter(name = "excludeAdjusted", description = "Whether to exclude adjusted items", example = "true")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:query')")
    public CommonResult<List<ErpPurchaseInItemForAdjustRespVO>> getApprovedPurchaseInItemsBySupplier(
            @RequestParam("supplierId") Long supplierId,
            @RequestParam(value = "excludeAdjusted", required = false) Boolean excludeAdjusted) {
        return success(purchaseInService.getApprovedPurchaseInItemsBySupplier(supplierId, excludeAdjusted));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export purchase in Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseInExcel(@Valid ErpPurchaseInPageReqVO pageReqVO,
                                      @RequestParam(value = "fields", required = false) String fields,
                                      HttpServletResponse response) throws IOException {
        PageResult<ErpPurchaseInDO> pageResult;
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            pageResult = new PageResult<>(purchaseInService.getPurchaseInList(pageReqVO.getIds()),
                    (long) pageReqVO.getIds().size());
        } else {
            pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
            pageResult = purchaseInService.getPurchaseInPage(pageReqVO);
        }
        List<ErpPurchaseInItemDO> purchaseInItemList = purchaseInService.getPurchaseInItemListByInIds(
                convertSet(pageResult.getList(), ErpPurchaseInDO::getId));
        markHasInvoice(pageResult.getList());
        Map<Long, List<ErpPurchaseInItemDO>> purchaseInItemMap = convertMultiMap(
                purchaseInItemList, ErpPurchaseInItemDO::getInId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(purchaseInItemList, ErpPurchaseInItemDO::getProductId)));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseInDO::getSupplierId));
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(
                convertSet(pageResult.getList(), ErpPurchaseInDO::getAccountId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(pageResult.getList(), ErpPurchaseInDO::getDeptId));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(purchaseIn -> collectUserIds(userIds, purchaseIn));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<Long, ErpWarehouseDO> warehouseMap = cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                warehouseService.getWarehouseListByStatus(0), ErpWarehouseDO::getId);
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpPurchaseInExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "采购入库.xls", "数据", ErpPurchaseInExportRespVO.class,
                buildPurchaseInExportList(pageResult.getList(), purchaseInItemMap, productMap, supplierMap,
                        accountMap, deptMap, userMap, warehouseMap), includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get purchase in export fields")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getPurchaseInExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpPurchaseInExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    private PageResult<ErpPurchaseInRespVO> buildPurchaseInVOPageResult(PageResult<ErpPurchaseInDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        markHasInvoice(pageResult.getList());
        List<ErpPurchaseInItemDO> purchaseInItemList = purchaseInService.getPurchaseInItemListByInIds(
                convertSet(pageResult.getList(), ErpPurchaseInDO::getId));
        Map<Long, List<ErpPurchaseInItemDO>> purchaseInItemMap = convertMultiMap(
                purchaseInItemList, ErpPurchaseInItemDO::getInId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(purchaseInItemList, ErpPurchaseInItemDO::getProductId)));
        Map<Long, BigDecimal> returnCountMap = getApprovedReturnCountMap(purchaseInItemList);
        Map<Long, BigDecimal> transferOutCountMap = getTransferOutCountMap(purchaseInItemList);
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseInDO::getSupplierId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpPurchaseInDO::getDeptId));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(purchaseIn -> collectUserIds(userIds, purchaseIn));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        PageResult<ErpPurchaseInRespVO> respResult = BeanUtils.toBean(pageResult, ErpPurchaseInRespVO.class, purchaseIn -> {
            List<ErpPurchaseInItemDO> items = purchaseInItemMap.getOrDefault(purchaseIn.getId(), Collections.emptyList());
            purchaseIn.setItems(BeanUtils.toBean(items, ErpPurchaseInRespVO.Item.class,
                    item -> {
                        MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                                .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                                .setProductCode(product.getCode()).setBatchNoEnabled(product.getBatchNoEnabled()));
                        fillPurchaseInItemReturnInfo(item, returnCountMap);
                    }));
            purchaseIn.setItemCount(items.size());
            purchaseIn.setProductNames(CollUtil.join(purchaseIn.getItems(), ", ",
                    ErpPurchaseInRespVO.Item::getProductName));
            fillPurchaseInReturnInfo(purchaseIn);
            fillPurchaseInTransferOutInfo(purchaseIn, transferOutCountMap);
            MapUtils.findAndThen(supplierMap, purchaseIn.getSupplierId(),
                    supplier -> purchaseIn.setSupplierName(supplier.getName()));
            MapUtils.findAndThen(deptMap, purchaseIn.getDeptId(),
                    dept -> purchaseIn.setDeptName(dept.getName()));
            fillUserNames(purchaseIn, userMap);
        });
        fieldPermissionMasker.maskList(FIELD_PERMISSION_MODULE, respResult.getList());
        return respResult;
    }

    private void markHasInvoice(List<ErpPurchaseInDO> purchaseIns) {
        if (CollUtil.isEmpty(purchaseIns)) {
            return;
        }
        Set<Long> sourceInIds = convertSet(
                purchaseInvoiceService.getPurchaseInvoiceItemListBySourceInIds(
                        convertSet(purchaseIns, ErpPurchaseInDO::getId)),
                ErpPurchaseInvoiceItemDO::getSourceInId);
        sourceInIds.remove(null);
        if (CollUtil.isEmpty(sourceInIds)) {
            return;
        }
        purchaseIns.forEach(purchaseIn -> {
            if (sourceInIds.contains(purchaseIn.getId())) {
                purchaseIn.setHasInvoice(true);
            }
        });
    }

    private void markHasInvoice(ErpPurchaseInDO purchaseIn) {
        if (purchaseIn == null) {
            return;
        }
        markHasInvoice(Collections.singletonList(purchaseIn));
    }

    private Map<Long, BigDecimal> getApprovedReturnCountMap(List<ErpPurchaseInItemDO> purchaseInItemList) {
        if (CollUtil.isEmpty(purchaseInItemList)) {
            return Collections.emptyMap();
        }
        Set<Long> inItemIds = convertSet(purchaseInItemList, ErpPurchaseInItemDO::getId);
        inItemIds.remove(null);
        return purchaseInService.getApprovedReturnCountMapByInItemIds(inItemIds);
    }

    private Map<Long, BigDecimal> getTransferOutCountMap(List<ErpPurchaseInItemDO> purchaseInItemList) {
        if (CollUtil.isEmpty(purchaseInItemList)) {
            return Collections.emptyMap();
        }
        Set<Long> inItemIds = convertSet(purchaseInItemList, ErpPurchaseInItemDO::getId);
        inItemIds.remove(null);
        return purchaseInService.getTransferOutCountMapByInItemIds(inItemIds);
    }

    private void fillPurchaseInItemReturnInfo(ErpPurchaseInRespVO.Item item, Map<Long, BigDecimal> returnCountMap) {
        BigDecimal returnCount = returnCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
        item.setReturnCount(returnCount);
        item.setReturnStatus(calculateReturnStatus(item.getCount(), returnCount));
    }

    private void fillPurchaseInReturnInfo(ErpPurchaseInRespVO purchaseIn) {
        BigDecimal returnCount = BigDecimal.ZERO;
        BigDecimal totalCount = purchaseIn.getTotalCount();
        if (CollUtil.isNotEmpty(purchaseIn.getItems())) {
            for (ErpPurchaseInRespVO.Item item : purchaseIn.getItems()) {
                returnCount = returnCount.add(item.getReturnCount() != null ? item.getReturnCount() : BigDecimal.ZERO);
            }
            if (totalCount == null) {
                totalCount = BigDecimal.ZERO;
                for (ErpPurchaseInRespVO.Item item : purchaseIn.getItems()) {
                    totalCount = totalCount.add(item.getCount() != null ? item.getCount() : BigDecimal.ZERO);
                }
            }
        }
        purchaseIn.setReturnCount(returnCount);
        purchaseIn.setReturnStatus(calculateReturnStatus(totalCount, returnCount));
    }

    private void fillPurchaseInTransferOutInfo(ErpPurchaseInRespVO purchaseIn,
                                               Map<Long, BigDecimal> transferOutCountMap) {
        BigDecimal transferOutCount = BigDecimal.ZERO;
        BigDecimal totalCount = purchaseIn.getTotalCount();
        if (CollUtil.isNotEmpty(purchaseIn.getItems())) {
            for (ErpPurchaseInRespVO.Item item : purchaseIn.getItems()) {
                transferOutCount = transferOutCount.add(
                        transferOutCountMap.getOrDefault(item.getId(), BigDecimal.ZERO));
            }
            if (totalCount == null) {
                totalCount = purchaseIn.getItems().stream()
                        .map(ErpPurchaseInRespVO.Item::getCount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }
        purchaseIn.setTransferOutCount(transferOutCount);
        purchaseIn.setTransferOutStatus(calculateReturnStatus(totalCount, transferOutCount));
    }

    private void fillPurchaseInStockInBillInfo(ErpPurchaseInRespVO respVO, Long purchaseInId) {
        List<ErpStockInBillDO> bills = stockInBillService.getStockInBillListByPurchaseInId(purchaseInId);
        respVO.setHasStockInBill(CollUtil.isNotEmpty(bills));
        if (CollUtil.isEmpty(bills)) {
            respVO.setStockInBills(Collections.emptyList());
            if (CollUtil.isNotEmpty(respVO.getItems())) {
                respVO.getItems().forEach(item -> item.setHasStockInBill(false));
            }
            return;
        }
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(bills, ErpStockInBillDO::getWarehouseId));
        Map<Long, ErpStockInBillDO> billMap = bills.stream()
                .collect(Collectors.toMap(ErpStockInBillDO::getId, bill -> bill, (first, second) -> first));
        respVO.setStockInBills(bills.stream().map(bill -> {
            ErpPurchaseInRespVO.StockInBillBrief brief = BeanUtils.toBean(bill, ErpPurchaseInRespVO.StockInBillBrief.class);
            brief.setStatusName(getStockInBillStatusName(bill.getStatus()));
            MapUtils.findAndThen(warehouseMap, bill.getWarehouseId(), warehouse -> brief.setWarehouseName(warehouse.getName()));
            return brief;
        }).collect(Collectors.toList()));

        List<ErpStockInBillItemDO> billItems = stockInBillService.getPurchaseInSourceItemList(purchaseInId);
        Map<Long, List<ErpStockInBillItemDO>> billItemMap = billItems.stream()
                .filter(item -> item.getSourceItemId() != null)
                .collect(Collectors.groupingBy(ErpStockInBillItemDO::getSourceItemId));
        if (CollUtil.isEmpty(respVO.getItems())) {
            return;
        }
        respVO.getItems().forEach(item -> fillPurchaseInItemStockInBillInfo(item, billItemMap.get(item.getId()), billMap));
    }

    private void fillPurchaseInItemStockInBillInfo(ErpPurchaseInRespVO.Item item,
                                                   List<ErpStockInBillItemDO> billItems,
                                                   Map<Long, ErpStockInBillDO> billMap) {
        item.setHasStockInBill(CollUtil.isNotEmpty(billItems));
        if (CollUtil.isEmpty(billItems)) {
            return;
        }
        item.setStockInBillNos(billItems.stream()
                .map(billItem -> billMap.get(billItem.getBillId()))
                .filter(Objects::nonNull)
                .map(ErpStockInBillDO::getNo)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(", ")));
        BigDecimal count = sumStockBillCount(billItems, ErpStockInBillItemDO::getCount);
        BigDecimal pickedCount = sumStockBillCount(billItems, ErpStockInBillItemDO::getPickedCount);
        Integer status = calculateStockBillStatus(billItems.stream()
                .map(ErpStockInBillItemDO::getStatus)
                .collect(Collectors.toList()));
        item.setStockInBillStatus(status);
        item.setStockInBillStatusName(getStockInBillStatusName(status));
        item.setStockInBillCount(count);
        item.setStockInBillPickedCount(pickedCount);
        item.setStockInBillRemainCount(count.subtract(pickedCount));
    }

    private String getStockInBillStatusName(Integer status) {
        if (Integer.valueOf(30).equals(status)) {
            return "已完成";
        }
        if (Integer.valueOf(20).equals(status)) {
            return "部分提货";
        }
        if (Integer.valueOf(10).equals(status)) {
            return "待提货";
        }
        return null;
    }

    private Integer calculateStockBillStatus(List<Integer> statuses) {
        if (CollUtil.isEmpty(statuses)) {
            return null;
        }
        if (statuses.stream().allMatch(status -> Integer.valueOf(30).equals(status))) {
            return 30;
        }
        if (statuses.stream().allMatch(status -> Integer.valueOf(10).equals(status))) {
            return 10;
        }
        return 20;
    }

    private static <T> BigDecimal sumStockBillCount(List<T> list, java.util.function.Function<T, BigDecimal> getter) {
        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer calculateReturnStatus(BigDecimal totalCount, BigDecimal returnCount) {
        BigDecimal safeReturnCount = returnCount != null ? returnCount : BigDecimal.ZERO;
        if (safeReturnCount.compareTo(BigDecimal.ZERO) <= 0) {
            return RETURN_STATUS_NONE;
        }
        if (totalCount != null && totalCount.compareTo(BigDecimal.ZERO) > 0
                && safeReturnCount.compareTo(totalCount) >= 0) {
            return RETURN_STATUS_ALL;
        }
        return RETURN_STATUS_PART;
    }

    private void fillUserNames(ErpPurchaseInRespVO purchaseIn, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(purchaseIn.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> purchaseIn.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(purchaseIn.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> purchaseIn.setUpdaterName(user.getNickname()));
        }
        Long purchaserId = parseUserId(purchaseIn.getPurchaser());
        if (purchaserId != null) {
            MapUtils.findAndThen(userMap, purchaserId, user -> purchaseIn.setPurchaserName(user.getNickname()));
        }
        Long accountantId = parseUserId(purchaseIn.getAccountant());
        if (accountantId != null) {
            MapUtils.findAndThen(userMap, accountantId, user -> purchaseIn.setAccountantName(user.getNickname()));
        }
        Long handlerId = parseUserId(purchaseIn.getHandler());
        if (handlerId != null) {
            MapUtils.findAndThen(userMap, handlerId, user -> purchaseIn.setHandlerName(user.getNickname()));
        }
    }

    private void collectUserIds(Set<Long> userIds, ErpPurchaseInDO purchaseIn) {
        addUserId(userIds, purchaseIn.getCreator());
        addUserId(userIds, purchaseIn.getUpdater());
        addUserId(userIds, purchaseIn.getPurchaser());
        addUserId(userIds, purchaseIn.getAccountant());
        addUserId(userIds, purchaseIn.getHandler());
    }

    private void addUserId(Set<Long> userIds, String userId) {
        Long parsed = parseUserId(userId);
        if (parsed != null) {
            userIds.add(parsed);
        }
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<ErpPurchaseInExportRespVO> buildPurchaseInExportList(List<ErpPurchaseInDO> list,
                                                                      Map<Long, List<ErpPurchaseInItemDO>> purchaseInItemMap,
                                                                      Map<Long, ErpProductRespVO> productMap,
                                                                      Map<Long, ErpSupplierDO> supplierMap,
                                                                      Map<Long, ErpAccountDO> accountMap,
                                                                      Map<Long, DeptRespDTO> deptMap,
                                                                      Map<Long, AdminUserRespDTO> userMap,
                                                                      Map<Long, ErpWarehouseDO> warehouseMap) {
        List<ErpPurchaseInExportRespVO> rows = new ArrayList<>();
        for (ErpPurchaseInDO purchaseIn : list) {
            List<ErpPurchaseInItemDO> items = purchaseInItemMap.getOrDefault(purchaseIn.getId(), Collections.emptyList());
            Long creatorId = parseUserId(purchaseIn.getCreator());
            if (CollUtil.isEmpty(items)) {
                rows.add(buildPurchaseInExportRow(purchaseIn,
                        supplierMap.get(purchaseIn.getSupplierId()),
                        accountMap.get(purchaseIn.getAccountId()),
                        deptMap.get(purchaseIn.getDeptId()),
                        creatorId == null ? null : userMap.get(creatorId),
                        userMap, null, null, true, warehouseMap));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                ErpPurchaseInItemDO item = items.get(i);
                rows.add(buildPurchaseInExportRow(purchaseIn,
                        supplierMap.get(purchaseIn.getSupplierId()),
                        accountMap.get(purchaseIn.getAccountId()),
                        deptMap.get(purchaseIn.getDeptId()),
                        creatorId == null ? null : userMap.get(creatorId),
                        userMap, item, productMap.get(item.getProductId()), i == 0, warehouseMap));
            }
        }
        return rows;
    }

    private ErpPurchaseInExportRespVO buildPurchaseInExportRow(ErpPurchaseInDO purchaseIn,
                                                               ErpSupplierDO supplier,
                                                               ErpAccountDO account,
                                                               DeptRespDTO dept,
                                                               AdminUserRespDTO creator,
                                                               Map<Long, AdminUserRespDTO> userMap,
                                                               ErpPurchaseInItemDO item,
                                                               ErpProductRespVO product,
                                                               boolean fillMainFields,
                                                               Map<Long, ErpWarehouseDO> warehouseMap) {
        ErpPurchaseInExportRespVO row = fillMainFields
                ? BeanUtils.toBean(purchaseIn, ErpPurchaseInExportRespVO.class)
                : new ErpPurchaseInExportRespVO();
        row.setSupplierName(fillMainFields && supplier != null ? supplier.getName() : null);
        row.setAccountName(fillMainFields && account != null ? account.getName() : null);
        row.setDeptName(fillMainFields && dept != null ? dept.getName() : null);
        row.setCreatorName(fillMainFields && creator != null ? creator.getNickname() : null);
        if (fillMainFields) {
            Long purchaserId = parseUserId(purchaseIn.getPurchaser());
            if (purchaserId != null) {
                MapUtils.findAndThen(userMap, purchaserId, user -> row.setPurchaserName(user.getNickname()));
            }
            Long accountantId = parseUserId(purchaseIn.getAccountant());
            if (accountantId != null) {
                MapUtils.findAndThen(userMap, accountantId, user -> row.setAccountantName(user.getNickname()));
            }
            Long handlerId = parseUserId(purchaseIn.getHandler());
            if (handlerId != null) {
                MapUtils.findAndThen(userMap, handlerId, user -> row.setHandlerName(user.getNickname()));
            }
        }
        if (fillMainFields) {
            row.setFactoryOrderNo(purchaseIn.getFactoryOrderNo());
        }
        if (item == null) {
            return row;
        }
        row.setProductCode(product != null ? product.getCode() : null);
        row.setProductName(product != null ? product.getName() : null);
        row.setProductUnitName(product != null ? product.getUnitName() : null);
        row.setPackageQty(item.getPackageQty());
        row.setWholeQty(item.getWholeQty());
        row.setItemCount(item.getCount());
        row.setProductPrice(item.getProductPrice());
        row.setItemTotalPrice(item.getProductPrice() == null || item.getCount() == null
                ? null : item.getProductPrice().multiply(item.getCount()));
        row.setWarehousePosition(item.getWarehousePosition());
        row.setBatchNo(item.getBatchNo());
        row.setDrawingNo(item.getDrawingNo());
        row.setBrand(item.getBrand());
        row.setItemRemark(item.getRemark());
        MapUtils.findAndThen(warehouseMap, item.getWarehouseId(),
                warehouse -> row.setWarehouseName(warehouse.getName()));
        return row;
    }

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("no", "main");
        map.put("supplierName", "main");
        map.put("inTime", "main");
        map.put("orderNo", "main");
        map.put("accountName", "main");
        map.put("factoryOrderNo", "main");
        map.put("status", "main");
        map.put("creatorName", "system");
        map.put("purchaserName", "main");
        map.put("accountantName", "main");
        map.put("handlerName", "main");
        map.put("deptName", "main");
        map.put("totalCount", "main");
        map.put("totalPrice", "main");
        map.put("feeAmount", "main");
        map.put("remark", "main");
        map.put("productCode", "detail");
        map.put("productName", "detail");
        map.put("productUnitName", "detail");
        map.put("packageQty", "detail");
        map.put("wholeQty", "detail");
        map.put("itemCount", "detail");
        map.put("productPrice", "detail");
        map.put("itemTotalPrice", "detail");
        map.put("warehouseName", "detail");
        map.put("warehousePosition", "detail");
        map.put("batchNo", "detail");
        map.put("drawingNo", "detail");
        map.put("brand", "detail");
        map.put("itemRemark", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("supplierName", "supplierId");
        map.put("accountName", "accountId");
        map.put("deptName", "deptId");
        map.put("purchaserName", "purchaser");
        map.put("accountantName", "accountant");
        map.put("handlerName", "handler");
        map.put("productCode", "item_productId");
        map.put("productName", "item_productId");
        map.put("productUnitName", "item_productUnitId");
        map.put("warehouseName", "item_warehouseId");
        map.put("warehousePosition", "item_warehousePosition");
        map.put("batchNo", "item_batchNo");
        map.put("drawingNo", "item_drawingNo");
        map.put("brand", "item_brand");
        map.put("itemRemark", "item_remark");
        return map;
    }
}
