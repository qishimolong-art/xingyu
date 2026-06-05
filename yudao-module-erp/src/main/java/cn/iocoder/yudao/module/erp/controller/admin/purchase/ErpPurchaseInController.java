package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseInFromOrderReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInvoiceService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "Admin - ERP Purchase In")
@RestController
@RequestMapping("/erp/purchase-in")
@Validated
public class ErpPurchaseInController {

    @Resource
    private ErpPurchaseInService purchaseInService;
    @Resource
    private ErpPurchaseInvoiceService purchaseInvoiceService;
    @Resource
    private ErpStockService stockService;
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
        purchaseInService.updatePurchaseInStatus(id, status);
        return success(true);
    }

    @PostMapping("/import")
    @Operation(summary = "Import purchase in from Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:create')")
    public CommonResult<ErpPurchaseInImportRespVO> importPurchaseIn(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPurchaseInImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseInImportExcelVO.class);
        return success(purchaseInService.importPurchaseInItems(list));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "Get purchase in import template")
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
        ExcelUtils.write(response, "purchase-in-import-template.xls", "purchase-in",
                ErpPurchaseInImportExcelVO.class, Collections.singletonList(example));
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
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseInItemList, ErpPurchaseInItemDO::getProductId));
        Set<Long> userIds = new HashSet<>();
        collectUserIds(userIds, purchaseIn);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        DeptRespDTO dept = purchaseIn.getDeptId() == null ? null : deptApi.getDept(purchaseIn.getDeptId());
        ErpPurchaseInRespVO respVO = BeanUtils.toBean(purchaseIn, ErpPurchaseInRespVO.class, purchaseInVO -> {
            purchaseInVO.setItems(BeanUtils.toBean(purchaseInItemList, ErpPurchaseInRespVO.Item.class, item -> {
                ErpStockDO stock = stockService.getStock(item.getProductId(), item.getWarehouseId());
                item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                        .setProductCode(product.getCode()));
            }));
            fillUserNames(purchaseInVO, userMap);
            if (dept != null) {
                purchaseInVO.setDeptName(dept.getName());
            }
        });
        fieldPermissionMasker.mask("erp_purchase_in", respVO);
        return success(respVO);
    }

    @GetMapping("/list-items")
    @Operation(summary = "Get purchase in items")
    @Parameter(name = "inId", description = "Purchase in ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:query')")
    public CommonResult<List<ErpPurchaseInRespVO.Item>> getPurchaseInItems(@RequestParam("inId") Long inId) {
        List<ErpPurchaseInItemDO> purchaseInItemList = purchaseInService.getPurchaseInItemListByInId(inId);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseInItemList, ErpPurchaseInItemDO::getProductId));
        List<ErpPurchaseInRespVO.Item> items = BeanUtils.toBean(purchaseInItemList, ErpPurchaseInRespVO.Item.class);
        if (items == null) {
            return success(Collections.emptyList());
        }
        items.forEach(item -> MapUtils.findAndThen(productMap, item.getProductId(),
                product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode())
                        .setProductUnitName(product.getUnitName())
                        .setProductCode(product.getCode())));
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
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseInItemList, ErpPurchaseInItemDO::getProductId));
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
        ExcelUtils.write(response, "purchase-in.xls", "data", ErpPurchaseInExportRespVO.class,
                buildPurchaseInExportList(pageResult.getList(), purchaseInItemMap, productMap, supplierMap,
                        accountMap, deptMap, userMap, warehouseMap));
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
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseInItemList, ErpPurchaseInItemDO::getProductId));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseInDO::getSupplierId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpPurchaseInDO::getDeptId));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(purchaseIn -> collectUserIds(userIds, purchaseIn));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        return BeanUtils.toBean(pageResult, ErpPurchaseInRespVO.class, purchaseIn -> {
            purchaseIn.setItems(BeanUtils.toBean(purchaseInItemMap.get(purchaseIn.getId()), ErpPurchaseInRespVO.Item.class,
                    item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                            .setProductCode(product.getCode()))));
            purchaseIn.setProductNames(CollUtil.join(purchaseIn.getItems(), ", ",
                    ErpPurchaseInRespVO.Item::getProductName));
            MapUtils.findAndThen(supplierMap, purchaseIn.getSupplierId(),
                    supplier -> purchaseIn.setSupplierName(supplier.getName()));
            MapUtils.findAndThen(deptMap, purchaseIn.getDeptId(),
                    dept -> purchaseIn.setDeptName(dept.getName()));
            fillUserNames(purchaseIn, userMap);
        });
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
}
