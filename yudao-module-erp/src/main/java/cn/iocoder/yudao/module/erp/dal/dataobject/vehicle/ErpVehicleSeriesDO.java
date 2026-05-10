package cn.iocoder.yudao.module.erp.dal.dataobject.vehicle;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 车系 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_vehicle_series")
@KeySequence("erp_vehicle_series_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpVehicleSeriesDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 品牌ID
     */
    private Long brandId;
    /**
     * 车系名称（凯美瑞/雅阁）
     */
    private String name;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态
     */
    private Integer status;

}
