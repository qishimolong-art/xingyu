package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreatePurchaseReturnReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreateTargetDraftRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreateTransferOutReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPurchaseReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnTransferOutableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockTransferOutDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseReturnModeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleReturnModeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleReturnStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpImportProductResolver;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.*;

@Service
@Validated
public class ErpSaleReturnServiceImpl implements ErpSaleReturnService {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_return";

    @Resource
    private ErpSaleReturnMapper saleReturnMapper;
    @Resource
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Resource
    private ErpStockMoveItemMapper stockMoveItemMapper;
    @Resource
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpProductBatchNoValidator productBatchNoValidator;
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
    private ErpStockService stockService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockOutBillService stockOutBillService;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleItemBatchUpdateSupport batchUpdateSupport;
    @Resource
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpStockMoveService stockMoveService;
    @Resource
    private ErpPurchaseReturnService purchaseReturnService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private DeptApi deptApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleReturn(ErpSaleReturnSaveReqVO createReqVO) {
        fieldPermissionMasker.clearSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        fieldPermissionMasker.clearSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, createReqVO, createReqVO.getItems());
        Integer returnMode = normalizeReturnMode(createReqVO);
        validateDraftReturnMode(returnMode);
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

        List<ErpSaleReturnItemDO> saleReturnItems = validateSaleReturnItems(createReqVO.getItems(), createReqVO.getDeptId());
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
        saleReturn.setReturnTime(LocalDateTime.now());
        fillSourceInfo(saleReturn, saleOrder, saleOut);
        calculateTotalPrice(saleReturn, saleReturnItems);
        saleDocumentDefaultService.fillCreateDefaults(saleReturn);
        saleReturnMapper.insert(saleReturn);
        saleReturnItems.forEach(item -> item.setReturnId(saleReturn.getId()));
        clearSaleReturnItemIds(saleReturnItems);
        saleReturnItemMapper.insertBatch(saleReturnItems);

        updateSaleOrderReturnCountIfPresent(saleReturn.getOrderId());
        operateLogService.recordCreate(ERP_SALE_RETURN_TYPE, saleReturn.getId(), saleReturn.getNo());
        return saleReturn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleReturnDraft(ErpSaleReturnDraftCreateReqVO createReqVO) {
        fieldPermissionMasker.clearSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        fieldPermissionMasker.clearSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, createReqVO, createReqVO.getItems());
        Integer returnMode = normalizeReturnMode(createReqVO);
        List<ErpSaleReturnSaveReqVO.Item> itemReqs = filterDraftItems(createReqVO.getItems(), returnMode);
        if (CollUtil.isEmpty(itemReqs)) {
            throw exception(SALE_RETURN_DRAFT_ITEMS_REQUIRED);
        }
        ErpSaleOrderDO saleOrder = null;
        ErpSaleOutDO saleOut = null;
        if (ErpSaleReturnModeEnum.LEGACY_ORDER.getMode().equals(returnMode) && createReqVO.getOrderId() != null) {
            saleOrder = saleOrderService.validateSaleOrder(createReqVO.getOrderId());
        } else if (ErpSaleReturnModeEnum.isBySaleOut(returnMode) && createReqVO.getSourceOutId() != null) {
            saleOut = CollUtil.isEmpty(itemReqs)
                    ? saleOutService.validateSaleOut(createReqVO.getSourceOutId())
                    : validateSaleOutReturnable(copyWithItems(createReqVO, itemReqs), null);
        } else if (ErpSaleReturnModeEnum.isByStock(returnMode) && createReqVO.getCustomerId() != null) {
            customerService.validateCustomer(createReqVO.getCustomerId());
        }
        List<ErpSaleReturnItemDO> items = CollUtil.isEmpty(itemReqs)
                ? Collections.emptyList() : validateSaleReturnItems(itemReqs, createReqVO.getDeptId());
        validateOptionalReferences(createReqVO);

        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_RETURN_NO_PREFIX);
        if (saleReturnMapper.selectByNo(no) != null) {
            throw exception(SALE_RETURN_NO_EXISTS);
        }
        ErpSaleReturnDO saleReturn = BeanUtils.toBean(createReqVO, ErpSaleReturnDO.class, in -> in
                .setNo(no).setStatus(ErpSaleReturnStatusEnum.DRAFT.getStatus()).setReturnMode(returnMode));
        saleReturn.setReturnTime(LocalDateTime.now());
        fillSourceInfo(saleReturn, saleOrder, saleOut);
        calculateTotalPrice(saleReturn, items);
        saleDocumentDefaultService.fillCreateDefaults(saleReturn);
        saleReturnMapper.insert(saleReturn);
        if (CollUtil.isNotEmpty(items)) {
            items.forEach(item -> item.setReturnId(saleReturn.getId()));
            clearSaleReturnItemIds(items);
            saleReturnItemMapper.insertBatch(items);
        }
        operateLogService.recordCreate(ERP_SALE_RETURN_TYPE, saleReturn.getId(), saleReturn.getNo());
        return saleReturn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleReturn(ErpSaleReturnSaveReqVO updateReqVO) {
        ErpSaleReturnDO oldSaleReturn = validateSaleReturnExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(oldSaleReturn.getStatus())) {
            throw exception(SALE_RETURN_UPDATE_FAIL_APPROVE, oldSaleReturn.getNo());
        }
        fieldPermissionMasker.preserveSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, oldSaleReturn);
        fieldPermissionMasker.preserveSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO, updateReqVO.getItems(),
                saleReturnItemMapper.selectListByReturnId(updateReqVO.getId()));

        Integer returnMode = normalizeReturnMode(updateReqVO);
        validateDraftReturnMode(returnMode);
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
        Long saleDeptId = updateReqVO.getDeptId() != null ? updateReqVO.getDeptId() : oldSaleReturn.getDeptId();
        List<ErpSaleReturnItemDO> saleReturnItems = validateSaleReturnItems(updateReqVO.getItems(), saleDeptId);

        ErpSaleReturnDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleReturnDO.class)
                .setReturnMode(returnMode);
        updateObj.setReturnTime(LocalDateTime.now());
        fillSourceInfo(updateObj, saleOrder, saleOut);
        calculateTotalPrice(updateObj, saleReturnItems);
        saleReturnMapper.updateById(updateObj);
        updateSaleReturnItemList(updateReqVO.getId(), saleReturnItems);

        updateSaleOrderReturnCountIfPresent(updateObj.getOrderId());
        if (ObjectUtil.notEqual(oldSaleReturn.getOrderId(), updateObj.getOrderId())) {
            updateSaleOrderReturnCountIfPresent(oldSaleReturn.getOrderId());
        }
        operateLogService.recordUpdate(ERP_SALE_RETURN_TYPE, updateReqVO.getId(), oldSaleReturn.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleReturnDraft(ErpSaleReturnDraftUpdateReqVO updateReqVO) {
        ErpSaleReturnDO oldSaleReturn = validateSaleReturnExists(updateReqVO.getId());
        if (!ErpSaleReturnStatusEnum.DRAFT.getStatus().equals(oldSaleReturn.getStatus())) {
            throw exception(SALE_RETURN_UPDATE_FAIL_NOT_DRAFT, oldSaleReturn.getNo());
        }
        fieldPermissionMasker.preserveSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, oldSaleReturn);
        fieldPermissionMasker.preserveSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO, updateReqVO.getItems(),
                saleReturnItemMapper.selectListByReturnId(updateReqVO.getId()));

        Integer returnMode = normalizeReturnMode(updateReqVO);
        List<ErpSaleReturnSaveReqVO.Item> itemReqs = filterDraftItems(updateReqVO.getItems(), returnMode);
        ErpSaleOrderDO saleOrder = null;
        ErpSaleOutDO saleOut = null;
        if (ErpSaleReturnModeEnum.LEGACY_ORDER.getMode().equals(returnMode) && updateReqVO.getOrderId() != null) {
            saleOrder = saleOrderService.validateSaleOrder(updateReqVO.getOrderId());
        } else if (ErpSaleReturnModeEnum.isBySaleOut(returnMode) && updateReqVO.getSourceOutId() != null) {
            saleOut = CollUtil.isEmpty(itemReqs)
                    ? saleOutService.validateSaleOut(updateReqVO.getSourceOutId())
                    : validateSaleOutReturnable(copyWithItems(updateReqVO, itemReqs), updateReqVO.getId());
        } else if (ErpSaleReturnModeEnum.isByStock(returnMode) && updateReqVO.getCustomerId() != null) {
            customerService.validateCustomer(updateReqVO.getCustomerId());
        }
        List<ErpSaleReturnItemDO> items = CollUtil.isEmpty(itemReqs)
                ? Collections.emptyList() : validateSaleReturnItems(itemReqs,
                updateReqVO.getDeptId() != null ? updateReqVO.getDeptId() : oldSaleReturn.getDeptId());
        validateOptionalReferences(updateReqVO);

        ErpSaleReturnDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleReturnDO.class)
                .setNo(oldSaleReturn.getNo())
                .setStatus(ErpSaleReturnStatusEnum.DRAFT.getStatus())
                .setReturnMode(returnMode)
                .setReturnTime(oldSaleReturn.getReturnTime() != null
                        ? oldSaleReturn.getReturnTime() : LocalDateTime.now());
        fillSourceInfo(updateObj, saleOrder, saleOut);
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(oldSaleReturn.getDeptId());
        }
        calculateTotalPrice(updateObj, items);
        saleReturnMapper.updateById(updateObj);
        saleReturnItemMapper.deleteByReturnId(updateReqVO.getId());
        if (CollUtil.isNotEmpty(items)) {
            items.forEach(item -> item.setReturnId(updateReqVO.getId()));
            clearSaleReturnItemIds(items);
            saleReturnItemMapper.insertBatch(items);
        }
        operateLogService.recordUpdate(ERP_SALE_RETURN_TYPE, updateReqVO.getId(), oldSaleReturn.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateSaleReturnItems(ErpSaleReturnItemBatchUpdateReqVO updateReqVO) {
        if (updateReqVO.getWarehouseId() == null && updateReqVO.getDeptId() == null) {
            throw exception(SALE_RETURN_ITEM_BATCH_UPDATE_EMPTY);
        }
        batchUpdateSupport.validateFieldPermission(FIELD_PERMISSION_MODULE,
                updateReqVO.getWarehouseId() != null, updateReqVO.getDeptId() != null,
                SALE_RETURN_ITEM_BATCH_UPDATE_FIELD_DENIED);
        ErpSaleReturnDO saleReturn = validateSaleReturnExists(updateReqVO.getReturnId());
        if (ErpSaleReturnStatusEnum.APPROVE.getStatus().equals(saleReturn.getStatus())) {
            throw exception(SALE_RETURN_UPDATE_FAIL_APPROVE, saleReturn.getNo());
        }
        boolean byStock = ErpSaleReturnModeEnum.isByStock(saleReturn.getReturnMode());
        if (!byStock && updateReqVO.getWarehouseId() != null) {
            throw exception(SALE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_READONLY);
        }

        List<ErpSaleReturnItemDO> returnItems = saleReturnItemMapper.selectListByReturnId(updateReqVO.getReturnId());
        Map<Long, ErpSaleReturnItemDO> itemMap = convertMap(returnItems, ErpSaleReturnItemDO::getId);
        List<ErpSaleReturnItemDO> selectedItems = new ArrayList<>();
        for (Long itemId : new LinkedHashSet<>(updateReqVO.getItemIds())) {
            ErpSaleReturnItemDO item = itemMap.get(itemId);
            if (item == null) {
                throw exception(SALE_RETURN_ITEM_BATCH_UPDATE_NOT_EXISTS, itemId);
            }
            selectedItems.add(item);
        }

        ErpWarehouseDO targetWarehouse = byStock
                ? batchUpdateSupport.validateTargetWarehouse(updateReqVO.getWarehouseId()) : null;
        Long targetDeptId = batchUpdateSupport.resolveTargetDeptId(targetWarehouse, updateReqVO.getDeptId(),
                FIELD_PERMISSION_MODULE, SALE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
        for (ErpSaleReturnItemDO item : selectedItems) {
            Long finalWarehouseId = targetWarehouse != null ? targetWarehouse.getId() : item.getWarehouseId();
            batchUpdateSupport.validateWarehouseDept(finalWarehouseId, targetDeptId, FIELD_PERMISSION_MODULE,
                    SALE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
        }
        if (targetWarehouse != null) {
            validateBatchUpdateSaleReturnNoDuplicate(returnItems, updateReqVO.getItemIds(), targetWarehouse);
        }

        selectedItems.forEach(item -> {
            if (targetWarehouse != null) {
                item.setWarehouseId(targetWarehouse.getId());
                stockService.ensureStockExists(item.getProductId(), targetWarehouse.getId());
            }
            item.setDeptId(targetDeptId);
        });
        saleReturnItemMapper.updateBatch(selectedItems);
        operateLogService.recordUpdate(ERP_SALE_RETURN_TYPE, updateReqVO.getReturnId(), saleReturn.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSaleReturn(Long id) {
        ErpSaleReturnDO saleReturn = validateSaleReturnExists(id);
        if (!ErpSaleReturnStatusEnum.DRAFT.getStatus().equals(saleReturn.getStatus())) {
            throw exception(SALE_RETURN_SUBMIT_FAIL);
        }
        List<ErpSaleReturnItemDO> persistedItems = saleReturnItemMapper.selectListByReturnId(id);
        if (CollUtil.isEmpty(persistedItems)) {
            throw exception(SALE_RETURN_SUBMIT_ITEMS_REQUIRED);
        }
        ErpSaleReturnSaveReqVO submitReqVO = BeanUtils.toBean(saleReturn, ErpSaleReturnSaveReqVO.class);
        submitReqVO.setItems(BeanUtils.toBean(persistedItems, ErpSaleReturnSaveReqVO.Item.class));
        validateFormalSaleReturn(submitReqVO, id);

        int updateCount = saleReturnMapper.updateByIdAndStatus(id, ErpSaleReturnStatusEnum.DRAFT.getStatus(),
                new ErpSaleReturnDO().setStatus(ErpSaleReturnStatusEnum.PROCESS.getStatus()));
        if (updateCount == 0) {
            throw exception(SALE_RETURN_SUBMIT_FAIL);
        }
        operateLogService.recordUpdate(ERP_SALE_RETURN_TYPE, id, saleReturn.getNo());
    }

    @Override
    public void updateSaleReturnRemark(ErpSaleUpdateRemarkReqVO updateReqVO) {
        ErpSaleReturnDO saleReturn = validateSaleReturnExists(updateReqVO.getId());
        saleReturnMapper.updateById(new ErpSaleReturnDO()
                .setId(updateReqVO.getId())
                .setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_SALE_RETURN_TYPE, updateReqVO.getId(), saleReturn.getNo());
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
        Map<Long, BigDecimal> pickedCountMap = getPickedCountMapBySaleOut(reqVO.getSourceOutId());
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
        saleReturns.removeIf(saleReturn -> !ErpAuditStatus.APPROVE.getStatus().equals(saleReturn.getStatus()));
        Map<Long, BigDecimal> returnedCountMap = saleReturnItemMapper.selectSourceOutItemCountSumMapByReturnIds(
                convertList(saleReturns, ErpSaleReturnDO::getId));
        currentCountMap.forEach((sourceOutItemId, count) -> {
            ErpSaleOutItemDO sourceItem = sourceItemMap.get(sourceOutItemId);
            if (sourceItem == null) {
                throw exception(SALE_RETURN_SOURCE_OUT_ITEM_NOT_EXISTS, sourceOutItemId);
            }
            BigDecimal returnedCount = returnedCountMap.getOrDefault(sourceOutItemId, BigDecimal.ZERO);
            BigDecimal sourceCount = pickedCountMap.isEmpty()
                    ? sourceItem.getCount() : pickedCountMap.getOrDefault(sourceOutItemId, BigDecimal.ZERO);
            BigDecimal returnableCount = sourceCount.subtract(returnedCount);
            if (count.compareTo(returnableCount) > 0) {
                throw exception(SALE_RETURN_EXCEED_RETURNABLE, sourceOutItemId, count, returnableCount);
            }
        });
        return saleOut;
    }

    private void validateOptionalReferences(ErpSaleReturnSaveReqVO reqVO) {
        if (reqVO.getAccountId() != null) {
            accountService.validateAccount(reqVO.getAccountId());
        }
        if (reqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(reqVO.getSaleUserId());
        }
    }

    private void validateDraftReturnMode(Integer returnMode) {
        if (!ErpSaleReturnModeEnum.LEGACY_ORDER.getMode().equals(returnMode)
                && !ErpSaleReturnModeEnum.isBySaleOut(returnMode)
                && !ErpSaleReturnModeEnum.isByStock(returnMode)) {
            throw exception(SALE_RETURN_MODE_INVALID);
        }
    }

    private void validateFormalSaleReturn(ErpSaleReturnSaveReqVO reqVO, Long excludeReturnId) {
        Integer returnMode = normalizeReturnMode(reqVO);
        if (ErpSaleReturnModeEnum.LEGACY_ORDER.getMode().equals(returnMode)) {
            saleOrderService.validateSaleOrder(reqVO.getOrderId());
        } else if (ErpSaleReturnModeEnum.isBySaleOut(returnMode)) {
            validateSaleOutReturnable(reqVO, excludeReturnId);
        } else if (ErpSaleReturnModeEnum.isByStock(returnMode)) {
            validateByStockCustomer(reqVO.getCustomerId());
        } else {
            throw exception(SALE_RETURN_MODE_INVALID);
        }
        validateSaleReturnItems(reqVO.getItems(), reqVO.getDeptId());
        validateOptionalReferences(reqVO);
    }

    private ErpSaleReturnSaveReqVO copyWithItems(
            ErpSaleReturnSaveReqVO source, List<ErpSaleReturnSaveReqVO.Item> items) {
        ErpSaleReturnSaveReqVO copy = BeanUtils.toBean(source, ErpSaleReturnSaveReqVO.class);
        copy.setItems(items);
        return copy;
    }

    private List<ErpSaleReturnSaveReqVO.Item> filterDraftItems(
            List<ErpSaleReturnSaveReqVO.Item> items, Integer returnMode) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        List<ErpSaleReturnSaveReqVO.Item> result = new ArrayList<>();
        for (ErpSaleReturnSaveReqVO.Item item : items) {
            boolean hasMinimumFields = item != null
                    && item.getProductId() != null
                    && item.getWarehouseId() != null
                    && item.getCount() != null
                    && item.getCount().compareTo(BigDecimal.ZERO) > 0
                    && item.getProductPrice() != null
                    && item.getProductPrice().compareTo(BigDecimal.ZERO) >= 0;
            boolean hasSourceItem = !ErpSaleReturnModeEnum.isBySaleOut(returnMode)
                    || item.getSourceOutItemId() != null;
            if (hasMinimumFields && hasSourceItem) {
                result.add(item);
            }
        }
        return result;
    }

    private Map<Long, BigDecimal> getPickedCountMapBySaleOut(Long sourceOutId) {
        List<ErpStockOutBillItemDO> stockOutBillItems = stockOutBillService.getSaleOutSourceItemList(sourceOutId);
        if (CollUtil.isEmpty(stockOutBillItems)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        stockOutBillItems.forEach(item -> {
            if (item.getSourceItemId() != null) {
                result.merge(item.getSourceItemId(),
                        item.getPickedCount() != null ? item.getPickedCount() : BigDecimal.ZERO,
                        BigDecimal::add);
            }
        });
        return result;
    }

    private void calculateTotalPrice(ErpSaleReturnDO saleReturn, List<ErpSaleReturnItemDO> saleReturnItems) {
        saleReturn.setTotalCount(getSumValue(saleReturnItems, ErpSaleReturnItemDO::getCount,
                BigDecimal::add, BigDecimal.ZERO));
        saleReturn.setTotalProductPrice(getSumValue(saleReturnItems, ErpSaleReturnItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        saleReturn.setTotalTaxPrice(BigDecimal.ZERO);
        saleReturn.setTotalPrice(saleReturn.getTotalProductPrice());
        if (saleReturn.getDiscountPercent() == null) {
            saleReturn.setDiscountPercent(BigDecimal.ZERO);
        }
        BigDecimal feeAmount = resolveFeeAmount(saleReturn.getFeeAmount(), saleReturn.getOtherPrice());
        saleReturn.setFeeAmount(feeAmount);
        saleReturn.setOtherPrice(feeAmount);
        saleReturn.setDiscountPrice(MoneyUtils.priceMultiplyPercent(saleReturn.getTotalPrice(), saleReturn.getDiscountPercent()));
        saleReturn.setTotalPrice(saleReturn.getTotalPrice().subtract(saleReturn.getDiscountPrice()).add(feeAmount));
        if (saleReturn.getRefundPrice() == null) {
            saleReturn.setRefundPrice(BigDecimal.ZERO);
        }
    }

    private BigDecimal resolveFeeAmount(BigDecimal feeAmount, BigDecimal otherPrice) {
        return feeAmount != null ? feeAmount : (otherPrice != null ? otherPrice : BigDecimal.ZERO);
    }

    private void updateSaleOrderReturnCountIfPresent(Long orderId) {
        if (orderId == null) {
            return;
        }
        List<ErpSaleReturnDO> saleReturns = saleReturnMapper.selectListByOrderId(orderId);
        saleReturns.removeIf(item -> ErpSaleReturnStatusEnum.DRAFT.getStatus().equals(item.getStatus()));
        Map<Long, BigDecimal> returnCountMap = saleReturnItemMapper.selectOrderItemCountSumMapByReturnIds(
                convertList(saleReturns, ErpSaleReturnDO::getId));
        saleOrderService.updateSaleOrderReturnCount(orderId, returnCountMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleReturnStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(SALE_RETURN_PROCESS_FAIL);
        }
        ErpSaleReturnDO saleReturn = validateSaleReturnExists(id);
        if (!ErpAuditStatus.PROCESS.getStatus().equals(saleReturn.getStatus())) {
            throw exception(SALE_RETURN_APPROVE_FAIL);
        }

        int updateCount = saleReturnMapper.updateByIdAndStatus(id, saleReturn.getStatus(),
                new ErpSaleReturnDO().setStatus(ErpAuditStatus.APPROVE.getStatus()));
        if (updateCount == 0) {
            throw exception(SALE_RETURN_APPROVE_FAIL);
        }

        List<ErpSaleReturnItemDO> saleReturnItems = saleReturnItemMapper.selectListByReturnId(id);
        warehouseService.validSaleWarehouseList(convertList(saleReturnItems, ErpSaleReturnItemDO::getWarehouseId));
        Integer bizType = ErpStockRecordBizTypeEnum.SALE_RETURN.getType();
        saleReturnItems.forEach(saleReturnItem -> {
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    saleReturnItem.getProductId(), saleReturnItem.getWarehouseId(), saleReturnItem.getBatchNo(),
                    saleReturnItem.getCount(),
                    bizType, saleReturnItem.getReturnId(), saleReturnItem.getId(), saleReturn.getNo(),
                    saleReturnItem.getProductPrice(), saleReturn.getReturnTime()));
        });

        // 更新原销售单的退货状态
        if (saleReturn.getSourceOutId() != null) {
            updateSaleOutReturnStatus(saleReturn.getSourceOutId());
        }

        // 审核通过：自动生成销售红字凭证（已开账并启用销售凭证类型）
        if (saleReturn.getReturnTime() != null && bookOpenService.isVoucherTypeEnabled(
                saleReturn.getReturnTime().atZone(ZoneId.systemDefault()).toLocalDate(),
                ErpVoucherTypeEnum.SALE.getType())) {
            BigDecimal sumCost = BigDecimal.ZERO;
            for (ErpSaleReturnItemDO item : saleReturnItems) {
                ErpStockDO stock = getStockIgnoreDataPermission(item.getProductId(), item.getWarehouseId());
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
        operateLogService.recordStatus(ERP_SALE_RETURN_TYPE, id, saleReturn.getNo(), true);
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

    private List<ErpSaleReturnItemDO> validateSaleReturnItems(List<ErpSaleReturnSaveReqVO.Item> list, Long saleDeptId) {
        if (CollUtil.isEmpty(list)) {
            throw exception(SALE_RETURN_ITEMS_EMPTY);
        }
        list.forEach(item -> {
            if (item.getProductId() == null) {
                throw exception(SALE_RETURN_ITEM_PRODUCT_REQUIRED);
            }
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(SALE_RETURN_COUNT_POSITIVE);
            }
            if (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw exception(SALE_RETURN_ITEM_PRICE_REQUIRED);
            }
        });
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() ->
                productService.validProductList(convertSet(list, ErpSaleReturnSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpSaleReturnSaveReqVO.Item::getProductId, ErpSaleReturnSaveReqVO.Item::getBatchNo);
        List<Long> warehouseIds = convertList(list, ErpSaleReturnSaveReqVO.Item::getWarehouseId);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(
                warehouseService.validSaleSelectableWarehouseListForDept(warehouseIds, saleDeptId),
                ErpWarehouseDO::getId);
        return convertList(list, itemVO -> BeanUtils.toBean(itemVO, ErpSaleReturnItemDO.class, item -> {
            ErpProductDO product = productMap.get(item.getProductId());
            if (product == null) {
                throw exception(SALE_RETURN_ITEM_PRODUCT_REQUIRED);
            }
            item.setProductUnitId(product.getUnitId());
            fillProductWeightAndPackage(item, product);
            fillDeptIdFromWarehouse(item, warehouseMap);
            item.setTaxPercent(null);
            item.setTaxPrice(BigDecimal.ZERO);
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
        }));
    }

    private void fillProductWeightAndPackage(ErpSaleReturnItemDO item, ErpProductDO product) {
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

    private void fillDeptIdFromWarehouse(ErpSaleReturnItemDO item, Map<Long, ErpWarehouseDO> warehouseMap) {
        if (item.getDeptId() == null && item.getWarehouseId() != null) {
            ErpStockDO stock = getStockIgnoreDataPermission(item.getProductId(), item.getWarehouseId());
            if (stock != null && stock.getDeptId() != null) {
                item.setDeptId(stock.getDeptId());
                return;
            }
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            item.setDeptId(warehouse == null ? null : warehouse.getDeptId());
        }
    }

    private ErpStockDO getStockIgnoreDataPermission(Long productId, Long warehouseId) {
        return DataPermissionUtils.executeIgnore(() -> stockService.getStock(productId, warehouseId));
    }

    private void clearSaleReturnItemIds(List<ErpSaleReturnItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> item.setId(null));
    }

    private void updateSaleReturnItemList(Long id, List<ErpSaleReturnItemDO> newList) {
        List<ErpSaleReturnItemDO> oldList = saleReturnItemMapper.selectListByReturnId(id);
        List<List<ErpSaleReturnItemDO>> diffList = diffList(oldList, newList,
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(item -> item.setReturnId(id));
            clearSaleReturnItemIds(diffList.get(0));
            saleReturnItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            saleReturnItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            saleReturnItemMapper.deleteByIds(convertList(diffList.get(2), ErpSaleReturnItemDO::getId));
        }
    }

    private void validateBatchUpdateSaleReturnNoDuplicate(List<ErpSaleReturnItemDO> returnItems,
                                                          List<Long> selectedItemIds,
                                                          ErpWarehouseDO targetWarehouse) {
        Set<Long> selectedItemIdSet = new LinkedHashSet<>(selectedItemIds);
        Set<String> itemKeySet = new LinkedHashSet<>();
        for (ErpSaleReturnItemDO item : returnItems) {
            boolean selected = selectedItemIdSet.contains(item.getId());
            Long finalWarehouseId = selected ? targetWarehouse.getId() : item.getWarehouseId();
            String itemKey = item.getProductId() + "|" + finalWarehouseId + "|" + normalizeBatchNoKey(item.getBatchNo());
            if (!itemKeySet.add(itemKey)) {
                throw exception(SALE_RETURN_ITEM_DUPLICATE,
                        "productId=" + item.getProductId() + ", warehouseId=" + finalWarehouseId
                                + ", batchNo=" + item.getBatchNo());
            }
        }
    }

    private String normalizeBatchNoKey(String batchNo) {
        return batchNo == null ? "" : batchNo.trim();
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
            operateLogService.recordDelete(ERP_SALE_RETURN_TYPE, saleReturn.getId(), saleReturn.getNo());
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
        PageResult<ErpSaleReturnDO> pageResult = saleReturnMapper.selectPage(pageReqVO);
        Map<Long, BigDecimal> receiptPriceMap = financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                convertSet(pageResult.getList(), ErpSaleReturnDO::getId), ErpBizTypeEnum.SALE_RETURN.getType());
        pageResult.getList().forEach(item -> item.setRefundPrice(
                receiptPriceMap.getOrDefault(item.getId(), BigDecimal.ZERO).abs()));
        return pageResult;
    }

    @Override
    public List<ErpSaleReturnItemDO> getSaleReturnItemListByReturnId(Long returnId) {
        return saleReturnItemMapper.selectListByReturnId(returnId);
    }

    @Override
    public PageResult<ErpSaleReturnItemDO> getSaleReturnItemPage(ErpSaleReturnItemPageReqVO pageReqVO) {
        validateSaleReturnExists(pageReqVO.getReturnId());
        return saleReturnItemMapper.selectPageByReturnId(pageReqVO);
    }

    @Override
    public List<ErpSaleReturnItemDO> getSaleReturnItemListByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyList();
        }
        return saleReturnItemMapper.selectListByReturnIds(returnIds);
    }

    @Override
    public List<DeptSimpleRespVO> getWarehouseAvailableDeptSimpleList(Long warehouseId) {
        return batchUpdateSupport.getWarehouseAvailableDeptSimpleList(warehouseId, FIELD_PERMISSION_MODULE);
    }

    @Override
    public List<ErpSaleReturnTransferOutableItemRespVO> getTransferOutableItemsByReturnId(Long returnId) {
        ErpSaleReturnDO saleReturn = validateSaleReturn(returnId);
        List<ErpSaleReturnItemDO> items = saleReturnItemMapper.selectListByReturnId(returnId);
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        Map<Long, BigDecimal> transferredCountMap = stockMoveItemMapper.selectMovedCountMapBySourceSaleReturnItemIds(
                convertSet(items, ErpSaleReturnItemDO::getId), null);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(items, ErpSaleReturnItemDO::getProductId)));
        Map<Long, ErpProductRespVO> safeProductMap = productMap == null ? Collections.emptyMap() : productMap;
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(items, ErpSaleReturnItemDO::getWarehouseId)));
        Map<Long, ErpWarehouseDO> safeWarehouseMap = warehouseMap == null ? Collections.emptyMap() : warehouseMap;
        Map<Long, DeptRespDTO> deptMap = getDeptMap(items, safeWarehouseMap);
        return convertList(items, item -> {
            BigDecimal transferredCount = transferredCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            BigDecimal availableCount = positiveSubtract(item.getCount(), transferredCount);
            ErpSaleReturnTransferOutableItemRespVO vo = BeanUtils.toBean(item,
                    ErpSaleReturnTransferOutableItemRespVO.class);
            vo.setSourceSaleReturnId(saleReturn.getId());
            vo.setSourceSaleReturnItemId(item.getId());
            vo.setSourceSaleReturnNo(saleReturn.getNo());
            vo.setReturnCount(item.getCount());
            vo.setTransferredCount(transferredCount);
            vo.setTransferOutableCount(availableCount);
            ErpProductRespVO product = safeProductMap.get(item.getProductId());
            if (product != null) {
                vo.setProductCode(product.getCode());
                vo.setProductName(product.getName());
                vo.setProductUnitName(product.getUnitName());
            }
            ErpWarehouseDO warehouse = safeWarehouseMap.get(item.getWarehouseId());
            if (warehouse != null) {
                vo.setFromWarehouseName(warehouse.getName());
                if (vo.getFromDeptId() == null) {
                    vo.setFromDeptId(warehouse.getDeptId());
                }
            }
            DeptRespDTO dept = deptMap.get(vo.getFromDeptId());
            if (dept != null) {
                vo.setFromDeptName(dept.getName());
            }
            vo.setFromWarehouseId(item.getWarehouseId());
            return vo;
        }).stream().filter(item -> item.getTransferOutableCount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpSaleReturnCreateTargetDraftRespVO createTransferOutFromSaleReturn(
            ErpSaleReturnCreateTransferOutReqVO reqVO) {
        ErpSaleReturnDO saleReturn = validateSaleReturn(reqVO.getReturnId());
        List<ErpSaleReturnItemDO> sourceItems = saleReturnItemMapper.selectListByReturnId(reqVO.getReturnId());
        if (CollUtil.isEmpty(sourceItems)) {
            throw exception(SALE_RETURN_TRANSFER_ITEMS_EMPTY);
        }
        Map<Long, ErpSaleReturnItemDO> sourceItemMap = convertMap(sourceItems, ErpSaleReturnItemDO::getId);
        Map<Long, BigDecimal> requestCountMap = validateTransferOutRequestItems(reqVO, sourceItemMap);
        validateSaleReturnTransferAvailable(requestCountMap, sourceItemMap,
                stockMoveItemMapper.selectMovedCountMapBySourceSaleReturnItemIds(requestCountMap.keySet(), null));

        List<ErpWarehouseDO> warehouses = DataPermissionUtils.executeIgnore(() -> warehouseService.validWarehouseList(
                collectTransferWarehouseIds(reqVO)));
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(warehouses, ErpWarehouseDO::getId);
        Map<Long, List<ErpSaleReturnCreateTransferOutReqVO.Item>> groupMap = reqVO.getItems().stream()
                .collect(Collectors.groupingBy(ErpSaleReturnCreateTransferOutReqVO.Item::getFromDeptId,
                        LinkedHashMap::new, Collectors.toList()));
        List<Long> ids = new ArrayList<>();
        List<String> nos = new ArrayList<>();
        for (Map.Entry<Long, List<ErpSaleReturnCreateTransferOutReqVO.Item>> entry : groupMap.entrySet()) {
            ErpStockTransferOutDraftCreateReqVO draftReqVO = new ErpStockTransferOutDraftCreateReqVO();
            draftReqVO.setDeptId(entry.getKey());
            draftReqVO.setFromDeptId(entry.getKey());
            draftReqVO.setToDeptId(entry.getValue().get(0).getToDeptId());
            draftReqVO.setMoveTime(reqVO.getMoveTime() != null ? reqVO.getMoveTime() : LocalDateTime.now());
            draftReqVO.setRemark(reqVO.getRemark());
            draftReqVO.setSourceType(ErpSaleBizSourceTypeEnum.SALE_RETURN.getType());
            draftReqVO.setSourceId(saleReturn.getId());
            draftReqVO.setSourceNo(saleReturn.getNo());
            draftReqVO.setItems(convertList(entry.getValue(), reqItem -> {
                ErpSaleReturnItemDO sourceItem = sourceItemMap.get(reqItem.getSourceSaleReturnItemId());
                ErpStockMoveSaveReqVO.Item item = new ErpStockMoveSaveReqVO.Item();
                item.setFromDeptId(reqItem.getFromDeptId());
                item.setFromWarehouseId(reqItem.getFromWarehouseId());
                item.setToDeptId(reqItem.getToDeptId());
                item.setToWarehouseId(reqItem.getToWarehouseId());
                item.setProductId(sourceItem.getProductId());
                item.setPackageQty(sourceItem.getPackageQty());
                item.setWeight(sourceItem.getWeight());
                item.setProductPrice(sourceItem.getProductPrice());
                item.setCount(reqItem.getCount());
                item.setBatchNo(sourceItem.getBatchNo());
                item.setFromShelf(sourceItem.getWarehousePosition());
                item.setRemark(reqItem.getRemark());
                item.setSourceSaleReturnId(saleReturn.getId());
                item.setSourceSaleReturnItemId(sourceItem.getId());
                item.setSourceSaleReturnNo(saleReturn.getNo());
                return item;
            }));
            validateTransferWarehouses(draftReqVO.getItems(), warehouseMap);
            Long id = stockMoveService.createStockTransferOutDraft(draftReqVO);
            ErpStockMoveDO stockMove = stockMoveService.getStockMove(id);
            ids.add(id);
            nos.add(stockMove == null ? null : stockMove.getNo());
        }
        return ErpSaleReturnCreateTargetDraftRespVO.multiple(ids, nos);
    }

    @Override
    public List<ErpSaleReturnPurchaseReturnableItemRespVO> getPurchaseReturnableItemsByReturnId(Long returnId) {
        ErpSaleReturnDO saleReturn = validateSaleReturn(returnId);
        List<ErpSaleReturnItemDO> items = saleReturnItemMapper.selectListByReturnId(returnId);
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        Map<Long, BigDecimal> purchaseReturnedCountMap =
                purchaseReturnItemMapper.selectReturnedCountMapBySourceSaleReturnItemIds(
                        convertSet(items, ErpSaleReturnItemDO::getId), null);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertSet(items, ErpSaleReturnItemDO::getProductId)));
        Map<Long, ErpProductRespVO> safeProductMap = productMap == null ? Collections.emptyMap() : productMap;
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(items, ErpSaleReturnItemDO::getWarehouseId)));
        Map<Long, ErpWarehouseDO> safeWarehouseMap = warehouseMap == null ? Collections.emptyMap() : warehouseMap;
        Map<Long, DeptRespDTO> deptMap = getDeptMap(items, safeWarehouseMap);
        return convertList(items, item -> {
            BigDecimal returnedCount = purchaseReturnedCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            BigDecimal availableCount = positiveSubtract(item.getCount(), returnedCount);
            ErpSaleReturnPurchaseReturnableItemRespVO vo = BeanUtils.toBean(item,
                    ErpSaleReturnPurchaseReturnableItemRespVO.class);
            vo.setSourceSaleReturnId(saleReturn.getId());
            vo.setSourceSaleReturnItemId(item.getId());
            vo.setSourceSaleReturnNo(saleReturn.getNo());
            vo.setReturnCount(item.getCount());
            vo.setPurchaseReturnedCount(returnedCount);
            vo.setPurchaseReturnableCount(availableCount);
            ErpProductRespVO product = safeProductMap.get(item.getProductId());
            if (product != null) {
                vo.setProductCode(product.getCode());
                vo.setProductName(product.getName());
                vo.setProductUnitName(product.getUnitName());
            }
            ErpWarehouseDO warehouse = safeWarehouseMap.get(item.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
                if (vo.getDeptId() == null) {
                    vo.setDeptId(warehouse.getDeptId());
                }
            }
            DeptRespDTO dept = deptMap.get(vo.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            return vo;
        }).stream().filter(item -> item.getPurchaseReturnableCount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpSaleReturnCreateTargetDraftRespVO createPurchaseReturnFromSaleReturn(
            ErpSaleReturnCreatePurchaseReturnReqVO reqVO) {
        ErpSaleReturnDO saleReturn = validateSaleReturn(reqVO.getReturnId());
        if (reqVO.getSupplierId() == null) {
            throw exception(SALE_RETURN_TRANSFER_SUPPLIER_REQUIRED);
        }
        if (reqVO.getDeptId() == null) {
            throw exception(SALE_RETURN_TRANSFER_DEPT_REQUIRED);
        }
        supplierService.validateSupplier(reqVO.getSupplierId());
        List<ErpSaleReturnItemDO> sourceItems = saleReturnItemMapper.selectListByReturnId(reqVO.getReturnId());
        if (CollUtil.isEmpty(sourceItems)) {
            throw exception(SALE_RETURN_TRANSFER_ITEMS_EMPTY);
        }
        Map<Long, ErpSaleReturnItemDO> sourceItemMap = convertMap(sourceItems, ErpSaleReturnItemDO::getId);
        Map<Long, BigDecimal> requestCountMap = validatePurchaseReturnRequestItems(reqVO, sourceItemMap);
        validateSaleReturnTransferAvailable(requestCountMap, sourceItemMap,
                purchaseReturnItemMapper.selectReturnedCountMapBySourceSaleReturnItemIds(requestCountMap.keySet(), null));

        ErpPurchaseReturnDraftCreateReqVO draftReqVO = new ErpPurchaseReturnDraftCreateReqVO();
        draftReqVO.setSupplierId(reqVO.getSupplierId());
        draftReqVO.setDeptId(reqVO.getDeptId());
        draftReqVO.setReturnMode(ErpPurchaseReturnModeEnum.BY_STOCK.getMode());
        draftReqVO.setReturnTime(reqVO.getReturnTime() != null ? reqVO.getReturnTime() : LocalDateTime.now());
        draftReqVO.setRemark(reqVO.getRemark());
        draftReqVO.setItems(convertList(reqVO.getItems(), reqItem -> {
            ErpSaleReturnItemDO sourceItem = sourceItemMap.get(reqItem.getSourceSaleReturnItemId());
            ErpPurchaseReturnSaveReqVO.Item item = new ErpPurchaseReturnSaveReqVO.Item();
            item.setProductId(sourceItem.getProductId());
            item.setProductCode(null);
            item.setProductUnitId(sourceItem.getProductUnitId());
            item.setWarehouseId(sourceItem.getWarehouseId());
            item.setDeptId(sourceItem.getDeptId());
            item.setProductPrice(reqItem.getProductPrice() != null ? reqItem.getProductPrice()
                    : sourceItem.getProductPrice());
            item.setCount(reqItem.getCount());
            item.setWeight(sourceItem.getWeight());
            item.setPackageQty(sourceItem.getPackageQty());
            item.setWarehousePosition(sourceItem.getWarehousePosition());
            item.setBatchNo(sourceItem.getBatchNo());
            item.setRemark(reqItem.getRemark());
            item.setSourceSaleReturnId(saleReturn.getId());
            item.setSourceSaleReturnItemId(sourceItem.getId());
            item.setSourceSaleReturnNo(saleReturn.getNo());
            return item;
        }));
        Long id = purchaseReturnService.createPurchaseReturnDraft(draftReqVO);
        ErpPurchaseReturnDO purchaseReturn = purchaseReturnService.getPurchaseReturn(id);
        return ErpSaleReturnCreateTargetDraftRespVO.single(id, purchaseReturn == null ? null : purchaseReturn.getNo());
    }

    private Map<Long, DeptRespDTO> getDeptMap(List<ErpSaleReturnItemDO> items, Map<Long, ErpWarehouseDO> warehouseMap) {
        Set<Long> deptIds = convertSet(items, ErpSaleReturnItemDO::getDeptId);
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        return CollUtil.isEmpty(deptIds) ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
    }

    private BigDecimal positiveSubtract(BigDecimal total, BigDecimal used) {
        BigDecimal result = (total == null ? BigDecimal.ZERO : total).subtract(
                used == null ? BigDecimal.ZERO : used);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    private Map<Long, BigDecimal> validateTransferOutRequestItems(ErpSaleReturnCreateTransferOutReqVO reqVO,
                                                                  Map<Long, ErpSaleReturnItemDO> sourceItemMap) {
        if (CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(SALE_RETURN_TRANSFER_ITEMS_EMPTY);
        }
        Map<Long, BigDecimal> requestCountMap = new HashMap<>();
        for (ErpSaleReturnCreateTransferOutReqVO.Item item : reqVO.getItems()) {
            ErpSaleReturnItemDO sourceItem = sourceItemMap.get(item.getSourceSaleReturnItemId());
            if (sourceItem == null) {
                throw exception(SALE_RETURN_TRANSFER_SOURCE_ITEM_NOT_EXISTS);
            }
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(SALE_RETURN_TRANSFER_COUNT_POSITIVE);
            }
            if (Objects.equals(item.getFromWarehouseId(), item.getToWarehouseId())) {
                throw exception(SALE_RETURN_TRANSFER_WAREHOUSE_SAME);
            }
            requestCountMap.merge(item.getSourceSaleReturnItemId(), item.getCount(), BigDecimal::add);
        }
        return requestCountMap;
    }

    private Map<Long, BigDecimal> validatePurchaseReturnRequestItems(ErpSaleReturnCreatePurchaseReturnReqVO reqVO,
                                                                     Map<Long, ErpSaleReturnItemDO> sourceItemMap) {
        if (CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(SALE_RETURN_TRANSFER_ITEMS_EMPTY);
        }
        Map<Long, BigDecimal> requestCountMap = new HashMap<>();
        for (ErpSaleReturnCreatePurchaseReturnReqVO.Item item : reqVO.getItems()) {
            if (!sourceItemMap.containsKey(item.getSourceSaleReturnItemId())) {
                throw exception(SALE_RETURN_TRANSFER_SOURCE_ITEM_NOT_EXISTS);
            }
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(SALE_RETURN_TRANSFER_COUNT_POSITIVE);
            }
            requestCountMap.merge(item.getSourceSaleReturnItemId(), item.getCount(), BigDecimal::add);
        }
        return requestCountMap;
    }

    private void validateSaleReturnTransferAvailable(Map<Long, BigDecimal> requestCountMap,
                                                     Map<Long, ErpSaleReturnItemDO> sourceItemMap,
                                                     Map<Long, BigDecimal> usedCountMap) {
        for (Map.Entry<Long, BigDecimal> entry : requestCountMap.entrySet()) {
            ErpSaleReturnItemDO sourceItem = sourceItemMap.get(entry.getKey());
            if (sourceItem == null) {
                throw exception(SALE_RETURN_TRANSFER_SOURCE_ITEM_NOT_EXISTS);
            }
            BigDecimal available = positiveSubtract(sourceItem.getCount(),
                    usedCountMap.getOrDefault(entry.getKey(), BigDecimal.ZERO));
            if (entry.getValue().compareTo(available) > 0) {
                throw exception(SALE_RETURN_TRANSFER_EXCEED_AVAILABLE,
                        entry.getKey(), entry.getValue(), available);
            }
        }
    }

    private Set<Long> collectTransferWarehouseIds(ErpSaleReturnCreateTransferOutReqVO reqVO) {
        Set<Long> warehouseIds = new LinkedHashSet<>();
        reqVO.getItems().forEach(item -> {
            warehouseIds.add(item.getFromWarehouseId());
            warehouseIds.add(item.getToWarehouseId());
        });
        warehouseIds.remove(null);
        return warehouseIds;
    }

    private void validateTransferWarehouses(List<ErpStockMoveSaveReqVO.Item> items,
                                            Map<Long, ErpWarehouseDO> warehouseMap) {
        for (ErpStockTransferOutDraftCreateReqVO.Item item : items) {
            ErpWarehouseDO fromWarehouse = warehouseMap.get(item.getFromWarehouseId());
            ErpWarehouseDO toWarehouse = warehouseMap.get(item.getToWarehouseId());
            if (fromWarehouse == null || toWarehouse == null
                    || !Objects.equals(fromWarehouse.getDeptId(), item.getFromDeptId())
                    || !Objects.equals(toWarehouse.getDeptId(), item.getToDeptId())) {
                throw exception(SALE_RETURN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
            }
        }
    }

    @Override
    public ErpSaleReturnImportRespVO parseImportData(List<ErpSaleReturnImportExcelVO> list) {
        ErpSaleReturnImportRespVO respVO = new ErpSaleReturnImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        ErpImportProductResolver productResolver = ErpImportProductResolver.build(list,
                ErpSaleReturnImportExcelVO::getProductCode, ErpSaleReturnImportExcelVO::getProductName, ErpSaleReturnImportExcelVO::getFactoryCode, productMapper);
        Map<Long, ErpProductRespVO> productVOMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertList(productResolver.getResolvedProducts(), ErpProductDO::getId)));
        Map<String, ErpWarehouseDO> warehouseMap = convertMap(
                warehouseService.getCurrentUserVisibleSaleWarehouseList(), ErpWarehouseDO::getName);
        for (int i = 0; i < list.size(); i++) {
            ErpSaleReturnImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            ErpImportProductResolver.ResolveResult productResult =
                    productResolver.resolve(row.getProductCode(), row.getProductName(), row.getFactoryCode());
            if (productResult.isFailure()) {
                respVO.getFailureDetails().add(new ErpSaleReturnImportRespVO.FailureItem(
                        rowNo, productResult.getIdentifier(), productResult.getErrorMessage()));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpProductDO product = productResult.getProduct();
            if (row.getCount() == null || row.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                respVO.getFailureDetails().add(new ErpSaleReturnImportRespVO.FailureItem(
                        rowNo, productResult.getIdentifier(), "退货数量必须大于 0"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            if (row.getWarehouseName() == null || row.getWarehouseName().isEmpty()) {
                respVO.getFailureDetails().add(new ErpSaleReturnImportRespVO.FailureItem(
                        rowNo, productResult.getIdentifier(), "所属仓库不能为空"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpWarehouseDO warehouse = warehouseMap.get(row.getWarehouseName());
            if (warehouse == null) {
                respVO.getFailureDetails().add(new ErpSaleReturnImportRespVO.FailureItem(
                        rowNo, productResult.getIdentifier(), "所属仓库不存在"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpSaleReturnRespVO.Item item = new ErpSaleReturnRespVO.Item();
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductName(product.getName());
            item.setProductUnitId(product.getUnitId());
            item.setProductUnitName(productVOMap.get(product.getId()) == null ? null : productVOMap.get(product.getId()).getUnitName());
            item.setWarehouseId(warehouse.getId());
            item.setWarehouseName(warehouse.getName());
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
