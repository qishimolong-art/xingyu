package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordFailureDetailExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveImportFailureRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveImportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitImportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpSimpleArchiveImportTaskStageEnum;
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
import javax.servlet.http.HttpServletResponse;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SIMPLE_ARCHIVE_IMPORT_FILE_TYPE_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SIMPLE_ARCHIVE_IMPORT_TASK_BUSY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SIMPLE_ARCHIVE_IMPORT_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SIMPLE_ARCHIVE_IMPORT_TASK_SUBMIT_FAILED;

@Service
@Slf4j
public class ErpSimpleArchiveImportTaskServiceImpl implements ErpSimpleArchiveImportTaskService {

    private static final String LOCK_KEY_PREFIX = "erp:simple-archive-import-task:";
    private static final int PROGRESS_BATCH_SIZE = 20;
    private static final long PROGRESS_INTERVAL_MILLIS = 500L;
    private static final Duration ORPHAN_FILE_TIMEOUT = Duration.ofHours(24);
    private static final Path TEMP_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir"),
            "xingyu", "simple-archive-import");

    @Resource private ErpProductCategoryService categoryService;
    @Resource private ErpProductBrandService brandService;
    @Resource private ErpProductUnitService unitService;
    @Resource private ErpImportExportRecordService importExportRecordService;
    @Resource private ErpImportExportRecordMapper recordMapper;
    @Resource private ErpAsyncImportTaskSupport taskSupport;
    @Resource private RedissonClient redissonClient;
    @Resource(name = "erpAsyncImportTaskExecutor") private ThreadPoolTaskExecutor taskExecutor;
    @Value("${yudao.erp.import.max-rows:3000}") private int importMaxRows;

    @PostConstruct
    public void cleanOrphanFilesOnStartup() {
        try {
            Files.createDirectories(TEMP_DIRECTORY);
            LocalDateTime cutoff = LocalDateTime.now().minus(ORPHAN_FILE_TIMEOUT);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(TEMP_DIRECTORY)) {
                for (Path path : stream) {
                    if (Files.isRegularFile(path) && LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(path).toInstant(), java.time.ZoneId.systemDefault())
                            .isBefore(cutoff)) {
                        Files.deleteIfExists(path);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[cleanOrphanFilesOnStartup][清理简单档案导入临时文件失败]", ex);
        }
    }

    @Override
    public ErpArchiveImportTaskStartRespVO startTask(ErpSimpleArchiveModule module, MultipartFile file)
            throws IOException {
        validateFile(module, file);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorId == null) {
            throw new AccessDeniedException("未登录，不能创建" + module.getModuleName() + "导入任务");
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + module.getModuleKey() + ":" + tenantId);
        lock.lock();
        try {
            ErpImportExportRecordDO active = refreshStaleRecord(module,
                    taskSupport.findTenantActiveRecord(module.getModuleKey()));
            if (active != null) {
                if (operatorId.equals(active.getOperatorId())) {
                    return new ErpArchiveImportTaskStartRespVO(active.getId(), true);
                }
                throw exception(SIMPLE_ARCHIVE_IMPORT_TASK_BUSY,
                        active.getOperatorName() == null ? "其他用户" : active.getOperatorName(),
                        module.getModuleName());
            }
            Path storedFile = storeFile(module, file);
            Long taskId;
            try {
                taskId = createTaskRecord(module, file.getOriginalFilename());
            } catch (RuntimeException ex) {
                deleteQuietly(storedFile);
                throw ex;
            }
            try {
                taskExecutor.execute(() -> executeTask(module, taskId, storedFile, file.getOriginalFilename()));
            } catch (RuntimeException ex) {
                deleteQuietly(storedFile);
                finishFailure(taskId, "后台导入任务队列繁忙，请稍后重试", System.currentTimeMillis());
                throw exception(SIMPLE_ARCHIVE_IMPORT_TASK_SUBMIT_FAILED, module.getModuleName());
            }
            return new ErpArchiveImportTaskStartRespVO(taskId, false);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public ErpArchiveImportTaskRespVO getActiveTask(ErpSimpleArchiveModule module) {
        ErpImportExportRecordDO record = refreshStaleRecord(module,
                taskSupport.findOwnedActiveRecord(module.getModuleKey()));
        return record == null ? null : buildTaskResponse(record);
    }

    @Override
    public ErpArchiveImportTaskRespVO getTask(ErpSimpleArchiveModule module, Long taskId) {
        ErpImportExportRecordDO record = taskSupport.findOwnedRecord(taskId, module.getModuleKey());
        if (record == null) {
            throw exception(SIMPLE_ARCHIVE_IMPORT_TASK_NOT_EXISTS, module.getModuleName());
        }
        return buildTaskResponse(refreshStaleRecord(module, record));
    }

    @Override
    public void downloadFailureDetails(ErpSimpleArchiveModule module, Long recordId,
                                       HttpServletResponse response) throws IOException {
        ErpImportExportRecordDO record = recordMapper.selectByIdAndTenant(
                recordId, TenantContextHolder.getRequiredTenantId());
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (record == null || operatorId == null || !operatorId.equals(record.getOperatorId())
                || !module.getModuleKey().equals(record.getModuleKey())
                || !ErpImportExportOperationTypeEnum.IMPORT.getType().equals(record.getOperationType())) {
            throw new AccessDeniedException("没有下载该导入错误数据的权限");
        }
        importExportRecordService.downloadOwnImportFailureDetails(recordId, module.getModuleKey(), response);
    }

    private ErpImportExportRecordDO refreshStaleRecord(ErpSimpleArchiveModule module,
                                                       ErpImportExportRecordDO record) {
        if (record == null || !taskSupport.isStale(record)) {
            return record;
        }
        finishFailure(record.getId(), "任务超过 30 分钟未更新进度，已自动结束，请重新导入",
                record.getStartTime() == null ? System.currentTimeMillis()
                        : System.currentTimeMillis() - Duration.between(
                                record.getStartTime(), LocalDateTime.now()).toMillis());
        log.warn("[refreshStaleRecord][module={} taskId={} 导入任务超时]", module.getModuleKey(), record.getId());
        return recordMapper.selectById(record.getId());
    }

    private void validateFile(ErpSimpleArchiveModule module, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(SIMPLE_ARCHIVE_IMPORT_FILE_TYPE_INVALID, module.getModuleName());
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        boolean supported = fileName.endsWith(".xls") || fileName.endsWith(".xlsx")
                || module.isCsvSupported() && fileName.endsWith(".csv");
        if (!supported) {
            throw exception(SIMPLE_ARCHIVE_IMPORT_FILE_TYPE_INVALID, module.getModuleName());
        }
    }

    private Path storeFile(ErpSimpleArchiveModule module, MultipartFile file) throws IOException {
        Files.createDirectories(TEMP_DIRECTORY);
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dotIndex = fileName.lastIndexOf('.');
        String suffix = dotIndex < 0 ? ".tmp" : fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
        Path storedFile = Files.createTempFile(TEMP_DIRECTORY,
                module.getModuleKey().replace("erp_", "") + "-", suffix);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, storedFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            deleteQuietly(storedFile);
            throw ex;
        }
        return storedFile;
    }

    private Long createTaskRecord(ErpSimpleArchiveModule module, String originalFileName) {
        ErpImportExportRecordCreateReqBO create = new ErpImportExportRecordCreateReqBO();
        create.setOperationType(ErpImportExportOperationTypeEnum.IMPORT.getType());
        create.setModuleKey(module.getModuleKey());
        create.setModuleName(module.getModuleName());
        create.setTemplateKey(module.getImportClass().getName());
        create.setFileName(originalFileName);
        create.setFileType(fileType(originalFileName));
        create.setStage(ErpSimpleArchiveImportTaskStageEnum.QUEUED.getStage());
        create.setProcessedCount(0);
        create.setStartTime(LocalDateTime.now());
        create.setHeartbeatTime(LocalDateTime.now());
        return importExportRecordService.createRecord(create);
    }

    private void executeTask(ErpSimpleArchiveModule module, Long taskId, Path storedFile,
                             String originalFileName) {
        long startMillis = System.currentTimeMillis();
        try {
            updateProgress(taskId, ErpSimpleArchiveImportTaskStageEnum.PARSING.getStage(),
                    0, 0, 0, 0, 0, 0, startMillis);
            List<?> rows = parseRows(module, storedFile, originalFileName);
            if (rows.size() > importMaxRows) {
                throw exception(ERP_IMPORT_ROW_LIMIT_EXCEEDED, rows.size(), importMaxRows);
            }
            ProgressListener listener = new ProgressListener(taskId, startMillis);
            ImportOutcome outcome = importRows(module, rows, listener);
            finishSuccess(module, taskId, rows, outcome, listener.getTotalCount(), startMillis);
        } catch (Throwable ex) {
            log.error("[executeTask][module={} taskId={} 简单档案导入失败]", module.getModuleKey(), taskId, ex);
            finishFailure(taskId, briefErrorMessage(ex), startMillis);
        } finally {
            ExcelUtils.clearLastOperation();
            deleteQuietly(storedFile);
        }
    }

    private List<?> parseRows(ErpSimpleArchiveModule module, Path storedFile,
                              String originalFileName) throws IOException {
        boolean csv = originalFileName != null && originalFileName.toLowerCase(Locale.ROOT).endsWith(".csv");
        if (csv) {
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(storedFile), StandardCharsets.UTF_8)) {
                return module == ErpSimpleArchiveModule.PRODUCT_BRAND
                        ? brandService.parseCsvImport(reader) : unitService.parseCsvImport(reader);
            }
        }
        StoredMultipartFile multipartFile = new StoredMultipartFile(storedFile, originalFileName);
        int rowCount = ExcelUtils.countFirstSheetDataRows(multipartFile, 1);
        if (rowCount > importMaxRows) {
            throw exception(ERP_IMPORT_ROW_LIMIT_EXCEEDED, rowCount, importMaxRows);
        }
        if (module == ErpSimpleArchiveModule.PRODUCT_CATEGORY) {
            return ExcelUtils.read(multipartFile, ErpProductCategoryImportExcelVO.class);
        }
        if (module == ErpSimpleArchiveModule.PRODUCT_BRAND) {
            return ExcelUtils.read(multipartFile, ErpProductBrandImportExcelVO.class);
        }
        return ExcelUtils.read(multipartFile, ErpProductUnitImportExcelVO.class);
    }

    @SuppressWarnings("unchecked")
    private ImportOutcome importRows(ErpSimpleArchiveModule module, List<?> rows,
                                     ErpSimpleArchiveImportProgressListener listener) {
        if (module == ErpSimpleArchiveModule.PRODUCT_CATEGORY) {
            ErpProductCategoryImportRespVO result = categoryService.importProductCategoryList(
                    (List<ErpProductCategoryImportExcelVO>) rows, listener);
            List<Failure> failures = new ArrayList<>();
            for (ErpProductCategoryImportRespVO.FailureItem item : result.getFailureDetails()) {
                failures.add(new Failure(item.getRowNo(), item.getCode(), item.getReason()));
            }
            return new ImportOutcome(result.getSuccessCount(), result.getCreateCount(), result.getUpdateCount(),
                    result.getFailureCount(), failures);
        }
        if (module == ErpSimpleArchiveModule.PRODUCT_BRAND) {
            ErpProductBrandImportRespVO result = brandService.importProductBrandList(
                    (List<ErpProductBrandImportExcelVO>) rows, listener);
            List<Failure> failures = new ArrayList<>();
            for (ErpProductBrandImportRespVO.FailureItem item : result.getFailureDetails()) {
                failures.add(new Failure(item.getRowNo(), item.getName(), item.getReason()));
            }
            return new ImportOutcome(result.getSuccessCount(), result.getCreateCount(), result.getUpdateCount(),
                    result.getFailureCount(), failures);
        }
        ErpProductUnitImportRespVO result = unitService.importProductUnitList(
                (List<ErpProductUnitImportExcelVO>) rows, listener);
        List<Failure> failures = new ArrayList<>();
        for (ErpProductUnitImportRespVO.FailureItem item : result.getFailureDetails()) {
            failures.add(new Failure(item.getRowNo(), item.getName(), item.getReason()));
        }
        return new ImportOutcome(result.getSuccessCount(), result.getCreateCount(), result.getUpdateCount(),
                result.getFailureCount(), failures);
    }

    private void finishSuccess(ErpSimpleArchiveModule module, Long taskId, List<?> rows,
                               ImportOutcome outcome, int totalCount, long startMillis) {
        ErpImportExportRecordFinishReqBO finish = new ErpImportExportRecordFinishReqBO();
        finish.setTemplateKey(module.getImportClass().getName());
        finish.setStatus(ErpImportExportRecordStatusEnum.ofCount(outcome.successCount, outcome.failureCount));
        finish.setStage(ErpSimpleArchiveImportTaskStageEnum.FINISHED.getStage());
        finish.setTotalCount(totalCount);
        finish.setProcessedCount(totalCount);
        finish.setSuccessCount(outcome.successCount);
        finish.setFailureCount(outcome.failureCount);
        finish.setCreateCount(outcome.createCount);
        finish.setUpdateCount(outcome.updateCount);
        finish.setDurationMs(System.currentTimeMillis() - startMillis);
        finish.setEndTime(LocalDateTime.now());
        finish.setHeartbeatTime(LocalDateTime.now());
        finish.setFailureDetails(buildFailureDetails(rows, outcome.failures));
        importExportRecordService.finishRecord(taskId, finish);
    }

    private List<ErpImportExportFailureDetailBO> buildFailureDetails(List<?> rows, List<Failure> failures) {
        if (failures.isEmpty()) {
            return Collections.emptyList();
        }
        List<ErpImportExportFailureDetailBO> details = new ArrayList<>(failures.size());
        for (Failure failure : failures) {
            ErpImportExportFailureDetailBO detail = new ErpImportExportFailureDetailBO();
            detail.setRowNo(failure.rowNo);
            detail.setGroupKey(failure.name == null ? "ROW-" + failure.rowNo : failure.name);
            detail.setDetailType("FAILURE");
            detail.setBizKey(failure.name);
            detail.setBizName(failure.name);
            detail.setFailureReason(failure.reason);
            int rowIndex = failure.rowNo == null ? -1 : failure.rowNo - 2;
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
        finish.setStage(ErpSimpleArchiveImportTaskStageEnum.FINISHED.getStage());
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
                                int successCount, int createCount, int updateCount, int failureCount,
                                long startMillis) {
        ErpImportExportRecordProgressReqBO progress = new ErpImportExportRecordProgressReqBO();
        progress.setStage(stage);
        progress.setTotalCount(totalCount);
        progress.setProcessedCount(processedCount);
        progress.setSuccessCount(successCount);
        progress.setFailureCount(failureCount);
        progress.setCreateCount(createCount);
        progress.setUpdateCount(updateCount);
        progress.setDurationMs(System.currentTimeMillis() - startMillis);
        progress.setHeartbeatTime(LocalDateTime.now());
        importExportRecordService.updateProgress(taskId, progress);
    }

    private ErpArchiveImportTaskRespVO buildTaskResponse(ErpImportExportRecordDO record) {
        ErpArchiveImportTaskRespVO response = new ErpArchiveImportTaskRespVO();
        response.setTaskId(record.getId());
        response.setRecordId(record.getId());
        response.setStatus(record.getStatus());
        response.setStage(record.getStage());
        response.setStageName(ErpImportExportRecordStatusEnum.FAILURE.getStatus().equals(record.getStatus())
                ? "导入失败" : ErpSimpleArchiveImportTaskStageEnum.getName(record.getStage()));
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
            List<ErpArchiveImportFailureRespVO> failures = new ArrayList<>();
            for (ErpImportExportRecordFailureDetailExportRespVO detail
                    : importExportRecordService.getFailureDetailList(record.getId())) {
                failures.add(new ErpArchiveImportFailureRespVO(
                        detail.getRowNo(), detail.getBizName(), detail.getFailureReason()));
            }
            response.setFailureDetails(failures);
        }
        return response;
    }

    private int calculateProgress(ErpImportExportRecordDO record) {
        if (!ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())
                || ErpSimpleArchiveImportTaskStageEnum.FINISHED.getStage().equals(record.getStage())) {
            return 100;
        }
        if (ErpSimpleArchiveImportTaskStageEnum.PARSING.getStage().equals(record.getStage())) {
            return 5;
        }
        if (ErpSimpleArchiveImportTaskStageEnum.IMPORTING.getStage().equals(record.getStage())) {
            int total = taskSupport.valueOrZero(record.getTotalCount());
            int processed = taskSupport.valueOrZero(record.getProcessedCount());
            return total <= 0 ? 10 : Math.min(90, 10 + (int) Math.floor(processed * 80.0D / total));
        }
        if (ErpSimpleArchiveImportTaskStageEnum.FINALIZING.getStage().equals(record.getStage())) {
            return 95;
        }
        return 0;
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
        try {
            if (file != null) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ex) {
            log.warn("[deleteQuietly][删除简单档案导入临时文件失败 file={}]", file, ex);
        }
    }

    private class ProgressListener implements ErpSimpleArchiveImportProgressListener {
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
        public void onRowsProgress(int processedCount, int totalCount, int successCount,
                                   int createCount, int updateCount, int failureCount) {
            this.totalCount = totalCount;
            long now = System.currentTimeMillis();
            if (processedCount != totalCount && processedCount - lastProcessed < PROGRESS_BATCH_SIZE
                    && now - lastFlushMillis < PROGRESS_INTERVAL_MILLIS) {
                return;
            }
            flush(ErpSimpleArchiveImportTaskStageEnum.IMPORTING.getStage(), processedCount, totalCount,
                    successCount, createCount, updateCount, failureCount);
            lastProcessed = processedCount;
            lastFlushMillis = now;
        }

        @Override
        public void onFinalizing(int totalCount, int successCount,
                                 int createCount, int updateCount, int failureCount) {
            this.totalCount = totalCount;
            flush(ErpSimpleArchiveImportTaskStageEnum.FINALIZING.getStage(), totalCount, totalCount,
                    successCount, createCount, updateCount, failureCount);
        }

        private void flush(String stage, int processedCount, int totalCount, int successCount,
                           int createCount, int updateCount, int failureCount) {
            try {
                updateProgress(taskId, stage, processedCount, totalCount, successCount,
                        createCount, updateCount, failureCount, startMillis);
            } catch (RuntimeException ex) {
                log.warn("[flush][taskId={} 更新简单档案导入进度失败]", taskId, ex);
            }
        }

        private int getTotalCount() {
            return totalCount;
        }
    }

    private static class ImportOutcome {
        private final int successCount;
        private final int createCount;
        private final int updateCount;
        private final int failureCount;
        private final List<Failure> failures;

        private ImportOutcome(int successCount, int createCount, int updateCount,
                              int failureCount, List<Failure> failures) {
            this.successCount = successCount;
            this.createCount = createCount;
            this.updateCount = updateCount;
            this.failureCount = failureCount;
            this.failures = failures;
        }
    }

    private static class Failure {
        private final Integer rowNo;
        private final String name;
        private final String reason;

        private Failure(Integer rowNo, String name, String reason) {
            this.rowNo = rowNo;
            this.name = name;
            this.reason = reason;
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
