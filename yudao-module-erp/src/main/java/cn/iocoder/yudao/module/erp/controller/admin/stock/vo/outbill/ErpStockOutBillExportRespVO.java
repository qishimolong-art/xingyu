package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpStockOutBillExportRespVO {

    @ExcelProperty("优先级")
    private String priority;

    @ExcelProperty("单号")
    private String no;

    @ExcelProperty("拣货")
    private String pick;

    @ExcelProperty("拣货人")
    private String pickUserName;

    @ExcelProperty("日期")
    private LocalDateTime billDate;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("发货区")
    private String shippingArea;

    @ExcelProperty("来源单位")
    private String sourceUnitName;

    @ExcelProperty("来源单号")
    private String sourceNo;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("修改人")
    private String updaterName;

    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;

    @ExcelProperty("审核人")
    private String auditorName;

    @ExcelProperty("审核时间")
    private LocalDateTime auditTime;

    @ExcelProperty("打印时间")
    private LocalDateTime printTime;

    @ExcelProperty("打印次数")
    private Integer printCount;

    @ExcelProperty("来源单据备注")
    private String sourceRemark;

    @ExcelProperty("总重")
    private BigDecimal totalWeight;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("超时")
    private Boolean timeoutFlag;

    @ExcelProperty("整件数")
    private BigDecimal wholeQty;

    @ExcelProperty("散件数")
    private BigDecimal looseQty;

    @ExcelProperty("应拣数量")
    private BigDecimal totalCount;

    @ExcelProperty("已拣数量")
    private BigDecimal pickedCount;

    @ExcelProperty("明细来源单号")
    private String itemSourceNo;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("包装数")
    private Integer packageQty;

    @ExcelProperty("明细整件数")
    private Integer itemWholeQty;

    @ExcelProperty("应拣明细数量")
    private BigDecimal itemCount;

    @ExcelProperty("已拣明细数量")
    private BigDecimal itemPickedCount;

    @ExcelProperty("剩余待拣数量")
    private BigDecimal itemRemainCount;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelProperty("金额")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("明细仓库")
    private String itemWarehouseName;

    @ExcelProperty("货架位")
    private String itemWarehousePosition;

    @ExcelProperty("图号")
    private String drawingNo;

    @ExcelProperty("批次")
    private String batchNo;

    @ExcelProperty("条形码")
    private String barCode;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("适用车型")
    private String vehicleModel;

    @ExcelProperty("产地")
    private String originPlace;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
