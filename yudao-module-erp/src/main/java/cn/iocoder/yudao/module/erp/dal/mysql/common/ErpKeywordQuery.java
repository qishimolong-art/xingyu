package cn.iocoder.yudao.module.erp.dal.mysql.common;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP list keyword-search helpers.
 */
public final class ErpKeywordQuery {

    private static final String CREATE_TIME_LIKE_SQL = "DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') LIKE {0}";
    private static final String JOIN_CREATE_TIME_LIKE_SQL = "DATE_FORMAT(t.create_time, '%Y-%m-%d %H:%i:%s') LIKE {0}";
    private static final String DEPT_NAME_LIKE_SQL = "EXISTS (SELECT 1 FROM system_dept d "
            + "WHERE d.id = dept_id AND d.deleted = b'0' AND d.name LIKE {0})";
    private static final String JOIN_DEPT_NAME_LIKE_SQL = "EXISTS (SELECT 1 FROM system_dept d "
            + "WHERE d.id = t.dept_id AND d.deleted = b'0' AND d.name LIKE {0})";
    private static final String CUSTOMER_LIKE_SQL = "EXISTS (SELECT 1 FROM erp_customer c "
            + "WHERE c.id = customer_id AND c.deleted = b'0' "
            + "AND (c.code LIKE {0} OR c.name LIKE {1} OR c.short_name LIKE {2} OR c.contact LIKE {3} "
            + "OR c.mobile LIKE {4} OR c.telephone LIKE {5} OR c.pinyin_code LIKE {6} OR c.wubi_code LIKE {7} "
            + "OR c.member_code LIKE {8} OR c.platform_code LIKE {9}))";
    private static final String SUPPLIER_LIKE_SQL = "EXISTS (SELECT 1 FROM erp_supplier s "
            + "WHERE s.id = supplier_id AND s.deleted = b'0' "
            + "AND (s.code LIKE {0} OR s.name LIKE {1} OR s.short_name LIKE {2} OR s.contact LIKE {3} "
            + "OR s.mobile LIKE {4} OR s.telephone LIKE {5} OR s.pinyin_code LIKE {6} OR s.wubi_code LIKE {7}))";
    private static final String PARTY_LIKE_SQL = "("
            + "EXISTS (SELECT 1 FROM erp_customer c WHERE party_type = 1 AND c.id = party_id AND c.deleted = b'0' "
            + "AND (c.code LIKE {0} OR c.name LIKE {1} OR c.short_name LIKE {2} OR c.contact LIKE {3} "
            + "OR c.mobile LIKE {4} OR c.telephone LIKE {5} OR c.pinyin_code LIKE {6} OR c.wubi_code LIKE {7} "
            + "OR c.member_code LIKE {8} OR c.platform_code LIKE {9})) "
            + "OR EXISTS (SELECT 1 FROM erp_supplier s WHERE party_type = 2 AND s.id = party_id AND s.deleted = b'0' "
            + "AND (s.code LIKE {10} OR s.name LIKE {11} OR s.short_name LIKE {12} OR s.contact LIKE {13} "
            + "OR s.mobile LIKE {14} OR s.telephone LIKE {15} OR s.pinyin_code LIKE {16} OR s.wubi_code LIKE {17})))";
    private static final String JOIN_CUSTOMER_LIKE_SQL = "EXISTS (SELECT 1 FROM erp_customer c "
            + "WHERE c.id = t.customer_id AND c.deleted = b'0' AND c.tenant_id = t.tenant_id "
            + "AND (c.code LIKE {0} OR c.name LIKE {1} OR c.short_name LIKE {2} OR c.contact LIKE {3} "
            + "OR c.mobile LIKE {4} OR c.telephone LIKE {5} OR c.pinyin_code LIKE {6} OR c.wubi_code LIKE {7} "
            + "OR c.member_code LIKE {8} OR c.platform_code LIKE {9}))";
    private static final String JOIN_PURCHASE_SUPPLIER_LIKE_SQL = "EXISTS (SELECT 1 FROM erp_supplier s "
            + "WHERE s.id = t.supplier_id AND s.deleted = b'0' AND s.tenant_id = t.tenant_id "
            + "AND (s.code LIKE {0} OR s.name LIKE {1} OR s.short_name LIKE {2} OR s.contact LIKE {3} "
            + "OR s.mobile LIKE {4} OR s.telephone LIKE {5} OR s.pinyin_code LIKE {6} OR s.wubi_code LIKE {7}))";
    private static final int RELATED_NONE = 0;
    private static final int RELATED_CUSTOMER = 1;
    private static final int RELATED_SUPPLIER = 2;
    private static final int RELATED_PARTY = 3;
    private static final String EXPLICIT_KEYWORD_DELIMITER_REGEX = "[/\\\\,，;；+|]+";
    private static final String SPACE_KEYWORD_DELIMITER_REGEX = "[\\s\\u3000]+";

    private ErpKeywordQuery() {
    }

    @SafeVarargs
    public static <T> void append(LambdaQueryWrapperX<T> wrapper, String keyword, SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendLikeGroup(w, value, false, columns));
    }

    @SafeVarargs
    public static <T> void append(MPJLambdaWrapperX<T> wrapper, String keyword, SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendJoinLikeGroup(w, value, false, columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptName(LambdaQueryWrapperX<T> wrapper, String keyword, SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendLikeGroup(w, value, true, columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptName(MPJLambdaWrapperX<T> wrapper, String keyword, SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendJoinLikeGroup(w, value, true, RELATED_NONE, null, columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndCustomer(LambdaQueryWrapperX<T> wrapper, String keyword,
                                                         SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendLikeGroup(w, value, true, RELATED_CUSTOMER, columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndSupplier(LambdaQueryWrapperX<T> wrapper, String keyword,
                                                         SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendLikeGroup(w, value, true, RELATED_SUPPLIER, columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndParty(LambdaQueryWrapperX<T> wrapper, String keyword,
                                                      SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendLikeGroup(w, value, true, RELATED_PARTY, columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndCustomerAndProductItems(LambdaQueryWrapperX<T> wrapper,
                                                                        String keyword,
                                                                        String outerTable,
                                                                        String itemTable,
                                                                        String itemForeignKey,
                                                                        SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendLikeGroup(w, value, true, RELATED_CUSTOMER,
                new ProductItemKeyword(outerTable, itemTable, itemForeignKey, false), columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndProductItemsAndItemSupplier(LambdaQueryWrapperX<T> wrapper,
                                                                            String keyword,
                                                                            String outerTable,
                                                                            String itemTable,
                                                                            String itemForeignKey,
                                                                            SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendLikeGroup(w, value, true, RELATED_NONE,
                new ProductItemKeyword(outerTable, itemTable, itemForeignKey, true), columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndSaleCustomer(MPJLambdaWrapperX<T> wrapper, String keyword,
                                                             SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendJoinLikeGroup(w, value, true, RELATED_CUSTOMER, null, columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndPurchaseSupplier(MPJLambdaWrapperX<T> wrapper, String keyword,
                                                                 SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendJoinLikeGroup(w, value, true, RELATED_SUPPLIER, null, columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndProductItems(MPJLambdaWrapperX<T> wrapper, String keyword,
                                                             String itemTable, String itemForeignKey,
                                                             SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendJoinLikeGroup(w, value, true, RELATED_NONE,
                new ProductItemKeyword(itemTable, itemForeignKey), columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndSaleCustomerAndProductItems(MPJLambdaWrapperX<T> wrapper,
                                                                            String keyword,
                                                                            String itemTable,
                                                                            String itemForeignKey,
                                                                            SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendJoinLikeGroup(w, value, true, RELATED_CUSTOMER,
                new ProductItemKeyword(itemTable, itemForeignKey), columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndPurchaseSupplierAndProductItems(MPJLambdaWrapperX<T> wrapper,
                                                                                String keyword,
                                                                                String itemTable,
                                                                                String itemForeignKey,
                                                                                SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendJoinLikeGroup(w, value, true, RELATED_SUPPLIER,
                new ProductItemKeyword(itemTable, itemForeignKey), columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndSaleCustomerAndProductItemTokens(MPJLambdaWrapperX<T> wrapper,
                                                                                 String keyword,
                                                                                 String itemTable,
                                                                                 String itemForeignKey,
                                                                                 SFunction<T, ?>... columns) {
        appendJoinLikeGroupWithProductItemTokens(wrapper, keyword, true, RELATED_CUSTOMER,
                new ProductItemKeyword(itemTable, itemForeignKey, true), columns);
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndSaleCustomerAndProductItemTokensByProductUnit(
            MPJLambdaWrapperX<T> wrapper, String keyword, String itemTable, String itemForeignKey,
            SFunction<T, ?>... columns) {
        appendJoinLikeGroupWithProductItemTokens(wrapper, keyword, true, RELATED_CUSTOMER,
                ProductItemKeyword.withProductUnit(itemTable, itemForeignKey), columns);
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndPurchaseSupplierAndProductItemTokens(MPJLambdaWrapperX<T> wrapper,
                                                                                     String keyword,
                                                                                     String itemTable,
                                                                                     String itemForeignKey,
                                                                                     SFunction<T, ?>... columns) {
        appendJoinLikeGroupWithProductItemTokens(wrapper, keyword, true, RELATED_SUPPLIER,
                new ProductItemKeyword(itemTable, itemForeignKey, true), columns);
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndPurchaseSupplierAndProductItemTokensByProductUnit(
            MPJLambdaWrapperX<T> wrapper, String keyword, String itemTable, String itemForeignKey,
            SFunction<T, ?>... columns) {
        appendJoinLikeGroupWithProductItemTokens(wrapper, keyword, true, RELATED_SUPPLIER,
                ProductItemKeyword.withProductUnit(itemTable, itemForeignKey), columns);
    }

    public static <T> void appendProductKeyword(MPJLambdaWrapper<T> wrapper, String keyword) {
        appendProductKeyword(wrapper, keyword, (SFunction<?, ?>[]) null);
    }

    public static <T> void appendProductKeyword(MPJLambdaWrapper<T> wrapper, String keyword,
                                                SFunction<?, ?>... extraColumns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value)) {
            return;
        }
        KeywordSearch keywordSearch = parseKeywordSearch(keyword);
        if (keywordSearch.explicitDelimited()) {
            appendProductKeywordTokensAndCondition(wrapper, keywordSearch.tokens(), extraColumns);
            return;
        }
        if (keywordSearch.spaceDelimited()) {
            wrapper.and(w -> {
                appendProductKeywordMatchGroup(w, value, extraColumns);
                w.or(or -> appendProductKeywordTokensAndCondition(or, keywordSearch.tokens(), extraColumns));
            });
            return;
        }
        appendProductKeywordMatchGroup(wrapper, value, extraColumns);
    }

    public static <T> SFunction<?, ?> productKeywordColumn(SFunction<T, ?> column) {
        return column;
    }

    public static String normalize(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().replaceAll("\\s+", "%");
    }

    public static KeywordSearch parseKeywordSearch(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return new KeywordSearch(Collections.emptyList(), false, false);
        }
        String rawKeyword = keyword.trim();
        List<String> explicitTokens = splitKeywordTokens(rawKeyword, EXPLICIT_KEYWORD_DELIMITER_REGEX);
        if (containsExplicitDelimiter(rawKeyword) && !explicitTokens.isEmpty()) {
            return new KeywordSearch(explicitTokens, true, false);
        }
        List<String> spaceTokens = splitKeywordTokens(rawKeyword, SPACE_KEYWORD_DELIMITER_REGEX);
        if (spaceTokens.size() > 1) {
            return new KeywordSearch(spaceTokens, false, true);
        }
        return new KeywordSearch(Collections.emptyList(), false, false);
    }

    public static List<String> splitKeywordTokens(String keyword, String delimiterRegex) {
        return Arrays.stream(keyword.split(delimiterRegex))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    public static boolean shouldAppendTokenProductItemCondition(KeywordSearch keywordSearch) {
        return (keywordSearch.explicitDelimited() || keywordSearch.spaceDelimited())
                && !keywordSearch.tokens().isEmpty();
    }

    public static String buildProductItemKeywordSql(String itemTable, String itemForeignKey,
                                                    String outerQualifier, int tokenCount,
                                                    boolean includeUnit) {
        return new ProductItemKeyword(outerQualifier, itemTable, itemForeignKey, false, includeUnit).toSql(tokenCount);
    }

    @SafeVarargs
    private static <T> void appendLikeGroup(LambdaQueryWrapper<T> wrapper, String keyword,
                                            boolean includeDeptName, SFunction<T, ?>... columns) {
        appendLikeGroup(wrapper, keyword, includeDeptName, RELATED_NONE, columns);
    }

    @SafeVarargs
    private static <T> void appendLikeGroup(LambdaQueryWrapper<T> wrapper, String keyword,
                                            boolean includeDeptName, int relatedParty,
                                            SFunction<T, ?>... columns) {
        appendLikeGroup(wrapper, keyword, includeDeptName, relatedParty, null, columns);
    }

    @SafeVarargs
    private static <T> void appendLikeGroup(LambdaQueryWrapper<T> wrapper, String keyword,
                                            boolean includeDeptName, int relatedParty,
                                            ProductItemKeyword productItemKeyword,
                                            SFunction<T, ?>... columns) {
        boolean hasCondition = false;
        for (SFunction<T, ?> column : columns) {
            if (column == null) {
                continue;
            }
            if (hasCondition) {
                wrapper.or();
            }
            wrapper.like(column, keyword);
            hasCondition = true;
        }
        hasCondition = appendDeptNameCondition(wrapper, includeDeptName, hasCondition, keyword);
        hasCondition = appendAssociatedPartyCondition(wrapper, relatedParty, hasCondition, keyword);
        hasCondition = appendProductItemsCondition(wrapper, productItemKeyword, hasCondition, keyword);
        appendCreateTimeCondition(wrapper, hasCondition, keyword);
    }

    @SafeVarargs
    private static <T> void appendJoinLikeGroup(MPJLambdaWrapper<T> wrapper, String keyword,
                                                boolean includeDeptName, SFunction<T, ?>... columns) {
        appendJoinLikeGroup(wrapper, keyword, includeDeptName, RELATED_NONE, null, columns);
    }

    @SafeVarargs
    private static <T> void appendJoinLikeGroup(MPJLambdaWrapper<T> wrapper, String keyword,
                                                boolean includeDeptName, int relatedParty,
                                                ProductItemKeyword productItemKeyword,
                                                SFunction<T, ?>... columns) {
        boolean hasCondition = false;
        for (SFunction<T, ?> column : columns) {
            if (column == null) {
                continue;
            }
            if (hasCondition) {
                wrapper.or();
            }
            wrapper.like(column, keyword);
            hasCondition = true;
        }
        hasCondition = appendJoinDeptNameCondition(wrapper, includeDeptName, hasCondition, keyword);
        hasCondition = appendJoinAssociatedPartyCondition(wrapper, relatedParty, hasCondition, keyword);
        hasCondition = appendJoinProductItemsCondition(wrapper, productItemKeyword, hasCondition, keyword);
        appendJoinCreateTimeCondition(wrapper, hasCondition, keyword);
    }

    @SafeVarargs
    private static <T> void appendJoinLikeGroupWithProductItemTokens(MPJLambdaWrapperX<T> wrapper,
                                                                     String keyword,
                                                                     boolean includeDeptName,
                                                                     int relatedParty,
                                                                     ProductItemKeyword productItemKeyword,
                                                                     SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        KeywordSearch keywordSearch = parseKeywordSearch(keyword);
        wrapper.and(w -> {
            appendJoinLikeGroup(w, value, includeDeptName, relatedParty, productItemKeyword, columns);
            if (shouldAppendTokenProductItemCondition(keywordSearch)) {
                w.or(or -> appendJoinProductItemsTokenCondition(or, productItemKeyword, keywordSearch.tokens()));
            }
        });
    }

    private static <T> boolean appendDeptNameCondition(LambdaQueryWrapper<T> wrapper,
                                                       boolean includeDeptName,
                                                       boolean hasCondition,
                                                       String keyword) {
        if (!includeDeptName) {
            return hasCondition;
        }
        if (hasCondition) {
            wrapper.or();
        }
        wrapper.apply(DEPT_NAME_LIKE_SQL, "%" + keyword + "%");
        return true;
    }

    private static <T> boolean appendJoinDeptNameCondition(MPJLambdaWrapper<T> wrapper,
                                                           boolean includeDeptName,
                                                           boolean hasCondition,
                                                           String keyword) {
        if (!includeDeptName) {
            return hasCondition;
        }
        if (hasCondition) {
            wrapper.or();
        }
        wrapper.apply(JOIN_DEPT_NAME_LIKE_SQL, "%" + keyword + "%");
        return true;
    }

    private static <T> boolean appendJoinPurchaseSupplierCondition(MPJLambdaWrapper<T> wrapper,
                                                                   boolean includePurchaseSupplier,
                                                                   boolean hasCondition,
                                                                   String keyword) {
        if (!includePurchaseSupplier) {
            return hasCondition;
        }
        if (hasCondition) {
            wrapper.or();
        }
        String likeValue = "%" + keyword + "%";
        wrapper.apply(JOIN_PURCHASE_SUPPLIER_LIKE_SQL,
                likeValue, likeValue, likeValue, likeValue,
                likeValue, likeValue, likeValue, likeValue);
        return true;
    }

    private static <T> boolean appendAssociatedPartyCondition(LambdaQueryWrapper<T> wrapper,
                                                              int relatedParty,
                                                              boolean hasCondition,
                                                              String keyword) {
        if (relatedParty == RELATED_NONE) {
            return hasCondition;
        }
        if (hasCondition) {
            wrapper.or();
        }
        String likeValue = "%" + keyword + "%";
        if (relatedParty == RELATED_CUSTOMER) {
            wrapper.apply(CUSTOMER_LIKE_SQL,
                    likeValue, likeValue, likeValue, likeValue, likeValue,
                    likeValue, likeValue, likeValue, likeValue, likeValue);
        } else if (relatedParty == RELATED_SUPPLIER) {
            wrapper.apply(SUPPLIER_LIKE_SQL,
                    likeValue, likeValue, likeValue, likeValue,
                    likeValue, likeValue, likeValue, likeValue);
        } else if (relatedParty == RELATED_PARTY) {
            wrapper.apply(PARTY_LIKE_SQL,
                    likeValue, likeValue, likeValue, likeValue, likeValue,
                    likeValue, likeValue, likeValue, likeValue, likeValue,
                    likeValue, likeValue, likeValue, likeValue,
                    likeValue, likeValue, likeValue, likeValue);
        }
        return true;
    }

    private static <T> boolean appendJoinAssociatedPartyCondition(MPJLambdaWrapper<T> wrapper,
                                                                  int relatedParty,
                                                                  boolean hasCondition,
                                                                  String keyword) {
        if (relatedParty == RELATED_NONE) {
            return hasCondition;
        }
        if (relatedParty == RELATED_SUPPLIER) {
            return appendJoinPurchaseSupplierCondition(wrapper, true, hasCondition, keyword);
        }
        if (hasCondition) {
            wrapper.or();
        }
        String likeValue = "%" + keyword + "%";
        if (relatedParty == RELATED_CUSTOMER) {
            wrapper.apply(JOIN_CUSTOMER_LIKE_SQL,
                    likeValue, likeValue, likeValue, likeValue, likeValue,
                    likeValue, likeValue, likeValue, likeValue, likeValue);
        }
        return true;
    }

    private static <T> boolean appendJoinProductItemsCondition(MPJLambdaWrapper<T> wrapper,
                                                               ProductItemKeyword productItemKeyword,
                                                               boolean hasCondition,
                                                               String keyword) {
        if (productItemKeyword == null) {
            return hasCondition;
        }
        if (hasCondition) {
            wrapper.or();
        }
        wrapper.apply(productItemKeyword.toSql(), "%" + keyword + "%");
        return true;
    }

    private static <T> void appendJoinProductItemsTokenCondition(MPJLambdaWrapper<T> wrapper,
                                                                 ProductItemKeyword productItemKeyword,
                                                                 List<String> tokens) {
        Object[] values = tokens.stream()
                .map(ErpKeywordQuery::normalize)
                .map(value -> "%" + value + "%")
                .toArray();
        wrapper.apply(productItemKeyword.toSql(tokens.size()), values);
    }

    private static <T> void appendProductKeywordTokensAndCondition(MPJLambdaWrapper<T> wrapper,
                                                                   List<String> tokens,
                                                                   SFunction<?, ?>[] extraColumns) {
        tokens.forEach(token -> wrapper.and(w -> appendProductKeywordMatchGroup(w, normalize(token), extraColumns)));
    }

    private static <T> void appendProductKeywordMatchGroup(MPJLambdaWrapper<T> wrapper, String keyword,
                                                           SFunction<?, ?>[] extraColumns) {
        wrapper.like(ErpProductDO::getCode, keyword)
                .or().like(ErpProductDO::getName, keyword)
                .or().like(ErpProductDO::getPinyinCode, keyword)
                .or().like(ErpProductDO::getWubiCode, keyword)
                .or().like(ErpProductDO::getBarCode, keyword)
                .or().like(ErpProductDO::getVehicleModel, keyword)
                .or().like(ErpProductDO::getFactoryCode, keyword)
                .or().like(ErpProductDO::getStandard, keyword)
                .or().like(ErpProductDO::getBrand, keyword)
                .or().like(ErpProductDO::getDrawingNo, keyword)
                .or().like(ErpProductUnitDO::getName, keyword);
        if (extraColumns == null) {
            return;
        }
        for (SFunction<?, ?> column : extraColumns) {
            if (column != null) {
                wrapper.or().like(column, keyword);
            }
        }
    }

    private static <T> boolean appendProductItemsCondition(LambdaQueryWrapper<T> wrapper,
                                                           ProductItemKeyword productItemKeyword,
                                                           boolean hasCondition,
                                                           String keyword) {
        if (productItemKeyword == null) {
            return hasCondition;
        }
        if (hasCondition) {
            wrapper.or();
        }
        wrapper.apply(productItemKeyword.toSql(), "%" + keyword + "%");
        return true;
    }

    private static <T> void appendCreateTimeCondition(LambdaQueryWrapper<T> wrapper,
                                                      boolean hasCondition,
                                                      String keyword) {
        if (hasCondition) {
            wrapper.or();
        }
        wrapper.apply(CREATE_TIME_LIKE_SQL, "%" + keyword + "%");
    }

    private static <T> void appendJoinCreateTimeCondition(MPJLambdaWrapper<T> wrapper,
                                                          boolean hasCondition,
                                                          String keyword) {
        if (hasCondition) {
            wrapper.or();
        }
        wrapper.apply(JOIN_CREATE_TIME_LIKE_SQL, "%" + keyword + "%");
    }

    private static final class ProductItemKeyword {

        private final String itemTable;
        private final String itemForeignKey;
        private final String outerQualifier;
        private final boolean includeItemSupplier;
        private final boolean includeUnit;
        private final String unitIdExpression;

        private ProductItemKeyword(String itemTable, String itemForeignKey) {
            this("t", itemTable, itemForeignKey, false);
        }

        private ProductItemKeyword(String itemTable, String itemForeignKey, boolean includeUnit) {
            this("t", itemTable, itemForeignKey, false, includeUnit);
        }

        private ProductItemKeyword(String outerQualifier, String itemTable, String itemForeignKey,
                                   boolean includeItemSupplier) {
            this(outerQualifier, itemTable, itemForeignKey, includeItemSupplier, false);
        }

        private ProductItemKeyword(String outerQualifier, String itemTable, String itemForeignKey,
                                   boolean includeItemSupplier, boolean includeUnit) {
            this(outerQualifier, itemTable, itemForeignKey, includeItemSupplier, includeUnit,
                    "COALESCE(i.product_unit_id, p.unit_id)");
        }

        private ProductItemKeyword(String outerQualifier, String itemTable, String itemForeignKey,
                                   boolean includeItemSupplier, boolean includeUnit,
                                   String unitIdExpression) {
            this.itemTable = requireSqlIdentifier(itemTable);
            this.itemForeignKey = requireSqlIdentifier(itemForeignKey);
            this.outerQualifier = requireSqlIdentifier(outerQualifier);
            this.includeItemSupplier = includeItemSupplier;
            this.includeUnit = includeUnit;
            this.unitIdExpression = unitIdExpression;
        }

        private static ProductItemKeyword withProductUnit(String itemTable, String itemForeignKey) {
            return new ProductItemKeyword("t", itemTable, itemForeignKey, false, true, "p.unit_id");
        }

        private String toSql() {
            return toSql(1);
        }

        private String toSql(int tokenCount) {
            String supplierJoin = includeItemSupplier
                    ? "LEFT JOIN erp_supplier s ON s.id = i.supplier_id AND s.deleted = b'0' "
                    + "AND s.tenant_id = " + outerQualifier + ".tenant_id "
                    : "";
            String unitJoin = includeUnit
                    ? "LEFT JOIN erp_product_unit u ON u.id = " + unitIdExpression + " "
                    + "AND u.deleted = b'0' AND u.tenant_id = " + outerQualifier + ".tenant_id "
                    : "";
            StringBuilder sql = new StringBuilder("EXISTS (SELECT 1 FROM " + itemTable + " i "
                    + "LEFT JOIN erp_product p ON p.id = i.product_id AND p.deleted = b'0' "
                    + "AND p.tenant_id = " + outerQualifier + ".tenant_id "
                    + supplierJoin
                    + unitJoin
                    + "WHERE i." + itemForeignKey + " = " + outerQualifier + ".id AND i.deleted = b'0' "
                    + "AND i.tenant_id = " + outerQualifier + ".tenant_id ");
            for (int i = 0; i < tokenCount; i++) {
                sql.append("AND ").append(toCondition(i)).append(' ');
            }
            sql.append(')');
            return sql.toString();
        }

        private String toCondition(int index) {
            String placeholder = "{" + index + "}";
            String supplierLike = includeItemSupplier
                    ? " OR s.code LIKE " + placeholder + " OR s.name LIKE " + placeholder
                    + " OR s.short_name LIKE " + placeholder
                    + " OR s.contact LIKE " + placeholder + " OR s.mobile LIKE " + placeholder
                    + " OR s.telephone LIKE " + placeholder
                    + " OR s.pinyin_code LIKE " + placeholder + " OR s.wubi_code LIKE " + placeholder
                    : "";
            String unitLike = includeUnit ? " OR u.name LIKE " + placeholder : "";
            return "(p.code LIKE " + placeholder + " OR p.name LIKE " + placeholder
                    + " OR p.pinyin_code LIKE " + placeholder
                    + " OR p.wubi_code LIKE " + placeholder + " OR p.bar_code LIKE " + placeholder
                    + " OR p.vehicle_model LIKE " + placeholder
                    + " OR p.factory_code LIKE " + placeholder + " OR p.standard LIKE " + placeholder
                    + " OR p.brand LIKE " + placeholder + " OR p.drawing_no LIKE " + placeholder
                    + supplierLike + unitLike + ")";
        }
    }

    public static final class KeywordSearch {

        private final List<String> tokens;
        private final boolean explicitDelimited;
        private final boolean spaceDelimited;

        private KeywordSearch(List<String> tokens, boolean explicitDelimited, boolean spaceDelimited) {
            this.tokens = tokens;
            this.explicitDelimited = explicitDelimited;
            this.spaceDelimited = spaceDelimited;
        }

        public List<String> tokens() {
            return tokens;
        }

        public boolean explicitDelimited() {
            return explicitDelimited;
        }

        public boolean spaceDelimited() {
            return spaceDelimited;
        }

    }

    private static boolean containsExplicitDelimiter(String keyword) {
        return keyword.indexOf('/') >= 0
                || keyword.indexOf('\\') >= 0
                || keyword.indexOf(',') >= 0
                || keyword.indexOf('，') >= 0
                || keyword.indexOf(';') >= 0
                || keyword.indexOf('；') >= 0
                || keyword.indexOf('+') >= 0
                || keyword.indexOf('|') >= 0;
    }

    private static String requireSqlIdentifier(String value) {
        if (!StringUtils.hasText(value) || !value.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + value);
        }
        return value;
    }
}
