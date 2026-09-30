package cn.iocoder.yudao.module.erp.service.assistant;

import com.alibaba.druid.pool.DruidDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.sql.*;
import java.util.*;

/** Dedicated parameterized executor for validated semantic SQL; never falls back to the write datasource. */
@Component
public class AssistantSemanticReadOnly {
    private static final Logger log=LoggerFactory.getLogger(AssistantSemanticReadOnly.class);
    @Resource private AssistantProperties properties;
    private DruidDataSource datasource;
    private volatile boolean grantsChecked;

    public List<Map<String,Object>> query(String sql,List<Object> parameters,int maximumRows) {
        AssistantTask.checkCurrent();int cap=Math.max(1,Math.min(200,maximumRows));
        try(Connection connection=datasource().getConnection()) {
            checkGrants(connection);connection.setReadOnly(true);connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);connection.setAutoCommit(false);
            try(PreparedStatement statement=connection.prepareStatement(sql)) {
                statement.setQueryTimeout(10);statement.setMaxRows(cap+1);
                if(AssistantTask.current()!=null) AssistantTask.current().statement(statement);
                for(int i=0;i<parameters.size();i++) statement.setObject(i+1,parameters.get(i));
                try(ResultSet result=statement.executeQuery()) {
                    ResultSetMetaData metadata=result.getMetaData();List<Map<String,Object>> rows=new ArrayList<>();
                    while(result.next()) {
                        if(rows.size()>=cap) throw new AssistantFailure("RESULT_TOO_LARGE","查询结果超过200行，请增加筛选条件");
                        Map<String,Object> row=new LinkedHashMap<>();
                        for(int i=1;i<=metadata.getColumnCount();i++) row.put(metadata.getColumnLabel(i),result.getObject(i));
                        rows.add(row);
                    }
                    return rows;
                }
            }
        } catch(AssistantFailure e) {throw e;}
        catch(SQLException e) {
            log.warn("Assistant read-only query failed: sqlState={}, errorCode={}, message={}",
                    e.getSQLState(),e.getErrorCode(),e.getMessage());
            AssistantTask.checkCurrent();throw new AssistantFailure("QUERY_FAILED","业务查询失败，请调整条件后重试");
        }
        finally {if(AssistantTask.current()!=null) AssistantTask.current().statement(null);}
    }

    private synchronized DruidDataSource datasource() {
        AssistantProperties.ReadOnly config=properties.getReadOnly();
        if(config.getUrl()==null || !config.getUrl().startsWith("jdbc:mysql://") || config.getUsername()==null
                || config.getUsername().trim().isEmpty() || config.getPassword()==null || config.getPassword().isEmpty())
            throw new AssistantFailure("READ_ONLY_NOT_CONFIGURED","正式业务只读账号尚未配置");
        if(datasource==null) {
            DruidDataSource value=new DruidDataSource();value.setName("assistantSemanticReadOnly");value.setUrl(config.getUrl());
            value.setUsername(config.getUsername());value.setPassword(config.getPassword());value.setInitialSize(0);value.setMinIdle(0);value.setMaxActive(4);
            value.setMaxWait(5000);value.setQueryTimeout(10);value.setValidationQuery("SELECT 1");value.setValidationQueryTimeout(3);
            value.setConnectionProperties("connectTimeout=5000;socketTimeout=10000");value.setDefaultReadOnly(true);datasource=value;
        }
        return datasource;
    }

    private synchronized void checkGrants(Connection connection) throws SQLException {
        if(grantsChecked) return;boolean select=false;
        try(Statement statement=connection.createStatement()) {
            statement.setQueryTimeout(3);
            try(ResultSet grants=statement.executeQuery("SHOW GRANTS")) {
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
            }
        }
        if(!select) throw new AssistantFailure("READ_ONLY_PRIVILEGES","业务查询账号尚未获得 SELECT 权限");
        grantsChecked=true;
    }

    @PreDestroy public synchronized void close(){if(datasource!=null)datasource.close();}
}
