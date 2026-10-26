package cn.iocoder.yudao.module.erp.service.purchase;

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
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseOrderImportTaskStageEnum;
import cn.iocoder.yudao.module.erp.service.common.*;
import cn.iocoder.yudao.module.erp.service.common.bo.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Slf4j
public class ErpPurchaseOrderImportTaskServiceImpl implements ErpPurchaseOrderImportTaskService {
    public static final String MODULE_KEY = "erp_purchase_order";
    private static final String MODULE_NAME = "采购订单";
    private static final String LOCK_PREFIX = "erp:purchase-order-import-task:";
    private static final Path TEMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "xingyu", "purchase-order-import");

    @Resource private ErpPurchaseOrderService purchaseOrderService;
    @Resource(name = "erpImportExportRecordServiceImpl") private ErpImportExportRecordService recordService;
    @Resource private ErpImportExportRecordMapper recordMapper;
    @Resource private ErpAsyncImportTaskSupport taskSupport;
    @Resource private RedissonClient redissonClient;
    @Resource(name = "erpAsyncImportTaskExecutor") private ThreadPoolTaskExecutor executor;
    @Value("${yudao.erp.import.max-rows:3000}") private int maxRows;

    @Override
    public ErpPurchaseOrderTaskStartRespVO startTask(MultipartFile file) throws IOException {
        validateFile(file);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorId == null) throw new AccessDeniedException("未登录，不能创建采购订单导入任务");
        RLock lock = redissonClient.getLock(LOCK_PREFIX + tenantId);
        lock.lock();
        try {
            ErpImportExportRecordDO active = refreshStale(taskSupport.findTenantActiveRecord(MODULE_KEY));
            if (active != null) {
                if (operatorId.equals(active.getOperatorId())) return new ErpPurchaseOrderTaskStartRespVO(active.getId(), true);
                throw exception(PURCHASE_ORDER_IMPORT_TASK_BUSY,
                        active.getOperatorName() == null ? "其他用户" : active.getOperatorName());
            }
            Path stored = store(file);
            Long taskId = createRecord(file.getOriginalFilename());
            try {
                executor.execute(() -> execute(taskId, stored, file.getOriginalFilename()));
            } catch (RuntimeException ex) {
                Files.deleteIfExists(stored);
                finishFailure(taskId, "后台任务队列繁忙，请稍后重试", System.currentTimeMillis());
                throw exception(PURCHASE_ORDER_IMPORT_TASK_SUBMIT_FAILED);
            }
            return new ErpPurchaseOrderTaskStartRespVO(taskId, false);
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    @Override public ErpPurchaseOrderImportTaskRespVO getActiveTask() {
        ErpImportExportRecordDO record = refreshStale(taskSupport.findOwnedActiveRecord(MODULE_KEY));
        return record == null ? null : response(record);
    }

    @Override public ErpPurchaseOrderImportTaskRespVO getTask(Long taskId) {
        ErpImportExportRecordDO record = taskSupport.findOwnedRecord(taskId, MODULE_KEY);
        if (record == null) throw exception(PURCHASE_ORDER_IMPORT_TASK_NOT_EXISTS);
        return response(refreshStale(record));
    }

    private void execute(Long taskId, Path stored, String name) {
        long started = System.currentTimeMillis();
        try {
            update(taskId, ErpPurchaseOrderImportTaskStageEnum.PARSING.getStage(), new ImportMeta(), 0, 0, 0, started);
            ErpStoredMultipartFile file = new ErpStoredMultipartFile(stored, name);
            int rowCount = ExcelUtils.countFirstSheetDataRows(file, 1);
            if (rowCount > maxRows) throw exception(ERP_IMPORT_ROW_LIMIT_EXCEEDED, rowCount, maxRows);
            List<ErpPurchaseOrderImportExcelVO> rows = ExcelUtils.read(file, ErpPurchaseOrderImportExcelVO.class);
            ImportMeta meta = new ImportMeta();
            meta.setScannedRowCount(rowCount);
            update(taskId, ErpPurchaseOrderImportTaskStageEnum.VALIDATING.getStage(), meta, 0, 0, 0, started);
            ErpPurchaseOrderImportResultRespVO result = purchaseOrderService.importPurchaseOrderList(rows, value -> {
                ImportMeta progressMeta = ImportMeta.from(value);
                update(taskId, ErpPurchaseOrderImportTaskStageEnum.IMPORTING.getStage(), progressMeta,
                        value.getSuccessCount(), value.getFailureCount(), value.getSuccessCount(), started);
            });
            update(taskId, ErpPurchaseOrderImportTaskStageEnum.FINALIZING.getStage(), ImportMeta.from(result),
                    result.getSuccessCount(), result.getFailureCount(), result.getSuccessCount(), started);
            finishSuccess(taskId, rows, result, started);
        } catch (Throwable ex) {
            log.error("[execute][taskId({}) 采购订单导入失败]", taskId, ex);
            finishFailure(taskId, brief(ex), started);
        } finally {
            ExcelUtils.clearLastOperation();
            try { Files.deleteIfExists(stored); } catch (IOException ignored) { }
        }
    }

    private void finishSuccess(Long taskId, List<ErpPurchaseOrderImportExcelVO> rows,
                               ErpPurchaseOrderImportResultRespVO result, long started) {
        ErpImportExportRecordFinishReqBO finish = new ErpImportExportRecordFinishReqBO();
        finish.setTemplateKey(ErpPurchaseOrderImportExcelVO.class.getName());
        finish.setStatus(ErpImportExportRecordStatusEnum.ofCount(result.getSuccessCount(), result.getFailureCount()));
        finish.setStage(ErpPurchaseOrderImportTaskStageEnum.FINISHED.getStage());
        finish.setTotalCount(result.getTotalOrderCount());
        finish.setProcessedCount(result.getProcessedOrderCount());
        finish.setSuccessCount(result.getSuccessCount());
        finish.setFailureCount(result.getFailureCount());
        finish.setCreateCount(result.getSuccessCount());
        finish.setUpdateCount(result.getFailureOrderCount());
        finish.setQueryParams(JsonUtils.toJsonString(ImportMeta.from(result)));
        finish.setDurationMs(System.currentTimeMillis() - started);
        finish.setEndTime(LocalDateTime.now());
        finish.setHeartbeatTime(LocalDateTime.now());
        finish.setFailureDetails(buildFailureDetails(rows, result.getFailureDetails()));
        recordService.finishRecord(taskId, finish);
    }

    private List<ErpImportExportFailureDetailBO> buildFailureDetails(List<ErpPurchaseOrderImportExcelVO> rows,
            List<ErpPurchaseOrderImportResultRespVO.FailureItem> failures) {
        if (failures == null || failures.isEmpty()) return Collections.emptyList();
        Map<Integer, List<ErpPurchaseOrderImportResultRespVO.FailureItem>> byRow = new HashMap<>();
        Set<String> failedGroups = new HashSet<>();
        for (ErpPurchaseOrderImportResultRespVO.FailureItem failure : failures) {
            if (failure.getRowNo() != null) byRow.computeIfAbsent(failure.getRowNo(), key -> new ArrayList<>()).add(failure);
            if (failure.getOrderNo() != null) failedGroups.add(failure.getOrderNo());
        }
        List<ErpImportExportFailureDetailBO> result = new ArrayList<>();
        String currentGroup = null;
        for (int index = 0; index < rows.size(); index++) {
            int rowNo = index + 2;
            ErpPurchaseOrderImportExcelVO row = rows.get(index);
            if (hasMain(row)) currentGroup = orderKey(rowNo, row);
            List<ErpPurchaseOrderImportResultRespVO.FailureItem> rowFailures = byRow.get(rowNo);
            if (!failedGroups.contains(currentGroup) && rowFailures == null) continue;
            ErpImportExportFailureDetailBO detail = new ErpImportExportFailureDetailBO();
            detail.setRowNo(rowNo);
            detail.setGroupKey(currentGroup == null ? "ROW-" + rowNo : currentGroup);
            detail.setDetailType(rowFailures == null ? "CONTEXT" : "FAILURE");
            detail.setBizKey(row.getProductCode());
            detail.setFailureReason(rowFailures == null ? "" : rowFailures.stream()
                    .map(ErpPurchaseOrderImportResultRespVO.FailureItem::getReason).filter(Objects::nonNull)
                    .reduce((a, b) -> a + "；" + b).orElse("导入失败"));
            detail.setRawData(row);
            result.add(detail);
        }
        return result;
    }

    private boolean hasMain(ErpPurchaseOrderImportExcelVO row) {
        return hasText(row.getSupplierName()) || hasText(row.getFactoryOrderNo()) || hasText(row.getRemark());
    }
    private String orderKey(int rowNo, ErpPurchaseOrderImportExcelVO row) {
        return hasText(row.getFactoryOrderNo()) ? row.getFactoryOrderNo().trim() : "row " + rowNo;
    }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }

    private void update(Long taskId, String stage, ImportMeta meta, int success, int failure, int created, long started) {
        ErpImportExportRecordProgressReqBO progress = new ErpImportExportRecordProgressReqBO();
        progress.setStage(stage);
        progress.setTotalCount(meta.getTotalOrderCount());
        progress.setProcessedCount(meta.getProcessedOrderCount());
        progress.setSuccessCount(success);
        progress.setFailureCount(failure);
        progress.setCreateCount(created);
        progress.setUpdateCount(meta.getFailureOrderCount());
        progress.setQueryParams(JsonUtils.toJsonString(meta));
        progress.setDurationMs(System.currentTimeMillis() - started);
        progress.setHeartbeatTime(LocalDateTime.now());
        recordService.updateProgress(taskId, progress);
    }

    private ErpPurchaseOrderImportTaskRespVO response(ErpImportExportRecordDO record) {
        ErpPurchaseOrderImportTaskRespVO response = new ErpPurchaseOrderImportTaskRespVO();
        response.setTaskId(record.getId()); response.setRecordId(record.getId()); response.setStatus(record.getStatus());
        response.setStage(record.getStage());
        response.setStageName(ErpImportExportRecordStatusEnum.FAILURE.getStatus().equals(record.getStatus())
                ? "导入失败" : ErpPurchaseOrderImportTaskStageEnum.getName(record.getStage()));
        response.setProgressPercent(progress(record));
        response.setTotalCount(taskSupport.valueOrZero(record.getTotalCount()));
        response.setProcessedCount(taskSupport.valueOrZero(record.getProcessedCount()));
        response.setSuccessCount(taskSupport.valueOrZero(record.getSuccessCount()));
        response.setFailureCount(taskSupport.valueOrZero(record.getFailureCount()));
        response.setCreateCount(taskSupport.valueOrZero(record.getCreateCount()));
        ImportMeta meta = parseMeta(record.getQueryParams());
        response.setTotalOrderCount(meta.getTotalOrderCount()); response.setProcessedOrderCount(meta.getProcessedOrderCount());
        response.setFailureOrderCount(meta.getFailureOrderCount()); response.setScannedRowCount(meta.getScannedRowCount());
        response.setFileName(record.getFileName()); response.setOperatorName(record.getOperatorName());
        response.setStartTime(record.getStartTime()); response.setEndTime(record.getEndTime());
        response.setDurationMs(taskSupport.resolveDuration(record)); response.setErrorMessage(record.getErrorMessage());
        if (!ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())) {
            List<ErpPurchaseOrderImportResultRespVO.FailureItem> failures = new ArrayList<>();
            for (ErpImportExportRecordFailureDetailExportRespVO detail : recordService.getFailureDetailList(record.getId())) {
                failures.add(new ErpPurchaseOrderImportResultRespVO.FailureItem(detail.getRowNo(), null,
                        detail.getGroupKey(), detail.getBizKey(), detail.getFailureReason()));
            }
            response.setFailureDetails(failures);
        }
        return response;
    }

    private int progress(ErpImportExportRecordDO record) {
        if (!ErpImportExportRecordStatusEnum.PROCESSING.getStatus().equals(record.getStatus())) return 100;
        String stage = record.getStage();
        if (ErpPurchaseOrderImportTaskStageEnum.PARSING.getStage().equals(stage)) return 5;
        if (ErpPurchaseOrderImportTaskStageEnum.VALIDATING.getStage().equals(stage)) return 15;
        if (ErpPurchaseOrderImportTaskStageEnum.IMPORTING.getStage().equals(stage)) {
            int total = taskSupport.valueOrZero(record.getTotalCount());
            return total == 0 ? 20 : Math.min(90, 20 + taskSupport.valueOrZero(record.getProcessedCount()) * 70 / total);
        }
        if (ErpPurchaseOrderImportTaskStageEnum.FINALIZING.getStage().equals(stage)) return 95;
        return 0;
    }

    private ErpImportExportRecordDO refreshStale(ErpImportExportRecordDO record) {
        if (record == null || !taskSupport.isStale(record)) return record;
        finishFailure(record.getId(), "任务超过30分钟未更新进度，已自动结束，请重新导入", System.currentTimeMillis());
        return recordMapper.selectById(record.getId());
    }
    private void finishFailure(Long taskId, String message, long started) {
        ErpImportExportRecordFinishReqBO finish = new ErpImportExportRecordFinishReqBO();
        finish.setStatus(ErpImportExportRecordStatusEnum.FAILURE.getStatus());
        finish.setStage(ErpPurchaseOrderImportTaskStageEnum.FINISHED.getStage()); finish.setErrorMessage(message);
        finish.setFailureCount(1); finish.setDurationMs(Math.max(0, System.currentTimeMillis() - started));
        finish.setEndTime(LocalDateTime.now()); finish.setHeartbeatTime(LocalDateTime.now());
        recordService.finishRecord(taskId, finish);
    }
    private Long createRecord(String fileName) {
        ErpImportExportRecordCreateReqBO create = new ErpImportExportRecordCreateReqBO();
        create.setOperationType(ErpImportExportOperationTypeEnum.IMPORT.getType()); create.setModuleKey(MODULE_KEY);
        create.setModuleName(MODULE_NAME); create.setTemplateKey(ErpPurchaseOrderImportExcelVO.class.getName());
        create.setFileName(fileName); create.setFileType(extension(fileName));
        create.setStage(ErpPurchaseOrderImportTaskStageEnum.QUEUED.getStage()); create.setProcessedCount(0);
        create.setStartTime(LocalDateTime.now()); create.setHeartbeatTime(LocalDateTime.now());
        return recordService.createRecord(create);
    }
    private Path store(MultipartFile file) throws IOException {
        Files.createDirectories(TEMP_DIR); String suffix = ".xlsx";
        String name = file.getOriginalFilename(); if (name != null && name.toLowerCase(Locale.ROOT).endsWith(".xls")) suffix = ".xls";
        Path target = Files.createTempFile(TEMP_DIR, "purchase-order-", suffix);
        try (InputStream in = file.getInputStream()) { Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING); }
        return target;
    }
    private void validateFile(MultipartFile file) {
        String name = file == null ? "" : String.valueOf(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (file == null || file.isEmpty() || (!name.endsWith(".xls") && !name.endsWith(".xlsx"))) {
            throw exception(PURCHASE_ORDER_IMPORT_TASK_SUBMIT_FAILED);
        }
    }
    private String extension(String name) { int i = name == null ? -1 : name.lastIndexOf('.'); return i < 0 ? null : name.substring(i + 1); }
    private String brief(Throwable ex) { String value = ex.getMessage(); return value == null ? "采购订单导入失败" : value.substring(0, Math.min(500, value.length())); }
    private ImportMeta parseMeta(String json) {
        if (json == null || json.isEmpty()) return new ImportMeta();
        try { return JsonUtils.parseObject(json, ImportMeta.class); } catch (RuntimeException ex) { return new ImportMeta(); }
    }

    @Data
    public static class ImportMeta {
        private int totalOrderCount;
        private int processedOrderCount;
        private int failureOrderCount;
        private int scannedRowCount;
        static ImportMeta from(ErpPurchaseOrderImportResultRespVO result) {
            ImportMeta value = new ImportMeta(); value.totalOrderCount = result.getTotalOrderCount();
            value.processedOrderCount = result.getProcessedOrderCount(); value.failureOrderCount = result.getFailureOrderCount();
            value.scannedRowCount = result.getScannedRowCount(); return value;
        }
    }
}
