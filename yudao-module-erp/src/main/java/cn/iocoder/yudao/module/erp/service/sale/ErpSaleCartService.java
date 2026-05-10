package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 销售手推车 Service 接口
 */
public interface ErpSaleCartService {

    Long createSaleCart(@Valid ErpSaleCartSaveReqVO createReqVO);

    void updateSaleCart(@Valid ErpSaleCartSaveReqVO updateReqVO);

    void submitSaleCart(Long id);

    void firstApproveSaleCart(Long id);

    Long finalApproveSaleCart(Long id);

    Long convertToQuote(Long id);

    ErpSaleCartDO getSaleCart(Long id);

    PageResult<ErpSaleCartDO> getSaleCartPage(ErpSaleCartPageReqVO pageReqVO);

    List<ErpSaleCartItemDO> getSaleCartItemListByCartId(Long cartId);

    List<ErpSaleCartItemDO> getSaleCartItemListByCartIds(Collection<Long> cartIds);

}
