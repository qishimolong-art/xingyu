package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportRespVO;

/** 客户导入进度监听器；同步导入使用 {@link #NOOP} 保持原有行为。 */
public interface ErpCustomerImportProgressListener {

    ErpCustomerImportProgressListener NOOP = new ErpCustomerImportProgressListener() {
        @Override
        public void onRowProcessed(int processedCount, int totalCount, ErpCustomerImportRespVO result) {
        }

        @Override
        public void onFinalizing(int totalCount, ErpCustomerImportRespVO result) {
        }
    };

    void onRowProcessed(int processedCount, int totalCount, ErpCustomerImportRespVO result);

    void onFinalizing(int totalCount, ErpCustomerImportRespVO result);

}
