package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 凭证字分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpVoucherWordPageReqVO extends PageParam {

    @Schema(description = "凭证字代码", example = "记")
    private String code;

    @Schema(description = "名称", example = "记账凭证")
    private String name;

    @Schema(description = "是否启用", example = "true")
    private Boolean enable;

}
