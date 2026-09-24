package cn.iocoder.yudao.module.erp.dal.mysql.cloudprint;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintTaskDO;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpCloudPrintTaskMapper extends BaseMapperX<ErpCloudPrintTaskDO> {

    default ErpCloudPrintTaskDO selectByReqid(String reqid) {
        return selectOne(ErpCloudPrintTaskDO::getReqid, reqid);
    }

    default ErpCloudPrintTaskDO selectRunningByBiz(String bizType, Long bizId) {
        return selectOne(new LambdaQueryWrapper<ErpCloudPrintTaskDO>()
                .eq(ErpCloudPrintTaskDO::getBizType, bizType)
                .eq(ErpCloudPrintTaskDO::getBizId, bizId)
                .in(ErpCloudPrintTaskDO::getStatus, ErpCloudPrintConstants.RUNNING_STATUSES)
                .orderByDesc(ErpCloudPrintTaskDO::getId)
                .last("LIMIT 1"));
    }

    default ErpCloudPrintTaskDO selectRunningByBizAndWarehouse(String bizType, Long bizId, Long warehouseId) {
        return selectOne(new LambdaQueryWrapper<ErpCloudPrintTaskDO>()
                .eq(ErpCloudPrintTaskDO::getBizType, bizType)
                .eq(ErpCloudPrintTaskDO::getBizId, bizId)
                .eq(ErpCloudPrintTaskDO::getWarehouseId, warehouseId)
                .in(ErpCloudPrintTaskDO::getStatus, ErpCloudPrintConstants.RUNNING_STATUSES)
                .orderByDesc(ErpCloudPrintTaskDO::getId)
                .last("LIMIT 1"));
    }

    default List<ErpCloudPrintTaskDO> selectListByBiz(String bizType, Long bizId) {
        return selectList(new LambdaQueryWrapper<ErpCloudPrintTaskDO>()
                .eq(ErpCloudPrintTaskDO::getBizType, bizType)
                .eq(ErpCloudPrintTaskDO::getBizId, bizId)
                .orderByDesc(ErpCloudPrintTaskDO::getId));
    }

    default List<ErpCloudPrintTaskDO> selectListByStatuses(Collection<Integer> statuses) {
        return selectList(new LambdaQueryWrapper<ErpCloudPrintTaskDO>()
                .in(ErpCloudPrintTaskDO::getStatus, statuses)
                .orderByAsc(ErpCloudPrintTaskDO::getSubmitTime));
    }

}
