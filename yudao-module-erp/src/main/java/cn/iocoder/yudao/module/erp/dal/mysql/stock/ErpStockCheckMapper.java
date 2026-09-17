package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 库存调拨�?Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockCheckMapper extends BaseMapperX<ErpStockCheckDO> {

    String EXPLICIT_KEYWORD_DELIMITER_REGEX = "[/\\\\,，;；+|]+";
    String SPACE_KEYWORD_DELIMITER_REGEX = "[\\s\\u3000]+";
    String STOCK_CHECK_PRODUCT_ITEM_KEYWORD_SQL_PREFIX = "EXISTS (SELECT 1 FROM erp_stock_check_item i "
            + "LEFT JOIN erp_product p ON p.id = i.product_id AND p.deleted = b'0' AND p.tenant_id = t.tenant_id "
            + "LEFT JOIN erp_product_unit u ON u.id = COALESCE(i.product_unit_id, p.unit_id) "
            + "AND u.deleted = b'0' AND u.tenant_id = t.tenant_id "
            + "WHERE i.check_id = t.id AND i.deleted = b'0' AND i.tenant_id = t.tenant_id";
    String STOCK_CHECK_PRODUCT_ITEM_KEYWORD_SQL_SUFFIX = ")";
    String STOCK_CHECK_PRODUCT_ITEM_KEYWORD_CONDITION = "(p.code LIKE {0} OR p.name LIKE {0} "
            + "OR p.pinyin_code LIKE {0} OR p.wubi_code LIKE {0} OR p.bar_code LIKE {0} "
            + "OR p.vehicle_model LIKE {0} OR p.factory_code LIKE {0} OR p.standard LIKE {0} "
            + "OR p.brand LIKE {0} OR p.drawing_no LIKE {0} OR u.name LIKE {0})";
    String JOIN_DEPT_NAME_LIKE_SQL = "EXISTS (SELECT 1 FROM system_dept d "
            + "WHERE d.id = t.dept_id AND d.deleted = b'0' AND d.name LIKE {0})";
    String JOIN_CREATE_TIME_LIKE_SQL = "DATE_FORMAT(t.create_time, '%Y-%m-%d %H:%i:%s') LIKE {0}";

    default PageResult<ErpStockCheckDO> selectPage(ErpStockCheckPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpStockCheckDO> query = new MPJLambdaWrapperX<ErpStockCheckDO>()
                .inIfPresent(ErpStockCheckDO::getId, reqVO.getIds())
                .likeIfPresent(ErpStockCheckDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpStockCheckDO::getCheckTime, reqVO.getCheckTime())
                .eqIfPresent(ErpStockCheckDO::getCheckType, reqVO.getCheckType())
                .eqIfPresent(ErpStockCheckDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpStockCheckDO::getDeptId, reqVO.getDeptId())
                .likeIfPresent(ErpStockCheckDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpStockCheckDO::getCreator, reqVO.getCreator());
        if (reqVO.getWarehouseId() != null || reqVO.getProductId() != null || StringUtils.hasText(reqVO.getProductKeyword())) {
            query.leftJoin(ErpStockCheckItemDO.class, ErpStockCheckItemDO::getCheckId, ErpStockCheckDO::getId)
                    .leftJoin(ErpProductDO.class, ErpProductDO::getId, ErpStockCheckItemDO::getProductId)
                    .leftJoin(ErpProductUnitDO.class, ErpProductUnitDO::getId, ErpStockCheckItemDO::getProductUnitId)
                    .eq(reqVO.getWarehouseId() != null, ErpStockCheckItemDO::getWarehouseId, reqVO.getWarehouseId())
                    .eq(reqVO.getProductId() != null, ErpStockCheckItemDO::getProductId, reqVO.getProductId())
                    .and(StringUtils.hasText(reqVO.getProductKeyword()), w -> appendProductKeyword(w, reqVO.getProductKeyword()))
                    .groupBy(ErpStockCheckDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        appendStockCheckKeyword(query, reqVO.getKeyword());
        orderBy(query, reqVO);
        return selectJoinPage(reqVO, ErpStockCheckDO.class, query);
    }

    static void orderBy(MPJLambdaWrapperX<ErpStockCheckDO> query, ErpStockCheckPageReqVO reqVO) {
        String expression = getOrderExpression(reqVO.getOrderField());
        SFunction<ErpStockCheckDO, ?> column = getOrderColumn(reqVO.getOrderField());
        boolean asc = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        boolean desc = "desc".equalsIgnoreCase(reqVO.getOrderDirection());
        if (expression != null && (asc || desc)) {
            query.last("ORDER BY " + expression + (asc ? " ASC" : " DESC") + ", t.id DESC");
            return;
        }
        if (column != null && (asc || desc)) query.orderBy(true, asc, column);
        query.orderByDesc(ErpStockCheckDO::getId);
    }

    static String getOrderExpression(String field) {
        if (field == null) return null;
        switch (field.trim()) {
            case "productNames":
                return productFieldExpression("name");
            case "productCodes":
                return productFieldExpression("code");
            case "deptName":
                return "(SELECT d.name FROM system_dept d WHERE d.id = t.dept_id AND d.deleted = b'0')";
            case "creatorName":
                return "(SELECT u.nickname FROM system_users u WHERE u.id = t.creator AND u.deleted = b'0')";
            default:
                return null;
        }
    }

    static String productFieldExpression(String column) {
        return "(SELECT MIN(p." + column + ") FROM erp_stock_check_item i "
                + "INNER JOIN erp_product p ON p.id = i.product_id AND p.deleted = b'0' "
                + "WHERE i.check_id = t.id AND i.deleted = b'0')";
    }

    static SFunction<ErpStockCheckDO, ?> getOrderColumn(String field) {
        if (field == null) return null;
        switch (field.trim()) {
            case "no": return ErpStockCheckDO::getNo;
            case "checkType": return ErpStockCheckDO::getCheckType;
            case "checkTime": return ErpStockCheckDO::getCheckTime;
            case "updateTime": return ErpStockCheckDO::getUpdateTime;
            case "totalCount": return ErpStockCheckDO::getTotalCount;
            case "totalPrice": return ErpStockCheckDO::getTotalPrice;
            case "status": return ErpStockCheckDO::getStatus;
            default: return null;
        }
    }

    static void appendProductKeyword(MPJLambdaWrapper<ErpStockCheckDO> query, String keyword) {
        List<String> tokens = splitKeywordTokens(keyword);
        if (shouldUseKeywordTokens(keyword, tokens)) {
            tokens.forEach(token -> query.and(w -> appendSingleProductKeyword(w, normalizeKeyword(token))));
            return;
        }
        appendSingleProductKeyword(query, normalizeKeyword(keyword));
    }

    static void appendSingleProductKeyword(MPJLambdaWrapper<ErpStockCheckDO> query, String value) {
        query.like(ErpProductDO::getCode, value)
                .or().like(ErpProductDO::getName, value)
                .or().like(ErpProductDO::getPinyinCode, value)
                .or().like(ErpProductDO::getWubiCode, value)
                .or().like(ErpProductDO::getBarCode, value)
                .or().like(ErpProductDO::getVehicleModel, value)
                .or().like(ErpProductDO::getFactoryCode, value)
                .or().like(ErpProductDO::getStandard, value)
                .or().like(ErpProductDO::getBrand, value)
                .or().like(ErpProductDO::getDrawingNo, value)
                .or().like(ErpProductUnitDO::getName, value);
    }

    static void appendStockCheckKeyword(MPJLambdaWrapperX<ErpStockCheckDO> query, String keyword) {
        String value = normalizeKeyword(keyword);
        if (!StringUtils.hasText(value)) {
            return;
        }
        List<String> tokens = splitKeywordTokens(keyword);
        query.and(w -> {
            appendStockCheckSingleKeyword(w, value);
            if (shouldUseKeywordTokens(keyword, tokens)) {
                w.or(or -> appendStockCheckProductItemTokens(or, tokens));
            }
        });
    }

    static void appendStockCheckSingleKeyword(MPJLambdaWrapper<ErpStockCheckDO> query, String value) {
        String likeValue = "%" + value + "%";
        query.like(ErpStockCheckDO::getNo, value)
                .or().like(ErpStockCheckDO::getRemark, value)
                .or().apply(JOIN_DEPT_NAME_LIKE_SQL, likeValue)
                .or().apply(buildStockCheckProductItemKeywordSql(1), likeValue)
                .or().apply(JOIN_CREATE_TIME_LIKE_SQL, likeValue);
    }

    static void appendStockCheckProductItemTokens(MPJLambdaWrapper<ErpStockCheckDO> query, List<String> tokens) {
        Object[] values = tokens.stream()
                .map(ErpStockCheckMapper::normalizeKeyword)
                .map(value -> "%" + value + "%")
                .toArray();
        query.apply(buildStockCheckProductItemKeywordSql(tokens.size()), values);
    }

    static String buildStockCheckProductItemKeywordSql(int tokenCount) {
        StringBuilder sql = new StringBuilder(STOCK_CHECK_PRODUCT_ITEM_KEYWORD_SQL_PREFIX);
        for (int i = 0; i < tokenCount; i++) {
            sql.append(" AND ").append(STOCK_CHECK_PRODUCT_ITEM_KEYWORD_CONDITION.replace("{0}", "{" + i + "}"));
        }
        sql.append(STOCK_CHECK_PRODUCT_ITEM_KEYWORD_SQL_SUFFIX);
        return sql.toString();
    }

    static List<String> splitKeywordTokens(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return java.util.Collections.emptyList();
        }
        String rawKeyword = keyword.trim();
        List<String> explicitTokens = splitKeywordTokens(rawKeyword, EXPLICIT_KEYWORD_DELIMITER_REGEX);
        if (containsExplicitDelimiter(rawKeyword) && !explicitTokens.isEmpty()) {
            return explicitTokens;
        }
        return splitKeywordTokens(rawKeyword, SPACE_KEYWORD_DELIMITER_REGEX);
    }

    static List<String> splitKeywordTokens(String keyword, String delimiterRegex) {
        return Arrays.stream(keyword.split(delimiterRegex))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    static String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().replaceAll("\\s+", "%");
    }

    static boolean shouldUseKeywordTokens(String keyword, List<String> tokens) {
        return !tokens.isEmpty() && (tokens.size() > 1 || containsExplicitDelimiter(keyword));
    }

    static boolean containsExplicitDelimiter(String keyword) {
        return keyword.indexOf('/') >= 0
                || keyword.indexOf('\\') >= 0
                || keyword.indexOf(',') >= 0
                || keyword.indexOf('，') >= 0
                || keyword.indexOf(';') >= 0
                || keyword.indexOf('；') >= 0
                || keyword.indexOf('+') >= 0
                || keyword.indexOf('|') >= 0;
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpStockCheckDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpStockCheckDO>()
                .eq(ErpStockCheckDO::getId, id).eq(ErpStockCheckDO::getStatus, status));
    }

    default ErpStockCheckDO selectByNo(String no) {
        return selectOne(ErpStockCheckDO::getNo, no);
    }

}
