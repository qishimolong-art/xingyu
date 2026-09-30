package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.module.erp.dal.mysql.assistant.AssistantQueryMapper;
import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import javax.annotation.PreDestroy;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;

/** Explicit connection: never participates in or falls back to the ERP write datasource. */
@Component
public class AssistantReadOnly {
    @Resource private AssistantProperties properties;
    @Resource private SqlSessionFactory sqlSessionFactory;
    private DruidDataSource datasource;
    private SqlSessionFactory readFactory;
    private volatile boolean grantsChecked;
    private final ThreadLocal<SqlSession> sessions=new ThreadLocal<>();
    public boolean configured() {
        AssistantProperties.ReadOnly p=properties.getReadOnly();
        return p.getUrl()!=null && p.getUrl().startsWith("jdbc:mysql://") && p.getUsername()!=null && !p.getUsername().trim().isEmpty()
            && p.getPassword()!=null && !p.getPassword().isEmpty();
    }
    private synchronized DruidDataSource datasource() {
        if(!configured()) throw new AssistantFailure("READ_ONLY_NOT_CONFIGURED","正式业务只读账号尚未配置");
        if(datasource==null) {
            AssistantProperties.ReadOnly p=properties.getReadOnly();
            DruidDataSource ds=new DruidDataSource(); ds.setName("assistantReadOnly");
            ds.setUrl(p.getUrl());ds.setUsername(p.getUsername());ds.setPassword(p.getPassword());
            ds.setInitialSize(0);ds.setMinIdle(0);ds.setMaxActive(4);ds.setMaxWait(5000);
            ds.setQueryTimeout(10);ds.setValidationQuery("SELECT 1");ds.setValidationQueryTimeout(3);
            ds.setConnectionProperties("connectTimeout=5000;socketTimeout=10000");
            ds.setDefaultReadOnly(true);datasource=ds;
        }
        return datasource;
    }
    public void probe() { snapshot(()->{try(Statement s=sessions.get().getConnection().createStatement()) {s.setQueryTimeout(3);s.execute("SELECT 1");return true;}catch(SQLException e){throw unavailable();}}); }
    private AssistantFailure unavailable() {return new AssistantFailure("READ_ONLY_UNAVAILABLE","业务只读数据源不可用，请联系管理员检查连接和授权");}
    private synchronized SqlSessionFactory factory() {
        if(readFactory==null) {
            com.baomidou.mybatisplus.core.MybatisConfiguration config=new com.baomidou.mybatisplus.core.MybatisConfiguration();
            config.setMapUnderscoreToCamelCase(true);config.setCacheEnabled(false);
            config.setLocalCacheScope(org.apache.ibatis.session.LocalCacheScope.STATEMENT);
            config.setLogImpl(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
            config.setEnvironment(new org.apache.ibatis.mapping.Environment("assistantReadOnly",new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),datasource()));
            // Reuse tenant/data interceptors, but never the Spring write transaction factory.
            for(org.apache.ibatis.plugin.Interceptor interceptor:sqlSessionFactory.getConfiguration().getInterceptors()) config.addInterceptor(interceptor);
            config.addMapper(AssistantQueryMapper.class);
            readFactory=new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(config);
        }
        return readFactory;
    }
    public <T> T snapshot(Supplier<T> work) {
        AssistantTask.checkCurrent();
        if(sessions.get()!=null) return work.get();
        try(Connection connection=datasource().getConnection()) {
            checkGrants(connection);
            connection.setReadOnly(true);connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);connection.setAutoCommit(false);
            // Register the actual prepared statement without changing the shared MyBatis configuration.
            Connection tracked=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(proxy,method,args)->{
                try {
                    Object value=method.invoke(connection,args);
                    if(value instanceof Statement && AssistantTask.current()!=null) AssistantTask.current().statement((Statement)value);
                    return value;
                } catch(InvocationTargetException e) {throw e.getCause();}
            });
            try(SqlSession session=factory().openSession(tracked)) {
                sessions.set(session); T result=work.get();AssistantTask.checkCurrent();return result;
            } finally {sessions.remove();if(AssistantTask.current()!=null) AssistantTask.current().statement(null);}
        } catch(AssistantFailure e) {throw e;}
        catch(IllegalArgumentException e) {throw e;}
        catch(Exception e) {AssistantTask.checkCurrent();throw unavailable();}
    }
    private synchronized void checkGrants(Connection connection) throws SQLException {
        if(grantsChecked) return;
        try(Statement statement=connection.createStatement()) {
            statement.setQueryTimeout(3);
            try(ResultSet grants=statement.executeQuery("SHOW GRANTS")) {
                boolean select=false;
                while(grants.next()) {
                    String grant=grants.getString(1).toUpperCase(Locale.ROOT);
                    if(grant.startsWith("GRANT USAGE ON *.* TO ")) continue;
                    if(!grant.startsWith("GRANT SELECT ON ") || grant.contains("WITH GRANT OPTION") || grant.startsWith("GRANT SELECT ON *.*"))
                        throw new AssistantFailure("READ_ONLY_PRIVILEGES","业务查询账号必须只具有目标业务库的 SELECT 权限");
                    String catalog=connection.getCatalog();
                    if(catalog==null || !grant.startsWith("GRANT SELECT ON `"+catalog.toUpperCase(Locale.ROOT)+"`."))
                        throw new AssistantFailure("READ_ONLY_PRIVILEGES","业务查询账号的授权范围与目标业务库不一致");
                    select=true;
                }
                if(!select) throw new AssistantFailure("READ_ONLY_PRIVILEGES","业务查询账号尚未获得 SELECT 权限");
            }
        }
        grantsChecked=true;
    }
    public List<Map<String,Object>> select(Map<String,Object> args) {
        AssistantTask.checkCurrent();
        if(sessions.get()==null) throw new IllegalStateException("Assistant read-only snapshot required");
        return sessions.get().getMapper(AssistantQueryMapper.class).select(args);
    }
    @PreDestroy public synchronized void close() {if(datasource!=null) datasource.close();}
}
