package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivableRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableItemDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpPreReceivableService;
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
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - ERP 预收账款单")
@RestController
@RequestMapping("/erp/pre-receivable")
@Validated
public class ErpPreReceivableController {

    @Resource
    private ErpPreReceivableService preReceivableService;
    @Resource
    private ErpAccountService accountService;

    @PostMapping("/create")
    @Operation(summary = "创建预收账款单")
    @PreAuthorize("@ss.hasPermission('erp:pre-receivable:create')")
    public CommonResult<Long> createPreReceivable(@Valid @RequestBody ErpPreReceivableSaveReqVO createReqVO) {
        return success(preReceivableService.createPreReceivable(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新预收账款单")
    @PreAuthorize("@ss.hasPermission('erp:pre-receivable:update')")
    public CommonResult<Boolean> updatePreReceivable(@Valid @RequestBody ErpPreReceivableSaveReqVO updateReqVO) {
        preReceivableService.updatePreReceivable(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新预收账款单状态")
    @Parameter(name = "id", description = "编号", required = true)
    @Parameter(name = "status", description = "状态", required = true)
    @PreAuthorize("@ss.hasPermission('erp:pre-receivable:update')")
    public CommonResult<Boolean> updatePreReceivableStatus(@RequestParam("id") Long id,
                                                           @RequestParam("status") Integer status) {
        preReceivableService.updatePreReceivableStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除预收账款单")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('erp:pre-receivable:delete')")
    public CommonResult<Boolean> deletePreReceivable(@RequestParam("ids") List<Long> ids) {
        preReceivableService.deletePreReceivable(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得预收账款单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:pre-receivable:query')")
    public CommonResult<ErpPreReceivableRespVO> getPreReceivable(@RequestParam("id") Long id) {
        ErpPreReceivableDO preReceivable = preReceivableService.getPreReceivable(id);
        if (preReceivable == null) {
            return success(null);
        }
        // 组装响应
        ErpPreReceivableRespVO respVO = BeanUtils.toBean(preReceivable, ErpPreReceivableRespVO.class);
        // 填充账户名称
        if (preReceivable.getAccountId() != null) {
            ErpAccountDO account = accountService.getAccount(preReceivable.getAccountId());
            if (account != null) {
                respVO.setAccountName(account.getName());
            }
        }
        // 填充明细
        List<ErpPreReceivableItemDO> items = preReceivableService.getPreReceivableItemListByPreReceivableId(id);
        respVO.setItems(BeanUtils.toBean(items, ErpPreReceivableRespVO.Item.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得预收账款单分页")
    @PreAuthorize("@ss.hasPermission('erp:pre-receivable:query')")
    public CommonResult<PageResult<ErpPreReceivableRespVO>> getPreReceivablePage(@Valid ErpPreReceivablePageReqVO pageReqVO) {
        PageResult<ErpPreReceivableDO> pageResult = preReceivableService.getPreReceivablePage(pageReqVO);
        // 组装响应
        PageResult<ErpPreReceivableRespVO> voPageResult = BeanUtils.toBean(pageResult, ErpPreReceivableRespVO.class);
        // 批量填充账户名称
        Set<Long> accountIds = convertSet(pageResult.getList(), ErpPreReceivableDO::getAccountId);
        accountIds.remove(null);
        Map<Long, ErpAccountDO> accountMap = accountIds.isEmpty() ? Collections.emptyMap()
                : accountService.getAccountMap(accountIds);
        voPageResult.getList().forEach(vo -> {
            if (vo.getAccountId() != null) {
                ErpAccountDO account = accountMap.get(vo.getAccountId());
                if (account != null) {
                    vo.setAccountName(account.getName());
                }
            }
        });
        return success(voPageResult);
    }

}
