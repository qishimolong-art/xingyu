package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ErpCustomerImageSaveReqVO {

    private Long id;

    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    private String imageType;
    private String imageName;

    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

    private Boolean defaulted;
    private Integer sort;
    private String remark;

}
