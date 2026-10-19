package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
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
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import lombok.Data;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 客户、供应商、配件和员工基础档案数量统计。
 *
 * <p>只复用各档案页面已经核验的分页与可见范围，不接受模型生成 SQL。</p>
 */
@Service
public class AssistantMasterDataQueryService {

    public static final String TOOL_NAME = "query_master_data_stats";

    public enum ArchiveType { CUSTOMER, SUPPLIER, PRODUCT, EMPLOYEE }
    public enum StatusMode { ENABLED, DISABLED, ALL, SPLIT }

    @Data
    public static class Query {
        private ArchiveType archiveType;
        private StatusMode statusMode;
        /** 产品档案普通关键词；仅 PRODUCT 使用，严格复用产品档案八字段检索口径。 */
        private String productKeyword;
        /** 用户明确表达的产品品牌；不并入普通关键词模糊字段。 */
        private String productBrand;

        public Query(ArchiveType archiveType, StatusMode statusMode) {
            this(archiveType, statusMode, null);
        }

        public Query(ArchiveType archiveType, StatusMode statusMode, String productKeyword) {
            this(archiveType,statusMode,productKeyword,null);
        }

        public Query(ArchiveType archiveType, StatusMode statusMode, String productKeyword,String productBrand) {
            this.archiveType = archiveType;
            this.statusMode = statusMode;
            this.productKeyword = productKeyword;
            this.productBrand = productBrand;
        }
    }

    @Resource private PermissionApi permissions;
    @Resource private ErpCustomerService customerService;
    @Resource private ErpSupplierService supplierService;
    @Resource private ErpProductService productService;
    @Resource private AdminUserApi userApi;

    /** Returns {@code null} when the question is not an unambiguous archive-count question. */
    public Query parse(String question) {
        String text = normalize(question);
        if (text.isEmpty() || containsBusinessMetric(text) || !asksForCount(text)) {
            return null;
        }
        boolean customer = text.contains("客户");
        boolean supplier = text.contains("供应商");
        boolean product = text.contains("配件") || text.contains("产品") || text.contains("商品");
        boolean employee = text.contains("员工") || text.contains("职工");
        if ((customer ? 1 : 0) + (supplier ? 1 : 0) + (product ? 1 : 0) + (employee ? 1 : 0) != 1) {
            return null;
        }
        ArchiveType archiveType = customer ? ArchiveType.CUSTOMER
                : supplier ? ArchiveType.SUPPLIER : product ? ArchiveType.PRODUCT : ArchiveType.EMPLOYEE;
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
        String productBrand=archiveType==ArchiveType.PRODUCT
                ?AssistantSemanticExtractor.explicitProductBrand(question):null;
        return new Query(archiveType, statusMode,
                archiveType == ArchiveType.PRODUCT && productBrand==null ? extractProductKeyword(text) : null,
                productBrand);
    }

    public Query parseArguments(Map<String, Object> arguments) {
        if (arguments == null) {
            return null;
        }
        try {
            ArchiveType type = ArchiveType.valueOf(String.valueOf(arguments.get("archiveType")));
            String productKeyword = type == ArchiveType.PRODUCT
                    ? boundedKeyword(arguments.get("keyword")) : null;
            String productBrand = type == ArchiveType.PRODUCT
                    ? boundedKeyword(arguments.get("brand")) : null;
            return new Query(type, StatusMode.valueOf(String.valueOf(arguments.get("statusMode"))),
                    productKeyword,productBrand);
        } catch (Exception ignored) {
            return null;
        }
    }

    public boolean hasAnyAvailableType() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return userId != null && TenantContextHolder.getTenantId() != null && !TenantContextHolder.isIgnore()
                && permissions.hasAnyPermissions(userId, "erp:assistant:query")
                && permissions.hasAnyPermissions(userId,
                "erp:customer:query", "erp:supplier:query", "erp:product:query", "system:user:query");
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
            if(query.getProductBrand()!=null && !productBrandVisible())
                throw new AssistantFailure("FIELD_FORBIDDEN","当前账号没有产品品牌字段权限");
            return;
        }
        if (query.getArchiveType() == ArchiveType.EMPLOYEE) {
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
                enabled = count(query, CommonStatusEnum.ENABLE.getStatus());
                total = enabled;
                addSummary(summary, "启用" + archiveName(type), enabled);
                break;
            case DISABLED:
                disabled = count(query, CommonStatusEnum.DISABLE.getStatus());
                total = disabled;
                addSummary(summary, "停用" + archiveName(type), disabled);
                break;
            case ALL:
                total = count(query, null);
                addSummary(summary, "全部" + archiveName(type), total);
                break;
            case SPLIT:
                enabled = count(query, CommonStatusEnum.ENABLE.getStatus());
                disabled = count(query, CommonStatusEnum.DISABLE.getStatus());
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
        String answer = answer(query, enabled, disabled, total);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("rows", Collections.emptyList());
        result.put("status", total == 0L ? "EMPTY" : "SUCCESS");
        result.put("emptyText", total == 0L ? answer : null);
        result.put("answerText", answer);
        result.put("summaryLabel", summaryLabel(query));
        result.put("queryType", AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL.name());
        result.put("toolName", TOOL_NAME);
        result.put("title", archiveName(type) + "档案数量统计");
        result.put("scope", queryScope(query));
        result.put("queriedAt", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
        result.put("columns", Collections.emptyList());
        result.put("basis", Collections.singletonList(type == ArchiveType.EMPLOYEE
                ? "ERP 系统用户档案；当前租户、启停用状态口径"
                : archiveName(type) + "档案页面同权限、同状态"
                + (query.getProductBrand()!=null?"、同品牌"
                :query.getProductKeyword()!=null?"、同关键词":"") + "口径"));
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
        if (query.getProductKeyword() != null) trace.put("productKeyword", query.getProductKeyword());
        if (query.getProductBrand() != null) trace.put("productBrand", query.getProductBrand());
        trace.put("resultCount", total);
        result.put("queryTrace", trace);
        return result;
    }

    private long count(Query query, Integer status) {
        ArchiveType type = query.getArchiveType();
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
                ErpProductPageReqVO request = productRequest(status, query.getProductKeyword(),query.getProductBrand());
                return total(productService.getProductVOPage(request, false));
            }
            case EMPLOYEE: {
                PageParam request = new PageParam();
                request.setPageNo(1);
                request.setPageSize(1);
                return total(userApi.getUserSimplePage(status, null, request));
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

    private static ErpProductPageReqVO productRequest(Integer status, String keyword,String brand) {
        ErpProductPageReqVO request = new ErpProductPageReqVO();
        request.setPageNo(1);
        request.setPageSize(1);
        request.setStatus(status);
        request.setKeyword(keyword);
        request.setKeywordSearchMode(ErpProductPageReqVO.KEYWORD_SEARCH_MODE_PRODUCT_ARCHIVE);
        request.setBrand(brand);
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

    private static String answer(Query query, long enabled, long disabled, long total) {
        ArchiveType type = query.getArchiveType();
        StatusMode mode = query.getStatusMode();
        String name = archiveName(type);
        String scope = type == ArchiveType.EMPLOYEE ? "按 ERP 员工账号口径，当前租户内" : "当前账号可见范围内";
        String filter = query.getProductBrand()!=null?"品牌“"+query.getProductBrand()+"”的"
                :query.getProductKeyword()!=null?"关键词“"+query.getProductKeyword()+"”匹配的":"";
        if (total == 0L) {
            String status = mode == StatusMode.ENABLED ? "启用" : mode == StatusMode.DISABLED ? "停用" : "";
            return scope + "没有" + filter + status + name + "档案。";
        }
        if (mode == StatusMode.SPLIT) {
            return scope + filter + "启用" + name + " " + enabled + " 个，停用" + name + " "
                    + disabled + " 个，共 " + total + " 个。";
        }
        String status = mode == StatusMode.ENABLED ? "启用" : mode == StatusMode.DISABLED ? "停用" : "";
        return scope + filter + "共有 " + total + " 个" + status + name + "档案。";
    }

    private static String summaryLabel(Query query) {
        ArchiveType type = query.getArchiveType();
        StatusMode mode = query.getStatusMode();
        String status = mode == StatusMode.ENABLED ? "启用" : mode == StatusMode.DISABLED ? "停用" : "";
        return status + archiveName(type) + "数量" + productFilterText(query);
    }

    private static String queryScope(Query query) {
        if (query.getArchiveType() == ArchiveType.EMPLOYEE) return "当前租户内员工账号";
        String scope = "当前登录用户有权限查看的" + archiveName(query.getArchiveType()) + "档案";
        if(query.getProductBrand()!=null) return scope+"；产品品牌："+query.getProductBrand();
        return query.getProductKeyword() == null ? scope : scope + "；产品关键词：" + query.getProductKeyword();
    }

    private boolean productBrandVisible() {
        List<String> hidden=permissions.getCurrentUserHiddenFields("erp_product",null,false);
        if(hidden==null) throw new AssistantFailure("FORBIDDEN","产品字段权限尚未确定");
        for(String field:hidden) if("brand".equals(normalizeField(field))) return false;
        return true;
    }

    private static String normalizeField(String value) {
        return value==null?"":value.toLowerCase(Locale.ROOT)
                .replaceFirst("^(col_|report_|item_)","").replace("_","");
    }

    private static String productFilterText(Query query) {
        if(query.getProductBrand()!=null) return "（品牌："+query.getProductBrand()+"）";
        if(query.getProductKeyword()!=null) return "（关键词："+query.getProductKeyword()+"）";
        return "";
    }

    private static String permission(ArchiveType type) {
        switch (type) {
            case CUSTOMER: return "erp:customer:query";
            case SUPPLIER: return "erp:supplier:query";
            case PRODUCT: return "erp:product:query";
            case EMPLOYEE: return "system:user:query";
            default: throw new IllegalArgumentException("Unsupported archive type");
        }
    }

    private static String module(ArchiveType type) {
        switch (type) {
            case CUSTOMER: return "erp_customer";
            case SUPPLIER: return "erp_supplier";
            case PRODUCT: return "erp_product";
            case EMPLOYEE: return "system_users";
            default: throw new IllegalArgumentException("Unsupported archive type");
        }
    }

    private static String archiveName(ArchiveType type) {
        switch (type) {
            case CUSTOMER: return "客户";
            case SUPPLIER: return "供应商";
            case PRODUCT: return "配件";
            case EMPLOYEE: return "员工";
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

    /**
     * 从产品数量问题中剥离 ERP 场景词、数量词、状态词和档案类别词，保留品牌、系列、
     * 名称、规格或编码等真实限定。结果作为产品列表现有 keyword 条件，不拼接 SQL。
     */
    private static String extractProductKeyword(String normalizedQuestion) {
        String value = normalizedQuestion == null ? "" : normalizedQuestion;
        value = value.replaceFirst("^(?:(?:erp系统|系统erp|erp|系统)(?:中|里|内|里面)?)", "");
        value = value.replaceAll("(?:请问|帮我查一下|帮我查下|帮我查询|帮我查|查一下|查询)", "");
        value = value.replaceAll("(?:当前账号可见范围内|当前账号可见的|当前账号|可见范围内|当前|现在|目前)", "");
        value = value.replaceAll("(?:启用和停用|启用与停用|启停用|已启用|已停用|启用|正常|有效|停用|禁用)", "");
        value = value.replaceAll("(?:总共有|一共有|共有|总共|有多少(?:个|条|种)?|有几个|多少(?:个|条|种)?|几个|数量|总数|统计|分别)", "");
        value = value.replaceAll("(?:品牌为|品牌是|品牌|系列|牌|配件档案|产品档案|商品档案|配件|产品|商品|档案)", "");
        value = value.replaceAll("(?:包含|包括|所有|全部|其中|的|和|与|及|下)", "");
        value = value.replaceAll("[^\\p{IsHan}a-z0-9+./-]", "");
        return value.length() >= 2 && value.length() <= 80 ? value : null;
    }

    private static String boundedKeyword(Object value) {
        if (value == null) return null;
        String keyword = String.valueOf(value).trim();
        return keyword.length() >= 2 && keyword.length() <= 80 ? keyword : null;
    }
}
