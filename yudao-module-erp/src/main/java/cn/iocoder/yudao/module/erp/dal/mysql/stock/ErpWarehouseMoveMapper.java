package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMovePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface ErpWarehouseMoveMapper extends BaseMapperX<ErpWarehouseMoveDO> {

    String EXPLICIT_KEYWORD_DELIMITER_REGEX = "[/\\\\,，;；+|]+";
    String SPACE_KEYWORD_DELIMITER_REGEX = "[\\s\\u3000]+";
    String WAREHOUSE_MOVE_PRODUCT_ITEM_KEYWORD_SQL_PREFIX = "EXISTS (SELECT 1 FROM erp_warehouse_move_item i "
            + "LEFT JOIN erp_product p ON p.id = i.product_id AND p.deleted = b'0' AND p.tenant_id = t.tenant_id "
            + "LEFT JOIN erp_product_unit u ON u.id = COALESCE(i.product_unit_id, p.unit_id) "
            + "AND u.deleted = b'0' AND u.tenant_id = t.tenant_id "
            + "WHERE i.move_id = t.id AND i.deleted = b'0' AND i.tenant_id = t.tenant_id";
    String WAREHOUSE_MOVE_PRODUCT_ITEM_KEYWORD_SQL_SUFFIX = ")";
    String WAREHOUSE_MOVE_PRODUCT_ITEM_KEYWORD_CONDITION = "(p.code LIKE {0} OR p.name LIKE {0} "
            + "OR p.pinyin_code LIKE {0} OR p.wubi_code LIKE {0} OR p.bar_code LIKE {0} "
            + "OR p.vehicle_model LIKE {0} OR p.factory_code LIKE {0} OR p.standard LIKE {0} "
            + "OR p.brand LIKE {0} OR p.drawing_no LIKE {0} OR u.name LIKE {0})";
    String JOIN_DEPT_NAME_LIKE_SQL = "EXISTS (SELECT 1 FROM system_dept d "
            + "WHERE d.id = t.dept_id AND d.deleted = b'0' AND d.name LIKE {0})";
    String JOIN_CREATE_TIME_LIKE_SQL = "DATE_FORMAT(t.create_time, '%Y-%m-%d %H:%i:%s') LIKE {0}";

    default PageResult<ErpWarehouseMoveDO> selectPage(ErpWarehouseMovePageReqVO reqVO) {
        MPJLambdaWrapperX<ErpWarehouseMoveDO> query = buildQuery(reqVO);
        orderBy(query, reqVO);
        return selectJoinPage(reqVO, ErpWarehouseMoveDO.class, query);
    }

    default List<ErpWarehouseMoveDO> selectList(ErpWarehouseMovePageReqVO reqVO) {
        MPJLambdaWrapperX<ErpWarehouseMoveDO> query = buildQuery(reqVO);
        orderBy(query, reqVO);
        return selectJoinList(ErpWarehouseMoveDO.class, query);
    }

    default MPJLambdaWrapperX<ErpWarehouseMoveDO> buildQuery(ErpWarehouseMovePageReqVO reqVO) {
        MPJLambdaWrapperX<ErpWarehouseMoveDO> query = new MPJLambdaWrapperX<ErpWarehouseMoveDO>()
                .inIfPresent(ErpWarehouseMoveDO::getId, reqVO.getIds())
                .likeIfPresent(ErpWarehouseMoveDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpWarehouseMoveDO::getMoveTime, reqVO.getMoveTime())
                .eqIfPresent(ErpWarehouseMoveDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpWarehouseMoveDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(ErpWarehouseMoveDO::getSourceId, reqVO.getSourceId())
                .likeIfPresent(ErpWarehouseMoveDO::getSourceNo, reqVO.getSourceNo())
                .eqIfPresent(ErpWarehouseMoveDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpWarehouseMoveDO::getFromWarehouseId, reqVO.getFromWarehouseId())
                .eqIfPresent(ErpWarehouseMoveDO::getToWarehouseId, reqVO.getToWarehouseId())
                .likeIfPresent(ErpWarehouseMoveDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpWarehouseMoveDO::getCreator, reqVO.getCreator());
        if (reqVO.getProductId() != null || StringUtils.hasText(reqVO.getProductKeyword())) {
            query.leftJoin(ErpWarehouseMoveItemDO.class, ErpWarehouseMoveItemDO::getMoveId, ErpWarehouseMoveDO::getId)
                    .leftJoin(ErpProductDO.class, ErpProductDO::getId, ErpWarehouseMoveItemDO::getProductId)
                    .leftJoin(ErpProductUnitDO.class, ErpProductUnitDO::getId, ErpWarehouseMoveItemDO::getProductUnitId)
                    .eq(reqVO.getProductId() != null, ErpWarehouseMoveItemDO::getProductId, reqVO.getProductId())
                    .and(StringUtils.hasText(reqVO.getProductKeyword()), w -> appendProductKeyword(w, reqVO.getProductKeyword()))
                    .groupBy(ErpWarehouseMoveDO::getId);
        }
        appendWarehouseMoveKeyword(query, reqVO.getKeyword());
        return query;
    }

    static void orderBy(MPJLambdaWrapperX<ErpWarehouseMoveDO> query, ErpWarehouseMovePageReqVO reqVO) {
        String expression = getOrderExpression(reqVO.getOrderField());
        SFunction<ErpWarehouseMoveDO, ?> column = getOrderColumn(reqVO.getOrderField());
        boolean asc = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        boolean desc = "desc".equalsIgnoreCase(reqVO.getOrderDirection());
        if (expression != null && (asc || desc)) {
            query.last("ORDER BY " + expression + (asc ? " ASC" : " DESC") + ", t.id DESC");
            return;
        }
        if (column != null && (asc || desc)) query.orderBy(true, asc, column);
        query.orderByDesc(ErpWarehouseMoveDO::getId);
    }

    static String getOrderExpression(String field) {
        if (field == null) return null;
        switch (field.trim()) {
            case "fromWarehouseName": return warehouseNameExpression("from_warehouse_id");
            case "toWarehouseName": return warehouseNameExpression("to_warehouse_id");
            case "productNames": return productFieldExpression("name");
            case "productCodes": return productFieldExpression("code");
            case "deptName":
                return "(SELECT d.name FROM system_dept d WHERE d.id = t.dept_id AND d.deleted = b'0')";
            case "handlerName": return userNameExpression("handler_id");
            case "itemCount":
                return "(SELECT COUNT(*) FROM erp_warehouse_move_item i "
                        + "WHERE i.move_id = t.id AND i.deleted = b'0')";
            case "approveUserName": return userNameExpression("approve_user_id");
            case "creatorName": return userNameExpression("creator");
            default: return null;
        }
    }

    static String warehouseNameExpression(String foreignKey) {
        return "(SELECT w.name FROM erp_warehouse w WHERE w.id = t." + foreignKey
                + " AND w.deleted = b'0')";
    }

    static String userNameExpression(String foreignKey) {
        return "(SELECT u.nickname FROM system_users u WHERE u.id = t." + foreignKey
                + " AND u.deleted = b'0')";
    }

    static String productFieldExpression(String column) {
        return "(SELECT MIN(p." + column + ") FROM erp_warehouse_move_item i "
                + "INNER JOIN erp_product p ON p.id = i.product_id AND p.deleted = b'0' "
                + "WHERE i.move_id = t.id AND i.deleted = b'0')";
    }

    static SFunction<ErpWarehouseMoveDO, ?> getOrderColumn(String field) {
        if (field == null) return null;
        switch (field.trim()) {
            case "no": return ErpWarehouseMoveDO::getNo;
            case "moveTime": return ErpWarehouseMoveDO::getMoveTime;
            case "totalCount": return ErpWarehouseMoveDO::getTotalCount;
            case "totalCostAmount": return ErpWarehouseMoveDO::getTotalCostAmount;
            case "totalPrice": return ErpWarehouseMoveDO::getTotalPrice;
            case "status": return ErpWarehouseMoveDO::getStatus;
            case "approveTime": return ErpWarehouseMoveDO::getApproveTime;
            case "createTime": return ErpWarehouseMoveDO::getCreateTime;
            case "remark": return ErpWarehouseMoveDO::getRemark;
            default: return null;
        }
    }

    static void appendProductKeyword(MPJLambdaWrapper<ErpWarehouseMoveDO> query, String keyword) {
        List<String> tokens = splitKeywordTokens(keyword);
        if (shouldUseKeywordTokens(keyword, tokens)) {
            tokens.forEach(token -> query.and(w -> appendSingleProductKeyword(w, normalizeKeyword(token))));
            return;
        }
        appendSingleProductKeyword(query, normalizeKeyword(keyword));
    }

    static void appendSingleProductKeyword(MPJLambdaWrapper<ErpWarehouseMoveDO> query, String value) {
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

    static void appendWarehouseMoveKeyword(MPJLambdaWrapperX<ErpWarehouseMoveDO> query, String keyword) {
        String value = normalizeKeyword(keyword);
        if (!StringUtils.hasText(value)) {
            return;
        }
        List<String> tokens = splitKeywordTokens(keyword);
        query.and(w -> {
            appendWarehouseMoveSingleKeyword(w, value);
            if (shouldUseKeywordTokens(keyword, tokens)) {
                w.or(or -> appendWarehouseMoveProductItemTokens(or, tokens));
            }
        });
    }

    static void appendWarehouseMoveSingleKeyword(MPJLambdaWrapper<ErpWarehouseMoveDO> query, String value) {
        String likeValue = "%" + value + "%";
        query.like(ErpWarehouseMoveDO::getNo, value)
                .or().like(ErpWarehouseMoveDO::getSourceNo, value)
                .or().like(ErpWarehouseMoveDO::getRemark, value)
                .or().apply(JOIN_DEPT_NAME_LIKE_SQL, likeValue)
                .or().apply(buildWarehouseMoveProductItemKeywordSql(1), likeValue)
                .or().apply(JOIN_CREATE_TIME_LIKE_SQL, likeValue);
    }

    static void appendWarehouseMoveProductItemTokens(MPJLambdaWrapper<ErpWarehouseMoveDO> query, List<String> tokens) {
        Object[] values = tokens.stream()
                .map(ErpWarehouseMoveMapper::normalizeKeyword)
                .map(value -> "%" + value + "%")
                .toArray();
        query.apply(buildWarehouseMoveProductItemKeywordSql(tokens.size()), values);
    }

    static String buildWarehouseMoveProductItemKeywordSql(int tokenCount) {
        StringBuilder sql = new StringBuilder(WAREHOUSE_MOVE_PRODUCT_ITEM_KEYWORD_SQL_PREFIX);
        for (int i = 0; i < tokenCount; i++) {
            sql.append(" AND ").append(WAREHOUSE_MOVE_PRODUCT_ITEM_KEYWORD_CONDITION.replace("{0}", "{" + i + "}"));
        }
        sql.append(WAREHOUSE_MOVE_PRODUCT_ITEM_KEYWORD_SQL_SUFFIX);
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

    default int updateByIdAndStatus(Long id, Integer status, ErpWarehouseMoveDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpWarehouseMoveDO>()
                .eq(ErpWarehouseMoveDO::getId, id).eq(ErpWarehouseMoveDO::getStatus, status));
    }

    default int updateDraftByIdAndStatus(Long id, Integer status, ErpWarehouseMoveDO updateObj) {
        return update(null, new LambdaUpdateWrapper<ErpWarehouseMoveDO>()
                .eq(ErpWarehouseMoveDO::getId, id)
                .eq(ErpWarehouseMoveDO::getStatus, status)
                .set(ErpWarehouseMoveDO::getDeptId, updateObj.getDeptId())
                .set(ErpWarehouseMoveDO::getMoveTime, updateObj.getMoveTime())
                .set(ErpWarehouseMoveDO::getFromWarehouseId, updateObj.getFromWarehouseId())
                .set(ErpWarehouseMoveDO::getToWarehouseId, updateObj.getToWarehouseId())
                .set(ErpWarehouseMoveDO::getHandlerId, updateObj.getHandlerId())
                .set(ErpWarehouseMoveDO::getSourceType, updateObj.getSourceType())
                .set(ErpWarehouseMoveDO::getSourceId, updateObj.getSourceId())
                .set(ErpWarehouseMoveDO::getSourceNo, updateObj.getSourceNo())
                .set(ErpWarehouseMoveDO::getTotalCount, updateObj.getTotalCount())
                .set(ErpWarehouseMoveDO::getTotalPrice, updateObj.getTotalPrice())
                .set(ErpWarehouseMoveDO::getTotalCostAmount, updateObj.getTotalCostAmount())
                .set(ErpWarehouseMoveDO::getRemark, updateObj.getRemark())
                .set(ErpWarehouseMoveDO::getFileUrl, updateObj.getFileUrl()));
    }

    default ErpWarehouseMoveDO selectByNo(String no) {
        return selectOne(ErpWarehouseMoveDO::getNo, no);
    }

}
