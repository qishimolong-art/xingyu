package cn.iocoder.yudao.module.erp.controller.admin.common;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordDetailPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordFailureDetailExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordRespVO;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 导入导出记录")
@RestController
@RequestMapping("/erp/import-export-record")
@Validated
public class ErpImportExportRecordController {

    private static final String IMPORT_QUERY_PERMISSION = "erp:import-record:query";
    private static final String EXPORT_QUERY_PERMISSION = "erp:export-record:query";

    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private PermissionApi permissionApi;

    @GetMapping("/page")
    @Operation(summary = "获得导入导出记录分页")
    @PreAuthorize("@ss.hasPermission('erp:import-record:query') or @ss.hasPermission('erp:export-record:query')")
    public CommonResult<PageResult<ErpImportExportRecordRespVO>> getRecordPage(
            @Valid ErpImportExportRecordPageReqVO pageReqVO) {
        validateOperationPermission(pageReqVO.getOperationType());
        return success(importExportRecordService.getRecordPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得导入导出记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:import-record:query') or @ss.hasPermission('erp:export-record:query')")
    public CommonResult<ErpImportExportRecordRespVO> getRecord(@RequestParam("id") Long id) {
        ErpImportExportRecordRespVO record = importExportRecordService.getRecord(id);
        if (record != null) {
            validateOperationPermission(record.getOperationType());
        }
        return success(record);
    }

    @GetMapping("/detail-page")
    @Operation(summary = "获得导入失败明细分页")
    @PreAuthorize("@ss.hasPermission('erp:import-record:query') or @ss.hasPermission('erp:export-record:query')")
    public CommonResult<PageResult<ErpImportExportRecordDetailRespVO>> getDetailPage(
            @Valid ErpImportExportRecordDetailPageReqVO pageReqVO) {
        ErpImportExportRecordRespVO record = importExportRecordService.getRecord(pageReqVO.getRecordId());
        if (record != null) {
            validateOperationPermission(record.getOperationType());
        }
        return success(importExportRecordService.getDetailPage(pageReqVO));
    }

    @GetMapping("/failure-details/download")
    @Operation(summary = "下载导入失败明细")
    @PreAuthorize("@ss.hasPermission('erp:import-record:query')")
    public void downloadFailureDetails(@RequestParam("recordId") Long recordId, HttpServletResponse response)
            throws Exception {
        ErpImportExportRecordRespVO record = importExportRecordService.getRecord(recordId);
        if (record != null) {
            validateImportPermission(record.getOperationType());
        } else {
            validateQueryPermission(SecurityFrameworkUtils.getLoginUserId(), IMPORT_QUERY_PERMISSION);
        }
        String moduleName = record == null || record.getModuleName() == null ? "导入" : record.getModuleName();
        ExcelUtils.write(response, moduleName + "失败明细.xls", "失败明细",
                ErpImportExportRecordFailureDetailExportRespVO.class,
                importExportRecordService.getFailureDetailList(recordId));
    }

    private void validateOperationPermission(String operationType) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (ErpImportExportOperationTypeEnum.IMPORT.getType().equals(operationType)) {
            validateQueryPermission(loginUserId, IMPORT_QUERY_PERMISSION);
            return;
        }
        if (ErpImportExportOperationTypeEnum.EXPORT.getType().equals(operationType)) {
            validateQueryPermission(loginUserId, EXPORT_QUERY_PERMISSION);
            return;
        }
        if ((operationType == null || operationType.isEmpty())
                && permissionApi.hasAnyPermissions(loginUserId, IMPORT_QUERY_PERMISSION, EXPORT_QUERY_PERMISSION)) {
            return;
        }
        throw new AccessDeniedException("没有导入导出记录查询权限");
    }

    private void validateImportPermission(String operationType) {
        if (ErpImportExportOperationTypeEnum.IMPORT.getType().equals(operationType)) {
            validateQueryPermission(SecurityFrameworkUtils.getLoginUserId(), IMPORT_QUERY_PERMISSION);
            return;
        }
        throw new AccessDeniedException("没有导入记录查询权限");
    }

    private void validateQueryPermission(Long loginUserId, String permission) {
        if (permissionApi.hasAnyPermissions(loginUserId, permission)) {
            return;
        }
        throw new AccessDeniedException("没有导入导出记录查询权限");
    }

}
