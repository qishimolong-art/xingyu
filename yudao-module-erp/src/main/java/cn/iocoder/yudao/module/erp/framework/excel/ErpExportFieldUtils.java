package cn.iocoder.yudao.module.erp.framework.excel;

import cn.hutool.core.collection.CollUtil;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * ERP export field metadata and include-column resolution helpers.
 */
public class ErpExportFieldUtils {

    private static final ErrorCode EXPORT_FIELD_EMPTY = new ErrorCode(1_030_000_001, "请至少选择一个导出字段");

    private static final Set<String> ERP_DOCUMENT_TAX_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "taxPercent", "taxRate", "taxPrice", "totalTaxPrice",
            "taxAmount", "taxExclusiveAmount", "taxExclusivePrice",
            "taxInclusivePrice", "taxInclusiveAmount"));
    private static final List<String> ERP_DOCUMENT_EXPORT_PACKAGES = Arrays.asList(
            ".purchase.vo.order.", ".purchase.vo.in.", ".purchase.vo.returns.", ".purchase.vo.invoice.",
            ".sale.vo.quote.", ".sale.vo.order.", ".sale.vo.cart.", ".sale.vo.out.", ".sale.vo.returns.");

    private ErpExportFieldUtils() {
    }

    public static List<ErpExportFieldRespVO> listFields(Class<?> head,
                                                        Map<String, String> groupMap,
                                                        Collection<String> hiddenFields,
                                                        Map<String, String> permissionFieldMap) {
        Set<String> hiddenFieldSet = toSet(hiddenFields);
        List<Field> excelFields = getExcelFields(head);
        List<ErpExportFieldRespVO> result = new ArrayList<>(excelFields.size());
        for (int i = 0; i < excelFields.size(); i++) {
            Field field = excelFields.get(i);
            if (isErpDocumentTaxField(head, field.getName())) {
                continue;
            }
            if (isHidden(field.getName(), hiddenFieldSet, permissionFieldMap)) {
                continue;
            }
            ErpExportFieldRespVO respVO = new ErpExportFieldRespVO();
            respVO.setField(field.getName());
            respVO.setLabel(getExcelLabel(field));
            respVO.setGroup(getGroup(field.getName(), groupMap));
            respVO.setGroupLabel(getGroupLabel(respVO.getGroup()));
            respVO.setRequired(false);
            respVO.setSort((i + 1) * 10);
            result.add(respVO);
        }
        return result;
    }

    public static Set<String> resolveIncludeFields(Class<?> head,
                                                   Collection<String> requestedFields,
                                                   Collection<String> hiddenFields,
                                                   Map<String, String> permissionFieldMap) {
        Set<String> orderedAllFields = new LinkedHashSet<>();
        for (Field field : getExcelFields(head)) {
            if (isErpDocumentTaxField(head, field.getName())) {
                continue;
            }
            orderedAllFields.add(field.getName());
        }
        Set<String> hiddenFieldSet = toSet(hiddenFields);
        Set<String> visibleFields = new LinkedHashSet<>();
        for (String field : orderedAllFields) {
            if (!isHidden(field, hiddenFieldSet, permissionFieldMap)) {
                visibleFields.add(field);
            }
        }
        if (CollUtil.isEmpty(visibleFields)) {
            throw exception(EXPORT_FIELD_EMPTY);
        }
        Set<String> requestedFieldSet = normalizeRequestedFields(requestedFields);
        if (CollUtil.isEmpty(requestedFieldSet)) {
            return visibleFields.size() == orderedAllFields.size() ? null : visibleFields;
        }
        Set<String> includeFields = new LinkedHashSet<>();
        for (String field : visibleFields) {
            if (requestedFieldSet.contains(field)) {
                includeFields.add(field);
            }
        }
        if (CollUtil.isEmpty(includeFields)) {
            throw exception(EXPORT_FIELD_EMPTY);
        }
        return includeFields;
    }

    public static Set<String> parseFieldParam(String fields) {
        if (fields == null || fields.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(field -> !field.isEmpty())
                .forEach(result::add);
        return result;
    }

    private static List<Field> getExcelFields(Class<?> head) {
        List<Field> result = new ArrayList<>();
        Class<?> current = head;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getAnnotation(ExcelProperty.class) != null) {
                    result.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return result;
    }

    private static String getExcelLabel(Field field) {
        ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
        if (excelProperty == null || excelProperty.value().length == 0) {
            return field.getName();
        }
        return excelProperty.value()[excelProperty.value().length - 1];
    }

    private static String getGroup(String field, Map<String, String> groupMap) {
        if (groupMap == null || !groupMap.containsKey(field)) {
            return "other";
        }
        return groupMap.get(field);
    }

    private static String getGroupLabel(String group) {
        if ("main".equals(group)) {
            return "单据信息";
        }
        if ("detail".equals(group)) {
            return "明细信息";
        }
        if ("system".equals(group)) {
            return "系统信息";
        }
        if ("category_purchase".equals(group)) {
            return "采购分类";
        }
        if ("settle_logistics".equals(group)) {
            return "结算物流";
        }
        if ("address_info".equals(group)) {
            return "地址信息";
        }
        if ("invoice_info".equals(group)) {
            return "开票信息";
        }
        if ("finance_info".equals(group)) {
            return "财务信息";
        }
        return "其他信息";
    }

    private static Set<String> normalizeRequestedFields(Collection<String> requestedFields) {
        if (CollUtil.isEmpty(requestedFields)) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        requestedFields.stream()
                .filter(field -> field != null && !field.trim().isEmpty())
                .map(String::trim)
                .forEach(result::add);
        return result;
    }

    private static Set<String> toSet(Collection<String> fields) {
        if (CollUtil.isEmpty(fields)) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(fields);
    }

    private static boolean isHidden(String exportField,
                                    Set<String> hiddenFields,
                                    Map<String, String> permissionFieldMap) {
        if (CollUtil.isEmpty(hiddenFields)) {
            return false;
        }
        if (hiddenFields.contains(exportField) || hiddenFields.contains("col_" + exportField)) {
            return true;
        }
        String permissionField = permissionFieldMap == null ? null : permissionFieldMap.get(exportField);
        if (permissionField != null
                && (hiddenFields.contains(permissionField) || hiddenFields.contains("col_" + permissionField))) {
            return true;
        }
        if (exportField.endsWith("Name")) {
            String idField = exportField.substring(0, exportField.length() - "Name".length()) + "Id";
            return hiddenFields.contains(idField) || hiddenFields.contains("col_" + idField);
        }
        return false;
    }

    private static boolean isErpDocumentTaxField(Class<?> head, String fieldName) {
        if (!ERP_DOCUMENT_TAX_FIELDS.contains(fieldName)) {
            return false;
        }
        String className = head.getName();
        for (String packagePart : ERP_DOCUMENT_EXPORT_PACKAGES) {
            if (className.contains(packagePart)) {
                return true;
            }
        }
        return false;
    }

}
