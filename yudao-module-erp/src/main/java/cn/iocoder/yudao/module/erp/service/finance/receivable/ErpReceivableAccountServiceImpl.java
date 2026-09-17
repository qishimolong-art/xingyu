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
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_WRITEOFF_AMOUNT_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.RECEIVABLE_WRITEOFF_BALANCE_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_RECEIVABLE_WRITEOFF_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_WRITEOFF_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherService.RECEIPT_DISCOUNT_SOURCE_TYPE;

@Service
@Validated
public class ErpReceivableAccountServiceImpl implements ErpReceivableAccountService {

    @Resource
    private ErpReceivableAccountMapper receivableAccountMapper;
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
    private ErpReceivableMiscMapper receivableMiscMapper;
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
        receipts.forEach(item -> rows.add(buildReceiptAllocatedRow("收款单", null, item.getId(),
                item.getReceiptTime(), item.getNo(),
                discountReceiptIds.contains(item.getId()) ? item.getReceiptPrice() : item.getTotalPrice(),
                receiptAllocated.get(item.getId()), item.getDeptId())));

        receivableWriteOffMapper.selectListByCustomerId(reqVO.getCustomerId(), reqVO.getStartTime(), reqVO.getEndTime(),
                        scope.getDeptIds(), scope.getSelfUserId(), scope.isAll(), reqVO.getDeptId(),
                        Boolean.TRUE.equals(reqVO.getDeptUnassigned()))
                .forEach(item -> rows.add(buildRow("核销", item.getBizType(), item.getBizId(),
                        item.getWriteOffTime(), item.getBizNo() == null ? String.valueOf(item.getId()) : item.getBizNo(),
                        negateAmount(item.getWriteOffAmount()), true, item.getDeptId())));

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

        LambdaQueryWrapperX<ErpReceivableMiscDO> miscQuery = new LambdaQueryWrapperX<ErpReceivableMiscDO>()
                .eq(ErpReceivableMiscDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpReceivableMiscDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpReceivableMiscDO::getBizTime, reqVO.getStartTime())
                .ltIfPresent(ErpReceivableMiscDO::getBizTime, reqVO.getEndTime());
        applyDetailDeptFilter(miscQuery, reqVO, ErpReceivableMiscDO::getDeptId);
        applyScope(miscQuery, scope, ErpReceivableMiscDO::getDeptId, ErpReceivableMiscDO::getHandlerId);
        List<ErpReceivableMiscDO> miscReceivables = receivableMiscMapper.selectList(miscQuery);
        Map<Long, BigDecimal> miscAllocated = financeReceiptItemMapper
                .selectReceiptPriceSumMapByBizIdsAndBizType(miscReceivables.stream().map(ErpReceivableMiscDO::getId)
                        .collect(Collectors.toSet()), ErpBizTypeEnum.RECEIVABLE_MISC.getType());
        miscReceivables.forEach(item -> rows.add(buildAllocatedRow("其他应收",
                ErpBizTypeEnum.RECEIVABLE_MISC.getType(), item.getId(), item.getBizTime(), item.getNo(),
                item.getAmount(), miscAllocated.get(item.getId()), item.getDeptId())));

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
        return item.getBizTime() == null ? null : item.getBizTime().atStartOfDay();
    }

    private ErpReceivableDetailRespVO buildSaleOutDetailRow(ErpSaleOutDO saleOut, BigDecimal allocatedAmount,
                                                            List<ErpSaleOutItemDO> items) {
        BigDecimal originalSettlementAmount = ErpOriginalSettlementAmountUtils.calculateSaleOut(saleOut, items);
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
