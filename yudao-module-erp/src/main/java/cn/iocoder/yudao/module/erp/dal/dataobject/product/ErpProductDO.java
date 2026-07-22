package cn.iocoder.yudao.module.erp.dal.dataobject.product;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 产品 DO
 *
 * @author 芋道源码
 */
@TableName("erp_product")
@KeySequence("erp_product_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpProductDO extends BaseDO {

    /**
     * 产品编号
     */
    @TableId
    private Long id;
    /**
     * Department id.
     */
    private Long deptId;
    /**
     * 创建时所在部门编号，与配件后续分配的业务部门分开保存。
     */
    private Long createDeptId;
    /**
     * 产品名称
     */
    private String name;
    /**
     * 产品条码
     */
    private String barCode;
    /**
     * 产品分类编号
     *
     * 关联 {@link ErpProductCategoryDO#getId()}
     */
    private Long categoryId;
    /**
     * 是否开启批次号管理
     */
    private Boolean batchNoEnabled;
    /**
     * 单位编号
     *
     * 关联 {@link ErpProductUnitDO#getId()}
     */
    private Long unitId;
    /**
     * 产品状态
     *
     * 枚举 {@link cn.iocoder.yudao.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 停用人用户编号
     */
    private Long disabledBy;
    /**
     * 停用时间
     */
    private LocalDateTime disabledTime;
    /**
     * 产品规格
     */
    private String standard;
    /**
     * 产品备注
     */
    private String remark;
    /**
     * 保质期天数
     */
    private Integer expiryDay;
    /**
     * 基础重量（kg）
     */
    private BigDecimal weight;
    /**
     * 采购价格，单位：元
     */
    private BigDecimal purchasePrice;
    /**
     * 销售价格，单位：元
     */
    private BigDecimal salePrice;
    /**
     * 最低价格，单位：元
     */
    private BigDecimal minPrice;

    // ========== 汽配扩展字段 ==========
    /**
     * 品牌
     */
    private String brand;
    /**
     * OE编号
     */
    private String oeNumber;
    /**
     * 产地
     */
    private String originPlace;
    /**
     * 适用车型文本(冗余)
     */
    private String vehicleModelText;
    /**
     * 特征码
     */
    private String featureCode;
    /**
     * 图号
     */
    private String drawingNo;
    /**
     * 货架位置
     */
    private String shelf;

    // ========== 配件信息管理扩展字段（按《配件信息更改明细》需求） ==========
    /**
     * 配件编码（自动生成，P+6 位流水，唯一）
     */
    private String code;
    /**
     * 默认仓库编号
     *
     * 关联 {@link cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO#getId()}
     */
    private Long defaultWarehouseId;
    /**
     * 适用车型（新字段，与旧 vehicleModelText 并存，前端只用此字段）
     */
    private String vehicleModel;
    /**
     * 厂家编码
     */
    private String factoryCode;

    /**
     * 参考价
     */
    private BigDecimal referencePrice;
    /**
     * 零售价
     */
    private BigDecimal retailPrice;
    /**
     * 最后一次采购入库价格（由采购入库单回写，前端只读）
     */
    private BigDecimal lastPurchasePrice;
    /**
     * 毛利率（百分比整数，可为负）
     */
    private Integer grossProfitRate;
    /**
     * 备用价 1
     */
    private BigDecimal backupPrice1;
    /**
     * 批发价
     */
    private BigDecimal wholesalePrice;
    /**
     * 股份价
     */
    private BigDecimal sharePrice;

    /**
     * 库存上限
     */
    private Integer stockMax;
    /**
     * 库存下限
     */
    private Integer stockMin;
    /**
     * 标准库存
     */
    private Integer stockStandard;
    /**
     * 包装数（默认 1）
     */
    private Integer packageQty;

    /**
     * 主图 URL（走 infra 文件服务）
     */
    private String mainImage;
    /**
     * 配件详情页 Markdown 内容
     */
    private String detailContent;

    /**
     * 是否已合并：false 未合并，true 已合并
     */
    private Boolean mergedFlag;
    /**
     * 合并目标配件编号（mergedFlag=true 时有值）
     *
     * 关联 {@link ErpProductDO#getId()}
     */
    private Long mergedTargetId;
    /**
     * Merge operator user id.
     */
    private Long mergedBy;
    /**
     * Merge time.
     */
    private LocalDateTime mergedTime;

}
