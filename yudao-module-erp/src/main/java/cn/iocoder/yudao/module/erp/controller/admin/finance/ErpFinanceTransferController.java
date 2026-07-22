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
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceTransferService;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertListByFlatMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.allBlank;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.failureReason;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.parseDateTime;

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
    private DeptApi deptApi;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpVoucherMapper voucherMapper;

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
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
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

    @GetMapping("/get-import-template")
    @Operation(summary = "获得银行转账导入模板")
    @PreAuthorize("@ss.hasPermission('erp:finance-transfer:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "银行转账导入模板.xls", "银行转账",
                ErpFinanceTransferImportExcelVO.class,
                Collections.singletonList(new ErpFinanceTransferImportExcelVO()));
    }

    @PostMapping("/import")
    @Operation(summary = "导入银行转账")
    @PreAuthorize("@ss.hasPermission('erp:finance-transfer:import')")
    public CommonResult<ErpFinanceImportRespVO> importFinanceTransfer(@RequestParam("file") MultipartFile file)
            throws Exception {
        List<ErpFinanceTransferImportExcelVO> list = ExcelUtils.read(file, ErpFinanceTransferImportExcelVO.class);
        ErpFinanceImportRespVO result = new ErpFinanceImportRespVO();
        for (int i = 0; i < list.size(); i++) {
            ErpFinanceTransferImportExcelVO row = list.get(i);
            if (row == null || allBlank(row.getTransferTime(), row.getOutAccountId(), row.getInAccountId(),
                    row.getTransferPrice(), row.getFinanceUserId(), row.getDeptId(), row.getRemark(), row.getFileUrl())) {
                continue;
            }
            try {
                ErpFinanceTransferSaveReqVO reqVO = BeanUtils.toBean(row, ErpFinanceTransferSaveReqVO.class);
                reqVO.setTransferTime(parseDateTime(row.getTransferTime(), null));
                financeTransferService.createFinanceTransfer(reqVO);
                result.addCreated();
            } catch (Exception ex) {
                result.addFailure(i + 2, row.getTransferTime(), failureReason(ex));
            }
        }
        return success(result);
    }

    private PageResult<ErpFinanceTransferRespVO> buildFinanceTransferVOPageResult(PageResult<ErpFinanceTransferDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(convertListByFlatMap(pageResult.getList(),
                transfer -> Stream.of(transfer.getOutAccountId(), transfer.getInAccountId())));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(pageResult.getList(),
                transfer -> Stream.of(NumberUtils.parseLong(transfer.getCreator()), NumberUtils.parseLong(transfer.getUpdater()),
                        transfer.getFinanceUserId())));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertListByFlatMap(pageResult.getList(),
                transfer -> Stream.of(transfer.getDeptId())));
        Map<Long, String> voucherNoMap = new HashMap<>();
        List<ErpVoucherDO> vouchers = voucherMapper.selectList(new LambdaQueryWrapperX<ErpVoucherDO>()
                .eq(ErpVoucherDO::getSourceBizType, ErpVoucherSourceBizTypeEnum.BANK_TRANSFER.getType())
                .in(ErpVoucherDO::getSourceBizId, convertSet(pageResult.getList(), ErpFinanceTransferDO::getId))
                .orderByDesc(ErpVoucherDO::getId));
        for (ErpVoucherDO voucher : vouchers) {
            voucherNoMap.putIfAbsent(voucher.getSourceBizId(), voucher.getVoucherNo());
        }
        return BeanUtils.toBean(pageResult, ErpFinanceTransferRespVO.class, transfer -> {
            MapUtils.findAndThen(accountMap, transfer.getOutAccountId(), account -> transfer.setOutAccountName(account.getName()));
            MapUtils.findAndThen(accountMap, transfer.getInAccountId(), account -> transfer.setInAccountName(account.getName()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(transfer.getCreator()), user -> transfer.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(transfer.getUpdater()), user -> transfer.setUpdaterName(user.getNickname()));
            MapUtils.findAndThen(userMap, transfer.getFinanceUserId(), user -> transfer.setFinanceUserName(user.getNickname()));
            MapUtils.findAndThen(deptMap, transfer.getDeptId(), dept -> transfer.setDeptName(dept.getName()));
            if (ErpAuditStatus.APPROVE.getStatus().equals(transfer.getStatus())) {
                transfer.setAuditorName(transfer.getUpdaterName());
                transfer.setAuditTime(transfer.getUpdateTime());
            }
            transfer.setVoucherNo(voucherNoMap.get(transfer.getId()));
        });
    }

    private ErpFinanceTransferRespVO buildFinanceTransferRespVO(ErpFinanceTransferDO transfer) {
        PageResult<ErpFinanceTransferDO> pageResult = new PageResult<>(Collections.singletonList(transfer), 1L);
        List<ErpFinanceTransferRespVO> list = buildFinanceTransferVOPageResult(pageResult).getList();
        return CollUtil.getFirst(list);
    }

}
