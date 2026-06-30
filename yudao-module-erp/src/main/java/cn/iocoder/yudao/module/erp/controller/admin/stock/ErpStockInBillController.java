package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPickupReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 入仓单报表")
@RestController
@RequestMapping("/erp/stock-in-bill")
@Validated
public class ErpStockInBillController {

    @Resource
    private ErpStockInBillService stockInBillService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpProductService productService;
    @Resource
    private AdminUserApi adminUserApi;

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
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                Collections.singleton(stockInBill.getWarehouseId()));
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
                : warehouseService.getWarehouseMap(convertSet(items, ErpStockInBillItemDO::getWarehouseId));
        Map<Long, ErpProductRespVO> productMap = CollUtil.isEmpty(items) ? Collections.emptyMap()
                : productService.getProductVOMap(convertSet(items, ErpStockInBillItemDO::getProductId));
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
                                       HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpStockInBillRespVO> list = buildStockInBillVOPageResult(
                stockInBillService.getStockInBillPage(pageReqVO)).getList();
        ExcelUtils.write(response, "入仓单.xls", "数据", ErpStockInBillRespVO.class, list);
    }

    private PageResult<ErpStockInBillRespVO> buildStockInBillVOPageResult(PageResult<ErpStockInBillDO> pageResult) {
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(pageResult.getList()) ? Collections.emptyMap()
                : warehouseService.getWarehouseMap(convertSet(pageResult.getList(), ErpStockInBillDO::getWarehouseId));
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

}
