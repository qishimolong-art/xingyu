package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact.ErpCustomerContactSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerContactDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerContactMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CONTACT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpCustomerContactServiceImpl} 的单元测试
 */
public class ErpCustomerContactServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerContactServiceImpl contactService;

    @Mock
    private ErpCustomerContactMapper contactMapper;
    @Mock
    private ErpCustomerService customerService;

    // ==================== createContact ====================

    @Test
    public void testCreateContact_normalCase_returnId() {
        // 准备：构造参数
        ErpCustomerContactSaveReqVO reqVO = new ErpCustomerContactSaveReqVO();
        reqVO.setCustomerId(10L);
        reqVO.setName("张三");
        reqVO.setMobile("13800138000");
        reqVO.setPrimaryContact(true);

        // mock：insert 时回填 id
        when(contactMapper.insert(ArgumentMatchers.<ErpCustomerContactDO>any())).thenAnswer(invocation -> {
            ErpCustomerContactDO contact = invocation.getArgument(0);
            contact.setId(100L);
            return 1;
        });

        // 执行
        Long resultId = contactService.createContact(reqVO);

        // 断言：返回 id
        assertNotNull(resultId);
        assertEquals(100L, resultId);
        // 校验：先 validateCustomer 再 insert
        verify(customerService).validateCustomer(eq(10L));
        verify(contactMapper).insert(ArgumentMatchers.<ErpCustomerContactDO>argThat(contact ->
                Long.valueOf(10L).equals(contact.getCustomerId())
                        && "张三".equals(contact.getName())
                        && "13800138000".equals(contact.getMobile())
                        && Boolean.TRUE.equals(contact.getPrimaryContact())));
    }

    @Test
    public void testCreateContact_invalidCustomer_throwException() {
        // 准备
        ErpCustomerContactSaveReqVO reqVO = new ErpCustomerContactSaveReqVO();
        reqVO.setCustomerId(11L);
        reqVO.setName("李四");
        // mock：客户校验失败
        doThrow(new ServiceException(CUSTOMER_NOT_EXISTS))
                .when(customerService).validateCustomer(eq(11L));

        // 执行 & 断言
        assertServiceException(() -> contactService.createContact(reqVO), CUSTOMER_NOT_EXISTS);
        // 校验：未 insert
        verify(contactMapper, never()).insert(ArgumentMatchers.<ErpCustomerContactDO>any());
    }

    // ==================== updateContact ====================

    @Test
    public void testUpdateContact_normalCase_success() {
        Long contactId = 20L;
        // 准备：existing
        ErpCustomerContactDO existContact = new ErpCustomerContactDO()
                .setId(contactId)
                .setCustomerId(20L)
                .setName("王五");
        when(contactMapper.selectById(eq(contactId))).thenReturn(existContact);
        // 准备：更新参数
        ErpCustomerContactSaveReqVO reqVO = new ErpCustomerContactSaveReqVO();
        reqVO.setId(contactId);
        reqVO.setCustomerId(20L);
        reqVO.setName("王五-更新");
        reqVO.setMobile("13900139000");

        // 执行
        contactService.updateContact(reqVO);

        // 断言
        verify(customerService).validateCustomer(eq(20L));
        verify(contactMapper).updateById(ArgumentMatchers.<ErpCustomerContactDO>argThat(update ->
                contactId.equals(update.getId())
                        && "王五-更新".equals(update.getName())
                        && "13900139000".equals(update.getMobile())));
    }

    @Test
    public void testUpdateContact_notExists_throwException() {
        Long contactId = 21L;
        // 准备：not exists
        when(contactMapper.selectById(eq(contactId))).thenReturn(null);
        ErpCustomerContactSaveReqVO reqVO = new ErpCustomerContactSaveReqVO();
        reqVO.setId(contactId);
        reqVO.setCustomerId(21L);
        reqVO.setName("not exists");

        // 执行 & 断言
        assertServiceException(() -> contactService.updateContact(reqVO), CUSTOMER_CONTACT_NOT_EXISTS);
        // 校验：未发生 customer 校验和 update
        verify(customerService, never()).validateCustomer(any());
        verify(contactMapper, never()).updateById(ArgumentMatchers.<ErpCustomerContactDO>any());
    }

    // ==================== deleteContact ====================

    @Test
    public void testDeleteContact_normalCase_success() {
        Long contactId = 30L;
        ErpCustomerContactDO existContact = new ErpCustomerContactDO()
                .setId(contactId)
                .setCustomerId(30L)
                .setName("赵六");
        when(contactMapper.selectById(eq(contactId))).thenReturn(existContact);

        // 执行
        contactService.deleteContact(contactId);

        // 断言
        verify(contactMapper).deleteById(eq(contactId));
    }

    // ==================== getContactListByCustomerId ====================

    @Test
    public void testGetContactListByCustomerId_returnList() {
        Long customerId = 40L;
        ErpCustomerContactDO contact1 = new ErpCustomerContactDO().setId(401L).setCustomerId(customerId).setName("联系人1");
        ErpCustomerContactDO contact2 = new ErpCustomerContactDO().setId(402L).setCustomerId(customerId).setName("联系人2");
        when(contactMapper.selectListByCustomerId(eq(customerId)))
                .thenReturn(Arrays.asList(contact1, contact2));

        // 执行
        List<ErpCustomerContactDO> result = contactService.getContactListByCustomerId(customerId);

        // 断言
        assertEquals(2, result.size());
        assertEquals(401L, result.get(0).getId());
        assertEquals(402L, result.get(1).getId());
        // 校验：先 validateCustomer 再查询
        verify(customerService).validateCustomer(eq(customerId));
        verify(contactMapper).selectListByCustomerId(eq(customerId));
    }

}
