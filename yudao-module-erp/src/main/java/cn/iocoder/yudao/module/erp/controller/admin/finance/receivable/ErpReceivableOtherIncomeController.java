package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable;

import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeItemDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherIncomeService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertListByFlatMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.allBlank;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.failureReason;
import static cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportUtils.parseDateTime;

@Tag(name = "ERP 其他收入")
@RestController
@RequestMapping("/erp/receivable-other-income")
@Validated
public class ErpReceivableOtherIncomeController {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_receivable_other_income";

    @Resource
    private ErpReceivableOtherIncomeService otherIncomeService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @PostMapping("/create")
    @Operation(summary = "创建其他收入")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:create') && "
            + "@ss.hasPermission('erp:receivable-other-income:update-status')")
    public CommonResult<Long> create(@Valid @RequestBody ErpReceivableOtherIncomeSaveReqVO reqVO) {
        return success(otherIncomeService.createOtherIncome(reqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建其他收入草稿")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:create')")
    public CommonResult<Long> createDraft(@RequestBody ErpReceivableOtherIncomeDraftSaveReqVO reqVO) {
        return success(otherIncomeService.createOtherIncomeDraft(reqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交其他收入")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:create') && "
            + "@ss.hasPermission('erp:receivable-other-income:update-status')")
    public CommonResult<Long> createAndSubmit(@Valid @RequestBody ErpReceivableOtherIncomeSaveReqVO reqVO) {
        return success(otherIncomeService.createOtherIncomeAndSubmit(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改其他收入")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody ErpReceivableOtherIncomeSaveReqVO reqVO) {
        otherIncomeService.updateOtherIncome(reqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "修改其他收入草稿")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:update')")
    public CommonResult<Boolean> updateDraft(@RequestBody ErpReceivableOtherIncomeDraftSaveReqVO reqVO) {
        otherIncomeService.updateOtherIncomeDraft(reqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "修改并提交其他收入草稿")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:update') && "
            + "@ss.hasPermission('erp:receivable-other-income:update-status')")
    public CommonResult<Boolean> updateAndSubmit(
            @RequestBody ErpReceivableOtherIncomeDraftSaveReqVO reqVO) {
        otherIncomeService.updateOtherIncomeDraftAndSubmit(reqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交其他收入草稿")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:update-status')")
    public CommonResult<Boolean> submit(@RequestParam("id") Long id) {
        otherIncomeService.submitOtherIncome(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "更新其他收入单备注")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:update')")
    public CommonResult<Boolean> updateRemark(@Valid @RequestBody ErpFinanceUpdateRemarkReqVO reqVO) {
        otherIncomeService.updateOtherIncomeRemark(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改其他收入状态")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:update-status')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                              @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        otherIncomeService.updateOtherIncomeStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除其他收入")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:delete')")
    public CommonResult<Boolean> delete(@RequestParam("ids") java.util.List<Long> ids) {
        otherIncomeService.deleteOtherIncome(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取其他收入")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:query')")
    public CommonResult<ErpReceivableOtherIncomeRespVO> get(@RequestParam("id") Long id) {
        ErpReceivableOtherIncomeDO db = otherIncomeService.getOtherIncome(id);
        if (db == null) {
            return success(null);
        }
        ErpReceivableOtherIncomeRespVO vo = BeanUtils.toBean(db, ErpReceivableOtherIncomeRespVO.class);
        vo.setItems(BeanUtils.toBean(otherIncomeService.getOtherIncomeItemListByIncomeId(id),
                ErpReceivableOtherIncomeRespVO.Item.class));
        fillExtend(vo);
        fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, vo);
        return success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "获取其他收入分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:query')")
    public CommonResult<PageResult<ErpReceivableOtherIncomeRespVO>> page(@Valid ErpReceivableOtherIncomePageReqVO pageReqVO) {
        PageResult<ErpReceivableOtherIncomeDO> pageResult = otherIncomeService.getOtherIncomePage(pageReqVO);
        return success(buildPageResult(pageResult));
    }

    private PageResult<ErpReceivableOtherIncomeRespVO> buildPageResult(PageResult<ErpReceivableOtherIncomeDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        java.util.List<ErpReceivableOtherIncomeItemDO> itemList = otherIncomeService
                .getOtherIncomeItemListByIncomeIds(CollectionUtils.convertSet(pageResult.getList(), ErpReceivableOtherIncomeDO::getId));
        Map<Long, java.util.List<ErpReceivableOtherIncomeItemDO>> itemMap = CollectionUtils.convertMultiMap(
                itemList, ErpReceivableOtherIncomeItemDO::getIncomeId);
        java.util.Set<Long> accountIds = CollectionUtils.convertSet(pageResult.getList(), ErpReceivableOtherIncomeDO::getAccountId);
        accountIds.remove(null);
        Map<Long, ErpAccountDO> accountMap = accountIds.isEmpty() ? java.util.Collections.emptyMap()
                : accountService.getAccountMap(accountIds);
        List<Long> userIds = convertListByFlatMap(pageResult.getList(), item -> Stream.of(item.getHandlerId(),
                NumberUtils.parseLong(item.getCreator()), NumberUtils.parseLong(item.getUpdater())));
        userIds.addAll(CollectionUtils.convertList(itemList, ErpReceivableOtherIncomeItemDO::getHandlerId));
        userIds.remove(null);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> deptIds = CollectionUtils.convertSet(pageResult.getList(), ErpReceivableOtherIncomeDO::getDeptId);
        deptIds.addAll(CollectionUtils.convertSet(itemList, ErpReceivableOtherIncomeItemDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(deptIds);
        PageResult<ErpReceivableOtherIncomeRespVO> result = BeanUtils.toBean(pageResult, ErpReceivableOtherIncomeRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemMap.get(vo.getId()), ErpReceivableOtherIncomeRespVO.Item.class, item -> {
                MapUtils.findAndThen(userMap, item.getHandlerId(), user -> item.setHandlerName(user.getNickname()));
                MapUtils.findAndThen(deptMap, item.getDeptId(), dept -> item.setDeptName(dept.getName()));
            }));
            MapUtils.findAndThen(accountMap, vo.getAccountId(), account -> vo.setAccountName(account.getName()));
            MapUtils.findAndThen(userMap, vo.getHandlerId(), user -> vo.setHandlerName(user.getNickname()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getUpdater()), user -> vo.setUpdaterName(user.getNickname()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
        });
        result.getList().forEach(item -> fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, item));
        return result;
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出其他收入 Excel")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid ErpReceivableOtherIncomePageReqVO pageReqVO,
                            HttpServletResponse response) throws IOException {
        if (CollUtil.isEmpty(pageReqVO.getIds())) {
            pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        }
        PageResult<ErpReceivableOtherIncomeRespVO> voPage = buildPageResult(otherIncomeService.getOtherIncomePage(pageReqVO));
        List<ErpReceivableOtherIncomeExportRespVO> rows = new ArrayList<>();
        for (ErpReceivableOtherIncomeRespVO income : voPage.getList()) {
            List<ErpReceivableOtherIncomeRespVO.Item> items = income.getItems() == null ? Collections.emptyList() : income.getItems();
            if (CollUtil.isEmpty(items)) {
                rows.add(buildExportRow(income, null, true));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                rows.add(buildExportRow(income, items.get(i), i == 0));
            }
        }
        ExcelUtils.write(response, "其他收入.xls", "数据", ErpReceivableOtherIncomeExportRespVO.class, rows);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得其他收入导入模板")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "其他收入导入模板.xls", "其他收入",
                ErpReceivableOtherIncomeImportExcelVO.class,
                Collections.singletonList(new ErpReceivableOtherIncomeImportExcelVO()));
    }

    @PostMapping("/import")
    @Operation(summary = "导入其他收入")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:import')")
    public CommonResult<ErpFinanceImportRespVO> importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpReceivableOtherIncomeImportExcelVO> list = ExcelUtils.read(file,
                ErpReceivableOtherIncomeImportExcelVO.class);
        ErpFinanceImportRespVO result = new ErpFinanceImportRespVO();
        for (int i = 0; i < list.size(); i++) {
            ErpReceivableOtherIncomeImportExcelVO row = list.get(i);
            if (row == null || allBlank(row.getBizTime(), row.getSettleMethod(), row.getAccountId(),
                    row.getIncomeType(), row.getHandlerId(), row.getItemName(), row.getAmount())) {
                continue;
            }
            try {
                ErpReceivableOtherIncomeSaveReqVO reqVO = BeanUtils.toBean(row,
                        ErpReceivableOtherIncomeSaveReqVO.class);
                reqVO.setBizTime(parseDateTime(row.getBizTime(), null));
                ErpReceivableOtherIncomeSaveReqVO.Item item = new ErpReceivableOtherIncomeSaveReqVO.Item();
                item.setItemName(row.getItemName());
                item.setAmount(row.getAmount());
                item.setInvoiceNo(row.getInvoiceNo());
                item.setParty(row.getItemParty());
                item.setDeptId(row.getItemDeptId());
                item.setBizDate(parseDateTime(row.getItemBizDate(), null));
                item.setHandlerId(row.getItemHandlerId());
                item.setQty(row.getQty());
                item.setFreightType(row.getFreightType());
                item.setRemark(row.getItemRemark());
                reqVO.setItems(Collections.singletonList(item));
                otherIncomeService.createOtherIncome(reqVO);
                result.addCreated();
            } catch (Exception ex) {
                result.addFailure(i + 2, row.getItemName(), failureReason(ex));
            }
        }
        return success(result);
    }

    private ErpReceivableOtherIncomeExportRespVO buildExportRow(ErpReceivableOtherIncomeRespVO income,
                                                                ErpReceivableOtherIncomeRespVO.Item item,
                                                                boolean fillMainFields) {
        ErpReceivableOtherIncomeExportRespVO row = fillMainFields
                ? BeanUtils.toBean(income, ErpReceivableOtherIncomeExportRespVO.class)
                : new ErpReceivableOtherIncomeExportRespVO();
        if (item == null) {
            return row;
        }
        row.setItemName(item.getItemName());
        row.setItemAmount(item.getAmount());
        row.setItemInvoiceNo(item.getInvoiceNo());
        row.setItemParty(item.getParty());
        row.setItemDeptName(item.getDeptName());
        row.setItemBizDate(item.getBizDate());
        row.setItemHandlerName(item.getHandlerName());
        row.setItemQty(item.getQty());
        row.setItemFreightType(item.getFreightType());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private void fillExtend(ErpReceivableOtherIncomeRespVO vo) {
        if (vo.getAccountId() != null) {
            ErpAccountDO account = accountService.getAccount(vo.getAccountId());
            if (account != null) {
                vo.setAccountName(account.getName());
            }
        }
        if (vo.getHandlerId() != null) {
            AdminUserRespDTO user = adminUserApi.getUser(vo.getHandlerId());
            if (user != null) {
                vo.setHandlerName(user.getNickname());
            }
        }
        if (vo.getCreator() != null) {
            try {
                AdminUserRespDTO user = adminUserApi.getUser(Long.parseLong(vo.getCreator()));
                if (user != null) {
                    vo.setCreatorName(user.getNickname());
                }
            } catch (Exception ignored) {
            }
        }
        if (vo.getUpdater() != null) {
            try {
                AdminUserRespDTO user = adminUserApi.getUser(Long.parseLong(vo.getUpdater()));
                if (user != null) {
                    vo.setUpdaterName(user.getNickname());
                }
            } catch (Exception ignored) {
            }
        }
        if (vo.getDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(vo.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
        if (CollUtil.isNotEmpty(vo.getItems())) {
            vo.getItems().forEach(item -> {
                if (item.getHandlerId() != null) {
                    AdminUserRespDTO user = adminUserApi.getUser(item.getHandlerId());
                    if (user != null) {
                        item.setHandlerName(user.getNickname());
                    }
                }
                if (item.getDeptId() != null) {
                    DeptRespDTO dept = deptApi.getDept(item.getDeptId());
                    if (dept != null) {
                        item.setDeptName(dept.getName());
                    }
                }
            });
        }
    }
}
