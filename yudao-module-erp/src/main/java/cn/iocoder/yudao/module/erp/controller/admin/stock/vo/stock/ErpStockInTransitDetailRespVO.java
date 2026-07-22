package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 产品库存在途明细 Response VO")
@Data
public class ErpStockInTransitDetailRespVO {

    @Schema(description = "采购订单编号", example = "1")
    private Long orderId;

    @Schema(description = "采购订单明细编号", example = "10")
    private Long itemId;

    @Schema(description = "单号", example = "CGDD202607130001")
    private String no;

    @Schema(description = "配件编号", example = "100")
    private Long productId;

    @Schema(description = "配件编码", example = "P000001")
    private String productCode;

    @Schema(description = "配件名称", example = "机油滤芯")
    private String productName;

    @Schema(description = "仓库编号", example = "20")
    private Long warehouseId;

    @Schema(description = "仓库", example = "主仓")
    private String warehouseName;

    @Schema(description = "批次号", example = "BATCH-001")
    private String batchNo;

    @Schema(description = "数量", example = "10")
    private BigDecimal count;

    @Schema(description = "车型", example = "A6L")
    private String vehicleModel;

    @Schema(description = "产地", example = "上海")
    private String originPlace;

    @Schema(description = "图号", example = "DR-001")
    private String drawingNo;

    @Schema(description = "规格", example = "标准")
    private String standard;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人编号", example = "1")
    private String creator;

    @Schema(description = "创建人", example = "管理员")
    private String creatorName;

}
