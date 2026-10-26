package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class ErpAsyncImportTaskSupport {

    public static final Duration STALE_TIMEOUT = Duration.ofMinutes(30);

    @Resource
    private ErpImportExportRecordMapper recordMapper;

    public ErpImportExportRecordDO findTenantActiveRecord(String moduleKey) {
        return recordMapper.selectLatestProcessing(ErpImportExportOperationTypeEnum.IMPORT.getType(), moduleKey,
                TenantContextHolder.getRequiredTenantId());
    }

    public ErpImportExportRecordDO findOwnedActiveRecord(String moduleKey) {
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorId == null) {
            return null;
        }
        return recordMapper.selectLatestProcessingByOperator(
                ErpImportExportOperationTypeEnum.IMPORT.getType(), moduleKey,
                TenantContextHolder.getRequiredTenantId(), operatorId);
    }

    public ErpImportExportRecordDO findOwnedRecord(Long taskId, String moduleKey) {
        ErpImportExportRecordDO record = recordMapper.selectByIdAndTenant(
                taskId, TenantContextHolder.getRequiredTenantId());
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (record == null || operatorId == null || !operatorId.equals(record.getOperatorId())
                || !moduleKey.equals(record.getModuleKey())
                || !ErpImportExportOperationTypeEnum.IMPORT.getType().equals(record.getOperationType())) {
            return null;
        }
        return record;
    }

    public boolean isStale(ErpImportExportRecordDO record) {
        if (record == null || !ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())) {
            return false;
        }
        LocalDateTime heartbeat = record.getHeartbeatTime() == null ? record.getStartTime() : record.getHeartbeatTime();
        return heartbeat != null && heartbeat.isBefore(LocalDateTime.now().minus(STALE_TIMEOUT));
    }

    public long resolveDuration(ErpImportExportRecordDO record) {
        if (record.getDurationMs() != null) {
            return record.getDurationMs();
        }
        return record.getStartTime() == null ? 0L
                : Math.max(0L, Duration.between(record.getStartTime(), LocalDateTime.now()).toMillis());
    }

    public int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

}
