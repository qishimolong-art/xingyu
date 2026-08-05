package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceTransferMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableExpenseMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherIncomeMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableExpenseServiceImpl;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableOtherServiceImpl;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherIncomeServiceImpl;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.validation.Validation;
import javax.validation.Validator;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpFinanceRemarkServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpFinanceTransferServiceImpl transferService;
    @InjectMocks
    private ErpFinancePaymentServiceImpl paymentService;
    @InjectMocks
    private ErpFinanceReceiptServiceImpl receiptService;
    @InjectMocks
    private ErpPayableExpenseServiceImpl expenseService;
    @InjectMocks
    private ErpPayableOtherServiceImpl payableOtherService;
    @InjectMocks
    private ErpReceivableOtherServiceImpl receivableOtherService;
    @InjectMocks
    private ErpReceivableOtherIncomeServiceImpl otherIncomeService;

    @Mock
    private ErpFinanceTransferMapper transferMapper;
    @Mock
    private ErpFinancePaymentMapper paymentMapper;
    @Mock
    private ErpFinanceReceiptMapper receiptMapper;
    @Mock
    private ErpPayableExpenseMapper expenseMapper;
    @Mock
    private ErpPayableOtherMapper payableOtherMapper;
    @Mock
    private ErpReceivableOtherMapper receivableOtherMapper;
    @Mock
    private ErpReceivableOtherIncomeMapper otherIncomeMapper;
    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    public void testUpdateRemarkRequest_validationBoundaries() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        ErpFinanceUpdateRemarkReqVO reqVO = req(1L, null);
        assertEquals(1, validator.validate(reqVO).size());
        reqVO.setRemark(new String(new char[501]).replace('\0', 'a'));
        assertEquals(1, validator.validate(reqVO).size());
        reqVO.setRemark("");
        assertEquals(0, validator.validate(reqVO).size());
    }

    @Test
    public void testUpdateTransferRemark_approvedSuccess() {
        when(transferMapper.selectById(eq(1L))).thenReturn(new ErpFinanceTransferDO()
                .setId(1L).setNo("ZZ001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        transferService.updateFinanceTransferRemark(req(1L, "审批后备注"));
        ErpFinanceTransferDO update = capture(ErpFinanceTransferDO.class);
        verify(transferMapper).updateById(update);
        assertPartial(update.getId(), update.getRemark(), update.getStatus(), update.getNo(), "审批后备注");
    }

    @Test
    public void testUpdatePaymentRemark_approvedSuccess() {
        when(paymentMapper.selectById(eq(2L))).thenReturn(new ErpFinancePaymentDO()
                .setId(2L).setNo("FK001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        paymentService.updateFinancePaymentRemark(req(2L, "审批后备注"));
        ArgumentCaptor<ErpFinancePaymentDO> captor = ArgumentCaptor.forClass(ErpFinancePaymentDO.class);
        verify(paymentMapper).updateById(captor.capture());
        ErpFinancePaymentDO update = captor.getValue();
        assertPartial(update.getId(), update.getRemark(), update.getStatus(), update.getNo(), "审批后备注");
    }

    @Test
    public void testUpdateReceiptRemark_approvedSuccess() {
        when(receiptMapper.selectById(eq(3L))).thenReturn(new ErpFinanceReceiptDO()
                .setId(3L).setNo("SK001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        receiptService.updateFinanceReceiptRemark(req(3L, "审批后备注"));
        ArgumentCaptor<ErpFinanceReceiptDO> captor = ArgumentCaptor.forClass(ErpFinanceReceiptDO.class);
        verify(receiptMapper).updateById(captor.capture());
        ErpFinanceReceiptDO update = captor.getValue();
        assertPartial(update.getId(), update.getRemark(), update.getStatus(), update.getNo(), "审批后备注");
    }

    @Test
    public void testUpdateExpenseRemark_approvedSuccess() {
        when(expenseMapper.selectById(eq(4L))).thenReturn(new ErpPayableExpenseDO()
                .setId(4L).setNo("ZC001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        expenseService.updatePayableExpenseRemark(req(4L, "审批后备注"));
        ArgumentCaptor<ErpPayableExpenseDO> captor = ArgumentCaptor.forClass(ErpPayableExpenseDO.class);
        verify(expenseMapper).updateById(captor.capture());
        ErpPayableExpenseDO update = captor.getValue();
        assertPartial(update.getId(), update.getRemark(), update.getStatus(), update.getNo(), "审批后备注");
    }

    @Test
    public void testUpdatePayableOtherRemark_approvedSuccess() {
        when(payableOtherMapper.selectById(eq(5L))).thenReturn(new ErpPayableOtherDO()
                .setId(5L).setNo("QTYF001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        payableOtherService.updatePayableOtherRemark(req(5L, "审批后备注"));
        ArgumentCaptor<ErpPayableOtherDO> captor = ArgumentCaptor.forClass(ErpPayableOtherDO.class);
        verify(payableOtherMapper).updateById(captor.capture());
        ErpPayableOtherDO update = captor.getValue();
        assertPartial(update.getId(), update.getRemark(), update.getStatus(), update.getNo(), "审批后备注");
    }

    @Test
    public void testUpdateReceivableOtherRemark_approvedSuccess() {
        when(receivableOtherMapper.selectById(eq(6L))).thenReturn(new ErpReceivableOtherDO()
                .setId(6L).setNo("QTYS001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        receivableOtherService.updateReceivableOtherRemark(req(6L, "审批后备注"));
        ArgumentCaptor<ErpReceivableOtherDO> captor = ArgumentCaptor.forClass(ErpReceivableOtherDO.class);
        verify(receivableOtherMapper).updateById(captor.capture());
        ErpReceivableOtherDO update = captor.getValue();
        assertPartial(update.getId(), update.getRemark(), update.getStatus(), update.getNo(), "审批后备注");
    }

    @Test
    public void testUpdateOtherIncomeRemark_approvedSuccess() {
        when(otherIncomeMapper.selectById(eq(7L))).thenReturn(new ErpReceivableOtherIncomeDO()
                .setId(7L).setNo("QTSR001").setStatus(ErpAuditStatus.APPROVE.getStatus()));
        otherIncomeService.updateOtherIncomeRemark(req(7L, "审批后备注"));
        ArgumentCaptor<ErpReceivableOtherIncomeDO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherIncomeDO.class);
        verify(otherIncomeMapper).updateById(captor.capture());
        ErpReceivableOtherIncomeDO update = captor.getValue();
        assertPartial(update.getId(), update.getRemark(), update.getStatus(), update.getNo(), "审批后备注");
    }

    @Test
    public void testUpdateRemark_unapprovedAndClearSuccess() {
        when(paymentMapper.selectById(eq(8L))).thenReturn(new ErpFinancePaymentDO()
                .setId(8L).setNo("FK002").setStatus(ErpAuditStatus.PROCESS.getStatus()));
        paymentService.updateFinancePaymentRemark(req(8L, ""));
        ArgumentCaptor<ErpFinancePaymentDO> captor = ArgumentCaptor.forClass(ErpFinancePaymentDO.class);
        verify(paymentMapper).updateById(captor.capture());
        ErpFinancePaymentDO update = captor.getValue();
        assertPartial(update.getId(), update.getRemark(), update.getStatus(), update.getNo(), "");
    }

    @Test
    public void testUpdateTransferRemark_notExists() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> transferService.updateFinanceTransferRemark(req(9L, "备注")));
        assertEquals(FINANCE_TRANSFER_NOT_EXISTS.getCode(), ex.getCode());
        verify(transferMapper, never()).updateById(any(ErpFinanceTransferDO.class));
    }

    private ErpFinanceTransferDO capture(Class<ErpFinanceTransferDO> type) {
        ArgumentCaptor<ErpFinanceTransferDO> captor = ArgumentCaptor.forClass(type);
        verify(transferMapper).updateById(captor.capture());
        return captor.getValue();
    }

    private static ErpFinanceUpdateRemarkReqVO req(Long id, String remark) {
        ErpFinanceUpdateRemarkReqVO reqVO = new ErpFinanceUpdateRemarkReqVO();
        reqVO.setId(id);
        reqVO.setRemark(remark);
        return reqVO;
    }

    private static void assertPartial(Long id, String remark, Integer status, String no, String expectedRemark) {
        assertEquals(expectedRemark, remark);
        assertNull(status);
        assertNull(no);
        if ("".equals(expectedRemark)) {
            assertEquals(8L, id);
        } else {
            assertEquals(true, id >= 1L && id <= 7L);
        }
    }

}
