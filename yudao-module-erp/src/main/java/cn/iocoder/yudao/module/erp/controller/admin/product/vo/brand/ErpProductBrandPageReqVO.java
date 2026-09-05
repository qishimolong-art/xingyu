package cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 配件品牌分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpProductBrandPageReqVO extends PageParam {

    @Schema(description = "品牌名称", example = "博世")
    private String name;

    @Schema(description = "品牌状态", example = "0")
    private Integer status;

    @Schema(description = "排序字段", example = "createTime")
    private String orderField;

    @Schema(description = "排序方向", example = "desc")
    private String orderDirection;

}
