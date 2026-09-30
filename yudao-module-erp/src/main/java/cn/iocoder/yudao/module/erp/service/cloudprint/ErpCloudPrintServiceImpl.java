package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDevicePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDeviceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDeviceSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintTaskRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintDeviceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintTaskDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintTemplateDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintDeviceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintTaskMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpPrintTemplateMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintConstants;
import cn.iocoder.yudao.module.erp.framework.cloudprint.SwPrintClient;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.DeviceInfo;
import cn.iocoder.yudao.module.erp.service.common.ErpPrintService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Slf4j
@Service
@Validated
public class ErpCloudPrintServiceImpl implements ErpCloudPrintService {

    private static final String SALE_OUT_FILE_DIRECTORY = "erp/cloud-print/sale-out";

    @Resource
    private ErpCloudPrintDeviceMapper deviceMapper;
    @Resource
    private ErpCloudPrintTaskMapper taskMapper;
    @Resource
    private ErpSaleOutService saleOutService;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpWarehouseMapper warehouseMapper;
    @Resource
    private ErpPrintService printService;
    @Resource
    private ErpPrintTemplateMapper printTemplateMapper;
    @Resource
    private ErpCloudPrintHtmlRenderer htmlRenderer;
    @Resource
    private FileApi fileApi;
    @Resource
    private SwPrintClient swPrintClient;
    @Resource
    private ErpCloudPrintQueueService queueService;
    @Resource
    private DeptApi deptApi;

    @Override
    public List<ErpCloudPrintTaskRespVO> submitSaleOut(Long saleOutId, Integer copies, Long templateId) {
        ErpSaleOutDO saleOut = saleOutService.validateSaleOut(saleOutId);
        ErpPrintTemplateDO template = resolveSaleOutTemplate(templateId);
        List<ErpSaleOutItemDO> saleOutItems = saleOutItemMapper.selectListByOutId(saleOutId);
        List<WarehousePrintGroup> groups = resolveWarehousePrintGroups(saleOutItems);
        Map<String, Object> fullPrintData = printService.getPrintData(ErpCloudPrintConstants.MODULE_KEY_SALE_OUT, saleOutId);
        List<Map<String, Object>> fullItemRows = getPrintItemRows(fullPrintData);
        if (fullItemRows.size() != saleOutItems.size()) {
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, "销售单打印数据与明细行数不一致");
        }

        List<ErpCloudPrintTaskRespVO> result = new ArrayList<>();
        Set<Long> deviceIds = new LinkedHashSet<>();
        for (WarehousePrintGroup group : groups) {
            Map<String, Object> warehousePrintData = buildWarehousePrintData(fullPrintData, fullItemRows, group);
            ErpCloudPrintTaskDO task = submitSaleOutGroup(saleOut, template, group, copies, warehousePrintData);
            result.add(toTaskResp(task));
            deviceIds.add(group.getDevice().getId());
        }
        for (Long deviceId : deviceIds) {
            queueService.dispatchDeviceAsync(deviceId);
        }
        return result;
    }

    private ErpCloudPrintTaskDO submitSaleOutGroup(ErpSaleOutDO saleOut, ErpPrintTemplateDO template,
                                                   WarehousePrintGroup group, Integer copies,
                                                   Map<String, Object> printData) {
        ErpCloudPrintDeviceDO device = group.getDevice();
        Integer finalCopies = resolveCopies(copies, device);
        String reqid = buildReqid(saleOut.getNo());
        Integer contentType = ErpCloudPrintConstants.CONTENT_TYPE_PDF;
        String fileName = saleOut.getNo() + "-" + safeFileName(group.getWarehouse().getName()) + "-" + reqid + ".pdf";
        byte[] content = htmlRenderer.renderSaleOutTemplatePdf(template.getTemplateJson(), template.getPaperConfig(),
                printData, device.getPrintWidth(), device.getPrintHeight());
        String fileUrl = fileApi.createFile(content, fileName,
                SALE_OUT_FILE_DIRECTORY + "/" + saleOut.getId() + "/" + group.getWarehouse().getId(), "application/pdf");
        ErpCloudPrintTaskDO task = new ErpCloudPrintTaskDO()
                .setReqid(reqid)
                .setBizType(ErpCloudPrintConstants.BIZ_TYPE_SALE_OUT)
                .setBizId(saleOut.getId())
                .setBizNo(saleOut.getNo())
                .setWarehouseId(group.getWarehouse().getId())
                .setWarehouseName(group.getWarehouse().getName())
                .setDevid(device.getDevid())
                .setDeviceId(device.getId())
                .setTemplateId(template.getId())
                .setContentType(contentType)
                .setCopies(finalCopies)
                .setFileUrl(fileUrl)
                .setFileName(fileName)
                .setStatus(ErpCloudPrintConstants.STATUS_PENDING);
        taskMapper.insert(task);
        log.info("[submitSaleOutGroup][销售单云打印入队 saleOutId({}) warehouseId({}) templateId({}) deviceId({}) taskId({})]",
                saleOut.getId(), group.getWarehouse().getId(), template.getId(), device.getId(), task.getId());
        return task;
    }

    private List<WarehousePrintGroup> resolveWarehousePrintGroups(List<ErpSaleOutItemDO> saleOutItems) {
        if (saleOutItems == null || saleOutItems.isEmpty()) {
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, "销售单明细为空，无法按仓库分发云打印");
        }
        Map<Long, List<Integer>> itemIndexesByWarehouse = new LinkedHashMap<>();
        for (int i = 0; i < saleOutItems.size(); i++) {
            ErpSaleOutItemDO item = saleOutItems.get(i);
            if (item.getWarehouseId() == null) {
                throw exception(CLOUD_PRINT_SUBMIT_FAILED, "销售单存在未填写仓库的明细，无法分发云打印");
            }
            itemIndexesByWarehouse.computeIfAbsent(item.getWarehouseId(), key -> new ArrayList<>()).add(i);
        }
        Set<Long> warehouseIds = itemIndexesByWarehouse.keySet();
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseMapper.selectByIds(warehouseIds).stream()
                .collect(Collectors.toMap(ErpWarehouseDO::getId, warehouse -> warehouse));
        Set<Long> deviceIds = new LinkedHashSet<>();
        for (Long warehouseId : warehouseIds) {
            ErpWarehouseDO warehouse = warehouseMap.get(warehouseId);
            if (warehouse == null) {
                throw exception(CLOUD_PRINT_SUBMIT_FAILED, "销售单明细仓库不存在：" + warehouseId);
            }
            if (warehouse.getCloudPrintDeviceId() == null) {
                throw exception(CLOUD_PRINT_SUBMIT_FAILED,
                        "仓库【" + warehouse.getName() + "】未绑定云打印设备");
            }
            deviceIds.add(warehouse.getCloudPrintDeviceId());
        }
        Map<Long, ErpCloudPrintDeviceDO> deviceMap = deviceMapper.selectByIds(deviceIds).stream()
                .collect(Collectors.toMap(ErpCloudPrintDeviceDO::getId, device -> device));
        List<WarehousePrintGroup> groups = new ArrayList<>();
        for (Map.Entry<Long, List<Integer>> entry : itemIndexesByWarehouse.entrySet()) {
            ErpWarehouseDO warehouse = warehouseMap.get(entry.getKey());
            ErpCloudPrintDeviceDO device = deviceMap.get(warehouse.getCloudPrintDeviceId());
            if (device == null) {
                throw exception(CLOUD_PRINT_SUBMIT_FAILED,
                        "仓库【" + warehouse.getName() + "】绑定的云打印设备不存在");
            }
            validateDeviceForSubmit(device);
            List<ErpSaleOutItemDO> items = entry.getValue().stream()
                    .map(saleOutItems::get)
                    .collect(Collectors.toList());
            groups.add(new WarehousePrintGroup()
                    .setWarehouse(warehouse)
                    .setDevice(device)
                    .setItemIndexes(entry.getValue())
                    .setItems(items));
        }
        return groups;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getPrintItemRows(Map<String, Object> printData) {
        if (printData == null || !(printData.get("items") instanceof List)) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) printData.get("items");
    }

    private Map<String, Object> buildWarehousePrintData(Map<String, Object> fullPrintData,
                                                        List<Map<String, Object>> fullItemRows,
                                                        WarehousePrintGroup group) {
        Map<String, Object> result = new LinkedHashMap<>(fullPrintData);
        Map<String, Object> documentMap = copyMap(fullPrintData.get("document"));
        Map<String, Object> mainMap = copyMap(fullPrintData.get("main"));
        List<Map<String, Object>> itemRows = new ArrayList<>();
        for (int i = 0; i < group.getItemIndexes().size(); i++) {
            Map<String, Object> row = new LinkedHashMap<>(fullItemRows.get(group.getItemIndexes().get(i)));
            row.put("items.seq", i + 1);
            itemRows.add(row);
        }
        result.put("document", documentMap);
        result.put("main", mainMap);
        result.put("warehouse", simpleNameMap(group.getWarehouse().getName()));
        result.put("items", itemRows);
        putWarehousePrintTotals(documentMap, mainMap, group);
        putDocumentField(documentMap, mainMap, "warehouseNames", group.getWarehouse().getName());
        putDocumentField(documentMap, mainMap, "cloudPrintWarehouseName", group.getWarehouse().getName());
        mainMap.put("warehouse.name", group.getWarehouse().getName());
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> copyMap(Object value) {
        if (value instanceof Map) {
            return new LinkedHashMap<>((Map<String, Object>) value);
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> simpleNameMap(String name) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name == null ? "" : name);
        return map;
    }

    private void putWarehousePrintTotals(Map<String, Object> documentMap, Map<String, Object> mainMap,
                                         WarehousePrintGroup group) {
        BigDecimal totalCount = sum(group.getItems(), ErpSaleOutItemDO::getCount);
        BigDecimal totalProductPrice = sum(group.getItems(), ErpSaleOutItemDO::getTotalPrice);
        BigDecimal discountPercent = parseDecimal(documentMap.get("discountPercent"));
        if (discountPercent == null) {
            discountPercent = BigDecimal.ZERO;
        }
        BigDecimal discountPrice = MoneyUtils.priceMultiplyPercent(totalProductPrice, discountPercent);
        BigDecimal totalPrice = totalProductPrice.subtract(discountPrice);
        BigDecimal totalWeight = sum(group.getItems(), ErpSaleOutItemDO::getTotalWeight);
        putDocumentField(documentMap, mainMap, "totalCount", totalCount);
        putDocumentField(documentMap, mainMap, "totalProductPrice", totalProductPrice);
        putDocumentField(documentMap, mainMap, "totalTaxPrice", BigDecimal.ZERO);
        putDocumentField(documentMap, mainMap, "discountPercent", discountPercent);
        putDocumentField(documentMap, mainMap, "discountPrice", discountPrice);
        putDocumentField(documentMap, mainMap, "reductionAmount", discountPrice);
        putDocumentField(documentMap, mainMap, "feeAmount", BigDecimal.ZERO);
        putDocumentField(documentMap, mainMap, "otherPrice", BigDecimal.ZERO);
        putDocumentField(documentMap, mainMap, "extraFee", BigDecimal.ZERO);
        putDocumentField(documentMap, mainMap, "totalPrice", totalPrice);
        putDocumentField(documentMap, mainMap, "totalAmount", totalPrice);
        putDocumentField(documentMap, mainMap, "afterReductionAmount", totalPrice);
        putDocumentField(documentMap, mainMap, "currentDebt", totalPrice);
        putDocumentField(documentMap, mainMap, "totalWeight", totalWeight);
        BigDecimal previousReceivable = parseDecimal(documentMap.get("previousReceivable"));
        if (previousReceivable != null) {
            putDocumentField(documentMap, mainMap, "totalDebt", previousReceivable.add(totalPrice));
        }
        putDocumentField(documentMap, mainMap, "totalPriceUpper", MoneyUtils.formatAmountUpper(totalPrice));
        putDocumentField(documentMap, mainMap, "totalAmountUpper", MoneyUtils.formatAmountUpper(totalPrice));
    }

    private BigDecimal sum(List<ErpSaleOutItemDO> items,
                           java.util.function.Function<ErpSaleOutItemDO, BigDecimal> getter) {
        return items.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void putDocumentField(Map<String, Object> documentMap, Map<String, Object> mainMap,
                                  String fieldKey, Object value) {
        Object formatted = formatPrintValue(value);
        documentMap.put(fieldKey, formatted);
        mainMap.put("document." + fieldKey, formatted);
    }

    private Object formatPrintValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        return value;
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer resolveCopies(Integer copies, ErpCloudPrintDeviceDO device) {
        Integer finalCopies = copies != null && copies > 0 ? copies : device.getCopies();
        return finalCopies != null && finalCopies > 0 ? finalCopies : 1;
    }

    private String safeFileName(String value) {
        String text = StringUtils.hasText(value) ? value.trim() : "warehouse";
        return text.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    @Override
    public List<ErpCloudPrintTaskRespVO> getTaskListBySaleOut(Long saleOutId) {
        return convertList(taskMapper.selectListByBiz(ErpCloudPrintConstants.BIZ_TYPE_SALE_OUT, saleOutId), this::toTaskResp);
    }

    @Override
    public List<ErpCloudPrintDeviceRespVO> getEnabledDevices() {
        return fillDeviceDeptNames(convertList(
                deviceMapper.selectList(ErpCloudPrintDeviceDO::getStatus, CommonStatusEnum.ENABLE.getStatus()),
                this::toDeviceResp));
    }

    @Override
    public PageResult<ErpCloudPrintDeviceRespVO> getDevicePage(ErpCloudPrintDevicePageReqVO pageReqVO) {
        PageResult<ErpCloudPrintDeviceDO> pageResult = deviceMapper.selectPage(pageReqVO);
        List<ErpCloudPrintDeviceRespVO> list = fillDeviceDeptNames(convertList(pageResult.getList(), this::toDeviceResp));
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public ErpCloudPrintDeviceRespVO getDevice(Long id) {
        return toDeviceResp(validateDeviceExists(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDevice(ErpCloudPrintDeviceSaveReqVO createReqVO) {
        validateDevidUnique(null, createReqVO.getDevid());
        validateDeviceConfig(createReqVO);
        ErpCloudPrintDeviceDO device = BeanUtils.toBean(createReqVO, ErpCloudPrintDeviceDO.class);
        normalizeDevice(device);
        if (Boolean.TRUE.equals(device.getDefaulted())) {
            ensureDefaultDeviceEnabled(device);
            deviceMapper.clearDefaulted(null);
        }
        deviceMapper.insert(device);
        return device.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDevice(ErpCloudPrintDeviceSaveReqVO updateReqVO) {
        ErpCloudPrintDeviceDO existing = validateDeviceExists(updateReqVO.getId());
        validateDevidUnique(updateReqVO.getId(), updateReqVO.getDevid());
        validateDeviceConfig(updateReqVO);
        if (CommonStatusEnum.isDisable(updateReqVO.getStatus())) {
            validateDeviceNotBoundByEnabledWarehouse(existing);
        }
        ErpCloudPrintDeviceDO updateObj = BeanUtils.toBean(updateReqVO, ErpCloudPrintDeviceDO.class);
        normalizeDevice(updateObj);
        if (!StringUtils.hasText(updateReqVO.getDevKey())) {
            updateObj.setDevKey(existing.getDevKey());
        }
        if (Boolean.TRUE.equals(updateObj.getDefaulted())) {
            ensureDefaultDeviceEnabled(updateObj);
            deviceMapper.clearDefaulted(updateObj.getId());
        }
        if (CommonStatusEnum.isDisable(updateObj.getStatus())) {
            updateObj.setDefaulted(false);
        }
        deviceMapper.updateById(updateObj);
    }

    @Override
    public void deleteDevice(Long id) {
        ErpCloudPrintDeviceDO device = validateDeviceExists(id);
        validateDeviceNotBoundByAnyWarehouse(device);
        deviceMapper.deleteById(id);
    }

    @Override
    public void updateDeviceStatus(Long id, Integer status) {
        ErpCloudPrintDeviceDO device = validateDeviceExists(id);
        validateCommonStatus(status);
        if (CommonStatusEnum.isDisable(status)) {
            validateDeviceNotBoundByEnabledWarehouse(device);
        }
        ErpCloudPrintDeviceDO updateObj = new ErpCloudPrintDeviceDO()
                .setId(id)
                .setStatus(status);
        if (CommonStatusEnum.isDisable(status)) {
            updateObj.setDefaulted(false);
        }
        deviceMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultDevice(Long id) {
        ErpCloudPrintDeviceDO device = validateDeviceExists(id);
        ensureDefaultDeviceEnabled(device);
        deviceMapper.clearDefaulted(id);
        deviceMapper.updateById(new ErpCloudPrintDeviceDO().setId(id).setDefaulted(true));
    }

    @Override
    public ErpCloudPrintDeviceRespVO refreshDeviceStatus(Long id) {
        ErpCloudPrintDeviceDO device = validateDeviceExists(id);
        DeviceInfo info = swPrintClient.getDevice(device.getDevid());
        boolean online = isDeviceOnline(info);
        deviceMapper.updateById(new ErpCloudPrintDeviceDO()
                .setId(device.getId())
                .setOnlineState(online ? 1 : 0)
                .setLastStatusCode(info == null ? null : info.getStatus())
                .setLastStatusMessage(info == null ? null : info.getMessage())
                .setLastStatusTime(LocalDateTime.now()));
        return getDevice(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpCloudPrintDeviceRespVO resumeQueue(Long deviceId, String failedTaskAction) {
        validateDeviceExists(deviceId);
        queueService.resumeQueue(deviceId, failedTaskAction);
        dispatchAfterCommit(deviceId, null);
        return toDeviceResp(validateDeviceExists(deviceId));
    }

    private void dispatchAfterCommit(Long deviceId, Long tenantId) {
        Runnable dispatch = () -> {
            if (tenantId == null) {
                queueService.dispatchDeviceAsync(deviceId);
            } else {
                TenantUtils.execute(tenantId, () -> queueService.dispatchDeviceAsync(deviceId));
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }

    static boolean isDeviceOnline(DeviceInfo info) {
        if (info == null) {
            return false;
        }
        String state = StrUtil.trim(info.getState());
        if (StrUtil.equalsAnyIgnoreCase(state, "online", "on-line") || StrUtil.contains(state, "在线")) {
            return true;
        }
        if (StrUtil.equalsAnyIgnoreCase(state, "offline", "off-line") || StrUtil.contains(state, "离线")) {
            return false;
        }
        return Integer.valueOf(0).equals(info.getCode())
                || Integer.valueOf(200).equals(info.getCode())
                || Integer.valueOf(0).equals(info.getStatus());
    }

    private void validateDeviceForSubmit(ErpCloudPrintDeviceDO device) {
        if (!CommonStatusEnum.ENABLE.getStatus().equals(device.getStatus())) {
            throw exception(CLOUD_PRINT_DEVICE_DISABLED, device.getNickname());
        }
        if (Integer.valueOf(ErpCloudPrintConstants.DEV_TYPE_DOT_MATRIX).equals(device.getDevType())
                && device.getPrintHeight() == null) {
            throw exception(CLOUD_PRINT_DEVICE_HEIGHT_REQUIRED, device.getNickname());
        }
        if (device.getPrintWidth() == null) {
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, "打印设备未配置有效宽度");
        }
        if (device.getContentType() == null) {
            device.setContentType(ErpCloudPrintConstants.CONTENT_TYPE_HTML);
        }
    }

    private ErpPrintTemplateDO resolveSaleOutTemplate(Long templateId) {
        ErpPrintTemplateDO template = templateId == null
                ? printTemplateMapper.selectDefaultByModuleKey(ErpCloudPrintConstants.MODULE_KEY_SALE_OUT)
                : printTemplateMapper.selectById(templateId);
        if (template == null) {
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, templateId == null
                    ? "销售单未配置默认打印模板" : "销售单打印模板不存在");
        }
        if (!ErpCloudPrintConstants.MODULE_KEY_SALE_OUT.equals(template.getModuleKey())
                || !CommonStatusEnum.ENABLE.getStatus().equals(template.getStatus())) {
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, "销售单打印模板不存在或已停用");
        }
        if (!StringUtils.hasText(template.getTemplateJson())) {
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, "销售单打印模板内容为空");
        }
        return template;
    }

    private String buildReqid(String bizNo) {
        String prefix = "SO" + (bizNo == null ? "" : bizNo.replaceAll("[^A-Za-z0-9]", ""));
        String reqid = prefix + "-" + RandomUtil.randomNumbers(6);
        return reqid.length() <= 64 ? reqid : reqid.substring(reqid.length() - 64);
    }

    private ErpCloudPrintTaskRespVO toTaskResp(ErpCloudPrintTaskDO task) {
        ErpCloudPrintTaskRespVO respVO = BeanUtils.toBean(task, ErpCloudPrintTaskRespVO.class);
        ErpCloudPrintDeviceDO device = task.getDeviceId() == null ? null : deviceMapper.selectById(task.getDeviceId());
        if (Integer.valueOf(ErpCloudPrintConstants.STATUS_PENDING).equals(task.getStatus())) {
            Long position = taskMapper.selectQueuePosition(task.getDeviceId(), task.getId());
            respVO.setQueuePosition(position);
            if (device != null && Boolean.TRUE.equals(device.getQueuePaused())) {
                respVO.setQueueState("PAUSED");
                respVO.setQueueMessage("已加入" + device.getNickname() + "队列，队列已暂停："
                        + StrUtil.blankToDefault(device.getQueuePauseReason(), "等待设备恢复"));
            } else {
                respVO.setQueueState("WAITING");
                respVO.setQueueMessage("已加入" + (device == null ? "云打印机" : device.getNickname())
                        + "队列，当前第" + position + "位");
            }
            return respVO;
        }
        if (Integer.valueOf(ErpCloudPrintConstants.STATUS_SUBMITTED).equals(task.getStatus())) {
            return respVO.setQueuePosition(0L).setQueueState("PRINTING")
                    .setQueueMessage("已提交至" + (device == null ? "云打印机" : device.getNickname()) + "，等待打印回调");
        }
        if (Integer.valueOf(ErpCloudPrintConstants.STATUS_UNKNOWN).equals(task.getStatus())) {
            return respVO.setQueueState("UNKNOWN").setQueueMessage("提交结果未知，队列已暂停，请到云打印机页面处理");
        }
        if (Integer.valueOf(ErpCloudPrintConstants.STATUS_SUCCESS).equals(task.getStatus())) {
            return respVO.setQueueState("SUCCESS").setQueueMessage("打印成功");
        }
        if (Integer.valueOf(ErpCloudPrintConstants.STATUS_TIMEOUT).equals(task.getStatus())) {
            return respVO.setQueueState("TIMEOUT").setQueueMessage("打印回调超时，队列已暂停");
        }
        if (Integer.valueOf(ErpCloudPrintConstants.STATUS_CANCELED).equals(task.getStatus())) {
            return respVO.setQueueState("SKIPPED").setQueueMessage("故障任务已跳过");
        }
        return respVO.setQueueState("FAILED")
                .setQueueMessage(StrUtil.blankToDefault(task.getErrorMsg(), "打印失败，队列已暂停"));
    }

    private ErpCloudPrintDeviceDO validateDeviceExists(Long id) {
        ErpCloudPrintDeviceDO device = id == null ? null : deviceMapper.selectById(id);
        if (device == null) {
            throw exception(CLOUD_PRINT_DEVICE_NOT_EXISTS);
        }
        return device;
    }

    private void validateDevidUnique(Long id, String devid) {
        ErpCloudPrintDeviceDO device = deviceMapper.selectByDevid(devid);
        if (device == null || Objects.equals(device.getId(), id)) {
            return;
        }
        throw exception(CLOUD_PRINT_DEVICE_DEVID_DUPLICATE, devid);
    }

    private void validateDeviceConfig(ErpCloudPrintDeviceSaveReqVO reqVO) {
        validateCommonStatus(reqVO.getStatus());
        if (!Integer.valueOf(ErpCloudPrintConstants.CONTENT_TYPE_PDF).equals(reqVO.getContentType())
                && !Integer.valueOf(ErpCloudPrintConstants.CONTENT_TYPE_HTML).equals(reqVO.getContentType())) {
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, "云打印内容类型不支持");
        }
        if (reqVO.getDevType() == null || reqVO.getDevType() < ErpCloudPrintConstants.DEV_TYPE_THERMAL_80
                || reqVO.getDevType() > ErpCloudPrintConstants.DEV_TYPE_DOT_MATRIX) {
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, "云打印设备类型不支持");
        }
        if (Integer.valueOf(ErpCloudPrintConstants.DEV_TYPE_DOT_MATRIX).equals(reqVO.getDevType())
                && reqVO.getPrintHeight() == null) {
            throw exception(CLOUD_PRINT_DEVICE_HEIGHT_REQUIRED, reqVO.getNickname());
        }
        if (Boolean.TRUE.equals(reqVO.getDefaulted()) && CommonStatusEnum.isDisable(reqVO.getStatus())) {
            throw exception(CLOUD_PRINT_DEVICE_DISABLED, reqVO.getNickname());
        }
    }

    private void validateCommonStatus(Integer status) {
        if (!CommonStatusEnum.ENABLE.getStatus().equals(status)
                && !CommonStatusEnum.DISABLE.getStatus().equals(status)) {
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, "云打印设备状态不支持");
        }
    }

    private void normalizeDevice(ErpCloudPrintDeviceDO device) {
        device.setDevid(StrUtil.trim(device.getDevid()));
        device.setDevKey(StrUtil.trimToNull(device.getDevKey()));
        device.setNickname(StrUtil.trim(device.getNickname()));
        if (device.getContentType() == null) {
            device.setContentType(ErpCloudPrintConstants.CONTENT_TYPE_PDF);
        }
        if (device.getPaperType() == null) {
            device.setPaperType(4);
        }
        if (device.getRotate() == null) {
            device.setRotate(0);
        }
        if (device.getCopies() == null || device.getCopies() <= 0) {
            device.setCopies(1);
        }
        if (device.getDefaulted() == null) {
            device.setDefaulted(false);
        }
        if (device.getQueuePaused() == null) {
            device.setQueuePaused(false);
        }
    }

    private void ensureDefaultDeviceEnabled(ErpCloudPrintDeviceDO device) {
        if (!CommonStatusEnum.ENABLE.getStatus().equals(device.getStatus())) {
            throw exception(CLOUD_PRINT_DEVICE_DISABLED, device.getNickname());
        }
    }

    private void validateDeviceNotBoundByAnyWarehouse(ErpCloudPrintDeviceDO device) {
        ErpWarehouseDO warehouse = selectBoundWarehouse(device.getId(), false);
        if (warehouse != null) {
            throw exception(CLOUD_PRINT_DEVICE_BOUND_BY_WAREHOUSE, device.getNickname(), warehouse.getName());
        }
    }

    private void validateDeviceNotBoundByEnabledWarehouse(ErpCloudPrintDeviceDO device) {
        ErpWarehouseDO warehouse = selectBoundWarehouse(device.getId(), true);
        if (warehouse != null) {
            throw exception(CLOUD_PRINT_DEVICE_BOUND_BY_WAREHOUSE, device.getNickname(), warehouse.getName());
        }
    }

    private ErpWarehouseDO selectBoundWarehouse(Long deviceId, boolean enabledOnly) {
        LambdaQueryWrapperX<ErpWarehouseDO> wrapper = new LambdaQueryWrapperX<ErpWarehouseDO>()
                .eq(ErpWarehouseDO::getCloudPrintDeviceId, deviceId)
                .orderByDesc(ErpWarehouseDO::getId)
                .last("LIMIT 1");
        if (enabledOnly) {
            wrapper.eq(ErpWarehouseDO::getStatus, CommonStatusEnum.ENABLE.getStatus());
        }
        return warehouseMapper.selectOne(wrapper);
    }

    private ErpCloudPrintDeviceRespVO toDeviceResp(ErpCloudPrintDeviceDO device) {
        ErpCloudPrintDeviceRespVO respVO = BeanUtils.toBean(device, ErpCloudPrintDeviceRespVO.class);
        ErpCloudPrintTaskDO currentTask = taskMapper.selectInFlightByDevice(device.getId());
        if (currentTask != null) {
            respVO.setCurrentTask(currentTask.getBizNo()
                    + (StringUtils.hasText(currentTask.getWarehouseName()) ? " / " + currentTask.getWarehouseName() : ""));
            respVO.setCurrentTaskStatus(currentTask.getStatus());
        }
        respVO.setPendingCount(taskMapper.selectPendingCountByDevice(device.getId()));
        ErpCloudPrintTaskDO latestCallback = taskMapper.selectLatestCallbackByDevice(device.getId());
        if (latestCallback != null) {
            respVO.setLastCallbackTime(latestCallback.getCallbackTime());
        }
        return respVO;
    }

    private List<ErpCloudPrintDeviceRespVO> fillDeviceDeptNames(List<ErpCloudPrintDeviceRespVO> list) {
        Set<Long> deptIds = list.stream()
                .map(ErpCloudPrintDeviceRespVO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return list;
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(deptIds);
        list.forEach(device -> {
            DeptRespDTO dept = deptMap.get(device.getDeptId());
            if (dept != null) {
                device.setDeptName(dept.getName());
            }
        });
        return list;
    }

    @Data
    @Accessors(chain = true)
    private static class WarehousePrintGroup {
        private ErpWarehouseDO warehouse;
        private ErpCloudPrintDeviceDO device;
        private List<Integer> itemIndexes;
        private List<ErpSaleOutItemDO> items;
    }

}
