package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/** Opt-in bounded, permission-filtered export for the three movement pages. */
final class ErpStockMovementExportSupport {
    private ErpStockMovementExportSupport() {}
    static boolean detail(String key) { return key.startsWith("item") && !key.equals("itemCount"); }

    static Set<String> hidden(Class<?> head, Set<String> original, Map<String, String> aliases) {
        Set<String> result = new HashSet<>(original == null ? Collections.emptySet() : original);
        for (Field field : head.getDeclaredFields()) {
            String key = field.getName();
            Set<String> dependencies = new HashSet<>();
            dependencies.add(key);
            dependencies.add(aliases.getOrDefault(key, key));
            String value = detail(key) ? Character.toLowerCase(key.charAt(4)) + key.substring(5) : key;
            if (detail(key)) {
                dependencies.add("item_" + (value.equals("quantity") ? "count" : value));
                if (value.startsWith("product") && !value.equals("productPrice")) dependencies.add("item_productId");
                if (value.startsWith("fromWarehouse")) dependencies.add("item_fromWarehouseId");
                if (value.startsWith("toWarehouse")) dependencies.add("item_toWarehouseId");
                if (value.equals("pieceCount")) dependencies.addAll(Arrays.asList("item_count", "item_packageQty"));
                if (value.equals("totalPrice")) dependencies.addAll(Arrays.asList("item_productPrice", "item_count"));
                if (value.equals("costAmount")) dependencies.addAll(Arrays.asList("item_costPrice", "item_count"));
            } else {
                if (key.equals("creatorName")) dependencies.add("creator");
                if (key.equals("updaterName")) dependencies.add("updater");
                if (key.equals("approveUserName")) dependencies.add("approveUserId");
                if (key.equals("confirmUserName")) dependencies.add("confirmUserId");
                if (key.equals("statusName")) dependencies.add("status");
                if (key.equals("totalCount")) dependencies.add("item_count");
                if (key.equals("totalPrice")) dependencies.addAll(Arrays.asList("item_totalPrice", "item_productPrice", "item_count"));
                if (key.equals("totalCostAmount")) dependencies.addAll(Arrays.asList("item_costAmount", "item_costPrice", "item_count"));
                if (key.equals("totalWeight")) dependencies.add("item_totalWeight");
                if (key.equals("productNames") || key.equals("productCodes")) dependencies.add("item_productId");
                if (key.startsWith("fromWarehouse")) dependencies.add("item_fromWarehouseId");
                if (key.startsWith("toWarehouse")) dependencies.add("item_toWarehouseId");
            }
            if (dependencies.stream().anyMatch(result::contains)) result.add(key);
        }
        return result;
    }

    static List<ErpExportFieldRespVO> fields(Class<?> head, Set<String> hidden) {
        Map<String, String> groups = new HashMap<>();
        for (Field field : head.getDeclaredFields()) groups.put(field.getName(), detail(field.getName()) ? "detail" : "main");
        List<ErpExportFieldRespVO> fields = ErpExportFieldUtils.listFields(head, groups, hidden, Collections.emptyMap());
        fields.forEach(field -> field.setGroupLabel("detail".equals(field.getGroup()) ? "明细信息" : "单据信息"));
        return fields;
    }

    static Object read(Object bean, String name) {
        if (bean == null) return null;
        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        return wrapper.isReadableProperty(name) ? wrapper.getPropertyValue(name) : null;
    }
    private static BigDecimal decimal(Object bean, String name) {
        Object value = read(bean, name);
        return value == null ? null : new BigDecimal(value.toString());
    }
    static <E> E row(Class<E> head, Object main, Object item) {
        E row = BeanUtils.instantiateClass(head);
        BeanUtils.copyProperties(main, row);
        BeanWrapper target = new BeanWrapperImpl(row);
        if (target.isWritableProperty("status")) {
            Object statusName = read(main, "statusName");
            Object status = read(main, "status");
            target.setPropertyValue("status", statusName != null ? statusName :
                    status == null ? null : "0".equals(status.toString()) ? "草稿" : "20".equals(status.toString()) ? "已审核" : "待审核");
        }
        if (item == null) return row;
        for (Field field : head.getDeclaredFields()) {
            if (!detail(field.getName())) continue;
            String name = Character.toLowerCase(field.getName().charAt(4)) + field.getName().substring(5);
            if (name.equals("quantity")) name = "count";
            Object value = read(item, name);
            if (name.equals("pieceCount")) {
                BigDecimal count = decimal(item, "count"), pack = decimal(item, "packageQty");
                value = count == null || pack == null || pack.signum() == 0 ? null : count.divide(pack, 6, RoundingMode.HALF_UP);
            }
            if (name.equals("totalPrice") && value == null) {
                BigDecimal count = decimal(item, "count"), price = decimal(item, "productPrice");
                value = count == null || price == null ? null : count.multiply(price);
            }
            if (Boolean.FALSE.equals(read(item, "priceVisible")) && (name.toLowerCase().contains("price") || name.toLowerCase().contains("amount"))) value = null;
            target.setPropertyValue(field.getName(), value);
        }
        return row;
    }

    static <D, H, I> void write(HttpServletResponse response, String filename, String fields,
            Class<?> legacyHead, Class<?> head, Set<String> hidden,
            IntFunction<PageResult<D>> pages, Function<PageResult<D>, List<H>> headers,
            BiFunction<Collection<Long>, Integer, List<I>> items,
            Function<I, Long> owner, Function<List<I>, List<?>> hydrate) throws IOException {
        boolean legacy = fields == null;
        Class<?> output = legacy ? legacyHead : head;
        Set<String> requested = ErpExportFieldUtils.parseFieldParam(fields);
        if (!legacy && requested.isEmpty()) throw exception(new ErrorCode(1_030_000_001, "请至少选择一个导出字段"));
        Set<String> selected = ErpExportFieldUtils.resolveIncludeFields(output, requested, hidden, Collections.emptyMap());
        boolean withItems = !legacy && selected != null && selected.stream().anyMatch(ErpStockMovementExportSupport::detail);
        PageResult<D> page = pages.apply(1);
        checkLimit(page);
        Path temp = Files.createTempFile("erp-movement-export-", ".xlsx");
        try {
            cn.idev.excel.write.builder.ExcelWriterBuilder builder = FastExcelFactory.write(temp.toFile(), output).registerConverter(new LongStringConverter());
            if (selected != null) builder.includeColumnFieldNames(selected);
            try (ExcelWriter writer = builder.build()) {
                WriteSheet sheet = FastExcelFactory.writerSheet("数据").build();
                writer.write(Collections.emptyList(), sheet);
                int pageNo = 1;
                while (!page.getList().isEmpty()) {
                    List<H> mains = headers.apply(page);
                    if (!withItems) {
                        List<Object> rows = new ArrayList<>();
                        mains.forEach(main -> rows.add(legacy ? main : row(head, main, null)));
                        writer.write(rows, sheet);
                    } else {
                        Map<Long, H> byId = new LinkedHashMap<>();
                        mains.forEach(main -> byId.put((Long) read(main, "id"), main));
                        List<Long> ids = new ArrayList<>(byId.keySet());
                        Set<Long> written = new HashSet<>();
                        int cursor = 0;
                        for (int itemPage = 1; ; itemPage++) {
                            List<I> batch = items.apply(ids, itemPage);
                            if (batch.isEmpty()) break;
                            List<?> display = hydrate.apply(batch);
                            List<Object> rows = new ArrayList<>();
                            for (int index = 0; index < batch.size(); index++) {
                                Long id = owner.apply(batch.get(index));
                                if (!byId.containsKey(id)) continue;
                                while (cursor < ids.size() && !ids.get(cursor).equals(id)) {
                                    Long previous = ids.get(cursor++);
                                    if (!written.contains(previous)) rows.add(row(head, byId.get(previous), null));
                                }
                                written.add(id);
                                rows.add(row(head, byId.get(id), display.get(index)));
                            }
                            writer.write(rows, sheet);
                            if (batch.size() < 500) break;
                        }
                        List<Object> empty = new ArrayList<>();
                        for (; cursor < ids.size(); cursor++) if (!written.contains(ids.get(cursor))) empty.add(row(head, byId.get(ids.get(cursor)), null));
                        writer.write(empty, sheet);
                    }
                    if (page.getList().size() < 100 || pageNo * 100L >= page.getTotal()) break;
                    page = pages.apply(++pageNo); checkLimit(page);
                }
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + filename + ".xlsx");
            Files.copy(temp, response.getOutputStream());
        } finally { Files.deleteIfExists(temp); }
    }
    private static void checkLimit(PageResult<?> page) {
        if (page.getTotal() > 5000) throw exception(new ErrorCode(1_030_000_002, "单次最多导出 5000 张单据，请缩小筛选范围后重试"));
    }
}
