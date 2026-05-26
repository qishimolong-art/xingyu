package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpSupplierServiceImpl} 的单元测试类
 */
public class ErpSupplierServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSupplierServiceImpl supplierService;

    @Mock
    private ErpSupplierMapper supplierMapper;

    // ========== createSupplier ==========

    @Test
    public void testCreateSupplier_withoutMaxCode_generatesGYS000001() {
        when(supplierMapper.selectMaxCode()).thenReturn(null);
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setName("芋道源码");

        supplierService.createSupplier(reqVO);

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        ErpSupplierDO inserted = captor.getValue();
        assertEquals("芋道源码", inserted.getName());
        assertEquals("GYS000001", inserted.getCode());
        // 兜底默认值
        assertEquals(Integer.valueOf(0), inserted.getSort());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), inserted.getStatus());
    }

    @Test
    public void testCreateSupplier_withMaxCode_generatesNextSeq() {
        when(supplierMapper.selectMaxCode()).thenReturn("GYS000023");
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setName("芋道源码");
        reqVO.setSort(5);
        reqVO.setStatus(CommonStatusEnum.DISABLE.getStatus());

        supplierService.createSupplier(reqVO);

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        ErpSupplierDO inserted = captor.getValue();
        assertEquals("GYS000024", inserted.getCode());
        // 已提供 sort/status 时不覆盖
        assertEquals(Integer.valueOf(5), inserted.getSort());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), inserted.getStatus());
    }

    @Test
    public void testCreateSupplier_withInvalidMaxCode_fallbackToOne() {
        // 编码后缀非数字，应当兜底为 GYS000001
        when(supplierMapper.selectMaxCode()).thenReturn("GYSABCDEF");
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setName("芋道源码");

        supplierService.createSupplier(reqVO);

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        assertEquals("GYS000001", captor.getValue().getCode());
    }

    // ========== updateSupplier ==========

    @Test
    public void testUpdateSupplier_success() {
        when(supplierMapper.selectById(eq(10L)))
                .thenReturn(new ErpSupplierDO().setId(10L));
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setId(10L);
        reqVO.setName("芋道源码-更新");

        supplierService.updateSupplier(reqVO);

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).updateById(captor.capture());
        assertEquals(Long.valueOf(10L), captor.getValue().getId());
        assertEquals("芋道源码-更新", captor.getValue().getName());
    }

    @Test
    public void testUpdateSupplier_notExists_throwException() {
        when(supplierMapper.selectById(eq(10L))).thenReturn(null);
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setId(10L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.updateSupplier(reqVO));
        assertEquals(SUPPLIER_NOT_EXISTS.getCode(), ex.getCode());
        verify(supplierMapper, never()).updateById(any(ErpSupplierDO.class));
    }

    // ========== deleteSupplier ==========

    @Test
    public void testDeleteSupplier_success() {
        when(supplierMapper.selectById(eq(10L)))
                .thenReturn(new ErpSupplierDO().setId(10L));

        supplierService.deleteSupplier(10L);

        verify(supplierMapper).deleteById(eq(10L));
    }

    @Test
    public void testDeleteSupplier_notExists_throwException() {
        when(supplierMapper.selectById(eq(10L))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.deleteSupplier(10L));
        assertEquals(SUPPLIER_NOT_EXISTS.getCode(), ex.getCode());
        verify(supplierMapper, never()).deleteById(any(Long.class));
    }

    // ========== validateSupplier ==========

    @Test
    public void testValidateSupplier_success() {
        ErpSupplierDO supplier = new ErpSupplierDO().setId(10L).setName("芋道源码")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(supplierMapper.selectById(eq(10L))).thenReturn(supplier);

        ErpSupplierDO result = supplierService.validateSupplier(10L);

        assertSame(supplier, result);
    }

    @Test
    public void testValidateSupplier_notExists_throwException() {
        when(supplierMapper.selectById(eq(10L))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.validateSupplier(10L));
        assertEquals(SUPPLIER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testValidateSupplier_disabled_throwException() {
        when(supplierMapper.selectById(eq(10L))).thenReturn(new ErpSupplierDO()
                .setId(10L).setName("芋道源码")
                .setStatus(CommonStatusEnum.DISABLE.getStatus()));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.validateSupplier(10L));
        assertEquals(SUPPLIER_NOT_ENABLE.getCode(), ex.getCode());
    }

    // ========== getSupplier / getSupplierList / getSupplierPage ==========

    @Test
    public void testGetSupplier() {
        ErpSupplierDO supplier = new ErpSupplierDO().setId(10L);
        when(supplierMapper.selectById(eq(10L))).thenReturn(supplier);

        assertSame(supplier, supplierService.getSupplier(10L));
    }

    @Test
    public void testGetSupplier_notExists_returnsNull() {
        when(supplierMapper.selectById(eq(10L))).thenReturn(null);

        assertNull(supplierService.getSupplier(10L));
    }

    @Test
    public void testGetSupplierList() {
        List<Long> ids = Arrays.asList(1L, 2L);
        List<ErpSupplierDO> suppliers = Arrays.asList(
                new ErpSupplierDO().setId(1L), new ErpSupplierDO().setId(2L));
        when(supplierMapper.selectByIds(eq(ids))).thenReturn(suppliers);

        assertSame(suppliers, supplierService.getSupplierList(ids));
    }

    @Test
    public void testGetSupplierPage() {
        ErpSupplierPageReqVO reqVO = new ErpSupplierPageReqVO();
        PageResult<ErpSupplierDO> page = new PageResult<>(
                Collections.singletonList(new ErpSupplierDO().setId(1L)), 1L);
        when(supplierMapper.selectPage(eq(reqVO))).thenReturn(page);

        assertSame(page, supplierService.getSupplierPage(reqVO));
    }

    // ========== updateSupplierStatus ==========

    @Test
    public void testUpdateSupplierStatus_success() {
        when(supplierMapper.selectById(eq(10L)))
                .thenReturn(new ErpSupplierDO().setId(10L)
                        .setStatus(CommonStatusEnum.ENABLE.getStatus()));

        supplierService.updateSupplierStatus(10L, CommonStatusEnum.DISABLE.getStatus());

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).updateById(captor.capture());
        ErpSupplierDO updated = captor.getValue();
        assertEquals(Long.valueOf(10L), updated.getId());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), updated.getStatus());
        // 仅更新 id + status，其他字段保持 null
        assertNull(updated.getName());
        assertNull(updated.getCode());
    }

    @Test
    public void testUpdateSupplierStatus_notExists_throwException() {
        when(supplierMapper.selectById(eq(10L))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.updateSupplierStatus(10L,
                        CommonStatusEnum.DISABLE.getStatus()));
        assertEquals(SUPPLIER_NOT_EXISTS.getCode(), ex.getCode());
        verify(supplierMapper, never()).updateById(any(ErpSupplierDO.class));
    }

    // ========== getSupplierListByStatus ==========

    @Test
    public void testGetSupplierListByStatus() {
        Integer status = CommonStatusEnum.ENABLE.getStatus();
        List<ErpSupplierDO> suppliers = Arrays.asList(
                new ErpSupplierDO().setId(1L).setStatus(status),
                new ErpSupplierDO().setId(2L).setStatus(status));
        when(supplierMapper.selectListByStatus(eq(status))).thenReturn(suppliers);

        List<ErpSupplierDO> result = supplierService.getSupplierListByStatus(status);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertSame(suppliers, result);
    }

}
