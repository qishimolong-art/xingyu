package cn.iocoder.yudao.module.erp.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TestErpDeptDO extends BaseDO {

    private Long deptId;

}
