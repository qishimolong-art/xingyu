package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderDetailImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 采购订单")
@RestController
@RequestMapping("/erp/purchase-order")
@Validated
public class ErpPurchaseOrderController {

    private static final String FIELD_PERMISSION_MODULE = "erp_purchase_order";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();
    private static final Map<String, String> ORDER_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "supplierId", "supplierName",
            "supplierName", "supplierName",
            "orderTime", "orderTime",
            "factoryOrderNo", "factoryOrderNo",
            "remark", "remark",
            "productId", "productCode",
            "productCode", "productCode",
            "count", "itemCount",
            "item_count", "itemCount",
            "itemCount", "itemCount",
            "productPrice", "productPrice",
            "taxPercent", "itemTaxPercent",
            "itemTaxPercent", "itemTaxPercent",
            "item_remark", "itemRemark",
            "itemRemark", "itemRemark");
    private static final Map<String, String> DETAIL_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "productId", "productCode",
            "productCode", "productCode",
            "count", "count",
            "item_count", "count",
            "itemCount", "count",
            "productPrice", "productPrice",
            "gift", "gift");

    @Resource
    private ErpPurchaseOrderService purchaseOrderService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpFieldConfigService fieldConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建采购订单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:create')")
    public CommonResult<Long> createPurchaseOrder(@Valid @RequestBody ErpPurchaseOrderSaveReqVO createReqVO) {
        return success(purchaseOrderService.createPurchaseOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购订单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:update')")
    public CommonResult<Boolean> updatePurchaseOrder(@Valid @RequestBody ErpPurchaseOrderSaveReqVO updateReqVO) {
        purchaseOrderService.updatePurchaseOrder(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新采购订单状态")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:update-status')")
    public CommonResult<Boolean> updatePurchaseOrderStatus(@RequestParam("id") Long id,
                                                           @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        purchaseOrderService.updatePurchaseOrderStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购订单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:delete')")
    public CommonResult<Boolean> deletePurchaseOrder(@RequestParam("ids") List<Long> ids) {
        purchaseOrderService.deletePurchaseOrder(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购订单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:query')")
    public CommonResult<ErpPurchaseOrderRespVO> getPurchaseOrder(@RequestParam("id") Long id) {
        ErpPurchaseOrderDO purchaseOrder = purchaseOrderService.getPurchaseOrder(id);
        if (purchaseOrder == null) {
            return success(null);
        }
        List<ErpPurchaseOrderItemDO> purchaseOrderItemList = purchaseOrderService.getPurchaseOrderItemListByOrderId(id);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseOrderItemList, ErpPurchaseOrderItemDO::getProductId));
        Set<Long> userIds = new HashSet<>();
        addUserId(userIds, purchaseOrder.getCreator());
        addUserId(userIds, purchaseOrder.getUpdater());
        addUserId(userIds, purchaseOrder.getPurchaser());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        DeptRespDTO dept = purchaseOrder.getDeptId() == null ? null : deptApi.getDept(purchaseOrder.getDeptId());
        ErpPurchaseOrderRespVO respVO = BeanUtils.toBean(purchaseOrder, ErpPurchaseOrderRespVO.class, purchaseOrderVO -> {
            purchaseOrderVO.setItems(BeanUtils.toBean(purchaseOrderItemList, ErpPurchaseOrderRespVO.Item.class, item -> {
                BigDecimal stockCount = stockService.getStockCount(item.getProductId());
                item.setStockCount(stockCount != null ? stockCount : BigDecimal.ZERO);
                MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                        .setProductCode(product.getCode()));
            }));
            if (dept != null) {
                purchaseOrderVO.setDeptName(dept.getName());
            }
            fillUserNames(purchaseOrderVO, userMap);
        });
        fieldPermissionMasker.mask("erp_purchase_order", respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购订单分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:query')")
    public CommonResult<PageResult<ErpPurchaseOrderRespVO>> getPurchaseOrderPage(@Valid ErpPurchaseOrderPageReqVO pageReqVO) {
        return success(buildPurchaseOrderVOPageResult(purchaseOrderService.getPurchaseOrderPage(pageReqVO)));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购订单 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseOrderExcel(@Valid ErpPurchaseOrderPageReqVO pageReqVO,
                                         @RequestParam(value = "fields", required = false) String fields,
                                         HttpServletResponse response) throws IOException {
        PageResult<ErpPurchaseOrderDO> pageResult = CollUtil.isNotEmpty(pageReqVO.getIds())
                ? new PageResult<>(purchaseOrderService.getPurchaseOrderList(pageReqVO.getIds()), (long) pageReqVO.getIds().size())
                : getPurchaseOrderExportPage(pageReqVO);
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpPurchaseOrderExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "采购订单.xls", "数据", ErpPurchaseOrderExportRespVO.class,
                buildPurchaseOrderExportList(pageResult), includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get purchase order export fields")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getPurchaseOrderExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpPurchaseOrderExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得采购订单导入模板")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchaseOrderImportExcelVO example = new ErpPurchaseOrderImportExcelVO();
        example.setFactoryOrderNo("FACTORY-001");
        example.setSupplierName("示例供应商");
        example.setOrderTime("2024-06-01 09:00:00");
        example.setRemark("整单备注");
        example.setProductCode("P0001");
        example.setProductName("示例产品一");
        example.setProductUnitName("个");
        example.setItemCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("100.00"));
        example.setGift("否");
        example.setItemTotalPrice(new BigDecimal("100.00"));
        example.setItemTaxPercent(BigDecimal.ZERO);
        example.setItemTaxPrice(BigDecimal.ZERO);
        example.setItemRemark("明细备注");

        ErpPurchaseOrderImportExcelVO secondItem = new ErpPurchaseOrderImportExcelVO();
        secondItem.setProductCode("P0002");
        secondItem.setProductName("示例产品二");
        secondItem.setProductUnitName("个");
        secondItem.setItemCount(new BigDecimal("2"));
        secondItem.setProductPrice(new BigDecimal("50.00"));
        secondItem.setGift("否");
        secondItem.setItemTotalPrice(new BigDecimal("100.00"));
        secondItem.setItemTaxPercent(BigDecimal.ZERO);
        secondItem.setItemTaxPrice(BigDecimal.ZERO);
        secondItem.setItemRemark("第二行明细");

        ErpPurchaseOrderImportExcelVO nextOrder = new ErpPurchaseOrderImportExcelVO();
        nextOrder.setFactoryOrderNo("FACTORY-002");
        nextOrder.setSupplierName("另一供应商");
        nextOrder.setOrderTime("2024-06-02 09:00:00");
        nextOrder.setRemark("下一张订单备注");
        nextOrder.setProductCode("P0003");
        nextOrder.setProductName("示例产品三");
        nextOrder.setProductUnitName("个");
        nextOrder.setItemCount(BigDecimal.ONE);
        nextOrder.setProductPrice(new BigDecimal("80.00"));
        nextOrder.setGift("否");
        nextOrder.setItemTotalPrice(new BigDecimal("80.00"));
        nextOrder.setItemTaxPercent(BigDecimal.ZERO);
        nextOrder.setItemTaxPrice(BigDecimal.ZERO);
        nextOrder.setItemRemark("下一张订单明细");
        ExcelUtils.writeImportTemplate(response, "采购订单导入模板.xls", "采购订单",
                ErpPurchaseOrderImportExcelVO.class, Arrays.asList(example, secondItem, nextOrder), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.PURCHASE_ORDER, ErpPurchaseOrderImportExcelVO.class,
                        ORDER_IMPORT_FIELD_ALIAS_MAP));
    }

    @PostMapping("/import")
    @Operation(summary = "导入采购订单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:create')")
    public CommonResult<ErpPurchaseOrderImportResultRespVO> importPurchaseOrder(@RequestParam("file") MultipartFile file)
            throws Exception {
        List<ErpPurchaseOrderImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseOrderImportExcelVO.class);
        if (CollUtil.isEmpty(list)) {
            return success(new ErpPurchaseOrderImportResultRespVO());
        }
        return success(purchaseOrderService.importPurchaseOrderList(list));
    }

    @GetMapping("/get-detail-import-template")
    @Operation(summary = "获得采购订单明细导入模板")
    public void getDetailImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchaseOrderDetailImportExcelVO example = new ErpPurchaseOrderDetailImportExcelVO();
        example.setProductCode("P0001");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("100.00"));
        example.setGift("否");
        ExcelUtils.writeImportTemplate(response, "采购订单明细导入模板.xls", "采购订单明细",
                ErpPurchaseOrderDetailImportExcelVO.class, Arrays.asList(example), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.PURCHASE_ORDER, ErpPurchaseOrderDetailImportExcelVO.class,
                        DETAIL_IMPORT_FIELD_ALIAS_MAP));
    }

    @PostMapping("/parse-detail-import-excel")
    @Operation(summary = "解析采购订单明细导入 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:create')")
    public CommonResult<ErpPurchaseOrderImportRespVO> parseImportExcel(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPurchaseOrderDetailImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseOrderDetailImportExcelVO.class);
        if (CollUtil.isEmpty(list)) {
            return success(new ErpPurchaseOrderImportRespVO());
        }
        return success(purchaseOrderService.parseImportData(list));
    }

    @Deprecated
    @PostMapping("/parse-import-excel")
    @Operation(summary = "解析采购订单明细导入 Excel（兼容旧地址）")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:create')")
    public CommonResult<ErpPurchaseOrderImportRespVO> parseOldImportExcel(@RequestParam("file") MultipartFile file)
            throws Exception {
        List<ErpPurchaseOrderDetailImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseOrderDetailImportExcelVO.class);
        if (CollUtil.isEmpty(list)) {
            return success(new ErpPurchaseOrderImportRespVO());
        }
        return success(purchaseOrderService.parseImportData(list));
    }

    @GetMapping("/inable-items")
    @Operation(summary = "获取采购订单的可入库明细")
    @Parameter(name = "orderId", description = "采购订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:create')")
    public CommonResult<List<ErpPurchaseOrderInableItemRespVO>> getInableItems(@RequestParam("orderId") Long orderId) {
        return success(purchaseOrderService.getInableItemsByOrderId(orderId));
    }

    private PageResult<ErpPurchaseOrderRespVO> buildPurchaseOrderVOPageResult(PageResult<ErpPurchaseOrderDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpPurchaseOrderItemDO> purchaseOrderItemList = purchaseOrderService.getPurchaseOrderItemListByOrderIds(
                convertSet(pageResult.getList(), ErpPurchaseOrderDO::getId));
        Map<Long, List<ErpPurchaseOrderItemDO>> purchaseOrderItemMap = convertMultiMap(
                purchaseOrderItemList, ErpPurchaseOrderItemDO::getOrderId);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseOrderItemList, ErpPurchaseOrderItemDO::getProductId));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseOrderDO::getSupplierId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(pageResult.getList(), ErpPurchaseOrderDO::getDeptId));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(purchaseOrder -> {
            addUserId(userIds, purchaseOrder.getCreator());
            addUserId(userIds, purchaseOrder.getUpdater());
            addUserId(userIds, purchaseOrder.getPurchaser());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        return BeanUtils.toBean(pageResult, ErpPurchaseOrderRespVO.class, purchaseOrder -> {
            purchaseOrder.setItems(BeanUtils.toBean(purchaseOrderItemMap.get(purchaseOrder.getId()),
                    ErpPurchaseOrderRespVO.Item.class, item ->
                            MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                                    .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                                    .setProductCode(product.getCode()))));
            purchaseOrder.setProductNames(CollUtil.join(purchaseOrder.getItems(), "，",
                    ErpPurchaseOrderRespVO.Item::getProductName));
            MapUtils.findAndThen(supplierMap, purchaseOrder.getSupplierId(),
                    supplier -> purchaseOrder.setSupplierName(supplier.getName()));
            MapUtils.findAndThen(deptMap, purchaseOrder.getDeptId(),
                    dept -> purchaseOrder.setDeptName(dept.getName()));
            fillUserNames(purchaseOrder, userMap);
            purchaseOrder.setInStatus(calcStatus(purchaseOrder.getInCount(), purchaseOrder.getTotalCount()));
            purchaseOrder.setReturnStatus(calcStatus(purchaseOrder.getReturnCount(), purchaseOrder.getTotalCount()));
        });
    }

    private List<ErpPurchaseOrderExportRespVO> buildPurchaseOrderExportList(PageResult<ErpPurchaseOrderDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return Collections.emptyList();
        }
        List<ErpPurchaseOrderItemDO> purchaseOrderItemList = purchaseOrderService.getPurchaseOrderItemListByOrderIds(
                convertSet(pageResult.getList(), ErpPurchaseOrderDO::getId));
        Map<Long, List<ErpPurchaseOrderItemDO>> purchaseOrderItemMap = convertMultiMap(
                purchaseOrderItemList, ErpPurchaseOrderItemDO::getOrderId);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseOrderItemList, ErpPurchaseOrderItemDO::getProductId));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseOrderDO::getSupplierId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(pageResult.getList(), purchaseOrder -> parseUserId(purchaseOrder.getCreator())));
        List<ErpPurchaseOrderExportRespVO> rows = new ArrayList<>();
        for (ErpPurchaseOrderDO purchaseOrder : pageResult.getList()) {
            List<ErpPurchaseOrderItemDO> items = purchaseOrderItemMap.get(purchaseOrder.getId());
            Long creatorId = parseUserId(purchaseOrder.getCreator());
            if (CollUtil.isEmpty(items)) {
                rows.add(buildPurchaseOrderExportRow(purchaseOrder,
                        supplierMap.get(purchaseOrder.getSupplierId()),
                        creatorId == null ? null : userMap.get(creatorId), null, null, true));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                ErpPurchaseOrderItemDO item = items.get(i);
                rows.add(buildPurchaseOrderExportRow(purchaseOrder,
                        supplierMap.get(purchaseOrder.getSupplierId()),
                        creatorId == null ? null : userMap.get(creatorId),
                        item, productMap.get(item.getProductId()), i == 0));
            }
        }
        return rows;
    }

    private PageResult<ErpPurchaseOrderDO> getPurchaseOrderExportPage(ErpPurchaseOrderPageReqVO pageReqVO) {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        return purchaseOrderService.getPurchaseOrderPage(pageReqVO);
    }

    private ErpPurchaseOrderExportRespVO buildPurchaseOrderExportRow(ErpPurchaseOrderDO purchaseOrder,
                                                                     ErpSupplierDO supplier,
                                                                     AdminUserRespDTO creator,
                                                                     ErpPurchaseOrderItemDO item,
                                                                     ErpProductRespVO product,
                                                                     boolean fillOrderFields) {
        ErpPurchaseOrderExportRespVO row = fillOrderFields
                ? BeanUtils.toBean(purchaseOrder, ErpPurchaseOrderExportRespVO.class)
                : new ErpPurchaseOrderExportRespVO();
        if (fillOrderFields) {
            row.setFactoryOrderNo(purchaseOrder.getFactoryOrderNo());
        }
        row.setSupplierName(fillOrderFields && supplier != null ? supplier.getName() : null);
        row.setCreatorName(fillOrderFields && creator != null ? creator.getNickname() : null);
        if (item == null) {
            return row;
        }
        row.setProductCode(product != null ? product.getCode() : null);
        row.setProductName(product != null ? product.getName() : null);
        row.setProductUnitName(product != null ? product.getUnitName() : null);
        row.setItemCount(item.getCount());
        row.setProductPrice(item.getProductPrice());
        row.setGift(Boolean.TRUE.equals(item.getGift()) ? "是" : "否");
        row.setItemTotalPrice(item.getProductPrice() == null || item.getCount() == null
                ? null : item.getProductPrice().multiply(item.getCount()));
        row.setItemTaxPercent(item.getTaxPercent());
        row.setItemTaxPrice(item.getTaxPrice());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer calcStatus(BigDecimal doneCount, BigDecimal totalCount) {
        if (doneCount == null || doneCount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (totalCount == null || totalCount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (doneCount.compareTo(totalCount) >= 0) {
            return 2;
        }
        return 1;
    }

    private void fillUserNames(ErpPurchaseOrderRespVO purchaseOrder, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(purchaseOrder.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> purchaseOrder.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(purchaseOrder.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> purchaseOrder.setUpdaterName(user.getNickname()));
        }
        Long purchaserId = purchaseOrder.getPurchaser();
        if (purchaserId != null) {
            MapUtils.findAndThen(userMap, purchaserId, user -> purchaseOrder.setPurchaserName(user.getNickname()));
        }
    }

    private void addUserId(Set<Long> userIds, String userId) {
        Long parsed = parseUserId(userId);
        if (parsed != null) {
            userIds.add(parsed);
        }
    }

    private void addUserId(Set<Long> userIds, Long userId) {
        if (userId != null) {
            userIds.add(userId);
        }
    }

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("no", "main");
        map.put("factoryOrderNo", "main");
        map.put("supplierName", "main");
        map.put("orderTime", "main");
        map.put("status", "main");
        map.put("creatorName", "system");
        map.put("totalCount", "main");
        map.put("inCount", "main");
        map.put("returnCount", "main");
        map.put("totalPrice", "main");
        map.put("feeAmount", "main");
        map.put("remark", "main");
        map.put("productCode", "detail");
        map.put("productName", "detail");
        map.put("productUnitName", "detail");
        map.put("itemCount", "detail");
        map.put("productPrice", "detail");
        map.put("gift", "detail");
        map.put("itemTotalPrice", "detail");
        map.put("itemTaxPercent", "detail");
        map.put("itemTaxPrice", "detail");
        map.put("itemRemark", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("supplierName", "supplierId");
        map.put("productCode", "item_productCode");
        map.put("productName", "item_productId");
        map.put("productUnitName", "item_productUnitName");
        map.put("itemCount", "item_count");
        map.put("productPrice", "item_productPrice");
        map.put("gift", "item_gift");
        map.put("itemTotalPrice", "item_totalProductPrice");
        map.put("itemRemark", "item_remark");
        return map;
    }

}
