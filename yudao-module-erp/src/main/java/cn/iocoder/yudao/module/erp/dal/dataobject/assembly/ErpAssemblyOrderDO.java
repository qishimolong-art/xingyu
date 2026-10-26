package cn.iocoder.yudao.module.erp.dal.dataobject.assembly;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** ERP 套件组装单主表。 */
@TableName("erp_assembly_order")
@KeySequence("erp_assembly_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssemblyOrderDO extends BaseDO {

    @TableId
    private Long id;
    private String no;
    private Long recipeId;
    private Integer recipeVersion;
    private String recipeModuleCode;
    private String recipeModuleName;
    private Long productId;
    private Long customerId;
    private Long handlerId;
    private Integer setCount;
    /** 成品入库仓库，沿用历史字段名。 */
    private Long warehouseId;
    /** 组成配件出库仓库。 */
    private Long outWarehouseId;
    /** 跨仓组装自动生成的仓库移货单。 */
    private Long warehouseMoveId;
    private Long deptId;
    private String shelf;
    private LocalDateTime assemblyTime;
    private String finishedBatchNo;
    private BigDecimal laborCostPerSet;
    private BigDecimal otherCost;
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal totalCost;
    private BigDecimal unitCost;
    private Integer status;
    /** 当前装配执行轮次；每次开始装配递增。 */
    private Integer executionRevision;
    private Long confirmUserId;
    private LocalDateTime confirmTime;
    private Long reverseConfirmUserId;
    private LocalDateTime reverseConfirmTime;
    private Long startUserId;
    private LocalDateTime startTime;
    private Long cancelStartUserId;
    private LocalDateTime cancelStartTime;
    private Long completeUserId;
    private LocalDateTime completeTime;
    private Long reverseAssemblyUserId;
    private LocalDateTime reverseAssemblyTime;
    private Long voidUserId;
    private LocalDateTime voidTime;
    private String voidReason;
    private String remark;

}
