package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetPageReqVO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableReportMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableReportMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.settlement.ErpSettlementOffsetMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpFinanceReportKeywordSqlTest {

    @Test
    void receivableReportKeywordIncludesCustomerMnemonicFields() throws Exception {
        String script = selectScript(ErpReceivableReportMapper.class, "selectList",
                ErpReceivableReportPageReqVO.class, Collection.class, String.class, boolean.class,
                Collection.class, Long.class, boolean.class);

        assertCustomerKeywordFields(script, "c");
        assertTrue(script.contains("d.name LIKE CONCAT('%', #{reqVO.keyword}, '%')"), script);
        assertTrue(script.contains("u.nickname LIKE CONCAT('%', #{reqVO.keyword}, '%')"), script);
    }

    @Test
    void payableReportKeywordIncludesSupplierMnemonicFields() throws Exception {
        String script = selectScript(ErpPayableReportMapper.class, "selectList",
                ErpPayableReportPageReqVO.class, Collection.class, String.class, boolean.class,
                Collection.class, Long.class, boolean.class);

        assertSupplierKeywordFields(script, "s");
        assertTrue(script.contains("d.name LIKE CONCAT('%', #{reqVO.keyword}, '%')"), script);
        assertTrue(script.contains("u.nickname LIKE CONCAT('%', #{reqVO.keyword}, '%')"), script);
    }

    @Test
    void receivableReportUsesIndependentMiscReceivableLedgerAndWriteOffType() throws Exception {
        String script = selectScript(ErpReceivableReportMapper.class, "selectList",
                ErpReceivableReportPageReqVO.class, Collection.class, String.class, boolean.class,
                Collection.class, Long.class, boolean.class);

        assertTrue(script.contains("FROM erp_receivable_misc rm"), script);
        assertTrue(script.contains("SUM(rm.amount) AS otherReceivableAmount"), script);
        assertTrue(script.contains("fri.biz_type = 24"), script);
        assertTrue(script.contains("fri.write_off_status = 1"), script);
        assertTrue(script.contains("fr.status = 20"), script);
        assertFalse(script.contains("erp_sale_out"), script);
        assertFalse(script.contains("erp_sale_return"), script);
        assertFalse(script.contains("erp_receivable_other ro"), script);
    }

    @Test
    void payableReportUsesIndependentMiscPayableLedgerAndWriteOffType() throws Exception {
        String script = selectScript(ErpPayableReportMapper.class, "selectList",
                ErpPayableReportPageReqVO.class, Collection.class, String.class, boolean.class,
                Collection.class, Long.class, boolean.class);

        assertTrue(script.contains("FROM erp_payable_misc pm"), script);
        assertTrue(script.contains("SUM(pm.amount) AS otherPayableAmount"), script);
        assertTrue(script.contains("fpi.biz_type = 14"), script);
        assertTrue(script.contains("fpi.write_off_status = 1"), script);
        assertTrue(script.contains("fp.status = 20"), script);
        assertFalse(script.contains("erp_purchase_in"), script);
        assertFalse(script.contains("erp_purchase_return"), script);
        assertFalse(script.contains("erp_payable_other po"), script);
    }
    @Test
    void receivableAccountKeywordIncludesCustomerMnemonicFields() {
        String script = new ErpReceivableAccountMapper.SqlProvider().selectList();

        assertCustomerKeywordFields(script, "c");
        assertTrue(script.contains("DATE_FORMAT(c.create_time, '%Y-%m-%d %H:%i:%s') LIKE CONCAT('%', #{reqVO.keyword}, '%')"),
                script);
    }

    @Test
    void payableAccountKeywordIncludesSupplierMnemonicFields() throws Exception {
        String script = selectScript(ErpPayableAccountMapper.class, "selectList",
                ErpPayableAccountPageReqVO.class, Collection.class, String.class, boolean.class,
                Collection.class, Long.class, boolean.class);

        assertSupplierKeywordFields(script, "s");
        assertTrue(script.contains("DATE_FORMAT(s.create_time, '%Y-%m-%d %H:%i:%s') LIKE CONCAT('%', #{reqVO.keyword}, '%')"),
                script);
    }

    @Test
    void settlementOffsetKeywordIncludesCustomerAndSupplierMnemonicFields() throws Exception {
        String script = selectScript(ErpSettlementOffsetMapper.class, "selectList",
                ErpSettlementOffsetPageReqVO.class);

        assertCustomerKeywordFields(script, "c");
        assertSupplierKeywordFields(script, "s");
        assertTrue(script.contains("reqVO.subjectName"), script);
        assertTrue(script.contains("AND c.name LIKE CONCAT('%', #{reqVO.subjectName}, '%')"), script);
    }

    private static String selectScript(Class<?> mapperClass, String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = mapperClass.getMethod(methodName, parameterTypes);
        Select select = method.getAnnotation(Select.class);
        assertNotNull(select, method + " should declare @Select");
        return String.join("\n", select.value());
    }

    private static void assertCustomerKeywordFields(String script, String alias) {
        assertKeywordFields(script, alias, "code", "name", "short_name", "contact", "mobile",
                "telephone", "pinyin_code", "wubi_code", "member_code", "platform_code");
    }

    private static void assertSupplierKeywordFields(String script, String alias) {
        assertKeywordFields(script, alias, "code", "name", "short_name", "contact", "mobile",
                "telephone", "pinyin_code", "wubi_code");
    }

    private static void assertKeywordFields(String script, String alias, String... columns) {
        for (String column : columns) {
            assertTrue(script.contains(alias + "." + column + " LIKE CONCAT('%', #{reqVO.keyword}, '%')"),
                    "Missing keyword match for " + alias + "." + column + " in:\n" + script);
        }
    }
}
