package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 系统开账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpBookOpenPageReqVO extends PageParam {

    @Schema(description = "开账编号", example = "KZ20260514000001")
    private String no;

    @Schema(description = "连锁名称", example = "总部")
    private String chainName;

    @Schema(description = "会计年度", example = "2026")
    private Integer fiscalYear;

    @Schema(description = "开账期间（兼容旧查询字段；年度开账固定为 1）", example = "1")
    private Integer period;

    @Schema(description = "是否开账", example = "true")
    private Boolean opened;

}
