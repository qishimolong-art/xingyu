package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchasePriceAdjustTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchasePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
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

    @Resource
    private ErpPurchasePriceAdjustService priceAdjustService;
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

    @PostMapping("/create")
    @Operation(summary = "创建采购调价单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:create')")
    public CommonResult<Long> createPurchasePriceAdjust(@Valid @RequestBody ErpPurchasePriceAdjustSaveReqVO reqVO) {
        return success(priceAdjustService.createPurchasePriceAdjust(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购调价单")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:update')")
    public CommonResult<Boolean> updatePurchasePriceAdjust(@Valid @RequestBody ErpPurchasePriceAdjustSaveReqVO reqVO) {
        priceAdjustService.updatePurchasePriceAdjust(reqVO);
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

    @GetMapping("/get-import-template")
    @Operation(summary = "获取采购调价导入模板")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:create')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpPurchasePriceAdjustImportExcelVO example = new ErpPurchasePriceAdjustImportExcelVO();
        example.setProductCode("P000001");
        example.setCount(BigDecimal.ONE);
        example.setNewPrice(new BigDecimal("10.00"));
        ExcelUtils.write(response, "purchase-price-adjust-import-template.xls", "purchase-price-adjust",
                ErpPurchasePriceAdjustImportExcelVO.class, Collections.singletonList(example));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新采购调价单状态")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:update')")
    public CommonResult<Boolean> updatePurchasePriceAdjustStatus(@RequestParam("id") Long id,
                                                                 @RequestParam("status") Integer status) {
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

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购调价单 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-price-adjust:query')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchasePriceAdjustExcel(@Valid ErpPurchasePriceAdjustPageReqVO pageReqVO,
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
        ExcelUtils.write(response, "采购调价单.xls", "数据", ErpPurchasePriceAdjustExportRespVO.class,
                buildExportList(list));
    }

    private PageResult<ErpPurchasePriceAdjustRespVO> buildVOPageResult(PageResult<ErpPurchasePriceAdjustDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpPurchasePriceAdjustItemDO> itemList = priceAdjustService.getPurchasePriceAdjustItemListByAdjustIds(
                convertSet(pageResult.getList(), ErpPurchasePriceAdjustDO::getId));
        Map<Long, List<ErpPurchasePriceAdjustItemDO>> itemMap = convertMultiMap(itemList, ErpPurchasePriceAdjustItemDO::getAdjustId);
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
        return BeanUtils.toBean(pageResult, ErpPurchasePriceAdjustRespVO.class, respVO -> {
            respVO.setItems(BeanUtils.toBean(itemMap.get(respVO.getId()), ErpPurchasePriceAdjustRespVO.Item.class));
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
        });
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

}
