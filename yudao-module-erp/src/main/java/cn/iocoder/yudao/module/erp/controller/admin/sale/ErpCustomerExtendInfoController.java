package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.extend.ErpCustomerExtendInfoRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.extend.ErpCustomerExtendInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendInfoDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerExtendInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 客户拓展信息（结构化）")
@RestController
@RequestMapping("/erp/customer-extend-info")
@Validated
public class ErpCustomerExtendInfoController {

    @Resource
    private ErpCustomerExtendInfoService extendInfoService;

    @GetMapping("/get-by-customer")
    @Operation(summary = "获得客户拓展信息（结构化）")
    @Parameter(name = "customerId", description = "客户编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<ErpCustomerExtendInfoRespVO> getByCustomer(@RequestParam("customerId") Long customerId) {
        ErpCustomerExtendInfoDO extendInfo = extendInfoService.getByCustomerId(customerId);
        return success(BeanUtils.toBean(extendInfo, ErpCustomerExtendInfoRespVO.class));
    }

    @PutMapping("/save")
    @Operation(summary = "保存客户拓展信息（结构化）")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Long> save(@Valid @RequestBody ErpCustomerExtendInfoSaveReqVO saveReqVO) {
        return success(extendInfoService.saveExtendInfo(saveReqVO));
    }

}
