package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierimage.ErpSupplierImageSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierImageDO;

import java.util.List;

public interface ErpSupplierImageService {

    Long createSupplierImage(ErpSupplierImageSaveReqVO createReqVO);
    void updateSupplierImage(ErpSupplierImageSaveReqVO updateReqVO);
    void deleteSupplierImage(Long id);
    ErpSupplierImageDO getSupplierImage(Long id);
    List<ErpSupplierImageDO> getSupplierImageList(Long supplierId);

}
