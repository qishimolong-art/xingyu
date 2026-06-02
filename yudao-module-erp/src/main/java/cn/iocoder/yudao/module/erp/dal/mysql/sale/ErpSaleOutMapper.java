package cn.iocoder.yudao.module.erp.dal.mysql.sale;


import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Objects;

/**
 * ERP 销售出库 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSaleOutMapper extends BaseMapperX<ErpSaleOutDO> {

    default PageResult<ErpSaleOutDO> selectPage(ErpSaleOutPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpSaleOutDO> query = new MPJLambdaWrapperX<ErpSaleOutDO>()
                .likeIfPresent(ErpSaleOutDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpSaleOutDO::getCustomerId, reqVO.getCustomerId())
                .betweenIfPresent(ErpSaleOutDO::getOutTime, reqVO.getOutTime())
                .eqIfPresent(ErpSaleOutDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpSaleOutDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpSaleOutDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpSaleOutDO::getAccountId, reqVO.getAccountId())
                .likeIfPresent(ErpSaleOutDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(ErpSaleOutDO::getSourceType, reqVO.getSourceType())
                .likeIfPresent(ErpSaleOutDO::getSourceNo, reqVO.getSourceNo())
                .eqIfPresent(ErpSaleOutDO::getSaleUserId, reqVO.getSaleUserId())
                .inIfPresent(ErpSaleOutDO::getId, reqVO.getIds())
                .orderByDesc(ErpSaleOutDO::getId);
        // 收款状态。为什么需要 t. 的原因，是因为联表查询时，需要指定表名，不然会报字段不存在的错误
        if (Objects.equals(reqVO.getReceiptStatus(), ErpSaleOutPageReqVO.RECEIPT_STATUS_NONE)) {
            query.eq(ErpSaleOutDO::getReceiptPrice, 0);
        } else if (Objects.equals(reqVO.getReceiptStatus(), ErpSaleOutPageReqVO.RECEIPT_STATUS_PART)) {
            query.gt(ErpSaleOutDO::getReceiptPrice, 0).apply("t.receipt_price < t.total_price");
        } else if (Objects.equals(reqVO.getReceiptStatus(), ErpSaleOutPageReqVO.RECEIPT_STATUS_ALL)) {
            query.apply("t.receipt_price = t.total_price");
        }
        if (Boolean.TRUE.equals(reqVO.getReceiptEnable())) {
            query.eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply("t.receipt_price < t.total_price");
        }
        if (reqVO.getWarehouseId() != null || reqVO.getProductId() != null) {
            query.leftJoin(ErpSaleOutItemDO.class, ErpSaleOutItemDO::getOutId, ErpSaleOutDO::getId)
                    .eq(reqVO.getWarehouseId() != null, ErpSaleOutItemDO::getWarehouseId, reqVO.getWarehouseId())
                    .eq(reqVO.getProductId() != null, ErpSaleOutItemDO::getProductId, reqVO.getProductId())
                    .groupBy(ErpSaleOutDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        return selectJoinPage(reqVO, ErpSaleOutDO.class, query);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpSaleOutDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getId, id).eq(ErpSaleOutDO::getStatus, status));
    }

    default ErpSaleOutDO selectByNo(String no) {
        return selectOne(ErpSaleOutDO::getNo, no);
    }

    default List<ErpSaleOutDO> selectListByOrderId(Long orderId) {
        return selectList(ErpSaleOutDO::getOrderId, orderId);
    }

    default ErpSaleOutDO selectBySourceTypeAndSourceId(Integer sourceType, Long sourceId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getSourceType, sourceType)
                .eq(ErpSaleOutDO::getSourceId, sourceId)
                .last("LIMIT 1"));
    }

    @org.apache.ibatis.annotations.Select({
        "<script>",
        "SELECT c.id AS customerId,",
        "       so.lastSaleTime AS lastSaleTime,",
        "       IFNULL(so.totalSaleAmount, 0) AS totalSaleAmount,",
        "       IFNULL(ro.otherReceivableAmount, 0) AS otherReceivableAmount,",
        "       IFNULL(so.receivableBalance, 0) + IFNULL(ro.otherReceivableAmount, 0) AS receivableBalance",
        "  FROM erp_customer c",
        "  LEFT JOIN (",
        "       SELECT customer_id, MAX(out_time) AS lastSaleTime, SUM(total_price) AS totalSaleAmount,",
        "              SUM(total_price - IFNULL(receipt_price, 0)) AS receivableBalance",
        "         FROM erp_sale_out",
        "        WHERE deleted = 0 AND status = 20",
        "          AND customer_id IN",
        "          <foreach collection='customerIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "        GROUP BY customer_id",
        "  ) so ON so.customer_id = c.id",
        "  LEFT JOIN (",
        "       SELECT customer_id, SUM(receivable_amount) AS otherReceivableAmount",
        "         FROM erp_receivable_other",
        "        WHERE deleted = 0 AND status = 20",
        "          AND customer_id IN",
        "          <foreach collection='customerIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "        GROUP BY customer_id",
        "  ) ro ON ro.customer_id = c.id",
        " WHERE c.deleted = 0",
        "   AND c.id IN",
        "   <foreach collection='customerIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    java.util.List<cn.iocoder.yudao.module.erp.service.sale.bo.ErpCustomerSaleStatsBO> selectSaleStatsByCustomerIds(
            @org.apache.ibatis.annotations.Param("customerIds") java.util.Collection<Long> customerIds);

}
