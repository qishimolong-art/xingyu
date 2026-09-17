package cn.iocoder.yudao.module.erp.service.finance.accounting.rule;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;

/** 固定模板的配置及记账输入；不接收用户自定义公式或分录。 */
public final class ErpVoucherRuleModels {
    private ErpVoucherRuleModels() {}
    @Data public static class Auxiliary {
        private String type;
        private Long id;
        private String name;
    }
    @Data public static class Mapping {
        private String scenario;
        private String role;
        private Long categoryId;
        private Long subjectId;
    }
    @Data public static class AccountMapping {
        private Long accountId;
        private Long subjectId;
    }
    @Data public static class Project {
        private Long id;
        private String name;
        private Boolean enabled = true;
        private List<Long> deptIds = new ArrayList<>();
    }
    @Data public static class Config {
        private Long version = 0L;
        private String taxpayer = "UNSET";
        private Set<String> enabledScenarios = new LinkedHashSet<>();
        private List<Mapping> mappings = new ArrayList<>();
        private List<AccountMapping> accounts = new ArrayList<>();
        private List<Project> projects = new ArrayList<>();
    }
    @Data public static class EnableRequest {
        private String scenario;
        private Boolean enabled;
        private Long version;
    }
    @Data public static class Context {
        private Integer bizType;
        private Long bizId;
        private Long version = 0L;
        private String sourceVersion;
        private String scenario;
        private Boolean revenueConfirmed;
        private String discountNature;
        private String priceBasis;
        private String invoiceStatus;
        private String deductionStatus;
        private BigDecimal taxAmount;
        private Boolean feeConfirmed;
        private String feeNature;
        private Integer feePartyType;
        private Long feePartyId;
        private Boolean feeAlreadyPosted;
        private Integer postedBizType;
        private Long postedBizId;
        private String note;
        private List<Auxiliary> auxiliaries = new ArrayList<>();
        /** 金额为零、无法按比例分摊时，财务明确提供逐明细权重。 */
        private Map<Long, BigDecimal> allocationWeights = new LinkedHashMap<>();
    }
    @Data public static class Request {
        private Integer bizType;
        private Long bizId;
        private LocalDate voucherDate;
        private Integer attributionYear;
        private Integer attributionMonth;
        private String previewToken;
    }
    @Data public static class Batch {
        private List<Request> items = new ArrayList<>();
    }
    @Data public static class Preview {
        private Integer bizType;
        private Long bizId;
        private String bizNo;
        private String status;
        private List<String> issues = new ArrayList<>();
        private String sourceVersion;
        private String previewToken;
        private Long configVersion;
        private Long contextVersion;
        private String scenario;
        private LocalDate voucherDate;
        private Long voucherId;
        private BigDecimal sourceAmount;
        private BigDecimal stockAmount;
        private List<ErpVoucherItemDO> items = new ArrayList<>();
        private Map<String, Object> evidence = new LinkedHashMap<>();
    }
    @Data public static class Template {
        private String code;
        private String name;
        private int bizType;
        private boolean taxable;
        private List<String> roles;
        private String treatment;
        private String basis = "https://kjs.mof.gov.cn/zhengcefabu/201111/P020111118325852734144.pdf";
        public Template(String code, String name, int bizType, boolean taxable, String treatment, String... roles) {
            this.code=code; this.name=name; this.bizType=bizType; this.taxable=taxable;
            this.treatment=treatment; this.roles=Arrays.asList(roles);
        }
    }
}
