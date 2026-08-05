package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMovePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpWarehouseMoveMapper extends BaseMapperX<ErpWarehouseMoveDO> {

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
        if (reqVO.getProductId() != null) {
            query.leftJoin(ErpWarehouseMoveItemDO.class, ErpWarehouseMoveItemDO::getMoveId, ErpWarehouseMoveDO::getId)
                    .eq(ErpWarehouseMoveItemDO::getProductId, reqVO.getProductId())
                    .groupBy(ErpWarehouseMoveDO::getId);
        }
        ErpKeywordQuery.appendWithDeptName(query, reqVO.getKeyword(),
                ErpWarehouseMoveDO::getNo, ErpWarehouseMoveDO::getSourceNo, ErpWarehouseMoveDO::getRemark);
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
