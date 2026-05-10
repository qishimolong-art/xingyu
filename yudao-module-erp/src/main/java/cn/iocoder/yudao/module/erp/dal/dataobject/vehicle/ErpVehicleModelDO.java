package cn.iocoder.yudao.module.erp.dal.dataobject.vehicle;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 车型 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_vehicle_model")
@KeySequence("erp_vehicle_model_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpVehicleModelDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 车系ID
     */
    private Long seriesId;
    /**
     * 车型名称
     */
    private String name;
    /**
     * 起始年份
     */
    private Integer yearStart;
    /**
     * 结束年份
     */
    private Integer yearEnd;
    /**
     * 发动机型号
     */
    private String engineModel;
    /**
     * 排量
     */
    private String displacement;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态
     */
    private Integer status;

}
