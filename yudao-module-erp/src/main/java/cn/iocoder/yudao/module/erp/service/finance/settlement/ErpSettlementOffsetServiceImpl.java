package cn.iocoder.yudao.module.erp.service.finance.settlement;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.settlement.ErpSettlementOffsetDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.settlement.ErpSettlementOffsetMapper;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableAccountService;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableAccountService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
public class ErpSettlementOffsetServiceImpl implements ErpSettlementOffsetService {

    @Resource
    private ErpSettlementOffsetMapper settlementOffsetMapper;
    @Resource
    private ErpReceivableAccountService receivableAccountService;
    @Resource
    private ErpPayableAccountService payableAccountService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public PageResult<ErpSettlementOffsetDO> getSettlementOffsetPage(ErpSettlementOffsetPageReqVO reqVO) {
        ErpFinanceVisibleScope scope = getVisibleScope();
        if (scope == null || !scope.hasAccess()) {
            return PageResult.empty();
        }
        Boolean originalShowZero = reqVO.getShowZeroBalance();
        List<ErpSettlementOffsetDO> candidates;
        try {
            reqVO.setShowZeroBalance(true);
            candidates = DataPermissionUtils.executeIgnore(() -> settlementOffsetMapper.selectList(reqVO));
        } finally {
            reqVO.setShowZeroBalance(originalShowZero);
        }
        Set<Long> visibleCustomerIds = customerService.getCustomerList(candidates.stream()
                        .map(ErpSettlementOffsetDO::getCustomerId).collect(Collectors.toSet())).stream()
                .map(ErpCustomerDO::getId).collect(Collectors.toSet());
        Set<Long> visibleSupplierIds = supplierService.getSupplierList(candidates.stream()
                        .map(ErpSettlementOffsetDO::getSupplierId).collect(Collectors.toSet())).stream()
                .map(ErpSupplierDO::getId).collect(Collectors.toSet());
        List<ErpSettlementOffsetDO> visible = new ArrayList<>();
        for (ErpSettlementOffsetDO row : candidates) {
            if (!visibleCustomerIds.contains(row.getCustomerId())
                    || !visibleSupplierIds.contains(row.getSupplierId())) {
                continue;
            }
            ErpReceivableDetailReqVO receivableReq = new ErpReceivableDetailReqVO();
            receivableReq.setCustomerId(row.getCustomerId());
            receivableReq.setBizTime(reqVO.getBizTime());
            List<ErpReceivableDetailRespVO> receivableDetails =
                    receivableAccountService.getReceivableDetailList(receivableReq, scope);
            ErpPayableDetailReqVO payableReq = new ErpPayableDetailReqVO();
            payableReq.setSupplierId(row.getSupplierId());
            payableReq.setBizTime(reqVO.getBizTime());
            List<ErpPayableDetailRespVO> payableDetails = payableAccountService.getPayableDetailList(payableReq, scope);
            if (receivableDetails.isEmpty() && payableDetails.isEmpty()) {
                continue;
            }
            row.setReceivableBalance(sumReceivable(receivableDetails));
            row.setPayableBalance(sumPayable(payableDetails));
            row.setOffsetBalance(row.getReceivableBalance().subtract(row.getPayableBalance()));
            if (Boolean.TRUE.equals(originalShowZero) || row.getOffsetBalance().compareTo(BigDecimal.ZERO) != 0) {
                visible.add(row);
            }
        }
        sort(visible, reqVO);
        long total = visible.size();
        int from = Math.max(0, (reqVO.getPageNo() - 1) * reqVO.getPageSize());
        if (from >= visible.size()) return PageResult.empty(total);
        return new PageResult<>(visible.subList(from, Math.min(visible.size(), from + reqVO.getPageSize())), total);
    }

    @Override
    public ErpSettlementOffsetDetailRespVO getSettlementOffsetDetail(ErpSettlementOffsetDetailReqVO reqVO) {
        ErpFinanceVisibleScope scope = getVisibleScope();
        if (scope == null || !scope.hasAccess()) {
            ErpSettlementOffsetDetailRespVO empty = new ErpSettlementOffsetDetailRespVO();
            empty.setReceivableDetails(new ArrayList<>());
            empty.setPayableDetails(new ArrayList<>());
            return empty;
        }
        ErpReceivableDetailReqVO receivableReqVO = new ErpReceivableDetailReqVO();
        receivableReqVO.setCustomerId(reqVO.getCustomerId());
        receivableReqVO.setBizTime(reqVO.getBizTime());

        ErpPayableDetailReqVO payableReqVO = new ErpPayableDetailReqVO();
        payableReqVO.setSupplierId(reqVO.getSupplierId());
        payableReqVO.setBizTime(reqVO.getBizTime());

        ErpSettlementOffsetDetailRespVO respVO = new ErpSettlementOffsetDetailRespVO();
        respVO.setReceivableDetails(receivableAccountService.getReceivableDetailList(receivableReqVO, scope));
        respVO.setPayableDetails(payableAccountService.getPayableDetailList(payableReqVO, scope));
        sortReceivableDetails(respVO.getReceivableDetails(), reqVO.getReceivableOrderField(),
                reqVO.getReceivableOrderDirection());
        sortPayableDetails(respVO.getPayableDetails(), reqVO.getPayableOrderField(),
                reqVO.getPayableOrderDirection());
        return respVO;
    }

    private void sortReceivableDetails(List<ErpReceivableDetailRespVO> rows, String orderField,
                                       String orderDirection) {
        if (!isSortDirectionValid(orderDirection)) {
            return;
        }
        boolean ascending = "asc".equalsIgnoreCase(orderDirection);
        Comparator<ErpReceivableDetailRespVO> comparator;
        if ("docType".equals(orderField)) {
            comparator = comparing(ErpReceivableDetailRespVO::getDocType, ascending);
        } else if ("docDate".equals(orderField)) {
            comparator = comparing(ErpReceivableDetailRespVO::getDocDate, ascending);
        } else if ("docNo".equals(orderField)) {
            comparator = comparing(ErpReceivableDetailRespVO::getDocNo, ascending);
        } else if ("prevBalance".equals(orderField)) {
            comparator = comparing(ErpReceivableDetailRespVO::getPrevBalance, ascending);
        } else if ("increaseAmount".equals(orderField)) {
            comparator = comparing(ErpReceivableDetailRespVO::getIncreaseAmount, ascending);
        } else if ("receiptAmount".equals(orderField)) {
            comparator = comparing(ErpReceivableDetailRespVO::getReceiptAmount, ascending);
        } else if ("writeOffAmount".equals(orderField)) {
            comparator = comparing(ErpReceivableDetailRespVO::getWriteOffAmount, ascending);
        } else if ("balance".equals(orderField)) {
            comparator = comparing(ErpReceivableDetailRespVO::getBalance, ascending);
        } else {
            return;
        }
        rows.sort(comparator.thenComparing(ErpReceivableDetailRespVO::getBizId,
                Comparator.nullsLast(Comparator.reverseOrder())));
    }

    private void sortPayableDetails(List<ErpPayableDetailRespVO> rows, String orderField,
                                    String orderDirection) {
        if (!isSortDirectionValid(orderDirection)) {
            return;
        }
        boolean ascending = "asc".equalsIgnoreCase(orderDirection);
        Comparator<ErpPayableDetailRespVO> comparator;
        if ("docType".equals(orderField)) {
            comparator = comparing(ErpPayableDetailRespVO::getDocType, ascending);
        } else if ("docDate".equals(orderField)) {
            comparator = comparing(ErpPayableDetailRespVO::getDocDate, ascending);
        } else if ("docNo".equals(orderField)) {
            comparator = comparing(ErpPayableDetailRespVO::getDocNo, ascending);
        } else if ("prevBalance".equals(orderField)) {
            comparator = comparing(ErpPayableDetailRespVO::getPrevBalance, ascending);
        } else if ("increaseAmount".equals(orderField)) {
            comparator = comparing(ErpPayableDetailRespVO::getIncreaseAmount, ascending);
        } else if ("paymentAmount".equals(orderField)) {
            comparator = comparing(ErpPayableDetailRespVO::getPaymentAmount, ascending);
        } else if ("writeOffAmount".equals(orderField)) {
            comparator = comparing(ErpPayableDetailRespVO::getWriteOffAmount, ascending);
        } else if ("balance".equals(orderField)) {
            comparator = comparing(ErpPayableDetailRespVO::getBalance, ascending);
        } else {
            return;
        }
        rows.sort(comparator.thenComparing(ErpPayableDetailRespVO::getBizId,
                Comparator.nullsLast(Comparator.reverseOrder())));
    }

    private boolean isSortDirectionValid(String direction) {
        return "asc".equalsIgnoreCase(direction) || "desc".equalsIgnoreCase(direction);
    }

    private <T, U extends Comparable<? super U>> Comparator<T> comparing(Function<T, U> getter,
                                                                          boolean ascending) {
        Comparator<U> valueComparator = ascending ? Comparator.naturalOrder() : Comparator.reverseOrder();
        return Comparator.comparing(getter, Comparator.nullsLast(valueComparator));
    }

    private ErpFinanceVisibleScope getVisibleScope() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        DeptDataPermissionRespDTO permission = loginUserId == null ? null
                : permissionApi.getDeptDataPermission(loginUserId, "erp_finance_settlement_offset");
        return ErpFinanceVisibleScope.from(permission, loginUserId);
    }

    private BigDecimal sumReceivable(List<ErpReceivableDetailRespVO> rows) {
        return rows.stream().map(row -> defaultAmount(row.getIncreaseAmount())
                .subtract(defaultAmount(row.getReceiptAmount())).subtract(defaultAmount(row.getWriteOffAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPayable(List<ErpPayableDetailRespVO> rows) {
        return rows.stream().map(row -> defaultAmount(row.getIncreaseAmount())
                .subtract(defaultAmount(row.getPaymentAmount())).subtract(defaultAmount(row.getWriteOffAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal defaultAmount(BigDecimal amount) { return amount == null ? BigDecimal.ZERO : amount; }

    private void sort(List<ErpSettlementOffsetDO> rows, ErpSettlementOffsetPageReqVO reqVO) {
        boolean ascending = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        Comparator<ErpSettlementOffsetDO> comparator;
        if ("customerCode".equals(reqVO.getOrderField())) {
            comparator = comparing(ErpSettlementOffsetDO::getCustomerCode, ascending);
        } else if ("subjectName".equals(reqVO.getOrderField())) {
            comparator = comparing(ErpSettlementOffsetDO::getSubjectName, ascending);
        } else if ("receivableBalance".equals(reqVO.getOrderField())) {
            comparator = comparing(ErpSettlementOffsetDO::getReceivableBalance, ascending);
        } else if ("payableBalance".equals(reqVO.getOrderField())) {
            comparator = comparing(ErpSettlementOffsetDO::getPayableBalance, ascending);
        } else {
            comparator = comparing(ErpSettlementOffsetDO::getOffsetBalance, ascending);
        }
        rows.sort(comparator.thenComparing(ErpSettlementOffsetDO::getCustomerId, Comparator.reverseOrder()));
    }
}
