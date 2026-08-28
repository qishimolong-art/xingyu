package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.biz.system.dict.DictDataCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.dict.dto.DictDataRespDTO;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordReportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.enums.DictTypeConstants;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DictDataCommonApi dictDataApi;

    @BeforeEach
    public void setUp() {
        DictFrameworkUtils.init(dictDataApi);
        DictFrameworkUtils.clearCache();
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());
    }

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

    @Test
    public void testExportStockRecordReportExcel_writeExcel() throws Exception {
        ErpStockRecordDO inRecord = new ErpStockRecordDO()
                .setId(1L).setProductId(11L).setWarehouseId(21L)
                .setBizType(10).setBizNo("IN001").setBizDate(LocalDateTime.of(2026, 7, 14, 10, 0))
                .setCount(new BigDecimal("4"))
                .setUnitPrice(new BigDecimal("2.50"))
                .setTotalPrice(new BigDecimal("10.00"))
                .setTotalCount(new BigDecimal("14"))
                .setCostPrice(new BigDecimal("2.00"))
                .setCostAmount(new BigDecimal("28.00"));
        inRecord.setCreator("1001");
        ErpStockRecordDO outRecord = new ErpStockRecordDO()
                .setId(2L).setProductId(12L).setWarehouseId(22L)
                .setBizType(20).setBizNo("OUT001").setBizDate(LocalDateTime.of(2026, 7, 14, 11, 0))
                .setCount(new BigDecimal("-3"))
                .setUnitPrice(new BigDecimal("5.00"))
                .setTotalPrice(new BigDecimal("-15.00"))
                .setTotalCount(new BigDecimal("11"))
                .setCostPrice(new BigDecimal("2.00"))
                .setCostAmount(new BigDecimal("22.00"));
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
        when(dictDataApi.getDictDataList(eq(DictTypeConstants.STOCK_RECORD_BIZ_TYPE))).thenReturn(Arrays.asList(
                buildDictData("10", "采购入库"),
                buildDictData("20", "销售出库")
        ));

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportStockRecordReportExcel(new ErpStockRecordPageReqVO(), response);

        byte[] content = response.getContentAsByteArray();
        assertTrue(content.length > 0);
        assertEquals('P', content[0]);
        assertEquals('K', content[1]);
        assertTrue(response.getHeader("Content-Disposition").contains(".xlsx"));
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("采购入库", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("IN001", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals(4D, sheet.getRow(1).getCell(7).getNumericCellValue());
            assertEquals("销售出库", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("OUT001", sheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals(3D, sheet.getRow(2).getCell(10).getNumericCellValue());
        }
    }

    @Test
    public void testExportStockRecordReportExcel_writeEmptyExcel() throws Exception {
        when(stockRecordService.getStockRecordPage(any())).thenReturn(new PageResult<>(Arrays.asList(), 0L));

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportStockRecordReportExcel(new ErpStockRecordPageReqVO(), response);

        byte[] content = response.getContentAsByteArray();
        assertTrue(content.length > 0);
        assertEquals('P', content[0]);
        assertEquals('K', content[1]);
        assertTrue(response.getHeader("Content-Disposition").contains(".xlsx"));
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals(0, sheet.getLastRowNum());
        }
    }

    @Test
    public void testStockRecordReport_masksPurchasePriceByProductPermission() throws Exception {
        ErpStockRecordDO inRecord = new ErpStockRecordDO()
                .setId(1L).setProductId(11L).setWarehouseId(21L)
                .setBizType(70).setBizNo("PIN001").setBizDate(LocalDateTime.of(2026, 7, 14, 10, 0))
                .setCount(new BigDecimal("4"))
                .setUnitPrice(new BigDecimal("2.50"))
                .setTotalPrice(new BigDecimal("10.00"))
                .setTotalCount(new BigDecimal("14"))
                .setCostPrice(new BigDecimal("2.00"))
                .setCostAmount(new BigDecimal("28.00"));
        inRecord.setCreator("1001");
        when(stockRecordService.getStockRecordPage(any())).thenReturn(
                new PageResult<>(Collections.singletonList(inRecord), 1L));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(11L,
                new ErpProductRespVO().setId(11L).setCode("P-11").setName("Product In")));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(21L,
                new ErpWarehouseDO().setId(21L).setName("Warehouse A")));
        when(permissionApi.getCurrentUserHiddenFields("erp_product"))
                .thenReturn(Arrays.asList("lastPurchasePrice", "col_lastPurchasePrice"));
        when(dictDataApi.getDictDataList(eq(DictTypeConstants.STOCK_RECORD_BIZ_TYPE))).thenReturn(Collections.singletonList(
                buildDictData("70", "采购入库")
        ));

        CommonResult<PageResult<ErpStockRecordReportRespVO>> response =
                controller.getStockRecordReportPage(new ErpStockRecordPageReqVO());

        ErpStockRecordReportRespVO row = response.getData().getList().get(0);
        assertNull(row.getInUnitPrice());
        assertNull(row.getInAmount());
        assertNull(row.getCostPrice());
        assertNull(row.getCostAmount());

        MockHttpServletResponse exportResponse = new MockHttpServletResponse();
        controller.exportStockRecordReportExcel(new ErpStockRecordPageReqVO(), exportResponse);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(exportResponse.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("****", sheet.getRow(1).getCell(8).getStringCellValue());
            assertEquals("****", sheet.getRow(1).getCell(9).getStringCellValue());
            assertEquals("****", sheet.getRow(1).getCell(14).getStringCellValue());
            assertEquals("****", sheet.getRow(1).getCell(15).getStringCellValue());
        }
    }

    private static DictDataRespDTO buildDictData(String value, String label) {
        DictDataRespDTO dictData = new DictDataRespDTO();
        dictData.setDictType(DictTypeConstants.STOCK_RECORD_BIZ_TYPE);
        dictData.setValue(value);
        dictData.setLabel(label);
        return dictData;
    }

}
