package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpAsyncExportTaskStageEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;

/**
 * ERP 异步导出任务的通用归属、进度和临时文件支持。
 * 业务模块仍负责查询、Excel 行转换以及模块错误码。
 */
@Component
public class ErpAsyncExportTaskSupport {

    public static final Duration FILE_RETENTION = Duration.ofHours(24);
    public static final Duration STALE_TIMEOUT = Duration.ofMinutes(30);

    @Resource
    private ErpImportExportRecordMapper recordMapper;

    public ErpImportExportRecordDO findActiveRecord(String moduleKey) {
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorId == null) {
            return null;
        }
        return recordMapper.selectLatestProcessingByOperator(
                ErpImportExportOperationTypeEnum.EXPORT.getType(), moduleKey,
                TenantContextHolder.getRequiredTenantId(), operatorId);
    }

    public ErpImportExportRecordDO findOwnedRecord(Long taskId, String moduleKey) {
        ErpImportExportRecordDO record = recordMapper.selectByIdAndTenant(
                taskId, TenantContextHolder.getRequiredTenantId());
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (record == null || operatorId == null || !operatorId.equals(record.getOperatorId())
                || !moduleKey.equals(record.getModuleKey())
                || !ErpImportExportOperationTypeEnum.EXPORT.getType().equals(record.getOperationType())) {
            return null;
        }
        return record;
    }

    public PageResult<ErpImportExportRecordDO> getOwnedHistory(String moduleKey, int pageNo, int pageSize) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(Math.max(1, pageNo));
        pageParam.setPageSize(Math.max(1, Math.min(50, pageSize)));
        return recordMapper.selectOwnedExportPage(pageParam, moduleKey,
                TenantContextHolder.getRequiredTenantId(), SecurityFrameworkUtils.getLoginUserId());
    }

    public boolean isStale(ErpImportExportRecordDO record) {
        if (record == null || !ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())) {
            return false;
        }
        LocalDateTime heartbeat = record.getHeartbeatTime() == null ? record.getStartTime() : record.getHeartbeatTime();
        return heartbeat != null && heartbeat.isBefore(LocalDateTime.now().minus(STALE_TIMEOUT));
    }

    public int calculateProgress(ErpImportExportRecordDO record) {
        if (!ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())
                || ErpAsyncExportTaskStageEnum.FINISHED.getStage().equals(record.getStage())) {
            return 100;
        }
        if (ErpAsyncExportTaskStageEnum.COUNTING.getStage().equals(record.getStage())) {
            return 5;
        }
        if (ErpAsyncExportTaskStageEnum.EXPORTING.getStage().equals(record.getStage())) {
            int total = valueOrZero(record.getTotalCount());
            int processed = valueOrZero(record.getProcessedCount());
            return total <= 0 ? 5 : Math.min(90, 5 + (int) Math.floor(processed * 85.0D / total));
        }
        if (ErpAsyncExportTaskStageEnum.FINALIZING.getStage().equals(record.getStage())) {
            return 95;
        }
        return 0;
    }

    public long resolveDuration(ErpImportExportRecordDO record) {
        if (record.getDurationMs() != null) {
            return record.getDurationMs();
        }
        return record.getStartTime() == null ? 0L
                : Math.max(0L, Duration.between(record.getStartTime(), LocalDateTime.now()).toMillis());
    }

    public boolean isExpired(ErpImportExportRecordDO record) {
        return record.getEndTime() != null && record.getEndTime().plus(FILE_RETENTION).isBefore(LocalDateTime.now());
    }

    public LocalDateTime expiresAt(ErpImportExportRecordDO record) {
        return record.getEndTime() == null ? null : record.getEndTime().plus(FILE_RETENTION);
    }

    public Path taskDirectory(Path root, Long tenantId, Long operatorId) {
        return root.resolve(String.valueOf(tenantId)).resolve(String.valueOf(operatorId));
    }

    public void moveCompletedFile(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void cleanExpiredFiles(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minus(FILE_RETENTION);
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try {
                    LocalDateTime modified = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault());
                    if (modified.isBefore(cutoff)) {
                        Files.deleteIfExists(path);
                    }
                } catch (IOException ignored) {
                    // 单个文件清理失败不影响其它任务文件。
                }
            });
        }
    }

    public void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 失败文件由下次定时清理兜底。
        }
    }

    public int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

}
