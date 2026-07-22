package cn.iocoder.yudao.module.erp.service.stock.bo;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存明细的创建 Request BO
 *
 * @author 芋道源码
 */
@Data
@NoArgsConstructor
public class ErpStockRecordCreateReqBO {

    /**
     * 产品编号
     */
    @NotNull(message = "产品编号不能为空")
    private Long productId;
    /**
     * 仓库编号
     */
    @NotNull(message = "仓库编号不能为空")
    private Long warehouseId;
    /**
     * 批次号
     */
    private String batchNo;
    /**
     * 出入库数量
     *
     * 正数，表示入库；负数，表示出库
     */
    @NotNull(message = "出入库数量不能为空")
    private BigDecimal count;

    /**
     * 业务类型
     */
    @NotNull(message = "业务类型不能为空")
    private Integer bizType;
    /**
     * 业务编号
     */
    @NotNull(message = "业务编号不能为空")
    private Long bizId;
    /**
     * 业务项编号
     */
    @NotNull(message = "业务项编号不能为空")
    private Long bizItemId;
    /**
     * 业务单号
     */
    @NotNull(message = "业务单号不能为空")
    private String bizNo;

    /**
     * 本次业务单价（入库进价；出库可为 null，由 Service 回填为当前成本均价）
     */
    private BigDecimal unitPrice;

    /**
     * 业务发生日期（为 null 时 Service 使用当前时间兜底）
     */
    private LocalDateTime bizDate;

    /**
     * 兼容老调用的 7 参构造：unitPrice / bizDate 保持 null，由 Service 内部决定如何处理
     */
    public ErpStockRecordCreateReqBO(Long productId, Long warehouseId, BigDecimal count,
                                     Integer bizType, Long bizId, Long bizItemId, String bizNo) {
        this(productId, warehouseId, count, bizType, bizId, bizItemId, bizNo, null, null);
    }

    public ErpStockRecordCreateReqBO(Long productId, Long warehouseId, BigDecimal count,
                                     Integer bizType, Long bizId, Long bizItemId, String bizNo,
                                     BigDecimal unitPrice, LocalDateTime bizDate) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.count = count;
        this.bizType = bizType;
        this.bizId = bizId;
        this.bizItemId = bizItemId;
        this.bizNo = bizNo;
        this.unitPrice = unitPrice;
        this.bizDate = bizDate;
    }

    public ErpStockRecordCreateReqBO(Long productId, Long warehouseId, String batchNo, BigDecimal count,
                                     Integer bizType, Long bizId, Long bizItemId, String bizNo,
                                     BigDecimal unitPrice, LocalDateTime bizDate) {
        this(productId, warehouseId, count, bizType, bizId, bizItemId, bizNo, unitPrice, bizDate);
        this.batchNo = batchNo;
    }

}
