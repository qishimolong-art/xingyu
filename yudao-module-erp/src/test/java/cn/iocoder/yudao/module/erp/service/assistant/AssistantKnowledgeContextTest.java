package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssistantKnowledgeContextTest {

    @Test
    void acceptsControlledOrganizationFollowUpsAndBuildsRollingRetrievalQuestion() {
        AssistantExecutionEnvelope previous = knowledge("公司的组织架构");

        assertTrue(AssistantKnowledgeContext.isFollowUp("具体的人员组成", previous));
        assertEquals("公司的组织架构；追问：具体的人员组成",
                AssistantKnowledgeContext.contextualQuestion("具体的人员组成", previous));
        assertTrue(AssistantKnowledgeContext.isFollowUp("有哪些部门", previous));
        assertEquals("公司的组织架构；追问：有哪些部门",
                AssistantKnowledgeContext.contextualQuestion("有哪些部门", previous));
        assertTrue(AssistantKnowledgeContext.isFollowUp("那总经办负责人是谁", previous));
        assertEquals("公司的组织架构；追问：那总经办负责人是谁",
                AssistantKnowledgeContext.contextualQuestion("那总经办负责人是谁", previous));
        assertTrue(AssistantKnowledgeContext.isFollowUp("那财务部主要负责什么", previous));
        assertEquals("公司的组织架构；追问：那财务部主要负责什么",
                AssistantKnowledgeContext.contextualQuestion("那财务部主要负责什么", previous));
        assertTrue(AssistantKnowledgeContext.isFollowUp("这些部门的负责人分别是谁", previous));
        assertTrue(AssistantKnowledgeContext.isFollowUp("它们之间如何协作", previous));
        assertTrue(AssistantKnowledgeContext.isFollowUp("谁负责", knowledge("传媒运营部有哪些人")));

        AssistantExecutionEnvelope second = knowledge("具体的人员组成");
        second.getArguments().put(AssistantKnowledgeContext.CONTEXT_ARGUMENT,
                "公司的组织架构；追问：具体的人员组成");
        assertTrue(AssistantKnowledgeContext.isFollowUp("这些人分别负责什么", second));
        assertEquals("公司的组织架构；追问：具体的人员组成；追问：这些人分别负责什么",
                AssistantKnowledgeContext.contextualQuestion("这些人分别负责什么", second));
    }

    @Test
    void rejectsLiveBusinessQueriesAndNonKnowledgePreviousTurns() {
        AssistantExecutionEnvelope previous = knowledge("公司的组织架构");

        assertFalse(AssistantKnowledgeContext.isFollowUp("当前有哪些部门", previous));
        assertFalse(AssistantKnowledgeContext.isFollowUp("列出当前启用部门", previous));
        assertFalse(AssistantKnowledgeContext.isFollowUp("部门列表", previous));
        assertFalse(AssistantKnowledgeContext.isFollowUp("黑霸王库存", previous));
        assertFalse(AssistantKnowledgeContext.isFollowUp("本月销售额", previous));
        assertFalse(AssistantKnowledgeContext.isFollowUp("具体的人员组成", new AssistantExecutionEnvelope()));
    }

    @Test
    void companyTopicFrameSupportsControlledFollowUpsAndStopsAtErpBoundary() {
        AssistantExecutionEnvelope channel = knowledge("公司是通过哪些渠道把产品卖出去的");
        channel.getArguments().put(AssistantKnowledgeContext.KNOWLEDGE_FRAME_ARGUMENT,
                AssistantKnowledgeContext.knowledgeFrame(channel.getQuestion(), null, null));
        assertTrue(AssistantKnowledgeContext.isFollowUp("这些渠道分别怎么运作", channel));
        assertFalse(AssistantKnowledgeContext.isFollowUp("按渠道统计本月销售额", channel));

        AssistantExecutionEnvelope relationship = knowledge("我们和美孚是什么关系");
        relationship.getArguments().put(AssistantKnowledgeContext.KNOWLEDGE_FRAME_ARGUMENT,
                AssistantKnowledgeContext.knowledgeFrame(relationship.getQuestion(), null, null));
        assertTrue(AssistantKnowledgeContext.isFollowUp("具体怎么合作的", relationship));
        assertFalse(AssistantKnowledgeContext.isFollowUp("美孚当前库存", relationship));

        AssistantExecutionEnvelope region = knowledge("兴宇业务覆盖哪些区域");
        region.getArguments().put(AssistantKnowledgeContext.KNOWLEDGE_FRAME_ARGUMENT,
                AssistantKnowledgeContext.knowledgeFrame(region.getQuestion(), null, null));
        assertTrue(AssistantKnowledgeContext.isFollowUp("西藏呢", region));
        assertFalse(AssistantKnowledgeContext.isFollowUp("各地区本月销量", region));
    }

    @Test
    void recognizesShortDepartmentQuestionsAsCompanyKnowledgeUnlessRealtime() {
        for (String question : new String[]{"有哪些部门", "公司有哪些部门", "都有哪些部门", "部门有哪些", "有哪几个部门", "包括哪些部门"}) {
            assertTrue(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
        }
        for (String question : new String[]{"当前有哪些部门", "列出启用部门", "部门列表"}) {
            assertFalse(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
        }
    }

    @Test
    void recognizesSpecificOrganizationResponsibleQuestionsUnlessRealtimeOrNonOrganization() {
        for (String question : new String[]{"谁是总经办的负责人", "市场部负责人是谁", "项目部谁负责",
                "项目部由谁负责", "谁负责物流部", "事业部负责人是谁", "运营中心由谁负责",
                "一组谁负责", "销售团队负责人是谁", "成都分公司负责人是谁", "西藏办事处谁负责",
                "系统维护部负责人是谁"}) {
            assertTrue(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
            assertTrue(AssistantKnowledgeContext.containsOrganizationLanguage(question), question);
        }
        for (String question : new String[]{"当前总经办负责人是谁", "现在市场部由谁负责", "最新项目部负责人是谁",
                "ERP中总经办负责人是谁", "系统中市场部负责人是谁", "系统记录的物流部负责人是谁",
                "客户负责人是谁", "供应商负责人是谁", "产品负责人是谁"}) {
            assertFalse(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
        }
        for (String question : new String[]{"当前总经办负责人是谁", "现在市场部由谁负责", "最新项目部负责人是谁",
                "ERP中总经办负责人是谁", "系统中市场部负责人是谁", "系统记录的物流部负责人是谁"}) {
            assertTrue(AssistantKnowledgeContext.isRealtimeOrganizationResponsibleQuestion(question), question);
        }
        for (String question : new String[]{"谁是总经办的负责人", "客户负责人是谁", "系统集成部负责人是谁",
                "系统维护部负责人是谁"}) {
            assertFalse(AssistantKnowledgeContext.isRealtimeOrganizationResponsibleQuestion(question), question);
        }
    }

    @Test
    void recognizesCompanyLeadershipRolesWithoutConflatingExternalOwnersOrRealtimeData() {
        for (String question : new String[]{"公司的老板是谁", "公司的一把手是谁", "董事长是谁",
                "兴宇的掌舵人是谁", "公司的负责人是谁", "公司最高负责人是哪位", "谁负责整个公司", "公司由谁负责",
                "总经理是谁", "法人是谁", "法定代表人是谁", "创始人是谁"}) {
            assertTrue(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
            assertEquals(AssistantKnowledgeContext.OrganizationIntent.COMPANY_LEADERSHIP,
                    AssistantKnowledgeContext.organizationIntent(question), question);
        }
        for (String question : new String[]{"公司的老板是谁", "公司的一把手是谁", "兴宇的掌舵人是谁",
                "公司的负责人是谁", "公司最高负责人是哪位", "谁负责整个公司"}) {
            assertEquals("董事长", AssistantKnowledgeContext.companyLeadershipRole(question), question);
            assertTrue(AssistantKnowledgeContext.isColloquialCompanyLeadershipQuestion(question), question);
        }
        assertEquals("总经理", AssistantKnowledgeContext.companyLeadershipRole("总经理是谁"));
        assertEquals("法定代表人", AssistantKnowledgeContext.companyLeadershipRole("法定代表人是谁"));
        assertEquals(AssistantKnowledgeContext.OrganizationIntent.HISTORY,
                AssistantKnowledgeContext.organizationIntent("前任董事长是谁"));

        for (String question : new String[]{"客户老板是谁", "供应商老板是谁", "门店老板是谁",
                "项目负责人是谁", "产品负责人是谁", "公司老板本月销售额"}) {
            assertFalse(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
        }
        for (String question : new String[]{"现任董事长是谁", "当前公司一把手是谁",
                "ERP中公司的老板是谁", "系统记录的公司负责人是谁"}) {
            assertFalse(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
            assertTrue(AssistantKnowledgeContext.isRealtimeCompanyLeadershipQuestion(question), question);
            assertTrue(AssistantKnowledgeContext.isRealtimeOrganizationKnowledgeBoundary(question), question);
        }
    }

    @Test
    void recognizesSpecificOrganizationDutyQuestionsUnlessRealtimeOrBusinessMetric() {
        for (String question : new String[]{"财务部的主要职责", "市场部职责是什么", "物流部负责什么",
                "项目部主要做什么", "总经办有哪些职责", "事业部的职能", "运营中心工作范围",
                "一组责任分工", "销售团队承担什么工作", "成都分公司主要负责什么", "西藏办事处有哪些职责"}) {
            assertTrue(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
            assertTrue(AssistantKnowledgeContext.containsOrganizationLanguage(question), question);
            assertFalse(AssistantKnowledgeContext.isRealtimeOrganizationDutyQuestion(question), question);
        }
        assertTrue(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion("财务部职责都有哪些"));
        assertTrue(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion("市场部的工作职责是什么"));
        assertTrue(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion("物流部都负责什么"));
        for (String question : new String[]{"当前财务部职责是什么", "现在市场部负责什么", "最新物流部工作范围",
                "ERP中财务部有哪些职责", "系统记录的项目部责任分工"}) {
            assertFalse(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
            assertTrue(AssistantKnowledgeContext.isRealtimeOrganizationDutyQuestion(question), question);
        }
        for (String question : new String[]{"财务部本月付款金额", "市场部销售额", "物流部库存",
                "项目部采购订单", "客户负责什么", "供应商主要做什么", "产品的主要职责"}) {
            assertFalse(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
            assertFalse(AssistantKnowledgeContext.isRealtimeOrganizationDutyQuestion(question), question);
        }
    }

    @Test
    void recognizesUnifiedOrganizationCollaborationWorkOwnerPersonHistoryAndRegionQuestions() {
        for (String question : new String[]{"公司各部门之间如何协作", "部门之间怎么配合", "这些部门如何协同",
                "谁负责传媒工作", "物流工作谁负责", "传媒工作由谁负责", "汪凯虎的工作",
                "汪凯虎负责什么", "汪凯虎在哪个部门", "组织架构经历过哪些变化", "兴宇人事变迁",
                "经销商业务部如何划分区域", "经销商业务区域分工", "这些部门的负责人分别是谁"}) {
            assertTrue(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
            assertTrue(AssistantKnowledgeContext.containsOrganizationLanguage(question), question);
        }
        for (String question : new String[]{"当前传媒工作由谁负责", "现在各部门如何协作",
                "ERP中汪凯虎负责什么", "系统记录的各部门负责人是谁"}) {
            assertFalse(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
            assertTrue(AssistantKnowledgeContext.isRealtimeOrganizationKnowledgeBoundary(question), question);
        }
    }

    @Test
    void recognizesSpecificOrganizationPeopleQuestionsAndCommonThoseTypo() {
        for (String question : new String[]{"总经办有哪些人", "总经办有那些人", "总经办都有谁",
                "总经办有谁", "总经办的人员组成", "传媒运营部有哪些人", "财务部成员有哪些",
                "运营中心的人员构成", "销售团队成员名单", "成都分公司包括哪些人员",
                "西藏办事处包含那些成员"}) {
            assertTrue(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
            assertEquals(AssistantKnowledgeContext.OrganizationIntent.PEOPLE,
                    AssistantKnowledgeContext.organizationIntent(question), question);
        }
        for (String question : new String[]{"客户有哪些人", "供应商有哪些人", "产品有哪些人",
                "总经办本月销售额", "物流部库存"}) {
            assertFalse(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
        }
        assertFalse(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion("当前总经办有哪些人"));
        assertTrue(AssistantKnowledgeContext.isRealtimeOrganizationKnowledgeBoundary("当前总经办有哪些人"));
        assertTrue(AssistantKnowledgeContext.isRealtimeOrganizationKnowledgeBoundary("ERP中总经办有那些人"));
    }

    @Test
    void currentOrganizationIntentOverridesEarlierKnowledgeIntent() {
        AssistantExecutionEnvelope collaboration = knowledge("公司各部门之间如何协作");
        collaboration.getArguments().put(AssistantKnowledgeContext.CONTEXT_ARGUMENT,
                "公司各部门之间如何协作；追问：这些部门的负责人");

        assertEquals(AssistantKnowledgeContext.OrganizationIntent.PEOPLE,
                AssistantKnowledgeContext.organizationIntent("总经办有哪些人", collaboration));
        assertEquals(AssistantKnowledgeContext.OrganizationIntent.PEOPLE,
                AssistantKnowledgeContext.organizationIntent("总经办有那些人", collaboration));
        assertEquals(AssistantKnowledgeContext.OrganizationIntent.RESPONSIBILITY,
                AssistantKnowledgeContext.organizationIntent("谁负责", knowledge("传媒运营部有哪些人")));
        assertEquals(AssistantKnowledgeContext.OrganizationIntent.PEOPLE,
                AssistantKnowledgeContext.organizationIntent("有哪些人", knowledge("总经办负责人是谁")));
    }

    @Test
    void responsibleEllipsisSupportsNaturalVariantsOnlyAfterOrganizationKnowledge() {
        AssistantExecutionEnvelope previous = knowledge("传媒运营部有哪些人");
        for (String question : new String[]{"谁负责", "谁是负责人", "负责人是谁", "由谁负责",
                "哪位是负责人", "哪个是负责人", "负责人呢", "那负责人呢"}) {
            assertTrue(AssistantKnowledgeContext.isFollowUp(question, previous), question);
            assertEquals(AssistantKnowledgeContext.OrganizationIntent.RESPONSIBILITY,
                    AssistantKnowledgeContext.organizationIntent(question, previous), question);
        }
        assertEquals("传媒运营部有哪些人；追问：谁是负责人",
                AssistantKnowledgeContext.contextualQuestion("谁是负责人", previous));

        AssistantExecutionEnvelope product = knowledge("黑霸王有哪些产品");
        assertFalse(AssistantKnowledgeContext.isFollowUp("谁是负责人", product));
        assertEquals(AssistantKnowledgeContext.OrganizationIntent.NONE,
                AssistantKnowledgeContext.organizationIntent("谁是负责人", product));
        assertFalse(AssistantKnowledgeContext.isFollowUp("客户负责人是谁", previous));
        assertFalse(AssistantKnowledgeContext.isFollowUp("供应商负责人是谁", previous));
        assertFalse(AssistantKnowledgeContext.isFollowUp("产品负责人是谁", previous));
        assertFalse(AssistantKnowledgeContext.isFollowUp("项目负责人是谁", previous));
    }

    @Test
    void singularPersonPronounUsesOnlyReviewedOrganizationContext() {
        AssistantExecutionEnvelope previous = knowledge("谁负责");
        previous.getArguments().put(AssistantKnowledgeContext.CONTEXT_ARGUMENT,
                "传媒运营部有哪些人；追问：谁负责");
        previous.getArguments().put(AssistantKnowledgeContext.ORGANIZATION_CONTEXT_ARGUMENT,
                organizationContext("传媒运营部", "王宇"));

        assertTrue(AssistantKnowledgeContext.isFollowUp("他还负责哪些工作", previous));
        assertEquals(AssistantKnowledgeContext.OrganizationIntent.PERSON_WORK,
                AssistantKnowledgeContext.organizationIntent("他还负责哪些工作", previous));
        assertEquals("传媒运营部有哪些人；追问：谁负责；已确认负责人：王宇；追问：他还负责哪些工作",
                AssistantKnowledgeContext.contextualQuestion("他还负责哪些工作", previous));
        assertNull(AssistantKnowledgeContext.personReferenceClarification("他还负责哪些工作", previous));

        AssistantExecutionEnvelope ambiguous = knowledge("后勤部负责人是谁");
        ambiguous.getArguments().put(AssistantKnowledgeContext.ORGANIZATION_CONTEXT_ARGUMENT,
                organizationContext("后勤部", "毛琳", "冯靖雯"));
        assertTrue(AssistantKnowledgeContext.personReferenceClarification("他还负责什么", ambiguous)
                .contains("毛琳、冯靖雯"));
        assertTrue(AssistantKnowledgeContext.isFollowUp("他们还负责哪些工作", ambiguous));
        assertTrue(AssistantKnowledgeContext.contextualQuestion("他们还负责哪些工作", ambiguous)
                .contains("已确认负责人：毛琳、冯靖雯"));
        assertTrue(AssistantKnowledgeContext.personReferenceClarification("他还负责什么", null)
                .contains("具体指哪位人员"));

        assertTrue(AssistantKnowledgeContext.isContextualRealtimeOrganizationBoundary(
                "当前谁是负责人", knowledge("传媒运营部有哪些人")));
        assertTrue(AssistantKnowledgeContext.isRealtimeOrganizationKnowledgeBoundary("他当前还负责什么"));
    }

    @Test
    void organizationRoutingKeepsBusinessMetricsAndNonOrganizationOwnersOut() {
        for (String question : new String[]{"财务部本月付款金额", "市场部销售额", "物流部库存",
                "汪凯虎本月销售额", "客户负责人是谁", "供应商负责人是谁", "产品负责人是谁",
                "项目负责人是谁", "谁负责客户工作"}) {
            assertFalse(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question), question);
        }
        assertTrue(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion("项目部负责人是谁"));
        assertTrue(AssistantKnowledgeContext.isOrganizationKnowledgeQuestion("谁负责项目部"));
    }

    private static AssistantExecutionEnvelope knowledge(String question) {
        AssistantExecutionEnvelope envelope = new AssistantExecutionEnvelope();
        envelope.setQuestion(question);
        envelope.setRouteType(AssistantExecutionEnvelope.RouteType.KNOWLEDGE);
        envelope.setToolName(AssistantCompanyKnowledge.TOOL_NAME);
        return envelope;
    }

    private static Map<String, Object> organizationContext(String unit, String... people) {
        Map<String, Object> context = new java.util.LinkedHashMap<>();
        context.put("unit", unit);
        context.put("people", java.util.Arrays.asList(people));
        context.put("role", "负责人");
        return context;
    }
}
