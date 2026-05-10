package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask.ErpCustomerTaskPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask.ErpCustomerTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask.ErpCustomerTaskSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerTaskDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerTaskService;
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

@Tag(name = "管理后台 - ERP 客户任务量")
@RestController
@RequestMapping("/erp/customer-task")
@Validated
public class ErpCustomerTaskController {

    @Resource
    private ErpCustomerTaskService taskService;

    @PostMapping("/create")
    @Operation(summary = "创建客户任务量")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Long> createTask(@Valid @RequestBody ErpCustomerTaskSaveReqVO createReqVO) {
        return success(taskService.createTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户任务量")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> updateTask(@Valid @RequestBody ErpCustomerTaskSaveReqVO updateReqVO) {
        taskService.updateTask(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户任务量")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> deleteTask(@RequestParam("id") Long id) {
        taskService.deleteTask(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户任务量")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<ErpCustomerTaskRespVO> getTask(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(taskService.getTask(id), ErpCustomerTaskRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户任务量分页")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<ErpCustomerTaskRespVO>> getTaskPage(@Valid ErpCustomerTaskPageReqVO pageReqVO) {
        PageResult<ErpCustomerTaskDO> pageResult = taskService.getTaskPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCustomerTaskRespVO.class));
    }

    @GetMapping("/list-by-customer")
    @Operation(summary = "获得客户任务量列表")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<List<ErpCustomerTaskRespVO>> getTaskListByCustomer(@RequestParam("customerId") Long customerId) {
        return success(BeanUtils.toBean(taskService.getTaskListByCustomerId(customerId), ErpCustomerTaskRespVO.class));
    }

}
