package cn.iocoder.yudao.module.erp.service.assistant;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** AST validation for model SQL. Execution still revalidates through this class. */
@Component
public class AssistantSemanticSqlGuard {
    @Resource private AssistantSemanticCatalog catalog;
    private static final Set<String> FUNCTIONS=new HashSet<>(Arrays.asList(
            "COUNT","SUM","MIN","MAX","AVG","COALESCE","ROUND","DATE","DATE_FORMAT","ABS"));

    public Validated validate(String raw) {
        return validate(raw,true);
    }

    public Validated validateServerOwnedPage(String raw) {
        return validate(raw,false);
    }

    private Validated validate(String raw,boolean appendDefaultLimit) {
        if(raw==null || raw.trim().isEmpty() || raw.length()>12000) throw invalid("SQL为空或过长");
        if(raw.contains(";") || raw.contains("--") || raw.contains("/*") || raw.contains("#")) throw invalid("SQL包含多个语句或注释");
        try {
            Statement statement=CCJSqlParserUtil.parse(raw);
            if(!(statement instanceof PlainSelect)) throw invalid("只允许单条SELECT");
            PlainSelect select=(PlainSelect)statement;
            if(select.getWithItemsList()!=null && !select.getWithItemsList().isEmpty()) throw invalid("不支持WITH查询");
            if(select.getIntoTables()!=null && !select.getIntoTables().isEmpty()) throw invalid("不允许SELECT INTO");
            if(select.getWindowDefinitions()!=null && !select.getWindowDefinitions().isEmpty()) throw invalid("不支持窗口函数");
            String normalized=select.toString();
            if(normalized.matches("(?is).*[\\(]\\s*SELECT\\s+.*")) throw invalid("不支持子查询");
            for(SelectItem<?> item:select.getSelectItems())
                if(item.getExpression() instanceof AllColumns || item.getExpression() instanceof AllTableColumns) throw invalid("不允许SELECT *");
            if(select.getJoins()!=null && !select.getJoins().isEmpty()) throw invalid("首版语义查询不允许自行关联数据集");
            Set<String> tables=new LinkedHashSet<>();for(String table:new TablesNamesFinder().getTableList(statement)) tables.add(table.toLowerCase(Locale.ROOT));
            if(tables.size()!=1) throw invalid("首版智能查询一次只允许一个语义数据集");
            Set<String> allowedColumns=new HashSet<>(),sourceColumns=new HashSet<>();
            for(String table:tables) for(String column:catalog.requirePublished(table).getColumns().keySet())
                {allowedColumns.add(column.toLowerCase(Locale.ROOT));sourceColumns.add(column.toLowerCase(Locale.ROOT));}
            for(SelectItem<?> item:select.getSelectItems()) if(item.getAlias()!=null)
                allowedColumns.add(item.getAlias().getName().toLowerCase(Locale.ROOT));
            Validator visitor=new Validator(allowedColumns,sourceColumns);
            for(SelectItem<?> item:select.getSelectItems()) item.getExpression().accept(visitor);
            accept(select.getWhere(),visitor);accept(select.getHaving(),visitor);
            if(select.getGroupBy()!=null && select.getGroupBy().getGroupByExpressions()!=null)
                for(Object value:select.getGroupBy().getGroupByExpressions()) if(value instanceof Expression) accept((Expression)value,visitor);
            if(select.getOrderByElements()!=null) for(OrderByElement value:select.getOrderByElements()) accept(value.getExpression(),visitor);
            if(select.getJoins()!=null) for(Join join:select.getJoins()) for(Expression value:join.getOnExpressions()) accept(value,visitor);
            Limit limit=select.getLimit();
            if(limit==null) {
                if(appendDefaultLimit) normalized=normalized+" LIMIT 200";
            } else if(!(limit.getRowCount() instanceof LongValue) || ((LongValue)limit.getRowCount()).getValue()>200) throw invalid("结果最多返回200行");
            else if(limit.getOffset()!=null && (!(limit.getOffset() instanceof LongValue)
                    || ((LongValue)limit.getOffset()).getValue()>10_000)) throw invalid("分页偏移量无效");
            Offset offset=select.getOffset();
            if(offset!=null && (!(offset.getOffset() instanceof LongValue)
                    || ((LongValue)offset.getOffset()).getValue()>10_000)) throw invalid("分页偏移量无效");
            return new Validated(normalized,tables,visitor.referenced,visitor.parameters,hash(normalized));
        } catch(AssistantFailure e) {throw e;}
        catch(Exception e) {throw invalid("SQL结构无法验证");}
    }
    private static void accept(Expression expression,ExpressionVisitor visitor){if(expression!=null) expression.accept(visitor);}
    private static AssistantFailure invalid(String text){return new AssistantFailure("SQL_REJECTED",text);}
    private static String hash(String value) throws Exception {
        StringBuilder result=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))) result.append(String.format("%02x",b));return result.toString();
    }
    private static final class Validator extends ExpressionVisitorAdapter {
        private final Set<String> columns,sourceColumns;
        private final Set<String> referenced=new LinkedHashSet<>();
        private final Set<String> parameters=new LinkedHashSet<>();
        Validator(Set<String> columns,Set<String> sourceColumns){this.columns=columns;this.sourceColumns=sourceColumns;}
        @Override public void visit(Column column) {
            String name=column.getColumnName().toLowerCase(Locale.ROOT);
            if(!columns.contains(name)) throw invalid("查询包含未公开字段: "+name);
            if(sourceColumns.contains(name)) referenced.add(name);
        }
        @Override public void visit(Function function) {
            if(function.getName()==null || !FUNCTIONS.contains(function.getName().toUpperCase(Locale.ROOT))) throw invalid("查询包含未允许函数");
            if(function.isAllColumns() && !"COUNT".equalsIgnoreCase(function.getName())) throw invalid("仅COUNT允许星号参数");
            super.visit(function);
        }
        @Override public void visit(AnalyticExpression expression){throw invalid("不支持窗口函数");}
        @Override public void visit(UserVariable variable){throw invalid("不支持用户变量");}
        @Override public void visit(StringValue value){throw invalid("筛选值必须使用命名参数");}
        @Override public void visit(DateValue value){throw invalid("日期必须使用命名参数");}
        @Override public void visit(TimestampValue value){throw invalid("时间必须使用命名参数");}
        @Override public void visit(JdbcNamedParameter value){
            if(value.getName()==null || !value.getName().matches("p(?:[1-9]|1[0-9]|20)")) throw invalid("筛选参数必须使用:p1至:p20");
            parameters.add(value.getName());
        }
    }
    public static final class Validated {
        private final String sql,fingerprint;private final Set<String> datasets,columns,parameters;
        Validated(String sql,Set<String> datasets,Set<String> columns,Set<String> parameters,String fingerprint){this.sql=sql;this.datasets=Collections.unmodifiableSet(new LinkedHashSet<>(datasets));this.columns=Collections.unmodifiableSet(new LinkedHashSet<>(columns));this.parameters=Collections.unmodifiableSet(new LinkedHashSet<>(parameters));this.fingerprint=fingerprint;}
        public String getSql(){return sql;}public Set<String> getDatasets(){return datasets;}public Set<String> getColumns(){return columns;}public Set<String> getParameters(){return parameters;}public String getFingerprint(){return fingerprint;}
    }
}
