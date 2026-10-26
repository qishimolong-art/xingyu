package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportRespVO;

@FunctionalInterface
public interface ErpPurchaseOrderDetailImportProgressListener {
    void onProgress(int processedCount, int totalCount, ErpPurchaseOrderImportRespVO result);

    ErpPurchaseOrderDetailImportProgressListener NOOP = (processed, total, result) -> { };
}
