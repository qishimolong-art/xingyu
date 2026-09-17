package cn.iocoder.yudao.module.erp.service.sale.returncost;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpSaleReturnCurrentCostModels.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.*;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleReturnModeEnum;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.stock.cost.ErpDualCostLedgerRepository;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.util.StringUtils;
import javax.annotation.Resource;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** 无单退货当前基准与独立补齐确认；不是原销售成本分摊。 */
@Service
public class ErpSaleReturnCurrentCostService {
    private static final Object CONTEXT = new Object();
    @Value("${erp.reporting.dual-cost-enabled:false}") private boolean enabled;
    @Value("${erp.reporting.dual-cost-cutover:}") private String cutover;
    @Resource private ErpSaleReturnMapper returnMapper;
    @Resource private ErpSaleReturnItemMapper itemMapper;
    @Resource private ErpStockMapper stockMapper;
    @Resource private ErpProductMapper productMapper;
    @Resource private ErpWarehouseService warehouseService;
    @Resource private ErpCustomerService customerService;
    @Resource private PermissionApi permissionApi;
    @Resource private DeptApi deptApi;
    @Resource private ErpDualCostLedgerRepository ledger;
    @Resource private ErpSaleReturnCurrentCostRepository repository;

    public boolean isEnabled() { return enabled; }

    @Transactional(rollbackFor = Exception.class)
    public Header preview(Long id) {
        Source source = source(id);
        if (!enabled || !ErpSaleReturnModeEnum.isByStock(source.header.getReturnMode())) {
            return header(source);
        }
        try {
            complete(source);
        } catch (BadSqlGrammarException exception) {
            source.status = "SCHEMA_MISSING";
            source.reason = "无单退货成本或库存核算增量表未完整迁移";
        }
        return header(source);
    }

    @Transactional(rollbackFor = Exception.class)
    public PageResult<Row> itemPage(Long id, String expectedSourceSignature, String expectedBasisSignature,
                                    int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 200) { throw error("明细分页参数无效，单页最多200行"); }
        Source source = source(id);
        if (!enabled) { throw error("新核算尚未启用"); }
        complete(source);
        checkSignatures(source, expectedSourceSignature, expectedBasisSignature);
        int start = (int) Math.min((long) (pageNo - 1) * pageSize, source.items.size());
        int end = Math.min(start + pageSize, source.items.size());
        List<ErpSaleReturnItemDO> page = source.items.subList(start, end);
        Set<Long> productIds = page.stream().map(ErpSaleReturnItemDO::getProductId).collect(Collectors.toSet());
        Map<Long, ErpProductDO> products = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (ErpProductDO product : DataPermissionUtils.executeIgnore(() -> productMapper.selectByIds(productIds))) {
                products.put(product.getId(), product);
            }
        }
        List<Row> result = new ArrayList<>();
        Map<Long, ErpWarehouseDO> warehouseNames = warehouseService.getWarehouseMap(page.stream()
                .map(ErpSaleReturnItemDO::getWarehouseId).collect(Collectors.toSet()));
        for (ErpSaleReturnItemDO item : page) {
            Row row = new Row();
            row.setSourceItemId(item.getId()); row.setProductId(item.getProductId());
            row.setWarehouseId(item.getWarehouseId()); row.setQuantity(item.getCount());
            ErpWarehouseDO warehouse = warehouseNames.get(item.getWarehouseId());
            if (warehouse != null) { row.setWarehouseName(warehouse.getName()); }
            row.setSourceSignature(source.sourceSignature); row.setBasisSignature(source.basisSignature);
            ErpProductDO product = products.get(item.getProductId());
            if (product != null) { row.setProductCode(product.getCode()); row.setProductName(product.getName()); }
            Amounts amounts = source.amounts.get(item.getId());
            row.setCostSource(amounts == null ? "MISSING" : amounts.kind);
            row.setReason(amounts == null ? source.reason : source.masked ? "成本字段无查看权限" : null);
            if (amounts != null && !source.masked) {
                row.setFinancialAmount(amounts.financial); row.setSettlementAmount(amounts.settlement);
            }
            result.add(row);
        }
        return new PageResult<>(result, (long) source.items.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public Header confirm(ConfirmRequest request) {
        validateRequest(request);
        if (!enabled) { throw error("新核算未启用，不能产生正式无单退货成本确认"); }
        Source source = source(request.getId());
        repository.checkSchema();
        repository.ensureState(tenant(), request.getId());
        complete(source);
        if (source.masked) { throw error("两套成本必须均可查看，才能补齐或修改"); }
        String requestHash = requestHash(request);
        Map<String, Object> previous = repository.byRequest(tenant(), request.getId(), request.getExpectedRevision(), request.getRequestKey());
        if (previous != null) {
            if (!Objects.equals(number(previous, "return_id"), request.getId())
                    || !Objects.equals(previous.get("request_hash"), requestHash)) {
                throw error("相同确认请求键的内容已变化，不能当作重试");
            }
            return header(source);
        }
        requireEditable(source);
        if (!source.reconciled) { throw error("库存缺核对起点或存在差异，补单价不能替代库存核对"); }
        checkSignatures(source, request.getExpectedSourceSignature(), request.getExpectedBasisSignature());
        if (revision(source.confirmation) != request.getExpectedRevision()) { throw error("成本确认版本已变化，请刷新"); }
        Map<Long, ConfirmItem> byId = new HashMap<>();
        for (ConfirmItem item : request.getItems()) {
            if (byId.put(item.getSourceItemId(), item) != null) { throw error("成本确认存在重复明细"); }
        }
        if (!byId.keySet().equals(source.items.stream().map(ErpSaleReturnItemDO::getId).collect(Collectors.toSet()))) {
            throw error("成本确认必须包含当前完整源行，不能只提交已加载页面");
        }
        int revision = revision(source.confirmation) + 1;
        long id;
        try {
            id = repository.insert("INSERT INTO erp_sale_return_current_cost_confirmation "
                        + "(tenant_id,return_id,revision,request_key,request_hash,source_signature,stock_signature,evidence,confirmed_by,confirmed_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                tenant(), request.getId(), revision, request.getRequestKey(), requestHash, source.sourceSignature,
                source.stockSignature, request.getEvidence(), operator(), now());
        } catch (DuplicateKeyException exception) {
            // 同单合法重试已通过存在版本校验；跨单键复用或不同内容不得查询空键后重试插入。
            throw error("确认请求键已被使用或确认版本冲突，请刷新后使用新的请求键");
        }
        for (ErpSaleReturnItemDO item : source.items) {
            ConfirmItem value = byId.get(item.getId());
            repository.execute("INSERT INTO erp_sale_return_current_cost_line "
                            + "(tenant_id,confirmation_id,return_item_id,financial_amount,settlement_amount,evidence) VALUES (?,?,?,?,?,?)",
                    tenant(), id, item.getId(), value.getFinancialAmount(), value.getSettlementAmount(), value.getEvidence());
        }
        repository.setLatest(tenant(), request.getId(), id);
        complete(source);
        return header(source);
    }

    /** 原审核接口携带用户本次看到的完整基准；资源随事务挂起，不允许跨事务借用。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void authorizeApproval(Long id, String expectedBasisSignature) {
        if (!enabled) { return; }
        if (!StringUtils.hasText(expectedBasisSignature)) { throw error("请先查看本次无单退货成本基准"); }
        context().approvalTokens.put(id, expectedBasisSignature);
    }

    /** 认领标记只能伴随本方法实际成功的10→20 CAS生成，不能凭调用方布尔值或历史20状态构造。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public int claimApproval(Long id) {
        Context context = context();
        if (!enabled || !context.approvalTokens.containsKey(id)) { throw error("请先预览本次无单成本再审核"); }
        ErpSaleReturnDO source = returnMapper.selectByIdForUpdate(id);
        List<ErpSaleReturnItemDO> items = itemMapper.selectListByReturnIdForUpdate(id);
        if (source == null || !Integer.valueOf(10).equals(source.getStatus()) || items.isEmpty()) { throw error("无单退货已审核或不处于可审核状态"); }
        validateNoOriginalSource(source, items);
        repository.checkSchema();
        repository.ensureState(tenant(), id);
        int changed = returnMapper.updateByIdAndStatus(id, 10, new ErpSaleReturnDO().setStatus(20));
        if (changed != 1 || !context.claimed.add(id)) { throw error("无单退货审核认领冲突，请刷新"); }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void beforeCommit(boolean readOnly) {
                long preparedCount = context.prepared.entrySet().stream().filter(entry -> entry.getKey().startsWith(id + ":") && entry.getValue().posted).count();
                if (preparedCount != items.size() || repository.linkCount(tenant(), id) != items.size()) {
                    throw error("本次无单审核认领未完成全部真实库存，整单回滚");
                }
            }
        });
        return changed;
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public boolean isApprovedRetry(Long id) {
        if (!enabled) { return false; }
        Context context = context();
        if (!context.approvalTokens.containsKey(id)) { return false; }
        Source source = source(id);
        if (!ErpSaleReturnModeEnum.isByStock(source.header.getReturnMode()) || !Integer.valueOf(20).equals(source.header.getStatus())) { return false; }
        complete(source);
        if (source.masked || !"POSTED".equals(source.status)
                || !Objects.equals(source.basisSignature, context.approvalTokens.get(id))) {
            throw error("已审核单据的本次重试基准不一致，禁止再次写入");
        }
        return true;
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public Map<Long, ErpStockRecordCreateReqBO> prepareApproval(ErpSaleReturnDO header,
                                                              List<ErpSaleReturnItemDO> requestedItems) {
        if (!enabled) { return Collections.emptyMap(); }
        Source source = source(header.getId());
        if (!context().claimed.contains(header.getId()) || !Integer.valueOf(20).equals(source.header.getStatus())
                || !source.sourceSignature.equals(signature(header, requestedItems))) {
            throw error("无单成本仅能在本次真实完整审核事务中消费");
        }
        complete(source);
        Context context = context();
        if (source.masked) { throw error("本次审核人员没有完整双成本查看权限"); }
        if (!"READY".equals(source.status)) { throw error(source.reason); }
        if (!Objects.equals(context.approvalTokens.get(header.getId()), source.basisSignature)) {
            throw error("单据、库存成本或确认版本已变化，请重新展示成本后审核");
        }
        Map<Long, ErpStockRecordCreateReqBO> result = new LinkedHashMap<>();
        for (ErpSaleReturnItemDO item : source.items) {
            Amounts amounts = source.amounts.get(item.getId());
            if (item.getProductPrice() == null || item.getProductPrice().signum() < 0) { throw error("无单退货正式审核必须有明确业务退款单价"); }
            ErpStockRecordCreateReqBO request = new ErpStockRecordCreateReqBO(item.getProductId(), item.getWarehouseId(),
                    item.getBatchNo(), item.getCount(), 60, header.getId(), item.getId(), source.header.getNo(),
                    item.getProductPrice(), source.header.getReturnTime())
                    .setAccountingDeptId(source.header.getDeptId()).setFinancialMovementAmount(amounts.financial)
                    .setSettlementMovementAmount(amounts.settlement).setSourcePriceBasis("INCLUSIVE_UNCONFIRMED");
            Prepared prepared = new Prepared(request, source, amounts);
            if (context.prepared.putIfAbsent(key(request), prepared) != null) { throw error("本事务无单退货行已经准备"); }
            result.put(item.getId(), request);
        }
        if (source.confirmation != null && source.manualValid) {
            int count = repository.execute("UPDATE erp_sale_return_current_cost_confirmation SET consumed_at=? WHERE tenant_id=? AND id=? AND consumed_at IS NULL",
                    now(), tenant(), number(source.confirmation, "id"));
            if (count != 1) { throw error("成本确认已消费，禁止覆盖或重复使用"); }
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void beforeCommit(boolean readOnly) {
                if (repository.linkCount(tenant(), header.getId()) != source.items.size()
                        || result.values().stream().anyMatch(request -> !context.prepared.get(key(request)).posted)) {
                    throw error("无单退货未完成全部真实库存及成本证据，整单回滚");
                }
            }
        });
        return result;
    }

    public boolean hasPrepared(ErpStockRecordCreateReqBO request) {
        Context context = (Context) TransactionSynchronizationManager.getResource(CONTEXT);
        return context != null && context.tenant == tenant() && context.prepared.containsKey(key(request));
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void validatePosting(ErpStockRecordCreateReqBO request) { prepared(request); }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void appendPosting(long postingId, LocalDateTime postedAt, ErpStockRecordCreateReqBO request) {
        Prepared prepared = prepared(request);
        Map<String, Object> actual = repository.posting(tenant(), postingId);
        if (prepared.posted || actual == null || !Objects.equals(number(actual, "biz_type"), 60L)
                || !Objects.equals(number(actual, "biz_id"), request.getBizId())
                || !Objects.equals(number(actual, "biz_item_id"), request.getBizItemId())
                || !Objects.equals(number(actual, "product_id"), request.getProductId())
                || !Objects.equals(number(actual, "warehouse_id"), request.getWarehouseId())
                || !Objects.equals(number(actual, "accounting_dept_id"), request.getAccountingDeptId())
                || actual.get("source_biz_type") != null || actual.get("source_biz_id") != null
                || actual.get("source_biz_item_id") != null || actual.get("reversal_posting_id") != null
                || !equal(decimal(actual, "quantity"), request.getCount())
                || !equal(decimal(actual, "financial_movement"), request.getFinancialMovementAmount())
                || !equal(decimal(actual, "settlement_movement"), request.getSettlementMovementAmount())
                || !Objects.equals(date(actual.get("posted_at")), postedAt)) {
            throw error("无单退货证据必须绑定本次真实无来源成本事件");
        }
        repository.execute("INSERT INTO erp_sale_return_current_cost_posting_link "
                        + "(tenant_id,return_id,return_item_id,posting_id,confirmation_id,confirmation_revision,basis_signature,source_signature,stock_signature,cost_source,quantity,financial_amount,settlement_amount,stock_basis,posted_at,approved_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                tenant(), request.getBizId(), request.getBizItemId(), postingId, prepared.confirmationId, prepared.revision,
                prepared.basisSignature, prepared.sourceSignature, prepared.stockSignature, prepared.kind,
                request.getCount(), request.getFinancialMovementAmount(), request.getSettlementMovementAmount(),
                prepared.stockBasis, postedAt, operator());
        repository.markPosted(tenant(), request.getBizId());
        prepared.posted = true;
    }

    private Source source(Long id) {
        operator();
        ErpSaleReturnDO header = returnMapper.selectByIdForUpdate(id);
        if (header == null) { throw new ServiceException(403, "销售退货不存在或无单据数据权限"); }
        Source source = new Source(); source.header = header;
        source.items = new ArrayList<>(itemMapper.selectListByReturnIdForUpdate(id));
        source.items.sort(Comparator.comparing(ErpSaleReturnItemDO::getId));
        source.sourceSignature = signature(header, source.items);
        source.status = enabled ? "NOT_APPLICABLE" : "DISABLED";
        source.reason = enabled ? "仅明确按库存退货适用此成本流程" : "新核算尚未启用";
        return source;
    }

    private void complete(Source source) {
        validateNoOriginalSource(source.header, source.items);
        if (source.header.getCustomerId() == null || source.header.getCustomerId() <= 0
                || source.header.getDeptId() == null || source.header.getDeptId() <= 0 || source.items.isEmpty()) {
            throw error("正式无单退货必须有真实客户、核算部门及完整明细");
        }
        // 历史客户可以停用，但不能绕过当前客户可见范围；启用校验仅适用于下方新确认/审核。
        if (customerService.getCustomer(source.header.getCustomerId()) == null) {
            throw new ServiceException(403, "销售退货客户不存在或无客户数据权限");
        }
        repository.checkSchema();
        source.amounts.clear(); source.reconciled = true;
        Set<Long> products = source.items.stream().map(ErpSaleReturnItemDO::getProductId).collect(Collectors.toSet());
        Set<Long> warehouses = source.items.stream().map(ErpSaleReturnItemDO::getWarehouseId).collect(Collectors.toSet());
        TreeMap<String, List<ErpSaleReturnItemDO>> groups = new TreeMap<>();
        for (ErpSaleReturnItemDO item : source.items) {
            if (item.getProductId() == null || item.getWarehouseId() == null || item.getCount() == null || item.getCount().signum() <= 0) {
                throw error("无单退货明细商品、仓库或数量无效");
            }
            groups.computeIfAbsent(pair(item.getProductId(), item.getWarehouseId()), ignored -> new ArrayList<>()).add(item);
        }
        List<ErpStockDO> available = DataPermissionUtils.executeIgnore(() -> stockMapper.selectListByProductIdsAndWarehouseIds(products, warehouses));
        Map<String, ErpStockDO> stocks = new HashMap<>();
        for (ErpStockDO stock : available) {
            String key = pair(stock.getProductId(), stock.getWarehouseId());
            if (groups.containsKey(key) && stocks.put(key, stock) != null) { throw error("商品仓库存在重复库存维度"); }
        }
        warehouseService.validateCurrentUserStockPermission(stocks.values());
        if (!stocks.keySet().equals(groups.keySet())) { throw error("退入库存缺少可核对记录，不能凭空补成本"); }
        source.masked = masked(source, new ArrayList<>(stocks.values()));
        source.confirmation = repository.latest(tenant(), source.header.getId());
        List<Long> sourceItemIds = source.items.stream().map(ErpSaleReturnItemDO::getId).collect(Collectors.toList());
        List<Map<String, Object>> posted = repository.postingLinks(tenant(), source.header.getId(), sourceItemIds);
        Context active = (Context) TransactionSynchronizationManager.getResource(CONTEXT);
        boolean newlyClaimed = active != null && active.tenant == tenant() && active.claimed.contains(source.header.getId());
        if (!posted.isEmpty() || (Integer.valueOf(20).equals(source.header.getStatus()) && !newlyClaimed)) {
            // 历史展示仅当前读库存身份以检查权限，不读取今天余额、更不按今天均价重算。
            List<ErpStockDO> historicalScope = new ArrayList<>();
            for (List<ErpSaleReturnItemDO> group : groups.values()) {
                ErpSaleReturnItemDO item = group.get(0);
                ErpStockDO stock = DataPermissionUtils.executeIgnore(() -> stockMapper.selectByProductIdAndWarehouseIdForUpdate(item.getProductId(), item.getWarehouseId()));
                if (stock == null) { throw error("历史库存身份缺失，无法核对查询权限"); }
                historicalScope.add(stock);
            }
            warehouseService.validateCurrentUserStockPermission(historicalScope);
            source.masked = source.masked || masked(source, historicalScope);
            if (!posted.isEmpty()) {
                applyPosted(source, posted);
            } else {
                source.status = "HISTORICAL_COST_MISSING";
                source.reason = "该历史已审核无单退货未记录本次核算成本，不能以今天均价补算";
                source.basisSignature = hash(source.sourceSignature + ":HISTORICAL_COST_MISSING");
                source.reconciled = false;
            }
            return;
        }
        customerService.validateCustomer(source.header.getCustomerId());
        deptApi.validateDeptList(Collections.singleton(source.header.getDeptId()));
        LocalDateTime start;
        try { start = LocalDateTime.parse(cutover); }
        catch (RuntimeException exception) { throw error("双成本切换时间配置无效"); }
        if (start.getNano() != 0 || LocalDateTime.now().isBefore(start)) { throw error("双成本尚未到有效切换时间"); }
        List<Object> basis = new ArrayList<>();
        basis.add(tenant()); basis.add(start);
        List<ErpStockDO> currentStocks = new ArrayList<>();
        for (Map.Entry<String, List<ErpSaleReturnItemDO>> group : groups.entrySet()) {
            ErpSaleReturnItemDO item = group.getValue().get(0);
            ErpStockDO stock = DataPermissionUtils.executeIgnore(() -> stockMapper.selectByProductIdAndWarehouseIdForUpdate(item.getProductId(), item.getWarehouseId()));
            if (stock == null) { throw error("锁定期间实际库存已缺失，请核对"); }
            currentStocks.add(stock);
            Map<String, Object> balance = ledger.lockBalance(tenant(), stock.getId());
            long watermark = ledger.latestLegacyRecordId(tenant(), item.getProductId(), item.getWarehouseId());
            Map<String, Object> snapshot = new TreeMap<>();
            snapshot.put("stockId", stock.getId()); snapshot.put("productId", stock.getProductId());
            snapshot.put("warehouseId", stock.getWarehouseId()); snapshot.put("deptId", stock.getDeptId());
            snapshot.put("quantity", stock.getCount()); snapshot.put("legacyAmount", stock.getCostAmount());
            snapshot.put("legacyPrice", stock.getCostPrice()); snapshot.put("legacyWatermark", watermark);
            snapshot.put("balance", balance == null ? null : new TreeMap<>(balance)); basis.add(snapshot);
            boolean valid = balance != null && Objects.equals(date(balance.get("cutover_at")), start)
                    && stock.getDeptId() != null && stock.getDeptId() > 0
                    && equal(stock.getCount(), decimal(balance, "quantity"))
                    && equal(stock.getCostAmount(), decimal(balance, "legacy_cost_amount"))
                    && equal(stock.getCostPrice(), decimal(balance, "legacy_cost_price"))
                    && Objects.equals(stock.getDeptId(), number(balance, "stock_dept_id"))
                    && Objects.equals(watermark, number(balance, "legacy_record_id"))
                    && decimal(balance, "financial_amount") != null && decimal(balance, "financial_amount").signum() >= 0
                    && decimal(balance, "settlement_amount") != null && decimal(balance, "settlement_amount").signum() >= 0
                    && stock.getCount() != null && stock.getCount().signum() >= 0;
            if (!valid || (stock.getCount().signum() == 0 && (decimal(balance, "financial_amount").signum() != 0
                    || decimal(balance, "settlement_amount").signum() != 0))) {
                source.reconciled = false;
                continue;
            }
            if (stock.getCount().signum() == 0) { continue; }
            BigDecimal accumulatedQuantity = BigDecimal.ZERO;
            BigDecimal accumulatedFinancial = BigDecimal.ZERO, accumulatedSettlement = BigDecimal.ZERO;
            for (ErpSaleReturnItemDO line : group.getValue()) {
                if (source.masked) {
                    // 只返回权限状态，不通过金额计算异常暴露隐藏成本；这些行不允许进入审核准备。
                    source.amounts.put(line.getId(), new Amounts(null, null, "CURRENT_AVERAGE"));
                    continue;
                }
                accumulatedQuantity = accumulatedQuantity.add(line.getCount());
                BigDecimal financial = proportional(decimal(balance, "financial_amount"), accumulatedQuantity, stock.getCount());
                BigDecimal settlement = proportional(decimal(balance, "settlement_amount"), accumulatedQuantity, stock.getCount());
                source.amounts.put(line.getId(), new Amounts(financial.subtract(accumulatedFinancial), settlement.subtract(accumulatedSettlement), "CURRENT_AVERAGE"));
                accumulatedFinancial = financial; accumulatedSettlement = settlement;
            }
        }
        warehouseService.validateCurrentUserStockPermission(currentStocks);
        source.masked = source.masked || masked(source, currentStocks);
        source.stockBasis = JsonUtils.toJsonString(basis);
        source.stockSignature = hash(source.stockBasis);
        source.manualValid = source.confirmation != null && source.confirmation.get("consumed_at") == null
                && Objects.equals(source.confirmation.get("source_signature"), source.sourceSignature)
                && Objects.equals(source.confirmation.get("stock_signature"), source.stockSignature);
        if (source.manualValid) {
            Map<Long, Amounts> confirmed = new HashMap<>();
            for (Map<String, Object> row : repository.lines(tenant(), number(source.confirmation, "id"), sourceItemIds)) {
                BigDecimal financial = decimal(row, "financial_amount"), settlement = decimal(row, "settlement_amount");
                if (financial == null || settlement == null || financial.signum() < 0 || settlement.signum() < 0) { throw error("确认成本证据金额不完整"); }
                confirmed.put(number(row, "return_item_id"), new Amounts(financial, settlement, "MANUAL_CONFIRMED"));
            }
            if (!confirmed.keySet().equals(source.items.stream().map(ErpSaleReturnItemDO::getId).collect(Collectors.toSet()))) { throw error("确认成本证据未覆盖完整源行"); }
            source.amounts = confirmed;
        }
        source.basisSignature = hash(tenant() + ":" + source.sourceSignature + ":" + source.stockSignature + ":"
                + revision(source.confirmation) + ":" + (source.confirmation == null ? "" : source.confirmation.get("request_hash")));
        source.status = !source.reconciled ? "STOCK_RECONCILIATION_REQUIRED"
                : source.amounts.size() == source.items.size() ? "READY" : "CONFIRMATION_REQUIRED";
        source.reason = !source.reconciled ? "库存缺少可靠核对起点或发生差异，请先核对库存"
                : "READY".equals(source.status) ? null : "零库存无有效当前均价，请明确补齐本次两套成本及依据";
        if (source.reconciled && source.confirmation != null && !source.manualValid && source.confirmation.get("consumed_at") == null) {
            source.status = "CONFIRMATION_REQUIRED";
            source.reason = "人工成本确认后单据或库存基准已变化，必须重新查看并确认";
        }
        if (!source.reconciled) { source.amounts.clear(); }
    }

    /** 已审核返回不可变实际事件；今天库存变化不改写历史成本。 */
    private void applyPosted(Source source, List<Map<String, Object>> posted) {
        Map<Long, ErpSaleReturnItemDO> items = source.items.stream().collect(Collectors.toMap(ErpSaleReturnItemDO::getId, item -> item));
        if (!Integer.valueOf(20).equals(source.header.getStatus()) || posted.size() != items.size()) { throw error("无单退货实际证据与完整审核状态不一致"); }
        source.amounts.clear();
        String basis = null;
        for (Map<String, Object> row : posted) {
            ErpSaleReturnItemDO item = items.get(number(row, "return_item_id"));
            if (item == null || row.get("actual_id") == null || !Objects.equals(number(row, "actual_type"), 60L)
                    || !Objects.equals(number(row, "actual_return_id"), source.header.getId())
                    || !Objects.equals(number(row, "actual_item_id"), item.getId())
                    || !Objects.equals(number(row, "actual_product"), item.getProductId())
                    || !Objects.equals(number(row, "actual_warehouse"), item.getWarehouseId())
                    || !Objects.equals(number(row, "actual_dept"), source.header.getDeptId())
                    || row.get("actual_source") != null || row.get("actual_reversal") != null
                    || !Objects.equals(source.sourceSignature, row.get("source_signature"))
                    || !equal(item.getCount(), decimal(row, "quantity")) || !equal(item.getCount(), decimal(row, "actual_quantity"))
                    || !equal(decimal(row, "financial_amount"), decimal(row, "actual_financial"))
                    || !equal(decimal(row, "settlement_amount"), decimal(row, "actual_settlement"))
                    || (basis != null && !basis.equals(row.get("basis_signature")))) {
                throw error("无单退货历史证据与真实成本事件不一致，请核对");
            }
            basis = (String) row.get("basis_signature");
            source.amounts.put(item.getId(), new Amounts(decimal(row, "financial_amount"), decimal(row, "settlement_amount"), (String) row.get("cost_source")));
        }
        source.basisSignature = basis; source.status = "POSTED"; source.reason = "已按保存的成本基准完成审核过账";
    }

    public void validateNoOriginalSource(ErpSaleReturnDO header, List<ErpSaleReturnItemDO> items) {
        if (!ErpSaleReturnModeEnum.isByStock(header.getReturnMode()) || header.getSourceOutId() != null
                || header.getOrderId() != null || header.getSourceOutNo() != null || header.getOrderNo() != null
                || items.stream().anyMatch(item -> item.getSourceOutItemId() != null || item.getOrderItemId() != null)) {
            throw error("无单退货不得夹带原销售或旧订单引用，请明确修正单据来源");
        }
    }

    private Header header(Source source) {
        Header result = new Header(); result.setId(source.header.getId()); result.setNo(source.header.getNo());
        result.setEnabled(enabled); result.setStatus(source.status); result.setReason(source.reason);
        result.setSourceStatus(source.header.getStatus()); result.setSourceSignature(source.sourceSignature);
        result.setBasisSignature(source.basisSignature); result.setSourceItemCount(source.items.size());
        result.setLatestRevision(revision(source.confirmation)); result.setCostMasked(source.masked);
        result.setConfirmationId(source.confirmation == null ? null : number(source.confirmation, "id"));
        boolean editable = (Integer.valueOf(0).equals(source.header.getStatus()) || Integer.valueOf(10).equals(source.header.getStatus()))
                && source.reconciled && !source.masked && enabled;
        result.setCanConfirm(editable);
        result.setCanApprove(editable && Integer.valueOf(10).equals(source.header.getStatus()) && "READY".equals(source.status));
        if (source.masked) { result.setReason("成本字段无查看权限，本次不能确认或审核无单成本"); }
        if (!source.masked && source.amounts.size() == source.items.size() && !source.items.isEmpty()) {
            result.setFinancialAmount(source.amounts.values().stream().map(value -> value.financial).reduce(BigDecimal.ZERO, BigDecimal::add));
            result.setSettlementAmount(source.amounts.values().stream().map(value -> value.settlement).reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        return result;
    }

    private boolean masked(Source source, List<ErpStockDO> stocks) {
        Set<Long> departments = new HashSet<>(); departments.add(source.header.getDeptId());
        source.items.forEach(item -> { if (item.getDeptId() != null) { departments.add(item.getDeptId()); } });
        stocks.forEach(stock -> departments.add(stock.getDeptId()));
        Set<String> fields = new HashSet<>();
        for (String module : Arrays.asList("erp_product", "erp_sale_return", "erp_stock")) {
            add(fields, permissionApi.getCurrentUserHiddenFields(module));
            for (Long department : departments) {
                if (department != null) { add(fields, permissionApi.getCurrentUserHiddenFields(module, department)); }
            }
        }
        return fields.stream().map(value -> value.replaceFirst("^(select_)?(item_)?(col_)?", "")).anyMatch(value ->
                Arrays.asList("purchasePrice", "lastPurchasePrice", "costPrice", "costAmount", "financialAmount", "settlementAmount",
                        "financialUnitCost", "settlementUnitCost", "financialMovement", "settlementMovement", "financialBalance", "settlementBalance").contains(value));
    }

    private Prepared prepared(ErpStockRecordCreateReqBO request) {
        Context context = context();
        Prepared prepared = context.prepared.get(key(request));
        if (prepared == null || prepared.tenant != tenant() || !prepared.signature.equals(postingSignature(request))
                || request.getSourceBizType() != null || request.getSourceBizId() != null || request.getSourceBizItemId() != null
                || request.getReversalPostingId() != null || request.getCostConfirmationId() != null
                || (request.getPostingActionKey() != null && !"APPROVE".equals(request.getPostingActionKey()))) {
            throw error("无单退货缺少本审核事务的真实成本准备或夹带其他来源");
        }
        return prepared;
    }

    private Context context() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) { throw error("无单退货核算必须处于业务事务"); }
        Context context = (Context) TransactionSynchronizationManager.getResource(CONTEXT);
        if (context != null) {
            if (context.tenant != tenant()) { throw error("无单退货成本上下文不能跨租户"); }
            return context;
        }
        Context created = new Context();
        TransactionSynchronizationManager.bindResource(CONTEXT, created);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void suspend() { TransactionSynchronizationManager.unbindResourceIfPossible(CONTEXT); }
            @Override public void resume() { TransactionSynchronizationManager.bindResource(CONTEXT, created); }
            @Override public void afterCompletion(int status) { TransactionSynchronizationManager.unbindResourceIfPossible(CONTEXT); }
        });
        return created;
    }

    private static void checkSignatures(Source source, String expectedSource, String expectedBasis) {
        if (!Objects.equals(source.sourceSignature, expectedSource) || !StringUtils.hasText(expectedBasis)
                || !Objects.equals(source.basisSignature, expectedBasis)) { throw error("来源或成本基准已变化，请刷新完整明细后重试"); }
    }
    private static void requireEditable(Source source) {
        if (!Integer.valueOf(0).equals(source.header.getStatus()) && !Integer.valueOf(10).equals(source.header.getStatus())) {
            throw error("仅草稿或未审核无单退货允许成本确认，已审核不能补确认");
        }
    }
    private static String signature(ErpSaleReturnDO header, List<ErpSaleReturnItemDO> items) {
        List<Object> fields = new ArrayList<>(); fields.add(tenant()); fields.add(ErpSaleReturnCostService.sourceSignature(header, items));
        fields.add(header.getOrderId()); fields.add(header.getOrderNo()); fields.add(header.getSourceOutNo());
        items.stream().sorted(Comparator.comparing(ErpSaleReturnItemDO::getId)).forEach(item -> { fields.add(item.getId()); fields.add(item.getOrderItemId()); });
        return hash(JsonUtils.toJsonString(fields));
    }
    private static String postingSignature(ErpStockRecordCreateReqBO request) {
        return hash(JsonUtils.toJsonString(Arrays.asList(tenant(), request.getBizType(), request.getBizId(), request.getBizItemId(),
                request.getProductId(), request.getWarehouseId(), request.getBatchNo(), request.getCount(), request.getUnitPrice(),
                request.getAccountingDeptId(), request.getFinancialMovementAmount(), request.getSettlementMovementAmount(), request.getBizNo(), request.getBizDate())));
    }
    private static String requestHash(ConfirmRequest request) {
        List<ConfirmItem> items = new ArrayList<>(request.getItems()); items.sort(Comparator.comparing(ConfirmItem::getSourceItemId));
        return hash(JsonUtils.toJsonString(Arrays.asList(tenant(), request.getId(), request.getExpectedSourceSignature(),
                request.getExpectedBasisSignature(), request.getExpectedRevision(), request.getEvidence(), items)));
    }
    static void validateRequest(ConfirmRequest request) {
        if (request == null || request.getId() == null || request.getExpectedRevision() == null || request.getExpectedRevision() < 0
                || !StringUtils.hasText(request.getExpectedSourceSignature()) || !StringUtils.hasText(request.getExpectedBasisSignature())
                || request.getRequestKey() == null || !request.getRequestKey().matches("[A-Za-z0-9_-]{1,80}")
                || !validText(request.getEvidence()) || request.getItems() == null || request.getItems().isEmpty()) { throw error("请提供完整成本确认版本和依据"); }
        for (ConfirmItem item : request.getItems()) {
            if (item == null || item.getSourceItemId() == null || !validAmount(item.getFinancialAmount())
                    || !validAmount(item.getSettlementAmount()) || !validText(item.getEvidence())) { throw error("两套行总成本必须明确，未知不可补0，并需填写依据"); }
        }
    }
    private static boolean validText(String value) { return StringUtils.hasText(value) && value.length() <= 500; }
    private static boolean validAmount(BigDecimal value) {
        if (value == null || value.signum() < 0) { return false; }
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() <= 6 && normalized.precision() - normalized.scale() <= 18;
    }
    static BigDecimal proportional(BigDecimal amount, BigDecimal quantity, BigDecimal basisQuantity) {
        BigDecimal result = amount.multiply(quantity).divide(basisQuantity, 6, RoundingMode.HALF_UP);
        if (!validAmount(result)) { throw error("本次退入成本超过可保存金额精度，请核对数量及成本依据"); }
        return result;
    }
    private static void add(Set<String> target, List<String> source) { if (source != null) { target.addAll(source); } }
    private static BigDecimal decimal(Map<String, Object> row, String field) { return row.get(field) == null ? null : new BigDecimal(row.get(field).toString()); }
    private static Long number(Map<String, Object> row, String field) { return row.get(field) == null ? null : ((Number) row.get(field)).longValue(); }
    private static boolean equal(BigDecimal first, BigDecimal second) { return first != null && second != null && first.compareTo(second) == 0; }
    private static LocalDateTime date(Object value) { return value instanceof java.sql.Timestamp ? ((java.sql.Timestamp) value).toLocalDateTime() : (LocalDateTime) value; }
    private static LocalDateTime now() { return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS); }
    private static long tenant() { return TenantContextHolder.getRequiredTenantId(); }
    private static long operator() { Long id = getLoginUserId(); if (id == null) { throw new ServiceException(403, "请先登录"); } return id; }
    private static int revision(Map<String, Object> confirmation) { return confirmation == null ? 0 : ((Number) confirmation.get("revision")).intValue(); }
    private static String key(ErpStockRecordCreateReqBO request) { return request.getBizId() + ":" + request.getBizItemId(); }
    private static String pair(Long product, Long warehouse) { return String.format("%020d:%020d", product, warehouse); }
    private static ServiceException error(String message) { return new ServiceException(409, message == null ? "无单退货成本不可用" : message); }
    private static String hash(String value) {
        try {
            StringBuilder output = new StringBuilder();
            for (byte part : MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))) { output.append(String.format("%02x", part & 255)); }
            return output.toString();
        } catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private static final class Source {
        private ErpSaleReturnDO header;
        private List<ErpSaleReturnItemDO> items;
        private Map<String, Object> confirmation;
        private Map<Long, Amounts> amounts = new HashMap<>();
        private String sourceSignature, basisSignature, stockSignature, stockBasis, status, reason;
        private boolean masked, reconciled, manualValid;
    }
    private static final class Amounts {
        private final BigDecimal financial, settlement;
        private final String kind;
        private Amounts(BigDecimal financial, BigDecimal settlement, String kind) { this.financial = financial; this.settlement = settlement; this.kind = kind; }
    }
    private static final class Context {
        private final long tenant = tenant();
        private final Map<Long, String> approvalTokens = new HashMap<>();
        private final Set<Long> claimed = new HashSet<>();
        private final Map<String, Prepared> prepared = new HashMap<>();
    }
    private static final class Prepared {
        private final long tenant = tenant();
        private final String signature, sourceSignature, basisSignature, stockSignature, stockBasis, kind;
        private final Long confirmationId;
        private final Integer revision;
        private boolean posted;
        private Prepared(ErpStockRecordCreateReqBO request, Source source, Amounts amounts) {
            signature = postingSignature(request); sourceSignature = source.sourceSignature; basisSignature = source.basisSignature;
            stockSignature = source.stockSignature; stockBasis = source.stockBasis; kind = amounts.kind;
            confirmationId = source.manualValid ? number(source.confirmation, "id") : null;
            revision = source.manualValid ? revision(source.confirmation) : null;
        }
    }
}
