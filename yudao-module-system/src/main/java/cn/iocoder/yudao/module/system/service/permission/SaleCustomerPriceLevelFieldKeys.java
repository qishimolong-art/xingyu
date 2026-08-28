package cn.iocoder.yudao.module.system.service.permission;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Product price fields used by each customer price level in sales documents.
 */
public final class SaleCustomerPriceLevelFieldKeys {

    private static final Set<String> ALL_SALE_PRICE_SOURCE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "salePrice",
            "backupPrice1",
            "referencePrice",
            "retailPrice",
            "wholesalePrice",
            "lastPurchasePrice",
            "purchasePrice",
            "costPrice",
            "productPurchasePrice"));

    private SaleCustomerPriceLevelFieldKeys() {
    }

    public static Set<String> getSourceFields(Integer customerPriceLevel) {
        if (customerPriceLevel == null) {
            return ALL_SALE_PRICE_SOURCE_FIELDS;
        }
        switch (customerPriceLevel) {
            case 1:
                return Collections.singleton("backupPrice1");
            case 2:
                return Collections.singleton("referencePrice");
            case 3:
                return Collections.singleton("retailPrice");
            case 4:
                return Collections.singleton("wholesalePrice");
            case 5:
                return new LinkedHashSet<>(Arrays.asList("lastPurchasePrice", "productPurchasePrice"));
            case 6:
                return new LinkedHashSet<>(Arrays.asList("costPrice", "purchasePrice", "productPurchasePrice"));
            default:
                return ALL_SALE_PRICE_SOURCE_FIELDS;
        }
    }

    public static boolean isHidden(Collection<String> hiddenFields, Integer customerPriceLevel) {
        if (hiddenFields == null || hiddenFields.isEmpty()) {
            return false;
        }
        Set<String> hiddenFieldSet = new LinkedHashSet<>();
        for (String field : hiddenFields) {
            ProductPriceFieldKeys.addHiddenField(hiddenFieldSet, field);
        }
        return getSourceFields(customerPriceLevel).stream()
                .anyMatch(field -> hiddenFieldSet.contains(field) || hiddenFieldSet.contains("col_" + field));
    }

}
