package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpFieldConfigMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigFieldSourceEnum;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.permission.ProductPriceFieldKeys;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_PRICE_LEVEL_PERMISSION_DENIED;

@Component
public class ErpSalePriceLevelPermissionValidator {

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
    private static final String PRODUCT_FIELD_PERMISSION_MODULE = "erp_product";
    private static final String ORDER_PRICE_FIELD_GROUP = "price_info";
    private static final String ORDER_PRICE_FIELD_LABEL = "订货价";

    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpFieldConfigMapper fieldConfigMapper;

    public void validateSelectablePriceLevel(Integer priceLevel, Long businessDeptId) {
        if (priceLevel == null) {
            return;
        }
        Set<String> sourceFields = getSourceFields(priceLevel);
        if (CollUtil.isEmpty(sourceFields)) {
            throw exception(SALE_PRICE_LEVEL_PERMISSION_DENIED);
        }
        Set<String> hiddenFields = normalizeHiddenFields(
                permissionApi.getCurrentUserHiddenFields(PRODUCT_FIELD_PERMISSION_MODULE, businessDeptId));
        if (sourceFields.stream().anyMatch(field -> isHiddenField(hiddenFields, field))) {
            throw exception(SALE_PRICE_LEVEL_PERMISSION_DENIED);
        }
    }

    private Set<String> getSourceFields(Integer priceLevel) {
        switch (priceLevel) {
            case PRICE_LEVEL_BACKUP_PRICE:
                return Collections.singleton("backupPrice1");
            case PRICE_LEVEL_REFERENCE_PRICE:
                return Collections.singleton("referencePrice");
            case PRICE_LEVEL_RETAIL_PRICE:
                return Collections.singleton("retailPrice");
            case PRICE_LEVEL_WHOLESALE_PRICE:
                return Collections.singleton("wholesalePrice");
            case PRICE_LEVEL_LAST_PURCHASE_PRICE:
                return Collections.singleton("lastPurchasePrice");
            case PRICE_LEVEL_STOCK_COST_PRICE:
            case PRICE_LEVEL_PURCHASE_PRICE:
                return Collections.singleton("purchasePrice");
            case PRICE_LEVEL_SALE_PRICE:
                return Collections.singleton("salePrice");
            case PRICE_LEVEL_MIN_PRICE:
                return Collections.singleton("minPrice");
            case PRICE_LEVEL_SHARE_PRICE:
                return Collections.singleton("sharePrice");
            case PRICE_LEVEL_ORDER_PRICE:
                return loadOrderPriceFieldNames();
            default:
                return Collections.emptySet();
        }
    }

    private Set<String> loadOrderPriceFieldNames() {
        Set<String> fieldNames = fieldConfigMapper.selectListByModuleKey(ErpFieldConfigModuleEnum.ERP_PRODUCT.getKey())
                .stream()
                .filter(this::isOrderPriceCustomField)
                .map(ErpFieldConfigDO::getFieldName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return CollUtil.isEmpty(fieldNames) ? Collections.singleton("orderPrice") : fieldNames;
    }

    private boolean isOrderPriceCustomField(ErpFieldConfigDO config) {
        return config != null
                && ErpFieldConfigFieldSourceEnum.CUSTOM.getSource().equals(config.getFieldSource())
                && ORDER_PRICE_FIELD_GROUP.equals(config.getFieldGroup())
                && ORDER_PRICE_FIELD_LABEL.equals(StringUtils.trimWhitespace(config.getFieldLabel()));
    }

    private Set<String> normalizeHiddenFields(List<String> hiddenFields) {
        Set<String> result = new LinkedHashSet<>();
        CollUtil.emptyIfNull(hiddenFields).forEach(field -> ProductPriceFieldKeys.addHiddenField(result, field));
        return result;
    }

    private boolean isHiddenField(Set<String> hiddenFields, String field) {
        List<String> candidates = Arrays.asList(field, "col_" + field);
        return candidates.stream().anyMatch(hiddenFields::contains);
    }

}
