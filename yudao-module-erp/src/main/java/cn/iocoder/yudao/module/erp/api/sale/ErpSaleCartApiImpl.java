package cn.iocoder.yudao.module.erp.api.sale;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpSaleCartDraftCreateReqDTO;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpSaleCartDraftCreateRespDTO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * ERP sale cart API implementation.
 */
@Service
@Validated
public class ErpSaleCartApiImpl implements ErpSaleCartApi {

    @Resource
    private ErpSaleCartService saleCartService;

    @Override
    public ErpSaleCartDraftCreateRespDTO createSaleCartDraft(ErpSaleCartDraftCreateReqDTO createReqDTO) {
        ErpSaleCartSaveReqVO createReqVO = BeanUtils.toBean(createReqDTO, ErpSaleCartSaveReqVO.class);
        createReqVO.setItems(BeanUtils.toBean(createReqDTO.getItems(), ErpSaleCartSaveReqVO.Item.class));
        Long saleCartId = saleCartService.createSaleCartDraftFromSource(createReqVO);
        ErpSaleCartDO saleCart = saleCartService.getSaleCart(saleCartId);
        return new ErpSaleCartDraftCreateRespDTO()
                .setId(saleCart.getId())
                .setNo(saleCart.getNo())
                .setStatus(saleCart.getStatus());
    }

}
