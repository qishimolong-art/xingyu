package cn.iocoder.yudao.module.erp.controller.admin.finance.payable;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableWriteOffReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableAccountDO;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableAccountService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.ErpFinanceSimplePageUtils.buildUserSimplePage;

@Tag(name = "ERP 应付账款")
@RestController
@RequestMapping("/erp/payable-account")
@Validated
public class ErpPayableAccountController {

    @Resource
    private ErpPayableAccountService payableAccountService;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private AdminUserApi adminUserApi;

    @GetMapping("/page")
    @Operation(summary = "获取应付账款分页")
    @PreAuthorize("@ss.hasPermission('erp:payable-account:query')")
    public CommonResult<PageResult<ErpPayableAccountRespVO>> getPayableAccountPage(@Valid ErpPayableAccountPageReqVO reqVO) {
        PageResult<ErpPayableAccountDO> pageResult = payableAccountService.getPayableAccountPage(reqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        return success(BeanUtils.toBean(pageResult, ErpPayableAccountRespVO.class));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "获取应付账款可搜索部门精简信息列表")
    @PreAuthorize("@ss.hasPermission('erp:payable-account:query')")
    public CommonResult<List<DeptSimpleRespVO>> getPayableAccountDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_finance_payable_account"));
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "获取应付账款可搜索部门精简分页")
    @PreAuthorize("@ss.hasPermission('erp:payable-account:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getPayableAccountDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage("erp_finance_payable_account", pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获取应付账款用户精简分页")
    @PreAuthorize("@ss.hasPermission('erp:payable-account:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        return success(buildUserSimplePage(adminUserApi, pageReqVO));
    }

    @GetMapping("/detail")
    @Operation(summary = "获取供应商应付明细流水")
    @PreAuthorize("@ss.hasPermission('erp:payable-account:query')")
    public CommonResult<List<ErpPayableDetailRespVO>> getPayableDetailList(@Valid ErpPayableDetailReqVO reqVO) {
        return success(payableAccountService.getPayableDetailList(reqVO));
    }

    @GetMapping("/export")
    @Operation(summary = "导出应付账款概览 Excel")
    @PreAuthorize("@ss.hasPermission('erp:payable-account:query')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPayableAccount(@Valid ErpPayableAccountPageReqVO reqVO,
                                     HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "应付账款概览.xls", "概览", ErpPayableAccountExportRespVO.class,
                payableAccountService.getPayableAccountList(reqVO).stream()
                        .map(this::toExportVO).collect(Collectors.toList()));
    }

    @GetMapping("/export-detail")
    @Operation(summary = "导出应付账款明细 Excel")
    @PreAuthorize("@ss.hasPermission('erp:payable-account:query')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPayableDetail(@Valid ErpPayableDetailReqVO reqVO,
                                    HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "应付账款明细.xls", "明细", ErpPayableDetailRespVO.class,
                payableAccountService.getPayableDetailList(reqVO));
    }

    @PostMapping("/writeoff")
    @Operation(summary = "核销应付账款")
    @PreAuthorize("@ss.hasPermission('erp:payable-account:writeoff')")
    public CommonResult<Long> writeOffPayable(@Valid @RequestBody ErpPayableWriteOffReqVO reqVO) {
        return success(payableAccountService.writeOffPayable(reqVO));
    }

    private ErpPayableAccountExportRespVO toExportVO(ErpPayableAccountDO source) {
        ErpPayableAccountExportRespVO target = new ErpPayableAccountExportRespVO();
        target.setSupplierName(source.getSupplierName());
        target.setContact(source.getContact());
        target.setMobile(source.getMobile());
        target.setDeptName(source.getDeptName());
        target.setHandlerName(source.getHandlerName());
        target.setPurchaseInAmount(source.getPurchaseInAmount());
        target.setPurchaseReturnAmount(source.getPurchaseReturnAmount());
        target.setPriceAdjustAmount(source.getPriceAdjustAmount());
        target.setMiscPayableAmount(source.getMiscPayableAmount());
        target.setPaymentAmount(source.getPaymentAmount());
        target.setWriteOffAmount(source.getWriteOffAmount());
        target.setBalance(source.getBalance());
        target.setUnclearedPrepayment(source.getUnclearedPrepayment());
        target.setLastBizTime(source.getLastBizTime());
        return target;
    }
}
