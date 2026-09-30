package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/** Permission projection used only by hybrid business tools and semantic datasets. */
@Service
public class AssistantToolPermissionService {
    @Resource private PermissionApi permissions;
    @Resource private ErpWarehouseService warehouses;

    public void authorize(String permission,String module,Collection<String> protectedFields) {
        Long uid=requiredUser(permission);
        List<String> hidden=permissions.getCurrentUserHiddenFields(module);
        if(hidden==null) throw new AssistantFailure("FORBIDDEN","字段权限尚未确定");
        Set<String> requested=new HashSet<>();for(String field:protectedFields) requested.add(normalizeField(field));
        for(String field:hidden) if(requested.contains(normalizeField(field)))
            throw new AssistantFailure("FIELD_FORBIDDEN","该查询涉及当前不可见字段");
    }

    public Map<String,String> visibleFields(String permission,String module,Map<String,String> fields) {
        requiredUser(permission);
        List<String> hidden=permissions.getCurrentUserHiddenFields(module);
        if(hidden==null) throw new AssistantFailure("FORBIDDEN","字段权限尚未确定");
        Set<String> blocked=new HashSet<>();for(String field:hidden) blocked.add(normalizeField(field));
        Map<String,String> visible=new LinkedHashMap<>();
        for(Map.Entry<String,String> field:fields.entrySet()) if(!blocked.contains(normalizeField(field.getKey()))) visible.put(field.getKey(),field.getValue());
        if(visible.isEmpty()) throw new AssistantFailure("FIELD_FORBIDDEN","该数据集没有当前可见字段");
        return visible;
    }

    public void documentScope(Map<String,Object> args,String prefix,String module,boolean stringSelf) {
        Long uid=requiredUser(null);
        DeptDataPermissionRespDTO scope=permissions.getDeptDataPermission(uid,module);
        if(scope==null || (!Boolean.TRUE.equals(scope.getAll()) && !Boolean.TRUE.equals(scope.getSelf())
                && (scope.getDeptIds()==null || scope.getDeptIds().isEmpty())))
            throw new AssistantFailure("FORBIDDEN","没有该业务的数据访问范围");
        args.put(prefix+"All",Boolean.TRUE.equals(scope.getAll()));
        args.put(prefix+"DeptIds",scope.getDeptIds()==null?Collections.emptySet():scope.getDeptIds());
        args.put(prefix+"SelfUserId",Boolean.TRUE.equals(scope.getSelf())?(stringSelf?String.valueOf(uid):uid):null);
    }

    public void stockScope(Map<String,Object> args) {
        requiredUser(null);
        ErpProductStockPermissionScope scope=warehouses.getCurrentUserProductStockPermissionScope();
        if(scope==null || (!scope.isAll() && scope.getVisibleWarehouseIds().isEmpty())) throw new AssistantFailure("FORBIDDEN","没有可见仓库");
        args.put("stockAll",scope.isAll());args.put("stockDeptIds",scope.getDepartmentWarehouseIds());args.put("stockSelfIds",scope.getSelfWarehouseIds());
    }

    private Long requiredUser(String permission) {
        Long uid=SecurityFrameworkUtils.getLoginUserId();
        if(uid==null || TenantContextHolder.getTenantId()==null || TenantContextHolder.isIgnore()
                || !permissions.hasAnyPermissions(uid,"erp:assistant:query")
                || permission!=null && !permissions.hasAnyPermissions(uid,permission))
            throw new AssistantFailure("FORBIDDEN","没有该业务查询权限");
        return uid;
    }

    private static String normalizeField(String value) {
        return value==null?"":value.toLowerCase(Locale.ROOT).replaceFirst("^(col_|report_|item_)","").replace("_","");
    }
}
