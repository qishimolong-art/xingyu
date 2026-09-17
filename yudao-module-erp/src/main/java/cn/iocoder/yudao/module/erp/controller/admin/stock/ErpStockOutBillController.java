package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPickReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 出仓单")
@RestController
@RequestMapping("/erp/stock-out-bill")
@Validated
public class ErpStockOutBillController {

    private static final int EXPORT_MAX_COUNT = 5000;
    private static final ErrorCode EXPORT_COUNT_EXCEEDED = new ErrorCode(1_030_590_007,
            "单次最多导出 5000 条出仓单，请缩小筛选范围后重试");
    private static final String FIELD_PERMISSION_MODULE = "erp_stock_out_bill";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();

    @Resource
    private ErpStockOutBillService stockOutBillService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpProductService productService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;

    @GetMapping("/page")
    @Operation(summary = "获得出仓单分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-out-bill:query')")
    public CommonResult<PageResult<ErpStockOutBillRespVO>> getStockOutBillPage(
            @Valid ErpStockOutBillPageReqVO pageReqVO) {
        return success(buildStockOutBillVOPageResult(stockOutBillService.getStockOutBillPage(pageReqVO)));
    }

    @GetMapping("/get")
    @Operation(summary = "获得出仓单")
    @PreAuthorize("@ss.hasPermission('erp:stock-out-bill:query')")
    public CommonResult<ErpStockOutBillRespVO> getStockOutBill(@RequestParam("id") Long id) {
        ErpStockOutBillDO stockOutBill = stockOutBillService.getStockOutBill(id);
        ErpStockOutBillRespVO respVO = BeanUtils.toBean(stockOutBill, ErpStockOutBillRespVO.class);
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(Collections.singleton(stockOutBill.getWarehouseId())));
        MapUtils.findAndThen(warehouseMap, respVO.getWarehouseId(),
                warehouse -> respVO.setWarehouseName(warehouse.getName()));
        fillUserNames(respVO, adminUserApi.getUserMap(collectUserIds(Collections.singletonList(stockOutBill))));
        return success(respVO);
    }

    @GetMapping("/items")
    @Operation(summary = "获得出仓单明细")
    @PreAuthorize("@ss.hasPermission('erp:stock-out-bill:query')")
    public CommonResult<List<ErpStockOutBillItemRespVO>> getStockOutBillItems(@RequestParam("id") Long id) {
        List<ErpStockOutBillItemDO> items = stockOutBillService.getStockOutBillItemList(id);
        return success(buildStockOutBillItemVOList(items));
    }

    @GetMapping("/item-page")
    @Operation(summary = "获得出仓单明细分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-out-bill:query')")
    public CommonResult<PageResult<ErpStockOutBillItemRespVO>> getStockOutBillItemPage(
            @Valid ErpStockOutBillItemPageReqVO pageReqVO) {
        PageResult<ErpStockOutBillItemDO> pageResult = stockOutBillService.getStockOutBillItemPage(pageReqVO);
        PageResult<ErpStockOutBillItemRespVO> respResult = new PageResult<>(
                buildStockOutBillItemVOList(pageResult.getList()), pageResult.getTotal());
        if (Boolean.TRUE.equals(pageReqVO.getMask())) {
            fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, respResult.getList());
        }
        return success(respResult);
    }

    @PutMapping("/pick")
    @Operation(summary = "出仓单拣货")
    @PreAuthorize("@ss.hasPermission('erp:stock-out-bill:pick')")
    public CommonResult<Boolean> pick(@Valid @RequestBody ErpStockOutBillPickReqVO reqVO) {
        stockOutBillService.pick(reqVO);
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出出仓单报表 Excel")
    @PreAuthorize("@ss.hasPermission('erp:stock-out-bill:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockOutBillExcel(@Valid ErpStockOutBillPageReqVO pageReqVO,
                                        @RequestParam(value = "fields", required = false) String fields,
                                        HttpServletResponse response) throws IOException {
        PageResult<ErpStockOutBillDO> pageResult;
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            if (pageReqVO.getIds().size() > EXPORT_MAX_COUNT) {
                throw exception(EXPORT_COUNT_EXCEEDED);
            }
            pageResult = new PageResult<>(stockOutBillService.getStockOutBillList(pageReqVO.getIds()),
                    (long) pageReqVO.getIds().size());
        } else {
            pageReqVO.setPageNo(1);
            pageReqVO.setPageSize(EXPORT_MAX_COUNT);
            pageResult = stockOutBillService.getStockOutBillPage(pageReqVO);
            if (pageResult.getTotal() > EXPORT_MAX_COUNT) {
                throw exception(EXPORT_COUNT_EXCEEDED);
            }
        }
        List<ErpStockOutBillItemDO> itemList = stockOutBillService.getStockOutBillItemListByBillIds(
                convertSet(pageResult.getList(), ErpStockOutBillDO::getId));
        Map<Long, List<ErpStockOutBillItemDO>> itemMap = convertMultiMap(itemList, ErpStockOutBillItemDO::getBillId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(itemList, ErpStockOutBillItemDO::getProductId)));
        Set<Long> warehouseIds = new HashSet<>();
        warehouseIds.addAll(convertSet(pageResult.getList(), ErpStockOutBillDO::getWarehouseId));
        warehouseIds.addAll(convertSet(itemList, ErpStockOutBillItemDO::getWarehouseId));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(warehouseIds));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(collectUserIds(pageResult.getList()));
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpStockOutBillExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "出仓单.xls", "数据", ErpStockOutBillExportRespVO.class,
                buildStockOutBillExportList(pageResult.getList(), itemMap, productMap, warehouseMap, userMap),
                includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "获得出仓单可导出字段")
    @PreAuthorize("@ss.hasPermission('erp:stock-out-bill:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getStockOutBillExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpStockOutBillExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获得出仓单搜索用户分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-out-bill:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        PageResult<AdminUserRespDTO> page = adminUserApi.getUserSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageReqVO.getKeyword(), pageReqVO);
        List<UserSimpleRespVO> list = convertList(page.getList(), user ->
                new UserSimpleRespVO(user.getId(), user.getNickname(), user.getDeptId(), null));
        return success(new PageResult<>(list, page.getTotal()));
    }

    private PageResult<ErpStockOutBillRespVO> buildStockOutBillVOPageResult(PageResult<ErpStockOutBillDO> pageResult) {
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(pageResult.getList()) ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(pageResult.getList(), ErpStockOutBillDO::getWarehouseId)));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(stockOutBill -> {
            addUserId(userIds, stockOutBill.getCreator());
            addUserId(userIds, stockOutBill.getUpdater());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        return BeanUtils.toBean(pageResult, ErpStockOutBillRespVO.class, vo -> {
            MapUtils.findAndThen(warehouseMap, vo.getWarehouseId(),
                    warehouse -> vo.setWarehouseName(warehouse.getName()));
            fillUserNames(vo, userMap);
        });
    }

    private Set<Long> collectUserIds(List<ErpStockOutBillDO> list) {
        Set<Long> userIds = new HashSet<>();
        list.forEach(stockOutBill -> {
            addUserId(userIds, stockOutBill.getCreator());
            addUserId(userIds, stockOutBill.getUpdater());
        });
        return userIds;
    }

    private java.math.BigDecimal nullToZero(java.math.BigDecimal value) {
        return value != null ? value : java.math.BigDecimal.ZERO;
    }

    private List<ErpStockOutBillItemRespVO> buildStockOutBillItemVOList(List<ErpStockOutBillItemDO> items) {
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(items) ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(items, ErpStockOutBillItemDO::getWarehouseId)));
        Map<Long, ErpProductRespVO> productMap = CollUtil.isEmpty(items) ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(items, ErpStockOutBillItemDO::getProductId)));
        return BeanUtils.toBean(items, ErpStockOutBillItemRespVO.class, vo -> {
            MapUtils.findAndThen(warehouseMap, vo.getWarehouseId(),
                    warehouse -> vo.setWarehouseName(warehouse.getName()));
            MapUtils.findAndThen(productMap, vo.getProductId(), product -> vo
                    .setProductCode(product.getCode())
                    .setProductName(product.getName())
                    .setProductUnitId(product.getUnitId())
                    .setProductUnitName(product.getUnitName()));
            vo.setRemainCount(nullToZero(vo.getCount()).subtract(nullToZero(vo.getPickedCount())));
        });
    }

    private void fillUserNames(ErpStockOutBillRespVO vo, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(vo.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> vo.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(vo.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> vo.setUpdaterName(user.getNickname()));
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
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<ErpStockOutBillExportRespVO> buildStockOutBillExportList(List<ErpStockOutBillDO> list,
                                                                          Map<Long, List<ErpStockOutBillItemDO>> itemMap,
                                                                          Map<Long, ErpProductRespVO> productMap,
                                                                          Map<Long, ErpWarehouseDO> warehouseMap,
                                                                          Map<Long, AdminUserRespDTO> userMap) {
        List<ErpStockOutBillExportRespVO> rows = new ArrayList<>();
        for (ErpStockOutBillDO bill : list) {
            List<ErpStockOutBillItemDO> items = itemMap.getOrDefault(bill.getId(), Collections.emptyList());
            if (CollUtil.isEmpty(items)) {
                rows.add(buildStockOutBillExportRow(bill, null, null, true, warehouseMap, userMap));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                ErpStockOutBillItemDO item = items.get(i);
                rows.add(buildStockOutBillExportRow(bill, item, productMap.get(item.getProductId()),
                        i == 0, warehouseMap, userMap));
            }
        }
        return rows;
    }

    private ErpStockOutBillExportRespVO buildStockOutBillExportRow(ErpStockOutBillDO bill,
                                                                   ErpStockOutBillItemDO item,
                                                                   ErpProductRespVO product,
                                                                   boolean fillMainFields,
                                                                   Map<Long, ErpWarehouseDO> warehouseMap,
                                                                   Map<Long, AdminUserRespDTO> userMap) {
        ErpStockOutBillExportRespVO row = fillMainFields
                ? BeanUtils.toBean(bill, ErpStockOutBillExportRespVO.class)
                : new ErpStockOutBillExportRespVO();
        if (fillMainFields) {
            MapUtils.findAndThen(warehouseMap, bill.getWarehouseId(),
                    warehouse -> row.setWarehouseName(warehouse.getName()));
            fillUserNames(row, bill, userMap);
        }
        if (item == null) {
            return row;
        }
        row.setItemSourceNo(item.getSourceNo());
        row.setProductCode(product != null ? product.getCode() : null);
        row.setProductName(product != null ? product.getName() : null);
        row.setProductUnitName(product != null ? product.getUnitName() : null);
        row.setPackageQty(item.getPackageQty());
        row.setWeight(item.getWeight());
        row.setItemTotalWeight(item.getTotalWeight());
        row.setItemWholeQty(item.getWholeQty());
        row.setItemCount(item.getCount());
        row.setItemPickedCount(item.getPickedCount());
        row.setItemRemainCount(nullToZero(item.getCount()).subtract(nullToZero(item.getPickedCount())));
        row.setProductPrice(item.getProductPrice());
        row.setItemTotalPrice(item.getProductPrice() == null || item.getCount() == null
                ? null : item.getProductPrice().multiply(item.getCount()));
        MapUtils.findAndThen(warehouseMap, item.getWarehouseId(),
                warehouse -> row.setItemWarehouseName(warehouse.getName()));
        row.setItemWarehousePosition(item.getWarehousePosition());
        row.setDrawingNo(item.getDrawingNo());
        row.setBatchNo(item.getBatchNo());
        row.setBarCode(item.getBarCode());
        row.setBrand(item.getBrand());
        row.setVehicleModel(item.getVehicleModel());
        row.setOriginPlace(item.getOriginPlace());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private void fillUserNames(ErpStockOutBillExportRespVO row, ErpStockOutBillDO bill,
                               Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(bill.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> row.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(bill.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> row.setUpdaterName(user.getNickname()));
        }
    }

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("priority", "main");
        map.put("no", "main");
        map.put("pick", "main");
        map.put("pickUserName", "main");
        map.put("billDate", "main");
        map.put("warehouseName", "main");
        map.put("shippingArea", "main");
        map.put("sourceUnitName", "main");
        map.put("sourceNo", "main");
        map.put("status", "main");
        map.put("creatorName", "system");
        map.put("createTime", "system");
        map.put("updaterName", "system");
        map.put("updateTime", "system");
        map.put("auditorName", "system");
        map.put("auditTime", "system");
        map.put("printTime", "system");
        map.put("printCount", "system");
        map.put("sourceRemark", "main");
        map.put("totalWeight", "main");
        map.put("remark", "main");
        map.put("timeoutFlag", "main");
        map.put("wholeQty", "main");
        map.put("looseQty", "main");
        map.put("totalCount", "main");
        map.put("pickedCount", "main");
        map.put("itemSourceNo", "detail");
        map.put("productCode", "detail");
        map.put("productName", "detail");
        map.put("productUnitName", "detail");
        map.put("packageQty", "detail");
        map.put("weight", "detail");
        map.put("itemTotalWeight", "detail");
        map.put("itemWholeQty", "detail");
        map.put("itemCount", "detail");
        map.put("itemPickedCount", "detail");
        map.put("itemRemainCount", "detail");
        map.put("productPrice", "detail");
        map.put("itemTotalPrice", "detail");
        map.put("itemWarehouseName", "detail");
        map.put("itemWarehousePosition", "detail");
        map.put("drawingNo", "detail");
        map.put("batchNo", "detail");
        map.put("barCode", "detail");
        map.put("brand", "detail");
        map.put("vehicleModel", "detail");
        map.put("originPlace", "detail");
        map.put("itemRemark", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("warehouseName", "warehouseId");
        map.put("itemSourceNo", "item_sourceNo");
        map.put("productCode", "item_productId");
        map.put("productName", "item_productId");
        map.put("productUnitName", "item_productUnitId");
        map.put("packageQty", "item_packageQty");
        map.put("weight", "item_weight");
        map.put("itemTotalWeight", "item_totalWeight");
        map.put("itemWholeQty", "item_wholeQty");
        map.put("itemCount", "item_count");
        map.put("itemPickedCount", "item_pickedCount");
        map.put("itemRemainCount", "item_count");
        map.put("productPrice", "item_productPrice");
        map.put("itemTotalPrice", "item_productPrice");
        map.put("itemWarehouseName", "item_warehouseId");
        map.put("itemWarehousePosition", "item_warehousePosition");
        map.put("drawingNo", "item_drawingNo");
        map.put("batchNo", "item_batchNo");
        map.put("barCode", "item_barCode");
        map.put("brand", "item_brand");
        map.put("vehicleModel", "item_vehicleModel");
        map.put("originPlace", "item_originPlace");
        map.put("itemRemark", "item_remark");
        return map;
    }

}
