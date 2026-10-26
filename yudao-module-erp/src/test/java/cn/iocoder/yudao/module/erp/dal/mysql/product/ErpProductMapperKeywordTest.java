package cn.iocoder.yudao.module.erp.dal.mysql.product;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpProductMapperKeywordTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpProductMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpProductDO.class);
    }

    @Test
    void appendStockKeywordCondition_matchesProductPinyinAndWubiCodes() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendStockKeywordCondition(wrapper, "XYPJ");

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("pinyin_code"), sql);
        assertTrue(sql.contains("wubi_code"), sql);
    }

    @Test
    void appendStockKeywordCondition_expandsCompactViscosity() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendStockKeywordCondition(wrapper, "530");

        wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%530%"), values.toString());
        assertTrue(values.contains("%5W-30%"), values.toString());
        assertTrue(values.contains("%5%3%0%"), values.toString());
    }

    @Test
    void appendProductStockListKeywordCondition_keepsPureNumberContinuous() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendProductStockListKeywordCondition(wrapper, "138145");

        wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%138145%"), values.toString());
        assertFalse(values.contains("%1%3%8%1%4%5%"), values.toString());
    }

    @Test
    void appendProductStockListKeywordCondition_doesNotExpandCompactViscosity() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendProductStockListKeywordCondition(wrapper, "美孚 530");

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains(" AND "), sql);
        assertTrue(values.contains("%美孚%530%"), values.toString());
        assertTrue(values.contains("%美孚%"), values.toString());
        assertTrue(values.contains("%530%"), values.toString());
        assertFalse(values.contains("%5W-30%"), values.toString());
        assertFalse(values.contains("%5%3%0%"), values.toString());
    }

    @Test
    void appendProductStockListKeywordCondition_keepsLiteralViscosityAndExplicitTokens() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendProductStockListKeywordCondition(wrapper, "美孚/5W-30");

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains(" AND "), sql);
        assertTrue(values.contains("%美孚%"), values.toString());
        assertTrue(values.contains("%5W-30%"), values.toString());
        assertFalse(values.contains("%美孚/5W-30%"), values.toString());
    }

    @Test
    void appendStockProductNameKeywordCondition_combinesTokensButOnlySearchesName() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendStockProductNameKeywordCondition(wrapper, "经典表现 530");

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("name"), sql);
        assertTrue(sql.contains(" AND "), sql);
        assertTrue(sql.contains(" OR "), sql);
        assertFalse(sql.contains("code"), sql);
        assertFalse(sql.contains("factory_code"), sql);
        assertFalse(sql.contains("brand"), sql);
        assertFalse(sql.contains("vehicle_model"), sql);
        assertTrue(values.contains("%经典表现%"), values.toString());
        assertTrue(values.contains("%经%典%表%现%"), values.toString());
        assertTrue(values.contains("%530%"), values.toString());
        assertTrue(values.contains("%5W-30%"), values.toString());
        assertTrue(values.contains("%5W30%"), values.toString());
        assertTrue(values.contains("%5-30%"), values.toString());
        assertTrue(values.contains("%5%3%0%"), values.toString());
    }

    @Test
    void appendStockProductNameKeywordCondition_keepsOrderedCharacterDirection() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendStockProductNameKeywordCondition(wrapper, "305");

        wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%3%0%5%"), values.toString());
        assertFalse(values.contains("%5%3%0%"), values.toString());
        assertFalse(values.contains("%5W-30%"), values.toString());
    }

    @Test
    void appendStockProductCodeNameKeywordCondition_combinesTokensAcrossCodeAndNameOnly() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendStockProductCodeNameKeywordCondition(wrapper, "BKR6E 530");

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("code"), sql);
        assertTrue(sql.contains("name"), sql);
        assertTrue(sql.contains(" AND "), sql);
        assertTrue(sql.contains(" OR "), sql);
        assertFalse(sql.contains("pinyin_code"), sql);
        assertFalse(sql.contains("wubi_code"), sql);
        assertFalse(sql.contains("bar_code"), sql);
        assertFalse(sql.contains("factory_code"), sql);
        assertFalse(sql.contains("brand"), sql);
        assertFalse(sql.contains("vehicle_model"), sql);
        assertFalse(sql.contains("standard"), sql);
        assertTrue(values.contains("%BKR6E%"), values.toString());
        assertTrue(values.contains("%B%K%R%6%E%"), values.toString());
        assertTrue(values.contains("%530%"), values.toString());
        assertTrue(values.contains("%5W-30%"), values.toString());
        assertTrue(values.contains("%5W30%"), values.toString());
        assertTrue(values.contains("%5-30%"), values.toString());
        assertTrue(values.contains("%5%3%0%"), values.toString());
    }

    @Test
    void appendStockProductCodeNameKeywordCondition_keepsOrderedCharacterDirection() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendStockProductCodeNameKeywordCondition(wrapper, "305");

        wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%3%0%5%"), values.toString());
        assertFalse(values.contains("%5%3%0%"), values.toString());
        assertFalse(values.contains("%5W-30%"), values.toString());
    }

    @Test
    void appendKeywordCondition_addsOrderedCharacterFallbackForSingleKeyword() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendKeywordCondition(wrapper, "QTF60",
                Collections.singletonList(ErpProductMapper.KEYWORD_FIELD_NAME),
                Collections.emptyList());

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains(" OR "), sql);
        assertTrue(values.contains("%QTF60%"), values.toString());
        assertTrue(values.contains("%Q%T%F%6%0%"), values.toString());
        assertFalse(values.contains("%6%0%Q%T%F%"), values.toString());
    }

    @Test
    void appendKeywordCondition_productArchivePureNumberDisablesOrderedCharacterFallback() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendKeywordCondition(wrapper, "145988",
                Arrays.asList(
                        ErpProductMapper.KEYWORD_FIELD_CODE,
                        ErpProductMapper.KEYWORD_FIELD_NAME,
                        ErpProductMapper.KEYWORD_FIELD_PINYIN_CODE,
                        ErpProductMapper.KEYWORD_FIELD_WUBI_CODE,
                        ErpProductMapper.KEYWORD_FIELD_VEHICLE_MODEL,
                        ErpProductMapper.KEYWORD_FIELD_BAR_CODE,
                        ErpProductMapper.KEYWORD_FIELD_FACTORY_CODE,
                        ErpProductMapper.KEYWORD_FIELD_DRAWING_NO),
                Collections.emptyList(), true);

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%145988%"), values.toString());
        assertFalse(values.contains("%1%4%5%9%8%8%"), values.toString());
        assertTrue(sql.contains("code"), sql);
        assertTrue(sql.contains("name"), sql);
        assertTrue(sql.contains("pinyin_code"), sql);
        assertTrue(sql.contains("wubi_code"), sql);
        assertTrue(sql.contains("vehicle_model"), sql);
        assertTrue(sql.contains("bar_code"), sql);
        assertTrue(sql.contains("factory_code"), sql);
        assertTrue(sql.contains("drawing_no"), sql);
        assertFalse(sql.contains("standard"), sql);
        assertFalse(sql.contains("brand"), sql);
        assertFalse(sql.contains("erp_product_unit"), sql);
    }

    @Test
    void appendKeywordCondition_productArchiveKeepsViscosityButNotNumericOrderedMatch() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendKeywordCondition(wrapper, "经典表现 530",
                Collections.singletonList(ErpProductMapper.KEYWORD_FIELD_NAME),
                Collections.emptyList(), true);

        wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%经典表现%"), values.toString());
        assertTrue(values.contains("%经%典%表%现%"), values.toString());
        assertTrue(values.contains("%530%"), values.toString());
        assertTrue(values.contains("%5W-30%"), values.toString());
        assertTrue(values.contains("%5W30%"), values.toString());
        assertTrue(values.contains("%5-30%"), values.toString());
        assertFalse(values.contains("%5%3%0%"), values.toString());
    }

    @Test
    void orderByIfPresent_productArchivePrioritizesExactCodeOnlyWithoutUserSort() {
        ErpProductPageReqVO reqVO = new ErpProductPageReqVO();
        reqVO.setKeyword("145988");
        reqVO.setKeywordSearchMode(ErpProductPageReqVO.KEYWORD_SEARCH_MODE_PRODUCT_ARCHIVE);
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.orderByIfPresent(wrapper, reqVO,
                Collections.singletonList(ErpProductMapper.KEYWORD_FIELD_CODE));

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("CASE WHEN code = '145988' THEN 0 ELSE 1 END"), sql);

        reqVO.setOrderField("name");
        reqVO.setOrderDirection("asc");
        LambdaQueryWrapperX<ErpProductDO> sortedWrapper = new LambdaQueryWrapperX<>();
        ErpProductMapper.orderByIfPresent(sortedWrapper, reqVO,
                Collections.singletonList(ErpProductMapper.KEYWORD_FIELD_CODE));
        String sortedSql = sortedWrapper.getCustomSqlSegment();
        assertFalse(sortedSql.contains("CASE WHEN"), sortedSql);
        assertTrue(sortedSql.contains("name ASC"), sortedSql);
    }

    @Test
    void orderByIfPresent_productArchiveDoesNotUseHiddenCodeForPriority() {
        ErpProductPageReqVO reqVO = new ErpProductPageReqVO();
        reqVO.setKeyword("145988");
        reqVO.setKeywordSearchMode(ErpProductPageReqVO.KEYWORD_SEARCH_MODE_PRODUCT_ARCHIVE);
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.orderByIfPresent(wrapper, reqVO,
                Collections.singletonList(ErpProductMapper.KEYWORD_FIELD_NAME));

        String sql = wrapper.getCustomSqlSegment();
        assertFalse(sql.contains("CASE WHEN"), sql);
        assertTrue(sql.contains("id DESC"), sql);
    }

    @Test
    void orderByIfPresent_productArchiveRejectsUnsafeExactCodeExpression() {
        ErpProductPageReqVO reqVO = new ErpProductPageReqVO();
        reqVO.setKeyword("A' OR 1=1");
        reqVO.setKeywordSearchMode(ErpProductPageReqVO.KEYWORD_SEARCH_MODE_PRODUCT_ARCHIVE);
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.orderByIfPresent(wrapper, reqVO,
                Collections.singletonList(ErpProductMapper.KEYWORD_FIELD_CODE));

        String sql = wrapper.getCustomSqlSegment();
        assertFalse(sql.contains("CASE WHEN"), sql);
        assertFalse(sql.contains("OR 1=1"), sql);
        assertTrue(sql.contains("id DESC"), sql);
    }

    @Test
    void appendStockKeywordCondition_matchesProductUnitName() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendStockKeywordCondition(wrapper, "钻石205/根");

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("erp_product_unit"), sql);
        assertTrue(values.contains("%钻石205%"), values.toString());
        assertTrue(values.contains("%根%"), values.toString());
    }

    @Test
    void appendStockKeywordCondition_stripsTrailingExplicitDelimiter() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendStockKeywordCondition(wrapper, "极护SP新包装,");

        wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%极护SP新包装%"), values.toString());
        assertFalse(values.contains("%极护SP新包装,%"), values.toString());
    }

    @Test
    void appendStockKeywordCondition_usesAndAcrossExplicitProductTokens() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendStockKeywordCondition(wrapper, "宝马/刹车片");

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("brand"), sql);
        assertTrue(sql.contains("name"), sql);
        assertTrue(sql.contains("AND"), sql);
        assertTrue(values.contains("%宝马%"), values.toString());
        assertTrue(values.contains("%刹车片%"), values.toString());
        assertFalse(values.contains("%宝马/刹车片%"), values.toString());
    }

    @Test
    void appendStockKeywordCondition_keepsSpaceWildcardAndAddsProductTokens() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendStockKeywordCondition(wrapper, "ILK 7B11");

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("OR"), sql);
        assertTrue(values.contains("%ILK%7B11%"), values.toString());
        assertTrue(values.contains("%ILK%"), values.toString());
        assertTrue(values.contains("%7B11%"), values.toString());
    }

    @Test
    void appendStockKeywordCondition_doesNotSplitExplicitTokensAcrossWarehouseOrDeptFields() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendStockKeywordCondition(wrapper, "宝马/刹车片/大塘仓");

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("brand"), sql);
        assertTrue(sql.contains("name"), sql);
        assertFalse(sql.contains("system_dept"), sql);
        assertFalse(sql.contains("DATE_FORMAT"), sql);
    }

    @Test
    void appendKeywordCondition_keepsSpaceWildcardAndAddsCrossFieldTokens() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendKeywordCondition(wrapper, "ILK 7B11",
                Arrays.asList(
                        ErpProductMapper.KEYWORD_FIELD_CODE,
                        ErpProductMapper.KEYWORD_FIELD_NAME,
                        ErpProductMapper.KEYWORD_FIELD_BRAND),
                Collections.emptyList());

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("OR"), sql);
        assertTrue(values.contains("%ILK%7B11%"), values.toString());
        assertTrue(values.contains("%ILK%"), values.toString());
        assertTrue(values.contains("%7B11%"), values.toString());
    }

    @Test
    void appendKeywordCondition_usesAndAcrossExplicitDelimiterTokens() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendKeywordCondition(wrapper, "宝马/5系/刹车片",
                Arrays.asList(
                        ErpProductMapper.KEYWORD_FIELD_BRAND,
                        ErpProductMapper.KEYWORD_FIELD_VEHICLE_MODEL,
                        ErpProductMapper.KEYWORD_FIELD_NAME),
                Collections.emptyList());

        String sql = wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(sql.contains("brand"), sql);
        assertTrue(sql.contains("vehicle_model"), sql);
        assertTrue(sql.contains("name"), sql);
        assertTrue(values.contains("%宝马%"), values.toString());
        assertTrue(values.contains("%5系%"), values.toString());
        assertTrue(values.contains("%刹车片%"), values.toString());
        assertFalse(values.contains("%宝马/5系/刹车片%"), values.toString());
    }

    @Test
    void appendKeywordCondition_honorsKeywordFieldScopeAcrossTokens() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendKeywordCondition(wrapper, "宝马/刹车片",
                Collections.singletonList(ErpProductMapper.KEYWORD_FIELD_NAME),
                Collections.emptyList());

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("name"), sql);
        assertFalse(sql.contains("brand"), sql);
    }

    @Test
    void appendKeywordCondition_expandsCompactViscosityButKeepsOriginalKeyword() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendKeywordCondition(wrapper, "530",
                Arrays.asList(
                        ErpProductMapper.KEYWORD_FIELD_CODE,
                        ErpProductMapper.KEYWORD_FIELD_NAME,
                        ErpProductMapper.KEYWORD_FIELD_STANDARD),
                Collections.emptyList());

        wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%530%"), values.toString());
        assertTrue(values.contains("%5W-30%"), values.toString());
        assertTrue(values.contains("%5W30%"), values.toString());
    }

    @Test
    void appendKeywordCondition_usesOrAcrossCompactViscosityVariants() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendKeywordCondition(wrapper, "530",
                Collections.singletonList(ErpProductMapper.KEYWORD_FIELD_NAME),
                Collections.emptyList());

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains(" OR "), sql);
        assertFalse(sql.contains(" AND "), sql);
    }

    @Test
    void appendKeywordCondition_expandsCompactViscosityInsideCombinedSearch() {
        LambdaQueryWrapperX<ErpProductDO> wrapper = new LambdaQueryWrapperX<>();

        ErpProductMapper.appendKeywordCondition(wrapper, "经典表现 530 4L",
                Arrays.asList(
                        ErpProductMapper.KEYWORD_FIELD_CODE,
                        ErpProductMapper.KEYWORD_FIELD_NAME,
                        ErpProductMapper.KEYWORD_FIELD_STANDARD),
                Collections.emptyList());

        wrapper.getCustomSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.contains("%经典表现%530%4L%"), values.toString());
        assertTrue(values.contains("%经典表现%5W-30%4L%"), values.toString());
        assertTrue(values.contains("%530%"), values.toString());
        assertTrue(values.contains("%5W-30%"), values.toString());
        assertTrue(values.contains("%4L%"), values.toString());
        assertTrue(values.contains("%经%典%表%现%"), values.toString());
        assertTrue(values.contains("%5%3%0%"), values.toString());
        assertTrue(values.contains("%4%L%"), values.toString());
    }

}
