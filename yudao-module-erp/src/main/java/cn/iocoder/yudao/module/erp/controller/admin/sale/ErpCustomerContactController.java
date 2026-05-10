package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact.ErpCustomerContactPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact.ErpCustomerContactRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact.ErpCustomerContactSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerContactDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerContactService;
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

@Tag(name = "管理后台 - ERP 客户联系人")
@RestController
@RequestMapping("/erp/customer-contact")
@Validated
public class ErpCustomerContactController {

    @Resource
    private ErpCustomerContactService contactService;

    @PostMapping("/create")
    @Operation(summary = "创建客户联系人")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Long> createContact(@Valid @RequestBody ErpCustomerContactSaveReqVO createReqVO) {
        return success(contactService.createContact(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户联系人")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> updateContact(@Valid @RequestBody ErpCustomerContactSaveReqVO updateReqVO) {
        contactService.updateContact(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户联系人")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> deleteContact(@RequestParam("id") Long id) {
        contactService.deleteContact(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户联系人")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<ErpCustomerContactRespVO> getContact(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(contactService.getContact(id), ErpCustomerContactRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户联系人分页")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<ErpCustomerContactRespVO>> getContactPage(@Valid ErpCustomerContactPageReqVO pageReqVO) {
        PageResult<ErpCustomerContactDO> pageResult = contactService.getContactPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCustomerContactRespVO.class));
    }

    @GetMapping("/list-by-customer")
    @Operation(summary = "获得客户联系人列表")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<List<ErpCustomerContactRespVO>> getContactListByCustomer(@RequestParam("customerId") Long customerId) {
        return success(BeanUtils.toBean(contactService.getContactListByCustomerId(customerId), ErpCustomerContactRespVO.class));
    }

}
