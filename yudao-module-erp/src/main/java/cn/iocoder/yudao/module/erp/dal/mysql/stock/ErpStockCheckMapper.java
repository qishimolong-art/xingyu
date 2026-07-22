package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 库存调拨�?Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockCheckMapper extends BaseMapperX<ErpStockCheckDO> {

    default PageResult<ErpStockCheckDO> selectPage(ErpStockCheckPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpStockCheckDO> query = new MPJLambdaWrapperX<ErpStockCheckDO>()
                .likeIfPresent(ErpStockCheckDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpStockCheckDO::getCheckTime, reqVO.getCheckTime())
                .eqIfPresent(ErpStockCheckDO::getCheckType, reqVO.getCheckType())
                .eqIfPresent(ErpStockCheckDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpStockCheckDO::getDeptId, reqVO.getDeptId())
                .likeIfPresent(ErpStockCheckDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpStockCheckDO::getCreator, reqVO.getCreator());
        if (reqVO.getWarehouseId() != null || reqVO.getProductId() != null) {
            query.leftJoin(ErpStockCheckItemDO.class, ErpStockCheckItemDO::getCheckId, ErpStockCheckDO::getId)
                    .eq(reqVO.getWarehouseId() != null, ErpStockCheckItemDO::getWarehouseId, reqVO.getWarehouseId())
                    .eq(reqVO.getProductId() != null, ErpStockCheckItemDO::getProductId, reqVO.getProductId())
                    .groupBy(ErpStockCheckDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        ErpKeywordQuery.appendWithDeptName(query, reqVO.getKeyword(),
                ErpStockCheckDO::getNo, ErpStockCheckDO::getRemark);
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

    default int updateByIdAndStatus(Long id, Integer status, ErpStockCheckDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpStockCheckDO>()
                .eq(ErpStockCheckDO::getId, id).eq(ErpStockCheckDO::getStatus, status));
    }

    default ErpStockCheckDO selectByNo(String no) {
        return selectOne(ErpStockCheckDO::getNo, no);
    }

}
