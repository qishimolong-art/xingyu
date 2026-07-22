package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.module.system.controller.admin.permission.vo.deptpricefield.DeptPriceFieldConfigRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.deptpricefield.DeptPriceFieldUpdateReqVO;

import java.util.Collection;
import java.util.List;

public interface DeptPriceFieldService {

    DeptPriceFieldConfigRespVO getConfig();

    void updateConfig(DeptPriceFieldUpdateReqVO reqVO);

    /**
     * 计算部门层隐藏字段；多部门授权取并集。
     */
    List<String> getHiddenPriceFields(Collection<Long> enabledDeptIds);

    /**
     * 价格字段被删除时物理清理关系，避免残留授权污染同名新字段。
     */
    void deleteByFieldKeys(Collection<String> fieldKeys);

}
