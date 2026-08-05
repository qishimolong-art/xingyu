package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("form_permission_field_config")
@KeySequence("form_permission_field_config_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class FormPermissionFieldConfigDO extends BaseDO {

    @TableId
    private Long id;

    private String formType;

    private String columnName;

    private String columnDesc;

    private String valueType;

    private Boolean enabled;

}
