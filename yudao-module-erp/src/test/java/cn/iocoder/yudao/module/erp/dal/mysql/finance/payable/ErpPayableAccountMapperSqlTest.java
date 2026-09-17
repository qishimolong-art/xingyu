package cn.iocoder.yudao.module.erp.dal.mysql.finance.payable;

import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountPageReqVO;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpPayableAccountMapperSqlTest {

    @Test
    void selectList_shouldRenderAllDepartmentSelfAndSelfOnlyScopes() throws Exception {
        assertSql(render(true, Collections.emptyList(), null), "status = 20", false);
        assertSql(render(false, Arrays.asList(10L, 20L), 30L), "dept_id IN", true);
        assertSql(render(false, Collections.emptyList(), 30L), "creator = ?", true);
    }

    @Test
    void selectList_exposesMiscPayableWithoutChangingBalanceFormula() throws Exception {
        String sql = render(true, Collections.emptyList(), null);

        assertTrue(sql.contains("erp_payable_misc"));
        assertTrue(sql.contains("SUM(amount) AS miscPayableAmount"));
        assertTrue(sql.contains("IFNULL(pm.miscPayableAmount, 0) AS miscPayableAmount"));
        assertTrue(sql.contains("erp_payable_other"));
        assertTrue(sql.contains("+ IFNULL(po.otherPayableAmount, 0) + IFNULL(pm.miscPayableAmount, 0) - IFNULL(pr.purchaseReturnAmount, 0) - IFNULL(fp.paymentAmount, 0) AS balance"));
        assertFalse(sql.contains("+ IFNULL(po.otherPayableAmount, 0) - IFNULL(pr.purchaseReturnAmount, 0)"));
    }

    @Test
    void selectList_shouldUseOriginalPurchaseInSettlementAmount() throws Exception {
        String sql = render(true, Collections.emptyList(), null);

        assertTrue(sql.contains("original_product_price"));
        assertTrue(sql.contains("SUM((CASE WHEN EXISTS"));
        assertFalse(sql.contains("SUM(total_price) AS purchaseInAmount"));
    }

    private String render(boolean all, java.util.Collection<Long> deptIds, Long selfUserId) throws Exception {
        Method method = ErpPayableAccountMapper.class.getMethod("selectList", ErpPayableAccountPageReqVO.class,
                java.util.Collection.class, String.class, boolean.class, java.util.Collection.class,
                Long.class, boolean.class);
        Select select = method.getAnnotation(Select.class);
        String script = String.join(" ", select.value());
        Configuration configuration = new Configuration();
        Map<String, Object> params = new HashMap<>();
        params.put("reqVO", new ErpPayableAccountPageReqVO());
        params.put("deptIds", Collections.emptyList());
        params.put("selfUserId", null);
        params.put("all", true);
        params.put("documentDeptIds", deptIds);
        params.put("documentSelfUserId", selfUserId);
        params.put("documentAll", all);
        BoundSql boundSql = new XMLLanguageDriver().createSqlSource(configuration, script, Map.class)
                .getBoundSql(params);
        return boundSql.getSql().replaceAll("\\s+", " ");
    }

    private void assertSql(String sql, String expected, boolean scoped) {
        assertTrue(sql.contains(expected));
        if (scoped) {
            assertTrue(sql.contains("erp_payable_writeoff"));
            assertTrue(sql.contains("operator_user_id"));
            assertFalse(sql.contains("IN ()"));
        }
    }
}
