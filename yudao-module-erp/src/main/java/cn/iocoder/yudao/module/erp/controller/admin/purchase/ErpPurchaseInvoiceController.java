package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoicePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInvoiceService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_PROCESS_NOT_SUPPORT;

@Tag(name = "管理后台 - ERP 采购票据")
@RestController
@RequestMapping("/erp/purchase-invoice")
@Validated
public class ErpPurchaseInvoiceController {

    @Resource
    private ErpPurchaseInvoiceService purchaseInvoiceService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建采购票据")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:create')")
    public CommonResult<Long> createPurchaseInvoice(@Valid @RequestBody ErpPurchaseInvoiceSaveReqVO createReqVO) {
        return success(purchaseInvoiceService.createPurchaseInvoice(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购票据")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:update')")
    public CommonResult<Boolean> updatePurchaseInvoice(@Valid @RequestBody ErpPurchaseInvoiceSaveReqVO updateReqVO) {
        purchaseInvoiceService.updatePurchaseInvoice(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新采购票据状态")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:update-status')")
    public CommonResult<Boolean> updatePurchaseInvoiceStatus(@RequestParam("id") Long id,
                                                             @RequestParam("status") Integer status) {
        if (!Integer.valueOf(20).equals(status)) {
            throw exception(PURCHASE_INVOICE_PROCESS_NOT_SUPPORT);
        }
        purchaseInvoiceService.updatePurchaseInvoiceStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购票据")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:delete')")
    public CommonResult<Boolean> deletePurchaseInvoice(@RequestParam("ids") List<Long> ids) {
        purchaseInvoiceService.deletePurchaseInvoice(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购票据")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:query')")
    public CommonResult<ErpPurchaseInvoiceRespVO> getPurchaseInvoice(@RequestParam("id") Long id) {
        ErpPurchaseInvoiceDO purchaseInvoice = purchaseInvoiceService.getPurchaseInvoice(id);
        if (purchaseInvoice == null) {
            return success(null);
        }
        List<ErpPurchaseInvoiceItemDO> itemList = purchaseInvoiceService.getPurchaseInvoiceItemListByInvoiceId(id);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(itemList, ErpPurchaseInvoiceItemDO::getProductId));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                Collections.singleton(purchaseInvoice.getSupplierId()));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                collectUserIds(Collections.singletonList(purchaseInvoice)));

        ErpPurchaseInvoiceRespVO respVO = BeanUtils.toBean(purchaseInvoice, ErpPurchaseInvoiceRespVO.class);
        fillInvoiceRespItems(respVO, itemList, productMap);
        MapUtils.findAndThen(supplierMap, respVO.getSupplierId(), supplier -> respVO.setSupplierName(supplier.getName()));
        fillUserNames(respVO, userMap);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购票据分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:query')")
    public CommonResult<PageResult<ErpPurchaseInvoiceRespVO>> getPurchaseInvoicePage(@Valid ErpPurchaseInvoicePageReqVO pageReqVO) {
        PageResult<ErpPurchaseInvoiceDO> pageResult = purchaseInvoiceService.getPurchaseInvoicePage(pageReqVO);
        return success(buildPurchaseInvoiceVOPageResult(pageResult));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购票据 Excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseInvoiceExcel(@Valid ErpPurchaseInvoicePageReqVO pageReqVO,
                                           HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpPurchaseInvoiceRespVO> list = buildPurchaseInvoiceVOPageResult(
                purchaseInvoiceService.getPurchaseInvoicePage(pageReqVO)).getList();
        ExcelUtils.write(response, "采购票据.xls", "数据", ErpPurchaseInvoiceExportRespVO.class,
                buildPurchaseInvoiceExportList(list));
    }

    private PageResult<ErpPurchaseInvoiceRespVO> buildPurchaseInvoiceVOPageResult(PageResult<ErpPurchaseInvoiceDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpPurchaseInvoiceItemDO> itemList = purchaseInvoiceService.getPurchaseInvoiceItemListByInvoiceIds(
                convertSet(pageResult.getList(), ErpPurchaseInvoiceDO::getId));
        Map<Long, List<ErpPurchaseInvoiceItemDO>> itemMap = convertMultiMap(itemList, ErpPurchaseInvoiceItemDO::getInvoiceId);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(itemList, ErpPurchaseInvoiceItemDO::getProductId));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpPurchaseInvoiceDO::getSupplierId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(collectUserIds(pageResult.getList()));

        PageResult<ErpPurchaseInvoiceRespVO> respPage = BeanUtils.toBean(pageResult, ErpPurchaseInvoiceRespVO.class);
        respPage.getList().forEach(invoice -> {
            fillInvoiceRespItems(invoice, itemMap.get(invoice.getId()), productMap);
            MapUtils.findAndThen(supplierMap, invoice.getSupplierId(), supplier -> invoice.setSupplierName(supplier.getName()));
            fillUserNames(invoice, userMap);
        });
        return respPage;
    }

    private void fillInvoiceRespItems(ErpPurchaseInvoiceRespVO invoice, List<ErpPurchaseInvoiceItemDO> itemList,
                                      Map<Long, ErpProductRespVO> productMap) {
        List<ErpPurchaseInvoiceRespVO.Item> items = BeanUtils.toBean(itemList, ErpPurchaseInvoiceRespVO.Item.class);
        if (items == null) {
            items = new ArrayList<>();
        }
        for (ErpPurchaseInvoiceRespVO.Item item : items) {
            ErpProductRespVO product = productMap.get(item.getProductId());
            if (product == null) {
                continue;
            }
            item.setProductName(product.getName());
            item.setProductCode(product.getCode());
            if (item.getProductUnitName() == null) {
                item.setProductUnitName(product.getUnitName());
            }
            if (item.getProductBarCode() == null) {
                item.setProductBarCode(product.getBarCode());
            }
        }
        invoice.setItems(items);
        invoice.setProductNames(CollUtil.join(items, ", ", ErpPurchaseInvoiceRespVO.Item::getProductName));
    }

    private List<ErpPurchaseInvoiceExportRespVO> buildPurchaseInvoiceExportList(List<ErpPurchaseInvoiceRespVO> list) {
        List<ErpPurchaseInvoiceExportRespVO> rows = new ArrayList<>();
        for (ErpPurchaseInvoiceRespVO invoice : list) {
            if (CollUtil.isEmpty(invoice.getItems())) {
                rows.add(buildPurchaseInvoiceExportRow(invoice, null, true));
                continue;
            }
            for (int i = 0; i < invoice.getItems().size(); i++) {
                rows.add(buildPurchaseInvoiceExportRow(invoice, invoice.getItems().get(i), i == 0));
            }
        }
        return rows;
    }

    private ErpPurchaseInvoiceExportRespVO buildPurchaseInvoiceExportRow(ErpPurchaseInvoiceRespVO invoice,
                                                                         ErpPurchaseInvoiceRespVO.Item item,
                                                                         boolean fillMainFields) {
        ErpPurchaseInvoiceExportRespVO row = fillMainFields
                ? BeanUtils.toBean(invoice, ErpPurchaseInvoiceExportRespVO.class)
                : new ErpPurchaseInvoiceExportRespVO();
        if (fillMainFields) {
            row.setSupplierName(invoice.getSupplierName());
            row.setCreatorName(invoice.getCreatorName());
            row.setRemark(invoice.getRemark());
        }
        if (item == null) {
            return row;
        }
        row.setSourceInNo(item.getSourceInNo());
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setProductUnitName(item.getProductUnitName());
        row.setCount(item.getCount());
        row.setProductPrice(item.getProductPrice());
        row.setTaxExclusivePrice(item.getTaxExclusivePrice());
        row.setTaxPercent(item.getTaxPercent());
        row.setTaxPrice(item.getTaxPrice());
        row.setItemTotalPrice(item.getTotalPrice());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private Set<Long> collectUserIds(List<ErpPurchaseInvoiceDO> list) {
        Set<Long> userIds = new LinkedHashSet<>();
        list.forEach(invoice -> {
            addUserId(userIds, invoice.getCreator());
            addUserId(userIds, invoice.getUpdater());
        });
        return userIds;
    }

    private void fillUserNames(ErpPurchaseInvoiceRespVO invoice, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(invoice.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> invoice.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(invoice.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> invoice.setUpdaterName(user.getNickname()));
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
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
