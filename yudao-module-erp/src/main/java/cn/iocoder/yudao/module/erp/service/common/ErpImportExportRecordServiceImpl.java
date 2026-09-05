package cn.iocoder.yudao.module.erp.service.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordDetailPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordFailureDetailExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDetailDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordDetailMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportFailureDetailBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordFinishReqBO;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Validated
public class ErpImportExportRecordServiceImpl implements ErpImportExportRecordService {

    private static final int DETAIL_BATCH_SIZE = 500;
    private static final String DETAIL_TYPE_FAILURE = "FAILURE";
    private static final String ERROR_COLUMN_NAME = "错误信息";

    @Resource
    private ErpImportExportRecordMapper recordMapper;
    @Resource
    private ErpImportExportRecordDetailMapper detailMapper;

    @Override
    public Long createRecord(ErpImportExportRecordCreateReqBO reqBO) {
        ErpImportExportRecordDO record = BeanUtils.toBean(reqBO, ErpImportExportRecordDO.class);
        record.setStatus(ErpImportExportRecordStatusEnum.PROCESSING.getStatus());
        record.setTotalCount(0);
        record.setSuccessCount(0);
        record.setFailureCount(0);
        record.setCreateCount(0);
        record.setUpdateCount(0);
        record.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        record.setOperatorName(SecurityFrameworkUtils.getLoginUserNickname());
        if (record.getStartTime() == null) {
            record.setStartTime(LocalDateTime.now());
        }
        recordMapper.insert(record);
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishRecord(Long recordId, ErpImportExportRecordFinishReqBO reqBO) {
        if (recordId == null) {
            return;
        }
        ErpImportExportRecordDO update = BeanUtils.toBean(reqBO, ErpImportExportRecordDO.class);
        update.setId(recordId);
        if (update.getEndTime() == null) {
            update.setEndTime(LocalDateTime.now());
        }
        if (update.getStatus() == null) {
            update.setStatus(ErpImportExportRecordStatusEnum.ofCount(update.getSuccessCount(), update.getFailureCount()));
        }
        recordMapper.updateById(update);
        insertFailureDetails(recordId, reqBO.getFailureDetails());
    }

    @Override
    public ErpImportExportRecordRespVO getRecord(Long id) {
        return BeanUtils.toBean(recordMapper.selectById(id), ErpImportExportRecordRespVO.class);
    }

    @Override
    public PageResult<ErpImportExportRecordRespVO> getRecordPage(ErpImportExportRecordPageReqVO reqVO) {
        return BeanUtils.toBean(recordMapper.selectPage(reqVO), ErpImportExportRecordRespVO.class);
    }

    @Override
    public PageResult<ErpImportExportRecordDetailRespVO> getDetailPage(ErpImportExportRecordDetailPageReqVO reqVO) {
        return BeanUtils.toBean(detailMapper.selectPage(reqVO), ErpImportExportRecordDetailRespVO.class);
    }

    @Override
    public List<ErpImportExportRecordFailureDetailExportRespVO> getFailureDetailList(Long recordId) {
        return BeanUtils.toBean(detailMapper.selectFailureListByRecordId(recordId),
                ErpImportExportRecordFailureDetailExportRespVO.class);
    }

    @Override
    public void downloadFailureDetails(Long recordId, HttpServletResponse response) throws IOException {
        ErpImportExportRecordDO record = recordMapper.selectById(recordId);
        downloadFailureDetails(recordId, record, response);
    }

    @Override
    public void downloadOwnImportFailureDetails(Long recordId, String moduleKey, HttpServletResponse response)
            throws IOException {
        ErpImportExportRecordDO record = recordMapper.selectById(recordId);
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (record == null
                || loginUserId == null
                || !Objects.equals(loginUserId, record.getOperatorId())
                || !ErpImportExportOperationTypeEnum.IMPORT.getType().equals(record.getOperationType())
                || !Objects.equals(moduleKey, record.getModuleKey())) {
            throw new AccessDeniedException("没有下载该导入错误数据的权限");
        }
        downloadFailureDetails(recordId, record, response);
    }

    private void downloadFailureDetails(Long recordId, ErpImportExportRecordDO record, HttpServletResponse response)
            throws IOException {
        if (writeTemplateFailureDetailsIfPossible(record, response)) {
            return;
        }
        ExcelUtils.write(response, buildErrorDataFileName(record), "失败明细",
                ErpImportExportRecordFailureDetailExportRespVO.class, getFailureDetailList(recordId));
    }

    private boolean writeTemplateFailureDetailsIfPossible(ErpImportExportRecordDO record,
                                                          HttpServletResponse response) throws IOException {
        if (record == null || StrUtil.isBlank(record.getTemplateKey())) {
            return false;
        }
        try {
            Class<?> templateClass = Class.forName(record.getTemplateKey());
            List<ExcelColumn> columns = resolveExcelColumns(templateClass);
            if (CollUtil.isEmpty(columns)) {
                return false;
            }
            List<List<String>> head = buildTemplateFailureHead(columns);
            List<List<Object>> rows = buildTemplateFailureRows(record.getId(), templateClass, columns);
            ExcelUtils.writeDynamic(response, buildErrorDataFileName(record), "失败明细", head, rows);
            return true;
        } catch (ClassNotFoundException | RuntimeException | IllegalAccessException ex) {
            return false;
        }
    }

    private List<ExcelColumn> resolveExcelColumns(Class<?> templateClass) {
        List<ExcelColumn> columns = new ArrayList<>();
        Field[] fields = templateClass.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            ExcelProperty excelProperty = fields[i].getAnnotation(ExcelProperty.class);
            if (excelProperty == null || excelProperty.value().length == 0) {
                continue;
            }
            columns.add(new ExcelColumn(fields[i], excelProperty, i));
        }
        columns.sort(Comparator.comparingInt(ExcelColumn::getOrder));
        return columns;
    }

    private List<List<String>> buildTemplateFailureHead(List<ExcelColumn> columns) {
        List<List<String>> head = new ArrayList<>(columns.size() + 1);
        for (ExcelColumn column : columns) {
            head.add(Arrays.asList(column.getExcelProperty().value()));
        }
        head.add(Collections.singletonList(ERROR_COLUMN_NAME));
        return head;
    }

    private List<List<Object>> buildTemplateFailureRows(Long recordId, Class<?> templateClass,
                                                        List<ExcelColumn> columns) throws IllegalAccessException {
        List<ErpImportExportRecordDetailDO> details = detailMapper.selectListByRecordId(recordId);
        List<List<Object>> rows = new ArrayList<>(details.size());
        for (ErpImportExportRecordDetailDO detail : details) {
            Object rawData = JsonUtils.parseObject(detail.getRawData(), templateClass);
            List<Object> row = new ArrayList<>(columns.size() + 1);
            for (ExcelColumn column : columns) {
                Field field = column.getField();
                field.setAccessible(true);
                row.add(rawData == null ? null : field.get(rawData));
            }
            row.add(isFailureDetail(detail) ? detail.getFailureReason() : null);
            rows.add(row);
        }
        return rows;
    }

    private boolean isFailureDetail(ErpImportExportRecordDetailDO detail) {
        return detail.getDetailType() == null || DETAIL_TYPE_FAILURE.equals(detail.getDetailType());
    }

    private void insertFailureDetails(Long recordId, List<ErpImportExportFailureDetailBO> failureDetails) {
        if (CollUtil.isEmpty(failureDetails)) {
            return;
        }
        List<ErpImportExportRecordDetailDO> details = failureDetails.stream()
                .map(item -> ErpImportExportRecordDetailDO.builder()
                        .recordId(recordId)
                        .rowNo(item.getRowNo())
                        .groupKey(item.getGroupKey())
                        .detailType(item.getDetailType())
                        .bizKey(item.getBizKey())
                        .bizName(item.getBizName())
                        .failureReason(item.getFailureReason())
                        .rawData(item.getRawData() == null ? null : JsonUtils.toJsonString(item.getRawData()))
                        .build())
                .collect(Collectors.toList());
        detailMapper.insertBatch(details, DETAIL_BATCH_SIZE);
    }

    private String buildErrorDataFileName(ErpImportExportRecordDO record) {
        String baseName = record == null ? null : StrUtil.blankToDefault(record.getFileName(), record.getModuleName());
        baseName = StrUtil.blankToDefault(baseName, "导入");
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        return baseName + "-错误数据.xls";
    }

    private static class ExcelColumn {

        private final Field field;
        private final ExcelProperty excelProperty;
        private final int declarationOrder;

        private ExcelColumn(Field field, ExcelProperty excelProperty, int declarationOrder) {
            this.field = field;
            this.excelProperty = excelProperty;
            this.declarationOrder = declarationOrder;
        }

        public Field getField() {
            return field;
        }

        public ExcelProperty getExcelProperty() {
            return excelProperty;
        }

        public int getOrder() {
            return excelProperty.index() >= 0 ? excelProperty.index() : declarationOrder;
        }

    }

}
