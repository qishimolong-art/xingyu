package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理后台 - ERP 供应商异步导出任务创建 Request VO")
public class ErpSupplierExportTaskStartReqVO extends ErpSupplierPageReqVO {

    @NotEmpty(message = "请至少选择一个导出字段")
    private List<String> fields;

    private String captchaCode;
    private String verifyCode;

}
