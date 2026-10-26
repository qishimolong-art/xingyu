package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportResultRespVO;

@FunctionalInterface
public interface ErpPurchaseOrderImportProgressListener {
    void onProgress(ErpPurchaseOrderImportResultRespVO result);

    ErpPurchaseOrderImportProgressListener NOOP = result -> { };
}
