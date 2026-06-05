package cn.iocoder.yudao.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpAccountMapper;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpAccountBalanceBO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_NOT_EXISTS;

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
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @Override
    public Long createAccount(ErpAccountSaveReqVO createReqVO) {
        ErpAccountDO account = BeanUtils.toBean(createReqVO, ErpAccountDO.class);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, account);
        normalizeAccount(account);
        accountMapper.insert(account);
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
        normalizeAccount(updateObj);
        accountMapper.updateById(updateObj);
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
        validateAccountExists(id);
        accountMapper.deleteById(id);
    }

    private void validateAccountExists(Long id) {
        if (accountMapper.selectById(id) == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
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
