package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
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
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
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

    private static final String PURCHASE_ORDER_FORM_KEY = "erp_purchase_order";
    private static final String DEPT_SELECTION_PERMISSION_FORM_KEY = "system_dept";

    @Resource
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
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
    private ErpSupplierService supplierService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpSupplierDeptPermissionService supplierDeptPermissionService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductBatchNoValidator productBatchNoValidator;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseOrder(ErpPurchaseOrderSaveReqVO createReqVO) {
        // 1.1 校验订单项的有效�?
        List<ErpPurchaseOrderItemDO> purchaseOrderItems = validatePurchaseOrderItems(createReqVO.getItems());
        // 1.2 校验供应�?
        ErpSupplierDO supplier = supplierService.validateSupplier(createReqVO.getSupplierId());
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
        validatePurchaseOrderSupplierDept(supplier, purchaseOrder.getDeptId());
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
    public Long createPurchaseOrderDraft(ErpPurchaseOrderSaveReqVO createReqVO) {
        List<ErpPurchaseOrderItemDO> purchaseOrderItems = buildDraftPurchaseOrderItems(createReqVO.getItems());
        if (CollUtil.isEmpty(purchaseOrderItems)) {
            throw exception(PURCHASE_ORDER_SUBMIT_ITEMS_REQUIRED);
        }
        ErpSupplierDO supplier = validateOptionalDraftReferences(createReqVO);
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_ORDER_NO_PREFIX);
        if (purchaseOrderMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_ORDER_NO_EXISTS);
        }
        ErpPurchaseOrderDO purchaseOrder = BeanUtils.toBean(createReqVO, ErpPurchaseOrderDO.class, in -> in
                .setNo(no).setStatus(ErpPurchaseOrderStatusEnum.DRAFT.getStatus()));
        if (purchaseOrder.getOrderTime() == null) {
            purchaseOrder.setOrderTime(LocalDateTime.now());
        }
        purchaseDocumentDefaultService.fillCreateDefaults(purchaseOrder);
        validatePurchaseOrderSupplierDept(supplier, purchaseOrder.getDeptId());
        calculateTotalPrice(purchaseOrder, purchaseOrderItems);
        purchaseDocumentDefaultService.fillCreateAuditDefaults(purchaseOrder);
        purchaseOrderMapper.insert(purchaseOrder);
        replacePurchaseOrderItems(purchaseOrder.getId(), purchaseOrderItems);
        operateLogService.recordCreate(ERP_PURCHASE_ORDER_TYPE, purchaseOrder.getId(), purchaseOrder.getNo());
        return purchaseOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAndSubmitPurchaseOrder(ErpPurchaseOrderSaveReqVO createReqVO) {
        return createPurchaseOrder(createReqVO);
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
        ErpSupplierDO supplier = supplierService.validateSupplier(updateReqVO.getSupplierId());
        // 1.3 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        List<ErpPurchaseOrderItemDO> oldItems = purchaseOrderItemMapper.selectListByOrderId(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(PURCHASE_ORDER_FORM_KEY, updateReqVO, purchaseOrder);
        fieldPermissionMasker.preserveHiddenItemFields(PURCHASE_ORDER_FORM_KEY, updateReqVO.getItems(), oldItems);
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
        validatePurchaseOrderSupplierDept(supplier, updateObj.getDeptId());
        calculateTotalPrice(updateObj, purchaseOrderItems);
        purchaseOrderMapper.updateById(updateObj);
        // 2.2 更新订单�?
        updatePurchaseOrderItemList(updateReqVO.getId(), purchaseOrderItems);
        operateLogService.recordUpdate(ERP_PURCHASE_ORDER_TYPE, updateReqVO.getId(), purchaseOrder.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseOrderDraft(ErpPurchaseOrderSaveReqVO updateReqVO) {
        ErpPurchaseOrderDO purchaseOrder = validatePurchaseOrderExists(updateReqVO.getId());
        if (!ErpPurchaseOrderStatusEnum.DRAFT.getStatus().equals(purchaseOrder.getStatus())) {
            throw exception(PURCHASE_ORDER_UPDATE_FAIL_NOT_DRAFT, purchaseOrder.getNo());
        }
        ErpSupplierDO supplier = validateOptionalDraftReferences(updateReqVO);
        List<ErpPurchaseOrderItemDO> oldItems = purchaseOrderItemMapper.selectListByOrderId(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(PURCHASE_ORDER_FORM_KEY, updateReqVO, purchaseOrder);
        fieldPermissionMasker.preserveHiddenItemFields(PURCHASE_ORDER_FORM_KEY, updateReqVO.getItems(), oldItems);
        List<ErpPurchaseOrderItemDO> purchaseOrderItems = buildDraftPurchaseOrderItems(updateReqVO.getItems());
        ErpPurchaseOrderDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseOrderDO.class);
        updateObj.setStatus(null).setLatestOrderDate(null);
        if (updateObj.getOrderTime() == null) {
            updateObj.setOrderTime(purchaseOrder.getOrderTime());
        }
        if (updateObj.getPurchaser() == null) {
            updateObj.setPurchaser(purchaseOrder.getPurchaser());
        }
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(purchaseOrder.getDeptId());
        }
        if (supplier == null && purchaseOrder.getSupplierId() != null) {
            supplier = supplierService.validateSupplier(purchaseOrder.getSupplierId());
        }
        validatePurchaseOrderSupplierDept(supplier, updateObj.getDeptId());
        calculateTotalPrice(updateObj, purchaseOrderItems);
        int updateCount = purchaseOrderMapper.updateByIdAndStatus(
                updateReqVO.getId(), ErpPurchaseOrderStatusEnum.DRAFT.getStatus(), updateObj);
        if (updateCount == 0) {
            throw exception(PURCHASE_ORDER_UPDATE_FAIL_NOT_DRAFT, purchaseOrder.getNo());
        }
        replacePurchaseOrderItems(updateReqVO.getId(), purchaseOrderItems);
        operateLogService.recordUpdate(ERP_PURCHASE_ORDER_TYPE, updateReqVO.getId(), purchaseOrder.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitPurchaseOrder(ErpPurchaseOrderSaveReqVO updateReqVO) {
        ErpPurchaseOrderDO purchaseOrder = validatePurchaseOrderExists(updateReqVO.getId());
        if (!ErpPurchaseOrderStatusEnum.DRAFT.getStatus().equals(purchaseOrder.getStatus())) {
            throw exception(PURCHASE_ORDER_UPDATE_FAIL_NOT_DRAFT, purchaseOrder.getNo());
        }
        updatePurchaseOrder(updateReqVO);
        submitPurchaseOrderDraft(updateReqVO.getId());
    }

    private ErpSupplierDO validateOptionalDraftReferences(ErpPurchaseOrderSaveReqVO reqVO) {
        ErpSupplierDO supplier = null;
        if (reqVO.getSupplierId() != null) {
            supplier = supplierService.validateSupplier(reqVO.getSupplierId());
        }
        if (reqVO.getAccountId() != null) {
            accountService.validateAccount(reqVO.getAccountId());
        }
        return supplier;
    }

    private List<ErpPurchaseOrderItemDO> buildDraftPurchaseOrderItems(
            List<ErpPurchaseOrderSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        List<ErpPurchaseOrderSaveReqVO.Item> validItems = items.stream()
                .filter(item -> item.getProductId() != null && item.getWarehouseId() != null
                        && item.getCount() != null && item.getCount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        if (validItems.isEmpty()) {
            return Collections.emptyList();
        }
        return validateDraftPurchaseOrderItems(validItems);
    }

    private List<ErpPurchaseOrderItemDO> validateDraftPurchaseOrderItems(List<ErpPurchaseOrderSaveReqVO.Item> list) {
        Set<String> itemKeySet = new LinkedHashSet<>();
        for (ErpPurchaseOrderSaveReqVO.Item item : list) {
            String productKey = StrUtil.blankToDefault(item.getProductCode(), String.valueOf(item.getProductId()));
            String itemKey = item.getProductId() + "|" + item.getWarehouseId() + "|"
                    + (Boolean.TRUE.equals(item.getGift()) ? 1 : 0);
            if (!itemKeySet.add(itemKey)) {
                throw exception(PURCHASE_ORDER_ITEM_DUPLICATE, productKey);
            }
        }
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() -> productService.validProductList(
                convertSet(list, ErpPurchaseOrderSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoRequired(list, productMap,
                ErpPurchaseOrderSaveReqVO.Item::getProductId, ErpPurchaseOrderSaveReqVO.Item::getBatchNo);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpPurchaseOrderSaveReqVO.Item::getProductId, ErpPurchaseOrderSaveReqVO.Item::getBatchNo);
        Set<Long> warehouseIds = convertSet(list, ErpPurchaseOrderSaveReqVO.Item::getWarehouseId);
        warehouseService.validPurchaseWarehouseList(warehouseIds);
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);
        return convertList(list, o -> BeanUtils.toBean(o, ErpPurchaseOrderItemDO.class, item -> {
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            item.setDeptId(item.getDeptId() == null && warehouse != null ? warehouse.getDeptId() : item.getDeptId());
            item.setTaxPercent(null);
            item.setTaxPrice(BigDecimal.ZERO);
            BigDecimal productPrice = item.getProductPrice();
            if (productPrice == null || productPrice.compareTo(BigDecimal.ZERO) < 0 || Boolean.TRUE.equals(item.getGift())) {
                productPrice = BigDecimal.ZERO;
            }
            item.setProductPrice(productPrice);
            item.setTotalPrice(MoneyUtils.priceMultiply(productPrice, item.getCount()));
        }));
    }

    private void replacePurchaseOrderItems(Long orderId, List<ErpPurchaseOrderItemDO> items) {
        purchaseOrderItemMapper.deleteByOrderId(orderId);
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> item.setId(null).setOrderId(orderId));
        purchaseDocumentDefaultService.fillCreateAuditDefaults(items);
        purchaseOrderItemMapper.insertBatch(items);
    }

    @Override
    public void updatePurchaseOrderRemark(ErpPurchaseOrderUpdateRemarkReqVO updateReqVO) {
        ErpPurchaseOrderDO purchaseOrder = validatePurchaseOrderExists(updateReqVO.getId());
        purchaseOrderMapper.updateById(new ErpPurchaseOrderDO()
                .setId(updateReqVO.getId())
                .setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_PURCHASE_ORDER_TYPE, updateReqVO.getId(), purchaseOrder.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdatePurchaseOrderItems(ErpPurchaseOrderItemBatchUpdateReqVO updateReqVO) {
        ErpPurchaseOrderDO purchaseOrder = validatePurchaseOrderExists(updateReqVO.getOrderId());
        if (updateReqVO.getWarehouseId() == null && updateReqVO.getDeptId() == null) {
            throw exception(PURCHASE_ORDER_ITEM_BATCH_UPDATE_FIELD_REQUIRED);
        }

        List<ErpPurchaseOrderItemDO> orderItems = purchaseOrderItemMapper.selectListByOrderId(updateReqVO.getOrderId());
        Map<Long, ErpPurchaseOrderItemDO> orderItemMap = convertMap(orderItems, ErpPurchaseOrderItemDO::getId);
        List<ErpPurchaseOrderItemDO> selectedItems = updateReqVO.getItemIds().stream()
                .map(orderItemMap::get)
                .collect(Collectors.toList());
        if (selectedItems.stream().anyMatch(Objects::isNull)) {
            throw exception(PURCHASE_ORDER_ITEM_BATCH_UPDATE_ITEM_NOT_EXISTS);
        }
        validateBatchUpdateItemsNoDownstream(updateReqVO.getItemIds(), selectedItems);

        ErpWarehouseDO targetWarehouse = validateBatchUpdateTargetWarehouse(updateReqVO.getWarehouseId());
        Long targetDeptId = resolveBatchUpdateTargetDeptId(targetWarehouse, updateReqVO.getDeptId());
        Map<Long, ErpWarehouseDO> finalWarehouseMap = buildBatchUpdateFinalWarehouseMap(selectedItems, targetWarehouse);
        validateBatchUpdateWarehouseDept(selectedItems, targetWarehouse, targetDeptId, finalWarehouseMap);
        validateBatchUpdateNoDuplicate(orderItems, updateReqVO.getItemIds(), targetWarehouse);

        Set<String> ensuredStockKeys = new LinkedHashSet<>();
        for (ErpPurchaseOrderItemDO selectedItem : selectedItems) {
            Long finalWarehouseId = targetWarehouse == null ? selectedItem.getWarehouseId() : targetWarehouse.getId();
            Long finalDeptId = targetDeptId == null ? selectedItem.getDeptId() : targetDeptId;
            if (targetWarehouse != null) {
                String stockKey = selectedItem.getProductId() + "_" + finalWarehouseId;
                if (ensuredStockKeys.add(stockKey)) {
                    stockService.ensureStockExists(selectedItem.getProductId(), finalWarehouseId);
                }
            }
            purchaseOrderItemMapper.updateById(new ErpPurchaseOrderItemDO()
                    .setId(selectedItem.getId())
                    .setWarehouseId(finalWarehouseId)
                    .setDeptId(finalDeptId));
        }
        operateLogService.recordUpdate(ERP_PURCHASE_ORDER_TYPE, updateReqVO.getOrderId(), purchaseOrder.getNo());
    }

    private void validateBatchUpdateItemsNoDownstream(List<Long> itemIds, List<ErpPurchaseOrderItemDO> selectedItems) {
        for (ErpPurchaseOrderItemDO selectedItem : selectedItems) {
            if (nullToZero(selectedItem.getInCount()).compareTo(BigDecimal.ZERO) > 0) {
                throw exception(PURCHASE_ORDER_ITEM_BATCH_UPDATE_FAIL_HAS_IN);
            }
            if (nullToZero(selectedItem.getReturnCount()).compareTo(BigDecimal.ZERO) > 0) {
                throw exception(PURCHASE_ORDER_ITEM_BATCH_UPDATE_FAIL_HAS_RETURN);
            }
        }
        Long purchaseInItemCount = purchaseInItemMapper.selectCountByOrderItemIds(itemIds);
        if (purchaseInItemCount != null && purchaseInItemCount > 0) {
            throw exception(PURCHASE_ORDER_ITEM_BATCH_UPDATE_FAIL_HAS_IN);
        }
        Long purchaseReturnItemCount = purchaseReturnItemMapper.selectCountByOrderItemIds(itemIds);
        if (purchaseReturnItemCount != null && purchaseReturnItemCount > 0) {
            throw exception(PURCHASE_ORDER_ITEM_BATCH_UPDATE_FAIL_HAS_RETURN);
        }
    }

    private ErpWarehouseDO validateBatchUpdateTargetWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        List<ErpWarehouseDO> warehouses = warehouseService.validPurchaseWarehouseList(Collections.singleton(warehouseId));
        return CollUtil.getFirst(warehouses);
    }

    private Long resolveBatchUpdateTargetDeptId(ErpWarehouseDO targetWarehouse, Long deptId) {
        if (targetWarehouse == null) {
            return deptId;
        }
        Set<Long> allowedDeptIds = getBatchUpdateWarehouseAllowedDeptIds(targetWarehouse);
        if (deptId != null) {
            if (!allowedDeptIds.contains(deptId)) {
                throw exception(PURCHASE_ORDER_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
            }
            return deptId;
        }
        if (allowedDeptIds.size() == 1) {
            return CollUtil.getFirst(allowedDeptIds);
        }
        throw exception(PURCHASE_ORDER_ITEM_BATCH_UPDATE_DEPT_REQUIRED);
    }

    private Map<Long, ErpWarehouseDO> buildBatchUpdateFinalWarehouseMap(List<ErpPurchaseOrderItemDO> selectedItems,
                                                                       ErpWarehouseDO targetWarehouse) {
        if (targetWarehouse != null) {
            return Collections.singletonMap(targetWarehouse.getId(), targetWarehouse);
        }
        Set<Long> warehouseIds = selectedItems.stream()
                .map(ErpPurchaseOrderItemDO::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (selectedItems.stream().anyMatch(item -> item.getWarehouseId() == null)) {
            throw exception(PURCHASE_ORDER_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
        }
        List<ErpWarehouseDO> warehouses = warehouseService.validPurchaseWarehouseList(warehouseIds);
        return convertMap(warehouses, ErpWarehouseDO::getId);
    }

    private void validateBatchUpdateWarehouseDept(List<ErpPurchaseOrderItemDO> selectedItems,
                                                  ErpWarehouseDO targetWarehouse,
                                                  Long targetDeptId,
                                                  Map<Long, ErpWarehouseDO> finalWarehouseMap) {
        Map<Long, Set<Long>> allowedDeptCache = new LinkedHashMap<>();
        for (ErpPurchaseOrderItemDO selectedItem : selectedItems) {
            Long finalWarehouseId = targetWarehouse == null ? selectedItem.getWarehouseId() : targetWarehouse.getId();
            Long finalDeptId = targetDeptId == null ? selectedItem.getDeptId() : targetDeptId;
            if (finalDeptId == null) {
                throw exception(PURCHASE_ORDER_ITEM_BATCH_UPDATE_DEPT_REQUIRED);
            }
            ErpWarehouseDO finalWarehouse = finalWarehouseMap.get(finalWarehouseId);
            if (finalWarehouse == null) {
                throw exception(PURCHASE_ORDER_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
            }
            Set<Long> allowedDeptIds = allowedDeptCache.computeIfAbsent(finalWarehouseId,
                    key -> getBatchUpdateWarehouseAllowedDeptIds(finalWarehouse));
            if (!allowedDeptIds.contains(finalDeptId)) {
                throw exception(PURCHASE_ORDER_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
            }
        }
    }

    private Set<Long> getBatchUpdateWarehouseAllowedDeptIds(ErpWarehouseDO warehouse) {
        Set<Long> deptIds = new LinkedHashSet<>();
        if (warehouse.getDeptId() != null) {
            deptIds.add(warehouse.getDeptId());
        }
        return deptIds;
    }

    private void validateBatchUpdateNoDuplicate(List<ErpPurchaseOrderItemDO> orderItems,
                                                List<Long> selectedItemIds,
                                                ErpWarehouseDO targetWarehouse) {
        Set<Long> selectedItemIdSet = new LinkedHashSet<>(selectedItemIds);
        Set<String> itemKeySet = new LinkedHashSet<>();
        for (ErpPurchaseOrderItemDO item : orderItems) {
            boolean selected = selectedItemIdSet.contains(item.getId());
            Long finalWarehouseId = selected && targetWarehouse != null ? targetWarehouse.getId() : item.getWarehouseId();
            String itemKey = item.getProductId() + "|" + finalWarehouseId + "|"
                    + (Boolean.TRUE.equals(item.getGift()) ? 1 : 0);
            if (!itemKeySet.add(itemKey)) {
                throw exception(PURCHASE_ORDER_ITEM_DUPLICATE, String.valueOf(item.getProductId()));
            }
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
        purchaseOrder.setTotalCount(getSumValue(purchaseOrderItems, ErpPurchaseOrderItemDO::getCount,
                BigDecimal::add, BigDecimal.ZERO));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPurchaseOrderDraft(Long id) {
        ErpPurchaseOrderDO purchaseOrder = validatePurchaseOrderExists(id);
        if (!ErpPurchaseOrderStatusEnum.DRAFT.getStatus().equals(purchaseOrder.getStatus())) {
            throw exception(PURCHASE_ORDER_SUBMIT_FAIL);
        }
        validatePurchaseOrderForSubmit(purchaseOrder);

        int updateCount = purchaseOrderMapper.updateByIdAndStatus(id,
                ErpPurchaseOrderStatusEnum.DRAFT.getStatus(),
                new ErpPurchaseOrderDO().setStatus(ErpPurchaseOrderStatusEnum.PROCESS.getStatus()));
        if (updateCount == 0) {
            throw exception(PURCHASE_ORDER_SUBMIT_FAIL);
        }
        operateLogService.recordUpdate(ERP_PURCHASE_ORDER_TYPE, id, purchaseOrder.getNo());
    }

    private void orderPurchaseOrder(Long id, ErpPurchaseOrderDO purchaseOrder) {
        if (!ErpAuditStatus.PROCESS.getStatus().equals(purchaseOrder.getStatus())) {
            throw exception(PURCHASE_ORDER_APPROVE_FAIL);
        }
        validatePurchaseOrderForSubmit(purchaseOrder);

        int updateCount = purchaseOrderMapper.updateByIdAndStatus(id, purchaseOrder.getStatus(),
                new ErpPurchaseOrderDO().setStatus(ErpAuditStatus.APPROVE.getStatus()));
        if (updateCount == 0) {
            throw exception(PURCHASE_ORDER_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_PURCHASE_ORDER_TYPE, "下订", id, purchaseOrder.getNo());
    }

    private void validatePurchaseOrderForSubmit(ErpPurchaseOrderDO purchaseOrder) {
        if (purchaseOrder.getSupplierId() == null) {
            throw exception(PURCHASE_ORDER_SUBMIT_SUPPLIER_REQUIRED);
        }
        if (purchaseOrder.getOrderTime() == null) {
            throw exception(PURCHASE_ORDER_SUBMIT_TIME_REQUIRED);
        }
        ErpSupplierDO supplier = supplierService.validateSupplier(purchaseOrder.getSupplierId());
        validatePurchaseOrderSupplierDept(supplier, purchaseOrder.getDeptId());
        if (purchaseOrder.getAccountId() != null) {
            accountService.validateAccount(purchaseOrder.getAccountId());
        }
        List<ErpPurchaseOrderItemDO> persistedItems =
                purchaseOrderItemMapper.selectListByOrderId(purchaseOrder.getId());
        if (CollUtil.isEmpty(persistedItems)) {
            throw exception(PURCHASE_ORDER_SUBMIT_ITEMS_REQUIRED);
        }
        List<ErpPurchaseOrderSaveReqVO.Item> submitItems =
                BeanUtils.toBean(persistedItems, ErpPurchaseOrderSaveReqVO.Item.class);
        validatePurchaseOrderItems(submitItems);
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
        if (CollUtil.isEmpty(list)) {
            throw exception(PURCHASE_ORDER_SUBMIT_ITEMS_REQUIRED);
        }
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
        productBatchNoValidator.validateBatchNoRequired(list, productMap,
                ErpPurchaseOrderSaveReqVO.Item::getProductId, ErpPurchaseOrderSaveReqVO.Item::getBatchNo);
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
    public List<DeptSimpleRespVO> getSupplierAvailableDeptSimpleList(Long supplierId) {
        return supplierDeptPermissionService.getAvailableDeptSimpleList(supplierId, DEPT_SELECTION_PERMISSION_FORM_KEY);
    }

    @Override
    public List<DeptSimpleRespVO> getWarehouseAvailableDeptSimpleList(Long warehouseId) {
        ErpWarehouseDO warehouse = CollUtil.getFirst(warehouseService.validPurchaseWarehouseList(
                Collections.singleton(warehouseId)));
        if (warehouse == null) {
            return Collections.emptyList();
        }
        return buildBatchUpdateAvailableDeptSimpleList(getBatchUpdateWarehouseAllowedDeptIds(warehouse));
    }

    private List<DeptSimpleRespVO> buildBatchUpdateAvailableDeptSimpleList(Set<Long> allowedDeptIds) {
        if (CollUtil.isEmpty(allowedDeptIds)) {
            return Collections.emptyList();
        }
        Set<Long> availableDeptIds = new LinkedHashSet<>(allowedDeptIds);
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(getLoginUserId(),
                DEPT_SELECTION_PERMISSION_FORM_KEY);
        if (!Boolean.TRUE.equals(permission != null ? permission.getAll() : null)) {
            Set<Long> permissionDeptIds = permission != null ? permission.getDeptIds() : Collections.emptySet();
            if (CollUtil.isEmpty(permissionDeptIds)) {
                return Collections.emptyList();
            }
            availableDeptIds.retainAll(permissionDeptIds);
        }
        if (CollUtil.isEmpty(availableDeptIds)) {
            return Collections.emptyList();
        }
        Map<Long, DeptRespDTO> deptMap = convertMap(deptApi.getDeptList(availableDeptIds), DeptRespDTO::getId);
        return availableDeptIds.stream()
                .map(deptMap::get)
                .filter(dept -> dept != null && CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus()))
                .map(dept -> new DeptSimpleRespVO(dept.getId(), dept.getName(), dept.getParentId()))
                .collect(Collectors.toList());
    }

    private void validatePurchaseOrderSupplierDept(ErpSupplierDO supplier, Long deptId) {
        if (!supplierDeptPermissionService.hasAvailableDept(supplier, deptId, DEPT_SELECTION_PERMISSION_FORM_KEY)) {
            throw exception(PURCHASE_ORDER_SUPPLIER_DEPT_NOT_ALLOWED);
        }
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
            String batchNo = normalize(row.getBatchNo());
            if (Boolean.TRUE.equals(product.getBatchNoEnabled()) && StrUtil.isBlank(batchNo)) {
                respVO.getFailureDetails().add(new ErpPurchaseOrderImportRespVO.FailureItem(rowNo, row.getProductCode(),
                        "该配件已开启批次号管理，请填写批次号"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            if (!Boolean.TRUE.equals(product.getBatchNoEnabled()) && StrUtil.isNotBlank(batchNo)) {
                respVO.getFailureDetails().add(new ErpPurchaseOrderImportRespVO.FailureItem(rowNo, row.getProductCode(),
                        "该配件未开启批次号管理，不能填写批次号"));
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
            item.setBatchNo(batchNo);

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
        Map<String, ErpWarehouseDO> warehouseMap = buildOrderImportWarehouseMap();
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
            String warehouseName = normalize(row.getWarehouseName());
            ErpWarehouseDO warehouse = resolveOrderImportWarehouse(row, warehouseMap);
            String batchNo = normalize(row.getBatchNo());
            if (product != null && Boolean.TRUE.equals(product.getBatchNoEnabled()) && StrUtil.isBlank(batchNo)) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "该配件已开启批次号管理，请填写批次号");
                valid = false;
            }
            if (product != null && !Boolean.TRUE.equals(product.getBatchNoEnabled()) && StrUtil.isNotBlank(batchNo)) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "该配件未开启批次号管理，不能填写批次号");
                valid = false;
            }
            if (StrUtil.isBlank(productCode)) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "Product code is required");
                valid = false;
            } else if (product == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "Product not found");
                valid = false;
            }
            if (StrUtil.isNotBlank(warehouseName) && warehouse == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "所属仓库不存在或不可用：" + warehouseName);
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
            currentGroup.getRows().add(new OrderImportRow(rowNo, row, product, warehouse));
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
        saveReqVO.setOrderTime(LocalDateTime.now());
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
        item.setWarehouseId(row.getWarehouse() == null ? product.getDefaultWarehouseId() : row.getWarehouse().getId());
        boolean gift = parseGiftFlag(importRow.getGift());
        item.setProductPrice(resolveImportProductPrice(importRow.getProductPrice(), product, gift));
        item.setCount(importRow.getItemCount());
        item.setBatchNo(normalize(importRow.getBatchNo()));

        item.setRemark(importRow.getItemRemark());
        item.setGift(gift);
        return item;
    }

    private Map<String, ErpWarehouseDO> buildOrderImportWarehouseMap() {
        Map<String, ErpWarehouseDO> map = new LinkedHashMap<>();
        warehouseService.getCurrentUserAuthorizedPurchaseWarehouseList()
                .forEach(warehouse -> {
                    putIfNotBlank(map, warehouse.getName(), warehouse);
                    putIfNotBlank(map, warehouse.getId() == null ? null : warehouse.getId().toString(), warehouse);
                });
        return map;
    }

    private ErpWarehouseDO resolveOrderImportWarehouse(ErpPurchaseOrderImportExcelVO row,
                                                       Map<String, ErpWarehouseDO> warehouseMap) {
        return warehouseMap.get(normalizeKey(row.getWarehouseName()));
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
        return StrUtil.isNotBlank(normalize(row.getFactoryOrderNo()))
                || StrUtil.isNotBlank(normalize(row.getSupplierName()))
                || StrUtil.isNotBlank(normalize(row.getRemark()));
    }

    private boolean hasOrderDetailFields(ErpPurchaseOrderImportExcelVO row) {
        return StrUtil.isNotBlank(normalize(row.getProductCode()))
                || StrUtil.isNotBlank(normalize(row.getWarehouseName()))
                || row.getItemCount() != null
                || row.getProductPrice() != null
                || StrUtil.isNotBlank(normalize(row.getBatchNo()))
                || StrUtil.isNotBlank(normalize(row.getGift()))
                || StrUtil.isNotBlank(normalize(row.getItemRemark()));
    }

    private String resolveImportOrderNo(Integer rowNo, ErpPurchaseOrderImportExcelVO row) {
        if (StrUtil.isNotBlank(normalize(row.getFactoryOrderNo()))) {
            return normalize(row.getFactoryOrderNo());
        }
        return "row " + rowNo;
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
        private final ErpWarehouseDO warehouse;

        private OrderImportRow(Integer rowNo, ErpPurchaseOrderImportExcelVO row, ErpProductDO product,
                               ErpWarehouseDO warehouse) {
            this.rowNo = rowNo;
            this.row = row;
            this.product = product;
            this.warehouse = warehouse;
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

        public ErpWarehouseDO getWarehouse() {
            return warehouse;
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
            vo.setDeptId(item.getDeptId());
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

