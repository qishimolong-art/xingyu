package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_EXISTS;

/**
 * ERP 供应商 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpSupplierServiceImpl implements ErpSupplierService {

    @Resource
    private ErpSupplierMapper supplierMapper;

    @Override
    public Long createSupplier(ErpSupplierSaveReqVO createReqVO) {
        ErpSupplierDO supplier = BeanUtils.toBean(createReqVO, ErpSupplierDO.class);
        // 兜底默认值
        if (supplier.getSort() == null) {
            supplier.setSort(0);
        }
        if (supplier.getStatus() == null) {
            supplier.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        // 自动生成编码
        supplier.setCode(generateSupplierCode());
        supplierMapper.insert(supplier);
        return supplier.getId();
    }

    private String generateSupplierCode() {
        String maxCode = supplierMapper.selectMaxCode();
        int nextNum = 1;
        if (maxCode != null && maxCode.startsWith("GYS")) {
            try {
                nextNum = Integer.parseInt(maxCode.substring(3)) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return String.format("GYS%06d", nextNum);
    }

    @Override
    public void updateSupplier(ErpSupplierSaveReqVO updateReqVO) {
        // 校验存在
        validateSupplierExists(updateReqVO.getId());
        // 更新
        ErpSupplierDO updateObj = BeanUtils.toBean(updateReqVO, ErpSupplierDO.class);
        supplierMapper.updateById(updateObj);
    }

    @Override
    public void deleteSupplier(Long id) {
        // 校验存在
        validateSupplierExists(id);
        // 删除
        supplierMapper.deleteById(id);
    }

    private void validateSupplierExists(Long id) {
        if (supplierMapper.selectById(id) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    @Override
    public ErpSupplierDO getSupplier(Long id) {
        return supplierMapper.selectById(id);
    }

    @Override
    public ErpSupplierDO validateSupplier(Long id) {
        ErpSupplierDO supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(supplier.getStatus())) {
            throw exception(SUPPLIER_NOT_ENABLE, supplier.getName());
        }
        return supplier;
    }

    @Override
    public List<ErpSupplierDO> getSupplierList(Collection<Long> ids) {
        return supplierMapper.selectByIds(ids);
    }

    @Override
    public PageResult<ErpSupplierDO> getSupplierPage(ErpSupplierPageReqVO pageReqVO) {
        return supplierMapper.selectPage(pageReqVO);
    }

    @Override
    public void updateSupplierStatus(Long id, Integer status) {
        // 校验存在
        validateSupplierExists(id);
        // 更新状态
        ErpSupplierDO updateObj = new ErpSupplierDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        supplierMapper.updateById(updateObj);
    }

    @Override
    public List<ErpSupplierDO> getSupplierListByStatus(Integer status) {
        return supplierMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpSupplierDO> getSupplierListByNameLike(String name) {
        if (name == null || name.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return supplierMapper.selectListByNameLike(name);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importSupplierList(List<ErpSupplierImportExcelVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (ErpSupplierImportExcelVO importVO : list) {
            if (importVO == null || !StringUtils.hasText(importVO.getName())) {
                continue;
            }
            ErpSupplierDO supplier = BeanUtils.toBean(importVO, ErpSupplierDO.class);
            if (!StringUtils.hasText(supplier.getCode())) {
                supplier.setCode(generateSupplierCode());
            }
            if (supplier.getStatus() == null) {
                supplier.setStatus(CommonStatusEnum.ENABLE.getStatus());
            }
            if (supplier.getSort() == null) {
                supplier.setSort(0);
            }
            supplierMapper.insert(supplier);
        }
    }

}
