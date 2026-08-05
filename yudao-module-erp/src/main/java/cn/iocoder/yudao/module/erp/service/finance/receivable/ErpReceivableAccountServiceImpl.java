package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableWriteOffReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableWriteOffDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableWriteOffMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_WRITEOFF_AMOUNT_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_WRITEOFF_BALANCE_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_RECEIVABLE_WRITEOFF_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_WRITEOFF_SUB_TYPE;

@Service
@Validated
public class ErpReceivableAccountServiceImpl implements ErpReceivableAccountService {

    @Resource
    private ErpReceivableAccountMapper receivableAccountMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
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
    public List<ErpReceivableDetailRespVO> getReceivableDetailList(ErpReceivableDetailReqVO reqVO) {
        customerService.validateCustomer(reqVO.getCustomerId());
        ReceivableVisibleScope scope = getReceivableVisibleScope();
        if (scope == null || !scope.hasAccess()) {
            return Collections.emptyList();
        }
        return DataPermissionUtils.executeIgnore(() -> buildReceivableDetailList(reqVO, scope));
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
        return result;
    }

    private List<ErpReceivableDetailRespVO> buildRows(ErpReceivableDetailReqVO reqVO, ReceivableVisibleScope scope) {
        List<ErpReceivableDetailRespVO> rows = new ArrayList<>();

        LambdaQueryWrapperX<ErpSaleOutDO> saleOutQuery = new LambdaQueryWrapperX<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpSaleOutDO::getOutTime, reqVO.getStartTime())
                .ltIfPresent(ErpSaleOutDO::getOutTime, reqVO.getEndTime());
        applyScope(saleOutQuery, scope, ErpSaleOutDO::getDeptId, ErpSaleOutDO::getSaleUserId);
        List<ErpSaleOutDO> saleOuts = saleOutMapper.selectList(saleOutQuery);
        Map<Long, BigDecimal> saleOutAllocated = financeReceiptItemMapper
                .selectReceiptPriceSumMapByBizIdsAndBizType(saleOuts.stream().map(ErpSaleOutDO::getId)
                        .collect(Collectors.toSet()), ErpBizTypeEnum.SALE_OUT.getType());
        saleOuts.forEach(item -> rows.add(buildAllocatedRow("销售出库", ErpBizTypeEnum.SALE_OUT.getType(),
                item.getId(), item.getOutTime(), item.getNo(), item.getTotalPrice(),
                saleOutAllocated.get(item.getId()))));

        LambdaQueryWrapperX<ErpSaleReturnDO> saleReturnQuery = new LambdaQueryWrapperX<ErpSaleReturnDO>()
                .eq(ErpSaleReturnDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpSaleReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpSaleReturnDO::getReturnTime, reqVO.getStartTime())
                .ltIfPresent(ErpSaleReturnDO::getReturnTime, reqVO.getEndTime());
        applyScope(saleReturnQuery, scope, ErpSaleReturnDO::getDeptId, ErpSaleReturnDO::getSaleUserId);
        List<ErpSaleReturnDO> saleReturns = saleReturnMapper.selectList(saleReturnQuery);
        Map<Long, BigDecimal> saleReturnAllocated = financeReceiptItemMapper
                .selectReceiptPriceSumMapByBizIdsAndBizType(saleReturns.stream().map(ErpSaleReturnDO::getId)
                        .collect(Collectors.toSet()), ErpBizTypeEnum.SALE_RETURN.getType());
        saleReturns.forEach(item -> rows.add(buildAllocatedRow("销售退货", ErpBizTypeEnum.SALE_RETURN.getType(),
                item.getId(), item.getReturnTime(), item.getNo(), negateAmount(item.getTotalPrice()),
                saleReturnAllocated.get(item.getId()))));

        LambdaQueryWrapperX<ErpSalePriceAdjustDO> priceAdjustQuery = new LambdaQueryWrapperX<ErpSalePriceAdjustDO>()
                .eq(ErpSalePriceAdjustDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpSalePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpSalePriceAdjustDO::getAdjustDate, reqVO.getStartTime())
                .ltIfPresent(ErpSalePriceAdjustDO::getAdjustDate, reqVO.getEndTime());
        applyScope(priceAdjustQuery, scope, ErpSalePriceAdjustDO::getDeptId, ErpSalePriceAdjustDO::getAdjustUserId);
        List<ErpSalePriceAdjustDO> priceAdjusts = salePriceAdjustMapper.selectList(priceAdjustQuery);
        Map<Long, BigDecimal> priceAdjustAllocated = financeReceiptItemMapper
                .selectReceiptPriceSumMapByBizIdsAndBizType(priceAdjusts.stream().map(ErpSalePriceAdjustDO::getId)
                        .collect(Collectors.toSet()), ErpBizTypeEnum.SALE_PRICE_ADJUST.getType());
        priceAdjusts.forEach(item -> rows.add(buildAllocatedRow("销售调价",
                ErpBizTypeEnum.SALE_PRICE_ADJUST.getType(), item.getId(), item.getAdjustDate(), item.getNo(),
                item.getTotalAdjustPrice(), priceAdjustAllocated.get(item.getId()))));

        LambdaQueryWrapperX<ErpFinanceReceiptDO> receiptQuery = new LambdaQueryWrapperX<ErpFinanceReceiptDO>()
                .eq(ErpFinanceReceiptDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpFinanceReceiptDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getStartTime())
                .ltIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getEndTime());
        applyScope(receiptQuery, scope, ErpFinanceReceiptDO::getDeptId, ErpFinanceReceiptDO::getFinanceUserId);
        financeReceiptMapper.selectList(receiptQuery)
                .forEach(item -> rows.add(buildRow("收款单", null, item.getId(),
                        item.getReceiptTime(), item.getNo(), negateAmount(item.getTotalPrice()), false)));

        receivableWriteOffMapper.selectListByCustomerId(reqVO.getCustomerId(), reqVO.getStartTime(), reqVO.getEndTime(),
                        scope.getDeptIds(), scope.getSelfUserId(), scope.isAll())
                .forEach(item -> rows.add(buildRow("核销", item.getBizType(), item.getBizId(),
                        item.getWriteOffTime(), item.getBizNo() == null ? String.valueOf(item.getId()) : item.getBizNo(),
                        negateAmount(item.getWriteOffAmount()), true)));

        LambdaQueryWrapperX<ErpReceivableOtherDO> otherQuery = new LambdaQueryWrapperX<ErpReceivableOtherDO>()
                .eq(ErpReceivableOtherDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpReceivableOtherDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpReceivableOtherDO::getBizTime, reqVO.getStartTime() == null ? null : reqVO.getStartTime().toLocalDate())
                .leIfPresent(ErpReceivableOtherDO::getBizTime, reqVO.getEndTime() == null ? null : reqVO.getEndTime().toLocalDate());
        applyScope(otherQuery, scope, ErpReceivableOtherDO::getDeptId, ErpReceivableOtherDO::getHandlerId);
        receivableOtherMapper.selectList(otherQuery)
                .forEach(item -> rows.add(buildRow("其他应收", null, item.getId(),
                        item.getBizTime() == null ? null : item.getBizTime().atStartOfDay(),
                        item.getNo(), item.getReceivableAmount(), false)));

        rows.sort(Comparator.comparing(ErpReceivableDetailRespVO::getDocDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ErpReceivableDetailRespVO::getDocType)
                .thenComparing(ErpReceivableDetailRespVO::getDocNo, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    private BigDecimal negateAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.negate();
    }

    private ErpReceivableDetailRespVO buildRow(String docType, Integer bizType, Long bizId,
                                               LocalDateTime docDate, String docNo, BigDecimal amount,
                                               boolean writeOff) {
        BigDecimal actualAmount = amount == null ? BigDecimal.ZERO : amount;
        ErpReceivableDetailRespVO row = new ErpReceivableDetailRespVO();
        row.setDocType(docType);
        row.setBizType(bizType);
        row.setBizId(bizId);
        row.setDocDate(docDate);
        row.setDocNo(docNo);
        row.setIncreaseAmount(actualAmount.compareTo(BigDecimal.ZERO) > 0 ? actualAmount : BigDecimal.ZERO);
        row.setReceiptAmount(actualAmount.compareTo(BigDecimal.ZERO) < 0 && !writeOff ? actualAmount.abs() : BigDecimal.ZERO);
        row.setWriteOffAmount(actualAmount.compareTo(BigDecimal.ZERO) < 0 && writeOff ? actualAmount.abs() : BigDecimal.ZERO);
        return row;
    }

    private ErpReceivableDetailRespVO buildAllocatedRow(String docType, Integer bizType, Long bizId,
                                                        LocalDateTime docDate, String docNo, BigDecimal amount,
                                                        BigDecimal allocatedAmount) {
        ErpReceivableDetailRespVO row = buildRow(docType, bizType, bizId, docDate, docNo, amount, false);
        row.setAllocatedAmount(allocatedAmount == null ? BigDecimal.ZERO : allocatedAmount.abs());
        return row;
    }

    private BigDecimal getChangeAmount(ErpReceivableDetailRespVO row) {
        BigDecimal increaseAmount = row.getIncreaseAmount() == null ? BigDecimal.ZERO : row.getIncreaseAmount();
        BigDecimal receiptAmount = row.getReceiptAmount() == null ? BigDecimal.ZERO : row.getReceiptAmount();
        return increaseAmount.subtract(receiptAmount);
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
