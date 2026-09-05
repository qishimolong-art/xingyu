package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpFieldConfigMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigFieldSourceEnum;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

@Component
public class ErpSalePriceLevelPricePicker {

    private static final int PRICE_LEVEL_BACKUP_PRICE = 1;
    private static final int PRICE_LEVEL_REFERENCE_PRICE = 2;
    private static final int PRICE_LEVEL_RETAIL_PRICE = 3;
    private static final int PRICE_LEVEL_WHOLESALE_PRICE = 4;
    private static final int PRICE_LEVEL_LAST_PURCHASE_PRICE = 5;
    private static final int PRICE_LEVEL_STOCK_COST_PRICE = 6;
    private static final int PRICE_LEVEL_PURCHASE_PRICE = 7;
    private static final int PRICE_LEVEL_SALE_PRICE = 8;
    private static final int PRICE_LEVEL_MIN_PRICE = 9;
    private static final int PRICE_LEVEL_SHARE_PRICE = 10;
    private static final int PRICE_LEVEL_ORDER_PRICE = 11;
    private static final String ORDER_PRICE_FIELD_GROUP = "price_info";
    private static final String ORDER_PRICE_FIELD_LABEL = "订货价";

    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpFieldConfigMapper fieldConfigMapper;

    public Map<Long, BigDecimal> pickProductPriceMap(Collection<Long> productIds, Integer priceLevel) {
        if (CollUtil.isEmpty(productIds) || priceLevel == null) {
            return Collections.emptyMap();
        }
        List<ErpProductDO> products = DataPermissionUtils.executeIgnore(() -> productMapper.selectByIds(productIds));
        Map<Long, ErpProductDO> productMap = convertMap(products, ErpProductDO::getId);
        Map<Long, BigDecimal> orderPriceMap = PRICE_LEVEL_ORDER_PRICE == priceLevel
                ? loadOrderPriceMap(productMap.keySet()) : Collections.emptyMap();
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        productMap.forEach((productId, product) ->
                result.put(productId, pickProductPrice(product, priceLevel, orderPriceMap.get(productId))));
        return result;
    }

    private BigDecimal pickProductPrice(ErpProductDO product, Integer priceLevel, BigDecimal orderPrice) {
        if (product == null) {
            return BigDecimal.ZERO;
        }
        switch (priceLevel) {
            case PRICE_LEVEL_BACKUP_PRICE:
                return firstPrice(product.getBackupPrice1());
            case PRICE_LEVEL_REFERENCE_PRICE:
                return firstPrice(product.getReferencePrice());
            case PRICE_LEVEL_RETAIL_PRICE:
                return firstPrice(product.getRetailPrice());
            case PRICE_LEVEL_WHOLESALE_PRICE:
                return firstPrice(product.getWholesalePrice());
            case PRICE_LEVEL_LAST_PURCHASE_PRICE:
                return firstPrice(product.getLastPurchasePrice(), product.getPurchasePrice());
            case PRICE_LEVEL_STOCK_COST_PRICE:
            case PRICE_LEVEL_PURCHASE_PRICE:
                return firstPrice(product.getPurchasePrice());
            case PRICE_LEVEL_SALE_PRICE:
                return firstPrice(product.getSalePrice());
            case PRICE_LEVEL_MIN_PRICE:
                return firstPrice(product.getMinPrice());
            case PRICE_LEVEL_SHARE_PRICE:
                return firstPrice(product.getSharePrice());
            case PRICE_LEVEL_ORDER_PRICE:
                return orderPrice != null ? orderPrice : defaultSalePrice(product);
            default:
                return defaultSalePrice(product);
        }
    }

    private Map<Long, BigDecimal> loadOrderPriceMap(Collection<Long> productIds) {
        List<String> columns = fieldConfigMapper.selectListByModuleKey(ErpFieldConfigModuleEnum.ERP_PRODUCT.getKey())
                .stream()
                .filter(this::isOrderPriceCustomField)
                .map(ErpFieldConfigDO::getPhysicalColumn)
                .filter(StringUtils::hasText)
                .filter(this::isSafeCustomColumn)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(columns)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (Map<String, Object> row : productMapper.selectCustomFieldMaps(productIds, columns)) {
            Object id = row.get("id");
            if (!(id instanceof Number)) {
                continue;
            }
            for (String column : columns) {
                BigDecimal value = toBigDecimal(row.get(column));
                if (value != null) {
                    result.put(((Number) id).longValue(), value);
                    break;
                }
            }
        }
        return result;
    }

    private boolean isOrderPriceCustomField(ErpFieldConfigDO config) {
        return config != null
                && ErpFieldConfigFieldSourceEnum.CUSTOM.getSource().equals(config.getFieldSource())
                && ORDER_PRICE_FIELD_GROUP.equals(config.getFieldGroup())
                && ORDER_PRICE_FIELD_LABEL.equals(StringUtils.trimWhitespace(config.getFieldLabel()));
    }

    private boolean isSafeCustomColumn(String column) {
        return column != null && column.matches("ext_[a-z0-9_]+");
    }

    private BigDecimal defaultSalePrice(ErpProductDO product) {
        return firstPrice(product.getSalePrice(), product.getRetailPrice(), product.getReferencePrice());
    }

    private BigDecimal firstPrice(Object... values) {
        for (Object value : values) {
            BigDecimal price = toBigDecimal(value);
            if (price != null) {
                return price;
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
