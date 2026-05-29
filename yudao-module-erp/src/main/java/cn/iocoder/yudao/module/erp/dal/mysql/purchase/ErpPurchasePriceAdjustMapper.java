package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Objects;

/**
 * ERP 采购调价单 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpPurchasePriceAdjustMapper extends BaseMapperX<ErpPurchasePriceAdjustDO> {

    default PageResult<ErpPurchasePriceAdjustDO> selectPage(ErpPurchasePriceAdjustPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPurchasePriceAdjustDO> query = new LambdaQueryWrapperX<ErpPurchasePriceAdjustDO>()
                .likeIfPresent(ErpPurchasePriceAdjustDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getAdjustType, reqVO.getAdjustType())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getSupplierId, reqVO.getSupplierId())
                .betweenIfPresent(ErpPurchasePriceAdjustDO::getAdjustTime, reqVO.getAdjustTime())
                .likeIfPresent(ErpPurchasePriceAdjustDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getAdjuster, reqVO.getAdjuster())
                .orderByDesc(ErpPurchasePriceAdjustDO::getId);
        if (Objects.equals(reqVO.getPaymentStatus(), ErpPurchasePriceAdjustPageReqVO.PAYMENT_STATUS_NONE)) {
            query.apply(paymentPriceSql() + " = 0");
        } else if (Objects.equals(reqVO.getPaymentStatus(), ErpPurchasePriceAdjustPageReqVO.PAYMENT_STATUS_PART)) {
            query.apply(paymentPriceSql() + " <> 0")
                    .apply("ABS(" + paymentPriceSql() + ") < ABS(total_adjust_price)");
        } else if (Objects.equals(reqVO.getPaymentStatus(), ErpPurchasePriceAdjustPageReqVO.PAYMENT_STATUS_ALL)) {
            query.apply("ABS(" + paymentPriceSql() + ") = ABS(total_adjust_price)");
        }
        if (Boolean.TRUE.equals(reqVO.getPaymentEnable())) {
            query.eq(ErpPurchasePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply("ABS(" + paymentPriceSql() + ") < ABS(total_adjust_price)");
        }
        return selectPage(reqVO, query);
    }

    static String paymentPriceSql() {
        return "(SELECT COALESCE(SUM(item.payment_price), 0) FROM erp_finance_payment_item item "
                + "WHERE item.deleted = 0 "
                + "AND item.biz_type = " + ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType() + " "
                + "AND item.biz_id = erp_purchase_price_adjust.id)";
    }

    default ErpPurchasePriceAdjustDO selectByNo(String no) {
        return selectOne(ErpPurchasePriceAdjustDO::getNo, no);
    }

    /**
     * 基于 id + 原状态的乐观锁更新，避免并发审批 / 反审批
     *
     * @param id        调价单 id
     * @param status    期望的原状态（作为乐观锁 where 条件）
     * @param updateObj 更新内容
     * @return 实际影响行数
     */
    default int updateByIdAndStatus(Long id, Integer status, ErpPurchasePriceAdjustDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPurchasePriceAdjustDO>()
                .eq(ErpPurchasePriceAdjustDO::getId, id)
                .eq(ErpPurchasePriceAdjustDO::getStatus, status));
    }

}
