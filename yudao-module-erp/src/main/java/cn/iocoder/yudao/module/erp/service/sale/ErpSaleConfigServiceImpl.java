package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CONFIG_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CONFIG_NOT_EXISTS;

@Service
@Validated
public class ErpSaleConfigServiceImpl implements ErpSaleConfigService {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_config";

    @Resource
    private ErpSaleConfigMapper saleConfigMapper;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;

    @Override
    public Long createSaleConfig(ErpSaleConfigSaveReqVO createReqVO) {
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        validateCodeUnique(null, createReqVO.getConfigType(), createReqVO.getCode());
        ErpSaleConfigDO config = BeanUtils.toBean(createReqVO, ErpSaleConfigDO.class);
        saleConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateSaleConfig(ErpSaleConfigSaveReqVO updateReqVO) {
        ErpSaleConfigDO existing = validateSaleConfigExists(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, existing);
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getConfigType(), updateReqVO.getCode());
        saleConfigMapper.updateById(BeanUtils.toBean(updateReqVO, ErpSaleConfigDO.class));
    }

    @Override
    public void deleteSaleConfig(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(this::validateSaleConfigExists);
        saleConfigMapper.deleteByIds(ids);
    }

    @Override
    public ErpSaleConfigDO getSaleConfig(Long id) {
        return saleConfigMapper.selectById(id);
    }

    @Override
    public PageResult<ErpSaleConfigDO> getSaleConfigPage(ErpSaleConfigPageReqVO pageReqVO) {
        return saleConfigMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpSaleConfigDO> getSaleConfigSimpleList(String configType, Integer status) {
        return saleConfigMapper.selectListByTypeAndStatus(configType, status);
    }

    private ErpSaleConfigDO validateSaleConfigExists(Long id) {
        ErpSaleConfigDO config = saleConfigMapper.selectById(id);
        if (config == null) {
            throw exception(SALE_CONFIG_NOT_EXISTS);
        }
        return config;
    }

    private void validateCodeUnique(Long id, String configType, String code) {
        ErpSaleConfigDO config = saleConfigMapper.selectByTypeAndCode(configType, code);
        if (config == null) {
            return;
        }
        if (id == null || !config.getId().equals(id)) {
            throw exception(SALE_CONFIG_CODE_DUPLICATE, configType, code);
        }
    }

}
