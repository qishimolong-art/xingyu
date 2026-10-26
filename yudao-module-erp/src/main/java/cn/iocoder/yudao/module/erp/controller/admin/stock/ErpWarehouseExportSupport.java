package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseRespVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntConsumer;

/** Warehouse-only export: bounded database batches and a temporary workbook, no full in-memory list. */
public final class ErpWarehouseExportSupport {
    public static final Set<String> LEGACY_FIELDS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "id", "name", "deptId", "deptName", "address", "mapName", "longitude", "latitude", "sort", "remark",
            "principal", "warehousePrice", "truckagePrice", "status", "defaultStatus", "createTime")));
    public static final Map<String, String> GROUPS;
    public static final Map<String, String> PERMISSIONS;
    static {
        Map<String, String> groups = new HashMap<>();
        LEGACY_FIELDS.forEach(field -> groups.put(field, "main"));
        Arrays.asList("address", "mapName", "longitude", "latitude").forEach(field -> groups.put(field, "address_info"));
        Arrays.asList("creatorName", "updaterName", "createTime", "updateTime").forEach(field -> groups.put(field, "system"));
        GROUPS = Collections.unmodifiableMap(groups);
        Map<String, String> permissions = new HashMap<>();
        permissions.put("creatorName", "creator");
        permissions.put("updaterName", "updater");
        PERMISSIONS = Collections.unmodifiableMap(permissions);
    }

    private ErpWarehouseExportSupport() { }

    public static ErpWarehouseExportRespVO toExportRow(ErpWarehouseRespVO row) {
        ErpWarehouseExportRespVO result = BeanUtils.toBean(row, ErpWarehouseExportRespVO.class);
        result.setStatus(row.getStatus() == null ? null : row.getStatus() == 0 ? "启用" : "停用");
        result.setWarehouseType(row.getWarehouseType() == null ? null : row.getWarehouseType() == 1 ? "物理仓" : "虚拟仓");
        return result;
    }

    public static void write(HttpServletResponse response, Set<String> fields,
                      IntFunction<List<ErpWarehouseExportRespVO>> loader) throws IOException {
        Path temporary = Files.createTempFile("erp-warehouse-export-", ".xlsx");
        try {
            try (OutputStream output = Files.newOutputStream(temporary);
                 ExcelWriter writer = FastExcelFactory.write(output, ErpWarehouseExportRespVO.class)
                         .autoCloseStream(false).includeColumnFieldNames(fields)
                         .registerWriteHandler(new ColumnWidthMatchStyleStrategy()).build()) {
                WriteSheet sheet = FastExcelFactory.writerSheet("仓库").build();
                int pageNo = 1;
                List<ErpWarehouseExportRespVO> batch;
                do {
                    batch = loader.apply(pageNo++);
                    writer.write(batch, sheet);
                } while (batch.size() == 500);
            }
            // Prepare the complete workbook before changing the HTTP response.
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8("仓库.xlsx"));
            Files.copy(temporary, response.getOutputStream());
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static int write(Path target, Set<String> fields,
                            IntFunction<List<ErpWarehouseExportRespVO>> loader,
                            IntConsumer progressConsumer) throws IOException {
        int written = 0;
        try (OutputStream output = Files.newOutputStream(target);
             ExcelWriter writer = FastExcelFactory.write(output, ErpWarehouseExportRespVO.class)
                     .autoCloseStream(false).includeColumnFieldNames(fields)
                     .registerWriteHandler(new ColumnWidthMatchStyleStrategy()).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("仓库").build();
            int pageNo = 1;
            List<ErpWarehouseExportRespVO> batch;
            do {
                batch = loader.apply(pageNo++);
                writer.write(batch, sheet);
                written += batch.size();
                progressConsumer.accept(written);
            } while (batch.size() == 500);
        }
        return written;
    }
}
