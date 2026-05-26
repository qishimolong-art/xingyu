package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 客户联系人 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpCustomerContactRespVO {

    @Schema(description = "编号")
    @ExcelProperty("编号")
    private Long id;
    private Long customerId;
    @ExcelProperty("联系人")
    private String name;
    @ExcelProperty("手机号")
    private String mobile;
    private String telephone;
    private String email;
    private String position;
    private String wechat;
    private String qq;
    private String address;
    private Boolean primaryContact;
    private Boolean receiverContact;
    private Boolean settleContact;
    private Boolean messageContact;
    private String businessCardFrontUrl;
    private String businessCardBackUrl;
    private Integer status;

    @Schema(description = "是否导购员")
    private Boolean salesperson;
    @Schema(description = "性别 1=男 2=女")
    private Integer gender;
    @Schema(description = "所属公司（客户ID）")
    private Long companyId;
    @Schema(description = "传真")
    private String fax;
    @Schema(description = "所属部门")
    private Long deptId;
    @Schema(description = "重要性 1=普通 2=重要 3=决策人")
    private Integer importance;
    @Schema(description = "提成率（百分比）")
    private BigDecimal commissionRate;
    @Schema(description = "固定提成")
    private Boolean fixedCommission;
    @Schema(description = "邮编")
    private String postCode;
    @Schema(description = "生日")
    private LocalDate birthday;
    @Schema(description = "最后联系时间")
    private LocalDateTime lastContactTime;

    private String remark;
    private LocalDateTime createTime;

}
