package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 付款单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpFinancePaymentRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "23752")
    private Long id;

    @Schema(description = "付款单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "FKD888")
    private String no;

    @Schema(description = "付款状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "付款时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime paymentTime;

    @Schema(description = "经手人编号", example = "19690")
    private Long financeUserId;
    @Schema(description = "经手人名称", example = "张三")
    private String financeUserName;

    @Schema(description = "所属部门", example = "100")
    private Long deptId;
    @Schema(description = "所属部门名称", example = "财务部")
    private String deptName;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "29399")
    private Long supplierId;
    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "小番茄公司")
    private String supplierName;

    @Schema(description = "付款账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "28989")
    private Long accountId;
    @Schema(description = "付款账户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String accountName;

    @Schema(description = "来源其他应付单编号", example = "1024")
    private Long sourcePayableMiscId;

    @Schema(description = "来源其他应付单号", example = "QTYFM20260922000001")
    private String sourcePayableMiscNo;

    @Schema(description = "结算方式", example = "月结")
    private String settleMethod;

    @Schema(description = "开户行", example = "中国银行")
    private String bankName;

    @Schema(description = "合计价格，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "13832")
    private BigDecimal totalPrice;

    @Schema(description = "优惠金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "11600")
    private BigDecimal discountPrice;

    @Schema(description = "实际价格，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000")
    private BigDecimal paymentPrice;

    @Schema(description = "已核销金额，单位：元")
    private BigDecimal allocatedPrice;

    @Schema(description = "未核销金额，单位：元")
    private BigDecimal unallocatedPrice;

    @Schema(description = "核销状态：0 未核销、1 部分核销、2 已核销、3 数据异常")
    private Integer writeOffStatus;

    @Schema(description = "有效核销明细数")
    private Integer writeOffCount;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "关联凭证号", example = "记-202607-000001")
    private String voucherNo;

    @Schema(description = "创建人", example = "芋道")
    private String creator;
    @Schema(description = "创建人名称", example = "芋道")
    private String creatorName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改人", example = "芋道")
    private String updater;
    @Schema(description = "修改人名称", example = "芋道")
    private String updaterName;
    @Schema(description = "修改时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

    @Schema(description = "审核人", example = "芋道")
    private String auditorName;
    @Schema(description = "审核日期")
    private LocalDateTime auditTime;

    @Schema(description = "打印次数", example = "0")
    private Integer printCount;
    @Schema(description = "打印时间")
    private LocalDateTime printTime;

    @Schema(description = "付款项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "付款项编号", example = "11756")
        private Long id;

        @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Integer bizType;

        @Schema(description = "业务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11756")
        private Long bizId;

        @Schema(description = "业务单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11756")
        private String bizNo;

        @Schema(description = "应付金额，单位：分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000")
        private BigDecimal totalPrice;

        @Schema(description = "已付金额，单位：分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000")
        private BigDecimal paidPrice;

        @Schema(description = "本次付款，单位：分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000")
        @NotNull(message = "本次付款不能为空")
        private BigDecimal paymentPrice;

        @Schema(description = "核销生命周期状态：0 待生效、1 已生效、2 已撤销")
        private Integer writeOffStatus;

        private LocalDateTime writeOffTime;
        private Long writeOffUserId;
        private LocalDateTime reverseTime;
        private Long reverseUserId;
        private String reverseReason;

        @Schema(description = "备注", example = "随便")
        private String remark;

    }

}
