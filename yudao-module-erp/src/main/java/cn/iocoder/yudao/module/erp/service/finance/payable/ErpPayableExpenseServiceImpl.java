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
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_UPDATE_FAIL_STATUS_CHANGED;

@Service
@Validated
public class ErpPayableExpenseServiceImpl implements ErpPayableExpenseService {

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayableExpense(ErpPayableExpenseSaveReqVO createReqVO) {
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
        db.setTotalAmount(sumAmount(createReqVO.getItems()));
        payableExpenseMapper.insert(db);
        payableExpenseItemMapper.insertBatch(BeanUtils.toBean(createReqVO.getItems(),
                ErpPayableExpenseItemDO.class, item -> {
                    item.setExpenseId(db.getId());
                    normalizeItem(item);
                }));
        return db.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableExpense(ErpPayableExpenseSaveReqVO updateReqVO) {
        ErpPayableExpenseDO db = validateExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_EXPENSE_UPDATE_FAIL_APPROVE, db.getNo());
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
        List<ErpPayableExpenseItemDO> oldItems = payableExpenseItemMapper.selectListByExpenseId(updateReqVO.getId());
        payableExpenseItemMapper.deleteByIds(convertList(oldItems, ErpPayableExpenseItemDO::getId));
        payableExpenseItemMapper.insertBatch(BeanUtils.toBean(updateReqVO.getItems(),
                ErpPayableExpenseItemDO.class, item -> {
                    item.setExpenseId(updateReqVO.getId());
                    normalizeItem(item);
                }));
    }

    @Override
    public void updatePayableExpenseStatus(Long id, Integer status) {
        ErpPayableExpenseDO db = validateExists(id);
        if (db.getStatus().equals(status)) {
            throw exception(ErpAuditStatus.APPROVE.getStatus().equals(status)
                    ? PAYABLE_EXPENSE_APPROVE_FAIL : PAYABLE_EXPENSE_PROCESS_FAIL);
        }
        if (payableExpenseMapper.updateByIdAndStatus(id, db.getStatus(),
                ErpPayableExpenseDO.builder().status(status).build()) == 0) {
            throw exception(ErpAuditStatus.APPROVE.getStatus().equals(status)
                    ? PAYABLE_EXPENSE_APPROVE_FAIL : PAYABLE_EXPENSE_PROCESS_FAIL);
        }
    }

    @Override
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
    }

    @Override
    public ErpPayableExpenseDO getPayableExpense(Long id) {
        return payableExpenseMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPayableExpenseDO> getPayableExpensePage(ErpPayableExpensePageReqVO pageReqVO) {
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
        if (deptId != null) {
            deptApi.getDept(deptId);
        }
    }

    private void validateItemRefs(List<ErpPayableExpenseSaveReqVO.Item> items) {
        items.forEach(item -> {
            if (item.getHandlerId() != null) {
                adminUserApi.validateUser(item.getHandlerId());
            }
            if (item.getDeptId() != null) {
                deptApi.getDept(item.getDeptId());
            }
        });
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

}
