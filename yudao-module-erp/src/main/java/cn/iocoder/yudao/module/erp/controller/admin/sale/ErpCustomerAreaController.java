package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea.ErpCustomerAreaPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea.ErpCustomerAreaRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea.ErpCustomerAreaSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerAreaDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerAreaService;
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

@Tag(name = "管理后台 - ERP 客户企业地区")
@RestController
@RequestMapping("/erp/customer-area")
@Validated
public class ErpCustomerAreaController {

    @Resource
    private ErpCustomerAreaService areaService;

    @PostMapping("/create")
    @Operation(summary = "创建客户企业地区")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Long> createArea(@Valid @RequestBody ErpCustomerAreaSaveReqVO createReqVO) {
        return success(areaService.createArea(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户企业地区")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> updateArea(@Valid @RequestBody ErpCustomerAreaSaveReqVO updateReqVO) {
        areaService.updateArea(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户企业地区")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> deleteArea(@RequestParam("id") Long id) {
        areaService.deleteArea(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户企业地区")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<ErpCustomerAreaRespVO> getArea(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(areaService.getArea(id), ErpCustomerAreaRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户企业地区分页")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<ErpCustomerAreaRespVO>> getAreaPage(@Valid ErpCustomerAreaPageReqVO pageReqVO) {
        PageResult<ErpCustomerAreaDO> pageResult = areaService.getAreaPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCustomerAreaRespVO.class));
    }

    @GetMapping("/list-by-customer")
    @Operation(summary = "获得客户企业地区列表")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<List<ErpCustomerAreaRespVO>> getAreaListByCustomer(@RequestParam("customerId") Long customerId) {
        return success(BeanUtils.toBean(areaService.getAreaListByCustomerId(customerId), ErpCustomerAreaRespVO.class));
    }

}
