package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockDocumentSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
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
        MPJLambdaWrapperX<ErpStockInDO> query = buildQuery(reqVO);
        orderBy(query, reqVO);
        return selectJoinPage(reqVO, ErpStockInDO.class, query);
    }

    default ErpStockDocumentSummaryRespVO selectSummary(ErpStockInPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpStockInDO> query = buildQuery(reqVO)
                .selectAs("COALESCE(SUM(t.total_count), 0)", ErpStockDocumentSummaryRespVO::getTotalCount)
                .selectAs("COALESCE(SUM(t.total_price), 0)", ErpStockDocumentSummaryRespVO::getTotalPrice);
        return selectJoinOne(ErpStockDocumentSummaryRespVO.class, query);
    }

    static MPJLambdaWrapperX<ErpStockInDO> buildQuery(ErpStockInPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpStockInDO> query = new MPJLambdaWrapperX<ErpStockInDO>()
                .inIfPresent(ErpStockInDO::getId, reqVO.getIds())
                .likeIfPresent(ErpStockInDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpStockInDO::getSupplierId, reqVO.getSupplierId())
                .betweenIfPresent(ErpStockInDO::getInTime, reqVO.getInTime())
                .eqIfPresent(ErpStockInDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpStockInDO::getDeptId, reqVO.getDeptId())
                .likeIfPresent(ErpStockInDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpStockInDO::getCreator, reqVO.getCreator());
        if (reqVO.getPickStatus() != null) {
            query.apply("EXISTS (SELECT 1 FROM erp_sale_pick_delivery_order pdo "
                            + "WHERE pdo.source_type = {0} AND pdo.source_id = t.id "
                            + "AND pdo.pick_status = {1} AND pdo.deleted = b'0')",
                    ErpSaleBizSourceTypeEnum.STOCK_IN.getType(), reqVO.getPickStatus());
        }
        ErpKeywordQuery.appendProductItemFilter(query, "erp_stock_in_item", "in_id",
                reqVO.getProductId(), reqVO.getWarehouseId(), null, null,
                reqVO.getProductKeyword(), true);
        ErpKeywordQuery.appendWithDeptNameAndPurchaseSupplierAndProductItemTokens(query, reqVO.getKeyword(),
                "erp_stock_in_item", "in_id",
                ErpStockInDO::getNo, ErpStockInDO::getRemark);
        return query;
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

    default ErpStockInDO selectByIdForUpdate(Long id) {
        return selectOne(new MPJLambdaWrapperX<ErpStockInDO>()
                .eq(ErpStockInDO::getId, id).last("FOR UPDATE"));
    }

    default int cancelConfirmByIdAndStatus(Long id, Integer status, Integer targetStatus) {
        return update(new ErpStockInDO().setStatus(targetStatus), new LambdaUpdateWrapper<ErpStockInDO>()
                .eq(ErpStockInDO::getId, id).eq(ErpStockInDO::getStatus, status)
                .set(ErpStockInDO::getConfirmUserId, null).set(ErpStockInDO::getConfirmTime, null));
    }

    default ErpStockInDO selectByNo(String no) {
        return selectOne(ErpStockInDO::getNo, no);
    }

}
