package cn.iocoder.yudao.module.erp.service.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 公司知识连续追问的受控上下文判定与检索问题拼接。 */
final class AssistantKnowledgeContext {

    static final String CONTEXT_ARGUMENT = "knowledgeContextQuestion";
    static final String KNOWLEDGE_FRAME_ARGUMENT = "knowledgeFrame";
    static final String ORGANIZATION_CONTEXT_ARGUMENT = "organizationContext";
    static final String CONTEXTUAL_REALTIME_BOUNDARY_ARGUMENT = "contextualRealtimeOrganizationBoundary";
    private static final int MAX_CONTEXT_LENGTH = 1000;

    enum OrganizationIntent {
        NONE, STRUCTURE, PEOPLE, RESPONSIBILITY, COMPANY_LEADERSHIP, DUTY, COLLABORATION, WORK_OWNER,
        PERSON_WORK, HISTORY, REGION
    }

    private AssistantKnowledgeContext() {
    }

    static boolean isFollowUp(String question, AssistantExecutionEnvelope previous) {
        if (!isKnowledge(previous) || question == null) return false;
        String text = compact(question);
        AssistantCompanyKnowledgeIntent.Analysis analysis = AssistantCompanyKnowledgeIntent.analyze(question);
        if (text.isEmpty() || analysis.isExplicitRealtime() || isBusinessMetricQuestion(text)) return false;
        if (isOrganizationFollowUp(text)) return isOrganizationKnowledge(previous);
        if (analysis.isKnowledge()) {
            return !analysis.hasCompanySubject() && hasKnowledgeContextReference(text);
        }
        return isGenericElaboration(text) || isTopicFollowUp(text, knowledgeFrame(previous));
    }

    static boolean isOrganizationKnowledgeQuestion(String question) {
        String text = compact(question);
        return !text.isEmpty() && !isRealtimeOrBusinessQuestion(text)
                && rawOrganizationIntent(text) != OrganizationIntent.NONE;
    }

    static boolean containsOrganizationLanguage(String question) {
        String text = compact(question);
        return rawOrganizationIntent(text) != OrganizationIntent.NONE
                || text.matches(".*(?:组织架构|组织结构|公司架构|部门架构|总经办|事业部|分公司|办事处|"
                + "[\\p{IsHan}a-z0-9]{1,24}(?:部门|中心|团队|小组|部|办|组)).*"
                + "(?:负责人|人员|成员|任职|岗位|分管|负责|职责|协作|分工).*" );
    }

    static OrganizationIntent organizationIntent(String question) {
        String text = compact(question);
        if (text.isEmpty() || isRealtimeOrBusinessQuestion(text)) return OrganizationIntent.NONE;
        return rawOrganizationIntent(text);
    }

    /**
     * Resolve the intent of the current turn.  The previous turn may prove that an
     * abbreviated sentence is an organization follow-up, but must never donate its
     * own intent (for example collaboration) to a complete current question.
     */
    static OrganizationIntent organizationIntent(String question, AssistantExecutionEnvelope previous) {
        String text = compact(question);
        if (text.isEmpty() || isRealtimeOrBusinessQuestion(text)) return OrganizationIntent.NONE;
        OrganizationIntent direct = rawOrganizationIntent(text);
        if (direct != OrganizationIntent.NONE) return direct;
        return isOrganizationKnowledge(previous) ? abbreviatedOrganizationIntent(text) : OrganizationIntent.NONE;
    }

    static boolean isRealtimeOrganizationResponsibleQuestion(String question) {
        String text = compact(question);
        return !text.isEmpty() && isRealtimeOrBusinessQuestion(text)
                && isSpecificOrganizationResponsibleQuestion(text);
    }

    static boolean isRealtimeOrganizationDutyQuestion(String question) {
        String text = compact(question);
        return !text.isEmpty() && isRealtimeOrBusinessQuestion(text)
                && isSpecificOrganizationDutyQuestion(text);
    }

    static boolean isRealtimeCompanyLeadershipQuestion(String question) {
        String text = compact(question);
        return !text.isEmpty() && hasRealtimeQualifier(text) && !isBusinessMetricQuestion(text)
                && isCompanyLeadership(withoutRealtimeQualifierAnywhere(text));
    }

    static boolean isRealtimeOrganizationKnowledgeBoundary(String question) {
        String text = compact(question);
        if (text.isEmpty() || !hasRealtimeQualifier(text) || isBusinessMetricQuestion(text)) return false;
        OrganizationIntent intent = rawOrganizationIntent(withoutRealtimeQualifier(text));
        return intent == OrganizationIntent.PEOPLE || intent == OrganizationIntent.RESPONSIBILITY
                || intent == OrganizationIntent.COMPANY_LEADERSHIP
                || intent == OrganizationIntent.DUTY
                || intent == OrganizationIntent.COLLABORATION || intent == OrganizationIntent.WORK_OWNER
                || intent == OrganizationIntent.PERSON_WORK;
    }

    static boolean isContextualRealtimeOrganizationBoundary(String question, AssistantExecutionEnvelope previous) {
        String text = compact(question);
        if (text.isEmpty() || !hasRealtimeQualifier(text) || isBusinessMetricQuestion(text)
                || isRealtimeOrganizationKnowledgeBoundary(text)) return false;
        String withoutRealtime = withoutRealtimeQualifierAnywhere(text);
        OrganizationIntent abbreviated = abbreviatedOrganizationIntent(withoutRealtime);
        if (abbreviated == OrganizationIntent.RESPONSIBILITY) return isOrganizationKnowledge(previous);
        return isPersonPronounWorkQuestion(withoutRealtime) && !organizationPeople(previous).isEmpty();
    }

    static String personReferenceClarification(String question, AssistantExecutionEnvelope previous) {
        String text = compact(question);
        if (text.isEmpty() || hasRealtimeQualifier(text) || !isSingularPersonPronounWorkQuestion(text)) return null;
        List<String> people = organizationPeople(previous);
        if (people.size() == 1) return null;
        if (people.size() > 1) {
            return "“他/她”可能指“" + String.join("、", people) + "”，请说明具体人员";
        }
        return "请先说明“他/她”具体指哪位人员";
    }

    static boolean isOrganizationCollaborationQuestion(String question) {
        return rawOrganizationIntent(compact(question)) == OrganizationIntent.COLLABORATION;
    }

    static boolean isOrganizationWorkOwnerQuestion(String question) {
        return rawOrganizationIntent(compact(question)) == OrganizationIntent.WORK_OWNER;
    }

    static boolean isOrganizationPersonWorkQuestion(String question) {
        return rawOrganizationIntent(compact(question)) == OrganizationIntent.PERSON_WORK;
    }

    static boolean isOrganizationHistoryQuestion(String question) {
        return rawOrganizationIntent(compact(question)) == OrganizationIntent.HISTORY;
    }

    static boolean isOrganizationRegionQuestion(String question) {
        return rawOrganizationIntent(compact(question)) == OrganizationIntent.REGION;
    }

    static boolean isCompanyLeadershipQuestion(String question) {
        return rawOrganizationIntent(compact(question)) == OrganizationIntent.COMPANY_LEADERSHIP;
    }

    static String companyLeadershipRole(String question) {
        String text = compact(question);
        if (text.contains("法定代表人")) return "法定代表人";
        if (text.contains("法人")) return "法人";
        if (text.contains("总经理")) return "总经理";
        if (text.contains("创始人")) return "创始人";
        if (text.contains("董事长")) return "董事长";
        if (text.matches(".*(?:老板|一把手|掌舵人|最高负责人|最高领导|公司负责人).*" )
                || text.matches(".*(?:四川兴宇|兴宇公司|兴宇|我们公司|咱们公司|本公司|公司)(?:的)?负责人.*")
                || text.matches(".*(?:谁负责(?:整个|这家)?(?:四川兴宇|兴宇公司|兴宇|我们公司|咱们公司|本公司|公司)|"
                + "(?:整个|这家)?(?:四川兴宇|兴宇公司|兴宇|我们公司|咱们公司|本公司|公司)(?:是)?(?:由)?谁(?:在|来)?负责).*")) {
            return "董事长";
        }
        return null;
    }

    static boolean isColloquialCompanyLeadershipQuestion(String question) {
        String text = compact(question);
        return isCompanyLeadershipQuestion(text) && !text.contains("董事长")
                && "董事长".equals(companyLeadershipRole(text));
    }

    static String contextualQuestion(String question, AssistantExecutionEnvelope previous) {
        String current = AssistantModelClient.redact(question == null ? "" : question.trim());
        if (!isFollowUp(question, previous)) return limit(current);
        String base = previousKnowledgeQuestion(previous);
        if (base == null || base.trim().isEmpty()) return limit(current);
        if (isPersonPronounWorkQuestion(compact(question))) {
            List<String> people = organizationPeople(previous);
            if (!people.isEmpty()) base = base.trim() + "；已确认负责人：" + String.join("、", people);
        }
        return limit(base.trim() + "；追问：" + current);
    }

    static boolean isPersonReferenceQuestion(String question) {
        return isPersonPronounWorkQuestion(compact(question));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> organizationContext(AssistantExecutionEnvelope previous) {
        if (previous == null || previous.getArguments() == null) return Collections.emptyMap();
        Object raw = previous.getArguments().get(ORGANIZATION_CONTEXT_ARGUMENT);
        if (!(raw instanceof Map)) return Collections.emptyMap();
        return new LinkedHashMap<>((Map<String, Object>) raw);
    }

    static List<String> organizationPeople(AssistantExecutionEnvelope previous) {
        Object raw = organizationContext(previous).get("people");
        if (!(raw instanceof Collection)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Object value : (Collection<?>) raw) {
            String person = value == null ? "" : String.valueOf(value).trim();
            if (!person.isEmpty() && !result.contains(person)) result.add(person);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> knowledgeFrame(AssistantExecutionEnvelope previous) {
        if (previous == null || previous.getArguments() == null) return Collections.emptyMap();
        Object raw = previous.getArguments().get(KNOWLEDGE_FRAME_ARGUMENT);
        if (!(raw instanceof Map)) return Collections.emptyMap();
        return new LinkedHashMap<>((Map<String, Object>) raw);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> knowledgeFrame(String question, Map<String, Object> result,
                                              AssistantExecutionEnvelope previous) {
        AssistantCompanyKnowledgeIntent.Analysis analysis = AssistantCompanyKnowledgeIntent.analyze(question);
        Map<String, Object> frame = new LinkedHashMap<>(analysis.toAuditMap());
        Map<String, Object> inherited = knowledgeFrame(previous);
        if (analysis.getTopic() == AssistantCompanyKnowledgeIntent.Topic.NONE && inherited.get("topic") != null) {
            frame.put("topic", inherited.get("topic"));
            frame.put("reason", "INHERITED_KNOWLEDGE_TOPIC");
        }
        if ((analysis.getSubject() == null || analysis.getSubject().isEmpty()) && inherited.get("subject") != null) {
            frame.put("subject", inherited.get("subject"));
        }
        if (analysis.getEntities().isEmpty() && inherited.get("entities") instanceof Collection) {
            frame.put("entities", new ArrayList<>((Collection<Object>) inherited.get("entities")));
        }
        List<String> sourceIds = new ArrayList<>();
        if (result != null && result.get("sources") instanceof Collection) {
            for (Object value : (Collection<?>) result.get("sources")) {
                if (!(value instanceof Map)) continue;
                Object id = ((Map<?, ?>) value).get("id");
                if (id != null && !sourceIds.contains(String.valueOf(id))) sourceIds.add(String.valueOf(id));
            }
        }
        if (!sourceIds.isEmpty()) frame.put("sourceIds", sourceIds);
        else if (inherited.get("sourceIds") instanceof Collection) {
            frame.put("sourceIds", new ArrayList<>((Collection<Object>) inherited.get("sourceIds")));
        }
        return frame;
    }

    private static boolean isKnowledge(AssistantExecutionEnvelope previous) {
        return previous != null
                && previous.getRouteType() == AssistantExecutionEnvelope.RouteType.KNOWLEDGE
                && AssistantCompanyKnowledge.TOOL_NAME.equals(previous.getToolName());
    }

    private static boolean isOrganizationKnowledge(AssistantExecutionEnvelope previous) {
        if (!isKnowledge(previous)) return false;
        if (!organizationContext(previous).isEmpty()) return true;
        String previousQuestion = compact(previousKnowledgeQuestion(previous));
        return rawOrganizationIntent(previousQuestion) != OrganizationIntent.NONE
                || containsOrganizationLanguage(previousQuestion);
    }

    private static String previousKnowledgeQuestion(AssistantExecutionEnvelope previous) {
        Map<String, Object> arguments = previous.getArguments();
        Object value = arguments == null ? null : arguments.get(CONTEXT_ARGUMENT);
        if (value instanceof String && !((String) value).trim().isEmpty()) return (String) value;
        return previous.getQuestion();
    }

    private static boolean isOrganizationFollowUp(String text) {
        if (isNonOrganizationOwnerQuestion(text)) return false;
        return rawOrganizationIntent(text) != OrganizationIntent.NONE
                || abbreviatedOrganizationIntent(text) != OrganizationIntent.NONE
                || text.matches(".*(?:这些人|他们|上述人员|各负责人|这些部门|上述部门|各部门|它们).*"
                + "(?:分别|各自)?(?:负责什么|职责|分工|是谁|如何协作|怎么配合).*" );
    }

    private static OrganizationIntent rawOrganizationIntent(String text) {
        if (text == null || text.isEmpty() || isNonOrganizationOwnerQuestion(text)) return OrganizationIntent.NONE;
        if (isLeadershipHistory(text)) return OrganizationIntent.HISTORY;
        if (isCompanyLeadership(text)) return OrganizationIntent.COMPANY_LEADERSHIP;
        if (isSpecificOrganizationResponsibleQuestion(text)) return OrganizationIntent.RESPONSIBILITY;
        if (isSpecificOrganizationDutyQuestion(text)) return OrganizationIntent.DUTY;
        if (isSpecificOrganizationPeopleQuestion(text)) return OrganizationIntent.PEOPLE;
        if (isOrganizationCollaboration(text)) return OrganizationIntent.COLLABORATION;
        if (isWorkOwnerQuestion(text)) return OrganizationIntent.WORK_OWNER;
        if (isPersonWorkQuestion(text)) return OrganizationIntent.PERSON_WORK;
        if (isOrganizationHistory(text)) return OrganizationIntent.HISTORY;
        if (isOrganizationRegion(text)) return OrganizationIntent.REGION;
        if (isDepartmentKnowledgeQuestion(text)
                || text.matches(".*(?:组织架构|组织结构|公司架构|集团架构|部门架构).*")) {
            return OrganizationIntent.STRUCTURE;
        }
        if (text.matches(".*(?:组织负责人|部门负责人|人员组成|人员构成|成员组成|成员构成|具体人员|"
                + "领导层|管理层|部门分工|部门职责|职责分工|人员名单).*" )
                || text.matches(".*(?:这些部门|上述部门|各部门).*(?:负责人|人员|成员|分别是谁|各自负责).*" )
                || text.matches(".*(?:兴宇|公司).*(?:部门.*(?:和|及|与).*负责人|人员|成员).*")) {
            return OrganizationIntent.PEOPLE;
        }
        return OrganizationIntent.NONE;
    }

    private static OrganizationIntent abbreviatedOrganizationIntent(String text) {
        String prefix = "(?:那|那么|请问|想了解|我想知道)?";
        if (text.matches("^" + prefix + "(?:谁负责|谁是负责人|负责人是谁|由谁负责|"
                + "(?:哪位|哪个)是负责人|负责人)(?:呢|吗)?$")) {
            return OrganizationIntent.RESPONSIBILITY;
        }
        if (text.matches("^" + prefix + "(?:有[哪那]些人|都有谁|有谁|人员组成|人员构成|成员组成|"
                + "成员构成|成员名单|人员名单|分别是谁)(?:呢|吗)?$")) {
            return OrganizationIntent.PEOPLE;
        }
        if (text.matches("^" + prefix + "(?:如何协作|怎么协作|如何配合|怎么配合)(?:呢|吗)?$")) {
            return OrganizationIntent.COLLABORATION;
        }
        if (text.matches("^" + prefix + "(?:这些人|他们|上述人员|各负责人)(?:分别|各自)?"
                + "(?:负责什么|职责|分工)(?:呢|吗)?$")) {
            return OrganizationIntent.PERSON_WORK;
        }
        if (isPersonPronounWorkQuestion(text)) return OrganizationIntent.PERSON_WORK;
        return OrganizationIntent.NONE;
    }

    private static boolean isDepartmentKnowledgeQuestion(String text) {
        return text.matches("^(?:请问|想了解)?(?:兴宇|兴宇公司|公司)?(?:都|具体|主要)?"
                + "(?:有哪些部门|哪些部门|有哪几个部门|包括哪些部门|部门有哪些)(?:呢|吗)?$");
    }

    private static boolean isSpecificOrganizationResponsibleQuestion(String text) {
        String prefix = "(?:那|那么|请问|想了解|我想知道)?";
        String unit = "[\\p{IsHan}a-z0-9]{0,30}(?:事业部|分公司|办事处|中心|团队|小组|部门|部|办|组)";
        return text.matches("^" + prefix + "谁是" + unit + "(?:的)?负责人(?:呢|吗)?$")
                || text.matches("^" + prefix + unit + "(?:的)?负责人(?:分别|各自)?是谁(?:呢|吗)?$")
                || text.matches("^" + prefix + unit + "(?:是)?(?:由)?谁负责(?:的)?(?:呢|吗)?$")
                || text.matches("^" + prefix + "谁负责" + unit + "(?:呢|吗)?$");
    }

    private static boolean isSpecificOrganizationDutyQuestion(String text) {
        String prefix = "(?:那|那么|请问|想了解|我想知道)?";
        String unit = "[\\p{IsHan}a-z0-9]{0,30}(?:事业部|分公司|办事处|中心|团队|小组|部门|部|办|组)";
        return text.matches("^" + prefix + unit + "(?:的)?(?:主要)?(?:职责|工作职责|职能|工作范围|责任分工)"
                + "(?:都)?(?:是|有|包括|包含)?(?:什么|哪些)?(?:呢|吗)?$")
                || text.matches("^" + prefix + unit + "(?:的)?(?:有|包括|包含)(?:哪些|什么)(?:职责|职能|工作|责任)(?:呢|吗)?$")
                || text.matches("^" + prefix + unit + "(?:的)?(?:主要|都)?(?:负责什么|做什么|承担什么工作)(?:呢|吗)?$");
    }

    private static boolean isSpecificOrganizationPeopleQuestion(String text) {
        String prefix = "(?:那|那么|请问|想了解|我想知道)?";
        String unit = "[\\p{IsHan}a-z0-9]{0,30}(?:事业部|分公司|办事处|中心|团队|小组|部门|部|办|组)";
        return text.matches("^" + prefix + unit + "(?:有[哪那]些人|都有谁|有谁)(?:呢|吗)?$")
                || text.matches("^" + prefix + unit + "(?:的)?(?:人员组成|人员构成|成员组成|成员构成|"
                + "成员名单|人员名单)(?:是什么|有[哪那]些|包括[哪那]些)?(?:呢|吗)?$")
                || text.matches("^" + prefix + unit + "(?:包括|包含)(?:[哪那]些)?(?:人员|成员)(?:呢|吗)?$")
                || text.matches("^" + prefix + unit + "(?:的)?成员有[哪那]些(?:呢|吗)?$");
    }

    private static boolean isOrganizationCollaboration(String text) {
        boolean relation = text.matches(".*(?:协作|协同|配合|衔接|联动|合作|数据归属|数据责任|数据所有权).*" );
        boolean organization = text.matches(".*(?:公司|组织|部门|各部门|这些部门|上述部门|它们|"
                + "事业部|分公司|办事处|中心|团队|小组).*" );
        return relation && organization;
    }

    private static boolean isWorkOwnerQuestion(String text) {
        String prefix = "(?:那|那么|请问|想了解|我想知道)?";
        String work = "[\\p{IsHan}a-z0-9]{1,24}(?:工作|业务|事务|职能)";
        return text.matches("^" + prefix + "谁(?:来|在)?负责" + work + "(?:呢|吗)?$")
                || text.matches("^" + prefix + work + "(?:是)?(?:由)?谁负责(?:的)?(?:呢|吗)?$")
                || text.matches("^" + prefix + work + "负责人(?:分别|各自)?是谁(?:呢|吗)?$");
    }

    private static boolean isPersonWorkQuestion(String text) {
        String prefix = "(?:那|那么|请问|想了解|我想知道)?";
        String person = "([\\p{IsHan}·]{2,4}?)";
        for (String expression : new String[]{
                "^" + prefix + person + "(?:的)?(?:工作|岗位|职务|任职|分管范围)(?:是什么|有哪些|呢|吗)?$",
                "^" + prefix + person + "(?:主要|具体|都)?(?:负责什么|做什么|分管什么)(?:呢|吗)?$",
                "^" + prefix + person + "(?:在|属于)(?:哪个|什么)?(?:部门|组织|团队|中心)(?:呢|吗)?$",
                "^" + prefix + person + "(?:的)?(?:所属部门|所在部门)(?:是什么|呢|吗)?$"}) {
            Matcher matcher = Pattern.compile(expression).matcher(text);
            if (matcher.matches() && !matcher.group(1).matches(".*(?:客户|供应商|产品|商品|配件|项目|公司|组织|部门).*")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPersonPronounWorkQuestion(String text) {
        return isSingularPersonPronounWorkQuestion(text)
                || text.matches("^(?:那|那么|请问|想了解|我想知道)?(?:他们|她们|这些负责人|上述负责人)"
                + "(?:还|也|另外|同时)?(?:主要|具体|都)?(?:负责(?:什么|哪些工作|哪些业务|哪些事务)|"
                + "分管(?:什么|哪些工作|哪些业务|哪些事务)|做什么|有哪些工作)(?:呢|吗)?$");
    }

    private static boolean isSingularPersonPronounWorkQuestion(String text) {
        return text.matches("^(?:那|那么|请问|想了解|我想知道)?(?:他|她)"
                + "(?:还|也|另外|同时)?(?:主要|具体|都)?(?:负责(?:什么|哪些工作|哪些业务|哪些事务)|"
                + "分管(?:什么|哪些工作|哪些业务|哪些事务)|做什么|有哪些工作)(?:呢|吗)?$");
    }

    private static boolean isOrganizationHistory(String text) {
        return text.matches(".*(?:组织|架构|部门|人事|人员).*(?:变迁|变化|调整|演变|沿革|历史).*" )
                || text.matches(".*(?:变迁|变化|调整|演变|沿革|历史).*(?:组织|架构|部门|人事|人员).*" );
    }

    private static boolean isLeadershipHistory(String text) {
        String role = "(?:老板|一把手|掌舵人|最高负责人|最高领导|公司负责人|董事长|总经理|法人|法定代表人|创始人)";
        return text.matches(".*(?:历任|历届|前任|上一任|历史).*(?:" + role + ").*")
                || text.matches(".*(?:" + role + ").*(?:历任|历届|前任|上一任|历史).*");
    }

    private static boolean isCompanyLeadership(String text) {
        String prefix = "(?:那|那么|请问|想了解|我想知道)?";
        String company = "(?:四川兴宇|兴宇公司|兴宇|我们公司|咱们公司|本公司|公司)";
        String role = "(?:老板|一把手|掌舵人|最高负责人|最高领导|负责人|董事长|总经理|法人|法定代表人|创始人)";
        String standaloneRole = "(?:老板|一把手|掌舵人|最高负责人|最高领导|公司负责人|董事长|总经理|法人|法定代表人|创始人)";
        return text.matches("^" + prefix + company + "(?:的)?" + role + "(?:是|为)?(?:谁|哪位|何人)(?:呢|吗)?$")
                || text.matches("^" + prefix + "谁是" + company + "(?:的)?" + role + "(?:呢|吗)?$")
                || text.matches("^" + prefix + standaloneRole + "(?:是|为)?(?:谁|哪位|何人)(?:呢|吗)?$")
                || text.matches("^" + prefix + "谁(?:在|来)?负责(?:整个|这家)?" + company + "(?:呢|吗)?$")
                || text.matches("^" + prefix + "(?:整个|这家)?" + company
                + "(?:是)?(?:由)?谁(?:在|来)?负责(?:呢|吗)?$");
    }

    private static boolean isOrganizationRegion(String text) {
        return text.matches(".*(?:经销商业务|经销商部|业务部).*(?:区域|辖区|分工|划分|负责哪里).*" )
                || text.matches(".*(?:区域|辖区).*(?:经销商业务|经销商部|业务部).*(?:分工|划分|负责).*" );
    }

    private static boolean isNonOrganizationOwnerQuestion(String text) {
        String prefix = "(?:那|那么|请问|想了解|我想知道)?";
        return text.matches("^" + prefix + "(?:谁是)?(?:客户|供应商|产品|商品|配件|项目|门店)(?:的)?"
                + "(?:老板|一把手|掌舵人|最高负责人|最高领导|负责人)(?:分别|各自)?(?:是谁|是哪个|是哪位)?(?:呢|吗)?$" )
                || text.matches("^" + prefix + "谁负责(?:客户|供应商|产品|商品|配件|项目)"
                + "(?:工作|业务|事务)?(?:呢|吗)?$" );
    }

    private static boolean isGenericElaboration(String text) {
        return text.matches("^(?:那|那么|然后|再|继续)?(?:请|麻烦)?(?:再)?(?:具体|详细|进一步)?(?:说说|介绍|说明|讲讲|展开)(?:一下)?(?:呢|吗)?$")
                || text.matches("^(?:那|那么|然后|再|继续)?(?:具体|详细|进一步)(?:一点|一些|说说|介绍一下|说明一下|讲讲)(?:呢|吗)?$")
                || text.matches("^(?:那|那么)?(?:还有呢|还有哪些|分别是什么|具体是什么)(?:呢|吗)?$");
    }

    private static boolean hasKnowledgeContextReference(String text) {
        return text.matches(".*(?:这些|上述|它们|他们|各自|分别|其中|该|这个|这种|这类|那|其).*" )
                || text.length() <= 16 && text.matches(".*(?:呢|吗)$");
    }

    private static boolean isTopicFollowUp(String text, Map<String, Object> frame) {
        if (frame.isEmpty()) return false;
        String topic = String.valueOf(frame.get("topic"));
        if (hasKnowledgeContextReference(text)) return true;
        if ("RELATIONSHIP".equals(topic)) {
            return text.matches(".*(?:合作多久|怎么合作|合作方式|授权多久|关系呢).*" );
        }
        if ("CHANNEL".equals(topic)) {
            return text.matches(".*(?:渠道|门店|经销商|分销商|怎么卖|如何卖|怎么运作|如何运作).*" );
        }
        if ("REGION".equals(topic)) {
            return text.matches(".*(?:西藏|四川|成都|甘孜|阿坝|区域|地区|哪里).*(?:呢|情况|布局|覆盖)?.*" );
        }
        return false;
    }

    private static boolean isRealtimeOrBusinessQuestion(String text) {
        return hasRealtimeQualifier(text) || isBusinessMetricQuestion(text);
    }

    private static boolean hasRealtimeQualifier(String text) {
        return text.matches(".*(?:当前|现在|目前|现任|实时|erp|系统用户|用户列表|所有用户|全部用户|启用用户|停用用户|"
                + "最新|系统(?:中|内|里|记录(?:中|内|里|的)|显示(?:中|内|里|的)|登记的|维护的)|"
                + "部门列表|所有部门|全部部门|启用部门|停用部门|部门有多少|用户有多少|多少人|人数|"
                + "关联部门|主部门|部门关系).*" );
    }

    private static String withoutRealtimeQualifier(String text) {
        return text.replaceFirst("^(?:当前|现在|目前|现任|实时|最新|erp(?:中|内|里)?|"
                + "系统(?:中|内|里|记录(?:中|内|里|的)?|显示(?:中|内|里|的)?|登记的|维护的))", "");
    }

    private static String withoutRealtimeQualifierAnywhere(String text) {
        return text.replaceAll("(?:当前|现在|目前|现任|实时|最新|erp(?:中|内|里)?|"
                + "系统(?:中|内|里|记录(?:中|内|里|的)?|显示(?:中|内|里|的)?|登记的|维护的))", "");
    }

    private static boolean isBusinessMetricQuestion(String text) {
        return text.matches(".*(?:库存|价格|报价|销量|销售量|销售情况|销售额|采购额|订单|单据|收款|回款|"
                + "付款|应收|应付|欠款|金额|数量|排名|本月|上月|本周|上周|今年|去年).*" );
    }

    private static String compact(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\u3000，,。；;：:？?！!]", "");
    }

    private static String limit(String value) {
        if (value == null) return "";
        return value.substring(0, Math.min(MAX_CONTEXT_LENGTH, value.length()));
    }
}
