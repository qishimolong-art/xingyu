package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 仓库分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpWarehousePageReqVO extends PageParam {

    @Schema(description = "仓库名称", example = "李四")
    private String name;

    @Schema(description = "开启状态", example = "1")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "仓库类型", example = "1")
    private Integer warehouseType;

    @Schema(description = "仓库编码", example = "WH001")
    private String warehouseCode;

    @Schema(description = "所属部门", example = "100")
    private Long deptId;

    @Schema(description = "销售启用", example = "true")
    private Boolean saleEnabled;

    @Schema(description = "采购启用", example = "true")
    private Boolean purchaseEnabled;

    @Schema(description = "入出仓单", example = "true")
    private Boolean stockBillEnabled;

    @Schema(description = "扫码管控", example = "true")
    private Boolean scanControl;

    @Schema(description = "是否拆单", example = "true")
    private Boolean splitOrder;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序字段", example = "name")
    private String orderField;

    @Schema(description = "排序方向", example = "asc")
    private String orderDirection;

}
