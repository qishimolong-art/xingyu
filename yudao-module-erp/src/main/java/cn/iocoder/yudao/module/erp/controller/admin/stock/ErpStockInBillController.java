package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPickupReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 入仓单报表")
@RestController
@RequestMapping("/erp/stock-in-bill")
@Validated
public class ErpStockInBillController {

    private static final int EXPORT_MAX_COUNT = 5000;
    private static final ErrorCode EXPORT_COUNT_EXCEEDED = new ErrorCode(1_030_590_006,
            "单次最多导出 5000 条入仓单，请缩小筛选范围后重试");
    private static final String FIELD_PERMISSION_MODULE = "erp_stock_in_bill";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();

    @Resource
    private ErpStockInBillService stockInBillService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpProductService productService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;

    @GetMapping("/page")
    @Operation(summary = "获得入仓单报表分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-in-bill:query')")
    public CommonResult<PageResult<ErpStockInBillRespVO>> getStockInBillPage(
            @Valid ErpStockInBillPageReqVO pageReqVO) {
        return success(buildStockInBillVOPageResult(stockInBillService.getStockInBillPage(pageReqVO)));
    }

    @GetMapping("/get")
    @Operation(summary = "获得入仓单")
    @PreAuthorize("@ss.hasPermission('erp:stock-in-bill:query')")
    public CommonResult<ErpStockInBillRespVO> getStockInBill(@RequestParam("id") Long id) {
        ErpStockInBillDO stockInBill = stockInBillService.getStockInBill(id);
        ErpStockInBillRespVO respVO = BeanUtils.toBean(stockInBill, ErpStockInBillRespVO.class);
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(Collections.singleton(stockInBill.getWarehouseId())));
        MapUtils.findAndThen(warehouseMap, respVO.getWarehouseId(),
                warehouse -> respVO.setWarehouseName(warehouse.getName()));
        fillUserNames(respVO, adminUserApi.getUserMap(collectUserIds(Collections.singletonList(stockInBill))));
        return success(respVO);
    }

    @GetMapping("/items")
    @Operation(summary = "获得入仓单明细")
    @PreAuthorize("@ss.hasPermission('erp:stock-in-bill:query')")
    public CommonResult<List<ErpStockInBillItemRespVO>> getStockInBillItems(@RequestParam("id") Long id) {
        List<ErpStockInBillItemDO> items = stockInBillService.getStockInBillItemList(id);
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(items) ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(items, ErpStockInBillItemDO::getWarehouseId)));
        Map<Long, ErpProductRespVO> productMap = CollUtil.isEmpty(items) ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(items, ErpStockInBillItemDO::getProductId)));
        return success(BeanUtils.toBean(items, ErpStockInBillItemRespVO.class, vo -> {
            MapUtils.findAndThen(warehouseMap, vo.getWarehouseId(),
                    warehouse -> vo.setWarehouseName(warehouse.getName()));
            MapUtils.findAndThen(productMap, vo.getProductId(), product -> {
                vo.setProductCode(product.getCode());
                vo.setProductName(product.getName());
                vo.setProductUnitId(product.getUnitId());
                vo.setProductUnitName(product.getUnitName());
            });
            vo.setRemainCount(nullToZero(vo.getCount()).subtract(nullToZero(vo.getPickedCount())));
        }));
    }

    @PutMapping("/pickup")
    @Operation(summary = "入仓单提货")
    @PreAuthorize("@ss.hasPermission('erp:stock-in-bill:pickup')")
    public CommonResult<Boolean> pickup(@Valid @RequestBody ErpStockInBillPickupReqVO reqVO) {
        stockInBillService.pickup(reqVO);
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出入仓单报表 Excel")
    @PreAuthorize("@ss.hasPermission('erp:stock-in-bill:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockInBillExcel(@Valid ErpStockInBillPageReqVO pageReqVO,
                                       @RequestParam(value = "fields", required = false) String fields,
                                       HttpServletResponse response) throws IOException {
        PageResult<ErpStockInBillDO> pageResult;
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            if (pageReqVO.getIds().size() > EXPORT_MAX_COUNT) {
                throw exception(EXPORT_COUNT_EXCEEDED);
            }
            pageResult = new PageResult<>(stockInBillService.getStockInBillList(pageReqVO.getIds()),
                    (long) pageReqVO.getIds().size());
        } else {
            pageReqVO.setPageNo(1);
            pageReqVO.setPageSize(EXPORT_MAX_COUNT);
            pageResult = stockInBillService.getStockInBillPage(pageReqVO);
            if (pageResult.getTotal() > EXPORT_MAX_COUNT) {
                throw exception(EXPORT_COUNT_EXCEEDED);
            }
        }
        List<ErpStockInBillItemDO> itemList = stockInBillService.getStockInBillItemListByBillIds(
                convertSet(pageResult.getList(), ErpStockInBillDO::getId));
        Map<Long, List<ErpStockInBillItemDO>> itemMap = convertMultiMap(itemList, ErpStockInBillItemDO::getBillId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(itemList, ErpStockInBillItemDO::getProductId)));
        Set<Long> warehouseIds = new HashSet<>();
        warehouseIds.addAll(convertSet(pageResult.getList(), ErpStockInBillDO::getWarehouseId));
        warehouseIds.addAll(convertSet(itemList, ErpStockInBillItemDO::getWarehouseId));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(warehouseIds));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(collectUserIds(pageResult.getList()));
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpStockInBillExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "入仓单.xls", "数据", ErpStockInBillExportRespVO.class,
                buildStockInBillExportList(pageResult.getList(), itemMap, productMap, warehouseMap, userMap),
                includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "获得入仓单可导出字段")
    @PreAuthorize("@ss.hasPermission('erp:stock-in-bill:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getStockInBillExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpStockInBillExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    private PageResult<ErpStockInBillRespVO> buildStockInBillVOPageResult(PageResult<ErpStockInBillDO> pageResult) {
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(pageResult.getList()) ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(pageResult.getList(), ErpStockInBillDO::getWarehouseId)));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(stockInBill -> {
            addUserId(userIds, stockInBill.getCreator());
            addUserId(userIds, stockInBill.getUpdater());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        return BeanUtils.toBean(pageResult, ErpStockInBillRespVO.class, vo -> {
            MapUtils.findAndThen(warehouseMap, vo.getWarehouseId(),
                    warehouse -> vo.setWarehouseName(warehouse.getName()));
            fillUserNames(vo, userMap);
        });
    }

    private Set<Long> collectUserIds(List<ErpStockInBillDO> list) {
        Set<Long> userIds = new HashSet<>();
        list.forEach(stockInBill -> {
            addUserId(userIds, stockInBill.getCreator());
            addUserId(userIds, stockInBill.getUpdater());
        });
        return userIds;
    }

    private java.math.BigDecimal nullToZero(java.math.BigDecimal value) {
        return value != null ? value : java.math.BigDecimal.ZERO;
    }

    private void fillUserNames(ErpStockInBillRespVO vo, Map<Long, AdminUserRespDTO> userMap) {
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

    private List<ErpStockInBillExportRespVO> buildStockInBillExportList(List<ErpStockInBillDO> list,
                                                                        Map<Long, List<ErpStockInBillItemDO>> itemMap,
                                                                        Map<Long, ErpProductRespVO> productMap,
                                                                        Map<Long, ErpWarehouseDO> warehouseMap,
                                                                        Map<Long, AdminUserRespDTO> userMap) {
        List<ErpStockInBillExportRespVO> rows = new ArrayList<>();
        for (ErpStockInBillDO bill : list) {
            List<ErpStockInBillItemDO> items = itemMap.getOrDefault(bill.getId(), Collections.emptyList());
            if (CollUtil.isEmpty(items)) {
                rows.add(buildStockInBillExportRow(bill, null, null, true, warehouseMap, userMap));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                ErpStockInBillItemDO item = items.get(i);
                rows.add(buildStockInBillExportRow(bill, item, productMap.get(item.getProductId()),
                        i == 0, warehouseMap, userMap));
            }
        }
        return rows;
    }

    private ErpStockInBillExportRespVO buildStockInBillExportRow(ErpStockInBillDO bill,
                                                                 ErpStockInBillItemDO item,
                                                                 ErpProductRespVO product,
                                                                 boolean fillMainFields,
                                                                 Map<Long, ErpWarehouseDO> warehouseMap,
                                                                 Map<Long, AdminUserRespDTO> userMap) {
        ErpStockInBillExportRespVO row = fillMainFields
                ? BeanUtils.toBean(bill, ErpStockInBillExportRespVO.class)
                : new ErpStockInBillExportRespVO();
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

    private void fillUserNames(ErpStockInBillExportRespVO row, ErpStockInBillDO bill,
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
        map.put("pickup", "main");
        map.put("pickupUserName", "main");
        map.put("billDate", "main");
        map.put("warehouseName", "main");
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
