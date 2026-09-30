package cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery;

import cn.iocoder.yudao.framework.common.pojo.*;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliverySubmitItemDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpSalePickDeliverySubmitItemMapper extends BaseMapperX<ErpSalePickDeliverySubmitItemDO> {
    default PageResult<ErpSalePickDeliverySubmitItemDO> selectPageBySubmitId(PageParam page, Long submitId) {
        return selectPage(page, new LambdaQueryWrapperX<ErpSalePickDeliverySubmitItemDO>()
                .eq(ErpSalePickDeliverySubmitItemDO::getSubmitId, submitId)
                .orderByAsc(ErpSalePickDeliverySubmitItemDO::getId));
    }
}
