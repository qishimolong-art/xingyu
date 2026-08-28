package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customermember;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 客户小程序授权 Response VO")
@Data
public class ErpCustomerMemberRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "客户编号", example = "1024")
    private Long customerId;

    @Schema(description = "客户名称", example = "星宇汽配")
    private String customerName;

    @Schema(description = "会员用户编号", example = "2048")
    private Long memberUserId;

    @Schema(description = "会员昵称", example = "张三")
    private String memberNickname;

    @Schema(description = "授权时手机号快照", example = "15601691300")
    private String mobile;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "客户小程序下单账号")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
