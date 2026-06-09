package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableWriteOffDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ErpReceivableWriteOffMapper extends BaseMapperX<ErpReceivableWriteOffDO> {

    default List<ErpReceivableWriteOffDO> selectListByCustomerId(Long customerId, LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<ErpReceivableWriteOffDO>()
                .eq(ErpReceivableWriteOffDO::getCustomerId, customerId)
                .geIfPresent(ErpReceivableWriteOffDO::getWriteOffTime, startTime)
                .ltIfPresent(ErpReceivableWriteOffDO::getWriteOffTime, endTime));
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpReceivableWriteOffDO::getCustomerId, customerId);
    }

}
