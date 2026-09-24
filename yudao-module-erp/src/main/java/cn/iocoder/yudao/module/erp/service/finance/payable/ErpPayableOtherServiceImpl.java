package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPayableOtherStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_DRAFT_SAVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_SAVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_SUPPLIER_DEPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_UPDATE_FAIL_STATUS_CHANGED;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PAYABLE_OTHER_TYPE;

@Service
@Validated
public class ErpPayableOtherServiceImpl implements ErpPayableOtherService {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_payable_other";

    @Resource
    private ErpPayableOtherMapper payableOtherMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpSupplierDeptPermissionService supplierDeptPermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayableOther(ErpPayableOtherSaveReqVO createReqVO) {
        validatePayableAmountNonZero(createReqVO.getPayableAmount(), false);
        ErpSupplierDO supplier = supplierService.validateSupplier(createReqVO.getSupplierId());
        String no = noRedisDAO.generate("QTFK");
        if (payableOtherMapper.selectByNo(no) != null) {
            throw exception(OTHER_PAYABLE_NO_EXISTS);
        }
        ErpPayableOtherDO doObj = BeanUtils.toBean(createReqVO, ErpPayableOtherDO.class, obj -> obj
                .setNo(no)
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setSourceType(StrUtil.isBlank(createReqVO.getSourceType()) ? "调账" : createReqVO.getSourceType()));
        fillCreateDeptId(doObj);
        validateRefs(createReqVO.getHandlerId(), doObj.getDeptId());
        validateSupplierDept(supplier, doObj.getDeptId(), false);
        normalize(doObj);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, doObj);
        payableOtherMapper.insert(doObj);
        operateLogService.recordCreate(ERP_PAYABLE_OTHER_TYPE, doObj.getId(), doObj.getNo());
        return doObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayableOtherDraft(ErpPayableOtherDraftSaveReqVO createReqVO) {
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        validateDraftForSave(createReqVO.getSupplierId());
        String no = noRedisDAO.generate("QTFK");
        if (payableOtherMapper.selectByNo(no) != null) {
            throw exception(OTHER_PAYABLE_NO_EXISTS);
        }
        ErpPayableOtherDO doObj = BeanUtils.toBean(createReqVO, ErpPayableOtherDO.class)
                .setId(null)
                .setNo(no)
                .setStatus(ErpPayableOtherStatusEnum.DRAFT.getStatus())
                .setSourceType(StrUtil.blankToDefault(createReqVO.getSourceType(), "调账"));
        fillCreateDeptId(doObj);
        validateSupplierDept(doObj.getSupplierId(), doObj.getDeptId(), true);
        normalizeDraft(doObj);
        payableOtherMapper.insert(doObj);
        operateLogService.recordCreate(ERP_PAYABLE_OTHER_TYPE, doObj.getId(), doObj.getNo());
        return doObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAndSubmitPayableOther(ErpPayableOtherSaveReqVO createReqVO) {
        return createPayableOther(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromFinancePaymentDiscount(ErpFinancePaymentDO payment) {
        BigDecimal discountPrice = payment == null || payment.getDiscountPrice() == null
                ? BigDecimal.ZERO : payment.getDiscountPrice();
        if (payment == null || payment.getId() == null || discountPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        ErpPayableOtherDO existing = payableOtherMapper.selectBySource(
                PAYMENT_DISCOUNT_SOURCE_TYPE, payment.getId());
        if (existing != null) {
            return existing.getId();
        }
        supplierService.validateSupplier(payment.getSupplierId());
        validateRefs(payment.getFinanceUserId(), payment.getDeptId());
        String no = noRedisDAO.generate("QTFK");
        if (payableOtherMapper.selectByNo(no) != null) {
            throw exception(OTHER_PAYABLE_NO_EXISTS);
        }
        ErpPayableOtherDO doObj = new ErpPayableOtherDO()
                .setNo(no)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setBizTime(payment.getPaymentTime() == null ? null : payment.getPaymentTime().toLocalDate())
                .setSupplierId(payment.getSupplierId())
                .setVoucherNo("")
                .setSettledAmount(BigDecimal.ZERO)
                .setDeptId(payment.getDeptId())
                .setPayableAmount(discountPrice.negate())
                .setProject("优惠折让")
                .setSourceType(PAYMENT_DISCOUNT_SOURCE_TYPE)
                .setSourceId(payment.getId())
                .setSourceNo(payment.getNo())
                .setHandlerId(payment.getFinanceUserId())
                .setRemark("付款单审核自动生成，来源单号：" + payment.getNo());
        normalize(doObj);
        payableOtherMapper.insert(doObj);
        operateLogService.recordCreate(ERP_PAYABLE_OTHER_TYPE, doObj.getId(), doObj.getNo());
        operateLogService.recordStatus(ERP_PAYABLE_OTHER_TYPE, doObj.getId(), doObj.getNo(), true);
        return doObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableOther(ErpPayableOtherSaveReqVO updateReqVO) {
        ErpPayableOtherDO db = validatePayableOtherExists(updateReqVO.getId());
        if (ErpPayableOtherStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_PAYABLE_DRAFT_UPDATE_FAIL, db.getNo());
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_PAYABLE_UPDATE_FAIL_APPROVE, db.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        validatePayableAmountNonZero(updateReqVO.getPayableAmount(), false);
        ErpSupplierDO supplier = supplierService.validateSupplier(updateReqVO.getSupplierId());
        ErpPayableOtherDO updateObj = BeanUtils.toBean(updateReqVO, ErpPayableOtherDO.class, obj -> {
            if (StrUtil.isBlank(obj.getSourceType())) {
                obj.setSourceType("调账");
            }
        });
        preserveSource(updateObj, db);
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(db.getDeptId());
        }
        validateRefs(updateReqVO.getHandlerId(), updateObj.getDeptId());
        validateSupplierDept(supplier, updateObj.getDeptId(), false);
        normalize(updateObj);
        int affected = payableOtherMapper.updateByIdAndStatus(updateReqVO.getId(), ErpAuditStatus.PROCESS.getStatus(), updateObj);
        if (affected == 0) {
            throw exception(OTHER_PAYABLE_UPDATE_FAIL_STATUS_CHANGED);
        }
        operateLogService.recordUpdate(ERP_PAYABLE_OTHER_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableOtherDraft(ErpPayableOtherDraftSaveReqVO updateReqVO) {
        ErpPayableOtherDO db = validatePayableOtherExists(updateReqVO.getId());
        if (!ErpPayableOtherStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_PAYABLE_DRAFT_UPDATE_FAIL, db.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        validateDraftForSave(updateReqVO.getSupplierId());
        ErpPayableOtherDO updateObj = BeanUtils.toBean(updateReqVO, ErpPayableOtherDO.class)
                .setId(db.getId())
                .setNo(db.getNo())
                .setStatus(db.getStatus());
        preserveSource(updateObj, db);
        if (StrUtil.isBlank(updateObj.getSourceType())) {
            updateObj.setSourceType(StrUtil.blankToDefault(db.getSourceType(), "调账"));
        }
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(db.getDeptId());
        }
        validateSupplierDept(updateObj.getSupplierId(), updateObj.getDeptId(), true);
        normalizeDraft(updateObj);
        if (payableOtherMapper.updateByIdAndStatus(db.getId(),
                ErpPayableOtherStatusEnum.DRAFT.getStatus(), updateObj) == 0) {
            throw exception(OTHER_PAYABLE_DRAFT_UPDATE_FAIL, db.getNo());
        }
        operateLogService.recordUpdate(ERP_PAYABLE_OTHER_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitPayableOther(ErpPayableOtherSaveReqVO updateReqVO) {
        updatePayableOtherDraft(BeanUtils.toBean(updateReqVO, ErpPayableOtherDraftSaveReqVO.class));
        submitPayableOther(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPayableOther(Long id) {
        ErpPayableOtherDO db = payableOtherMapper.selectByIdForUpdate(id);
        if (db == null) {
            throw exception(OTHER_PAYABLE_NOT_EXISTS);
        }
        if (!ErpPayableOtherStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_PAYABLE_DRAFT_SUBMIT_FAIL, "当前状态不是草稿");
        }
        validateDraftForSubmit(db);
        if (payableOtherMapper.updateByIdAndStatus(id,
                ErpPayableOtherStatusEnum.DRAFT.getStatus(),
                new ErpPayableOtherDO().setStatus(ErpPayableOtherStatusEnum.PROCESS.getStatus())) == 0) {
            throw exception(OTHER_PAYABLE_DRAFT_SUBMIT_FAIL, "状态已变化，请刷新后重试");
        }
        operateLogService.recordUpdate(ERP_PAYABLE_OTHER_TYPE, db.getId(), db.getNo());
    }

    @Override
    public void updatePayableOtherRemark(ErpFinanceUpdateRemarkReqVO updateReqVO) {
        ErpPayableOtherDO db = validatePayableOtherExists(updateReqVO.getId());
        payableOtherMapper.updateById(new ErpPayableOtherDO()
                .setId(updateReqVO.getId()).setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_PAYABLE_OTHER_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableOtherStatus(Long id, Integer status) {
        ErpPayableOtherDO db = validatePayableOtherExists(id);
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)
                || !ErpPayableOtherStatusEnum.PROCESS.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_PAYABLE_APPROVE_FAIL);
        }
        validateSupplierDept(db.getSupplierId(), db.getDeptId(), false);
        int affected = payableOtherMapper.updateByIdAndStatus(id, db.getStatus(),
                ErpPayableOtherDO.builder().status(status).build());
        if (affected == 0) {
            throw exception(OTHER_PAYABLE_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_PAYABLE_OTHER_TYPE, id, db.getNo(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePayableOther(Long id) {
        ErpPayableOtherDO db = validatePayableOtherExists(id);
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_PAYABLE_DELETE_FAIL_APPROVE, db.getNo());
        }
        payableOtherMapper.deleteById(id);
        operateLogService.recordDelete(ERP_PAYABLE_OTHER_TYPE, db.getId(), db.getNo());
    }

    @Override
    public ErpPayableOtherDO getPayableOther(Long id) {
        return payableOtherMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPayableOtherDO> getPayableOtherPage(ErpPayableOtherPageReqVO pageReqVO) {
        return payableOtherMapper.selectPage(pageReqVO);
    }

    private ErpPayableOtherDO validatePayableOtherExists(Long id) {
        ErpPayableOtherDO db = payableOtherMapper.selectById(id);
        if (db == null) {
            throw exception(OTHER_PAYABLE_NOT_EXISTS);
        }
        return db;
    }

    private void validateRefs(Long handlerId, Long deptId) {
        if (handlerId != null) {
            adminUserApi.validateUser(handlerId);
        }
        if (deptId != null && deptApi.getDept(deptId) == null) {
            throw exception(OTHER_PAYABLE_NOT_EXISTS);
        }
    }

    private void fillCreateDeptId(ErpPayableOtherDO doObj) {
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

    private void normalize(ErpPayableOtherDO doObj) {
        doObj.setVoucherNo(StrUtil.blankToDefault(doObj.getVoucherNo(), ""));
        doObj.setSettledAmount(doObj.getSettledAmount() == null ? BigDecimal.ZERO : doObj.getSettledAmount());
    }

    private void normalizeDraft(ErpPayableOtherDO doObj) {
        doObj.setVoucherNo(StrUtil.blankToDefault(doObj.getVoucherNo(), ""));
        doObj.setSettledAmount(doObj.getSettledAmount() == null ? BigDecimal.ZERO : doObj.getSettledAmount());
    }

    private void validateDraftForSave(Long supplierId) {
        if (supplierId == null) {
            throw exception(OTHER_PAYABLE_DRAFT_SAVE_FAIL, "供应商不能为空");
        }
        supplierService.validateSupplier(supplierId);
    }

    private void validatePayableAmountNonZero(BigDecimal payableAmount, boolean draftSubmit) {
        if (payableAmount != null && payableAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw exception(draftSubmit ? OTHER_PAYABLE_DRAFT_SUBMIT_FAIL : OTHER_PAYABLE_SAVE_FAIL,
                    "应付金额不能为 0");
        }
    }

    private void validateDraftForSubmit(ErpPayableOtherDO doObj) {
        if (doObj.getSupplierId() == null) {
            throw exception(OTHER_PAYABLE_DRAFT_SUBMIT_FAIL, "供应商不能为空");
        }
        if (doObj.getPayableAmount() == null) {
            throw exception(OTHER_PAYABLE_DRAFT_SUBMIT_FAIL, "应付金额不能为空");
        }
        validatePayableAmountNonZero(doObj.getPayableAmount(), true);
        if (doObj.getDeptId() == null) {
            throw exception(OTHER_PAYABLE_DRAFT_SUBMIT_FAIL, "部门不能为空");
        }
        ErpSupplierDO supplier = supplierService.validateSupplier(doObj.getSupplierId());
        validateRefs(doObj.getHandlerId(), doObj.getDeptId());
        validateSupplierDept(supplier, doObj.getDeptId(), true);
    }

    private void validateSupplierDept(Long supplierId, Long deptId, boolean draftSubmit) {
        if (supplierId == null || deptId == null) {
            return;
        }
        validateSupplierDept(supplierService.validateSupplier(supplierId), deptId, draftSubmit);
    }

    private void validateSupplierDept(ErpSupplierDO supplier, Long deptId, boolean draftSubmit) {
        if (supplier == null || deptId == null) {
            return;
        }
        if (supplierDeptPermissionService.hasAvailableDept(supplier, deptId, "erp_payable_other")) {
            return;
        }
        if (draftSubmit) {
            throw exception(OTHER_PAYABLE_DRAFT_SUBMIT_FAIL,
                    OTHER_PAYABLE_SUPPLIER_DEPT_NOT_ALLOWED.getMsg());
        }
        throw exception(OTHER_PAYABLE_SUPPLIER_DEPT_NOT_ALLOWED);
    }

    private void preserveSource(ErpPayableOtherDO target, ErpPayableOtherDO db) {
        target.setSourceType(StrUtil.blankToDefault(db.getSourceType(), "调账"));
        target.setSourceId(db.getSourceId());
        target.setSourceNo(db.getSourceNo());
    }

}
