package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.apache.ibatis.mapping.Environment;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_PRICE_POSITIVE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErpPurchaseOrderSubmitPriceCacheTest {

    @Test
    void submitValidatesUpdatedPriceAfterBatchWriteInSameTransaction() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:order_price_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        MybatisConfiguration config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), ds));
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                new TenantDatabaseInterceptor(new TenantProperties())));
        config.addInterceptor(interceptor);
        config.addMapper(ErpPurchaseOrderItemMapper.class);
        SqlSessionTemplate session = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        ErpPurchaseOrderItemMapper mapper = session.getMapper(ErpPurchaseOrderItemMapper.class);
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        String columns = TableInfoHelper.getTableInfo(ErpPurchaseOrderItemDO.class).getFieldList().stream()
                .map(field -> field.getColumn() + " " + sqlType(field)).collect(Collectors.joining(","));
        jdbc.execute("CREATE TABLE erp_purchase_order_items (id bigint PRIMARY KEY,tenant_id bigint," + columns + ")");
        jdbc.execute("INSERT INTO erp_purchase_order_items"
                + "(id,tenant_id,order_id,product_id,product_unit_id,warehouse_id,count,product_price,gift,deleted) VALUES"
                + "(1,1,10,200,1,7,1,0,false,false),"
                + "(2,2,10,200,1,7,1,0,false,false),"
                + "(3,1,10,200,1,7,1,0,false,true),"
                + "(4,1,11,200,1,7,1,0,false,false)");

        ErpPurchaseOrderServiceImpl service = new ErpPurchaseOrderServiceImpl();
        ReflectionTestUtils.setField(service, "purchaseOrderItemMapper", mapper);
        ReflectionTestUtils.setField(service, "purchaseDocumentDefaultService", mock(ErpPurchaseDocumentDefaultService.class));
        ErpSupplierService suppliers = mock(ErpSupplierService.class);
        when(suppliers.validateSupplier(100L)).thenReturn(new ErpSupplierDO().setId(100L));
        ReflectionTestUtils.setField(service, "supplierService", suppliers);
        ErpSupplierDeptPermissionService depts = mock(ErpSupplierDeptPermissionService.class);
        when(depts.hasAvailableDept(any(), any(), anyString())).thenReturn(true);
        ReflectionTestUtils.setField(service, "supplierDeptPermissionService", depts);
        ErpProductService products = mock(ErpProductService.class);
        when(products.validProductList(any())).thenReturn(Collections.singletonList(new ErpProductDO().setId(200L).setUnitId(1L)));
        ReflectionTestUtils.setField(service, "productService", products);
        ReflectionTestUtils.setField(service, "warehouseService", mock(ErpWarehouseService.class));
        ReflectionTestUtils.setField(service, "productBatchNoValidator", new ErpProductBatchNoValidator());
        ErpPurchaseOrderDO order = new ErpPurchaseOrderDO().setId(10L).setSupplierId(100L)
                .setDeptId(20L).setOrderTime(LocalDateTime.now());

        try {
            TenantContextHolder.setTenantId(1L);
            new TransactionTemplate(new DataSourceTransactionManager(ds)).execute(status -> {
                // 模拟更新流程先查询旧明细，再通过独立批量会话保存价格。
                assertEquals(0, mapper.selectListByOrderId(10L).get(0).getProductPrice().compareTo(BigDecimal.ZERO));
                ErpPurchaseOrderItemDO edited = new ErpPurchaseOrderItemDO().setId(1L).setOrderId(10L)
                        .setProductId(200L).setProductUnitId(1L).setWarehouseId(7L).setGift(false)
                        .setProductPrice(new BigDecimal("1.11111")).setCount(new BigDecimal("1.1111"));
                ReflectionTestUtils.invokeMethod(service, "updatePurchaseOrderItemList", 10L, Collections.singletonList(edited));
                assertEquals(0, jdbc.queryForObject("SELECT product_price FROM erp_purchase_order_items WHERE id=1", BigDecimal.class)
                        .compareTo(new BigDecimal("1.11111")));
                assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validatePurchaseOrderForSubmit", order));
                assertEquals(1, mapper.selectListByOrderId(10L).size());
                assertEquals(0, mapper.selectListByOrderId(10L).get(0).getProductPrice().compareTo(new BigDecimal("1.11111")));
                // 刷新后的校验仍须拦截真实零价／负价，并允许赠品零价。
                for (int price : new int[] {0, -1}) {
                    jdbc.update("UPDATE erp_purchase_order_items SET product_price=? WHERE id=1", price);
                    ServiceException error = assertThrows(
                            ServiceException.class,
                            () -> ReflectionTestUtils.invokeMethod(service, "validatePurchaseOrderForSubmit", order));
                    assertEquals(PURCHASE_ORDER_ITEM_PRICE_POSITIVE.getCode(), error.getCode());
                }
                jdbc.update("UPDATE erp_purchase_order_items SET product_price=0,gift=true WHERE id=1");
                assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validatePurchaseOrderForSubmit", order));
                status.setRollbackOnly();
                return null;
            });
            assertEquals(0, jdbc.queryForObject(
                    "SELECT product_price FROM erp_purchase_order_items WHERE id=1", BigDecimal.class)
                    .compareTo(BigDecimal.ZERO));
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static String sqlType(TableFieldInfo field) {
        Class<?> type = field.getPropertyType();
        if (type == Boolean.class) return "boolean";
        if (type == Long.class) return "bigint";
        if (type == Integer.class) return "int";
        if (type == BigDecimal.class) return "decimal(24,6)";
        if (type == LocalDateTime.class) return "timestamp";
        return "varchar(255)";
    }
}
