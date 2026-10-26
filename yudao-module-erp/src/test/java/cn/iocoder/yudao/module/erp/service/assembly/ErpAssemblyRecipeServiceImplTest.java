package cn.iocoder.yudao.module.erp.service.assembly;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipeSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipeItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyRecipeItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyRecipeMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_RECIPE_CYCLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_RECIPE_DELETE_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_RECIPE_ITEM_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_RECIPE_LABOR_COST_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_RECIPE_LABOR_COST_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_RECIPE_MODULE_CODE_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_RECIPE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_RECIPE_PRODUCT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpAssemblyRecipeServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpAssemblyRecipeServiceImpl recipeService;
    @Mock
    private ErpAssemblyRecipeMapper recipeMapper;
    @Mock
    private ErpAssemblyRecipeItemMapper recipeItemMapper;
    @Mock
    private ErpAssemblyOrderMapper orderMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    void createRecipe_rejectsDuplicateComponentsBeforeWriting() {
        ErpAssemblyRecipeSaveReqVO reqVO = recipe(1L, item(2L), item(2L));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recipeService.createRecipe(reqVO));

        assertEquals(ASSEMBLY_RECIPE_ITEM_DUPLICATE.getCode(), exception.getCode());
        verify(recipeMapper, never()).insert(any(ErpAssemblyRecipeDO.class));
    }

    @Test
    void createRecipe_rejectsIndirectCycle() {
        ErpAssemblyRecipeSaveReqVO reqVO = recipe(1L, item(2L));
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(1L).setName("成品A"),
                new ErpProductDO().setId(2L).setName("组件B")));
        when(recipeMapper.selectList()).thenReturn(Collections.singletonList(
                new ErpAssemblyRecipeDO().setId(20L).setProductId(2L)));
        when(recipeItemMapper.selectListByRecipeIds(Collections.singleton(20L)))
                .thenReturn(Collections.singletonList(
                        new ErpAssemblyRecipeItemDO().setRecipeId(20L).setProductId(1L)));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recipeService.createRecipe(reqVO));

        assertEquals(ASSEMBLY_RECIPE_CYCLE.getCode(), exception.getCode());
        verify(recipeMapper, never()).insert(any(ErpAssemblyRecipeDO.class));
    }

    @Test
    void createRecipe_rejectsExistingProductRecipe() {
        ErpAssemblyRecipeSaveReqVO reqVO = recipe(1L, item(2L));
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(1L).setName("成品A"),
                new ErpProductDO().setId(2L).setName("组件B")));
        when(recipeMapper.selectByProductIdForUpdate(1L))
                .thenReturn(new ErpAssemblyRecipeDO().setId(9L).setProductId(1L));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recipeService.createRecipe(reqVO));

        assertEquals(ASSEMBLY_RECIPE_PRODUCT_EXISTS.getCode(), exception.getCode());
        verify(recipeMapper, never()).insert(any(ErpAssemblyRecipeDO.class));
    }

    @Test
    void createRecipe_rejectsDuplicateModuleCode() {
        ErpAssemblyRecipeSaveReqVO reqVO = recipe(1L, item(2L));
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(1L).setCode("P001").setName("成品A"),
                new ErpProductDO().setId(2L).setName("组件B")));
        when(recipeMapper.selectByModuleCodeForUpdate("MOD-1"))
                .thenReturn(new ErpAssemblyRecipeDO().setId(9L).setModuleCode("MOD-1"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recipeService.createRecipe(reqVO));

        assertEquals(ASSEMBLY_RECIPE_MODULE_CODE_EXISTS.getCode(), exception.getCode());
        verify(recipeMapper, never()).insert(any(ErpAssemblyRecipeDO.class));
    }

    @Test
    void createRecipe_defaultsPurposeAndModuleFieldsForLegacyCaller() {
        ErpAssemblyRecipeSaveReqVO reqVO = recipe(1L, item(2L));
        reqVO.setPurpose(null);
        reqVO.setModuleCode(null);
        reqVO.setModuleName(null);
        reqVO.setLaborCostPerSet(BigDecimal.ZERO);
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(1L).setCode("P001").setName("成品A").setDeptId(8L),
                new ErpProductDO().setId(2L).setName("组件B")));
        when(noRedisDAO.generate(any())).thenReturn("PF0001");

        recipeService.createRecipe(reqVO);

        ArgumentCaptor<ErpAssemblyRecipeDO> captor = ArgumentCaptor.forClass(ErpAssemblyRecipeDO.class);
        verify(recipeMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getPurpose());
        assertEquals("P001", captor.getValue().getModuleCode());
        assertEquals("成品A", captor.getValue().getModuleName());
        assertEquals(new BigDecimal("0.000000"), captor.getValue().getLaborCostPerSet());
    }

    @Test
    void createRecipe_rejectsMissingLaborCost() {
        ErpAssemblyRecipeSaveReqVO reqVO = recipe(1L, item(2L));
        reqVO.setLaborCostPerSet(null);
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(1L).setCode("P001").setName("成品A"),
                new ErpProductDO().setId(2L).setName("组件B")));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recipeService.createRecipe(reqVO));

        assertEquals(ASSEMBLY_RECIPE_LABOR_COST_REQUIRED.getCode(), exception.getCode());
        verify(recipeMapper, never()).insert(any(ErpAssemblyRecipeDO.class));
    }

    @Test
    void createRecipe_rejectsNegativeLaborCost() {
        ErpAssemblyRecipeSaveReqVO reqVO = recipe(1L, item(2L));
        reqVO.setLaborCostPerSet(new BigDecimal("-0.01"));
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(1L).setCode("P001").setName("成品A"),
                new ErpProductDO().setId(2L).setName("组件B")));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recipeService.createRecipe(reqVO));

        assertEquals(ASSEMBLY_RECIPE_LABOR_COST_INVALID.getCode(), exception.getCode());
        verify(recipeMapper, never()).insert(any(ErpAssemblyRecipeDO.class));
    }

    @Test
    void updateRecipe_incrementsVersionAndReplacesItems() {
        ErpAssemblyRecipeSaveReqVO reqVO = recipe(1L, item(2L));
        reqVO.setId(7L);
        when(recipeMapper.selectByIdForUpdate(7L)).thenReturn(new ErpAssemblyRecipeDO()
                .setId(7L).setNo("PF0007").setProductId(1L).setVersion(3).setStatus(1).setDeptId(8L));
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(1L).setCode("P001").setName("成品A").setDeptId(8L),
                new ErpProductDO().setId(2L).setName("组件B")));

        recipeService.updateRecipe(reqVO);

        ArgumentCaptor<ErpAssemblyRecipeDO> captor = ArgumentCaptor.forClass(ErpAssemblyRecipeDO.class);
        verify(recipeMapper).updateById(captor.capture());
        assertEquals(4, captor.getValue().getVersion());
        verify(recipeItemMapper).deleteByRecipeId(7L);
        verify(recipeItemMapper).insertBatch(any());
    }

    @Test
    void deleteRecipe_rejectsReferencedRecipe() {
        when(recipeMapper.selectByIdForUpdate(7L)).thenReturn(new ErpAssemblyRecipeDO()
                .setId(7L).setNo("PF0007").setProductId(1L));
        when(orderMapper.selectCountByRecipeId(7L)).thenReturn(1L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recipeService.deleteRecipe(7L));

        assertEquals(ASSEMBLY_RECIPE_DELETE_REFERENCED.getCode(), exception.getCode());
        verify(recipeItemMapper, never()).deleteByRecipeId(any());
        verify(recipeMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void getRecipeItemPage_rejectsMissingOrInaccessibleRecipe() {
        ErpAssemblyRecipeItemPageReqVO reqVO = new ErpAssemblyRecipeItemPageReqVO();
        reqVO.setRecipeId(99L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> recipeService.getRecipeItemPage(reqVO));

        assertEquals(ASSEMBLY_RECIPE_NOT_EXISTS.getCode(), exception.getCode());
        verify(recipeItemMapper, never()).selectPage(any());
    }

    private static ErpAssemblyRecipeSaveReqVO recipe(Long productId,
                                                      ErpAssemblyRecipeSaveReqVO.Item... items) {
        ErpAssemblyRecipeSaveReqVO reqVO = new ErpAssemblyRecipeSaveReqVO();
        reqVO.setProductId(productId);
        reqVO.setPurpose(1);
        reqVO.setModuleCode("MOD-1");
        reqVO.setModuleName("模块一");
        reqVO.setLaborCostPerSet(new BigDecimal("8"));
        reqVO.setStatus(1);
        reqVO.setItems(Arrays.asList(items));
        return reqVO;
    }

    private static ErpAssemblyRecipeSaveReqVO.Item item(Long productId) {
        ErpAssemblyRecipeSaveReqVO.Item item = new ErpAssemblyRecipeSaveReqVO.Item();
        item.setProductId(productId);
        item.setQuantityPerSet(BigDecimal.ONE);
        return item;
    }
}
