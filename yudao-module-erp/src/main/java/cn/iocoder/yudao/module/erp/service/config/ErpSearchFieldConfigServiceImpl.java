package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpSearchFieldConfigBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpSearchFieldConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpSearchFieldConfigMapper;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SEARCH_FIELD_CONFIG_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SEARCH_FIELD_CONFIG_MODULE_KEY_INVALID;

/**
 * ERP 搜索字段配置 Service 实现类
 */
@Service
@Validated
public class ErpSearchFieldConfigServiceImpl implements ErpSearchFieldConfigService {

    @Resource
    private ErpSearchFieldConfigMapper searchFieldConfigMapper;

    @Override
    public List<ErpSearchFieldConfigDO> getSearchFieldConfigListByModule(String moduleKey) {
        validateModuleKey(moduleKey);
        List<ErpSearchFieldConfigDO> list = searchFieldConfigMapper.selectListByModuleKey(moduleKey);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public List<ErpSearchFieldConfigDO> getEnabledSearchFieldConfigListByModule(String moduleKey) {
        validateModuleKey(moduleKey);
        List<ErpSearchFieldConfigDO> list = searchFieldConfigMapper.selectEnabledListByModuleKey(moduleKey);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(ErpSearchFieldConfigBatchUpdateReqVO reqVO) {
        String moduleKey = reqVO.getModuleKey();
        validateModuleKey(moduleKey);
        validateDuplicateFields(moduleKey, reqVO.getItems());

        List<ErpSearchFieldConfigDO> existList = searchFieldConfigMapper.selectListByModuleKey(moduleKey);
        if (existList != null && !existList.isEmpty()) {
            List<Long> ids = CollectionUtils.convertList(existList, ErpSearchFieldConfigDO::getId);
            searchFieldConfigMapper.physicalDeleteByIds(ids);
        }
        if (reqVO.getItems() == null || reqVO.getItems().isEmpty()) {
            return;
        }
        List<ErpSearchFieldConfigDO> insertList = CollectionUtils.convertList(reqVO.getItems(), item -> {
            ErpSearchFieldConfigDO configDO = BeanUtils.toBean(item, ErpSearchFieldConfigDO.class);
            configDO.setModuleKey(moduleKey);
            if (configDO.getEnabled() == null) {
                configDO.setEnabled(Boolean.TRUE);
            }
            return configDO;
        });
        searchFieldConfigMapper.insertBatch(insertList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetSearchFieldConfig(String moduleKey) {
        validateModuleKey(moduleKey);
        List<ErpSearchFieldConfigDO> existList = searchFieldConfigMapper.selectListByModuleKey(moduleKey);
        if (existList == null || existList.isEmpty()) {
            return;
        }
        List<Long> ids = CollectionUtils.convertList(existList, ErpSearchFieldConfigDO::getId);
        searchFieldConfigMapper.physicalDeleteByIds(ids);
    }

    @Override
    public List<ErpFieldConfigModuleEnum> listAllModules() {
        return Arrays.asList(ErpFieldConfigModuleEnum.values());
    }

    private void validateModuleKey(String moduleKey) {
        if (!ErpFieldConfigModuleEnum.isValid(moduleKey)) {
            throw exception(SEARCH_FIELD_CONFIG_MODULE_KEY_INVALID, moduleKey);
        }
    }

    private void validateDuplicateFields(String moduleKey, List<ErpSearchFieldConfigBatchUpdateReqVO.Item> items) {
        if (items == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (ErpSearchFieldConfigBatchUpdateReqVO.Item item : items) {
            if (!seen.add(item.getFieldName())) {
                throw exception(SEARCH_FIELD_CONFIG_DUPLICATE, moduleKey, item.getFieldName());
            }
        }
    }

}
