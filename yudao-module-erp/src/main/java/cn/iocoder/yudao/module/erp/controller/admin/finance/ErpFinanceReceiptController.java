package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptWriteOffCandidateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptWriteOffReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptWriteOffReverseReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceReceiptService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.allBlank;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.failureReason;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.parseDateTime;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.zeroIfNull;

@Tag(name = "管理后台 - ERP 收款单")
@RestController
@RequestMapping("/erp/finance-receipt")
@Validated
public class ErpFinanceReceiptController {

    @Resource
    private ErpFinanceReceiptService financeReceiptService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpVoucherMapper voucherMapper;

    @PostMapping("/create")
    @Operation(summary = "创建收款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:create')")
    public CommonResult<Long> createFinanceReceipt(@Valid @RequestBody ErpFinanceReceiptSaveReqVO createReqVO) {
        return success(financeReceiptService.createFinanceReceipt(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新收款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:update')")
    public CommonResult<Boolean> updateFinanceReceipt(@Valid @RequestBody ErpFinanceReceiptSaveReqVO updateReqVO) {
        financeReceiptService.updateFinanceReceipt(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "审核收款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:update-status')")
    public CommonResult<Boolean> approveFinanceReceipt(@RequestParam("id") Long id,
                                                       @RequestParam(value = "status", defaultValue = "20") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        financeReceiptService.approveFinanceReceipt(id);
        return success(true);
    }

    @GetMapping("/writeoff-candidates")
    @Operation(summary = "获得收款单可核销业务单据")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:writeoff')")
    public CommonResult<List<ErpFinanceReceiptWriteOffCandidateRespVO>> getWriteOffCandidates(
            @RequestParam("receiptId") Long receiptId) {
        return success(financeReceiptService.getWriteOffCandidates(receiptId));
    }

    @PostMapping("/writeoff")
    @Operation(summary = "收款单后续核销")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:writeoff')")
    public CommonResult<Boolean> writeOffFinanceReceipt(
            @Valid @RequestBody ErpFinanceReceiptWriteOffReqVO reqVO) {
        financeReceiptService.writeOffFinanceReceipt(reqVO);
        return success(true);
    }

    @PostMapping("/writeoff-reverse")
    @Operation(summary = "撤销收款核销")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:writeoff-reverse')")
    public CommonResult<Boolean> reverseFinanceReceiptWriteOff(
            @Valid @RequestBody ErpFinanceReceiptWriteOffReverseReqVO reqVO) {
        financeReceiptService.reverseFinanceReceiptWriteOff(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除收款单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:delete')")
    public CommonResult<Boolean> deleteFinanceReceipt(@RequestParam("ids") List<Long> ids) {
        financeReceiptService.deleteFinanceReceipt(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得收款单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query')")
    public CommonResult<ErpFinanceReceiptRespVO> getFinanceReceipt(@RequestParam("id") Long id) {
        ErpFinanceReceiptDO receipt = financeReceiptService.getFinanceReceipt(id);
        if (receipt == null) {
            return success(null);
        }
        List<ErpFinanceReceiptItemDO> receiptItemList = financeReceiptService.getFinanceReceiptItemListByReceiptId(id);
        ErpFinanceReceiptRespVO respVO = BeanUtils.toBean(receipt, ErpFinanceReceiptRespVO.class,
                financeReceiptVO -> financeReceiptVO.setItems(
                        BeanUtils.toBean(receiptItemList, ErpFinanceReceiptRespVO.Item.class)));
        fillWriteOffSummary(Collections.singletonList(respVO));
        fillFinanceReceiptNames(Collections.singletonList(respVO));
        fieldPermissionMasker.maskFormWithItems("erp_finance_receipt", respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得收款单分页")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:query')")
    public CommonResult<PageResult<ErpFinanceReceiptRespVO>> getFinanceReceiptPage(@Valid ErpFinanceReceiptPageReqVO pageReqVO) {
        PageResult<ErpFinanceReceiptDO> pageResult = financeReceiptService.getFinanceReceiptPage(pageReqVO);
        return success(buildFinanceReceiptVOPageResult(pageResult));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出收款单 Excel")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFinanceReceiptExcel(@Valid ErpFinanceReceiptPageReqVO pageReqVO,
                                          HttpServletResponse response) throws IOException {
        List<ErpFinanceReceiptDO> exportList = getFinanceReceiptExportList(pageReqVO);
        List<ErpFinanceReceiptItemDO> receiptItemList = financeReceiptService.getFinanceReceiptItemListByReceiptIds(
                convertSet(exportList, ErpFinanceReceiptDO::getId));
        Map<Long, List<ErpFinanceReceiptItemDO>> financeReceiptItemMap = convertMultiMap(receiptItemList,
                ErpFinanceReceiptItemDO::getReceiptId);
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(exportList, ErpFinanceReceiptDO::getCustomerId));
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(
                convertSet(exportList, ErpFinanceReceiptDO::getAccountId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(exportList,
                receipt -> Stream.of(NumberUtils.parseLong(receipt.getCreator()), receipt.getFinanceUserId())));
        ExcelUtils.write(response, "收款单.xls", "数据", ErpFinanceReceiptExportRespVO.class,
                buildFinanceReceiptExportList(exportList, financeReceiptItemMap, customerMap, accountMap, userMap));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得收款单导入模板")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "收款单导入模板.xls", "收款单",
                ErpFinanceReceiptImportExcelVO.class,
                Collections.singletonList(new ErpFinanceReceiptImportExcelVO()));
    }

    @PostMapping("/import")
    @Operation(summary = "导入收款单")
    @PreAuthorize("@ss.hasPermission('erp:finance-receipt:import')")
    public CommonResult<ErpFinanceImportRespVO> importFinanceReceipt(@RequestParam("file") MultipartFile file)
            throws Exception {
        List<ErpFinanceReceiptImportExcelVO> list = ExcelUtils.read(file, ErpFinanceReceiptImportExcelVO.class);
        ErpFinanceImportRespVO result = new ErpFinanceImportRespVO();
        for (int i = 0; i < list.size(); i++) {
            ErpFinanceReceiptImportExcelVO row = list.get(i);
            if (row == null || allBlank(row.getReceiptTime(), row.getCustomerId(), row.getAccountId(),
                    row.getTotalPrice(), row.getReceiptPrice(), row.getBizType(), row.getBizId(),
                    row.getItemReceiptPrice())) {
                continue;
            }
            try {
                ErpFinanceReceiptSaveReqVO reqVO = BeanUtils.toBean(row, ErpFinanceReceiptSaveReqVO.class);
                reqVO.setReceiptTime(parseDateTime(row.getReceiptTime(), null));
                reqVO.setDiscountPrice(zeroIfNull(row.getDiscountPrice()));
                ErpFinanceReceiptSaveReqVO.Item item = new ErpFinanceReceiptSaveReqVO.Item();
                item.setBizType(row.getBizType());
                item.setBizId(row.getBizId());
                item.setReceiptedPrice(zeroIfNull(row.getReceiptedPrice()));
                item.setReceiptPrice(row.getItemReceiptPrice());
                item.setRemark(row.getItemRemark());
                reqVO.setItems(Collections.singletonList(item));
                financeReceiptService.createFinanceReceipt(reqVO);
                result.addCreated();
            } catch (Exception ex) {
                result.addFailure(i + 2, row.getReceiptTime(), failureReason(ex));
            }
        }
        return success(result);
    }

    private PageResult<ErpFinanceReceiptRespVO> buildFinanceReceiptVOPageResult(PageResult<ErpFinanceReceiptDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpFinanceReceiptItemDO> receiptItemList = financeReceiptService.getFinanceReceiptItemListByReceiptIds(
                convertSet(pageResult.getList(), ErpFinanceReceiptDO::getId));
        Map<Long, List<ErpFinanceReceiptItemDO>> financeReceiptItemMap = convertMultiMap(receiptItemList,
                ErpFinanceReceiptItemDO::getReceiptId);
        PageResult<ErpFinanceReceiptRespVO> result = BeanUtils.toBean(pageResult,
                ErpFinanceReceiptRespVO.class, receipt -> {
            receipt.setItems(BeanUtils.toBean(financeReceiptItemMap.get(receipt.getId()), ErpFinanceReceiptRespVO.Item.class));
        });
        fillFinanceReceiptNames(result.getList());
        fillWriteOffSummary(result.getList());
        return result;
    }

    private void fillWriteOffSummary(List<ErpFinanceReceiptRespVO> rows) {
        for (ErpFinanceReceiptRespVO row : rows) {
            List<ErpFinanceReceiptRespVO.Item> items = row.getItems() == null
                    ? Collections.emptyList() : row.getItems();
            BigDecimal allocatedPrice = items.stream()
                    .filter(item -> ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus().equals(item.getWriteOffStatus()))
                    .map(ErpFinanceReceiptRespVO.Item::getReceiptPrice)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalPrice = row.getTotalPrice() == null ? BigDecimal.ZERO : row.getTotalPrice();
            BigDecimal unallocatedPrice = totalPrice.subtract(allocatedPrice);
            row.setAllocatedPrice(allocatedPrice).setUnallocatedPrice(unallocatedPrice)
                    .setWriteOffCount((int) items.stream()
                            .filter(item -> ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus()
                                    .equals(item.getWriteOffStatus())).count());
            if (allocatedPrice.compareTo(BigDecimal.ZERO) < 0 || allocatedPrice.compareTo(totalPrice) > 0) {
                row.setWriteOffStatus(3);
            } else if (allocatedPrice.compareTo(BigDecimal.ZERO) == 0) {
                row.setWriteOffStatus(0);
            } else if (allocatedPrice.compareTo(totalPrice) == 0) {
                row.setWriteOffStatus(2);
            } else {
                row.setWriteOffStatus(1);
            }
        }
    }

    private void fillFinanceReceiptNames(List<ErpFinanceReceiptRespVO> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(rows, ErpFinanceReceiptRespVO::getCustomerId));
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(
                convertSet(rows, ErpFinanceReceiptRespVO::getAccountId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(rows, ErpFinanceReceiptRespVO::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(rows,
                row -> Stream.of(parseUserId(row.getCreator()), parseUserId(row.getUpdater()), row.getFinanceUserId())));
        Map<Long, String> voucherNoMap = new HashMap<>();
        List<ErpVoucherDO> vouchers = voucherMapper.selectList(new LambdaQueryWrapperX<ErpVoucherDO>()
                .eq(ErpVoucherDO::getSourceBizType, ErpVoucherSourceBizTypeEnum.RECEIPT.getType())
                .in(ErpVoucherDO::getSourceBizId, convertSet(rows, ErpFinanceReceiptRespVO::getId))
                .orderByDesc(ErpVoucherDO::getId));
        for (ErpVoucherDO voucher : vouchers) {
            voucherNoMap.putIfAbsent(voucher.getSourceBizId(), voucher.getVoucherNo());
        }
        for (ErpFinanceReceiptRespVO row : rows) {
            MapUtils.findAndThen(customerMap, row.getCustomerId(), customer -> {
                row.setCustomerName(customer.getName());
                row.setSettleMethod(getCustomerSettleMethodName(customer.getSettleMethod()));
                row.setBankName(customer.getBankName());
            });
            MapUtils.findAndThen(accountMap, row.getAccountId(), account -> {
                row.setAccountName(account.getName());
                if (account.getBankName() != null && !account.getBankName().isEmpty()) {
                    row.setBankName(account.getBankName());
                }
            });
            MapUtils.findAndThen(deptMap, row.getDeptId(), dept -> row.setDeptName(dept.getName()));
            MapUtils.findAndThen(userMap, parseUserId(row.getCreator()), user -> row.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, parseUserId(row.getUpdater()), user -> row.setUpdaterName(user.getNickname()));
            MapUtils.findAndThen(userMap, row.getFinanceUserId(), user -> row.setFinanceUserName(user.getNickname()));
            if (ErpAuditStatus.APPROVE.getStatus().equals(row.getStatus())) {
                row.setAuditorName(row.getUpdaterName());
                row.setAuditTime(row.getUpdateTime());
            }
            row.setVoucherNo(voucherNoMap.get(row.getId()));
        }
    }

    private String getCustomerSettleMethodName(Integer settleMethod) {
        if (settleMethod == null) {
            return null;
        }
        switch (settleMethod) {
            case 1:
                return "现金";
            case 2:
                return "挂账";
            case 3:
                return "汇款";
            case 4:
                return "网上支付";
            default:
                return String.valueOf(settleMethod);
        }
    }

    private List<ErpFinanceReceiptDO> getFinanceReceiptExportList(ErpFinanceReceiptPageReqVO pageReqVO) {
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            return financeReceiptService.getFinanceReceiptList(pageReqVO.getIds());
        }
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        return financeReceiptService.getFinanceReceiptPage(pageReqVO).getList();
    }

    private List<ErpFinanceReceiptExportRespVO> buildFinanceReceiptExportList(List<ErpFinanceReceiptDO> list,
                                                                              Map<Long, List<ErpFinanceReceiptItemDO>> financeReceiptItemMap,
                                                                              Map<Long, ErpCustomerDO> customerMap,
                                                                              Map<Long, ErpAccountDO> accountMap,
                                                                              Map<Long, AdminUserRespDTO> userMap) {
        List<ErpFinanceReceiptExportRespVO> rows = new ArrayList<>();
        for (ErpFinanceReceiptDO receipt : list) {
            List<ErpFinanceReceiptItemDO> items = financeReceiptItemMap.getOrDefault(receipt.getId(), Collections.emptyList());
            if (CollUtil.isEmpty(items)) {
                rows.add(buildFinanceReceiptExportRow(receipt, null, true, customerMap, accountMap, userMap));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                rows.add(buildFinanceReceiptExportRow(receipt, items.get(i), i == 0, customerMap, accountMap, userMap));
            }
        }
        return rows;
    }

    private ErpFinanceReceiptExportRespVO buildFinanceReceiptExportRow(ErpFinanceReceiptDO receipt,
                                                                       ErpFinanceReceiptItemDO item,
                                                                       boolean fillMainFields,
                                                                       Map<Long, ErpCustomerDO> customerMap,
                                                                       Map<Long, ErpAccountDO> accountMap,
                                                                       Map<Long, AdminUserRespDTO> userMap) {
        ErpFinanceReceiptExportRespVO row = fillMainFields
                ? BeanUtils.toBean(receipt, ErpFinanceReceiptExportRespVO.class)
                : new ErpFinanceReceiptExportRespVO();
        if (fillMainFields) {
            MapUtils.findAndThen(customerMap, receipt.getCustomerId(), customer -> row.setCustomerName(customer.getName()));
            MapUtils.findAndThen(accountMap, receipt.getAccountId(), account -> row.setAccountName(account.getName()));
            MapUtils.findAndThen(userMap, receipt.getFinanceUserId(), user -> row.setFinanceUserName(user.getNickname()));
            MapUtils.findAndThen(userMap, parseUserId(receipt.getCreator()), user -> row.setCreatorName(user.getNickname()));
        }
        if (item == null) {
            return row;
        }
        row.setBizTypeName(getBizTypeName(item.getBizType()));
        row.setBizNo(item.getBizNo());
        row.setItemTotalPrice(item.getTotalPrice());
        row.setItemReceiptedPrice(item.getReceiptedPrice());
        row.setItemReceiptPrice(item.getReceiptPrice());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private String getBizTypeName(Integer bizType) {
        if (bizType == null) {
            return null;
        }
        for (ErpBizTypeEnum value : ErpBizTypeEnum.values()) {
            if (value.getType().equals(bizType)) {
                return value.getName();
            }
        }
        return String.valueOf(bizType);
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
