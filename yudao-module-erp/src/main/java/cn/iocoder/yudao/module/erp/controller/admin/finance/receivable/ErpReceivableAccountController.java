package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableWriteOffReqVO;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableAccountService;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
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

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "ERP 应收账款")
@RestController
@RequestMapping("/erp/receivable-account")
@Validated
public class ErpReceivableAccountController {

    @Resource
    private ErpReceivableAccountService receivableAccountService;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;

    @GetMapping("/page")
    @Operation(summary = "获得应收账款分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    public CommonResult<PageResult<ErpReceivableAccountRespVO>> getReceivableAccountPage(@Valid ErpReceivableAccountPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(receivableAccountService.getReceivableAccountPage(pageReqVO), ErpReceivableAccountRespVO.class));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "获取应收账款可搜索部门精简信息列表")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    public CommonResult<List<DeptSimpleRespVO>> getReceivableAccountDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList("erp_finance_receivable_account"));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得应收账款明细")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    public CommonResult<List<ErpReceivableDetailRespVO>> getReceivableDetailList(@Valid ErpReceivableDetailReqVO reqVO) {
        return success(receivableAccountService.getReceivableDetailList(reqVO));
    }

    @PostMapping("/writeoff")
    @Operation(summary = "核销应收账款")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:writeoff')")
    public CommonResult<Long> writeOffReceivable(@Valid @RequestBody ErpReceivableWriteOffReqVO reqVO) {
        return success(receivableAccountService.writeOffReceivable(reqVO));
    }

    @GetMapping("/export-detail")
    @Operation(summary = "导出应收账款明细 Excel")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReceivableDetail(@Valid ErpReceivableDetailReqVO reqVO,
                                       HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "应收账款明细.xls", "明细", ErpReceivableDetailRespVO.class,
                receivableAccountService.getReceivableDetailList(reqVO));
    }
}
