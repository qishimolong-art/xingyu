package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.imports.ErpSaleImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleItemMatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleItemMatchRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteConvertCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuotePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleItemPriceReferenceFiller;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleQuoteService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleQuoteProgressService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
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
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;

@Tag(name = "管理后台 - ERP 报价订单")
@RestController
@RequestMapping("/erp/sale-quote")
@Validated
public class ErpSaleQuoteController {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_quote";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();
    private static final Map<String, String> ORDER_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "customerId", "customerName",
            "customerName", "customerName",
            "productId", "productIdentity",
            "productCode", "productIdentity",
            "productName", "productIdentity",
            "factoryCode", "productIdentity",
            "warehouseId", "warehouseName",
            "warehouseName", "warehouseName",
            "warehouse_name", "warehouseName",
            "count", "itemCount",
            "item_count", "itemCount",
            "itemCount", "itemCount",
            "productPrice", "productPrice",
            "giftFlag", "giftFlag",
            "taxPercent", "taxPercent",
            "remark", "remark",
            "item_remark", "itemRemark",
            "itemRemark", "itemRemark");
    private static final Map<String, String> DETAIL_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "productId", "productIdentity",
            "productCode", "productIdentity",
            "productName", "productIdentity",
            "factoryCode", "productIdentity",
            "warehouseId", "warehouseName",
            "warehouseName", "warehouseName",
            "count", "count",
            "productPrice", "productPrice",
            "giftFlag", "giftFlag",
            "remark", "remark",
            "item_remark", "remark",
            "itemRemark", "remark");

    @Resource
    private ErpSaleQuoteService saleQuoteService;
    @Resource
    private ErpSaleQuoteProgressService saleQuoteProgressService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleItemPriceReferenceFiller itemPriceReferenceFiller;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpFieldConfigService fieldConfigService;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private ErpCustomerDeptPermissionService customerDeptPermissionService;

    @PostMapping("/create")
    @Operation(summary = "创建报价订单")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:create')")
    public CommonResult<Long> createSaleQuote(@Valid @RequestBody ErpSaleQuoteSaveReqVO createReqVO) {
        return success(saleQuoteService.createSaleQuote(createReqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建报价订单草稿")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:create')")
    public CommonResult<Long> createSaleQuoteDraft(@RequestBody ErpSaleQuoteDraftCreateReqVO createReqVO) {
        return success(saleQuoteService.createSaleQuoteDraft(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新报价订单")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:update')")
    public CommonResult<Boolean> updateSaleQuote(@Valid @RequestBody ErpSaleQuoteSaveReqVO updateReqVO) {
        saleQuoteService.updateSaleQuote(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "更新报价订单草稿")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:update')")
    public CommonResult<Boolean> updateSaleQuoteDraft(@RequestBody ErpSaleQuoteDraftUpdateReqVO updateReqVO) {
        saleQuoteService.updateSaleQuoteDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/batch-update-items")
    @Operation(summary = "批量修改报价订单明细仓库/部门")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:update')")
    public CommonResult<Boolean> batchUpdateSaleQuoteItems(
            @Valid @RequestBody ErpSaleQuoteItemBatchUpdateReqVO updateReqVO) {
        saleQuoteService.batchUpdateSaleQuoteItems(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交报价订单草稿")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:create')")
    public CommonResult<Boolean> submitSaleQuote(@RequestParam("id") Long id) {
        saleQuoteService.submitSaleQuote(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "修改销售报价单备注")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:update')")
    public CommonResult<Boolean> updateSaleQuoteRemark(
            @Valid @RequestBody ErpSaleUpdateRemarkReqVO updateReqVO) {
        saleQuoteService.updateSaleQuoteRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "报价订单下订")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:approve')")
    public CommonResult<Long> approveSaleQuote(@RequestParam("id") Long id) {
        return success(saleQuoteService.approveSaleQuote(id));
    }

    @PutMapping("/reverse-order")
    @Operation(summary = "报价订单反下订")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:approve')")
    public CommonResult<Boolean> reverseOrderSaleQuote(@RequestParam("id") Long id) {
        saleQuoteService.reverseOrderSaleQuote(id);
        return success(true);
    }

    @PostMapping("/convert-cart")
    @Operation(summary = "报价订单分批出库并生成待审核销售手推车")
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
    public CommonResult<ErpSaleQuoteRespVO> getSaleQuote(@RequestParam("id") Long id,
                                                         @RequestParam(value = "includeItems", required = false,
                                                                 defaultValue = "true") Boolean includeItems,
                                                         @RequestParam(value = "includeStock", required = false,
                                                                 defaultValue = "false") Boolean includeStock) {
        ErpSaleQuoteDO quote = saleQuoteService.getSaleQuote(id);
        if (quote == null) {
            return success(null);
        }
        List<ErpSaleQuoteItemDO> items = Boolean.TRUE.equals(includeItems)
                ? saleQuoteService.getSaleQuoteItemListByQuoteId(id) : Collections.emptyList();
        ErpSaleQuoteRespVO respVO = buildSaleQuoteRespVO(quote, items);
        if (Boolean.TRUE.equals(includeStock)) {
            fillItemStock(respVO.getItems());
        }
        fieldPermissionMasker.maskSaleDetailFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<ErpSaleQuoteRespVO> getSaleQuote(Long id) {
        return getSaleQuote(id, true, false);
    }

    @GetMapping("/item-page")
    @Operation(summary = "获得报价订单明细分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<PageResult<ErpSaleQuoteRespVO.Item>> getSaleQuoteItemPage(
            @Valid ErpSaleQuoteItemPageReqVO pageReqVO) {
        ErpSaleQuoteDO quote = saleQuoteService.getSaleQuote(pageReqVO.getQuoteId());
        if (quote == null) {
            return success(PageResult.empty());
        }
        PageResult<ErpSaleQuoteItemDO> pageResult = saleQuoteService.getSaleQuoteItemPage(pageReqVO);
        ErpSaleQuoteRespVO respVO = BeanUtils.toBean(quote, ErpSaleQuoteRespVO.class);
        PageResult<ErpSaleQuoteRespVO.Item> respResult = new PageResult<>(
                buildSaleQuoteItemRespVOList(pageResult.getList()), pageResult.getTotal());
        if (Boolean.TRUE.equals(pageReqVO.getMask())) {
            fieldPermissionMasker.clearSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, respVO, respResult.getList());
        }
        return success(respResult);
    }

    @PostMapping("/item-page")
    @Operation(summary = "获得报价订单编辑态明细分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<PageResult<ErpSaleQuoteRespVO.Item>> getSaleQuoteEditItemPage(
            @Valid @RequestBody ErpSaleQuoteItemPageReqVO pageReqVO) {
        return getSaleQuoteItemPage(pageReqVO);
    }

    @PostMapping("/item-match")
    @Operation(summary = "匹配报价订单整单已有明细")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<ErpSaleItemMatchRespVO> matchSaleQuoteItems(
            @Valid @RequestBody ErpSaleItemMatchReqVO reqVO) {
        if (saleQuoteService.getSaleQuote(reqVO.getDocumentId()) == null) {
            return success(new ErpSaleItemMatchRespVO());
        }
        Set<Long> excludedIds = new HashSet<>(reqVO.getExcludedItemIds() == null
                ? Collections.emptyList() : reqVO.getExcludedItemIds());
        List<ErpSaleQuoteItemDO> items = new ArrayList<>(
                saleQuoteService.getSaleQuoteItemListByQuoteId(reqVO.getDocumentId()));
        items.removeIf(item -> excludedIds.contains(item.getId()));
        items.sort(Comparator.comparing(ErpSaleQuoteItemDO::getId));
        ErpSaleItemMatchRespVO result = new ErpSaleItemMatchRespVO();
        for (int keyIndex = 0; keyIndex < reqVO.getKeys().size(); keyIndex++) {
            ErpSaleItemMatchReqVO.Key key = reqVO.getKeys().get(keyIndex);
            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                ErpSaleQuoteItemDO item = items.get(itemIndex);
                if (Objects.equals(item.getProductId(), key.getProductId())
                        && Objects.equals(item.getWarehouseId(), key.getWarehouseId())
                        && Objects.equals(normalizeBatchNo(item.getBatchNo()), normalizeBatchNo(key.getBatchNo()))
                        && Objects.equals(Boolean.TRUE.equals(item.getGiftFlag()),
                                Boolean.TRUE.equals(key.getGiftFlag()))) {
                    result.getMatches().add(new ErpSaleItemMatchRespVO.Match(keyIndex, item.getId(),
                            itemIndex / reqVO.getPageSize() + 1));
                    break;
                }
            }
        }
        return success(result);
    }

    private static String normalizeBatchNo(String batchNo) {
        return batchNo == null ? "" : batchNo.trim();
    }

    @GetMapping("/warehouse-dept-simple-list")
    @Operation(summary = "获取报价订单批量修改仓库可用部门精简列表")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:update')")
    public CommonResult<List<DeptSimpleRespVO>> getWarehouseAvailableDeptSimpleList(
            @RequestParam("warehouseId") Long warehouseId) {
        return success(saleQuoteService.getWarehouseAvailableDeptSimpleList(warehouseId));
    }

    @GetMapping("/warehouse-dept-simple-page")
    @Operation(summary = "获取报价订单批量修改仓库可用部门分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:update')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getWarehouseAvailableDeptSimplePage(
            @RequestParam("warehouseId") Long warehouseId, @Valid PageParam pageReqVO) {
        return success(saleQuoteService.getWarehouseAvailableDeptSimplePage(warehouseId, pageReqVO));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "获取报价订单可见部门精简列表")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<List<DeptSimpleRespVO>> getVisibleDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList(FIELD_PERMISSION_MODULE));
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "获取报价订单可见部门精简分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getVisibleDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage(FIELD_PERMISSION_MODULE, pageReqVO));
    }

    @GetMapping("/customer-dept-simple-page")
    @Operation(summary = "获取报价订单客户可用部门分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getCustomerAvailableDeptSimplePage(
            @RequestParam("customerId") Long customerId, @Valid PageParam pageReqVO) {
        return success(customerDeptPermissionService.getAvailableDeptSimplePage(customerId, FIELD_PERMISSION_MODULE, pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获取报价订单业务员精简分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getSaleUserSimplePage(@Valid PageParam pageReqVO) {
        PageResult<AdminUserRespDTO> page = adminUserApi.getUserSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageReqVO.getKeyword(), pageReqVO);
        List<UserSimpleRespVO> list = convertList(page.getList(), user ->
                new UserSimpleRespVO(user.getId(), user.getNickname(), user.getDeptId(), null));
        return success(new PageResult<>(list, page.getTotal()));
    }

    @GetMapping("/page")
    @Operation(summary = "获得报价订单分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<PageResult<ErpSaleQuoteRespVO>> getSaleQuotePage(@Valid ErpSaleQuotePageReqVO pageReqVO) {
        PageResult<ErpSaleQuoteDO> pageResult = saleQuoteService.getSaleQuotePage(pageReqVO);
        PageResult<ErpSaleQuoteRespVO> respResult = Boolean.FALSE.equals(pageReqVO.getIncludeItems())
                ? buildSaleQuoteVOPageResultWithoutItems(pageResult)
                : buildSaleQuoteVOPageResult(pageResult);
        fieldPermissionMasker.maskSaleDetailFormsWithItems(FIELD_PERMISSION_MODULE, respResult.getList());
        return success(respResult);
    }

    @GetMapping("/summary")
    @Operation(summary = "获得报价订单查询汇总")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:query')")
    public CommonResult<ErpSaleQuoteSummaryRespVO> getSaleQuoteSummary(
            @Valid ErpSaleQuotePageReqVO pageReqVO) {
        ErpSaleQuoteSummaryRespVO summary = saleQuoteService.getSaleQuoteSummary(pageReqVO);
        fieldPermissionMasker.clearSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, summary);
        return success(summary);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出报价订单 Excel")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSaleQuoteExcel(@Valid ErpSaleQuotePageReqVO pageReqVO,
                                     @RequestParam(value = "fields", required = false) String fields,
                                     HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpSaleQuoteRespVO> list = buildSaleQuoteVOPageResult(
                saleQuoteService.getSaleQuotePage(pageReqVO)).getList();
        fieldPermissionMasker.maskSaleDetailFormsWithItems(FIELD_PERMISSION_MODULE, list);
        List<ErpSaleQuoteExportRespVO> rows = buildSaleQuoteExportList(list);
        fieldPermissionMasker.maskSaleDetailExportRows(FIELD_PERMISSION_MODULE, rows);
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpSaleQuoteExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE, false), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "报价订单.xls", "数据", ErpSaleQuoteExportRespVO.class, rows, includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get sale quote export fields")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getSaleQuoteExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpSaleQuoteExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE, false), EXPORT_FIELD_PERMISSION_MAP));
    }

    @GetMapping("/export-import-template")
    @Operation(summary = "获得报价订单导入模板")
    public void exportImportTemplate(HttpServletResponse response) throws IOException {
        ErpSaleQuoteImportExcelVO example = new ErpSaleQuoteImportExcelVO();
        example.setProductCode("P0001");
        example.setProductName("示例配件");
        example.setWarehouseName("默认仓");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("100.00"));
        example.setGiftFlag(Boolean.FALSE);
        example.setRemark("明细备注");
        ExcelUtils.writeImportTemplate(response, "报价订单明细导入模板.xls", "报价订单明细",
                ErpSaleQuoteImportExcelVO.class, Collections.singletonList(example), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.SALE_QUOTE, ErpSaleQuoteImportExcelVO.class,
                        DETAIL_IMPORT_FIELD_ALIAS_MAP));
    }

    @GetMapping("/get-order-import-template")
    @Operation(summary = "获得报价订单整单导入模板")
    public void getOrderImportTemplate(HttpServletResponse response) throws IOException {
        ErpSaleQuoteOrderImportExcelVO example = new ErpSaleQuoteOrderImportExcelVO();
        example.setCustomerName("示例客户");
        example.setRemark("整单备注");
        example.setProductCode("P0001");
        example.setProductName("示例配件一");
        example.setWarehouseName("默认仓库");
        example.setItemCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("100.00"));
        example.setGiftFlag(Boolean.FALSE);
        example.setItemRemark("明细备注");
        ErpSaleQuoteOrderImportExcelVO secondItem = new ErpSaleQuoteOrderImportExcelVO();
        secondItem.setProductCode("P0002");
        secondItem.setProductName("示例配件二");
        secondItem.setWarehouseName("默认仓库");
        secondItem.setItemCount(BigDecimal.ONE);
        secondItem.setProductPrice(new BigDecimal("50.00"));
        secondItem.setGiftFlag(Boolean.FALSE);
        ExcelUtils.writeImportTemplate(response, "报价订单整单导入模板.xls", "报价订单整单",
                ErpSaleQuoteOrderImportExcelVO.class, Arrays.asList(example, secondItem), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.SALE_QUOTE, ErpSaleQuoteOrderImportExcelVO.class,
                        ORDER_IMPORT_FIELD_ALIAS_MAP));
    }

    @PostMapping("/import")
    @Operation(summary = "导入报价订单明细")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:create')")
    public CommonResult<ErpSaleQuoteImportRespVO> importSaleQuote(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpSaleQuoteImportExcelVO> list = ExcelUtils.read(file, ErpSaleQuoteImportExcelVO.class);
        return success(saleQuoteService.parseImportData(list));
    }

    @PostMapping("/import-order")
    @Operation(summary = "导入报价订单整单")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:create')")
    public CommonResult<ErpSaleImportResultRespVO> importSaleQuoteOrder(@RequestParam("file") MultipartFile file) throws Exception {
        return success(saleQuoteService.importSaleQuoteOrderList(
                ExcelUtils.read(file, ErpSaleQuoteOrderImportExcelVO.class)));
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "下载报价订单导入错误数据")
    @PreAuthorize("@ss.hasPermission('erp:sale-quote:create')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, FIELD_PERMISSION_MODULE, response);
    }

    private PageResult<ErpSaleQuoteRespVO> buildSaleQuoteVOPageResult(PageResult<ErpSaleQuoteDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Set<Long> quoteIds = convertSet(pageResult.getList(), ErpSaleQuoteDO::getId);
        List<ErpSaleQuoteItemDO> itemList = saleQuoteService.getSaleQuoteItemListByQuoteIds(quoteIds);
        Map<Long, List<ErpSaleQuoteItemDO>> itemMap = convertMultiMap(itemList, ErpSaleQuoteItemDO::getQuoteId);
        Map<Long, ErpProductRespVO> productMap = CollUtil.isEmpty(itemList)
                ? Collections.emptyMap()
                : getProductVOMapIgnoreDataPermission(convertSet(itemList, ErpSaleQuoteItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(itemList)
                ? Collections.emptyMap()
                : getWarehouseMapIgnoreDataPermission(convertSet(itemList, ErpSaleQuoteItemDO::getWarehouseId));
        Set<Long> customerIds = convertSet(pageResult.getList(), ErpSaleQuoteDO::getCustomerId);
        customerIds.remove(null);
        Map<Long, ErpCustomerDO> customerMap = CollUtil.isEmpty(customerIds)
                ? Collections.emptyMap() : customerService.getCustomerMap(customerIds);
        Set<Long> userIds = convertSet(pageResult.getList(), quote -> parseLongSafely(quote.getCreator()));
        userIds.addAll(convertSet(pageResult.getList(), quote -> parseLongSafely(quote.getUpdater())));
        userIds.addAll(convertSet(pageResult.getList(), ErpSaleQuoteDO::getSaleUserId));
        userIds.remove(null);
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isNotEmpty(userIds)
                ? adminUserApi.getUserMap(userIds) : Collections.emptyMap();
        Set<Long> deptIds = convertSet(pageResult.getList(), ErpSaleQuoteDO::getDeptId);
        deptIds.addAll(convertSet(itemList, ErpSaleQuoteItemDO::getDeptId));
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isNotEmpty(deptIds)
                ? deptApi.getDeptMap(deptIds) : Collections.emptyMap();
        List<ErpSaleOutDO> saleOutList = saleOutMapper.selectListBySourceTypeAndSourceIds(
                ErpSaleBizSourceTypeEnum.QUOTE.getType(),
                convertSet(pageResult.getList(), ErpSaleQuoteDO::getId,
                        quote -> ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(quote.getStatus())));
        Map<Long, ErpSaleOutDO> saleOutMap = convertMap(saleOutList, ErpSaleOutDO::getSourceId);
        PageResult<ErpSaleQuoteRespVO> result = BeanUtils.toBean(pageResult, ErpSaleQuoteRespVO.class,
                quote -> fillRelation(quote, itemMap.get(quote.getId()), productMap, warehouseMap, customerMap, userMap, deptMap, saleOutMap));
        applyProgress(result.getList());
        return result;
    }

    private PageResult<ErpSaleQuoteRespVO> buildSaleQuoteVOPageResultWithoutItems(PageResult<ErpSaleQuoteDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Set<Long> customerIds = convertSet(pageResult.getList(), ErpSaleQuoteDO::getCustomerId);
        customerIds.remove(null);
        Map<Long, ErpCustomerDO> customerMap = CollUtil.isEmpty(customerIds)
                ? Collections.emptyMap() : customerService.getCustomerMap(customerIds);
        Set<Long> userIds = convertSet(pageResult.getList(), quote -> parseLongSafely(quote.getCreator()));
        userIds.addAll(convertSet(pageResult.getList(), quote -> parseLongSafely(quote.getUpdater())));
        userIds.addAll(convertSet(pageResult.getList(), ErpSaleQuoteDO::getSaleUserId));
        userIds.remove(null);
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isNotEmpty(userIds)
                ? adminUserApi.getUserMap(userIds) : Collections.emptyMap();
        Set<Long> deptIds = convertSet(pageResult.getList(), ErpSaleQuoteDO::getDeptId);
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isNotEmpty(deptIds)
                ? deptApi.getDeptMap(deptIds) : Collections.emptyMap();
        List<ErpSaleOutDO> saleOutList = saleOutMapper.selectListBySourceTypeAndSourceIds(
                ErpSaleBizSourceTypeEnum.QUOTE.getType(),
                convertSet(pageResult.getList(), ErpSaleQuoteDO::getId,
                        quote -> ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(quote.getStatus())));
        Map<Long, ErpSaleOutDO> saleOutMap = convertMap(saleOutList, ErpSaleOutDO::getSourceId);
        PageResult<ErpSaleQuoteRespVO> result = BeanUtils.toBean(pageResult, ErpSaleQuoteRespVO.class,
                quote -> fillMainRelation(quote, customerMap, userMap, deptMap, saleOutMap));
        applyProgress(result.getList());
        return result;
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
        if (quote.getSaleUserId() != null) {
            userIds.add(quote.getSaleUserId());
        }
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isNotEmpty(userIds)
                ? adminUserApi.getUserMap(userIds) : Collections.emptyMap();
        Map<Long, ErpProductRespVO> productMap = CollUtil.isEmpty(items)
                ? Collections.emptyMap()
                : getProductVOMapIgnoreDataPermission(convertSet(items, ErpSaleQuoteItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(items)
                ? Collections.emptyMap()
                : getWarehouseMapIgnoreDataPermission(convertSet(items, ErpSaleQuoteItemDO::getWarehouseId));
        Map<Long, ErpCustomerDO> customerMap = quote.getCustomerId() == null
                ? Collections.emptyMap()
                : customerService.getCustomerMap(Collections.singleton(quote.getCustomerId()));
        Map<Long, ErpSaleOutDO> saleOutMap = Collections.emptyMap();
        if (ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(quote.getStatus())) {
            saleOutMap = convertMap(saleOutMapper.selectListBySourceTypeAndSourceIds(
                    ErpSaleBizSourceTypeEnum.QUOTE.getType(), Collections.singleton(quote.getId())),
                    ErpSaleOutDO::getSourceId);
        }
        Map<Long, ErpSaleOutDO> finalSaleOutMap = saleOutMap;
        Set<Long> deptIds = convertSet(items, ErpSaleQuoteItemDO::getDeptId);
        if (quote.getDeptId() != null) {
            deptIds.add(quote.getDeptId());
        }
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds)
                ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        ErpSaleQuoteRespVO result = BeanUtils.toBean(quote, ErpSaleQuoteRespVO.class,
                vo -> fillRelation(vo, items, productMap, warehouseMap, customerMap, userMap, deptMap, finalSaleOutMap));
        applyProgress(Collections.singletonList(result));
        return result;
    }

    private List<ErpSaleQuoteRespVO.Item> buildSaleQuoteItemRespVOList(List<ErpSaleQuoteItemDO> items) {
        List<ErpSaleQuoteItemDO> safeItems = CollUtil.isEmpty(items) ? Collections.emptyList() : items;
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(safeItems, ErpSaleQuoteItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(
                convertSet(safeItems, ErpSaleQuoteItemDO::getWarehouseId));
        Set<Long> deptIds = convertSet(safeItems, ErpSaleQuoteItemDO::getDeptId);
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds)
                ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        Set<Long> productIds = convertSet(safeItems, ErpSaleQuoteItemDO::getProductId);
        Map<Long, BigDecimal> lastSalePriceMap = CollUtil.isEmpty(productIds)
                ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> saleOutItemMapper.selectLatestSalePriceMap(productIds));
        List<ErpSaleQuoteRespVO.Item> result = BeanUtils.toBean(safeItems, ErpSaleQuoteRespVO.Item.class, item -> {
            item.setLastSalePrice(lastSalePriceMap.get(item.getProductId()));
            MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                    .setProductCode(product.getCode()).setProductBarCode(product.getBarCode())
                    .setProductUnitName(product.getUnitName()).setBatchNoEnabled(product.getBatchNoEnabled()));
            MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> {
                item.setWarehouseName(warehouse.getName());
                item.setWarehouseDeptId(warehouse.getDeptId());
                MapUtils.findAndThen(deptMap, warehouse.getDeptId(), dept -> item.setWarehouseDeptName(dept.getName()));
            });
            MapUtils.findAndThen(deptMap, item.getDeptId(), dept -> item.setDeptName(dept.getName()));
            item.setRemainingCount((item.getCount() != null ? item.getCount() : BigDecimal.ZERO)
                    .subtract(item.getConvertedCount() != null ? item.getConvertedCount() : BigDecimal.ZERO)
                    .max(BigDecimal.ZERO));
        });
        fillItemStock(result);
        itemPriceReferenceFiller.fill(result);
        return result;
    }

    private Map<Long, ErpProductRespVO> getProductVOMapIgnoreDataPermission(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(productIds));
    }

    private void fillItemStock(List<ErpSaleQuoteRespVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Set<Long> productIds = convertSet(items, ErpSaleQuoteRespVO.Item::getProductId);
        Set<Long> warehouseIds = convertSet(items, ErpSaleQuoteRespVO.Item::getWarehouseId);
        productIds.remove(null);
        warehouseIds.remove(null);
        if (CollUtil.isEmpty(productIds) || CollUtil.isEmpty(warehouseIds)) {
            items.forEach(item -> {
                item.setStockCount(BigDecimal.ZERO);
                item.setAvailableCount(BigDecimal.ZERO);
            });
            return;
        }
        Map<String, ErpStockDO> stockMap = DataPermissionUtils.executeIgnore(() ->
                stockService.getStockMap(productIds, warehouseIds));
        Map<String, BigDecimal> occupiedCountMap = DataPermissionUtils.executeIgnore(() ->
                stockService.getOccupiedCountMap(productIds, warehouseIds));
        items.forEach(item -> {
            String key = buildStockMapKey(item.getProductId(), item.getWarehouseId());
            ErpStockDO stock = stockMap.get(key);
            BigDecimal stockCount = stock != null && stock.getCount() != null
                    ? stock.getCount() : BigDecimal.ZERO;
            BigDecimal occupiedCount = occupiedCountMap.getOrDefault(key, BigDecimal.ZERO);
            item.setStockCount(stockCount);
            item.setAvailableCount(stockCount.subtract(occupiedCount));
        });
    }

    private static String buildStockMapKey(Long productId, Long warehouseId) {
        return productId + "_" + warehouseId;
    }

    private Map<Long, ErpWarehouseDO> getWarehouseMapIgnoreDataPermission(Collection<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds));
    }

    private void fillRelation(ErpSaleQuoteRespVO vo, List<ErpSaleQuoteItemDO> items,
                              Map<Long, ErpProductRespVO> productMap,
                              Map<Long, ErpWarehouseDO> warehouseMap,
                              Map<Long, ErpCustomerDO> customerMap,
                              Map<Long, AdminUserRespDTO> userMap,
                              Map<Long, DeptRespDTO> deptMap,
                              Map<Long, ErpSaleOutDO> saleOutMap) {
        List<ErpSaleQuoteItemDO> safeItems = CollUtil.isEmpty(items) ? Collections.emptyList() : items;
        Set<Long> productIds = convertSet(safeItems, ErpSaleQuoteItemDO::getProductId);
        Map<Long, BigDecimal> lastSalePriceMap = saleOutItemMapper == null || CollUtil.isEmpty(productIds)
                ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> saleOutItemMapper.selectLatestSalePriceMap(productIds));
        List<ErpSaleQuoteRespVO.Item> respItems = BeanUtils.toBean(safeItems, ErpSaleQuoteRespVO.Item.class,
                item -> {
                    item.setLastSalePrice(lastSalePriceMap.get(item.getProductId()));
                    MapUtils.findAndThen(productMap, item.getProductId(), product -> {
                        item.setProductName(product.getName())
                                .setProductCode(product.getCode()).setProductBarCode(product.getBarCode())
                                .setProductUnitName(product.getUnitName()).setBatchNoEnabled(product.getBatchNoEnabled());
                    });
                    MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> {
                        item.setWarehouseName(warehouse.getName());
                        item.setWarehouseDeptId(warehouse.getDeptId());
                        MapUtils.findAndThen(deptMap, warehouse.getDeptId(), dept -> item.setWarehouseDeptName(dept.getName()));
                    });
                    MapUtils.findAndThen(deptMap, item.getDeptId(), dept -> item.setDeptName(dept.getName()));
                    item.setRemainingCount((item.getCount() != null ? item.getCount() : BigDecimal.ZERO)
                            .subtract(item.getConvertedCount() != null ? item.getConvertedCount() : BigDecimal.ZERO)
                            .max(BigDecimal.ZERO));
                });
        vo.setItems(respItems == null ? Collections.emptyList() : respItems);
        itemPriceReferenceFiller.fill(vo.getItems());
        vo.setProductNames(CollUtil.join(vo.getItems(), "，", ErpSaleQuoteRespVO.Item::getProductName));
        fillMainRelation(vo, customerMap, userMap, deptMap, saleOutMap);
    }

    private void fillMainRelation(ErpSaleQuoteRespVO vo,
                                  Map<Long, ErpCustomerDO> customerMap,
                                  Map<Long, AdminUserRespDTO> userMap,
                                  Map<Long, DeptRespDTO> deptMap,
                                  Map<Long, ErpSaleOutDO> saleOutMap) {
        if (vo.getCustomerId() != null) {
            MapUtils.findAndThen(customerMap, vo.getCustomerId(), customer -> {
                vo.setCustomerName(customer.getName());
                vo.setCustomerCode(customer.getCode());
            });
        }
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
            MapUtils.findAndThen(userMap, vo.getSaleUserId(), user -> vo.setSaleUserName(user.getNickname()));
        }
        // 填充部门名称
        if (vo.getDeptId() != null) {
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
        }
        // 反查生成的销售单号
        if (ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(vo.getStatus())) {
            ErpSaleOutDO saleOut = saleOutMap.get(vo.getId());
            if (saleOut != null) {
                vo.setGeneratedSaleOutNo(saleOut.getNo());
            }
        }
    }

    private void applyProgress(List<ErpSaleQuoteRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Map<Long, ErpSaleQuoteProgressService.Progress> progressMap = saleQuoteProgressService == null
                ? Collections.emptyMap()
                : saleQuoteProgressService.getProgressMap(convertSet(list, ErpSaleQuoteRespVO::getId));
        list.forEach(vo -> {
            ErpSaleQuoteProgressService.Progress progress = progressMap.get(vo.getId());
            if (progress != null) {
                vo.setStatus(progress.getRawStatus());
                vo.setOrderStatus(progress.getOrderStatus());
                vo.setOutStatus(progress.getOutStatus());
                vo.setConvertedCount(progress.getConvertedCount());
                vo.setActualOutCount(progress.getActualOutCount());
                vo.setRemainingCount(progress.getRemainingCount());
                return;
            }
            int status = vo.getStatus() != null ? vo.getStatus() : ErpSaleQuoteStatusEnum.DRAFT.getStatus();
            vo.setOrderStatus(status == ErpSaleQuoteStatusEnum.CANCEL.getStatus()
                    ? ErpSaleQuoteStatusEnum.CANCEL.getStatus()
                    : status >= ErpSaleQuoteStatusEnum.APPROVE.getStatus()
                    ? ErpSaleQuoteStatusEnum.APPROVE.getStatus() : ErpSaleQuoteStatusEnum.DRAFT.getStatus());
            vo.setOutStatus(status == ErpSaleQuoteStatusEnum.CANCEL.getStatus()
                    ? ErpSaleQuoteStatusEnum.DRAFT.getStatus()
                    : status >= ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus()
                    ? ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus()
                    : status >= ErpSaleQuoteStatusEnum.CONVERTED_CART.getStatus()
                    ? ErpSaleQuoteStatusEnum.CONVERTED_CART.getStatus()
                    : status >= ErpSaleQuoteStatusEnum.PART_CONVERTED_CART.getStatus()
                    ? ErpSaleQuoteStatusEnum.PART_CONVERTED_CART.getStatus()
                    : ErpSaleQuoteStatusEnum.DRAFT.getStatus());
            BigDecimal converted = vo.getItems() == null ? BigDecimal.ZERO : vo.getItems().stream()
                    .map(ErpSaleQuoteRespVO.Item::getConvertedCount).filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setConvertedCount(converted);
            vo.setActualOutCount(status >= ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus()
                    ? (vo.getTotalCount() != null ? vo.getTotalCount() : BigDecimal.ZERO) : BigDecimal.ZERO);
            vo.setRemainingCount((vo.getTotalCount() != null ? vo.getTotalCount() : BigDecimal.ZERO)
                    .subtract(converted).max(BigDecimal.ZERO));
        });
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
        row.setCustomerId(quote.getCustomerId());
        if (item == null) {
            return row;
        }
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setProductUnitName(item.getProductUnitName());
        row.setWeight(item.getWeight());
        row.setPackageQty(item.getPackageQty());
        if (item.getCount() != null && item.getPackageQty() != null && item.getPackageQty() > 0) {
            row.setPieceCount(item.getCount().divide(BigDecimal.valueOf(item.getPackageQty()),
                    3, RoundingMode.HALF_UP).stripTrailingZeros());
        }
        row.setItemCount(item.getCount());
        row.setConvertedCount(item.getConvertedCount());
        row.setProductPrice(item.getProductPrice());
        row.setLastSalePrice(item.getLastSalePrice());
        row.setItemTotalPrice(item.getTotalPrice());
        row.setItemTaxPercent(item.getTaxPercent());
        row.setItemTaxPrice(item.getTaxPrice());
        row.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()) ? "是" : "否");
        row.setWarehouseName(item.getWarehouseName());
        row.setItemDeptName(item.getDeptName());
        row.setStockCount(item.getStockCount());
        row.setAvailableCount(item.getAvailableCount());
        row.setBatchNo(item.getBatchNo());
        row.setProductBarCode(item.getProductBarCode());
        row.setDrawingNo(item.getDrawingNo());
        row.setBrand(item.getBrand());
        row.setVehicleModel(item.getVehicleModel());
        row.setStandard(item.getStandard());
        row.setOriginPlace(item.getOriginPlace());
        row.setWarehousePosition(item.getWarehousePosition());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("no", "main");
        map.put("customerName", "main");
        map.put("status", "main");
        map.put("orderStatus", "main");
        map.put("outStatus", "main");
        map.put("saleUserName", "main");
        map.put("deptName", "main");
        map.put("customerCode", "main");
        map.put("quoteTime", "main");
        map.put("totalCount", "main");
        map.put("totalPrice", "main");
        map.put("totalProductPrice", "main");
        map.put("discountPrice", "main");
        map.put("feeAmount", "main");
        map.put("otherPrice", "main");
        map.put("priority", "main");
        map.put("deliveryMethod", "main");
        map.put("deliveryAddress", "main");
        map.put("settleMethod", "main");
        map.put("expectedDeliveryTime", "main");
        map.put("internalRemark", "main");
        map.put("creatorName", "system");
        map.put("createTime", "system");
        map.put("updaterName", "system");
        map.put("updateTime", "system");
        map.put("remark", "main");
        map.put("productCode", "detail");
        map.put("productName", "detail");
        map.put("productUnitName", "detail");
        map.put("weight", "detail");
        map.put("packageQty", "detail");
        map.put("pieceCount", "detail");
        map.put("itemCount", "detail");
        map.put("convertedCount", "detail");
        map.put("productPrice", "detail");
        map.put("lastSalePrice", "detail");
        map.put("itemTotalPrice", "detail");
        map.put("itemTaxPercent", "detail");
        map.put("itemTaxPrice", "detail");
        map.put("giftFlag", "detail");
        map.put("warehouseName", "detail");
        map.put("itemDeptName", "detail");
        map.put("stockCount", "detail");
        map.put("availableCount", "detail");
        map.put("batchNo", "detail");
        map.put("productBarCode", "detail");
        map.put("drawingNo", "detail");
        map.put("brand", "detail");
        map.put("vehicleModel", "detail");
        map.put("standard", "detail");
        map.put("originPlace", "detail");
        map.put("warehousePosition", "detail");
        map.put("itemRemark", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("customerName", "customerId");
        map.put("customerCode", "customerId");
        map.put("saleUserName", "saleUserId");
        map.put("deptName", "deptId");
        map.put("productCode", "item_productCode");
        map.put("productName", "item_productId");
        map.put("productUnitName", "item_productUnitName");
        map.put("weight", "item_weight");
        map.put("packageQty", "item_packageQty");
        map.put("pieceCount", "item_count");
        map.put("itemCount", "item_count");
        map.put("convertedCount", "item_convertedCount");
        map.put("productPrice", "item_productPrice");
        map.put("lastSalePrice", "item_lastSalePrice");
        map.put("itemTotalPrice", "item_totalPrice");
        map.put("itemTaxPercent", "item_taxPercent");
        map.put("itemTaxPrice", "item_taxPrice");
        map.put("giftFlag", "item_giftFlag");
        map.put("warehouseName", "item_warehouseId");
        map.put("itemDeptName", "item_deptId");
        map.put("stockCount", "item_stockCount");
        map.put("availableCount", "item_stockCount");
        map.put("batchNo", "item_batchNo");
        map.put("productBarCode", "item_productBarCode");
        map.put("drawingNo", "item_drawingNo");
        map.put("brand", "item_brand");
        map.put("vehicleModel", "item_vehicleModel");
        map.put("standard", "item_standard");
        map.put("originPlace", "item_originPlace");
        map.put("warehousePosition", "item_warehousePosition");
        map.put("itemRemark", "item_remark");
        return map;
    }

}
