package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportRespVO;

/**
 * Progress callback for product imports. Implementations must keep callbacks lightweight.
 */
public interface ErpProductImportProgressListener {

    ErpProductImportProgressListener NOOP = new ErpProductImportProgressListener() {
    };

    default void onRowsProgress(int processedCount, int totalCount, ErpProductImportRespVO result) {
    }

    default void onFinalizing(int totalCount, ErpProductImportRespVO result) {
    }

}
