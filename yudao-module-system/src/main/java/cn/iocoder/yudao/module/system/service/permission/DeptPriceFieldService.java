package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.module.system.controller.admin.permission.vo.deptpricefield.DeptPriceFieldConfigRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.deptpricefield.DeptPriceFieldUpdateReqVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DeptPriceFieldService {

    DeptPriceFieldConfigRespVO getConfig();

    void updateConfig(DeptPriceFieldUpdateReqVO reqVO);

    /**
     * 计算部门层隐藏字段；多部门授权取并集。
     */
    List<String> getHiddenPriceFields(Collection<Long> enabledDeptIds);

    /**
     * 批量计算每个部门的隐藏价格字段，避免按部门重复读取价格字段目录和授权关系。
     * 返回 Map 的 null 键代表没有可用部门时的隐藏字段。
     */
    Map<Long, List<String>> getHiddenPriceFieldsByDeptIds(Collection<Long> enabledDeptIds);

    /**
     * 价格字段被删除时物理清理关系，避免残留授权污染同名新字段。
     */
    void deleteByFieldKeys(Collection<String> fieldKeys);

}
