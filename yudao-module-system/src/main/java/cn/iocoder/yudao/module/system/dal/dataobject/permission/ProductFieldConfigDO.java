package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("erp_field_config")
@KeySequence("erp_field_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductFieldConfigDO extends BaseDO {

    @TableId
    private Long id;

    private String moduleKey;

    private String fieldName;

    private String fieldLabel;

    private String fieldGroup;

    private Integer sort;

}
