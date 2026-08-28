package cn.iocoder.yudao.module.trade.service.cart;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.erp.api.sale.ErpCustomerMemberApi;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpCustomerMemberAuthRespDTO;
import cn.iocoder.yudao.module.erp.service.stock.ErpMallStockService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockOptionBO;
import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.product.api.sku.dto.ProductSkuRespDTO;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import cn.iocoder.yudao.module.trade.controller.app.cart.vo.*;
import cn.iocoder.yudao.module.trade.convert.cart.TradeCartConvert;
import cn.iocoder.yudao.module.trade.dal.dataobject.cart.CartDO;
import cn.iocoder.yudao.module.trade.dal.mysql.cart.CartMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SKU_NOT_EXISTS;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.CARD_ITEM_NOT_FOUND;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.CART_DEPT_REQUIRED;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.CART_STOCK_REQUIRED;
import static java.util.Collections.emptyList;

/**
 * 购物车 Service 实现类
 *
 * // TODO 芋艿：未来优化：购物车的价格计算，支持营销信息；目前不支持的原因，前端界面需要前端 pr 支持下；例如说：会员价格；
 *
 * @author 芋道源码
 */
@Service
@Validated
public class CartServiceImpl implements CartService {

    @Resource
    private CartMapper cartMapper;

    @Resource
    private ProductSpuApi productSpuApi;
    @Resource
    private ProductSkuApi productSkuApi;
    @Resource
    private ErpMallStockService mallStockService;
    @Resource
    private ErpCustomerMemberApi customerMemberApi;

    @Override
    public Long addCart(Long userId, AppCartAddReqVO addReqVO) {
        CartScope scope = validateCartScope(userId, addReqVO.getDeptId());
        // 查询 TradeCartDO
        if (addReqVO.getStockId() == null) {
            throw exception(CART_STOCK_REQUIRED);
        }
        CartDO cart = cartMapper.selectByScopeAndSkuIdAndStockId(userId, scope.customerId(), scope.deptId(),
                addReqVO.getSkuId(), addReqVO.getStockId());
        Integer count = addReqVO.getCount();
        Integer targetCount = cart == null ? count : cart.getCount() + count;
        // 校验 SKU 和 ERP 库存
        ProductSkuRespDTO sku = checkProductSku(addReqVO.getSkuId());
        ErpMallStockOptionBO stockOption = mallStockService.validateMallStock(
                sku.getSpuId(), sku.getId(), addReqVO.getStockId(), targetCount);

        // 情况一：存在，则进行数量更新
        if (cart != null) {
            cartMapper.updateById(new CartDO().setId(cart.getId()).setSelected(true)
                    .setCount(cart.getCount() + count));
            return cart.getId();
        // 情况二：不存在，则进行插入
        } else {
            cart = new CartDO().setUserId(userId).setCustomerId(scope.customerId()).setDeptId(scope.deptId())
                    .setSelected(true)
                    .setSpuId(sku.getSpuId()).setSkuId(sku.getId()).setCount(count)
                    .setStockId(stockOption.getStockId()).setErpProductId(stockOption.getErpProductId())
                    .setWarehouseId(stockOption.getWarehouseId());
            cartMapper.insert(cart);
        }
        return cart.getId();
    }

    @Override
    public void updateCartCount(Long userId, AppCartUpdateCountReqVO updateReqVO) {
        CartScope scope = validateCartScope(userId, updateReqVO.getDeptId());
        // 校验 TradeCartDO 存在
        CartDO cart = cartMapper.selectById(updateReqVO.getId(), userId, scope.customerId(), scope.deptId());
        if (cart == null) {
            throw exception(CARD_ITEM_NOT_FOUND);
        }
        // 校验商品 SKU
        checkProductSku(cart.getSkuId());
        mallStockService.validateMallStock(cart.getSpuId(), cart.getSkuId(), cart.getStockId(), updateReqVO.getCount());

        // 更新数量
        cartMapper.updateById(new CartDO().setId(cart.getId())
                .setCount(updateReqVO.getCount()));
    }

    @Override
    public void updateCartSelected(Long userId, AppCartUpdateSelectedReqVO updateSelectedReqVO) {
        CartScope scope = validateCartScope(userId, updateSelectedReqVO.getDeptId());
        cartMapper.updateByIds(updateSelectedReqVO.getIds(), userId, scope.customerId(), scope.deptId(),
                new CartDO().setSelected(updateSelectedReqVO.getSelected()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetCart(Long userId, AppCartResetReqVO resetReqVO) {
        CartScope scope = validateCartScope(userId, resetReqVO.getDeptId());
        // 第一步：删除原本的购物项
        CartDO oldCart = cartMapper.selectById(resetReqVO.getId(), userId, scope.customerId(), scope.deptId());
        if (oldCart == null) {
            throw exception(CARD_ITEM_NOT_FOUND);
        }
        cartMapper.deleteById(oldCart.getId());

        // 第二步：添加新的购物项
        if (resetReqVO.getStockId() == null) {
            throw exception(CART_STOCK_REQUIRED);
        }
        ProductSkuRespDTO sku = checkProductSku(resetReqVO.getSkuId());
        ErpMallStockOptionBO stockOption = mallStockService.validateMallStock(
                sku.getSpuId(), sku.getId(), resetReqVO.getStockId(), resetReqVO.getCount());
        CartDO newCart = cartMapper.selectByScopeAndSkuIdAndStockId(userId, scope.customerId(), scope.deptId(),
                resetReqVO.getSkuId(), resetReqVO.getStockId());
        if (newCart != null) {
            updateCartCount(userId, new AppCartUpdateCountReqVO()
                    .setId(newCart.getId()).setCount(resetReqVO.getCount()).setDeptId(scope.deptId()));
        } else {
            addCart(userId, new AppCartAddReqVO().setSkuId(resetReqVO.getSkuId())
                    .setCount(resetReqVO.getCount()).setStockId(stockOption.getStockId()).setDeptId(scope.deptId()));
        }
    }

    /**
     * 购物车删除商品
     *
     * @param userId 用户编号
     * @param ids 商品 SKU 编号的数组
     */
    @Override
    public void deleteCart(Long userId, Long deptId, Collection<Long> ids) {
        CartScope scope = validateCartScope(userId, deptId);
        // 查询 TradeCartDO 列表
        List<CartDO> carts = cartMapper.selectListByIds(ids, userId, scope.customerId(), scope.deptId());
        if (CollUtil.isEmpty(carts)) {
            return;
        }

        // 批量标记删除
        cartMapper.deleteByIds(convertSet(carts, CartDO::getId));
    }

    @Override
    public Integer getCartCount(Long userId, Long deptId) {
        CartScope scope = getAuthorizedCartScope(userId, deptId);
        if (scope == null) {
            return 0;
        }
        // TODO 芋艿：需要算上 selected
        return cartMapper.selectSumByScope(userId, scope.customerId(), scope.deptId());
    }

    @Override
    public AppCartListRespVO getCartList(Long userId, Long deptId) {
        CartScope scope = getAuthorizedCartScope(userId, deptId);
        if (scope == null) {
            return buildUnauthorizedCartList();
        }
        // 获得购物车的商品
        List<CartDO> carts = cartMapper.selectListByScope(userId, scope.customerId(), scope.deptId());
        carts.sort(Comparator.comparing(CartDO::getId).reversed());
        // 如果未空，则返回空结果
        if (CollUtil.isEmpty(carts)) {
            return new AppCartListRespVO()
                    .setAuthorized(true)
                    .setPriceVisible(scope.priceVisible())
                    .setOrderEnabled(scope.orderEnabled())
                    .setValidList(emptyList())
                    .setInvalidList(emptyList());
        }

        // 查询 SPU、SKU 列表
        List<ProductSpuRespDTO> spus = productSpuApi.getSpuList(convertSet(carts, CartDO::getSpuId));
        List<ProductSkuRespDTO> skus = productSkuApi.getSkuList(convertSet(carts, CartDO::getSkuId));
        Map<Long, ErpMallStockOptionBO> stockOptionMap = mallStockService.getMallStockOptionMap(
                convertSet(carts, CartDO::getStockId));

        // 如果 SPU 被删除，则删除购物车对应的商品。延迟删除
        // 为什么不是 SKU 被删除呢？因为 SKU 被删除时，还可以通过 SPU 选择其它 SKU
        deleteCartIfSpuDeleted(carts, spus);

        // 拼接数据
        return TradeCartConvert.INSTANCE.convertList(carts, spus, skus, stockOptionMap)
                .setAuthorized(true)
                .setPriceVisible(scope.priceVisible())
                .setOrderEnabled(scope.orderEnabled());
    }

    @Override
    public List<CartDO> getCartList(Long userId, Long deptId, Set<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        CartScope scope = validateCartScope(userId, deptId);
        return cartMapper.selectListByScope(userId, scope.customerId(), scope.deptId(), ids);
    }

    private CartScope getAuthorizedCartScope(Long userId, Long deptId) {
        if (deptId == null) {
            throw exception(CART_DEPT_REQUIRED);
        }
        ErpCustomerMemberAuthRespDTO auth = customerMemberApi.getCustomerMemberAuth(userId);
        if (!Boolean.TRUE.equals(auth.getAuthorized())) {
            return null;
        }
        auth = customerMemberApi.validateCustomerMemberAuth(userId, deptId);
        return new CartScope(auth.getCustomerId(), deptId,
                Boolean.TRUE.equals(auth.getPriceVisible()), Boolean.TRUE.equals(auth.getOrderEnabled()));
    }

    private CartScope validateCartScope(Long userId, Long deptId) {
        if (deptId == null) {
            throw exception(CART_DEPT_REQUIRED);
        }
        ErpCustomerMemberAuthRespDTO auth = customerMemberApi.validateCustomerMemberAuth(userId, deptId);
        return new CartScope(auth.getCustomerId(), deptId,
                Boolean.TRUE.equals(auth.getPriceVisible()), Boolean.TRUE.equals(auth.getOrderEnabled()));
    }

    private AppCartListRespVO buildUnauthorizedCartList() {
        return new AppCartListRespVO()
                .setAuthorized(false)
                .setPriceVisible(false)
                .setOrderEnabled(false)
                .setValidList(emptyList())
                .setInvalidList(emptyList());
    }

    private static class CartScope {

        private final Long customerId;
        private final Long deptId;
        private final Boolean priceVisible;
        private final Boolean orderEnabled;

        CartScope(Long customerId, Long deptId, Boolean priceVisible, Boolean orderEnabled) {
            this.customerId = customerId;
            this.deptId = deptId;
            this.priceVisible = priceVisible;
            this.orderEnabled = orderEnabled;
        }

        Long customerId() {
            return customerId;
        }

        Long deptId() {
            return deptId;
        }

        Boolean priceVisible() {
            return priceVisible;
        }

        Boolean orderEnabled() {
            return orderEnabled;
        }

    }

    private void deleteCartIfSpuDeleted(List<CartDO> carts, List<ProductSpuRespDTO> spus) {
        // 如果 SPU 被删除，则删除购物车对应的商品。延迟删除
        carts.removeIf(cart -> {
            if (spus.stream().noneMatch(spu -> spu.getId().equals(cart.getSpuId()))) {
                cartMapper.deleteById(cart.getId());
                return true;
            }
            return false;
        });
    }

    /**
     * 校验商品 SKU 是否合法
     * 1. 是否存在
     * 2. 是否下架
     *
     * @param skuId 商品 SKU 编号
     * @return 商品 SKU
     */
    private ProductSkuRespDTO checkProductSku(Long skuId) {
        ProductSkuRespDTO sku = productSkuApi.getSku(skuId);
        if (sku == null) {
            throw exception(SKU_NOT_EXISTS);
        }
        return sku;
    }

}
