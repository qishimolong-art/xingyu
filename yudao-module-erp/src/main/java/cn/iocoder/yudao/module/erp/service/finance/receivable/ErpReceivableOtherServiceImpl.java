package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableOtherStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpSaleCartFreightDraftCreateReqBO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_DRAFT_SAVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_SAVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_UPDATE_FAIL_STATUS_CHANGED;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_RECEIVABLE_OTHER_TYPE;

@Service
@Validated
public class ErpReceivableOtherServiceImpl implements ErpReceivableOtherService {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_receivable_other";
    private static final String SALE_CART_SOURCE_TYPE = "销售手推车";

    @Resource
    private ErpReceivableOtherMapper receivableOtherMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpCustomerService customerService;
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
    public Long createReceivableOther(ErpReceivableOtherSaveReqVO createReqVO) {
        validateReceivableAmountNonZero(createReqVO.getReceivableAmount(), false);
        customerService.validateCustomer(createReqVO.getCustomerId());
        String no = noRedisDAO.generate(ErpNoRedisDAO.OTHER_RECEIVABLE_NO_PREFIX);
        if (receivableOtherMapper.selectByNo(no) != null) {
            throw exception(OTHER_RECEIVABLE_NO_EXISTS);
        }
        ErpReceivableOtherDO doObj = BeanUtils.toBean(createReqVO, ErpReceivableOtherDO.class, obj -> obj
                .setNo(no)
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setSourceType(StrUtil.blankToDefault(createReqVO.getSourceType(), "调账")));
        fillCreateDeptId(doObj);
        validateRefs(createReqVO.getHandlerId(), doObj.getDeptId());
        normalize(doObj);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, doObj);
        receivableOtherMapper.insert(doObj);
        operateLogService.recordCreate(ERP_RECEIVABLE_OTHER_TYPE, doObj.getId(), doObj.getNo());
        return doObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReceivableOtherDraft(ErpReceivableOtherDraftSaveReqVO createReqVO) {
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        validateDraftForSave(createReqVO.getCustomerId());
        String no = noRedisDAO.generate(ErpNoRedisDAO.OTHER_RECEIVABLE_NO_PREFIX);
        if (receivableOtherMapper.selectByNo(no) != null) {
            throw exception(OTHER_RECEIVABLE_NO_EXISTS);
        }
        ErpReceivableOtherDO doObj = BeanUtils.toBean(createReqVO, ErpReceivableOtherDO.class)
                .setId(null)
                .setNo(no)
                .setStatus(ErpReceivableOtherStatusEnum.DRAFT.getStatus())
                .setSourceType(StrUtil.blankToDefault(createReqVO.getSourceType(), "调账"));
        fillCreateDeptId(doObj);
        normalizeDraft(doObj);
        receivableOtherMapper.insert(doObj);
        operateLogService.recordCreate(ERP_RECEIVABLE_OTHER_TYPE, doObj.getId(), doObj.getNo());
        return doObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAndSubmitReceivableOther(ErpReceivableOtherSaveReqVO createReqVO) {
        return createReceivableOther(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromSaleCartFreight(ErpSaleCartFreightDraftCreateReqBO createReqBO) {
        ErpReceivableOtherDO existing = receivableOtherMapper.selectBySource(
                SALE_CART_SOURCE_TYPE, createReqBO.getCartId());
        if (existing != null) {
            return existing.getId();
        }
        customerService.validateCustomerForGeneratedSale(createReqBO.getCustomerId(), createReqBO.getDeptId());
        validateRefs(createReqBO.getHandlerId(), createReqBO.getDeptId());
        String no = noRedisDAO.generate(ErpNoRedisDAO.OTHER_RECEIVABLE_NO_PREFIX);
        if (receivableOtherMapper.selectByNo(no) != null) {
            throw exception(OTHER_RECEIVABLE_NO_EXISTS);
        }
        ErpReceivableOtherDO doObj = new ErpReceivableOtherDO()
                .setNo(no)
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(createReqBO.getBizTime())
                .setCustomerId(createReqBO.getCustomerId())
                .setSettledAmount(BigDecimal.ZERO)
                .setDeptId(createReqBO.getDeptId())
                .setReceivableAmount(createReqBO.getAmount())
                .setProject("代客户付运费")
                .setSourceType(SALE_CART_SOURCE_TYPE)
                .setSourceId(createReqBO.getCartId())
                .setSourceNo(createReqBO.getCartNo())
                .setHandlerId(createReqBO.getHandlerId())
                .setReceivableType("客户运费")
                .setCostAmount(BigDecimal.ZERO)
                .setRemark("销售手推车终审自动生成，来源单号：" + createReqBO.getCartNo());
        normalize(doObj);
        receivableOtherMapper.insert(doObj);
        operateLogService.recordCreate(ERP_RECEIVABLE_OTHER_TYPE, doObj.getId(), doObj.getNo());
        return doObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceivableOther(ErpReceivableOtherSaveReqVO updateReqVO) {
        ErpReceivableOtherDO db = validateReceivableOtherExists(updateReqVO.getId());
        if (ErpReceivableOtherStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_RECEIVABLE_DRAFT_UPDATE_FAIL, db.getNo());
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_RECEIVABLE_UPDATE_FAIL_APPROVE, db.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        validateReceivableAmountNonZero(updateReqVO.getReceivableAmount(), false);
        customerService.validateCustomer(updateReqVO.getCustomerId());
        ErpReceivableOtherDO updateObj = BeanUtils.toBean(updateReqVO, ErpReceivableOtherDO.class, obj -> {
            if (StrUtil.isBlank(obj.getSourceType())) {
                obj.setSourceType("调账");
            }
        });
        if (db.getSourceId() != null) {
            updateObj.setSourceType(db.getSourceType());
            updateObj.setSourceId(db.getSourceId());
            updateObj.setSourceNo(db.getSourceNo());
        }
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(db.getDeptId());
        }
        validateRefs(updateReqVO.getHandlerId(), updateObj.getDeptId());
        normalize(updateObj);
        int affected = receivableOtherMapper.updateByIdAndStatus(updateReqVO.getId(),
                ErpAuditStatus.PROCESS.getStatus(), updateObj);
        if (affected == 0) {
            throw exception(OTHER_RECEIVABLE_UPDATE_FAIL_STATUS_CHANGED);
        }
        operateLogService.recordUpdate(ERP_RECEIVABLE_OTHER_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceivableOtherDraft(ErpReceivableOtherDraftSaveReqVO updateReqVO) {
        ErpReceivableOtherDO db = validateReceivableOtherExists(updateReqVO.getId());
        if (!ErpReceivableOtherStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_RECEIVABLE_DRAFT_UPDATE_FAIL, db.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        ErpReceivableOtherDO updateObj = BeanUtils.toBean(updateReqVO, ErpReceivableOtherDO.class)
                .setId(db.getId())
                .setNo(db.getNo())
                .setStatus(db.getStatus());
        if (db.getSourceId() != null) {
            updateObj.setSourceType(db.getSourceType());
            updateObj.setSourceId(db.getSourceId());
            updateObj.setSourceNo(db.getSourceNo());
        } else if (StrUtil.isBlank(updateObj.getSourceType())) {
            updateObj.setSourceType("调账");
        }
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(db.getDeptId());
        }
        validateDraftForSave(updateObj.getCustomerId());
        normalizeDraft(updateObj);
        if (receivableOtherMapper.updateByIdAndStatus(db.getId(),
                ErpReceivableOtherStatusEnum.DRAFT.getStatus(), updateObj) == 0) {
            throw exception(OTHER_RECEIVABLE_DRAFT_UPDATE_FAIL, db.getNo());
        }
        operateLogService.recordUpdate(ERP_RECEIVABLE_OTHER_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitReceivableOther(ErpReceivableOtherSaveReqVO updateReqVO) {
        updateReceivableOtherDraft(
                BeanUtils.toBean(updateReqVO, ErpReceivableOtherDraftSaveReqVO.class));
        submitReceivableOther(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReceivableOther(Long id) {
        ErpReceivableOtherDO db = receivableOtherMapper.selectByIdForUpdate(id);
        if (db == null) {
            throw exception(OTHER_RECEIVABLE_NOT_EXISTS);
        }
        if (!ErpReceivableOtherStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_RECEIVABLE_DRAFT_SUBMIT_FAIL, "当前状态不是草稿");
        }
        validateDraftForSubmit(db);
        if (receivableOtherMapper.updateByIdAndStatus(id,
                ErpReceivableOtherStatusEnum.DRAFT.getStatus(),
                new ErpReceivableOtherDO().setStatus(ErpReceivableOtherStatusEnum.PROCESS.getStatus())) == 0) {
            throw exception(OTHER_RECEIVABLE_DRAFT_SUBMIT_FAIL, "状态已变化，请刷新后重试");
        }
        operateLogService.recordUpdate(ERP_RECEIVABLE_OTHER_TYPE, db.getId(), db.getNo());
    }

    @Override
    public void updateReceivableOtherRemark(ErpFinanceUpdateRemarkReqVO updateReqVO) {
        ErpReceivableOtherDO db = validateReceivableOtherExists(updateReqVO.getId());
        receivableOtherMapper.updateById(new ErpReceivableOtherDO()
                .setId(updateReqVO.getId()).setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_RECEIVABLE_OTHER_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceivableOtherStatus(Long id, Integer status) {
        ErpReceivableOtherDO db = validateReceivableOtherExists(id);
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)
                || !ErpReceivableOtherStatusEnum.PROCESS.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_RECEIVABLE_PROCESS_FAIL);
        }
        int affected = receivableOtherMapper.updateByIdAndStatus(id, db.getStatus(),
                ErpReceivableOtherDO.builder().status(status).build());
        if (affected == 0) {
            throw exception(OTHER_RECEIVABLE_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_RECEIVABLE_OTHER_TYPE, id, db.getNo(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReceivableOther(Long id) {
        ErpReceivableOtherDO db = validateReceivableOtherExists(id);
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_RECEIVABLE_DELETE_FAIL_APPROVE, db.getNo());
        }
        receivableOtherMapper.deleteById(id);
        operateLogService.recordDelete(ERP_RECEIVABLE_OTHER_TYPE, db.getId(), db.getNo());
    }

    @Override
    public ErpReceivableOtherDO getReceivableOther(Long id) {
        return receivableOtherMapper.selectById(id);
    }

    @Override
    public PageResult<ErpReceivableOtherDO> getReceivableOtherPage(ErpReceivableOtherPageReqVO pageReqVO) {
        return receivableOtherMapper.selectPage(pageReqVO);
    }

    private ErpReceivableOtherDO validateReceivableOtherExists(Long id) {
        ErpReceivableOtherDO db = receivableOtherMapper.selectById(id);
        if (db == null) {
            throw exception(OTHER_RECEIVABLE_NOT_EXISTS);
        }
        return db;
    }

    private void validateRefs(Long handlerId, Long deptId) {
        if (handlerId != null) {
            adminUserApi.validateUser(handlerId);
        }
        if (deptId != null && deptApi.getDept(deptId) == null) {
            throw exception(OTHER_RECEIVABLE_NOT_EXISTS);
        }
    }

    private void fillCreateDeptId(ErpReceivableOtherDO doObj) {
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

    private void normalize(ErpReceivableOtherDO doObj) {
        doObj.setVoucherNo(StrUtil.blankToDefault(doObj.getVoucherNo(), ""));
        doObj.setSettledAmount(doObj.getSettledAmount() == null ? BigDecimal.ZERO : doObj.getSettledAmount());
        doObj.setReceivableAmount(doObj.getReceivableAmount() == null ? BigDecimal.ZERO : doObj.getReceivableAmount());
        doObj.setCostAmount(doObj.getCostAmount() == null ? BigDecimal.ZERO : doObj.getCostAmount());
        doObj.setIsPaperNote(Boolean.TRUE.equals(doObj.getIsPaperNote()));
    }

    private void normalizeDraft(ErpReceivableOtherDO doObj) {
        doObj.setVoucherNo(StrUtil.blankToDefault(doObj.getVoucherNo(), ""));
        doObj.setSettledAmount(doObj.getSettledAmount() == null ? BigDecimal.ZERO : doObj.getSettledAmount());
        doObj.setCostAmount(doObj.getCostAmount() == null ? BigDecimal.ZERO : doObj.getCostAmount());
        doObj.setIsPaperNote(Boolean.TRUE.equals(doObj.getIsPaperNote()));
    }

    private void validateDraftForSave(Long customerId) {
        if (customerId == null) {
            throw exception(OTHER_RECEIVABLE_DRAFT_SAVE_FAIL, "客户不能为空");
        }
        customerService.validateCustomer(customerId);
    }

    private void validateDraftForSubmit(ErpReceivableOtherDO doObj) {
        if (doObj.getBizTime() == null) {
            throw exception(OTHER_RECEIVABLE_DRAFT_SUBMIT_FAIL, "业务日期不能为空");
        }
        if (doObj.getCustomerId() == null) {
            throw exception(OTHER_RECEIVABLE_DRAFT_SUBMIT_FAIL, "客户不能为空");
        }
        if (doObj.getReceivableAmount() == null) {
            throw exception(OTHER_RECEIVABLE_DRAFT_SUBMIT_FAIL, "应收金额不能为空");
        }
        validateReceivableAmountNonZero(doObj.getReceivableAmount(), true);
        customerService.validateCustomer(doObj.getCustomerId());
        validateRefs(doObj.getHandlerId(), doObj.getDeptId());
    }

    private void validateReceivableAmountNonZero(BigDecimal receivableAmount, boolean draftSubmit) {
        if (receivableAmount != null && receivableAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw exception(draftSubmit ? OTHER_RECEIVABLE_DRAFT_SUBMIT_FAIL : OTHER_RECEIVABLE_SAVE_FAIL,
                    "应收金额不能为 0");
        }
    }
}
