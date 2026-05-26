package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivableRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableItemDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpOtherReceivableService;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 其他应收单")
@RestController
@RequestMapping("/erp/other-receivable")
@Validated
public class ErpOtherReceivableController {

    @Resource
    private ErpOtherReceivableService otherReceivableService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建其他应收单")
    @PreAuthorize("@ss.hasPermission('erp:other-receivable:create')")
    public CommonResult<Long> createOtherReceivable(@Valid @RequestBody ErpOtherReceivableSaveReqVO createReqVO) {
        return success(otherReceivableService.createOtherReceivable(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新其他应收单")
    @PreAuthorize("@ss.hasPermission('erp:other-receivable:update')")
    public CommonResult<Boolean> updateOtherReceivable(@Valid @RequestBody ErpOtherReceivableSaveReqVO updateReqVO) {
        otherReceivableService.updateOtherReceivable(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新其他应收单状态")
    @PreAuthorize("@ss.hasPermission('erp:other-receivable:update-status')")
    public CommonResult<Boolean> updateOtherReceivableStatus(@RequestParam("id") Long id,
                                                             @RequestParam("status") Integer status) {
        otherReceivableService.updateOtherReceivableStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除其他应收单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:other-receivable:delete')")
    public CommonResult<Boolean> deleteOtherReceivable(@RequestParam("ids") List<Long> ids) {
        otherReceivableService.deleteOtherReceivable(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得其他应收单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:other-receivable:query')")
    public CommonResult<ErpOtherReceivableRespVO> getOtherReceivable(@RequestParam("id") Long id) {
        ErpOtherReceivableDO receivable = otherReceivableService.getOtherReceivable(id);
        if (receivable == null) {
            return success(null);
        }
        List<ErpOtherReceivableItemDO> items = otherReceivableService.getOtherReceivableItemListByReceivableId(id);
        return success(BeanUtils.toBean(receivable, ErpOtherReceivableRespVO.class, vo ->
                vo.setItems(BeanUtils.toBean(items, ErpOtherReceivableRespVO.Item.class))));
    }

    @GetMapping("/page")
    @Operation(summary = "获得其他应收单分页")
    @PreAuthorize("@ss.hasPermission('erp:other-receivable:query')")
    public CommonResult<PageResult<ErpOtherReceivableRespVO>> getOtherReceivablePage(@Valid ErpOtherReceivablePageReqVO pageReqVO) {
        PageResult<ErpOtherReceivableDO> pageResult = otherReceivableService.getOtherReceivablePage(pageReqVO);
        return success(buildOtherReceivableVOPageResult(pageResult));
    }

    private PageResult<ErpOtherReceivableRespVO> buildOtherReceivableVOPageResult(PageResult<ErpOtherReceivableDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        // 1.1 明细
        List<ErpOtherReceivableItemDO> itemList = otherReceivableService.getOtherReceivableItemListByReceivableIds(
                convertSet(pageResult.getList(), ErpOtherReceivableDO::getId));
        Map<Long, List<ErpOtherReceivableItemDO>> itemMap = convertMultiMap(itemList, ErpOtherReceivableItemDO::getReceivableId);
        // 1.2 结算账户
        Set<Long> accountIds = convertSet(pageResult.getList(), ErpOtherReceivableDO::getAccountId);
        accountIds.remove(null);
        Map<Long, ErpAccountDO> accountMap = accountIds.isEmpty() ? Collections.emptyMap()
                : accountService.getAccountMap(accountIds);
        // 1.3 管理员
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(pageResult.getList(), r -> NumberUtils.parseLong(r.getCreator())));
        // 2. 拼接
        return BeanUtils.toBean(pageResult, ErpOtherReceivableRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemMap.get(vo.getId()), ErpOtherReceivableRespVO.Item.class));
            MapUtils.findAndThen(accountMap, vo.getAccountId(), account -> vo.setAccountName(account.getName()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
        });
    }

}
