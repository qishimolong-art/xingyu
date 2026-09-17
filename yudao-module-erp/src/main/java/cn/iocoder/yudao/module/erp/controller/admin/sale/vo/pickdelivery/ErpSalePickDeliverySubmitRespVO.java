package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售拣货送货提交批次 Response VO")
@Data
public class ErpSalePickDeliverySubmitRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "类型：10=拣货，20=送货")
    private Integer type;

    @Schema(description = "提交人编号")
    private Long submitUserId;

    @Schema(description = "提交人名称")
    private String submitUserName;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "提交明细数")
    private Integer itemCount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "凭证列表")
    private List<ErpSalePickDeliveryFileRespVO> files;

}
