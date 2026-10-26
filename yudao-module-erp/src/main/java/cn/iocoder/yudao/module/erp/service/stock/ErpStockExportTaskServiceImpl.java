package cn.iocoder.yudao.module.erp.service.stock;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockExportTaskStageEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.common.ErpAsyncExportTaskSupport;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordFinishReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordProgressReqBO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_EXPORT_TASK_FILE_EXPIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_EXPORT_TASK_FILE_NOT_READY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_EXPORT_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_EXPORT_TASK_SUBMIT_FAILED;

@Service
@Slf4j
public class ErpStockExportTaskServiceImpl implements ErpStockExportTaskService {

    private static final String MODULE_KEY = "erp_stock";
    private static final String MODULE_NAME = "产品库存";
    private static final String LOCK_KEY_PREFIX = "erp:stock-export-task:";
    private static final int EXPORT_PAGE_SIZE = 500;
    private static final Duration STALE_TIMEOUT = Duration.ofMinutes(30);
    private static final Path TEMP_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir"),
            "xingyu", "stock-export");
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private ErpAsyncExportTaskSupport asyncExportTaskSupport;
    @Resource
    private RedissonClient redissonClient;
    @Resource(name = "erpStockExportTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @PostConstruct
    public void prepareDirectory() {
        try {
            Files.createDirectories(TEMP_DIRECTORY);
            cleanExpiredFiles();
        } catch (Exception ex) {
            log.warn("[prepareDirectory][初始化产品库存导出目录失败]", ex);
        }
    }

    @Scheduled(cron = "0 17 * * * ?")
    public void cleanExpiredFilesScheduled() {
        try {
            cleanExpiredFiles();
        } catch (Exception ex) {
            log.warn("[cleanExpiredFilesScheduled][清理过期产品库存导出文件失败]", ex);
        }
    }

    @Override
    public ErpStockExportTaskStartRespVO startTask(ErpStockPageReqVO reqVO,
                                                   ErpStockExportPageLoader pageLoader) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorId == null) {
            throw new AccessDeniedException("未登录，不能创建产品库存导出任务");
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + tenantId + ":" + operatorId);
        lock.lock();
        try {
            ErpImportExportRecordDO activeRecord = getActiveRecord(tenantId, operatorId);
            if (activeRecord != null) {
                return new ErpStockExportTaskStartRespVO(activeRecord.getId(), true);
            }
            ErpStockPageReqVO taskReqVO = BeanUtils.toBean(reqVO, ErpStockPageReqVO.class);
            taskReqVO.setPageNo(1);
            taskReqVO.setPageSize(EXPORT_PAGE_SIZE);
            taskReqVO.setStreamingExport(true);
            Long taskId = createTaskRecord(taskReqVO);
            try {
                taskExecutor.execute(() -> executeTask(taskId, tenantId, operatorId, taskReqVO, pageLoader));
            } catch (RuntimeException ex) {
                finishFailure(taskId, "后台导出任务队列繁忙，请稍后重试", System.currentTimeMillis());
                throw exception(STOCK_EXPORT_TASK_SUBMIT_FAILED);
            }
            return new ErpStockExportTaskStartRespVO(taskId, false);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public ErpStockExportTaskRespVO getActiveTask() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        ErpImportExportRecordDO record = getActiveRecord(tenantId, operatorId);
        return record == null ? null : buildTaskResponse(record);
    }

    @Override
    public ErpStockExportTaskRespVO getTask(Long taskId) {
        ErpImportExportRecordDO record = getOwnedRecord(taskId);
        if (markStaleTaskFailed(record)) {
            record = getOwnedRecord(taskId);
        }
        return buildTaskResponse(record);
    }

    @Override
    public void downloadTask(Long taskId, HttpServletResponse response) throws IOException {
        ErpImportExportRecordDO record = getOwnedRecord(taskId);
        if (ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())) {
            throw exception(STOCK_EXPORT_TASK_FILE_NOT_READY);
        }
        Path file = finalFile(record.getTenantId(), record.getOperatorId(), record.getId());
        if (!ErpImportExportRecordStatusEnum.SUCCESS.getStatus().equals(record.getStatus())) {
            throw exception(STOCK_EXPORT_TASK_FILE_NOT_READY);
        }
        if (isExpired(record) || !Files.isRegularFile(file)) {
            throw exception(STOCK_EXPORT_TASK_FILE_EXPIRED);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename="
                + HttpUtils.encodeUtf8(record.getFileName()));
        Files.copy(file, response.getOutputStream());
    }

    private ErpImportExportRecordDO getActiveRecord(Long tenantId, Long operatorId) {
        ErpImportExportRecordDO record = asyncExportTaskSupport.findActiveRecord(MODULE_KEY);
        if (record == null) {
            return null;
        }
        if (markStaleTaskFailed(record)) {
            return null;
        }
        return record;
    }

    private boolean markStaleTaskFailed(ErpImportExportRecordDO record) {
        if (record == null
                || !ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())) {
            return false;
        }
        LocalDateTime heartbeat = record.getHeartbeatTime() == null ? record.getStartTime() : record.getHeartbeatTime();
        if (heartbeat == null || !heartbeat.isBefore(LocalDateTime.now().minus(STALE_TIMEOUT))) {
            return false;
        }
        finishFailure(record.getId(), "任务超过 30 分钟未更新进度，已自动结束，请重新导出",
                record.getStartTime() == null ? System.currentTimeMillis()
                        : System.currentTimeMillis()
                        - Duration.between(record.getStartTime(), LocalDateTime.now()).toMillis());
        return true;
    }

    private ErpImportExportRecordDO getOwnedRecord(Long taskId) {
        ErpImportExportRecordDO record = asyncExportTaskSupport.findOwnedRecord(taskId, MODULE_KEY);
        if (record == null) {
            throw exception(STOCK_EXPORT_TASK_NOT_EXISTS);
        }
        return record;
    }

    private Long createTaskRecord(ErpStockPageReqVO reqVO) {
        ErpImportExportRecordCreateReqBO createReqBO = new ErpImportExportRecordCreateReqBO();
        createReqBO.setOperationType(ErpImportExportOperationTypeEnum.EXPORT.getType());
        createReqBO.setModuleKey(MODULE_KEY);
        createReqBO.setModuleName(MODULE_NAME);
        createReqBO.setFileName(buildFileName());
        createReqBO.setFileType("XLSX");
        createReqBO.setStage(ErpStockExportTaskStageEnum.QUEUED.getStage());
        createReqBO.setProcessedCount(0);
        createReqBO.setQueryParams(JsonUtils.toJsonString(reqVO));
        createReqBO.setExportFields("默认导出字段");
        createReqBO.setStartTime(LocalDateTime.now());
        createReqBO.setHeartbeatTime(LocalDateTime.now());
        return importExportRecordService.createRecord(createReqBO);
    }

    private void executeTask(Long taskId, Long tenantId, Long operatorId, ErpStockPageReqVO reqVO,
                             ErpStockExportPageLoader pageLoader) {
        long startMillis = System.currentTimeMillis();
        Path partFile = partFile(tenantId, operatorId, taskId);
        Path finalFile = finalFile(tenantId, operatorId, taskId);
        List<Path> chunkFiles = new ArrayList<>();
        try {
            Files.createDirectories(partFile.getParent());
            Files.deleteIfExists(partFile);
            Files.deleteIfExists(finalFile);
            updateProgress(taskId, ErpStockExportTaskStageEnum.COUNTING.getStage(), 0, 0, 0, startMillis);
            ErpStockPageReqVO firstReqVO = pageRequest(reqVO, 1);
            PageResult<ErpStockRespVO> firstPage = pageLoader.load(firstReqVO);
            int totalCount = Math.toIntExact(firstPage.getTotal());
            ExportCounts counts;
            if (requiresExternalBatchMerge(firstReqVO)) {
                counts = writeSortedChunks(taskId, tenantId, operatorId, reqVO, pageLoader,
                        firstPage, totalCount, startMillis, chunkFiles);
                writeMergedChunks(partFile, chunkFiles, firstReqVO, taskId, totalCount,
                        counts, startMillis);
            } else {
                counts = writeDirectWorkbook(partFile, taskId, reqVO, pageLoader,
                        firstPage, totalCount, startMillis);
            }
            updateProgress(taskId, ErpStockExportTaskStageEnum.FINALIZING.getStage(), totalCount,
                    counts.processedCount, counts.exportedCount, startMillis);
            moveCompletedFile(partFile, finalFile);
            finishSuccess(taskId, totalCount, counts.processedCount, counts.exportedCount, startMillis);
        } catch (Throwable ex) {
            log.error("[executeTask][taskId({}) 产品库存导出失败]", taskId, ex);
            deleteQuietly(partFile);
            deleteQuietly(finalFile);
            finishFailure(taskId, briefErrorMessage(ex), startMillis);
        } finally {
            chunkFiles.forEach(this::deleteQuietly);
        }
    }

    private ExportCounts writeDirectWorkbook(Path partFile, Long taskId, ErpStockPageReqVO reqVO,
                                              ErpStockExportPageLoader pageLoader,
                                              PageResult<ErpStockRespVO> firstPage, int totalCount,
                                              long startMillis) throws IOException {
        int processedCount = 0;
        int exportedCount = 0;
        int pageNo = 1;
        try (OutputStream outputStream = Files.newOutputStream(partFile);
             ExcelWriter writer = createExcelWriter(outputStream)) {
            WriteSheet sheet = FastExcelFactory.writerSheet("产品库存").build();
            PageResult<ErpStockRespVO> page = firstPage;
            if (totalCount == 0) {
                writer.write(Collections.emptyList(), sheet);
            }
            while (totalCount > 0) {
                List<ErpStockRespVO> rows = page.getList();
                writer.write(rows, sheet);
                exportedCount += rows.size();
                processedCount = Math.min(totalCount, pageNo * EXPORT_PAGE_SIZE);
                updateProgress(taskId, ErpStockExportTaskStageEnum.EXPORTING.getStage(), totalCount,
                        processedCount, exportedCount, startMillis);
                if (processedCount >= totalCount) {
                    break;
                }
                pageNo++;
                page = pageLoader.load(pageRequest(reqVO, pageNo));
            }
        }
        return new ExportCounts(processedCount, exportedCount);
    }

    private ExportCounts writeSortedChunks(Long taskId, Long tenantId, Long operatorId,
                                            ErpStockPageReqVO reqVO, ErpStockExportPageLoader pageLoader,
                                            PageResult<ErpStockRespVO> firstPage, int totalCount,
                                            long startMillis, List<Path> chunkFiles) throws IOException {
        int processedCount = 0;
        int exportedCount = 0;
        int pageNo = 1;
        PageResult<ErpStockRespVO> page = firstPage;
        while (totalCount > 0) {
            List<ErpStockRespVO> rows = page.getList();
            Path chunk = chunkFile(tenantId, operatorId, taskId, pageNo);
            writeChunk(chunk, rows);
            chunkFiles.add(chunk);
            exportedCount += rows.size();
            processedCount = Math.min(totalCount, pageNo * EXPORT_PAGE_SIZE);
            updateProgress(taskId, ErpStockExportTaskStageEnum.EXPORTING.getStage(), totalCount,
                    processedCount, exportedCount, startMillis);
            if (processedCount >= totalCount) {
                break;
            }
            pageNo++;
            page = pageLoader.load(pageRequest(reqVO, pageNo));
        }
        return new ExportCounts(processedCount, exportedCount);
    }

    private ExcelWriter createExcelWriter(OutputStream outputStream) {
        return FastExcelFactory.write(outputStream, ErpStockRespVO.class)
                .autoCloseStream(false)
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                .registerConverter(new LongStringConverter())
                .build();
    }

    private ErpStockPageReqVO pageRequest(ErpStockPageReqVO source, int pageNo) {
        ErpStockPageReqVO reqVO = BeanUtils.toBean(source, ErpStockPageReqVO.class);
        reqVO.setPageNo(pageNo);
        reqVO.setPageSize(EXPORT_PAGE_SIZE);
        return reqVO;
    }

    boolean requiresExternalBatchMerge(ErpStockPageReqVO reqVO) {
        if (!Boolean.TRUE.equals(reqVO.getShowBatchNo())) {
            return false;
        }
        String orderField = reqVO.getOrderField();
        return "batchNo".equals(orderField)
                || "batchNoSummary".equals(orderField)
                || "count".equals(orderField)
                || "costAmount".equals(orderField)
                || "availableCount".equals(orderField)
                || "currentPriceAmount".equals(orderField);
    }

    private void writeChunk(Path chunkFile, List<ErpStockRespVO> rows) throws IOException {
        Files.deleteIfExists(chunkFile);
        try (BufferedWriter writer = Files.newBufferedWriter(chunkFile, StandardCharsets.UTF_8)) {
            for (ErpStockRespVO row : rows) {
                writer.write(JsonUtils.toJsonString(row));
                writer.newLine();
            }
        }
    }

    private void writeMergedChunks(Path partFile, List<Path> chunkFiles, ErpStockPageReqVO reqVO,
                                   Long taskId, int totalCount, ExportCounts counts,
                                   long startMillis) throws IOException {
        List<ChunkCursor> cursors = new ArrayList<>(chunkFiles.size());
        PriorityQueue<ChunkCursor> queue = new PriorityQueue<>(
                (first, second) -> compareBatchExportRows(first.current, second.current, reqVO));
        try {
            for (Path chunkFile : chunkFiles) {
                ChunkCursor cursor = new ChunkCursor(chunkFile);
                cursors.add(cursor);
                if (cursor.advance()) {
                    queue.offer(cursor);
                }
            }
            try (OutputStream outputStream = Files.newOutputStream(partFile);
                 ExcelWriter writer = createExcelWriter(outputStream)) {
                WriteSheet sheet = FastExcelFactory.writerSheet("产品库存").build();
                List<ErpStockRespVO> writeBuffer = new ArrayList<>(EXPORT_PAGE_SIZE);
                int mergedCount = 0;
                while (!queue.isEmpty()) {
                    ChunkCursor cursor = queue.poll();
                    writeBuffer.add(cursor.current);
                    mergedCount++;
                    if (writeBuffer.size() >= EXPORT_PAGE_SIZE) {
                        writer.write(writeBuffer, sheet);
                        writeBuffer.clear();
                    }
                    if (mergedCount % (EXPORT_PAGE_SIZE * 10) == 0) {
                        updateProgress(taskId, ErpStockExportTaskStageEnum.EXPORTING.getStage(), totalCount,
                                counts.processedCount, counts.exportedCount, startMillis);
                    }
                    if (cursor.advance()) {
                        queue.offer(cursor);
                    }
                }
                if (!writeBuffer.isEmpty() || chunkFiles.isEmpty()) {
                    writer.write(writeBuffer, sheet);
                }
            }
        } finally {
            for (ChunkCursor cursor : cursors) {
                cursor.close();
            }
        }
    }

    int compareBatchExportRows(ErpStockRespVO first, ErpStockRespVO second,
                               ErpStockPageReqVO reqVO) {
        Comparable<?> left = resolveBatchSortValue(first, reqVO.getOrderField());
        Comparable<?> right = resolveBatchSortValue(second, reqVO.getOrderField());
        int result = compareNullable(left, right);
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection()) && left != null && right != null) {
            result = -result;
        }
        if (result != 0) {
            return result;
        }
        return String.valueOf(first.getRowKey()).compareTo(String.valueOf(second.getRowKey()));
    }

    private Comparable<?> resolveBatchSortValue(ErpStockRespVO row, String field) {
        if ("batchNo".equals(field) || "batchNoSummary".equals(field)) {
            return row.getBatchNo() == null ? null : row.getBatchNo().toLowerCase(java.util.Locale.ROOT);
        }
        if ("count".equals(field)) {
            return row.getCount();
        }
        if ("costAmount".equals(field)) {
            return row.getCostAmount();
        }
        if ("availableCount".equals(field)) {
            return row.getAvailableCount();
        }
        if ("currentPriceAmount".equals(field)) {
            return row.getCurrentPriceAmount();
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int compareNullable(Comparable left, Comparable right) {
        if (left == null) {
            return right == null ? 0 : 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private void updateProgress(Long taskId, String stage, int totalCount, int processedCount,
                                int exportedCount, long startMillis) {
        ErpImportExportRecordProgressReqBO progressReqBO = new ErpImportExportRecordProgressReqBO();
        progressReqBO.setStage(stage);
        progressReqBO.setTotalCount(totalCount);
        progressReqBO.setProcessedCount(processedCount);
        progressReqBO.setSuccessCount(exportedCount);
        progressReqBO.setFailureCount(0);
        progressReqBO.setDurationMs(System.currentTimeMillis() - startMillis);
        progressReqBO.setHeartbeatTime(LocalDateTime.now());
        importExportRecordService.updateProgress(taskId, progressReqBO);
    }

    private void finishSuccess(Long taskId, int totalCount, int processedCount,
                               int exportedCount, long startMillis) {
        ErpImportExportRecordFinishReqBO finishReqBO = new ErpImportExportRecordFinishReqBO();
        finishReqBO.setStatus(ErpImportExportRecordStatusEnum.SUCCESS.getStatus());
        finishReqBO.setStage(ErpStockExportTaskStageEnum.FINISHED.getStage());
        finishReqBO.setTotalCount(totalCount);
        finishReqBO.setProcessedCount(processedCount);
        finishReqBO.setSuccessCount(exportedCount);
        finishReqBO.setFailureCount(0);
        finishReqBO.setCreateCount(0);
        finishReqBO.setUpdateCount(0);
        finishReqBO.setDurationMs(System.currentTimeMillis() - startMillis);
        finishReqBO.setEndTime(LocalDateTime.now());
        finishReqBO.setHeartbeatTime(LocalDateTime.now());
        importExportRecordService.finishRecord(taskId, finishReqBO);
    }

    private void finishFailure(Long taskId, String errorMessage, long startMillis) {
        ErpImportExportRecordFinishReqBO finishReqBO = new ErpImportExportRecordFinishReqBO();
        finishReqBO.setStatus(ErpImportExportRecordStatusEnum.FAILURE.getStatus());
        finishReqBO.setStage(ErpStockExportTaskStageEnum.FINISHED.getStage());
        finishReqBO.setTotalCount(0);
        finishReqBO.setProcessedCount(0);
        finishReqBO.setSuccessCount(0);
        finishReqBO.setFailureCount(1);
        finishReqBO.setCreateCount(0);
        finishReqBO.setUpdateCount(0);
        finishReqBO.setErrorMessage(errorMessage);
        finishReqBO.setDurationMs(Math.max(0L, System.currentTimeMillis() - startMillis));
        finishReqBO.setEndTime(LocalDateTime.now());
        finishReqBO.setHeartbeatTime(LocalDateTime.now());
        importExportRecordService.finishRecord(taskId, finishReqBO);
    }

    private ErpStockExportTaskRespVO buildTaskResponse(ErpImportExportRecordDO record) {
        ErpStockExportTaskRespVO response = new ErpStockExportTaskRespVO();
        response.setTaskId(record.getId());
        response.setStatus(record.getStatus());
        response.setStage(record.getStage());
        response.setStageName(ErpImportExportRecordStatusEnum.FAILURE.getStatus().equals(record.getStatus())
                ? "导出失败" : ErpStockExportTaskStageEnum.getName(record.getStage()));
        response.setProgressPercent(asyncExportTaskSupport.calculateProgress(record));
        response.setTotalCount(valueOrZero(record.getTotalCount()));
        response.setProcessedCount(valueOrZero(record.getProcessedCount()));
        response.setExportedCount(valueOrZero(record.getSuccessCount()));
        response.setFileName(record.getFileName());
        response.setOperatorName(record.getOperatorName());
        response.setStartTime(record.getStartTime());
        response.setEndTime(record.getEndTime());
        response.setDurationMs(asyncExportTaskSupport.resolveDuration(record));
        response.setErrorMessage(record.getErrorMessage());
        response.setExpiresAt(asyncExportTaskSupport.expiresAt(record));
        response.setDownloadAvailable(isDownloadAvailable(record));
        return response;
    }

    private boolean isDownloadAvailable(ErpImportExportRecordDO record) {
        return ErpImportExportRecordStatusEnum.SUCCESS.getStatus().equals(record.getStatus())
                && !isExpired(record)
                && Files.isRegularFile(finalFile(record.getTenantId(), record.getOperatorId(), record.getId()));
    }

    private boolean isExpired(ErpImportExportRecordDO record) {
        return asyncExportTaskSupport.isExpired(record);
    }

    private Path partFile(Long tenantId, Long operatorId, Long taskId) {
        return taskDirectory(tenantId, operatorId).resolve(taskId + ".xlsx.part");
    }

    private Path chunkFile(Long tenantId, Long operatorId, Long taskId, int pageNo) {
        return taskDirectory(tenantId, operatorId).resolve(taskId + ".chunk-" + pageNo + ".jsonl");
    }

    private Path finalFile(Long tenantId, Long operatorId, Long taskId) {
        return taskDirectory(tenantId, operatorId).resolve(taskId + ".xlsx");
    }

    private Path taskDirectory(Long tenantId, Long operatorId) {
        return asyncExportTaskSupport.taskDirectory(TEMP_DIRECTORY, tenantId, operatorId);
    }

    private void moveCompletedFile(Path source, Path target) throws IOException {
        asyncExportTaskSupport.moveCompletedFile(source, target);
    }

    private void cleanExpiredFiles() throws IOException {
        asyncExportTaskSupport.cleanExpiredFiles(TEMP_DIRECTORY);
    }

    private String buildFileName() {
        return "产品库存-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
    }

    private String briefErrorMessage(Throwable ex) {
        String message = ex == null ? null : ex.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "产品库存导出失败，请稍后重试";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private int valueOrZero(Integer value) {
        return asyncExportTaskSupport.valueOrZero(value);
    }

    private void deleteQuietly(Path path) {
        asyncExportTaskSupport.deleteQuietly(path);
    }

    private static final class ExportCounts {

        private final int processedCount;
        private final int exportedCount;

        private ExportCounts(int processedCount, int exportedCount) {
            this.processedCount = processedCount;
            this.exportedCount = exportedCount;
        }
    }

    private static final class ChunkCursor implements AutoCloseable {

        private final BufferedReader reader;
        private ErpStockRespVO current;

        private ChunkCursor(Path file) throws IOException {
            this.reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        }

        private boolean advance() throws IOException {
            String line = reader.readLine();
            if (line == null) {
                current = null;
                return false;
            }
            current = JsonUtils.parseObject(line, ErpStockRespVO.class);
            return true;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }

}
