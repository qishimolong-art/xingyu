package cn.iocoder.yudao.module.erp.dal.dataobject.mall;

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

import java.time.LocalDateTime;

/**
 * ERP 产品同步商城商品映射 DO
 */
@TableName("erp_mall_product_mapping")
@KeySequence("erp_mall_product_mapping_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpMallProductMappingDO extends BaseDO {

    @TableId
    private Long id;

    private Long erpProductId;

    private Long mallSpuId;

    private Long mallSkuId;

    private Integer syncStatus;

    private LocalDateTime lastSyncTime;

    private String failReason;

}
