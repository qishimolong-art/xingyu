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
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPickupReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillSummaryRespVO;
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
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
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

import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInBillItemMapper;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockItemPriceReferenceFiller;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import java.util.function.BiFunction;
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

    @Resource private ErpStockInBillItemMapper billItemMapper;
    @Resource private DeptApi deptApi;
    @Resource private ErpStockItemPriceReferenceFiller itemPriceReferenceFiller;

    @GetMapping("/page")
    @Operation(summary = "获得入仓单报表分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-in-bill:query')")
    public CommonResult<PageResult<ErpStockInBillRespVO>> getStockInBillPage(
            @Valid ErpStockInBillPageReqVO pageReqVO) {
        return success(buildStockInBillVOPageResult(stockInBillService.getStockInBillPage(pageReqVO)));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得入仓单查询汇总")
    @PreAuthorize("@ss.hasPermission('erp:stock-in-bill:query')")
    public CommonResult<ErpStockInBillSummaryRespVO> getStockInBillSummary(
            @Valid ErpStockInBillPageReqVO pageReqVO) {
        ErpStockInBillSummaryRespVO summary = stockInBillService.getStockInBillSummary(pageReqVO);
        fieldPermissionMasker.maskListColumns(FIELD_PERMISSION_MODULE, summary);
        ErpStockBillExportSupport.mask(summary, exportHidden());
        return success(summary);
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
        fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, respVO);
        ErpStockBillExportSupport.mask(respVO, exportHidden());
        return success(respVO);
    }

    @GetMapping("/items")
    @Operation(summary = "获得入仓单明细")
    @PreAuthorize("@ss.hasPermission('erp:stock-in-bill:query')")
    public CommonResult<List<ErpStockInBillItemRespVO>> getStockInBillItems(@RequestParam("id") Long id) {
        stockInBillService.getStockInBill(id);
        List<ErpStockInBillItemDO> items = stockInBillService.getStockInBillItemList(id);
        List<ErpStockInBillItemRespVO> result = buildStockInBillItemVOList(items);
        maskItems(result);
        return success(result);
    }

    @GetMapping("/item-page")
    @Operation(summary = "获得入仓单明细分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-in-bill:query')")
    public CommonResult<PageResult<ErpStockInBillItemRespVO>> getStockInBillItemPage(
            @Valid ErpStockInBillItemPageReqVO pageReqVO) {
        PageResult<ErpStockInBillItemDO> pageResult = stockInBillService.getStockInBillItemPage(pageReqVO);
        PageResult<ErpStockInBillItemRespVO> respResult = new PageResult<>(
                buildStockInBillItemVOList(pageResult.getList()), pageResult.getTotal());
        if (Boolean.TRUE.equals(pageReqVO.getMask())) {
            maskItems(respResult.getList());
        }
        return success(respResult);
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
        if (pageReqVO.getIds() != null && pageReqVO.getIds().size() > EXPORT_MAX_COUNT) throw exception(EXPORT_COUNT_EXCEEDED);
        Set<String> hidden = ErpStockBillExportSupport.hidden(fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        Set<String> legacyFields = new java.util.LinkedHashSet<>(EXPORT_FIELD_GROUP_MAP.keySet());
        legacyFields.removeAll(java.util.Arrays.asList("warehouseDeptName","remainingCount","itemWarehouseDeptName"));
        ErpStockBillExportSupport.write(response,"stock-in-bill",fields,ErpStockInBillExportRespVO.class,
                EXPORT_FIELD_GROUP_MAP,hidden,legacyFields,
                pageNo -> { pageReqVO.setPageNo(pageNo); pageReqVO.setPageSize(100); return stockInBillService.getStockInBillPage(pageReqVO); },
                ErpStockInBillDO::getId,(ids,pageNo) -> billItemMapper.selectExportPageByBillIds(ids,pageNo).getList(),
                ErpStockInBillItemDO::getBillId,this::prepareExportRows);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "获得入仓单可导出字段")
    @PreAuthorize("@ss.hasPermission('erp:stock-in-bill:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getStockInBillExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpStockInBillExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                ErpStockBillExportSupport.hidden(fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP), EXPORT_FIELD_PERMISSION_MAP));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获得入仓单搜索用户分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-in-bill:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        PageResult<AdminUserRespDTO> page = adminUserApi.getUserSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageReqVO.getKeyword(), pageReqVO);
        List<UserSimpleRespVO> list = convertList(page.getList(), user ->
                new UserSimpleRespVO(user.getId(), user.getNickname(), user.getDeptId(), null));
        return success(new PageResult<>(list, page.getTotal()));
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
        PageResult<ErpStockInBillRespVO> result = BeanUtils.toBean(pageResult, ErpStockInBillRespVO.class, vo -> {
            MapUtils.findAndThen(warehouseMap, vo.getWarehouseId(),
                    warehouse -> vo.setWarehouseName(warehouse.getName()));
            fillUserNames(vo, userMap);
        });
        fieldPermissionMasker.maskListColumns(FIELD_PERMISSION_MODULE,result.getList());
        result.getList().forEach(vo -> ErpStockBillExportSupport.mask(vo, exportHidden()));
        return result;
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

    private List<ErpStockInBillItemRespVO> buildStockInBillItemVOList(List<ErpStockInBillItemDO> items) {
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(items) ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(items, ErpStockInBillItemDO::getWarehouseId)));
        Map<Long, ErpProductRespVO> productMap = CollUtil.isEmpty(items) ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(items, ErpStockInBillItemDO::getProductId)));
        List<ErpStockInBillItemRespVO> result = BeanUtils.toBean(items, ErpStockInBillItemRespVO.class, vo -> {
            MapUtils.findAndThen(warehouseMap, vo.getWarehouseId(),
                    warehouse -> vo.setWarehouseName(warehouse.getName()));
            MapUtils.findAndThen(productMap, vo.getProductId(), product -> {
                vo.setProductCode(product.getCode());
                vo.setProductName(product.getName());
                vo.setProductUnitId(product.getUnitId());
                vo.setProductUnitName(product.getUnitName());
            });
            vo.setRemainCount(vo.getCount() == null || vo.getPickedCount() == null ? null : vo.getCount().subtract(vo.getPickedCount()));
        });
        itemPriceReferenceFiller.fill(result);
        return result;
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
        row.setItemRemainCount(item.getCount() == null || item.getPickedCount() == null ? null : item.getCount().subtract(item.getPickedCount()));
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


    private BiFunction<ErpStockInBillDO,ErpStockInBillItemDO,ErpStockInBillExportRespVO> prepareExportRows(List<ErpStockInBillDO> bills, List<ErpStockInBillItemDO> items) {
        Map<Long, ErpProductRespVO> productMap = items.isEmpty() ? Collections.emptyMap() : DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(items,ErpStockInBillItemDO::getProductId)));
        Set<Long> warehouseIds = new HashSet<>(convertSet(bills,ErpStockInBillDO::getWarehouseId));
        warehouseIds.addAll(convertSet(items,ErpStockInBillItemDO::getWarehouseId));
        Map<Long,ErpWarehouseDO> warehouses = warehouseIds.isEmpty() ? Collections.emptyMap() : DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds));
        Set<Long> deptIds = convertSet(new ArrayList<>(warehouses.values()),ErpWarehouseDO::getDeptId);
        Map<Long,DeptRespDTO> departments = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        Map<Long,AdminUserRespDTO> users = adminUserApi.getUserMap(collectUserIds(bills));
        return (bill,item) -> {
            ErpStockInBillExportRespVO row = buildStockInBillExportRow(bill,item,item == null ? null : productMap.get(item.getProductId()),true,warehouses,users);
            row.setRemainingCount(bill.getTotalCount() == null || bill.getPickedCount() == null ? null : bill.getTotalCount().subtract(bill.getPickedCount()).max(java.math.BigDecimal.ZERO));
            ErpWarehouseDO warehouse = warehouses.get(bill.getWarehouseId());
            if (warehouse != null && departments.get(warehouse.getDeptId()) != null) row.setWarehouseDeptName(departments.get(warehouse.getDeptId()).getName());
            ErpWarehouseDO itemWarehouse = item == null ? null : warehouses.get(item.getWarehouseId());
            if (itemWarehouse != null && departments.get(itemWarehouse.getDeptId()) != null) row.setItemWarehouseDeptName(departments.get(itemWarehouse.getDeptId()).getName());
            return row;
        };
    }

    private Set<String> exportHidden() {
        return ErpStockBillExportSupport.hidden(fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE),EXPORT_FIELD_PERMISSION_MAP);
    }

    private void maskItems(List<ErpStockInBillItemRespVO> items) {
        fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE,items);
        Set<String> hidden = ErpStockBillExportSupport.hidden(fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE),EXPORT_FIELD_PERMISSION_MAP);
        items.forEach(item -> {
            ErpStockBillExportSupport.maskItem(item,hidden);
            if (hidden.contains("itemRemainCount")) item.setRemainCount(null);
            if (hidden.contains("productPrice")) item.setProductPrice(null);
            if (hidden.contains("itemCount")) item.setCount(null);
            if (hidden.contains("itemPickedCount")) item.setPickedCount(null);
        });
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
        map.put("creatorName", "main");
        map.put("createTime", "main");
        map.put("updaterName", "main");
        map.put("updateTime", "main");
        map.put("auditorName", "main");
        map.put("auditTime", "main");
        map.put("printTime", "main");
        map.put("printCount", "main");
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
        map.put("warehouseDeptName", "main");
        map.put("remainingCount", "main");
        map.put("itemWarehouseDeptName", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("warehouseName", "warehouseId");
        map.put("warehouseDeptName", "warehouseId");
        map.put("itemWarehouseDeptName", "item_warehouseId");
        map.put("creatorName", "creator");
        map.put("updaterName", "updater");
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
