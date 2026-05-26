package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierimage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 供应商图片新增/修改 Request VO")
@Data
public class ErpSupplierImageSaveReqVO {

    private Long id;
    @NotNull(message = "供应商不能为空")
    private Long supplierId;
    @NotEmpty(message = "图片类型不能为空")
    private String imageType;
    private String imageName;
    @NotEmpty(message = "图片地址不能为空")
    private String imageUrl;
    private Boolean defaulted;
    private Integer sort;
    private String remark;

}
