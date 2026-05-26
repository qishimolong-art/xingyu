package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenVoucherConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen.ErpBookOpenVoucherConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpBookOpenVoucherConfigDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpBookOpenControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpBookOpenController controller;

    @Mock
    private ErpBookOpenService bookOpenService;

    // ==================== createBookOpen ====================

    @Test
    public void testCreateBookOpen_paramPassThrough() {
        ErpBookOpenSaveReqVO reqVO = new ErpBookOpenSaveReqVO();
        reqVO.setFiscalYear(2026);
        reqVO.setPeriod(5);
        reqVO.setStartDate(LocalDate.of(2026, 5, 1));
        when(bookOpenService.createBookOpen(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createBookOpen(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(bookOpenService).createBookOpen(eq(reqVO));
    }

    // ==================== updateBookOpen ====================

    @Test
    public void testUpdateBookOpen_paramPassThrough() {
        ErpBookOpenSaveReqVO reqVO = new ErpBookOpenSaveReqVO();
        reqVO.setId(1L);
        reqVO.setFiscalYear(2026);
        reqVO.setPeriod(5);
        reqVO.setStartDate(LocalDate.of(2026, 5, 1));

        CommonResult<Boolean> result = controller.updateBookOpen(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(bookOpenService).updateBookOpen(eq(reqVO));
    }

    // ==================== deleteBookOpen ====================

    @Test
    public void testDeleteBookOpen_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteBookOpen(1L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(bookOpenService).deleteBookOpen(eq(1L));
    }

    // ==================== getBookOpen ====================

    @Test
    public void testGetBookOpen_returnsNullWhenNotFound() {
        when(bookOpenService.getBookOpen(eq(1L))).thenReturn(null);

        CommonResult<ErpBookOpenRespVO> result = controller.getBookOpen(1L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    @Test
    public void testGetBookOpen_fillsVoucherConfigs() {
        ErpBookOpenDO bookOpen = new ErpBookOpenDO();
        bookOpen.setId(1L);
        bookOpen.setNo("KZ20260514000001");
        bookOpen.setFiscalYear(2026);
        bookOpen.setPeriod(5);
        when(bookOpenService.getBookOpen(eq(1L))).thenReturn(bookOpen);
        ErpBookOpenVoucherConfigDO cfg1 = ErpBookOpenVoucherConfigDO.builder()
                .id(10L).bookOpenId(1L).voucherType(1).enabled(true).sort(1).build();
        ErpBookOpenVoucherConfigDO cfg2 = ErpBookOpenVoucherConfigDO.builder()
                .id(11L).bookOpenId(1L).voucherType(7).enabled(false).sort(2).build();
        when(bookOpenService.getBookOpenVoucherConfigList(eq(1L))).thenReturn(Arrays.asList(cfg1, cfg2));

        CommonResult<ErpBookOpenRespVO> result = controller.getBookOpen(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("KZ20260514000001", result.getData().getNo());
        assertNotNull(result.getData().getVoucherConfigs());
        assertEquals(2, result.getData().getVoucherConfigs().size());
        verify(bookOpenService).getBookOpenVoucherConfigList(eq(1L));
    }

    // ==================== getBookOpenPage ====================

    @Test
    public void testGetBookOpenPage_emptyList() {
        ErpBookOpenPageReqVO pageReqVO = new ErpBookOpenPageReqVO();
        PageResult<ErpBookOpenDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(bookOpenService.getBookOpenPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpBookOpenRespVO>> result = controller.getBookOpenPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(bookOpenService).getBookOpenPage(eq(pageReqVO));
    }

    @Test
    public void testGetBookOpenPage_nonEmptyList() {
        ErpBookOpenPageReqVO pageReqVO = new ErpBookOpenPageReqVO();
        ErpBookOpenDO bookOpen = new ErpBookOpenDO();
        bookOpen.setId(1L);
        bookOpen.setNo("KZ20260514000001");
        PageResult<ErpBookOpenDO> pageResult = new PageResult<>(Arrays.asList(bookOpen), 1L);
        when(bookOpenService.getBookOpenPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpBookOpenRespVO>> result = controller.getBookOpenPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals(1L, result.getData().getList().get(0).getId());
    }

    // ==================== getBookOpenVoucherConfigList ====================

    @Test
    public void testGetBookOpenVoucherConfigList_paramPassThrough() {
        ErpBookOpenVoucherConfigDO cfg = ErpBookOpenVoucherConfigDO.builder()
                .id(10L).bookOpenId(1L).voucherType(1).enabled(true).sort(1).build();
        when(bookOpenService.getBookOpenVoucherConfigList(eq(1L)))
                .thenReturn(Collections.singletonList(cfg));

        CommonResult<java.util.List<ErpBookOpenVoucherConfigRespVO>> result =
                controller.getBookOpenVoucherConfigList(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        verify(bookOpenService).getBookOpenVoucherConfigList(eq(1L));
    }

    // ==================== updateBookOpenVoucherConfigs ====================

    @Test
    public void testUpdateBookOpenVoucherConfigs_paramPassThrough() {
        ErpBookOpenVoucherConfigSaveReqVO reqVO = new ErpBookOpenVoucherConfigSaveReqVO();
        reqVO.setBookOpenId(1L);
        ErpBookOpenVoucherConfigSaveReqVO.Item item = new ErpBookOpenVoucherConfigSaveReqVO.Item();
        item.setVoucherType(1);
        item.setEnabled(true);
        reqVO.setItems(Collections.singletonList(item));

        CommonResult<Boolean> result = controller.updateBookOpenVoucherConfigs(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(bookOpenService).updateBookOpenVoucherConfigs(eq(reqVO));
    }

}
