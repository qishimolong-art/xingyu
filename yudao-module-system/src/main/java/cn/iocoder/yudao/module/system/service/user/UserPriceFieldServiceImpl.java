package cn.iocoder.yudao.module.system.service.user;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.user.UserPriceFieldDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.UserPriceFieldMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User price field visibility service.
 */
@Service
public class UserPriceFieldServiceImpl implements UserPriceFieldService {

    private static final Map<String, List<String>> PRODUCT_PRICE_FIELD_MAP = new LinkedHashMap<String, List<String>>() {{
        put("branch_price", Arrays.asList("referencePrice", "col_referencePrice"));
        put("wholesale_price", Arrays.asList("wholesalePrice", "col_wholesalePrice"));
        put("retail_price", Arrays.asList("retailPrice", "col_retailPrice"));
        put("cost_price", Arrays.asList("purchasePrice", "col_purchasePrice"));
        put("last_purchase_price", Collections.singletonList("lastPurchasePrice"));
        put("gross_profit_rate", Collections.singletonList("grossProfitRate"));
        put("backup_price1", Arrays.asList("backupPrice1", "col_backupPrice1"));
    }};

    @Resource
    private UserPriceFieldMapper userPriceFieldMapper;

    @Override
    public List<UserPriceFieldDO> getUserPriceFields(Long userId) {
        return userPriceFieldMapper.selectListByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserPriceFields(Long userId, List<String> fieldCodes) {
        userPriceFieldMapper.deleteByUserId(userId, TenantContextHolder.getRequiredTenantId());

        Set<String> visibleCodes = fieldCodes == null ? Collections.emptySet() : fieldCodes.stream()
                .filter(StrUtil::isNotBlank)
                .filter(PRODUCT_PRICE_FIELD_MAP::containsKey)
                .collect(Collectors.toSet());
        List<UserPriceFieldDO> records = PRODUCT_PRICE_FIELD_MAP.keySet().stream()
                .map(code -> UserPriceFieldDO.builder()
                        .userId(userId)
                        .priceFieldCode(code)
                        .visible(visibleCodes.contains(code))
                        .build())
                .collect(Collectors.toList());
        records.forEach(userPriceFieldMapper::insert);
    }

    @Override
    public List<String> getHiddenProductPriceFields(Long userId) {
        List<UserPriceFieldDO> priceFields = userPriceFieldMapper.selectListByUserId(userId);
        if (priceFields.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> visibleCodes = priceFields.stream()
                .filter(item -> Boolean.TRUE.equals(item.getVisible()))
                .map(UserPriceFieldDO::getPriceFieldCode)
                .filter(PRODUCT_PRICE_FIELD_MAP::containsKey)
                .collect(Collectors.toSet());
        List<String> hiddenFields = new ArrayList<>();
        PRODUCT_PRICE_FIELD_MAP.forEach((code, productFields) -> {
            if (!visibleCodes.contains(code)) {
                hiddenFields.addAll(productFields);
            }
        });
        return hiddenFields.stream().distinct().collect(Collectors.toList());
    }

}
