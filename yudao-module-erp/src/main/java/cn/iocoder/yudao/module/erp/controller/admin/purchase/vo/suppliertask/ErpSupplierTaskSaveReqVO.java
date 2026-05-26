package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliertask;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 供应商任务量新增/修改 Request VO")
@Data
public class ErpSupplierTaskSaveReqVO {

    private Long id;
    @NotNull(message = "供应商不能为空")
    private Long supplierId;
    @NotNull(message = "年份不能为空")
    private Integer year;
    @NotNull(message = "月份不能为空")
    private Integer month;
    private String taskLevel;
    @NotNull(message = "任务量不能为空")
    private BigDecimal taskAmount;
    private String remark;

}
