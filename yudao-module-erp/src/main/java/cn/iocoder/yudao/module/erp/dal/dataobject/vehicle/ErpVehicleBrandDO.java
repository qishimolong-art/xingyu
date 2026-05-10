package cn.iocoder.yudao.module.erp.dal.dataobject.vehicle;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 车型品牌 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_vehicle_brand")
@KeySequence("erp_vehicle_brand_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpVehicleBrandDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 品牌名称（丰田/本田/大众）
     */
    private String name;
    /**
     * Logo 图片地址
     */
    private String logo;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态
     */
    private Integer status;

}
