package cn.iocoder.yudao.module.erp.dal.dataobject.stock;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 库存调拨单项 DO
 *
 * @author 芋道源码
 */
@TableName("erp_stock_move_item")
@KeySequence("erp_stock_move_item_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpStockMoveItemDO extends BaseDO {

    /**
     * 调拨项编号
     */
    @TableId
    private Long id;
    /**
     * 调拨编号
     *
     * 关联 {@link ErpStockMoveDO#getId()}
     */
    private Long moveId;
    /**
     * 调出仓库编号
     *
     * 关联 {@link ErpWarehouseDO#getId()}
     */
    private Long fromWarehouseId;
    /**
     * 调入仓库编号
     *
     * 关联 {@link ErpWarehouseDO#getId()}
     */
    private Long toWarehouseId;

    private Long fromDeptId;

    private Long toDeptId;
    /**
     * 产品编号
     *
     * 关联 {@link ErpProductDO#getId()}
     */
    private Long productId;
    /**
     * 产品单位编号
     *
     * 冗余 {@link ErpProductDO#getUnitId()}
     */
    private Long productUnitId;
    /**
     * 包装数
     */
    private Integer packageQty;
    /**
     * 单重
     */
    private BigDecimal weight;
    /**
     * 总重
     */
    private BigDecimal totalWeight;
    /**
     * 产品单价
     */
    private BigDecimal productPrice;
    /**
     * 产品数量
     */
    private BigDecimal count;
    /**
     * 合计金额，单位：元
     */
    private BigDecimal totalPrice;
    /**
     * 备注
     */
    private String remark;

    private String fromShelf;

    private String batchNo;

    /**
     * 来源采购入库单 ID。
     */
    private Long sourceInId;

    /**
     * 来源采购入库明细 ID。
     */
    private Long sourceInItemId;

    /**
     * 来源采购入库单号。
     */
    private String sourceInNo;

    /**
     * 来源采购入库数量快照。
     */
    private BigDecimal sourceCount;

    /**
     * 来源销售退货单 ID。
     */
    private Long sourceSaleReturnId;

    /**
     * 来源销售退货明细 ID。
     */
    private Long sourceSaleReturnItemId;

    /**
     * 来源销售退货单号。
     */
    private String sourceSaleReturnNo;

}
