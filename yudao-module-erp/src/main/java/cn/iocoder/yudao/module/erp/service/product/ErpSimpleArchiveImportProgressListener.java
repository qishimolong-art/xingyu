package cn.iocoder.yudao.module.erp.service.product;

public interface ErpSimpleArchiveImportProgressListener {

    ErpSimpleArchiveImportProgressListener NOOP = new ErpSimpleArchiveImportProgressListener() { };

    default void onRowsProgress(int processedCount, int totalCount, int successCount,
                                int createCount, int updateCount, int failureCount) {
    }

    default void onFinalizing(int totalCount, int successCount,
                              int createCount, int updateCount, int failureCount) {
    }

}
