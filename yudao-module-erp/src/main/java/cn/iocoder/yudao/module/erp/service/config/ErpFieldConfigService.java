package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpFieldConfigBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpFieldConfigCreateCustomReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 字段必填配置 Service 接口
 *
 * @author Claude
 */
public interface ErpFieldConfigService {

    /**
     * 获取某模块的字段配置列表（按 sort 升序）
     *
     * @param moduleKey 模块标识
     * @return 字段配置列表
     */
    List<ErpFieldConfigDO> getFieldConfigListByModule(String moduleKey);

    /**
     * 批量保存一个模块的全部字段配置（全量覆盖）
     *
     * @param reqVO 批量保存请求
     */
    void batchUpdate(@Valid ErpFieldConfigBatchUpdateReqVO reqVO);

    ErpFieldConfigDO createCustomField(@Valid ErpFieldConfigCreateCustomReqVO reqVO);

    /**
     * 清空某模块的自定义配置：删除当前租户下该模块的所有记录，清空后前端将按代码默认规则渲染
     *
     * @param moduleKey 模块标识
     */
    void resetFieldConfig(String moduleKey);

    /**
     * 获得所有可配置模块列表，供前端菜单渲染
     *
     * @return 模块列表
     */
    List<ErpFieldConfigModuleEnum> listAllModules();

}
