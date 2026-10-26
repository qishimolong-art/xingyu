package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.product.ErpSimpleArchiveExportSupport;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryListReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductBrandDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpAsyncExportTaskStageEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpAsyncExportTaskSupport;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordFinishReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordProgressReqBO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SIMPLE_ARCHIVE_EXPORT_TASK_FILE_EXPIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SIMPLE_ARCHIVE_EXPORT_TASK_FILE_NOT_READY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SIMPLE_ARCHIVE_EXPORT_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SIMPLE_ARCHIVE_EXPORT_TASK_SUBMIT_FAILED;

@Service
@Slf4j
public class ErpSimpleArchiveExportTaskServiceImpl implements ErpSimpleArchiveExportTaskService {

    private static final String LOCK_KEY_PREFIX = "erp:simple-archive-export-task:";
    private static final int EXPORT_PAGE_SIZE = 500;
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Path TEMP_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir"),
            "xingyu", "simple-archive-export");

    @Resource private ErpProductCategoryService categoryService;
    @Resource private ErpProductBrandService brandService;
    @Resource private ErpProductUnitService unitService;
    @Resource private PermissionApi permissionApi;
    @Resource private ErpImportExportRecordService importExportRecordService;
    @Resource private ErpImportExportRecordMapper recordMapper;
    @Resource private ErpAsyncExportTaskSupport taskSupport;
    @Resource private RedissonClient redissonClient;
    @Resource(name = "erpAsyncExportTaskExecutor") private ThreadPoolTaskExecutor taskExecutor;

    @PostConstruct
    public void cleanExpiredFilesOnStartup() {
        cleanExpiredFiles();
    }

    @Scheduled(cron = "0 37 * * * ?")
    public void cleanExpiredFilesScheduled() {
        cleanExpiredFiles();
    }

    private void cleanExpiredFiles() {
        try {
            Files.createDirectories(TEMP_DIRECTORY);
            taskSupport.cleanExpiredFiles(TEMP_DIRECTORY);
        } catch (Exception ex) {
            log.warn("[cleanExpiredFiles][清理简单档案导出文件失败]", ex);
        }
    }

    @Override
    public ErpArchiveExportTaskStartRespVO startCategoryTask(ErpProductCategoryListReqVO reqVO) {
        ErpProductCategoryListReqVO request = BeanUtils.toBean(reqVO, ErpProductCategoryListReqVO.class);
        normalizeCategoryRequest(request);
        return startTask(ErpSimpleArchiveModule.PRODUCT_CATEGORY, request,
                (taskId, tenantId, operatorId, fields) ->
                        executeCategoryTask(taskId, tenantId, operatorId, fields, request));
    }

    @Override
    public ErpArchiveExportTaskStartRespVO startBrandTask(ErpProductBrandPageReqVO reqVO) {
        ErpProductBrandPageReqVO request = BeanUtils.toBean(reqVO, ErpProductBrandPageReqVO.class);
        normalizeBrandRequest(request);
        return startTask(ErpSimpleArchiveModule.PRODUCT_BRAND, request,
                (taskId, tenantId, operatorId, fields) ->
                        executeBrandTask(taskId, tenantId, operatorId, fields, request));
    }

    @Override
    public ErpArchiveExportTaskStartRespVO startUnitTask(ErpProductUnitPageReqVO reqVO) {
        ErpProductUnitPageReqVO request = BeanUtils.toBean(reqVO, ErpProductUnitPageReqVO.class);
        normalizeUnitRequest(request);
        return startTask(ErpSimpleArchiveModule.PRODUCT_UNIT, request,
                (taskId, tenantId, operatorId, fields) ->
                        executeUnitTask(taskId, tenantId, operatorId, fields, request));
    }

    private ErpArchiveExportTaskStartRespVO startTask(ErpSimpleArchiveModule module, Object request,
                                                       ExportExecutor executor) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorId == null) {
            throw new AccessDeniedException("未登录，不能创建" + module.getModuleName() + "导出任务");
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + module.getModuleKey() + ":" + tenantId + ":" + operatorId);
        lock.lock();
        try {
            ErpImportExportRecordDO active = getActiveRecord(module);
            if (active != null) {
                return new ErpArchiveExportTaskStartRespVO(active.getId(), true);
            }
            Set<String> fields = resolveVisibleFields(module);
            if (fields.isEmpty()) {
                throw new IllegalArgumentException("当前用户没有可导出的字段");
            }
            Long taskId = createTaskRecord(module, request, fields);
            try {
                taskExecutor.execute(() -> executor.execute(taskId, tenantId, operatorId, fields));
            } catch (RuntimeException ex) {
                finishFailure(taskId, "后台导出任务队列繁忙，请稍后重试", System.currentTimeMillis());
                throw exception(SIMPLE_ARCHIVE_EXPORT_TASK_SUBMIT_FAILED, module.getModuleName());
            }
            return new ErpArchiveExportTaskStartRespVO(taskId, false);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public ErpArchiveExportTaskRespVO getActiveTask(ErpSimpleArchiveModule module) {
        ErpImportExportRecordDO record = getActiveRecord(module);
        return record == null ? null : buildTaskResponse(module, record);
    }

    @Override
    public ErpArchiveExportTaskRespVO getTask(ErpSimpleArchiveModule module, Long taskId) {
        return buildTaskResponse(module, refreshStaleRecord(module, getOwnedRecord(module, taskId)));
    }

    @Override
    public PageResult<ErpArchiveExportTaskRespVO> getHistory(ErpSimpleArchiveModule module,
                                                            int pageNo, int pageSize) {
        PageResult<ErpImportExportRecordDO> page = taskSupport.getOwnedHistory(module.getModuleKey(), pageNo, pageSize);
        List<ErpArchiveExportTaskRespVO> list = page.getList().stream()
                .map(record -> refreshStaleRecord(module, record))
                .map(record -> buildTaskResponse(module, record))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public void downloadTask(ErpSimpleArchiveModule module, Long taskId,
                             HttpServletResponse response) throws IOException {
        ErpImportExportRecordDO record = getOwnedRecord(module, taskId);
        if (!ErpImportExportRecordStatusEnum.SUCCESS.getStatus().equals(record.getStatus())) {
            throw exception(SIMPLE_ARCHIVE_EXPORT_TASK_FILE_NOT_READY, module.getModuleName());
        }
        Path file = finalFile(module, record.getTenantId(), record.getOperatorId(), record.getId());
        if (taskSupport.isExpired(record) || !Files.isRegularFile(file)) {
            throw exception(SIMPLE_ARCHIVE_EXPORT_TASK_FILE_EXPIRED, module.getModuleName());
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(record.getFileName()));
        Files.copy(file, response.getOutputStream());
    }

    private void normalizeCategoryRequest(ErpProductCategoryListReqVO request) {
        request.setPageNo(1);
        request.setPageSize(EXPORT_PAGE_SIZE);
        request.setIds(normalizeIds(request.getIds()));
        if (request.getIds() != null) {
            request.setName(null);
            request.setStatus(null);
        }
    }

    private void normalizeBrandRequest(ErpProductBrandPageReqVO request) {
        request.setPageNo(1);
        request.setPageSize(EXPORT_PAGE_SIZE);
        request.setIds(normalizeIds(request.getIds()));
        if (request.getIds() != null) {
            request.setName(null);
            request.setStatus(null);
            request.setKeyword(null);
        }
    }

    private void normalizeUnitRequest(ErpProductUnitPageReqVO request) {
        request.setPageNo(1);
        request.setPageSize(EXPORT_PAGE_SIZE);
        request.setIds(normalizeIds(request.getIds()));
        if (request.getIds() != null) {
            request.setName(null);
            request.setStatus(null);
            request.setKeyword(null);
        }
    }

    private Set<Long> normalizeIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        Set<Long> result = ids.stream().filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return result.isEmpty() ? null : result;
    }

    private Set<String> resolveVisibleFields(ErpSimpleArchiveModule module) {
        List<String> hidden = permissionApi.getCurrentUserHiddenFields(module.getModuleKey());
        Set<String> hiddenSet = hidden == null ? Collections.emptySet() : new HashSet<>(hidden);
        return module.getExportFields().stream()
                .filter(field -> !hiddenSet.contains(field) && !hiddenSet.contains("col_" + field))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void executeCategoryTask(Long taskId, Long tenantId, Long operatorId,
                                     Set<String> fields, ErpProductCategoryListReqVO request) {
        executeTask(ErpSimpleArchiveModule.PRODUCT_CATEGORY, taskId, tenantId, operatorId, fields,
                pageNo -> {
                    ErpProductCategoryListReqVO pageReq = BeanUtils.toBean(request, ErpProductCategoryListReqVO.class);
                    pageReq.setPageNo(pageNo);
                    pageReq.setPageSize(EXPORT_PAGE_SIZE);
                    PageResult<ErpProductCategoryDO> page = categoryService.getProductCategoryPage(pageReq);
                    return new ExportPage(page.getTotal(), BeanUtils.toBean(page.getList(), ErpProductCategoryRespVO.class));
                });
    }

    private void executeBrandTask(Long taskId, Long tenantId, Long operatorId,
                                  Set<String> fields, ErpProductBrandPageReqVO request) {
        executeTask(ErpSimpleArchiveModule.PRODUCT_BRAND, taskId, tenantId, operatorId, fields,
                pageNo -> {
                    ErpProductBrandPageReqVO pageReq = BeanUtils.toBean(request, ErpProductBrandPageReqVO.class);
                    pageReq.setPageNo(pageNo);
                    pageReq.setPageSize(EXPORT_PAGE_SIZE);
                    PageResult<ErpProductBrandDO> page = brandService.getProductBrandPage(pageReq);
                    return new ExportPage(page.getTotal(), BeanUtils.toBean(page.getList(), ErpProductBrandRespVO.class));
                });
    }

    private void executeUnitTask(Long taskId, Long tenantId, Long operatorId,
                                 Set<String> fields, ErpProductUnitPageReqVO request) {
        executeTask(ErpSimpleArchiveModule.PRODUCT_UNIT, taskId, tenantId, operatorId, fields,
                pageNo -> {
                    ErpProductUnitPageReqVO pageReq = BeanUtils.toBean(request, ErpProductUnitPageReqVO.class);
                    pageReq.setPageNo(pageNo);
                    pageReq.setPageSize(EXPORT_PAGE_SIZE);
                    PageResult<ErpProductUnitDO> page = unitService.getProductUnitPage(pageReq);
                    return new ExportPage(page.getTotal(), BeanUtils.toBean(page.getList(), ErpProductUnitRespVO.class));
                });
    }

    private void executeTask(ErpSimpleArchiveModule module, Long taskId, Long tenantId, Long operatorId,
                             Set<String> fields, ExportPageLoader loader) {
        long startMillis = System.currentTimeMillis();
        Path partFile = partFile(module, tenantId, operatorId, taskId);
        Path finalFile = finalFile(module, tenantId, operatorId, taskId);
        try {
            Files.createDirectories(partFile.getParent());
            taskSupport.deleteQuietly(partFile);
            taskSupport.deleteQuietly(finalFile);
            updateProgress(taskId, ErpAsyncExportTaskStageEnum.COUNTING.getStage(), 0, 0, 0, startMillis);
            ExportPage firstPage = loader.load(1);
            int totalCount = Math.toIntExact(firstPage.total);
            int exportedCount = ErpSimpleArchiveExportSupport.write(partFile, module.getExportClass(),
                    module.getSheetName(), fields,
                    pageNo -> pageNo == 1 ? firstPage.rows : loader.load(pageNo).rows,
                    written -> updateProgress(taskId, ErpAsyncExportTaskStageEnum.EXPORTING.getStage(),
                            totalCount, Math.min(totalCount, written), written, startMillis));
            updateProgress(taskId, ErpAsyncExportTaskStageEnum.FINALIZING.getStage(),
                    totalCount, totalCount, exportedCount, startMillis);
            taskSupport.moveCompletedFile(partFile, finalFile);
            finishSuccess(taskId, totalCount, exportedCount, startMillis);
        } catch (Throwable ex) {
            log.error("[executeTask][module={} taskId={} 简单档案导出失败]", module.getModuleKey(), taskId, ex);
            taskSupport.deleteQuietly(partFile);
            taskSupport.deleteQuietly(finalFile);
            finishFailure(taskId, briefErrorMessage(ex), startMillis);
        }
    }

    private Long createTaskRecord(ErpSimpleArchiveModule module, Object request, Set<String> fields) {
        ErpImportExportRecordCreateReqBO create = new ErpImportExportRecordCreateReqBO();
        create.setOperationType(ErpImportExportOperationTypeEnum.EXPORT.getType());
        create.setModuleKey(module.getModuleKey());
        create.setModuleName(module.getModuleName());
        create.setFileName(module.getFileNamePrefix() + "-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx");
        create.setFileType("XLSX");
        create.setStage(ErpAsyncExportTaskStageEnum.QUEUED.getStage());
        create.setProcessedCount(0);
        create.setQueryParams(JsonUtils.toJsonString(request));
        create.setExportFields(JsonUtils.toJsonString(new ArrayList<>(fields)));
        create.setStartTime(LocalDateTime.now());
        create.setHeartbeatTime(LocalDateTime.now());
        return importExportRecordService.createRecord(create);
    }

    private ErpImportExportRecordDO getActiveRecord(ErpSimpleArchiveModule module) {
        ErpImportExportRecordDO record = taskSupport.findActiveRecord(module.getModuleKey());
        if (record != null && taskSupport.isStale(record)) {
            finishFailure(record.getId(), "任务超过 30 分钟未更新进度，已自动结束，请重新导出",
                    System.currentTimeMillis() - elapsed(record));
            return null;
        }
        return record;
    }

    private ErpImportExportRecordDO getOwnedRecord(ErpSimpleArchiveModule module, Long taskId) {
        ErpImportExportRecordDO record = taskSupport.findOwnedRecord(taskId, module.getModuleKey());
        if (record == null) {
            throw exception(SIMPLE_ARCHIVE_EXPORT_TASK_NOT_EXISTS, module.getModuleName());
        }
        return record;
    }

    private ErpImportExportRecordDO refreshStaleRecord(ErpSimpleArchiveModule module,
                                                       ErpImportExportRecordDO record) {
        if (!taskSupport.isStale(record)) {
            return record;
        }
        finishFailure(record.getId(), "任务超过 30 分钟未更新进度，已自动结束，请重新导出",
                System.currentTimeMillis() - elapsed(record));
        log.warn("[refreshStaleRecord][module={} taskId={} 导出任务超时]", module.getModuleKey(), record.getId());
        return recordMapper.selectById(record.getId());
    }

    private long elapsed(ErpImportExportRecordDO record) {
        return record.getStartTime() == null ? 0L
                : Math.max(0L, Duration.between(record.getStartTime(), LocalDateTime.now()).toMillis());
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

    private ErpArchiveExportTaskRespVO buildTaskResponse(ErpSimpleArchiveModule module,
                                                         ErpImportExportRecordDO record) {
        ErpArchiveExportTaskRespVO response = new ErpArchiveExportTaskRespVO();
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
        response.setDownloadAvailable(isDownloadAvailable(module, record));
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

    private boolean isDownloadAvailable(ErpSimpleArchiveModule module, ErpImportExportRecordDO record) {
        return ErpImportExportRecordStatusEnum.SUCCESS.getStatus().equals(record.getStatus())
                && !taskSupport.isExpired(record)
                && Files.isRegularFile(finalFile(module, record.getTenantId(), record.getOperatorId(), record.getId()));
    }

    private Path partFile(ErpSimpleArchiveModule module, Long tenantId, Long operatorId, Long taskId) {
        return taskSupport.taskDirectory(TEMP_DIRECTORY.resolve(module.getModuleKey()), tenantId, operatorId)
                .resolve(taskId + ".xlsx.part");
    }

    private Path finalFile(ErpSimpleArchiveModule module, Long tenantId, Long operatorId, Long taskId) {
        return taskSupport.taskDirectory(TEMP_DIRECTORY.resolve(module.getModuleKey()), tenantId, operatorId)
                .resolve(taskId + ".xlsx");
    }

    private String briefErrorMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = current.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    @FunctionalInterface
    private interface ExportExecutor {
        void execute(Long taskId, Long tenantId, Long operatorId, Set<String> fields);
    }

    @FunctionalInterface
    private interface ExportPageLoader {
        ExportPage load(int pageNo);
    }

    private static class ExportPage {
        private final long total;
        private final List<?> rows;

        private ExportPage(long total, List<?> rows) {
            this.total = total;
            this.rows = rows;
        }
    }

}
