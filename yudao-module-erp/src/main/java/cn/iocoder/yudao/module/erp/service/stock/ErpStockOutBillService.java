package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPickReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillItemDO;

import java.util.Collection;
import java.util.List;

public interface ErpStockOutBillService {

    PageResult<ErpStockOutBillDO> getStockOutBillPage(ErpStockOutBillPageReqVO pageReqVO);

    ErpStockOutBillDO getStockOutBill(Long id);

    List<ErpStockOutBillDO> getStockOutBillList(List<Long> ids);

    List<ErpStockOutBillItemDO> getStockOutBillItemList(Long billId);

    PageResult<ErpStockOutBillItemDO> getStockOutBillItemPage(ErpStockOutBillItemPageReqVO pageReqVO);

    List<ErpStockOutBillItemDO> getStockOutBillItemListByBillIds(Collection<Long> billIds);

    List<ErpStockOutBillDO> getStockOutBillListBySaleOutId(Long saleOutId);

    List<ErpStockOutBillDO> getStockOutBillListBySaleOutIds(Collection<Long> saleOutIds);

    List<ErpStockOutBillItemDO> getSaleOutSourceItemList(Long saleOutId);

    void createFromSaleOut(ErpSaleOutDO saleOut, List<ErpSaleOutItemDO> saleOutItems);

    void pick(ErpStockOutBillPickReqVO reqVO);

}
