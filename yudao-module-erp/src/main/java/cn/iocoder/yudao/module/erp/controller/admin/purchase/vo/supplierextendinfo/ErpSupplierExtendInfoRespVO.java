package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierextendinfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 供应商结构化拓展信息 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpSupplierExtendInfoRespVO extends ErpSupplierExtendInfoSaveReqVO {

    private LocalDateTime createTime;

}
