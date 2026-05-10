package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontact;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 客户联系人保存 Request VO")
@Data
public class ErpCustomerContactSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @Schema(description = "联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "联系人姓名不能为空")
    private String name;

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
    private String remark;

}
