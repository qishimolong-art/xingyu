package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
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

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_PROCESS_FAIL;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayableOther(ErpPayableOtherSaveReqVO createReqVO) {
        supplierService.validateSupplier(createReqVO.getSupplierId());
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
        normalize(doObj);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, doObj);
        payableOtherMapper.insert(doObj);
        operateLogService.recordCreate(ERP_PAYABLE_OTHER_TYPE, doObj.getId(), doObj.getNo());
        return doObj.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableOther(ErpPayableOtherSaveReqVO updateReqVO) {
        ErpPayableOtherDO db = validatePayableOtherExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_PAYABLE_UPDATE_FAIL_APPROVE, db.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        supplierService.validateSupplier(updateReqVO.getSupplierId());
        ErpPayableOtherDO updateObj = BeanUtils.toBean(updateReqVO, ErpPayableOtherDO.class, obj -> {
            if (StrUtil.isBlank(obj.getSourceType())) {
                obj.setSourceType("调账");
            }
        });
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(db.getDeptId());
        }
        validateRefs(updateReqVO.getHandlerId(), updateObj.getDeptId());
        normalize(updateObj);
        int affected = payableOtherMapper.updateByIdAndStatus(updateReqVO.getId(), ErpAuditStatus.PROCESS.getStatus(), updateObj);
        if (affected == 0) {
            throw exception(OTHER_PAYABLE_UPDATE_FAIL_STATUS_CHANGED);
        }
        operateLogService.recordUpdate(ERP_PAYABLE_OTHER_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableOtherStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        ErpPayableOtherDO db = validatePayableOtherExists(id);
        if (db.getStatus().equals(status)) {
            throw exception(approve ? OTHER_PAYABLE_APPROVE_FAIL : OTHER_PAYABLE_PROCESS_FAIL);
        }
        int affected = payableOtherMapper.updateByIdAndStatus(id, db.getStatus(),
                ErpPayableOtherDO.builder().status(status).build());
        if (affected == 0) {
            throw exception(approve ? OTHER_PAYABLE_APPROVE_FAIL : OTHER_PAYABLE_PROCESS_FAIL);
        }
        operateLogService.recordStatus(ERP_PAYABLE_OTHER_TYPE, id, db.getNo(), approve);
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

}
