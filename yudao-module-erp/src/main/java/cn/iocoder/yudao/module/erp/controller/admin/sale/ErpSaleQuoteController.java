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
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteConvertCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuotePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleQuoteService;
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
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;

@Tag(name = "管理后台 - ERP 报价订单")
@RestController
@RequestMapping("/erp/sale-quote")
@Validated
public class ErpSaleQuoteController {

    @Resource
    private ErpSaleQuoteService saleQuoteService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建报价订单")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:create')")
    public CommonResult<Long> createSaleQuote(@Valid @RequestBody ErpSaleQuoteSaveReqVO createReqVO) {
        return success(saleQuoteService.createSaleQuote(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新报价订单")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:update')")
    public CommonResult<Boolean> updateSaleQuote(@Valid @RequestBody ErpSaleQuoteSaveReqVO updateReqVO) {
        saleQuoteService.updateSaleQuote(updateReqVO);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "审核报价订单，并自动生成销售单")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:approve')")
    public CommonResult<Long> approveSaleQuote(@RequestParam("id") Long id) {
        return success(saleQuoteService.approveSaleQuote(id));
    }

    @PostMapping("/convert-cart")
    @Operation(summary = "报价订单转销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:convert-cart')")
    public CommonResult<Long> convertSaleQuoteToCart(@Valid @RequestBody ErpSaleQuoteConvertCartReqVO reqVO) {
        return success(saleQuoteService.convertToCart(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除报价订单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:delete')")
    public CommonResult<Boolean> deleteSaleQuote(@RequestParam("ids") List<Long> ids) {
        saleQuoteService.deleteSaleQuote(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得报价订单")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<ErpSaleQuoteRespVO> getSaleQuote(@RequestParam("id") Long id) {
        ErpSaleQuoteDO quote = saleQuoteService.getSaleQuote(id);
        if (quote == null) {
            return success(null);
        }
        return success(buildSaleQuoteRespVO(quote, saleQuoteService.getSaleQuoteItemListByQuoteId(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得报价订单分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<PageResult<ErpSaleQuoteRespVO>> getSaleQuotePage(@Valid ErpSaleQuotePageReqVO pageReqVO) {
        PageResult<ErpSaleQuoteDO> pageResult = saleQuoteService.getSaleQuotePage(pageReqVO);
        return success(buildSaleQuoteVOPageResult(pageResult));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出报价订单 Excel")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSaleQuoteExcel(@Valid ErpSaleQuotePageReqVO pageReqVO,
                                     HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpSaleQuoteRespVO> list = buildSaleQuoteVOPageResult(
                saleQuoteService.getSaleQuotePage(pageReqVO)).getList();
        ExcelUtils.write(response, "报价订单.xls", "数据", ErpSaleQuoteExportRespVO.class, buildSaleQuoteExportList(list));
    }

    @GetMapping("/export-import-template")
    @Operation(summary = "获得报价订单导入模板")
    public void exportImportTemplate(HttpServletResponse response) throws IOException {
        ErpSaleQuoteImportExcelVO example = new ErpSaleQuoteImportExcelVO();
        example.setProductCode("P0001");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("100.00"));
        ExcelUtils.write(response, "报价订单导入模板.xls", "报价订单", ErpSaleQuoteImportExcelVO.class, Collections.singletonList(example));
    }

    @PostMapping("/import")
    @Operation(summary = "导入报价订单明细")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:create')")
    public CommonResult<ErpSaleQuoteImportRespVO> importSaleQuote(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpSaleQuoteImportExcelVO> list = ExcelUtils.read(file, ErpSaleQuoteImportExcelVO.class);
        return success(saleQuoteService.parseImportData(list));
    }

    private PageResult<ErpSaleQuoteRespVO> buildSaleQuoteVOPageResult(PageResult<ErpSaleQuoteDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpSaleQuoteItemDO> itemList = saleQuoteService.getSaleQuoteItemListByQuoteIds(
                convertSet(pageResult.getList(), ErpSaleQuoteDO::getId));
        Map<Long, List<ErpSaleQuoteItemDO>> itemMap = convertMultiMap(itemList, ErpSaleQuoteItemDO::getQuoteId);
        Set<Long> userIds = convertSet(pageResult.getList(), quote -> parseLongSafely(quote.getCreator()));
        userIds.addAll(convertSet(pageResult.getList(), quote -> parseLongSafely(quote.getUpdater())));
        userIds.remove(null);
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isNotEmpty(userIds)
                ? adminUserApi.getUserMap(userIds) : Collections.emptyMap();
        return BeanUtils.toBean(pageResult, ErpSaleQuoteRespVO.class,
                quote -> fillRelation(quote, itemMap.get(quote.getId()), userMap));
    }

    private ErpSaleQuoteRespVO buildSaleQuoteRespVO(ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> items) {
        Long creatorId = parseLongSafely(quote.getCreator());
        Long updaterId = parseLongSafely(quote.getUpdater());
        List<Long> userIds = new ArrayList<>();
        if (creatorId != null) {
            userIds.add(creatorId);
        }
        if (updaterId != null && !updaterId.equals(creatorId)) {
            userIds.add(updaterId);
        }
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isNotEmpty(userIds)
                ? adminUserApi.getUserMap(userIds) : Collections.emptyMap();
        return BeanUtils.toBean(quote, ErpSaleQuoteRespVO.class, vo -> fillRelation(vo, items, userMap));
    }

    private void fillRelation(ErpSaleQuoteRespVO vo, List<ErpSaleQuoteItemDO> items,
                              Map<Long, AdminUserRespDTO> userMap) {
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(convertSet(items, ErpSaleQuoteItemDO::getProductId));
        vo.setItems(BeanUtils.toBean(items, ErpSaleQuoteRespVO.Item.class,
                item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName()))));
        vo.setProductNames(CollUtil.join(vo.getItems(), "，", ErpSaleQuoteRespVO.Item::getProductName));
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(convertSet(Collections.singletonList(vo), ErpSaleQuoteRespVO::getCustomerId));
        MapUtils.findAndThen(customerMap, vo.getCustomerId(), customer -> vo.setCustomerName(customer.getName()));
        Long creatorId = parseLongSafely(vo.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> vo.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseLongSafely(vo.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> vo.setUpdaterName(user.getNickname()));
        }
        // 填充业务员名称
        if (vo.getSaleUserId() != null) {
            AdminUserRespDTO user = adminUserApi.getUser(vo.getSaleUserId());
            if (user != null) {
                vo.setSaleUserName(user.getNickname());
            }
        }
        // 反查生成的销售单号
        if (ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(vo.getStatus())) {
            ErpSaleOutDO saleOut = saleOutMapper.selectBySourceTypeAndSourceId(
                    ErpSaleBizSourceTypeEnum.QUOTE.getType(), vo.getId());
            if (saleOut != null) {
                vo.setGeneratedSaleOutNo(saleOut.getNo());
            }
        }
    }

    private static Long parseLongSafely(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<ErpSaleQuoteExportRespVO> buildSaleQuoteExportList(List<ErpSaleQuoteRespVO> list) {
        List<ErpSaleQuoteExportRespVO> rows = new ArrayList<>();
        for (ErpSaleQuoteRespVO quote : list) {
            if (CollUtil.isEmpty(quote.getItems())) {
                rows.add(buildSaleQuoteExportRow(quote, null, true));
                continue;
            }
            for (int i = 0; i < quote.getItems().size(); i++) {
                rows.add(buildSaleQuoteExportRow(quote, quote.getItems().get(i), i == 0));
            }
        }
        return rows;
    }

    private ErpSaleQuoteExportRespVO buildSaleQuoteExportRow(ErpSaleQuoteRespVO quote,
                                                             ErpSaleQuoteRespVO.Item item,
                                                             boolean fillQuoteFields) {
        ErpSaleQuoteExportRespVO row = fillQuoteFields
                ? BeanUtils.toBean(quote, ErpSaleQuoteExportRespVO.class)
                : new ErpSaleQuoteExportRespVO();
        if (item == null) {
            return row;
        }
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setProductUnitName(item.getProductUnitName());
        row.setItemCount(item.getCount());
        row.setConvertedCount(item.getConvertedCount());
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

}
