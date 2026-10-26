package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierRespVO;

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

/** Supplier-only export: bounded database batches and a temporary workbook, no full in-memory list. */
public final class ErpSupplierExportSupport {
    private ErpSupplierExportSupport() { }

    public static ErpSupplierExportRespVO toExportRow(ErpSupplierRespVO row) {
        ErpSupplierExportRespVO result = BeanUtils.toBean(row, ErpSupplierExportRespVO.class);
        if (row.getPurchaserName() != null) result.setPurchaser(row.getPurchaserName());
        return result;
    }

    public static void write(HttpServletResponse response, Set<String> fields,
                      IntFunction<List<ErpSupplierExportRespVO>> loader) throws IOException {
        Path temporary = Files.createTempFile("erp-supplier-export-", ".xlsx");
        try {
            int rowCount = 0;
            try (OutputStream output = Files.newOutputStream(temporary);
                 ExcelWriter writer = FastExcelFactory.write(output, ErpSupplierExportRespVO.class)
                         .autoCloseStream(false).includeColumnFieldNames(fields)
                         .registerWriteHandler(new ColumnWidthMatchStyleStrategy()).build()) {
                WriteSheet sheet = FastExcelFactory.writerSheet("供应商").build();
                int pageNo = 1;
                List<ErpSupplierExportRespVO> batch;
                do {
                    batch = loader.apply(pageNo++);
                    writer.write(batch, sheet);
                    rowCount += batch.size();
                } while (batch.size() == 500);
            }
            // Prepare the complete workbook before changing the HTTP response.
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8("供应商.xlsx"));
            Files.copy(temporary, response.getOutputStream());
            ExcelUtils.recordStreamedWrite("供应商.xlsx", rowCount, fields);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static int write(Path target, Set<String> fields,
                            IntFunction<List<ErpSupplierExportRespVO>> loader,
                            IntConsumer progressConsumer) throws IOException {
        int written = 0;
        try (OutputStream output = Files.newOutputStream(target);
             ExcelWriter writer = FastExcelFactory.write(output, ErpSupplierExportRespVO.class)
                     .autoCloseStream(false).includeColumnFieldNames(fields)
                     .registerWriteHandler(new ColumnWidthMatchStyleStrategy()).build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet("供应商").build();
            int pageNo = 1;
            List<ErpSupplierExportRespVO> batch;
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
