package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerTotalRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.settlement.ErpSettlementOffsetDO;
import cn.iocoder.yudao.module.erp.service.finance.settlement.ErpSettlementOffsetService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockTransferLedgerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 阶段 5 固定业务能力。
 *
 * <p>所有表名、列名和关联关系均由服务端登记；模型只负责把自然语言路由到工具，不能提供 SQL。
 * 查询前先完成菜单权限、字段权限及部门/本人范围裁剪，之后才使用独立只读数据源执行参数化 SQL。</p>
 */
@Service
public class AssistantStage5BusinessQuery {

    public static final String MASTER_TOOL = "query_master_data_details";
    public static final String ORGANIZATION_TOOL = "query_user_departments";
    public static final String STOCK_TOOL = "query_stock_documents";
    public static final String SALE_TOOL = "query_sale_business";
    public static final String PURCHASE_TOOL = "query_purchase_business";
    public static final String FINANCE_TOOL = "query_finance_business";
    public static final List<String> TOOLS = Collections.unmodifiableList(Arrays.asList(
            MASTER_TOOL, ORGANIZATION_TOOL, STOCK_TOOL, SALE_TOOL, PURCHASE_TOOL, FINANCE_TOOL));

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern QUOTED = Pattern.compile("[“\"']([^”\"']{1,80})[”\"']");
    private static final Pattern DOCUMENT_NO = Pattern.compile("(?i)([A-Z]{1,12}(?:[-_][A-Z0-9]+){1,8})");
    private static final Pattern YEAR_MONTH = Pattern.compile("(20\\d{2})[年/-](1[0-2]|0?[1-9])月?");
    private static final Pattern DEPARTMENT_NAME = Pattern.compile("([\\p{IsHan}A-Za-z0-9]{1,30}(?:事业部|办事处|销售部|采购部|财务部|部门|中心|分公司|公司))");

    @Resource private AssistantSemanticReadOnly reader;
    @Resource private AssistantToolPermissionService toolPermissions;
    @Resource private PermissionApi permissions;
    @Resource private DeptApi departments;
    @Resource private ErpSettlementOffsetService settlementOffsets;
    @Resource private ErpStockTransferLedgerService transferLedger;

    private static final Map<String, Spec> SPECS = createSpecs();

    public boolean available(String tool) {
        if (!TOOLS.contains(tool)) return false;
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null || TenantContextHolder.getTenantId() == null || TenantContextHolder.isIgnore()
                || !permissions.hasAnyPermissions(userId, "erp:assistant:query")) return false;
        if (ORGANIZATION_TOOL.equals(tool)) {
            return permissions.hasAnyPermissions(userId, "system:user:query", "system:dept:query");
        }
        if (STOCK_TOOL.equals(tool) && permissions.hasAnyPermissions(userId, "erp:stock-transfer-ledger:query")) return true;
        for (Spec spec : SPECS.values()) {
            if (tool.equals(spec.tool) && permissions.hasAnyPermissions(userId, spec.permission)) return true;
        }
        return false;
    }

    public Map<String, Object> execute(String tool, String question, AssistantExecutionEnvelope envelope) {
        if (ORGANIZATION_TOOL.equals(tool)) return executeOrganization(question, envelope);
        if (STOCK_TOOL.equals(tool) && compact(question).contains("调拨台账")) return executeTransferLedger(question);
        if (FINANCE_TOOL.equals(tool) && compact(question).contains("往来对冲")) return executeSettlementOffset(question);
        List<Spec> selected = selectSpecs(tool, question);
        if (selected.isEmpty()) throw new AssistantFailure("CLARIFY", "请说明要查询的具体业务对象或单据类型");
        List<Map<String, Object>> summary = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> basis = new ArrayList<>();
        for (Spec spec : selected) {
            QueryParts parts = prepare(spec, question, envelope);
            if ("purchase_in".equals(spec.id) && purchaserGrouping(question)) {
                List<Map<String, Object>> grouped = reader.query(purchaserGroupSql(spec, parts), parts.parameters, 50);
                summary.addAll(grouped);
                for (Map<String, Object> row : grouped) row.put("businessObject", spec.title);
                rows.addAll(grouped);
                basis.add(spec.title + "（" + spec.permission + "；按采购员分组）");
                continue;
            }
            summary.addAll(reader.query(summarySql(spec, parts), parts.parameters, 1));
            if (rows.size() < 100) {
                List<Map<String, Object>> details = reader.query(detailSql(spec, parts), parts.parameters,
                        Math.min(50, 100 - rows.size()));
                for (Map<String, Object> row : details) row.put("businessObject", spec.title);
                rows.addAll(details);
            }
            basis.add(spec.title + "（" + spec.permission + "）");
        }
        Map<String, Object> result = baseResult(selected.size() == 1 ? selected.get(0).title + "查询" : "业务单据综合查询");
        result.put("summary", summary);
        result.put("rows", rows);
        result.put("columns", columns(rows));
        result.put("basis", basis);
        result.put("status", empty(summary, rows) ? "EMPTY" : "SUCCESS");
        result.put("capabilityBatch", batch(tool));
        result.put("queryTrace", trace(tool, selected, rows.size()));
        return result;
    }

    private Map<String, Object> executeSettlementOffset(String question) {
        toolPermissions.authorize("erp:settlement-offset:query", "erp_finance_settlement_offset",
                Arrays.asList("subjectName", "receivableBalance", "payableBalance", "offsetBalance"));
        ErpSettlementOffsetPageReqVO request = new ErpSettlementOffsetPageReqVO();
        request.setPageNo(1); request.setPageSize(100); request.setShowZeroBalance(false);
        request.setOrderField("offsetBalance"); request.setOrderDirection("desc");
        request.setSubjectName(quoted(question));
        LocalDateTime[] range = timeRange(question, LocalDateTime.now(BUSINESS_ZONE));
        if (range != null) request.setBizTime(range);
        PageResult<ErpSettlementOffsetDO> page = settlementOffsets.getSettlementOffsetPage(request);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (page != null && page.getList() != null) for (ErpSettlementOffsetDO value : page.getList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", value.getSubjectName());
            row.put("receivable_balance", value.getReceivableBalance());
            row.put("payable_balance", value.getPayableBalance());
            row.put("offset_balance", value.getOffsetBalance());
            rows.add(row);
        }
        Map<String, Object> summaryRow = new LinkedHashMap<>();
        summaryRow.put("capability_id", "settlement_offset"); summaryRow.put("business_object", "往来对冲派生视图");
        summaryRow.put("record_count", page == null ? 0L : page.getTotal());
        Map<String, Object> result = baseResult("往来对冲查询");
        result.put("summary", Collections.singletonList(summaryRow)); result.put("rows", rows); result.put("columns", columns(rows));
        result.put("basis", Arrays.asList("应收余额", "应付余额", "往来对冲派生差额（非独立业务单）"));
        result.put("status", rows.isEmpty() ? "EMPTY" : "SUCCESS"); result.put("capabilityBatch", "5E");
        result.put("queryTrace", trace(FINANCE_TOOL, Collections.<Spec>emptyList(), rows.size()));
        return result;
    }

    private Map<String, Object> executeTransferLedger(String question) {
        toolPermissions.authorize("erp:stock-transfer-ledger:query", "erp_stock_transfer_ledger",
                Arrays.asList("businessDate", "transferOutCount", "transferInCount", "differenceCount"));
        LocalDateTime[] range = timeRange(question, LocalDateTime.now(BUSINESS_ZONE));
        if (range == null) {
            LocalDate start = LocalDate.now(BUSINESS_ZONE).withDayOfMonth(1);
            range = new LocalDateTime[]{start.atStartOfDay(), start.plusMonths(1).atStartOfDay()};
        }
        ErpStockTransferLedgerPageReqVO request = new ErpStockTransferLedgerPageReqVO();
        request.setPageNo(1); request.setPageSize(100);
        request.setMoveTime(new LocalDateTime[]{range[0], range[1].minusNanos(1)});
        request.setOrderDirection("desc");
        String text = compact(question);
        if (text.contains("异常")) request.setMatchStatus("ABNORMAL");
        else if (text.contains("正常")) request.setMatchStatus("NORMAL");
        String no = documentNo(question);
        if (no != null) request.setKeyword(no);
        PageResult<ErpStockTransferLedgerSummaryRespVO> page = transferLedger.getSummaryPage(request);
        ErpStockTransferLedgerTotalRespVO total = transferLedger.getTotal(request);
        List<Map<String, Object>> rows = new ArrayList<>();
        long documentCount = 0L;
        if (page != null && page.getList() != null) for (ErpStockTransferLedgerSummaryRespVO value : page.getList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("business_date", value.getBusinessDate());
            row.put("transfer_out_document_count", value.getTransferOutDocumentCount());
            row.put("transfer_out_count", value.getTransferOutCount());
            row.put("transfer_in_document_count", value.getTransferInDocumentCount());
            row.put("transfer_in_count", value.getTransferInCount());
            row.put("difference_count", value.getDifferenceCount());
            row.put("abnormal_group_count", value.getAbnormalGroupCount());
            row.put("ledger_status", value.getLedgerStatus());
            rows.add(row);
            documentCount += value.getTransferOutDocumentCount() == null ? 0L : value.getTransferOutDocumentCount();
            documentCount += value.getTransferInDocumentCount() == null ? 0L : value.getTransferInDocumentCount();
        }
        Map<String, Object> summaryRow = new LinkedHashMap<>();
        summaryRow.put("capability_id", "stock_transfer_ledger");
        summaryRow.put("business_object", "调拨出入库台账");
        summaryRow.put("record_count", documentCount);
        summaryRow.put("transfer_out_count", total == null ? null : total.getTransferOutCount());
        summaryRow.put("transfer_in_count", total == null ? null : total.getTransferInCount());
        summaryRow.put("difference_count", total == null ? null : total.getDifferenceCount());
        summaryRow.put("abnormal_group_count", total == null ? null : total.getAbnormalGroupCount());
        Map<String, Object> result = baseResult("调拨出入库台账查询");
        result.put("summary", Collections.singletonList(summaryRow));
        result.put("rows", rows); result.put("columns", columns(rows));
        result.put("basis", Collections.singletonList("调拨出入库业务配对（erp:stock-transfer-ledger:query）"));
        result.put("status", documentCount == 0L ? "EMPTY" : "SUCCESS"); result.put("capabilityBatch", "5B");
        result.put("queryTrace", trace(STOCK_TOOL, Collections.<Spec>emptyList(), rows.size()));
        return result;
    }

    static List<String> detectCapabilityIds(String tool, String question) {
        if (STOCK_TOOL.equals(tool) && compact(question).contains("调拨台账"))
            return Collections.singletonList("stock_transfer_ledger");
        List<String> result = new ArrayList<>();
        for (Spec spec : selectSpecs(tool, question)) result.add(spec.id);
        return result;
    }

    private QueryParts prepare(Spec spec, String question, AssistantExecutionEnvelope envelope) {
        toolPermissions.authorize(spec.permission, spec.module, spec.protectedFields);
        Map<String, Object> scope = new LinkedHashMap<>();
        if (spec.scoped) toolPermissions.documentScope(scope, "scope", spec.module, true);
        if (spec.warehouseScoped) toolPermissions.stockScope(scope);
        List<Object> parameters = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE t.tenant_id=? AND t.deleted=0");
        parameters.add(TenantContextHolder.getRequiredTenantId());
        applyScope(spec, scope, where, parameters);
        applyWarehouseScope(spec, scope, where, parameters);
        applyFixedConditions(spec, where, parameters);
        applyStatus(spec, compact(question), where);
        applyTime(spec, question, where, parameters);
        applyKeyword(spec, question, where, parameters);
        applySubtype(spec, question, where, parameters);
        applyEntity(spec, envelope, where, parameters);
        return new QueryParts(where.toString(), parameters);
    }

    private static String summarySql(Spec spec, QueryParts parts) {
        String amount = spec.amountColumn == null ? "NULL" : "COALESCE(SUM(t." + spec.amountColumn + "),0)";
        String quantity = spec.quantityColumn == null ? "NULL" : "COALESCE(SUM(t." + spec.quantityColumn + "),0)";
        return "SELECT '" + spec.id + "' capability_id,'" + spec.title + "' business_object,COUNT(DISTINCT t.id) record_count,"
                + amount + " total_amount," + quantity + " total_quantity FROM " + spec.table + " t" + spec.joinClause + parts.where;
    }

    private static String detailSql(Spec spec, QueryParts parts) {
        List<String> fields = new ArrayList<>();
        fields.add("t.id id");
        if (spec.labelColumn != null) fields.add("t." + spec.labelColumn + " label");
        if (spec.statusColumn != null) fields.add(statusExpression(spec) + " status_name");
        if (spec.timeColumn != null) fields.add("t." + spec.timeColumn + " biz_time");
        if (spec.quantityColumn != null) fields.add("t." + spec.quantityColumn + " quantity");
        if (spec.amountColumn != null) fields.add("t." + spec.amountColumn + " amount");
        if ("purchase_in".equals(spec.id)) fields.add("pu.nickname purchaser_name");
        for (String extra : spec.extraColumns) fields.add("t." + extra + " " + extra);
        String order = spec.timeColumn == null ? "t.id DESC" : "t." + spec.timeColumn + " DESC,t.id DESC";
        return "SELECT " + String.join(",", fields) + " FROM " + spec.table + " t" + spec.joinClause + parts.where
                + " ORDER BY " + order + " LIMIT 50";
    }

    private static boolean purchaserGrouping(String question) {
        return compact(question).matches(".*(?:按|每个|各个|各)采购员.*");
    }

    private static String purchaserGroupSql(Spec spec, QueryParts parts) {
        String amount = spec.amountColumn == null ? "NULL" : "COALESCE(SUM(t." + spec.amountColumn + "),0)";
        String quantity = spec.quantityColumn == null ? "NULL" : "COALESCE(SUM(t." + spec.quantityColumn + "),0)";
        return "SELECT COALESCE(pu.nickname,'未指定采购员') purchaser_name,COUNT(DISTINCT t.id) record_count,"
                + amount + " total_amount," + quantity + " total_quantity FROM " + spec.table + " t"
                + spec.joinClause + parts.where + " GROUP BY t.purchaser,pu.nickname ORDER BY total_amount DESC,purchaser_name LIMIT 50";
    }

    private static String statusExpression(Spec spec) {
        if (spec.statusProfile == StatusProfile.MASTER) return "CASE t." + spec.statusColumn + " WHEN 0 THEN '启用' WHEN 1 THEN '停用' ELSE CONCAT('状态',t."
                + spec.statusColumn + ") END";
        if (spec.statusProfile == StatusProfile.WRITEOFF) return "CASE t." + spec.statusColumn + " WHEN 0 THEN '待生效' WHEN 1 THEN '有效核销' WHEN 2 THEN '已反核销' ELSE CONCAT('状态',t."
                + spec.statusColumn + ") END";
        if (spec.statusProfile == StatusProfile.STOCK_IN_BILL) return "CASE t." + spec.statusColumn
                + " WHEN 10 THEN '待提货' WHEN 20 THEN '部分提货' WHEN 30 THEN '已完成' ELSE CONCAT('状态',t." + spec.statusColumn + ") END";
        if (spec.statusProfile == StatusProfile.STOCK_OUT_BILL) return "CASE t." + spec.statusColumn
                + " WHEN 10 THEN '待拣货' WHEN 20 THEN '部分拣货' WHEN 30 THEN '已完成' ELSE CONCAT('状态',t." + spec.statusColumn + ") END";
        if (spec.statusProfile == StatusProfile.PICK) return "CASE t." + spec.statusColumn
                + " WHEN 10 THEN '待拣货' WHEN 20 THEN '部分拣货' WHEN 30 THEN '已拣货' ELSE CONCAT('状态',t." + spec.statusColumn + ") END";
        if (spec.statusProfile == StatusProfile.DELIVERY) return "CASE t." + spec.statusColumn
                + " WHEN 5 THEN '待拣货' WHEN 10 THEN '待送货' WHEN 20 THEN '部分送货' WHEN 30 THEN '已送货' ELSE CONCAT('状态',t." + spec.statusColumn + ") END";
        if (spec.statusProfile == StatusProfile.OCR) return "CASE t." + spec.statusColumn
                + " WHEN 'DRAFT' THEN '已上传' WHEN 'RECOGNIZING' THEN '识别中' WHEN 'RECOGNIZED' THEN '已识别'"
                + " WHEN 'MATCHED' THEN '已匹配完成' WHEN 'CONFIRMED' THEN '已确认生成采购票据' WHEN 'FAILED' THEN '处理失败'"
                + " ELSE CONCAT('状态',t." + spec.statusColumn + ") END";
        return "CASE t." + spec.statusColumn + " WHEN 20 THEN '已审核' WHEN 10 THEN '待审核' WHEN 0 THEN '草稿' WHEN 30 THEN '已作废' ELSE CONCAT('状态',t."
                + spec.statusColumn + ") END";
    }

    private static void applyScope(Spec spec, Map<String, Object> scope, StringBuilder where, List<Object> parameters) {
        if (!spec.scoped || Boolean.TRUE.equals(scope.get("scopeAll"))) return;
        Collection<?> deptIds = scope.get("scopeDeptIds") instanceof Collection
                ? (Collection<?>) scope.get("scopeDeptIds") : Collections.emptyList();
        Object self = scope.get("scopeSelfUserId");
        List<String> alternatives = new ArrayList<>();
        if (spec.deptColumn != null && !deptIds.isEmpty()) {
            alternatives.add(column(spec.deptColumn) + " IN (" + placeholders(deptIds.size()) + ")");
            parameters.addAll(deptIds);
        }
        if (spec.creatorColumn != null && self != null) {
            alternatives.add(column(spec.creatorColumn) + "=?");
            parameters.add(self);
        }
        if (alternatives.isEmpty()) throw new AssistantFailure("NO_PERMISSION", "没有该业务的数据访问范围");
        where.append(" AND (").append(String.join(" OR ", alternatives)).append(')');
    }

    private static void applyWarehouseScope(Spec spec, Map<String, Object> scope,
                                            StringBuilder where, List<Object> parameters) {
        if (!spec.warehouseScoped || Boolean.TRUE.equals(scope.get("stockAll"))) return;
        Set<Object> ids = new LinkedHashSet<>();
        if (scope.get("stockDeptIds") instanceof Collection) ids.addAll((Collection<?>) scope.get("stockDeptIds"));
        if (scope.get("stockSelfIds") instanceof Collection) ids.addAll((Collection<?>) scope.get("stockSelfIds"));
        if (ids.isEmpty()) throw new AssistantFailure("NO_PERMISSION", "没有该拣货任务的仓库访问范围");
        where.append(" AND ").append(column(spec.warehouseColumn)).append(" IN (")
                .append(placeholders(ids.size())).append(')');
        parameters.addAll(ids);
    }

    private static void applyFixedConditions(Spec spec, StringBuilder where, List<Object> parameters) {
        for (Map.Entry<String, Object> condition : spec.fixedConditions.entrySet()) {
            where.append(" AND ").append(column(condition.getKey())).append("=?");
            parameters.add(condition.getValue());
        }
    }

    private static void applyStatus(Spec spec, String text, StringBuilder where) {
        if (spec.statusColumn == null) return;
        if (spec.statusProfile == StatusProfile.WRITEOFF) {
            if (text.matches(".*(?:反核销|已撤销核销|撤销核销).*")) where.append(" AND t.").append(spec.statusColumn).append("=2 AND t.reverse_time IS NOT NULL");
            else if (text.matches(".*(?:待生效|未核销).*")) where.append(" AND t.").append(spec.statusColumn).append("=0");
            else where.append(" AND t.").append(spec.statusColumn).append("=1 AND t.write_off_time IS NOT NULL");
            return;
        }
        if (spec.statusProfile == StatusProfile.MASTER) {
            if (text.matches(".*(?:停用|禁用|无效).*")) where.append(" AND t.").append(spec.statusColumn).append("=1");
            else if (text.matches(".*(?:启用|可用|有效).*")) where.append(" AND t.").append(spec.statusColumn).append("=0");
            return;
        }
        if (spec.statusProfile == StatusProfile.STOCK_IN_BILL) {
            if (text.contains("待提货")) where.append(" AND t.").append(spec.statusColumn).append("=10");
            else if (text.contains("部分提货")) where.append(" AND t.").append(spec.statusColumn).append("=20");
            else if (text.matches(".*(?:已完成|完成提货).*")) where.append(" AND t.").append(spec.statusColumn).append("=30");
            return;
        }
        if (spec.statusProfile == StatusProfile.STOCK_OUT_BILL || spec.statusProfile == StatusProfile.PICK) {
            if (text.contains("待拣货")) where.append(" AND t.").append(spec.statusColumn).append("=10");
            else if (text.contains("部分拣货")) where.append(" AND t.").append(spec.statusColumn).append("=20");
            else if (text.matches(".*(?:已拣货|完成拣货|已完成).*")) where.append(" AND t.").append(spec.statusColumn).append("=30");
            return;
        }
        if (spec.statusProfile == StatusProfile.DELIVERY) {
            if (text.matches(".*(?:待配送|待送货).*")) where.append(" AND t.").append(spec.statusColumn).append("=10");
            else if (text.matches(".*(?:部分配送|部分送货).*")) where.append(" AND t.").append(spec.statusColumn).append("=20");
            else if (text.matches(".*(?:已配送|已送货|完成配送|完成送货).*")) where.append(" AND t.").append(spec.statusColumn).append("=30");
            return;
        }
        if (spec.statusProfile == StatusProfile.OCR) {
            if (text.matches(".*(?:识别中|正在识别).*")) where.append(" AND t.").append(spec.statusColumn).append("='RECOGNIZING'");
            else if (text.matches(".*(?:识别失败|处理失败|失败).*")) where.append(" AND t.").append(spec.statusColumn).append("='FAILED'");
            else if (text.matches(".*(?:已确认|已生成).*")) where.append(" AND t.").append(spec.statusColumn).append("='CONFIRMED'");
            else if (text.matches(".*(?:已匹配|匹配完成).*")) where.append(" AND t.").append(spec.statusColumn).append("='MATCHED'");
            else if (text.matches(".*(?:已识别|识别完成).*")) where.append(" AND t.").append(spec.statusColumn).append("='RECOGNIZED'");
            return;
        }
        if (text.matches(".*(?:已审核|审核通过|已审批|审批通过).*")) where.append(" AND t.").append(spec.statusColumn).append("=20");
        else if (text.matches(".*(?:草稿|未提交).*")) where.append(" AND t.").append(spec.statusColumn).append(" IN (0,10)");
        else if (text.matches(".*(?:待审核|审核中|审批中|待审批).*")) where.append(" AND t.").append(spec.statusColumn).append("=10");
        else if (text.matches(".*(?:作废|已作废).*")) where.append(" AND t.").append(spec.statusColumn).append("=30");
    }

    private static void applyTime(Spec spec, String question, StringBuilder where, List<Object> parameters) {
        if (spec.timeColumn == null) return;
        LocalDateTime[] range = timeRange(question, LocalDateTime.now(BUSINESS_ZONE));
        if (range == null) return;
        where.append(" AND t.").append(spec.timeColumn).append(">=? AND t.").append(spec.timeColumn).append("<?");
        parameters.add(range[0]);
        parameters.add(range[1]);
    }

    private static void applyKeyword(Spec spec, String question, StringBuilder where, List<Object> parameters) {
        String documentNo = documentNo(question);
        if (documentNo != null && spec.labelColumn != null && ("no".equals(spec.labelColumn) || spec.labelColumn.endsWith("_no"))) {
            where.append(" AND UPPER(t.").append(spec.labelColumn).append(")=UPPER(?)");
            parameters.add(documentNo);
            return;
        }
        String quoted = quoted(question);
        if (quoted != null && spec.searchColumn != null) {
            where.append(" AND t.").append(spec.searchColumn).append(" LIKE CONCAT('%',?,'%')");
            parameters.add(quoted);
            return;
        }
        if ("product_brand".equals(spec.id) && question.contains("美孚")) {
            where.append(" AND t.name LIKE CONCAT('%',?,'%')");
            parameters.add("美孚");
        }
    }

    private static void applySubtype(Spec spec, String question, StringBuilder where, List<Object> parameters) {
        if ("base_data".equals(spec.id)) {
            List<String> types = baseDataTypes(question);
            if (!types.isEmpty()) {
                where.append(" AND t.type IN (").append(placeholders(types.size())).append(')');
                parameters.addAll(types);
            }
        }
        if ("product_category".equals(spec.id) && compact(question).matches(".*(?:最末级|末级|没有子分类|叶子分类).*")) {
            where.append(" AND NOT EXISTS (SELECT 1 FROM erp_product_category c WHERE c.parent_id=t.id")
                    .append(" AND c.tenant_id=t.tenant_id AND c.deleted=0)");
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyEntity(Spec spec, AssistantExecutionEnvelope envelope,
                                    StringBuilder where, List<Object> parameters) {
        if (envelope == null || envelope.getArguments() == null) return;
        List<Map<String, Object>> entities = new ArrayList<>();
        Object all = envelope.getArguments().get("resolvedEntities");
        if (all instanceof Map) for (Object value : ((Map<?, ?>) all).values())
            if (value instanceof Map) entities.add((Map<String, Object>) value);
        Object primary = envelope.getArguments().get("resolvedEntity");
        if (entities.isEmpty() && primary instanceof Map) entities.add((Map<String, Object>) primary);
        Set<String> used = new HashSet<>();
        for (Map<String, Object> entity : entities) {
            Object id = entity.get("id");
            String type = String.valueOf(entity.get("entityType"));
            String column = spec.entityColumns.get(type);
            if ("PURCHASER".equals(type) && column == null)
                throw new AssistantFailure("DATASET_UNPUBLISHED", "采购员条件目前仅支持采购订单和采购入库单，未执行删除采购员条件的查询");
            if ("SALESPERSON".equals(type) && column == null)
                throw new AssistantFailure("DATASET_UNPUBLISHED", "该销售单据尚未核验业务员关联字段，未执行删除业务员条件的查询");
            if (!(id instanceof Number) || column == null || !used.add(column)) continue;
            where.append(" AND ").append(column(column)).append("=?");
            parameters.add("PURCHASER".equals(type) && "purchase_in".equals(spec.id)
                    ? String.valueOf(((Number) id).longValue()) : ((Number) id).longValue());
        }
    }

    private static String column(String value) {
        return value != null && value.indexOf('.') >= 0 ? value : "t." + value;
    }

    private Map<String, Object> executeOrganization(String question, AssistantExecutionEnvelope envelope) {
        String text = compact(question);
        if (Boolean.TRUE.equals(envelope.getArguments()
                .get(AssistantKnowledgeContext.CONTEXTUAL_REALTIME_BOUNDARY_ARGUMENT))) {
            throw new AssistantFailure("CAPABILITY_UNVERIFIED",
                    "ERP 组织负责人及人员分工实时查询尚未开放；未使用公司知识库的历史组织资料代替实时结果");
        }
        if (AssistantKnowledgeContext.isRealtimeCompanyLeadershipQuestion(text)) {
            throw new AssistantFailure("CAPABILITY_UNVERIFIED",
                    "ERP 公司管理角色实时查询尚未开放；未使用公司知识库的历史组织资料代替当前结果");
        }
        if (AssistantKnowledgeContext.isRealtimeOrganizationKnowledgeBoundary(text)
                && !AssistantKnowledgeContext.isRealtimeOrganizationResponsibleQuestion(text)
                && !AssistantKnowledgeContext.isRealtimeOrganizationDutyQuestion(text)) {
            throw new AssistantFailure("CAPABILITY_UNVERIFIED",
                    "ERP 组织人员分工与协作关系实时查询尚未开放；未使用公司知识库的历史组织资料代替实时结果");
        }
        if (AssistantKnowledgeContext.isRealtimeOrganizationResponsibleQuestion(text)) {
            throw new AssistantFailure("CAPABILITY_UNVERIFIED",
                    "ERP 部门负责人实时查询尚未开放；未使用公司知识库的历史组织资料代替实时结果");
        }
        if (AssistantKnowledgeContext.isRealtimeOrganizationDutyQuestion(text)) {
            throw new AssistantFailure("CAPABILITY_UNVERIFIED",
                    "ERP 部门职责实时查询尚未开放；未使用公司知识库的历史组织资料代替实时结果");
        }
        boolean asksRelation = text.matches(".*(?:用户|员工|职工).*(?:部门|组织).*" )
                || text.matches(".*(?:所属部门|关联部门|用户与部门|用户和部门).*" );
        boolean asksUser = asksRelation || text.matches(".*(?:用户|员工|职工|人数|多少人).*" );
        boolean asksDept = text.matches(".*(?:部门|组织|层级).*" );
        if (!asksUser && !asksDept) throw new AssistantFailure("CLARIFY", "请说明要查询用户、部门还是用户与部门关系");
        if (asksUser) toolPermissions.authorize("system:user:query", "system_users",
                Arrays.asList("id", "nickname", "status", "deptId", "deptName"));
        if (asksDept) toolPermissions.authorize("system:dept:query", "system_dept",
                Arrays.asList("id", "name", "parentId", "status"));

        OrganizationScope scope = organizationScope();
        Long targetUserId = resolvedId(envelope, "USER");
        Long targetDeptId = resolvedId(envelope, "DEPARTMENT");
        if (targetDeptId == null) targetDeptId = resolveDepartmentId(question, scope);
        Set<Long> targetDeptIds = new LinkedHashSet<>();
        if (targetDeptId != null) {
            if (!scope.all && !scope.visibleDeptIds.contains(targetDeptId)) throw new AssistantFailure("NO_PERMISSION", "没有该部门的数据访问范围");
            targetDeptIds.add(targetDeptId);
            if (!text.matches(".*(?:仅|只看|只统计|本部门).*(?:不含|不包含|不要|排除)?下级.*")) {
                for (DeptRespDTO child : departments.getChildDeptList(targetDeptId))
                    if (scope.all || scope.visibleDeptIds.contains(child.getId())) targetDeptIds.add(child.getId());
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> summary = new ArrayList<>();
        if (asksDept && !asksRelation && targetUserId == null) rows.addAll(queryDepartments(scope, targetDeptIds, question));
        if (asksUser) {
            List<Object> parameters = new ArrayList<>();
            String userScope = userScopeSql(scope, "u", parameters);
            if (!targetDeptIds.isEmpty()) userScope += relationDeptSql(targetDeptIds, "u", parameters);
            if (targetUserId != null) { userScope += " AND u.id=?"; parameters.add(targetUserId); }
            if (text.matches(".*(?:启用|在用|正常).*")) userScope += " AND u.status=0";
            else if (text.matches(".*(?:停用|禁用).*")) userScope += " AND u.status=1";
            String capabilityId = asksRelation ? "user_department_relation" : "system_user";
            String businessObject = asksRelation ? "用户与部门关系" : "系统用户";
            List<Map<String, Object>> counts = reader.query("SELECT '" + capabilityId + "' capability_id,'" + businessObject + "' business_object,"
                    + "COUNT(DISTINCT u.id) record_count,NULL total_amount,NULL total_quantity FROM system_users u" + userScope,
                    parameters, 1);
            summary.addAll(counts);
            if (!text.matches(".*(?:共有多少|多少名|多少人|人数|数量|几个|总数).*")) {
                String detail;
                if (asksRelation) {
                    detail = "SELECT u.id,u.nickname label,CASE u.status WHEN 0 THEN '启用' WHEN 1 THEN '停用' ELSE CONCAT('状态',u.status) END status_name,"
                            + "d.name main_department,GROUP_CONCAT(DISTINCT ad.name ORDER BY ad.name SEPARATOR '、') related_departments "
                            + "FROM system_users u LEFT JOIN system_dept d ON d.id=u.dept_id AND d.tenant_id=u.tenant_id AND d.deleted=0 "
                            + "LEFT JOIN system_user_dept ud ON ud.user_id=u.id AND ud.tenant_id=u.tenant_id AND ud.deleted=0 "
                            + "LEFT JOIN system_dept ad ON ad.id=ud.dept_id AND ad.tenant_id=u.tenant_id AND ad.deleted=0 "
                            + userScope + " GROUP BY u.id,u.nickname,u.status,d.name ORDER BY u.id LIMIT 50";
                } else {
                    detail = "SELECT u.id,u.nickname label,CASE u.status WHEN 0 THEN '启用' WHEN 1 THEN '停用' ELSE CONCAT('状态',u.status) END status_name "
                            + "FROM system_users u" + userScope + " ORDER BY u.id LIMIT 50";
                }
                rows.addAll(reader.query(detail, parameters, 50));
            }
        }
        String title = asksRelation ? "用户与部门关系查询" : asksUser && asksDept ? "用户与部门查询"
                : asksUser ? "系统用户查询" : "系统部门查询";
        List<String> basis = new ArrayList<>();
        if (asksUser) basis.add("系统用户（system:user:query）");
        if (asksDept) basis.add("系统部门（system:dept:query）");
        Map<String, Object> result = baseResult(title);
        result.put("summary", summary);
        result.put("rows", rows);
        result.put("columns", columns(rows));
        result.put("basis", basis);
        result.put("status", empty(summary, rows) ? "EMPTY" : "SUCCESS");
        result.put("capabilityBatch", "5A");
        result.put("queryTrace", trace(ORGANIZATION_TOOL, Collections.<Spec>emptyList(), rows.size()));
        return result;
    }

    private List<Map<String, Object>> queryDepartments(OrganizationScope scope, Set<Long> targetIds, String question) {
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT d.id,d.name label,p.name parent_name,CASE d.status WHEN 0 THEN '启用' WHEN 1 THEN '停用' ELSE CONCAT('状态',d.status) END status_name ")
                .append("FROM system_dept d LEFT JOIN system_dept p ON p.id=d.parent_id AND p.tenant_id=d.tenant_id AND p.deleted=0 ")
                .append("WHERE d.tenant_id=? AND d.deleted=0");
        parameters.add(TenantContextHolder.getRequiredTenantId());
        Set<Long> ids = targetIds.isEmpty() ? scope.visibleDeptIds : targetIds;
        if (!scope.all || !targetIds.isEmpty()) {
            if (ids.isEmpty()) return Collections.emptyList();
            sql.append(" AND d.id IN (").append(placeholders(ids.size())).append(')'); parameters.addAll(ids);
        }
        if (compact(question).matches(".*(?:启用|在用|正常).*")) sql.append(" AND d.status=0");
        else if (compact(question).matches(".*(?:停用|禁用).*")) sql.append(" AND d.status=1");
        sql.append(" ORDER BY d.parent_id,d.sort,d.id LIMIT 100");
        return reader.query(sql.toString(), parameters, 100);
    }

    private OrganizationScope organizationScope() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) throw new AssistantFailure("NO_PERMISSION", "账号未登录");
        DeptDataPermissionRespDTO value = permissions.getDeptDataPermission(userId, "system_users");
        if (value == null) throw new AssistantFailure("NO_PERMISSION", "用户组织可见范围尚未配置");
        boolean all = Boolean.TRUE.equals(value.getAll());
        Set<Long> dataDeptIds = new LinkedHashSet<>();
        if (value.getDeptIds() != null) dataDeptIds.addAll(value.getDeptIds());
        Set<Long> visibleDeptIds = new LinkedHashSet<>(dataDeptIds);
        if (Boolean.TRUE.equals(value.getSelf())) {
            Set<Long> own = permissions.getDeptIdsByUserId(userId);
            if (own != null) visibleDeptIds.addAll(own);
        }
        if (!all && dataDeptIds.isEmpty() && !Boolean.TRUE.equals(value.getSelf()))
            throw new AssistantFailure("NO_PERMISSION", "没有用户或部门的数据访问范围");
        return new OrganizationScope(all, dataDeptIds, visibleDeptIds,
                Boolean.TRUE.equals(value.getSelf()) ? userId : null);
    }

    private Long resolveDepartmentId(String question, OrganizationScope scope) {
        Matcher matcher = DEPARTMENT_NAME.matcher(question == null ? "" : question.replaceAll("\\s+", ""));
        if (!matcher.find()) return null;
        String keyword = matcher.group(1).replaceFirst(
                "^(?:请|请帮我|帮我)?(?:列出|查询|查看|统计|显示)?(?:所有|全部|当前|系统)?", "");
        if (keyword.matches("(?:部门|组织|组织层级|所属部门|关联部门|用户与部门|用户和部门|员工与部门|员工和部门)")
                || keyword.matches(".*(?:用户|员工|职工).*(?:部门|组织)$")) return null;
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT d.id FROM system_dept d WHERE d.tenant_id=? AND d.deleted=0 AND d.name=?");
        parameters.add(TenantContextHolder.getRequiredTenantId()); parameters.add(keyword);
        if (!scope.all) {
            if (scope.visibleDeptIds.isEmpty()) return null;
            sql.append(" AND d.id IN (").append(placeholders(scope.visibleDeptIds.size())).append(')'); parameters.addAll(scope.visibleDeptIds);
        }
        sql.append(" ORDER BY d.id LIMIT 2");
        List<Map<String, Object>> rows = reader.query(sql.toString(), parameters, 2);
        if (rows.isEmpty()) throw new AssistantFailure("ENTITY_NOT_FOUND", "当前可见范围内没有找到该部门");
        if (rows.size() > 1) throw new AssistantFailure("CHOICE_REQUIRED", "存在多个同名部门，请选择具体部门");
        return ((Number) rows.get(0).get("id")).longValue();
    }

    private static String userScopeSql(OrganizationScope scope, String alias, List<Object> parameters) {
        StringBuilder sql = new StringBuilder(" WHERE ").append(alias).append(".tenant_id=? AND ").append(alias).append(".deleted=0");
        parameters.add(TenantContextHolder.getRequiredTenantId());
        if (scope.all) return sql.toString();
        List<String> alternatives = new ArrayList<>();
        if (!scope.dataDeptIds.isEmpty()) {
            alternatives.add(alias + ".dept_id IN (" + placeholders(scope.dataDeptIds.size()) + ")"); parameters.addAll(scope.dataDeptIds);
            alternatives.add("EXISTS (SELECT 1 FROM system_user_dept visible_ud WHERE visible_ud.user_id=" + alias + ".id AND visible_ud.tenant_id="
                    + alias + ".tenant_id AND visible_ud.deleted=0 AND visible_ud.dept_id IN (" + placeholders(scope.dataDeptIds.size()) + "))");
            parameters.addAll(scope.dataDeptIds);
        }
        if (scope.selfUserId != null) { alternatives.add(alias + ".id=?"); parameters.add(scope.selfUserId); }
        if (alternatives.isEmpty()) throw new AssistantFailure("NO_PERMISSION", "没有用户数据访问范围");
        return sql.append(" AND (").append(String.join(" OR ", alternatives)).append(')').toString();
    }

    private static String relationDeptSql(Set<Long> deptIds, String alias, List<Object> parameters) {
        String placeholders = placeholders(deptIds.size());
        String sql = " AND (" + alias + ".dept_id IN (" + placeholders + ") OR EXISTS (SELECT 1 FROM system_user_dept target_ud WHERE target_ud.user_id="
                + alias + ".id AND target_ud.tenant_id=" + alias + ".tenant_id AND target_ud.deleted=0 AND target_ud.dept_id IN (" + placeholders + ")))";
        parameters.addAll(deptIds); parameters.addAll(deptIds);
        return sql;
    }

    @SuppressWarnings("unchecked")
    private static Long resolvedId(AssistantExecutionEnvelope envelope, String type) {
        if (envelope == null || envelope.getArguments() == null) return null;
        Object all = envelope.getArguments().get("resolvedEntities");
        if (all instanceof Map) for (Object value : ((Map<?, ?>) all).values()) {
            if (!(value instanceof Map)) continue;
            Map<String, Object> row = (Map<String, Object>) value;
            if (type.equals(row.get("entityType")) && row.get("id") instanceof Number)
                return ((Number) row.get("id")).longValue();
        }
        Object primary = envelope.getArguments().get("resolvedEntity");
        if (primary instanceof Map) {
            Map<String, Object> row = (Map<String, Object>) primary;
            if (type.equals(row.get("entityType")) && row.get("id") instanceof Number)
                return ((Number) row.get("id")).longValue();
        }
        return null;
    }

    private static Map<String, Object> baseResult(String title) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("queryType", "CONTROLLED_BUSINESS_DATASET");
        result.put("title", title);
        result.put("scope", "当前登录用户有权限查看的数据");
        result.put("queriedAt", LocalDateTime.now(BUSINESS_ZONE).toString());
        result.put("traceId", UUID.randomUUID().toString());
        return result;
    }

    private static Map<String, Object> trace(String tool, Collection<Spec> specs, int rowCount) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("tool", tool);
        List<String> ids = new ArrayList<>(); for (Spec spec : specs) ids.add(spec.id);
        trace.put("capabilities", ids);
        trace.put("validation", "SERVER_OWNED_SQL+TENANT+PERMISSION+FIELD+DATA_SCOPE");
        trace.put("rowCount", rowCount);
        return trace;
    }

    private static List<Map<String, Object>> columns(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return Collections.emptyList();
        Map<String, String> titles = new LinkedHashMap<>();
        titles.put("businessObject", "业务对象"); titles.put("label", "名称/单号"); titles.put("status_name", "状态");
        titles.put("biz_time", "业务时间"); titles.put("quantity", "数量"); titles.put("amount", "金额");
        titles.put("main_department", "主部门"); titles.put("related_departments", "关联部门"); titles.put("parent_name", "上级部门");
        List<Map<String, Object>> result = new ArrayList<>();
        for (String key : rows.get(0).keySet()) {
            if ("id".equals(key)) continue;
            Map<String, Object> column = new LinkedHashMap<>(); column.put("key", key); column.put("title", titles.containsKey(key) ? titles.get(key) : key);
            result.add(column);
        }
        return result;
    }

    private static boolean empty(List<Map<String, Object>> summary, List<Map<String, Object>> rows) {
        if (!rows.isEmpty()) return false;
        for (Map<String, Object> value : summary) {
            Object count = value.get("record_count");
            if (count instanceof Number && ((Number) count).longValue() > 0) return false;
        }
        return true;
    }

    private static String batch(String tool) {
        if (MASTER_TOOL.equals(tool) || ORGANIZATION_TOOL.equals(tool)) return "5A";
        if (STOCK_TOOL.equals(tool)) return "5B";
        if (SALE_TOOL.equals(tool)) return "5C";
        if (PURCHASE_TOOL.equals(tool)) return "5D";
        return "5E";
    }

    private static List<Spec> selectSpecs(String tool, String question) {
        String text = compact(question);
        List<Spec> result = new ArrayList<>();
        for (Spec spec : SPECS.values()) {
            if (!tool.equals(spec.tool)) continue;
            for (String keyword : spec.keywords) {
                if (text.contains(keyword)) { result.add(spec); break; }
            }
        }
        // “收付款”“应收应付”“调账”“核销”这类并列问法应同时执行两侧，不允许只取先命中的一侧。
        if (FINANCE_TOOL.equals(tool)) {
            if (text.contains("收付款")) add(result, "finance_receipt", "finance_payment");
            if (text.contains("应收应付")) add(result, "receivable_misc", "payable_misc");
            if (text.contains("调账") && !text.contains("应收调账") && !text.contains("应付调账"))
                add(result, "receivable_adjust", "payable_adjust");
            if (text.contains("核销") && !text.matches(".*(?:收款核销|应收核销).*")
                    && !text.matches(".*(?:付款核销|应付核销).*"))
                add(result, "receipt_writeoff", "payment_writeoff");
        }
        if (SALE_TOOL.equals(tool)) {
            if (text.contains("出库") && text.contains("退货")) add(result, "sale_out", "sale_return");
            if (text.contains("拣货") && text.contains("配送")) add(result, "sale_pick", "sale_delivery");
        }
        if (PURCHASE_TOOL.equals(tool) && text.contains("入库") && text.contains("退货"))
            add(result, "purchase_in", "purchase_return");
        if (PURCHASE_TOOL.equals(tool) && text.matches(".*(?:配件价格调整|商品价格调整|产品价格调整|价格历史|历史价格).*"))
            add(result, "product_price_history");
        return deduplicate(result);
    }

    private static void add(List<Spec> values, String... ids) { for (String id : ids) if (SPECS.containsKey(id)) values.add(SPECS.get(id)); }
    private static List<Spec> deduplicate(List<Spec> values) { return new ArrayList<>(new LinkedHashSet<>(values)); }

    private static Map<String, Spec> createSpecs() {
        Map<String, Spec> result = new LinkedHashMap<>();
        register(result, spec("product_category", MASTER_TOOL, "商品分类", "erp:product-category:query", "erp_product_category", "erp_product_category", "name", "status", null, null, "dept_id", "creator", true, true, false, "name", Arrays.asList("id","name","code","parent_id"), "商品分类", "分类"));
        register(result, spec("product_brand", MASTER_TOOL, "商品品牌", "erp:product-brand:query", "erp_product_brand", "erp_product_brand", "name", "status", null, null, null, null, false, true, false, "name", Arrays.asList("id","name"), "商品品牌", "品牌"));
        register(result, spec("product_unit", MASTER_TOOL, "商品单位", "erp:product-unit:query", "erp_product_unit", "erp_product_unit", "name", "status", null, null, "dept_id", "creator", true, true, false, "name", Arrays.asList("id","name"), "商品单位", "产品单位", "单位"));
        register(result, spec("base_data", MASTER_TOOL, "ERP 基础数据", "erp:base-data:query", "erp_base_data", "erp_base_data", "name", "status", null, null, "dept_id", "creator", true, true, false, "name", Arrays.asList("id","type","name","code"),
                "往来类别", "供应商分类", "客户分类", "供应商类型", "客户类型", "区域", "销售区域", "价格级别",
                "结算方式", "配送方式", "送货方式", "运费类型", "物流公司", "发票类型", "开票类型", "线路", "运费说明",
                "采购类型", "采购方式", "下单公司", "订货公司", "收货地址", "仓储中心", "仓储仓库",
                "费用支付类型", "支出类型", "费用单据类型", "费用项目", "基础数据"));

        register(result, spec("price_system", STOCK_TOOL, "价格体系", "erp:price-system:query", "erp_price_system", "erp_price_system", "name", "status", null, null, "dept_id", "creator", true, true, false, "name", Arrays.asList("id","code","name"), "价格体系"));
        Spec productPriceHistory = extra(spec("product_price_history", STOCK_TOOL, "配件价格历史", "erp:product:query",
                "erp_product", "erp_price_history", "biz_no", null, "price_time", null,
                "p.dept_id", "p.creator", true, false, false, "biz_no",
                Arrays.asList("id", "purchasePrice", "salePrice"), "配件价格调整", "商品价格调整", "产品价格调整", "价格历史", "历史价格"),
                "price", "count", "biz_type", "partner_type").join(
                " JOIN erp_product p ON p.id=t.product_id AND p.tenant_id=t.tenant_id AND p.deleted=0");
        productPriceHistory.entityColumns.put("PRODUCT", "p.id");
        productPriceHistory.entityColumns.put("DEPARTMENT", "p.dept_id");
        register(result, productPriceHistory);
        register(result, spec("warehouse", STOCK_TOOL, "仓库", "erp:warehouse:query", "erp_warehouse", "erp_warehouse", "name", "status", null, null, "dept_id", "creator", true, true, false, "name", Arrays.asList("id","name","warehouse_code","warehouse_type"), "启用仓库", "仓库主数据", "仓库详情"));
        register(result, doc("stock_check", STOCK_TOOL, "库存盘点", "erp:stock-check:query", "erp_stock_check", "erp_stock_check", "no", "check_time", "total_price", "total_count", "dept_id", "creator", "盘点单", "库存盘点"));
        register(result, doc("stock_in", STOCK_TOOL, "其他入库", "erp:stock-in:query", "erp_stock_in", "erp_stock_in", "no", "in_time", "total_price", "total_count", "dept_id", "creator", "其他入库"));
        register(result, doc("stock_in_bill", STOCK_TOOL, "库存入库单", "erp:stock-in-bill:query", "erp_stock_in_bill", "erp_stock_in_bill", "no", "bill_date", null, "total_count", null, "creator", "入库单", "提货记录")
                .statusProfile(StatusProfile.STOCK_IN_BILL));
        register(result, doc("stock_out", STOCK_TOOL, "其他出库", "erp:stock-out:query", "erp_stock_out", "erp_stock_out", "no", "out_time", "total_price", "total_count", "dept_id", "creator", "其他出库"));
        register(result, doc("stock_out_bill", STOCK_TOOL, "库存出库单", "erp:stock-out-bill:query", "erp_stock_out_bill", "erp_stock_out_bill", "no", "bill_date", null, "total_count", null, "creator", "出库单", "拣货记录")
                .statusProfile(StatusProfile.STOCK_OUT_BILL));
        register(result, doc("stock_transfer", STOCK_TOOL, "库存调拨", "erp:stock-move:query", "erp_stock_move", "erp_stock_move", "no", "move_time", "total_price", "total_count", "dept_id", "creator", "调拨单", "库存调拨"));
        register(result, doc("stock_transfer_out", STOCK_TOOL, "调拨出库", "erp:stock-transfer-out:query", "erp_stock_transfer_out", "erp_stock_move", "no", "move_time", "total_price", "total_count", "dept_id", "creator", "调拨出库", "调出单", "调出")
                .fixed("transfer_direction", 10));
        register(result, doc("stock_transfer_in", STOCK_TOOL, "调拨入库", "erp:stock-transfer-in:query", "erp_stock_transfer_in", "erp_stock_move", "no", "move_time", "total_price", "total_count", "dept_id", "creator", "调拨入库", "调入单", "调入")
                .fixed("transfer_direction", 20));
        register(result, doc("warehouse_move", STOCK_TOOL, "仓库移仓", "erp:warehouse-move:query", "erp_warehouse_move", "erp_warehouse_move", "no", "move_time", "total_price", "total_count", "dept_id", "creator", "移仓", "移库"));

        register(result, party(doc("sale_quote", SALE_TOOL, "销售报价", "erp:sale-quote:query", "erp_sale_quote", "erp_sale_quote", "no", "quote_time", "total_price", "total_count", "dept_id", "creator", "销售报价", "报价单"), "CUSTOMER", "customer_id", "SALESPERSON", "sale_user_id"));
        register(result, party(doc("sale_cart", SALE_TOOL, "销售开单", "erp:sale-cart:query", "erp_sale_cart", "erp_sale_cart", "no", "cart_time", "total_price", "total_count", "dept_id", "creator", "销售开单", "销售手推车", "开单"), "CUSTOMER", "customer_id", "SALESPERSON", "sale_user_id"));
        register(result, party(doc("sale_out", SALE_TOOL, "销售出库", "erp:sale-out:query", "erp_sale_out", "erp_sale_out", "no", "out_time", "total_price", "total_count", "dept_id", "creator", "销售出库", "出库退货关系"), "CUSTOMER", "customer_id", "SALESPERSON", "sale_user_id"));
        register(result, party(extra(doc("sale_return", SALE_TOOL, "销售退货", "erp:sale-return:query", "erp_sale_return", "erp_sale_return", "no", "return_time", "total_price", "total_count", "dept_id", "creator", "销售退货", "可退数量", "退货"), "source_out_no", "order_no", "return_mode"), "CUSTOMER", "customer_id", "SALESPERSON", "sale_user_id"));
        register(result, party(extra(spec("sale_pick", SALE_TOOL, "销售拣货", "erp:sale-pick:query", "erp_sale_out",
                "erp_sale_pick_delivery_pick_task", "sale_out_no", "status", "create_time", null,
                "p.dept_id", "p.creator", true, false, false, "sale_out_no",
                Arrays.asList("id", "sale_out_no", "status", "create_time", "total_item_count", "picked_item_count", "warehouse_id"),
                "销售拣货", "待拣货", "部分拣货", "已拣货", "拣货任务"), "picked_item_count", "warehouse_id")
                .quantity("total_item_count").join(" JOIN erp_sale_pick_delivery_order p ON p.id=t.order_id AND p.tenant_id=t.tenant_id AND p.deleted=0"
                        + " JOIN erp_sale_out so ON so.id=p.sale_out_id AND so.tenant_id=t.tenant_id AND so.deleted=0")
                .warehouseScope("warehouse_id").statusProfile(StatusProfile.PICK), "CUSTOMER", "p.customer_id", "SALESPERSON", "so.sale_user_id"));
        register(result, party(extra(spec("sale_delivery", SALE_TOOL, "销售配送", "erp:sale-delivery:query", "erp_sale_out",
                "erp_sale_pick_delivery_order", "sale_out_no", "delivery_status", "create_time", null,
                "dept_id", "creator", true, false, false, "sale_out_no",
                Arrays.asList("id", "sale_out_no", "delivery_status", "create_time", "total_item_count", "delivered_item_count"),
                "销售配送", "待配送", "待送货", "部分配送", "部分送货", "已配送", "已送货", "配送任务", "销售任务"),
                "pick_status", "delivered_item_count").quantity("total_item_count").statusProfile(StatusProfile.DELIVERY),
                "CUSTOMER", "customer_id", "SALESPERSON", "so.sale_user_id")
                .join(" JOIN erp_sale_out so ON so.id=t.sale_out_id AND so.tenant_id=t.tenant_id AND so.deleted=0"));
        register(result, party(extra(doc("sale_price_adjust", SALE_TOOL, "销售调价", "erp:sale-price-adjust:query", "erp_sale_price_adjust", "erp_sale_price_adjust", "no", "adjust_date", "total_adjust_price", null, "dept_id", "creator", "销售调价", "调价前", "调价后"), "original_sale_out_no", "new_sale_out_no", "adjust_type"), "CUSTOMER", "customer_id"));

        register(result, party(doc("purchase_in", PURCHASE_TOOL, "采购入库", "erp:purchase-in:query", "erp_purchase_in", "erp_purchase_in", "no", "in_time", "total_price", "total_count", "dept_id", "creator", "采购入库", "入库退货关系")
                .join(" LEFT JOIN system_users pu ON CAST(pu.id AS CHAR)=t.purchaser AND pu.tenant_id=t.tenant_id AND pu.deleted=0"),
                "SUPPLIER", "supplier_id", "PURCHASER", "purchaser"));
        register(result, party(extra(doc("purchase_return", PURCHASE_TOOL, "采购退货", "erp:purchase-return:query", "erp_purchase_return", "erp_purchase_return", "no", "return_time", "total_price", "total_count", "dept_id", "creator", "采购退货", "可退数量"), "order_no", "return_mode"), "SUPPLIER", "supplier_id"));
        register(result, party(extra(doc("purchase_invoice", PURCHASE_TOOL, "采购发票", "erp:purchase-invoice:query", "erp_purchase_invoice", "erp_purchase_invoice", "no", "invoice_date", "total_amount", "invoice_count", "dept_id", "creator", "采购发票", "发票号码"), "invoice_no", "invoice_type"), "SUPPLIER", "supplier_id"));
        register(result, extra(doc("purchase_invoice_ocr", PURCHASE_TOOL, "采购发票识别", "erp:purchase-invoice-ocr:query", "erp_purchase_invoice_ocr_batch", "erp_purchase_invoice_ocr_batch", "batch_no", "create_time", null, "file_count", null, "creator", "发票识别", "ocr", "识别批次"), "generated_invoice_count", "error_msg")
                .statusProfile(StatusProfile.OCR));
        register(result, party(extra(doc("purchase_price_adjust", PURCHASE_TOOL, "采购调价", "erp:purchase-price-adjust:query", "erp_purchase_price_adjust", "erp_purchase_price_adjust", "no", "adjust_time", "total_adjust_price", null, "dept_id", "creator", "采购调价", "采购价格调整"), "adjust_type", "approve_time"), "SUPPLIER", "supplier_id"));

        register(result, spec("account", FINANCE_TOOL, "资金账户", "erp:account:query", "erp_account", "erp_account", "name", "status", null, null, "dept_id", "creator", true, true, false, "name", Arrays.asList("id","name","account_type","document_status"), "资金账户", "现金银行", "账户余额"));
        register(result, extra(doc("finance_transfer", FINANCE_TOOL, "账户转账", "erp:finance-transfer:query", "erp_finance_transfer", "erp_finance_transfer", "no", "transfer_time", "transfer_price", null, "dept_id", "creator", "账户转账", "内部资金调拨", "转账单"), "out_account_id", "in_account_id", "fee_price"));
        register(result, party(doc("finance_receipt", FINANCE_TOOL, "收款单", "erp:finance-receipt:query", "erp_finance_receipt", "erp_finance_receipt", "no", "receipt_time", "receipt_price", null, "dept_id", "creator", "收款单", "收款明细"), "CUSTOMER", "customer_id"));
        register(result, party(doc("finance_payment", FINANCE_TOOL, "付款单", "erp:finance-payment:query", "erp_finance_payment", "erp_finance_payment", "no", "payment_time", "payment_price", null, "dept_id", "creator", "付款单", "付款明细"), "SUPPLIER", "supplier_id"));
        register(result, party(doc("receivable_misc", FINANCE_TOOL, "应收杂项", "erp:receivable-misc:query", "erp_receivable_misc", "erp_receivable_misc", "no", "biz_time", "amount", null, "dept_id", "creator", "应收杂项", "其他应收"), "CUSTOMER", "customer_id"));
        register(result, doc("receivable_income", FINANCE_TOOL, "其他收入", "erp:receivable-other-income:query", "erp_receivable_other_income", "erp_receivable_other_income", "no", "biz_time", "total_amount", null, "dept_id", "creator", "其他收入"));
        register(result, party(extra(doc("receivable_adjust", FINANCE_TOOL, "应收调账", "erp:receivable-other:query", "erp_receivable_other", "erp_receivable_other", "no", "biz_time", "receivable_amount", null, "dept_id", "creator", "应收调账", "应收调增", "应收调减"), "source_type", "source_id", "source_no", "settled_amount"), "CUSTOMER", "customer_id"));
        register(result, party(doc("payable_misc", FINANCE_TOOL, "应付杂项", "erp:payable-misc:query", "erp_payable_misc", "erp_payable_misc", "no", "biz_time", "amount", null, "dept_id", "creator", "应付杂项", "其他应付"), "SUPPLIER", "supplier_id"));
        register(result, doc("payable_expense", FINANCE_TOOL, "应付费用", "erp:payable-expense:query", "erp_payable_expense", "erp_payable_expense", "no", "biz_time", "total_amount", null, "dept_id", "creator", "应付费用", "费用单"));
        register(result, party(extra(doc("payable_adjust", FINANCE_TOOL, "应付调账", "erp:payable-other:query", "erp_payable_other", "erp_payable_other", "no", "biz_time", "payable_amount", null, "dept_id", "creator", "应付调账", "应付调增", "应付调减"), "source_type", "source_id", "source_no", "settled_amount"), "SUPPLIER", "supplier_id"));
        register(result, writeoff("receipt_writeoff", "收款核销", "erp:finance-receipt:query", "erp_finance_receipt",
                "erp_finance_receipt_item", "erp_finance_receipt", "receipt_id", "receipt_price", "CUSTOMER", "customer_id", "收款核销", "应收核销"));
        register(result, writeoff("payment_writeoff", "付款核销", "erp:finance-payment:query", "erp_finance_payment",
                "erp_finance_payment_item", "erp_finance_payment", "payment_id", "payment_price", "SUPPLIER", "supplier_id", "付款核销", "应付核销"));
        return Collections.unmodifiableMap(result);
    }

    private static Spec writeoff(String id, String title, String permission, String module, String table,
                                 String parentTable, String parentId, String amount, String partyType,
                                 String partyColumn, String... keywords) {
        Spec result = spec(id, FINANCE_TOOL, title, permission, module, table, parentId, "write_off_status",
                "write_off_time", amount, "p.dept_id", "p.creator", true, false, true, "biz_no",
                Arrays.asList("id", "biz_type", "biz_id", "biz_no", amount, "write_off_status", "write_off_time", "reverse_time"), keywords);
        result.join(" JOIN " + parentTable + " p ON p.id=t." + parentId
                + " AND p.tenant_id=t.tenant_id AND p.deleted=0");
        result.entityColumns.put(partyType, "p." + partyColumn);
        result.entityColumns.put("DEPARTMENT", "p.dept_id");
        return result.statusProfile(StatusProfile.WRITEOFF);
    }

    private static Spec doc(String id, String tool, String title, String permission, String module, String table,
                            String label, String time, String amount, String quantity, String dept, String creator, String... keywords) {
        return spec(id, tool, title, permission, module, table, label, "status", time, amount, dept, creator,
                true, false, false, label, Arrays.asList("id",label,"status",time,amount,quantity), keywords).quantity(quantity);
    }

    private static Spec spec(String id, String tool, String title, String permission, String module, String table,
                             String label, String status, String time, String amount, String dept, String creator,
                             boolean scoped, boolean masterStatus, boolean writeOff, String search,
                             Collection<String> protectedFields, String... keywords) {
        return new Spec(id, tool, title, permission, module, table, label, status, time, amount, null, dept, creator,
                scoped, masterStatus, writeOff, search, protectedFields, Arrays.asList(keywords));
    }

    private static Spec party(Spec spec, String... values) {
        for (int i = 0; i + 1 < values.length; i += 2) spec.entityColumns.put(values[i], values[i + 1]);
        spec.entityColumns.put("DEPARTMENT", spec.deptColumn);
        return spec;
    }

    private static Spec extra(Spec spec, String... columns) { spec.extraColumns.addAll(Arrays.asList(columns)); return spec; }
    private static void register(Map<String, Spec> target, Spec spec) { if (target.put(spec.id, spec) != null) throw new IllegalStateException("重复阶段5能力: " + spec.id); }

    private static List<String> baseDataTypes(String question) {
        String text = compact(question); Set<String> result = new LinkedHashSet<>();
        if (text.contains("往来类别") && !text.contains("供应商往来类别") && !text.contains("客户往来类别")) {
            result.add("supplier_category"); result.add("customer_category");
        }
        type(result, text, "supplier_category", "供应商往来类别", "供应商分类");
        type(result, text, "customer_category", "客户往来类别", "客户分类");
        type(result, text, "supplier_type", "供应商类型"); type(result, text, "customer_type", "客户类型");
        type(result, text.replace("销售区域", ""), "region", "区域"); type(result, text, "sales_area", "销售区域");
        type(result, text, "price_level", "价格级别"); type(result, text, "settle_method", "结算方式");
        type(result, text, "delivery_method", "配送方式", "送货方式"); type(result, text, "freight_type", "运费类型");
        type(result, text, "logistics_company", "物流公司"); type(result, text, "invoice_type", "发票类型", "开票类型");
        type(result, text, "route", "线路"); type(result, text, "freight_explain", "运费说明");
        type(result, text, "purchase_type", "采购类型", "采购方式"); type(result, text, "order_company", "下单公司", "订货公司");
        type(result, text, "receive_address", "收货地址"); type(result, text, "storage_center", "仓储中心");
        type(result, text, "storage_warehouse", "仓储仓库");
        type(result, text, "payable_expense_biz_type", "费用支付类型");
        type(result, text, "payable_expense_type", "支出类型");
        type(result, text, "payable_expense_doc_type", "费用单据类型");
        type(result, text, "bank_transfer_fee_expense_category", "费用项目");
        return new ArrayList<>(result);
    }
    private static void type(Set<String> values, String text, String code, String... keywords) {
        for (String keyword : keywords) if (text.contains(keyword)) { values.add(code); return; }
    }

    private static LocalDateTime[] timeRange(String question, LocalDateTime now) {
        String text = compact(question);
        Matcher ym = YEAR_MONTH.matcher(text);
        if (ym.find()) {
            YearMonth value = YearMonth.of(Integer.parseInt(ym.group(1)), Integer.parseInt(ym.group(2)));
            return new LocalDateTime[]{value.atDay(1).atStartOfDay(), value.plusMonths(1).atDay(1).atStartOfDay()};
        }
        AssistantRelativePeriod.Window window = AssistantRelativePeriod.window(question, now);
        return window == null ? null : new LocalDateTime[]{window.getStart(), window.getEnd()};
    }

    private static String quoted(String question) { Matcher matcher = QUOTED.matcher(question == null ? "" : question); return matcher.find() ? matcher.group(1).trim() : null; }
    private static String documentNo(String question) { Matcher matcher = DOCUMENT_NO.matcher(question == null ? "" : question); return matcher.find() ? matcher.group(1) : null; }
    private static String compact(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s\\u3000，,。；;：:？?！!]", ""); }
    private static String placeholders(int count) { String[] values = new String[count]; Arrays.fill(values, "?"); return String.join(",", values); }

    private static final class QueryParts {
        final String where; final List<Object> parameters;
        QueryParts(String where, List<Object> parameters) { this.where = where; this.parameters = parameters; }
    }

    private static final class OrganizationScope {
        final boolean all;
        final Set<Long> dataDeptIds;
        final Set<Long> visibleDeptIds;
        final Long selfUserId;
        OrganizationScope(boolean all, Set<Long> dataDeptIds, Set<Long> visibleDeptIds, Long selfUserId) {
            this.all = all;
            this.dataDeptIds = dataDeptIds;
            this.visibleDeptIds = visibleDeptIds;
            this.selfUserId = selfUserId;
        }
    }

    private static final class Spec {
        final String id, tool, title, permission, module, table, labelColumn, statusColumn, timeColumn, amountColumn;
        String quantityColumn;
        final String deptColumn, creatorColumn, searchColumn;
        final boolean scoped, masterStatus, writeOff;
        final List<String> protectedFields, keywords, extraColumns = new ArrayList<>();
        final Map<String, String> entityColumns = new LinkedHashMap<>();
        final Map<String, Object> fixedConditions = new LinkedHashMap<>();
        String joinClause = "";
        StatusProfile statusProfile;
        boolean warehouseScoped;
        String warehouseColumn;
        Spec(String id, String tool, String title, String permission, String module, String table, String labelColumn,
             String statusColumn, String timeColumn, String amountColumn, String quantityColumn, String deptColumn,
             String creatorColumn, boolean scoped, boolean masterStatus, boolean writeOff, String searchColumn,
             Collection<String> protectedFields, Collection<String> keywords) {
            this.id=id;this.tool=tool;this.title=title;this.permission=permission;this.module=module;this.table=table;
            this.labelColumn=labelColumn;this.statusColumn=statusColumn;this.timeColumn=timeColumn;this.amountColumn=amountColumn;
            this.quantityColumn=quantityColumn;this.deptColumn=deptColumn;this.creatorColumn=creatorColumn;this.scoped=scoped;
            this.masterStatus=masterStatus;this.writeOff=writeOff;this.searchColumn=searchColumn;
            this.statusProfile=masterStatus?StatusProfile.MASTER:writeOff?StatusProfile.WRITEOFF:StatusProfile.DOCUMENT;
            List<String> fields=new ArrayList<>();for(String field:protectedFields) if(field!=null) fields.add(field);
            this.protectedFields=Collections.unmodifiableList(fields);this.keywords=Collections.unmodifiableList(new ArrayList<>(keywords));
            if(deptColumn!=null) entityColumns.put("DEPARTMENT",deptColumn);
        }
        Spec quantity(String value) { quantityColumn=value; return this; }
        Spec fixed(String column, Object value) { fixedConditions.put(column, value); return this; }
        Spec join(String value) { joinClause=value; return this; }
        Spec statusProfile(StatusProfile value) { statusProfile=value; return this; }
        Spec warehouseScope(String value) { warehouseScoped=true;warehouseColumn=value;return this; }
    }

    private enum StatusProfile { DOCUMENT, MASTER, WRITEOFF, STOCK_IN_BILL, STOCK_OUT_BILL, PICK, DELIVERY, OCR }
}
