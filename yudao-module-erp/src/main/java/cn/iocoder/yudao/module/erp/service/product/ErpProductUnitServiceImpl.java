package cn.iocoder.yudao.module.erp.service.product;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUnitMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 产品单位 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpProductUnitServiceImpl implements ErpProductUnitService {

    private static final String FIELD_PERMISSION_MODULE = "erp_product_unit";

    @Resource
    private ErpProductUnitMapper productUnitMapper;
    @Resource
    private PermissionApi permissionApi;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private ErpProductService productService;

    @Override
    public Long createProductUnit(ErpProductUnitSaveReqVO createReqVO) {
        // 1. 校验名字唯一
        validateProductUnitNameUnique(null, createReqVO.getName());
        // 2. 插入
        ErpProductUnitDO unit = BeanUtils.toBean(createReqVO, ErpProductUnitDO.class);
        productUnitMapper.insert(unit);
        return unit.getId();
    }

    @Override
    public void updateProductUnit(ErpProductUnitSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpProductUnitDO existing = validateProductUnitExists(updateReqVO.getId());
        applyProductUnitSaveFieldPermissions(updateReqVO, existing);
        ValidationUtils.validate(updateReqVO);
        // 1.2 校验名字唯一
        validateProductUnitNameUnique(updateReqVO.getId(), updateReqVO.getName());
        // 2. 更新
        ErpProductUnitDO updateObj = BeanUtils.toBean(updateReqVO, ErpProductUnitDO.class);
        productUnitMapper.updateById(updateObj);
    }

    @VisibleForTesting
    void validateProductUnitNameUnique(Long id, String name) {
        ErpProductUnitDO unit = productUnitMapper.selectByName(name);
        if (unit == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的字典类型
        if (id == null) {
            throw exception(PRODUCT_UNIT_NAME_DUPLICATE);
        }
        if (!unit.getId().equals(id)) {
            throw exception(PRODUCT_UNIT_NAME_DUPLICATE);
        }
    }

    @Override
    public void deleteProductUnit(Long id) {
        // 1.1 校验存在
        validateProductUnitExists(id);
        // 1.2 校验产品是否使用
        if (productService.getProductCountByUnitId(id) > 0) {
            throw exception(PRODUCT_UNIT_EXITS_PRODUCT);
        }
        // 2. 删除
        productUnitMapper.deleteById(id);
    }

    private ErpProductUnitDO validateProductUnitExists(Long id) {
        ErpProductUnitDO unit = productUnitMapper.selectById(id);
        if (unit == null) {
            throw exception(PRODUCT_UNIT_NOT_EXISTS);
        }
        return unit;
    }

    @Override
    public ErpProductUnitDO getProductUnit(Long id) {
        return applyProductUnitFieldPermissions(productUnitMapper.selectById(id));
    }

    @Override
    public PageResult<ErpProductUnitDO> getProductUnitPage(ErpProductUnitPageReqVO pageReqVO) {
        return productUnitMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpProductUnitDO> getProductUnitListByStatus(Integer status) {
        return productUnitMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpProductUnitDO> getProductUnitList(Collection<Long> ids) {
         return productUnitMapper.selectByIds(ids);
    }

    private ErpProductUnitDO applyProductUnitFieldPermissions(ErpProductUnitDO unit) {
        if (unit == null) {
            return null;
        }
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return unit;
        }
        ErpProductUnitDO result = BeanUtils.toBean(unit, ErpProductUnitDO.class);
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
        if (isFieldHidden(hiddenFieldSet, "name")) {
            result.setName(null);
        }
        if (isFieldHidden(hiddenFieldSet, "status")) {
            result.setStatus(null);
        }
        return result;
    }

    private void applyProductUnitSaveFieldPermissions(ErpProductUnitSaveReqVO reqVO, ErpProductUnitDO existing) {
        List<String> hiddenFields = permissionApi.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        if (CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        Set<String> hiddenFieldSet = new HashSet<>(hiddenFields);
        if (isFieldHidden(hiddenFieldSet, "name")) {
            reqVO.setName(existing.getName());
        }
        if (isFieldHidden(hiddenFieldSet, "status")) {
            reqVO.setStatus(existing.getStatus());
        }
    }

    private boolean isFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey);
    }

}
