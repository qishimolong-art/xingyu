package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierbusinessinfo.ErpSupplierBusinessInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierBusinessInfoDO;

import java.util.List;

public interface ErpSupplierBusinessInfoService {

    Long createSupplierBusinessInfo(ErpSupplierBusinessInfoSaveReqVO createReqVO);
    void updateSupplierBusinessInfo(ErpSupplierBusinessInfoSaveReqVO updateReqVO);
    void deleteSupplierBusinessInfo(Long id);
    ErpSupplierBusinessInfoDO getSupplierBusinessInfo(Long id);
    List<ErpSupplierBusinessInfoDO> getSupplierBusinessInfoList(Long supplierId);

}
