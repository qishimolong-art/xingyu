package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
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
    private ErpReceivableOtherMapper receivableOtherMapper;

    @Override
    public PageResult<ErpReceivableAccountDO> getReceivableAccountPage(ErpReceivableAccountPageReqVO reqVO) {
        return receivableAccountMapper.selectPage(reqVO);
    }

    @Override
    public List<ErpReceivableDetailRespVO> getReceivableDetailList(ErpReceivableDetailReqVO reqVO) {
        List<Row> rows = buildRows(reqVO);
        BigDecimal balance = getInitialBalance(reqVO);
        List<ErpReceivableDetailRespVO> result = new ArrayList<>(rows.size());
        for (Row row : rows) {
            BigDecimal prevBalance = balance;
            balance = balance.add(row.amount);

            ErpReceivableDetailRespVO respVO = new ErpReceivableDetailRespVO();
            respVO.setDocType(row.docType);
            respVO.setDocDate(row.docDate);
            respVO.setDocNo(row.docNo);
            respVO.setPrevBalance(prevBalance);
            respVO.setIncreaseAmount(row.amount.compareTo(BigDecimal.ZERO) > 0 ? row.amount : BigDecimal.ZERO);
            respVO.setReceiptAmount(row.amount.compareTo(BigDecimal.ZERO) < 0 ? row.amount.abs() : BigDecimal.ZERO);
            respVO.setBalance(balance);
            result.add(respVO);
        }
        return result;
    }

    private BigDecimal getInitialBalance(ErpReceivableDetailReqVO reqVO) {
        if (reqVO.getStartTime() == null) {
            return BigDecimal.ZERO;
        }
        ErpReceivableDetailReqVO copy = new ErpReceivableDetailReqVO();
        copy.setCustomerId(reqVO.getCustomerId());
        copy.setBizTime(new LocalDateTime[]{null, reqVO.getStartTime()});

        BigDecimal total = BigDecimal.ZERO;
        for (Row row : buildRows(copy)) {
            total = total.add(row.amount);
        }
        return total;
    }

    private List<Row> buildRows(ErpReceivableDetailReqVO reqVO) {
        List<Row> rows = new ArrayList<>();

        saleOutMapper.selectList(new LambdaQueryWrapperX<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpSaleOutDO::getOutTime, reqVO.getStartTime())
                .ltIfPresent(ErpSaleOutDO::getOutTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(new Row("销售出库", item.getOutTime(), item.getNo(), item.getTotalPrice())));

        saleReturnMapper.selectList(new LambdaQueryWrapperX<ErpSaleReturnDO>()
                .eq(ErpSaleReturnDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpSaleReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpSaleReturnDO::getReturnTime, reqVO.getStartTime())
                .ltIfPresent(ErpSaleReturnDO::getReturnTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(new Row("销售退货", item.getReturnTime(), item.getNo(), negateAmount(item.getTotalPrice()))));

        salePriceAdjustMapper.selectList(new LambdaQueryWrapperX<ErpSalePriceAdjustDO>()
                .eq(ErpSalePriceAdjustDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpSalePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpSalePriceAdjustDO::getAdjustDate, reqVO.getStartTime())
                .ltIfPresent(ErpSalePriceAdjustDO::getAdjustDate, reqVO.getEndTime()))
                .forEach(item -> rows.add(new Row("销售调价", item.getAdjustDate(), item.getNo(), item.getTotalAdjustPrice())));

        financeReceiptMapper.selectList(new LambdaQueryWrapperX<ErpFinanceReceiptDO>()
                .eq(ErpFinanceReceiptDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpFinanceReceiptDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getStartTime())
                .ltIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(new Row("收款单", item.getReceiptTime(), item.getNo(), negateAmount(item.getReceiptPrice()))));

        receivableOtherMapper.selectList(new LambdaQueryWrapperX<ErpReceivableOtherDO>()
                .eq(ErpReceivableOtherDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpReceivableOtherDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpReceivableOtherDO::getBizTime, reqVO.getStartTime() == null ? null : reqVO.getStartTime().toLocalDate())
                .leIfPresent(ErpReceivableOtherDO::getBizTime, reqVO.getEndTime() == null ? null : reqVO.getEndTime().toLocalDate()))
                .forEach(item -> rows.add(new Row("其他应收", item.getBizTime() == null ? null : item.getBizTime().atStartOfDay(),
                        item.getNo(), item.getReceivableAmount())));

        rows.sort(Comparator.comparing(Row::getDocDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Row::getDocType)
                .thenComparing(Row::getDocNo, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    private BigDecimal negateAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.negate();
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

        public String getDocType() {
            return docType;
        }

        public LocalDateTime getDocDate() {
            return docDate;
        }

        public String getDocNo() {
            return docNo;
        }
    }
}
