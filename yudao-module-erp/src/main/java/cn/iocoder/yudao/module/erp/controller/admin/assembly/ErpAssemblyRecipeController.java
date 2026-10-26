package cn.iocoder.yudao.module.erp.controller.admin.assembly;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.*;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyRecipeItemMapper;
import cn.iocoder.yudao.module.erp.service.assembly.ErpAssemblyRecipeService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 模块管理")
@RestController
@RequestMapping("/erp/assembly-recipe")
@Validated
public class ErpAssemblyRecipeController {

    @Resource
    private ErpAssemblyRecipeService recipeService;
    @Resource
    private ErpAssemblyRecipeItemMapper recipeItemMapper;
    @Resource
    private ErpProductService productService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "新增组装配方")
    @PreAuthorize("@ss.hasPermission('erp:assembly-recipe:create')")
    public CommonResult<Long> createRecipe(@Valid @RequestBody ErpAssemblyRecipeSaveReqVO reqVO) {
        return success(recipeService.createRecipe(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改组装配方")
    @PreAuthorize("@ss.hasPermission('erp:assembly-recipe:update')")
    public CommonResult<Boolean> updateRecipe(@Valid @RequestBody ErpAssemblyRecipeSaveReqVO reqVO) {
        recipeService.updateRecipe(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "启用或停用组装配方")
    @PreAuthorize("@ss.hasPermission('erp:assembly-recipe:update')")
    public CommonResult<Boolean> updateRecipeStatus(@Valid @RequestBody ErpAssemblyRecipeStatusReqVO reqVO) {
        recipeService.updateRecipeStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除未被引用的组装配方")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('erp:assembly-recipe:delete')")
    public CommonResult<Boolean> deleteRecipe(@RequestParam("id") Long id) {
        recipeService.deleteRecipe(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取组装配方")
    @PreAuthorize("@ss.hasPermission('erp:assembly-recipe:query')")
    public CommonResult<ErpAssemblyRecipeRespVO> getRecipe(
            @RequestParam("id") Long id,
            @RequestParam(value = "includeItems", defaultValue = "true") Boolean includeItems) {
        return success(readRecipe(id, includeItems));
    }

    public ErpAssemblyRecipeRespVO readRecipe(Long id, Boolean includeItems) {
        ErpAssemblyRecipeDO recipe = recipeService.getRecipe(id);
        if (recipe == null) {
            return null;
        }
        List<ErpAssemblyRecipeItemDO> items = Boolean.TRUE.equals(includeItems)
                ? recipeService.getRecipeItemList(id) : Collections.emptyList();
        ErpAssemblyRecipeRespVO respVO = BeanUtils.toBean(recipe, ErpAssemblyRecipeRespVO.class);
        fillProducts(Collections.singletonList(respVO), items);
        fillUserNames(Collections.singletonList(respVO));
        respVO.setItemCount(Boolean.TRUE.equals(includeItems) ? items.size()
                : toCountMap(recipeItemMapper.selectCountRowsByRecipeIds(Collections.singleton(id)), "recipeId")
                .getOrDefault(id, 0));
        if (Boolean.TRUE.equals(includeItems)) {
            respVO.setItems(buildItemVOList(items));
        }
        return respVO;
    }

    @GetMapping("/page")
    @Operation(summary = "获取组装配方分页")
    @PreAuthorize("@ss.hasPermission('erp:assembly-recipe:query')")
    public CommonResult<PageResult<ErpAssemblyRecipeRespVO>> getRecipePage(
            @Valid ErpAssemblyRecipePageReqVO reqVO) {
        return success(readRecipePage(reqVO));
    }

    public PageResult<ErpAssemblyRecipeRespVO> readRecipePage(ErpAssemblyRecipePageReqVO reqVO) {
        PageResult<ErpAssemblyRecipeDO> page = recipeService.getRecipePage(reqVO);
        List<ErpAssemblyRecipeRespVO> list = BeanUtils.toBean(page.getList(), ErpAssemblyRecipeRespVO.class);
        List<Long> recipeIds = list.stream().map(ErpAssemblyRecipeRespVO::getId).collect(Collectors.toList());
        Map<Long, Integer> countMap = toCountMap(recipeItemMapper.selectCountRowsByRecipeIds(recipeIds), "recipeId");
        list.forEach(vo -> vo.setItemCount(countMap.getOrDefault(vo.getId(), 0)));
        fillProducts(list, Collections.emptyList());
        fillUserNames(list);
        return new PageResult<>(list, page.getTotal());
    }

    @GetMapping("/item-page")
    @Operation(summary = "获取组装配方明细分页")
    @PreAuthorize("@ss.hasPermission('erp:assembly-recipe:query')")
    public CommonResult<PageResult<ErpAssemblyRecipeRespVO.Item>> getRecipeItemPage(
            @Valid ErpAssemblyRecipeItemPageReqVO reqVO) {
        return success(readRecipeItemPage(reqVO));
    }

    public PageResult<ErpAssemblyRecipeRespVO.Item> readRecipeItemPage(ErpAssemblyRecipeItemPageReqVO reqVO) {
        PageResult<ErpAssemblyRecipeItemDO> page = recipeService.getRecipeItemPage(reqVO);
        return new PageResult<>(buildItemVOList(page.getList()), page.getTotal());
    }

    @PostMapping("/item-page")
    @Operation(summary = "获取组装配方明细分页（编辑模式）")
    @PreAuthorize("@ss.hasPermission('erp:assembly-recipe:query')")
    public CommonResult<PageResult<ErpAssemblyRecipeRespVO.Item>> getRecipeEditItemPage(
            @Valid @RequestBody ErpAssemblyRecipeItemPageReqVO reqVO) {
        return getRecipeItemPage(reqVO);
    }

    private List<ErpAssemblyRecipeRespVO.Item> buildItemVOList(List<ErpAssemblyRecipeItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        List<ErpAssemblyRecipeRespVO.Item> result = BeanUtils.toBean(items, ErpAssemblyRecipeRespVO.Item.class);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductDisplayVOMap(items.stream().map(ErpAssemblyRecipeItemDO::getProductId)
                        .collect(Collectors.toSet())));
        result.forEach(item -> fillProduct(item, productMap.get(item.getProductId())));
        return result;
    }

    private void fillProducts(List<ErpAssemblyRecipeRespVO> recipes, List<ErpAssemblyRecipeItemDO> items) {
        Set<Long> productIds = recipes.stream().map(ErpAssemblyRecipeRespVO::getProductId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        productIds.addAll(items.stream().map(ErpAssemblyRecipeItemDO::getProductId).collect(Collectors.toSet()));
        if (productIds.isEmpty()) {
            return;
        }
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductDisplayVOMap(productIds));
        recipes.forEach(recipe -> {
            ErpProductRespVO product = productMap.get(recipe.getProductId());
            if (product != null) {
                recipe.setProductCode(product.getCode()).setProductName(product.getName())
                        .setProductDrawingNo(product.getDrawingNo()).setProductFeatureCode(product.getFeatureCode())
                        .setProductOriginPlace(product.getOriginPlace()).setProductVehicleModel(product.getVehicleModel())
                        .setProductReferencePrice(product.getReferencePrice())
                        .setProductFactoryCode(product.getFactoryCode()).setProductStandard(product.getStandard())
                        .setProductUnitName(product.getUnitName()).setBatchNoEnabled(product.getBatchNoEnabled());
            }
        });
    }

    private void fillProduct(ErpAssemblyRecipeRespVO.Item item, ErpProductRespVO product) {
        if (product == null) {
            return;
        }
        item.setProductCode(product.getCode()).setProductName(product.getName())
                .setProductFactoryCode(product.getFactoryCode()).setProductStandard(product.getStandard())
                .setProductUnitName(product.getUnitName()).setProductDrawingNo(product.getDrawingNo())
                .setProductOriginPlace(product.getOriginPlace()).setProductBrand(product.getBrand())
                .setProductVehicleModel(product.getVehicleModel()).setProductFeatureCode(product.getFeatureCode())
                .setProductRetailPrice(product.getRetailPrice()).setBatchNoEnabled(product.getBatchNoEnabled());
    }

    private void fillUserNames(List<ErpAssemblyRecipeRespVO> recipes) {
        if (CollUtil.isEmpty(recipes)) {
            return;
        }
        Set<Long> userIds = new HashSet<>();
        recipes.forEach(recipe -> {
            addUserId(userIds, recipe.getCreator());
            addUserId(userIds, recipe.getUpdater());
        });
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        recipes.forEach(recipe -> {
            AdminUserRespDTO creator = userMap.get(parseUserId(recipe.getCreator()));
            if (creator != null) {
                recipe.setCreatorName(creator.getNickname());
            }
            AdminUserRespDTO updater = userMap.get(parseUserId(recipe.getUpdater()));
            if (updater != null) {
                recipe.setUpdaterName(updater.getNickname());
            }
        });
    }

    private void addUserId(Set<Long> userIds, String value) {
        Long userId = parseUserId(value);
        if (userId != null) {
            userIds.add(userId);
        }
    }

    private Long parseUserId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<Long, Integer> toCountMap(List<Map<String, Object>> rows, String key) {
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get(key);
            if (id == null) id = row.get(key.toLowerCase(Locale.ROOT));
            Object count = row.get("itemCount");
            if (count == null) count = row.get("itemcount");
            if (id != null && count != null) {
                result.put(Long.valueOf(String.valueOf(id)), Integer.valueOf(String.valueOf(count)));
            }
        }
        return result;
    }

}
