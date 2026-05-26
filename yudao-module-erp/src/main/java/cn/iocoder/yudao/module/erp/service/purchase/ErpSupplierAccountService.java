package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplieraccount.ErpSupplierAccountSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierAccountDO;

import java.util.List;

public interface ErpSupplierAccountService {

    Long createSupplierAccount(ErpSupplierAccountSaveReqVO createReqVO);
    void updateSupplierAccount(ErpSupplierAccountSaveReqVO updateReqVO);
    void deleteSupplierAccount(Long id);
    ErpSupplierAccountDO getSupplierAccount(Long id);
    List<ErpSupplierAccountDO> getSupplierAccountList(Long supplierId);

}
