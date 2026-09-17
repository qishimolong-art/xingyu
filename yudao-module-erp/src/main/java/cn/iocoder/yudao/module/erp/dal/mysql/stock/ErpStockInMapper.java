package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

/**
 * ERP 其它入库�?Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockInMapper extends BaseMapperX<ErpStockInDO> {

    default PageResult<ErpStockInDO> selectPage(ErpStockInPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpStockInDO> query = new MPJLambdaWrapperX<ErpStockInDO>()
                .likeIfPresent(ErpStockInDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpStockInDO::getSupplierId, reqVO.getSupplierId())
                .betweenIfPresent(ErpStockInDO::getInTime, reqVO.getInTime())
                .eqIfPresent(ErpStockInDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpStockInDO::getDeptId, reqVO.getDeptId())
                .likeIfPresent(ErpStockInDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpStockInDO::getCreator, reqVO.getCreator());
        if (reqVO.getWarehouseId() != null || reqVO.getProductId() != null || StringUtils.hasText(reqVO.getProductKeyword())) {
            query.leftJoin(ErpStockInItemDO.class, ErpStockInItemDO::getInId, ErpStockInDO::getId)
                    .leftJoin(ErpProductDO.class, ErpProductDO::getId, ErpStockInItemDO::getProductId)
                    .leftJoin(ErpProductUnitDO.class, ErpProductUnitDO::getId, ErpStockInItemDO::getProductUnitId)
                    .eq(reqVO.getWarehouseId() != null, ErpStockInItemDO::getWarehouseId, reqVO.getWarehouseId())
                    .eq(reqVO.getProductId() != null, ErpStockInItemDO::getProductId, reqVO.getProductId())
                    .and(StringUtils.hasText(reqVO.getProductKeyword()), w -> ErpKeywordQuery.appendProductKeyword(w, reqVO.getProductKeyword()))
                    .groupBy(ErpStockInDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        ErpKeywordQuery.appendWithDeptNameAndPurchaseSupplierAndProductItemTokens(query, reqVO.getKeyword(),
                "erp_stock_in_item", "in_id",
                ErpStockInDO::getNo, ErpStockInDO::getRemark);
        orderBy(query, reqVO);
        return selectJoinPage(reqVO, ErpStockInDO.class, query);
    }

    static void orderBy(MPJLambdaWrapperX<ErpStockInDO> query, ErpStockInPageReqVO reqVO) {
        SFunction<ErpStockInDO, ?> column = getOrderColumn(reqVO.getOrderField());
        boolean asc = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        boolean desc = "desc".equalsIgnoreCase(reqVO.getOrderDirection());
        if (column != null && (asc || desc)) query.orderBy(true, asc, column);
        query.orderByDesc(ErpStockInDO::getId);
    }

    static SFunction<ErpStockInDO, ?> getOrderColumn(String field) {
        if (field == null) return null;
        switch (field.trim()) {
            case "no": return ErpStockInDO::getNo;
            case "inTime": return ErpStockInDO::getInTime;
            case "updateTime": return ErpStockInDO::getUpdateTime;
            case "status": return ErpStockInDO::getStatus;
            default: return null;
        }
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpStockInDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpStockInDO>()
                .eq(ErpStockInDO::getId, id).eq(ErpStockInDO::getStatus, status));
    }

    default ErpStockInDO selectByNo(String no) {
        return selectOne(ErpStockInDO::getNo, no);
    }

}
