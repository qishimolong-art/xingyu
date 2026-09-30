package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope;
import cn.iocoder.yudao.module.erp.dal.mysql.assistant.AssistantEntitySql;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves user text to an authorized ERP master-data id before a business query is executed. */
@Service
public class AssistantEntityResolver {

    public enum EntityType { PRODUCT, CUSTOMER, SUPPLIER, WAREHOUSE, DEPARTMENT, SALESPERSON }

    private static final Pattern WRAPPED_KEYWORD = Pattern.compile(
            "^(?:(?:产品|配件|客户|供应商|仓库|部门|业务员|销售员)\\s*)?(?:厂家编码|旧编码|名称|名字|编码|编号|代码|条码|型号)\\s*(?:为|是|[:：])?\\s*(.+)$",
            Pattern.CASE_INSENSITIVE);

    @Resource private AssistantProperties properties;
    @Resource private AssistantSemanticReadOnly reader;
    @Resource private PermissionApi permissions;
    @Resource private DeptApi departments;
    @Resource private ErpWarehouseService warehouses;

    public boolean enabled() {
        return properties.getEntitySearch().isEnabled();
    }

    public List<String> availableTypes() {
        if(!enabled()) return Collections.emptyList();
        List<String> result=new ArrayList<>();
        for(EntityType type:EntityType.values()) {
            if(!canSearch(type)) continue;
            try {
                Map<String,Object> args=baseContext();
                prepareScope(args,type,null);
                applyVisibleFields(args,type);
                result.add(type.name());
            } catch (AssistantFailure ignored) {
                // Only advertise entity types whose permission and field scopes are usable now.
            }
        }
        return result;
    }

    public List<Map<String, Object>> resolveForPlan(Map<String, Object> base, AssistantPlan plan,
                                                     String field, String rawKeyword, Long selectedId) {
        EntityType type = typeForPlan(plan, field);
        Map<String, Object> args = new HashMap<>(base);
        prepareScope(args, type, plan.getMetric());
        return resolve(args, type, rawKeyword, selectedId);
    }

    /** Resolves every textual plan filter to a verified id before the legacy metric engine runs. */
    public void preparePlan(AssistantPlan plan) {
        if (!enabled() || plan == null) return;
        String[] fields={"warehouse","party","product","dept"};
        String[] names={plan.getWarehouse(),plan.getParty(),plan.getProduct(),plan.getDepartment()};
        Long[] ids={plan.getWarehouseId(),plan.getPartyId(),plan.getProductId(),plan.getDepartmentId()};
        for(int i=0;i<fields.length;i++) {
            if(names[i]==null) continue;
            List<Map<String,Object>> candidates=resolveForPlan(Collections.emptyMap(),plan,fields[i],names[i],ids[i]);
            if(ids[i]!=null) {
                if(candidates.size()!=1 || !Objects.equals(((Number)candidates.get(0).get("id")).longValue(),ids[i]))
                    throw new AssistantFailure("FORBIDDEN","该候选对象已不可访问，请重新查询");
                clearName(plan,fields[i]);
                continue;
            }
            if(candidates.isEmpty()) throw new AssistantFailure("ENTITY_NOT_FOUND","指定范围内未找到有权限的业务对象");
            if(candidates.size()>maxCandidates())
                throw new AssistantFailure("CLARIFY_ENTITY","匹配对象较多，请填写更完整的名称或完整编码");
            if(candidates.size()==1) {
                AssistantQueryService.assignChoice(plan,fields[i],((Number)candidates.get(0).get("id")).longValue());
                clearName(plan,fields[i]);
                continue;
            }
            plan.setPendingField(fields[i]);plan.setPendingIds(new ArrayList<>());
            for(Map<String,Object> candidate:candidates) plan.getPendingIds().add(((Number)candidate.get("id")).longValue());
            throw new AssistantEntityChoice(fields[i],candidates);
        }
    }

    private static void clearName(AssistantPlan plan,String field) {
        switch(field) {
            case "warehouse": plan.setWarehouse(null);break;
            case "party": plan.setParty(null);break;
            case "product": plan.setProduct(null);break;
            case "dept": plan.setDepartment(null);break;
            default: throw new IllegalArgumentException("Unsupported plan entity field");
        }
    }

    /** Searches every currently usable object type for a question containing only an identifier. */
    public List<Map<String, Object>> resolveAcrossTypes(String rawKeyword) {
        String keyword = normalizeKeyword(rawKeyword);
        if (keyword == null) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            if (!canSearch(type)) continue;
            Map<String, Object> args = baseContext();
            try {
                prepareScope(args, type, null);
                result.addAll(resolve(args, type, keyword, null));
            } catch (AssistantFailure failure) {
                // An inaccessible entity type must be indistinguishable from no match. Connection and query
                // failures must still surface instead of being reported as an absent business object.
                if(!Arrays.asList("FORBIDDEN","FIELD_FORBIDDEN").contains(failure.getCode())) throw failure;
            }
            if (result.size() > maxCandidates()) break;
        }
        return deduplicate(result, maxCandidates() + 1);
    }

    public List<Map<String, Object>> resolveType(EntityType type,String rawKeyword) {
        Map<String,Object> args=baseContext();prepareScope(args,type,null);
        return resolve(args,type,rawKeyword,null);
    }

    public Map<String,Object> validate(EntityType type,Long id) {
        if(id==null || id<=0) throw new AssistantFailure("INVALID_CHOICE","候选对象无效");
        Map<String,Object> args=baseContext();prepareScope(args,type,null);
        List<Map<String,Object>> matches=resolve(args,type,null,id);
        if(matches.size()!=1) throw new AssistantFailure("FORBIDDEN","该候选对象已不可访问，请重新查询");
        return matches.get(0);
    }

    /** Returns an explicitly labelled code, for example "客户编码 KH001". */
    public static Map<String,Object> explicitReference(String question) {
        if(question==null) return null;
        String[][] definitions={{"PRODUCT","(?:产品|配件)","(?:编码|编号|代码|条码|厂家编码|型号)"},
                {"CUSTOMER","客户","(?:编码|编号|代码)"},{"SUPPLIER","供应商","(?:编码|编号|代码|旧编码)"},
                {"WAREHOUSE","仓库","(?:编码|编号|代码)"}};
        String token="([A-Za-z0-9][A-Za-z0-9_./\\-]{0,79})";
        for(String[] definition:definitions) {
            Pattern forward=Pattern.compile(definition[1]+"(?:的)?"+definition[2]+"\\s*(?:为|是|[:：])?\\s*[\\\"'“”‘’]?"+token,Pattern.CASE_INSENSITIVE);
            Matcher match=forward.matcher(question);
            if(!match.find()) {
                Pattern reverse=Pattern.compile(definition[2]+"\\s*(?:为|是|[:：])?\\s*[\\\"'“”‘’]?"+token+"[\\\"'“”‘’]?(?:的)?"+definition[1],Pattern.CASE_INSENSITIVE);
                match=reverse.matcher(question);if(!match.find()) continue;
            }
            Map<String,Object> result=new LinkedHashMap<>();result.put("entityType",definition[0]);result.put("keyword",match.group(1));return result;
        }
        return null;
    }

    public static String normalizeKeyword(String raw) {
        if (raw == null) return null;
        String value = java.text.Normalizer.normalize(raw,java.text.Normalizer.Form.NFKC).trim()
                .replace('／','/').replace('－','-')
                .replaceAll("^[\\s\\\"'“”‘’《》【】]+|[\\s\\\"'“”‘’《》【】]+$", "");
        value = value.replaceAll("^[，,。；;：:]+|[，,。；;：:]+$", "").trim();
        Matcher wrapper = WRAPPED_KEYWORD.matcher(value);
        if (wrapper.matches()) value = wrapper.group(1).trim();
        value = value.replaceAll("^(?:为|是|[:：])\\s*", "").trim();
        return value.isEmpty() || value.length() > 80 ? null : value;
    }

    private List<Map<String, Object>> resolve(Map<String, Object> args, EntityType type,
                                               String rawKeyword, Long selectedId) {
        String keyword = normalizeKeyword(rawKeyword);
        if (selectedId == null && keyword == null) return Collections.emptyList();
        if(type==EntityType.DEPARTMENT) return resolveDepartment(args,keyword,selectedId);
        args.put("mode", "entityCandidates");
        args.put("entityType", type.name());
        args.put("entityKeyword", keyword);
        args.put("entityId", selectedId);
        args.put("rowLimit", maxCandidates() + 1);
        applyVisibleFields(args, type);
        if (selectedId != null) {
            args.put("entityMatchMode", "ID");
            return decorate(select(args), type, "ID");
        }
        List<String> variants=type==EntityType.PRODUCT?AssistantSemanticKnowledge.productKeywordVariants(keyword):Collections.singletonList(keyword);
        for (String matchMode : matchModes(type, args, keyword)) {
            List<Map<String,Object>> combined=new ArrayList<>();
            List<String> candidates=Arrays.asList("EXACT_NAME","FUZZY").contains(matchMode)?variants:Collections.singletonList(keyword);
            if("TOKEN_FUZZY".equals(matchMode)) candidates=variants;
            for(String candidate:candidates) {
                args.put("entityKeyword",candidate);args.put("entityMatchMode", matchMode);
                List<Map<String,Object>> rows=decorate(select(args), type, matchMode);
                if("TOKEN_FUZZY".equals(matchMode)) {
                    List<String> tokens=ErpKeywordQuery.parseKeywordSearch(candidate).tokens();
                    for(Map<String,Object> row:rows) row.put("matchTokens",new ArrayList<>(tokens));
                }
                combined.addAll(rows);
                combined=deduplicate(combined,maxCandidates()+1);
                if(combined.size()>maxCandidates()) break;
            }
            if (!combined.isEmpty()) return combined;
        }
        return Collections.emptyList();
    }

    /** Department is permission master data, so keep its lookup inside DeptApi and its paged query. */
    List<Map<String,Object>> resolveDepartment(Map<String,Object> args,String keyword,Long selectedId) {
        if(selectedId!=null) {
            DeptRespDTO department=departments.getDept(selectedId);
            if(!usableDepartment(args,department)) return Collections.emptyList();
            return decorate(Collections.singletonList(departmentRow(department)),EntityType.DEPARTMENT,"ID");
        }
        List<DeptRespDTO> exact=departments.getDeptListByName(keyword);
        List<Map<String,Object>> exactRows=new ArrayList<>();
        if(exact!=null) for(DeptRespDTO department:exact)
            if(usableDepartment(args,department)) exactRows.add(departmentRow(department));
        if(!exactRows.isEmpty()) return decorate(exactRows,EntityType.DEPARTMENT,"NAME");

        Collection<Long> scopeIds=departmentScopeIds(args);
        if(scopeIds!=null && scopeIds.isEmpty()) return Collections.emptyList();
        PageParam page=new PageParam();page.setPageNo(1);page.setPageSize(maxCandidates()+1);
        List<Map<String,Object>> rows=new ArrayList<>();
        List<DeptRespDTO> pageRows=departments.getDeptSimplePage(CommonStatusEnum.ENABLE.getStatus(),keyword,scopeIds,page).getList();
        if(pageRows==null) return Collections.emptyList();
        for(DeptRespDTO department:pageRows)
            rows.add(departmentRow(department));
        return decorate(rows,EntityType.DEPARTMENT,"FUZZY_NAME");
    }

    private static boolean usableDepartment(Map<String,Object> args,DeptRespDTO department) {
        if(department==null || !CommonStatusEnum.isEnable(department.getStatus())) return false;
        Collection<Long> scopeIds=departmentScopeIds(args);
        return scopeIds==null || scopeIds.contains(department.getId());
    }

    /** Null means all departments; an empty collection means no visible department. */
    private static Collection<Long> departmentScopeIds(Map<String,Object> args) {
        if(Boolean.TRUE.equals(args.get("entityAll"))) return null;
        Set<Long> ids=new LinkedHashSet<>(safeLongs(args.get("entityDeptIds")));
        Object self=args.get("entitySelfDeptId");if(self instanceof Number) ids.add(((Number)self).longValue());
        return ids;
    }

    private static Map<String,Object> departmentRow(DeptRespDTO department) {
        Map<String,Object> row=new LinkedHashMap<>();row.put("id",department.getId());row.put("name",department.getName());
        row.put("code",null);row.put("description",department.getName());return row;
    }

    private List<Map<String, Object>> select(Map<String, Object> args) {
        AssistantEntitySql.Query query=AssistantEntitySql.select(args);
        return DataPermissionUtils.executeIgnore(() -> reader.query(query.getSql(),query.getParameters(),query.getMaximumRows()));
    }

    private void prepareScope(Map<String, Object> args, EntityType type, AssistantPlan.Metric metric) {
        Long userId = requiredUser();
        args.put("tenantId", TenantContextHolder.getRequiredTenantId());
        args.put("userId", String.valueOf(userId));
        args.put("entityDeptIds", Collections.emptySet());
        args.put("entityWarehouseIds", Collections.emptySet());
        args.put("entitySelfUserId", null);
        args.put("entitySelfDeptId", loginDeptId());
        switch (type) {
            case PRODUCT: {
                requireAny("erp:stock:query", "erp:sale-report:query", "erp:purchase-report:query", "erp:sale-order:query");
                DeptDataPermissionRespDTO scope = permissions.getDeptDataPermission(userId, "erp_product");
                ErpProductStockPermissionScope stock = warehouses.getCurrentUserProductStockPermissionScope();
                boolean all=scope!=null && Boolean.TRUE.equals(scope.getAll());
                Collection<Long> deptIds=scope==null?Collections.emptySet():safe(scope.getDeptIds());
                Collection<Long> warehouseIds=stock==null?Collections.emptySet():stock.getVisibleWarehouseIds();
                boolean self=scope!=null && Boolean.TRUE.equals(scope.getSelf());
                if(!all && deptIds.isEmpty() && warehouseIds.isEmpty() && !self)
                    throw new AssistantFailure("FORBIDDEN","没有可见产品范围");
                args.put("entityAll", all);
                args.put("entityDeptIds", deptIds);
                args.put("entityWarehouseIds", warehouseIds);
                args.put("entitySelfUserId", self ? String.valueOf(userId) : null);
                break;
            }
            case CUSTOMER:
                requireAny("erp:sale-report:query", "erp:finance-receipt:query", "erp:receivable-account:query", "erp:sale-order:query");
                applyDeptScope(args, userId, "erp_customer");
                break;
            case SUPPLIER:
                requireAny("erp:purchase-report:query", "erp:finance-payment:query", "erp:payable-account:query");
                applyDeptScope(args, userId, "erp_supplier");
                break;
            case WAREHOUSE: {
                requireAny("erp:stock:query");
                ErpProductStockPermissionScope scope = warehouses.getCurrentUserProductStockPermissionScope();
                if (scope == null || (!scope.isAll() && scope.getVisibleWarehouseIds().isEmpty()))
                    throw new AssistantFailure("FORBIDDEN", "没有可见仓库");
                args.put("entityAll", scope.isAll());
                args.put("entityWarehouseIds", scope.getVisibleWarehouseIds());
                break;
            }
            case DEPARTMENT:
                applyDepartmentScope(args, userId, metric);
                break;
            case SALESPERSON:
                requireAny("erp:sale-report:query", "erp:receivable-account:query", "erp:sale-order:query");
                applyAnyDeptScope(args,userId,"erp_sale_report","erp_sale_order");
                break;
            default: throw new IllegalArgumentException("Unsupported entity type");
        }
    }

    private void applyDeptScope(Map<String, Object> args, Long userId, String module) {
        DeptDataPermissionRespDTO scope = permissions.getDeptDataPermission(userId, module);
        if (scope == null || (!Boolean.TRUE.equals(scope.getAll()) && !Boolean.TRUE.equals(scope.getSelf())
                && safe(scope.getDeptIds()).isEmpty())) throw new AssistantFailure("FORBIDDEN", "没有该对象的数据访问范围");
        args.put("entityAll", Boolean.TRUE.equals(scope.getAll()));
        args.put("entityDeptIds", safe(scope.getDeptIds()));
        args.put("entitySelfUserId", Boolean.TRUE.equals(scope.getSelf()) ? String.valueOf(userId) : null);
    }

    private void applyAnyDeptScope(Map<String,Object> args,Long userId,String... modules) {
        Set<Long> deptIds=new LinkedHashSet<>();boolean all=false,self=false;boolean found=false;
        for(String module:modules) {
            DeptDataPermissionRespDTO scope=permissions.getDeptDataPermission(userId,module);
            if(scope==null) continue;
            found=true;all|=Boolean.TRUE.equals(scope.getAll());self|=Boolean.TRUE.equals(scope.getSelf());
            deptIds.addAll(safe(scope.getDeptIds()));
        }
        if(!found || !all && !self && deptIds.isEmpty()) throw new AssistantFailure("FORBIDDEN","没有该对象的数据访问范围");
        args.put("entityAll",all);args.put("entityDeptIds",deptIds);
        args.put("entitySelfUserId",self?String.valueOf(userId):null);
    }

    private void applyDepartmentScope(Map<String, Object> args, Long userId, AssistantPlan.Metric metric) {
        List<String> modules = metric == null
                ? Arrays.asList("erp_product_stock", "erp_sale_report", "erp_sale_order", "erp_purchase_report", "erp_finance_receipt", "erp_finance_payment")
                : Collections.singletonList(AssistantQueryService.module(metric));
        Set<Long> deptIds = new LinkedHashSet<>(); boolean all = false; boolean self = false;
        for (String module : modules) {
            DeptDataPermissionRespDTO scope = permissions.getDeptDataPermission(userId, module);
            if (scope == null) continue;
            all |= Boolean.TRUE.equals(scope.getAll()); self |= Boolean.TRUE.equals(scope.getSelf());
            deptIds.addAll(safe(scope.getDeptIds()));
        }
        Long selfDeptId = loginDeptId();
        if (!all && deptIds.isEmpty() && (!self || selfDeptId == null))
            throw new AssistantFailure("FORBIDDEN", "没有可见部门");
        args.put("entityAll", all); args.put("entityDeptIds", deptIds);
        args.put("entitySelfDeptId", self ? selfDeptId : null);
    }

    private void applyVisibleFields(Map<String, Object> args, EntityType type) {
        String module = module(type);
        Set<String> blocked = new HashSet<>();
        if (type != EntityType.DEPARTMENT) {
            addHiddenFields(blocked, permissions.getCurrentUserHiddenFields(module));
            for (Long deptId : safeLongs(args.get("entityDeptIds"))) {
                addHiddenFields(blocked, permissions.getCurrentUserHiddenFields(module, deptId));
            }
            Object selfDeptId=args.get("entitySelfDeptId");
            if(selfDeptId instanceof Number && !safeLongs(args.get("entityDeptIds")).contains(((Number)selfDeptId).longValue())) {
                addHiddenFields(blocked,permissions.getCurrentUserHiddenFields(module,((Number)selfDeptId).longValue()));
            }
        }
        boolean name = !blocked.contains("name") && !blocked.contains("nickname");
        if (!name) throw new AssistantFailure("FIELD_FORBIDDEN", "对象名称字段不可见");
        args.put("searchCode", type!=EntityType.SALESPERSON && !blocked.contains(type == EntityType.WAREHOUSE ? "warehousecode" : "code"));
        args.put("searchFactoryCode", type == EntityType.PRODUCT && !blocked.contains("factorycode"));
        args.put("searchBarCode", type == EntityType.PRODUCT && !blocked.contains("barcode"));
        args.put("searchVehicleModel", type == EntityType.PRODUCT && !blocked.contains("vehiclemodel"));
        args.put("searchStandard", type == EntityType.PRODUCT && !blocked.contains("standard"));
        args.put("searchRemark", type == EntityType.PRODUCT && !blocked.contains("remark"));
        args.put("searchBrand", type == EntityType.PRODUCT && !blocked.contains("brand"));
        args.put("searchOeNumber", type == EntityType.PRODUCT && !blocked.contains("oenumber"));
        args.put("searchOriginPlace", type == EntityType.PRODUCT && !blocked.contains("originplace"));
        args.put("searchFeatureCode", type == EntityType.PRODUCT && !blocked.contains("featurecode"));
        args.put("searchDrawingNo", type == EntityType.PRODUCT && !blocked.contains("drawingno"));
        args.put("searchShelf", type == EntityType.PRODUCT && !blocked.contains("shelf"));
        args.put("searchOldCode", type == EntityType.SUPPLIER && !blocked.contains("oldcode"));
        args.put("searchShortName", (type == EntityType.CUSTOMER || type == EntityType.SUPPLIER) && !blocked.contains("shortname"));
        args.put("searchPinyin", Arrays.asList(EntityType.PRODUCT, EntityType.CUSTOMER, EntityType.SUPPLIER).contains(type) && !blocked.contains("pinyincode"));
        args.put("searchWubi", Arrays.asList(EntityType.PRODUCT, EntityType.CUSTOMER, EntityType.SUPPLIER).contains(type) && !blocked.contains("wubicode"));
        args.put("showCode", Boolean.TRUE.equals(args.get("searchCode")));
    }

    private static List<String> matchModes(EntityType type,Map<String,Object> args,String keyword) {
        List<String> result=new ArrayList<>();
        if(Boolean.TRUE.equals(args.get("searchCode"))) result.add("CODE");
        if(type==EntityType.PRODUCT && Boolean.TRUE.equals(args.get("searchFactoryCode"))) result.add("FACTORY_CODE");
        if(type==EntityType.PRODUCT && Boolean.TRUE.equals(args.get("searchBarCode"))) result.add("BAR_CODE");
        if(type==EntityType.SUPPLIER && Boolean.TRUE.equals(args.get("searchOldCode"))) result.add("OLD_CODE");
        result.add("EXACT_NAME");
        if(type==EntityType.PRODUCT && ErpKeywordQuery.shouldAppendTokenProductItemCondition(
                ErpKeywordQuery.parseKeywordSearch(keyword))) result.add("TOKEN_FUZZY");
        result.add("FUZZY");
        return result;
    }

    private static void addHiddenFields(Set<String> blocked,List<String> hidden) {
        if(hidden==null) throw new AssistantFailure("FORBIDDEN","字段权限尚未确定");
        for(String field:hidden) blocked.add(normalizeField(field));
    }

    private static Set<Long> safeLongs(Object value) {
        if(!(value instanceof Collection)) return Collections.emptySet();
        Set<Long> result=new LinkedHashSet<>();
        for(Object item:(Collection<?>)value) if(item instanceof Number) result.add(((Number)item).longValue());
        return result;
    }

    private boolean canSearch(EntityType type) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null || !permissions.hasAnyPermissions(userId, "erp:assistant:query")) return false;
        switch (type) {
            case PRODUCT: return permissions.hasAnyPermissions(userId, "erp:stock:query", "erp:sale-report:query", "erp:purchase-report:query", "erp:sale-order:query");
            case CUSTOMER: return permissions.hasAnyPermissions(userId, "erp:sale-report:query", "erp:finance-receipt:query", "erp:receivable-account:query", "erp:sale-order:query");
            case SUPPLIER: return permissions.hasAnyPermissions(userId, "erp:purchase-report:query", "erp:finance-payment:query", "erp:payable-account:query");
            case WAREHOUSE: return permissions.hasAnyPermissions(userId, "erp:stock:query");
            case DEPARTMENT: return true;
            case SALESPERSON: return permissions.hasAnyPermissions(userId,"erp:sale-report:query","erp:receivable-account:query","erp:sale-order:query");
            default: return false;
        }
    }

    private Long requiredUser() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null || TenantContextHolder.getTenantId() == null || TenantContextHolder.isIgnore()
                || !permissions.hasAnyPermissions(userId, "erp:assistant:query"))
            throw new AssistantFailure("FORBIDDEN", "没有智能问数权限");
        return userId;
    }

    private void requireAny(String... permissionCodes) {
        Long userId = requiredUser();
        if (!permissions.hasAnyPermissions(userId, permissionCodes))
            throw new AssistantFailure("FORBIDDEN", "没有该对象对应的业务查询权限");
    }

    private Map<String, Object> baseContext() {
        Map<String, Object> args = new HashMap<>();
        args.put("tenantId", TenantContextHolder.getRequiredTenantId());
        args.put("userId", String.valueOf(requiredUser()));
        return args;
    }

    private int maxCandidates() {
        return Math.max(1, Math.min(20, properties.getEntitySearch().getMaxCandidates()));
    }

    private static EntityType typeForPlan(AssistantPlan plan, String field) {
        if ("product".equals(field)) return EntityType.PRODUCT;
        if ("warehouse".equals(field)) return EntityType.WAREHOUSE;
        if ("dept".equals(field)) return EntityType.DEPARTMENT;
        if ("party".equals(field)) return Arrays.asList(AssistantPlan.Metric.SALE, AssistantPlan.Metric.RECEIPT,
                AssistantPlan.Metric.RECEIVABLE).contains(plan.getMetric()) ? EntityType.CUSTOMER : EntityType.SUPPLIER;
        throw new IllegalArgumentException("Unsupported plan entity field");
    }

    private static String module(EntityType type) {
        switch (type) {
            case PRODUCT: return "erp_product";
            case CUSTOMER: return "erp_customer";
            case SUPPLIER: return "erp_supplier";
            case WAREHOUSE: return "erp_warehouse";
            case DEPARTMENT: return "system_dept";
            case SALESPERSON: return "erp_sale_report";
            default: throw new IllegalArgumentException();
        }
    }

    private static String normalizeField(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceFirst("^(col_|report_|item_)", "").replace("_", "");
    }

    private static Collection<Long> safe(Collection<Long> values) {
        return values == null ? Collections.emptySet() : values;
    }

    private static Long loginDeptId() {
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        if (user == null || user.getInfo() == null) return null;
        String value = user.getInfo().get(LoginUser.INFO_KEY_DEPT_ID);
        try { return value == null ? null : Long.valueOf(value); } catch (NumberFormatException ignored) { return null; }
    }

    private static List<Map<String, Object>> decorate(List<Map<String, Object>> rows, EntityType type, String matchMode) {
        for (Map<String, Object> row : rows) {
            row.put("entityType", type.name());
            row.put("matchType", row.get("matchedBy") == null ? matchMode : row.remove("matchedBy"));
        }
        return rows;
    }

    private static List<Map<String, Object>> deduplicate(List<Map<String, Object>> values, int limit) {
        Map<String, Map<String, Object>> unique = new LinkedHashMap<>();
        for (Map<String, Object> value : values) {
            String key = value.get("entityType") + ":" + value.get("id");
            unique.putIfAbsent(key, value);
            if (unique.size() >= limit) break;
        }
        return new ArrayList<>(unique.values());
    }
}
