package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.service.finance.accounting.rule.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
@RestController @RequestMapping("/erp/voucher-rule")
public class ErpVoucherRuleController {
    @Resource private ErpVoucherRuleStore store;
    @Resource private ErpVoucherAuxiliarySupport auxiliaries;
    @Resource private ErpVoucherSourceReader reader;
    @GetMapping("/get") @PreAuthorize("@ss.hasAnyPermissions('erp:voucher-rule:query','erp:voucher-attribution:generate')")
    public CommonResult<Config> get(){return success(store.config());}
    @PutMapping("/save") @PreAuthorize("@ss.hasPermission('erp:voucher-rule:update')")
    public CommonResult<Boolean> save(@RequestBody Config c){
        if(c.getProjects()==null) throw ErpVoucherSourceReader.problem("项目集合不能为空");
        Set<Long> validDepartments=new HashSet<>();auxiliaries.options("dept").forEach(d->validDepartments.add(d.getId()));
        Config previous=store.config();
        for(Project p:c.getProjects()) {
            if(p==null||p.getDeptIds()==null||p.getDeptIds().isEmpty()) throw ErpVoucherSourceReader.problem("请选择项目部门");
            Project old=previous.getProjects().stream().filter(o->Objects.equals(o.getId(),p.getId())).findFirst().orElse(null);
            List<Long> oldIds=old==null?Collections.emptyList():old.getDeptIds();
            if(p.getDeptIds().stream().anyMatch(id->!validDepartments.contains(id)&&!oldIds.contains(id))) throw ErpVoucherSourceReader.problem("只能新增有访问权限且启用的项目部门");
            if(oldIds.stream().anyMatch(id->!validDepartments.contains(id)&&!p.getDeptIds().contains(id))) throw ErpVoucherSourceReader.problem("无权限或已停用的既有部门须保留");
        }
        store.saveConfig(c);return success(true);
    }
    @PutMapping("/enabled") @PreAuthorize("@ss.hasPermission('erp:voucher-rule:update')")
    public CommonResult<Boolean> enabled(@RequestBody EnableRequest request){store.setEnabled(request);return success(true);}
    @GetMapping("/templates") @PreAuthorize("@ss.hasAnyPermissions('erp:voucher-rule:query','erp:voucher-attribution:generate')")
    public CommonResult<List<Template>> templates(){return success(ErpVoucherTemplates.ALL);}
    @GetMapping("/auxiliary-options") @PreAuthorize("@ss.hasAnyPermissions('erp:voucher:query','erp:voucher:create','erp:voucher:update','erp:voucher-attribution:generate','erp:voucher-rule:query')")
    public CommonResult<List<Auxiliary>> options(@RequestParam String type){return success(auxiliaries.options(type));}
    @GetMapping("/choices") @PreAuthorize("@ss.hasAnyPermissions('erp:voucher-rule:query','erp:voucher-attribution:generate')")
    public CommonResult<Map<String,Object>> choices(){
        Map<String,Object> r=new LinkedHashMap<>();
        r.put("subjects",reader.list("erpAccountingSubjectMapper",new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>().eq("enable",true).eq("is_leaf",true)));
        r.put("categories",reader.list("erpProductCategoryMapper",new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>().eq("status",0)));
        r.put("accounts",reader.list("erpAccountMapper",new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>().eq("status",0)));
        r.put("departments",auxiliaries.options("dept"));return success(r);
    }
}
