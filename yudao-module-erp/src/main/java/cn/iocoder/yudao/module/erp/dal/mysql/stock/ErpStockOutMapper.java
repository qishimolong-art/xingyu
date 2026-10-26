package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockDocumentSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

/**
 * ERP 其它出库�?Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockOutMapper extends BaseMapperX<ErpStockOutDO> {

    default PageResult<ErpStockOutDO> selectPage(ErpStockOutPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpStockOutDO> query = buildQuery(reqVO);
        orderBy(query, reqVO);
        return selectJoinPage(reqVO, ErpStockOutDO.class, query);
    }

    default ErpStockDocumentSummaryRespVO selectSummary(ErpStockOutPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpStockOutDO> query = buildQuery(reqVO)
                .selectAs("COALESCE(SUM(t.total_count), 0)", ErpStockDocumentSummaryRespVO::getTotalCount)
                .selectAs("COALESCE(SUM(t.total_price), 0)", ErpStockDocumentSummaryRespVO::getTotalPrice);
        return selectJoinOne(ErpStockDocumentSummaryRespVO.class, query);
    }

    static MPJLambdaWrapperX<ErpStockOutDO> buildQuery(ErpStockOutPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpStockOutDO> query = new MPJLambdaWrapperX<ErpStockOutDO>()
                .inIfPresent(ErpStockOutDO::getId, reqVO.getIds())
                .likeIfPresent(ErpStockOutDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpStockOutDO::getCustomerId, reqVO.getCustomerId())
                .betweenIfPresent(ErpStockOutDO::getOutTime, reqVO.getOutTime())
                .eqIfPresent(ErpStockOutDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpStockOutDO::getDeptId, reqVO.getDeptId())
                .likeIfPresent(ErpStockOutDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpStockOutDO::getCreator, reqVO.getCreator());
        if (reqVO.getPickStatus() != null) {
            query.apply("EXISTS (SELECT 1 FROM erp_sale_pick_delivery_order pdo "
                            + "WHERE pdo.source_type = {0} AND pdo.source_id = t.id "
                            + "AND pdo.pick_status = {1} AND pdo.deleted = b'0')",
                    ErpSaleBizSourceTypeEnum.STOCK_OUT.getType(), reqVO.getPickStatus());
        }
        ErpKeywordQuery.appendProductItemFilter(query, "erp_stock_out_item", "out_id",
                reqVO.getProductId(), reqVO.getWarehouseId(), null, null,
                reqVO.getProductKeyword(), true);
        ErpKeywordQuery.appendWithDeptNameAndSaleCustomerAndProductItemTokens(query, reqVO.getKeyword(),
                "erp_stock_out_item", "out_id",
                ErpStockOutDO::getNo, ErpStockOutDO::getRemark);
        return query;
    }

    static void orderBy(MPJLambdaWrapperX<ErpStockOutDO> query, ErpStockOutPageReqVO reqVO) {
        SFunction<ErpStockOutDO, ?> column = getOrderColumn(reqVO.getOrderField());
        boolean asc = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        boolean desc = "desc".equalsIgnoreCase(reqVO.getOrderDirection());
        if (column != null && (asc || desc)) query.orderBy(true, asc, column);
        query.orderByDesc(ErpStockOutDO::getId);
    }

    static SFunction<ErpStockOutDO, ?> getOrderColumn(String field) {
        if (field == null) return null;
        switch (field.trim()) {
            case "no": return ErpStockOutDO::getNo;
            case "outTime": return ErpStockOutDO::getOutTime;
            case "updateTime": return ErpStockOutDO::getUpdateTime;
            case "status": return ErpStockOutDO::getStatus;
            default: return null;
        }
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpStockOutDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpStockOutDO>()
                .eq(ErpStockOutDO::getId, id).eq(ErpStockOutDO::getStatus, status));
    }

    default ErpStockOutDO selectByIdForUpdate(Long id) {
        return selectOne(new MPJLambdaWrapperX<ErpStockOutDO>()
                .eq(ErpStockOutDO::getId, id).last("FOR UPDATE"));
    }

    default int cancelConfirmByIdAndStatus(Long id, Integer status, Integer targetStatus) {
        return update(new ErpStockOutDO().setStatus(targetStatus), new LambdaUpdateWrapper<ErpStockOutDO>()
                .eq(ErpStockOutDO::getId, id).eq(ErpStockOutDO::getStatus, status)
                .set(ErpStockOutDO::getConfirmUserId, null).set(ErpStockOutDO::getConfirmTime, null));
    }

    default ErpStockOutDO selectByNo(String no) {
        return selectOne(ErpStockOutDO::getNo, no);
    }

}
