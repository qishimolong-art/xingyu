package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ContentStyle;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpSaleOrderExportRespVO {

    @ExcelProperty("销售单号")
    private String no;

    @ExcelProperty("客户名称")
    private String customerName;

    private Long customerId;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("业务员")
    private String saleUserName;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("下单时间")
    private LocalDateTime orderTime;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("合计数量")
    @NumberFormat("0.######")
    private BigDecimal totalCount;

    @ExcelProperty("出库数量")
    @NumberFormat("0.######")
    private BigDecimal outCount;

    @ExcelProperty("退货数量")
    @NumberFormat("0.######")
    private BigDecimal returnCount;

    @ExcelProperty("商品金额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal totalProductPrice;

    @ExcelProperty("合计金额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal totalPrice;

    @ExcelProperty("优惠金额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal discountPrice;

    @ExcelProperty("费用金额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal feeAmount;

    @ExcelProperty("定金金额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal depositPrice;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("修改人")
    private String updaterName;

    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("重量")
    @NumberFormat("0.######")
    private BigDecimal weight;

    @ExcelProperty("包装数")
    private Integer packageQty;

    @ExcelProperty("件数")
    @NumberFormat("0.######")
    private BigDecimal pieceCount;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("数量")
    @NumberFormat("0.######")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal productPrice;

    @ExcelProperty("最后销售价")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal lastSalePrice;

    @ExcelProperty("金额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("税率")
    @NumberFormat("0.######")
    private BigDecimal itemTaxPercent;

    @ExcelProperty("税额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal itemTaxPrice;

    @ExcelProperty("赠品")
    private String giftFlag;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("明细所属部门")
    private String itemDeptName;

    @ExcelProperty("库存")
    @NumberFormat("0.######")
    private BigDecimal stockCount;

    @ExcelProperty("已出库")
    @NumberFormat("0.######")
    private BigDecimal itemOutCount;

    @ExcelProperty("已退货")
    @NumberFormat("0.######")
    private BigDecimal itemReturnCount;

    @ExcelProperty("产品条码")
    private String productBarCode;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
