package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_NOT_EXISTS;

@Tag(name = "Admin - ERP stock transfer in")
@RestController
@RequestMapping("/erp/stock-transfer-in")
@Validated
public class ErpStockTransferInController {

    private static final int EXPORT_MAX_COUNT = 5000;
    private static final ErrorCode EXPORT_COUNT_EXCEEDED = new ErrorCode(1_030_590_003,
            "单次最多导出 5000 条调拨入库单，请缩小筛选范围后重试");
    private static final String FIELD_PERMISSION_MODULE = "erp_stock_transfer_in";
    private static final int TRANSFER_DIRECTION_IN = 20;
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();

    @Resource
    private ErpStockMoveService stockMoveService;
    @Resource
    private ErpStockMoveController stockMoveController;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;

    @GetMapping("/page")
    @Operation(summary = "Get stock transfer-in page")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-in:query')")
    public CommonResult<PageResult<ErpStockMoveRespVO>> getStockTransferInPage(
            @Valid ErpStockMovePageReqVO pageReqVO) {
        pageReqVO.setTransferDirection(TRANSFER_DIRECTION_IN);
        return success(stockMoveController.buildStockMoveVOPageResult(
                stockMoveService.getVisibleStockTransferInPage(pageReqVO), FIELD_PERMISSION_MODULE));
    }

    @GetMapping("/get")
    @Operation(summary = "Get stock transfer-in")
    @Parameter(name = "id", description = "id", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-in:query')")
    public CommonResult<ErpStockMoveRespVO> getStockTransferIn(@RequestParam("id") Long id) {
        ErpStockMoveDO stockMove = stockMoveService.getVisibleStockTransferIn(id);
        if (stockMove == null || !Integer.valueOf(TRANSFER_DIRECTION_IN).equals(stockMove.getTransferDirection())) {
            throw exception(STOCK_MOVE_NOT_EXISTS);
        }
        return stockMoveController.buildStockMoveDetail(stockMove, FIELD_PERMISSION_MODULE);
    }

    @GetMapping("/from-dept-simple-list")
    @Operation(summary = "Get visible stock transfer-in from department simple list")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-in:query')")
    public CommonResult<List<DeptSimpleRespVO>> getFromDeptSimpleList() {
        return success(stockMoveService.getVisibleStockTransferInFromDeptSimpleList());
    }

    @GetMapping("/to-dept-simple-list")
    @Operation(summary = "Get visible stock transfer-in to department simple list")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-in:query')")
    public CommonResult<List<DeptSimpleRespVO>> getToDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList(FIELD_PERMISSION_MODULE));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export stock transfer-in")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-in:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockTransferInExcel(@Valid ErpStockMovePageReqVO pageReqVO,
                                           @RequestParam(value = "fields", required = false) String fields,
                                           HttpServletResponse response) throws IOException {
        pageReqVO.setTransferDirection(TRANSFER_DIRECTION_IN);
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(EXPORT_MAX_COUNT);
        PageResult<ErpStockMoveDO> pageResult = stockMoveService.getVisibleStockTransferInPage(pageReqVO);
        if (pageResult.getTotal() > EXPORT_MAX_COUNT) {
            throw exception(EXPORT_COUNT_EXCEEDED);
        }
        List<ErpStockMoveRespVO> list = stockMoveController.buildStockMoveVOPageResult(
                pageResult, FIELD_PERMISSION_MODULE).getList();
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpStockMoveRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "stock-transfer-in.xls", "data", ErpStockMoveRespVO.class, list, includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get stock transfer-in export fields")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-in:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getStockTransferInExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpStockMoveRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "system");
        map.put("no", "main");
        map.put("deptName", "main");
        map.put("relatedMoveNo", "main");
        map.put("fromDeptName", "main");
        map.put("toDeptName", "main");
        map.put("moveTime", "main");
        map.put("sourceNo", "main");
        map.put("totalCount", "main");
        map.put("totalPrice", "main");
        map.put("status", "main");
        map.put("approveUserName", "system");
        map.put("approveTime", "system");
        map.put("remark", "main");
        map.put("createTime", "system");
        map.put("productNames", "detail");
        map.put("productCodes", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("deptName", "deptId");
        map.put("relatedMoveNo", "relatedMoveNo");
        map.put("fromDeptName", "fromDeptId");
        map.put("toDeptName", "toDeptId");
        map.put("sourceNo", "sourceNo");
        map.put("approveUserName", "approveUserName");
        map.put("productNames", "item_productId");
        map.put("productCodes", "item_productId");
        return map;
    }

}
