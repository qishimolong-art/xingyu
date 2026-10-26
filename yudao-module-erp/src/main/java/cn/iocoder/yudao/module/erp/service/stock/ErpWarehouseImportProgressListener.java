package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportRespVO;

/** 仓库导入进度回调，调用方应保持实现轻量。 */
public interface ErpWarehouseImportProgressListener {

    ErpWarehouseImportProgressListener NOOP = new ErpWarehouseImportProgressListener() {
    };

    default void onRowsProgress(int processedCount, int totalCount, ErpWarehouseImportRespVO result) {
    }

    default void onFinalizing(int totalCount, ErpWarehouseImportRespVO result) {
    }

}
