package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceTransferService;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertListByFlatMap;

@Tag(name = "管理后台 - ERP 银行转账单")
@RestController
@RequestMapping("/erp/finance-transfer")
@Validated
public class ErpFinanceTransferController {

    @Resource
    private ErpFinanceTransferService financeTransferService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @PostMapping("/create")
    @Operation(summary = "创建银行转账单")
    @PreAuthorize("@ss.hasPermission('erp:finance-transfer:create')")
    public CommonResult<Long> createFinanceTransfer(@Valid @RequestBody ErpFinanceTransferSaveReqVO createReqVO) {
        return success(financeTransferService.createFinanceTransfer(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新银行转账单")
    @PreAuthorize("@ss.hasPermission('erp:finance-transfer:update')")
    public CommonResult<Boolean> updateFinanceTransfer(@Valid @RequestBody ErpFinanceTransferSaveReqVO updateReqVO) {
        financeTransferService.updateFinanceTransfer(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新银行转账单状态")
    @PreAuthorize("@ss.hasPermission('erp:finance-transfer:update-status')")
    public CommonResult<Boolean> updateFinanceTransferStatus(@RequestParam("id") Long id,
                                                             @RequestParam("status") Integer status) {
        financeTransferService.updateFinanceTransferStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除银行转账单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:finance-transfer:delete')")
    public CommonResult<Boolean> deleteFinanceTransfer(@RequestParam("ids") List<Long> ids) {
        financeTransferService.deleteFinanceTransfer(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得银行转账单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:finance-transfer:query')")
    public CommonResult<ErpFinanceTransferRespVO> getFinanceTransfer(@RequestParam("id") Long id) {
        ErpFinanceTransferDO transfer = financeTransferService.getFinanceTransfer(id);
        if (transfer == null) {
            return success(null);
        }
        ErpFinanceTransferRespVO respVO = buildFinanceTransferRespVO(transfer);
        fieldPermissionMasker.maskForm("erp_finance_transfer", respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得银行转账单分页")
    @PreAuthorize("@ss.hasPermission('erp:finance-transfer:query')")
    public CommonResult<PageResult<ErpFinanceTransferRespVO>> getFinanceTransferPage(@Valid ErpFinanceTransferPageReqVO pageReqVO) {
        PageResult<ErpFinanceTransferDO> pageResult = financeTransferService.getFinanceTransferPage(pageReqVO);
        return success(buildFinanceTransferVOPageResult(pageResult));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出银行转账单 Excel")
    @PreAuthorize("@ss.hasPermission('erp:finance-transfer:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFinanceTransferExcel(@Valid ErpFinanceTransferPageReqVO pageReqVO,
                                           HttpServletResponse response) throws IOException {
        List<ErpFinanceTransferRespVO> list;
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            list = buildFinanceTransferVOPageResult(
                    new PageResult<>(financeTransferService.getFinanceTransferList(pageReqVO.getIds()),
                            (long) pageReqVO.getIds().size())).getList();
        } else {
            pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
            list = buildFinanceTransferVOPageResult(
                    financeTransferService.getFinanceTransferPage(pageReqVO)).getList();
        }
        ExcelUtils.write(response, "银行转账.xls", "数据", ErpFinanceTransferRespVO.class, list);
    }

    private PageResult<ErpFinanceTransferRespVO> buildFinanceTransferVOPageResult(PageResult<ErpFinanceTransferDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(convertListByFlatMap(pageResult.getList(),
                transfer -> Stream.of(transfer.getOutAccountId(), transfer.getInAccountId())));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(pageResult.getList(),
                transfer -> Stream.of(NumberUtils.parseLong(transfer.getCreator()), transfer.getFinanceUserId())));
        return BeanUtils.toBean(pageResult, ErpFinanceTransferRespVO.class, transfer -> {
            MapUtils.findAndThen(accountMap, transfer.getOutAccountId(), account -> transfer.setOutAccountName(account.getName()));
            MapUtils.findAndThen(accountMap, transfer.getInAccountId(), account -> transfer.setInAccountName(account.getName()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(transfer.getCreator()), user -> transfer.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, transfer.getFinanceUserId(), user -> transfer.setFinanceUserName(user.getNickname()));
        });
    }

    private ErpFinanceTransferRespVO buildFinanceTransferRespVO(ErpFinanceTransferDO transfer) {
        PageResult<ErpFinanceTransferDO> pageResult = new PageResult<>(Collections.singletonList(transfer), 1L);
        List<ErpFinanceTransferRespVO> list = buildFinanceTransferVOPageResult(pageResult).getList();
        return CollUtil.getFirst(list);
    }

}
