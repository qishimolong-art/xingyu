package cn.iocoder.yudao.module.trade.convert.cart;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockOptionBO;
import cn.iocoder.yudao.module.product.api.sku.dto.ProductSkuRespDTO;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import cn.iocoder.yudao.module.product.enums.spu.ProductSpuStatusEnum;
import cn.iocoder.yudao.module.trade.controller.app.base.sku.AppProductSkuBaseRespVO;
import cn.iocoder.yudao.module.trade.controller.app.base.spu.AppProductSpuBaseRespVO;
import cn.iocoder.yudao.module.trade.controller.app.cart.vo.AppCartListRespVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.cart.CartDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

@Mapper
public interface TradeCartConvert {

    TradeCartConvert INSTANCE = Mappers.getMapper(TradeCartConvert.class);

    default AppCartListRespVO convertList(List<CartDO> carts,
                                          List<ProductSpuRespDTO> spus, List<ProductSkuRespDTO> skus,
                                          Map<Long, ErpMallStockOptionBO> stockOptionMap) {
        Map<Long, ProductSpuRespDTO> spuMap = convertMap(spus, ProductSpuRespDTO::getId);
        Map<Long, ProductSkuRespDTO> skuMap = convertMap(skus, ProductSkuRespDTO::getId);
        // 遍历，开始转换
        List<AppCartListRespVO.Cart> validList = new ArrayList<>(carts.size());
        List<AppCartListRespVO.Cart> invalidList = new ArrayList<>();
        carts.forEach(cart -> {
            AppCartListRespVO.Cart cartVO = new AppCartListRespVO.Cart();
            cartVO.setId(cart.getId()).setCustomerId(cart.getCustomerId()).setDeptId(cart.getDeptId())
                    .setCount(cart.getCount()).setSelected(cart.getSelected())
                    .setStockId(cart.getStockId()).setErpProductId(cart.getErpProductId())
                    .setWarehouseId(cart.getWarehouseId());
            ErpMallStockOptionBO stockOption = stockOptionMap.get(cart.getStockId());
            if (stockOption != null) {
                cartVO.setWarehouseName(stockOption.getWarehouseName())
                        .setStockAvailable(stockOption.getAvailable())
                        .setStockAvailableCount(stockOption.getAvailableCount())
                        .setStockStatusText(stockOption.getAvailableStatusText());
            } else {
                cartVO.setStockAvailable(false);
            }
            ProductSpuRespDTO spu = spuMap.get(cart.getSpuId());
            ProductSkuRespDTO sku = skuMap.get(cart.getSkuId());
            cartVO.setSpu(BeanUtils.toBean(spu, AppProductSpuBaseRespVO.class))
                    .setSku(BeanUtils.toBean(sku, AppProductSkuBaseRespVO.class));
            // 购物车普通购买库存以所选 ERP 仓库库存为准，不再使用商城 SPU/SKU 的旧库存字段判断。
            if (spu == null
                || !ProductSpuStatusEnum.isEnable(spu.getStatus())
                || sku == null
                || cart.getStockId() == null
                || stockOption == null
                || !Boolean.TRUE.equals(stockOption.getAvailable())
                || stockOption.getAvailableCount() == null
                || stockOption.getAvailableCount().compareTo(BigDecimal.valueOf(cart.getCount())) < 0) {
                invalidList.add(cartVO);
            } else {
                validList.add(cartVO);
            }
        });
        return new AppCartListRespVO().setValidList(validList).setInvalidList(invalidList);
    }

}
