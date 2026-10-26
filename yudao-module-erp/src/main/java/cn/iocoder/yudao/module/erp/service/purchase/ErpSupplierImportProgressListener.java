package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportRespVO;

/** 供应商导入进度监听器；同步导入使用 {@link #NOOP} 保持原有行为。 */
public interface ErpSupplierImportProgressListener {

    ErpSupplierImportProgressListener NOOP = new ErpSupplierImportProgressListener() {
        @Override
        public void onRowProcessed(int processedCount, int totalCount, ErpSupplierImportRespVO result) {
        }

        @Override
        public void onFinalizing(int totalCount, ErpSupplierImportRespVO result) {
        }
    };

    void onRowProcessed(int processedCount, int totalCount, ErpSupplierImportRespVO result);

    void onFinalizing(int totalCount, ErpSupplierImportRespVO result);

}
