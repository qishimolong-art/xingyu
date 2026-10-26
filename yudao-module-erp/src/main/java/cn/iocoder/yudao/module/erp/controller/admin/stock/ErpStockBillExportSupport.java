package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import org.springframework.beans.BeanWrapperImpl;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/** Bounded historical-bill export; no stock, status or print mutations. */
final class ErpStockBillExportSupport {
    private ErpStockBillExportSupport() {}
    static Set<String> hidden(Set<String> source, Map<String,String> aliases) {
        Set<String> result = new HashSet<>(source == null ? Collections.emptySet() : source);
        for (String value : new HashSet<>(result)) if (value.startsWith("col_")) result.add(value.substring(4));
        aliases.forEach((field, permission) -> { if (result.contains(permission)) result.add(field); });
        if (result.contains("creator")) result.add("creatorName");
        if (result.contains("updater")) result.add("updaterName");
        if (result.contains("auditor")) result.add("auditorName");
        if (result.contains("item_count") || result.contains("itemCount")) {
            result.addAll(Arrays.asList("itemTotalPrice", "itemRemainCount", "remainingCount", "totalCount"));
        }
        if (result.contains("item_pickedCount") || result.contains("itemPickedCount")) result.addAll(Arrays.asList("itemRemainCount", "remainingCount", "pickedCount"));
        if (result.contains("item_totalWeight") || result.contains("itemTotalWeight")) result.add("totalWeight");
        if (result.contains("totalCount") || result.contains("pickedCount")) result.add("remainingCount");
        if (result.contains("item_productPrice") || result.contains("productPrice")) result.add("itemTotalPrice");
        return result;
    }
    static void mask(Object bean, Set<String> hidden) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
        for (String key : hidden) {
            String name = key;
            if (wrapper.isWritableProperty(name)) wrapper.setPropertyValue(name,null);
        }
    }
    static void maskItem(Object bean, Set<String> hidden) {
        mask(bean,hidden);
        BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
        for (String key : hidden) {
            if (!key.startsWith("item") || key.length() <= 4) continue;
            String name = key.startsWith("item_") ? key.substring(5) : Character.toLowerCase(key.charAt(4)) + key.substring(5);
            if (wrapper.isWritableProperty(name)) wrapper.setPropertyValue(name,null);
        }
    }
    static <D,I,E> void write(HttpServletResponse response, String filename, String fields, Class<E> head,
            Map<String,String> groups, Set<String> hidden, Set<String> legacyFields,
            IntFunction<PageResult<D>> pages, Function<D,Long> id,
            BiFunction<Collection<Long>,Integer,List<I>> items, Function<I,Long> owner,
            BiFunction<List<D>,List<I>,BiFunction<D,I,E>> prepare) throws IOException {
        boolean legacy = fields == null;
        Set<String> requested = legacy ? legacyFields : ErpExportFieldUtils.parseFieldParam(fields);
        if (requested.isEmpty()) throw exception(new ErrorCode(1_030_000_001,"请至少选择一个导出字段"));
        Set<String> selected = ErpExportFieldUtils.resolveIncludeFields(head, requested, hidden, Collections.emptyMap());
        boolean withItems = legacy || selected.stream().anyMatch(key -> "detail".equals(groups.get(key)));
        PageResult<D> page = pages.apply(1);
        checkLimit(page);
        Path temp = Files.createTempFile("erp-bill-export-", ".xlsx");
        try {
            try (ExcelWriter writer = FastExcelFactory.write(temp.toFile(),head).includeColumnFieldNames(selected).build()) {
                WriteSheet sheet = FastExcelFactory.writerSheet("数据").build();
                writer.write(Collections.emptyList(),sheet);
                int pageNo = 1;
                while (!page.getList().isEmpty()) {
                    List<D> mains = page.getList();
                    if (!withItems) {
                        BiFunction<D,I,E> row = prepare.apply(mains,Collections.emptyList());
                        List<E> rows = new ArrayList<>();
                        mains.forEach(main -> rows.add(row.apply(main,null)));
                        writer.write(rows,sheet);
                    } else {
                        Map<Long,D> byId = new LinkedHashMap<>();
                        mains.forEach(main -> byId.put(id.apply(main),main));
                        List<Long> ids = new ArrayList<>(byId.keySet());
                        Set<Long> written = new HashSet<>();
                        int cursor = 0;
                        for (int itemPage=1;;itemPage++) {
                            List<I> batch = items.apply(ids,itemPage);
                            if (batch.isEmpty()) break;
                            BiFunction<D,I,E> row = prepare.apply(mains,batch);
                            List<E> rows = new ArrayList<>();
                            for (I item : batch) {
                                Long billId = owner.apply(item);
                                if (!byId.containsKey(billId)) continue;
                                while (cursor < ids.size() && !ids.get(cursor).equals(billId)) {
                                    Long previous = ids.get(cursor++);
                                    if (!written.contains(previous)) rows.add(row.apply(byId.get(previous),null));
                                }
                                E value = row.apply(byId.get(billId),item);
                                if (legacy && written.contains(billId)) {
                                    BeanWrapperImpl bean = new BeanWrapperImpl(value);
                                    groups.forEach((key, group) -> { if (!"detail".equals(group)) bean.setPropertyValue(key,null); });
                                }
                                rows.add(value); written.add(billId);
                            }
                            writer.write(rows,sheet);
                            if (batch.size()<500) break;
                        }
                        BiFunction<D,I,E> emptyRow = prepare.apply(mains,Collections.emptyList());
                        List<E> empty = new ArrayList<>();
                        for (;cursor<ids.size();cursor++) if (!written.contains(ids.get(cursor))) empty.add(emptyRow.apply(byId.get(ids.get(cursor)),null));
                        writer.write(empty,sheet);
                    }
                    if (page.getList().size()<100 || pageNo*100L >= page.getTotal()) break;
                    page=pages.apply(++pageNo); checkLimit(page);
                }
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename="+filename+".xlsx");
            Files.copy(temp,response.getOutputStream());
        } finally { Files.deleteIfExists(temp); }
    }
    private static void checkLimit(PageResult<?> page) {
        if (page.getTotal()>5000) throw exception(new ErrorCode(1_030_000_002,"单次最多导出 5000 张单据，请缩小筛选范围后重试"));
    }
}
