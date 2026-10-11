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
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class AssistantQueryService {
    @Resource private AssistantReadOnly reader;
    @Resource private PermissionApi permissions;
    @Resource private ErpWarehouseService warehouses;
    @Resource private AssistantKnowledge knowledge;
    private static final int ACCOUNT_PARTY_LIMIT = 10;
    private static final List<Map<String,String>> PRODUCT_PRICE_FIELDS = Arrays.asList(
            priceField("purchasePrice","采购价"), priceField("salePrice","销售价"), priceField("minPrice","最低价"),
            priceField("referencePrice","参考价"), priceField("retailPrice","零售价"), priceField("wholesalePrice","批发价"),
            priceField("sharePrice","股份价"), priceField("lastPurchasePrice","最后采购价"));

    public static String permission(AssistantPlan.Metric metric) {
        switch (metric) {
            case SALE: return "erp:sale-out:query";
            case PURCHASE: return "erp:purchase-in:query";
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
            case SALE: return "erp_sale_out";
            case PURCHASE: return "erp_purchase_in";
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
                || !permissions.hasAnyPermissions(uid,"erp:assistant:query"))
            throw new AssistantFailure("FORBIDDEN", "没有该指标的查询权限");
        for(String permission:requiredPermissions(metric)) if(!permissions.hasAnyPermissions(uid,permission))
            throw new AssistantFailure("FORBIDDEN", "没有该指标来源单据的完整查询权限");
        checkFields(metric,null);
    }

    static List<String> requiredPermissions(AssistantPlan.Metric metric) {
        if(metric==AssistantPlan.Metric.SALE)
            return Arrays.asList("erp:sale-out:query","erp:sale-return:query");
        if(metric==AssistantPlan.Metric.PURCHASE)
            return Arrays.asList("erp:purchase-in:query","erp:purchase-return:query");
        return Collections.singletonList(permission(metric));
    }

    private void checkFields(AssistantPlan.Metric metric,Long department) {
        List<String> modules = new ArrayList<>();
        if(metric!=AssistantPlan.Metric.SALE && metric!=AssistantPlan.Metric.PURCHASE) modules.add(module(metric));
        if(Arrays.asList(AssistantPlan.Metric.SALE,AssistantPlan.Metric.RECEIPT,AssistantPlan.Metric.RECEIVABLE).contains(metric)) modules.add("erp_customer");
        if(Arrays.asList(AssistantPlan.Metric.PURCHASE,AssistantPlan.Metric.PAYMENT,AssistantPlan.Metric.PAYABLE).contains(metric)) modules.add("erp_supplier");
        if(metric==AssistantPlan.Metric.STOCK || metric==AssistantPlan.Metric.STOCK_SKU) modules.add("erp_product");
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
        Clock queryClock=Clock.fixed(Instant.now(),ZoneOffset.UTC);
        LocalDateTime queriedAt=LocalDateTime.now(queryClock.withZone(AssistantRelativePeriod.BUSINESS_ZONE));
        LocalDateTime[] range = plan.range(queryClock);
        args.put("start",range == null ? null : range[0]);
        args.put("end",range == null ? null : range[1]);
        args.put("displayEnd",responseEnd(range,queriedAt));
        args.put("queriedAt",queriedAt);
        // Existing report adapters use an inclusive end. Subtract one microsecond for MySQL DATETIME(6).
        LocalDateTime[] inclusive = range == null ? null : new LocalDateTime[]{range[0],range[1].minusNanos(1000)};
        switch (plan.getMetric()) {
            case SALE:
                ErpSaleReportPageReqVO sale = new ErpSaleReportPageReqVO(); sale.setBizTime(inclusive);sale.setSaleUserId(plan.getSalespersonId()); args.put("reqVO",sale); break;
            case PURCHASE:
                ErpPurchaseReportPageReqVO purchase = new ErpPurchaseReportPageReqVO(); purchase.setBizTime(inclusive); args.put("reqVO",purchase); break;
            case RECEIVABLE:
                ErpReceivableAccountPageReqVO receivable=new ErpReceivableAccountPageReqVO();receivable.setSaleUserId(plan.getSalespersonId());args.put("reqVO",receivable);break;
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
            if(plan.getMetric()==AssistantPlan.Metric.SALE) {
                scope(args,"saleOut","erp_sale_out",true);
                scope(args,"saleReturn","erp_sale_return",true);
                // Compatibility keys are retained for nearby code that only inspects the context map.
                args.put("documentAll",false);args.put("documentDeptIds",Collections.emptySet());args.put("documentSelfUserId",null);
            } else if(plan.getMetric()==AssistantPlan.Metric.PURCHASE) {
                scope(args,"purchaseIn","erp_purchase_in",true);
                scope(args,"purchaseReturn","erp_purchase_return",true);
                args.put("documentAll",false);args.put("documentDeptIds",Collections.emptySet());args.put("documentSelfUserId",null);
            } else scope(args,"document",module(plan.getMetric()),false);
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
        includeZeroBalanceForSpecifiedParty(args,plan);
        resolveNames(args,plan);
        includeZeroBalanceForSpecifiedParty(args,plan);
        Map<String,Object> result = new LinkedHashMap<>();
        List<Map<String,Object>> summary = AssistantPresentation.skipsSummary(plan) ? Collections.emptyList() : run(args,"summary",101);
        if(summary.size()>100) throw new AssistantFailure("TOO_MANY_UNITS","计量单位过多，请缩小范围");
        List<Map<String,Object>> rows = plan.getGroup() == AssistantPlan.Group.NONE ? Collections.emptyList()
                : run(args,"groups",plan.getGroup() == AssistantPlan.Group.DAY ? 367 : plan.getLimit());
        List<Map<String,Object>> productRows = plan.isProductResultTable()
                ? run(args,"productResults",Math.max(10,plan.getProductIds().size())) : Collections.emptyList();
        List<Map<String,Object>> accountRows=Collections.emptyList();
        boolean accountPartyTruncated=false;
        if(isAccountMetric(plan)) {
            List<Map<String,Object>> parties=run(args,"accountParties",ACCOUNT_PARTY_LIMIT+1);
            accountPartyTruncated=parties.size()>ACCOUNT_PARTY_LIMIT;
            if(accountPartyTruncated) parties=new ArrayList<>(parties.subList(0,ACCOUNT_PARTY_LIMIT));
            if(!parties.isEmpty()) {
                List<Long> partyIds=new ArrayList<>();
                Map<Long,Integer> partyOrder=new HashMap<>();
                for(int index=0;index<parties.size();index++) {
                    Object value=parties.get(index).get("partyId");
                    if(!(value instanceof Number)) continue;
                    long partyId=((Number)value).longValue();
                    partyIds.add(partyId);partyOrder.put(partyId,index);
                }
                if(!partyIds.isEmpty()) {
                    args.put("accountPartyIds",partyIds);
                    accountRows=run(args,"accountResults",2001);
                    if(accountRows.size()>2000) throw new AssistantFailure("TOO_MANY_ACCOUNT_DEPARTMENTS","往来单位部门明细过多，请指定客户、供应商或部门后查询");
                    accountRows.sort(Comparator
                            .comparingInt((Map<String,Object> row)->partyOrder.getOrDefault(longValue(row.get("partyId")),Integer.MAX_VALUE))
                            .thenComparing((Map<String,Object> row)->decimal(row.get("amount")),Comparator.reverseOrder())
                            .thenComparing(row->String.valueOf(row.get("deptName"))));
                }
            }
        }
        if((plan.getMetric()==AssistantPlan.Metric.STOCK || plan.getMetric()==AssistantPlan.Metric.STOCK_SKU) && rows.size()>200) {
            String target=plan.getMetric()==AssistantPlan.Metric.STOCK_SKU?"SKU":"库存";
            throw new AssistantFailure("TOO_MANY_TIES",target+"排名的并列结果超过 200 条，请增加仓库、部门或库存范围条件");
        }
        if(plan.isIncludeProductPrices()) {
            ProductPriceResult prices=sanitizeProductPrices(summary,rows,productRows,plan);
            result.put("priceFields",prices.availableFields);
            result.put("priceStatus",prices.status);
            result.put("missingPriceFields",prices.missingFields);
            if(!prices.availableFields.isEmpty()) result.put("columns",productColumns(prices.availableFields));
            if(!plan.isProductResultTable() && !summary.isEmpty() && stockAmountIsZero(summary))
                result.put("stockNotice","系统有产品档案，但当前授权仓库范围内没有库存数量");
            if(prices.notice!=null) result.put("priceNotice",prices.notice);
            if(plan.isProductResultTable()) result.put("productResults",buildProductResults(productRows,prices,plan));
        }
        if(plan.isProductResultTable()) {
            result.put("productMatchTruncated",plan.isProductMatchTruncated());
            result.put("productMatchLimit",10);
        }
        if(isAccountMetric(plan)) {
            result.put("accountResults",accountRows);
            result.put("accountPartyLimit",ACCOUNT_PARTY_LIMIT);
            result.put("accountPartyTruncated",accountPartyTruncated);
            result.put("summaryLabel",plan.getMetric()==AssistantPlan.Metric.RECEIVABLE?"应收余额合计":"应付余额合计");
        }
        result.put("summary",summary);
        result.put("rows",rows); result.put("plan",plan); result.put("status",summary.isEmpty() && rows.isEmpty() && accountRows.isEmpty() ? "EMPTY" : "SUCCESS");
        result.put("start",args.get("start")); result.put("end",args.get("displayEnd"));
        result.put("queriedAt",String.valueOf(args.get("queriedAt")));
        result.put("knowledge",knowledge.get(plan.getMetric()));
        result.put("scope","当前登录用户有权限查看的数据");
        AssistantPresentation.apply(null,plan,result);
        return result;
    }

    private static boolean isAccountMetric(AssistantPlan plan) {
        return plan.getMetric()==AssistantPlan.Metric.RECEIVABLE || plan.getMetric()==AssistantPlan.Metric.PAYABLE;
    }

    private static void includeZeroBalanceForSpecifiedParty(Map<String,Object> args,AssistantPlan plan) {
        if(!isAccountMetric(plan) || plan.getParty()==null && plan.getPartyId()==null) return;
        Object request=args.get("reqVO");
        if(request instanceof ErpReceivableAccountPageReqVO)
            ((ErpReceivableAccountPageReqVO)request).setShowZeroBalance(true);
        if(request instanceof ErpPayableAccountPageReqVO)
            ((ErpPayableAccountPageReqVO)request).setShowZeroBalance(true);
    }

    private static long longValue(Object value) {
        return value instanceof Number?((Number)value).longValue():Long.MIN_VALUE;
    }

    static LocalDateTime responseEnd(LocalDateTime[] range,LocalDateTime queriedAt) {
        if(range==null) return null;
        return range[1].equals(queriedAt)?range[1]:range[1].minusNanos(1000);
    }

    private void resolveNames(Map<String,Object> args,AssistantPlan plan) {
        String[] names = {plan.getWarehouse(),plan.getParty(),plan.getProduct(),plan.getDepartment(),plan.getSalesperson()};
        String[] fields = {"warehouse","party","product","dept","salesperson"};
        Long[] ids={plan.getWarehouseId(),plan.getPartyId(),plan.getProductId(),plan.getDepartmentId(),plan.getSalespersonId()};
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
            } else if("product".equals(fields[i]) && plan.isProductResultTable()) {
                List<Map<String,Object>> options=exact.isEmpty()?candidates:exact;
                List<Long> productIds=new ArrayList<>();
                for(int index=0;index<Math.min(10,options.size());index++)
                    productIds.add(((Number)options.get(index).get("id")).longValue());
                plan.setProductIds(productIds);plan.setProductMatchTruncated(options.size()>10);
                plan.setProduct(null);plan.setProductId(productIds.size()==1?productIds.get(0):null);
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
        if("salesperson".equals(field)) return "没有找到匹配的业务员，请检查业务员名称";
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
            case "salesperson": plan.setSalespersonId(id);break;
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

    private ProductPriceResult sanitizeProductPrices(List<Map<String,Object>> summary,List<Map<String,Object>> rows,
                                                     List<Map<String,Object>> productRows,AssistantPlan plan) {
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
            if(!selected || fieldHidden) {removeKey(summary,key);removeKey(rows,key);removeKey(productRows,key);hasHidden|=fieldHidden;continue;}
            permitted.add(field);
        }
        if(permitted.isEmpty()) {
            if(implicitStockPriceQuery(plan)) return new ProductPriceResult(Collections.emptyList(),Collections.emptyList(),
                    Collections.emptyList(),"NO_PERMISSION",null,"当前账号没有可见的产品价格字段");
            throw new AssistantFailure("NO_PERMISSION",specificPriceQuery(plan)
                    ? "当前账号没有查看"+requested.get(0).get("title")+"的权限"
                    : "当前账号没有可见的产品价格字段");
        }
        List<Map<String,String>> available=new ArrayList<>();
        List<Map<String,String>> missing=new ArrayList<>();
        for(Map<String,String> field:permitted) {
            String key=field.get("key");
            boolean present=containsNonNull(summary,key) || containsNonNull(rows,key) || containsNonNull(productRows,key);
            if(present) available.add(field);
            else {missing.add(field);removeKey(summary,key);removeKey(rows,key);removeKey(productRows,key);}
        }
        String status=available.isEmpty()?"NOT_MAINTAINED":missing.isEmpty()?"AVAILABLE":"PARTIAL";
        String notice=priceNotice(plan,available,missing,hasHidden);
        return new ProductPriceResult(permitted,available,missing,status,notice,null);
    }

    private static List<Map<String,Object>> buildProductResults(List<Map<String,Object>> rows,
                                                                 ProductPriceResult priceResult,AssistantPlan plan) {
        Map<Long,ProductResultAccumulator> grouped=new LinkedHashMap<>();
        for(Map<String,Object> row:rows) {
            Object rawId=row.get("productId");if(!(rawId instanceof Number)) continue;
            long productId=((Number)rawId).longValue();
            ProductResultAccumulator item=grouped.computeIfAbsent(productId,id->new ProductResultAccumulator(id));
            if(item.name==null && row.get("productName")!=null) item.name=String.valueOf(row.get("productName"));
            if(item.unit==null && row.get("unit")!=null) item.unit=String.valueOf(row.get("unit"));
            for(Map<String,String> field:priceResult.permittedFields) {
                Object value=row.get(field.get("key"));if(value!=null) item.prices.putIfAbsent(field.get("key"),value);
            }
            Object rawWarehouseId=row.get("warehouseId"),rawWarehouseName=row.get("warehouseName");
            BigDecimal amount=decimal(row.get("amount"));
            if(rawWarehouseId instanceof Number && rawWarehouseName!=null && amount.compareTo(BigDecimal.ZERO)!=0) {
                long warehouseId=((Number)rawWarehouseId).longValue();
                WarehouseStockAccumulator stock=item.stocks.computeIfAbsent(warehouseId,
                        id->new WarehouseStockAccumulator(id,String.valueOf(rawWarehouseName)));
                stock.amount=stock.amount.add(amount);
            }
        }
        List<ProductResultAccumulator> ordered=new ArrayList<>(grouped.values());
        Map<Long,Integer> order=new HashMap<>();
        for(int index=0;index<plan.getProductIds().size();index++) order.put(plan.getProductIds().get(index),index);
        ordered.sort(Comparator.comparingInt(value->order.getOrDefault(value.id,Integer.MAX_VALUE)));
        List<Map<String,Object>> result=new ArrayList<>();
        for(ProductResultAccumulator item:ordered) {
            Map<String,Object> product=new LinkedHashMap<>();product.put("productId",item.id);
            product.put("productName",item.name);product.put("unit",item.unit);
            List<Map<String,Object>> prices=new ArrayList<>();List<Map<String,String>> missing=new ArrayList<>();
            for(Map<String,String> field:priceResult.permittedFields) {
                Object value=item.prices.get(field.get("key"));
                if(value==null) missing.add(field);
                else {Map<String,Object> price=new LinkedHashMap<>();price.put("key",field.get("key"));
                    price.put("title",field.get("title"));price.put("value",value);price.put("unit","元");prices.add(price);}
            }
            product.put("prices",prices);product.put("missingPriceFields",missing);
            if(prices.isEmpty() && priceResult.emptyMessage!=null)
                product.put("priceDisplayText",priceResult.emptyMessage);
            List<WarehouseStockAccumulator> stocks=new ArrayList<>(item.stocks.values());
            stocks.removeIf(stock->stock.amount.compareTo(BigDecimal.ZERO)==0);
            stocks.sort(Comparator.comparing((WarehouseStockAccumulator stock)->stock.amount).reversed()
                    .thenComparing(stock->stock.name));
            List<Map<String,Object>> warehouseStocks=new ArrayList<>();
            for(WarehouseStockAccumulator stock:stocks) {Map<String,Object> value=new LinkedHashMap<>();
                value.put("warehouseId",stock.id);value.put("warehouseName",stock.name);value.put("amount",stock.amount);
                warehouseStocks.add(value);}
            product.put("warehouseStocks",warehouseStocks);result.add(product);
        }
        return result;
    }

    private static BigDecimal decimal(Object value) {
        if(value==null) return BigDecimal.ZERO;
        try{return new BigDecimal(String.valueOf(value));}catch(NumberFormatException ignored){return BigDecimal.ZERO;}
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
    private static boolean implicitStockPriceQuery(AssistantPlan plan) {
        return plan.isProductResultTable() && plan.getPriceQueryMode()==AssistantPlan.PriceQueryMode.NONE;
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
        final List<Map<String,String>> permittedFields;
        final List<Map<String,String>> availableFields;
        final List<Map<String,String>> missingFields;
        final String status;
        final String notice;
        final String emptyMessage;
        ProductPriceResult(List<Map<String,String>> permittedFields,List<Map<String,String>> availableFields,
                           List<Map<String,String>> missingFields,String status,String notice,String emptyMessage) {
            this.permittedFields=permittedFields;this.availableFields=availableFields;this.missingFields=missingFields;
            this.status=status;this.notice=notice;this.emptyMessage=emptyMessage;
        }
    }
    private static final class ProductResultAccumulator {
        final long id;String name;String unit;final Map<String,Object> prices=new LinkedHashMap<>();
        final Map<Long,WarehouseStockAccumulator> stocks=new LinkedHashMap<>();
        ProductResultAccumulator(long id){this.id=id;}
    }
    private static final class WarehouseStockAccumulator {
        final long id;final String name;BigDecimal amount=BigDecimal.ZERO;
        WarehouseStockAccumulator(long id,String name){this.id=id;this.name=name;}
    }
}
