package cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpArchiveExportTaskStartRespVO {

    private Long taskId;
    private Boolean reused;

}
