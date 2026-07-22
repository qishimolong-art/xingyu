package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 采购入库项 DO
 *
 * @author 芋道源码
 */
@TableName("erp_purchase_in_items")
@KeySequence("erp_purchase_in_items_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseInItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 采购入库编号
     *
     * 关联 {@link ErpPurchaseInDO##getId()}
     */
    private Long inId;
    /**
     * 采购订单项编号
     *
     * 关联 {@link ErpPurchaseOrderItemDO#getId()}
     * 目的：方便更新关联的采购订单项的入库数量
     */
    private Long orderItemId;
    /**
     * 仓库编号
     *
     * 关联 {@link ErpWarehouseDO#getId()}
     */
    private Long warehouseId;
    private Long deptId;
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

    // ========== 汽配扩展字段 ==========
    /**
     * 包装数
     *
     * 默认 1，从商品资料 {@link ErpProductDO#getPackageQty()} 带出
     */
    private Integer packageQty;
    /**
     * 整件数
     *
     * 用户填写，触发 count = wholeQty × packageQty
     */
    private Integer wholeQty;
    /**
     * 仓位 / 货架位
     *
     * 注意：是仓库内的货架位（如 A-01-02），而非仓库 ID
     */
    private String warehousePosition;
    /**
     * 图号
     */
    private String drawingNo;
    /**
     * 批次
     */
    private String batchNo;
    /**
     * 条形码
     */
    private String barCode;
    /**
     * 品牌
     *
     * 从商品资料带出
     */
    private String brand;
    /**
     * 适用车型
     *
     * 从商品资料带出
     */
    private String vehicleModel;
    /**
     * 产地
     *
     * 从商品资料带出
     */
    private String originPlace;
    /**
     * 所属经营（单据项级）
     */
    private String businessEntity;

    // ========== 调价相关 ==========
    /**
     * 原价快照（首次调价时写入，后续不变）
     *
     * 方便追溯历次调价前的最初进价
     */
    private BigDecimal originalProductPrice;
    /**
     * 是否被调价过
     */
    private Boolean adjusted;
    /**
     * 最后一次调价单编号
     *
     * 关联 {@link ErpPurchasePriceAdjustDO#getId()}
     */
    private Long adjustId;

}
