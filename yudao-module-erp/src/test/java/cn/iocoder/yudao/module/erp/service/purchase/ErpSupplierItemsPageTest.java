package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ErpSupplierItemsPageTest {
    private Object service;
    private Object mapper;
    private Object child;
    private ErpSupplierService suppliers;
    private String kind;

    private void setup(String kind) throws Exception {
        this.kind = kind;
        String root = "cn.iocoder.yudao.module.erp.";
        service = Class.forName(root + "service.purchase.ErpSupplier" + kind + "ServiceImpl").newInstance();
        mapper = mock(Class.forName(root + "dal.mysql.purchase.ErpSupplier" + kind + "Mapper"));
        child = Class.forName(root + "dal.dataobject.purchase.ErpSupplier" + kind + "DO").newInstance();
        ReflectionTestUtils.setField(child, "id", 10L);
        ReflectionTestUtils.setField(child, "supplierId", 1L);
        ReflectionTestUtils.setField(service, "supplier" + kind + "Mapper", mapper);
        suppliers = mock(ErpSupplierService.class);
        ReflectionTestUtils.setField(service, "supplierService", suppliers);
    }

    private Object call(Object target, String method, Class<?> type, Object value) throws Exception {
        return target.getClass().getMethod(method, type).invoke(target, value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Contact", "Contract", "Extend", "Image", "Account", "Bill", "Task", "BusinessInfo"})
    void pageIsBoundedAndUsesDatabasePagination(String kind) throws Exception {
        setup(kind);
        when(suppliers.getSupplier(1L)).thenReturn(new ErpSupplierDO().setId(1L));
        ErpSupplierItemPageReqVO req = new ErpSupplierItemPageReqVO();
        req.setSupplierId(1L);
        req.setPageNo(2);
        req.setPageSize(-1);
        Object stub = doReturn(new PageResult<>(Collections.singletonList(child), 101L)).when(mapper);
        call(stub, "selectPage", ErpSupplierItemPageReqVO.class, req);
        PageResult<?> page = (PageResult<?>) call(service, "getSupplier" + kind + "Page", ErpSupplierItemPageReqVO.class, req);
        assertEquals(100, req.getPageSize());
        assertEquals(2, req.getPageNo());
        assertEquals(101L, page.getTotal());
        assertSame(child, page.getList().get(0));
        assertTrue(mockingDetails(mapper).getInvocations().stream().noneMatch(i -> i.getMethod().getName().equals("selectListBySupplierId")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Contact", "Contract", "Extend", "Image", "Account", "Bill", "Task", "BusinessInfo"})
    void hiddenParentCannotBeQueriedOrDeletedByChildId(String kind) throws Exception {
        setup(kind);
        Object stub = doReturn(child).when(mapper);
        call(stub, "selectById", java.io.Serializable.class, 10L);
        for (String method : new String[]{"getSupplier" + kind, "deleteSupplier" + kind}) {
            InvocationTargetException error = assertThrows(InvocationTargetException.class, () -> call(service, method, Long.class, 10L));
            assertInstanceOf(ServiceException.class, error.getCause());
        }
        assertTrue(mockingDetails(mapper).getInvocations().stream().noneMatch(i -> i.getMethod().getName().startsWith("delete")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Contact", "Contract", "Extend", "Image", "Account", "Bill", "Task", "BusinessInfo"})
    void updateCannotMoveAChildToAnotherVisibleParent(String kind) throws Exception {
        setup(kind);
        when(suppliers.getSupplier(any())).thenAnswer(invocation -> new ErpSupplierDO().setId(invocation.getArgument(0)));
        Object stub = doReturn(child).when(mapper);
        call(stub, "selectById", java.io.Serializable.class, 10L);
        Class<?> reqType = Class.forName("cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier" + kind.toLowerCase() + ".ErpSupplier" + kind + "SaveReqVO");
        Object req = reqType.newInstance();
        ReflectionTestUtils.setField(req, "id", 10L);
        ReflectionTestUtils.setField(req, "supplierId", 2L);
        if (kind.equals("Task")) ReflectionTestUtils.setField(req, "month", 1);
        InvocationTargetException error = assertThrows(InvocationTargetException.class, () -> call(service, "updateSupplier" + kind, reqType, req));
        assertInstanceOf(ServiceException.class, error.getCause());
        assertTrue(mockingDetails(mapper).getInvocations().stream().noneMatch(i -> i.getMethod().getName().startsWith("update")));
    }
}
