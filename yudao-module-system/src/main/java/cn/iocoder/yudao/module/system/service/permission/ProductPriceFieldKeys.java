package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Normalizes historical product price field codes to the Java property keys used by ERP APIs.
 */
public final class ProductPriceFieldKeys {

    private static final Map<String, String> ALIAS_TO_CANONICAL = new LinkedHashMap<String, String>() {{
        put("purchase_price", "purchasePrice");
        put("sale_price", "salePrice");
        put("min_price", "minPrice");
        put("reference_price", "referencePrice");
        put("branch_price", "referencePrice");
        put("retail_price", "retailPrice");
        put("last_purchase_price", "lastPurchasePrice");
        put("gross_profit_rate", "grossProfitRate");
        put("backup_price1", "backupPrice1");
        put("wholesale_price", "wholesalePrice");
        put("share_price", "sharePrice");
    }};

    private ProductPriceFieldKeys() {
    }

    public static String normalize(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        String trimmedCode = StrUtil.trim(code);
        boolean columnKey = trimmedCode.startsWith("col_");
        String rawKey = columnKey ? trimmedCode.substring(4) : trimmedCode;
        String normalizedKey = ALIAS_TO_CANONICAL.getOrDefault(rawKey, rawKey);
        return columnKey ? "col_" + normalizedKey : normalizedKey;
    }

    public static Set<String> normalizeSet(Collection<String> codes) {
        Set<String> result = new LinkedHashSet<>();
        if (codes == null) {
            return result;
        }
        for (String code : codes) {
            String normalized = normalize(code);
            if (StrUtil.isNotBlank(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    public static List<String> expandHiddenField(String code) {
        String normalized = normalize(code);
        if (StrUtil.isBlank(normalized)) {
            return new ArrayList<>();
        }
        String rawKey = normalized.startsWith("col_") ? normalized.substring(4) : normalized;
        List<String> result = new ArrayList<>();
        addKey(result, rawKey);
        addKey(result, "col_" + rawKey);
        return result;
    }

    public static void addHiddenField(Collection<String> hiddenFields, String code) {
        hiddenFields.addAll(expandHiddenField(code));
    }

    private static void addKey(List<String> result, String key) {
        if (StrUtil.isNotBlank(key) && !result.contains(key)) {
            result.add(key);
        }
    }

}
