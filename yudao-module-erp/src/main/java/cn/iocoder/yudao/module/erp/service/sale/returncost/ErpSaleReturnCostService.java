package cn.iocoder.yudao.module.erp.service.sale.returncost;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.*;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleReturnModeEnum;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 原销售实际过账成本按累计金额冲回，不读取当前均价替代原成本。 */
@Service
public class ErpSaleReturnCostService {
    private static final Object CONTEXT_KEY = new Object();
    private static final String RULE = "SALE_RETURN_ORIGINAL_COST_V1";
    @Value("${erp.reporting.dual-cost-enabled:false}")
    private boolean enabled;
    @Resource private ErpSaleReturnMapper returnMapper;
    @Resource private ErpSaleReturnItemMapper returnItemMapper;
    @Resource private ErpSaleOutMapper saleOutMapper;
    @Resource private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource private ErpStockMapper stockMapper;
    @Resource private ErpWarehouseService warehouseService;
    @Resource private ErpSaleReturnCostRepository repository;
    @Resource private ErpSaleReturnCurrentCostService currentCostService;

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public Map<Long, ErpStockRecordCreateReqBO> prepareApproval(ErpSaleReturnDO header,
                                                              List<ErpSaleReturnItemDO> requestedItems) {
        if (!enabled) {
            return Collections.emptyMap();
        }
        if (ErpSaleReturnModeEnum.isByStock(header.getReturnMode())) {
            return currentCostService.prepareApproval(header, requestedItems);
        }
        requireTransaction();
        long tenant = tenant();
        ErpSaleReturnDO stored = returnMapper.selectByIdForUpdate(header.getId());
        if (stored == null || !ErpAuditStatus.APPROVE.getStatus().equals(stored.getStatus())) {
            throw error("原单退货成本只能在本次已认领审核事务中消费");
        }
        List<ErpSaleReturnItemDO> items = ordered(returnItemMapper.selectListByReturnIdForUpdate(header.getId()));
        if (items.isEmpty() || !sourceSignature(stored, items).equals(sourceSignature(header, requestedItems))) {
            throw error("退货审核来源与锁定完整明细不一致");
        }
        if (!ErpSaleReturnModeEnum.isBySaleOut(stored.getReturnMode()) || stored.getSourceOutId() == null) {
            throw error("无单退货和旧订单退货尚未接入新核算，请等待对应成本确认流程");
        }
        ErpSaleOutDO source = saleOutMapper.selectByIdForUpdate(stored.getSourceOutId());
        if (source == null || !ErpAuditStatus.APPROVE.getStatus().equals(source.getStatus())
                || !Objects.equals(source.getCustomerId(), stored.getCustomerId())) {
            throw error("原销售单不存在、无数据权限、未审核或往来对象不一致");
        }
        Map<Long, ErpSaleOutItemDO> sourceItems = new HashMap<>();
        for (ErpSaleOutItemDO item : saleOutItemMapper.selectListByOutIdForUpdate(source.getId())) {
            sourceItems.put(item.getId(), item);
        }
        if (repository.hasUnmappedApprovedReturn(tenant, source.getId(), stored.getId())) {
            throw error("原销售存在未关联新成本分摊的历史已审核退货，请先核对历史差异");
        }
        validateReceivingStocks(items);
        // 在库存锁之前读取最新累计；其他退货可能已在本事务旧RR快照之后提交。
        int expectedReturnStatus = calculateReturnStatus(tenant, source.getId(), sourceItems.values(), items);

        // 同一原销售父锁串行全部退货；原事件再按来源明细排序锁定，不在库存锁后取来源锁。
        Map<Long, Map<String, Object>> originalByItem = new TreeMap<>();
        for (Long sourceItemId : items.stream().map(ErpSaleReturnItemDO::getSourceOutItemId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new))) {
            List<Map<String, Object>> postings = repository.lockSourcePostings(tenant, source.getId(), sourceItemId);
            if (postings.size() != 1) {
                throw error("原销售成本缺失或存在尚未映射的多次出库事件，禁止猜测成本");
            }
            Map<String, Object> original = postings.get(0);
            ErpSaleOutItemDO sourceItem = sourceItems.get(sourceItemId);
            if (sourceItem == null || !Objects.equals(sourceItem.getProductId(), number(original, "product_id"))
                    || !Objects.equals(sourceItem.getWarehouseId(), number(original, "warehouse_id"))
                    || !equal(sourceItem.getCount(), decimal(original, "quantity").negate())
                    || decimal(original, "quantity").signum() >= 0
                    || decimal(original, "financial_movement").signum() > 0
                    || decimal(original, "settlement_movement").signum() > 0
                    || number(original, "accounting_dept_id") == null || number(original, "accounting_dept_id") <= 0) {
                throw error("原销售行与成本事件的商品、仓库、方向或核算归属不一致");
            }
            originalByItem.put(sourceItemId, original);
        }

        Map<Long, Totals> totalsBySource = new HashMap<>();
        Map<Long, String> signatureBySource = new HashMap<>();
        Map<String, Prepared> context = context();
        for (Map<String, Object> original : originalByItem.values()) {
            long originalId = number(original, "id");
            if (context.values().stream().anyMatch(p -> p.sourceId == originalId && !p.posted)) {
                throw error("本事务还有未完成的同来源退货过账，不能重复准备成本");
            }
            BigDecimal quantity = decimal(original, "quantity").negate();
            BigDecimal financial = decimal(original, "financial_movement").negate();
            BigDecimal settlement = decimal(original, "settlement_movement").negate();
            repository.execute("INSERT IGNORE INTO erp_sale_return_cost_progress"
                            + "(tenant_id,source_posting_id,original_quantity,original_financial_amount,original_settlement_amount,"
                            + "returned_quantity,returned_financial_amount,returned_settlement_amount,rule_version,updated_at)"
                            + " VALUES (?,?,?,?,?,0,0,0,?,?)", tenant, originalId, quantity, financial, settlement, RULE, LocalDateTime.now());
            Map<String, Object> progress = repository.lockProgress(tenant, originalId);
            String signature = originalSignature(original);
            Totals previous = verifyProgress(tenant, originalId, original, progress, signature);
            totalsBySource.put(originalId, previous);
            signatureBySource.put(originalId, signature);
        }

        Map<Long, ErpStockRecordCreateReqBO> result = new LinkedHashMap<>();
        for (ErpSaleReturnItemDO item : items) {
            Map<String, Object> original = originalByItem.get(item.getSourceOutItemId());
            if (original == null || !Objects.equals(item.getProductId(), number(original, "product_id"))
                    || item.getCount() == null || item.getCount().signum() <= 0) {
                throw error("退货明细缺少真实原销售行、商品不一致或数量无效");
            }
            long originalId = number(original, "id");
            Totals previous = totalsBySource.get(originalId);
            BigDecimal quantity = decimal(original, "quantity").negate();
            BigDecimal financial = ErpSaleReturnCostCalculator.allocate(quantity, decimal(original, "financial_movement").negate(),
                    previous.quantity, previous.financial, item.getCount());
            BigDecimal settlement = ErpSaleReturnCostCalculator.allocate(quantity, decimal(original, "settlement_movement").negate(),
                    previous.quantity, previous.settlement, item.getCount());
            previous.add(item.getCount(), financial, settlement);
            ErpStockRecordCreateReqBO bo = new ErpStockRecordCreateReqBO(item.getProductId(), item.getWarehouseId(),
                    item.getBatchNo(), item.getCount(), 60, stored.getId(), item.getId(), stored.getNo(),
                    item.getProductPrice(), stored.getReturnTime());
            bo.setAccountingDeptId(number(original, "accounting_dept_id"));
            bo.setFinancialMovementAmount(financial).setSettlementMovementAmount(settlement);
            bo.setSourceBizType(50).setSourceBizId(source.getId()).setSourceBizItemId(item.getSourceOutItemId());
            bo.setReversalPostingId(originalId).setSourcePriceBasis("INCLUSIVE_UNCONFIRMED");
            Prepared prepared = new Prepared(tenant, originalId, signatureBySource.get(originalId), postingSignature(bo),
                    expectedReturnStatus);
            if (context.putIfAbsent(key(bo), prepared) != null) {
                throw error("退货明细已在当前事务准备，不能重复消费");
            }
            result.put(item.getId(), bo);
        }
        final long returnId = stored.getId();
        final int expected = items.size();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void beforeCommit(boolean readOnly) {
                if (repository.allocationCount(tenant, returnId) != expected
                        || result.values().stream().anyMatch(bo -> !context.get(key(bo)).posted)) {
                    throw error("退货未完成全部库存和成本分摊，整单回滚");
                }
                for (Map<String, Object> original : originalByItem.values()) {
                    long originalId = number(original, "id");
                    verifyProgress(tenant, originalId, original, repository.lockProgress(tenant, originalId),
                            signatureBySource.get(originalId));
                }
            }
        });
        return result;
    }

    /** 只有当前审核已完成全部实际库存过账，才能使用准备时锁定的累计状态。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public Integer getPreparedReturnStatus(Long returnId) {
        if (!enabled) {
            return null;
        }
        requireTransaction();
        Integer result = null;
        for (Map.Entry<String, Prepared> entry : context().entrySet()) {
            if (!entry.getKey().startsWith(returnId + ":")) {
                continue;
            }
            Prepared prepared = entry.getValue();
            if (prepared.tenant != tenant() || !prepared.posted
                    || (result != null && result != prepared.expectedReturnStatus)) {
                throw error("退货状态只能在本次完整库存过账后确认");
            }
            result = prepared.expectedReturnStatus;
        }
        if (result == null) {
            throw error("缺少当前审核事务的退货状态依据");
        }
        return result;
    }

    private int calculateReturnStatus(long tenant, long outId, Collection<ErpSaleOutItemDO> sourceItems,
                                     List<ErpSaleReturnItemDO> returnItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpSaleOutItemDO sourceItem : sourceItems) {
            if (sourceItem.getCount() == null || sourceItem.getCount().signum() <= 0) {
                throw error("原销售数量不完整，无法确定退货状态");
            }
            total = total.add(sourceItem.getCount());
        }
        BigDecimal returned = BigDecimal.ZERO;
        for (Map<String, Object> row : repository.lockReturnStatusAllocations(tenant, outId)) {
            BigDecimal quantity = decimal(row, "quantity");
            if (row.get("return_posting_id") == null || quantity.signum() <= 0
                    || !Objects.equals(number(row, "actual_type"), 60L)
                    || !Objects.equals(number(row, "actual_source_id"), number(row, "original_posting_id"))
                    || !equal(quantity, decimal(row, "actual_quantity"))) {
                throw error("退货累计状态与实际成本事件不一致");
            }
            returned = returned.add(quantity);
        }
        for (ErpSaleReturnItemDO item : returnItems) {
            if (item.getCount() == null || item.getCount().signum() <= 0) {
                throw error("退货数量无效");
            }
            returned = returned.add(item.getCount());
        }
        if (returned.compareTo(total) > 0) {
            throw error("累计退货超过原销售数量");
        }
        return returned.compareTo(total) == 0 ? 2 : 1;
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void validatePosting(ErpStockRecordCreateReqBO request) {
        if (currentCostService.hasPrepared(request)) {
            currentCostService.validatePosting(request);
            return;
        }
        prepared(request);
    }

    /** 必须依据实际事务上下文，外部BO无权选择成本规则。 */
    public boolean isCurrentCostPosting(ErpStockRecordCreateReqBO request) {
        if (!currentCostService.hasPrepared(request)) { return false; }
        currentCostService.validatePosting(request);
        return true;
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void appendAllocation(long postingId, LocalDateTime postedAt, ErpStockRecordCreateReqBO request) {
        if (currentCostService.hasPrepared(request)) {
            currentCostService.appendPosting(postingId, postedAt, request);
            return;
        }
        Prepared prepared = prepared(request);
        if (prepared.posted) {
            throw error("同一退货成本分摊已追加，不能重复写入");
        }
        Map<String, Object> posting = repository.posting(tenant(), postingId);
        if (posting == null || !Objects.equals(number(posting, "biz_id"), request.getBizId())
                || !Objects.equals(number(posting, "biz_item_id"), request.getBizItemId())
                || !Objects.equals(number(posting, "reversal_posting_id"), request.getReversalPostingId())
                || !Objects.equals(number(posting, "biz_type"), 60L)
                || !Objects.equals(number(posting, "product_id"), request.getProductId())
                || !Objects.equals(number(posting, "warehouse_id"), request.getWarehouseId())
                || !Objects.equals(number(posting, "accounting_dept_id"), request.getAccountingDeptId())
                || !Objects.equals(number(posting, "source_biz_id"), request.getSourceBizId())
                || !Objects.equals(number(posting, "source_biz_type"), 50L)
                || !Objects.equals(number(posting, "source_biz_item_id"), request.getSourceBizItemId())
                || !Objects.equals(dateTime(posting.get("posted_at")), postedAt)
                || !equal(decimal(posting, "quantity"), request.getCount())
                || !equal(decimal(posting, "financial_movement"), request.getFinancialMovementAmount())
                || !equal(decimal(posting, "settlement_movement"), request.getSettlementMovementAmount())) {
            throw error("退货成本分摊必须关联本次真实库存过账");
        }
        int inserted = repository.execute("INSERT INTO erp_sale_return_cost_allocation"
                        + "(tenant_id,source_posting_id,return_id,return_item_id,return_posting_id,quantity,financial_amount,"
                        + "settlement_amount,source_signature,posted_at,rule_version) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                tenant(), prepared.sourceId, request.getBizId(), request.getBizItemId(), postingId, request.getCount(),
                request.getFinancialMovementAmount(), request.getSettlementMovementAmount(), prepared.sourceSignature, postedAt, RULE);
        int updated = repository.execute("UPDATE erp_sale_return_cost_progress SET returned_quantity=returned_quantity+?,"
                        + "returned_financial_amount=returned_financial_amount+?,returned_settlement_amount=returned_settlement_amount+?,updated_at=?"
                        + " WHERE tenant_id=? AND source_posting_id=?",
                request.getCount(), request.getFinancialMovementAmount(), request.getSettlementMovementAmount(), postedAt, tenant(), prepared.sourceId);
        if (inserted != 1 || updated != 1) {
            throw error("退货成本进度追加异常，整单回滚");
        }
        prepared.posted = true;
    }

    private Totals verifyProgress(long tenant, long originalId, Map<String, Object> original,
                                  Map<String, Object> progress, String signature) {
        if (progress == null || !RULE.equals(progress.get("rule_version"))
                || !equal(decimal(progress, "original_quantity"), decimal(original, "quantity").negate())
                || !equal(decimal(progress, "original_financial_amount"), decimal(original, "financial_movement").negate())
                || !equal(decimal(progress, "original_settlement_amount"), decimal(original, "settlement_movement").negate())) {
            throw error("退货成本进度与原销售事件不一致");
        }
        Totals actual = new Totals();
        for (Map<String, Object> allocation : repository.lockAllocations(tenant, originalId)) {
            if (decimal(allocation, "quantity").signum() <= 0
                    || decimal(allocation, "financial_amount").signum() < 0
                    || decimal(allocation, "settlement_amount").signum() < 0
                    || allocation.get("actual_posting_id") == null || !Objects.equals(number(allocation, "actual_biz_type"), 60L)
                    || !Objects.equals(number(allocation, "actual_biz_id"), number(allocation, "return_id"))
                    || !Objects.equals(number(allocation, "actual_item_id"), number(allocation, "return_item_id"))
                    || !Objects.equals(number(allocation, "actual_source_id"), originalId)
                    || !Objects.equals(number(allocation, "actual_product_id"), number(original, "product_id"))
                    || !Objects.equals(number(allocation, "actual_accounting_dept_id"), number(original, "accounting_dept_id"))
                    || !Objects.equals(number(allocation, "actual_source_biz_type"), 50L)
                    || !Objects.equals(number(allocation, "actual_source_biz_id"), number(original, "biz_id"))
                    || !Objects.equals(number(allocation, "actual_source_item_id"), number(original, "biz_item_id"))
                    || !signature.equals(allocation.get("source_signature")) || !RULE.equals(allocation.get("rule_version"))
                    || !equal(decimal(allocation, "quantity"), decimal(allocation, "actual_quantity"))
                    || !equal(decimal(allocation, "financial_amount"), decimal(allocation, "actual_financial"))
                    || !equal(decimal(allocation, "settlement_amount"), decimal(allocation, "actual_settlement"))) {
                throw error("退货成本分摊与实际过账或原销售签名不一致");
            }
            actual.add(decimal(allocation, "quantity"), decimal(allocation, "financial_amount"), decimal(allocation, "settlement_amount"));
        }
        if (!equal(actual.quantity, decimal(progress, "returned_quantity"))
                || !equal(actual.financial, decimal(progress, "returned_financial_amount"))
                || !equal(actual.settlement, decimal(progress, "returned_settlement_amount"))
                || actual.quantity.signum() < 0 || actual.quantity.compareTo(decimal(progress, "original_quantity")) > 0) {
            throw error("退货成本进度与累计分摊不一致，请先核对");
        }
        BigDecimal originalQuantity = decimal(progress, "original_quantity");
        BigDecimal expectedFinancial = decimal(progress, "original_financial_amount").multiply(actual.quantity)
                .divide(originalQuantity, 6, java.math.RoundingMode.HALF_UP);
        BigDecimal expectedSettlement = decimal(progress, "original_settlement_amount").multiply(actual.quantity)
                .divide(originalQuantity, 6, java.math.RoundingMode.HALF_UP);
        if (!equal(expectedFinancial, actual.financial) || !equal(expectedSettlement, actual.settlement)) {
            throw error("累计退货成本不符合原销售累计分摊公式，请先核对");
        }
        return actual;
    }

    private void validateReceivingStocks(List<ErpSaleReturnItemDO> items) {
        Set<Long> products = items.stream().map(ErpSaleReturnItemDO::getProductId).collect(Collectors.toSet());
        Set<Long> warehouses = items.stream().map(ErpSaleReturnItemDO::getWarehouseId).collect(Collectors.toSet());
        Set<String> required = items.stream().map(i -> i.getProductId() + ":" + i.getWarehouseId()).collect(Collectors.toSet());
        Map<String, ErpStockDO> stocks = new HashMap<>();
        for (ErpStockDO stock : DataPermissionUtils.executeIgnore(() -> stockMapper.selectListByProductIdsAndWarehouseIds(products, warehouses))) {
            String key = stock.getProductId() + ":" + stock.getWarehouseId();
            if (required.contains(key) && stocks.put(key, stock) != null) {
                throw error("退入商品仓库存在重复库存维度，请先核对");
            }
        }
        if (!stocks.keySet().equals(required)) {
            throw error("退入库存尚未建立或无可靠核算起点");
        }
        warehouseService.validateCurrentUserStockPermission(stocks.values());
    }

    private Prepared prepared(ErpStockRecordCreateReqBO request) {
        requireTransaction();
        @SuppressWarnings("unchecked") Map<String, Prepared> values =
                (Map<String, Prepared>) TransactionSynchronizationManager.getResource(CONTEXT_KEY);
        Prepared prepared = values == null ? null : values.get(key(request));
        if (prepared == null || prepared.tenant != tenant() || !prepared.signature.equals(postingSignature(request))
                || (request.getPostingActionKey() != null && !"APPROVE".equals(request.getPostingActionKey()))) {
            throw error("退货成本没有当前审核事务的真实原单授权，不能凭历史引用直接过账");
        }
        return prepared;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Prepared> context() {
        Map<String, Prepared> existing = (Map<String, Prepared>) TransactionSynchronizationManager.getResource(CONTEXT_KEY);
        if (existing != null) {
            return existing;
        }
        Map<String, Prepared> created = new HashMap<>();
        TransactionSynchronizationManager.bindResource(CONTEXT_KEY, created);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void suspend() { TransactionSynchronizationManager.unbindResourceIfPossible(CONTEXT_KEY); }
            @Override public void resume() { TransactionSynchronizationManager.bindResource(CONTEXT_KEY, created); }
            @Override public void afterCompletion(int status) { TransactionSynchronizationManager.unbindResourceIfPossible(CONTEXT_KEY); }
        });
        return created;
    }

    static String sourceSignature(ErpSaleReturnDO header, List<ErpSaleReturnItemDO> items) {
        List<Object> values = new ArrayList<>(Arrays.asList(header.getId(), header.getNo(), header.getReturnMode(),
                header.getCustomerId(), header.getSourceOutId(), header.getDeptId(), header.getReturnTime(),
                header.getTotalPrice(), header.getDiscountPrice(), header.getFeeAmount(), header.getFreightAmount()));
        for (ErpSaleReturnItemDO item : ordered(items)) {
            Collections.addAll(values, item.getId(), item.getReturnId(), item.getSourceOutItemId(), item.getProductId(),
                    item.getWarehouseId(), item.getDeptId(), item.getBatchNo(), item.getCount(), item.getProductPrice(), item.getTotalPrice());
        }
        return hash(values);
    }

    private static String originalSignature(Map<String, Object> original) {
        return hash(Arrays.asList(original.get("tenant_id"), original.get("id"), original.get("request_hash"),
                original.get("biz_id"), original.get("biz_item_id"), original.get("product_id"), original.get("warehouse_id"),
                original.get("accounting_dept_id"), original.get("quantity"), original.get("financial_movement"), original.get("settlement_movement")));
    }

    private static String postingSignature(ErpStockRecordCreateReqBO request) {
        return hash(Arrays.asList(request.getBizType(), request.getBizId(), request.getBizItemId(), request.getProductId(),
                request.getWarehouseId(), request.getCount(), request.getAccountingDeptId(), request.getFinancialMovementAmount(),
                request.getSettlementMovementAmount(), request.getSourceBizType(), request.getSourceBizId(),
                request.getSourceBizItemId(), request.getReversalPostingId(), request.getBizNo(), request.getBizDate(), request.getBatchNo()));
    }

    private static List<ErpSaleReturnItemDO> ordered(List<ErpSaleReturnItemDO> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        List<ErpSaleReturnItemDO> result = new ArrayList<>(items);
        result.sort(Comparator.comparing(ErpSaleReturnItemDO::getId));
        return result;
    }

    private static String key(ErpStockRecordCreateReqBO request) { return request.getBizId() + ":" + request.getBizItemId(); }
    private static long tenant() { return TenantContextHolder.getRequiredTenantId(); }
    private static LocalDateTime dateTime(Object value) {
        return value instanceof java.sql.Timestamp ? ((java.sql.Timestamp) value).toLocalDateTime() : (LocalDateTime) value;
    }
    private static boolean equal(BigDecimal first, BigDecimal second) { return first != null && second != null && first.compareTo(second) == 0; }
    private static Long number(Map<String, Object> row, String field) { return row.get(field) == null ? null : ((Number) row.get(field)).longValue(); }
    private static BigDecimal decimal(Map<String, Object> row, String field) {
        if (row.get(field) == null) { throw error("原成本核算字段缺失，禁止按零处理"); }
        return new BigDecimal(row.get(field).toString());
    }
    private static void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) { throw error("退货成本必须处于审核事务"); }
    }
    private static ServiceException error(String message) { return new ServiceException(409, message); }
    private static String hash(List<Object> values) {
        StringBuilder canonical = new StringBuilder();
        for (Object value : values) {
            String text = value == null ? "" : value instanceof BigDecimal ? ((BigDecimal) value).stripTrailingZeros().toPlainString() : value.toString();
            canonical.append(value == null ? -1 : text.length()).append(':').append(text);
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) { hex.append(String.format("%02x", value & 255)); }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
    private static final class Prepared {
        private final long tenant, sourceId;
        private final String sourceSignature, signature;
        private final int expectedReturnStatus;
        private boolean posted;
        private Prepared(long tenant, long sourceId, String sourceSignature, String signature, int expectedReturnStatus) {
            this.tenant = tenant; this.sourceId = sourceId; this.sourceSignature = sourceSignature; this.signature = signature;
            this.expectedReturnStatus = expectedReturnStatus;
        }
    }
    private static final class Totals {
        private BigDecimal quantity = BigDecimal.ZERO, financial = BigDecimal.ZERO, settlement = BigDecimal.ZERO;
        private void add(BigDecimal quantity, BigDecimal financial, BigDecimal settlement) {
            this.quantity = this.quantity.add(quantity);
            this.financial = this.financial.add(financial);
            this.settlement = this.settlement.add(settlement);
        }
    }
}
