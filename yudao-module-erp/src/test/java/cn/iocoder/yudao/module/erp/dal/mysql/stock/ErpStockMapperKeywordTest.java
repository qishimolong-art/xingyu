package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockMapperKeywordTest {

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
