package cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliverySubmitFileDO;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpSalePickDeliverySubmitFileMapper extends BaseMapperX<ErpSalePickDeliverySubmitFileDO> {

    default List<ErpSalePickDeliverySubmitFileDO> selectListBySubmitIds(Collection<Long> submitIds) {
        if (submitIds == null || submitIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpSalePickDeliverySubmitFileDO>()
                .in(ErpSalePickDeliverySubmitFileDO::getSubmitId, submitIds)
                .orderByAsc(ErpSalePickDeliverySubmitFileDO::getSort)
                .orderByAsc(ErpSalePickDeliverySubmitFileDO::getId));
    }

    default List<Long> selectSubmitIdsWithFiles(Collection<Long> submitIds) {
        if (submitIds == null || submitIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new QueryWrapper<ErpSalePickDeliverySubmitFileDO>()
                .select("submit_id")
                .in("submit_id", submitIds)
                .groupBy("submit_id")).stream()
                .map(ErpSalePickDeliverySubmitFileDO::getSubmitId)
                .collect(java.util.stream.Collectors.toList());
    }

}
