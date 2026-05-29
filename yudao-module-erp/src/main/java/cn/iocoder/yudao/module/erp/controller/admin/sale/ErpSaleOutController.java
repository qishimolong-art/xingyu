package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 销售出库")
@RestController
@RequestMapping("/erp/sale-out")
@Validated
public class ErpSaleOutController {

    @Resource
    private ErpSaleOutService saleOutService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpSaleReturnItemMapper saleReturnItemMapper;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建销售出库")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:create')")
    public CommonResult<Long> createSaleOut(@Valid @RequestBody ErpSaleOutSaveReqVO createReqVO) {
        return success(saleOutService.createSaleOut(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新销售出库")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:update')")
    public CommonResult<Boolean> updateSaleOut(@Valid @RequestBody ErpSaleOutSaveReqVO updateReqVO) {
        saleOutService.updateSaleOut(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新销售出库的状态")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:update-status')")
    public CommonResult<Boolean> updateSaleOutStatus(@RequestParam("id") Long id,
                                                      @RequestParam("status") Integer status) {
        saleOutService.updateSaleOutStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除销售出库")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-out:delete')")
    public CommonResult<Boolean> deleteSaleOut(@RequestParam("ids") List<Long> ids) {
        saleOutService.deleteSaleOut(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售出库")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:query')")
    public CommonResult<ErpSaleOutRespVO> getSaleOut(@RequestParam("id") Long id) {
        ErpSaleOutDO saleOut = saleOutService.getSaleOut(id);
        if (saleOut == null) {
            return success(null);
        }
        List<ErpSaleOutItemDO> saleOutItemList = saleOutService.getSaleOutItemListByOutId(id);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(saleOutItemList, ErpSaleOutItemDO::getProductId));
        // 仓库信息
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(saleOutItemList, ErpSaleOutItemDO::getWarehouseId));
        // 退货状态
        Set<Long> outItemIds = convertSet(saleOutItemList, ErpSaleOutItemDO::getId);
        Map<Long, BigDecimal> returnedCountMap = saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(outItemIds);

        ErpSaleOutRespVO respVO = BeanUtils.toBean(saleOut, ErpSaleOutRespVO.class, saleOutVO ->
                saleOutVO.setItems(BeanUtils.toBean(saleOutItemList, ErpSaleOutRespVO.Item.class, item -> {
                    ErpStockDO stock = stockService.getStock(item.getProductId(), item.getWarehouseId());
                    item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                    MapUtils.findAndThen(productMap, item.getProductId(), product -> {
                        item.setProductName(product.getName())
                                .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName());
                        item.setProductCode(product.getCode());
                        item.setVehicleModel(product.getVehicleModel());
                        item.setStandard(product.getStandard());
                        item.setFeatureCode(product.getFeatureCode());
                        item.setBrand(product.getBrand());
                        item.setDrawingNo(product.getDrawingNo());
                        item.setOriginPlace(product.getOriginPlace());
                    });
                    MapUtils.findAndThen(warehouseMap, item.getWarehouseId(),
                            warehouse -> item.setWarehouseName(warehouse.getName()));
                    // 计算产品金额
                    if (item.getProductPrice() != null && item.getCount() != null) {
                        item.setTotalProductPrice(item.getProductPrice().multiply(item.getCount()));
                    }
                    // 已退数量
                    item.setReturnedCount(returnedCountMap.get(item.getId()));
                })));
        // 填充主表关联字段
        fillSaleOutRelationFields(respVO, saleOut);
        // 退货状态
        respVO.setReturnStatus(calculateReturnStatus(saleOutItemList, returnedCountMap));
        return success(respVO);
    }

    /**
     * 填充销售单主表的关联字段（客户编码、审核人、业务员、部门）
     */
    private void fillSaleOutRelationFields(ErpSaleOutRespVO respVO, ErpSaleOutDO saleOut) {
        // 客户
        if (saleOut.getCustomerId() != null) {
            ErpCustomerDO customer = customerService.getCustomer(saleOut.getCustomerId());
            if (customer != null) {
                respVO.setCustomerName(customer.getName());
                respVO.setCustomerCode(customer.getCode());
            }
        }
        // 用户信息（创建人、业务员、审核人）
        Set<Long> userIds = new HashSet<>();
        if (saleOut.getCreator() != null) {
            try { userIds.add(Long.parseLong(saleOut.getCreator())); } catch (NumberFormatException ignored) {}
        }
        if (saleOut.getSaleUserId() != null) {
            userIds.add(saleOut.getSaleUserId());
        }
        if (saleOut.getAuditorId() != null) {
            userIds.add(saleOut.getAuditorId());
        }
        if (!userIds.isEmpty()) {
            Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
            if (saleOut.getCreator() != null) {
                try {
                    MapUtils.findAndThen(userMap, Long.parseLong(saleOut.getCreator()), user -> respVO.setCreatorName(user.getNickname()));
                } catch (NumberFormatException ignored) {}
            }
            if (saleOut.getSaleUserId() != null) {
                MapUtils.findAndThen(userMap, saleOut.getSaleUserId(), user -> respVO.setSaleUserName(user.getNickname()));
            }
            if (saleOut.getAuditorId() != null) {
                MapUtils.findAndThen(userMap, saleOut.getAuditorId(), user -> respVO.setAuditorName(user.getNickname()));
            }
        }
        // 部门
        if (saleOut.getDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(saleOut.getDeptId());
            if (dept != null) {
                respVO.setDeptName(dept.getName());
            }
        }
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售出库分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:query')")
    public CommonResult<PageResult<ErpSaleOutRespVO>> getSaleOutPage(@Valid ErpSaleOutPageReqVO pageReqVO) {
        PageResult<ErpSaleOutDO> pageResult = saleOutService.getSaleOutPage(pageReqVO);
        return success(buildSaleOutVOPageResult(pageResult));
    }

    @GetMapping("/returnable-items")
    @Operation(summary = "获取销售单的可退明细（按销售单退货使用）")
    @Parameter(name = "outId", description = "销售单 ID", required = true, example = "17386")
    @PreAuthorize("@ss.hasPermission('erp:sale-return:create')")
    public CommonResult<List<ErpSaleReturnableItemRespVO>> getReturnableItems(@RequestParam("outId") Long outId) {
        return success(saleOutService.getReturnableItemsByOutId(outId));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出销售出库 Excel")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSaleOutExcel(@Valid ErpSaleOutPageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpSaleOutDO> pageResult = saleOutService.getSaleOutPage(pageReqVO);
        List<ErpSaleOutItemDO> saleOutItemList = saleOutService.getSaleOutItemListByOutIds(
                convertSet(pageResult.getList(), ErpSaleOutDO::getId));
        Map<Long, List<ErpSaleOutItemDO>> saleOutItemMap = convertMultiMap(saleOutItemList, ErpSaleOutItemDO::getOutId);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(saleOutItemList, ErpSaleOutItemDO::getProductId));
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpSaleOutDO::getCustomerId));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(out -> {
            if (out.getCreator() != null) {
                try {
                    userIds.add(Long.parseLong(out.getCreator()));
                } catch (NumberFormatException ignored) {
                }
            }
            if (out.getSaleUserId() != null) {
                userIds.add(out.getSaleUserId());
            }
        });
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty() ? new HashMap<>() : adminUserApi.getUserMap(userIds);
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(saleOutItemList, ErpSaleOutItemDO::getWarehouseId));
        ExcelUtils.write(response, "销售单.xls", "数据", ErpSaleOutExportRespVO.class,
                buildSaleOutExportList(pageResult.getList(), saleOutItemMap, productMap, customerMap, userMap, warehouseMap));
    }

    private PageResult<ErpSaleOutRespVO> buildSaleOutVOPageResult(PageResult<ErpSaleOutDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        // 1.1 出库项
        List<ErpSaleOutItemDO> saleOutItemList = saleOutService.getSaleOutItemListByOutIds(
                convertSet(pageResult.getList(), ErpSaleOutDO::getId));
        Map<Long, List<ErpSaleOutItemDO>> saleOutItemMap = convertMultiMap(saleOutItemList, ErpSaleOutItemDO::getOutId);
        // 1.2 产品信息
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(saleOutItemList, ErpSaleOutItemDO::getProductId));
        // 1.3 客户信息
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpSaleOutDO::getCustomerId));
        // 1.4 管理员信息（创建人 + 业务员）
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(out -> {
            if (out.getCreator() != null) {
                try { userIds.add(Long.parseLong(out.getCreator())); } catch (NumberFormatException ignored) {}
            }
            if (out.getSaleUserId() != null) {
                userIds.add(out.getSaleUserId());
            }
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        // 1.5 退货状态：按 sourceOutItemId 聚合已退数量
        Set<Long> allOutItemIds = convertSet(saleOutItemList, ErpSaleOutItemDO::getId);
        Map<Long, BigDecimal> returnedCountMap = saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(allOutItemIds);
        // 2. 开始拼接
        return BeanUtils.toBean(pageResult, ErpSaleOutRespVO.class, saleOut -> {
            saleOut.setItems(BeanUtils.toBean(saleOutItemMap.get(saleOut.getId()), ErpSaleOutRespVO.Item.class,
                    item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                            .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName()))));
            saleOut.setProductNames(CollUtil.join(saleOut.getItems(), "，", ErpSaleOutRespVO.Item::getProductName));
            MapUtils.findAndThen(customerMap, saleOut.getCustomerId(), customer -> {
                saleOut.setCustomerName(customer.getName());
                saleOut.setCustomerCode(customer.getCode());
            });
            if (saleOut.getCreator() != null) {
                try {
                    MapUtils.findAndThen(userMap, Long.parseLong(saleOut.getCreator()), user -> saleOut.setCreatorName(user.getNickname()));
                } catch (NumberFormatException ignored) {}
            }
            // 业务员名称
            if (saleOut.getSaleUserId() != null) {
                MapUtils.findAndThen(userMap, saleOut.getSaleUserId(), user -> saleOut.setSaleUserName(user.getNickname()));
            }
            // 退货状态计算
            saleOut.setReturnStatus(calculateReturnStatus(saleOutItemMap.get(saleOut.getId()), returnedCountMap));
        });
    }

    /**
     * 计算退货状态：0=未退, 1=部分退, 2=整退
     */
    private Integer calculateReturnStatus(List<ErpSaleOutItemDO> items, Map<Long, BigDecimal> returnedCountMap) {
        if (CollUtil.isEmpty(items) || returnedCountMap.isEmpty()) {
            return 0;
        }
        boolean hasReturn = false;
        boolean allReturned = true;
        for (ErpSaleOutItemDO item : items) {
            BigDecimal returned = returnedCountMap.get(item.getId());
            if (returned != null && returned.compareTo(BigDecimal.ZERO) > 0) {
                hasReturn = true;
                if (returned.compareTo(item.getCount()) < 0) {
                    allReturned = false;
                }
            } else {
                allReturned = false;
            }
        }
        if (!hasReturn) {
            return 0;
        }
        return allReturned ? 2 : 1;
    }

    private List<ErpSaleOutExportRespVO> buildSaleOutExportList(List<ErpSaleOutDO> list,
                                                                Map<Long, List<ErpSaleOutItemDO>> saleOutItemMap,
                                                                Map<Long, ErpProductRespVO> productMap,
                                                                Map<Long, ErpCustomerDO> customerMap,
                                                                Map<Long, AdminUserRespDTO> userMap,
                                                                Map<Long, ErpWarehouseDO> warehouseMap) {
        List<ErpSaleOutExportRespVO> rows = new ArrayList<>();
        for (ErpSaleOutDO saleOut : list) {
            List<ErpSaleOutItemDO> items = saleOutItemMap.getOrDefault(saleOut.getId(), Collections.emptyList());
            if (CollUtil.isEmpty(items)) {
                rows.add(buildSaleOutExportRow(saleOut, customerMap.get(saleOut.getCustomerId()),
                        getCreator(userMap, saleOut.getCreator()), userMap.get(saleOut.getSaleUserId()),
                        null, null, null, true));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                ErpSaleOutItemDO item = items.get(i);
                rows.add(buildSaleOutExportRow(saleOut, customerMap.get(saleOut.getCustomerId()),
                        getCreator(userMap, saleOut.getCreator()), userMap.get(saleOut.getSaleUserId()),
                        item, productMap.get(item.getProductId()), warehouseMap.get(item.getWarehouseId()), i == 0));
            }
        }
        return rows;
    }

    private ErpSaleOutExportRespVO buildSaleOutExportRow(ErpSaleOutDO saleOut,
                                                         ErpCustomerDO customer,
                                                         AdminUserRespDTO creator,
                                                         AdminUserRespDTO saleUser,
                                                         ErpSaleOutItemDO item,
                                                         ErpProductRespVO product,
                                                         ErpWarehouseDO warehouse,
                                                         boolean fillMainFields) {
        ErpSaleOutExportRespVO row = fillMainFields
                ? BeanUtils.toBean(saleOut, ErpSaleOutExportRespVO.class)
                : new ErpSaleOutExportRespVO();
        row.setCustomerName(fillMainFields && customer != null ? customer.getName() : null);
        row.setCreatorName(fillMainFields && creator != null ? creator.getNickname() : null);
        row.setSaleUserName(fillMainFields && saleUser != null ? saleUser.getNickname() : null);
        if (item == null) {
            return row;
        }
        row.setProductCode(product != null ? product.getCode() : null);
        row.setProductName(product != null ? product.getName() : null);
        row.setProductUnitName(product != null ? product.getUnitName() : null);
        row.setWarehouseName(warehouse != null ? warehouse.getName() : null);
        row.setItemCount(item.getCount());
        row.setProductPrice(item.getProductPrice());
        row.setItemTotalPrice(item.getTotalPrice());
        row.setItemTaxPercent(item.getTaxPercent());
        row.setItemTaxPrice(item.getTaxPrice());
        row.setBrand(item.getBrand());
        row.setVehicleModel(item.getVehicleModel());
        row.setStandard(item.getStandard());
        row.setOriginPlace(item.getOriginPlace());
        row.setWarehousePosition(item.getWarehousePosition());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private AdminUserRespDTO getCreator(Map<Long, AdminUserRespDTO> userMap, String creator) {
        if (creator == null) {
            return null;
        }
        try {
            return userMap.get(Long.parseLong(creator));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
