package cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - ERP 简单档案导入失败明细")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpArchiveImportFailureRespVO {

    private Integer rowNo;
    private String name;
    private String reason;

}
