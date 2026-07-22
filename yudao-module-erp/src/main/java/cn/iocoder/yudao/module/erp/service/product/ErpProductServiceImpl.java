package cn.iocoder.yudao.module.erp.service.product;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryListReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductStockDistributionBatchSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductStockDistributionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductStockDistributionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ProductBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ProductSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDeptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUniversalDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductDeptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUniversalMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockLockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.base.ErpArchiveMergeService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.ConstraintViolationException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FIELD_CONFIG_FIELD_NAME_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ARCHIVE_MERGE_SAME_ID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_CODE_GENERATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_CATEGORY_NOT_LEAF;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_DELETE_FAIL_STOCK_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_MERGED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_FIELD_NO_PERMISSION;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_READONLY_BY_SALE_DISTRIBUTION;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_STOCK_DISTRIBUTION_PRODUCT_DISABLED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_STOCK_DISTRIBUTION_REMOVE_DENIED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_STOCK_DISTRIBUTION_WAREHOUSE_DENIED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_STOCK_DISTRIBUTION_WAREHOUSE_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_UNIT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_UNIVERSAL_CODE_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_UNIVERSAL_CODE_SELF;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_WAREHOUSE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_WAREHOUSE_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_CREATE_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_DELETE_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_IMPORT_SUB_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PRODUCT_TYPE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_UPDATE_SUB_TYPE;

/**
 * ERP 产品 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ErpProductServiceImpl implements ErpProductService {

    public static final String PRODUCT_CODE_MODE_CONFIG_KEY = "erp.product.code-mode";
    private static final String FIELD_PERMISSION_MODULE = "erp_product";
    private static final String PRODUCT_CODE_MODE_AUTO = "AUTO";
    private static final String PRODUCT_CODE_MODE_MANUAL = "MANUAL";

    static boolean isLowStockWarning(BigDecimal currentStock, Integer stockMin) {
        BigDecimal current = currentStock == null ? BigDecimal.ZERO : currentStock;
        return current.compareTo(BigDecimal.ZERO) <= 0
                || (stockMin != null && current.compareTo(BigDecimal.valueOf(stockMin)) <= 0);
    }

    /**
     * 生成配件编码的最大重试次数，用于并发下 uk_code 冲突兜底
     */
    private static final int CODE_GENERATE_MAX_RETRY = 5;

    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpProductDeptMapper productDeptMapper;
    @Resource
    private ErpProductUniversalMapper productUniversalMapper;
    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpStockLockMapper stockLockMapper;
    @Resource
    private ErpStockRecordMapper stockRecordMapper;
    @Resource
    private ErpProductCategoryService productCategoryService;
    @Resource
    private ErpProductUnitService productUnitService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpProductPriceSystemService productPriceSystemService;
    @Resource
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Resource
    private ErpArchiveMergeService archiveMergeService;
    @Resource
    private ErpFieldConfigService fieldConfigService;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ConfigApi configApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(ProductSaveReqVO createReqVO) {
        // 1. 默认仓库必填，并校验仓库存在
        applyProductSaveFieldPermissions(createReqVO, null);
        ignoreReadonlyProductSaveFields(createReqVO);
        ValidationUtils.validate(createReqVO);
        validateProductCategoryLeaf(createReqVO.getCategoryId());






















































































































































































































































































































































































































































































































































































































































































































































































































































































































        validateDefaultWarehouseRequiredAndExists(createReqVO.getDefaultWarehouseId());

        // 2. 生成配件编码（带重试，防并发）并插入主表
        ErpProductDO product = BeanUtils.toBean(createReqVO, ErpProductDO.class);
        if (product.getMergedFlag() == null) {
            product.setMergedFlag(false);
        }
        if (product.getBatchNoEnabled() == null) {
            product.setBatchNoEnabled(false);
        }
        if (product.getPackageQty() == null) {
            product.setPackageQty(1);
        }
        Long loginUserDeptId = getLoginUserDeptId();
        product.setCreateDeptId(loginUserDeptId);
        List<Long> productDeptIds = buildProductDeptIds(product.getDeptId(), createReqVO.getDeptIds(), loginUserDeptId);
        product.setDeptId(productDeptIds.isEmpty() ? null : productDeptIds.get(0));
        if (product.getCreateDeptId() == null && !productDeptIds.isEmpty()) {
            product.setCreateDeptId(productDeptIds.get(0));
        }
        prepareProductCode(product, createReqVO.getCode(), null);
        insertProduct(product);
        saveProductCustomFields(product.getId(), createReqVO, getHiddenFieldSet(), true);
        initProductStock(product.getId(), createReqVO.getDefaultWarehouseId());
        syncProductOpenDeptByStockDistribution(product.getId());

        // 3. 校验并插入通用件子表
        validateUniversalCodes(product.getId(), createReqVO.getUniversals());
        saveUniversals(product.getId(), createReqVO.getUniversals());

        ProductLogSnapshot after = loadProductLogSnapshot(product.getId());
        recordProductLog(ERP_CREATE_SUB_TYPE, product.getId(), buildProductLogAction("新增配件信息", after));
        return product.getId();
    }

    private void insertWithGeneratedCode(ErpProductDO product) {
        for (int i = 0; i < CODE_GENERATE_MAX_RETRY; i++) {
            String code = generateProductCode(i);
            product.setCode(code);
            try {
                productMapper.insert(product);
                return;
            } catch (DuplicateKeyException ex) {
                // 唯一键冲突则重试下一个编码
                product.setId(null);
            }
        }
        throw exception(PRODUCT_CODE_GENERATE_FAIL);
    }

    private String generateProductCode(int retryIndex) {
        try {
            String code = noRedisDAO.generatePlain(ErpNoRedisDAO.PRODUCT_CODE_PREFIX);
            if (StringUtils.hasText(code)) {
                return code;
            }
        } catch (RuntimeException ignored) {
            // Redis 不可用时回退到数据库最大编码续号，避免新增配件直接失败。
        }
        long nextNumber = getNextProductCodeNumber() + retryIndex;
        return ErpNoRedisDAO.PRODUCT_CODE_PREFIX + String.format("%06d", nextNumber);
    }

    private long getNextProductCodeNumber() {
        String prefix = ErpNoRedisDAO.PRODUCT_CODE_PREFIX;
        return productMapper.selectCodesByPrefix(prefix).stream()
                .map(code -> parseProductCodeNumber(code, prefix))
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private Long parseProductCodeNumber(String code, String prefix) {
        if (!StringUtils.hasText(code) || !code.startsWith(prefix)) {
            return null;
        }
        String numberPart = code.substring(prefix.length());
        if (!numberPart.matches("\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(numberPart);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductSaveReqVO updateReqVO) {
        // 1. 校验存在 & 未被合并
        ErpProductDO existing = validateManageableProductExists(updateReqVO.getId());
        ProductLogSnapshot before = loadProductLogSnapshot(updateReqVO.getId());
        if (Boolean.TRUE.equals(existing.getMergedFlag())) {
            throw exception(PRODUCT_MERGED, existing.getName());
        }
        applyProductSaveFieldPermissions(updateReqVO, existing);
        ignoreReadonlyProductSaveFields(updateReqVO);
        ValidationUtils.validate(updateReqVO);
        validateProductCategoryLeaf(updateReqVO.getCategoryId());
        Long targetDefaultWarehouseId = updateReqVO.getDefaultWarehouseId() != null
                ? updateReqVO.getDefaultWarehouseId() : existing.getDefaultWarehouseId();
        validateDefaultWarehouseExists(targetDefaultWarehouseId);

        // 2. 更新主表（code/mergedFlag/mergedTargetId 不允许通过此接口修改，保留原值）
        ErpProductDO updateObj = BeanUtils.toBean(updateReqVO, ErpProductDO.class);
        prepareProductCode(updateObj, updateReqVO.getCode(), existing.getId());
        updateObj.setDeptId(existing.getDeptId());
        updateObj.setCreateDeptId(existing.getCreateDeptId());
        updateObj.setDefaultWarehouseId(targetDefaultWarehouseId);
        if (updateObj.getBatchNoEnabled() == null) {
            updateObj.setBatchNoEnabled(Boolean.TRUE.equals(existing.getBatchNoEnabled()));
        }
        updateObj.setMergedFlag(null);
        updateObj.setMergedTargetId(null);
        try {
            productMapper.updateById(updateObj);
        } catch (DuplicateKeyException ex) {
            throw exception(PRODUCT_CODE_DUPLICATE, updateReqVO.getCode());
        }
        saveProductCustomFields(updateReqVO.getId(), updateReqVO, getHiddenFieldSet(), false);
        initProductStock(updateReqVO.getId(), updateObj.getDefaultWarehouseId());
        syncProductOpenDeptByStockDistribution(updateReqVO.getId());

        // 4. 子表：先删后插
        validateUniversalCodes(updateReqVO.getId(), updateReqVO.getUniversals());
        productUniversalMapper.deleteByProductId(updateReqVO.getId());
        saveUniversals(updateReqVO.getId(), updateReqVO.getUniversals());
        ProductLogSnapshot after = loadProductLogSnapshot(updateReqVO.getId());
        recordProductLog(ERP_UPDATE_SUB_TYPE, updateReqVO.getId(), buildProductLogAction("修改配件信息", before, after));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateProduct(ProductBatchUpdateReqVO updateReqVO) {
        if (CollUtil.isEmpty(updateReqVO.getIds())) {
            return;
        }
        validateManageableProductIds(updateReqVO.getIds());
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        Set<String> hiddenFieldSet = CollUtil.isEmpty(hiddenFields) ? Collections.emptySet() : new HashSet<>(hiddenFields);

        Long categoryId = isFieldHidden(hiddenFieldSet, "categoryId") ? null : updateReqVO.getCategoryId();
        Long unitId = isFieldHidden(hiddenFieldSet, "unitId") ? null : updateReqVO.getUnitId();
        Long defaultWarehouseId = isFieldHidden(hiddenFieldSet, "defaultWarehouseId") ? null : updateReqVO.getDefaultWarehouseId();
        Integer status = isFieldHidden(hiddenFieldSet, "status") ? null : updateReqVO.getStatus();
        String remark = isFieldHidden(hiddenFieldSet, "remark") ? null : trimToNull(updateReqVO.getRemark());

        if (categoryId == null && unitId == null && defaultWarehouseId == null && status == null && remark == null) {
            return;
        }

        validateProductCategoryLeaf(categoryId);
        if (unitId != null && productUnitService.getProductUnit(unitId) == null) {
            throw exception(PRODUCT_UNIT_NOT_EXISTS);
        }
        validateDefaultWarehouseExists(defaultWarehouseId);

        List<ErpProductDO> products = productMapper.selectByIds(updateReqVO.getIds());
        Map<Long, ErpProductDO> productMap = convertMap(products, ErpProductDO::getId);
        for (Long id : updateReqVO.getIds()) {
            ErpProductDO existing = productMap.get(id);
            if (existing == null) {
                throw exception(PRODUCT_NOT_EXISTS);
            }
            if (Boolean.TRUE.equals(existing.getMergedFlag())) {
                throw exception(PRODUCT_MERGED, existing.getName());
            }
            ProductLogSnapshot before = loadProductLogSnapshot(id);
            ErpProductDO updateObj = new ErpProductDO();
            updateObj.setId(id);
            updateObj.setCategoryId(categoryId);
            updateObj.setUnitId(unitId);
            updateObj.setDefaultWarehouseId(defaultWarehouseId);
            updateObj.setStatus(status);
            if (CommonStatusEnum.isDisable(status)) {
                updateObj.setDisabledBy(getLoginUserId());
                updateObj.setDisabledTime(LocalDateTime.now());
            }
            updateObj.setRemark(remark);
            productMapper.updateById(updateObj);
            initProductStock(id, defaultWarehouseId);
            ProductLogSnapshot after = loadProductLogSnapshot(id);
            recordProductLog(ERP_UPDATE_SUB_TYPE, id, buildProductLogAction("批量修改配件信息", before, after));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDisableProduct(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        for (Long id : distinctIds) {
            ErpProductDO product = validateManageableProductExists(id);
            if (Boolean.TRUE.equals(product.getMergedFlag())) {
                throw exception(PRODUCT_MERGED, product.getName());
            }
            ProductLogSnapshot before = loadProductLogSnapshot(id);
            ErpProductDO updateObj = new ErpProductDO();
            updateObj.setId(id);
            updateObj.setStatus(CommonStatusEnum.DISABLE.getStatus());
            updateObj.setDisabledBy(getLoginUserId());
            updateObj.setDisabledTime(LocalDateTime.now());
            productMapper.updateById(updateObj);
            ProductLogSnapshot after = loadProductLogSnapshot(id);
            recordProductLog(ERP_UPDATE_SUB_TYPE, id, buildProductLogAction("停用配件信息", before, after));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreProduct(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        for (Long id : distinctIds) {
            ErpProductDO product = validateManageableProductExists(id);
            if (Boolean.TRUE.equals(product.getMergedFlag())) {
                throw exception(PRODUCT_MERGED, product.getName());
            }
            if (!CommonStatusEnum.isDisable(product.getStatus())) {
                throw exception(PRODUCT_NOT_ENABLE, product.getName());
            }
            ProductLogSnapshot before = loadProductLogSnapshot(id);
            productMapper.update(null, new LambdaUpdateWrapper<ErpProductDO>()
                    .eq(ErpProductDO::getId, id)
                    .set(ErpProductDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                    .set(ErpProductDO::getDisabledBy, null)
                    .set(ErpProductDO::getDisabledTime, null));
            ProductLogSnapshot after = loadProductLogSnapshot(id);
            recordProductLog(ERP_UPDATE_SUB_TYPE, id, buildProductLogAction("还原配件信息", before, after));
        }
    }

    private void validateDefaultWarehouseRequiredAndExists(Long defaultWarehouseId) {
        if (defaultWarehouseId == null) {
            throw exception(PRODUCT_WAREHOUSE_REQUIRED);
        }
        validateDefaultWarehouseExists(defaultWarehouseId);
    }

    private void validateProductCategoryLeaf(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        if (productCategoryService.getProductCategory(categoryId) == null) {
            throw exception(PRODUCT_CATEGORY_NOT_EXISTS);
        }
        Long childCount = productCategoryService.getProductCategoryChildCount(categoryId);
        if (childCount != null && childCount > 0) {
            throw exception(PRODUCT_CATEGORY_NOT_LEAF);
        }
    }

    private void validateDefaultWarehouseExists(Long defaultWarehouseId) {
        if (defaultWarehouseId == null) {
            return;
        }
        if (warehouseService.getWarehouse(defaultWarehouseId) == null) {
            throw exception(PRODUCT_WAREHOUSE_NOT_EXISTS);
        }
        warehouseService.validateCurrentUserWarehousePermission(Collections.singleton(defaultWarehouseId));
    }

    private void prepareProductCode(ErpProductDO product, String inputCode, Long excludeId) {
        String code = trimToNull(inputCode);
        if (PRODUCT_CODE_MODE_MANUAL.equals(getProductCodeMode())) {
            if (!StringUtils.hasText(code)) {
                throw exception(PRODUCT_CODE_GENERATE_FAIL);
            }
            validateProductCodeUnique(code, excludeId);
            product.setCode(code);
            return;
        }
        if (StringUtils.hasText(code)) {
            validateProductCodeUnique(code, excludeId);
            product.setCode(code);
            return;
        }
        product.setCode(null);
    }

    private String getProductCodeMode() {
        String mode = configApi.getConfigValueByKey(PRODUCT_CODE_MODE_CONFIG_KEY);
        if (PRODUCT_CODE_MODE_MANUAL.equalsIgnoreCase(mode)) {
            return PRODUCT_CODE_MODE_MANUAL;
        }
        return PRODUCT_CODE_MODE_AUTO;
    }

    private void validateProductCodeUnique(String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        ErpProductDO existing = productMapper.selectByCodeExcludeId(code, excludeId);
        if (existing != null) {
            throw exception(PRODUCT_CODE_DUPLICATE, code);
        }
    }

    private void insertProduct(ErpProductDO product) {
        if (!StringUtils.hasText(product.getCode())) {
            insertWithGeneratedCode(product);
            return;
        }
        try {
            productMapper.insert(product);
        } catch (DuplicateKeyException ex) {
            throw exception(PRODUCT_CODE_DUPLICATE, product.getCode());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        ErpProductDO product = validateManageableProductExists(id);
        ProductLogSnapshot before = loadProductLogSnapshot(id);
        validateProductCanDelete(product);
        stockMapper.delete(ErpStockDO::getProductId, id);
        productDeptMapper.deleteByProductId(id);
        productUniversalMapper.deleteByProductId(id);
        productPriceSystemService.deleteByProductId(id);
        productMapper.deleteById(id);
        recordProductLog(ERP_DELETE_SUB_TYPE, id, buildProductLogAction("删除配件信息", before));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeProduct(Long sourceId, Long keepId) {
        if (Objects.equals(sourceId, keepId)) {
            throw exception(ARCHIVE_MERGE_SAME_ID);
        }
        ErpProductDO source = validateManageableProductExists(sourceId);
        ErpProductDO keep = validateManageableProductExists(keepId);
        if (Boolean.TRUE.equals(source.getMergedFlag())) {
            throw exception(PRODUCT_MERGED, source.getName());
        }
        if (Boolean.TRUE.equals(keep.getMergedFlag())) {
            throw exception(PRODUCT_MERGED, keep.getName());
        }
        ProductLogSnapshot before = loadProductLogSnapshot(sourceId);
        ProductLogSnapshot keepSnapshot = loadProductLogSnapshot(keepId);
        archiveMergeService.validateProductStockMergeConflict(sourceId, keepId);
        String operatorId = String.valueOf(getLoginUserId());
        archiveMergeService.mergeProductReferences(sourceId, keepId, source.getCode(), keep.getCode(), operatorId);
        productMapper.update(null, new LambdaUpdateWrapper<ErpProductDO>()
                .eq(ErpProductDO::getId, sourceId)
                .ne(ErpProductDO::getMergedFlag, Boolean.TRUE)
                .set(ErpProductDO::getMergedFlag, true)
                .set(ErpProductDO::getMergedTargetId, keepId)
                .set(ErpProductDO::getMergedBy, getLoginUserId())
                .set(ErpProductDO::getMergedTime, LocalDateTime.now()));
        ProductLogSnapshot after = loadProductLogSnapshot(sourceId);
        recordProductLog(ERP_UPDATE_SUB_TYPE, sourceId,
                limitLogAction(buildProductLogAction("合并配件信息", before, after)
                        + "；合并到：" + buildProductIdentityText(keepSnapshot)));
    }

    @Override
    public List<ErpProductDO> validProductList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ErpProductDO> list = productMapper.selectByIds(ids);
        Map<Long, ErpProductDO> productMap = convertMap(list, ErpProductDO::getId);
        for (Long id : ids) {
            ErpProductDO product = productMap.get(id);
            if (product == null) {
                throw exception(PRODUCT_NOT_EXISTS);
            }
            if (Boolean.TRUE.equals(product.getMergedFlag())) {
                throw exception(PRODUCT_MERGED, product.getName());
            }
            if (CommonStatusEnum.isDisable(product.getStatus())) {
                throw exception(PRODUCT_NOT_ENABLE, product.getName());
            }
        }
        return list;
    }

    private ErpProductDO validateProductExists(Long id) {
        ErpProductDO product = productMapper.selectById(id);
        if (product == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        return product;
    }

    private void initProductStock(Long productId, Long warehouseId) {
        if (productId == null || warehouseId == null) {
            return;
        }
        if (stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId) != null) {
            return;
        }
        stockMapper.insert(new ErpStockDO()
                .setProductId(productId)
                .setWarehouseId(warehouseId)
                .setDeptId(resolveWarehouseDeptId(warehouseId))
                .setCount(BigDecimal.ZERO)
                .setLockCount(BigDecimal.ZERO)
                .setCostPrice(BigDecimal.ZERO)
                .setCostAmount(BigDecimal.ZERO));
    }

    private Long resolveWarehouseDeptId(Long warehouseId) {
        ErpWarehouseDO warehouse = warehouseService.getWarehouse(warehouseId);
        return warehouse != null ? warehouse.getDeptId() : null;
    }

    private ErpProductStockDistributionRespVO.WarehouseItem buildStockDistributionWarehouseItem(
            ErpWarehouseDO warehouse, ErpStockDO stock, Long defaultWarehouseId, Map<Long, DeptRespDTO> deptMap) {
        ErpProductStockDistributionRespVO.WarehouseItem item =
                new ErpProductStockDistributionRespVO.WarehouseItem();
        item.setWarehouseId(warehouse.getId());
        item.setWarehouseCode(warehouse.getWarehouseCode());
        item.setWarehouseName(warehouse.getName());
        item.setDeptId(warehouse.getDeptId());
        DeptRespDTO dept = warehouse.getDeptId() == null ? null : deptMap.get(warehouse.getDeptId());
        item.setDeptName(dept == null ? null : dept.getName());
        item.setDistributed(stock != null);
        item.setDefaultWarehouse(Objects.equals(warehouse.getId(), defaultWarehouseId));
        item.setStockId(stock == null ? null : stock.getId());
        item.setCount(stock == null ? BigDecimal.ZERO : zeroIfNull(stock.getCount()));
        item.setLockCount(stock == null ? BigDecimal.ZERO : zeroIfNull(stock.getLockCount()));
        if (stock == null) {
            item.setRemovable(true);
            return item;
        }
        if (Boolean.TRUE.equals(item.getDefaultWarehouse())) {
            item.setRemovable(false);
            item.setDisabledReason("默认仓库不能取消分发");
            return item;
        }
        boolean removable = canRemoveStockDistribution(stock);
        item.setRemovable(removable);
        item.setDisabledReason(removable ? null : buildStockDistributionRemoveDeniedReason(stock));
        return item;
    }

    private boolean canRemoveStockDistribution(ErpStockDO stock) {
        if (stock == null) {
            return true;
        }
        if (hasStockTrace(stock)) {
            return false;
        }
        if (stockRecordMapper.selectCountByProductIdAndWarehouseId(
                stock.getProductId(), stock.getWarehouseId()) > 0) {
            return false;
        }
        return stockLockMapper.selectActiveCountByProductIdAndWarehouseId(
                stock.getProductId(), stock.getWarehouseId()) <= 0;
    }

    private String buildStockDistributionRemoveDeniedReason(ErpStockDO stock) {
        if (hasStockTrace(stock)) {
            return "已有库存数量、占用、在途或成本数据";
        }
        if (stockRecordMapper.selectCountByProductIdAndWarehouseId(
                stock.getProductId(), stock.getWarehouseId()) > 0) {
            return "已有库存流水";
        }
        if (stockLockMapper.selectActiveCountByProductIdAndWarehouseId(
                stock.getProductId(), stock.getWarehouseId()) > 0) {
            return "已有库存占用";
        }
        return null;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String getWarehouseDisplayName(Long warehouseId) {
        ErpWarehouseDO warehouse = warehouseService.getWarehouse(warehouseId);
        return warehouse == null ? String.valueOf(warehouseId) : warehouse.getName();
    }

    private String formatWarehouseNames(Collection<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return "无";
        }
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);
        return warehouseIds.stream()
                .map(id -> {
                    ErpWarehouseDO warehouse = warehouseMap.get(id);
                    return warehouse == null ? String.valueOf(id) : formatNameAndId(warehouse.getName(), id);
                })
                .collect(Collectors.joining("、"));
    }

    private void validateProductCanDelete(ErpProductDO product) {
        Long productId = product.getId();
        baseArchiveReferenceService.validateProductNotReferenced(productId);
        if (stockRecordMapper.selectCount(ErpStockRecordDO::getProductId, productId) > 0) {
            throw exception(PRODUCT_DELETE_FAIL_STOCK_EXISTS, product.getName());
        }
        if (stockLockMapper.selectCount(new LambdaQueryWrapper<ErpStockLockDO>()
                .eq(ErpStockLockDO::getProductId, productId)
                .eq(ErpStockLockDO::getStatus, 1)) > 0) {
            throw exception(PRODUCT_DELETE_FAIL_STOCK_EXISTS, product.getName());
        }
        List<ErpStockDO> stocks = stockMapper.selectList(ErpStockDO::getProductId, productId);
        boolean hasStockTrace = stocks.stream().anyMatch(this::hasStockTrace);
        if (hasStockTrace) {
            throw exception(PRODUCT_DELETE_FAIL_STOCK_EXISTS, product.getName());
        }
    }

    private boolean hasStockTrace(ErpStockDO stock) {
        return isNonZero(stock.getCount())
                || isNonZero(stock.getLockCount())
                || isNonZero(stock.getOccupiedCount())
                || isNonZero(stock.getPendingInCount())
                || isNonZero(stock.getInTransitCount())
                || isNonZero(stock.getCostAmount())
                || isNonZero(stock.getCostPrice());
    }

    private boolean isNonZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) != 0;
    }

    /**
     * 校验通用件编码：编码必须存在、且不能是自身。
     */
    private void validateUniversalCodes(Long productId, List<ProductSaveReqVO.Universal> universals) {
        if (CollUtil.isEmpty(universals)) {
            return;
        }
        Set<String> codes = universals.stream()
                .map(ProductSaveReqVO.Universal::getUniversalCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (codes.isEmpty()) {
            return;
        }
        List<ErpProductDO> exists = productMapper.selectListByCodes(codes);
        Set<String> existCodes = convertSet(exists, ErpProductDO::getCode);
        for (String code : codes) {
            if (!existCodes.contains(code)) {
                throw exception(PRODUCT_UNIVERSAL_CODE_INVALID, code);
            }
        }
        // 不能引用自己
        if (productId != null) {
            ErpProductDO self = productMapper.selectById(productId);
            if (self != null && self.getCode() != null && codes.contains(self.getCode())) {
                throw exception(PRODUCT_UNIVERSAL_CODE_SELF);
            }
        }
    }

    private void saveUniversals(Long productId, List<ProductSaveReqVO.Universal> universals) {
        if (CollUtil.isEmpty(universals)) {
            return;
        }
        List<ErpProductUniversalDO> list = new ArrayList<>(universals.size());
        for (ProductSaveReqVO.Universal item : universals) {
            list.add(new ErpProductUniversalDO()
                    .setProductId(productId)
                    .setUniversalCode(item.getUniversalCode())
                    .setUniversalName(item.getUniversalName())
                    .setUniversalVehicle(item.getUniversalVehicle()));
        }
        productUniversalMapper.insertBatch(list);
    }

    @Override
    public ErpProductDO getProduct(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    public ErpProductRespVO getProductDetail(Long id) {
        return buildProductDetail(getVisibleProduct(id), false);
    }

    @Override
    public ErpProductRespVO getProductArchiveDetail(Long id) {
        return buildProductDetail(getArchiveVisibleProduct(id), true);
    }

    private ErpProductRespVO buildProductDetail(ErpProductDO product,
                                                 boolean markReadonlyBySaleDistribution) {
        if (product == null) {
            return null;
        }
        List<ErpProductRespVO> list = buildProductVOList(
                Collections.singletonList(product), markReadonlyBySaleDistribution);
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        ErpProductRespVO result = list.get(0);
        applyProductFieldPermissions(Collections.singletonList(result));
        return result;
    }

    @Override
    public ErpProductStockDistributionRespVO getProductStockDistribution(Long productId) {
        ErpProductDO product = validateVisibleProductExists(productId);
        List<ErpWarehouseDO> warehouses = warehouseService.getCurrentUserAuthorizedWarehouseList();
        List<ErpStockDO> stocks = DataPermissionUtils.executeIgnore(() ->
                stockMapper.selectListByProductId(productId));
        Map<Long, ErpStockDO> stockMap = stocks.stream()
                .filter(stock -> stock.getWarehouseId() != null)
                .collect(Collectors.toMap(ErpStockDO::getWarehouseId, stock -> stock, (a, b) -> a,
                        LinkedHashMap::new));
        Set<Long> distributedWarehouseIds = new LinkedHashSet<>(stockMap.keySet());
        Set<Long> deptIds = warehouses.stream()
                .map(ErpWarehouseDO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, DeptRespDTO> rawDeptMap = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        Map<Long, DeptRespDTO> deptMap = rawDeptMap == null ? Collections.emptyMap() : rawDeptMap;

        ErpProductStockDistributionRespVO respVO = new ErpProductStockDistributionRespVO();
        respVO.setProductId(product.getId());
        respVO.setProductCode(product.getCode());
        respVO.setProductName(product.getName());
        respVO.setDistributedWarehouseIds(new ArrayList<>(distributedWarehouseIds));
        respVO.setWarehouses(warehouses.stream()
                .map(warehouse -> buildStockDistributionWarehouseItem(
                        warehouse, stockMap.get(warehouse.getId()), product.getDefaultWarehouseId(), deptMap))
                .collect(Collectors.toList()));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductStockDistribution(ErpProductStockDistributionSaveReqVO reqVO) {
        ErpProductDO product = validateVisibleProductExists(reqVO.getProductId());
        validateProductCanStockDistribute(product);

        Set<Long> targetWarehouseIds = reqVO.getWarehouseIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        validateStockDistributionWarehouseIds(targetWarehouseIds);

        List<ErpWarehouseDO> visibleWarehouses = warehouseService.getCurrentUserAuthorizedWarehouseList();
        Set<Long> visibleWarehouseIds = convertSet(visibleWarehouses, ErpWarehouseDO::getId);
        List<ErpStockDO> existingStocks = stockMapper.selectListByProductId(reqVO.getProductId());
        Map<Long, ErpStockDO> existingStockMap = existingStocks.stream()
                .filter(stock -> stock.getWarehouseId() != null)
                .collect(Collectors.toMap(ErpStockDO::getWarehouseId, stock -> stock, (a, b) -> a,
                        LinkedHashMap::new));

        List<Long> addedWarehouseIds = new ArrayList<>();
        for (Long warehouseId : targetWarehouseIds) {
            if (existingStockMap.containsKey(warehouseId)) {
                continue;
            }
            initProductStock(reqVO.getProductId(), warehouseId);
            addedWarehouseIds.add(warehouseId);
        }

        List<Long> removedWarehouseIds = new ArrayList<>();
        for (ErpStockDO stock : existingStocks) {
            Long warehouseId = stock.getWarehouseId();
            if (warehouseId == null || !visibleWarehouseIds.contains(warehouseId)
                    || targetWarehouseIds.contains(warehouseId)) {
                continue;
            }
            if (Objects.equals(warehouseId, product.getDefaultWarehouseId())) {
                throw exception(PRODUCT_STOCK_DISTRIBUTION_REMOVE_DENIED, getWarehouseDisplayName(warehouseId));
            }
            if (!canRemoveStockDistribution(stock)) {
                throw exception(PRODUCT_STOCK_DISTRIBUTION_REMOVE_DENIED, getWarehouseDisplayName(warehouseId));
            }
            stockMapper.deleteById(stock.getId());
            removedWarehouseIds.add(warehouseId);
        }

        syncProductOpenDeptByStockDistribution(product.getId());
        if (CollUtil.isNotEmpty(addedWarehouseIds) || CollUtil.isNotEmpty(removedWarehouseIds)) {
            ProductLogSnapshot snapshot = loadProductLogSnapshot(product.getId());
            recordProductLog(ERP_UPDATE_SUB_TYPE, product.getId(),
                    limitLogAction(buildProductLogAction("库存分发配件信息", snapshot)
                            + "；新增分发仓库：" + formatWarehouseNames(addedWarehouseIds)
                            + "；取消分发仓库：" + formatWarehouseNames(removedWarehouseIds)));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateProductStockDistribution(ErpProductStockDistributionBatchSaveReqVO reqVO) {
        Set<Long> productIds = reqVO.getProductIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(productIds)) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        Set<Long> targetWarehouseIds = reqVO.getWarehouseIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        validateStockDistributionWarehouseIds(targetWarehouseIds);

        for (Long productId : productIds) {
            ErpProductDO product = validateVisibleProductExists(productId);
            validateProductCanStockDistribute(product);

            List<ErpStockDO> existingStocks = stockMapper.selectListByProductId(productId);
            Set<Long> existingWarehouseIds = existingStocks.stream()
                    .map(ErpStockDO::getWarehouseId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            List<Long> addedWarehouseIds = new ArrayList<>();
            for (Long warehouseId : targetWarehouseIds) {
                if (existingWarehouseIds.contains(warehouseId)) {
                    continue;
                }
                initProductStock(productId, warehouseId);
                addedWarehouseIds.add(warehouseId);
            }
            syncProductOpenDeptByStockDistribution(productId);
            if (CollUtil.isNotEmpty(addedWarehouseIds)) {
                ProductLogSnapshot snapshot = loadProductLogSnapshot(productId);
                recordProductLog(ERP_UPDATE_SUB_TYPE, productId,
                        limitLogAction(buildProductLogAction("批量库存分发配件信息", snapshot)
                                + "；新增分发仓库：" + formatWarehouseNames(addedWarehouseIds)));
            }
        }
    }

    private void validateProductCanStockDistribute(ErpProductDO product) {
        if (Boolean.TRUE.equals(product.getMergedFlag())) {
            throw exception(PRODUCT_MERGED, product.getName());
        }
        if (CommonStatusEnum.isDisable(product.getStatus())) {
            throw exception(PRODUCT_STOCK_DISTRIBUTION_PRODUCT_DISABLED, product.getName());
        }
    }

    private void validateStockDistributionWarehouseIds(Set<Long> targetWarehouseIds) {
        if (CollUtil.isEmpty(targetWarehouseIds)) {
            throw exception(PRODUCT_STOCK_DISTRIBUTION_WAREHOUSE_EMPTY);
        }
        warehouseService.validateCurrentUserWarehousePermission(targetWarehouseIds);
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(targetWarehouseIds);
        for (Long warehouseId : targetWarehouseIds) {
            if (!warehouseMap.containsKey(warehouseId)) {
                throw exception(PRODUCT_STOCK_DISTRIBUTION_WAREHOUSE_DENIED, warehouseId);
            }
            ErpWarehouseDO warehouse = warehouseMap.get(warehouseId);
            if (CommonStatusEnum.isDisable(warehouse.getStatus())) {
                throw exception(PRODUCT_STOCK_DISTRIBUTION_WAREHOUSE_DENIED, warehouse.getName());
            }
        }
    }

    @Override
    public List<ErpProductRespVO> getProductVOListByStatus(Integer status) {
        ErpProductPageReqVO visibleReqVO = buildCurrentUserVisibleReqVO();
        List<ErpProductDO> list = DataPermissionUtils.executeIgnore(() ->
                productMapper.selectVisibleListByStatus(status, visibleReqVO));
        List<ErpProductRespVO> result = buildProductVOList(list);
        applyProductFieldPermissions(result);
        return result;
    }

    @Override
    public List<ErpProductRespVO> getProductVOList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ErpProductDO> list = productMapper.selectByIds(ids);
        List<ErpProductRespVO> result = buildProductVOList(list);
        applyProductFieldPermissions(result);
        return result;
    }

    @Override
    public PageResult<ErpProductRespVO> getProductVOPage(ErpProductPageReqVO pageReqVO) {
        Set<String> hiddenFieldSet = getHiddenFieldSet();
        boolean includeSaleDistributedArchive = Boolean.TRUE.equals(
                pageReqVO.getIncludeSaleDistributedArchive());
        if (includeSaleDistributedArchive) {
            applyProductArchiveVisibleScope(pageReqVO);
        } else {
            applyProductVisibleScope(pageReqVO);
        }
        PageResult<ErpProductDO> pageResult = DataPermissionUtils.executeIgnore(() ->
                productMapper.selectPage(pageReqVO,
                        buildKeywordSearchFields(hiddenFieldSet),
                        buildCustomKeywordSearchColumns(hiddenFieldSet)));
        List<ErpProductRespVO> result = buildProductVOList(
                pageResult.getList(), includeSaleDistributedArchive);
        applyProductFieldPermissions(result);
        return new PageResult<>(result, pageResult.getTotal());
    }

    private void applyProductVisibleScope(ErpProductPageReqVO pageReqVO) {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            pageReqVO.setVisibleAll(false);
            pageReqVO.setVisibleDeptIds(Collections.emptyList());
            pageReqVO.setVisibleWarehouseIds(Collections.emptyList());
            pageReqVO.setVisibleSelfUserId(null);
            return;
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(loginUserId, "erp_product");
        if (permission == null || Boolean.TRUE.equals(permission.getAll())) {
            pageReqVO.setVisibleAll(true);
            pageReqVO.setVisibleDeptIds(Collections.emptyList());
            pageReqVO.setVisibleWarehouseIds(Collections.emptyList());
            pageReqVO.setVisibleSelfUserId(null);
            return;
        }
        pageReqVO.setVisibleAll(false);
        pageReqVO.setVisibleDeptIds(permission.getDeptIds());
        pageReqVO.setVisibleWarehouseIds(warehouseService.getCurrentUserAuthorizedWarehouseIds());
        pageReqVO.setVisibleSelfUserId(Boolean.TRUE.equals(permission.getSelf()) ? loginUserId : null);
    }

    /**
     * 配件档案读取范围：保留原产品数据权限，并额外纳入当前部门获分配、且销售业务可选的仓库。
     * 该范围只用于配件分页、详情和导出，不用于业务单据精简列表或任何写操作。
     */
    private void applyProductArchiveVisibleScope(ErpProductPageReqVO pageReqVO) {
        applyProductVisibleScope(pageReqVO);
        if (Boolean.TRUE.equals(pageReqVO.getVisibleAll())) {
            return;
        }
        Set<Long> visibleWarehouseIds = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(pageReqVO.getVisibleWarehouseIds())) {
            visibleWarehouseIds.addAll(pageReqVO.getVisibleWarehouseIds());
        }
        visibleWarehouseIds.addAll(getCurrentUserSaleDistributedSelectableWarehouseIds());
        pageReqVO.setVisibleWarehouseIds(visibleWarehouseIds);
    }

    private ErpProductPageReqVO buildCurrentUserVisibleReqVO() {
        ErpProductPageReqVO reqVO = new ErpProductPageReqVO();
        applyProductVisibleScope(reqVO);
        return reqVO;
    }

    private ErpProductPageReqVO buildCurrentUserArchiveVisibleReqVO() {
        ErpProductPageReqVO reqVO = new ErpProductPageReqVO();
        applyProductArchiveVisibleScope(reqVO);
        return reqVO;
    }

    private ErpProductDO getVisibleProduct(Long id) {
        if (id == null) {
            return null;
        }
        ErpProductPageReqVO visibleReqVO = buildCurrentUserVisibleReqVO();
        return DataPermissionUtils.executeIgnore(() ->
                productMapper.selectVisibleById(id, visibleReqVO));
    }

    private ErpProductDO getArchiveVisibleProduct(Long id) {
        if (id == null) {
            return null;
        }
        ErpProductPageReqVO visibleReqVO = buildCurrentUserArchiveVisibleReqVO();
        return DataPermissionUtils.executeIgnore(() ->
                productMapper.selectVisibleById(id, visibleReqVO));
    }

    private ErpProductDO validateManageableProductExists(Long id) {
        ErpProductDO product = getVisibleProduct(id);
        if (product != null) {
            return product;
        }
        if (getArchiveVisibleProduct(id) != null) {
            throw exception(PRODUCT_READONLY_BY_SALE_DISTRIBUTION);
        }
        throw exception(PRODUCT_NOT_EXISTS);
    }

    private void validateManageableProductIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        ids.stream().filter(Objects::nonNull).distinct().forEach(this::validateManageableProductExists);
    }

    private ErpProductDO validateVisibleProductExists(Long id) {
        ErpProductDO product = getVisibleProduct(id);
        if (product == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        return product;
    }

    private List<ErpProductRespVO> buildProductVOList(List<ErpProductDO> list) {
        return buildProductVOList(list, false);
    }

    private List<ErpProductRespVO> buildProductVOList(List<ErpProductDO> list,
                                                      boolean markReadonlyBySaleDistribution) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1. 批量查询关联数据
        Set<Long> productIds = convertSet(list, ErpProductDO::getId);
        Set<Long> readonlyProductIds = markReadonlyBySaleDistribution
                ? getSaleDistributedReadonlyProductIds(productIds) : Collections.emptySet();
        Set<Long> saleDistributedWarehouseIds = CollUtil.isEmpty(readonlyProductIds)
                ? Collections.emptySet() : getCurrentUserSaleDistributedSelectableWarehouseIds();
        Set<Long> warehouseIds = new HashSet<>();
        for (ErpProductDO p : list) {
            if (p.getDefaultWarehouseId() != null) {
                warehouseIds.add(p.getDefaultWarehouseId());
            }
        }
        Map<Long, ErpProductCategoryDO> categoryMap = productCategoryService.getProductCategoryMap(
                convertSet(list, ErpProductDO::getCategoryId));
        Map<Long, ErpProductUnitDO> unitMap = productUnitService.getProductUnitMap(
                convertSet(list, ErpProductDO::getUnitId));
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseIds.isEmpty()
                ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds));
        Map<Long, List<Long>> productDeptMap = getProductDeptMap(productIds);
        Map<Long, List<Long>> productOpenDeptMap = new LinkedHashMap<>(getProductOpenDeptMap(productIds));
        if (CollUtil.isNotEmpty(readonlyProductIds)) {
            Map<Long, List<Long>> readonlyProductOpenDeptMap = getProductOpenDeptMap(
                    readonlyProductIds, saleDistributedWarehouseIds);
            readonlyProductIds.forEach(productId -> {
                List<Long> openDeptIds = readonlyProductOpenDeptMap.get(productId);
                if (CollUtil.isEmpty(openDeptIds)) {
                    productOpenDeptMap.remove(productId);
                } else {
                    productOpenDeptMap.put(productId, openDeptIds);
                }
            });
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(collectUserIds(list));
        Set<Long> allDeptIds = new HashSet<>(convertSet(list, ErpProductDO::getDeptId));
        allDeptIds.addAll(convertSet(list, ErpProductDO::getCreateDeptId));
        productDeptMap.values().forEach(allDeptIds::addAll);
        productOpenDeptMap.values().forEach(allDeptIds::addAll);
        list.forEach(product -> {
            Long creatorId = parseUserId(product.getCreator());
            AdminUserRespDTO creator = creatorId == null ? null : userMap.get(creatorId);
            if (creator != null && creator.getDeptId() != null) {
                allDeptIds.add(creator.getDeptId());
            }
        });
        allDeptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = loadDeptWithParents(allDeptIds);
        Map<Long, BigDecimal> stockMap = new HashMap<>(stockMapper.selectSumMapByProductIds(productIds));
        Map<Long, BigDecimal> lockCountMap = new HashMap<>(stockLockMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpStockLockDO>()
                        .in(ErpStockLockDO::getProductId, productIds)
                        .eq(ErpStockLockDO::getStatus, 1))
                .stream()
                .collect(Collectors.groupingBy(ErpStockLockDO::getProductId,
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> item.getLockCount() != null ? item.getLockCount() : BigDecimal.ZERO,
                                BigDecimal::add))));
        if (CollUtil.isNotEmpty(readonlyProductIds)) {
            Map<Long, BigDecimal> readonlyStockMap = selectStockCountMap(
                    readonlyProductIds, saleDistributedWarehouseIds);
            Map<Long, BigDecimal> readonlyLockCountMap = selectStockLockCountMap(
                    readonlyProductIds, saleDistributedWarehouseIds);
            readonlyProductIds.forEach(productId -> {
                stockMap.put(productId, readonlyStockMap.getOrDefault(productId, BigDecimal.ZERO));
                lockCountMap.put(productId, readonlyLockCountMap.getOrDefault(productId, BigDecimal.ZERO));
            });
        }
        List<ErpProductUniversalDO> universalList = productUniversalMapper.selectListByProductIds(productIds);
        Map<Long, List<ErpProductUniversalDO>> universalByProduct = universalList.stream()
                .collect(Collectors.groupingBy(ErpProductUniversalDO::getProductId));

        // 2. 组装
        List<ErpProductRespVO> result = BeanUtils.toBean(list, ErpProductRespVO.class, vo -> {
            MapUtils.findAndThen(categoryMap, vo.getCategoryId(),
                    c -> vo.setCategoryName(c.getName()));
            MapUtils.findAndThen(unitMap, vo.getUnitId(),
                    u -> vo.setUnitName(u.getName()));
            fillProductWarehouseInfo(vo, warehouseMap);
            List<Long> deptIds = productDeptMap.get(vo.getId());
            if (CollUtil.isEmpty(deptIds) && vo.getDeptId() != null) {
                deptIds = Collections.singletonList(vo.getDeptId());
            }
            vo.setDeptIds(deptIds == null ? Collections.emptyList() : deptIds);
            vo.setDeptNames(formatDeptNames(vo.getDeptIds(), deptMap));
            vo.setDeptName(vo.getDeptNames());
            List<Long> openDeptIds = productOpenDeptMap.get(vo.getId());
            String openDeptNames = formatDeptNames(openDeptIds, deptMap);
            vo.setOpenDeptNames(StringUtils.hasText(openDeptNames) ? openDeptNames : vo.getDeptNames());
            fillProductAuditInfo(vo, userMap, deptMap);
            Long updaterId = parseUserId(vo.getUpdater());
            if (updaterId != null) {
                MapUtils.findAndThen(userMap, updaterId, user -> vo.setUpdaterName(user.getNickname()));
            }
            // 库存：currentStock 实时聚合；lockCount 为占用数量；available=current-lock
            BigDecimal current = stockMap.getOrDefault(vo.getId(), BigDecimal.ZERO);
            BigDecimal lockCount = lockCountMap.getOrDefault(vo.getId(), BigDecimal.ZERO);
            vo.setReadonlyBySaleDistribution(readonlyProductIds.contains(vo.getId()));
            vo.setCurrentStock(current);
            vo.setLockCount(lockCount);
            vo.setInTransitStock(BigDecimal.ZERO);
            vo.setAvailableStock(current.subtract(lockCount));
            vo.setLowStockWarning(isLowStockWarning(current, vo.getStockMin()));
            // 通用件子列表
            List<ErpProductUniversalDO> universals = universalByProduct.get(vo.getId());
            vo.setUniversals(universals == null ? Collections.emptyList()
                    : BeanUtils.toBean(universals, ErpProductRespVO.Universal.class));
        });
        fillCustomFields(result, productIds);
        return result;
    }

    private Set<Long> getCurrentUserSaleDistributedSelectableWarehouseIds() {
        Set<Long> distributedWarehouseIds = new LinkedHashSet<>(
                warehouseService.getCurrentUserSaleDistributedVisibleWarehouseIds());
        if (CollUtil.isEmpty(distributedWarehouseIds)) {
            return distributedWarehouseIds;
        }
        Set<Long> selectableWarehouseIds = warehouseService.getCurrentUserVisibleSaleWarehouseList().stream()
                .map(ErpWarehouseDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        distributedWarehouseIds.retainAll(selectableWarehouseIds);
        return distributedWarehouseIds;
    }

    private Set<Long> getSaleDistributedReadonlyProductIds(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptySet();
        }
        ErpProductPageReqVO baseVisibleReqVO = buildCurrentUserVisibleReqVO();
        if (Boolean.TRUE.equals(baseVisibleReqVO.getVisibleAll())) {
            return Collections.emptySet();
        }
        Set<Long> baseVisibleProductIds = DataPermissionUtils.executeIgnore(() ->
                        productMapper.selectVisibleListByIds(productIds, baseVisibleReqVO)).stream()
                .map(ErpProductDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return productIds.stream()
                .filter(Objects::nonNull)
                .filter(productId -> !baseVisibleProductIds.contains(productId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<Long, BigDecimal> selectStockCountMap(Collection<Long> productIds,
                                                      Collection<Long> warehouseIds) {
        if (CollUtil.isEmpty(productIds) || CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> stockMapper.selectList(
                        new LambdaQueryWrapper<ErpStockDO>()
                                .in(ErpStockDO::getProductId, productIds)
                                .in(ErpStockDO::getWarehouseId, warehouseIds))).stream()
                .collect(Collectors.groupingBy(ErpStockDO::getProductId,
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> item.getCount() != null ? item.getCount() : BigDecimal.ZERO,
                                BigDecimal::add)));
    }

    private Map<Long, BigDecimal> selectStockLockCountMap(Collection<Long> productIds,
                                                          Collection<Long> warehouseIds) {
        if (CollUtil.isEmpty(productIds) || CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> stockLockMapper.selectList(
                        new LambdaQueryWrapper<ErpStockLockDO>()
                                .in(ErpStockLockDO::getProductId, productIds)
                                .in(ErpStockLockDO::getWarehouseId, warehouseIds)
                                .eq(ErpStockLockDO::getStatus, 1))).stream()
                .collect(Collectors.groupingBy(ErpStockLockDO::getProductId,
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> item.getLockCount() != null ? item.getLockCount() : BigDecimal.ZERO,
                                BigDecimal::add)));
    }

    private Set<Long> collectUserIds(List<ErpProductDO> list) {
        Set<Long> userIds = new HashSet<>();
        list.forEach(product -> {
            addUserId(userIds, product.getCreator());
            addUserId(userIds, product.getUpdater());
        });
        return userIds;
    }

    private void addUserId(Set<Long> userIds, String userId) {
        Long parsed = parseUserId(userId);
        if (parsed != null) {
            userIds.add(parsed);
        }
    }

    private static Long parseUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static void fillProductAuditInfo(ErpProductRespVO vo,
                                     Map<Long, AdminUserRespDTO> userMap,
                                     Map<Long, DeptRespDTO> deptMap) {
        Long creatorId = parseUserId(vo.getCreator());
        AdminUserRespDTO creator = creatorId == null ? null : userMap.get(creatorId);
        if (creator != null) {
            vo.setCreatorName(creator.getNickname());
            if (vo.getCreateDeptId() == null) {
                vo.setCreateDeptId(creator.getDeptId());
            }
        }
        vo.setCreateDeptName(buildDeptFullName(vo.getCreateDeptId(), deptMap));
    }

    static void fillProductWarehouseInfo(ErpProductRespVO vo,
                                         Map<Long, ErpWarehouseDO> warehouseMap) {
        MapUtils.findAndThen(warehouseMap, vo.getDefaultWarehouseId(),
                warehouse -> vo.setDefaultWarehouseName(warehouse.getName()));
    }

    private void recordProductLog(String subType, Long productId, String action) {
        operateLogService.record(ERP_PRODUCT_TYPE, subType, productId, action, String.valueOf(productId));
    }

    private ProductLogSnapshot loadProductLogSnapshot(Long productId) {
        if (productId == null) {
            return null;
        }
        ErpProductDO product = productMapper.selectById(productId);
        if (product == null) {
            return null;
        }
        List<ErpFieldConfigDO> customFieldConfigs = getCustomFieldConfigs();
        Map<String, Object> customFields = loadProductLogCustomFields(productId, customFieldConfigs);
        List<String> universalSummaries = productUniversalMapper.selectListByProductId(productId).stream()
                .sorted(Comparator.comparing(item -> valueOrDash(item.getUniversalCode())))
                .map(this::formatUniversalForLog)
                .collect(Collectors.toList());
        return new ProductLogSnapshot(product, customFields, customFieldConfigs, universalSummaries);
    }

    private Map<Long, ProductLogSnapshot> loadProductLogSnapshotMap(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        Map<Long, ProductLogSnapshot> result = new LinkedHashMap<>();
        for (Long productId : productIds) {
            ProductLogSnapshot snapshot = loadProductLogSnapshot(productId);
            if (snapshot != null) {
                result.put(productId, snapshot);
            }
        }
        return result;
    }

    private Map<String, Object> loadProductLogCustomFields(Long productId, List<ErpFieldConfigDO> customFieldConfigs) {
        if (productId == null || CollUtil.isEmpty(customFieldConfigs)) {
            return Collections.emptyMap();
        }
        List<String> columns = customFieldConfigs.stream()
                .map(ErpFieldConfigDO::getPhysicalColumn)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(columns)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = productMapper.selectCustomFieldMaps(Collections.singleton(productId), columns);
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyMap();
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> values = new LinkedHashMap<>();
        for (ErpFieldConfigDO config : customFieldConfigs) {
            if (StringUtils.hasText(config.getPhysicalColumn())) {
                values.put(config.getFieldName(), normalizeCustomFieldValue(row.get(config.getPhysicalColumn())));
            }
        }
        return values;
    }

    private String buildProductLogAction(String operation, ProductLogSnapshot snapshot) {
        return buildProductLogAction(operation, null, snapshot);
    }

    private String buildProductLogAction(String operation, ProductLogSnapshot before, ProductLogSnapshot after) {
        ProductLogSnapshot identitySnapshot = after != null ? after : before;
        StringBuilder builder = new StringBuilder(operation)
                .append("；").append(buildProductIdentityText(identitySnapshot));
        if (before != null && after != null) {
            builder.append("；变更明细：").append(buildProductChangeSummary(before, after));
        }
        return limitLogAction(builder.toString());
    }

    private String buildProductIdentityText(ProductLogSnapshot snapshot) {
        ErpProductDO product = snapshot == null ? null : snapshot.getProduct();
        return "编号：" + valueOrDash(product == null ? null : product.getId())
                + "；名称：" + valueOrDash(product == null ? null : product.getName())
                + "；编码：" + valueOrDash(product == null ? null : product.getCode());
    }

    private String buildProductChangeSummary(ProductLogSnapshot beforeSnapshot, ProductLogSnapshot afterSnapshot) {
        ErpProductDO before = beforeSnapshot.getProduct();
        ErpProductDO after = afterSnapshot.getProduct();
        List<String> changes = new ArrayList<>();
        addChange(changes, "所属部门", formatDept(before.getDeptId()), formatDept(after.getDeptId()));
        addChange(changes, "名称", before.getName(), after.getName());
        addChange(changes, "编码", before.getCode(), after.getCode());
        addChange(changes, "条码", before.getBarCode(), after.getBarCode());
        addChange(changes, "分类", formatCategory(before.getCategoryId()), formatCategory(after.getCategoryId()));
        addChange(changes, "批次号管理", formatBoolean(before.getBatchNoEnabled()), formatBoolean(after.getBatchNoEnabled()));
        addChange(changes, "单位", formatUnit(before.getUnitId()), formatUnit(after.getUnitId()));
        addChange(changes, "状态", formatStatus(before.getStatus()), formatStatus(after.getStatus()));
        addChange(changes, "停用人", formatUser(before.getDisabledBy()), formatUser(after.getDisabledBy()));
        addChange(changes, "停用时间", before.getDisabledTime(), after.getDisabledTime());
        addChange(changes, "默认仓库", formatWarehouse(before.getDefaultWarehouseId()), formatWarehouse(after.getDefaultWarehouseId()));
        addChange(changes, "适用车型", before.getVehicleModel(), after.getVehicleModel());
        addChange(changes, "厂家编码", before.getFactoryCode(), after.getFactoryCode());
        addChange(changes, "规格型号", before.getStandard(), after.getStandard());
        addChange(changes, "备注", summarizeLongText(before.getRemark()), summarizeLongText(after.getRemark()));
        addChange(changes, "品牌", before.getBrand(), after.getBrand());
        addChange(changes, "OE 编号", before.getOeNumber(), after.getOeNumber());
        addChange(changes, "产地", before.getOriginPlace(), after.getOriginPlace());
        addChange(changes, "特征码", before.getFeatureCode(), after.getFeatureCode());
        addChange(changes, "图号", before.getDrawingNo(), after.getDrawingNo());
        addChange(changes, "货架位置", before.getShelf(), after.getShelf());
        addChange(changes, "采购价", before.getPurchasePrice(), after.getPurchasePrice());
        addChange(changes, "销售价", before.getSalePrice(), after.getSalePrice());
        addChange(changes, "最低价", before.getMinPrice(), after.getMinPrice());
        addChange(changes, "参考价", before.getReferencePrice(), after.getReferencePrice());
        addChange(changes, "零售价", before.getRetailPrice(), after.getRetailPrice());
        addChange(changes, "最后采购入库价", before.getLastPurchasePrice(), after.getLastPurchasePrice());
        addChange(changes, "毛利率", before.getGrossProfitRate(), after.getGrossProfitRate());
        addChange(changes, "备用价1", before.getBackupPrice1(), after.getBackupPrice1());
        addChange(changes, "批发价", before.getWholesalePrice(), after.getWholesalePrice());
        addChange(changes, "股份价", before.getSharePrice(), after.getSharePrice());
        addChange(changes, "库存上限", before.getStockMax(), after.getStockMax());
        addChange(changes, "库存下限", before.getStockMin(), after.getStockMin());
        addChange(changes, "标准库存", before.getStockStandard(), after.getStockStandard());
        addChange(changes, "包装数", before.getPackageQty(), after.getPackageQty());
        addChange(changes, "保质期天数", before.getExpiryDay(), after.getExpiryDay());
        addChange(changes, "重量", before.getWeight(), after.getWeight());
        addImageChange(changes, "主图", before.getMainImage(), after.getMainImage());
        addLongContentChange(changes, "详情内容", before.getDetailContent(), after.getDetailContent());
        addChange(changes, "合并状态", formatBoolean(before.getMergedFlag()), formatBoolean(after.getMergedFlag()));
        addChange(changes, "合并目标", before.getMergedTargetId(), after.getMergedTargetId());
        addChange(changes, "合并人", formatUser(before.getMergedBy()), formatUser(after.getMergedBy()));
        addChange(changes, "合并时间", before.getMergedTime(), after.getMergedTime());
        addCustomFieldChanges(changes, beforeSnapshot, afterSnapshot);
        addChange(changes, "通用件", summarizeList(beforeSnapshot.getUniversalSummaries()),
                summarizeList(afterSnapshot.getUniversalSummaries()));
        return CollUtil.isEmpty(changes) ? "无字段变化" : summarizeChanges(changes);
    }

    private void addCustomFieldChanges(List<String> changes, ProductLogSnapshot beforeSnapshot,
                                       ProductLogSnapshot afterSnapshot) {
        Map<String, ErpFieldConfigDO> configMap = new LinkedHashMap<>();
        beforeSnapshot.getCustomFieldConfigs().forEach(config -> configMap.put(config.getFieldName(), config));
        afterSnapshot.getCustomFieldConfigs().forEach(config -> configMap.put(config.getFieldName(), config));
        Set<String> fieldNames = new HashSet<>();
        fieldNames.addAll(beforeSnapshot.getCustomFields().keySet());
        fieldNames.addAll(afterSnapshot.getCustomFields().keySet());
        fieldNames.stream().sorted().forEach(fieldName -> {
            ErpFieldConfigDO config = configMap.get(fieldName);
            String label = config != null && StringUtils.hasText(config.getFieldLabel())
                    ? config.getFieldLabel() : fieldName;
            addChange(changes, label,
                    summarizeLongText(beforeSnapshot.getCustomFields().get(fieldName)),
                    summarizeLongText(afterSnapshot.getCustomFields().get(fieldName)));
        });
    }

    private void addChange(List<String> changes, String label, Object before, Object after) {
        if (before instanceof BigDecimal && after instanceof BigDecimal
                && ((BigDecimal) before).compareTo((BigDecimal) after) == 0) {
            return;
        }
        if (Objects.equals(before, after)) {
            return;
        }
        changes.add(label + "：" + valueOrDash(before) + " -> " + valueOrDash(after));
    }

    private void addImageChange(List<String> changes, String label, String before, String after) {
        if (Objects.equals(before, after)) {
            return;
        }
        changes.add(label + "：" + formatImageValue(before) + " -> " + formatImageValue(after));
    }

    private void addLongContentChange(List<String> changes, String label, String before, String after) {
        if (Objects.equals(before, after)) {
            return;
        }
        changes.add(label + "：" + formatLongContentValue(before) + " -> " + formatLongContentValue(after));
    }

    private String valueOrDash(Object value) {
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? "-" : String.valueOf(value);
    }

    private String formatCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        ErpProductCategoryDO category = productCategoryService.getProductCategory(categoryId);
        return formatNameAndId(category == null ? null : category.getName(), categoryId);
    }

    private String formatUnit(Long unitId) {
        if (unitId == null) {
            return null;
        }
        ErpProductUnitDO unit = productUnitService.getProductUnit(unitId);
        return formatNameAndId(unit == null ? null : unit.getName(), unitId);
    }

    private String formatWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        ErpWarehouseDO warehouse = warehouseService.getWarehouse(warehouseId);
        return formatNameAndId(warehouse == null ? null : warehouse.getName(), warehouseId);
    }

    private String formatDept(Long deptId) {
        if (deptId == null) {
            return null;
        }
        DeptRespDTO dept = deptApi.getDept(deptId);
        return formatNameAndId(dept == null ? null : dept.getName(), deptId);
    }

    private String formatUser(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return formatNameAndId(user == null ? null : user.getNickname(), userId);
    }

    private String formatNameAndId(String name, Long id) {
        return StringUtils.hasText(name) ? name + "(" + id + ")" : String.valueOf(id);
    }

    private String formatStatus(Integer status) {
        if (status == null) {
            return null;
        }
        if (CommonStatusEnum.isEnable(status)) {
            return CommonStatusEnum.ENABLE.getName() + "(" + status + ")";
        }
        if (CommonStatusEnum.isDisable(status)) {
            return CommonStatusEnum.DISABLE.getName() + "(" + status + ")";
        }
        return String.valueOf(status);
    }

    private String formatBoolean(Boolean value) {
        return value == null ? null : (Boolean.TRUE.equals(value) ? "是" : "否");
    }

    private String formatImageValue(String value) {
        return StringUtils.hasText(value) ? "已设置" : "未设置";
    }

    private String formatLongContentValue(String value) {
        return StringUtils.hasText(value) ? "已填写" : "未填写";
    }

    private Object summarizeLongText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.length() <= 80 ? text : text.substring(0, 80) + "...";
    }

    private String formatUniversalForLog(ErpProductUniversalDO item) {
        StringBuilder builder = new StringBuilder(valueOrDash(item.getUniversalCode()));
        if (StringUtils.hasText(item.getUniversalName())) {
            builder.append("/").append(item.getUniversalName());
        }
        if (StringUtils.hasText(item.getUniversalVehicle())) {
            builder.append("/").append(item.getUniversalVehicle());
        }
        return builder.toString();
    }

    private String summarizeList(List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        List<String> limited = values.stream().limit(10).collect(Collectors.toList());
        String result = String.join(",", limited);
        if (values.size() > limited.size()) {
            result += "等" + values.size() + "项";
        }
        return result;
    }

    private String summarizeChanges(List<String> changes) {
        if (changes.size() <= 20) {
            return String.join("；", changes);
        }
        return String.join("；", changes.subList(0, 20)) + "；等" + changes.size() + "项";
    }

    private String limitLogAction(String action) {
        if (action == null || action.length() <= 1900) {
            return action;
        }
        return action.substring(0, 1900) + "...";
    }

    private String buildPriceAdjustRule(
            cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchAdjustPriceReqVO reqVO) {
        return formatPriceType(reqVO.getSourcePriceType()) + "按" + formatAdjustMethod(reqVO.getAdjustMethod())
                + valueOrDash(reqVO.getAdjustCoefficient()) + "调整到" + formatPriceType(reqVO.getTargetPriceType())
                + "，保留" + valueOrDash(reqVO.getDecimalPlaces()) + "位小数";
    }

    private String formatAdjustMethod(String method) {
        if ("ADD".equals(method)) {
            return "加";
        }
        if ("SUBTRACT".equals(method)) {
            return "减";
        }
        if ("MULTIPLY".equals(method)) {
            return "乘";
        }
        if ("DIVIDE".equals(method)) {
            return "除";
        }
        return valueOrDash(method);
    }

    private String formatPriceType(String priceType) {
        if ("SPARE_PRICE_1".equals(priceType)) {
            return "备用价1";
        }
        if ("REFERENCE_PRICE".equals(priceType)) {
            return "参考价";
        }
        if ("RETAIL_PRICE".equals(priceType)) {
            return "零售价";
        }
        if ("WHOLESALE_PRICE".equals(priceType)) {
            return "批发价";
        }
        if ("SHARE_PRICE".equals(priceType)) {
            return "股份价";
        }
        if ("LAST_PURCHASE_PRICE".equals(priceType)) {
            return "最后采购入库价";
        }
        if ("PURCHASE_PRICE".equals(priceType)) {
            return "采购价";
        }
        if ("SALE_PRICE".equals(priceType)) {
            return "销售价";
        }
        return valueOrDash(priceType);
    }

    private static class ProductLogSnapshot {

        private final ErpProductDO product;
        private final Map<String, Object> customFields;
        private final List<ErpFieldConfigDO> customFieldConfigs;
        private final List<String> universalSummaries;

        ProductLogSnapshot(ErpProductDO product, Map<String, Object> customFields,
                           List<ErpFieldConfigDO> customFieldConfigs, List<String> universalSummaries) {
            this.product = product;
            this.customFields = customFields == null ? Collections.emptyMap() : customFields;
            this.customFieldConfigs = customFieldConfigs == null ? Collections.emptyList() : customFieldConfigs;
            this.universalSummaries = universalSummaries == null ? Collections.emptyList() : universalSummaries;
        }

        ErpProductDO getProduct() {
            return product;
        }

        Map<String, Object> getCustomFields() {
            return customFields;
        }

        List<ErpFieldConfigDO> getCustomFieldConfigs() {
            return customFieldConfigs;
        }

        List<String> getUniversalSummaries() {
            return universalSummaries;
        }
    }

    private void applyProductFieldPermissions(List<ErpProductRespVO> list) {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
        for (ErpProductRespVO vo : list) {
            if (isFieldHidden(hiddenFieldSet, "code")) {
                vo.setCode(null);
                vo.setProductCode(null);
            }
            if (isFieldHidden(hiddenFieldSet, "deptId") || isFieldHidden(hiddenFieldSet, "deptIds")) {
                vo.setDeptId(null);
                vo.setDeptIds(Collections.emptyList());
                vo.setDeptName(null);
                vo.setDeptNames(null);
            }
            if (isFieldHidden(hiddenFieldSet, "name")) {
                vo.setName(null);
            }
            if (isFieldHidden(hiddenFieldSet, "unitId")) {
                vo.setUnitId(null);
                vo.setUnitName(null);
            }
            if (isFieldHidden(hiddenFieldSet, "defaultWarehouseId")) {
                vo.setDefaultWarehouseId(null);
                vo.setDefaultWarehouseName(null);
            }
            if (isFieldHidden(hiddenFieldSet, "vehicleModel")) {
                vo.setVehicleModel(null);
            }
            if (isFieldHidden(hiddenFieldSet, "standard")) {
                vo.setStandard(null);
            }
            if (isFieldHidden(hiddenFieldSet, "categoryId")) {
                vo.setCategoryId(null);
                vo.setCategoryName(null);
            }
            if (isFieldHidden(hiddenFieldSet, "batchNoEnabled")) {
                vo.setBatchNoEnabled(null);
            }
            if (isFieldHidden(hiddenFieldSet, "barCode")) {
                vo.setBarCode(null);
            }
            if (isFieldHidden(hiddenFieldSet, "factoryCode")) {
                vo.setFactoryCode(null);
            }
            if (isFieldHidden(hiddenFieldSet, "status")) {
                vo.setStatus(null);
            }
            if (isFieldHidden(hiddenFieldSet, "remark")) {
                vo.setRemark(null);
            }
            if (isFieldHidden(hiddenFieldSet, "brand")) {
                vo.setBrand(null);
            }
            if (isFieldHidden(hiddenFieldSet, "oeNumber")) {
                vo.setOeNumber(null);
            }
            if (isFieldHidden(hiddenFieldSet, "originPlace")) {
                vo.setOriginPlace(null);
            }
            if (isFieldHidden(hiddenFieldSet, "featureCode")) {
                vo.setFeatureCode(null);
            }
            if (isFieldHidden(hiddenFieldSet, "drawingNo")) {
                vo.setDrawingNo(null);
            }
            if (isFieldHidden(hiddenFieldSet, "shelf")) {
                vo.setShelf(null);
            }
            if (isFieldHidden(hiddenFieldSet, "purchasePrice")) {
                vo.setPurchasePrice(null);
            }
            if (isFieldHidden(hiddenFieldSet, "salePrice")) {
                vo.setSalePrice(null);
            }
            if (isFieldHidden(hiddenFieldSet, "minPrice")) {
                vo.setMinPrice(null);
            }
            if (isFieldHidden(hiddenFieldSet, "referencePrice")) {
                vo.setReferencePrice(null);
            }
            if (isFieldHidden(hiddenFieldSet, "retailPrice")) {
                vo.setRetailPrice(null);
            }
            if (isFieldHidden(hiddenFieldSet, "lastPurchasePrice")) {
                vo.setLastPurchasePrice(null);
            }
            if (isFieldHidden(hiddenFieldSet, "grossProfitRate")) {
                vo.setGrossProfitRate(null);
            }
            if (isFieldHidden(hiddenFieldSet, "backupPrice1")) {
                vo.setBackupPrice1(null);
            }
            if (isFieldHidden(hiddenFieldSet, "wholesalePrice")) {
                vo.setWholesalePrice(null);
            }
            if (isFieldHidden(hiddenFieldSet, "sharePrice")) {
                vo.setSharePrice(null);
            }
            if (isFieldHidden(hiddenFieldSet, "weight")) {
                vo.setWeight(null);
            }
            if (isFieldHidden(hiddenFieldSet, "stockMax")) {
                vo.setStockMax(null);
            }
            if (isFieldHidden(hiddenFieldSet, "stockMin")) {
                vo.setStockMin(null);
            }
            if (isFieldHidden(hiddenFieldSet, "stockStandard")) {
                vo.setStockStandard(null);
            }
            if (isFieldHidden(hiddenFieldSet, "packageQty")) {
                vo.setPackageQty(null);
            }
            if (isFieldHidden(hiddenFieldSet, "currentStock")) {
                vo.setCurrentStock(null);
            }
            if (isFieldHidden(hiddenFieldSet, "inTransitStock")) {
                vo.setInTransitStock(null);
            }
            if (isFieldHidden(hiddenFieldSet, "availableStock")) {
                vo.setAvailableStock(null);
            }
            if (vo.getCustomFields() != null) {
                vo.getCustomFields().keySet().removeIf(fieldKey -> isFieldHidden(hiddenFieldSet, fieldKey));
            }
        }
    }

    private boolean isFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey) || hiddenFields.contains("col_" + fieldKey);
    }

    private Set<String> getHiddenFieldSet() {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        return CollUtil.isEmpty(hiddenFields) ? Collections.emptySet() : new HashSet<>(hiddenFields);
    }

    private Set<String> buildKeywordSearchFields(Set<String> hiddenFieldSet) {
        Set<String> fields = new LinkedHashSet<>();
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_CODE, "code");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_NAME, "name");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_BAR_CODE, "barCode");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_VEHICLE_MODEL, "vehicleModel");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_FACTORY_CODE, "factoryCode");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_STANDARD, "standard");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_REMARK, "remark");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_BRAND, "brand");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_OE_NUMBER, "oeNumber");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_ORIGIN_PLACE, "originPlace");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_FEATURE_CODE, "featureCode");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_DRAWING_NO, "drawingNo");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_SHELF, "shelf");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_CATEGORY_NAME, "categoryId", "categoryName");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_UNIT_NAME, "unitId", "unitName");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_DEFAULT_WAREHOUSE_NAME,
                "defaultWarehouseId", "defaultWarehouseName");
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_DEPT_NAME, "deptId", "deptIds", "deptName");
        fields.add(ErpProductMapper.KEYWORD_FIELD_UNIVERSAL);
        addKeywordFieldIfVisible(fields, hiddenFieldSet, ErpProductMapper.KEYWORD_FIELD_CREATE_TIME, "createTime");
        return fields;
    }

    private void addKeywordFieldIfVisible(Set<String> fields, Set<String> hiddenFieldSet,
                                          String keywordField, String... permissionFields) {
        for (String permissionField : permissionFields) {
            if (isFieldHidden(hiddenFieldSet, permissionField)) {
                return;
            }
        }
        fields.add(keywordField);
    }

    private List<String> buildCustomKeywordSearchColumns(Set<String> hiddenFieldSet) {
        return getCustomFieldConfigs().stream()
                .filter(config -> Boolean.TRUE.equals(config.getVisible()))
                .filter(config -> isTextCustomField(config.getFieldType()))
                .filter(config -> !isFieldHidden(hiddenFieldSet, config.getFieldName()))
                .map(ErpFieldConfigDO::getPhysicalColumn)
                .filter(StringUtils::hasText)
                .filter(this::isSafeCustomKeywordColumn)
                .collect(Collectors.toList());
    }

    private boolean isTextCustomField(String fieldType) {
        return "TEXT".equals(fieldType) || "TEXTAREA".equals(fieldType);
    }

    private boolean isSafeCustomKeywordColumn(String column) {
        return column != null && column.matches("ext_[a-z0-9_]+");
    }

    private void fillCustomFields(List<ErpProductRespVO> list, Set<Long> productIds) {
        List<ErpFieldConfigDO> customFields = getCustomFieldConfigs();
        if (CollUtil.isEmpty(customFields) || CollUtil.isEmpty(productIds)) {
            return;
        }
        List<String> columns = customFields.stream()
                .map(ErpFieldConfigDO::getPhysicalColumn)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(columns)) {
            return;
        }
        Map<String, ErpFieldConfigDO> configByColumn = customFields.stream()
                .collect(Collectors.toMap(ErpFieldConfigDO::getPhysicalColumn, item -> item, (a, b) -> a));
        Map<Long, ErpProductRespVO> voMap = convertMap(list, ErpProductRespVO::getId);
        for (Map<String, Object> row : productMapper.selectCustomFieldMaps(productIds, columns)) {
            Long productId = ((Number) row.get("id")).longValue();
            ErpProductRespVO vo = voMap.get(productId);
            if (vo == null) {
                continue;
            }
            Map<String, Object> values = new LinkedHashMap<>();
            for (String column : columns) {
                ErpFieldConfigDO config = configByColumn.get(column);
                if (config != null) {
                    values.put(config.getFieldName(), normalizeCustomFieldValue(row.get(column)));
                }
            }
            vo.setCustomFields(values);
        }
    }

    private Object normalizeCustomFieldValue(Object value) {
        if (value instanceof Boolean) {
            return value;
        }
        return value;
    }

    private void saveProductCustomFields(Long productId, ProductSaveReqVO reqVO, Set<String> hiddenFieldSet,
                                         boolean validateRequired) {
        if (productId == null || reqVO == null) {
            return;
        }
        Map<String, Object> requestValues = reqVO.getCustomFields() == null
                ? Collections.emptyMap() : reqVO.getCustomFields();
        List<ErpFieldConfigDO> customFields = getCustomFieldConfigs();
        if (CollUtil.isEmpty(customFields)) {
            return;
        }
        Map<String, Object> updateValues = new LinkedHashMap<>();
        for (ErpFieldConfigDO config : customFields) {
            if (Boolean.TRUE.equals(config.getReadonly()) || !StringUtils.hasText(config.getPhysicalColumn())
                    || isFieldHidden(hiddenFieldSet, config.getFieldName())) {
                continue;
            }
            boolean containsKey = requestValues.containsKey(config.getFieldName());
            if (Boolean.TRUE.equals(config.getRequired()) && Boolean.TRUE.equals(config.getVisible())
                    && ((validateRequired && !containsKey) || (containsKey && requestValues.get(config.getFieldName()) == null))) {
                throw exception(FIELD_CONFIG_FIELD_NAME_EMPTY);
            }
            if (!containsKey) {
                continue;
            }
            Object value = requestValues.get(config.getFieldName());
            updateValues.put(config.getPhysicalColumn(), convertCustomFieldValue(value, config));
        }
        if (!updateValues.isEmpty()) {
            productMapper.updateCustomFields(productId, updateValues);
        }
    }

    private Object convertCustomFieldValue(Object value, ErpFieldConfigDO config) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        String type = config.getFieldType();
        if ("INTEGER".equals(type)) {
            return Long.valueOf(String.valueOf(value));
        }
        if ("DECIMAL".equals(type)) {
            return new BigDecimal(String.valueOf(value));
        }
        if ("BOOLEAN".equals(type)) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.valueOf(String.valueOf(value));
        }
        if ("DATE".equals(type)) {
            return value instanceof LocalDate ? value : LocalDate.parse(String.valueOf(value));
        }
        if ("DATETIME".equals(type)) {
            return value instanceof LocalDateTime ? value : LocalDateTime.parse(String.valueOf(value));
        }
        return String.valueOf(value);
    }

    private List<ErpFieldConfigDO> getCustomFieldConfigs() {
        return fieldConfigService.getFieldConfigListByModule(FIELD_PERMISSION_MODULE).stream()
                .filter(config -> "CUSTOM".equals(config.getFieldSource()))
                .filter(config -> StringUtils.hasText(config.getPhysicalColumn()))
                .collect(Collectors.toList());
    }

    private Map<Long, List<Long>> getProductDeptMap(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        return productDeptMapper.selectListByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(ErpProductDeptDO::getProductId,
                        Collectors.mapping(ErpProductDeptDO::getDeptId, Collectors.toList())));
    }

    private Map<Long, List<Long>> getProductOpenDeptMap(Collection<Long> productIds) {
        return getProductOpenDeptMap(productIds, null);
    }

    private Map<Long, List<Long>> getProductOpenDeptMap(Collection<Long> productIds,
                                                        Collection<Long> warehouseIdFilter) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        if (warehouseIdFilter != null && CollUtil.isEmpty(warehouseIdFilter)) {
            return Collections.emptyMap();
        }
        List<ErpStockDO> stocks = DataPermissionUtils.executeIgnore(() ->
                stockMapper.selectListByProductIds(productIds));
        if (warehouseIdFilter != null) {
            Set<Long> allowedWarehouseIds = new HashSet<>(warehouseIdFilter);
            stocks = stocks.stream()
                    .filter(stock -> allowedWarehouseIds.contains(stock.getWarehouseId()))
                    .collect(Collectors.toList());
        }
        if (CollUtil.isEmpty(stocks)) {
            return Collections.emptyMap();
        }
        Set<Long> warehouseIds = stocks.stream()
                .map(ErpStockDO::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(warehouseIds)
                ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds));
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (ErpStockDO stock : stocks) {
            if (stock.getProductId() == null || stock.getWarehouseId() == null) {
                continue;
            }
            ErpWarehouseDO warehouse = warehouseMap.get(stock.getWarehouseId());
            if (warehouse == null || warehouse.getDeptId() == null) {
                continue;
            }
            List<Long> deptIds = result.computeIfAbsent(stock.getProductId(), key -> new ArrayList<>());
            if (!deptIds.contains(warehouse.getDeptId())) {
                deptIds.add(warehouse.getDeptId());
            }
        }
        return result;
    }

    private List<Long> buildProductDeptIds(Long deptId, Collection<Long> deptIds, Long fallbackDeptId) {
        Set<Long> ids = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(deptIds)) {
            deptIds.stream().filter(Objects::nonNull).forEach(ids::add);
        }
        if (deptId != null) {
            ids.add(deptId);
        }
        if (ids.isEmpty() && fallbackDeptId != null) {
            ids.add(fallbackDeptId);
        }
        List<Long> result = new ArrayList<>(ids);
        if (CollUtil.isNotEmpty(result)) {
            deptApi.validateDeptList(result);
        }
        return result;
    }

    private void syncProductDeptList(Long productId, Collection<Long> deptIds) {
        productDeptMapper.deleteByProductId(productId);
        if (CollUtil.isEmpty(deptIds)) {
            return;
        }
        productDeptMapper.insertBatch(deptIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(deptId -> {
                    ErpProductDeptDO dept = new ErpProductDeptDO();
                    dept.setProductId(productId);
                    dept.setDeptId(deptId);
                    return dept;
                })
                .collect(Collectors.toList()));
    }

    private void syncProductOpenDeptByStockDistribution(Long productId) {
        if (productId == null) {
            return;
        }
        List<ErpStockDO> stocks = DataPermissionUtils.executeIgnore(() ->
                stockMapper.selectListByProductId(productId));
        Set<Long> warehouseIds = stocks.stream()
                .map(ErpStockDO::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(warehouseIds)) {
            productMapper.update(null, new LambdaUpdateWrapper<ErpProductDO>()
                    .eq(ErpProductDO::getId, productId)
                    .set(ErpProductDO::getDeptId, null));
            syncProductDeptList(productId, Collections.emptyList());
            return;
        }
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(warehouseIds));
        List<Long> deptIds = warehouseIds.stream()
                .map(warehouseMap::get)
                .filter(Objects::nonNull)
                .map(ErpWarehouseDO::getDeptId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        productMapper.update(null, new LambdaUpdateWrapper<ErpProductDO>()
                .eq(ErpProductDO::getId, productId)
                .set(ErpProductDO::getDeptId, CollUtil.isEmpty(deptIds) ? null : deptIds.get(0)));
        syncProductDeptList(productId, deptIds);
    }

    private String formatDeptNames(Collection<Long> deptIds, Map<Long, DeptRespDTO> deptMap) {
        if (CollUtil.isEmpty(deptIds)) {
            return null;
        }
        return deptIds.stream()
                .filter(Objects::nonNull)
                .map(deptId -> buildDeptFullName(deptId, deptMap))
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining("、"));
    }

    private Map<Long, DeptRespDTO> loadDeptWithParents(Collection<Long> deptIds) {
        Map<Long, DeptRespDTO> result = new LinkedHashMap<>();
        Set<Long> pendingIds = deptIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        while (CollUtil.isNotEmpty(pendingIds)) {
            Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(pendingIds);
            pendingIds = new LinkedHashSet<>();
            if (CollUtil.isEmpty(deptMap)) {
                break;
            }
            for (DeptRespDTO dept : deptMap.values()) {
                if (dept == null || dept.getId() == null || result.containsKey(dept.getId())) {
                    continue;
                }
                result.put(dept.getId(), dept);
                Long parentId = dept.getParentId();
                if (parentId != null && parentId > 0 && !result.containsKey(parentId)) {
                    pendingIds.add(parentId);
                }
            }
        }
        return result;
    }

    private static String buildDeptFullName(Long deptId, Map<Long, DeptRespDTO> deptMap) {
        if (deptId == null || CollUtil.isEmpty(deptMap) || !deptMap.containsKey(deptId)) {
            return null;
        }
        List<String> names = new ArrayList<>();
        Set<Long> visitedIds = new LinkedHashSet<>();
        Long currentId = deptId;
        while (currentId != null && visitedIds.add(currentId)) {
            DeptRespDTO current = deptMap.get(currentId);
            if (current == null) {
                break;
            }
            if (StringUtils.hasText(current.getName())) {
                names.add(0, current.getName());
            }
            Long parentId = current.getParentId();
            if (parentId == null || parentId <= 0) {
                break;
            }
            currentId = parentId;
        }
        return CollUtil.isEmpty(names) ? null : String.join(" / ", names);
    }

    private void ignoreReadonlyProductSaveFields(ProductSaveReqVO reqVO) {
        reqVO.setLastPurchasePrice(null);
    }

    private void applyProductSaveFieldPermissions(ProductSaveReqVO reqVO, ErpProductDO existing) {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
        if (isFieldHidden(hiddenFieldSet, "code")) {
            reqVO.setCode(existing == null ? null : existing.getCode());
        }
        if (isFieldHidden(hiddenFieldSet, "deptId") || isFieldHidden(hiddenFieldSet, "deptIds")) {
            reqVO.setDeptId(existing == null ? null : existing.getDeptId());
            reqVO.setDeptIds(null);
        }
        if (isFieldHidden(hiddenFieldSet, "name")) {
            reqVO.setName(existing == null ? null : existing.getName());
        }
        if (isFieldHidden(hiddenFieldSet, "unitId")) {
            reqVO.setUnitId(existing == null ? null : existing.getUnitId());
        }
        if (isFieldHidden(hiddenFieldSet, "defaultWarehouseId")) {
            reqVO.setDefaultWarehouseId(existing == null ? null : existing.getDefaultWarehouseId());
        }
        if (isFieldHidden(hiddenFieldSet, "vehicleModel")) {
            reqVO.setVehicleModel(existing == null ? null : existing.getVehicleModel());
        }
        if (isFieldHidden(hiddenFieldSet, "standard")) {
            reqVO.setStandard(existing == null ? null : existing.getStandard());
        }
        if (isFieldHidden(hiddenFieldSet, "categoryId")) {
            reqVO.setCategoryId(existing == null ? null : existing.getCategoryId());
        }
        if (isFieldHidden(hiddenFieldSet, "batchNoEnabled")) {
            reqVO.setBatchNoEnabled(existing == null ? null : existing.getBatchNoEnabled());
        }
        if (isFieldHidden(hiddenFieldSet, "barCode")) {
            reqVO.setBarCode(existing == null ? null : existing.getBarCode());
        }
        if (isFieldHidden(hiddenFieldSet, "factoryCode")) {
            reqVO.setFactoryCode(existing == null ? null : existing.getFactoryCode());
        }
        if (isFieldHidden(hiddenFieldSet, "status")) {
            reqVO.setStatus(existing == null ? null : existing.getStatus());
        }
        if (isFieldHidden(hiddenFieldSet, "remark")) {
            reqVO.setRemark(existing == null ? null : existing.getRemark());
        }
        if (isFieldHidden(hiddenFieldSet, "purchasePrice")) {
            reqVO.setPurchasePrice(existing == null ? null : existing.getPurchasePrice());
        }
        if (isFieldHidden(hiddenFieldSet, "salePrice")) {
            reqVO.setSalePrice(existing == null ? null : existing.getSalePrice());
        }
        if (isFieldHidden(hiddenFieldSet, "minPrice")) {
            reqVO.setMinPrice(existing == null ? null : existing.getMinPrice());
        }
        if (isFieldHidden(hiddenFieldSet, "referencePrice")) {
            reqVO.setReferencePrice(existing == null ? null : existing.getReferencePrice());
        }
        if (isFieldHidden(hiddenFieldSet, "retailPrice")) {
            reqVO.setRetailPrice(existing == null ? null : existing.getRetailPrice());
        }
        if (isFieldHidden(hiddenFieldSet, "grossProfitRate")) {
            reqVO.setGrossProfitRate(existing == null ? null : existing.getGrossProfitRate());
        }
        if (isFieldHidden(hiddenFieldSet, "backupPrice1")) {
            reqVO.setBackupPrice1(existing == null ? null : existing.getBackupPrice1());
        }
        if (isFieldHidden(hiddenFieldSet, "wholesalePrice")) {
            reqVO.setWholesalePrice(existing == null ? null : existing.getWholesalePrice());
        }
        if (isFieldHidden(hiddenFieldSet, "sharePrice")) {
            reqVO.setSharePrice(existing == null ? null : existing.getSharePrice());
        }
        if (isFieldHidden(hiddenFieldSet, "weight")) {
            reqVO.setWeight(existing == null ? null : existing.getWeight());
        }
        if (isFieldHidden(hiddenFieldSet, "stockMax")) {
            reqVO.setStockMax(existing == null ? null : existing.getStockMax());
        }
        if (isFieldHidden(hiddenFieldSet, "stockMin")) {
            reqVO.setStockMin(existing == null ? null : existing.getStockMin());
        }
        if (isFieldHidden(hiddenFieldSet, "stockStandard")) {
            reqVO.setStockStandard(existing == null ? null : existing.getStockStandard());
        }
        if (isFieldHidden(hiddenFieldSet, "packageQty")) {
            reqVO.setPackageQty(existing == null ? null : existing.getPackageQty());
        }
    }

    @Override
    public Long getProductCountByCategoryId(Long categoryId) {
        return productMapper.selectCountByCategoryId(categoryId);
    }

    @Override
    public Long getProductCountByUnitId(Long unitId) {
        return productMapper.selectCountByUnitId(unitId);
    }

    @Override
    public void updateProductLastPurchasePrice(Long productId, BigDecimal lastPurchasePrice) {
        if (productId == null || lastPurchasePrice == null) {
            return;
        }
        ErpProductDO update = new ErpProductDO();
        update.setId(productId);
        update.setLastPurchasePrice(lastPurchasePrice);
        productMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductsShelf(Collection<Long> productIds, String shelf) {
        if (CollUtil.isEmpty(productIds)) {
            return;
        }
        List<Long> ids = productIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        validateManageableProductIds(ids);
        Map<Long, ProductLogSnapshot> beforeMap = loadProductLogSnapshotMap(ids);
        productMapper.updateShelfByIds(ids, shelf);
        Map<Long, ProductLogSnapshot> afterMap = loadProductLogSnapshotMap(ids);
        for (Long id : ids) {
            ProductLogSnapshot before = beforeMap.get(id);
            ProductLogSnapshot after = afterMap.get(id);
            if (before != null && after != null) {
                recordProductLog(ERP_UPDATE_SUB_TYPE, id,
                        buildProductLogAction("批量修改配件货架位置", before, after));
            }
        }
    }

    @Override
    public List<Long> findDuplicateShelfProductIds() {
        List<String> duplicateShelfs = productMapper.selectDuplicateShelfValues();
        if (CollUtil.isEmpty(duplicateShelfs)) {
            return Collections.emptyList();
        }
        return productMapper.selectIdsByShelfIn(duplicateShelfs);
    }

    @Override
    public List<Long> findEmptyShelfProductIds() {
        return productMapper.selectIdsByEmptyShelf();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpProductImportRespVO importProductList(List<ErpProductImportExcelVO> list) {
        ErpProductImportRespVO respVO = new ErpProductImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        Map<String, ErpProductCategoryDO> categoryMap = buildCategoryNameMap();
        Map<String, ErpProductUnitDO> unitMap = buildUnitNameMap();
        Map<String, ErpWarehouseDO> warehouseMap = buildWarehouseNameMap();
        Map<String, ErpProductDO> existedMap = buildExistedProductMap(list);

        for (int i = 0; i < list.size(); i++) {
            ErpProductImportExcelVO row = list.get(i);
            if (isEmptyRow(row)) {
                continue;
            }
            Integer rowNo = i + 2;
            String code = trimToNull(row.getCode());
            try {
                ProductSaveReqVO saveReqVO = buildSaveReqVO(row, categoryMap, unitMap, warehouseMap);
                if (StringUtils.hasText(code) && existedMap.containsKey(code)) {
                    saveReqVO.setId(existedMap.get(code).getId());
                    updateProduct(saveReqVO);
                    respVO.setUpdateCount(respVO.getUpdateCount() + 1);
                } else {
                    createProduct(saveReqVO);
                    respVO.setCreateCount(respVO.getCreateCount() + 1);
                }
            } catch (Exception ex) {
                respVO.getFailureDetails().add(new ErpProductImportRespVO.FailureItem(
                        rowNo, code, getImportFailureReason(ex)));
            }
        }

        respVO.setSuccessCount(respVO.getCreateCount() + respVO.getUpdateCount());
        respVO.setFailureCount(respVO.getFailureDetails().size());
        operateLogService.record(ERP_PRODUCT_TYPE, ERP_IMPORT_SUB_TYPE, 0L,
                "导入配件信息，新增：" + respVO.getCreateCount()
                        + "，更新：" + respVO.getUpdateCount()
                        + "，失败：" + respVO.getFailureCount(),
                "产品导入");
        return respVO;
    }

    public List<ErpProductImportExcelVO> parseCsvImport(Reader reader) {
        CsvReadConfig config = CsvReadConfig.defaultConfig().setContainsHeader(true);
        CsvReader csvReader = CsvUtil.getReader(config);
        CsvData csvData = csvReader.read(reader);
        List<ErpProductImportExcelVO> result = new ArrayList<>();
        for (CsvRow row : csvData.getRows()) {
            ErpProductImportExcelVO item = new ErpProductImportExcelVO();
            item.setCode(trimToNull(readCsvValue(row, "配件编码", "产品编码")));
            item.setName(trimToNull(readCsvValue(row, "产品名称")));
            item.setBarCode(trimToNull(readCsvValue(row, "产品条码")));
            item.setCategoryName(trimToNull(readCsvValue(row, "商品分类", "产品分类", "分类")));
            item.setBatchNoEnabled(parseBoolean(readCsvValue(row, "开启批次号", "是否开启批次号管理", "批次号管理")));
            item.setUnitName(trimToNull(readCsvValue(row, "单位")));
            item.setStatus(parseInteger(readCsvValue(row, "状态")));
            item.setDefaultWarehouseName(trimToNull(readCsvValue(row, "默认仓库")));
            item.setVehicleModel(trimToNull(readCsvValue(row, "适用车型")));
            item.setFactoryCode(trimToNull(readCsvValue(row, "厂家编码")));
            item.setPurchasePrice(parseBigDecimal(readCsvValue(row, "采购价格")));
            item.setSalePrice(parseBigDecimal(readCsvValue(row, "销售价格")));
            item.setMinPrice(parseBigDecimal(readCsvValue(row, "最低价格")));
            item.setStandard(trimToNull(readCsvValue(row, "规格")));
            item.setRemark(trimToNull(readCsvValue(row, "备注")));
            item.setExpiryDay(parseInteger(readCsvValue(row, "保质期天数")));
            item.setWeight(parseBigDecimal(readCsvValue(row, "重量")));
            item.setReferencePrice(parseBigDecimal(readCsvValue(row, "参考价")));
            item.setRetailPrice(parseBigDecimal(readCsvValue(row, "零售价")));
            item.setLastPurchasePrice(parseBigDecimal(readCsvValue(row, "最后一次采购入库价")));
            item.setGrossProfitRate(parseInteger(readCsvValue(row, "毛利率")));
            item.setBackupPrice1(parseBigDecimal(readCsvValue(row, "备用价1")));
            item.setWholesalePrice(parseBigDecimal(readCsvValue(row, "批发价")));
            item.setStockMax(parseInteger(readCsvValue(row, "库存上限")));
            item.setStockMin(parseInteger(readCsvValue(row, "库存下限")));
            item.setStockStandard(parseInteger(readCsvValue(row, "标准库存")));
            item.setPackageQty(parseInteger(readCsvValue(row, "包装数")));
            item.setMainImage(trimToNull(readCsvValue(row, "主图URL")));
            item.setDetailContent(trimToNull(readCsvValue(row, "详情内容")));
            item.setSharePrice(parseBigDecimal(readCsvValue(row, "股份价")));
            result.add(item);
        }
        return result;
    }

    private Map<String, ErpProductCategoryDO> buildCategoryNameMap() {
        return productCategoryService.getProductCategoryList(new ErpProductCategoryListReqVO()).stream()
                .filter(item -> StringUtils.hasText(item.getName()))
                .collect(Collectors.toMap(ErpProductCategoryDO::getName, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, ErpProductUnitDO> buildUnitNameMap() {
        return productUnitService.getProductUnitListByStatus(CommonStatusEnum.ENABLE.getStatus()).stream()
                .filter(item -> StringUtils.hasText(item.getName()))
                .collect(Collectors.toMap(ErpProductUnitDO::getName, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, ErpWarehouseDO> buildWarehouseNameMap() {
        return warehouseService.getWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()).stream()
                .filter(item -> StringUtils.hasText(item.getName()))
                .collect(Collectors.toMap(ErpWarehouseDO::getName, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, ErpProductDO> buildExistedProductMap(List<ErpProductImportExcelVO> list) {
        List<String> codes = list.stream()
                .map(ErpProductImportExcelVO::getCode)
                .map(this::trimToNull)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(codes)) {
            return new HashMap<>();
        }
        return productMapper.selectListByCodes(codes).stream()
                .collect(Collectors.toMap(ErpProductDO::getCode, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private ProductSaveReqVO buildSaveReqVO(ErpProductImportExcelVO row,
                                            Map<String, ErpProductCategoryDO> categoryMap,
                                            Map<String, ErpProductUnitDO> unitMap,
                                            Map<String, ErpWarehouseDO> warehouseMap) {
        ProductSaveReqVO reqVO = new ProductSaveReqVO();
        reqVO.setCode(trimToNull(row.getCode()));
        reqVO.setName(trimToNull(row.getName()));
        reqVO.setBarCode(trimToNull(row.getBarCode()));
        reqVO.setCategoryId(resolveCategoryId(row.getCategoryName(), categoryMap));
        reqVO.setBatchNoEnabled(Boolean.TRUE.equals(row.getBatchNoEnabled()));
        reqVO.setUnitId(resolveUnitId(row.getUnitName(), unitMap));
        reqVO.setDefaultWarehouseId(resolveWarehouseIdIfPresent(row.getDefaultWarehouseName(), warehouseMap));
        reqVO.setStatus(row.getStatus() == null ? CommonStatusEnum.ENABLE.getStatus() : row.getStatus());
        reqVO.setVehicleModel(trimToNull(row.getVehicleModel()));
        reqVO.setFactoryCode(trimToNull(row.getFactoryCode()));
        reqVO.setPurchasePrice(row.getPurchasePrice());
        reqVO.setSalePrice(row.getSalePrice());
        reqVO.setMinPrice(row.getMinPrice());
        reqVO.setStandard(trimToNull(row.getStandard()));
        reqVO.setRemark(trimToNull(row.getRemark()));
        reqVO.setExpiryDay(row.getExpiryDay());
        reqVO.setWeight(row.getWeight());
        reqVO.setReferencePrice(row.getReferencePrice());
        reqVO.setRetailPrice(row.getRetailPrice());
        reqVO.setGrossProfitRate(row.getGrossProfitRate());
        reqVO.setBackupPrice1(row.getBackupPrice1());
        reqVO.setWholesalePrice(row.getWholesalePrice());
        reqVO.setSharePrice(row.getSharePrice());
        reqVO.setStockMax(row.getStockMax());
        reqVO.setStockMin(row.getStockMin());
        reqVO.setStockStandard(row.getStockStandard());
        reqVO.setPackageQty(row.getPackageQty());
        reqVO.setMainImage(trimToNull(row.getMainImage()));
        reqVO.setDetailContent(trimToNull(row.getDetailContent()));
        ValidationUtils.validate(reqVO);
        return reqVO;
    }

    private Long resolveCategoryId(String categoryName, Map<String, ErpProductCategoryDO> categoryMap) {
        ErpProductCategoryDO category = categoryMap.get(trimToNull(categoryName));
        if (category == null) {
            throw new IllegalArgumentException("商品分类不存在：" + categoryName);
        }
        return category.getId();
    }

    private Long resolveUnitId(String unitName, Map<String, ErpProductUnitDO> unitMap) {
        ErpProductUnitDO unit = unitMap.get(trimToNull(unitName));
        if (unit == null) {
            throw new IllegalArgumentException("单位不存在：" + unitName);
        }
        return unit.getId();
    }

    private Long resolveWarehouseIdIfPresent(String warehouseName, Map<String, ErpWarehouseDO> warehouseMap) {
        String normalizedName = trimToNull(warehouseName);
        if (!StringUtils.hasText(normalizedName)) {
            return null;
        }
        ErpWarehouseDO warehouse = warehouseMap.get(normalizedName);
        if (warehouse == null) {
            throw new IllegalArgumentException("默认仓库不存在：" + warehouseName);
        }
        return warehouse.getId();
    }

    private String getImportFailureReason(Exception ex) {
        if (ex instanceof ConstraintViolationException) {
            return ex.getMessage();
        }
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    private boolean isEmptyRow(ErpProductImportExcelVO row) {
        return row == null
                || (!StringUtils.hasText(trimToNull(row.getCode()))
                && !StringUtils.hasText(trimToNull(row.getName()))
                && !StringUtils.hasText(trimToNull(row.getBarCode())));
    }

    private String getCsvValue(CsvRow row, int index) {
        return row.size() > index ? row.get(index) : null;
    }

    private String readCsvValue(CsvRow row, String header) {
        return row.getByName(header);
    }

    private String readCsvValue(CsvRow row, String header, String fallbackHeader) {
        String value = readCsvValue(row, header);
        return value != null ? value : readCsvValue(row, fallbackHeader);
    }

    private String readCsvValue(CsvRow row, String header, String fallbackHeader, String secondFallbackHeader) {
        String value = readCsvValue(row, header, fallbackHeader);
        return value != null ? value : readCsvValue(row, secondFallbackHeader);
    }

    private String trimToNull(String value) {
        return StrUtil.emptyToNull(StrUtil.trim(value));
    }

    private Integer parseInteger(String value) {
        String trim = trimToNull(value);
        if (trim == null) {
            return null;
        }
        return Integer.valueOf(trim);
    }

    private BigDecimal parseBigDecimal(String value) {
        String trim = trimToNull(value);
        if (trim == null) {
            return null;
        }
        return new BigDecimal(trim);
    }

    private Boolean parseBoolean(String value) {
        String trim = trimToNull(value);
        if (trim == null) {
            return null;
        }
        if ("是".equals(trim) || "启用".equals(trim) || "开启".equals(trim) || "1".equals(trim)
                || "true".equalsIgnoreCase(trim) || "yes".equalsIgnoreCase(trim)) {
            return true;
        }
        if ("否".equals(trim) || "禁用".equals(trim) || "关闭".equals(trim) || "0".equals(trim)
                || "false".equalsIgnoreCase(trim) || "no".equalsIgnoreCase(trim)) {
            return false;
        }
        throw new IllegalArgumentException("开启批次号只能填写是/否");
    }

    // ========== 配件价格调整 ==========

    private static final String PARTS_ADJUST_PASSWORD_CONFIG_KEY = "erp.parts.adjustPassword";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdatePriceFields(
            List<cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchUpdatePriceFieldsReqVO> reqList) {
        if (CollUtil.isEmpty(reqList)) {
            return;
        }
        validateManageableProductIds(reqList.stream()
                .map(cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchUpdatePriceFieldsReqVO::getId)
                .collect(Collectors.toList()));
        Set<String> hiddenFieldSet = getHiddenFieldSet();
        for (cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchUpdatePriceFieldsReqVO req : reqList) {
            validateBatchWritableField(hiddenFieldSet, "backupPrice1", "备用价1", req.getBackupPrice1());
            validateBatchWritableField(hiddenFieldSet, "referencePrice", "参考价", req.getReferencePrice());
            validateBatchWritableField(hiddenFieldSet, "retailPrice", "零售价", req.getRetailPrice());
            validateBatchWritableField(hiddenFieldSet, "wholesalePrice", "批发价", req.getWholesalePrice());
            validateBatchWritableField(hiddenFieldSet, "sharePrice", "股份价", req.getSharePrice());
            validateBatchWritableField(hiddenFieldSet, "stockMax", "库存上限", req.getStockMax());
            validateBatchWritableField(hiddenFieldSet, "stockMin", "库存下限", req.getStockMin());
            validateBatchWritableField(hiddenFieldSet, "stockStandard", "标准库存", req.getStockStandard());
            ProductLogSnapshot before = loadProductLogSnapshot(req.getId());
            ErpProductDO update = new ErpProductDO();
            update.setId(req.getId());
            if (req.getBackupPrice1() != null) update.setBackupPrice1(req.getBackupPrice1());
            if (req.getReferencePrice() != null) update.setReferencePrice(req.getReferencePrice());
            if (req.getRetailPrice() != null) update.setRetailPrice(req.getRetailPrice());
            if (req.getWholesalePrice() != null) update.setWholesalePrice(req.getWholesalePrice());
            if (req.getSharePrice() != null) update.setSharePrice(req.getSharePrice());
            if (req.getStockMax() != null) update.setStockMax(req.getStockMax());
            if (req.getStockMin() != null) update.setStockMin(req.getStockMin());
            if (req.getStockStandard() != null) update.setStockStandard(req.getStockStandard());
            productMapper.updateById(update);
            ProductLogSnapshot after = loadProductLogSnapshot(req.getId());
            recordProductLog(ERP_UPDATE_SUB_TYPE, req.getId(),
                    buildProductLogAction("列表编辑配件价格/库存", before, after));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAdjustPrice(
            cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchAdjustPriceReqVO reqVO) {
        Set<String> hiddenFieldSet = getHiddenFieldSet();
        validatePriceTypeVisible(hiddenFieldSet, reqVO.getSourcePriceType());
        validatePriceTypeVisible(hiddenFieldSet, reqVO.getTargetPriceType());
        // 1. 按筛选条件查询配件 ID
        List<Long> productIds = selectProductIdsByFilter(reqVO.getFilterCondition());
        if (CollUtil.isEmpty(productIds)) {
            return 0;
        }

        // 2. 分批查询配件，逐条计算新价格并更新
        int adjusted = 0;
        // 每批最多 200 条
        List<List<Long>> batches = CollUtil.split(productIds, 200);
        for (List<Long> batch : batches) {
            List<ErpProductDO> products = productMapper.selectByIds(batch);
            for (ErpProductDO product : products) {
                ProductLogSnapshot before = loadProductLogSnapshot(product.getId());
                BigDecimal sourcePrice = getSourcePrice(product, reqVO.getSourcePriceType());
                if (sourcePrice == null) {
                    continue;
                }
                BigDecimal newPrice = calcAdjustedPrice(sourcePrice, reqVO.getAdjustMethod(),
                        reqVO.getAdjustCoefficient(), reqVO.getDecimalPlaces());
                if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
                    continue;
                }
                setTargetPrice(product, reqVO.getTargetPriceType(), newPrice);
                productMapper.updateById(product);
                ProductLogSnapshot after = loadProductLogSnapshot(product.getId());
                recordProductLog(ERP_UPDATE_SUB_TYPE, product.getId(),
                        limitLogAction(buildProductLogAction("批量调整配件价格", before, after)
                                + "；调价规则：" + buildPriceAdjustRule(reqVO)));
                adjusted++;
            }
        }
        return adjusted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAdjustStockLimits(
            cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsBatchAdjustStockLimitsReqVO reqVO) {
        // 1. 口令校验
        validateAdjustPassword(reqVO.getPassword());

        // 2. 至少有一项需要更新
        if (reqVO.getStockMax() == null && reqVO.getStockMin() == null && reqVO.getStockStandard() == null) {
            return 0;
        }

        // 3. 按筛选条件查询配件 ID
        List<Long> productIds = selectProductIdsByFilter(reqVO.getFilterCondition());
        if (CollUtil.isEmpty(productIds)) {
            return 0;
        }

        // 4. 分批更新
        int updated = 0;
        List<List<Long>> batches = CollUtil.split(productIds, 200);
        for (List<Long> batch : batches) {
            for (Long id : batch) {
                ProductLogSnapshot before = loadProductLogSnapshot(id);
                ErpProductDO update = new ErpProductDO();
                update.setId(id);
                if (reqVO.getStockMax() != null) update.setStockMax(reqVO.getStockMax());
                if (reqVO.getStockMin() != null) update.setStockMin(reqVO.getStockMin());
                if (reqVO.getStockStandard() != null) update.setStockStandard(reqVO.getStockStandard());
                productMapper.updateById(update);
                ProductLogSnapshot after = loadProductLogSnapshot(id);
                recordProductLog(ERP_UPDATE_SUB_TYPE, id,
                        buildProductLogAction("批量调整配件库存上下限", before, after));
                updated++;
            }
        }
        return updated;
    }

    private void validateAdjustPassword(String password) {
        String configPassword = configApi.getConfigValueByKey(PARTS_ADJUST_PASSWORD_CONFIG_KEY);
        if (!StringUtils.hasText(configPassword)) {
            throw exception(
                    new cn.iocoder.yudao.framework.common.exception.ErrorCode(1_030_507_000, "调整口令未配置，请联系管理员"));
        }
        if (!configPassword.equals(password)) {
            throw exception(
                    new cn.iocoder.yudao.framework.common.exception.ErrorCode(1_030_507_001, "调整口令错误，请重新输入"));
        }
    }

    private List<Long> selectProductIdsByFilter(
            cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpPartsPriceAdjustFilterVO filter) {
        LambdaQueryWrapper<ErpProductDO> w = new LambdaQueryWrapper<>();
        w.select(ErpProductDO::getId);
        w.ne(ErpProductDO::getMergedFlag, Boolean.TRUE);
        ErpProductMapper.applyVisibleScope(w, buildCurrentUserVisibleReqVO());
        if (filter == null) {
            List<Map<String, Object>> rows = DataPermissionUtils.executeIgnore(() -> productMapper.selectMaps(w));
            return rows.stream().map(m -> (Long) m.get("id")).collect(Collectors.toList());
        }
        if (StringUtils.hasText(filter.getCode())) {
            w.like(ErpProductDO::getCode, filter.getCode().trim());
        }
        if (StringUtils.hasText(filter.getName())) {
            w.like(ErpProductDO::getName, filter.getName().trim());
        }
        if (filter.getCategoryId() != null) {
            w.eq(ErpProductDO::getCategoryId, filter.getCategoryId());
        }
        if (StringUtils.hasText(filter.getVehicleModel())) {
            w.like(ErpProductDO::getVehicleModel, filter.getVehicleModel().trim());
        }
        if (StringUtils.hasText(filter.getOriginPlace())) {
            w.like(ErpProductDO::getOriginPlace, filter.getOriginPlace().trim());
        }
        if (StringUtils.hasText(filter.getBrand())) {
            w.like(ErpProductDO::getBrand, filter.getBrand().trim());
        }
        if (filter.getWarehouseId() != null) {
            w.eq(ErpProductDO::getDefaultWarehouseId, filter.getWarehouseId());
        }
        List<Map<String, Object>> rows = DataPermissionUtils.executeIgnore(() -> productMapper.selectMaps(w));
        return rows.stream().map(m -> (Long) m.get("id")).collect(Collectors.toList());
    }

    private BigDecimal getSourcePrice(ErpProductDO product, String sourcePriceType) {
        switch (sourcePriceType) {
            case "SPARE_PRICE_1": return product.getBackupPrice1();
            case "REFERENCE_PRICE": return product.getReferencePrice();
            case "RETAIL_PRICE": return product.getRetailPrice();
            case "WHOLESALE_PRICE": return product.getWholesalePrice();
            case "SHARE_PRICE": return product.getSharePrice();
            case "LAST_PURCHASE_PRICE": return product.getLastPurchasePrice();
            case "PURCHASE_PRICE": return product.getPurchasePrice();
            case "SALE_PRICE": return product.getSalePrice();
            default: return null;
        }
    }

    private void validateBatchWritableField(Set<String> hiddenFieldSet, String fieldKey,
                                            String fieldLabel, Object submittedValue) {
        if (submittedValue != null && isFieldHidden(hiddenFieldSet, fieldKey)) {
            throw exception(PRODUCT_FIELD_NO_PERMISSION, fieldLabel);
        }
    }

    private void validatePriceTypeVisible(Set<String> hiddenFieldSet, String priceType) {
        String fieldKey;
        switch (priceType) {
            case "SPARE_PRICE_1": fieldKey = "backupPrice1"; break;
            case "REFERENCE_PRICE": fieldKey = "referencePrice"; break;
            case "RETAIL_PRICE": fieldKey = "retailPrice"; break;
            case "WHOLESALE_PRICE": fieldKey = "wholesalePrice"; break;
            case "SHARE_PRICE": fieldKey = "sharePrice"; break;
            case "LAST_PURCHASE_PRICE": fieldKey = "lastPurchasePrice"; break;
            case "PURCHASE_PRICE": fieldKey = "purchasePrice"; break;
            case "SALE_PRICE": fieldKey = "salePrice"; break;
            default: return;
        }
        if (isFieldHidden(hiddenFieldSet, fieldKey)) {
            throw exception(PRODUCT_FIELD_NO_PERMISSION, fieldKey);
        }
    }

    private void setTargetPrice(ErpProductDO product, String targetPriceType, BigDecimal newPrice) {
        switch (targetPriceType) {
            case "SPARE_PRICE_1": product.setBackupPrice1(newPrice); break;
            case "REFERENCE_PRICE": product.setReferencePrice(newPrice); break;
            case "RETAIL_PRICE": product.setRetailPrice(newPrice); break;
            case "WHOLESALE_PRICE": product.setWholesalePrice(newPrice); break;
            case "SHARE_PRICE": product.setSharePrice(newPrice); break;
            case "PURCHASE_PRICE": product.setPurchasePrice(newPrice); break;
            case "SALE_PRICE": product.setSalePrice(newPrice); break;
            default: break;
        }
    }

    private BigDecimal calcAdjustedPrice(BigDecimal source, String method,
                                          BigDecimal coefficient, int decimalPlaces) {
        if (source == null || coefficient == null) {
            return null;
        }
        BigDecimal result;
        switch (method) {
            case "ADD": result = source.add(coefficient); break;
            case "SUBTRACT": result = source.subtract(coefficient); break;
            case "MULTIPLY": result = source.multiply(coefficient); break;
            case "DIVIDE":
                if (coefficient.compareTo(BigDecimal.ZERO) == 0) {
                    return null;
                }
                result = source.divide(coefficient, decimalPlaces + 4, java.math.RoundingMode.HALF_UP);
                break;
            default: return null;
        }
        return result.setScale(decimalPlaces, java.math.RoundingMode.HALF_UP);
    }

}
