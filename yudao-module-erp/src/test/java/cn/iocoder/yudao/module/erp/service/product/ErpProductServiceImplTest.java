package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryListReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchAdjustPriceReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchUpdatePriceFieldsReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ProductSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductBrandDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductDeptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUniversalMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockLockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.base.ErpArchiveMergeService;
import cn.iocoder.yudao.module.erp.service.common.ErpMnemonicCodeUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.mall.ErpMallProductSyncPublisher;
import cn.iocoder.yudao.module.erp.service.stock.ErpProductStockInitService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_READONLY_BY_SALE_DISTRIBUTION;

class ErpProductServiceImplTest extends BaseMockitoUnitTest {
    @Mock
    private cn.iocoder.yudao.module.erp.service.stock.cost.ErpStockDimensionService stockDimensionService;
    @Mock
    private ErpProductStockInitService productStockInitService;


    @InjectMocks
    private ErpProductServiceImpl productService;

    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpProductDeptMapper productDeptMapper;
    @Mock
    private ErpProductUniversalMapper productUniversalMapper;
    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private ErpStockLockMapper stockLockMapper;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private ConfigApi configApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpProductCategoryService productCategoryService;
    @Mock
    private ErpProductUnitService productUnitService;
    @Mock
    private ErpProductBrandService productBrandService;
    @Mock
    private ErpFieldConfigService fieldConfigService;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpMallProductSyncPublisher mallProductSyncPublisher;
    @Mock
    private ErpArchiveMergeService archiveMergeService;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(ErpProductMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, ErpProductDO.class);
    }

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
    void createProduct_whenBarCodeBlank_thenStoresNullBarCode() {
        ProductSaveReqVO reqVO = new ProductSaveReqVO();
        reqVO.setCode("P-CREATE-001");
        reqVO.setName("Brake Pad");
        reqVO.setBarCode("   ");
        reqVO.setCategoryId(102L);
        reqVO.setUnitId(201L);
        reqVO.setDefaultWarehouseId(301L);
        reqVO.setStatus(0);

        ErpProductCategoryDO category = ErpProductCategoryDO.builder().id(102L).name("Parts").build();
        ErpWarehouseDO warehouse = ErpWarehouseDO.builder().id(301L).name("Main").deptId(401L).build();

        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());
        when(fieldConfigService.getFieldConfigListByModule("erp_product")).thenReturn(Collections.emptyList());
        when(productCategoryService.getProductCategory(102L)).thenReturn(category);
        when(productCategoryService.getProductCategoryChildCount(102L)).thenReturn(0L);
        when(warehouseService.getWarehouse(301L)).thenReturn(warehouse);
        when(productUniversalMapper.selectListByProductId(501L)).thenReturn(Collections.emptyList());
        when(productMapper.selectById(501L)).thenReturn(ErpProductDO.builder()
                .id(501L)
                .code("P-CREATE-001")
                .name("Brake Pad")
                .categoryId(102L)
                .unitId(201L)
                .defaultWarehouseId(301L)
                .status(0)
                .build());
        when(productMapper.insert(any(ErpProductDO.class))).thenAnswer(invocation -> {
            ErpProductDO product = invocation.getArgument(0);
            product.setId(501L);
            return 1;
        });

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(401L);

            Long productId = productService.createProduct(reqVO);

            assertEquals(501L, productId);
        }
        ArgumentCaptor<ErpProductDO> productCaptor = ArgumentCaptor.forClass(ErpProductDO.class);
        verify(productMapper).insert(productCaptor.capture());
        assertNull(productCaptor.getValue().getBarCode());
    }

    @Test
    void createProduct_whenBrandEnabled_thenStoresBrandName() {
        ProductSaveReqVO reqVO = new ProductSaveReqVO();
        reqVO.setCode("P-CREATE-002");
        reqVO.setName("Oil Filter");
        reqVO.setBrand("博世");
        reqVO.setCategoryId(102L);
        reqVO.setUnitId(201L);
        reqVO.setDefaultWarehouseId(301L);
        reqVO.setStatus(0);

        ErpProductCategoryDO category = ErpProductCategoryDO.builder().id(102L).name("Parts").build();
        ErpWarehouseDO warehouse = ErpWarehouseDO.builder().id(301L).name("Main").deptId(401L).build();

        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());
        when(fieldConfigService.getFieldConfigListByModule("erp_product")).thenReturn(Collections.emptyList());
        when(productCategoryService.getProductCategory(102L)).thenReturn(category);
        when(productCategoryService.getProductCategoryChildCount(102L)).thenReturn(0L);
        when(warehouseService.getWarehouse(301L)).thenReturn(warehouse);
        when(productUniversalMapper.selectListByProductId(502L)).thenReturn(Collections.emptyList());
        when(productMapper.selectById(502L)).thenReturn(ErpProductDO.builder()
                .id(502L)
                .code("P-CREATE-002")
                .name("Oil Filter")
                .brand("博世")
                .categoryId(102L)
                .unitId(201L)
                .defaultWarehouseId(301L)
                .status(0)
                .build());
        when(productMapper.insert(any(ErpProductDO.class))).thenAnswer(invocation -> {
            ErpProductDO product = invocation.getArgument(0);
            product.setId(502L);
            return 1;
        });

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(401L);

            Long productId = productService.createProduct(reqVO);

            assertEquals(502L, productId);
        }
        ArgumentCaptor<ErpProductDO> productCaptor = ArgumentCaptor.forClass(ErpProductDO.class);
        verify(productBrandService).validateEnabledProductBrand("博世");
        verify(productMapper).insert(productCaptor.capture());
        assertEquals("博世", productCaptor.getValue().getBrand());
    }

    @Test
    void createProduct_whenNameDuplicate_thenRejects() {
        ProductSaveReqVO reqVO = new ProductSaveReqVO();
        reqVO.setCode("P-CREATE-003");
        reqVO.setName(" Brake Pad ");
        reqVO.setCategoryId(102L);
        reqVO.setUnitId(201L);
        reqVO.setDefaultWarehouseId(301L);
        reqVO.setStatus(0);

        ErpProductCategoryDO category = ErpProductCategoryDO.builder().id(102L).name("Parts").build();
        ErpWarehouseDO warehouse = ErpWarehouseDO.builder().id(301L).name("Main").deptId(401L).build();
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());
        when(productCategoryService.getProductCategory(102L)).thenReturn(category);
        when(productCategoryService.getProductCategoryChildCount(102L)).thenReturn(0L);
        when(warehouseService.getWarehouse(301L)).thenReturn(warehouse);
        when(productMapper.selectByNameExcludeId(eq("Brake Pad"), eq(null)))
                .thenReturn(ErpProductDO.builder().id(900L).name("Brake Pad").build());

        ServiceException ex = assertThrows(ServiceException.class, () -> productService.createProduct(reqVO));

        assertEquals(PRODUCT_NAME_DUPLICATE.getCode(), ex.getCode());
        verify(productMapper, never()).insert(any(ErpProductDO.class));
    }

    @Test
    void updateProduct_whenNameDuplicate_thenRejects() {
        ProductSaveReqVO reqVO = new ProductSaveReqVO();
        reqVO.setId(601L);
        reqVO.setCode("P-UPDATE-001");
        reqVO.setName(" Oil Filter ");
        reqVO.setCategoryId(102L);
        reqVO.setUnitId(201L);
        reqVO.setDefaultWarehouseId(301L);
        reqVO.setStatus(0);

        ErpProductDO existing = ErpProductDO.builder()
                .id(601L)
                .code("P-UPDATE-001")
                .name("Brake Pad")
                .categoryId(102L)
                .unitId(201L)
                .defaultWarehouseId(301L)
                .status(0)
                .build();
        ErpProductCategoryDO category = ErpProductCategoryDO.builder().id(102L).name("Parts").build();
        ErpWarehouseDO warehouse = ErpWarehouseDO.builder().id(301L).name("Main").deptId(401L).build();

        when(productMapper.selectVisibleById(eq(601L), any(ErpProductPageReqVO.class))).thenReturn(existing);
        when(productMapper.selectById(601L)).thenReturn(existing);
        when(productUniversalMapper.selectListByProductId(601L)).thenReturn(Collections.emptyList());
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());
        when(fieldConfigService.getFieldConfigListByModule("erp_product")).thenReturn(Collections.emptyList());
        when(productCategoryService.getProductCategory(102L)).thenReturn(category);
        when(productCategoryService.getProductCategoryChildCount(102L)).thenReturn(0L);
        when(warehouseService.getWarehouse(301L)).thenReturn(warehouse);
        when(productMapper.selectByNameExcludeId(eq("Oil Filter"), eq(601L)))
                .thenReturn(ErpProductDO.builder().id(602L).name("Oil Filter").build());

        ServiceException ex = assertThrows(ServiceException.class, () -> productService.updateProduct(reqVO));

        assertEquals(PRODUCT_NAME_DUPLICATE.getCode(), ex.getCode());
        verify(productMapper, never()).updateById(any(ErpProductDO.class));
    }

    @Test
    void updateProduct_whenBrandFieldHidden_thenKeepsExistingBrandWithoutRevalidating() {
        ProductSaveReqVO reqVO = new ProductSaveReqVO();
        reqVO.setId(601L);
        reqVO.setCode("P-UPDATE-001");
        reqVO.setName("Brake Pad");
        reqVO.setBrand("新品牌");

        ErpProductDO existing = ErpProductDO.builder()
                .id(601L)
                .code("P-UPDATE-001")
                .name("Brake Pad")
                .brand("旧品牌")
                .build();
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.singletonList("brand"));

        ReflectionTestUtils.invokeMethod(productService, "applyProductSaveFieldPermissions", reqVO, existing);
        ReflectionTestUtils.invokeMethod(productService, "validateProductBrandSelection", reqVO.getBrand(), existing);

        assertEquals("旧品牌", reqVO.getBrand());
        verify(productBrandService, never()).validateEnabledProductBrand(any());
    }

    @Test
    void importProductList_whenCategoryNamesDuplicated_thenMatchesByCategoryCode() {
        ErpProductImportExcelVO row = new ErpProductImportExcelVO();
        row.setCode("P-001");
        row.setName("刹车片");
        row.setCategoryCode("CAT-B");
        row.setUnitName("个");
        row.setDefaultWarehouseName("主仓");

        ErpProductCategoryDO firstCategory = ErpProductCategoryDO.builder()
                .id(101L)
                .name("保养件")
                .code("CAT-A")
                .build();
        ErpProductCategoryDO secondCategory = ErpProductCategoryDO.builder()
                .id(102L)
                .name("保养件")
                .code("CAT-B")
                .build();
        ErpProductUnitDO unit = ErpProductUnitDO.builder()
                .id(201L)
                .name("个")
                .build();
        ErpWarehouseDO warehouse = ErpWarehouseDO.builder()
                .id(301L)
                .name("主仓")
                .deptId(401L)
                .build();

        when(productCategoryService.getProductCategoryList(any(ErpProductCategoryListReqVO.class)))
                .thenReturn(Arrays.asList(firstCategory, secondCategory));
        when(productUnitService.getProductUnitListByStatus(any())).thenReturn(Collections.singletonList(unit));
        when(productBrandService.getProductBrandListByStatus(any())).thenReturn(Collections.emptyList());
        when(warehouseService.getWarehouseListByStatus(any())).thenReturn(Collections.singletonList(warehouse));
        when(productMapper.selectListByCodes(any())).thenReturn(Collections.emptyList());
        when(productCategoryService.getProductCategory(102L)).thenReturn(secondCategory);
        when(productCategoryService.getProductCategoryChildCount(102L)).thenReturn(0L);
        when(warehouseService.getWarehouse(301L)).thenReturn(warehouse);
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());
        when(productMapper.insert(any(ErpProductDO.class))).thenAnswer(invocation -> {
            ErpProductDO product = invocation.getArgument(0);
            product.setId(500L);
            return 1;
        });

        ErpProductImportRespVO result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(401L);

            result = productService.importProductList(Collections.singletonList(row));
        }

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getCreateCount());
        assertEquals(0, result.getFailureCount());
        ArgumentCaptor<ErpProductDO> productCaptor = ArgumentCaptor.forClass(ErpProductDO.class);
        verify(productMapper).insert(productCaptor.capture());
        assertEquals(102L, productCaptor.getValue().getCategoryId());
        assertEquals(ErpMnemonicCodeUtils.buildPinyinCode("刹车片"), productCaptor.getValue().getPinyinCode());
        assertEquals(ErpMnemonicCodeUtils.buildWubiCode("刹车片"), productCaptor.getValue().getWubiCode());
    }

    @Test
    void importProductList_whenBrandEnabled_thenStoresBrandName() {
        ErpProductImportExcelVO row = new ErpProductImportExcelVO();
        row.setCode("P-003");
        row.setName("雨刮片");
        row.setCategoryCode("CAT-A");
        row.setUnitName("个");
        row.setBrand("博世");
        row.setPinyinCode("YGP");
        row.setWubiCode("TTT");
        row.setDefaultWarehouseName("主仓");

        ErpProductCategoryDO category = ErpProductCategoryDO.builder()
                .id(101L)
                .name("保养件")
                .code("CAT-A")
                .build();
        ErpProductUnitDO unit = ErpProductUnitDO.builder()
                .id(201L)
                .name("个")
                .build();
        ErpProductBrandDO brand = ErpProductBrandDO.builder()
                .id(301L)
                .name("博世")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        ErpWarehouseDO warehouse = ErpWarehouseDO.builder()
                .id(401L)
                .name("主仓")
                .deptId(501L)
                .build();

        when(productCategoryService.getProductCategoryList(any(ErpProductCategoryListReqVO.class)))
                .thenReturn(Collections.singletonList(category));
        when(productUnitService.getProductUnitListByStatus(any())).thenReturn(Collections.singletonList(unit));
        when(productBrandService.getProductBrandListByStatus(any())).thenReturn(Collections.singletonList(brand));
        when(warehouseService.getWarehouseListByStatus(any())).thenReturn(Collections.singletonList(warehouse));
        when(productMapper.selectListByCodes(any())).thenReturn(Collections.emptyList());
        when(productCategoryService.getProductCategory(101L)).thenReturn(category);
        when(productCategoryService.getProductCategoryChildCount(101L)).thenReturn(0L);
        when(warehouseService.getWarehouse(401L)).thenReturn(warehouse);
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());
        when(productMapper.insert(any(ErpProductDO.class))).thenAnswer(invocation -> {
            ErpProductDO product = invocation.getArgument(0);
            product.setId(500L);
            return 1;
        });

        ErpProductImportRespVO result = productService.importProductList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        ArgumentCaptor<ErpProductDO> productCaptor = ArgumentCaptor.forClass(ErpProductDO.class);
        verify(productMapper).insert(productCaptor.capture());
        assertEquals("博世", productCaptor.getValue().getBrand());
        assertEquals("YGP", productCaptor.getValue().getPinyinCode());
        assertEquals("TTT", productCaptor.getValue().getWubiCode());
    }

    @Test
    void importProductList_whenBrandDisabledOrMissing_thenRecordsFailure() {
        ErpProductImportExcelVO row = new ErpProductImportExcelVO();
        row.setCode("P-004");
        row.setName("雨刮片");
        row.setCategoryCode("CAT-A");
        row.setUnitName("个");
        row.setBrand("停用品牌");

        when(productCategoryService.getProductCategoryList(any(ErpProductCategoryListReqVO.class))).thenReturn(Collections.singletonList(
                ErpProductCategoryDO.builder().id(101L).name("保养件").code("CAT-A").build()));
        when(productUnitService.getProductUnitListByStatus(any())).thenReturn(Collections.singletonList(
                ErpProductUnitDO.builder().id(201L).name("个").build()));
        when(productBrandService.getProductBrandListByStatus(any())).thenReturn(Collections.emptyList());
        when(warehouseService.getWarehouseListByStatus(any())).thenReturn(Collections.emptyList());
        when(productMapper.selectListByCodes(any())).thenReturn(Collections.emptyList());

        ErpProductImportRespVO result = productService.importProductList(Collections.singletonList(row));

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals("配件品牌不存在或已停用：停用品牌", result.getFailureDetails().get(0).getReason());
        verify(productMapper, never()).insert(any(ErpProductDO.class));
    }

    @Test
    void importProductList_whenCategoryCodeMissing_thenRecordsFailure() {
        ErpProductImportExcelVO row = new ErpProductImportExcelVO();
        row.setCode("P-002");
        row.setName("机油滤芯");
        row.setBarCode("BC-002");
        row.setCategoryCode("CAT-MISSING");
        row.setUnitName("个");
        row.setDefaultWarehouseName("主仓");

        when(productCategoryService.getProductCategoryList(any(ErpProductCategoryListReqVO.class))).thenReturn(Collections.singletonList(
                ErpProductCategoryDO.builder().id(101L).name("保养件").code("CAT-A").build()));
        when(productUnitService.getProductUnitListByStatus(any())).thenReturn(Collections.singletonList(
                ErpProductUnitDO.builder().id(201L).name("个").build()));
        when(productBrandService.getProductBrandListByStatus(any())).thenReturn(Collections.emptyList());
        when(warehouseService.getWarehouseListByStatus(any())).thenReturn(Collections.singletonList(
                ErpWarehouseDO.builder().id(301L).name("主仓").build()));
        when(productMapper.selectListByCodes(any())).thenReturn(Collections.emptyList());

        ErpProductImportRespVO result = productService.importProductList(Collections.singletonList(row));

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals("配件分类编码不存在：CAT-MISSING", result.getFailureDetails().get(0).getReason());
        verify(productMapper, never()).insert(any(ErpProductDO.class));
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
    void getProductVOPage_whenSkippingPriceViewPermission_keepsPriceFieldsButAppliesRoleHiddenFields() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(permissionApi.getCurrentUserHiddenFields("erp_product", null, false))
                .thenReturn(Collections.singletonList("name"));
        when(fieldConfigService.getFieldConfigListByModule("erp_product")).thenReturn(Collections.emptyList());
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));
        when(productMapper.selectPage(any(ErpProductPageReqVO.class), anyCollection(), anyCollection()))
                .thenReturn(new PageResult<>(Collections.singletonList(ErpProductDO.builder()
                        .id(1L)
                        .name("刹车片")
                        .retailPrice(BigDecimal.TEN)
                        .build()), 1L));
        when(productCategoryService.getProductCategoryMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(productUnitService.getProductUnitMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(productDeptMapper.selectListByProductIds(anyCollection())).thenReturn(Collections.emptyList());
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(Collections.emptyList());
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(stockMapper.selectSumMapByProductIds(anyCollection())).thenReturn(Collections.emptyMap());
        when(stockLockMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(productUniversalMapper.selectListByProductIds(anyCollection())).thenReturn(Collections.emptyList());

        PageResult<ErpProductRespVO> result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            result = productService.getProductVOPage(new ErpProductPageReqVO(), false);
        }

        assertEquals(1L, result.getTotal());
        assertEquals(BigDecimal.TEN, result.getList().get(0).getRetailPrice());
        assertEquals(null, result.getList().get(0).getName());
        verify(permissionApi, never()).getCurrentUserHiddenFields("erp_product");
    }

    @Test
    void getProductDetail_appliesDefaultProductFieldPermissionMasking() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));
        ErpProductDO product = ErpProductDO.builder()
                .id(1L)
                .deptId(20L)
                .categoryId(2L)
                .unitId(3L)
                .name("閰嶄欢")
                .retailPrice(BigDecimal.TEN)
                .build();
        when(productMapper.selectVisibleById(eq(1L), any(ErpProductPageReqVO.class))).thenReturn(product);
        when(productCategoryService.getProductCategoryMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(productUnitService.getProductUnitMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(productDeptMapper.selectListByProductIds(anyCollection())).thenReturn(Collections.emptyList());
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(Collections.emptyList());
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(deptApi.getDeptMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(stockMapper.selectSumMapByProductIds(anyCollection())).thenReturn(Collections.emptyMap());
        when(stockLockMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(productUniversalMapper.selectListByProductIds(anyCollection())).thenReturn(Collections.emptyList());
        when(fieldConfigService.getFieldConfigListByModule("erp_product")).thenReturn(Collections.emptyList());
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());

        ErpProductRespVO detail;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            detail = productService.getProductDetail(1L);
        }

        assertEquals(BigDecimal.TEN, detail.getRetailPrice());
        verify(permissionApi).getCurrentUserHiddenFields("erp_product");
        verify(permissionApi, never()).getCurrentUserHiddenFields("erp_product", 20L);
        verify(permissionApi, never()).getCurrentUserHiddenFields("erp_product", null, false);
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
    void mergeProduct_passesKeepProductIdentityToArchiveMergeService() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));
        when(fieldConfigService.getFieldConfigListByModule("erp_product"))
                .thenReturn(Collections.emptyList());
        ErpProductDO source = ErpProductDO.builder()
                .id(10L)
                .code("SRC")
                .name("源配件")
                .mergedFlag(false)
                .build();
        ErpProductDO keep = ErpProductDO.builder()
                .id(20L)
                .code("KEEP")
                .name("保留配件")
                .mergedFlag(false)
                .build();
        when(productMapper.selectVisibleById(eq(10L), any())).thenReturn(source);
        when(productMapper.selectVisibleById(eq(20L), any())).thenReturn(keep);
        when(productMapper.selectById(10L)).thenReturn(source);
        when(productMapper.selectById(20L)).thenReturn(keep);
        when(productUniversalMapper.selectListByProductId(10L)).thenReturn(Collections.emptyList());
        when(productUniversalMapper.selectListByProductId(20L)).thenReturn(Collections.emptyList());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            productService.mergeProduct(10L, 20L);
        }

        verify(archiveMergeService).mergeProductReferences(10L, 20L,
                "SRC", "KEEP", "源配件", "保留配件", "104");
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
    void batchUpdatePriceFields_whenPurchaseAndSaleSubmitted_thenUpdatesBothFields() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));
        when(productMapper.selectVisibleById(eq(1L), any()))
                .thenReturn(ErpProductDO.builder().id(1L).build());
        when(permissionApi.getCurrentUserHiddenFields("erp_product"))
                .thenReturn(Collections.emptyList());
        when(fieldConfigService.getFieldConfigListByModule("erp_product"))
                .thenReturn(Collections.emptyList());
        when(productMapper.selectById(1L))
                .thenReturn(ErpProductDO.builder().id(1L).build());
        when(productUniversalMapper.selectListByProductId(1L))
                .thenReturn(Collections.emptyList());
        ErpPartsBatchUpdatePriceFieldsReqVO reqVO = new ErpPartsBatchUpdatePriceFieldsReqVO();
        reqVO.setId(1L);
        reqVO.setPurchasePrice(new BigDecimal("13.50"));
        reqVO.setSalePrice(new BigDecimal("18.90"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            productService.batchUpdatePriceFields(Collections.singletonList(reqVO));
        }

        ArgumentCaptor<ErpProductDO> productCaptor = ArgumentCaptor.forClass(ErpProductDO.class);
        verify(productMapper).updateById(productCaptor.capture());
        assertEquals(new BigDecimal("13.50"), productCaptor.getValue().getPurchasePrice());
        assertEquals(new BigDecimal("18.90"), productCaptor.getValue().getSalePrice());
    }

    @Test
    void batchUpdatePriceFields_whenCustomPriceSubmitted_thenUpdatesPhysicalColumn() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));
        when(productMapper.selectVisibleById(eq(1L), any()))
                .thenReturn(ErpProductDO.builder().id(1L).build());
        when(permissionApi.getCurrentUserHiddenFields("erp_product"))
                .thenReturn(Collections.emptyList());
        when(fieldConfigService.getFieldConfigListByModule("erp_product"))
                .thenReturn(Collections.singletonList(customPriceField()));
        ErpPartsBatchUpdatePriceFieldsReqVO reqVO = new ErpPartsBatchUpdatePriceFieldsReqVO();
        reqVO.setId(1L);
        reqVO.setCustomFields(Collections.singletonMap("vipPrice", "12.340"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            productService.batchUpdatePriceFields(Collections.singletonList(reqVO));
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> valuesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(productMapper).updateCustomFields(eq(1L), valuesCaptor.capture());
        assertEquals(new BigDecimal("12.340"), valuesCaptor.getValue().get("ext_vip_price"));
        verify(productMapper).updateById(any(ErpProductDO.class));
    }

    @Test
    void batchUpdatePriceFields_whenCustomPriceHidden_thenRejectsEvenNullWrite() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));
        when(productMapper.selectVisibleById(eq(1L), any()))
                .thenReturn(ErpProductDO.builder().id(1L).build());
        when(permissionApi.getCurrentUserHiddenFields("erp_product"))
                .thenReturn(Collections.singletonList("col_vipPrice"));
        when(fieldConfigService.getFieldConfigListByModule("erp_product"))
                .thenReturn(Collections.singletonList(customPriceField()));
        Map<String, Object> customFields = new HashMap<>();
        customFields.put("vipPrice", null);
        ErpPartsBatchUpdatePriceFieldsReqVO reqVO = new ErpPartsBatchUpdatePriceFieldsReqVO();
        reqVO.setId(1L);
        reqVO.setCustomFields(customFields);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> productService.batchUpdatePriceFields(Collections.singletonList(reqVO)));

            assertEquals(PRODUCT_FIELD_NO_PERMISSION.getCode(), ex.getCode());
        }
        verify(productMapper, never()).updateCustomFields(any(), any());
        verify(productMapper, never()).updateById(any(ErpProductDO.class));
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

    @Test
    void batchAdjustPrice_whenSourcePriceIsNull_thenTreatsAsZero() {
        mockProductPermission(104L, Collections.singleton(200L));
        when(warehouseService.getCurrentUserAuthorizedWarehouseIds())
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(11L)));
        when(permissionApi.getCurrentUserHiddenFields("erp_product"))
                .thenReturn(Collections.emptyList());
        when(productMapper.selectMaps(any()))
                .thenReturn(Collections.singletonList(Collections.singletonMap("id", 1L)));
        ErpProductDO product = ErpProductDO.builder()
                .id(1L)
                .referencePrice(null)
                .build();
        when(productMapper.selectByIds(any()))
                .thenReturn(Collections.singletonList(product));
        when(productMapper.selectById(1L))
                .thenReturn(ErpProductDO.builder().id(1L).referencePrice(null).build());
        when(fieldConfigService.getFieldConfigListByModule("erp_product"))
                .thenReturn(Collections.emptyList());
        when(productUniversalMapper.selectListByProductId(1L))
                .thenReturn(Collections.emptyList());

        ErpPartsBatchAdjustPriceReqVO reqVO = new ErpPartsBatchAdjustPriceReqVO();
        reqVO.setSourcePriceType("REFERENCE_PRICE");
        reqVO.setTargetPriceType("RETAIL_PRICE");
        reqVO.setAdjustMethod("ADD");
        reqVO.setAdjustCoefficient(new BigDecimal("5.12"));
        reqVO.setDecimalPlaces(2);

        int adjusted;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            adjusted = productService.batchAdjustPrice(reqVO);
        }

        assertEquals(1, adjusted);
        ArgumentCaptor<ErpProductDO> productCaptor = ArgumentCaptor.forClass(ErpProductDO.class);
        verify(productMapper).updateById(productCaptor.capture());
        assertEquals(new BigDecimal("5.12"), productCaptor.getValue().getRetailPrice());
    }

    private void mockProductPermission(Long loginUserId, Set<Long> deptIds) {
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setAll(false);
        permission.setSelf(false);
        permission.setDeptIds(deptIds);
        when(permissionApi.getDeptDataPermission(loginUserId, "erp_product")).thenReturn(permission);
    }

    private static ErpFieldConfigDO customPriceField() {
        return ErpFieldConfigDO.builder()
                .fieldName("vipPrice")
                .fieldLabel("会员价")
                .fieldSource("CUSTOM")
                .fieldType("DECIMAL")
                .fieldGroup("price_info")
                .physicalColumn("ext_vip_price")
                .visible(true)
                .readonly(false)
                .required(false)
                .build();
    }

    private static DeptRespDTO buildDept(Long id, String name, Long parentId) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setParentId(parentId);
        return dept;
    }

}
