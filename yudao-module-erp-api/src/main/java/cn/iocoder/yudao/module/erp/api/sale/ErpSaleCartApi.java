package cn.iocoder.yudao.module.erp.api.sale;

import cn.iocoder.yudao.module.erp.api.sale.dto.ErpSaleCartDraftCreateReqDTO;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpSaleCartDraftCreateRespDTO;

import javax.validation.Valid;

/**
 * ERP sale cart API.
 */
public interface ErpSaleCartApi {

    ErpSaleCartDraftCreateRespDTO createSaleCartDraft(@Valid ErpSaleCartDraftCreateReqDTO createReqDTO);

}
