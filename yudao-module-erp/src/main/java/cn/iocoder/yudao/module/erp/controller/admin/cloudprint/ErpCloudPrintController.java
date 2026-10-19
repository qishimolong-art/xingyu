package cn.iocoder.yudao.module.erp.controller.admin.cloudprint;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDevicePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDeviceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDeviceSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintResumeQueueReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintSubmitSaleOutReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintPreviewFileReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintPreviewSaleOutReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintPreviewSaleOutRespVO;
import cn.iocoder.yudao.module.erp.service.cloudprint.ErpCloudPrintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 云打印")
@RestController
@RequestMapping("/erp/cloud-print")
@Validated
public class ErpCloudPrintController {

    @Resource
    private ErpCloudPrintService cloudPrintService;

    @PostMapping("/task/preview-sale-out")
    @Operation(summary = "生成销售出库单云打印预览")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:print')")
    public CommonResult<ErpCloudPrintPreviewSaleOutRespVO> previewSaleOut(
            @Valid @RequestBody ErpCloudPrintPreviewSaleOutReqVO reqVO) {
        return success(cloudPrintService.previewSaleOut(reqVO.getSaleOutId(), reqVO.getCopies(), reqVO.getTemplateId(),
                reqVO.getItemOrderField(), reqVO.getItemOrderDirection()));
    }

    @PostMapping(value = "/task/preview-sale-out/file", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "获取销售出库单云打印预览 PDF")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:print')")
    public ResponseEntity<byte[]> getSaleOutPreviewFile(
            @Valid @RequestBody ErpCloudPrintPreviewFileReqVO reqVO) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .body(cloudPrintService.getSaleOutPreviewFile(reqVO.getPreviewToken(), reqVO.getPreviewItemId()));
    }

    @PostMapping("/task/submit-sale-out")
    @Operation(summary = "确认销售出库单云打印预览")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:print')")
    public CommonResult<List<ErpCloudPrintTaskRespVO>> submitSaleOut(
            @Valid @RequestBody ErpCloudPrintSubmitSaleOutReqVO reqVO) {
        return success(cloudPrintService.submitSaleOut(reqVO.getPreviewToken()));
    }

    @GetMapping("/task/list-by-sale-out")
    @Operation(summary = "获得销售单云打印记录")
    @PreAuthorize("@ss.hasPermission('erp:sale-out:query')")
    public CommonResult<List<ErpCloudPrintTaskRespVO>> getTaskListBySaleOut(@RequestParam("saleOutId") Long saleOutId) {
        return success(cloudPrintService.getTaskListBySaleOut(saleOutId));
    }

    @GetMapping("/device/enabled-list")
    @Operation(summary = "获得启用云打印设备")
    @PreAuthorize("@ss.hasAnyPermissions('erp:sale-out:print', 'erp:warehouse:query', 'erp:warehouse:update')")
    public CommonResult<List<ErpCloudPrintDeviceRespVO>> getEnabledDevices() {
        return success(cloudPrintService.getEnabledDevices());
    }

    @GetMapping("/device/page")
    @Operation(summary = "获得云打印设备分页")
    @PreAuthorize("@ss.hasPermission('erp:cloud-print-device:query')")
    public CommonResult<PageResult<ErpCloudPrintDeviceRespVO>> getDevicePage(
            @Valid ErpCloudPrintDevicePageReqVO pageReqVO) {
        return success(cloudPrintService.getDevicePage(pageReqVO));
    }

    @GetMapping("/device/get")
    @Operation(summary = "获得云打印设备")
    @PreAuthorize("@ss.hasPermission('erp:cloud-print-device:query')")
    public CommonResult<ErpCloudPrintDeviceRespVO> getDevice(@RequestParam("id") Long id) {
        return success(cloudPrintService.getDevice(id));
    }

    @PostMapping("/device/create")
    @Operation(summary = "创建云打印设备")
    @PreAuthorize("@ss.hasPermission('erp:cloud-print-device:create')")
    public CommonResult<Long> createDevice(@Valid @RequestBody ErpCloudPrintDeviceSaveReqVO createReqVO) {
        return success(cloudPrintService.createDevice(createReqVO));
    }

    @PutMapping("/device/update")
    @Operation(summary = "更新云打印设备")
    @PreAuthorize("@ss.hasPermission('erp:cloud-print-device:update')")
    public CommonResult<Boolean> updateDevice(@Valid @RequestBody ErpCloudPrintDeviceSaveReqVO updateReqVO) {
        cloudPrintService.updateDevice(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/device/delete")
    @Operation(summary = "删除云打印设备")
    @PreAuthorize("@ss.hasPermission('erp:cloud-print-device:delete')")
    public CommonResult<Boolean> deleteDevice(@RequestParam("id") Long id) {
        cloudPrintService.deleteDevice(id);
        return success(true);
    }

    @PutMapping("/device/update-status")
    @Operation(summary = "修改云打印设备启用状态")
    @PreAuthorize("@ss.hasPermission('erp:cloud-print-device:status')")
    public CommonResult<Boolean> updateDeviceStatus(@RequestParam("id") Long id,
                                                    @RequestParam("status") Integer status) {
        cloudPrintService.updateDeviceStatus(id, status);
        return success(true);
    }

    @PutMapping("/device/set-default")
    @Operation(summary = "设置默认云打印设备")
    @PreAuthorize("@ss.hasPermission('erp:cloud-print-device:default')")
    public CommonResult<Boolean> setDefaultDevice(@RequestParam("id") Long id) {
        cloudPrintService.setDefaultDevice(id);
        return success(true);
    }

    @PostMapping("/device/refresh-status")
    @Operation(summary = "刷新云打印设备在线状态")
    @PreAuthorize("@ss.hasPermission('erp:cloud-print-device:refresh')")
    public CommonResult<ErpCloudPrintDeviceRespVO> refreshDeviceStatus(@RequestParam("id") Long id) {
        return success(cloudPrintService.refreshDeviceStatus(id));
    }

    @PostMapping("/device/resume-queue")
    @Operation(summary = "处理故障任务并恢复云打印队列")
    @PreAuthorize("@ss.hasAnyPermissions('erp:cloud-print-device:update', 'erp:cloud-print-device:refresh')")
    public CommonResult<ErpCloudPrintDeviceRespVO> resumeQueue(
            @Valid @RequestBody ErpCloudPrintResumeQueueReqVO reqVO) {
        return success(cloudPrintService.resumeQueue(reqVO.getDeviceId(), reqVO.getFailedTaskAction()));
    }

}
