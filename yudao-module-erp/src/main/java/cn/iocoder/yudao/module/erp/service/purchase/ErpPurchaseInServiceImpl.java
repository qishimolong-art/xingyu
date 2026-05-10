package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseInFromOrderReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

// TODO 芋艿：记录操作日志

/**
 * ERP 采购入库 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpPurchaseInServiceImpl implements ErpPurchaseInService {

    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;

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

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseIn(ErpPurchaseInSaveReqVO createReqVO) {
        // 1.1 校验采购订单已审核（如果填写了 orderId）
        ErpPurchaseOrderDO purchaseOrder = null;
        if (createReqVO.getOrderId() != null) {
            purchaseOrder = purchaseOrderService.validatePurchaseOrder(createReqVO.getOrderId());
        }
        // 1.2 校验入库项的有效性
        List<ErpPurchaseInItemDO> purchaseInItems = validatePurchaseInItems(createReqVO.getItems());
        // 1.3 校验结算账户
        accountService.validateAccount(createReqVO.getAccountId());
        // 1.4 生成入库单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_IN_NO_PREFIX);
        if (purchaseInMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_IN_NO_EXISTS);
        }

        // 2.1 插入入库
        ErpPurchaseInDO purchaseIn = BeanUtils.toBean(createReqVO, ErpPurchaseInDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        if (purchaseOrder != null) {
            purchaseIn.setOrderNo(purchaseOrder.getNo()).setSupplierId(purchaseOrder.getSupplierId());
        }
        // （当 orderId 为空时，supplierId 由 createReqVO 传进来；若未传，则在主表 supplierId 为 null，业务允许）
        calculateTotalPrice(purchaseIn, purchaseInItems);
        purchaseInMapper.insert(purchaseIn);
        // 2.2 插入入库项
        purchaseInItems.forEach(o -> o.setInId(purchaseIn.getId()));
        purchaseInItemMapper.insertBatch(purchaseInItems);

        // 3. 仅在有 orderId 时更新采购订单的入库数量
        if (createReqVO.getOrderId() != null) {
            updatePurchaseOrderInCount(createReqVO.getOrderId());
        }
        return purchaseIn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseIn(ErpPurchaseInSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpPurchaseInDO purchaseIn = validatePurchaseInExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseIn.getStatus())) {
            throw exception(PURCHASE_IN_UPDATE_FAIL_APPROVE, purchaseIn.getNo());
        }
        // 1.2 校验采购订单已审核（如果填写了 orderId）
        ErpPurchaseOrderDO purchaseOrder = null;
        if (updateReqVO.getOrderId() != null) {
            purchaseOrder = purchaseOrderService.validatePurchaseOrder(updateReqVO.getOrderId());
        }
        // 1.3 校验结算账户
        accountService.validateAccount(updateReqVO.getAccountId());
        // 1.4 校验订单项的有效性
        List<ErpPurchaseInItemDO> purchaseInItems = validatePurchaseInItems(updateReqVO.getItems());

        // 2.1 更新入库
        ErpPurchaseInDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseInDO.class);
        if (purchaseOrder != null) {
            updateObj.setOrderNo(purchaseOrder.getNo()).setSupplierId(purchaseOrder.getSupplierId());
        }
        calculateTotalPrice(updateObj, purchaseInItems);
        purchaseInMapper.updateById(updateObj);
        // 2.2 更新入库项
        updatePurchaseInItemList(updateReqVO.getId(), purchaseInItems);

        // 3.1 更新采购订单的入库数量（新 orderId 非空时）
        if (updateObj.getOrderId() != null) {
            updatePurchaseOrderInCount(updateObj.getOrderId());
        }
        // 3.2 注意：如果采购订单编号变更了，需要更新“老”采购订单的入库数量
        if (ObjectUtil.notEqual(purchaseIn.getOrderId(), updateObj.getOrderId())) {
            if (purchaseIn.getOrderId() != null) {
                updatePurchaseOrderInCount(purchaseIn.getOrderId());
            }
        }
    }

    private void calculateTotalPrice(ErpPurchaseInDO purchaseIn, List<ErpPurchaseInItemDO> purchaseInItems) {
        purchaseIn.setTotalCount(getSumValue(purchaseInItems, ErpPurchaseInItemDO::getCount, BigDecimal::add));
        purchaseIn.setTotalProductPrice(getSumValue(purchaseInItems, ErpPurchaseInItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        purchaseIn.setTotalTaxPrice(getSumValue(purchaseInItems, ErpPurchaseInItemDO::getTaxPrice, BigDecimal::add, BigDecimal.ZERO));
        purchaseIn.setTotalPrice(purchaseIn.getTotalProductPrice().add(purchaseIn.getTotalTaxPrice()));
        // 计算优惠价格
        if (purchaseIn.getDiscountPercent() == null) {
            purchaseIn.setDiscountPercent(BigDecimal.ZERO);
        }
        purchaseIn.setDiscountPrice(MoneyUtils.priceMultiplyPercent(purchaseIn.getTotalPrice(), purchaseIn.getDiscountPercent()));
        purchaseIn.setTotalPrice(purchaseIn.getTotalPrice().subtract(purchaseIn.getDiscountPrice()).add(purchaseIn.getOtherPrice()));
    }

    private void updatePurchaseOrderInCount(Long orderId) {
        // 1.1 查询采购订单对应的采购入库单列表
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectListByOrderId(orderId);
        // 1.2 查询对应的采购订单项的入库数量
        Map<Long, BigDecimal> returnCountMap = purchaseInItemMapper.selectOrderItemCountSumMapByInIds(
                convertList(purchaseIns, ErpPurchaseInDO::getId));
        // 2. 更新采购订单的入库数量
        purchaseOrderService.updatePurchaseOrderInCount(orderId, returnCountMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseInStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1.1 校验存在
        ErpPurchaseInDO purchaseIn = validatePurchaseInExists(id);
        // 1.2 校验状态
        if (purchaseIn.getStatus().equals(status)) {
            throw exception(approve ? PURCHASE_IN_APPROVE_FAIL : PURCHASE_IN_PROCESS_FAIL);
        }
        // 1.3 校验已付款
        if (!approve && purchaseIn.getPaymentPrice().compareTo(BigDecimal.ZERO) > 0) {
            throw exception(PURCHASE_IN_PROCESS_FAIL_EXISTS_PAYMENT);
        }

        // 2. 更新状态
        int updateCount = purchaseInMapper.updateByIdAndStatus(id, purchaseIn.getStatus(),
                new ErpPurchaseInDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(approve ? PURCHASE_IN_APPROVE_FAIL : PURCHASE_IN_PROCESS_FAIL);
        }

        // 3. 变更库存
        List<ErpPurchaseInItemDO> purchaseInItems = purchaseInItemMapper.selectListByInId(id);
        Integer bizType = approve ? ErpStockRecordBizTypeEnum.PURCHASE_IN.getType()
                : ErpStockRecordBizTypeEnum.PURCHASE_IN_CANCEL.getType();
        purchaseInItems.forEach(purchaseInItem -> {
            BigDecimal count = approve ? purchaseInItem.getCount() : purchaseInItem.getCount().negate();
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    purchaseInItem.getProductId(), purchaseInItem.getWarehouseId(), count,
                    bizType, purchaseInItem.getInId(), purchaseInItem.getId(), purchaseIn.getNo(),
                    purchaseInItem.getProductPrice(), purchaseIn.getInTime()));
        });

        // 4. 仅在审批通过时，回写每个产品的最近采购价 last_purchase_price
        if (approve) {
            purchaseInItems.forEach(item -> productService.updateProductLastPurchasePrice(
                    item.getProductId(), item.getProductPrice()));
        }
    }

    @Override
    public void updatePurchaseInPaymentPrice(Long id, BigDecimal paymentPrice) {
        ErpPurchaseInDO purchaseIn = purchaseInMapper.selectById(id);
        if (purchaseIn.getPaymentPrice().equals(paymentPrice)) {
            return;
        }
        if (paymentPrice.compareTo(purchaseIn.getTotalPrice()) > 0) {
            throw exception(PURCHASE_IN_FAIL_PAYMENT_PRICE_EXCEED, paymentPrice, purchaseIn.getTotalPrice());
        }
        purchaseInMapper.updateById(new ErpPurchaseInDO().setId(id).setPaymentPrice(paymentPrice));
    }

    private List<ErpPurchaseInItemDO> validatePurchaseInItems(List<ErpPurchaseInSaveReqVO.Item> list) {
        // 1. 校验产品存在
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpPurchaseInSaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 2. 转化为 ErpPurchaseInItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpPurchaseInItemDO.class, item -> {
            ErpProductDO product = productMap.get(item.getProductId());
            item.setProductUnitId(product.getUnitId());

            // 包装数：优先用 VO 传来的；为空或非正数 → 用产品资料 packageQty；再为空 → 默认 1
            Integer packageQty = item.getPackageQty();
            if (packageQty == null || packageQty <= 0) {
                packageQty = product.getPackageQty() != null && product.getPackageQty() > 0
                        ? product.getPackageQty() : 1;
                item.setPackageQty(packageQty);
            }

            // 整件数有值 → count = wholeQty × packageQty，覆盖 VO 传的 count
            if (item.getWholeQty() != null) {
                item.setCount(new BigDecimal(item.getWholeQty()).multiply(new BigDecimal(packageQty)));
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

    private void updatePurchaseInItemList(Long id, List<ErpPurchaseInItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpPurchaseInItemDO> oldList = purchaseInItemMapper.selectListByInId(id);
        List<List<ErpPurchaseInItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setInId(id));
            purchaseInItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            purchaseInItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            purchaseInItemMapper.deleteByIds(convertList(diffList.get(2), ErpPurchaseInItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseIn(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectByIds(ids);
        if (CollUtil.isEmpty(purchaseIns)) {
            return;
        }
        purchaseIns.forEach(purchaseIn -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseIn.getStatus())) {
                throw exception(PURCHASE_IN_DELETE_FAIL_APPROVE, purchaseIn.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        purchaseIns.forEach(purchaseIn -> {
            // 2.1 删除订单
            purchaseInMapper.deleteById(purchaseIn.getId());
            // 2.2 删除订单项
            purchaseInItemMapper.deleteByInId(purchaseIn.getId());

            // 2.3 更新采购订单的入库数量
            if (purchaseIn.getOrderId() != null) {
                updatePurchaseOrderInCount(purchaseIn.getOrderId());
            }
        });

    }

    private ErpPurchaseInDO validatePurchaseInExists(Long id) {
        ErpPurchaseInDO purchaseIn = purchaseInMapper.selectById(id);
        if (purchaseIn == null) {
            throw exception(PURCHASE_IN_NOT_EXISTS);
        }
        return purchaseIn;
    }

    @Override
    public ErpPurchaseInDO getPurchaseIn(Long id) {
        return purchaseInMapper.selectById(id);
    }

    @Override
    public ErpPurchaseInDO validatePurchaseIn(Long id) {
        ErpPurchaseInDO purchaseIn = validatePurchaseInExists(id);
        if (ObjectUtil.notEqual(purchaseIn.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(PURCHASE_IN_NOT_APPROVE);
        }
        return purchaseIn;
    }

    @Override
    public PageResult<ErpPurchaseInDO> getPurchaseInPage(ErpPurchaseInPageReqVO pageReqVO) {
        return purchaseInMapper.selectPage(pageReqVO);
    }

    // ==================== 采购入库项 ====================

    @Override
    public List<ErpPurchaseInItemDO> getPurchaseInItemListByInId(Long inId) {
        return purchaseInItemMapper.selectListByInId(inId);
    }

    @Override
    public List<ErpPurchaseInItemDO> getPurchaseInItemListByInIds(Collection<Long> inIds) {
        if (CollUtil.isEmpty(inIds)) {
            return Collections.emptyList();
        }
        return purchaseInItemMapper.selectListByInIds(inIds);
    }

    @Override
    public List<ErpPurchaseReturnableItemRespVO> getReturnableItemsByInId(Long inId) {
        // 1. 校验入库单存在且已审批
        ErpPurchaseInDO purchaseIn = validatePurchaseIn(inId);

        // 2. 查入库项列表
        List<ErpPurchaseInItemDO> items = purchaseInItemMapper.selectListByInId(inId);
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }

        // 3. 查所有入库项的累计已退数量
        Set<Long> itemIds = convertSet(items, ErpPurchaseInItemDO::getId);
        Map<Long, BigDecimal> returnedMap = purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(itemIds);

        // 4. 组装结果：可退数量 = 入库数量 - 已退
        return items.stream().map(item -> {
            ErpPurchaseReturnableItemRespVO vo = new ErpPurchaseReturnableItemRespVO();
            vo.setSourceInId(inId);
            vo.setSourceInItemId(item.getId());
            vo.setSourceInNo(purchaseIn.getNo());
            vo.setProductId(item.getProductId());
            vo.setProductUnitId(item.getProductUnitId());
            vo.setWarehouseId(item.getWarehouseId());
            vo.setProductPrice(item.getProductPrice());
            vo.setInCount(item.getCount());
            BigDecimal returned = returnedMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            vo.setReturnedCount(returned);
            BigDecimal returnable = item.getCount().subtract(returned);
            // 可退不能为负
            if (returnable.compareTo(BigDecimal.ZERO) < 0) {
                returnable = BigDecimal.ZERO;
            }
            vo.setReturnableCount(returnable);
            vo.setTaxPercent(item.getTaxPercent());
            vo.setPackageQty(item.getPackageQty());
            vo.setWholeQty(item.getWholeQty());
            vo.setWarehousePosition(item.getWarehousePosition());
            vo.setDrawingNo(item.getDrawingNo());
            vo.setBatchNo(item.getBatchNo());
            vo.setBarCode(item.getBarCode());
            vo.setBrand(item.getBrand());
            vo.setVehicleModel(item.getVehicleModel());
            vo.setOriginPlace(item.getOriginPlace());
            vo.setBusinessEntity(item.getBusinessEntity());
            vo.setRemark(item.getRemark());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseInFromOrder(ErpPurchaseInFromOrderReqVO reqVO) {
        // 1. 校验采购订单已审批
        ErpPurchaseOrderDO purchaseOrder = purchaseOrderService.validatePurchaseOrder(reqVO.getOrderId());
        // 2. 查询订单子表
        List<ErpPurchaseOrderItemDO> orderItems = purchaseOrderService.getPurchaseOrderItemListByOrderId(reqVO.getOrderId());
        Map<Long, ErpPurchaseOrderItemDO> orderItemMap = convertMap(orderItems, ErpPurchaseOrderItemDO::getId);
        // 3. 校验每项 count ≤ inableCount，并构造入库项
        List<ErpPurchaseInSaveReqVO.Item> inItems = new java.util.ArrayList<>();
        for (ErpPurchaseInFromOrderReqVO.Item reqItem : reqVO.getItems()) {
            ErpPurchaseOrderItemDO orderItem = orderItemMap.get(reqItem.getOrderItemId());
            if (orderItem == null) {
                throw exception(PURCHASE_ORDER_NOT_EXISTS);
            }
            BigDecimal inCount = orderItem.getInCount() != null ? orderItem.getInCount() : BigDecimal.ZERO;
            BigDecimal inableCount = orderItem.getCount().subtract(inCount);
            if (reqItem.getCount().compareTo(inableCount) > 0) {
                String productName = productService.getProduct(orderItem.getProductId()).getName();
                throw exception(PURCHASE_ORDER_IN_EXCEED_INABLE, productName, inableCount, reqItem.getCount());
            }
            // 构造入库项
            ErpPurchaseInSaveReqVO.Item inItem = new ErpPurchaseInSaveReqVO.Item();
            inItem.setOrderItemId(orderItem.getId());
            inItem.setWarehouseId(reqItem.getWarehouseId());
            inItem.setProductId(orderItem.getProductId());
            inItem.setProductUnitId(orderItem.getProductUnitId());
            // 赠品行 productPrice=0
            inItem.setProductPrice(Boolean.TRUE.equals(orderItem.getGift()) ? BigDecimal.ZERO : orderItem.getProductPrice());
            inItem.setCount(reqItem.getCount());
            inItem.setTaxPercent(orderItem.getTaxPercent());
            inItem.setRemark(orderItem.getRemark());
            inItems.add(inItem);
        }
        // 4. 构造 ErpPurchaseInSaveReqVO
        ErpPurchaseInSaveReqVO saveReqVO = new ErpPurchaseInSaveReqVO();
        saveReqVO.setOrderId(reqVO.getOrderId());
        saveReqVO.setInTime(reqVO.getInTime() != null ? reqVO.getInTime() : java.time.LocalDateTime.now());
        saveReqVO.setAccountId(reqVO.getAccountId());
        saveReqVO.setDiscountPercent(BigDecimal.ZERO);
        saveReqVO.setOtherPrice(BigDecimal.ZERO);
        saveReqVO.setItems(inItems);
        // 5. 创建入库单
        Long inId = createPurchaseIn(saveReqVO);
        // 6. 自动审批生效
        updatePurchaseInStatus(inId, ErpAuditStatus.APPROVE.getStatus());
        return inId;
    }

}
