package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountTransactionPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountTransactionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceBillService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpAccountBalanceBO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertListByFlatMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.allBlank;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.failureReason;

@Tag(name = "管理后台 - ERP 结算账户")
@RestController
@RequestMapping("/erp/account")
@Validated
public class ErpAccountController {

    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpFinanceBillService financeBillService;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建结算账户")
    @PreAuthorize("@ss.hasPermission('erp:account:create')")
    public CommonResult<Long> createAccount(@Valid @RequestBody ErpAccountSaveReqVO createReqVO) {
        return success(accountService.createAccount(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新结算账户")
    @PreAuthorize("@ss.hasPermission('erp:account:update')")
    public CommonResult<Boolean> updateAccount(@Valid @RequestBody ErpAccountSaveReqVO updateReqVO) {
        accountService.updateAccount(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-default-status")
    @Operation(summary = "更新结算账户默认状态")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "defaultStatus", description = "默认状态", required = true)
    })
    public CommonResult<Boolean> updateAccountDefaultStatus(@RequestParam("id") Long id,
                                                            @RequestParam("defaultStatus") Boolean defaultStatus) {
        accountService.updateAccountDefaultStatus(id, defaultStatus);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除结算账户")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:account:delete')")
    public CommonResult<Boolean> deleteAccount(@RequestParam("id") Long id) {
        accountService.deleteAccount(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得结算账户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:account:query')")
    public CommonResult<ErpAccountRespVO> getAccount(@RequestParam("id") Long id) {
        ErpAccountDO account = accountService.getAccount(id);
        ErpAccountRespVO respVO = BeanUtils.toBean(account, ErpAccountRespVO.class);
        Map<Long, ErpAccountBalanceBO> balanceMap = accountService.getAccountBalanceMap(Collections.singletonList(id));
        ErpAccountBalanceBO balance = balanceMap.get(id);
        if (balance != null) {
            respVO.setCurrentBalance(balance.getCurrentBalance());
        }
        fillAccountNames(Collections.singletonList(respVO));
        fieldPermissionMasker.maskForm("erp_account", respVO);
        return success(respVO);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得结算账户精简列表", description = "只包含已启用的结算账户，主要用于前端下拉选择")
    public CommonResult<List<ErpAccountRespVO>> getAccountSimpleList() {
        List<ErpAccountDO> list = accountService.getAccountListByStatus(CommonStatusEnum.ENABLE.getStatus());
        Map<Long, ErpAccountBalanceBO> balanceMap = accountService.getAccountBalanceMap(
                convertSet(list, ErpAccountDO::getId));
        return success(convertList(list, account -> new ErpAccountRespVO()
                .setId(account.getId())
                .setName(account.getName())
                .setAccountType(account.getAccountType())
                .setBankName(account.getBankName())
                .setBankAccount(account.getBankAccount())
                .setDefaultStatus(account.getDefaultStatus())
                .setCurrentBalance(balanceMap.containsKey(account.getId())
                        ? balanceMap.get(account.getId()).getCurrentBalance()
                        : null)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得结算账户分页")
    @PreAuthorize("@ss.hasPermission('erp:account:query')")
    public CommonResult<PageResult<ErpAccountRespVO>> getAccountPage(@Valid ErpAccountPageReqVO pageReqVO) {
        PageResult<ErpAccountDO> pageResult = accountService.getAccountPage(pageReqVO);
        Map<Long, ErpAccountBalanceBO> balanceMap = accountService.getAccountBalanceMap(
                convertSet(pageResult.getList(), ErpAccountDO::getId));
        PageResult<ErpAccountRespVO> result = BeanUtils.toBean(pageResult, ErpAccountRespVO.class, account ->
                account.setCurrentBalance(balanceMap.containsKey(account.getId())
                        ? balanceMap.get(account.getId()).getCurrentBalance()
                        : null));
        fillAccountNames(result.getList());
        return success(result);
    }

    @GetMapping("/transaction-page")
    @Operation(summary = "获得结算账户收付款流水分页")
    @PreAuthorize("@ss.hasPermission('erp:account:query')")
    public CommonResult<PageResult<ErpAccountTransactionRespVO>> getAccountTransactionPage(
            @Valid ErpAccountTransactionPageReqVO pageReqVO) {
        return success(financeBillService.getAccountTransactionPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出结算账户 Excel")
    @PreAuthorize("@ss.hasPermission('erp:account:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAccountExcel(@Valid ErpAccountPageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        List<ErpAccountDO> list;
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            list = accountService.getAccountList(pageReqVO.getIds());
        } else {
            pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
            list = accountService.getAccountPage(pageReqVO).getList();
        }
        List<ErpAccountRespVO> rows = BeanUtils.toBean(list, ErpAccountRespVO.class);
        fillAccountNames(rows);
        ExcelUtils.write(response, "结算账户.xls", "数据", ErpAccountRespVO.class, rows);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得结算账户导入模板")
    @PreAuthorize("@ss.hasPermission('erp:account:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "结算账户导入模板.xls", "结算账户",
                ErpAccountImportExcelVO.class, Collections.singletonList(new ErpAccountImportExcelVO()));
    }

    @PostMapping("/import")
    @Operation(summary = "导入结算账户")
    @PreAuthorize("@ss.hasPermission('erp:account:import')")
    public CommonResult<ErpFinanceImportRespVO> importAccount(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpAccountImportExcelVO> list = ExcelUtils.read(file, ErpAccountImportExcelVO.class);
        ErpFinanceImportRespVO result = new ErpFinanceImportRespVO();
        for (int i = 0; i < list.size(); i++) {
            ErpAccountImportExcelVO row = list.get(i);
            if (row == null || allBlank(row.getName(), row.getAccountType(), row.getBankName(), row.getBankAccount(),
                    row.getNo(), row.getDeptId(), row.getRemark(), row.getStatus(), row.getSort())) {
                continue;
            }
            try {
                accountService.createAccount(BeanUtils.toBean(row, ErpAccountSaveReqVO.class));
                result.addCreated();
            } catch (Exception ex) {
                result.addFailure(i + 2, row.getName(), failureReason(ex));
            }
        }
        return success(result);
    }

    private void fillAccountNames(List<ErpAccountRespVO> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(rows, ErpAccountRespVO::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(rows,
                row -> Stream.of(NumberUtils.parseLong(row.getCreator()), NumberUtils.parseLong(row.getUpdater()))));
        for (ErpAccountRespVO row : rows) {
            MapUtils.findAndThen(deptMap, row.getDeptId(), dept -> row.setDeptName(dept.getName()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(row.getCreator()), user -> row.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(row.getUpdater()), user -> row.setUpdaterName(user.getNickname()));
        }
    }

}
