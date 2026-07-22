package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockBatchQuantityMapperSqlTest {

    private static final String UTF8MB4_UNICODE_SUFFIX =
            " USING utf8mb4) COLLATE utf8mb4_unicode_ci";

    @Test
    void selectOccupiedList_shouldNormalizeEveryUnionBatchNoToOneCollation() throws Exception {
        String sql = getSql("selectOccupiedList", Collection.class, Collection.class,
                Integer.class, Collection.class, Integer.class, Integer.class);

        String[] batchNoColumns = {
                "sci.batch_no", "soi.batch_no", "pri.batch_no", "smi.batch_no",
                "wmi.batch_no", "sci2.batch_no", "sobi.batch_no"
        };
        assertNormalized(sql, batchNoColumns);
    }

    @Test
    void selectPendingInList_shouldNormalizeEveryUnionBatchNoToOneCollation() throws Exception {
        String sql = getSql("selectPendingInList", Collection.class, Collection.class,
                Integer.class, Integer.class, Integer.class);

        String[] batchNoColumns = {
                "pii.batch_no", "smi.batch_no", "wmi.batch_no", "sci.batch_no", "sibi.batch_no"
        };
        assertNormalized(sql, batchNoColumns);
    }

    private static String getSql(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = ErpStockBatchQuantityMapper.class.getMethod(methodName, parameterTypes);
        Select select = method.getAnnotation(Select.class);
        assertNotNull(select);
        return String.join(" ", select.value()).replaceAll("\\s+", " ");
    }

    private static void assertNormalized(String sql, String[] batchNoColumns) {
        for (String batchNoColumn : batchNoColumns) {
            assertTrue(sql.contains("CONVERT(" + batchNoColumn + UTF8MB4_UNICODE_SUFFIX),
                    batchNoColumn + " must use the shared UNION collation");
        }
    }

}
