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
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnImportRespVO;
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
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
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
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建采购退货")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public CommonResult<Long> createPurchaseReturn(@Valid @RequestBody ErpPurchaseReturnSaveReqVO createReqVO) {
        return success(purchaseReturnService.createPurchaseReturn(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购退货")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update')")
    public CommonResult<Boolean> updatePurchaseReturn(@Valid @RequestBody ErpPurchaseReturnSaveReqVO updateReqVO) {
        purchaseReturnService.updatePurchaseReturn(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新采购退货的状态")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:update-status')")
    public CommonResult<Boolean> updatePurchaseReturnStatus(@RequestParam("id") Long id,
                                                             @RequestParam("status") Integer status) {
        purchaseReturnService.updatePurchaseReturnStatus(id, status);
        return success(true);
    }

    @PostMapping("/import")
    @Operation(summary = "解析采购退货导入 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public CommonResult<ErpPurchaseReturnImportRespVO> importPurchaseReturn(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpPurchaseReturnImportExcelVO> list = ExcelUtils.read(file, ErpPurchaseReturnImportExcelVO.class);
        return success(purchaseReturnService.importPurchaseReturnItems(list));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得采购退货导入模板")
    @PreAuthorize("@ss.hasPermission('erp:purchase-return:create')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchaseReturnImportExcelVO example = new ErpPurchaseReturnImportExcelVO();
        example.setProductCode("P000001");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("10.00"));
        example.setWarehouseName("主仓");
        example.setRemark("示例");
        ExcelUtils.write(response, "采购退货导入模板.xls", "采购退货", ErpPurchaseReturnImportExcelVO.class,
                java.util.Collections.singletonList(example));
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
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseReturnItemList, ErpPurchaseReturnItemDO::getProductId));
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
        return success(BeanUtils.toBean(purchaseReturn, ErpPurchaseReturnRespVO.class, purchaseReturnVO -> {
                purchaseReturnVO.setItems(BeanUtils.toBean(purchaseReturnItemList, ErpPurchaseReturnRespVO.Item.class, item -> {
                    ErpStockDO stock = stockService.getStock(item.getProductId(), item.getWarehouseId());
                    item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                    MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName()));
                    if (item.getSourceInItemId() != null) {
                        ErpPurchaseInItemDO inItem = finalInItemMap.get(item.getSourceInItemId());
                        if (inItem != null) {
                            item.setInCount(inItem.getCount());
                            BigDecimal otherReturned = finalReturnedMap.getOrDefault(item.getSourceInItemId(), BigDecimal.ZERO);
                            item.setReturnableCount(inItem.getCount().subtract(otherReturned));
                        }
                    }
                }));
                fillUserNames(purchaseReturnVO, userMap);
        }));
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
        ExcelUtils.write(response, "采购退货.xls", "数据", ErpPurchaseReturnExportRespVO.class,
                buildPurchaseReturnExportList(list));
    }

    private PageResult<ErpPurchaseReturnRespVO> buildPurchaseReturnVOPageResult(PageResult<ErpPurchaseReturnDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpPurchaseReturnItemDO> purchaseReturnItemList = purchaseReturnService.getPurchaseReturnItemListByReturnIds(
                convertSet(pageResult.getList(), ErpPurchaseReturnDO::getId));
        Map<Long, List<ErpPurchaseReturnItemDO>> purchaseReturnItemMap = convertMultiMap(purchaseReturnItemList, ErpPurchaseReturnItemDO::getReturnId);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(purchaseReturnItemList, ErpPurchaseReturnItemDO::getProductId));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseReturnDO::getSupplierId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(collectUserIds(pageResult.getList()));
        return BeanUtils.toBean(pageResult, ErpPurchaseReturnRespVO.class, purchaseReturn -> {
            purchaseReturn.setItems(BeanUtils.toBean(purchaseReturnItemMap.get(purchaseReturn.getId()), ErpPurchaseReturnRespVO.Item.class,
                    item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName()))));
            purchaseReturn.setProductNames(CollUtil.join(purchaseReturn.getItems(), "，", ErpPurchaseReturnRespVO.Item::getProductName));
            MapUtils.findAndThen(supplierMap, purchaseReturn.getSupplierId(), supplier -> purchaseReturn.setSupplierName(supplier.getName()));
            fillUserNames(purchaseReturn, userMap);
        });
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

}
