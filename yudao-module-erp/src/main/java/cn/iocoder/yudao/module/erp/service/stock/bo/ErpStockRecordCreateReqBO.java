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
     * 产品单位编号快照
     */
    private Long productUnitId;
    /**
     * 包装数快照
     */
    private Integer packageQty;
    /**
     * 单重快照
     */
    private BigDecimal weight;
    /**
     * 本次业务总重
     */
    private BigDecimal totalWeight;
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

    /** 新口径业务核算部门，独立于库存归属部门。 */
    private Long accountingDeptId;
    /** 同一业务动作的稳定标识；反审核再审核必须由来源状态流提供新动作标识。 */
    private String postingActionKey;
    private BigDecimal financialUnitCost;
    private BigDecimal settlementUnitCost;
    /** 已确认发生总额为真值，禁止以展示单价回乘重算。 */
    private BigDecimal financialMovementAmount;
    private BigDecimal settlementMovementAmount;
    private Long costConfirmationId;
    private Integer costConfirmationRevision;
    /** 只有来源业务完成不含税成本及分摊确认才能设为 true。 */
    private Boolean costBasisConfirmed;
    /** 原业务录入价格口径；含税待确认不得自动转换成未税成本。 */
    private String sourcePriceBasis;
    private Integer sourceBizType;
    private Long sourceBizId;
    private Long sourceBizItemId;
    private Long reversalPostingId;
    /** 在已锁定来源单的审批事务内准备的不可变交易快照，仅内部流转。 */
    private cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeSnapshotService.PreparedTradeContext tradeContext;

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

    public ErpStockRecordCreateReqBO(Long productId, Long warehouseId, String batchNo, Long productUnitId,
                                     Integer packageQty, BigDecimal weight, BigDecimal totalWeight,
                                     BigDecimal count, Integer bizType, Long bizId, Long bizItemId, String bizNo,
                                     BigDecimal unitPrice, LocalDateTime bizDate) {
        this(productId, warehouseId, batchNo, count, bizType, bizId, bizItemId, bizNo, unitPrice, bizDate);
        this.productUnitId = productUnitId;
        this.packageQty = packageQty;
        this.weight = weight;
        this.totalWeight = totalWeight;
    }

}
