package cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ErpCloudPrintSubmitSaleOutReqVO {

    @NotNull(message = "销售单编号不能为空")
    private Long saleOutId;
    private Long deviceId;
    private Integer copies;
    private Long templateId;

}
