package cn.iocoder.yudao.module.erp.service.product;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryListReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategorySaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductCategoryMapper;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.*;

/**
 * ERP 产品分类 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpProductCategoryServiceImpl implements ErpProductCategoryService {

    private static final String FIELD_PERMISSION_MODULE = "erp_product_category";
    private static final Pattern CATEGORY_CODE_PATTERN = Pattern.compile("^\\d{3}( \\d{3})*$");

    @Resource
    private ErpProductCategoryMapper erpProductCategoryMapper;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpOperateLogService operateLogService;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private ErpProductService productService;

    @Override
    public Long createProductCategory(ErpProductCategorySaveReqVO createReqVO) {
        // 校验父分类编号的有效性
        prepareProductCategorySaveReqVO(null, createReqVO);
        validateParentProductCategory(null, createReqVO.getParentId());
        // 校验分类名称的唯一性
        validateProductCategoryNameUnique(null, createReqVO.getParentId(), createReqVO.getName());
        validateProductCategoryCodeUnique(null, createReqVO.getCode());

        // 插入
        ErpProductCategoryDO category = BeanUtils.toBean(createReqVO, ErpProductCategoryDO.class);
        if (category.getDeptId() == null) {
            category.setDeptId(getLoginUserDeptId());
        }
        erpProductCategoryMapper.insert(category);
        operateLogService.recordCreate(ERP_PRODUCT_CATEGORY_TYPE, category.getId(), category, category.getCode());
        // 返回
        return category.getId();
    }

    @Override
    public void updateProductCategory(ErpProductCategorySaveReqVO updateReqVO) {
        // 校验存在
        ErpProductCategoryDO existing = validateProductCategoryExists(updateReqVO.getId());
        applyProductCategorySaveFieldPermissions(updateReqVO, existing);
        ValidationUtils.validate(updateReqVO);
        // 校验父分类编号的有效性
        validateProductCategoryCodeCanUpdate(existing, updateReqVO.getCode());
        prepareProductCategorySaveReqVO(updateReqVO.getId(), updateReqVO);
        validateParentProductCategory(updateReqVO.getId(), updateReqVO.getParentId());
        // 校验分类名称的唯一性
        validateProductCategoryNameUnique(updateReqVO.getId(), updateReqVO.getParentId(), updateReqVO.getName());
        validateProductCategoryCodeUnique(updateReqVO.getId(), updateReqVO.getCode());

        // 更新
        ErpProductCategoryDO updateObj = BeanUtils.toBean(updateReqVO, ErpProductCategoryDO.class);
        updateObj.setDeptId(existing.getDeptId());
        erpProductCategoryMapper.updateById(updateObj);
        operateLogService.recordUpdate(ERP_PRODUCT_CATEGORY_TYPE, updateReqVO.getId(), existing,
                erpProductCategoryMapper.selectById(updateReqVO.getId()), updateObj.getCode());
    }

    @Override
    public void batchUpdateProductCategory(ErpProductCategoryBatchUpdateReqVO updateReqVO) {
        for (Long id : updateReqVO.getIds()) {
            ErpProductCategoryDO existing = validateProductCategoryExists(id);
            ErpProductCategoryDO updateObj = new ErpProductCategoryDO();
            updateObj.setId(id);
            updateObj.setStatus(updateReqVO.getStatus());
            erpProductCategoryMapper.updateById(updateObj);
            operateLogService.recordUpdate(ERP_PRODUCT_CATEGORY_TYPE, id, existing,
                    erpProductCategoryMapper.selectById(id), existing.getCode());
        }
    }

    @Override
    public void deleteProductCategory(Long id) {
        // 1.1 校验存在
        ErpProductCategoryDO category = validateProductCategoryExists(id);
        // 1.2 校验是否有子产品分类
        if (erpProductCategoryMapper.selectCountByParentId(id) > 0) {
            throw exception(PRODUCT_CATEGORY_EXITS_CHILDREN);
        }
        // 1.3 校验是否有产品
        if (productService.getProductCountByCategoryId(id) > 0) {
            throw exception(PRODUCT_CATEGORY_EXITS_PRODUCT);
        }
        // 2. 删除
        erpProductCategoryMapper.deleteById(id);
        operateLogService.recordDelete(ERP_PRODUCT_CATEGORY_TYPE, id, category, category.getCode());
    }

    @Override
    public void deleteProductCategoryList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            deleteProductCategory(id);
        }
    }

    private ErpProductCategoryDO validateProductCategoryExists(Long id) {
        ErpProductCategoryDO category = erpProductCategoryMapper.selectById(id);
        if (category == null) {
            throw exception(PRODUCT_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    private void validateParentProductCategory(Long id, Long parentId) {
        if (parentId == null || ErpProductCategoryDO.PARENT_ID_ROOT.equals(parentId)) {
            return;
        }
        // 1. 不能设置自己为父产品分类
        if (Objects.equals(id, parentId)) {
            throw exception(PRODUCT_CATEGORY_PARENT_ERROR);
        }
        // 2. 父产品分类不存在
        ErpProductCategoryDO parentCategory = erpProductCategoryMapper.selectById(parentId);
        if (parentCategory == null) {
            throw exception(PRODUCT_CATEGORY_PARENT_NOT_EXITS);
        }
        // 3. 递归校验父产品分类，如果父产品分类是自己的子产品分类，则报错，避免形成环路
        if (id == null) { // id 为空，说明新增，不需要考虑环路
            return;
        }
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            // 3.1 校验环路
            parentId = parentCategory.getParentId();
            if (Objects.equals(id, parentId)) {
                throw exception(PRODUCT_CATEGORY_PARENT_IS_CHILD);
            }
            // 3.2 继续递归下一级父产品分类
            if (parentId == null || ErpProductCategoryDO.PARENT_ID_ROOT.equals(parentId)) {
                break;
            }
            parentCategory = erpProductCategoryMapper.selectById(parentId);
            if (parentCategory == null) {
                break;
            }
        }
    }

    private void validateProductCategoryNameUnique(Long id, Long parentId, String name) {
        ErpProductCategoryDO productCategory = erpProductCategoryMapper.selectByParentIdAndName(parentId, name);
        if (productCategory == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的产品分类
        if (id == null) {
            throw exception(PRODUCT_CATEGORY_NAME_DUPLICATE);
        }
        if (!Objects.equals(productCategory.getId(), id)) {
            throw exception(PRODUCT_CATEGORY_NAME_DUPLICATE);
        }
    }

    private void prepareProductCategorySaveReqVO(Long id, ErpProductCategorySaveReqVO reqVO) {
        String code = normalizeCategoryCode(reqVO.getCode());
        validateProductCategoryCodeFormat(code);
        reqVO.setCode(code);
        reqVO.setParentId(resolveParentIdByCode(id, code));
    }

    private Long resolveParentIdByCode(Long id, String code) {
        String parentCode = getParentCode(code);
        if (parentCode == null) {
            return ErpProductCategoryDO.PARENT_ID_ROOT;
        }
        ErpProductCategoryDO parent = erpProductCategoryMapper.selectByCode(parentCode);
        if (parent == null) {
            throw exception(PRODUCT_CATEGORY_PARENT_CODE_NOT_EXISTS, parentCode);
        }
        if (Objects.equals(id, parent.getId())) {
            throw exception(PRODUCT_CATEGORY_PARENT_ERROR);
        }
        return parent.getId();
    }

    private void validateProductCategoryCodeUnique(Long id, String code) {
        ErpProductCategoryDO productCategory = erpProductCategoryMapper.selectByCodeExcludeId(code, id);
        if (productCategory != null) {
            throw exception(PRODUCT_CATEGORY_CODE_DUPLICATE, code);
        }
    }

    private void validateProductCategoryCodeCanUpdate(ErpProductCategoryDO existing, String code) {
        String oldCode = normalizeOptionalCategoryCode(existing.getCode());
        String newCode = normalizeOptionalCategoryCode(code);
        if (!Objects.equals(oldCode, newCode) && erpProductCategoryMapper.selectCountByParentId(existing.getId()) > 0) {
            throw exception(PRODUCT_CATEGORY_CODE_UPDATE_FAIL_HAS_CHILDREN);
        }
    }

    private String normalizeCategoryCode(String code) {
        return StrUtil.blankToDefault(code, "").trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptionalCategoryCode(String code) {
        String normalizedCode = normalizeCategoryCode(code);
        return StrUtil.emptyToNull(normalizedCode);
    }

    private void validateProductCategoryCodeFormat(String code) {
        if (!CATEGORY_CODE_PATTERN.matcher(code).matches()) {
            throw exception(PRODUCT_CATEGORY_CODE_INVALID);
        }
    }

    private String getParentCode(String code) {
        int lastSpaceIndex = code.lastIndexOf(' ');
        return lastSpaceIndex < 0 ? null : code.substring(0, lastSpaceIndex);
    }

    private int parseLastCodeSegment(String code) {
        if (!CATEGORY_CODE_PATTERN.matcher(code).matches()) {
            return 0;
        }
        int lastSpaceIndex = code.lastIndexOf(' ');
        String lastSegment = lastSpaceIndex < 0 ? code : code.substring(lastSpaceIndex + 1);
        return Integer.parseInt(lastSegment);
    }

    @Override
    public ErpProductCategoryDO getProductCategory(Long id) {
        return applyProductCategoryFieldPermissions(erpProductCategoryMapper.selectById(id));
    }

    @Override
    public Long getProductCategoryChildCount(Long parentId) {
        return erpProductCategoryMapper.selectCountByParentId(parentId);
    }

    @Override
    public List<ErpProductCategoryDO> getProductCategoryList(ErpProductCategoryListReqVO listReqVO) {
        return erpProductCategoryMapper.selectList(listReqVO);
    }

    @Override
    public List<ErpProductCategoryDO> getProductCategoryList(Collection<Long> ids) {
        return erpProductCategoryMapper.selectByIds(ids);
    }

    @Override
    public ErpProductCategoryImportRespVO importProductCategoryList(List<ErpProductCategoryImportExcelVO> list) {
        ErpProductCategoryImportRespVO result = new ErpProductCategoryImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return result;
        }
        Map<String, ErpProductCategoryDO> categoryMap = buildCategoryCodeMap();
        for (int i = 0; i < list.size(); i++) {
            ErpProductCategoryImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            try {
                importProductCategory(row, rowNo, categoryMap);
                result.setSuccessCount(result.getSuccessCount() + 1);
                if (categoryMap.containsKey(normalizeOptionalCategoryCode(row.getCode()))) {
                    result.setUpdateCount(result.getUpdateCount() + 1);
                } else {
                    result.setCreateCount(result.getCreateCount() + 1);
                }
                categoryMap = buildCategoryCodeMap();
            } catch (Exception ex) {
                result.setFailureCount(result.getFailureCount() + 1);
                result.getFailureDetails().add(new ErpProductCategoryImportRespVO.FailureItem(
                        rowNo, row == null ? null : row.getCode(), ex.getMessage()));
            }
        }
        return result;
    }

    @Override
    public String getNextProductCategoryCode(String parentCode) {
        String normalizedParentCode = normalizeOptionalCategoryCode(parentCode);
        Long parentId = ErpProductCategoryDO.PARENT_ID_ROOT;
        if (StrUtil.isNotBlank(normalizedParentCode)) {
            validateProductCategoryCodeFormat(normalizedParentCode);
            ErpProductCategoryDO parent = erpProductCategoryMapper.selectByCode(normalizedParentCode);
            if (parent == null) {
                throw exception(PRODUCT_CATEGORY_PARENT_CODE_NOT_EXISTS, normalizedParentCode);
            }
            parentId = parent.getId();
        }
        List<ErpProductCategoryDO> children = erpProductCategoryMapper.selectListByParentId(parentId);
        int nextNo = children.stream()
                .map(ErpProductCategoryDO::getCode)
                .map(this::normalizeOptionalCategoryCode)
                .filter(StrUtil::isNotBlank)
                .filter(code -> Objects.equals(getParentCode(code), normalizedParentCode))
                .mapToInt(this::parseLastCodeSegment)
                .max()
                .orElse(0) + 1;
        if (nextNo > 999) {
            throw exception(PRODUCT_CATEGORY_CODE_GENERATE_FAIL);
        }
        String nextSegment = String.format("%03d", nextNo);
        return StrUtil.isBlank(normalizedParentCode) ? nextSegment : normalizedParentCode + " " + nextSegment;
    }

    private void importProductCategory(ErpProductCategoryImportExcelVO row, int rowNo,
                                       Map<String, ErpProductCategoryDO> categoryMap) {
        if (row == null || (StrUtil.isBlank(row.getName()) && StrUtil.isBlank(row.getCode())
                && StrUtil.isBlank(row.getParentCode()) && row.getStatus() == null)) {
            throw new IllegalArgumentException("第 " + rowNo + " 行为空行");
        }
        String code = normalizeOptionalCategoryCode(row.getCode());
        String name = trimToNull(row.getName());
        if (StrUtil.isBlank(name)) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        if (StrUtil.isBlank(code)) {
            throw new IllegalArgumentException("分类编码不能为空");
        }
        validateProductCategoryCodeFormat(code);
        String parentCode = normalizeOptionalCategoryCode(row.getParentCode());
        String inferredParentCode = getParentCode(code);
        if (StrUtil.isNotBlank(parentCode) && !Objects.equals(parentCode, inferredParentCode)) {
            throw new IllegalArgumentException("上级分类编码与分类编码不一致：" + parentCode);
        }
        if (StrUtil.isNotBlank(parentCode)) {
            ErpProductCategoryDO parent = categoryMap.get(parentCode);
            if (parent == null) {
                throw new IllegalArgumentException("上级分类编码不存在：" + parentCode);
            }
            if (Objects.equals(parentCode, code)) {
                throw new IllegalArgumentException("上级分类不能是自身");
            }
        }
        Integer status = row.getStatus() == null ? 0 : row.getStatus();
        ErpProductCategoryDO existing = categoryMap.get(code);
        ErpProductCategorySaveReqVO saveReqVO = new ErpProductCategorySaveReqVO();
        saveReqVO.setId(existing == null ? null : existing.getId());
        saveReqVO.setName(name);
        saveReqVO.setCode(code);
        saveReqVO.setSort(existing == null ? rowNo * 10 : existing.getSort());
        saveReqVO.setStatus(status);
        if (existing == null) {
            createProductCategory(saveReqVO);
        } else {
            updateProductCategory(saveReqVO);
        }
    }

    private Map<String, ErpProductCategoryDO> buildCategoryCodeMap() {
        List<ErpProductCategoryDO> categories = erpProductCategoryMapper.selectList(new ErpProductCategoryListReqVO());
        Map<String, ErpProductCategoryDO> result = new HashMap<>(categories.size());
        for (ErpProductCategoryDO category : categories) {
            String code = normalizeOptionalCategoryCode(category.getCode());
            if (StrUtil.isNotBlank(code)) {
                result.put(code, category);
            }
        }
        return result;
    }

    private String trimToNull(String value) {
        return StrUtil.emptyToNull(StrUtil.trim(value));
    }

    private ErpProductCategoryDO applyProductCategoryFieldPermissions(ErpProductCategoryDO category) {
        if (category == null) {
            return null;
        }
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return category;
        }
        ErpProductCategoryDO result = BeanUtils.toBean(category, ErpProductCategoryDO.class);
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
        if (isFieldHidden(hiddenFieldSet, "parentId")) {
            result.setParentId(null);
        }
        if (isFieldHidden(hiddenFieldSet, "name")) {
            result.setName(null);
        }
        if (isFieldHidden(hiddenFieldSet, "code")) {
            result.setCode(null);
        }
        if (isFieldHidden(hiddenFieldSet, "sort")) {
            result.setSort(null);
        }
        if (isFieldHidden(hiddenFieldSet, "status")) {
            result.setStatus(null);
        }
        return result;
    }

    private void applyProductCategorySaveFieldPermissions(ErpProductCategorySaveReqVO reqVO,
                                                          ErpProductCategoryDO existing) {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
        if (isFieldHidden(hiddenFieldSet, "parentId")) {
            reqVO.setParentId(existing.getParentId());
        }
        if (isFieldHidden(hiddenFieldSet, "name")) {
            reqVO.setName(existing.getName());
        }
        if (isFieldHidden(hiddenFieldSet, "code")) {
            reqVO.setCode(existing.getCode());
        }
        if (isFieldHidden(hiddenFieldSet, "sort")) {
            reqVO.setSort(existing.getSort());
        }
        if (isFieldHidden(hiddenFieldSet, "status")) {
            reqVO.setStatus(existing.getStatus());
        }
    }

    private boolean isFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey);
    }

}
