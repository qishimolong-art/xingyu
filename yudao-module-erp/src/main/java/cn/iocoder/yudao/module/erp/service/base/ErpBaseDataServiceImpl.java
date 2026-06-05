package cn.iocoder.yudao.module.erp.service.base;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.dal.mysql.base.ErpBaseDataMapper;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 基础数据 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpBaseDataServiceImpl implements ErpBaseDataService {

    private static final String FIELD_PERMISSION_MODULE = "erp_base_data";

    @Resource
    private ErpBaseDataMapper baseDataMapper;
    @Resource
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @Override
    public Long createBaseData(ErpBaseDataSaveReqVO createReqVO) {
        // 1. 校验同类型下名字唯一
        validateBaseDataNameUnique(null, createReqVO.getType(), createReqVO.getName());
        // 2. 插入
        ErpBaseDataDO baseData = BeanUtils.toBean(createReqVO, ErpBaseDataDO.class);
        fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, baseData);
        baseDataMapper.insert(baseData);
        return baseData.getId();
    }

    @Override
    public void updateBaseData(ErpBaseDataSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpBaseDataDO db = validateBaseDataExists(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, db);
        // 1.2 校验同类型下名字唯一
        validateBaseDataNameUnique(updateReqVO.getId(), updateReqVO.getType(), updateReqVO.getName());
        // 2. 更新
        ErpBaseDataDO updateObj = BeanUtils.toBean(updateReqVO, ErpBaseDataDO.class);
        baseDataMapper.updateById(updateObj);
    }

    @Override
    public void deleteBaseData(Long id) {
        // 校验存在
        validateBaseDataExists(id);
        // 删除
        baseDataMapper.deleteById(id);
    }

    private ErpBaseDataDO validateBaseDataExists(Long id) {
        ErpBaseDataDO baseData = baseDataMapper.selectById(id);
        if (baseData == null) {
            throw exception(BASE_DATA_NOT_EXISTS);
        }
        return baseData;
    }

    private void validateBaseDataNameUnique(Long id, String type, String name) {
        ErpBaseDataDO baseData = baseDataMapper.selectByTypeAndName(type, name);
        if (baseData == null) {
            return;
        }
        if (id == null) {
            throw exception(BASE_DATA_NAME_DUPLICATE);
        }
        if (!baseData.getId().equals(id)) {
            throw exception(BASE_DATA_NAME_DUPLICATE);
        }
    }

    @Override
    public ErpBaseDataDO getBaseData(Long id) {
        return baseDataMapper.selectById(id);
    }

    @Override
    public PageResult<ErpBaseDataDO> getBaseDataPage(ErpBaseDataPageReqVO pageReqVO) {
        return baseDataMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpBaseDataDO> getBaseDataSimpleListByType(String type) {
        return baseDataMapper.selectListByTypeAndStatus(type, CommonStatusEnum.ENABLE.getStatus());
    }

}
