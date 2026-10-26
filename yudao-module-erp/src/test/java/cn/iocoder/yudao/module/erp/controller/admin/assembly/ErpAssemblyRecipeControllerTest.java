package cn.iocoder.yudao.module.erp.controller.admin.assembly;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipeRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeDO;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyRecipeItemMapper;
import cn.iocoder.yudao.module.erp.service.assembly.ErpAssemblyRecipeService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

class ErpAssemblyRecipeControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpAssemblyRecipeController controller;

    @Mock
    private ErpAssemblyRecipeService recipeService;
    @Mock
    private ErpAssemblyRecipeItemMapper recipeItemMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private AdminUserApi adminUserApi;

    @Test
    void testGetRecipe_fillsAuditUserNames() {
        LocalDateTime createTime = LocalDateTime.of(2026, 10, 22, 9, 30);
        LocalDateTime updateTime = LocalDateTime.of(2026, 10, 22, 10, 45);
        ErpAssemblyRecipeDO recipe = new ErpAssemblyRecipeDO();
        recipe.setId(1L);
        recipe.setProductId(99L);
        recipe.setCreator("11");
        recipe.setUpdater("12");
        recipe.setCreateTime(createTime);
        recipe.setUpdateTime(updateTime);
        when(recipeService.getRecipe(1L)).thenReturn(recipe);
        when(recipeService.getRecipeItemList(1L)).thenReturn(Collections.emptyList());

        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(99L);
        product.setCode("CP-001");
        product.setName("测试组成件");
        product.setDrawingNo("T-001");
        product.setStandard("标准规格");
        product.setFeatureCode("TZ-001");
        product.setOriginPlace("成都");
        product.setVehicleModel("通用车型");
        product.setReferencePrice(new BigDecimal("88.50"));
        when(productService.getProductDisplayVOMap(anySet()))
                .thenReturn(Collections.singletonMap(99L, product));

        Map<Long, AdminUserRespDTO> users = new HashMap<>();
        AdminUserRespDTO creator = new AdminUserRespDTO();
        creator.setId(11L);
        creator.setNickname("创建用户");
        users.put(11L, creator);
        AdminUserRespDTO updater = new AdminUserRespDTO();
        updater.setId(12L);
        updater.setNickname("修改用户");
        users.put(12L, updater);
        when(adminUserApi.getUserMap(anySet())).thenReturn(users);

        CommonResult<ErpAssemblyRecipeRespVO> result = controller.getRecipe(1L, true);

        assertEquals("创建用户", result.getData().getCreatorName());
        assertEquals("修改用户", result.getData().getUpdaterName());
        assertEquals(createTime, result.getData().getCreateTime());
        assertEquals(updateTime, result.getData().getUpdateTime());
        assertEquals("测试组成件", result.getData().getProductName());
        assertEquals("T-001", result.getData().getProductDrawingNo());
        assertEquals("标准规格", result.getData().getProductStandard());
        assertEquals("TZ-001", result.getData().getProductFeatureCode());
        assertEquals("成都", result.getData().getProductOriginPlace());
        assertEquals("通用车型", result.getData().getProductVehicleModel());
        assertEquals(new BigDecimal("88.50"), result.getData().getProductReferencePrice());
    }
}
