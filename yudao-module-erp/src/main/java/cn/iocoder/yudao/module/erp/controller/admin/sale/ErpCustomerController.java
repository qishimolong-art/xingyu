package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.bo.ErpCustomerSaleStatsBO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

import cn.hutool.core.collection.CollUtil;

@Tag(name = "管理后台 - ERP 客户")
@RestController
@RequestMapping("/erp/customer")
@Validated
public class ErpCustomerController {

    private static final String FIELD_PERMISSION_MODULE = "erp_customer";

    @Resource
    private ErpCustomerService customerService;

    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;

    @PostMapping("/create")
    @Operation(summary = "创建客户")
    @PreAuthorize("@ss.hasPermission('erp:customer:create')")
    public CommonResult<Long> createCustomer(@Valid @RequestBody ErpCustomerSaveReqVO createReqVO) {
        return success(customerService.createCustomer(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> updateCustomer(@Valid @RequestBody ErpCustomerSaveReqVO updateReqVO) {
        customerService.updateCustomer(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:customer:delete')")
    public CommonResult<Boolean> deleteCustomer(@RequestParam("id") Long id) {
        customerService.deleteCustomer(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<ErpCustomerRespVO> getCustomer(@RequestParam("id") Long id) {
        ErpCustomerDO customer = customerService.getCustomer(id);
        ErpCustomerRespVO respVO = BeanUtils.toBean(customer, ErpCustomerRespVO.class);
        fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户分页")
    @PreAuthorize("@ss.hasPermission('erp:customer:query')")
    public CommonResult<PageResult<ErpCustomerRespVO>> getCustomerPage(@Valid ErpCustomerPageReqVO pageReqVO) {
        PageResult<ErpCustomerDO> pageResult = customerService.getCustomerPage(pageReqVO);
        PageResult<ErpCustomerRespVO> respResult = BeanUtils.toBean(pageResult, ErpCustomerRespVO.class);
        // 批量聚合销售统计（最近销售日期 / 累计销售额 / 应收余额）
        if (CollUtil.isNotEmpty(respResult.getList())) {
            java.util.Collection<Long> customerIds = convertList(respResult.getList(), ErpCustomerRespVO::getId);
            java.util.Map<Long, ErpCustomerSaleStatsBO> statsMap = convertMap(
                    saleOutMapper.selectSaleStatsByCustomerIds(customerIds),
                    ErpCustomerSaleStatsBO::getCustomerId);
            respResult.getList().forEach(vo -> {
                ErpCustomerSaleStatsBO stats = statsMap.get(vo.getId());
                if (stats != null) {
                    if (stats.getLastSaleTime() != null) {
                        vo.setLastSaleDate(stats.getLastSaleTime().toLocalDate());
                    }
                    vo.setTotalSaleAmount(stats.getTotalSaleAmount());
                    vo.setReceivableBalance(stats.getReceivableBalance());
                }
            });
        }
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, respResult.getList());
        return success(respResult);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得客户精简列表", description = "只包含被开启的客户，主要用于前端的下拉选项")
    public CommonResult<List<ErpCustomerRespVO>> getCustomerSimpleList() {
        List<ErpCustomerDO> list = customerService.getCustomerListByStatus(CommonStatusEnum.ENABLE.getStatus());
        List<ErpCustomerRespVO> respList = convertList(list, customer -> new ErpCustomerRespVO().setId(customer.getId())
                .setName(customer.getName()).setContact(customer.getContact()).setMobile(customer.getMobile()));
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, respList);
        return success(respList);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出客户 Excel")
    @PreAuthorize("@ss.hasPermission('erp:customer:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCustomerExcel(@Valid ErpCustomerPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpCustomerDO> list = customerService.getCustomerPage(pageReqVO).getList();
        List<ErpCustomerRespVO> rows = BeanUtils.toBean(list, ErpCustomerRespVO.class);
        fieldPermissionMasker.maskForms(FIELD_PERMISSION_MODULE, rows);
        // 导出 Excel
        ExcelUtils.write(response, "客户.xls", "数据", ErpCustomerRespVO.class, rows);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得客户导入模板")
    @PreAuthorize("@ss.hasPermission('erp:customer:import')")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<ErpCustomerImportExcelVO> list = Collections.singletonList(new ErpCustomerImportExcelVO());
        ExcelUtils.write(response, "客户导入模板.xls", "客户", ErpCustomerImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入客户")
    @PreAuthorize("@ss.hasPermission('erp:customer:import')")
    public CommonResult<Boolean> importCustomer(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpCustomerImportExcelVO> list = ExcelUtils.read(file, ErpCustomerImportExcelVO.class);
        customerService.importCustomerList(list);
        return success(true);
    }

    @PutMapping("/batch-update")
    @Operation(summary = "批量编辑客户")
    @PreAuthorize("@ss.hasPermission('erp:customer:update')")
    public CommonResult<Boolean> batchUpdateCustomer(@Valid @RequestBody ErpCustomerBatchUpdateReqVO reqVO) {
        customerService.batchUpdateCustomer(reqVO);
        return success(true);
    }

}
