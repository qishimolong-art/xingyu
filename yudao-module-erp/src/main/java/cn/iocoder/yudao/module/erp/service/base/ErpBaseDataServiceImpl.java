package cn.iocoder.yudao.module.erp.service.base;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.ErpBaseDataSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.dal.mysql.base.ErpBaseDataMapper;
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

    @Resource
    private ErpBaseDataMapper baseDataMapper;

    @Override
    public Long createBaseData(ErpBaseDataSaveReqVO createReqVO) {
        // 1. 校验同类型下名字唯一
        validateBaseDataNameUnique(null, createReqVO.getType(), createReqVO.getName());
        // 2. 插入
        ErpBaseDataDO baseData = BeanUtils.toBean(createReqVO, ErpBaseDataDO.class);
        baseDataMapper.insert(baseData);
        return baseData.getId();
    }

    @Override
    public void updateBaseData(ErpBaseDataSaveReqVO updateReqVO) {
        // 1.1 校验存在
        validateBaseDataExists(updateReqVO.getId());
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

    private void validateBaseDataExists(Long id) {
        if (baseDataMapper.selectById(id) == null) {
            throw exception(BASE_DATA_NOT_EXISTS);
        }
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
