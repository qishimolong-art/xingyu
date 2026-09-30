package cn.iocoder.yudao.module.erp.dal.mysql.cloudprint;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintTaskDO;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpCloudPrintTaskMapper extends BaseMapperX<ErpCloudPrintTaskDO> {

    default List<ErpCloudPrintTaskDO> selectListByReqid(String reqid) {
        return selectList(ErpCloudPrintTaskDO::getReqid, reqid);
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

    default ErpCloudPrintTaskDO selectInFlightByDevice(Long deviceId) {
        return selectOne(new LambdaQueryWrapper<ErpCloudPrintTaskDO>()
                .eq(ErpCloudPrintTaskDO::getDeviceId, deviceId)
                .in(ErpCloudPrintTaskDO::getStatus, ErpCloudPrintConstants.IN_FLIGHT_STATUSES)
                .orderByAsc(ErpCloudPrintTaskDO::getId)
                .last("LIMIT 1"));
    }

    default ErpCloudPrintTaskDO selectFirstPendingByDevice(Long deviceId) {
        return selectOne(new LambdaQueryWrapper<ErpCloudPrintTaskDO>()
                .eq(ErpCloudPrintTaskDO::getDeviceId, deviceId)
                .eq(ErpCloudPrintTaskDO::getStatus, ErpCloudPrintConstants.STATUS_PENDING)
                .last("ORDER BY (retry_of IS NOT NULL) DESC, id ASC LIMIT 1"));
    }

    default List<ErpCloudPrintTaskDO> selectPendingTasks() {
        return selectList(new LambdaQueryWrapper<ErpCloudPrintTaskDO>()
                .eq(ErpCloudPrintTaskDO::getStatus, ErpCloudPrintConstants.STATUS_PENDING)
                .orderByAsc(ErpCloudPrintTaskDO::getId));
    }

    default Long selectPendingCountByDevice(Long deviceId) {
        return selectCount(new LambdaQueryWrapper<ErpCloudPrintTaskDO>()
                .eq(ErpCloudPrintTaskDO::getDeviceId, deviceId)
                .eq(ErpCloudPrintTaskDO::getStatus, ErpCloudPrintConstants.STATUS_PENDING));
    }

    default Long selectQueuePosition(Long deviceId, Long taskId) {
        return selectCount(new LambdaQueryWrapper<ErpCloudPrintTaskDO>()
                .eq(ErpCloudPrintTaskDO::getDeviceId, deviceId)
                .eq(ErpCloudPrintTaskDO::getStatus, ErpCloudPrintConstants.STATUS_PENDING)
                .le(ErpCloudPrintTaskDO::getId, taskId));
    }

    default ErpCloudPrintTaskDO selectLatestCallbackByDevice(Long deviceId) {
        return selectOne(new LambdaQueryWrapper<ErpCloudPrintTaskDO>()
                .eq(ErpCloudPrintTaskDO::getDeviceId, deviceId)
                .isNotNull(ErpCloudPrintTaskDO::getCallbackTime)
                .orderByDesc(ErpCloudPrintTaskDO::getCallbackTime)
                .last("LIMIT 1"));
    }

    default int markTimeoutIfInFlight(Long taskId, String errorMsg) {
        return update(null, new LambdaUpdateWrapper<ErpCloudPrintTaskDO>()
                .set(ErpCloudPrintTaskDO::getStatus, ErpCloudPrintConstants.STATUS_TIMEOUT)
                .set(ErpCloudPrintTaskDO::getErrorMsg, errorMsg)
                .eq(ErpCloudPrintTaskDO::getId, taskId)
                .in(ErpCloudPrintTaskDO::getStatus, ErpCloudPrintConstants.IN_FLIGHT_STATUSES));
    }

    default int updatePrintCallbackIfProcessable(Long taskId, Integer status, Integer callbackCode,
                                                  String callbackMsg, java.time.LocalDateTime callbackTime) {
        return update(null, new LambdaUpdateWrapper<ErpCloudPrintTaskDO>()
                .set(ErpCloudPrintTaskDO::getStatus, status)
                .set(ErpCloudPrintTaskDO::getCallbackCode, callbackCode)
                .set(ErpCloudPrintTaskDO::getCallbackMsg, callbackMsg)
                .set(ErpCloudPrintTaskDO::getCallbackTime, callbackTime)
                .set(ErpCloudPrintTaskDO::getErrorMsg,
                        Integer.valueOf(ErpCloudPrintConstants.STATUS_SUCCESS).equals(status) ? null : callbackMsg)
                .eq(ErpCloudPrintTaskDO::getId, taskId)
                .in(ErpCloudPrintTaskDO::getStatus, ErpCloudPrintConstants.STATUS_PENDING,
                        ErpCloudPrintConstants.STATUS_SUBMITTED, ErpCloudPrintConstants.STATUS_UNKNOWN,
                        ErpCloudPrintConstants.STATUS_TIMEOUT));
    }

}
