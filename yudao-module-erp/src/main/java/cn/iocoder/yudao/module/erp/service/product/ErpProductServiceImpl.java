package cn.iocoder.yudao.module.erp.service.product;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ProductSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUniversalDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUniversalMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_CODE_GENERATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_MERGED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_UNIVERSAL_CODE_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_UNIVERSAL_CODE_SELF;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_WAREHOUSE_NOT_EXISTS;

/**
 * ERP 产品 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpProductServiceImpl implements ErpProductService {

    /**
     * 生成配件编码的最大重试次数，用于并发下 uk_code 冲突兜底
     */
    private static final int CODE_GENERATE_MAX_RETRY = 5;

    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpProductUniversalMapper productUniversalMapper;
    @Resource
    private ErpStockMapper stockMapper;

    @Resource
    private ErpProductCategoryService productCategoryService;
    @Resource
    private ErpProductUnitService productUnitService;
    @Resource
    private ErpWarehouseService warehouseService;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(ProductSaveReqVO createReqVO) {
        // 1. 校验仓库存在
        if (createReqVO.getDefaultWarehouseId() != null
                && warehouseService.getWarehouse(createReqVO.getDefaultWarehouseId()) == null) {
            throw exception(PRODUCT_WAREHOUSE_NOT_EXISTS);
        }

        // 2. 生成配件编码（带重试，防并发）并插入主表
        ErpProductDO product = BeanUtils.toBean(createReqVO, ErpProductDO.class);
        if (product.getMergedFlag() == null) {
            product.setMergedFlag(false);
        }
        if (product.getPackageQty() == null) {
            product.setPackageQty(1);
        }
        insertWithGeneratedCode(product);

        // 3. 校验并插入通用件子表
        validateUniversalCodes(product.getId(), createReqVO.getUniversals());
        saveUniversals(product.getId(), createReqVO.getUniversals());

        return product.getId();
    }

    private void insertWithGeneratedCode(ErpProductDO product) {
        for (int i = 0; i < CODE_GENERATE_MAX_RETRY; i++) {
            String code = noRedisDAO.generatePlain(ErpNoRedisDAO.PRODUCT_CODE_PREFIX);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductSaveReqVO updateReqVO) {
        // 1. 校验存在 & 未被合并
        ErpProductDO existing = validateProductExists(updateReqVO.getId());
        if (Boolean.TRUE.equals(existing.getMergedFlag())) {
            throw exception(PRODUCT_MERGED, existing.getName());
        }
        // 2. 校验仓库
        if (updateReqVO.getDefaultWarehouseId() != null
                && warehouseService.getWarehouse(updateReqVO.getDefaultWarehouseId()) == null) {
            throw exception(PRODUCT_WAREHOUSE_NOT_EXISTS);
        }

        // 3. 更新主表（code/mergedFlag/mergedTargetId/lastPurchasePrice 不允许通过此接口修改，保留原值）
        ErpProductDO updateObj = BeanUtils.toBean(updateReqVO, ErpProductDO.class);
        updateObj.setCode(null);
        updateObj.setMergedFlag(null);
        updateObj.setMergedTargetId(null);
        updateObj.setLastPurchasePrice(null);
        productMapper.updateById(updateObj);

        // 4. 子表：先删后插
        validateUniversalCodes(updateReqVO.getId(), updateReqVO.getUniversals());
        productUniversalMapper.deleteByProductId(updateReqVO.getId());
        saveUniversals(updateReqVO.getId(), updateReqVO.getUniversals());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        validateProductExists(id);
        productUniversalMapper.deleteByProductId(id);
        productMapper.deleteById(id);
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
        ErpProductDO product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        List<ErpProductRespVO> list = buildProductVOList(Collections.singletonList(product));
        return CollUtil.isEmpty(list) ? null : list.get(0);
    }

    @Override
    public List<ErpProductRespVO> getProductVOListByStatus(Integer status) {
        List<ErpProductDO> list = productMapper.selectListByStatus(status);
        return buildProductVOList(list);
    }

    @Override
    public List<ErpProductRespVO> getProductVOList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ErpProductDO> list = productMapper.selectByIds(ids);
        return buildProductVOList(list);
    }

    @Override
    public PageResult<ErpProductRespVO> getProductVOPage(ErpProductPageReqVO pageReqVO) {
        PageResult<ErpProductDO> pageResult = productMapper.selectPage(pageReqVO);
        return new PageResult<>(buildProductVOList(pageResult.getList()), pageResult.getTotal());
    }

    private List<ErpProductRespVO> buildProductVOList(List<ErpProductDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1. 批量查询关联数据
        Set<Long> productIds = convertSet(list, ErpProductDO::getId);
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
                : warehouseService.getWarehouseMap(warehouseIds);
        Map<Long, BigDecimal> stockMap = stockMapper.selectSumMapByProductIds(productIds);
        List<ErpProductUniversalDO> universalList = productUniversalMapper.selectListByProductIds(productIds);
        Map<Long, List<ErpProductUniversalDO>> universalByProduct = universalList.stream()
                .collect(Collectors.groupingBy(ErpProductUniversalDO::getProductId));

        // 2. 组装
        return BeanUtils.toBean(list, ErpProductRespVO.class, vo -> {
            MapUtils.findAndThen(categoryMap, vo.getCategoryId(),
                    c -> vo.setCategoryName(c.getName()));
            MapUtils.findAndThen(unitMap, vo.getUnitId(),
                    u -> vo.setUnitName(u.getName()));
            MapUtils.findAndThen(warehouseMap, vo.getDefaultWarehouseId(),
                    w -> vo.setDefaultWarehouseName(w.getName()));
            // 库存：currentStock 实时聚合；inTransit=0；available=current（按需求 Q4 共识）
            BigDecimal current = stockMap.getOrDefault(vo.getId(), BigDecimal.ZERO);
            vo.setCurrentStock(current);
            vo.setInTransitStock(BigDecimal.ZERO);
            vo.setAvailableStock(current);
            vo.setLowStockWarning(vo.getStockMin() != null
                    && current.compareTo(BigDecimal.valueOf(vo.getStockMin())) <= 0);
            // 通用件子列表
            List<ErpProductUniversalDO> universals = universalByProduct.get(vo.getId());
            vo.setUniversals(universals == null ? Collections.emptyList()
                    : BeanUtils.toBean(universals, ErpProductRespVO.Universal.class));
        });
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
        productMapper.updateShelfByIds(productIds, shelf);
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

}
