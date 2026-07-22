package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpReceivableAccountMapperSortTest {

    private static final String[] DISPLAYED_SORT_FIELDS = {
            "mobile", "customerName", "receivableBalance", "preAdvanceAmount", "receiptAmount", "baseAmount",
            "creditLimit", "creditBalance", "creditTermDays", "customerType", "saleUserName", "areaId",
            "deptName", "saleOutAmount", "saleReturnAmount", "routeId"
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
        assertTrue(script.contains("<otherwise>ORDER BY receivableBalance DESC, c.id DESC</otherwise>"));
        assertTrue(script.contains("ORDER BY lastBizTime ASC, c.id DESC"));
        assertTrue(script.contains("ORDER BY lastBizTime DESC, c.id DESC"));
    }

    @Test
    void selectList_returnsRequestedCustomerCreditAndClassificationFields() throws Exception {
        String script = getSelectListScript();

        assertTrue(script.contains("c.area_id AS areaId"));
        assertTrue(script.contains("c.route_id AS routeId"));
        assertTrue(script.contains("IFNULL(ext.base_amount, 0) AS baseAmount"));
        assertTrue(script.contains("c.credit_limit AS creditLimit"));
        assertTrue(script.contains("c.credit_term_days AS creditTermDays"));
        assertTrue(script.contains("END AS creditBalance"));
    }

    private String getSelectListScript() throws Exception {
        Method method = ErpReceivableAccountMapper.class.getMethod("selectList",
                ErpReceivableAccountPageReqVO.class,
                Collection.class, String.class, boolean.class, Collection.class, Long.class, boolean.class);
        Select select = method.getAnnotation(Select.class);
        return String.join("\n", select.value());
    }

}
