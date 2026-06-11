package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoicePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_ITEM_PRICE_NEGATIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_NOT_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_PROCESS_NOT_SUPPORT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SOURCE_IN_INVOICED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SUPPLIER_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_PURCHASE_INVOICE_TYPE;

@Service
@Validated
public class ErpPurchaseInvoiceServiceImpl implements ErpPurchaseInvoiceService {

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
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseInvoice(ErpPurchaseInvoiceSaveReqVO createReqVO) {
        validateSupplierExists(createReqVO.getSupplierId());
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
    public void updatePurchaseInvoice(ErpPurchaseInvoiceSaveReqVO updateReqVO) {
        ErpPurchaseInvoiceDO purchaseInvoice = validatePurchaseInvoiceExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseInvoice.getStatus())) {
            throw exception(PURCHASE_INVOICE_UPDATE_FAIL_APPROVE, purchaseInvoice.getNo());
        }
        validateSupplierExists(updateReqVO.getSupplierId());
        List<ErpPurchaseInvoiceItemDO> items = validatePurchaseInvoiceItems(updateReqVO.getItems(), updateReqVO.getId());
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
        updatePurchaseInvoiceItemList(updateReqVO.getId(), items);
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
    public List<ErpPurchaseInvoiceItemDO> getPurchaseInvoiceItemListByInvoiceId(Long invoiceId) {
        return purchaseInvoiceItemMapper.selectListByInvoiceId(invoiceId);
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

    private void validateSupplierExists(Long supplierId) {
        if (supplierId == null) {
            throw exception(PURCHASE_INVOICE_SUPPLIER_REQUIRED);
        }
        supplierService.validateSupplier(supplierId);
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
        List<ErpProductDO> products = productService.validProductList(
                convertSet(list, ErpPurchaseInvoiceSaveReqVO.Item::getProductId));
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

    private void normalizeInvoiceCount(ErpPurchaseInvoiceDO purchaseInvoice) {
        if (purchaseInvoice.getInvoiceCount() == null || purchaseInvoice.getInvoiceCount() <= 0) {
            purchaseInvoice.setInvoiceCount(1);
        }
    }

    private void calculateTotalPrice(ErpPurchaseInvoiceDO purchaseInvoice, List<ErpPurchaseInvoiceItemDO> items) {
        BigDecimal totalAmount = getSumValue(items,
                ErpPurchaseInvoiceItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO);
        purchaseInvoice.setTaxExclusiveAmount(null);
        purchaseInvoice.setTaxAmount(BigDecimal.ZERO);
        purchaseInvoice.setTotalAmount(totalAmount);
    }

}
