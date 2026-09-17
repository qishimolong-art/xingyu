package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpensePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableExpenseItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableExpenseMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPayableExpenseStatusEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseDataService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceItemOperationHelper;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpSaleCartFreightDraftCreateReqBO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_DEPT_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_DRAFT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_ITEM_OPERATION_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_ITEM_UPDATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_OPTION_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_UPDATE_FAIL_STATUS_CHANGED;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PAYABLE_EXPENSE_TYPE;

@Service
@Validated
public class ErpPayableExpenseServiceImpl implements ErpPayableExpenseService {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_payable_expense";
    private static final String SALE_CART_SOURCE_TYPE = "销售手推车";
    private static final String SETTLE_METHOD_TYPE = "settle_method";
    private static final String PAYABLE_EXPENSE_BIZ_TYPE = "payable_expense_biz_type";
    private static final String PAYABLE_EXPENSE_TYPE = "payable_expense_type";
    private static final String PAYABLE_EXPENSE_DOC_TYPE = "payable_expense_doc_type";
    private static final String PAYABLE_EXPENSE_ITEM_PROJECT = "payable_expense_item_project";
    private static final String DEFAULT_EXPENSE_TYPE = "其他";
    private static final String SALE_CART_FREIGHT_ITEM_PROJECT = "销售产生运费";

    @Resource
    private ErpPayableExpenseMapper payableExpenseMapper;
    @Resource
    private ErpPayableExpenseItemMapper payableExpenseItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpBaseDataService baseDataService;
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
    public Long createPayableExpense(ErpPayableExpenseSaveReqVO createReqVO) {
        fillDefaultDeptId(createReqVO);
        validateRequiredItems(createReqVO.getItems());
        validateFormalDeptId(createReqVO.getDeptId());
        validateSaveOptions(createReqVO);
        validateRefs(createReqVO.getAccountId(), createReqVO.getHandlerId(), createReqVO.getDeptId());
        validateItemRefs(createReqVO.getItems());
        String no = noRedisDAO.generate("FYZF");
        if (payableExpenseMapper.selectByNo(no) != null) {
            throw exception(PAYABLE_EXPENSE_NO_EXISTS);
        }
        ErpPayableExpenseDO db = BeanUtils.toBean(createReqVO, ErpPayableExpenseDO.class, o -> o
                .setNo(no)
                .setStatus(ErpAuditStatus.PROCESS.getStatus()));
        normalizeMain(db);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, db);
        db.setTotalAmount(sumAmount(createReqVO.getItems()));
        payableExpenseMapper.insert(db);
        List<ErpPayableExpenseItemDO> expenseItems = BeanUtils.toBean(createReqVO.getItems(),
                ErpPayableExpenseItemDO.class, item -> {
                    item.setId(null);
                    item.setExpenseId(db.getId());
                    normalizeItem(item);
                });
        fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, expenseItems);
        payableExpenseItemMapper.insertBatch(expenseItems);
        operateLogService.recordCreate(ERP_PAYABLE_EXPENSE_TYPE, db.getId(), db.getNo());
        return db.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayableExpenseDraft(ErpPayableExpenseDraftSaveReqVO createReqVO) {
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, createReqVO.getItems());
        fillDraftDefaultDeptId(createReqVO);
        List<ErpPayableExpenseItemDO> expenseItems = buildDraftItems(createReqVO.getItems());
        if (CollUtil.isEmpty(expenseItems)) {
            throw exception(PAYABLE_EXPENSE_DRAFT_ITEMS_REQUIRED);
        }
        validateDraftOptions(createReqVO, BeanUtils.toBean(expenseItems, ErpPayableExpenseSaveReqVO.Item.class));
        String no = noRedisDAO.generate("FYZF");
        if (payableExpenseMapper.selectByNo(no) != null) {
            throw exception(PAYABLE_EXPENSE_NO_EXISTS);
        }
        ErpPayableExpenseDO db = BeanUtils.toBean(createReqVO, ErpPayableExpenseDO.class)
                .setId(null)
                .setNo(no)
                .setStatus(ErpPayableExpenseStatusEnum.DRAFT.getStatus())
                .setTotalAmount(sumItemAmount(expenseItems));
        normalizeMain(db);
        payableExpenseMapper.insert(db);
        replaceItems(db.getId(), expenseItems);
        operateLogService.recordCreate(ERP_PAYABLE_EXPENSE_TYPE, db.getId(), db.getNo());
        return db.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAndSubmitPayableExpense(ErpPayableExpenseSaveReqVO createReqVO) {
        return createPayableExpense(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromSaleCartFreight(ErpSaleCartFreightDraftCreateReqBO createReqBO) {
        ErpPayableExpenseDO existing = payableExpenseMapper.selectBySource(
                SALE_CART_SOURCE_TYPE, createReqBO.getCartId());
        if (existing != null) {
            return existing.getId();
        }
        validateRequiredOption("结算方式", SETTLE_METHOD_TYPE, createReqBO.getSettleMethod());
        validateRequiredOption("支出类型", PAYABLE_EXPENSE_TYPE, DEFAULT_EXPENSE_TYPE);
        validateRequiredOption("第 1 条明细的项目名称", PAYABLE_EXPENSE_ITEM_PROJECT, SALE_CART_FREIGHT_ITEM_PROJECT);
        validateRefs(createReqBO.getAccountId(), createReqBO.getHandlerId(), createReqBO.getDeptId());
        String no = noRedisDAO.generate("FYZF");
        if (payableExpenseMapper.selectByNo(no) != null) {
            throw exception(PAYABLE_EXPENSE_NO_EXISTS);
        }
        ErpPayableExpenseDO db = new ErpPayableExpenseDO()
                .setNo(no)
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(createReqBO.getBizTime())
                .setSettleMethod(createReqBO.getSettleMethod())
                .setAccountId(createReqBO.getAccountId())
                .setExpenseType(DEFAULT_EXPENSE_TYPE)
                .setTotalAmount(createReqBO.getAmount())
                .setDeptId(createReqBO.getDeptId())
                .setHandlerId(createReqBO.getHandlerId())
                .setParty(createReqBO.getParty())
                .setRelatedBiz("销售手推车：" + createReqBO.getCartNo())
                .setSourceType(SALE_CART_SOURCE_TYPE)
                .setSourceId(createReqBO.getCartId())
                .setSourceNo(createReqBO.getCartNo())
                .setRemark("销售手推车终审自动生成，来源单号：" + createReqBO.getCartNo());
        normalizeMain(db);
        payableExpenseMapper.insert(db);
        ErpPayableExpenseItemDO item = new ErpPayableExpenseItemDO()
                .setExpenseId(db.getId())
                .setItemName(SALE_CART_FREIGHT_ITEM_PROJECT)
                .setAmount(createReqBO.getAmount())
                .setParty(createReqBO.getParty())
                .setDeptId(createReqBO.getDeptId())
                .setBizDate(createReqBO.getBizTime())
                .setHandlerId(createReqBO.getHandlerId())
                .setQty(1)
                .setExpenseCategory("运费")
                .setRemark("销售手推车：" + createReqBO.getCartNo());
        normalizeItem(item);
        payableExpenseItemMapper.insertBatch(Collections.singletonList(item));
        operateLogService.recordCreate(ERP_PAYABLE_EXPENSE_TYPE, db.getId(), db.getNo());
        return db.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableExpense(ErpPayableExpenseSaveReqVO updateReqVO) {
        ErpPayableExpenseDO db = validateExists(updateReqVO.getId());
        if (ErpPayableExpenseStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_EXPENSE_DRAFT_UPDATE_FAIL, db.getNo());
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_EXPENSE_UPDATE_FAIL_APPROVE, db.getNo());
        }
        List<ErpPayableExpenseItemDO> oldItems =
                payableExpenseItemMapper.selectListByExpenseIdForUpdate(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        if (fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "items")) {
            updateReqVO.setItems(BeanUtils.toBean(oldItems, ErpPayableExpenseSaveReqVO.Item.class));
        } else {
            fieldPermissionMasker.preserveOrClearHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldItems);
        }
        boolean incrementalItems = ErpFinanceItemOperationHelper.useIncrementalItems(
                updateReqVO.getItems(), ErpPayableExpenseSaveReqVO.Item::getOperation,
                PAYABLE_EXPENSE_ITEM_OPERATION_INVALID);
        ErpFinanceItemOperationHelper.RequestChangeSet<ErpPayableExpenseSaveReqVO.Item> itemChangeSet = null;
        List<ErpPayableExpenseSaveReqVO.Item> finalReqItems = updateReqVO.getItems();
        if (incrementalItems) {
            itemChangeSet = ErpFinanceItemOperationHelper.buildRequestChangeSet(
                    updateReqVO.getItems(), oldItems, ErpPayableExpenseSaveReqVO.Item.class,
                    ErpPayableExpenseSaveReqVO.Item::getId, ErpPayableExpenseSaveReqVO.Item::setId,
                    ErpPayableExpenseSaveReqVO.Item::getOperation, ErpPayableExpenseItemDO::getId,
                    PAYABLE_EXPENSE_ITEM_OPERATION_INVALID, PAYABLE_EXPENSE_ITEM_UPDATE_NOT_EXISTS);
            finalReqItems = itemChangeSet.getFinalItems();
        }
        fillDefaultItemDeptId(finalReqItems);
        updateReqVO.setItems(finalReqItems);
        if (updateReqVO.getDeptId() == null) {
            updateReqVO.setDeptId(db.getDeptId());
        }
        validateFormalDeptId(updateReqVO.getDeptId());
        validateSaveOptions(updateReqVO);
        validateRefs(updateReqVO.getAccountId(), updateReqVO.getHandlerId(), updateReqVO.getDeptId());
        validateItemRefs(finalReqItems);
        ErpPayableExpenseDO updateObj = BeanUtils.toBean(updateReqVO, ErpPayableExpenseDO.class);
        if (db.getSourceId() != null) {
            updateObj.setSourceType(db.getSourceType());
            updateObj.setSourceId(db.getSourceId());
            updateObj.setSourceNo(db.getSourceNo());
        }
        normalizeMain(updateObj);
        updateObj.setTotalAmount(sumAmount(finalReqItems));
        if (payableExpenseMapper.updateByIdAndStatus(updateReqVO.getId(),
                ErpAuditStatus.PROCESS.getStatus(), updateObj) == 0) {
            throw exception(PAYABLE_EXPENSE_UPDATE_FAIL_STATUS_CHANGED);
        }
        List<ErpPayableExpenseItemDO> finalItems = BeanUtils.toBean(finalReqItems,
                ErpPayableExpenseItemDO.class, this::normalizeItem);
        if (incrementalItems) {
            applyPayableExpenseItemChangeSet(updateReqVO.getId(), finalItems, itemChangeSet);
        } else {
            payableExpenseItemMapper.deleteByIds(convertList(oldItems, ErpPayableExpenseItemDO::getId));
            payableExpenseItemMapper.insertBatch(BeanUtils.toBean(finalReqItems,
                    ErpPayableExpenseItemDO.class, item -> {
                        item.setId(null);
                        item.setExpenseId(updateReqVO.getId());
                        normalizeItem(item);
                    }));
        }
        operateLogService.recordUpdate(ERP_PAYABLE_EXPENSE_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableExpenseDraft(ErpPayableExpenseDraftSaveReqVO updateReqVO) {
        ErpPayableExpenseDO db = validateExists(updateReqVO.getId());
        if (!ErpPayableExpenseStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_EXPENSE_DRAFT_UPDATE_FAIL, db.getNo());
        }
        List<ErpPayableExpenseItemDO> oldItems =
                payableExpenseItemMapper.selectListByExpenseIdForUpdate(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        if (fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "items")) {
            updateReqVO.setItems(BeanUtils.toBean(oldItems, ErpPayableExpenseSaveReqVO.Item.class));
        } else {
            if (updateReqVO.getItems() == null) {
                updateReqVO.setItems(Collections.emptyList());
            }
            fieldPermissionMasker.preserveHiddenItemFields(
                    FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldItems);
        }
        boolean incrementalItems = ErpFinanceItemOperationHelper.useIncrementalItems(
                updateReqVO.getItems(), ErpPayableExpenseSaveReqVO.Item::getOperation,
                PAYABLE_EXPENSE_ITEM_OPERATION_INVALID);
        ErpFinanceItemOperationHelper.RequestChangeSet<ErpPayableExpenseSaveReqVO.Item> itemChangeSet = null;
        List<ErpPayableExpenseSaveReqVO.Item> finalReqItems = updateReqVO.getItems();
        if (incrementalItems) {
            itemChangeSet = ErpFinanceItemOperationHelper.buildRequestChangeSet(
                    updateReqVO.getItems(), oldItems, ErpPayableExpenseSaveReqVO.Item.class,
                    ErpPayableExpenseSaveReqVO.Item::getId, ErpPayableExpenseSaveReqVO.Item::setId,
                    ErpPayableExpenseSaveReqVO.Item::getOperation, ErpPayableExpenseItemDO::getId,
                    PAYABLE_EXPENSE_ITEM_OPERATION_INVALID, PAYABLE_EXPENSE_ITEM_UPDATE_NOT_EXISTS);
            finalReqItems = itemChangeSet.getFinalItems();
        }
        List<ErpPayableExpenseItemDO> expenseItems = buildDraftItems(finalReqItems, incrementalItems);
        validateDraftOptions(updateReqVO, BeanUtils.toBean(expenseItems, ErpPayableExpenseSaveReqVO.Item.class));
        ErpPayableExpenseDO updateObj = BeanUtils.toBean(updateReqVO, ErpPayableExpenseDO.class)
                .setId(db.getId())
                .setNo(db.getNo())
                .setStatus(db.getStatus())
                .setDeptId(updateReqVO.getDeptId() != null ? updateReqVO.getDeptId() : db.getDeptId())
                .setSourceType(db.getSourceType())
                .setSourceId(db.getSourceId())
                .setSourceNo(db.getSourceNo())
                .setTotalAmount(sumItemAmount(expenseItems));
        normalizeMain(updateObj);
        if (payableExpenseMapper.updateByIdAndStatus(db.getId(),
                ErpPayableExpenseStatusEnum.DRAFT.getStatus(), updateObj) == 0) {
            throw exception(PAYABLE_EXPENSE_DRAFT_UPDATE_FAIL, db.getNo());
        }
        if (incrementalItems) {
            applyPayableExpenseItemChangeSet(db.getId(), expenseItems, itemChangeSet);
        } else {
            replaceItems(db.getId(), expenseItems);
        }
        operateLogService.recordUpdate(ERP_PAYABLE_EXPENSE_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitPayableExpense(ErpPayableExpenseSaveReqVO updateReqVO) {
        updatePayableExpenseDraft(BeanUtils.toBean(updateReqVO, ErpPayableExpenseDraftSaveReqVO.class));
        submitPayableExpense(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPayableExpense(Long id) {
        ErpPayableExpenseDO db = payableExpenseMapper.selectByIdForUpdate(id);
        if (db == null) {
            throw exception(PAYABLE_EXPENSE_NOT_EXISTS);
        }
        if (!ErpPayableExpenseStatusEnum.DRAFT.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL, "当前状态不是草稿");
        }
        validateDraftForSubmit(db);
        List<ErpPayableExpenseItemDO> items = payableExpenseItemMapper.selectListByExpenseId(id);
        if (CollUtil.isEmpty(items)) {
            throw exception(PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL, "请至少添加一条费用明细");
        }
        List<ErpPayableExpenseSaveReqVO.Item> itemReqs =
                BeanUtils.toBean(items, ErpPayableExpenseSaveReqVO.Item.class);
        for (ErpPayableExpenseSaveReqVO.Item item : itemReqs) {
            if (!StringUtils.hasText(item.getItemName()) || item.getAmount() == null) {
                throw exception(PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL, "费用项目和金额不能为空");
            }
        }
        validateSubmitOptions(db, items);
        validateItemRefs(itemReqs);
        BigDecimal totalAmount = sumAmount(itemReqs);
        ErpPayableExpenseDO statusUpdate = new ErpPayableExpenseDO()
                .setStatus(ErpPayableExpenseStatusEnum.PROCESS.getStatus())
                .setTotalAmount(totalAmount);
        if (payableExpenseMapper.updateByIdAndStatus(id,
                ErpPayableExpenseStatusEnum.DRAFT.getStatus(), statusUpdate) == 0) {
            throw exception(PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL, "状态已变化，请刷新后重试");
        }
        operateLogService.recordUpdate(ERP_PAYABLE_EXPENSE_TYPE, id, db.getNo());
    }

    @Override
    public void updatePayableExpenseRemark(ErpFinanceUpdateRemarkReqVO updateReqVO) {
        ErpPayableExpenseDO db = validateExists(updateReqVO.getId());
        payableExpenseMapper.updateById(new ErpPayableExpenseDO()
                .setId(updateReqVO.getId()).setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_PAYABLE_EXPENSE_TYPE, db.getId(), db.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayableExpenseStatus(Long id, Integer status) {
        if (!ErpPayableExpenseStatusEnum.APPROVE.getStatus().equals(status)) {
            throw exception(PAYABLE_EXPENSE_PROCESS_FAIL);
        }
        ErpPayableExpenseDO db = validateExists(id);
        if (!ErpPayableExpenseStatusEnum.PROCESS.getStatus().equals(db.getStatus())) {
            throw exception(PAYABLE_EXPENSE_APPROVE_FAIL);
        }
        if (payableExpenseMapper.updateByIdAndStatus(id, db.getStatus(),
                ErpPayableExpenseDO.builder().status(status).build()) == 0) {
            throw exception(PAYABLE_EXPENSE_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_PAYABLE_EXPENSE_TYPE, id, db.getNo(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePayableExpense(List<Long> ids) {
        List<ErpPayableExpenseDO> list = payableExpenseMapper.selectByIds(ids);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list.forEach(item -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(item.getStatus())) {
                throw exception(PAYABLE_EXPENSE_DELETE_FAIL_APPROVE, item.getNo());
            }
        });
        payableExpenseMapper.deleteByIds(ids);
        ids.forEach(id -> payableExpenseItemMapper.deleteByIds(
                convertList(payableExpenseItemMapper.selectListByExpenseId(id), ErpPayableExpenseItemDO::getId)));
        list.forEach(item -> operateLogService.recordDelete(ERP_PAYABLE_EXPENSE_TYPE, item.getId(), item.getNo()));
    }

    @Override
    public ErpPayableExpenseDO getPayableExpense(Long id) {
        return payableExpenseMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPayableExpenseDO> getPayableExpensePage(ErpPayableExpensePageReqVO pageReqVO) {
        if (hasItemFilter(pageReqVO)) {
            List<Long> expenseIds = convertList(
                    payableExpenseItemMapper.selectListByItemNameOrInvoiceNo(pageReqVO.getItemName(), pageReqVO.getInvoiceNo()),
                    ErpPayableExpenseItemDO::getExpenseId);
            if (CollUtil.isEmpty(expenseIds)) {
                return PageResult.empty();
            }
            pageReqVO.setIds(expenseIds);
        }
        return payableExpenseMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPayableExpenseItemDO> getPayableExpenseItemListByExpenseId(Long expenseId) {
        return payableExpenseItemMapper.selectListByExpenseId(expenseId);
    }

    @Override
    public PageResult<ErpPayableExpenseItemDO> getPayableExpenseItemPage(ErpPayableExpenseItemPageReqVO pageReqVO) {
        validateExists(pageReqVO.getExpenseId());
        return payableExpenseItemMapper.selectPageByExpenseId(pageReqVO);
    }

    @Override
    public List<ErpPayableExpenseItemDO> getPayableExpenseItemListByExpenseIds(Collection<Long> expenseIds) {
        if (CollUtil.isEmpty(expenseIds)) {
            return Collections.emptyList();
        }
        return payableExpenseItemMapper.selectListByExpenseIds(expenseIds);
    }

    private void validateRefs(Long accountId, Long handlerId, Long deptId) {
        if (accountId != null) {
            accountService.validateAccount(accountId);
        }
        if (handlerId != null) {
            adminUserApi.validateUser(handlerId);
        }
        if (deptId != null && deptApi.getDept(deptId) == null) {
            throw exception(PAYABLE_EXPENSE_NOT_EXISTS);
        }
    }

    private void validateFormalDeptId(Long deptId) {
        if (deptId == null) {
            throw exception(PAYABLE_EXPENSE_DEPT_REQUIRED);
        }
    }

    private void validateItemRefs(List<ErpPayableExpenseSaveReqVO.Item> items) {
        items.forEach(item -> {
            if (item.getHandlerId() != null) {
                adminUserApi.validateUser(item.getHandlerId());
            }
            if (item.getDeptId() != null && deptApi.getDept(item.getDeptId()) == null) {
                throw exception(PAYABLE_EXPENSE_NOT_EXISTS);
            }
        });
    }

    private void fillDefaultDeptId(ErpPayableExpenseSaveReqVO reqVO) {
        Long deptId = getLoginUserDeptId();
        if (deptId == null) {
            return;
        }
        if (reqVO.getDeptId() == null) {
            reqVO.setDeptId(deptId);
        }
        fillDefaultItemDeptId(reqVO.getItems(), deptId);
    }

    private void fillDefaultItemDeptId(List<ErpPayableExpenseSaveReqVO.Item> items) {
        Long deptId = getLoginUserDeptId();
        if (deptId != null) {
            fillDefaultItemDeptId(items, deptId);
        }
    }

    private void fillDefaultItemDeptId(List<ErpPayableExpenseSaveReqVO.Item> items, Long deptId) {
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

    private ErpPayableExpenseDO validateExists(Long id) {
        ErpPayableExpenseDO db = payableExpenseMapper.selectById(id);
        if (db == null) {
            throw exception(PAYABLE_EXPENSE_NOT_EXISTS);
        }
        return db;
    }

    private BigDecimal sumAmount(List<ErpPayableExpenseSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(ErpPayableExpenseSaveReqVO.Item::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ErpPayableExpenseItemDO> buildDraftItems(
            List<ErpPayableExpenseSaveReqVO.Item> items) {
        return buildDraftItems(items, false);
    }

    private List<ErpPayableExpenseItemDO> buildDraftItems(
            List<ErpPayableExpenseSaveReqVO.Item> items, boolean preserveId) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        List<ErpPayableExpenseItemDO> result = new ArrayList<>();
        for (ErpPayableExpenseSaveReqVO.Item source : items) {
            if (source == null || !StringUtils.hasText(source.getItemName()) || source.getAmount() == null) {
                continue;
            }
            ErpPayableExpenseItemDO item = BeanUtils.toBean(source, ErpPayableExpenseItemDO.class);
            if (!preserveId) {
                item.setId(null);
            }
            normalizeItem(item);
            result.add(item);
        }
        return result;
    }

    private void replaceItems(Long expenseId, List<ErpPayableExpenseItemDO> items) {
        payableExpenseItemMapper.deleteByExpenseId(expenseId);
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> item.setId(null).setExpenseId(expenseId));
        payableExpenseItemMapper.insertBatch(items);
    }

    private void applyPayableExpenseItemChangeSet(Long expenseId, List<ErpPayableExpenseItemDO> finalItems,
            ErpFinanceItemOperationHelper.RequestChangeSet<ErpPayableExpenseSaveReqVO.Item> itemChangeSet) {
        if (itemChangeSet == null) {
            return;
        }
        if (CollUtil.isNotEmpty(itemChangeSet.getDeleteIds())) {
            payableExpenseItemMapper.deleteByIds(itemChangeSet.getDeleteIds());
        }
        List<ErpPayableExpenseItemDO> insertList = finalItems.stream()
                .filter(item -> item.getId() == null)
                .peek(item -> item.setExpenseId(expenseId))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(insertList)) {
            payableExpenseItemMapper.insertBatch(insertList);
        }
        List<ErpPayableExpenseItemDO> updateList = finalItems.stream()
                .filter(item -> item.getId() != null && itemChangeSet.getUpdateIds().contains(item.getId()))
                .peek(item -> item.setExpenseId(expenseId))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(updateList)) {
            payableExpenseItemMapper.updateBatch(updateList);
        }
    }

    private BigDecimal sumItemAmount(List<ErpPayableExpenseItemDO> items) {
        return items.stream()
                .map(ErpPayableExpenseItemDO::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void fillDraftDefaultDeptId(ErpPayableExpenseDraftSaveReqVO reqVO) {
        Long deptId = getLoginUserDeptId();
        if (deptId == null) {
            return;
        }
        if (reqVO.getDeptId() == null) {
            reqVO.setDeptId(deptId);
        }
        fillDefaultItemDeptId(reqVO.getItems(), deptId);
    }

    private void validateDraftForSubmit(ErpPayableExpenseDO db) {
        if (db.getBizTime() == null) {
            throw exception(PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL, "单据日期不能为空");
        }
        if (!StringUtils.hasText(db.getSettleMethod())) {
            throw exception(PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL, "结算方式不能为空");
        }
        if (db.getAccountId() == null) {
            throw exception(PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL, "结算账户不能为空");
        }
        if (!StringUtils.hasText(db.getExpenseType())) {
            throw exception(PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL, "支出类型不能为空");
        }
        if (db.getDeptId() == null) {
            throw exception(PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL, "开单部门不能为空");
        }
        if (db.getHandlerId() == null) {
            throw exception(PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL, "经手人不能为空");
        }
        validateRefs(db.getAccountId(), db.getHandlerId(), db.getDeptId());
    }

    private void normalizeMain(ErpPayableExpenseDO db) {
        db.setSettleMethod(StrUtil.blankToDefault(db.getSettleMethod(), ""));
        db.setVoucherNo(StrUtil.blankToDefault(db.getVoucherNo(), ""));
        db.setExpenseBizType(StrUtil.blankToDefault(db.getExpenseBizType(), ""));
        db.setExpenseType(StrUtil.blankToDefault(db.getExpenseType(), ""));
        db.setDocType(StrUtil.blankToDefault(db.getDocType(), "正常单据"));
    }

    private void normalizeItem(ErpPayableExpenseItemDO item) {
        item.setQty(item.getQty() == null || item.getQty() <= 0 ? 1 : item.getQty());
        item.setAmount(item.getAmount() == null ? BigDecimal.ZERO : item.getAmount());
    }

    private boolean hasItemFilter(ErpPayableExpensePageReqVO pageReqVO) {
        return StringUtils.hasText(pageReqVO.getItemName()) || StringUtils.hasText(pageReqVO.getInvoiceNo());
    }

    private void validateSaveOptions(ErpPayableExpenseSaveReqVO reqVO) {
        validateRequiredOption("结算方式", SETTLE_METHOD_TYPE, reqVO.getSettleMethod());
        validateOptionalOption("类型", PAYABLE_EXPENSE_BIZ_TYPE, reqVO.getExpenseBizType());
        validateRequiredOption("支出类型", PAYABLE_EXPENSE_TYPE, reqVO.getExpenseType());
        validateOptionalOption("单据类型", PAYABLE_EXPENSE_DOC_TYPE, reqVO.getDocType());
        validateRequiredItemNames(reqVO.getItems());
    }

    private void validateDraftOptions(ErpPayableExpenseDraftSaveReqVO reqVO,
                                      List<ErpPayableExpenseSaveReqVO.Item> items) {
        validateOptionalOption("结算方式", SETTLE_METHOD_TYPE, reqVO.getSettleMethod());
        validateOptionalOption("类型", PAYABLE_EXPENSE_BIZ_TYPE, reqVO.getExpenseBizType());
        validateOptionalOption("支出类型", PAYABLE_EXPENSE_TYPE, reqVO.getExpenseType());
        validateOptionalOption("单据类型", PAYABLE_EXPENSE_DOC_TYPE, reqVO.getDocType());
        validateDraftItemNames(items);
    }

    private void validateSubmitOptions(ErpPayableExpenseDO db,
                                       List<ErpPayableExpenseItemDO> items) {
        validateRequiredOption("结算方式", SETTLE_METHOD_TYPE, db.getSettleMethod());
        validateOptionalOption("类型", PAYABLE_EXPENSE_BIZ_TYPE, db.getExpenseBizType());
        validateRequiredOption("支出类型", PAYABLE_EXPENSE_TYPE, db.getExpenseType());
        validateOptionalOption("单据类型", PAYABLE_EXPENSE_DOC_TYPE, db.getDocType());
        Set<String> validItemNames = getEnabledOptionNames(PAYABLE_EXPENSE_ITEM_PROJECT);
        for (int i = 0; i < items.size(); i++) {
            String itemName = items.get(i).getItemName();
            if (!validItemNames.contains(itemName)) {
                throw exception(PAYABLE_EXPENSE_OPTION_INVALID,
                        "第 " + (i + 1) + " 条明细的项目名称", itemName);
            }
        }
    }

    private void validateRequiredItemNames(List<ErpPayableExpenseSaveReqVO.Item> items) {
        validateRequiredItems(items);
        Set<String> validItemNames = getEnabledOptionNames(PAYABLE_EXPENSE_ITEM_PROJECT);
        for (int i = 0; i < items.size(); i++) {
            String itemName = items.get(i).getItemName();
            if (!StringUtils.hasText(itemName) || !validItemNames.contains(itemName)) {
                throw exception(PAYABLE_EXPENSE_OPTION_INVALID,
                        "第 " + (i + 1) + " 条明细的项目名称", itemName);
            }
        }
    }

    private void validateRequiredItems(List<ErpPayableExpenseSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            throw exception(PAYABLE_EXPENSE_OPTION_INVALID, "费用明细", "不能为空");
        }
    }

    private void validateDraftItemNames(List<ErpPayableExpenseSaveReqVO.Item> items) {
        Set<String> validItemNames = getEnabledOptionNames(PAYABLE_EXPENSE_ITEM_PROJECT);
        for (int i = 0; i < items.size(); i++) {
            String itemName = items.get(i).getItemName();
            if (StringUtils.hasText(itemName) && !validItemNames.contains(itemName)) {
                throw exception(PAYABLE_EXPENSE_OPTION_INVALID,
                        "第 " + (i + 1) + " 条明细的项目名称", itemName);
            }
        }
    }

    private void validateRequiredOption(String label, String type, String value) {
        if (!StringUtils.hasText(value) || !getEnabledOptionNames(type).contains(value)) {
            throw exception(PAYABLE_EXPENSE_OPTION_INVALID, label, value);
        }
    }

    private void validateOptionalOption(String label, String type, String value) {
        if (StringUtils.hasText(value) && !getEnabledOptionNames(type).contains(value)) {
            throw exception(PAYABLE_EXPENSE_OPTION_INVALID, label, value);
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

}
