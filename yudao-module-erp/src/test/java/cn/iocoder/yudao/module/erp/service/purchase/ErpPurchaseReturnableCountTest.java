package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.util.JdbcUtils;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.ErpPurchaseReturnController;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInReturnableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.apache.ibatis.mapping.Environment;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 真实 H2 / Mapper 校验：待审核占用、编辑排除本单、分页过滤与提示精度。 */
class ErpPurchaseReturnableCountTest {
    private JdbcTemplate jdbc;
    private ErpPurchaseInItemMapper inItems;
    private ErpPurchaseReturnItemMapper returnItems;
    private ErpProductMapper productMapper;
    private ErpPurchaseInServiceImpl inService;
    private ErpPurchaseReturnServiceImpl returnService;
    private ErpPurchaseReturnController controller;
    private MockedStatic<JdbcUtils> databaseType;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:returnable_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        MybatisConfiguration config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), ds));
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                new TenantDatabaseInterceptor(new TenantProperties())));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        config.addInterceptor(interceptor);
        config.addMapper(ErpPurchaseInMapper.class);
        config.addMapper(ErpPurchaseInItemMapper.class);
        config.addMapper(ErpPurchaseReturnMapper.class);
        config.addMapper(ErpPurchaseReturnItemMapper.class);
        SqlSessionTemplate session = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        jdbc = new JdbcTemplate(ds);
        for (Class<?> type : List.of(ErpPurchaseInDO.class, ErpPurchaseInItemDO.class,
                ErpPurchaseReturnDO.class, ErpPurchaseReturnItemDO.class)) {
            createTable(type);
        }
        inItems = session.getMapper(ErpPurchaseInItemMapper.class);
        returnItems = session.getMapper(ErpPurchaseReturnItemMapper.class);
        Map<Class<?>, Object> dependencies = new HashMap<>();
        dependencies.put(ErpPurchaseInMapper.class, session.getMapper(ErpPurchaseInMapper.class));
        dependencies.put(ErpPurchaseInItemMapper.class, inItems);
        dependencies.put(ErpPurchaseReturnItemMapper.class, returnItems);
        productMapper = mock(ErpProductMapper.class);
        when(productMapper.selectById(6243L)).thenReturn(new ErpProductDO()
                .setId(6243L).setName("孚配清洗剂").setCode("FPPCAN00450"));
        dependencies.put(ErpProductMapper.class, productMapper);
        inService = new ErpPurchaseInServiceImpl();
        returnService = new ErpPurchaseReturnServiceImpl();
        controller = new ErpPurchaseReturnController();
        inject(inService, dependencies);
        inject(returnService, dependencies);
        inject(controller, dependencies);
        databaseType = mockStatic(JdbcUtils.class, CALLS_REAL_METHODS);
        databaseType.when(JdbcUtils::getDbType).thenReturn(DbType.H2);
        TenantContextHolder.setTenantId(1L);
        jdbc.update("INSERT INTO erp_purchase_in(id,tenant_id,deleted,no,status) VALUES(364,1,false,'CGRK',20)");
        jdbc.update("INSERT INTO erp_purchase_in_items(id,in_id,tenant_id,deleted,product_id,count)"
                + " VALUES(7505,364,1,false,6243,1)");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        if (databaseType != null) databaseType.close();
    }

    @ParameterizedTest
    @CsvSource({"10,0,0", "20,0,1", "0,1,0", "30,1,0"})
    void candidateCountAndPageFilterUseSameOccupancy(int status, int available, int approved) {
        addReturn(13, status, "1.000000", 1, false, false);
        var row = inService.getReturnableItemsByInId(364L).get(0);
        assertEquals(0, row.getReturnableCount().compareTo(BigDecimal.valueOf(available)));
        assertEquals(0, row.getReturnedCount().compareTo(BigDecimal.valueOf(approved)));
        var request = new ErpPurchaseInReturnableItemPageReqVO();
        request.setInId(364L);
        request.setPageNo(1);
        request.setPageSize(20);
        var page = inService.getReturnableItemPage(request);
        assertEquals(available, page.getTotal());
        assertEquals(available, page.getList().size());
    }

    @Test
    void deletedAndOtherTenantReturnsDoNotConsumeQuota() {
        addReturn(13, 10, "1", 1, true, false);
        addReturn(14, 20, "1", 1, false, true);
        addReturn(15, 10, "1", 2, false, false);
        assertEquals(0, inService.getReturnableItemsByInId(364L).get(0)
                .getReturnableCount().compareTo(BigDecimal.ONE));
        assertTrue(returnItems.selectProcessingAndApprovedReturnedCountMapBySourceInItemIdsExcludeReturn(
                List.of(7505L), null).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"10", "20", "0"})
    void editingExcludesCurrentDocumentButIncludesOtherProcessingRows(int currentStatus) {
        addReturn(13, currentStatus, "0.4", 1, false, false);
        addReturn(14, 10, "0.250000", 1, false, false);
        // 本单同来源多行均应排除，不仅仅是当前行。
        jdbc.update("INSERT INTO erp_purchase_return_items(id,return_id,tenant_id,deleted,source_in_item_id,count)"
                + " VALUES(999,13,1,false,7505,0.1)");
        List<ErpPurchaseReturnRespVO.Item> rows = ReflectionTestUtils.invokeMethod(controller,
                "buildPurchaseReturnItemVOList", returnItems.selectListByReturnId(13L), 13L);
        assertNotNull(rows);
        assertEquals(2, rows.size());
        rows.forEach(row -> assertEquals(0, row.getReturnableCount().compareTo(new BigDecimal("0.75"))));
    }

    @Test
    void sortingUsesQuotaExcludingCurrentDocument() {
        addReturn(13, 10, "0.9", 1, false, false);
        addReturn(14, 10, "0.25", 1, false, false);
        jdbc.update("INSERT INTO erp_purchase_in_items(id,in_id,tenant_id,deleted,count) VALUES(7506,364,1,false,0.8)");
        jdbc.update("INSERT INTO erp_purchase_return_items(id,return_id,tenant_id,deleted,source_in_item_id,count)"
                + " VALUES(999,13,1,false,7506,0.8)");
        // 7505 可退 .75，7506 可退 .8；忽略待审核时旧排序会相反。
        assertEquals(List.of(13L, 999L), returnItems.selectListByReturnId(13L, "returnableCount", "asc")
                .stream().map(ErpPurchaseReturnItemDO::getId).collect(Collectors.toList()));
    }

    @ParameterizedTest
    @CsvSource({"0", "10", "20"})
    void exportExcludesAllCurrentRowsWithSameSource(int currentStatus) {
        addReturn(13, currentStatus, "0.4", 1, false, false);
        addReturn(14, 10, "0.25", 1, false, false);
        jdbc.update("INSERT INTO erp_purchase_return_items(id,return_id,tenant_id,deleted,source_in_item_id,count)"
                + " VALUES(999,13,1,false,7505,0.1)");
        List<ErpPurchaseReturnRespVO.Item> items = ReflectionTestUtils.invokeMethod(controller,
                "buildPurchaseReturnItemVOList", returnItems.selectListByReturnId(13L), 13L);
        var document = new ErpPurchaseReturnRespVO();
        document.setId(13L);
        document.setStatus(currentStatus);
        document.setItems(items);
        List<cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnExportRespVO> rows =
                ReflectionTestUtils.invokeMethod(controller, "buildPurchaseReturnExportList", List.of(document));
        assertNotNull(rows);
        assertEquals(2, rows.size());
        rows.forEach(row -> assertEquals(0, row.getReturnableCount().compareTo(new BigDecimal("0.75"))));
    }

    @ParameterizedTest
    @CsvSource({"1.000000,1.000000,0,1", "0.875000,0.125001,0.125,0.125001",
            "0.000000,1000.000000,1,1000", "2.000000,1.000000,0,1"})
    void exceededQuotaMessageTrimsZerosWithoutRounding(String occupied, String requested,
                                                       String availableText, String requestedText) {
        addReturn(13, 10, occupied, 1, false, false);
        var item = new ErpPurchaseReturnSaveReqVO.Item();
        item.setSourceInItemId(7505L);
        item.setCount(new BigDecimal(requested));
        // 提示必须使用真实来源，不能让前端传入的描述覆盖商品及来源单号。
        item.setProductName("伪造商品");
        item.setProductCode("伪造编码");
        item.setSourceInNo("伪造来源单");
        ServiceException error = assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                returnService, "validateReturnableCountForByOrderIncludingProcessing", List.of(item), null));
        assertEquals("商品【孚配清洗剂】（商品编码：FPPCAN00450，原入库单：CGRK）"
                + "：退货数量超过可退数量，可退 " + availableText
                + "，本次退货 " + requestedText, error.getMessage());
    }

    @Test
    void missingMetadataStillReportsQuotaErrorWithSourceFallback() {
        addReturn(13, 10, "1", 1, false, false);
        when(productMapper.selectById(6243L)).thenReturn(null);
        jdbc.update("DELETE FROM erp_purchase_in WHERE id=364");
        var item = new ErpPurchaseReturnSaveReqVO.Item();
        item.setSourceInItemId(7505L);
        item.setCount(BigDecimal.ONE);
        ServiceException error = assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                returnService, "validateReturnableCountForByOrderIncludingProcessing", List.of(item), null));
        assertEquals("商品【未找到商品资料】（商品编码：商品编号 6243，原入库单：入库单编号 364）"
                + "：退货数量超过可退数量，可退 0，本次退货 1", error.getMessage());
    }

    @Test
    void validQuotaDoesNotQueryMessageMetadata() {
        var headerMapper = mock(ErpPurchaseInMapper.class);
        ReflectionTestUtils.setField(returnService, "purchaseInMapper", headerMapper);
        var item = new ErpPurchaseReturnSaveReqVO.Item();
        item.setSourceInItemId(7505L);
        item.setCount(BigDecimal.ONE);
        ReflectionTestUtils.invokeMethod(returnService,
                "validateReturnableCountForByOrderIncludingProcessing", List.of(item), null);
        verifyNoInteractions(headerMapper, productMapper);
    }

    private void addReturn(long id, int status, String count, long tenant, boolean headerDeleted,
                           boolean itemDeleted) {
        jdbc.update("INSERT INTO erp_purchase_return(id,tenant_id,deleted,status,no) VALUES(?,?,?,?,?)",
                id, tenant, headerDeleted, status, "CGTH-" + id);
        jdbc.update("INSERT INTO erp_purchase_return_items(id,return_id,tenant_id,deleted,source_in_item_id,count)"
                + " VALUES(?,?,?,?,7505,?)", id, id, tenant, itemDeleted, new BigDecimal(count));
    }

    private static void inject(Object target, Map<Class<?>, Object> dependencies) throws Exception {
        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Resource.class)) {
                field.setAccessible(true);
                field.set(target, dependencies.computeIfAbsent(field.getType(), type -> mock(type)));
            }
        }
    }

    private void createTable(Class<?> type) {
        var table = TableInfoHelper.getTableInfo(type);
        String columns = table.getFieldList().stream().map(field -> {
            Class<?> property = field.getPropertyType();
            String sqlType = property == BigDecimal.class ? "decimal(24,6)"
                    : property == Boolean.class ? "boolean DEFAULT false"
                    : property == Long.class ? "bigint" : property == Integer.class ? "int"
                    : property == LocalDateTime.class ? "timestamp" : "varchar(255)";
            return field.getColumn() + " " + sqlType;
        }).collect(Collectors.joining(","));
        jdbc.execute("CREATE TABLE " + table.getTableName()
                + " (id bigint PRIMARY KEY,tenant_id bigint DEFAULT 1," + columns + ")");
    }
}
