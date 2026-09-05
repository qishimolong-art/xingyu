package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherIncomeItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherIncomeMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseDataService;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableOtherIncomeStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_UPDATE_FAIL_STATUS_CHANGED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_INCOME_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_INCOME_DRAFT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_INCOME_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_INCOME_DEPT_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_INCOME_OPTION_INVALID;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_RECEIVABLE_OTHER_INCOME_TYPE;

@Service
@Validated
public class ErpReceivableOtherIncomeServiceImpl implements ErpReceivableOtherIncomeService {

    private static final LocalDateTime MIN_VALID_BIZ_TIME = LocalDateTime.of(1970, 1, 2, 0, 0);

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_receivable_other_income";
    private static final String SETTLE_METHOD_TYPE = "settle_method";
    private static final String OTHER_INCOME_TYPE = "receivable_other_income_type";
    private static final String OTHER_INCOME_DOC_TYPE = "receivable_other_income_doc_type";
    private static final String OTHER_INCOME_ITEM_PROJECT = "receivable_other_income_item_project";

    @Resource
    private ErpReceivableOtherIncomeMapper otherIncomeMapper;
    @Resource
    private ErpReceivableOtherIncomeItemMapper otherIncomeItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpBaseDataService baseDataService;
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
    public Long createOtherIncome(ErpReceivableOtherIncomeSaveReqVO createReqVO) {
        fillDefaultDeptId(createReqVO);
        validateFormalDeptId(createReqVO.getDeptId());
        validateSaveOptions(createReqVO);
        validateRefs(createReqVO.getAccountId(), createReqVO.getHandlerId(), createReqVO.getDeptId());
        createReqVO.getItems().forEach(this::validateSaveItemRefs);

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
    public Long createOtherIncomeDraft(ErpReceivableOtherIncomeDraftSaveReqVO createReqVO) {
        normalizeDraftBizTime(createReqVO, null);
        List<ErpReceivableOtherIncomeDraftSaveReqVO.Item> items = safeDraftItems(createReqVO.getItems());
        if (CollUtil.isEmpty(items)) {
            throw exception(OTHER_INCOME_DRAFT_ITEMS_REQUIRED);
        }
        validateDraftOptions(createReqVO, items);
        validateRefs(createReqVO.getAccountId(), createReqVO.getHandlerId(), createReqVO.getDeptId());
        items.forEach(this::validateDraftItemRefs);

        String no = noRedisDAO.generate(ErpNoRedisDAO.OTHER_INCOME_NO_PREFIX);
        ErpReceivableOtherIncomeDO db = BeanUtils.toBean(createReqVO, ErpReceivableOtherIncomeDO.class,
                obj -> obj.setId(null).setNo(no)
                        .setStatus(ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus()));
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, db);
        db.setTotalAmount(sumDraftAmount(items));
        otherIncomeMapper.insert(db);
        List<ErpReceivableOtherIncomeItemDO> incomeItems = BeanUtils.toBean(items,
                ErpReceivableOtherIncomeItemDO.class, item -> item.setId(null).setIncomeId(db.getId()));
        fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, incomeItems);
        if (CollUtil.isNotEmpty(incomeItems)) {
            otherIncomeItemMapper.insertBatch(incomeItems);
        }
        operateLogService.recordCreate(ERP_RECEIVABLE_OTHER_INCOME_TYPE, db.getId(), db.getNo());
        return db.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOtherIncomeAndSubmit(ErpReceivableOtherIncomeSaveReqVO createReqVO) {
        return createOtherIncome(createReqVO);
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
        validateFormalDeptId(updateReqVO.getDeptId());
        validateSaveOptions(updateReqVO);
        validateRefs(updateReqVO.getAccountId(), updateReqVO.getHandlerId(), updateReqVO.getDeptId());
        updateReqVO.getItems().forEach(this::validateSaveItemRefs);

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
    public void updateOtherIncomeDraft(ErpReceivableOtherIncomeDraftSaveReqVO updateReqVO) {
        ErpReceivableOtherIncomeDO db = validateExists(updateReqVO.getId());
        if (!ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_INCOME_DRAFT_UPDATE_FAIL, db.getNo());
        }
        normalizeDraftBizTime(updateReqVO, db.getBizTime());
        List<ErpReceivableOtherIncomeItemDO> oldItems =
                otherIncomeItemMapper.selectListByIncomeId(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        List<ErpReceivableOtherIncomeDraftSaveReqVO.Item> items = safeDraftItems(updateReqVO.getItems());
        if (fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "items")) {
            items = BeanUtils.toBean(oldItems, ErpReceivableOtherIncomeDraftSaveReqVO.Item.class);
        } else {
            fieldPermissionMasker.preserveOrClearHiddenItemFields(FIELD_PERMISSION_MODULE, items, oldItems);
        }
        if (updateReqVO.getDeptId() == null) {
            updateReqVO.setDeptId(db.getDeptId());
        }
        validateDraftOptions(updateReqVO, items);
        validateRefs(updateReqVO.getAccountId(), updateReqVO.getHandlerId(), updateReqVO.getDeptId());
        items.forEach(this::validateDraftItemRefs);

        ErpReceivableOtherIncomeDO updateObj = BeanUtils.toBean(updateReqVO,
                ErpReceivableOtherIncomeDO.class);
        updateObj.setId(updateReqVO.getId());
        updateObj.setNo(db.getNo());
        updateObj.setStatus(ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus());
        updateObj.setTotalAmount(sumDraftAmount(items));
        if (otherIncomeMapper.updateByIdAndStatus(updateReqVO.getId(),
                ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus(), updateObj) == 0) {
            throw exception(OTHER_INCOME_DRAFT_UPDATE_FAIL, db.getNo());
        }
        otherIncomeItemMapper.deleteByIncomeId(updateReqVO.getId());
        if (CollUtil.isNotEmpty(items)) {
            otherIncomeItemMapper.insertBatch(BeanUtils.toBean(items,
                    ErpReceivableOtherIncomeItemDO.class,
                    item -> item.setId(null).setIncomeId(updateReqVO.getId())));
        }
        operateLogService.recordUpdate(ERP_RECEIVABLE_OTHER_INCOME_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOtherIncomeDraftAndSubmit(ErpReceivableOtherIncomeDraftSaveReqVO updateReqVO) {
        updateOtherIncomeDraft(updateReqVO);
        submitOtherIncome(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitOtherIncome(Long id) {
        ErpReceivableOtherIncomeDO db = otherIncomeMapper.selectByIdForUpdate(id);
        if (db == null) {
            throw exception(OTHER_RECEIVABLE_NOT_EXISTS);
        }
        if (!ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_INCOME_DRAFT_SUBMIT_FAIL, "单据不是草稿或状态已变化");
        }
        List<ErpReceivableOtherIncomeItemDO> items = otherIncomeItemMapper.selectListByIncomeId(id);
        validateOtherIncomeForSubmit(db, items);
        if (otherIncomeMapper.updateByIdAndStatus(id,
                ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus(),
                ErpReceivableOtherIncomeDO.builder()
                        .status(ErpReceivableOtherIncomeStatusEnum.PROCESS.getStatus())
                        .totalAmount(items.stream()
                                .map(ErpReceivableOtherIncomeItemDO::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build()) == 0) {
            throw exception(OTHER_INCOME_DRAFT_SUBMIT_FAIL, "状态已变化，请刷新后重试");
        }
        operateLogService.recordStatus(ERP_RECEIVABLE_OTHER_INCOME_TYPE, id, db.getNo(), true);
    }

    @Override
    public void updateOtherIncomeRemark(ErpFinanceUpdateRemarkReqVO updateReqVO) {
        ErpReceivableOtherIncomeDO db = validateExists(updateReqVO.getId());
        otherIncomeMapper.updateById(new ErpReceivableOtherIncomeDO()
                .setId(updateReqVO.getId()).setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_RECEIVABLE_OTHER_INCOME_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOtherIncomeStatus(Long id, Integer status) {
        ErpReceivableOtherIncomeDO db = validateExists(id);
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)
                || !ErpReceivableOtherIncomeStatusEnum.PROCESS.getStatus().equals(db.getStatus())) {
            throw exception(OTHER_RECEIVABLE_PROCESS_FAIL);
        }
        validateFormalDeptId(db.getDeptId());
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
    public PageResult<ErpReceivableOtherIncomeItemDO> getOtherIncomeItemPage(ErpReceivableOtherIncomeItemPageReqVO pageReqVO) {
        validateExists(pageReqVO.getIncomeId());
        return otherIncomeItemMapper.selectPageByIncomeId(pageReqVO);
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

    private void validateFormalDeptId(Long deptId) {
        if (deptId == null) {
            throw exception(OTHER_INCOME_DEPT_REQUIRED);
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

    private List<ErpReceivableOtherIncomeDraftSaveReqVO.Item> safeDraftItems(
            List<ErpReceivableOtherIncomeDraftSaveReqVO.Item> items) {
        return items == null ? Collections.emptyList() : items.stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.hasText(item.getItemName()) && item.getAmount() != null)
                .collect(Collectors.toList());
    }

    private BigDecimal sumDraftAmount(List<ErpReceivableOtherIncomeDraftSaveReqVO.Item> items) {
        return items.stream()
                .filter(Objects::nonNull)
                .map(ErpReceivableOtherIncomeDraftSaveReqVO.Item::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void normalizeDraftBizTime(ErpReceivableOtherIncomeDraftSaveReqVO reqVO,
                                       LocalDateTime persistedBizTime) {
        LocalDateTime bizTime = reqVO.getBizTime();
        if (bizTime != null && bizTime.isAfter(MIN_VALID_BIZ_TIME)) {
            return;
        }
        if (persistedBizTime != null && persistedBizTime.isAfter(MIN_VALID_BIZ_TIME)) {
            reqVO.setBizTime(persistedBizTime);
            return;
        }
        reqVO.setBizTime(LocalDateTime.now());
    }

    private void validateOtherIncomeForSubmit(ErpReceivableOtherIncomeDO db,
                                              List<ErpReceivableOtherIncomeItemDO> items) {
        if (db.getBizTime() == null) {
            throw exception(OTHER_INCOME_DRAFT_SUBMIT_FAIL, "日期不能为空");
        }
        if (!StringUtils.hasText(db.getSettleMethod())) {
            throw exception(OTHER_INCOME_DRAFT_SUBMIT_FAIL, "结算方式不能为空");
        }
        if (db.getAccountId() == null) {
            throw exception(OTHER_INCOME_DRAFT_SUBMIT_FAIL, "账户不能为空");
        }
        if (!StringUtils.hasText(db.getIncomeType())) {
            throw exception(OTHER_INCOME_DRAFT_SUBMIT_FAIL, "收入类型不能为空");
        }
        if (db.getDeptId() == null) {
            throw exception(OTHER_INCOME_DRAFT_SUBMIT_FAIL, "开单部门不能为空");
        }
        if (db.getHandlerId() == null) {
            throw exception(OTHER_INCOME_DRAFT_SUBMIT_FAIL, "经手人不能为空");
        }
        if (CollUtil.isEmpty(items)) {
            throw exception(OTHER_INCOME_DRAFT_SUBMIT_FAIL, "至少需要一条收入明细");
        }
        for (int i = 0; i < items.size(); i++) {
            ErpReceivableOtherIncomeItemDO item = items.get(i);
            if (item == null || !StringUtils.hasText(item.getItemName())) {
                throw exception(OTHER_INCOME_DRAFT_SUBMIT_FAIL,
                        "第 " + (i + 1) + " 条明细的项目名称不能为空");
            }
            if (item.getAmount() == null) {
                throw exception(OTHER_INCOME_DRAFT_SUBMIT_FAIL,
                        "第 " + (i + 1) + " 条明细的金额不能为空");
            }
        }
        validateRefs(db.getAccountId(), db.getHandlerId(), db.getDeptId());
        validateSubmitOptions(db, items);
        items.forEach(this::validateDbItemRefs);
    }

    private boolean hasItemFilter(ErpReceivableOtherIncomePageReqVO pageReqVO) {
        return StringUtils.hasText(pageReqVO.getItemName()) || StringUtils.hasText(pageReqVO.getInvoiceNo());
    }

    private void validateSaveOptions(ErpReceivableOtherIncomeSaveReqVO reqVO) {
        validateRequiredOption("结算方式", SETTLE_METHOD_TYPE, reqVO.getSettleMethod());
        validateRequiredOption("收入类型", OTHER_INCOME_TYPE, reqVO.getIncomeType());
        validateOptionalOption("单据类型", OTHER_INCOME_DOC_TYPE, reqVO.getDocType());
        validateRequiredItemNames(reqVO.getItems());
    }

    private void validateDraftOptions(ErpReceivableOtherIncomeDraftSaveReqVO reqVO,
                                      List<ErpReceivableOtherIncomeDraftSaveReqVO.Item> items) {
        validateOptionalOption("结算方式", SETTLE_METHOD_TYPE, reqVO.getSettleMethod());
        validateOptionalOption("收入类型", OTHER_INCOME_TYPE, reqVO.getIncomeType());
        validateOptionalOption("单据类型", OTHER_INCOME_DOC_TYPE, reqVO.getDocType());
        validateDraftItemNames(items);
    }

    private void validateSubmitOptions(ErpReceivableOtherIncomeDO db,
                                       List<ErpReceivableOtherIncomeItemDO> items) {
        validateRequiredOption("结算方式", SETTLE_METHOD_TYPE, db.getSettleMethod());
        validateRequiredOption("收入类型", OTHER_INCOME_TYPE, db.getIncomeType());
        validateOptionalOption("单据类型", OTHER_INCOME_DOC_TYPE, db.getDocType());
        Set<String> validItemNames = getEnabledOptionNames(OTHER_INCOME_ITEM_PROJECT);
        for (int i = 0; i < items.size(); i++) {
            String itemName = items.get(i).getItemName();
            if (!validItemNames.contains(itemName)) {
                throw exception(OTHER_INCOME_OPTION_INVALID,
                        "第 " + (i + 1) + " 条明细的项目名称", itemName);
            }
        }
    }

    private void validateRequiredItemNames(List<ErpReceivableOtherIncomeSaveReqVO.Item> items) {
        Set<String> validItemNames = getEnabledOptionNames(OTHER_INCOME_ITEM_PROJECT);
        for (int i = 0; i < items.size(); i++) {
            String itemName = items.get(i).getItemName();
            if (!StringUtils.hasText(itemName) || !validItemNames.contains(itemName)) {
                throw exception(OTHER_INCOME_OPTION_INVALID,
                        "第 " + (i + 1) + " 条明细的项目名称", itemName);
            }
        }
    }

    private void validateDraftItemNames(List<ErpReceivableOtherIncomeDraftSaveReqVO.Item> items) {
        Set<String> validItemNames = getEnabledOptionNames(OTHER_INCOME_ITEM_PROJECT);
        for (int i = 0; i < items.size(); i++) {
            String itemName = items.get(i).getItemName();
            if (StringUtils.hasText(itemName) && !validItemNames.contains(itemName)) {
                throw exception(OTHER_INCOME_OPTION_INVALID,
                        "第 " + (i + 1) + " 条明细的项目名称", itemName);
            }
        }
    }

    private void validateRequiredOption(String label, String type, String value) {
        if (!StringUtils.hasText(value) || !getEnabledOptionNames(type).contains(value)) {
            throw exception(OTHER_INCOME_OPTION_INVALID, label, value);
        }
    }

    private void validateOptionalOption(String label, String type, String value) {
        if (StringUtils.hasText(value) && !getEnabledOptionNames(type).contains(value)) {
            throw exception(OTHER_INCOME_OPTION_INVALID, label, value);
        }
    }

    private Set<String> getEnabledOptionNames(String type) {
        List<ErpBaseDataDO> options = baseDataService.getBaseDataSimpleListByType(type);
        if (options == null) {
            return Collections.emptySet();
        }
        return options.stream()
                .map(ErpBaseDataDO::getName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private void validateSaveItemRefs(ErpReceivableOtherIncomeSaveReqVO.Item item) {
        validateRefs(null, item.getHandlerId(), item.getDeptId());
        if (item.getCustomerId() != null) {
            customerService.validateCustomer(item.getCustomerId());
        }
    }

    private void validateDraftItemRefs(ErpReceivableOtherIncomeDraftSaveReqVO.Item item) {
        validateRefs(null, item.getHandlerId(), item.getDeptId());
        if (item.getCustomerId() != null) {
            customerService.validateCustomer(item.getCustomerId());
        }
    }

    private void validateDbItemRefs(ErpReceivableOtherIncomeItemDO item) {
        validateRefs(null, item.getHandlerId(), item.getDeptId());
        if (item.getCustomerId() != null) {
            customerService.validateCustomer(item.getCustomerId());
        }
    }
}
