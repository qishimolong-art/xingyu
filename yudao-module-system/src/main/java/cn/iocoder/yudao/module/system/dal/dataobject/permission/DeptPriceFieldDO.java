package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门可查看的配件价格字段。
 */
@TableName("system_dept_price_field")
@KeySequence("system_dept_price_field_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class DeptPriceFieldDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long deptId;

    private String fieldKey;

}
