package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderDetailImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.*;

// TODO 芋艿：记录操作日�?

/**
 * ERP 采购订单 Service 实现�?
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpPurchaseOrderServiceImpl implements ErpPurchaseOrderService {

    @Resource
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpProductMapper productMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpProductBatchNoValidator productBatchNoValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseOrder(ErpPurchaseOrderSaveReqVO createReqVO) {
        // 1.1 校验订单项的有效�?
        List<ErpPurchaseOrderItemDO> purchaseOrderItems = validatePurchaseOrderItems(createReqVO.getItems());
        // 1.2 校验供应�?
        supplierService.validateSupplier(createReqVO.getSupplierId());
        // 1.3 校验结算账户
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1.4 生成订单号，并校验唯一�?
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_ORDER_NO_PREFIX);
        if (purchaseOrderMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_ORDER_NO_EXISTS);
        }

        // 2.1 插入订单
        ErpPurchaseOrderDO purchaseOrder = BeanUtils.toBean(createReqVO, ErpPurchaseOrderDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        if (purchaseOrder.getOrderTime() == null) {
            purchaseOrder.setOrderTime(LocalDateTime.now());
        }
        purchaseDocumentDefaultService.fillCreateDefaults(purchaseOrder);
        calculateTotalPrice(purchaseOrder, purchaseOrderItems);
        purchaseDocumentDefaultService.fillCreateAuditDefaults(purchaseOrder);
        purchaseOrderMapper.insert(purchaseOrder);
        // 2.2 插入订单项
        purchaseOrderItems.forEach(o -> o.setOrderId(purchaseOrder.getId()));
        purchaseDocumentDefaultService.fillCreateAuditDefaults(purchaseOrderItems);
        purchaseOrderItemMapper.insertBatch(purchaseOrderItems);
        operateLogService.recordCreate(ERP_PURCHASE_ORDER_TYPE, purchaseOrder.getId(), purchaseOrder.getNo());
        return purchaseOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseOrder(ErpPurchaseOrderSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpPurchaseOrderDO purchaseOrder = validatePurchaseOrderExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseOrder.getStatus())) {
            throw exception(PURCHASE_ORDER_UPDATE_FAIL_APPROVE, purchaseOrder.getNo());
        }
        // 1.2 校验供应�?
        supplierService.validateSupplier(updateReqVO.getSupplierId());
        // 1.3 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        // 1.4 校验订单项的有效�?
        List<ErpPurchaseOrderItemDO> purchaseOrderItems = validatePurchaseOrderItems(updateReqVO.getItems());
        // 1.5 校验已入库项不允许修改赠品标�?
        validateGiftModify(updateReqVO.getId(), updateReqVO.getItems());

        // 2.1 更新订单
        ErpPurchaseOrderDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseOrderDO.class);
        if (updateObj.getOrderTime() == null) {
            updateObj.setOrderTime(LocalDateTime.now());
        }
        if (updateObj.getPurchaser() == null) {
            updateObj.setPurchaser(purchaseOrder.getPurchaser());
        }
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(purchaseOrder.getDeptId());
        }
        purchaseDocumentDefaultService.fillCreateDefaults(updateObj);
        calculateTotalPrice(updateObj, purchaseOrderItems);
        purchaseOrderMapper.updateById(updateObj);
        // 2.2 更新订单�?
        updatePurchaseOrderItemList(updateReqVO.getId(), purchaseOrderItems);
        operateLogService.recordUpdate(ERP_PURCHASE_ORDER_TYPE, updateReqVO.getId(), purchaseOrder.getNo());
    }

    private void validateGiftModify(Long orderId, List<ErpPurchaseOrderSaveReqVO.Item> newItems) {
        List<ErpPurchaseOrderItemDO> oldItems = purchaseOrderItemMapper.selectListByOrderId(orderId);
        Map<Long, ErpPurchaseOrderItemDO> oldItemMap = convertMap(oldItems, ErpPurchaseOrderItemDO::getId);
        for (ErpPurchaseOrderSaveReqVO.Item newItem : newItems) {
            if (newItem.getId() == null) {
                continue; // 新增项无需校验
            }
            ErpPurchaseOrderItemDO oldItem = oldItemMap.get(newItem.getId());
            if (oldItem == null) {
                continue;
            }
            // 已有入库记录时，不允许修�?gift 标记
            if (oldItem.getInCount() != null && oldItem.getInCount().compareTo(BigDecimal.ZERO) > 0) {
                boolean oldGift = Boolean.TRUE.equals(oldItem.getGift());
                boolean newGift = Boolean.TRUE.equals(newItem.getGift());
                if (oldGift != newGift) {
                    throw exception(PURCHASE_ORDER_ITEM_GIFT_MODIFY_FAIL_HAS_IN,
                            productService.getProduct(oldItem.getProductId()).getName());
                }
            }
        }
    }

    private void calculateTotalPrice(ErpPurchaseOrderDO purchaseOrder, List<ErpPurchaseOrderItemDO> purchaseOrderItems) {
        purchaseOrder.setTotalCount(getSumValue(purchaseOrderItems, ErpPurchaseOrderItemDO::getCount, BigDecimal::add));
        // totalProductPrice / totalTaxPrice 排除赠品�?
        purchaseOrder.setTotalProductPrice(getSumValue(purchaseOrderItems,
                item -> Boolean.TRUE.equals(item.getGift()) ? BigDecimal.ZERO : item.getTotalPrice(),
                BigDecimal::add, BigDecimal.ZERO));
        purchaseOrder.setTotalTaxPrice(BigDecimal.ZERO);
        BigDecimal feeAmount = purchaseOrder.getFeeAmount() == null ? BigDecimal.ZERO : purchaseOrder.getFeeAmount();
        purchaseOrder.setFeeAmount(feeAmount);
        purchaseOrder.setTotalPrice(purchaseOrder.getTotalProductPrice());
        // 计算优惠价格
        if (purchaseOrder.getDiscountPercent() == null) {
            purchaseOrder.setDiscountPercent(BigDecimal.ZERO);
        }
        purchaseOrder.setDiscountPrice(MoneyUtils.priceMultiplyPercent(purchaseOrder.getTotalPrice(), purchaseOrder.getDiscountPercent()));
        purchaseOrder.setTotalPrice(purchaseOrder.getTotalPrice().subtract(purchaseOrder.getDiscountPrice()).add(feeAmount));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseOrderStatus(Long id, Integer status) {
        ErpPurchaseOrderDO purchaseOrder = validatePurchaseOrderExists(id);
        if (ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            orderPurchaseOrder(id, purchaseOrder);
            return;
        }
        if (ErpAuditStatus.PROCESS.getStatus().equals(status)) {
            reverseOrderPurchaseOrder(id, purchaseOrder);
            return;
        }
        throw exception(PURCHASE_ORDER_PROCESS_FAIL);
    }

    private void orderPurchaseOrder(Long id, ErpPurchaseOrderDO purchaseOrder) {
        if (!ErpAuditStatus.PROCESS.getStatus().equals(purchaseOrder.getStatus())) {
            throw exception(PURCHASE_ORDER_APPROVE_FAIL);
        }

        int updateCount = purchaseOrderMapper.updateByIdAndStatus(id, purchaseOrder.getStatus(),
                new ErpPurchaseOrderDO().setStatus(ErpAuditStatus.APPROVE.getStatus()));
        if (updateCount == 0) {
            throw exception(PURCHASE_ORDER_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_PURCHASE_ORDER_TYPE, "下订", id, purchaseOrder.getNo());
    }

    private void reverseOrderPurchaseOrder(Long id, ErpPurchaseOrderDO purchaseOrder) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(purchaseOrder.getStatus())) {
            throw exception(PURCHASE_ORDER_PROCESS_FAIL);
        }
        if (purchaseOrder.getInCount() != null && purchaseOrder.getInCount().compareTo(BigDecimal.ZERO) > 0) {
            throw exception(PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN);
        }
        List<ErpPurchaseInDO> approvedPurchaseIns = purchaseInMapper.selectListByOrderIdAndStatus(id,
                ErpAuditStatus.APPROVE.getStatus());
        if (CollUtil.isNotEmpty(approvedPurchaseIns)) {
            throw exception(PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN);
        }
        if (purchaseOrder.getReturnCount() != null && purchaseOrder.getReturnCount().compareTo(BigDecimal.ZERO) > 0) {
            throw exception(PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN);
        }

        int updateCount = purchaseOrderMapper.updateByIdAndStatus(id, purchaseOrder.getStatus(),
                new ErpPurchaseOrderDO().setStatus(ErpAuditStatus.PROCESS.getStatus()));
        if (updateCount == 0) {
            throw exception(PURCHASE_ORDER_PROCESS_FAIL);
        }
        operateLogService.recordStatus(ERP_PURCHASE_ORDER_TYPE, "反下订", id, purchaseOrder.getNo());
    }

    private List<ErpPurchaseOrderItemDO> validatePurchaseOrderItems(List<ErpPurchaseOrderSaveReqVO.Item> list) {
        // 0. 校验每项的数量和单价必须大于 0（赠品行仅校验数量，单价强制�?0�?
        if (CollUtil.isNotEmpty(list)) {
            for (ErpPurchaseOrderSaveReqVO.Item item : list) {
                if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw exception(PURCHASE_ORDER_ITEM_COUNT_POSITIVE);
                }
                // 赠品行单价会被强制为 0，跳过单价校�?
                if (Boolean.TRUE.equals(item.getGift())) {
                    continue;
                }
                if (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw exception(PURCHASE_ORDER_ITEM_PRICE_POSITIVE);
                }
            }
        }
        Set<String> itemKeySet = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(list)) {
            for (ErpPurchaseOrderSaveReqVO.Item item : list) {
                String productKey = StrUtil.blankToDefault(item.getProductCode(), String.valueOf(item.getProductId()));
                String itemKey = item.getProductId() + "|" + item.getWarehouseId() + "|"
                        + (Boolean.TRUE.equals(item.getGift()) ? 1 : 0);
                if (!itemKeySet.add(itemKey)) {
                    throw exception(PURCHASE_ORDER_ITEM_DUPLICATE, productKey);
                }
            }
        }
        // 1. 校验产品存在
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() -> productService.validProductList(
                convertSet(list, ErpPurchaseOrderSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpPurchaseOrderSaveReqVO.Item::getProductId, ErpPurchaseOrderSaveReqVO.Item::getBatchNo);
        Set<Long> warehouseIds = convertSet(list, ErpPurchaseOrderSaveReqVO.Item::getWarehouseId);
        warehouseService.validPurchaseWarehouseList(warehouseIds);
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);
        // 2. 转化�?ErpPurchaseOrderItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpPurchaseOrderItemDO.class, item -> {
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            if (item.getDeptId() == null && item.getWarehouseId() != null) {
                ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
                item.setDeptId(warehouse == null ? null : warehouse.getDeptId());
            }
            // 赠品行：强制 productPrice=0, totalPrice=0, taxPrice=0
            if (Boolean.TRUE.equals(item.getGift())) {
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

    private void updatePurchaseOrderItemList(Long id, List<ErpPurchaseOrderItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpPurchaseOrderItemDO> oldList = purchaseOrderItemMapper.selectListByOrderId(id);
        List<List<ErpPurchaseOrderItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // Batch insert, update and delete items.
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setOrderId(id));
            purchaseDocumentDefaultService.fillCreateAuditDefaults(diffList.get(0));
            purchaseOrderItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            purchaseOrderItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            purchaseOrderItemMapper.deleteByIds(convertList(diffList.get(2), ErpPurchaseOrderItemDO::getId));
        }
    }

    @Override
    public void updatePurchaseOrderInCount(Long id, Map<Long, BigDecimal> inCountMap) {
        List<ErpPurchaseOrderItemDO> orderItems = purchaseOrderItemMapper.selectListByOrderId(id);
        // 1. 更新每个采购订单�?
        orderItems.forEach(item -> {
            BigDecimal inCount = inCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            BigDecimal oldInCount = item.getInCount() != null ? item.getInCount() : BigDecimal.ZERO;
            if (oldInCount.compareTo(inCount) == 0) {
                return;
            }
            BigDecimal orderCount = item.getCount() != null ? item.getCount() : BigDecimal.ZERO;
            if (inCount.compareTo(orderCount) > 0) {
                ErpProductDO product = productService.getProduct(item.getProductId());
                throw exception(PURCHASE_ORDER_ITEM_IN_FAIL_PRODUCT_EXCEED,
                        product != null ? product.getName() : String.valueOf(item.getProductId()), orderCount);
            }
            purchaseOrderItemMapper.updateById(new ErpPurchaseOrderItemDO().setId(item.getId()).setInCount(inCount));
        });
        // 2. 更新采购订单
        BigDecimal totalInCount = getSumValue(inCountMap.values(), value -> value, BigDecimal::add, BigDecimal.ZERO);
        purchaseOrderMapper.updateById(new ErpPurchaseOrderDO().setId(id).setInCount(totalInCount));
    }

    @Override
    public void updatePurchaseOrderReturnCount(Long orderId, Map<Long, BigDecimal> returnCountMap) {
        List<ErpPurchaseOrderItemDO> orderItems = purchaseOrderItemMapper.selectListByOrderId(orderId);
        // 1. 更新每个采购订单�?
        orderItems.forEach(item -> {
            BigDecimal returnCount = returnCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            BigDecimal oldReturnCount = item.getReturnCount() != null ? item.getReturnCount() : BigDecimal.ZERO;
            if (oldReturnCount.compareTo(returnCount) == 0) {
                return;
            }
            BigDecimal inCount = item.getInCount() != null ? item.getInCount() : BigDecimal.ZERO;
            if (returnCount.compareTo(inCount) > 0) {
                ErpProductDO product = productService.getProduct(item.getProductId());
                throw exception(PURCHASE_ORDER_ITEM_RETURN_FAIL_IN_EXCEED,
                        product != null ? product.getName() : String.valueOf(item.getProductId()), inCount);
            }
            purchaseOrderItemMapper.updateById(new ErpPurchaseOrderItemDO().setId(item.getId()).setReturnCount(returnCount));
        });
        // 2. 更新采购订单
        BigDecimal totalReturnCount = getSumValue(returnCountMap.values(), value -> value, BigDecimal::add, BigDecimal.ZERO);
        purchaseOrderMapper.updateById(new ErpPurchaseOrderDO().setId(orderId).setReturnCount(totalReturnCount));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseOrder(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpPurchaseOrderDO> purchaseOrders = purchaseOrderMapper.selectByIds(ids);
        if (CollUtil.isEmpty(purchaseOrders)) {
            return;
        }
        purchaseOrders.forEach(purchaseOrder -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseOrder.getStatus())) {
                throw exception(PURCHASE_ORDER_DELETE_FAIL_APPROVE, purchaseOrder.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        purchaseOrders.forEach(purchaseOrder -> {
            // 2.1 删除订单
            purchaseOrderMapper.deleteById(purchaseOrder.getId());
            // 2.2 删除订单�?
            purchaseOrderItemMapper.deleteByOrderId(purchaseOrder.getId());
            operateLogService.recordDelete(ERP_PURCHASE_ORDER_TYPE, purchaseOrder.getId(), purchaseOrder.getNo());
        });
    }

    private ErpPurchaseOrderDO validatePurchaseOrderExists(Long id) {
        ErpPurchaseOrderDO purchaseOrder = purchaseOrderMapper.selectById(id);
        if (purchaseOrder == null) {
            throw exception(PURCHASE_ORDER_NOT_EXISTS);
        }
        return purchaseOrder;
    }

    @Override
    public ErpPurchaseOrderDO getPurchaseOrder(Long id) {
        return purchaseOrderMapper.selectById(id);
    }

    @Override
    public ErpPurchaseOrderDO validatePurchaseOrder(Long id) {
        ErpPurchaseOrderDO purchaseOrder = validatePurchaseOrderExists(id);
        if (ObjectUtil.notEqual(purchaseOrder.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(PURCHASE_ORDER_NOT_APPROVE);
        }
        return purchaseOrder;
    }

    @Override
    public PageResult<ErpPurchaseOrderDO> getPurchaseOrderPage(ErpPurchaseOrderPageReqVO pageReqVO) {
        return purchaseOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPurchaseOrderDO> getPurchaseOrderList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return purchaseOrderMapper.selectByIds(ids);
    }

    // ==================== 订单�?====================

    @Override
    public List<ErpPurchaseOrderItemDO> getPurchaseOrderItemListByOrderId(Long orderId) {
        return purchaseOrderItemMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<ErpPurchaseOrderItemDO> getPurchaseOrderItemListByOrderIds(Collection<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyList();
        }
        return purchaseOrderItemMapper.selectListByOrderIds(orderIds);
    }

    @Override
    public ErpPurchaseOrderImportRespVO parseImportData(List<ErpPurchaseOrderDetailImportExcelVO> list) {
        ErpPurchaseOrderImportRespVO respVO = new ErpPurchaseOrderImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        LinkedHashSet<String> productCodes = new LinkedHashSet<>();
        list.forEach(row -> {
            if (StrUtil.isNotBlank(row.getProductCode())) {
                productCodes.add(row.getProductCode());
            }
        });
        Map<String, ErpProductDO> productMap = productMapper.selectListByCodes(productCodes).stream()
                .collect(Collectors.toMap(ErpProductDO::getCode, product -> product, (a, b) -> a));
        Map<Long, ErpProductRespVO> productVOMap = productService.getProductVOMap(
                convertSet(productMap.values(), ErpProductDO::getId));

        for (int i = 0; i < list.size(); i++) {
            ErpPurchaseOrderDetailImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (StrUtil.isBlank(row.getProductCode())) {
                respVO.getFailureDetails().add(new ErpPurchaseOrderImportRespVO.FailureItem(rowNo, null, "产品编码不能为空"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpProductDO product = productMap.get(row.getProductCode());
            if (product == null) {
                respVO.getFailureDetails().add(new ErpPurchaseOrderImportRespVO.FailureItem(rowNo, row.getProductCode(), "Product not found"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            if (row.getCount() == null || row.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                respVO.getFailureDetails().add(new ErpPurchaseOrderImportRespVO.FailureItem(rowNo, row.getProductCode(), "数量必须大于 0"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            boolean gift = parseGiftFlag(row.getGift());
            BigDecimal productPrice = resolveImportProductPrice(row.getProductPrice(), product, gift);
            if (!gift && !isPositive(productPrice)) {
                respVO.getFailureDetails().add(new ErpPurchaseOrderImportRespVO.FailureItem(rowNo, row.getProductCode(), "产品单价必须大于 0"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpPurchaseOrderSaveReqVO.Item item = new ErpPurchaseOrderSaveReqVO.Item();
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductUnitId(product.getUnitId());
            item.setProductUnitName(productVOMap.get(product.getId()) == null ? null : productVOMap.get(product.getId()).getUnitName());
            item.setWarehouseId(product.getDefaultWarehouseId());
            item.setProductPrice(productPrice);
            item.setCount(row.getCount());

            item.setRemark(null);
            item.setGift(gift);
            respVO.getItems().add(item);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpPurchaseOrderImportResultRespVO importPurchaseOrderList(List<ErpPurchaseOrderImportExcelVO> list) {
        ErpPurchaseOrderImportResultRespVO respVO = new ErpPurchaseOrderImportResultRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        Map<String, ErpSupplierDO> supplierMap = buildSupplierMap();
        Map<String, ErpProductDO> productMap = productMapper.selectListByCodes(extractOrderImportProductCodes(list)).stream()
                .collect(Collectors.toMap(ErpProductDO::getCode, product -> product, (a, b) -> a));
        Map<Long, ErpProductRespVO> productVOMap = productService.getProductVOMap(
                convertSet(productMap.values(), ErpProductDO::getId));

        List<OrderImportGroup> groups = new ArrayList<>();
        OrderImportGroup currentGroup = null;
        for (int i = 0; i < list.size(); i++) {
            ErpPurchaseOrderImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (isBlankImportRow(row)) {
                continue;
            }
            boolean hasMain = hasOrderMainFields(row);
            boolean hasDetail = hasOrderDetailFields(row);
            if (hasMain) {
                ErpSupplierDO supplier = resolveSupplier(row, supplierMap);
                currentGroup = new OrderImportGroup(rowNo, row, supplier);
                groups.add(currentGroup);
                String orderNo = resolveImportOrderNo(rowNo, row);
                String supplierName = normalize(row.getSupplierName());
                if (StrUtil.isBlank(supplierName)) {
                    addImportFailure(respVO, rowNo, orderNo, null, "Supplier is required");
                } else if (supplier == null) {
                    addImportFailure(respVO, rowNo, orderNo, null, "Supplier not found: " + supplierName);
                } else if (CommonStatusEnum.isDisable(supplier.getStatus())) {
                    addImportFailure(respVO, rowNo, orderNo, null, "Supplier(" + supplier.getName() + ") is disabled");
                }
                validateImportDate(respVO, rowNo, orderNo, null, "采购时间", row.getOrderTime());
            } else if (hasDetail && currentGroup == null) {
                addImportFailure(respVO, rowNo, null, normalize(row.getProductCode()), "明细行前缺少采购订单主表信息");
                continue;
            }
            if (!hasDetail) {
                continue;
            }

            String orderNo = currentGroup == null ? null : resolveImportOrderNo(currentGroup.getRowNo(), currentGroup.getMainRow());
            String productCode = normalize(row.getProductCode());
            boolean valid = true;

            ErpProductDO product = productMap.get(productCode);
            if (StrUtil.isBlank(productCode)) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "Product code is required");
                valid = false;
            } else if (product == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "Product not found");
                valid = false;
            }
            if (row.getItemCount() == null || row.getItemCount().compareTo(BigDecimal.ZERO) <= 0) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "数量必须大于 0");
                valid = false;
            }
            boolean gift = parseGiftFlag(row.getGift());
            if (product != null && !gift && !isPositive(resolveImportProductPrice(row.getProductPrice(), product, false))) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "产品单价必须大于 0");
                valid = false;
            }
            if (!valid) {
                continue;
            }
            currentGroup.getRows().add(new OrderImportRow(rowNo, row, product));
        }

        for (OrderImportGroup group : groups) {
            if (CollUtil.isEmpty(group.getRows())) {
                addImportFailure(respVO, group.getRowNo(), resolveImportOrderNo(group.getRowNo(), group.getMainRow()), null,
                        "Purchase order requires at least one item");
            }
        }

        if (respVO.getFailureCount() > 0) {
            return respVO;
        }

        for (OrderImportGroup group : groups) {
            if (CollUtil.isEmpty(group.getRows())) {
                continue;
            }
            ErpPurchaseOrderSaveReqVO saveReqVO = buildPurchaseOrderSaveReq(group, productVOMap);
            Long orderId = createPurchaseOrder(saveReqVO);
            respVO.getOrderIds().add(orderId);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    private ErpPurchaseOrderSaveReqVO buildPurchaseOrderSaveReq(OrderImportGroup group,
                                                               Map<Long, ErpProductRespVO> productVOMap) {
        ErpPurchaseOrderImportExcelVO firstRow = group.getMainRow();
        ErpPurchaseOrderSaveReqVO saveReqVO = new ErpPurchaseOrderSaveReqVO();
        saveReqVO.setSupplierId(group.getSupplier().getId());
        saveReqVO.setOrderTime(parseImportDate(firstRow.getOrderTime(), LocalDateTime.now()));
        saveReqVO.setFactoryOrderNo(firstRow.getFactoryOrderNo());
        saveReqVO.setRemark(firstRow.getRemark());
        saveReqVO.setItems(convertList(group.getRows(), row -> buildPurchaseOrderItem(row, productVOMap)));
        return saveReqVO;
    }

    private ErpPurchaseOrderSaveReqVO.Item buildPurchaseOrderItem(OrderImportRow row,
                                                                  Map<Long, ErpProductRespVO> productVOMap) {
        ErpPurchaseOrderImportExcelVO importRow = row.getRow();
        ErpProductDO product = row.getProduct();
        ErpProductRespVO productVO = productVOMap.get(product.getId());
        ErpPurchaseOrderSaveReqVO.Item item = new ErpPurchaseOrderSaveReqVO.Item();
        item.setProductId(product.getId());
        item.setProductCode(product.getCode());
        item.setProductUnitId(product.getUnitId());
        item.setProductUnitName(productVO == null ? null : productVO.getUnitName());
        item.setWarehouseId(product.getDefaultWarehouseId());
        boolean gift = parseGiftFlag(importRow.getGift());
        item.setProductPrice(resolveImportProductPrice(importRow.getProductPrice(), product, gift));
        item.setCount(importRow.getItemCount());

        item.setRemark(importRow.getItemRemark());
        item.setGift(gift);
        return item;
    }

    private Map<String, ErpSupplierDO> buildSupplierMap() {
        Map<String, ErpSupplierDO> map = new LinkedHashMap<>();
        ErpSupplierPageReqVO pageReqVO = new ErpSupplierPageReqVO();
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        supplierService.getSupplierPage(pageReqVO).getList().forEach(supplier -> putSupplierKeys(map, supplier));
        return map;
    }

    private ErpSupplierDO resolveSupplier(ErpPurchaseOrderImportExcelVO row, Map<String, ErpSupplierDO> supplierMap) {
        return supplierMap.get(normalizeKey(row.getSupplierName()));
    }

    private void putSupplierKeys(Map<String, ErpSupplierDO> map, ErpSupplierDO supplier) {
        putIfNotBlank(map, supplier.getCode(), supplier);
        putIfNotBlank(map, supplier.getOldCode(), supplier);
        putIfNotBlank(map, supplier.getName(), supplier);
        putIfNotBlank(map, supplier.getShortName(), supplier);
    }

    private LinkedHashSet<String> extractOrderImportProductCodes(List<ErpPurchaseOrderImportExcelVO> list) {
        LinkedHashSet<String> productCodes = new LinkedHashSet<>();
        list.forEach(row -> {
            if (StrUtil.isNotBlank(row.getProductCode())) {
                productCodes.add(normalize(row.getProductCode()));
            }
        });
        return productCodes;
    }

    private BigDecimal resolveImportProductPrice(BigDecimal importPrice, ErpProductDO product, boolean gift) {
        if (gift) {
            return BigDecimal.ZERO;
        }
        return importPrice != null ? importPrice : product.getPurchasePrice();
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean parseGiftFlag(String value) {
        String normalized = normalizeKey(value);
        return StrUtil.equalsAny(normalized, "1", "true", "yes", "y", "gift");
    }

    private boolean isBlankImportRow(ErpPurchaseOrderImportExcelVO row) {
        return !hasOrderMainFields(row) && !hasOrderDetailFields(row);
    }

    private boolean hasOrderMainFields(ErpPurchaseOrderImportExcelVO row) {
        return StrUtil.isNotBlank(normalize(row.getNo()))
                || StrUtil.isNotBlank(normalize(row.getFactoryOrderNo()))
                || StrUtil.isNotBlank(normalize(row.getSupplierName()))
                || StrUtil.isNotBlank(normalize(row.getOrderTime()))
                || row.getStatus() != null
                || StrUtil.isNotBlank(normalize(row.getCreatorName()))
                || row.getTotalCount() != null
                || row.getInCount() != null
                || row.getReturnCount() != null
                || row.getTotalPrice() != null
                || StrUtil.isNotBlank(normalize(row.getRemark()));
    }

    private boolean hasOrderDetailFields(ErpPurchaseOrderImportExcelVO row) {
        return StrUtil.isNotBlank(normalize(row.getProductCode()))
                || StrUtil.isNotBlank(normalize(row.getProductName()))
                || StrUtil.isNotBlank(normalize(row.getProductUnitName()))
                || row.getItemCount() != null
                || row.getProductPrice() != null
                || StrUtil.isNotBlank(normalize(row.getGift()))
                || row.getItemTotalPrice() != null

                || StrUtil.isNotBlank(normalize(row.getItemRemark()));
    }

    private String resolveImportOrderNo(Integer rowNo, ErpPurchaseOrderImportExcelVO row) {
        if (StrUtil.isNotBlank(normalize(row.getNo()))) {
            return normalize(row.getNo());
        }
        if (StrUtil.isNotBlank(normalize(row.getFactoryOrderNo()))) {
            return normalize(row.getFactoryOrderNo());
        }
        return "row " + rowNo;
    }

    private boolean validateImportDate(ErpPurchaseOrderImportResultRespVO respVO, Integer rowNo, String orderNo,
                                       String productCode, String label, String value) {
        if (StrUtil.isBlank(value)) {
            return true;
        }
        try {
            parseImportDate(value, null);
            return true;
        } catch (IllegalArgumentException ignored) {
            addImportFailure(respVO, rowNo, orderNo, productCode, label + "格式不正确，请使�?yyyy-MM-dd �?yyyy-MM-dd HH:mm:ss");
            return false;
        }
    }

    private LocalDateTime parseImportDate(String value, LocalDateTime defaultValue) {
        String normalized = normalize(value);
        if (StrUtil.isBlank(normalized)) {
            return defaultValue;
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        }) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format.
            }
        }
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            throw new IllegalArgumentException("Invalid date format: " + value);
        }
    }

    private void addImportFailure(ErpPurchaseOrderImportResultRespVO respVO, Integer rowNo, String orderNo,
                                  String productCode, String reason) {
        respVO.getFailureDetails().add(new ErpPurchaseOrderImportResultRespVO.FailureItem(
                rowNo, null, orderNo, productCode, reason));
        respVO.setFailureCount(respVO.getFailureCount() + 1);
    }

    private <T> void putIfNotBlank(Map<String, T> map, String key, T value) {
        String normalized = normalizeKey(key);
        if (StrUtil.isNotBlank(normalized)) {
            map.putIfAbsent(normalized, value);
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeKey(String value) {
        String normalized = normalize(value);
        if (StrUtil.isBlank(normalized)) {
            return normalized;
        }
        return StrUtil.cleanBlank(normalized).toLowerCase(Locale.ROOT);
    }

    private static class OrderImportGroup {

        private final Integer rowNo;
        private final ErpPurchaseOrderImportExcelVO mainRow;
        private final ErpSupplierDO supplier;
        private final List<OrderImportRow> rows = new ArrayList<>();

        private OrderImportGroup(Integer rowNo, ErpPurchaseOrderImportExcelVO mainRow, ErpSupplierDO supplier) {
            this.rowNo = rowNo;
            this.mainRow = mainRow;
            this.supplier = supplier;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public ErpPurchaseOrderImportExcelVO getMainRow() {
            return mainRow;
        }

        public ErpSupplierDO getSupplier() {
            return supplier;
        }

        public List<OrderImportRow> getRows() {
            return rows;
        }

    }

    private static class OrderImportRow {

        private final Integer rowNo;
        private final ErpPurchaseOrderImportExcelVO row;
        private final ErpProductDO product;

        private OrderImportRow(Integer rowNo, ErpPurchaseOrderImportExcelVO row, ErpProductDO product) {
            this.rowNo = rowNo;
            this.row = row;
            this.product = product;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public ErpPurchaseOrderImportExcelVO getRow() {
            return row;
        }

        public ErpProductDO getProduct() {
            return product;
        }

    }

    @Override
    public List<ErpPurchaseOrderInableItemRespVO> getInableItemsByOrderId(Long orderId) {
        // 1. 校验订单已审�?
        validatePurchaseOrder(orderId);
        // 2. 查询订单�?
        List<ErpPurchaseOrderItemDO> orderItems = purchaseOrderItemMapper.selectListByOrderId(orderId);
        if (CollUtil.isEmpty(orderItems)) {
            return Collections.emptyList();
        }
        // 3. 查产品信息（使用 VO Map，包�?unitName�?
        Map<Long, ErpProductRespVO> productVOMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(orderItems, ErpPurchaseOrderItemDO::getProductId)));
        // 4. 组装可入库明�?
        return convertList(orderItems, item -> {
            BigDecimal inCount = item.getInCount() != null ? item.getInCount() : BigDecimal.ZERO;
            BigDecimal orderCount = item.getCount() != null ? item.getCount() : BigDecimal.ZERO;
            BigDecimal inableCount = orderCount.subtract(inCount).max(BigDecimal.ZERO);
            if (inableCount.compareTo(BigDecimal.ZERO) <= 0) {
                return null; // 已全部入库，跳过
            }
            ErpPurchaseOrderInableItemRespVO vo = new ErpPurchaseOrderInableItemRespVO();
            vo.setOrderItemId(item.getId());
            vo.setProductId(item.getProductId());
            vo.setProductPrice(item.getProductPrice());
            vo.setOrderCount(item.getCount());
            vo.setInCount(inCount);
            vo.setInableCount(inableCount);
            vo.setWarehouseId(item.getWarehouseId());
            vo.setGift(item.getGift());
            // 产品扩展字段
            ErpProductRespVO product = productVOMap.get(item.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductCode(product.getCode());
                vo.setProductUnitName(product.getUnitName());
                vo.setStandard(product.getStandard());
            }
            vo.setVehicleModel(item.getVehicleModel());
            vo.setOriginPlace(item.getOriginPlace());
            vo.setBrand(item.getBrand());
            if (vo.getStandard() == null) {
                vo.setStandard(item.getStandard());
            }
            vo.setDrawingNo(item.getDrawingNo());
            vo.setWarehousePosition(item.getWarehousePosition());
            return vo;
        });
    }

}

