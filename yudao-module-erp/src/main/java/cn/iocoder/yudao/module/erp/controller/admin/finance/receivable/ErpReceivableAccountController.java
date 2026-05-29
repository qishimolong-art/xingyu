package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "ERP 应收账款")
@RestController
@RequestMapping("/erp/receivable-account")
@Validated
public class ErpReceivableAccountController {

    @Resource
    private ErpReceivableAccountService receivableAccountService;

    @GetMapping("/page")
    @Operation(summary = "获得应收账款分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    public CommonResult<PageResult<ErpReceivableAccountRespVO>> getReceivableAccountPage(@Valid ErpReceivableAccountPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(receivableAccountService.getReceivableAccountPage(pageReqVO), ErpReceivableAccountRespVO.class));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得应收账款明细")
    @PreAuthorize("@ss.hasPermission('erp:receivable-account:query')")
    public CommonResult<List<ErpReceivableDetailRespVO>> getReceivableDetailList(@Valid ErpReceivableDetailReqVO reqVO) {
        return success(receivableAccountService.getReceivableDetailList(reqVO));
    }
}
