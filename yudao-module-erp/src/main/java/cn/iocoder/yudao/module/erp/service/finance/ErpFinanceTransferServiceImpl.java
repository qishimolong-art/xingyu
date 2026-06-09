package cn.iocoder.yudao.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceTransferMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_ACCOUNTS_SAME;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_UPDATE_FAIL_APPROVE;
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

    @Resource
    private ErpFinanceTransferMapper financeTransferMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinanceTransfer(ErpFinanceTransferSaveReqVO createReqVO) {
        validateTransferAccounts(createReqVO.getOutAccountId(), createReqVO.getInAccountId());
        validateFinanceUser(createReqVO.getFinanceUserId());
        String no = noRedisDAO.generateMonthSequence(ErpNoRedisDAO.FINANCE_TRANSFER_NO_PREFIX);
        if (financeTransferMapper.selectByNo(no) != null) {
            throw exception(FINANCE_TRANSFER_NO_EXISTS);
        }
        ErpFinanceTransferDO transfer = BeanUtils.toBean(createReqVO, ErpFinanceTransferDO.class, in -> in
                .setNo(no)
                .setStatus(ErpAuditStatus.PROCESS.getStatus()));
        fillCreateDeptId(transfer);
        financeTransferMapper.insert(transfer);
        recordCreate(transfer);
        return transfer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinanceTransfer(ErpFinanceTransferSaveReqVO updateReqVO) {
        ErpFinanceTransferDO transfer = validateFinanceTransferExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(transfer.getStatus())) {
            throw exception(FINANCE_TRANSFER_UPDATE_FAIL_APPROVE, transfer.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, transfer);
        validateTransferAccounts(updateReqVO.getOutAccountId(), updateReqVO.getInAccountId());
        validateFinanceUser(updateReqVO.getFinanceUserId());
        ErpFinanceTransferDO updateObj = BeanUtils.toBean(updateReqVO, ErpFinanceTransferDO.class);
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(transfer.getDeptId());
        }
        financeTransferMapper.updateById(updateObj);
        recordUpdate(transfer, updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinanceTransferStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        if (!approve) {
            throw exception(FINANCE_TRANSFER_APPROVE_FAIL);
        }
        ErpFinanceTransferDO transfer = validateFinanceTransferExists(id);
        if (transfer.getStatus().equals(status)) {
            throw exception(FINANCE_TRANSFER_APPROVE_FAIL);
        }
        validateTransferAccounts(transfer.getOutAccountId(), transfer.getInAccountId());
        int updateCount = financeTransferMapper.updateByIdAndStatus(id, transfer.getStatus(),
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

    private void fillCreateDeptId(ErpFinanceTransferDO transfer) {
        if (transfer.getDeptId() != null) {
            return;
        }
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return;
        }
        AdminUserRespDTO user = adminUserApi.getUser(loginUserId);
        transfer.setDeptId(user == null ? null : user.getDeptId());
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
