package cn.iocoder.yudao.module.erp.service.stock.report;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockReportV2Models.*;
import cn.iocoder.yudao.module.erp.service.stock.report.ErpStockReportV2Repository.Scope;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;

@Service
public class ErpStockReportV2Service {
    @Resource private ErpStockReportV2Repository repository;
    @Resource private PermissionApi permissionApi;
    @Value("${erp.reporting.dual-cost-enabled:false}") private boolean enabled;
    @Value("${erp.reporting.dual-cost-cutover:}") private String cutover;

    public Status status() {
        if (!enabled) return new Status().setStatus("DISABLED").setReason("新核算尚未启用，请完成期初核对及各业务接入后启用");
        LocalDateTime start;
        try { start=LocalDateTime.parse(cutover); }
        catch (RuntimeException ex) { return new Status().setStatus("INVALID_CONFIG").setReason("双成本切换时间未正确配置"); }
        if (start.isAfter(LocalDateTime.now())) return new Status().setStatus("INVALID_CONFIG").setCutoverAt(start).setReason("尚未到达配置的切换时间");
        try { repository.checkSchema(); }
        catch (BadSqlGrammarException ex) { return new Status().setStatus("SCHEMA_MISSING").setCutoverAt(start).setReason("新版核算数据表尚未迁移或版本不匹配"); }
        return new Status().setStatus("READY").setCutoverAt(start).setReason("按已核对期初和追加过账事件查询，缺失数据将在结果中标识");
    }

    @Transactional(readOnly = true)
    public PageResult<MovementRow> movementPage(Filter filter) {
        Context c=prepare(filter,true);
        long count=repository.movementCount(filter,c.scope);
        List<MovementRow> rows=repository.movementPage(filter,c.scope,offset(filter),filter.getPageSize());
        rows.forEach(row->maskMovement(row,c));
        return new PageResult<>(rows,count);
    }

    @Transactional(readOnly = true)
    public Summary movementSummary(Filter filter) {
        Context c=prepare(filter,true);
        Summary result=repository.movementSummary(filter,c.scope);
        Summary coverage=repository.balanceSummary(filter,c.scope,c.cutover,true);
        applyCompleteness(result,coverage);
        maskAmounts(result,c);
        return result;
    }

    @Transactional(readOnly = true)
    public PageResult<BalanceRow> balancePage(Filter filter) {
        Context c=prepare(filter,false);
        long count=repository.balanceCount(filter,c.scope);
        List<BalanceRow> rows=repository.balancePage(filter,c.scope,c.cutover,offset(filter),filter.getPageSize());
        attachBusinessTypes(rows,filter,c);
        rows.forEach(row->maskBalance(row,c));
        return new PageResult<>(rows,count);
    }

    @Transactional(readOnly = true)
    public Summary balanceSummary(Filter filter) {
        Context c=prepare(filter,false);
        Summary result=repository.balanceSummary(filter,c.scope,c.cutover,false);
        applyCompleteness(result,result);
        if("COMPLETE".equals(result.getDataStatus())) result.setByBizType(repository.businessTypeAmounts(filter,c.scope,null,true));
        maskAmounts(result,c);
        return result;
    }

    @Transactional(readOnly = true)
    public PageResult<ProductOption> productOptions(String keyword,int pageNo,int pageSize) {
        return productOptions(keyword,null,pageNo,pageSize);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductOption> productOptions(String keyword,Long warehouseId,int pageNo,int pageSize) {
        if (pageNo<1 || pageSize<1 || pageSize>100 || (keyword!=null && keyword.length()>100))
            throw new cn.iocoder.yudao.framework.common.exception.ServiceException(400, "商品选项分页或关键字不合法");
        Scope scope=resolveScope();
        return new PageResult<>(repository.productOptions(keyword,warehouseId,scope,(long)(pageNo-1)*pageSize,pageSize),
                repository.productOptionCount(keyword,warehouseId,scope));
    }

    @Transactional(readOnly = true)
    public ReportOptions reportOptions() { return repository.reportOptions(resolveScope()); }

    /** 同一只读事务快照内分批导出，不把全表加载到Java集合。 */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void export(Filter filter,String reportType,HttpServletResponse response) throws IOException {
        boolean movement="MOVEMENT".equals(reportType);
        if (!movement && !"BALANCE".equals(reportType)) throw new cn.iocoder.yudao.framework.common.exception.ServiceException(400, "不支持的报表类型");
        if(filter!=null) { filter.setPageNo(1);filter.setPageSize(500); }
        Context c=prepare(filter,movement);
        long count=movement?repository.movementCount(filter,c.scope):repository.balanceCount(filter,c.scope);
        if (count>1_000_000) throw new cn.iocoder.yudao.framework.common.exception.ServiceException(400, "单次Excel导出最多100万行，请缩小查询范围");
        String name=movement?"库存流水-新核算.xlsx":"库存收发存-新核算.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition","attachment;filename*=UTF-8''"+URLEncoder.encode(name,"UTF-8").replace("+","%20"));
        Class<?> rowType=movement?MovementRow.class:BalanceRow.class;
        Summary coverage=repository.balanceSummary(filter,c.scope,c.cutover,movement);
        boolean incomplete=number(coverage.getMissingOpeningCount())+number(coverage.getOutsideCoverageCount())+number(coverage.getStaleCount())>0;
        try (ExcelWriter writer=FastExcelFactory.write(response.getOutputStream(),rowType).autoCloseStream(false)
                .registerConverter(new LongStringConverter()).registerConverter(new ErpReportDecimalTextConverter()).build()) {
            WriteSheet sheet=FastExcelFactory.writerSheet(0,"报表数据").build();
            WriteSheet types=movement?null:FastExcelFactory.writerSheet(2,"业务类型发生额").head(BizTypeAmount.class).build();
            if (count==0) writer.write(Collections.emptyList(),sheet);
            if(types!=null) writer.write(Collections.emptyList(),types);
            for (long offset=0;offset<count;offset+=1000) {
                if (movement) {
                    List<MovementRow> rows=repository.movementPage(filter,c.scope,offset,1000);
                    rows.forEach(row->maskMovement(row,c)); writer.write(rows,sheet);
                } else {
                    List<BalanceRow> rows=repository.balancePage(filter,c.scope,c.cutover,offset,1000);
                    attachBusinessTypes(rows,filter,c);
                    rows.forEach(row->maskBalance(row,c)); writer.write(rows,sheet);
                    List<BizTypeAmount> details=new ArrayList<>();
                    rows.forEach(row->{if(row.getByBizType()!=null) details.addAll(row.getByBizType());});
                    writer.write(details,types);
                }
            }
            WriteSheet notes=FastExcelFactory.writerSheet(1,"完整性说明")
                    .head(Arrays.asList(Collections.singletonList("项目"),Collections.singletonList("说明"))).build();
            writer.write(Arrays.asList(Arrays.asList("数据状态",incomplete?"INCOMPLETE（包含未核对或覆盖差异，不能作为完整结存）":"COMPLETE"),
                    Arrays.asList("缺失期初库存数",String.valueOf(coverage.getMissingOpeningCount())),
                    Arrays.asList("查询早于切换库存数",String.valueOf(coverage.getOutsideCoverageCount())),
                    Arrays.asList("实物核对差异库存数",String.valueOf(coverage.getStaleCount())),
                    Arrays.asList("财务成本隐藏",String.valueOf(c.financialMasked)),Arrays.asList("结算成本隐藏",String.valueOf(c.settlementMasked)),
                    Arrays.asList("金额精度","数量和金额按十进制文本保存，避免Excel数值精度损失；空值不是零")),notes);
        }
    }

    private Context prepare(Filter filter,boolean movement) {
        validateFilter(filter,movement);
        Status status=status();
        if (!"READY".equals(status.getStatus())) throw new cn.iocoder.yudao.framework.common.exception.ServiceException(409, status.getReason());
        Scope scope=resolveScope();
        if (!movement && !scope.isAll() && scope.getDeptIds().isEmpty() && scope.isSelf())
            throw new cn.iocoder.yudao.framework.common.exception.ServiceException(409, "仅本人流水权限无法查询完整库存收发存，请配置允许的库存部门范围");
        repository.requireOpeningStockReferences(scope.getTenantId());
        Context c=new Context(scope,status.getCutoverAt());
        Set<Long> departments=repository.permissionDepartments(filter,scope,movement);
        Set<String> hidden=new HashSet<>();
        List<String> global=permissionApi.getCurrentUserHiddenFields("erp_product");
        if (global!=null) hidden.addAll(global);
        for (Long dept:departments) {
            List<String> fields=permissionApi.getCurrentUserHiddenFields("erp_product",dept);
            if (fields!=null) hidden.addAll(fields);
        }
        boolean legacy=hidden(hidden,"lastPurchasePrice","costPrice","costAmount");
        c.financialMasked=legacy||hidden(hidden,"financialUnitCost","financialAmount","financialMovement","financialBalance","financialCostPrice",
                "openingFinancialAmount","financialInAmount","financialOutAmount","closingFinancialAmount");
        c.settlementMasked=legacy||hidden(hidden,"settlementUnitCost","settlementAmount","settlementMovement","settlementBalance","settlementCostPrice",
                "openingSettlementAmount","settlementInAmount","settlementOutAmount","closingSettlementAmount");
        return c;
    }

    private Scope resolveScope() {
        Long user=getLoginUserId();
        if (user==null) throw new cn.iocoder.yudao.framework.common.exception.ServiceException(409, "请先登录后查询报表");
        long tenant=TenantContextHolder.getRequiredTenantId();
        DeptDataPermissionRespDTO record=permissionApi.getDeptDataPermission(user,"erp_stock_record");
        DeptDataPermissionRespDTO stock=permissionApi.getDeptDataPermission(user,"erp_stock");
        Scope scope=new Scope().setTenantId(tenant).setUserId(user);
        if (record!=null) scope.setAll(Boolean.TRUE.equals(record.getAll())).setSelf(Boolean.TRUE.equals(record.getSelf()))
                .setDeptIds(record.getDeptIds()==null?Collections.emptySet():record.getDeptIds());
        if (stock==null) return scope;
        Set<Long> whole=repository.warehouseIds(tenant,stock.getDeptIds()==null?Collections.emptySet():stock.getDeptIds(),Boolean.TRUE.equals(stock.getAll()));
        Set<Long> own=Boolean.TRUE.equals(stock.getSelf()) && getLoginUserDeptId()!=null
                ? repository.warehouseIds(tenant,Collections.singleton(getLoginUserDeptId()),false):Collections.emptySet();
        Set<Long> all=new LinkedHashSet<>(whole); all.addAll(own);
        return scope.setWholeWarehouseIds(whole).setSelfWarehouseIds(own).setWarehouseIds(all);
    }

    static void validateFilter(Filter f,boolean movement) {
        if (f==null || f.getPostedFrom()==null || f.getPostedTo()==null || !f.getPostedFrom().isBefore(f.getPostedTo()))
            throw new cn.iocoder.yudao.framework.common.exception.ServiceException(400, "请选择有效的过账时间范围（起始含、截止不含）");
        if (f.getPageNo()==null || f.getPageNo()<1 || f.getPageSize()==null || f.getPageSize()<1 || f.getPageSize()>500)
            throw new cn.iocoder.yudao.framework.common.exception.ServiceException(400, "分页大小必须在1到500之间");
        if (!movement && (notEmpty(f.getAccountingDeptIds())||notEmpty(f.getBizTypes())||hasText(f.getBatchNo())))
            throw new cn.iocoder.yudao.framework.common.exception.ServiceException(400, "收发存不支持核算部门、业务类型或批次过滤，以免把局部发生额误当完整结存");
        Set<String> orders=movement?new HashSet<>(Arrays.asList("postedAt","postingId")):new HashSet<>(Arrays.asList("stockId","productId","warehouseId"));
        if (hasText(f.getOrderField()) && !orders.contains(f.getOrderField())) throw new cn.iocoder.yudao.framework.common.exception.ServiceException(400, "不支持的排序字段");
        if (hasText(f.getOrderDirection()) && !"asc".equalsIgnoreCase(f.getOrderDirection()) && !"desc".equalsIgnoreCase(f.getOrderDirection()))
            throw new cn.iocoder.yudao.framework.common.exception.ServiceException(400, "排序方向必须为asc或desc");
        if ((f.getStockDeptIds()!=null&&f.getStockDeptIds().size()>200)||(f.getAccountingDeptIds()!=null&&f.getAccountingDeptIds().size()>200)
                ||(f.getBizTypes()!=null&&f.getBizTypes().size()>100)||(f.getBatchNo()!=null&&f.getBatchNo().length()>128))
            throw new cn.iocoder.yudao.framework.common.exception.ServiceException(400, "查询条件数量或长度超限");
    }

    private static boolean hasText(String s) { return s!=null&&!s.trim().isEmpty(); }
    private static boolean notEmpty(Collection<?> c) { return c!=null&&!c.isEmpty(); }
    private static long offset(Filter f) { return (long)(f.getPageNo()-1)*f.getPageSize(); }
    private static boolean hidden(Set<String> hidden,String...fields) {
        for(String field:fields) if(hidden.contains(field)||hidden.contains("col_"+field)) return true;
        return false;
    }

    private void maskMovement(MovementRow row,Context c) {
        row.setBizTypeName(businessTypeName(row.getBizType()));
        boolean balanceMasked=!c.scope.isAll()&&!c.scope.getDeptIds().contains(row.getStockDeptId());
        row.setBalanceMasked(balanceMasked).setFinancialMasked(c.financialMasked).setSettlementMasked(c.settlementMasked)
                .setDataStatus(balanceMasked?"SELF_MOVEMENT_ONLY":"READY");
        if(c.financialMasked) row.setFinancialMovement(null).setFinancialBalance(null);
        if(c.settlementMasked) row.setSettlementMovement(null).setSettlementBalance(null);
        if(balanceMasked) row.setBalanceQuantity(null).setFinancialBalance(null).setSettlementBalance(null);
    }

    private void maskBalance(BalanceRow row,Context c) {
        if(!"READY".equals(row.getDataStatus())) clearAmounts(row);
        maskAmounts(row,c);
    }

    private void maskAmounts(Amounts a,Context c) {
        a.setFinancialMasked(c.financialMasked).setSettlementMasked(c.settlementMasked);
        if(c.financialMasked) a.setOpeningFinancialAmount(null).setFinancialInAmount(null).setFinancialOutAmount(null).setClosingFinancialAmount(null);
        if(c.settlementMasked) a.setOpeningSettlementAmount(null).setSettlementInAmount(null).setSettlementOutAmount(null).setClosingSettlementAmount(null);
        if(a.getByBizType()!=null) a.getByBizType().forEach(item->{
            item.setBizTypeName(businessTypeName(item.getBizType()));
            if(c.financialMasked) item.setFinancialMovement(null);
            if(c.settlementMasked) item.setSettlementMovement(null);
        });
    }

    private String businessTypeName(Integer type) {
        for(ErpStockRecordBizTypeEnum value:ErpStockRecordBizTypeEnum.values())
            if(Objects.equals(type,value.getType())) return value.getName();
        return "业务类型 "+type;
    }

    private void attachBusinessTypes(List<BalanceRow> rows,Filter filter,Context c) {
        List<Long> stocks=new ArrayList<>();rows.forEach(row->{if("READY".equals(row.getDataStatus())) stocks.add(row.getStockId());});
        Map<Long,List<BizTypeAmount>> byStock=new HashMap<>();
        if(!stocks.isEmpty()) for(BizTypeAmount amount:repository.businessTypeAmounts(filter,c.scope,stocks,false))
            byStock.computeIfAbsent(amount.getStockId(),key->new ArrayList<>()).add(amount);
        rows.forEach(row->row.setByBizType("READY".equals(row.getDataStatus())
                ?byStock.getOrDefault(row.getStockId(),Collections.emptyList()):null));
    }

    private void applyCompleteness(Summary target,Summary coverage) {
        target.setMissingOpeningCount(coverage.getMissingOpeningCount()).setOutsideCoverageCount(coverage.getOutsideCoverageCount()).setStaleCount(coverage.getStaleCount());
        boolean incomplete=number(coverage.getMissingOpeningCount())+number(coverage.getOutsideCoverageCount())+number(coverage.getStaleCount())>0;
        target.setDataStatus(incomplete?"INCOMPLETE":"COMPLETE");
        if(incomplete) clearAmounts(target);
    }
    private long number(Long value) { return value==null?0:value; }
    private void clearAmounts(Amounts a) {
        a.setOpeningQuantity(null).setInQuantity(null).setOutQuantity(null).setClosingQuantity(null)
                .setOpeningFinancialAmount(null).setFinancialInAmount(null).setFinancialOutAmount(null).setClosingFinancialAmount(null)
                .setOpeningSettlementAmount(null).setSettlementInAmount(null).setSettlementOutAmount(null).setClosingSettlementAmount(null).setByBizType(null);
    }
    private static class Context {
        final Scope scope; final LocalDateTime cutover; boolean financialMasked; boolean settlementMasked;
        Context(Scope scope,LocalDateTime cutover) { this.scope=scope;this.cutover=cutover; }
    }
}
