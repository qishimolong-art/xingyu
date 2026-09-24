package cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 云打印设备新增/修改 Request VO")
@Data
public class ErpCloudPrintDeviceSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "设备机器码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SW263000012")
    @NotBlank(message = "设备机器码不能为空")
    private String devid;

    @Schema(description = "设备绑定校验码，编辑时留空表示保持原值")
    private String devKey;

    @Schema(description = "设备别名", requiredMode = Schema.RequiredMode.REQUIRED, example = "销售单默认云打印机")
    @NotBlank(message = "设备别名不能为空")
    private String nickname;

    @Schema(description = "设备类型：1=80mm热敏 2=标签 3=A4盒子 4=针式", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    @NotNull(message = "设备类型不能为空")
    private Integer devType;

    @Schema(description = "内容类型：7=PDF 9=HTML", requiredMode = Schema.RequiredMode.REQUIRED, example = "7")
    @NotNull(message = "内容类型不能为空")
    private Integer contentType;

    @Schema(description = "有效打印宽度(mm)", requiredMode = Schema.RequiredMode.REQUIRED, example = "241")
    @NotNull(message = "有效打印宽度不能为空")
    @Min(value = 1, message = "有效打印宽度必须大于 0")
    private Integer printWidth;

    @Schema(description = "纸张高度(mm)，针式必填", example = "140")
    @Min(value = 1, message = "纸张高度必须大于 0")
    private Integer printHeight;

    @Schema(description = "纸张类型", example = "4")
    private Integer paperType;

    @Schema(description = "旋转参数", example = "0")
    private Integer rotate;

    @Schema(description = "默认份数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "默认份数不能为空")
    @Min(value = 1, message = "默认份数必须大于 0")
    private Integer copies;

    @Schema(description = "归属部门", example = "1")
    private Long deptId;

    @Schema(description = "是否默认设备", example = "false")
    private Boolean defaulted;

    @Schema(description = "状态：0=启用 1=停用", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
