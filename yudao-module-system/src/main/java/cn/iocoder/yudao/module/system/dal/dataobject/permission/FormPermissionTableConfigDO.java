package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("form_permission_table_config")
@KeySequence("form_permission_table_config_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class FormPermissionTableConfigDO extends BaseDO {

    @TableId
    private Long id;

    private String formType;

    private String tableDesc;

    private Boolean enabled;

}
