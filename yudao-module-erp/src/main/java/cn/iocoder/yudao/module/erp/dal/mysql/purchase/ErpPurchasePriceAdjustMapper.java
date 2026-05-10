package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 采购调价单 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpPurchasePriceAdjustMapper extends BaseMapperX<ErpPurchasePriceAdjustDO> {

    default PageResult<ErpPurchasePriceAdjustDO> selectPage(ErpPurchasePriceAdjustPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpPurchasePriceAdjustDO>()
                .likeIfPresent(ErpPurchasePriceAdjustDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpPurchasePriceAdjustDO::getAdjustDate, reqVO.getAdjustDate())
                .orderByDesc(ErpPurchasePriceAdjustDO::getId));
    }

}
