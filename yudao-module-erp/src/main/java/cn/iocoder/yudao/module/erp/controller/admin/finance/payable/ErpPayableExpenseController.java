package cn.iocoder.yudao.module.erp.controller.admin.finance.payable;

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
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpensePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableExpenseService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertListByFlatMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "ERP 费用支付")
@RestController
@RequestMapping("/erp/payable-expense")
@Validated
public class ErpPayableExpenseController {

    @Resource
    private ErpPayableExpenseService payableExpenseService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建费用支付")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:create')")
    public CommonResult<Long> create(@Valid @RequestBody ErpPayableExpenseSaveReqVO reqVO) {
        return success(payableExpenseService.createPayableExpense(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改费用支付")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody ErpPayableExpenseSaveReqVO reqVO) {
        payableExpenseService.updatePayableExpense(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改费用支付状态")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:update-status')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                              @RequestParam("status") Integer status) {
        payableExpenseService.updatePayableExpenseStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除费用支付")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:delete')")
    public CommonResult<Boolean> delete(@RequestParam("ids") List<Long> ids) {
        payableExpenseService.deletePayableExpense(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取费用支付")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:query')")
    public CommonResult<ErpPayableExpenseRespVO> get(@RequestParam("id") Long id) {
        ErpPayableExpenseDO db = payableExpenseService.getPayableExpense(id);
        if (db == null) {
            return success(null);
        }
        ErpPayableExpenseRespVO vo = BeanUtils.toBean(db, ErpPayableExpenseRespVO.class);
        vo.setItems(BeanUtils.toBean(payableExpenseService.getPayableExpenseItemListByExpenseId(id),
                ErpPayableExpenseRespVO.Item.class));
        fillExtend(vo);
        return success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "获取费用支付分页")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:query')")
    public CommonResult<PageResult<ErpPayableExpenseRespVO>> page(@Valid ErpPayableExpensePageReqVO reqVO) {
        PageResult<ErpPayableExpenseDO> pageResult = payableExpenseService.getPayableExpensePage(reqVO);
        return success(buildPageResult(pageResult));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出费用支付 Excel")
    @PreAuthorize("@ss.hasPermission('erp:payable-expense:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid ErpPayableExpensePageReqVO reqVO,
                            HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpPayableExpenseRespVO> voPage = buildPageResult(payableExpenseService.getPayableExpensePage(reqVO));
        List<ErpPayableExpenseExportRespVO> rows = new ArrayList<>();
        for (ErpPayableExpenseRespVO expense : voPage.getList()) {
            List<ErpPayableExpenseRespVO.Item> items = expense.getItems() == null ? Collections.emptyList() : expense.getItems();
            if (CollUtil.isEmpty(items)) {
                rows.add(buildExportRow(expense, null, true));
                continue;
            }
            for (int i = 0; i < items.size(); i++) {
                rows.add(buildExportRow(expense, items.get(i), i == 0));
            }
        }
        ExcelUtils.write(response, "费用支付.xls", "数据", ErpPayableExpenseExportRespVO.class, rows);
    }

    private PageResult<ErpPayableExpenseRespVO> buildPageResult(PageResult<ErpPayableExpenseDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpPayableExpenseItemDO> itemList = payableExpenseService.getPayableExpenseItemListByExpenseIds(
                convertSet(pageResult.getList(), ErpPayableExpenseDO::getId));
        Map<Long, List<ErpPayableExpenseItemDO>> itemMap = convertMultiMap(itemList,
                ErpPayableExpenseItemDO::getExpenseId);
        Set<Long> accountIds = convertSet(pageResult.getList(), ErpPayableExpenseDO::getAccountId);
        accountIds.remove(null);
        Map<Long, ErpAccountDO> accountMap = accountIds.isEmpty()
                ? Collections.emptyMap() : accountService.getAccountMap(accountIds);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(pageResult.getList(),
                item -> Stream.of(item.getHandlerId(), NumberUtils.parseLong(item.getCreator()))));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(CollectionUtils.convertSet(pageResult.getList(),
                ErpPayableExpenseDO::getDeptId));
        return BeanUtils.toBean(pageResult, ErpPayableExpenseRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemMap.get(vo.getId()), ErpPayableExpenseRespVO.Item.class));
            MapUtils.findAndThen(accountMap, vo.getAccountId(), account -> vo.setAccountName(account.getName()));
            MapUtils.findAndThen(userMap, vo.getHandlerId(), user -> vo.setHandlerName(user.getNickname()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillItemExtend(vo);
        });
    }

    private ErpPayableExpenseExportRespVO buildExportRow(ErpPayableExpenseRespVO expense,
                                                         ErpPayableExpenseRespVO.Item item,
                                                         boolean fillMainFields) {
        ErpPayableExpenseExportRespVO row = fillMainFields
                ? BeanUtils.toBean(expense, ErpPayableExpenseExportRespVO.class)
                : new ErpPayableExpenseExportRespVO();
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
        row.setItemExpenseCategory(item.getExpenseCategory());
        row.setItemRemark(item.getRemark());
        return row;
    }

    private void fillExtend(ErpPayableExpenseRespVO vo) {
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
        fillItemExtend(vo);
    }

    private void fillItemExtend(ErpPayableExpenseRespVO vo) {
        if (CollUtil.isEmpty(vo.getItems())) {
            return;
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(CollectionUtils.convertSet(vo.getItems(),
                ErpPayableExpenseRespVO.Item::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(CollectionUtils.convertSet(vo.getItems(),
                ErpPayableExpenseRespVO.Item::getHandlerId));
        vo.getItems().forEach(item -> {
            MapUtils.findAndThen(deptMap, item.getDeptId(), dept -> item.setDeptName(dept.getName()));
            MapUtils.findAndThen(userMap, item.getHandlerId(), user -> item.setHandlerName(user.getNickname()));
        });
    }

}
