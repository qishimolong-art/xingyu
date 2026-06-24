package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - ERP 仓库批量修改 Request VO")
@Data
public class ErpWarehouseBatchUpdateReqVO {

    @Schema(description = "仓库编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "仓库编号列表不能为空")
    private List<Long> ids;

    @Schema(description = "所属部门", example = "100")
    private Long deptId;

    @Schema(description = "仓库类型")
    private Integer warehouseType;

    @Schema(description = "开启状态", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "开启状态必须是 {value}")
    private Integer status;

    @Schema(description = "销售启用")
    private Boolean saleEnabled;

    @Schema(description = "采购启用")
    private Boolean purchaseEnabled;

    @Schema(description = "入出仓单")
    private Boolean stockBillEnabled;

    @Schema(description = "扫码管控")
    private Boolean scanControl;

    @Schema(description = "是否拆单")
    private Boolean splitOrder;

    @Schema(description = "排序")
    private Long sort;

    @Schema(description = "备注")
    private String remark;

}
