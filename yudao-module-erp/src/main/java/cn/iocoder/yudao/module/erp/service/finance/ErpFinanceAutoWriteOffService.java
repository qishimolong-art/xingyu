package cn.iocoder.yudao.module.erp.service.finance;

/**
 * ERP 财务自动核销 Service
 */
public interface ErpFinanceAutoWriteOffService {

    /**
     * 收款单审批通过后，自动按业务时间顺序整单核销销售侧未核销单据。
     *
     * @param receiptId 收款单编号
     * @param userId 核销操作人
     */
    void autoWriteOffReceipt(Long receiptId, Long userId);

    /**
     * 付款单审批通过后，自动按业务时间顺序整单核销采购侧未核销单据。
     *
     * @param paymentId 付款单编号
     * @param userId 核销操作人
     */
    void autoWriteOffPayment(Long paymentId, Long userId);

}
