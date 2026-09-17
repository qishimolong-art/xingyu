package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售送货单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSaleDeliveryPageReqVO extends PageParam {

    @Schema(description = "送货单编号列表")
    private List<Long> ids;

    @Schema(description = "销售单号")
    private String saleOutNo;

    @Schema(description = "客户编号")
    private Long customerId;

    @Schema(description = "拣货状态")
    private Integer pickStatus;

    @Schema(description = "送货状态")
    private Integer deliveryStatus;

    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;

    @Schema(description = "送货完成时间")
    private LocalDateTime[] deliveryCompleteTime;

}
