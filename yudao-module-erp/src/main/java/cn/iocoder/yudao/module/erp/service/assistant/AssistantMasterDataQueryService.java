package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 客户、供应商和配件基础档案数量统计。
 *
 * <p>只复用各档案页面已经核验的分页与可见范围，不接受模型生成 SQL。</p>
 */
@Service
public class AssistantMasterDataQueryService {

    public static final String TOOL_NAME = "query_master_data_stats";

    public enum ArchiveType { CUSTOMER, SUPPLIER, PRODUCT }
    public enum StatusMode { ENABLED, DISABLED, ALL, SPLIT }

    @Data
    @AllArgsConstructor
    public static class Query {
        private ArchiveType archiveType;
        private StatusMode statusMode;
    }

    @Resource private PermissionApi permissions;
    @Resource private ErpCustomerService customerService;
    @Resource private ErpSupplierService supplierService;
    @Resource private ErpProductService productService;

    /** Returns {@code null} when the question is not an unambiguous archive-count question. */
    public Query parse(String question) {
        String text = normalize(question);
        if (text.isEmpty() || containsBusinessMetric(text) || !asksForCount(text)) {
            return null;
        }
        boolean customer = text.contains("客户");
        boolean supplier = text.contains("供应商");
        boolean product = text.contains("配件") || text.contains("产品");
        if ((customer ? 1 : 0) + (supplier ? 1 : 0) + (product ? 1 : 0) != 1) {
            return null;
        }
        ArchiveType archiveType = customer ? ArchiveType.CUSTOMER
                : supplier ? ArchiveType.SUPPLIER : ArchiveType.PRODUCT;
        StatusMode statusMode;
        boolean enabled = text.contains("启用") || text.contains("正常") || text.contains("有效");
        boolean disabled = text.contains("停用") || text.contains("禁用");
        if (enabled && disabled) {
            statusMode = StatusMode.SPLIT;
        } else if (text.matches(".*(?:包含|包括)(?:已)?停用.*")
                || text.matches(".*(?:全部|所有)(?:启停用|状态).*")) {
            statusMode = StatusMode.ALL;
        } else if (disabled) {
            statusMode = StatusMode.DISABLED;
        } else {
            // 产品约定：未指定状态时只统计启用档案。
            statusMode = StatusMode.ENABLED;
        }
        return new Query(archiveType, statusMode);
    }

    public Query parseArguments(Map<String, Object> arguments) {
        if (arguments == null) {
            return null;
        }
        try {
            return new Query(ArchiveType.valueOf(String.valueOf(arguments.get("archiveType"))),
                    StatusMode.valueOf(String.valueOf(arguments.get("statusMode"))));
        } catch (Exception ignored) {
            return null;
        }
    }

    public boolean hasAnyAvailableType() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return userId != null && TenantContextHolder.getTenantId() != null && !TenantContextHolder.isIgnore()
                && permissions.hasAnyPermissions(userId, "erp:assistant:query")
                && permissions.hasAnyPermissions(userId,
                "erp:customer:query", "erp:supplier:query", "erp:product:query");
    }

    public void validateAccess(Query query) {
        if (query == null) {
            throw new AssistantFailure("MODEL_INVALID", "基础档案统计条件无效");
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null || TenantContextHolder.getTenantId() == null || TenantContextHolder.isIgnore()
                || !permissions.hasAnyPermissions(userId, "erp:assistant:query")
                || !permissions.hasAnyPermissions(userId, permission(query.getArchiveType()))) {
            throw new AssistantFailure("FORBIDDEN", "当前账号没有" + archiveName(query.getArchiveType()) + "档案查询权限");
        }
        if (query.getArchiveType() == ArchiveType.PRODUCT) {
            if (!productService.hasCurrentUserProductArchiveVisibleScope()) {
                throw new AssistantFailure("FORBIDDEN", "当前账号没有可见的配件档案范围");
            }
            return;
        }
        DeptDataPermissionRespDTO scope = permissions.getDeptDataPermission(userId, module(query.getArchiveType()));
        if (scope == null || !Boolean.TRUE.equals(scope.getAll())
                && !Boolean.TRUE.equals(scope.getSelf())
                && (scope.getDeptIds() == null || scope.getDeptIds().isEmpty())) {
            throw new AssistantFailure("FORBIDDEN", "当前账号没有可见的" + archiveName(query.getArchiveType()) + "档案范围");
        }
    }

    public Map<String, Object> execute(Query query) {
        validateAccess(query);
        ArchiveType type = query.getArchiveType();
        StatusMode mode = query.getStatusMode();
        long enabled = -1L;
        long disabled = -1L;
        long total;
        List<Map<String, Object>> summary = new ArrayList<>();
        switch (mode) {
            case ENABLED:
                enabled = count(type, CommonStatusEnum.ENABLE.getStatus());
                total = enabled;
                addSummary(summary, "启用" + archiveName(type), enabled);
                break;
            case DISABLED:
                disabled = count(type, CommonStatusEnum.DISABLE.getStatus());
                total = disabled;
                addSummary(summary, "停用" + archiveName(type), disabled);
                break;
            case ALL:
                total = count(type, null);
                addSummary(summary, "全部" + archiveName(type), total);
                break;
            case SPLIT:
                enabled = count(type, CommonStatusEnum.ENABLE.getStatus());
                disabled = count(type, CommonStatusEnum.DISABLE.getStatus());
                total = enabled + disabled;
                addSummary(summary, "启用" + archiveName(type), enabled);
                addSummary(summary, "停用" + archiveName(type), disabled);
                break;
            default:
                throw new AssistantFailure("MODEL_INVALID", "基础档案状态口径无效");
        }
        if (total == 0L) {
            summary.clear();
        }
        String answer = answer(type, mode, enabled, disabled, total);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("rows", Collections.emptyList());
        result.put("status", total == 0L ? "EMPTY" : "SUCCESS");
        result.put("emptyText", total == 0L ? answer : null);
        result.put("answerText", answer);
        result.put("summaryLabel", summaryLabel(type, mode));
        result.put("queryType", AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL.name());
        result.put("toolName", TOOL_NAME);
        result.put("title", archiveName(type) + "档案数量统计");
        result.put("scope", "当前登录用户有权限查看的" + archiveName(type) + "档案");
        result.put("queriedAt", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
        result.put("columns", Collections.emptyList());
        result.put("basis", Collections.singletonList(archiveName(type) + "档案页面同权限、同状态口径"));
        result.put("traceId", UUID.randomUUID().toString());
        Map<String, Object> presentation = new LinkedHashMap<>();
        presentation.put("mode", "SUMMARY");
        presentation.put("showSummary", total > 0L);
        presentation.put("showChart", false);
        presentation.put("showTable", false);
        result.put("presentation", presentation);
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("stage", "COMPLETED");
        trace.put("archiveType", type.name());
        trace.put("statusMode", mode.name());
        trace.put("resultCount", total);
        result.put("queryTrace", trace);
        return result;
    }

    private long count(ArchiveType type, Integer status) {
        switch (type) {
            case CUSTOMER: {
                ErpCustomerPageReqVO request = customerRequest();
                PageResult<?> page = status == null ? customerService.getCustomerPage(request)
                        : customerService.getCustomerPageByStatus(request, status);
                return total(page);
            }
            case SUPPLIER: {
                ErpSupplierPageReqVO request = supplierRequest();
                PageResult<?> page = status == null ? supplierService.getSupplierPage(request)
                        : supplierService.getSupplierPageByStatus(request, status);
                return total(page);
            }
            case PRODUCT: {
                ErpProductPageReqVO request = productRequest(status);
                return total(productService.getProductVOPage(request, false));
            }
            default:
                throw new AssistantFailure("MODEL_INVALID", "基础档案类型无效");
        }
    }

    private static ErpCustomerPageReqVO customerRequest() {
        ErpCustomerPageReqVO request = new ErpCustomerPageReqVO();
        request.setPageNo(1);
        request.setPageSize(1);
        return request;
    }

    private static ErpSupplierPageReqVO supplierRequest() {
        ErpSupplierPageReqVO request = new ErpSupplierPageReqVO();
        request.setPageNo(1);
        request.setPageSize(1);
        return request;
    }

    private static ErpProductPageReqVO productRequest(Integer status) {
        ErpProductPageReqVO request = new ErpProductPageReqVO();
        request.setPageNo(1);
        request.setPageSize(1);
        request.setStatus(status);
        request.setIncludeSaleDistributedArchive(true);
        return request;
    }

    private static long total(PageResult<?> page) {
        return page == null || page.getTotal() == null ? 0L : page.getTotal();
    }

    private static void addSummary(List<Map<String, Object>> summary, String label, long amount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("amount", amount);
        row.put("unit", "个");
        summary.add(row);
    }

    private static String answer(ArchiveType type, StatusMode mode, long enabled, long disabled, long total) {
        String name = archiveName(type);
        if (total == 0L) {
            String status = mode == StatusMode.ENABLED ? "启用" : mode == StatusMode.DISABLED ? "停用" : "";
            return "当前账号可见范围内没有" + status + name + "档案。";
        }
        if (mode == StatusMode.SPLIT) {
            return "当前账号可见范围内启用" + name + " " + enabled + " 个，停用" + name + " "
                    + disabled + " 个，共 " + total + " 个。";
        }
        String status = mode == StatusMode.ENABLED ? "启用" : mode == StatusMode.DISABLED ? "停用" : "";
        return "当前账号可见范围内共有 " + total + " 个" + status + name + "档案。";
    }

    private static String summaryLabel(ArchiveType type, StatusMode mode) {
        String status = mode == StatusMode.ENABLED ? "启用" : mode == StatusMode.DISABLED ? "停用" : "";
        return status + archiveName(type) + "数量";
    }

    private static String permission(ArchiveType type) {
        switch (type) {
            case CUSTOMER: return "erp:customer:query";
            case SUPPLIER: return "erp:supplier:query";
            case PRODUCT: return "erp:product:query";
            default: throw new IllegalArgumentException("Unsupported archive type");
        }
    }

    private static String module(ArchiveType type) {
        switch (type) {
            case CUSTOMER: return "erp_customer";
            case SUPPLIER: return "erp_supplier";
            case PRODUCT: return "erp_product";
            default: throw new IllegalArgumentException("Unsupported archive type");
        }
    }

    private static String archiveName(ArchiveType type) {
        switch (type) {
            case CUSTOMER: return "客户";
            case SUPPLIER: return "供应商";
            case PRODUCT: return "配件";
            default: return "基础";
        }
    }

    private static boolean asksForCount(String text) {
        return text.matches(".*(?:总共有|一共有|共有|总共|总数|数量|多少(?:个|条)?|几个|统计).*" );
    }

    private static boolean containsBusinessMetric(String text) {
        return text.matches(".*(?:库存|有货|缺货|销售|销量|采购|收款|回款|付款|应收|应付|欠款|余额|"
                + "订单|单据|价格|金额|入库|出库|流水|排名|最多|最少).*" );
    }

    private static String normalize(String question) {
        return question == null ? "" : question.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\u3000？?。！!，,；;：:]", "");
    }
}
