package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleReturnModeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpSaleReturnServiceImpl implements ErpSaleReturnService {

    @Resource
    private ErpSaleReturnMapper saleReturnMapper;
    @Resource
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpProductService productService;
    @Resource
    @Lazy
    private ErpSaleOrderService saleOrderService;
    @Resource
    @Lazy
    private ErpSaleOutService saleOutService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
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
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpWarehouseService warehouseService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleReturn(ErpSaleReturnSaveReqVO createReqVO) {
        Integer returnMode = normalizeReturnMode(createReqVO);
        ErpSaleOrderDO saleOrder = null;
        ErpSaleOutDO saleOut = null;
        if (ErpSaleReturnModeEnum.LEGACY_ORDER.getMode().equals(returnMode)) {
            saleOrder = saleOrderService.validateSaleOrder(createReqVO.getOrderId());
        } else if (ErpSaleReturnModeEnum.isBySaleOut(returnMode)) {
            saleOut = validateSaleOutReturnable(createReqVO, null);
        } else if (ErpSaleReturnModeEnum.isByStock(returnMode)) {
            validateByStockCustomer(createReqVO.getCustomerId());
        } else {
            throw exception(SALE_RETURN_MODE_INVALID);
        }

        List<ErpSaleReturnItemDO> saleReturnItems = validateSaleReturnItems(createReqVO.getItems());
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        if (createReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(createReqVO.getSaleUserId());
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_RETURN_NO_PREFIX);
        if (saleReturnMapper.selectByNo(no) != null) {
            throw exception(SALE_RETURN_NO_EXISTS);
        }

        ErpSaleReturnDO saleReturn = BeanUtils.toBean(createReqVO, ErpSaleReturnDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()).setReturnMode(returnMode));
        fillSourceInfo(saleReturn, saleOrder, saleOut);
        calculateTotalPrice(saleReturn, saleReturnItems);
        saleReturnMapper.insert(saleReturn);
        saleReturnItems.forEach(item -> item.setReturnId(saleReturn.getId()));
        saleReturnItemMapper.insertBatch(saleReturnItems);

        updateSaleOrderReturnCountIfPresent(saleReturn.getOrderId());
        return saleReturn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleReturn(ErpSaleReturnSaveReqVO updateReqVO) {
        ErpSaleReturnDO oldSaleReturn = validateSaleReturnExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(oldSaleReturn.getStatus())) {
            throw exception(SALE_RETURN_UPDATE_FAIL_APPROVE, oldSaleReturn.getNo());
        }

        Integer returnMode = normalizeReturnMode(updateReqVO);
        ErpSaleOrderDO saleOrder = null;
        ErpSaleOutDO saleOut = null;
        if (ErpSaleReturnModeEnum.LEGACY_ORDER.getMode().equals(returnMode)) {
            saleOrder = saleOrderService.validateSaleOrder(updateReqVO.getOrderId());
        } else if (ErpSaleReturnModeEnum.isBySaleOut(returnMode)) {
            saleOut = validateSaleOutReturnable(updateReqVO, updateReqVO.getId());
        } else if (ErpSaleReturnModeEnum.isByStock(returnMode)) {
            validateByStockCustomer(updateReqVO.getCustomerId());
        } else {
            throw exception(SALE_RETURN_MODE_INVALID);
        }

        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        List<ErpSaleReturnItemDO> saleReturnItems = validateSaleReturnItems(updateReqVO.getItems());

        ErpSaleReturnDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleReturnDO.class)
                .setReturnMode(returnMode);
        fillSourceInfo(updateObj, saleOrder, saleOut);
        calculateTotalPrice(updateObj, saleReturnItems);
        saleReturnMapper.updateById(updateObj);
        updateSaleReturnItemList(updateReqVO.getId(), saleReturnItems);

        updateSaleOrderReturnCountIfPresent(updateObj.getOrderId());
        if (ObjectUtil.notEqual(oldSaleReturn.getOrderId(), updateObj.getOrderId())) {
            updateSaleOrderReturnCountIfPresent(oldSaleReturn.getOrderId());
        }
    }

    private Integer normalizeReturnMode(ErpSaleReturnSaveReqVO reqVO) {
        if (reqVO.getReturnMode() != null) {
            return reqVO.getReturnMode();
        }
        if (reqVO.getOrderId() != null) {
            return ErpSaleReturnModeEnum.LEGACY_ORDER.getMode();
        }
        if (reqVO.getSourceOutId() != null) {
            return ErpSaleReturnModeEnum.BY_SALE_OUT.getMode();
        }
        return ErpSaleReturnModeEnum.BY_STOCK.getMode();
    }

    private void validateByStockCustomer(Long customerId) {
        if (customerId == null) {
            throw exception(SALE_RETURN_BY_STOCK_CUSTOMER_REQUIRED);
        }
        customerService.validateCustomer(customerId);
    }

    private void fillSourceInfo(ErpSaleReturnDO saleReturn, ErpSaleOrderDO saleOrder, ErpSaleOutDO saleOut) {
        if (saleOrder != null) {
            saleReturn.setOrderNo(saleOrder.getNo()).setCustomerId(saleOrder.getCustomerId());
        } else if (saleOut != null) {
            saleReturn.setSourceOutId(saleOut.getId()).setSourceOutNo(saleOut.getNo())
                    .setCustomerId(saleOut.getCustomerId()).setOrderId(null).setOrderNo(null);
        } else {
            saleReturn.setOrderId(null).setOrderNo(null).setSourceOutId(null).setSourceOutNo(null);
        }
    }

    private ErpSaleOutDO validateSaleOutReturnable(ErpSaleReturnSaveReqVO reqVO, Long excludeReturnId) {
        if (reqVO.getSourceOutId() == null || CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(SALE_RETURN_BY_SALE_OUT_SOURCE_REQUIRED);
        }
        ErpSaleOutDO saleOut = saleOutService.validateSaleOut(reqVO.getSourceOutId());
        Map<Long, ErpSaleOutItemDO> sourceItemMap = convertMap(
                saleOutService.getSaleOutItemListByOutId(reqVO.getSourceOutId()), ErpSaleOutItemDO::getId);
        Map<Long, BigDecimal> currentCountMap = new HashMap<>();
        reqVO.getItems().forEach(item -> {
            if (item.getSourceOutItemId() == null) {
                throw exception(SALE_RETURN_BY_SALE_OUT_SOURCE_REQUIRED);
            }
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(SALE_RETURN_COUNT_POSITIVE);
            }
            currentCountMap.merge(item.getSourceOutItemId(), item.getCount(), BigDecimal::add);
        });

        List<ErpSaleReturnDO> saleReturns = saleReturnMapper.selectListBySourceOutId(reqVO.getSourceOutId());
        if (excludeReturnId != null) {
            saleReturns.removeIf(saleReturn -> excludeReturnId.equals(saleReturn.getId()));
        }
        Map<Long, BigDecimal> returnedCountMap = saleReturnItemMapper.selectSourceOutItemCountSumMapByReturnIds(
                convertList(saleReturns, ErpSaleReturnDO::getId));
        currentCountMap.forEach((sourceOutItemId, count) -> {
            ErpSaleOutItemDO sourceItem = sourceItemMap.get(sourceOutItemId);
            if (sourceItem == null) {
                throw exception(SALE_RETURN_SOURCE_OUT_ITEM_NOT_EXISTS, sourceOutItemId);
            }
            BigDecimal returnedCount = returnedCountMap.getOrDefault(sourceOutItemId, BigDecimal.ZERO);
            BigDecimal returnableCount = sourceItem.getCount().subtract(returnedCount);
            if (count.compareTo(returnableCount) > 0) {
                throw exception(SALE_RETURN_EXCEED_RETURNABLE, sourceOutItemId, count, returnableCount);
            }
        });
        return saleOut;
    }

    private void calculateTotalPrice(ErpSaleReturnDO saleReturn, List<ErpSaleReturnItemDO> saleReturnItems) {
        saleReturn.setTotalCount(getSumValue(saleReturnItems, ErpSaleReturnItemDO::getCount, BigDecimal::add));
        saleReturn.setTotalProductPrice(getSumValue(saleReturnItems, ErpSaleReturnItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        saleReturn.setTotalTaxPrice(getSumValue(saleReturnItems, ErpSaleReturnItemDO::getTaxPrice, BigDecimal::add, BigDecimal.ZERO));
        saleReturn.setTotalPrice(saleReturn.getTotalProductPrice().add(saleReturn.getTotalTaxPrice()));
        if (saleReturn.getDiscountPercent() == null) {
            saleReturn.setDiscountPercent(BigDecimal.ZERO);
        }
        if (saleReturn.getOtherPrice() == null) {
            saleReturn.setOtherPrice(BigDecimal.ZERO);
        }
        saleReturn.setDiscountPrice(MoneyUtils.priceMultiplyPercent(saleReturn.getTotalPrice(), saleReturn.getDiscountPercent()));
        saleReturn.setTotalPrice(saleReturn.getTotalPrice().subtract(saleReturn.getDiscountPrice()).add(saleReturn.getOtherPrice()));
        if (saleReturn.getRefundPrice() == null) {
            saleReturn.setRefundPrice(BigDecimal.ZERO);
        }
    }

    private void updateSaleOrderReturnCountIfPresent(Long orderId) {
        if (orderId == null) {
            return;
        }
        List<ErpSaleReturnDO> saleReturns = saleReturnMapper.selectListByOrderId(orderId);
        Map<Long, BigDecimal> returnCountMap = saleReturnItemMapper.selectOrderItemCountSumMapByReturnIds(
                convertList(saleReturns, ErpSaleReturnDO::getId));
        saleOrderService.updateSaleOrderReturnCount(orderId, returnCountMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleReturnStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        ErpSaleReturnDO saleReturn = validateSaleReturnExists(id);
        if (saleReturn.getStatus().equals(status)) {
            throw exception(approve ? SALE_RETURN_APPROVE_FAIL : SALE_RETURN_PROCESS_FAIL);
        }
        if (!approve && saleReturn.getRefundPrice().compareTo(BigDecimal.ZERO) > 0) {
            throw exception(SALE_RETURN_PROCESS_FAIL_EXISTS_REFUND);
        }

        // 反审：先校验并删除关联凭证（红字销售凭证）
        if (!approve) {
            List<ErpVoucherDO> related = voucherMapper.selectListByBiz(
                    ErpVoucherSourceBizTypeEnum.SALE_RETURN.getType(), id);
            for (ErpVoucherDO v : related) {
                if (ErpVoucherAuditStatusEnum.APPROVE.getStatus().equals(v.getAuditStatus())) {
                    throw exception(BIZ_PROCESS_FAIL_VOUCHER_APPROVED, v.getVoucherNo());
                }
                voucherMapper.deleteById(v.getId());
                voucherItemMapper.delete(new LambdaQueryWrapper<ErpVoucherItemDO>()
                        .eq(ErpVoucherItemDO::getVoucherId, v.getId()));
            }
        }

        int updateCount = saleReturnMapper.updateByIdAndStatus(id, saleReturn.getStatus(),
                new ErpSaleReturnDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(approve ? SALE_RETURN_APPROVE_FAIL : SALE_RETURN_PROCESS_FAIL);
        }

        List<ErpSaleReturnItemDO> saleReturnItems = saleReturnItemMapper.selectListByReturnId(id);
        Integer bizType = approve ? ErpStockRecordBizTypeEnum.SALE_RETURN.getType()
                : ErpStockRecordBizTypeEnum.SALE_RETURN_CANCEL.getType();
        saleReturnItems.forEach(saleReturnItem -> {
            BigDecimal count = approve ? saleReturnItem.getCount() : saleReturnItem.getCount().negate();
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    saleReturnItem.getProductId(), saleReturnItem.getWarehouseId(), count,
                    bizType, saleReturnItem.getReturnId(), saleReturnItem.getId(), saleReturn.getNo(),
                    saleReturnItem.getProductPrice(), saleReturn.getReturnTime()));
        });

        // 更新原销售单的退货状态
        if (saleReturn.getSourceOutId() != null) {
            updateSaleOutReturnStatus(saleReturn.getSourceOutId());
        }

        // 审核通过：自动生成销售红字凭证（已开账并启用销售凭证类型）
        if (approve && saleReturn.getReturnTime() != null && bookOpenService.isVoucherTypeEnabled(
                saleReturn.getReturnTime().atZone(ZoneId.systemDefault()).toLocalDate(),
                ErpVoucherTypeEnum.SALE.getType())) {
            BigDecimal sumCost = BigDecimal.ZERO;
            for (ErpSaleReturnItemDO item : saleReturnItems) {
                ErpStockDO stock = stockService.getStock(item.getProductId(), item.getWarehouseId());
                BigDecimal cost = (stock != null && stock.getCostPrice() != null)
                        ? stock.getCostPrice() : BigDecimal.ZERO;
                sumCost = sumCost.add(cost.multiply(item.getCount()));
            }
            String customerName = saleReturn.getCustomerId() != null
                    ? customerService.getCustomer(saleReturn.getCustomerId()).getName() : "";
            List<ErpVoucherItemDO> voucherItems = autoVoucherBuilder.buildSaleReturnItems(saleReturn, customerName, sumCost);
            voucherService.createVoucherFromBiz(
                    ErpVoucherSourceBizTypeEnum.SALE_RETURN.getType(),
                    saleReturn.getId(),
                    saleReturn.getNo(),
                    saleReturn.getTotalPrice(),
                    saleReturn.getReturnTime().atZone(ZoneId.systemDefault()).toLocalDate(),
                    "销售退货 - " + customerName,
                    voucherItems);
        }
    }

    @Override
    public void updateSaleReturnRefundPrice(Long id, BigDecimal refundPrice) {
        ErpSaleReturnDO saleReturn = saleReturnMapper.selectById(id);
        if (saleReturn.getRefundPrice().equals(refundPrice)) {
            return;
        }
        if (refundPrice.compareTo(saleReturn.getTotalPrice()) > 0) {
            throw exception(SALE_RETURN_FAIL_REFUND_PRICE_EXCEED, refundPrice, saleReturn.getTotalPrice());
        }
        saleReturnMapper.updateById(new ErpSaleReturnDO().setId(id).setRefundPrice(refundPrice));
    }

    private List<ErpSaleReturnItemDO> validateSaleReturnItems(List<ErpSaleReturnSaveReqVO.Item> list) {
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpSaleReturnSaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        return convertList(list, itemVO -> BeanUtils.toBean(itemVO, ErpSaleReturnItemDO.class, item -> {
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(SALE_RETURN_COUNT_POSITIVE);
            }
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            if (item.getTotalPrice() != null && item.getTaxPercent() != null) {
                item.setTaxPrice(MoneyUtils.priceMultiplyPercent(item.getTotalPrice(), item.getTaxPercent()));
            }
        }));
    }

    private void updateSaleReturnItemList(Long id, List<ErpSaleReturnItemDO> newList) {
        List<ErpSaleReturnItemDO> oldList = saleReturnItemMapper.selectListByReturnId(id);
        List<List<ErpSaleReturnItemDO>> diffList = diffList(oldList, newList,
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(item -> item.setReturnId(id));
            saleReturnItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            saleReturnItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            saleReturnItemMapper.deleteByIds(convertList(diffList.get(2), ErpSaleReturnItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleReturn(List<Long> ids) {
        List<ErpSaleReturnDO> saleReturns = saleReturnMapper.selectByIds(ids);
        if (CollUtil.isEmpty(saleReturns)) {
            return;
        }
        saleReturns.forEach(saleReturn -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(saleReturn.getStatus())) {
                throw exception(SALE_RETURN_DELETE_FAIL_APPROVE, saleReturn.getNo());
            }
        });

        saleReturns.forEach(saleReturn -> {
            saleReturnMapper.deleteById(saleReturn.getId());
            saleReturnItemMapper.deleteByReturnId(saleReturn.getId());
            updateSaleOrderReturnCountIfPresent(saleReturn.getOrderId());
        });
    }

    private ErpSaleReturnDO validateSaleReturnExists(Long id) {
        ErpSaleReturnDO saleReturn = saleReturnMapper.selectById(id);
        if (saleReturn == null) {
            throw exception(SALE_RETURN_NOT_EXISTS);
        }
        return saleReturn;
    }

    @Override
    public ErpSaleReturnDO getSaleReturn(Long id) {
        return saleReturnMapper.selectById(id);
    }

    @Override
    public ErpSaleReturnDO validateSaleReturn(Long id) {
        ErpSaleReturnDO saleReturn = validateSaleReturnExists(id);
        if (ObjectUtil.notEqual(saleReturn.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(SALE_RETURN_NOT_APPROVE);
        }
        return saleReturn;
    }

    @Override
    public PageResult<ErpSaleReturnDO> getSaleReturnPage(ErpSaleReturnPageReqVO pageReqVO) {
        return saleReturnMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpSaleReturnItemDO> getSaleReturnItemListByReturnId(Long returnId) {
        return saleReturnItemMapper.selectListByReturnId(returnId);
    }

    @Override
    public List<ErpSaleReturnItemDO> getSaleReturnItemListByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyList();
        }
        return saleReturnItemMapper.selectListByReturnIds(returnIds);
    }

    @Override
    public ErpSaleReturnImportRespVO parseImportData(List<ErpSaleReturnImportExcelVO> list) {
        ErpSaleReturnImportRespVO respVO = new ErpSaleReturnImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        LinkedHashSet<String> productCodes = new LinkedHashSet<>();
        list.forEach(row -> {
            if (row.getProductCode() != null && !row.getProductCode().isEmpty()) {
                productCodes.add(row.getProductCode());
            }
        });
        Map<String, ErpProductDO> productMap = convertMap(productMapper.selectListByCodes(productCodes), ErpProductDO::getCode);
        Map<Long, ErpProductRespVO> productVOMap = productService.getProductVOMap(convertList(productMap.values(), ErpProductDO::getId));
        Map<String, ErpWarehouseDO> warehouseMap = convertMap(
                warehouseService.getWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()), ErpWarehouseDO::getName);
        for (int i = 0; i < list.size(); i++) {
            ErpSaleReturnImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (row.getProductCode() == null || row.getProductCode().isEmpty()) {
                respVO.getFailureDetails().add(new ErpSaleReturnImportRespVO.FailureItem(rowNo, null, "产品编码不能为空"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpProductDO product = productMap.get(row.getProductCode());
            if (product == null) {
                respVO.getFailureDetails().add(new ErpSaleReturnImportRespVO.FailureItem(rowNo, row.getProductCode(), "产品不存在"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            if (row.getCount() == null || row.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                respVO.getFailureDetails().add(new ErpSaleReturnImportRespVO.FailureItem(rowNo, row.getProductCode(), "退货数量必须大于 0"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpWarehouseDO warehouse = row.getWarehouseName() == null || row.getWarehouseName().isEmpty()
                    ? null : warehouseMap.get(row.getWarehouseName());
            if (row.getWarehouseName() != null && !row.getWarehouseName().isEmpty() && warehouse == null) {
                respVO.getFailureDetails().add(new ErpSaleReturnImportRespVO.FailureItem(rowNo, row.getProductCode(), "仓库不存在"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpSaleReturnRespVO.Item item = new ErpSaleReturnRespVO.Item();
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductName(product.getName());
            item.setProductUnitId(product.getUnitId());
            item.setProductUnitName(productVOMap.get(product.getId()) == null ? null : productVOMap.get(product.getId()).getUnitName());
            item.setWarehouseId(warehouse == null ? product.getDefaultWarehouseId() : warehouse.getId());
            item.setWarehouseName(warehouse == null ? null : warehouse.getName());
            item.setProductPrice(row.getProductPrice() != null ? row.getProductPrice() : product.getSalePrice());
            item.setCount(row.getCount());
            item.setReturnReason(row.getReturnReason());
            item.setRemark(row.getRemark());
            respVO.getItems().add(item);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    private void updateSaleOutReturnStatus(Long saleOutId) {
        // 1. 查询原销售单所有明细的总数量
        List<ErpSaleOutItemDO> outItems = saleOutItemMapper.selectListByOutId(saleOutId);
        BigDecimal totalOutCount = outItems.stream()
                .map(ErpSaleOutItemDO::getCount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. 查询该销售单所有已审批退货单的退货总数量
        List<ErpSaleReturnDO> approvedReturns = saleReturnMapper.selectListBySourceOutId(saleOutId);
        approvedReturns.removeIf(r -> !ErpAuditStatus.APPROVE.getStatus().equals(r.getStatus()));
        BigDecimal totalReturnedCount = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(approvedReturns)) {
            List<ErpSaleReturnItemDO> returnItems = saleReturnItemMapper.selectListByReturnIds(
                    convertList(approvedReturns, ErpSaleReturnDO::getId));
            totalReturnedCount = returnItems.stream()
                    .map(ErpSaleReturnItemDO::getCount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 3. 判断状态
        Integer returnStatus;
        if (totalReturnedCount.compareTo(BigDecimal.ZERO) == 0) {
            returnStatus = 0; // 未退货
        } else if (totalReturnedCount.compareTo(totalOutCount) >= 0) {
            returnStatus = 2; // 已退货
        } else {
            returnStatus = 1; // 部分退货
        }

        // 4. 更新
        saleOutMapper.updateById(new ErpSaleOutDO().setId(saleOutId).setReturnStatus(returnStatus));
    }

}
