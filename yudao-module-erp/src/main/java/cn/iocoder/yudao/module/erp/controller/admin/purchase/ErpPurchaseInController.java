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
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
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
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 采购入库")
@RestController
@RequestMapping("/erp/purchase-in")
@Validated
public class ErpPurchaseInController {

    @Resource
    private ErpPurchaseInService purchaseInService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpWarehouseService warehouseService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建采购入库")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:create')")
    public CommonResult<Long> createPurchaseIn(@Valid @RequestBody ErpPurchaseInSaveReqVO createReqVO) {
        return success(purchaseInService.createPurchaseIn(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购入库")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:update')")
    public CommonResult<Boolean> updatePurchaseIn(@Valid @RequestBody ErpPurchaseInSaveReqVO updateReqVO) {
        purchaseInService.updatePurchaseIn(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新采购入库的状态")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:update-status')")
    public CommonResult<Boolean> updatePurchaseInStatus(@RequestParam("id") Long id,
                                                        @RequestParam("status") Integer status) {
        purchaseInService.updatePurchaseInStatus(id, status);
        return success(true);
    }

    @PostMapping("/import")
    @Operation(summary = "解析采购入库导入 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:create')")
    public CommonResult<ErpPurchaseInImportRespVO> importPurchaseIn(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPurchaseInImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseInImportExcelVO.class);
        return success(purchaseInService.importPurchaseInItems(list));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得采购入库导入模板")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:create')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchaseInImportExcelVO example = new ErpPurchaseInImportExcelVO();
        example.setProductCode("P000001");
        example.setWarehouseName("主仓");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("10.00"));
        example.setWholeQty(1);
        example.setWarehousePosition("A-01-01");
        example.setBatchNo("B20260526");
        example.setRemark("示例");
        ExcelUtils.write(response, "采购入库导入模板.xls", "采购入库", ErpPurchaseInImportExcelVO.class,
                java.util.Collections.singletonList(example));
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购入库")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:query')")
    public CommonResult<ErpPurchaseInRespVO> getPurchaseIn(@RequestParam("id") Long id) {
        ErpPurchaseInDO purchaseIn = purchaseInService.getPurchaseIn(id);
        if (purchaseIn == null) {
            return success(null);
        }
        List<ErpPurchaseInItemDO> purchaseInItemList = purchaseInService.getPurchaseInItemListByInId(id);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseInItemList, ErpPurchaseInItemDO::getProductId));
        return success(BeanUtils.toBean(purchaseIn, ErpPurchaseInRespVO.class, purchaseInVO ->
                purchaseInVO.setItems(BeanUtils.toBean(purchaseInItemList, ErpPurchaseInRespVO.Item.class, item -> {
                    ErpStockDO stock = stockService.getStock(item.getProductId(), item.getWarehouseId());
                    item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                    MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                            .setProductCode(product.getCode()));
                }))));
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购入库分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:query')")
    public CommonResult<PageResult<ErpPurchaseInRespVO>> getPurchaseInPage(@Valid ErpPurchaseInPageReqVO pageReqVO) {
        PageResult<ErpPurchaseInDO> pageResult = purchaseInService.getPurchaseInPage(pageReqVO);
        return success(buildPurchaseInVOPageResult(pageResult));
    }

    @GetMapping("/returnable-items")
    @Operation(summary = "获得采购入库单的可退明细（按单退货使用）")
    @Parameter(name = "inId", description = "采购入库单 ID", required = true, example = "17386")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public CommonResult<List<ErpPurchaseReturnableItemRespVO>> getReturnableItems(@RequestParam("inId") Long inId) {
        return success(purchaseInService.getReturnableItemsByInId(inId));
    }

    @PostMapping("/create-from-order")
    @Operation(summary = "从采购订单分批入库（自动审批生效）")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:create')")
    public CommonResult<Long> createPurchaseInFromOrder(@Valid @RequestBody ErpPurchaseInFromOrderReqVO reqVO) {
        return success(purchaseInService.createPurchaseInFromOrder(reqVO));
    }

    @GetMapping("/list-approved-for-adjust")
    @Operation(summary = "获得指定供应商下已审批的入库单（供采购调价选择）")
    @Parameter(name = "supplierId", description = "供应商编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:query')")
    public CommonResult<List<ErpPurchaseInForAdjustRespVO>> getApprovedPurchaseInsBySupplier(
            @RequestParam("supplierId") Long supplierId) {
        return success(purchaseInService.getApprovedPurchaseInsBySupplier(supplierId));
    }

    @GetMapping("/list-items-for-adjust")
    @Operation(summary = "获得指定供应商下已审批入库单的明细（供采购调价选择）")
    @Parameter(name = "supplierId", description = "供应商编号", required = true, example = "1024")
    @Parameter(name = "excludeAdjusted", description = "是否过滤已调价明细", example = "true")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:query')")
    public CommonResult<List<ErpPurchaseInItemForAdjustRespVO>> getApprovedPurchaseInItemsBySupplier(
            @RequestParam("supplierId") Long supplierId,
            @RequestParam(value = "excludeAdjusted", required = false) Boolean excludeAdjusted) {
        return success(purchaseInService.getApprovedPurchaseInItemsBySupplier(supplierId, excludeAdjusted));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购入库 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-in:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseInExcel(@Valid ErpPurchaseInPageReqVO pageReqVO,
                                      HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpPurchaseInRespVO> list = buildPurchaseInVOPageResult(purchaseInService.getPurchaseInPage(pageReqVO)).getList();
        ExcelUtils.write(response, "采购入库.xls", "数据", ErpPurchaseInExportRespVO.class,
                buildPurchaseInExportList(list));
    }

    private PageResult<ErpPurchaseInRespVO> buildPurchaseInVOPageResult(PageResult<ErpPurchaseInDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpPurchaseInItemDO> purchaseInItemList = purchaseInService.getPurchaseInItemListByInIds(
                convertSet(pageResult.getList(), ErpPurchaseInDO::getId));
        Map<Long, List<ErpPurchaseInItemDO>> purchaseInItemMap = convertMultiMap(purchaseInItemList, ErpPurchaseInItemDO::getInId);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseInItemList, ErpPurchaseInItemDO::getProductId));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseInDO::getSupplierId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(pageResult.getList(), purchaseIn -> Long.parseLong(purchaseIn.getCreator())));
        return BeanUtils.toBean(pageResult, ErpPurchaseInRespVO.class, purchaseIn -> {
            purchaseIn.setItems(BeanUtils.toBean(purchaseInItemMap.get(purchaseIn.getId()), ErpPurchaseInRespVO.Item.class,
                    item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                            .setProductCode(product.getCode()))));
            purchaseIn.setProductNames(CollUtil.join(purchaseIn.getItems(), "，", ErpPurchaseInRespVO.Item::getProductName));
            MapUtils.findAndThen(supplierMap, purchaseIn.getSupplierId(), supplier -> purchaseIn.setSupplierName(supplier.getName()));
            MapUtils.findAndThen(userMap, Long.parseLong(purchaseIn.getCreator()), user -> purchaseIn.setCreatorName(user.getNickname()));
        });
    }

    private List<ErpPurchaseInExportRespVO> buildPurchaseInExportList(List<ErpPurchaseInRespVO> list) {
        List<ErpWarehouseDO> warehouses = warehouseService.getWarehouseListByStatus(0);
        Map<Long, ErpWarehouseDO> warehouseMap = cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                warehouses, ErpWarehouseDO::getId);
        List<ErpPurchaseInExportRespVO> rows = new ArrayList<>();
        for (ErpPurchaseInRespVO purchaseIn : list) {
            if (CollUtil.isEmpty(purchaseIn.getItems())) {
                rows.add(BeanUtils.toBean(purchaseIn, ErpPurchaseInExportRespVO.class));
                continue;
            }
            for (ErpPurchaseInRespVO.Item item : purchaseIn.getItems()) {
                rows.add(BeanUtils.toBean(purchaseIn, ErpPurchaseInExportRespVO.class, row -> {
                    row.setProductCode(item.getProductCode());
                    row.setProductName(item.getProductName());
                    row.setProductUnitName(item.getProductUnitName());
                    row.setPackageQty(item.getPackageQty());
                    row.setWholeQty(item.getWholeQty());
                    row.setItemCount(item.getCount());
                    row.setProductPrice(item.getProductPrice());
                    row.setWarehousePosition(item.getWarehousePosition());
                    row.setBatchNo(item.getBatchNo());
                    row.setDrawingNo(item.getDrawingNo());
                    row.setBrand(item.getBrand());
                    row.setItemRemark(item.getRemark());
                    MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> row.setWarehouseName(warehouse.getName()));
                }));
            }
        }
        return rows;
    }

}
