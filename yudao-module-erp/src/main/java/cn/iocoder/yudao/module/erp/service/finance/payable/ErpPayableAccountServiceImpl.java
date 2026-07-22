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
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableWriteOffDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableWriteOffMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_WRITEOFF_AMOUNT_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_WRITEOFF_BALANCE_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PAYABLE_WRITEOFF_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_WRITEOFF_SUB_TYPE;

@Service
@Validated
public class ErpPayableAccountServiceImpl implements ErpPayableAccountService {

    @Resource
    private ErpPayableAccountMapper payableAccountMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
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
    private ErpSupplierService supplierService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private AdminUserApi adminUserApi;

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
        BigDecimal balance = DataPermissionUtils.executeIgnore(() -> buildRows(balanceReqVO, scope).stream()
                .map(this::getChangeAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PAYABLE_WRITEOFF_BALANCE_EMPTY);
        }
        if (reqVO.getWriteOffAmount().compareTo(balance) > 0) {
            throw exception(PAYABLE_WRITEOFF_AMOUNT_EXCEED, reqVO.getWriteOffAmount(), balance);
        }
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
        purchaseIns.forEach(item -> rows.add(buildAllocatedRow("采购入库",
                ErpBizTypeEnum.PURCHASE_IN.getType(), item.getId(), item.getInTime(), item.getNo(),
                item.getTotalPrice(), purchaseInAllocated.get(item.getId()))));

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
        purchaseReturns.forEach(item -> rows.add(buildAllocatedRow("采购退货",
                ErpBizTypeEnum.PURCHASE_RETURN.getType(), item.getId(), item.getReturnTime(), item.getNo(),
                negateAmount(item.getTotalPrice()), purchaseReturnAllocated.get(item.getId()))));

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
        priceAdjusts.forEach(item -> rows.add(buildAllocatedRow("采购调价",
                ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType(), item.getId(), item.getAdjustTime(), item.getNo(),
                defaultAmount(item.getTotalAdjustPrice()), priceAdjustAllocated.get(item.getId()))));

        LambdaQueryWrapperX<ErpFinancePaymentDO> paymentQuery = new LambdaQueryWrapperX<ErpFinancePaymentDO>()
                .eq(ErpFinancePaymentDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpFinancePaymentDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getStartTime())
                .ltIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getEndTime());
        applyScope(paymentQuery, scope, ErpFinancePaymentDO::getDeptId, ErpFinancePaymentDO::getFinanceUserId);
        financePaymentMapper.selectList(paymentQuery)
                .forEach(item -> rows.add(buildRow("付款单", null, item.getId(),
                        item.getPaymentTime(), item.getNo(), negateAmount(item.getTotalPrice()), false)));

        payableWriteOffMapper.selectListBySupplierId(reqVO.getSupplierId(), reqVO.getStartTime(), reqVO.getEndTime(),
                        scope.getDeptIds(), scope.getSelfUserId(), scope.isAll())
                .forEach(item -> rows.add(buildRow("核销", item.getBizType(), item.getBizId(),
                        item.getWriteOffTime(), item.getBizNo() == null ? String.valueOf(item.getId()) : item.getBizNo(),
                        negateAmount(item.getWriteOffAmount()), true)));

        LambdaQueryWrapperX<ErpPayableOtherDO> otherQuery = new LambdaQueryWrapperX<ErpPayableOtherDO>()
                .eq(ErpPayableOtherDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPayableOtherDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpPayableOtherDO::getBizTime, reqVO.getStartTime() == null ? null : reqVO.getStartTime().toLocalDate())
                .leIfPresent(ErpPayableOtherDO::getBizTime, reqVO.getEndTime() == null ? null : reqVO.getEndTime().toLocalDate());
        applyScope(otherQuery, scope, ErpPayableOtherDO::getDeptId, ErpPayableOtherDO::getHandlerId);
        payableOtherMapper.selectList(otherQuery)
                .forEach(item -> rows.add(buildRow("其他应付", null, item.getId(),
                        item.getBizTime() == null ? null : item.getBizTime().atStartOfDay(),
                        item.getNo(), item.getPayableAmount(), false)));

        rows.sort(Comparator.comparing(ErpPayableDetailRespVO::getDocDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ErpPayableDetailRespVO::getDocType)
                .thenComparing(ErpPayableDetailRespVO::getDocNo, Comparator.nullsLast(String::compareTo)));
        return rows;
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
        BigDecimal actualAmount = defaultAmount(amount);
        ErpPayableDetailRespVO row = new ErpPayableDetailRespVO();
        row.setDocType(docType);
        row.setBizType(bizType);
        row.setBizId(bizId);
        row.setDocDate(docDate);
        row.setDocNo(docNo);
        row.setIncreaseAmount(actualAmount.compareTo(BigDecimal.ZERO) > 0 ? actualAmount : BigDecimal.ZERO);
        row.setPaymentAmount(actualAmount.compareTo(BigDecimal.ZERO) < 0 && !writeOff ? actualAmount.abs() : BigDecimal.ZERO);
        row.setWriteOffAmount(actualAmount.compareTo(BigDecimal.ZERO) < 0 && writeOff ? actualAmount.abs() : BigDecimal.ZERO);
        return row;
    }

    private ErpPayableDetailRespVO buildAllocatedRow(String docType, Integer bizType, Long bizId,
                                                     LocalDateTime docDate, String docNo, BigDecimal amount,
                                                     BigDecimal allocatedAmount) {
        ErpPayableDetailRespVO row = buildRow(docType, bizType, bizId, docDate, docNo, amount, false);
        row.setAllocatedAmount(defaultAmount(allocatedAmount).abs());
        return row;
    }

    private BigDecimal getChangeAmount(ErpPayableDetailRespVO row) {
        BigDecimal increaseAmount = row.getIncreaseAmount() == null ? BigDecimal.ZERO : row.getIncreaseAmount();
        BigDecimal paymentAmount = row.getPaymentAmount() == null ? BigDecimal.ZERO : row.getPaymentAmount();
        BigDecimal writeOffAmount = row.getWriteOffAmount() == null ? BigDecimal.ZERO : row.getWriteOffAmount();
        return increaseAmount.subtract(paymentAmount).subtract(writeOffAmount);
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
