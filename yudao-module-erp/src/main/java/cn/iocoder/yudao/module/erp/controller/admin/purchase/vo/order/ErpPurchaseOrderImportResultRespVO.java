package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购订单整单导入 Response VO")
@Data
public class ErpPurchaseOrderImportResultRespVO {

    @Schema(description = "成功单据数")
    private Integer successCount = 0;

    @Schema(description = "失败行数")
    private Integer failureCount = 0;

    @Schema(description = "创建成功的采购订单编号")
    private List<Long> orderIds = new ArrayList<>();

    @Schema(description = "导入记录编号，用于下载错误数据")
    private Long recordId;

    @Schema(description = "失败明细")
    private List<FailureItem> failureDetails = new ArrayList<>();

    @Data
    public static class FailureItem {

        @Schema(description = "行号")
        private Integer rowNo;

        @Schema(description = "兼容旧版本字段，当前导入不再使用")
        private String groupNo;

        @Schema(description = "采购单号或订单标识")
        private String orderNo;

        @Schema(description = "产品编码")
        private String productCode;

        @Schema(description = "失败原因")
        private String reason;

        public FailureItem(Integer rowNo, String groupNo, String productCode, String reason) {
            this.rowNo = rowNo;
            this.groupNo = groupNo;
            this.productCode = productCode;
            this.reason = reason;
        }

        public FailureItem(Integer rowNo, String groupNo, String orderNo, String productCode, String reason) {
            this.rowNo = rowNo;
            this.groupNo = groupNo;
            this.orderNo = orderNo;
            this.productCode = productCode;
            this.reason = reason;
        }

    }

}
