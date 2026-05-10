package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 销售单据转换记录 DO
 */
@TableName("erp_sale_convert_record")
@KeySequence("erp_sale_convert_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSaleConvertRecordDO extends BaseDO {

    @TableId
    private Long id;
    private Integer convertType;
    private Integer sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long sourceItemId;
    private Integer targetType;
    private Long targetId;
    private String targetNo;
    private Long targetItemId;
    private Long productId;
    private Long warehouseId;
    private BigDecimal count;

}
