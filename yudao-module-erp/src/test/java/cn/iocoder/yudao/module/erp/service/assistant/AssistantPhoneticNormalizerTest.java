package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class AssistantPhoneticNormalizerTest {

    @Test void correctsControlledHomophonesAndKeepsSpecsUntouched() {
        AssistantPhoneticNormalizer normalizer=normalizer();

        AssistantPhoneticNormalizer.Normalization product=normalizer.normalize("金典表现欧系 5W-30 API SP 多少钱");
        AssistantPhoneticNormalizer.Normalization metric=normalizer.normalize("本月销瘦额是多少");

        assertEquals("经典表现欧系 5W-30 API SP 多少钱",product.getNormalizedQuestion());
        assertEquals("本月销售额是多少",metric.getNormalizedQuestion());
        assertEquals("ALIAS",product.getCorrections().get(0).get("matchType"));
        assertEquals("NEAR_SOUND",normalizer.normalize("金点表现有什么认证")
                .getCorrections().get(0).get("matchType"));
        assertTrue(product.getNormalizedQuestion().contains("5W-30 API SP"));
    }

    @Test void supportsControlledNearSoundsButDoesNotRewriteLiteralCanonicalWords() {
        AssistantPhoneticNormalizer normalizer=normalizer();

        AssistantPhoneticNormalizer.Normalization near=normalizer.normalize("查询库存娘");
        AssistantPhoneticNormalizer.Normalization literal=normalizer.normalize("查询库存量");

        assertEquals("查询库存量",near.getNormalizedQuestion());
        assertEquals("NEAR_SOUND",near.getCorrections().get(0).get("matchType"));
        assertEquals("查询库存量",literal.getNormalizedQuestion());
        assertTrue(literal.getCorrections().isEmpty());
    }

    @Test void ignoresSingleCharactersCodesDatesAndPureEnglish() {
        AssistantPhoneticNormalizer normalizer=normalizer();
        String question="查金 A-100 2026-10-06 5W-30 API SP";

        AssistantPhoneticNormalizer.Normalization result=normalizer.normalize(question);

        assertEquals(question,result.getNormalizedQuestion());
        assertTrue(result.getCorrections().isEmpty());
    }

    @Test void correctsTianOnlyInsideControlledRollingDateExpression() {
        AssistantPhoneticNormalizer normalizer=normalizer();

        AssistantPhoneticNormalizer.Normalization rolling=normalizer.normalize("最近7填的销售额");
        AssistantPhoneticNormalizer.Normalization literal=normalizer.normalize("请填写最近7天的说明");

        assertEquals("最近7天的销售额",rolling.getNormalizedQuestion());
        assertEquals("填",rolling.getCorrections().get(0).get("original"));
        assertEquals("天",rolling.getCorrections().get(0).get("corrected"));
        assertEquals("CONTROLLED_TEMPORAL_PATTERN",rolling.getCorrections().get(0).get("source"));
        assertEquals("请填写最近7天的说明",literal.getNormalizedQuestion());
        assertTrue(literal.getCorrections().isEmpty());
    }

    @Test void ambiguousSameTierTermsRequireAChoiceBeforeExecution() throws Exception {
        String json="{\"schemaVersion\":1,\"version\":\"test\",\"terms\":["
                +"{\"canonical\":\"金典表现\",\"category\":\"KNOWLEDGE\",\"aliases\":[],\"allowNearSound\":true},"
                +"{\"canonical\":\"津点表显\",\"category\":\"KNOWLEDGE\",\"aliases\":[],\"allowNearSound\":true}]}";
        byte[] bytes=json.getBytes(StandardCharsets.UTF_8);
        AssistantPhoneticNormalizer normalizer=new AssistantPhoneticNormalizer(properties(),bytes,sha256(bytes));

        AssistantPhoneticNormalizer.Normalization result=normalizer.normalize("今点表现有什么认证");

        assertTrue(result.needsClarification());
        assertEquals(2,result.getAmbiguousChoices().size());
        assertTrue(result.getAmbiguousChoices().containsAll(Arrays.asList("金典表现","津点表显")));
        assertEquals("PHONETIC_AMBIGUOUS",result.clarification().get("reasonCode"));
    }

    @Test void entityMatcherRanksExactHomophoneBeforeNearSound() {
        AssistantPhoneticNormalizer normalizer=normalizer();

        AssistantPhoneticNormalizer.EntityMatch strict=normalizer.matchEntity(
                "金典表现","美孚1号经典表现欧系 5W-30");
        AssistantPhoneticNormalizer.EntityMatch near=normalizer.matchEntity(
                "库存娘","库存量");

        assertNotNull(strict);assertEquals("NEAR_PHONETIC",strict.getMatchType());
        assertNotNull(near);assertEquals("NEAR_PHONETIC",near.getMatchType());
        AssistantPhoneticNormalizer.EntityMatch exact=normalizer.matchEntity("成都路通原","成都路通源");
        assertNotNull(exact);assertEquals("PHONETIC",exact.getMatchType());
        assertTrue(exact.getConfidence()>near.getConfidence());
    }

    private static AssistantPhoneticNormalizer normalizer() {
        AssistantPhoneticNormalizer value=new AssistantPhoneticNormalizer();
        ReflectionTestUtils.setField(value,"properties",properties());
        ReflectionTestUtils.setField(value,"companyKnowledge",null);
        return value;
    }

    private static AssistantProperties properties() {
        AssistantProperties value=new AssistantProperties();
        value.getPhonetic().setEnabled(true);
        value.getPhonetic().setNearSoundEnabled(true);
        return value;
    }

    private static String sha256(byte[] bytes) throws Exception {
        StringBuilder result=new StringBuilder();
        for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes)) result.append(String.format("%02x",value));
        return result.toString();
    }
}
