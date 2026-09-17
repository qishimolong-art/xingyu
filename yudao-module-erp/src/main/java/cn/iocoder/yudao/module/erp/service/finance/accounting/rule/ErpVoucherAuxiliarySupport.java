package cn.iocoder.yudao.module.erp.service.finance.accounting.rule;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.*;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpSubjectAuxiliaryService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherSourceReader.*;

@Component
public class ErpVoucherAuxiliarySupport {
    @Resource private ErpSubjectAuxiliaryService auxiliaryService;
    @Resource private ErpVoucherSourceReader reader;
    @Resource private ErpVoucherRuleStore store;
    @Resource private DeptApi deptApi;
    @Resource private AdminUserApi userApi;
    @Resource private PermissionApi permissionApi;

    public boolean allowedDept(Long id) {
        DeptDataPermissionRespDTO p=permissionApi.getDeptDataPermission(SecurityFrameworkUtils.getLoginUserId(),"erp_voucher");
        return p!=null&&(Boolean.TRUE.equals(p.getAll())||(id!=null&&p.getDeptIds().contains(id)));
    }
    public List<Auxiliary> options(String type) {
        List<Auxiliary> result=new ArrayList<>();
        if("customer".equals(type)||"supplier".equals(type)) {
            for(Map<String,Object> row:reader.list("customer".equals(type)?"erpCustomerMapper":"erpSupplierMapper",new QueryWrapper<>().eq("status",0).orderByAsc("id")))
                result.add(value(type,longValue(row,"id"),Objects.toString(row.get("name"),"")));
        } else if("dept".equals(type)) {
            deptApi.getDeptListByStatus(0).stream().filter(d->allowedDept(d.getId())).forEach(d->result.add(value(type,d.getId(),d.getName())));
        } else if("person".equals(type)) {
            userApi.getUserListByStatus(0).stream().filter(u->allowedDept(u.getDeptId())||Objects.equals(u.getId(),SecurityFrameworkUtils.getLoginUserId())).forEach(u->result.add(value(type,u.getId(),u.getNickname())));
        } else if("project".equals(type)) {
            store.config().getProjects().stream().filter(p->Boolean.TRUE.equals(p.getEnabled())&&p.getDeptIds()!=null&&p.getDeptIds().stream().anyMatch(this::allowedDept)).forEach(p->result.add(value(type,p.getId(),p.getName())));
        } else throw problem("不支持的辅助核算维度："+type);
        return result;
    }
    public Auxiliary resolve(Auxiliary input) {
        if(input==null||input.getType()==null||input.getId()==null) throw problem("待补充：辅助核算类型和对象不能为空");
        return options(input.getType()).stream().filter(a->input.getId().equals(a.getId())).findFirst().orElseThrow(()->problem("辅助核算对象不存在、已停用或无权限："+input.getType()+" / "+input.getId()));
    }
    public void validate(ErpVoucherItemDO item) {
        List<Auxiliary> list=item.getAuxiliaries();
        if(list==null) {
            list=new ArrayList<>();
            if(item.getAuxiliaryType()!=null && item.getAuxiliaryId()!=null) list.add(value(item.getAuxiliaryType(),item.getAuxiliaryId(),item.getAuxiliaryName()));
        } else if(item.getAuxiliaryId()!=null && list.stream().noneMatch(a->Objects.equals(a.getType(),item.getAuxiliaryType())&&Objects.equals(a.getId(),item.getAuxiliaryId()))) {
            throw problem("新旧辅助核算字段冲突");
        }
        Set<String> required=auxiliaryService.getListBySubjectId(item.getSubjectId()).stream().map(ErpSubjectAuxiliaryDO::getAuxiliaryType).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> seen=new HashSet<>(); List<Auxiliary> normalized=new ArrayList<>();
        for(Auxiliary a:list) {
            if(!seen.add(a.getType())) throw problem("辅助核算维度重复");
            if(!required.contains(a.getType())) throw problem("科目未配置此辅助核算维度："+a.getType());
            normalized.add(resolve(a));
        }
        if(!seen.containsAll(required)) throw problem("待补充：科目要求辅助核算 "+String.join("、",required));
        item.setAuxiliaries(normalized);
        item.setAuxiliaryType(normalized.isEmpty()?null:normalized.get(0).getType());
        item.setAuxiliaryId(normalized.isEmpty()?null:normalized.get(0).getId());
        item.setAuxiliaryName(normalized.isEmpty()?item.getAuxiliaryName():normalized.stream().map(Auxiliary::getName).collect(Collectors.joining(" / ")));
    }
    public void fill(ErpVoucherItemDO item,Map<String,Object> source,List<Auxiliary> supplements) {
        Map<String,Auxiliary> values=new HashMap<>();
        if(supplements!=null) for(Auxiliary a:supplements) {
            if(values.put(a.getType(),a)!=null) throw problem("辅助核算维度重复");
        }
        for(String type:Arrays.asList("customer","supplier","dept","person","project")) {
            Long id=longValue(source,type+"Id"); if(id!=null) values.put(type,value(type,id,null));
        }
        Long party=longValue(source,"partyId"), partyType=longValue(source,"partyType");
        if(party!=null&&partyType!=null) {
            String type=partyType==1?"customer":partyType==2?"supplier":partyType==3?"person":null;
            if(type!=null) values.put(type,value(type,party,null));
        }
        List<Auxiliary> all=new ArrayList<>();
        for(ErpSubjectAuxiliaryDO required:auxiliaryService.getListBySubjectId(item.getSubjectId())) {
            Auxiliary a=values.get(required.getAuxiliaryType()); if(a!=null) all.add(a);
        }
        item.setAuxiliaries(all); validate(item);
    }
    public static Auxiliary value(String type,Long id,String name) { Auxiliary a=new Auxiliary();a.setType(type);a.setId(id);a.setName(name);return a; }
}
