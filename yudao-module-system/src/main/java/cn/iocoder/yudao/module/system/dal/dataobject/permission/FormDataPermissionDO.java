package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("form_data_permission")
@KeySequence("form_data_permission_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class FormDataPermissionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String formType;

    private Long formId;

    private Long userId;

    private String relation;

}
