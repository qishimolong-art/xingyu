package cn.iocoder.yudao.module.erp.dal.mysql.price;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.price.ErpPriceHistoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 价格历史 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpPriceHistoryMapper extends BaseMapperX<ErpPriceHistoryDO> {

    default List<ErpPriceHistoryDO> selectListByProductAndPartner(Long productId, Integer partnerType, Long partnerId) {
        return selectList(new LambdaQueryWrapperX<ErpPriceHistoryDO>()
                .eq(ErpPriceHistoryDO::getProductId, productId)
                .eq(ErpPriceHistoryDO::getPartnerType, partnerType)
                .eq(ErpPriceHistoryDO::getPartnerId, partnerId)
                .orderByDesc(ErpPriceHistoryDO::getPriceTime));
    }

    default ErpPriceHistoryDO selectLatest(Long productId, Integer partnerType, Long partnerId) {
        return selectOne(new LambdaQueryWrapperX<ErpPriceHistoryDO>()
                .eq(ErpPriceHistoryDO::getProductId, productId)
                .eq(ErpPriceHistoryDO::getPartnerType, partnerType)
                .eq(ErpPriceHistoryDO::getPartnerId, partnerId)
                .orderByDesc(ErpPriceHistoryDO::getPriceTime)
                .last("LIMIT 1"));
    }

}
