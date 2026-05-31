package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseReturnModeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private ErpProductMapper productMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private ErpPurchaseOrderService purchaseOrderService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Resource
    private ErpVoucherService voucherService;
    @Resource
    private ErpBookOpenService bookOpenService;
    @Resource
    private ErpVoucherMapper voucherMapper;
    @Resource
    private ErpVoucherItemMapper voucherItemMapper;

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
        // 1.5 生成退货单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_RETURN_NO_PREFIX);
        if (purchaseReturnMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_RETURN_NO_EXISTS);
        }

        // 2.1 插入退货
        ErpPurchaseReturnDO purchaseReturn = BeanUtils.toBean(createReqVO, ErpPurchaseReturnDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        purchaseReturn.setReturnTime(LocalDateTime.now());
        if (purchaseOrder != null) {
            purchaseReturn.setOrderNo(purchaseOrder.getNo()).setSupplierId(purchaseOrder.getSupplierId());
        }
        // 1.6 兜底校验：supplierId 必填（按单退货走 orderId 带出；按库存退货要求前端传）
        if (purchaseReturn.getSupplierId() == null) {
            throw exception(PURCHASE_RETURN_SUPPLIER_REQUIRED);
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
        // 1.4 按入库单退货模式下，校验 sourceInItem 可退数量（排除当前退货单自己）
        if (ErpPurchaseReturnModeEnum.isByOrder(returnMode)) {
            validateReturnableCountForByOrder(updateReqVO.getItems(), updateReqVO.getId());
        }
        // 1.5 校验订单项的有效性
        List<ErpPurchaseReturnItemDO> purchaseReturnItems = validatePurchaseReturnItems(updateReqVO.getItems());

        // 2.1 更新退货
        ErpPurchaseReturnDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseReturnDO.class);
        updateObj.setReturnTime(LocalDateTime.now());
        if (purchaseOrder != null) {
            updateObj.setOrderNo(purchaseOrder.getNo()).setSupplierId(purchaseOrder.getSupplierId());
        }
        // 兜底校验：supplierId 必填
        if (updateObj.getSupplierId() == null) {
            throw exception(PURCHASE_RETURN_SUPPLIER_REQUIRED);
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
        // 1.4 反审：先校验关联凭证未审核，并删除未审核凭证
        if (!approve) {
            List<ErpVoucherDO> related = voucherMapper.selectListByBiz(
                    ErpVoucherSourceBizTypeEnum.PURCHASE_RETURN.getType(), id);
            for (ErpVoucherDO v : related) {
                if (ErpVoucherAuditStatusEnum.APPROVE.getStatus().equals(v.getAuditStatus())) {
                    throw exception(BIZ_PROCESS_FAIL_VOUCHER_APPROVED, v.getVoucherNo());
                }
                voucherMapper.deleteById(v.getId());
                voucherItemMapper.delete(new LambdaQueryWrapper<ErpVoucherItemDO>()
                        .eq(ErpVoucherItemDO::getVoucherId, v.getId()));
            }
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

        // 4. 审批通过：自动生成采购红字凭证（受系统开账配置控制）
        if (approve && bookOpenService.isVoucherTypeEnabled(
                purchaseReturn.getReturnTime().toLocalDate(),
                ErpVoucherTypeEnum.PURCHASE.getType())) {
            String supplierName = supplierService.getSupplier(purchaseReturn.getSupplierId()).getName();
            List<ErpVoucherItemDO> items = autoVoucherBuilder.buildPurchaseReturnItems(purchaseReturn, supplierName);
            voucherService.createVoucherFromBiz(
                    ErpVoucherSourceBizTypeEnum.PURCHASE_RETURN.getType(),
                    purchaseReturn.getId(),
                    purchaseReturn.getNo(),
                    purchaseReturn.getTotalPrice(),
                    purchaseReturn.getReturnTime().toLocalDate(),
                    "采购退货 - " + supplierName,
                    items);
        }
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
        // 0. 校验每项的退货数量和退货价格必须大于 0
        if (CollUtil.isNotEmpty(list)) {
            for (ErpPurchaseReturnSaveReqVO.Item item : list) {
                if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw exception(PURCHASE_RETURN_ITEM_COUNT_POSITIVE);
                }
                if (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw exception(PURCHASE_RETURN_ITEM_PRICE_POSITIVE);
                }
            }
        }
        // 0.5 校验同一明细中产品不重复
        Set<String> productCodeSet = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(list)) {
            for (ErpPurchaseReturnSaveReqVO.Item item : list) {
                String productCode = StrUtil.blankToDefault(item.getProductCode(), String.valueOf(item.getProductId()));
                if (!productCodeSet.add(productCode)) {
                    throw exception(PURCHASE_RETURN_ITEM_DUPLICATE, productCode);
                }
            }
        }
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

    @Override
    public List<ErpPurchaseReturnDO> getPurchaseReturnList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return purchaseReturnMapper.selectByIds(ids);
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

    @Override
    public ErpPurchaseReturnImportRespVO importPurchaseReturnItems(List<ErpPurchaseReturnImportExcelVO> list) {
        ErpPurchaseReturnImportRespVO respVO = new ErpPurchaseReturnImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        Map<String, ErpProductDO> productMap = productMapper.selectListByCodes(extractPurchaseReturnCodes(list)).stream()
                .collect(Collectors.toMap(ErpProductDO::getCode, product -> product, (a, b) -> a));
        Map<String, cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO> enabledWarehouseMap = cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                warehouseService.getWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()),
                cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO::getName);
        Map<Long, cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO> productVOMap = productService.getProductVOMap(
                convertSet(productMap.values(), ErpProductDO::getId));

        for (int i = 0; i < list.size(); i++) {
            ErpPurchaseReturnImportExcelVO row = list.get(i);
            if (isEmptyPurchaseReturnRow(row)) {
                continue;
            }
            try {
                ErpProductDO product = getPurchaseReturnProduct(row, productMap);
                cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO warehouse =
                        getPurchaseReturnWarehouse(row, enabledWarehouseMap);
                ErpPurchaseReturnSaveReqVO.Item item = new ErpPurchaseReturnSaveReqVO.Item();
                item.setProductId(product.getId());
                item.setProductCode(product.getCode());
                item.setProductName(product.getName());
                item.setProductUnitId(product.getUnitId());
                item.setWarehouseId(warehouse.getId());
                item.setWarehouseName(warehouse.getName());
                item.setCount(requirePositiveReturnCount(row.getCount()));
                item.setProductPrice(requirePositiveReturnPrice(defaultReturnPrice(row.getProductPrice(), product.getPurchasePrice())));
                item.setPackageQty(defaultPackageQty(product.getPackageQty()));
                item.setRemark(trimToNull(row.getRemark()));
                item.setBarCode(product.getBarCode());
                item.setBrand(product.getBrand());
                item.setVehicleModel(product.getVehicleModel());
                item.setOriginPlace(product.getOriginPlace());
                item.setDrawingNo(product.getDrawingNo());
                cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO productVO = productVOMap.get(product.getId());
                if (productVO != null) {
                    item.setProductUnitName(productVO.getUnitName());
                }
                respVO.getItems().add(item);
                respVO.setSuccessCount(respVO.getSuccessCount() + 1);
            } catch (Exception ex) {
                respVO.getFailureDetails().add(new ErpPurchaseReturnImportRespVO.FailureItem(
                        i + 2, row.getProductCode(), ex.getMessage()));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
            }
        }
        return respVO;
    }

    private static Set<String> extractPurchaseReturnCodes(List<ErpPurchaseReturnImportExcelVO> list) {
        Set<String> codes = new LinkedHashSet<>();
        for (ErpPurchaseReturnImportExcelVO row : list) {
            String code = trimToNull(row.getProductCode());
            if (code != null) {
                codes.add(code);
            }
        }
        return codes;
    }

    private boolean isEmptyPurchaseReturnRow(ErpPurchaseReturnImportExcelVO row) {
        return row == null || StrUtil.isAllBlank(row.getProductCode(), row.getWarehouseName(), row.getRemark())
                && row.getCount() == null && row.getProductPrice() == null;
    }

    private ErpProductDO getPurchaseReturnProduct(ErpPurchaseReturnImportExcelVO row, Map<String, ErpProductDO> productMap) {
        String code = trimToNull(row.getProductCode());
        if (code == null) {
            throw new IllegalArgumentException("产品编码不能为空");
        }
        ErpProductDO product = productMap.get(code);
        if (product == null) {
            throw new IllegalArgumentException("产品不存在：" + code);
        }
        return product;
    }

    private cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO getPurchaseReturnWarehouse(
            ErpPurchaseReturnImportExcelVO row,
            Map<String, cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO> warehouseMap) {
        String warehouseName = trimToNull(row.getWarehouseName());
        if (warehouseName == null) {
            throw new IllegalArgumentException("仓库名称不能为空");
        }
        cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO warehouse = warehouseMap.get(warehouseName);
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在：" + warehouseName);
        }
        return warehouse;
    }

    private BigDecimal requirePositiveReturnCount(BigDecimal count) {
        if (count == null || count.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退货数量不能为空且必须大于 0");
        }
        return count;
    }

    private BigDecimal defaultReturnPrice(BigDecimal importPrice, BigDecimal productPrice) {
        return importPrice != null ? importPrice : productPrice;
    }

    private BigDecimal requirePositiveReturnPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退货单价不能为空且必须大于 0");
        }
        return price;
    }

    private Integer defaultPackageQty(Integer packageQty) {
        return packageQty == null || packageQty <= 0 ? 1 : packageQty;
    }

    private static String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isEmpty(trimmed) ? null : trimmed;
    }

}
