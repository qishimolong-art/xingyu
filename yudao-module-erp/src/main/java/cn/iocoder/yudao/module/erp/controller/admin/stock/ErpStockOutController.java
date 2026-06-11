package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutService;
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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "Admin - ERP stock out")
@RestController
@RequestMapping("/erp/stock-out")
@Validated
public class ErpStockOutController {

    private static final String FIELD_PERMISSION_MODULE = "erp_stock_out";

    @Resource
    private ErpStockOutService stockOutService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "Create stock out")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:create')")
    public CommonResult<Long> createStockOut(@Valid @RequestBody ErpStockOutSaveReqVO createReqVO) {
        return success(stockOutService.createStockOut(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "Update stock out")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:update')")
    public CommonResult<Boolean> updateStockOut(@Valid @RequestBody ErpStockOutSaveReqVO updateReqVO) {
        stockOutService.updateStockOut(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "Update stock out status")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:update-status')")
    public CommonResult<Boolean> updateStockOutStatus(@RequestParam("id") Long id,
                                                      @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        stockOutService.updateStockOutStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete stock out")
    @Parameter(name = "ids", description = "ids", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock-out:delete')")
    public CommonResult<Boolean> deleteStockOut(@RequestParam("ids") List<Long> ids) {
        stockOutService.deleteStockOut(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Get stock out")
    @Parameter(name = "id", description = "id", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<ErpStockOutRespVO> getStockOut(@RequestParam("id") Long id) {
        ErpStockOutDO stockOut = stockOutService.getStockOut(id);
        if (stockOut == null) {
            return success(null);
        }
        List<ErpStockOutItemDO> itemList = stockOutService.getStockOutItemListByOutId(id);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(itemList, ErpStockOutItemDO::getProductId));
        Set<Long> userIds = new HashSet<>();
        addUserId(userIds, stockOut.getCreator());
        addUserId(userIds, stockOut.getUpdater());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        DeptRespDTO dept = stockOut.getDeptId() == null ? null : deptApi.getDept(stockOut.getDeptId());

        ErpStockOutRespVO respVO = BeanUtils.toBean(stockOut, ErpStockOutRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemList, ErpStockOutRespVO.Item.class, item -> {
                ErpStockDO stock = stockService.getStock(item.getProductId(), item.getWarehouseId());
                item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                fillProduct(item, productMap.get(item.getProductId()));
            }));
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockOutRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockOutRespVO.Item::getProductCode));
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            fillUserNames(vo, userMap);
        });
        fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "Get stock out page")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<PageResult<ErpStockOutRespVO>> getStockOutPage(@Valid ErpStockOutPageReqVO pageReqVO) {
        return success(buildStockOutVOPageResult(stockOutService.getStockOutPage(pageReqVO)));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export stock out")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockOutExcel(@Valid ErpStockOutPageReqVO pageReqVO,
                                    HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpStockOutRespVO> list = buildStockOutVOPageResult(stockOutService.getStockOutPage(pageReqVO)).getList();
        ExcelUtils.write(response, "stock-out.xls", "data", ErpStockOutRespVO.class, list);
    }

    private PageResult<ErpStockOutRespVO> buildStockOutVOPageResult(PageResult<ErpStockOutDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpStockOutItemDO> itemList = stockOutService.getStockOutItemListByOutIds(
                convertSet(pageResult.getList(), ErpStockOutDO::getId));
        Map<Long, List<ErpStockOutItemDO>> itemMap = convertMultiMap(itemList, ErpStockOutItemDO::getOutId);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(itemList, ErpStockOutItemDO::getProductId));
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpStockOutDO::getCustomerId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpStockOutDO::getDeptId));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(stockOut -> {
            addUserId(userIds, stockOut.getCreator());
            addUserId(userIds, stockOut.getUpdater());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        return BeanUtils.toBean(pageResult, ErpStockOutRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemMap.get(vo.getId()), ErpStockOutRespVO.Item.class,
                    item -> fillProduct(item, productMap.get(item.getProductId()))));
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockOutRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockOutRespVO.Item::getProductCode));
            MapUtils.findAndThen(customerMap, vo.getCustomerId(), customer -> vo.setCustomerName(customer.getName()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillUserNames(vo, userMap);
        });
    }

    private void fillProduct(ErpStockOutRespVO.Item item, ErpProductRespVO product) {
        if (product == null) {
            return;
        }
        item.setProductName(product.getName())
                .setProductCode(product.getCode())
                .setProductBarCode(product.getBarCode())
                .setProductUnitName(product.getUnitName());
    }

    private void fillUserNames(ErpStockOutRespVO stockOut, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(stockOut.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> stockOut.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(stockOut.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> stockOut.setUpdaterName(user.getNickname()));
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
