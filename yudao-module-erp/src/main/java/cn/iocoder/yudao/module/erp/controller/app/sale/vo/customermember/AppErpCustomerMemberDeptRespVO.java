package cn.iocoder.yudao.module.erp.controller.app.sale.vo.customermember;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 App - ERP 客户可用部门 Response VO")
@Data
public class AppErpCustomerMemberDeptRespVO {

    @Schema(description = "部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long id;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "成都主城区销售账套")
    private String name;

    @Schema(description = "父部门编号", example = "0")
    private Long parentId;

    @Schema(description = "联系电话", example = "028-88888888")
    private String phone;

    @Schema(description = "详细地址", example = "四川省成都市高新区天府大道 1 号")
    private String address;

    @Schema(description = "经度", example = "104.065735")
    private BigDecimal longitude;

    @Schema(description = "纬度", example = "30.659462")
    private BigDecimal latitude;

    @Schema(description = "地图显示名称", example = "兴宇成都仓")
    private String mapName;

}
