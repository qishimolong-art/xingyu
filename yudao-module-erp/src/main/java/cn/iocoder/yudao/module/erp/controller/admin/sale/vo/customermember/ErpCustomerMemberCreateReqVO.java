package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customermember;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 客户小程序授权创建 Request VO")
@Data
public class ErpCustomerMemberCreateReqVO {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @Schema(description = "会员用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "会员用户编号不能为空")
    private Long memberUserId;

    @Schema(description = "备注", example = "客户小程序下单账号")
    private String remark;

}
