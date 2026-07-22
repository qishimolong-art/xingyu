package cn.iocoder.yudao.module.system.controller.admin.logger.vo.operatelog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 操作日志模块选项 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperateLogModuleOptionRespVO {

    @Schema(description = "显示名称", example = "仓库信息")
    private String label;

    @Schema(description = "选项值", example = "仓库信息")
    private String value;

}
