package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierItemPageReqVO;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliercontact.ErpSupplierContactSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierContactDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierContactMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpSupplierContactServiceImpl implements ErpSupplierContactService {

    @Resource
    private ErpSupplierContactMapper supplierContactMapper;
    @Resource
    private ErpSupplierService supplierService;

    @Override
    public Long createSupplierContact(ErpSupplierContactSaveReqVO createReqVO) {
        validateSupplierExists(createReqVO.getSupplierId());
        ErpSupplierContactDO contact = BeanUtils.toBean(createReqVO, ErpSupplierContactDO.class);
        if (contact.getCompanyId() == null) {
            contact.setCompanyId(contact.getSupplierId());
        }
        supplierContactMapper.insert(contact);
        return contact.getId();
    }

    @Override
    public void updateSupplierContact(ErpSupplierContactSaveReqVO updateReqVO) {
        validateSupplierExists(updateReqVO.getSupplierId());
        ErpSupplierContactDO existing = validateSupplierContactExists(updateReqVO.getId());
        if (!java.util.Objects.equals(existing.getSupplierId(), updateReqVO.getSupplierId())) {
            throw exception(SUPPLIER_CONTACT_NOT_EXISTS);
        }
        ErpSupplierContactDO updateObj = BeanUtils.toBean(updateReqVO, ErpSupplierContactDO.class);
        if (updateObj.getCompanyId() == null) {
            updateObj.setCompanyId(updateObj.getSupplierId());
        }
        supplierContactMapper.updateById(updateObj);
    }

    @Override
    public void deleteSupplierContact(Long id) {
        validateSupplierContactExists(id);
        supplierContactMapper.deleteById(id);
    }

    private ErpSupplierContactDO validateSupplierContactExists(Long id) {
        ErpSupplierContactDO existing = supplierContactMapper.selectById(id);
        if (existing == null) throw exception(SUPPLIER_CONTACT_NOT_EXISTS);
        validateSupplierExists(existing.getSupplierId());
        return existing;
    }

    private void validateSupplierExists(Long supplierId) {
        if (supplierService.getSupplier(supplierId) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    @Override
    public ErpSupplierContactDO getSupplierContact(Long id) {
        return validateSupplierContactExists(id);
    }

    @Override
    public List<ErpSupplierContactDO> getSupplierContactList(Long supplierId) {
        validateSupplierExists(supplierId);
        return supplierContactMapper.selectListBySupplierId(supplierId);
    }


    @Override
    public PageResult<ErpSupplierContactDO> getSupplierContactPage(ErpSupplierItemPageReqVO reqVO) {
        validateSupplierExists(reqVO.getSupplierId());
        reqVO.boundPageSize();
        return supplierContactMapper.selectPage(reqVO);
    }
}
