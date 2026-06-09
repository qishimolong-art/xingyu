package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
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
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableWriteOffMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
    private ErpPayableOtherMapper payableOtherMapper;
    @Resource
    private ErpPayableWriteOffMapper payableWriteOffMapper;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    public PageResult<ErpPayableAccountDO> getPayableAccountPage(ErpPayableAccountPageReqVO reqVO) {
        return payableAccountMapper.selectPage(reqVO);
    }

    @Override
    public List<ErpPayableDetailRespVO> getPayableDetailList(ErpPayableDetailReqVO reqVO) {
        List<ErpPayableDetailRespVO> rows = buildRows(reqVO);
        BigDecimal runningBalance = getInitialBalance(reqVO);
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

    @Override
    public Long writeOffPayable(ErpPayableWriteOffReqVO reqVO) {
        supplierService.validateSupplier(reqVO.getSupplierId());
        ErpPayableAccountDO account = payableAccountMapper.selectBySupplierId(reqVO.getSupplierId());
        BigDecimal balance = account == null || account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
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
        writeOff.setOperatorUserId(SecurityFrameworkUtils.getLoginUserId());
        payableWriteOffMapper.insert(writeOff);
        operateLogService.record(ERP_PAYABLE_WRITEOFF_TYPE, ERP_WRITEOFF_SUB_TYPE, writeOff.getId(),
                "核销应付账款，供应商编号：" + reqVO.getSupplierId()
                        + (reqVO.getBizNo() == null ? "" : "，单据号：" + reqVO.getBizNo())
                        + "，核销金额：" + reqVO.getWriteOffAmount(), String.valueOf(writeOff.getId()));
        return writeOff.getId();
    }

    private BigDecimal getInitialBalance(ErpPayableDetailReqVO reqVO) {
        if (reqVO.getStartTime() == null) {
            return BigDecimal.ZERO;
        }
        ErpPayableDetailReqVO copy = new ErpPayableDetailReqVO();
        copy.setSupplierId(reqVO.getSupplierId());
        copy.setBizTime(new LocalDateTime[]{null, reqVO.getStartTime()});

        BigDecimal total = BigDecimal.ZERO;
        for (ErpPayableDetailRespVO row : buildRows(copy)) {
            total = total.add(getChangeAmount(row));
        }
        return total;
    }

    private List<ErpPayableDetailRespVO> buildRows(ErpPayableDetailReqVO reqVO) {
        List<ErpPayableDetailRespVO> rows = new ArrayList<>();

        purchaseInMapper.selectList(new LambdaQueryWrapperX<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpPurchaseInDO::getInTime, reqVO.getStartTime())
                .ltIfPresent(ErpPurchaseInDO::getInTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(buildRow("采购入库", ErpBizTypeEnum.PURCHASE_IN.getType(), item.getId(),
                        item.getInTime(), item.getNo(), item.getTotalPrice(), false)));

        purchaseReturnMapper.selectList(new LambdaQueryWrapperX<ErpPurchaseReturnDO>()
                .eq(ErpPurchaseReturnDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPurchaseReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpPurchaseReturnDO::getReturnTime, reqVO.getStartTime())
                .ltIfPresent(ErpPurchaseReturnDO::getReturnTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(buildRow("采购退货", ErpBizTypeEnum.PURCHASE_RETURN.getType(), item.getId(),
                        item.getReturnTime(), item.getNo(), negateAmount(item.getTotalPrice()), false)));

        purchasePriceAdjustMapper.selectList(new LambdaQueryWrapperX<ErpPurchasePriceAdjustDO>()
                .eq(ErpPurchasePriceAdjustDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPurchasePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpPurchasePriceAdjustDO::getAdjustTime, reqVO.getStartTime())
                .ltIfPresent(ErpPurchasePriceAdjustDO::getAdjustTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(buildRow("采购调价", ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType(), item.getId(),
                        item.getAdjustTime(), item.getNo(), defaultAmount(item.getTotalAdjustPrice()), false)));

        financePaymentMapper.selectList(new LambdaQueryWrapperX<ErpFinancePaymentDO>()
                .eq(ErpFinancePaymentDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpFinancePaymentDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getStartTime())
                .ltIfPresent(ErpFinancePaymentDO::getPaymentTime, reqVO.getEndTime()))
                .forEach(item -> rows.add(buildRow("付款单", null, item.getId(),
                        item.getPaymentTime(), item.getNo(), negateAmount(item.getPaymentPrice()), false)));

        payableWriteOffMapper.selectListBySupplierId(reqVO.getSupplierId(), reqVO.getStartTime(), reqVO.getEndTime())
                .forEach(item -> rows.add(buildRow("核销", item.getBizType(), item.getBizId(),
                        item.getWriteOffTime(), item.getBizNo() == null ? String.valueOf(item.getId()) : item.getBizNo(),
                        negateAmount(item.getWriteOffAmount()), true)));

        payableOtherMapper.selectList(new LambdaQueryWrapperX<ErpPayableOtherDO>()
                .eq(ErpPayableOtherDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPayableOtherDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .geIfPresent(ErpPayableOtherDO::getBizTime, reqVO.getStartTime() == null ? null : reqVO.getStartTime().toLocalDate())
                .leIfPresent(ErpPayableOtherDO::getBizTime, reqVO.getEndTime() == null ? null : reqVO.getEndTime().toLocalDate()))
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

    private BigDecimal getChangeAmount(ErpPayableDetailRespVO row) {
        BigDecimal increaseAmount = row.getIncreaseAmount() == null ? BigDecimal.ZERO : row.getIncreaseAmount();
        BigDecimal paymentAmount = row.getPaymentAmount() == null ? BigDecimal.ZERO : row.getPaymentAmount();
        BigDecimal writeOffAmount = row.getWriteOffAmount() == null ? BigDecimal.ZERO : row.getWriteOffAmount();
        return increaseAmount.subtract(paymentAmount).subtract(writeOffAmount);
    }
}
