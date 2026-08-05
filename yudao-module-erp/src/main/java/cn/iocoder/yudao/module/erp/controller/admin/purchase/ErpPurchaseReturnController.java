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
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.ErpPurchaseUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 采购退货")
@RestController
@RequestMapping("/erp/purchase-return")
@Validated
public class ErpPurchaseReturnController {

    private static final String FIELD_PERMISSION_MODULE = "erp_purchase_return";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();
    private static final Map<String, String> DETAIL_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "productId", "productCode",
            "warehouseId", "warehouseName",
            "count", "count",
            "item_count", "count",
            "itemCount", "count",
            "productPrice", "productPrice",
            "item_remark", "remark",
            "itemRemark", "remark");
    private static final Map<String, String> ORDER_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "supplierId", "supplierName",
            "supplierName", "supplierName",
            "returnTime", "returnTime",
            "remark", "remark",
            "productId", "productCode",
            "warehouseId", "warehouseName",
            "count", "itemCount",
            "item_count", "itemCount",
            "itemCount", "itemCount",
            "productPrice", "productPrice",
            "item_remark", "itemRemark",
            "itemRemark", "itemRemark");

    @Resource
    private ErpPurchaseReturnService purchaseReturnService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;

    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpFieldConfigService fieldConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建采购退货")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public CommonResult<Long> createPurchaseReturn(@Valid @RequestBody ErpPurchaseReturnSaveReqVO createReqVO) {
        return success(purchaseReturnService.createPurchaseReturn(createReqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建采购退货草稿")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public CommonResult<Long> createPurchaseReturnDraft(
            @RequestBody ErpPurchaseReturnDraftCreateReqVO createReqVO) {
        return success(purchaseReturnService.createPurchaseReturnDraft(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购退货")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update')")
    public CommonResult<Boolean> updatePurchaseReturn(@Valid @RequestBody ErpPurchaseReturnSaveReqVO updateReqVO) {
        purchaseReturnService.updatePurchaseReturn(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "保存采购退货草稿")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update')")
    public CommonResult<Boolean> updatePurchaseReturnDraft(
            @RequestBody ErpPurchaseReturnDraftUpdateReqVO updateReqVO) {
        purchaseReturnService.updatePurchaseReturnDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "更新并提交采购退货草稿")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update') and " +
            "@ss.hasPermission('erp:purchase-return:update-status')")
    public CommonResult<Boolean> updateAndSubmitPurchaseReturnDraft(
            @Valid @RequestBody ErpPurchaseReturnDraftUpdateReqVO updateReqVO) {
        purchaseReturnService.updateAndSubmitPurchaseReturnDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交采购退货草稿")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update-status')")
    public CommonResult<Boolean> submitPurchaseReturn(@RequestParam("id") Long id) {
        purchaseReturnService.submitPurchaseReturn(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "修改采购退货备注")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update')")
    public CommonResult<Boolean> updatePurchaseReturnRemark(
            @Valid @RequestBody ErpPurchaseUpdateRemarkReqVO updateReqVO) {
        purchaseReturnService.updatePurchaseReturnRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新采购退货的状态")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update-status')")
    public CommonResult<Boolean> updatePurchaseReturnStatus(@RequestParam("id") Long id,
                                                            @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        purchaseReturnService.updatePurchaseReturnStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购退货")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:delete')")
    public CommonResult<Boolean> deletePurchaseReturn(@RequestParam("ids") List<Long> ids) {
        purchaseReturnService.deletePurchaseReturn(ids);
        return success(true);
    }

    @PostMapping("/import")
    @Operation(summary = "解析采购退货导入 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public CommonResult<ErpPurchaseReturnImportRespVO> importPurchaseReturn(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPurchaseReturnImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseReturnImportExcelVO.class);
        return success(purchaseReturnService.importPurchaseReturnItems(list));
    }

    @PostMapping("/import-order")
    @Operation(summary = "Import purchase return order")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public CommonResult<ErpPurchaseImportResultRespVO> importPurchaseReturnOrder(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPurchaseReturnOrderImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseReturnOrderImportExcelVO.class);
        return success(purchaseReturnService.importPurchaseReturnOrderList(list));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得采购退货明细导入模板")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchaseReturnImportExcelVO example = new ErpPurchaseReturnImportExcelVO();
        example.setProductCode("P000001");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("10.00"));
        example.setWarehouseName("主仓");
        example.setRemark("示例");
        ExcelUtils.writeImportTemplate(response, "采购退货明细导入模板.xls", "采购退货明细", ErpPurchaseReturnImportExcelVO.class,
                java.util.Collections.singletonList(example), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.PURCHASE_RETURN, ErpPurchaseReturnImportExcelVO.class,
                        DETAIL_IMPORT_FIELD_ALIAS_MAP));
    }

    @GetMapping("/get-order-import-template")
    @Operation(summary = "Get purchase return order import template")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public void getOrderImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchaseReturnOrderImportExcelVO example = new ErpPurchaseReturnOrderImportExcelVO();
        example.setNo("TH-IMPORT-001");
        example.setSupplierName("Example Supplier");
        example.setReturnTime("2026-06-05 09:00:00");
        example.setRemark("Order remark");
        example.setProductCode("P000001");
        example.setWarehouseName("Main Warehouse");
        example.setItemCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("10.00"));
        example.setItemRemark("Item remark");

        ErpPurchaseReturnOrderImportExcelVO secondItem = new ErpPurchaseReturnOrderImportExcelVO();
        secondItem.setProductCode("P000002");
        secondItem.setWarehouseName("Main Warehouse");
        secondItem.setItemCount(new BigDecimal("2"));
        secondItem.setProductPrice(new BigDecimal("20.00"));

        ExcelUtils.writeImportTemplate(response, "采购退货导入模板.xls", "采购退货",
                ErpPurchaseReturnOrderImportExcelVO.class, java.util.Arrays.asList(example, secondItem), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.PURCHASE_RETURN, ErpPurchaseReturnOrderImportExcelVO.class,
                        ORDER_IMPORT_FIELD_ALIAS_MAP));
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购退货")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<ErpPurchaseReturnRespVO> getPurchaseReturn(@RequestParam("id") Long id) {
        ErpPurchaseReturnDO purchaseReturn = purchaseReturnService.getPurchaseReturn(id);
        if (purchaseReturn == null) {
            return success(null);
        }
        List<ErpPurchaseReturnItemDO> purchaseReturnItemList = purchaseReturnService.getPurchaseReturnItemListByReturnId(id);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(purchaseReturnItemList, ErpPurchaseReturnItemDO::getProductId)));
        Map<Long, ErpPurchaseInItemDO> inItemMap = java.util.Collections.emptyMap();
        Map<Long, BigDecimal> returnedMap = java.util.Collections.emptyMap();
        java.util.Set<Long> sourceInItemIds = convertSet(purchaseReturnItemList, ErpPurchaseReturnItemDO::getSourceInItemId);
        sourceInItemIds.remove(null);
        if (CollUtil.isNotEmpty(sourceInItemIds)) {
            List<ErpPurchaseInItemDO> inItems = purchaseInItemMapper.selectBatchIds(sourceInItemIds);
            inItemMap = cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(inItems, ErpPurchaseInItemDO::getId);
            returnedMap = purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIdsExcludeReturn(sourceInItemIds, id);
        }
        Map<Long, ErpPurchaseInItemDO> finalInItemMap = inItemMap;
        Map<Long, BigDecimal> finalReturnedMap = returnedMap;
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(collectUserIds(java.util.Collections.singletonList(purchaseReturn)));
        DeptRespDTO dept = purchaseReturn.getDeptId() == null ? null : deptApi.getDept(purchaseReturn.getDeptId());
        ErpSupplierDO supplier = purchaseReturn.getSupplierId() == null
                ? null : supplierService.getSupplier(purchaseReturn.getSupplierId());
        ErpPurchaseReturnRespVO respVO = BeanUtils.toBean(purchaseReturn, ErpPurchaseReturnRespVO.class, purchaseReturnVO -> {
                purchaseReturnVO.setItems(BeanUtils.toBean(purchaseReturnItemList, ErpPurchaseReturnRespVO.Item.class, item -> {
                    ErpStockDO stock = stockService.getStock(item.getProductId(), item.getWarehouseId());
                    item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                    MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductCode(product.getCode()).setProductBarCode(product.getBarCode())
                            .setProductUnitName(product.getUnitName()).setBatchNoEnabled(product.getBatchNoEnabled()));
                    if (item.getSourceInItemId() != null) {
                        ErpPurchaseInItemDO inItem = finalInItemMap.get(item.getSourceInItemId());
                        if (inItem != null) {
                            item.setInCount(inItem.getCount());
                            BigDecimal otherReturned = finalReturnedMap.getOrDefault(item.getSourceInItemId(), BigDecimal.ZERO);
                            item.setReturnableCount(inItem.getCount().subtract(otherReturned));
                        }
                    }
                }));
                purchaseReturnVO.setItemCount(purchaseReturnItemList.size());
                if (dept != null) {
                    purchaseReturnVO.setDeptName(dept.getName());
                }
                if (supplier != null) {
                    purchaseReturnVO.setSupplierName(supplier.getName());
                }
                fillUserNames(purchaseReturnVO, userMap);
        });
        fieldPermissionMasker.mask("erp_purchase_return", respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购退货分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:query')")
    public CommonResult<PageResult<ErpPurchaseReturnRespVO>> getPurchaseReturnPage(@Valid ErpPurchaseReturnPageReqVO pageReqVO) {
        PageResult<ErpPurchaseReturnDO> pageResult = purchaseReturnService.getPurchaseReturnPage(pageReqVO);
        return success(buildPurchaseReturnVOPageResult(pageResult));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购退货 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseReturnExcel(@Valid ErpPurchaseReturnPageReqVO pageReqVO,
                                          @RequestParam(value = "fields", required = false) String fields,
                                          HttpServletResponse response) throws IOException {
        List<ErpPurchaseReturnRespVO> list;
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            list = buildPurchaseReturnVOPageResult(new PageResult<>(
                    purchaseReturnService.getPurchaseReturnList(pageReqVO.getIds()),
                    (long) pageReqVO.getIds().size())).getList();
        } else {
            pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
            list = buildPurchaseReturnVOPageResult(purchaseReturnService.getPurchaseReturnPage(pageReqVO)).getList();
        }
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpPurchaseReturnExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "采购退货.xls", "数据", ErpPurchaseReturnExportRespVO.class,
                buildPurchaseReturnExportList(list), includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "获得采购退货导出字段")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getPurchaseReturnExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpPurchaseReturnExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    private PageResult<ErpPurchaseReturnRespVO> buildPurchaseReturnVOPageResult(PageResult<ErpPurchaseReturnDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpPurchaseReturnItemDO> purchaseReturnItemList = purchaseReturnService.getPurchaseReturnItemListByReturnIds(
                convertSet(pageResult.getList(), ErpPurchaseReturnDO::getId));
        Map<Long, List<ErpPurchaseReturnItemDO>> purchaseReturnItemMap = convertMultiMap(purchaseReturnItemList, ErpPurchaseReturnItemDO::getReturnId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(purchaseReturnItemList, ErpPurchaseReturnItemDO::getProductId)));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseReturnDO::getSupplierId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(pageResult.getList(), ErpPurchaseReturnDO::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(collectUserIds(pageResult.getList()));
        PageResult<ErpPurchaseReturnRespVO> respResult = BeanUtils.toBean(pageResult, ErpPurchaseReturnRespVO.class, purchaseReturn -> {
            List<ErpPurchaseReturnItemDO> itemList = purchaseReturnItemMap.getOrDefault(purchaseReturn.getId(), java.util.Collections.emptyList());
            purchaseReturn.setItems(BeanUtils.toBean(itemList, ErpPurchaseReturnRespVO.Item.class,
                    item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductCode(product.getCode()).setProductBarCode(product.getBarCode())
                            .setProductUnitName(product.getUnitName()).setBatchNoEnabled(product.getBatchNoEnabled()))));
            purchaseReturn.setItemCount(itemList.size());
            purchaseReturn.setProductNames(CollUtil.join(purchaseReturn.getItems(), "，", ErpPurchaseReturnRespVO.Item::getProductName));
            MapUtils.findAndThen(supplierMap, purchaseReturn.getSupplierId(), supplier -> purchaseReturn.setSupplierName(supplier.getName()));
            MapUtils.findAndThen(deptMap, purchaseReturn.getDeptId(), dept -> purchaseReturn.setDeptName(dept.getName()));
            fillUserNames(purchaseReturn, userMap);
        });
        fieldPermissionMasker.maskList(FIELD_PERMISSION_MODULE, respResult.getList());
        return respResult;
    }

    private List<ErpPurchaseReturnExportRespVO> buildPurchaseReturnExportList(List<ErpPurchaseReturnRespVO> list) {
        Map<Long, ErpWarehouseDO> warehouseMap = cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                warehouseService.getWarehouseListByStatus(0), ErpWarehouseDO::getId);
        List<ErpPurchaseReturnExportRespVO> rows = new ArrayList<>();
        for (ErpPurchaseReturnRespVO purchaseReturn : list) {
            if (CollUtil.isEmpty(purchaseReturn.getItems())) {
                rows.add(buildPurchaseReturnExportRow(purchaseReturn, null, null, true));
                continue;
            }
            for (int i = 0; i < purchaseReturn.getItems().size(); i++) {
                ErpPurchaseReturnRespVO.Item item = purchaseReturn.getItems().get(i);
                rows.add(buildPurchaseReturnExportRow(purchaseReturn,
                        item,
                        warehouseMap.get(item.getWarehouseId()),
                        i == 0));
            }
        }
        return rows;
    }

    private ErpPurchaseReturnExportRespVO buildPurchaseReturnExportRow(ErpPurchaseReturnRespVO purchaseReturn,
                                                                       ErpPurchaseReturnRespVO.Item item,
                                                                       ErpWarehouseDO warehouse,
                                                                       boolean fillOrderFields) {
        ErpPurchaseReturnExportRespVO row = fillOrderFields
                ? BeanUtils.toBean(purchaseReturn, ErpPurchaseReturnExportRespVO.class)
                : new ErpPurchaseReturnExportRespVO();
        if (fillOrderFields) {
            row.setSupplierName(purchaseReturn.getSupplierName());
            row.setReturnTime(purchaseReturn.getReturnTime());
            row.setStatus(purchaseReturn.getStatus());
            row.setCreatorName(purchaseReturn.getCreatorName());
            row.setUpdaterName(purchaseReturn.getUpdaterName());
            row.setUpdateTime(purchaseReturn.getUpdateTime());
            row.setRemark(purchaseReturn.getRemark());
        }
        if (item == null) {
            return row;
        }
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setProductUnitName(item.getProductUnitName());
        row.setItemCount(item.getCount());
        row.setProductPrice(item.getProductPrice());
        row.setItemTotalPrice(item.getProductPrice() == null || item.getCount() == null
                ? null : item.getProductPrice().multiply(item.getCount()));
        row.setSourceInNo(item.getSourceInNo());
        row.setWarehouseName(warehouse != null ? warehouse.getName() : null);
        row.setWarehousePosition(item.getWarehousePosition());
        row.setBatchNo(item.getBatchNo());
        row.setBrand(item.getBrand());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private Set<Long> collectUserIds(List<ErpPurchaseReturnDO> list) {
        Set<Long> userIds = new LinkedHashSet<>();
        list.forEach(purchaseReturn -> {
            addUserId(userIds, purchaseReturn.getCreator());
            addUserId(userIds, purchaseReturn.getUpdater());
            addUserId(userIds, purchaseReturn.getPurchaser());
        });
        return userIds;
    }

    private void fillUserNames(ErpPurchaseReturnRespVO purchaseReturn, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(purchaseReturn.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> purchaseReturn.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(purchaseReturn.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> purchaseReturn.setUpdaterName(user.getNickname()));
        }
        Long purchaserId = parseUserId(purchaseReturn.getPurchaser());
        if (purchaserId != null) {
            MapUtils.findAndThen(userMap, purchaserId, user -> purchaseReturn.setPurchaserName(user.getNickname()));
        }
    }

    private void addUserId(Set<Long> userIds, String userId) {
        Long parsed = parseUserId(userId);
        if (parsed != null) {
            userIds.add(parsed);
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
        map.put("deptName", "main");
        map.put("returnTime", "main");
        map.put("status", "main");
        map.put("creatorName", "system");
        map.put("updaterName", "system");
        map.put("updateTime", "system");
        map.put("feeAmount", "main");
        map.put("remark", "main");
        map.put("productCode", "detail");
        map.put("productName", "detail");
        map.put("productUnitName", "detail");
        map.put("sourceInNo", "detail");
        map.put("itemCount", "detail");
        map.put("productPrice", "detail");
        map.put("itemTotalPrice", "detail");
        map.put("warehouseName", "detail");
        map.put("warehousePosition", "detail");
        map.put("batchNo", "detail");
        map.put("brand", "detail");
        map.put("itemRemark", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("supplierName", "supplierId");
        map.put("deptName", "deptId");
        map.put("productCode", "item_productCode");
        map.put("productName", "item_productName");
        map.put("productUnitName", "item_productUnitName");
        map.put("itemCount", "item_count");
        map.put("productPrice", "item_productPrice");
        map.put("itemTotalPrice", "item_totalProductPrice");
        map.put("warehouseName", "item_warehouseId");
        map.put("warehousePosition", "item_warehousePosition");
        map.put("batchNo", "item_batchNo");
        map.put("brand", "item_brand");
        map.put("itemRemark", "item_remark");
        return map;
    }

}
