package cn.iocoder.yudao.module.erp.service.finance.accounting.rule;

import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.*;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.math.*;
import java.util.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherSourceReader.*;

/** 只计算和校验，无数据库写入；预览、正式生成共用。 */
@Component
public class ErpVoucherRuleCalculator {
    @Resource private ErpVoucherRuleStore store;
    @Resource private ErpVoucherSourceReader reader;
    @Resource private ErpVoucherAuxiliarySupport auxiliaries;
    public static BigDecimal money(BigDecimal v) { return v.setScale(2,RoundingMode.HALF_UP); }
    public static void require(boolean ok,String reason) { if(!ok) throw problem(reason); }

    public List<ErpVoucherItemDO> calculate(Source s,Context c,Config cfg,Preview preview) {
        Template t=ErpVoucherTemplates.get(c.getScenario());
        require(t!=null && t.getBizType()==s.getType(),"待补充：请选择适用业务性质");
        require(cfg.getEnabledScenarios().contains(t.getCode()),"未配置：此业务规则尚未启用");
        if(t.isTaxable()) require(!"UNSET".equals(cfg.getTaxpayer()),"未配置：请设置纳税身份");
        List<ErpVoucherItemDO> result=new ArrayList<>();
        Ledger ledger=new Ledger(s,c,cfg,result,preview);
        Map<String,Object> h=s.getHeader(); BigDecimal amount=money(s.amount());
        BigDecimal discount=money(optional(h,h.containsKey("discountAmount")?"discountAmount":"discountPrice"));
        String scene=c.getScenario();
        if(Arrays.asList(2,3,8,9).contains(s.getType())) {
            require(Boolean.TRUE.equals(c.getRevenueConfirmed()) || (s.getType()!=2&&s.getType()!=3),"待补充：请确认收入/退货确认条件");
            require(discount.signum()>=0,"需人工处理：负数购销优惠");
            if(discount.signum()!=0) require("COMMERCIAL".equals(c.getDiscountNature()),"待补充：购销优惠须确认为商业折扣；现金折扣在收付款处理");
            BigDecimal fee=money(h.get("feeAmount")!=null?decimal(h,"feeAmount"):optional(h,"otherPrice"));
            require(fee.signum()>=0,"需人工处理：负数附加费用");
            if(fee.signum()!=0) {
                require(Boolean.TRUE.equals(c.getFeeConfirmed()) && Boolean.FALSE.equals(c.getFeeAlreadyPosted()),"待补充：确认费用承担及未在其他单据记账");
                require(c.getFeePartyId()!=null && c.getFeePartyType()!=null,"待补充：确认费用往来对象");
                Long sourceParty=longValue(h,s.getType()==8||s.getType()==9?"supplierId":"customerId");
                require(Objects.equals(c.getFeePartyId(),sourceParty)&&c.getFeePartyType()==(s.getType()==8||s.getType()==9?2:1),"需人工处理：第三方费用需独立记账，不能计入本单往来");
                require((s.getType()==8||s.getType()==9?"PROCUREMENT":"SALE_RECHARGE").equals(c.getFeeNature()),"待补充：请选择采购费用或销售附加价款性质");
            }
            BigDecimal product=money(decimal(h,"totalProductPrice").subtract(discount));
            require(product.signum()>=0,"需人工处理：优惠超过商品金额");
            BigDecimal tax=tax(c,cfg,scene);
            boolean inclusive="INCLUSIVE".equals(c.getPriceBasis());
            BigDecimal expected=product.add(fee).add(inclusive?BigDecimal.ZERO:tax);
            require(money(expected).compareTo(amount)==0,"需人工处理：价税及费用合计与原单金额不一致，差额 "+money(expected.subtract(amount)));
            Map<Long,BigDecimal> stock=stockCosts(s);
            BigDecimal stockTotal=sum(stock.values()); preview.setStockAmount(stockTotal);
            if(s.getType()==8||s.getType()==9) {
                require(!"PURCHASE".equals(scene)||"RECEIVED".equals(c.getInvoiceStatus()),"需人工处理：未到票请使用月末暂估场景");
                if("PROVISIONAL".equals(scene)) {
                    require("NOT_RECEIVED".equals(c.getInvoiceStatus())&&tax.signum()==0,"暂估不得确认进项税额");
                    require(preview.getVoucherDate().equals(preview.getVoucherDate().withDayOfMonth(preview.getVoucherDate().lengthOfMonth())),"暂估首次入账请选择月末日期");
                }
                // 第一版税额仅对应商品；有独立费用税额、混合抵扣条件时转人工。
                boolean deductible="GENERAL".equals(cfg.getTaxpayer())&&"DEDUCTIBLE".equals(c.getDeductionStatus());
                BigDecimal inventory=product.subtract(inclusive&&deductible?tax:BigDecimal.ZERO).add(!inclusive&&!deductible?tax:BigDecimal.ZERO);
                require(inventory.signum()>=0,"税额不能超过商品金额");
                require(money(inventory).compareTo(stockTotal)==0,"需人工处理：凭证库存与库存流水不一致，差额 "+money(inventory.subtract(stockTotal)));
                int sign=s.getType()==9?-1:1;
                ledger.distribute("INVENTORY",inventory.multiply(BigDecimal.valueOf(sign)),weights(s,c));
                if(deductible) ledger.line("INPUT_TAX",tax.multiply(BigDecimal.valueOf(sign)),null,h);
                ledger.line("SELLING_EXPENSE",fee.multiply(BigDecimal.valueOf(sign)),null,h);
                ledger.line("PROVISIONAL".equals(scene)?"PROVISIONAL_AP":"AP",amount.multiply(BigDecimal.valueOf(-sign)),null,h);
            } else {
                int sign=s.getType()==3?-1:1;
                BigDecimal revenue=amount.subtract(tax);
                require(revenue.signum()>=0,"税额不能超过销售金额");
                ledger.line("AR",amount.multiply(BigDecimal.valueOf(sign)),null,h);
                ledger.distribute("REVENUE",revenue.multiply(BigDecimal.valueOf(-sign)),weights(s,c));
                ledger.line("OUTPUT_TAX",tax.multiply(BigDecimal.valueOf(-sign)),null,h);
                for(Map<String,Object> d:s.getDetails()) {
                    BigDecimal cost=stock.get(longValue(d,"id")).multiply(BigDecimal.valueOf(sign));
                    ledger.detail("COST",cost,d); ledger.detail("INVENTORY",cost.negate(),d);
                }
            }
        } else if(s.getType()==16||s.getType()==17) {
            require("NOT_APPLICABLE".equals(c.getDeductionStatus()),"待补充：请确认无进项转出、福利赠送或视同销售事项");
            Map<Long,BigDecimal> costs=stockCosts(s); preview.setStockAmount(sum(costs.values()));
            require(preview.getStockAmount().compareTo(amount.abs())==0,"需人工处理：单据金额与库存流水不一致");
            for(Map<String,Object> d:s.getDetails()) {
                BigDecimal cost=costs.get(longValue(d,"id")); if(s.getType()==16) cost=cost.negate();
                ledger.detail("INVENTORY",cost,d);
                ledger.detail(scene.startsWith("STOCK_")?"PENDING":"EXPENSE",cost.negate(),d);
            }
        } else if(s.getType()==18) {
            require(BigDecimal.ONE.compareTo(decimal(h,"exchangeRate"))==0,"需人工处理：第一版仅支持人民币、汇率为1的转账");
            require("CNY".equals(c.getPriceBasis()),"待补充：请确认人民币转账");
            BigDecimal fee=money(optional(h,"feePrice"));
            require(amount.signum()>0&&fee.signum()>=0,"转账金额或手续费无效");
            require(!Objects.equals(h.get("inAccountId"),h.get("outAccountId")),"转入转出账户不能相同");
            ledger.fund(longValue(h,"inAccountId"),amount); ledger.fund(longValue(h,"outAccountId"),amount.add(fee).negate());
            ledger.line("FINANCE_EXPENSE",fee,null,h);
        } else if("RECEIPT".equals(scene)||"PAYMENT".equals(scene)) {
            BigDecimal total=money(decimal(h,"totalPrice"));
            require(discount.signum()>=0&&money(total.subtract(discount)).compareTo(amount)==0,"收付款金额与原单合计不一致");
            if(discount.signum()!=0) require("CASH".equals(c.getDiscountNature())&&total.signum()>0,"待补充：请确认现金折扣；退款折让转人工处理");
            boolean receipt="RECEIPT".equals(scene);
            ledger.fund(longValue(h,"accountId"),receipt?amount:amount.negate());
            ledger.line(receipt?"AR":"AP",receipt?total.negate():total,null,h);
            ledger.line("FINANCE_EXPENSE",receipt?discount:discount.negate(),null,h);
        } else if("EXPENSE_ACCRUAL".equals(scene)) {
            require(discount.signum()==0,"需人工处理：费用应付优惠");
            BigDecimal tax=tax(c,cfg,scene);
            require("INCLUSIVE".equals(c.getPriceBasis()),"费用应付须按原单价税合计录入");
            boolean deductible="GENERAL".equals(cfg.getTaxpayer())&&"DEDUCTIBLE".equals(c.getDeductionStatus());
            require(amount.compareTo(tax)>=0,"税额超过费用金额");
            ledger.line("EXPENSE",deductible?amount.subtract(tax):amount,null,h);
            if(deductible) ledger.line("INPUT_TAX",tax,null,h);
            ledger.line("OTHER_AP",amount.negate(),null,h);
        } else {
            require(discount.signum()==0,"需人工处理：预收预付、借款押金不能将优惠直接冲减本金");
            String role; int fundSign;
            switch(scene) {
                case "PRE_RECEIPT":case "PRE_RECEIVABLE":case "RECEIPT_ADVANCE": role="ADVANCE_RECEIPT";fundSign=1;break;
                case "PRE_PAYMENT":case "PAYMENT_ADVANCE":role="PREPAID";fundSign=-1;break;
                case "OTHER_AR_CREATE":role="OTHER_AR";fundSign=-1;break;
                case "OTHER_AR_RECOVER":role="OTHER_AR";fundSign=1;break;
                case "OTHER_AP_CREATE":role="OTHER_AP";fundSign=1;break;
                case "OTHER_AP_REPAY":role="OTHER_AP";fundSign=-1;break;
                default:throw problem("需人工处理：无适用模板");
            }
            BigDecimal fund=amount.multiply(BigDecimal.valueOf(fundSign));
            ledger.fund(longValue(h,"accountId"),fund);ledger.line(role,fund.negate(),null,h);
        }
        require(!result.isEmpty(),"需人工处理：无可生成的非零分录");
        BigDecimal debit=BigDecimal.ZERO,credit=BigDecimal.ZERO;int i=1;
        for(ErpVoucherItemDO item:result) {item.setLineNo(i++);debit=debit.add(item.getDebitAmount());credit=credit.add(item.getCreditAmount());}
        require(debit.compareTo(credit)==0,"借贷不平衡，禁止生成：借 "+debit+"，贷 "+credit);
        return result;
    }
    private BigDecimal tax(Context c,Config cfg,String scene) {
        require(Arrays.asList("INCLUSIVE","EXCLUSIVE").contains(c.getPriceBasis()),"待补充：请确认原单含税口径");
        require(Arrays.asList("RECEIVED","NOT_RECEIVED","EXEMPT").contains(c.getInvoiceStatus()),"待补充：请确认发票/免税状态");
        require(Arrays.asList("DEDUCTIBLE","NON_DEDUCTIBLE","NOT_APPLICABLE").contains(c.getDeductionStatus()),"待补充：请确认抵扣状态");
        require(c.getTaxAmount()!=null && c.getTaxAmount().signum()>=0,"待补充：请明确税额，不能依据原单零值推断");
        if("SMALL".equals(cfg.getTaxpayer())) require(!"DEDUCTIBLE".equals(c.getDeductionStatus()),"小规模纳税人不能配置可抵扣进项");
        if("DEDUCTIBLE".equals(c.getDeductionStatus())) require("RECEIVED".equals(c.getInvoiceStatus()),"确认可抵扣进项需要到票依据");
        if("EXEMPT".equals(c.getInvoiceStatus())||"PROVISIONAL".equals(scene)) require(c.getTaxAmount().signum()==0,"免税或暂估场景税额须为零");
        return money(c.getTaxAmount());
    }
    public static Map<Long,BigDecimal> stockCosts(Source s) {
        require(!s.getDetails().isEmpty()&&!s.getStocks().isEmpty(),"需人工处理：缺少库存明细或成本流水");
        Map<Long,BigDecimal> amount=new LinkedHashMap<>(),counts=new HashMap<>();
        for(Map<String,Object> r:s.getStocks()) {
            Long item=longValue(r,"bizItemId"); require(item!=null,"需人工处理：库存流水无来源明细");
            BigDecimal count=decimal(r,"count");
            BigDecimal cost=r.get("totalPrice")!=null?decimal(r,"totalPrice"):decimal(r,"unitPrice").multiply(count);
            // 流水金额必须带方向，不能 abs 后将原审核及反审核累计。
            require(cost.signum()==0||cost.signum()==count.signum(),"需人工处理：库存流水金额方向异常");
            amount.merge(item,cost,BigDecimal::add);counts.merge(item,count,BigDecimal::add);
        }
        int sign=Arrays.asList(2,9,16).contains(s.getType())?-1:1;
        Map<Long,BigDecimal> result=new LinkedHashMap<>();
        for(Map<String,Object> d:s.getDetails()) {
            Long id=longValue(d,"id");BigDecimal count=decimal(d,"count").multiply(BigDecimal.valueOf(sign));
            require(counts.containsKey(id)&&counts.get(id).compareTo(count)==0,"需人工处理：库存流水数量与单据明细不一致："+id);
            BigDecimal cost=money(amount.get(id).multiply(BigDecimal.valueOf(sign)));
            require(cost.signum()>=0,"需人工处理：库存成本方向异常");result.put(id,cost);
        }
        for(Long id:amount.keySet()) require(result.containsKey(id)||amount.get(id).signum()==0,"需人工处理：库存流水含无法匹配的明细");
        return result;
    }
    private Map<Long,BigDecimal> weights(Source s,Context c) {
        Map<Long,BigDecimal> weights=new LinkedHashMap<>();
        for(Map<String,Object> d:s.getDetails()) {
            BigDecimal value=d.get("totalPrice")!=null?decimal(d,"totalPrice"):decimal(d,"productPrice").multiply(decimal(d,"count"));
            weights.put(longValue(d,"id"),value.abs());
        }
        if(sum(weights.values()).signum()==0 && c.getAllocationWeights()!=null && !c.getAllocationWeights().isEmpty()) {
            require(c.getAllocationWeights()!=null&&c.getAllocationWeights().keySet().equals(weights.keySet()),"待补充：零金额明细请明确逐行分摊权重");
            weights.replaceAll((id,value)->c.getAllocationWeights().get(id));
        }
        require(weights.values().stream().allMatch(v->v!=null&&v.signum()>=0),"分摊权重无效"); return weights;
    }
    public static Map<Long,BigDecimal> allocate(BigDecimal total,Map<Long,BigDecimal> weights) {
        Map<Long,BigDecimal> result=new LinkedHashMap<>();BigDecimal sum=sum(weights.values()), remaining=money(total);
        if(total.signum()==0) {weights.keySet().forEach(id->result.put(id,BigDecimal.ZERO));return result;}
        require(sum.signum()>0,"待补充：缺少金额分摊依据");
        List<Long> positive=new ArrayList<>();weights.forEach((id,v)->{if(v.signum()>0) positive.add(id);});
        for(int i=0;i<positive.size();i++) {
            Long id=positive.get(i);BigDecimal share=i==positive.size()-1?remaining:total.multiply(weights.get(id)).divide(sum,2,RoundingMode.HALF_UP);
            if(share.abs().compareTo(remaining.abs())>0) share=remaining;
            result.put(id,share);remaining=remaining.subtract(share);
        }
        return result;
    }
    private static BigDecimal sum(Collection<BigDecimal> values) {return values.stream().reduce(BigDecimal.ZERO,BigDecimal::add);}
    private class Ledger {
        final Source source;final Context context;final Config config;final List<ErpVoucherItemDO> lines;final List<Map<String,Object>> matches=new ArrayList<>();
        Ledger(Source s,Context c,Config f,List<ErpVoucherItemDO> l,Preview p){source=s;context=c;config=f;lines=l;p.getEvidence().put("matches",matches);}
        void distribute(String role,BigDecimal value,Map<Long,BigDecimal> weights) {
            if(value.signum()==0)return;
            Map<Long,BigDecimal> allocated=allocate(value,weights);
            for(Map<String,Object> d:source.getDetails()) detail(role,allocated.getOrDefault(longValue(d,"id"),BigDecimal.ZERO),d);
        }
        void detail(String role,BigDecimal value,Map<String,Object> d) {
            Map<String,Object> h=new TreeMap<>(source.getHeader()); if(d.get("deptId")!=null) h.put("deptId",d.get("deptId"));
            h.put("accountingDetailId",d.get("id"));
            line(role,value,source.getCategories().get(longValue(d,"productId")),h);
        }
        void line(String role,BigDecimal value,Long category,Map<String,Object> header) {
            if(money(value).signum()==0)return;
            Long sourceCategory=category; List<Long> path=new ArrayList<>();
            Mapping match=null; Set<Long> seen=new HashSet<>();
            while(category!=null&&category!=0) {
                require(seen.add(category),"配件类别存在循环层级");final Long current=category;path.add(current);
                match=config.getMappings().stream().filter(m->context.getScenario().equals(m.getScenario())&&role.equals(m.getRole())&&current.equals(m.getCategoryId())).findFirst().orElse(null);
                if(match!=null)break;
                category=longValue(reader.one("erpProductCategoryMapper",category),"parentId");
            }
            if(match==null) match=config.getMappings().stream().filter(m->context.getScenario().equals(m.getScenario())&&role.equals(m.getRole())&&m.getCategoryId()==null).findFirst().orElse(null);
            require(match!=null,"未配置：缺少科目映射 "+role);
            Map<String,Object> evidence=new LinkedHashMap<>();evidence.put("role",role);evidence.put("detailId",header.get("accountingDetailId"));evidence.put("sourceCategoryId",sourceCategory);evidence.put("categoryPath",path);evidence.put("matchedCategoryId",match.getCategoryId());evidence.put("subjectId",match.getSubjectId());evidence.put("signedAmount",money(value));matches.add(evidence);
            add(match.getSubjectId(),value,role,header);
        }
        void fund(Long account,BigDecimal value) {
            if(money(value).signum()==0)return;
            Map<String,Object> a=reader.one("erpAccountMapper",account);
            require(Integer.valueOf(0).equals(a.get("status")),"结算账户已停用");
            AccountMapping mapping=config.getAccounts().stream().filter(m->Objects.equals(account,m.getAccountId())).findFirst().orElse(null);
            require(mapping!=null,"未配置：结算账户未绑定会计科目："+account);
            Map<String,Object> evidence=new LinkedHashMap<>();evidence.put("role","FUND");evidence.put("accountId",account);evidence.put("subjectId",mapping.getSubjectId());evidence.put("signedAmount",money(value));matches.add(evidence);
            add(mapping.getSubjectId(),value,"实际资金 / "+a.get("name"),source.getHeader());
        }
        void add(Long subjectId,BigDecimal value,String role,Map<String,Object> header) {
            ErpAccountingSubjectDO subject=store.subject(subjectId);ErpVoucherItemDO item=new ErpVoucherItemDO();
            item.setSubjectId(subjectId);item.setSubjectCode(subject.getSubjectCode());item.setSubjectName(subject.getSubjectName());
            item.setSummary(source.no()+" / "+role);item.setDebitAmount(value.signum()>0?money(value):BigDecimal.ZERO);item.setCreditAmount(value.signum()<0?money(value.negate()):BigDecimal.ZERO);
            auxiliaries.fill(item,header,context.getAuxiliaries());lines.add(item);
        }
    }
}
