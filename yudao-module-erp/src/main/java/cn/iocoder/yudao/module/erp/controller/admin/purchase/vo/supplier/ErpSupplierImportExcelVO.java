package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 供应商导入 Excel VO
 */
@Data
public class ErpSupplierImportExcelVO {

    @ExcelProperty("供应商名称")
    private String name;

    @ExcelProperty("供应商编码")
    private String code;

    @ExcelProperty("旧编码")
    private String oldCode;

    @ExcelProperty("简称")
    private String shortName;

    @ExcelProperty("外文名")
    private String foreignName;

    @ExcelProperty("联系人")
    private String contact;

    @ExcelProperty("手机号码")
    private String mobile;

    @ExcelProperty("联系电话")
    private String telephone;

    @ExcelProperty("电子邮箱")
    private String email;

    @ExcelProperty("传真")
    private String fax;

    @ExcelProperty("区域")
    private String region;

    @ExcelProperty("往来类别")
    private String category;

    @ExcelProperty("供应商类型")
    private String supplierType;

    @ExcelProperty("采购员")
    private String purchaser;

    @ExcelProperty("公司性质")
    private String companyNature;

    @ExcelProperty("采购管控")
    private String purchaseControl;

    @ExcelProperty("到货周期(天)")
    private Integer arrivalCycle;

    @ExcelProperty("采购提前期(天)")
    private Integer purchaseLeadDays;

    @ExcelProperty("淘汰")
    private Boolean obsolete;

    @ExcelProperty("淘汰日期")
    private LocalDateTime obsoleteDate;

    @ExcelProperty("集团供应商")
    private Boolean groupSupplier;

    @ExcelProperty("允许分店开单")
    private Boolean allowBranchOrder;

    @ExcelProperty("结算方式")
    private String settleMethod;

    @ExcelProperty("结算锁定")
    private Boolean settleLocked;

    @ExcelProperty("运输方式")
    private String transportMethod;

    @ExcelProperty("运费类型")
    private String freightType;

    @ExcelProperty("物流公司")
    private String logisticsCompany;

    @ExcelProperty("到货点")
    private String arrivalPoint;

    @ExcelProperty("浮动是否更新供应商最后进价")
    private String floatUpdateLastPrice;

    @ExcelProperty("绩效考核利润参考依据")
    private String performanceProfitRef;

    @ExcelProperty("地址")
    private String address;

    @ExcelProperty("省")
    private String province;

    @ExcelProperty("市")
    private String city;

    @ExcelProperty("区县")
    private String district;

    @ExcelProperty("邮政编码")
    private String postalCode;

    @ExcelProperty("网址")
    private String website;

    @ExcelProperty("开票类型")
    private String invoiceType;

    @ExcelProperty("纳税人识别号")
    private String taxpayerId;

    @ExcelProperty("开票银行")
    private String invoiceBank;

    @ExcelProperty("开票银行账号")
    private String invoiceBankAccount;

    @ExcelProperty("开票地址")
    private String invoiceAddress;

    @ExcelProperty("开票电话")
    private String invoicePhone;

    @ExcelProperty("开票单位")
    private String invoiceCompany;

    @ExcelProperty("账户")
    private String account;

    @ExcelProperty("开户行")
    private String bankName;

    @ExcelProperty("开户账号")
    private String bankAccount;

    @ExcelProperty("开户地址")
    private String bankAddress;

    @ExcelProperty("税号")
    private String taxNo;

    @ExcelProperty("税率")
    private BigDecimal taxPercent;

    @ExcelProperty("财务联系电话")
    private String financePhone;

    @ExcelProperty("会员编码")
    private String memberCode;

    @ExcelProperty("法定代表")
    private String legalPerson;

    @ExcelProperty("统一信用代码")
    private String creditCode;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("排序")
    private Integer sort;

    @ExcelProperty("状态")
    private Integer status;

}
