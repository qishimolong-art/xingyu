package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.mybatis.core.util.JdbcUtils;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.apache.ibatis.mapping.Environment;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.mockito.MockedStatic;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** 使用真实 Mapper/批量写入，检查销售草稿更新后提交读取新价格及六位小数。 */
class ErpSaleDraftSubmitPriceReadTest {

    private static final String ERP = "cn.iocoder.yudao.module.erp.";
    private static final BigDecimal PRICE = new BigDecimal("1.111111");
    private static final BigDecimal COUNT = new BigDecimal("1.234567");

    @ParameterizedTest(name = "{0}, incremental={1}")
    @CsvSource({"Quote,true", "Quote,false", "Cart,true", "Cart,false", "CartFormal,true", "CartFormal,false",
            "Return,true", "Return,false", "PriceAdjust,true", "PriceAdjust,false"})
    void updatedPriceSurvivesDraftSaveAndSubmit(String scenario, boolean incremental) throws Exception {
        boolean formalCart = "CartFormal".equals(scenario);
        String document = formalCart ? "Cart" : scenario;
        String stem = "ErpSale" + document;
        String path = "Return".equals(document) ? "returns" : document.toLowerCase();
        String requestName = "Cart".equals(document) ? "SaveReqVO"
                : "PriceAdjust".equals(document) ? "DraftSaveReqVO" : "DraftUpdateReqVO";
        Class<?> mainType = Class.forName(ERP + "dal.dataobject.sale." + stem + "DO");
        Class<?> itemType = Class.forName(ERP + "dal.dataobject.sale." + stem + "ItemDO");
        Class<?> mainMapper = Class.forName(ERP + "dal.mysql.sale." + stem + "Mapper");
        Class<?> itemMapper = Class.forName(ERP + "dal.mysql.sale." + stem + "ItemMapper");
        Object service = Class.forName(ERP + "service.sale." + stem + "ServiceImpl").getConstructor().newInstance();
        Object request = Class.forName(ERP + "controller.admin.sale.vo." + path + "." + stem + requestName)
                .getConstructor().newInstance();
        Object item = Class.forName(ERP + "controller.admin.sale.vo." + path + "." + stem + "SaveReqVO$Item")
                .getConstructor().newInstance();
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:sale_submit_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        MybatisConfiguration config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), ds));
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                new TenantDatabaseInterceptor(new TenantProperties())));
        config.addInterceptor(interceptor);
        config.addMapper(mainMapper);
        config.addMapper(itemMapper);
        SqlSessionTemplate session = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        createTable(jdbc, mainType);
        createTable(jdbc, itemType);
        String mainTable = TableInfoHelper.getTableInfo(mainType).getTableName();
        String itemTable = TableInfoHelper.getTableInfo(itemType).getTableName();
        boolean adjust = "PriceAdjust".equals(document);
        boolean cart = "Cart".equals(document);
        int draftStatus = cart ? 10 : 0;
        int targetStatus = cart ? 15 : 10;
        String owner = adjust ? "adjust_id" : document.toLowerCase() + "_id";
        String priceColumn = adjust ? "new_price" : "product_price";
        String countColumn = adjust ? "out_count" : "count";
        jdbc.update("INSERT INTO " + mainTable + "(id,tenant_id,deleted,no,status,dept_id,customer_id)"
                + " VALUES(10,1,false,'AUDIT-ONLY'," + draftStatus + ",20,100)");
        jdbc.update("INSERT INTO " + itemTable + "(id,tenant_id,deleted," + owner + ",product_id,"
                + countColumn + "," + priceColumn + ") VALUES(1,1,false,10,200,1,0)");
        Map<Class<?>, Object> dependencies = new HashMap<>();
        dependencies.put(mainMapper, session.getMapper(mainMapper));
        dependencies.put(itemMapper, session.getMapper(itemMapper));
        for (Field field : service.getClass().getDeclaredFields()) {
            if (field.getAnnotation(Resource.class) != null) {
                Object value = dependencies.computeIfAbsent(field.getType(), type ->
                        type == ErpProductBatchNoValidator.class ? new ErpProductBatchNoValidator() : mock(type));
                ReflectionTestUtils.setField(service, field.getName(), value);
            }
        }
        ErpProductService products = (ErpProductService) dependencies.get(ErpProductService.class);
        if (products != null) when(products.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        ErpWarehouseService warehouses = (ErpWarehouseService) dependencies.get(ErpWarehouseService.class);
        if (warehouses != null) when(warehouses.validSaleSelectableWarehouseListForDept(any(), any(), anyString()))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(7L).setDeptId(20L)));
        ErpStockService stocks = (ErpStockService) dependencies.get(ErpStockService.class);
        if (stocks != null) when(stocks.getStockMap(any(), any())).thenReturn(Collections.singletonMap("200_7",
                new ErpStockDO().setProductId(200L).setWarehouseId(7L).setDeptId(20L).setCount(new BigDecimal("100"))));
        BeanWrapperImpl header = new BeanWrapperImpl(request);
        header.setPropertyValue("id", 10L);
        header.setPropertyValue("customerId", 100L);
        header.setPropertyValue("deptId", 20L);
        if ("Return".equals(document)) header.setPropertyValue("returnMode", 20);
        if (adjust) {
            header.setPropertyValue("adjustUserId", 30L);
            header.setPropertyValue("settleMethod", "AUDIT-ONLY");
            header.setPropertyValue("deliveryMethod", "AUDIT-ONLY");
        }
        BeanWrapperImpl row = new BeanWrapperImpl(item);
        row.setPropertyValue("id", 1L);
        row.setPropertyValue("productId", 200L);
        row.setPropertyValue("warehouseId", 7L);
        row.setPropertyValue(adjust ? "outCount" : "count", COUNT);
        row.setPropertyValue(adjust ? "newPrice" : "productPrice", PRICE);
        if (adjust) {
            row.setPropertyValue("saleOutId", 300L);
            row.setPropertyValue("saleOutItemId", 400L);
            row.setPropertyValue("oldPrice", BigDecimal.ONE);
        }
        if (incremental) row.setPropertyValue("operation", "update");
        header.setPropertyValue("items", Collections.singletonList(item));
        try (MockedStatic<JdbcUtils> databaseType = mockStatic(JdbcUtils.class, CALLS_REAL_METHODS)) {
            databaseType.when(JdbcUtils::getDbType).thenReturn(DbType.H2);
            TenantContextHolder.setTenantId(1L);
            new TransactionTemplate(new DataSourceTransactionManager(ds)).execute(status -> {
                // 比实际两个 HTTP 请求更严格：同一会话中先读取旧价格，再保存并提交。
                ReflectionTestUtils.invokeMethod(dependencies.get(itemMapper), "selectListBy"
                        + (adjust ? "Adjust" : document) + "Id", 10L);
                ReflectionTestUtils.invokeMethod(service, "updateSale" + document + (formalCart ? "" : "Draft"), request);
                assertEquals(0, jdbc.queryForObject("SELECT " + priceColumn + " FROM " + itemTable
                        + " WHERE " + owner + "=10 AND deleted=false", BigDecimal.class).compareTo(PRICE));
                ReflectionTestUtils.invokeMethod(service, "submitSale" + document, 10L);
                assertEquals(targetStatus, jdbc.queryForObject("SELECT status FROM " + mainTable + " WHERE id=10", Integer.class));
                assertEquals(0, jdbc.queryForObject("SELECT " + priceColumn + " FROM " + itemTable
                        + " WHERE " + owner + "=10 AND deleted=false", BigDecimal.class).compareTo(PRICE));
                assertEquals(0, jdbc.queryForObject("SELECT " + countColumn + " FROM " + itemTable
                        + " WHERE " + owner + "=10 AND deleted=false", BigDecimal.class).compareTo(COUNT));
                status.setRollbackOnly();
                return null;
            });
            assertEquals(draftStatus, jdbc.queryForObject("SELECT status FROM " + mainTable + " WHERE id=10", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT " + priceColumn + " FROM " + itemTable
                    + " WHERE id=1", BigDecimal.class).compareTo(BigDecimal.ZERO));
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static void createTable(JdbcTemplate jdbc, Class<?> type) {
        TableInfo table = TableInfoHelper.getTableInfo(type);
        String columns = table.getFieldList().stream()
                .map(field -> field.getColumn() + " " + sqlType(field)).collect(Collectors.joining(","));
        jdbc.execute("CREATE TABLE " + table.getTableName()
                + " (id bigint AUTO_INCREMENT PRIMARY KEY,tenant_id bigint DEFAULT 1," + columns + ")");
    }

    private static String sqlType(TableFieldInfo field) {
        Class<?> type = field.getPropertyType();
        if (type == Boolean.class) return "boolean DEFAULT false";
        if (type == Long.class) return "bigint";
        if (type == Integer.class) return "int";
        if (type == BigDecimal.class) return "decimal(24,6)";
        if (type == LocalDateTime.class) return "timestamp";
        if (type == LocalDate.class) return "date";
        return "varchar(255)";
    }
}
