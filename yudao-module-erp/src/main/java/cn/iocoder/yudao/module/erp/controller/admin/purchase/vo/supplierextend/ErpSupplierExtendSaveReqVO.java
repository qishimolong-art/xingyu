package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierextend;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 供应商动态拓展字段新增/修改 Request VO")
@Data
public class ErpSupplierExtendSaveReqVO {

    private Long id;
    @NotNull(message = "供应商不能为空")
    private Long supplierId;
    @NotEmpty(message = "字段标识不能为空")
    private String extendKey;
    private String extendName;
    private String extendValue;
    private String extendType;
    private Boolean required;
    private String optionValues;
    private Integer sort;
    private String remark;

}
