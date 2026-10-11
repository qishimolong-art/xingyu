package cn.iocoder.yudao.module.erp.service.assistant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 公司知识问题的统一语义框架。
 *
 * <p>该类只判断“用户在问什么”，不读取知识正文、业务数据或权限。公司知识、ERP 实时问数和
 * 混合问答都以这里确认的主题、操作和实时证据为边界，避免由“销售、产品、区域、品牌”等单个词
 * 直接决定工具。</p>
 */
final class AssistantCompanyKnowledgeIntent {

    enum Topic {
        NONE, PROFILE, HISTORY, ADVANTAGE, RELATIONSHIP, BRAND, PRODUCT, CHANNEL, REGION,
        ORGANIZATION, PEOPLE, RESPONSIBILITY, CUSTOMER, TRAINING, STRATEGY, METHOD
    }

    enum Operation {
        NONE, INTRODUCE, LIST, COMPARE, RELATIONSHIP, REASON, METHOD, SCOPE, RESPONSIBILITY, FOLLOW_UP
    }

    enum Freshness {
        UNSPECIFIED, LATEST_REVIEWED, REALTIME_REQUIRED
    }

    static final class Analysis {
        private final String normalizedQuestion;
        private final String subject;
        private final Topic topic;
        private final Operation operation;
        private final Freshness freshness;
        private final List<String> entities;
        private final boolean companySubject;
        private final boolean explicitRealtime;
        private final boolean knowledge;
        private final double confidence;
        private final String reason;

        private Analysis(String normalizedQuestion, String subject, Topic topic, Operation operation,
                         Freshness freshness, List<String> entities, boolean companySubject,
                         boolean explicitRealtime, boolean knowledge, double confidence, String reason) {
            this.normalizedQuestion = normalizedQuestion;
            this.subject = subject;
            this.topic = topic;
            this.operation = operation;
            this.freshness = freshness;
            this.entities = entities;
            this.companySubject = companySubject;
            this.explicitRealtime = explicitRealtime;
            this.knowledge = knowledge;
            this.confidence = confidence;
            this.reason = reason;
        }

        String getNormalizedQuestion() { return normalizedQuestion; }
        String getSubject() { return subject; }
        Topic getTopic() { return topic; }
        Operation getOperation() { return operation; }
        Freshness getFreshness() { return freshness; }
        List<String> getEntities() { return entities; }
        boolean hasCompanySubject() { return companySubject; }
        boolean isExplicitRealtime() { return explicitRealtime; }
        boolean isKnowledge() { return knowledge; }
        double getConfidence() { return confidence; }
        String getReason() { return reason; }

        Map<String, Object> toAuditMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("subject", subject);
            result.put("topic", topic.name());
            result.put("operation", operation.name());
            result.put("freshness", freshness.name());
            result.put("entities", new ArrayList<>(entities));
            result.put("confidence", confidence);
            result.put("reason", reason);
            result.put("explicitRealtime", explicitRealtime);
            return result;
        }
    }

    private AssistantCompanyKnowledgeIntent() {
    }

    static Analysis analyze(String question) {
        String text = compact(question);
        if (text.isEmpty()) return none(text, false, false);

        boolean companySubject = hasCompanySubject(text);
        boolean explicitRealtime = hasExplicitRealtimeEvidence(text);
        Topic topic = topic(text, companySubject);
        Operation operation = operation(text, topic);
        boolean standaloneKnowledge = isStandaloneKnowledgeTopic(text, topic);
        boolean knowledge = !explicitRealtime && topic != Topic.NONE
                && (companySubject || standaloneKnowledge || isImplicitCompanyQuestion(text, topic));
        Freshness freshness = explicitRealtime ? Freshness.REALTIME_REQUIRED
                : containsAny(text, "现在", "目前", "当前", "现阶段", "如今")
                ? Freshness.LATEST_REVIEWED : Freshness.UNSPECIFIED;
        double confidence = knowledge ? confidence(topic, operation, companySubject) : 0D;
        String reason = knowledge ? reason(topic) : explicitRealtime
                ? "EXPLICIT_ERP_DATA_EVIDENCE" : "NO_HIGH_CONFIDENCE_COMPANY_KNOWLEDGE_INTENT";
        List<String> entities = topic == Topic.RELATIONSHIP
                ? singletonIfPresent(relationshipCounterparty(text)) : Collections.<String>emptyList();
        return new Analysis(normalizeSelfReference(text), companySubject ? "四川兴宇" : null,
                topic, operation, freshness, entities, companySubject, explicitRealtime,
                knowledge, confidence, reason);
    }

    static String normalizeSelfReference(String question) {
        String text = compact(question);
        return text.replaceAll("(?:我们公司|咱们公司|本公司|我们|咱们)", "四川兴宇");
    }

    static String relationshipCounterparty(String question) {
        Analysis analysis = analyzeWithoutEntities(question);
        if (analysis.topic != Topic.RELATIONSHIP || analysis.explicitRealtime) return null;
        String value = normalizeSelfReference(question)
                .replaceAll("(?:四川兴宇科技服务有限公司|四川兴宇|兴宇公司|兴宇)", "");
        value = value.replaceAll("(?:合作多长时间|合作了多久|合作多久|是什么样的关系|是什么关系|什么关系|有何关系|"
                + "合作关系|合作模式|代理关系|经销关系|授权关系|合作伙伴|代理商|经销商|"
                + "怎么合作|如何合作|怎样合作|是否合作|有没有合作|有无合作|到底|究竟|还是|之间|"
                + "怎么|如何|怎样|是否|有没有|有无|什么|与|和|跟|同|是|的|关系|合作|代理|经销|授权)", "");
        value = value.replaceAll("[^\\p{IsHan}a-z0-9+./-]", "");
        if (value.matches("(?:品牌|厂家|厂商|渠道|合作方|伙伴|对方|供应商|客户)")) return null;
        return value.length() >= 2 && value.length() <= 40 ? value : null;
    }

    private static Analysis analyzeWithoutEntities(String question) {
        String text = compact(question);
        boolean companySubject = hasCompanySubject(text);
        boolean explicitRealtime = hasExplicitRealtimeEvidence(text);
        Topic topic = topic(text, companySubject);
        Operation operation = operation(text, topic);
        boolean knowledge = !explicitRealtime && topic != Topic.NONE
                && (companySubject || isStandaloneKnowledgeTopic(text, topic)
                || isImplicitCompanyQuestion(text, topic));
        Freshness freshness = explicitRealtime ? Freshness.REALTIME_REQUIRED
                : containsAny(text, "现在", "目前", "当前", "现阶段", "如今")
                ? Freshness.LATEST_REVIEWED : Freshness.UNSPECIFIED;
        return new Analysis(normalizeSelfReference(text), companySubject ? "四川兴宇" : null,
                topic, operation, freshness, Collections.<String>emptyList(), companySubject,
                explicitRealtime, knowledge, knowledge ? confidence(topic, operation, companySubject) : 0D,
                knowledge ? reason(topic) : "NO_HIGH_CONFIDENCE_COMPANY_KNOWLEDGE_INTENT");
    }

    private static Topic topic(String text, boolean companySubject) {
        if (isRelationship(text, companySubject)) return Topic.RELATIONSHIP;
        if (isAdvantage(text)) return Topic.ADVANTAGE;
        if (isChannel(text)) return Topic.CHANNEL;
        if (isRegion(text)) return Topic.REGION;
        if (isResponsibility(text)) return Topic.RESPONSIBILITY;
        if (isPeople(text)) return Topic.PEOPLE;
        if (isOrganization(text)) return Topic.ORGANIZATION;
        if (isHistory(text)) return Topic.HISTORY;
        if (isTraining(text)) return Topic.TRAINING;
        if (isStrategy(text)) return Topic.STRATEGY;
        if (isCustomerKnowledge(text)) return Topic.CUSTOMER;
        if (isBrandKnowledge(text)) return Topic.BRAND;
        if (isProductKnowledge(text, companySubject)) return Topic.PRODUCT;
        if (isMethodKnowledge(text)) return Topic.METHOD;
        if (isProfile(text)) return Topic.PROFILE;
        // 组织、人事和负责人已有更细的确定性语义识别；在这里统一收口，避免编排层
        // 再维护一套不完整的正则。实时组织问题已在前置实时证据判断中排除。
        if (AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(text)) {
            AssistantKnowledgeContext.OrganizationIntent intent = AssistantKnowledgeContext.organizationIntent(text);
            if (intent == AssistantKnowledgeContext.OrganizationIntent.PEOPLE
                    || intent == AssistantKnowledgeContext.OrganizationIntent.COMPANY_LEADERSHIP
                    || intent == AssistantKnowledgeContext.OrganizationIntent.PERSON_WORK) {
                return Topic.PEOPLE;
            }
            if (intent == AssistantKnowledgeContext.OrganizationIntent.RESPONSIBILITY
                    || intent == AssistantKnowledgeContext.OrganizationIntent.DUTY
                    || intent == AssistantKnowledgeContext.OrganizationIntent.COLLABORATION
                    || intent == AssistantKnowledgeContext.OrganizationIntent.WORK_OWNER) {
                return Topic.RESPONSIBILITY;
            }
            if (intent == AssistantKnowledgeContext.OrganizationIntent.HISTORY) return Topic.HISTORY;
            if (intent == AssistantKnowledgeContext.OrganizationIntent.REGION) return Topic.REGION;
            return Topic.ORGANIZATION;
        }
        return Topic.NONE;
    }

    private static Operation operation(String text, Topic topic) {
        if (topic == Topic.RELATIONSHIP) return Operation.RELATIONSHIP;
        if (topic == Topic.ADVANTAGE || containsAny(text, "比较", "相比", "对比", "区别", "不同")) {
            return Operation.COMPARE;
        }
        if (topic == Topic.RESPONSIBILITY) return Operation.RESPONSIBILITY;
        if (topic == Topic.REGION || containsAny(text, "范围", "覆盖", "布局", "分布")) return Operation.SCOPE;
        if (containsAny(text, "为什么", "为何", "原因")) return Operation.REASON;
        if (containsAny(text, "怎么", "如何", "怎样", "通过什么", "通过哪些", "方式", "模式")) {
            return Operation.METHOD;
        }
        if (containsAny(text, "哪些", "哪一些", "有什么", "都有", "包括", "列出", "几类", "类型")) {
            return Operation.LIST;
        }
        return Operation.INTRODUCE;
    }

    private static boolean hasCompanySubject(String text) {
        return containsAny(text, "四川兴宇", "兴宇公司", "兴宇", "我们公司", "咱们公司", "本公司", "公司", "集团",
                "我们", "咱们");
    }

    /** “现在/目前”本身不是实时数据证据；必须同时存在可执行的数据诉求。 */
    private static boolean hasExplicitRealtimeEvidence(String text) {
        if (AssistantKnowledgeContext.isRealtimeOrganizationKnowledgeBoundary(text)) return true;
        if (text.matches(".*(?:价格贵|太贵|贵了|嫌贵).*(?:怎么|如何|沟通|回答|回应|解释|处理|说).*")) {
            return false;
        }
        // 部门清单是已接入的 ERP 主数据能力。“当前/现在/最新有哪些部门”必须继续
        // 查询实时组织，而“现在覆盖哪些区域”仍使用最新已审核的公司知识。
        if (containsAny(text, "当前", "现在", "目前", "实时", "最新", "系统里", "erp里")
                && text.matches(".*(?:有哪些部门|哪些部门|部门有哪些|部门列表|启用部门|停用部门|组织架构|组织结构).*")) {
            return true;
        }
        if (containsAny(text, "当前库存", "实时库存", "账面库存", "库存多少", "库存情况", "库存数量",
                "价格", "报价", "售价", "采购价", "销售额", "销售量", "销量", "采购额", "采购金额",
                "收款", "付款", "应收", "应付", "欠款", "余额", "订单", "单据", "报表", "出库", "入库",
                "退货", "调价", "流水", "排名", "排行", "趋势", "明细", "记录")) return true;
        if (text.matches(".*(?:本日|今天|昨日|昨天|本周|上周|本月|上月|本年|今年|去年|\\d{1,2}月|\\d{1,2}[日号]).*"
                + ".*(?:销售|采购|库存|客户|供应商|收款|付款|金额|数量|多少).*")) return true;
        if (text.matches(".*(?:统计|汇总|查询|查一下|查看).*(?:金额|数量|客户|供应商|产品|配件|销售|采购|库存).*")) {
            return true;
        }
        if (text.matches(".*(?:客户|供应商|产品|配件|员工|人员).*(?:有多少|多少个|几个|数量|总数|多少种).*")) {
            return true;
        }
        if (text.matches(".*(?:有多少|多少个|几个|数量|总数|多少种).*(?:客户|供应商|产品|配件|员工|人员).*")) {
            return true;
        }
        return containsAny(text, "erp里", "系统里", "数据库里", "实时数据", "截至今天")
                && containsAny(text, "多少", "哪些客户", "哪些订单", "状态", "金额", "数量", "库存", "价格");
    }

    private static boolean isRelationship(String text, boolean companySubject) {
        if (!companySubject) return false;
        return containsAny(text, "合作关系", "合作模式", "代理关系", "经销关系", "授权关系", "合作伙伴",
                "代理商", "经销商")
                || text.matches(".*(?:是什么|什么|有何)(?:样的)?关系.*")
                || text.matches(".*(?:怎么|如何|怎样).{0,20}合作.*")
                || text.matches(".*(?:是否|有没有|有无).{0,12}合作.*")
                || text.matches(".*合作(?:了)?(?:多久|多长时间).*");
    }

    private static boolean isAdvantage(String text) {
        return containsAny(text, "核心优势", "竞争优势", "差异化优势", "我们的优势", "公司优势", "强项", "长处")
                || text.matches(".*(?:同行|竞争对手|其他公司|别家公司).*(?:相比|比较|比).*(?:好|强|优势|领先).*" )
                || text.matches(".*(?:哪里|哪些方面|什么地方).*(?:做得更好|更有优势|更强|领先).*" );
    }

    private static boolean isChannel(String text) {
        return containsAny(text, "渠道网络", "销售渠道", "销售路径", "经销方式", "分销方式", "渠道模式", "门店体系",
                "终端网络")
                || text.matches(".*(?:通过|依靠|利用).{0,8}(?:哪些|什么|哪种)?渠道.*")
                || text.matches(".*(?:产品|商品|配件).{0,8}(?:怎么|如何|怎样|通过什么|通过哪些).*(?:卖出|卖给|销售).*" )
                || text.matches(".*(?:怎么|如何|怎样|通过什么|通过哪些).{0,12}(?:把)?(?:产品|商品|配件).*(?:卖出|卖给|销售).*" );
    }

    private static boolean isRegion(String text) {
        return containsAny(text, "区域市场格局", "市场布局", "业务布局", "地区布局", "区域布局", "服务区域", "经营区域")
                || text.matches(".*(?:业务|市场|公司|兴宇).*(?:覆盖|辐射).*(?:区域|地区|城市|哪里|范围).*" )
                || text.matches(".*(?:区域|地区|城市|哪里|范围).*(?:业务|市场).*(?:覆盖|布局|开展).*" );
    }

    private static boolean isOrganization(String text) {
        return containsAny(text, "组织架构", "组织结构", "公司架构", "集团架构", "部门架构", "有哪些部门", "哪些部门",
                "部门有哪些", "部门设置", "部门构成", "部门组成", "销售部门", "财务部", "总经办", "事业部",
                "分公司", "办事处");
    }

    private static boolean isPeople(String text) {
        return text.matches(".*(?:部门|总经办|事业部|分公司|办事处|团队|小组|部|办|组).*(?:有哪些人|有谁|成员|人员|负责人|谁负责).*" )
                || text.matches(".*(?:负责人|人员|成员).*(?:是谁|有哪些|有谁).*" );
    }

    private static boolean isResponsibility(String text) {
        return text.matches(".*(?:部门|总经办|事业部|分公司|办事处|团队|小组|部|办|组).*(?:职责|负责什么|主要负责|职能|工作范围|如何分工|怎么分工|协作|配合).*" )
                || text.matches(".*(?:职责|职能|分工|协作).*(?:部门|团队|人员).*" );
    }

    private static boolean isHistory(String text) {
        return containsAny(text, "发展历程", "公司历史", "发展历史", "历史沿革", "怎么发展", "如何发展", "成立多久",
                "成立了多久", "创立多久", "创立了多久", "什么时候成立", "什么时候创立", "创立的", "发展起来");
    }

    private static boolean isTraining(String text) {
        return containsAny(text, "新员工", "新人", "入职培训", "培训路径", "学习路径", "公司制度", "业务知识",
                "应该先了解", "入门知识");
    }

    private static boolean isStrategy(String text) {
        return containsAny(text, "战略目标", "战略演变", "发展战略", "经营战略", "未来规划", "发展规划", "公司规划");
    }

    private static boolean isCustomerKnowledge(String text) {
        return containsAny(text, "客户类型", "客户群体", "目标客户", "客户画像", "服务对象", "主要客户")
                || text.matches(".*(?:服务|面向).*(?:哪些|什么|哪类|哪几类).*(?:客户|用户).*" )
                || text.matches(".*(?:客户|用户).*(?:类型|群体|画像|哪类|哪几类).*" );
    }

    private static boolean isBrandKnowledge(String text) {
        return containsAny(text, "品牌与产品线", "合作品牌", "代理品牌", "经营品牌", "品牌矩阵")
                || text.matches(".*(?:代理|经营|合作).*(?:哪些|什么).*(?:品牌).*" )
                || text.matches(".*(?:品牌).*(?:有哪些|有什么|包括|是什么).*" );
    }

    private static boolean isProductKnowledge(String text, boolean companySubject) {
        boolean technical = containsAny(text, "产品知识", "产品选型", "如何选油", "怎么选油", "机油标准", "认证", "标准",
                "规格", "性能", "适用范围", "卖点", "用途", "产品线", "产品地图", "api sp", "ilsac", "acea");
        boolean companyProducts = companySubject && (text.matches(".*(?:主要|主营|经营|代理).*(?:产品|商品|品类).*" )
                || text.matches(".*(?:产品|商品|品类).*(?:有哪些|有什么|包括).*" ));
        return technical || companyProducts;
    }

    private static boolean isMethodKnowledge(String text) {
        return containsAny(text, "经营方法", "运营方法", "经营模式", "运营模式", "ddc", "五步赋能", "标杆客户体系",
                "经营分析方法", "销售话术", "客户异议", "向客户介绍公司", "介绍公司时", "重点讲哪些")
                || text.matches(".*(?:客户|顾客)?.*(?:价格贵|太贵|贵了|嫌贵).*(?:怎么|如何|沟通|回答|回应|解释|处理|说).*" );
    }

    private static boolean isProfile(String text) {
        return containsAny(text, "公司介绍", "公司概况", "公司简介", "主营业务", "主要业务", "业务范围", "经营范围",
                "公司定位", "公司知识待核实", "待核实内容", "待核实事项", "什么公司")
                || hasCompanySubject(text) && text.matches(".*(?:公司|兴宇|我们|咱们).*(?:做什么|是做什么的).*" )
                || text.matches(".*(?:主要|主营|重点)(?:经营|开展|从事)(?:哪些|什么)?业务.*")
                || text.matches(".*(?:介绍|讲讲|说说|了解).{0,8}(?:四川兴宇|兴宇公司|兴宇|我们公司|咱们公司|本公司|公司).*" );
    }

    private static boolean isStandaloneKnowledgeTopic(String text, Topic topic) {
        if (topic == Topic.TRAINING || topic == Topic.METHOD) return true;
        if (topic == Topic.PRODUCT) {
            return containsAny(text, "认证", "标准", "规格", "性能", "适用范围", "卖点", "用途", "如何选油", "怎么选油", "产品选型",
                    "api sp", "ilsac", "acea");
        }
        return false;
    }

    private static boolean isImplicitCompanyQuestion(String text, Topic topic) {
        if (topic == Topic.ORGANIZATION || topic == Topic.PEOPLE || topic == Topic.RESPONSIBILITY) return true;
        if (topic == Topic.REGION && AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(text)) return true;
        return text.matches(".*(?:公司|兴宇|我们|咱们).*" );
    }

    private static double confidence(Topic topic, Operation operation, boolean companySubject) {
        if (topic == Topic.RELATIONSHIP || topic == Topic.CHANNEL || topic == Topic.REGION || topic == Topic.ADVANTAGE) {
            return 0.99D;
        }
        if (topic == Topic.ORGANIZATION || topic == Topic.PEOPLE || topic == Topic.RESPONSIBILITY) return 0.98D;
        if (companySubject && operation != Operation.NONE) return 0.97D;
        return 0.95D;
    }

    private static String reason(Topic topic) {
        // 保持历史审计字段和调用方断言兼容。
        if (topic == Topic.RELATIONSHIP) return "COMPANY_PARTNER_RELATIONSHIP";
        return "COMPANY_KNOWLEDGE_" + topic.name();
    }

    private static Analysis none(String text, boolean companySubject, boolean explicitRealtime) {
        return new Analysis(text, companySubject ? "四川兴宇" : null, Topic.NONE, Operation.NONE,
                explicitRealtime ? Freshness.REALTIME_REQUIRED : Freshness.UNSPECIFIED,
                Collections.<String>emptyList(), companySubject, explicitRealtime, false, 0D,
                explicitRealtime ? "EXPLICIT_ERP_DATA_EVIDENCE" : "NO_HIGH_CONFIDENCE_COMPANY_KNOWLEDGE_INTENT");
    }

    private static List<String> singletonIfPresent(String value) {
        if (value == null || value.trim().isEmpty()) return Collections.emptyList();
        return Collections.singletonList(value);
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private static String compact(String question) {
        return question == null ? "" : question.replaceAll("[\\s，。！？、,.!?；;：:]", "")
                .toLowerCase(Locale.ROOT);
    }
}
