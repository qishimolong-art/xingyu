package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInvoiceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_DELETE_FAIL_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_SUPPLIER_TYPE;

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
    @Resource
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpPurchaseInvoiceMapper purchaseInvoiceMapper;
    @Resource
    private ErpPurchasePriceAdjustMapper purchasePriceAdjustMapper;
    @Resource
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;

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
        if (supplier.getDeptId() == null) {
            supplier.setDeptId(getLoginUserDeptId());
        }
        // 自动生成编码
        supplier.setCode(generateSupplierCode());
        supplierMapper.insert(supplier);
        operateLogService.recordCreate(ERP_SUPPLIER_TYPE, supplier.getId(), supplier.getName());
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
        ErpSupplierDO supplier = validateSupplierExists(updateReqVO.getId());
        // 更新
        ErpSupplierDO updateObj = BeanUtils.toBean(updateReqVO, ErpSupplierDO.class);
        supplierMapper.updateById(updateObj);
        operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, updateReqVO.getId(),
                StringUtils.hasText(updateObj.getName()) ? updateObj.getName() : supplier.getName());
    }

    @Override
    public void deleteSupplier(Long id) {
        // 校验存在
        ErpSupplierDO supplier = validateSupplierExists(id);
        baseArchiveReferenceService.validateSupplierNotReferenced(id);
        // 删除
        supplierMapper.deleteById(id);
        operateLogService.recordDelete(ERP_SUPPLIER_TYPE, id, supplier.getName());
    }

    private void validateSupplierNotReferenced(Long supplierId) {
        if (purchaseOrderMapper.selectCountBySupplierId(supplierId) > 0) {
            throw exception(SUPPLIER_DELETE_FAIL_REFERENCED,
                    buildReferencedMessage("采购订单", purchaseOrderMapper.selectFirstNoBySupplierId(supplierId)));
        }
        if (purchaseInMapper.selectCountBySupplierId(supplierId) > 0) {
            throw exception(SUPPLIER_DELETE_FAIL_REFERENCED,
                    buildReferencedMessage("采购入库单", purchaseInMapper.selectFirstNoBySupplierId(supplierId)));
        }
        if (purchaseReturnMapper.selectCountBySupplierId(supplierId) > 0) {
            throw exception(SUPPLIER_DELETE_FAIL_REFERENCED,
                    buildReferencedMessage("采购退货单", purchaseReturnMapper.selectFirstNoBySupplierId(supplierId)));
        }
        if (purchaseInvoiceMapper.selectCountBySupplierId(supplierId) > 0) {
            throw exception(SUPPLIER_DELETE_FAIL_REFERENCED,
                    buildReferencedMessage("采购票据", purchaseInvoiceMapper.selectFirstNoBySupplierId(supplierId)));
        }
        if (purchasePriceAdjustMapper.selectCountBySupplierId(supplierId) > 0) {
            throw exception(SUPPLIER_DELETE_FAIL_REFERENCED,
                    buildReferencedMessage("采购调价单", purchasePriceAdjustMapper.selectFirstNoBySupplierId(supplierId)));
        }
    }

    private String buildReferencedMessage(String bizName, String no) {
        return StringUtils.hasText(no) ? bizName + " " + no : bizName;
    }

    private ErpSupplierDO validateSupplierExists(Long id) {
        ErpSupplierDO supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
        return supplier;
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
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return supplierMapper.selectByIds(ids);
    }

    @Override
    public PageResult<ErpSupplierDO> getSupplierPage(ErpSupplierPageReqVO pageReqVO) {
        return supplierMapper.selectPage(pageReqVO);
    }

    @Override
    public void updateSupplierStatus(Long id, Integer status) {
        // 校验存在
        ErpSupplierDO supplier = validateSupplierExists(id);
        // 更新状态
        ErpSupplierDO updateObj = new ErpSupplierDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        supplierMapper.updateById(updateObj);
        operateLogService.recordUpdate(ERP_SUPPLIER_TYPE, id, supplier.getName());
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
            purchaseDocumentDefaultService.fillCreateDefaults(supplier);
            supplierMapper.insert(supplier);
            operateLogService.recordCreate(ERP_SUPPLIER_TYPE, supplier.getId(), supplier.getName());
        }
    }

}
