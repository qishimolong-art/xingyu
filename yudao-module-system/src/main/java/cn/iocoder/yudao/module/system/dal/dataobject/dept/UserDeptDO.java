package cn.iocoder.yudao.module.system.dal.dataobject.dept;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户和部门关联
 */
@TableName("system_user_dept")
@KeySequence("system_user_dept_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDeptDO extends BaseDO {

    /**
     * 自增主键
     */
    @TableId
    private Long id;
    /**
     * 用户 ID
     *
     * 关联 {@link AdminUserDO#getId()}
     */
    private Long userId;
    /**
     * 部门 ID
     *
     * 关联 {@link DeptDO#getId()}
     */
    private Long deptId;

}
