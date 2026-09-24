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
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleItemPriceReferenceFiller;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSetByFlatMap;

@Tag(name = "管理后台 - ERP 销售订单")
@RestController
@RequestMapping("/erp/sale-order")
@Validated
public class ErpSaleOrderController {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_order";
    private static final Map<String, String> DETAIL_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "productId", "productIdentity",
            "productCode", "productIdentity",
            "productName", "productIdentity",
            "factoryCode", "productIdentity",
            "count", "count",
            "itemCount", "count",
            "productPrice", "productPrice",
            "taxPercent", "taxPercent",
            "remark", "remark");

    @Resource
    private ErpSaleOrderService saleOrderService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpCustomerService customerService;
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
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpStockRecordMapper stockRecordMapper;
    @Resource
    private ErpCustomerDeptPermissionService customerDeptPermissionService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建销售订单")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:create')")
    public CommonResult<Long> createSaleOrder(@Valid @RequestBody ErpSaleOrderSaveReqVO createReqVO) {
        return success(saleOrderService.createSaleOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新销售订单")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:update')")
    public CommonResult<Boolean> updateSaleOrder(@Valid @RequestBody ErpSaleOrderSaveReqVO updateReqVO) {
        saleOrderService.updateSaleOrder(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "修改销售订单备注")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:update')")
    public CommonResult<Boolean> updateSaleOrderRemark(
            @Valid @RequestBody ErpSaleUpdateRemarkReqVO updateReqVO) {
        saleOrderService.updateSaleOrderRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新销售订单的状态")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:update-status')")
    public CommonResult<Boolean> updateSaleOrderStatus(@RequestParam("id") Long id,
                                                      @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        saleOrderService.updateSaleOrderStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除销售订单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-order:delete')")
    public CommonResult<Boolean> deleteSaleOrder(@RequestParam("ids") List<Long> ids) {
        saleOrderService.deleteSaleOrder(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售订单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:query')")
    public CommonResult<ErpSaleOrderRespVO> getSaleOrder(@RequestParam("id") Long id,
                                                         @RequestParam(value = "includeItems", required = false,
                                                                 defaultValue = "true") Boolean includeItems) {
        ErpSaleOrderDO saleOrder = saleOrderService.getSaleOrder(id);
        if (saleOrder == null) {
            return success(null);
        }
        List<ErpSaleOrderItemDO> saleOrderItemList = Boolean.TRUE.equals(includeItems)
                ? saleOrderService.getSaleOrderItemListByOrderId(id) : Collections.emptyList();
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(saleOrderItemList, ErpSaleOrderItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(
                convertSet(saleOrderItemList, ErpSaleOrderItemDO::getWarehouseId));
        Set<Long> userIds = convertUserIds(Collections.singletonList(saleOrder));
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isEmpty(userIds) ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        DeptRespDTO dept = saleOrder.getDeptId() == null ? null : deptApi.getDept(saleOrder.getDeptId());
        ErpCustomerDO customer = saleOrder.getCustomerId() == null
                ? null : customerService.getCustomer(saleOrder.getCustomerId());
        Set<Long> deptIds = convertSet(saleOrderItemList, ErpSaleOrderItemDO::getDeptId);
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> itemDeptMap = CollUtil.isEmpty(deptIds) ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        Map<Long, SaleOrderItemCost> itemCostMap = getSaleOrderItemCostMap(
                convertSet(saleOrderItemList, ErpSaleOrderItemDO::getId));
        ErpSaleOrderRespVO respVO = BeanUtils.toBean(saleOrder, ErpSaleOrderRespVO.class, saleOrderVO -> {
                fillAuditNames(saleOrderVO, userMap);
                if (dept != null) {
                    saleOrderVO.setDeptName(dept.getName());
                }
                if (customer != null) {
                    saleOrderVO.setCustomerName(customer.getName());
                }
                saleOrderVO.setItems(BeanUtils.toBean(saleOrderItemList, ErpSaleOrderRespVO.Item.class, item -> {
                    BigDecimal stockCount = getStockCountIgnoreDataPermission(item.getProductId());
                    item.setStockCount(stockCount != null ? stockCount : BigDecimal.ZERO);
                    MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                            .setProductCode(product.getCode()));
                    MapUtils.findAndThen(warehouseMap, item.getWarehouseId(),
                            warehouse -> {
                                item.setWarehouseName(warehouse.getName());
                                item.setWarehouseDeptId(warehouse.getDeptId());
                                MapUtils.findAndThen(itemDeptMap, warehouse.getDeptId(),
                                        deptResp -> item.setWarehouseDeptName(deptResp.getName()));
                            });
                    fillSaleOrderItemCost(item, itemCostMap);
                }));
        });
        itemPriceReferenceFiller.fill(respVO.getItems());
        fieldPermissionMasker.maskSaleDetailFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @PreAuthorize("@ss.hasPermission('erp:sale-order:query')")
    public CommonResult<ErpSaleOrderRespVO> getSaleOrder(Long id) {
        return getSaleOrder(id, true);
    }

    @GetMapping("/item-page")
    @Operation(summary = "获得销售订单明细分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:query')")
    public CommonResult<PageResult<ErpSaleOrderRespVO.Item>> getSaleOrderItemPage(
            @Valid ErpSaleOrderItemPageReqVO pageReqVO) {
        ErpSaleOrderDO saleOrder = saleOrderService.getSaleOrder(pageReqVO.getOrderId());
        if (saleOrder == null) {
            return success(PageResult.empty());
        }
        PageResult<ErpSaleOrderItemDO> pageResult = saleOrderService.getSaleOrderItemPage(pageReqVO);
        List<ErpSaleOrderItemDO> itemList = pageResult.getList();
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(itemList, ErpSaleOrderItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(
                convertSet(itemList, ErpSaleOrderItemDO::getWarehouseId));
        Set<Long> deptIds = convertSet(itemList, ErpSaleOrderItemDO::getDeptId);
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> itemDeptMap = CollUtil.isEmpty(deptIds)
                ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        Map<Long, BigDecimal> stockCountMap = getStockCountMapIgnoreDataPermission(
                convertSet(itemList, ErpSaleOrderItemDO::getProductId));
        Map<Long, SaleOrderItemCost> itemCostMap = getSaleOrderItemCostMap(
                convertSet(itemList, ErpSaleOrderItemDO::getId));
        List<ErpSaleOrderRespVO.Item> items = BeanUtils.toBean(itemList, ErpSaleOrderRespVO.Item.class, item -> {
            BigDecimal stockCount = stockCountMap.get(item.getProductId());
            item.setStockCount(stockCount != null ? stockCount : BigDecimal.ZERO);
            MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                    .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                    .setProductCode(product.getCode()));
            MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> {
                item.setWarehouseName(warehouse.getName());
                item.setWarehouseDeptId(warehouse.getDeptId());
                MapUtils.findAndThen(itemDeptMap, warehouse.getDeptId(),
                        deptResp -> item.setWarehouseDeptName(deptResp.getName()));
            });
            fillSaleOrderItemCost(item, itemCostMap);
        });
        itemPriceReferenceFiller.fill(items);
        PageResult<ErpSaleOrderRespVO.Item> respResult = new PageResult<>(items, pageResult.getTotal());
        if (Boolean.TRUE.equals(pageReqVO.getMask())) {
            ErpSaleOrderRespVO context = BeanUtils.toBean(saleOrder, ErpSaleOrderRespVO.class);
            fieldPermissionMasker.clearSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, context,
                    respResult.getList());
        }
        return success(respResult);
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "获取销售订单可见部门精简分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getVisibleDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage(FIELD_PERMISSION_MODULE, pageReqVO));
    }

    @GetMapping("/customer-dept-simple-page")
    @Operation(summary = "获取销售订单客户可用部门分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getCustomerAvailableDeptSimplePage(
            @RequestParam("customerId") Long customerId, @Valid PageParam pageReqVO) {
        return success(customerDeptPermissionService.getAvailableDeptSimplePage(customerId, FIELD_PERMISSION_MODULE, pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获取销售订单用户精简分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        return success(buildUserSimplePage(pageReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售订单分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:query')")
    public CommonResult<PageResult<ErpSaleOrderRespVO>> getSaleOrderPage(@Valid ErpSaleOrderPageReqVO pageReqVO) {
        PageResult<ErpSaleOrderDO> pageResult = saleOrderService.getSaleOrderPage(pageReqVO);
        PageResult<ErpSaleOrderRespVO> respResult = Boolean.FALSE.equals(pageReqVO.getIncludeItems())
                ? buildSaleOrderVOPageResultWithoutItems(pageResult)
                : buildSaleOrderVOPageResult(pageResult);
        fieldPermissionMasker.maskSaleDetailFormsWithItems(FIELD_PERMISSION_MODULE, respResult.getList());
        return success(respResult);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出销售订单 Excel")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSaleOrderExcel(@Valid ErpSaleOrderPageReqVO pageReqVO,
                                    HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpSaleOrderRespVO> list = buildSaleOrderVOPageResult(saleOrderService.getSaleOrderPage(pageReqVO)).getList();
        fieldPermissionMasker.maskSaleDetailFormsWithItems(FIELD_PERMISSION_MODULE, list);
        List<ErpSaleOrderExportRespVO> rows = buildSaleOrderExportList(list);
        fieldPermissionMasker.maskSaleDetailExportRows(FIELD_PERMISSION_MODULE, rows);
        ExcelUtils.write(response, "销售订单.xls", "数据", ErpSaleOrderExportRespVO.class, rows);
    }

    @GetMapping("/export-import-template")
    @Operation(summary = "获得销售订单导入模板")
    public void exportImportTemplate(HttpServletResponse response) throws IOException {
        ErpSaleOrderImportExcelVO example = new ErpSaleOrderImportExcelVO();
        example.setProductCode("P0001");
        example.setProductName("示例配件");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("100.00"));
        example.setTaxPercent(BigDecimal.ZERO);
        example.setRemark("备注");
        ExcelUtils.writeImportTemplate(response, "销售订单导入模板.xls", "销售订单",
                ErpSaleOrderImportExcelVO.class, Collections.singletonList(example), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.SALE_ORDER, ErpSaleOrderImportExcelVO.class,
                        DETAIL_IMPORT_FIELD_ALIAS_MAP));
    }

    @PostMapping("/import")
    @Operation(summary = "导入销售订单明细")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:create')")
    public CommonResult<ErpSaleOrderImportRespVO> importSaleOrder(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpSaleOrderImportExcelVO> list = ExcelUtils.read(file, ErpSaleOrderImportExcelVO.class);
        return success(saleOrderService.parseImportData(list));
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "下载销售订单导入错误数据")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:create')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, FIELD_PERMISSION_MODULE, response);
    }

    private PageResult<ErpSaleOrderRespVO> buildSaleOrderVOPageResult(PageResult<ErpSaleOrderDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        // 1.1 订单项
        List<ErpSaleOrderItemDO> saleOrderItemList = saleOrderService.getSaleOrderItemListByOrderIds(
                convertSet(pageResult.getList(), ErpSaleOrderDO::getId));
        Map<Long, List<ErpSaleOrderItemDO>> saleOrderItemMap = convertMultiMap(saleOrderItemList, ErpSaleOrderItemDO::getOrderId);
        // 1.2 产品信息
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(saleOrderItemList, ErpSaleOrderItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(
                convertSet(saleOrderItemList, ErpSaleOrderItemDO::getWarehouseId));
        Map<Long, SaleOrderItemCost> itemCostMap = getSaleOrderItemCostMap(
                convertSet(saleOrderItemList, ErpSaleOrderItemDO::getId));
        // 1.3 客户信息
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpSaleOrderDO::getCustomerId));
        // 1.4 管理员信息
        Set<Long> userIds = convertUserIds(pageResult.getList());
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isEmpty(userIds) ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        Set<Long> deptIds = convertSet(pageResult.getList(), ErpSaleOrderDO::getDeptId);
        deptIds.addAll(convertSet(saleOrderItemList, ErpSaleOrderItemDO::getDeptId));
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds) ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        // 2. 开始拼接
        return BeanUtils.toBean(pageResult, ErpSaleOrderRespVO.class, saleOrder -> {
            saleOrder.setItems(BeanUtils.toBean(saleOrderItemMap.get(saleOrder.getId()), ErpSaleOrderRespVO.Item.class,
                    item -> {
                        MapUtils.findAndThen(productMap, item.getProductId(),
                                product -> item.setProductName(product.getName())
                                        .setProductBarCode(product.getBarCode())
                                        .setProductUnitName(product.getUnitName())
                                        .setProductCode(product.getCode()));
                    fillSaleOrderItemCost(item, itemCostMap);
                    }));
            saleOrder.getItems().forEach(item ->
                    MapUtils.findAndThen(warehouseMap, item.getWarehouseId(),
                            warehouse -> {
                                item.setWarehouseName(warehouse.getName());
                                item.setWarehouseDeptId(warehouse.getDeptId());
                                MapUtils.findAndThen(deptMap, warehouse.getDeptId(),
                                        dept -> item.setWarehouseDeptName(dept.getName()));
                            }));
            itemPriceReferenceFiller.fill(saleOrder.getItems());
            saleOrder.setProductNames(CollUtil.join(saleOrder.getItems(), "，", ErpSaleOrderRespVO.Item::getProductName));
            MapUtils.findAndThen(customerMap, saleOrder.getCustomerId(), supplier -> saleOrder.setCustomerName(supplier.getName()));
            fillAuditNames(saleOrder, userMap);
            MapUtils.findAndThen(deptMap, saleOrder.getDeptId(), dept -> saleOrder.setDeptName(dept.getName()));
        });
    }

    private Map<Long, SaleOrderItemCost> getSaleOrderItemCostMap(Collection<Long> orderItemIds) {
        if (CollUtil.isEmpty(orderItemIds) || saleOutItemMapper == null || stockRecordMapper == null) {
            return Collections.emptyMap();
        }
        List<ErpSaleOutItemDO> outItems = saleOutItemMapper.selectList(new QueryWrapper<ErpSaleOutItemDO>()
                .select("id", "order_item_id")
                .in("order_item_id", orderItemIds)
                .isNotNull("order_item_id"));
        if (CollUtil.isEmpty(outItems)) {
            return Collections.emptyMap();
        }
        Map<Long, Long> outItemOrderItemMap = new HashMap<>();
        for (ErpSaleOutItemDO outItem : outItems) {
            outItemOrderItemMap.put(outItem.getId(), outItem.getOrderItemId());
        }
        List<ErpStockRecordDO> records = stockRecordMapper.selectList(new QueryWrapper<ErpStockRecordDO>()
                .select("id", "biz_item_id", "count", "total_price")
                .eq("biz_type", ErpStockRecordBizTypeEnum.SALE_OUT.getType())
                .in("biz_item_id", outItemOrderItemMap.keySet())
                .isNotNull("biz_item_id")
                .orderByAsc("id"));
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyMap();
        }
        Map<Long, SaleOrderItemCost> result = new HashMap<>();
        for (ErpStockRecordDO record : records) {
            Long orderItemId = outItemOrderItemMap.get(record.getBizItemId());
            if (orderItemId == null) {
                continue;
            }
            SaleOrderItemCost cost = result.computeIfAbsent(orderItemId, ignored -> new SaleOrderItemCost());
            cost.add(abs(record.getTotalPrice()), abs(record.getCount()));
        }
        return result;
    }

    private void fillSaleOrderItemCost(ErpSaleOrderRespVO.Item item,
                                       Map<Long, SaleOrderItemCost> itemCostMap) {
        SaleOrderItemCost cost = itemCostMap.get(item.getId());
        if (cost == null) {
            return;
        }
        item.setSaleCostAmount(cost.getAmount());
        if (cost.getCount().compareTo(BigDecimal.ZERO) > 0) {
            item.setSaleCostPrice(cost.getAmount().divide(cost.getCount(), 2, RoundingMode.HALF_UP));
        }
    }

    private BigDecimal abs(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.abs();
    }

    private static final class SaleOrderItemCost {

        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal count = BigDecimal.ZERO;

        void add(BigDecimal amount, BigDecimal count) {
            this.amount = this.amount.add(amount);
            this.count = this.count.add(count);
        }

        BigDecimal getAmount() {
            return amount;
        }

        BigDecimal getCount() {
            return count;
        }

    }

    private PageResult<ErpSaleOrderRespVO> buildSaleOrderVOPageResultWithoutItems(PageResult<ErpSaleOrderDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Set<Long> orderIds = convertSet(pageResult.getList(), ErpSaleOrderDO::getId);
        Map<Long, String> productNamesMap = saleOrderItemMapper.selectProductNamesMapByOrderIds(orderIds);
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpSaleOrderDO::getCustomerId));
        Set<Long> userIds = convertUserIds(pageResult.getList());
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isEmpty(userIds)
                ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        Set<Long> deptIds = convertSet(pageResult.getList(), ErpSaleOrderDO::getDeptId);
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds)
                ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        return BeanUtils.toBean(pageResult, ErpSaleOrderRespVO.class, saleOrder -> {
            saleOrder.setProductNames(productNamesMap.get(saleOrder.getId()));
            MapUtils.findAndThen(customerMap, saleOrder.getCustomerId(),
                    customer -> saleOrder.setCustomerName(customer.getName()));
            fillAuditNames(saleOrder, userMap);
            MapUtils.findAndThen(deptMap, saleOrder.getDeptId(), dept -> saleOrder.setDeptName(dept.getName()));
        });
    }

    private PageResult<UserSimpleRespVO> buildUserSimplePage(PageParam pageReqVO) {
        PageResult<AdminUserRespDTO> page = adminUserApi.getUserSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageReqVO.getKeyword(), pageReqVO);
        List<UserSimpleRespVO> list = convertList(page.getList(), user ->
                new UserSimpleRespVO(user.getId(), user.getNickname(), user.getDeptId(), null));
        return new PageResult<>(list, page.getTotal());
    }

    private Set<Long> convertUserIds(List<ErpSaleOrderDO> saleOrders) {
        Set<Long> userIds = convertSetByFlatMap(saleOrders, saleOrder -> {
            List<Long> ids = new ArrayList<>(2);
            parseUserId(saleOrder.getCreator(), ids);
            parseUserId(saleOrder.getUpdater(), ids);
            return ids.stream();
        });
        userIds.remove(null);
        return userIds;
    }

    private Map<Long, ErpProductRespVO> getProductVOMapIgnoreDataPermission(Set<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(productIds));
    }

    private Map<Long, ErpWarehouseDO> getWarehouseMapIgnoreDataPermission(Set<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds));
    }

    private BigDecimal getStockCountIgnoreDataPermission(Long productId) {
        return DataPermissionUtils.executeIgnore(() -> stockService.getStockCount(productId));
    }

    private Map<Long, BigDecimal> getStockCountMapIgnoreDataPermission(Set<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> stockService.getStockCountMap(productIds));
    }

    private void fillAuditNames(ErpSaleOrderRespVO saleOrder, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(saleOrder.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> saleOrder.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(saleOrder.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> saleOrder.setUpdaterName(user.getNickname()));
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

    private List<ErpSaleOrderExportRespVO> buildSaleOrderExportList(List<ErpSaleOrderRespVO> list) {
        List<ErpSaleOrderExportRespVO> rows = new ArrayList<>();
        for (ErpSaleOrderRespVO saleOrder : list) {
            if (CollUtil.isEmpty(saleOrder.getItems())) {
                rows.add(buildSaleOrderExportRow(saleOrder, null, true));
                continue;
            }
            for (int i = 0; i < saleOrder.getItems().size(); i++) {
                rows.add(buildSaleOrderExportRow(saleOrder, saleOrder.getItems().get(i), i == 0));
            }
        }
        return rows;
    }

    private ErpSaleOrderExportRespVO buildSaleOrderExportRow(ErpSaleOrderRespVO saleOrder,
                                                             ErpSaleOrderRespVO.Item item,
                                                             boolean fillMainFields) {
        ErpSaleOrderExportRespVO row = fillMainFields
                ? BeanUtils.toBean(saleOrder, ErpSaleOrderExportRespVO.class)
                : new ErpSaleOrderExportRespVO();
        row.setCustomerId(saleOrder.getCustomerId());
        if (item == null) {
            return row;
        }
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setProductUnitName(item.getProductUnitName());
        row.setWeight(item.getWeight());
        row.setPackageQty(item.getPackageQty());
        row.setBatchNo(item.getBatchNo());
        row.setItemCount(item.getCount());
        row.setProductPrice(item.getProductPrice());
        row.setItemTotalPrice(item.getTotalPrice());
        row.setItemTaxPercent(item.getTaxPercent());
        row.setItemTaxPrice(item.getTaxPrice());
        row.setItemRemark(item.getRemark());
        return row;
    }

}
