package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseReturnModeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

// TODO 芋艿：记录操作日志

/**
 * ERP 采购退货 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpPurchaseReturnServiceImpl implements ErpPurchaseReturnService {

    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private ErpPurchaseOrderService purchaseOrderService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpStockRecordService stockRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseReturn(ErpPurchaseReturnSaveReqVO createReqVO) {
        // 1.1 校验退货模式
        Integer returnMode = createReqVO.getReturnMode();
        if (returnMode == null || (!ErpPurchaseReturnModeEnum.isByOrder(returnMode) && !ErpPurchaseReturnModeEnum.isByStock(returnMode))) {
            throw exception(PURCHASE_RETURN_MODE_INVALID);
        }
        // 1.2 校验采购订单已审核（orderId 可空）
        ErpPurchaseOrderDO purchaseOrder = null;
        if (createReqVO.getOrderId() != null) {
            purchaseOrder = purchaseOrderService.validatePurchaseOrder(createReqVO.getOrderId());
        }
        // 1.3 按入库单退货模式下，校验 sourceInItem 可退数量
        if (ErpPurchaseReturnModeEnum.isByOrder(returnMode)) {
            validateReturnableCountForByOrder(createReqVO.getItems(), null);
        }
        // 1.4 校验退货项的有效性
        List<ErpPurchaseReturnItemDO> purchaseReturnItems = validatePurchaseReturnItems(createReqVO.getItems());
        // 1.5 校验结算账户
        accountService.validateAccount(createReqVO.getAccountId());
        // 1.6 生成退货单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_RETURN_NO_PREFIX);
        if (purchaseReturnMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_RETURN_NO_EXISTS);
        }

        // 2.1 插入退货
        ErpPurchaseReturnDO purchaseReturn = BeanUtils.toBean(createReqVO, ErpPurchaseReturnDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        if (purchaseOrder != null) {
            purchaseReturn.setOrderNo(purchaseOrder.getNo()).setSupplierId(purchaseOrder.getSupplierId());
        }
        calculateTotalPrice(purchaseReturn, purchaseReturnItems);
        purchaseReturnMapper.insert(purchaseReturn);
        // 2.2 插入退货项
        purchaseReturnItems.forEach(o -> o.setReturnId(purchaseReturn.getId()));
        purchaseReturnItemMapper.insertBatch(purchaseReturnItems);

        // 3. 更新采购订单的退货数量
        if (createReqVO.getOrderId() != null) {
            updatePurchaseOrderReturnCount(createReqVO.getOrderId());
        }
        return purchaseReturn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseReturn(ErpPurchaseReturnSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpPurchaseReturnDO purchaseReturn = validatePurchaseReturnExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseReturn.getStatus())) {
            throw exception(PURCHASE_RETURN_UPDATE_FAIL_APPROVE, purchaseReturn.getNo());
        }
        // 1.2 校验退货模式
        Integer returnMode = updateReqVO.getReturnMode();
        if (returnMode == null || (!ErpPurchaseReturnModeEnum.isByOrder(returnMode) && !ErpPurchaseReturnModeEnum.isByStock(returnMode))) {
            throw exception(PURCHASE_RETURN_MODE_INVALID);
        }
        // 1.3 校验采购订单已审核（orderId 可空）
        ErpPurchaseOrderDO purchaseOrder = null;
        if (updateReqVO.getOrderId() != null) {
            purchaseOrder = purchaseOrderService.validatePurchaseOrder(updateReqVO.getOrderId());
        }
        // 1.4 校验结算账户
        accountService.validateAccount(updateReqVO.getAccountId());
        // 1.5 按入库单退货模式下，校验 sourceInItem 可退数量（排除当前退货单自己）
        if (ErpPurchaseReturnModeEnum.isByOrder(returnMode)) {
            validateReturnableCountForByOrder(updateReqVO.getItems(), updateReqVO.getId());
        }
        // 1.6 校验订单项的有效性
        List<ErpPurchaseReturnItemDO> purchaseReturnItems = validatePurchaseReturnItems(updateReqVO.getItems());

        // 2.1 更新退货
        ErpPurchaseReturnDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseReturnDO.class);
        if (purchaseOrder != null) {
            updateObj.setOrderNo(purchaseOrder.getNo()).setSupplierId(purchaseOrder.getSupplierId());
        }
        calculateTotalPrice(updateObj, purchaseReturnItems);
        purchaseReturnMapper.updateById(updateObj);
        // 2.2 更新退货项
        updatePurchaseReturnItemList(updateReqVO.getId(), purchaseReturnItems);

        // 3.1 更新采购订单的出库数量
        if (updateObj.getOrderId() != null) {
            updatePurchaseOrderReturnCount(updateObj.getOrderId());
        }
        // 3.2 注意：如果采购订单编号变更了，需要更新“老”采购订单的出库数量
        if (ObjectUtil.notEqual(purchaseReturn.getOrderId(), updateObj.getOrderId())
                && purchaseReturn.getOrderId() != null) {
            updatePurchaseOrderReturnCount(purchaseReturn.getOrderId());
        }
    }

    private void calculateTotalPrice(ErpPurchaseReturnDO purchaseReturn, List<ErpPurchaseReturnItemDO> purchaseReturnItems) {
        purchaseReturn.setTotalCount(getSumValue(purchaseReturnItems, ErpPurchaseReturnItemDO::getCount, BigDecimal::add));
        purchaseReturn.setTotalProductPrice(getSumValue(purchaseReturnItems, ErpPurchaseReturnItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        purchaseReturn.setTotalTaxPrice(getSumValue(purchaseReturnItems, ErpPurchaseReturnItemDO::getTaxPrice, BigDecimal::add, BigDecimal.ZERO));
        purchaseReturn.setTotalPrice(purchaseReturn.getTotalProductPrice().add(purchaseReturn.getTotalTaxPrice()));
        // 计算优惠价格
        if (purchaseReturn.getDiscountPercent() == null) {
            purchaseReturn.setDiscountPercent(BigDecimal.ZERO);
        }
        purchaseReturn.setDiscountPrice(MoneyUtils.priceMultiplyPercent(purchaseReturn.getTotalPrice(), purchaseReturn.getDiscountPercent()));
        purchaseReturn.setTotalPrice(purchaseReturn.getTotalPrice().subtract(purchaseReturn.getDiscountPrice()).add(purchaseReturn.getOtherPrice()));
    }

    private void updatePurchaseOrderReturnCount(Long orderId) {
        // 1.1 查询采购订单对应的采购出库单列表
        List<ErpPurchaseReturnDO> purchaseReturns = purchaseReturnMapper.selectListByOrderId(orderId);
        // 1.2 查询对应的采购订单项的退货数量
        Map<Long, BigDecimal> returnCountMap = purchaseReturnItemMapper.selectOrderItemCountSumMapByReturnIds(
                convertList(purchaseReturns, ErpPurchaseReturnDO::getId));
        // 2. 更新采购订单的出库数量
        purchaseOrderService.updatePurchaseOrderReturnCount(orderId, returnCountMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseReturnStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1.1 校验存在
        ErpPurchaseReturnDO purchaseReturn = validatePurchaseReturnExists(id);
        // 1.2 校验状态
        if (purchaseReturn.getStatus().equals(status)) {
            throw exception(approve ? PURCHASE_RETURN_APPROVE_FAIL : PURCHASE_RETURN_PROCESS_FAIL);
        }
        // 1.3 校验已退款
        if (!approve && purchaseReturn.getRefundPrice().compareTo(BigDecimal.ZERO) > 0) {
            throw exception(PURCHASE_RETURN_PROCESS_FAIL_EXISTS_REFUND);
        }

        // 2. 更新状态
        int updateCount = purchaseReturnMapper.updateByIdAndStatus(id, purchaseReturn.getStatus(),
                new ErpPurchaseReturnDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(approve ? PURCHASE_RETURN_APPROVE_FAIL : PURCHASE_RETURN_PROCESS_FAIL);
        }

        // 3. 变更库存
        List<ErpPurchaseReturnItemDO> purchaseReturnItems = purchaseReturnItemMapper.selectListByReturnId(id);
        Integer bizType = approve ? ErpStockRecordBizTypeEnum.PURCHASE_RETURN.getType()
                : ErpStockRecordBizTypeEnum.PURCHASE_RETURN_CANCEL.getType();
        purchaseReturnItems.forEach(purchaseReturnItem -> {
            BigDecimal count = approve ? purchaseReturnItem.getCount().negate() : purchaseReturnItem.getCount();
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    purchaseReturnItem.getProductId(), purchaseReturnItem.getWarehouseId(), count,
                    bizType, purchaseReturnItem.getReturnId(), purchaseReturnItem.getId(), purchaseReturn.getNo(),
                    purchaseReturnItem.getProductPrice(), purchaseReturn.getReturnTime()));
        });
    }

    @Override
    public void updatePurchaseReturnRefundPrice(Long id, BigDecimal refundPrice) {
        ErpPurchaseReturnDO purchaseReturn = purchaseReturnMapper.selectById(id);
        if (purchaseReturn.getRefundPrice().equals(refundPrice)) {
            return;
        }
        if (refundPrice.compareTo(purchaseReturn.getTotalPrice()) > 0) {
            throw exception(PURCHASE_RETURN_FAIL_REFUND_PRICE_EXCEED, refundPrice, purchaseReturn.getTotalPrice());
        }
        purchaseReturnMapper.updateById(new ErpPurchaseReturnDO().setId(id).setRefundPrice(refundPrice));
    }

    private List<ErpPurchaseReturnItemDO> validatePurchaseReturnItems(List<ErpPurchaseReturnSaveReqVO.Item> list) {
        // 1. 校验产品存在
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpPurchaseReturnSaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 2. 转化为 ErpPurchaseReturnItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpPurchaseReturnItemDO.class, item -> {
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            if (item.getTotalPrice() == null) {
                return;
            }
            if (item.getTaxPercent() != null) {
                item.setTaxPrice(MoneyUtils.priceMultiplyPercent(item.getTotalPrice(), item.getTaxPercent()));
            }
        }));
    }

    /**
     * 校验"按单退货"模式下每一项的可退数量合法
     * - sourceInItemId 必填
     * - 可退数量 = 原入库数量 - 其他退货单对该项已退数量（排除当前 currentReturnId 自己）
     * - 当前单所有行对同一 sourceInItemId 的 count 总和不能超过可退数量
     *
     * @param items 提交的子表
     * @param currentReturnId 当前退货单 ID（更新场景传入；新建场景传 null）
     */
    private void validateReturnableCountForByOrder(List<ErpPurchaseReturnSaveReqVO.Item> items, Long currentReturnId) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        // 1. 必填校验
        for (ErpPurchaseReturnSaveReqVO.Item it : items) {
            if (it.getSourceInItemId() == null) {
                throw exception(PURCHASE_RETURN_BY_ORDER_SOURCE_REQUIRED);
            }
            if (it.getCount() == null || it.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PURCHASE_RETURN_COUNT_POSITIVE);
            }
        }

        // 2. 聚合当前单对每个 sourceInItemId 的提交数量
        Map<Long, BigDecimal> currentItemCountMap = new HashMap<>();
        for (ErpPurchaseReturnSaveReqVO.Item it : items) {
            currentItemCountMap.merge(it.getSourceInItemId(), it.getCount(), BigDecimal::add);
        }

        // 3. 查询这些 sourceInItemId 对应的原入库项
        Set<Long> sourceInItemIds = currentItemCountMap.keySet();
        List<ErpPurchaseInItemDO> inItems = purchaseInItemMapper.selectBatchIds(sourceInItemIds);
        Map<Long, ErpPurchaseInItemDO> inItemMap = convertMap(inItems, ErpPurchaseInItemDO::getId);

        // 4. 查询这些入库项的"其他退货单"累计已退数量（排除 currentReturnId）
        Map<Long, BigDecimal> returnedMap = purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIdsExcludeReturn(
                sourceInItemIds, currentReturnId);

        // 5. 逐项校验
        for (Map.Entry<Long, BigDecimal> entry : currentItemCountMap.entrySet()) {
            Long sourceInItemId = entry.getKey();
            BigDecimal currentCount = entry.getValue();
            ErpPurchaseInItemDO inItem = inItemMap.get(sourceInItemId);
            if (inItem == null) {
                throw exception(PURCHASE_RETURN_SOURCE_IN_ITEM_NOT_EXISTS);
            }
            BigDecimal otherReturned = returnedMap.getOrDefault(sourceInItemId, BigDecimal.ZERO);
            BigDecimal returnable = inItem.getCount().subtract(otherReturned);
            if (currentCount.compareTo(returnable) > 0) {
                throw exception(PURCHASE_RETURN_EXCEED_RETURNABLE, sourceInItemId, returnable, currentCount);
            }
        }
    }

    private void updatePurchaseReturnItemList(Long id, List<ErpPurchaseReturnItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpPurchaseReturnItemDO> oldList = purchaseReturnItemMapper.selectListByReturnId(id);
        List<List<ErpPurchaseReturnItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setReturnId(id));
            purchaseReturnItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            purchaseReturnItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            purchaseReturnItemMapper.deleteByIds(convertList(diffList.get(2), ErpPurchaseReturnItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseReturn(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpPurchaseReturnDO> purchaseReturns = purchaseReturnMapper.selectByIds(ids);
        if (CollUtil.isEmpty(purchaseReturns)) {
            return;
        }
        purchaseReturns.forEach(purchaseReturn -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseReturn.getStatus())) {
                throw exception(PURCHASE_RETURN_DELETE_FAIL_APPROVE, purchaseReturn.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        purchaseReturns.forEach(purchaseReturn -> {
            // 2.1 删除订单
            purchaseReturnMapper.deleteById(purchaseReturn.getId());
            // 2.2 删除订单项
            purchaseReturnItemMapper.deleteByReturnId(purchaseReturn.getId());

            // 2.3 更新采购订单的出库数量
            updatePurchaseOrderReturnCount(purchaseReturn.getOrderId());
        });

    }

    private ErpPurchaseReturnDO validatePurchaseReturnExists(Long id) {
        ErpPurchaseReturnDO purchaseReturn = purchaseReturnMapper.selectById(id);
        if (purchaseReturn == null) {
            throw exception(PURCHASE_RETURN_NOT_EXISTS);
        }
        return purchaseReturn;
    }

    @Override
    public ErpPurchaseReturnDO getPurchaseReturn(Long id) {
        return purchaseReturnMapper.selectById(id);
    }

    @Override
    public ErpPurchaseReturnDO validatePurchaseReturn(Long id) {
        ErpPurchaseReturnDO purchaseReturn = getPurchaseReturn(id);
        if (ObjectUtil.notEqual(purchaseReturn.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(PURCHASE_RETURN_NOT_APPROVE);
        }
        return purchaseReturn;
    }

    @Override
    public PageResult<ErpPurchaseReturnDO> getPurchaseReturnPage(ErpPurchaseReturnPageReqVO pageReqVO) {
        return purchaseReturnMapper.selectPage(pageReqVO);
    }

    // ==================== 采购退货项 ====================

    @Override
    public List<ErpPurchaseReturnItemDO> getPurchaseReturnItemListByReturnId(Long returnId) {
        return purchaseReturnItemMapper.selectListByReturnId(returnId);
    }

    @Override
    public List<ErpPurchaseReturnItemDO> getPurchaseReturnItemListByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyList();
        }
        return purchaseReturnItemMapper.selectListByReturnIds(returnIds);
    }

}
