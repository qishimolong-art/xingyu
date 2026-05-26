package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.system.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 供应商 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpSupplierRespVO {

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17791")
    @ExcelProperty("供应商编号")
    private Long id;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋道源码")
    @ExcelProperty("供应商名称")
    private String name;

    @Schema(description = "联系人", example = "芋艿")
    @ExcelProperty("联系人")
    private String contact;

    @Schema(description = "手机号码", example = "15601691300")
    @ExcelProperty("手机号码")
    private String mobile;

    @Schema(description = "联系电话", example = "18818288888")
    @ExcelProperty("联系电话")
    private String telephone;

    @Schema(description = "电子邮箱", example = "76853@qq.com")
    @ExcelProperty("电子邮箱")
    private String email;

    @Schema(description = "传真", example = "20 7123 4567")
    @ExcelProperty("传真")
    private String fax;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "开启状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "纳税人识别号", example = "91130803MA098BY05W")
    @ExcelProperty("纳税人识别号")
    private String taxNo;

    @Schema(description = "税率", example = "10")
    @ExcelProperty("税率")
    private BigDecimal taxPercent;

    @Schema(description = "开户行", example = "张三")
    @ExcelProperty("开户行")
    private String bankName;

    @Schema(description = "开户账号", example = "622908212277228617")
    @ExcelProperty("开户账号")
    private String bankAccount;

    @Schema(description = "开户地址", example = "兴业银行浦东支行")
    @ExcelProperty("开户地址")
    private String bankAddress;

    // ========== 汽配扩展字段 ==========

    @Schema(description = "编码(自动生成)", example = "GYS001")
    @ExcelProperty("编码")
    private String code;

    @Schema(description = "旧编码", example = "OLD001")
    @ExcelProperty("旧编码")
    private String oldCode;

    @Schema(description = "简称", example = "芋道")
    @ExcelProperty("简称")
    private String shortName;

    @Schema(description = "外文名", example = "YuDao")
    @ExcelProperty("外文名")
    private String foreignName;

    @Schema(description = "区域", example = "华东")
    @ExcelProperty("区域")
    private String region;

    @Schema(description = "往来类别", example = "供应商")
    @ExcelProperty("往来类别")
    private String category;

    @Schema(description = "账户", example = "ACC001")
    @ExcelProperty("账户")
    private String account;

    @Schema(description = "结算方式", example = "月结")
    @ExcelProperty("结算方式")
    private String settleMethod;

    @Schema(description = "结算锁定", example = "false")
    @ExcelProperty("结算锁定")
    private Boolean settleLocked;

    @Schema(description = "供应商类型", example = "生产厂家")
    @ExcelProperty("供应商类型")
    private String supplierType;

    @Schema(description = "到货周期(天)", example = "7")
    @ExcelProperty("到货周期(天)")
    private Integer arrivalCycle;

    @Schema(description = "采购提前期(天)", example = "3")
    @ExcelProperty("采购提前期(天)")
    private Integer purchaseLeadDays;

    @Schema(description = "运输方式", example = "公路")
    @ExcelProperty("运输方式")
    private String transportMethod;

    @Schema(description = "运费类型", example = "到付")
    @ExcelProperty("运费类型")
    private String freightType;

    @Schema(description = "五笔码", example = "YDYM")
    @ExcelProperty("五笔码")
    private String wubiCode;

    @Schema(description = "拼音码", example = "YDYM")
    @ExcelProperty("拼音码")
    private String pinyinCode;

    @Schema(description = "采购员", example = "张三")
    @ExcelProperty("采购员")
    private String purchaser;

    @Schema(description = "公司性质", example = "民营")
    @ExcelProperty("公司性质")
    private String companyNature;

    @Schema(description = "淘汰", example = "false")
    @ExcelProperty("淘汰")
    private Boolean obsolete;

    @Schema(description = "淘汰日期")
    @ExcelProperty("淘汰日期")
    private LocalDateTime obsoleteDate;

    @Schema(description = "开票类型", example = "增值税专用发票")
    @ExcelProperty("开票类型")
    private String invoiceType;

    @Schema(description = "集团供应商", example = "false")
    @ExcelProperty("集团供应商")
    private Boolean groupSupplier;

    @Schema(description = "是否允许分店开单", example = "true")
    @ExcelProperty("是否允许分店开单")
    private Boolean allowBranchOrder;

    @Schema(description = "物流公司", example = "顺丰")
    @ExcelProperty("物流公司")
    private String logisticsCompany;

    @Schema(description = "到货点", example = "仓库A")
    @ExcelProperty("到货点")
    private String arrivalPoint;

    @Schema(description = "邮政编码", example = "200000")
    @ExcelProperty("邮政编码")
    private String postalCode;

    @Schema(description = "会员编码(自动生成)", example = "HY001")
    @ExcelProperty("会员编码")
    private String memberCode;

    @Schema(description = "地址", example = "上海市浦东新区")
    @ExcelProperty("地址")
    private String address;

    @Schema(description = "省", example = "上海")
    @ExcelProperty("省")
    private String province;

    @Schema(description = "市", example = "上海市")
    @ExcelProperty("市")
    private String city;

    @Schema(description = "区/县", example = "浦东新区")
    @ExcelProperty("区/县")
    private String district;

    @Schema(description = "网址", example = "https://www.iocoder.cn")
    @ExcelProperty("网址")
    private String website;

    @Schema(description = "法定代表", example = "张三")
    @ExcelProperty("法定代表")
    private String legalPerson;

    @Schema(description = "统一信用代码", example = "91130803MA098BY05W")
    @ExcelProperty("统一信用代码")
    private String creditCode;

    @Schema(description = "采购管控", example = "严格")
    @ExcelProperty("采购管控")
    private String purchaseControl;

    @Schema(description = "浮动是否更新供应商最后进价", example = "是")
    @ExcelProperty("浮动是否更新供应商最后进价")
    private String floatUpdateLastPrice;

    @Schema(description = "纳税人识别号", example = "91130803MA098BY05W")
    @ExcelProperty("纳税人识别号")
    private String taxpayerId;

    @Schema(description = "开票银行", example = "工商银行")
    @ExcelProperty("开票银行")
    private String invoiceBank;

    @Schema(description = "开票银行账号", example = "622908212277228617")
    @ExcelProperty("开票银行账号")
    private String invoiceBankAccount;

    @Schema(description = "开票地址", example = "上海市浦东新区")
    @ExcelProperty("开票地址")
    private String invoiceAddress;

    @Schema(description = "开票电话", example = "021-12345678")
    @ExcelProperty("开票电话")
    private String invoicePhone;

    @Schema(description = "开票单位", example = "芋道源码有限公司")
    @ExcelProperty("开票单位")
    private String invoiceCompany;

    @Schema(description = "财务联系电话", example = "021-87654321")
    @ExcelProperty("财务联系电话")
    private String financePhone;

    @Schema(description = "绩效考核利润参考依据", example = "毛利率")
    @ExcelProperty("绩效考核利润参考依据")
    private String performanceProfitRef;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
