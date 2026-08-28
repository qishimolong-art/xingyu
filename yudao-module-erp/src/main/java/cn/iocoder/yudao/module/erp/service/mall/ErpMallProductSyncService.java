package cn.iocoder.yudao.module.erp.service.mall;

import cn.iocoder.yudao.module.erp.controller.admin.mall.vo.ErpMallCategorySyncStatusRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.mall.vo.ErpMallProductSyncStatusRespVO;

import java.util.List;

public interface ErpMallProductSyncService {

    int syncAllCategories();

    void syncCategory(Long erpCategoryId);

    int syncAllProducts();

    void syncProduct(Long erpProductId);

    void syncProductStock(Long erpProductId);

    List<ErpMallCategorySyncStatusRespVO> getCategorySyncStatus(Integer syncStatus);

    List<ErpMallProductSyncStatusRespVO> getProductSyncStatus(Integer syncStatus);

}
