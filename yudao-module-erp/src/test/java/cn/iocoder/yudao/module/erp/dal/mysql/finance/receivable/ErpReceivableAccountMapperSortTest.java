package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpReceivableAccountMapperSortTest {

    private static final String[] DISPLAYED_SORT_FIELDS = {
            "mobile", "customerName", "receivableBalance", "preAdvanceAmount", "receiptAmount", "baseAmount",
            "creditLimit", "creditBalance", "creditTermDays", "customerType", "saleUserName", "areaId",
            "deptName", "saleOutAmount", "saleReturnAmount", "routeId", "miscReceivableAmount",
            "openingReceivableBalance"
    };

    @Test
    void selectList_whitelistsEveryDisplayedBusinessColumnInBothDirections() throws Exception {
        String script = getSelectListScript();

        for (String field : DISPLAYED_SORT_FIELDS) {
            assertTrue(script.contains("orderDirection == \"asc\" and reqVO.orderField == \"" + field + "\""),
                    "Missing ascending sort whitelist entry for " + field);
            assertTrue(script.contains("orderDirection == \"desc\" and reqVO.orderField == \"" + field + "\""),
                    "Missing descending sort whitelist entry for " + field);
        }
    }

    @Test
    void selectList_rejectsUnknownFieldsAndKeepsStableFallbackOrder() throws Exception {
        String script = getSelectListScript();

        assertFalse(script.contains("${"), "Sort SQL must not interpolate request values");
        assertTrue(script.contains("<otherwise>ORDER BY receivableBalance DESC, c.id DESC, accountKeys.deptId DESC</otherwise>"));
        assertTrue(script.contains("ORDER BY lastBizTime ASC, c.id DESC, accountKeys.deptId DESC"));
        assertTrue(script.contains("ORDER BY lastBizTime DESC, c.id DESC, accountKeys.deptId DESC"));
    }

    @Test
    void selectList_returnsRequestedCustomerCreditAndClassificationFields() throws Exception {
        String script = getSelectListScript();

        assertTrue(script.contains("c.area_id AS areaId"));
        assertTrue(script.contains("c.route_id AS routeId"));
        assertTrue(script.contains("IFNULL(ext.base_amount, 0) AS baseAmount"));
        assertTrue(script.contains("COALESCE(cdc.credit_limit, c.credit_limit) AS creditLimit"));
        assertTrue(script.contains("COALESCE(cdc.credit_term_days, c.credit_term_days) AS creditTermDays"));
        assertTrue(script.contains("END AS creditBalance"));
    }

    @Test
    void selectList_groupsAmountsByCustomerAndDocumentDepartment() {
        String script = getSelectListScript();

        assertTrue(script.contains("accountKeys.deptId AS deptId"));
        assertTrue(script.contains("IFNULL(d.name, '未归属部门') AS deptName"));
        assertTrue(script.contains("GROUP BY so_key.customer_id, so_key.dept_id"));
        assertTrue(script.contains("GROUP BY rc.customer_id, rc.dept_id"));
        assertTrue(script.contains("ON so.customer_id = c.id AND (so.deptId = accountKeys.deptId"));
        assertTrue(script.contains("COALESCE(cdc.credit_limit, c.credit_limit) AS creditLimit"));
        assertTrue(script.contains("<if test='reqVO.deptId != null'> AND so_key.dept_id = #{reqVO.deptId}</if>"));
        assertTrue(script.contains("cdc.deleted = b'0' AND cdc.tenant_id = c.tenant_id"));
        assertTrue(script.contains("LEFT JOIN system_dept d ON d.id = accountKeys.deptId"));
        assertFalse(script.contains("LEFT JOIN system_dept d ON d.id = c.dept_id"));
    }

    @Test
    void selectList_showsNegatedMiscReceivableWithoutPuttingItInReceivableAccountBalance() {
        String script = getSelectListScript();

        assertTrue(script.contains("FROM erp_receivable_misc rm_key"));
        assertTrue(script.contains("FROM erp_receivable_misc rm"));
        assertTrue(script.contains("-SUM(rm.amount) AS miscReceivableAmount"));
        assertTrue(script.contains("IFNULL(rm.miscReceivableAmount, 0) AS miscReceivableAmount"));
        assertFalse(script.contains("+ IFNULL(rm.miscReceivableAmount, 0)"));
        assertFalse(script.contains("- IFNULL(rm.miscReceivableAmount, 0)"));
        assertTrue(script.contains("IFNULL(ro.otherReceivableAmount, 0)"));
        assertTrue(script.contains("- IFNULL(csr.saleReturnAmount, 0) - IFNULL(crc.receiptAmount, 0)"));
        assertTrue(script.contains("OR IFNULL(rm.miscReceivableAmount, 0) &lt;&gt; 0"));
        assertTrue(script.contains("ORDER BY miscReceivableAmount ASC"));
        assertTrue(script.contains("ORDER BY miscReceivableAmount DESC"));
        assertTrue(script.contains("amountJoinSql(\"erp_receivable_other\"")
                || script.contains("FROM erp_receivable_other ro"));
    }

    @Test
    void selectList_keepsDateRangePeriodAmountsSeparateFromOpeningAndCutoffBalances() {
        String script = getSelectListScript();

        assertTrue(script.contains("IFNULL(oso.saleOutAmount, 0)"));
        assertTrue(script.contains("AS openingReceivableBalance"));
        assertTrue(script.contains("IFNULL(cso.saleOutAmount, 0)"));
        assertTrue(script.contains("AS receivableBalance"));
        assertTrue(script.contains("so_key.out_time &lt;= #{reqVO.endTime}"));
        assertFalse(script.contains("so_key.out_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}"));
        assertTrue(script.contains("t.out_time BETWEEN #{reqVO.startTime} AND #{reqVO.endTime}"));
        assertTrue(script.contains("t.out_time &lt; #{reqVO.startTime}"));
        assertTrue(script.contains("t.out_time &lt;= #{reqVO.endTime}"));
        assertTrue(script.contains("OR (" +
                "IFNULL(oso.saleOutAmount, 0) + IFNULL(opa.priceAdjustAmount, 0)"));
        assertTrue(script.contains("ORDER BY openingReceivableBalance ASC"));
        assertTrue(script.contains("ORDER BY openingReceivableBalance DESC"));
    }

    @Test
    void selectList_usesReceivableAccountSaleOutAmountBeforeAddingPriceAdjustments() {
        String script = getSelectListScript();

        assertTrue(script.contains(ErpSaleOutMapper.RECEIVABLE_ACCOUNT_SALE_AMOUNT_EXPRESSION));
        assertTrue(script.contains("SUM(" + ErpSaleOutMapper.RECEIVABLE_ACCOUNT_SALE_AMOUNT_EXPRESSION
                + ") AS saleOutAmount"));
        assertFalse(script.contains(ErpSaleOutMapper.ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION));
        assertTrue(script.contains("- COALESCE(t.fee_amount, t.other_price, t.extra_fee, t.freight, 0)"));
        assertFalse(script.contains("+ COALESCE(t.fee_amount, t.other_price, t.extra_fee, 0)"));
        assertTrue(script.contains("IFNULL(so.saleOutAmount, 0) + IFNULL(pa.priceAdjustAmount, 0)"));
        assertFalse(script.contains("amountJoinSql(\"erp_sale_out\""));
        assertFalse(script.contains("SUM(so.total_price) AS saleOutAmount"));
    }

    private String getSelectListScript() {
        return new ErpReceivableAccountMapper.SqlProvider().selectList();
    }

}
