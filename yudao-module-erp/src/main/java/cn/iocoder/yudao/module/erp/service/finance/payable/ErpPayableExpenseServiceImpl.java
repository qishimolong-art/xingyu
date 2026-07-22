package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpensePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableExpenseItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableExpenseMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_UPDATE_FAIL_STATUS_CHANGED;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PAYABLE_EXPENSE_TYPE;

@Service
@Validated
public class ErpPayableExpenseServiceImpl implements ErpPayableExpenseService {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_payable_expense";

    @Resource
    private ErpPayableExpenseMapper payableExpenseMapper;
    @Resource
    private ErpPayableExpenseItemMapper payableExpenseItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayableExpense(ErpPayableExpenseSaveReqVO createReqVO) {
        fillDefaultDeptId(createReqVO);
        validateRefs(createReqVO.getAccountId(), createReqVO.getHandlerId(), createReqVO.getDeptId());
        validateItemRefs(createReqVO.getItems());
        String no = noRedisDAO.generate("FYZF");
        if (payableExpenseMapper.selectByNo(no) != null) {
            throw exception(PAYABLE_EXPENSE_NO_EXISTS);
        }
        ErpPayableExpenseDO db = BeanUtils.toBean(createReqVO, ErpPayableExpenseDO.class, o -> o
                .setNo(no)
                .setStatus(ErpAuditStatus.PROCESS.getStatus()));
        normalizeMain(db);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, db);
        db.setTotalAmount(sumAmount(createReqVO.getItems()));
        payableExpenseMapper.insert(db);
        List<ErpPayableExpenseItemDO> expenseItems = BeanUtils.toBean(createReqVO.getItems(),
                ErpPayableExpenseItemDO.class, item -> {
                    item.setId(null);
                    item.setExpenseId(db.getId());
                    normalizeItem(item);
                });
        fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, expenseItems);
        payableExpenseItemMapper.insertBatch(expenseItems);
        operateLogService.recordCreate(ERP_PAYABLE_EXPENSE_TYPE, db.getId(), db.getNo());
        return db.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableExpense(ErpPayableExpenseSaveReqVO updateReqVO) {
        ErpPayableExpenseDO db = validateExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_EXPENSE_UPDATE_FAIL_APPROVE, db.getNo());
        }
        List<ErpPayableExpenseItemDO> oldItems = payableExpenseItemMapper.selectListByExpenseId(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        if (fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "items")) {
            updateReqVO.setItems(BeanUtils.toBean(oldItems, ErpPayableExpenseSaveReqVO.Item.class));
        } else {
            fieldPermissionMasker.preserveOrClearHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldItems);
            fillDefaultItemDeptId(updateReqVO.getItems());
        }
        if (updateReqVO.getDeptId() == null) {
            updateReqVO.setDeptId(db.getDeptId());
        }
        validateRefs(updateReqVO.getAccountId(), updateReqVO.getHandlerId(), updateReqVO.getDeptId());
        validateItemRefs(updateReqVO.getItems());
        ErpPayableExpenseDO updateObj = BeanUtils.toBean(updateReqVO, ErpPayableExpenseDO.class);
        normalizeMain(updateObj);
        updateObj.setTotalAmount(sumAmount(updateReqVO.getItems()));
        if (payableExpenseMapper.updateByIdAndStatus(updateReqVO.getId(),
                ErpAuditStatus.PROCESS.getStatus(), updateObj) == 0) {
            throw exception(PAYABLE_EXPENSE_UPDATE_FAIL_STATUS_CHANGED);
        }
        payableExpenseItemMapper.deleteByIds(convertList(oldItems, ErpPayableExpenseItemDO::getId));
        payableExpenseItemMapper.insertBatch(BeanUtils.toBean(updateReqVO.getItems(),
                ErpPayableExpenseItemDO.class, item -> {
                    item.setId(null);
                    item.setExpenseId(updateReqVO.getId());
                    normalizeItem(item);
                }));
        operateLogService.recordUpdate(ERP_PAYABLE_EXPENSE_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableExpenseStatus(Long id, Integer status) {
        ErpPayableExpenseDO db = validateExists(id);
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        if (db.getStatus().equals(status)) {
            throw exception(approve ? PAYABLE_EXPENSE_APPROVE_FAIL : PAYABLE_EXPENSE_PROCESS_FAIL);
        }
        if (payableExpenseMapper.updateByIdAndStatus(id, db.getStatus(),
                ErpPayableExpenseDO.builder().status(status).build()) == 0) {
            throw exception(approve ? PAYABLE_EXPENSE_APPROVE_FAIL : PAYABLE_EXPENSE_PROCESS_FAIL);
        }
        operateLogService.recordStatus(ERP_PAYABLE_EXPENSE_TYPE, id, db.getNo(), approve);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePayableExpense(List<Long> ids) {
        List<ErpPayableExpenseDO> list = payableExpenseMapper.selectByIds(ids);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list.forEach(item -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(item.getStatus())) {
                throw exception(PAYABLE_EXPENSE_DELETE_FAIL_APPROVE, item.getNo());
            }
        });
        payableExpenseMapper.deleteByIds(ids);
        ids.forEach(id -> payableExpenseItemMapper.deleteByIds(
                convertList(payableExpenseItemMapper.selectListByExpenseId(id), ErpPayableExpenseItemDO::getId)));
        list.forEach(item -> operateLogService.recordDelete(ERP_PAYABLE_EXPENSE_TYPE, item.getId(), item.getNo()));
    }

    @Override
    public ErpPayableExpenseDO getPayableExpense(Long id) {
        return payableExpenseMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPayableExpenseDO> getPayableExpensePage(ErpPayableExpensePageReqVO pageReqVO) {
        if (hasItemFilter(pageReqVO)) {
            List<Long> expenseIds = convertList(
                    payableExpenseItemMapper.selectListByItemNameOrInvoiceNo(pageReqVO.getItemName(), pageReqVO.getInvoiceNo()),
                    ErpPayableExpenseItemDO::getExpenseId);
            if (CollUtil.isEmpty(expenseIds)) {
                return PageResult.empty();
            }
            pageReqVO.setIds(expenseIds);
        }
        return payableExpenseMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPayableExpenseItemDO> getPayableExpenseItemListByExpenseId(Long expenseId) {
        return payableExpenseItemMapper.selectListByExpenseId(expenseId);
    }

    @Override
    public List<ErpPayableExpenseItemDO> getPayableExpenseItemListByExpenseIds(Collection<Long> expenseIds) {
        if (CollUtil.isEmpty(expenseIds)) {
            return Collections.emptyList();
        }
        return payableExpenseItemMapper.selectListByExpenseIds(expenseIds);
    }

    private void validateRefs(Long accountId, Long handlerId, Long deptId) {
        if (accountId != null) {
            accountService.validateAccount(accountId);
        }
        if (handlerId != null) {
            adminUserApi.validateUser(handlerId);
        }
        if (deptId != null && deptApi.getDept(deptId) == null) {
            throw exception(PAYABLE_EXPENSE_NOT_EXISTS);
        }
    }

    private void validateItemRefs(List<ErpPayableExpenseSaveReqVO.Item> items) {
        items.forEach(item -> {
            if (item.getHandlerId() != null) {
                adminUserApi.validateUser(item.getHandlerId());
            }
            if (item.getDeptId() != null && deptApi.getDept(item.getDeptId()) == null) {
                throw exception(PAYABLE_EXPENSE_NOT_EXISTS);
            }
        });
    }

    private void fillDefaultDeptId(ErpPayableExpenseSaveReqVO reqVO) {
        Long deptId = getLoginUserDeptId();
        if (deptId == null) {
            return;
        }
        if (reqVO.getDeptId() == null) {
            reqVO.setDeptId(deptId);
        }
        fillDefaultItemDeptId(reqVO.getItems(), deptId);
    }

    private void fillDefaultItemDeptId(List<ErpPayableExpenseSaveReqVO.Item> items) {
        Long deptId = getLoginUserDeptId();
        if (deptId != null) {
            fillDefaultItemDeptId(items, deptId);
        }
    }

    private void fillDefaultItemDeptId(List<ErpPayableExpenseSaveReqVO.Item> items, Long deptId) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> {
            if (item.getDeptId() == null) {
                item.setDeptId(deptId);
            }
        });
    }

    private Long getLoginUserDeptId() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUser(loginUserId);
        return user == null ? null : user.getDeptId();
    }

    private ErpPayableExpenseDO validateExists(Long id) {
        ErpPayableExpenseDO db = payableExpenseMapper.selectById(id);
        if (db == null) {
            throw exception(PAYABLE_EXPENSE_NOT_EXISTS);
        }
        return db;
    }

    private BigDecimal sumAmount(List<ErpPayableExpenseSaveReqVO.Item> items) {
        return items.stream()
                .map(ErpPayableExpenseSaveReqVO.Item::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void normalizeMain(ErpPayableExpenseDO db) {
        db.setSettleMethod(StrUtil.blankToDefault(db.getSettleMethod(), ""));
        db.setVoucherNo(StrUtil.blankToDefault(db.getVoucherNo(), ""));
        db.setExpenseType(StrUtil.blankToDefault(db.getExpenseType(), ""));
        db.setDocType(StrUtil.blankToDefault(db.getDocType(), "正常单据"));
    }

    private void normalizeItem(ErpPayableExpenseItemDO item) {
        item.setQty(item.getQty() == null || item.getQty() <= 0 ? 1 : item.getQty());
        item.setAmount(item.getAmount() == null ? BigDecimal.ZERO : item.getAmount());
    }

    private boolean hasItemFilter(ErpPayableExpensePageReqVO pageReqVO) {
        return StringUtils.hasText(pageReqVO.getItemName()) || StringUtils.hasText(pageReqVO.getInvoiceNo());
    }

}
