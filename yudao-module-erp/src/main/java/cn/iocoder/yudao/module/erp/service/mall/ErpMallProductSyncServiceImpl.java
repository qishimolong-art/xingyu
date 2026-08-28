package cn.iocoder.yudao.module.erp.service.mall;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.mall.vo.ErpMallCategorySyncStatusRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.mall.vo.ErpMallProductSyncStatusRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryListReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.mall.ErpMallCategoryMappingDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.mall.ErpMallProductMappingDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.mysql.mall.ErpMallCategoryMappingMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.mall.ErpMallProductMappingMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductCategoryService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.product.controller.admin.brand.vo.ProductBrandCreateReqVO;
import cn.iocoder.yudao.module.product.controller.admin.brand.vo.ProductBrandListReqVO;
import cn.iocoder.yudao.module.product.controller.admin.category.vo.ProductCategorySaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSkuSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuUpdateStatusReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.brand.ProductBrandDO;
import cn.iocoder.yudao.module.product.dal.dataobject.category.ProductCategoryDO;
import cn.iocoder.yudao.module.product.dal.dataobject.sku.ProductSkuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.spu.ProductSpuDO;
import cn.iocoder.yudao.module.product.enums.spu.ProductSpuStatusEnum;
import cn.iocoder.yudao.module.product.service.brand.ProductBrandService;
import cn.iocoder.yudao.module.product.service.category.ProductCategoryService;
import cn.iocoder.yudao.module.product.service.sku.ProductSkuService;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ERP 配件资料同步到商城商品中心。
 */
@Service
@Validated
public class ErpMallProductSyncServiceImpl implements ErpMallProductSyncService {

    private static final int SYNC_STATUS_SUCCESS = 0;
    private static final int SYNC_STATUS_FAIL = 1;
    private static final String DEFAULT_CATEGORY_PIC_URL = "/static/img/category/category-83.png";
    private static final String DEFAULT_PRODUCT_PIC_URL = "/static/goods-empty.png";
    private static final String DEFAULT_BRAND_NAME = "ERP同步";
    private static final int DELIVERY_TYPE_EXPRESS = 1;

    @Resource
    private ErpProductCategoryService erpProductCategoryService;
    @Resource
    private ErpProductMapper erpProductMapper;
    @Resource
    private ErpStockService erpStockService;

    @Resource
    private ProductCategoryService productCategoryService;
    @Resource
    private ProductSpuService productSpuService;
    @Resource
    private ProductSkuService productSkuService;
    @Resource
    private ProductBrandService productBrandService;

    @Resource
    private ErpMallCategoryMappingMapper categoryMappingMapper;
    @Resource
    private ErpMallProductMappingMapper productMappingMapper;

    @Override
    public int syncAllCategories() {
        List<ErpProductCategoryDO> categories = erpProductCategoryService.getProductCategoryList(
                new ErpProductCategoryListReqVO());
        categories.sort(Comparator.comparingInt(this::getCategoryLevel)
                .thenComparing(category -> category.getSort() == null ? 0 : category.getSort())
                .thenComparing(ErpProductCategoryDO::getId));
        categories.forEach(category -> syncCategory(category.getId()));
        return categories.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncCategory(Long erpCategoryId) {
        ErpProductCategoryDO category = erpProductCategoryService.getProductCategory(erpCategoryId);
        if (category == null) {
            saveCategoryFailure(erpCategoryId, null, "ERP 分类不存在");
            return;
        }
        try {
            doSyncCategory(category);
        } catch (Exception ex) {
            saveCategoryFailure(erpCategoryId, null, ex.getMessage());
        }
    }

    @Override
    public int syncAllProducts() {
        List<ErpProductDO> products = DataPermissionUtils.executeIgnore(() -> erpProductMapper.selectList());
        products.forEach(product -> syncProduct(product.getId()));
        return products.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncProduct(Long erpProductId) {
        ErpProductDO product = DataPermissionUtils.executeIgnore(() -> erpProductMapper.selectById(erpProductId));
        if (product == null) {
            saveProductFailure(erpProductId, null, null, "ERP 产品不存在");
            return;
        }
        try {
            doSyncProduct(product);
        } catch (Exception ex) {
            ErpMallProductMappingDO mapping = productMappingMapper.selectByErpProductId(erpProductId);
            saveProductFailure(erpProductId, mapping == null ? null : mapping.getMallSpuId(),
                    mapping == null ? null : mapping.getMallSkuId(), ex.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncProductStock(Long erpProductId) {
        ErpMallProductMappingDO mapping = productMappingMapper.selectByErpProductId(erpProductId);
        if (mapping == null || mapping.getMallSpuId() == null || mapping.getMallSkuId() == null) {
            saveProductFailure(erpProductId, mapping == null ? null : mapping.getMallSpuId(),
                    mapping == null ? null : mapping.getMallSkuId(), "产品尚未同步到商城，无法同步库存");
            return;
        }
        Integer stock = toStockInt(erpStockService.getStockCount(erpProductId));
        productSkuService.updateSkuStockCount(mapping.getMallSkuId(), stock);
        productSpuService.updateSpuStockCount(mapping.getMallSpuId(), stock);
        saveProductSuccess(erpProductId, mapping.getMallSpuId(), mapping.getMallSkuId(), null);
    }

    @Override
    public List<ErpMallCategorySyncStatusRespVO> getCategorySyncStatus(Integer syncStatus) {
        return BeanUtils.toBean(categoryMappingMapper.selectListBySyncStatus(syncStatus),
                ErpMallCategorySyncStatusRespVO.class);
    }

    @Override
    public List<ErpMallProductSyncStatusRespVO> getProductSyncStatus(Integer syncStatus) {
        return BeanUtils.toBean(productMappingMapper.selectListBySyncStatus(syncStatus),
                ErpMallProductSyncStatusRespVO.class);
    }

    private void doSyncCategory(ErpProductCategoryDO category) {
        int level = getCategoryLevel(category);
        if (level > ProductCategoryDO.CATEGORY_LEVEL) {
            throw new IllegalArgumentException("商城分类最多支持二级，当前 ERP 分类层级为 " + level);
        }
        Long mallParentId = ProductCategoryDO.PARENT_ID_NULL;
        if (!Objects.equals(category.getParentId(), ErpProductCategoryDO.PARENT_ID_ROOT)) {
            ErpMallCategoryMappingDO parentMapping = categoryMappingMapper.selectByErpCategoryId(category.getParentId());
            if (parentMapping == null || parentMapping.getMallCategoryId() == null
                    || !Objects.equals(parentMapping.getSyncStatus(), SYNC_STATUS_SUCCESS)) {
                ErpProductCategoryDO parent = erpProductCategoryService.getProductCategory(category.getParentId());
                if (parent == null) {
                    throw new IllegalArgumentException("ERP 父分类不存在");
                }
                doSyncCategory(parent);
                parentMapping = categoryMappingMapper.selectByErpCategoryId(category.getParentId());
            }
            mallParentId = parentMapping.getMallCategoryId();
        }

        ProductCategorySaveReqVO reqVO = new ProductCategorySaveReqVO();
        ErpMallCategoryMappingDO mapping = categoryMappingMapper.selectByErpCategoryId(category.getId());
        if (mapping != null) {
            reqVO.setId(mapping.getMallCategoryId());
        }
        reqVO.setParentId(mallParentId);
        reqVO.setName(truncate(requireText(category.getName(), "未命名分类"), 255));
        reqVO.setPicUrl(DEFAULT_CATEGORY_PIC_URL);
        reqVO.setSort(category.getSort() == null ? 0 : category.getSort());
        reqVO.setStatus(category.getStatus() == null ? CommonStatusEnum.DISABLE.getStatus() : category.getStatus());

        Long mallCategoryId;
        if (reqVO.getId() == null || productCategoryService.getCategory(reqVO.getId()) == null) {
            mallCategoryId = productCategoryService.createCategory(reqVO);
        } else {
            productCategoryService.updateCategory(reqVO);
            mallCategoryId = reqVO.getId();
        }
        saveCategorySuccess(category.getId(), mallCategoryId, null);
    }

    private void doSyncProduct(ErpProductDO product) {
        ErpProductCategoryDO category = erpProductCategoryService.getProductCategory(product.getCategoryId());
        if (category == null) {
            throw new IllegalArgumentException("ERP 产品分类不存在");
        }
        Long childCount = erpProductCategoryService.getProductCategoryChildCount(product.getCategoryId());
        if (childCount != null && childCount > 0) {
            throw new IllegalArgumentException("ERP 产品必须挂在叶子分类");
        }
        ErpMallCategoryMappingDO categoryMapping = categoryMappingMapper.selectByErpCategoryId(product.getCategoryId());
        if (categoryMapping == null || categoryMapping.getMallCategoryId() == null
                || !Objects.equals(categoryMapping.getSyncStatus(), SYNC_STATUS_SUCCESS)) {
            syncCategory(product.getCategoryId());
            categoryMapping = categoryMappingMapper.selectByErpCategoryId(product.getCategoryId());
        }
        if (categoryMapping == null || categoryMapping.getMallCategoryId() == null
                || !Objects.equals(categoryMapping.getSyncStatus(), SYNC_STATUS_SUCCESS)) {
            throw new IllegalArgumentException("ERP 产品分类尚未同步到商城");
        }

        Long brandId = getOrCreateBrandId(product.getBrand());
        Integer stock = toStockInt(erpStockService.getStockCount(product.getId()));
        SyncBuildResult buildResult = buildSpuSaveReqVO(product, categoryMapping.getMallCategoryId(), brandId, stock);
        ErpMallProductMappingDO mapping = productMappingMapper.selectByErpProductId(product.getId());
        Long reusableSpuId = resolveReusableSpuId(product, mapping);
        if (reusableSpuId != null) {
            buildResult.reqVO.setId(reusableSpuId);
            if (mapping == null || !Objects.equals(mapping.getMallSpuId(), reusableSpuId)) {
                buildResult.notice = appendNotice(buildResult.notice, "映射缺失，已按配件编码绑定已有商城商品");
            }
        }

        Long spuId;
        if (buildResult.reqVO.getId() == null || productSpuService.getSpu(buildResult.reqVO.getId()) == null) {
            spuId = productSpuService.createSpu(buildResult.reqVO);
        } else {
            productSpuService.updateSpu(buildResult.reqVO);
            spuId = buildResult.reqVO.getId();
        }
        ProductSpuStatusEnum targetStatus = isProductEnabled(product)
                ? ProductSpuStatusEnum.ENABLE : ProductSpuStatusEnum.DISABLE;
        ProductSpuDO spu = productSpuService.getSpu(spuId);
        if (spu != null && !Objects.equals(spu.getStatus(), targetStatus.getStatus())) {
            productSpuService.updateSpuStatus(new ProductSpuUpdateStatusReqVO()
                    .setId(spuId).setStatus(targetStatus.getStatus()));
        }

        List<ProductSkuDO> skus = productSkuService.getSkuListBySpuId(spuId);
        Long skuId = CollUtil.isEmpty(skus) ? null : skus.get(0).getId();
        saveProductSuccess(product.getId(), spuId, skuId, buildResult.notice);
    }

    private SyncBuildResult buildSpuSaveReqVO(ErpProductDO product, Long categoryId, Long brandId, Integer stock) {
        String picUrl = StrUtil.blankToDefault(product.getMainImage(), DEFAULT_PRODUCT_PIC_URL);
        String notice = null;
        if (StrUtil.isBlank(product.getMainImage())) {
            notice = appendNotice(notice, "图片缺失，已使用默认图");
        }
        Integer price = toCent(product.getRetailPrice() != null ? product.getRetailPrice() : product.getSalePrice());
        if (price == null) {
            price = 0;
            notice = appendNotice(notice, "价格缺失，已按 0 元同步");
        }
        Integer marketPrice = toCent(product.getReferencePrice());
        Integer costPrice = toCent(product.getPurchasePrice());

        ProductSkuSaveReqVO sku = new ProductSkuSaveReqVO();
        sku.setName(truncate(requireText(product.getName(), "未命名商品"), 128));
        sku.setPrice(price);
        sku.setMarketPrice(marketPrice == null ? price : marketPrice);
        sku.setCostPrice(costPrice == null ? 0 : costPrice);
        sku.setBarCode(StrUtil.blankToDefault(product.getBarCode(), product.getCode()));
        sku.setPicUrl(picUrl);
        sku.setStock(stock);
        sku.setWeight(product.getWeight() == null ? null : product.getWeight().doubleValue());
        sku.setVolume(0D);
        sku.setFirstBrokeragePrice(0);
        sku.setSecondBrokeragePrice(0);

        ProductSpuSaveReqVO reqVO = new ProductSpuSaveReqVO();
        reqVO.setName(truncate(requireText(product.getName(), "未命名商品"), 128));
        reqVO.setKeyword(truncate(buildKeyword(product), 256));
        reqVO.setIntroduction(truncate(buildIntroduction(product), 256));
        reqVO.setDescription(buildDescription(product));
        reqVO.setCategoryId(categoryId);
        reqVO.setBrandId(brandId);
        reqVO.setCode(normalizeText(product.getCode(), 64));
        reqVO.setStandard(normalizeText(product.getStandard(), 128));
        reqVO.setFeatureCode(normalizeText(product.getFeatureCode(), 64));
        reqVO.setVehicleModel(normalizeText(StrUtil.blankToDefault(product.getVehicleModel(),
                product.getVehicleModelText()), 128));
        reqVO.setPicUrl(picUrl);
        reqVO.setSliderPicUrls(Collections.singletonList(picUrl));
        reqVO.setSort(product.getId() == null ? 0 : product.getId().intValue());
        reqVO.setSpecType(false);
        reqVO.setDeliveryTypes(Collections.singletonList(DELIVERY_TYPE_EXPRESS));
        reqVO.setGiveIntegral(0);
        reqVO.setSubCommissionType(false);
        reqVO.setVirtualSalesCount(0);
        reqVO.setSalesCount(0);
        reqVO.setBrowseCount(0);
        reqVO.setSkus(Collections.singletonList(sku));
        return new SyncBuildResult(reqVO, notice);
    }

    private Long getOrCreateBrandId(String sourceBrandName) {
        String brandName = normalizeText(sourceBrandName, 255);
        if (StrUtil.isBlank(brandName)) {
            brandName = DEFAULT_BRAND_NAME;
        }
        ProductBrandListReqVO reqVO = new ProductBrandListReqVO();
        reqVO.setName(brandName);
        List<ProductBrandDO> brands = productBrandService.getBrandList(reqVO);
        for (ProductBrandDO brand : brands) {
            if (brandName.equals(brand.getName())
                    && CommonStatusEnum.ENABLE.getStatus().equals(brand.getStatus())) {
                return brand.getId();
            }
        }
        ProductBrandCreateReqVO createReqVO = new ProductBrandCreateReqVO();
        createReqVO.setName(brandName);
        createReqVO.setPicUrl(DEFAULT_PRODUCT_PIC_URL);
        createReqVO.setSort(0);
        createReqVO.setDescription(DEFAULT_BRAND_NAME.equals(brandName) ? "ERP 配件同步默认品牌" : "ERP 配件同步品牌");
        createReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return productBrandService.createBrand(createReqVO);
    }

    private boolean isProductEnabled(ErpProductDO product) {
        return CommonStatusEnum.ENABLE.getStatus().equals(product.getStatus())
                && !Boolean.TRUE.equals(product.getMergedFlag());
    }

    private int getCategoryLevel(ErpProductCategoryDO category) {
        int level = 1;
        Long parentId = category.getParentId();
        for (int i = 0; i < Byte.MAX_VALUE; i++) {
            if (parentId == null || Objects.equals(parentId, ErpProductCategoryDO.PARENT_ID_ROOT)) {
                return level;
            }
            ErpProductCategoryDO parent = erpProductCategoryService.getProductCategory(parentId);
            if (parent == null) {
                return level + 1;
            }
            level++;
            parentId = parent.getParentId();
        }
        return level;
    }

    private String buildKeyword(ErpProductDO product) {
        List<String> values = new ArrayList<>();
        addIfNotBlank(values, product.getCode());
        addIfNotBlank(values, product.getBarCode());
        addIfNotBlank(values, product.getFactoryCode());
        addIfNotBlank(values, product.getFeatureCode());
        addIfNotBlank(values, product.getBrand());
        addIfNotBlank(values, product.getVehicleModel());
        return values.isEmpty() ? requireText(product.getName(), "ERP商品") : String.join(" ", values);
    }

    private String buildIntroduction(ErpProductDO product) {
        List<String> values = new ArrayList<>();
        addIfNotBlank(values, product.getCode() == null ? null : "编码：" + product.getCode());
        addIfNotBlank(values, product.getStandard() == null ? null : "规格：" + product.getStandard());
        addIfNotBlank(values, product.getFeatureCode() == null ? null : "特征码：" + product.getFeatureCode());
        return values.isEmpty() ? requireText(product.getName(), "ERP商品") : String.join("；", values);
    }

    private String buildDescription(ErpProductDO product) {
        if (StrUtil.isNotBlank(product.getDetailContent())) {
            return product.getDetailContent();
        }
        StringBuilder builder = new StringBuilder();
        appendDescription(builder, "配件编码", product.getCode());
        appendDescription(builder, "条码", product.getBarCode());
        appendDescription(builder, "规格", product.getStandard());
        appendDescription(builder, "品牌", product.getBrand());
        appendDescription(builder, "特征码", product.getFeatureCode());
        appendDescription(builder, "厂家编码", product.getFactoryCode());
        appendDescription(builder, "适用车型", product.getVehicleModel());
        appendDescription(builder, "备注", product.getRemark());
        return builder.length() == 0 ? requireText(product.getName(), "ERP商品") : builder.toString();
    }

    private Long resolveReusableSpuId(ErpProductDO product, ErpMallProductMappingDO mapping) {
        if (mapping != null && mapping.getMallSpuId() != null
                && productSpuService.getSpu(mapping.getMallSpuId()) != null) {
            return mapping.getMallSpuId();
        }
        String partCode = normalizeText(product.getCode(), 64);
        if (StrUtil.isBlank(partCode)) {
            return null;
        }
        List<ProductSpuDO> matchedSpus = productSpuService.getSpuListByPartCode(partCode);
        if (CollUtil.isEmpty(matchedSpus)) {
            return null;
        }
        if (matchedSpus.size() > 1) {
            throw new IllegalArgumentException("配件编码 " + partCode + " 匹配到多个商城商品，请先清理重复商品");
        }
        Long matchedSpuId = matchedSpus.get(0).getId();
        List<ErpMallProductMappingDO> occupiedMappings = productMappingMapper.selectListByMallSpuId(matchedSpuId);
        for (ErpMallProductMappingDO occupiedMapping : occupiedMappings) {
            if (!Objects.equals(occupiedMapping.getErpProductId(), product.getId())) {
                throw new IllegalArgumentException("配件编码 " + partCode + " 对应的商城商品已绑定其它 ERP 产品，请先清理映射");
            }
        }
        return matchedSpuId;
    }

    private void appendDescription(StringBuilder builder, String label, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        builder.append("<p>").append(label).append("：").append(value).append("</p>");
    }

    private void addIfNotBlank(List<String> values, String value) {
        if (StrUtil.isNotBlank(value)) {
            values.add(value);
        }
    }

    private Integer toCent(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        BigDecimal cents = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
        if (cents.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            return Integer.MAX_VALUE;
        }
        if (cents.compareTo(BigDecimal.ZERO) < 0) {
            return 0;
        }
        return cents.intValue();
    }

    private Integer toStockInt(BigDecimal stock) {
        if (stock == null) {
            return 0;
        }
        BigDecimal count = stock.setScale(0, RoundingMode.DOWN);
        if (count.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            return Integer.MAX_VALUE;
        }
        if (count.compareTo(BigDecimal.ZERO) < 0) {
            return 0;
        }
        return count.intValue();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String requireText(String value, String fallback) {
        return StrUtil.blankToDefault(value, fallback);
    }

    private String normalizeText(String value, int maxLength) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return truncate(value.trim(), maxLength);
    }

    private String appendNotice(String current, String notice) {
        return StrUtil.isBlank(current) ? notice : current + "；" + notice;
    }

    private void saveCategorySuccess(Long erpCategoryId, Long mallCategoryId, String notice) {
        ErpMallCategoryMappingDO mapping = categoryMappingMapper.selectByErpCategoryId(erpCategoryId);
        if (mapping == null) {
            mapping = new ErpMallCategoryMappingDO().setErpCategoryId(erpCategoryId);
        }
        mapping.setMallCategoryId(mallCategoryId)
                .setSyncStatus(SYNC_STATUS_SUCCESS)
                .setLastSyncTime(LocalDateTime.now())
                .setFailReason(truncate(notice, 512));
        saveCategoryMapping(mapping);
    }

    private void saveCategoryFailure(Long erpCategoryId, Long mallCategoryId, String failReason) {
        ErpMallCategoryMappingDO mapping = categoryMappingMapper.selectByErpCategoryId(erpCategoryId);
        if (mapping == null) {
            mapping = new ErpMallCategoryMappingDO().setErpCategoryId(erpCategoryId);
        }
        mapping.setMallCategoryId(mallCategoryId != null ? mallCategoryId : mapping.getMallCategoryId())
                .setSyncStatus(SYNC_STATUS_FAIL)
                .setLastSyncTime(LocalDateTime.now())
                .setFailReason(truncate(failReason, 512));
        saveCategoryMapping(mapping);
    }

    private void saveProductSuccess(Long erpProductId, Long mallSpuId, Long mallSkuId, String notice) {
        ErpMallProductMappingDO mapping = productMappingMapper.selectByErpProductId(erpProductId);
        if (mapping == null) {
            mapping = new ErpMallProductMappingDO().setErpProductId(erpProductId);
        }
        mapping.setMallSpuId(mallSpuId)
                .setMallSkuId(mallSkuId)
                .setSyncStatus(SYNC_STATUS_SUCCESS)
                .setLastSyncTime(LocalDateTime.now())
                .setFailReason(truncate(notice, 512));
        saveProductMapping(mapping);
    }

    private void saveProductFailure(Long erpProductId, Long mallSpuId, Long mallSkuId, String failReason) {
        ErpMallProductMappingDO mapping = productMappingMapper.selectByErpProductId(erpProductId);
        if (mapping == null) {
            mapping = new ErpMallProductMappingDO().setErpProductId(erpProductId);
        }
        mapping.setMallSpuId(mallSpuId != null ? mallSpuId : mapping.getMallSpuId())
                .setMallSkuId(mallSkuId != null ? mallSkuId : mapping.getMallSkuId())
                .setSyncStatus(SYNC_STATUS_FAIL)
                .setLastSyncTime(LocalDateTime.now())
                .setFailReason(truncate(failReason, 512));
        saveProductMapping(mapping);
    }

    private void saveCategoryMapping(ErpMallCategoryMappingDO mapping) {
        if (mapping.getId() == null) {
            categoryMappingMapper.insert(mapping);
        } else {
            categoryMappingMapper.updateById(mapping);
        }
    }

    private void saveProductMapping(ErpMallProductMappingDO mapping) {
        if (mapping.getId() == null) {
            productMappingMapper.insert(mapping);
        } else {
            productMappingMapper.updateById(mapping);
        }
    }

    private static class SyncBuildResult {
        private final ProductSpuSaveReqVO reqVO;
        private String notice;

        private SyncBuildResult(ProductSpuSaveReqVO reqVO, String notice) {
            this.reqVO = reqVO;
            this.notice = notice;
        }
    }

}
