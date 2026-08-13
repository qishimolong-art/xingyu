package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.ErpPurchaseUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseReturnModeEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseReturnStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.springframework.context.annotation.Lazy;
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
import java.util.HashMap;
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
 * ERP 采购退�?Service 实现�?
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpPurchaseReturnServiceImpl implements ErpPurchaseReturnService {

    private static final String FIELD_PERMISSION_MODULE = "erp_purchase_return";
    private static final String DEPT_PERMISSION_FORM_KEY = "erp_purchase_return";

    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpFinancePaymentItemMapper financePaymentItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    @Lazy // 延迟加载，避免循环依�?
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
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Resource
    private ErpProductBatchNoValidator productBatchNoValidator;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpStockService stockService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseReturn(ErpPurchaseReturnSaveReqVO createReqVO) {
        // 1.1 校验退货模�?
        Integer returnMode = createReqVO.getReturnMode();
        if (returnMode == null || (!ErpPurchaseReturnModeEnum.isByOrder(returnMode) && !ErpPurchaseReturnModeEnum.isByStock(returnMode))) {
            throw exception(PURCHASE_RETURN_MODE_INVALID);
        }
        // 1.2 校验采购订单已审核（orderId 可空�?
        ErpPurchaseOrderDO purchaseOrder = null;
        if (createReqVO.getOrderId() != null) {
            purchaseOrder = purchaseOrderService.validatePurchaseOrder(createReqVO.getOrderId());
        }
        // 1.3 按入库单退货模式下，校�?sourceInItem 可退数量
        if (ErpPurchaseReturnModeEnum.isByOrder(returnMode)) {
            validateReturnableCountForByOrderIncludingProcessing(createReqVO.getItems(), null);
        }
        // 1.4 校验退货项的有效�?
        List<ErpPurchaseReturnItemDO> purchaseReturnItems = validatePurchaseReturnItems(createReqVO.getItems());
        // 1.5 生成退货单号，并校验唯一�?
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_RETURN_NO_PREFIX);
        if (purchaseReturnMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_RETURN_NO_EXISTS);
        }

        // 2.1 插入退�?
        ErpPurchaseReturnDO purchaseReturn = BeanUtils.toBean(createReqVO, ErpPurchaseReturnDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        purchaseReturn.setReturnTime(createReqVO.getReturnTime() != null ? createReqVO.getReturnTime() : LocalDateTime.now());
        if (purchaseOrder != null) {
            purchaseReturn.setOrderNo(purchaseOrder.getNo()).setSupplierId(purchaseOrder.getSupplierId())
                    .setDeptId(purchaseOrder.getDeptId());
        }
        purchaseDocumentDefaultService.fillCreateDefaults(purchaseReturn);
        // Supplier is required.
        if (purchaseReturn.getSupplierId() == null) {
            throw exception(PURCHASE_RETURN_SUPPLIER_REQUIRED);
        }
        calculateTotalPrice(purchaseReturn, purchaseReturnItems);
        purchaseDocumentDefaultService.fillCreateAuditDefaults(purchaseReturn);
        purchaseReturnMapper.insert(purchaseReturn);
        // 2.2 插入退货项
        purchaseReturnItems.forEach(o -> o.setReturnId(purchaseReturn.getId()));
        purchaseDocumentDefaultService.fillCreateAuditDefaults(purchaseReturnItems);
        purchaseReturnItemMapper.insertBatch(purchaseReturnItems);

        // 3. 更新采购订单的退货数�?
        if (createReqVO.getOrderId() != null) {
            updatePurchaseOrderReturnCount(createReqVO.getOrderId());
        }
        operateLogService.recordCreate(ERP_PURCHASE_RETURN_TYPE, purchaseReturn.getId(), purchaseReturn.getNo());
        return purchaseReturn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseReturnDraft(ErpPurchaseReturnDraftCreateReqVO createReqVO) {
        Integer returnMode = normalizeDraftReturnMode(createReqVO);
        List<ErpPurchaseReturnSaveReqVO.Item> itemReqs = filterDraftItems(createReqVO.getItems(), returnMode);
        if (CollUtil.isEmpty(itemReqs)) {
            throw exception(PURCHASE_RETURN_SUBMIT_ITEMS_REQUIRED);
        }
        validateOptionalDraftReferences(createReqVO);
        if (ErpPurchaseReturnModeEnum.isByOrder(returnMode)) {
            validateReturnableCountForByOrderIncludingProcessing(itemReqs, null);
        }
        List<ErpPurchaseReturnItemDO> items = CollUtil.isEmpty(itemReqs)
                ? Collections.emptyList() : validateDraftPurchaseReturnItems(itemReqs);

        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_RETURN_NO_PREFIX);
        if (purchaseReturnMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_RETURN_NO_EXISTS);
        }
        ErpPurchaseReturnDO purchaseReturn = BeanUtils.toBean(createReqVO, ErpPurchaseReturnDO.class, target -> target
                .setNo(no).setStatus(ErpPurchaseReturnStatusEnum.DRAFT.getStatus()).setReturnMode(returnMode));
        purchaseReturn.setReturnTime(createReqVO.getReturnTime() != null
                ? createReqVO.getReturnTime() : LocalDateTime.now());
        fillDraftSource(createReqVO, purchaseReturn);
        purchaseDocumentDefaultService.fillCreateDefaults(purchaseReturn);
        calculateTotalPrice(purchaseReturn, items);
        purchaseDocumentDefaultService.fillCreateAuditDefaults(purchaseReturn);
        purchaseReturnMapper.insert(purchaseReturn);
        replacePurchaseReturnItems(purchaseReturn.getId(), items);
        operateLogService.recordCreate(ERP_PURCHASE_RETURN_TYPE, purchaseReturn.getId(), purchaseReturn.getNo());
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
        List<ErpPurchaseReturnItemDO> oldItems = purchaseReturnItemMapper.selectListByReturnId(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, purchaseReturn);
        fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldItems);
        // 1.2 校验退货模�?
        Integer returnMode = updateReqVO.getReturnMode();
        if (returnMode == null || (!ErpPurchaseReturnModeEnum.isByOrder(returnMode) && !ErpPurchaseReturnModeEnum.isByStock(returnMode))) {
            throw exception(PURCHASE_RETURN_MODE_INVALID);
        }
        // 1.3 校验采购订单已审核（orderId 可空�?
        ErpPurchaseOrderDO purchaseOrder = null;
        if (updateReqVO.getOrderId() != null) {
            purchaseOrder = purchaseOrderService.validatePurchaseOrder(updateReqVO.getOrderId());
        }
        // 1.4 按入库单退货模式下，校�?sourceInItem 可退数量（排除当前退货单自己�?
        if (ErpPurchaseReturnModeEnum.isByOrder(returnMode)) {
            validateReturnableCountForByOrderIncludingProcessing(updateReqVO.getItems(), updateReqVO.getId());
        }
        // 1.5 校验订单项的有效�?
        List<ErpPurchaseReturnItemDO> purchaseReturnItems = validatePurchaseReturnItems(updateReqVO.getItems());

        // 2.1 更新退�?
        ErpPurchaseReturnDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseReturnDO.class);
        updateObj.setReturnTime(updateReqVO.getReturnTime() != null ? updateReqVO.getReturnTime() : LocalDateTime.now());
        if (purchaseOrder != null) {
            updateObj.setOrderNo(purchaseOrder.getNo()).setSupplierId(purchaseOrder.getSupplierId())
                    .setDeptId(purchaseOrder.getDeptId());
        }
        if (updateObj.getHandler() == null) {
            updateObj.setHandler(purchaseReturn.getHandler());
        }
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(purchaseReturn.getDeptId());
        }
        purchaseDocumentDefaultService.fillCreateDefaults(updateObj);
        // 兜底校验：supplierId 必填
        if (updateObj.getSupplierId() == null) {
            throw exception(PURCHASE_RETURN_SUPPLIER_REQUIRED);
        }
        calculateTotalPrice(updateObj, purchaseReturnItems);
        purchaseReturnMapper.updateById(updateObj);
        // 2.2 更新退货项
        updatePurchaseReturnItemList(updateReqVO.getId(), purchaseReturnItems);

        // 3.1 更新采购订单的出库数�?
        if (updateObj.getOrderId() != null) {
            updatePurchaseOrderReturnCount(updateObj.getOrderId());
        }
        // 3.2 注意：如果采购订单编号变更了，需要更新“老”采购订单的出库数量
        if (ObjectUtil.notEqual(purchaseReturn.getOrderId(), updateObj.getOrderId())
                && purchaseReturn.getOrderId() != null) {
            updatePurchaseOrderReturnCount(purchaseReturn.getOrderId());
        }
        operateLogService.recordUpdate(ERP_PURCHASE_RETURN_TYPE, updateReqVO.getId(), purchaseReturn.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdatePurchaseReturnItems(ErpPurchaseReturnItemBatchUpdateReqVO updateReqVO) {
        ErpPurchaseReturnDO purchaseReturn = validatePurchaseReturnExists(updateReqVO.getReturnId());
        if (updateReqVO.getWarehouseId() == null && updateReqVO.getDeptId() == null) {
            throw exception(PURCHASE_RETURN_ITEM_BATCH_UPDATE_FIELD_REQUIRED);
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseReturn.getStatus())) {
            throw exception(PURCHASE_RETURN_ITEM_BATCH_UPDATE_FAIL_APPROVE, purchaseReturn.getNo());
        }
        if (ErpPurchaseReturnModeEnum.isByOrder(purchaseReturn.getReturnMode())
                && updateReqVO.getWarehouseId() != null) {
            throw exception(PURCHASE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_NOT_ALLOWED_BY_ORDER);
        }

        List<ErpPurchaseReturnItemDO> returnItems = purchaseReturnItemMapper.selectListByReturnId(updateReqVO.getReturnId());
        Map<Long, ErpPurchaseReturnItemDO> returnItemMap = convertMap(returnItems, ErpPurchaseReturnItemDO::getId);
        List<ErpPurchaseReturnItemDO> selectedItems = updateReqVO.getItemIds().stream()
                .map(returnItemMap::get)
                .collect(Collectors.toList());
        if (selectedItems.stream().anyMatch(Objects::isNull)) {
            throw exception(PURCHASE_RETURN_ITEM_BATCH_UPDATE_ITEM_NOT_EXISTS);
        }

        ErpWarehouseDO targetWarehouse = validateBatchUpdateTargetWarehouse(updateReqVO.getWarehouseId());
        Long targetDeptId = resolveBatchUpdateTargetDeptId(targetWarehouse, updateReqVO.getDeptId());
        Map<Long, ErpWarehouseDO> finalWarehouseMap = buildBatchUpdateFinalWarehouseMap(selectedItems, targetWarehouse);
        validateBatchUpdateWarehouseDept(selectedItems, targetWarehouse, targetDeptId, finalWarehouseMap);
        validateBatchUpdateNoDuplicate(returnItems, updateReqVO.getItemIds(), targetWarehouse);

        Set<String> ensuredStockKeys = new LinkedHashSet<>();
        for (ErpPurchaseReturnItemDO selectedItem : selectedItems) {
            Long finalWarehouseId = targetWarehouse == null ? selectedItem.getWarehouseId() : targetWarehouse.getId();
            Long finalDeptId = targetDeptId == null ? selectedItem.getDeptId() : targetDeptId;
            if (targetWarehouse != null) {
                String stockKey = selectedItem.getProductId() + "_" + finalWarehouseId;
                if (ensuredStockKeys.add(stockKey)) {
                    stockService.ensureStockExists(selectedItem.getProductId(), finalWarehouseId);
                }
            }
            purchaseReturnItemMapper.updateById(new ErpPurchaseReturnItemDO()
                    .setId(selectedItem.getId())
                    .setWarehouseId(finalWarehouseId)
                    .setDeptId(finalDeptId));
        }
        operateLogService.recordUpdate(ERP_PURCHASE_RETURN_TYPE, updateReqVO.getReturnId(), purchaseReturn.getNo());
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
                throw exception(PURCHASE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
            }
            return deptId;
        }
        if (allowedDeptIds.size() == 1) {
            return CollUtil.getFirst(allowedDeptIds);
        }
        throw exception(PURCHASE_RETURN_ITEM_BATCH_UPDATE_DEPT_REQUIRED);
    }

    private Map<Long, ErpWarehouseDO> buildBatchUpdateFinalWarehouseMap(List<ErpPurchaseReturnItemDO> selectedItems,
                                                                       ErpWarehouseDO targetWarehouse) {
        if (targetWarehouse != null) {
            return Collections.singletonMap(targetWarehouse.getId(), targetWarehouse);
        }
        Set<Long> warehouseIds = selectedItems.stream()
                .map(ErpPurchaseReturnItemDO::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (selectedItems.stream().anyMatch(item -> item.getWarehouseId() == null)) {
            throw exception(PURCHASE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
        }
        List<ErpWarehouseDO> warehouses = warehouseService.validPurchaseWarehouseList(warehouseIds);
        return convertMap(warehouses, ErpWarehouseDO::getId);
    }

    private void validateBatchUpdateWarehouseDept(List<ErpPurchaseReturnItemDO> selectedItems,
                                                  ErpWarehouseDO targetWarehouse,
                                                  Long targetDeptId,
                                                  Map<Long, ErpWarehouseDO> finalWarehouseMap) {
        Map<Long, Set<Long>> allowedDeptCache = new LinkedHashMap<>();
        for (ErpPurchaseReturnItemDO selectedItem : selectedItems) {
            Long finalWarehouseId = targetWarehouse == null ? selectedItem.getWarehouseId() : targetWarehouse.getId();
            Long finalDeptId = targetDeptId == null ? selectedItem.getDeptId() : targetDeptId;
            if (finalDeptId == null) {
                throw exception(PURCHASE_RETURN_ITEM_BATCH_UPDATE_DEPT_REQUIRED);
            }
            ErpWarehouseDO finalWarehouse = finalWarehouseMap.get(finalWarehouseId);
            if (finalWarehouse == null) {
                throw exception(PURCHASE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
            }
            Set<Long> allowedDeptIds = allowedDeptCache.computeIfAbsent(finalWarehouseId,
                    key -> getBatchUpdateWarehouseAllowedDeptIds(finalWarehouse));
            if (!allowedDeptIds.contains(finalDeptId)) {
                throw exception(PURCHASE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
            }
        }
    }

    private Set<Long> getBatchUpdateWarehouseAllowedDeptIds(ErpWarehouseDO warehouse) {
        Set<Long> deptIds = new LinkedHashSet<>();
        if (warehouse.getDeptId() != null) {
            deptIds.add(warehouse.getDeptId());
        }
        deptIds.addAll(warehouseService.getWarehouseSaleDeptIds(warehouse.getId()));
        return deptIds;
    }

    private void validateBatchUpdateNoDuplicate(List<ErpPurchaseReturnItemDO> returnItems,
                                                List<Long> selectedItemIds,
                                                ErpWarehouseDO targetWarehouse) {
        Set<Long> selectedItemIdSet = new LinkedHashSet<>(selectedItemIds);
        Set<String> itemKeySet = new LinkedHashSet<>();
        for (ErpPurchaseReturnItemDO item : returnItems) {
            boolean selected = selectedItemIdSet.contains(item.getId());
            Long finalWarehouseId = selected && targetWarehouse != null ? targetWarehouse.getId() : item.getWarehouseId();
            String itemKey = item.getProductId() + "|" + finalWarehouseId + "|0";
            if (!itemKeySet.add(itemKey)) {
                throw exception(PURCHASE_RETURN_ITEM_DUPLICATE, String.valueOf(item.getProductId()));
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseReturnDraft(ErpPurchaseReturnDraftUpdateReqVO updateReqVO) {
        ErpPurchaseReturnDO purchaseReturn = validatePurchaseReturnExists(updateReqVO.getId());
        if (!ErpPurchaseReturnStatusEnum.DRAFT.getStatus().equals(purchaseReturn.getStatus())) {
            throw exception(PURCHASE_RETURN_UPDATE_FAIL_NOT_DRAFT, purchaseReturn.getNo());
        }
        List<ErpPurchaseReturnItemDO> oldItems = purchaseReturnItemMapper.selectListByReturnId(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, purchaseReturn);
        fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldItems);

        Integer returnMode = normalizeDraftReturnMode(updateReqVO);
        List<ErpPurchaseReturnSaveReqVO.Item> itemReqs = filterDraftItems(updateReqVO.getItems(), returnMode);
        validateOptionalDraftReferences(updateReqVO);
        if (ErpPurchaseReturnModeEnum.isByOrder(returnMode)) {
            validateReturnableCountForByOrderIncludingProcessing(itemReqs, updateReqVO.getId());
        }
        List<ErpPurchaseReturnItemDO> items = CollUtil.isEmpty(itemReqs)
                ? Collections.emptyList() : validateDraftPurchaseReturnItems(itemReqs);

        ErpPurchaseReturnDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseReturnDO.class)
                .setNo(null)
                .setStatus(null)
                .setReturnMode(returnMode)
                .setReturnTime(updateReqVO.getReturnTime() != null
                        ? updateReqVO.getReturnTime() : purchaseReturn.getReturnTime());
        fillDraftSource(updateReqVO, updateObj);
        if (updateObj.getHandler() == null) {
            updateObj.setHandler(purchaseReturn.getHandler());
        }
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(purchaseReturn.getDeptId());
        }
        calculateTotalPrice(updateObj, items);
        purchaseReturnMapper.updateById(updateObj);
        replacePurchaseReturnItems(updateReqVO.getId(), items);
        operateLogService.recordUpdate(ERP_PURCHASE_RETURN_TYPE, updateReqVO.getId(), purchaseReturn.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitPurchaseReturnDraft(ErpPurchaseReturnDraftUpdateReqVO updateReqVO) {
        ErpPurchaseReturnDO purchaseReturn = validatePurchaseReturnExists(updateReqVO.getId());
        if (!ErpPurchaseReturnStatusEnum.DRAFT.getStatus().equals(purchaseReturn.getStatus())) {
            throw exception(PURCHASE_RETURN_UPDATE_FAIL_NOT_DRAFT, purchaseReturn.getNo());
        }
        updatePurchaseReturnDraft(updateReqVO);
        submitPurchaseReturn(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPurchaseReturn(Long id) {
        ErpPurchaseReturnDO purchaseReturn = validatePurchaseReturnExists(id);
        if (!ErpPurchaseReturnStatusEnum.DRAFT.getStatus().equals(purchaseReturn.getStatus())) {
            throw exception(PURCHASE_RETURN_SUBMIT_FAIL);
        }
        if (purchaseReturn.getSupplierId() == null) {
            throw exception(PURCHASE_RETURN_SUBMIT_SUPPLIER_REQUIRED);
        }
        if (purchaseReturn.getReturnTime() == null) {
            throw exception(PURCHASE_RETURN_SUBMIT_TIME_REQUIRED);
        }
        List<ErpPurchaseReturnItemDO> items = purchaseReturnItemMapper.selectListByReturnId(id);
        if (CollUtil.isEmpty(items)) {
            throw exception(PURCHASE_RETURN_SUBMIT_ITEMS_REQUIRED);
        }
        Integer returnMode = purchaseReturn.getReturnMode();
        if (returnMode == null || (!ErpPurchaseReturnModeEnum.isByOrder(returnMode)
                && !ErpPurchaseReturnModeEnum.isByStock(returnMode))) {
            throw exception(PURCHASE_RETURN_MODE_INVALID);
        }
        if (purchaseReturn.getOrderId() != null) {
            purchaseOrderService.validatePurchaseOrder(purchaseReturn.getOrderId());
        } else {
            supplierService.validateSupplier(purchaseReturn.getSupplierId());
        }
        List<ErpPurchaseReturnSaveReqVO.Item> itemReqs =
                BeanUtils.toBean(items, ErpPurchaseReturnSaveReqVO.Item.class);
        if (ErpPurchaseReturnModeEnum.isByOrder(returnMode)) {
            validateReturnableCountForByOrderIncludingProcessing(itemReqs, id);
        }
        validatePurchaseReturnItems(itemReqs);
        int updateCount = purchaseReturnMapper.updateByIdAndStatus(id,
                ErpPurchaseReturnStatusEnum.DRAFT.getStatus(),
                new ErpPurchaseReturnDO().setStatus(ErpPurchaseReturnStatusEnum.PROCESS.getStatus()));
        if (updateCount == 0) {
            throw exception(PURCHASE_RETURN_SUBMIT_FAIL);
        }
        if (purchaseReturn.getOrderId() != null) {
            updatePurchaseOrderReturnCount(purchaseReturn.getOrderId());
        }
        operateLogService.recordUpdate(ERP_PURCHASE_RETURN_TYPE, id, purchaseReturn.getNo());
    }

    @Override
    public void updatePurchaseReturnRemark(ErpPurchaseUpdateRemarkReqVO updateReqVO) {
        ErpPurchaseReturnDO purchaseReturn = validatePurchaseReturnExists(updateReqVO.getId());
        purchaseReturnMapper.updateById(new ErpPurchaseReturnDO()
                .setId(updateReqVO.getId())
                .setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_PURCHASE_RETURN_TYPE, updateReqVO.getId(), purchaseReturn.getNo());
    }

    private void calculateTotalPrice(ErpPurchaseReturnDO purchaseReturn, List<ErpPurchaseReturnItemDO> purchaseReturnItems) {
        purchaseReturn.setTotalCount(getSumValue(purchaseReturnItems, ErpPurchaseReturnItemDO::getCount,
                BigDecimal::add, BigDecimal.ZERO));
        purchaseReturn.setTotalProductPrice(getSumValue(purchaseReturnItems, ErpPurchaseReturnItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        purchaseReturn.setTotalTaxPrice(BigDecimal.ZERO);
        purchaseReturn.setTotalPrice(purchaseReturn.getTotalProductPrice());
        if (purchaseReturn.getDiscountPercent() == null) {
            purchaseReturn.setDiscountPercent(BigDecimal.ZERO);
        }
        BigDecimal feeAmount = resolveFeeAmount(purchaseReturn.getFeeAmount(), purchaseReturn.getOtherPrice());
        purchaseReturn.setFeeAmount(feeAmount);
        purchaseReturn.setOtherPrice(feeAmount);
        purchaseReturn.setDiscountPrice(MoneyUtils.priceMultiplyPercent(purchaseReturn.getTotalPrice(), purchaseReturn.getDiscountPercent()));
        purchaseReturn.setTotalPrice(purchaseReturn.getTotalPrice().subtract(purchaseReturn.getDiscountPrice()).add(feeAmount));
    }

    private BigDecimal resolveFeeAmount(BigDecimal feeAmount, BigDecimal otherPrice) {
        return feeAmount != null ? feeAmount : (otherPrice != null ? otherPrice : BigDecimal.ZERO);
    }

    private Integer normalizeDraftReturnMode(ErpPurchaseReturnSaveReqVO reqVO) {
        if (reqVO.getReturnMode() != null) {
            if (!ErpPurchaseReturnModeEnum.isByOrder(reqVO.getReturnMode())
                    && !ErpPurchaseReturnModeEnum.isByStock(reqVO.getReturnMode())) {
                throw exception(PURCHASE_RETURN_MODE_INVALID);
            }
            return reqVO.getReturnMode();
        }
        return reqVO.getOrderId() != null
                ? ErpPurchaseReturnModeEnum.BY_ORDER.getMode()
                : ErpPurchaseReturnModeEnum.BY_STOCK.getMode();
    }

    private void validateOptionalDraftReferences(ErpPurchaseReturnSaveReqVO reqVO) {
        if (reqVO.getOrderId() != null) {
            purchaseOrderService.validatePurchaseOrder(reqVO.getOrderId());
        } else if (reqVO.getSupplierId() != null) {
            supplierService.validateSupplier(reqVO.getSupplierId());
        }
    }

    private void fillDraftSource(ErpPurchaseReturnSaveReqVO reqVO, ErpPurchaseReturnDO target) {
        if (reqVO.getOrderId() == null) {
            target.setOrderNo(StrUtil.EMPTY);
            return;
        }
        ErpPurchaseOrderDO purchaseOrder = purchaseOrderService.validatePurchaseOrder(reqVO.getOrderId());
        target.setOrderNo(purchaseOrder.getNo()).setSupplierId(purchaseOrder.getSupplierId())
                .setDeptId(purchaseOrder.getDeptId());
    }

    private List<ErpPurchaseReturnSaveReqVO.Item> filterDraftItems(
            List<ErpPurchaseReturnSaveReqVO.Item> items, Integer returnMode) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(item -> item.getProductId() != null && item.getWarehouseId() != null
                        && item.getCount() != null && item.getCount().compareTo(BigDecimal.ZERO) > 0
                        && (!ErpPurchaseReturnModeEnum.isByOrder(returnMode) || item.getSourceInItemId() != null))
                .collect(Collectors.toList());
    }

    private List<ErpPurchaseReturnItemDO> validateDraftPurchaseReturnItems(List<ErpPurchaseReturnSaveReqVO.Item> list) {
        Set<String> itemKeySet = new LinkedHashSet<>();
        for (ErpPurchaseReturnSaveReqVO.Item item : list) {
            String productKey = StrUtil.blankToDefault(item.getProductCode(), String.valueOf(item.getProductId()));
            String itemKey = item.getProductId() + "|" + item.getWarehouseId() + "|0";
            if (!itemKeySet.add(itemKey)) {
                throw exception(PURCHASE_RETURN_ITEM_DUPLICATE, productKey);
            }
        }
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() -> productService.validProductList(
                convertSet(list, ErpPurchaseReturnSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpPurchaseReturnSaveReqVO.Item::getProductId, ErpPurchaseReturnSaveReqVO.Item::getBatchNo);
        Set<Long> warehouseIds = convertSet(list, ErpPurchaseReturnSaveReqVO.Item::getWarehouseId);
        warehouseService.validPurchaseWarehouseList(warehouseIds);
        Map<Long, cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO> warehouseMap =
                warehouseService.getWarehouseMap(warehouseIds);
        return convertList(list, o -> BeanUtils.toBean(o, ErpPurchaseReturnItemDO.class, item -> {
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            fillDeptIdFromWarehouse(item, warehouseMap);
            item.setTaxPercent(null);
            item.setTaxPrice(BigDecimal.ZERO);
            BigDecimal productPrice = item.getProductPrice();
            if (productPrice == null || productPrice.compareTo(BigDecimal.ZERO) < 0) {
                productPrice = BigDecimal.ZERO;
            }
            item.setProductPrice(productPrice);
            item.setTotalPrice(MoneyUtils.priceMultiply(productPrice, item.getCount()));
        }));
    }

    private void replacePurchaseReturnItems(Long returnId, List<ErpPurchaseReturnItemDO> items) {
        purchaseReturnItemMapper.deleteByReturnId(returnId);
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> item.setId(null).setReturnId(returnId));
        purchaseDocumentDefaultService.fillCreateAuditDefaults(items);
        purchaseReturnItemMapper.insertBatch(items);
    }

    private void updatePurchaseOrderReturnCount(Long orderId) {
        // 1.1 查询采购订单对应的采购出库单列表
        List<ErpPurchaseReturnDO> purchaseReturns = purchaseReturnMapper.selectListByOrderId(orderId);
        purchaseReturns.removeIf(purchaseReturn ->
                ErpPurchaseReturnStatusEnum.DRAFT.getStatus().equals(purchaseReturn.getStatus()));
        // 1.2 查询对应的采购订单项的退货数�?
        Map<Long, BigDecimal> returnCountMap = purchaseReturnItemMapper.selectOrderItemCountSumMapByReturnIds(
                convertList(purchaseReturns, ErpPurchaseReturnDO::getId));
        // 2. 更新采购订单的出库数�?
        purchaseOrderService.updatePurchaseOrderReturnCount(orderId, returnCountMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseReturnStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(PURCHASE_RETURN_PROCESS_FAIL);
        }
        // 1.1 校验存在
        ErpPurchaseReturnDO purchaseReturn = validatePurchaseReturnExists(id);
        // Validate status.
        if (!ErpAuditStatus.PROCESS.getStatus().equals(purchaseReturn.getStatus())) {
            throw exception(PURCHASE_RETURN_APPROVE_FAIL);
        }

        List<ErpPurchaseReturnItemDO> purchaseReturnItems = purchaseReturnItemMapper.selectListByReturnId(id);
        if (ErpPurchaseReturnModeEnum.isByOrder(purchaseReturn.getReturnMode())) {
            validateReturnableCountForByOrder(BeanUtils.toBean(purchaseReturnItems, ErpPurchaseReturnSaveReqVO.Item.class), id);
        }

        // Update status.
        int updateCount = purchaseReturnMapper.updateByIdAndStatus(id, purchaseReturn.getStatus(),
                new ErpPurchaseReturnDO().setStatus(ErpAuditStatus.APPROVE.getStatus()));
        if (updateCount == 0) {
            throw exception(PURCHASE_RETURN_APPROVE_FAIL);
        }

        // 3. 变更库存
        warehouseService.validPurchaseWarehouseList(convertSet(purchaseReturnItems, ErpPurchaseReturnItemDO::getWarehouseId));
        purchaseReturnItems.forEach(purchaseReturnItem -> {
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    purchaseReturnItem.getProductId(), purchaseReturnItem.getWarehouseId(), purchaseReturnItem.getBatchNo(),
                    purchaseReturnItem.getCount().negate(),
                    ErpStockRecordBizTypeEnum.PURCHASE_RETURN.getType(), purchaseReturnItem.getReturnId(), purchaseReturnItem.getId(), purchaseReturn.getNo(),
                    purchaseReturnItem.getProductPrice(), purchaseReturn.getReturnTime()));
        });

        // 4. 审批通过：自动生成采购红字凭证（受系统开账配置控制）
        if (bookOpenService.isVoucherTypeEnabled(
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
        operateLogService.recordStatus(ERP_PURCHASE_RETURN_TYPE, id, purchaseReturn.getNo(), true);
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
        // 0.5 校验同一明细中产品 + 仓库 + 赠品标识不重复（采购退货暂无赠品字段，固定 false）
        Set<String> itemKeySet = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(list)) {
            for (ErpPurchaseReturnSaveReqVO.Item item : list) {
                String productKey = StrUtil.blankToDefault(item.getProductCode(), String.valueOf(item.getProductId()));
                String itemKey = item.getProductId() + "|" + item.getWarehouseId() + "|0";
                if (!itemKeySet.add(itemKey)) {
                    throw exception(PURCHASE_RETURN_ITEM_DUPLICATE, productKey);
                }
            }
        }
        // 1. 校验产品存在
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() -> productService.validProductList(
                convertSet(list, ErpPurchaseReturnSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpPurchaseReturnSaveReqVO.Item::getProductId, ErpPurchaseReturnSaveReqVO.Item::getBatchNo);
        Set<Long> warehouseIds = convertSet(list, ErpPurchaseReturnSaveReqVO.Item::getWarehouseId);
        warehouseService.validPurchaseWarehouseList(warehouseIds);
        Map<Long, cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);
        // 2. 转化为 ErpPurchaseReturnItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpPurchaseReturnItemDO.class, item -> {
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            fillDeptIdFromWarehouse(item, warehouseMap);
            item.setTaxPercent(null);
            item.setTaxPrice(BigDecimal.ZERO);
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
        }));
    }

    private void fillDeptIdFromWarehouse(ErpPurchaseReturnItemDO item,
                                         Map<Long, cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO> warehouseMap) {
        if (item.getDeptId() == null && item.getWarehouseId() != null) {
            cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            item.setDeptId(warehouse == null ? null : warehouse.getDeptId());
        }
    }

    private void validateReturnableCountForByOrder(List<ErpPurchaseReturnSaveReqVO.Item> items, Long currentReturnId) {
        validateReturnableCountForByOrder(items, currentReturnId, false);
    }

    private void validateReturnableCountForByOrderIncludingProcessing(List<ErpPurchaseReturnSaveReqVO.Item> items,
                                                                      Long currentReturnId) {
        validateReturnableCountForByOrder(items, currentReturnId, true);
    }

    private void validateReturnableCountForByOrder(List<ErpPurchaseReturnSaveReqVO.Item> items, Long currentReturnId,
                                                   boolean includeProcessingReturns) {
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

        // 2. 聚合当前单对每个 sourceInItemId 的提交数�?
        Map<Long, BigDecimal> currentItemCountMap = new HashMap<>();
        for (ErpPurchaseReturnSaveReqVO.Item it : items) {
            currentItemCountMap.merge(it.getSourceInItemId(), it.getCount(), BigDecimal::add);
        }

        // 3. 查询这些 sourceInItemId 对应的原入库�?
        Set<Long> sourceInItemIds = currentItemCountMap.keySet();
        List<ErpPurchaseInItemDO> inItems = purchaseInItemMapper.selectBatchIds(sourceInItemIds);
        Map<Long, ErpPurchaseInItemDO> inItemMap = convertMap(inItems, ErpPurchaseInItemDO::getId);

        // 4. 查询这些入库项的"其他退货单"累计已退数量（排�?currentReturnId�?
        Map<Long, BigDecimal> returnedMap = includeProcessingReturns
                ? purchaseReturnItemMapper.selectProcessingAndApprovedReturnedCountMapBySourceInItemIdsExcludeReturn(
                        sourceInItemIds, currentReturnId)
                : purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIdsExcludeReturn(
                        sourceInItemIds, currentReturnId);
        if (returnedMap == null) {
            returnedMap = Collections.emptyMap();
        }

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

        // Batch insert, update and delete items.
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setReturnId(id));
            purchaseDocumentDefaultService.fillCreateAuditDefaults(diffList.get(0));
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
            // 2.2 删除订单�?
            purchaseReturnItemMapper.deleteByReturnId(purchaseReturn.getId());

            // 2.3 更新采购订单的出库数�?
            if (purchaseReturn.getOrderId() != null) {
                updatePurchaseOrderReturnCount(purchaseReturn.getOrderId());
            }
            operateLogService.recordDelete(ERP_PURCHASE_RETURN_TYPE, purchaseReturn.getId(), purchaseReturn.getNo());
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
                DEPT_PERMISSION_FORM_KEY);
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

    @Override
    public PageResult<ErpPurchaseReturnDO> getPurchaseReturnPage(ErpPurchaseReturnPageReqVO pageReqVO) {
        PageResult<ErpPurchaseReturnDO> pageResult = purchaseReturnMapper.selectPage(pageReqVO);
        Map<Long, BigDecimal> paymentPriceMap = financePaymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(
                convertSet(pageResult.getList(), ErpPurchaseReturnDO::getId),
                ErpBizTypeEnum.PURCHASE_RETURN.getType());
        pageResult.getList().forEach(item -> item.setRefundPrice(
                paymentPriceMap.getOrDefault(item.getId(), BigDecimal.ZERO).abs()));
        return pageResult;
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
                warehouseService.getPurchaseWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()),
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpPurchaseImportResultRespVO importPurchaseReturnOrderList(List<ErpPurchaseReturnOrderImportExcelVO> list) {
        ErpPurchaseImportResultRespVO respVO = new ErpPurchaseImportResultRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        Map<String, cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO> supplierMap = buildSupplierMap();
        Map<String, ErpProductDO> productMap = productMapper.selectListByCodes(extractPurchaseReturnOrderProductCodes(list)).stream()
                .collect(Collectors.toMap(ErpProductDO::getCode, product -> product, (a, b) -> a));
        Map<String, cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO> warehouseMap =
                warehouseService.getPurchaseWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()).stream()
                        .collect(Collectors.toMap(cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO::getName,
                                warehouse -> warehouse, (a, b) -> a));
        Map<Long, cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO> productVOMap =
                productService.getProductVOMap(convertSet(productMap.values(), ErpProductDO::getId));

        List<PurchaseReturnOrderImportGroup> groups = new ArrayList<>();
        PurchaseReturnOrderImportGroup currentGroup = null;
        for (int i = 0; i < list.size(); i++) {
            ErpPurchaseReturnOrderImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (isBlankPurchaseReturnOrderRow(row)) {
                continue;
            }
            boolean hasMain = hasPurchaseReturnOrderMainFields(row);
            boolean hasDetail = hasPurchaseReturnOrderDetailFields(row);
            if (hasMain) {
                cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO supplier =
                        resolveSupplier(row.getSupplierName(), supplierMap);
                currentGroup = new PurchaseReturnOrderImportGroup(rowNo, row, supplier);
                groups.add(currentGroup);
                String orderNo = resolvePurchaseReturnOrderNo(rowNo, row);
                String supplierName = trimToNull(row.getSupplierName());
                if (supplierName == null) {
                    addImportFailure(respVO, rowNo, orderNo, null, "供应商不能为空");
                } else if (supplier == null) {
                    addImportFailure(respVO, rowNo, orderNo, null, "供应商不存在：" + supplierName);
                } else if (CommonStatusEnum.isDisable(supplier.getStatus())) {
                    addImportFailure(respVO, rowNo, orderNo, null, "供应商(" + supplier.getName() + ")已禁用");
                }
                try {
                    parsePurchaseReturnImportTime(row.getReturnTime(), null);
                } catch (IllegalArgumentException ex) {
                    addImportFailure(respVO, rowNo, orderNo, null, ex.getMessage());
                }
            } else if (hasDetail && currentGroup == null) {
                addImportFailure(respVO, rowNo, null, trimToNull(row.getProductCode()), "明细行前缺少退货单主表信息");
                continue;
            }
            if (!hasDetail) {
                continue;
            }
            String orderNo = currentGroup == null ? null : resolvePurchaseReturnOrderNo(currentGroup.getRowNo(), currentGroup.getMainRow());
            String productCode = trimToNull(row.getProductCode());
            boolean valid = true;
            ErpProductDO product = productMap.get(productCode);
            cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO warehouse = warehouseMap.get(trimToNull(row.getWarehouseName()));
            if (productCode == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "产品编码不能为空");
                valid = false;
            } else if (product == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "产品不存在：" + productCode);
                valid = false;
            }
            if (trimToNull(row.getWarehouseName()) == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "所属仓库不能为空");
                valid = false;
            } else if (warehouse == null) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "所属仓库不存在：" + row.getWarehouseName());
                valid = false;
            }
            if (row.getItemCount() == null || row.getItemCount().compareTo(BigDecimal.ZERO) <= 0) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "退货数量必须大于 0");
                valid = false;
            }
            BigDecimal price = row.getProductPrice() != null ? row.getProductPrice() : (product == null ? null : product.getPurchasePrice());
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                addImportFailure(respVO, rowNo, orderNo, productCode, "退货单价必须大于 0");
                valid = false;
            }
            if (valid) {
                currentGroup.getRows().add(new PurchaseReturnOrderImportRow(rowNo, row, product, warehouse));
            }
        }

        for (PurchaseReturnOrderImportGroup group : groups) {
            if (CollUtil.isEmpty(group.getRows())) {
                addImportFailure(respVO, group.getRowNo(), resolvePurchaseReturnOrderNo(group.getRowNo(), group.getMainRow()), null,
                        "采购退货单至少需要一条有效明细");
            }
        }
        if (respVO.getFailureCount() > 0) {
            return respVO;
        }

        for (PurchaseReturnOrderImportGroup group : groups) {
            ErpPurchaseReturnSaveReqVO saveReqVO = buildPurchaseReturnOrderSaveReq(group, productVOMap);
            Long id = createPurchaseReturn(saveReqVO);
            respVO.getDocumentIds().add(id);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    private ErpPurchaseReturnSaveReqVO buildPurchaseReturnOrderSaveReq(PurchaseReturnOrderImportGroup group,
                                                                       Map<Long, cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO> productVOMap) {
        ErpPurchaseReturnOrderImportExcelVO mainRow = group.getMainRow();
        ErpPurchaseReturnSaveReqVO saveReqVO = new ErpPurchaseReturnSaveReqVO();
        saveReqVO.setSupplierId(group.getSupplier().getId());
        saveReqVO.setReturnMode(ErpPurchaseReturnModeEnum.BY_STOCK.getMode());
        saveReqVO.setReturnTime(parsePurchaseReturnImportTime(mainRow.getReturnTime(), LocalDateTime.now()));
        saveReqVO.setRemark(trimToNull(mainRow.getRemark()));
        saveReqVO.setDiscountPercent(BigDecimal.ZERO);
        saveReqVO.setOtherPrice(BigDecimal.ZERO);
        saveReqVO.setItems(convertList(group.getRows(), row -> buildPurchaseReturnOrderItem(row, productVOMap)));
        return saveReqVO;
    }

    private ErpPurchaseReturnSaveReqVO.Item buildPurchaseReturnOrderItem(PurchaseReturnOrderImportRow importRow,
                                                                         Map<Long, cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO> productVOMap) {
        ErpPurchaseReturnOrderImportExcelVO row = importRow.getRow();
        ErpProductDO product = importRow.getProduct();
        ErpPurchaseReturnSaveReqVO.Item item = new ErpPurchaseReturnSaveReqVO.Item();
        item.setProductId(product.getId());
        item.setProductCode(product.getCode());
        item.setProductName(product.getName());
        item.setProductUnitId(product.getUnitId());
        item.setWarehouseId(importRow.getWarehouse().getId());
        item.setWarehouseName(importRow.getWarehouse().getName());
        item.setCount(row.getItemCount());
        item.setProductPrice(row.getProductPrice() != null ? row.getProductPrice() : product.getPurchasePrice());
        item.setPackageQty(defaultPackageQty(product.getPackageQty()));
        item.setBatchNo(trimToNull(row.getBatchNo()));
        item.setRemark(trimToNull(row.getItemRemark()));
        item.setBarCode(product.getBarCode());
        item.setBrand(product.getBrand());
        item.setVehicleModel(product.getVehicleModel());
        item.setOriginPlace(product.getOriginPlace());
        item.setDrawingNo(product.getDrawingNo());
        cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO productVO = productVOMap.get(product.getId());
        if (productVO != null) {
            item.setProductUnitName(productVO.getUnitName());
        }
        return item;
    }

    private Map<String, cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO> buildSupplierMap() {
        Map<String, cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO> map = new LinkedHashMap<>();
        ErpSupplierPageReqVO pageReqVO = new ErpSupplierPageReqVO();
        pageReqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        supplierService.getSupplierPage(pageReqVO).getList().forEach(supplier -> putSupplierKeys(map, supplier));
        return map;
    }

    private cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO resolveSupplier(
            String supplierName, Map<String, cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO> supplierMap) {
        return supplierMap.get(normalizeKey(supplierName));
    }

    private void putSupplierKeys(Map<String, cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO> map,
                                 cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO supplier) {
        putIfNotBlank(map, supplier.getCode(), supplier);
        putIfNotBlank(map, supplier.getOldCode(), supplier);
        putIfNotBlank(map, supplier.getName(), supplier);
        putIfNotBlank(map, supplier.getShortName(), supplier);
    }

    private <T> void putIfNotBlank(Map<String, T> map, String key, T value) {
        String normalized = normalizeKey(key);
        if (StrUtil.isNotBlank(normalized)) {
            map.putIfAbsent(normalized, value);
        }
    }

    private String normalizeKey(String value) {
        String normalized = trimToNull(value);
        if (StrUtil.isBlank(normalized)) {
            return normalized;
        }
        return StrUtil.cleanBlank(normalized).toLowerCase(Locale.ROOT);
    }

    private static Set<String> extractPurchaseReturnOrderProductCodes(List<ErpPurchaseReturnOrderImportExcelVO> list) {
        Set<String> codes = new LinkedHashSet<>();
        for (ErpPurchaseReturnOrderImportExcelVO row : list) {
            if (row == null) {
                continue;
            }
            String code = trimToNull(row.getProductCode());
            if (code != null) {
                codes.add(code);
            }
        }
        return codes;
    }

    private boolean isBlankPurchaseReturnOrderRow(ErpPurchaseReturnOrderImportExcelVO row) {
        return row == null || !hasPurchaseReturnOrderMainFields(row) && !hasPurchaseReturnOrderDetailFields(row);
    }

    private boolean hasPurchaseReturnOrderMainFields(ErpPurchaseReturnOrderImportExcelVO row) {
        return StrUtil.isNotBlank(trimToNull(row.getSupplierName()))
                || StrUtil.isNotBlank(trimToNull(row.getRemark()));
    }

    private boolean hasPurchaseReturnOrderDetailFields(ErpPurchaseReturnOrderImportExcelVO row) {
        return StrUtil.isNotBlank(trimToNull(row.getProductCode()))
                || StrUtil.isNotBlank(trimToNull(row.getWarehouseName()))
                || row.getItemCount() != null
                || row.getProductPrice() != null
                || StrUtil.isNotBlank(trimToNull(row.getBatchNo()))
                || StrUtil.isNotBlank(trimToNull(row.getItemRemark()));
    }

    private String resolvePurchaseReturnOrderNo(Integer rowNo, ErpPurchaseReturnOrderImportExcelVO row) {
        String no = trimToNull(row.getNo());
        if (no != null) {
            return no;
        }
        String supplierName = trimToNull(row.getSupplierName());
        if (supplierName != null) {
            return supplierName;
        }
        return "第 " + rowNo + " 行";
    }

    private LocalDateTime parsePurchaseReturnImportTime(String value, LocalDateTime defaultValue) {
        String text = trimToNull(value);
        if (text == null) {
            return defaultValue;
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
            // Try date-only format below.
        }
        try {
            return LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            throw new IllegalArgumentException("退货时间格式不正确，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    private void addImportFailure(ErpPurchaseImportResultRespVO respVO, Integer rowNo, String orderNo,
                                  String productCode, String reason) {
        respVO.getFailureDetails().add(new ErpPurchaseImportResultRespVO.FailureItem(
                rowNo, orderNo, productCode, reason));
        respVO.setFailureCount(respVO.getFailureCount() + 1);
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

    private static class PurchaseReturnOrderImportGroup {

        private final Integer rowNo;
        private final ErpPurchaseReturnOrderImportExcelVO mainRow;
        private final cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO supplier;
        private final List<PurchaseReturnOrderImportRow> rows = new ArrayList<>();

        private PurchaseReturnOrderImportGroup(Integer rowNo, ErpPurchaseReturnOrderImportExcelVO mainRow,
                                               cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO supplier) {
            this.rowNo = rowNo;
            this.mainRow = mainRow;
            this.supplier = supplier;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public ErpPurchaseReturnOrderImportExcelVO getMainRow() {
            return mainRow;
        }

        public cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO getSupplier() {
            return supplier;
        }

        public List<PurchaseReturnOrderImportRow> getRows() {
            return rows;
        }

    }

    private static class PurchaseReturnOrderImportRow {

        private final Integer rowNo;
        private final ErpPurchaseReturnOrderImportExcelVO row;
        private final ErpProductDO product;
        private final cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO warehouse;

        private PurchaseReturnOrderImportRow(Integer rowNo, ErpPurchaseReturnOrderImportExcelVO row,
                                             ErpProductDO product,
                                             cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO warehouse) {
            this.rowNo = rowNo;
            this.row = row;
            this.product = product;
            this.warehouse = warehouse;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public ErpPurchaseReturnOrderImportExcelVO getRow() {
            return row;
        }

        public ErpProductDO getProduct() {
            return product;
        }

        public cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO getWarehouse() {
            return warehouse;
        }

    }

}
