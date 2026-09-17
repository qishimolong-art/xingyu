package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPayableMiscStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_MISC_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_MISC_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_MISC_DRAFT_SAVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_MISC_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_MISC_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_MISC_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_MISC_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_MISC_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_MISC_SAVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_MISC_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_MISC_UPDATE_FAIL_STATUS_CHANGED;

@Service
@Validated
public class ErpPayableMiscServiceImpl implements ErpPayableMiscService {

    private static final String NO_PREFIX = "QTYFM";
    private static final String FIELD_PERMISSION_MODULE = "erp_finance_payable_misc";
    private static final String LOG_TYPE = "其他应付";

    @Resource
    private ErpPayableMiscMapper payableMiscMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpSupplierService supplierService;
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
    public Long createPayableMisc(ErpPayableMiscSaveReqVO createReqVO) {
        validateAmountNonZero(createReqVO.getAmount(), false);
        supplierService.validateSupplier(createReqVO.getSupplierId());
        validateSubmitRefs(createReqVO.getAccountId(), createReqVO.getDeptId(), createReqVO.getHandlerId(), false);
        ErpPayableMiscDO doObj = BeanUtils.toBean(createReqVO, ErpPayableMiscDO.class)
                .setNo(generateNo())
                .setStatus(ErpPayableMiscStatusEnum.PROCESS.getStatus())
                .setBizTime(LocalDateTime.now());
        fillCreateDeptId(doObj);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, doObj);
        payableMiscMapper.insert(doObj);
        operateLogService.recordCreate(LOG_TYPE, doObj.getId(), doObj.getNo());
        return doObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayableMiscDraft(ErpPayableMiscDraftSaveReqVO createReqVO) {
        validateDraftForSave(createReqVO);
        ErpPayableMiscDO doObj = BeanUtils.toBean(createReqVO, ErpPayableMiscDO.class)
                .setId(null)
                .setNo(generateNo())
                .setStatus(ErpPayableMiscStatusEnum.DRAFT.getStatus())
                .setBizTime(LocalDateTime.now());
        fillCreateDeptId(doObj);
        validateAccountIfPresent(doObj.getAccountId());
        validateRefs(doObj.getHandlerId(), doObj.getDeptId());
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, doObj);
        payableMiscMapper.insert(doObj);
        operateLogService.recordCreate(LOG_TYPE, doObj.getId(), doObj.getNo());
        return doObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAndSubmitPayableMisc(ErpPayableMiscSaveReqVO createReqVO) {
        return createPayableMisc(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableMisc(ErpPayableMiscSaveReqVO updateReqVO) {
        ErpPayableMiscDO db = validatePayableMiscExists(updateReqVO.getId());
        if (ErpPayableMiscStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_MISC_DRAFT_UPDATE_FAIL, db.getNo());
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_MISC_UPDATE_FAIL_APPROVE, db.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        validateAmountNonZero(updateReqVO.getAmount(), false);
        supplierService.validateSupplier(updateReqVO.getSupplierId());
        validateSubmitRefs(updateReqVO.getAccountId(), updateReqVO.getDeptId(), updateReqVO.getHandlerId(), false);
        ErpPayableMiscDO updateObj = BeanUtils.toBean(updateReqVO, ErpPayableMiscDO.class)
                .setNo(db.getNo())
                .setStatus(db.getStatus())
                .setBizTime(db.getBizTime());
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(db.getDeptId());
        }
        validateAccountIfPresent(updateObj.getAccountId());
        validateRefs(updateObj.getHandlerId(), updateObj.getDeptId());
        if (payableMiscMapper.updateByIdAndStatus(db.getId(), ErpPayableMiscStatusEnum.PROCESS.getStatus(),
                updateObj) == 0) {
            throw exception(PAYABLE_MISC_UPDATE_FAIL_STATUS_CHANGED);
        }
        operateLogService.recordUpdate(LOG_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableMiscDraft(ErpPayableMiscDraftSaveReqVO updateReqVO) {
        ErpPayableMiscDO db = validatePayableMiscExists(updateReqVO.getId());
        if (!ErpPayableMiscStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_MISC_DRAFT_UPDATE_FAIL, db.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        validateDraftForSave(updateReqVO);
        ErpPayableMiscDO updateObj = BeanUtils.toBean(updateReqVO, ErpPayableMiscDO.class)
                .setId(db.getId())
                .setNo(db.getNo())
                .setStatus(db.getStatus())
                .setBizTime(db.getBizTime());
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(db.getDeptId());
        }
        validateRefs(updateObj.getHandlerId(), updateObj.getDeptId());
        if (payableMiscMapper.updateByIdAndStatus(db.getId(),
                ErpPayableMiscStatusEnum.DRAFT.getStatus(), updateObj) == 0) {
            throw exception(PAYABLE_MISC_DRAFT_UPDATE_FAIL, db.getNo());
        }
        operateLogService.recordUpdate(LOG_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitPayableMisc(ErpPayableMiscSaveReqVO updateReqVO) {
        updatePayableMiscDraft(BeanUtils.toBean(updateReqVO, ErpPayableMiscDraftSaveReqVO.class));
        submitPayableMisc(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPayableMisc(Long id) {
        ErpPayableMiscDO db = payableMiscMapper.selectByIdForUpdate(id);
        if (db == null) {
            throw exception(PAYABLE_MISC_NOT_EXISTS);
        }
        if (!ErpPayableMiscStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_MISC_DRAFT_SUBMIT_FAIL, "当前状态不是草稿");
        }
        validateDraftForSubmit(db);
        if (payableMiscMapper.updateByIdAndStatus(id,
                ErpPayableMiscStatusEnum.DRAFT.getStatus(),
                new ErpPayableMiscDO().setStatus(ErpPayableMiscStatusEnum.PROCESS.getStatus())) == 0) {
            throw exception(PAYABLE_MISC_DRAFT_SUBMIT_FAIL, "状态已变化，请刷新后重试");
        }
        operateLogService.recordUpdate(LOG_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableMiscStatus(Long id, Integer status) {
        ErpPayableMiscDO db = validatePayableMiscExists(id);
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)
                || !ErpPayableMiscStatusEnum.PROCESS.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_MISC_PROCESS_FAIL);
        }
        if (payableMiscMapper.updateByIdAndStatus(id, db.getStatus(),
                new ErpPayableMiscDO().setStatus(status)) == 0) {
            throw exception(PAYABLE_MISC_APPROVE_FAIL);
        }
        operateLogService.recordStatus(LOG_TYPE, id, db.getNo(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePayableMisc(Long id) {
        ErpPayableMiscDO db = validatePayableMiscExists(id);
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_MISC_DELETE_FAIL_APPROVE, db.getNo());
        }
        payableMiscMapper.deleteById(id);
        operateLogService.recordDelete(LOG_TYPE, db.getId(), db.getNo());
    }

    @Override
    public ErpPayableMiscDO getPayableMisc(Long id) {
        return payableMiscMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPayableMiscDO> getPayableMiscPage(ErpPayableMiscPageReqVO pageReqVO) {
        return payableMiscMapper.selectPage(pageReqVO);
    }

    private String generateNo() {
        String no = noRedisDAO.generate(NO_PREFIX);
        if (payableMiscMapper.selectByNo(no) != null) {
            throw exception(PAYABLE_MISC_NO_EXISTS);
        }
        return no;
    }

    private ErpPayableMiscDO validatePayableMiscExists(Long id) {
        ErpPayableMiscDO db = payableMiscMapper.selectById(id);
        if (db == null) {
            throw exception(PAYABLE_MISC_NOT_EXISTS);
        }
        return db;
    }

    private void validateDraftForSave(ErpPayableMiscDraftSaveReqVO reqVO) {
        if (reqVO.getSupplierId() == null) {
            throw exception(PAYABLE_MISC_DRAFT_SAVE_FAIL, "供应商不能为空");
        }
        validateAmountNonZeroIfPresent(reqVO.getAmount(), true);
        supplierService.validateSupplier(reqVO.getSupplierId());
    }

    private void validateDraftForSubmit(ErpPayableMiscDO doObj) {
        if (doObj.getSupplierId() == null) {
            throw exception(PAYABLE_MISC_DRAFT_SUBMIT_FAIL, "供应商不能为空");
        }
        if (doObj.getAmount() == null) {
            throw exception(PAYABLE_MISC_DRAFT_SUBMIT_FAIL, "金额不能为空");
        }
        validateAmountNonZero(doObj.getAmount(), true);
        supplierService.validateSupplier(doObj.getSupplierId());
        validateSubmitRefs(doObj.getAccountId(), doObj.getDeptId(), doObj.getHandlerId(), true);
    }

    private void validateAmountNonZeroIfPresent(BigDecimal amount, boolean draftSubmit) {
        if (amount != null) {
            validateAmountNonZero(amount, draftSubmit);
        }
    }

    private void validateAmountNonZero(BigDecimal amount, boolean draftSubmit) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw exception(draftSubmit ? PAYABLE_MISC_DRAFT_SUBMIT_FAIL : PAYABLE_MISC_SAVE_FAIL,
                    "金额不能为 0");
        }
    }

    private void validateRefs(Long handlerId, Long deptId) {
        if (handlerId != null) {
            adminUserApi.validateUser(handlerId);
        }
        if (deptId != null && deptApi.getDept(deptId) == null) {
            throw exception(PAYABLE_MISC_NOT_EXISTS);
        }
    }

    private void validateAccountIfPresent(Long accountId) {
        if (accountId != null) {
            accountService.validateAccount(accountId);
        }
    }

    private void validateSubmitRefs(Long accountId, Long deptId, Long handlerId, boolean draftSubmit) {
        if (accountId == null) {
            throw exception(draftSubmit ? PAYABLE_MISC_DRAFT_SUBMIT_FAIL : PAYABLE_MISC_SAVE_FAIL,
                    "账户不能为空");
        }
        if (deptId == null) {
            throw exception(draftSubmit ? PAYABLE_MISC_DRAFT_SUBMIT_FAIL : PAYABLE_MISC_SAVE_FAIL,
                    "部门不能为空");
        }
        accountService.validateAccount(accountId);
        validateRefs(handlerId, deptId);
    }

    private void fillCreateDeptId(ErpPayableMiscDO doObj) {
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
