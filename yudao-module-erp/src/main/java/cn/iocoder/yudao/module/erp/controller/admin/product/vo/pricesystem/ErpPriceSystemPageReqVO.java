package cn.iocoder.yudao.module.erp.controller.admin.product.vo.pricesystem;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 价格体系分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPriceSystemPageReqVO extends PageParam {

    @Schema(description = "名称", example = "批发价")
    private String name;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "编码", example = "PS001")
    private String code;

}
