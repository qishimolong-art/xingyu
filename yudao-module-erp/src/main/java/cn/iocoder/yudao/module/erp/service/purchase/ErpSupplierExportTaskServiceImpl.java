package cn.iocoder.yudao.module.erp.service.purchase;

import com.fasterxml.jackson.core.type.TypeReference;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.ErpSupplierExportSupport;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpAsyncExportTaskStageEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpAsyncExportTaskSupport;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_EXPORT_TASK_FILE_EXPIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_EXPORT_TASK_FILE_NOT_READY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_EXPORT_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_EXPORT_TASK_SUBMIT_FAILED;

@Service
@Slf4j
public class ErpSupplierExportTaskServiceImpl implements ErpSupplierExportTaskService {

    private static final String MODULE_KEY = "erp_supplier";
    private static final String MODULE_NAME = "供应商信息";
    private static final String LOCK_KEY_PREFIX = "erp:supplier-export-task:";
    private static final int EXPORT_PAGE_SIZE = 500;
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Path TEMP_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir"),
            "xingyu", "supplier-export");

    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpSupplierQueryService supplierQueryService;
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
    public void prepareDirectory() {
        try {
            Files.createDirectories(TEMP_DIRECTORY);
            taskSupport.cleanExpiredFiles(TEMP_DIRECTORY);
        } catch (Exception ex) {
            log.warn("[prepareDirectory][初始化供应商导出目录失败]", ex);
        }
    }

    @Scheduled(cron = "0 29 * * * ?")
    public void cleanExpiredFilesScheduled() {
        try {
            taskSupport.cleanExpiredFiles(TEMP_DIRECTORY);
        } catch (Exception ex) {
            log.warn("[cleanExpiredFilesScheduled][清理过期供应商导出文件失败]", ex);
        }
    }

    @Override
    public ErpSupplierExportTaskStartRespVO startTask(ErpSupplierPageReqVO reqVO, Set<String> fields) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorId == null) {
            throw new AccessDeniedException("未登录，不能创建供应商导出任务");
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + tenantId + ":" + operatorId);
        lock.lock();
        try {
            ErpImportExportRecordDO active = getActiveRecord();
            if (active != null) {
                return new ErpSupplierExportTaskStartRespVO(active.getId(), true);
            }
            ErpSupplierPageReqVO taskReqVO = BeanUtils.toBean(reqVO, ErpSupplierPageReqVO.class);
            taskReqVO.setPageNo(1);
            taskReqVO.setPageSize(EXPORT_PAGE_SIZE);
            Set<String> taskFields = new java.util.LinkedHashSet<>(fields);
            Long taskId = createTaskRecord(taskReqVO, taskFields);
            try {
                taskExecutor.execute(() -> executeTask(taskId, tenantId, operatorId, taskReqVO, taskFields));
            } catch (RuntimeException ex) {
                finishFailure(taskId, "后台导出任务队列繁忙，请稍后重试", System.currentTimeMillis());
                throw exception(SUPPLIER_EXPORT_TASK_SUBMIT_FAILED);
            }
            return new ErpSupplierExportTaskStartRespVO(taskId, false);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public ErpSupplierExportTaskRespVO getActiveTask() {
        ErpImportExportRecordDO record = getActiveRecord();
        return record == null ? null : buildTaskResponse(record);
    }

    @Override
    public ErpSupplierExportTaskRespVO getTask(Long taskId) {
        return buildTaskResponse(refreshStaleRecord(getOwnedRecord(taskId)));
    }

    @Override
    public PageResult<ErpSupplierExportTaskRespVO> getHistory(int pageNo, int pageSize) {
        PageResult<ErpImportExportRecordDO> page = taskSupport.getOwnedHistory(MODULE_KEY, pageNo, pageSize);
        List<ErpSupplierExportTaskRespVO> list = page.getList().stream()
                .map(this::refreshStaleRecord)
                .map(this::buildTaskResponse)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public void downloadTask(Long taskId, HttpServletResponse response) throws IOException {
        ErpImportExportRecordDO record = getOwnedRecord(taskId);
        if (!ErpImportExportRecordStatusEnum.SUCCESS.getStatus().equals(record.getStatus())) {
            throw exception(SUPPLIER_EXPORT_TASK_FILE_NOT_READY);
        }
        Path file = finalFile(record.getTenantId(), record.getOperatorId(), record.getId());
        if (taskSupport.isExpired(record) || !Files.isRegularFile(file)) {
            throw exception(SUPPLIER_EXPORT_TASK_FILE_EXPIRED);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(record.getFileName()));
        Files.copy(file, response.getOutputStream());
    }

    private ErpImportExportRecordDO getActiveRecord() {
        ErpImportExportRecordDO record = taskSupport.findActiveRecord(MODULE_KEY);
        if (record != null && taskSupport.isStale(record)) {
            finishFailure(record.getId(), "任务超过 30 分钟未更新进度，已自动结束，请重新导出",
                    record.getStartTime() == null ? System.currentTimeMillis()
                            : System.currentTimeMillis() - Duration.between(
                                    record.getStartTime(), LocalDateTime.now()).toMillis());
            return null;
        }
        return record;
    }

    private ErpImportExportRecordDO getOwnedRecord(Long taskId) {
        ErpImportExportRecordDO record = taskSupport.findOwnedRecord(taskId, MODULE_KEY);
        if (record == null) {
            throw exception(SUPPLIER_EXPORT_TASK_NOT_EXISTS);
        }
        return record;
    }

    private ErpImportExportRecordDO refreshStaleRecord(ErpImportExportRecordDO record) {
        if (!taskSupport.isStale(record)) {
            return record;
        }
        finishFailure(record.getId(), "任务超过 30 分钟未更新进度，已自动结束，请重新导出",
                record.getStartTime() == null ? System.currentTimeMillis()
                        : System.currentTimeMillis() - Duration.between(
                                record.getStartTime(), LocalDateTime.now()).toMillis());
        return recordMapper.selectById(record.getId());
    }

    private Long createTaskRecord(ErpSupplierPageReqVO reqVO, Set<String> fields) {
        ErpImportExportRecordCreateReqBO create = new ErpImportExportRecordCreateReqBO();
        create.setOperationType(ErpImportExportOperationTypeEnum.EXPORT.getType());
        create.setModuleKey(MODULE_KEY);
        create.setModuleName(MODULE_NAME);
        create.setFileName("供应商信息-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx");
        create.setFileType("XLSX");
        create.setStage(ErpAsyncExportTaskStageEnum.QUEUED.getStage());
        create.setProcessedCount(0);
        create.setQueryParams(JsonUtils.toJsonString(reqVO));
        create.setExportFields(JsonUtils.toJsonString(fields));
        create.setStartTime(LocalDateTime.now());
        create.setHeartbeatTime(LocalDateTime.now());
        return importExportRecordService.createRecord(create);
    }

    private void executeTask(Long taskId, Long tenantId, Long operatorId,
                             ErpSupplierPageReqVO reqVO, Set<String> fields) {
        long startMillis = System.currentTimeMillis();
        Path partFile = partFile(tenantId, operatorId, taskId);
        Path finalFile = finalFile(tenantId, operatorId, taskId);
        try {
            Files.createDirectories(partFile.getParent());
            taskSupport.deleteQuietly(partFile);
            taskSupport.deleteQuietly(finalFile);
            updateProgress(taskId, ErpAsyncExportTaskStageEnum.COUNTING.getStage(), 0, 0, 0, startMillis);
            PageResult<ErpSupplierDO> firstPage = supplierService.getSupplierPage(pageRequest(reqVO, 1));
            int totalCount = Math.toIntExact(firstPage.getTotal());
            int exportedCount = ErpSupplierExportSupport.write(partFile, fields, pageNo -> {
                List<ErpSupplierDO> rows = pageNo == 1 ? firstPage.getList()
                        : supplierService.getSupplierPage(pageRequest(reqVO, pageNo)).getList();
                List<ErpSupplierRespVO> responseRows = supplierQueryService.buildSupplierRespList(rows);
                return responseRows.stream().map(ErpSupplierExportSupport::toExportRow).collect(Collectors.toList());
            }, written -> updateProgress(taskId, ErpAsyncExportTaskStageEnum.EXPORTING.getStage(),
                    totalCount, Math.min(totalCount, written), written, startMillis));
            updateProgress(taskId, ErpAsyncExportTaskStageEnum.FINALIZING.getStage(),
                    totalCount, totalCount, exportedCount, startMillis);
            taskSupport.moveCompletedFile(partFile, finalFile);
            finishSuccess(taskId, totalCount, exportedCount, startMillis);
        } catch (Throwable ex) {
            log.error("[executeTask][taskId({}) 供应商信息导出失败]", taskId, ex);
            taskSupport.deleteQuietly(partFile);
            taskSupport.deleteQuietly(finalFile);
            finishFailure(taskId, briefErrorMessage(ex), startMillis);
        }
    }

    private ErpSupplierPageReqVO pageRequest(ErpSupplierPageReqVO source, int pageNo) {
        ErpSupplierPageReqVO request = BeanUtils.toBean(source, ErpSupplierPageReqVO.class);
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

    private ErpSupplierExportTaskRespVO buildTaskResponse(ErpImportExportRecordDO record) {
        ErpSupplierExportTaskRespVO response = new ErpSupplierExportTaskRespVO();
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
            return "供应商信息导出失败，请稍后重试";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

}
