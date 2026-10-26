package cn.iocoder.yudao.module.erp.controller.admin.product;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
public final class ErpProductExportSupport {
    private ErpProductExportSupport() { }
    static String permissionKey(String field) {
        switch (field) {
            case "productCode": return "code";
            case "deptName": return "deptIds";
            case "categoryName": return "categoryId";
            case "unitName": return "unitId";
            case "defaultWarehouseName": return "defaultWarehouseId";
            case "createDeptName": return "createDeptId";
            case "creatorName": return "creator";
            case "updaterName": return "updater";
            default: return field;
        }
    }
    static boolean hidden(Set<String> hidden, String field) {
        String key = permissionKey(field);
        return ("deptName".equals(field) && (hidden.contains("deptId") || hidden.contains("col_deptId")))
                || hidden.contains(key) || hidden.contains("col_" + key)
                || hidden.contains(field) || hidden.contains("col_" + field)
                || hidden.contains("customFields__" + field) || hidden.contains("col_customFields__" + field);
    }
    static List<ErpExportFieldRespVO> fields(Set<String> hidden, List<ErpFieldConfigDO> configs) {
        List<ErpExportFieldRespVO> result = new ArrayList<>();
        for (Field field : ErpProductExportRespVO.class.getDeclaredFields()) {
            ExcelProperty label = field.getAnnotation(ExcelProperty.class);
            if (label == null || hidden(hidden, field.getName())) continue;
            result.add(metadata(field.getName(), label.value()[0], "main", result.size()));
        }
        for (ErpFieldConfigDO config : configs) {
            if (!"CUSTOM".equalsIgnoreCase(config.getFieldSource()) || Boolean.FALSE.equals(config.getVisible())
                    || hidden(hidden, config.getFieldName())) continue;
            result.add(metadata(config.getFieldName(), config.getFieldLabel(), config.getFieldGroup(), result.size()));
        }
        return result;
    }
    private static ErpExportFieldRespVO metadata(String key, String label, String group, int sort) {
        ErpExportFieldRespVO field = new ErpExportFieldRespVO();
        field.setField(key); field.setLabel(label); field.setGroup(group == null ? "main" : group);
        field.setGroupLabel("price_info".equals(group) ? "价格信息" : "extend_info".equals(group) ? "拓展信息" : "配件信息");
        field.setSort(sort); field.setRequired(false); return field;
    }
    static List<ErpExportFieldRespVO> select(List<ErpExportFieldRespVO> allowed, String requested, Set<String> defaults) {
        Set<String> names = new LinkedHashSet<>();
        allowed.forEach(f -> names.add(f.getField()));
        Set<String> selected = requested == null ? new LinkedHashSet<>(defaults)
                : new LinkedHashSet<>(Arrays.asList(requested.split(",", -1)));
        if (requested != null && (selected.isEmpty() || !names.containsAll(selected)))
            throw new IllegalArgumentException("包含未知或无权限的导出字段");
        List<ErpExportFieldRespVO> result = new ArrayList<>();
        for (ErpExportFieldRespVO field : allowed) if (selected.contains(field.getField())) result.add(field);
        if (result.isEmpty()) throw new IllegalArgumentException("请至少选择一个导出字段");
        return result;
    }
    static void write(HttpServletResponse response, List<ErpExportFieldRespVO> fields,
            List<ErpFieldConfigDO> configs, IntFunction<List<ErpProductRespVO>> loader) throws IOException {
        Path temporary = Files.createTempFile("erp-product-export-", ".xlsx");
        try {
            int count = write(temporary, fields, configs, loader, ignored -> { });
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8("配件信息.xlsx"));
            Files.copy(temporary, response.getOutputStream());
            Set<String> keys = new LinkedHashSet<>(); fields.forEach(f -> keys.add(f.getField()));
            ExcelUtils.recordStreamedWrite("配件信息.xlsx", count, keys);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static int write(Path target, List<ErpExportFieldRespVO> fields,
            List<ErpFieldConfigDO> configs, IntFunction<List<ErpProductRespVO>> loader,
            IntConsumer progressConsumer) throws IOException {
        Map<String, ErpFieldConfigDO> custom = new HashMap<>();
        configs.forEach(f -> { if ("CUSTOM".equalsIgnoreCase(f.getFieldSource())) custom.put(f.getFieldName(), f); });
        List<List<String>> head = new ArrayList<>();
        fields.forEach(f -> head.add(Collections.singletonList(f.getLabel())));
        int count = 0;
        try (OutputStream output = Files.newOutputStream(target);
                 ExcelWriter writer = FastExcelFactory.write(output).head(head).autoCloseStream(false)
                    .registerWriteHandler(new cn.idev.excel.write.handler.CellWriteHandler() {
                        private final Map<String,org.apache.poi.ss.usermodel.CellStyle> styles = new HashMap<>();
                        @Override public void afterCellDispose(cn.idev.excel.write.metadata.holder.WriteSheetHolder sh,
                                cn.idev.excel.write.metadata.holder.WriteTableHolder th,
                                List<cn.idev.excel.metadata.data.WriteCellData<?>> data, org.apache.poi.ss.usermodel.Cell cell,
                                cn.idev.excel.metadata.Head h, Integer rowIndex, Boolean isHead) {
                            if (Boolean.TRUE.equals(isHead)) return;
                            String key = fields.get(cell.getColumnIndex()).getField();
                            ErpFieldConfigDO config = custom.get(key);
                            String format = isMoney(key) ? "0.00" : "General";
                            if (config != null && "DECIMAL".equals(config.getFieldType())) {
                                int scale = config.getDecimalScale() == null ? 2 : config.getDecimalScale();
                                format = scale == 0 ? "0" : "0." + String.join("", Collections.nCopies(Math.min(20, Math.max(0,scale)), "0"));
                            }
                            final String fmt = format;
                            cell.setCellStyle(styles.computeIfAbsent(fmt, ignored -> {
                                org.apache.poi.ss.usermodel.CellStyle style = cell.getSheet().getWorkbook().createCellStyle();
                                style.setDataFormat(cell.getSheet().getWorkbook().createDataFormat().getFormat(fmt)); return style;
                            }));
                        }
                    }).build()) {
                WriteSheet sheet = FastExcelFactory.writerSheet("配件信息").build();
                List<ErpProductRespVO> batch;
                int page = 1;
                do {
                    batch = loader.apply(page++);
                    List<List<Object>> rows = new ArrayList<>();
                    for (ErpProductRespVO row : batch) {
                        ErpProductExportRespVO model = BeanUtils.toBean(row, ErpProductExportRespVO.class);
                        List<Object> cells = new ArrayList<>();
                        for (ErpExportFieldRespVO field : fields) {
                            String key = field.getField(); Object value;
                            if (custom.containsKey(key)) value = row.getCustomFields() == null ? null : row.getCustomFields().get(key);
                            else try { Field f = ErpProductExportRespVO.class.getDeclaredField(key); f.setAccessible(true); value = f.get(model); }
                            catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
                            if (value instanceof TemporalAccessor) value = value.toString().replace('T', ' ');
                            if (value instanceof Number && isMoney(key)) value = new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
                            ErpFieldConfigDO config = custom.get(key);
                            if (value != null && config != null && ("DECIMAL".equals(config.getFieldType()) || "INTEGER".equals(config.getFieldType())))
                                value = new BigDecimal(value.toString()).setScale("INTEGER".equals(config.getFieldType()) ? 0 : config.getDecimalScale() == null ? 2 : config.getDecimalScale(), RoundingMode.HALF_UP);
                            cells.add(value);
                        }
                        rows.add(cells);
                    }
                    writer.write(rows, sheet);
                    count += rows.size();
                    progressConsumer.accept(count);
                } while (batch.size() == 500);
        }
        return count;
    }
    private static boolean isMoney(String field) { return field.toLowerCase(Locale.ROOT).contains("price"); }
}
