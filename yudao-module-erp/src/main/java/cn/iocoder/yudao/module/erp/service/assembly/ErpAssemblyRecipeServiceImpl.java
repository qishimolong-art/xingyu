package cn.iocoder.yudao.module.erp.service.assembly;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipeItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipeSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyRecipeItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyRecipeMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.assembly.ErpAssemblyRecipeStatusEnum;
import cn.iocoder.yudao.module.erp.enums.assembly.ErpAssemblyRecipePurposeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_ASSEMBLY_RECIPE_TYPE;

@Service
@Validated
public class ErpAssemblyRecipeServiceImpl implements ErpAssemblyRecipeService {

    @Resource
    private ErpAssemblyRecipeMapper recipeMapper;
    @Resource
    private ErpAssemblyRecipeItemMapper recipeItemMapper;
    @Resource
    private ErpAssemblyOrderMapper orderMapper;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRecipe(ErpAssemblyRecipeSaveReqVO reqVO) {
        Integer status = reqVO.getStatus() != null ? reqVO.getStatus() : ErpAssemblyRecipeStatusEnum.ENABLED.getStatus();
        validateStatus(status);
        Integer purpose = reqVO.getPurpose() != null ? reqVO.getPurpose()
                : ErpAssemblyRecipePurposeEnum.ASSEMBLY.getPurpose();
        validatePurpose(purpose);
        Map<Long, ErpProductDO> productMap = validateRecipe(reqVO.getProductId(), reqVO.getItems(), null);
        ErpAssemblyRecipeDO existing = recipeMapper.selectByProductIdForUpdate(reqVO.getProductId());
        if (existing != null) {
            throw exception(ASSEMBLY_RECIPE_PRODUCT_EXISTS, productName(productMap.get(reqVO.getProductId())));
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.ASSEMBLY_RECIPE_NO_PREFIX);
        if (recipeMapper.selectOne(ErpAssemblyRecipeDO::getNo, no) != null) {
            throw exception(ASSEMBLY_RECIPE_NO_EXISTS);
        }
        ErpProductDO finishedProduct = productMap.get(reqVO.getProductId());
        String moduleCode = resolveModuleCode(reqVO.getModuleCode(), finishedProduct);
        String moduleName = resolveModuleName(reqVO.getModuleName(), finishedProduct);
        BigDecimal laborCostPerSet = resolveLaborCost(reqVO.getLaborCostPerSet());
        validateModuleCodeUnique(moduleCode, null);
        ErpAssemblyRecipeDO recipe = BeanUtils.toBean(reqVO, ErpAssemblyRecipeDO.class)
                .setId(null)
                .setNo(no)
                .setPurpose(purpose)
                .setModuleCode(moduleCode)
                .setModuleName(moduleName)
                .setLaborCostPerSet(laborCostPerSet)
                .setDeptId(finishedProduct.getDeptId() != null ? finishedProduct.getDeptId() : getLoginUserDeptId())
                .setVersion(1)
                .setStatus(status);
        recipeMapper.insert(recipe);
        insertItems(recipe.getId(), reqVO.getItems());
        operateLogService.recordCreate(ERP_ASSEMBLY_RECIPE_TYPE, recipe.getId(), recipe.getNo());
        return recipe.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecipe(ErpAssemblyRecipeSaveReqVO reqVO) {
        ErpAssemblyRecipeDO recipe = validateRecipeExistsForUpdate(reqVO.getId());
        Integer status = reqVO.getStatus() != null ? reqVO.getStatus() : recipe.getStatus();
        validateStatus(status);
        Integer purpose = reqVO.getPurpose() != null ? reqVO.getPurpose()
                : defaultPurpose(recipe.getPurpose());
        validatePurpose(purpose);
        Map<Long, ErpProductDO> productMap = validateRecipe(reqVO.getProductId(), reqVO.getItems(), recipe.getId());
        if (!Objects.equals(recipe.getProductId(), reqVO.getProductId())) {
            ErpAssemblyRecipeDO existing = recipeMapper.selectByProductIdForUpdate(reqVO.getProductId());
            if (existing != null && !Objects.equals(existing.getId(), recipe.getId())) {
                throw exception(ASSEMBLY_RECIPE_PRODUCT_EXISTS, productName(productMap.get(reqVO.getProductId())));
            }
        }
        ErpProductDO finishedProduct = productMap.get(reqVO.getProductId());
        String moduleCode = resolveModuleCode(reqVO.getModuleCode(), finishedProduct);
        String moduleName = resolveModuleName(reqVO.getModuleName(), finishedProduct);
        BigDecimal laborCostPerSet = resolveLaborCost(reqVO.getLaborCostPerSet());
        validateModuleCodeUnique(moduleCode, recipe.getId());
        ErpAssemblyRecipeDO updateObj = BeanUtils.toBean(reqVO, ErpAssemblyRecipeDO.class)
                .setId(recipe.getId())
                .setNo(null)
                .setPurpose(purpose)
                .setModuleCode(moduleCode)
                .setModuleName(moduleName)
                .setLaborCostPerSet(laborCostPerSet)
                .setDeptId(finishedProduct.getDeptId() != null ? finishedProduct.getDeptId() : recipe.getDeptId())
                .setVersion(recipe.getVersion() == null ? 1 : recipe.getVersion() + 1)
                .setStatus(status);
        recipeMapper.updateById(updateObj);
        recipeItemMapper.deleteByRecipeId(recipe.getId());
        insertItems(recipe.getId(), reqVO.getItems());
        operateLogService.recordUpdate(ERP_ASSEMBLY_RECIPE_TYPE, recipe.getId(), recipe.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecipeStatus(Long id, Integer status) {
        validateStatus(status);
        ErpAssemblyRecipeDO recipe = validateRecipeExistsForUpdate(id);
        if (Objects.equals(recipe.getStatus(), status)) {
            return;
        }
        if (ErpAssemblyRecipeStatusEnum.ENABLED.getStatus().equals(status)) {
            List<ErpAssemblyRecipeItemDO> items = recipeItemMapper.selectListByRecipeId(id);
            validateRecipe(recipe.getProductId(), BeanUtils.toBean(items, ErpAssemblyRecipeSaveReqVO.Item.class), id);
            ErpAssemblyRecipeDO existing = recipeMapper.selectByProductIdForUpdate(recipe.getProductId());
            if (existing != null && !Objects.equals(existing.getId(), id)) {
                throw exception(ASSEMBLY_RECIPE_PRODUCT_EXISTS, String.valueOf(recipe.getProductId()));
            }
        }
        if (recipeMapper.updateStatus(id, recipe.getStatus(), status) == 0) {
            throw exception(ASSEMBLY_RECIPE_UPDATE_CONFLICT);
        }
        operateLogService.recordUpdate(ERP_ASSEMBLY_RECIPE_TYPE, recipe.getId(), recipe.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecipe(Long id) {
        ErpAssemblyRecipeDO recipe = validateRecipeExistsForUpdate(id);
        if (orderMapper.selectCountByRecipeId(id) > 0) {
            throw exception(ASSEMBLY_RECIPE_DELETE_REFERENCED);
        }
        recipeItemMapper.deleteByRecipeId(id);
        recipeMapper.deleteById(id);
        operateLogService.recordDelete(ERP_ASSEMBLY_RECIPE_TYPE, recipe.getId(), recipe.getNo());
    }

    @Override
    public ErpAssemblyRecipeDO getRecipe(Long id) {
        return recipeMapper.selectById(id);
    }

    @Override
    public PageResult<ErpAssemblyRecipeDO> getRecipePage(ErpAssemblyRecipePageReqVO reqVO) {
        return recipeMapper.selectPage(reqVO);
    }

    @Override
    public List<ErpAssemblyRecipeItemDO> getRecipeItemList(Long recipeId) {
        return recipeItemMapper.selectListByRecipeId(recipeId);
    }

    @Override
    public List<ErpAssemblyRecipeItemDO> getRecipeItemList(Collection<Long> recipeIds) {
        return recipeItemMapper.selectListByRecipeIds(recipeIds);
    }

    @Override
    public PageResult<ErpAssemblyRecipeItemDO> getRecipeItemPage(ErpAssemblyRecipeItemPageReqVO reqVO) {
        if (recipeMapper.selectById(reqVO.getRecipeId()) == null) {
            throw exception(ASSEMBLY_RECIPE_NOT_EXISTS);
        }
        return recipeItemMapper.selectPage(reqVO);
    }

    private Map<Long, ErpProductDO> validateRecipe(Long finishedProductId,
                                                   List<ErpAssemblyRecipeSaveReqVO.Item> items,
                                                   Long currentRecipeId) {
        if (CollUtil.isEmpty(items)) {
            throw exception(ASSEMBLY_RECIPE_ITEMS_REQUIRED);
        }
        Set<Long> componentIds = new LinkedHashSet<>();
        for (ErpAssemblyRecipeSaveReqVO.Item item : items) {
            if (item.getProductId() == null || item.getQuantityPerSet() == null
                    || item.getQuantityPerSet().signum() <= 0) {
                throw exception(ASSEMBLY_RECIPE_ITEMS_REQUIRED);
            }
            if (Objects.equals(finishedProductId, item.getProductId())) {
                throw exception(ASSEMBLY_RECIPE_ITEM_SELF);
            }
            if (!componentIds.add(item.getProductId())) {
                throw exception(ASSEMBLY_RECIPE_ITEM_DUPLICATE, String.valueOf(item.getProductId()));
            }
        }
        Set<Long> productIds = new LinkedHashSet<>(componentIds);
        productIds.add(finishedProductId);
        List<ErpProductDO> products = productService.validProductList(productIds);
        Map<Long, ErpProductDO> productMap = products.stream()
                .collect(Collectors.toMap(ErpProductDO::getId, Function.identity()));
        if (productMap.size() != productIds.size()) {
            throw exception(ASSEMBLY_RECIPE_ITEMS_REQUIRED);
        }
        validateNoCycle(finishedProductId, componentIds, currentRecipeId, productMap);
        return productMap;
    }

    private void validateNoCycle(Long finishedProductId, Set<Long> componentIds, Long currentRecipeId,
                                 Map<Long, ErpProductDO> currentProductMap) {
        List<ErpAssemblyRecipeDO> recipes = recipeMapper.selectList();
        Set<Long> recipeIds = recipes.stream().map(ErpAssemblyRecipeDO::getId)
                .filter(id -> !Objects.equals(id, currentRecipeId)).collect(Collectors.toSet());
        List<ErpAssemblyRecipeItemDO> allItems = recipeItemMapper.selectListByRecipeIds(recipeIds);
        Map<Long, Set<Long>> graph = new HashMap<>();
        Map<Long, Long> recipeProductMap = recipes.stream()
                .filter(recipe -> !Objects.equals(recipe.getId(), currentRecipeId))
                .collect(Collectors.toMap(ErpAssemblyRecipeDO::getId, ErpAssemblyRecipeDO::getProductId));
        for (ErpAssemblyRecipeItemDO item : allItems) {
            Long owner = recipeProductMap.get(item.getRecipeId());
            if (owner != null) {
                graph.computeIfAbsent(owner, key -> new LinkedHashSet<>()).add(item.getProductId());
            }
        }
        graph.put(finishedProductId, componentIds);
        for (Long componentId : componentIds) {
            if (reaches(componentId, finishedProductId, graph, new HashSet<>())) {
                throw exception(ASSEMBLY_RECIPE_CYCLE,
                        productName(currentProductMap.get(finishedProductId)) + " -> "
                                + productName(currentProductMap.get(componentId)));
            }
        }
    }

    private boolean reaches(Long current, Long target, Map<Long, Set<Long>> graph, Set<Long> visited) {
        if (Objects.equals(current, target)) {
            return true;
        }
        if (!visited.add(current)) {
            return false;
        }
        for (Long next : graph.getOrDefault(current, Collections.emptySet())) {
            if (reaches(next, target, graph, visited)) {
                return true;
            }
        }
        return false;
    }

    private void insertItems(Long recipeId, List<ErpAssemblyRecipeSaveReqVO.Item> items) {
        List<ErpAssemblyRecipeItemDO> records = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            ErpAssemblyRecipeSaveReqVO.Item item = items.get(i);
            records.add(BeanUtils.toBean(item, ErpAssemblyRecipeItemDO.class)
                    .setId(null)
                    .setRecipeId(recipeId)
                    .setSort(item.getSort() != null ? item.getSort() : i + 1));
        }
        recipeItemMapper.insertBatch(records);
    }

    private ErpAssemblyRecipeDO validateRecipeExistsForUpdate(Long id) {
        if (id == null) {
            throw exception(ASSEMBLY_RECIPE_NOT_EXISTS);
        }
        ErpAssemblyRecipeDO recipe = recipeMapper.selectByIdForUpdate(id);
        if (recipe == null) {
            throw exception(ASSEMBLY_RECIPE_NOT_EXISTS);
        }
        return recipe;
    }

    private void validateStatus(Integer status) {
        if (!ErpAssemblyRecipeStatusEnum.isValid(status)) {
            throw exception(ASSEMBLY_RECIPE_STATUS_INVALID);
        }
    }

    private void validatePurpose(Integer purpose) {
        if (!ErpAssemblyRecipePurposeEnum.isValid(purpose)) {
            throw exception(ASSEMBLY_RECIPE_PURPOSE_INVALID);
        }
    }

    private void validateModuleCodeUnique(String moduleCode, Long currentRecipeId) {
        ErpAssemblyRecipeDO existing = recipeMapper.selectByModuleCodeForUpdate(moduleCode);
        if (existing != null && !Objects.equals(existing.getId(), currentRecipeId)) {
            throw exception(ASSEMBLY_RECIPE_MODULE_CODE_EXISTS, moduleCode);
        }
    }

    private String resolveModuleCode(String moduleCode, ErpProductDO finishedProduct) {
        String value = normalizeText(moduleCode);
        if (value == null && finishedProduct != null) {
            value = normalizeText(finishedProduct.getCode());
        }
        if (value == null) {
            throw exception(ASSEMBLY_RECIPE_MODULE_CODE_REQUIRED);
        }
        return value;
    }

    private String resolveModuleName(String moduleName, ErpProductDO finishedProduct) {
        String value = normalizeText(moduleName);
        if (value == null && finishedProduct != null) {
            value = normalizeText(finishedProduct.getName());
        }
        if (value == null) {
            throw exception(ASSEMBLY_RECIPE_MODULE_NAME_REQUIRED);
        }
        return value;
    }

    private BigDecimal resolveLaborCost(BigDecimal laborCostPerSet) {
        if (laborCostPerSet == null) {
            throw exception(ASSEMBLY_RECIPE_LABOR_COST_REQUIRED);
        }
        BigDecimal value = laborCostPerSet;
        if (value.signum() < 0) {
            throw exception(ASSEMBLY_RECIPE_LABOR_COST_INVALID);
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private Integer defaultPurpose(Integer purpose) {
        return purpose != null ? purpose : ErpAssemblyRecipePurposeEnum.ASSEMBLY.getPurpose();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String normalizeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String productName(ErpProductDO product) {
        return product != null && product.getName() != null ? product.getName() : "未知配件";
    }

}
