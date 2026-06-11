package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Schema(description = "管理后台 - ERP 供应商分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSupplierPageReqVO extends PageParam {

    @Schema(description = "供应商名称", example = "芋道源码")
    private String name;

    @Schema(description = "手机号码", example = "15601691300")
    private String mobile;

    @Schema(description = "联系电话", example = "18818288888")
    private String telephone;

    @Schema(description = "所属部门")
    private Long deptId;

    @Schema(description = "Sort field, supports: code, name, status, region, address, remark, createTime, mobile, telephone, purchaser")
    private String orderField;

    @Schema(description = "Sort direction: asc or desc")
    private String orderDirection;

    @Schema(description = "勾选导出的供应商编号数组", example = "[1,2,3]")
    private List<Long> ids;

}
