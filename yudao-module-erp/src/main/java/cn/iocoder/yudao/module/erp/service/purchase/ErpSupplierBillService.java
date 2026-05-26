package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierbill.ErpSupplierBillSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierBillDO;

import java.util.List;

public interface ErpSupplierBillService {

    Long createSupplierBill(ErpSupplierBillSaveReqVO createReqVO);
    void updateSupplierBill(ErpSupplierBillSaveReqVO updateReqVO);
    void deleteSupplierBill(Long id);
    ErpSupplierBillDO getSupplierBill(Long id);
    List<ErpSupplierBillDO> getSupplierBillList(Long supplierId);

}
