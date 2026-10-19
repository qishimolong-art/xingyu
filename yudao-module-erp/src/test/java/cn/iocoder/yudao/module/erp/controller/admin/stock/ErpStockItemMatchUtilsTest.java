package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockItemMatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockItemMatchRespVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErpStockItemMatchUtilsTest {

    @Test
    void excludesDeletedRowsSortsByIdAndCalculatesStablePage() {
        ErpStockItemMatchReqVO.Key key = new ErpStockItemMatchReqVO.Key();
        key.setProductId(30L);
        ErpStockItemMatchReqVO reqVO = new ErpStockItemMatchReqVO();
        reqVO.setPageSize(2);
        reqVO.setKeys(Collections.singletonList(key));
        reqVO.setExcludedItemIds(Arrays.asList(2L, 2L, null));

        ErpStockItemMatchRespVO result = ErpStockItemMatchUtils.match(
                Arrays.asList(new Row(3L, 30L), new Row(1L, 10L), new Row(2L, 20L)),
                reqVO, Row::getId, (row, candidate) -> row.getProductId().equals(candidate.getProductId()));

        assertEquals(1, result.getMatches().size());
        assertEquals(3L, result.getMatches().get(0).getItemId());
        assertEquals(1, result.getMatches().get(0).getPageNo());
    }

    @Test
    void normalizesNullableBusinessText() {
        assertEquals("", ErpStockItemMatchUtils.normalizeText(null));
        assertEquals("A01", ErpStockItemMatchUtils.normalizeText("  A01 "));
    }

    @Data
    @AllArgsConstructor
    private static class Row {
        private Long id;
        private Long productId;
    }

}
