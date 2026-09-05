package cn.iocoder.yudao.module.erp.service.common;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * ERP 导入明细中的配件匹配规则：配件编码、配件名称、厂家编码任选一；非唯一字段必须能唯一定位配件。
 */
public class ErpImportProductResolver {

    private static final String REQUIRED_MESSAGE = "配件编码、配件名称和厂家编码为三选一字段，请至少填写其中一个";
    private static final String MISMATCH_MESSAGE = "配件编码、配件名称和厂家编码不一致";

    private final Map<String, ErpProductDO> productMapByCode;
    private final Map<String, List<ErpProductDO>> productMapByName;
    private final Map<String, List<ErpProductDO>> productMapByFactoryCode;
    private final Map<Long, ErpProductDO> resolvedProductMap;

    private ErpImportProductResolver(Map<String, ErpProductDO> productMapByCode,
                                     Map<String, List<ErpProductDO>> productMapByName,
                                     Map<String, List<ErpProductDO>> productMapByFactoryCode,
                                     Map<Long, ErpProductDO> resolvedProductMap) {
        this.productMapByCode = productMapByCode;
        this.productMapByName = productMapByName;
        this.productMapByFactoryCode = productMapByFactoryCode;
        this.resolvedProductMap = resolvedProductMap;
    }

    public static <T> ErpImportProductResolver build(Collection<T> rows,
                                                     Function<T, String> productCodeGetter,
                                                     Function<T, String> productNameGetter,
                                                     ErpProductMapper productMapper) {
        return build(rows, productCodeGetter, productNameGetter, row -> null, productMapper);
    }

    public static <T> ErpImportProductResolver build(Collection<T> rows,
                                                     Function<T, String> productCodeGetter,
                                                     Function<T, String> productNameGetter,
                                                     Function<T, String> factoryCodeGetter,
                                                     ErpProductMapper productMapper) {
        if (CollUtil.isEmpty(rows)) {
            return empty();
        }
        Set<String> productCodes = new LinkedHashSet<>();
        Set<String> productNames = new LinkedHashSet<>();
        Set<String> factoryCodes = new LinkedHashSet<>();
        for (T row : rows) {
            if (row == null) {
                continue;
            }
            String productCode = trimToNull(productCodeGetter.apply(row));
            if (productCode != null) {
                productCodes.add(productCode);
            }
            String productName = trimToNull(productNameGetter.apply(row));
            if (productName != null) {
                productNames.add(productName);
            }
            String factoryCode = trimToNull(factoryCodeGetter.apply(row));
            if (factoryCode != null) {
                factoryCodes.add(factoryCode);
            }
        }

        Map<Long, ErpProductDO> productsById = new LinkedHashMap<>();
        if (CollUtil.isNotEmpty(productCodes)) {
            List<ErpProductDO> products = DataPermissionUtils.executeIgnore(() ->
                    productMapper.selectListByCodes(productCodes));
            addProducts(productsById, products);
        }
        if (CollUtil.isNotEmpty(productNames)) {
            List<ErpProductDO> products = DataPermissionUtils.executeIgnore(() ->
                    productMapper.selectListByNames(productNames));
            addProducts(productsById, products);
        }
        if (CollUtil.isNotEmpty(factoryCodes)) {
            List<ErpProductDO> products = DataPermissionUtils.executeIgnore(() ->
                    productMapper.selectListByFactoryCodes(factoryCodes));
            addProducts(productsById, products);
        }

        Map<String, ErpProductDO> productMapByCode = new LinkedHashMap<>();
        Map<String, List<ErpProductDO>> productMapByName = new LinkedHashMap<>();
        Map<String, List<ErpProductDO>> productMapByFactoryCode = new LinkedHashMap<>();
        for (ErpProductDO product : productsById.values()) {
            String productCode = trimToNull(product.getCode());
            if (productCode != null) {
                productMapByCode.putIfAbsent(productCode, product);
            }
            String productName = trimToNull(product.getName());
            if (productName != null) {
                productMapByName.computeIfAbsent(productName, key -> new ArrayList<>()).add(product);
            }
            String factoryCode = trimToNull(product.getFactoryCode());
            if (factoryCode != null) {
                productMapByFactoryCode.computeIfAbsent(factoryCode, key -> new ArrayList<>()).add(product);
            }
        }
        return new ErpImportProductResolver(productMapByCode, productMapByName, productMapByFactoryCode, productsById);
    }

    public static ErpImportProductResolver empty() {
        return new ErpImportProductResolver(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap());
    }

    public ResolveResult resolve(String productCode, String productName) {
        return resolve(productCode, productName, null);
    }

    public ResolveResult resolve(String productCode, String productName, String factoryCode) {
        String code = trimToNull(productCode);
        String name = trimToNull(productName);
        String factory = trimToNull(factoryCode);
        String identifier = getIdentifier(code, name, factory);
        if (code == null && name == null && factory == null) {
            return ResolveResult.failure(identifier, REQUIRED_MESSAGE);
        }

        Set<Long> candidateIds = null;
        if (code != null) {
            ErpProductDO productByCode = productMapByCode.get(code);
            if (productByCode == null) {
                return ResolveResult.failure(identifier, "配件编码不存在：" + code);
            }
            candidateIds = intersect(candidateIds, Collections.singleton(productByCode.getId()));
        }

        if (name != null) {
            List<ErpProductDO> products = productMapByName.get(name);
            if (CollUtil.isEmpty(products)) {
                return ResolveResult.failure(identifier, "配件名称不存在：" + name);
            }
            candidateIds = intersect(candidateIds, toIdSet(products));
        }

        if (factory != null) {
            List<ErpProductDO> products = productMapByFactoryCode.get(factory);
            if (CollUtil.isEmpty(products)) {
                return ResolveResult.failure(identifier, "厂家编码不存在：" + factory);
            }
            candidateIds = intersect(candidateIds, toIdSet(products));
        }

        if (CollUtil.isEmpty(candidateIds)) {
            return ResolveResult.failure(identifier, MISMATCH_MESSAGE);
        }

        if (candidateIds.size() > 1) {
            if (code == null && factory != null) {
                return ResolveResult.failure(identifier, "厂家编码存在重复，请填写配件编码：" + factory);
            }
            if (code == null && name != null) {
                return ResolveResult.failure(identifier, "配件名称存在重复，请填写配件编码：" + name);
            }
            return ResolveResult.failure(identifier, MISMATCH_MESSAGE);
        }
        Long productId = candidateIds.iterator().next();
        return ResolveResult.success(identifier, resolvedProductMap.get(productId));
    }

    public Collection<ErpProductDO> getResolvedProducts() {
        return resolvedProductMap.values();
    }

    public static String getIdentifier(String productCode, String productName) {
        return getIdentifier(productCode, productName, null);
    }

    public static String getIdentifier(String productCode, String productName, String factoryCode) {
        String code = trimToNull(productCode);
        if (code != null) {
            return code;
        }
        String factory = trimToNull(factoryCode);
        return factory != null ? factory : trimToNull(productName);
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void addProducts(Map<Long, ErpProductDO> productsById, List<ErpProductDO> products) {
        if (CollUtil.isEmpty(products)) {
            return;
        }
        for (ErpProductDO product : products) {
            if (product != null && product.getId() != null) {
                productsById.putIfAbsent(product.getId(), product);
            }
        }
    }

    private static Set<Long> toIdSet(List<ErpProductDO> products) {
        Set<Long> ids = new LinkedHashSet<>();
        for (ErpProductDO product : products) {
            if (product != null && product.getId() != null) {
                ids.add(product.getId());
            }
        }
        return ids;
    }

    private static Set<Long> intersect(Set<Long> current, Set<Long> candidate) {
        if (current == null) {
            return new LinkedHashSet<>(candidate);
        }
        current.removeIf(id -> !candidate.contains(id));
        return current;
    }

    public static class ResolveResult {
        private final String identifier;
        private final ErpProductDO product;
        private final String errorMessage;

        private ResolveResult(String identifier, ErpProductDO product, String errorMessage) {
            this.identifier = identifier;
            this.product = product;
            this.errorMessage = errorMessage;
        }

        public static ResolveResult success(String identifier, ErpProductDO product) {
            return new ResolveResult(identifier, product, null);
        }

        public static ResolveResult failure(String identifier, String errorMessage) {
            return new ResolveResult(identifier, null, errorMessage);
        }

        public boolean isFailure() {
            return errorMessage != null;
        }

        public String getIdentifier() {
            return identifier;
        }

        public ErpProductDO getProduct() {
            return product;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

}
