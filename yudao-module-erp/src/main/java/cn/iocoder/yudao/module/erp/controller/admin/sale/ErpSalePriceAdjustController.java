package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSaleOutItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePriceAdjustService;
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
import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 销售调价单")
@RestController
@RequestMapping("/erp/sale-price-adjust")
@Validated
public class ErpSalePriceAdjustController {

    @Resource
    private ErpSalePriceAdjustService salePriceAdjustService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建销售调价单")
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:create')")
    public CommonResult<Long> createSalePriceAdjust(@Valid @RequestBody ErpSalePriceAdjustSaveReqVO createReqVO) {
        return success(salePriceAdjustService.createSalePriceAdjust(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新销售调价单")
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:update')")
    public CommonResult<Boolean> updateSalePriceAdjust(@Valid @RequestBody ErpSalePriceAdjustSaveReqVO updateReqVO) {
        salePriceAdjustService.updateSalePriceAdjust(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新销售调价单状态")
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:update-status')")
    public CommonResult<Boolean> updateSalePriceAdjustStatus(@RequestParam("id") Long id,
                                                              @RequestParam("status") Integer status) {
        salePriceAdjustService.updateSalePriceAdjustStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除销售调价单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:delete')")
    public CommonResult<Boolean> deleteSalePriceAdjust(@RequestParam("ids") List<Long> ids) {
        salePriceAdjustService.deleteSalePriceAdjust(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售调价单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:query')")
    public CommonResult<ErpSalePriceAdjustRespVO> getSalePriceAdjust(@RequestParam("id") Long id) {
        ErpSalePriceAdjustDO adjust = salePriceAdjustService.getSalePriceAdjust(id);
        if (adjust == null) {
            return success(null);
        }
        List<ErpSalePriceAdjustItemDO> items = salePriceAdjustService.getSalePriceAdjustItemListByAdjustId(id);
        ErpSalePriceAdjustRespVO respVO = BeanUtils.toBean(adjust, ErpSalePriceAdjustRespVO.class);
        respVO.setItems(BeanUtils.toBean(items, ErpSalePriceAdjustRespVO.Item.class));

        // 客户名称
        if (adjust.getCustomerId() != null) {
            ErpCustomerDO customer = customerService.getCustomer(adjust.getCustomerId());
            if (customer != null) {
                respVO.setCustomerName(customer.getName());
            }
        }
        // 部门名称
        if (adjust.getDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(adjust.getDeptId());
            if (dept != null) {
                respVO.setDeptName(dept.getName());
            }
        }
        // 用户名（调价人 + 创建人）
        Set<Long> userIds = new HashSet<>();
        if (adjust.getAdjustUserId() != null) {
            userIds.add(adjust.getAdjustUserId());
        }
        if (adjust.getCreator() != null) {
            try {
                userIds.add(Long.parseLong(adjust.getCreator()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (!userIds.isEmpty()) {
            Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
            if (adjust.getAdjustUserId() != null) {
                MapUtils.findAndThen(userMap, adjust.getAdjustUserId(), u -> respVO.setAdjustUserName(u.getNickname()));
            }
            if (adjust.getCreator() != null) {
                try {
                    long creatorId = Long.parseLong(adjust.getCreator());
                    MapUtils.findAndThen(userMap, creatorId, u -> respVO.setCreatorName(u.getNickname()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售调价单分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:query')")
    public CommonResult<PageResult<ErpSalePriceAdjustRespVO>> getSalePriceAdjustPage(@Valid ErpSalePriceAdjustPageReqVO pageReqVO) {
        PageResult<ErpSalePriceAdjustDO> pageResult = salePriceAdjustService.getSalePriceAdjustPage(pageReqVO);
        return success(buildSalePriceAdjustVOPageResult(pageResult));
    }

    @GetMapping("/adjustable-items")
    @Operation(summary = "获取客户可调价明细")
    @PreAuthorize("@ss.hasPermission('erp:sale-price-adjust:query')")
    public CommonResult<List<ErpSaleOutItemForAdjustRespVO>> getAdjustableItemsByCustomerId(
            @RequestParam("customerId") Long customerId,
            @RequestParam(value = "saleOutId", required = false) Long saleOutId) {
        return success(salePriceAdjustService.getAdjustableItemsByCustomerId(customerId, saleOutId));
    }

    private PageResult<ErpSalePriceAdjustRespVO> buildSalePriceAdjustVOPageResult(PageResult<ErpSalePriceAdjustDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        // 1.1 子项
        List<ErpSalePriceAdjustItemDO> itemList = salePriceAdjustService.getSalePriceAdjustItemListByAdjustIds(
                convertSet(pageResult.getList(), ErpSalePriceAdjustDO::getId));
        Map<Long, List<ErpSalePriceAdjustItemDO>> itemMap = convertMultiMap(itemList, ErpSalePriceAdjustItemDO::getAdjustId);
        // 1.2 客户
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpSalePriceAdjustDO::getCustomerId));
        // 1.3 用户（调价人 + 创建人）
        Set<Long> userIds = new HashSet<>();
        for (ErpSalePriceAdjustDO adjust : pageResult.getList()) {
            if (adjust.getAdjustUserId() != null) {
                userIds.add(adjust.getAdjustUserId());
            }
            if (adjust.getCreator() != null) {
                try {
                    userIds.add(Long.parseLong(adjust.getCreator()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty() ? new java.util.HashMap<>() : adminUserApi.getUserMap(userIds);
        // 1.4 部门
        Set<Long> deptIds = convertSet(pageResult.getList(), ErpSalePriceAdjustDO::getDeptId);
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? new java.util.HashMap<>() : deptApi.getDeptMap(deptIds);

        // 2. 拼装
        return BeanUtils.toBean(pageResult, ErpSalePriceAdjustRespVO.class, respVO -> {
            respVO.setItems(BeanUtils.toBean(itemMap.get(respVO.getId()), ErpSalePriceAdjustRespVO.Item.class));
            MapUtils.findAndThen(customerMap, respVO.getCustomerId(), customer -> respVO.setCustomerName(customer.getName()));
            MapUtils.findAndThen(deptMap, respVO.getDeptId(), d -> respVO.setDeptName(d.getName()));
            if (respVO.getAdjustUserId() != null) {
                MapUtils.findAndThen(userMap, respVO.getAdjustUserId(), u -> respVO.setAdjustUserName(u.getNickname()));
            }
            if (respVO.getCreator() != null) {
                try {
                    long creatorId = Long.parseLong(respVO.getCreator());
                    MapUtils.findAndThen(userMap, creatorId, u -> respVO.setCreatorName(u.getNickname()));
                } catch (NumberFormatException ignored) {
                }
            }
        });
    }

}
