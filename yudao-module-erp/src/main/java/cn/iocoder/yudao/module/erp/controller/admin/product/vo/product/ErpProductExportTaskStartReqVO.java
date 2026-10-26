package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - 配件信息异步导出任务创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpProductExportTaskStartReqVO extends ErpProductPageReqVO {

    @NotEmpty(message = "请至少选择一个导出字段")
    private List<String> fields;

    private String captchaCode;
    private String verifyCode;

}
