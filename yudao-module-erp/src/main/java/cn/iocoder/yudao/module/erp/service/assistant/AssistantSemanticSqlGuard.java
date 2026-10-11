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
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        if(raw.matches("(?is).*\\bFOR\\s+UPDATE\\b.*") || raw.matches("(?is).*\\bLOCK\\s+IN\\s+SHARE\\s+MODE\\b.*")
                || raw.matches("(?is).*\\bINTO\\s+(?:OUTFILE|DUMPFILE)\\b.*")) throw invalid("SQL包含锁定或导出操作");
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
            AssistantSemanticCatalog.Dataset dataset=catalog.requirePublished(tables.iterator().next());
            if(select.getSelectItems().size()>dataset.getMaxSelectedColumns()) throw invalid("查询选择字段过多");
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
            int groupColumns=select.getGroupBy()==null || select.getGroupBy().getGroupByExpressions()==null
                    ?0:select.getGroupBy().getGroupByExpressions().size();
            if(groupColumns>dataset.getMaxGroupColumns()) throw invalid("分组字段过多");
            if(select.getOrderByElements()!=null && select.getOrderByElements().size()>4) throw invalid("排序字段过多");
            Set<String> operations=new LinkedHashSet<>();operations.add("PROJECT");
            if(select.getWhere()!=null || select.getHaving()!=null) operations.add("FILTER");
            if(visitor.aggregate || select.getDistinct()!=null) operations.add("AGGREGATE");
            if(groupColumns>0) operations.add("GROUP");
            if(select.getOrderByElements()!=null && !select.getOrderByElements().isEmpty()) operations.add("SORT");
            if(!dataset.getAllowedOperations().containsAll(operations)) throw invalid("查询包含数据集未登记的操作");
            Limit limit=select.getLimit();
            if(limit==null) {
                if(appendDefaultLimit) normalized=normalized+" LIMIT "+dataset.getMaxRows();
            } else if(!(limit.getRowCount() instanceof LongValue) || ((LongValue)limit.getRowCount()).getValue()>dataset.getMaxRows())
                throw invalid("结果最多返回"+dataset.getMaxRows()+"行");
            else if(limit.getOffset()!=null && (!(limit.getOffset() instanceof LongValue)
                    || ((LongValue)limit.getOffset()).getValue()>dataset.getMaxOffset())) throw invalid("分页偏移量无效");
            Offset offset=select.getOffset();
            if(offset!=null && (!(offset.getOffset() instanceof LongValue)
                    || ((LongValue)offset.getOffset()).getValue()>dataset.getMaxOffset())) throw invalid("分页偏移量无效");
            return new Validated(normalized,tables,visitor.referenced,visitor.parameters,operations,
                    select.getWhere()==null?"":select.getWhere().toString(),fingerprint(normalized));
        } catch(AssistantFailure e) {throw e;}
        catch(Exception e) {throw invalid("SQL结构无法验证");}
    }

    /** Applies dataset-specific scan controls after model parameters and server-resolved entities are known. */
    public void enforceCost(Validated checked,AssistantSemanticCatalog.Dataset dataset,
                            Map<String,Object> parameters,boolean resolvedEntityConstraint) {
        if(!dataset.isRequiresSelectiveFilter() || resolvedEntityConstraint) return;
        String where=checked.where;
        if(where==null || where.trim().isEmpty()) throw invalid("高数据量查询必须限定单据、业务对象或时间范围");
        if(Pattern.compile("(?i)\\bOR\\b").matcher(where).find())
            throw invalid("高数据量查询不允许使用OR绕过选择性筛选，请改用IN或收紧时间范围");
        for(String column:dataset.getSelectiveColumns()) {
            if(dataset.getTimeColumns().contains(column)) continue;
            if(hasExactFilter(where,column)) return;
        }
        for(String column:dataset.getTimeColumns()) if(validateTimeRange(where,column,parameters,dataset.getMaxTimeRangeDays())) return;
        throw invalid("高数据量查询必须使用登记的标识精确筛选，或提供有起止边界的时间范围");
    }

    private static boolean hasExactFilter(String where,String column) {
        String ref="(?:\\b[A-Za-z_][A-Za-z0-9_]*\\.)?"+Pattern.quote(column)+"\\b";
        return Pattern.compile("(?i)"+ref+"\\s*=\\s*:p(?:[1-9]|1[0-9]|20)\\b").matcher(where).find()
                || Pattern.compile("(?i)"+ref+"\\s+IN\\s*\\(\\s*:p(?:[1-9]|1[0-9]|20)(?:\\s*,\\s*:p(?:[1-9]|1[0-9]|20))*\\s*\\)").matcher(where).find();
    }

    private static boolean validateTimeRange(String where,String column,Map<String,Object> parameters,int maximumDays) {
        String ref="(?:\\b[A-Za-z_][A-Za-z0-9_]*\\.)?"+Pattern.quote(column)+"\\b";
        Matcher equal=Pattern.compile("(?i)"+ref+"\\s*=\\s*:(p(?:[1-9]|1[0-9]|20))\\b").matcher(where);
        if(equal.find()) {temporal(parameters.get(equal.group(1)));return true;}
        Matcher between=Pattern.compile("(?i)"+ref+"\\s+BETWEEN\\s+:(p(?:[1-9]|1[0-9]|20))\\s+AND\\s+:(p(?:[1-9]|1[0-9]|20))").matcher(where);
        if(between.find()) {validateSpan(parameters.get(between.group(1)),parameters.get(between.group(2)),maximumDays);return true;}
        Matcher comparison=Pattern.compile("(?i)"+ref+"\\s*(>=|>|<=|<)\\s*:(p(?:[1-9]|1[0-9]|20))\\b").matcher(where);
        Object lower=null,upper=null;
        while(comparison.find()) {
            if(comparison.group(1).startsWith(">")) lower=parameters.get(comparison.group(2));
            else upper=parameters.get(comparison.group(2));
        }
        if(lower==null || upper==null) return false;
        validateSpan(lower,upper,maximumDays);return true;
    }

    private static void validateSpan(Object lower,Object upper,int maximumDays) {
        LocalDateTime start=temporal(lower),end=temporal(upper);
        if(end.isBefore(start)) throw new AssistantFailure("MODEL_INVALID","时间范围起止顺序无效");
        long days=ChronoUnit.DAYS.between(start.toLocalDate(),end.toLocalDate())+1;
        if(days>maximumDays) throw invalid("时间范围超过该数据集允许的"+maximumDays+"天");
    }

    private static LocalDateTime temporal(Object value) {
        if(value instanceof java.sql.Timestamp) return ((java.sql.Timestamp)value).toLocalDateTime();
        if(value instanceof java.sql.Date) return ((java.sql.Date)value).toLocalDate().atStartOfDay();
        if(value instanceof LocalDateTime) return (LocalDateTime)value;
        if(value instanceof LocalDate) return ((LocalDate)value).atStartOfDay();
        if(value instanceof String) {
            String text=((String)value).trim();
            try {return LocalDateTime.parse(text.replace(' ','T'));} catch(Exception ignored) {}
            try {return LocalDate.parse(text).atStartOfDay();} catch(Exception ignored) {}
        }
        throw new AssistantFailure("MODEL_INVALID","时间筛选参数格式无效");
    }
    private static void accept(Expression expression,ExpressionVisitor visitor){if(expression!=null) expression.accept(visitor);}
    private static AssistantFailure invalid(String text){return new AssistantFailure("SQL_REJECTED",text);}
    public static String fingerprint(String value) {
        try {
            String safe=value==null?"":value;
            StringBuilder result=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(safe.getBytes(StandardCharsets.UTF_8))) result.append(String.format("%02x",b));return result.toString();
        } catch(Exception e) {throw new IllegalStateException(e);}
    }
    private static final class Validator extends ExpressionVisitorAdapter {
        private final Set<String> columns,sourceColumns;
        private final Set<String> referenced=new LinkedHashSet<>();
        private final Set<String> parameters=new LinkedHashSet<>();
        private boolean aggregate;
        Validator(Set<String> columns,Set<String> sourceColumns){this.columns=columns;this.sourceColumns=sourceColumns;}
        @Override public void visit(Column column) {
            String name=column.getColumnName().toLowerCase(Locale.ROOT);
            if(!columns.contains(name)) throw invalid("查询包含未公开字段: "+name);
            if(sourceColumns.contains(name)) referenced.add(name);
        }
        @Override public void visit(Function function) {
            if(function.getName()==null || !FUNCTIONS.contains(function.getName().toUpperCase(Locale.ROOT))) throw invalid("查询包含未允许函数");
            if(function.isAllColumns() && !"COUNT".equalsIgnoreCase(function.getName())) throw invalid("仅COUNT允许星号参数");
            if(Arrays.asList("COUNT","SUM","MIN","MAX","AVG").contains(function.getName().toUpperCase(Locale.ROOT))) aggregate=true;
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
        private final String sql,fingerprint,where;private final Set<String> datasets,columns,parameters,operations;
        Validated(String sql,Set<String> datasets,Set<String> columns,Set<String> parameters,Set<String> operations,String where,String fingerprint){this.sql=sql;this.datasets=Collections.unmodifiableSet(new LinkedHashSet<>(datasets));this.columns=Collections.unmodifiableSet(new LinkedHashSet<>(columns));this.parameters=Collections.unmodifiableSet(new LinkedHashSet<>(parameters));this.operations=Collections.unmodifiableSet(new LinkedHashSet<>(operations));this.where=where;this.fingerprint=fingerprint;}
        public String getSql(){return sql;}public Set<String> getDatasets(){return datasets;}public Set<String> getColumns(){return columns;}public Set<String> getParameters(){return parameters;}public Set<String> getOperations(){return operations;}public String getFingerprint(){return fingerprint;}
    }
}
