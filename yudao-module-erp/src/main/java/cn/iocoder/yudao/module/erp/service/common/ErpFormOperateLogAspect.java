package cn.iocoder.yudao.module.erp.service.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.biz.system.logger.OperateLogContentUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_CREATE_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_DELETE_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_UPDATE_SUB_TYPE;

/**
 * ERP 表单操作日志兜底切面。
 *
 * <p>已手写 ErpOperateLogService 的服务会跳过，避免重复记录。</p>
 */
@Aspect
@Component
@Slf4j
public class ErpFormOperateLogAspect {

    private static final Map<String, String> FORM_NAME_MAP = new LinkedHashMap<>();

    static {
        FORM_NAME_MAP.put("PurchaseOrder", "采购订单");
        FORM_NAME_MAP.put("PurchaseIn", "采购入库");
        FORM_NAME_MAP.put("PurchaseReturn", "采购退货");
        FORM_NAME_MAP.put("PurchasePriceAdjust", "采购调价");
        FORM_NAME_MAP.put("PurchaseInvoice", "采购票据");
        FORM_NAME_MAP.put("Supplier", "供应商");
        FORM_NAME_MAP.put("SaleOrder", "销售订单");
        FORM_NAME_MAP.put("SaleOut", "销售出库");
        FORM_NAME_MAP.put("SaleReturn", "销售退货");
        FORM_NAME_MAP.put("SaleQuote", "销售报价");
        FORM_NAME_MAP.put("SaleCart", "销售手推车");
        FORM_NAME_MAP.put("SalePriceAdjust", "销售调价");
        FORM_NAME_MAP.put("Customer", "客户");
        FORM_NAME_MAP.put("Product", "配件信息");
        FORM_NAME_MAP.put("ProductCategory", "配件分类");
        FORM_NAME_MAP.put("ProductUnit", "配件单位");
        FORM_NAME_MAP.put("Account", "银行账户");
        FORM_NAME_MAP.put("FinanceReceipt", "收款单");
        FORM_NAME_MAP.put("FinancePayment", "付款单");
        FORM_NAME_MAP.put("FinanceTransfer", "银行转账单");
        FORM_NAME_MAP.put("ReceivableOther", "其他应收");
        FORM_NAME_MAP.put("ReceivableOtherIncome", "其他收入");
        FORM_NAME_MAP.put("PayableOther", "其他应付");
        FORM_NAME_MAP.put("PayableExpense", "费用支付");
        FORM_NAME_MAP.put("Warehouse", "仓库");
        FORM_NAME_MAP.put("StockIn", "其它入库单");
        FORM_NAME_MAP.put("StockOut", "其它出库单");
        FORM_NAME_MAP.put("StockMove", "库存调拨单");
        FORM_NAME_MAP.put("StockCheck", "库存盘点单");
    }

    @Resource
    private ErpOperateLogService operateLogService;

    @Around("execution(public * cn.iocoder.yudao.module.erp.service..*ServiceImpl.create*(..))"
            + " || execution(public * cn.iocoder.yudao.module.erp.service..*ServiceImpl.update*(..))"
            + " || execution(public * cn.iocoder.yudao.module.erp.service..*ServiceImpl.batchUpdate*(..))"
            + " || execution(public * cn.iocoder.yudao.module.erp.service..*ServiceImpl.delete*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object target = joinPoint.getTarget();
        String methodName = joinPoint.getSignature().getName();
        if (shouldSkip(target, methodName)) {
            return joinPoint.proceed();
        }
        List<Long> ids = resolveIds(joinPoint.getArgs());
        Map<Long, Object> oldSnapshots = readSnapshots(target, ids);
        Object result;
        ErpOperateLogService.beginCaptureRecord();
        ErpOperateLogService.beginSuppressSimpleRecord();
        try {
            result = joinPoint.proceed();
        } finally {
            ErpOperateLogService.endSuppressSimpleRecord();
        }
        try {
            if (!ErpOperateLogService.hasCapturedRecord()) {
                recordLog(target, methodName, joinPoint.getArgs(), result, ids, oldSnapshots);
            }
        } catch (Throwable ex) {
            log.error("[around][method({}) record operate log error]", methodName, ex);
        } finally {
            ErpOperateLogService.endCaptureRecord();
        }
        return result;
    }

    private void recordLog(Object target, String methodName, Object[] args, Object result,
                           List<Long> ids, Map<Long, Object> oldSnapshots) {
        String type = buildType(target);
        if (methodName.startsWith("create")) {
            Long bizId = result instanceof Number
                    ? Long.valueOf(((Number) result).longValue())
                    : firstId(resolveIds(args));
            if (bizId == null) {
                return;
            }
            Object newObj = firstNonNull(readSnapshot(target, bizId), firstArg(args));
            operateLogService.record(type, ERP_CREATE_SUB_TYPE, bizId,
                    OperateLogContentUtils.buildErpFormCreateAction(newObj, bizId, null), null);
            return;
        }
        if (methodName.startsWith("delete")) {
            oldSnapshots.forEach((id, oldObj) -> operateLogService.record(type, ERP_DELETE_SUB_TYPE, id,
                    OperateLogContentUtils.buildErpFormDeleteAction(oldObj, id, null), null));
            return;
        }
        if (methodName.startsWith("update") || methodName.startsWith("batchUpdate")) {
            if (CollUtil.isEmpty(ids)) {
                return;
            }
            ids.forEach(id -> {
                Object oldObj = oldSnapshots.get(id);
                Object newObj = firstNonNull(readSnapshot(target, id), firstArg(args));
                operateLogService.record(type, ERP_UPDATE_SUB_TYPE, id,
                        OperateLogContentUtils.buildErpFormUpdateAction(oldObj, newObj, id, null), null);
            });
        }
    }

    private boolean shouldSkip(Object target, String methodName) {
        if (target == null || StrUtil.isBlank(methodName)) {
            return true;
        }
        Class<?> targetClass = target.getClass();
        String className = targetClass.getName();
        String simpleName = targetClass.getSimpleName();
        if (className.contains(".statistics.") || className.contains(".service.common.")
                || simpleName.contains("Report") || simpleName.contains("History")
                || methodName.contains("Report") || methodName.contains("Template")) {
            return true;
        }
        return false;
    }

    private Map<Long, Object> readSnapshots(Object target, List<Long> ids) {
        Map<Long, Object> snapshots = new LinkedHashMap<>();
        if (CollUtil.isEmpty(ids)) {
            return snapshots;
        }
        ids.forEach(id -> snapshots.put(id, readSnapshot(target, id)));
        return snapshots;
    }

    private Object readSnapshot(Object target, Long id) {
        if (target == null || id == null) {
            return null;
        }
        String preferredMapperName = buildPreferredMapperName(target);
        Object snapshot = readSnapshot(target, id, preferredMapperName, true);
        if (snapshot != null) {
            return snapshot;
        }
        return readSnapshot(target, id, preferredMapperName, false);
    }

    private Object readSnapshot(Object target, Long id, String preferredMapperName, boolean preferredOnly) {
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.getName().endsWith("Mapper")) {
                    continue;
                }
                if (preferredOnly && !field.getName().equals(preferredMapperName)) {
                    continue;
                }
                if (!preferredOnly && field.getName().equals(preferredMapperName)) {
                    continue;
                }
                Object mapper = getFieldValue(target, field);
                Object snapshot = invokeSelectById(mapper, id);
                if (snapshot != null) {
                    return snapshot;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private String buildPreferredMapperName(Object target) {
        String name = target.getClass().getSimpleName();
        name = name.replace("$$EnhancerBySpringCGLIB", "");
        name = name.replace("Erp", "").replace("ServiceImpl", "");
        return Character.toLowerCase(name.charAt(0)) + name.substring(1) + "Mapper";
    }

    private Object getFieldValue(Object target, Field field) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private Object invokeSelectById(Object mapper, Long id) {
        if (mapper == null) {
            return null;
        }
        for (Method method : mapper.getClass().getMethods()) {
            if (!"selectById".equals(method.getName()) || method.getParameterTypes().length != 1) {
                continue;
            }
            try {
                return method.invoke(mapper, id);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private List<Long> resolveIds(Object[] args) {
        List<Long> ids = new ArrayList<>();
        if (args == null) {
            return ids;
        }
        for (Object arg : args) {
            collectIds(arg, ids);
        }
        return ids;
    }

    private void collectIds(Object value, List<Long> ids) {
        if (value == null) {
            return;
        }
        if (value instanceof Number) {
            ids.add(((Number) value).longValue());
            return;
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                collectIds(item, ids);
            }
            return;
        }
        Object id = readBeanProperty(value, "id");
        if (id instanceof Number) {
            ids.add(((Number) id).longValue());
            return;
        }
        Object idList = readBeanProperty(value, "ids");
        if (idList instanceof Collection) {
            collectIds(idList, ids);
        }
    }

    private Object readBeanProperty(Object bean, String property) {
        if (bean == null) {
            return null;
        }
        String methodName = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        try {
            Method method = bean.getClass().getMethod(methodName);
            return method.invoke(bean);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long firstId(List<Long> ids) {
        return CollUtil.isEmpty(ids) ? null : ids.get(0);
    }

    private Object firstArg(Object[] args) {
        return args == null || args.length == 0 ? null : args[0];
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String buildType(Object target) {
        String name = target.getClass().getSimpleName();
        name = name.replace("$$EnhancerBySpringCGLIB", "");
        name = name.replace("Erp", "").replace("ServiceImpl", "");
        return FORM_NAME_MAP.getOrDefault(name, name);
    }

}
