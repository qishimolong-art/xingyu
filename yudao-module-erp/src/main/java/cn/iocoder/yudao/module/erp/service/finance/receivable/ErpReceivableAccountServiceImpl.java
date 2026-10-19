package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDrilldownReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableWriteOffReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpAccountDrilldownPageRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpAccountDrilldownSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpMiscSettlementLedgerRow;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableWriteOffDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableWriteOffMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableDrilldownTypeEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpOriginalSettlementAmountUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_WRITEOFF_AMOUNT_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_WRITEOFF_BALANCE_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_RECEIVABLE_WRITEOFF_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_WRITEOFF_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherService.RECEIPT_DISCOUNT_SOURCE_TYPE;

@Service
@Validated
public class ErpReceivableAccountServiceImpl implements ErpReceivableAccountService {

    private static final long SUMMARY_BATCH_SIZE = 500L;

    @Resource
    private ErpReceivableAccountMapper receivableAccountMapper;
    @Resource
    private ErpReceivableMiscMapper receivableMiscMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpSaleReturnMapper saleReturnMapper;
    @Resource
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;
    @Resource
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Resource
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;
    @Resource
    private ErpReceivableOtherMapper receivableOtherMapper;
    @Resource
    private ErpReceivableWriteOffMapper receivableWriteOffMapper;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpReceivableDetailDisplayService detailDisplayService;

    @Override
    public PageResult<ErpReceivableAccountDO> getReceivableAccountPage(ErpReceivableAccountPageReqVO reqVO) {
        CustomerVisibleScope customerScope = getCustomerVisibleScope();
        ReceivableVisibleScope documentScope = getReceivableVisibleScope();
        if (customerScope == null || documentScope == null || !documentScope.hasAccess()) {
            return PageResult.empty();
        }
        return DataPermissionUtils.executeIgnore(() ->
                receivableAccountMapper.selectPage(reqVO, customerScope.getDeptIds(), customerScope.getSelfUserId(),
                        customerScope.isAll(), documentScope.getDeptIds(), documentScope.getSelfUserId(),
                        documentScope.isAll()));
    }

    @Override
    public ErpReceivableAccountSummaryRespVO getReceivableAccountSummary(ErpReceivableAccountPageReqVO reqVO) {
        CustomerVisibleScope customerScope = getCustomerVisibleScope();
        ReceivableVisibleScope documentScope = getReceivableVisibleScope();
        if (customerScope == null || documentScope == null || !documentScope.hasAccess()) {
            return new ErpReceivableAccountSummaryRespVO();
        }
        return DataPermissionUtils.executeIgnore(() ->
                calculateReceivableAccountSummary(reqVO, customerScope, documentScope));
    }

    private ErpReceivableAccountSummaryRespVO calculateReceivableAccountSummary(
            ErpReceivableAccountPageReqVO reqVO, CustomerVisibleScope customerScope,
            ReceivableVisibleScope documentScope) {
        ErpReceivableAccountSummaryRespVO summary = new ErpReceivableAccountSummaryRespVO();
        Map<Long, BigDecimal> customerAdvanceAmounts = new HashMap<>();
        long pageNo = 1L;
        while (true) {
            Page<ErpReceivableAccountDO> page = new Page<>(pageNo, SUMMARY_BATCH_SIZE, false);
            List<ErpReceivableAccountDO> rows = receivableAccountMapper.selectPageRows(page, reqVO,
                    customerScope.getDeptIds(), customerScope.getSelfUserId(), customerScope.isAll(),
                    documentScope.getDeptIds(), documentScope.getSelfUserId(), documentScope.isAll()).getRecords();
            rows.forEach(row -> addSummaryRow(summary, row, customerAdvanceAmounts));
            if (rows.size() < SUMMARY_BATCH_SIZE) {
                return summary;
            }
            pageNo++;
        }
    }

    private void addSummaryRow(ErpReceivableAccountSummaryRespVO summary, ErpReceivableAccountDO row,
                               Map<Long, BigDecimal> customerAdvanceAmounts) {
        summary.setComboCount(summary.getComboCount() + 1);
        summary.setReceivableBalance(summary.getReceivableBalance()
                .add(defaultAmount(row.getReceivableBalance())));
        summary.setOpeningReceivableBalance(summary.getOpeningReceivableBalance()
                .add(defaultAmount(row.getOpeningReceivableBalance())));
        summary.setReceiptAmount(summary.getReceiptAmount().add(defaultAmount(row.getReceiptAmount())));
        summary.setMiscReceivableAmount(summary.getMiscReceivableAmount()
                .add(defaultAmount(row.getMiscReceivableAmount())));
        summary.setCreditBalance(summary.getCreditBalance().add(defaultAmount(row.getCreditBalance())));

        Long customerId = row.getCustomerId();
        if (customerId == null) {
            return;
        }
        BigDecimal currentAdvance = defaultAmount(row.getPreAdvanceAmount());
        BigDecimal previousAdvance = customerAdvanceAmounts.putIfAbsent(customerId, currentAdvance);
        if (previousAdvance == null) {
            summary.setCustomerCount(summary.getCustomerCount() + 1);
            summary.setPreAdvanceAmount(summary.getPreAdvanceAmount().add(currentAdvance));
        } else if (currentAdvance.compareTo(previousAdvance) > 0) {
            customerAdvanceAmounts.put(customerId, currentAdvance);
            summary.setPreAdvanceAmount(summary.getPreAdvanceAmount().add(currentAdvance.subtract(previousAdvance)));
        }
    }

    @Override
    public List<ErpReceivableAccountDO> getReceivableAccountList(ErpReceivableAccountPageReqVO reqVO) {
        CustomerVisibleScope customerScope = getCustomerVisibleScope();
        ReceivableVisibleScope documentScope = getReceivableVisibleScope();
        if (customerScope == null || documentScope == null || !documentScope.hasAccess()) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() ->
                receivableAccountMapper.selectList(reqVO, customerScope.getDeptIds(), customerScope.getSelfUserId(),
                        customerScope.isAll(), documentScope.getDeptIds(), documentScope.getSelfUserId(),
                        documentScope.isAll()));
    }

    @Override
    public List<ErpReceivableAccountDO> getReceivableAccountList(ErpReceivableAccountPageReqVO reqVO,
                                                                  ErpFinanceVisibleScope scope) {
        CustomerVisibleScope customerScope = getCustomerVisibleScope();
        if (customerScope == null || scope == null || !scope.hasAccess()) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> receivableAccountMapper.selectList(reqVO,
                customerScope.getDeptIds(), customerScope.getSelfUserId(), customerScope.isAll(),
                scope.getDeptIds(), scope.getSelfUserId(), scope.isAll()));
    }

    @Override
    public List<ErpReceivableDetailRespVO> getReceivableDetailList(ErpReceivableDetailReqVO reqVO) {
        customerService.validateCustomer(reqVO.getCustomerId());
        ReceivableVisibleScope scope = getReceivableVisibleScope();
        if (scope == null || !scope.hasAccess()) {
            return Collections.emptyList();
        }
        List<ErpReceivableDetailRespVO> rows = DataPermissionUtils.executeIgnore(() ->
                buildReceivableDetailList(reqVO, scope));
        return detailDisplayService.decorate(reqVO, rows);
    }

    @Override
    public List<ErpReceivableDetailRespVO> getReceivableDetailList(ErpReceivableDetailReqVO reqVO,
                                                                   ErpFinanceVisibleScope scope) {
        customerService.validateCustomer(reqVO.getCustomerId());
        if (scope == null || !scope.hasAccess()) {
            return Collections.emptyList();
        }
        ReceivableVisibleScope localScope = new ReceivableVisibleScope(scope.isAll(), scope.getDeptIds(),
                scope.getSelfUserId());
        return DataPermissionUtils.executeIgnore(() -> buildReceivableDetailList(reqVO, localScope));
    }

    @Override
    public ErpAccountDrilldownPageRespVO<ErpReceivableDetailRespVO> getReceivableDrilldownPage(
            ErpReceivableDrilldownReqVO reqVO) {
        List<ErpReceivableDetailRespVO> rows = getReceivableDrilldownList(reqVO);
        ErpReceivableDrilldownTypeEnum type = ErpReceivableDrilldownTypeEnum.fromValue(reqVO.getDetailType());
        ErpAccountDrilldownSummaryRespVO summary = buildDrilldownSummary(rows, type,
                resolveEmptyBalanceAmount(reqVO, rows, type));
        List<ErpReceivableDetailRespVO> sortedRows = sortDrilldownRows(rows, reqVO.getOrderField(),
                reqVO.getOrderDirection());
        int from = Math.min((reqVO.getPageNo() - 1) * reqVO.getPageSize(), sortedRows.size());
        int to = Math.min(from + reqVO.getPageSize(), sortedRows.size());
        return new ErpAccountDrilldownPageRespVO<>(new ArrayList<>(sortedRows.subList(from, to)),
                (long) sortedRows.size(), summary);
    }

    @Override
    public List<ErpReceivableDetailRespVO> getReceivableDrilldownList(ErpReceivableDrilldownReqVO reqVO) {
        customerService.validateCustomer(reqVO.getCustomerId());
        ReceivableVisibleScope scope = getReceivableVisibleScope();
        if (scope == null || !scope.hasAccess()) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> buildDrilldownRows(reqVO, scope));
    }

    private List<ErpReceivableDetailRespVO> buildDrilldownRows(ErpReceivableDrilldownReqVO reqVO,
                                                               ReceivableVisibleScope scope) {
        ErpReceivableDrilldownTypeEnum type = ErpReceivableDrilldownTypeEnum.fromValue(reqVO.getDetailType());
        if (type == ErpReceivableDrilldownTypeEnum.MISC_RECEIVABLE) {
            return buildMiscReceivableRows(reqVO, scope);
        }
        if (type == ErpReceivableDrilldownTypeEnum.OPENING_BALANCE && reqVO.getStartTime() == null) {
            return Collections.emptyList();
        }
        ErpReceivableDetailReqVO detailReqVO = new ErpReceivableDetailReqVO();
        detailReqVO.setCustomerId(reqVO.getCustomerId());
        detailReqVO.setDeptId(reqVO.getDeptId());
        detailReqVO.setDeptUnassigned(reqVO.getDeptUnassigned());
        detailReqVO.setBizTime(type == ErpReceivableDrilldownTypeEnum.OPENING_BALANCE
                ? new LocalDateTime[]{null, reqVO.getStartTime()} : reqVO.getBizTime());
        List<ErpReceivableDetailRespVO> rows = detailDisplayService.decorate(detailReqVO,
                buildReceivableDetailList(detailReqVO, scope));
        rows = rows.stream().filter(row -> matchesDrilldownType(row, type)).collect(Collectors.toList());
        applyDrilldownAmounts(rows, type);
        return rows;
    }

    private boolean matchesDrilldownType(ErpReceivableDetailRespVO row,
                                         ErpReceivableDrilldownTypeEnum type) {
        if (type == ErpReceivableDrilldownTypeEnum.BALANCE
                || type == ErpReceivableDrilldownTypeEnum.OPENING_BALANCE) {
            return true;
        }
        if (type == ErpReceivableDrilldownTypeEnum.SALE_OUT) {
            return ErpBizTypeEnum.SALE_OUT.getType().equals(row.getBizType());
        }
        if (type == ErpReceivableDrilldownTypeEnum.SALE_RETURN) {
            return ErpBizTypeEnum.SALE_RETURN.getType().equals(row.getBizType());
        }
        return type == ErpReceivableDrilldownTypeEnum.RECEIPT && "收款单".equals(row.getDocType());
    }

    private void applyDrilldownAmounts(List<ErpReceivableDetailRespVO> rows,
                                       ErpReceivableDrilldownTypeEnum type) {
        boolean balanceType = type == ErpReceivableDrilldownTypeEnum.BALANCE
                || type == ErpReceivableDrilldownTypeEnum.OPENING_BALANCE;
        BigDecimal cumulative = BigDecimal.ZERO;
        for (ErpReceivableDetailRespVO row : rows) {
            BigDecimal change;
            if (balanceType) {
                change = getChangeAmount(row);
            } else if (type == ErpReceivableDrilldownTypeEnum.SALE_RETURN) {
                change = defaultAmount(getChangeAmount(row)).abs();
            } else if (type == ErpReceivableDrilldownTypeEnum.RECEIPT) {
                change = defaultAmount(row.getReceiptAmount()).abs();
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

    private List<ErpReceivableDetailRespVO> buildMiscReceivableRows(ErpReceivableDrilldownReqVO reqVO,
                                                                    ReceivableVisibleScope scope) {
        LambdaQueryWrapperX<ErpReceivableMiscDO> query = new LambdaQueryWrapperX<ErpReceivableMiscDO>()
                .eq(ErpReceivableMiscDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpReceivableMiscDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpReceivableMiscDO::getBizTime, reqVO.getStartTime())
                .leIfPresent(ErpReceivableMiscDO::getBizTime, reqVO.getEndTime());
        if (Boolean.TRUE.equals(reqVO.getDeptUnassigned())) {
            query.isNull(ErpReceivableMiscDO::getDeptId);
        } else {
            query.eqIfPresent(ErpReceivableMiscDO::getDeptId, reqVO.getDeptId());
        }
        applyMiscScope(query, scope);
        List<ErpMiscSettlementLedgerRow> ledgerRows = receivableMiscMapper.selectSettlementLedger(query
                .orderByAsc(ErpReceivableMiscDO::getBizTime)
                .orderByAsc(ErpReceivableMiscDO::getNo)
                .orderByAsc(ErpReceivableMiscDO::getId));
        List<ErpReceivableDetailRespVO> rows = new ArrayList<>(ledgerRows.size());
        BigDecimal cumulative = BigDecimal.ZERO;
        for (ErpMiscSettlementLedgerRow item : ledgerRows) {
            BigDecimal change = defaultAmount(item.getAmount()).negate();
            ErpReceivableDetailRespVO row = new ErpReceivableDetailRespVO();
            row.setDocType(item.getDocumentId() == null ? "其他应收" : "其他应收收款");
            row.setBizType(item.getDocumentId() == null ? ErpBizTypeEnum.RECEIVABLE_MISC.getType() : null);
            row.setBizId(item.getDocumentId() == null ? item.getId() : item.getDocumentId());
            row.setDocDate(item.getBizTime());
            row.setDocNo(item.getNo());
            row.setDeptId(item.getDeptId());
            row.setRemark(item.getRemark());
            row.setIncreaseAmount(change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO);
            row.setReceiptAmount(change.compareTo(BigDecimal.ZERO) < 0 ? change.abs() : BigDecimal.ZERO);
            row.setReceiptDisplayAmount(row.getReceiptAmount());
            row.setChangeAmount(change);
            row.setPrevBalance(cumulative);
            cumulative = cumulative.add(change);
            row.setBalance(cumulative);
            rows.add(row);
        }
        fillDeptNames(rows);
        return rows;
    }

    private void applyMiscScope(LambdaQueryWrapperX<ErpReceivableMiscDO> query, ReceivableVisibleScope scope) {
        if (scope.isAll()) {
            return;
        }
        if (!scope.getDeptIds().isEmpty() && scope.getSelfUserId() != null) {
            query.and(wrapper -> wrapper.in(ErpReceivableMiscDO::getDeptId, scope.getDeptIds())
                    .or().eq(ErpReceivableMiscDO::getHandlerId, scope.getSelfUserId()));
        } else if (!scope.getDeptIds().isEmpty()) {
            query.in(ErpReceivableMiscDO::getDeptId, scope.getDeptIds());
        } else {
            query.eq(ErpReceivableMiscDO::getHandlerId, scope.getSelfUserId());
        }
    }

    private ErpAccountDrilldownSummaryRespVO buildDrilldownSummary(List<ErpReceivableDetailRespVO> rows,
                                                                    ErpReceivableDrilldownTypeEnum type,
                                                                    BigDecimal emptyBalanceAmount) {
        ErpAccountDrilldownSummaryRespVO summary = new ErpAccountDrilldownSummaryRespVO();
        summary.setDetailCount((long) rows.size());
        BigDecimal opening = rows.isEmpty() ? defaultAmount(emptyBalanceAmount)
                : defaultAmount(rows.get(0).getPrevBalance());
        BigDecimal increase = rows.stream().map(ErpReceivableDetailRespVO::getChangeAmount)
                .map(this::defaultAmount).filter(value -> value.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal decrease = rows.stream().map(ErpReceivableDetailRespVO::getChangeAmount)
                .map(this::defaultAmount).filter(value -> value.signum() < 0)
                .map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ending = rows.isEmpty() ? opening : defaultAmount(rows.get(rows.size() - 1).getBalance());
        boolean balanceType = type == ErpReceivableDrilldownTypeEnum.BALANCE
                || type == ErpReceivableDrilldownTypeEnum.OPENING_BALANCE;
        summary.setOpeningAmount(opening);
        summary.setIncreaseAmount(increase);
        summary.setDecreaseAmount(decrease);
        summary.setTargetAmount(balanceType ? ending : increase.subtract(decrease));
        summary.setEndingAmount(ending);
        return summary;
    }

    private BigDecimal resolveEmptyBalanceAmount(ErpReceivableDrilldownReqVO reqVO,
                                                  List<ErpReceivableDetailRespVO> rows,
                                                  ErpReceivableDrilldownTypeEnum type) {
        if (!rows.isEmpty() || type != ErpReceivableDrilldownTypeEnum.BALANCE || reqVO.getStartTime() == null) {
            return BigDecimal.ZERO;
        }
        ReceivableVisibleScope scope = getReceivableVisibleScope();
        if (scope == null || !scope.hasAccess()) {
            return BigDecimal.ZERO;
        }
        ErpReceivableDetailReqVO detailReqVO = new ErpReceivableDetailReqVO();
        detailReqVO.setCustomerId(reqVO.getCustomerId());
        detailReqVO.setDeptId(reqVO.getDeptId());
        detailReqVO.setDeptUnassigned(reqVO.getDeptUnassigned());
        detailReqVO.setBizTime(reqVO.getBizTime());
        return DataPermissionUtils.executeIgnore(() -> getInitialBalance(detailReqVO, scope));
    }

    private List<ErpReceivableDetailRespVO> sortDrilldownRows(List<ErpReceivableDetailRespVO> rows,
                                                               String field, String direction) {
        if (field == null || direction == null) {
            return new ArrayList<>(rows);
        }
        Comparator<ErpReceivableDetailRespVO> comparator;
        switch (field) {
            case "docType": comparator = comparing(ErpReceivableDetailRespVO::getDocType); break;
            case "docNo": comparator = comparing(ErpReceivableDetailRespVO::getDocNo); break;
            case "changeAmount": comparator = comparing(ErpReceivableDetailRespVO::getChangeAmount); break;
            case "increaseAmount": comparator = comparing(ErpReceivableDetailRespVO::getIncreaseAmount); break;
            case "receiptAmount": comparator = comparing(ErpReceivableDetailRespVO::getReceiptAmount); break;
            case "receiptDisplayAmount": comparator = comparing(ErpReceivableDetailRespVO::getReceiptDisplayAmount); break;
            case "balance": comparator = comparing(ErpReceivableDetailRespVO::getBalance); break;
            case "deptName": comparator = comparing(ErpReceivableDetailRespVO::getDeptName); break;
            case "docDate": comparator = comparing(ErpReceivableDetailRespVO::getDocDate); break;
            default: return new ArrayList<>(rows);
        }
        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }
        List<ErpReceivableDetailRespVO> sorted = new ArrayList<>(rows);
        sorted.sort(comparator);
        return sorted;
    }

    private <U extends Comparable<? super U>> Comparator<ErpReceivableDetailRespVO> comparing(
            Function<ErpReceivableDetailRespVO, U> extractor) {
        return Comparator.comparing(extractor, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    @Override
    public Long writeOffReceivable(ErpReceivableWriteOffReqVO reqVO) {
        customerService.validateCustomer(reqVO.getCustomerId());
        ReceivableVisibleScope scope = getReceivableVisibleScope();
        if (scope == null || !scope.hasAccess()) {
            throw exception(RECEIVABLE_WRITEOFF_BALANCE_EMPTY);
        }
        ErpReceivableDetailReqVO balanceReqVO = new ErpReceivableDetailReqVO();
        balanceReqVO.setCustomerId(reqVO.getCustomerId());
        List<ErpReceivableDetailRespVO> balanceRows = DataPermissionUtils.executeIgnore(() ->
                buildRows(balanceReqVO, scope));
        BigDecimal balance = balanceRows.stream().map(this::getChangeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(RECEIVABLE_WRITEOFF_BALANCE_EMPTY);
        }
        if (reqVO.getWriteOffAmount().compareTo(balance) > 0) {
            throw exception(RECEIVABLE_WRITEOFF_AMOUNT_EXCEED, reqVO.getWriteOffAmount(), balance);
        }
        validateReceivableWriteOffAvailable(reqVO, balanceRows);
        ErpReceivableWriteOffDO writeOff = new ErpReceivableWriteOffDO();
        writeOff.setCustomerId(reqVO.getCustomerId());
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
        receivableWriteOffMapper.insert(writeOff);
        operateLogService.record(ERP_RECEIVABLE_WRITEOFF_TYPE, ERP_WRITEOFF_SUB_TYPE, writeOff.getId(),
                "核销应收账款，客户编号：" + reqVO.getCustomerId()
                        + (reqVO.getBizNo() == null ? "" : "，单据号：" + reqVO.getBizNo())
                        + "，核销金额：" + reqVO.getWriteOffAmount(), String.valueOf(writeOff.getId()));
        return writeOff.getId();
    }

    private void validateReceivableWriteOffAvailable(ErpReceivableWriteOffReqVO reqVO,
                                                     List<ErpReceivableDetailRespVO> rows) {
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
            throw exception(RECEIVABLE_WRITEOFF_BALANCE_EMPTY);
        }
        if (reqVO.getWriteOffAmount().compareTo(available) > 0) {
            throw exception(RECEIVABLE_WRITEOFF_AMOUNT_EXCEED, reqVO.getWriteOffAmount(), available);
        }
    }

    private Long resolveWriteOffDeptId(ErpReceivableWriteOffReqVO reqVO, AdminUserRespDTO loginUser) {
        if (reqVO.getBizId() != null && reqVO.getBizType() != null) {
            if (ErpBizTypeEnum.SALE_OUT.getType().equals(reqVO.getBizType())) {
                ErpSaleOutDO source = saleOutMapper.selectById(reqVO.getBizId());
                if (source != null && source.getDeptId() != null) {
                    return source.getDeptId();
                }
            } else if (ErpBizTypeEnum.SALE_RETURN.getType().equals(reqVO.getBizType())) {
                ErpSaleReturnDO source = saleReturnMapper.selectById(reqVO.getBizId());
                if (source != null && source.getDeptId() != null) {
                    return source.getDeptId();
                }
            } else if (ErpBizTypeEnum.SALE_PRICE_ADJUST.getType().equals(reqVO.getBizType())) {
                ErpSalePriceAdjustDO source = salePriceAdjustMapper.selectById(reqVO.getBizId());
                if (source != null && source.getDeptId() != null) {
                    return source.getDeptId();
                }
            }
        }
        return loginUser == null ? null : loginUser.getDeptId();
    }

    private BigDecimal getInitialBalance(ErpReceivableDetailReqVO reqVO, ReceivableVisibleScope scope) {
        if (reqVO.getStartTime() == null) {
            return BigDecimal.ZERO;
        }
        ErpReceivableDetailReqVO copy = new ErpReceivableDetailReqVO();
        copy.setCustomerId(reqVO.getCustomerId());
        copy.setDeptId(reqVO.getDeptId());
        copy.setDeptUnassigned(reqVO.getDeptUnassigned());
        copy.setBizTime(new LocalDateTime[]{null, reqVO.getStartTime()});

        BigDecimal total = BigDecimal.ZERO;
        for (ErpReceivableDetailRespVO row : buildRows(copy, scope)) {
            total = total.add(getChangeAmount(row));
        }
        return total;
    }

    private List<ErpReceivableDetailRespVO> buildReceivableDetailList(ErpReceivableDetailReqVO reqVO,
                                                                      ReceivableVisibleScope scope) {
        List<ErpReceivableDetailRespVO> rows = buildRows(reqVO, scope);
        BigDecimal balance = getInitialBalance(reqVO, scope);
        List<ErpReceivableDetailRespVO> result = new ArrayList<>(rows.size());
        for (ErpReceivableDetailRespVO row : rows) {
            BigDecimal prevBalance = balance;
            balance = balance.add(getChangeAmount(row));

            row.setPrevBalance(prevBalance);
            row.setBalance(balance);
            result.add(row);
        }
        fillDeptNames(result);
        return result;
    }

    private List<ErpReceivableDetailRespVO> buildRows(ErpReceivableDetailReqVO reqVO, ReceivableVisibleScope scope) {
        List<ErpReceivableDetailRespVO> rows = new ArrayList<>();

        LambdaQueryWrapperX<ErpSaleOutDO> saleOutQuery = new LambdaQueryWrapperX<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpSaleOutDO::getOutTime, reqVO.getStartTime())
                .ltIfPresent(ErpSaleOutDO::getOutTime, reqVO.getEndTime());
        applyDetailDeptFilter(saleOutQuery, reqVO, ErpSaleOutDO::getDeptId);
        applyScope(saleOutQuery, scope, ErpSaleOutDO::getDeptId, ErpSaleOutDO::getSaleUserId);
        List<ErpSaleOutDO> saleOuts = saleOutMapper.selectList(saleOutQuery);
        Map<Long, BigDecimal> saleOutAllocated = financeReceiptItemMapper
                .selectReceiptPriceSumMapByBizIdsAndBizType(saleOuts.stream().map(ErpSaleOutDO::getId)
                        .collect(Collectors.toSet()), ErpBizTypeEnum.SALE_OUT.getType());
        Map<Long, List<ErpSaleOutItemDO>> saleOutItemMap = saleOuts.isEmpty() ? Collections.emptyMap()
                : saleOutItemMapper.selectListByOutIds(saleOuts.stream().map(ErpSaleOutDO::getId)
                        .collect(Collectors.toSet())).stream()
                .collect(Collectors.groupingBy(ErpSaleOutItemDO::getOutId));
        saleOuts.forEach(item -> rows.add(buildSaleOutDetailRow(item, saleOutAllocated.get(item.getId()),
                saleOutItemMap.getOrDefault(item.getId(), Collections.emptyList()))));

        LambdaQueryWrapperX<ErpSaleReturnDO> saleReturnQuery = new LambdaQueryWrapperX<ErpSaleReturnDO>()
                .eq(ErpSaleReturnDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpSaleReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpSaleReturnDO::getReturnTime, reqVO.getStartTime())
                .ltIfPresent(ErpSaleReturnDO::getReturnTime, reqVO.getEndTime());
        applyDetailDeptFilter(saleReturnQuery, reqVO, ErpSaleReturnDO::getDeptId);
        applyScope(saleReturnQuery, scope, ErpSaleReturnDO::getDeptId, ErpSaleReturnDO::getSaleUserId);
        List<ErpSaleReturnDO> saleReturns = saleReturnMapper.selectList(saleReturnQuery);
        Map<Long, BigDecimal> saleReturnAllocated = financeReceiptItemMapper
                .selectReceiptPriceSumMapByBizIdsAndBizType(saleReturns.stream().map(ErpSaleReturnDO::getId)
                        .collect(Collectors.toSet()), ErpBizTypeEnum.SALE_RETURN.getType());
        saleReturns.forEach(item -> rows.add(buildAllocatedRow("销售退货", ErpBizTypeEnum.SALE_RETURN.getType(),
                item.getId(), item.getReturnTime(), item.getNo(), negateAmount(item.getTotalPrice()),
                saleReturnAllocated.get(item.getId()), item.getDeptId())));

        LambdaQueryWrapperX<ErpSalePriceAdjustDO> priceAdjustQuery = new LambdaQueryWrapperX<ErpSalePriceAdjustDO>()
                .eq(ErpSalePriceAdjustDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpSalePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpSalePriceAdjustDO::getAdjustDate, reqVO.getStartTime())
                .ltIfPresent(ErpSalePriceAdjustDO::getAdjustDate, reqVO.getEndTime());
        applyDetailDeptFilter(priceAdjustQuery, reqVO, ErpSalePriceAdjustDO::getDeptId);
        applyScope(priceAdjustQuery, scope, ErpSalePriceAdjustDO::getDeptId, ErpSalePriceAdjustDO::getAdjustUserId);
        List<ErpSalePriceAdjustDO> priceAdjusts = salePriceAdjustMapper.selectList(priceAdjustQuery);
        Map<Long, BigDecimal> priceAdjustAllocated = financeReceiptItemMapper
                .selectReceiptPriceSumMapByBizIdsAndBizType(priceAdjusts.stream().map(ErpSalePriceAdjustDO::getId)
                        .collect(Collectors.toSet()), ErpBizTypeEnum.SALE_PRICE_ADJUST.getType());
        priceAdjusts.forEach(item -> rows.add(buildAllocatedRow("销售调价",
                ErpBizTypeEnum.SALE_PRICE_ADJUST.getType(), item.getId(), item.getAdjustDate(), item.getNo(),
                item.getTotalAdjustPrice(), priceAdjustAllocated.get(item.getId()), item.getDeptId())));

        LambdaQueryWrapperX<ErpFinanceReceiptDO> receiptQuery = new LambdaQueryWrapperX<ErpFinanceReceiptDO>()
                .eq(ErpFinanceReceiptDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpFinanceReceiptDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getStartTime())
                .ltIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getEndTime());
        applyDetailDeptFilter(receiptQuery, reqVO, ErpFinanceReceiptDO::getDeptId);
        applyScope(receiptQuery, scope, ErpFinanceReceiptDO::getDeptId, ErpFinanceReceiptDO::getFinanceUserId);
        List<ErpFinanceReceiptDO> receipts = financeReceiptMapper.selectList(receiptQuery);
        Map<Long, BigDecimal> receiptAllocated = financeReceiptItemMapper
                .selectEffectivePriceSumMapByReceiptIds(receipts.stream().map(ErpFinanceReceiptDO::getId)
                        .collect(Collectors.toSet()));
        Set<Long> discountReceiptIds = receivableOtherMapper
                .selectApprovedListBySourceIds(RECEIPT_DISCOUNT_SOURCE_TYPE,
                        receipts.stream().map(ErpFinanceReceiptDO::getId).collect(Collectors.toSet()))
                .stream().map(ErpReceivableOtherDO::getSourceId).collect(Collectors.toSet());
        receipts.forEach(item -> {
            BigDecimal receiptAmount = resolveReceiptDetailReceiptAmount(item, discountReceiptIds);
            ErpReceivableDetailRespVO row = buildReceiptAllocatedRow("收款单", null, item.getId(),
                    item.getReceiptTime(), item.getNo(), receiptAmount,
                    receiptAllocated.get(item.getId()), item.getDeptId());
            row.setReceiptDisplayAmount(resolveReceiptDisplayAmount(item, receiptAmount));
            rows.add(row);
        });

        receivableWriteOffMapper.selectListByCustomerId(reqVO.getCustomerId(), reqVO.getStartTime(), reqVO.getEndTime(),
                        scope.getDeptIds(), scope.getSelfUserId(), scope.isAll(), reqVO.getDeptId(),
                        Boolean.TRUE.equals(reqVO.getDeptUnassigned()))
                .forEach(item -> {
                    ErpReceivableDetailRespVO row = buildRow("核销", item.getBizType(), item.getBizId(),
                            item.getWriteOffTime(), item.getBizNo() == null ? String.valueOf(item.getId()) : item.getBizNo(),
                            negateAmount(item.getWriteOffAmount()), true, item.getDeptId());
                    row.setRemark(item.getRemark());
                    rows.add(row);
                });

        LambdaQueryWrapperX<ErpReceivableOtherDO> otherQuery = new LambdaQueryWrapperX<ErpReceivableOtherDO>()
                .eq(ErpReceivableOtherDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpReceivableOtherDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpReceivableOtherDO::getBizTime, reqVO.getStartTime() == null ? null : reqVO.getStartTime().toLocalDate())
                .leIfPresent(ErpReceivableOtherDO::getBizTime, reqVO.getEndTime() == null ? null : reqVO.getEndTime().toLocalDate());
        applyDetailDeptFilter(otherQuery, reqVO, ErpReceivableOtherDO::getDeptId);
        applyScope(otherQuery, scope, ErpReceivableOtherDO::getDeptId, ErpReceivableOtherDO::getHandlerId);
        List<ErpReceivableOtherDO> otherReceivables = receivableOtherMapper.selectList(otherQuery);
        Map<Long, LocalDateTime> receiptDiscountTimeMap = getReceiptDiscountTimeMap(otherReceivables);
        otherReceivables
                .forEach(item -> rows.add(buildOtherReceivableRow("应收调账", null, item.getId(),
                        resolveOtherReceivableDocDate(item, receiptDiscountTimeMap),
                        item.getNo(), item.getReceivableAmount(), item.getDeptId())));

        rows.forEach(row -> {
            if (row.getReceiptDisplayAmount() == null) {
                row.setReceiptDisplayAmount(row.getReceiptAmount());
            }
        });
        rows.sort(Comparator.comparing(ErpReceivableDetailRespVO::getDocDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ErpReceivableDetailRespVO::getDocType)
                .thenComparing(ErpReceivableDetailRespVO::getDocNo, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    private Map<Long, LocalDateTime> getReceiptDiscountTimeMap(List<ErpReceivableOtherDO> otherReceivables) {
        Set<Long> receiptIds = otherReceivables.stream()
                .filter(item -> RECEIPT_DISCOUNT_SOURCE_TYPE.equals(item.getSourceType()))
                .map(ErpReceivableOtherDO::getSourceId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (receiptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return financeReceiptMapper.selectList(new LambdaQueryWrapperX<ErpFinanceReceiptDO>()
                        .in(ErpFinanceReceiptDO::getId, receiptIds))
                .stream()
                .filter(item -> item.getReceiptTime() != null)
                .collect(Collectors.toMap(ErpFinanceReceiptDO::getId, ErpFinanceReceiptDO::getReceiptTime,
                        (first, second) -> first));
    }

    private LocalDateTime resolveOtherReceivableDocDate(ErpReceivableOtherDO item,
                                                        Map<Long, LocalDateTime> receiptDiscountTimeMap) {
        if (RECEIPT_DISCOUNT_SOURCE_TYPE.equals(item.getSourceType()) && item.getSourceId() != null) {
            LocalDateTime receiptTime = receiptDiscountTimeMap.get(item.getSourceId());
            if (receiptTime != null) {
                return receiptTime;
            }
        }
        // 调账业务日期仅精确到天，明细使用首次保存的创建时间，避免丢失时分秒。
        if (item.getCreateTime() != null) {
            return item.getCreateTime();
        }
        // 兼容缺少创建时间的历史记录，不推测实际发生时间。
        return item.getBizTime() == null ? null : item.getBizTime().atStartOfDay();
    }

    private BigDecimal resolveReceiptDetailReceiptAmount(ErpFinanceReceiptDO item,
                                                          Set<Long> discountReceiptIds) {
        return discountReceiptIds.contains(item.getId()) ? item.getReceiptPrice() : item.getTotalPrice();
    }

    private BigDecimal resolveReceiptDisplayAmount(ErpFinanceReceiptDO item, BigDecimal receiptAmount) {
        return item.getTotalPrice() == null ? defaultAmount(receiptAmount) : item.getTotalPrice();
    }

    private ErpReceivableDetailRespVO buildSaleOutDetailRow(ErpSaleOutDO saleOut, BigDecimal allocatedAmount,
                                                            List<ErpSaleOutItemDO> items) {
        BigDecimal originalSettlementAmount = ErpOriginalSettlementAmountUtils
                .calculateSaleOutReceivableAccountAmount(saleOut, items);
        ErpReceivableDetailRespVO row = buildAllocatedRow("销售出库",
                ErpBizTypeEnum.SALE_OUT.getType(), saleOut.getId(), saleOut.getOutTime(), saleOut.getNo(),
                originalSettlementAmount, allocatedAmount, originalSettlementAmount, saleOut.getDeptId());
        row.setReturnStatus(saleOut.getReturnStatus());
        row.setPriceAdjusted(isSaleOutPriceAdjusted(saleOut, items));
        return row;
    }

    private boolean isSaleOutPriceAdjusted(ErpSaleOutDO saleOut, List<ErpSaleOutItemDO> items) {
        return Boolean.TRUE.equals(saleOut.getAdjusted()) || items.stream()
                .anyMatch(item -> item.getOriginalProductPrice() != null);
    }

    private BigDecimal negateAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.negate();
    }

    private ErpReceivableDetailRespVO buildRow(String docType, Integer bizType, Long bizId,
                                               LocalDateTime docDate, String docNo, BigDecimal amount,
                                               boolean writeOff) {
        return buildRow(docType, bizType, bizId, docDate, docNo, amount, writeOff, null);
    }

    private ErpReceivableDetailRespVO buildRow(String docType, Integer bizType, Long bizId,
                                               LocalDateTime docDate, String docNo, BigDecimal amount,
                                               boolean writeOff, Long deptId) {
        BigDecimal actualAmount = amount == null ? BigDecimal.ZERO : amount;
        ErpReceivableDetailRespVO row = new ErpReceivableDetailRespVO();
        row.setDocType(docType);
        row.setBizType(bizType);
        row.setBizId(bizId);
        row.setDocDate(docDate);
        row.setDocNo(docNo);
        row.setDeptId(deptId);
        row.setIncreaseAmount(writeOff ? BigDecimal.ZERO : actualAmount);
        row.setOtherReceivableAmount(BigDecimal.ZERO);
        row.setReceiptAmount(BigDecimal.ZERO);
        row.setWriteOffAmount(actualAmount.compareTo(BigDecimal.ZERO) < 0 && writeOff ? actualAmount.abs() : BigDecimal.ZERO);
        row.setWriteOffBaseAmount(actualAmount.abs());
        return row;
    }

    private ErpReceivableDetailRespVO buildOtherReceivableRow(String docType, Integer bizType, Long bizId,
                                                              LocalDateTime docDate, String docNo,
                                                              BigDecimal amount) {
        return buildOtherReceivableRow(docType, bizType, bizId, docDate, docNo, amount, null);
    }

    private ErpReceivableDetailRespVO buildOtherReceivableRow(String docType, Integer bizType, Long bizId,
                                                              LocalDateTime docDate, String docNo,
                                                              BigDecimal amount, Long deptId) {
        BigDecimal actualAmount = amount == null ? BigDecimal.ZERO : amount;
        ErpReceivableDetailRespVO row = buildRow(docType, bizType, bizId, docDate, docNo, BigDecimal.ZERO, false,
                deptId);
        row.setOtherReceivableAmount(actualAmount);
        row.setWriteOffBaseAmount(actualAmount.abs());
        return row;
    }

    private ErpReceivableDetailRespVO buildAllocatedRow(String docType, Integer bizType, Long bizId,
                                                        LocalDateTime docDate, String docNo, BigDecimal amount,
                                                        BigDecimal allocatedAmount) {
        return buildAllocatedRow(docType, bizType, bizId, docDate, docNo, amount, allocatedAmount, amount);
    }

    private ErpReceivableDetailRespVO buildAllocatedRow(String docType, Integer bizType, Long bizId,
                                                        LocalDateTime docDate, String docNo, BigDecimal amount,
                                                        BigDecimal allocatedAmount, BigDecimal writeOffBaseAmount) {
        return buildAllocatedRow(docType, bizType, bizId, docDate, docNo, amount, allocatedAmount,
                writeOffBaseAmount, null);
    }

    private ErpReceivableDetailRespVO buildAllocatedRow(String docType, Integer bizType, Long bizId,
                                                        LocalDateTime docDate, String docNo, BigDecimal amount,
                                                        BigDecimal allocatedAmount, BigDecimal writeOffBaseAmount,
                                                        Long deptId) {
        ErpReceivableDetailRespVO row = buildRow(docType, bizType, bizId, docDate, docNo, amount, false, deptId);
        row.setAllocatedAmount(allocatedAmount == null ? BigDecimal.ZERO : allocatedAmount.abs());
        row.setWriteOffBaseAmount(writeOffBaseAmount == null ? BigDecimal.ZERO : writeOffBaseAmount.abs());
        return row;
    }

    private ErpReceivableDetailRespVO buildAllocatedRow(String docType, Integer bizType, Long bizId,
                                                        LocalDateTime docDate, String docNo, BigDecimal amount,
                                                        BigDecimal allocatedAmount, Long deptId) {
        return buildAllocatedRow(docType, bizType, bizId, docDate, docNo, amount, allocatedAmount, amount, deptId);
    }

    private ErpReceivableDetailRespVO buildReceiptAllocatedRow(String docType, Integer bizType, Long bizId,
                                                               LocalDateTime docDate, String docNo,
                                                               BigDecimal amount,
                                                               BigDecimal allocatedAmount) {
        return buildReceiptAllocatedRow(docType, bizType, bizId, docDate, docNo, amount, allocatedAmount, null);
    }

    private ErpReceivableDetailRespVO buildReceiptAllocatedRow(String docType, Integer bizType, Long bizId,
                                                               LocalDateTime docDate, String docNo,
                                                               BigDecimal amount,
                                                               BigDecimal allocatedAmount, Long deptId) {
        BigDecimal actualAmount = amount == null ? BigDecimal.ZERO : amount;
        ErpReceivableDetailRespVO row = buildRow(docType, bizType, bizId, docDate, docNo, BigDecimal.ZERO, false,
                deptId);
        row.setReceiptAmount(actualAmount);
        row.setReceiptDisplayAmount(actualAmount);
        row.setAllocatedAmount(allocatedAmount == null ? BigDecimal.ZERO : allocatedAmount.abs());
        row.setWriteOffBaseAmount(actualAmount.abs());
        return row;
    }

    private void fillDeptNames(List<ErpReceivableDetailRespVO> rows) {
        Set<Long> deptIds = rows.stream()
                .map(ErpReceivableDetailRespVO::getDeptId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return;
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(deptIds);
        for (ErpReceivableDetailRespVO row : rows) {
            DeptRespDTO dept = deptMap.get(row.getDeptId());
            if (dept != null) {
                row.setDeptName(dept.getName());
            }
        }
    }

    private BigDecimal getChangeAmount(ErpReceivableDetailRespVO row) {
        BigDecimal increaseAmount = row.getIncreaseAmount() == null ? BigDecimal.ZERO : row.getIncreaseAmount();
        BigDecimal otherReceivableAmount = row.getOtherReceivableAmount() == null ? BigDecimal.ZERO : row.getOtherReceivableAmount();
        BigDecimal receiptAmount = row.getReceiptAmount() == null ? BigDecimal.ZERO : row.getReceiptAmount();
        return increaseAmount.add(otherReceivableAmount).subtract(receiptAmount);
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private CustomerVisibleScope getCustomerVisibleScope() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return null;
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(loginUserId, "erp_customer");
        if (permission == null) {
            return null;
        }
        String selfUserId = Boolean.TRUE.equals(permission.getSelf()) ? String.valueOf(loginUserId) : null;
        return new CustomerVisibleScope(Boolean.TRUE.equals(permission.getAll()), permission.getDeptIds(), selfUserId);
    }

    private ReceivableVisibleScope getReceivableVisibleScope() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return null;
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(
                loginUserId, "erp_finance_receivable_account");
        if (permission == null) {
            return null;
        }
        Long selfUserId = Boolean.TRUE.equals(permission.getSelf()) ? loginUserId : null;
        return new ReceivableVisibleScope(Boolean.TRUE.equals(permission.getAll()), permission.getDeptIds(), selfUserId);
    }

    private <T> void applyScope(LambdaQueryWrapperX<T> query, ReceivableVisibleScope scope,
                                com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> deptColumn,
                                com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> userColumn) {
        if (scope.isAll()) {
            return;
        }
        if (!scope.getDeptIds().isEmpty() && scope.getSelfUserId() != null) {
            query.and(wrapper -> wrapper.in(deptColumn, scope.getDeptIds())
                    .or().eq(userColumn, scope.getSelfUserId()));
        } else if (!scope.getDeptIds().isEmpty()) {
            query.in(deptColumn, scope.getDeptIds());
        } else {
            query.eq(userColumn, scope.getSelfUserId());
        }
    }

    private <T> void applyDetailDeptFilter(LambdaQueryWrapperX<T> query, ErpReceivableDetailReqVO reqVO,
                                           com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> deptColumn) {
        if (Boolean.TRUE.equals(reqVO.getDeptUnassigned())) {
            query.isNull(deptColumn);
        } else {
            query.eqIfPresent(deptColumn, reqVO.getDeptId());
        }
    }

    private static class ReceivableVisibleScope {
        private final boolean all;
        private final Collection<Long> deptIds;
        private final Long selfUserId;

        private ReceivableVisibleScope(boolean all, Collection<Long> deptIds, Long selfUserId) {
            this.all = all;
            this.deptIds = deptIds == null ? Collections.emptyList() : deptIds;
            this.selfUserId = selfUserId;
        }

        boolean hasAccess() { return all || !deptIds.isEmpty() || selfUserId != null; }
        boolean isAll() { return all; }
        Collection<Long> getDeptIds() { return deptIds; }
        Long getSelfUserId() { return selfUserId; }
    }

    private static class CustomerVisibleScope {

        private final boolean all;
        private final Collection<Long> deptIds;
        private final String selfUserId;

        private CustomerVisibleScope(boolean all, Collection<Long> deptIds, String selfUserId) {
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
