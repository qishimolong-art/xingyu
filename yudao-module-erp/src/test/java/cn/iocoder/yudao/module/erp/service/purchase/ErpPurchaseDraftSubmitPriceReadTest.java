package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.mybatis.core.util.JdbcUtils;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.annotation.DbType;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.when;

/** 使用真实主表、明细 Mapper 和事务，检查更新并提交时读取刚保存的价格。 */
class ErpPurchaseDraftSubmitPriceReadTest {

    private static final BigDecimal PRICE = new BigDecimal("1.111111");
    private static final BigDecimal COUNT = new BigDecimal("1.234567");

    @ParameterizedTest(name = "{0}, incremental={1}")
    @CsvSource({"in,true", "return,true", "return,false", "invoice,true", "invoice,false",
            "adjust,true", "adjust,false"})
    void updatedPriceIsUsedBySubmitInSameTransaction(String document, boolean incremental) throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:purchase_submit_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        MybatisConfiguration config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), ds));
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                new TenantDatabaseInterceptor(new TenantProperties())));
        config.addInterceptor(interceptor);
        Class<?>[] mappers = {ErpPurchaseInMapper.class, ErpPurchaseInItemMapper.class,
                ErpPurchaseReturnMapper.class, ErpPurchaseReturnItemMapper.class,
                ErpPurchaseInvoiceMapper.class, ErpPurchaseInvoiceItemMapper.class,
                ErpPurchasePriceAdjustMapper.class, ErpPurchasePriceAdjustItemMapper.class};
        for (Class<?> mapper : mappers) {
            config.addMapper(mapper);
        }
        SqlSessionTemplate session = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        Class<?> mainType = mainType(document);
        Class<?> itemType = itemType(document);
        createTable(jdbc, mainType);
        createTable(jdbc, itemType);
        String mainTable = TableInfoHelper.getTableInfo(mainType).getTableName();
        String itemTable = TableInfoHelper.getTableInfo(itemType).getTableName();
        String owner = "adjust".equals(document) ? "adjust_id"
                : "invoice".equals(document) ? "invoice_id" : document + "_id";
        String priceColumn = "adjust".equals(document) ? "new_price" : "product_price";
        jdbc.update("INSERT INTO " + mainTable + "(id,tenant_id,deleted,no,status,supplier_id,dept_id)"
                + " VALUES(10,1,false,'AUDIT-ONLY',0,100,20)");
        jdbc.update("INSERT INTO " + itemTable + "(id,tenant_id,deleted," + owner
                + ",product_id,count," + priceColumn + ") VALUES(1,1,false,10,200,1,0)");
        if (!"invoice".equals(document)) {
            jdbc.update("UPDATE " + itemTable + " SET warehouse_id=7");
        }
        if ("in".equals(document) || "return".equals(document)) {
            jdbc.update("UPDATE " + itemTable + " SET product_unit_id=1");
        }
        Map<Class<?>, Object> dependencies = new HashMap<>();
        for (Class<?> mapper : mappers) {
            dependencies.put(mapper, session.getMapper(mapper));
        }
        Object service = service(document);
        injectDependencies(service, dependencies);
        configureReferences(dependencies);
        Object request = request(document, incremental);
        try (MockedStatic<JdbcUtils> databaseType = mockStatic(JdbcUtils.class, CALLS_REAL_METHODS)) {
            // 独立 H2 测试没有 Spring 容器，只替代数据库类型发现，保留实际批量写入。
            databaseType.when(JdbcUtils::getDbType).thenReturn(DbType.H2);
            TenantContextHolder.setTenantId(1L);
            new TransactionTemplate(new DataSourceTransactionManager(ds)).execute(status -> {
                // 先读旧值并缓存，确保测试不会因没有旧缓存而假通过。
                readItems(document, dependencies);
                if (service instanceof ErpPurchaseInServiceImpl) {
                    ((ErpPurchaseInServiceImpl) service).updateAndSubmitPurchaseInDraft(
                            (ErpPurchaseInDraftUpdateReqVO) request);
                } else if (service instanceof ErpPurchaseReturnServiceImpl) {
                    ((ErpPurchaseReturnServiceImpl) service).updateAndSubmitPurchaseReturnDraft(
                            (ErpPurchaseReturnDraftUpdateReqVO) request);
                } else if (service instanceof ErpPurchaseInvoiceServiceImpl) {
                    ((ErpPurchaseInvoiceServiceImpl) service).updateAndSubmitPurchaseInvoiceDraft(
                            (ErpPurchaseInvoiceDraftUpdateReqVO) request);
                } else {
                    ((ErpPurchasePriceAdjustServiceImpl) service).updateAndSubmitPurchasePriceAdjust(
                            (ErpPurchasePriceAdjustSaveReqVO) request);
                }
                assertEquals(10, jdbc.queryForObject("SELECT status FROM " + mainTable + " WHERE id=10", Integer.class));
                assertEquals(0, jdbc.queryForObject("SELECT " + priceColumn + " FROM " + itemTable
                        + " WHERE " + owner + "=10 AND deleted=false", BigDecimal.class).compareTo(PRICE));
                assertEquals(0, jdbc.queryForObject("SELECT count FROM " + itemTable
                        + " WHERE " + owner + "=10 AND deleted=false", BigDecimal.class).compareTo(COUNT));
                status.setRollbackOnly();
                return null;
            });
            assertEquals(0, jdbc.queryForObject("SELECT status FROM " + mainTable + " WHERE id=10", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT " + priceColumn + " FROM " + itemTable
                    + " WHERE id=1", BigDecimal.class).compareTo(BigDecimal.ZERO));
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static void configureReferences(Map<Class<?>, Object> dependencies) {
        ErpSupplierService supplier = (ErpSupplierService) dependencies.get(ErpSupplierService.class);
        when(supplier.validateSupplier(100L)).thenReturn(new ErpSupplierDO().setId(100L));
        ErpSupplierDeptPermissionService dept =
                (ErpSupplierDeptPermissionService) dependencies.get(ErpSupplierDeptPermissionService.class);
        when(dept.hasAvailableDept(any(), any(), anyString())).thenReturn(true);
        ErpProductService products = (ErpProductService) dependencies.get(ErpProductService.class);
        when(products.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(products.getProductVOMap(any())).thenReturn(Collections.singletonMap(200L,
                new ErpProductRespVO().setId(200L).setLastPurchasePrice(BigDecimal.ZERO)));
        ErpWarehouseService warehouses = (ErpWarehouseService) dependencies.get(ErpWarehouseService.class);
        if (warehouses != null) {
            when(warehouses.getWarehouseMap(any())).thenReturn(Collections.singletonMap(7L,
                    new ErpWarehouseDO().setId(7L).setDeptId(20L)));
        }
    }

    private static void injectDependencies(Object service, Map<Class<?>, Object> dependencies) {
        for (Field field : service.getClass().getDeclaredFields()) {
            if (field.getAnnotation(Resource.class) != null) {
                Object value = dependencies.computeIfAbsent(field.getType(), type ->
                        type == ErpProductBatchNoValidator.class ? new ErpProductBatchNoValidator() : mock(type));
                ReflectionTestUtils.setField(service, field.getName(), value);
            }
        }
    }

    private static Object request(String document, boolean incremental) {
        Object request;
        Object item;
        switch (document) {
            case "in": request = new ErpPurchaseInDraftUpdateReqVO(); item = new ErpPurchaseInSaveReqVO.Item(); break;
            case "return": request = new ErpPurchaseReturnDraftUpdateReqVO(); item = new ErpPurchaseReturnSaveReqVO.Item(); break;
            case "invoice": request = new ErpPurchaseInvoiceDraftUpdateReqVO(); item = new ErpPurchaseInvoiceSaveReqVO.Item(); break;
            default: request = new ErpPurchasePriceAdjustSaveReqVO(); item = new ErpPurchasePriceAdjustSaveReqVO.Item();
        }
        BeanWrapperImpl main = new BeanWrapperImpl(request);
        main.setPropertyValue("id", 10L);
        main.setPropertyValue("supplierId", 100L);
        main.setPropertyValue("deptId", 20L);
        if ("in".equals(document)) main.setPropertyValue("inTime", LocalDateTime.now());
        if ("return".equals(document)) {
            main.setPropertyValue("returnTime", LocalDateTime.now());
            main.setPropertyValue("returnMode", 20);
        }
        if ("invoice".equals(document)) {
            main.setPropertyValue("invoiceDate", LocalDate.now());
            main.setPropertyValue("invoiceType", "增值税专用发票");
            main.setPropertyValue("invoiceNo", "AUDIT-ONLY");
        }
        if ("adjust".equals(document)) {
            main.setPropertyValue("adjustTime", LocalDateTime.now());
            main.setPropertyValue("adjustType", 20);
        }
        BeanWrapperImpl row = new BeanWrapperImpl(item);
        row.setPropertyValue("id", 1L);
        row.setPropertyValue("productId", 200L);
        if (row.isWritableProperty("warehouseId")) row.setPropertyValue("warehouseId", 7L);
        row.setPropertyValue("count", COUNT);
        row.setPropertyValue("adjust".equals(document) ? "newPrice" : "productPrice", PRICE);
        if (incremental) row.setPropertyValue("operation", "update");
        main.setPropertyValue("items", Collections.singletonList(item));
        return request;
    }

    private static void readItems(String document, Map<Class<?>, Object> dependencies) {
        switch (document) {
            case "in": ((ErpPurchaseInItemMapper) dependencies.get(ErpPurchaseInItemMapper.class)).selectListByInId(10L); break;
            case "return": ((ErpPurchaseReturnItemMapper) dependencies.get(ErpPurchaseReturnItemMapper.class)).selectListByReturnIdForUpdate(10L); break;
            case "invoice": ((ErpPurchaseInvoiceItemMapper) dependencies.get(ErpPurchaseInvoiceItemMapper.class)).selectListByInvoiceId(10L); break;
            default: ((ErpPurchasePriceAdjustItemMapper) dependencies.get(ErpPurchasePriceAdjustItemMapper.class)).selectListByAdjustId(10L);
        }
    }

    private static Object service(String document) {
        switch (document) {
            case "in": return new ErpPurchaseInServiceImpl();
            case "return": return new ErpPurchaseReturnServiceImpl();
            case "invoice": return new ErpPurchaseInvoiceServiceImpl();
            default: return new ErpPurchasePriceAdjustServiceImpl();
        }
    }

    private static Class<?> mainType(String document) {
        switch (document) {
            case "in": return ErpPurchaseInDO.class;
            case "return": return ErpPurchaseReturnDO.class;
            case "invoice": return ErpPurchaseInvoiceDO.class;
            default: return ErpPurchasePriceAdjustDO.class;
        }
    }

    private static Class<?> itemType(String document) {
        switch (document) {
            case "in": return ErpPurchaseInItemDO.class;
            case "return": return ErpPurchaseReturnItemDO.class;
            case "invoice": return ErpPurchaseInvoiceItemDO.class;
            default: return ErpPurchasePriceAdjustItemDO.class;
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
