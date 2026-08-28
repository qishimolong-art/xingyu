package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - ERP 调拨出入库台账配对明细 Response VO")
@Data
@Accessors(chain = true)
@ExcelIgnoreUnannotated
public class ErpStockTransferLedgerDetailRespVO {

    private String rowKey;
    private Long groupKey;

    @ExcelProperty("业务日期")
    private LocalDate businessDate;
    private String matchStatus;

    private Long transferOutId;
    @ExcelProperty("调拨出库单号")
    private String transferOutNo;
    private Long transferInId;
    @ExcelProperty("调拨入库单号")
    private String transferInNo;
    @ExcelProperty("来源单号")
    private String sourceNo;

    private Long fromDeptId;
    @ExcelProperty("调出部门")
    private String fromDeptName;
    private Long toDeptId;
    @ExcelProperty("调入部门")
    private String toDeptName;

    private Long productId;
    @ExcelProperty("产品编码")
    private String productCode;
    @ExcelProperty("产品名称")
    private String productName;
    private Long fromWarehouseId;
    @ExcelProperty("调出仓库")
    private String fromWarehouseName;
    private Long toWarehouseId;
    @ExcelProperty("调入仓库")
    private String toWarehouseName;
    @ExcelProperty("批次号")
    private String batchNo;
    @ExcelProperty("单位")
    private String productUnitName;
    @ExcelProperty("包装数")
    private Integer packageQty;
    @ExcelProperty("单重")
    private BigDecimal weight;
    @ExcelProperty("总重")
    private BigDecimal totalWeight;

    @ExcelProperty("出库数量")
    private BigDecimal transferOutCount;
    @ExcelProperty("入库数量")
    private BigDecimal transferInCount;
    @ExcelProperty("差异数量")
    private BigDecimal differenceCount;
    private Integer transferOutStatus;
    private Integer transferInStatus;
    private List<String> exceptionCodes;
    @ExcelProperty("异常原因")
    private String exceptionReason;

}
