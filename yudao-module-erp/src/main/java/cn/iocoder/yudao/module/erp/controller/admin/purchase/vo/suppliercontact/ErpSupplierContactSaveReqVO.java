package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliercontact;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 供应商联系人新增/修改 Request VO")
@Data
public class ErpSupplierContactSaveReqVO {

    private Long id;
    @NotNull(message = "供应商不能为空")
    private Long supplierId;
    @NotEmpty(message = "姓名不能为空")
    private String name;
    private Boolean salesperson;
    private String telephone;
    @NotEmpty(message = "移动电话不能为空")
    private String mobile;
    private String address;
    private String email;
    private Integer gender;
    private String position;
    private Long companyId;
    private String fax;
    private Long deptId;
    private Integer importance;
    private BigDecimal commissionRate;
    private Boolean fixedCommission;
    private String postCode;
    private String qq;
    private LocalDate birthday;
    private Boolean primaryContact;
    private Boolean receiverContact;
    private Boolean settleContact;
    private Boolean messageContact;
    private String wechat;
    private Integer wechatOfficialStatus;
    private Boolean orderAccess;
    private Boolean ecommerceAccess;
    private String businessCardFrontUrl;
    private String businessCardBackUrl;
    private LocalDateTime lastContactTime;
    private Integer status;
    private String remark;

}
