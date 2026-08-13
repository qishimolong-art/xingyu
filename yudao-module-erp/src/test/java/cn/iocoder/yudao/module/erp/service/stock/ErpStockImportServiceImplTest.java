package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockCheckTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpStockImportServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockImportServiceImpl stockImportService;

    @Mock
    private ErpStockInService stockInService;
    @Mock
    private ErpStockOutService stockOutService;
    @Mock
    private ErpStockMoveService stockMoveService;
    @Mock
    private ErpStockCheckService stockCheckService;
    @Mock
    private ErpWarehouseMoveService warehouseMoveService;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpWarehouseMapper warehouseMapper;
    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private DeptApi deptApi;

    @Test
    void importStockInList_invalidBizTime_returnsReadableMessage() {
        ErpStockImportExcelVO row = new ErpStockImportExcelVO();
        row.setOrderNo("IN-001");
        row.setBizTime("2026/07/01");
        row.setProductCode("P0001");
        row.setCount(BigDecimal.ONE);

        ErpStockImportResultRespVO result = stockImportService.importStockInList(Collections.singletonList(row));

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getFailureDetails().size());
        assertEquals("其它入库导入失败：业务时间格式不正确，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss",
                result.getFailureDetails().get(0).getReason());
    }

    @Test
    void importStockTransferOutList_invalidBizTime_returnsReadableMessage() {
        ErpStockImportExcelVO row = new ErpStockImportExcelVO();
        row.setOrderNo("STO-001");
        row.setBizTime("2026/07/01");
        row.setProductCode("P0001");
        row.setCount(BigDecimal.ONE);
        row.setProductPrice(BigDecimal.ONE);

        ErpStockImportResultRespVO result = stockImportService.importStockTransferOutList(
                Collections.singletonList(row));

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getFailureDetails().size());
        assertEquals("调拨出库单导入失败：业务时间格式不正确，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss",
                result.getFailureDetails().get(0).getReason());
    }

    @Test
    void importStockCheckList_autoFillsTimeStockAndPrice() {
        ErpStockCheckImportExcelVO first = new ErpStockCheckImportExcelVO();
        first.setCheckTypeName("盘数量");
        first.setWarehouseName("默认仓");
        first.setProductCode("P0001");
        first.setBatchNo("BATCH-001");
        first.setActualCount(new BigDecimal("12"));
        first.setRemark("单据备注");
        first.setItemRemark("明细备注");

        ErpStockCheckImportExcelVO second = new ErpStockCheckImportExcelVO();
        second.setWarehouseName("默认仓");
        second.setProductCode("P0002");
        second.setActualCount(new BigDecimal("18"));
        second.setProductPrice(new BigDecimal("8.80"));

        ErpProductDO firstProduct = new ErpProductDO()
                .setId(100L).setCode("P0001").setUnitId(1L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setLastPurchasePrice(new BigDecimal("6.66"));
        ErpProductDO secondProduct = new ErpProductDO()
                .setId(101L).setCode("P0002").setUnitId(1L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        ErpWarehouseDO warehouse = new ErpWarehouseDO()
                .setId(200L).setName("默认仓").setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(productMapper.selectByCode("P0001")).thenReturn(firstProduct);
        when(productMapper.selectByCode("P0002")).thenReturn(secondProduct);
        when(warehouseMapper.selectByName("默认仓")).thenReturn(warehouse);
        when(stockMapper.selectByProductIdAndWarehouseId(100L, 200L)).thenReturn(new ErpStockDO()
                .setProductId(100L).setWarehouseId(200L).setCount(new BigDecimal("10"))
                .setCostPrice(new BigDecimal("7.77")));
        when(stockMapper.selectByProductIdAndWarehouseId(101L, 200L)).thenReturn(new ErpStockDO()
                .setProductId(101L).setWarehouseId(200L).setCount(new BigDecimal("20"))
                .setCostPrice(new BigDecimal("9.99")));
        when(stockCheckService.createStockCheck(any())).thenReturn(1L);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        ErpStockImportResultRespVO result = stockImportService.importStockCheckList(Arrays.asList(first, second));
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertEquals(1, result.getSuccessCount(), result.getFailureDetails().toString());
        assertEquals(1, result.getCreateCount());
        assertEquals(0, result.getFailureCount());

        ArgumentCaptor<ErpStockCheckSaveReqVO> captor = ArgumentCaptor.forClass(ErpStockCheckSaveReqVO.class);
        verify(stockCheckService).createStockCheck(captor.capture());
        ErpStockCheckSaveReqVO request = captor.getValue();
        assertEquals(ErpStockCheckTypeEnum.COUNT.getType(), request.getCheckType());
        assertEquals("单据备注", request.getRemark());
        assertFalse(request.getCheckTime().isBefore(before));
        assertFalse(request.getCheckTime().isAfter(after));
        assertEquals(2, request.getItems().size());

        ErpStockCheckSaveReqVO.Item firstItem = request.getItems().get(0);
        assertEquals(200L, firstItem.getWarehouseId());
        assertEquals(100L, firstItem.getProductId());
        assertEquals("BATCH-001", firstItem.getBatchNo());
        assertEquals(new BigDecimal("10"), firstItem.getStockCount());
        assertEquals(new BigDecimal("12"), firstItem.getActualCount());
        assertEquals(new BigDecimal("2"), firstItem.getCount());
        assertEquals(new BigDecimal("7.77"), firstItem.getProductPrice());
        assertEquals("明细备注", firstItem.getRemark());

        ErpStockCheckSaveReqVO.Item secondItem = request.getItems().get(1);
        assertEquals(new BigDecimal("20"), secondItem.getStockCount());
        assertEquals(new BigDecimal("18"), secondItem.getActualCount());
        assertEquals(new BigDecimal("-2"), secondItem.getCount());
        assertEquals(new BigDecimal("8.80"), secondItem.getProductPrice());
    }

    @Test
    void importWarehouseMoveList_autoFillsTimeAndGroupsRepeatedWarehouses() {
        ErpWarehouseMoveImportExcelVO first = new ErpWarehouseMoveImportExcelVO();
        first.setFromWarehouseName("移出仓");
        first.setToWarehouseName("移入仓");
        first.setProductCode("P0001");
        first.setCount(new BigDecimal("3"));
        first.setFromShelf("A-01");
        first.setToShelf("B-01");
        first.setBatchNo("BATCH-001");
        first.setProductPrice(new BigDecimal("12.34"));
        first.setRemark("单据备注");
        first.setItemRemark("第一行备注");

        ErpWarehouseMoveImportExcelVO second = new ErpWarehouseMoveImportExcelVO();
        second.setFromWarehouseName("移出仓");
        second.setToWarehouseName("移入仓");
        second.setProductCode("P0002");
        second.setCount(new BigDecimal("5"));
        second.setFromShelf("A-02");
        second.setToShelf("B-02");

        when(warehouseMapper.selectByName("移出仓")).thenReturn(new ErpWarehouseDO()
                .setId(200L).setName("移出仓").setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(warehouseMapper.selectByName("移入仓")).thenReturn(new ErpWarehouseDO()
                .setId(201L).setName("移入仓").setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(productMapper.selectByCode("P0001")).thenReturn(new ErpProductDO()
                .setId(100L).setCode("P0001").setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(productMapper.selectByCode("P0002")).thenReturn(new ErpProductDO()
                .setId(101L).setCode("P0002").setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(warehouseMoveService.createWarehouseMove(any())).thenReturn(1L);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        ErpStockImportResultRespVO result = stockImportService.importWarehouseMoveList(Arrays.asList(first, second));
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertEquals(1, result.getSuccessCount(), result.getFailureDetails().toString());
        assertEquals(1, result.getCreateCount());
        assertEquals(0, result.getFailureCount());

        ArgumentCaptor<ErpWarehouseMoveSaveReqVO> captor = ArgumentCaptor.forClass(ErpWarehouseMoveSaveReqVO.class);
        verify(warehouseMoveService).createWarehouseMove(captor.capture());
        ErpWarehouseMoveSaveReqVO request = captor.getValue();
        assertEquals(200L, request.getFromWarehouseId());
        assertEquals(201L, request.getToWarehouseId());
        assertEquals("单据备注", request.getRemark());
        assertFalse(request.getMoveTime().isBefore(before));
        assertFalse(request.getMoveTime().isAfter(after));
        assertEquals(2, request.getItems().size());

        ErpWarehouseMoveSaveReqVO.Item firstItem = request.getItems().get(0);
        assertEquals(100L, firstItem.getProductId());
        assertEquals(new BigDecimal("3"), firstItem.getCount());
        assertEquals(new BigDecimal("12.34"), firstItem.getProductPrice());
        assertEquals("A-01", firstItem.getFromShelf());
        assertEquals("B-01", firstItem.getToShelf());
        assertEquals("BATCH-001", firstItem.getBatchNo());
        assertEquals("第一行备注", firstItem.getRemark());

        ErpWarehouseMoveSaveReqVO.Item secondItem = request.getItems().get(1);
        assertEquals(101L, secondItem.getProductId());
        assertEquals(new BigDecimal("5"), secondItem.getCount());
        assertEquals("A-02", secondItem.getFromShelf());
        assertEquals("B-02", secondItem.getToShelf());
    }

}
