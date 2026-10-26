package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpWarehouseExportRespVO {

    @ExcelProperty(value = "仓库编号", converter = LongStringConverter.class)
    private Long id;

    @ExcelProperty(value = "仓库名称")
    private String name;

    @ExcelProperty(value = "所属部门ID", converter = LongStringConverter.class)
    private Long deptId;

    @ExcelProperty(value = "所属部门")
    private String deptName;

    @ExcelProperty(value = "仓库地址")
    private String address;

    @ExcelProperty(value = "地图名称")
    private String mapName;

    @ExcelProperty(value = "经度", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal longitude;

    @ExcelProperty(value = "纬度", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal latitude;

    @ExcelProperty(value = "排序")
    @NumberFormat("0")
    private Long sort;

    @ExcelProperty(value = "备注")
    private String remark;

    @ExcelProperty(value = "负责人")
    private String principal;

    @ExcelProperty(value = "仓储费，单位：元", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal warehousePrice;

    @ExcelProperty(value = "搬运费，单位：元", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal truckagePrice;

    @ExcelProperty(value = "开启状态")
    private String status;

    @ExcelProperty(value = "是否默认")
    private Boolean defaultStatus;

    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ExcelProperty(value = "仓库编码")
    private String warehouseCode;

    @ExcelProperty(value = "仓库类型")
    private String warehouseType;

    @ExcelProperty(value = "销售启用")
    private Boolean saleEnabled;

    @ExcelProperty(value = "采购启用")
    private Boolean purchaseEnabled;

    @ExcelProperty(value = "历史领货设置")
    private Boolean stockBillEnabled;

    @ExcelProperty(value = "扫码管控")
    private Boolean scanControl;

    @ExcelProperty(value = "是否拆单")
    private Boolean splitOrder;

    @ExcelProperty(value = "是否直发仓")
    private Boolean directWarehouse;

    @ExcelProperty(value = "云打印设备ID", converter = LongStringConverter.class)
    private Long cloudPrintDeviceId;

    @ExcelProperty(value = "创建人")
    private String creatorName;

    @ExcelProperty(value = "修改人")
    private String updaterName;

    @ExcelProperty(value = "修改时间")
    private LocalDateTime updateTime;
}
