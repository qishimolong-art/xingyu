package cn.iocoder.yudao.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentWriteOffCandidateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentWriteOffReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentWriteOffReverseReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchasePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpPurchaseInService purchaseInService;
    @Resource
    private ErpPurchaseReturnService purchaseReturnService;
    @Resource
    private ErpPurchasePriceAdjustService purchasePriceAdjustService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpFinancePermissionFieldFiller permissionFieldFiller;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinancePayment(ErpFinancePaymentSaveReqVO createReqVO) {
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
    public void updateFinancePayment(ErpFinancePaymentSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpFinancePaymentDO payment = validateFinancePaymentExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(payment.getStatus())) {
            throw exception(FINANCE_PAYMENT_UPDATE_FAIL_APPROVE, payment.getNo());
        }
        List<ErpFinancePaymentItemDO> oldPaymentItems = financePaymentItemMapper.selectListByPaymentId(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, payment);
        if (fieldPermissionMasker.isFieldHidden(FIELD_PERMISSION_MODULE, "items")) {
            updateReqVO.setItems(BeanUtils.toBean(oldPaymentItems, ErpFinancePaymentSaveReqVO.Item.class));
        } else {
            fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldPaymentItems);
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
                updateReqVO.getSupplierId(), updateReqVO.getItems());

        // 2.1 更新付款单
        ErpFinancePaymentDO updateObj = BeanUtils.toBean(updateReqVO, ErpFinancePaymentDO.class);
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(payment.getDeptId());
        }
        fillDefaultAmount(updateObj);
        preparePendingItems(updateObj, paymentItems);
        financePaymentMapper.updateById(updateObj);
        // 2.2 更新付款单项
        updateFinancePaymentItemList(updateReqVO.getId(), paymentItems);
        recordUpdate(payment, updateObj);
    }

    private void fillDefaultAmount(ErpFinancePaymentDO payment) {
        payment.setDiscountPrice(getZeroIfNull(payment.getDiscountPrice()));
        BigDecimal totalPrice = getZeroIfNull(payment.getTotalPrice());
        BigDecimal paymentPrice = totalPrice.subtract(payment.getDiscountPrice());
        if (paymentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(FINANCE_PAYMENT_WRITEOFF_AMOUNT_INVALID, "实际付款金额必须大于 0");
        }
        payment.setPaymentPrice(paymentPrice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveFinancePayment(Long id) {
        ErpFinancePaymentDO payment = financePaymentMapper.selectByIdForUpdate(id);
        if (payment == null) {
            throw exception(FINANCE_PAYMENT_NOT_EXISTS);
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(payment.getStatus())) {
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
        operateLogService.recordStatus(ERP_FINANCE_PAYMENT_TYPE, id, payment.getNo(), true);
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
                item.setTotalPrice(purchaseIn.getTotalPrice()).setBizNo(purchaseIn.getNo());
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
            } else {
                throw exception(FINANCE_PAYMENT_WRITEOFF_BIZ_INVALID, "业务类型不正确：" + item.getBizType());
            }
            item.setWriteOffStatus(ErpFinanceWriteOffStatusEnum.PENDING.getStatus());
        }));
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
            BigDecimal remainingPrice = biz.totalPrice.subtract(allocatedPrice);
            validatePaymentWriteOffAmount(item.getPaymentPrice(), remainingPrice);
            item.setBizNo(biz.bizNo).setTotalPrice(biz.totalPrice).setPaidPrice(allocatedPrice);
            allocationAmount = allocationAmount.add(item.getPaymentPrice());
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
        Map<Long, BigDecimal> inAllocated = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(purchaseIns, ErpPurchaseInDO::getId), ErpBizTypeEnum.PURCHASE_IN.getType());
        purchaseIns.forEach(row -> addPaymentCandidate(result, ErpBizTypeEnum.PURCHASE_IN, row.getId(), row.getNo(),
                row.getInTime(), getZeroIfNull(row.getTotalPrice()), inAllocated.getOrDefault(row.getId(), BigDecimal.ZERO)));

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

        result.sort(Comparator.comparing(ErpFinancePaymentWriteOffCandidateRespVO::getBizTime,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                .thenComparing(ErpFinancePaymentWriteOffCandidateRespVO::getBizNo,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
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
            validatePaymentWriteOffAmount(reqItem.getWriteOffAmount(), biz.totalPrice.subtract(allocatedPrice));
            return new ErpFinancePaymentItemDO().setPaymentId(payment.getId())
                    .setBizType(reqItem.getBizType()).setBizId(reqItem.getBizId()).setBizNo(biz.bizNo)
                    .setTotalPrice(biz.totalPrice).setPaidPrice(allocatedPrice)
                    .setPaymentPrice(reqItem.getWriteOffAmount()).setRemark(reqItem.getRemark())
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
                    row.getNo(), getZeroIfNull(row.getTotalPrice()));
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
        if (allocatedPrice.compareTo(BigDecimal.ZERO) < 0
                || allocatedPrice.compareTo(getZeroIfNull(payment.getTotalPrice())) > 0) {
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
    public List<ErpFinancePaymentItemDO> getFinancePaymentItemListByPaymentIds(Collection<Long> paymentIds) {
        if (CollUtil.isEmpty(paymentIds)) {
            return Collections.emptyList();
        }
        return financePaymentItemMapper.selectListByPaymentIds(paymentIds);
    }

    private BigDecimal getZeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

}
