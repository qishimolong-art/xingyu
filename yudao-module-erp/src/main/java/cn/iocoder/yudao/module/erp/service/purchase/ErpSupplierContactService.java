package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliercontact.ErpSupplierContactSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierContactDO;

import java.util.List;

public interface ErpSupplierContactService {

    Long createSupplierContact(ErpSupplierContactSaveReqVO createReqVO);
    void updateSupplierContact(ErpSupplierContactSaveReqVO updateReqVO);
    void deleteSupplierContact(Long id);
    ErpSupplierContactDO getSupplierContact(Long id);
    List<ErpSupplierContactDO> getSupplierContactList(Long supplierId);

}
