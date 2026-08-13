package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - ERP 客户部门授信 Response VO")
@Data
public class ErpCustomerDeptCreditRespVO {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "所属部门")
    private Long deptId;

    @Schema(description = "适用部门")
    private List<Long> deptIds;

    @Schema(description = "部门名称映射")
    private Map<Long, String> deptNameMap;

    @Schema(description = "部门授信配置")
    private List<Item> items;

    @Schema(description = "部门授信配置明细")
    @Data
    public static class Item {

        @Schema(description = "配置编号", example = "1")
        private Long id;

        @Schema(description = "部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "102")
        private Long deptId;

        @Schema(description = "部门名称")
        private String deptName;

        @Schema(description = "是否启用授信", example = "true")
        private Boolean creditEnabled;

        @Schema(description = "授信金额", example = "100000.00")
        private BigDecimal creditLimit;

        @Schema(description = "授信期限（天）", example = "30")
        private Integer creditTermDays;

        @Schema(description = "备注")
        private String remark;

    }

}
