package cn.iocoder.yudao.module.erp.service.report.trade;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ErpTradeReportValueFormatterTest {
    @Test void translatesUnavailableReasonsWithoutChangingApiStates() {
        Map<String,Object> row=new HashMap<>();
        Map<String,String> states=new HashMap<>();states.put("netAmount","TAX_BASIS_UNCONFIRMED");row.put("metricStates",states);
        assertEquals("不可用（税率和未税金额尚未确认）",ErpTradeReportValueFormatter.display(row,"netAmount"));
        assertEquals("TAX_BASIS_UNCONFIRMED",states.get("netAmount"));
        states.put("netAmount","FUTURE_INTERNAL_STATUS");
        assertEquals("不可用（数据状态待核对）",ErpTradeReportValueFormatter.display(row,"netAmount"));
    }
    @Test void hiddenStateWinsEvenWhenUnexpectedValueIsPresent() {
        Map<String,Object> row=new HashMap<>();row.put("financialCost","9007199254740993.123456");row.put("metricStates",Collections.singletonMap("financialCost","MASKED"));
        assertEquals("****",ErpTradeReportValueFormatter.display(row,"financialCost"));
    }
    @Test void preservesExactAmountsAndDoesNotInventOriginalSale() {
        Map<String,Object> row=new HashMap<>();row.put("financialCost","9007199254740993.123456");row.put("sourceRole","NO_ORIGINAL_SALE");row.put("costBasis","MANUAL_CONFIRMED");
        assertEquals("9007199254740993.123456",ErpTradeReportValueFormatter.display(row,"financialCost"));
        assertEquals("无原销售单",ErpTradeReportValueFormatter.display(row,"sourceRole"));assertEquals("人工确认成本",ErpTradeReportValueFormatter.display(row,"costBasis"));
        row.put("metricStates",Collections.singletonMap("saleUserName","NO_ORIGINAL_SALE"));
        assertEquals("不可用（无原销售单，无法还原原销售数据）",ErpTradeReportValueFormatter.display(row,"saleUserName"));
    }
    @Test void purchaseLabelsAndNonApplicableDifferenceRemainReadable() {
        Map<String,Object> row=new HashMap<>();row.put("returnMode",10);row.put("businessType","PURCHASE_RETURN");
        assertEquals("按采购入库单退货",ErpTradeReportValueFormatter.display(row,"returnMode"));
        row.put("metricStates",Collections.singletonMap("returnDifferenceProfit","NOT_APPLICABLE"));
        assertEquals("不适用",ErpTradeReportValueFormatter.display(row,"returnDifferenceProfit"));
    }
}
