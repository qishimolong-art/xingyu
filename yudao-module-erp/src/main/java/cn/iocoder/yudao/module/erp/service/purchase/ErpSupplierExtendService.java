package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierextend.ErpSupplierExtendSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierExtendDO;

import java.util.List;

public interface ErpSupplierExtendService {

    Long createSupplierExtend(ErpSupplierExtendSaveReqVO createReqVO);
    void updateSupplierExtend(ErpSupplierExtendSaveReqVO updateReqVO);
    void deleteSupplierExtend(Long id);
    ErpSupplierExtendDO getSupplierExtend(Long id);
    List<ErpSupplierExtendDO> getSupplierExtendList(Long supplierId);

}
