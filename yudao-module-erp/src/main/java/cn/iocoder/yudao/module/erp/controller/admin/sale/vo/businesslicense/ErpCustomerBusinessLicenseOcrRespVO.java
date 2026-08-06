package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.businesslicense;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - ERP 客户营业执照识别 Response VO")
@Data
public class ErpCustomerBusinessLicenseOcrRespVO {

    @Schema(description = "接口是否返回成功")
    private Boolean success;

    @Schema(description = "接口消息")
    private String message;

    @Schema(description = "统一信用代码")
    private String unifiedCreditCode;

    @Schema(description = "纳税人识别号")
    private String taxNo;

    @Schema(description = "开票单位")
    private String invoiceCompany;

    @Schema(description = "开票地址")
    private String invoiceAddress;

    @Schema(description = "法定代表人")
    private String legalPerson;

    @Schema(description = "企业类型")
    private String companyType;

    @Schema(description = "注册资本")
    private String registeredCapital;

    @Schema(description = "成立日期")
    private String establishDate;

    @Schema(description = "营业期限")
    private String validPeriod;

    @Schema(description = "经营范围")
    private String businessScope;

    @Schema(description = "解析后的主要数据")
    private Map<String, Object> data;

    @Schema(description = "接口原始返回")
    private Map<String, Object> raw;

}
