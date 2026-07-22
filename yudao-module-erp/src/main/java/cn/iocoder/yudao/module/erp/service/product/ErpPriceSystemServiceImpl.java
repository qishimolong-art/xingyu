package cn.iocoder.yudao.module.erp.service.product;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.pricesystem.ErpPriceSystemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.pricesystem.ErpPriceSystemSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpPriceSystemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductPriceSystemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpPriceSystemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductPriceSystemMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 价格体系 Service 实现类
 *
 * @author Claude
 */
@Service
@Validated
public class ErpPriceSystemServiceImpl implements ErpPriceSystemService {

    private static final String FIELD_PERMISSION_MODULE = "erp_price_system";

    @Resource
    private ErpPriceSystemMapper priceSystemMapper;

    @Resource
    private ErpProductPriceSystemMapper productPriceSystemMapper;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public Long createPriceSystem(ErpPriceSystemSaveReqVO createReqVO) {
        // 1. 校验 code 唯一
        validatePriceSystemCodeUnique(null, createReqVO.getCode());
        // 2. 插入
        ErpPriceSystemDO priceSystem = BeanUtils.toBean(createReqVO, ErpPriceSystemDO.class);
        priceSystemMapper.insert(priceSystem);
        return priceSystem.getId();
    }

    @Override
    public void updatePriceSystem(ErpPriceSystemSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpPriceSystemDO existing = validatePriceSystem(updateReqVO.getId());
        applyPriceSystemSaveFieldPermissions(updateReqVO, existing);
        ValidationUtils.validate(updateReqVO);
        // 1.2 校验 code 唯一
        validatePriceSystemCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 2. 更新
        ErpPriceSystemDO updateObj = BeanUtils.toBean(updateReqVO, ErpPriceSystemDO.class);
        priceSystemMapper.updateById(updateObj);
    }

    private void validatePriceSystemCodeUnique(Long id, String code) {
        if (code == null) {
            return;
        }
        ErpPriceSystemDO priceSystem = priceSystemMapper.selectByCode(code);
        if (priceSystem == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的价格体系
        if (id == null || !priceSystem.getId().equals(id)) {
            throw exception(PRICE_SYSTEM_CODE_DUPLICATE, code);
        }
    }

    @Override
    public void deletePriceSystem(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // 1. 校验是否存在关联商品
        for (Long id : ids) {
            // 校验存在
            validatePriceSystem(id);
            // 校验是否已有商品关联
            Long count = productPriceSystemMapper.selectCount(
                    new LambdaQueryWrapperX<ErpProductPriceSystemDO>()
                            .eq(ErpProductPriceSystemDO::getPriceSystemId, id));
            if (count != null && count > 0) {
                throw exception(PRICE_SYSTEM_IN_USE);
            }
        }
        // 2. 批量删除
        priceSystemMapper.deleteByIds(ids);
    }

    @Override
    public ErpPriceSystemDO getPriceSystem(Long id) {
        return applyPriceSystemFieldPermissions(priceSystemMapper.selectById(id));
    }

    @Override
    public ErpPriceSystemDO validatePriceSystem(Long id) {
        ErpPriceSystemDO priceSystem = priceSystemMapper.selectById(id);
        if (priceSystem == null) {
            throw exception(PRICE_SYSTEM_NOT_EXISTS);
        }
        return priceSystem;
    }

    @Override
    public PageResult<ErpPriceSystemDO> getPriceSystemPage(ErpPriceSystemPageReqVO pageReqVO) {
        return priceSystemMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPriceSystemDO> getPriceSystemList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return priceSystemMapper.selectByIds(ids);
    }

    @Override
    public List<ErpPriceSystemDO> getPriceSystemListByStatus(Integer status) {
        return priceSystemMapper.selectList(
                new LambdaQueryWrapperX<ErpPriceSystemDO>()
                        .eqIfPresent(ErpPriceSystemDO::getStatus, status)
                        .orderByAsc(ErpPriceSystemDO::getSort));
    }

    private ErpPriceSystemDO applyPriceSystemFieldPermissions(ErpPriceSystemDO priceSystem) {
        if (priceSystem == null) {
            return null;
        }
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return priceSystem;
        }
        ErpPriceSystemDO result = BeanUtils.toBean(priceSystem, ErpPriceSystemDO.class);
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
        if (isFieldHidden(hiddenFieldSet, "code")) {
            result.setCode(null);
        }
        if (isFieldHidden(hiddenFieldSet, "name")) {
            result.setName(null);
        }
        if (isFieldHidden(hiddenFieldSet, "status")) {
            result.setStatus(null);
        }
        if (isFieldHidden(hiddenFieldSet, "sort")) {
            result.setSort(null);
        }
        if (isFieldHidden(hiddenFieldSet, "remark")) {
            result.setRemark(null);
        }
        return result;
    }

    private void applyPriceSystemSaveFieldPermissions(ErpPriceSystemSaveReqVO reqVO, ErpPriceSystemDO existing) {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
        if (isFieldHidden(hiddenFieldSet, "code")) {
            reqVO.setCode(existing.getCode());
        }
        if (isFieldHidden(hiddenFieldSet, "name")) {
            reqVO.setName(existing.getName());
        }
        if (isFieldHidden(hiddenFieldSet, "status")) {
            reqVO.setStatus(existing.getStatus());
        }
        if (isFieldHidden(hiddenFieldSet, "sort")) {
            reqVO.setSort(existing.getSort());
        }
        if (isFieldHidden(hiddenFieldSet, "remark")) {
            reqVO.setRemark(existing.getRemark());
        }
    }

    private boolean isFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey);
    }

}
