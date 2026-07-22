package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 客户新增/修改 Request VO")
@Data
public class ErpCustomerSaveReqVO {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "27520")
    private Long id;

    @Schema(description = "客户编码")
    private String code;

    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "客户名称不能为空")
    private String name;

    @Schema(description = "简称")
    private String shortName;
    @Schema(description = "往来类别")
    @Min(value = 1, message = "往来类别必须为正数")
    private Integer relationType;
    @Schema(description = "区域编号")
    private Long areaId;
    @Schema(description = "客户类型")
    private Integer customerType;
    @Schema(description = "是否集团客户")
    private Boolean groupCustomer;
    @Schema(description = "企业匹配状态")
    private Integer enterpriseMatchStatus;

    @Schema(description = "联系人", example = "老王")
    private String contact;

    @Schema(description = "手机号码", example = "15601691300")
    private String mobile;

    @Schema(description = "联系电话", example = "15601691300")
    private String telephone;

    @Schema(description = "电子邮箱", example = "7685323@qq.com")
    private String email;

    @Schema(description = "财务联系电话")
    private String financeTelephone;
    @Schema(description = "地址区域编号")
    private Long addressAreaId;
    @Schema(description = "地址")
    private String address;
    @Schema(description = "详细地址")
    private String detailAddress;
    @Schema(description = "邮政编码")
    private String postCode;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "开启状态不能为空")
    private Integer status;

    @Schema(description = "排序", example = "10")
    private Integer sort;

    @Schema(description = "纳税人识别号", example = "91130803MA098BY05W")
    private String taxNo;

    @Schema(description = "账户")
    private String accountName;
    @Schema(description = "结算方式")
    @Min(value = 1, message = "结算方式必须为正数")
    private Integer settleMethod;
    @Schema(description = "结算锁定")
    private Boolean settleLocked;
    @Schema(description = "开票类型")
    private Integer invoiceType;
    @Schema(description = "起订金额")
    private BigDecimal minOrderAmount;
    @Schema(description = "运费类型")
    private Integer freightType;
    @Schema(description = "佣金比例")
    private BigDecimal commissionRate;
    @Schema(description = "启用佣金机制")
    private Boolean commissionEnabled;

    @Schema(description = "税率", example = "10")
    private BigDecimal taxPercent;

    @Schema(description = "开户行", example = "芋艿")
    private String bankName;

    @Schema(description = "开户账号", example = "622908212277228617")
    private String bankAccount;

    @Schema(description = "开户地址", example = "兴业银行浦东支行")
    private String bankAddress;

    @Schema(description = "统一信用代码")
    private String unifiedCreditCode;
    @Schema(description = "开票银行")
    private String invoiceBankName;
    @Schema(description = "开票银行账号")
    private String invoiceBankAccount;
    @Schema(description = "开票地址")
    private String invoiceAddress;
    @Schema(description = "开票电话")
    private String invoiceTelephone;
    @Schema(description = "开票单位")
    private String invoiceCompany;
    @Schema(description = "所属业务员")
    private Long saleUserId;
    @Schema(description = "所属开发员")
    private Long developerUserId;
    @Schema(description = "所属部门")
    private Long deptId;
    @Schema(description = "适用部门")
    private List<Long> deptIds;
    @Schema(description = "允许多部门")
    private Boolean allowMultiDept;
    @Schema(description = "价格级别")
    private Integer priceLevel;
    @Schema(description = "价格锁定")
    private Boolean priceLocked;
    @Schema(description = "是否物流")
    private Boolean logistics;
    @Schema(description = "运输方式")
    private Integer transportMethod;
    @Schema(description = "线路编号")
    private Long routeId;
    @Schema(description = "运费说明编号")
    private Long freightExplainId;
    @Schema(description = "五笔码")
    private String wubiCode;
    @Schema(description = "拼音码")
    private String pinyinCode;
    @Schema(description = "会员编码")
    private String memberCode;
    @Schema(description = "平台唯一码")
    private String platformCode;

    @Schema(description = "白条授信额度")
    private BigDecimal creditLimit;
    @Schema(description = "是否启用白条授信")
    private Boolean creditEnabled;
    @Schema(description = "白条授信期限（天）")
    private Integer creditTermDays;
    @Schema(description = "数据中心审核状态 0未审核 1已审核 2驳回")
    private Integer dataCenterAuditStatus;

}
