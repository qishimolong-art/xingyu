package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptItemDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpPreReceiptService;
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
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;

@Tag(name = "管理后台 - ERP 预收款单")
@RestController
@RequestMapping("/erp/pre-receipt")
@Validated
public class ErpPreReceiptController {

    @Resource
    private ErpPreReceiptService preReceiptService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建预收款单")
    @PreAuthorize("@ss.hasPermission('erp:pre-receipt:create')")
    public CommonResult<Long> createPreReceipt(@Valid @RequestBody ErpPreReceiptSaveReqVO createReqVO) {
        return success(preReceiptService.createPreReceipt(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新预收款单")
    @PreAuthorize("@ss.hasPermission('erp:pre-receipt:update')")
    public CommonResult<Boolean> updatePreReceipt(@Valid @RequestBody ErpPreReceiptSaveReqVO updateReqVO) {
        preReceiptService.updatePreReceipt(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新预收款单状态")
    @PreAuthorize("@ss.hasPermission('erp:pre-receipt:update-status')")
    public CommonResult<Boolean> updatePreReceiptStatus(@RequestParam("id") Long id,
                                                       @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        preReceiptService.updatePreReceiptStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除预收款单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:pre-receipt:delete')")
    public CommonResult<Boolean> deletePreReceipt(@RequestParam("ids") List<Long> ids) {
        preReceiptService.deletePreReceipt(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得预收款单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:pre-receipt:query')")
    public CommonResult<ErpPreReceiptRespVO> getPreReceipt(@RequestParam("id") Long id) {
        ErpPreReceiptDO preReceipt = preReceiptService.getPreReceipt(id);
        if (preReceipt == null) {
            return success(null);
        }
        List<ErpPreReceiptItemDO> items = preReceiptService.getPreReceiptItemListByPreReceiptId(id);
        return success(BeanUtils.toBean(preReceipt, ErpPreReceiptRespVO.class, vo ->
                vo.setItems(BeanUtils.toBean(items, ErpPreReceiptRespVO.Item.class))));
    }

    @GetMapping("/page")
    @Operation(summary = "获得预收款单分页")
    @PreAuthorize("@ss.hasPermission('erp:pre-receipt:query')")
    public CommonResult<PageResult<ErpPreReceiptRespVO>> getPreReceiptPage(@Valid ErpPreReceiptPageReqVO pageReqVO) {
        PageResult<ErpPreReceiptDO> pageResult = preReceiptService.getPreReceiptPage(pageReqVO);
        return success(buildPreReceiptVOPageResult(pageResult));
    }

    private PageResult<ErpPreReceiptRespVO> buildPreReceiptVOPageResult(PageResult<ErpPreReceiptDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        // 1.1 明细
        List<ErpPreReceiptItemDO> itemList = preReceiptService.getPreReceiptItemListByPreReceiptIds(
                convertSet(pageResult.getList(), ErpPreReceiptDO::getId));
        Map<Long, List<ErpPreReceiptItemDO>> itemMap = convertMultiMap(itemList, ErpPreReceiptItemDO::getPreReceiptId);
        // 1.2 结算账户
        Set<Long> accountIds = convertSet(pageResult.getList(), ErpPreReceiptDO::getAccountId);
        accountIds.remove(null);
        Map<Long, ErpAccountDO> accountMap = accountIds.isEmpty() ? Collections.emptyMap()
                : accountService.getAccountMap(accountIds);
        // 1.3 管理员
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(pageResult.getList(), r -> NumberUtils.parseLong(r.getCreator())));
        // 2. 拼接
        return BeanUtils.toBean(pageResult, ErpPreReceiptRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemMap.get(vo.getId()), ErpPreReceiptRespVO.Item.class));
            MapUtils.findAndThen(accountMap, vo.getAccountId(), account -> vo.setAccountName(account.getName()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
        });
    }

}
