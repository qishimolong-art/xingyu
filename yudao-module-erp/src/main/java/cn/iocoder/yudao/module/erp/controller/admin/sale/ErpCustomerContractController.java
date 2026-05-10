package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract.ErpCustomerContractPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract.ErpCustomerContractRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract.ErpCustomerContractSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerContractDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerContractService;
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

@Tag(name = "管理后台 - ERP 客户合同")
@RestController
@RequestMapping("/erp/customer-contract")
@Validated
public class ErpCustomerContractController {

    @Resource
    private ErpCustomerContractService contractService;

    @PostMapping("/create")
    @Operation(summary = "创建客户合同")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Long> createContract(@Valid @RequestBody ErpCustomerContractSaveReqVO createReqVO) {
        return success(contractService.createContract(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户合同")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> updateContract(@Valid @RequestBody ErpCustomerContractSaveReqVO updateReqVO) {
        contractService.updateContract(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户合同")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> deleteContract(@RequestParam("id") Long id) {
        contractService.deleteContract(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户合同")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<ErpCustomerContractRespVO> getContract(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(contractService.getContract(id), ErpCustomerContractRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户合同分页")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<ErpCustomerContractRespVO>> getContractPage(@Valid ErpCustomerContractPageReqVO pageReqVO) {
        PageResult<ErpCustomerContractDO> pageResult = contractService.getContractPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCustomerContractRespVO.class));
    }

    @GetMapping("/list-by-customer")
    @Operation(summary = "获得客户合同列表")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<List<ErpCustomerContractRespVO>> getContractListByCustomer(@RequestParam("customerId") Long customerId) {
        return success(BeanUtils.toBean(contractService.getContractListByCustomerId(customerId), ErpCustomerContractRespVO.class));
    }

}
