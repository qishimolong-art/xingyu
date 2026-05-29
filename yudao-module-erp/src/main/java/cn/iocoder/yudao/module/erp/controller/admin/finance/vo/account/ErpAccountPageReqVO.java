package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 结算账户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAccountPageReqVO extends PageParam {

    @Schema(description = "账户编码", example = "A88")
    private String no;

    @Schema(description = "账户名称", example = "基本户")
    private String name;

    @Schema(description = "账户类型", example = "1")
    private Integer accountType;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
