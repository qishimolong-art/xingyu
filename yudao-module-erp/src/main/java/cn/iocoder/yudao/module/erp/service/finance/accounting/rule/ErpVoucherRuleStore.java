package cn.iocoder.yudao.module.erp.service.finance.accounting.rule;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.*;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.*;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAccountingSubjectService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherSourceReader.*;

@Service
public class ErpVoucherRuleStore {
    @Resource private ErpVoucherRuleStateMapper mapper;
    @Resource private ErpAccountingSubjectService subjects;
    @Resource private ErpVoucherSourceReader sources;

    public Config config() {
        ErpVoucherRuleStateDO row=mapper.get("CONFIG",false);
        if(row==null) return new Config();
        Config c=JsonUtils.parseObject(row.getPayload(),Config.class); c.setVersion(row.getVersion()); return c;
    }
    public ErpVoucherRuleStateDO lockConfig() {
        ErpVoucherRuleStateDO row=mapper.get("CONFIG",true);
        if(row==null) throw problem("未配置：请先保存凭证生成规则"); return row;
    }
    @Transactional(rollbackFor=Exception.class)
    public void bindNewFundAccount(Long accountId,Long subjectId) {
        if(mapper.get("CONFIG",true)==null)return;
        Config c=config();
        if(c.getAccounts().stream().anyMatch(a->Objects.equals(accountId,a.getAccountId())))return;
        subject(subjectId);AccountMapping a=new AccountMapping();a.setAccountId(accountId);a.setSubjectId(subjectId);c.getAccounts().add(a);
        save("CONFIG",c.getVersion(),c);
    }
    public ErpAccountingSubjectDO subject(Long id) {
        ErpAccountingSubjectDO s=id==null?null:subjects.getSubject(id);
        if(s==null || !Boolean.TRUE.equals(s.getEnable()) || !Boolean.TRUE.equals(s.getIsLeaf()))
            throw problem("未配置：科目不存在、已停用或不是末级科目："+id);
        return s;
    }
    @Transactional(rollbackFor=Exception.class)
    public void saveConfig(Config c) {
        if(c==null || !Arrays.asList("UNSET","GENERAL","SMALL").contains(c.getTaxpayer())) throw problem("纳税身份无效");
        if(c.getMappings()==null||c.getAccounts()==null||c.getEnabledScenarios()==null||c.getProjects()==null) throw problem("配置集合不能为空");
        Set<String> keys=new HashSet<>();
        for(Mapping m:c.getMappings()) {
            if(m==null) throw problem("科目映射不能为空");
            Template t=ErpVoucherTemplates.get(m.getScenario());
            if(t==null || !t.getRoles().contains(m.getRole())) throw problem("模板或科目角色无效");
            if(!keys.add(m.getScenario()+":"+m.getRole()+":"+m.getCategoryId())) throw problem("同一场景、角色和类别不能重复配置");
            if(m.getCategoryId()!=null) sources.one("erpProductCategoryMapper",m.getCategoryId());
            subject(m.getSubjectId());
        }
        Set<Long> accounts=new HashSet<>();
        for(AccountMapping a:c.getAccounts()) {
            if(a==null) throw problem("账户映射不能为空");
            if(!accounts.add(a.getAccountId())) throw problem("结算账户重复配置");
            Map<String,Object> account=sources.one("erpAccountMapper",a.getAccountId());
            if(!Integer.valueOf(0).equals(account.get("status"))) throw problem("结算账户已停用");
            subject(a.getSubjectId());
        }
        for(String code:c.getEnabledScenarios()) {
            Template t=ErpVoucherTemplates.get(code); if(t==null) throw problem("模板无效");
            if(t.isTaxable() && "UNSET".equals(c.getTaxpayer())) throw problem("启用涉税模板前请配置纳税身份");
            for(String role:t.getRoles()) {
                if("SMALL".equals(c.getTaxpayer())&&"INPUT_TAX".equals(role))continue;
                if(c.getMappings().stream().noneMatch(m->code.equals(m.getScenario())&&role.equals(m.getRole())&&m.getCategoryId()==null))
                    throw problem("启用前请配置默认科目："+t.getName()+" / "+role);
            }
        }
        Config old=config();
        long next=old.getProjects().stream().map(Project::getId).filter(Objects::nonNull).max(Long::compare).orElse(0L)+1;
        Set<Long> projectIds=new HashSet<>(); Set<String> names=new HashSet<>();
        for(Project p:c.getProjects()) {
            if(p==null) throw problem("项目不能为空");
            if(p.getName()==null || p.getName().trim().isEmpty() || !names.add(p.getName().trim())) throw problem("项目名称不能为空或重复");
            if(p.getId()==null) p.setId(next++);
            else if(old.getProjects().stream().noneMatch(o->o.getId().equals(p.getId()))) throw problem("项目编号无效");
            if(!projectIds.add(p.getId())) throw problem("项目编号重复");
        }
        // 项目可停用不可删除，保证已生成凭证的引用仍可解释。
        for(Project p:old.getProjects()) if(!projectIds.contains(p.getId())) throw problem("已有项目请停用，不可删除");
        save("CONFIG",c.getVersion(),c);
    }
    @Transactional(rollbackFor=Exception.class)
    public void setEnabled(EnableRequest request) {
        if(request==null||request.getEnabled()==null||ErpVoucherTemplates.get(request.getScenario())==null) throw problem("启停参数无效");
        lockConfig();Config c=config();
        if(!Objects.equals(c.getVersion(),request.getVersion())) throw problem("配置已变化，请刷新");
        if(request.getEnabled()) {c.getEnabledScenarios().add(request.getScenario());saveConfig(c);}
        else {c.getEnabledScenarios().remove(request.getScenario());save("CONFIG",c.getVersion(),c);}
    }
    public Context context(Integer type,Long id) {
        ErpVoucherSourceReader.Source s=sources.read(type,id);
        ErpVoucherRuleStateDO row=mapper.get(key(type,id),false);
        Context c=row==null?new Context():JsonUtils.parseObject(row.getPayload(),Context.class);
        c.setBizType(type); c.setBizId(id); c.setVersion(row==null?0L:row.getVersion());
        if(row==null) c.setSourceVersion(s.version()); return c;
    }
    @Transactional(rollbackFor=Exception.class)
    public void saveContext(Context c) {
        if(c==null) throw problem("记账信息不能为空");
        if(c.getAuxiliaries()==null||c.getAuxiliaries().stream().anyMatch(a->a==null||a.getType()==null||a.getId()==null)) throw problem("辅助核算信息不完整");
        if(c.getAllocationWeights()==null) throw problem("分摊权重集合不能为空");
        lockConfig();
        ErpVoucherSourceReader.Source s=sources.read(c.getBizType(),c.getBizId());
        if(!s.version().equals(c.getSourceVersion())) throw problem("来源已变化，请重新加载记账信息");
        if(c.getPostedBizType()!=null||c.getPostedBizId()!=null) {
            if(Objects.equals(c.getBizType(),c.getPostedBizType())&&Objects.equals(c.getBizId(),c.getPostedBizId())) throw problem("不能关联自身");
            sources.read(c.getPostedBizType(),c.getPostedBizId());
        }
        save(key(c.getBizType(),c.getBizId()),c.getVersion(),c);
    }
    private void save(String key,Long expected,Object payload) {
        if(expected==null) throw problem("缺少版本号，请刷新");
        ErpVoucherRuleStateDO row=mapper.get(key,true);
        if(row==null) {
            if(expected!=0L) throw problem("配置已变化，请刷新");
            row=new ErpVoucherRuleStateDO(); row.setStateKey(key); row.setVersion(1L); row.setPayload(JsonUtils.toJsonString(payload)); mapper.insert(row);
        } else {
            if(!expected.equals(row.getVersion())) throw problem("配置已变化，请刷新");
            ErpVoucherRuleStateDO patch=new ErpVoucherRuleStateDO(); patch.setVersion(expected+1); patch.setPayload(JsonUtils.toJsonString(payload));
            if(mapper.update(patch,new LambdaUpdateWrapper<ErpVoucherRuleStateDO>().eq(ErpVoucherRuleStateDO::getId,row.getId()).eq(ErpVoucherRuleStateDO::getVersion,expected))!=1) throw problem("配置已变化，请刷新");
        }
        // 每次保存一个不可变审计快照，保留 BaseDO 的操作人及时间。
        ErpVoucherRuleStateDO audit=new ErpVoucherRuleStateDO();
        audit.setStateKey("AUDIT:"+UUID.randomUUID()); audit.setVersion(expected+1); audit.setPayload(JsonUtils.toJsonString(payload)); mapper.insert(audit);
    }
    private String key(Integer t,Long id) { return "CONTEXT:"+t+":"+id; }
}
