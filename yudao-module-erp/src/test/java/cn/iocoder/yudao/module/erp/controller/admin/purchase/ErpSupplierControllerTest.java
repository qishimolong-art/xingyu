package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierBusinessInfoDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierBusinessInfoMapper;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierQueryService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @BeforeEach
    void setUpSupplierQueryService() {
        ErpSupplierQueryService queryService = new ErpSupplierQueryService();
        ReflectionTestUtils.setField(queryService, "supplierService", supplierService);
        ReflectionTestUtils.setField(queryService, "deptApi", deptApi);
        ReflectionTestUtils.setField(queryService, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(controller, "supplierQueryService", queryService);
        when(supplierService.getSupplierDeptMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(deptApi.getDeptMap(anyCollection())).thenReturn(Collections.emptyMap());
    }

    @Test
    void getSupplierPage_masks500RowsWithOnePermissionReadPerModule() {
        PermissionApi permissionApi = useRealFieldMasker();
        when(permissionApi.getCurrentUserHiddenFields("erp_supplier"))
                .thenReturn(Arrays.asList("col_purchaser", "bankAccount"));
        List<ErpSupplierDO> suppliers = IntStream.rangeClosed(1, 500)
                .mapToObj(id -> new ErpSupplierDO().setId((long) id).setName("Supplier " + id)
                        .setPurchaser("7").setBankAccount("00001234"))
                .collect(Collectors.toList());
        ErpSupplierPageReqVO query = new ErpSupplierPageReqVO();
        when(supplierService.getSupplierPage(query)).thenReturn(new PageResult<>(suppliers, 500L));
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(
                Collections.singletonMap(7L, buildUser(7L, "采购员", 202L)));

        PageResult<ErpSupplierRespVO> result = controller.getSupplierPage(query).getData();

        assertEquals(500L, result.getTotal());
        assertEquals(500, result.getList().size());
        result.getList().forEach(row -> {
            assertNull(row.getPurchaser());
            assertNull(row.getPurchaserName());
            assertNull(row.getBankAccount());
            assertEquals("Supplier " + row.getId(), row.getName());
        });
        verify(permissionApi).getCurrentUserHiddenFields("erp_supplier");
        verify(permissionApi).getCurrentUserHiddenFields("erp_product");
        verifyNoMoreInteractions(permissionApi);
    }

    @Test
    void getSupplierPage_keepsVisiblePurchaserAndSkipsPermissionReadForEmptyPage() {
        PermissionApi permissionApi = useRealFieldMasker();
        ErpSupplierPageReqVO query = new ErpSupplierPageReqVO();
        when(supplierService.getSupplierPage(query)).thenReturn(new PageResult<>(Collections.emptyList(), 0L));
        assertEquals(0L, controller.getSupplierPage(query).getData().getTotal());
        verifyNoInteractions(permissionApi);

        when(supplierService.getSupplierPage(query)).thenReturn(new PageResult<>(Collections.singletonList(
                new ErpSupplierDO().setId(1L).setPurchaser("7").setBankAccount("00001234")), 1L));
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(
                Collections.singletonMap(7L, buildUser(7L, "采购员", 202L)));
        ErpSupplierRespVO row = controller.getSupplierPage(query).getData().getList().get(0);
        assertEquals("7", row.getPurchaser());
        assertEquals("采购员", row.getPurchaserName());
        assertEquals("00001234", row.getBankAccount());
        verify(permissionApi).getCurrentUserHiddenFields("erp_supplier");
        verify(permissionApi).getCurrentUserHiddenFields("erp_product");
        verifyNoMoreInteractions(permissionApi);
    }

    @Test
    void getSupplier_masksPurchaserDisplayNameTogetherWithHiddenId() {
        PermissionApi permissionApi = useRealFieldMasker();
        when(permissionApi.getCurrentUserHiddenFields("erp_supplier"))
                .thenReturn(Collections.singletonList("purchaser"));
        when(supplierService.getSupplier(1L)).thenReturn(new ErpSupplierDO().setId(1L).setPurchaser("7"));
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(
                Collections.singletonMap(7L, buildUser(7L, "采购员", 202L)));

        ErpSupplierRespVO row = controller.getSupplier(1L).getData();

        assertNull(row.getPurchaser());
        assertNull(row.getPurchaserName());
        verify(permissionApi).getCurrentUserHiddenFields("erp_supplier");
        verify(permissionApi).getCurrentUserHiddenFields("erp_product");
        verifyNoMoreInteractions(permissionApi);
    }

    private PermissionApi useRealFieldMasker() {
        PermissionApi permissionApi = mock(PermissionApi.class);
        ErpPurchaseFieldPermissionMasker masker = new ErpPurchaseFieldPermissionMasker();
        ReflectionTestUtils.setField(masker, "permissionApi", permissionApi);
        ReflectionTestUtils.setField(controller, "fieldPermissionMasker", masker);
        return permissionApi;
    }

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
        when(supplierBusinessInfoMapper.selectSyncedSupplierIds(anyCollection())).thenReturn(
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
        when(supplierBusinessInfoMapper.selectSyncedSupplierIds(anyCollection())).thenReturn(Collections.emptyList());

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
