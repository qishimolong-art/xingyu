package cn.iocoder.yudao.module.erp.controller.admin.sale;

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
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
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
            "productId", "productCode",
            "productCode", "productCode",
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
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpFieldConfigService fieldConfigService;

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

    @PutMapping("/update")
    @Operation(summary = "更新销售退货")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:update')")
    public CommonResult<Boolean> updateSaleReturn(@Valid @RequestBody ErpSaleReturnSaveReqVO updateReqVO) {
        saleReturnService.updateSaleReturn(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新销售退货的状态")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:update-status')")
    public CommonResult<Boolean> updateSaleReturnStatus(@RequestParam("id") Long id,
                                                        @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        saleReturnService.updateSaleReturnStatus(id, status);
        return success(true);
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
    public CommonResult<ErpSaleReturnRespVO> getSaleReturn(@RequestParam("id") Long id) {
        ErpSaleReturnDO saleReturn = saleReturnService.getSaleReturn(id);
        if (saleReturn == null) {
            return success(null);
        }
        List<ErpSaleReturnItemDO> saleReturnItemList = saleReturnService.getSaleReturnItemListByReturnId(id);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(saleReturnItemList, ErpSaleReturnItemDO::getProductId));
        Set<Long> userIds = convertUserIds(Collections.singletonList(saleReturn));
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty() ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        DeptRespDTO dept = saleReturn.getDeptId() == null ? null : deptApi.getDept(saleReturn.getDeptId());
        ErpSaleReturnRespVO respVO = BeanUtils.toBean(saleReturn, ErpSaleReturnRespVO.class, saleReturnVO -> {
            fillUserNames(saleReturnVO, userMap);
            if (dept != null) {
                saleReturnVO.setDeptName(dept.getName());
            }
            saleReturnVO.setItems(BeanUtils.toBean(saleReturnItemList, ErpSaleReturnRespVO.Item.class, item -> {
                ErpStockDO stock = stockService.getStock(item.getProductId(), item.getWarehouseId());
                item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                        .setProductCode(product.getCode()));
            }));
        });
        fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售退货分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:query')")
    public CommonResult<PageResult<ErpSaleReturnRespVO>> getSaleReturnPage(@Valid ErpSaleReturnPageReqVO pageReqVO) {
        PageResult<ErpSaleReturnDO> pageResult = saleReturnService.getSaleReturnPage(pageReqVO);
        PageResult<ErpSaleReturnRespVO> respResult = buildSaleReturnVOPageResult(pageResult);
        fieldPermissionMasker.maskFormsWithItems(FIELD_PERMISSION_MODULE, respResult.getList());
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
        fieldPermissionMasker.maskFormsWithItems(FIELD_PERMISSION_MODULE, list);
        List<ErpSaleReturnExportRespVO> rows = buildSaleReturnExportList(list);
        fieldPermissionMasker.maskExportRows(FIELD_PERMISSION_MODULE, rows);
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpSaleReturnExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "销售退货.xls", "数据", ErpSaleReturnExportRespVO.class, rows, includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get sale return export fields")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getSaleReturnExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpSaleReturnExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    @GetMapping("/export-import-template")
    @Operation(summary = "获得销售退货导入模板")
    public void exportImportTemplate(HttpServletResponse response) throws IOException {
        ErpSaleReturnImportExcelVO example = new ErpSaleReturnImportExcelVO();
        example.setProductCode("P0001");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("100.00"));
        example.setWarehouseName("默认仓");
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

    private PageResult<ErpSaleReturnRespVO> buildSaleReturnVOPageResult(PageResult<ErpSaleReturnDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        // 1.1 退货项
        List<ErpSaleReturnItemDO> saleReturnItemList = saleReturnService.getSaleReturnItemListByReturnIds(
                convertSet(pageResult.getList(), ErpSaleReturnDO::getId));
        Map<Long, List<ErpSaleReturnItemDO>> saleReturnItemMap = convertMultiMap(saleReturnItemList, ErpSaleReturnItemDO::getReturnId);
        // 1.2 产品信息
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(saleReturnItemList, ErpSaleReturnItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(saleReturnItemList, ErpSaleReturnItemDO::getWarehouseId));
        // 1.3 客户信息
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpSaleReturnDO::getCustomerId));
        // 1.4 管理员信息
        Set<Long> userIds = convertUserIds(pageResult.getList());
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty() ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpSaleReturnDO::getDeptId));
        // 2. 开始拼接
        return BeanUtils.toBean(pageResult, ErpSaleReturnRespVO.class, saleReturn -> {
            saleReturn.setItems(BeanUtils.toBean(saleReturnItemMap.get(saleReturn.getId()), ErpSaleReturnRespVO.Item.class,
                    item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                            .setProductCode(product.getCode()))));
            saleReturn.setProductNames(CollUtil.join(saleReturn.getItems(), "，", ErpSaleReturnRespVO.Item::getProductName));
            MapUtils.findAndThen(customerMap, saleReturn.getCustomerId(), supplier -> saleReturn.setCustomerName(supplier.getName()));
            fillUserNames(saleReturn, userMap);
            MapUtils.findAndThen(deptMap, saleReturn.getDeptId(), dept -> saleReturn.setDeptName(dept.getName()));
            saleReturn.getItems().forEach(item ->
                    MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> item.setWarehouseName(warehouse.getName())));
        });
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
        if (item == null) {
            return row;
        }
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setProductUnitName(item.getProductUnitName());
        row.setWarehouseName(item.getWarehouseName());
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
        map.put("warehouseName", "detail");
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
        map.put("warehouseName", "item_warehouseId");
        map.put("itemCount", "item_count");
        map.put("productPrice", "item_productPrice");
        map.put("returnReason", "item_returnReason");
        map.put("warehousePosition", "item_warehousePosition");
        map.put("itemRemark", "item_remark");
        return map;
    }

}
