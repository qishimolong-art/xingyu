package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordReportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ErpStockRecordControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockRecordController controller;

    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private AdminUserApi adminUserApi;

    @Test
    public void testGetStockRecordReportPage_showFilledAmount() {
        ErpStockRecordDO inRecord = new ErpStockRecordDO()
                .setId(1L).setProductId(11L).setWarehouseId(21L)
                .setCount(new BigDecimal("4"))
                .setUnitPrice(new BigDecimal("2.50"))
                .setTotalPrice(new BigDecimal("10.00"))
                .setBizNo("IN001");
        inRecord.setCreator("1001");
        ErpStockRecordDO outRecord = new ErpStockRecordDO()
                .setId(2L).setProductId(12L).setWarehouseId(22L)
                .setCount(new BigDecimal("-3"))
                .setUnitPrice(new BigDecimal("5.00"))
                .setTotalPrice(new BigDecimal("-15.00"))
                .setBizNo("OUT001");
        outRecord.setCreator("1002");
        when(stockRecordService.getStockRecordPage(any())).thenReturn(
                new PageResult<>(Arrays.asList(inRecord, outRecord), 2L));

        Map<Long, ErpProductRespVO> productMap = new HashMap<>();
        productMap.put(11L, new ErpProductRespVO().setId(11L).setCode("P-11").setName("Product In"));
        productMap.put(12L, new ErpProductRespVO().setId(12L).setCode("P-12").setName("Product Out"));
        when(productService.getProductVOMap(any())).thenReturn(productMap);

        Map<Long, ErpWarehouseDO> warehouseMap = new HashMap<>();
        warehouseMap.put(21L, new ErpWarehouseDO().setId(21L).setName("Warehouse A"));
        warehouseMap.put(22L, new ErpWarehouseDO().setId(22L).setName("Warehouse B"));
        when(warehouseService.getWarehouseMap(any())).thenReturn(warehouseMap);

        CommonResult<PageResult<ErpStockRecordReportRespVO>> response =
                controller.getStockRecordReportPage(new ErpStockRecordPageReqVO());
        PageResult<ErpStockRecordReportRespVO> result = response.getData();

        assertNotNull(result);
        assertEquals(2L, result.getTotal());
        assertEquals("P-11", result.getList().get(0).getProductCode());
        assertEquals(new BigDecimal("4"), result.getList().get(0).getInCount());
        assertEquals(new BigDecimal("2.50"), result.getList().get(0).getInUnitPrice());
        assertEquals(new BigDecimal("10.00"), result.getList().get(0).getInAmount());
        assertEquals("P-12", result.getList().get(1).getProductCode());
        assertEquals(new BigDecimal("3"), result.getList().get(1).getOutCount());
        assertEquals(new BigDecimal("5.00"), result.getList().get(1).getOutUnitPrice());
        assertEquals(new BigDecimal("15.00"), result.getList().get(1).getOutAmount());
    }

}
