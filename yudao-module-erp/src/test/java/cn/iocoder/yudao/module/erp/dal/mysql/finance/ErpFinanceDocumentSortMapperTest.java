package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpFinanceDocumentSortMapperTest {

    @Test
    void testLambdaWrapper_appliesWhitelistedFieldAndStableTieBreaker() {
        LambdaQueryWrapperX<ErpAccountDO> wrapper = new LambdaQueryWrapperX<>();
        ErpFinanceSortUtils.apply(wrapper, "accountType", "asc", "erp_account",
                "name", "accountType");

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("erp_account.account_type ASC"), sql);
        assertTrue(sql.contains("erp_account.id DESC"), sql);
    }

    @Test
    void testMpjWrapper_supportsDescendingOrder() {
        MPJLambdaWrapperX<ErpFinancePaymentDO> wrapper = new MPJLambdaWrapperX<>();
        ErpFinanceSortUtils.apply(wrapper, "paymentTime", "desc",
                "no", "paymentTime");

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("t.payment_time DESC"), sql);
        assertTrue(sql.contains("t.id DESC"), sql);
        assertFalse(sql.contains("erp_finance_payment."), sql);
    }

    @Test
    void testMpjWrapper_withoutSort_usesMainTableAliasForDefaultOrder() {
        MPJLambdaWrapperX<ErpFinanceReceiptDO> wrapper = new MPJLambdaWrapperX<>();
        ErpFinanceSortUtils.apply(wrapper, null, null,
                "no", "receiptTime");

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("t.id DESC"), sql);
        assertFalse(sql.contains("erp_finance_receipt."), sql);
    }

    @Test
    void testUnknownField_fallsBackWithoutLeakingInput() {
        LambdaQueryWrapperX<ErpAccountDO> wrapper = new LambdaQueryWrapperX<>();
        ErpFinanceSortUtils.apply(wrapper, "id desc; delete from erp_account", "asc", "erp_account",
                "name");

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("erp_account.id DESC"), sql);
        assertFalse(sql.contains("delete from"), sql);
    }

    @Test
    void testInvalidDirection_fallsBackWithoutLeakingInput() {
        LambdaQueryWrapperX<ErpAccountDO> wrapper = new LambdaQueryWrapperX<>();
        ErpFinanceSortUtils.apply(wrapper, "name", "desc; drop table erp_account", "erp_account",
                "name");

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("erp_account.id DESC"), sql);
        assertFalse(sql.contains("drop table"), sql);
    }

    @Test
    void testPaymentMapper_allDisplayedNameFieldsAreWhitelisted() {
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("supplierName").contains("erp_supplier"));
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("settleMethod").contains("erp_supplier"));
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("creatorName").contains("system_users"));
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("financeUserName").contains("system_users"));
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("deptName").contains("system_dept"));
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("accountName").contains("erp_account"));
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("bankName").contains("COALESCE"));
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("bankName").contains("erp_account"));
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("bankName").contains("erp_supplier"));
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("updaterName").contains("system_users"));
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("auditorName").contains("system_users"));
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("auditorName").contains("t.status = 20"));
        assertNotNull(ErpFinancePaymentMapper.getOrderExpression("paymentTime"));
        assertNotNull(ErpFinancePaymentMapper.getOrderExpression("paymentPrice"));
        assertNotNull(ErpFinancePaymentMapper.getOrderExpression("auditTime"));
        assertTrue(ErpFinancePaymentMapper.getOrderExpression("auditTime").contains("t.status = 20"));
        assertNotNull(ErpFinancePaymentMapper.getOrderExpression("remark"));
    }

    @Test
    void testPaymentMapper_relatedNameSortHasStableTieBreaker() {
        MPJLambdaWrapperX<ErpFinancePaymentDO> wrapper = new MPJLambdaWrapperX<>();
        ErpFinancePaymentPageReqVO reqVO = new ErpFinancePaymentPageReqVO();
        reqVO.setOrderField("supplierName");
        reqVO.setOrderDirection("asc");

        ErpFinancePaymentMapper.orderBy(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("SELECT s.name FROM erp_supplier"), sql);
        assertTrue(sql.contains("ASC"), sql);
        assertTrue(sql.contains("t.id DESC"), sql);
    }

    @Test
    void testPaymentMapper_withoutSortUsesDefaultDescOrder() {
        MPJLambdaWrapperX<ErpFinancePaymentDO> wrapper = new MPJLambdaWrapperX<>();
        ErpFinancePaymentPageReqVO reqVO = new ErpFinancePaymentPageReqVO();

        ErpFinancePaymentMapper.orderBy(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("t.id DESC"), sql);
    }

    @Test
    void testPaymentMapper_rejectsUnknownFieldAndInvalidDirection() {
        assertNull(ErpFinancePaymentMapper.getOrderExpression("supplierName desc; delete from erp_supplier"));
        assertNull(ErpFinancePaymentMapper.getOrderExpression("id desc; delete from erp_finance_payment"));

        MPJLambdaWrapperX<ErpFinancePaymentDO> wrapper = new MPJLambdaWrapperX<>();
        ErpFinancePaymentPageReqVO reqVO = new ErpFinancePaymentPageReqVO();
        reqVO.setOrderField("supplierName");
        reqVO.setOrderDirection("asc; drop table erp_supplier");
        ErpFinancePaymentMapper.orderBy(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("t.id DESC"), sql);
        assertFalse(sql.contains("drop table"), sql);
        assertFalse(sql.contains("erp_supplier"), sql);
    }

    @Test
    void testReceiptMapper_allDisplayedFieldsAreWhitelisted() {
        assertTrue(ErpFinanceReceiptMapper.getOrderExpression("customerName").contains("erp_customer"));
        assertTrue(ErpFinanceReceiptMapper.getOrderExpression("settleMethod").contains("erp_customer"));
        assertTrue(ErpFinanceReceiptMapper.getOrderExpression("accountName").contains("erp_account"));
        assertTrue(ErpFinanceReceiptMapper.getOrderExpression("bankName").contains("erp_account"));
        assertTrue(ErpFinanceReceiptMapper.getOrderExpression("financeUserName").contains("system_users"));
        assertTrue(ErpFinanceReceiptMapper.getOrderExpression("auditorName").contains("system_users"));
        assertNotNull(ErpFinanceReceiptMapper.getOrderExpression("receiptTime"));
        assertNotNull(ErpFinanceReceiptMapper.getOrderExpression("totalPrice"));
        assertNotNull(ErpFinanceReceiptMapper.getOrderExpression("discountPrice"));
        assertNotNull(ErpFinanceReceiptMapper.getOrderExpression("receiptPrice"));
        assertNotNull(ErpFinanceReceiptMapper.getOrderExpression("status"));
        assertNotNull(ErpFinanceReceiptMapper.getOrderExpression("remark"));
        assertNotNull(ErpFinanceReceiptMapper.getOrderExpression("auditTime"));
    }

    @Test
    void testReceiptMapper_relatedNameSortHasStableTieBreaker() {
        MPJLambdaWrapperX<ErpFinanceReceiptDO> wrapper = new MPJLambdaWrapperX<>();
        ErpFinanceReceiptPageReqVO reqVO = new ErpFinanceReceiptPageReqVO();
        reqVO.setOrderField("customerName");
        reqVO.setOrderDirection("asc");

        ErpFinanceReceiptMapper.orderBy(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("SELECT c.name FROM erp_customer"), sql);
        assertTrue(sql.contains("ASC"), sql);
        assertTrue(sql.contains("t.id DESC"), sql);
    }

    @Test
    void testReceiptMapper_withoutSortUsesDefaultDescOrder() {
        MPJLambdaWrapperX<ErpFinanceReceiptDO> wrapper = new MPJLambdaWrapperX<>();
        ErpFinanceReceiptPageReqVO reqVO = new ErpFinanceReceiptPageReqVO();

        ErpFinanceReceiptMapper.orderBy(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("t.id DESC"), sql);
    }

    @Test
    void testReceiptMapper_rejectsUnknownFieldAndInvalidDirection() {
        assertNull(ErpFinanceReceiptMapper.getOrderExpression(
                "customerName desc; delete from erp_customer"));
        assertNull(ErpFinanceReceiptMapper.getOrderExpression(
                "id desc; delete from erp_finance_receipt"));

        MPJLambdaWrapperX<ErpFinanceReceiptDO> wrapper = new MPJLambdaWrapperX<>();
        ErpFinanceReceiptPageReqVO reqVO = new ErpFinanceReceiptPageReqVO();
        reqVO.setOrderField("customerName");
        reqVO.setOrderDirection("asc; drop table erp_customer");
        ErpFinanceReceiptMapper.orderBy(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("t.id DESC"), sql);
        assertFalse(sql.contains("drop table"), sql);
        assertFalse(sql.contains("erp_customer"), sql);
    }

}
