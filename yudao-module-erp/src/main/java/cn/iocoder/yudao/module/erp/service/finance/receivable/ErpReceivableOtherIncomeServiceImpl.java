package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherIncomeItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherIncomeMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_UPDATE_FAIL_STATUS_CHANGED;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_RECEIVABLE_OTHER_INCOME_TYPE;

@Service
@Validated
public class ErpReceivableOtherIncomeServiceImpl implements ErpReceivableOtherIncomeService {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_receivable_other_income";

    @Resource
    private ErpReceivableOtherIncomeMapper otherIncomeMapper;
    @Resource
    private ErpReceivableOtherIncomeItemMapper otherIncomeItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
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
    public Long createOtherIncome(ErpReceivableOtherIncomeSaveReqVO createReqVO) {
        fillDefaultDeptId(createReqVO);
        validateRefs(createReqVO.getAccountId(), createReqVO.getHandlerId(), createReqVO.getDeptId());
        createReqVO.getItems().forEach(item -> validateRefs(null, item.getHandlerId(), item.getDeptId()));

        String no = noRedisDAO.generate(ErpNoRedisDAO.OTHER_INCOME_NO_PREFIX);
        ErpReceivableOtherIncomeDO db = BeanUtils.toBean(createReqVO, ErpReceivableOtherIncomeDO.class,
                obj -> obj.setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, db);
        db.setTotalAmount(sumAmount(createReqVO.getItems()));
        otherIncomeMapper.insert(db);
        List<ErpReceivableOtherIncomeItemDO> incomeItems = BeanUtils.toBean(createReqVO.getItems(),
                ErpReceivableOtherIncomeItemDO.class, item -> item.setId(null).setIncomeId(db.getId()));
        fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, incomeItems);
        otherIncomeItemMapper.insertBatch(incomeItems);
        operateLogService.recordCreate(ERP_RECEIVABLE_OTHER_INCOME_TYPE, db.getId(), db.getNo());
        return db.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOtherIncome(ErpReceivableOtherIncomeSaveReqVO updateReqVO) {
        ErpReceivableOtherIncomeDO db = validateExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_RECEIVABLE_UPDATE_FAIL_APPROVE, db.getNo());
        }
        List<ErpReceivableOtherIncomeItemDO> oldItems = otherIncomeItemMapper.selectListByIncomeId(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        if (fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "items")) {
            updateReqVO.setItems(BeanUtils.toBean(oldItems, ErpReceivableOtherIncomeSaveReqVO.Item.class));
        } else {
            fieldPermissionMasker.preserveOrClearHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldItems);
            fillDefaultItemDeptId(updateReqVO.getItems());
        }
        if (updateReqVO.getDeptId() == null) {
            updateReqVO.setDeptId(db.getDeptId());
        }
        validateRefs(updateReqVO.getAccountId(), updateReqVO.getHandlerId(), updateReqVO.getDeptId());
        updateReqVO.getItems().forEach(item -> validateRefs(null, item.getHandlerId(), item.getDeptId()));

        ErpReceivableOtherIncomeDO updateObj = BeanUtils.toBean(updateReqVO, ErpReceivableOtherIncomeDO.class);
        updateObj.setTotalAmount(sumAmount(updateReqVO.getItems()));
        if (otherIncomeMapper.updateByIdAndStatus(updateReqVO.getId(), ErpAuditStatus.PROCESS.getStatus(), updateObj) == 0) {
            throw exception(OTHER_RECEIVABLE_UPDATE_FAIL_STATUS_CHANGED);
        }

        if (CollUtil.isNotEmpty(oldItems)) {
            otherIncomeItemMapper.deleteByIds(convertList(oldItems, ErpReceivableOtherIncomeItemDO::getId));
        }
        otherIncomeItemMapper.insertBatch(BeanUtils.toBean(updateReqVO.getItems(),
                ErpReceivableOtherIncomeItemDO.class, item -> item.setId(null).setIncomeId(updateReqVO.getId())));
        operateLogService.recordUpdate(ERP_RECEIVABLE_OTHER_INCOME_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOtherIncomeStatus(Long id, Integer status) {
        ErpReceivableOtherIncomeDO db = validateExists(id);
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)
                || ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_RECEIVABLE_PROCESS_FAIL);
        }
        if (otherIncomeMapper.updateByIdAndStatus(id, db.getStatus(),
                ErpReceivableOtherIncomeDO.builder().status(status).build()) == 0) {
            throw exception(OTHER_RECEIVABLE_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_RECEIVABLE_OTHER_INCOME_TYPE, id, db.getNo(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOtherIncome(List<Long> ids) {
        List<ErpReceivableOtherIncomeDO> list = otherIncomeMapper.selectByIds(ids);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list.forEach(item -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(item.getStatus())) {
                throw exception(OTHER_RECEIVABLE_DELETE_FAIL_APPROVE, item.getNo());
            }
        });
        otherIncomeMapper.deleteByIds(ids);
        for (Long id : ids) {
            List<ErpReceivableOtherIncomeItemDO> items = otherIncomeItemMapper.selectListByIncomeId(id);
            if (CollUtil.isNotEmpty(items)) {
                otherIncomeItemMapper.deleteByIds(convertList(items, ErpReceivableOtherIncomeItemDO::getId));
            }
        }
        list.forEach(item -> operateLogService.recordDelete(ERP_RECEIVABLE_OTHER_INCOME_TYPE, item.getId(), item.getNo()));
    }

    @Override
    public ErpReceivableOtherIncomeDO getOtherIncome(Long id) {
        return otherIncomeMapper.selectById(id);
    }

    @Override
    public PageResult<ErpReceivableOtherIncomeDO> getOtherIncomePage(ErpReceivableOtherIncomePageReqVO pageReqVO) {
        if (hasItemFilter(pageReqVO)) {
            List<Long> incomeIds = convertList(
                    otherIncomeItemMapper.selectListByItemNameOrInvoiceNo(pageReqVO.getItemName(), pageReqVO.getInvoiceNo()),
                    ErpReceivableOtherIncomeItemDO::getIncomeId);
            if (CollUtil.isEmpty(incomeIds)) {
                return PageResult.empty();
            }
            pageReqVO.setIds(incomeIds);
        }
        return otherIncomeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpReceivableOtherIncomeItemDO> getOtherIncomeItemListByIncomeId(Long incomeId) {
        return otherIncomeItemMapper.selectListByIncomeId(incomeId);
    }

    @Override
    public List<ErpReceivableOtherIncomeItemDO> getOtherIncomeItemListByIncomeIds(Collection<Long> incomeIds) {
        if (CollUtil.isEmpty(incomeIds)) {
            return Collections.emptyList();
        }
        return otherIncomeItemMapper.selectListByIncomeIds(incomeIds);
    }

    private ErpReceivableOtherIncomeDO validateExists(Long id) {
        ErpReceivableOtherIncomeDO db = otherIncomeMapper.selectById(id);
        if (db == null) {
            throw exception(OTHER_RECEIVABLE_NOT_EXISTS);
        }
        return db;
    }

    private void validateRefs(Long accountId, Long handlerId, Long deptId) {
        if (accountId != null) {
            accountService.validateAccount(accountId);
        }
        if (handlerId != null) {
            adminUserApi.validateUser(handlerId);
        }
        if (deptId != null && deptApi.getDept(deptId) == null) {
            throw exception(OTHER_RECEIVABLE_NOT_EXISTS);
        }
    }

    private void fillDefaultDeptId(ErpReceivableOtherIncomeSaveReqVO reqVO) {
        Long deptId = getLoginUserDeptId();
        if (deptId == null) {
            return;
        }
        if (reqVO.getDeptId() == null) {
            reqVO.setDeptId(deptId);
        }
        fillDefaultItemDeptId(reqVO.getItems(), deptId);
    }

    private void fillDefaultItemDeptId(List<ErpReceivableOtherIncomeSaveReqVO.Item> items) {
        Long deptId = getLoginUserDeptId();
        if (deptId != null) {
            fillDefaultItemDeptId(items, deptId);
        }
    }

    private void fillDefaultItemDeptId(List<ErpReceivableOtherIncomeSaveReqVO.Item> items, Long deptId) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> {
            if (item.getDeptId() == null) {
                item.setDeptId(deptId);
            }
        });
    }

    private Long getLoginUserDeptId() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUser(loginUserId);
        return user == null ? null : user.getDeptId();
    }

    private BigDecimal sumAmount(List<ErpReceivableOtherIncomeSaveReqVO.Item> items) {
        return items.stream()
                .map(ErpReceivableOtherIncomeSaveReqVO.Item::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean hasItemFilter(ErpReceivableOtherIncomePageReqVO pageReqVO) {
        return StringUtils.hasText(pageReqVO.getItemName()) || StringUtils.hasText(pageReqVO.getInvoiceNo());
    }
}
