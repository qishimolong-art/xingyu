package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 销售拣货送货凭证 Response VO")
@Data
public class ErpSalePickDeliveryFileRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "提交批次编号")
    private Long submitId;

    @Schema(description = "文件地址")
    private String fileUrl;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "排序")
    private Integer sort;

}
