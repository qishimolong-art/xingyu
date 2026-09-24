package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordReportExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordReportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordSummaryVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 产品库存明细")
@RestController
@RequestMapping("/erp/stock-record")
@Validated
@Slf4j
public class ErpStockRecordController {

    private static final String PRODUCT_PRICE_PERMISSION_MODULE = "erp_product";
    private static final String MASK_TEXT = "****";

    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;

    @GetMapping("/get")
    @Operation(summary = "获得产品库存明细")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:query')")
    public CommonResult<ErpStockRecordRespVO> getStockRecord(@RequestParam("id") Long id) {
        ErpStockRecordDO stockRecord = stockRecordService.getStockRecord(id);
        ErpStockRecordRespVO respVO = BeanUtils.toBean(stockRecord, ErpStockRecordRespVO.class);
        maskStockRecord(respVO, getHiddenProductPriceFieldSet());
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得产品库存明细分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:query')")
    public CommonResult<PageResult<ErpStockRecordRespVO>> getStockRecordPage(@Valid ErpStockRecordPageReqVO pageReqVO) {
        PageResult<ErpStockRecordDO> pageResult = stockRecordService.getStockRecordPage(pageReqVO);
        return success(buildStockRecrodVOPageResult(pageResult));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出产品库存明细 Excel")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockRecordExcel(@Valid ErpStockRecordPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpStockRecordRespVO> list = buildStockRecrodVOPageResult(stockRecordService.getStockRecordPage(pageReqVO)).getList();
        // 导出 Excel
        ExcelUtils.write(response, "产品库存明细.xls", "数据", ErpStockRecordRespVO.class, list);
    }

    private PageResult<ErpStockRecordRespVO> buildStockRecrodVOPageResult(PageResult<ErpStockRecordDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(pageResult.getList(), ErpStockRecordDO::getProductId)));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(
                convertSet(pageResult.getList(), ErpStockRecordDO::getWarehouseId)));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(pageResult.getList(), record -> Long.parseLong(record.getCreator())));
        Set<String> hiddenFields = getHiddenProductPriceFieldSet();
        return BeanUtils.toBean(pageResult, ErpStockRecordRespVO.class, stock -> {
            MapUtils.findAndThen(productMap, stock.getProductId(), product -> stock.setProductName(product.getName())
                    .setCategoryName(product.getCategoryName()).setUnitName(product.getUnitName()));
            MapUtils.findAndThen(warehouseMap, stock.getWarehouseId(), warehouse -> stock.setWarehouseName(warehouse.getName()));
            MapUtils.findAndThen(userMap, Long.parseLong(stock.getCreator()), user -> stock.setCreatorName(user.getNickname()));
            maskStockRecord(stock, hiddenFields);
        });
    }

    // ==================== 五期：库存进出流水明细账报表 ====================

    @GetMapping("/report-page")
    @Operation(summary = "获得库存进出流水明细账分页（入出分列，五期）")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:query')")
    public CommonResult<PageResult<ErpStockRecordReportRespVO>> getStockRecordReportPage(
            @Valid ErpStockRecordPageReqVO pageReqVO) {
        PageResult<ErpStockRecordDO> pageResult = stockRecordService.getStockRecordPage(pageReqVO);
        return success(buildReportPageResult(pageResult));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得库存进出流水汇总（底部汇总栏，五期）")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:query')")
    public CommonResult<ErpStockRecordSummaryVO> getStockRecordSummary(
            @Valid ErpStockRecordPageReqVO pageReqVO) {
        ErpStockRecordSummaryVO summary = stockRecordService.getStockRecordSummary(pageReqVO);
        maskStockRecordSummary(summary, getHiddenProductPriceFieldSet());
        return success(summary);
    }

    @GetMapping("/report-export-excel")
    @Operation(summary = "导出库存进出流水明细账 Excel（五期）")
    @PreAuthorize("@ss.hasPermission('erp:stock-record:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockRecordReportExcel(@Valid ErpStockRecordPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        Set<String> hiddenFields = getHiddenProductPriceFieldSet();
        List<ErpStockRecordReportExportRespVO> list = buildReportExportList(
                stockRecordService.getStockRecordPage(pageReqVO), hiddenFields);
        try {
            ExcelUtils.write(response, "库存进出流水明细账.xlsx", "数据",
                    ErpStockRecordReportExportRespVO.class, list);
        } catch (IOException | RuntimeException ex) {
            log.error("[exportStockRecordReportExcel][库存流水导出失败，count={}, reqVO={}]",
                    list.size(), pageReqVO, ex);
            throw ex;
        }
    }

    private PageResult<ErpStockRecordReportRespVO> buildReportPageResult(PageResult<ErpStockRecordDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(pageResult.getList(), ErpStockRecordDO::getProductId)));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(
                convertSet(pageResult.getList(), ErpStockRecordDO::getWarehouseId)));
        Set<Long> deptIds = convertSet(pageResult.getList(), ErpStockRecordDO::getDeptId);
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds) ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        Set<String> hiddenFields = getHiddenProductPriceFieldSet();

        List<ErpStockRecordReportRespVO> list = pageResult.getList().stream().map(r -> {
            ErpStockRecordReportRespVO vo = BeanUtils.toBean(r, ErpStockRecordReportRespVO.class);
            ErpProductRespVO product = productMap.get(r.getProductId());
            if (product != null) {
                vo.setProductCode(product.getCode())
                        .setProductName(product.getName());
            }
            ErpWarehouseDO warehouse = warehouseMap.get(r.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
            }
            DeptRespDTO dept = deptMap.get(r.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            BigDecimal count = r.getCount() != null ? r.getCount() : BigDecimal.ZERO;
            BigDecimal unitPrice = r.getUnitPrice() != null ? r.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal totalPrice = r.getTotalPrice() != null ? r.getTotalPrice() : BigDecimal.ZERO;
            int sign = count.compareTo(BigDecimal.ZERO);
            if (sign > 0) {
                vo.setInCount(count);
                vo.setInUnitPrice(unitPrice);
                vo.setInAmount(totalPrice);
            } else if (sign < 0) {
                vo.setOutCount(count.abs());
                vo.setOutUnitPrice(unitPrice);
                vo.setOutAmount(totalPrice.abs());
            }
            maskStockRecordReport(vo, hiddenFields);
            return vo;
        }).collect(Collectors.toList());
        return new PageResult<>(list, pageResult.getTotal());
    }

    private List<ErpStockRecordReportExportRespVO> buildReportExportList(PageResult<ErpStockRecordDO> pageResult,
                                                                         Set<String> hiddenFields) {
        return buildReportPageResult(pageResult).getList().stream()
                .map(vo -> new ErpStockRecordReportExportRespVO()
                        .setBizDate(vo.getBizDate())
                        .setBizType(vo.getBizType())
                        .setBizNo(vo.getBizNo())
                        .setProductCode(vo.getProductCode())
                        .setProductName(vo.getProductName())
                        .setBatchNo(vo.getBatchNo())
                        .setWarehouseName(vo.getWarehouseName())
                        .setInCount(vo.getInCount())
                        .setInUnitPrice(formatExportPrice(vo.getInUnitPrice(),
                                isStockRecordTradePriceHidden(hiddenFields, vo.getBizType())))
                        .setInAmount(formatExportPrice(vo.getInAmount(),
                                isStockRecordTradePriceHidden(hiddenFields, vo.getBizType())))
                        .setOutCount(vo.getOutCount())
                        .setOutUnitPrice(formatExportPrice(vo.getOutUnitPrice(),
                                isStockRecordTradePriceHidden(hiddenFields, vo.getBizType())))
                        .setOutAmount(formatExportPrice(vo.getOutAmount(),
                                isStockRecordTradePriceHidden(hiddenFields, vo.getBizType())))
                        .setTotalCount(vo.getTotalCount())
                        .setCostPrice(formatExportPrice(vo.getCostPrice(), isStockRecordCostHidden(hiddenFields)))
                        .setCostAmount(formatExportPrice(vo.getCostAmount(), isStockRecordCostHidden(hiddenFields))))
                .collect(Collectors.toList());
    }

    private Set<String> getHiddenProductPriceFieldSet() {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(PRODUCT_PRICE_PERMISSION_MODULE);
        return CollUtil.isEmpty(hiddenFields) ? Collections.emptySet() : new HashSet<>(hiddenFields);
    }

    private boolean isProductPriceFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey) || hiddenFields.contains("col_" + fieldKey);
    }

    private boolean isStockRecordCostHidden(Set<String> hiddenFields) {
        return isProductPriceFieldHidden(hiddenFields, "lastPurchasePrice");
    }

    private boolean isStockRecordTradePriceHidden(Set<String> hiddenFields, Integer bizType) {
        if (isSaleStockRecordBizType(bizType)) {
            return isProductPriceFieldHidden(hiddenFields, "salePrice");
        }
        return isProductPriceFieldHidden(hiddenFields, "lastPurchasePrice")
                || isProductPriceFieldHidden(hiddenFields, "purchasePrice");
    }

    private boolean isSaleStockRecordBizType(Integer bizType) {
        return ErpStockRecordBizTypeEnum.SALE_OUT.getType().equals(bizType)
                || ErpStockRecordBizTypeEnum.SALE_OUT_CANCEL.getType().equals(bizType)
                || ErpStockRecordBizTypeEnum.SALE_RETURN.getType().equals(bizType)
                || ErpStockRecordBizTypeEnum.SALE_RETURN_CANCEL.getType().equals(bizType)
                || ErpStockRecordBizTypeEnum.SALE_PRICE_ADJUST.getType().equals(bizType);
    }

    private void maskStockRecord(ErpStockRecordRespVO vo, Set<String> hiddenFields) {
        if (vo == null || CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        if (isStockRecordTradePriceHidden(hiddenFields, vo.getBizType())) {
            vo.setUnitPrice(null);
            vo.setTotalPrice(null);
        }
        if (isStockRecordCostHidden(hiddenFields)) {
            vo.setCostPrice(null);
            vo.setCostAmount(null);
        }
    }

    private void maskStockRecordReport(ErpStockRecordReportRespVO vo, Set<String> hiddenFields) {
        if (vo == null || CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        if (isStockRecordTradePriceHidden(hiddenFields, vo.getBizType())) {
            vo.setInUnitPrice(null);
            vo.setInAmount(null);
            vo.setOutUnitPrice(null);
            vo.setOutAmount(null);
        }
        if (isStockRecordCostHidden(hiddenFields)) {
            vo.setCostPrice(null);
            vo.setCostAmount(null);
        }
    }

    private void maskStockRecordSummary(ErpStockRecordSummaryVO summary, Set<String> hiddenFields) {
        if (summary == null || CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        if (isProductPriceFieldHidden(hiddenFields, "lastPurchasePrice")
                || isProductPriceFieldHidden(hiddenFields, "purchasePrice")
                || isProductPriceFieldHidden(hiddenFields, "salePrice")) {
            summary.setTotalInAmount(null);
            summary.setTotalOutAmount(null);
        }
        if (isStockRecordCostHidden(hiddenFields)) {
            summary.setBalanceAmount(null);
        }
    }

    private String formatExportPrice(BigDecimal value, boolean hidden) {
        if (hidden) {
            return MASK_TEXT;
        }
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

}
