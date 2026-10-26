package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordFailureDetailExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseOrderDetailImportTaskStageEnum;
import cn.iocoder.yudao.module.erp.service.common.*;
import cn.iocoder.yudao.module.erp.service.common.bo.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Slf4j
public class ErpPurchaseOrderDetailImportTaskServiceImpl implements ErpPurchaseOrderDetailImportTaskService {
    public static final String MODULE_KEY = "erp_purchase_order_detail";
    private static final String MODULE_NAME = "采购订单明细";
    private static final String LOCK_PREFIX = "erp:purchase-order-detail-import-task:";
    private static final int CHUNK_SIZE = 500;
    private static final Duration RETENTION = Duration.ofHours(24);
    private static final Path TEMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "xingyu", "purchase-order-detail-import");

    @Resource private ErpPurchaseOrderService purchaseOrderService;
    @Resource(name = "erpImportExportRecordServiceImpl") private ErpImportExportRecordService recordService;
    @Resource private ErpImportExportRecordMapper recordMapper;
    @Resource private ErpAsyncImportTaskSupport taskSupport;
    @Resource private RedissonClient redissonClient;
    @Resource(name = "erpAsyncImportTaskExecutor") private ThreadPoolTaskExecutor executor;
    @Value("${yudao.erp.import.max-rows:3000}") private int maxRows;

    @PostConstruct public void prepare() { try { Files.createDirectories(TEMP_DIR); clean(); } catch (IOException ex) { log.warn("清理采购订单明细导入临时文件失败", ex); } }
    @Scheduled(cron = "0 41 * * * ?") public void scheduledClean() { try { clean(); } catch (IOException ex) { log.warn("清理采购订单明细导入临时文件失败", ex); } }

    @Override
    public ErpPurchaseOrderTaskStartRespVO startTask(MultipartFile file, String formSessionId) throws IOException {
        validateFile(file);
        if (formSessionId == null || formSessionId.trim().isEmpty() || formSessionId.length() > 100) {
            throw exception(PURCHASE_ORDER_DETAIL_IMPORT_TASK_SUBMIT_FAILED);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorId == null) throw new AccessDeniedException("未登录，不能解析采购订单明细");
        RLock lock = redissonClient.getLock(LOCK_PREFIX + tenantId + ":" + operatorId);
        lock.lock();
        try {
            ErpImportExportRecordDO active = refreshStale(taskSupport.findOwnedActiveRecord(MODULE_KEY));
            if (active != null) return new ErpPurchaseOrderTaskStartRespVO(active.getId(), true);
            Path stored = store(file);
            Long taskId = createRecord(file.getOriginalFilename(), formSessionId.trim());
            try {
                executor.execute(() -> execute(taskId, stored, file.getOriginalFilename(), formSessionId.trim()));
            } catch (RuntimeException ex) {
                Files.deleteIfExists(stored); finishFailure(taskId, "后台任务队列繁忙，请稍后重试", System.currentTimeMillis());
                throw exception(PURCHASE_ORDER_DETAIL_IMPORT_TASK_SUBMIT_FAILED);
            }
            return new ErpPurchaseOrderTaskStartRespVO(taskId, false);
        } finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
    }

    @Override public ErpPurchaseOrderDetailImportTaskRespVO getActiveTask(String formSessionId) {
        ErpImportExportRecordDO record = refreshStale(taskSupport.findOwnedActiveRecord(MODULE_KEY));
        if (record == null) return null;
        DetailMeta meta = parseMeta(record.getQueryParams());
        return Objects.equals(meta.getFormSessionId(), formSessionId) ? response(record, meta) : null;
    }
    @Override public ErpPurchaseOrderDetailImportTaskRespVO getTask(Long taskId) {
        ErpImportExportRecordDO record = owned(taskId); return response(refreshStale(record), parseMeta(record.getQueryParams()));
    }
    @Override public PageResult<ErpPurchaseOrderSaveReqVO.Item> getResult(Long taskId, int pageNo, int pageSize) throws IOException {
        ErpImportExportRecordDO record = owned(taskId);
        if (ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())) {
            throw exception(PURCHASE_ORDER_DETAIL_IMPORT_RESULT_NOT_READY);
        }
        DetailMeta meta = parseMeta(record.getQueryParams());
        if (isExpired(record)) throw exception(PURCHASE_ORDER_DETAIL_IMPORT_RESULT_EXPIRED);
        int safePage = Math.max(1, pageNo), safeSize = Math.max(1, Math.min(CHUNK_SIZE, pageSize));
        int start = (safePage - 1) * safeSize;
        if (start >= meta.getSuccessCount()) return new PageResult<>(Collections.emptyList(), (long) meta.getSuccessCount());
        int end = Math.min(meta.getSuccessCount(), start + safeSize);
        List<ErpPurchaseOrderSaveReqVO.Item> items = new ArrayList<>(safeSize);
        int firstChunk = start / CHUNK_SIZE + 1, lastChunk = (end - 1) / CHUNK_SIZE + 1;
        for (int chunk = firstChunk; chunk <= lastChunk; chunk++) {
            Path path = chunkFile(record, chunk);
            if (!Files.isRegularFile(path)) throw exception(PURCHASE_ORDER_DETAIL_IMPORT_RESULT_EXPIRED);
            List<ErpPurchaseOrderSaveReqVO.Item> values = JsonUtils.parseArray(
                    new String(Files.readAllBytes(path), StandardCharsets.UTF_8), ErpPurchaseOrderSaveReqVO.Item.class);
            int chunkStart = (chunk - 1) * CHUNK_SIZE;
            int from = Math.max(start, chunkStart) - chunkStart, to = Math.min(end, chunkStart + values.size()) - chunkStart;
            if (from < to) items.addAll(values.subList(from, to));
        }
        return new PageResult<>(items, (long) meta.getSuccessCount());
    }

    private void execute(Long taskId, Path stored, String name, String sessionId) {
        long started = System.currentTimeMillis();
        try {
            update(taskId, ErpPurchaseOrderDetailImportTaskStageEnum.PARSING.getStage(), sessionId, 0, 0, 0, 0, started);
            ErpStoredMultipartFile file = new ErpStoredMultipartFile(stored, name);
            int rowCount = ExcelUtils.countFirstSheetDataRows(file, 1);
            if (rowCount > maxRows) throw exception(ERP_IMPORT_ROW_LIMIT_EXCEEDED, rowCount, maxRows);
            List<ErpPurchaseOrderDetailImportExcelVO> rows = ExcelUtils.read(file, ErpPurchaseOrderDetailImportExcelVO.class);
            Progress progress = new Progress(taskId, sessionId, started);
            ErpPurchaseOrderImportRespVO result = purchaseOrderService.parseImportData(rows, progress);
            update(taskId, ErpPurchaseOrderDetailImportTaskStageEnum.FINALIZING.getStage(), sessionId,
                    rows.size(), rows.size(), result.getSuccessCount(), result.getFailureCount(), started);
            writeChunks(taskId, result.getItems());
            finishSuccess(taskId, rows, result, sessionId, started);
        } catch (Throwable ex) {
            log.error("[execute][taskId({}) 采购订单明细解析失败]", taskId, ex);
            deleteTaskFiles(taskId); finishFailure(taskId, brief(ex), started);
        } finally { ExcelUtils.clearLastOperation(); try { Files.deleteIfExists(stored); } catch (IOException ignored) { } }
    }

    private void writeChunks(Long taskId, List<ErpPurchaseOrderSaveReqVO.Item> items) throws IOException {
        Path dir = TEMP_DIR.resolve(String.valueOf(taskId)); Files.createDirectories(dir);
        for (int offset = 0, chunk = 1; offset < items.size(); offset += CHUNK_SIZE, chunk++) {
            List<ErpPurchaseOrderSaveReqVO.Item> values = items.subList(offset, Math.min(items.size(), offset + CHUNK_SIZE));
            Path part = dir.resolve(chunk + ".json.part"), target = dir.resolve(chunk + ".json");
            Files.write(part, JsonUtils.toJsonString(values).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void finishSuccess(Long taskId, List<ErpPurchaseOrderDetailImportExcelVO> rows,
                               ErpPurchaseOrderImportRespVO result, String sessionId, long started) {
        ErpImportExportRecordFinishReqBO finish = new ErpImportExportRecordFinishReqBO();
        finish.setTemplateKey(ErpPurchaseOrderDetailImportExcelVO.class.getName());
        finish.setStatus(ErpImportExportRecordStatusEnum.ofCount(result.getSuccessCount(), result.getFailureCount()));
        finish.setStage(ErpPurchaseOrderDetailImportTaskStageEnum.FINISHED.getStage()); finish.setTotalCount(rows.size());
        finish.setProcessedCount(rows.size()); finish.setSuccessCount(result.getSuccessCount()); finish.setFailureCount(result.getFailureCount());
        finish.setQueryParams(JsonUtils.toJsonString(new DetailMeta(sessionId, result.getSuccessCount())));
        finish.setDurationMs(System.currentTimeMillis() - started); finish.setEndTime(LocalDateTime.now()); finish.setHeartbeatTime(LocalDateTime.now());
        List<ErpImportExportFailureDetailBO> failures = new ArrayList<>();
        for (ErpPurchaseOrderImportRespVO.FailureItem failure : result.getFailureDetails()) {
            ErpImportExportFailureDetailBO detail = new ErpImportExportFailureDetailBO(); detail.setRowNo(failure.getRowNo());
            detail.setGroupKey("ROW-" + failure.getRowNo()); detail.setDetailType("FAILURE"); detail.setBizKey(failure.getProductCode());
            detail.setFailureReason(failure.getReason()); int index = failure.getRowNo() == null ? -1 : failure.getRowNo() - 2;
            if (index >= 0 && index < rows.size()) detail.setRawData(rows.get(index)); failures.add(detail);
        }
        finish.setFailureDetails(failures); recordService.finishRecord(taskId, finish);
    }

    private ErpPurchaseOrderDetailImportTaskRespVO response(ErpImportExportRecordDO record, DetailMeta meta) {
        ErpPurchaseOrderDetailImportTaskRespVO value = new ErpPurchaseOrderDetailImportTaskRespVO();
        value.setTaskId(record.getId()); value.setRecordId(record.getId()); value.setFormSessionId(meta.getFormSessionId());
        value.setStatus(record.getStatus()); value.setStage(record.getStage());
        value.setStageName(ErpImportExportRecordStatusEnum.FAILURE.getStatus().equals(record.getStatus()) ? "解析失败"
                : ErpPurchaseOrderDetailImportTaskStageEnum.getName(record.getStage()));
        value.setProgressPercent(progress(record)); value.setTotalCount(taskSupport.valueOrZero(record.getTotalCount()));
        value.setProcessedCount(taskSupport.valueOrZero(record.getProcessedCount())); value.setSuccessCount(taskSupport.valueOrZero(record.getSuccessCount()));
        value.setFailureCount(taskSupport.valueOrZero(record.getFailureCount())); value.setFileName(record.getFileName());
        value.setOperatorName(record.getOperatorName()); value.setStartTime(record.getStartTime()); value.setEndTime(record.getEndTime());
        value.setDurationMs(taskSupport.resolveDuration(record)); value.setErrorMessage(record.getErrorMessage());
        value.setExpiresAt(record.getEndTime() == null ? null : record.getEndTime().plus(RETENTION));
        value.setResultAvailable(!ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())
                && meta.getSuccessCount() > 0 && !isExpired(record) && Files.isDirectory(TEMP_DIR.resolve(String.valueOf(record.getId()))));
        if (!ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())) {
            List<ErpPurchaseOrderImportRespVO.FailureItem> failures = new ArrayList<>();
            for (ErpImportExportRecordFailureDetailExportRespVO detail : recordService.getFailureDetailList(record.getId())) {
                failures.add(new ErpPurchaseOrderImportRespVO.FailureItem(detail.getRowNo(), detail.getBizKey(), detail.getFailureReason()));
            }
            value.setFailureDetails(failures);
        }
        return value;
    }

    private void update(Long id, String stage, String session, int total, int processed, int success, int failure, long started) {
        ErpImportExportRecordProgressReqBO value = new ErpImportExportRecordProgressReqBO(); value.setStage(stage);
        value.setTotalCount(total); value.setProcessedCount(processed); value.setSuccessCount(success); value.setFailureCount(failure);
        value.setQueryParams(JsonUtils.toJsonString(new DetailMeta(session, success))); value.setDurationMs(System.currentTimeMillis() - started);
        value.setHeartbeatTime(LocalDateTime.now()); recordService.updateProgress(id, value);
    }
    private int progress(ErpImportExportRecordDO record) {
        if (!ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())) return 100;
        if (ErpPurchaseOrderDetailImportTaskStageEnum.PARSING.getStage().equals(record.getStage())) return 5;
        if (ErpPurchaseOrderDetailImportTaskStageEnum.MATCHING.getStage().equals(record.getStage())) {
            int total = taskSupport.valueOrZero(record.getTotalCount()); return total == 0 ? 10 : Math.min(90, 10 + taskSupport.valueOrZero(record.getProcessedCount()) * 80 / total);
        }
        if (ErpPurchaseOrderDetailImportTaskStageEnum.FINALIZING.getStage().equals(record.getStage())) return 95;
        return 0;
    }

    private class Progress implements ErpPurchaseOrderDetailImportProgressListener {
        private final Long id; private final String session; private final long started; private long last;
        Progress(Long id, String session, long started) { this.id = id; this.session = session; this.started = started; }
        @Override public void onProgress(int processed, int total, ErpPurchaseOrderImportRespVO result) {
            long now = System.currentTimeMillis(); if (processed < total && processed % 20 != 0 && now - last < 500) return; last = now;
            update(id, ErpPurchaseOrderDetailImportTaskStageEnum.MATCHING.getStage(), session, total, processed,
                    result.getSuccessCount(), result.getFailureCount(), started);
        }
    }

    private ErpImportExportRecordDO owned(Long id) { ErpImportExportRecordDO r = taskSupport.findOwnedRecord(id, MODULE_KEY);
        if (r == null) throw exception(PURCHASE_ORDER_DETAIL_IMPORT_TASK_NOT_EXISTS); return r; }
    private ErpImportExportRecordDO refreshStale(ErpImportExportRecordDO r) { if (r == null || !taskSupport.isStale(r)) return r;
        finishFailure(r.getId(), "任务超过30分钟未更新进度，已自动结束，请重新导入", System.currentTimeMillis()); return recordMapper.selectById(r.getId()); }
    private Long createRecord(String name, String session) { ErpImportExportRecordCreateReqBO c = new ErpImportExportRecordCreateReqBO();
        c.setOperationType(ErpImportExportOperationTypeEnum.IMPORT.getType()); c.setModuleKey(MODULE_KEY); c.setModuleName(MODULE_NAME);
        c.setTemplateKey(ErpPurchaseOrderDetailImportExcelVO.class.getName()); c.setFileName(name); c.setFileType(extension(name));
        c.setStage(ErpPurchaseOrderDetailImportTaskStageEnum.QUEUED.getStage()); c.setQueryParams(JsonUtils.toJsonString(new DetailMeta(session, 0)));
        c.setStartTime(LocalDateTime.now()); c.setHeartbeatTime(LocalDateTime.now()); return recordService.createRecord(c); }
    private void finishFailure(Long id, String message, long started) { ErpImportExportRecordFinishReqBO f = new ErpImportExportRecordFinishReqBO();
        f.setStatus(ErpImportExportRecordStatusEnum.FAILURE.getStatus()); f.setStage(ErpPurchaseOrderDetailImportTaskStageEnum.FINISHED.getStage());
        f.setFailureCount(1); f.setErrorMessage(message); f.setDurationMs(Math.max(0, System.currentTimeMillis() - started));
        f.setEndTime(LocalDateTime.now()); f.setHeartbeatTime(LocalDateTime.now()); recordService.finishRecord(id, f); }
    private Path store(MultipartFile file) throws IOException { Files.createDirectories(TEMP_DIR); Path p = Files.createTempFile(TEMP_DIR, "upload-", ".xlsx");
        try (InputStream in = file.getInputStream()) { Files.copy(in, p, StandardCopyOption.REPLACE_EXISTING); } return p; }
    private void validateFile(MultipartFile file) { String name = file == null ? "" : String.valueOf(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (file == null || file.isEmpty() || (!name.endsWith(".xls") && !name.endsWith(".xlsx"))) throw exception(PURCHASE_ORDER_DETAIL_IMPORT_TASK_SUBMIT_FAILED); }
    private String extension(String name) { int i = name == null ? -1 : name.lastIndexOf('.'); return i < 0 ? null : name.substring(i + 1); }
    private String brief(Throwable ex) { String m = ex.getMessage(); return m == null ? "采购订单明细解析失败" : m.substring(0, Math.min(500, m.length())); }
    private DetailMeta parseMeta(String value) { if (value == null) return new DetailMeta(); try { return JsonUtils.parseObject(value, DetailMeta.class); } catch (RuntimeException ex) { return new DetailMeta(); } }
    private boolean isExpired(ErpImportExportRecordDO r) { return r.getEndTime() != null && r.getEndTime().plus(RETENTION).isBefore(LocalDateTime.now()); }
    private Path chunkFile(ErpImportExportRecordDO r, int chunk) { return TEMP_DIR.resolve(String.valueOf(r.getId())).resolve(chunk + ".json"); }
    private void deleteTaskFiles(Long id) { Path dir = TEMP_DIR.resolve(String.valueOf(id)); if (!Files.isDirectory(dir)) return;
        try (java.util.stream.Stream<Path> s = Files.walk(dir)) { s.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) { } }); } catch (IOException ignored) { } }
    private void clean() throws IOException { if (!Files.isDirectory(TEMP_DIR)) return; LocalDateTime cutoff = LocalDateTime.now().minus(RETENTION);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(TEMP_DIR)) { for (Path p : stream) { LocalDateTime modified = LocalDateTime.ofInstant(Files.getLastModifiedTime(p).toInstant(), java.time.ZoneId.systemDefault()); if (modified.isBefore(cutoff)) { if (Files.isDirectory(p)) deleteTaskFiles(Long.valueOf(p.getFileName().toString())); else Files.deleteIfExists(p); } } } }

    @Data
    public static class DetailMeta {
        private String formSessionId; private int successCount;
        public DetailMeta() { }
        public DetailMeta(String formSessionId, int successCount) { this.formSessionId = formSessionId; this.successCount = successCount; }
    }
}
