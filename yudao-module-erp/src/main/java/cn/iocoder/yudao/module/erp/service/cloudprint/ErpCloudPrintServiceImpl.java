package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDevicePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDeviceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintDeviceSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo.ErpCloudPrintTaskRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintCallbackLogDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintDeviceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintTaskDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintTemplateDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintCallbackLogMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintDeviceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintTaskMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpPrintRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpPrintTemplateMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintConstants;
import cn.iocoder.yudao.module.erp.framework.cloudprint.SwPrintClient;
import cn.iocoder.yudao.module.erp.framework.cloudprint.SwPrintException;
import cn.iocoder.yudao.module.erp.framework.cloudprint.config.SwPrintProperties;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.DeviceInfo;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.PtFileData;
import cn.iocoder.yudao.module.erp.framework.cloudprint.dto.SwPrintDtos.PtFileReq;
import cn.iocoder.yudao.module.erp.service.common.ErpPrintService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
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
    private ErpCloudPrintCallbackLogMapper callbackLogMapper;
    @Resource
    private ErpSaleOutService saleOutService;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpWarehouseMapper warehouseMapper;
    @Resource
    private ErpPrintService printService;
    @Resource
    private ErpPrintRecordMapper printRecordMapper;
    @Resource
    private ErpPrintTemplateMapper printTemplateMapper;
    @Resource
    private ErpCloudPrintHtmlRenderer htmlRenderer;
    @Resource
    private FileApi fileApi;
    @Resource
    private SwPrintClient swPrintClient;
    @Resource
    private SwPrintProperties properties;
    @Resource
    private AdminUserApi adminUserApi;
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

        if (Boolean.TRUE.equals(properties.getCheckDeviceOnline())) {
            for (WarehousePrintGroup group : groups) {
                checkDeviceOnline(group.getDevice());
            }
        }

        List<ErpCloudPrintTaskRespVO> result = new ArrayList<>();
        for (WarehousePrintGroup group : groups) {
            ErpCloudPrintTaskDO runningTask = taskMapper.selectRunningByBizAndWarehouse(
                    ErpCloudPrintConstants.BIZ_TYPE_SALE_OUT, saleOutId, group.getWarehouse().getId());
            if (runningTask != null) {
                result.add(toTaskResp(runningTask));
                continue;
            }
            Map<String, Object> warehousePrintData = buildWarehousePrintData(fullPrintData, fullItemRows, group);
            result.add(toTaskResp(submitSaleOutGroup(saleOut, template, group, copies, warehousePrintData)));
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

        try {
            PtFileReq req = new PtFileReq()
                    .setDevid(device.getDevid())
                    .setReqid(reqid)
                    .setType(contentType)
                    .setWidth(device.getPrintWidth())
                    .setHeight(device.getPrintHeight())
                    .setPcopy(finalCopies)
                    .setPtype(device.getPaperType())
                    .setRotate(device.getRotate());
            task.setSubmitReq(maskSubmitReq(req));
            log.info("[submitSaleOut][销售单云打印提交 saleOutId({}) warehouseId({}) warehouseName({}) templateId({}) "
                            + "deviceId({}) type({}) fileName({})]",
                    saleOut.getId(), group.getWarehouse().getId(), group.getWarehouse().getName(),
                    template.getId(), device.getId(), contentType, fileName);
            PtFileData result = swPrintClient.ptFile(req, content, fileName);
            taskMapper.updateById(new ErpCloudPrintTaskDO()
                    .setId(task.getId())
                    .setSubmitReq(task.getSubmitReq())
                    .setSubmitResp(JsonUtils.toJsonString(result))
                    .setSubmitTime(LocalDateTime.now())
                    .setStatus(ErpCloudPrintConstants.STATUS_SUBMITTED));
            task.setStatus(ErpCloudPrintConstants.STATUS_SUBMITTED).setSubmitTime(LocalDateTime.now());
            return task;
        } catch (SwPrintException ex) {
            int status = ex.isTimeout() ? ErpCloudPrintConstants.STATUS_UNKNOWN : ErpCloudPrintConstants.STATUS_SUBMIT_FAILED;
            taskMapper.updateById(new ErpCloudPrintTaskDO()
                    .setId(task.getId())
                    .setSubmitReq(task.getSubmitReq())
                    .setSubmitTime(LocalDateTime.now())
                    .setStatus(status)
                    .setErrorMsg(ex.getMessage()));
            if (ex.isTimeout()) {
                return task.setStatus(status).setErrorMsg(ex.getMessage());
            }
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, ex.getMessage());
        } catch (Exception ex) {
            taskMapper.updateById(new ErpCloudPrintTaskDO()
                    .setId(task.getId())
                    .setStatus(ErpCloudPrintConstants.STATUS_SUBMIT_FAILED)
                    .setErrorMsg(StrUtil.maxLength(ex.getMessage(), 512)));
            throw exception(CLOUD_PRINT_SUBMIT_FAILED, ex.getMessage());
        }
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
                .setLastStatusCode(info == null ? null : info.getCode())
                .setLastStatusTime(LocalDateTime.now()));
        return getDevice(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCallback(String pathToken, String rawBody) {
        if (!StringUtils.hasText(properties.getCallbackPathToken())
                || !properties.getCallbackPathToken().equals(pathToken)) {
            insertCallbackLog(rawBody, null, false);
            return;
        }
        CallbackReq req;
        try {
            req = JsonUtils.parseObject(rawBody, CallbackReq.class);
        } catch (Exception ex) {
            insertCallbackLog(rawBody, null, false);
            throw ex;
        }
        if (req == null) {
            insertCallbackLog(rawBody, null, false);
            return;
        }
        if ("printRlt".equals(req.getMethod())) {
            handlePrintResult(rawBody, req);
        } else if ("devStatus".equals(req.getMethod())) {
            handleDeviceStatus(rawBody, req);
        } else {
            insertCallbackLog(rawBody, req, false);
        }
    }

    private void handlePrintResult(String rawBody, CallbackReq req) {
        ErpCloudPrintTaskDO task = StringUtils.hasText(req.getReqid()) ? taskMapper.selectByReqid(req.getReqid()) : null;
        insertCallbackLog(rawBody, req, task != null);
        if (task == null) {
            return;
        }
        Integer oldStatus = task.getStatus();
        Integer newStatus = Integer.valueOf(0).equals(req.getCode())
                ? ErpCloudPrintConstants.STATUS_SUCCESS : ErpCloudPrintConstants.STATUS_FAILED;
        ErpCloudPrintTaskDO updateObj = new ErpCloudPrintTaskDO()
                .setId(task.getId())
                .setCallbackCode(req.getCode())
                .setCallbackMsg(req.getMessage())
                .setCallbackTime(LocalDateTime.now());
        if (!ErpCloudPrintConstants.FINAL_STATUSES.contains(oldStatus)
                || Integer.valueOf(ErpCloudPrintConstants.STATUS_TIMEOUT).equals(oldStatus)) {
            updateObj.setStatus(newStatus);
        }
        taskMapper.updateById(updateObj);
        if (Integer.valueOf(ErpCloudPrintConstants.STATUS_SUCCESS).equals(newStatus)
                && !Integer.valueOf(ErpCloudPrintConstants.STATUS_SUCCESS).equals(oldStatus)
                && ErpCloudPrintConstants.BIZ_TYPE_SALE_OUT.equals(task.getBizType())) {
            recordSaleOutPrintSuccess(task);
        }
    }

    private void handleDeviceStatus(String rawBody, CallbackReq req) {
        ErpCloudPrintDeviceDO device = deviceMapper.selectByDevid(req.getDevid());
        insertCallbackLog(rawBody, req, device != null);
        if (device == null) {
            return;
        }
        deviceMapper.updateById(new ErpCloudPrintDeviceDO()
                .setId(device.getId())
                .setOnlineState(Integer.valueOf(0).equals(req.getCode()) ? 1 : 0)
                .setLastStatusCode(req.getCode())
                .setLastStatusTime(LocalDateTime.now()));
    }

    private void recordSaleOutPrintSuccess(ErpCloudPrintTaskDO task) {
        saleOutMapper.incrementPrintCount(task.getBizId());
        Long loginUserId = getCallbackLoginUserId();
        AdminUserRespDTO user = loginUserId == null ? null : adminUserApi.getUser(loginUserId);
        printRecordMapper.insert(ErpPrintRecordDO.builder()
                .moduleKey(ErpCloudPrintConstants.MODULE_KEY_SALE_OUT)
                .businessId(task.getBizId())
                .businessNo(task.getBizNo())
                .templateId(task.getTemplateId())
                .printerId(loginUserId)
                .printerName(user == null ? "云打印回调" : user.getNickname())
                .printTime(LocalDateTime.now())
                .build());
    }

    private Long getCallbackLoginUserId() {
        try {
            return getLoginUserId();
        } catch (Exception ex) {
            return null;
        }
    }

    private void checkDeviceOnline(ErpCloudPrintDeviceDO device) {
        DeviceInfo info = swPrintClient.getDevice(device.getDevid());
        boolean online = isDeviceOnline(info);
        log.info("[checkDeviceOnline][云打印设备在线检查 deviceId({}) nickname({}) devid({}) online({}) deviceInfo({})]",
                device.getId(), device.getNickname(), device.getDevid(), online, JsonUtils.toJsonString(info));
        deviceMapper.updateById(new ErpCloudPrintDeviceDO()
                .setId(device.getId())
                .setOnlineState(online ? 1 : 0)
                .setLastStatusCode(info.getCode())
                .setLastStatusTime(LocalDateTime.now()));
        if (!online) {
            throw exception(CLOUD_PRINT_DEVICE_OFFLINE, device.getNickname());
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

    private ErpCloudPrintDeviceDO getSubmitDevice(Long deviceId) {
        ErpCloudPrintDeviceDO device = deviceId == null ? deviceMapper.selectDefault() : deviceMapper.selectById(deviceId);
        if (device == null) {
            throw exception(deviceId == null ? CLOUD_PRINT_DEFAULT_DEVICE_NOT_EXISTS : CLOUD_PRINT_DEVICE_NOT_EXISTS);
        }
        return device;
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

    private String maskSubmitReq(PtFileReq req) {
        return JsonUtils.toJsonString(req);
    }

    private void insertCallbackLog(String rawBody, CallbackReq req, boolean matched) {
        try {
            callbackLogMapper.insert(new ErpCloudPrintCallbackLogDO()
                    .setRawBody(rawBody)
                    .setMethod(req == null ? null : req.getMethod())
                    .setDevid(req == null ? null : req.getDevid())
                    .setReqid(req == null ? null : req.getReqid())
                    .setCode(req == null ? null : req.getCode())
                    .setMatched(matched));
        } catch (Exception ex) {
            log.error("[insertCallbackLog][云打印回调日志落库失败 rawBody({})]", rawBody, ex);
        }
    }

    private ErpCloudPrintTaskRespVO toTaskResp(ErpCloudPrintTaskDO task) {
        return BeanUtils.toBean(task, ErpCloudPrintTaskRespVO.class);
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
        return BeanUtils.toBean(device, ErpCloudPrintDeviceRespVO.class);
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

    @Data
    public static class CallbackReq {
        private String method;
        private String devid;
        private String reqid;
        private Integer code;
        private String message;
    }

}
