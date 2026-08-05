package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - ERP 采购订单修改备注 Request VO")
@Data
public class ErpPurchaseOrderUpdateRemarkReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "备注", example = "请优先安排发货")
    @NotNull(message = "备注不能为 null，清空备注请传空字符串")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}
