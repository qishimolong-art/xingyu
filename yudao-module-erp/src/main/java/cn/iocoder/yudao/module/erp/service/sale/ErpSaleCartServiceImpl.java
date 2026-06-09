package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartRespVO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartConvertQuoteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartUpdateBasicReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartUpdateFileReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
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
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_SALE_CART_TYPE;

/**
 * ERP 销售手推车 Service 实现类
 */
@Service
@Validated
public class ErpSaleCartServiceImpl implements ErpSaleCartService {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_cart";

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
    private ErpProductMapper productMapper;
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
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpSaleOutService saleOutService;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleCart(ErpSaleCartSaveReqVO createReqVO) {
        clearHiddenFields(createReqVO);
        clearHiddenItemFields(createReqVO.getItems());
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
                in -> in.setNo(no).setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus())
                        .setCartTime(LocalDateTime.now()));
        calculateTotalPrice(cart, items);
        saleDocumentDefaultService.fillCreateDefaults(cart);
        saleCartMapper.insert(cart);
        items.forEach(item -> item.setCartId(cart.getId()));
        saleCartItemMapper.insertBatch(items);
        recordCreate(cart.getId(), cart.getNo());
        return cart.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleCart(ErpSaleCartSaveReqVO updateReqVO) {
        ErpSaleCartDO cart = validateSaleCartExists(updateReqVO.getId());
        if (!ErpSaleCartStatusEnum.PROCESS.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_UPDATE_FAIL_NOT_PROCESS, cart.getNo());
        }
        preserveHiddenFields(updateReqVO, cart);
        preserveHiddenItemFields(updateReqVO.getItems(), saleCartItemMapper.selectListByCartId(updateReqVO.getId()));
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
        updateObj.setCartTime(LocalDateTime.now());
        calculateTotalPrice(updateObj, items);
        saleCartMapper.updateById(updateObj);
        saleCartItemMapper.deleteByCartId(updateReqVO.getId());
        items.forEach(item -> {
            item.setId(null);
            item.setCartId(updateReqVO.getId());
        });
        saleCartItemMapper.insertBatch(items);
        recordUpdate(updateReqVO.getId(), cart.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleCartBasic(ErpSaleCartUpdateBasicReqVO updateReqVO) {
        ErpSaleCartDO cart = validateSaleCartExists(updateReqVO.getId());
        if (!ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus().equals(cart.getStatus())
                && !ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_UPDATE_BASIC_FAIL_STATUS, cart.getNo());
        }
        customerService.validateCustomer(updateReqVO.getCustomerId());
        BigDecimal feeAmount = updateReqVO.getFeeAmount() != null ? updateReqVO.getFeeAmount() : BigDecimal.ZERO;
        saleCartMapper.updateById(new ErpSaleCartDO()
                .setId(updateReqVO.getId())
                .setCustomerId(updateReqVO.getCustomerId())
                .setFeeAmount(feeAmount)
                .setOtherPrice(feeAmount)
                .setRemark(updateReqVO.getRemark()));
        recordUpdate(updateReqVO.getId(), cart.getNo());
    }

    @Override
    public void updateSaleCartFile(ErpSaleCartUpdateFileReqVO updateReqVO) {
        ErpSaleCartDO cart = validateSaleCartExists(updateReqVO.getId());
        saleCartMapper.updateById(new ErpSaleCartDO().setId(cart.getId()).setFileUrl(updateReqVO.getFileUrl()));
        recordUpdate(updateReqVO.getId(), cart.getNo());
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
    public List<Long> finalApproveSaleCart(Long id) {
        ErpSaleCartDO cart = validateSaleCartExists(id);
        List<ErpSaleCartItemDO> items = saleCartItemMapper.selectListByCartId(id);
        // 终审时实时校验库存（不依赖缓存的 stockCount）
        validateStockEnoughRealtime(items);
        Map<Long, List<ErpSaleCartItemDO>> itemsByWarehouse = items.stream()
                .collect(Collectors.groupingBy(ErpSaleCartItemDO::getWarehouseId, LinkedHashMap::new, Collectors.toList()));
        List<Long> saleOutIds = new ArrayList<>();
        for (Map.Entry<Long, List<ErpSaleCartItemDO>> entry : itemsByWarehouse.entrySet()) {
            Long saleOutId = saleOutService.createGeneratedSaleOut(buildSaleOutReqVO(cart, entry.getValue()),
                    ErpSaleBizSourceTypeEnum.CART.getType(), cart.getId(), cart.getNo());
            saleOutIds.add(saleOutId);
        }
        int updateCount = saleCartMapper.updateByIdAndStatus(id, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus(),
                new ErpSaleCartDO().setStatus(ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus())
                        .setFinalAuditTime(LocalDateTime.now())
                        .setFinalAuditUserId(SecurityFrameworkUtils.getLoginUserId()));
        if (updateCount == 0) {
            throw exception(SALE_CART_FINAL_APPROVE_FAIL);
        }
        recordStatus(id, cart.getNo(), true);
        return saleOutIds;
    }

    @Override
    public void rejectSaleCart(Long id) {
        ErpSaleCartDO cart = validateSaleCartExists(id);
        if (!ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(cart.getStatus())
                && !ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_REJECT_FAIL);
        }
        saleCartMapper.updateByIdAndStatus(id, cart.getStatus(),
                new ErpSaleCartDO().setStatus(ErpSaleCartStatusEnum.PROCESS.getStatus()));
        record(id, "驳回", "驳回销售手推车，单据编号：" + cart.getNo(), cart.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToQuote(ErpSaleCartConvertQuoteReqVO reqVO) {
        ErpSaleCartDO cart = validateSaleCartExists(reqVO.getCartId());
        if (!ErpSaleCartStatusEnum.PROCESS.getStatus().equals(cart.getStatus())) {
            throw exception(SALE_CART_CONVERT_QUOTE_FAIL);
        }

        // 1. 查询所有子表行（整单转换）
        List<ErpSaleCartItemDO> allItems = saleCartItemMapper.selectListByCartId(cart.getId());
        if (allItems.isEmpty()) {
            throw exception(SALE_CART_CONVERT_QUOTE_ITEMS_EMPTY);
        }

        // 2. 生成报价订单
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_QUOTE_NO_PREFIX);
        if (saleQuoteMapper.selectByNo(no) != null) {
            throw exception(SALE_QUOTE_NO_EXISTS);
        }
        ErpSaleQuoteDO quote = BeanUtils.toBean(cart, ErpSaleQuoteDO.class, in -> in
                .setId(null).setNo(no).setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus())
                .setQuoteTime(cart.getCartTime())
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType())
                .setSourceId(cart.getId()).setSourceNo(cart.getNo()));
        List<ErpSaleQuoteItemDO> quoteItems = convertList(allItems, cartItem ->
                BeanUtils.toBean(cartItem, ErpSaleQuoteItemDO.class, item ->
                        item.setId(null).setQuoteId(null).setConvertedCount(BigDecimal.ZERO)));
        calculateQuoteTotalPrice(quote, quoteItems);
        saleDocumentDefaultService.fillCreateDefaults(quote);
        saleQuoteMapper.insert(quote);
        quoteItems.forEach(item -> item.setQuoteId(quote.getId()));
        saleQuoteItemMapper.insertBatch(quoteItems);

        // 3. 记录转换关系（先记录，再删除原手推车）
        saleConvertRecordMapper.insertBatch(buildCartToQuoteRecords(cart, allItems, quote, quoteItems));

        // 4. 删除原手推车主表 + 子表
        saleCartItemMapper.deleteByCartId(cart.getId());
        saleCartMapper.deleteById(cart.getId());
        record(cart.getId(), "转报价",
                "销售手推车转报价订单，单据编号：" + cart.getNo() + "，目标单号：" + quote.getNo(), cart.getNo());

        return quote.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleCart(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<ErpSaleCartDO> carts = saleCartMapper.selectBatchIds(ids);
        carts.forEach(cart -> {
            if (ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus().equals(cart.getStatus())) {
                throw exception(SALE_CART_DELETE_FAIL_FINAL_APPROVED, cart.getNo());
            }
            if (ErpSaleCartStatusEnum.CONVERTED_QUOTE.getStatus().equals(cart.getStatus())) {
                throw exception(SALE_CART_DELETE_FAIL_CONVERTED, cart.getNo());
            }
        });
        saleCartMapper.deleteBatchIds(ids);
        ids.forEach(saleCartItemMapper::deleteByCartId);
        carts.forEach(cart -> recordDelete(cart.getId(), cart.getNo()));
    }

    private void updateStatus(Long id, Integer oldStatus, Integer newStatus, cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        validateSaleCartExists(id);
        boolean isFirstApprove = ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(newStatus);
        int updateCount = saleCartMapper.updateByIdAndStatus(id, oldStatus,
                new ErpSaleCartDO().setStatus(newStatus)
                        .setFirstAuditTime(isFirstApprove ? LocalDateTime.now() : null)
                        .setFirstAuditUserId(isFirstApprove ? SecurityFrameworkUtils.getLoginUserId() : null));
        if (updateCount == 0) {
            throw exception(errorCode);
        }
        ErpSaleCartDO cart = saleCartMapper.selectById(id);
        if (cart != null) {
            if (ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus().equals(newStatus)) {
                recordStatus(id, cart.getNo(), true);
            } else if (ErpSaleCartStatusEnum.SUBMITTED.getStatus().equals(newStatus)) {
                record(id, "提交", "提交销售手推车，单据编号：" + cart.getNo(), cart.getNo());
            }
        }
    }

    private ErpSaleOutSaveReqVO buildSaleOutReqVO(ErpSaleCartDO cart, List<ErpSaleCartItemDO> items) {
        ErpSaleOutSaveReqVO reqVO = new ErpSaleOutSaveReqVO();
        reqVO.setCustomerId(cart.getCustomerId());
        reqVO.setAccountId(cart.getAccountId());
        reqVO.setSaleUserId(cart.getSaleUserId());
        reqVO.setDeptId(cart.getDeptId());
        reqVO.setOutTime(cart.getCartTime());
        reqVO.setDiscountPercent(cart.getDiscountPercent());
        reqVO.setFeeAmount(cart.getFeeAmount());
        reqVO.setOtherPrice(cart.getOtherPrice());
        reqVO.setFileUrl(cart.getFileUrl());
        reqVO.setRemark(cart.getRemark());
        reqVO.setItems(convertList(items, item -> {
            ErpSaleOutSaveReqVO.Item outItem = new ErpSaleOutSaveReqVO.Item();
            outItem.setWarehouseId(item.getWarehouseId());
            outItem.setProductId(item.getProductId());
            outItem.setProductUnitId(item.getProductUnitId());
            outItem.setProductPrice(Boolean.TRUE.equals(item.getGiftFlag()) ? BigDecimal.ZERO : item.getProductPrice());
            outItem.setCount(item.getCount());
            outItem.setTaxPercent(item.getTaxPercent());
            outItem.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            outItem.setRemark(item.getRemark());
            return outItem;
        }));
        return reqVO;
    }

    private List<ErpSaleCartItemDO> validateSaleCartItems(List<ErpSaleCartSaveReqVO.Item> list) {
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpSaleCartSaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 校验数量和价格
        list.forEach(item -> {
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(SALE_CART_ITEM_COUNT_POSITIVE);
            }
            if (!Boolean.TRUE.equals(item.getGiftFlag())
                    && (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) <= 0)) {
                throw exception(SALE_CART_ITEM_PRICE_POSITIVE);
            }
        });
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleCartItemDO.class, item -> {
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            item.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            if (Boolean.TRUE.equals(item.getGiftFlag())) {
                item.setProductPrice(BigDecimal.ZERO);
            }
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

    private void validateStockEnoughRealtime(List<ErpSaleCartItemDO> items) {
        items.forEach(item -> {
            ErpStockDO stock = stockService.getStock(item.getProductId(), item.getWarehouseId());
            BigDecimal stockCount = stock != null ? stock.getCount() : BigDecimal.ZERO;
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
        BigDecimal feeAmount = resolveFeeAmount(cart.getFeeAmount(), cart.getOtherPrice());
        cart.setFeeAmount(feeAmount);
        cart.setOtherPrice(feeAmount);
        cart.setDiscountPrice(MoneyUtils.priceMultiplyPercent(cart.getTotalPrice(), cart.getDiscountPercent()));
        cart.setTotalPrice(cart.getTotalPrice().subtract(cart.getDiscountPrice()).add(feeAmount));
    }

    private void calculateQuoteTotalPrice(ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> items) {
        quote.setTotalCount(getSumValue(items, ErpSaleQuoteItemDO::getCount, BigDecimal::add));
        quote.setTotalProductPrice(getSumValue(items, ErpSaleQuoteItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        quote.setTotalTaxPrice(getSumValue(items, ErpSaleQuoteItemDO::getTaxPrice, BigDecimal::add, BigDecimal.ZERO));
        quote.setTotalPrice(quote.getTotalProductPrice().add(quote.getTotalTaxPrice()));
        if (quote.getDiscountPercent() == null) {
            quote.setDiscountPercent(BigDecimal.ZERO);
        }
        BigDecimal feeAmount = resolveFeeAmount(quote.getFeeAmount(), quote.getOtherPrice());
        quote.setFeeAmount(feeAmount);
        quote.setOtherPrice(feeAmount);
        quote.setDiscountPrice(MoneyUtils.priceMultiplyPercent(quote.getTotalPrice(), quote.getDiscountPercent()));
        quote.setTotalPrice(quote.getTotalPrice().subtract(quote.getDiscountPrice()).add(feeAmount));
    }

    private BigDecimal resolveFeeAmount(BigDecimal feeAmount, BigDecimal otherPrice) {
        return feeAmount != null ? feeAmount : (otherPrice != null ? otherPrice : BigDecimal.ZERO);
    }

    private List<ErpSaleConvertRecordDO> buildCartToQuoteRecords(ErpSaleCartDO cart, List<ErpSaleCartItemDO> cartItems,
                                                                 ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> quoteItems) {
        return convertList(cartItems, cartItem -> {
            ErpSaleQuoteItemDO targetItem = quoteItems.stream()
                    .filter(item -> cartItem.getProductId().equals(item.getProductId())
                            && cartItem.getWarehouseId().equals(item.getWarehouseId())
                            && Boolean.TRUE.equals(cartItem.getGiftFlag()) == Boolean.TRUE.equals(item.getGiftFlag())
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

    @Override
    public ErpSaleCartImportRespVO parseImportData(List<ErpSaleCartImportExcelVO> list) {
        ErpSaleCartImportRespVO respVO = new ErpSaleCartImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        LinkedHashSet<String> productCodes = new LinkedHashSet<>();
        list.forEach(row -> {
            if (row.getProductCode() != null && !row.getProductCode().isEmpty()) {
                productCodes.add(row.getProductCode());
            }
        });
        Map<String, ErpProductDO> productMap = convertMap(productMapper.selectListByCodes(productCodes), ErpProductDO::getCode);
        Map<Long, ErpProductRespVO> productVOMap = productService.getProductVOMap(convertList(productMap.values(), ErpProductDO::getId));
        Map<String, ErpWarehouseDO> warehouseMap = convertMap(
                warehouseService.getWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()), ErpWarehouseDO::getName);
        for (int i = 0; i < list.size(); i++) {
            ErpSaleCartImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (row.getProductCode() == null || row.getProductCode().isEmpty()) {
                respVO.getFailureDetails().add(new ErpSaleCartImportRespVO.FailureItem(rowNo, null, "产品编码不能为空"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpProductDO product = productMap.get(row.getProductCode());
            if (product == null) {
                respVO.getFailureDetails().add(new ErpSaleCartImportRespVO.FailureItem(rowNo, row.getProductCode(), "产品不存在"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpWarehouseDO warehouse = warehouseMap.get(row.getWarehouseName());
            if (warehouse == null) {
                respVO.getFailureDetails().add(new ErpSaleCartImportRespVO.FailureItem(rowNo, row.getProductCode(), "仓库不存在"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            if (row.getCount() == null || row.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                respVO.getFailureDetails().add(new ErpSaleCartImportRespVO.FailureItem(rowNo, row.getProductCode(), "数量必须大于 0"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                continue;
            }
            ErpSaleCartRespVO.Item item = new ErpSaleCartRespVO.Item();
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductName(product.getName());
            item.setProductUnitId(product.getUnitId());
            item.setProductUnitName(productVOMap.get(product.getId()) == null ? null : productVOMap.get(product.getId()).getUnitName());
            item.setWarehouseId(warehouse.getId());
            item.setWarehouseName(warehouse.getName());
            item.setGiftFlag(Boolean.TRUE.equals(row.getGiftFlag()));
            item.setProductPrice(Boolean.TRUE.equals(item.getGiftFlag())
                    ? BigDecimal.ZERO : (row.getProductPrice() != null ? row.getProductPrice() : product.getSalePrice()));
            item.setCount(row.getCount());
            item.setBrand(row.getBrand());
            item.setVehicleModel(row.getVehicleModel());
            item.setStandard(row.getStandard());
            item.setRemark(row.getRemark());
            respVO.getItems().add(item);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    private void clearHiddenFields(Object target) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.clearHiddenFields(FIELD_PERMISSION_MODULE, target);
        }
    }

    private void clearHiddenItemFields(List<?> items) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, items);
        }
    }

    private void preserveHiddenFields(Object target, Object existing) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, target, existing);
        }
    }

    private void preserveHiddenItemFields(List<?> items, List<?> existingItems) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, items, existingItems);
        }
    }

    private void recordCreate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordCreate(ERP_SALE_CART_TYPE, id, no);
        }
    }

    private void recordUpdate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordUpdate(ERP_SALE_CART_TYPE, id, no);
        }
    }

    private void recordDelete(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordDelete(ERP_SALE_CART_TYPE, id, no);
        }
    }

    private void recordStatus(Long id, String no, boolean approve) {
        if (operateLogService != null) {
            operateLogService.recordStatus(ERP_SALE_CART_TYPE, id, no, approve);
        }
    }

    private void record(Long id, String subType, String action, String no) {
        if (operateLogService != null) {
            operateLogService.record(ERP_SALE_CART_TYPE, subType, id, action, no);
        }
    }

}
