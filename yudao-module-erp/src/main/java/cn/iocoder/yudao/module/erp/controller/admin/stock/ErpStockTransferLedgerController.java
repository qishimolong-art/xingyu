package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerDetailPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerTotalRespVO;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockTransferLedgerService;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 调拨出入库台账")
@RestController
@RequestMapping("/erp/stock-transfer-ledger")
@Validated
public class ErpStockTransferLedgerController {

    private static final int EXPORT_MAX_COUNT = 5000;
    private static final String FIELD_PERMISSION_MODULE = "erp_stock_transfer_ledger";
    private static final ErrorCode EXPORT_COUNT_EXCEEDED = new ErrorCode(1_030_505_008,
            "单次最多导出 5000 条调拨台账明细，请缩小筛选范围后重试");
    private static final Map<String, String> EXPORT_PERMISSION_MAP = buildExportPermissionMap();

    @Resource
    private ErpStockTransferLedgerService transferLedgerService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;

    @GetMapping("/page")
    @Operation(summary = "获得调拨出入库台账日汇总分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-ledger:query')")
    public CommonResult<PageResult<ErpStockTransferLedgerSummaryRespVO>> getSummaryPage(
            @Valid ErpStockTransferLedgerPageReqVO reqVO) {
        PageResult<ErpStockTransferLedgerSummaryRespVO> result = transferLedgerService.getSummaryPage(reqVO);
        fieldPermissionMasker.maskReportColumns(FIELD_PERMISSION_MODULE, result.getList());
        return success(result);
    }

    @GetMapping("/total")
    @Operation(summary = "获得调拨出入库台账筛选总计")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-ledger:query')")
    public CommonResult<ErpStockTransferLedgerTotalRespVO> getTotal(
            @Valid ErpStockTransferLedgerPageReqVO reqVO) {
        ErpStockTransferLedgerTotalRespVO result = transferLedgerService.getTotal(reqVO);
        fieldPermissionMasker.maskReportColumns(FIELD_PERMISSION_MODULE, result);
        return success(result);
    }

    @GetMapping("/detail-page")
    @Operation(summary = "获得调拨出入库台账钻取明细")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-ledger:query')")
    public CommonResult<PageResult<ErpStockTransferLedgerDetailRespVO>> getDetailPage(
            @Valid ErpStockTransferLedgerDetailPageReqVO reqVO) {
        PageResult<ErpStockTransferLedgerDetailRespVO> result = transferLedgerService.getDetailPage(reqVO);
        fieldPermissionMasker.maskReportColumns(FIELD_PERMISSION_MODULE, result.getList());
        return success(result);
    }

    @GetMapping("/from-dept-simple-list")
    @Operation(summary = "鑾峰緱褰撳墠鐢ㄦ埛鍙璋冩嫧鍙拌处璋冨嚭閮ㄩ棬绮剧畝鍒楄〃")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-ledger:query')")
    public CommonResult<List<DeptSimpleRespVO>> getFromDeptSimpleList() {
        return success(transferLedgerService.getVisibleFromDeptSimpleList());
    }

    @GetMapping("/from-dept-simple-page")
    @Operation(summary = "获得当前用户可见调拨台账调出部门分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-ledger:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getFromDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(pageDeptSimpleList(transferLedgerService.getVisibleFromDeptSimpleList(), pageReqVO));
    }

    @GetMapping("/to-dept-simple-list")
    @Operation(summary = "鑾峰緱褰撳墠鐢ㄦ埛鍙璋冩嫧鍙拌处璋冨叆閮ㄩ棬绮剧畝鍒楄〃")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-ledger:query')")
    public CommonResult<List<DeptSimpleRespVO>> getToDeptSimpleList() {
        return success(transferLedgerService.getVisibleToDeptSimpleList());
    }

    @GetMapping("/to-dept-simple-page")
    @Operation(summary = "获得当前用户可见调拨台账调入部门分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-ledger:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getToDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(pageDeptSimpleList(transferLedgerService.getVisibleToDeptSimpleList(), pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出调拨出入库台账明细")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-ledger:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid ErpStockTransferLedgerPageReqVO reqVO,
                            HttpServletResponse response) throws IOException {
        List<ErpStockTransferLedgerDetailRespVO> list = transferLedgerService.getExportList(reqVO);
        if (list.size() > EXPORT_MAX_COUNT) {
            throw exception(EXPORT_COUNT_EXCEEDED);
        }
        fieldPermissionMasker.maskReportColumns(FIELD_PERMISSION_MODULE, list);
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(
                ErpStockTransferLedgerDetailRespVO.class, Collections.emptySet(),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_PERMISSION_MAP);
        ExcelUtils.write(response, "调拨出入库台账.xls", "台账明细",
                ErpStockTransferLedgerDetailRespVO.class, list, includeFields);
    }

    private PageResult<DeptSimpleRespVO> pageDeptSimpleList(List<DeptSimpleRespVO> list, PageParam pageReqVO) {
        String keyword = pageReqVO.getKeyword() == null ? null : pageReqVO.getKeyword().trim();
        List<DeptSimpleRespVO> filtered = keyword == null || keyword.isEmpty() ? list : list.stream()
                .filter(dept -> containsKeyword(dept.getName(), keyword)
                        || containsKeyword(String.valueOf(dept.getId()), keyword))
                .collect(Collectors.toList());
        int fromIndex = Math.max(0, (pageReqVO.getPageNo() - 1) * pageReqVO.getPageSize());
        if (fromIndex >= filtered.size()) {
            return new PageResult<>(Collections.emptyList(), (long) filtered.size());
        }
        int toIndex = Math.min(filtered.size(), fromIndex + pageReqVO.getPageSize());
        return new PageResult<>(filtered.subList(fromIndex, toIndex), (long) filtered.size());
    }

    private boolean containsKeyword(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private static Map<String, String> buildExportPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("businessDate", "report_businessDate");
        map.put("transferOutNo", "report_transferOutNo");
        map.put("transferInNo", "report_transferInNo");
        map.put("sourceNo", "report_sourceNo");
        map.put("fromDeptName", "report_fromDeptName");
        map.put("toDeptName", "report_toDeptName");
        map.put("productCode", "report_productCode");
        map.put("productName", "report_productName");
        map.put("fromWarehouseName", "report_fromWarehouseName");
        map.put("toWarehouseName", "report_toWarehouseName");
        map.put("batchNo", "report_batchNo");
        map.put("transferOutCount", "report_transferOutCount");
        map.put("transferInCount", "report_transferInCount");
        map.put("differenceCount", "report_differenceCount");
        map.put("exceptionReason", "report_exceptionReason");
        return map;
    }

}
