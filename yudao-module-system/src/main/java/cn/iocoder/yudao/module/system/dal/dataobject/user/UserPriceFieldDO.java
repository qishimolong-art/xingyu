package cn.iocoder.yudao.module.system.dal.dataobject.user;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 用户-价格字段关联 DO
 */
@TableName("system_user_price_field")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPriceFieldDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 价格字段编码
     */
    private String priceFieldCode;

    /**
     * 是否可见：true=可见，false=不可见
     */
    private Boolean visible;

}
