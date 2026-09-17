package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.settlement.ErpFinanceBillPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.settlement.ErpFinanceBillRespVO;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceBillService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
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
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 收付款统一查询")
@RestController
@RequestMapping("/erp/finance-bill")
@Validated
public class ErpFinanceBillController {

    @Resource
    private ErpFinanceBillService financeBillService;
    @Resource
    private DeptApi deptApi;

    @GetMapping("/page")
    @Operation(summary = "获得收付款统一分页")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query') or @ss.hasPermission('erp:finance-payment:query')")
    public CommonResult<PageResult<ErpFinanceBillRespVO>> getFinanceBillPage(@Valid ErpFinanceBillPageReqVO pageReqVO) {
        return success(financeBillService.getFinanceBillPage(pageReqVO));
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "获得收付款统一查询部门精简分页")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query') or @ss.hasPermission('erp:finance-payment:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getFinanceBillDeptSimplePage(@Valid PageParam pageReqVO) {
        PageResult<DeptRespDTO> page = deptApi.getDeptSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageReqVO.getKeyword(), null, pageReqVO);
        List<DeptSimpleRespVO> list = page.getList().stream()
                .map(dept -> new DeptSimpleRespVO(dept.getId(), dept.getName(), dept.getParentId()))
                .collect(Collectors.toList());
        return success(new PageResult<>(list, page.getTotal()));
    }

}
