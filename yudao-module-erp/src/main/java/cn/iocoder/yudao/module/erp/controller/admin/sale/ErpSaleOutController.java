package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
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
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutUpdateExpressFileReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.common.ErpOriginalSettlementAmountUtils;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutBillService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_OUT_EXPRESS_FILE_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_OUT_EXPRESS_FILE_SIZE_EXCEEDED;

@Tag(name = "管理后台 - ERP 销售出库")
@RestController
@RequestMapping("/erp/sale-out")
@Validated
public class ErpSaleOutController {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_out";

    private static final class SourceDocumentMeta {

        private final String creator;
        private final LocalDateTime createTime;
        private final String freightType;

        private SourceDocumentMeta(String creator, LocalDateTime createTime, String freightType) {
            this.creator = creator;
            this.createTime = createTime;
            this.freightType = freightType;
        }
    }

    @Resource
    private ErpSaleOutService saleOutService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockOutBillService stockOutBillService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Resource
    private ErpSaleOrderMapper saleOrderMapper;
    @Resource
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Resource
    private ErpSaleCartMapper saleCartMapper;
    @Resource
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建销售出库")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:create')")
    public CommonResult<Long> createSaleOut(@Valid @RequestBody ErpSaleOutSaveReqVO createReqVO) {
        return success(saleOutService.createSaleOut(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新销售出库")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:update')")
    public CommonResult<Boolean> updateSaleOut(@Valid @RequestBody ErpSaleOutSaveReqVO updateReqVO) {
        saleOutService.updateSaleOut(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "修改销售出库备注")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:update')")
    public CommonResult<Boolean> updateSaleOutRemark(
            @Valid @RequestBody ErpSaleUpdateRemarkReqVO updateReqVO) {
        saleOutService.updateSaleOutRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-express-file")
    @Operation(summary = "更新销售单快递单")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:upload-express')")
    public CommonResult<Boolean> updateSaleOutExpressFile(
            @Valid @RequestBody ErpSaleOutUpdateExpressFileReqVO updateReqVO) {
        saleOutService.updateSaleOutExpressFile(updateReqVO);
        return success(true);
    }

    @PostMapping("/upload-express-file")
    @Operation(summary = "上传销售单快递单")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:upload-express')")
    public CommonResult<String> uploadSaleOutExpressFile(
            @RequestParam("id") Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw exception(SALE_OUT_EXPRESS_FILE_EMPTY);
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw exception(SALE_OUT_EXPRESS_FILE_SIZE_EXCEEDED);
        }
        return success(saleOutService.uploadSaleOutExpressFile(
                id, file.getBytes(), file.getOriginalFilename()));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新销售出库的状态")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:update-status')")
    public CommonResult<Boolean> updateSaleOutStatus(@RequestParam("id") Long id,
                                                    @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        saleOutService.updateSaleOutStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除销售出库")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-out:delete')")
    public CommonResult<Boolean> deleteSaleOut(@RequestParam("ids") List<Long> ids) {
        saleOutService.deleteSaleOut(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售出库")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:query')")
    public CommonResult<ErpSaleOutRespVO> getSaleOut(@RequestParam("id") Long id) {
        ErpSaleOutDO saleOut = saleOutService.getSaleOut(id);
        if (saleOut == null) {
            return success(null);
        }
        List<ErpSaleOutItemDO> saleOutItemList = saleOutService.getSaleOutItemListByOutId(id);
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(saleOutItemList, ErpSaleOutItemDO::getProductId));
        // 仓库信息
        Set<Long> warehouseIds = convertSet(saleOutItemList, ErpSaleOutItemDO::getWarehouseId);
        warehouseIds.addAll(convertSet(saleOutItemList, ErpSaleOutItemDO::getSourceWarehouseId));
        warehouseIds.remove(null);
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(warehouseIds);
        Set<Long> deptIds = convertSet(saleOutItemList, ErpSaleOutItemDO::getDeptId);
        deptIds.addAll(convertSet(saleOutItemList, ErpSaleOutItemDO::getSourceDeptId));
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> itemDeptMap = CollUtil.isEmpty(deptIds) ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        // 退货状态
        Set<Long> outItemIds = convertSet(saleOutItemList, ErpSaleOutItemDO::getId);
        Map<Long, BigDecimal> returnedCountMap = saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(outItemIds);

        ErpSaleOutRespVO respVO = BeanUtils.toBean(saleOut, ErpSaleOutRespVO.class, saleOutVO ->
                saleOutVO.setItems(BeanUtils.toBean(saleOutItemList, ErpSaleOutRespVO.Item.class, item -> {
                    ErpStockDO stock = getStockIgnoreDataPermission(item.getProductId(), item.getWarehouseId());
                    item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                    MapUtils.findAndThen(productMap, item.getProductId(), product -> {
                        item.setProductName(product.getName())
                                .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName());
                        item.setProductCode(product.getCode());
                        item.setVehicleModel(product.getVehicleModel());
                        item.setStandard(product.getStandard());
                        item.setFeatureCode(product.getFeatureCode());
                        item.setBrand(product.getBrand());
                        item.setDrawingNo(product.getDrawingNo());
                        item.setOriginPlace(product.getOriginPlace());
                    });
                    MapUtils.findAndThen(warehouseMap, item.getWarehouseId(),
                            warehouse -> {
                                item.setWarehouseName(warehouse.getName());
                                item.setWarehouseDeptId(warehouse.getDeptId());
                                MapUtils.findAndThen(itemDeptMap, warehouse.getDeptId(),
                                        dept -> item.setWarehouseDeptName(dept.getName()));
                            });
                    MapUtils.findAndThen(warehouseMap, item.getSourceWarehouseId(),
                            warehouse -> item.setSourceWarehouseName(warehouse.getName()));
                    MapUtils.findAndThen(itemDeptMap, item.getSourceDeptId(),
                            dept -> item.setSourceDeptName(dept.getName()));
                    item.setCrossDept(item.getSourceWarehouseId() != null
                            && !Objects.equals(item.getSourceWarehouseId(), item.getWarehouseId()));
                    MapUtils.findAndThen(itemDeptMap, item.getDeptId(),
                            dept -> item.setDeptName(dept.getName()));
                    // 计算产品金额
                    if (item.getProductPrice() != null && item.getCount() != null) {
                        item.setTotalProductPrice(item.getProductPrice().multiply(item.getCount()));
                    }
                    // 已退数量
                    item.setReturnedCount(returnedCountMap.get(item.getId()));
                })));
        // 填充主表关联字段
        fillSaleOutRelationFields(respVO, saleOut);
        fillSaleOutStockOutBillInfo(respVO, id);
        // 退货状态
        respVO.setReturnStatus(calculateReturnStatus(saleOutItemList, returnedCountMap));
        fieldPermissionMasker.maskSaleDetailFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    private Map<Long, ErpProductRespVO> getProductVOMapIgnoreDataPermission(Set<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(productIds));
    }

    private ErpStockDO getStockIgnoreDataPermission(Long productId, Long warehouseId) {
        return DataPermissionUtils.executeIgnore(() -> stockService.getStock(productId, warehouseId));
    }

    private Map<Long, ErpWarehouseDO> getWarehouseMapIgnoreDataPermission(Set<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds));
    }

    private Map<Integer, Map<Long, SourceDocumentMeta>> buildSourceDocumentMetaMap(
            Collection<ErpSaleOutDO> saleOuts) {
        Map<Integer, Map<Long, SourceDocumentMeta>> result = new HashMap<>();
        Set<Long> sourceIds = getSourceIds(saleOuts, ErpSaleBizSourceTypeEnum.LEGACY_ORDER.getType());
        if (CollUtil.isNotEmpty(sourceIds)) {
            putSourceDocumentMeta(result, ErpSaleBizSourceTypeEnum.LEGACY_ORDER.getType(),
                    saleOrderMapper.selectBatchIds(sourceIds), ErpSaleOrderDO::getId,
                    ErpSaleOrderDO::getCreator, ErpSaleOrderDO::getCreateTime);
        }
        sourceIds = getSourceIds(saleOuts, ErpSaleBizSourceTypeEnum.QUOTE.getType());
        if (CollUtil.isNotEmpty(sourceIds)) {
            putSourceDocumentMeta(result, ErpSaleBizSourceTypeEnum.QUOTE.getType(),
                    saleQuoteMapper.selectBatchIds(sourceIds), ErpSaleQuoteDO::getId,
                    ErpSaleQuoteDO::getCreator, ErpSaleQuoteDO::getCreateTime,
                    ErpSaleQuoteDO::getFreightType);
        }
        sourceIds = getSourceIds(saleOuts, ErpSaleBizSourceTypeEnum.CART.getType());
        if (CollUtil.isNotEmpty(sourceIds)) {
            putSourceDocumentMeta(result, ErpSaleBizSourceTypeEnum.CART.getType(),
                    saleCartMapper.selectBatchIds(sourceIds), ErpSaleCartDO::getId,
                    ErpSaleCartDO::getCreator, ErpSaleCartDO::getCreateTime,
                    ErpSaleCartDO::getFreightType);
        }
        sourceIds = getSourceIds(saleOuts, ErpSaleBizSourceTypeEnum.PRICE_ADJUST.getType());
        if (CollUtil.isNotEmpty(sourceIds)) {
            putSourceDocumentMeta(result, ErpSaleBizSourceTypeEnum.PRICE_ADJUST.getType(),
                    salePriceAdjustMapper.selectBatchIds(sourceIds), ErpSalePriceAdjustDO::getId,
                    ErpSalePriceAdjustDO::getCreator, ErpSalePriceAdjustDO::getCreateTime);
        }
        sourceIds = getSourceIds(saleOuts, ErpSaleBizSourceTypeEnum.PURCHASE_IN.getType());
        if (CollUtil.isNotEmpty(sourceIds)) {
            putSourceDocumentMeta(result, ErpSaleBizSourceTypeEnum.PURCHASE_IN.getType(),
                    purchaseInMapper.selectBatchIds(sourceIds), ErpPurchaseInDO::getId,
                    ErpPurchaseInDO::getCreator, ErpPurchaseInDO::getCreateTime);
        }
        return result;
    }

    private Set<Long> getSourceIds(Collection<ErpSaleOutDO> saleOuts, Integer sourceType) {
        if (CollUtil.isEmpty(saleOuts)) {
            return Collections.emptySet();
        }
        return saleOuts.stream()
                .filter(saleOut -> Objects.equals(sourceType, saleOut.getSourceType()))
                .map(ErpSaleOutDO::getSourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private <T> void putSourceDocumentMeta(Map<Integer, Map<Long, SourceDocumentMeta>> result,
                                           Integer sourceType,
                                           Collection<T> documents,
                                           Function<T, Long> idGetter,
                                           Function<T, String> creatorGetter,
                                           Function<T, LocalDateTime> createTimeGetter) {
        putSourceDocumentMeta(result, sourceType, documents, idGetter, creatorGetter, createTimeGetter,
                document -> null);
    }

    private <T> void putSourceDocumentMeta(Map<Integer, Map<Long, SourceDocumentMeta>> result,
                                           Integer sourceType,
                                           Collection<T> documents,
                                           Function<T, Long> idGetter,
                                           Function<T, String> creatorGetter,
                                           Function<T, LocalDateTime> createTimeGetter,
                                           Function<T, String> freightTypeGetter) {
        if (CollUtil.isEmpty(documents)) {
            return;
        }
        result.put(sourceType, documents.stream().collect(Collectors.toMap(
                idGetter,
                document -> new SourceDocumentMeta(creatorGetter.apply(document), createTimeGetter.apply(document),
                        freightTypeGetter.apply(document)),
                (first, second) -> first)));
    }

    private SourceDocumentMeta getSourceDocumentMeta(
            Map<Integer, Map<Long, SourceDocumentMeta>> sourceDocumentMetaMap,
            ErpSaleOutDO saleOut) {
        return getSourceDocumentMeta(sourceDocumentMetaMap, saleOut.getSourceType(), saleOut.getSourceId());
    }

    private SourceDocumentMeta getSourceDocumentMeta(
            Map<Integer, Map<Long, SourceDocumentMeta>> sourceDocumentMetaMap,
            ErpSaleOutRespVO saleOut) {
        return getSourceDocumentMeta(sourceDocumentMetaMap, saleOut.getSourceType(), saleOut.getSourceId());
    }

    private SourceDocumentMeta getSourceDocumentMeta(
            Map<Integer, Map<Long, SourceDocumentMeta>> sourceDocumentMetaMap,
            Integer sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return null;
        }
        Map<Long, SourceDocumentMeta> sourceTypeMap = sourceDocumentMetaMap.get(sourceType);
        return sourceTypeMap != null ? sourceTypeMap.get(sourceId) : null;
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

    /**
     * 填充销售单主表的关联字段（客户编码、审核人、业务员、部门）
     */
    private void fillSaleOutRelationFields(ErpSaleOutRespVO respVO, ErpSaleOutDO saleOut) {
        SourceDocumentMeta sourceDocumentMeta = getSourceDocumentMeta(
                buildSourceDocumentMetaMap(Collections.singletonList(saleOut)), saleOut);
        if (sourceDocumentMeta != null && respVO.getSourceCreateTime() == null) {
            respVO.setSourceCreateTime(sourceDocumentMeta.createTime);
        }
        if (sourceDocumentMeta != null && respVO.getFreightType() == null) {
            respVO.setFreightType(sourceDocumentMeta.freightType);
        }
        // 客户
        if (saleOut.getCustomerId() != null) {
            ErpCustomerDO customer = customerService.getCustomer(saleOut.getCustomerId());
            if (customer != null) {
                respVO.setCustomerName(customer.getName());
                respVO.setCustomerCode(customer.getCode());
            }
        }
        // 用户信息（创建人、业务员、审核人）
        Set<Long> userIds = new HashSet<>();
        if (saleOut.getCreator() != null) {
            try { userIds.add(Long.parseLong(saleOut.getCreator())); } catch (NumberFormatException ignored) {}
        }
        if (saleOut.getUpdater() != null) {
            try { userIds.add(Long.parseLong(saleOut.getUpdater())); } catch (NumberFormatException ignored) {}
        }
        if (saleOut.getSaleUserId() != null) {
            userIds.add(saleOut.getSaleUserId());
        }
        if (saleOut.getAuditorId() != null) {
            userIds.add(saleOut.getAuditorId());
        }
        Long sourceCreatorId = sourceDocumentMeta != null ? parseUserId(sourceDocumentMeta.creator) : null;
        if (sourceCreatorId != null) {
            userIds.add(sourceCreatorId);
        }
        if (!userIds.isEmpty()) {
            Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
            if (saleOut.getCreator() != null) {
                try {
                    MapUtils.findAndThen(userMap, Long.parseLong(saleOut.getCreator()), user -> respVO.setCreatorName(user.getNickname()));
                } catch (NumberFormatException ignored) {}
            }
            if (saleOut.getUpdater() != null) {
                try {
                    MapUtils.findAndThen(userMap, Long.parseLong(saleOut.getUpdater()), user -> respVO.setUpdaterName(user.getNickname()));
                } catch (NumberFormatException ignored) {}
            }
            if (saleOut.getSaleUserId() != null) {
                MapUtils.findAndThen(userMap, saleOut.getSaleUserId(), user -> respVO.setSaleUserName(user.getNickname()));
            }
            if (saleOut.getAuditorId() != null) {
                MapUtils.findAndThen(userMap, saleOut.getAuditorId(), user -> respVO.setAuditorName(user.getNickname()));
            }
            if (sourceCreatorId != null) {
                MapUtils.findAndThen(userMap, sourceCreatorId,
                        user -> respVO.setSourceCreatorName(user.getNickname()));
            }
        }
        // 部门
        if (saleOut.getDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(saleOut.getDeptId());
            if (dept != null) {
                respVO.setDeptName(dept.getName());
            }
        }
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售出库分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:query')")
    public CommonResult<PageResult<ErpSaleOutRespVO>> getSaleOutPage(@Valid ErpSaleOutPageReqVO pageReqVO) {
        PageResult<ErpSaleOutDO> pageResult = saleOutService.getSaleOutPage(pageReqVO);
        PageResult<ErpSaleOutRespVO> respResult = buildSaleOutVOPageResult(pageResult,
                Boolean.TRUE.equals(pageReqVO.getReceiptEnable()));
        fieldPermissionMasker.maskSaleDetailFormsWithItems(FIELD_PERMISSION_MODULE, respResult.getList());
        return success(respResult);
    }

    @GetMapping("/returnable-items")
    @Operation(summary = "获取销售单的可退明细（按销售单退货使用）")
    @Parameter(name = "outId", description = "销售单 ID", required = true, example = "17386")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:create')")
    public CommonResult<List<ErpSaleReturnableItemRespVO>> getReturnableItems(@RequestParam("outId") Long outId) {
        List<ErpSaleReturnableItemRespVO> list = saleOutService.getReturnableItemsByOutId(outId);
        fieldPermissionMasker.maskSaleDetailExportRows(FIELD_PERMISSION_MODULE, list);
        return success(list);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出销售出库 Excel")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSaleOutExcel(@Valid ErpSaleOutPageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpSaleOutDO> pageResult = saleOutService.getSaleOutPage(pageReqVO);
        List<ErpSaleOutItemDO> saleOutItemList = saleOutService.getSaleOutItemListByOutIds(
                convertSet(pageResult.getList(), ErpSaleOutDO::getId));
        Map<Long, List<ErpSaleOutItemDO>> saleOutItemMap = convertMultiMap(saleOutItemList, ErpSaleOutItemDO::getOutId);
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(saleOutItemList, ErpSaleOutItemDO::getProductId));
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpSaleOutDO::getCustomerId));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(out -> {
            if (out.getCreator() != null) {
                try {
                    userIds.add(Long.parseLong(out.getCreator()));
                } catch (NumberFormatException ignored) {
                }
            }
            if (out.getSaleUserId() != null) {
                userIds.add(out.getSaleUserId());
            }
        });
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty() ? new HashMap<>() : adminUserApi.getUserMap(userIds);
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(saleOutItemList, ErpSaleOutItemDO::getWarehouseId));
        List<ErpSaleOutExportRespVO> rows = buildSaleOutExportList(
                pageResult.getList(), saleOutItemMap, productMap, customerMap, userMap, warehouseMap);
        fieldPermissionMasker.maskSaleDetailExportRows(FIELD_PERMISSION_MODULE, rows);
        ExcelUtils.write(response, "销售单.xls", "数据", ErpSaleOutExportRespVO.class, rows);
    }

    private PageResult<ErpSaleOutRespVO> buildSaleOutVOPageResult(PageResult<ErpSaleOutDO> pageResult,
                                                                  boolean useOriginalSettlementAmount) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        // 1.1 出库项
        List<ErpSaleOutItemDO> saleOutItemList = saleOutService.getSaleOutItemListByOutIds(
                convertSet(pageResult.getList(), ErpSaleOutDO::getId));
        Map<Long, List<ErpSaleOutItemDO>> saleOutItemMap = convertMultiMap(saleOutItemList, ErpSaleOutItemDO::getOutId);
        Map<Long, ErpSaleOutDO> saleOutMap = convertMap(pageResult.getList(), ErpSaleOutDO::getId);
        // 1.2 产品信息
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(saleOutItemList, ErpSaleOutItemDO::getProductId));
        // 1.3 客户信息
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpSaleOutDO::getCustomerId));
        Map<Integer, Map<Long, SourceDocumentMeta>> sourceDocumentMetaMap =
                buildSourceDocumentMetaMap(pageResult.getList());
        // 1.4 管理员信息（创建人 + 业务员 + 审核人 + 来源单制单人）
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(out -> {
            if (out.getCreator() != null) {
                try { userIds.add(Long.parseLong(out.getCreator())); } catch (NumberFormatException ignored) {}
            }
            if (out.getUpdater() != null) {
                try { userIds.add(Long.parseLong(out.getUpdater())); } catch (NumberFormatException ignored) {}
            }
            if (out.getSaleUserId() != null) {
                userIds.add(out.getSaleUserId());
            }
            if (out.getAuditorId() != null) {
                userIds.add(out.getAuditorId());
            }
            SourceDocumentMeta sourceDocumentMeta = getSourceDocumentMeta(sourceDocumentMetaMap, out);
            Long sourceCreatorId = sourceDocumentMeta != null ? parseUserId(sourceDocumentMeta.creator) : null;
            if (sourceCreatorId != null) {
                userIds.add(sourceCreatorId);
            }
        });
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isEmpty(userIds)
                ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpSaleOutDO::getDeptId));
        Set<Long> warehouseIds = convertSet(saleOutItemList, ErpSaleOutItemDO::getWarehouseId);
        warehouseIds.addAll(convertSet(saleOutItemList, ErpSaleOutItemDO::getSourceWarehouseId));
        warehouseIds.remove(null);
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(warehouseIds);
        Set<Long> itemDeptIds = convertSet(saleOutItemList, ErpSaleOutItemDO::getDeptId);
        itemDeptIds.addAll(convertSet(saleOutItemList, ErpSaleOutItemDO::getSourceDeptId));
        itemDeptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        itemDeptIds.remove(null);
        Map<Long, DeptRespDTO> itemDeptMap = CollUtil.isEmpty(itemDeptIds) ? Collections.emptyMap() : deptApi.getDeptMap(itemDeptIds);
        List<ErpStockOutBillDO> stockOutBills = stockOutBillService.getStockOutBillListBySaleOutIds(
                convertSet(pageResult.getList(), ErpSaleOutDO::getId));
        Map<Long, List<ErpStockOutBillDO>> stockOutBillMap = CollUtil.isEmpty(stockOutBills)
                ? Collections.emptyMap()
                : stockOutBills.stream().filter(bill -> bill.getSourceId() != null)
                .collect(Collectors.groupingBy(ErpStockOutBillDO::getSourceId));
        // 1.5 退货状态：按 sourceOutItemId 聚合已退数量
        Set<Long> allOutItemIds = convertSet(saleOutItemList, ErpSaleOutItemDO::getId);
        Map<Long, BigDecimal> returnedCountMap = saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(allOutItemIds);
        // 2. 开始拼接
        return BeanUtils.toBean(pageResult, ErpSaleOutRespVO.class, saleOut -> {
            List<ErpSaleOutItemDO> items = saleOutItemMap.getOrDefault(saleOut.getId(), Collections.emptyList());
            if (useOriginalSettlementAmount) {
                saleOut.setTotalPrice(ErpOriginalSettlementAmountUtils.calculateSaleOut(
                        saleOutMap.get(saleOut.getId()), items));
            }
            saleOut.setItems(BeanUtils.toBean(items, ErpSaleOutRespVO.Item.class,
                    item -> {
                        MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                                .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName()));
                        MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> {
                            item.setWarehouseName(warehouse.getName());
                            item.setWarehouseDeptId(warehouse.getDeptId());
                            MapUtils.findAndThen(itemDeptMap, warehouse.getDeptId(),
                                    dept -> item.setWarehouseDeptName(dept.getName()));
                        });
                        MapUtils.findAndThen(warehouseMap, item.getSourceWarehouseId(),
                                warehouse -> item.setSourceWarehouseName(warehouse.getName()));
                        MapUtils.findAndThen(itemDeptMap, item.getSourceDeptId(),
                                dept -> item.setSourceDeptName(dept.getName()));
                        item.setCrossDept(item.getSourceWarehouseId() != null
                                && !Objects.equals(item.getSourceWarehouseId(), item.getWarehouseId()));
                        MapUtils.findAndThen(itemDeptMap, item.getDeptId(), dept -> item.setDeptName(dept.getName()));
                    }));
            saleOut.setProductNames(CollUtil.join(saleOut.getItems(), "，", ErpSaleOutRespVO.Item::getProductName));
            MapUtils.findAndThen(customerMap, saleOut.getCustomerId(), customer -> {
                saleOut.setCustomerName(customer.getName());
                saleOut.setCustomerCode(customer.getCode());
            });
            if (saleOut.getCreator() != null) {
                try {
                    MapUtils.findAndThen(userMap, Long.parseLong(saleOut.getCreator()), user -> saleOut.setCreatorName(user.getNickname()));
                } catch (NumberFormatException ignored) {}
            }
            if (saleOut.getUpdater() != null) {
                try {
                    MapUtils.findAndThen(userMap, Long.parseLong(saleOut.getUpdater()), user -> saleOut.setUpdaterName(user.getNickname()));
                } catch (NumberFormatException ignored) {}
            }
            // 业务员名称
            if (saleOut.getSaleUserId() != null) {
                MapUtils.findAndThen(userMap, saleOut.getSaleUserId(), user -> saleOut.setSaleUserName(user.getNickname()));
            }
            if (saleOut.getAuditorId() != null) {
                MapUtils.findAndThen(userMap, saleOut.getAuditorId(), user -> saleOut.setAuditorName(user.getNickname()));
            }
            SourceDocumentMeta sourceDocumentMeta = getSourceDocumentMeta(sourceDocumentMetaMap, saleOut);
            if (sourceDocumentMeta != null) {
                if (saleOut.getSourceCreateTime() == null) {
                    saleOut.setSourceCreateTime(sourceDocumentMeta.createTime);
                }
                if (saleOut.getFreightType() == null) {
                    saleOut.setFreightType(sourceDocumentMeta.freightType);
                }
                Long sourceCreatorId = parseUserId(sourceDocumentMeta.creator);
                if (sourceCreatorId != null) {
                    MapUtils.findAndThen(userMap, sourceCreatorId,
                            user -> saleOut.setSourceCreatorName(user.getNickname()));
                }
            }
            MapUtils.findAndThen(deptMap, saleOut.getDeptId(), dept -> saleOut.setDeptName(dept.getName()));
            List<ErpStockOutBillDO> saleOutBills = stockOutBillMap.getOrDefault(
                    saleOut.getId(), Collections.emptyList());
            saleOut.setHasStockOutBill(CollUtil.isNotEmpty(saleOutBills));
            saleOut.setStockOutBills(toStockOutBillBriefs(saleOutBills));
            // 退货状态计算
            saleOut.setReturnStatus(calculateReturnStatus(saleOutItemMap.get(saleOut.getId()), returnedCountMap));
        });
    }

    /**
     * 计算退货状态：0=未退, 1=部分退, 2=整退
     */
    private Integer calculateReturnStatus(List<ErpSaleOutItemDO> items, Map<Long, BigDecimal> returnedCountMap) {
        if (CollUtil.isEmpty(items) || returnedCountMap.isEmpty()) {
            return 0;
        }
        boolean hasReturn = false;
        boolean allReturned = true;
        for (ErpSaleOutItemDO item : items) {
            BigDecimal returned = returnedCountMap.get(item.getId());
            if (returned != null && returned.compareTo(BigDecimal.ZERO) > 0) {
                hasReturn = true;
                if (returned.compareTo(item.getCount()) < 0) {
                    allReturned = false;
                }
            } else {
                allReturned = false;
            }
        }
        if (!hasReturn) {
            return 0;
        }
        return allReturned ? 2 : 1;
    }

    private void fillSaleOutStockOutBillInfo(ErpSaleOutRespVO respVO, Long saleOutId) {
        List<ErpStockOutBillDO> bills = stockOutBillService.getStockOutBillListBySaleOutId(saleOutId);
        respVO.setHasStockOutBill(CollUtil.isNotEmpty(bills));
        if (CollUtil.isEmpty(bills)) {
            respVO.setStockOutBills(Collections.emptyList());
            if (CollUtil.isNotEmpty(respVO.getItems())) {
                respVO.getItems().forEach(item -> item.setHasStockOutBill(false));
            }
            return;
        }
        Map<Long, ErpStockOutBillDO> billMap = bills.stream()
                .collect(Collectors.toMap(ErpStockOutBillDO::getId, bill -> bill, (first, second) -> first));
        respVO.setStockOutBills(toStockOutBillBriefs(bills));

        List<ErpStockOutBillItemDO> billItems = stockOutBillService.getSaleOutSourceItemList(saleOutId);
        Map<Long, List<ErpStockOutBillItemDO>> billItemMap = billItems.stream()
                .filter(item -> item.getSourceItemId() != null)
                .collect(Collectors.groupingBy(ErpStockOutBillItemDO::getSourceItemId));
        if (CollUtil.isEmpty(respVO.getItems())) {
            return;
        }
        respVO.getItems().forEach(item -> fillSaleOutItemStockOutBillInfo(item, billItemMap.get(item.getId()), billMap));
    }

    private List<ErpSaleOutRespVO.StockOutBillBrief> toStockOutBillBriefs(List<ErpStockOutBillDO> bills) {
        if (CollUtil.isEmpty(bills)) {
            return Collections.emptyList();
        }
        return bills.stream().map(bill -> {
            ErpSaleOutRespVO.StockOutBillBrief brief = BeanUtils.toBean(
                    bill, ErpSaleOutRespVO.StockOutBillBrief.class);
            brief.setStatusName(getStockOutBillStatusName(bill.getStatus()));
            return brief;
        }).collect(Collectors.toList());
    }

    private void fillSaleOutItemStockOutBillInfo(ErpSaleOutRespVO.Item item,
                                                 List<ErpStockOutBillItemDO> billItems,
                                                 Map<Long, ErpStockOutBillDO> billMap) {
        item.setHasStockOutBill(CollUtil.isNotEmpty(billItems));
        if (CollUtil.isEmpty(billItems)) {
            return;
        }
        item.setStockOutBillNos(billItems.stream()
                .map(billItem -> billMap.get(billItem.getBillId()))
                .filter(Objects::nonNull)
                .map(ErpStockOutBillDO::getNo)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(", ")));
        BigDecimal count = sumStockBillCount(billItems, ErpStockOutBillItemDO::getCount);
        BigDecimal pickedCount = sumStockBillCount(billItems, ErpStockOutBillItemDO::getPickedCount);
        Integer status = calculateStockBillStatus(billItems.stream()
                .map(ErpStockOutBillItemDO::getStatus)
                .collect(Collectors.toList()));
        item.setStockOutBillStatus(status);
        item.setStockOutBillStatusName(getStockOutBillStatusName(status));
        item.setStockOutBillCount(count);
        item.setStockOutBillPickedCount(pickedCount);
        item.setStockOutBillRemainCount(count.subtract(pickedCount));
    }

    private String getStockOutBillStatusName(Integer status) {
        if (Integer.valueOf(30).equals(status)) {
            return "已完成";
        }
        if (Integer.valueOf(20).equals(status)) {
            return "部分拣货";
        }
        if (Integer.valueOf(10).equals(status)) {
            return "待拣货";
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

    private List<ErpSaleOutExportRespVO> buildSaleOutExportList(List<ErpSaleOutDO> list,
                                                                Map<Long, List<ErpSaleOutItemDO>> saleOutItemMap,
                                                                Map<Long, ErpProductRespVO> productMap,
                                                                Map<Long, ErpCustomerDO> customerMap,
                                                                Map<Long, AdminUserRespDTO> userMap,
                                                                Map<Long, ErpWarehouseDO> warehouseMap) {
        List<ErpSaleOutExportRespVO> rows = new ArrayList<>();
        for (ErpSaleOutDO saleOut : list) {
            List<ErpSaleOutItemDO> items = saleOutItemMap.getOrDefault(saleOut.getId(), Collections.emptyList());
            if (CollUtil.isEmpty(items)) {
                rows.add(buildSaleOutExportRow(saleOut, customerMap.get(saleOut.getCustomerId()),
                        getCreator(userMap, saleOut.getCreator()), userMap.get(saleOut.getSaleUserId()),
                        null, null, null, true));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                ErpSaleOutItemDO item = items.get(i);
                rows.add(buildSaleOutExportRow(saleOut, customerMap.get(saleOut.getCustomerId()),
                        getCreator(userMap, saleOut.getCreator()), userMap.get(saleOut.getSaleUserId()),
                        item, productMap.get(item.getProductId()), warehouseMap.get(item.getWarehouseId()), i == 0));
            }
        }
        return rows;
    }

    private ErpSaleOutExportRespVO buildSaleOutExportRow(ErpSaleOutDO saleOut,
                                                         ErpCustomerDO customer,
                                                         AdminUserRespDTO creator,
                                                         AdminUserRespDTO saleUser,
                                                         ErpSaleOutItemDO item,
                                                         ErpProductRespVO product,
                                                         ErpWarehouseDO warehouse,
                                                         boolean fillMainFields) {
        ErpSaleOutExportRespVO row = fillMainFields
                ? BeanUtils.toBean(saleOut, ErpSaleOutExportRespVO.class)
                : new ErpSaleOutExportRespVO();
        row.setCustomerId(saleOut.getCustomerId());
        row.setCustomerName(fillMainFields && customer != null ? customer.getName() : null);
        row.setCreatorName(fillMainFields && creator != null ? creator.getNickname() : null);
        row.setSaleUserName(fillMainFields && saleUser != null ? saleUser.getNickname() : null);
        if (item == null) {
            return row;
        }
        row.setProductCode(product != null ? product.getCode() : null);
        row.setProductName(product != null ? product.getName() : null);
        row.setProductUnitName(product != null ? product.getUnitName() : null);
        row.setUnitWeight(item.getUnitWeight());
        row.setPackageQty(item.getPackageQty());
        row.setTotalWeight(item.getTotalWeight());
        row.setWarehouseName(warehouse != null ? warehouse.getName() : null);
        row.setItemCount(item.getCount());
        row.setProductPrice(item.getProductPrice());
        row.setItemTotalPrice(item.getTotalPrice());
        row.setItemTaxPercent(item.getTaxPercent());
        row.setItemTaxPrice(item.getTaxPrice());
        row.setBrand(item.getBrand());
        row.setVehicleModel(item.getVehicleModel());
        row.setStandard(item.getStandard());
        row.setOriginPlace(item.getOriginPlace());
        row.setWarehousePosition(item.getWarehousePosition());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private AdminUserRespDTO getCreator(Map<Long, AdminUserRespDTO> userMap, String creator) {
        if (creator == null) {
            return null;
        }
        try {
            return userMap.get(Long.parseLong(creator));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
