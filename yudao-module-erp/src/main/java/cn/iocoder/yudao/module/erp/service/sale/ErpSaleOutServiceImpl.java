package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
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
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.*;

// TODO 芋艿：记录操作日志

/**
 * ERP 销售出库 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpSaleOutServiceImpl implements ErpSaleOutService {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_out";

    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Resource
    private ErpSaleReturnMapper saleReturnMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private ErpSaleOrderService saleOrderService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpOperateLogService operateLogService;

    @Resource
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Resource
    private ErpVoucherService voucherService;
    @Resource
    private ErpBookOpenService bookOpenService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleOut(ErpSaleOutSaveReqVO createReqVO) {
        clearHiddenFields(createReqVO);
        clearHiddenItemFields(createReqVO.getItems());
        // 1.1 校验销售订单已审核
        ErpSaleOrderDO saleOrder = saleOrderService.validateSaleOrder(createReqVO.getOrderId());
        // 1.2 校验出库项的有效性
        List<ErpSaleOutItemDO> saleOutItems = validateSaleOutItems(createReqVO.getItems(), createReqVO.getOrderId());
        // 1.3 校验结算账户
        accountService.validateAccount(createReqVO.getAccountId());
        // 1.4 校验销售人员
        if (createReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(createReqVO.getSaleUserId());
        }
        // 1.5 生成出库单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_OUT_NO_PREFIX);
        if (saleOutMapper.selectByNo(no) != null) {
            throw exception(SALE_OUT_NO_EXISTS);
        }

        // 2.1 插入出库
        ErpSaleOutDO saleOut = BeanUtils.toBean(createReqVO, ErpSaleOutDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()))
                .setOrderNo(saleOrder.getNo()).setCustomerId(saleOrder.getCustomerId());
        calculateTotalPrice(saleOut, saleOutItems);
        saleDocumentDefaultService.fillCreateDefaults(saleOut);
        saleOutMapper.insert(saleOut);
        // 2.2 插入出库项
        saleOutItems.forEach(o -> o.setOutId(saleOut.getId()));
        saleOutItemMapper.insertBatch(saleOutItems);

        // 3. 更新销售订单的出库数量
        updateSaleOrderOutCount(createReqVO.getOrderId());
        recordCreate(saleOut.getId(), saleOut.getNo());
        return saleOut.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGeneratedSaleOut(ErpSaleOutSaveReqVO createReqVO, Integer sourceType, Long sourceId, String sourceNo) {
        clearHiddenFields(createReqVO);
        clearHiddenItemFields(createReqVO.getItems());
        // 1. 校验基础资料。新销售流程不再强制依赖旧销售订单。
        customerService.validateCustomer(createReqVO.getCustomerId());
        List<ErpSaleOutItemDO> saleOutItems = validateSaleOutItems(createReqVO.getItems());
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        if (createReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(createReqVO.getSaleUserId());
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_OUT_NO_PREFIX);
        if (saleOutMapper.selectByNo(no) != null) {
            throw exception(SALE_OUT_NO_EXISTS);
        }

        // 2. 插入销售单，并保留来源单据用于追溯。
        ErpSaleOutDO saleOut = BeanUtils.toBean(createReqVO, ErpSaleOutDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setSourceType(sourceType).setSourceId(sourceId).setSourceNo(sourceNo)
                .setOrderId(null).setOrderNo(null));
        calculateTotalPrice(saleOut, saleOutItems);
        saleDocumentDefaultService.fillCreateDefaults(saleOut);
        saleOutMapper.insert(saleOut);
        Long saleOutId = saleOut.getId();
        if (saleOutId == null) {
            ErpSaleOutDO inserted = DataPermissionUtils.executeIgnore(
                    () -> saleOutMapper.selectBySourceTypeAndSourceId(sourceType, sourceId));
            if (inserted == null) {
                throw exception(SALE_OUT_NO_EXISTS);
            }
            saleOutId = inserted.getId();
            saleOut.setId(saleOutId).setNo(inserted.getNo());
        }
        saleOutItems.forEach(o -> o.setOutId(saleOut.getId()).setOrderItemId(null));
        saleOutItemMapper.insertBatch(saleOutItems);

        // 3. 自动审核，复用现有销售出库扣库存流水。
        Long generatedSaleOutId = saleOutId;
        DataPermissionUtils.executeIgnore(() -> updateSaleOutStatus(generatedSaleOutId, ErpAuditStatus.APPROVE.getStatus()));
        recordCreate(generatedSaleOutId, saleOut.getNo());
        return generatedSaleOutId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOut(ErpSaleOutSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpSaleOutDO saleOut = validateSaleOutExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(saleOut.getStatus())) {
            throw exception(SALE_OUT_UPDATE_FAIL_APPROVE, saleOut.getNo());
        }
        preserveHiddenFields(updateReqVO, saleOut);
        preserveHiddenItemFields(updateReqVO.getItems(), saleOutItemMapper.selectListByOutId(updateReqVO.getId()));
        // 1.2 校验销售订单已审核
        ErpSaleOrderDO saleOrder = saleOrderService.validateSaleOrder(updateReqVO.getOrderId());
        // 1.3 校验结算账户
        accountService.validateAccount(updateReqVO.getAccountId());
        // 1.4 校验销售人员
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        // 1.5 校验订单项的有效性
        List<ErpSaleOutItemDO> saleOutItems = validateSaleOutItems(updateReqVO.getItems(), updateReqVO.getOrderId());

        // 2.1 更新出库
        ErpSaleOutDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleOutDO.class)
                .setOrderNo(saleOrder.getNo()).setCustomerId(saleOrder.getCustomerId());
        calculateTotalPrice(updateObj, saleOutItems);
        saleOutMapper.updateById(updateObj);
        // 2.2 更新出库项
        updateSaleOutItemList(updateReqVO.getId(), saleOutItems);

        // 3.1 更新销售订单的出库数量
        if (updateObj.getOrderId() != null) {
            updateSaleOrderOutCount(updateObj.getOrderId());
        }
        // 3.2 注意：如果销售订单编号变更了，需要更新“老”销售订单的出库数量
        if (ObjectUtil.notEqual(saleOut.getOrderId(), updateObj.getOrderId())) {
            if (saleOut.getOrderId() != null) {
                updateSaleOrderOutCount(saleOut.getOrderId());
            }
        }
        recordUpdate(updateReqVO.getId(), saleOut.getNo());
    }

    private void calculateTotalPrice(ErpSaleOutDO saleOut, List<ErpSaleOutItemDO> saleOutItems) {
        saleOut.setTotalCount(getSumValue(saleOutItems, ErpSaleOutItemDO::getCount, BigDecimal::add));
        saleOut.setTotalProductPrice(getSumValue(saleOutItems, ErpSaleOutItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        saleOut.setTotalTaxPrice(BigDecimal.ZERO);
        saleOut.setTotalPrice(saleOut.getTotalProductPrice());
        // 计算优惠价格
        if (saleOut.getDiscountPercent() == null) {
            saleOut.setDiscountPercent(BigDecimal.ZERO);
        }
        BigDecimal feeAmount = resolveFeeAmount(saleOut.getFeeAmount(), saleOut.getOtherPrice(), saleOut.getExtraFee());
        saleOut.setFeeAmount(feeAmount);
        saleOut.setOtherPrice(feeAmount);
        saleOut.setExtraFee(feeAmount);
        saleOut.setDiscountPrice(MoneyUtils.priceMultiplyPercent(saleOut.getTotalPrice(), saleOut.getDiscountPercent()));
        saleOut.setTotalPrice(saleOut.getTotalPrice().subtract(saleOut.getDiscountPrice()).add(feeAmount));
    }

    private BigDecimal resolveFeeAmount(BigDecimal feeAmount, BigDecimal otherPrice, BigDecimal extraFee) {
        if (feeAmount != null) {
            return feeAmount;
        }
        if (otherPrice != null) {
            return otherPrice;
        }
        return extraFee != null ? extraFee : BigDecimal.ZERO;
    }

    private void updateSaleOrderOutCount(Long orderId) {
        // 1.1 查询销售订单对应的销售出库单列表
        List<ErpSaleOutDO> saleOuts = saleOutMapper.selectListByOrderId(orderId);
        // 1.2 查询对应的销售订单项的出库数量
        Map<Long, BigDecimal> returnCountMap = saleOutItemMapper.selectOrderItemCountSumMapByOutIds(
                convertList(saleOuts, ErpSaleOutDO::getId));
        // 2. 更新销售订单的出库数量
        saleOrderService.updateSaleOrderOutCount(orderId, returnCountMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOutStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(SALE_OUT_PROCESS_FAIL);
        }
        // 1.1 校验存在
        ErpSaleOutDO saleOut = validateSaleOutExists(id);
        // 1.2 校验状态
        if (!ErpAuditStatus.PROCESS.getStatus().equals(saleOut.getStatus())) {
            throw exception(SALE_OUT_APPROVE_FAIL);
        }

        // 2. 更新状态
        int updateCount = saleOutMapper.updateByIdAndStatus(id, saleOut.getStatus(),
                new ErpSaleOutDO().setStatus(ErpAuditStatus.APPROVE.getStatus()));
        if (updateCount == 0) {
            throw exception(SALE_OUT_APPROVE_FAIL);
        }

        // 3. 变更库存
        List<ErpSaleOutItemDO> saleOutItems = saleOutItemMapper.selectListByOutId(id);
        Integer bizType = ErpStockRecordBizTypeEnum.SALE_OUT.getType();

        // 3.1 审批通过且销售凭证已开账时，先快照成本（必须前置于扣库存）
        boolean enableVoucher = bookOpenService.isVoucherTypeEnabled(
                saleOut.getOutTime().toLocalDate(), ErpVoucherTypeEnum.SALE.getType());
        BigDecimal sumCost = BigDecimal.ZERO;
        if (enableVoucher) {
            for (ErpSaleOutItemDO item : saleOutItems) {
                ErpStockDO stock = stockService.getStock(item.getProductId(), item.getWarehouseId());
                BigDecimal cost = (stock != null && stock.getCostPrice() != null)
                        ? stock.getCostPrice() : BigDecimal.ZERO;
                sumCost = sumCost.add(cost.multiply(item.getCount()));
            }
        }

        // 3.2 扣库存
        saleOutItems.forEach(saleOutItem -> {
            BigDecimal count = saleOutItem.getCount().negate();
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    saleOutItem.getProductId(), saleOutItem.getWarehouseId(), count,
                    bizType, saleOutItem.getOutId(), saleOutItem.getId(), saleOut.getNo(),
                    saleOutItem.getProductPrice(), saleOut.getOutTime()));
        });

        // 4. 审批通过且已开账：生成销售凭证
        if (enableVoucher) {
            ErpCustomerDO customer = customerService.getCustomer(saleOut.getCustomerId());
            String customerName = customer != null ? customer.getName() : "";
            List<ErpVoucherItemDO> voucherItems = autoVoucherBuilder.buildSaleOutItems(
                    saleOut, customerName, sumCost);
            voucherService.createVoucherFromBiz(
                    ErpVoucherSourceBizTypeEnum.SALE_OUT.getType(),
                    saleOut.getId(),
                    saleOut.getNo(),
                    saleOut.getTotalPrice(),
                    saleOut.getOutTime().toLocalDate(),
                    "销售出库 - " + customerName,
                    voucherItems);
        }
        recordStatus(id, saleOut.getNo(), true);
    }

    @Override
    public void updateSaleInReceiptPrice(Long id, BigDecimal receiptPrice) {
        ErpSaleOutDO saleOut = saleOutMapper.selectById(id);
        if (saleOut.getReceiptPrice().equals(receiptPrice)) {
            return;
        }
        if (receiptPrice.compareTo(saleOut.getTotalPrice()) > 0) {
            throw exception(SALE_OUT_FAIL_RECEIPT_PRICE_EXCEED, receiptPrice,  saleOut.getTotalPrice());
        }
        saleOutMapper.updateById(new ErpSaleOutDO().setId(id).setReceiptPrice(receiptPrice));
    }

    private List<ErpSaleOutItemDO> validateSaleOutItems(List<ErpSaleOutSaveReqVO.Item> list) {
        return validateSaleOutItems(list, null);
    }

    private List<ErpSaleOutItemDO> validateSaleOutItems(List<ErpSaleOutSaveReqVO.Item> list, Long orderId) {
        // 1. 校验产品存在
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpSaleOutSaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        Map<Long, Boolean> orderItemGiftFlagMap = buildOrderItemGiftFlagMap(orderId);
        // 2. 转化为 ErpSaleOutItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleOutItemDO.class, item -> {
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            item.setGiftFlag(resolveGiftFlag(o.getGiftFlag(), item.getOrderItemId(), orderItemGiftFlagMap));
            if (Boolean.TRUE.equals(item.getGiftFlag())) {
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

    private Map<Long, Boolean> buildOrderItemGiftFlagMap(Long orderId) {
        if (orderId == null) {
            return Collections.emptyMap();
        }
        List<ErpSaleOrderItemDO> orderItems = saleOrderService.getSaleOrderItemListByOrderId(orderId);
        if (CollUtil.isEmpty(orderItems)) {
            return Collections.emptyMap();
        }
        return orderItems.stream().collect(Collectors.toMap(ErpSaleOrderItemDO::getId,
                item -> Boolean.TRUE.equals(item.getGiftFlag()), (oldValue, newValue) -> oldValue));
    }

    private Boolean resolveGiftFlag(Boolean reqGiftFlag, Long orderItemId, Map<Long, Boolean> orderItemGiftFlagMap) {
        if (reqGiftFlag != null) {
            return Boolean.TRUE.equals(reqGiftFlag);
        }
        if (orderItemId != null && orderItemGiftFlagMap.containsKey(orderItemId)) {
            return Boolean.TRUE.equals(orderItemGiftFlagMap.get(orderItemId));
        }
        return Boolean.FALSE;
    }

    private void updateSaleOutItemList(Long id, List<ErpSaleOutItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpSaleOutItemDO> oldList = saleOutItemMapper.selectListByOutId(id);
        List<List<ErpSaleOutItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setOutId(id));
            saleOutItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            saleOutItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            saleOutItemMapper.deleteByIds(convertList(diffList.get(2), ErpSaleOutItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleOut(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpSaleOutDO> saleOuts = saleOutMapper.selectByIds(ids);
        if (CollUtil.isEmpty(saleOuts)) {
            return;
        }
        saleOuts.forEach(saleOut -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(saleOut.getStatus())) {
                throw exception(SALE_OUT_DELETE_FAIL_APPROVE, saleOut.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        saleOuts.forEach(saleOut -> {
            // 2.1 删除订单
            saleOutMapper.deleteById(saleOut.getId());
            // 2.2 删除订单项
            saleOutItemMapper.deleteByOutId(saleOut.getId());

            // 2.3 更新销售订单的出库数量
            if (saleOut.getOrderId() != null) {
                updateSaleOrderOutCount(saleOut.getOrderId());
            }
            recordDelete(saleOut.getId(), saleOut.getNo());
        });

    }

    private ErpSaleOutDO validateSaleOutExists(Long id) {
        ErpSaleOutDO saleOut = saleOutMapper.selectById(id);
        if (saleOut == null) {
            throw exception(SALE_OUT_NOT_EXISTS);
        }
        return saleOut;
    }

    @Override
    public ErpSaleOutDO getSaleOut(Long id) {
        return saleOutMapper.selectById(id);
    }

    @Override
    public ErpSaleOutDO validateSaleOut(Long id) {
        ErpSaleOutDO saleOut = validateSaleOutExists(id);
        if (ObjectUtil.notEqual(saleOut.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(SALE_OUT_NOT_APPROVE);
        }
        return saleOut;
    }

    @Override
    public PageResult<ErpSaleOutDO> getSaleOutPage(ErpSaleOutPageReqVO pageReqVO) {
        return saleOutMapper.selectPage(pageReqVO);
    }

    // ==================== 销售出库项 ====================

    @Override
    public List<ErpSaleOutItemDO> getSaleOutItemListByOutId(Long outId) {
        return saleOutItemMapper.selectListByOutId(outId);
    }

    @Override
    public List<ErpSaleOutItemDO> getSaleOutItemListByOutIds(Collection<Long> outIds) {
        if (CollUtil.isEmpty(outIds)) {
            return Collections.emptyList();
        }
        return saleOutItemMapper.selectListByOutIds(outIds);
    }

    @Override
    public List<ErpSaleReturnableItemRespVO> getReturnableItemsByOutId(Long outId) {
        ErpSaleOutDO saleOut = validateSaleOut(outId);
        List<ErpSaleOutItemDO> items = saleOutItemMapper.selectListByOutId(outId);
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        Map<Long, BigDecimal> returnedMap;
        if (saleReturnMapper == null) {
            returnedMap = saleReturnItemMapper.selectReturnedCountMapBySourceOutItemIds(
                    convertList(items, ErpSaleOutItemDO::getId));
        } else {
            List<ErpSaleReturnDO> approvedReturns = saleReturnMapper.selectListBySourceOutId(outId);
            approvedReturns.removeIf(saleReturn -> !ErpAuditStatus.APPROVE.getStatus().equals(saleReturn.getStatus()));
            returnedMap = CollUtil.isEmpty(approvedReturns) ? Collections.emptyMap()
                    : saleReturnItemMapper.selectSourceOutItemCountSumMapByReturnIds(
                            convertList(approvedReturns, ErpSaleReturnDO::getId));
        }
        return items.stream().map(item -> {
            ErpSaleReturnableItemRespVO vo = new ErpSaleReturnableItemRespVO();
            vo.setSourceOutId(outId);
            vo.setSourceOutItemId(item.getId());
            vo.setSourceOutNo(saleOut.getNo());
            vo.setProductId(item.getProductId());
            vo.setProductUnitId(item.getProductUnitId());
            vo.setWarehouseId(item.getWarehouseId());
            vo.setProductPrice(item.getProductPrice());
            vo.setOutCount(item.getCount());
            BigDecimal returned = returnedMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            vo.setReturnedCount(returned);
            BigDecimal returnable = item.getCount().subtract(returned);
            vo.setReturnableCount(returnable.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : returnable);

            vo.setRemark(item.getRemark());
            return vo;
        }).collect(Collectors.toList());
    }

    private void clearHiddenFields(Object target) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, target);
        }
    }

    private void clearHiddenItemFields(List<?> items) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, items);
        }
    }

    private void preserveHiddenFields(Object target, Object existing) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, target, existing);
        }
    }

    private void preserveHiddenItemFields(List<?> items, List<?> existingItems) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, items, existingItems);
        }
    }

    private void recordCreate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordCreate(ERP_SALE_OUT_TYPE, id, no);
        }
    }

    private void recordUpdate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordUpdate(ERP_SALE_OUT_TYPE, id, no);
        }
    }

    private void recordStatus(Long id, String no, boolean approve) {
        if (operateLogService != null) {
            operateLogService.recordStatus(ERP_SALE_OUT_TYPE, id, no, approve);
        }
    }

    private void recordDelete(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordDelete(ERP_SALE_OUT_TYPE, id, no);
        }
    }

}
