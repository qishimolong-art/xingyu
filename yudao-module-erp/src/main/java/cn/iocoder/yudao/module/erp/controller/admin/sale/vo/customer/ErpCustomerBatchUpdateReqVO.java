package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - ERP 客户批量编辑 Request VO")
@Data
public class ErpCustomerBatchUpdateReqVO {

    @Schema(description = "客户编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "客户编号列表不能为空")
    private List<Long> ids;

    @Schema(description = "所属业务员")
    private Long saleUserId;

    @Schema(description = "所属开发员")
    private Long developerUserId;

    @Schema(description = "所属部门")
    private Long deptId;

    @Schema(description = "开启状态")
    private Integer status;

    @Schema(description = "价格级别")
    private Integer priceLevel;

    @Schema(description = "线路编号")
    private Long routeId;

    @Schema(description = "运费说明编号")
    private Long freightExplainId;

    @Schema(description = "备注")
    private String remark;

}
