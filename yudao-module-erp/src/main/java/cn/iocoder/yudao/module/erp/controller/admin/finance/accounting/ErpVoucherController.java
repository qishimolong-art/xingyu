package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher.ErpVoucherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.ErpFinanceSimplePageUtils.buildUserSimplePage;

@Tag(name = "管理后台 - ERP 凭证")
@RestController
@RequestMapping("/erp/voucher")
@Validated
public class ErpVoucherController {

    @Resource
    private ErpVoucherService voucherService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建凭证")
    @PreAuthorize("@ss.hasPermission('erp:voucher:create')")
    public CommonResult<Long> createVoucher(@Valid @RequestBody ErpVoucherSaveReqVO createReqVO) {
        return success(voucherService.createVoucher(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新凭证")
    @PreAuthorize("@ss.hasPermission('erp:voucher:update')")
    public CommonResult<Boolean> updateVoucher(@Valid @RequestBody ErpVoucherSaveReqVO updateReqVO) {
        voucherService.updateVoucher(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除凭证")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:voucher:delete')")
    public CommonResult<Boolean> deleteVoucher(@RequestParam("id") Long id) {
        voucherService.deleteVoucher(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得凭证")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:voucher:query')")
    public CommonResult<ErpVoucherRespVO> getVoucher(@RequestParam("id") Long id) {
        ErpVoucherDO voucher = voucherService.getVoucher(id);
        if (voucher == null) {
            return success(null);
        }
        List<ErpVoucherItemDO> items = voucherService.getVoucherItemListByVoucherId(id);
        return success(BeanUtils.toBean(voucher, ErpVoucherRespVO.class, vo -> {
            fillPeriod(vo);
            vo.setItems(BeanUtils.toBean(items, ErpVoucherItemRespVO.class));
        }));
    }

    @GetMapping("/page")
    @Operation(summary = "获得凭证分页")
    @PreAuthorize("@ss.hasPermission('erp:voucher:query')")
    public CommonResult<PageResult<ErpVoucherRespVO>> getVoucherPage(@Valid ErpVoucherPageReqVO pageReqVO) {
        PageResult<ErpVoucherDO> pageResult = voucherService.getVoucherPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        PageResult<ErpVoucherRespVO> respPageResult = BeanUtils.toBean(pageResult, ErpVoucherRespVO.class);
        respPageResult.getList().forEach(this::fillPeriod);
        return success(respPageResult);
    }

    @PutMapping("/audit")
    @Operation(summary = "审核凭证")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:voucher:audit')")
    public CommonResult<Boolean> auditVoucher(@RequestParam("id") Long id) {
        voucherService.auditVoucher(id);
        return success(true);
    }

    @PutMapping("/process")
    @Operation(summary = "反审核凭证")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:voucher:process')")
    public CommonResult<Boolean> processVoucher(@RequestParam("id") Long id) {
        voucherService.processVoucher(id);
        return success(true);
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获得凭证用户精简分页")
    @PreAuthorize("@ss.hasPermission('erp:voucher:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        return success(buildUserSimplePage(adminUserApi, pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出凭证 Excel")
    @PreAuthorize("@ss.hasPermission('erp:voucher:query')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportVoucherExcel(@Valid ErpVoucherPageReqVO pageReqVO,
                                    HttpServletResponse response) throws IOException {
        List<ErpVoucherDO> exportList = getVoucherExportList(pageReqVO);
        ExcelUtils.write(response, "凭证.xls", "数据", ErpVoucherRespVO.class,
                BeanUtils.toBean(exportList, ErpVoucherRespVO.class, this::fillPeriod));
    }

    private List<ErpVoucherDO> getVoucherExportList(ErpVoucherPageReqVO pageReqVO) {
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            return voucherService.getVoucherList(pageReqVO.getIds());
        }
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        return voucherService.getVoucherPage(pageReqVO).getList();
    }

    private void fillPeriod(ErpVoucherRespVO vo) {
        if (vo.getPeriodYear() == null || vo.getPeriodMonth() == null) {
            return;
        }
        vo.setPeriod(String.format("%d-%02d", vo.getPeriodYear(), vo.getPeriodMonth()));
    }

}
