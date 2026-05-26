package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract.ErpCustomerContractPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract.ErpCustomerContractRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract.ErpCustomerContractSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerContractDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerContractService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpCustomerContractControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerContractController controller;

    @Mock
    private ErpCustomerContractService contractService;

    // ==================== createContract ====================

    @Test
    public void testCreateContract_paramPassThrough() {
        ErpCustomerContractSaveReqVO reqVO = new ErpCustomerContractSaveReqVO();
        when(contractService.createContract(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createContract(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(contractService).createContract(eq(reqVO));
    }

    @Test
    public void testCreateContract_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerContractController.class.getMethod("createContract", ErpCustomerContractSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    // ==================== updateContract ====================

    @Test
    public void testUpdateContract_paramPassThrough() {
        ErpCustomerContractSaveReqVO reqVO = new ErpCustomerContractSaveReqVO();

        CommonResult<Boolean> result = controller.updateContract(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(contractService).updateContract(eq(reqVO));
    }

    @Test
    public void testUpdateContract_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerContractController.class.getMethod("updateContract", ErpCustomerContractSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    // ==================== deleteContract ====================

    @Test
    public void testDeleteContract_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteContract(1L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(contractService).deleteContract(eq(1L));
    }

    @Test
    public void testDeleteContract_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerContractController.class.getMethod("deleteContract", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    // ==================== getContract ====================

    @Test
    public void testGetContract_paramPassThrough() {
        ErpCustomerContractDO contract = new ErpCustomerContractDO();
        contract.setId(1L);
        contract.setContractNo("HT001");
        when(contractService.getContract(eq(1L))).thenReturn(contract);

        CommonResult<ErpCustomerContractRespVO> result = controller.getContract(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("HT001", result.getData().getContractNo());
    }

    @Test
    public void testGetContract_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerContractController.class.getMethod("getContract", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    // ==================== getContractPage ====================

    @Test
    public void testGetContractPage_paramPassThrough() {
        ErpCustomerContractPageReqVO pageReqVO = new ErpCustomerContractPageReqVO();
        ErpCustomerContractDO contract = new ErpCustomerContractDO();
        contract.setId(1L);
        PageResult<ErpCustomerContractDO> pageResult = new PageResult<>(Arrays.asList(contract), 1L);
        when(contractService.getContractPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpCustomerContractRespVO>> result = controller.getContractPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
    }

    @Test
    public void testGetContractPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerContractController.class.getMethod("getContractPage", ErpCustomerContractPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    // ==================== getContractListByCustomer ====================

    @Test
    public void testGetContractListByCustomer_paramPassThrough() {
        ErpCustomerContractDO contract = new ErpCustomerContractDO();
        contract.setId(1L);
        contract.setCustomerId(100L);
        when(contractService.getContractListByCustomerId(eq(100L))).thenReturn(Arrays.asList(contract));

        CommonResult<List<ErpCustomerContractRespVO>> result = controller.getContractListByCustomer(100L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(100L, result.getData().get(0).getCustomerId());
    }

    @Test
    public void testGetContractListByCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerContractController.class.getMethod("getContractListByCustomer", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

}
