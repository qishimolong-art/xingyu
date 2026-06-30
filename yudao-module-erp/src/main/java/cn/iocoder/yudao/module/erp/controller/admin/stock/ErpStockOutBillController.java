package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPickReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
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
import javax.validation.Valid;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 出仓单")
@RestController
@RequestMapping("/erp/stock-out-bill")
@Validated
public class ErpStockOutBillController {

    @Resource
    private ErpStockOutBillService stockOutBillService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpProductService productService;
    @Resource
    private AdminUserApi adminUserApi;

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
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                Collections.singleton(stockOutBill.getWarehouseId()));
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
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(items) ? Collections.emptyMap()
                : warehouseService.getWarehouseMap(convertSet(items, ErpStockOutBillItemDO::getWarehouseId));
        Map<Long, ErpProductRespVO> productMap = CollUtil.isEmpty(items) ? Collections.emptyMap()
                : productService.getProductVOMap(convertSet(items, ErpStockOutBillItemDO::getProductId));
        return success(BeanUtils.toBean(items, ErpStockOutBillItemRespVO.class, vo -> {
            MapUtils.findAndThen(warehouseMap, vo.getWarehouseId(),
                    warehouse -> vo.setWarehouseName(warehouse.getName()));
            MapUtils.findAndThen(productMap, vo.getProductId(), product -> vo
                    .setProductCode(product.getCode())
                    .setProductName(product.getName())
                    .setProductUnitId(product.getUnitId())
                    .setProductUnitName(product.getUnitName()));
            vo.setRemainCount(nullToZero(vo.getCount()).subtract(nullToZero(vo.getPickedCount())));
        }));
    }

    @PutMapping("/pick")
    @Operation(summary = "出仓单拣货")
    @PreAuthorize("@ss.hasPermission('erp:stock-out-bill:pick')")
    public CommonResult<Boolean> pick(@Valid @RequestBody ErpStockOutBillPickReqVO reqVO) {
        stockOutBillService.pick(reqVO);
        return success(true);
    }

    private PageResult<ErpStockOutBillRespVO> buildStockOutBillVOPageResult(PageResult<ErpStockOutBillDO> pageResult) {
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(pageResult.getList()) ? Collections.emptyMap()
                : warehouseService.getWarehouseMap(convertSet(pageResult.getList(), ErpStockOutBillDO::getWarehouseId));
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

}
