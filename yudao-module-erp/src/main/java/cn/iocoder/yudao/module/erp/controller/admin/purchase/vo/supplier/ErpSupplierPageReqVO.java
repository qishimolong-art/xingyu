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

    @Schema(description = "供应商编码", example = "GYS0001")
    private String code;

    @Schema(description = "手机号码", example = "15601691300")
    private String mobile;

    @Schema(description = "联系电话", example = "18818288888")
    private String telephone;

    @Schema(description = "联系方式，匹配手机号码或联系电话", example = "18818288888")
    private String contactInfo;

    @Schema(description = "所属部门")
    private Long deptId;

    @Schema(description = "区域", example = "华东")
    private String region;

    @Schema(description = "默认结算方式", example = "月结")
    private String settleMethod;

    @Schema(description = "采购员", example = "1")
    private String purchaser;

    @Schema(description = "供应商类别", example = "供应商")
    private String category;

    @Schema(description = "地址", example = "上海市")
    private String address;

    @Schema(description = "备注", example = "长期合作")
    private String remark;

    @Schema(description = "创建人", example = "1")
    private String creator;

    @Schema(description = "Sort field, supports: code, name, status, region, address, remark, createTime, mobile, telephone, purchaser")
    private String orderField;

    @Schema(description = "Sort direction: asc or desc")
    private String orderDirection;

    @Schema(description = "勾选导出的供应商编号数组", example = "[1,2,3]")
    private List<Long> ids;

}
