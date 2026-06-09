package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
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
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
    private ErpReceivableOtherMapper receivableOtherMapper;
    @Resource
    private ErpReceivableWriteOffMapper receivableWriteOffMapper;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    public PageResult<ErpReceivableAccountDO> getReceivableAccountPage(ErpReceivableAccountPageReqVO reqVO) {
        return receivableAccountMapper.selectPage(reqVO);
    }

    @Override
    public List<ErpReceivableDetailRespVO> getReceivableDetailList(ErpReceivableDetailReqVO reqVO) {
        List<ErpReceivableDetailRespVO> rows = buildRows(reqVO);
        BigDecimal balance = getInitialBalance(reqVO);
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

    @Override
    public Long writeOffReceivable(ErpReceivableWriteOffReqVO reqVO) {
        customerService.validateCustomer(reqVO.getCustomerId());
        ErpReceivableAccountDO account = receivableAccountMapper.selectByCustomerId(reqVO.getCustomerId());
        BigDecimal balance = account == null || account.getReceivableBalance() == null
                ? BigDecimal.ZERO : account.getReceivableBalance();
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(RECEIVABLE_WRITEOFF_BALANCE_EMPTY);
        }
        if (reqVO.getWriteOffAmount().compareTo(balance) > 0) {
            throw exception(RECEIVABLE_WRITEOFF_AMOUNT_EXCEED, reqVO.getWriteOffAmount(), balance);
        }
        ErpReceivableWriteOffDO writeOff = new ErpReceivableWriteOffDO();
        writeOff.setCustomerId(reqVO.getCustomerId());
        writeOff.setBizType(reqVO.getBizType());
        writeOff.setBizId(reqVO.getBizId());
        writeOff.setBizNo(reqVO.getBizNo());
        writeOff.setWriteOffAmount(reqVO.getWriteOffAmount());
        writeOff.setRemark(reqVO.getRemark());
        writeOff.setWriteOffTime(LocalDateTime.now());
        writeOff.setOperatorUserId(SecurityFrameworkUtils.getLoginUserId());
        receivableWriteOffMapper.insert(writeOff);
        operateLogService.record(ERP_RECEIVABLE_WRITEOFF_TYPE, ERP_WRITEOFF_SUB_TYPE, writeOff.getId(),
                "核销应收账款，客户编号：" + reqVO.getCustomerId()
                        + (reqVO.getBizNo() == null ? "" : "，单据号：" + reqVO.getBizNo())
                        + "，核销金额：" + reqVO.getWriteOffAmount(), String.valueOf(writeOff.getId()));
        return writeOff.getId();
    }

    private BigDecimal getInitialBalance(ErpReceivableDetailReqVO reqVO) {
        if (reqVO.getStartTime() == null) {
            return BigDecimal.ZERO;
        }
        ErpReceivableDetailReqVO copy = new ErpReceivableDetailReqVO();
        copy.setCustomerId(reqVO.getCustomerId());
        copy.setBizTime(new LocalDateTime[]{null, reqVO.getStartTime()});

        BigDecimal total = BigDecimal.ZERO;
        for (ErpReceivableDetailRespVO row : buildRows(copy)) {
            total = total.add(getChangeAmount(row));
        }
        return total;
    }

    private List<ErpReceivableDetailRespVO> buildRows(ErpReceivableDetailReqVO reqVO) {
        List<ErpReceivableDetailRespVO> rows = new ArrayList<>();

        saleOutMapper.selectList(new LambdaQueryWrapperX<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpSaleOutDO::getOutTime, reqVO.getStartTime())
                .ltIfPresent(ErpSaleOutDO::getOutTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(buildRow("销售出库", ErpBizTypeEnum.SALE_OUT.getType(), item.getId(),
                        item.getOutTime(), item.getNo(), item.getTotalPrice(), false)));

        saleReturnMapper.selectList(new LambdaQueryWrapperX<ErpSaleReturnDO>()
                .eq(ErpSaleReturnDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpSaleReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpSaleReturnDO::getReturnTime, reqVO.getStartTime())
                .ltIfPresent(ErpSaleReturnDO::getReturnTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(buildRow("销售退货", ErpBizTypeEnum.SALE_RETURN.getType(), item.getId(),
                        item.getReturnTime(), item.getNo(), negateAmount(item.getTotalPrice()), false)));

        salePriceAdjustMapper.selectList(new LambdaQueryWrapperX<ErpSalePriceAdjustDO>()
                .eq(ErpSalePriceAdjustDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpSalePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpSalePriceAdjustDO::getAdjustDate, reqVO.getStartTime())
                .ltIfPresent(ErpSalePriceAdjustDO::getAdjustDate, reqVO.getEndTime()))
                .forEach(item -> rows.add(buildRow("销售调价", ErpBizTypeEnum.SALE_PRICE_ADJUST.getType(), item.getId(),
                        item.getAdjustDate(), item.getNo(), item.getTotalAdjustPrice(), false)));

        financeReceiptMapper.selectList(new LambdaQueryWrapperX<ErpFinanceReceiptDO>()
                .eq(ErpFinanceReceiptDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpFinanceReceiptDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getStartTime())
                .ltIfPresent(ErpFinanceReceiptDO::getReceiptTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(buildRow("收款单", null, null,
                        item.getReceiptTime(), item.getNo(), negateAmount(item.getReceiptPrice()), false)));

        receivableWriteOffMapper.selectListByCustomerId(reqVO.getCustomerId(), reqVO.getStartTime(), reqVO.getEndTime())
                .forEach(item -> rows.add(buildRow("核销", item.getBizType(), item.getBizId(),
                        item.getWriteOffTime(), item.getBizNo() == null ? String.valueOf(item.getId()) : item.getBizNo(),
                        negateAmount(item.getWriteOffAmount()), true)));

        receivableOtherMapper.selectList(new LambdaQueryWrapperX<ErpReceivableOtherDO>()
                .eq(ErpReceivableOtherDO::getCustomerId, reqVO.getCustomerId())
                .eq(ErpReceivableOtherDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpReceivableOtherDO::getBizTime, reqVO.getStartTime() == null ? null : reqVO.getStartTime().toLocalDate())
                .leIfPresent(ErpReceivableOtherDO::getBizTime, reqVO.getEndTime() == null ? null : reqVO.getEndTime().toLocalDate()))
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

    private BigDecimal getChangeAmount(ErpReceivableDetailRespVO row) {
        BigDecimal increaseAmount = row.getIncreaseAmount() == null ? BigDecimal.ZERO : row.getIncreaseAmount();
        BigDecimal receiptAmount = row.getReceiptAmount() == null ? BigDecimal.ZERO : row.getReceiptAmount();
        BigDecimal writeOffAmount = row.getWriteOffAmount() == null ? BigDecimal.ZERO : row.getWriteOffAmount();
        return increaseAmount.subtract(receiptAmount).subtract(writeOffAmount);
    }
}
