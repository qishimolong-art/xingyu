package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintDeviceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintTaskDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintDeviceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintTaskMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpPrintRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintCallbackConstants;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ErpCloudPrintCallbackProcessor {

    @Resource
    private ErpCloudPrintTaskMapper taskMapper;
    @Resource
    private ErpCloudPrintDeviceMapper deviceMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpPrintRecordMapper printRecordMapper;
    @Resource
    private ErpCloudPrintQueueService queueService;

    @TenantIgnore
    @Transactional(rollbackFor = Exception.class)
    public ErpCloudPrintCallbackProcessResult process(ErpCloudPrintCallbackRequest request,
                                                       LocalDateTime receivedTime) {
        if ("printRlt".equals(request.getMethod())) {
            return processPrintResult(request, receivedTime);
        }
        return processDeviceStatus(request);
    }

    private ErpCloudPrintCallbackProcessResult processPrintResult(ErpCloudPrintCallbackRequest request,
                                                                   LocalDateTime receivedTime) {
        List<ErpCloudPrintTaskDO> candidates = taskMapper.selectListByReqid(request.getReqid());
        Set<Long> deviceIds = candidates.stream().map(ErpCloudPrintTaskDO::getDeviceId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ErpCloudPrintDeviceDO> deviceMap = new HashMap<>();
        if (!deviceIds.isEmpty()) {
            deviceMapper.selectBatchIds(deviceIds).forEach(device -> deviceMap.put(device.getId(), device));
        }
        List<ErpCloudPrintTaskDO> matchedTasks = candidates.stream().filter(task -> {
            ErpCloudPrintDeviceDO device = deviceMap.get(task.getDeviceId());
            return device != null && Objects.equals(task.getTenantId(), device.getTenantId())
                    && request.getDevid().equals(device.getDevid());
        }).collect(Collectors.toList());
        if (matchedTasks.isEmpty()) {
            LocalDateTime waitDeadline = (receivedTime == null ? LocalDateTime.now() : receivedTime)
                    .plusMinutes(ErpCloudPrintCallbackConstants.TASK_WAIT_MINUTES);
            if (LocalDateTime.now().isBefore(waitDeadline)) {
                return ErpCloudPrintCallbackProcessResult.retry("暂未找到 reqid 与 devid 同时匹配的打印任务");
            }
            return ErpCloudPrintCallbackProcessResult.ignored(false,
                    "超过10分钟仍未找到 reqid 与 devid 同时匹配的打印任务");
        }
        if (matchedTasks.size() > 1) {
            return ErpCloudPrintCallbackProcessResult.ignored(false,
                    "reqid 与 devid 匹配到多个打印任务，未更新业务数据");
        }
        ErpCloudPrintTaskDO task = matchedTasks.get(0);
        return TenantUtils.execute(task.getTenantId(), () -> processTenantPrintResult(task.getId(), request));
    }

    private ErpCloudPrintCallbackProcessResult processTenantPrintResult(Long taskId,
                                                                         ErpCloudPrintCallbackRequest request) {
        ErpCloudPrintTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            return ErpCloudPrintCallbackProcessResult.retry("打印任务在租户上下文中暂不可见");
        }
        boolean success = Integer.valueOf(0).equals(request.getCode());
        int newStatus = success ? ErpCloudPrintConstants.STATUS_SUCCESS : ErpCloudPrintConstants.STATUS_FAILED;
        String callbackMessage = limit(request.getMessage(), 512);
        if (!success && StrUtil.isBlank(callbackMessage)) {
            callbackMessage = printResultMessage(request.getCode());
        }
        int transitioned = taskMapper.updatePrintCallbackIfProcessable(task.getId(), newStatus,
                request.getCode(), callbackMessage, LocalDateTime.now());
        if (transitioned == 0) {
            return ErpCloudPrintCallbackProcessResult.success(true,
                    "打印任务已是最终状态，保留首次最终结果");
        }
        if (success) {
            if (ErpCloudPrintConstants.BIZ_TYPE_SALE_OUT.equals(task.getBizType())) {
                recordSaleOutPrintSuccess(task);
            }
            queueService.clearPauseIfTask(task.getDeviceId(), task.getId());
            dispatchAfterCommit(task.getDeviceId(), task.getTenantId());
            return ErpCloudPrintCallbackProcessResult.success(true, "打印任务已更新为成功");
        }
        queueService.pauseDevice(task.getDeviceId(), task.getId(), callbackMessage);
        return ErpCloudPrintCallbackProcessResult.success(true, "打印任务已更新为失败");
    }

    private ErpCloudPrintCallbackProcessResult processDeviceStatus(ErpCloudPrintCallbackRequest request) {
        List<ErpCloudPrintDeviceDO> devices = deviceMapper.selectListByDevid(request.getDevid());
        if (devices.isEmpty()) {
            return ErpCloudPrintCallbackProcessResult.ignored(false, "未找到对应的云打印设备");
        }
        String statusMessage = deviceStatusMessage(request.getCode(), request.getMessage());
        String finalStatusMessage = statusMessage;
        for (ErpCloudPrintDeviceDO device : devices) {
            TenantUtils.execute(device.getTenantId(), () -> {
                deviceMapper.updateById(new ErpCloudPrintDeviceDO()
                        .setId(device.getId())
                        .setLastStatusCode(request.getCode())
                        .setLastStatusMessage(finalStatusMessage)
                        .setLastStatusTime(LocalDateTime.now()));
                if (!Integer.valueOf(0).equals(request.getCode())) {
                    queueService.pauseDevice(device.getId(), null, finalStatusMessage);
                }
            });
        }
        return ErpCloudPrintCallbackProcessResult.success(true,
                Integer.valueOf(0).equals(request.getCode())
                        ? "设备状态已更新为正常，未自动恢复队列" : "设备故障状态已更新并暂停队列");
    }

    private void recordSaleOutPrintSuccess(ErpCloudPrintTaskDO task) {
        saleOutMapper.incrementPrintCount(task.getBizId());
        printRecordMapper.insert(ErpPrintRecordDO.builder()
                .moduleKey(ErpCloudPrintConstants.MODULE_KEY_SALE_OUT)
                .businessId(task.getBizId())
                .businessNo(task.getBizNo())
                .templateId(task.getTemplateId())
                .printerId(null)
                .printerName("云打印回调")
                .printTime(LocalDateTime.now())
                .build());
    }

    private void dispatchAfterCommit(Long deviceId, Long tenantId) {
        Runnable dispatch = () -> TenantUtils.execute(tenantId, () -> queueService.dispatchDeviceAsync(deviceId));
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
            return;
        }
        dispatch.run();
    }

    private static String printResultMessage(Integer code) {
        Map<Integer, String> messages = new HashMap<>();
        messages.put(201, "无任务 ID");
        messages.put(202, "没有内容 type");
        messages.put(203, "无效内容类型 type");
        messages.put(204, "无任务内容");
        messages.put(205, "无效 MSG");
        messages.put(206, "打印内容下载失败");
        messages.put(207, "打印任务超时");
        messages.put(210, "打印内容数据格式错误");
        return messages.getOrDefault(code, "打印失败，回调码：" + code);
    }

    private static String deviceStatusMessage(Integer code, String vendorMessage) {
        if (Integer.valueOf(0).equals(code)) {
            return limit(vendorMessage, 512);
        }
        Map<Integer, String> messages = new HashMap<>();
        messages.put(100, "其他错误");
        messages.put(101, "缺纸");
        messages.put(102, "开盖");
        messages.put(103, "故障");
        messages.put(104, "纸将尽");
        messages.put(109, "打印机未接入");
        messages.put(300, "固件升级中");
        messages.put(301, "固件升级中");
        String documentedMessage = messages.get(code);
        if (documentedMessage != null) {
            return documentedMessage;
        }
        String normalizedVendorMessage = limit(vendorMessage, 512);
        return StrUtil.isNotBlank(normalizedVendorMessage)
                ? normalizedVendorMessage : "设备故障，状态码：" + code;
    }

    private static String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

}
