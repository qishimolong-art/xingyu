package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable;

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
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeItemDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertListByFlatMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "ERP 其他收入")
@RestController
@RequestMapping("/erp/receivable-other-income")
@Validated
public class ErpReceivableOtherIncomeController {

    @Resource
    private ErpReceivableOtherIncomeService otherIncomeService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建其他收入")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:create')")
    public CommonResult<Long> create(@Valid @RequestBody ErpReceivableOtherIncomeSaveReqVO reqVO) {
        return success(otherIncomeService.createOtherIncome(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改其他收入")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody ErpReceivableOtherIncomeSaveReqVO reqVO) {
        otherIncomeService.updateOtherIncome(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改其他收入状态")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:update-status')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                              @RequestParam("status") Integer status) {
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
        return success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "获取其他收入分页")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:query')")
    public CommonResult<PageResult<ErpReceivableOtherIncomeRespVO>> page(@Valid ErpReceivableOtherIncomePageReqVO pageReqVO) {
        PageResult<ErpReceivableOtherIncomeDO> pageResult = otherIncomeService.getOtherIncomePage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty(pageResult.getTotal()));
        }
        java.util.List<ErpReceivableOtherIncomeItemDO> itemList = otherIncomeService
                .getOtherIncomeItemListByIncomeIds(CollectionUtils.convertSet(pageResult.getList(), ErpReceivableOtherIncomeDO::getId));
        Map<Long, java.util.List<ErpReceivableOtherIncomeItemDO>> itemMap = CollectionUtils.convertMultiMap(
                itemList, ErpReceivableOtherIncomeItemDO::getIncomeId);
        java.util.Set<Long> accountIds = CollectionUtils.convertSet(pageResult.getList(), ErpReceivableOtherIncomeDO::getAccountId);
        accountIds.remove(null);
        Map<Long, ErpAccountDO> accountMap = accountIds.isEmpty() ? java.util.Collections.emptyMap()
                : accountService.getAccountMap(accountIds);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(pageResult.getList(),
                item -> Stream.of(item.getHandlerId(), NumberUtils.parseLong(item.getCreator()))));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(CollectionUtils.convertSet(pageResult.getList(),
                ErpReceivableOtherIncomeDO::getDeptId));
        return success(BeanUtils.toBean(pageResult, ErpReceivableOtherIncomeRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemMap.get(vo.getId()), ErpReceivableOtherIncomeRespVO.Item.class, item -> {
                MapUtils.findAndThen(userMap, item.getHandlerId(), user -> item.setHandlerName(user.getNickname()));
                MapUtils.findAndThen(deptMap, item.getDeptId(), dept -> item.setDeptName(dept.getName()));
            }));
            MapUtils.findAndThen(accountMap, vo.getAccountId(), account -> vo.setAccountName(account.getName()));
            MapUtils.findAndThen(userMap, vo.getHandlerId(), user -> vo.setHandlerName(user.getNickname()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
        }));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出其他收入 Excel")
    @PreAuthorize("@ss.hasPermission('erp:receivable-other-income:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid ErpReceivableOtherIncomePageReqVO pageReqVO,
                            HttpServletResponse response) throws IOException {
        List<ErpReceivableOtherIncomeDO> exportList = getExportList(pageReqVO);
        List<ErpReceivableOtherIncomeItemDO> itemList = otherIncomeService.getOtherIncomeItemListByIncomeIds(
                convertSet(exportList, ErpReceivableOtherIncomeDO::getId));
        Map<Long, List<ErpReceivableOtherIncomeItemDO>> itemMap = convertMultiMap(itemList,
                ErpReceivableOtherIncomeItemDO::getIncomeId);
        Map<Long, ErpAccountDO> accountMap = accountService.getAccountMap(
                convertSet(exportList, ErpReceivableOtherIncomeDO::getAccountId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(exportList,
                item -> Stream.of(item.getHandlerId(), NumberUtils.parseLong(item.getCreator()))));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(exportList, ErpReceivableOtherIncomeDO::getDeptId));
        List<ErpReceivableOtherIncomeExportRespVO> rows = new ArrayList<>();
        for (ErpReceivableOtherIncomeDO income : exportList) {
            List<ErpReceivableOtherIncomeItemDO> items = itemMap.getOrDefault(income.getId(), Collections.emptyList());
            if (CollUtil.isEmpty(items)) {
                rows.add(buildExportRow(income, null, true, accountMap, userMap, deptMap));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                rows.add(buildExportRow(income, items.get(i), i == 0, accountMap, userMap, deptMap));
            }
        }
        ExcelUtils.write(response, "其他收入.xls", "数据", ErpReceivableOtherIncomeExportRespVO.class, rows);
    }

    private List<ErpReceivableOtherIncomeDO> getExportList(ErpReceivableOtherIncomePageReqVO pageReqVO) {
        if (CollUtil.isNotEmpty(pageReqVO.getIds())) {
            return otherIncomeService.getOtherIncomePage(pageReqVO).getList();
        }
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        return otherIncomeService.getOtherIncomePage(pageReqVO).getList();
    }

    private ErpReceivableOtherIncomeExportRespVO buildExportRow(ErpReceivableOtherIncomeDO income,
                                                                ErpReceivableOtherIncomeItemDO item,
                                                                boolean fillMainFields,
                                                                Map<Long, ErpAccountDO> accountMap,
                                                                Map<Long, AdminUserRespDTO> userMap,
                                                                Map<Long, DeptRespDTO> deptMap) {
        ErpReceivableOtherIncomeExportRespVO row = fillMainFields
                ? BeanUtils.toBean(income, ErpReceivableOtherIncomeExportRespVO.class)
                : new ErpReceivableOtherIncomeExportRespVO();
        if (fillMainFields) {
            MapUtils.findAndThen(accountMap, income.getAccountId(), account -> row.setAccountName(account.getName()));
            MapUtils.findAndThen(userMap, income.getHandlerId(), user -> row.setHandlerName(user.getNickname()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(income.getCreator()), user -> row.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(deptMap, income.getDeptId(), dept -> row.setDeptName(dept.getName()));
        }
        if (item == null) {
            return row;
        }
        row.setItemName(item.getItemName());
        row.setItemAmount(item.getAmount());
        row.setItemInvoiceNo(item.getInvoiceNo());
        row.setItemParty(item.getParty());
        MapUtils.findAndThen(deptMap, item.getDeptId(), dept -> row.setItemDeptName(dept.getName()));
        row.setItemBizDate(item.getBizDate());
        MapUtils.findAndThen(userMap, item.getHandlerId(), user -> row.setItemHandlerName(user.getNickname()));
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
