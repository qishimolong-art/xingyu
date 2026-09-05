package cn.iocoder.yudao.module.erp.dal.dataobject.product;

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

/**
 * ERP 配件品牌 DO
 */
@TableName("erp_product_brand")
@KeySequence("erp_product_brand_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpProductBrandDO extends BaseDO {

    /**
     * 品牌编号
     */
    @TableId
    private Long id;

    /**
     * 品牌名称
     */
    private String name;

    /**
     * 品牌状态
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sort;

}
