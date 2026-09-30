package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售拣货提交 Request VO")
@Data
public class ErpSalePickSubmitReqVO {

    @Schema(description = "拣货任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "拣货任务编号不能为空")
    private Long taskId;

    @Schema(description = "旧版整条拣货明细编号，与 items 二选一")
    private List<Long> itemIds;

    @Schema(description = "本次分批数量，与 itemIds 二选一")
    @Valid
    private List<ErpSalePickDeliverySubmitItemReqVO> items;

    @Schema(description = "分批提交请求唯一标识，重试时必须复用")
    private String requestId;

    @Schema(description = "本次拣货凭证", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "请至少上传一张拣货凭证")
    private List<File> files;

    @Schema(description = "备注")
    private String remark;

    @Data
    public static class File {

        @Schema(description = "文件地址", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "凭证文件地址不能为空")
        private String fileUrl;

        @Schema(description = "文件名")
        private String fileName;

        @Schema(description = "文件类型")
        private String fileType;

    }

}
