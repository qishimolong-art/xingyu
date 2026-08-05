package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Schema(description = "管理后台 - ERP 结算账户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAccountPageReqVO extends PageParam {

    @Schema(description = "勾选导出的账户编号数组", example = "[1,2,3]")
    private List<Long> ids;

    @Schema(description = "账户编码", example = "A88")
    private String no;

    @Schema(description = "账户名称", example = "基本账户")
    private String name;

    @Schema(description = "账户类型", example = "1")
    private Integer accountType;

    @Schema(description = "所属部门", example = "100")
    private Long deptId;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "提交状态：0-草稿 10-正式", example = "10")
    private Integer documentStatus;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "排序字段")
    private String orderField;

    @Schema(description = "排序方向（asc/desc）")
    private String orderDirection;

}
