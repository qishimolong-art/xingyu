package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteConvertCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuotePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
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
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 报价订单 Service 实现类
 */
@Service
@Validated
public class ErpSaleQuoteServiceImpl implements ErpSaleQuoteService {

    @Resource
    private ErpSaleQuoteMapper saleQuoteMapper;
    @Resource
    private ErpSaleQuoteItemMapper saleQuoteItemMapper;
    @Resource
    private ErpSaleCartMapper saleCartMapper;
    @Resource
    private ErpSaleCartItemMapper saleCartItemMapper;
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
    private ErpSaleOutService saleOutService;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleQuote(ErpSaleQuoteSaveReqVO createReqVO) {
        List<ErpSaleQuoteItemDO> items = validateSaleQuoteItems(createReqVO.getItems());
        customerService.validateCustomer(createReqVO.getCustomerId());
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        if (createReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(createReqVO.getSaleUserId());
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_QUOTE_NO_PREFIX);
        if (saleQuoteMapper.selectByNo(no) != null) {
            throw exception(SALE_QUOTE_NO_EXISTS);
        }

        ErpSaleQuoteDO quote = BeanUtils.toBean(createReqVO, ErpSaleQuoteDO.class,
                in -> in.setNo(no).setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus()));
        calculateTotalPrice(quote, items);
        saleQuoteMapper.insert(quote);
        items.forEach(item -> item.setQuoteId(quote.getId()));
        saleQuoteItemMapper.insertBatch(items);
        return quote.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleQuote(ErpSaleQuoteSaveReqVO updateReqVO) {
        ErpSaleQuoteDO quote = validateSaleQuoteExists(updateReqVO.getId());
        if (ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(quote.getStatus())) {
            throw exception(SALE_QUOTE_UPDATE_FAIL_GENERATED, quote.getNo());
        }
        List<ErpSaleQuoteItemDO> items = validateSaleQuoteItems(updateReqVO.getItems());
        customerService.validateCustomer(updateReqVO.getCustomerId());
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        ErpSaleQuoteDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleQuoteDO.class);
        calculateTotalPrice(updateObj, items);
        saleQuoteMapper.updateById(updateObj);
        saleQuoteItemMapper.deleteByQuoteId(updateReqVO.getId());
        items.forEach(item -> item.setQuoteId(updateReqVO.getId()));
        saleQuoteItemMapper.insertBatch(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleQuote(List<Long> ids) {
        List<ErpSaleQuoteDO> quotes = saleQuoteMapper.selectByIds(ids);
        if (CollUtil.isEmpty(quotes)) {
            return;
        }
        quotes.forEach(quote -> {
            if (ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(quote.getStatus())) {
                throw exception(SALE_QUOTE_DELETE_FAIL_GENERATED, quote.getNo());
            }
            saleQuoteMapper.deleteById(quote.getId());
            saleQuoteItemMapper.deleteByQuoteId(quote.getId());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long approveSaleQuote(Long id) {
        ErpSaleQuoteDO quote = validateSaleQuoteExists(id);
        List<ErpSaleQuoteItemDO> items = saleQuoteItemMapper.selectListByQuoteId(id);
        Long saleOutId = saleOutService.createGeneratedSaleOut(buildSaleOutReqVO(quote, items),
                ErpSaleBizSourceTypeEnum.QUOTE.getType(), quote.getId(), quote.getNo());
        int updateCount = saleQuoteMapper.updateByIdAndStatus(id, ErpSaleQuoteStatusEnum.PROCESS.getStatus(),
                new ErpSaleQuoteDO().setStatus(ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus()));
        if (updateCount == 0) {
            throw exception(SALE_QUOTE_APPROVE_FAIL);
        }
        return saleOutId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToCart(ErpSaleQuoteConvertCartReqVO reqVO) {
        ErpSaleQuoteDO quote = validateSaleQuoteExists(reqVO.getQuoteId());
        List<ErpSaleQuoteItemDO> quoteItems = saleQuoteItemMapper.selectListByQuoteId(reqVO.getQuoteId());
        Map<Long, ErpSaleQuoteItemDO> quoteItemMap = convertMap(quoteItems, ErpSaleQuoteItemDO::getId);
        Map<Long, BigDecimal> convertCountMap = convertMap(reqVO.getItems(), ErpSaleQuoteConvertCartReqVO.Item::getQuoteItemId,
                ErpSaleQuoteConvertCartReqVO.Item::getCount);
        validateConvertCartCount(quoteItemMap, convertCountMap);

        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_CART_NO_PREFIX);
        if (saleCartMapper.selectByNo(no) != null) {
            throw exception(SALE_CART_NO_EXISTS);
        }
        ErpSaleCartDO cart = BeanUtils.toBean(quote, ErpSaleCartDO.class, in -> in
                .setId(null).setNo(no).setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus())
                .setCartTime(quote.getQuoteTime())
                .setSourceType(ErpSaleBizSourceTypeEnum.QUOTE.getType())
                .setSourceId(quote.getId()).setSourceNo(quote.getNo()));
        List<ErpSaleCartItemDO> cartItems = convertList(reqVO.getItems(), reqItem -> {
            ErpSaleQuoteItemDO quoteItem = quoteItemMap.get(reqItem.getQuoteItemId());
            return BeanUtils.toBean(quoteItem, ErpSaleCartItemDO.class, item -> item
                    .setId(null).setCartId(null).setCount(reqItem.getCount())
                    .setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount())));
        });
        calculateCartTotalPrice(cart, cartItems);
        saleCartMapper.insert(cart);
        cartItems.forEach(item -> item.setCartId(cart.getId()));
        saleCartItemMapper.insertBatch(cartItems);

        reqVO.getItems().forEach(reqItem -> {
            ErpSaleQuoteItemDO quoteItem = quoteItemMap.get(reqItem.getQuoteItemId());
            BigDecimal oldConvertedCount = quoteItem.getConvertedCount() != null ? quoteItem.getConvertedCount() : BigDecimal.ZERO;
            saleQuoteItemMapper.updateById(new ErpSaleQuoteItemDO().setId(quoteItem.getId())
                    .setConvertedCount(oldConvertedCount.add(reqItem.getCount())));
        });
        Integer nextStatus = isAllConverted(quoteItems, convertCountMap) ? ErpSaleQuoteStatusEnum.CONVERTED_CART.getStatus()
                : ErpSaleQuoteStatusEnum.PART_CONVERTED_CART.getStatus();
        int updateCount = saleQuoteMapper.updateByIdAndStatus(quote.getId(), quote.getStatus(),
                new ErpSaleQuoteDO().setStatus(nextStatus));
        if (updateCount == 0) {
            throw exception(SALE_QUOTE_CONVERT_CART_FAIL);
        }
        saleConvertRecordMapper.insertBatch(buildQuoteToCartRecords(quote, quoteItemMap, cart, cartItems, reqVO));
        return cart.getId();
    }

    private void validateConvertCartCount(Map<Long, ErpSaleQuoteItemDO> quoteItemMap, Map<Long, BigDecimal> convertCountMap) {
        convertCountMap.forEach((quoteItemId, count) -> {
            ErpSaleQuoteItemDO item = quoteItemMap.get(quoteItemId);
            if (item == null) {
                throw exception(SALE_QUOTE_ITEM_NOT_EXISTS, quoteItemId);
            }
            if (count == null || count.compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(SALE_QUOTE_CONVERT_COUNT_POSITIVE, quoteItemId);
            }
            BigDecimal convertedCount = item.getConvertedCount() != null ? item.getConvertedCount() : BigDecimal.ZERO;
            BigDecimal convertibleCount = item.getCount().subtract(convertedCount);
            if (count.compareTo(convertibleCount) > 0) {
                throw exception(SALE_QUOTE_CONVERT_COUNT_EXCEED, quoteItemId, count, convertibleCount);
            }
        });
    }

    private boolean isAllConverted(List<ErpSaleQuoteItemDO> quoteItems, Map<Long, BigDecimal> convertCountMap) {
        return quoteItems.stream().allMatch(item -> {
            BigDecimal convertedCount = item.getConvertedCount() != null ? item.getConvertedCount() : BigDecimal.ZERO;
            BigDecimal currentCount = convertCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            return convertedCount.add(currentCount).compareTo(item.getCount()) >= 0;
        });
    }

    private List<ErpSaleConvertRecordDO> buildQuoteToCartRecords(ErpSaleQuoteDO quote, Map<Long, ErpSaleQuoteItemDO> quoteItemMap,
                                                                 ErpSaleCartDO cart, List<ErpSaleCartItemDO> cartItems,
                                                                 ErpSaleQuoteConvertCartReqVO reqVO) {
        return convertList(reqVO.getItems(), reqItem -> {
            ErpSaleQuoteItemDO sourceItem = quoteItemMap.get(reqItem.getQuoteItemId());
            ErpSaleCartItemDO targetItem = cartItems.stream()
                    .filter(item -> sourceItem.getProductId().equals(item.getProductId())
                            && sourceItem.getWarehouseId().equals(item.getWarehouseId())
                            && reqItem.getCount().compareTo(item.getCount()) == 0)
                    .findFirst().orElse(null);
            return new ErpSaleConvertRecordDO()
                    .setConvertType(ErpSaleConvertTypeEnum.QUOTE_TO_CART.getType())
                    .setSourceType(ErpSaleBizSourceTypeEnum.QUOTE.getType())
                    .setSourceId(quote.getId()).setSourceNo(quote.getNo()).setSourceItemId(sourceItem.getId())
                    .setTargetType(ErpSaleBizSourceTypeEnum.CART.getType())
                    .setTargetId(cart.getId()).setTargetNo(cart.getNo()).setTargetItemId(targetItem != null ? targetItem.getId() : null)
                    .setProductId(sourceItem.getProductId()).setWarehouseId(sourceItem.getWarehouseId()).setCount(reqItem.getCount());
        });
    }

    private void calculateCartTotalPrice(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
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

    private ErpSaleOutSaveReqVO buildSaleOutReqVO(ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> items) {
        ErpSaleOutSaveReqVO reqVO = new ErpSaleOutSaveReqVO();
        reqVO.setCustomerId(quote.getCustomerId());
        reqVO.setAccountId(quote.getAccountId());
        reqVO.setSaleUserId(quote.getSaleUserId());
        reqVO.setOutTime(quote.getQuoteTime());
        reqVO.setDiscountPercent(quote.getDiscountPercent());
        reqVO.setOtherPrice(quote.getOtherPrice());
        reqVO.setFileUrl(quote.getFileUrl());
        reqVO.setRemark(quote.getRemark());
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

    private List<ErpSaleQuoteItemDO> validateSaleQuoteItems(List<ErpSaleQuoteSaveReqVO.Item> list) {
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpSaleQuoteSaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleQuoteItemDO.class, item -> {
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            item.setConvertedCount(BigDecimal.ZERO);
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            if (item.getTotalPrice() != null && item.getTaxPercent() != null) {
                item.setTaxPrice(MoneyUtils.priceMultiplyPercent(item.getTotalPrice(), item.getTaxPercent()));
            }
        }));
    }

    private void calculateTotalPrice(ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> items) {
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

    private ErpSaleQuoteDO validateSaleQuoteExists(Long id) {
        ErpSaleQuoteDO quote = saleQuoteMapper.selectById(id);
        if (quote == null) {
            throw exception(SALE_QUOTE_NOT_EXISTS);
        }
        return quote;
    }

    @Override
    public ErpSaleQuoteDO getSaleQuote(Long id) {
        return saleQuoteMapper.selectById(id);
    }

    @Override
    public PageResult<ErpSaleQuoteDO> getSaleQuotePage(ErpSaleQuotePageReqVO pageReqVO) {
        return saleQuoteMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpSaleQuoteItemDO> getSaleQuoteItemListByQuoteId(Long quoteId) {
        return saleQuoteItemMapper.selectListByQuoteId(quoteId);
    }

    @Override
    public List<ErpSaleQuoteItemDO> getSaleQuoteItemListByQuoteIds(Collection<Long> quoteIds) {
        if (CollUtil.isEmpty(quoteIds)) {
            return Collections.emptyList();
        }
        return saleQuoteItemMapper.selectListByQuoteIds(quoteIds);
    }

}
