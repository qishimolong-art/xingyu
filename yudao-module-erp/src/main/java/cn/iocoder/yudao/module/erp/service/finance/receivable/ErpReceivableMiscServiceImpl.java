package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableMiscStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_MISC_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_MISC_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_MISC_DRAFT_SAVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_MISC_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_MISC_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_MISC_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_MISC_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_MISC_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_MISC_SAVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_MISC_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_MISC_UPDATE_FAIL_STATUS_CHANGED;

@Service
@Validated
public class ErpReceivableMiscServiceImpl implements ErpReceivableMiscService {

    private static final String NO_PREFIX = "QTYSM";
    private static final String FIELD_PERMISSION_MODULE = "erp_finance_receivable_misc";
    private static final String LOG_TYPE = "其他应收";

    @Resource
    private ErpReceivableMiscMapper receivableMiscMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpCustomerService customerService;
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
    public Long createReceivableMisc(ErpReceivableMiscSaveReqVO createReqVO) {
        validateAmountNonZero(createReqVO.getAmount(), false);
        customerService.validateCustomer(createReqVO.getCustomerId());
        validateSubmitRefs(createReqVO.getAccountId(), createReqVO.getDeptId(), createReqVO.getHandlerId(), false);
        ErpReceivableMiscDO doObj = BeanUtils.toBean(createReqVO, ErpReceivableMiscDO.class)
                .setNo(generateNo())
                .setStatus(ErpReceivableMiscStatusEnum.PROCESS.getStatus())
                .setBizTime(LocalDateTime.now());
        fillCreateDeptId(doObj);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, doObj);
        receivableMiscMapper.insert(doObj);
        operateLogService.recordCreate(LOG_TYPE, doObj.getId(), doObj.getNo());
        return doObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReceivableMiscDraft(ErpReceivableMiscDraftSaveReqVO createReqVO) {
        validateDraftForSave(createReqVO);
        ErpReceivableMiscDO doObj = BeanUtils.toBean(createReqVO, ErpReceivableMiscDO.class)
                .setId(null)
                .setNo(generateNo())
                .setStatus(ErpReceivableMiscStatusEnum.DRAFT.getStatus())
                .setBizTime(LocalDateTime.now());
        fillCreateDeptId(doObj);
        validateAccountIfPresent(doObj.getAccountId());
        validateRefs(doObj.getHandlerId(), doObj.getDeptId());
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, doObj);
        receivableMiscMapper.insert(doObj);
        operateLogService.recordCreate(LOG_TYPE, doObj.getId(), doObj.getNo());
        return doObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAndSubmitReceivableMisc(ErpReceivableMiscSaveReqVO createReqVO) {
        return createReceivableMisc(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceivableMisc(ErpReceivableMiscSaveReqVO updateReqVO) {
        ErpReceivableMiscDO db = validateReceivableMiscExists(updateReqVO.getId());
        if (ErpReceivableMiscStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(RECEIVABLE_MISC_DRAFT_UPDATE_FAIL, db.getNo());
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(RECEIVABLE_MISC_UPDATE_FAIL_APPROVE, db.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        validateAmountNonZero(updateReqVO.getAmount(), false);
        customerService.validateCustomer(updateReqVO.getCustomerId());
        validateSubmitRefs(updateReqVO.getAccountId(), updateReqVO.getDeptId(), updateReqVO.getHandlerId(), false);
        ErpReceivableMiscDO updateObj = BeanUtils.toBean(updateReqVO, ErpReceivableMiscDO.class)
                .setNo(db.getNo())
                .setStatus(db.getStatus())
                .setBizTime(db.getBizTime());
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(db.getDeptId());
        }
        validateAccountIfPresent(updateObj.getAccountId());
        validateRefs(updateObj.getHandlerId(), updateObj.getDeptId());
        if (receivableMiscMapper.updateByIdAndStatus(db.getId(), ErpReceivableMiscStatusEnum.PROCESS.getStatus(),
                updateObj) == 0) {
            throw exception(RECEIVABLE_MISC_UPDATE_FAIL_STATUS_CHANGED);
        }
        operateLogService.recordUpdate(LOG_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceivableMiscDraft(ErpReceivableMiscDraftSaveReqVO updateReqVO) {
        ErpReceivableMiscDO db = validateReceivableMiscExists(updateReqVO.getId());
        if (!ErpReceivableMiscStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(RECEIVABLE_MISC_DRAFT_UPDATE_FAIL, db.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        validateDraftForSave(updateReqVO);
        ErpReceivableMiscDO updateObj = BeanUtils.toBean(updateReqVO, ErpReceivableMiscDO.class)
                .setId(db.getId())
                .setNo(db.getNo())
                .setStatus(db.getStatus())
                .setBizTime(db.getBizTime());
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(db.getDeptId());
        }
        validateRefs(updateObj.getHandlerId(), updateObj.getDeptId());
        if (receivableMiscMapper.updateByIdAndStatus(db.getId(),
                ErpReceivableMiscStatusEnum.DRAFT.getStatus(), updateObj) == 0) {
            throw exception(RECEIVABLE_MISC_DRAFT_UPDATE_FAIL, db.getNo());
        }
        operateLogService.recordUpdate(LOG_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitReceivableMisc(ErpReceivableMiscSaveReqVO updateReqVO) {
        updateReceivableMiscDraft(BeanUtils.toBean(updateReqVO, ErpReceivableMiscDraftSaveReqVO.class));
        submitReceivableMisc(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReceivableMisc(Long id) {
        ErpReceivableMiscDO db = receivableMiscMapper.selectByIdForUpdate(id);
        if (db == null) {
            throw exception(RECEIVABLE_MISC_NOT_EXISTS);
        }
        if (!ErpReceivableMiscStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(RECEIVABLE_MISC_DRAFT_SUBMIT_FAIL, "当前状态不是草稿");
        }
        validateDraftForSubmit(db);
        if (receivableMiscMapper.updateByIdAndStatus(id,
                ErpReceivableMiscStatusEnum.DRAFT.getStatus(),
                new ErpReceivableMiscDO().setStatus(ErpReceivableMiscStatusEnum.PROCESS.getStatus())) == 0) {
            throw exception(RECEIVABLE_MISC_DRAFT_SUBMIT_FAIL, "状态已变化，请刷新后重试");
        }
        operateLogService.recordUpdate(LOG_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceivableMiscStatus(Long id, Integer status) {
        ErpReceivableMiscDO db = validateReceivableMiscExists(id);
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)
                || !ErpReceivableMiscStatusEnum.PROCESS.getStatus().equals(db.getStatus())) {
            throw exception(RECEIVABLE_MISC_PROCESS_FAIL);
        }
        if (receivableMiscMapper.updateByIdAndStatus(id, db.getStatus(),
                new ErpReceivableMiscDO().setStatus(status)) == 0) {
            throw exception(RECEIVABLE_MISC_APPROVE_FAIL);
        }
        operateLogService.recordStatus(LOG_TYPE, id, db.getNo(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReceivableMisc(Long id) {
        ErpReceivableMiscDO db = validateReceivableMiscExists(id);
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(RECEIVABLE_MISC_DELETE_FAIL_APPROVE, db.getNo());
        }
        receivableMiscMapper.deleteById(id);
        operateLogService.recordDelete(LOG_TYPE, db.getId(), db.getNo());
    }

    @Override
    public ErpReceivableMiscDO getReceivableMisc(Long id) {
        return receivableMiscMapper.selectById(id);
    }

    @Override
    public PageResult<ErpReceivableMiscDO> getReceivableMiscPage(ErpReceivableMiscPageReqVO pageReqVO) {
        return receivableMiscMapper.selectPage(pageReqVO);
    }

    private String generateNo() {
        String no = noRedisDAO.generate(NO_PREFIX);
        if (receivableMiscMapper.selectByNo(no) != null) {
            throw exception(RECEIVABLE_MISC_NO_EXISTS);
        }
        return no;
    }

    private ErpReceivableMiscDO validateReceivableMiscExists(Long id) {
        ErpReceivableMiscDO db = receivableMiscMapper.selectById(id);
        if (db == null) {
            throw exception(RECEIVABLE_MISC_NOT_EXISTS);
        }
        return db;
    }

    private void validateDraftForSave(ErpReceivableMiscDraftSaveReqVO reqVO) {
        if (reqVO.getCustomerId() == null) {
            throw exception(RECEIVABLE_MISC_DRAFT_SAVE_FAIL, "客户不能为空");
        }
        validateAmountNonZeroIfPresent(reqVO.getAmount(), true);
        customerService.validateCustomer(reqVO.getCustomerId());
    }

    private void validateDraftForSubmit(ErpReceivableMiscDO doObj) {
        if (doObj.getCustomerId() == null) {
            throw exception(RECEIVABLE_MISC_DRAFT_SUBMIT_FAIL, "客户不能为空");
        }
        if (doObj.getAmount() == null) {
            throw exception(RECEIVABLE_MISC_DRAFT_SUBMIT_FAIL, "金额不能为空");
        }
        validateAmountNonZero(doObj.getAmount(), true);
        customerService.validateCustomer(doObj.getCustomerId());
        validateSubmitRefs(doObj.getAccountId(), doObj.getDeptId(), doObj.getHandlerId(), true);
    }

    private void validateAmountNonZeroIfPresent(BigDecimal amount, boolean draftSubmit) {
        if (amount != null) {
            validateAmountNonZero(amount, draftSubmit);
        }
    }

    private void validateAmountNonZero(BigDecimal amount, boolean draftSubmit) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw exception(draftSubmit ? RECEIVABLE_MISC_DRAFT_SUBMIT_FAIL : RECEIVABLE_MISC_SAVE_FAIL,
                    "金额不能为 0");
        }
    }

    private void validateRefs(Long handlerId, Long deptId) {
        if (handlerId != null) {
            adminUserApi.validateUser(handlerId);
        }
        if (deptId != null && deptApi.getDept(deptId) == null) {
            throw exception(RECEIVABLE_MISC_NOT_EXISTS);
        }
    }

    private void validateAccountIfPresent(Long accountId) {
        if (accountId != null) {
            accountService.validateAccount(accountId);
        }
    }

    private void validateSubmitRefs(Long accountId, Long deptId, Long handlerId, boolean draftSubmit) {
        if (accountId == null) {
            throw exception(draftSubmit ? RECEIVABLE_MISC_DRAFT_SUBMIT_FAIL : RECEIVABLE_MISC_SAVE_FAIL,
                    "账户不能为空");
        }
        if (deptId == null) {
            throw exception(draftSubmit ? RECEIVABLE_MISC_DRAFT_SUBMIT_FAIL : RECEIVABLE_MISC_SAVE_FAIL,
                    "部门不能为空");
        }
        accountService.validateAccount(accountId);
        validateRefs(handlerId, deptId);
    }

    private void fillCreateDeptId(ErpReceivableMiscDO doObj) {
        if (doObj.getDeptId() != null) {
            return;
        }
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return;
        }
        AdminUserRespDTO user = adminUserApi.getUser(loginUserId);
        doObj.setDeptId(user == null ? null : user.getDeptId());
    }

}
