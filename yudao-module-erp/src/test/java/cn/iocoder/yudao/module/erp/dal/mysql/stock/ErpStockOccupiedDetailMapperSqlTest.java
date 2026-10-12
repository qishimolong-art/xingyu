package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockOccupiedDetailMapperSqlTest {

    private static final String UTF8MB4_UNICODE_SUFFIX =
            " USING utf8mb4) COLLATE utf8mb4_unicode_ci";

    @Test
    void selectList_shouldNormalizeEveryUnionTextColumnToOneCollation() throws Exception {
        Method method = ErpStockOccupiedDetailMapper.class.getMethod("selectList", Long.class, Long.class,
                Integer.class, List.class, Integer.class, Integer.class, String.class, Boolean.class);
        Select select = method.getAnnotation(Select.class);
        assertNotNull(select);
        String sql = String.join(" ", select.value()).replaceAll("\\s+", " ");
        assertTrue(sql.contains("sc.status &lt;&gt; 20 OR sc.billing_confirm_time IS NULL"));

        String[] aliases = {"sc", "so", "pr", "so2", "sm", "wm", "sc2", "sob"};
        for (String alias : aliases) {
            assertTrue(sql.contains("CONVERT(" + alias + ".no" + UTF8MB4_UNICODE_SUFFIX),
                    alias + ".no must use the shared UNION collation");
            assertTrue(sql.contains("CONVERT(" + alias + ".creator" + UTF8MB4_UNICODE_SUFFIX),
                    alias + ".creator must use the shared UNION collation");
        }

        String[] batchNoColumns = {
                "sci.batch_no", "soi.batch_no", "pri.batch_no", "smi.batch_no",
                "wmi.batch_no", "sci2.batch_no", "sobi.batch_no"
        };
        for (String batchNoColumn : batchNoColumns) {
            assertTrue(sql.contains("CONVERT(" + batchNoColumn + UTF8MB4_UNICODE_SUFFIX),
                    batchNoColumn + " must use the shared UNION collation");
        }
    }

    @Test
    void saleCartOccupiedQueries_shouldExcludeConfirmedPendingFirstApproveCarts() {
        Class<?>[] mapperTypes = {
                ErpStockOccupiedDetailMapper.class,
                ErpStockBatchQuantityMapper.class,
                ErpSaleCartItemMapper.class
        };
        for (Class<?> mapperType : mapperTypes) {
            boolean foundSaleCartQuery = false;
            for (Method method : mapperType.getDeclaredMethods()) {
                Select select = method.getAnnotation(Select.class);
                if (select == null) {
                    continue;
                }
                String sql = String.join(" ", select.value()).replaceAll("\\s+", " ");
                if (!sql.contains("erp_sale_cart_items")) {
                    continue;
                }
                foundSaleCartQuery = true;
                assertTrue(sql.contains("sc.status &lt;&gt; 20 OR sc.billing_confirm_time IS NULL"),
                        mapperType.getSimpleName() + "." + method.getName()
                                + " must exclude confirmed status-20 carts from stock occupied queries");
            }
            assertTrue(foundSaleCartQuery, mapperType.getSimpleName() + " must expose a sale-cart occupied query");
        }
    }

}
