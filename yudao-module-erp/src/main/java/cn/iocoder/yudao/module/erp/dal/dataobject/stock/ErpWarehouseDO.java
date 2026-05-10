package cn.iocoder.yudao.module.erp.dal.dataobject.stock;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 仓库 DO
 *
 * @author 芋道源码
 */
@TableName("erp_warehouse")
@KeySequence("erp_warehouse_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpWarehouseDO extends BaseDO {

    /**
     * 仓库编号
     */
    @TableId
    private Long id;
    /**
     * 仓库名称
     */
    private String name;
    /**
     * 仓库地址
     */
    private String address;
    /**
     * 排序
     */
    private Long sort;
    /**
     * 备注
     */
    private String remark;
    /**
     * 负责人
     */
    private String principal;
    /**
     * 仓储费，单位：元
     */
    private BigDecimal warehousePrice;
    /**
     * 搬运费，单位：元
     */
    private BigDecimal truckagePrice;
    /**
     * 开启状态
     *
     * 枚举 {@link cn.iocoder.yudao.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 是否默认
     */
    private Boolean defaultStatus;
    /**
     * 仓库类型
     *
     * 1-正品仓库 2-废品仓库 3-待处理仓库 4-急件仓库 5-旧件仓库 6-寄售仓库 7-托管仓库 8-半成品仓
     */
    private Integer warehouseType;

    // ========== 扩展字段 ==========

    /**
     * 仓储中心ID
     */
    private Long storageCenterId;
    /**
     * 仓储对应仓库ID
     */
    private Long storageWarehouseId;
    /**
     * 销售启用
     */
    private Boolean saleEnabled;
    /**
     * 采购启用
     */
    private Boolean purchaseEnabled;
    /**
     * 入出仓单(0不生成 1生成)
     */
    private Boolean stockBillEnabled;
    /**
     * 允许电商销售
     */
    private Boolean ecommerceEnabled;
    /**
     * 扫码管控
     */
    private Boolean scanControl;
    /**
     * 销售开单管控
     */
    private Boolean saleBillControl;
    /**
     * 销售0库存不显示
     */
    private Boolean zeroStockHide;
    /**
     * 货到分店(字典erp_warehouse_goods_branch)
     */
    private String goodsToBranch;
    /**
     * 部门(字典erp_warehouse_dept)
     */
    private String dept;
    /**
     * 仓库地点
     */
    private String warehouseLocation;
    /**
     * 出仓打包装箱
     */
    private Boolean outPacking;
    /**
     * 自动订货
     */
    private Boolean autoOrder;
    /**
     * 允许同时拣货单数
     */
    private Integer maxPickCount;
    /**
     * 仓库编码
     */
    private String warehouseCode;
    /**
     * 是否拆单
     */
    private Boolean splitOrder;
    /**
     * 出入仓分组(1-全部 2-入仓单 3-出仓单 4-全部不分组)
     */
    private Integer stockGroupType;
    /**
     * 额度管控
     */
    private BigDecimal creditControl;
    /**
     * 区域ID(关联erp_base_data type=region)
     */
    private Long regionId;

}
