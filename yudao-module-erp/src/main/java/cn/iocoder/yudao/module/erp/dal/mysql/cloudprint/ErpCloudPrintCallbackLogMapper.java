package cn.iocoder.yudao.module.erp.dal.mysql.cloudprint;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintCallbackLogDO;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintCallbackConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ErpCloudPrintCallbackLogMapper extends BaseMapperX<ErpCloudPrintCallbackLogDO> {

    default List<ErpCloudPrintCallbackLogDO> selectDueCallbacks(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapper<ErpCloudPrintCallbackLogDO>()
                .in(ErpCloudPrintCallbackLogDO::getProcessStatus,
                        ErpCloudPrintCallbackConstants.PROCESS_STATUS_PENDING,
                        ErpCloudPrintCallbackConstants.PROCESS_STATUS_RETRY)
                .and(wrapper -> wrapper.isNull(ErpCloudPrintCallbackLogDO::getNextRetryTime)
                        .or().le(ErpCloudPrintCallbackLogDO::getNextRetryTime, now))
                .orderByAsc(ErpCloudPrintCallbackLogDO::getId)
                .last("LIMIT " + limit));
    }

    default int claim(Long id, String processToken, LocalDateTime now) {
        return update(null, new LambdaUpdateWrapper<ErpCloudPrintCallbackLogDO>()
                .set(ErpCloudPrintCallbackLogDO::getProcessStatus,
                        ErpCloudPrintCallbackConstants.PROCESS_STATUS_PROCESSING)
                .set(ErpCloudPrintCallbackLogDO::getProcessStartedTime, now)
                .set(ErpCloudPrintCallbackLogDO::getProcessToken, processToken)
                .set(ErpCloudPrintCallbackLogDO::getNextRetryTime, null)
                .eq(ErpCloudPrintCallbackLogDO::getId, id)
                .in(ErpCloudPrintCallbackLogDO::getProcessStatus,
                        ErpCloudPrintCallbackConstants.PROCESS_STATUS_PENDING,
                        ErpCloudPrintCallbackConstants.PROCESS_STATUS_RETRY)
                .and(wrapper -> wrapper.isNull(ErpCloudPrintCallbackLogDO::getNextRetryTime)
                        .or().le(ErpCloudPrintCallbackLogDO::getNextRetryTime, now)));
    }

    default int recoverStuckProcessing(LocalDateTime deadline, LocalDateTime nextRetryTime) {
        return update(null, new LambdaUpdateWrapper<ErpCloudPrintCallbackLogDO>()
                .set(ErpCloudPrintCallbackLogDO::getProcessStatus,
                        ErpCloudPrintCallbackConstants.PROCESS_STATUS_RETRY)
                .set(ErpCloudPrintCallbackLogDO::getNextRetryTime, nextRetryTime)
                .set(ErpCloudPrintCallbackLogDO::getProcessToken, null)
                .set(ErpCloudPrintCallbackLogDO::getProcessMessage, "处理租约超时，等待重新处理")
                .eq(ErpCloudPrintCallbackLogDO::getProcessStatus,
                        ErpCloudPrintCallbackConstants.PROCESS_STATUS_PROCESSING)
                .lt(ErpCloudPrintCallbackLogDO::getProcessStartedTime, deadline));
    }

    default int updateParsed(Long id, String processToken, String method, String devid,
                             String reqid, Integer code) {
        return update(null, new LambdaUpdateWrapper<ErpCloudPrintCallbackLogDO>()
                .set(ErpCloudPrintCallbackLogDO::getMethod, method)
                .set(ErpCloudPrintCallbackLogDO::getDevid, devid)
                .set(ErpCloudPrintCallbackLogDO::getReqid, reqid)
                .set(ErpCloudPrintCallbackLogDO::getCode, code)
                .eq(ErpCloudPrintCallbackLogDO::getId, id)
                .eq(ErpCloudPrintCallbackLogDO::getProcessStatus,
                        ErpCloudPrintCallbackConstants.PROCESS_STATUS_PROCESSING)
                .eq(ErpCloudPrintCallbackLogDO::getProcessToken, processToken));
    }

    default int finish(Long id, String processToken, int status, boolean matched,
                       String processMessage, LocalDateTime processedTime) {
        return update(null, new LambdaUpdateWrapper<ErpCloudPrintCallbackLogDO>()
                .set(ErpCloudPrintCallbackLogDO::getProcessStatus, status)
                .set(ErpCloudPrintCallbackLogDO::getMatched, matched)
                .set(ErpCloudPrintCallbackLogDO::getProcessedTime, processedTime)
                .set(ErpCloudPrintCallbackLogDO::getProcessToken, null)
                .set(ErpCloudPrintCallbackLogDO::getProcessMessage, processMessage)
                .set(ErpCloudPrintCallbackLogDO::getNextRetryTime, null)
                .eq(ErpCloudPrintCallbackLogDO::getId, id)
                .eq(ErpCloudPrintCallbackLogDO::getProcessStatus,
                        ErpCloudPrintCallbackConstants.PROCESS_STATUS_PROCESSING)
                .eq(ErpCloudPrintCallbackLogDO::getProcessToken, processToken));
    }

    default int markRetry(Long id, String processToken, int retryCount,
                          LocalDateTime nextRetryTime, String processMessage) {
        return update(null, new LambdaUpdateWrapper<ErpCloudPrintCallbackLogDO>()
                .set(ErpCloudPrintCallbackLogDO::getProcessStatus,
                        ErpCloudPrintCallbackConstants.PROCESS_STATUS_RETRY)
                .set(ErpCloudPrintCallbackLogDO::getRetryCount, retryCount)
                .set(ErpCloudPrintCallbackLogDO::getNextRetryTime, nextRetryTime)
                .set(ErpCloudPrintCallbackLogDO::getProcessToken, null)
                .set(ErpCloudPrintCallbackLogDO::getProcessMessage, processMessage)
                .eq(ErpCloudPrintCallbackLogDO::getId, id)
                .eq(ErpCloudPrintCallbackLogDO::getProcessStatus,
                        ErpCloudPrintCallbackConstants.PROCESS_STATUS_PROCESSING)
                .eq(ErpCloudPrintCallbackLogDO::getProcessToken, processToken));
    }
}
