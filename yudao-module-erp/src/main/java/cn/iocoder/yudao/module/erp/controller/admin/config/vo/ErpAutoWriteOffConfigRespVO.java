package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - ERP 自动核销配置 Response VO")
@Data
public class ErpAutoWriteOffConfigRespVO {

    @Schema(description = "关闭自动核销的配置部门编号列表")
    private List<Long> disabledDeptIds;

    @Schema(description = "实际关闭自动核销的部门编号列表，包含配置部门及其下级部门")
    private List<Long> effectiveDisabledDeptIds;

}
