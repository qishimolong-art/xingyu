package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
        if (reqVO.getFromWarehouseId() != null || reqVO.getToWarehouseId() != null || reqVO.getProductId() != null) {
            query.leftJoin(ErpStockMoveItemDO.class, ErpStockMoveItemDO::getMoveId, ErpStockMoveDO::getId)
                    .eq(reqVO.getFromWarehouseId() != null, ErpStockMoveItemDO::getFromWarehouseId, reqVO.getFromWarehouseId())
                    .eq(reqVO.getToWarehouseId() != null, ErpStockMoveItemDO::getToWarehouseId, reqVO.getToWarehouseId())
                    .eq(reqVO.getProductId() != null, ErpStockMoveItemDO::getProductId, reqVO.getProductId())
                    .groupBy(ErpStockMoveDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        appendStockMoveKeyword(query, reqVO.getKeyword());
        return query;
    }

    static void applyTransferOutVisibleScope(MPJLambdaWrapperX<ErpStockMoveDO> query,
                                             Collection<Long> deptIds,
                                             boolean all) {
        if (all) {
            return;
        }
        query.and(scope -> {
            if (CollUtil.isNotEmpty(deptIds)) {
                Object[] parameters = deptIds.toArray();
                scope.and(deptScope -> deptScope.in(ErpStockMoveDO::getFromDeptId, deptIds)
                        .apply("NOT EXISTS (SELECT 1 FROM erp_stock_move_item i "
                                        + "WHERE i.move_id = t.id AND i.deleted = b'0' "
                                        + "AND (i.from_dept_id IS NULL OR i.from_dept_id NOT IN ("
                                        + buildIndexedPlaceholders(parameters.length) + ")))", parameters));
            } else {
                scope.apply("1 = 0");
            }
        });
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
        if (all) {
            return;
        }
        query.and(scope -> {
            if (CollUtil.isNotEmpty(deptIds)) {
                Object[] parameters = deptIds.toArray();
                scope.and(deptScope -> deptScope.in(ErpStockMoveDO::getToDeptId, deptIds)
                        .apply("NOT EXISTS (SELECT 1 FROM erp_stock_move_item i "
                                        + "WHERE i.move_id = t.id AND i.deleted = b'0' "
                                        + "AND (i.to_dept_id IS NULL OR i.to_dept_id NOT IN ("
                                        + buildIndexedPlaceholders(parameters.length) + ")))", parameters));
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
        query.and(wrapper -> wrapper
                .like(ErpStockMoveDO::getNo, value)
                .or().like(ErpStockMoveDO::getRelatedMoveNo, value)
                .or().like(ErpStockMoveDO::getSourceNo, value)
                .or().like(ErpStockMoveDO::getRemark, value)
                .or().apply("EXISTS (SELECT 1 FROM system_dept d "
                        + "WHERE d.id IN (t.dept_id, t.from_dept_id, t.to_dept_id) "
                        + "AND d.deleted = b'0' AND d.name LIKE {0})", likeValue)
                .or().apply("EXISTS (SELECT 1 FROM erp_stock_move_item mi "
                        + "INNER JOIN erp_product p ON p.id = mi.product_id AND p.deleted = b'0' "
                        + "WHERE mi.move_id = t.id AND mi.deleted = b'0' "
                        + "AND (p.name LIKE {0} OR p.code LIKE {0}))", likeValue));
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
