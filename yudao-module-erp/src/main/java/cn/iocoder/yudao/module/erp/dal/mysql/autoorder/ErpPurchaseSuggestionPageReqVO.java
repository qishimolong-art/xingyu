package cn.iocoder.yudao.module.erp.dal.mysql.autoorder;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 采购建议单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseSuggestionPageReqVO extends PageParam {

    @Schema(description = "建议单号")
    private String no;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "仓库编号")
    private Long warehouseId;

}
