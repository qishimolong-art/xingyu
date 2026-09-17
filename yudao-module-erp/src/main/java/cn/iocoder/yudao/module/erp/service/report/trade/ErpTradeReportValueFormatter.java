package cn.iocoder.yudao.module.erp.service.report.trade;

import java.util.Map;

/** Excel可读文本转换；API的金额与状态码保持原样。 */
public final class ErpTradeReportValueFormatter {
    private ErpTradeReportValueFormatter() { }

    public static String display(Map<String, Object> row, String key) {
        Object statesValue = row.get("metricStates");
        Object state = statesValue instanceof Map ? ((Map<?, ?>) statesValue).get(key) : null;
        if ("MASKED".equals(state)) return "****";
        if ("NOT_APPLICABLE".equals(state)) return "不适用";
        Object value = row.get(key);
        if (value == null) return state == null ? "未记录" : "不可用（" + reason(state.toString()) + "）";
        String text = value.toString();
        if ("returnMode".equals(key)) return "10".equals(text)
                ? ("SALE_RETURN".equals(row.get("businessType")) ? "按原销售单退货" : "按采购入库单退货")
                : "20".equals(text) ? "按库存退货" : "未记录";
        if ("costBasis".equals(key) && "CURRENT_AVERAGE".equals(text)) return "退货时两套当前均价";
        if ("costBasis".equals(key) && "MANUAL_CONFIRMED".equals(text)) return "人工确认成本";
        if ("sourceRole".equals(key)) {
            switch (text) {
                case "SOURCE_PURCHASE_IN": return "原采购入库追溯";
                case "NO_ORIGINAL_PURCHASE": return "无原采购入库";
                case "SOURCE_ORDER": return "来源订单";
                case "ORIGINAL_SALE": return "原销售";
                case "NO_ORIGINAL_SALE": return "无原销售单";
                default: return "未记录";
            }
        }
        if ("returnDifferenceStatus".equals(key)) return reason(text);
        return text;
    }

    private static String reason(String state) {
        switch (state) {
            case "MASKED": return "没有金额查看权限";
            case "NOT_APPLICABLE": return "不适用";
            case "SOURCE_SNAPSHOT_MISSING": return "缺少当时交易快照";
            case "HEADER_ALLOCATION_PENDING": return "头部折扣或费用尚未分配至商品";
            case "TAX_BASIS_UNCONFIRMED": return "税率和未税金额尚未确认";
            case "SETTLEMENT_ALLOCATION_PENDING": return "商品收付款及余额归集尚未接入";
            case "FEE_ALLOCATION_PENDING": return "费用分摊依据尚未配置";
            case "ORIGINAL_SNAPSHOT_MISSING": return "缺少原销售属性快照";
            case "ZERO_DENOMINATOR": return "分母为零";
            case "NO_ORIGINAL_SALE": return "无原销售单，无法还原原销售数据";
            case "COST_BASIS_NOT_RECORDED": return "当时成本依据未记录";
            default: return "数据状态待核对";
        }
    }
}
