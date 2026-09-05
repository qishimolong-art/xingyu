package cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - ERP 配件品牌新增/修改 Request VO")
@Data
public class ErpProductBrandSaveReqVO {

    @Schema(description = "品牌编号", example = "1024")
    private Long id;

    @Schema(description = "品牌名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "博世")
    @NotEmpty(message = "品牌名称不能为空")
    @Size(max = 64, message = "品牌名称长度不能超过 64 个字符")
    private String name;

    @Schema(description = "品牌状态", example = "0")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

}
