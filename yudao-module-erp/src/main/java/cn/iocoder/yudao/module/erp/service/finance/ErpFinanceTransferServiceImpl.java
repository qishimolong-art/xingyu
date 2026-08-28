package cn.iocoder.yudao.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceTransferMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceTransferStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_ACCOUNTS_SAME;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_UPDATE_FAIL_STATUS_CHANGED;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_DELETE_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_FINANCE_TRANSFER_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_UPDATE_SUB_TYPE;

/**
 * ERP 银行转账单 Service 实现类
 */
@Service
@Validated
public class ErpFinanceTransferServiceImpl implements ErpFinanceTransferService {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_transfer";
    private static final BigDecimal DEFAULT_EXCHANGE_RATE = BigDecimal.ONE;
    private static final BigDecimal DEFAULT_FEE_PRICE = BigDecimal.ZERO;

    @Resource
    private ErpFinanceTransferMapper financeTransferMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpFinancePermissionFieldFiller permissionFieldFiller;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinanceTransfer(ErpFinanceTransferSaveReqVO createReqVO) {
        ErpFinanceTransferDO transfer = BeanUtils.toBean(createReqVO, ErpFinanceTransferDO.class);
        permissionFieldFiller.fillCreateFields(transfer);
        validateFinanceTransferForSubmit(transfer);
        String no = noRedisDAO.generateMonthSequence(ErpNoRedisDAO.FINANCE_TRANSFER_NO_PREFIX);
        if (financeTransferMapper.selectByNo(no) != null) {
            throw exception(FINANCE_TRANSFER_NO_EXISTS);
        }
        transfer.setId(null);
        transfer.setNo(no);
        transfer.setStatus(ErpFinanceTransferStatusEnum.PROCESS.getStatus());
        financeTransferMapper.insert(transfer);
        recordCreate(transfer);
        return transfer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinanceTransferDraft(ErpFinanceTransferDraftSaveReqVO createReqVO) {
        validateDraftReferences(createReqVO.getOutAccountId(), createReqVO.getInAccountId(),
                createReqVO.getFinanceUserId());
        String no = noRedisDAO.generateMonthSequence(ErpNoRedisDAO.FINANCE_TRANSFER_NO_PREFIX);
        if (financeTransferMapper.selectByNo(no) != null) {
            throw exception(FINANCE_TRANSFER_NO_EXISTS);
        }
        ErpFinanceTransferDO transfer = BeanUtils.toBean(createReqVO, ErpFinanceTransferDO.class, in -> in
                .setId(null)
                .setNo(no)
                .setStatus(ErpFinanceTransferStatusEnum.DRAFT.getStatus()));
        permissionFieldFiller.fillCreateFields(transfer);
        financeTransferMapper.insert(transfer);
        recordCreate(transfer);
        return transfer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinanceTransferAndSubmit(ErpFinanceTransferSaveReqVO createReqVO) {
        return createFinanceTransfer(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinanceTransfer(ErpFinanceTransferSaveReqVO updateReqVO) {
        ErpFinanceTransferDO transfer = validateFinanceTransferExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(transfer.getStatus())) {
            throw exception(FINANCE_TRANSFER_UPDATE_FAIL_APPROVE, transfer.getNo());
        }
        if (!ErpFinanceTransferStatusEnum.PROCESS.getStatus().equals(transfer.getStatus())) {
            throw exception(FINANCE_TRANSFER_UPDATE_FAIL_STATUS_CHANGED, transfer.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, transfer);
        ErpFinanceTransferDO updateObj = BeanUtils.toBean(updateReqVO, ErpFinanceTransferDO.class);
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(transfer.getDeptId());
        }
        validateFinanceTransferForSubmit(updateObj);
        if (financeTransferMapper.updateByIdAndStatus(updateReqVO.getId(),
                ErpFinanceTransferStatusEnum.PROCESS.getStatus(), updateObj) == 0) {
            throw exception(FINANCE_TRANSFER_UPDATE_FAIL_STATUS_CHANGED, transfer.getNo());
        }
        recordUpdate(transfer, updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinanceTransferDraft(ErpFinanceTransferDraftSaveReqVO updateReqVO) {
        ErpFinanceTransferDO transfer = validateFinanceTransferExists(updateReqVO.getId());
        if (!ErpFinanceTransferStatusEnum.DRAFT.getStatus().equals(transfer.getStatus())) {
            throw exception(FINANCE_TRANSFER_DRAFT_UPDATE_FAIL, transfer.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, transfer);
        validateDraftReferences(updateReqVO.getOutAccountId(), updateReqVO.getInAccountId(),
                updateReqVO.getFinanceUserId());
        ErpFinanceTransferDO updateObj = BeanUtils.toBean(updateReqVO, ErpFinanceTransferDO.class);
        updateObj.setId(transfer.getId());
        updateObj.setNo(transfer.getNo());
        updateObj.setStatus(ErpFinanceTransferStatusEnum.DRAFT.getStatus());
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(transfer.getDeptId());
        }
        if (financeTransferMapper.updateByIdAndStatus(transfer.getId(),
                ErpFinanceTransferStatusEnum.DRAFT.getStatus(), updateObj) == 0) {
            throw exception(FINANCE_TRANSFER_DRAFT_UPDATE_FAIL, transfer.getNo());
        }
        recordUpdate(transfer, updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinanceTransferDraftAndSubmit(ErpFinanceTransferDraftSaveReqVO updateReqVO) {
        updateFinanceTransferDraft(updateReqVO);
        submitFinanceTransfer(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFinanceTransfer(Long id) {
        ErpFinanceTransferDO transfer = financeTransferMapper.selectByIdForUpdate(id);
        if (transfer == null) {
            throw exception(FINANCE_TRANSFER_NOT_EXISTS);
        }
        if (!ErpFinanceTransferStatusEnum.DRAFT.getStatus().equals(transfer.getStatus())) {
            throw exception(FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, "单据不是草稿或状态已变化");
        }
        validateFinanceTransferForSubmit(transfer);
        if (financeTransferMapper.updateByIdAndStatus(id,
                ErpFinanceTransferStatusEnum.DRAFT.getStatus(),
                new ErpFinanceTransferDO().setStatus(ErpFinanceTransferStatusEnum.PROCESS.getStatus())) == 0) {
            throw exception(FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, "状态已变化，请刷新后重试");
        }
        operateLogService.recordStatus(ERP_FINANCE_TRANSFER_TYPE, id, transfer.getNo(), true);
    }

    @Override
    public void updateFinanceTransferRemark(ErpFinanceUpdateRemarkReqVO updateReqVO) {
        ErpFinanceTransferDO transfer = validateFinanceTransferExists(updateReqVO.getId());
        financeTransferMapper.updateById(new ErpFinanceTransferDO()
                .setId(updateReqVO.getId()).setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_FINANCE_TRANSFER_TYPE, transfer.getId(), transfer.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinanceTransferStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        if (!approve) {
            throw exception(FINANCE_TRANSFER_APPROVE_FAIL);
        }
        ErpFinanceTransferDO transfer = validateFinanceTransferExists(id);
        if (!ErpFinanceTransferStatusEnum.PROCESS.getStatus().equals(transfer.getStatus())) {
            throw exception(FINANCE_TRANSFER_APPROVE_FAIL);
        }
        validateTransferAccounts(transfer.getOutAccountId(), transfer.getInAccountId());
        int updateCount = financeTransferMapper.updateByIdAndStatus(id,
                ErpFinanceTransferStatusEnum.PROCESS.getStatus(),
                new ErpFinanceTransferDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(FINANCE_TRANSFER_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_FINANCE_TRANSFER_TYPE, id, transfer.getNo(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFinanceTransfer(List<Long> ids) {
        List<ErpFinanceTransferDO> transfers = financeTransferMapper.selectByIds(ids);
        if (CollUtil.isEmpty(transfers)) {
            return;
        }
        transfers.forEach(transfer -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(transfer.getStatus())) {
                throw exception(FINANCE_TRANSFER_DELETE_FAIL_APPROVE, transfer.getNo());
            }
        });
        transfers.forEach(transfer -> {
            financeTransferMapper.deleteById(transfer.getId());
            operateLogService.record(ERP_FINANCE_TRANSFER_TYPE, ERP_DELETE_SUB_TYPE, transfer.getId(),
                    buildAction("删除银行转账单", transfer), transfer.getNo());
        });
    }

    @Override
    public ErpFinanceTransferDO getFinanceTransfer(Long id) {
        return financeTransferMapper.selectById(id);
    }

    @Override
    public List<ErpFinanceTransferDO> getFinanceTransferList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return financeTransferMapper.selectByIds(ids);
    }

    @Override
    public PageResult<ErpFinanceTransferDO> getFinanceTransferPage(ErpFinanceTransferPageReqVO pageReqVO) {
        return financeTransferMapper.selectPage(pageReqVO);
    }

    private ErpFinanceTransferDO validateFinanceTransferExists(Long id) {
        ErpFinanceTransferDO transfer = financeTransferMapper.selectById(id);
        if (transfer == null) {
            throw exception(FINANCE_TRANSFER_NOT_EXISTS);
        }
        return transfer;
    }

    private void validateTransferAccounts(Long outAccountId, Long inAccountId) {
        if (ObjectUtil.equal(outAccountId, inAccountId)) {
            throw exception(FINANCE_TRANSFER_ACCOUNTS_SAME);
        }
        accountService.validateAccount(outAccountId);
        accountService.validateAccount(inAccountId);
    }

    private void validateFinanceUser(Long financeUserId) {
        if (financeUserId != null) {
            adminUserApi.validateUser(financeUserId);
        }
    }

    private void validateDraftReferences(Long outAccountId, Long inAccountId, Long financeUserId) {
        if (outAccountId != null && inAccountId != null && ObjectUtil.equal(outAccountId, inAccountId)) {
            throw exception(FINANCE_TRANSFER_ACCOUNTS_SAME);
        }
        if (outAccountId != null) {
            accountService.validateAccount(outAccountId);
        }
        if (inAccountId != null) {
            accountService.validateAccount(inAccountId);
        }
        validateFinanceUser(financeUserId);
    }

    private void normalizeFinanceTransfer(ErpFinanceTransferDO transfer) {
        if (transfer.getExchangeRate() == null) {
            transfer.setExchangeRate(DEFAULT_EXCHANGE_RATE);
        }
        if (transfer.getFeePrice() == null) {
            transfer.setFeePrice(DEFAULT_FEE_PRICE);
        }
    }

    private void validateFinanceTransferForSubmit(ErpFinanceTransferDO transfer) {
        normalizeFinanceTransfer(transfer);
        if (transfer.getTransferTime() == null) {
            throw exception(FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, "转账时间不能为空");
        }
        if (transfer.getOutAccountId() == null) {
            throw exception(FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, "转出账户不能为空");
        }
        if (transfer.getInAccountId() == null) {
            throw exception(FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, "转入账户不能为空");
        }
        if (transfer.getTransferPrice() == null) {
            throw exception(FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, "转账金额不能为空");
        }
        if (transfer.getTransferPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, "转账金额必须大于 0");
        }
        if (transfer.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, "汇率必须大于 0");
        }
        if (transfer.getFeePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw exception(FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, "手续费不能小于 0");
        }
        if (transfer.getFinanceUserId() == null) {
            throw exception(FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, "经手人不能为空");
        }
        if (transfer.getDeptId() == null) {
            throw exception(FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, "所属部门不能为空");
        }
        validateTransferAccounts(transfer.getOutAccountId(), transfer.getInAccountId());
        validateFinanceUser(transfer.getFinanceUserId());
    }

    private void recordCreate(ErpFinanceTransferDO transfer) {
        operateLogService.record(ERP_FINANCE_TRANSFER_TYPE, cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_CREATE_SUB_TYPE,
                transfer.getId(), buildAction("创建银行转账单", transfer), transfer.getNo());
    }

    private void recordUpdate(ErpFinanceTransferDO oldTransfer, ErpFinanceTransferDO newTransfer) {
        operateLogService.record(ERP_FINANCE_TRANSFER_TYPE, ERP_UPDATE_SUB_TYPE, newTransfer.getId(),
                buildAction("更新银行转账单", oldTransfer)
                        + "，转账金额：" + oldTransfer.getTransferPrice() + " -> " + newTransfer.getTransferPrice(),
                oldTransfer.getNo());
    }

    private String buildAction(String operation, ErpFinanceTransferDO transfer) {
        return operation + "，单据编号：" + transfer.getNo()
                + "，类型：银行转账"
                + "，金额：" + transfer.getTransferPrice()
                + "，财务人员：" + getUserName(transfer.getFinanceUserId());
    }

    private String getUserName(Long userId) {
        if (userId == null) {
            return "";
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user == null ? String.valueOf(userId) : user.getNickname();
    }

}
