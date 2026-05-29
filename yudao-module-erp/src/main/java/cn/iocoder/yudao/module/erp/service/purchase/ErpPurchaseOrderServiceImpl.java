package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

// TODO 芋艿：记录操作日志

/**
 * ERP 采购订单 Service 实现类
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseOrder(ErpPurchaseOrderSaveReqVO createReqVO) {
        // 1.1 校验订单项的有效性
        List<ErpPurchaseOrderItemDO> purchaseOrderItems = validatePurchaseOrderItems(createReqVO.getItems());
        // 1.2 校验供应商
        supplierService.validateSupplier(createReqVO.getSupplierId());
        // 1.3 校验结算账户
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1.4 生成订单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_ORDER_NO_PREFIX);
        if (purchaseOrderMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_ORDER_NO_EXISTS);
        }

        // 2.1 插入订单
        ErpPurchaseOrderDO purchaseOrder = BeanUtils.toBean(createReqVO, ErpPurchaseOrderDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        purchaseOrder.setOrderTime(LocalDateTime.now());
        calculateTotalPrice(purchaseOrder, purchaseOrderItems);
        purchaseOrderMapper.insert(purchaseOrder);
        // 2.2 插入订单项
        purchaseOrderItems.forEach(o -> o.setOrderId(purchaseOrder.getId()));
        purchaseOrderItemMapper.insertBatch(purchaseOrderItems);
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
        // 1.2 校验供应商
        supplierService.validateSupplier(updateReqVO.getSupplierId());
        // 1.3 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        // 1.4 校验订单项的有效性
        List<ErpPurchaseOrderItemDO> purchaseOrderItems = validatePurchaseOrderItems(updateReqVO.getItems());
        // 1.5 校验已入库项不允许修改赠品标记
        validateGiftModify(updateReqVO.getId(), updateReqVO.getItems());

        // 2.1 更新订单
        ErpPurchaseOrderDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseOrderDO.class);
        updateObj.setOrderTime(LocalDateTime.now());
        calculateTotalPrice(updateObj, purchaseOrderItems);
        purchaseOrderMapper.updateById(updateObj);
        // 2.2 更新订单项
        updatePurchaseOrderItemList(updateReqVO.getId(), purchaseOrderItems);
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
            // 已有入库记录时，不允许修改 gift 标记
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
        // totalProductPrice / totalTaxPrice 排除赠品行
        purchaseOrder.setTotalProductPrice(getSumValue(purchaseOrderItems,
                item -> Boolean.TRUE.equals(item.getGift()) ? BigDecimal.ZERO : item.getTotalPrice(),
                BigDecimal::add, BigDecimal.ZERO));
        purchaseOrder.setTotalTaxPrice(getSumValue(purchaseOrderItems,
                item -> Boolean.TRUE.equals(item.getGift()) ? BigDecimal.ZERO : (item.getTaxPrice() != null ? item.getTaxPrice() : BigDecimal.ZERO),
                BigDecimal::add, BigDecimal.ZERO));
        purchaseOrder.setTotalPrice(purchaseOrder.getTotalProductPrice().add(purchaseOrder.getTotalTaxPrice()));
        // 计算优惠价格
        if (purchaseOrder.getDiscountPercent() == null) {
            purchaseOrder.setDiscountPercent(BigDecimal.ZERO);
        }
        purchaseOrder.setDiscountPrice(MoneyUtils.priceMultiplyPercent(purchaseOrder.getTotalPrice(), purchaseOrder.getDiscountPercent()));
        purchaseOrder.setTotalPrice(purchaseOrder.getTotalPrice().subtract(purchaseOrder.getDiscountPrice()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseOrderStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1.1 校验存在
        ErpPurchaseOrderDO purchaseOrder = validatePurchaseOrderExists(id);
        // 1.2 校验状态
        if (purchaseOrder.getStatus().equals(status)) {
            throw exception(approve ? PURCHASE_ORDER_APPROVE_FAIL : PURCHASE_ORDER_PROCESS_FAIL);
        }
        // 1.3 存在采购入单，无法反审核
        if (!approve && purchaseOrder.getInCount().compareTo(BigDecimal.ZERO) > 0) {
            throw exception(PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN);
        }
        // 1.4 存在采购退货单，无法反审核
        if (!approve && purchaseOrder.getReturnCount().compareTo(BigDecimal.ZERO) > 0) {
            throw exception(PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN);
        }

        // 2. 更新状态
        int updateCount = purchaseOrderMapper.updateByIdAndStatus(id, purchaseOrder.getStatus(),
                new ErpPurchaseOrderDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(approve ? PURCHASE_ORDER_APPROVE_FAIL : PURCHASE_ORDER_PROCESS_FAIL);
        }
    }

    private List<ErpPurchaseOrderItemDO> validatePurchaseOrderItems(List<ErpPurchaseOrderSaveReqVO.Item> list) {
        // 0. 校验每项的数量和单价必须大于 0（赠品行仅校验数量，单价强制为 0）
        if (CollUtil.isNotEmpty(list)) {
            for (ErpPurchaseOrderSaveReqVO.Item item : list) {
                if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw exception(PURCHASE_ORDER_ITEM_COUNT_POSITIVE);
                }
                // 赠品行单价会被强制为 0，跳过单价校验
                if (Boolean.TRUE.equals(item.getGift())) {
                    continue;
                }
                if (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw exception(PURCHASE_ORDER_ITEM_PRICE_POSITIVE);
                }
            }
        }
        // 1. 校验产品存在
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpPurchaseOrderSaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 2. 转化为 ErpPurchaseOrderItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpPurchaseOrderItemDO.class, item -> {
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            // 赠品行：强制 productPrice=0, totalPrice=0, taxPrice=0
            if (Boolean.TRUE.equals(item.getGift())) {
                item.setProductPrice(BigDecimal.ZERO);
                item.setTotalPrice(BigDecimal.ZERO);
                item.setTaxPrice(BigDecimal.ZERO);
                return;
            }
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            if (item.getTotalPrice() == null) {
                return;
            }
            if (item.getTaxPercent() != null) {
                item.setTaxPrice(MoneyUtils.priceMultiplyPercent(item.getTotalPrice(), item.getTaxPercent()));
            }
        }));
    }

    private void updatePurchaseOrderItemList(Long id, List<ErpPurchaseOrderItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpPurchaseOrderItemDO> oldList = purchaseOrderItemMapper.selectListByOrderId(id);
        List<List<ErpPurchaseOrderItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setOrderId(id));
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
        // 1. 更新每个采购订单项
        orderItems.forEach(item -> {
            BigDecimal inCount = inCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (item.getInCount().equals(inCount)) {
                return;
            }
            if (inCount.compareTo(item.getCount()) > 0) {
                throw exception(PURCHASE_ORDER_ITEM_IN_FAIL_PRODUCT_EXCEED,
                        productService.getProduct(item.getProductId()).getName(), item.getCount());
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
        // 1. 更新每个采购订单项
        orderItems.forEach(item -> {
            BigDecimal returnCount = returnCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (item.getReturnCount().equals(returnCount)) {
                return;
            }
            if (returnCount.compareTo(item.getInCount()) > 0) {
                throw exception(PURCHASE_ORDER_ITEM_RETURN_FAIL_IN_EXCEED,
                        productService.getProduct(item.getProductId()).getName(), item.getInCount());
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
            // 2.2 删除订单项
            purchaseOrderItemMapper.deleteByOrderId(purchaseOrder.getId());
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

    // ==================== 订单项 ====================

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
    public ErpPurchaseOrderImportRespVO parseImportData(List<ErpPurchaseOrderImportExcelVO> list) {
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
            ErpPurchaseOrderImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (StrUtil.isBlank(row.getProductCode())) {
                respVO.getFailureDetails().add(new ErpPurchaseOrderImportRespVO.FailureItem(rowNo, null, "产品编码不能为空"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpProductDO product = productMap.get(row.getProductCode());
            if (product == null) {
                respVO.getFailureDetails().add(new ErpPurchaseOrderImportRespVO.FailureItem(rowNo, row.getProductCode(), "产品不存在"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            if (row.getCount() == null || row.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                respVO.getFailureDetails().add(new ErpPurchaseOrderImportRespVO.FailureItem(rowNo, row.getProductCode(), "数量必须大于 0"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpPurchaseOrderSaveReqVO.Item item = new ErpPurchaseOrderSaveReqVO.Item();
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductUnitId(product.getUnitId());
            item.setProductUnitName(productVOMap.get(product.getId()) == null ? null : productVOMap.get(product.getId()).getUnitName());
            item.setWarehouseId(product.getDefaultWarehouseId());
            item.setProductPrice(row.getProductPrice() != null ? row.getProductPrice() : product.getPurchasePrice());
            item.setCount(row.getCount());
            item.setTaxPercent(BigDecimal.ZERO);
            item.setRemark(null);
            respVO.getItems().add(item);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    @Override
    public List<ErpPurchaseOrderInableItemRespVO> getInableItemsByOrderId(Long orderId) {
        // 1. 校验订单已审批
        validatePurchaseOrder(orderId);
        // 2. 查询订单项
        List<ErpPurchaseOrderItemDO> orderItems = purchaseOrderItemMapper.selectListByOrderId(orderId);
        if (CollUtil.isEmpty(orderItems)) {
            return Collections.emptyList();
        }
        // 3. 查产品信息（使用 VO Map，包含 unitName）
        Map<Long, ErpProductRespVO> productVOMap = productService.getProductVOMap(
                convertSet(orderItems, ErpPurchaseOrderItemDO::getProductId));
        // 4. 组装可入库明细
        return convertList(orderItems, item -> {
            BigDecimal inCount = item.getInCount() != null ? item.getInCount() : BigDecimal.ZERO;
            BigDecimal inableCount = item.getCount().subtract(inCount);
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
            }
            vo.setVehicleModel(item.getVehicleModel());
            vo.setOriginPlace(item.getOriginPlace());
            vo.setBrand(item.getBrand());
            vo.setStandard(item.getStandard());
            vo.setDrawingNo(item.getDrawingNo());
            vo.setWarehousePosition(item.getWarehousePosition());
            return vo;
        });
    }

}
