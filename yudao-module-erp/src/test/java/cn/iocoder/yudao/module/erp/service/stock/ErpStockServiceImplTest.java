package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockLockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpStockServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockServiceImpl stockService;

    @Mock
    private ErpProductService productService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private ErpStockLockMapper stockLockMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpStockRecordMapper stockRecordMapper;
    @Mock
    private ErpStockCheckService stockCheckService;

    @Test
    public void testGetStockPage_productName_usesProductIdFilter() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setProductName("brake pad");
        List<Long> productIds = Arrays.asList(10L, 20L);
        PageResult<ErpStockDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(productMapper.selectIdsByComplexQuery(eq(reqVO), isNull(), isNull())).thenReturn(productIds);
        when(stockMapper.selectPage(eq(reqVO), eq(productIds), isNull())).thenReturn(pageResult);

        PageResult<ErpStockDO> result = stockService.getStockPage(reqVO);

        assertSame(pageResult, result);
        verify(productMapper).selectIdsByComplexQuery(eq(reqVO), isNull(), isNull());
        verify(stockMapper).selectPage(eq(reqVO), eq(productIds), isNull());
    }

    @Test
    public void testGetStockPage_blankProductName_doesNotUseProductIdFilter() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setProductName("   ");
        PageResult<ErpStockDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(stockMapper.selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull()))
                .thenReturn(pageResult);

        PageResult<ErpStockDO> result = stockService.getStockPage(reqVO);

        assertSame(pageResult, result);
        verify(productMapper, never()).selectIdsByComplexQuery(eq(reqVO), isNull(), isNull());
        verify(stockMapper).selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull());
    }

}
