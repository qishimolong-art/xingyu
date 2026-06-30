package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - ERP 客户 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpCustomerRespVO {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "27520")
    @ExcelProperty("客户编号")
    private Long id;

    @Schema(description = "客户编码")
    @ExcelProperty("客户编码")
    private String code;

    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @ExcelProperty("客户名称")
    private String name;

    @Schema(description = "简称")
    @ExcelProperty("简称")
    private String shortName;

    @Schema(description = "往来类别")
    private Integer relationType;

    @Schema(description = "区域编号")
    private Long areaId;

    @Schema(description = "客户类型")
    @ExcelProperty("客户类型")
    private Integer customerType;

    @Schema(description = "是否集团客户")
    private Boolean groupCustomer;

    @Schema(description = "企业匹配状态")
    @ExcelProperty("企业匹配")
    private Integer enterpriseMatchStatus;

    @Schema(description = "联系人", example = "老王")
    @ExcelProperty("联系人")
    private String contact;

    @Schema(description = "手机号码", example = "15601691300")
    @ExcelProperty("手机号码")
    private String mobile;

    @Schema(description = "联系电话", example = "15601691300")
    @ExcelProperty("联系电话")
    private String telephone;

    @Schema(description = "电子邮箱", example = "7685323@qq.com")
    @ExcelProperty("电子邮箱")
    private String email;

    private String financeTelephone;
    private Long addressAreaId;
    @ExcelProperty("地址")
    private String address;
    private String detailAddress;
    private String postCode;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "开启状态", converter = DictConvert.class)
    @DictFormat("common_status") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private Integer status;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "纳税人识别号", example = "91130803MA098BY05W")
    @ExcelProperty("纳税人识别号")
    private String taxNo;

    private String accountName;
    @ExcelProperty("默认结算方式")
    private Integer settleMethod;
    private Boolean settleLocked;
    private Integer invoiceType;
    private BigDecimal minOrderAmount;
    private Integer freightType;
    private BigDecimal commissionRate;
    private Boolean commissionEnabled;

    @Schema(description = "税率", example = "10")
    @ExcelProperty("税率")
    private BigDecimal taxPercent;

    @Schema(description = "开户行", example = "芋艿")
    @ExcelProperty("开户行")
    private String bankName;

    @Schema(description = "开户账号", example = "622908212277228617")
    @ExcelProperty("开户账号")
    private String bankAccount;

    @Schema(description = "开户地址", example = "兴业银行浦东支行")
    @ExcelProperty("开户地址")
    private String bankAddress;

    private String unifiedCreditCode;
    private String invoiceBankName;
    private String invoiceBankAccount;
    private String invoiceAddress;
    private String invoiceTelephone;
    private String invoiceCompany;
    @ExcelProperty("所属业务员")
    private Long saleUserId;
    private Long developerUserId;
    private Long deptId;
    @ExcelProperty("价格级别")
    private Integer priceLevel;
    private Boolean priceLocked;
    private Boolean logistics;
    private Integer transportMethod;
    @ExcelProperty("线路")
    private Long routeId;
    private Long freightExplainId;
    private String wubiCode;
    private String pinyinCode;
    private String memberCode;
    private String platformCode;

    @Schema(description = "白条授信额度")
    private BigDecimal creditLimit;
    @Schema(description = "数据中心审核状态 0未审核 1已审核 2驳回")
    private Integer dataCenterAuditStatus;

    @Schema(description = "最近销售日期")
    private LocalDate lastSaleDate;
    @Schema(description = "累计销售额")
    private BigDecimal totalSaleAmount;
    @Schema(description = "应收余额")
    private BigDecimal receivableBalance;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
