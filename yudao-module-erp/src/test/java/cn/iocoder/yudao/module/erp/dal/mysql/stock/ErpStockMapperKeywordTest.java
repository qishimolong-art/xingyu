package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockMapperKeywordTest {

    @Test
    void crossDimensionKeyword_supportsAllExistingExplicitDelimiters() {
        ErpKeywordQuery.KeywordSearch keywordSearch = ErpKeywordQuery.parseKeywordSearch(
                "美孚/轮胎\\甘孜,成都，兴宇;路通；批次+现货|仓库");

        assertTrue(keywordSearch.explicitDelimited());
        assertEquals(Arrays.asList("美孚", "轮胎", "甘孜", "成都", "兴宇", "路通", "批次", "现货", "仓库"),
                keywordSearch.tokens());
    }

    @Test
    void appendKeywordCondition_batchModeSearchesBatchNo() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setShowBatchNo(true);
        reqVO.setKeyword(" PC 202607 ");
        QueryWrapperX<ErpStockDO> wrapper = new QueryWrapperX<>();
        Map<Long, Set<Long>> batchKeywordStockKeyMap = new LinkedHashMap<>();
        batchKeywordStockKeyMap.put(10L, new LinkedHashSet<>(Collections.singletonList(20L)));

        boolean appended = ErpStockMapper.appendKeywordCondition(
                wrapper, Collections.emptyList(), Collections.emptyList(), batchKeywordStockKeyMap);

        assertTrue(appended);
        assertFalse(wrapper.getCustomSqlSegment().contains("erp_stock_record"));
        assertTrue(wrapper.getCustomSqlSegment().contains("product_id"));
        assertTrue(wrapper.getCustomSqlSegment().contains("warehouse_id"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(10L));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(20L));
    }

    @Test
    void appendKeywordCondition_nonBatchModeKeepsOriginalEmptyResult() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setShowBatchNo(false);
        reqVO.setKeyword("PC202607");
        QueryWrapperX<ErpStockDO> wrapper = new QueryWrapperX<>();

        boolean appended = ErpStockMapper.appendKeywordCondition(
                wrapper, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());

        assertFalse(appended);
        assertTrue(wrapper.getCustomSqlSegment().isEmpty());
    }

    @Test
    void appendCrossDimensionKeywordCondition_andsTokenGroupsAndOrsDimensions() {
        QueryWrapperX<ErpStockDO> wrapper = new QueryWrapperX<>();
        Map<Long, Set<Long>> batchScope = new LinkedHashMap<>();
        batchScope.put(11L, Collections.singleton(30L));
        List<ErpStockKeywordMatchScope> keywordScopes = Arrays.asList(
                new ErpStockKeywordMatchScope(Collections.singletonList(10L),
                        Collections.emptyList(), null),
                new ErpStockKeywordMatchScope(Collections.singletonList(10L),
                        Collections.singletonList(20L), null),
                new ErpStockKeywordMatchScope(Collections.emptyList(),
                        Collections.emptyList(), batchScope));

        boolean appended = ErpStockMapper.appendCrossDimensionKeywordCondition(wrapper, keywordScopes);

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(appended);
        assertTrue(sql.contains("product_id"));
        assertTrue(sql.contains("warehouse_id"));
        assertTrue(sql.contains(" AND "));
        assertTrue(sql.contains(" OR "));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(10L));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(20L));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(11L));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(30L));
    }

    @Test
    void appendCrossDimensionKeywordCondition_rejectsAnyUnmatchedToken() {
        QueryWrapperX<ErpStockDO> wrapper = new QueryWrapperX<>();
        List<ErpStockKeywordMatchScope> keywordScopes = Arrays.asList(
                new ErpStockKeywordMatchScope(Collections.singletonList(10L),
                        Collections.emptyList(), null),
                new ErpStockKeywordMatchScope(Collections.emptyList(),
                        Collections.emptyList(), null));

        boolean appended = ErpStockMapper.appendCrossDimensionKeywordCondition(wrapper, keywordScopes);

        assertFalse(appended);
    }

    @Test
    void buildStockQueryWrapper_combinesNameFiltersAndStrictBatchStockKeysWithAnd() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setWarehouseName(" 甘孜 ");
        reqVO.setDeptName(" 兴宇路通 ");
        Map<Long, Set<Long>> batchNoStockKeyMap = new LinkedHashMap<>();
        batchNoStockKeyMap.put(10L, new LinkedHashSet<>(Collections.singletonList(20L)));

        ErpStockMapper mapper = org.mockito.Mockito.mock(
                ErpStockMapper.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        QueryWrapperX<ErpStockDO> wrapper = mapper.buildStockQueryWrapper(
                reqVO, null, Collections.singletonList(20L),
                null, null, null, null, null, null, null, batchNoStockKeyMap);

        String sql = wrapper.getCustomSqlSegment();
        assertTrue(sql.contains("erp_warehouse"));
        assertTrue(sql.contains("system_dept"));
        assertTrue(sql.contains("product_id"));
        assertTrue(sql.contains("warehouse_id"));
        assertTrue(sql.contains(" AND "));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("%甘孜%"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("%兴宇路通%"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(10L));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(20L));
    }

    @Test
    void buildStockQueryWrapper_limitsSelectedStockIds() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setIds(Arrays.asList(101L, 202L));

        ErpStockMapper mapper = org.mockito.Mockito.mock(
                ErpStockMapper.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        QueryWrapperX<ErpStockDO> wrapper = mapper.buildStockQueryWrapper(
                reqVO, null, null, null, null, null, null, null, null, null, null);

        assertTrue(wrapper.getCustomSqlSegment().contains("id IN"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(101L));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(202L));
    }

    @Test
    void appendStockKeyCondition_rejectsEmptyStrictBatchResult() {
        QueryWrapperX<ErpStockDO> wrapper = new QueryWrapperX<>();

        assertFalse(ErpStockMapper.appendStockKeyCondition(wrapper, Collections.emptyMap()));
        assertTrue(wrapper.getCustomSqlSegment().isEmpty());
    }

    @Test
    void buildBatchNoKeywordStockKeyWrapper_fuzzyMatchesWithinVisibleWarehouses() {
        QueryWrapper<ErpStockRecordDO> wrapper =
                ErpStockRecordMapper.buildBatchNoKeywordStockKeyWrapper(
                        " test 260721 ", Collections.singletonList(26L));

        assertTrue(wrapper.getCustomSqlSegment().contains("TRIM(batch_no) LIKE"));
        assertTrue(wrapper.getCustomSqlSegment().contains("warehouse_id"));
        assertTrue(wrapper.getCustomSqlSegment().contains("GROUP BY product_id,warehouse_id"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("%test%260721%"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(26L));
    }

    @Test
    void associatedBatchKeywordQuery_coversDisplayedBatchSources() throws NoSuchMethodException {
        Select select = ErpStockBatchQuantityMapper.class
                .getMethod("selectAssociatedBatchKeywordStockKeyList",
                        String.class, java.util.Collection.class, Integer.class,
                        java.util.Collection.class, Integer.class, Integer.class,
                        Integer.class, java.util.Collection.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", select.value());

        assertFalse(sql.contains("erp_stock_record"));
        assertTrue(sql.contains("erp_sale_cart_items"));
        assertTrue(sql.contains("erp_purchase_in_items"));
        assertTrue(sql.contains("erp_purchase_order_items"));
        assertTrue(sql.contains("TRIM(d.batch_no) LIKE"));
        assertTrue(sql.contains("HAVING SUM(d.count)"));
    }

}
