package cn.iocoder.yudao.module.erp.service.finance.accounting.rule;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.*;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.*;
import cn.iocoder.yudao.module.erp.service.finance.accounting.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.time.*;
import java.util.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherSourceReader.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleCalculator.require;

@Service
public class ErpVoucherGenerationService {
    @Resource private ErpVoucherSourceReader reader;
    @Resource private ErpVoucherRuleStore store;
    @Resource private ErpVoucherRuleCalculator calculator;
    @Resource private ErpVoucherMapper vouchers;
    @Resource private ErpVoucherOriginLookup originLookup;
    @Resource private ErpVoucherAttributionMapper attributions;
    @Resource private ErpVoucherService voucherService;
    @Resource private ErpBookOpenService bookOpenService;

    public List<Preview> preview(Batch batch) {
        require(batch!=null&&batch.getItems()!=null&&batch.getItems().size()<=200,"单次最多预览200笔单据");
        List<Preview> result=new ArrayList<>();for(Request r:batch.getItems())result.add(previewOne(r));return result;
    }
    public Preview previewOne(Request r) {
        Preview p=new Preview();
        if(r==null) {p.setStatus("MANUAL");p.getIssues().add("缺少来源参数");return p;}
        p.setBizType(r.getBizType());p.setBizId(r.getBizId());
        try {
            Source s=reader.read(r.getBizType(),r.getBizId());p.setBizNo(s.no());p.setSourceAmount(s.amount());p.setSourceVersion(s.version());p.getEvidence().put("source",s);
            List<ErpVoucherDO> existing=originLookup.find(r.getBizType(),r.getBizId());
            if(!existing.isEmpty()) {p.setStatus("GENERATED");p.setVoucherId(existing.get(0).getId());return p;}
            Config cfg=store.config();Context c=store.context(r.getBizType(),r.getBizId());
            p.setConfigVersion(cfg.getVersion());p.setContextVersion(c.getVersion());p.setScenario(c.getScenario());
            require(s.version().equals(c.getSourceVersion()),"待补充：来源已变化，请重新核对并保存记账信息");
            if(c.getPostedBizType()!=null || c.getPostedBizId()!=null) {
                reader.read(c.getPostedBizType(),c.getPostedBizId());
                require(!originLookup.find(c.getPostedBizType(),c.getPostedBizId()).isEmpty(),"待补充：关联来源尚未生成凭证");
                p.setStatus("LINKED");p.getIssues().add("该经济事项由关联来源记账，不独立生成");return p;
            }
            LocalDate date=r.getVoucherDate()==null?s.date():r.getVoucherDate();p.setVoucherDate(date);
            require(!YearMonth.from(date).isBefore(YearMonth.from(s.date())),"归属月份不能早于业务月份");
            require(r.getAttributionYear()==null||r.getAttributionYear()==date.getYear(),"制单日期与归属年度不一致");
            require(r.getAttributionMonth()==null||r.getAttributionMonth()==date.getMonthValue(),"制单日期与归属月份不一致");
            require(bookOpenService.isVoucherTypeEnabled(date,voucherType(r.getBizType())),"待补充：目标期间未开账或未启用对应凭证类型");
            p.setItems(calculator.calculate(s,c,cfg,p));p.setStatus("READY");
            p.getEvidence().put("source",s);p.getEvidence().put("context",c);p.getEvidence().put("config",cfg);
            p.getEvidence().put("template",ErpVoucherTemplates.get(c.getScenario()));
            p.setPreviewToken(DigestUtil.sha256Hex(JsonUtils.toJsonString(p)));return p;
        } catch(ServiceException ex) {
            p.setStatus(ex.getMessage().startsWith("未配置")?"UNCONFIGURED":ex.getMessage().startsWith("待补充")?"INCOMPLETE":"MANUAL");
            p.getIssues().add(ex.getMessage());p.getItems().clear();return p;
        }
    }
    @Transactional(rollbackFor=Exception.class)
    public List<Long> generate(Batch batch) {
        require(batch!=null&&batch.getItems()!=null&&!batch.getItems().isEmpty()&&batch.getItems().size()<=200,"请选择1至200笔单据");
        // 每个租户一把数据库配置行锁，与配置和记账信息修改互斥；事务结束自动释放。
        store.lockConfig();List<Long> ids=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Request r:batch.getItems()) {
            require(r!=null&&r.getBizType()!=null&&r.getBizId()!=null,"缺少来源类型或单据编号");
            require(seen.add(r.getBizType()+":"+r.getBizId()),"批次包含重复单据");
            reader.lock(r.getBizType(),r.getBizId());
            Preview p=previewOne(r);
            if("GENERATED".equals(p.getStatus())) {ids.add(p.getVoucherId());continue;}
            require("READY".equals(p.getStatus()),p.getBizNo()+"："+String.join("；",p.getIssues()));
            require(r.getPreviewToken()!=null&&r.getPreviewToken().equals(p.getPreviewToken()),"预览已过期，请重新预览后生成："+p.getBizNo());
            Source s=reader.read(r.getBizType(),r.getBizId());
            Long id=voucherService.createVoucherFromBiz(r.getBizType(),r.getBizId(),p.getBizNo(),p.getSourceAmount(),p.getVoucherDate(),p.getBizNo(),p.getItems());
            ErpVoucherDO patch=new ErpVoucherDO();patch.setId(id);patch.setDeptId(longValue(s.getHeader(),"deptId"));patch.setGenerationSnapshot(JsonUtils.toJsonString(p));vouchers.updateById(patch);
            ErpVoucherAttributionDO a=attributions.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpVoucherAttributionDO>().eq(ErpVoucherAttributionDO::getBizType,r.getBizType()).eq(ErpVoucherAttributionDO::getBizId,r.getBizId()));
            if(a==null)a=new ErpVoucherAttributionDO();
            a.setBizType(r.getBizType());a.setBizId(r.getBizId());a.setBizNo(s.no());a.setBizDate(s.date().atStartOfDay());a.setBizAmount(p.getSourceAmount());a.setDeptId(longValue(s.getHeader(),"deptId"));a.setHandlerUserId(SecurityFrameworkUtils.getLoginUserId());
            a.setVoucherMakeDate(p.getVoucherDate());a.setAttributionYear(p.getVoucherDate().getYear());a.setAttributionMonth(p.getVoucherDate().getMonthValue());a.setAttributionStatus(30);a.setVoucherId(id);
            if(a.getId()==null)attributions.insert(a);else attributions.updateById(a);ids.add(id);
        }
        return ids;
    }
    public static int voucherType(int type) {
        switch(type) {case 2:case 3:return 1;case 4:return 2;case 6:case 21:return 3;case 22:return 4;case 23:return 5;case 8:case 9:return 7;case 10:case 12:return 8;case 16:return 9;case 17:return 10;case 18:return 11;default:throw problem("不支持的凭证来源");}
    }
}
