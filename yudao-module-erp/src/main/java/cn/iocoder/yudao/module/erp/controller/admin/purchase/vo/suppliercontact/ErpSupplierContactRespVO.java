package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliercontact;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 供应商联系人 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpSupplierContactRespVO extends ErpSupplierContactSaveReqVO {

    private LocalDateTime createTime;

}
