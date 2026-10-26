package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordFailureDetailExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpCustomerImportTaskStageEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpAsyncImportTaskSupport;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportFailureDetailBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordFinishReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordProgressReqBO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_IMPORT_FILE_TYPE_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_IMPORT_TASK_BUSY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_IMPORT_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_IMPORT_TASK_SUBMIT_FAILED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ERP_IMPORT_ROW_LIMIT_EXCEEDED;

@Service
@Slf4j
public class ErpCustomerImportTaskServiceImpl implements ErpCustomerImportTaskService {

    private static final String MODULE_KEY = "erp_customer";
    private static final String MODULE_NAME = "客户档案";
    private static final String LOCK_KEY_PREFIX = "erp:customer-import-task:";
    private static final Duration ORPHAN_FILE_TIMEOUT = Duration.ofHours(24);
    private static final int PROGRESS_BATCH_SIZE = 20;
    private static final long PROGRESS_INTERVAL_MILLIS = 500L;
    private static final Path TEMP_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir"),
            "xingyu", "customer-import");

    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private ErpImportExportRecordMapper recordMapper;
    @Resource
    private ErpAsyncImportTaskSupport taskSupport;
    @Resource
    private RedissonClient redissonClient;
    @Resource(name = "erpAsyncImportTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    @Value("${yudao.erp.import.max-rows:3000}")
    private int importMaxRows;

    @PostConstruct
    public void cleanOrphanFilesOnStartup() {
        try {
            Files.createDirectories(TEMP_DIRECTORY);
            LocalDateTime cutoff = LocalDateTime.now().minus(ORPHAN_FILE_TIMEOUT);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(TEMP_DIRECTORY)) {
                for (Path path : stream) {
                    LocalDateTime modified = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(path).toInstant(), java.time.ZoneId.systemDefault());
                    if (modified.isBefore(cutoff)) {
                        Files.deleteIfExists(path);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[cleanOrphanFilesOnStartup][清理客户导入临时文件失败]", ex);
        }
    }

    @Override
    public ErpCustomerImportTaskStartRespVO startTask(MultipartFile file) throws IOException {
        validateFile(file);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorId == null) {
            throw new AccessDeniedException("未登录，不能创建客户导入任务");
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + tenantId);
        lock.lock();
        try {
            ErpImportExportRecordDO active = refreshStaleRecord(taskSupport.findTenantActiveRecord(MODULE_KEY));
            if (active != null) {
                if (operatorId.equals(active.getOperatorId())) {
                    return new ErpCustomerImportTaskStartRespVO(active.getId(), true);
                }
                throw exception(CUSTOMER_IMPORT_TASK_BUSY,
                        active.getOperatorName() == null ? "其他用户" : active.getOperatorName());
            }
            Path storedFile = storeFile(file);
            Long taskId;
            try {
                taskId = createTaskRecord(file.getOriginalFilename());
            } catch (RuntimeException ex) {
                deleteQuietly(storedFile);
                throw ex;
            }
            try {
                taskExecutor.execute(() -> executeTask(taskId, storedFile, file.getOriginalFilename()));
            } catch (RuntimeException ex) {
                deleteQuietly(storedFile);
                finishFailure(taskId, "后台任务队列繁忙，请稍后重试", System.currentTimeMillis());
                throw exception(CUSTOMER_IMPORT_TASK_SUBMIT_FAILED);
            }
            return new ErpCustomerImportTaskStartRespVO(taskId, false);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public ErpCustomerImportTaskRespVO getActiveTask() {
        ErpImportExportRecordDO record = refreshStaleRecord(taskSupport.findOwnedActiveRecord(MODULE_KEY));
        return record == null ? null : buildTaskResponse(record);
    }

    @Override
    public ErpCustomerImportTaskRespVO getTask(Long taskId) {
        ErpImportExportRecordDO record = taskSupport.findOwnedRecord(taskId, MODULE_KEY);
        if (record == null) {
            throw exception(CUSTOMER_IMPORT_TASK_NOT_EXISTS);
        }
        return buildTaskResponse(refreshStaleRecord(record));
    }

    private ErpImportExportRecordDO refreshStaleRecord(ErpImportExportRecordDO record) {
        if (record == null || !taskSupport.isStale(record)) {
            return record;
        }
        finishFailure(record.getId(), "任务超过 30 分钟未更新进度，已自动结束，请重新导入",
                record.getStartTime() == null ? System.currentTimeMillis()
                        : System.currentTimeMillis() - Duration.between(
                                record.getStartTime(), LocalDateTime.now()).toMillis());
        return recordMapper.selectById(record.getId());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(CUSTOMER_IMPORT_FILE_TYPE_INVALID);
        }
        String fileName = file.getOriginalFilename() == null ? ""
                : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
            throw exception(CUSTOMER_IMPORT_FILE_TYPE_INVALID);
        }
    }

    private Path storeFile(MultipartFile file) throws IOException {
        Files.createDirectories(TEMP_DIRECTORY);
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dotIndex = fileName.lastIndexOf('.');
        String suffix = dotIndex < 0 ? ".tmp" : fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
        Path storedFile = Files.createTempFile(TEMP_DIRECTORY, "customer-import-", suffix);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, storedFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            deleteQuietly(storedFile);
            throw ex;
        }
        return storedFile;
    }

    private Long createTaskRecord(String originalFileName) {
        ErpImportExportRecordCreateReqBO create = new ErpImportExportRecordCreateReqBO();
        create.setOperationType(ErpImportExportOperationTypeEnum.IMPORT.getType());
        create.setModuleKey(MODULE_KEY);
        create.setModuleName(MODULE_NAME);
        create.setTemplateKey(ErpCustomerImportExcelVO.class.getName());
        create.setFileName(originalFileName);
        create.setFileType(fileType(originalFileName));
        create.setStage(ErpCustomerImportTaskStageEnum.QUEUED.getStage());
        create.setProcessedCount(0);
        create.setStartTime(LocalDateTime.now());
        create.setHeartbeatTime(LocalDateTime.now());
        return importExportRecordService.createRecord(create);
    }

    private void executeTask(Long taskId, Path storedFile, String originalFileName) {
        long startMillis = System.currentTimeMillis();
        try {
            updateProgress(taskId, ErpCustomerImportTaskStageEnum.PARSING.getStage(), 0, 0,
                    new ErpCustomerImportRespVO(), startMillis);
            StoredMultipartFile multipartFile = new StoredMultipartFile(storedFile, originalFileName);
            int rowCount = ExcelUtils.countFirstSheetDataRows(multipartFile, 1);
            if (rowCount > importMaxRows) {
                throw exception(ERP_IMPORT_ROW_LIMIT_EXCEEDED, rowCount, importMaxRows);
            }
            List<ErpCustomerImportExcelVO> rows = ExcelUtils.read(multipartFile, ErpCustomerImportExcelVO.class);
            ProgressListener listener = new ProgressListener(taskId, startMillis);
            ErpCustomerImportRespVO result = customerService.importCustomerList(rows, listener);
            finishSuccess(taskId, rows, result, listener.getTotalCount(), startMillis);
        } catch (Throwable ex) {
            log.error("[executeTask][taskId({}) 客户导入失败]", taskId, ex);
            finishFailure(taskId, briefErrorMessage(ex), startMillis);
        } finally {
            ExcelUtils.clearLastOperation();
            deleteQuietly(storedFile);
        }
    }

    private void finishSuccess(Long taskId, List<ErpCustomerImportExcelVO> rows,
                               ErpCustomerImportRespVO result, int totalCount, long startMillis) {
        ErpImportExportRecordFinishReqBO finish = new ErpImportExportRecordFinishReqBO();
        finish.setTemplateKey(ErpCustomerImportExcelVO.class.getName());
        finish.setStatus(ErpImportExportRecordStatusEnum.ofCount(result.getSuccessCount(), result.getFailureCount()));
        finish.setStage(ErpCustomerImportTaskStageEnum.FINISHED.getStage());
        finish.setTotalCount(totalCount);
        finish.setProcessedCount(totalCount);
        finish.setSuccessCount(result.getSuccessCount());
        finish.setFailureCount(result.getFailureCount());
        finish.setCreateCount(result.getCreateCount());
        finish.setUpdateCount(result.getUpdateCount());
        finish.setDurationMs(System.currentTimeMillis() - startMillis);
        finish.setEndTime(LocalDateTime.now());
        finish.setHeartbeatTime(LocalDateTime.now());
        finish.setFailureDetails(buildFailureDetails(rows, result.getFailureDetails()));
        importExportRecordService.finishRecord(taskId, finish);
    }

    private List<ErpImportExportFailureDetailBO> buildFailureDetails(
            List<ErpCustomerImportExcelVO> rows, List<ErpCustomerImportRespVO.FailureItem> failures) {
        if (failures == null || failures.isEmpty()) {
            return Collections.emptyList();
        }
        List<ErpImportExportFailureDetailBO> details = new ArrayList<>(failures.size());
        for (ErpCustomerImportRespVO.FailureItem failure : failures) {
            ErpImportExportFailureDetailBO detail = new ErpImportExportFailureDetailBO();
            detail.setRowNo(failure.getRowNo());
            detail.setGroupKey(failure.getCode() == null ? "ROW-" + failure.getRowNo() : failure.getCode());
            detail.setDetailType("FAILURE");
            detail.setBizKey(failure.getCode());
            detail.setFailureReason(failure.getReason());
            int rowIndex = failure.getRowNo() == null ? -1 : failure.getRowNo() - 2;
            if (rowIndex >= 0 && rowIndex < rows.size()) {
                detail.setRawData(rows.get(rowIndex));
            }
            details.add(detail);
        }
        return details;
    }

    private void finishFailure(Long taskId, String errorMessage, long startMillis) {
        ErpImportExportRecordFinishReqBO finish = new ErpImportExportRecordFinishReqBO();
        finish.setStatus(ErpImportExportRecordStatusEnum.FAILURE.getStatus());
        finish.setStage(ErpCustomerImportTaskStageEnum.FINISHED.getStage());
        finish.setTotalCount(0);
        finish.setProcessedCount(0);
        finish.setSuccessCount(0);
        finish.setFailureCount(1);
        finish.setCreateCount(0);
        finish.setUpdateCount(0);
        finish.setErrorMessage(errorMessage);
        finish.setDurationMs(Math.max(0L, System.currentTimeMillis() - startMillis));
        finish.setEndTime(LocalDateTime.now());
        finish.setHeartbeatTime(LocalDateTime.now());
        importExportRecordService.finishRecord(taskId, finish);
    }

    private void updateProgress(Long taskId, String stage, int processedCount, int totalCount,
                                ErpCustomerImportRespVO result, long startMillis) {
        ErpImportExportRecordProgressReqBO progress = new ErpImportExportRecordProgressReqBO();
        progress.setStage(stage);
        progress.setTotalCount(totalCount);
        progress.setProcessedCount(processedCount);
        progress.setSuccessCount(result.getSuccessCount());
        progress.setFailureCount(result.getFailureCount());
        progress.setCreateCount(result.getCreateCount());
        progress.setUpdateCount(result.getUpdateCount());
        progress.setDurationMs(System.currentTimeMillis() - startMillis);
        progress.setHeartbeatTime(LocalDateTime.now());
        importExportRecordService.updateProgress(taskId, progress);
    }

    private ErpCustomerImportTaskRespVO buildTaskResponse(ErpImportExportRecordDO record) {
        ErpCustomerImportTaskRespVO response = new ErpCustomerImportTaskRespVO();
        response.setTaskId(record.getId());
        response.setRecordId(record.getId());
        response.setStatus(record.getStatus());
        response.setStage(record.getStage());
        response.setStageName(ErpImportExportRecordStatusEnum.FAILURE.getStatus().equals(record.getStatus())
                ? "导入失败" : ErpCustomerImportTaskStageEnum.getName(record.getStage()));
        response.setProgressPercent(calculateProgress(record));
        response.setTotalCount(taskSupport.valueOrZero(record.getTotalCount()));
        response.setProcessedCount(taskSupport.valueOrZero(record.getProcessedCount()));
        response.setSuccessCount(taskSupport.valueOrZero(record.getSuccessCount()));
        response.setFailureCount(taskSupport.valueOrZero(record.getFailureCount()));
        response.setCreateCount(taskSupport.valueOrZero(record.getCreateCount()));
        response.setUpdateCount(taskSupport.valueOrZero(record.getUpdateCount()));
        response.setFileName(record.getFileName());
        response.setOperatorName(record.getOperatorName());
        response.setStartTime(record.getStartTime());
        response.setEndTime(record.getEndTime());
        response.setDurationMs(taskSupport.resolveDuration(record));
        response.setErrorMessage(record.getErrorMessage());
        if (!ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())) {
            List<ErpCustomerImportRespVO.FailureItem> failures = new ArrayList<>();
            for (ErpImportExportRecordFailureDetailExportRespVO detail
                    : importExportRecordService.getFailureDetailList(record.getId())) {
                failures.add(new ErpCustomerImportRespVO.FailureItem(
                        detail.getRowNo(), detail.getBizKey(), detail.getFailureReason()));
            }
            response.setFailureDetails(failures);
        }
        return response;
    }

    private int calculateProgress(ErpImportExportRecordDO record) {
        if (!ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())
                || ErpCustomerImportTaskStageEnum.FINISHED.getStage().equals(record.getStage())) {
            return 100;
        }
        if (ErpCustomerImportTaskStageEnum.PARSING.getStage().equals(record.getStage())) {
            return 5;
        }
        if (ErpCustomerImportTaskStageEnum.IMPORTING.getStage().equals(record.getStage())) {
            int total = taskSupport.valueOrZero(record.getTotalCount());
            int processed = taskSupport.valueOrZero(record.getProcessedCount());
            return total <= 0 ? 10 : Math.min(90, 10 + (int) Math.floor(processed * 80.0D / total));
        }
        if (ErpCustomerImportTaskStageEnum.FINALIZING.getStage().equals(record.getStage())) {
            return 95;
        }
        return 0;
    }

    private String fileType(String fileName) {
        if (fileName == null) return null;
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 ? null : fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String briefErrorMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.trim().isEmpty()) message = current.getClass().getSimpleName();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private void deleteQuietly(Path file) {
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            log.warn("[deleteQuietly][删除客户导入临时文件失败 file={}]", file, ex);
        }
    }

    private class ProgressListener implements ErpCustomerImportProgressListener {

        private final Long taskId;
        private final long startMillis;
        private int totalCount;
        private int lastProcessed = -PROGRESS_BATCH_SIZE;
        private long lastFlushMillis;

        private ProgressListener(Long taskId, long startMillis) {
            this.taskId = taskId;
            this.startMillis = startMillis;
        }

        @Override
        public void onRowProcessed(int processedCount, int totalCount, ErpCustomerImportRespVO result) {
            this.totalCount = totalCount;
            long now = System.currentTimeMillis();
            if (processedCount != totalCount && processedCount - lastProcessed < PROGRESS_BATCH_SIZE
                    && now - lastFlushMillis < PROGRESS_INTERVAL_MILLIS) {
                return;
            }
            flush(ErpCustomerImportTaskStageEnum.IMPORTING.getStage(), processedCount, totalCount, result);
            lastProcessed = processedCount;
            lastFlushMillis = now;
        }

        @Override
        public void onFinalizing(int totalCount, ErpCustomerImportRespVO result) {
            this.totalCount = totalCount;
            flush(ErpCustomerImportTaskStageEnum.FINALIZING.getStage(), totalCount, totalCount, result);
        }

        private void flush(String stage, int processedCount, int totalCount, ErpCustomerImportRespVO result) {
            try {
                updateProgress(taskId, stage, processedCount, totalCount, result, startMillis);
            } catch (RuntimeException ex) {
                log.warn("[flush][taskId({}) 更新客户导入进度失败]", taskId, ex);
            }
        }

        private int getTotalCount() {
            return totalCount;
        }
    }

    private static class StoredMultipartFile implements MultipartFile {

        private final Path path;
        private final String originalFilename;

        private StoredMultipartFile(Path path, String originalFilename) {
            this.path = path;
            this.originalFilename = originalFilename;
        }

        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() {
            try { return Files.probeContentType(path); } catch (IOException ex) { return null; }
        }
        @Override public boolean isEmpty() { return getSize() <= 0; }
        @Override public long getSize() {
            try { return Files.size(path); } catch (IOException ex) { return 0L; }
        }
        @Override public byte[] getBytes() throws IOException { return Files.readAllBytes(path); }
        @Override public InputStream getInputStream() throws IOException { return Files.newInputStream(path); }
        @Override public void transferTo(File dest) throws IOException {
            Files.copy(path, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
