package cn.iocoder.yudao.module.erp.controller.admin.finance.payable;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableWriteOffReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableAccountDO;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableAccountService;
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
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "ERP 应付账款")
@RestController
@RequestMapping("/erp/payable-account")
@Validated
public class ErpPayableAccountController {

    @Resource
    private ErpPayableAccountService payableAccountService;

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

    @GetMapping("/detail")
    @Operation(summary = "获取供应商应付明细流水")
    @PreAuthorize("@ss.hasPermission('erp:payable-account:query')")
    public CommonResult<List<ErpPayableDetailRespVO>> getPayableDetailList(@Valid ErpPayableDetailReqVO reqVO) {
        return success(payableAccountService.getPayableDetailList(reqVO));
    }

    @PostMapping("/writeoff")
    @Operation(summary = "核销应付账款")
    @PreAuthorize("@ss.hasPermission('erp:payable-account:writeoff')")
    public CommonResult<Long> writeOffPayable(@Valid @RequestBody ErpPayableWriteOffReqVO reqVO) {
        return success(payableAccountService.writeOffPayable(reqVO));
    }
}
