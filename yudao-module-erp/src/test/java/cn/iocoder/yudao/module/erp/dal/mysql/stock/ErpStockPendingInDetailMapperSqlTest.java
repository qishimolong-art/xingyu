package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockPendingInDetailMapperSqlTest {

    private static final String UTF8MB4_UNICODE_SUFFIX =
            " USING utf8mb4) COLLATE utf8mb4_unicode_ci";

    @Test
    void selectList_shouldNormalizeEveryUnionTextColumnToOneCollation() throws Exception {
        Method method = ErpStockPendingInDetailMapper.class.getMethod("selectList", Long.class, Long.class,
                Integer.class, Integer.class, Integer.class, String.class, Boolean.class);
        Select select = method.getAnnotation(Select.class);
        assertNotNull(select);
        String sql = String.join(" ", select.value()).replaceAll("\\s+", " ");

        String[] documentAliases = {"pi", "sr", "si", "sm", "wm", "sc", "sib"};
        for (String alias : documentAliases) {
            assertTrue(sql.contains("CONVERT(" + alias + ".no" + UTF8MB4_UNICODE_SUFFIX),
                    alias + ".no must use the shared UNION collation");
            assertTrue(sql.contains("CONVERT(" + alias + ".creator" + UTF8MB4_UNICODE_SUFFIX),
                    alias + ".creator must use the shared UNION collation");
        }

        String[] batchNoColumns = {
                "pii.batch_no", "smi.batch_no", "wmi.batch_no", "sci.batch_no", "sibi.batch_no"
        };
        for (String batchNoColumn : batchNoColumns) {
            assertTrue(sql.contains("CONVERT(" + batchNoColumn + UTF8MB4_UNICODE_SUFFIX),
                    batchNoColumn + " must use the shared UNION collation");
        }
    }

}
