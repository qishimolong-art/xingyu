package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.imports.ErpSaleImportResultRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpImportProductResolver;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
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
    private ErpSaleDirectDeptPermissionService saleDirectDeptPermissionService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleOrder(ErpSaleOrderSaveReqVO createReqVO) {
        ErpSaleDecimalUtils.requireScale(createReqVO.getDiscountPercent(), "优惠率，百分比");
        ErpSaleDecimalUtils.requireScale(createReqVO.getFeeAmount(), "费用金额，单位：元");
        ErpSaleDecimalUtils.requireScale(createReqVO.getDepositPrice(), "定金金额，单位：元");
        if (createReqVO.getItems() != null) {
            for (int decimalRow = 0; decimalRow < createReqVO.getItems().size(); decimalRow++) {
                ErpSaleOrderSaveReqVO.Item decimalItem = createReqVO.getItems().get(decimalRow);
                if (decimalItem == null || "delete".equals(decimalItem.getOperation())) continue;
                ErpSaleDecimalUtils.requireScale(decimalItem.getWeight(), "第" + (decimalRow + 1) + "行重量");
                ErpSaleDecimalUtils.requireScale(decimalItem.getProductPrice(), "第" + (decimalRow + 1) + "行产品单价");
                ErpSaleDecimalUtils.requireScale(decimalItem.getCount(), "第" + (decimalRow + 1) + "行产品数量");
                ErpSaleDecimalUtils.requireScale(decimalItem.getTaxPercent(), "第" + (decimalRow + 1) + "行税率，百分比");
            }
        }
        fieldPermissionMasker.clearSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        fieldPermissionMasker.clearSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, createReqVO, createReqVO.getItems());
        // 1.1 校验订单项的有效性
        Long saleDeptId = resolveSaleDeptId(createReqVO.getDeptId());
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(saleDeptId);
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
        ErpSaleDecimalUtils.requireScale(updateReqVO.getDiscountPercent(), "优惠率，百分比");
        ErpSaleDecimalUtils.requireScale(updateReqVO.getFeeAmount(), "费用金额，单位：元");
        ErpSaleDecimalUtils.requireScale(updateReqVO.getDepositPrice(), "定金金额，单位：元");
        if (updateReqVO.getItems() != null) {
            for (int decimalRow = 0; decimalRow < updateReqVO.getItems().size(); decimalRow++) {
                ErpSaleOrderSaveReqVO.Item decimalItem = updateReqVO.getItems().get(decimalRow);
                if (decimalItem == null || "delete".equals(decimalItem.getOperation())) continue;
                ErpSaleDecimalUtils.requireScale(decimalItem.getWeight(), "第" + (decimalRow + 1) + "行重量");
                ErpSaleDecimalUtils.requireScale(decimalItem.getProductPrice(), "第" + (decimalRow + 1) + "行产品单价");
                ErpSaleDecimalUtils.requireScale(decimalItem.getCount(), "第" + (decimalRow + 1) + "行产品数量");
                ErpSaleDecimalUtils.requireScale(decimalItem.getTaxPercent(), "第" + (decimalRow + 1) + "行税率，百分比");
            }
        }
        // 1.1 校验存在
        ErpSaleOrderDO saleOrder = validateSaleOrderExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(saleOrder.getStatus())) {
            throw exception(SALE_ORDER_UPDATE_FAIL_APPROVE, saleOrder.getNo());
        }
        List<ErpSaleOrderItemDO> oldItems = saleOrderItemMapper.selectListByOrderIdForUpdate(updateReqVO.getId());
        fieldPermissionMasker.preserveSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, saleOrder);
        fieldPermissionMasker.preserveSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO, updateReqVO.getItems(),
                oldItems);
        boolean incrementalItems = ErpSaleItemOperationHelper.useIncrementalItems(updateReqVO.getItems(),
                ErpSaleOrderSaveReqVO.Item::getOperation, SALE_ORDER_ITEM_OPERATION_INVALID);
        ErpSaleItemOperationHelper.RequestChangeSet<ErpSaleOrderSaveReqVO.Item> itemChangeSet = null;
        List<ErpSaleOrderSaveReqVO.Item> itemReqs = updateReqVO.getItems();
        if (incrementalItems) {
            itemChangeSet = ErpSaleItemOperationHelper.buildRequestChangeSet(updateReqVO.getItems(), oldItems,
                    ErpSaleOrderSaveReqVO.Item.class, ErpSaleOrderSaveReqVO.Item::getId,
                    ErpSaleOrderSaveReqVO.Item::setId, ErpSaleOrderSaveReqVO.Item::getOperation,
                    ErpSaleOrderItemDO::getId, SALE_ORDER_ITEM_OPERATION_INVALID,
                    SALE_ORDER_ITEM_UPDATE_NOT_EXISTS);
            itemReqs = itemChangeSet.getFinalItems();
        }
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
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(saleDeptId);
        List<ErpSaleOrderItemDO> saleOrderItems = validateSaleOrderItems(itemReqs, saleDeptId);
        preserveSaleOrderDownstreamCounts(saleOrderItems, oldItems);

        // 2.1 更新订单
        ErpSaleOrderDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleOrderDO.class);
        calculateTotalPrice(updateObj, saleOrderItems);
        saleOrderMapper.updateById(updateObj);
        // 2.2 更新订单项
        if (incrementalItems) {
            applySaleOrderItemChangeSet(updateReqVO.getId(), itemChangeSet, saleOrderItems);
        } else {
            updateSaleOrderItemList(updateReqVO.getId(), saleOrderItems);
        }
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
        saleOrder.setDiscountPrice(ErpSaleDecimalUtils.multiplyPercent(saleOrder.getTotalPrice(), saleOrder.getDiscountPercent()));
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
        saleDirectDeptPermissionService.validateSaleDocumentDeptAllowed(saleOrder.getDeptId());

        // 2. 更新状态
        int updateCount = saleOrderMapper.updateByIdAndStatus(id, saleOrder.getStatus(),
                new ErpSaleOrderDO().setStatus(ErpAuditStatus.APPROVE.getStatus()));
        if (updateCount == 0) {
            throw exception(SALE_ORDER_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_SALE_ORDER_TYPE, id, saleOrder.getNo(), true);
    }

    private List<ErpSaleOrderItemDO> validateSaleOrderItems(List<ErpSaleOrderSaveReqVO.Item> list, Long saleDeptId) {
        if (CollUtil.isEmpty(list)) {
            throw exception(SALE_ORDER_ITEM_EMPTY);
        }
        // 1. 校验产品存在
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() ->
                productService.validProductList(convertSet(list, ErpSaleOrderSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
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
                warehouseService.validSaleSelectableWarehouseListForDept(warehouseIds, saleDeptId,
                        ErpWarehouseService.SALE_ORDER_ALL_PRODUCT_PERMISSION),
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
            item.setTotalPrice(ErpSaleDecimalUtils.multiply(item.getProductPrice(), item.getCount()));
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

    private void applySaleOrderItemChangeSet(Long orderId,
            ErpSaleItemOperationHelper.RequestChangeSet<ErpSaleOrderSaveReqVO.Item> changeSet,
            List<ErpSaleOrderItemDO> finalItems) {
        if (CollUtil.isNotEmpty(changeSet.getDeleteIds())) {
            saleOrderItemMapper.deleteByIds(changeSet.getDeleteIds());
        }
        List<ErpSaleOrderItemDO> insertList = finalItems.stream()
                .filter(item -> item.getId() == null)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(insertList)) {
            insertList.forEach(item -> item.setOrderId(orderId));
            saleOrderItemMapper.insertBatch(insertList);
        }
        List<ErpSaleOrderItemDO> updateList = finalItems.stream()
                .filter(item -> item.getId() != null && changeSet.getUpdateIds().contains(item.getId()))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(updateList)) {
            updateList.forEach(item -> item.setOrderId(orderId));
            saleOrderItemMapper.updateBatch(updateList);
        }
    }

    private void preserveSaleOrderDownstreamCounts(List<ErpSaleOrderItemDO> items,
                                                   List<ErpSaleOrderItemDO> oldItems) {
        if (CollUtil.isEmpty(items) || CollUtil.isEmpty(oldItems)) {
            return;
        }
        Map<Long, ErpSaleOrderItemDO> oldItemMap = convertMap(oldItems, ErpSaleOrderItemDO::getId);
        items.forEach(item -> {
            ErpSaleOrderItemDO oldItem = oldItemMap.get(item.getId());
            if (oldItem == null) {
                return;
            }
            item.setOutCount(oldItem.getOutCount());
            item.setReturnCount(oldItem.getReturnCount());
        });
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
    public List<ErpSaleOrderItemDO> getSaleOrderItemListByOrderId(Long orderId, String orderField,
                                                                  String orderDirection) {
        return saleOrderItemMapper.selectListByOrderId(orderId, orderField, orderDirection);
    }

    @Override
    public PageResult<ErpSaleOrderItemDO> getSaleOrderItemPage(ErpSaleOrderItemPageReqVO pageReqVO) {
        validateSaleOrderExists(pageReqVO.getOrderId());
        return saleOrderItemMapper.selectPageByOrderId(pageReqVO);
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
        ErpImportProductResolver productResolver = ErpImportProductResolver.build(list,
                ErpSaleOrderImportExcelVO::getProductCode, ErpSaleOrderImportExcelVO::getProductName, ErpSaleOrderImportExcelVO::getFactoryCode, productMapper);
        Map<Long, ErpProductRespVO> productVOMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertList(productResolver.getResolvedProducts(), ErpProductDO::getId)));
        for (int i = 0; i < list.size(); i++) {
            ErpSaleOrderImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            String decimalError = ErpSaleDecimalUtils.importScaleError(row);
            if (decimalError != null) {
                respVO.getFailureDetails().add(new ErpSaleOrderImportRespVO.FailureItem(rowNo, row.getProductCode(), decimalError));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpImportProductResolver.ResolveResult productResult =
                    productResolver.resolve(row.getProductCode(), row.getProductName(), row.getFactoryCode());
            if (productResult.isFailure()) {
                respVO.getFailureDetails().add(new ErpSaleOrderImportRespVO.FailureItem(
                        rowNo, productResult.getIdentifier(), productResult.getErrorMessage()));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpProductDO product = productResult.getProduct();
            if (row.getCount() == null || row.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                respVO.getFailureDetails().add(new ErpSaleOrderImportRespVO.FailureItem(
                        rowNo, productResult.getIdentifier(), "数量必须大于 0"));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpSaleImportResultRespVO importSaleOrderList(List<ErpSaleOrderOrderImportExcelVO> list) {
        ErpSaleImportResultRespVO respVO = new ErpSaleImportResultRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        Map<String, ErpCustomerDO> customerMap = buildImportCustomerMap();
        Map<String, AdminUserRespDTO> saleUserMap = new LinkedHashMap<>();
        Set<String> ambiguousSaleUsers = new LinkedHashSet<>();
        for (AdminUserRespDTO user : adminUserApi.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus())) {
            String key = normalizeImportKey(user.getNickname());
            if (key == null) {
                continue;
            }
            if (saleUserMap.containsKey(key)) {
                ambiguousSaleUsers.add(key);
            } else {
                saleUserMap.put(key, user);
            }
        }
        ErpImportProductResolver productResolver = ErpImportProductResolver.build(list,
                ErpSaleOrderOrderImportExcelVO::getProductCode,
                ErpSaleOrderOrderImportExcelVO::getProductName,
                ErpSaleOrderOrderImportExcelVO::getFactoryCode, productMapper);
        Map<String, ErpWarehouseDO> warehouseMap = warehouseService.getCurrentUserVisibleSaleWarehouseList().stream()
                .collect(Collectors.toMap(item -> normalizeImportKey(item.getName()), item -> item, (a, b) -> a));

        List<SaleOrderImportGroup> groups = new ArrayList<>();
        SaleOrderImportGroup currentGroup = null;
        for (int i = 0; i < list.size(); i++) {
            ErpSaleOrderOrderImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            String decimalError = ErpSaleDecimalUtils.importScaleError(row);
            if (decimalError != null) {
                respVO.getFailureDetails().add(new ErpSaleImportResultRespVO.FailureItem(rowNo, null, row.getProductCode(), decimalError));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            if (isBlankImportRow(row)) {
                continue;
            }
            if (hasImportMainFields(row)) {
                String userKey = normalizeImportKey(row.getSaleUserName());
                AdminUserRespDTO saleUser = userKey == null ? null : saleUserMap.get(userKey);
                ErpCustomerDO customer = customerMap.get(normalizeImportKey(row.getCustomerName()));
                currentGroup = new SaleOrderImportGroup(rowNo, row, customer, saleUser);
                groups.add(currentGroup);
                String orderNo = resolveImportNo(rowNo, row);
                if (trimImportText(row.getCustomerName()) == null) {
                    addImportFailure(respVO, rowNo, orderNo, null, "客户名称不能为空");
                } else if (customer == null) {
                    addImportFailure(respVO, rowNo, orderNo, null, "客户不存在：" + row.getCustomerName());
                } else if (CommonStatusEnum.isDisable(customer.getStatus())) {
                    addImportFailure(respVO, rowNo, orderNo, null, "客户未启用：" + customer.getName());
                }
                if (!validateImportDate(respVO, rowNo, orderNo, row.getOrderTime())) {
                    // Failure has been recorded.
                }
                if (userKey != null && ambiguousSaleUsers.contains(userKey)) {
                    addImportFailure(respVO, rowNo, orderNo, null, "销售员名称不唯一：" + row.getSaleUserName());
                } else if (userKey != null && saleUser == null) {
                    addImportFailure(respVO, rowNo, orderNo, null, "销售员不存在或未启用：" + row.getSaleUserName());
                }
            } else if (hasImportDetailFields(row) && currentGroup == null) {
                addImportFailure(respVO, rowNo, null,
                        ErpImportProductResolver.getIdentifier(row.getProductCode(), row.getProductName(),
                                row.getFactoryCode()),
                        "明细行前缺少销售订单主表信息");
                continue;
            }
            if (!hasImportDetailFields(row)) {
                continue;
            }
            String orderNo = currentGroup == null ? null
                    : resolveImportNo(currentGroup.getRowNo(), currentGroup.getMainRow());
            ErpImportProductResolver.ResolveResult productResult = productResolver.resolve(
                    row.getProductCode(), row.getProductName(), row.getFactoryCode());
            String productIdentifier = productResult.getIdentifier();
            ErpProductDO product = productResult.getProduct();
            ErpWarehouseDO warehouse = trimImportText(row.getWarehouseName()) == null ? null
                    : warehouseMap.get(normalizeImportKey(row.getWarehouseName()));
            boolean valid = true;
            if (productResult.isFailure()) {
                addImportFailure(respVO, rowNo, orderNo, productIdentifier, productResult.getErrorMessage());
                valid = false;
            }
            if (row.getItemCount() == null || row.getItemCount().compareTo(BigDecimal.ZERO) <= 0) {
                addImportFailure(respVO, rowNo, orderNo, productIdentifier, "数量必须大于 0");
                valid = false;
            }
            BigDecimal productPrice = Boolean.TRUE.equals(row.getGiftFlag()) ? BigDecimal.ZERO
                    : row.getProductPrice() != null ? row.getProductPrice()
                    : product != null ? product.getSalePrice() : null;
            if (!Boolean.TRUE.equals(row.getGiftFlag())
                    && (productPrice == null || productPrice.compareTo(BigDecimal.ZERO) <= 0)) {
                addImportFailure(respVO, rowNo, orderNo, productIdentifier, "非赠品单价必须大于 0");
                valid = false;
            }
            if (trimImportText(row.getWarehouseName()) == null) {
                addImportFailure(respVO, rowNo, orderNo, productIdentifier, "所属仓库不能为空");
                valid = false;
            } else if (warehouse == null) {
                addImportFailure(respVO, rowNo, orderNo, productIdentifier,
                        "仓库不存在或无销售权限：" + row.getWarehouseName());
                valid = false;
            }
            if (valid && currentGroup != null) {
                currentGroup.getRows().add(new SaleOrderImportRow(row, product, warehouse, productPrice));
            }
        }
        for (SaleOrderImportGroup group : groups) {
            if (CollUtil.isEmpty(group.getRows())) {
                addImportFailure(respVO, group.getRowNo(), resolveImportNo(group.getRowNo(), group.getMainRow()),
                        null, "销售订单至少需要一条明细");
            }
        }
        if (respVO.getFailureCount() > 0) {
            return respVO;
        }
        for (SaleOrderImportGroup group : groups) {
            Long id = createSaleOrder(buildImportSaveReq(group));
            respVO.getDocumentIds().add(id);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    private ErpSaleOrderSaveReqVO buildImportSaveReq(SaleOrderImportGroup group) {
        ErpSaleOrderOrderImportExcelVO mainRow = group.getMainRow();
        ErpSaleOrderSaveReqVO saveReqVO = new ErpSaleOrderSaveReqVO();
        saveReqVO.setCustomerId(group.getCustomer().getId());
        saveReqVO.setOrderTime(parseImportDate(mainRow.getOrderTime(), LocalDateTime.now()));
        saveReqVO.setSaleUserId(group.getSaleUser() == null ? null : group.getSaleUser().getId());
        saveReqVO.setDiscountPercent(BigDecimal.ZERO);
        saveReqVO.setFeeAmount(BigDecimal.ZERO);
        saveReqVO.setDepositPrice(BigDecimal.ZERO);
        saveReqVO.setRemark(trimImportText(mainRow.getRemark()));
        saveReqVO.setItems(convertList(group.getRows(), this::buildImportItem));
        return saveReqVO;
    }

    private ErpSaleOrderSaveReqVO.Item buildImportItem(SaleOrderImportRow importRow) {
        ErpSaleOrderOrderImportExcelVO row = importRow.getRow();
        ErpSaleOrderSaveReqVO.Item item = new ErpSaleOrderSaveReqVO.Item();
        item.setProductId(importRow.getProduct().getId());
        item.setWarehouseId(importRow.getWarehouse().getId());
        item.setCount(row.getItemCount());
        item.setProductPrice(importRow.getProductPrice());
        item.setGiftFlag(Boolean.TRUE.equals(row.getGiftFlag()));
        item.setTaxPercent(row.getTaxPercent());
        item.setRemark(trimImportText(row.getItemRemark()));
        return item;
    }

    private Map<String, ErpCustomerDO> buildImportCustomerMap() {
        Map<String, ErpCustomerDO> map = new LinkedHashMap<>();
        cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO pageReqVO =
                new cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO();
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        customerService.getCustomerPage(pageReqVO).getList().forEach(customer -> {
            putImportCustomer(map, customer.getCode(), customer);
            putImportCustomer(map, customer.getName(), customer);
            putImportCustomer(map, customer.getShortName(), customer);
        });
        return map;
    }

    private void putImportCustomer(Map<String, ErpCustomerDO> map, String key, ErpCustomerDO customer) {
        String normalized = normalizeImportKey(key);
        if (normalized != null) {
            map.putIfAbsent(normalized, customer);
        }
    }

    private boolean isBlankImportRow(ErpSaleOrderOrderImportExcelVO row) {
        return row == null || !hasImportMainFields(row) && !hasImportDetailFields(row);
    }

    private boolean hasImportMainFields(ErpSaleOrderOrderImportExcelVO row) {
        return trimImportText(row.getImportNo()) != null
                || trimImportText(row.getCustomerName()) != null
                || trimImportText(row.getOrderTime()) != null
                || trimImportText(row.getSaleUserName()) != null
                || trimImportText(row.getRemark()) != null;
    }

    private boolean hasImportDetailFields(ErpSaleOrderOrderImportExcelVO row) {
        return trimImportText(row.getProductCode()) != null
                || trimImportText(row.getProductName()) != null
                || trimImportText(row.getFactoryCode()) != null
                || trimImportText(row.getWarehouseName()) != null
                || row.getItemCount() != null
                || row.getProductPrice() != null
                || row.getGiftFlag() != null
                || row.getTaxPercent() != null
                || trimImportText(row.getItemRemark()) != null;
    }

    private String resolveImportNo(Integer rowNo, ErpSaleOrderOrderImportExcelVO row) {
        String importNo = trimImportText(row.getImportNo());
        if (importNo != null) {
            return importNo;
        }
        String customerName = trimImportText(row.getCustomerName());
        return customerName != null ? customerName : "row " + rowNo;
    }

    private boolean validateImportDate(ErpSaleImportResultRespVO respVO, Integer rowNo,
                                       String orderNo, String value) {
        if (trimImportText(value) == null) {
            return true;
        }
        try {
            parseImportDate(value, null);
            return true;
        } catch (IllegalArgumentException ignored) {
            addImportFailure(respVO, rowNo, orderNo, null,
                    "下单时间格式不正确，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
            return false;
        }
    }

    private LocalDateTime parseImportDate(String value, LocalDateTime defaultValue) {
        String normalized = trimImportText(value);
        if (normalized == null) {
            return defaultValue;
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")}) {
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

    private void addImportFailure(ErpSaleImportResultRespVO respVO, Integer rowNo, String orderNo,
                                  String productCode, String reason) {
        respVO.getFailureDetails().add(new ErpSaleImportResultRespVO.FailureItem(
                rowNo, orderNo, productCode, reason));
        respVO.setFailureCount(respVO.getFailureCount() + 1);
    }

    private String normalizeImportKey(String value) {
        String normalized = trimImportText(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static String trimImportText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class SaleOrderImportGroup {
        private final Integer rowNo;
        private final ErpSaleOrderOrderImportExcelVO mainRow;
        private final ErpCustomerDO customer;
        private final AdminUserRespDTO saleUser;
        private final List<SaleOrderImportRow> rows = new ArrayList<>();

        private SaleOrderImportGroup(Integer rowNo, ErpSaleOrderOrderImportExcelVO mainRow,
                                     ErpCustomerDO customer, AdminUserRespDTO saleUser) {
            this.rowNo = rowNo;
            this.mainRow = mainRow;
            this.customer = customer;
            this.saleUser = saleUser;
        }

        Integer getRowNo() { return rowNo; }
        ErpSaleOrderOrderImportExcelVO getMainRow() { return mainRow; }
        ErpCustomerDO getCustomer() { return customer; }
        AdminUserRespDTO getSaleUser() { return saleUser; }
        List<SaleOrderImportRow> getRows() { return rows; }
    }

    private static final class SaleOrderImportRow {
        private final ErpSaleOrderOrderImportExcelVO row;
        private final ErpProductDO product;
        private final ErpWarehouseDO warehouse;
        private final BigDecimal productPrice;

        private SaleOrderImportRow(ErpSaleOrderOrderImportExcelVO row, ErpProductDO product,
                                   ErpWarehouseDO warehouse, BigDecimal productPrice) {
            this.row = row;
            this.product = product;
            this.warehouse = warehouse;
            this.productPrice = productPrice;
        }

        ErpSaleOrderOrderImportExcelVO getRow() { return row; }
        ErpProductDO getProduct() { return product; }
        ErpWarehouseDO getWarehouse() { return warehouse; }
        BigDecimal getProductPrice() { return productPrice; }
    }

}
