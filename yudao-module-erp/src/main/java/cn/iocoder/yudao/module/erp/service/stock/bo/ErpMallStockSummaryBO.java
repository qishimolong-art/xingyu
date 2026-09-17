package cn.iocoder.yudao.module.erp.service.stock.bo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * ERP 商城商品库存汇总 BO
 */
@Data
@Accessors(chain = true)
public class ErpMallStockSummaryBO {

    /**
     * 商城 SPU 编号
     */
    private Long spuId;
    /**
     * 是否已绑定 ERP 产品
     */
    private Boolean mapped;
    /**
     * 绑定关系数量
     */
    private Integer mappingCount;
    /**
     * ERP 可用库存汇总
     */
    private BigDecimal totalAvailableCount;
    /**
     * ERP 当前库存汇总
     */
    private BigDecimal totalCount;
    /**
     * ERP 锁定库存汇总
     */
    private BigDecimal totalLockCount;
    /**
     * ERP 占用库存汇总
     */
    private BigDecimal totalOccupiedCount;
    /**
     * ERP 待入库存汇总
     */
    private BigDecimal totalPendingInCount;
    /**
     * ERP 在途库存汇总
     */
    private BigDecimal totalInTransitCount;
    /**
     * 按仓库聚合的库存明细
     */
    private List<WarehouseStock> warehouseStocks;

    @Data
    @Accessors(chain = true)
    public static class WarehouseStock {

        /**
         * 仓库编号
         */
        private Long warehouseId;
        /**
         * 仓库名称
         */
        private String warehouseName;
        /**
         * 所属部门编号
         */
        private Long deptId;
        /**
         * 所属部门名称
         */
        private String deptName;
        /**
         * 货架位
         */
        private String shelf;
        /**
         * 当前库存
         */
        private BigDecimal count;
        /**
         * 锁定库存
         */
        private BigDecimal lockCount;
        /**
         * 占用库存
         */
        private BigDecimal occupiedCount;
        /**
         * 待入库存
         */
        private BigDecimal pendingInCount;
        /**
         * 在途库存
         */
        private BigDecimal inTransitCount;
        /**
         * 可用库存
         */
        private BigDecimal availableCount;

    }

}
