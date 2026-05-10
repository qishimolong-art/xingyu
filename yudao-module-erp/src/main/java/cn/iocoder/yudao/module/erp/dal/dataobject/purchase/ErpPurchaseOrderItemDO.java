package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 采购订单项 DO
 *
 * @author 芋道源码
 */
@TableName("erp_purchase_order_items")
@KeySequence("erp_purchase_order_items_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseOrderItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 采购订单编号
     *
     * 关联 {@link ErpPurchaseOrderDO#getId()}
     */
    private Long orderId;
    /**
     * 产品编号
     *
     * 关联 {@link ErpProductDO#getId()}
     */
    private Long productId;
    /**
     * 产品单位单位
     *
     * 冗余 {@link ErpProductDO#getUnitId()}
     */
    private Long productUnitId;

    /**
     * 产品单位单价，单位：元
     */
    private BigDecimal productPrice;
    /**
     * 数量
     */
    private BigDecimal count;
    /**
     * 总价，单位：元
     *
     * totalPrice = productPrice * count
     */
    private BigDecimal totalPrice;
    /**
     * 税率，百分比
     */
    private BigDecimal taxPercent;
    /**
     * 税额，单位：元
     *
     * taxPrice = totalPrice * taxPercent
     */
    private BigDecimal taxPrice;

    /**
     * 备注
     */
    private String remark;

    // ========== 采购入库 ==========
    /**
     * 采购入库数量
     */
    private BigDecimal inCount;

    // ========== 采购退货（出库）） ==========
    /**
     * 采购退货数量
     */
    private BigDecimal returnCount;

    // ========== 汽配扩展字段 ==========

    /**
     * 仓库编号
     */
    private Long warehouseId;
    /**
     * 货架位
     */
    private String warehousePosition;
    /**
     * 适用车型（产品带出）
     */
    private String vehicleModel;
    /**
     * 产地（产品带出）
     */
    private String originPlace;
    /**
     * 规格（产品带出）
     */
    private String standard;
    /**
     * 特征码（产品带出）
     */
    private String featureCode;
    /**
     * 图号（产品带出）
     */
    private String drawingNo;
    /**
     * 批次
     */
    private String batchNo;
    /**
     * 厂家编码（产品带出）
     */
    private String factoryCode;
    /**
     * 品牌（产品带出）
     */
    private String brand;
    /**
     * 默认供应商编号
     */
    private Long supplierId;
    /**
     * 到货数量
     */
    private BigDecimal arrivalCount;

    /**
     * 是否赠品（0=否，1=是）
     * 赠品行 productPrice 强制为 0
     */
    private Boolean gift;

}