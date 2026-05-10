package cn.iocoder.yudao.module.erp.controller.admin.product.vo.pricesystem;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 价格体系新增/修改 Request VO")
@Data
public class ErpPriceSystemSaveReqVO {

    @Schema(description = "价格体系编号", example = "1024")
    private Long id;

    @Schema(description = "编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PS001")
    @NotEmpty(message = "编码不能为空")
    private String code;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "批发价")
    @NotEmpty(message = "名称不能为空")
    private String name;

    @Schema(description = "状态", example = "0")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

}
