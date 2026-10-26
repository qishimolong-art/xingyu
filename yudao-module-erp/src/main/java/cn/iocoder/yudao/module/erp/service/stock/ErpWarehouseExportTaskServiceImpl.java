package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.stock.ErpWarehouseExportSupport;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpAsyncExportTaskStageEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpAsyncExportTaskSupport;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordFinishReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordProgressReqBO;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_EXPORT_TASK_FILE_EXPIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_EXPORT_TASK_FILE_NOT_READY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_EXPORT_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_EXPORT_TASK_SUBMIT_FAILED;

@Service
@Slf4j
public class ErpWarehouseExportTaskServiceImpl implements ErpWarehouseExportTaskService {

    private static final String MODULE_KEY = "erp_warehouse";
    private static final String MODULE_NAME = "仓库信息";
    private static final String FIELD_PERMISSION_MODULE = "erp_warehouse";
    private static final String LOCK_KEY_PREFIX = "erp:warehouse-export-task:";
    private static final int EXPORT_PAGE_SIZE = 500;
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Path TEMP_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir"),
            "xingyu", "warehouse-export");

    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpWarehouseQueryService warehouseQueryService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private ErpImportExportRecordMapper recordMapper;
    @Resource
    private ErpAsyncExportTaskSupport taskSupport;
    @Resource
    private RedissonClient redissonClient;
    @Resource(name = "erpAsyncExportTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @PostConstruct
    public void cleanExpiredFilesOnStartup() {
        try {
            Files.createDirectories(TEMP_DIRECTORY);
            taskSupport.cleanExpiredFiles(TEMP_DIRECTORY);
        } catch (Exception ex) {
            log.warn("[cleanExpiredFilesOnStartup][清理过期仓库导出文件失败]", ex);
        }
    }

    @Scheduled(cron = "0 29 * * * ?")
    public void cleanExpiredFilesScheduled() {
        try {
            taskSupport.cleanExpiredFiles(TEMP_DIRECTORY);
        } catch (Exception ex) {
            log.warn("[cleanExpiredFilesScheduled][清理过期仓库导出文件失败]", ex);
        }
    }

    @Override
    public ErpWarehouseExportTaskStartRespVO startTask(ErpWarehousePageReqVO reqVO, Set<String> fields) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorId == null) {
            throw new AccessDeniedException("未登录，不能创建仓库导出任务");
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + tenantId + ":" + operatorId);
        lock.lock();
        try {
            ErpImportExportRecordDO active = getActiveRecord();
            if (active != null) {
                return new ErpWarehouseExportTaskStartRespVO(active.getId(), true);
            }
            ErpWarehousePageReqVO taskReqVO = BeanUtils.toBean(reqVO, ErpWarehousePageReqVO.class);
            taskReqVO.setPageNo(1);
            taskReqVO.setPageSize(EXPORT_PAGE_SIZE);
            Set<String> taskFields = new LinkedHashSet<>(fields);
            Long taskId = createTaskRecord(taskReqVO, taskFields);
            try {
                taskExecutor.execute(() -> executeTask(taskId, tenantId, operatorId, taskReqVO, taskFields));
            } catch (RuntimeException ex) {
                finishFailure(taskId, "后台导出任务队列繁忙，请稍后重试", System.currentTimeMillis());
                throw exception(WAREHOUSE_EXPORT_TASK_SUBMIT_FAILED);
            }
            return new ErpWarehouseExportTaskStartRespVO(taskId, false);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public ErpWarehouseExportTaskRespVO getActiveTask() {
        ErpImportExportRecordDO record = getActiveRecord();
        return record == null ? null : buildTaskResponse(record);
    }

    @Override
    public ErpWarehouseExportTaskRespVO getTask(Long taskId) {
        return buildTaskResponse(refreshStaleRecord(getOwnedRecord(taskId)));
    }

    @Override
    public PageResult<ErpWarehouseExportTaskRespVO> getHistory(int pageNo, int pageSize) {
        PageResult<ErpImportExportRecordDO> page = taskSupport.getOwnedHistory(MODULE_KEY, pageNo, pageSize);
        List<ErpWarehouseExportTaskRespVO> list = page.getList().stream()
                .map(this::refreshStaleRecord)
                .map(this::buildTaskResponse)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public void downloadTask(Long taskId, HttpServletResponse response) throws IOException {
        ErpImportExportRecordDO record = getOwnedRecord(taskId);
        if (!ErpImportExportRecordStatusEnum.SUCCESS.getStatus().equals(record.getStatus())) {
            throw exception(WAREHOUSE_EXPORT_TASK_FILE_NOT_READY);
        }
        Path file = finalFile(record.getTenantId(), record.getOperatorId(), record.getId());
        if (taskSupport.isExpired(record) || !Files.isRegularFile(file)) {
            throw exception(WAREHOUSE_EXPORT_TASK_FILE_EXPIRED);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(record.getFileName()));
        Files.copy(file, response.getOutputStream());
    }

    private ErpImportExportRecordDO getActiveRecord() {
        ErpImportExportRecordDO record = taskSupport.findActiveRecord(MODULE_KEY);
        if (record != null && taskSupport.isStale(record)) {
            finishFailure(record.getId(), "任务超过 30 分钟未更新进度，已自动结束，请重新导出",
                    System.currentTimeMillis() - elapsed(record));
            return null;
        }
        return record;
    }

    private ErpImportExportRecordDO getOwnedRecord(Long taskId) {
        ErpImportExportRecordDO record = taskSupport.findOwnedRecord(taskId, MODULE_KEY);
        if (record == null) {
            throw exception(WAREHOUSE_EXPORT_TASK_NOT_EXISTS);
        }
        return record;
    }

    private ErpImportExportRecordDO refreshStaleRecord(ErpImportExportRecordDO record) {
        if (!taskSupport.isStale(record)) {
            return record;
        }
        finishFailure(record.getId(), "任务超过 30 分钟未更新进度，已自动结束，请重新导出",
                System.currentTimeMillis() - elapsed(record));
        return recordMapper.selectById(record.getId());
    }

    private long elapsed(ErpImportExportRecordDO record) {
        return record.getStartTime() == null ? 0L
                : Math.max(0L, Duration.between(record.getStartTime(), LocalDateTime.now()).toMillis());
    }

    private Long createTaskRecord(ErpWarehousePageReqVO reqVO, Set<String> fields) {
        ErpImportExportRecordCreateReqBO create = new ErpImportExportRecordCreateReqBO();
        create.setOperationType(ErpImportExportOperationTypeEnum.EXPORT.getType());
        create.setModuleKey(MODULE_KEY);
        create.setModuleName(MODULE_NAME);
        create.setFileName("仓库信息-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx");
        create.setFileType("XLSX");
        create.setStage(ErpAsyncExportTaskStageEnum.QUEUED.getStage());
        create.setProcessedCount(0);
        create.setQueryParams(JsonUtils.toJsonString(reqVO));
        create.setExportFields(JsonUtils.toJsonString(new ArrayList<>(fields)));
        create.setStartTime(LocalDateTime.now());
        create.setHeartbeatTime(LocalDateTime.now());
        return importExportRecordService.createRecord(create);
    }

    private void executeTask(Long taskId, Long tenantId, Long operatorId,
                             ErpWarehousePageReqVO reqVO, Set<String> fields) {
        long startMillis = System.currentTimeMillis();
        Path partFile = partFile(tenantId, operatorId, taskId);
        Path finalFile = finalFile(tenantId, operatorId, taskId);
        try {
            Files.createDirectories(partFile.getParent());
            taskSupport.deleteQuietly(partFile);
            taskSupport.deleteQuietly(finalFile);
            updateProgress(taskId, ErpAsyncExportTaskStageEnum.COUNTING.getStage(), 0, 0, 0, startMillis);
            ErpWarehousePageReqVO firstReqVO = pageRequest(reqVO, 1);
            PageResult<ErpWarehouseDO> firstPage = warehouseService.getWarehousePage(firstReqVO);
            int totalCount = Math.toIntExact(firstPage.getTotal());
            int exportedCount = ErpWarehouseExportSupport.write(partFile, fields, pageNo -> {
                List<ErpWarehouseDO> rows = pageNo == 1 ? firstPage.getList()
                        : warehouseService.getWarehousePage(pageRequest(reqVO, pageNo)).getList();
                List<ErpWarehouseRespVO> responseRows = warehouseQueryService.buildWarehouseVOList(rows);
                fieldPermissionMasker.maskListColumns(FIELD_PERMISSION_MODULE, responseRows);
                return responseRows.stream().map(ErpWarehouseExportSupport::toExportRow)
                        .collect(Collectors.toList());
            }, written -> updateProgress(taskId, ErpAsyncExportTaskStageEnum.EXPORTING.getStage(),
                    totalCount, Math.min(totalCount, written), written, startMillis));
            updateProgress(taskId, ErpAsyncExportTaskStageEnum.FINALIZING.getStage(),
                    totalCount, totalCount, exportedCount, startMillis);
            taskSupport.moveCompletedFile(partFile, finalFile);
            finishSuccess(taskId, totalCount, exportedCount, startMillis);
        } catch (Throwable ex) {
            log.error("[executeTask][taskId({}) 仓库导出失败]", taskId, ex);
            taskSupport.deleteQuietly(partFile);
            taskSupport.deleteQuietly(finalFile);
            finishFailure(taskId, briefErrorMessage(ex), startMillis);
        }
    }

    private ErpWarehousePageReqVO pageRequest(ErpWarehousePageReqVO source, int pageNo) {
        ErpWarehousePageReqVO request = BeanUtils.toBean(source, ErpWarehousePageReqVO.class);
        request.setPageNo(pageNo);
        request.setPageSize(EXPORT_PAGE_SIZE);
        return request;
    }

    private void updateProgress(Long taskId, String stage, int totalCount, int processedCount,
                                int exportedCount, long startMillis) {
        ErpImportExportRecordProgressReqBO progress = new ErpImportExportRecordProgressReqBO();
        progress.setStage(stage);
        progress.setTotalCount(totalCount);
        progress.setProcessedCount(processedCount);
        progress.setSuccessCount(exportedCount);
        progress.setFailureCount(0);
        progress.setDurationMs(System.currentTimeMillis() - startMillis);
        progress.setHeartbeatTime(LocalDateTime.now());
        importExportRecordService.updateProgress(taskId, progress);
    }

    private void finishSuccess(Long taskId, int totalCount, int exportedCount, long startMillis) {
        ErpImportExportRecordFinishReqBO finish = new ErpImportExportRecordFinishReqBO();
        finish.setStatus(ErpImportExportRecordStatusEnum.SUCCESS.getStatus());
        finish.setStage(ErpAsyncExportTaskStageEnum.FINISHED.getStage());
        finish.setTotalCount(totalCount);
        finish.setProcessedCount(totalCount);
        finish.setSuccessCount(exportedCount);
        finish.setFailureCount(0);
        finish.setCreateCount(0);
        finish.setUpdateCount(0);
        finish.setDurationMs(System.currentTimeMillis() - startMillis);
        finish.setEndTime(LocalDateTime.now());
        finish.setHeartbeatTime(LocalDateTime.now());
        importExportRecordService.finishRecord(taskId, finish);
    }

    private void finishFailure(Long taskId, String errorMessage, long startMillis) {
        ErpImportExportRecordFinishReqBO finish = new ErpImportExportRecordFinishReqBO();
        finish.setStatus(ErpImportExportRecordStatusEnum.FAILURE.getStatus());
        finish.setStage(ErpAsyncExportTaskStageEnum.FINISHED.getStage());
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

    private ErpWarehouseExportTaskRespVO buildTaskResponse(ErpImportExportRecordDO record) {
        ErpWarehouseExportTaskRespVO response = new ErpWarehouseExportTaskRespVO();
        response.setTaskId(record.getId());
        response.setStatus(record.getStatus());
        response.setStage(record.getStage());
        response.setStageName(ErpImportExportRecordStatusEnum.FAILURE.getStatus().equals(record.getStatus())
                ? "导出失败" : ErpAsyncExportTaskStageEnum.getName(record.getStage()));
        response.setProgressPercent(taskSupport.calculateProgress(record));
        response.setTotalCount(taskSupport.valueOrZero(record.getTotalCount()));
        response.setProcessedCount(taskSupport.valueOrZero(record.getProcessedCount()));
        response.setExportedCount(taskSupport.valueOrZero(record.getSuccessCount()));
        response.setFileName(record.getFileName());
        response.setSelectedFields(parseSelectedFields(record.getExportFields()));
        response.setStartTime(record.getStartTime());
        response.setEndTime(record.getEndTime());
        response.setDurationMs(taskSupport.resolveDuration(record));
        response.setErrorMessage(record.getErrorMessage());
        response.setExpiresAt(taskSupport.expiresAt(record));
        response.setDownloadAvailable(isDownloadAvailable(record));
        return response;
    }

    private List<String> parseSelectedFields(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JsonUtils.parseObject(value, new TypeReference<List<String>>() { });
        } catch (RuntimeException ex) {
            return Collections.emptyList();
        }
    }

    private boolean isDownloadAvailable(ErpImportExportRecordDO record) {
        return ErpImportExportRecordStatusEnum.SUCCESS.getStatus().equals(record.getStatus())
                && !taskSupport.isExpired(record)
                && Files.isRegularFile(finalFile(record.getTenantId(), record.getOperatorId(), record.getId()));
    }

    private Path partFile(Long tenantId, Long operatorId, Long taskId) {
        return taskSupport.taskDirectory(TEMP_DIRECTORY, tenantId, operatorId).resolve(taskId + ".xlsx.part");
    }

    private Path finalFile(Long tenantId, Long operatorId, Long taskId) {
        return taskSupport.taskDirectory(TEMP_DIRECTORY, tenantId, operatorId).resolve(taskId + ".xlsx");
    }

    private String briefErrorMessage(Throwable ex) {
        Throwable current = ex;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current == null ? null : current.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "仓库导出失败，请稍后重试";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

}
