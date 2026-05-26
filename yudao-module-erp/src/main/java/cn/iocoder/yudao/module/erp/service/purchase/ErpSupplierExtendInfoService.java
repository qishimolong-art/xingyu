package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierextendinfo.ErpSupplierExtendInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierExtendInfoDO;

public interface ErpSupplierExtendInfoService {

    ErpSupplierExtendInfoDO getSupplierExtendInfo(Long supplierId);
    Long saveSupplierExtendInfo(ErpSupplierExtendInfoSaveReqVO saveReqVO);

}
