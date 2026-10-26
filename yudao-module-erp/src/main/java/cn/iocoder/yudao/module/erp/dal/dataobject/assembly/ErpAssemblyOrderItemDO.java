package cn.iocoder.yudao.module.erp.dal.dataobject.assembly;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/** ERP 套件组装单组成配件明细。 */
@TableName("erp_assembly_order_item")
@KeySequence("erp_assembly_order_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssemblyOrderItemDO extends BaseDO {

    @TableId
    private Long id;
    private Long orderId;
    private Long recipeItemId;
    private Long productId;
    private BigDecimal quantityPerSet;
    private BigDecimal requiredQuantity;
    private String batchNo;
    private BigDecimal consumeQuantity;
    private BigDecimal costPrice;
    private BigDecimal costAmount;
    private String productCode;
    private String productName;
    private String productStandard;
    private String productUnitName;
    private String productFactoryCode;
    private Boolean batchNoEnabled;
    private Integer sort;
    private String remark;

}
