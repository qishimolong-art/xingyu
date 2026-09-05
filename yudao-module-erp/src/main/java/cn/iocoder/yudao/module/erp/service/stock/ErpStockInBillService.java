package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPickupReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillItemDO;

import java.util.Collection;
import java.util.List;

public interface ErpStockInBillService {

    PageResult<ErpStockInBillDO> getStockInBillPage(ErpStockInBillPageReqVO pageReqVO);

    ErpStockInBillDO getStockInBill(Long id);

    List<ErpStockInBillDO> getStockInBillList(List<Long> ids);

    List<ErpStockInBillItemDO> getStockInBillItemList(Long billId);

    PageResult<ErpStockInBillItemDO> getStockInBillItemPage(ErpStockInBillItemPageReqVO pageReqVO);

    List<ErpStockInBillItemDO> getStockInBillItemListByBillIds(Collection<Long> billIds);

    List<ErpStockInBillDO> getStockInBillListByPurchaseInId(Long purchaseInId);

    List<ErpStockInBillItemDO> getPurchaseInSourceItemList(Long purchaseInId);

    void createFromPurchaseIn(ErpPurchaseInDO purchaseIn, List<ErpPurchaseInItemDO> purchaseInItems);

    void pickup(ErpStockInBillPickupReqVO reqVO);

}
