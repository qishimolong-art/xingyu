package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPickReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillItemDO;

import java.util.List;

public interface ErpStockOutBillService {

    PageResult<ErpStockOutBillDO> getStockOutBillPage(ErpStockOutBillPageReqVO pageReqVO);

    ErpStockOutBillDO getStockOutBill(Long id);

    List<ErpStockOutBillItemDO> getStockOutBillItemList(Long billId);

    List<ErpStockOutBillItemDO> getSaleOutSourceItemList(Long saleOutId);

    void createFromSaleOut(ErpSaleOutDO saleOut, List<ErpSaleOutItemDO> saleOutItems);

    void pick(ErpStockOutBillPickReqVO reqVO);

}
