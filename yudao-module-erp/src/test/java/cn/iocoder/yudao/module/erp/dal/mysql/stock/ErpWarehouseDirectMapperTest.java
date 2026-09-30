package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErpWarehouseDirectMapperTest {

    @Test
    void directQueryUsesFlagDepartmentTenantStatusAndLogicalDeletion() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:warehouse_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        MybatisConfiguration config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.setEnvironment(new Environment("test", new JdbcTransactionFactory(), ds));
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties())));
        config.addInterceptor(interceptor);
        config.addMapper(ErpWarehouseMapper.class);
        SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder().build(config);

        String columns = TableInfoHelper.getTableInfo(ErpWarehouseDO.class).getFieldList().stream()
                .map(field -> field.getColumn() + " " + sqlType(field))
                .collect(Collectors.joining(","));
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE erp_warehouse (id bigint PRIMARY KEY,tenant_id bigint," + columns + ")");
            stmt.execute("INSERT INTO erp_warehouse(id,tenant_id,dept_id,name,direct_warehouse,status,deleted) VALUES "
                    + "(1,1,20,'甘孜分公司直发仓',true,0,false),"
                    + "(2,1,20,'普通直发仓',false,0,false),"
                    + "(3,1,20,'直发仓',false,0,false),"
                    + "(4,2,20,'其他租户',true,0,false),"
                    + "(5,1,21,'其他部门',true,0,false),"
                    + "(6,1,20,'已停用',true,1,false),"
                    + "(7,1,20,'已删除',true,0,true),"
                    + "(8,1,20,'用户自定义名称',true,0,false)");
        }
        try (SqlSession session = factory.openSession()) {
            TenantContextHolder.setTenantId(1L);
            ErpWarehouseMapper mapper = session.getMapper(ErpWarehouseMapper.class);
            List<Long> ids = mapper.selectDirectListByDeptIdAndStatus(20L, 0).stream()
                    .map(ErpWarehouseDO::getId).sorted().collect(Collectors.toList());
            assertEquals(java.util.Arrays.asList(1L, 8L), ids);
            TenantContextHolder.setTenantId(2L);
            assertEquals(java.util.Collections.singletonList(4L),
                    mapper.selectDirectListByDeptIdAndStatus(20L, 0).stream()
                            .map(ErpWarehouseDO::getId).collect(Collectors.toList()));
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static String sqlType(TableFieldInfo field) {
        Class<?> type = field.getPropertyType();
        if (type == Boolean.class) return "boolean";
        if (type == Long.class) return "bigint";
        if (type == Integer.class) return "int";
        if (type == BigDecimal.class) return "decimal(18,6)";
        if (type == LocalDateTime.class) return "timestamp";
        return "varchar(255)";
    }
}
