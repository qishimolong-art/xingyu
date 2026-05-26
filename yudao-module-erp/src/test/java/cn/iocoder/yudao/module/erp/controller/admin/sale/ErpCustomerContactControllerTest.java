package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact.ErpCustomerContactPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact.ErpCustomerContactRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact.ErpCustomerContactSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerContactDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerContactService;
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

public class ErpCustomerContactControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerContactController controller;

    @Mock
    private ErpCustomerContactService contactService;

    // ==================== createContact ====================

    @Test
    public void testCreateContact_paramPassThrough() {
        ErpCustomerContactSaveReqVO reqVO = new ErpCustomerContactSaveReqVO();
        when(contactService.createContact(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createContact(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(contactService).createContact(eq(reqVO));
    }

    @Test
    public void testCreateContact_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerContactController.class.getMethod("createContact", ErpCustomerContactSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    // ==================== updateContact ====================

    @Test
    public void testUpdateContact_paramPassThrough() {
        ErpCustomerContactSaveReqVO reqVO = new ErpCustomerContactSaveReqVO();

        CommonResult<Boolean> result = controller.updateContact(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(contactService).updateContact(eq(reqVO));
    }

    @Test
    public void testUpdateContact_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerContactController.class.getMethod("updateContact", ErpCustomerContactSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    // ==================== deleteContact ====================

    @Test
    public void testDeleteContact_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteContact(1L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(contactService).deleteContact(eq(1L));
    }

    @Test
    public void testDeleteContact_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerContactController.class.getMethod("deleteContact", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    // ==================== getContact ====================

    @Test
    public void testGetContact_paramPassThrough() {
        ErpCustomerContactDO contact = new ErpCustomerContactDO();
        contact.setId(1L);
        contact.setName("test");
        when(contactService.getContact(eq(1L))).thenReturn(contact);

        CommonResult<ErpCustomerContactRespVO> result = controller.getContact(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("test", result.getData().getName());
    }

    @Test
    public void testGetContact_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerContactController.class.getMethod("getContact", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    // ==================== getContactPage ====================

    @Test
    public void testGetContactPage_paramPassThrough() {
        ErpCustomerContactPageReqVO pageReqVO = new ErpCustomerContactPageReqVO();
        ErpCustomerContactDO contact = new ErpCustomerContactDO();
        contact.setId(1L);
        PageResult<ErpCustomerContactDO> pageResult = new PageResult<>(Arrays.asList(contact), 1L);
        when(contactService.getContactPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpCustomerContactRespVO>> result = controller.getContactPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
    }

    @Test
    public void testGetContactPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerContactController.class.getMethod("getContactPage", ErpCustomerContactPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    // ==================== getContactListByCustomer ====================

    @Test
    public void testGetContactListByCustomer_paramPassThrough() {
        ErpCustomerContactDO contact = new ErpCustomerContactDO();
        contact.setId(1L);
        contact.setCustomerId(100L);
        when(contactService.getContactListByCustomerId(eq(100L))).thenReturn(Arrays.asList(contact));

        CommonResult<List<ErpCustomerContactRespVO>> result = controller.getContactListByCustomer(100L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(100L, result.getData().get(0).getCustomerId());
    }

    @Test
    public void testGetContactListByCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerContactController.class.getMethod("getContactListByCustomer", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

}
