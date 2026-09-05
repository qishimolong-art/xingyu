package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartConvertQuoteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartFirstApproveConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartFirstApproveConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSubmitRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartUpdateBasicReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.vin.ErpVinRecognizeRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpVinRecognizeService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;

@Tag(name = "管理后台 - ERP 销售手推车")
@RestController
@RequestMapping("/erp/sale-cart")
@Validated
public class ErpSaleCartController {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_cart";
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();
    private static final Map<String, String> DETAIL_IMPORT_FIELD_ALIAS_MAP = ErpImportTemplateRequiredFieldUtils.aliasMap(
            "productId", "productIdentity",
            "productCode", "productIdentity",
            "productName", "productIdentity",
            "factoryCode", "productIdentity",
            "warehouseId", "warehouseName",
            "warehouseName", "warehouseName",
            "count", "count",
            "itemCount", "count",
            "productPrice", "productPrice",
            "giftFlag", "giftFlag",
            "item_giftFlag", "giftFlag",
            "remark", "remark");

    @Resource
    private ErpSaleCartService saleCartService;
    @Resource
    private ErpVinRecognizeService vinRecognizeService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpFieldConfigService fieldConfigService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private ErpImportExportRecordService importExportRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:create')")
    public CommonResult<Long> createSaleCart(@RequestBody ErpSaleCartDraftCreateReqVO createReqVO) {
        return success(saleCartService.createSaleCart(
                BeanUtils.toBean(createReqVO, ErpSaleCartSaveReqVO.class)));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:create')")
    public CommonResult<ErpSaleCartSubmitRespVO> createAndSubmitSaleCart(@Valid @RequestBody ErpSaleCartSaveReqVO createReqVO) {
        return success(saleCartService.createAndSubmitSaleCart(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:update')")
    public CommonResult<Boolean> updateSaleCart(@Valid @RequestBody ErpSaleCartSaveReqVO updateReqVO) {
        saleCartService.updateSaleCart(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "修改销售手推车备注")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:update')")
    public CommonResult<Boolean> updateSaleCartRemark(
            @Valid @RequestBody ErpSaleUpdateRemarkReqVO updateReqVO) {
        saleCartService.updateSaleCartRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "更新销售手推车草稿")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:update')")
    public CommonResult<Boolean> updateSaleCartDraft(@RequestBody ErpSaleCartDraftUpdateReqVO updateReqVO) {
        saleCartService.updateSaleCartDraft(BeanUtils.toBean(updateReqVO, ErpSaleCartSaveReqVO.class));
        return success(true);
    }

    @PutMapping("/batch-update-items")
    @Operation(summary = "批量修改销售手推车明细仓库/部门")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:update')")
    public CommonResult<Boolean> batchUpdateSaleCartItems(
            @Valid @RequestBody ErpSaleCartItemBatchUpdateReqVO updateReqVO) {
        saleCartService.batchUpdateSaleCartItems(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-basic")
    @Operation(summary = "更新销售手推车基础信息")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:update')")
    public CommonResult<Boolean> updateSaleCartBasic(@Valid @RequestBody ErpSaleCartUpdateBasicReqVO updateReqVO) {
        saleCartService.updateSaleCartBasic(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:submit')")
    public CommonResult<ErpSaleCartSubmitRespVO> submitSaleCart(@RequestParam("id") Long id) {
        return success(saleCartService.submitSaleCart(id));
    }

    @PutMapping("/first-approve")
    @Operation(summary = "初审销售手推车")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:first-approve')")
    public CommonResult<Boolean> firstApproveSaleCart(@RequestParam("id") Long id) {
        saleCartService.firstApproveSaleCart(id);
        return success(true);
    }

    @PutMapping("/cancel-first-approve")
    @Operation(summary = "撤销销售手推车初审")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:cancel-first-approve')")
    public CommonResult<Boolean> cancelFirstApproveSaleCart(@RequestParam("id") Long id) {
        saleCartService.cancelFirstApproveSaleCart(id);
        return success(true);
    }

    @GetMapping("/first-approve-config")
    @Operation(summary = "获得销售手推车初审配置")
    @PreAuthorize("@ss.hasAnyPermissions('erp:sale-cart:query', 'erp:sale-cart:first-approve-config')")
    public CommonResult<ErpSaleCartFirstApproveConfigRespVO> getFirstApproveConfig() {
        return success(saleCartService.getFirstApproveConfig());
    }

    @PutMapping("/first-approve-config")
    @Operation(summary = "保存销售手推车初审配置")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:first-approve-config')")
    public CommonResult<Boolean> updateFirstApproveConfig(
            @Valid @RequestBody ErpSaleCartFirstApproveConfigSaveReqVO reqVO) {
        saleCartService.updateFirstApproveConfig(reqVO);
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
    public CommonResult<ErpSaleCartRespVO> getSaleCart(@RequestParam("id") Long id,
                                                       @RequestParam(value = "includeItems", required = false,
                                                               defaultValue = "true") Boolean includeItems) {
        ErpSaleCartDO cart = saleCartService.getSaleCart(id);
        if (cart == null) {
            return success(null);
        }
        List<ErpSaleCartItemDO> items = Boolean.TRUE.equals(includeItems)
                ? saleCartService.getSaleCartItemListByCartId(id) : Collections.emptyList();
        ErpSaleCartRespVO respVO = buildSaleCartRespVO(cart, items);
        fieldPermissionMasker.maskSaleDetailFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @PreAuthorize("@ss.hasPermission('erp:sale-cart:query')")
    public CommonResult<ErpSaleCartRespVO> getSaleCart(Long id) {
        return getSaleCart(id, true);
    }

    @GetMapping("/item-page")
    @Operation(summary = "获得销售手推车明细分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:query')")
    public CommonResult<PageResult<ErpSaleCartRespVO.Item>> getSaleCartItemPage(
            @Valid ErpSaleCartItemPageReqVO pageReqVO) {
        ErpSaleCartDO cart = saleCartService.getSaleCart(pageReqVO.getCartId());
        if (cart == null) {
            return success(PageResult.empty());
        }
        PageResult<ErpSaleCartItemDO> pageResult = saleCartService.getSaleCartItemPage(pageReqVO);
        ErpSaleCartRespVO respVO = buildSaleCartRespVO(cart, pageResult.getList());
        PageResult<ErpSaleCartRespVO.Item> respResult = new PageResult<>(respVO.getItems(), pageResult.getTotal());
        if (Boolean.TRUE.equals(pageReqVO.getMask())) {
            fieldPermissionMasker.clearSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, respVO, respResult.getList());
        }
        return success(respResult);
    }

    @GetMapping("/warehouse-dept-simple-list")
    @Operation(summary = "获取销售手推车批量修改仓库可用部门精简列表")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:update')")
    public CommonResult<List<DeptSimpleRespVO>> getWarehouseAvailableDeptSimpleList(
            @RequestParam("warehouseId") Long warehouseId) {
        return success(saleCartService.getWarehouseAvailableDeptSimpleList(warehouseId));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "获取销售手推车可见部门精简列表")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:query')")
    public CommonResult<List<DeptSimpleRespVO>> getVisibleDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList(FIELD_PERMISSION_MODULE));
    }

    @GetMapping("/recognize-vin")
    @Operation(summary = "VIN码识别")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:query')")
    public CommonResult<ErpVinRecognizeRespVO> recognizeVin(@RequestParam("vin") String vin) {
        return success(vinRecognizeService.recognize(vin));
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售手推车分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:query')")
    public CommonResult<PageResult<ErpSaleCartRespVO>> getSaleCartPage(@Valid ErpSaleCartPageReqVO pageReqVO) {
        PageResult<ErpSaleCartDO> pageResult = saleCartService.getSaleCartPage(pageReqVO);
        PageResult<ErpSaleCartRespVO> respResult = buildSaleCartVOPageResult(pageResult);
        fieldPermissionMasker.maskSaleDetailFormsWithItems(FIELD_PERMISSION_MODULE, respResult.getList());
        return success(respResult);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出销售手推车 Excel")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSaleCartExcel(@Valid ErpSaleCartPageReqVO pageReqVO,
                                    @RequestParam(value = "fields", required = false) String fields,
                                    HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpSaleCartRespVO> list = buildSaleCartVOPageResult(
                saleCartService.getSaleCartPage(pageReqVO)).getList();
        fieldPermissionMasker.maskSaleDetailFormsWithItems(FIELD_PERMISSION_MODULE, list);
        List<ErpSaleCartExportRespVO> rows = buildSaleCartExportList(list);
        fieldPermissionMasker.maskSaleDetailExportRows(FIELD_PERMISSION_MODULE, rows);
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpSaleCartExportRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE, false), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "销售手推车.xls", "数据", ErpSaleCartExportRespVO.class, rows, includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get sale cart export fields")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getSaleCartExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpSaleCartExportRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE, false), EXPORT_FIELD_PERMISSION_MAP));
    }

    @GetMapping("/export-import-template")
    @Operation(summary = "获得销售手推车导入模板")
    public void exportImportTemplate(HttpServletResponse response) throws IOException {
        ErpSaleCartImportExcelVO example = new ErpSaleCartImportExcelVO();
        example.setProductCode("P0001");
        example.setProductName("示例配件");
        example.setWarehouseName("默认仓");
        example.setCount(BigDecimal.ONE);
        example.setProductPrice(new BigDecimal("100.00"));
        example.setGiftFlag(false);
        example.setRemark("备注");
        ExcelUtils.writeImportTemplate(response, "销售手推车导入模板.xls", "销售手推车",
                ErpSaleCartImportExcelVO.class, Collections.singletonList(example), null,
                ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                        ErpFieldConfigModuleEnum.SALE_CART, ErpSaleCartImportExcelVO.class,
                        DETAIL_IMPORT_FIELD_ALIAS_MAP));
    }

    @PostMapping("/import")
    @Operation(summary = "导入销售手推车明细")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:create')")
    public CommonResult<ErpSaleCartImportRespVO> importSaleCart(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpSaleCartImportExcelVO> list = ExcelUtils.read(file, ErpSaleCartImportExcelVO.class);
        return success(saleCartService.parseImportData(list));
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "下载销售手推车导入错误数据")
    @PreAuthorize("@ss.hasPermission('erp:sale-cart:create')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, FIELD_PERMISSION_MODULE, response);
    }

    private PageResult<ErpSaleCartRespVO> buildSaleCartVOPageResult(PageResult<ErpSaleCartDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Set<Long> cartIds = convertSet(pageResult.getList(), ErpSaleCartDO::getId);
        List<ErpSaleCartItemDO> itemList = saleCartService.getSaleCartItemListByCartIds(
                cartIds);
        Map<Long, List<ErpSaleCartItemDO>> itemMap = convertMultiMap(itemList, ErpSaleCartItemDO::getCartId);
        Map<Long, ErpProductRespVO> productMap = CollUtil.isEmpty(itemList)
                ? Collections.emptyMap()
                : getProductVOMapIgnoreDataPermission(convertSet(itemList, ErpSaleCartItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(itemList)
                ? Collections.emptyMap()
                : getWarehouseMapIgnoreDataPermission(convertSet(itemList, ErpSaleCartItemDO::getWarehouseId));
        Set<Long> customerIds = convertSet(pageResult.getList(), ErpSaleCartDO::getCustomerId);
        customerIds.remove(null);
        Map<Long, ErpCustomerDO> customerMap = CollUtil.isEmpty(customerIds)
                ? Collections.emptyMap() : customerService.getCustomerMap(customerIds);
        Set<Long> quoteIds = convertSet(pageResult.getList(), cart ->
                ErpSaleBizSourceTypeEnum.QUOTE.getType().equals(cart.getSourceType()) ? cart.getSourceId() : null);
        quoteIds.remove(null);
        Map<Long, ErpSaleQuoteDO> quoteMap = CollUtil.isEmpty(quoteIds)
                ? Collections.emptyMap()
                : convertMap(saleQuoteMapper.selectByIds(quoteIds), ErpSaleQuoteDO::getId);
        Set<Long> userIds = convertSet(pageResult.getList(), cart -> parseLongSafely(cart.getCreator()));
        userIds.addAll(convertSet(pageResult.getList(), cart -> parseLongSafely(cart.getUpdater())));
        userIds.addAll(convertSet(pageResult.getList(), ErpSaleCartDO::getSaleUserId));
        userIds.remove(null);
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isNotEmpty(userIds)
                ? adminUserApi.getUserMap(userIds) : Collections.emptyMap();
        Set<Long> deptIds = convertSet(pageResult.getList(), ErpSaleCartDO::getDeptId);
        Map<Long, Boolean> firstApproveRequiredMap = saleCartService.getFirstApproveRequiredMap(deptIds);
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds)
                ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        return BeanUtils.toBean(pageResult, ErpSaleCartRespVO.class,
                cart -> fillRelation(cart, itemMap.get(cart.getId()), productMap, warehouseMap, customerMap,
                        quoteMap, userMap, deptMap, firstApproveRequiredMap.get(cart.getDeptId())));
    }

    private ErpSaleCartRespVO buildSaleCartRespVO(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        Map<Long, ErpProductRespVO> productMap = CollUtil.isEmpty(items)
                ? Collections.emptyMap()
                : getProductVOMapIgnoreDataPermission(convertSet(items, ErpSaleCartItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(items)
                ? Collections.emptyMap()
                : getWarehouseMapIgnoreDataPermission(convertSet(items, ErpSaleCartItemDO::getWarehouseId));
        Map<Long, ErpCustomerDO> customerMap = cart.getCustomerId() == null
                ? Collections.emptyMap()
                : customerService.getCustomerMap(Collections.singleton(cart.getCustomerId()));
        Map<Long, ErpSaleQuoteDO> quoteMap = ErpSaleBizSourceTypeEnum.QUOTE.getType().equals(cart.getSourceType())
                && cart.getSourceId() != null
                ? convertMap(saleQuoteMapper.selectByIds(Collections.singleton(cart.getSourceId())), ErpSaleQuoteDO::getId)
                : Collections.emptyMap();
        Set<Long> userIds = convertSet(Collections.singletonList(cart), item -> parseLongSafely(item.getCreator()));
        userIds.addAll(convertSet(Collections.singletonList(cart), item -> parseLongSafely(item.getUpdater())));
        userIds.add(cart.getSaleUserId());
        userIds.remove(null);
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isNotEmpty(userIds)
                ? adminUserApi.getUserMap(userIds) : Collections.emptyMap();
        Set<Long> deptIds = new java.util.HashSet<>();
        if (cart.getDeptId() != null) {
            deptIds.add(cart.getDeptId());
        }
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds)
                ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        return BeanUtils.toBean(cart, ErpSaleCartRespVO.class,
                vo -> fillRelation(vo, items, productMap, warehouseMap, customerMap, quoteMap, userMap, deptMap,
                        saleCartService.isFirstApproveRequiredForDept(cart.getDeptId())));
    }

    private Map<Long, ErpProductRespVO> getProductVOMapIgnoreDataPermission(Set<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(productIds));
    }

    private Map<Long, ErpWarehouseDO> getWarehouseMapIgnoreDataPermission(Set<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds));
    }

    private void fillRelation(ErpSaleCartRespVO vo, List<ErpSaleCartItemDO> items,
                              Map<Long, ErpProductRespVO> productMap,
                              Map<Long, ErpWarehouseDO> warehouseMap,
                              Map<Long, ErpCustomerDO> customerMap,
                              Map<Long, ErpSaleQuoteDO> quoteMap,
                              Map<Long, AdminUserRespDTO> userMap,
                              Map<Long, DeptRespDTO> deptMap,
                              Boolean firstApproveRequired) {
        List<ErpSaleCartItemDO> safeItems = CollUtil.isEmpty(items) ? Collections.emptyList() : items;
        List<ErpSaleCartRespVO.Item> respItems = BeanUtils.toBean(safeItems, ErpSaleCartRespVO.Item.class,
                item -> MapUtils.findAndThen(productMap, item.getProductId(), product -> item.setProductName(product.getName())
                        .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                        .setProductCode(product.getCode()).setLockCount(product.getLockCount())
                        .setBatchNoEnabled(product.getBatchNoEnabled())));
        vo.setItems(respItems == null ? Collections.emptyList() : respItems);
        vo.getItems().forEach(item ->
                MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> {
                    item.setWarehouseName(warehouse.getName());
                    item.setWarehouseDeptId(warehouse.getDeptId());
                    MapUtils.findAndThen(deptMap, warehouse.getDeptId(), dept -> item.setWarehouseDeptName(dept.getName()));
                }));
        vo.setProductNames(CollUtil.join(vo.getItems(), "，", ErpSaleCartRespVO.Item::getProductName));
        if (vo.getCustomerId() != null) {
            MapUtils.findAndThen(customerMap, vo.getCustomerId(), customer -> {
                vo.setCustomerName(customer.getName());
                vo.setCustomerCode(customer.getCode());
            });
        }
        if (ErpSaleBizSourceTypeEnum.QUOTE.getType().equals(vo.getSourceType())) {
            if (vo.getSourceNo() != null && !vo.getSourceNo().isEmpty()) {
                vo.setQuoteNo(vo.getSourceNo());
            }
            MapUtils.findAndThen(quoteMap, vo.getSourceId(), quote -> {
                if (vo.getQuoteNo() == null || vo.getQuoteNo().isEmpty()) {
                    vo.setQuoteNo(quote.getNo());
                }
                vo.setVehiclePlateNo(quote.getVehiclePlateNo());
            });
        }
        if (vo.getCreator() != null) {
            MapUtils.findAndThen(userMap, parseLongSafely(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
        }
        if (vo.getUpdater() != null) {
            MapUtils.findAndThen(userMap, parseLongSafely(vo.getUpdater()), user -> vo.setUpdaterName(user.getNickname()));
        }
        if (vo.getSaleUserId() != null) {
            MapUtils.findAndThen(userMap, vo.getSaleUserId(), user -> vo.setSaleUserName(user.getNickname()));
        }
        if (vo.getDeptId() != null) {
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
        }
        vo.setFirstApproveRequired(firstApproveRequired);
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
        row.setCustomerId(cart.getCustomerId());
        if (item == null) {
            return row;
        }
        row.setProductCode(item.getProductCode());
        row.setProductName(item.getProductName());
        row.setProductUnitName(item.getProductUnitName());
        row.setWeight(item.getWeight());
        row.setPackageQty(item.getPackageQty());
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

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("no", "main");
        map.put("customerName", "main");
        map.put("status", "main");
        map.put("cartTime", "main");
        map.put("totalCount", "main");
        map.put("totalPrice", "main");
        map.put("feeAmount", "main");
        map.put("remark", "main");
        map.put("fileUrl", "main");
        map.put("productCode", "detail");
        map.put("productName", "detail");
        map.put("productUnitName", "detail");
        map.put("weight", "detail");
        map.put("packageQty", "detail");
        map.put("warehouseName", "detail");
        map.put("lockCount", "detail");
        map.put("itemCount", "detail");
        map.put("productPrice", "detail");
        map.put("itemTotalPrice", "detail");
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
        map.put("productCode", "item_productCode");
        map.put("productName", "item_productId");
        map.put("productUnitName", "item_productUnitName");
        map.put("weight", "item_weight");
        map.put("packageQty", "item_packageQty");
        map.put("warehouseName", "item_warehouseId");
        map.put("lockCount", "item_lockCount");
        map.put("itemCount", "item_count");
        map.put("productPrice", "item_productPrice");
        map.put("itemTotalPrice", "item_totalPrice");
        map.put("brand", "item_brand");
        map.put("vehicleModel", "item_vehicleModel");
        map.put("standard", "item_standard");
        map.put("originPlace", "item_originPlace");
        map.put("warehousePosition", "item_warehousePosition");
        map.put("itemRemark", "item_remark");
        return map;
    }

}
