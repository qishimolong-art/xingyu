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
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartConvertQuoteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartUpdateFileReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
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
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;

@Tag(name = "管理后台 - ERP 销售手推车")
@RestController
@RequestMapping("/erp/sale-cart")
@Validated
public class ErpSaleCartController {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_cart";

    @Resource
    private ErpSaleCartService saleCartService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:create')")
    public CommonResult<Long> createSaleCart(@Valid @RequestBody ErpSaleCartSaveReqVO createReqVO) {
        return success(saleCartService.createSaleCart(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:update')")
    public CommonResult<Boolean> updateSaleCart(@Valid @RequestBody ErpSaleCartSaveReqVO updateReqVO) {
        saleCartService.updateSaleCart(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-file")
    @Operation(summary = "更新销售手推车快递单")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:update')")
    public CommonResult<Boolean> updateSaleCartFile(@Valid @RequestBody ErpSaleCartUpdateFileReqVO updateReqVO) {
        saleCartService.updateSaleCartFile(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:submit')")
    public CommonResult<Boolean> submitSaleCart(@RequestParam("id") Long id) {
        saleCartService.submitSaleCart(id);
        return success(true);
    }

    @PutMapping("/first-approve")
    @Operation(summary = "初审销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:first-approve')")
    public CommonResult<Boolean> firstApproveSaleCart(@RequestParam("id") Long id) {
        saleCartService.firstApproveSaleCart(id);
        return success(true);
    }

    @PutMapping("/final-approve")
    @Operation(summary = "终审销售手推车，并自动生成销售单")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:final-approve')")
    public CommonResult<List<Long>> finalApproveSaleCart(@RequestParam("id") Long id) {
        return success(saleCartService.finalApproveSaleCart(id));
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:reject')")
    public CommonResult<Boolean> rejectSaleCart(@RequestParam("id") Long id) {
        saleCartService.rejectSaleCart(id);
        return success(true);
    }

    @PostMapping("/convert-quote")
    @Operation(summary = "销售手推车部分商品转报价订单")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:convert-quote')")
    public CommonResult<Long> convertSaleCartToQuote(@Valid @RequestBody ErpSaleCartConvertQuoteReqVO reqVO) {
        return success(saleCartService.convertToQuote(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除销售手推车")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:delete')")
    public CommonResult<Boolean> deleteSaleCart(@RequestParam("ids") List<Long> ids) {
        saleCartService.deleteSaleCart(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:query')")
    public CommonResult<ErpSaleCartRespVO> getSaleCart(@RequestParam("id") Long id) {
        ErpSaleCartDO cart = saleCartService.getSaleCart(id);
        if (cart == null) {
            return success(null);
        }
        ErpSaleCartRespVO respVO = buildSaleCartRespVO(cart, saleCartService.getSaleCartItemListByCartId(id));
        fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售手推车分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:query')")
    public CommonResult<PageResult<ErpSaleCartRespVO>> getSaleCartPage(@Valid ErpSaleCartPageReqVO pageReqVO) {
        PageResult<ErpSaleCartDO> pageResult = saleCartService.getSaleCartPage(pageReqVO);
        PageResult<ErpSaleCartRespVO> respResult = buildSaleCartVOPageResult(pageResult);
        fieldPermissionMasker.maskFormsWithItems(FIELD_PERMISSION_MODULE, respResult.getList());
        return success(respResult);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出销售手推车 Excel")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSaleCartExcel(@Valid ErpSaleCartPageReqVO pageReqVO,
                                    HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpSaleCartRespVO> list = buildSaleCartVOPageResult(
                saleCartService.getSaleCartPage(pageReqVO)).getList();
        fieldPermissionMasker.maskFormsWithItems(FIELD_PERMISSION_MODULE, list);
        List<ErpSaleCartExportRespVO> rows = buildSaleCartExportList(list);
        fieldPermissionMasker.maskExportRows(FIELD_PERMISSION_MODULE, rows);
        ExcelUtils.write(response, "销售手推车.xls", "数据", ErpSaleCartExportRespVO.class, rows);
    }

    @GetMapping("/export-import-template")
    @Operation(summary = "获得销售手推车导入模板")
    public void exportImportTemplate(HttpServletResponse response) throws IOException {
        ErpSaleCartImportExcelVO example = new ErpSaleCartImportExcelVO();
        example.setProductCode("P0001");
        example.setWarehouseName("默认仓");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("100.00"));
        example.setBrand("品牌");
        example.setVehicleModel("车型");
        example.setStandard("规格");
        example.setRemark("备注");
        ExcelUtils.write(response, "销售手推车导入模板.xls", "销售手推车", ErpSaleCartImportExcelVO.class, Collections.singletonList(example));
    }

    @PostMapping("/import")
    @Operation(summary = "导入销售手推车明细")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:create')")
    public CommonResult<ErpSaleCartImportRespVO> importSaleCart(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpSaleCartImportExcelVO> list = ExcelUtils.read(file, ErpSaleCartImportExcelVO.class);
        return success(saleCartService.parseImportData(list));
    }

    private PageResult<ErpSaleCartRespVO> buildSaleCartVOPageResult(PageResult<ErpSaleCartDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpSaleCartItemDO> itemList = saleCartService.getSaleCartItemListByCartIds(
                convertSet(pageResult.getList(), ErpSaleCartDO::getId));
        Map<Long, List<ErpSaleCartItemDO>> itemMap = convertMultiMap(itemList, ErpSaleCartItemDO::getCartId);
        return BeanUtils.toBean(pageResult, ErpSaleCartRespVO.class,
                cart -> fillRelation(cart, itemMap.get(cart.getId())));
    }

    private ErpSaleCartRespVO buildSaleCartRespVO(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        return BeanUtils.toBean(cart, ErpSaleCartRespVO.class, vo -> fillRelation(vo, items));
    }

    private void fillRelation(ErpSaleCartRespVO vo, List<ErpSaleCartItemDO> items) {
        List<ErpSaleCartItemDO> safeItems = CollUtil.isEmpty(items) ? Collections.emptyList() : items;
        Map<Long, ErpProductRespVO> productMap = CollUtil.isEmpty(safeItems)
                ? Collections.emptyMap()
                : productService.getProductVOMap(convertSet(safeItems, ErpSaleCartItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(safeItems)
                ? Collections.emptyMap()
                : warehouseService.getWarehouseMap(convertSet(safeItems, ErpSaleCartItemDO::getWarehouseId));
        List<ErpSaleCartRespVO.Item> respItems = BeanUtils.toBean(safeItems, ErpSaleCartRespVO.Item.class,
                item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                        .setProductCode(product.getCode()).setLockCount(product.getLockCount())));
        vo.setItems(respItems == null ? Collections.emptyList() : respItems);
        vo.getItems().forEach(item ->
                MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> item.setWarehouseName(warehouse.getName())));
        vo.setProductNames(CollUtil.join(vo.getItems(), "，", ErpSaleCartRespVO.Item::getProductName));
        if (vo.getCustomerId() != null) {
            Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(Collections.singleton(vo.getCustomerId()));
            MapUtils.findAndThen(customerMap, vo.getCustomerId(), customer -> vo.setCustomerName(customer.getName()));
        }
        if (vo.getCreator() != null) {
            try {
                AdminUserRespDTO creator = adminUserApi.getUser(Long.parseLong(vo.getCreator()));
                if (creator != null) {
                    vo.setCreatorName(creator.getNickname());
                }
            } catch (NumberFormatException ignored) {
                // ignore invalid creator value
            }
        }
        if (vo.getUpdater() != null) {
            try {
                AdminUserRespDTO updater = adminUserApi.getUser(Long.parseLong(vo.getUpdater()));
                if (updater != null) {
                    vo.setUpdaterName(updater.getNickname());
                }
            } catch (NumberFormatException ignored) {
                // ignore invalid updater value
            }
        }
        if (vo.getSaleUserId() != null) {
            AdminUserRespDTO user = adminUserApi.getUser(vo.getSaleUserId());
            if (user != null) {
                vo.setSaleUserName(user.getNickname());
            }
        }
    }

    private List<ErpSaleCartExportRespVO> buildSaleCartExportList(List<ErpSaleCartRespVO> list) {
        List<ErpSaleCartExportRespVO> rows = new ArrayList<>();
        for (ErpSaleCartRespVO cart : list) {
            if (CollUtil.isEmpty(cart.getItems())) {
                rows.add(buildSaleCartExportRow(cart, null, true));
                continue;
            }
            for (int i = 0; i < cart.getItems().size(); i++) {
                rows.add(buildSaleCartExportRow(cart, cart.getItems().get(i), i == 0));
            }
        }
        return rows;
    }

    private ErpSaleCartExportRespVO buildSaleCartExportRow(ErpSaleCartRespVO cart,
                                                           ErpSaleCartRespVO.Item item,
                                                           boolean fillMainFields) {
        ErpSaleCartExportRespVO row = fillMainFields
                ? BeanUtils.toBean(cart, ErpSaleCartExportRespVO.class)
                : new ErpSaleCartExportRespVO();
        if (item == null) {
            return row;
        }
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setProductUnitName(item.getProductUnitName());
        row.setWarehouseName(item.getWarehouseName());
        row.setLockCount(item.getLockCount());
        row.setItemCount(item.getCount());
        row.setProductPrice(item.getProductPrice());
        row.setItemTotalPrice(item.getTotalPrice());
        row.setBrand(item.getBrand());
        row.setVehicleModel(item.getVehicleModel());
        row.setStandard(item.getStandard());
        row.setOriginPlace(item.getOriginPlace());
        row.setWarehousePosition(item.getWarehousePosition());
        row.setItemRemark(item.getRemark());
        return row;
    }

}
