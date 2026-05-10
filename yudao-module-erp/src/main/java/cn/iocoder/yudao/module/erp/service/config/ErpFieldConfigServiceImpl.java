package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpFieldConfigBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpFieldConfigMapper;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FIELD_CONFIG_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FIELD_CONFIG_MODULE_KEY_INVALID;

/**
 * ERP 字段必填配置 Service 实现类
 *
 * @author Claude
 */
@Service
@Validated
public class ErpFieldConfigServiceImpl implements ErpFieldConfigService {

    @Resource
    private ErpFieldConfigMapper fieldConfigMapper;

    @Override
    public List<ErpFieldConfigDO> getFieldConfigListByModule(String moduleKey) {
        validateModuleKey(moduleKey);
        List<ErpFieldConfigDO> list = fieldConfigMapper.selectListByModuleKey(moduleKey);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(ErpFieldConfigBatchUpdateReqVO reqVO) {
        // 1. 校验 moduleKey 合法
        String moduleKey = reqVO.getModuleKey();
        validateModuleKey(moduleKey);
        // 2. 批内 fieldName 去重防御（DB 层也有 uk_tenant_module_field 唯一索引兜底）
        if (reqVO.getItems() != null) {
            Set<String> seen = new HashSet<>();
            for (ErpFieldConfigBatchUpdateReqVO.Item item : reqVO.getItems()) {
                if (!seen.add(item.getFieldName())) {
                    throw exception(FIELD_CONFIG_DUPLICATE, moduleKey, item.getFieldName());
                }
            }
        }
        // 3. 查询当前模块下所有记录，逻辑删除
        List<ErpFieldConfigDO> existList = fieldConfigMapper.selectListByModuleKey(moduleKey);
        if (existList != null && !existList.isEmpty()) {
            List<Long> ids = CollectionUtils.convertList(existList, ErpFieldConfigDO::getId);
            fieldConfigMapper.deleteByIds(ids);
        }
        // 4. 把 items 转成 DO 批量插入
        if (reqVO.getItems() == null || reqVO.getItems().isEmpty()) {
            return;
        }
        List<ErpFieldConfigDO> insertList = CollectionUtils.convertList(reqVO.getItems(), item -> {
            ErpFieldConfigDO configDO = BeanUtils.toBean(item, ErpFieldConfigDO.class);
            configDO.setModuleKey(moduleKey);
            // required 兜底，避免 null 写入
            if (configDO.getRequired() == null) {
                configDO.setRequired(Boolean.FALSE);
            }
            return configDO;
        });
        fieldConfigMapper.insertBatch(insertList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetFieldConfig(String moduleKey) {
        validateModuleKey(moduleKey);
        List<ErpFieldConfigDO> existList = fieldConfigMapper.selectListByModuleKey(moduleKey);
        if (existList == null || existList.isEmpty()) {
            return;
        }
        List<Long> ids = CollectionUtils.convertList(existList, ErpFieldConfigDO::getId);
        fieldConfigMapper.deleteByIds(ids);
    }

    @Override
    public List<ErpFieldConfigModuleEnum> listAllModules() {
        return Arrays.asList(ErpFieldConfigModuleEnum.values());
    }

    private void validateModuleKey(String moduleKey) {
        if (!ErpFieldConfigModuleEnum.isValid(moduleKey)) {
            throw exception(FIELD_CONFIG_MODULE_KEY_INVALID, moduleKey);
        }
    }

}
