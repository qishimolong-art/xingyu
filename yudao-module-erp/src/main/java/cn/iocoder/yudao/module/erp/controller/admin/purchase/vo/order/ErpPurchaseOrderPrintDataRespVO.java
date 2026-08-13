package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - ERP 采购订单打印数据 Response VO")
@Data
public class ErpPurchaseOrderPrintDataRespVO {

    private Map<String, Object> main;
    private List<Map<String, Object>> items;
    private Map<String, Object> system;

}
