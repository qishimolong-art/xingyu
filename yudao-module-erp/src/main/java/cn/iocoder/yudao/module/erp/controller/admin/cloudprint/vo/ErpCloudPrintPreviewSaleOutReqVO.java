package cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class ErpCloudPrintPreviewSaleOutReqVO {

    @NotNull(message = "销售出库单编号不能为空")
    private Long saleOutId;

    @NotNull(message = "打印模板不能为空")
    private Long templateId;

    @Min(value = 1, message = "打印份数不能小于 1")
    @Max(value = 99, message = "打印份数不能大于 99")
    private Integer copies;

    private String itemOrderField;

    private String itemOrderDirection;
}
