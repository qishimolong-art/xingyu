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
 * ERP 采购退货项 DO
 *
 * @author 芋道源码
 */
@TableName("erp_purchase_return_items")
@KeySequence("erp_purchase_return_items_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseReturnItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 采购退货编号
     *
     * 关联 {@link ErpPurchaseReturnDO##getId()}
     */
    private Long returnId;
    /**
     * 采购订单项编号
     *
     * 关联 {@link ErpPurchaseOrderItemDO#getId()}
     * 目的：方便更新关联的采购订单项的退货数量
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
     * 零件编码
     */
    private String partCode;
    /**
     * 零件名称
     */
    private String partName;
    /**
     * 适用车型
     */
    private String vehicleModel;
    /**
     * 产地
     */
    private String originPlace;
    /**
     * 包装数
     *
     * 从商品资料 {@link ErpProductDO#getPackageQty()} 带出
     */
    private Integer packageQty;
    /**
     * 整件数
     *
     * 按单退货时可只读，按库存退货时有用
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
     */
    private String brand;
    /**
     * 所属经营（单据项级）
     */
    private String businessEntity;

    // ========== 三期：按单退货关联字段 ==========
    /**
     * 原采购入库单 ID（BY_ORDER 模式下必填）
     *
     * 关联 {@link ErpPurchaseInDO#getId()}
     */
    private Long sourceInId;
    /**
     * 原采购入库项 ID（BY_ORDER 模式下必填）
     *
     * 关联 ErpPurchaseInItemDO#getId()
     */
    private Long sourceInItemId;
    /**
     * 原采购入库单号（冗余，便于前端展示）
     */
    private String sourceInNo;

}
