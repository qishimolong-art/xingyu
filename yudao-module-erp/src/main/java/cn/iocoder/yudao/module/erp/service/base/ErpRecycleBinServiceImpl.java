package cn.iocoder.yudao.module.erp.service.base;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin.ErpRecycleBinBatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin.ErpRecycleBinItemReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin.ErpRecycleBinPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin.ErpRecycleBinRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.base.ErpRecycleBinMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_ENABLE;

@Service
@Validated
public class ErpRecycleBinServiceImpl implements ErpRecycleBinService {

    private static final String SOURCE_SUPPLIER = "supplier";
    private static final String SOURCE_CUSTOMER = "customer";
    private static final String SOURCE_PRODUCT = "product";
    private static final String SOURCE_WAREHOUSE = "warehouse";

    @Resource
    private ErpRecycleBinMapper recycleBinMapper;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public PageResult<ErpRecycleBinRespVO> getRecycleBinPage(ErpRecycleBinPageReqVO pageReqVO) {
        String sourceType = normalizeSourceType(pageReqVO.getSourceType());
        String name = trimToNull(pageReqVO.getName());
        String code = trimToNull(pageReqVO.getCode());
        Long total = recycleBinMapper.selectCount(sourceType, name, code);
        if (total == null || total <= 0) {
            return PageResult.empty();
        }
        int offset = (pageReqVO.getPageNo() - 1) * pageReqVO.getPageSize();
        List<ErpRecycleBinRespVO> list = recycleBinMapper.selectPage(sourceType, name, code, offset, pageReqVO.getPageSize());
        fillDisabledByName(list);
        return new PageResult<>(list, total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(ErpRecycleBinBatchReqVO reqVO) {
        Map<String, List<Long>> idMap = groupIds(reqVO);
        restoreIfPresent(idMap, SOURCE_SUPPLIER, supplierService::restoreSupplier);
        restoreIfPresent(idMap, SOURCE_CUSTOMER, customerService::restoreCustomer);
        restoreIfPresent(idMap, SOURCE_PRODUCT, productService::restoreProduct);
        restoreIfPresent(idMap, SOURCE_WAREHOUSE, warehouseService::restoreWarehouse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clear(ErpRecycleBinBatchReqVO reqVO) {
        Map<String, List<Long>> idMap = groupIds(reqVO);
        clearIfPresent(idMap, SOURCE_SUPPLIER, supplierService::getSupplier, supplierService::deleteSupplier);
        clearIfPresent(idMap, SOURCE_CUSTOMER, customerService::getCustomer, customerService::deleteCustomer);
        clearIfPresent(idMap, SOURCE_PRODUCT, productService::getProduct, productService::deleteProduct);
        clearIfPresent(idMap, SOURCE_WAREHOUSE, warehouseService::getWarehouse, warehouseService::deleteWarehouse);
    }

    private Map<String, List<Long>> groupIds(ErpRecycleBinBatchReqVO reqVO) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getItems())) {
            return Collections.emptyMap();
        }
        reqVO.getItems().forEach(item -> normalizeSourceType(item.getSourceType()));
        return reqVO.getItems().stream()
                .filter(item -> item.getSourceId() != null)
                .collect(Collectors.groupingBy(item -> normalizeSourceType(item.getSourceType()),
                        Collectors.mapping(ErpRecycleBinItemReqVO::getSourceId,
                                Collectors.collectingAndThen(Collectors.toList(), ids -> ids.stream()
                                        .filter(Objects::nonNull).distinct().collect(Collectors.toList())))));
    }

    private void restoreIfPresent(Map<String, List<Long>> idMap, String sourceType, java.util.function.Consumer<List<Long>> consumer) {
        List<Long> ids = idMap.get(sourceType);
        if (CollUtil.isNotEmpty(ids)) {
            consumer.accept(ids);
        }
    }

    private <T> void clearIfPresent(Map<String, List<Long>> idMap, String sourceType,
                                    java.util.function.Function<Long, T> getter,
                                    java.util.function.Consumer<Long> deleter) {
        List<Long> ids = idMap.get(sourceType);
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            T row = getter.apply(id);
            validateDisabled(sourceType, row);
            deleter.accept(id);
        }
    }

    private void validateDisabled(String sourceType, Object row) {
        if (SOURCE_SUPPLIER.equals(sourceType)) {
            ErpSupplierDO supplier = (ErpSupplierDO) row;
            if (supplier == null || !CommonStatusEnum.isDisable(supplier.getStatus())) {
                throw exception(SUPPLIER_NOT_ENABLE, supplier == null ? "" : supplier.getName());
            }
            return;
        }
        if (SOURCE_CUSTOMER.equals(sourceType)) {
            ErpCustomerDO customer = (ErpCustomerDO) row;
            if (customer == null || !CommonStatusEnum.isDisable(customer.getStatus())) {
                throw exception(CUSTOMER_NOT_ENABLE, customer == null ? "" : customer.getName());
            }
            return;
        }
        if (SOURCE_PRODUCT.equals(sourceType)) {
            ErpProductDO product = (ErpProductDO) row;
            if (product == null || !CommonStatusEnum.isDisable(product.getStatus())) {
                throw exception(PRODUCT_NOT_ENABLE, product == null ? "" : product.getName());
            }
            return;
        }
        ErpWarehouseDO warehouse = (ErpWarehouseDO) row;
        if (warehouse == null || !CommonStatusEnum.isDisable(warehouse.getStatus())) {
            throw exception(WAREHOUSE_NOT_ENABLE, warehouse == null ? "" : warehouse.getName());
        }
    }

    private void fillDisabledByName(List<ErpRecycleBinRespVO> list) {
        Set<Long> userIds = convertSet(list, ErpRecycleBinRespVO::getDisabledBy);
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        list.forEach(item -> {
            AdminUserRespDTO user = userMap.get(item.getDisabledBy());
            if (user != null) {
                item.setDisabledByName(user.getNickname());
            }
        });
    }

    private String normalizeSourceType(String sourceType) {
        String normalized = trimToNull(sourceType);
        if (normalized == null) {
            return null;
        }
        switch (normalized) {
            case SOURCE_SUPPLIER:
            case SOURCE_CUSTOMER:
            case SOURCE_PRODUCT:
            case SOURCE_WAREHOUSE:
                return normalized;
            default:
                throw new IllegalArgumentException("Invalid recycle-bin sourceType: " + sourceType);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return StrUtil.trim(value);
    }

}
