package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductBrandDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductBrandMapper;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_BRAND_EXITS_PRODUCT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_BRAND_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_BRAND_NOT_ENABLED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_BRAND_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpProductBrandServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpProductBrandServiceImpl productBrandService;

    @Mock
    private ErpProductBrandMapper erpProductBrandMapper;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpProductService productService;

    @Test
    void createProductBrand_whenValid_thenTrimsAndAppliesDefaults() {
        ErpProductBrandSaveReqVO reqVO = new ErpProductBrandSaveReqVO();
        reqVO.setName(" 博世 ");

        when(permissionApi.getCurrentUserHiddenFields("erp_product_brand")).thenReturn(Collections.emptyList());
        when(erpProductBrandMapper.selectByName("博世")).thenReturn(null);
        when(erpProductBrandMapper.insert(any(ErpProductBrandDO.class))).thenAnswer(invocation -> {
            ErpProductBrandDO brand = invocation.getArgument(0);
            brand.setId(101L);
            return 1;
        });

        Long id = productBrandService.createProductBrand(reqVO);

        assertEquals(101L, id);
        ArgumentCaptor<ErpProductBrandDO> captor = ArgumentCaptor.forClass(ErpProductBrandDO.class);
        verify(erpProductBrandMapper).insert(captor.capture());
        assertEquals("博世", captor.getValue().getName());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getSort());
    }

    @Test
    void createProductBrand_whenNameDuplicate_thenRejects() {
        ErpProductBrandSaveReqVO reqVO = new ErpProductBrandSaveReqVO();
        reqVO.setName("博世");

        when(permissionApi.getCurrentUserHiddenFields("erp_product_brand")).thenReturn(Collections.emptyList());
        when(erpProductBrandMapper.selectByName("博世")).thenReturn(ErpProductBrandDO.builder().id(101L).name("博世").build());

        assertServiceException(() -> productBrandService.createProductBrand(reqVO), PRODUCT_BRAND_NAME_DUPLICATE);

        verify(erpProductBrandMapper, never()).insert(any(ErpProductBrandDO.class));
    }

    @Test
    void validateEnabledProductBrand_whenMissingOrDisabled_thenRejects() {
        when(erpProductBrandMapper.selectByName("未知")).thenReturn(null);
        when(erpProductBrandMapper.selectByName("停用")).thenReturn(ErpProductBrandDO.builder()
                .id(102L)
                .name("停用")
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build());

        assertServiceException(() -> productBrandService.validateEnabledProductBrand("未知"), PRODUCT_BRAND_NOT_EXISTS);
        assertServiceException(() -> productBrandService.validateEnabledProductBrand("停用"), PRODUCT_BRAND_NOT_ENABLED, "停用");
    }

    @Test
    void deleteProductBrand_whenReferencedByProduct_thenRejects() {
        when(erpProductBrandMapper.selectById(101L)).thenReturn(ErpProductBrandDO.builder()
                .id(101L)
                .name("博世")
                .build());
        when(productService.getProductCountByBrand("博世")).thenReturn(1L);

        assertServiceException(() -> productBrandService.deleteProductBrand(101L), PRODUCT_BRAND_EXITS_PRODUCT);

        verify(erpProductBrandMapper, never()).deleteById(101L);
    }

    @Test
    void getProductBrand_whenFieldsHidden_thenMasksConfiguredFields() {
        when(erpProductBrandMapper.selectById(101L)).thenReturn(ErpProductBrandDO.builder()
                .id(101L)
                .name("博世")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .sort(10)
                .build());
        when(permissionApi.getCurrentUserHiddenFields("erp_product_brand")).thenReturn(Arrays.asList("name", "sort"));

        ErpProductBrandDO result = productBrandService.getProductBrand(101L);

        assertEquals(101L, result.getId());
        assertNull(result.getName());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), result.getStatus());
        assertNull(result.getSort());
    }

    @Test
    void deleteProductBrand_whenMissing_thenRejects() {
        when(erpProductBrandMapper.selectById(404L)).thenReturn(null);

        assertServiceException(() -> productBrandService.deleteProductBrand(404L), PRODUCT_BRAND_NOT_EXISTS);
    }

    @Test
    void importProductBrandList_whenNewBrand_thenCreatesAndReturnsStats() {
        ErpProductBrandImportExcelVO row = new ErpProductBrandImportExcelVO();
        row.setName(" 博世 ");

        when(erpProductBrandMapper.selectListAll()).thenReturn(Collections.emptyList());
        when(permissionApi.getCurrentUserHiddenFields("erp_product_brand")).thenReturn(Collections.emptyList());
        when(erpProductBrandMapper.selectByName("博世")).thenReturn(null);
        when(erpProductBrandMapper.insert(any(ErpProductBrandDO.class))).thenAnswer(invocation -> {
            ErpProductBrandDO brand = invocation.getArgument(0);
            brand.setId(101L);
            return 1;
        });

        ErpProductBrandImportRespVO result = productBrandService.importProductBrandList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getCreateCount());
        assertEquals(0, result.getUpdateCount());
        assertEquals(0, result.getFailureCount());
        ArgumentCaptor<ErpProductBrandDO> captor = ArgumentCaptor.forClass(ErpProductBrandDO.class);
        verify(erpProductBrandMapper).insert(captor.capture());
        assertEquals("博世", captor.getValue().getName());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getSort());
    }

    @Test
    void importProductBrandList_whenExistingAndBlankStatusSort_thenPreservesValues() {
        ErpProductBrandDO existing = ErpProductBrandDO.builder()
                .id(101L)
                .name("博世")
                .status(CommonStatusEnum.DISABLE.getStatus())
                .sort(20)
                .build();
        ErpProductBrandImportExcelVO row = new ErpProductBrandImportExcelVO();
        row.setName("博世");

        when(erpProductBrandMapper.selectListAll()).thenReturn(Collections.singletonList(existing));
        when(erpProductBrandMapper.selectById(101L)).thenReturn(existing);
        when(permissionApi.getCurrentUserHiddenFields("erp_product_brand")).thenReturn(Collections.emptyList());
        when(erpProductBrandMapper.selectByName("博世")).thenReturn(existing);

        ErpProductBrandImportRespVO result = productBrandService.importProductBrandList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getCreateCount());
        assertEquals(1, result.getUpdateCount());
        assertEquals(0, result.getFailureCount());
        ArgumentCaptor<ErpProductBrandDO> captor = ArgumentCaptor.forClass(ErpProductBrandDO.class);
        verify(erpProductBrandMapper).updateById(captor.capture());
        assertEquals(101L, captor.getValue().getId());
        assertEquals("博世", captor.getValue().getName());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(20, captor.getValue().getSort());
    }

    @Test
    void importProductBrandList_whenInvalidRow_thenReturnsFailureDetail() {
        ErpProductBrandImportExcelVO row = new ErpProductBrandImportExcelVO();
        row.setStatus(CommonStatusEnum.ENABLE.getStatus());

        when(erpProductBrandMapper.selectListAll()).thenReturn(Collections.emptyList());
        when(permissionApi.getCurrentUserHiddenFields("erp_product_brand")).thenReturn(Collections.emptyList());

        ErpProductBrandImportRespVO result = productBrandService.importProductBrandList(Collections.singletonList(row));

        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getCreateCount());
        assertEquals(0, result.getUpdateCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(2, result.getFailureDetails().get(0).getRowNo());
        assertNull(result.getFailureDetails().get(0).getName());
    }

    @Test
    void parseCsvImport_whenBrandHeaders_thenParsesRows() {
        List<ErpProductBrandImportExcelVO> rows = productBrandService.parseCsvImport(
                new StringReader("品牌名称,状态,排序\n博世,0,10\n"));

        assertEquals(1, rows.size());
        assertEquals("博世", rows.get(0).getName());
        assertEquals(0, rows.get(0).getStatus());
        assertEquals(10, rows.get(0).getSort());
    }

}
