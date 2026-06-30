package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - ERP 供应商批量编辑 Request VO")
@Data
public class ErpSupplierBatchUpdateReqVO {

    @Schema(description = "供应商编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "供应商编号列表不能为空")
    private List<Long> ids;

    @Schema(description = "开启状态")
    @InEnum(value = CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "所属部门")
    private Long deptId;

    @Schema(description = "适用部门")
    private List<Long> deptIds;

    @Schema(description = "允许多部门")
    private Boolean allowMultiDept;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "供应商类型")
    private String supplierType;

    @Schema(description = "采购员")
    private String purchaser;

    @Schema(description = "结算方式")
    private String settleMethod;

    @Schema(description = "备注")
    private String remark;

}
