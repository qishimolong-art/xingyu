package cn.iocoder.yudao.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentFormCandidateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentFormCandidateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentWriteOffCandidateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentWriteOffReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentWriteOffReverseReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinancePaymentStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.common.ErpOriginalSettlementAmountUtils;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableOtherService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchasePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.*;
import static cn.iocoder.yudao.module.erp.service.common.ErpFinanceAmountUtils.normalize;

// TODO 芋艿：记录操作日志

/**
 * ERP 付款单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpFinancePaymentServiceImpl implements ErpFinancePaymentService {

    private static final String FIELD_PERMISSION_MODULE = "erp_finance_payment";

    @Resource
    private ErpFinancePaymentMapper financePaymentMapper;
    @Resource
    private ErpFinancePaymentItemMapper financePaymentItemMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpPurchasePriceAdjustMapper purchasePriceAdjustMapper;
    @Resource
    private ErpPayableMiscMapper payableMiscMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    @Lazy
    private ErpPurchaseInService purchaseInService;
    @Resource
    private ErpPurchaseReturnService purchaseReturnService;
    @Resource
    private ErpPurchasePriceAdjustService purchasePriceAdjustService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpFinancePermissionFieldFiller permissionFieldFiller;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpFinanceAutoWriteOffService financeAutoWriteOffService;
    @Resource
    private ErpPayableOtherService payableOtherService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinancePayment(ErpFinancePaymentSaveReqVO createReqVO) {
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, createReqVO.getItems());
        // 1.1 校验订单项的有效性
        List<ErpFinancePaymentItemDO> paymentItems = validateFinancePaymentItems(
                createReqVO.getSupplierId(), createReqVO.getItems());
        // 1.2 校验供应商
        supplierService.validateSupplier(createReqVO.getSupplierId());
        // 1.3 校验结算账户
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1.4 校验财务人员
        if (createReqVO.getFinanceUserId() != null) {
            adminUserApi.validateUser(createReqVO.getFinanceUserId());
        }
        // 1.5 生成付款单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.FINANCE_PAYMENT_NO_PREFIX);
        if (financePaymentMapper.selectByNo(no) != null) {
            throw exception(FINANCE_PAYMENT_NO_EXISTS);
        }

        // 2.1 插入付款单
        ErpFinancePaymentDO payment = BeanUtils.toBean(createReqVO, ErpFinancePaymentDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        permissionFieldFiller.fillCreateFields(payment);
        fillDefaultAmount(payment);
        validateAndFillSourcePayableMisc(payment, false);
        preparePendingItems(payment, paymentItems);
        financePaymentMapper.insert(payment);
        // 2.2 插入付款单项
        if (CollUtil.isNotEmpty(paymentItems)) {
            paymentItems.forEach(o -> o.setPaymentId(payment.getId()));
            financePaymentItemMapper.insertBatch(paymentItems);
        }

        operateLogService.recordCreate(ERP_FINANCE_PAYMENT_TYPE, payment.getId(), payment.getNo());
        return payment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinancePaymentDraft(ErpFinancePaymentDraftSaveReqVO createReqVO) {
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, createReqVO.getItems());
        List<ErpFinancePaymentItemDO> paymentItems = buildFinancePaymentDraftItems(createReqVO.getItems());
        if (CollUtil.isEmpty(paymentItems) && createReqVO.getSourcePayableMiscId() == null) {
            throw exception(FINANCE_PAYMENT_DRAFT_ITEMS_REQUIRED);
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.FINANCE_PAYMENT_NO_PREFIX);
        if (financePaymentMapper.selectByNo(no) != null) {
            throw exception(FINANCE_PAYMENT_NO_EXISTS);
        }
        ErpFinancePaymentDO payment = BeanUtils.toBean(createReqVO, ErpFinancePaymentDO.class)
                .setId(null)
                .setNo(no)
                .setStatus(ErpFinancePaymentStatusEnum.DRAFT.getStatus())
                .setTotalPrice(createReqVO.getTotalPrice())
                .setDiscountPrice(createReqVO.getDiscountPrice())
                .setPaymentTime(createReqVO.getPaymentTime() != null
                        ? createReqVO.getPaymentTime() : LocalDateTime.now());
        fillDraftAmounts(payment, paymentItems);
        permissionFieldFiller.fillCreateFields(payment);
        validateAndFillSourcePayableMisc(payment, false);
        financePaymentMapper.insert(payment);
        insertFinancePaymentDraftItems(payment.getId(), paymentItems);
        operateLogService.recordCreate(ERP_FINANCE_PAYMENT_TYPE, payment.getId(), payment.getNo());
        return payment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAndSubmitFinancePayment(ErpFinancePaymentSaveReqVO createReqVO) {
        return createFinancePayment(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinancePayment(ErpFinancePaymentSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpFinancePaymentDO payment = validateFinancePaymentExists(updateReqVO.getId());
        if (ErpFinancePaymentStatusEnum.DRAFT.getStatus().equals(payment.getStatus())) {
            throw exception(FINANCE_PAYMENT_DRAFT_UPDATE_FAIL, payment.getNo());
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(payment.getStatus())) {
            throw exception(FINANCE_PAYMENT_UPDATE_FAIL_APPROVE, payment.getNo());
        }
        List<ErpFinancePaymentItemDO> oldPaymentItems =
                financePaymentItemMapper.selectListByPaymentIdForUpdate(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, payment);
        if (fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "items")) {
            updateReqVO.setItems(BeanUtils.toBean(oldPaymentItems, ErpFinancePaymentSaveReqVO.Item.class));
        } else {
            fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldPaymentItems);
        }
        boolean incrementalItems = ErpFinanceItemOperationHelper.useIncrementalItems(
                updateReqVO.getItems(), ErpFinancePaymentSaveReqVO.Item::getOperation,
                FINANCE_PAYMENT_ITEM_OPERATION_INVALID);
        ErpFinanceItemOperationHelper.RequestChangeSet<ErpFinancePaymentSaveReqVO.Item> itemChangeSet = null;
        List<ErpFinancePaymentSaveReqVO.Item> finalReqItems = updateReqVO.getItems();
        if (incrementalItems) {
            itemChangeSet = ErpFinanceItemOperationHelper.buildRequestChangeSet(
                    updateReqVO.getItems(), oldPaymentItems, ErpFinancePaymentSaveReqVO.Item.class,
                    ErpFinancePaymentSaveReqVO.Item::getId, ErpFinancePaymentSaveReqVO.Item::setId,
                    ErpFinancePaymentSaveReqVO.Item::getOperation, ErpFinancePaymentItemDO::getId,
                    FINANCE_PAYMENT_ITEM_OPERATION_INVALID, FINANCE_PAYMENT_ITEM_UPDATE_NOT_EXISTS);
            finalReqItems = itemChangeSet.getFinalItems();
        }
        // 1.2 校验供应商
        supplierService.validateSupplier(updateReqVO.getSupplierId());
        // 1.3 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        // 1.4 校验财务人员
        if (updateReqVO.getFinanceUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getFinanceUserId());
        }
        // 1.5 校验付款单项的有效性
        List<ErpFinancePaymentItemDO> paymentItems = validateFinancePaymentItems(
                updateReqVO.getSupplierId(), finalReqItems);

        // 2.1 更新付款单
        ErpFinancePaymentDO updateObj = BeanUtils.toBean(updateReqVO, ErpFinancePaymentDO.class);
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(payment.getDeptId());
        }
        preserveSourcePayableMisc(updateObj, payment);
        fillDefaultAmount(updateObj);
        validateAndFillSourcePayableMisc(updateObj, false);
        preparePendingItems(updateObj, paymentItems);
        financePaymentMapper.updateById(updateObj);
        // 2.2 更新付款单项
        if (incrementalItems) {
            applyFinancePaymentItemChangeSet(updateReqVO.getId(), paymentItems, itemChangeSet);
        } else {
            updateFinancePaymentItemList(updateReqVO.getId(), paymentItems);
        }
        recordUpdate(payment, updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinancePaymentDraft(ErpFinancePaymentDraftSaveReqVO updateReqVO) {
        ErpFinancePaymentDO payment = validateFinancePaymentExists(updateReqVO.getId());
        if (!ErpFinancePaymentStatusEnum.DRAFT.getStatus().equals(payment.getStatus())) {
            throw exception(FINANCE_PAYMENT_DRAFT_UPDATE_FAIL, payment.getNo());
        }
        List<ErpFinancePaymentItemDO> oldPaymentItems =
                financePaymentItemMapper.selectListByPaymentIdForUpdate(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, payment);
        if (fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "items")) {
            updateReqVO.setItems(BeanUtils.toBean(oldPaymentItems, ErpFinancePaymentSaveReqVO.Item.class));
        } else {
            fieldPermissionMasker.preserveHiddenItemFields(
                    FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldPaymentItems);
        }
        boolean incrementalItems = ErpFinanceItemOperationHelper.useIncrementalItems(
                updateReqVO.getItems(), ErpFinancePaymentSaveReqVO.Item::getOperation,
                FINANCE_PAYMENT_ITEM_OPERATION_INVALID);
        ErpFinanceItemOperationHelper.RequestChangeSet<ErpFinancePaymentSaveReqVO.Item> itemChangeSet = null;
        List<ErpFinancePaymentSaveReqVO.Item> finalReqItems = updateReqVO.getItems();
        if (incrementalItems) {
            itemChangeSet = ErpFinanceItemOperationHelper.buildRequestChangeSet(
                    updateReqVO.getItems(), oldPaymentItems, ErpFinancePaymentSaveReqVO.Item.class,
                    ErpFinancePaymentSaveReqVO.Item::getId, ErpFinancePaymentSaveReqVO.Item::setId,
                    ErpFinancePaymentSaveReqVO.Item::getOperation, ErpFinancePaymentItemDO::getId,
                    FINANCE_PAYMENT_ITEM_OPERATION_INVALID, FINANCE_PAYMENT_ITEM_UPDATE_NOT_EXISTS);
            finalReqItems = itemChangeSet.getFinalItems();
        }
        List<ErpFinancePaymentItemDO> paymentItems =
                buildFinancePaymentDraftItems(finalReqItems, incrementalItems);
        ErpFinancePaymentDO updateObj = BeanUtils.toBean(updateReqVO, ErpFinancePaymentDO.class)
                .setId(payment.getId())
                .setNo(payment.getNo())
                .setStatus(payment.getStatus())
                .setTotalPrice(updateReqVO.getTotalPrice())
                .setDiscountPrice(updateReqVO.getDiscountPrice())
                .setPaymentTime(updateReqVO.getPaymentTime() != null
                        ? updateReqVO.getPaymentTime() : payment.getPaymentTime());
        preserveSourcePayableMisc(updateObj, payment);
        fillDraftAmounts(updateObj, paymentItems);
        validateAndFillSourcePayableMisc(updateObj, false);
        if (financePaymentMapper.updateByIdAndStatus(payment.getId(),
                ErpFinancePaymentStatusEnum.DRAFT.getStatus(), updateObj) == 0) {
            throw exception(FINANCE_PAYMENT_DRAFT_UPDATE_FAIL, payment.getNo());
        }
        if (incrementalItems) {
            applyFinancePaymentItemChangeSet(payment.getId(), paymentItems, itemChangeSet);
        } else {
            financePaymentItemMapper.deleteByPaymentId(payment.getId());
            insertFinancePaymentDraftItems(payment.getId(), paymentItems);
        }
        recordUpdate(payment, updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitFinancePayment(ErpFinancePaymentSaveReqVO updateReqVO) {
        ErpFinancePaymentDraftSaveReqVO draftReqVO =
                BeanUtils.toBean(updateReqVO, ErpFinancePaymentDraftSaveReqVO.class);
        updateFinancePaymentDraft(draftReqVO);
        submitFinancePayment(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFinancePayment(Long id) {
        ErpFinancePaymentDO payment = financePaymentMapper.selectByIdForUpdate(id);
        if (payment == null) {
            throw exception(FINANCE_PAYMENT_NOT_EXISTS);
        }
        if (!ErpFinancePaymentStatusEnum.DRAFT.getStatus().equals(payment.getStatus())) {
            throw exception(FINANCE_PAYMENT_DRAFT_SUBMIT_FAIL, "当前状态不是草稿");
        }
        validateFinancePaymentDraftForSubmit(payment);
        List<ErpFinancePaymentItemDO> paymentItems = validateFinancePaymentItems(
                payment.getSupplierId(),
                BeanUtils.toBean(financePaymentItemMapper.selectListByPaymentId(id),
                        ErpFinancePaymentSaveReqVO.Item.class));
        fillDefaultAmount(payment);
        validateAndFillSourcePayableMisc(payment, false);
        preparePendingItems(payment, paymentItems);
        ErpFinancePaymentDO statusUpdate = new ErpFinancePaymentDO()
                .setStatus(ErpFinancePaymentStatusEnum.PROCESS.getStatus())
                .setTotalPrice(payment.getTotalPrice())
                .setDiscountPrice(payment.getDiscountPrice())
                .setPaymentPrice(payment.getPaymentPrice());
        if (financePaymentMapper.updateByIdAndStatus(id,
                ErpFinancePaymentStatusEnum.DRAFT.getStatus(), statusUpdate) == 0) {
            throw exception(FINANCE_PAYMENT_DRAFT_SUBMIT_FAIL, "状态已变化，请刷新后重试");
        }
        if (CollUtil.isNotEmpty(paymentItems)) {
            paymentItems.forEach(item -> item.setPaymentId(id));
            financePaymentItemMapper.updateBatch(paymentItems);
        }
        operateLogService.recordUpdate(ERP_FINANCE_PAYMENT_TYPE, id, payment.getNo());
    }

    @Override
    public void updateFinancePaymentRemark(ErpFinanceUpdateRemarkReqVO updateReqVO) {
        ErpFinancePaymentDO payment = validateFinancePaymentExists(updateReqVO.getId());
        financePaymentMapper.updateById(new ErpFinancePaymentDO()
                .setId(updateReqVO.getId()).setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_FINANCE_PAYMENT_TYPE, payment.getId(), payment.getNo());
    }

    private void fillDefaultAmount(ErpFinancePaymentDO payment) {
        payment.setDiscountPrice(normalize(getZeroIfNull(payment.getDiscountPrice())));
        BigDecimal totalPrice = normalize(getZeroIfNull(payment.getTotalPrice()));
        if (totalPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_AMOUNT_INVALID, "合计付款不能为 0");
        }
        if (payment.getDiscountPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_AMOUNT_INVALID, "优惠金额不能小于 0");
        }
        BigDecimal paymentPrice = totalPrice.subtract(payment.getDiscountPrice());
        if (paymentPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_AMOUNT_INVALID, "实际付款不能为 0");
        }
        payment.setTotalPrice(totalPrice).setPaymentPrice(normalize(paymentPrice));
    }

    private void preserveSourcePayableMisc(ErpFinancePaymentDO target, ErpFinancePaymentDO source) {
        target.setSourcePayableMiscId(source.getSourcePayableMiscId())
                .setSourcePayableMiscNo(source.getSourcePayableMiscNo());
    }

    private ErpPayableMiscDO validateAndFillSourcePayableMisc(ErpFinancePaymentDO payment, boolean forUpdate) {
        if (payment.getSourcePayableMiscId() == null) {
            payment.setSourcePayableMiscNo(null);
            return null;
        }
        ErpPayableMiscDO sourceMisc = forUpdate
                ? payableMiscMapper.selectByIdForUpdate(payment.getSourcePayableMiscId())
                : payableMiscMapper.selectById(payment.getSourcePayableMiscId());
        if (sourceMisc == null || !ErpAuditStatus.APPROVE.getStatus().equals(sourceMisc.getStatus())) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "来源其他应付单不存在或未审核");
        }
        if (isPayableMiscOffset(sourceMisc)) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "系统生成的其他应付冲减单不能转付款");
        }
        if (!Objects.equals(sourceMisc.getSupplierId(), payment.getSupplierId())) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "供应商必须相同");
        }
        payment.setSourcePayableMiscNo(sourceMisc.getNo());
        return sourceMisc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveFinancePayment(Long id) {
        ErpFinancePaymentDO payment = financePaymentMapper.selectByIdForUpdate(id);
        if (payment == null) {
            throw exception(FINANCE_PAYMENT_NOT_EXISTS);
        }
        if (!ErpFinancePaymentStatusEnum.PROCESS.getStatus().equals(payment.getStatus())) {
            throw exception(FINANCE_PAYMENT_APPROVE_FAIL);
        }
        List<ErpFinancePaymentItemDO> pendingItems = financePaymentItemMapper.selectListByPaymentId(id).stream()
                .filter(item -> item.getWriteOffStatus() == null
                        || ErpFinanceWriteOffStatusEnum.PENDING.getStatus().equals(item.getWriteOffStatus()))
                .collect(Collectors.toList());
        validateAndFillEffectiveItems(payment, pendingItems);
        int updateCount = financePaymentMapper.updateByIdAndStatus(id, payment.getStatus(),
                new ErpFinancePaymentDO().setStatus(ErpAuditStatus.APPROVE.getStatus()));
        if (updateCount == 0) {
            throw exception(FINANCE_PAYMENT_APPROVE_FAIL);
        }
        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        pendingItems.forEach(item -> item.setWriteOffStatus(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                .setWriteOffTime(now).setWriteOffUserId(loginUserId));
        if (CollUtil.isNotEmpty(pendingItems)) {
            financePaymentItemMapper.updateBatch(pendingItems);
            updatePurchasePrice(pendingItems);
        }
        createPayableMiscOffset(payment, now);
        payableOtherService.createFromFinancePaymentDiscount(payment);
        if (payment.getSourcePayableMiscId() == null) {
            financeAutoWriteOffService.autoWriteOffPayment(id, loginUserId);
        }
        operateLogService.recordStatus(ERP_FINANCE_PAYMENT_TYPE, id, payment.getNo(), true);
    }

    private void createPayableMiscOffset(ErpFinancePaymentDO payment, LocalDateTime approveTime) {
        if (payment.getSourcePayableMiscId() == null) {
            return;
        }
        BigDecimal paymentPrice = normalize(getZeroIfNull(payment.getPaymentPrice()));
        if (paymentPrice.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        ErpPayableMiscDO existing = payableMiscMapper.selectBySourceDocument(
                ErpMiscTransferOffsetConstants.PAYMENT_OFFSET_SOURCE_TYPE, payment.getId());
        if (existing != null) {
            return;
        }
        ErpPayableMiscDO sourceMisc = validateAndFillSourcePayableMisc(payment, true);
        BigDecimal settledAmount = payableMiscMapper.selectOffsetAmountSumMapBySourceMiscIds(
                Collections.singleton(sourceMisc.getId()),
                ErpMiscTransferOffsetConstants.PAYMENT_OFFSET_SOURCE_TYPE)
                .getOrDefault(sourceMisc.getId(), BigDecimal.ZERO);
        BigDecimal remainingPrice = normalize(getZeroIfNull(sourceMisc.getAmount()).subtract(settledAmount));
        validatePaymentWriteOffAmount(paymentPrice, remainingPrice);
        String sourceMiscNo = sourceMisc.getNo() == null ? payment.getSourcePayableMiscNo() : sourceMisc.getNo();
        createPayableMiscOffset(payment, sourceMisc, null, sourceMiscNo, paymentPrice, approveTime);
    }

    private void createPayableMiscOffset(ErpFinancePaymentDO payment, ErpPayableMiscDO sourceMisc,
                                         Long sourceItemId, String sourceMiscNo, BigDecimal paymentPrice,
                                         LocalDateTime approveTime) {
        String no = noRedisDAO.generate("QTYFM");
        if (payableMiscMapper.selectByNo(no) != null) {
            throw exception(PAYABLE_MISC_NO_EXISTS);
        }
        ErpPayableMiscDO offset = new ErpPayableMiscDO()
                .setNo(no)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setBizTime(payment.getPaymentTime() == null ? approveTime : payment.getPaymentTime())
                .setSupplierId(sourceMisc.getSupplierId())
                .setAccountId(payment.getAccountId())
                .setAmount(paymentPrice.abs().negate())
                .setRemark("付款单审核自动生成，来源单号：" + payment.getNo() + "，冲减其他应付：" + sourceMiscNo)
                .setSourceType(ErpMiscTransferOffsetConstants.PAYMENT_OFFSET_SOURCE_TYPE)
                .setSourceId(payment.getId())
                .setSourceNo(payment.getNo())
                .setSourceItemId(sourceItemId)
                .setSourceMiscId(sourceMisc.getId())
                .setSourceMiscNo(sourceMiscNo)
                .setDeptId(payment.getDeptId())
                .setHandlerId(payment.getFinanceUserId());
        payableMiscMapper.insert(offset);
        operateLogService.recordCreate("其他应付", offset.getId(), offset.getNo());
        operateLogService.recordStatus("其他应付", offset.getId(), offset.getNo(), true);
    }

    private List<ErpFinancePaymentItemDO> validateFinancePaymentItems(
            Long supplierId,
            List<ErpFinancePaymentSaveReqVO.Item> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return convertList(list, o -> BeanUtils.toBean(o, ErpFinancePaymentItemDO.class, item -> {
            if (ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.PURCHASE_IN.getType())) {
                ErpPurchaseInDO purchaseIn = purchaseInService.validatePurchaseIn(item.getBizId());
                if (!Objects.equals(purchaseIn.getSupplierId(), supplierId)) {
                    throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "供应商必须相同");
                }
                item.setTotalPrice(ErpOriginalSettlementAmountUtils.calculatePurchaseIn(purchaseIn,
                        purchaseInService.getPurchaseInItemListByInIds(Collections.singleton(item.getBizId()))))
                        .setBizNo(purchaseIn.getNo());
            } else if (ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.PURCHASE_RETURN.getType())) {
                ErpPurchaseReturnDO purchaseReturn = purchaseReturnService.validatePurchaseReturn(item.getBizId());
                if (!Objects.equals(purchaseReturn.getSupplierId(), supplierId)) {
                    throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "供应商必须相同");
                }
                item.setTotalPrice(purchaseReturn.getTotalPrice().negate()).setBizNo(purchaseReturn.getNo());
            } else if (ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType())) {
                ErpPurchasePriceAdjustDO purchasePriceAdjust = purchasePriceAdjustService.validatePurchasePriceAdjust(item.getBizId());
                if (!Objects.equals(purchasePriceAdjust.getSupplierId(), supplierId)) {
                    throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "供应商必须相同");
                }
                item.setTotalPrice(purchasePriceAdjust.getTotalAdjustPrice()).setBizNo(purchasePriceAdjust.getNo());
            } else if (ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.PAYABLE_MISC.getType())) {
                ErpPayableMiscDO payableMisc = payableMiscMapper.selectById(item.getBizId());
                if (payableMisc == null || !ErpAuditStatus.APPROVE.getStatus().equals(payableMisc.getStatus())) {
                    throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "其他应付单不存在或未审核");
                }
                if (isPayableMiscOffset(payableMisc)) {
                    throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "系统生成的其他应付冲减单不能转付款");
                }
                if (!Objects.equals(payableMisc.getSupplierId(), supplierId)) {
                    throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "供应商必须相同");
                }
                item.setTotalPrice(getZeroIfNull(payableMisc.getAmount())).setBizNo(payableMisc.getNo());
            } else {
                throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "业务类型不正确：" + item.getBizType());
            }
            item.setWriteOffStatus(ErpFinanceWriteOffStatusEnum.PENDING.getStatus());
        }));
    }

    private List<ErpFinancePaymentItemDO> buildFinancePaymentDraftItems(
            List<ErpFinancePaymentSaveReqVO.Item> list) {
        return buildFinancePaymentDraftItems(list, false);
    }

    private List<ErpFinancePaymentItemDO> buildFinancePaymentDraftItems(
            List<ErpFinancePaymentSaveReqVO.Item> list, boolean preserveId) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getBizType() != null
                        && item.getBizId() != null
                        && item.getPaymentPrice() != null)
                .filter(item -> ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.PURCHASE_IN.getType())
                        || ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.PURCHASE_RETURN.getType())
                        || ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType())
                        || ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.PAYABLE_MISC.getType()))
                .map(item -> {
                    ErpFinancePaymentItemDO draftItem =
                            BeanUtils.toBean(item, ErpFinancePaymentItemDO.class);
                    draftItem.setId(null)
                            .setTotalPrice(normalize(getZeroIfNull(item.getTotalPrice())))
                            .setPaidPrice(normalize(getZeroIfNull(item.getPaidPrice())))
                            .setPaymentPrice(normalize(item.getPaymentPrice()))
                            .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.PENDING.getStatus())
                            .setWriteOffTime(null)
                            .setWriteOffUserId(null)
                            .setReverseTime(null)
                            .setReverseUserId(null)
                            .setReverseReason(null);
                    if (preserveId) {
                        draftItem.setId(item.getId());
                    }
                    return draftItem;
                })
                .collect(Collectors.toList());
    }

    private void fillDraftAmounts(ErpFinancePaymentDO payment, List<ErpFinancePaymentItemDO> paymentItems) {
        BigDecimal totalPrice = CollUtil.isEmpty(paymentItems)
                ? normalize(getZeroIfNull(payment.getTotalPrice()))
                : normalize(paymentItems.stream()
                    .map(ErpFinancePaymentItemDO::getPaymentPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal discountPrice = normalize(getZeroIfNull(payment.getDiscountPrice()));
        payment.setTotalPrice(totalPrice)
                .setDiscountPrice(discountPrice)
                .setPaymentPrice(normalize(totalPrice.subtract(discountPrice)));
    }

    private void insertFinancePaymentDraftItems(Long paymentId, List<ErpFinancePaymentItemDO> paymentItems) {
        if (CollUtil.isEmpty(paymentItems)) {
            return;
        }
        paymentItems.forEach(item -> item.setId(null).setPaymentId(paymentId));
        financePaymentItemMapper.insertBatch(paymentItems);
    }

    private void validateFinancePaymentDraftForSubmit(ErpFinancePaymentDO payment) {
        if (payment.getPaymentTime() == null) {
            throw exception(FINANCE_PAYMENT_DRAFT_SUBMIT_FAIL, "付款时间不能为空");
        }
        if (payment.getSupplierId() == null) {
            throw exception(FINANCE_PAYMENT_DRAFT_SUBMIT_FAIL, "供应商不能为空");
        }
        if (payment.getAccountId() == null) {
            throw exception(FINANCE_PAYMENT_DRAFT_SUBMIT_FAIL, "付款账户不能为空");
        }
        supplierService.validateSupplier(payment.getSupplierId());
        accountService.validateAccount(payment.getAccountId());
        if (payment.getFinanceUserId() != null) {
            adminUserApi.validateUser(payment.getFinanceUserId());
        }
    }

    private void updateFinancePaymentItemList(Long id, List<ErpFinancePaymentItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpFinancePaymentItemDO> oldList = financePaymentItemMapper.selectListByPaymentId(id);
        List<List<ErpFinancePaymentItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setPaymentId(id));
            financePaymentItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            financePaymentItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            financePaymentItemMapper.deleteByIds(convertList(diffList.get(2), ErpFinancePaymentItemDO::getId));
        }

    }

    private void applyFinancePaymentItemChangeSet(Long id, List<ErpFinancePaymentItemDO> finalItems,
            ErpFinanceItemOperationHelper.RequestChangeSet<ErpFinancePaymentSaveReqVO.Item> itemChangeSet) {
        if (itemChangeSet == null) {
            return;
        }
        if (CollUtil.isNotEmpty(itemChangeSet.getDeleteIds())) {
            financePaymentItemMapper.deleteByIds(itemChangeSet.getDeleteIds());
        }
        List<ErpFinancePaymentItemDO> insertList = finalItems.stream()
                .filter(item -> item.getId() == null)
                .peek(item -> item.setPaymentId(id))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(insertList)) {
            financePaymentItemMapper.insertBatch(insertList);
        }
        List<ErpFinancePaymentItemDO> updateList = finalItems.stream()
                .filter(item -> item.getId() != null && itemChangeSet.getUpdateIds().contains(item.getId()))
                .peek(item -> item.setPaymentId(id))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(updateList)) {
            financePaymentItemMapper.updateBatch(updateList);
        }
    }

    private void updatePurchasePrice(List<ErpFinancePaymentItemDO> paymentItems) {
        paymentItems.forEach(paymentItem -> {
            BigDecimal totalPaymentPrice = financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                    paymentItem.getBizId(), paymentItem.getBizType());
            if (ErpBizTypeEnum.PURCHASE_IN.getType().equals(paymentItem.getBizType())) {
                purchaseInService.updatePurchaseInPaymentPrice(paymentItem.getBizId(), totalPaymentPrice);
            } else if (ErpBizTypeEnum.PURCHASE_RETURN.getType().equals(paymentItem.getBizType())) {
                purchaseReturnService.updatePurchaseReturnRefundPrice(paymentItem.getBizId(), totalPaymentPrice.negate());
            } else if (ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType().equals(paymentItem.getBizType())) {
                purchasePriceAdjustService.updatePurchasePriceAdjustPaymentPrice(paymentItem.getBizId(), totalPaymentPrice);
            } else if (ErpBizTypeEnum.PAYABLE_MISC.getType().equals(paymentItem.getBizType())) {
                // 其他应付的冲减在付款单审核后生成负数其他应付单，不回写原主单。
            } else {
                throw new IllegalArgumentException("业务类型不正确：" + paymentItem.getBizType());
            }
        });
    }

    private void preparePendingItems(ErpFinancePaymentDO payment, List<ErpFinancePaymentItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        validateAllocationItems(payment, items);
        items.forEach(item -> item.setWriteOffStatus(ErpFinanceWriteOffStatusEnum.PENDING.getStatus())
                .setWriteOffTime(null).setWriteOffUserId(null)
                .setReverseTime(null).setReverseUserId(null).setReverseReason(null));
    }

    private void validateAndFillEffectiveItems(ErpFinancePaymentDO payment,
                                               List<ErpFinancePaymentItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        validateAllocationItems(payment, items);
    }

    private void validateAllocationItems(ErpFinancePaymentDO payment, List<ErpFinancePaymentItemDO> items) {
        Set<String> bizKeys = new HashSet<>();
        BigDecimal allocationAmount = BigDecimal.ZERO;
        for (ErpFinancePaymentItemDO item : items) {
            String bizKey = item.getBizType() + ":" + item.getBizId();
            if (!bizKeys.add(bizKey)) {
                throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "同一业务单据不能重复选择");
            }
            PaymentBizSnapshot biz = lockPaymentBiz(item.getBizType(), item.getBizId(), payment);
            BigDecimal allocatedPrice = financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                    item.getBizId(), item.getBizType());
            BigDecimal paymentPrice = normalize(item.getPaymentPrice());
            BigDecimal remainingPrice = normalize(biz.totalPrice.subtract(allocatedPrice));
            validatePaymentWriteOffAmount(paymentPrice, remainingPrice);
            item.setPaymentPrice(paymentPrice);
            item.setBizNo(biz.bizNo).setTotalPrice(biz.totalPrice).setPaidPrice(allocatedPrice);
            allocationAmount = normalize(allocationAmount.add(paymentPrice));
        }
        BigDecimal currentAllocatedPrice = payment.getId() == null ? BigDecimal.ZERO
                : financePaymentItemMapper.selectEffectivePriceSumMapByPaymentIds(
                        Collections.singleton(payment.getId())).getOrDefault(payment.getId(), BigDecimal.ZERO);
        validatePaymentAllocationLimit(payment, currentAllocatedPrice.add(allocationAmount));
    }

    @Override
    public List<ErpFinancePaymentWriteOffCandidateRespVO> getWriteOffCandidates(Long paymentId) {
        ErpFinancePaymentDO payment = validateFinancePaymentExists(paymentId);
        validatePaymentWriteOffStatus(payment);
        List<ErpFinancePaymentWriteOffCandidateRespVO> result = new java.util.ArrayList<>();

        LambdaQueryWrapperX<ErpPurchaseInDO> inQuery = new LambdaQueryWrapperX<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getSupplierId, payment.getSupplierId())
                .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (payment.getDeptId() == null) {
            inQuery.isNull(ErpPurchaseInDO::getDeptId);
        } else {
            inQuery.eq(ErpPurchaseInDO::getDeptId, payment.getDeptId());
        }
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectList(inQuery);
        Map<Long, List<ErpPurchaseInItemDO>> purchaseInItemMap = CollUtil.isEmpty(purchaseIns)
                ? Collections.emptyMap()
                : convertMultiMap(purchaseInService.getPurchaseInItemListByInIds(
                        convertSet(purchaseIns, ErpPurchaseInDO::getId)), ErpPurchaseInItemDO::getInId);
        Map<Long, BigDecimal> inAllocated = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(purchaseIns, ErpPurchaseInDO::getId), ErpBizTypeEnum.PURCHASE_IN.getType());
        purchaseIns.forEach(row -> addPaymentCandidate(result, ErpBizTypeEnum.PURCHASE_IN, row.getId(), row.getNo(),
                row.getInTime(), ErpOriginalSettlementAmountUtils.calculatePurchaseIn(row,
                        purchaseInItemMap.getOrDefault(row.getId(), Collections.emptyList())),
                inAllocated.getOrDefault(row.getId(), BigDecimal.ZERO)));

        LambdaQueryWrapperX<ErpPurchaseReturnDO> returnQuery = new LambdaQueryWrapperX<ErpPurchaseReturnDO>()
                .eq(ErpPurchaseReturnDO::getSupplierId, payment.getSupplierId())
                .eq(ErpPurchaseReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (payment.getDeptId() == null) {
            returnQuery.isNull(ErpPurchaseReturnDO::getDeptId);
        } else {
            returnQuery.eq(ErpPurchaseReturnDO::getDeptId, payment.getDeptId());
        }
        List<ErpPurchaseReturnDO> purchaseReturns = purchaseReturnMapper.selectList(returnQuery);
        Map<Long, BigDecimal> returnAllocated = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(purchaseReturns, ErpPurchaseReturnDO::getId), ErpBizTypeEnum.PURCHASE_RETURN.getType());
        purchaseReturns.forEach(row -> addPaymentCandidate(result, ErpBizTypeEnum.PURCHASE_RETURN, row.getId(), row.getNo(),
                row.getReturnTime(), getZeroIfNull(row.getTotalPrice()).negate(),
                returnAllocated.getOrDefault(row.getId(), BigDecimal.ZERO)));

        LambdaQueryWrapperX<ErpPurchasePriceAdjustDO> adjustQuery = new LambdaQueryWrapperX<ErpPurchasePriceAdjustDO>()
                .eq(ErpPurchasePriceAdjustDO::getSupplierId, payment.getSupplierId())
                .eq(ErpPurchasePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (payment.getDeptId() == null) {
            adjustQuery.isNull(ErpPurchasePriceAdjustDO::getDeptId);
        } else {
            adjustQuery.eq(ErpPurchasePriceAdjustDO::getDeptId, payment.getDeptId());
        }
        List<ErpPurchasePriceAdjustDO> adjusts = purchasePriceAdjustMapper.selectList(adjustQuery);
        Map<Long, BigDecimal> adjustAllocated = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(adjusts, ErpPurchasePriceAdjustDO::getId), ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType());
        adjusts.forEach(row -> addPaymentCandidate(result, ErpBizTypeEnum.PURCHASE_PRICE_ADJUST, row.getId(), row.getNo(),
                row.getAdjustTime(), getZeroIfNull(row.getTotalAdjustPrice()),
                adjustAllocated.getOrDefault(row.getId(), BigDecimal.ZERO)));

        LambdaQueryWrapperX<ErpPayableMiscDO> miscQuery = new LambdaQueryWrapperX<ErpPayableMiscDO>()
                .eq(ErpPayableMiscDO::getSupplierId, payment.getSupplierId())
                .eq(ErpPayableMiscDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        miscQuery.and(query -> query.isNull(ErpPayableMiscDO::getSourceType)
                .or().ne(ErpPayableMiscDO::getSourceType,
                        ErpMiscTransferOffsetConstants.PAYMENT_OFFSET_SOURCE_TYPE));
        if (payment.getDeptId() == null) {
            miscQuery.isNull(ErpPayableMiscDO::getDeptId);
        } else {
            miscQuery.eq(ErpPayableMiscDO::getDeptId, payment.getDeptId());
        }
        List<ErpPayableMiscDO> miscPayables = payableMiscMapper.selectList(miscQuery);
        Map<Long, BigDecimal> miscAllocated = payableMiscMapper.selectOffsetAmountSumMapBySourceMiscIds(
                convertSet(miscPayables, ErpPayableMiscDO::getId),
                ErpMiscTransferOffsetConstants.PAYMENT_OFFSET_SOURCE_TYPE);
        miscPayables.forEach(row -> addPaymentCandidate(result, ErpBizTypeEnum.PAYABLE_MISC, row.getId(),
                row.getNo(), row.getBizTime(), getZeroIfNull(row.getAmount()),
                miscAllocated.getOrDefault(row.getId(), BigDecimal.ZERO)));

        result.sort(Comparator.comparing(ErpFinancePaymentWriteOffCandidateRespVO::getBizTime,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                .thenComparing(ErpFinancePaymentWriteOffCandidateRespVO::getBizNo,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    @Override
    public List<ErpFinancePaymentFormCandidateRespVO> getFormCandidates(ErpFinancePaymentFormCandidateReqVO reqVO) {
        if (reqVO == null || reqVO.getSupplierId() == null) {
            return Collections.emptyList();
        }
        List<ErpFinancePaymentFormCandidateRespVO> result = new ArrayList<>();

        LambdaQueryWrapperX<ErpPurchaseInDO> inQuery = new LambdaQueryWrapperX<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPurchaseInDO::getDeptId, reqVO.getDeptId())
                .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .likeIfPresent(ErpPurchaseInDO::getNo, reqVO.getNo());
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectList(inQuery);
        Map<Long, List<ErpPurchaseInItemDO>> purchaseInItemMap = CollUtil.isEmpty(purchaseIns)
                ? Collections.emptyMap()
                : convertMultiMap(purchaseInService.getPurchaseInItemListByInIds(
                        convertSet(purchaseIns, ErpPurchaseInDO::getId)), ErpPurchaseInItemDO::getInId);
        Map<Long, BigDecimal> inAllocated = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(purchaseIns, ErpPurchaseInDO::getId), ErpBizTypeEnum.PURCHASE_IN.getType());
        purchaseIns.forEach(row -> addPaymentFormCandidate(result, ErpBizTypeEnum.PURCHASE_IN,
                row.getId(), row.getNo(), row.getInTime(), row.getCreateTime(), row.getSupplierId(), row.getDeptId(),
                ErpOriginalSettlementAmountUtils.calculatePurchaseIn(row,
                        purchaseInItemMap.getOrDefault(row.getId(), Collections.emptyList())),
                inAllocated.getOrDefault(row.getId(), BigDecimal.ZERO)));

        LambdaQueryWrapperX<ErpPurchaseReturnDO> returnQuery = new LambdaQueryWrapperX<ErpPurchaseReturnDO>()
                .eq(ErpPurchaseReturnDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPurchaseReturnDO::getDeptId, reqVO.getDeptId())
                .eq(ErpPurchaseReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .likeIfPresent(ErpPurchaseReturnDO::getNo, reqVO.getNo());
        List<ErpPurchaseReturnDO> purchaseReturns = purchaseReturnMapper.selectList(returnQuery);
        Map<Long, BigDecimal> returnAllocated = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(purchaseReturns, ErpPurchaseReturnDO::getId), ErpBizTypeEnum.PURCHASE_RETURN.getType());
        purchaseReturns.forEach(row -> addPaymentFormCandidate(result, ErpBizTypeEnum.PURCHASE_RETURN,
                row.getId(), row.getNo(), row.getReturnTime(), row.getCreateTime(), row.getSupplierId(), row.getDeptId(),
                getZeroIfNull(row.getTotalPrice()).negate(),
                returnAllocated.getOrDefault(row.getId(), BigDecimal.ZERO)));

        LambdaQueryWrapperX<ErpPurchasePriceAdjustDO> adjustQuery = new LambdaQueryWrapperX<ErpPurchasePriceAdjustDO>()
                .eq(ErpPurchasePriceAdjustDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getDeptId, reqVO.getDeptId())
                .eq(ErpPurchasePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .likeIfPresent(ErpPurchasePriceAdjustDO::getNo, reqVO.getNo());
        List<ErpPurchasePriceAdjustDO> adjusts = purchasePriceAdjustMapper.selectList(adjustQuery);
        Map<Long, BigDecimal> adjustAllocated = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(adjusts, ErpPurchasePriceAdjustDO::getId), ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType());
        adjusts.forEach(row -> addPaymentFormCandidate(result, ErpBizTypeEnum.PURCHASE_PRICE_ADJUST,
                row.getId(), row.getNo(), row.getAdjustTime(), row.getCreateTime(), row.getSupplierId(), row.getDeptId(),
                getZeroIfNull(row.getTotalAdjustPrice()),
                adjustAllocated.getOrDefault(row.getId(), BigDecimal.ZERO)));

        LambdaQueryWrapperX<ErpPayableMiscDO> miscQuery = new LambdaQueryWrapperX<ErpPayableMiscDO>()
                .eq(ErpPayableMiscDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPayableMiscDO::getDeptId, reqVO.getDeptId())
                .eq(ErpPayableMiscDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .likeIfPresent(ErpPayableMiscDO::getNo, reqVO.getNo());
        miscQuery.and(query -> query.isNull(ErpPayableMiscDO::getSourceType)
                .or().ne(ErpPayableMiscDO::getSourceType,
                        ErpMiscTransferOffsetConstants.PAYMENT_OFFSET_SOURCE_TYPE));
        List<ErpPayableMiscDO> miscPayables = payableMiscMapper.selectList(miscQuery);
        Map<Long, BigDecimal> miscAllocated = payableMiscMapper.selectOffsetAmountSumMapBySourceMiscIds(
                convertSet(miscPayables, ErpPayableMiscDO::getId),
                ErpMiscTransferOffsetConstants.PAYMENT_OFFSET_SOURCE_TYPE);
        miscPayables.forEach(row -> addPaymentFormCandidate(result, ErpBizTypeEnum.PAYABLE_MISC,
                row.getId(), row.getNo(), row.getBizTime(), row.getCreateTime(), row.getSupplierId(), row.getDeptId(),
                getZeroIfNull(row.getAmount()),
                miscAllocated.getOrDefault(row.getId(), BigDecimal.ZERO)));

        fillPaymentFormCandidateNames(result);
        result.sort(Comparator.comparing(ErpFinancePaymentFormCandidateRespVO::getCreateTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ErpFinancePaymentFormCandidateRespVO::getBizId,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    @Override
    public List<ErpFinancePaymentItemDO> getFinancePaymentItemListByBiz(Integer bizType, Long bizId) {
        return financePaymentItemMapper.selectListByBizTypeAndBizId(bizType, bizId);
    }

    private void addPaymentCandidate(List<ErpFinancePaymentWriteOffCandidateRespVO> result, ErpBizTypeEnum bizType,
                                     Long bizId, String bizNo, LocalDateTime bizTime,
                                     BigDecimal totalPrice, BigDecimal allocatedPrice) {
        BigDecimal unallocatedPrice = totalPrice.subtract(allocatedPrice);
        if (unallocatedPrice.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        result.add(new ErpFinancePaymentWriteOffCandidateRespVO()
                .setBizType(bizType.getType()).setBizTypeName(bizType.getName())
                .setBizId(bizId).setBizNo(bizNo).setBizTime(bizTime)
                .setTotalPrice(totalPrice).setAllocatedPrice(allocatedPrice)
                .setUnallocatedPrice(unallocatedPrice));
    }

    private void addPaymentFormCandidate(List<ErpFinancePaymentFormCandidateRespVO> result, ErpBizTypeEnum bizType,
                                         Long bizId, String bizNo, LocalDateTime bizTime, LocalDateTime createTime,
                                         Long supplierId, Long deptId, BigDecimal totalPrice, BigDecimal allocatedPrice) {
        BigDecimal normalizedTotalPrice = normalize(getZeroIfNull(totalPrice));
        BigDecimal normalizedAllocatedPrice = normalize(getZeroIfNull(allocatedPrice));
        BigDecimal unallocatedPrice = normalize(normalizedTotalPrice.subtract(normalizedAllocatedPrice));
        if (unallocatedPrice.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        result.add(new ErpFinancePaymentFormCandidateRespVO()
                .setBizType(bizType.getType()).setBizTypeName(bizType.getName())
                .setBizId(bizId).setBizNo(bizNo).setBizTime(bizTime).setCreateTime(createTime)
                .setSupplierId(supplierId).setDeptId(deptId)
                .setTotalPrice(normalizedTotalPrice).setAllocatedPrice(normalizedAllocatedPrice)
                .setUnallocatedPrice(unallocatedPrice));
    }

    private void fillPaymentFormCandidateNames(List<ErpFinancePaymentFormCandidateRespVO> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(rows, ErpFinancePaymentFormCandidateRespVO::getSupplierId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(rows, ErpFinancePaymentFormCandidateRespVO::getDeptId));
        rows.forEach(row -> {
            MapUtils.findAndThen(supplierMap, row.getSupplierId(),
                    supplier -> row.setSupplierName(supplier.getName()));
            MapUtils.findAndThen(deptMap, row.getDeptId(),
                    dept -> row.setDeptName(dept.getName()));
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeOffFinancePayment(ErpFinancePaymentWriteOffReqVO reqVO) {
        ErpFinancePaymentDO payment = financePaymentMapper.selectByIdForUpdate(reqVO.getPaymentId());
        if (payment == null) {
            throw exception(FINANCE_PAYMENT_NOT_EXISTS);
        }
        validatePaymentWriteOffStatus(payment);
        Set<String> bizKeys = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        List<ErpFinancePaymentItemDO> items = reqVO.getItems().stream().map(reqItem -> {
            String bizKey = reqItem.getBizType() + ":" + reqItem.getBizId();
            if (!bizKeys.add(bizKey)) {
                throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "同一业务单据不能重复选择");
            }
            PaymentBizSnapshot biz = lockPaymentBiz(reqItem.getBizType(), reqItem.getBizId(), payment);
            BigDecimal allocatedPrice = financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                    reqItem.getBizId(), reqItem.getBizType());
            BigDecimal writeOffAmount = normalize(reqItem.getWriteOffAmount());
            validatePaymentWriteOffAmount(writeOffAmount, normalize(biz.totalPrice.subtract(allocatedPrice)));
            return new ErpFinancePaymentItemDO().setPaymentId(payment.getId())
                    .setBizType(reqItem.getBizType()).setBizId(reqItem.getBizId()).setBizNo(biz.bizNo)
                    .setTotalPrice(biz.totalPrice).setPaidPrice(allocatedPrice)
                    .setPaymentPrice(writeOffAmount).setRemark(reqItem.getRemark())
                    .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                    .setWriteOffTime(now).setWriteOffUserId(loginUserId);
        }).collect(Collectors.toList());
        BigDecimal currentAllocatedPrice = financePaymentItemMapper.selectEffectivePriceSumMapByPaymentIds(
                Collections.singleton(payment.getId())).getOrDefault(payment.getId(), BigDecimal.ZERO);
        BigDecimal newAllocatedPrice = items.stream().map(ErpFinancePaymentItemDO::getPaymentPrice)
                .reduce(currentAllocatedPrice, BigDecimal::add);
        validatePaymentAllocationLimit(payment, newAllocatedPrice);
        financePaymentItemMapper.insertBatch(items);
        updatePurchasePrice(items);
        operateLogService.record(ERP_FINANCE_PAYMENT_TYPE, ERP_UPDATE_SUB_TYPE, payment.getId(),
                "付款单后续核销，单据编号：" + payment.getNo() + "，本次核销："
                        + items.stream().map(ErpFinancePaymentItemDO::getPaymentPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add), payment.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseFinancePaymentWriteOff(ErpFinancePaymentWriteOffReverseReqVO reqVO) {
        ErpFinancePaymentItemDO item = financePaymentItemMapper.selectByIdForUpdate(reqVO.getItemId());
        if (item == null || !ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus().equals(item.getWriteOffStatus())) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_ITEM_NOT_EFFECTIVE);
        }
        ErpFinancePaymentDO payment = financePaymentMapper.selectByIdForUpdate(item.getPaymentId());
        if (payment == null) {
            throw exception(FINANCE_PAYMENT_NOT_EXISTS);
        }
        validatePaymentWriteOffStatus(payment);
        lockPaymentBiz(item.getBizType(), item.getBizId(), payment);
        item.setWriteOffStatus(ErpFinanceWriteOffStatusEnum.REVERSED.getStatus())
                .setReverseTime(LocalDateTime.now()).setReverseUserId(SecurityFrameworkUtils.getLoginUserId())
                .setReverseReason(reqVO.getReason());
        financePaymentItemMapper.updateById(item);
        updatePurchasePrice(Collections.singletonList(item));
        operateLogService.record(ERP_FINANCE_PAYMENT_TYPE, ERP_UPDATE_SUB_TYPE, payment.getId(),
                "撤销付款核销，单据编号：" + payment.getNo() + "，核销明细：" + item.getId()
                        + "，原因：" + reqVO.getReason(), payment.getNo());
    }

    private PaymentBizSnapshot lockPaymentBiz(Integer bizType, Long bizId, ErpFinancePaymentDO payment) {
        PaymentBizSnapshot result;
        if (ObjectUtil.equal(bizType, ErpBizTypeEnum.PURCHASE_IN.getType())) {
            ErpPurchaseInDO row = purchaseInMapper.selectOne(new LambdaQueryWrapperX<ErpPurchaseInDO>()
                    .eq(ErpPurchaseInDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new PaymentBizSnapshot(row.getSupplierId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), ErpOriginalSettlementAmountUtils.calculatePurchaseIn(row,
                    purchaseInService.getPurchaseInItemListByInIds(Collections.singleton(bizId))));
        } else if (ObjectUtil.equal(bizType, ErpBizTypeEnum.PURCHASE_RETURN.getType())) {
            ErpPurchaseReturnDO row = purchaseReturnMapper.selectOne(new LambdaQueryWrapperX<ErpPurchaseReturnDO>()
                    .eq(ErpPurchaseReturnDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new PaymentBizSnapshot(row.getSupplierId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), getZeroIfNull(row.getTotalPrice()).negate());
        } else if (ObjectUtil.equal(bizType, ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType())) {
            ErpPurchasePriceAdjustDO row = purchasePriceAdjustMapper.selectOne(
                    new LambdaQueryWrapperX<ErpPurchasePriceAdjustDO>()
                            .eq(ErpPurchasePriceAdjustDO::getId, bizId).last("FOR UPDATE"));
            result = row == null ? null : new PaymentBizSnapshot(row.getSupplierId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), getZeroIfNull(row.getTotalAdjustPrice()));
        } else if (ObjectUtil.equal(bizType, ErpBizTypeEnum.PAYABLE_MISC.getType())) {
            ErpPayableMiscDO row = payableMiscMapper.selectOne(new LambdaQueryWrapperX<ErpPayableMiscDO>()
                    .eq(ErpPayableMiscDO::getId, bizId).last("FOR UPDATE"));
            if (isPayableMiscOffset(row)) {
                throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "系统生成的其他应付冲减单不能转付款");
            }
            result = row == null ? null : new PaymentBizSnapshot(row.getSupplierId(), row.getDeptId(), row.getStatus(),
                    row.getNo(), getZeroIfNull(row.getAmount()));
        } else {
            throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "业务类型不正确：" + bizType);
        }
        if (result == null || !ErpAuditStatus.APPROVE.getStatus().equals(result.status)) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "业务单据不存在或未审核");
        }
        if (!Objects.equals(result.partyId, payment.getSupplierId())) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "供应商必须相同");
        }
        if (!Objects.equals(result.deptId, payment.getDeptId())) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "所属部门必须相同");
        }
        return result;
    }

    private boolean isPayableMiscOffset(ErpPayableMiscDO row) {
        return row != null
                && ErpMiscTransferOffsetConstants.PAYMENT_OFFSET_SOURCE_TYPE.equals(row.getSourceType());
    }

    private void validatePaymentWriteOffStatus(ErpFinancePaymentDO payment) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(payment.getStatus())) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_STATUS_INVALID);
        }
    }

    private void validatePaymentWriteOffAmount(BigDecimal amount, BigDecimal remainingPrice) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_AMOUNT_INVALID, "核销金额不能为 0");
        }
        if (remainingPrice.compareTo(BigDecimal.ZERO) == 0
                || amount.signum() != remainingPrice.signum()
                || amount.abs().compareTo(remainingPrice.abs()) > 0) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_AMOUNT_INVALID, "核销金额超过业务单据未核销金额或符号不一致");
        }
    }

    private void validatePaymentAllocationLimit(ErpFinancePaymentDO payment, BigDecimal allocatedPrice) {
        BigDecimal normalizedAllocatedPrice = normalize(allocatedPrice);
        BigDecimal totalPrice = normalize(getZeroIfNull(payment.getTotalPrice()));
        if (normalizedAllocatedPrice.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        if (totalPrice.compareTo(BigDecimal.ZERO) == 0
                || normalizedAllocatedPrice.signum() != totalPrice.signum()
                || normalizedAllocatedPrice.abs().compareTo(totalPrice.abs()) > 0) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_AMOUNT_EXCEED);
        }
    }

    private static final class PaymentBizSnapshot {
        private final Long partyId;
        private final Long deptId;
        private final Integer status;
        private final String bizNo;
        private final BigDecimal totalPrice;

        private PaymentBizSnapshot(Long partyId, Long deptId, Integer status, String bizNo, BigDecimal totalPrice) {
            this.partyId = partyId;
            this.deptId = deptId;
            this.status = status;
            this.bizNo = bizNo;
            this.totalPrice = totalPrice;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFinancePayment(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpFinancePaymentDO> payments = financePaymentMapper.selectByIds(ids);
        if (CollUtil.isEmpty(payments)) {
            return;
        }
        payments.forEach(payment -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(payment.getStatus())) {
                throw exception(FINANCE_PAYMENT_DELETE_FAIL_APPROVE, payment.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        payments.forEach(payment -> {
            // 2.1 删除付款单
            financePaymentMapper.deleteById(payment.getId());
            // 2.2 删除付款单项
            List<ErpFinancePaymentItemDO> paymentItems = financePaymentItemMapper.selectListByPaymentId(payment.getId());
            financePaymentItemMapper.deleteByIds(convertSet(paymentItems, ErpFinancePaymentItemDO::getId));

            operateLogService.recordDelete(ERP_FINANCE_PAYMENT_TYPE, payment.getId(), payment.getNo());
        });
    }

    private void recordUpdate(ErpFinancePaymentDO oldPayment, ErpFinancePaymentDO newPayment) {
        operateLogService.record(ERP_FINANCE_PAYMENT_TYPE, ERP_UPDATE_SUB_TYPE, newPayment.getId(),
                "更新付款单，单据编号：" + oldPayment.getNo()
                        + "，合计金额：" + oldPayment.getTotalPrice() + " -> " + newPayment.getTotalPrice()
                        + "，实际付款：" + oldPayment.getPaymentPrice() + " -> " + newPayment.getPaymentPrice(),
                oldPayment.getNo());
    }

    private ErpFinancePaymentDO validateFinancePaymentExists(Long id) {
        ErpFinancePaymentDO payment = financePaymentMapper.selectById(id);
        if (payment == null) {
            throw exception(FINANCE_PAYMENT_NOT_EXISTS);
        }
        return payment;
    }

    @Override
    public ErpFinancePaymentDO getFinancePayment(Long id) {
        return financePaymentMapper.selectById(id);
    }

    @Override
    public List<ErpFinancePaymentDO> getFinancePaymentList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return financePaymentMapper.selectByIds(ids);
    }

    @Override
    public PageResult<ErpFinancePaymentDO> getFinancePaymentPage(ErpFinancePaymentPageReqVO pageReqVO) {
        return financePaymentMapper.selectPage(pageReqVO);
    }

    // ==================== 付款单项 ====================

    @Override
    public List<ErpFinancePaymentItemDO> getFinancePaymentItemListByPaymentId(Long paymentId) {
        return financePaymentItemMapper.selectListByPaymentId(paymentId);
    }

    @Override
    public PageResult<ErpFinancePaymentItemDO> getFinancePaymentItemPage(ErpFinancePaymentItemPageReqVO pageReqVO) {
        validateFinancePaymentExists(pageReqVO.getPaymentId());
        return financePaymentItemMapper.selectPageByPaymentId(pageReqVO);
    }

    @Override
    public List<ErpFinancePaymentItemDO> getFinancePaymentItemListByPaymentIds(Collection<Long> paymentIds) {
        if (CollUtil.isEmpty(paymentIds)) {
            return Collections.emptyList();
        }
        return financePaymentItemMapper.selectListByPaymentIds(paymentIds);
    }

    @Override
    public Map<Long, BigDecimal> getEffectivePaymentPriceSumMapByPaymentIds(Collection<Long> paymentIds) {
        return financePaymentItemMapper.selectEffectivePriceSumMapByPaymentIds(paymentIds);
    }

    @Override
    public Map<Long, Long> getEffectivePaymentItemCountMapByPaymentIds(Collection<Long> paymentIds) {
        return financePaymentItemMapper.selectEffectiveCountMapByPaymentIds(paymentIds);
    }

    private BigDecimal getZeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

}
