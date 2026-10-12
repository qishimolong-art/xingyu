package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceMixedSourceItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSourceDocumentPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSourceDocumentRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSourceItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceSourceAllocationDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceSourceAllocationMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseInvoiceSourceTypeEnum;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SOURCE_DOCUMENT_NOT_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SOURCE_DOCUMENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SOURCE_ITEM_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SOURCE_ITEM_AMBIGUOUS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SOURCE_OVER_ALLOCATED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SOURCE_SUPPLIER_MISMATCH;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_INVOICE_SOURCE_TYPE_INVALID;

/**
 * 采购票据来源统一处理：入库原价、调价差额、退货负向金额，以及审核后的数量分摊台账。
 */
@Service
public class ErpPurchaseInvoiceSourceService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final Comparator<LocalDateTime> BIZ_TIME_DESC =
            Comparator.nullsLast(Comparator.reverseOrder());
    private static final Comparator<SourceDocumentCandidate> SOURCE_DOCUMENT_ORDER =
            Comparator.comparing(SourceDocumentCandidate::getBizTime, BIZ_TIME_DESC)
                    .thenComparing(SourceDocumentCandidate::getSourceType)
                    .thenComparing(SourceDocumentCandidate::getId, Comparator.reverseOrder());
    private static final Comparator<SourceSnapshot> MIXED_SOURCE_ITEM_ORDER =
            Comparator.comparing(SourceSnapshot::getSourceDocBizTime, BIZ_TIME_DESC)
                    .thenComparing(SourceSnapshot::getSourceType)
                    .thenComparing(SourceSnapshot::getSourceDocId, Comparator.reverseOrder())
                    .thenComparing(SourceSnapshot::getSourceItemId);

    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpPurchasePriceAdjustMapper priceAdjustMapper;
    @Resource
    private ErpPurchasePriceAdjustItemMapper priceAdjustItemMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Resource
    private ErpPurchaseInvoiceSourceAllocationMapper allocationMapper;

    public PageResult<ErpPurchaseInvoiceSourceDocumentRespVO> getSourceDocumentPage(
            ErpPurchaseInvoiceSourceDocumentPageReqVO reqVO) {
        if (reqVO.getSourceType() == null) {
            return getMixedSourceDocumentPage(reqVO);
        }
        ErpPurchaseInvoiceSourceTypeEnum sourceType = validateSourceType(reqVO.getSourceType());
        switch (sourceType) {
            case PURCHASE_IN:
                return getPurchaseInDocumentPage(reqVO);
            case PRICE_ADJUST:
                return getPriceAdjustDocumentPage(reqVO);
            case PURCHASE_RETURN:
                return getPurchaseReturnDocumentPage(reqVO);
            default:
                throw exception(PURCHASE_INVOICE_SOURCE_TYPE_INVALID);
        }
    }

    public PageResult<ErpPurchaseInvoiceItemDO> getSourceItemPage(ErpPurchaseInvoiceSourceItemPageReqVO reqVO) {
        ErpPurchaseInvoiceSourceTypeEnum sourceType = validateSourceType(reqVO.getSourceType());
        List<ErpPurchaseInvoiceItemDO> rows;
        long total;
        switch (sourceType) {
            case PURCHASE_IN: {
                PageResult<ErpPurchaseInItemDO> page = purchaseInItemMapper.selectInvoiceSourcePage(reqVO);
                Map<Long, ErpPurchaseInDO> docMap = toMap(purchaseInMapper.selectBatchIds(reqVO.getSourceDocIds()),
                        ErpPurchaseInDO::getId);
                List<SourceSnapshot> sourceSnapshots = buildInboundSnapshots(page.getList(), docMap, false);
                validateSnapshotDocuments(sourceSnapshots, reqVO.getSupplierId());
                rows = sourceSnapshots.stream()
                        .filter(SourceSnapshot::hasRemaining).map(SourceSnapshot::toInvoiceItem).collect(Collectors.toList());
                total = page.getTotal();
                break;
            }
            case PRICE_ADJUST: {
                PageResult<ErpPurchasePriceAdjustItemDO> page = priceAdjustItemMapper.selectInvoiceSourcePage(reqVO);
                Map<Long, ErpPurchasePriceAdjustDO> docMap = toMap(priceAdjustMapper.selectBatchIds(reqVO.getSourceDocIds()),
                        ErpPurchasePriceAdjustDO::getId);
                List<SourceSnapshot> sourceSnapshots = buildAdjustSnapshots(page.getList(), docMap, false);
                validateSnapshotDocuments(sourceSnapshots, reqVO.getSupplierId());
                rows = sourceSnapshots.stream()
                        .filter(SourceSnapshot::hasRemaining).map(SourceSnapshot::toInvoiceItem).collect(Collectors.toList());
                total = page.getTotal();
                break;
            }
            case PURCHASE_RETURN: {
                PageResult<ErpPurchaseReturnItemDO> page = purchaseReturnItemMapper.selectInvoiceSourcePage(reqVO);
                Map<Long, ErpPurchaseReturnDO> docMap = toMap(purchaseReturnMapper.selectBatchIds(reqVO.getSourceDocIds()),
                        ErpPurchaseReturnDO::getId);
                List<SourceSnapshot> sourceSnapshots = buildReturnSnapshots(page.getList(), docMap, false);
                validateSnapshotDocuments(sourceSnapshots, reqVO.getSupplierId());
                rows = sourceSnapshots.stream()
                        .filter(SourceSnapshot::hasRemaining).map(SourceSnapshot::toInvoiceItem).collect(Collectors.toList());
                total = page.getTotal();
                break;
            }
            default:
                throw exception(PURCHASE_INVOICE_SOURCE_TYPE_INVALID);
        }
        return new PageResult<>(rows, total);
    }

    public PageResult<ErpPurchaseInvoiceItemDO> getMixedSourceItemPage(
            ErpPurchaseInvoiceMixedSourceItemPageReqVO reqVO) {
        Map<Integer, Set<Long>> idsByType = normalizeMixedSourceRefs(reqVO.getSourceRefs());
        int upperBound = pageUpperBound(reqVO.getPageNo(), reqVO.getPageSize());
        List<SourceSnapshot> snapshots = new ArrayList<>();
        long total = 0L;

        Set<Long> purchaseInIds = idsByType.get(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType());
        if (CollUtil.isNotEmpty(purchaseInIds)) {
            Map<Long, ErpPurchaseInDO> docMap = toMap(purchaseInMapper.selectBatchIds(purchaseInIds),
                    ErpPurchaseInDO::getId);
            validateRequestedDocuments(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN, purchaseInIds, docMap);
            validateSourceDocumentCandidates(docMap.values().stream().map(this::toCandidate).collect(Collectors.toList()),
                    reqVO.getSupplierId());
            PageResult<ErpPurchaseInItemDO> page = purchaseInItemMapper.selectInvoiceMixedSourcePage(
                    buildSourceItemPageReq(reqVO, ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN, purchaseInIds, upperBound));
            snapshots.addAll(buildInboundSnapshots(page.getList(), docMap, false));
            total += page.getTotal();
        }

        Set<Long> priceAdjustIds = idsByType.get(ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST.getType());
        if (CollUtil.isNotEmpty(priceAdjustIds)) {
            Map<Long, ErpPurchasePriceAdjustDO> docMap = toMap(priceAdjustMapper.selectBatchIds(priceAdjustIds),
                    ErpPurchasePriceAdjustDO::getId);
            validateRequestedDocuments(ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST, priceAdjustIds, docMap);
            validateSourceDocumentCandidates(docMap.values().stream().map(this::toCandidate).collect(Collectors.toList()),
                    reqVO.getSupplierId());
            PageResult<ErpPurchasePriceAdjustItemDO> page = priceAdjustItemMapper.selectInvoiceMixedSourcePage(
                    buildSourceItemPageReq(reqVO, ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST,
                            priceAdjustIds, upperBound));
            snapshots.addAll(buildAdjustSnapshots(page.getList(), docMap, false));
            total += page.getTotal();
        }

        Set<Long> purchaseReturnIds = idsByType.get(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType());
        if (CollUtil.isNotEmpty(purchaseReturnIds)) {
            Map<Long, ErpPurchaseReturnDO> docMap = toMap(purchaseReturnMapper.selectBatchIds(purchaseReturnIds),
                    ErpPurchaseReturnDO::getId);
            validateRequestedDocuments(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN, purchaseReturnIds, docMap);
            validateSourceDocumentCandidates(docMap.values().stream().map(this::toCandidate).collect(Collectors.toList()),
                    reqVO.getSupplierId());
            PageResult<ErpPurchaseReturnItemDO> page = purchaseReturnItemMapper.selectInvoiceMixedSourcePage(
                    buildSourceItemPageReq(reqVO, ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN,
                            purchaseReturnIds, upperBound));
            snapshots.addAll(buildReturnSnapshots(page.getList(), docMap, false));
            total += page.getTotal();
        }

        List<ErpPurchaseInvoiceItemDO> rows = pageSlice(snapshots.stream()
                .filter(SourceSnapshot::hasRemaining)
                .sorted(MIXED_SOURCE_ITEM_ORDER)
                .map(SourceSnapshot::toInvoiceItem)
                .collect(Collectors.toList()), reqVO.getPageNo(), reqVO.getPageSize());
        return new PageResult<>(rows, total);
    }

    public void mergeSelectedSourceItems(ErpPurchaseInvoiceSaveReqVO reqVO,
                                         List<ErpPurchaseInvoiceItemDO> oldItems) {
        List<ErpPurchaseInvoiceSaveReqVO.SourceRef> refs = normalizeSourceRefs(reqVO);
        if (CollUtil.isEmpty(refs)) {
            return;
        }
        List<SourceSnapshot> snapshots = loadSnapshotsByRefs(refs, false);
        validateSnapshotDocuments(snapshots, reqVO.getSupplierId());
        Map<String, ErpPurchaseInvoiceSaveReqVO.Item> submittedMap = new LinkedHashMap<>();
        List<ErpPurchaseInvoiceSaveReqVO.Item> manualItems = new ArrayList<>();
        for (ErpPurchaseInvoiceSaveReqVO.Item item : CollUtil.emptyIfNull(reqVO.getItems())) {
            normalizeLegacySource(item);
            if (item.getSourceType() == null || item.getSourceItemId() == null) {
                manualItems.add(item);
            } else {
                submittedMap.put(sourceKey(item.getSourceType(), item.getSourceItemId()), item);
            }
        }
        Map<String, ErpPurchaseInvoiceItemDO> oldMap = new LinkedHashMap<>();
        for (ErpPurchaseInvoiceItemDO item : CollUtil.emptyIfNull(oldItems)) {
            normalizeLegacySource(item);
            if (item.getSourceType() != null && item.getSourceItemId() != null) {
                oldMap.put(sourceKey(item.getSourceType(), item.getSourceItemId()), item);
            }
        }
        Set<String> excludedKeys = new LinkedHashSet<>(CollUtil.emptyIfNull(reqVO.getExcludedSourceItemKeys()));
        for (Long legacyId : CollUtil.emptyIfNull(reqVO.getExcludedSourceInItemIds())) {
            if (legacyId != null) {
                excludedKeys.add(sourceKey(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType(), legacyId));
            }
        }
        if (CollUtil.isNotEmpty(oldItems)) {
            mergeAdditionalSourceItems(reqVO, snapshots, submittedMap, oldMap, excludedKeys, refs);
            return;
        }
        List<ErpPurchaseInvoiceSaveReqVO.Item> mergedItems = snapshots.stream()
                .filter(SourceSnapshot::hasRemaining)
                .sorted(Comparator.comparing(SourceSnapshot::getSourceType)
                        .thenComparing(SourceSnapshot::getSourceDocId)
                        .thenComparing(SourceSnapshot::getSourceItemId))
                .filter(snapshot -> !excludedKeys.contains(snapshot.key()))
                .map(snapshot -> snapshot.toSaveItem(submittedMap.get(snapshot.key()), oldMap.get(snapshot.key())))
                .collect(Collectors.toList());
        mergedItems.addAll(manualItems);
        reqVO.setItems(mergedItems);
    }

    /**
     * 已保存票据选择来源时采用增量追加语义：现有明细由 operation 增量协议保留，
     * 服务端只把尚未存在的已选来源明细展开为 insert，避免只保存当前加载页或覆盖旧草稿明细。
     */
    private void mergeAdditionalSourceItems(ErpPurchaseInvoiceSaveReqVO reqVO,
                                            List<SourceSnapshot> snapshots,
                                            Map<String, ErpPurchaseInvoiceSaveReqVO.Item> submittedMap,
                                            Map<String, ErpPurchaseInvoiceItemDO> oldMap,
                                            Set<String> excludedKeys,
                                            List<ErpPurchaseInvoiceSaveReqVO.SourceRef> refs) {
        Set<String> selectedDocumentKeys = refs.stream()
                .map(ref -> sourceKey(ref.getSourceType(), ref.getSourceDocId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ErpPurchaseInvoiceSaveReqVO.Item> incrementalItems = new ArrayList<>();
        for (ErpPurchaseInvoiceSaveReqVO.Item item : CollUtil.emptyIfNull(reqVO.getItems())) {
            normalizeLegacySource(item);
            boolean selectedNewSourceItem = item.getId() == null
                    && item.getSourceType() != null && item.getSourceItemId() != null
                    && selectedDocumentKeys.contains(sourceKey(item.getSourceType(), item.getSourceDocId()));
            if (selectedNewSourceItem) {
                continue; // 由下面的来源快照统一生成，保证未加载页也能完整追加。
            }
            if (StrUtil.isBlank(item.getOperation())) {
                item.setOperation(item.getId() == null ? ErpPurchaseItemOperationHelper.OPERATION_INSERT
                        : ErpPurchaseItemOperationHelper.OPERATION_UPDATE);
            }
            incrementalItems.add(item);
        }
        snapshots.stream()
                .filter(SourceSnapshot::hasRemaining)
                .sorted(Comparator.comparing(SourceSnapshot::getSourceType)
                        .thenComparing(SourceSnapshot::getSourceDocId)
                        .thenComparing(SourceSnapshot::getSourceItemId))
                .filter(snapshot -> !oldMap.containsKey(snapshot.key()))
                .filter(snapshot -> !excludedKeys.contains(snapshot.key()))
                .map(snapshot -> snapshot.toSaveItem(submittedMap.get(snapshot.key()), null))
                .peek(item -> item.setOperation(ErpPurchaseItemOperationHelper.OPERATION_INSERT))
                .forEach(incrementalItems::add);
        reqVO.setItems(incrementalItems);
    }

    public void normalizeAndValidateSourceItems(List<ErpPurchaseInvoiceSaveReqVO.Item> items, Long supplierId) {
        List<ErpPurchaseInvoiceSaveReqVO.Item> sourceItems = CollUtil.emptyIfNull(items).stream()
                .peek(this::normalizeLegacySource)
                .filter(item -> item.getSourceType() != null || item.getSourceItemId() != null)
                .collect(Collectors.toList());
        if (sourceItems.isEmpty()) {
            return;
        }
        List<SourceSnapshot> snapshots = loadSnapshotsByItems(sourceItems, false);
        validateSnapshotDocuments(snapshots, supplierId);
        Map<String, SourceSnapshot> snapshotMap = snapshots.stream()
                .collect(Collectors.toMap(SourceSnapshot::key, Function.identity(), (a, b) -> a));
        Map<String, BigDecimal> requestedCountMap = new HashMap<>();
        for (ErpPurchaseInvoiceSaveReqVO.Item item : sourceItems) {
            String key = sourceKey(item.getSourceType(), item.getSourceItemId());
            SourceSnapshot snapshot = snapshotMap.get(key);
            if (snapshot == null) {
                throw exception(PURCHASE_INVOICE_SOURCE_ITEM_NOT_EXISTS, key);
            }
            requestedCountMap.merge(key, nvl(item.getCount()), BigDecimal::add);
            snapshot.applyCanonical(item);
        }
        for (Map.Entry<String, BigDecimal> entry : requestedCountMap.entrySet()) {
            SourceSnapshot snapshot = snapshotMap.get(entry.getKey());
            if (entry.getValue().compareTo(snapshot.remainingCount) > 0) {
                throw exception(PURCHASE_INVOICE_SOURCE_OVER_ALLOCATED, snapshot.sourceDocNo,
                        snapshot.remainingCount.stripTrailingZeros().toPlainString(),
                        entry.getValue().stripTrailingZeros().toPlainString());
            }
        }
    }

    /**
     * 为已保存的票据明细补齐来源类型名称、仓库、包装数和剩余可开数量等展示字段。
     */
    public void fillSourceDisplayFields(List<ErpPurchaseInvoiceItemDO> items) {
        List<ErpPurchaseInvoiceItemDO> sourceItems = CollUtil.emptyIfNull(items).stream()
                .peek(this::normalizeLegacySource)
                .filter(item -> item.getSourceType() != null && item.getSourceItemId() != null)
                .collect(Collectors.toList());
        if (sourceItems.isEmpty()) {
            return;
        }
        Map<String, SourceSnapshot> snapshotMap = loadSnapshotsByInvoiceItems(sourceItems, false).stream()
                .collect(Collectors.toMap(SourceSnapshot::key, Function.identity(), (a, b) -> a));
        sourceItems.forEach(item -> {
            SourceSnapshot snapshot = snapshotMap.get(sourceKey(item.getSourceType(), item.getSourceItemId()));
            if (snapshot == null) {
                return;
            }
            item.setSourceTypeName(snapshot.sourceTypeName);
            item.setWarehouseId(snapshot.warehouseId);
            item.setPackageQty(snapshot.packageQty);
            item.setSourceTotalCount(snapshot.totalCount);
            item.setSourceRemainingCount(snapshot.remainingCount);
        });
    }

    /**
     * 导入时按“来源类型 + 来源单号 + 产品”解析唯一来源明细，并使用后端金额口径覆盖导入单价。
     */
    public ErpPurchaseInvoiceSaveReqVO.Item resolveImportSourceItem(Integer sourceType, String sourceDocNo,
                                                                    Long productId, BigDecimal count,
                                                                    Long supplierId) {
        ErpPurchaseInvoiceSourceTypeEnum type = validateSourceType(sourceType);
        List<SourceSnapshot> snapshots;
        switch (type) {
            case PURCHASE_IN: {
                ErpPurchaseInDO doc = purchaseInMapper.selectByNo(sourceDocNo);
                if (doc == null) {
                    throw exception(PURCHASE_INVOICE_SOURCE_DOCUMENT_NOT_EXISTS, sourceDocNo);
                }
                snapshots = buildInboundSnapshots(purchaseInItemMapper.selectListByInId(doc.getId()),
                        Collections.singletonMap(doc.getId(), doc), false);
                break;
            }
            case PRICE_ADJUST: {
                ErpPurchasePriceAdjustDO doc = priceAdjustMapper.selectByNo(sourceDocNo);
                if (doc == null) {
                    throw exception(PURCHASE_INVOICE_SOURCE_DOCUMENT_NOT_EXISTS, sourceDocNo);
                }
                snapshots = buildAdjustSnapshots(priceAdjustItemMapper.selectListByAdjustId(doc.getId()),
                        Collections.singletonMap(doc.getId(), doc), false);
                break;
            }
            case PURCHASE_RETURN: {
                ErpPurchaseReturnDO doc = purchaseReturnMapper.selectByNo(sourceDocNo);
                if (doc == null) {
                    throw exception(PURCHASE_INVOICE_SOURCE_DOCUMENT_NOT_EXISTS, sourceDocNo);
                }
                snapshots = buildReturnSnapshots(purchaseReturnItemMapper.selectListByReturnId(doc.getId()),
                        Collections.singletonMap(doc.getId(), doc), false);
                break;
            }
            default:
                throw exception(PURCHASE_INVOICE_SOURCE_TYPE_INVALID);
        }
        validateSnapshotDocuments(snapshots, supplierId);
        List<SourceSnapshot> matches = snapshots.stream()
                .filter(SourceSnapshot::hasRemaining)
                .filter(snapshot -> java.util.Objects.equals(snapshot.productId, productId))
                .collect(Collectors.toList());
        if (matches.isEmpty()) {
            throw exception(PURCHASE_INVOICE_SOURCE_ITEM_NOT_EXISTS, sourceDocNo + "/产品#" + productId);
        }
        if (matches.size() > 1) {
            throw exception(PURCHASE_INVOICE_SOURCE_ITEM_AMBIGUOUS, sourceDocNo);
        }
        SourceSnapshot snapshot = matches.get(0);
        ErpPurchaseInvoiceSaveReqVO.Item result = snapshot.toSaveItem(null, null);
        result.setCount(count);
        return result;
    }

    /**
     * 锁定并按当前已审批来源重新规范票据明细，然后写入审批占用台账。
     *
     * <p>该方法会原地更新 {@code invoiceItems} 中的来源字段、单价和金额，调用方需要在同一事务内
     * 持久化这些明细并据此重算票据主表金额。这样即使来源单据在票据保存后曾反审、修改并重新审批，
     * 最终审批金额仍以当前业务原单为准。</p>
     */
    public void createApprovedAllocations(Long invoiceId, Long supplierId,
                                          List<ErpPurchaseInvoiceItemDO> invoiceItems) {
        List<ErpPurchaseInvoiceItemDO> sourceItems = CollUtil.emptyIfNull(invoiceItems).stream()
                .peek(this::normalizeLegacySource)
                .filter(item -> item.getSourceType() != null && item.getSourceItemId() != null)
                .collect(Collectors.toList());
        if (sourceItems.isEmpty()) {
            return;
        }
        List<SourceSnapshot> snapshots = loadSnapshotsByInvoiceItems(sourceItems, true);
        validateSnapshotDocuments(snapshots, supplierId);
        Map<String, SourceSnapshot> snapshotMap = snapshots.stream()
                .collect(Collectors.toMap(SourceSnapshot::key, Function.identity(), (a, b) -> a));
        Map<String, BigDecimal> requestedCountMap = new LinkedHashMap<>();
        for (ErpPurchaseInvoiceItemDO item : sourceItems) {
            requestedCountMap.merge(sourceKey(item.getSourceType(), item.getSourceItemId()),
                    nvl(item.getCount()), BigDecimal::add);
        }
        for (Map.Entry<String, BigDecimal> entry : requestedCountMap.entrySet()) {
            SourceSnapshot snapshot = snapshotMap.get(entry.getKey());
            if (snapshot == null) {
                throw exception(PURCHASE_INVOICE_SOURCE_ITEM_NOT_EXISTS, entry.getKey());
            }
            if (entry.getValue().compareTo(snapshot.remainingCount) > 0) {
                throw exception(PURCHASE_INVOICE_SOURCE_OVER_ALLOCATED, snapshot.sourceDocNo,
                        snapshot.remainingCount.stripTrailingZeros().toPlainString(),
                        entry.getValue().stripTrailingZeros().toPlainString());
            }
        }
        sourceItems.forEach(item -> snapshotMap.get(sourceKey(item.getSourceType(), item.getSourceItemId()))
                .applyCanonical(item));
        Map<String, ErpPurchaseInvoiceSourceAllocationDO> allocationMap = new LinkedHashMap<>();
        for (ErpPurchaseInvoiceItemDO item : sourceItems) {
            String key = sourceKey(item.getSourceType(), item.getSourceItemId());
            SourceSnapshot snapshot = snapshotMap.get(key);
            ErpPurchaseInvoiceSourceAllocationDO allocation = allocationMap.get(key);
            if (allocation != null) {
                allocation.setAllocatedCount(nvl(allocation.getAllocatedCount()).add(nvl(item.getCount())));
                allocation.setAllocatedAmount(nvl(allocation.getAllocatedAmount()).add(nvl(item.getTotalPrice())));
                continue;
            }
            allocationMap.put(key, ErpPurchaseInvoiceSourceAllocationDO.builder()
                    .invoiceId(invoiceId)
                    .invoiceItemId(item.getId())
                    .sourceType(snapshot.sourceType)
                    .sourceDocId(snapshot.sourceDocId)
                    .sourceDocNo(snapshot.sourceDocNo)
                    .sourceItemId(snapshot.sourceItemId)
                    .sourceTotalCount(snapshot.totalCount)
                    .sourceTotalAmount(snapshot.totalAmount)
                    .allocatedCount(item.getCount())
                    .allocatedAmount(item.getTotalPrice())
                    .amountDirection(snapshot.amountDirection)
                    .legacyFullAllocation(false)
                    .build());
        }
        allocationMapper.insertBatch(new ArrayList<>(allocationMap.values()));
    }

    private PageResult<ErpPurchaseInvoiceSourceDocumentRespVO> getMixedSourceDocumentPage(
            ErpPurchaseInvoiceSourceDocumentPageReqVO reqVO) {
        int upperBound = pageUpperBound(reqVO.getPageNo(), reqVO.getPageSize());
        PageResult<ErpPurchaseInDO> purchaseInPage = purchaseInMapper.selectInvoiceSourcePage(
                buildSourceDocumentPageReq(reqVO, ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN, upperBound));
        PageResult<ErpPurchasePriceAdjustDO> priceAdjustPage = priceAdjustMapper.selectInvoiceSourcePage(
                buildSourceDocumentPageReq(reqVO, ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST, upperBound));
        PageResult<ErpPurchaseReturnDO> purchaseReturnPage = purchaseReturnMapper.selectInvoiceSourcePage(
                buildSourceDocumentPageReq(reqVO, ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN, upperBound));

        List<SourceDocumentCandidate> candidates = new ArrayList<>();
        candidates.addAll(purchaseInPage.getList().stream().map(this::toCandidate).collect(Collectors.toList()));
        candidates.addAll(priceAdjustPage.getList().stream().map(this::toCandidate).collect(Collectors.toList()));
        candidates.addAll(purchaseReturnPage.getList().stream().map(this::toCandidate).collect(Collectors.toList()));
        List<SourceDocumentCandidate> pageCandidates = pageSlice(candidates.stream()
                .sorted(SOURCE_DOCUMENT_ORDER).collect(Collectors.toList()), reqVO.getPageNo(), reqVO.getPageSize());

        Map<Long, ErpPurchaseInDO> purchaseInMap = toMap(purchaseInPage.getList(), ErpPurchaseInDO::getId);
        Map<Long, ErpPurchasePriceAdjustDO> priceAdjustMap = toMap(priceAdjustPage.getList(),
                ErpPurchasePriceAdjustDO::getId);
        Map<Long, ErpPurchaseReturnDO> purchaseReturnMap = toMap(purchaseReturnPage.getList(),
                ErpPurchaseReturnDO::getId);
        Map<String, List<SourceSnapshot>> snapshotsByDocument = loadPageDocumentSnapshots(pageCandidates,
                purchaseInMap, priceAdjustMap, purchaseReturnMap);
        List<ErpPurchaseInvoiceSourceDocumentRespVO> rows = pageCandidates.stream()
                .map(candidate -> buildDocumentResp(candidate.type, candidate.id, candidate.no,
                        candidate.supplierId, candidate.deptId, candidate.bizTime,
                        snapshotsByDocument.get(documentKey(candidate.sourceType, candidate.id))))
                .collect(Collectors.toList());
        long total = purchaseInPage.getTotal() + priceAdjustPage.getTotal() + purchaseReturnPage.getTotal();
        return new PageResult<>(rows, total);
    }

    private Map<String, List<SourceSnapshot>> loadPageDocumentSnapshots(
            List<SourceDocumentCandidate> candidates,
            Map<Long, ErpPurchaseInDO> purchaseInMap,
            Map<Long, ErpPurchasePriceAdjustDO> priceAdjustMap,
            Map<Long, ErpPurchaseReturnDO> purchaseReturnMap) {
        Map<Integer, Set<Long>> idsByType = candidates.stream().collect(Collectors.groupingBy(
                SourceDocumentCandidate::getSourceType, LinkedHashMap::new,
                Collectors.mapping(SourceDocumentCandidate::getId, Collectors.toCollection(LinkedHashSet::new))));
        List<SourceSnapshot> snapshots = new ArrayList<>();
        Set<Long> purchaseInIds = idsByType.get(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType());
        if (CollUtil.isNotEmpty(purchaseInIds)) {
            snapshots.addAll(buildInboundSnapshots(purchaseInItemMapper.selectListByInIds(purchaseInIds),
                    purchaseInMap, false));
        }
        Set<Long> priceAdjustIds = idsByType.get(ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST.getType());
        if (CollUtil.isNotEmpty(priceAdjustIds)) {
            snapshots.addAll(buildAdjustSnapshots(priceAdjustItemMapper.selectListByAdjustIds(priceAdjustIds),
                    priceAdjustMap, false));
        }
        Set<Long> purchaseReturnIds = idsByType.get(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType());
        if (CollUtil.isNotEmpty(purchaseReturnIds)) {
            snapshots.addAll(buildReturnSnapshots(purchaseReturnItemMapper.selectListByReturnIds(purchaseReturnIds),
                    purchaseReturnMap, false));
        }
        return snapshots.stream().collect(Collectors.groupingBy(
                snapshot -> documentKey(snapshot.sourceType, snapshot.sourceDocId)));
    }

    private ErpPurchaseInvoiceSourceDocumentPageReqVO buildSourceDocumentPageReq(
            ErpPurchaseInvoiceSourceDocumentPageReqVO source,
            ErpPurchaseInvoiceSourceTypeEnum sourceType, int upperBound) {
        ErpPurchaseInvoiceSourceDocumentPageReqVO target = new ErpPurchaseInvoiceSourceDocumentPageReqVO();
        target.setPageNo(1);
        target.setPageSize(upperBound);
        target.setKeyword(source.getKeyword());
        target.setSupplierId(source.getSupplierId());
        target.setSourceType(sourceType.getType());
        return target;
    }

    private ErpPurchaseInvoiceSourceItemPageReqVO buildSourceItemPageReq(
            ErpPurchaseInvoiceMixedSourceItemPageReqVO source,
            ErpPurchaseInvoiceSourceTypeEnum sourceType, Collection<Long> sourceDocIds, int upperBound) {
        ErpPurchaseInvoiceSourceItemPageReqVO target = new ErpPurchaseInvoiceSourceItemPageReqVO();
        target.setPageNo(1);
        target.setPageSize(upperBound);
        target.setMask(source.getMask());
        target.setSupplierId(source.getSupplierId());
        target.setSourceType(sourceType.getType());
        target.setSourceDocIds(new ArrayList<>(sourceDocIds));
        return target;
    }

    private Map<Integer, Set<Long>> normalizeMixedSourceRefs(
            List<ErpPurchaseInvoiceMixedSourceItemPageReqVO.SourceRef> sourceRefs) {
        Map<Integer, Set<Long>> idsByType = new LinkedHashMap<>();
        for (ErpPurchaseInvoiceMixedSourceItemPageReqVO.SourceRef ref : CollUtil.emptyIfNull(sourceRefs)) {
            ErpPurchaseInvoiceSourceTypeEnum type = validateSourceType(ref.getSourceType());
            idsByType.computeIfAbsent(type.getType(), key -> new LinkedHashSet<>()).add(ref.getSourceDocId());
        }
        return idsByType;
    }

    private int pageUpperBound(Integer pageNo, Integer pageSize) {
        long upperBound = (long) pageNo * pageSize;
        return (int) Math.min(upperBound, Integer.MAX_VALUE);
    }

    private static <T> List<T> pageSlice(List<T> rows, Integer pageNo, Integer pageSize) {
        long offset = (long) (pageNo - 1) * pageSize;
        if (offset >= rows.size()) {
            return Collections.emptyList();
        }
        int fromIndex = (int) offset;
        int toIndex = (int) Math.min(offset + pageSize, rows.size());
        return new ArrayList<>(rows.subList(fromIndex, toIndex));
    }

    private SourceDocumentCandidate toCandidate(ErpPurchaseInDO doc) {
        return new SourceDocumentCandidate(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN, doc.getId(), doc.getNo(),
                doc.getSupplierId(), doc.getDeptId(), doc.getStatus(), doc.getInTime());
    }

    private SourceDocumentCandidate toCandidate(ErpPurchasePriceAdjustDO doc) {
        return new SourceDocumentCandidate(ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST, doc.getId(), doc.getNo(),
                doc.getSupplierId(), doc.getDeptId(), doc.getStatus(), doc.getAdjustTime());
    }

    private SourceDocumentCandidate toCandidate(ErpPurchaseReturnDO doc) {
        return new SourceDocumentCandidate(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN, doc.getId(), doc.getNo(),
                doc.getSupplierId(), doc.getDeptId(), doc.getStatus(), doc.getReturnTime());
    }

    private void validateSourceDocumentCandidates(List<SourceDocumentCandidate> candidates, Long supplierId) {
        for (SourceDocumentCandidate candidate : candidates) {
            if (!ErpAuditStatus.APPROVE.getStatus().equals(candidate.status)) {
                throw exception(PURCHASE_INVOICE_SOURCE_DOCUMENT_NOT_APPROVE, candidate.no);
            }
            if (supplierId != null && !supplierId.equals(candidate.supplierId)) {
                throw exception(PURCHASE_INVOICE_SOURCE_SUPPLIER_MISMATCH, candidate.no);
            }
        }
    }

    private PageResult<ErpPurchaseInvoiceSourceDocumentRespVO> getPurchaseInDocumentPage(
            ErpPurchaseInvoiceSourceDocumentPageReqVO reqVO) {
        PageResult<ErpPurchaseInDO> page = purchaseInMapper.selectInvoiceSourcePage(reqVO);
        List<Long> ids = page.getList().stream().map(ErpPurchaseInDO::getId).collect(Collectors.toList());
        Map<Long, List<SourceSnapshot>> snapshots = buildInboundSnapshots(
                ids.isEmpty() ? Collections.emptyList() : purchaseInItemMapper.selectListByInIds(ids),
                toMap(page.getList(), ErpPurchaseInDO::getId), false)
                .stream().collect(Collectors.groupingBy(SourceSnapshot::getSourceDocId));
        List<ErpPurchaseInvoiceSourceDocumentRespVO> rows = page.getList().stream()
                .map(doc -> buildDocumentResp(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN, doc.getId(), doc.getNo(),
                        doc.getSupplierId(), doc.getDeptId(), doc.getInTime(), snapshots.get(doc.getId())))
                .collect(Collectors.toList());
        return new PageResult<>(rows, page.getTotal());
    }

    private PageResult<ErpPurchaseInvoiceSourceDocumentRespVO> getPriceAdjustDocumentPage(
            ErpPurchaseInvoiceSourceDocumentPageReqVO reqVO) {
        PageResult<ErpPurchasePriceAdjustDO> page = priceAdjustMapper.selectInvoiceSourcePage(reqVO);
        List<Long> ids = page.getList().stream().map(ErpPurchasePriceAdjustDO::getId).collect(Collectors.toList());
        Map<Long, List<SourceSnapshot>> snapshots = buildAdjustSnapshots(
                ids.isEmpty() ? Collections.emptyList() : priceAdjustItemMapper.selectListByAdjustIds(ids),
                toMap(page.getList(), ErpPurchasePriceAdjustDO::getId), false)
                .stream().collect(Collectors.groupingBy(SourceSnapshot::getSourceDocId));
        List<ErpPurchaseInvoiceSourceDocumentRespVO> rows = page.getList().stream()
                .map(doc -> buildDocumentResp(ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST, doc.getId(), doc.getNo(),
                        doc.getSupplierId(), doc.getDeptId(), doc.getAdjustTime(), snapshots.get(doc.getId())))
                .collect(Collectors.toList());
        return new PageResult<>(rows, page.getTotal());
    }

    private PageResult<ErpPurchaseInvoiceSourceDocumentRespVO> getPurchaseReturnDocumentPage(
            ErpPurchaseInvoiceSourceDocumentPageReqVO reqVO) {
        PageResult<ErpPurchaseReturnDO> page = purchaseReturnMapper.selectInvoiceSourcePage(reqVO);
        List<Long> ids = page.getList().stream().map(ErpPurchaseReturnDO::getId).collect(Collectors.toList());
        Map<Long, List<SourceSnapshot>> snapshots = buildReturnSnapshots(
                ids.isEmpty() ? Collections.emptyList() : purchaseReturnItemMapper.selectListByReturnIds(ids),
                toMap(page.getList(), ErpPurchaseReturnDO::getId), false)
                .stream().collect(Collectors.groupingBy(SourceSnapshot::getSourceDocId));
        List<ErpPurchaseInvoiceSourceDocumentRespVO> rows = page.getList().stream()
                .map(doc -> buildDocumentResp(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN, doc.getId(), doc.getNo(),
                        doc.getSupplierId(), doc.getDeptId(), doc.getReturnTime(), snapshots.get(doc.getId())))
                .collect(Collectors.toList());
        return new PageResult<>(rows, page.getTotal());
    }

    private ErpPurchaseInvoiceSourceDocumentRespVO buildDocumentResp(
            ErpPurchaseInvoiceSourceTypeEnum type, Long id, String no, Long supplierId, Long deptId,
            LocalDateTime bizTime, List<SourceSnapshot> snapshots) {
        List<SourceSnapshot> items = CollUtil.emptyIfNull(snapshots);
        BigDecimal totalCount = items.stream().map(item -> item.totalCount).reduce(ZERO, BigDecimal::add);
        BigDecimal remainingCount = items.stream().map(item -> item.remainingCount).reduce(ZERO, BigDecimal::add);
        BigDecimal totalAmount = items.stream().map(item -> item.proratedAmount(item.remainingCount))
                .reduce(ZERO, BigDecimal::add);
        BigDecimal allocatedCount = totalCount.subtract(remainingCount);
        ErpPurchaseInvoiceSourceDocumentRespVO respVO = new ErpPurchaseInvoiceSourceDocumentRespVO();
        respVO.setSourceType(type.getType());
        respVO.setSourceTypeName(type.getName());
        respVO.setId(id);
        respVO.setNo(no);
        respVO.setSupplierId(supplierId);
        respVO.setDeptId(deptId);
        respVO.setBizTime(bizTime);
        respVO.setTotalCount(totalCount);
        respVO.setTotalAmount(totalAmount);
        respVO.setAllocatedCount(allocatedCount);
        respVO.setRemainingCount(remainingCount);
        respVO.setInvoiceStatus(allocatedCount.compareTo(ZERO) <= 0 ? "未开票"
                : remainingCount.compareTo(ZERO) <= 0 ? "已开票" : "部分开票");
        return respVO;
    }

    private List<ErpPurchaseInvoiceSaveReqVO.SourceRef> normalizeSourceRefs(ErpPurchaseInvoiceSaveReqVO reqVO) {
        Map<String, ErpPurchaseInvoiceSaveReqVO.SourceRef> refs = new LinkedHashMap<>();
        for (ErpPurchaseInvoiceSaveReqVO.SourceRef ref : CollUtil.emptyIfNull(reqVO.getSourceRefs())) {
            if (ref == null || ref.getSourceType() == null || ref.getSourceDocId() == null) {
                continue;
            }
            validateSourceType(ref.getSourceType());
            refs.put(sourceKey(ref.getSourceType(), ref.getSourceDocId()), ref);
        }
        for (Long sourceInId : CollUtil.emptyIfNull(reqVO.getSourceInIds())) {
            if (sourceInId == null) {
                continue;
            }
            ErpPurchaseInvoiceSaveReqVO.SourceRef ref = new ErpPurchaseInvoiceSaveReqVO.SourceRef();
            ref.setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType());
            ref.setSourceDocId(sourceInId);
            refs.putIfAbsent(sourceKey(ref.getSourceType(), ref.getSourceDocId()), ref);
        }
        return new ArrayList<>(refs.values());
    }

    private List<SourceSnapshot> loadSnapshotsByRefs(List<ErpPurchaseInvoiceSaveReqVO.SourceRef> refs, boolean lock) {
        Map<Integer, Set<Long>> idsByType = refs.stream().collect(Collectors.groupingBy(
                ErpPurchaseInvoiceSaveReqVO.SourceRef::getSourceType, TreeMap::new,
                Collectors.mapping(ErpPurchaseInvoiceSaveReqVO.SourceRef::getSourceDocId, Collectors.toCollection(LinkedHashSet::new))));
        List<SourceSnapshot> result = new ArrayList<>();
        for (Map.Entry<Integer, Set<Long>> entry : idsByType.entrySet()) {
            ErpPurchaseInvoiceSourceTypeEnum type = validateSourceType(entry.getKey());
            Set<Long> ids = entry.getValue();
            switch (type) {
                case PURCHASE_IN:
                    Map<Long, ErpPurchaseInDO> inMap = toMap(purchaseInMapper.selectBatchIds(ids), ErpPurchaseInDO::getId);
                    validateRequestedDocuments(type, ids, inMap);
                    result.addAll(buildInboundSnapshots(purchaseInItemMapper.selectListByInIds(ids), inMap, lock));
                    break;
                case PRICE_ADJUST:
                    Map<Long, ErpPurchasePriceAdjustDO> adjustMap = toMap(priceAdjustMapper.selectBatchIds(ids),
                            ErpPurchasePriceAdjustDO::getId);
                    validateRequestedDocuments(type, ids, adjustMap);
                    result.addAll(buildAdjustSnapshots(priceAdjustItemMapper.selectListByAdjustIds(ids), adjustMap, lock));
                    break;
                case PURCHASE_RETURN:
                    Map<Long, ErpPurchaseReturnDO> returnMap = toMap(purchaseReturnMapper.selectBatchIds(ids),
                            ErpPurchaseReturnDO::getId);
                    validateRequestedDocuments(type, ids, returnMap);
                    result.addAll(buildReturnSnapshots(purchaseReturnItemMapper.selectListByReturnIds(ids), returnMap, lock));
                    break;
                default:
                    throw exception(PURCHASE_INVOICE_SOURCE_TYPE_INVALID);
            }
        }
        return result;
    }

    private void validateRequestedDocuments(ErpPurchaseInvoiceSourceTypeEnum type, Collection<Long> ids,
                                            Map<Long, ?> docMap) {
        for (Long id : ids) {
            if (!docMap.containsKey(id)) {
                throw exception(PURCHASE_INVOICE_SOURCE_DOCUMENT_NOT_EXISTS, type.getName() + "#" + id);
            }
        }
    }

    private List<SourceSnapshot> loadSnapshotsByItems(List<ErpPurchaseInvoiceSaveReqVO.Item> items, boolean lock) {
        List<ErpPurchaseInvoiceItemDO> converted = items.stream().map(item -> {
            ErpPurchaseInvoiceItemDO target = new ErpPurchaseInvoiceItemDO();
            target.setSourceType(item.getSourceType());
            target.setSourceItemId(item.getSourceItemId());
            return target;
        }).collect(Collectors.toList());
        return loadSnapshotsByInvoiceItems(converted, lock);
    }

    private List<SourceSnapshot> loadSnapshotsByInvoiceItems(List<ErpPurchaseInvoiceItemDO> items, boolean lock) {
        Map<Integer, Set<Long>> idsByType = items.stream().collect(Collectors.groupingBy(
                ErpPurchaseInvoiceItemDO::getSourceType, TreeMap::new,
                Collectors.mapping(ErpPurchaseInvoiceItemDO::getSourceItemId, Collectors.toCollection(LinkedHashSet::new))));
        List<SourceSnapshot> result = new ArrayList<>();
        for (Map.Entry<Integer, Set<Long>> entry : idsByType.entrySet()) {
            ErpPurchaseInvoiceSourceTypeEnum type = validateSourceType(entry.getKey());
            Set<Long> ids = entry.getValue();
            switch (type) {
                case PURCHASE_IN: {
                    List<ErpPurchaseInItemDO> sourceItems = lock
                            ? purchaseInItemMapper.selectListByIdsForUpdate(ids) : purchaseInItemMapper.selectBatchIds(ids);
                    Set<Long> docIds = sourceItems.stream().map(ErpPurchaseInItemDO::getInId).collect(Collectors.toSet());
                    result.addAll(buildInboundSnapshots(sourceItems,
                            toMap(purchaseInMapper.selectBatchIds(docIds), ErpPurchaseInDO::getId), false));
                    break;
                }
                case PRICE_ADJUST: {
                    List<ErpPurchasePriceAdjustItemDO> sourceItems = lock
                            ? priceAdjustItemMapper.selectListByIdsForUpdate(ids) : priceAdjustItemMapper.selectBatchIds(ids);
                    Set<Long> docIds = sourceItems.stream().map(ErpPurchasePriceAdjustItemDO::getAdjustId).collect(Collectors.toSet());
                    result.addAll(buildAdjustSnapshots(sourceItems,
                            toMap(priceAdjustMapper.selectBatchIds(docIds), ErpPurchasePriceAdjustDO::getId), false));
                    break;
                }
                case PURCHASE_RETURN: {
                    List<ErpPurchaseReturnItemDO> sourceItems = lock
                            ? purchaseReturnItemMapper.selectListByIdsForUpdate(ids) : purchaseReturnItemMapper.selectBatchIds(ids);
                    Set<Long> docIds = sourceItems.stream().map(ErpPurchaseReturnItemDO::getReturnId).collect(Collectors.toSet());
                    result.addAll(buildReturnSnapshots(sourceItems,
                            toMap(purchaseReturnMapper.selectBatchIds(docIds), ErpPurchaseReturnDO::getId), false));
                    break;
                }
                default:
                    throw exception(PURCHASE_INVOICE_SOURCE_TYPE_INVALID);
            }
        }
        return result;
    }

    private List<SourceSnapshot> buildInboundSnapshots(List<ErpPurchaseInItemDO> items,
                                                       Map<Long, ErpPurchaseInDO> docMap, boolean ignored) {
        Map<Long, BigDecimal> allocatedMap = allocationMapper.selectAllocatedCountMap(
                ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType(), ids(items, ErpPurchaseInItemDO::getId));
        return CollUtil.emptyIfNull(items).stream().map(item -> {
            ErpPurchaseInDO doc = docMap.get(item.getInId());
            BigDecimal price = item.getOriginalProductPrice() == null ? nvl(item.getProductPrice())
                    : item.getOriginalProductPrice();
            BigDecimal count = nvl(item.getCount());
            return new SourceSnapshot(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN, item.getInId(),
                    doc == null ? null : doc.getNo(), doc == null ? null : doc.getSupplierId(),
                    doc == null ? null : doc.getDeptId(), doc == null ? null : doc.getStatus(),
                    doc == null ? null : doc.getInTime(), item.getId(),
                    item.getProductId(), item.getWarehouseId(), item.getPackageQty(), count, price,
                    MoneyUtils.priceMultiply(price, count), 1, item.getRemark(), nvl(allocatedMap.get(item.getId())));
        }).collect(Collectors.toList());
    }

    private List<SourceSnapshot> buildAdjustSnapshots(List<ErpPurchasePriceAdjustItemDO> items,
                                                      Map<Long, ErpPurchasePriceAdjustDO> docMap, boolean ignored) {
        Map<Long, BigDecimal> allocatedMap = allocationMapper.selectAllocatedCountMap(
                ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST.getType(), ids(items, ErpPurchasePriceAdjustItemDO::getId));
        return CollUtil.emptyIfNull(items).stream().map(item -> {
            ErpPurchasePriceAdjustDO doc = docMap.get(item.getAdjustId());
            BigDecimal unitDifference = nvl(item.getNewPrice()).subtract(nvl(item.getOldPrice()));
            BigDecimal count = nvl(item.getCount());
            BigDecimal amount = item.getAdjustPrice() == null
                    ? MoneyUtils.priceMultiply(unitDifference, count) : item.getAdjustPrice();
            int direction = amount.compareTo(ZERO) < 0 ? -1 : 1;
            return new SourceSnapshot(ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST, item.getAdjustId(),
                    doc == null ? null : doc.getNo(), doc == null ? null : doc.getSupplierId(),
                    doc == null ? null : doc.getDeptId(), doc == null ? null : doc.getStatus(),
                    doc == null ? null : doc.getAdjustTime(), item.getId(),
                    item.getProductId(), item.getWarehouseId(), item.getPackageQty(), count, unitDifference,
                    amount, direction, null, nvl(allocatedMap.get(item.getId())));
        }).filter(snapshot -> snapshot.totalAmount.compareTo(ZERO) != 0).collect(Collectors.toList());
    }

    private List<SourceSnapshot> buildReturnSnapshots(List<ErpPurchaseReturnItemDO> items,
                                                      Map<Long, ErpPurchaseReturnDO> docMap, boolean ignored) {
        Map<Long, BigDecimal> allocatedMap = allocationMapper.selectAllocatedCountMap(
                ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType(), ids(items, ErpPurchaseReturnItemDO::getId));
        return CollUtil.emptyIfNull(items).stream().map(item -> {
            ErpPurchaseReturnDO doc = docMap.get(item.getReturnId());
            BigDecimal price = nvl(item.getProductPrice()).abs();
            BigDecimal count = nvl(item.getCount());
            BigDecimal amount = MoneyUtils.priceMultiply(price, count).negate();
            return new SourceSnapshot(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN, item.getReturnId(),
                    doc == null ? null : doc.getNo(), doc == null ? null : doc.getSupplierId(),
                    doc == null ? null : doc.getDeptId(), doc == null ? null : doc.getStatus(),
                    doc == null ? null : doc.getReturnTime(), item.getId(),
                    item.getProductId(), item.getWarehouseId(), item.getPackageQty(), count, price,
                    amount, -1, item.getRemark(), nvl(allocatedMap.get(item.getId())));
        }).collect(Collectors.toList());
    }

    private void validateSnapshotDocuments(List<SourceSnapshot> snapshots, Long supplierId) {
        for (SourceSnapshot snapshot : snapshots) {
            if (snapshot.sourceDocNo == null) {
                throw exception(PURCHASE_INVOICE_SOURCE_DOCUMENT_NOT_EXISTS,
                        snapshot.sourceType + "#" + snapshot.sourceDocId);
            }
            if (!ErpAuditStatus.APPROVE.getStatus().equals(snapshot.status)) {
                throw exception(PURCHASE_INVOICE_SOURCE_DOCUMENT_NOT_APPROVE, snapshot.sourceDocNo);
            }
            if (supplierId != null && !supplierId.equals(snapshot.supplierId)) {
                throw exception(PURCHASE_INVOICE_SOURCE_SUPPLIER_MISMATCH, snapshot.sourceDocNo);
            }
        }
    }

    private ErpPurchaseInvoiceSourceTypeEnum validateSourceType(Integer type) {
        ErpPurchaseInvoiceSourceTypeEnum value = ErpPurchaseInvoiceSourceTypeEnum.valueOfType(type);
        if (value == null) {
            throw exception(PURCHASE_INVOICE_SOURCE_TYPE_INVALID);
        }
        return value;
    }

    private void normalizeLegacySource(ErpPurchaseInvoiceSaveReqVO.Item item) {
        if (item == null || item.getSourceType() != null || item.getSourceInItemId() == null) {
            return;
        }
        item.setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType());
        item.setSourceDocId(item.getSourceInId());
        item.setSourceDocNo(item.getSourceInNo());
        item.setSourceItemId(item.getSourceInItemId());
        item.setAmountDirection(1);
    }

    private void normalizeLegacySource(ErpPurchaseInvoiceItemDO item) {
        if (item == null || item.getSourceType() != null || item.getSourceInItemId() == null) {
            return;
        }
        item.setSourceType(ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType());
        item.setSourceDocId(item.getSourceInId());
        item.setSourceDocNo(item.getSourceInNo());
        item.setSourceItemId(item.getSourceInItemId());
        item.setAmountDirection(1);
    }

    private static String sourceKey(Integer sourceType, Long sourceItemId) {
        return sourceType + ":" + sourceItemId;
    }

    private static String documentKey(Integer sourceType, Long sourceDocId) {
        return sourceType + ":" + sourceDocId;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private static <T> Map<Long, T> toMap(Collection<T> rows, Function<T, Long> idGetter) {
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyMap();
        }
        return rows.stream().collect(Collectors.toMap(idGetter, Function.identity(), (a, b) -> a));
    }

    private static <T> Set<Long> ids(Collection<T> rows, Function<T, Long> idGetter) {
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptySet();
        }
        return rows.stream().map(idGetter).filter(id -> id != null).collect(Collectors.toSet());
    }

    private static final class SourceDocumentCandidate {
        private final ErpPurchaseInvoiceSourceTypeEnum type;
        private final Integer sourceType;
        private final Long id;
        private final String no;
        private final Long supplierId;
        private final Long deptId;
        private final Integer status;
        private final LocalDateTime bizTime;

        private SourceDocumentCandidate(ErpPurchaseInvoiceSourceTypeEnum type, Long id, String no,
                                        Long supplierId, Long deptId, Integer status, LocalDateTime bizTime) {
            this.type = type;
            this.sourceType = type.getType();
            this.id = id;
            this.no = no;
            this.supplierId = supplierId;
            this.deptId = deptId;
            this.status = status;
            this.bizTime = bizTime;
        }

        private Integer getSourceType() {
            return sourceType;
        }

        private Long getId() {
            return id;
        }

        private LocalDateTime getBizTime() {
            return bizTime;
        }
    }

    private static final class SourceSnapshot {
        private final Integer sourceType;
        private final String sourceTypeName;
        private final Long sourceDocId;
        private final String sourceDocNo;
        private final Long supplierId;
        private final Long deptId;
        private final Integer status;
        private final LocalDateTime sourceDocBizTime;
        private final Long sourceItemId;
        private final Long productId;
        private final Long warehouseId;
        private final Integer packageQty;
        private final BigDecimal totalCount;
        private final BigDecimal unitPrice;
        private final BigDecimal totalAmount;
        private final Integer amountDirection;
        private final String remark;
        private final BigDecimal remainingCount;

        private SourceSnapshot(ErpPurchaseInvoiceSourceTypeEnum type, Long sourceDocId, String sourceDocNo,
                               Long supplierId, Long deptId, Integer status, LocalDateTime sourceDocBizTime,
                               Long sourceItemId, Long productId,
                               Long warehouseId, Integer packageQty, BigDecimal totalCount, BigDecimal unitPrice,
                               BigDecimal totalAmount, Integer amountDirection, String remark,
                               BigDecimal allocatedCount) {
            this.sourceType = type.getType();
            this.sourceTypeName = type.getName();
            this.sourceDocId = sourceDocId;
            this.sourceDocNo = sourceDocNo;
            this.supplierId = supplierId;
            this.deptId = deptId;
            this.status = status;
            this.sourceDocBizTime = sourceDocBizTime;
            this.sourceItemId = sourceItemId;
            this.productId = productId;
            this.warehouseId = warehouseId;
            this.packageQty = packageQty;
            this.totalCount = nvl(totalCount);
            this.unitPrice = nvl(unitPrice);
            this.totalAmount = nvl(totalAmount);
            this.amountDirection = amountDirection;
            this.remark = remark;
            this.remainingCount = this.totalCount.subtract(nvl(allocatedCount)).max(ZERO);
        }

        private Integer getSourceType() {
            return sourceType;
        }

        private Long getSourceDocId() {
            return sourceDocId;
        }

        private Long getSourceItemId() {
            return sourceItemId;
        }

        private LocalDateTime getSourceDocBizTime() {
            return sourceDocBizTime;
        }

        private boolean hasRemaining() {
            return remainingCount.compareTo(ZERO) > 0;
        }

        private String key() {
            return sourceKey(sourceType, sourceItemId);
        }

        private ErpPurchaseInvoiceItemDO toInvoiceItem() {
            ErpPurchaseInvoiceItemDO item = new ErpPurchaseInvoiceItemDO();
            item.setSourceType(sourceType);
            item.setSourceTypeName(sourceTypeName);
            item.setSourceDocId(sourceDocId);
            item.setSourceDocNo(sourceDocNo);
            item.setSourceItemId(sourceItemId);
            item.setAmountDirection(amountDirection);
            if (ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType().equals(sourceType)) {
                item.setSourceInId(sourceDocId);
                item.setSourceInNo(sourceDocNo);
                item.setSourceInItemId(sourceItemId);
            }
            item.setProductId(productId);
            item.setWarehouseId(warehouseId);
            item.setPackageQty(packageQty);
            item.setCount(remainingCount);
            item.setProductPrice(unitPrice);
            item.setTaxExclusivePrice(proratedAmount(remainingCount));
            item.setTaxPrice(ZERO);
            item.setTotalPrice(item.getTaxExclusivePrice());
            item.setSourceTotalCount(totalCount);
            item.setSourceRemainingCount(remainingCount);
            item.setRemark(remark);
            return item;
        }

        private ErpPurchaseInvoiceSaveReqVO.Item toSaveItem(ErpPurchaseInvoiceSaveReqVO.Item submitted,
                                                            ErpPurchaseInvoiceItemDO oldItem) {
            ErpPurchaseInvoiceSaveReqVO.Item item = new ErpPurchaseInvoiceSaveReqVO.Item();
            item.setId(submitted == null ? null : submitted.getId());
            if (item.getId() == null && oldItem != null) {
                item.setId(oldItem.getId());
            }
            item.setSourceType(sourceType);
            item.setSourceDocId(sourceDocId);
            item.setSourceDocNo(sourceDocNo);
            item.setSourceItemId(sourceItemId);
            item.setAmountDirection(amountDirection);
            if (ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType().equals(sourceType)) {
                item.setSourceInId(sourceDocId);
                item.setSourceInNo(sourceDocNo);
                item.setSourceInItemId(sourceItemId);
            }
            item.setProductId(productId);
            item.setCount(submitted == null ? remainingCount : submitted.getCount());
            item.setProductPrice(unitPrice);
            item.setSourceTotalCount(totalCount);
            item.setSourceRemainingCount(remainingCount);
            item.setRemark(submitted == null ? remark : submitted.getRemark());
            return item;
        }

        private void applyCanonical(ErpPurchaseInvoiceSaveReqVO.Item item) {
            item.setSourceType(sourceType);
            item.setSourceDocId(sourceDocId);
            item.setSourceDocNo(sourceDocNo);
            item.setSourceItemId(sourceItemId);
            item.setAmountDirection(amountDirection);
            item.setProductId(productId);
            item.setProductPrice(unitPrice);
            item.setSourceTotalCount(totalCount);
            item.setSourceRemainingCount(remainingCount);
            if (ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType().equals(sourceType)) {
                item.setSourceInId(sourceDocId);
                item.setSourceInNo(sourceDocNo);
                item.setSourceInItemId(sourceItemId);
            } else {
                item.setSourceInId(null);
                item.setSourceInNo(null);
                item.setSourceInItemId(null);
            }
        }

        private void applyCanonical(ErpPurchaseInvoiceItemDO item) {
            item.setSourceType(sourceType);
            item.setSourceDocId(sourceDocId);
            item.setSourceDocNo(sourceDocNo);
            item.setSourceItemId(sourceItemId);
            item.setAmountDirection(amountDirection);
            item.setProductId(productId);
            item.setProductPrice(unitPrice);
            item.setSourceTotalCount(totalCount);
            item.setSourceRemainingCount(remainingCount);
            if (ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_IN.getType().equals(sourceType)) {
                item.setSourceInId(sourceDocId);
                item.setSourceInNo(sourceDocNo);
                item.setSourceInItemId(sourceItemId);
            } else {
                item.setSourceInId(null);
                item.setSourceInNo(null);
                item.setSourceInItemId(null);
            }
            BigDecimal canonicalAmount = proratedAmount(item.getCount());
            item.setTaxExclusivePrice(canonicalAmount);
            item.setTaxPercent(null);
            item.setTaxPrice(ZERO);
            item.setTotalPrice(canonicalAmount);
        }

        private BigDecimal proratedAmount(BigDecimal count) {
            if (totalCount.compareTo(ZERO) <= 0) {
                return ZERO;
            }
            if (ErpPurchaseInvoiceSourceTypeEnum.PRICE_ADJUST.getType().equals(sourceType)) {
                return MoneyUtils.priceMultiply(unitPrice, count);
            }
            if (ErpPurchaseInvoiceSourceTypeEnum.PURCHASE_RETURN.getType().equals(sourceType)) {
                return MoneyUtils.priceMultiply(unitPrice, count).negate();
            }
            return MoneyUtils.priceMultiply(unitPrice, count);
        }
    }
}
