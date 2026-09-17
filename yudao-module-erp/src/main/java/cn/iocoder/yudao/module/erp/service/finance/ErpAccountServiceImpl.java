package cn.iocoder.yudao.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountDraftSaveReqVO;
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
import cn.iocoder.yudao.module.erp.enums.finance.ErpAccountDocumentStatusEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAccountingSubjectService;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpAccountBalanceBO;
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
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_ACCOUNT_TYPE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_NOT_SUBMITTED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_DELETE_FAIL_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_FORMAL_UPDATE_FAIL_DRAFT;

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
    @Resource
    private ErpAccountingSubjectService accountingSubjectService;
    @Resource
    private cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleStore voucherRuleStore;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAccount(ErpAccountSaveReqVO createReqVO) {
        ValidationUtils.validate(createReqVO);
        ErpAccountDO account = BeanUtils.toBean(createReqVO, ErpAccountDO.class);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, account);
        permissionFieldFiller.fillCreateFields(account);
        applyAccountDefaults(account);
        account.setDocumentStatus(ErpAccountDocumentStatusEnum.SUBMITTED.getStatus());
        normalizeAccount(account);
        clearOtherDefaultAccountIfNeeded(account);
        accountMapper.insert(account);
        ensureFundAccountSubject(account);
        operateLogService.recordCreate(ERP_ACCOUNT_TYPE, account.getId(), account, account.getNo());
        return account.getId();
    }

    @Override
    public Long createAccountDraft(ErpAccountDraftSaveReqVO createReqVO) {
        ErpAccountDO account = BeanUtils.toBean(createReqVO, ErpAccountDO.class);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, account);
        permissionFieldFiller.fillCreateFields(account);
        applyAccountDefaults(account);
        account.setDocumentStatus(ErpAccountDocumentStatusEnum.DRAFT.getStatus());
        normalizeAccount(account);
        accountMapper.insert(account);
        operateLogService.recordCreate(ERP_ACCOUNT_TYPE, account.getId(), account, account.getNo());
        return account.getId();
    }

    @Override
    public Long createAndSubmitAccount(ErpAccountSaveReqVO createReqVO) {
        return createAccount(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccount(ErpAccountSaveReqVO updateReqVO) {
        ValidationUtils.validate(updateReqVO);
        ErpAccountDO account = accountMapper.selectById(updateReqVO.getId());
        if (account == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        if (ErpAccountDocumentStatusEnum.DRAFT.getStatus().equals(account.getDocumentStatus())) {
            throw exception(ACCOUNT_FORMAL_UPDATE_FAIL_DRAFT);
        }
        ErpAccountDO updateObj = BeanUtils.toBean(updateReqVO, ErpAccountDO.class);
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateObj, account);
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(account.getDeptId());
        }
        if (updateObj.getDefaultStatus() == null) {
            updateObj.setDefaultStatus(account.getDefaultStatus());
        }
        applyAccountDefaults(updateObj);
        normalizeAccount(updateObj);
        clearOtherDefaultAccountIfNeeded(updateObj);
        accountMapper.updateById(updateObj);
        ensureFundAccountSubject(updateObj);
        operateLogService.recordUpdate(ERP_ACCOUNT_TYPE, updateObj.getId(), account,
                accountMapper.selectById(updateObj.getId()), account.getNo());
    }

    @Override
    public void updateAccountDraft(ErpAccountDraftSaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        ErpAccountDO account = accountMapper.selectById(updateReqVO.getId());
        if (account == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        if (!ErpAccountDocumentStatusEnum.DRAFT.getStatus().equals(account.getDocumentStatus())) {
            throw exception(ACCOUNT_DRAFT_UPDATE_FAIL, "当前账户不是草稿");
        }
        ErpAccountDO updateObj = BeanUtils.toBean(updateReqVO, ErpAccountDO.class);
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateObj, account);
        if (updateObj.getAccountType() == null) {
            updateObj.setAccountType(account.getAccountType());
        }
        if (updateObj.getStatus() == null) {
            updateObj.setStatus(account.getStatus());
        }
        if (updateObj.getSort() == null) {
            updateObj.setSort(account.getSort());
        }
        if (updateObj.getDefaultStatus() == null) {
            updateObj.setDefaultStatus(account.getDefaultStatus());
        }
        applyAccountDefaults(updateObj);
        normalizeAccount(updateObj);
        if (accountMapper.updateDraftByIdAndDocumentStatus(updateReqVO.getId(),
                ErpAccountDocumentStatusEnum.DRAFT.getStatus(), updateObj) == 0) {
            throw exception(ACCOUNT_DRAFT_UPDATE_FAIL, "状态已变化，请刷新后重试");
        }
        operateLogService.recordUpdate(ERP_ACCOUNT_TYPE, updateReqVO.getId(), account,
                accountMapper.selectById(updateReqVO.getId()), account.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitAccountDraft(ErpAccountSaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        ErpAccountDO account = validateAccountExists(updateReqVO.getId());
        if (!ErpAccountDocumentStatusEnum.DRAFT.getStatus().equals(account.getDocumentStatus())) {
            throw exception(ACCOUNT_DRAFT_UPDATE_FAIL, "当前账户不是草稿");
        }
        ValidationUtils.validate(updateReqVO);
        updateAccountDraft(BeanUtils.toBean(updateReqVO, ErpAccountDraftSaveReqVO.class));
        submitAccountDraft(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAccountDraft(Long id) {
        ErpAccountDO account = validateAccountExists(id);
        if (!ErpAccountDocumentStatusEnum.DRAFT.getStatus().equals(account.getDocumentStatus())) {
            throw exception(ACCOUNT_DRAFT_SUBMIT_FAIL, "当前账户不是草稿");
        }
        if (StrUtil.isBlank(account.getName())) {
            throw exception(ACCOUNT_DRAFT_SUBMIT_FAIL, "账户名称不能为空");
        }
        ValidationUtils.validate(BeanUtils.toBean(account, ErpAccountSaveReqVO.class));
        clearOtherDefaultAccountIfNeeded(account);
        ensureFundAccountSubject(account);
        if (accountMapper.updateByIdAndDocumentStatus(id,
                ErpAccountDocumentStatusEnum.DRAFT.getStatus(),
                new ErpAccountDO().setDocumentStatus(
                        ErpAccountDocumentStatusEnum.SUBMITTED.getStatus())) == 0) {
            throw exception(ACCOUNT_DRAFT_SUBMIT_FAIL, "状态已变化，请刷新后重试");
        }
        operateLogService.recordUpdate(ERP_ACCOUNT_TYPE, id, account,
                accountMapper.selectById(id), account.getNo());
    }

    @Override
    public void updateAccountDefaultStatus(Long id, Boolean defaultStatus) {
        ErpAccountDO oldAccount = validateAccountExists(id);
        if (!ErpAccountDocumentStatusEnum.SUBMITTED.getStatus().equals(oldAccount.getDocumentStatus())) {
            throw exception(ACCOUNT_NOT_SUBMITTED, oldAccount.getName());
        }
        if (Boolean.TRUE.equals(defaultStatus)) {
            clearOtherDefaultAccount(id);
        }
        accountMapper.updateById(new ErpAccountDO().setId(id).setDefaultStatus(defaultStatus));
        operateLogService.recordUpdate(ERP_ACCOUNT_TYPE, id, oldAccount, accountMapper.selectById(id), oldAccount.getNo());
    }

    @Override
    public void deleteAccount(Long id) {
        ErpAccountDO account = validateAccountExists(id);
        baseArchiveReferenceService.validateAccountNotReferenced(id);
        accountMapper.deleteById(id);
        operateLogService.recordDelete(ERP_ACCOUNT_TYPE, id, account, account.getNo());
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
        if (!ErpAccountDocumentStatusEnum.SUBMITTED.getStatus().equals(account.getDocumentStatus())) {
            throw exception(ACCOUNT_NOT_SUBMITTED, account.getName());
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

    private void ensureFundAccountSubject(ErpAccountDO account) {
        if (account == null) {
            return;
        }
        Long subjectId = accountingSubjectService.ensureFundAccountSubject(account.getAccountType(), account.getName());
        if (subjectId != null && account.getId() != null) voucherRuleStore.bindNewFundAccount(account.getId(), subjectId);
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

    private void applyAccountDefaults(ErpAccountDO account) {
        if (account.getAccountType() == null) {
            account.setAccountType(BANK_ACCOUNT_TYPE);
        }
        if (account.getStatus() == null) {
            account.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        if (account.getSort() == null) {
            account.setSort(0);
        }
        if (account.getDefaultStatus() == null) {
            account.setDefaultStatus(false);
        }
    }

    private void clearOtherDefaultAccountIfNeeded(ErpAccountDO account) {
        if (Boolean.TRUE.equals(account.getDefaultStatus())) {
            clearOtherDefaultAccount(account.getId());
        }
    }

    private void clearOtherDefaultAccount(Long excludeId) {
        ErpAccountDO currentDefault = accountMapper.selectByDefaultStatus();
        if (currentDefault == null || Objects.equals(currentDefault.getId(), excludeId)) {
            return;
        }
        accountMapper.updateById(new ErpAccountDO().setId(currentDefault.getId()).setDefaultStatus(false));
        operateLogService.recordUpdate(ERP_ACCOUNT_TYPE, currentDefault.getId(), currentDefault,
                accountMapper.selectById(currentDefault.getId()), currentDefault.getNo());
    }

}
