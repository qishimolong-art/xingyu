package cn.iocoder.yudao.module.erp.dal.mysql.finance.payable;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableWriteOffDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ErpPayableWriteOffMapper extends BaseMapperX<ErpPayableWriteOffDO> {

    default List<ErpPayableWriteOffDO> selectListBySupplierId(Long supplierId, LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<ErpPayableWriteOffDO>()
                .eq(ErpPayableWriteOffDO::getSupplierId, supplierId)
                .geIfPresent(ErpPayableWriteOffDO::getWriteOffTime, startTime)
                .ltIfPresent(ErpPayableWriteOffDO::getWriteOffTime, endTime));
    }

    default Long selectCountBySupplierId(Long supplierId) {
        return selectCount(ErpPayableWriteOffDO::getSupplierId, supplierId);
    }

}
