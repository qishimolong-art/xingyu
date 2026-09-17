package cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliverySubmitFileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpSalePickDeliverySubmitFileMapper extends BaseMapperX<ErpSalePickDeliverySubmitFileDO> {

    default List<ErpSalePickDeliverySubmitFileDO> selectListBySubmitIds(Collection<Long> submitIds) {
        return selectList(ErpSalePickDeliverySubmitFileDO::getSubmitId, submitIds);
    }

}
