package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 自动核销配置更新 Request VO")
@Data
public class ErpAutoWriteOffConfigUpdateReqVO {

    @Schema(description = "关闭自动核销的配置部门编号列表")
    @NotNull(message = "关闭自动核销部门不能为空")
    private List<Long> disabledDeptIds;

}
