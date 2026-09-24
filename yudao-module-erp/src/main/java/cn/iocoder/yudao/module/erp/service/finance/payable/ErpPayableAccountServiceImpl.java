package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableWriteOffReqVO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableWriteOffDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableWriteOffMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOriginalSettlementAmountUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_WRITEOFF_AMOUNT_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_WRITEOFF_BALANCE_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PAYABLE_WRITEOFF_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_WRITEOFF_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableOtherService.PAYMENT_DISCOUNT_SOURCE_TYPE;

@Service
@Validated
public class ErpPayableAccountServiceImpl implements ErpPayableAccountService {

    @Resource
    private ErpPayableAccountMapper payableAccountMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpPurchasePriceAdjustMapper purchasePriceAdjustMapper;
    @Resource
    private ErpFinancePaymentMapper financePaymentMapper;
    @Resource
    private ErpFinancePaymentItemMapper financePaymentItemMapper;
    @Resource
    private ErpPayableOtherMapper payableOtherMapper;
    @Resource
    private ErpPayableWriteOffMapper payableWriteOffMapper;
    @Resource
    private ErpVoucherMapper voucherMapper;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Override
    public PageResult<ErpPayableAccountDO> getPayableAccountPage(ErpPayableAccountPageReqVO reqVO) {
        SupplierVisibleScope scope = getSupplierVisibleScope();
        ErpFinanceVisibleScope documentScope = getPayableVisibleScope();
        if (scope == null || documentScope == null || !documentScope.hasAccess()) {
            return PageResult.empty();
        }
        return DataPermissionUtils.executeIgnore(() ->
                payableAccountMapper.selectPage(reqVO, scope.getDeptIds(), scope.getSelfUserId(), scope.isAll(),
                        documentScope.getDeptIds(), documentScope.getSelfUserId(), documentScope.isAll()));
    }

    @Override
    public List<ErpPayableAccountDO> getPayableAccountList(ErpPayableAccountPageReqVO reqVO) {
        SupplierVisibleScope scope = getSupplierVisibleScope();
        ErpFinanceVisibleScope documentScope = getPayableVisibleScope();
        if (scope == null || documentScope == null || !documentScope.hasAccess()) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() ->
                payableAccountMapper.selectList(reqVO, scope.getDeptIds(), scope.getSelfUserId(), scope.isAll(),
                        documentScope.getDeptIds(), documentScope.getSelfUserId(), documentScope.isAll()));
    }

    @Override
    public List<ErpPayableDetailRespVO> getPayableDetailList(ErpPayableDetailReqVO reqVO) {
        supplierService.validateSupplier(reqVO.getSupplierId());
        ErpFinanceVisibleScope scope = getPayableVisibleScope();
        if (scope == null || !scope.hasAccess()) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> buildPayableDetailList(reqVO, scope));
    }

    @Override
    public List<ErpPayableDetailRespVO> getPayableDetailList(ErpPayableDetailReqVO reqVO,
                                                             ErpFinanceVisibleScope scope) {
        supplierService.validateSupplier(reqVO.getSupplierId());
        if (scope == null || !scope.hasAccess()) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> buildPayableDetailList(reqVO, scope));
    }

    @Override
    public Long writeOffPayable(ErpPayableWriteOffReqVO reqVO) {
        supplierService.validateSupplier(reqVO.getSupplierId());
        ErpFinanceVisibleScope scope = getPayableVisibleScope();
        if (scope == null || !scope.hasAccess()) {
            throw exception(PAYABLE_WRITEOFF_BALANCE_EMPTY);
        }
        ErpPayableDetailReqVO balanceReqVO = new ErpPayableDetailReqVO();
        balanceReqVO.setSupplierId(reqVO.getSupplierId());
        List<ErpPayableDetailRespVO> balanceRows = DataPermissionUtils.executeIgnore(() ->
                buildRows(balanceReqVO, scope));
        BigDecimal balance = balanceRows.stream().map(this::getChangeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PAYABLE_WRITEOFF_BALANCE_EMPTY);
        }
        if (reqVO.getWriteOffAmount().compareTo(balance) > 0) {
            throw exception(PAYABLE_WRITEOFF_AMOUNT_EXCEED, reqVO.getWriteOffAmount(), balance);
        }
        validatePayableWriteOffAvailable(reqVO, balanceRows);
        ErpPayableWriteOffDO writeOff = new ErpPayableWriteOffDO();
        writeOff.setSupplierId(reqVO.getSupplierId());
        writeOff.setBizType(reqVO.getBizType());
        writeOff.setBizId(reqVO.getBizId());
        writeOff.setBizNo(reqVO.getBizNo());
        writeOff.setWriteOffAmount(reqVO.getWriteOffAmount());
        writeOff.setRemark(reqVO.getRemark());
        writeOff.setWriteOffTime(LocalDateTime.now());
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        writeOff.setOperatorUserId(loginUserId);
        AdminUserRespDTO loginUser = loginUserId == null ? null : adminUserApi.getUser(loginUserId);
        writeOff.setDeptId(resolveWriteOffDeptId(reqVO, loginUser));
        payableWriteOffMapper.insert(writeOff);
        operateLogService.record(ERP_PAYABLE_WRITEOFF_TYPE, ERP_WRITEOFF_SUB_TYPE, writeOff.getId(),
                "核销应付账款，供应商编号：" + reqVO.getSupplierId()
                        + (reqVO.getBizNo() == null ? "" : "，单据号：" + reqVO.getBizNo())
                        + "，核销金额：" + reqVO.getWriteOffAmount(), String.valueOf(writeOff.getId()));
        return writeOff.getId();
    }

    private void validatePayableWriteOffAvailable(ErpPayableWriteOffReqVO reqVO,
                                                  List<ErpPayableDetailRespVO> rows) {
        if (reqVO.getBizType() == null || reqVO.getBizId() == null) {
            return;
        }
        BigDecimal documentAmount = rows.stream()
                .filter(row -> reqVO.getBizType().equals(row.getBizType()))
                .filter(row -> reqVO.getBizId().equals(row.getBizId()))
                .map(row -> row.getIncreaseAmount() == null ? BigDecimal.ZERO : row.getIncreaseAmount())
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        BigDecimal allocatedAmount = rows.stream()
                .filter(row -> reqVO.getBizType().equals(row.getBizType()))
                .filter(row -> reqVO.getBizId().equals(row.getBizId()))
                .map(row -> row.getAllocatedAmount() == null ? BigDecimal.ZERO : row.getAllocatedAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal writtenOffAmount = rows.stream()
                .filter(row -> reqVO.getBizType().equals(row.getBizType()))
                .filter(row -> reqVO.getBizId().equals(row.getBizId()))
                .map(row -> row.getWriteOffAmount() == null ? BigDecimal.ZERO : row.getWriteOffAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal available = documentAmount.subtract(allocatedAmount).subtract(writtenOffAmount);
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PAYABLE_WRITEOFF_BALANCE_EMPTY);
        }
        if (reqVO.getWriteOffAmount().compareTo(available) > 0) {
            throw exception(PAYABLE_WRITEOFF_AMOUNT_EXCEED, reqVO.getWriteOffAmount(), available);
        }
    }

    private Long resolveWriteOffDeptId(ErpPayableWriteOffReqVO reqVO, AdminUserRespDTO loginUser) {
        if (reqVO.getBizId() != null && reqVO.getBizType() != null) {
            if (ErpBizTypeEnum.PURCHASE_IN.getType().equals(reqVO.getBizType())) {
                ErpPurchaseInDO source = purchaseInMapper.selectById(reqVO.getBizId());
                if (source != null && source.getDeptId() != null) return source.getDeptId();
            } else if (ErpBizTypeEnum.PURCHASE_RETURN.getType().equals(reqVO.getBizType())) {
                ErpPurchaseReturnDO source = purchaseReturnMapper.selectById(reqVO.getBizId());
                if (source != null && source.getDeptId() != null) return source.getDeptId();
            } else if (ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType().equals(reqVO.getBizType())) {
                ErpPurchasePriceAdjustDO source = purchasePriceAdjustMapper.selectById(reqVO.getBizId());
                if (source != null && source.getDeptId() != null) return source.getDeptId();
            }
        }
        return loginUser == null ? null : loginUser.getDeptId();
    }

    private BigDecimal getInitialBalance(ErpPayableDetailReqVO reqVO, ErpFinanceVisibleScope scope) {
        if (reqVO.getStartTime() == null) {
            return BigDecimal.ZERO;
        }
        ErpPayableDetailReqVO copy = new ErpPayableDetailReqVO();
        copy.setSupplierId(reqVO.getSupplierId());
        copy.setBizTime(new LocalDateTime[]{null, reqVO.getStartTime()});

        BigDecimal total = BigDecimal.ZERO;
        for (ErpPayableDetailRespVO row : buildRows(copy, scope)) {
            total = total.add(getChangeAmount(row));
        }
        return total;
    }

    private List<ErpPayableDetailRespVO> buildPayableDetailList(ErpPayableDetailReqVO reqVO,
                                                                ErpFinanceVisibleScope scope) {
        List<ErpPayableDetailRespVO> rows = buildRows(reqVO, scope);
        BigDecimal runningBalance = getInitialBalance(reqVO, scope);
        List<ErpPayableDetailRespVO> result = new ArrayList<>(rows.size());
        for (ErpPayableDetailRespVO row : rows) {
            BigDecimal prevBalance = runningBalance;
            runningBalance = runningBalance.add(getChangeAmount(row));

            row.setPrevBalance(prevBalance);
            row.setBalance(runningBalance);
            result.add(row);
        }
        fillDeptNames(result);
        fillBusinessUserNames(result);
        fillVoucherNos(result);
        return result;
    }

    private List<ErpPayableDetailRespVO> buildRows(ErpPayableDetailReqVO reqVO, ErpFinanceVisibleScope scope) {
        List<ErpPayableDetailRespVO> rows = new ArrayList<>();

        LambdaQueryWrapperX<ErpPurchaseInDO> purchaseInQuery = new LambdaQueryWrapperX<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpPurchaseInDO::getInTime, reqVO.getStartTime())
                .ltIfPresent(ErpPurchaseInDO::getInTime, reqVO.getEndTime());
        applyScope(purchaseInQuery, scope, ErpPurchaseInDO::getDeptId, ErpPurchaseInDO::getCreator);
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectList(purchaseInQuery);
        Map<Long, BigDecimal> purchaseInAllocated = financePaymentItemMapper
                .selectPaymentPriceSumMapByBizIdsAndBizType(purchaseIns.stream().map(ErpPurchaseInDO::getId)
                        .collect(Collectors.toSet()), ErpBizTypeEnum.PURCHASE_IN.getType());
        Map<Long, List<ErpPurchaseInItemDO>> purchaseInItemMap = purchaseIns.isEmpty() ? Collections.emptyMap()
                : purchaseInItemMapper.selectListByInIds(purchaseIns.stream().map(ErpPurchaseInDO::getId)
                        .collect(Collectors.toSet())).stream()
                .collect(Collectors.groupingBy(ErpPurchaseInItemDO::getInId));
        purchaseIns.forEach(item -> rows.add(buildPurchaseInDetailRow(item, purchaseInAllocated.get(item.getId()),
                purchaseInItemMap.getOrDefault(item.getId(), Collections.emptyList()))));

        LambdaQueryWrapperX<ErpPurchaseReturnDO> purchaseReturnQuery = new LambdaQueryWrapperX<ErpPurchaseReturnDO>()
                .eq(ErpPurchaseReturnDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPurchaseReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpPurchaseReturnDO::getReturnTime, reqVO.getStartTime())
                .ltIfPresent(ErpPurchaseReturnDO::getReturnTime, reqVO.getEndTime());
        applyScope(purchaseReturnQuery, scope, ErpPurchaseReturnDO::getDeptId, ErpPurchaseReturnDO::getHandler);
        List<ErpPurchaseReturnDO> purchaseReturns = purchaseReturnMapper.selectList(purchaseReturnQuery);
        Map<Long, BigDecimal> purchaseReturnAllocated = financePaymentItemMapper
                .selectPaymentPriceSumMapByBizIdsAndBizType(purchaseReturns.stream().map(ErpPurchaseReturnDO::getId)
                        .collect(Collectors.toSet()), ErpBizTypeEnum.PURCHASE_RETURN.getType());
        purchaseReturns.forEach(item -> {
            ErpPayableDetailRespVO row = buildAllocatedRow("采购退货",
                    ErpBizTypeEnum.PURCHASE_RETURN.getType(), item.getId(), item.getReturnTime(), item.getNo(),
                    negateAmount(item.getTotalPrice()), purchaseReturnAllocated.get(item.getId()), item.getDeptId());
            row.setBusinessUserName(firstNonBlank(item.getPurchaser(), null));
            row.setBusinessUserId(row.getBusinessUserName() == null ? item.getHandler() : null);
            row.setSourceNo(item.getOrderNo());
            row.setFactoryOrderNo(item.getFactoryOrderNo());
            row.setInvoiceStatus(item.getInvoiceType());
            row.setRemark(item.getRemark());
            row.setVoucherSourceBizType(ErpVoucherSourceBizTypeEnum.PURCHASE_RETURN.getType());
            rows.add(row);
        });

        LambdaQueryWrapperX<ErpPurchasePriceAdjustDO> priceAdjustQuery = new LambdaQueryWrapperX<ErpPurchasePriceAdjustDO>()
                .eq(ErpPurchasePriceAdjustDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPurchasePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpPurchasePriceAdjustDO::getAdjustTime, reqVO.getStartTime())
                .ltIfPresent(ErpPurchasePriceAdjustDO::getAdjustTime, reqVO.getEndTime());
        applyScope(priceAdjustQuery, scope, ErpPurchasePriceAdjustDO::getDeptId, ErpPurchasePriceAdjustDO::getAdjuster);
        List<ErpPurchasePriceAdjustDO> priceAdjusts = purchasePriceAdjustMapper.selectList(priceAdjustQuery);
        Map<Long, BigDecimal> priceAdjustAllocated = financePaymentItemMapper
                .selectPaymentPriceSumMapByBizIdsAndBizType(priceAdjusts.stream().map(ErpPurchasePriceAdjustDO::getId)
                        .collect(Collectors.toSet()), ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType());
        priceAdjusts.forEach(item -> {
            ErpPayableDetailRespVO row = buildAllocatedRow("采购调价",
                    ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType(), item.getId(), item.getAdjustTime(), item.getNo(),
                    defaultAmount(item.getTotalAdjustPrice()), priceAdjustAllocated.get(item.getId()), item.getDeptId());
            row.setBusinessUserId(item.getAdjuster());
            row.setRemark(item.getRemark());
            row.setConfirmTime(item.getApproveTime());
            row.setVoucherSourceBizType(ErpVoucherSourceBizTypeEnum.PURCHASE_PRICE_ADJUST.getType());
            rows.add(row);
        });

        LambdaQueryWrapperX<ErpFinancePaymentDO> paymentQuery = new LambdaQueryWrapperX<ErpFinancePaymentDO>()
                .eq(ErpFinancePaymentDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpFinancePaymentDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getStartTime())
                .ltIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getEndTime());
        applyScope(paymentQuery, scope, ErpFinancePaymentDO::getDeptId, ErpFinancePaymentDO::getFinanceUserId);
        List<ErpFinancePaymentDO> payments = financePaymentMapper.selectList(paymentQuery);
        Map<Long, BigDecimal> paymentAllocated = financePaymentItemMapper
                .selectEffectivePriceSumMapByPaymentIds(payments.stream().map(ErpFinancePaymentDO::getId)
                        .collect(Collectors.toSet()));
        Set<Long> discountPaymentIds = payableOtherMapper
                .selectApprovedListBySourceIds(PAYMENT_DISCOUNT_SOURCE_TYPE,
                        payments.stream().map(ErpFinancePaymentDO::getId).collect(Collectors.toSet()))
                .stream().map(ErpPayableOtherDO::getSourceId).collect(Collectors.toSet());
        payments.forEach(item -> {
            ErpPayableDetailRespVO row = buildPaymentAllocatedRow("付款单", null, item.getId(),
                    item.getPaymentTime(), item.getNo(), resolvePaymentDetailPaymentAmount(item, discountPaymentIds),
                    paymentAllocated.get(item.getId()),
                    item.getDeptId());
            row.setBusinessUserId(item.getFinanceUserId());
            row.setRemark(item.getRemark());
            row.setVoucherSourceBizType(ErpVoucherSourceBizTypeEnum.PAYMENT.getType());
            rows.add(row);
        });

        payableWriteOffMapper.selectListBySupplierId(reqVO.getSupplierId(), reqVO.getStartTime(), reqVO.getEndTime(),
                        scope.getDeptIds(), scope.getSelfUserId(), scope.isAll())
                .forEach(item -> rows.add(buildRow("核销", item.getBizType(), item.getBizId(),
                        item.getWriteOffTime(), item.getBizNo() == null ? String.valueOf(item.getId()) : item.getBizNo(),
                        negateAmount(item.getWriteOffAmount()), true, item.getDeptId())));

        LambdaQueryWrapperX<ErpPayableOtherDO> otherQuery = new LambdaQueryWrapperX<ErpPayableOtherDO>()
                .eq(ErpPayableOtherDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPayableOtherDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpPayableOtherDO::getBizTime, reqVO.getStartTime() == null ? null : reqVO.getStartTime().toLocalDate())
                .leIfPresent(ErpPayableOtherDO::getBizTime, reqVO.getEndTime() == null ? null : reqVO.getEndTime().toLocalDate());
        applyScope(otherQuery, scope, ErpPayableOtherDO::getDeptId, ErpPayableOtherDO::getHandlerId);
        List<ErpPayableOtherDO> otherPayables = payableOtherMapper.selectList(otherQuery);
        Map<Long, LocalDateTime> paymentDiscountTimeMap = getPaymentDiscountTimeMap(otherPayables);
        otherPayables
                .forEach(item -> {
                    ErpPayableDetailRespVO row = buildRow("应付调账", null, item.getId(),
                            resolveOtherPayableDocDate(item, paymentDiscountTimeMap),
                            item.getNo(), item.getPayableAmount(), false, item.getDeptId());
                    row.setBusinessUserId(item.getHandlerId());
                    row.setSourceNo(item.getSourceNo());
                    row.setRemark(item.getRemark());
                    row.setVoucherNo(item.getVoucherNo());
                    row.setVoucherSourceBizType(ErpVoucherSourceBizTypeEnum.OTHER_PAYABLE.getType());
                    rows.add(row);
                });

        rows.sort(Comparator.comparing(ErpPayableDetailRespVO::getDocDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ErpPayableDetailRespVO::getDocType)
                .thenComparing(ErpPayableDetailRespVO::getDocNo, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    private Map<Long, LocalDateTime> getPaymentDiscountTimeMap(List<ErpPayableOtherDO> otherPayables) {
        Set<Long> paymentIds = otherPayables.stream()
                .filter(item -> PAYMENT_DISCOUNT_SOURCE_TYPE.equals(item.getSourceType()))
                .map(ErpPayableOtherDO::getSourceId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (paymentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return financePaymentMapper.selectList(new LambdaQueryWrapperX<ErpFinancePaymentDO>()
                        .in(ErpFinancePaymentDO::getId, paymentIds))
                .stream()
                .filter(item -> item.getPaymentTime() != null)
                .collect(Collectors.toMap(ErpFinancePaymentDO::getId, ErpFinancePaymentDO::getPaymentTime,
                        (first, second) -> first));
    }

    private LocalDateTime resolveOtherPayableDocDate(ErpPayableOtherDO item,
                                                     Map<Long, LocalDateTime> paymentDiscountTimeMap) {
        if (PAYMENT_DISCOUNT_SOURCE_TYPE.equals(item.getSourceType()) && item.getSourceId() != null) {
            LocalDateTime paymentTime = paymentDiscountTimeMap.get(item.getSourceId());
            if (paymentTime != null) {
                return paymentTime;
            }
        }
        return item.getBizTime() == null ? null : item.getBizTime().atStartOfDay();
    }

    private BigDecimal resolvePaymentDetailPaymentAmount(ErpFinancePaymentDO item, Set<Long> discountPaymentIds) {
        return discountPaymentIds.contains(item.getId()) ? item.getPaymentPrice() : item.getTotalPrice();
    }

    private ErpPayableDetailRespVO buildPurchaseInDetailRow(ErpPurchaseInDO purchaseIn, BigDecimal allocatedAmount,
                                                            List<ErpPurchaseInItemDO> items) {
        BigDecimal originalSettlementAmount = ErpOriginalSettlementAmountUtils.calculatePurchaseIn(purchaseIn, items);
        ErpPayableDetailRespVO row = buildAllocatedRow("采购入库",
                ErpBizTypeEnum.PURCHASE_IN.getType(), purchaseIn.getId(), purchaseIn.getInTime(), purchaseIn.getNo(),
                originalSettlementAmount, allocatedAmount, originalSettlementAmount, purchaseIn.getDeptId());
        row.setPriceAdjusted(isPurchaseInPriceAdjusted(purchaseIn, items));
        row.setBusinessUserName(firstNonBlank(purchaseIn.getPurchaser(), purchaseIn.getHandler()));
        row.setSourceNo(purchaseIn.getOrderNo());
        row.setFactoryOrderNo(purchaseIn.getFactoryOrderNo());
        row.setInvoiceStatus(resolvePurchaseInInvoiceStatus(purchaseIn));
        row.setRemark(purchaseIn.getRemark());
        row.setVoucherSourceBizType(ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType());
        return row;
    }

    private boolean isPurchaseInPriceAdjusted(ErpPurchaseInDO purchaseIn, List<ErpPurchaseInItemDO> items) {
        return Boolean.TRUE.equals(purchaseIn.getAdjusted()) || items.stream()
                .anyMatch(item -> item.getOriginalProductPrice() != null);
    }

    private BigDecimal negateAmount(BigDecimal amount) {
        return defaultAmount(amount).negate();
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private ErpPayableDetailRespVO buildRow(String docType, Integer bizType, Long bizId,
                                            LocalDateTime docDate, String docNo, BigDecimal amount,
                                            boolean writeOff) {
        return buildRow(docType, bizType, bizId, docDate, docNo, amount, writeOff, null);
    }

    private ErpPayableDetailRespVO buildRow(String docType, Integer bizType, Long bizId,
                                            LocalDateTime docDate, String docNo, BigDecimal amount,
                                            boolean writeOff, Long deptId) {
        BigDecimal actualAmount = defaultAmount(amount);
        ErpPayableDetailRespVO row = new ErpPayableDetailRespVO();
        row.setDocType(docType);
        row.setBizType(bizType);
        row.setBizId(bizId);
        row.setDocDate(docDate);
        row.setDocNo(docNo);
        row.setDeptId(deptId);
        row.setIncreaseAmount(writeOff ? BigDecimal.ZERO : actualAmount);
        row.setPaymentAmount(BigDecimal.ZERO);
        row.setWriteOffAmount(actualAmount.compareTo(BigDecimal.ZERO) < 0 && writeOff ? actualAmount.abs() : BigDecimal.ZERO);
        row.setWriteOffBaseAmount(actualAmount.abs());
        return row;
    }

    private ErpPayableDetailRespVO buildAllocatedRow(String docType, Integer bizType, Long bizId,
                                                     LocalDateTime docDate, String docNo, BigDecimal amount,
                                                     BigDecimal allocatedAmount) {
        return buildAllocatedRow(docType, bizType, bizId, docDate, docNo, amount, allocatedAmount, amount);
    }

    private ErpPayableDetailRespVO buildAllocatedRow(String docType, Integer bizType, Long bizId,
                                                     LocalDateTime docDate, String docNo, BigDecimal amount,
                                                     BigDecimal allocatedAmount, BigDecimal writeOffBaseAmount) {
        return buildAllocatedRow(docType, bizType, bizId, docDate, docNo, amount, allocatedAmount,
                writeOffBaseAmount, null);
    }

    private ErpPayableDetailRespVO buildAllocatedRow(String docType, Integer bizType, Long bizId,
                                                     LocalDateTime docDate, String docNo, BigDecimal amount,
                                                     BigDecimal allocatedAmount, BigDecimal writeOffBaseAmount,
                                                     Long deptId) {
        ErpPayableDetailRespVO row = buildRow(docType, bizType, bizId, docDate, docNo, amount, false, deptId);
        row.setAllocatedAmount(defaultAmount(allocatedAmount).abs());
        row.setWriteOffBaseAmount(defaultAmount(writeOffBaseAmount).abs());
        return row;
    }

    private ErpPayableDetailRespVO buildAllocatedRow(String docType, Integer bizType, Long bizId,
                                                     LocalDateTime docDate, String docNo, BigDecimal amount,
                                                     BigDecimal allocatedAmount, Long deptId) {
        return buildAllocatedRow(docType, bizType, bizId, docDate, docNo, amount, allocatedAmount, amount, deptId);
    }

    private ErpPayableDetailRespVO buildPaymentAllocatedRow(String docType, Integer bizType, Long bizId,
                                                            LocalDateTime docDate, String docNo, BigDecimal amount,
                                                            BigDecimal allocatedAmount) {
        return buildPaymentAllocatedRow(docType, bizType, bizId, docDate, docNo, amount, allocatedAmount, null);
    }

    private ErpPayableDetailRespVO buildPaymentAllocatedRow(String docType, Integer bizType, Long bizId,
                                                            LocalDateTime docDate, String docNo, BigDecimal amount,
                                                            BigDecimal allocatedAmount, Long deptId) {
        BigDecimal actualAmount = defaultAmount(amount);
        ErpPayableDetailRespVO row = buildRow(docType, bizType, bizId, docDate, docNo, BigDecimal.ZERO, false,
                deptId);
        row.setPaymentAmount(actualAmount);
        row.setAllocatedAmount(defaultAmount(allocatedAmount).abs());
        row.setWriteOffBaseAmount(actualAmount.abs());
        return row;
    }

    private void fillDeptNames(List<ErpPayableDetailRespVO> rows) {
        Set<Long> deptIds = rows.stream()
                .map(ErpPayableDetailRespVO::getDeptId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return;
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(deptIds);
        for (ErpPayableDetailRespVO row : rows) {
            DeptRespDTO dept = deptMap.get(row.getDeptId());
            if (dept != null) {
                row.setDeptName(dept.getName());
            }
        }
    }

    private void fillBusinessUserNames(List<ErpPayableDetailRespVO> rows) {
        Set<Long> userIds = rows.stream()
                .filter(row -> !hasText(row.getBusinessUserName()))
                .map(ErpPayableDetailRespVO::getBusinessUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        for (ErpPayableDetailRespVO row : rows) {
            if (hasText(row.getBusinessUserName()) || row.getBusinessUserId() == null) {
                continue;
            }
            AdminUserRespDTO user = userMap.get(row.getBusinessUserId());
            if (user != null) {
                row.setBusinessUserName(user.getNickname());
            }
        }
    }

    private void fillVoucherNos(List<ErpPayableDetailRespVO> rows) {
        Set<Integer> sourceBizTypes = rows.stream()
                .map(ErpPayableDetailRespVO::getVoucherSourceBizType)
                .filter(type -> type != null)
                .collect(Collectors.toSet());
        Set<Long> sourceBizIds = rows.stream()
                .filter(row -> row.getVoucherSourceBizType() != null)
                .map(ErpPayableDetailRespVO::getBizId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (sourceBizTypes.isEmpty() || sourceBizIds.isEmpty()) {
            return;
        }
        Map<String, String> voucherNoMap = new HashMap<>();
        for (ErpVoucherDO voucher : voucherMapper.selectListByBizTypes(sourceBizTypes, sourceBizIds)) {
            if (voucher.getSourceBizType() == null || voucher.getSourceBizId() == null
                    || !hasText(voucher.getVoucherNo())) {
                continue;
            }
            voucherNoMap.putIfAbsent(buildVoucherKey(voucher.getSourceBizType(), voucher.getSourceBizId()),
                    voucher.getVoucherNo());
        }
        for (ErpPayableDetailRespVO row : rows) {
            if (hasText(row.getVoucherNo()) || row.getVoucherSourceBizType() == null || row.getBizId() == null) {
                continue;
            }
            row.setVoucherNo(voucherNoMap.get(buildVoucherKey(row.getVoucherSourceBizType(), row.getBizId())));
        }
    }

    private String buildVoucherKey(Integer sourceBizType, Long sourceBizId) {
        return sourceBizType + ":" + sourceBizId;
    }

    private String resolvePurchaseInInvoiceStatus(ErpPurchaseInDO purchaseIn) {
        if (purchaseIn.getHasInvoice() != null) {
            return Boolean.TRUE.equals(purchaseIn.getHasInvoice()) ? "已开票" : "未开票";
        }
        return purchaseIn.getInvoiceType();
    }

    private String firstNonBlank(String first, String second) {
        if (hasText(first)) {
            return first;
        }
        return hasText(second) ? second : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private BigDecimal getChangeAmount(ErpPayableDetailRespVO row) {
        BigDecimal increaseAmount = row.getIncreaseAmount() == null ? BigDecimal.ZERO : row.getIncreaseAmount();
        BigDecimal paymentAmount = row.getPaymentAmount() == null ? BigDecimal.ZERO : row.getPaymentAmount();
        return increaseAmount.subtract(paymentAmount);
    }

    private SupplierVisibleScope getSupplierVisibleScope() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return null;
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(loginUserId, "erp_supplier");
        if (permission == null) {
            return null;
        }
        String selfUserId = Boolean.TRUE.equals(permission.getSelf()) ? String.valueOf(loginUserId) : null;
        return new SupplierVisibleScope(Boolean.TRUE.equals(permission.getAll()), permission.getDeptIds(), selfUserId);
    }

    private ErpFinanceVisibleScope getPayableVisibleScope() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return ErpFinanceVisibleScope.from(loginUserId == null ? null : permissionApi.getDeptDataPermission(
                loginUserId, "erp_finance_payable_account"), loginUserId);
    }

    private <T> void applyScope(LambdaQueryWrapperX<T> query, ErpFinanceVisibleScope scope,
                                com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> deptColumn,
                                com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> userColumn) {
        if (scope.isAll()) return;
        if (!scope.getDeptIds().isEmpty() && scope.getSelfUserId() != null) {
            query.and(w -> w.in(deptColumn, scope.getDeptIds()).or().eq(userColumn, scope.getSelfUserId()));
        } else if (!scope.getDeptIds().isEmpty()) {
            query.in(deptColumn, scope.getDeptIds());
        } else {
            query.eq(userColumn, scope.getSelfUserId());
        }
    }

    private static class SupplierVisibleScope {

        private final boolean all;
        private final Collection<Long> deptIds;
        private final String selfUserId;

        private SupplierVisibleScope(boolean all, Collection<Long> deptIds, String selfUserId) {
            this.all = all;
            this.deptIds = deptIds == null ? Collections.emptyList() : deptIds;
            this.selfUserId = selfUserId;
        }

        public boolean isAll() {
            return all;
        }

        public Collection<Long> getDeptIds() {
            return deptIds;
        }

        public String getSelfUserId() {
            return selfUserId;
        }
    }
}
