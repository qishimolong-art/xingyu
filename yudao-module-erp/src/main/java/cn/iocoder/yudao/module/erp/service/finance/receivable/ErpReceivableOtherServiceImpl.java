package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
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

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_UPDATE_FAIL_STATUS_CHANGED;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_RECEIVABLE_OTHER_TYPE;

@Service
@Validated
public class ErpReceivableOtherServiceImpl implements ErpReceivableOtherService {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_receivable_other";

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
    public void updateReceivableOther(ErpReceivableOtherSaveReqVO updateReqVO) {
        ErpReceivableOtherDO db = validateReceivableOtherExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_RECEIVABLE_UPDATE_FAIL_APPROVE, db.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        customerService.validateCustomer(updateReqVO.getCustomerId());
        ErpReceivableOtherDO updateObj = BeanUtils.toBean(updateReqVO, ErpReceivableOtherDO.class, obj -> {
            if (StrUtil.isBlank(obj.getSourceType())) {
                obj.setSourceType("调账");
            }
        });
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
    public void updateReceivableOtherStatus(Long id, Integer status) {
        ErpReceivableOtherDO db = validateReceivableOtherExists(id);
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)
                || ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
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
}
