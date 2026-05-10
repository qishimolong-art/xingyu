package cn.iocoder.yudao.module.erp.dal.dataobject.vehicle;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 车型配件适配 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_vehicle_product_fit")
@KeySequence("erp_vehicle_product_fit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpVehicleProductFitDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 车型ID
     */
    private Long vehicleModelId;
    /**
     * 产品ID
     */
    private Long productId;
    /**
     * 安装位置
     */
    private String fitPosition;
    /**
     * 备注
     */
    private String remark;

}
