package cn.iocoder.yudao.module.erp.dal.mysql.common;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintRecordDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpPrintRecordMapper extends BaseMapperX<ErpPrintRecordDO> {

    default Long selectCountByBusiness(String moduleKey, Long businessId) {
        return selectCount(new LambdaQueryWrapper<ErpPrintRecordDO>()
                .eq(ErpPrintRecordDO::getModuleKey, moduleKey)
                .eq(ErpPrintRecordDO::getBusinessId, businessId));
    }

    default List<ErpPrintRecordDO> selectListByBusinessIds(String moduleKey, Collection<Long> businessIds) {
        return selectList(new LambdaQueryWrapper<ErpPrintRecordDO>()
                .eq(ErpPrintRecordDO::getModuleKey, moduleKey)
                .in(ErpPrintRecordDO::getBusinessId, businessIds)
                .orderByDesc(ErpPrintRecordDO::getPrintTime));
    }

    default LocalDateTime selectLastPrintTime(String moduleKey, Long businessId) {
        ErpPrintRecordDO record = selectOne(new LambdaQueryWrapper<ErpPrintRecordDO>()
                .eq(ErpPrintRecordDO::getModuleKey, moduleKey)
                .eq(ErpPrintRecordDO::getBusinessId, businessId)
                .orderByDesc(ErpPrintRecordDO::getPrintTime)
                .last("LIMIT 1"));
        return record == null ? null : record.getPrintTime();
    }

}
