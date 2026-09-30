package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintDeviceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintTaskDO;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintDeviceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintTaskMapper;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintConstants;
import cn.iocoder.yudao.module.erp.framework.cloudprint.SwPrintClient;
import cn.iocoder.yudao.module.erp.framework.cloudprint.SwPrintException;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.DeviceInfo;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.PtFileData;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.PtFileReq;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CLOUD_PRINT_DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CLOUD_PRINT_SUBMIT_FAILED;

@Slf4j
@Service
public class ErpCloudPrintQueueServiceImpl implements ErpCloudPrintQueueService {

    private static final String LOCK_KEY_PREFIX = "erp:cloud-print:queue:";

    @Resource
    private ErpCloudPrintDeviceMapper deviceMapper;
    @Resource
    private ErpCloudPrintTaskMapper taskMapper;
    @Resource
    private SwPrintClient swPrintClient;
    @Resource
    private FileApi fileApi;
    @Resource
    private RedissonClient redissonClient;

    @Override
    @Async
    public void dispatchDeviceAsync(Long deviceId) {
        dispatchDevice(deviceId);
    }

    @Override
    public int dispatchPendingTasks() {
        List<ErpCloudPrintTaskDO> pendingTasks = taskMapper.selectPendingTasks();
        Set<Long> deviceIds = new LinkedHashSet<>();
        for (ErpCloudPrintTaskDO task : pendingTasks) {
            if (task.getDeviceId() != null) {
                deviceIds.add(task.getDeviceId());
            }
        }
        int count = 0;
        for (Long deviceId : deviceIds) {
            if (dispatchDevice(deviceId)) {
                count++;
            }
        }
        return count;
    }

    private boolean dispatchDevice(Long deviceId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + tenantId + ":" + deviceId);
        boolean locked = false;
        ErpCloudPrintTaskDO task = null;
        try {
            locked = lock.tryLock(0, 60, TimeUnit.SECONDS);
            if (!locked) {
                return false;
            }
            ErpCloudPrintDeviceDO device = deviceMapper.selectById(deviceId);
            if (device == null || Boolean.TRUE.equals(device.getQueuePaused())) {
                return false;
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(device.getStatus())) {
                pauseDevice(deviceId, null, "打印设备已停用");
                return false;
            }
            if (taskMapper.selectInFlightByDevice(deviceId) != null) {
                return false;
            }
            task = taskMapper.selectFirstPendingByDevice(deviceId);
            if (task == null) {
                return false;
            }
            DeviceInfo info = swPrintClient.getDevice(device.getDevid());
            boolean online = ErpCloudPrintServiceImpl.isDeviceOnline(info);
            deviceMapper.updateById(new ErpCloudPrintDeviceDO()
                    .setId(deviceId)
                    .setOnlineState(online ? 1 : 0)
                    .setLastStatusCode(info == null ? null : info.getStatus())
                    .setLastStatusMessage(info == null ? null : info.getMessage())
                    .setLastStatusTime(LocalDateTime.now()));
            if (!online) {
                pauseDevice(deviceId, task.getId(), "打印机离线，队列已暂停");
                return false;
            }
            submitTask(device, task);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception ex) {
            log.error("[dispatchDevice][云打印队列调度失败 deviceId({})]", deviceId, ex);
            pauseDevice(deviceId, task == null ? null : task.getId(),
                    "队列调度异常：" + StrUtil.maxLength(ex.getMessage(), 400));
            return false;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void submitTask(ErpCloudPrintDeviceDO device, ErpCloudPrintTaskDO task) throws Exception {
        byte[] content = fileApi.getFileContent(task.getFileUrl());
        if (content == null || content.length == 0) {
            throw new IllegalStateException("排队打印文件内容为空");
        }
        PtFileReq req = new PtFileReq()
                .setDevid(device.getDevid())
                .setReqid(task.getReqid())
                .setType(task.getContentType())
                .setWidth(device.getPrintWidth())
                .setHeight(device.getPrintHeight())
                .setPcopy(task.getCopies())
                .setPtype(device.getPaperType())
                .setRotate(device.getRotate());
        LocalDateTime submitTime = LocalDateTime.now();
        taskMapper.updateById(new ErpCloudPrintTaskDO()
                .setId(task.getId())
                .setSubmitReq(JsonUtils.toJsonString(req))
                .setSubmitTime(submitTime)
                .setStatus(ErpCloudPrintConstants.STATUS_SUBMITTED));
        try {
            PtFileData result = swPrintClient.ptFile(req, content, task.getFileName());
            String submitResp = JsonUtils.toJsonString(result);
            if (isAccepted(result)) {
                taskMapper.updateById(new ErpCloudPrintTaskDO()
                        .setId(task.getId())
                        .setSubmitResp(submitResp)
                        .setErrorMsg(null));
                return;
            }
            String message = result == null ? "云打印平台响应为空"
                    : StrUtil.blankToDefault(result.getMessage(), "云打印平台返回未确认状态");
            String resultCode = result == null ? null : StrUtil.trim(result.getCode());
            int failedStatus = "012".equals(resultCode) ? ErpCloudPrintConstants.STATUS_UNKNOWN
                    : ErpCloudPrintConstants.STATUS_SUBMIT_FAILED;
            taskMapper.updateById(new ErpCloudPrintTaskDO()
                    .setId(task.getId())
                    .setSubmitResp(submitResp)
                    .setStatus(failedStatus)
                    .setErrorMsg(StrUtil.maxLength(message, 512)));
            pauseDevice(device.getId(), task.getId(), message);
        } catch (SwPrintException ex) {
            int status = ex.isTimeout() ? ErpCloudPrintConstants.STATUS_UNKNOWN
                    : ErpCloudPrintConstants.STATUS_SUBMIT_FAILED;
            taskMapper.updateById(new ErpCloudPrintTaskDO()
                    .setId(task.getId())
                    .setStatus(status)
                    .setErrorMsg(StrUtil.maxLength(ex.getMessage(), 512)));
            pauseDevice(device.getId(), task.getId(), ex.getMessage());
        } catch (Exception ex) {
            taskMapper.updateById(new ErpCloudPrintTaskDO()
                    .setId(task.getId())
                    .setStatus(ErpCloudPrintConstants.STATUS_SUBMIT_FAILED)
                    .setErrorMsg(StrUtil.maxLength(ex.getMessage(), 512)));
            pauseDevice(device.getId(), task.getId(), ex.getMessage());
        }
    }

    static boolean isAccepted(PtFileData result) {
        if (result == null || !Boolean.TRUE.equals(result.getSuccess())) {
            return false;
        }
        String code = StrUtil.trim(result.getCode());
        return "0".equals(code) || "000".equals(code);
    }

    @Override
    public void pauseDevice(Long deviceId, Long taskId, String reason) {
        if (deviceId == null) {
            return;
        }
        deviceMapper.pauseQueue(deviceId,
                StrUtil.maxLength(StrUtil.blankToDefault(reason, "云打印任务异常"), 512), taskId);
    }

    @Override
    public boolean clearPauseIfTask(Long deviceId, Long taskId) {
        if (deviceId == null || taskId == null) {
            return false;
        }
        ErpCloudPrintDeviceDO device = deviceMapper.selectById(deviceId);
        if (device == null || !Boolean.TRUE.equals(device.getQueuePaused())
                || !taskId.equals(device.getQueuePauseTaskId())) {
            return false;
        }
        deviceMapper.resumeQueue(deviceId);
        return true;
    }

    @Override
    public ErpCloudPrintTaskDO resumeQueue(Long deviceId, String failedTaskAction) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + tenantId + ":" + deviceId);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw exception(CLOUD_PRINT_SUBMIT_FAILED, "打印机队列正在调度，请稍后再恢复");
            }
            ErpCloudPrintDeviceDO device = deviceMapper.selectById(deviceId);
            if (device == null) {
                throw exception(CLOUD_PRINT_DEVICE_NOT_EXISTS);
            }
            if (!"RETRY".equals(failedTaskAction) && !"SKIP".equals(failedTaskAction)) {
                throw exception(CLOUD_PRINT_SUBMIT_FAILED, "故障任务处理方式必须为 RETRY 或 SKIP");
            }
            ErpCloudPrintTaskDO retryTask = null;
            if (device.getQueuePauseTaskId() != null) {
                ErpCloudPrintTaskDO failedTask = taskMapper.selectById(device.getQueuePauseTaskId());
                if (failedTask != null
                        && !Integer.valueOf(ErpCloudPrintConstants.STATUS_SUCCESS).equals(failedTask.getStatus())) {
                    if ("RETRY".equals(failedTaskAction)) {
                        retryTask = cloneRetryTask(failedTask);
                        taskMapper.insert(retryTask);
                    }
                    taskMapper.updateById(new ErpCloudPrintTaskDO()
                            .setId(failedTask.getId())
                            .setStatus(ErpCloudPrintConstants.STATUS_CANCELED)
                            .setErrorMsg("SKIP".equals(failedTaskAction)
                                    ? "用户跳过故障任务并恢复队列" : "用户创建重试任务并恢复队列"));
                }
            }
            deviceMapper.resumeQueue(deviceId);
            return retryTask;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, "恢复打印队列被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private ErpCloudPrintTaskDO cloneRetryTask(ErpCloudPrintTaskDO source) {
        return new ErpCloudPrintTaskDO()
                .setReqid(buildReqid(source.getBizNo()))
                .setBizType(source.getBizType())
                .setBizId(source.getBizId())
                .setBizNo(source.getBizNo())
                .setWarehouseId(source.getWarehouseId())
                .setWarehouseName(source.getWarehouseName())
                .setDevid(source.getDevid())
                .setDeviceId(source.getDeviceId())
                .setTemplateId(source.getTemplateId())
                .setContentType(source.getContentType())
                .setCopies(source.getCopies())
                .setFileUrl(source.getFileUrl())
                .setFileName(source.getFileName())
                .setRetryOf(source.getId())
                .setStatus(ErpCloudPrintConstants.STATUS_PENDING);
    }

    private String buildReqid(String bizNo) {
        String prefix = "SO" + (bizNo == null ? "" : bizNo.replaceAll("[^A-Za-z0-9]", ""));
        String reqid = prefix + "-" + RandomUtil.randomNumbers(6);
        return reqid.length() <= 64 ? reqid : reqid.substring(reqid.length() - 64);
    }

}
