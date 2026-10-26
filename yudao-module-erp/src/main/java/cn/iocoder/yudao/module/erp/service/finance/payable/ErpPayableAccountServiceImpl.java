package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDrilldownReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableWriteOffReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpAccountDrilldownPageRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpAccountDrilldownSummaryRespVO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpMiscSettlementLedgerRow;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO;
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
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableWriteOffMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPayableDrilldownTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOriginalSettlementAmountUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.function.Function;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_WRITEOFF_AMOUNT_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_WRITEOFF_BALANCE_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PAYABLE_WRITEOFF_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_WRITEOFF_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableOtherService.PAYMENT_DISCOUNT_SOURCE_TYPE;

@Service
@Validated
public class ErpPayableAccountServiceImpl implements ErpPayableAccountService {

    private static final long SUMMARY_BATCH_SIZE = 500L;

    @Resource
    private ErpPayableAccountMapper payableAccountMapper;
    @Resource
    private ErpPayableMiscMapper payableMiscMapper;
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
    public ErpPayableAccountSummaryRespVO getPayableAccountSummary(ErpPayableAccountPageReqVO reqVO) {
        SupplierVisibleScope scope = getSupplierVisibleScope();
        ErpFinanceVisibleScope documentScope = getPayableVisibleScope();
        if (scope == null || documentScope == null || !documentScope.hasAccess()) {
            return new ErpPayableAccountSummaryRespVO();
        }
        return DataPermissionUtils.executeIgnore(() -> calculatePayableAccountSummary(reqVO, scope, documentScope));
    }

    private ErpPayableAccountSummaryRespVO calculatePayableAccountSummary(ErpPayableAccountPageReqVO reqVO,
                                                                           SupplierVisibleScope scope,
                                                                           ErpFinanceVisibleScope documentScope) {
        ErpPayableAccountSummaryRespVO summary = new ErpPayableAccountSummaryRespVO();
        long pageNo = 1L;
        while (true) {
            Page<ErpPayableAccountDO> page = new Page<>(pageNo, SUMMARY_BATCH_SIZE, false);
            List<ErpPayableAccountDO> rows = payableAccountMapper.selectPageRows(page, reqVO,
                    scope.getDeptIds(), scope.getSelfUserId(), scope.isAll(), documentScope.getDeptIds(),
                    documentScope.getSelfUserId(), documentScope.isAll()).getRecords();
            rows.forEach(row -> addSummaryRow(summary, row));
            if (rows.size() < SUMMARY_BATCH_SIZE) {
                return summary;
            }
            pageNo++;
        }
    }

    private void addSummaryRow(ErpPayableAccountSummaryRespVO summary, ErpPayableAccountDO row) {
        summary.setSupplierCount(summary.getSupplierCount() + 1);
        summary.setPurchaseInAmount(summary.getPurchaseInAmount().add(defaultAmount(row.getPurchaseInAmount())));
        summary.setPaymentAmount(summary.getPaymentAmount().add(defaultAmount(row.getPaymentAmount())));
        summary.setBalance(summary.getBalance().add(defaultAmount(row.getBalance())));
        summary.setOpeningPayableBalance(summary.getOpeningPayableBalance()
                .add(defaultAmount(row.getOpeningPayableBalance())));
        summary.setMiscPayableAmount(summary.getMiscPayableAmount().add(defaultAmount(row.getMiscPayableAmount())));
        summary.setUnclearedPrepayment(summary.getUnclearedPrepayment()
                .add(defaultAmount(row.getUnclearedPrepayment())));
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
    public List<ErpPayableAccountDO> getPayableAccountList(ErpPayableAccountPageReqVO reqVO,
                                                            ErpFinanceVisibleScope documentScope) {
        SupplierVisibleScope scope = getSupplierVisibleScope();
        if (scope == null || documentScope == null || !documentScope.hasAccess()) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> payableAccountMapper.selectList(reqVO,
                scope.getDeptIds(), scope.getSelfUserId(), scope.isAll(), documentScope.getDeptIds(),
                documentScope.getSelfUserId(), documentScope.isAll()));
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
    public ErpAccountDrilldownPageRespVO<ErpPayableDetailRespVO> getPayableDrilldownPage(
            ErpPayableDrilldownReqVO reqVO) {
        List<ErpPayableDetailRespVO> rows = getPayableDrilldownList(reqVO);
        ErpPayableDrilldownTypeEnum type = ErpPayableDrilldownTypeEnum.fromValue(reqVO.getDetailType());
        ErpAccountDrilldownSummaryRespVO summary = buildDrilldownSummary(rows, type,
                resolveEmptyBalanceAmount(reqVO, rows, type));
        List<ErpPayableDetailRespVO> sortedRows = sortDrilldownRows(rows, reqVO.getOrderField(),
                reqVO.getOrderDirection(), reqVO.isApproveTimeBasis());
        int from = Math.min((reqVO.getPageNo() - 1) * reqVO.getPageSize(), sortedRows.size());
        int to = Math.min(from + reqVO.getPageSize(), sortedRows.size());
        return new ErpAccountDrilldownPageRespVO<>(new ArrayList<>(sortedRows.subList(from, to)),
                (long) sortedRows.size(), summary);
    }

    @Override
    public List<ErpPayableDetailRespVO> getPayableDrilldownList(ErpPayableDrilldownReqVO reqVO) {
        supplierService.validateSupplier(reqVO.getSupplierId());
        ErpFinanceVisibleScope scope = getPayableVisibleScope();
        if (scope == null || !scope.hasAccess()) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> buildDrilldownRows(reqVO, scope));
    }

    private List<ErpPayableDetailRespVO> buildDrilldownRows(ErpPayableDrilldownReqVO reqVO,
                                                            ErpFinanceVisibleScope scope) {
        ErpPayableDrilldownTypeEnum type = ErpPayableDrilldownTypeEnum.fromValue(reqVO.getDetailType());
        if (type == ErpPayableDrilldownTypeEnum.MISC_PAYABLE) {
            return buildMiscPayableRows(reqVO, scope);
        }
        if (type == ErpPayableDrilldownTypeEnum.OPENING_BALANCE && reqVO.getStartTime() == null) {
            return Collections.emptyList();
        }
        ErpPayableDetailReqVO detailReqVO = new ErpPayableDetailReqVO();
        detailReqVO.setSupplierId(reqVO.getSupplierId());
        detailReqVO.setDateBasis(reqVO.getDateBasis());
        if (reqVO.isApproveTimeBasis()) {
            detailReqVO.setApproveTime(type == ErpPayableDrilldownTypeEnum.OPENING_BALANCE
                    ? new LocalDateTime[]{null, reqVO.getStartTime()} : reqVO.getApproveTime());
        } else {
            detailReqVO.setBizTime(type == ErpPayableDrilldownTypeEnum.OPENING_BALANCE
                    ? new LocalDateTime[]{null, reqVO.getStartTime()} : reqVO.getBizTime());
        }
        List<ErpPayableDetailRespVO> rows = buildPayableDetailList(detailReqVO, scope).stream()
                .filter(row -> matchesDrilldownType(row, type)).collect(Collectors.toList());
        applyDrilldownAmounts(rows, type);
        return rows;
    }

    private boolean matchesDrilldownType(ErpPayableDetailRespVO row, ErpPayableDrilldownTypeEnum type) {
        if (type == ErpPayableDrilldownTypeEnum.BALANCE
                || type == ErpPayableDrilldownTypeEnum.OPENING_BALANCE) {
            return true;
        }
        if (type == ErpPayableDrilldownTypeEnum.PURCHASE_IN) {
            return ErpBizTypeEnum.PURCHASE_IN.getType().equals(row.getBizType());
        }
        if (type == ErpPayableDrilldownTypeEnum.PURCHASE_RETURN) {
            return ErpBizTypeEnum.PURCHASE_RETURN.getType().equals(row.getBizType());
        }
        if (type == ErpPayableDrilldownTypeEnum.PRICE_ADJUST) {
            return ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType().equals(row.getBizType());
        }
        if (type == ErpPayableDrilldownTypeEnum.PAYABLE_ADJUST) {
            return "应付调账".equals(row.getDocType());
        }
        if (type == ErpPayableDrilldownTypeEnum.PAYMENT) {
            return "付款单".equals(row.getDocType());
        }
        return type == ErpPayableDrilldownTypeEnum.WRITE_OFF && "核销".equals(row.getDocType());
    }

    private void applyDrilldownAmounts(List<ErpPayableDetailRespVO> rows, ErpPayableDrilldownTypeEnum type) {
        boolean balanceType = type == ErpPayableDrilldownTypeEnum.BALANCE
                || type == ErpPayableDrilldownTypeEnum.OPENING_BALANCE;
        BigDecimal cumulative = BigDecimal.ZERO;
        for (ErpPayableDetailRespVO row : rows) {
            BigDecimal change;
            if (balanceType) {
                change = getChangeAmount(row);
            } else if (type == ErpPayableDrilldownTypeEnum.PURCHASE_RETURN) {
                change = defaultAmount(getChangeAmount(row)).abs();
            } else if (type == ErpPayableDrilldownTypeEnum.PAYMENT) {
                change = defaultAmount(row.getPaymentAmount()).abs();
            } else if (type == ErpPayableDrilldownTypeEnum.WRITE_OFF) {
                change = defaultAmount(row.getWriteOffAmount()).abs();
            } else {
                change = defaultAmount(row.getIncreaseAmount());
            }
            row.setChangeAmount(change);
            if (!balanceType) {
                row.setPrevBalance(cumulative);
                cumulative = cumulative.add(change);
                row.setBalance(cumulative);
            }
        }
    }

    private List<ErpPayableDetailRespVO> buildMiscPayableRows(ErpPayableDrilldownReqVO reqVO,
                                                               ErpFinanceVisibleScope scope) {
        LambdaQueryWrapperX<ErpPayableMiscDO> query = new LambdaQueryWrapperX<ErpPayableMiscDO>()
                .eq(ErpPayableMiscDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPayableMiscDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (reqVO.isApproveTimeBasis()) {
            query.geIfPresent(ErpPayableMiscDO::getApproveTime, reqVO.getStartTime())
                    .ltIfPresent(ErpPayableMiscDO::getApproveTime, reqVO.getEndTime());
        } else {
            query.geIfPresent(ErpPayableMiscDO::getBizTime, reqVO.getStartTime())
                    .leIfPresent(ErpPayableMiscDO::getBizTime, reqVO.getEndTime());
        }
        applyMiscScope(query, scope);
        List<ErpMiscSettlementLedgerRow> ledgerRows = payableMiscMapper.selectSettlementLedger(query
                .orderByAsc(reqVO.isApproveTimeBasis(), ErpPayableMiscDO::getApproveTime)
                .orderByAsc(!reqVO.isApproveTimeBasis(), ErpPayableMiscDO::getBizTime)
                .orderByAsc(ErpPayableMiscDO::getNo)
                .orderByAsc(ErpPayableMiscDO::getId));
        List<ErpPayableDetailRespVO> rows = new ArrayList<>(ledgerRows.size());
        BigDecimal cumulative = BigDecimal.ZERO;
        for (ErpMiscSettlementLedgerRow item : ledgerRows) {
            BigDecimal change = defaultAmount(item.getAmount()).negate();
            ErpPayableDetailRespVO row = new ErpPayableDetailRespVO();
            row.setDocType(item.getDocumentId() == null ? "其他应付" : "其他应付付款");
            row.setBizType(item.getDocumentId() == null ? ErpBizTypeEnum.PAYABLE_MISC.getType() : null);
            row.setBizId(item.getDocumentId() == null ? item.getId() : item.getDocumentId());
            row.setDocDate(item.getBizTime());
            row.setApproveTime(item.getApproveTime());
            row.setConfirmTime(item.getApproveTime());
            row.setEventKey(buildEventKey(item.getDocumentId() == null ? "70" : "71",
                    item.getDocumentId() == null ? item.getId() : item.getDocumentId()));
            row.setDocNo(item.getNo());
            row.setDeptId(item.getDeptId());
            row.setRemark(item.getRemark());
            row.setIncreaseAmount(change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO);
            row.setPaymentAmount(change.compareTo(BigDecimal.ZERO) < 0 ? change.abs() : BigDecimal.ZERO);
            row.setPaymentDisplayAmount(row.getPaymentAmount());
            row.setWriteOffAmount(BigDecimal.ZERO);
            row.setChangeAmount(change);
            row.setPrevBalance(cumulative);
            cumulative = cumulative.add(change);
            row.setBalance(cumulative);
            rows.add(row);
        }
        fillDeptNames(rows);
        return rows;
    }

    private void applyMiscScope(LambdaQueryWrapperX<ErpPayableMiscDO> query, ErpFinanceVisibleScope scope) {
        if (scope.isAll()) {
            return;
        }
        if (!scope.getDeptIds().isEmpty() && scope.getSelfUserId() != null) {
            query.and(wrapper -> wrapper.in(ErpPayableMiscDO::getDeptId, scope.getDeptIds())
                    .or().eq(ErpPayableMiscDO::getHandlerId, scope.getSelfUserId()));
        } else if (!scope.getDeptIds().isEmpty()) {
            query.in(ErpPayableMiscDO::getDeptId, scope.getDeptIds());
        } else {
            query.eq(ErpPayableMiscDO::getHandlerId, scope.getSelfUserId());
        }
    }

    private ErpAccountDrilldownSummaryRespVO buildDrilldownSummary(List<ErpPayableDetailRespVO> rows,
                                                                    ErpPayableDrilldownTypeEnum type,
                                                                    BigDecimal emptyBalanceAmount) {
        ErpAccountDrilldownSummaryRespVO summary = new ErpAccountDrilldownSummaryRespVO();
        summary.setDetailCount((long) rows.size());
        BigDecimal opening = rows.isEmpty() ? defaultAmount(emptyBalanceAmount)
                : defaultAmount(rows.get(0).getPrevBalance());
        BigDecimal increase = rows.stream().map(ErpPayableDetailRespVO::getChangeAmount)
                .map(this::defaultAmount).filter(value -> value.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal decrease = rows.stream().map(ErpPayableDetailRespVO::getChangeAmount)
                .map(this::defaultAmount).filter(value -> value.signum() < 0)
                .map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ending = rows.isEmpty() ? opening : defaultAmount(rows.get(rows.size() - 1).getBalance());
        boolean balanceType = type == ErpPayableDrilldownTypeEnum.BALANCE
                || type == ErpPayableDrilldownTypeEnum.OPENING_BALANCE;
        summary.setOpeningAmount(opening);
        summary.setIncreaseAmount(increase);
        summary.setDecreaseAmount(decrease);
        summary.setTargetAmount(balanceType ? ending : increase.subtract(decrease));
        summary.setEndingAmount(ending);
        return summary;
    }

    private BigDecimal resolveEmptyBalanceAmount(ErpPayableDrilldownReqVO reqVO,
                                                  List<ErpPayableDetailRespVO> rows,
                                                  ErpPayableDrilldownTypeEnum type) {
        if (!rows.isEmpty() || type != ErpPayableDrilldownTypeEnum.BALANCE || reqVO.getStartTime() == null) {
            return BigDecimal.ZERO;
        }
        ErpFinanceVisibleScope scope = getPayableVisibleScope();
        if (scope == null || !scope.hasAccess()) {
            return BigDecimal.ZERO;
        }
        ErpPayableDetailReqVO detailReqVO = new ErpPayableDetailReqVO();
        detailReqVO.setSupplierId(reqVO.getSupplierId());
        detailReqVO.setDateBasis(reqVO.getDateBasis());
        detailReqVO.setBizTime(reqVO.getBizTime());
        detailReqVO.setApproveTime(reqVO.getApproveTime());
        return DataPermissionUtils.executeIgnore(() -> getInitialBalance(detailReqVO, scope));
    }

    private List<ErpPayableDetailRespVO> sortDrilldownRows(List<ErpPayableDetailRespVO> rows,
                                                            String field, String direction,
                                                            boolean approveTimeBasis) {
        if (field == null || direction == null) {
            List<ErpPayableDetailRespVO> defaults = new ArrayList<>(rows);
            if (approveTimeBasis) defaults.sort(approvalDisplayComparator());
            return defaults;
        }
        Comparator<ErpPayableDetailRespVO> comparator;
        switch (field) {
            case "docType": comparator = comparing(ErpPayableDetailRespVO::getDocType); break;
            case "docNo": comparator = comparing(ErpPayableDetailRespVO::getDocNo); break;
            case "changeAmount": comparator = comparing(ErpPayableDetailRespVO::getChangeAmount); break;
            case "increaseAmount": comparator = comparing(ErpPayableDetailRespVO::getIncreaseAmount); break;
            case "paymentAmount": comparator = comparing(ErpPayableDetailRespVO::getPaymentAmount); break;
            case "paymentDisplayAmount": comparator = comparing(ErpPayableDetailRespVO::getPaymentDisplayAmount); break;
            case "writeOffAmount": comparator = comparing(ErpPayableDetailRespVO::getWriteOffAmount); break;
            case "balance": comparator = comparing(ErpPayableDetailRespVO::getBalance); break;
            case "deptName": comparator = comparing(ErpPayableDetailRespVO::getDeptName); break;
            case "docDate": comparator = comparing(ErpPayableDetailRespVO::getDocDate); break;
            case "approveTime": comparator = comparing(ErpPayableDetailRespVO::getApproveTime); break;
            default: return new ArrayList<>(rows);
        }
        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }
        List<ErpPayableDetailRespVO> sorted = new ArrayList<>(rows);
        sorted.sort(comparator.thenComparing(approvalDisplayComparator()));
        return sorted;
    }

    private Comparator<ErpPayableDetailRespVO> approvalDisplayComparator() {
        return Comparator.comparing(ErpPayableDetailRespVO::getApproveTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ErpPayableDetailRespVO::getEventKey,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private <U extends Comparable<? super U>> Comparator<ErpPayableDetailRespVO> comparing(
            Function<ErpPayableDetailRespVO, U> extractor) {
        return Comparator.comparing(extractor, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    @Override
    public Long writeOffPayable(ErpPayableWriteOffReqVO reqVO) {
        cn.iocoder.yudao.module.erp.service.common.ErpFinanceDecimalRules.validate(reqVO);
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
        copy.setDateBasis(reqVO.getDateBasis());
        if (reqVO.isApproveTimeBasis()) {
            copy.setApproveTime(new LocalDateTime[]{null, reqVO.getStartTime()});
        } else {
            copy.setBizTime(new LocalDateTime[]{null, reqVO.getStartTime()});
        }

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
                .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (reqVO.isApproveTimeBasis()) {
            purchaseInQuery.geIfPresent(ErpPurchaseInDO::getApproveTime, reqVO.getStartTime())
                    .ltIfPresent(ErpPurchaseInDO::getApproveTime, reqVO.getEndTime());
        } else {
            purchaseInQuery.geIfPresent(ErpPurchaseInDO::getInTime, reqVO.getStartTime())
                    .ltIfPresent(ErpPurchaseInDO::getInTime, reqVO.getEndTime());
        }
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
                .eq(ErpPurchaseReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (reqVO.isApproveTimeBasis()) {
            purchaseReturnQuery.geIfPresent(ErpPurchaseReturnDO::getApproveTime, reqVO.getStartTime())
                    .ltIfPresent(ErpPurchaseReturnDO::getApproveTime, reqVO.getEndTime());
        } else {
            purchaseReturnQuery.geIfPresent(ErpPurchaseReturnDO::getReturnTime, reqVO.getStartTime())
                    .ltIfPresent(ErpPurchaseReturnDO::getReturnTime, reqVO.getEndTime());
        }
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
            fillSourceLink(row, "PURCHASE_ORDER", item.getOrderId());
            row.setFactoryOrderNo(item.getFactoryOrderNo());
            row.setInvoiceStatus(item.getInvoiceType());
            row.setRemark(item.getRemark());
            row.setVoucherSourceBizType(ErpVoucherSourceBizTypeEnum.PURCHASE_RETURN.getType());
            setLedgerIdentity(row, "20", item.getId(), item.getApproveTime());
            rows.add(row);
        });

        LambdaQueryWrapperX<ErpPurchasePriceAdjustDO> priceAdjustQuery = new LambdaQueryWrapperX<ErpPurchasePriceAdjustDO>()
                .eq(ErpPurchasePriceAdjustDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPurchasePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (reqVO.isApproveTimeBasis()) {
            priceAdjustQuery.geIfPresent(ErpPurchasePriceAdjustDO::getApproveTime, reqVO.getStartTime())
                    .ltIfPresent(ErpPurchasePriceAdjustDO::getApproveTime, reqVO.getEndTime());
        } else {
            priceAdjustQuery.geIfPresent(ErpPurchasePriceAdjustDO::getAdjustTime, reqVO.getStartTime())
                    .ltIfPresent(ErpPurchasePriceAdjustDO::getAdjustTime, reqVO.getEndTime());
        }
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
            setLedgerIdentity(row, "30", item.getId(), item.getApproveTime());
            row.setVoucherSourceBizType(ErpVoucherSourceBizTypeEnum.PURCHASE_PRICE_ADJUST.getType());
            rows.add(row);
        });

        LambdaQueryWrapperX<ErpFinancePaymentDO> paymentQuery = new LambdaQueryWrapperX<ErpFinancePaymentDO>()
                .eq(ErpFinancePaymentDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpFinancePaymentDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (reqVO.isApproveTimeBasis()) {
            paymentQuery.geIfPresent(ErpFinancePaymentDO::getApproveTime, reqVO.getStartTime())
                    .ltIfPresent(ErpFinancePaymentDO::getApproveTime, reqVO.getEndTime());
        } else {
            paymentQuery.geIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getStartTime())
                    .ltIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getEndTime());
        }
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
            row.setPaymentDisplayAmount(resolvePaymentDisplayAmount(item, row.getPaymentAmount()));
            row.setBusinessUserId(item.getFinanceUserId());
            row.setRemark(item.getRemark());
            row.setVoucherSourceBizType(ErpVoucherSourceBizTypeEnum.PAYMENT.getType());
            setLedgerIdentity(row, "40", item.getId(), item.getApproveTime());
            rows.add(row);
        });

        payableWriteOffMapper.selectListBySupplierId(reqVO.getSupplierId(), reqVO.getStartTime(), reqVO.getEndTime(),
                        scope.getDeptIds(), scope.getSelfUserId(), scope.isAll())
                .forEach(item -> {
                    ErpPayableDetailRespVO row = buildRow("核销", item.getBizType(), item.getBizId(),
                            item.getWriteOffTime(), item.getBizNo() == null ? String.valueOf(item.getId()) : item.getBizNo(),
                            negateAmount(item.getWriteOffAmount()), true, item.getDeptId());
                    setLedgerIdentity(row, "50", item.getId(), item.getWriteOffTime());
                    rows.add(row);
                });

        LambdaQueryWrapperX<ErpPayableOtherDO> otherQuery = new LambdaQueryWrapperX<ErpPayableOtherDO>()
                .eq(ErpPayableOtherDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPayableOtherDO::getStatus, ErpAuditStatus.APPROVE.getStatus());
        if (reqVO.isApproveTimeBasis()) {
            otherQuery.geIfPresent(ErpPayableOtherDO::getApproveTime, reqVO.getStartTime())
                    .ltIfPresent(ErpPayableOtherDO::getApproveTime, reqVO.getEndTime());
        } else {
            otherQuery.geIfPresent(ErpPayableOtherDO::getBizTime,
                            reqVO.getStartTime() == null ? null : reqVO.getStartTime().toLocalDate())
                    .leIfPresent(ErpPayableOtherDO::getBizTime,
                            reqVO.getEndTime() == null ? null : reqVO.getEndTime().toLocalDate());
        }
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
                    if (PAYMENT_DISCOUNT_SOURCE_TYPE.equals(item.getSourceType())) {
                        fillSourceLink(row, "FINANCE_PAYMENT", item.getSourceId());
                    }
                    row.setRemark(item.getRemark());
                    row.setVoucherNo(item.getVoucherNo());
                    row.setVoucherSourceBizType(ErpVoucherSourceBizTypeEnum.OTHER_PAYABLE.getType());
                    setLedgerIdentity(row, "60", item.getId(), item.getApproveTime());
                    rows.add(row);
                });

        rows.forEach(row -> {
            if (row.getPaymentDisplayAmount() == null) {
                row.setPaymentDisplayAmount(row.getPaymentAmount());
            }
        });
        if (reqVO.isApproveTimeBasis()) {
            rows.sort(Comparator.comparing(ErpPayableDetailRespVO::getApproveTime,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(ErpPayableDetailRespVO::getEventKey,
                            Comparator.nullsLast(Comparator.naturalOrder())));
        } else {
            rows.sort(Comparator.comparing(ErpPayableDetailRespVO::getDocDate,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(ErpPayableDetailRespVO::getDocType)
                    .thenComparing(ErpPayableDetailRespVO::getDocNo, Comparator.nullsLast(String::compareTo)));
        }
        return rows;
    }

    private void fillSourceLink(ErpPayableDetailRespVO row, String sourceType, Long sourceId) {
        // 只使用已有的明确关联，不按单号推测来源单据。
        if (sourceId == null || sourceId <= 0 || row.getSourceNo() == null || row.getSourceNo().trim().isEmpty()) {
            return;
        }
        row.setSourceType(sourceType);
        row.setSourceId(sourceId);
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

    private BigDecimal resolvePaymentDisplayAmount(ErpFinancePaymentDO item, BigDecimal paymentAmount) {
        return item.getTotalPrice() == null ? defaultAmount(paymentAmount) : item.getTotalPrice();
    }

    private ErpPayableDetailRespVO buildPurchaseInDetailRow(ErpPurchaseInDO purchaseIn, BigDecimal allocatedAmount,
                                                            List<ErpPurchaseInItemDO> items) {
        BigDecimal originalSettlementAmount = ErpOriginalSettlementAmountUtils.calculatePurchaseIn(purchaseIn, items);
        ErpPayableDetailRespVO row = buildAllocatedRow("采购入库",
                ErpBizTypeEnum.PURCHASE_IN.getType(), purchaseIn.getId(), purchaseIn.getInTime(), purchaseIn.getNo(),
                originalSettlementAmount, allocatedAmount, originalSettlementAmount, purchaseIn.getDeptId());
        row.setPriceAdjusted(isPurchaseInPriceAdjusted(purchaseIn, items));
        setLedgerIdentity(row, "10", purchaseIn.getId(), purchaseIn.getApproveTime());
        row.setBusinessUserName(firstNonBlank(purchaseIn.getPurchaser(), purchaseIn.getHandler()));
        row.setSourceNo(purchaseIn.getOrderNo());
        fillSourceLink(row, "PURCHASE_ORDER", purchaseIn.getOrderId());
        row.setFactoryOrderNo(purchaseIn.getFactoryOrderNo());
        row.setInvoiceStatus(resolvePurchaseInInvoiceStatus(purchaseIn));
        row.setRemark(purchaseIn.getRemark());
        row.setVoucherSourceBizType(ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType());
        return row;
    }

    private void setLedgerIdentity(ErpPayableDetailRespVO row, String sourceOrder, Long recordId,
                                   LocalDateTime approveTime) {
        row.setApproveTime(approveTime);
        row.setConfirmTime(approveTime);
        row.setEventKey(buildEventKey(sourceOrder, recordId));
    }

    private String buildEventKey(String sourceOrder, Long recordId) {
        return sourceOrder + ":" + String.format("%020d", recordId == null ? 0L : recordId);
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
        row.setPaymentDisplayAmount(actualAmount);
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
