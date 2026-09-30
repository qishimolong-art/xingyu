package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintCallbackLogDO;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintCallbackLogMapper;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintCallbackConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ErpCloudPrintCallbackStorageService {

    @Resource
    private ErpCloudPrintCallbackLogMapper callbackLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long saveIncoming(String rawBody) {
        ErpCloudPrintCallbackLogDO callback = new ErpCloudPrintCallbackLogDO()
                .setRawBody(rawBody)
                .setMatched(false)
                .setProcessStatus(ErpCloudPrintCallbackConstants.PROCESS_STATUS_PENDING)
                .setRetryCount(0);
        callbackLogMapper.insert(callback);
        return callback.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean claim(Long id, String processToken, LocalDateTime now) {
        return callbackLogMapper.claim(id, processToken, now) > 0;
    }

    public ErpCloudPrintCallbackLogDO get(Long id) {
        return callbackLogMapper.selectById(id);
    }

    public List<ErpCloudPrintCallbackLogDO> getDueCallbacks(LocalDateTime now, int limit) {
        return callbackLogMapper.selectDueCallbacks(now, limit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int recoverStuckProcessing(LocalDateTime deadline, LocalDateTime nextRetryTime) {
        return callbackLogMapper.recoverStuckProcessing(deadline, nextRetryTime);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateParsed(Long id, String processToken, ErpCloudPrintCallbackRequest request) {
        return callbackLogMapper.updateParsed(id, processToken, request.getMethod(), request.getDevid(),
                request.getReqid(), request.getCode()) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean finish(Long id, String processToken, int status, boolean matched,
                          String processMessage, LocalDateTime processedTime) {
        return callbackLogMapper.finish(id, processToken, status, matched,
                processMessage, processedTime) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markRetry(Long id, String processToken, int retryCount,
                             LocalDateTime nextRetryTime, String processMessage) {
        return callbackLogMapper.markRetry(id, processToken, retryCount,
                nextRetryTime, processMessage) > 0;
    }

}
