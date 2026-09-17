package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fills read-only product price references for sale document item response VOs.
 */
@Component
public class ErpSaleItemPriceReferenceFiller {

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;

    public void fill(Collection<?> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Set<Long> productIds = collectLongFieldValues(items, "productId");
        if (CollUtil.isEmpty(productIds)) {
            return;
        }
        Set<Long> warehouseIds = collectLongFieldValues(items, "warehouseId");
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(productIds));
        Map<String, ErpStockDO> stockMap = CollUtil.isEmpty(warehouseIds)
                ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> stockService.getStockMap(productIds, warehouseIds));
        Map<Long, BigDecimal> lastSalePriceMap = DataPermissionUtils.executeIgnore(() ->
                saleOutItemMapper.selectLatestSalePriceMap(productIds));
        for (Object item : items) {
            Long productId = asLong(getFieldValue(item, "productId"));
            if (productId == null) {
                continue;
            }
            ErpProductRespVO product = productMap.get(productId);
            if (product == null) {
                continue;
            }
            Long warehouseId = asLong(getFieldValue(item, "warehouseId"));
            ErpStockDO stock = warehouseId == null ? null : stockMap.get(buildStockMapKey(productId, warehouseId));
            BigDecimal purchasePrice = stock != null && stock.getPurchasePrice() != null
                    ? stock.getPurchasePrice() : product.getPurchasePrice();
            setFieldValue(item, "productPurchasePrice", purchasePrice);
            setFieldValueIfEmpty(item, "salePrice", product.getSalePrice());
            setFieldValueIfEmpty(item, "lastSalePrice", lastSalePriceMap.get(productId));
            setFieldValue(item, "minPrice", product.getMinPrice());
            setFieldValue(item, "referencePrice", product.getReferencePrice());
            setFieldValue(item, "retailPrice", product.getRetailPrice());
            setFieldValue(item, "lastPurchasePrice", product.getLastPurchasePrice());
            setFieldValue(item, "grossProfitRate", product.getGrossProfitRate());
            setFieldValue(item, "backupPrice1", product.getBackupPrice1());
            setFieldValue(item, "wholesalePrice", product.getWholesalePrice());
            setFieldValue(item, "sharePrice", product.getSharePrice());
        }
    }

    private Set<Long> collectLongFieldValues(Collection<?> items, String fieldName) {
        Set<Long> values = new HashSet<>();
        for (Object item : items) {
            Long value = asLong(getFieldValue(item, fieldName));
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private Long asLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Object getFieldValue(Object bean, String fieldName) {
        Field field = findField(bean.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(bean);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private void setFieldValueIfEmpty(Object bean, String fieldName, Object value) {
        if (getFieldValue(bean, fieldName) != null) {
            return;
        }
        setFieldValue(bean, fieldName, value);
    }

    private void setFieldValue(Object bean, String fieldName, Object value) {
        Field field = findField(bean.getClass(), fieldName);
        if (field == null) {
            return;
        }
        try {
            field.setAccessible(true);
            field.set(bean, value);
        } catch (IllegalAccessException ignored) {
            // Ignore fields that cannot be filled reflectively.
        }
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private String buildStockMapKey(Long productId, Long warehouseId) {
        return productId + "_" + warehouseId;
    }
}
