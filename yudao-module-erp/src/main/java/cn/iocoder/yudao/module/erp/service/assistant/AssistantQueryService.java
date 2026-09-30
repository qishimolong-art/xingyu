package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.dal.mysql.assistant.AssistantQueryMapper;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.*;
import java.util.*;

@Service
public class AssistantQueryService {
    @Resource private AssistantReadOnly reader;
    @Resource private PermissionApi permissions;
    @Resource private ErpWarehouseService warehouses;
    @Resource private AssistantKnowledge knowledge;
    private static final List<Map<String,String>> PRODUCT_PRICE_FIELDS = Arrays.asList(
            priceField("purchasePrice","采购价"), priceField("salePrice","销售价"), priceField("minPrice","最低价"),
            priceField("referencePrice","参考价"), priceField("retailPrice","零售价"), priceField("wholesalePrice","批发价"),
            priceField("sharePrice","股份价"), priceField("lastPurchasePrice","最后采购价"));

    public static String permission(AssistantPlan.Metric metric) {
        switch (metric) {
            case SALE: return "erp:sale-report:query";
            case PURCHASE: return "erp:purchase-report:query";
            case STOCK: case STOCK_SKU: return "erp:stock:query";
            case RECEIPT: return "erp:finance-receipt:query";
            case PAYMENT: return "erp:finance-payment:query";
            case RECEIVABLE: return "erp:receivable-account:query";
            case PAYABLE: return "erp:payable-account:query";
            default: throw new IllegalArgumentException();
        }
    }
    public static String module(AssistantPlan.Metric metric) {
        switch (metric) {
            case SALE: return "erp_sale_report";
            case PURCHASE: return "erp_purchase_report";
            case STOCK: case STOCK_SKU: return "erp_product_stock";
            case RECEIPT: return "erp_finance_receipt";
            case PAYMENT: return "erp_finance_payment";
            case RECEIVABLE: return "erp_finance_receivable_account";
            case PAYABLE: return "erp_finance_payable_account";
            default: throw new IllegalArgumentException();
        }
    }

    public void authorize(AssistantPlan.Metric metric) {
        Long uid = SecurityFrameworkUtils.getLoginUserId();
        if (uid == null || TenantContextHolder.getTenantId() == null || TenantContextHolder.isIgnore()
                || !permissions.hasAnyPermissions(uid,"erp:assistant:query") || !permissions.hasAnyPermissions(uid,permission(metric)))
            throw new AssistantFailure("FORBIDDEN", "没有该指标的查询权限");
        checkFields(metric,null);
    }

    private void checkFields(AssistantPlan.Metric metric,Long department) {
        List<String> modules = new ArrayList<>(Collections.singletonList(module(metric)));
        if(Arrays.asList(AssistantPlan.Metric.SALE,AssistantPlan.Metric.RECEIPT,AssistantPlan.Metric.RECEIVABLE).contains(metric)) modules.add("erp_customer");
        if(Arrays.asList(AssistantPlan.Metric.PURCHASE,AssistantPlan.Metric.PAYMENT,AssistantPlan.Metric.PAYABLE).contains(metric)) modules.add("erp_supplier");
        if(metric!=AssistantPlan.Metric.RECEIPT && metric!=AssistantPlan.Metric.PAYMENT) modules.add("erp_product");
        switch (metric) {
            case SALE: modules.addAll(Arrays.asList("erp_sale_out","erp_sale_return")); break;
            case PURCHASE: modules.addAll(Arrays.asList("erp_purchase_in","erp_purchase_return")); break;
            case RECEIVABLE: modules.addAll(Arrays.asList("erp_finance_receipt","erp_sale_out","erp_receivable_other")); break;
            case PAYABLE: modules.addAll(Arrays.asList("erp_finance_payment","erp_purchase_in","erp_payable_other")); break;
            default: break;
        }
        boolean stock = metric == AssistantPlan.Metric.STOCK || metric == AssistantPlan.Metric.STOCK_SKU;
        for (String mod : modules) {
            // Sales reports authorize recorded amounts with salePrice, not every possible
            // customer-level pricing input used while creating a sales document.
            // Keep explicit source-document field restrictions; salePrice is checked above
            // through erp_product for both the user and each business department.
            boolean salesReportSource=(metric==AssistantPlan.Metric.SALE || metric==AssistantPlan.Metric.RECEIVABLE)
                    && ("erp_sale_out".equals(mod) || "erp_sale_return".equals(mod));
            List<String> hidden = salesReportSource?permissions.getCurrentUserHiddenFields(mod,department,false)
                    :department==null?permissions.getCurrentUserHiddenFields(mod):permissions.getCurrentUserHiddenFields(mod,department);
            if (hidden == null) throw new AssistantFailure("FORBIDDEN", "字段权限尚未确定");
            for (String key : hidden) {
                String lower = key.toLowerCase(Locale.ROOT);
                String normalized=lower.replaceFirst("^(col_|report_|item_)","");
                boolean protectedField;
                if("erp_product".equals(mod) && !stock) {
                    boolean sale=metric==AssistantPlan.Metric.SALE || metric==AssistantPlan.Metric.RECEIVABLE;
                    protectedField=normalized.equals(sale?"saleprice":"purchaseprice") || (!sale && normalized.equals("lastpurchaseprice"));
                } else protectedField=stock ? normalized.endsWith("count") || normalized.equals("quantity")
                        : Arrays.asList("writeoffamount","writeoffprice","allocatedamount","totalprice","receiptprice","paymentprice","discountprice","saleamount","returnamount","netamount","purchaseamount","receivablebalance","balance","amount","totalamount").contains(normalized);
                protectedField |= Arrays.asList("name","deptname","customername","suppliername","productname","warehousename","unitname","no").contains(normalized);
                if (protectedField) throw new AssistantFailure("FIELD_FORBIDDEN", "该统计涉及当前不可见字段");
            }
        }
    }

    private void checkDepartmentFields(Map<String,Object> args,AssistantPlan.Metric metric) {
        List<Map<String,Object>> departments=run(args,"permissionDepartments",201);
        if(departments.size()>200) throw new AssistantFailure("SCOPE_TOO_LARGE","涉及部门较多，请选择部门后查询");
        for(Map<String,Object> row:departments) if(row.get("deptId") instanceof Number) checkFields(metric,((Number)row.get("deptId")).longValue());
    }

    private void scope(Map<String,Object> args, String prefix, String module, boolean stringSelf) {
        Long uid = SecurityFrameworkUtils.getLoginUserId();
        DeptDataPermissionRespDTO s = permissions.getDeptDataPermission(uid,module);
        if (s == null || (!Boolean.TRUE.equals(s.getAll()) && !Boolean.TRUE.equals(s.getSelf()) && (s.getDeptIds() == null || s.getDeptIds().isEmpty())))
            throw new AssistantFailure("FORBIDDEN", "没有该业务的数据访问范围");
        args.put(prefix+"All",Boolean.TRUE.equals(s.getAll()));
        args.put(prefix+"DeptIds",s.getDeptIds() == null ? Collections.emptySet() : s.getDeptIds());
        args.put(prefix+"SelfUserId",Boolean.TRUE.equals(s.getSelf()) ? (stringSelf ? String.valueOf(uid) : uid) : null);
    }

    public Map<String,Object> context(AssistantPlan plan) {
        plan.validate();
        authorize(plan.getMetric());
        knowledge.requirePublished(plan);
        Map<String,Object> args = new HashMap<>();
        args.put("plan",plan);
        args.put("tenantId",TenantContextHolder.getRequiredTenantId());
        args.put("userId",String.valueOf(SecurityFrameworkUtils.getLoginUserId()));
        LocalDateTime[] range = plan.range(Clock.systemUTC());
        args.put("start",range == null ? null : range[0]);
        args.put("end",range == null ? null : range[1]);
        // Existing report adapters use an inclusive end. Subtract one microsecond for MySQL DATETIME(6).
        LocalDateTime[] inclusive = range == null ? null : new LocalDateTime[]{range[0],range[1].minusNanos(1000)};
        switch (plan.getMetric()) {
            case SALE:
                ErpSaleReportPageReqVO sale = new ErpSaleReportPageReqVO(); sale.setBizTime(inclusive); args.put("reqVO",sale); break;
            case PURCHASE:
                ErpPurchaseReportPageReqVO purchase = new ErpPurchaseReportPageReqVO(); purchase.setBizTime(inclusive); args.put("reqVO",purchase); break;
            case RECEIVABLE: args.put("reqVO",new ErpReceivableAccountPageReqVO()); break;
            case PAYABLE: args.put("reqVO",new ErpPayableAccountPageReqVO()); break;
            default: break;
        }
        if (plan.getMetric() == AssistantPlan.Metric.STOCK || plan.getMetric() == AssistantPlan.Metric.STOCK_SKU) {
            ErpProductStockPermissionScope s = warehouses.getCurrentUserProductStockPermissionScope();
            if(s == null || (!s.isAll() && s.getVisibleWarehouseIds().isEmpty())) throw new AssistantFailure("FORBIDDEN","没有可见仓库");
            args.put("stockAll",s.isAll()); args.put("stockDeptIds",s.getDepartmentWarehouseIds()); args.put("stockSelfIds",s.getSelfWarehouseIds());
        } else {
            boolean customer = Arrays.asList(AssistantPlan.Metric.SALE,AssistantPlan.Metric.RECEIPT,AssistantPlan.Metric.RECEIVABLE).contains(plan.getMetric());
            scope(args,"party",customer ? "erp_customer" : "erp_supplier",true);
            scope(args,"document",module(plan.getMetric()),plan.getMetric() == AssistantPlan.Metric.SALE || plan.getMetric() == AssistantPlan.Metric.PURCHASE);
            // Original account mappers use different parameter names; both derive from the same checked scopes.
            args.put("deptIds",args.get("partyDeptIds")); args.put("selfUserId",args.get("partySelfUserId")); args.put("all",args.get("partyAll"));
            args.put("docDeptIds",args.get("documentDeptIds")); args.put("docSelfUserId",args.get("documentSelfUserId")); args.put("docAll",args.get("documentAll"));
        }
        return args;
    }

    private List<Map<String,Object>> run(Map<String,Object> args, String mode, int limit) {
        args.put("mode",mode); args.put("rowLimit",limit);
        // Source SQL includes explicit form-specific scopes. Keep tenant interceptor enabled.
        return DataPermissionUtils.executeIgnore(() -> reader.select(args));
    }

    public Map<String,Object> execute(AssistantPlan plan) {
        return reader.snapshot(() -> executeSnapshot(plan));
    }
    private Map<String,Object> executeSnapshot(AssistantPlan plan) {
        Map<String,Object> args = context(plan);
        checkDepartmentFields(args,plan.getMetric());
        resolveNames(args,plan);
        Map<String,Object> result = new LinkedHashMap<>();
        List<Map<String,Object>> summary = AssistantPresentation.skipsSummary(plan) ? Collections.emptyList() : run(args,"summary",101);
        if(summary.size()>100) throw new AssistantFailure("TOO_MANY_UNITS","计量单位过多，请缩小范围");
        List<Map<String,Object>> rows = plan.getGroup() == AssistantPlan.Group.NONE ? Collections.emptyList()
                : run(args,"groups",plan.getGroup() == AssistantPlan.Group.DAY ? 367 : plan.getLimit());
        if((plan.getMetric()==AssistantPlan.Metric.STOCK || plan.getMetric()==AssistantPlan.Metric.STOCK_SKU) && rows.size()>200) {
            String target=plan.getMetric()==AssistantPlan.Metric.STOCK_SKU?"SKU":"库存";
            throw new AssistantFailure("TOO_MANY_TIES",target+"排名的并列结果超过 200 条，请增加仓库、部门或库存范围条件");
        }
        if(plan.isIncludeProductPrices()) {
            ProductPriceResult prices=sanitizeProductPrices(summary,rows,plan);
            result.put("priceFields",prices.availableFields);
            result.put("priceStatus",prices.status);
            result.put("missingPriceFields",prices.missingFields);
            if(!prices.availableFields.isEmpty()) result.put("columns",productColumns(prices.availableFields));
            if(!summary.isEmpty() && stockAmountIsZero(summary))
                result.put("stockNotice","系统有产品档案，但当前授权仓库范围内没有库存数量");
            if(prices.notice!=null) result.put("priceNotice",prices.notice);
        }
        result.put("summary",summary);
        result.put("rows",rows); result.put("plan",plan); result.put("status",summary.isEmpty() && rows.isEmpty() ? "EMPTY" : "SUCCESS");
        result.put("start",args.get("start")); result.put("end",args.get("end"));
        result.put("queriedAt",LocalDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
        result.put("knowledge",knowledge.get(plan.getMetric()));
        result.put("scope","当前登录用户有权限查看的数据");
        AssistantPresentation.apply(null,plan,result);
        return result;
    }

    private void resolveNames(Map<String,Object> args,AssistantPlan plan) {
        String[] names = {plan.getWarehouse(),plan.getParty(),plan.getProduct(),plan.getDepartment()};
        String[] fields = {"warehouse","party","product","dept"};
        Long[] ids={plan.getWarehouseId(),plan.getPartyId(),plan.getProductId(),plan.getDepartmentId()};
        for(int i=0;i<fields.length;i++) {
            if(names[i]==null) continue;
            args.put("candidateField",fields[i]); args.put("candidateName",names[i]);
            List<Map<String,Object>> candidates = run(args,"candidates",21);
            List<Map<String,Object>> exact = new ArrayList<>();
            for(Map<String,Object> c:candidates) if(names[i].equals(c.get("name"))) exact.add(c);
            if(ids[i]!=null) {
                boolean found=false;
                for(Map<String,Object> c:candidates) if(((Number)c.get("id")).longValue()==ids[i]) found=true;
                if(!found) throw new AssistantFailure("FORBIDDEN","该候选对象已不可访问，请重新查询");
            } else if(exact.size()==1) {
                assignChoice(plan,fields[i],((Number)exact.get(0).get("id")).longValue());
            } else if(candidates.isEmpty()) {
                throw new AssistantFailure("ENTITY_NOT_FOUND",entityNotFoundText(fields[i],plan.getMetric()));
            } else if(candidates.size()>20) {
                throw new AssistantFailure("CLARIFY_ENTITY","匹配对象较多，请填写更完整的名称");
            } else {
                List<Map<String,Object>> options=exact.isEmpty()?candidates:exact;
                plan.setPendingField(fields[i]);plan.setPendingIds(new ArrayList<>());
                for(Map<String,Object> c:options) plan.getPendingIds().add(((Number)c.get("id")).longValue());
                throw new AssistantEntityChoice(fields[i],options);
            }
        }
    }

    private static String entityNotFoundText(String field,AssistantPlan.Metric metric) {
        if("product".equals(field)) return "没有找到匹配的产品，请检查产品名称、编码或规格";
        if("warehouse".equals(field)) return "没有找到匹配的仓库，请检查仓库名称或编码";
        if("dept".equals(field)) return "没有找到匹配的部门，请检查部门名称";
        if("party".equals(field)) {
            boolean supplier=Arrays.asList(AssistantPlan.Metric.PURCHASE,AssistantPlan.Metric.PAYMENT,
                    AssistantPlan.Metric.PAYABLE).contains(metric);
            return supplier?"没有找到匹配的供应商，请检查供应商名称或编码"
                    :"没有找到匹配的客户，请检查客户名称或编码";
        }
        return "没有找到匹配的业务对象，请检查名称或编码";
    }

    /** Recheck scopes and department fields before returning persisted question text. */
    public void validateAccess(AssistantPlan plan) {
        reader.snapshot(() -> {Map<String,Object> args=context(plan);checkDepartmentFields(args,plan.getMetric());if(plan.getPendingField()==null) resolveNames(args,plan);return true;});
    }

    public static void assignChoice(AssistantPlan plan,String field,Long id) {
        if(id==null || id<=0) throw new IllegalArgumentException("候选编号无效");
        switch(field) {
            case "warehouse": plan.setWarehouseId(id);break;
            case "party": plan.setPartyId(id);break;
            case "product": plan.setProductId(id);break;
            case "dept": plan.setDepartmentId(id);break;
            default: throw new IllegalArgumentException("候选类型无效");
        }
    }

    public Map<String,Object> details(AssistantPlan plan,int page,int pageSize) {
        return reader.snapshot(() -> detailsSnapshot(plan,page,pageSize));
    }
    private Map<String,Object> detailsSnapshot(AssistantPlan plan,int page,int pageSize) {
        if(page<1 || page>10000 || pageSize<1 || pageSize>100) throw new IllegalArgumentException("分页参数无效");
        Map<String,Object> args=context(plan);checkDepartmentFields(args,plan.getMetric()); resolveNames(args,plan);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("total",run(args,"count",1).get(0).get("total"));
        args.put("offset",(page-1)*pageSize); args.put("pageSize",pageSize);
        result.put("list",run(args,"details",pageSize)); return result;
    }

    private ProductPriceResult sanitizeProductPrices(List<Map<String,Object>> summary,List<Map<String,Object>> rows,AssistantPlan plan) {
        List<String> hiddenFields=permissions.getCurrentUserHiddenFields("erp_product");
        if(hiddenFields==null) throw new AssistantFailure("FORBIDDEN","字段权限尚未确定");
        Set<String> hidden=normalizeHidden(hiddenFields);
        List<Map<String,String>> requested=requestedPriceFields(plan);
        List<Map<String,String>> permitted=new ArrayList<>();
        boolean hasHidden=false;
        for(Map<String,String> field:PRODUCT_PRICE_FIELDS) {
            String key=field.get("key");
            boolean selected=containsPriceField(requested,key);
            boolean fieldHidden=selected && hidden.contains(key.toLowerCase(Locale.ROOT));
            if(!selected || fieldHidden) {removeKey(summary,key);removeKey(rows,key);hasHidden|=fieldHidden;continue;}
            permitted.add(field);
        }
        if(permitted.isEmpty()) throw new AssistantFailure("NO_PERMISSION",specificPriceQuery(plan)
                ? "当前账号没有查看"+requested.get(0).get("title")+"的权限"
                : "当前账号没有可见的产品价格字段");
        List<Map<String,String>> available=new ArrayList<>();
        List<Map<String,String>> missing=new ArrayList<>();
        for(Map<String,String> field:permitted) {
            String key=field.get("key");
            boolean present=containsNonNull(summary,key) || containsNonNull(rows,key);
            if(present) available.add(field);
            else {missing.add(field);removeKey(summary,key);removeKey(rows,key);}
        }
        String status=available.isEmpty()?"NOT_MAINTAINED":missing.isEmpty()?"AVAILABLE":"PARTIAL";
        String notice=priceNotice(plan,available,missing,hasHidden);
        return new ProductPriceResult(available,missing,status,notice);
    }
    private static Map<String,String> priceField(String key,String title) {Map<String,String> row=new LinkedHashMap<>();row.put("key",key);row.put("title",title);return row;}
    private static List<Map<String,String>> requestedPriceFields(AssistantPlan plan) {
        if(!specificPriceQuery(plan)) return PRODUCT_PRICE_FIELDS;
        for(Map<String,String> field:PRODUCT_PRICE_FIELDS) if(field.get("key").equals(plan.getPriceField()))
            return Collections.singletonList(field);
        throw new AssistantFailure("MODEL_INVALID","不支持的产品价格类型");
    }
    private static boolean specificPriceQuery(AssistantPlan plan) {
        return plan.getPriceQueryMode()==AssistantPlan.PriceQueryMode.SPECIFIC;
    }
    private static boolean containsPriceField(List<Map<String,String>> fields,String key) {
        for(Map<String,String> field:fields) if(key.equals(field.get("key"))) return true;
        return false;
    }
    private static String priceNotice(AssistantPlan plan,List<Map<String,String>> available,List<Map<String,String>> missing,boolean hasHidden) {
        String notice=null;
        if(!missing.isEmpty()) {
            if(specificPriceQuery(plan)) {
                Map<String,String> field=missing.get(0);
                notice="lastPurchasePrice".equals(field.get("key"))
                        ? "已找到该产品，但暂无最后采购价，尚未形成采购入库价格"
                        : "已找到该产品，但尚未维护"+field.get("title");
            } else if(available.isEmpty()) notice="已找到该产品，但尚未完善价格信息";
            else notice="以下价格暂无数据："+joinPriceTitles(missing);
        }
        if(hasHidden) notice=notice==null?"另有部分价格字段当前账号不可见":notice+"；另有部分价格字段当前账号不可见";
        return notice;
    }
    private static String joinPriceTitles(List<Map<String,String>> fields) {
        List<String> titles=new ArrayList<>();for(Map<String,String> field:fields) titles.add(field.get("title"));
        return String.join("、",titles);
    }
    private static Set<String> normalizeHidden(List<String> fields) {
        Set<String> result=new HashSet<>();if(fields==null)return result;
        for(String field:fields) if(field!=null) result.add(field.toLowerCase(Locale.ROOT).replaceFirst("^(col_|report_|item_)",""));
        return result;
    }
    private static void removeKey(List<Map<String,Object>> rows,String key) {for(Map<String,Object> row:rows) row.remove(key);}
    private static boolean containsNonNull(List<Map<String,Object>> rows,String key) {for(Map<String,Object> row:rows) if(row.get(key)!=null) return true;return false;}
    private static List<Map<String,String>> productColumns(List<Map<String,String>> prices) {
        List<Map<String,String>> columns=new ArrayList<>();
        columns.add(column("label","产品/分组"));columns.add(column("amount","库存数量"));columns.add(column("unit","单位"));
        for(Map<String,String> price:prices) columns.add(column(price.get("key"),price.get("title")));
        return columns;
    }
    private static Map<String,String> column(String key,String title) {Map<String,String> row=new LinkedHashMap<>();row.put("key",key);row.put("title",title);return row;}
    private static boolean stockAmountIsZero(List<Map<String,Object>> rows) {
        for(Map<String,Object> row:rows) {
            Object value=row.get("amount");
            if(value instanceof Number && Math.abs(((Number)value).doubleValue())>0.000001D) return false;
        }
        return true;
    }
    private static final class ProductPriceResult {
        final List<Map<String,String>> availableFields;
        final List<Map<String,String>> missingFields;
        final String status;
        final String notice;
        ProductPriceResult(List<Map<String,String>> availableFields,List<Map<String,String>> missingFields,String status,String notice) {
            this.availableFields=availableFields;this.missingFields=missingFields;this.status=status;this.notice=notice;
        }
    }
}
