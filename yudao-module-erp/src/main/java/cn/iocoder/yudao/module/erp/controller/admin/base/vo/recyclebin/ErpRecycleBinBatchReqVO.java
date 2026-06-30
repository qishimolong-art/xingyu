package cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - ERP 基础档案回收站批量操作 Request VO")
@Data
public class ErpRecycleBinBatchReqVO {

    @Schema(description = "操作项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "操作项不能为空")
    private List<ErpRecycleBinItemReqVO> items;

}
