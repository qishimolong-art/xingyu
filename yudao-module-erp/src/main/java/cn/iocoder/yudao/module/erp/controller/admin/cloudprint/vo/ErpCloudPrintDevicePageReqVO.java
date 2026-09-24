package cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 云打印设备分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCloudPrintDevicePageReqVO extends PageParam {

    @Schema(description = "关键字，匹配设备别名或机器码", example = "成都")
    private String keyword;

    @Schema(description = "设备类型：1=80mm热敏 2=标签 3=A4盒子 4=针式", example = "4")
    private Integer devType;

    @Schema(description = "内容类型：7=PDF 9=HTML", example = "7")
    private Integer contentType;

    @Schema(description = "状态：0=启用 1=停用", example = "0")
    private Integer status;

    @Schema(description = "在线状态：0=离线 1=在线", example = "1")
    private Integer onlineState;

    @Schema(description = "归属部门", example = "1")
    private Long deptId;

}
