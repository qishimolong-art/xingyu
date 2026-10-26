package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordFailureDetailExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.enums.product.ErpProductImportTaskStageEnum;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ERP_IMPORT_ROW_LIMIT_EXCEEDED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_IMPORT_FILE_TYPE_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_IMPORT_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_IMPORT_TASK_SUBMIT_FAILED;

@Service
@Slf4j
public class ErpProductImportTaskServiceImpl implements ErpProductImportTaskService {

    private static final String MODULE_KEY = "erp_product";
    private static final String MODULE_NAME = "产品";
    private static final String LOCK_KEY_PREFIX = "erp:product-import-task:";
    private static final Duration STALE_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration ORPHAN_FILE_TIMEOUT = Duration.ofHours(24);
    private static final int PROGRESS_BATCH_SIZE = 20;
    private static final long PROGRESS_INTERVAL_MILLIS = 500L;
    private static final Path TEMP_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir"),
            "xingyu", "product-import");

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private RedissonClient redissonClient;
    @Resource(name = "erpProductImportTaskExecutor")
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
            log.warn("[cleanOrphanFilesOnStartup][清理配件导入临时文件失败]", ex);
        }
    }

    @Override
    public ErpProductImportTaskStartRespVO startTask(MultipartFile file) throws IOException {
        validateFile(file);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + tenantId);
        lock.lock();
        try {
            ErpImportExportRecordRespVO activeRecord = getActiveRecord();
            if (activeRecord != null) {
                return new ErpProductImportTaskStartRespVO(activeRecord.getId(), true);
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
                finishFailure(taskId, "后台任务队列繁忙，请稍后重试");
                throw exception(PRODUCT_IMPORT_TASK_SUBMIT_FAILED);
            }
            return new ErpProductImportTaskStartRespVO(taskId, false);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public ErpProductImportTaskRespVO getActiveTask() {
        ErpImportExportRecordRespVO record = getActiveRecord();
        return record == null ? null : buildTaskResponse(record);
    }

    @Override
    public ErpProductImportTaskRespVO getTask(Long taskId) {
        ErpImportExportRecordRespVO record = importExportRecordService.getRecordForCurrentTenant(taskId);
        if (!isProductImportTask(record)) {
            throw exception(PRODUCT_IMPORT_TASK_NOT_EXISTS);
        }
        return buildTaskResponse(record);
    }

    private ErpImportExportRecordRespVO getActiveRecord() {
        ErpImportExportRecordRespVO record = importExportRecordService.getLatestProcessingRecord(
                ErpImportExportOperationTypeEnum.IMPORT.getType(), MODULE_KEY);
        if (record == null) {
            return null;
        }
        LocalDateTime lastHeartbeat = record.getHeartbeatTime() == null
                ? record.getStartTime() : record.getHeartbeatTime();
        if (lastHeartbeat != null && lastHeartbeat.isBefore(LocalDateTime.now().minus(STALE_TIMEOUT))) {
            finishFailure(record.getId(), "任务超过 30 分钟未更新进度，已自动结束，请重新导入");
            return null;
        }
        return record;
    }

    private boolean isProductImportTask(ErpImportExportRecordRespVO record) {
        return record != null
                && MODULE_KEY.equals(record.getModuleKey())
                && ErpImportExportOperationTypeEnum.IMPORT.getType().equals(record.getOperationType());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(PRODUCT_IMPORT_FILE_TYPE_INVALID);
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx") && !fileName.endsWith(".csv")) {
            throw exception(PRODUCT_IMPORT_FILE_TYPE_INVALID);
        }
    }

    private Path storeFile(MultipartFile file) throws IOException {
        Files.createDirectories(TEMP_DIRECTORY);
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dotIndex = fileName.lastIndexOf('.');
        String suffix = dotIndex < 0 ? ".tmp" : fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
        Path storedFile = Files.createTempFile(TEMP_DIRECTORY, "product-import-", suffix);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, storedFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            deleteQuietly(storedFile);
            throw ex;
        }
        return storedFile;
    }

    private Long createTaskRecord(String originalFileName) {
        ErpImportExportRecordCreateReqBO reqBO = new ErpImportExportRecordCreateReqBO();
        reqBO.setOperationType(ErpImportExportOperationTypeEnum.IMPORT.getType());
        reqBO.setModuleKey(MODULE_KEY);
        reqBO.setModuleName(MODULE_NAME);
        reqBO.setTemplateKey(ErpProductImportExcelVO.class.getName());
        reqBO.setFileName(originalFileName);
        reqBO.setFileType(fileType(originalFileName));
        reqBO.setStage(ErpProductImportTaskStageEnum.QUEUED.getStage());
        reqBO.setProcessedCount(0);
        reqBO.setStartTime(LocalDateTime.now());
        reqBO.setHeartbeatTime(LocalDateTime.now());
        return importExportRecordService.createRecord(reqBO);
    }

    private void executeTask(Long taskId, Path storedFile, String originalFileName) {
        long startMillis = System.currentTimeMillis();
        try {
            updateProgress(taskId, ErpProductImportTaskStageEnum.PARSING.getStage(), 0, 0,
                    new ErpProductImportRespVO(), startMillis);
            List<ErpProductImportExcelVO> rows = readRows(storedFile, originalFileName);
            ProgressListener listener = new ProgressListener(taskId, startMillis);
            ErpProductImportRespVO result = productService.importProductList(rows, listener);
            finishSuccess(taskId, rows, result, listener.getTotalCount(), startMillis);
        } catch (Throwable ex) {
            log.error("[executeTask][taskId({}) 配件导入失败]", taskId, ex);
            finishFailure(taskId, briefErrorMessage(ex), startMillis);
        } finally {
            ExcelUtils.clearLastOperation();
            deleteQuietly(storedFile);
        }
    }

    private List<ErpProductImportExcelVO> readRows(Path storedFile, String originalFileName) throws IOException {
        String lowerName = originalFileName == null ? "" : originalFileName.toLowerCase(Locale.ROOT);
        StoredMultipartFile multipartFile = new StoredMultipartFile(storedFile, originalFileName);
        if (lowerName.endsWith(".csv")) {
            try (InputStreamReader reader = new InputStreamReader(multipartFile.getInputStream(), StandardCharsets.UTF_8)) {
                List<ErpProductImportExcelVO> rows = productService.parseCsvImport(reader);
                validateRowLimit(rows.size());
                return rows;
            }
        }
        int rowCount = ExcelUtils.countFirstSheetDataRows(multipartFile, 1);
        validateRowLimit(rowCount);
        return ExcelUtils.read(multipartFile, ErpProductImportExcelVO.class);
    }

    private void validateRowLimit(int rowCount) {
        if (rowCount > importMaxRows) {
            throw exception(ERP_IMPORT_ROW_LIMIT_EXCEEDED, rowCount, importMaxRows);
        }
    }

    private void finishSuccess(Long taskId, List<ErpProductImportExcelVO> rows, ErpProductImportRespVO result,
                               int totalCount, long startMillis) {
        ErpImportExportRecordFinishReqBO reqBO = new ErpImportExportRecordFinishReqBO();
        reqBO.setTemplateKey(ErpProductImportExcelVO.class.getName());
        reqBO.setStatus(ErpImportExportRecordStatusEnum.ofCount(result.getSuccessCount(), result.getFailureCount()));
        reqBO.setStage(ErpProductImportTaskStageEnum.FINISHED.getStage());
        reqBO.setTotalCount(totalCount);
        reqBO.setProcessedCount(totalCount);
        reqBO.setSuccessCount(result.getSuccessCount());
        reqBO.setFailureCount(result.getFailureCount());
        reqBO.setCreateCount(result.getCreateCount());
        reqBO.setUpdateCount(result.getUpdateCount());
        reqBO.setDurationMs(System.currentTimeMillis() - startMillis);
        reqBO.setEndTime(LocalDateTime.now());
        reqBO.setHeartbeatTime(LocalDateTime.now());
        reqBO.setFailureDetails(buildFailureDetails(rows, result.getFailureDetails()));
        importExportRecordService.finishRecord(taskId, reqBO);
    }

    private List<ErpImportExportFailureDetailBO> buildFailureDetails(
            List<ErpProductImportExcelVO> rows, List<ErpProductImportRespVO.FailureItem> failures) {
        if (failures == null || failures.isEmpty()) {
            return Collections.emptyList();
        }
        List<ErpImportExportFailureDetailBO> details = new ArrayList<>(failures.size());
        for (ErpProductImportRespVO.FailureItem failure : failures) {
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

    private void finishFailure(Long taskId, String errorMessage) {
        finishFailure(taskId, errorMessage, System.currentTimeMillis());
    }

    private void finishFailure(Long taskId, String errorMessage, long startMillis) {
        ErpImportExportRecordFinishReqBO reqBO = new ErpImportExportRecordFinishReqBO();
        reqBO.setStatus(ErpImportExportRecordStatusEnum.FAILURE.getStatus());
        reqBO.setStage(ErpProductImportTaskStageEnum.FINISHED.getStage());
        reqBO.setTotalCount(0);
        reqBO.setProcessedCount(0);
        reqBO.setSuccessCount(0);
        reqBO.setFailureCount(1);
        reqBO.setCreateCount(0);
        reqBO.setUpdateCount(0);
        reqBO.setErrorMessage(errorMessage);
        reqBO.setDurationMs(Math.max(0L, System.currentTimeMillis() - startMillis));
        reqBO.setEndTime(LocalDateTime.now());
        reqBO.setHeartbeatTime(LocalDateTime.now());
        importExportRecordService.finishRecord(taskId, reqBO);
    }

    private void updateProgress(Long taskId, String stage, int processedCount, int totalCount,
                                ErpProductImportRespVO result, long startMillis) {
        ErpImportExportRecordProgressReqBO reqBO = new ErpImportExportRecordProgressReqBO();
        reqBO.setStage(stage);
        reqBO.setTotalCount(totalCount);
        reqBO.setProcessedCount(processedCount);
        reqBO.setSuccessCount(result.getSuccessCount());
        reqBO.setFailureCount(result.getFailureCount());
        reqBO.setCreateCount(result.getCreateCount());
        reqBO.setUpdateCount(result.getUpdateCount());
        reqBO.setDurationMs(System.currentTimeMillis() - startMillis);
        reqBO.setHeartbeatTime(LocalDateTime.now());
        importExportRecordService.updateProgress(taskId, reqBO);
    }

    private ErpProductImportTaskRespVO buildTaskResponse(ErpImportExportRecordRespVO record) {
        ErpProductImportTaskRespVO response = new ErpProductImportTaskRespVO();
        response.setTaskId(record.getId());
        response.setRecordId(record.getId());
        response.setStatus(record.getStatus());
        response.setStage(record.getStage());
        response.setStageName(ErpProductImportTaskStageEnum.getName(record.getStage()));
        response.setProgressPercent(calculateProgress(record));
        response.setTotalCount(valueOrZero(record.getTotalCount()));
        response.setProcessedCount(valueOrZero(record.getProcessedCount()));
        response.setSuccessCount(valueOrZero(record.getSuccessCount()));
        response.setFailureCount(valueOrZero(record.getFailureCount()));
        response.setCreateCount(valueOrZero(record.getCreateCount()));
        response.setUpdateCount(valueOrZero(record.getUpdateCount()));
        response.setFileName(record.getFileName());
        response.setOperatorName(record.getOperatorName());
        response.setStartTime(record.getStartTime());
        response.setEndTime(record.getEndTime());
        response.setDurationMs(resolveDuration(record));
        response.setErrorMessage(record.getErrorMessage());
        if (!ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())) {
            List<ErpProductImportRespVO.FailureItem> failures = new ArrayList<>();
            for (ErpImportExportRecordFailureDetailExportRespVO detail
                    : importExportRecordService.getFailureDetailList(record.getId())) {
                failures.add(new ErpProductImportRespVO.FailureItem(
                        detail.getRowNo(), detail.getBizKey(), detail.getFailureReason()));
            }
            response.setFailureDetails(failures);
        }
        return response;
    }

    private int calculateProgress(ErpImportExportRecordRespVO record) {
        if (!ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())
                || ErpProductImportTaskStageEnum.FINISHED.getStage().equals(record.getStage())) {
            return 100;
        }
        if (ErpProductImportTaskStageEnum.PARSING.getStage().equals(record.getStage())) {
            return 5;
        }
        if (ErpProductImportTaskStageEnum.IMPORTING.getStage().equals(record.getStage())) {
            int total = valueOrZero(record.getTotalCount());
            int processed = valueOrZero(record.getProcessedCount());
            return total <= 0 ? 10 : Math.min(90, 10 + (int) Math.floor(processed * 80.0D / total));
        }
        if (ErpProductImportTaskStageEnum.FINALIZING.getStage().equals(record.getStage())) {
            return 95;
        }
        return 0;
    }

    private long resolveDuration(ErpImportExportRecordRespVO record) {
        if (record.getDurationMs() != null) {
            return record.getDurationMs();
        }
        return record.getStartTime() == null ? 0L
                : Math.max(0L, Duration.between(record.getStartTime(), LocalDateTime.now()).toMillis());
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String fileType(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 ? null : fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String briefErrorMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = current.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            log.warn("[deleteQuietly][删除配件导入临时文件失败 file={}]", file, ex);
        }
    }

    private class ProgressListener implements ErpProductImportProgressListener {

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
        public void onRowsProgress(int processedCount, int totalCount, ErpProductImportRespVO result) {
            this.totalCount = totalCount;
            long now = System.currentTimeMillis();
            if (processedCount != 0 && processedCount != totalCount
                    && processedCount - lastProcessed < PROGRESS_BATCH_SIZE
                    && now - lastFlushMillis < PROGRESS_INTERVAL_MILLIS) {
                return;
            }
            flush(ErpProductImportTaskStageEnum.IMPORTING.getStage(), processedCount, totalCount, result);
            lastProcessed = processedCount;
            lastFlushMillis = now;
        }

        @Override
        public void onFinalizing(int totalCount, ErpProductImportRespVO result) {
            this.totalCount = totalCount;
            flush(ErpProductImportTaskStageEnum.FINALIZING.getStage(), totalCount, totalCount, result);
        }

        private void flush(String stage, int processedCount, int totalCount, ErpProductImportRespVO result) {
            try {
                updateProgress(taskId, stage, processedCount, totalCount, result, startMillis);
            } catch (RuntimeException ex) {
                log.warn("[flush][taskId({}) 更新配件导入进度失败]", taskId, ex);
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

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            try {
                return Files.probeContentType(path);
            } catch (IOException ex) {
                return null;
            }
        }

        @Override
        public boolean isEmpty() {
            return getSize() <= 0;
        }

        @Override
        public long getSize() {
            try {
                return Files.size(path);
            } catch (IOException ex) {
                return 0L;
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(path);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.copy(path, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

    }

}
