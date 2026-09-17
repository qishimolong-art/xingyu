package cn.iocoder.yudao.module.erp.service.stock.cost;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 新口径库存过账边界。生产默认关闭；先核对期初，补齐各业务适配后才允许启用。
 * 新账和旧库存流水使用调用方同一事务，锁顺序固定为实物库存、成本余额。
 */
@Service
public class ErpDualCostPostingService {
    @Value("${erp.reporting.dual-cost-enabled:false}")
    private boolean enabled;
    @Value("${erp.reporting.dual-cost-cutover:}")
    private String cutover;
    @Resource
    private ErpDualCostLedgerRepository repository;
    @Resource
    private cn.iocoder.yudao.module.erp.service.purchase.cost.ErpPurchaseCostConfirmationService purchaseCostConfirmationService;
    @Resource
    private cn.iocoder.yudao.module.erp.service.sale.returncost.ErpSaleReturnCostService saleReturnCostService;
    @Resource
    private cn.iocoder.yudao.module.erp.service.purchase.returncost.ErpPurchaseReturnCostService purchaseReturnCostService;
    @Resource
    private cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeSnapshotService tradeSnapshotService;
    private static final Object LEGACY_CONTEXT = new Object();
    private static final String PENDING_POSTING_RULE = "PENDING_TRANSACTION_COMPLETION";

    public void assertLegacyMutationAllowed(boolean quantityPosting) {
        if (enabled && !(quantityPosting && currentLegacyContext() != null)) {
            throw new IllegalStateException("双成本核算已启用，该库存操作尚未接入统一过账，请使用已适配业务");
        }
    }

    /** 仅当前认证写入回调可启用80旧库存均价扣减，不接受外部BO标志。 */
    public boolean useLegacyCurrentAverage(Long productId, Long warehouseId, BigDecimal quantity, Integer bizType) {
        LegacyContext context = currentLegacyContext();
        return enabled && Integer.valueOf(80).equals(bizType) && context != null
                && context.matches(productId, warehouseId, quantity, bizType);
    }

    /** 无单退货兼容入库使用实际核准财务发生额，不使用退款单价；金额来自本次真实writer计算。 */
    public BigDecimal authenticatedLegacyIncomingAmount(Long productId, Long warehouseId, BigDecimal quantity, Integer bizType) {
        LegacyContext context = currentLegacyContext();
        return enabled && context != null && context.matches(productId, warehouseId, quantity, bizType)
                ? context.incomingFinancialAmount : null;
    }

    public void assertLegacyStockMutationAllowed(Long productId, Long warehouseId, BigDecimal quantity, Integer bizType) {
        assertLegacyMutationAllowed(true);
        if (enabled && !currentLegacyContext().matches(productId, warehouseId, quantity, bizType)) {
            throw new IllegalStateException("旧库存写入必须与本次认证过账的维度及数量一致");
        }
    }

    private static LegacyContext currentLegacyContext() {
        Object context = TransactionSynchronizationManager.getResource(LEGACY_CONTEXT);
        return TransactionSynchronizationManager.isActualTransactionActive() && context instanceof LegacyContext
                && ((LegacyContext) context).active
                && ((LegacyContext) context).tenant == TenantContextHolder.getRequiredTenantId() ? (LegacyContext) context : null;
    }

    private void writeLegacy(ErpStockRecordCreateReqBO request, BigDecimal incomingFinancialAmount, Runnable writer) {
        if (currentLegacyContext() != null) {
            throw new IllegalStateException("不支持在旧库存回调中嵌套新的库存过账");
        }
        LegacyContext context = new LegacyContext(request, incomingFinancialAmount);
        TransactionSynchronizationManager.bindResource(LEGACY_CONTEXT, context);
        TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void suspend() {
                if (TransactionSynchronizationManager.getResource(LEGACY_CONTEXT) == context) {
                    TransactionSynchronizationManager.unbindResource(LEGACY_CONTEXT);
                }
            }
            @Override public void resume() {
                if (context.active) {
                    TransactionSynchronizationManager.bindResource(LEGACY_CONTEXT, context);
                }
            }
            @Override public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.getResource(LEGACY_CONTEXT) == context) {
                    TransactionSynchronizationManager.unbindResource(LEGACY_CONTEXT);
                }
            }
        });
        try {
            writer.run();
        } finally {
            context.active = false;
            TransactionSynchronizationManager.unbindResourceIfPossible(LEGACY_CONTEXT);
        }
    }

    private static final class LegacyContext {
        private final Long productId, warehouseId;
        private final Integer bizType;
        private final BigDecimal quantity;
        private final BigDecimal incomingFinancialAmount;
        private final long tenant = TenantContextHolder.getRequiredTenantId();
        private boolean active = true;
        private LegacyContext(ErpStockRecordCreateReqBO request, BigDecimal incomingFinancialAmount) {
            productId = request.getProductId();
            warehouseId = request.getWarehouseId();
            bizType = request.getBizType();
            quantity = request.getCount();
            this.incomingFinancialAmount = incomingFinancialAmount;
        }
        private boolean matches(Long productId, Long warehouseId, BigDecimal quantity, Integer bizType) {
            return Objects.equals(this.productId, productId) && Objects.equals(this.warehouseId, warehouseId)
                    && Objects.equals(this.bizType, bizType) && quantity != null && this.quantity.compareTo(quantity) == 0;
        }
    }

    /** @return true 表示新账已处理（包含完全相同业务动作的重试）；false 表示旧口径。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public boolean post(ErpStockRecordCreateReqBO request, Runnable legacyWriter) {
        if (!enabled) {
            return false; // 未迁移新表的旧账套不得发起任何新表查询。
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("库存过账必须在业务审批事务中执行");
        }
        LocalDateTime start = parseCutover();
        if (LocalDateTime.now().isBefore(start)) {
            throw new IllegalStateException("尚未到双成本切换时间，不允许提前启用");
        }
        validateRequest(request);
        if(Integer.valueOf(70).equals(request.getBizType())) purchaseCostConfirmationService.validatePosting(request);
        if (Integer.valueOf(60).equals(request.getBizType())) saleReturnCostService.validatePosting(request);
        boolean currentSaleReturn = Integer.valueOf(60).equals(request.getBizType())
                && saleReturnCostService.isCurrentCostPosting(request);
        if (Integer.valueOf(80).equals(request.getBizType())) purchaseReturnCostService.validatePosting(request);
        tradeSnapshotService.validatePreparedForPosting(request);
        repository.assertLegacyCursorReady();
        long tenant = TenantContextHolder.getRequiredTenantId();
        Map<String, Object> stock = repository.lockStock(tenant, request.getProductId(), request.getWarehouseId());
        if (stock == null || stock.get("dept_id") == null || ((Number) stock.get("dept_id")).longValue() <= 0) {
            throw new IllegalStateException("需先建立库存归属部门并核对双成本期初");
        }
        long stockId = ((Number) stock.get("id")).longValue();
        Map<String, Object> balance = repository.lockBalance(tenant, stockId);
        if (balance == null || !start.equals(dateTime(balance.get("cutover_at")))) {
            throw new IllegalStateException("该库存尚未核对本次切换期初，缺失成本不能按零入账");
        }
        String key = actionKey(request);
        String fingerprint = fingerprint(request);
        BigDecimal oldCount = decimal(balance, "quantity");
        // 当前读取得全部锁后记录过账时间，与同库存真实发生顺序一致。
        LocalDateTime postedAt = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        long postingId;
        try {
            // INSERT 原子认领动作，不对不存在的 action_key 加 gap 锁。
            // 此行仅是当前未提交事务内部状态，计算完成前不会调用快照或旧库存回调。
            postingId = repository.insertPosting("INSERT INTO erp_stock_dual_cost_posting "
                        + "(tenant_id,action_key,request_hash,stock_id,product_id,warehouse_id,stock_dept_id,accounting_dept_id,"
                        + "biz_type,biz_id,biz_item_id,biz_no,source_biz_type,source_biz_id,source_biz_item_id,reversal_posting_id,"
                        + "biz_date,posted_at,quantity,financial_movement,settlement_movement,balance_quantity,"
                        + "financial_balance,settlement_balance,rule_version,raw_unit_price,source_price_basis,batch_no,posted_by) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                tenant, key, fingerprint, stockId, request.getProductId(), request.getWarehouseId(), stock.get("dept_id"),
                request.getAccountingDeptId(), request.getBizType(), request.getBizId(), request.getBizItemId(), request.getBizNo(),
                request.getSourceBizType(), request.getSourceBizId(), request.getSourceBizItemId(), request.getReversalPostingId(),
                request.getBizDate(), postedAt, request.getCount(), BigDecimal.ZERO, BigDecimal.ZERO,
                oldCount, decimal(balance, "financial_amount"), decimal(balance, "settlement_amount"), PENDING_POSTING_RULE,
                request.getUnitPrice(), request.getSourcePriceBasis(), request.getBatchNo(),
                cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId());
        } catch (DuplicateKeyException exception) {
            if (!isActionKeyConflict(exception)) {
                throw exception;
            }
            // 唯一约束已证明此动作存在，才对该实际键当前读；旧 RR 快照不能漏掉幂等。
            Map<String, Object> previous = repository.findPosting(tenant, key);
            if (previous == null || PENDING_POSTING_RULE.equals(previous.get("rule_version"))) {
                throw new IllegalStateException("过账动作缺少完整实际事件，请核对", exception);
            }
            if (!Objects.equals(previous.get("request_hash"), fingerprint)) {
                throw new IllegalStateException("相同过账动作的内容已变化，不能作为重试入账", exception);
            }
            return true;
        }
        if (postingId <= 0) {
            throw new IllegalStateException("库存过账未取得实际事件编号");
        }
        final boolean[] completed = {false};
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                if (!completed[0]) {
                    throw new IllegalStateException("库存过账内部认领未完成全部核算，整事务回滚");
                }
            }
        });
        long legacyRecordId = repository.latestLegacyRecordId(tenant, request.getProductId(), request.getWarehouseId());
        if (oldCount.compareTo(decimal(stock, "count")) != 0
                || !sameNumber(balance.get("legacy_cost_price"), stock.get("cost_price"))
                || !sameNumber(balance.get("legacy_cost_amount"), stock.get("cost_amount"))
                || !sameNumber(balance.get("stock_dept_id"), stock.get("dept_id"))
                || !sameNumber(balance.get("legacy_record_id"), legacyRecordId)) {
            throw new IllegalStateException("核对后库存数量、成本、归属或业务流水已变化，请重新核对切换差异");
        }
        ErpDualCostCalculator.Change financial = request.getFinancialMovementAmount() != null
                ? ErpDualCostCalculator.calculateWithAmount(oldCount, decimal(balance, "financial_amount"), request.getCount(), request.getFinancialMovementAmount())
                : ErpDualCostCalculator.calculate(oldCount, decimal(balance, "financial_amount"), request.getCount(), request.getFinancialUnitCost());
        ErpDualCostCalculator.Change settlement = request.getSettlementMovementAmount() != null
                ? ErpDualCostCalculator.calculateWithAmount(oldCount, decimal(balance, "settlement_amount"), request.getCount(), request.getSettlementMovementAmount())
                : ErpDualCostCalculator.calculate(oldCount, decimal(balance, "settlement_amount"), request.getCount(), request.getSettlementUnitCost());
        String ruleVersion = Integer.valueOf(60).equals(request.getBizType())
                ? (currentSaleReturn ? "SALE_RETURN_CURRENT_COST_V1" : "SALE_RETURN_ORIGINAL_COST_V1")
                : Integer.valueOf(80).equals(request.getBizType()) ? "PURCHASE_RETURN_CURRENT_AVERAGE_V1" : "DUAL_MOVING_AVERAGE_V1";
        int finalized = repository.execute("UPDATE erp_stock_dual_cost_posting SET financial_movement=?,settlement_movement=?,"
                        + "balance_quantity=?,financial_balance=?,settlement_balance=?,rule_version=? "
                        + "WHERE id=? AND tenant_id=? AND action_key=? AND request_hash=? AND rule_version=?",
                financial.getMovement(), settlement.getMovement(), financial.getQuantity(), financial.getAmount(),
                settlement.getAmount(), ruleVersion, postingId, tenant, key, fingerprint, PENDING_POSTING_RULE);
        if (finalized != 1) {
            throw new IllegalStateException("库存过账实际金额补全数量异常，整事务回滚");
        }
        tradeSnapshotService.appendForPosting(postingId, postedAt, request.getTradeContext());
        if (Integer.valueOf(60).equals(request.getBizType())) {
            saleReturnCostService.appendAllocation(postingId, postedAt, request);
        }
        if (Integer.valueOf(80).equals(request.getBizType())) {
            purchaseReturnCostService.appendAllocation(postingId, postedAt, request);
        }
        if(Integer.valueOf(70).equals(request.getBizType()) && request.getCostConfirmationId()!=null) {
            repository.execute("INSERT INTO erp_purchase_cost_posting_link (tenant_id,confirmation_id,revision,source_item_id,action_key,financial_amount,settlement_amount) VALUES (?,?,?,?,?,?,?)",
                    tenant,request.getCostConfirmationId(),request.getCostConfirmationRevision(),request.getBizItemId(),key,financial.getMovement(),settlement.getMovement());
        }
        int updated = repository.execute("UPDATE erp_stock_dual_cost_balance SET quantity=?,financial_amount=?,"
                        + "settlement_amount=?,updated_at=? WHERE tenant_id=? AND stock_id=?",
                financial.getQuantity(), financial.getAmount(), settlement.getAmount(), postedAt, tenant, stockId);
        if (postingId <= 0 || updated != 1) {
            throw new IllegalStateException("库存过账写入数量异常");
        }
        writeLegacy(request, currentSaleReturn ? financial.getMovement() : null, legacyWriter);
        Map<String, Object> legacyAfter = repository.lockStock(tenant, request.getProductId(), request.getWarehouseId());
        if (legacyAfter == null || financial.getQuantity().compareTo(decimal(legacyAfter, "count")) != 0) {
            throw new IllegalStateException("过账后实物库存与双成本数量不一致");
        }
        int legacyUpdated = repository.execute("UPDATE erp_stock_dual_cost_balance SET legacy_record_id=?,legacy_cost_price=?,"
                        + "legacy_cost_amount=? WHERE tenant_id=? AND stock_id=?",
                repository.latestLegacyRecordId(tenant, request.getProductId(), request.getWarehouseId()),
                legacyAfter.get("cost_price"), legacyAfter.get("cost_amount"), tenant, stockId);
        if (legacyUpdated != 1) {
            throw new IllegalStateException("库存过账兼容账水位更新数量异常，整事务回滚");
        }
        completed[0] = true;
        return true;
    }

    private boolean isActionKeyConflict(DuplicateKeyException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException) {
                SQLException sql = (SQLException) cause;
                if (sql.getErrorCode() == 1062 && sql.getMessage() != null
                        && java.util.regex.Pattern.compile("(?i)for key ['`\"](?:[^'`\"]*\\.)?uk_tenant_action['`\"]")
                        .matcher(sql.getMessage()).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 实际管理入口调用，授权及仓库数据权限由 Controller 检查；期初追加保存，禁止覆盖。 */
    @Transactional(rollbackFor = Exception.class)
    public void confirmOpening(long productId, long warehouseId, BigDecimal quantity,
                               BigDecimal financialAmount, BigDecimal settlementAmount,
                               LocalDateTime cutoverAt, String evidence, long confirmedBy) {
        if (enabled) {
            throw new IllegalStateException("新账启用后禁止覆盖期初，应使用受控调整业务");
        }
        if (quantity == null || financialAmount == null || settlementAmount == null
                || quantity.signum() < 0 || financialAmount.signum() < 0 || settlementAmount.signum() < 0
                || (quantity.signum() == 0 && (financialAmount.signum() != 0 || settlementAmount.signum() != 0))
                || cutoverAt == null || cutoverAt.getNano() != 0 || !StringUtils.hasText(evidence)
                || evidence.length() > 500 || confirmedBy <= 0) {
            throw new IllegalArgumentException("请提供有效的数量、双成本期初、秒精度切换时间和核对依据");
        }
        ErpDualCostCalculator.requireStoragePrecision(quantity);
        ErpDualCostCalculator.requireStoragePrecision(financialAmount);
        ErpDualCostCalculator.requireStoragePrecision(settlementAmount);
        long tenant = TenantContextHolder.getRequiredTenantId();
        Map<String, Object> stock = repository.lockStock(tenant, productId, warehouseId);
        if (stock == null || stock.get("dept_id") == null || ((Number) stock.get("dept_id")).longValue() <= 0 || decimal(stock, "count").compareTo(quantity) != 0) {
            throw new IllegalStateException("核对数量与当前库存不一致，或库存归属部门缺失");
        }
        long stockId = ((Number) stock.get("id")).longValue();
        if (repository.lockBalance(tenant, stockId) != null) {
            throw new IllegalStateException("期初已核对，不允许重复确认或覆盖");
        }
        repository.execute("INSERT INTO erp_stock_dual_cost_balance (tenant_id,stock_id,quantity,financial_amount,"
                        + "settlement_amount,opening_quantity,opening_financial_amount,opening_settlement_amount,"
                        + "cutover_at,confirmed_by,confirmed_at,evidence,updated_at,legacy_record_id,legacy_cost_price,"
                        + "legacy_cost_amount,stock_dept_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                tenant, stockId, quantity, financialAmount, settlementAmount, quantity, financialAmount, settlementAmount,
                cutoverAt, confirmedBy, LocalDateTime.now(), evidence.trim(), LocalDateTime.now(),
                repository.verifyLegacyCursorForOpening(tenant, productId, warehouseId), stock.get("cost_price"),
                stock.get("cost_amount"), stock.get("dept_id"));
    }

    private LocalDateTime parseCutover() {
        if (!StringUtils.hasText(cutover)) {
            throw new IllegalStateException("启用双成本前必须配置已核对的切换时间");
        }
        return LocalDateTime.parse(cutover);
    }

    static void validateRequest(ErpStockRecordCreateReqBO request) {
        if (request.getProductId() == null || request.getWarehouseId() == null || request.getBizId() == null
                || request.getBizItemId() == null || request.getAccountingDeptId() == null || request.getAccountingDeptId() <= 0
                || !StringUtils.hasText(request.getBizNo()) || request.getCount() == null || request.getBizDate() == null) {
            throw new IllegalArgumentException("新口径过账必须包含业务行、核算部门、业务日期及数量");
        }
        if (Integer.valueOf(70).equals(request.getBizType()) && request.getCount().signum() > 0) {
            if (!Boolean.TRUE.equals(request.getCostBasisConfirmed()) || request.getFinancialMovementAmount() == null
                    || request.getSettlementMovementAmount() == null || request.getCostConfirmationId()==null || request.getCostConfirmationRevision()==null) {
                throw new IllegalStateException("采购尚未确认不含税入库成本及费用折扣分摊，不能进入新账");
            }
        } else if (Integer.valueOf(60).equals(request.getBizType()) && request.getCount().signum() > 0) {
            boolean original = request.getReversalPostingId() != null && request.getSourceBizId() != null
                    && request.getSourceBizItemId() != null && Integer.valueOf(50).equals(request.getSourceBizType());
            boolean noOriginal = request.getReversalPostingId() == null && request.getSourceBizId() == null
                    && request.getSourceBizItemId() == null && request.getSourceBizType() == null;
            if ((!original && !noOriginal) || request.getFinancialMovementAmount() == null
                    || request.getSettlementMovementAmount() == null || request.getCostConfirmationId() != null) {
                throw new IllegalStateException("销售退货必须提供完整已核准双成本及明确来源规则");
            }
        } else if (Integer.valueOf(80).equals(request.getBizType()) && request.getCount().signum() < 0) {
            if (request.getFinancialMovementAmount() != null || request.getSettlementMovementAmount() != null
                    || request.getFinancialUnitCost() != null || request.getSettlementUnitCost() != null
                    || request.getCostConfirmationId() != null) {
                throw new IllegalStateException("采购退货只能按当时库存余额均价扣成本，禁止传入退款价或原采购成本覆盖");
            }
        } else if (!(Integer.valueOf(50).equals(request.getBizType()) && request.getCount().signum() < 0)) {
            throw new IllegalStateException("该业务尚未适配新核算口径（调拨、退货、调价及反审核），禁止部分记账");
        }
        if (request.getReversalPostingId() != null && !Integer.valueOf(60).equals(request.getBizType())) {
            throw new IllegalStateException("反向过账需使用已适配的原单冲回流程");
        }
    }

    static String actionKey(ErpStockRecordCreateReqBO request) {
        String action = StringUtils.hasText(request.getPostingActionKey()) ? request.getPostingActionKey() : "APPROVE";
        String key = request.getBizType() + ":" + request.getBizId() + ":" + request.getBizItemId() + ":" + action;
        if (key.length() > 160) {
            throw new IllegalArgumentException("过账动作标识过长");
        }
        return key;
    }

    static String fingerprint(ErpStockRecordCreateReqBO r) {
        // 长度前缀避免分隔符歧义；数字规范化使 1.0 与 1.00 是同一业务内容。
        Object[] values = {r.getProductId(), r.getWarehouseId(), r.getBizType(), r.getBizId(), r.getBizItemId(),
                r.getBizNo(), r.getCount(), r.getUnitPrice(), r.getFinancialUnitCost(), r.getSettlementUnitCost(),
                r.getAccountingDeptId(), r.getBizDate(), r.getBatchNo(), r.getSourceBizType(), r.getSourceBizId(),
                r.getSourceBizItemId(), r.getReversalPostingId(), r.getCostBasisConfirmed(), r.getSourcePriceBasis(),
                r.getFinancialMovementAmount(),r.getSettlementMovementAmount(),r.getCostConfirmationId(),r.getCostConfirmationRevision()};
        StringBuilder payload = new StringBuilder();
        for (Object value : values) {
            String text = value == null ? "" : value instanceof BigDecimal
                    ? ((BigDecimal) value).stripTrailingZeros().toPlainString() : value.toString();
            payload.append(value == null ? -1 : text.length()).append(':').append(text);
        }
        // 旧无交易快照动作保留原hash算法；带新快照的动作必须连同完整签名匹配，不能给旧事件补当前值。
        if (r.getTradeContext() != null) {
            String signature = r.getTradeContext().getSignature();
            payload.append("TRADE:").append(signature.length()).append(':').append(signature);
        }
        return DigestUtils.md5DigestAsHex(payload.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static BigDecimal decimal(Map<String, Object> row, String name) {
        Object value = row.get(name);
        if (value == null) {
            throw new IllegalStateException("核算字段缺失：" + name);
        }
        return new BigDecimal(value.toString());
    }

    private static boolean sameNumber(Object first, Object second) {
        return first == null || second == null ? first == second
                : new BigDecimal(first.toString()).compareTo(new BigDecimal(second.toString())) == 0;
    }

    private static LocalDateTime dateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        throw new IllegalStateException("切换日期类型不可识别");
    }
}
