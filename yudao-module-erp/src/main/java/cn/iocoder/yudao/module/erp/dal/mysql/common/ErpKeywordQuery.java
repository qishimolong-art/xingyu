package cn.iocoder.yudao.module.erp.dal.mysql.common;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.util.StringUtils;

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
    private static final String JOIN_PURCHASE_SUPPLIER_LIKE_SQL = "EXISTS (SELECT 1 FROM erp_supplier s "
            + "WHERE s.id = t.supplier_id AND s.deleted = b'0' AND s.tenant_id = t.tenant_id "
            + "AND (s.code LIKE {0} OR s.name LIKE {1} OR s.short_name LIKE {2} OR s.contact LIKE {3} "
            + "OR s.mobile LIKE {4} OR s.telephone LIKE {5} OR s.pinyin_code LIKE {6} OR s.wubi_code LIKE {7}))";

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
        wrapper.and(w -> appendJoinLikeGroup(w, value, true, columns));
    }

    @SafeVarargs
    public static <T> void appendWithDeptNameAndPurchaseSupplier(MPJLambdaWrapperX<T> wrapper, String keyword,
                                                                 SFunction<T, ?>... columns) {
        String value = normalize(keyword);
        if (!StringUtils.hasText(value) || columns == null || columns.length == 0) {
            return;
        }
        wrapper.and(w -> appendJoinLikeGroup(w, value, true, true, columns));
    }

    public static String normalize(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().replaceAll("\\s+", "%");
    }

    @SafeVarargs
    private static <T> void appendLikeGroup(LambdaQueryWrapper<T> wrapper, String keyword,
                                            boolean includeDeptName, SFunction<T, ?>... columns) {
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
        appendCreateTimeCondition(wrapper, hasCondition, keyword);
    }

    @SafeVarargs
    private static <T> void appendJoinLikeGroup(MPJLambdaWrapper<T> wrapper, String keyword,
                                                boolean includeDeptName, SFunction<T, ?>... columns) {
        appendJoinLikeGroup(wrapper, keyword, includeDeptName, false, columns);
    }

    @SafeVarargs
    private static <T> void appendJoinLikeGroup(MPJLambdaWrapper<T> wrapper, String keyword,
                                                boolean includeDeptName, boolean includePurchaseSupplier,
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
        hasCondition = appendJoinPurchaseSupplierCondition(wrapper, includePurchaseSupplier, hasCondition, keyword);
        appendJoinCreateTimeCondition(wrapper, hasCondition, keyword);
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
}
