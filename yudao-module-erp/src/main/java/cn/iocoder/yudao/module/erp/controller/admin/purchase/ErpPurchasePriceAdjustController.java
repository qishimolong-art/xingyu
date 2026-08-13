package cn.iocoder.yudao.module.erp.controller.admin.purchase;

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
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.ErpPurchaseUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchasePriceAdjustTypeEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchasePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 采购调价单")
@RestController
@RequestMapping("/erp/purchase-price-adjust")
@Validated
public class ErpPurchasePriceAdjustController {

    private static final String FIELD_PERMISSION_MODULE = "erp_purchase_price_adjust";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();
    private static final Map<String, String> DETAIL_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "productId", "productCode",
            "count", "count",
            "item_count", "count",
            "itemCount", "count",
            "newPrice", "newPrice",
            "adjustPrice", "newPrice");
    private static final Map<String, String> ORDER_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "no", "no",
            "supplierId", "supplierName",
            "supplierName", "supplierName",
            "adjustTime", "adjustTime",
            "remark", "remark",
            "productId", "productCode",
            "warehouseId", "warehouseName",
            "warehouseName", "warehouseName",
            "count", "itemCount",
            "item_count", "itemCount",
            "itemCount", "itemCount",
            "newPrice", "newPrice",
            "adjustPrice", "newPrice");

    @Resource
    private ErpPurchasePriceAdjustService priceAdjustService;
    @Resource
    private ErpSupplierDeptPermissionService supplierDeptPermissionService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpWarehouseService warehouseService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpFieldConfigService fieldConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建采购调价单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:create') and " +
            "@ss.hasPermission('erp:purchase-price-adjust:update-status')")
    public CommonResult<Long> createPurchasePriceAdjust(@Valid @RequestBody ErpPurchasePriceAdjustSaveReqVO reqVO) {
        return success(priceAdjustService.createPurchasePriceAdjust(reqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建采购调价单草稿")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:create')")
    public CommonResult<Long> createPurchasePriceAdjustDraft(
            @RequestBody ErpPurchasePriceAdjustDraftSaveReqVO reqVO) {
        return success(priceAdjustService.createPurchasePriceAdjustDraft(reqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交采购调价单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:create') and " +
            "@ss.hasPermission('erp:purchase-price-adjust:update-status')")
    public CommonResult<Long> createAndSubmitPurchasePriceAdjust(
            @Valid @RequestBody ErpPurchasePriceAdjustSaveReqVO reqVO) {
        return success(priceAdjustService.createAndSubmitPurchasePriceAdjust(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购调价单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:update')")
    public CommonResult<Boolean> updatePurchasePriceAdjust(@Valid @RequestBody ErpPurchasePriceAdjustSaveReqVO reqVO) {
        priceAdjustService.updatePurchasePriceAdjust(reqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "更新采购调价单草稿")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:update')")
    public CommonResult<Boolean> updatePurchasePriceAdjustDraft(
            @RequestBody ErpPurchasePriceAdjustDraftSaveReqVO reqVO) {
        priceAdjustService.updatePurchasePriceAdjustDraft(reqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "更新并提交采购调价单草稿")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:update') and " +
            "@ss.hasPermission('erp:purchase-price-adjust:update-status')")
    public CommonResult<Boolean> updateAndSubmitPurchasePriceAdjust(
            @Valid @RequestBody ErpPurchasePriceAdjustSaveReqVO reqVO) {
        priceAdjustService.updateAndSubmitPurchasePriceAdjust(reqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交采购调价单草稿")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:update-status')")
    public CommonResult<Boolean> submitPurchasePriceAdjust(@RequestParam("id") Long id) {
        priceAdjustService.submitPurchasePriceAdjust(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "修改采购调价单备注")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:update')")
    public CommonResult<Boolean> updatePurchasePriceAdjustRemark(
            @Valid @RequestBody ErpPurchaseUpdateRemarkReqVO reqVO) {
        priceAdjustService.updatePurchasePriceAdjustRemark(reqVO);
        return success(true);
    }

    @PostMapping("/import")
    @Operation(summary = "导入采购调价明细")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:create')")
    public CommonResult<ErpPurchasePriceAdjustImportRespVO> importPurchasePriceAdjust(@RequestParam("file") MultipartFile file)
            throws Exception {
        List<ErpPurchasePriceAdjustImportExcelVO> list = ExcelUtils.read(file, ErpPurchasePriceAdjustImportExcelVO.class);
        return success(priceAdjustService.importPurchasePriceAdjustItems(list));
    }

    @PostMapping("/import-order")
    @Operation(summary = "Import purchase price adjust order")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:create') and " +
            "@ss.hasPermission('erp:purchase-price-adjust:update-status')")
    public CommonResult<ErpPurchaseImportResultRespVO> importPurchasePriceAdjustOrder(@RequestParam("file") MultipartFile file)
            throws Exception {
        List<ErpPurchasePriceAdjustOrderImportExcelVO> list = ExcelUtils.read(file, ErpPurchasePriceAdjustOrderImportExcelVO.class);
        return success(priceAdjustService.importPurchasePriceAdjustOrderList(list));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获取采购调价导入模板")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:create')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchasePriceAdjustImportExcelVO example = new ErpPurchasePriceAdjustImportExcelVO();
        example.setProductCode("P000001");
        example.setCount(BigDecimal.ONE);
        example.setNewPrice(new BigDecimal("10.00"));
        ExcelUtils.writeImportTemplate(response, "采购调价明细导入模板.xls", "采购调价明细",
                ErpPurchasePriceAdjustImportExcelVO.class, Collections.singletonList(example), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.PURCHASE_PRICE_ADJUST, ErpPurchasePriceAdjustImportExcelVO.class,
                        DETAIL_IMPORT_FIELD_ALIAS_MAP));
    }

    @GetMapping("/get-order-import-template")
    @Operation(summary = "Get purchase price adjust order import template")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:create')")
    public void getOrderImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchasePriceAdjustOrderImportExcelVO example = new ErpPurchasePriceAdjustOrderImportExcelVO();
        example.setNo("CGTJ20260811000002");
        example.setSupplierName("示例供应商");
        example.setAdjustTime("2026-08-11");
        example.setRemark("整单备注");
        example.setProductCode("P000001");
        example.setWarehouseName("主仓库");
        example.setItemCount(BigDecimal.ONE);
        example.setNewPrice(new BigDecimal("10.00"));

        ErpPurchasePriceAdjustOrderImportExcelVO secondItem = new ErpPurchasePriceAdjustOrderImportExcelVO();
        secondItem.setProductCode("P000002");
        secondItem.setWarehouseName("主仓库");
        secondItem.setItemCount(new BigDecimal("2"));
        secondItem.setNewPrice(new BigDecimal("20.00"));

        ExcelUtils.writeImportTemplate(response, "采购调价导入模板.xls", "采购调价",
                ErpPurchasePriceAdjustOrderImportExcelVO.class, java.util.Arrays.asList(example, secondItem), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.PURCHASE_PRICE_ADJUST, ErpPurchasePriceAdjustOrderImportExcelVO.class,
                        ORDER_IMPORT_FIELD_ALIAS_MAP));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新采购调价单状态")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:update-status')")
    public CommonResult<Boolean> updatePurchasePriceAdjustStatus(@RequestParam("id") Long id,
                                                                 @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        priceAdjustService.updatePurchasePriceAdjustStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购调价单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:delete')")
    public CommonResult<Boolean> deletePurchasePriceAdjust(@RequestParam("ids") List<Long> ids) {
        priceAdjustService.deletePurchasePriceAdjust(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购调价单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:query')")
    public CommonResult<ErpPurchasePriceAdjustRespVO> getPurchasePriceAdjust(@RequestParam("id") Long id) {
        ErpPurchasePriceAdjustDO adjust = priceAdjustService.getPurchasePriceAdjust(id);
        if (adjust == null) {
            return success(null);
        }
        List<ErpPurchasePriceAdjustItemDO> items = priceAdjustService.getPurchasePriceAdjustItemListByAdjustId(id);
        ErpPurchasePriceAdjustRespVO respVO = BeanUtils.toBean(adjust, ErpPurchasePriceAdjustRespVO.class);
        respVO.setItems(BeanUtils.toBean(items, ErpPurchasePriceAdjustRespVO.Item.class));
        fillItemProductSnapshots(respVO.getItems());
        fillTotalPrices(respVO, items);
        if (adjust.getSupplierId() != null) {
            ErpSupplierDO supplier = supplierService.getSupplier(adjust.getSupplierId());
            if (supplier != null) {
                respVO.setSupplierName(supplier.getName());
            }
        }
        if (adjust.getDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(adjust.getDeptId());
            if (dept != null) {
                respVO.setDeptName(dept.getName());
            }
        }
        Set<Long> userIds = new HashSet<>();
        if (adjust.getAdjuster() != null) {
            userIds.add(adjust.getAdjuster());
        }
        if (adjust.getCreator() != null) {
            try {
                userIds.add(Long.parseLong(adjust.getCreator()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (adjust.getUpdater() != null) {
            try {
                userIds.add(Long.parseLong(adjust.getUpdater()));
            } catch (NumberFormatException ignored) {
            }
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        if (adjust.getAdjuster() != null) {
            MapUtils.findAndThen(userMap, adjust.getAdjuster(), u -> respVO.setAdjusterName(u.getNickname()));
        }
        if (adjust.getCreator() != null) {
            try {
                long creatorId = Long.parseLong(adjust.getCreator());
                MapUtils.findAndThen(userMap, creatorId, u -> respVO.setCreatorName(u.getNickname()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (adjust.getUpdater() != null) {
            try {
                long updaterId = Long.parseLong(adjust.getUpdater());
                MapUtils.findAndThen(userMap, updaterId, u -> respVO.setUpdaterName(u.getNickname()));
            } catch (NumberFormatException ignored) {
            }
        }
        fillApproverName(respVO, userMap);
        fieldPermissionMasker.mask("erp_purchase_price_adjust", respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购调价单分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:query')")
    public CommonResult<PageResult<ErpPurchasePriceAdjustRespVO>> getPurchasePriceAdjustPage(
            @Valid ErpPurchasePriceAdjustPageReqVO pageReqVO) {
        PageResult<ErpPurchasePriceAdjustDO> pageResult = priceAdjustService.getPurchasePriceAdjustPage(pageReqVO);
        return success(buildVOPageResult(pageResult));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "鑾峰緱褰撳墠鐢ㄦ埛鍙煡璇㈢殑閲囪喘璋冧环閮ㄩ棬绮剧畝鍒楄〃")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:query')")
    public CommonResult<List<DeptSimpleRespVO>> getVisibleDeptSimpleList() {
        return success(supplierDeptPermissionService.getDataPermissionDeptSimpleList(FIELD_PERMISSION_MODULE));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购调价单 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchasePriceAdjustExcel(@Valid ErpPurchasePriceAdjustPageReqVO pageReqVO,
                                               @RequestParam(value = "fields", required = false) String fields,
                                               HttpServletResponse response) throws IOException {
        List<ErpPurchasePriceAdjustRespVO> list;
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            list = buildVOPageResult(new PageResult<>(
                    priceAdjustService.getPurchasePriceAdjustList(pageReqVO.getIds()),
                    (long) pageReqVO.getIds().size())).getList();
        } else {
            pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
            list = buildVOPageResult(priceAdjustService.getPurchasePriceAdjustPage(pageReqVO)).getList();
        }
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpPurchasePriceAdjustExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "采购调价单.xls", "数据", ErpPurchasePriceAdjustExportRespVO.class,
                buildExportList(list), includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "获得采购调价单导出字段")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getPurchasePriceAdjustExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpPurchasePriceAdjustExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    private PageResult<ErpPurchasePriceAdjustRespVO> buildVOPageResult(PageResult<ErpPurchasePriceAdjustDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpPurchasePriceAdjustItemDO> itemList = priceAdjustService.getPurchasePriceAdjustItemListByAdjustIds(
                convertSet(pageResult.getList(), ErpPurchasePriceAdjustDO::getId));
        Map<Long, List<ErpPurchasePriceAdjustItemDO>> itemMap = convertMultiMap(itemList, ErpPurchasePriceAdjustItemDO::getAdjustId);
        Map<Long, ErpProductRespVO> productMap = getProductSnapshotMap(
                convertSet(itemList, ErpPurchasePriceAdjustItemDO::getProductId));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchasePriceAdjustDO::getSupplierId));
        Set<Long> userIds = new HashSet<>();
        for (ErpPurchasePriceAdjustDO adjust : pageResult.getList()) {
            if (adjust.getAdjuster() != null) {
                userIds.add(adjust.getAdjuster());
            }
            if (adjust.getCreator() != null) {
                try {
                    userIds.add(Long.parseLong(adjust.getCreator()));
                } catch (NumberFormatException ignored) {
                }
            }
            if (adjust.getUpdater() != null) {
                try {
                    userIds.add(Long.parseLong(adjust.getUpdater()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> deptIds = convertSet(pageResult.getList(), ErpPurchasePriceAdjustDO::getDeptId);
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? new java.util.HashMap<>() : deptApi.getDeptMap(deptIds);
        PageResult<ErpPurchasePriceAdjustRespVO> respResult = BeanUtils.toBean(pageResult, ErpPurchasePriceAdjustRespVO.class, respVO -> {
            List<ErpPurchasePriceAdjustItemDO> items = itemMap.get(respVO.getId());
            respVO.setItems(BeanUtils.toBean(items, ErpPurchasePriceAdjustRespVO.Item.class));
            fillItemProductSnapshots(respVO.getItems(), productMap);
            fillTotalPrices(respVO, items);
            MapUtils.findAndThen(supplierMap, respVO.getSupplierId(), s -> respVO.setSupplierName(s.getName()));
            MapUtils.findAndThen(deptMap, respVO.getDeptId(), d -> respVO.setDeptName(d.getName()));
            if (respVO.getAdjuster() != null) {
                MapUtils.findAndThen(userMap, respVO.getAdjuster(), u -> respVO.setAdjusterName(u.getNickname()));
            }
            if (respVO.getCreator() != null) {
                try {
                    long creatorId = Long.parseLong(respVO.getCreator());
                    MapUtils.findAndThen(userMap, creatorId, u -> respVO.setCreatorName(u.getNickname()));
                } catch (NumberFormatException ignored) {
                }
            }
            if (respVO.getUpdater() != null) {
                try {
                    long updaterId = Long.parseLong(respVO.getUpdater());
                    MapUtils.findAndThen(userMap, updaterId, u -> respVO.setUpdaterName(u.getNickname()));
                } catch (NumberFormatException ignored) {
                }
            }
            fillApproverName(respVO, userMap);
        });
        fieldPermissionMasker.maskList(FIELD_PERMISSION_MODULE, respResult.getList());
        return respResult;
    }

    private void fillTotalPrices(ErpPurchasePriceAdjustRespVO respVO, List<ErpPurchasePriceAdjustItemDO> items) {
        BigDecimal originalTotalPrice = BigDecimal.ZERO;
        BigDecimal adjustedTotalPrice = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(items)) {
            for (ErpPurchasePriceAdjustItemDO item : items) {
                BigDecimal count = nullToZero(item.getCount());
                originalTotalPrice = originalTotalPrice.add(nullToZero(item.getOldPrice()).multiply(count));
                adjustedTotalPrice = adjustedTotalPrice.add(nullToZero(item.getNewPrice()).multiply(count));
            }
        }
        respVO.setOriginalTotalPrice(originalTotalPrice);
        respVO.setAdjustedTotalPrice(adjustedTotalPrice);
    }

    private void fillItemProductSnapshots(List<ErpPurchasePriceAdjustRespVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        fillItemProductSnapshots(items,
                getProductSnapshotMap(convertSet(items, ErpPurchasePriceAdjustRespVO.Item::getProductId)));
    }

    private Map<Long, ErpProductRespVO> getProductSnapshotMap(Set<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(productIds));
    }

    private void fillItemProductSnapshots(List<ErpPurchasePriceAdjustRespVO.Item> items,
                                          Map<Long, ErpProductRespVO> productMap) {
        if (CollUtil.isEmpty(items) || CollUtil.isEmpty(productMap)) {
            return;
        }
        for (ErpPurchasePriceAdjustRespVO.Item item : items) {
            MapUtils.findAndThen(productMap, item.getProductId(), product -> {
                if (item.getProductCode() == null) {
                    item.setProductCode(product.getCode());
                }
                if (item.getProductName() == null) {
                    item.setProductName(product.getName());
                }
                if (item.getProductUnitName() == null) {
                    item.setProductUnitName(product.getUnitName());
                }
                if (item.getVehicleModel() == null) {
                    item.setVehicleModel(product.getVehicleModel());
                }
                if (item.getStandard() == null) {
                    item.setStandard(product.getStandard());
                }
                if (item.getFeatureCode() == null) {
                    item.setFeatureCode(product.getFeatureCode());
                }
                if (item.getOriginPlace() == null) {
                    item.setOriginPlace(product.getOriginPlace());
                }
                if (item.getBrand() == null) {
                    item.setBrand(product.getBrand());
                }
                if (item.getDrawingNo() == null) {
                    item.setDrawingNo(product.getDrawingNo());
                }
            });
        }
    }

    private void fillApproverName(ErpPurchasePriceAdjustRespVO respVO, Map<Long, AdminUserRespDTO> userMap) {
        if (respVO.getApproveTime() == null || respVO.getUpdater() == null) {
            return;
        }
        try {
            long updaterId = Long.parseLong(respVO.getUpdater());
            MapUtils.findAndThen(userMap, updaterId, u -> respVO.setApproverName(u.getNickname()));
        } catch (NumberFormatException ignored) {
        }
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<ErpPurchasePriceAdjustExportRespVO> buildExportList(List<ErpPurchasePriceAdjustRespVO> list) {
        Map<Long, ErpWarehouseDO> warehouseMap = cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                warehouseService.getWarehouseListByStatus(0), ErpWarehouseDO::getId);
        List<ErpPurchasePriceAdjustExportRespVO> rows = new ArrayList<>();
        for (ErpPurchasePriceAdjustRespVO adjust : list) {
            if (CollUtil.isEmpty(adjust.getItems())) {
                rows.add(buildExportRow(adjust, null, true, warehouseMap));
                continue;
            }
            for (int i = 0; i < adjust.getItems().size(); i++) {
                rows.add(buildExportRow(adjust, adjust.getItems().get(i), i == 0, warehouseMap));
            }
        }
        return rows;
    }

    private ErpPurchasePriceAdjustExportRespVO buildExportRow(ErpPurchasePriceAdjustRespVO adjust,
                                                              ErpPurchasePriceAdjustRespVO.Item item,
                                                              boolean fillMainFields,
                                                              Map<Long, ErpWarehouseDO> warehouseMap) {
        ErpPurchasePriceAdjustExportRespVO row = fillMainFields
                ? BeanUtils.toBean(adjust, ErpPurchasePriceAdjustExportRespVO.class)
                : new ErpPurchasePriceAdjustExportRespVO();
        if (!fillMainFields) {
            row.setNo(null);
            row.setSupplierName(null);
            row.setDeptName(null);
            row.setAdjustTime(null);
            row.setAdjustTypeName(null);
            row.setStatus(null);
            row.setAdjusterName(null);
            row.setTotalAdjustPrice(null);
            row.setPaymentPrice(null);
            row.setApproveTime(null);
            row.setCreatorName(null);
            row.setCreateTime(null);
            row.setUpdaterName(null);
            row.setUpdateTime(null);
            row.setRemark(null);
        }
        if (fillMainFields) {
            if (adjust.getAdjustType() != null) {
                row.setAdjustTypeName(ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getType().equals(adjust.getAdjustType())
                        ? ErpPurchasePriceAdjustTypeEnum.BY_IN_ORDER.getName()
                        : ErpPurchasePriceAdjustTypeEnum.BY_ITEM.getName());
            }
        }
        if (item == null) {
            return row;
        }
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setProductUnitName(item.getProductUnitName());
        row.setVehicleModel(item.getVehicleModel());
        row.setStandard(item.getStandard());
        row.setFeatureCode(item.getFeatureCode());
        row.setOriginPlace(item.getOriginPlace());
        row.setBrand(item.getBrand());
        row.setDrawingNo(item.getDrawingNo());
        row.setWarehouseName(null);
        row.setWarehousePosition(item.getWarehousePosition());
        MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> row.setWarehouseName(warehouse.getName()));
        row.setInNo(item.getInNo());
        row.setOldPrice(item.getOldPrice());
        row.setNewPrice(item.getNewPrice());
        row.setCount(item.getCount());
        row.setAdjustRatio(item.getAdjustRatio());
        row.setAdjustPrice(item.getAdjustPrice());
        return row;
    }

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("no", "main");
        map.put("supplierName", "main");
        map.put("deptName", "main");
        map.put("adjustTime", "main");
        map.put("adjustTypeName", "main");
        map.put("status", "main");
        map.put("adjusterName", "main");
        map.put("totalAdjustPrice", "main");
        map.put("paymentPrice", "main");
        map.put("approveTime", "main");
        map.put("creatorName", "system");
        map.put("createTime", "system");
        map.put("updaterName", "system");
        map.put("updateTime", "system");
        map.put("remark", "main");
        map.put("productCode", "detail");
        map.put("productName", "detail");
        map.put("productUnitName", "detail");
        map.put("vehicleModel", "detail");
        map.put("standard", "detail");
        map.put("featureCode", "detail");
        map.put("originPlace", "detail");
        map.put("brand", "detail");
        map.put("drawingNo", "detail");
        map.put("warehouseName", "detail");
        map.put("warehousePosition", "detail");
        map.put("inNo", "detail");
        map.put("oldPrice", "detail");
        map.put("newPrice", "detail");
        map.put("count", "detail");
        map.put("adjustRatio", "detail");
        map.put("adjustPrice", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("supplierName", "supplierId");
        map.put("deptName", "deptId");
        map.put("adjusterName", "adjuster");
        map.put("productCode", "item_productCode");
        map.put("productName", "item_productName");
        map.put("productUnitName", "item_productUnitName");
        map.put("vehicleModel", "item_vehicleModel");
        map.put("standard", "item_standard");
        map.put("featureCode", "item_featureCode");
        map.put("originPlace", "item_originPlace");
        map.put("brand", "item_brand");
        map.put("drawingNo", "item_drawingNo");
        map.put("warehouseName", "item_warehouseId");
        map.put("warehousePosition", "item_warehousePosition");
        map.put("oldPrice", "item_oldPrice");
        map.put("newPrice", "item_newPrice");
        map.put("count", "item_count");
        map.put("adjustRatio", "item_adjustRatio");
        map.put("adjustPrice", "item_adjustPrice");
        return map;
    }

}
