package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.*;

// TODO 芋艿：记录操作日志

/**
 * ERP 销售订单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpSaleOrderServiceImpl implements ErpSaleOrderService {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_order";

    @Resource
    private ErpSaleOrderMapper saleOrderMapper;
    @Resource
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Resource
    private ErpProductMapper productMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpProductBatchNoValidator productBatchNoValidator;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockService stockService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleOrder(ErpSaleOrderSaveReqVO createReqVO) {
        fieldPermissionMasker.clearSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        fieldPermissionMasker.clearSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, createReqVO, createReqVO.getItems());
        // 1.1 校验订单项的有效性
        Long saleDeptId = resolveSaleDeptId(createReqVO.getDeptId());
        List<ErpSaleOrderItemDO> saleOrderItems = validateSaleOrderItems(createReqVO.getItems(), saleDeptId);
        // 1.2 校验客户
        customerService.validateCustomerForSale(createReqVO.getCustomerId(), saleDeptId);
        // 1.3 校验结算账户
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1.4 校验销售人员
        if (createReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(createReqVO.getSaleUserId());
        }
        // 1.5 生成订单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_ORDER_NO_PREFIX);
        if (saleOrderMapper.selectByNo(no) != null) {
            throw exception(SALE_ORDER_NO_EXISTS);
        }

        // 2.1 插入订单
        ErpSaleOrderDO saleOrder = BeanUtils.toBean(createReqVO, ErpSaleOrderDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        calculateTotalPrice(saleOrder, saleOrderItems);
        saleDocumentDefaultService.fillCreateDefaults(saleOrder);
        saleOrderMapper.insert(saleOrder);
        // 2.2 插入订单项
        saleOrderItems.forEach(o -> o.setOrderId(saleOrder.getId()));
        saleOrderItemMapper.insertBatch(saleOrderItems);
        operateLogService.recordCreate(ERP_SALE_ORDER_TYPE, saleOrder.getId(), saleOrder.getNo());
        return saleOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOrder(ErpSaleOrderSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpSaleOrderDO saleOrder = validateSaleOrderExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(saleOrder.getStatus())) {
            throw exception(SALE_ORDER_UPDATE_FAIL_APPROVE, saleOrder.getNo());
        }
        fieldPermissionMasker.preserveSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, saleOrder);
        fieldPermissionMasker.preserveSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO, updateReqVO.getItems(),
                saleOrderItemMapper.selectListByOrderId(updateReqVO.getId()));
        // 1.2 校验客户
        customerService.validateCustomerForSale(updateReqVO.getCustomerId(),
                updateReqVO.getDeptId() != null ? updateReqVO.getDeptId() : saleOrder.getDeptId());
        // 1.3 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        // 1.4 校验销售人员
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        // 1.5 校验订单项的有效性
        Long saleDeptId = updateReqVO.getDeptId() != null ? updateReqVO.getDeptId() : saleOrder.getDeptId();
        List<ErpSaleOrderItemDO> saleOrderItems = validateSaleOrderItems(updateReqVO.getItems(), saleDeptId);

        // 2.1 更新订单
        ErpSaleOrderDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleOrderDO.class);
        calculateTotalPrice(updateObj, saleOrderItems);
        saleOrderMapper.updateById(updateObj);
        // 2.2 更新订单项
        updateSaleOrderItemList(updateReqVO.getId(), saleOrderItems);
        operateLogService.recordUpdate(ERP_SALE_ORDER_TYPE, updateReqVO.getId(), saleOrder.getNo());
    }

    @Override
    public void updateSaleOrderRemark(ErpSaleUpdateRemarkReqVO updateReqVO) {
        ErpSaleOrderDO saleOrder = validateSaleOrderExists(updateReqVO.getId());
        saleOrderMapper.updateById(new ErpSaleOrderDO()
                .setId(updateReqVO.getId())
                .setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_SALE_ORDER_TYPE, updateReqVO.getId(), saleOrder.getNo());
    }

    private void calculateTotalPrice(ErpSaleOrderDO saleOrder, List<ErpSaleOrderItemDO> saleOrderItems) {
        saleOrder.setTotalCount(getSumValue(saleOrderItems, ErpSaleOrderItemDO::getCount, BigDecimal::add));
        saleOrder.setTotalProductPrice(getSumValue(saleOrderItems, ErpSaleOrderItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        saleOrder.setTotalTaxPrice(BigDecimal.ZERO);
        saleOrder.setTotalPrice(saleOrder.getTotalProductPrice());
        // 计算优惠价格
        if (saleOrder.getDiscountPercent() == null) {
            saleOrder.setDiscountPercent(BigDecimal.ZERO);
        }
        BigDecimal feeAmount = saleOrder.getFeeAmount() == null ? BigDecimal.ZERO : saleOrder.getFeeAmount();
        saleOrder.setFeeAmount(feeAmount);
        saleOrder.setDiscountPrice(MoneyUtils.priceMultiplyPercent(saleOrder.getTotalPrice(), saleOrder.getDiscountPercent()));
        saleOrder.setTotalPrice(saleOrder.getTotalPrice().subtract(saleOrder.getDiscountPrice()).add(feeAmount));
    }

    private Long resolveSaleDeptId(Long deptId) {
        return deptId != null ? deptId : getLoginUserDeptId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOrderStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(SALE_ORDER_PROCESS_FAIL);
        }
        // 1.1 校验存在
        ErpSaleOrderDO saleOrder = validateSaleOrderExists(id);
        // 1.2 校验状态
        if (!ErpAuditStatus.PROCESS.getStatus().equals(saleOrder.getStatus())) {
            throw exception(SALE_ORDER_APPROVE_FAIL);
        }

        // 2. 更新状态
        int updateCount = saleOrderMapper.updateByIdAndStatus(id, saleOrder.getStatus(),
                new ErpSaleOrderDO().setStatus(ErpAuditStatus.APPROVE.getStatus()));
        if (updateCount == 0) {
            throw exception(SALE_ORDER_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_SALE_ORDER_TYPE, id, saleOrder.getNo(), true);
    }

    private List<ErpSaleOrderItemDO> validateSaleOrderItems(List<ErpSaleOrderSaveReqVO.Item> list, Long saleDeptId) {
        // 1. 校验产品存在
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() ->
                productService.validProductList(convertSet(list, ErpSaleOrderSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoRequired(list, productMap,
                ErpSaleOrderSaveReqVO.Item::getProductId, ErpSaleOrderSaveReqVO.Item::getBatchNo);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpSaleOrderSaveReqVO.Item::getProductId, ErpSaleOrderSaveReqVO.Item::getBatchNo);
        list.forEach(item -> {
            if (item.getWarehouseId() == null && item.getProductId() != null) {
                ErpProductDO product = productMap.get(item.getProductId());
                item.setWarehouseId(product == null ? null : product.getDefaultWarehouseId());
            }
        });
        Set<Long> warehouseIds = convertSet(list, ErpSaleOrderSaveReqVO.Item::getWarehouseId);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(
                warehouseService.validSaleSelectableWarehouseListForDept(warehouseIds, saleDeptId),
                ErpWarehouseDO::getId);
        // 2. 转化为 ErpSaleOrderItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleOrderItemDO.class, item -> {
            ErpProductDO product = productMap.get(item.getProductId());
            item.setProductUnitId(product.getUnitId());
            fillProductWeightAndPackage(item, product);
            fillDeptIdFromWarehouse(item, warehouseMap);
            item.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            if (item.getGiftFlag()) {
                item.setProductPrice(BigDecimal.ZERO);
                item.setTotalPrice(BigDecimal.ZERO);
                item.setTaxPercent(null);
                item.setTaxPrice(BigDecimal.ZERO);
                return;
            }
            item.setTaxPercent(null);
            item.setTaxPrice(BigDecimal.ZERO);
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
        }));
    }

    private void fillDeptIdFromWarehouse(ErpSaleOrderItemDO item, Map<Long, ErpWarehouseDO> warehouseMap) {
        if (item.getDeptId() == null && item.getWarehouseId() != null) {
            ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                    stockService.getStock(item.getProductId(), item.getWarehouseId()));
            if (stock != null && stock.getDeptId() != null) {
                item.setDeptId(stock.getDeptId());
                return;
            }
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            item.setDeptId(warehouse == null ? null : warehouse.getDeptId());
        }
    }

    private void fillProductWeightAndPackage(ErpSaleOrderItemDO item, ErpProductDO product) {
        if (product == null) {
            return;
        }
        if (item.getWeight() == null) {
            item.setWeight(product.getWeight());
        }
        if (item.getPackageQty() == null) {
            item.setPackageQty(product.getPackageQty());
        }
    }

    private void updateSaleOrderItemList(Long id, List<ErpSaleOrderItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpSaleOrderItemDO> oldList = saleOrderItemMapper.selectListByOrderId(id);
        List<List<ErpSaleOrderItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setOrderId(id));
            saleOrderItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            saleOrderItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            saleOrderItemMapper.deleteByIds(convertList(diffList.get(2), ErpSaleOrderItemDO::getId));
        }
    }

    @Override
    public void updateSaleOrderOutCount(Long id, Map<Long, BigDecimal> outCountMap) {
        List<ErpSaleOrderItemDO> orderItems = saleOrderItemMapper.selectListByOrderId(id);
        // 1. 更新每个销售订单项
        orderItems.forEach(item -> {
            BigDecimal outCount = outCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (item.getOutCount().equals(outCount)) {
                return;
            }
            if (outCount.compareTo(item.getCount()) > 0) {
                throw exception(SALE_ORDER_ITEM_OUT_FAIL_PRODUCT_EXCEED,
                        getProductNameIgnoreDataPermission(item.getProductId()), item.getCount());
            }
            saleOrderItemMapper.updateById(new ErpSaleOrderItemDO().setId(item.getId()).setOutCount(outCount));
        });
        // 2. 更新销售订单
        BigDecimal totalOutCount = getSumValue(outCountMap.values(), value -> value, BigDecimal::add, BigDecimal.ZERO);
        saleOrderMapper.updateById(new ErpSaleOrderDO().setId(id).setOutCount(totalOutCount));
    }

    @Override
    public void updateSaleOrderReturnCount(Long orderId, Map<Long, BigDecimal> returnCountMap) {
        List<ErpSaleOrderItemDO> orderItems = saleOrderItemMapper.selectListByOrderId(orderId);
        // 1. 更新每个销售订单项
        orderItems.forEach(item -> {
            BigDecimal returnCount = returnCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (item.getReturnCount().equals(returnCount)) {
                return;
            }
            if (returnCount.compareTo(item.getOutCount()) > 0) {
                throw exception(SALE_ORDER_ITEM_RETURN_FAIL_OUT_EXCEED,
                        getProductNameIgnoreDataPermission(item.getProductId()), item.getOutCount());
            }
            saleOrderItemMapper.updateById(new ErpSaleOrderItemDO().setId(item.getId()).setReturnCount(returnCount));
        });
        // 2. 更新销售订单
        BigDecimal totalReturnCount = getSumValue(returnCountMap.values(), value -> value, BigDecimal::add, BigDecimal.ZERO);
        saleOrderMapper.updateById(new ErpSaleOrderDO().setId(orderId).setReturnCount(totalReturnCount));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleOrder(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpSaleOrderDO> saleOrders = saleOrderMapper.selectByIds(ids);
        if (CollUtil.isEmpty(saleOrders)) {
            return;
        }
        saleOrders.forEach(saleOrder -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(saleOrder.getStatus())) {
                throw exception(SALE_ORDER_DELETE_FAIL_APPROVE, saleOrder.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        saleOrders.forEach(saleOrder -> {
            // 2.1 删除订单
            saleOrderMapper.deleteById(saleOrder.getId());
            // 2.2 删除订单项
            saleOrderItemMapper.deleteByOrderId(saleOrder.getId());
            operateLogService.recordDelete(ERP_SALE_ORDER_TYPE, saleOrder.getId(), saleOrder.getNo());
        });
    }

    private ErpSaleOrderDO validateSaleOrderExists(Long id) {
        ErpSaleOrderDO saleOrder = saleOrderMapper.selectById(id);
        if (saleOrder == null) {
            throw exception(SALE_ORDER_NOT_EXISTS);
        }
        return saleOrder;
    }

    private String getProductNameIgnoreDataPermission(Long productId) {
        ErpProductDO product = DataPermissionUtils.executeIgnore(() -> productService.getProduct(productId));
        return product == null ? String.valueOf(productId) : product.getName();
    }

    @Override
    public ErpSaleOrderDO getSaleOrder(Long id) {
        return saleOrderMapper.selectById(id);
    }

    @Override
    public ErpSaleOrderDO validateSaleOrder(Long id) {
        ErpSaleOrderDO saleOrder = validateSaleOrderExists(id);
        if (ObjectUtil.notEqual(saleOrder.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(SALE_ORDER_NOT_APPROVE);
        }
        return saleOrder;
    }

    @Override
    public PageResult<ErpSaleOrderDO> getSaleOrderPage(ErpSaleOrderPageReqVO pageReqVO) {
        return saleOrderMapper.selectPage(pageReqVO);
    }

    // ==================== 订单项 ====================

    @Override
    public List<ErpSaleOrderItemDO> getSaleOrderItemListByOrderId(Long orderId) {
        return saleOrderItemMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<ErpSaleOrderItemDO> getSaleOrderItemListByOrderIds(Collection<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyList();
        }
        return saleOrderItemMapper.selectListByOrderIds(orderIds);
    }

    @Override
    public ErpSaleOrderImportRespVO parseImportData(List<ErpSaleOrderImportExcelVO> list) {
        ErpSaleOrderImportRespVO respVO = new ErpSaleOrderImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        LinkedHashSet<String> productCodes = new LinkedHashSet<>();
        list.forEach(row -> {
            if (row.getProductCode() != null && !row.getProductCode().isEmpty()) {
                productCodes.add(row.getProductCode());
            }
        });
        Map<String, ErpProductDO> productMap = convertMap(
                DataPermissionUtils.executeIgnore(() -> productMapper.selectListByCodes(productCodes)), ErpProductDO::getCode);
        Map<Long, ErpProductRespVO> productVOMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertList(productMap.values(), ErpProductDO::getId)));
        for (int i = 0; i < list.size(); i++) {
            ErpSaleOrderImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (row.getProductCode() == null || row.getProductCode().isEmpty()) {
                respVO.getFailureDetails().add(new ErpSaleOrderImportRespVO.FailureItem(rowNo, null, "产品编码不能为空"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpProductDO product = productMap.get(row.getProductCode());
            if (product == null) {
                respVO.getFailureDetails().add(new ErpSaleOrderImportRespVO.FailureItem(rowNo, row.getProductCode(), "产品不存在"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            if (row.getCount() == null || row.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                respVO.getFailureDetails().add(new ErpSaleOrderImportRespVO.FailureItem(rowNo, row.getProductCode(), "数量必须大于 0"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpSaleOrderRespVO.Item item = new ErpSaleOrderRespVO.Item();
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductName(product.getName());
            item.setProductUnitId(product.getUnitId());
            item.setProductUnitName(productVOMap.get(product.getId()) == null ? null : productVOMap.get(product.getId()).getUnitName());
            item.setProductPrice(row.getProductPrice() != null ? row.getProductPrice() : product.getSalePrice());
            item.setCount(row.getCount());
            item.setRemark(row.getRemark());
            respVO.getItems().add(item);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

}
