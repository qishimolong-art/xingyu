package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ERP 库存调拨�?Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockMoveMapper extends BaseMapperX<ErpStockMoveDO> {

    default PageResult<ErpStockMoveDO> selectPage(ErpStockMovePageReqVO reqVO) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = buildPageQuery(reqVO);
        orderBy(query, reqVO);
        return selectJoinPage(reqVO, ErpStockMoveDO.class, query);
    }

    default PageResult<ErpStockMoveDO> selectTransferOutPage(ErpStockMovePageReqVO reqVO,
                                                             Collection<Long> deptIds,
                                                             boolean all) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = buildPageQuery(reqVO);
        applyTransferOutVisibleScope(query, deptIds, all);
        orderBy(query, reqVO);
        return selectJoinPage(reqVO, ErpStockMoveDO.class, query);
    }

    default PageResult<ErpStockMoveDO> selectTransferInPage(ErpStockMovePageReqVO reqVO,
                                                            Collection<Long> deptIds,
                                                            boolean all) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = buildPageQuery(reqVO);
        applyTransferInVisibleScope(query, deptIds, all);
        orderBy(query, reqVO);
        return selectJoinPage(reqVO, ErpStockMoveDO.class, query);
    }

    default List<ErpStockMoveDO> selectTransferOutList(ErpStockMovePageReqVO reqVO,
                                                       Collection<Long> deptIds,
                                                       boolean all) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = buildPageQuery(reqVO);
        applyTransferOutVisibleScope(query, deptIds, all);
        query.orderByDesc(ErpStockMoveDO::getMoveTime).orderByDesc(ErpStockMoveDO::getId);
        return selectList(query);
    }

    default List<ErpStockMoveDO> selectTransferInList(ErpStockMovePageReqVO reqVO,
                                                      Collection<Long> deptIds,
                                                      boolean all) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = buildPageQuery(reqVO);
        applyTransferInVisibleScope(query, deptIds, all);
        query.orderByDesc(ErpStockMoveDO::getMoveTime).orderByDesc(ErpStockMoveDO::getId);
        return selectList(query);
    }

    default List<Long> selectTransferOutVisibleFromDeptIdList(Collection<Long> deptIds, boolean all) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = new MPJLambdaWrapperX<>();
        query.select("DISTINCT t.from_dept_id AS fromDeptId");
        query.isNotNull(ErpStockMoveDO::getFromDeptId);
        appendTransferDirection(query, 10);
        applyTransferOutVisibleScope(query, deptIds, all);
        return selectJoinList(ErpStockMoveDO.class, query).stream()
                .map(ErpStockMoveDO::getFromDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    default List<Long> selectTransferOutVisibleToDeptIdList(Collection<Long> deptIds, boolean all) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = new MPJLambdaWrapperX<>();
        query.select("DISTINCT t.to_dept_id AS toDeptId");
        query.isNotNull(ErpStockMoveDO::getToDeptId);
        appendTransferDirection(query, 10);
        applyTransferOutVisibleScope(query, deptIds, all);
        return selectJoinList(ErpStockMoveDO.class, query).stream()
                .map(ErpStockMoveDO::getToDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    default List<Long> selectTransferInVisibleFromDeptIdList(Collection<Long> deptIds, boolean all) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = new MPJLambdaWrapperX<>();
        query.select("DISTINCT t.from_dept_id AS fromDeptId");
        query.isNotNull(ErpStockMoveDO::getFromDeptId);
        appendTransferDirection(query, 20);
        applyTransferInVisibleScope(query, deptIds, all);
        return selectJoinList(ErpStockMoveDO.class, query).stream()
                .map(ErpStockMoveDO::getFromDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    default List<Long> selectTransferInVisibleToDeptIdList(Collection<Long> deptIds, boolean all) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = new MPJLambdaWrapperX<>();
        query.select("DISTINCT t.to_dept_id AS toDeptId");
        query.isNotNull(ErpStockMoveDO::getToDeptId);
        appendTransferDirection(query, 20);
        applyTransferInVisibleScope(query, deptIds, all);
        return selectJoinList(ErpStockMoveDO.class, query).stream()
                .map(ErpStockMoveDO::getToDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    default List<ErpStockMoveDO> selectLedgerRelatedList(Collection<Long> moveIds) {
        if (CollUtil.isEmpty(moveIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpStockMoveDO>()
                .and(query -> query.in(ErpStockMoveDO::getId, moveIds)
                        .or().in(ErpStockMoveDO::getRelatedMoveId, moveIds)));
    }

    default ErpStockMoveDO selectVisibleTransferOutById(Long id, Collection<Long> deptIds,
                                                        boolean all) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = new MPJLambdaWrapperX<ErpStockMoveDO>()
                .eq(ErpStockMoveDO::getId, id);
        appendTransferDirection(query, 10);
        applyTransferOutVisibleScope(query, deptIds, all);
        return selectOne(query);
    }

    default List<ErpStockMoveDO> selectVisibleTransferOutListByIds(Collection<Long> ids,
                                                                   Collection<Long> deptIds,
                                                                   boolean all) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = new MPJLambdaWrapperX<ErpStockMoveDO>()
                .in(ErpStockMoveDO::getId, ids);
        appendTransferDirection(query, 10);
        applyTransferOutVisibleScope(query, deptIds, all);
        return selectList(query);
    }

    default ErpStockMoveDO selectVisibleTransferInById(Long id, Collection<Long> deptIds,
                                                       boolean all) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = new MPJLambdaWrapperX<ErpStockMoveDO>()
                .eq(ErpStockMoveDO::getId, id);
        appendTransferDirection(query, 20);
        applyTransferInVisibleScope(query, deptIds, all);
        return selectOne(query);
    }

    static MPJLambdaWrapperX<ErpStockMoveDO> buildPageQuery(ErpStockMovePageReqVO reqVO) {
        MPJLambdaWrapperX<ErpStockMoveDO> query = new MPJLambdaWrapperX<ErpStockMoveDO>()
                .inIfPresent(ErpStockMoveDO::getId, reqVO.getIds())
                .likeIfPresent(ErpStockMoveDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpStockMoveDO::getRelatedMoveId, reqVO.getRelatedMoveId())
                .likeIfPresent(ErpStockMoveDO::getRelatedMoveNo, reqVO.getRelatedMoveNo())
                .eqIfPresent(ErpStockMoveDO::getFromDeptId, reqVO.getFromDeptId())
                .eqIfPresent(ErpStockMoveDO::getToDeptId, reqVO.getToDeptId())
                .betweenIfPresent(ErpStockMoveDO::getMoveTime, reqVO.getMoveTime())
                .eqIfPresent(ErpStockMoveDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpStockMoveDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(ErpStockMoveDO::getSourceId, reqVO.getSourceId())
                .likeIfPresent(ErpStockMoveDO::getSourceNo, reqVO.getSourceNo())
                .eqIfPresent(ErpStockMoveDO::getDeptId, reqVO.getDeptId())
                .likeIfPresent(ErpStockMoveDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpStockMoveDO::getCreator, reqVO.getCreator());
        appendTransferDirection(query, reqVO.getTransferDirection());
        if (reqVO.getFromWarehouseId() != null || reqVO.getToWarehouseId() != null
                || reqVO.getProductId() != null || StringUtils.hasText(reqVO.getProductKeyword())) {
            query.leftJoin(ErpStockMoveItemDO.class, ErpStockMoveItemDO::getMoveId, ErpStockMoveDO::getId)
                    .leftJoin(ErpProductDO.class, ErpProductDO::getId, ErpStockMoveItemDO::getProductId)
                    .leftJoin(ErpProductUnitDO.class, ErpProductUnitDO::getId, ErpStockMoveItemDO::getProductUnitId)
                    .eq(reqVO.getFromWarehouseId() != null, ErpStockMoveItemDO::getFromWarehouseId, reqVO.getFromWarehouseId())
                    .eq(reqVO.getToWarehouseId() != null, ErpStockMoveItemDO::getToWarehouseId, reqVO.getToWarehouseId())
                    .eq(reqVO.getProductId() != null, ErpStockMoveItemDO::getProductId, reqVO.getProductId())
                    .and(StringUtils.hasText(reqVO.getProductKeyword()), w -> ErpKeywordQuery.appendProductKeyword(w, reqVO.getProductKeyword()))
                    .groupBy(ErpStockMoveDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        appendStockMoveKeyword(query, reqVO.getKeyword());
        return query;
    }

    static void applyTransferOutVisibleScope(MPJLambdaWrapperX<ErpStockMoveDO> query,
                                             Collection<Long> deptIds,
                                             boolean all) {
        applyTransferVisibleScope(query, deptIds, all);
    }

    static String buildIndexedPlaceholders(int size) {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                placeholders.append(',');
            }
            placeholders.append('{').append(i).append('}');
        }
        return placeholders.toString();
    }

    static void applyTransferInVisibleScope(MPJLambdaWrapperX<ErpStockMoveDO> query,
                                            Collection<Long> deptIds,
                                            boolean all) {
        applyTransferVisibleScope(query, deptIds, all);
    }

    static void applyTransferVisibleScope(MPJLambdaWrapperX<ErpStockMoveDO> query,
                                          Collection<Long> deptIds,
                                          boolean all) {
        if (all) {
            return;
        }
        query.and(scope -> {
            if (CollUtil.isNotEmpty(deptIds)) {
                Object[] parameters = deptIds.toArray();
                String placeholders = buildIndexedPlaceholders(parameters.length);
                scope.apply("t.dept_id IN (" + placeholders + ")", parameters)
                        .or().apply("t.from_dept_id IN (" + placeholders + ")", parameters)
                        .or().apply("t.to_dept_id IN (" + placeholders + ")", parameters)
                        .or().apply("EXISTS (SELECT 1 FROM erp_stock_move_item i "
                                + "WHERE i.move_id = t.id AND i.deleted = b'0' "
                                + "AND i.from_dept_id IN (" + placeholders + "))", parameters)
                        .or().apply("EXISTS (SELECT 1 FROM erp_stock_move_item i "
                                + "WHERE i.move_id = t.id AND i.deleted = b'0' "
                                + "AND i.to_dept_id IN (" + placeholders + "))", parameters);
            } else {
                scope.apply("1 = 0");
            }
        });
    }

    static void appendTransferDirection(MPJLambdaWrapperX<ErpStockMoveDO> query, Integer transferDirection) {
        if (transferDirection == null) {
            return;
        }
        if (Integer.valueOf(10).equals(transferDirection)) {
            query.and(wrapper -> wrapper.eq(ErpStockMoveDO::getTransferDirection, transferDirection)
                    .or().isNull(ErpStockMoveDO::getTransferDirection));
            return;
        }
        query.eq(ErpStockMoveDO::getTransferDirection, transferDirection);
    }

    static void orderBy(MPJLambdaWrapperX<ErpStockMoveDO> query, ErpStockMovePageReqVO reqVO) {
        String expression = getOrderExpression(reqVO.getOrderField());
        SFunction<ErpStockMoveDO, ?> column = getOrderColumn(reqVO.getOrderField());
        boolean asc = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        boolean desc = "desc".equalsIgnoreCase(reqVO.getOrderDirection());
        if (expression != null && (asc || desc)) {
            query.last("ORDER BY " + expression + (asc ? " ASC" : " DESC") + ", t.id DESC");
            return;
        }
        if (column != null && (asc || desc)) query.orderBy(true, asc, column);
        query.orderByDesc(ErpStockMoveDO::getId);
    }

    static String getOrderExpression(String field) {
        if (field == null) return null;
        switch (field.trim()) {
            case "fromDeptName": return deptNameExpression("from_dept_id");
            case "toDeptName": return deptNameExpression("to_dept_id");
            case "fromWarehouseNames": return warehouseNameExpression("from_warehouse_id");
            case "toWarehouseNames": return warehouseNameExpression("to_warehouse_id");
            case "productNames": return productFieldExpression("name");
            case "productCodes": return productFieldExpression("code");
            case "creatorName": return userNameExpression("creator");
            case "updaterName": return userNameExpression("updater");
            default: return null;
        }
    }

    static String deptNameExpression(String foreignKey) {
        return "(SELECT d.name FROM system_dept d WHERE d.id = t." + foreignKey
                + " AND d.deleted = b'0')";
    }

    static String userNameExpression(String foreignKey) {
        return "(SELECT u.nickname FROM system_users u WHERE u.id = t." + foreignKey
                + " AND u.deleted = b'0')";
    }

    static String warehouseNameExpression(String foreignKey) {
        return "(SELECT MIN(w.name) FROM erp_stock_move_item i "
                + "INNER JOIN erp_warehouse w ON w.id = i." + foreignKey + " AND w.deleted = b'0' "
                + "WHERE i.move_id = t.id AND i.deleted = b'0')";
    }

    static String productFieldExpression(String column) {
        return "(SELECT MIN(p." + column + ") FROM erp_stock_move_item i "
                + "INNER JOIN erp_product p ON p.id = i.product_id AND p.deleted = b'0' "
                + "WHERE i.move_id = t.id AND i.deleted = b'0')";
    }

    static SFunction<ErpStockMoveDO, ?> getOrderColumn(String field) {
        if (field == null) return null;
        switch (field.trim()) {
            case "no": return ErpStockMoveDO::getNo;
            case "relatedMoveNo": return ErpStockMoveDO::getRelatedMoveNo;
            case "sourceNo": return ErpStockMoveDO::getSourceNo;
            case "moveTime": return ErpStockMoveDO::getMoveTime;
            case "totalCount": return ErpStockMoveDO::getTotalCount;
            case "totalPrice": return ErpStockMoveDO::getTotalPrice;
            case "status": return ErpStockMoveDO::getStatus;
            case "remark": return ErpStockMoveDO::getRemark;
            case "createTime": return ErpStockMoveDO::getCreateTime;
            case "updateTime": return ErpStockMoveDO::getUpdateTime;
            default: return null;
        }
    }

    static void appendStockMoveKeyword(MPJLambdaWrapperX<ErpStockMoveDO> query, String keyword) {
        String value = ErpKeywordQuery.normalize(keyword);
        if (!StringUtils.hasText(value)) {
            return;
        }
        String likeValue = "%" + value + "%";
        ErpKeywordQuery.KeywordSearch keywordSearch = ErpKeywordQuery.parseKeywordSearch(keyword);
        query.and(wrapper -> {
            wrapper.like(ErpStockMoveDO::getNo, value)
                    .or().like(ErpStockMoveDO::getRelatedMoveNo, value)
                    .or().like(ErpStockMoveDO::getSourceNo, value)
                    .or().like(ErpStockMoveDO::getRemark, value)
                    .or().apply("EXISTS (SELECT 1 FROM system_dept d "
                            + "WHERE d.id IN (t.dept_id, t.from_dept_id, t.to_dept_id) "
                            + "AND d.deleted = b'0' AND d.name LIKE {0})", likeValue)
                    .or().apply(buildStockMoveProductItemKeywordSql(1), likeValue);
            if (ErpKeywordQuery.shouldAppendTokenProductItemCondition(keywordSearch)) {
                wrapper.or(or -> appendStockMoveProductItemTokens(or, keywordSearch.tokens()));
            }
        });
    }

    static void appendStockMoveProductItemTokens(MPJLambdaWrapper<ErpStockMoveDO> query, List<String> tokens) {
        Object[] values = tokens.stream()
                .map(ErpKeywordQuery::normalize)
                .map(token -> "%" + token + "%")
                .toArray();
        query.apply(buildStockMoveProductItemKeywordSql(tokens.size()), values);
    }

    static String buildStockMoveProductItemKeywordSql(int tokenCount) {
        return ErpKeywordQuery.buildProductItemKeywordSql("erp_stock_move_item", "move_id",
                "t", tokenCount, true);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpStockMoveDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpStockMoveDO>()
                .eq(ErpStockMoveDO::getId, id).eq(ErpStockMoveDO::getStatus, status));
    }

    default int clearRelatedMove(Long id) {
        return update(null, new LambdaUpdateWrapper<ErpStockMoveDO>()
                .eq(ErpStockMoveDO::getId, id)
                .set(ErpStockMoveDO::getRelatedMoveId, null)
                .set(ErpStockMoveDO::getRelatedMoveNo, null));
    }

    default ErpStockMoveDO selectByNo(String no) {
        return selectOne(ErpStockMoveDO::getNo, no);
    }

    default ErpStockMoveDO selectByRelatedMoveIdAndDirection(Long relatedMoveId, Integer transferDirection) {
        return selectOne(ErpStockMoveDO::getRelatedMoveId, relatedMoveId,
                ErpStockMoveDO::getTransferDirection, transferDirection);
    }

    default ErpStockMoveDO selectBySourceAndDirection(Integer sourceType, Long sourceId, Integer transferDirection) {
        return selectFirstOne(ErpStockMoveDO::getSourceType, sourceType,
                ErpStockMoveDO::getSourceId, sourceId,
                ErpStockMoveDO::getTransferDirection, transferDirection);
    }

    default List<ErpStockMoveDO> selectListBySourceAndDirection(Integer sourceType, Long sourceId,
                                                               Integer transferDirection) {
        LambdaQueryWrapperX<ErpStockMoveDO> query = new LambdaQueryWrapperX<ErpStockMoveDO>()
                .eq(ErpStockMoveDO::getSourceType, sourceType)
                .eq(ErpStockMoveDO::getSourceId, sourceId);
        appendTransferDirection(query, transferDirection);
        return selectList(query);
    }

    default ErpStockMoveDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpStockMoveDO>()
                .eq(ErpStockMoveDO::getId, id)
                .last("FOR UPDATE"));
    }

    default List<ErpStockMoveDO> selectListBySourceAndDirectionForUpdate(Integer sourceType, Long sourceId,
                                                                        Integer transferDirection) {
        LambdaQueryWrapperX<ErpStockMoveDO> query = new LambdaQueryWrapperX<ErpStockMoveDO>()
                .eq(ErpStockMoveDO::getSourceType, sourceType)
                .eq(ErpStockMoveDO::getSourceId, sourceId);
        appendTransferDirection(query, transferDirection);
        query.last("FOR UPDATE");
        return selectList(query);
    }

    static void appendTransferDirection(LambdaQueryWrapperX<ErpStockMoveDO> query, Integer transferDirection) {
        if (Integer.valueOf(10).equals(transferDirection)) {
            query.and(wrapper -> wrapper.eq(ErpStockMoveDO::getTransferDirection, transferDirection)
                    .or().isNull(ErpStockMoveDO::getTransferDirection));
            return;
        }
        query.eq(ErpStockMoveDO::getTransferDirection, transferDirection);
    }

}
