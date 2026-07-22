package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchAdjustPriceReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchUpdatePriceFieldsReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUniversalMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_FIELD_NO_PERMISSION;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_READONLY_BY_SALE_DISTRIBUTION;

class ErpProductServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpProductServiceImpl productService;

    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpProductUniversalMapper productUniversalMapper;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private ConfigApi configApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpFieldConfigService fieldConfigService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productService, "productMapper", productMapper);
        ReflectionTestUtils.setField(productService, "noRedisDAO", noRedisDAO);
    }

    @Test
    void isLowStockWarning_whenStockIsZero_thenWarns() {
        assertTrue(ErpProductServiceImpl.isLowStockWarning(BigDecimal.ZERO, null));
    }

    @Test
    void isLowStockWarning_whenStockIsPositiveAndBelowMin_thenWarns() {
        assertTrue(ErpProductServiceImpl.isLowStockWarning(BigDecimal.ONE, 5));
    }

    @Test
    void isLowStockWarning_whenStockIsPositiveAndMinMissing_thenDoesNotWarn() {
        assertFalse(ErpProductServiceImpl.isLowStockWarning(BigDecimal.ONE, null));
    }

    @Test
    void isLowStockWarning_whenStockIsAboveMin_thenDoesNotWarn() {
        assertFalse(ErpProductServiceImpl.isLowStockWarning(BigDecimal.valueOf(13), 10));
    }

    @Test
    void fillProductAuditInfo_resolvesCreatorAndKeepsPersistedCreateDepartment() {
        ErpProductRespVO product = new ErpProductRespVO();
        product.setCreator("7");
        product.setCreateDeptId(101L);

        AdminUserRespDTO creator = new AdminUserRespDTO();
        creator.setId(7L);
        creator.setNickname("张三");
        creator.setDeptId(202L);

        Map<Long, DeptRespDTO> deptMap = new HashMap<>();
        deptMap.put(100L, buildDept(100L, "总公司", 0L));
        deptMap.put(101L, buildDept(101L, "创建部门", 100L));
        deptMap.put(202L, buildDept(202L, "当前所属部门", 100L));

        ErpProductServiceImpl.fillProductAuditInfo(product,
                Collections.singletonMap(7L, creator), deptMap);

        assertEquals("张三", product.getCreatorName());
        assertEquals(101L, product.getCreateDeptId());
        assertEquals("总公司 / 创建部门", product.getCreateDeptName());
    }

    @Test
    void fillProductAuditInfo_usesCreatorDepartmentForLegacyProduct() {
        ErpProductRespVO product = new ErpProductRespVO();
        product.setCreator("7");

        AdminUserRespDTO creator = new AdminUserRespDTO();
        creator.setId(7L);
        creator.setNickname("张三");
        creator.setDeptId(202L);

        ErpProductServiceImpl.fillProductAuditInfo(product,
                Collections.singletonMap(7L, creator),
                Collections.singletonMap(202L, buildDept(202L, "销售二部", 0L)));

        assertEquals("张三", product.getCreatorName());
        assertEquals(202L, product.getCreateDeptId());
        assertEquals("销售二部", product.getCreateDeptName());
    }

    @Test
    void fillProductWarehouseInfo_resolvesDefaultWarehouseName() {
        ErpProductRespVO product = new ErpProductRespVO();
        product.setDefaultWarehouseId(11L);
        ErpWarehouseDO warehouse = ErpWarehouseDO.builder()
                .id(11L)
                .name("主仓库")
                .build();

        ErpProductServiceImpl.fillProductWarehouseInfo(product,
                Collections.singletonMap(11L, warehouse));

        assertEquals("主仓库", product.getDefaultWarehouseName());
    }

    @Test
    void generateProductCode_whenRedisUnavailable_thenFallbackToDatabase() {
        when(productMapper.selectCodesByPrefix("P")).thenReturn(Arrays.asList("P000123", "PABC"));
        doThrow(new RuntimeException("redis down")).when(noRedisDAO).generatePlain("P");

        String code = ReflectionTestUtils.invokeMethod(productService, "generateProductCode", 0);

        assertEquals("P000124", code);
    }

    @Test
    void applyProductArchiveVisibleScope_mergesOnlySaleSelectableDistributedWarehouses() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));
        when(warehouseService.getCurrentUserSaleDistributedVisibleWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Arrays.asList(21L, 22L)));
        when(warehouseService.getCurrentUserVisibleSaleWarehouseList()).thenReturn(Arrays.asList(
                ErpWarehouseDO.builder().id(11L).build(),
                ErpWarehouseDO.builder().id(21L).build()));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);
            ErpProductPageReqVO reqVO = new ErpProductPageReqVO();

            ReflectionTestUtils.invokeMethod(productService, "applyProductArchiveVisibleScope", reqVO);

            assertFalse(reqVO.getVisibleAll());
            assertEquals(Collections.singleton(200L), reqVO.getVisibleDeptIds());
            assertEquals(new LinkedHashSet<>(Arrays.asList(11L, 21L)), reqVO.getVisibleWarehouseIds());
        }
    }

    @Test
    void buildCurrentUserVisibleReqVO_doesNotExpandSaleDistributedWarehouses() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            ErpProductPageReqVO reqVO = ReflectionTestUtils.invokeMethod(
                    productService, "buildCurrentUserVisibleReqVO");

            assertEquals(new LinkedHashSet<>(Collections.singletonList(11L)), reqVO.getVisibleWarehouseIds());
            verify(warehouseService, never()).getCurrentUserSaleDistributedVisibleWarehouseIds();
        }
    }

    @Test
    void getProductVOPage_withoutArchiveMode_doesNotExpandSaleDistributedWarehouses() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());
        when(fieldConfigService.getFieldConfigListByModule("erp_product")).thenReturn(Collections.emptyList());
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));
        when(productMapper.selectPage(any(ErpProductPageReqVO.class), anyCollection(), anyCollection()))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            productService.getProductVOPage(new ErpProductPageReqVO());

            verify(warehouseService, never()).getCurrentUserSaleDistributedVisibleWarehouseIds();
        }
    }

    @Test
    void getSaleDistributedReadonlyProductIds_keepsBaseVisibleProductEditable() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));
        when(productMapper.selectVisibleListByIds(any(), any()))
                .thenReturn(Collections.singletonList(ErpProductDO.builder().id(1L).build()));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            Set<Long> readonlyIds = ReflectionTestUtils.invokeMethod(productService,
                    "getSaleDistributedReadonlyProductIds", Arrays.asList(1L, 2L));

            assertEquals(Collections.singleton(2L), readonlyIds);
        }
    }

    @Test
    void updateProductsShelf_whenOnlyArchiveVisible_thenRejectsWrite() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));
        when(warehouseService.getCurrentUserSaleDistributedVisibleWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(21L)));
        when(warehouseService.getCurrentUserVisibleSaleWarehouseList())
                .thenReturn(Collections.singletonList(ErpWarehouseDO.builder().id(21L).build()));
        when(productMapper.selectVisibleById(eq(2L), any()))
                .thenReturn(null, ErpProductDO.builder().id(2L).name("只读配件").build());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> productService.updateProductsShelf(Collections.singleton(2L), "A-01"));

            assertEquals(PRODUCT_READONLY_BY_SALE_DISTRIBUTION.getCode(), ex.getCode());
            verify(productMapper, never()).updateShelfByIds(any(), any());
        }
    }

    @Test
    void batchUpdatePriceFields_whenSubmittedPriceHidden_thenRejectsWrite() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));
        when(productMapper.selectVisibleById(eq(1L), any()))
                .thenReturn(ErpProductDO.builder().id(1L).build());
        when(permissionApi.getCurrentUserHiddenFields("erp_product"))
                .thenReturn(Collections.singletonList("col_retailPrice"));
        ErpPartsBatchUpdatePriceFieldsReqVO reqVO = new ErpPartsBatchUpdatePriceFieldsReqVO();
        reqVO.setId(1L);
        reqVO.setRetailPrice(BigDecimal.TEN);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> productService.batchUpdatePriceFields(Collections.singletonList(reqVO)));

            assertEquals(PRODUCT_FIELD_NO_PERMISSION.getCode(), ex.getCode());
            verify(productMapper, never()).updateById(any(ErpProductDO.class));
        }
    }

    @Test
    void batchAdjustPrice_whenSourcePriceHidden_thenRejectsBeforeQuery() {
        when(permissionApi.getCurrentUserHiddenFields("erp_product"))
                .thenReturn(Collections.singletonList("referencePrice"));
        ErpPartsBatchAdjustPriceReqVO reqVO = new ErpPartsBatchAdjustPriceReqVO();
        reqVO.setSourcePriceType("REFERENCE_PRICE");
        reqVO.setTargetPriceType("RETAIL_PRICE");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productService.batchAdjustPrice(reqVO));

        assertEquals(PRODUCT_FIELD_NO_PERMISSION.getCode(), ex.getCode());
        verify(productMapper, never()).selectMaps(any());
    }

    private void mockProductPermission(Long loginUserId, Set<Long> deptIds) {
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setAll(false);
        permission.setSelf(false);
        permission.setDeptIds(deptIds);
        when(permissionApi.getDeptDataPermission(loginUserId, "erp_product")).thenReturn(permission);
    }

    private static DeptRespDTO buildDept(Long id, String name, Long parentId) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setParentId(parentId);
        return dept;
    }

}
