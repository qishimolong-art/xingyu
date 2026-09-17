package cn.iocoder.yudao.module.erp.dal.mysql.product;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
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

}
