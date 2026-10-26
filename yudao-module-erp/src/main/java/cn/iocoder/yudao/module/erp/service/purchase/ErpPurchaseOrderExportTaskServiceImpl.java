package cn.iocoder.yudao.module.erp.service.purchase;

import cn.idev.excel.*;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.*;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.*;
import cn.iocoder.yudao.module.erp.service.common.*;
import cn.iocoder.yudao.module.erp.service.common.bo.*;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Slf4j
public class ErpPurchaseOrderExportTaskServiceImpl implements ErpPurchaseOrderExportTaskService {
    private static final String MODULE_KEY = "erp_purchase_order";
    private static final String LOCK_PREFIX = "erp:purchase-order-export-task:";
    private static final int PAGE_SIZE = 500;
    private static final Path TEMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "xingyu", "purchase-order-export");
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    @Resource private ErpPurchaseOrderService orderService;
    @Resource private ErpPurchaseOrderExportAssembler assembler;
    @Resource(name = "erpImportExportRecordServiceImpl") private ErpImportExportRecordService recordService;
    @Resource private ErpImportExportRecordMapper recordMapper;
    @Resource private ErpAsyncExportTaskSupport taskSupport;
    @Resource private RedissonClient redissonClient;
    @Resource(name = "erpAsyncExportTaskExecutor") private ThreadPoolTaskExecutor executor;

    @PostConstruct public void prepare() { try { Files.createDirectories(TEMP_DIR); taskSupport.cleanExpiredFiles(TEMP_DIR); } catch (IOException ex) { log.warn("初始化采购订单导出目录失败", ex); } }
    @Scheduled(cron = "0 47 * * * ?") public void clean() { try { taskSupport.cleanExpiredFiles(TEMP_DIR); } catch (IOException ex) { log.warn("清理采购订单导出文件失败", ex); } }

    @Override public ErpPurchaseOrderTaskStartRespVO startTask(ErpPurchaseOrderPageReqVO reqVO, Set<String> fields) {
        Long tenantId = TenantContextHolder.getRequiredTenantId(), operatorId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorId == null) throw new AccessDeniedException("未登录，不能创建采购订单导出任务");
        RLock lock = redissonClient.getLock(LOCK_PREFIX + tenantId + ":" + operatorId); lock.lock();
        try {
            ErpImportExportRecordDO active = active(); if (active != null) return new ErpPurchaseOrderTaskStartRespVO(active.getId(), true);
            ErpPurchaseOrderPageReqVO request = BeanUtils.toBean(reqVO, ErpPurchaseOrderPageReqVO.class);
            request.setPageNo(1); request.setPageSize(PAGE_SIZE); request.setIncludeItems(false);
            Set<String> selected = new LinkedHashSet<>(fields); Long id = createRecord(request, selected);
            try { executor.execute(() -> execute(id, tenantId, operatorId, request, selected)); }
            catch (RuntimeException ex) { finishFailure(id, "后台导出任务队列繁忙，请稍后重试", System.currentTimeMillis()); throw exception(PURCHASE_ORDER_EXPORT_TASK_SUBMIT_FAILED); }
            return new ErpPurchaseOrderTaskStartRespVO(id, false);
        } finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
    }
    @Override public ErpPurchaseOrderExportTaskRespVO getActiveTask() { ErpImportExportRecordDO r = active(); return r == null ? null : response(r); }
    @Override public ErpPurchaseOrderExportTaskRespVO getTask(Long id) { return response(refresh(owned(id))); }
    @Override public PageResult<ErpPurchaseOrderExportTaskRespVO> getHistory(int pageNo, int pageSize) {
        PageResult<ErpImportExportRecordDO> page = taskSupport.getOwnedHistory(MODULE_KEY, pageNo, pageSize);
        return new PageResult<>(page.getList().stream().map(this::refresh).map(this::response).collect(Collectors.toList()), page.getTotal());
    }
    @Override public void downloadTask(Long id, HttpServletResponse response) throws IOException {
        ErpImportExportRecordDO r = owned(id); if (!ErpImportExportRecordStatusEnum.SUCCESS.getStatus().equals(r.getStatus())) throw exception(PURCHASE_ORDER_EXPORT_TASK_FILE_NOT_READY);
        Path file = finalFile(r); if (taskSupport.isExpired(r) || !Files.isRegularFile(file)) throw exception(PURCHASE_ORDER_EXPORT_TASK_FILE_EXPIRED);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(r.getFileName())); Files.copy(file, response.getOutputStream());
    }

    private void execute(Long id, Long tenant, Long operator, ErpPurchaseOrderPageReqVO request, Set<String> fields) {
        long started = System.currentTimeMillis(); Path part = partFile(tenant, operator, id), target = finalFile(tenant, operator, id);
        try {
            Files.createDirectories(part.getParent()); taskSupport.deleteQuietly(part); taskSupport.deleteQuietly(target);
            update(id, ErpAsyncExportTaskStageEnum.COUNTING.getStage(), 0, 0, 0, started);
            PageResult<ErpPurchaseOrderDO> first = orderService.getPurchaseOrderPage(pageRequest(request, 1));
            int total = Math.toIntExact(first.getTotal()), processed = 0, written = 0;
            try (OutputStream out = Files.newOutputStream(part);
                 ExcelWriter writer = FastExcelFactory.write(out, ErpPurchaseOrderExportRespVO.class).autoCloseStream(false)
                         .includeColumnFieldNames(fields).registerWriteHandler(new ColumnWidthMatchStyleStrategy()).build()) {
                WriteSheet sheet = FastExcelFactory.writerSheet("采购订单").build();
                int pages = total == 0 ? 0 : (total + PAGE_SIZE - 1) / PAGE_SIZE;
                for (int pageNo = 1; pageNo <= pages; pageNo++) {
                    List<ErpPurchaseOrderDO> orders = pageNo == 1 ? first.getList() : orderService.getPurchaseOrderPage(pageRequest(request, pageNo)).getList();
                    for (ErpPurchaseOrderDO order : orders) {
                        boolean firstItemPage = true; int itemPage = 1; boolean wroteAny = false;
                        while (true) {
                            ErpPurchaseOrderItemPageReqVO itemReq = new ErpPurchaseOrderItemPageReqVO(); itemReq.setOrderId(order.getId());
                            itemReq.setPageNo(itemPage++); itemReq.setPageSize(PAGE_SIZE);
                            PageResult<ErpPurchaseOrderItemDO> items = orderService.getPurchaseOrderItemPage(itemReq);
                            if (items.getList().isEmpty()) {
                                if (!wroteAny) { List<ErpPurchaseOrderExportRespVO> rows = assembler.buildRows(order, Collections.emptyList(), true); writer.write(rows, sheet); written += rows.size(); }
                                break;
                            }
                            List<ErpPurchaseOrderExportRespVO> rows = assembler.buildRows(order, items.getList(), firstItemPage);
                            writer.write(rows, sheet); written += rows.size(); wroteAny = true; firstItemPage = false;
                            if (items.getList().size() < PAGE_SIZE) break;
                        }
                        processed++; update(id, ErpAsyncExportTaskStageEnum.EXPORTING.getStage(), total, processed, written, started);
                    }
                }
            }
            update(id, ErpAsyncExportTaskStageEnum.FINALIZING.getStage(), total, total, written, started);
            taskSupport.moveCompletedFile(part, target); finishSuccess(id, total, written, started);
        } catch (Throwable ex) { log.error("[execute][taskId({}) 采购订单导出失败]", id, ex); taskSupport.deleteQuietly(part); taskSupport.deleteQuietly(target); finishFailure(id, brief(ex), started); }
    }
    private ErpPurchaseOrderPageReqVO pageRequest(ErpPurchaseOrderPageReqVO source, int page) { ErpPurchaseOrderPageReqVO r = BeanUtils.toBean(source, ErpPurchaseOrderPageReqVO.class); r.setPageNo(page); r.setPageSize(PAGE_SIZE); r.setIncludeItems(false); return r; }
    private Long createRecord(ErpPurchaseOrderPageReqVO request, Set<String> fields) { ErpImportExportRecordCreateReqBO c = new ErpImportExportRecordCreateReqBO();
        c.setOperationType(ErpImportExportOperationTypeEnum.EXPORT.getType()); c.setModuleKey(MODULE_KEY); c.setModuleName("采购订单");
        c.setFileName("采购订单-" + LocalDateTime.now().format(FORMAT) + ".xlsx"); c.setFileType("XLSX"); c.setStage(ErpAsyncExportTaskStageEnum.QUEUED.getStage());
        c.setQueryParams(JsonUtils.toJsonString(request)); c.setExportFields(JsonUtils.toJsonString(fields)); c.setStartTime(LocalDateTime.now()); c.setHeartbeatTime(LocalDateTime.now()); return recordService.createRecord(c); }
    private void update(Long id, String stage, int total, int processed, int written, long started) { ErpImportExportRecordProgressReqBO p = new ErpImportExportRecordProgressReqBO();
        p.setStage(stage); p.setTotalCount(total); p.setProcessedCount(processed); p.setSuccessCount(written); p.setDurationMs(System.currentTimeMillis() - started); p.setHeartbeatTime(LocalDateTime.now()); recordService.updateProgress(id, p); }
    private void finishSuccess(Long id, int total, int written, long started) { ErpImportExportRecordFinishReqBO f = new ErpImportExportRecordFinishReqBO();
        f.setStatus(ErpImportExportRecordStatusEnum.SUCCESS.getStatus()); f.setStage(ErpAsyncExportTaskStageEnum.FINISHED.getStage()); f.setTotalCount(total); f.setProcessedCount(total);
        f.setSuccessCount(written); f.setFailureCount(0); f.setDurationMs(System.currentTimeMillis() - started); f.setEndTime(LocalDateTime.now()); f.setHeartbeatTime(LocalDateTime.now()); recordService.finishRecord(id, f); }
    private void finishFailure(Long id, String message, long started) { ErpImportExportRecordFinishReqBO f = new ErpImportExportRecordFinishReqBO(); f.setStatus(ErpImportExportRecordStatusEnum.FAILURE.getStatus());
        f.setStage(ErpAsyncExportTaskStageEnum.FINISHED.getStage()); f.setFailureCount(1); f.setErrorMessage(message); f.setDurationMs(Math.max(0, System.currentTimeMillis() - started));
        f.setEndTime(LocalDateTime.now()); f.setHeartbeatTime(LocalDateTime.now()); recordService.finishRecord(id, f); }
    private ErpPurchaseOrderExportTaskRespVO response(ErpImportExportRecordDO r) { ErpPurchaseOrderExportTaskRespVO v = new ErpPurchaseOrderExportTaskRespVO();
        v.setTaskId(r.getId()); v.setStatus(r.getStatus()); v.setStage(r.getStage()); v.setStageName(ErpImportExportRecordStatusEnum.FAILURE.getStatus().equals(r.getStatus()) ? "导出失败" : ErpAsyncExportTaskStageEnum.getName(r.getStage()));
        v.setProgressPercent(taskSupport.calculateProgress(r)); v.setTotalCount(taskSupport.valueOrZero(r.getTotalCount())); v.setProcessedCount(taskSupport.valueOrZero(r.getProcessedCount()));
        v.setExportedCount(taskSupport.valueOrZero(r.getSuccessCount())); v.setFileName(r.getFileName()); v.setSelectedFields(fields(r.getExportFields()));
        v.setStartTime(r.getStartTime()); v.setEndTime(r.getEndTime()); v.setDurationMs(taskSupport.resolveDuration(r)); v.setErrorMessage(r.getErrorMessage());
        v.setExpiresAt(taskSupport.expiresAt(r)); v.setDownloadAvailable(ErpImportExportRecordStatusEnum.SUCCESS.getStatus().equals(r.getStatus()) && !taskSupport.isExpired(r) && Files.isRegularFile(finalFile(r))); return v; }
    private List<String> fields(String json) { try { return json == null ? Collections.emptyList() : JsonUtils.parseObject(json, new TypeReference<List<String>>() {}); } catch (RuntimeException ex) { return Collections.emptyList(); } }
    private ErpImportExportRecordDO active() { ErpImportExportRecordDO r = taskSupport.findActiveRecord(MODULE_KEY); return r == null ? null : refresh(r); }
    private ErpImportExportRecordDO owned(Long id) { ErpImportExportRecordDO r = taskSupport.findOwnedRecord(id, MODULE_KEY); if (r == null) throw exception(PURCHASE_ORDER_EXPORT_TASK_NOT_EXISTS); return r; }
    private ErpImportExportRecordDO refresh(ErpImportExportRecordDO r) { if (!taskSupport.isStale(r)) return r; finishFailure(r.getId(), "任务超过30分钟未更新进度，已自动结束，请重新导出", System.currentTimeMillis()); return recordMapper.selectById(r.getId()); }
    private Path partFile(Long tenant, Long operator, Long id) { return taskSupport.taskDirectory(TEMP_DIR, tenant, operator).resolve(id + ".xlsx.part"); }
    private Path finalFile(Long tenant, Long operator, Long id) { return taskSupport.taskDirectory(TEMP_DIR, tenant, operator).resolve(id + ".xlsx"); }
    private Path finalFile(ErpImportExportRecordDO r) { return finalFile(r.getTenantId(), r.getOperatorId(), r.getId()); }
    private String brief(Throwable ex) { String m = ex.getMessage(); return m == null ? "采购订单导出失败" : m.substring(0, Math.min(500, m.length())); }
}
