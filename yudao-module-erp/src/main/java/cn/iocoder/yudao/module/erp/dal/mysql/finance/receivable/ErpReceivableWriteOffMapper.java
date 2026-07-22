package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableWriteOffDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

@Mapper
public interface ErpReceivableWriteOffMapper extends BaseMapperX<ErpReceivableWriteOffDO> {

    default List<ErpReceivableWriteOffDO> selectListByCustomerId(Long customerId, LocalDateTime startTime,
            LocalDateTime endTime) {
        return selectListByCustomerId(customerId, startTime, endTime, null, null, true);
    }

    default List<ErpReceivableWriteOffDO> selectListByCustomerId(Long customerId, LocalDateTime startTime,
            LocalDateTime endTime, Collection<Long> deptIds, Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpReceivableWriteOffDO> query = new LambdaQueryWrapperX<ErpReceivableWriteOffDO>()
                .eq(ErpReceivableWriteOffDO::getCustomerId, customerId)
                .geIfPresent(ErpReceivableWriteOffDO::getWriteOffTime, startTime)
                .ltIfPresent(ErpReceivableWriteOffDO::getWriteOffTime, endTime);
        if (!all) {
            if (deptIds != null && !deptIds.isEmpty() && selfUserId != null) {
                query.and(wrapper -> wrapper.in(ErpReceivableWriteOffDO::getDeptId, deptIds)
                        .or().eq(ErpReceivableWriteOffDO::getOperatorUserId, selfUserId));
            } else if (deptIds != null && !deptIds.isEmpty()) {
                query.in(ErpReceivableWriteOffDO::getDeptId, deptIds);
            } else {
                query.eq(ErpReceivableWriteOffDO::getOperatorUserId, selfUserId);
            }
        }
        return selectList(query);
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpReceivableWriteOffDO::getCustomerId, customerId);
    }

}
