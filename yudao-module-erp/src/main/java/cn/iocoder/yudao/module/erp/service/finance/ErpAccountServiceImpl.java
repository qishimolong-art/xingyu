package cn.iocoder.yudao.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceTransferMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableExpenseMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherIncomeMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpAccountBalanceBO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_ACCOUNT_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_DELETE_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_DELETE_FAIL_REFERENCED;

/**
 * ERP 结算账户 Service 实现类
 */
@Service
@Validated
public class ErpAccountServiceImpl implements ErpAccountService {

    private static final String FIELD_PERMISSION_MODULE = "erp_account";
    private static final Integer BANK_ACCOUNT_TYPE = 1;

    @Resource
    private ErpAccountMapper accountMapper;
    @Resource
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Resource
    private ErpFinancePaymentMapper financePaymentMapper;
    @Resource
    private ErpFinanceTransferMapper financeTransferMapper;
    @Resource
    private ErpReceivableOtherIncomeMapper receivableOtherIncomeMapper;
    @Resource
    private ErpPayableExpenseMapper payableExpenseMapper;
    @Resource
    private ErpFinancePermissionFieldFiller permissionFieldFiller;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    public Long createAccount(ErpAccountSaveReqVO createReqVO) {
        ErpAccountDO account = BeanUtils.toBean(createReqVO, ErpAccountDO.class);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, account);
        permissionFieldFiller.fillCreateFields(account);
        normalizeAccount(account);
        accountMapper.insert(account);
        operateLogService.recordCreate(ERP_ACCOUNT_TYPE, account.getId(), account.getName());
        return account.getId();
    }

    @Override
    public void updateAccount(ErpAccountSaveReqVO updateReqVO) {
        ErpAccountDO account = accountMapper.selectById(updateReqVO.getId());
        if (account == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        ErpAccountDO updateObj = BeanUtils.toBean(updateReqVO, ErpAccountDO.class);
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateObj, account);
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(account.getDeptId());
        }
        normalizeAccount(updateObj);
        accountMapper.updateById(updateObj);
        operateLogService.recordUpdate(ERP_ACCOUNT_TYPE, updateObj.getId(), updateObj.getName());
    }

    @Override
    public void updateAccountDefaultStatus(Long id, Boolean defaultStatus) {
        validateAccountExists(id);
        if (Boolean.TRUE.equals(defaultStatus)) {
            ErpAccountDO account = accountMapper.selectByDefaultStatus();
            if (account != null) {
                accountMapper.updateById(new ErpAccountDO().setId(account.getId()).setDefaultStatus(false));
            }
        }
        accountMapper.updateById(new ErpAccountDO().setId(id).setDefaultStatus(defaultStatus));
    }

    @Override
    public void deleteAccount(Long id) {
        ErpAccountDO account = validateAccountExists(id);
        baseArchiveReferenceService.validateAccountNotReferenced(id);
        accountMapper.deleteById(id);
        operateLogService.record(ERP_ACCOUNT_TYPE, ERP_DELETE_SUB_TYPE, id,
                "删除账户，账户名称：" + account.getName(), account.getName());
    }

    private ErpAccountDO validateAccountExists(Long id) {
        ErpAccountDO account = accountMapper.selectById(id);
        if (account == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        return account;
    }

    private void validateAccountNotReferenced(ErpAccountDO account) {
        Long accountId = account.getId();
        ErpFinanceReceiptDO receipt = financeReceiptMapper.selectFirstOne(ErpFinanceReceiptDO::getAccountId, accountId);
        if (receipt != null) {
            throw exception(ACCOUNT_DELETE_FAIL_REFERENCED, "收款单 " + receipt.getNo());
        }
        ErpFinancePaymentDO payment = financePaymentMapper.selectFirstOne(ErpFinancePaymentDO::getAccountId, accountId);
        if (payment != null) {
            throw exception(ACCOUNT_DELETE_FAIL_REFERENCED, "付款单 " + payment.getNo());
        }
        ErpFinanceTransferDO outTransfer = financeTransferMapper.selectFirstOne(ErpFinanceTransferDO::getOutAccountId, accountId);
        if (outTransfer != null) {
            throw exception(ACCOUNT_DELETE_FAIL_REFERENCED, "转账单 " + outTransfer.getNo());
        }
        ErpFinanceTransferDO inTransfer = financeTransferMapper.selectFirstOne(ErpFinanceTransferDO::getInAccountId, accountId);
        if (inTransfer != null) {
            throw exception(ACCOUNT_DELETE_FAIL_REFERENCED, "转账单 " + inTransfer.getNo());
        }
        ErpReceivableOtherIncomeDO income = receivableOtherIncomeMapper.selectFirstOne(
                ErpReceivableOtherIncomeDO::getAccountId, accountId);
        if (income != null) {
            throw exception(ACCOUNT_DELETE_FAIL_REFERENCED, "其他收入单 " + income.getNo());
        }
        ErpPayableExpenseDO expense = payableExpenseMapper.selectFirstOne(ErpPayableExpenseDO::getAccountId, accountId);
        if (expense != null) {
            throw exception(ACCOUNT_DELETE_FAIL_REFERENCED, "费用支付单 " + expense.getNo());
        }
    }

    @Override
    public ErpAccountDO getAccount(Long id) {
        return accountMapper.selectById(id);
    }

    @Override
    public ErpAccountDO validateAccount(Long id) {
        ErpAccountDO account = accountMapper.selectById(id);
        if (account == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(account.getStatus())) {
            throw exception(ACCOUNT_NOT_ENABLE, account.getName());
        }
        return account;
    }

    @Override
    public List<ErpAccountDO> getAccountListByStatus(Integer status) {
        return accountMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpAccountDO> getAccountList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return accountMapper.selectByIds(ids);
    }

    @Override
    public List<ErpAccountBalanceBO> getAccountBalanceList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ErpAccountBalanceBO> balances = accountMapper.selectAccountBalanceList(ids);
        ids.forEach(id -> {
            if (balances.stream().noneMatch(item -> id.equals(item.getAccountId()))) {
                ErpAccountBalanceBO balance = new ErpAccountBalanceBO();
                balance.setAccountId(id);
                balance.setCurrentBalance(BigDecimal.ZERO);
                balances.add(balance);
            }
        });
        return balances;
    }

    @Override
    public PageResult<ErpAccountDO> getAccountPage(ErpAccountPageReqVO pageReqVO) {
        return accountMapper.selectPage(pageReqVO);
    }

    private void normalizeAccount(ErpAccountDO account) {
        account.setName(StrUtil.trim(account.getName()));
        account.setNo(StrUtil.emptyToNull(StrUtil.trim(account.getNo())));
        account.setRemark(StrUtil.emptyToNull(StrUtil.trim(account.getRemark())));
        if (BANK_ACCOUNT_TYPE.equals(account.getAccountType())) {
            account.setBankName(StrUtil.emptyToNull(StrUtil.trim(account.getBankName())));
            account.setBankAccount(StrUtil.emptyToNull(StrUtil.trim(account.getBankAccount())));
            return;
        }
        account.setBankName(null);
        account.setBankAccount(null);
    }

}
