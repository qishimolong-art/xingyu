package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierBusinessInfoDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierBusinessInfoMapper;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

class ErpSupplierControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSupplierController controller;

    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpSupplierBusinessInfoMapper supplierBusinessInfoMapper;

    @Test
    void getSupplier_keepsPersistedCreationDepartmentSeparateFromBusinessDepartment() {
        ErpSupplierDO supplier = new ErpSupplierDO()
                .setId(1L)
                .setDeptId(303L)
                .setCreateDeptId(101L);
        supplier.setCreator("7");
        when(supplierService.getSupplier(1L)).thenReturn(supplier);
        when(supplierService.getSupplierDeptMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(
                Collections.singletonMap(7L, buildUser(7L, "张三", 202L)));
        when(supplierBusinessInfoMapper.selectListBySupplierIds(anyCollection())).thenReturn(
                Collections.singletonList(new ErpSupplierBusinessInfoDO().setSupplierId(1L)));

        Map<Long, DeptRespDTO> departments = new HashMap<>();
        departments.put(101L, buildDept(101L, "创建部门"));
        departments.put(303L, buildDept(303L, "业务分配部门"));
        when(deptApi.getDeptMap(anyCollection())).thenReturn(departments);

        ErpSupplierRespVO result = controller.getSupplier(1L).getData();

        assertEquals(101L, result.getCreateDeptId());
        assertEquals("创建部门", result.getCreateDeptName());
        assertEquals("业务分配部门", result.getDeptName());
        assertEquals("张三", result.getCreatorName());
        assertEquals(true, result.getBusinessInfoSynced());
    }

    @Test
    void getSupplier_usesCreatorDepartmentForLegacySupplier() {
        ErpSupplierDO supplier = new ErpSupplierDO()
                .setId(1L)
                .setDeptId(303L);
        supplier.setCreator("7");
        when(supplierService.getSupplier(1L)).thenReturn(supplier);
        when(supplierService.getSupplierDeptMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(
                Collections.singletonMap(7L, buildUser(7L, "张三", 202L)));
        when(deptApi.getDeptMap(anyCollection())).thenReturn(Collections.singletonMap(
                202L, buildDept(202L, "创建人部门")));
        when(supplierBusinessInfoMapper.selectListBySupplierIds(anyCollection())).thenReturn(Collections.emptyList());

        ErpSupplierRespVO result = controller.getSupplier(1L).getData();

        assertEquals(202L, result.getCreateDeptId());
        assertEquals("创建人部门", result.getCreateDeptName());
        assertEquals(false, result.getBusinessInfoSynced());
    }

    private static AdminUserRespDTO buildUser(Long id, String nickname, Long deptId) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname(nickname);
        user.setDeptId(deptId);
        return user;
    }

    private static DeptRespDTO buildDept(Long id, String name) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        return dept;
    }

}
