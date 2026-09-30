package cn.iocoder.yudao.module.erp.service.assistant;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts high-confidence business-object mentions from the current question.
 * It never resolves ids; all candidates still pass through {@link AssistantEntityResolver}.
 */
final class AssistantSemanticExtractor {

    static final class Reference {
        final AssistantEntityResolver.EntityType type;
        final String keyword;
        final String role;
        final String matchType;

        Reference(AssistantEntityResolver.EntityType type, String keyword, String role, String matchType) {
            this.type = type;
            this.keyword = keyword;
            this.role = role;
            this.matchType = matchType;
        }
    }

    private AssistantSemanticExtractor() {
    }

    static List<Reference> extract(String question, String tool) {
        if (question == null || question.trim().isEmpty()) return Collections.emptyList();
        Map<AssistantEntityResolver.EntityType, Reference> result = new LinkedHashMap<>();
        Map<String, Object> explicit = AssistantEntityResolver.explicitReference(question);
        if (explicit != null) {
            AssistantEntityResolver.EntityType type = AssistantEntityResolver.EntityType.valueOf(String.valueOf(explicit.get("entityType")));
            put(result, type, String.valueOf(explicit.get("keyword")), role(type), "EXPLICIT_CODE");
        }

        String source = question.trim();
        String compact = compact(source);
        boolean stock = "query_stock_movements".equals(tool) || "query_product_stock".equals(tool)
                || compact.matches(".*(?:库存|出入库|库存流水|库存变动|调拨记录|盘点记录).*");
        if (stock) {
            // Product specifications often contain meaningful spaces (for example "208L" or package
            // descriptions). Keep those spaces in the entity keyword so an exact/fuzzy SQL lookup does
            // not turn "产品 208L" into the unmatchable "产品208L".
            put(result, AssistantEntityResolver.EntityType.PRODUCT, extractStockProduct(source), "STOCK_PRODUCT", "NATURAL_NAME");
            put(result, AssistantEntityResolver.EntityType.WAREHOUSE, extractWarehouse(compact), "STOCK_LOCATION", "NATURAL_NAME");
        }

        if (!compact.matches(".*(?:哪个|哪些|什么|各个|每个|所有|全部|按)(?:的)?客户.*"))
            put(result, AssistantEntityResolver.EntityType.CUSTOMER,
                    extractParty(source, "客户", "收款|应收|欠款|欠钱|余额|销售|购买|买了|订单|下单|订货"), "CUSTOMER", "NATURAL_NAME");
        if (!compact.matches(".*(?:哪个|哪些|什么|各个|每个|所有|全部|按)(?:的)?供应商.*"))
            put(result, AssistantEntityResolver.EntityType.SUPPLIER,
                    extractParty(source, "供应商", "付款|应付|欠款|余额|采购|供货|供应|卖给"), "SUPPLIER", "NATURAL_NAME");
        put(result, AssistantEntityResolver.EntityType.DEPARTMENT, extractDepartment(compact), "BUSINESS_DEPARTMENT", "NATURAL_NAME");
        put(result, AssistantEntityResolver.EntityType.SALESPERSON, extractSalesperson(compact), "SALESPERSON", "NATURAL_NAME");

        if (compact.matches(".*(?:供应商|客户).*(?:产品|配件).*")
                || compact.matches(".*(?:购买|采购|供应|供货|卖给我们).*(?:产品|配件).*")) {
            put(result, AssistantEntityResolver.EntityType.PRODUCT, extractLabelledProduct(compact), "DOCUMENT_PRODUCT", "NATURAL_NAME");
        }
        return new ArrayList<>(result.values());
    }

    static List<String> contextReferences(String question) {
        if (question == null) return Collections.emptyList();
        String compact = compact(question);
        List<String> result = new ArrayList<>();
        for (String value : Arrays.asList("这个产品", "该产品", "它", "这个客户", "该客户", "这个供应商", "该供应商", "这个仓库", "该仓库", "换成"))
            if (compact.contains(value)) result.add(value);
        return result;
    }

    static boolean isConcreteKeyword(AssistantEntityResolver.EntityType type, String keyword) {
        String value = clean(keyword);
        return value != null && !generic(value, type);
    }

    private static void put(Map<AssistantEntityResolver.EntityType, Reference> result,
                            AssistantEntityResolver.EntityType type, String keyword, String role, String matchType) {
        keyword = clean(keyword);
        if (keyword == null || result.containsKey(type) || generic(keyword, type)) return;
        result.put(type, new Reference(type, keyword, role, matchType));
    }

    private static String extractStockProduct(String text) {
        if (text.matches(".*(?:哪个|哪些|什么|各个|每个|全部|所有)(?:产品|配件).*")
                || text.matches(".*(?:按|分)(?:产品|配件).*(?:排名|统计|汇总).*")
                || text.matches(".*(?:这个|该|此)(?:产品|配件|商品|它).*") ) return null;
        Matcher intent = Pattern.compile("(?:的)?(?:库存流水|出入库(?:情况|明细|记录)?|库存变动|调拨记录|盘点记录|(?:当前)?库存(?:情况|数量|量|多少|是多少|查询)?)(?:吗)?$").matcher(stripPunctuation(text));
        if (!intent.find()) return null;
        String prefix = text.substring(0, intent.start());
        prefix = prefix.replaceFirst("^(?:请问|帮我查一下|帮我查询|查询一下|查询|查看一下|查看|查一下)", "");
        Matcher warehouseFirst = Pattern.compile("^[^，,]{1,30}?仓(?:库)?(?:里|中|内)?[，,](.+)$").matcher(prefix);
        if (warehouseFirst.find()) prefix = warehouseFirst.group(1);
        prefix = prefix.replaceFirst("(?:在|到|从)[^，,。；;？?]{1,30}?仓(?:库)?(?:里|中|内)?$", "");
        prefix = prefix.replaceFirst("(?:在|到|从)[^，,。；;？?]{1,30}?仓(?:库)?(?:里|中|内)?(?:的)?", "");
        prefix = prefix.replaceFirst("^(?:产品|配件|商品)(?:名称)?(?:为|是|[:：])?", "");
        return clean(prefix);
    }

    private static String extractWarehouse(String text) {
        String[] patterns = {
                "(?:在|到|从|只看|换成)([^，。,.?？；;]{1,30}?仓(?:库)?)(?:里|中|内)?",
                "^([^，。,.?？；;]{1,30}?仓(?:库)?)(?:里|中|内)?[，,]",
                "^([^，。,.?？；;]{1,30}?仓(?:库)?)(?:里|中|内)?(?:中)?(?:哪个|哪些|什么|各个|每个|全部|所有)(?:产品|配件)",
                "(?:仓库)(?:名称)?(?:为|是|[:：])?([^，。,.?？；;]{1,30})"
        };
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            if (matcher.find()) return clean(matcher.group(1));
        }
        return null;
    }

    private static String extractParty(String text, String label, String actions) {
        String time = "(?:今天|今日|昨天|昨日|本周|这周|上周|本月|这个月|上月|当前)?";
        String quoted = "[\\\"'“”‘’]?";
        String[] patterns = {
                label + "(?:名称)?(?:为|是|[:：])?" + quoted + "([^，,。；;？?]{1,80}?)" + quoted + "(?=" + time + "(?:的)?(?:" + actions + "))",
                "([^，,。；;？?]{1,80}?)(?:的)?" + label + "(?=" + time + "(?:的)?(?:" + actions + "))",
                "([^，,。；;？?]{1,80}?)(?:的)?" + label + "(?:有)?(?:哪些|什么|多少)?(?:销售|采购)?订单",
                label + "([^，,。；;？?]{1,80}?)(?=(?:卖给我们|供应|提供|购买|采购))"
        };
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            if (!matcher.find()) continue;
            String value = clean(matcher.group(1));
            if (value != null && !generic(value, "客户".equals(label) ? AssistantEntityResolver.EntityType.CUSTOMER : AssistantEntityResolver.EntityType.SUPPLIER))
                return value;
        }
        return extractUnlabelledParty(text, label);
    }

    /**
     * Recognizes a complete finance question that omits the object label, for example
     * "理塘众鑫进口汽修 欠款". The business action determines the object type; rankings and
     * generic questions are deliberately excluded so they cannot become fake entity keywords.
     */
    private static String extractUnlabelledParty(String text, String label) {
        if (text == null) return null;
        String normalized = stripPunctuation(text).trim().replaceAll("\\s+", " ");
        normalized = normalized.replaceFirst("^(?:请问|帮我查一下|帮我查询|查询一下|查询|查看一下|查看|查一下)\\s*", "");
        normalized = normalized.replaceFirst("^(?:今天|今日|昨天|昨日|本周|这周|上周|本月|这个月|上月|当前|现在)\\s*", "");
        String compact = compact(normalized);
        if (compact.matches(".*(?:哪个|哪些|什么|谁|各个|每个|所有|全部|按|排名|排行|最多|最少|最高|最低|前\\d+|后\\d+).*") )
            return null;
        boolean customer = "客户".equals(label);
        if (customer && compact.contains("供应商")) return null;
        if (!customer && compact.contains("客户")) return null;
        String action = customer
                ? "(?:欠款(?:情况|余额|多少|是多少)?|欠钱(?:多少|是多少)?|应收(?:余额|情况|多少|是多少)?|客户余额(?:多少|是多少)?|收款(?:情况|金额|多少|是多少)?)"
                : "(?:应付(?:余额|情况|多少|是多少)?|付款(?:情况|金额|多少|是多少)?)";
        Matcher matcher = Pattern.compile("^(.{1,80}?)(?:\\s*)?(?:今天|今日|昨天|昨日|本周|这周|上周|本月|这个月|上月|当前|现在)?(?:的)?" + action + "(?:呢|吗)?$")
                .matcher(normalized);
        if (!matcher.find()) return null;
        String value = clean(matcher.group(1));
        AssistantEntityResolver.EntityType type = customer
                ? AssistantEntityResolver.EntityType.CUSTOMER : AssistantEntityResolver.EntityType.SUPPLIER;
        if (value == null || value.contains("客户") || value.contains("供应商") || generic(value, type)) return null;
        return value;
    }

    private static String extractDepartment(String text) {
        String action = "销售|采购|库存|收款|付款|应收|应付|欠款";
        String[] patterns = {
                "部门(?:名称)?(?:为|是|[:：])?([^，,。；;？?]{1,60}?)(?=(?:本月|上月|本周|今天|的)?(?:" + action + "))",
                "([^，,。；;？?]{1,60}?(?:部门|分公司|事业部|办事处))(?=(?:本月|上月|本周|今天|的)?(?:" + action + "|收了))"
        };
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            if (matcher.find()) return clean(matcher.group(1));
        }
        return null;
    }

    private static String extractSalesperson(String text) {
        String[] patterns={
                "(?:业务员|销售员|业务经理)(?:名称)?(?:为|是|[:：])?([^，,。；;？?\\d]{2,20}?)(?=(?:今年|本年|本月|上月|本周|上周|\\d{1,2}月份?))",
                "(?:业务员|销售员|业务经理)(?:名称)?(?:为|是|[:：])?([^，,。；;？?]{2,40}?)(?=(?:今年|本年|本月|上月|本周|上周|\\d{1,2}月份?)?(?:的)?(?:销量|销售量|销售情况|销售订单|订单|客户欠款|欠款排行))",
                "([^，,。；;？?]{2,20}?)(?:业务员|销售员)(?=(?:今年|本年|本月|上月|本周|上周|\\d{1,2}月份?)?(?:的)?(?:销量|销售量|销售情况|销售订单|订单|客户欠款|欠款排行))"
        };
        for(String pattern:patterns) {Matcher matcher=Pattern.compile(pattern).matcher(text);if(matcher.find()) return clean(matcher.group(1));}
        Matcher natural=Pattern.compile("^([\\p{IsHan}]{2,4})(?=(?:今年|本年|本月|上月|本周|上周|\\d{1,2}月份?).*(?:销量|销售量|销售情况)$)").matcher(text);
        if(!natural.find()) return null;
        String value=clean(natural.group(1));
        return value!=null && !value.matches(".*(?:机油|轮胎|液压油|防冻液|制动液|汽修|汽配|公司|门市).*")?value:null;
    }

    private static String extractLabelledProduct(String text) {
        Matcher matcher = Pattern.compile("(?:产品|配件)(?:名称)?(?:为|是|[:：])?([^，,。；;？?]{1,80}?)(?=(?:的)?(?:数量|金额|销量|销售额|采购量|采购额|多少|$))").matcher(text);
        if (matcher.find()) return clean(matcher.group(1));
        matcher = Pattern.compile("(?:多少个|多少|采购|购买|供应|提供)(?:的)?(?:产品|配件)([^，,。；;？?]{1,80})$").matcher(text);
        return matcher.find() ? clean(matcher.group(1)) : null;
    }

    private static boolean generic(String value, AssistantEntityResolver.EntityType type) {
        String compact = compact(value);
        if (compact.matches("(?:有)?(?:哪个|哪些|什么|各个|每个|全部|所有|按|该|这个|此|某)(?:的)?(?:产品|配件|客户|供应商|仓库|部门|业务员|销售员)?")) return true;
        if (compact.matches("(?:产品|配件|客户|供应商|仓库|部门|业务员|销售员|这个产品|该产品|这个客户|该客户|这个供应商|该供应商|这个仓库|该仓库|它)")) return true;
        if (compact.matches("(?:当前|今天|今日|昨天|昨日|本周|这周|上周|本月|这个月|上月|各仓|各个仓|各个仓库|各仓库|每个仓|每个仓库|所有仓|所有仓库)")) return true;
        if ((type == AssistantEntityResolver.EntityType.CUSTOMER || type == AssistantEntityResolver.EntityType.SUPPLIER)
                && compact.matches(".*(?:哪个|哪些|各个|每个|按).*$")) return true;
        return false;
    }

    private static String role(AssistantEntityResolver.EntityType type) {
        switch (type) {
            case PRODUCT: return "PRODUCT";
            case CUSTOMER: return "CUSTOMER";
            case SUPPLIER: return "SUPPLIER";
            case WAREHOUSE: return "WAREHOUSE";
            case DEPARTMENT: return "BUSINESS_DEPARTMENT";
            case SALESPERSON: return "SALESPERSON";
            default: return type.name();
        }
    }

    private static String clean(String value) {
        if (value == null) return null;
        value = value.trim().replaceAll("^[\\s，,。；;：:\"'“”‘’《》【】]+|[\\s，,。；;：:\"'“”‘’《》【】]+$", "");
        value = value.replaceAll("^(?:请问|帮我查一下|帮我查询|查询一下|查询|查看一下|查看|查一下)", "");
        value = value.replaceAll("(?:的)$", "").trim();
        return value.isEmpty() || value.length() > 80 ? null : value;
    }

    private static String compact(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    private static String stripPunctuation(String value) {
        return value.replaceAll("[？?。！!]+$", "");
    }
}
