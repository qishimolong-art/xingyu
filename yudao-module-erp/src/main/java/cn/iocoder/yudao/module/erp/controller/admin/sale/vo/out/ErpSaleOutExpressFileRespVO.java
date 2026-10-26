package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 销售单快递单 Response VO")
@Data
@Accessors(chain = true)
public class ErpSaleOutExpressFileRespVO {

    @Schema(description = "送货附件编号；人工补传快递单为空")
    private Long id;

    @Schema(description = "送货提交编号；人工补传快递单为空")
    private Long submitId;

    @Schema(description = "文件地址")
    private String fileUrl;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "来源类型：MANUAL 人工补传，DELIVERY 送货上传")
    private String sourceType;

    @Schema(description = "来源名称")
    private String sourceName;

    @Schema(description = "提交人编号")
    private Long submitUserId;

    @Schema(description = "提交人名称")
    private String submitUserName;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "附件排序")
    private Integer sort;

}
