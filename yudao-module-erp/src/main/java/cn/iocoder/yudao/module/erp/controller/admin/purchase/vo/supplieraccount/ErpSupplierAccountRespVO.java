package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplieraccount;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 供应商账户 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpSupplierAccountRespVO extends ErpSupplierAccountSaveReqVO {

    private LocalDateTime createTime;

}
