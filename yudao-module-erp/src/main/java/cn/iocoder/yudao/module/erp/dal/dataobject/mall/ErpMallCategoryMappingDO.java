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
 * ERP 分类同步商城分类映射 DO
 */
@TableName("erp_mall_category_mapping")
@KeySequence("erp_mall_category_mapping_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpMallCategoryMappingDO extends BaseDO {

    @TableId
    private Long id;

    private Long erpCategoryId;

    private Long mallCategoryId;

    private Integer syncStatus;

    private LocalDateTime lastSyncTime;

    private String failReason;

}
