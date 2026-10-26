package cn.iocoder.yudao.module.erp.service.purchase.history;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history.ErpPurchaseHistoryPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history.ErpPurchaseHistoryRespVO;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ErpPurchaseHistoryRepository {

    @Resource
    private NamedParameterJdbcTemplate jdbc;

    public long count(ErpPurchaseHistoryPageReqVO filter, Scope scope) {
        Sql sql = buildSql(filter, scope);
        Long value = jdbc.queryForObject("SELECT COUNT(*) " + sql.from, sql.params, Long.class);
        return value == null ? 0L : value;
    }

    public List<ErpPurchaseHistoryRespVO> page(ErpPurchaseHistoryPageReqVO filter, Scope scope,
                                               long offset, int limit) {
        Sql sql = buildSql(filter, scope);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        return jdbc.query("SELECT history.* " + sql.from + orderBy(filter) + " LIMIT :limit OFFSET :offset",
                sql.params, new BeanPropertyRowMapper<>(ErpPurchaseHistoryRespVO.class));
    }

    private Sql buildSql(ErpPurchaseHistoryPageReqVO filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenantId", scope.tenantId);
        sql.params.put("productId", filter.getProductId());
        List<String> branches = new ArrayList<>();
        if (scope.includePurchaseIn && includesDocumentType(filter, "PURCHASE_IN")) {
            branches.add(purchaseInSelect(documentScope(scope.purchaseInScope, "pi.dept_id", "pi.creator",
                    "purchaseIn", sql.params)));
        }
        if (scope.includePurchaseReturn && includesDocumentType(filter, "PURCHASE_RETURN")) {
            branches.add(purchaseReturnSelect(documentScope(scope.purchaseReturnScope, "pr.dept_id", "pr.creator",
                    "purchaseReturn", sql.params)));
        }
        if (branches.isEmpty()) {
            sql.from = "FROM (SELECT NULL AS row_key,NULL AS business_id,NULL AS item_id,NULL AS document_type_code,"
                    + "NULL AS document_type,NULL AS document_no,NULL AS supplier_id,NULL AS supplier_name,"
                    + "NULL AS count,NULL AS product_price,NULL AS business_time,NULL AS warehouse_name,"
                    + "NULL AS creator_name,NULL AS remark) history WHERE 1=0";
            return sql;
        }
        sql.from = "FROM (" + String.join(" UNION ALL ", branches) + ") history WHERE 1=1";
        if (filter.getSupplierId() != null) {
            sql.from += " AND history.supplier_id=:supplierId";
            sql.params.put("supplierId", filter.getSupplierId());
        }
        if (filter.getBusinessTimeFrom() != null) {
            sql.from += " AND history.business_time>=:businessTimeFrom";
            sql.params.put("businessTimeFrom", filter.getBusinessTimeFrom());
        }
        if (filter.getBusinessTimeTo() != null) {
            sql.from += " AND history.business_time<=:businessTimeTo";
            sql.params.put("businessTimeTo", filter.getBusinessTimeTo());
        }
        return sql;
    }

    private boolean includesDocumentType(ErpPurchaseHistoryPageReqVO filter, String documentTypeCode) {
        return filter.getDocumentTypeCode() == null
                || documentTypeCode.equals(filter.getDocumentTypeCode());
    }

    private String purchaseInSelect(String documentScope) {
        return "SELECT CONCAT('PURCHASE_IN-',pii.id) AS row_key,pi.id AS business_id,pii.id AS item_id,"
                + "'PURCHASE_IN' AS document_type_code,'采购入库' AS document_type,pi.no AS document_no,"
                + "pi.supplier_id,s.name AS supplier_name,COALESCE(pii.count,0) AS count,"
                + "pii.product_price AS product_price,pi.in_time AS business_time,w.name AS warehouse_name,"
                + "cu.nickname AS creator_name,pi.remark AS remark "
                + "FROM erp_purchase_in_items pii INNER JOIN erp_purchase_in pi ON pi.id=pii.in_id "
                + "AND pi.tenant_id=pii.tenant_id AND pi.deleted=FALSE AND pi.status=20 "
                + "LEFT JOIN erp_supplier s ON s.id=pi.supplier_id AND s.tenant_id=pi.tenant_id AND s.deleted=FALSE "
                + "LEFT JOIN erp_warehouse w ON w.id=pii.warehouse_id AND w.tenant_id=pii.tenant_id AND w.deleted=FALSE "
                + "LEFT JOIN system_users cu ON cu.id=pi.creator AND cu.tenant_id=pi.tenant_id AND cu.deleted=FALSE "
                + "WHERE pii.tenant_id=:tenantId AND pii.deleted=FALSE AND pii.product_id=:productId"
                + documentScope;
    }

    private String purchaseReturnSelect(String documentScope) {
        return "SELECT CONCAT('PURCHASE_RETURN-',pri.id) AS row_key,pr.id AS business_id,pri.id AS item_id,"
                + "'PURCHASE_RETURN' AS document_type_code,'采购退货' AS document_type,pr.no AS document_no,"
                + "pr.supplier_id,s.name AS supplier_name,-ABS(COALESCE(pri.count,0)) AS count,"
                + "ABS(pri.product_price) AS product_price,pr.return_time AS business_time,w.name AS warehouse_name,"
                + "cu.nickname AS creator_name,pr.remark AS remark "
                + "FROM erp_purchase_return_items pri INNER JOIN erp_purchase_return pr ON pr.id=pri.return_id "
                + "AND pr.tenant_id=pri.tenant_id AND pr.deleted=FALSE AND pr.status=20 "
                + "LEFT JOIN erp_supplier s ON s.id=pr.supplier_id AND s.tenant_id=pr.tenant_id AND s.deleted=FALSE "
                + "LEFT JOIN erp_warehouse w ON w.id=pri.warehouse_id AND w.tenant_id=pri.tenant_id AND w.deleted=FALSE "
                + "LEFT JOIN system_users cu ON cu.id=pr.creator AND cu.tenant_id=pr.tenant_id AND cu.deleted=FALSE "
                + "WHERE pri.tenant_id=:tenantId AND pri.deleted=FALSE AND pri.product_id=:productId"
                + documentScope;
    }

    private String documentScope(ErpFinanceVisibleScope scope, String deptColumn, String userColumn,
                                 String prefix, Map<String, Object> params) {
        if (scope == null || !scope.hasAccess()) {
            return " AND 1=0";
        }
        if (scope.isAll()) {
            return "";
        }
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append(deptColumn).append(" IN (:").append(prefix).append("DeptIds)");
            params.put(prefix + "DeptIds", scope.getDeptIds());
            appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) {
                condition.append(" OR ");
            }
            condition.append(userColumn).append("=:").append(prefix).append("SelfUserId");
            params.put(prefix + "SelfUserId", String.valueOf(scope.getSelfUserId()));
            appended = true;
        }
        return appended ? condition.append(')').toString() : " AND 1=0";
    }

    private String orderBy(ErpPurchaseHistoryPageReqVO filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("documentNo", "history.document_no");
        fields.put("documentType", "history.document_type");
        fields.put("supplierName", "history.supplier_name");
        fields.put("count", "history.count");
        fields.put("productPrice", "history.product_price");
        fields.put("businessTime", "history.business_time");
        fields.put("warehouseName", "history.warehouse_name");
        fields.put("creatorName", "history.creator_name");
        fields.put("remark", "history.remark");
        String field = fields.getOrDefault(filter.getOrderField(), "history.business_time");
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + field + direction + ",history.document_no" + direction
                + ",history.item_id" + direction;
    }

    public static class Scope {
        private final long tenantId;
        private final boolean includePurchaseIn;
        private final boolean includePurchaseReturn;
        private final ErpFinanceVisibleScope purchaseInScope;
        private final ErpFinanceVisibleScope purchaseReturnScope;

        public Scope(long tenantId, boolean includePurchaseIn, boolean includePurchaseReturn,
                     ErpFinanceVisibleScope purchaseInScope, ErpFinanceVisibleScope purchaseReturnScope) {
            this.tenantId = tenantId;
            this.includePurchaseIn = includePurchaseIn;
            this.includePurchaseReturn = includePurchaseReturn;
            this.purchaseInScope = purchaseInScope;
            this.purchaseReturnScope = purchaseReturnScope;
        }
    }

    private static class Sql {
        private String from;
        private final Map<String, Object> params = new HashMap<>();
    }
}
