package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    @Override
    public PageResult<ErpPayableAccountDO> getPayableAccountPage(ErpPayableAccountPageReqVO reqVO) {
        return payableAccountMapper.selectPage(reqVO);
    }

    @Override
    public List<ErpPayableDetailRespVO> getPayableDetailList(ErpPayableDetailReqVO reqVO) {
        List<Row> rows = buildRows(reqVO);
        BigDecimal runningBalance = getInitialBalance(reqVO);
        List<ErpPayableDetailRespVO> result = new ArrayList<>(rows.size());
        for (Row row : rows) {
            BigDecimal prevBalance = runningBalance;
            runningBalance = runningBalance.add(row.amount);

            ErpPayableDetailRespVO respVO = new ErpPayableDetailRespVO();
            respVO.setDocType(row.docType);
            respVO.setDocDate(row.docDate);
            respVO.setDocNo(row.docNo);
            respVO.setPrevBalance(prevBalance);
            respVO.setIncreaseAmount(row.amount.compareTo(BigDecimal.ZERO) > 0 ? row.amount : BigDecimal.ZERO);
            respVO.setPaymentAmount(row.amount.compareTo(BigDecimal.ZERO) < 0 ? row.amount.abs() : BigDecimal.ZERO);
            respVO.setBalance(runningBalance);
            result.add(respVO);
        }
        return result;
    }

    private BigDecimal getInitialBalance(ErpPayableDetailReqVO reqVO) {
        if (reqVO.getStartTime() == null) {
            return BigDecimal.ZERO;
        }
        ErpPayableDetailReqVO copy = new ErpPayableDetailReqVO();
        copy.setSupplierId(reqVO.getSupplierId());
        copy.setBizTime(new LocalDateTime[]{null, reqVO.getStartTime()});

        BigDecimal total = BigDecimal.ZERO;
        for (Row row : buildRows(copy)) {
            total = total.add(row.amount);
        }
        return total;
    }

    private List<Row> buildRows(ErpPayableDetailReqVO reqVO) {
        List<Row> rows = new ArrayList<>();

        purchaseInMapper.selectList(new LambdaQueryWrapperX<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpPurchaseInDO::getInTime, reqVO.getStartTime())
                .ltIfPresent(ErpPurchaseInDO::getInTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(new Row("采购入库", item.getInTime(), item.getNo(), item.getTotalPrice())));

        purchaseReturnMapper.selectList(new LambdaQueryWrapperX<ErpPurchaseReturnDO>()
                .eq(ErpPurchaseReturnDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPurchaseReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpPurchaseReturnDO::getReturnTime, reqVO.getStartTime())
                .ltIfPresent(ErpPurchaseReturnDO::getReturnTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(new Row("采购退货", item.getReturnTime(), item.getNo(), negateAmount(item.getTotalPrice()))));

        purchasePriceAdjustMapper.selectList(new LambdaQueryWrapperX<ErpPurchasePriceAdjustDO>()
                .eq(ErpPurchasePriceAdjustDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPurchasePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpPurchasePriceAdjustDO::getAdjustTime, reqVO.getStartTime())
                .ltIfPresent(ErpPurchasePriceAdjustDO::getAdjustTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(new Row("采购调价", item.getAdjustTime(), item.getNo(), defaultAmount(item.getTotalAdjustPrice()))));

        financePaymentMapper.selectList(new LambdaQueryWrapperX<ErpFinancePaymentDO>()
                .eq(ErpFinancePaymentDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpFinancePaymentDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getStartTime())
                .ltIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(new Row("付款单", item.getPaymentTime(), item.getNo(), negateAmount(item.getPaymentPrice()))));

        rows.sort(Comparator.comparing(Row::getDocDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Row::getDocType)
                .thenComparing(Row::getDocNo, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    private BigDecimal negateAmount(BigDecimal amount) {
        return defaultAmount(amount).negate();
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static final class Row {
        private final String docType;
        private final LocalDateTime docDate;
        private final String docNo;
        private final BigDecimal amount;

        private Row(String docType, LocalDateTime docDate, String docNo, BigDecimal amount) {
            this.docType = docType;
            this.docDate = docDate;
            this.docNo = docNo;
            this.amount = amount == null ? BigDecimal.ZERO : amount;
        }

        public LocalDateTime getDocDate() {
            return docDate;
        }

        public String getDocType() {
            return docType;
        }

        public String getDocNo() {
            return docNo;
        }
    }
}
