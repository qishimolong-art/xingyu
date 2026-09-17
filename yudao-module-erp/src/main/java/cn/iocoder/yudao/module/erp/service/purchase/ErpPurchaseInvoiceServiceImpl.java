package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.ErpPurchaseUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoicePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpImportProductResolver;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.getSumValue;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_ITEM_COUNT_POSITIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_ITEM_EMPTY;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_ITEM_OPERATION_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_ITEM_PRICE_NEGATIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_ITEM_UPDATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_NOT_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_PROCESS_NOT_SUPPORT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SOURCE_IN_INVOICED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SUBMIT_DATE_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SUBMIT_NO_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SUBMIT_TYPE_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SUPPLIER_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_UPDATE_FAIL_NOT_DRAFT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_NOT_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PURCHASE_INVOICE_TYPE;

@Service
@Validated
public class ErpPurchaseInvoiceServiceImpl implements ErpPurchaseInvoiceService {

    private static final String FIELD_PERMISSION_MODULE = "erp_purchase_invoice";
    private static final Integer DRAFT_STATUS = 0;
    private static final Integer PROCESS_STATUS = ErpAuditStatus.PROCESS.getStatus();

    @Resource
    private ErpPurchaseInvoiceMapper purchaseInvoiceMapper;
    @Resource
    private ErpPurchaseInvoiceItemMapper purchaseInvoiceItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseInvoice(ErpPurchaseInvoiceSaveReqVO createReqVO) {
        validateFormalMainFields(createReqVO);
        mergeSourceInItems(createReqVO, null, Collections.emptyList());
        List<ErpPurchaseInvoiceItemDO> items = validatePurchaseInvoiceItems(createReqVO.getItems(), null);
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_INVOICE_NO_PREFIX);
        if (purchaseInvoiceMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_INVOICE_NO_EXISTS);
        }
        ErpPurchaseInvoiceDO purchaseInvoice = BeanUtils.toBean(createReqVO, ErpPurchaseInvoiceDO.class);
        purchaseInvoice.setNo(no);
        purchaseInvoice.setStatus(ErpAuditStatus.PROCESS.getStatus());
        fillDeptIdFromSourceIn(purchaseInvoice, items);
        purchaseDocumentDefaultService.fillCreateDefaults(purchaseInvoice);
        normalizeInvoiceCount(purchaseInvoice);
        calculateTotalPrice(purchaseInvoice, items);
        purchaseDocumentDefaultService.fillCreateAuditDefaults(purchaseInvoice);
        purchaseInvoiceMapper.insert(purchaseInvoice);
        items.forEach(item -> item.setInvoiceId(purchaseInvoice.getId()));
        purchaseDocumentDefaultService.fillCreateAuditDefaults(items);
        purchaseInvoiceItemMapper.insertBatch(items);
        operateLogService.recordCreate(ERP_PURCHASE_INVOICE_TYPE, purchaseInvoice.getId(), purchaseInvoice.getNo());
        return purchaseInvoice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseInvoiceDraft(ErpPurchaseInvoiceDraftCreateReqVO createReqVO) {
        validateOptionalDraftReferences(createReqVO);
        mergeSourceInItems(createReqVO, null, Collections.emptyList());
        List<ErpPurchaseInvoiceItemDO> items = buildDraftPurchaseInvoiceItems(createReqVO.getItems(), null);
        if (CollUtil.isEmpty(items)) {
            throw exception(PURCHASE_INVOICE_ITEM_EMPTY);
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_INVOICE_NO_PREFIX);
        if (purchaseInvoiceMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_INVOICE_NO_EXISTS);
        }
        ErpPurchaseInvoiceDO purchaseInvoice = BeanUtils.toBean(createReqVO, ErpPurchaseInvoiceDO.class);
        purchaseInvoice.setNo(no);
        purchaseInvoice.setStatus(DRAFT_STATUS);
        fillDeptIdFromSourceIn(purchaseInvoice, items);
        purchaseDocumentDefaultService.fillCreateDefaults(purchaseInvoice);
        normalizeInvoiceCount(purchaseInvoice);
        calculateTotalPrice(purchaseInvoice, items);
        purchaseDocumentDefaultService.fillCreateAuditDefaults(purchaseInvoice);
        purchaseInvoiceMapper.insert(purchaseInvoice);
        replacePurchaseInvoiceItems(purchaseInvoice.getId(), items);
        operateLogService.recordCreate(ERP_PURCHASE_INVOICE_TYPE, purchaseInvoice.getId(), purchaseInvoice.getNo());
        return purchaseInvoice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseInvoice(ErpPurchaseInvoiceSaveReqVO updateReqVO) {
        ErpPurchaseInvoiceDO purchaseInvoice = validatePurchaseInvoiceExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseInvoice.getStatus())) {
            throw exception(PURCHASE_INVOICE_UPDATE_FAIL_APPROVE, purchaseInvoice.getNo());
        }
        List<ErpPurchaseInvoiceItemDO> oldItems =
                purchaseInvoiceItemMapper.selectListByInvoiceIdForUpdate(updateReqVO.getId());
        mergeSourceInItems(updateReqVO, updateReqVO.getId(), oldItems);
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, purchaseInvoice);
        fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldItems);
        boolean incrementalItems = ErpPurchaseItemOperationHelper.useIncrementalItems(updateReqVO.getItems(),
                ErpPurchaseInvoiceSaveReqVO.Item::getOperation, PURCHASE_INVOICE_ITEM_OPERATION_INVALID);
        ErpPurchaseItemOperationHelper.RequestChangeSet<ErpPurchaseInvoiceSaveReqVO.Item> itemChangeSet = null;
        List<ErpPurchaseInvoiceSaveReqVO.Item> itemReqs = updateReqVO.getItems();
        if (incrementalItems) {
            itemChangeSet = ErpPurchaseItemOperationHelper.buildRequestChangeSet(updateReqVO.getItems(), oldItems,
                    ErpPurchaseInvoiceSaveReqVO.Item.class,
                    ErpPurchaseInvoiceSaveReqVO.Item::getId,
                    ErpPurchaseInvoiceSaveReqVO.Item::setId,
                    ErpPurchaseInvoiceSaveReqVO.Item::getOperation,
                    ErpPurchaseInvoiceItemDO::getId,
                    PURCHASE_INVOICE_ITEM_OPERATION_INVALID,
                    PURCHASE_INVOICE_ITEM_UPDATE_NOT_EXISTS);
            itemReqs = itemChangeSet.getFinalItems();
        }
        validateFormalMainFields(updateReqVO);
        List<ErpPurchaseInvoiceItemDO> items = validatePurchaseInvoiceItems(itemReqs, updateReqVO.getId());
        ErpPurchaseInvoiceDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseInvoiceDO.class);
        fillDeptIdFromSourceIn(updateObj, items);
        if (updateObj.getHandlerId() == null) {
            updateObj.setHandlerId(purchaseInvoice.getHandlerId());
        }
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(purchaseInvoice.getDeptId());
        }
        purchaseDocumentDefaultService.fillCreateDefaults(updateObj);
        normalizeInvoiceCount(updateObj);
        calculateTotalPrice(updateObj, items);
        purchaseInvoiceMapper.updateById(updateObj);
        if (incrementalItems) {
            applyPurchaseInvoiceItemChangeSet(updateReqVO.getId(), itemChangeSet, items);
        } else {
            updatePurchaseInvoiceItemList(updateReqVO.getId(), items);
        }
        operateLogService.recordUpdate(ERP_PURCHASE_INVOICE_TYPE, updateReqVO.getId(), purchaseInvoice.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseInvoiceDraft(ErpPurchaseInvoiceDraftUpdateReqVO updateReqVO) {
        ErpPurchaseInvoiceDO purchaseInvoice = validatePurchaseInvoiceExists(updateReqVO.getId());
        if (!DRAFT_STATUS.equals(purchaseInvoice.getStatus())) {
            throw exception(PURCHASE_INVOICE_UPDATE_FAIL_NOT_DRAFT, purchaseInvoice.getNo());
        }
        List<ErpPurchaseInvoiceItemDO> oldItems =
                purchaseInvoiceItemMapper.selectListByInvoiceIdForUpdate(updateReqVO.getId());
        mergeSourceInItems(updateReqVO, updateReqVO.getId(), oldItems);
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, purchaseInvoice);
        fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldItems);
        validateOptionalDraftReferences(updateReqVO);
        boolean incrementalItems = ErpPurchaseItemOperationHelper.useIncrementalItems(updateReqVO.getItems(),
                ErpPurchaseInvoiceSaveReqVO.Item::getOperation, PURCHASE_INVOICE_ITEM_OPERATION_INVALID);
        ErpPurchaseItemOperationHelper.RequestChangeSet<ErpPurchaseInvoiceSaveReqVO.Item> itemChangeSet = null;
        List<ErpPurchaseInvoiceSaveReqVO.Item> itemReqs = updateReqVO.getItems();
        if (incrementalItems) {
            itemChangeSet = ErpPurchaseItemOperationHelper.buildRequestChangeSet(updateReqVO.getItems(), oldItems,
                    ErpPurchaseInvoiceSaveReqVO.Item.class,
                    ErpPurchaseInvoiceSaveReqVO.Item::getId,
                    ErpPurchaseInvoiceSaveReqVO.Item::setId,
                    ErpPurchaseInvoiceSaveReqVO.Item::getOperation,
                    ErpPurchaseInvoiceItemDO::getId,
                    PURCHASE_INVOICE_ITEM_OPERATION_INVALID,
                    PURCHASE_INVOICE_ITEM_UPDATE_NOT_EXISTS);
            itemReqs = itemChangeSet.getFinalItems();
        }
        List<ErpPurchaseInvoiceItemDO> items =
                buildDraftPurchaseInvoiceItems(itemReqs, updateReqVO.getId());

        ErpPurchaseInvoiceDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseInvoiceDO.class);
        fillDeptIdFromSourceIn(updateObj, items);
        if (updateObj.getHandlerId() == null) {
            updateObj.setHandlerId(purchaseInvoice.getHandlerId());
        }
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(purchaseInvoice.getDeptId());
        }
        normalizeInvoiceCount(updateObj);
        calculateTotalPrice(updateObj, items);
        int updateCount = purchaseInvoiceMapper.updateDraftByIdAndStatus(
                updateReqVO.getId(), DRAFT_STATUS, updateObj);
        if (updateCount == 0) {
            throw exception(PURCHASE_INVOICE_UPDATE_FAIL_NOT_DRAFT, purchaseInvoice.getNo());
        }
        if (incrementalItems) {
            applyPurchaseInvoiceItemChangeSet(updateReqVO.getId(), itemChangeSet, items);
        } else {
            replacePurchaseInvoiceItems(updateReqVO.getId(), items);
        }
        operateLogService.recordUpdate(ERP_PURCHASE_INVOICE_TYPE, updateReqVO.getId(), purchaseInvoice.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitPurchaseInvoiceDraft(ErpPurchaseInvoiceDraftUpdateReqVO updateReqVO) {
        ErpPurchaseInvoiceDO purchaseInvoice = validatePurchaseInvoiceExists(updateReqVO.getId());
        if (!DRAFT_STATUS.equals(purchaseInvoice.getStatus())) {
            throw exception(PURCHASE_INVOICE_UPDATE_FAIL_NOT_DRAFT, purchaseInvoice.getNo());
        }
        updatePurchaseInvoiceDraft(updateReqVO);
        submitPurchaseInvoice(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPurchaseInvoice(Long id) {
        ErpPurchaseInvoiceDO purchaseInvoice = validatePurchaseInvoiceExists(id);
        if (!DRAFT_STATUS.equals(purchaseInvoice.getStatus())) {
            throw exception(PURCHASE_INVOICE_SUBMIT_FAIL);
        }
        ErpPurchaseInvoiceSaveReqVO mainReqVO = BeanUtils.toBean(
                purchaseInvoice, ErpPurchaseInvoiceSaveReqVO.class);
        validateFormalMainFields(mainReqVO);
        List<ErpPurchaseInvoiceItemDO> persistedItems =
                purchaseInvoiceItemMapper.selectListByInvoiceId(id);
        List<ErpPurchaseInvoiceItemDO> validatedItems = validatePurchaseInvoiceItems(
                BeanUtils.toBean(persistedItems, ErpPurchaseInvoiceSaveReqVO.Item.class), id);
        ErpPurchaseInvoiceDO statusUpdate = new ErpPurchaseInvoiceDO()
                .setStatus(PROCESS_STATUS);
        calculateTotalPrice(statusUpdate, validatedItems);
        int updateCount = purchaseInvoiceMapper.updateByIdAndStatus(
                id, DRAFT_STATUS, statusUpdate);
        if (updateCount == 0) {
            throw exception(PURCHASE_INVOICE_SUBMIT_FAIL);
        }
        operateLogService.recordUpdate(ERP_PURCHASE_INVOICE_TYPE, id, purchaseInvoice.getNo());
    }

    @Override
    public void updatePurchaseInvoiceRemark(ErpPurchaseUpdateRemarkReqVO updateReqVO) {
        ErpPurchaseInvoiceDO purchaseInvoice = validatePurchaseInvoiceExists(updateReqVO.getId());
        purchaseInvoiceMapper.updateById(new ErpPurchaseInvoiceDO()
                .setId(updateReqVO.getId())
                .setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_PURCHASE_INVOICE_TYPE, updateReqVO.getId(), purchaseInvoice.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseInvoiceStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(PURCHASE_INVOICE_PROCESS_NOT_SUPPORT);
        }
        ErpPurchaseInvoiceDO purchaseInvoice = validatePurchaseInvoiceExists(id);
        if (!ErpAuditStatus.PROCESS.getStatus().equals(purchaseInvoice.getStatus())) {
            throw exception(PURCHASE_INVOICE_APPROVE_FAIL);
        }
        List<ErpPurchaseInvoiceItemDO> invoiceItems = purchaseInvoiceItemMapper.selectListByInvoiceId(id);
        validateApprovedSourceInNotInvoiced(invoiceItems, id);
        ErpPurchaseInvoiceDO updateObj = new ErpPurchaseInvoiceDO();
        updateObj.setStatus(ErpAuditStatus.APPROVE.getStatus());
        int updateCount = purchaseInvoiceMapper.updateByIdAndStatus(id, purchaseInvoice.getStatus(), updateObj);
        if (updateCount == 0) {
            throw exception(PURCHASE_INVOICE_APPROVE_FAIL);
        }
        markPurchaseInHasInvoice(id);
        operateLogService.recordStatus(ERP_PURCHASE_INVOICE_TYPE, id, purchaseInvoice.getNo(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseInvoice(List<Long> ids) {
        List<ErpPurchaseInvoiceDO> invoices = purchaseInvoiceMapper.selectByIds(ids);
        if (CollUtil.isEmpty(invoices)) {
            return;
        }
        invoices.forEach(invoice -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(invoice.getStatus())) {
                throw exception(PURCHASE_INVOICE_DELETE_FAIL_APPROVE, invoice.getNo());
            }
        });
        invoices.forEach(invoice -> {
            purchaseInvoiceMapper.deleteById(invoice.getId());
            purchaseInvoiceItemMapper.deleteByInvoiceId(invoice.getId());
            operateLogService.recordDelete(ERP_PURCHASE_INVOICE_TYPE, invoice.getId(), invoice.getNo());
        });
    }

    @Override
    public ErpPurchaseInvoiceDO getPurchaseInvoice(Long id) {
        return purchaseInvoiceMapper.selectById(id);
    }

    @Override
    public ErpPurchaseInvoiceDO validatePurchaseInvoice(Long id) {
        ErpPurchaseInvoiceDO purchaseInvoice = getPurchaseInvoice(id);
        if (purchaseInvoice == null) {
            throw exception(PURCHASE_INVOICE_NOT_EXISTS);
        }
        if (!ObjectUtil.equal(purchaseInvoice.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(PURCHASE_INVOICE_NOT_APPROVE);
        }
        return purchaseInvoice;
    }

    @Override
    public PageResult<ErpPurchaseInvoiceDO> getPurchaseInvoicePage(ErpPurchaseInvoicePageReqVO pageReqVO) {
        return purchaseInvoiceMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPurchaseInvoiceDO> getPurchaseInvoiceList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return purchaseInvoiceMapper.selectByIds(ids);
    }

    @Override
    public List<ErpPurchaseInDO> getPurchaseInList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return purchaseInMapper.selectBatchIds(ids);
    }

    @Override
    public List<ErpPurchaseInvoiceItemDO> getPurchaseInvoiceItemListByInvoiceId(Long invoiceId) {
        return purchaseInvoiceItemMapper.selectListByInvoiceId(invoiceId);
    }

    @Override
    public PageResult<ErpPurchaseInvoiceItemDO> getPurchaseInvoiceItemPage(ErpPurchaseInvoiceItemPageReqVO pageReqVO) {
        validatePurchaseInvoiceExists(pageReqVO.getInvoiceId());
        return purchaseInvoiceItemMapper.selectPageByInvoiceId(pageReqVO);
    }

    @Override
    public List<ErpPurchaseInvoiceItemDO> getPurchaseInvoiceItemListByInvoiceIds(Collection<Long> invoiceIds) {
        if (CollUtil.isEmpty(invoiceIds)) {
            return Collections.emptyList();
        }
        return purchaseInvoiceItemMapper.selectListByInvoiceIds(invoiceIds);
    }

    @Override
    public List<ErpPurchaseInvoiceItemDO> getPurchaseInvoiceItemListBySourceInIds(Collection<Long> sourceInIds) {
        if (CollUtil.isEmpty(sourceInIds)) {
            return Collections.emptyList();
        }
        return purchaseInvoiceItemMapper.selectApprovedListBySourceInIds(sourceInIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpPurchaseImportResultRespVO importPurchaseInvoiceList(List<ErpPurchaseInvoiceImportExcelVO> list) {
        ErpPurchaseImportResultRespVO respVO = new ErpPurchaseImportResultRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        Map<String, ErpSupplierDO> supplierMap = buildSupplierMap();
        ErpImportProductResolver productResolver = ErpImportProductResolver.build(list,
                ErpPurchaseInvoiceImportExcelVO::getProductCode, ErpPurchaseInvoiceImportExcelVO::getProductName,
                ErpPurchaseInvoiceImportExcelVO::getFactoryCode, productMapper);
        Map<String, ErpPurchaseInDO> purchaseInMap = getPurchaseInMapByNos(extractInvoiceSourceInNos(list)).stream()
                .collect(Collectors.toMap(ErpPurchaseInDO::getNo, purchaseIn -> purchaseIn, (a, b) -> a));

        List<PurchaseInvoiceImportGroup> groups = new ArrayList<>();
        PurchaseInvoiceImportGroup currentGroup = null;
        for (int i = 0; i < list.size(); i++) {
            ErpPurchaseInvoiceImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (isBlankInvoiceImportRow(row)) {
                continue;
            }
            boolean hasMain = hasInvoiceMainFields(row);
            boolean hasDetail = hasInvoiceDetailFields(row);
            if (hasMain) {
                ErpSupplierDO supplier = resolveSupplier(row.getSupplierName(), supplierMap);
                currentGroup = new PurchaseInvoiceImportGroup(rowNo, row, supplier);
                groups.add(currentGroup);
                String invoiceNo = resolveInvoiceImportNo(rowNo, row);
                String supplierName = trimToNull(row.getSupplierName());
                if (supplierName == null) {
                    addImportFailure(respVO, rowNo, invoiceNo, null, "供应商不能为空");
                } else if (supplier == null) {
                    addImportFailure(respVO, rowNo, invoiceNo, null, "供应商不存在：" + supplierName);
                } else if (CommonStatusEnum.isDisable(supplier.getStatus())) {
                    addImportFailure(respVO, rowNo, invoiceNo, null, "供应商(" + supplier.getName() + ")未启用");
                }
                if (trimToNull(row.getInvoiceType()) == null) {
                    addImportFailure(respVO, rowNo, invoiceNo, null, "票据类型不能为空");
                }
                if (trimToNull(row.getInvoiceNo()) == null) {
                    addImportFailure(respVO, rowNo, invoiceNo, null, "发票号不能为空");
                }
                validateImportDate(respVO, rowNo, invoiceNo, null, "开票日期", row.getInvoiceDate());
            } else if (hasDetail && currentGroup == null) {
                addImportFailure(respVO, rowNo, null,
                        ErpImportProductResolver.getIdentifier(row.getProductCode(), row.getProductName(), row.getFactoryCode()),
                        "明细行前缺少采购票据主表信息");
                continue;
            }
            if (!hasDetail) {
                continue;
            }
            String invoiceNo = currentGroup == null ? null : resolveInvoiceImportNo(currentGroup.getRowNo(), currentGroup.getMainRow());
            ErpImportProductResolver.ResolveResult productResult =
                    productResolver.resolve(row.getProductCode(), row.getProductName(), row.getFactoryCode());
            String productCode = productResult.getIdentifier();
            boolean valid = true;
            ErpProductDO product = productResult.getProduct();
            if (productResult.isFailure()) {
                addImportFailure(respVO, rowNo, invoiceNo, productCode, productResult.getErrorMessage());
                valid = false;
            }
            if (row.getCount() == null || row.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                addImportFailure(respVO, rowNo, invoiceNo, productCode, "数量必须大于 0");
                valid = false;
            }
            if (row.getProductPrice() == null || row.getProductPrice().compareTo(BigDecimal.ZERO) < 0) {
                addImportFailure(respVO, rowNo, invoiceNo, productCode, "不含税单价不能为空且不能小于 0");
                valid = false;
            }
            PurchaseInvoiceSourceRef sourceRef = validateInvoiceSourceRef(respVO, rowNo, invoiceNo, productCode, row,
                    purchaseInMap);
            valid = valid && sourceRef.isValid();
            if (valid && currentGroup != null) {
                currentGroup.getRows().add(new PurchaseInvoiceImportRow(rowNo, row, product, sourceRef.getPurchaseIn()));
            }
        }

        for (PurchaseInvoiceImportGroup group : groups) {
            if (CollUtil.isEmpty(group.getRows())) {
                addImportFailure(respVO, group.getRowNo(), resolveInvoiceImportNo(group.getRowNo(), group.getMainRow()), null,
                        "采购票据至少需要一行明细");
            }
        }
        if (respVO.getFailureCount() > 0) {
            return respVO;
        }

        Map<Long, ErpProductRespVO> productVOMap = productService.getProductVOMap(
                convertSet(productResolver.getResolvedProducts(), ErpProductDO::getId));
        for (PurchaseInvoiceImportGroup group : groups) {
            ErpPurchaseInvoiceSaveReqVO saveReqVO = buildPurchaseInvoiceImportSaveReq(group, productVOMap);
            Long id = createPurchaseInvoice(saveReqVO);
            respVO.getDocumentIds().add(id);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierId == null) {
            throw exception(PURCHASE_INVOICE_SUPPLIER_REQUIRED);
        }
        supplierService.validateSupplier(supplierId);
    }

    private void validateFormalMainFields(ErpPurchaseInvoiceSaveReqVO reqVO) {
        validateSupplierExists(reqVO.getSupplierId());
        if (reqVO.getInvoiceDate() == null) {
            throw exception(PURCHASE_INVOICE_SUBMIT_DATE_REQUIRED);
        }
        if (StrUtil.isBlank(reqVO.getInvoiceType())) {
            throw exception(PURCHASE_INVOICE_SUBMIT_TYPE_REQUIRED);
        }
        if (StrUtil.isBlank(reqVO.getInvoiceNo())) {
            throw exception(PURCHASE_INVOICE_SUBMIT_NO_REQUIRED);
        }
    }

    private void validateOptionalDraftReferences(ErpPurchaseInvoiceSaveReqVO reqVO) {
        if (reqVO.getSupplierId() != null) {
            supplierService.validateSupplier(reqVO.getSupplierId());
        }
    }

    private void markPurchaseInHasInvoice(Long invoiceId) {
        List<ErpPurchaseInvoiceItemDO> invoiceItems = purchaseInvoiceItemMapper.selectListByInvoiceId(invoiceId);
        if (CollUtil.isEmpty(invoiceItems)) {
            return;
        }
        Set<Long> sourceInIds = convertSet(invoiceItems, ErpPurchaseInvoiceItemDO::getSourceInId);
        sourceInIds.remove(null);
        if (CollUtil.isEmpty(sourceInIds)) {
            return;
        }
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectBatchIds(sourceInIds);
        purchaseIns.forEach(purchaseIn -> purchaseInMapper.updateById(
                new ErpPurchaseInDO().setId(purchaseIn.getId()).setHasInvoice(true)));
    }

    private ErpPurchaseInvoiceDO validatePurchaseInvoiceExists(Long id) {
        ErpPurchaseInvoiceDO purchaseInvoice = purchaseInvoiceMapper.selectById(id);
        if (purchaseInvoice == null) {
            throw exception(PURCHASE_INVOICE_NOT_EXISTS);
        }
        return purchaseInvoice;
    }

    private void mergeSourceInItems(ErpPurchaseInvoiceSaveReqVO reqVO, Long currentInvoiceId,
                                    List<ErpPurchaseInvoiceItemDO> oldItems) {
        if (CollUtil.isEmpty(reqVO.getSourceInIds())) {
            return;
        }
        Set<Long> sourceInIds = new LinkedHashSet<>(reqVO.getSourceInIds());
        sourceInIds.remove(null);
        if (CollUtil.isEmpty(sourceInIds)) {
            return;
        }
        List<ErpPurchaseInDO> purchaseIns = purchaseInMapper.selectBatchIds(sourceInIds);
        Map<Long, ErpPurchaseInDO> purchaseInMap = convertMap(purchaseIns, ErpPurchaseInDO::getId);
        for (Long sourceInId : sourceInIds) {
            ErpPurchaseInDO purchaseIn = purchaseInMap.get(sourceInId);
            if (purchaseIn == null) {
                throw exception(PURCHASE_IN_NOT_EXISTS);
            }
            if (!ErpAuditStatus.APPROVE.getStatus().equals(purchaseIn.getStatus())) {
                throw exception(PURCHASE_IN_NOT_APPROVE);
            }
        }

        Set<Long> excludedSourceInItemIds = CollUtil.isEmpty(reqVO.getExcludedSourceInItemIds())
                ? Collections.emptySet() : new LinkedHashSet<>(reqVO.getExcludedSourceInItemIds());
        Map<Long, ErpPurchaseInvoiceSaveReqVO.Item> submittedSourceItemMap = new LinkedHashMap<>();
        List<ErpPurchaseInvoiceSaveReqVO.Item> manualItems = new ArrayList<>();
        for (ErpPurchaseInvoiceSaveReqVO.Item item : CollUtil.emptyIfNull(reqVO.getItems())) {
            if (item == null) {
                continue;
            }
            if (item.getSourceInItemId() == null) {
                manualItems.add(item);
                continue;
            }
            submittedSourceItemMap.put(item.getSourceInItemId(), item);
        }
        Map<Long, ErpPurchaseInvoiceItemDO> oldSourceItemMap = CollUtil.emptyIfNull(oldItems).stream()
                .filter(item -> item.getSourceInItemId() != null)
                .collect(Collectors.toMap(ErpPurchaseInvoiceItemDO::getSourceInItemId,
                        item -> item, (a, b) -> a, LinkedHashMap::new));

        List<ErpPurchaseInItemDO> sourceItems = purchaseInItemMapper.selectListByInIds(sourceInIds);
        sourceItems.sort((a, b) -> {
            if (a.getId() == null && b.getId() == null) {
                return 0;
            }
            if (a.getId() == null) {
                return -1;
            }
            if (b.getId() == null) {
                return 1;
            }
            return a.getId().compareTo(b.getId());
        });
        List<ErpPurchaseInvoiceSaveReqVO.Item> mergedItems = new ArrayList<>();
        for (ErpPurchaseInItemDO sourceItem : sourceItems) {
            if (sourceItem.getId() != null && excludedSourceInItemIds.contains(sourceItem.getId())) {
                continue;
            }
            mergedItems.add(buildInvoiceItemFromSourceInItem(sourceItem, purchaseInMap.get(sourceItem.getInId()),
                    submittedSourceItemMap.get(sourceItem.getId()), oldSourceItemMap.get(sourceItem.getId())));
        }
        mergedItems.addAll(manualItems);
        validateSourceInNotInvoiced(mergedItems, currentInvoiceId);
        reqVO.setItems(mergedItems);
    }

    private ErpPurchaseInvoiceSaveReqVO.Item buildInvoiceItemFromSourceInItem(
            ErpPurchaseInItemDO sourceItem, ErpPurchaseInDO purchaseIn,
            ErpPurchaseInvoiceSaveReqVO.Item submittedItem, ErpPurchaseInvoiceItemDO oldItem) {
        ErpPurchaseInvoiceSaveReqVO.Item item = new ErpPurchaseInvoiceSaveReqVO.Item();
        item.setId(submittedItem == null ? null : submittedItem.getId());
        if (item.getId() == null && oldItem != null) {
            item.setId(oldItem.getId());
        }
        item.setSourceInId(sourceItem.getInId());
        item.setSourceInNo(purchaseIn == null ? null : purchaseIn.getNo());
        item.setSourceInItemId(sourceItem.getId());
        item.setProductId(sourceItem.getProductId());
        item.setCount(sourceItem.getCount());
        item.setProductPrice(sourceItem.getProductPrice());
        item.setTaxPercent(null);
        item.setRemark(sourceItem.getRemark());
        if (submittedItem != null) {
            item.setCount(submittedItem.getCount());
            item.setProductPrice(submittedItem.getProductPrice());
            item.setTaxPercent(submittedItem.getTaxPercent());
            item.setRemark(submittedItem.getRemark());
        }
        return item;
    }

    private void fillDeptIdFromSourceIn(ErpPurchaseInvoiceDO purchaseInvoice, List<ErpPurchaseInvoiceItemDO> items) {
        if (purchaseInvoice.getDeptId() != null || CollUtil.isEmpty(items)) {
            return;
        }
        Set<Long> sourceInIds = convertSet(items, ErpPurchaseInvoiceItemDO::getSourceInId);
        sourceInIds.remove(null);
        if (CollUtil.isEmpty(sourceInIds)) {
            return;
        }
        CollUtil.emptyIfNull(purchaseInMapper.selectBatchIds(sourceInIds)).stream()
                .map(ErpPurchaseInDO::getDeptId)
                .filter(deptId -> deptId != null)
                .findFirst()
                .ifPresent(purchaseInvoice::setDeptId);
    }

    private List<ErpPurchaseInvoiceItemDO> validatePurchaseInvoiceItems(List<ErpPurchaseInvoiceSaveReqVO.Item> list,
                                                                        Long currentInvoiceId) {
        if (CollUtil.isEmpty(list)) {
            throw exception(PURCHASE_INVOICE_ITEM_EMPTY);
        }
        validateSourceInNotInvoiced(list, currentInvoiceId);
        list.forEach(item -> {
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PURCHASE_INVOICE_ITEM_COUNT_POSITIVE);
            }
            if (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw exception(PURCHASE_INVOICE_ITEM_PRICE_NEGATIVE);
            }
        });
        List<ErpProductDO> products = DataPermissionUtils.executeIgnore(() -> productService.validProductList(
                convertSet(list, ErpPurchaseInvoiceSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(products, ErpProductDO::getId);
        Map<Long, ErpProductRespVO> productVOMap = productService.getProductVOMap(productMap.keySet());
        return convertList(list, item -> BeanUtils.toBean(item, ErpPurchaseInvoiceItemDO.class, invoiceItem -> {
            ErpProductDO product = productMap.get(invoiceItem.getProductId());
            ErpProductRespVO productVO = productVOMap.get(invoiceItem.getProductId());
            if (product != null) {
                invoiceItem.setProductBarCode(product.getBarCode());
            }
            if (productVO != null) {
                invoiceItem.setProductUnitName(productVO.getUnitName());
            }
            invoiceItem.setTaxExclusivePrice(MoneyUtils.priceMultiply(invoiceItem.getProductPrice(), invoiceItem.getCount()));
            if (invoiceItem.getTaxExclusivePrice() == null) {
                invoiceItem.setTaxExclusivePrice(BigDecimal.ZERO);
            }
            invoiceItem.setTaxPercent(null);
            invoiceItem.setTaxPrice(BigDecimal.ZERO);
            invoiceItem.setTotalPrice(invoiceItem.getTaxExclusivePrice());
        }));
    }

    private List<ErpPurchaseInvoiceItemDO> buildDraftPurchaseInvoiceItems(
            List<ErpPurchaseInvoiceSaveReqVO.Item> items, Long currentInvoiceId) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        List<ErpPurchaseInvoiceSaveReqVO.Item> validItems = items.stream()
                .filter(item -> item.getProductId() != null
                        && item.getCount() != null && item.getCount().compareTo(BigDecimal.ZERO) > 0
                        && item.getProductPrice() != null
                        && item.getProductPrice().compareTo(BigDecimal.ZERO) >= 0)
                .collect(Collectors.toList());
        if (validItems.isEmpty()) {
            return Collections.emptyList();
        }
        return validatePurchaseInvoiceItems(validItems, currentInvoiceId);
    }

    private void validateSourceInNotInvoiced(List<ErpPurchaseInvoiceSaveReqVO.Item> list, Long currentInvoiceId) {
        Set<Long> sourceInIds = convertSet(list, ErpPurchaseInvoiceSaveReqVO.Item::getSourceInId);
        sourceInIds.remove(null);
        if (CollUtil.isEmpty(sourceInIds)) {
            return;
        }
        List<ErpPurchaseInvoiceItemDO> existedItems = purchaseInvoiceItemMapper.selectApprovedListBySourceInIds(sourceInIds);
        if (CollUtil.isEmpty(existedItems)) {
            return;
        }
        Map<Long, String> sourceInNoMap = new HashMap<>();
        existedItems.forEach(item -> {
            if (item.getSourceInId() == null) {
                return;
            }
            if (currentInvoiceId != null && currentInvoiceId.equals(item.getInvoiceId())) {
                return;
            }
            sourceInNoMap.putIfAbsent(item.getSourceInId(), item.getSourceInNo());
        });
        if (!sourceInNoMap.isEmpty()) {
            Long conflictSourceInId = sourceInNoMap.keySet().iterator().next();
            throw exception(PURCHASE_INVOICE_SOURCE_IN_INVOICED,
                    sourceInNoMap.getOrDefault(conflictSourceInId, String.valueOf(conflictSourceInId)));
        }
    }

    private void validateApprovedSourceInNotInvoiced(List<ErpPurchaseInvoiceItemDO> list, Long currentInvoiceId) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> sourceInIds = convertSet(list, ErpPurchaseInvoiceItemDO::getSourceInId);
        sourceInIds.remove(null);
        if (CollUtil.isEmpty(sourceInIds)) {
            return;
        }
        List<ErpPurchaseInvoiceItemDO> existedItems = purchaseInvoiceItemMapper.selectApprovedListBySourceInIds(sourceInIds);
        if (CollUtil.isEmpty(existedItems)) {
            return;
        }
        Map<Long, String> sourceInNoMap = new HashMap<>();
        existedItems.forEach(item -> {
            if (item.getSourceInId() == null) {
                return;
            }
            if (currentInvoiceId != null && currentInvoiceId.equals(item.getInvoiceId())) {
                return;
            }
            sourceInNoMap.putIfAbsent(item.getSourceInId(), item.getSourceInNo());
        });
        if (!sourceInNoMap.isEmpty()) {
            Long conflictSourceInId = sourceInNoMap.keySet().iterator().next();
            throw exception(PURCHASE_INVOICE_SOURCE_IN_INVOICED,
                    sourceInNoMap.getOrDefault(conflictSourceInId, String.valueOf(conflictSourceInId)));
        }
    }

    private void updatePurchaseInvoiceItemList(Long id, List<ErpPurchaseInvoiceItemDO> newList) {
        List<ErpPurchaseInvoiceItemDO> oldList = purchaseInvoiceItemMapper.selectListByInvoiceId(id);
        List<List<ErpPurchaseInvoiceItemDO>> diffList = diffList(oldList, newList,
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(item -> item.setInvoiceId(id));
            purchaseDocumentDefaultService.fillCreateAuditDefaults(diffList.get(0));
            purchaseInvoiceItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            purchaseInvoiceItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            purchaseInvoiceItemMapper.deleteByIds(convertList(diffList.get(2), ErpPurchaseInvoiceItemDO::getId));
        }
    }

    private void replacePurchaseInvoiceItems(Long invoiceId, List<ErpPurchaseInvoiceItemDO> items) {
        purchaseInvoiceItemMapper.deleteByInvoiceId(invoiceId);
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> item.setId(null).setInvoiceId(invoiceId));
        purchaseDocumentDefaultService.fillCreateAuditDefaults(items);
        purchaseInvoiceItemMapper.insertBatch(items);
    }

    private void applyPurchaseInvoiceItemChangeSet(Long invoiceId,
            ErpPurchaseItemOperationHelper.RequestChangeSet<ErpPurchaseInvoiceSaveReqVO.Item> changeSet,
            List<ErpPurchaseInvoiceItemDO> finalItems) {
        if (CollUtil.isNotEmpty(changeSet.getDeleteIds())) {
            purchaseInvoiceItemMapper.deleteByIds(changeSet.getDeleteIds());
        }
        List<ErpPurchaseInvoiceItemDO> insertList = finalItems.stream()
                .filter(item -> item.getId() == null)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(insertList)) {
            insertList.forEach(item -> item.setId(null).setInvoiceId(invoiceId));
            purchaseDocumentDefaultService.fillCreateAuditDefaults(insertList);
            purchaseInvoiceItemMapper.insertBatch(insertList);
        }
        List<ErpPurchaseInvoiceItemDO> updateList = finalItems.stream()
                .filter(item -> item.getId() != null && changeSet.getUpdateIds().contains(item.getId()))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(updateList)) {
            updateList.forEach(item -> item.setInvoiceId(invoiceId));
            purchaseInvoiceItemMapper.updateBatch(updateList);
        }
    }

    private void normalizeInvoiceCount(ErpPurchaseInvoiceDO purchaseInvoice) {
        if (purchaseInvoice.getInvoiceCount() == null || purchaseInvoice.getInvoiceCount() <= 0) {
            purchaseInvoice.setInvoiceCount(1);
        }
    }

    private void calculateTotalPrice(ErpPurchaseInvoiceDO purchaseInvoice, List<ErpPurchaseInvoiceItemDO> items) {
        BigDecimal totalAmount = getSumValue(items,
                ErpPurchaseInvoiceItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO);
        purchaseInvoice.setTaxExclusiveAmount(BigDecimal.ZERO);
        purchaseInvoice.setTaxAmount(BigDecimal.ZERO);
        purchaseInvoice.setTotalAmount(totalAmount);
    }

    private ErpPurchaseInvoiceSaveReqVO buildPurchaseInvoiceImportSaveReq(PurchaseInvoiceImportGroup group,
                                                                          Map<Long, ErpProductRespVO> productVOMap) {
        ErpPurchaseInvoiceImportExcelVO mainRow = group.getMainRow();
        ErpPurchaseInvoiceSaveReqVO saveReqVO = new ErpPurchaseInvoiceSaveReqVO();
        saveReqVO.setSupplierId(group.getSupplier().getId());
        saveReqVO.setInvoiceDate(parseImportDate(mainRow.getInvoiceDate(), LocalDate.now()));
        saveReqVO.setInvoiceType(trimToNull(mainRow.getInvoiceType()));
        saveReqVO.setInvoiceNo(trimToNull(mainRow.getInvoiceNo()));
        saveReqVO.setInvoiceCount(mainRow.getInvoiceCount());
        saveReqVO.setRemark(trimToNull(mainRow.getRemark()));
        saveReqVO.setItems(convertList(group.getRows(), row -> buildPurchaseInvoiceImportItem(row, productVOMap)));
        return saveReqVO;
    }

    private ErpPurchaseInvoiceSaveReqVO.Item buildPurchaseInvoiceImportItem(PurchaseInvoiceImportRow importRow,
                                                                           Map<Long, ErpProductRespVO> productVOMap) {
        ErpPurchaseInvoiceImportExcelVO row = importRow.getRow();
        ErpProductDO product = importRow.getProduct();
        ErpProductRespVO productVO = productVOMap.get(product.getId());
        ErpPurchaseInvoiceSaveReqVO.Item item = new ErpPurchaseInvoiceSaveReqVO.Item();
        item.setSourceInId(importRow.getPurchaseIn() == null ? null : importRow.getPurchaseIn().getId());
        item.setSourceInNo(importRow.getPurchaseIn() == null ? trimToNull(row.getSourceInNo()) : importRow.getPurchaseIn().getNo());
        item.setSourceInItemId(null);
        item.setProductId(product.getId());
        item.setProductCode(product.getCode());
        item.setProductName(product.getName());
        item.setProductBarCode(product.getBarCode());
        item.setProductUnitName(productVO == null ? null : productVO.getUnitName());
        item.setCount(row.getCount());
        item.setProductPrice(row.getProductPrice());
        item.setTaxPercent(null);
        item.setRemark(trimToNull(row.getItemRemark()));
        return item;
    }

    private PurchaseInvoiceSourceRef validateInvoiceSourceRef(ErpPurchaseImportResultRespVO respVO, Integer rowNo,
                                                              String invoiceNo, String productCode,
                                                              ErpPurchaseInvoiceImportExcelVO row,
                                                              Map<String, ErpPurchaseInDO> purchaseInMap) {
        ErpPurchaseInDO purchaseIn = null;
        boolean valid = true;
        String sourceInNo = trimToNull(row.getSourceInNo());
        if (sourceInNo != null) {
            purchaseIn = purchaseInMap.get(sourceInNo);
            if (purchaseIn == null) {
                addImportFailure(respVO, rowNo, invoiceNo, productCode, "来源入库单不存在：" + sourceInNo);
                valid = false;
            } else if (!ErpAuditStatus.APPROVE.getStatus().equals(purchaseIn.getStatus())) {
                addImportFailure(respVO, rowNo, invoiceNo, productCode, "来源入库单未审批：" + sourceInNo);
                valid = false;
            }
        }
        return new PurchaseInvoiceSourceRef(valid, purchaseIn);
    }

    private List<ErpPurchaseInDO> getPurchaseInMapByNos(Set<String> sourceInNos) {
        if (CollUtil.isEmpty(sourceInNos)) {
            return Collections.emptyList();
        }
        return CollUtil.emptyIfNull(purchaseInMapper.selectList(ErpPurchaseInDO::getNo, sourceInNos));
    }

    private Map<String, ErpSupplierDO> buildSupplierMap() {
        Map<String, ErpSupplierDO> map = new LinkedHashMap<>();
        ErpSupplierPageReqVO pageReqVO = new ErpSupplierPageReqVO();
        pageReqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        supplierService.getSupplierPage(pageReqVO).getList().forEach(supplier -> putSupplierKeys(map, supplier));
        return map;
    }

    private ErpSupplierDO resolveSupplier(String supplierName, Map<String, ErpSupplierDO> supplierMap) {
        return supplierMap.get(normalizeKey(supplierName));
    }

    private void putSupplierKeys(Map<String, ErpSupplierDO> map, ErpSupplierDO supplier) {
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

    private static Set<String> extractInvoiceSourceInNos(List<ErpPurchaseInvoiceImportExcelVO> list) {
        Set<String> nos = new LinkedHashSet<>();
        for (ErpPurchaseInvoiceImportExcelVO row : list) {
            if (row == null) {
                continue;
            }
            String no = trimToNull(row.getSourceInNo());
            if (no != null) {
                nos.add(no);
            }
        }
        return nos;
    }

    private boolean isBlankInvoiceImportRow(ErpPurchaseInvoiceImportExcelVO row) {
        return row == null || !hasInvoiceMainFields(row) && !hasInvoiceDetailFields(row);
    }

    private boolean hasInvoiceMainFields(ErpPurchaseInvoiceImportExcelVO row) {
        return StrUtil.isNotBlank(trimToNull(row.getSupplierName()))
                || StrUtil.isNotBlank(trimToNull(row.getInvoiceDate()))
                || StrUtil.isNotBlank(trimToNull(row.getInvoiceType()))
                || StrUtil.isNotBlank(trimToNull(row.getInvoiceNo()))
                || row.getInvoiceCount() != null
                || StrUtil.isNotBlank(trimToNull(row.getRemark()));
    }

    private boolean hasInvoiceDetailFields(ErpPurchaseInvoiceImportExcelVO row) {
        return StrUtil.isNotBlank(trimToNull(row.getSourceInNo()))
                || StrUtil.isNotBlank(trimToNull(row.getProductCode()))
                || StrUtil.isNotBlank(trimToNull(row.getProductName()))
                || StrUtil.isNotBlank(trimToNull(row.getFactoryCode()))
                || row.getCount() != null
                || row.getProductPrice() != null
                || StrUtil.isNotBlank(trimToNull(row.getItemRemark()));
    }

    private String resolveInvoiceImportNo(Integer rowNo, ErpPurchaseInvoiceImportExcelVO row) {
        String invoiceNo = trimToNull(row.getInvoiceNo());
        if (invoiceNo != null) {
            return invoiceNo;
        }
        String supplierName = trimToNull(row.getSupplierName());
        if (supplierName != null) {
            return supplierName;
        }
        return "第" + rowNo + "行";
    }

    private boolean validateImportDate(ErpPurchaseImportResultRespVO respVO, Integer rowNo, String orderNo,
                                       String productCode, String label, String value) {
        if (StrUtil.isBlank(trimToNull(value))) {
            return true;
        }
        try {
            parseImportDate(value, null);
            return true;
        } catch (IllegalArgumentException ignored) {
            addImportFailure(respVO, rowNo, orderNo, productCode, label + "格式不正确，请使用 yyyy-MM-dd");
            return false;
        }
    }

    private LocalDate parseImportDate(String value, LocalDate defaultValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/M/d"),
                DateTimeFormatter.ofPattern("yyyy/M/dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/d"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        }) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format.
            }
        }
        throw new IllegalArgumentException("Invalid date format: " + value);
    }

    private void addImportFailure(ErpPurchaseImportResultRespVO respVO, Integer rowNo, String orderNo,
                                  String productCode, String reason) {
        respVO.getFailureDetails().add(new ErpPurchaseImportResultRespVO.FailureItem(
                rowNo, orderNo, productCode, reason));
        respVO.setFailureCount(respVO.getFailureCount() + 1);
    }

    private static String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isEmpty(trimmed) ? null : trimmed;
    }

    private static class PurchaseInvoiceImportGroup {

        private final Integer rowNo;
        private final ErpPurchaseInvoiceImportExcelVO mainRow;
        private final ErpSupplierDO supplier;
        private final List<PurchaseInvoiceImportRow> rows = new ArrayList<>();

        private PurchaseInvoiceImportGroup(Integer rowNo, ErpPurchaseInvoiceImportExcelVO mainRow, ErpSupplierDO supplier) {
            this.rowNo = rowNo;
            this.mainRow = mainRow;
            this.supplier = supplier;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public ErpPurchaseInvoiceImportExcelVO getMainRow() {
            return mainRow;
        }

        public ErpSupplierDO getSupplier() {
            return supplier;
        }

        public List<PurchaseInvoiceImportRow> getRows() {
            return rows;
        }

    }

    private static class PurchaseInvoiceImportRow {

        private final Integer rowNo;
        private final ErpPurchaseInvoiceImportExcelVO row;
        private final ErpProductDO product;
        private final ErpPurchaseInDO purchaseIn;

        private PurchaseInvoiceImportRow(Integer rowNo, ErpPurchaseInvoiceImportExcelVO row, ErpProductDO product,
                                         ErpPurchaseInDO purchaseIn) {
            this.rowNo = rowNo;
            this.row = row;
            this.product = product;
            this.purchaseIn = purchaseIn;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public ErpPurchaseInvoiceImportExcelVO getRow() {
            return row;
        }

        public ErpProductDO getProduct() {
            return product;
        }

        public ErpPurchaseInDO getPurchaseIn() {
            return purchaseIn;
        }

    }

    private static class PurchaseInvoiceSourceRef {

        private final boolean valid;
        private final ErpPurchaseInDO purchaseIn;

        private PurchaseInvoiceSourceRef(boolean valid, ErpPurchaseInDO purchaseIn) {
            this.valid = valid;
            this.purchaseIn = purchaseIn;
        }

        public boolean isValid() {
            return valid;
        }

        public ErpPurchaseInDO getPurchaseIn() {
            return purchaseIn;
        }

    }

}
