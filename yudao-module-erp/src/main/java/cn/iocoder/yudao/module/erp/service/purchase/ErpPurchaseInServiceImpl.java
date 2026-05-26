package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseInFromOrderReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.context.annotation.Lazy;
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
    private ErpSupplierService supplierService;
    @Resource
    private ErpWarehouseService warehouseService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private ErpVoucherService voucherService;
    @Resource
    private ErpBookOpenService bookOpenService;
    @Resource
    private ErpVoucherMapper voucherMapper;
    @Resource
    private ErpVoucherItemMapper voucherItemMapper;

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
        // 1.3 生成入库单号，并校验唯一性
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
        // 1.3 校验订单项的有效性
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
        if (purchaseIn.getOtherPrice() == null) {
            purchaseIn.setOtherPrice(BigDecimal.ZERO);
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
        // 1.4 反审：先校验关联凭证未审核，并删除未审核凭证
        if (!approve) {
            List<ErpVoucherDO> related = voucherMapper.selectListByBiz(
                    ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType(), id);
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

        // 5. 审批通过：自动生成采购凭证（仅在该月份已开账且启用采购凭证时触发）
        if (approve && bookOpenService.isVoucherTypeEnabled(
                purchaseIn.getInTime().toLocalDate(),
                ErpVoucherTypeEnum.PURCHASE.getType())) {
            ErpSupplierDO supplier = supplierService.validateSupplier(purchaseIn.getSupplierId());
            String supplierName = supplier.getName();
            List<ErpVoucherItemDO> voucherItems = autoVoucherBuilder.buildPurchaseInItems(purchaseIn, supplierName);
            voucherService.createVoucherFromBiz(
                    ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType(),
                    purchaseIn.getId(),
                    purchaseIn.getNo(),
                    purchaseIn.getTotalPrice(),
                    purchaseIn.getInTime().toLocalDate(),
                    "采购入库 - " + supplierName,
                    voucherItems);
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
        // 0. 校验每项的入库数量和入库价格必须大于 0（赠品行单价允许 0）
        if (CollUtil.isNotEmpty(list)) {
            for (ErpPurchaseInSaveReqVO.Item item : list) {
                if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw exception(PURCHASE_IN_ITEM_COUNT_POSITIVE);
                }
                // 校验单价：单价为 null 或 <0 直接报错；=0 仅允许赠品（orderItemId 关联的订单行是赠品由服务端预置为 0，这里只拦截负值/空值）
                if (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw exception(PURCHASE_IN_ITEM_PRICE_POSITIVE);
                }
            }
        }
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

        // 4. 批量查产品信息（名称、编码）
        Set<Long> productIds = convertSet(items, ErpPurchaseInItemDO::getProductId);
        Map<Long, ErpProductDO> productMap = convertMap(
                productService.validProductList(productIds), ErpProductDO::getId);

        // 5. 组装结果：可退数量 = 入库数量 - 已退
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
            ErpProductDO product = productMap.get(item.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductCode(product.getCode());
            }
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

    // ==================== 采购调价 专用查询 ====================

    @Override
    public List<ErpPurchaseInForAdjustRespVO> getApprovedPurchaseInsBySupplier(Long supplierId) {
        if (supplierId == null) {
            return Collections.emptyList();
        }
        // 1. 查该供应商下所有已审批的入库单
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectList(new LambdaQueryWrapper<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getSupplierId, supplierId)
                .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .orderByDesc(ErpPurchaseInDO::getId));
        if (CollUtil.isEmpty(purchaseIns)) {
            return Collections.emptyList();
        }

        // 2. 批量查入库项（只需要统计条数，用 inId 分组）
        Set<Long> inIds = convertSet(purchaseIns, ErpPurchaseInDO::getId);
        List<ErpPurchaseInItemDO> itemList = purchaseInItemMapper.selectListByInIds(inIds);
        Map<Long, Long> itemCountMap = itemList.stream()
                .collect(Collectors.groupingBy(ErpPurchaseInItemDO::getInId, Collectors.counting()));

        // 3. 批量查供应商 + 审核人信息
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(Collections.singleton(supplierId));
        Set<Long> updaterIds = purchaseIns.stream()
                .map(in -> parseLongSafely(in.getUpdater()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isNotEmpty(updaterIds)
                ? adminUserApi.getUserMap(updaterIds) : Collections.emptyMap();

        // 4. 拼装 VO
        return purchaseIns.stream().map(in -> {
            ErpPurchaseInForAdjustRespVO vo = BeanUtils.toBean(in, ErpPurchaseInForAdjustRespVO.class);
            Long itemCount = itemCountMap.get(in.getId());
            vo.setItemCount(itemCount != null ? itemCount.intValue() : 0);
            ErpSupplierDO supplier = supplierMap.get(in.getSupplierId());
            if (supplier != null) {
                vo.setSupplierName(supplier.getName());
            }
            // 近似：审核人 = updater，审批时间 = updateTime
            Long updaterId = parseLongSafely(in.getUpdater());
            if (updaterId != null) {
                vo.setAuditorId(updaterId);
                AdminUserRespDTO user = userMap.get(updaterId);
                if (user != null) {
                    vo.setAuditorName(user.getNickname());
                }
            }
            vo.setApproveTime(in.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ErpPurchaseInItemForAdjustRespVO> getApprovedPurchaseInItemsBySupplier(Long supplierId, Boolean excludeAdjusted) {
        if (supplierId == null) {
            return Collections.emptyList();
        }
        // 1. 查该供应商下所有已审批的入库单（用于拼 inNo / inTime）
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectList(new LambdaQueryWrapper<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getSupplierId, supplierId)
                .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus()));
        if (CollUtil.isEmpty(purchaseIns)) {
            return Collections.emptyList();
        }
        Map<Long, ErpPurchaseInDO> inMap = convertMap(purchaseIns, ErpPurchaseInDO::getId);

        // 2. 批量查入库项
        List<ErpPurchaseInItemDO> items = purchaseInItemMapper.selectListByInIds(inMap.keySet());
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        // 3. 过滤已调价行
        if (Boolean.TRUE.equals(excludeAdjusted)) {
            items = items.stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getAdjusted()))
                    .collect(Collectors.toList());
            if (items.isEmpty()) {
                return Collections.emptyList();
            }
        }

        // 4. 批量查产品（产品资料里有 code / name / unitName / vehicleModel / standard / featureCode / originPlace / brand / drawingNo）
        Set<Long> productIds = convertSet(items, ErpPurchaseInItemDO::getProductId);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(productIds);

        // 5. 批量查仓库
        Set<Long> warehouseIds = convertSet(items, ErpPurchaseInItemDO::getWarehouseId);
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);

        // 6. 拼装 VO
        return items.stream().map(item -> {
            ErpPurchaseInItemForAdjustRespVO vo = new ErpPurchaseInItemForAdjustRespVO();
            vo.setId(item.getId());
            vo.setInId(item.getInId());
            ErpPurchaseInDO in = inMap.get(item.getInId());
            if (in != null) {
                vo.setInNo(in.getNo());
                vo.setInTime(in.getInTime());
            }
            vo.setProductId(item.getProductId());
            vo.setProductUnitId(item.getProductUnitId());
            vo.setWarehouseId(item.getWarehouseId());
            vo.setProductPrice(item.getProductPrice());
            vo.setOriginalProductPrice(item.getOriginalProductPrice());
            vo.setCount(item.getCount());
            vo.setTotalPrice(item.getTotalPrice());
            vo.setDrawingNo(item.getDrawingNo());
            vo.setWarehousePosition(item.getWarehousePosition());
            vo.setAdjusted(item.getAdjusted());
            // 产品资料字段（优先从产品资料补齐，item 自身也有一部分冗余字段）
            ErpProductRespVO product = productMap.get(item.getProductId());
            if (product != null) {
                vo.setProductCode(product.getCode());
                vo.setProductName(product.getName());
                vo.setProductUnitName(product.getUnitName());
                vo.setStandard(product.getStandard());
                vo.setFeatureCode(product.getFeatureCode());
                // 车型 / 品牌 / 产地：item 自身有冗余（随入库时快照），优先用 item 的
                vo.setVehicleModel(ObjectUtil.defaultIfNull(item.getVehicleModel(), product.getVehicleModel()));
                vo.setBrand(ObjectUtil.defaultIfNull(item.getBrand(), product.getBrand()));
                vo.setOriginPlace(ObjectUtil.defaultIfNull(item.getOriginPlace(), product.getOriginPlace()));
            } else {
                vo.setVehicleModel(item.getVehicleModel());
                vo.setBrand(item.getBrand());
                vo.setOriginPlace(item.getOriginPlace());
            }
            // 仓库
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 安全地把 creator / updater（字符串）转成 Long；失败返回 null
     */
    private static Long parseLongSafely(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public ErpPurchaseInImportRespVO importPurchaseInItems(List<ErpPurchaseInImportExcelVO> list) {
        ErpPurchaseInImportRespVO respVO = new ErpPurchaseInImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        Map<String, ErpProductDO> productMap = productMapper.selectListByCodes(extractPurchaseInCodes(list)).stream()
                .collect(Collectors.toMap(ErpProductDO::getCode, product -> product, (a, b) -> a));
        Map<String, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()).stream()
                .collect(Collectors.toMap(ErpWarehouseDO::getName, warehouse -> warehouse, (a, b) -> a));
        Map<Long, ErpProductRespVO> productVOMap = productService.getProductVOMap(
                convertSet(productMap.values(), ErpProductDO::getId));

        for (int i = 0; i < list.size(); i++) {
            ErpPurchaseInImportExcelVO row = list.get(i);
            if (isEmptyPurchaseInRow(row)) {
                continue;
            }
            try {
                ErpProductDO product = getPurchaseInProduct(row, productMap);
                ErpWarehouseDO warehouse = getPurchaseInWarehouse(row, warehouseMap);
                ErpPurchaseInSaveReqVO.Item item = new ErpPurchaseInSaveReqVO.Item();
                item.setProductId(product.getId());
                item.setProductUnitId(product.getUnitId());
                item.setWarehouseId(warehouse.getId());
                item.setCount(requirePositiveCount(row.getCount(), "入库数量不能为空且必须大于 0"));
                item.setProductPrice(defaultPurchaseInPrice(row.getProductPrice(), product.getPurchasePrice()));
                item.setPackageQty(defaultPackageQty(product.getPackageQty()));
                item.setWholeQty(row.getWholeQty());
                item.setWarehousePosition(trimToNull(row.getWarehousePosition()));
                item.setBatchNo(trimToNull(row.getBatchNo()));
                item.setRemark(trimToNull(row.getRemark()));
                fillPurchaseInProductFields(item, product, productVOMap.get(product.getId()));
                respVO.getItems().add(item);
                respVO.setSuccessCount(respVO.getSuccessCount() + 1);
            } catch (Exception ex) {
                respVO.getFailureDetails().add(new ErpPurchaseInImportRespVO.FailureItem(
                        i + 2, row.getProductCode(), ex.getMessage()));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
            }
        }
        return respVO;
    }

    private static Set<String> extractPurchaseInCodes(List<ErpPurchaseInImportExcelVO> list) {
        Set<String> codes = new LinkedHashSet<>();
        for (ErpPurchaseInImportExcelVO row : list) {
            String code = trimToNull(row.getProductCode());
            if (code != null) {
                codes.add(code);
            }
        }
        return codes;
    }

    private boolean isEmptyPurchaseInRow(ErpPurchaseInImportExcelVO row) {
        return row == null || StrUtil.isAllBlank(row.getProductCode(), row.getWarehouseName(), row.getRemark())
                && row.getCount() == null && row.getProductPrice() == null
                && row.getWholeQty() == null && StrUtil.isBlank(row.getWarehousePosition())
                && StrUtil.isBlank(row.getBatchNo());
    }

    private ErpProductDO getPurchaseInProduct(ErpPurchaseInImportExcelVO row, Map<String, ErpProductDO> productMap) {
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

    private ErpWarehouseDO getPurchaseInWarehouse(ErpPurchaseInImportExcelVO row, Map<String, ErpWarehouseDO> warehouseMap) {
        String warehouseName = trimToNull(row.getWarehouseName());
        if (warehouseName == null) {
            throw new IllegalArgumentException("仓库名称不能为空");
        }
        ErpWarehouseDO warehouse = warehouseMap.get(warehouseName);
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在：" + warehouseName);
        }
        return warehouse;
    }

    private BigDecimal defaultPurchaseInPrice(BigDecimal importPrice, BigDecimal productPrice) {
        BigDecimal price = importPrice != null ? importPrice : productPrice;
        if (price == null) {
            return BigDecimal.ZERO;
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("入库单价不能小于 0");
        }
        return price;
    }

    private Integer defaultPackageQty(Integer packageQty) {
        return packageQty == null || packageQty <= 0 ? 1 : packageQty;
    }

    private BigDecimal requirePositiveCount(BigDecimal count, String message) {
        if (count == null || count.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return count;
    }

    private void fillPurchaseInProductFields(ErpPurchaseInSaveReqVO.Item item, ErpProductDO product, ErpProductRespVO productVO) {
        item.setProductCode(product.getCode());
        item.setProductName(product.getName());
        item.setBarCode(product.getBarCode());
        item.setBrand(product.getBrand());
        item.setVehicleModel(product.getVehicleModel());
        item.setOriginPlace(product.getOriginPlace());
        item.setDrawingNo(product.getDrawingNo());
        if (productVO != null) {
            item.setProductUnitName(productVO.getUnitName());
        }
    }

    private static String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isEmpty(trimmed) ? null : trimmed;
    }

}
