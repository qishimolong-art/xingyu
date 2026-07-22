package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportFailureDetailBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordFinishReqBO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Records ERP business import/export executions without changing each controller's core logic.
 */
@Aspect
@Component
@Slf4j
public class ErpImportExportRecordAspect {

    private static final String UNKNOWN_MODULE_KEY = "erp_unknown";
    private static final String DEFAULT_EXPORT_FIELDS = "默认导出字段";

    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private ErpExportCaptchaService exportCaptchaService;

    @Around("within(cn.iocoder.yudao.module.erp.controller.admin..*)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        OperationMeta meta = resolveOperationMeta(joinPoint, method);
        if (meta == null) {
            return joinPoint.proceed();
        }
        validateExportCaptchaBeforeRecording(meta, method, joinPoint.getArgs());
        ExcelUtils.clearLastOperation();
        long startMillis = System.currentTimeMillis();
        Long recordId = createRecordSafely(buildCreateReq(meta, joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            finishRecordSafely(recordId, buildSuccessFinishReq(meta, result, startMillis, joinPoint.getArgs()));
            return result;
        } catch (Throwable ex) {
            finishRecordSafely(recordId, buildFailureFinishReq(ex, startMillis));
            throw ex;
        } finally {
            ExcelUtils.clearLastOperation();
        }
    }

    private void validateExportCaptchaBeforeRecording(OperationMeta meta, Method method, Object[] args) {
        if (!meta.export || !hasRequestParam(method, "captchaCode") && !hasRequestParam(method, "verifyCode")) {
            return;
        }
        exportCaptchaService.validate(stringValue(getRequestParamValue(method, args, "captchaCode")),
                stringValue(getRequestParamValue(method, args, "verifyCode")));
    }

    private OperationMeta resolveOperationMeta(ProceedingJoinPoint joinPoint, Method method) {
        String methodName = method.getName();
        String path = getMethodPath(method);
        if (isExportMethod(method, methodName, path)) {
            return new OperationMeta(ErpImportExportOperationTypeEnum.EXPORT.getType(),
                    moduleKey(joinPoint), moduleName(joinPoint), true);
        }
        if (isBusinessImportMethod(method, methodName, path)) {
            return new OperationMeta(ErpImportExportOperationTypeEnum.IMPORT.getType(),
                    moduleKey(joinPoint), moduleName(joinPoint), false);
        }
        return null;
    }

    private boolean isExportMethod(Method method, String methodName, String path) {
        if (method.getAnnotation(GetMapping.class) == null) {
            return false;
        }
        return containsAny(path, "export-excel", "export-detail")
                || methodName.startsWith("export") && methodName.endsWith("Excel");
    }

    private boolean isBusinessImportMethod(Method method, String methodName, String path) {
        if (method.getAnnotation(PostMapping.class) == null) {
            return false;
        }
        String normalizedPath = normalizePath(path);
        if (containsAny(normalizedPath, "parse", "detail-import") || methodName.startsWith("parse")) {
            return false;
        }
        return normalizedPath.endsWith("/import") || normalizedPath.endsWith("/import-order")
                || methodName.startsWith("import");
    }

    private ErpImportExportRecordCreateReqBO buildCreateReq(OperationMeta meta, Object[] args) {
        ErpImportExportRecordCreateReqBO reqBO = new ErpImportExportRecordCreateReqBO();
        reqBO.setOperationType(meta.operationType);
        reqBO.setModuleKey(meta.moduleKey);
        reqBO.setModuleName(meta.moduleName);
        reqBO.setFileName(resolveFileName(meta, args));
        reqBO.setFileType(getFileType(reqBO.getFileName()));
        if (meta.export) {
            reqBO.setQueryParams(JsonUtils.toJsonString(buildQueryParams(args)));
            reqBO.setExportFields(resolveExportFields(args));
        }
        reqBO.setStartTime(LocalDateTime.now());
        return reqBO;
    }

    private ErpImportExportRecordFinishReqBO buildSuccessFinishReq(OperationMeta meta, Object result,
                                                                   long startMillis, Object[] args) {
        if (meta.export) {
            ExcelUtils.ExcelOperationContext context = ExcelUtils.getLastOperation();
            int count = context == null || context.getDataCount() == null ? 0 : context.getDataCount();
            ErpImportExportRecordFinishReqBO reqBO = basicFinishReq(startMillis);
            String fileName = resolveFileName(meta, args);
            reqBO.setFileName(fileName);
            reqBO.setFileType(getFileType(fileName));
            reqBO.setStatus(ErpImportExportRecordStatusEnum.SUCCESS.getStatus());
            reqBO.setTotalCount(count);
            reqBO.setSuccessCount(count);
            reqBO.setFailureCount(0);
            reqBO.setCreateCount(0);
            reqBO.setUpdateCount(0);
            reqBO.setExportFields(resolveExportFields(args));
            return reqBO;
        }
        Object data = unwrapCommonResultData(result);
        ImportResult importResult = parseImportResult(data);
        ErpImportExportRecordFinishReqBO reqBO = basicFinishReq(startMillis);
        reqBO.setStatus(ErpImportExportRecordStatusEnum.ofCount(importResult.successCount, importResult.failureCount));
        reqBO.setTotalCount(importResult.successCount + importResult.failureCount);
        reqBO.setSuccessCount(importResult.successCount);
        reqBO.setFailureCount(importResult.failureCount);
        reqBO.setCreateCount(importResult.createCount);
        reqBO.setUpdateCount(importResult.updateCount);
        reqBO.setFailureDetails(importResult.failureDetails);
        return reqBO;
    }

    private ErpImportExportRecordFinishReqBO buildFailureFinishReq(Throwable ex, long startMillis) {
        ErpImportExportRecordFinishReqBO reqBO = basicFinishReq(startMillis);
        reqBO.setStatus(ErpImportExportRecordStatusEnum.FAILURE.getStatus());
        reqBO.setTotalCount(0);
        reqBO.setSuccessCount(0);
        reqBO.setFailureCount(1);
        reqBO.setCreateCount(0);
        reqBO.setUpdateCount(0);
        reqBO.setErrorMessage(briefErrorMessage(ex));
        return reqBO;
    }

    private ErpImportExportRecordFinishReqBO basicFinishReq(long startMillis) {
        ErpImportExportRecordFinishReqBO reqBO = new ErpImportExportRecordFinishReqBO();
        reqBO.setDurationMs(System.currentTimeMillis() - startMillis);
        reqBO.setEndTime(LocalDateTime.now());
        return reqBO;
    }

    private ImportResult parseImportResult(Object data) {
        ImportResult result = new ImportResult();
        if (data == null) {
            return result;
        }
        if (data instanceof Boolean) {
            result.successCount = Boolean.TRUE.equals(data) ? readCountFromExcelContext() : 0;
            result.failureCount = Boolean.TRUE.equals(data) ? 0 : 1;
            return result;
        }
        result.successCount = intField(data, "successCount", 0);
        result.failureCount = intField(data, "failureCount", 0);
        result.createCount = intField(data, "createCount", 0);
        result.updateCount = intField(data, "updateCount", 0);
        if (result.successCount == 0) {
            result.successCount = collectionSizeField(data, "createdCodes") + collectionSizeField(data, "updatedCodes");
        }
        if (result.failureCount == 0) {
            result.failureCount = collectionSizeField(data, "failures");
        }
        if (result.createCount == 0) {
            result.createCount = collectionSizeField(data, "createdCodes");
        }
        if (result.updateCount == 0) {
            result.updateCount = collectionSizeField(data, "updatedCodes");
        }
        result.failureDetails = parseFailureDetails(data);
        return result;
    }

    private List<ErpImportExportFailureDetailBO> parseFailureDetails(Object data) {
        Object details = fieldValue(data, "failureDetails");
        if (details == null) {
            details = fieldValue(data, "failures");
        }
        if (!(details instanceof Collection<?>)) {
            return null;
        }
        List<ErpImportExportFailureDetailBO> list = new ArrayList<>();
        for (Object item : (Collection<?>) details) {
            list.add(new ErpImportExportFailureDetailBO(
                    integerValue(firstFieldValue(item, "rowNo", "rowNum")),
                    stringValue(firstFieldValue(item, "code", "productCode", "subjectCode", "name", "orderNo")),
                    stringValue(firstFieldValue(item, "name", "bizName")),
                    stringValue(firstFieldValue(item, "reason", "failureReason", "message")),
                    item));
        }
        return list;
    }

    private Object unwrapCommonResultData(Object result) {
        if (result instanceof CommonResult<?>) {
            return ((CommonResult<?>) result).getData();
        }
        return result;
    }

    private String resolveFileName(OperationMeta meta, Object[] args) {
        if (!meta.export) {
            MultipartFile file = findArg(args, MultipartFile.class);
            return file == null ? null : file.getOriginalFilename();
        }
        ExcelUtils.ExcelOperationContext context = ExcelUtils.getLastOperation();
        return context == null ? meta.moduleName + ".xls" : context.getFilename();
    }

    private String resolveExportFields(Object[] args) {
        Map<String, Object> params = buildQueryParams(args);
        Object fields = params.get("fields");
        if (fields == null) {
            ExcelUtils.ExcelOperationContext context = ExcelUtils.getLastOperation();
            Set<String> includeFields = context == null ? null : context.getIncludeColumnFieldNames();
            return includeFields == null || includeFields.isEmpty() ? DEFAULT_EXPORT_FIELDS : JsonUtils.toJsonString(includeFields);
        }
        return fields instanceof Collection<?> ? JsonUtils.toJsonString(fields) : String.valueOf(fields);
    }

    private Map<String, Object> buildQueryParams(Object[] args) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (Object arg : args) {
            if (shouldSkipQueryParamArg(arg)) {
                continue;
            }
            if (isSimpleValue(arg)) {
                continue;
            }
            Map<String, Object> argParams = JsonUtils.parseObject(JsonUtils.toJsonString(arg), Map.class);
            if (argParams != null) {
                params.putAll(argParams);
            }
        }
        return params;
    }

    private boolean shouldSkipQueryParamArg(Object arg) {
        if (arg == null || arg instanceof MultipartFile || arg instanceof Collection<?> || arg instanceof Map<?, ?>
                || arg.getClass().isArray() || arg instanceof ServletRequest || arg instanceof ServletResponse) {
            return true;
        }
        String className = arg.getClass().getName();
        return className.startsWith("javax.servlet.")
                || className.startsWith("jakarta.servlet.")
                || className.startsWith("org.springframework.web.");
    }

    private String moduleKey(ProceedingJoinPoint joinPoint) {
        RequestMapping mapping = joinPoint.getTarget().getClass().getAnnotation(RequestMapping.class);
        if (mapping == null || mapping.value().length == 0) {
            return UNKNOWN_MODULE_KEY;
        }
        String path = mapping.value()[0].replaceFirst("^/erp/", "").replace("/", "-");
        return "erp_" + path.replace("-", "_");
    }

    private String moduleName(ProceedingJoinPoint joinPoint) {
        io.swagger.v3.oas.annotations.tags.Tag tag = joinPoint.getTarget().getClass()
                .getAnnotation(io.swagger.v3.oas.annotations.tags.Tag.class);
        if (tag == null || tag.name() == null || tag.name().isEmpty()) {
            return moduleKey(joinPoint);
        }
        String name = tag.name();
        int index = name.lastIndexOf("ERP ");
        return index >= 0 ? name.substring(index + 4).trim() : name;
    }

    private String getMethodPath(Method method) {
        String path = firstMappingPath(method.getAnnotation(GetMapping.class));
        if (path != null) {
            return path;
        }
        path = firstMappingPath(method.getAnnotation(PostMapping.class));
        return path == null ? "" : path;
    }

    private boolean hasRequestParam(Method method, String name) {
        Annotation[][] annotations = method.getParameterAnnotations();
        for (Annotation[] parameterAnnotations : annotations) {
            for (Annotation annotation : parameterAnnotations) {
                if (!(annotation instanceof RequestParam)) {
                    continue;
                }
                RequestParam requestParam = (RequestParam) annotation;
                if (name.equals(requestParam.value()) || name.equals(requestParam.name())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Object getRequestParamValue(Method method, Object[] args, String name) {
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            for (Annotation annotation : annotations[i]) {
                if (!(annotation instanceof RequestParam)) {
                    continue;
                }
                RequestParam requestParam = (RequestParam) annotation;
                if (name.equals(requestParam.value()) || name.equals(requestParam.name())) {
                    return i < args.length ? args[i] : "";
                }
            }
        }
        return null;
    }

    private String firstMappingPath(Annotation annotation) {
        if (annotation == null) {
            return null;
        }
        try {
            String[] values = (String[]) annotation.annotationType().getMethod("value").invoke(annotation);
            return values.length == 0 ? "" : values[0];
        } catch (Exception ex) {
            return "";
        }
    }

    private Long createRecordSafely(ErpImportExportRecordCreateReqBO reqBO) {
        try {
            return importExportRecordService.createRecord(reqBO);
        } catch (Exception ex) {
            log.error("[createRecordSafely][operationType({}) moduleKey({}) error]",
                    reqBO.getOperationType(), reqBO.getModuleKey(), ex);
            return null;
        }
    }

    private void finishRecordSafely(Long recordId, ErpImportExportRecordFinishReqBO reqBO) {
        if (recordId == null) {
            return;
        }
        try {
            importExportRecordService.finishRecord(recordId, reqBO);
        } catch (Exception ex) {
            log.error("[finishRecordSafely][recordId({}) error]", recordId, ex);
        }
    }

    private int readCountFromExcelContext() {
        ExcelUtils.ExcelOperationContext context = ExcelUtils.getLastOperation();
        return context == null || context.getDataCount() == null ? 0 : context.getDataCount();
    }

    private Object firstFieldValue(Object bean, String... names) {
        for (String name : names) {
            Object value = fieldValue(bean, name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Object fieldValue(Object bean, String name) {
        if (bean == null || name == null) {
            return null;
        }
        Class<?> clazz = bean.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(bean);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    private int intField(Object bean, String name, int defaultValue) {
        Integer value = integerValue(fieldValue(bean, name));
        return value == null ? defaultValue : value;
    }

    private int collectionSizeField(Object bean, String name) {
        Object value = fieldValue(bean, name);
        return value instanceof Collection<?> ? ((Collection<?>) value).size() : 0;
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean containsAny(String value, String... targets) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (String target : targets) {
            if (lower.contains(target)) {
                return true;
            }
        }
        return false;
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private boolean isSimpleValue(Object value) {
        Class<?> clazz = value.getClass();
        return clazz.isPrimitive() || value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof Enum<?>;
    }

    private <T> T findArg(Object[] args, Class<T> clazz) {
        for (Object arg : args) {
            if (clazz.isInstance(arg)) {
                return clazz.cast(arg);
            }
        }
        return null;
    }

    private String getFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
    }

    private String briefErrorMessage(Throwable ex) {
        String message = ex.getMessage();
        if (message == null || message.length() <= 1000) {
            return message;
        }
        return message.substring(0, 1000);
    }

    private static class OperationMeta {

        private final String operationType;
        private final String moduleKey;
        private final String moduleName;
        private final boolean export;

        private OperationMeta(String operationType, String moduleKey, String moduleName, boolean export) {
            this.operationType = operationType;
            this.moduleKey = moduleKey;
            this.moduleName = moduleName;
            this.export = export;
        }
    }

    private static class ImportResult {

        private int successCount;
        private int failureCount;
        private int createCount;
        private int updateCount;
        private List<ErpImportExportFailureDetailBO> failureDetails;
    }
}
