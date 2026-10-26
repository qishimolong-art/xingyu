package cn.iocoder.yudao.module.erp.controller.admin.product;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public final class ErpSimpleArchiveExportSupport {

    private ErpSimpleArchiveExportSupport() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static int write(Path target, Class<?> headClass, String sheetName, Set<String> fields,
                            IntFunction<List<?>> loader, IntConsumer progressConsumer) throws IOException {
        int written = 0;
        try (OutputStream output = Files.newOutputStream(target);
             ExcelWriter writer = FastExcelFactory.write(output, headClass)
                     .autoCloseStream(false)
                     .includeColumnFieldNames(fields)
                     .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                     .build()) {
            WriteSheet sheet = FastExcelFactory.writerSheet(sheetName).build();
            int pageNo = 1;
            List batch;
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
