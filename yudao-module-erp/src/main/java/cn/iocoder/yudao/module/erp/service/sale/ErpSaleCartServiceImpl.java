package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConvertRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleConvertTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 销售手推车 Service 实现类
 */
@Service
@Validated
public class ErpSaleCartServiceImpl implements ErpSaleCartService {

    @Resource
    private ErpSaleCartMapper saleCartMapper;
    @Resource
    private ErpSaleCartItemMapper saleCartItemMapper;
    @Resource
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Resource
    private ErpSaleQuoteItemMapper saleQuoteItemMapper;
    @Resource
    private ErpSaleConvertRecordMapper saleConvertRecordMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpSaleOutService saleOutService;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleCart(ErpSaleCartSaveReqVO createReqVO) {
        List<ErpSaleCartItemDO> items = validateSaleCartItems(createReqVO.getItems());
        validateStockEnough(items);
        customerService.validateCustomer(createReqVO.getCustomerId());
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        if (createReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(createReqVO.getSaleUserId());
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_CART_NO_PREFIX);
        if (saleCartMapper.selectByNo(no) != null) {
            throw exception(SALE_CART_NO_EXISTS);
        }

        ErpSaleCartDO cart = BeanUtils.toBean(createReqVO, ErpSaleCartDO.class,
                in -> in.setNo(no).setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus()));
        calculateTotalPrice(cart, items);
        saleCartMapper.insert(cart);
        items.forEach(item -> item.setCartId(cart.getId()));
        saleCartItemMapper.insertBatch(items);
        return cart.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleCart(ErpSaleCartSaveReqVO updateReqVO) {
        ErpSaleCartDO cart = validateSaleCartExists(updateReqVO.getId());
        if (ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_UPDATE_FAIL_GENERATED, cart.getNo());
        }
        List<ErpSaleCartItemDO> items = validateSaleCartItems(updateReqVO.getItems());
        validateStockEnough(items);
        customerService.validateCustomer(updateReqVO.getCustomerId());
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        ErpSaleCartDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleCartDO.class);
        calculateTotalPrice(updateObj, items);
        saleCartMapper.updateById(updateObj);
        saleCartItemMapper.deleteByCartId(updateReqVO.getId());
        items.forEach(item -> item.setCartId(updateReqVO.getId()));
        saleCartItemMapper.insertBatch(items);
    }

    @Override
    public void submitSaleCart(Long id) {
        updateStatus(id, ErpSaleCartStatusEnum.PROCESS.getStatus(), ErpSaleCartStatusEnum.SUBMITTED.getStatus(),
                SALE_CART_SUBMIT_FAIL);
    }

    @Override
    public void firstApproveSaleCart(Long id) {
        updateStatus(id, ErpSaleCartStatusEnum.SUBMITTED.getStatus(), ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus(),
                SALE_CART_FIRST_APPROVE_FAIL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long finalApproveSaleCart(Long id) {
        ErpSaleCartDO cart = validateSaleCartExists(id);
        List<ErpSaleCartItemDO> items = saleCartItemMapper.selectListByCartId(id);
        Long saleOutId = saleOutService.createGeneratedSaleOut(buildSaleOutReqVO(cart, items),
                ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId(), cart.getNo());
        int updateCount = saleCartMapper.updateByIdAndStatus(id, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus(),
                new ErpSaleCartDO().setStatus(ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus())
                        .setFinalAuditTime(LocalDateTime.now()));
        if (updateCount == 0) {
            throw exception(SALE_CART_FINAL_APPROVE_FAIL);
        }
        return saleOutId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToQuote(Long id) {
        ErpSaleCartDO cart = validateSaleCartExists(id);
        List<ErpSaleCartItemDO> cartItems = saleCartItemMapper.selectListByCartId(id);
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_QUOTE_NO_PREFIX);
        if (saleQuoteMapper.selectByNo(no) != null) {
            throw exception(SALE_QUOTE_NO_EXISTS);
        }

        ErpSaleQuoteDO quote = BeanUtils.toBean(cart, ErpSaleQuoteDO.class, in -> in
                .setId(null).setNo(no).setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus())
                .setQuoteTime(cart.getCartTime())
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType())
                .setSourceId(cart.getId()).setSourceNo(cart.getNo()));
        List<ErpSaleQuoteItemDO> quoteItems = convertList(cartItems, cartItem -> BeanUtils.toBean(cartItem,
                ErpSaleQuoteItemDO.class, item -> item.setId(null).setQuoteId(null).setConvertedCount(BigDecimal.ZERO)));
        calculateQuoteTotalPrice(quote, quoteItems);
        saleQuoteMapper.insert(quote);
        quoteItems.forEach(item -> item.setQuoteId(quote.getId()));
        saleQuoteItemMapper.insertBatch(quoteItems);

        int updateCount = saleCartMapper.updateByIdAndStatus(id, ErpSaleCartStatusEnum.PROCESS.getStatus(),
                new ErpSaleCartDO().setStatus(ErpSaleCartStatusEnum.CONVERTED_QUOTE.getStatus()));
        if (updateCount == 0) {
            throw exception(SALE_CART_CONVERT_QUOTE_FAIL);
        }
        saleConvertRecordMapper.insertBatch(buildCartToQuoteRecords(cart, cartItems, quote, quoteItems));
        return quote.getId();
    }

    private void updateStatus(Long id, Integer oldStatus, Integer newStatus, cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        validateSaleCartExists(id);
        int updateCount = saleCartMapper.updateByIdAndStatus(id, oldStatus,
                new ErpSaleCartDO().setStatus(newStatus)
                        .setFirstAuditTime(ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(newStatus) ? LocalDateTime.now() : null));
        if (updateCount == 0) {
            throw exception(errorCode);
        }
    }

    private ErpSaleOutSaveReqVO buildSaleOutReqVO(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        ErpSaleOutSaveReqVO reqVO = new ErpSaleOutSaveReqVO();
        reqVO.setCustomerId(cart.getCustomerId());
        reqVO.setAccountId(cart.getAccountId());
        reqVO.setSaleUserId(cart.getSaleUserId());
        reqVO.setOutTime(cart.getCartTime());
        reqVO.setDiscountPercent(cart.getDiscountPercent());
        reqVO.setOtherPrice(cart.getOtherPrice());
        reqVO.setFileUrl(cart.getFileUrl());
        reqVO.setRemark(cart.getRemark());
        reqVO.setItems(convertList(items, item -> {
            ErpSaleOutSaveReqVO.Item outItem = new ErpSaleOutSaveReqVO.Item();
            outItem.setWarehouseId(item.getWarehouseId());
            outItem.setProductId(item.getProductId());
            outItem.setProductUnitId(item.getProductUnitId());
            outItem.setProductPrice(item.getProductPrice());
            outItem.setCount(item.getCount());
            outItem.setTaxPercent(item.getTaxPercent());
            outItem.setRemark(item.getRemark());
            return outItem;
        }));
        return reqVO;
    }

    private List<ErpSaleCartItemDO> validateSaleCartItems(List<ErpSaleCartSaveReqVO.Item> list) {
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpSaleCartSaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleCartItemDO.class, item -> {
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            ErpStockDO stock = stockService != null ? stockService.getStock(item.getProductId(), item.getWarehouseId()) : null;
            item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
            if (item.getTotalPrice() != null && item.getTaxPercent() != null) {
                item.setTaxPrice(MoneyUtils.priceMultiplyPercent(item.getTotalPrice(), item.getTaxPercent()));
            }
        }));
    }

    private void validateStockEnough(List<ErpSaleCartItemDO> items) {
        items.forEach(item -> {
            BigDecimal stockCount = item.getStockCount() != null ? item.getStockCount() : BigDecimal.ZERO;
            if (stockCount.compareTo(item.getCount()) < 0) {
                throw exception(STOCK_COUNT_NEGATIVE2, item.getProductId(), item.getWarehouseId());
            }
        });
    }

    private void calculateTotalPrice(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        cart.setTotalCount(getSumValue(items, ErpSaleCartItemDO::getCount, BigDecimal::add));
        cart.setTotalProductPrice(getSumValue(items, ErpSaleCartItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        cart.setTotalTaxPrice(getSumValue(items, ErpSaleCartItemDO::getTaxPrice, BigDecimal::add, BigDecimal.ZERO));
        cart.setTotalPrice(cart.getTotalProductPrice().add(cart.getTotalTaxPrice()));
        if (cart.getDiscountPercent() == null) {
            cart.setDiscountPercent(BigDecimal.ZERO);
        }
        if (cart.getOtherPrice() == null) {
            cart.setOtherPrice(BigDecimal.ZERO);
        }
        cart.setDiscountPrice(MoneyUtils.priceMultiplyPercent(cart.getTotalPrice(), cart.getDiscountPercent()));
        cart.setTotalPrice(cart.getTotalPrice().subtract(cart.getDiscountPrice()).add(cart.getOtherPrice()));
    }

    private void calculateQuoteTotalPrice(ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> items) {
        quote.setTotalCount(getSumValue(items, ErpSaleQuoteItemDO::getCount, BigDecimal::add));
        quote.setTotalProductPrice(getSumValue(items, ErpSaleQuoteItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        quote.setTotalTaxPrice(getSumValue(items, ErpSaleQuoteItemDO::getTaxPrice, BigDecimal::add, BigDecimal.ZERO));
        quote.setTotalPrice(quote.getTotalProductPrice().add(quote.getTotalTaxPrice()));
        if (quote.getDiscountPercent() == null) {
            quote.setDiscountPercent(BigDecimal.ZERO);
        }
        if (quote.getOtherPrice() == null) {
            quote.setOtherPrice(BigDecimal.ZERO);
        }
        quote.setDiscountPrice(MoneyUtils.priceMultiplyPercent(quote.getTotalPrice(), quote.getDiscountPercent()));
        quote.setTotalPrice(quote.getTotalPrice().subtract(quote.getDiscountPrice()).add(quote.getOtherPrice()));
    }

    private List<ErpSaleConvertRecordDO> buildCartToQuoteRecords(ErpSaleCartDO cart, List<ErpSaleCartItemDO> cartItems,
                                                                 ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> quoteItems) {
        return convertList(cartItems, cartItem -> {
            ErpSaleQuoteItemDO targetItem = quoteItems.stream()
                    .filter(item -> cartItem.getProductId().equals(item.getProductId())
                            && cartItem.getWarehouseId().equals(item.getWarehouseId())
                            && cartItem.getCount().compareTo(item.getCount()) == 0)
                    .findFirst().orElse(null);
            return new ErpSaleConvertRecordDO()
                    .setConvertType(ErpSaleConvertTypeEnum.CART_TO_QUOTE.getType())
                    .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType())
                    .setSourceId(cart.getId()).setSourceNo(cart.getNo()).setSourceItemId(cartItem.getId())
                    .setTargetType(ErpSaleBizSourceTypeEnum.QUOTE.getType())
                    .setTargetId(quote.getId()).setTargetNo(quote.getNo()).setTargetItemId(targetItem != null ? targetItem.getId() : null)
                    .setProductId(cartItem.getProductId()).setWarehouseId(cartItem.getWarehouseId()).setCount(cartItem.getCount());
        });
    }

    private ErpSaleCartDO validateSaleCartExists(Long id) {
        ErpSaleCartDO cart = saleCartMapper.selectById(id);
        if (cart == null) {
            throw exception(SALE_CART_NOT_EXISTS);
        }
        return cart;
    }

    @Override
    public ErpSaleCartDO getSaleCart(Long id) {
        return saleCartMapper.selectById(id);
    }

    @Override
    public PageResult<ErpSaleCartDO> getSaleCartPage(ErpSaleCartPageReqVO pageReqVO) {
        return saleCartMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpSaleCartItemDO> getSaleCartItemListByCartId(Long cartId) {
        return saleCartItemMapper.selectListByCartId(cartId);
    }

    @Override
    public List<ErpSaleCartItemDO> getSaleCartItemListByCartIds(Collection<Long> cartIds) {
        if (CollUtil.isEmpty(cartIds)) {
            return Collections.emptyList();
        }
        return saleCartItemMapper.selectListByCartIds(cartIds);
    }

}
