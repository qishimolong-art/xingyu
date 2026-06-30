package cn.iocoder.yudao.module.erp.dal.dataobject.stock;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 入仓单报表 DO
 */
@TableName("erp_stock_in_bill")
@KeySequence("erp_stock_in_bill_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpStockInBillDO extends BaseDO {

    @TableId
    private Long id;

    private String priority;

    private String no;

    private Boolean pickupFlag;

    private String pickup;

    private String pickupUserName;

    private LocalDateTime billDate;

    private Long warehouseId;

    private String sourceUnitName;

    private String sourceNo;

    private Integer sourceBizType;

    private Long sourceId;

    private Integer status;

    private String auditorName;

    private LocalDateTime auditTime;

    private LocalDateTime printTime;

    private Integer printCount;

    private BigDecimal totalWeight;

    private String remark;

    private Boolean timeoutFlag;

    private BigDecimal wholeQty;

    private BigDecimal looseQty;

    private BigDecimal totalCount;

    private BigDecimal pickedCount;

}
