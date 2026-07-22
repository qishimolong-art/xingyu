package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockImportService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "Admin - ERP stock in")
@RestController
@RequestMapping("/erp/stock-in")
@Validated
public class ErpStockInController {

    private static final int EXPORT_MAX_COUNT = 5000;
    private static final ErrorCode EXPORT_COUNT_EXCEEDED = new ErrorCode(1_030_590_001,
            "单次最多导出 5000 条入库单，请缩小筛选范围后重试");
    private static final String FIELD_PERMISSION_MODULE = "erp_stock_in";
    private static final Set<String> IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "orderNo", "supplierName", "bizTime", "warehouseName", "productCode", "count", "productPrice",
            "remark", "itemRemark"));

    @Resource
    private ErpStockInService stockInService;
    @Resource
    private ErpStockImportService stockImportService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "Create stock in")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:create')")
    public CommonResult<Long> createStockIn(@Valid @RequestBody ErpStockInSaveReqVO createReqVO) {
        return success(stockInService.createStockIn(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "Update stock in")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:update')")
    public CommonResult<Boolean> updateStockIn(@Valid @RequestBody ErpStockInSaveReqVO updateReqVO) {
        stockInService.updateStockIn(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "Update stock in status")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:update-status')")
    public CommonResult<Boolean> updateStockInStatus(@RequestParam("id") Long id,
                                                     @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        stockInService.updateStockInStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete stock in")
    @Parameter(name = "ids", description = "ids", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock-in:delete')")
    public CommonResult<Boolean> deleteStockIn(@RequestParam("ids") List<Long> ids) {
        stockInService.deleteStockIn(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Get stock in")
    @Parameter(name = "id", description = "id", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<ErpStockInRespVO> getStockIn(@RequestParam("id") Long id) {
        ErpStockInDO stockIn = stockInService.getStockIn(id);
        if (stockIn == null) {
            return success(null);
        }
        List<ErpStockInItemDO> itemList = stockInService.getStockInItemListByInId(id);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(itemList, ErpStockInItemDO::getProductId)));
        Set<Long> userIds = new HashSet<>();
        addUserId(userIds, stockIn.getCreator());
        addUserId(userIds, stockIn.getUpdater());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        DeptRespDTO dept = stockIn.getDeptId() == null ? null : deptApi.getDept(stockIn.getDeptId());

        ErpStockInRespVO respVO = BeanUtils.toBean(stockIn, ErpStockInRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemList, ErpStockInRespVO.Item.class, item -> {
                ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                        stockService.getStock(item.getProductId(), item.getWarehouseId()));
                item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                fillProduct(item, productMap.get(item.getProductId()));
            }));
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockInRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockInRespVO.Item::getProductCode));
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            fillUserNames(vo, userMap);
        });
        fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "Get stock in page")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<PageResult<ErpStockInRespVO>> getStockInPage(@Valid ErpStockInPageReqVO pageReqVO) {
        return success(buildStockInVOPageResult(stockInService.getStockInPage(pageReqVO)));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export stock in")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockInExcel(@Valid ErpStockInPageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(EXPORT_MAX_COUNT);
        PageResult<ErpStockInDO> pageResult = stockInService.getStockInPage(pageReqVO);
        if (pageResult.getTotal() > EXPORT_MAX_COUNT) {
            throw exception(EXPORT_COUNT_EXCEEDED);
        }
        List<ErpStockInRespVO> list = buildStockInVOPageResult(pageResult).getList();
        ExcelUtils.write(response, "stock-in.xls", "data", ErpStockInRespVO.class, list);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "Get stock in import template")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpStockImportExcelVO first = new ErpStockImportExcelVO();
        first.setOrderNo("IN-001");
        first.setSupplierName("示例供应商");
        first.setBizTime("2026-07-01 09:00:00");
        first.setWarehouseName("示例仓库");
        first.setProductCode("P0001");
        first.setCount(BigDecimal.ONE);
        first.setProductPrice(new BigDecimal("100.00"));
        first.setRemark("单据备注");
        first.setItemRemark("明细备注");
        ErpStockImportExcelVO second = new ErpStockImportExcelVO();
        second.setOrderNo("IN-001");
        second.setWarehouseName("示例仓库");
        second.setProductCode("P0002");
        second.setCount(new BigDecimal("2"));
        second.setProductPrice(new BigDecimal("50.00"));
        ExcelUtils.writeImportTemplate(response, "其它入库导入模板.xls", "其它入库",
                ErpStockImportExcelVO.class, Arrays.asList(first, second), IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "Import stock in")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:import')")
    public CommonResult<ErpStockImportResultRespVO> importStockIn(@RequestParam("file") MultipartFile file)
            throws Exception {
        return success(stockImportService.importStockInList(ExcelUtils.read(file, ErpStockImportExcelVO.class)));
    }

    private PageResult<ErpStockInRespVO> buildStockInVOPageResult(PageResult<ErpStockInDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpStockInItemDO> itemList = stockInService.getStockInItemListByInIds(
                convertSet(pageResult.getList(), ErpStockInDO::getId));
        Map<Long, List<ErpStockInItemDO>> itemMap = convertMultiMap(itemList, ErpStockInItemDO::getInId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(itemList, ErpStockInItemDO::getProductId)));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpStockInDO::getSupplierId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpStockInDO::getDeptId));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(stockIn -> {
            addUserId(userIds, stockIn.getCreator());
            addUserId(userIds, stockIn.getUpdater());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        return BeanUtils.toBean(pageResult, ErpStockInRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemMap.get(vo.getId()), ErpStockInRespVO.Item.class,
                    item -> fillProduct(item, productMap.get(item.getProductId()))));
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockInRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockInRespVO.Item::getProductCode));
            MapUtils.findAndThen(supplierMap, vo.getSupplierId(), supplier -> vo.setSupplierName(supplier.getName()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillUserNames(vo, userMap);
        });
    }

    private void fillProduct(ErpStockInRespVO.Item item, ErpProductRespVO product) {
        if (product == null) {
            return;
        }
        item.setProductName(product.getName())
                .setProductCode(product.getCode())
                .setProductBarCode(product.getBarCode())
                .setProductUnitName(product.getUnitName());
    }

    private void fillUserNames(ErpStockInRespVO stockIn, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(stockIn.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> stockIn.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(stockIn.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> stockIn.setUpdaterName(user.getNickname()));
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
