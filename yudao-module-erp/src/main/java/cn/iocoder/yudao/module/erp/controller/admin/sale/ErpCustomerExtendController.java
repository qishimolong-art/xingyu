package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerExtendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 客户拓展信息")
@RestController
@RequestMapping("/erp/customer-extend")
@Validated
public class ErpCustomerExtendController {

    @Resource
    private ErpCustomerExtendService extendService;

    @PostMapping("/create")
    @Operation(summary = "创建客户拓展信息")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Long> createExtend(@Valid @RequestBody ErpCustomerExtendSaveReqVO createReqVO) {
        return success(extendService.createExtend(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户拓展信息")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> updateExtend(@Valid @RequestBody ErpCustomerExtendSaveReqVO updateReqVO) {
        extendService.updateExtend(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户拓展信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> deleteExtend(@RequestParam("id") Long id) {
        extendService.deleteExtend(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户拓展信息")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<ErpCustomerExtendRespVO> getExtend(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(extendService.getExtend(id), ErpCustomerExtendRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户拓展信息分页")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<ErpCustomerExtendRespVO>> getExtendPage(@Valid ErpCustomerExtendPageReqVO pageReqVO) {
        PageResult<ErpCustomerExtendDO> pageResult = extendService.getExtendPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCustomerExtendRespVO.class));
    }

    @GetMapping("/list-by-customer")
    @Operation(summary = "获得客户拓展信息列表")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<List<ErpCustomerExtendRespVO>> getExtendListByCustomer(@RequestParam("customerId") Long customerId) {
        return success(BeanUtils.toBean(extendService.getExtendListByCustomerId(customerId), ErpCustomerExtendRespVO.class));
    }

}
