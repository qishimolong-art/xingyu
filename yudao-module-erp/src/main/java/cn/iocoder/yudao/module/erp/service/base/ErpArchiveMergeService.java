package cn.iocoder.yudao.module.erp.service.base;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpArchiveMergeLogDO;
import cn.iocoder.yudao.module.erp.dal.mysql.base.ErpArchiveMergeLogMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.base.ErpArchiveMergeReferenceMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ErpArchiveMergeService {

    private static final int PARTY_TYPE_CUSTOMER = 1;
    private static final int PARTY_TYPE_SUPPLIER = 2;
    private static final int PRICE_PARTNER_TYPE_SUPPLIER = 1;
    private static final int PRICE_PARTNER_TYPE_CUSTOMER = 2;
    private static final Set<String> NON_TENANT_SCOPED_REFERENCE_TABLES = Collections.unmodifiableSet(new HashSet<>(
            Collections.singletonList("erp_product_universal")));
    private static final String TENANT_ID_COLUMN = "tenant_id";

    @Resource
    private ErpArchiveMergeReferenceMapper referenceMapper;
    @Resource
    private ErpArchiveMergeLogMapper mergeLogMapper;
    private final Map<String, Boolean> tenantScopedCache = new ConcurrentHashMap<>();

    public void mergeSupplierReferences(Long sourceId, Long keepId, String operatorId) {
        DataPermissionUtils.executeIgnore(() -> doMergeSupplierReferences(sourceId, keepId, operatorId));
    }

    private void doMergeSupplierReferences(Long sourceId, Long keepId, String operatorId) {
        MergeResult result = new MergeResult();
        updateLong(result, "erp_purchase_order", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_purchase_order_items", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_purchase_in", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_purchase_return", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_purchase_invoice", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_purchase_price_adjust", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_purchase_suggestion_item", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock_in", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_finance_payment", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_payable_other", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_payable_writeoff", "supplier_id", sourceId, keepId, operatorId);
        updatePricePartner(result, sourceId, keepId, PRICE_PARTNER_TYPE_SUPPLIER, operatorId);
        updateLong(result, "erp_supplier_contact", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_supplier_contract", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_supplier_image", "supplier_id", sourceId, keepId, operatorId);
        updateUniqueLong(result, "erp_supplier_task", "supplier_id", sourceId, keepId,
                Arrays.asList("year", "month", "task_level"), operatorId);
        updateLong(result, "erp_supplier_business_info", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_supplier_extend", "supplier_id", sourceId, keepId, operatorId);
        updateUniqueLong(result, "erp_supplier_extend_info", "supplier_id", sourceId, keepId,
                Collections.emptyList(), operatorId);
        updateLong(result, "erp_supplier_bill", "supplier_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_supplier_account", "supplier_id", sourceId, keepId, operatorId);
        updateUniqueLong(result, "erp_supplier_dept", "supplier_id", sourceId, keepId,
                Collections.singletonList("dept_id"), operatorId);
        updateParty(result, "erp_pre_payment", sourceId, keepId, PARTY_TYPE_SUPPLIER, operatorId);
        updateParty(result, "erp_other_payable", sourceId, keepId, PARTY_TYPE_SUPPLIER, operatorId);
        insertLog("supplier", sourceId, keepId, result);
    }

    public void mergeCustomerReferences(Long sourceId, Long keepId, String operatorId) {
        DataPermissionUtils.executeIgnore(() -> doMergeCustomerReferences(sourceId, keepId, operatorId));
    }

    private void doMergeCustomerReferences(Long sourceId, Long keepId, String operatorId) {
        MergeResult result = new MergeResult();
        updateLong(result, "erp_sale_quote", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_sale_cart", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_sale_order", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_sale_out", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_sale_return", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_sale_price_adjust", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_chain_order", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock_out", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_finance_receipt", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_receivable_other", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_receivable_writeoff", "customer_id", sourceId, keepId, operatorId);
        updatePricePartner(result, sourceId, keepId, PRICE_PARTNER_TYPE_CUSTOMER, operatorId);
        updateLong(result, "erp_customer_contact", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_customer_contract", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_customer_image", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_customer_task", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_customer_area", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_customer_business_info", "customer_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_customer_extend", "customer_id", sourceId, keepId, operatorId);
        updateUniqueLong(result, "erp_customer_extend_info", "customer_id", sourceId, keepId,
                Collections.emptyList(), operatorId);
        updateUniqueLong(result, "erp_customer_dept", "customer_id", sourceId, keepId,
                Collections.singletonList("dept_id"), operatorId);
        updateUniqueLong(result, "erp_customer_dept_credit", "customer_id", sourceId, keepId,
                Collections.singletonList("dept_id"), operatorId);
        updateParty(result, "erp_pre_receipt", sourceId, keepId, PARTY_TYPE_CUSTOMER, operatorId);
        updateParty(result, "erp_pre_receivable", sourceId, keepId, PARTY_TYPE_CUSTOMER, operatorId);
        updateParty(result, "erp_other_receivable", sourceId, keepId, PARTY_TYPE_CUSTOMER, operatorId);
        insertLog("customer", sourceId, keepId, result);
    }

    public void mergeProductReferences(Long sourceId, Long keepId, String sourceCode, String keepCode, String operatorId) {
        DataPermissionUtils.executeIgnore(() -> doMergeProductReferences(sourceId, keepId, sourceCode, keepCode, operatorId));
    }

    private void doMergeProductReferences(Long sourceId, Long keepId, String sourceCode, String keepCode, String operatorId) {
        MergeResult result = new MergeResult();
        updateLong(result, "erp_purchase_order_items", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_purchase_in_items", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_purchase_return_items", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_purchase_invoice_item", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_purchase_price_adjust_item", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_sale_quote_items", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_sale_cart_items", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_sale_order_items", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_sale_out_items", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_sale_return_items", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_sale_price_adjust_item", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_chain_order_item", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_purchase_suggestion_item", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_auto_order_rule", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock_in_item", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock_in_bill_item", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock_in_bill_pickup_record", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock_out_item", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock_out_bill_item", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock_out_bill_pick_record", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock_move_item", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock_check_item", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock_record", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_stock_lock", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_vehicle_product_fit", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_product_price_system", "product_id", sourceId, keepId, operatorId);
        updateLong(result, "erp_price_history", "product_id", sourceId, keepId, operatorId);
        updateUniqueLong(result, "erp_product_dept", "product_id", sourceId, keepId,
                Collections.singletonList("dept_id"), operatorId);
        updateLong(result, "erp_product_universal", "product_id", sourceId, keepId, operatorId);
        if (sourceCode != null && keepCode != null) {
            updateUniqueString(result, "erp_product_universal", "universal_code", sourceCode, keepCode,
                    Arrays.asList("product_id", "universal_vehicle"), operatorId);
        }
        insertLog("product", sourceId, keepId, result);
    }

    public void validateProductStockMergeConflict(Long sourceId, Long keepId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long stockConflict = referenceMapper.selectProductStockWarehouseConflictCount(sourceId, keepId, tenantId);
        Long stockLockConflict = referenceMapper.selectProductStockLockWarehouseConflictCount(sourceId, keepId, tenantId);
        if ((stockConflict != null && stockConflict > 0) || (stockLockConflict != null && stockLockConflict > 0)) {
            throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRODUCT_MERGE_STOCK_CONFLICT);
        }
    }

    private void updateLong(MergeResult result, String tableName, String columnName,
                            Long sourceId, Long keepId, String operatorId) {
        int rows = referenceMapper.updateLongReference(tableName, columnName, sourceId, keepId,
                null, null, operatorId, TenantContextHolder.getRequiredTenantId(), isTenantScoped(tableName));
        result.add(tableName, rows);
    }

    private void updateUniqueLong(MergeResult result, String tableName, String columnName,
                                  Long sourceId, Long keepId, List<String> matchColumns, String operatorId) {
        int deletedRows = referenceMapper.deleteConflictingLongReference(tableName, columnName, sourceId, keepId,
                matchColumns, operatorId, TenantContextHolder.getRequiredTenantId(), isTenantScoped(tableName));
        result.add(tableName + "(conflict-deleted)", deletedRows);
        updateLong(result, tableName, columnName, sourceId, keepId, operatorId);
    }

    private void updateParty(MergeResult result, String tableName, Long sourceId, Long keepId,
                             Integer partyType, String operatorId) {
        int rows = referenceMapper.updateLongReference(tableName, "party_id", sourceId, keepId,
                "party_type", partyType, operatorId, TenantContextHolder.getRequiredTenantId(), isTenantScoped(tableName));
        result.add(tableName, rows);
    }

    private void updatePricePartner(MergeResult result, Long sourceId, Long keepId, Integer partnerType,
                                    String operatorId) {
        int rows = referenceMapper.updateLongReference("erp_price_history", "partner_id", sourceId, keepId,
                "partner_type", partnerType, operatorId, TenantContextHolder.getRequiredTenantId(), isTenantScoped("erp_price_history"));
        result.add("erp_price_history", rows);
    }

    private void updateString(MergeResult result, String tableName, String columnName,
                              String sourceValue, String keepValue, String operatorId) {
        int rows = referenceMapper.updateStringReference(tableName, columnName, sourceValue, keepValue, operatorId,
                TenantContextHolder.getRequiredTenantId(), isTenantScoped(tableName));
        result.add(tableName, rows);
    }

    private void updateUniqueString(MergeResult result, String tableName, String columnName,
                                    String sourceValue, String keepValue, List<String> matchColumns,
                                    String operatorId) {
        int deletedRows = referenceMapper.deleteConflictingStringReference(tableName, columnName, sourceValue, keepValue,
                matchColumns, operatorId, TenantContextHolder.getRequiredTenantId(), isTenantScoped(tableName));
        result.add(tableName + "(conflict-deleted)", deletedRows);
        updateString(result, tableName, columnName, sourceValue, keepValue, operatorId);
    }

    private void insertLog(String archiveType, Long sourceId, Long keepId, MergeResult result) {
        mergeLogMapper.insert(ErpArchiveMergeLogDO.builder()
                .archiveType(archiveType)
                .sourceId(sourceId)
                .keepId(keepId)
                .affectedTables(result.affectedTables())
                .affectedRows(result.affectedRows)
                .build());
    }

    private boolean isTenantScoped(String tableName) {
        if (NON_TENANT_SCOPED_REFERENCE_TABLES.contains(tableName)) {
            return false;
        }
        return tenantScopedCache.computeIfAbsent(tableName, this::hasTenantIdColumn);
    }

    private boolean hasTenantIdColumn(String tableName) {
        Long count = referenceMapper.selectTableColumnCount(tableName, TENANT_ID_COLUMN);
        return count != null && count > 0;
    }

    private static class MergeResult {
        private final List<TableResult> tableResults = new ArrayList<>();
        private int affectedRows;

        private void add(String tableName, int rows) {
            if (rows <= 0) {
                return;
            }
            tableResults.add(new TableResult(tableName, rows));
            affectedRows += rows;
        }

        private String affectedTables() {
            if (CollUtil.isEmpty(tableResults)) {
                return "";
            }
            return tableResults.stream()
                    .map(item -> item.tableName + ":" + item.rows)
                    .collect(Collectors.joining(","));
        }
    }

    private static class TableResult {
        private final String tableName;
        private final int rows;

        private TableResult(String tableName, int rows) {
            this.tableName = tableName;
            this.rows = rows;
        }
    }

}
