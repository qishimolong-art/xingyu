package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.mybatis.core.util.JdbcUtils;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveItemMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** 真实事务内更新并提交，旧草稿空价格/零数量不得覆盖刚输入的有效值。 */
class ErpStockDraftSubmitPriceReadTest {

    private static final BigDecimal PRICE = new BigDecimal("1.111111");
    private static final BigDecimal COUNT = new BigDecimal("1.234567");

    @ParameterizedTest(name = "{0}, incremental={1}, valid={2}")
    @CsvSource({"check,true,true", "check,false,true", "warehouseMove,true,true", "warehouseMove,false,true",
            "check,true,false", "check,false,false", "warehouseMove,true,false", "warehouseMove,false,false",
            "checkCost,true,true", "checkCost,false,true", "checkCost,true,false", "checkCost,false,false"})
    void updatedValuesAreValidatedInSameTransaction(String document, boolean incremental, boolean valid) {
        boolean check = !"warehouseMove".equals(document);
        boolean costCheck = "checkCost".equals(document);
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:stock_submit_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        MybatisConfiguration config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), ds));
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                new TenantDatabaseInterceptor(new TenantProperties())));
        config.addInterceptor(interceptor);
        Class<?>[] mappers = {ErpStockCheckMapper.class, ErpStockCheckItemMapper.class,
                ErpWarehouseMoveMapper.class, ErpWarehouseMoveItemMapper.class};
        for (Class<?> mapper : mappers) config.addMapper(mapper);
        SqlSessionTemplate session = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        Class<?> mainType = check ? ErpStockCheckDO.class : ErpWarehouseMoveDO.class;
        Class<?> itemType = check ? ErpStockCheckItemDO.class : ErpWarehouseMoveItemDO.class;
        createTable(jdbc, mainType);
        createTable(jdbc, itemType);
        String mainTable = TableInfoHelper.getTableInfo(mainType).getTableName();
        String itemTable = TableInfoHelper.getTableInfo(itemType).getTableName();
        String owner = check ? "check_id" : "move_id";
        jdbc.update("INSERT INTO " + mainTable
                + "(id,tenant_id,deleted,no,status,dept_id) VALUES(10,1,false,'AUDIT-ONLY',0,20)");
        jdbc.update("INSERT INTO " + itemTable + "(id,tenant_id,deleted," + owner
                + ",product_id,count,product_price) VALUES(1,1,false,10,200,0,null)");
        if (check) jdbc.update("UPDATE " + itemTable + " SET warehouse_id=7,stock_count=5");
        Map<Class<?>, Object> dependencies = new HashMap<>();
        for (Class<?> mapper : mappers) dependencies.put(mapper, session.getMapper(mapper));
        Object service = check ? new ErpStockCheckServiceImpl() : new ErpWarehouseMoveServiceImpl();
        for (Field field : service.getClass().getDeclaredFields()) {
            if (field.getAnnotation(Resource.class) != null) {
                Object value = dependencies.computeIfAbsent(field.getType(), type ->
                        type == ErpStockItemSnapshotSupport.class ? new ErpStockItemSnapshotSupport() : mock(type));
                ReflectionTestUtils.setField(service, field.getName(), value);
            }
        }
        when(((ErpProductService) dependencies.get(ErpProductService.class)).validProductList(any()))
                .thenReturn(Collections.singletonList(new ErpProductDO().setId(200L).setUnitId(1L)));
        Object request = check ? new ErpStockCheckSaveReqVO() : new ErpWarehouseMoveSaveReqVO();
        Object item = check ? new ErpStockCheckSaveReqVO.Item() : new ErpWarehouseMoveSaveReqVO.Item();
        BeanWrapperImpl header = new BeanWrapperImpl(request);
        header.setPropertyValue("id", 10L);
        header.setPropertyValue("deptId", 20L);
        header.setPropertyValue(check ? "checkTime" : "moveTime", LocalDateTime.now());
        if (check) header.setPropertyValue("checkType", costCheck ? 2 : 1);
        else {
            header.setPropertyValue("fromWarehouseId", 7L);
            header.setPropertyValue("toWarehouseId", 8L);
        }
        BeanWrapperImpl row = new BeanWrapperImpl(item);
        row.setPropertyValue("id", 1L);
        row.setPropertyValue("productId", 200L);
        row.setPropertyValue("count", COUNT);
        row.setPropertyValue("productPrice", PRICE);
        if (check) {
            row.setPropertyValue("warehouseId", 7L);
            row.setPropertyValue("stockCount", new BigDecimal("5"));
            if (costCheck) row.setPropertyValue("totalPrice", new BigDecimal("2.222222"));
        } else row.setPropertyValue("costPrice", PRICE);
        if (incremental) row.setPropertyValue("operation", "update");
        if (!valid) row.setPropertyValue(check ? "productPrice" : "count",
                check ? new BigDecimal("-1") : BigDecimal.ZERO);
        header.setPropertyValue("items", Collections.singletonList(item));
        try (MockedStatic<JdbcUtils> databaseType = mockStatic(JdbcUtils.class, CALLS_REAL_METHODS)) {
            databaseType.when(JdbcUtils::getDbType).thenReturn(DbType.H2);
            TenantContextHolder.setTenantId(1L);
            new TransactionTemplate(new DataSourceTransactionManager(ds)).execute(status -> {
                Runnable submit = () -> {
                    if (check) ((ErpStockCheckServiceImpl) service).updateAndSubmitStockCheckDraft(
                            (ErpStockCheckSaveReqVO) request);
                    else ((ErpWarehouseMoveServiceImpl) service).updateAndSubmitWarehouseMoveDraft(
                            (ErpWarehouseMoveSaveReqVO) request);
                };
                if (valid) {
                    submit.run();
                    assertEquals(10, jdbc.queryForObject("SELECT status FROM " + mainTable + " WHERE id=10", Integer.class));
                    assertEquals(0, jdbc.queryForObject("SELECT product_price FROM " + itemTable
                            + " WHERE " + owner + "=10 AND deleted=false", BigDecimal.class).compareTo(PRICE));
                    assertEquals(0, jdbc.queryForObject("SELECT count FROM " + itemTable
                            + " WHERE " + owner + "=10 AND deleted=false", BigDecimal.class)
                            .compareTo(costCheck ? BigDecimal.ZERO : COUNT));
                    if (costCheck) assertEquals(0, jdbc.queryForObject("SELECT total_price FROM " + itemTable
                            + " WHERE " + owner + "=10 AND deleted=false", BigDecimal.class)
                            .compareTo(new BigDecimal("2.222222")));
                } else {
                    RuntimeException error = assertThrows(RuntimeException.class, submit::run);
                    assertTrue(error.getMessage().contains(check ? "价格不能为负" : "移货数量必须大于 0"));
                    assertEquals(0, jdbc.queryForObject("SELECT status FROM " + mainTable + " WHERE id=10", Integer.class));
                    assertNull(jdbc.queryForObject("SELECT product_price FROM " + itemTable + " WHERE id=1", BigDecimal.class));
                }
                status.setRollbackOnly();
                return null;
            });
            assertEquals(0, jdbc.queryForObject("SELECT status FROM " + mainTable + " WHERE id=10", Integer.class));
            assertNull(jdbc.queryForObject("SELECT product_price FROM " + itemTable + " WHERE id=1", BigDecimal.class));
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
