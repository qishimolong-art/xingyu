package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售送货提交 Request VO")
@Data
public class ErpSaleDeliverySubmitReqVO {

    @Schema(description = "送货单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "送货单编号不能为空")
    private Long orderId;

    @Schema(description = "旧版整条送货明细编号，与 items 二选一")
    private List<Long> itemIds;

    @Schema(description = "本次分批数量，与 itemIds 二选一")
    @Valid
    private List<ErpSalePickDeliverySubmitItemReqVO> items;

    @Schema(description = "分批提交请求唯一标识，重试时必须复用")
    private String requestId;

    @Schema(description = "本次送货凭证", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "请至少上传一张送货凭证")
    private List<ErpSalePickSubmitReqVO.File> files;

    @Schema(description = "备注")
    private String remark;

}
