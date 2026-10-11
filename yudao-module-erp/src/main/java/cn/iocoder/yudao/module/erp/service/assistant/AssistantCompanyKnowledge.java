package cn.iocoder.yudao.module.erp.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Public company/product knowledge compiled from the reviewed Obsidian notes. */
@Component
@Slf4j
public class AssistantCompanyKnowledge {

    public static final String TOOL_NAME = "answer_company_knowledge";
    static final String INTERNAL_ORGANIZATION_CONTEXT = "_organizationContext";
    private static final int MAX_MATCHES = 6;
    private static final int MAX_CONTEXT_CHARS = 12_000;
    private static final Pattern ASCII_TERM = Pattern.compile("[a-z0-9][a-z0-9+./-]{1,}");
    private static final Pattern HAN_METADATA_TERM = Pattern.compile("[\\p{IsHan}]{3,12}");
    private static final Set<String> GENERIC_QUERY_GRAMS = new HashSet<>(Arrays.asList(
            "什么", "怎么", "如何", "哪些", "那些", "有哪", "有那", "有哪些", "有那些", "都有", "都有哪", "哪几", "哪几个", "几个",
            "介绍", "我们", "公司", "主要", "的主要", "做什么", "负责什", "责什么", "哪些职", "些职责",
            "职责是", "责是什", "是什么", "这些", "上述", "分别", "各自", "之间", "谁负责", "的工作"));
    private static final Pattern REFERENCE_INSTRUCTION = Pattern.compile(
            "(?i).*(?:ignore\\s+(?:all\\s+)?(?:previous|above)|system\\s*prompt|developer\\s*(?:message|instruction)|"
                    + "you\\s+are\\s+(?:chatgpt|an?\\s+assistant)|prompt\\s*injection|"
                    + "忽略(?:以上|之前|前面).*(?:指令|要求)|系统提示词|开发者(?:消息|指令)|"
                    + "执行(?:以下|上面|这条).*(?:命令|指令)|把.*(?:密钥|token|密码).*(?:输出|泄露)).*");

    private static final class Entry {
        final String id, title, section, path, domain, text, updated, verificationStatus, reason, contentHash;
        final List<String> aliases, tags;
        final String normalizedTitle, normalizedText;
        final boolean instructionIsolated;

        Entry(JsonNode node) {
            id = node.path("id").asText();
            title = safeMetadata(node.path("title").asText(), 200);
            section = safeMetadata(node.path("section").asText("知识卡"), 200);
            path = safeMetadata(node.path("path").asText(), 500);
            domain = safeMetadata(node.path("domain").asText(), 100);
            updated = safeMetadata(node.path("updated").asText(), 32);
            verificationStatus = node.path("verificationStatus").asText("REVIEWED");
            reason = safeMetadata(node.path("reason").asText(), 500);
            contentHash = safeMetadata(node.path("contentHash").asText(), 128);
            String rawText = node.path("text").asText(reason);
            text = isolateInstructions(rawText);
            instructionIsolated = !rawText.equals(text);
            aliases = values(node.path("aliases"));
            tags = values(node.path("tags"));
            normalizedTitle = normalize(String.join(" ", combine(title, section, domain, aliases, tags)));
            normalizedText = normalize(text);
        }

        private static List<String> values(JsonNode values) {
            List<String> result = new ArrayList<>();
            if (values.isArray()) for (JsonNode value : values) {
                String item = safeMetadata(value.asText(), 200);
                if (!item.isEmpty()) result.add(item);
            }
            return Collections.unmodifiableList(result);
        }

        private static List<String> combine(String title, String section, String domain,
                                            List<String> aliases, List<String> tags) {
            List<String> result = new ArrayList<>(Arrays.asList(title, section, domain));
            result.addAll(aliases); result.addAll(tags); return result;
        }
    }

    private static final class Scored {
        final Entry entry;
        final int score;
        Scored(Entry entry, int score) { this.entry = entry; this.score = score; }
    }

    private static final class Loaded {
        final List<Entry> entries;
        final String version;
        final String error;
        final int reviewedNotes;
        final int excludedDraftNotes;

        Loaded(List<Entry> entries, String version, String error, int reviewedNotes, int excludedDraftNotes) {
            this.entries = entries; this.version = version; this.error = error;
            this.reviewedNotes = reviewedNotes; this.excludedDraftNotes = excludedDraftNotes;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, List<Entry>> index = new HashMap<>();
    private final String version;
    private final String unavailableReason;
    private final int reviewedNotes;
    private final int excludedDraftNotes;

    public AssistantCompanyKnowledge() {
        this(loadClasspath());
    }

    AssistantCompanyKnowledge(byte[] bytes, String expectedChecksum) {
        this(load(bytes, expectedChecksum));
    }

    private AssistantCompanyKnowledge(Loaded loaded) {
        entries.addAll(loaded.entries);
        version = loaded.version;
        unavailableReason = loaded.error;
        reviewedNotes = loaded.reviewedNotes;
        excludedDraftNotes = loaded.excludedDraftNotes;
        buildIndex();
    }

    private static Loaded loadClasspath() {
        try {
            byte[] bytes;
            try (InputStream input = new ClassPathResource("assistant/company-knowledge.json").getInputStream()) {
                bytes = StreamUtils.copyToByteArray(input);
            }
            String expected;
            try (InputStream input = new ClassPathResource("assistant/company-knowledge.sha256").getInputStream()) {
                expected = StreamUtils.copyToString(input, StandardCharsets.US_ASCII).trim();
            }
            return load(bytes, expected);
        } catch (Exception error) {
            log.error("Company knowledge bundle is unavailable: {}", error.getMessage());
            return unavailable("公司知识资源未找到或无法读取");
        }
    }

    private static Loaded load(byte[] bytes, String expected) {
        try {
            String actual = sha256(bytes);
            if (!actual.equalsIgnoreCase(expected)) throw new IllegalStateException("Company knowledge checksum mismatch");
            JsonNode root = new ObjectMapper().readTree(bytes);
            int schemaVersion = root.path("schemaVersion").asInt();
            if ((schemaVersion != 1 && schemaVersion != 2)
                    || !"PUBLIC_TO_ASSISTANT_USERS".equals(root.path("access").asText())) {
                throw new IllegalStateException("Unsupported company knowledge bundle");
            }
            String version = root.path("version").asText();
            Set<String> ids = new HashSet<>();
            List<Entry> entries = new ArrayList<>();
            for (JsonNode node : root.path("documents")) {
                if (schemaVersion >= 2) validateEntryNode(node, "REVIEWED");
                Entry entry = new Entry(node);
                if (entry.id.isEmpty() || entry.title.isEmpty() || entry.text.isEmpty() || !ids.add(entry.id)) {
                    throw new IllegalStateException("Invalid or duplicate company knowledge entry");
                }
                entries.add(entry);
            }
            if (schemaVersion >= 2) {
                appendEntries(root.path("unverifiedTopics"), entries, ids, "UNVERIFIED");
                appendEntries(root.path("unverifiedDocuments"), entries, ids, "UNVERIFIED");
            }
            if (entries.isEmpty() || version.isEmpty()) throw new IllegalStateException("Empty company knowledge bundle");
            return new Loaded(entries, version, null, root.path("reviewedNotes").asInt(),
                    root.path("excludedDraftNotes").asInt());
        } catch (Exception error) {
            log.error("Company knowledge bundle validation failed: {}", error.getMessage());
            return unavailable("公司知识资源校验失败");
        }
    }

    private static Loaded unavailable(String reason) {
        return new Loaded(Collections.<Entry>emptyList(), null, reason, 0, 0);
    }

    private static void appendEntries(JsonNode nodes, List<Entry> entries, Set<String> ids, String expectedStatus)
            throws Exception {
        if (!nodes.isArray()) return;
        for (JsonNode node : nodes) {
            validateEntryNode(node, expectedStatus);
            Entry entry = new Entry(node);
            if (entry.id.isEmpty() || entry.title.isEmpty() || entry.text.isEmpty() || !ids.add(entry.id)) {
                throw new IllegalStateException("Invalid or duplicate company knowledge entry");
            }
            entries.add(entry);
        }
    }

    private static void validateEntryNode(JsonNode node, String expectedStatus) throws Exception {
        if (!expectedStatus.equals(node.path("verificationStatus").asText())) {
            throw new IllegalStateException("Invalid company knowledge verification status");
        }
        String text = node.path("text").asText();
        String contentHash = node.path("contentHash").asText();
        if (!text.isEmpty() && (!contentHash.matches("[0-9a-f]{64}") || !contentHash.equals(sha256(
                text.getBytes(StandardCharsets.UTF_8))))) {
            throw new IllegalStateException("Company knowledge entry hash mismatch");
        }
    }

    public boolean available() { return !entries.isEmpty(); }
    public String version() { return version; }
    public String unavailableReason() { return unavailableReason; }
    /**
     * 仅暴露已审核知识的标题、章节、领域、别名和标签词，用于受控同音召回。
     * 正文和待核实资料不会进入自动纠错词表。
     */
    public List<String> phoneticTerms() {
        Set<String> result = new LinkedHashSet<>();
        for (Entry entry : entries) {
            if (!"REVIEWED".equals(entry.verificationStatus)) continue;
            List<String> metadata = new ArrayList<>(Arrays.asList(entry.title, entry.section, entry.domain));
            metadata.addAll(entry.aliases);
            metadata.addAll(entry.tags);
            for (String value : metadata) {
                Matcher matcher = HAN_METADATA_TERM.matcher(value == null ? "" : value);
                while (matcher.find()) result.add(matcher.group());
            }
        }
        return new ArrayList<>(result);
    }
    public Map<String, Object> statistics() {
        Map<String, Object> result = new LinkedHashMap<>();
        int reviewedChunks = 0, unverifiedItems = 0, isolatedItems = 0;
        for (Entry entry : entries) {
            if ("REVIEWED".equals(entry.verificationStatus)) reviewedChunks++; else unverifiedItems++;
            if (entry.instructionIsolated) isolatedItems++;
        }
        result.put("reviewedNotes", reviewedNotes);
        result.put("reviewedChunks", reviewedChunks);
        result.put("excludedDraftNotes", excludedDraftNotes);
        result.put("unverifiedItems", unverifiedItems);
        result.put("isolatedInstructionItems", isolatedItems);
        result.put("version", version);
        return result;
    }

    public Map<String, Object> answer(String question, AssistantModelClient model) {
        return answer(question, question, AssistantKnowledgeContext.organizationIntent(question), model);
    }

    Map<String, Object> answer(String currentQuestion, String retrievalQuestion,
                               AssistantKnowledgeContext.OrganizationIntent currentIntent,
                               AssistantModelClient model) {
        if (!available()) throw new AssistantFailure("KNOWLEDGE_UNAVAILABLE", unavailableReason);
        String current = currentQuestion == null ? "" : currentQuestion;
        String retrieval = retrievalQuestion == null || retrievalQuestion.trim().isEmpty() ? current : retrievalQuestion;
        AssistantCompanyKnowledgeIntent.Analysis knowledgeIntent =
                AssistantCompanyKnowledgeIntent.analyze(retrieval);
        AssistantKnowledgeContext.OrganizationIntent intent = currentIntent == null
                ? AssistantKnowledgeContext.OrganizationIntent.NONE : currentIntent;
        List<Map<String, Object>> matches = retrieve(retrieval);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", matches.isEmpty() ? "KNOWLEDGE_NOT_FOUND" : "SUCCESS");
        result.put("queryType", AssistantExecutionEnvelope.RouteType.KNOWLEDGE.name());
        result.put("toolName", TOOL_NAME);
        result.put("title", "公司知识问答");
        result.put("summary", Collections.emptyList());
        result.put("rows", Collections.emptyList());
        result.put("queriedAt", LocalDateTime.now().toString());
        result.put("scope", "公司公共知识");
        if (matches.isEmpty()) {
            String text = "当前已发布的公司知识中没有找到足够相关的依据，请补充具体公司、产品、标准或业务主题。";
            result.put("emptyText", text); result.put("answerText", text);
            result.put("basis", Collections.emptyList()); result.put("sources", Collections.emptyList());
            result.put("verificationStatus", "NO_EVIDENCE");
            return result;
        }
        for (int index = 0; index < matches.size(); index++) matches.get(index).put("citation", "S" + (index + 1));
        if (intent == AssistantKnowledgeContext.OrganizationIntent.COMPANY_LEADERSHIP) {
            return companyLeadershipAnswer(current, matches, result);
        }
        AssistantModelClient.KnowledgeAnswer answer;
        try {
            answer = current.equals(retrieval)
                    ? model.answerKnowledge(current, matches)
                    : model.answerKnowledge(current, retrieval, matches);
        } catch (AssistantFailure failure) {
            if (isOrganizationModelFailure(intent, failure)) {
                return organizationFallback(current, retrieval, intent, matches, result);
            }
            if (isKnowledgeModelFailure(knowledgeIntent, failure)) {
                return knowledgeFallback(retrieval, knowledgeIntent, matches, result);
            }
            throw failure;
        }
        if (answer == null || answer.getAnswer() == null || answer.getAnswer().trim().isEmpty()
                || answer.getSourceIds() == null || answer.getSourceIds().isEmpty()) {
            if (intent != AssistantKnowledgeContext.OrganizationIntent.NONE) {
                return organizationFallback(current, retrieval, intent, matches, result);
            }
            if (knowledgeIntent.isKnowledge()) {
                return knowledgeFallback(retrieval, knowledgeIntent, matches, result);
            }
            throw new AssistantFailure("MODEL_INVALID", "模型未返回有效的公司知识来源");
        }
        Set<String> used = new LinkedHashSet<>(answer.getSourceIds());
        List<Map<String, Object>> sources = new ArrayList<>();
        List<Map<String, Object>> groundedMatches = new ArrayList<>();
        List<String> basis = new ArrayList<>();
        boolean unverified = false;
        for (Map<String, Object> match : matches) if (used.isEmpty() || used.contains(match.get("id"))) {
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("id", match.get("id")); source.put("title", match.get("title"));
            source.put("section", match.get("section")); source.put("path", match.get("path"));
            source.put("updated", match.get("updated"));
            source.put("verificationStatus", match.get("verificationStatus"));
            source.put("contentHash", match.get("contentHash"));
            source.put("citation", match.get("citation"));
            if ("UNVERIFIED".equals(match.get("verificationStatus"))) {
                unverified = true;
                source.put("reason", match.get("reason"));
            }
            sources.add(source);
            groundedMatches.add(match);
            String label = "[" + match.get("citation") + "]《" + match.get("title") + "》";
            if (!basis.contains(label)) basis.add(label);
        }
        if (sources.isEmpty()) {
            if (intent != AssistantKnowledgeContext.OrganizationIntent.NONE) {
                return organizationFallback(current, retrieval, intent, matches, result);
            }
            throw new AssistantFailure("MODEL_INVALID", "模型未返回有效的公司知识来源");
        }
        result.put("status", unverified ? "UNVERIFIED_KNOWLEDGE" : "SUCCESS");
        result.put("verificationStatus", unverified ? "UNVERIFIED" : "REVIEWED");
        result.put("answerText", ensureCitation(answer.getAnswer(), sources));
        result.put("sources", sources); result.put("basis", basis);
        if (unverified) {
            String notice = "部分依据仍处于待核实状态，以下内容仅说明现有材料和冲突，不能作为确定结论。";
            result.put("verificationNotice", notice);
            result.put("warnings", Collections.singletonList(notice));
        }
        Map<String, Object> knowledge = new LinkedHashMap<>();
        knowledge.put("title", "兴宇公司知识库");
        knowledge.put("description", unverified
                ? "依据已审核知识卡回答；待核实主题仅依据冲突登记说明"
                : "仅依据已审核公司、产品和销售知识回答");
        knowledge.put("source", "知识库/16-公司产品与销售知识"); knowledge.put("version", version);
        knowledge.putAll(statistics());
        result.put("knowledge", knowledge);
        attachOrganizationContext(result, retrieval, intent, groundedMatches);
        return result;
    }

    public List<Map<String, Object>> retrieve(String question) {
        AssistantCompanyKnowledgeIntent.Analysis knowledgeIntent =
                AssistantCompanyKnowledgeIntent.analyze(question);
        String normalized = normalize(AssistantIntentGeneralizer.normalizeCompanySelfReference(question));
        if (normalized.length() < 2) return Collections.emptyList();
        String relationshipCounterparty = AssistantIntentGeneralizer.companyRelationshipCounterparty(question);
        boolean relationshipQuestion = AssistantIntentGeneralizer.isCompanyRelationshipQuestion(question);
        boolean organizationQuestion = !relationshipQuestion
                && (AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question)
                || AssistantKnowledgeContext.containsOrganizationLanguage(question));
        boolean unverifiedOverview = normalized.contains("待核实")
                && normalized.matches(".*(?:哪些|内容|事项|清单|全部|所有).*" );
        Set<String> grams = grams(normalized);
        grams.addAll(expandedGrams(normalized, knowledgeIntent));
        Set<String> ascii = asciiTerms(normalized);
        LinkedHashSet<Entry> candidates = new LinkedHashSet<>();
        for (String gram : grams) candidates.addAll(index.getOrDefault(gram, Collections.<Entry>emptyList()));
        for (String term : ascii) candidates.addAll(index.getOrDefault(term, Collections.<Entry>emptyList()));
        if (candidates.isEmpty()) candidates.addAll(entries);
        List<Scored> scored = new ArrayList<>();
        for (Entry entry : candidates) {
            if (organizationQuestion && !isOrganizationEntry(entry)) continue;
            if (relationshipCounterparty != null
                    && !mentionsRelationshipCounterparty(entry, relationshipCounterparty)) continue;
            int score = score(entry, normalized, grams, ascii)
                    + contextualBoost(entry, normalized, knowledgeIntent)
                    + topicBoost(entry, knowledgeIntent);
            if (relationshipCounterparty != null) score += relationshipKnowledgeBoost(entry);
            int threshold = "REVIEWED".equals(entry.verificationStatus) ? 4 : 8;
            if (score >= threshold) scored.add(new Scored(entry, score));
        }
        scored.sort(Comparator.comparingInt((Scored value) -> value.score).reversed()
                .thenComparing(value -> value.entry.path).thenComparing(value -> value.entry.section));
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Integer> perPath = new HashMap<>();
        int length = 0;
        for (Scored value : scored) {
            if (result.size() >= MAX_MATCHES || length >= MAX_CONTEXT_CHARS) break;
            int used = perPath.getOrDefault(value.entry.path, 0);
            int perPathLimit = unverifiedOverview && "UNVERIFIED".equals(value.entry.verificationStatus) ? MAX_MATCHES : 2;
            if (used >= perPathLimit) continue;
            String excerpt = value.entry.text;
            if (length + excerpt.length() > MAX_CONTEXT_CHARS) excerpt = excerpt.substring(0, MAX_CONTEXT_CHARS - length);
            if (excerpt.trim().isEmpty()) break;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", value.entry.id); item.put("title", value.entry.title);
            item.put("section", value.entry.section); item.put("path", value.entry.path);
            item.put("text", excerpt); item.put("score", value.score);
            item.put("updated", value.entry.updated); item.put("verificationStatus", value.entry.verificationStatus);
            item.put("reason", value.entry.reason); item.put("contentHash", value.entry.contentHash);
            item.put("instructionIsolated", value.entry.instructionIsolated);
            result.add(item); length += excerpt.length(); perPath.put(value.entry.path, used + 1);
        }
        return result;
    }

    private static boolean mentionsRelationshipCounterparty(Entry entry, String counterparty) {
        String normalized = normalize(counterparty);
        return !normalized.isEmpty()
                && (entry.normalizedTitle.contains(normalized) || entry.normalizedText.contains(normalized));
    }

    private static int relationshipKnowledgeBoost(Entry entry) {
        if (entry.normalizedTitle.contains("合作关系")) return 40;
        if (entry.normalizedText.contains("授权经销商") || entry.normalizedText.contains("核心合作品牌")) return 20;
        return 0;
    }

    private void buildIndex() {
        for (Entry entry : entries) {
            Set<String> terms = new LinkedHashSet<>();
            terms.addAll(grams(entry.normalizedTitle));
            terms.addAll(grams(entry.normalizedText));
            terms.addAll(asciiTerms(entry.normalizedTitle));
            terms.addAll(asciiTerms(entry.normalizedText));
            for (String term : terms) index.computeIfAbsent(term, ignored -> new ArrayList<>()).add(entry);
        }
    }

    private static int score(Entry entry, String question, Set<String> grams, Set<String> ascii) {
        int score = 0;
        if (question.length() >= 3 && entry.normalizedTitle.contains(question)) score += 25;
        for (String term : ascii) {
            if (entry.normalizedTitle.contains(term)) score += 12;
            else if (entry.normalizedText.contains(term)) score += 5;
        }
        for (String gram : grams) {
            if (entry.normalizedTitle.contains(gram)) score += gram.length() == 3 ? 5 : 3;
            else if (entry.normalizedText.contains(gram)) score += gram.length() == 3 ? 2 : 1;
        }
        return score;
    }

    private static int contextualBoost(Entry entry, String question,
                                       AssistantCompanyKnowledgeIntent.Analysis knowledgeIntent) {
        int boost = 0;
        if (AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question)) {
            if ("兴宇组织架构与负责人".equals(entry.title)) boost = Math.max(boost, 120);
            if (entry.normalizedTitle.matches(".*(?:组织架构|组织结构|部门架构|组织负责人|部门负责人).*")) {
                boost = Math.max(boost, 50);
            }
            if (entry.section.matches(".*(?:关键事实|人员|负责人).*")) boost = Math.max(boost, 220);
            else if (entry.section.matches(".*(?:组织|部门|业务意义|职责|分工|职能).*")) boost = Math.max(boost, 100);
            if (entry.normalizedText.matches(".*(?:部门负责人|项目部.*经销商业务部|部门分工|部门职责|组织分工|责任边界).*")) {
                boost = Math.max(boost, 60);
            }
        }
        if (question.matches(".*(?:人员组成|人员构成|成员组成|成员构成|具体人员|有[哪那]些人|都有谁|有谁|谁负责|"
                + "负责人|领导层|管理层|人员名单|成员名单).*")) {
            if (entry.section.matches(".*(?:关键事实|人员|负责人).*")) boost = Math.max(boost, 80);
            if (entry.normalizedText.matches(".*(?:部门负责人|董事长.*总经办|项目部.*经销商业务部).*")) {
                boost = Math.max(boost, 60);
            }
        }
        if (question.matches(".*(?:主要职责|职责是什么|有哪些职责|负责什么|做什么|承担什么工作|职能|工作范围|责任分工).*")) {
            if (entry.section.matches(".*(?:关键事实|组织|部门|业务意义|职责|分工|职能).*")) boost = Math.max(boost, 80);
            if (entry.normalizedText.matches(".*(?:组织架构|部门负责人|部门分工|责任边界|财务部|市场部|物流部).*")) {
                boost = Math.max(boost, 65);
            }
        }
        if (AssistantKnowledgeContext.isOrganizationCollaborationQuestion(question)) {
            if ("兴宇组织架构与负责人".equals(entry.title)) boost = Math.max(boost, 150);
            if (entry.section.matches(".*(?:关键事实|业务意义|组织|部门|分工).*")) boost = Math.max(boost, 100);
            if (entry.normalizedText.matches(".*(?:跨部门协作|协作|数据归属|复核|部门分工).*")) boost = Math.max(boost, 90);
        }
        if (AssistantKnowledgeContext.isOrganizationWorkOwnerQuestion(question)
                || AssistantKnowledgeContext.isOrganizationPersonWorkQuestion(question)) {
            if ("兴宇组织架构与负责人".equals(entry.title)) boost = Math.max(boost, 170);
            if (entry.section.matches(".*(?:关键事实|人员|负责人|组织|部门).*")) boost = Math.max(boost, 110);
        }
        if (AssistantKnowledgeContext.isCompanyLeadershipQuestion(question)) {
            if ("兴宇组织架构与负责人".equals(entry.title)) boost = Math.max(boost, 360);
            if (entry.section.matches(".*(?:核心结论|关键事实|决策层|负责人|组织).*")) {
                boost = Math.max(boost, 300);
            }
            if (entry.normalizedText.matches(".*(?:董事长|决策层|公司负责人|领导层|管理层).*")) {
                boost = Math.max(boost, 260);
            }
        }
        if (AssistantKnowledgeContext.isOrganizationHistoryQuestion(question)) {
            if ("兴宇人事变迁".equals(entry.title)) boost = Math.max(boost, 320);
            if (entry.section.matches(".*(?:关键事实|人事|变迁|历史).*")) boost = Math.max(boost, 250);
        }
        if (AssistantKnowledgeContext.isOrganizationRegionQuestion(question)) {
            if ("经销商业务区域分工".equals(entry.title)) boost = Math.max(boost, 320);
            if (entry.section.matches(".*(?:关键事实|区域|辖区|分工).*")) boost = Math.max(boost, 250);
        }
        return boost;
    }

    private static int topicBoost(Entry entry, AssistantCompanyKnowledgeIntent.Analysis intent) {
        if (intent == null || !intent.isKnowledge()) return 0;
        switch (intent.getTopic()) {
            case CHANNEL:
                if ("兴宇渠道网络".equals(entry.title)) return 420;
                if ("五步赋能门店法".equals(entry.title)) return 180;
                if (entry.normalizedTitle.matches(".*(?:渠道|门店|分销|经销).*")) return 140;
                break;
            case REGION:
                if ("兴宇区域市场格局".equals(entry.title)) return 440;
                if ("兴宇渠道网络".equals(entry.title)) return 360;
                if ("四川兴宇公司概况".equals(entry.title) || "西藏市场开发路径".equals(entry.title)) return 220;
                break;
            case ADVANTAGE:
                if ("四川兴宇公司概况".equals(entry.title)) return 320;
                if ("兴宇战略演变".equals(entry.title) || "DDC运营方法论".equals(entry.title)
                        || "五步赋能门店法".equals(entry.title)) return 240;
                break;
            case RELATIONSHIP:
                return relationshipKnowledgeBoost(entry) * 10;
            case BRAND:
                if ("兴宇品牌与产品线".equals(entry.title)) return 420;
                break;
            case PROFILE:
                if ("四川兴宇公司概况".equals(entry.title)) return 420;
                break;
            case HISTORY:
                if ("四川兴宇发展历程".equals(entry.title)) return 420;
                break;
            case STRATEGY:
                if ("兴宇战略演变".equals(entry.title)) return 420;
                break;
            case TRAINING:
                if (entry.normalizedTitle.matches(".*(?:新员工|学习路径|培训).*")) return 360;
                break;
            case CUSTOMER:
                if (entry.normalizedTitle.matches(".*(?:客户|门店|渠道).*")) return 220;
                break;
            case METHOD:
                if (entry.normalizedTitle.matches(".*(?:运营方法|赋能门店|数据驱动|标杆客户|销售话术).*")) return 260;
                break;
            default:
                break;
        }
        return 0;
    }

    private static boolean isOrganizationEntry(Entry entry) {
        return entry.path.contains("组织与人事")
                || entry.normalizedTitle.matches(".*(?:组织架构|组织结构|集团架构|部门负责人|人事变迁|区域分工).*" )
                || entry.normalizedText.matches(".*(?:组织架构|部门负责人|总经办|传媒运营部|经销商业务部|部门分工).*" );
    }

    private static Set<String> grams(String value) {
        String chinese = value.replaceAll("[^\\p{IsHan}]", "");
        Set<String> result = new LinkedHashSet<>();
        for (int size : new int[]{3, 2}) for (int offset = 0; offset + size <= chinese.length(); offset++) {
            String gram = chinese.substring(offset, offset + size);
            if (!GENERIC_QUERY_GRAMS.contains(gram)) result.add(gram);
        }
        return result;
    }

    private static Set<String> asciiTerms(String value) {
        Set<String> result = new LinkedHashSet<>(); Matcher matcher = ASCII_TERM.matcher(value);
        while (matcher.find()) result.add(matcher.group()); return result;
    }

    private static Set<String> expandedGrams(String question,
                                             AssistantCompanyKnowledgeIntent.Analysis knowledgeIntent) {
        Set<String> result = new LinkedHashSet<>();
        if (question.matches(".*(?:价格贵|太贵|贵了|嫌贵).*(?:怎么|如何|沟通|回应|解释|处理|说).*")
                || question.matches(".*(?:怎么|如何|沟通|回应|解释|处理|说).*(?:价格贵|太贵|贵了|嫌贵).*")) {
            result.addAll(grams("客户价格异议销售话术沟通处理"));
        }
        if (question.matches(".*(?:怎么选|如何选|选型).*(?:机油|产品).*")) {
            result.addAll(grams("产品选型选择方法"));
        }
        if (question.matches(".*(?:新人|新员工).*(?:学习|培训|路径).*")) {
            result.addAll(grams("新员工学习培训路径"));
        }
        if (AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(question)
                || AssistantKnowledgeContext.containsOrganizationLanguage(question)) {
            result.addAll(grams("组织架构部门分工部门职责部门负责人组织职责部门职能工作分工责任范围人员组成人员构成成员名单任职岗位分管范围"));
        }
        if (AssistantKnowledgeContext.isOrganizationCollaborationQuestion(question)) {
            result.addAll(grams("跨部门协作部门协同配合机制数据归属数据复核责任边界业务意义"));
        }
        if (AssistantKnowledgeContext.isOrganizationWorkOwnerQuestion(question)) {
            result.addAll(grams("工作归属部门负责人组织分工人员组成"));
        }
        if (AssistantKnowledgeContext.isOrganizationPersonWorkQuestion(question)) {
            result.addAll(grams("任职岗位分管范围兼管部门负责人工作分工"));
        }
        if (AssistantKnowledgeContext.isCompanyLeadershipQuestion(question)) {
            String role = AssistantKnowledgeContext.companyLeadershipRole(question);
            result.addAll(grams("董事长决策层公司负责人最高负责人领导层管理层"));
            if (role != null) result.addAll(grams(role));
        }
        if (AssistantKnowledgeContext.isOrganizationHistoryQuestion(question)) {
            result.addAll(grams("组织变迁人事变迁人员变化组织调整历史沿革"));
        }
        if (AssistantKnowledgeContext.isOrganizationRegionQuestion(question)) {
            result.addAll(grams("经销商业务区域分工辖区负责人区域划分"));
        }
        if (knowledgeIntent != null) switch (knowledgeIntent.getTopic()) {
            case CHANNEL:
                result.addAll(grams("兴宇渠道网络总部直管直营分公司合作分销商经销商八部制门店体系快准服务站微仓前置仓"));
                break;
            case REGION:
                result.addAll(grams("兴宇区域市场格局业务覆盖市场布局渠道网络成都四川西藏甘孜阿坝"));
                break;
            case ADVANTAGE:
                result.addAll(grams("四川兴宇公司概况核心优势竞争优势终端服务区域运营仓配协同标准化管理运营方法"));
                break;
            case RELATIONSHIP:
                result.addAll(grams("合作关系授权经销商核心合作品牌厂家运营体系"));
                break;
            case BRAND:
                result.addAll(grams("兴宇品牌与产品线合作品牌代理品牌经营品牌品牌矩阵"));
                break;
            case PROFILE:
                result.addAll(grams("四川兴宇公司概况主营业务经营定位公司介绍"));
                break;
            case HISTORY:
                result.addAll(grams("四川兴宇发展历程公司历史发展沿革"));
                break;
            case CUSTOMER:
                result.addAll(grams("客户类型客户群体服务对象终端门店经销商"));
                break;
            case TRAINING:
                result.addAll(grams("新员工学习路径培训路径公司制度业务知识"));
                break;
            case STRATEGY:
                result.addAll(grams("兴宇战略演变战略目标发展规划经营战略"));
                break;
            case METHOD:
                result.addAll(grams("经营方法运营方法DDC五步赋能门店数据驱动标杆客户销售话术"));
                break;
            default:
                break;
        }
        return result;
    }

    private static boolean isOrganizationModelFailure(AssistantKnowledgeContext.OrganizationIntent intent,
                                                      AssistantFailure failure) {
        return "MODEL_INVALID".equals(failure.getCode())
                && intent != AssistantKnowledgeContext.OrganizationIntent.NONE;
    }

    private static boolean isKnowledgeModelFailure(AssistantCompanyKnowledgeIntent.Analysis intent,
                                                   AssistantFailure failure) {
        return "MODEL_INVALID".equals(failure.getCode()) && intent != null && intent.isKnowledge();
    }

    /**
     * Keep reviewed company knowledge available when the model returns malformed JSON or invalid citations.
     * This fallback never invents facts: it only exposes focused excerpts from reviewed source chunks.
     */
    private Map<String, Object> knowledgeFallback(String retrievalQuestion,
                                                  AssistantCompanyKnowledgeIntent.Analysis intent,
                                                  List<Map<String, Object>> matches,
                                                  Map<String, Object> result) {
        List<Map<String, Object>> selected = new ArrayList<>();
        Set<String> paths = new LinkedHashSet<>();
        for (Map<String, Object> match : matches) {
            if (!"REVIEWED".equals(match.get("verificationStatus"))) continue;
            String path = String.valueOf(match.get("path"));
            if (!paths.add(path) && !selected.isEmpty()) continue;
            selected.add(match);
            if (selected.size() >= 2) break;
        }
        if (selected.isEmpty()) {
            throw new AssistantFailure("MODEL_INVALID", "模型未返回可核验的公司知识回答，请换一种问法");
        }

        List<Map<String, Object>> sources = new ArrayList<>();
        List<String> basis = new ArrayList<>();
        StringBuilder answer = new StringBuilder(knowledgeFallbackLead(intent));
        for (Map<String, Object> match : selected) {
            sources.add(sourceMetadata(match));
            String label = "[" + match.get("citation") + "]《" + match.get("title") + "》";
            if (!basis.contains(label)) basis.add(label);
            String excerpt = focusedKnowledgeExcerpt(retrievalQuestion, intent,
                    String.valueOf(match.get("text")));
            if (!excerpt.isEmpty()) answer.append("\n\n").append(excerpt).append(" [")
                    .append(match.get("citation")).append("]");
        }
        if (answer.toString().equals(knowledgeFallbackLead(intent))) {
            throw new AssistantFailure("MODEL_INVALID", "模型未返回可核验的公司知识回答，请换一种问法");
        }

        result.put("status", "SUCCESS");
        result.put("verificationStatus", "REVIEWED");
        result.put("answerText", answer.toString());
        result.put("sources", sources);
        result.put("basis", basis);
        Map<String, Object> knowledge = new LinkedHashMap<>();
        knowledge.put("title", "兴宇公司知识库");
        knowledge.put("description", "模型回答未通过来源校验，已依据审核资料生成摘录式回答");
        knowledge.put("source", "知识库/16-公司产品与销售知识");
        knowledge.put("version", version);
        knowledge.putAll(statistics());
        result.put("knowledge", knowledge);
        result.put("warnings", Collections.singletonList(
                "模型回答未通过来源校验，已改用已审核公司资料生成可核验回答"));
        return result;
    }

    private static String knowledgeFallbackLead(AssistantCompanyKnowledgeIntent.Analysis intent) {
        if (intent == null) return "根据现有已审核公司资料，可核对到以下内容：";
        switch (intent.getTopic()) {
            case RELATIONSHIP:
                return "根据现有已审核公司资料，双方合作关系可概括为：";
            case CHANNEL:
                return "根据现有已审核公司资料，公司的销售渠道和产品销售方式包括：";
            case REGION:
                return "根据现有已审核公司资料，公司的业务覆盖和区域布局包括：";
            case ADVANTAGE:
                return "根据现有已审核公司资料，公司的核心优势主要体现在：";
            default:
                return "根据现有已审核公司资料，可核对到以下内容：";
        }
    }

    private static String focusedKnowledgeExcerpt(String question,
                                                  AssistantCompanyKnowledgeIntent.Analysis intent,
                                                  String text) {
        if (text == null || text.trim().isEmpty()) return "";
        Set<String> terms = grams(normalize(question));
        terms.addAll(fallbackTopicTerms(intent));
        List<String> preferred = new ArrayList<>();
        List<String> usable = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = raw.trim()
                    .replaceFirst("^[-*+]\\s+", "")
                    .replaceFirst("^#{1,6}\\s*", "");
            if (line.isEmpty() || line.matches("^\\|?\\s*:?-{3,}.*")
                    || line.matches("^\\[\\[.*]]$")) continue;
            String normalizedLine = normalize(line);
            if (normalizedLine.isEmpty()) continue;
            usable.add(line);
            int hits = 0;
            for (String term : terms) if (normalizedLine.contains(term)) hits++;
            if (hits > 0) preferred.add(line);
        }
        List<String> chosen = preferred.isEmpty() ? usable : preferred;
        if (chosen.isEmpty()) return "";
        String excerpt = String.join("\n", chosen.subList(0, Math.min(3, chosen.size())));
        return excerpt.length() > 1200 ? excerpt.substring(0, 1200) + "…" : excerpt;
    }

    private static Set<String> fallbackTopicTerms(AssistantCompanyKnowledgeIntent.Analysis intent) {
        if (intent == null) return Collections.emptySet();
        switch (intent.getTopic()) {
            case RELATIONSHIP:
                return grams("合作关系授权经销商核心合作品牌产品经销厂家运营体系");
            case CHANNEL:
                return grams("销售渠道产品销售总部直营分销经销商门店网络仓配");
            case REGION:
                return grams("业务覆盖区域布局市场布局四川西藏成都甘孜阿坝");
            case ADVANTAGE:
                return grams("核心优势竞争优势终端服务区域运营仓配协同标准化管理");
            default:
                return Collections.emptySet();
        }
    }

    private Map<String, Object> companyLeadershipAnswer(String question, List<Map<String, Object>> matches,
                                                        Map<String, Object> result) {
        String role = AssistantKnowledgeContext.companyLeadershipRole(question);
        if (role == null) role = "公司负责人";
        List<Map<String, Object>> reviewed = new ArrayList<>();
        List<Map<String, Object>> factual = new ArrayList<>();
        List<String> people = new ArrayList<>();
        for (Map<String, Object> match : matches) {
            if (!"REVIEWED".equals(match.get("verificationStatus")) || !isOrganizationMatch(match)) continue;
            reviewed.add(match);
            List<String> found = leadershipPeople(role, String.valueOf(match.get("text")));
            if (found.isEmpty()) continue;
            factual.add(match);
            for (String person : found) if (!people.contains(person)) people.add(person);
        }
        List<Map<String, Object>> selected = people.isEmpty() ? reviewed : factual;
        if (selected.size() > 2) selected = new ArrayList<>(selected.subList(0, 2));

        List<Map<String, Object>> sources = new ArrayList<>();
        List<String> basis = new ArrayList<>();
        for (Map<String, Object> match : selected) {
            sources.add(sourceMetadata(match));
            String label = "[" + match.get("citation") + "]《" + match.get("title") + "》";
            if (!basis.contains(label)) basis.add(label);
        }

        String answer;
        if (people.isEmpty()) {
            answer = "现有已审核公司知识未明确提供“" + role + "”的姓名，不能用董事长或其他职务信息代替。";
            result.put("status", "KNOWLEDGE_NOT_FOUND");
            result.put("verificationStatus", "NO_EVIDENCE");
        } else {
            String time = leadershipTimeLabel(reviewed);
            String citation = String.valueOf(factual.get(0).get("citation"));
            answer = time + "，资料记录的" + role + "为" + String.join("、", people) + "。";
            if (AssistantKnowledgeContext.isColloquialCompanyLeadershipQuestion(question)) {
                answer += "这里按资料中的最高管理角色“董事长”口径理解该口语称谓。";
            }
            answer += " [" + citation + "]";
            result.put("status", "SUCCESS");
            result.put("verificationStatus", "REVIEWED");
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("unit", "公司");
            context.put("people", people);
            context.put("role", role);
            List<String> sourceIds = new ArrayList<>();
            for (Map<String, Object> match : factual) {
                String sourceId = String.valueOf(match.get("id"));
                if (!sourceIds.contains(sourceId)) sourceIds.add(sourceId);
            }
            context.put("sourceIds", sourceIds);
            result.put(INTERNAL_ORGANIZATION_CONTEXT, context);
        }
        result.put("answerText", answer);
        if (people.isEmpty()) result.put("emptyText", answer);
        result.put("sources", sources);
        result.put("basis", basis);
        Map<String, Object> knowledge = new LinkedHashMap<>();
        knowledge.put("title", "兴宇公司知识库");
        knowledge.put("description", "公司管理角色仅依据已审核组织资料精确提取，不混用不同职务");
        knowledge.put("source", "知识库/16-公司产品与销售知识");
        knowledge.put("version", version);
        knowledge.putAll(statistics());
        result.put("knowledge", knowledge);
        return result;
    }

    private static boolean isOrganizationMatch(Map<String, Object> match) {
        String path = String.valueOf(match.get("path"));
        String title = normalize(String.valueOf(match.get("title")));
        String text = normalize(String.valueOf(match.get("text")));
        return path.contains("组织与人事")
                || title.matches(".*(?:组织架构|组织结构|集团架构|部门负责人|人事变迁|区域分工).*")
                || text.matches(".*(?:组织架构|部门负责人|总经办|传媒运营部|经销商业务部|部门分工).*");
    }

    private static List<String> leadershipPeople(String role, String text) {
        if (role == null || text == null || text.trim().isEmpty()) return Collections.emptyList();
        String compact = text.replaceAll("[\\s*_`#>\\-]", "");
        String separator = "董事长".equals(role) ? "(?:是|为|：|:)?" : "(?:是|为|：|:)";
        Matcher matcher = Pattern.compile(Pattern.quote(role) + separator
                + "([\\p{IsHan}·]{2,4}?)(?=（|\\(|兼管|分管|负责|，|,|。|；|;|$)").matcher(compact);
        List<String> result = new ArrayList<>();
        while (matcher.find()) addPersonNames(result, matcher.group(1));
        return result;
    }

    private static String leadershipTimeLabel(List<Map<String, Object>> matches) {
        for (Map<String, Object> match : matches) {
            Matcher version = Pattern.compile("截至(\\d{4})年原文版本").matcher(String.valueOf(match.get("text")));
            if (version.find()) return "截至" + version.group(1) + "年原文版本";
        }
        for (Map<String, Object> match : matches) {
            Matcher updated = Pattern.compile("^(\\d{4})").matcher(String.valueOf(match.get("updated")));
            if (updated.find()) return "截至" + updated.group(1) + "年原文版本";
        }
        return "截至现有历史资料版本";
    }

    private Map<String, Object> organizationFallback(String currentQuestion, String retrievalQuestion,
                                                     AssistantKnowledgeContext.OrganizationIntent intent,
                                                     List<Map<String, Object>> matches,
                                                     Map<String, Object> result) {
        List<Map<String, Object>> selected = new ArrayList<>();
        for (Map<String, Object> match : matches) {
            if (!"REVIEWED".equals(match.get("verificationStatus"))) continue;
            selected.add(match);
            if (selected.size() >= 2) break;
        }
        if (selected.isEmpty()) throw new AssistantFailure("MODEL_INVALID", "模型未返回有效的公司知识来源");

        List<Map<String, Object>> sources = new ArrayList<>();
        List<String> basis = new ArrayList<>();
        StringBuilder answer = new StringBuilder(organizationFallbackLead(currentQuestion, retrievalQuestion,
                intent, selected));
        for (Map<String, Object> match : selected) {
            Map<String, Object> source = sourceMetadata(match);
            sources.add(source);
            String label = "[" + match.get("citation") + "]《" + match.get("title") + "》";
            if (!basis.contains(label)) basis.add(label);
            String excerpt = focusedOrganizationExcerpt(retrievalQuestion, String.valueOf(match.get("text")));
            if (!excerpt.isEmpty()) answer.append("\n\n").append(excerpt).append(" [")
                    .append(match.get("citation")).append("]");
        }
        result.put("status", "SUCCESS");
        result.put("verificationStatus", "REVIEWED");
        result.put("answerText", answer.toString());
        result.put("sources", sources);
        result.put("basis", basis);
        Map<String, Object> knowledge = new LinkedHashMap<>();
        knowledge.put("title", "兴宇公司知识库");
        knowledge.put("description", "模型回答未通过来源校验，已依据审核组织资料生成摘录式回答");
        knowledge.put("source", "知识库/16-公司产品与销售知识");
        knowledge.put("version", version);
        knowledge.putAll(statistics());
        result.put("knowledge", knowledge);
        result.put("warnings", Collections.singletonList("回答由已审核组织资料确定性摘录生成，未使用未通过校验的模型内容"));
        return result;
    }

    private static void attachOrganizationContext(Map<String, Object> result, String retrievalQuestion,
                                                  AssistantKnowledgeContext.OrganizationIntent intent,
                                                  List<Map<String, Object>> groundedMatches) {
        if (intent != AssistantKnowledgeContext.OrganizationIntent.RESPONSIBILITY) return;
        String unit = organizationUnit(retrievalQuestion);
        if (unit == null) return;
        List<String> people = new ArrayList<>();
        List<String> sourceIds = new ArrayList<>();
        for (Map<String, Object> match : groundedMatches) {
            if (!"REVIEWED".equals(match.get("verificationStatus"))) continue;
            List<String> entryPeople = responsiblePeople(unit, String.valueOf(match.get("text")));
            if (entryPeople.isEmpty()) continue;
            for (String person : entryPeople) if (!people.contains(person)) people.add(person);
            String sourceId = String.valueOf(match.get("id"));
            if (!sourceIds.contains(sourceId)) sourceIds.add(sourceId);
        }
        if (people.isEmpty()) return;
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("unit", unit);
        context.put("people", people);
        context.put("role", "负责人");
        context.put("sourceIds", sourceIds);
        result.put(INTERNAL_ORGANIZATION_CONTEXT, context);
    }

    private static String organizationUnit(String question) {
        if (question == null) return null;
        Matcher matcher = Pattern.compile("([\\p{IsHan}a-zA-Z0-9]{1,30}(?:事业部|分公司|办事处|中心|团队|小组|部门|部|办|组))")
                .matcher(question.replaceAll("[\\s，,。；;：:？?！!]", ""));
        String result = null;
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (!candidate.matches("(?:公司|组织|全部|所有|各|这些|上述)?部门")
                    && !candidate.matches("(?:公司|组织|团队|小组|中心|分公司|办事处)")) result = candidate;
        }
        return result;
    }

    private static List<String> responsiblePeople(String unit, String text) {
        if (unit == null || text == null || text.trim().isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                String[] cells = trimmed.split("\\|", -1);
                if (cells.length >= 4 && normalizeCell(cells[1]).equals(normalizeCell(unit))) {
                    addPersonNames(result, cells[2]);
                }
                continue;
            }
            String[] publishedCells = trimmed.split("\\s{2,}");
            if (publishedCells.length >= 2
                    && normalizeCell(publishedCells[0]).equals(normalizeCell(unit))) {
                addPersonNames(result, publishedCells[1]);
                continue;
            }
            String compactLine = trimmed.replaceAll("[\\s*_`#>\\-]", "");
            Matcher owner = Pattern.compile(Pattern.quote(unit)
                    + "(?:的)?(?:负责人(?:是|为)?|由)?([\\p{IsHan}·]{2,4}(?:[/、][\\p{IsHan}·]{2,4})*)"
                    + "(?=负责|兼管|分管|（|\\(|$)").matcher(compactLine);
            if (owner.find()) addPersonNames(result, owner.group(1));
        }
        return result;
    }

    private static String normalizeCell(String value) {
        return value == null ? "" : value.replaceAll("[\\s*_`]", "");
    }

    private static void addPersonNames(List<String> result, String value) {
        if (value == null) return;
        String cleaned = value.replaceAll("[（(].*$", "").trim();
        for (String part : cleaned.split("[/、,，]")) {
            String person = part.replaceAll("[\\s*_`]", "");
            if (person.matches("[\\p{IsHan}·]{2,4}") && !result.contains(person)) result.add(person);
        }
    }

    private static Map<String, Object> sourceMetadata(Map<String, Object> match) {
        Map<String, Object> source = new LinkedHashMap<>();
        for (String key : Arrays.asList("id", "title", "section", "path", "updated",
                "verificationStatus", "contentHash", "citation")) source.put(key, match.get(key));
        return source;
    }

    private static String organizationFallbackLead(String question, String retrievalQuestion,
                                                   AssistantKnowledgeContext.OrganizationIntent intent,
                                                   List<Map<String, Object>> selected) {
        if (intent == AssistantKnowledgeContext.OrganizationIntent.PEOPLE) {
            return "现有资料未完整列出该组织单元的所有成员；以下仅列出资料中明确记录的负责人或人员信息：";
        }
        if (intent == AssistantKnowledgeContext.OrganizationIntent.DUTY) {
            return "现有资料未详细提供该组织单元的具体职责；以下仅列出资料中已有的负责人、人员组成或分工背景：";
        }
        if (intent == AssistantKnowledgeContext.OrganizationIntent.COLLABORATION) {
            return "现有资料未详细提供部门之间的具体协作流程；以下仅列出资料中已有的组织分工和协作背景：";
        }
        if (intent == AssistantKnowledgeContext.OrganizationIntent.PERSON_WORK
                && AssistantKnowledgeContext.isPersonReferenceQuestion(question)) {
            String person = confirmedPerson(retrievalQuestion);
            String label = person == null ? "该人员" : "“" + person + "”";
            return "现有资料仅明确提供以下与" + label
                    + "相关的任职或分工记录；除此之外，资料未提供其他明确工作分工：";
        }
        String subject = organizationSubject(question);
        if (subject != null) {
            String normalizedSubject = normalize(subject);
            boolean found = selected.stream().anyMatch(item -> normalize(String.valueOf(item.get("text"))).contains(normalizedSubject));
            if (!found) return "现有已审核组织资料未提供与“" + subject + "”直接对应的任职或工作归属；可核对的组织背景如下：";
        }
        return "根据现有已审核组织资料，可核对到以下记录（历史资料，不代表实时通讯录）：";
    }

    private static String confirmedPerson(String retrievalQuestion) {
        if (retrievalQuestion == null) return null;
        Matcher matcher = Pattern.compile("已确认负责人[：:]([\\p{IsHan}·]{2,4})(?:[、；;]|$)")
                .matcher(retrievalQuestion);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String organizationSubject(String question) {
        String text = question == null ? "" : question.replaceAll("[\\s，,。；;：:？?！!]", "");
        Matcher person = Pattern.compile("^(?:那|那么|请问|想了解|我想知道)?([\\p{IsHan}·]{2,4}?)(?:的)?"
                + "(?:工作|岗位|职务|任职|分管范围|负责什么|做什么|分管什么).*$").matcher(text);
        if (person.matches()) return person.group(1);
        Matcher owner = Pattern.compile("^(?:那|那么|请问|想了解|我想知道)?(?:谁(?:来|在)?负责)?"
                + "([\\p{IsHan}a-zA-Z0-9]{1,24})(?:工作|业务|事务|职能)(?:由谁负责|谁负责|负责人是谁|呢|吗)?$").matcher(text);
        return owner.matches() ? owner.group(1) : null;
    }

    private static String focusedOrganizationExcerpt(String question, String text) {
        if (text == null || text.trim().isEmpty()) return "";
        Set<String> terms = grams(normalize(question));
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String normalizedLine = normalize(line);
            if (normalizedLine.isEmpty()) continue;
            int hits = 0;
            for (String term : terms) if (normalizedLine.contains(term)) hits++;
            if (hits > 0) lines.add(line.trim());
            if (lines.size() >= 4) break;
        }
        String excerpt = lines.isEmpty() ? text.trim() : String.join("\n", lines);
        if (excerpt.length() > 1200) excerpt = excerpt.substring(0, 1200) + "…";
        return excerpt;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。；：！？、‘’“”（）【】]+", "");
    }

    static String isolateInstructions(String value) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (String line : value.split("\\R", -1)) {
            String safe = REFERENCE_INSTRUCTION.matcher(line).matches() ? "[非事实指令已隔离]" : line;
            if (result.length() > 0) result.append('\n');
            result.append(safe);
        }
        return result.toString();
    }

    private static String safeMetadata(String value, int maxLength) {
        if (value == null) return "";
        String safe = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
        if (REFERENCE_INSTRUCTION.matcher(safe).matches()) return "[非事实元数据已隔离]";
        return safe.substring(0, Math.min(maxLength, safe.length()));
    }

    private static String ensureCitation(String answer, List<Map<String, Object>> sources) {
        if (answer == null) return "";
        if (answer.matches("(?s).*\\[S\\d+].*")) return answer;
        List<String> citations = new ArrayList<>();
        for (Map<String, Object> source : sources) citations.add("[" + source.get("citation") + "]");
        return answer + "\n\n来源依据：" + String.join("、", citations);
    }

    private static String sha256(byte[] bytes) throws Exception {
        StringBuilder value = new StringBuilder();
        for (byte item : MessageDigest.getInstance("SHA-256").digest(bytes)) value.append(String.format("%02x", item));
        return value.toString();
    }
}
