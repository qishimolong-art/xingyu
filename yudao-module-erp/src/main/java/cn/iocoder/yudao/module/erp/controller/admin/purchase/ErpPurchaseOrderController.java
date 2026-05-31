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
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
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

    @Resource
    private ErpPurchaseOrderService purchaseOrderService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private AdminUserApi adminUserApi;

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
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        return success(BeanUtils.toBean(purchaseOrder, ErpPurchaseOrderRespVO.class, purchaseOrderVO -> {
            purchaseOrderVO.setItems(BeanUtils.toBean(purchaseOrderItemList, ErpPurchaseOrderRespVO.Item.class, item -> {
                BigDecimal stockCount = stockService.getStockCount(item.getProductId());
                item.setStockCount(stockCount != null ? stockCount : BigDecimal.ZERO);
                MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                        .setProductCode(product.getCode()));
            }));
            fillUserNames(purchaseOrderVO, userMap);
        }));
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
                                         HttpServletResponse response) throws IOException {
        PageResult<ErpPurchaseOrderDO> pageResult = CollUtil.isNotEmpty(pageReqVO.getIds())
                ? new PageResult<>(purchaseOrderService.getPurchaseOrderList(pageReqVO.getIds()), (long) pageReqVO.getIds().size())
                : getPurchaseOrderExportPage(pageReqVO);
        ExcelUtils.write(response, "采购订单.xls", "数据", ErpPurchaseOrderExportRespVO.class,
                buildPurchaseOrderExportList(pageResult));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得采购订单导入模板")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchaseOrderImportExcelVO example = new ErpPurchaseOrderImportExcelVO();
        example.setProductCode("P0001");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("100.00"));
        ExcelUtils.write(response, "采购订单导入模板.xls", "采购订单",
                ErpPurchaseOrderImportExcelVO.class, Arrays.asList(example));
    }

    @PostMapping("/parse-import-excel")
    @Operation(summary = "解析采购订单导入 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:create')")
    public CommonResult<ErpPurchaseOrderImportRespVO> parseImportExcel(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPurchaseOrderImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseOrderImportExcelVO.class);
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
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(purchaseOrder -> {
            addUserId(userIds, purchaseOrder.getCreator());
            addUserId(userIds, purchaseOrder.getUpdater());
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
    }

    private void addUserId(Set<Long> userIds, String userId) {
        Long parsed = parseUserId(userId);
        if (parsed != null) {
            userIds.add(parsed);
        }
    }

}
