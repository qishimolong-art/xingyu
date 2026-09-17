package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
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
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreatePurchaseReturnReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreateTargetDraftRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreateTransferOutReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPurchaseReturnableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPurchaseReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnRefundSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnTransferOutableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnTransferOutableItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleItemPriceReferenceFiller;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
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
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSetByFlatMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 销售退货")
@RestController
@RequestMapping("/erp/sale-return")
@Validated
public class ErpSaleReturnController {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_return";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();
    private static final Map<String, String> DETAIL_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "productId", "productIdentity",
            "productCode", "productIdentity",
            "productName", "productIdentity",
            "factoryCode", "productIdentity",
            "warehouseId", "warehouseName",
            "warehouseName", "warehouseName",
            "count", "count",
            "itemCount", "count",
            "productPrice", "productPrice",
            "returnReason", "returnReason",
            "remark", "remark");

    @Resource
    private ErpSaleReturnService saleReturnService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleItemPriceReferenceFiller itemPriceReferenceFiller;
    @Resource
    private ErpFieldConfigService fieldConfigService;
    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Resource
    private ErpCustomerDeptPermissionService customerDeptPermissionService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建销售退货")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:create')")
    public CommonResult<Long> createSaleReturn(@Valid @RequestBody ErpSaleReturnSaveReqVO createReqVO) {
        return success(saleReturnService.createSaleReturn(createReqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建销售退货草稿")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:create')")
    public CommonResult<Long> createSaleReturnDraft(@RequestBody ErpSaleReturnDraftCreateReqVO createReqVO) {
        return success(saleReturnService.createSaleReturnDraft(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新销售退货")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:update')")
    public CommonResult<Boolean> updateSaleReturn(@Valid @RequestBody ErpSaleReturnSaveReqVO updateReqVO) {
        saleReturnService.updateSaleReturn(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "更新销售退货草稿")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:update')")
    public CommonResult<Boolean> updateSaleReturnDraft(@RequestBody ErpSaleReturnDraftUpdateReqVO updateReqVO) {
        saleReturnService.updateSaleReturnDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/batch-update-items")
    @Operation(summary = "批量修改销售退货明细仓库/部门")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:update')")
    public CommonResult<Boolean> batchUpdateSaleReturnItems(
            @Valid @RequestBody ErpSaleReturnItemBatchUpdateReqVO updateReqVO) {
        saleReturnService.batchUpdateSaleReturnItems(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交销售退货草稿")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:update-status')")
    public CommonResult<Boolean> submitSaleReturn(@RequestParam("id") Long id) {
        saleReturnService.submitSaleReturn(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "修改销售退货备注")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:update')")
    public CommonResult<Boolean> updateSaleReturnRemark(
            @Valid @RequestBody ErpSaleUpdateRemarkReqVO updateReqVO) {
        saleReturnService.updateSaleReturnRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新销售退货的状态")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:update-status')")
    public CommonResult<Boolean> updateSaleReturnStatus(@RequestParam("id") Long id,
                                                        @RequestParam("status") Integer status,
            @RequestParam(value = "expectedCostBasisSignature", required = false) String expectedCostBasisSignature) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        if (expectedCostBasisSignature == null) {
            saleReturnService.updateSaleReturnStatus(id, status);
        } else {
            saleReturnService.updateSaleReturnStatusWithCostBasis(id, status, expectedCostBasisSignature);
        }
        return success(true);
    }

    @GetMapping("/transfer-outable-items")
    @Operation(summary = "获得销售退货可转调拨明细")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:transfer-out')")
    public CommonResult<List<ErpSaleReturnTransferOutableItemRespVO>> getTransferOutableItems(
            @RequestParam("returnId") Long returnId) {
        return success(saleReturnService.getTransferOutableItemsByReturnId(returnId));
    }

    @GetMapping("/transfer-outable-item-page")
    @Operation(summary = "获得销售退货可转调拨明细分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:transfer-out')")
    public CommonResult<PageResult<ErpSaleReturnTransferOutableItemRespVO>> getTransferOutableItemPage(
            @Valid ErpSaleReturnTransferOutableItemPageReqVO pageReqVO) {
        return success(saleReturnService.getTransferOutableItemPage(pageReqVO));
    }

    @PostMapping("/create-transfer-out")
    @Operation(summary = "由销售退货生成调拨出库草稿")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:transfer-out')")
    public CommonResult<ErpSaleReturnCreateTargetDraftRespVO> createTransferOutFromSaleReturn(
            @Valid @RequestBody ErpSaleReturnCreateTransferOutReqVO reqVO) {
        return success(saleReturnService.createTransferOutFromSaleReturn(reqVO));
    }

    @GetMapping("/purchase-returnable-items")
    @Operation(summary = "获得销售退货可转采购退货明细")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:purchase-return')")
    public CommonResult<List<ErpSaleReturnPurchaseReturnableItemRespVO>> getPurchaseReturnableItems(
            @RequestParam("returnId") Long returnId) {
        return success(saleReturnService.getPurchaseReturnableItemsByReturnId(returnId));
    }

    @GetMapping("/purchase-returnable-item-page")
    @Operation(summary = "获得销售退货可转采购退货明细分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:purchase-return')")
    public CommonResult<PageResult<ErpSaleReturnPurchaseReturnableItemRespVO>> getPurchaseReturnableItemPage(
            @Valid ErpSaleReturnPurchaseReturnableItemPageReqVO pageReqVO) {
        return success(saleReturnService.getPurchaseReturnableItemPage(pageReqVO));
    }

    @PostMapping("/create-purchase-return")
    @Operation(summary = "由销售退货生成采购退货草稿")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:purchase-return')")
    public CommonResult<ErpSaleReturnCreateTargetDraftRespVO> createPurchaseReturnFromSaleReturn(
            @Valid @RequestBody ErpSaleReturnCreatePurchaseReturnReqVO reqVO) {
        return success(saleReturnService.createPurchaseReturnFromSaleReturn(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除销售退货")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-return:delete')")
    public CommonResult<Boolean> deleteSaleReturn(@RequestParam("ids") List<Long> ids) {
        saleReturnService.deleteSaleReturn(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售退货")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:query')")
    public CommonResult<ErpSaleReturnRespVO> getSaleReturn(@RequestParam("id") Long id,
                                                           @RequestParam(value = "includeItems", required = false,
                                                                   defaultValue = "true") Boolean includeItems) {
        ErpSaleReturnDO saleReturn = saleReturnService.getSaleReturn(id);
        if (saleReturn == null) {
            return success(null);
        }
        List<ErpSaleReturnItemDO> saleReturnItemList = Boolean.TRUE.equals(includeItems)
                ? emptyIfNull(saleReturnService.getSaleReturnItemListByReturnId(id)) : Collections.emptyList();
        Map<Long, ErpProductRespVO> productMap = saleReturnItemList.isEmpty()
                ? Collections.emptyMap()
                : getProductVOMapIgnoreDataPermission(convertSet(saleReturnItemList, ErpSaleReturnItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = saleReturnItemList.isEmpty()
                ? Collections.emptyMap()
                : getWarehouseMapIgnoreDataPermission(convertSet(saleReturnItemList, ErpSaleReturnItemDO::getWarehouseId));
        Set<Long> itemDeptIds = convertSet(saleReturnItemList, ErpSaleReturnItemDO::getDeptId);
        itemDeptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        itemDeptIds.remove(null);
        Map<Long, DeptRespDTO> itemDeptMap = CollUtil.isEmpty(itemDeptIds) ? Collections.emptyMap() : deptApi.getDeptMap(itemDeptIds);
        Set<Long> userIds = convertUserIds(Collections.singletonList(saleReturn));
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : emptyIfNull(adminUserApi.getUserMap(userIds));
        DeptRespDTO dept = saleReturn.getDeptId() == null ? null : deptApi.getDept(saleReturn.getDeptId());
        ErpCustomerDO customer = saleReturn.getCustomerId() == null
                ? null : customerService.getCustomer(saleReturn.getCustomerId());
        ErpSaleReturnRespVO respVO = BeanUtils.toBean(saleReturn, ErpSaleReturnRespVO.class, saleReturnVO -> {
            fillUserNames(saleReturnVO, userMap);
            if (dept != null) {
                saleReturnVO.setDeptName(dept.getName());
            }
            if (customer != null) {
                saleReturnVO.setCustomerName(customer.getName());
            }
            List<ErpSaleReturnRespVO.Item> items = BeanUtils.toBean(saleReturnItemList, ErpSaleReturnRespVO.Item.class, item -> {
                ErpStockDO stock = getStockIgnoreDataPermission(item.getProductId(), item.getWarehouseId());
                item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                        .setProductCode(product.getCode()));
                MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> {
                    item.setWarehouseName(warehouse.getName());
                    item.setWarehouseDeptId(warehouse.getDeptId());
                    MapUtils.findAndThen(itemDeptMap, warehouse.getDeptId(),
                            deptResp -> item.setWarehouseDeptName(deptResp.getName()));
                });
            });
            saleReturnVO.setItems(emptyIfNull(items));
        });
        itemPriceReferenceFiller.fill(respVO.getItems());
        fieldPermissionMasker.maskSaleDetailFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @PreAuthorize("@ss.hasPermission('erp:sale-return:query')")
    public CommonResult<ErpSaleReturnRespVO> getSaleReturn(Long id) {
        return getSaleReturn(id, true);
    }

    @GetMapping("/refund-summary")
    @Operation(summary = "获得销售退货退款核销摘要")
    @Parameter(name = "id", description = "销售退货编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:query')")
    public CommonResult<ErpSaleReturnRefundSummaryRespVO> getSaleReturnRefundSummary(@RequestParam("id") Long id) {
        ErpSaleReturnDO saleReturn = saleReturnService.getSaleReturn(id);
        if (saleReturn == null) {
            return success(null);
        }
        BigDecimal refundableAmount = zeroIfNull(saleReturn.getTotalPrice());
        BigDecimal refundedAmount = zeroIfNull(financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                id, ErpBizTypeEnum.SALE_RETURN.getType())).abs();
        BigDecimal unrefundedAmount = refundableAmount.subtract(refundedAmount);
        return success(new ErpSaleReturnRefundSummaryRespVO()
                .setId(id)
                .setRefundableAmount(refundableAmount)
                .setRefundedAmount(refundedAmount)
                .setUnrefundedAmount(unrefundedAmount)
                .setRefundStatus(calculateSaleReturnRefundStatus(refundableAmount, refundedAmount)));
    }

    private Integer calculateSaleReturnRefundStatus(BigDecimal refundableAmount, BigDecimal refundedAmount) {
        BigDecimal normalizedRefundableAmount = zeroIfNull(refundableAmount).abs();
        BigDecimal normalizedRefundedAmount = zeroIfNull(refundedAmount).abs();
        if (normalizedRefundedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        if (normalizedRefundableAmount.compareTo(BigDecimal.ZERO) == 0
                || normalizedRefundedAmount.compareTo(normalizedRefundableAmount) < 0) {
            return 1;
        }
        return 2;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @GetMapping("/item-page")
    @Operation(summary = "获得销售退货明细分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:query')")
    public CommonResult<PageResult<ErpSaleReturnRespVO.Item>> getSaleReturnItemPage(
            @Valid ErpSaleReturnItemPageReqVO pageReqVO) {
        ErpSaleReturnDO saleReturn = saleReturnService.getSaleReturn(pageReqVO.getReturnId());
        if (saleReturn == null) {
            return success(PageResult.empty());
        }
        PageResult<ErpSaleReturnItemDO> pageResult = saleReturnService.getSaleReturnItemPage(pageReqVO);
        List<ErpSaleReturnItemDO> itemList = emptyIfNull(pageResult.getList());
        Map<Long, ErpProductRespVO> productMap = itemList.isEmpty()
                ? Collections.emptyMap()
                : getProductVOMapIgnoreDataPermission(convertSet(itemList, ErpSaleReturnItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = itemList.isEmpty()
                ? Collections.emptyMap()
                : getWarehouseMapIgnoreDataPermission(convertSet(itemList, ErpSaleReturnItemDO::getWarehouseId));
        Set<Long> itemDeptIds = convertSet(itemList, ErpSaleReturnItemDO::getDeptId);
        itemDeptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        itemDeptIds.remove(null);
        Map<Long, DeptRespDTO> itemDeptMap = CollUtil.isEmpty(itemDeptIds)
                ? Collections.emptyMap() : deptApi.getDeptMap(itemDeptIds);
        List<ErpSaleReturnRespVO.Item> items = BeanUtils.toBean(itemList, ErpSaleReturnRespVO.Item.class, item -> {
            ErpStockDO stock = getStockIgnoreDataPermission(item.getProductId(), item.getWarehouseId());
            item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
            MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                    .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                    .setProductCode(product.getCode()));
            MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> {
                item.setWarehouseName(warehouse.getName());
                item.setWarehouseDeptId(warehouse.getDeptId());
                MapUtils.findAndThen(itemDeptMap, warehouse.getDeptId(),
                        deptResp -> item.setWarehouseDeptName(deptResp.getName()));
            });
        });
        itemPriceReferenceFiller.fill(items);
        PageResult<ErpSaleReturnRespVO.Item> respResult = new PageResult<>(emptyIfNull(items), pageResult.getTotal());
        if (Boolean.TRUE.equals(pageReqVO.getMask())) {
            ErpSaleReturnRespVO context = BeanUtils.toBean(saleReturn, ErpSaleReturnRespVO.class);
            fieldPermissionMasker.clearSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, context,
                    respResult.getList());
        }
        return success(respResult);
    }

    @GetMapping("/warehouse-dept-simple-list")
    @Operation(summary = "获取销售退货批量修改仓库可用部门精简列表")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:update')")
    public CommonResult<List<DeptSimpleRespVO>> getWarehouseAvailableDeptSimpleList(
            @RequestParam("warehouseId") Long warehouseId) {
        return success(saleReturnService.getWarehouseAvailableDeptSimpleList(warehouseId));
    }

    @GetMapping("/warehouse-dept-simple-page")
    @Operation(summary = "获取销售退货批量修改仓库可用部门分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:update')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getWarehouseAvailableDeptSimplePage(
            @RequestParam("warehouseId") Long warehouseId, @Valid PageParam pageReqVO) {
        return success(saleReturnService.getWarehouseAvailableDeptSimplePage(warehouseId, pageReqVO));
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "获取销售退货可见部门精简分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getVisibleDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage(FIELD_PERMISSION_MODULE, pageReqVO));
    }

    @GetMapping("/customer-dept-simple-page")
    @Operation(summary = "获取销售退货客户可用部门分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getCustomerAvailableDeptSimplePage(
            @RequestParam("customerId") Long customerId, @Valid PageParam pageReqVO) {
        return success(customerDeptPermissionService.getAvailableDeptSimplePage(customerId, FIELD_PERMISSION_MODULE, pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获取销售退货用户精简分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        return success(buildUserSimplePage(pageReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售退货分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:query')")
    public CommonResult<PageResult<ErpSaleReturnRespVO>> getSaleReturnPage(@Valid ErpSaleReturnPageReqVO pageReqVO) {
        PageResult<ErpSaleReturnDO> pageResult = saleReturnService.getSaleReturnPage(pageReqVO);
        PageResult<ErpSaleReturnRespVO> respResult = Boolean.FALSE.equals(pageReqVO.getIncludeItems())
                ? buildSaleReturnVOPageResultWithoutItems(pageResult)
                : buildSaleReturnVOPageResult(pageResult);
        fieldPermissionMasker.maskSaleDetailFormsWithItems(FIELD_PERMISSION_MODULE, respResult.getList());
        return success(respResult);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出销售退货 Excel")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSaleReturnExcel(@Valid ErpSaleReturnPageReqVO pageReqVO,
                                    @RequestParam(value = "fields", required = false) String fields,
                                    HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpSaleReturnRespVO> list = buildSaleReturnVOPageResult(saleReturnService.getSaleReturnPage(pageReqVO)).getList();
        fieldPermissionMasker.maskSaleDetailFormsWithItems(FIELD_PERMISSION_MODULE, list);
        List<ErpSaleReturnExportRespVO> rows = buildSaleReturnExportList(list);
        fieldPermissionMasker.maskSaleDetailExportRows(FIELD_PERMISSION_MODULE, rows);
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpSaleReturnExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE, false), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "销售退货.xls", "数据", ErpSaleReturnExportRespVO.class, rows, includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get sale return export fields")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getSaleReturnExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpSaleReturnExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE, false), EXPORT_FIELD_PERMISSION_MAP));
    }

    @GetMapping("/export-import-template")
    @Operation(summary = "获得销售退货导入模板")
    public void exportImportTemplate(HttpServletResponse response) throws IOException {
        ErpSaleReturnImportExcelVO example = new ErpSaleReturnImportExcelVO();
        example.setProductCode("P0001");
        example.setProductName("示例配件");
        example.setWarehouseName("默认仓");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("100.00"));
        example.setReturnReason("质量问题");
        example.setRemark("备注");
        ExcelUtils.writeImportTemplate(response, "销售退货导入模板.xls", "销售退货",
                ErpSaleReturnImportExcelVO.class, Collections.singletonList(example), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.SALE_RETURN, ErpSaleReturnImportExcelVO.class,
                        DETAIL_IMPORT_FIELD_ALIAS_MAP));
    }

    @PostMapping("/import")
    @Operation(summary = "导入销售退货明细")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:create')")
    public CommonResult<ErpSaleReturnImportRespVO> importSaleReturn(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpSaleReturnImportExcelVO> list = ExcelUtils.read(file, ErpSaleReturnImportExcelVO.class);
        return success(saleReturnService.parseImportData(list));
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "下载销售退货导入错误数据")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:create')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, FIELD_PERMISSION_MODULE, response);
    }

    private PageResult<ErpSaleReturnRespVO> buildSaleReturnVOPageResult(PageResult<ErpSaleReturnDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpSaleReturnDO> saleReturnList = pageResult.getList();
        // 1.1 退货项
        List<ErpSaleReturnItemDO> saleReturnItemList = emptyIfNull(saleReturnService.getSaleReturnItemListByReturnIds(
                convertSet(saleReturnList, ErpSaleReturnDO::getId)));
        Map<Long, List<ErpSaleReturnItemDO>> saleReturnItemMap = convertMultiMap(saleReturnItemList, ErpSaleReturnItemDO::getReturnId);
        // 1.2 产品信息
        Map<Long, ErpProductRespVO> productMap = saleReturnItemList.isEmpty()
                ? Collections.emptyMap()
                : getProductVOMapIgnoreDataPermission(convertSet(saleReturnItemList, ErpSaleReturnItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = saleReturnItemList.isEmpty()
                ? Collections.emptyMap()
                : getWarehouseMapIgnoreDataPermission(convertSet(saleReturnItemList, ErpSaleReturnItemDO::getWarehouseId));
        // 1.3 客户信息
        Map<Long, ErpCustomerDO> customerMap = emptyIfNull(customerService.getCustomerMap(
                convertSet(saleReturnList, ErpSaleReturnDO::getCustomerId)));
        // 1.4 管理员信息
        Set<Long> userIds = convertUserIds(saleReturnList);
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : emptyIfNull(adminUserApi.getUserMap(userIds));
        Set<Long> deptIds = convertSet(saleReturnList, ErpSaleReturnDO::getDeptId);
        deptIds.addAll(convertSet(saleReturnItemList, ErpSaleReturnItemDO::getDeptId));
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds) ? Collections.emptyMap() : emptyIfNull(deptApi.getDeptMap(deptIds));
        // 2. 开始拼接
        return BeanUtils.toBean(pageResult, ErpSaleReturnRespVO.class, saleReturn -> {
            List<ErpSaleReturnItemDO> safeItems = emptyIfNull(saleReturnItemMap.get(saleReturn.getId()));
            List<ErpSaleReturnRespVO.Item> items = BeanUtils.toBean(safeItems, ErpSaleReturnRespVO.Item.class,
                    item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item
                            .setProductName(product.getName()).setProductBarCode(product.getBarCode())
                            .setProductUnitName(product.getUnitName()).setProductCode(product.getCode())));
            saleReturn.setItems(emptyIfNull(items));
            saleReturn.setProductNames(CollUtil.join(saleReturn.getItems(), "，", ErpSaleReturnRespVO.Item::getProductName));
            MapUtils.findAndThen(customerMap, saleReturn.getCustomerId(), supplier -> saleReturn.setCustomerName(supplier.getName()));
            fillUserNames(saleReturn, userMap);
            MapUtils.findAndThen(deptMap, saleReturn.getDeptId(), dept -> saleReturn.setDeptName(dept.getName()));
            saleReturn.getItems().forEach(item ->
                    MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> {
                        item.setWarehouseName(warehouse.getName());
                        item.setWarehouseDeptId(warehouse.getDeptId());
                        MapUtils.findAndThen(deptMap, warehouse.getDeptId(),
                                dept -> item.setWarehouseDeptName(dept.getName()));
                    }));
            itemPriceReferenceFiller.fill(saleReturn.getItems());
        });
    }

    private PageResult<ErpSaleReturnRespVO> buildSaleReturnVOPageResultWithoutItems(
            PageResult<ErpSaleReturnDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpSaleReturnDO> saleReturnList = pageResult.getList();
        Set<Long> returnIds = convertSet(saleReturnList, ErpSaleReturnDO::getId);
        Map<Long, String> productNamesMap = saleReturnItemMapper.selectProductNamesMapByReturnIds(returnIds);
        Map<Long, ErpCustomerDO> customerMap = emptyIfNull(customerService.getCustomerMap(
                convertSet(saleReturnList, ErpSaleReturnDO::getCustomerId)));
        Set<Long> userIds = convertUserIds(saleReturnList);
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : emptyIfNull(adminUserApi.getUserMap(userIds));
        Set<Long> deptIds = convertSet(saleReturnList, ErpSaleReturnDO::getDeptId);
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds)
                ? Collections.emptyMap() : emptyIfNull(deptApi.getDeptMap(deptIds));
        return BeanUtils.toBean(pageResult, ErpSaleReturnRespVO.class, saleReturn -> {
            saleReturn.setProductNames(productNamesMap.get(saleReturn.getId()));
            MapUtils.findAndThen(customerMap, saleReturn.getCustomerId(),
                    customer -> saleReturn.setCustomerName(customer.getName()));
            fillUserNames(saleReturn, userMap);
            MapUtils.findAndThen(deptMap, saleReturn.getDeptId(), dept -> saleReturn.setDeptName(dept.getName()));
        });
    }

    private PageResult<UserSimpleRespVO> buildUserSimplePage(PageParam pageReqVO) {
        PageResult<AdminUserRespDTO> page = adminUserApi.getUserSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageReqVO.getKeyword(), pageReqVO);
        List<UserSimpleRespVO> list = convertList(page.getList(), user ->
                new UserSimpleRespVO(user.getId(), user.getNickname(), user.getDeptId(), null));
        return new PageResult<>(list, page.getTotal());
    }

    private <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private <K, V> Map<K, V> emptyIfNull(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    private Map<Long, ErpProductRespVO> getProductVOMapIgnoreDataPermission(Set<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        return emptyIfNull(DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(productIds)));
    }

    private ErpStockDO getStockIgnoreDataPermission(Long productId, Long warehouseId) {
        return DataPermissionUtils.executeIgnore(() -> stockService.getStock(productId, warehouseId));
    }

    private Map<Long, ErpWarehouseDO> getWarehouseMapIgnoreDataPermission(Set<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyMap();
        }
        return emptyIfNull(DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds)));
    }

    private List<ErpSaleReturnExportRespVO> buildSaleReturnExportList(List<ErpSaleReturnRespVO> list) {
        List<ErpSaleReturnExportRespVO> rows = new ArrayList<>();
        for (ErpSaleReturnRespVO saleReturn : list) {
            if (CollUtil.isEmpty(saleReturn.getItems())) {
                rows.add(buildSaleReturnExportRow(saleReturn, null, true));
                continue;
            }
            for (int i = 0; i < saleReturn.getItems().size(); i++) {
                rows.add(buildSaleReturnExportRow(saleReturn, saleReturn.getItems().get(i), i == 0));
            }
        }
        return rows;
    }

    private ErpSaleReturnExportRespVO buildSaleReturnExportRow(ErpSaleReturnRespVO saleReturn,
                                                               ErpSaleReturnRespVO.Item item,
                                                               boolean fillMainFields) {
        ErpSaleReturnExportRespVO row = fillMainFields
                ? BeanUtils.toBean(saleReturn, ErpSaleReturnExportRespVO.class)
                : new ErpSaleReturnExportRespVO();
        row.setCustomerId(saleReturn.getCustomerId());
        if (item == null) {
            return row;
        }
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setProductUnitName(item.getProductUnitName());
        row.setWeight(item.getWeight());
        row.setPackageQty(item.getPackageQty());
        row.setWarehouseName(item.getWarehouseName());
        row.setBatchNo(item.getBatchNo());
        row.setItemCount(item.getCount());
        row.setProductPrice(item.getProductPrice());
        row.setReturnReason(item.getReturnReason());
        row.setWarehousePosition(item.getWarehousePosition());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private Set<Long> convertUserIds(List<ErpSaleReturnDO> saleReturns) {
        Set<Long> userIds = convertSetByFlatMap(saleReturns, saleReturn -> {
            List<Long> ids = new ArrayList<>(3);
            parseUserId(saleReturn.getCreator(), ids);
            parseUserId(saleReturn.getUpdater(), ids);
            ids.add(saleReturn.getSaleUserId());
            ids.add(saleReturn.getHandler());
            return ids.stream();
        });
        userIds.remove(null);
        return userIds;
    }

    private void fillUserNames(ErpSaleReturnRespVO saleReturn, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(saleReturn.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> saleReturn.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(saleReturn.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> saleReturn.setUpdaterName(user.getNickname()));
        }
        if (saleReturn.getSaleUserId() != null) {
            MapUtils.findAndThen(userMap, saleReturn.getSaleUserId(), user -> saleReturn.setSaleUserName(user.getNickname()));
        }
        if (saleReturn.getHandler() != null) {
            MapUtils.findAndThen(userMap, saleReturn.getHandler(), user -> saleReturn.setHandlerName(user.getNickname()));
        }
    }

    private Long parseUserId(String userId) {
        if (userId == null) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void parseUserId(String userId, List<Long> userIds) {
        Long parsedUserId = parseUserId(userId);
        if (parsedUserId != null) {
            userIds.add(parsedUserId);
        }
    }

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("no", "main");
        map.put("customerName", "main");
        map.put("status", "main");
        map.put("returnTime", "main");
        map.put("creatorName", "system");
        map.put("totalCount", "main");
        map.put("totalPrice", "main");
        map.put("feeAmount", "main");
        map.put("remark", "main");
        map.put("productCode", "detail");
        map.put("productName", "detail");
        map.put("productUnitName", "detail");
        map.put("weight", "detail");
        map.put("packageQty", "detail");
        map.put("warehouseName", "detail");
        map.put("batchNo", "detail");
        map.put("itemCount", "detail");
        map.put("productPrice", "detail");
        map.put("returnReason", "detail");
        map.put("warehousePosition", "detail");
        map.put("itemRemark", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("customerName", "customerId");
        map.put("productCode", "item_productCode");
        map.put("productName", "item_productId");
        map.put("productUnitName", "item_productUnitName");
        map.put("weight", "item_weight");
        map.put("packageQty", "item_packageQty");
        map.put("warehouseName", "item_warehouseId");
        map.put("batchNo", "item_batchNo");
        map.put("itemCount", "item_count");
        map.put("productPrice", "item_productPrice");
        map.put("returnReason", "item_returnReason");
        map.put("warehousePosition", "item_warehousePosition");
        map.put("itemRemark", "item_remark");
        return map;
    }

}
