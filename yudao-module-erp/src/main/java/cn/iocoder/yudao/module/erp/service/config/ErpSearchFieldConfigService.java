package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpSearchFieldConfigBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpSearchFieldConfigDO;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 搜索字段配置 Service 接口
 */
public interface ErpSearchFieldConfigService {

    /**
     * 获取某模块的全部搜索字段配置（按 sort 升序）。
     *
     * @param moduleKey 模块标识
     * @return 搜索字段配置列表
     */
    List<ErpSearchFieldConfigDO> getSearchFieldConfigListByModule(String moduleKey);

    /**
     * 获取某模块已启用的搜索字段配置（按 sort 升序）。
     *
     * @param moduleKey 模块标识
     * @return 已启用搜索字段配置列表
     */
    List<ErpSearchFieldConfigDO> getEnabledSearchFieldConfigListByModule(String moduleKey);

    /**
     * 批量保存一个模块的全部搜索字段配置（全量覆盖）。
     *
     * @param reqVO 批量保存请求
     */
    void batchUpdate(@Valid ErpSearchFieldConfigBatchUpdateReqVO reqVO);

    /**
     * 清空某模块的搜索字段配置。
     *
     * @param moduleKey 模块标识
     */
    void resetSearchFieldConfig(String moduleKey);

    /**
     * 获得所有可配置模块列表，供前端菜单渲染。
     *
     * @return 模块列表
     */
    List<ErpFieldConfigModuleEnum> listAllModules();

}
