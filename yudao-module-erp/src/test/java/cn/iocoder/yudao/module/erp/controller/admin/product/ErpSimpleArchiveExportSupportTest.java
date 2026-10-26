package cn.iocoder.yudao.module.erp.controller.admin.product;

import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandRespVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpSimpleArchiveExportSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteMoreThanOneBoundedPage() throws Exception {
        List<ErpProductBrandRespVO> firstPage = new ArrayList<>();
        for (long id = 1; id <= 500; id++) {
            ErpProductBrandRespVO row = new ErpProductBrandRespVO();
            row.setId(id);
            row.setName("品牌" + id);
            firstPage.add(row);
        }
        ErpProductBrandRespVO last = new ErpProductBrandRespVO();
        last.setId(501L);
        last.setName("品牌501");
        AtomicInteger loads = new AtomicInteger();
        AtomicInteger progress = new AtomicInteger();
        Path target = tempDir.resolve("brands.xlsx.part");

        int written = ErpSimpleArchiveExportSupport.write(
                target,
                ErpProductBrandRespVO.class,
                "配件品牌",
                new java.util.LinkedHashSet<>(Arrays.asList("id", "name")),
                pageNo -> {
                    loads.incrementAndGet();
                    if (pageNo == 1) {
                        return new ArrayList<>(firstPage);
                    }
                    if (pageNo == 2) {
                        return Collections.singletonList(last);
                    }
                    return Collections.emptyList();
                },
                progress::set);

        assertEquals(501, written);
        assertEquals(2, loads.get());
        assertEquals(501, progress.get());
        assertTrue(Files.size(target) > 0);
    }
}
