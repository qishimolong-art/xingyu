package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerBusinessInfoDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerBusinessInfoService;
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

@Tag(name = "管理后台 - ERP 客户工商信息")
@RestController
@RequestMapping("/erp/customer-business-info")
@Validated
public class ErpCustomerBusinessInfoController {

    @Resource
    private ErpCustomerBusinessInfoService businessInfoService;

    @PostMapping("/create")
    @Operation(summary = "创建客户工商信息")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Long> createBusinessInfo(@Valid @RequestBody ErpCustomerBusinessInfoSaveReqVO createReqVO) {
        return success(businessInfoService.createBusinessInfo(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户工商信息")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> updateBusinessInfo(@Valid @RequestBody ErpCustomerBusinessInfoSaveReqVO updateReqVO) {
        businessInfoService.updateBusinessInfo(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户工商信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> deleteBusinessInfo(@RequestParam("id") Long id) {
        businessInfoService.deleteBusinessInfo(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户工商信息")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<ErpCustomerBusinessInfoRespVO> getBusinessInfo(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(businessInfoService.getBusinessInfo(id), ErpCustomerBusinessInfoRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户工商信息分页")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<ErpCustomerBusinessInfoRespVO>> getBusinessInfoPage(@Valid ErpCustomerBusinessInfoPageReqVO pageReqVO) {
        PageResult<ErpCustomerBusinessInfoDO> pageResult = businessInfoService.getBusinessInfoPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCustomerBusinessInfoRespVO.class));
    }

    @GetMapping("/list-by-customer")
    @Operation(summary = "获得客户工商信息列表")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<List<ErpCustomerBusinessInfoRespVO>> getBusinessInfoListByCustomer(@RequestParam("customerId") Long customerId) {
        return success(BeanUtils.toBean(businessInfoService.getBusinessInfoListByCustomerId(customerId), ErpCustomerBusinessInfoRespVO.class));
    }

}
