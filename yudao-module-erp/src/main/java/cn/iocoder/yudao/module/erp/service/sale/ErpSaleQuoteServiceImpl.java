package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.imports.ErpSaleImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteConvertCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuotePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConvertRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleConvertTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleQuoteStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpImportProductResolver;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductBatchNoValidator;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_SALE_QUOTE_TYPE;

/**
 * ERP 报价订单 Service 实现类
 */
@Service
@Validated
public class ErpSaleQuoteServiceImpl implements ErpSaleQuoteService {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_quote";

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
    private ErpSaleOutMapper saleOutMapper;
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
    private ErpSaleCartService saleCartService;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleItemBatchUpdateSupport batchUpdateSupport;
    @Resource
    private ErpSalePriceLevelPricePicker priceLevelPricePicker;
    @Resource
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpProductBatchNoValidator productBatchNoValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleQuote(ErpSaleQuoteSaveReqVO createReqVO) {
        clearHiddenFields(createReqVO);
        clearHiddenItemFields(createReqVO, createReqVO.getItems());
        Long reqDeptId = createReqVO.getDeptId();
        List<ErpSaleQuoteItemDO> items = validateSaleQuoteItems(createReqVO.getItems(), reqDeptId);
        Long quoteDeptId = prepareSaleQuoteDept(createReqVO);
        if (reqDeptId == null && quoteDeptId != null) {
            validateSaleQuoteItemWarehousesAllowed(items, quoteDeptId);
        }
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
                in -> in.setNo(no)
                        .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus())
                        .setQuoteTime(LocalDateTime.now()));
        calculateTotalPrice(quote, items);
        saleDocumentDefaultService.fillCreateDefaults(quote);
        saleQuoteMapper.insert(quote);
        items.forEach(item -> item.setQuoteId(quote.getId()));
        saleQuoteItemMapper.insertBatch(items);
        recordCreate(quote.getId(), quote.getNo());
        return quote.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleQuoteDraft(ErpSaleQuoteDraftCreateReqVO createReqVO) {
        clearHiddenFields(createReqVO);
        clearHiddenItemFields(createReqVO, createReqVO.getItems());
        List<ErpSaleQuoteSaveReqVO.Item> itemReqs = filterDraftItems(createReqVO.getItems());
        if (CollUtil.isEmpty(itemReqs)) {
            throw exception(SALE_QUOTE_DRAFT_ITEMS_REQUIRED);
        }
        Long quoteDeptId = createReqVO.getDeptId();
        if (createReqVO.getCustomerId() != null) {
            quoteDeptId = prepareSaleQuoteDept(createReqVO);
        }
        List<ErpSaleQuoteItemDO> items = validateSaleQuoteDraftItems(itemReqs, quoteDeptId);
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
                in -> in.setNo(no)
                        .setStatus(ErpSaleQuoteStatusEnum.DRAFT.getStatus())
                        .setQuoteTime(LocalDateTime.now()));
        calculateTotalPrice(quote, items);
        saleDocumentDefaultService.fillCreateDefaults(quote);
        saleQuoteMapper.insert(quote);
        if (CollUtil.isNotEmpty(items)) {
            items.forEach(item -> item.setQuoteId(quote.getId()));
            saleQuoteItemMapper.insertBatch(items);
        }
        recordCreate(quote.getId(), quote.getNo());
        return quote.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleQuote(ErpSaleQuoteSaveReqVO updateReqVO) {
        ErpSaleQuoteDO quote = validateSaleQuoteExists(updateReqVO.getId());
        if (ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(quote.getStatus())) {
            throw exception(SALE_QUOTE_UPDATE_FAIL_GENERATED, quote.getNo());
        }
        if (!ErpSaleQuoteStatusEnum.PROCESS.getStatus().equals(quote.getStatus())
                && !ErpSaleQuoteStatusEnum.CANCEL.getStatus().equals(quote.getStatus())) {
            throw exception(SALE_QUOTE_UPDATE_FAIL_NOT_DRAFT, quote.getNo());
        }
        preserveHiddenFields(updateReqVO, quote);
        preserveHiddenItemFields(updateReqVO, updateReqVO.getItems(),
                saleQuoteItemMapper.selectListByQuoteId(updateReqVO.getId()));
        Long reqDeptId = updateReqVO.getDeptId();
        List<ErpSaleQuoteItemDO> items = validateSaleQuoteItems(updateReqVO.getItems(), reqDeptId);
        Long quoteDeptId = prepareSaleQuoteDept(updateReqVO);
        if (reqDeptId == null && quoteDeptId != null) {
            validateSaleQuoteItemWarehousesAllowed(items, quoteDeptId);
        }
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        ErpSaleQuoteDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleQuoteDO.class,
                in -> in.setQuoteTime(LocalDateTime.now()));
        calculateTotalPrice(updateObj, items);
        saleQuoteMapper.updateById(updateObj);
        saleQuoteItemMapper.deleteByQuoteId(updateReqVO.getId());
        items.forEach(item -> item.setQuoteId(updateReqVO.getId()));
        saleQuoteItemMapper.insertBatch(items);
        recordUpdate(updateReqVO.getId(), quote.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleQuoteDraft(ErpSaleQuoteDraftUpdateReqVO updateReqVO) {
        ErpSaleQuoteDO quote = validateSaleQuoteExists(updateReqVO.getId());
        if (!ErpSaleQuoteStatusEnum.DRAFT.getStatus().equals(quote.getStatus())) {
            throw exception(SALE_QUOTE_UPDATE_FAIL_NOT_DRAFT, quote.getNo());
        }
        preserveHiddenFields(updateReqVO, quote);
        List<ErpSaleQuoteItemDO> existingItems = saleQuoteItemMapper.selectListByQuoteId(updateReqVO.getId());
        preserveHiddenItemFields(updateReqVO, updateReqVO.getItems(), existingItems);
        List<ErpSaleQuoteSaveReqVO.Item> itemReqs = filterDraftItems(updateReqVO.getItems());
        Long quoteDeptId = updateReqVO.getDeptId() != null ? updateReqVO.getDeptId() : quote.getDeptId();
        if (updateReqVO.getCustomerId() != null) {
            quoteDeptId = prepareSaleQuoteDept(updateReqVO);
        }
        List<ErpSaleQuoteItemDO> items = validateSaleQuoteDraftItems(itemReqs, quoteDeptId);
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        ErpSaleQuoteDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleQuoteDO.class);
        updateObj.setNo(quote.getNo());
        updateObj.setStatus(quote.getStatus());
        updateObj.setDeptId(quoteDeptId);
        updateObj.setQuoteTime(LocalDateTime.now());
        calculateTotalPrice(updateObj, items);
        saleQuoteMapper.updateById(updateObj);
        saleQuoteItemMapper.deleteByQuoteId(updateReqVO.getId());
        if (CollUtil.isNotEmpty(items)) {
            items.forEach(item -> item.setQuoteId(updateReqVO.getId()));
            saleQuoteItemMapper.insertBatch(items);
        }
        recordUpdate(updateReqVO.getId(), quote.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateSaleQuoteItems(ErpSaleQuoteItemBatchUpdateReqVO updateReqVO) {
        boolean updateWarehouse = updateReqVO.getWarehouseId() != null;
        boolean updateDept = updateReqVO.getDeptId() != null;
        boolean updatePrice = updateReqVO.getPriceLevel() != null;
        if (!updateWarehouse && !updateDept && !updatePrice) {
            throw exception(SALE_QUOTE_ITEM_BATCH_UPDATE_EMPTY);
        }
        batchUpdateSupport.validateFieldPermission(FIELD_PERMISSION_MODULE,
                updateWarehouse, updateDept, updatePrice,
                SALE_QUOTE_ITEM_BATCH_UPDATE_FIELD_DENIED);
        ErpSaleQuoteDO quote = validateSaleQuoteExists(updateReqVO.getQuoteId());
        if (!ErpSaleQuoteStatusEnum.DRAFT.getStatus().equals(quote.getStatus())
                && !ErpSaleQuoteStatusEnum.PROCESS.getStatus().equals(quote.getStatus())
                && !ErpSaleQuoteStatusEnum.CANCEL.getStatus().equals(quote.getStatus())) {
            throw exception(SALE_QUOTE_ITEM_BATCH_UPDATE_FAIL_STATUS, quote.getNo());
        }

        List<ErpSaleQuoteItemDO> quoteItems = saleQuoteItemMapper.selectListByQuoteId(updateReqVO.getQuoteId());
        Map<Long, ErpSaleQuoteItemDO> itemMap = convertMap(quoteItems, ErpSaleQuoteItemDO::getId);
        List<ErpSaleQuoteItemDO> selectedItems = new ArrayList<>();
        for (Long itemId : new LinkedHashSet<>(updateReqVO.getItemIds())) {
            ErpSaleQuoteItemDO item = itemMap.get(itemId);
            if (item == null) {
                throw exception(SALE_QUOTE_ITEM_BATCH_UPDATE_NOT_EXISTS, itemId);
            }
            selectedItems.add(item);
        }

        ErpWarehouseDO targetWarehouse = null;
        Long targetDeptId = null;
        if (updateWarehouse || updateDept) {
            targetWarehouse = batchUpdateSupport.validateTargetWarehouse(updateReqVO.getWarehouseId());
            targetDeptId = batchUpdateSupport.resolveTargetDeptId(targetWarehouse, updateReqVO.getDeptId(),
                    FIELD_PERMISSION_MODULE, SALE_QUOTE_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
            for (ErpSaleQuoteItemDO item : selectedItems) {
                Long finalWarehouseId = targetWarehouse != null ? targetWarehouse.getId() : item.getWarehouseId();
                batchUpdateSupport.validateWarehouseDept(finalWarehouseId, targetDeptId, FIELD_PERMISSION_MODULE,
                        SALE_QUOTE_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);
            }
            validateBatchUpdateSaleQuoteNoDuplicate(quoteItems, updateReqVO.getItemIds(), targetWarehouse);
        }

        Map<Long, BigDecimal> productPriceMap = updatePrice
                ? priceLevelPricePicker.pickProductPriceMap(convertSet(selectedItems, ErpSaleQuoteItemDO::getProductId),
                updateReqVO.getPriceLevel()) : Collections.emptyMap();
        ErpWarehouseDO finalTargetWarehouse = targetWarehouse;
        Long finalTargetDeptId = targetDeptId;
        selectedItems.forEach(item -> {
            if (finalTargetWarehouse != null) {
                item.setWarehouseId(finalTargetWarehouse.getId());
                item.setBatchNo(null);
            }
            if (updateDept || finalTargetWarehouse != null) {
                item.setDeptId(finalTargetDeptId);
            }
            if (updatePrice) {
                BigDecimal productPrice = Boolean.TRUE.equals(item.getGiftFlag())
                        ? BigDecimal.ZERO : productPriceMap.getOrDefault(item.getProductId(), BigDecimal.ZERO);
                item.setProductPrice(productPrice);
                item.setTotalPrice(MoneyUtils.priceMultiply(productPrice, item.getCount()));
                item.setTaxPrice(item.getTotalPrice() != null && item.getTaxPercent() != null
                        ? MoneyUtils.priceMultiplyPercent(item.getTotalPrice(), item.getTaxPercent()) : BigDecimal.ZERO);
            }
        });
        if (updatePrice) {
            saleQuoteItemMapper.updateBatch(selectedItems);
            ErpSaleQuoteDO totalUpdate = new ErpSaleQuoteDO()
                    .setId(updateReqVO.getQuoteId())
                    .setDiscountPercent(quote.getDiscountPercent())
                    .setFeeAmount(quote.getFeeAmount())
                    .setOtherPrice(quote.getOtherPrice());
            calculateTotalPrice(totalUpdate, quoteItems);
            saleQuoteMapper.updateById(totalUpdate);
        } else {
            saleQuoteItemMapper.updateWarehouseDeptByIds(convertList(selectedItems, ErpSaleQuoteItemDO::getId),
                    targetWarehouse != null ? targetWarehouse.getId() : null, targetDeptId, targetWarehouse != null);
        }
        recordUpdate(updateReqVO.getQuoteId(), quote.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSaleQuote(Long id) {
        ErpSaleQuoteDO quote = validateSaleQuoteExists(id);
        if (!ErpSaleQuoteStatusEnum.DRAFT.getStatus().equals(quote.getStatus())) {
            throw exception(SALE_QUOTE_SUBMIT_FAIL);
        }
        if (quote.getCustomerId() == null) {
            throw exception(SALE_QUOTE_SUBMIT_CUSTOMER_REQUIRED);
        }
        List<ErpSaleQuoteItemDO> persistedItems = saleQuoteItemMapper.selectListByQuoteId(id);
        if (CollUtil.isEmpty(persistedItems)) {
            throw exception(SALE_QUOTE_SUBMIT_ITEMS_REQUIRED);
        }
        ErpSaleQuoteSaveReqVO submitReqVO = BeanUtils.toBean(quote, ErpSaleQuoteSaveReqVO.class);
        submitReqVO.setItems(BeanUtils.toBean(persistedItems, ErpSaleQuoteSaveReqVO.Item.class));
        Long quoteDeptId = prepareSaleQuoteDept(submitReqVO);
        validateSaleQuoteItems(submitReqVO.getItems(), quoteDeptId);
        if (submitReqVO.getAccountId() != null) {
            accountService.validateAccount(submitReqVO.getAccountId());
        }
        if (submitReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(submitReqVO.getSaleUserId());
        }
        int updateCount = saleQuoteMapper.updateByIdAndStatus(id, ErpSaleQuoteStatusEnum.DRAFT.getStatus(),
                new ErpSaleQuoteDO()
                        .setStatus(ErpSaleQuoteStatusEnum.PROCESS.getStatus())
                        .setDeptId(quoteDeptId));
        if (updateCount == 0) {
            throw exception(SALE_QUOTE_SUBMIT_FAIL);
        }
        record(id, "提交", "提交报价订单，单据编号：" + quote.getNo(), quote.getNo());
    }

    @Override
    public void updateSaleQuoteRemark(ErpSaleUpdateRemarkReqVO updateReqVO) {
        ErpSaleQuoteDO quote = validateSaleQuoteExists(updateReqVO.getId());
        saleQuoteMapper.updateById(new ErpSaleQuoteDO()
                .setId(updateReqVO.getId())
                .setRemark(updateReqVO.getRemark()));
        recordUpdate(updateReqVO.getId(), quote.getNo());
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
            recordDelete(quote.getId(), quote.getNo());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long approveSaleQuote(Long id) {
        ErpSaleQuoteDO quote = validateSaleQuoteExists(id);
        if (ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus().equals(quote.getStatus())) {
            ErpSaleOutDO saleOut = getGeneratedSaleOut(id);
            if (saleOut != null) {
                return saleOut.getId();
            }
            throw exception(SALE_OUT_NOT_EXISTS);
        }
        if (!ErpSaleQuoteStatusEnum.PROCESS.getStatus().equals(quote.getStatus())) {
            throw exception(SALE_QUOTE_APPROVE_FAIL);
        }
        List<ErpSaleQuoteItemDO> items = saleQuoteItemMapper.selectListByQuoteId(id);
        Long saleOutId = saleOutService.createGeneratedSaleOut(buildSaleOutReqVO(quote, items),
                ErpSaleBizSourceTypeEnum.QUOTE.getType(), quote.getId(), quote.getNo());
        int updateCount = saleQuoteMapper.updateByIdAndStatus(id, ErpSaleQuoteStatusEnum.PROCESS.getStatus(),
                new ErpSaleQuoteDO().setStatus(ErpSaleQuoteStatusEnum.GENERATED_SALE_OUT.getStatus()));
        if (updateCount == 0) {
            throw exception(SALE_QUOTE_APPROVE_FAIL);
        }
        recordStatus(id, quote.getNo(), true);
        return saleOutId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToCart(ErpSaleQuoteConvertCartReqVO reqVO) {
        ErpSaleQuoteDO quote = validateSaleQuoteExists(reqVO.getQuoteId());
        if (!ErpSaleQuoteStatusEnum.PROCESS.getStatus().equals(quote.getStatus())
                && !ErpSaleQuoteStatusEnum.APPROVE.getStatus().equals(quote.getStatus())
                && !ErpSaleQuoteStatusEnum.PART_CONVERTED_CART.getStatus().equals(quote.getStatus())) {
            throw exception(SALE_QUOTE_CONVERT_CART_FAIL);
        }
        List<ErpSaleQuoteItemDO> quoteItems = saleQuoteItemMapper.selectListByQuoteId(reqVO.getQuoteId());
        Map<Long, ErpSaleQuoteItemDO> quoteItemMap = convertMap(quoteItems, ErpSaleQuoteItemDO::getId);
        Map<Long, BigDecimal> convertCountMap = new LinkedHashMap<>();
        reqVO.getItems().forEach(reqItem -> convertCountMap.merge(reqItem.getQuoteItemId(),
                reqItem.getCount(), BigDecimal::add));
        validateConvertCartCount(quoteItemMap, convertCountMap);

        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_CART_NO_PREFIX);
        if (saleCartMapper.selectByNo(no) != null) {
            throw exception(SALE_CART_NO_EXISTS);
        }
        ErpSaleCartDO cart = BeanUtils.toBean(quote, ErpSaleCartDO.class, in -> in
                .setId(null).setNo(no).setStatus(ErpSaleCartStatusEnum.SUBMITTED.getStatus())
                .setCartTime(quote.getQuoteTime())
                .setSourceType(ErpSaleBizSourceTypeEnum.QUOTE.getType())
                .setSourceId(quote.getId()).setSourceNo(quote.getNo()));
        List<ErpSaleCartItemDO> cartItems = convertList(convertCountMap.entrySet(), entry -> {
            ErpSaleQuoteItemDO quoteItem = quoteItemMap.get(entry.getKey());
            BigDecimal productPrice = Boolean.TRUE.equals(quoteItem.getGiftFlag())
                    ? BigDecimal.ZERO : quoteItem.getProductPrice();
            return BeanUtils.toBean(quoteItem, ErpSaleCartItemDO.class, item -> item
                    .setId(null).setCartId(null)
                    .setProductPrice(productPrice)
                    .setGiftFlag(Boolean.TRUE.equals(quoteItem.getGiftFlag()))
                    .setTaxPercent(quoteItem.getTaxPercent())
                    .setRemark(quoteItem.getRemark())
                    .setWarehouseId(quoteItem.getWarehouseId())
                    .setDeptId(quoteItem.getDeptId())
                    .setBatchNo(quoteItem.getBatchNo())
                    .setProductUnitId(quoteItem.getProductUnitId())
                    .setCount(entry.getValue())
                    .setTotalPrice(MoneyUtils.priceMultiply(productPrice, entry.getValue()))
                    .setTaxPrice(MoneyUtils.priceMultiplyPercent(
                            MoneyUtils.priceMultiply(productPrice, entry.getValue()),
                            quoteItem.getTaxPercent())));
        });
        calculateCartTotalPrice(cart, cartItems);
        saleDocumentDefaultService.fillCreateDefaults(cart);
        saleCartMapper.insert(cart);
        cartItems.forEach(item -> item.setCartId(cart.getId()));
        cartItems.forEach(item ->
                item.setStockCount(getStockCountIgnoreDataPermission(item.getProductId(), item.getWarehouseId())));
        saleCartItemMapper.insertBatch(cartItems);
        convertCountMap.forEach((quoteItemId, count) -> {
            ErpSaleQuoteItemDO quoteItem = quoteItemMap.get(quoteItemId);
            BigDecimal oldConvertedCount = quoteItem.getConvertedCount() != null ? quoteItem.getConvertedCount() : BigDecimal.ZERO;
            saleQuoteItemMapper.updateById(new ErpSaleQuoteItemDO().setId(quoteItem.getId())
                    .setConvertedCount(oldConvertedCount.add(count)));
        });
        Integer nextStatus = isAllConverted(quoteItems, convertCountMap) ? ErpSaleQuoteStatusEnum.CONVERTED_CART.getStatus()
                : ErpSaleQuoteStatusEnum.PART_CONVERTED_CART.getStatus();
        int updateCount = saleQuoteMapper.updateByIdAndStatus(quote.getId(), quote.getStatus(),
                new ErpSaleQuoteDO().setStatus(nextStatus));
        if (updateCount == 0) {
            throw exception(SALE_QUOTE_CONVERT_CART_FAIL);
        }
        saleConvertRecordMapper.insertBatch(buildQuoteToCartRecords(quote, quoteItemMap, cart, cartItems, convertCountMap));
        record(quote.getId(), "转手推车",
                "报价订单转销售手推车，单据编号：" + quote.getNo() + "，目标单号：" + cart.getNo(), quote.getNo());
        return cart.getId();
    }

    private BigDecimal getStockCountIgnoreDataPermission(Long productId, Long warehouseId) {
        if (stockService == null) {
            return BigDecimal.ZERO;
        }
        return DataPermissionUtils.executeIgnore(() -> stockService.getStockCount(productId, warehouseId));
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
            if (!Boolean.TRUE.equals(item.getGiftFlag()) && item.getProductPrice() == null) {
                throw exception(SALE_QUOTE_ITEM_PRODUCT_PRICE_NOT_NULL, quoteItemId);
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
                                                                 Map<Long, BigDecimal> convertCountMap) {
        return convertList(convertCountMap.entrySet(), entry -> {
            ErpSaleQuoteItemDO sourceItem = quoteItemMap.get(entry.getKey());
            ErpSaleCartItemDO targetItem = cartItems.stream()
                    .filter(item -> sourceItem.getProductId().equals(item.getProductId())
                            && sourceItem.getWarehouseId().equals(item.getWarehouseId())
                            && Boolean.TRUE.equals(sourceItem.getGiftFlag()) == Boolean.TRUE.equals(item.getGiftFlag())
                            && entry.getValue().compareTo(item.getCount()) == 0)
                    .findFirst().orElse(null);
            return new ErpSaleConvertRecordDO()
                    .setConvertType(ErpSaleConvertTypeEnum.QUOTE_TO_CART.getType())
                    .setSourceType(ErpSaleBizSourceTypeEnum.QUOTE.getType())
                    .setSourceId(quote.getId()).setSourceNo(quote.getNo()).setSourceItemId(sourceItem.getId())
                    .setTargetType(ErpSaleBizSourceTypeEnum.CART.getType())
                    .setTargetId(cart.getId()).setTargetNo(cart.getNo()).setTargetItemId(targetItem != null ? targetItem.getId() : null)
                    .setProductId(sourceItem.getProductId()).setWarehouseId(sourceItem.getWarehouseId()).setCount(entry.getValue());
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
        BigDecimal feeAmount = resolveFeeAmount(cart.getFeeAmount(), cart.getOtherPrice());
        cart.setFeeAmount(feeAmount);
        cart.setOtherPrice(feeAmount);
        cart.setDiscountPrice(MoneyUtils.priceMultiplyPercent(cart.getTotalPrice(), cart.getDiscountPercent()));
        cart.setTotalPrice(cart.getTotalPrice().subtract(cart.getDiscountPrice()).add(feeAmount));
    }

    private ErpSaleOutSaveReqVO buildSaleOutReqVO(ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> items) {
        ErpSaleOutSaveReqVO reqVO = new ErpSaleOutSaveReqVO();
        reqVO.setCustomerId(quote.getCustomerId());
        reqVO.setAccountId(quote.getAccountId());
        reqVO.setSaleUserId(quote.getSaleUserId());
        reqVO.setDeptId(quote.getDeptId());
        reqVO.setOutTime(quote.getQuoteTime());
        reqVO.setDiscountPercent(quote.getDiscountPercent());
        reqVO.setFeeAmount(quote.getFeeAmount());
        reqVO.setOtherPrice(quote.getOtherPrice());
        reqVO.setFileUrl(quote.getFileUrl());
        reqVO.setRemark(quote.getRemark());
        reqVO.setItems(convertList(items, item -> {
            ErpSaleOutSaveReqVO.Item outItem = new ErpSaleOutSaveReqVO.Item();
            outItem.setWarehouseId(item.getWarehouseId());
            outItem.setDeptId(item.getDeptId());
            outItem.setProductId(item.getProductId());
            outItem.setProductUnitId(item.getProductUnitId());
            outItem.setUnitWeight(item.getWeight());
            outItem.setPackageQty(item.getPackageQty());
            outItem.setProductPrice(Boolean.TRUE.equals(item.getGiftFlag()) ? BigDecimal.ZERO : item.getProductPrice());
            outItem.setCount(item.getCount());
            outItem.setTaxPercent(item.getTaxPercent());
            outItem.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            outItem.setBatchNo(item.getBatchNo());
            outItem.setRemark(item.getRemark());
            return outItem;
        }));
        return reqVO;
    }

    private ErpSaleOutDO getGeneratedSaleOut(Long quoteId) {
        return DataPermissionUtils.executeIgnore(() -> saleOutMapper.selectBySourceTypeAndSourceId(
                ErpSaleBizSourceTypeEnum.QUOTE.getType(), quoteId));
    }

    private Long prepareSaleQuoteDept(ErpSaleQuoteSaveReqVO reqVO) {
        List<Long> saleDeptIds = customerService.getCustomerSaleDeptIds(reqVO.getCustomerId());
        Long deptId = reqVO.getDeptId();
        if (deptId == null) {
            deptId = resolveDefaultQuoteDeptId(saleDeptIds);
            reqVO.setDeptId(deptId);
        }
        customerService.validateCustomerSaleDept(reqVO.getCustomerId(), deptId);
        customerService.validateCustomerForSale(reqVO.getCustomerId(), deptId);
        return deptId;
    }

    private Long resolveDefaultQuoteDeptId(List<Long> saleDeptIds) {
        if (CollUtil.isEmpty(saleDeptIds)) {
            return null;
        }
        Long loginDeptId = getLoginUserDeptId();
        if (loginDeptId != null && saleDeptIds.contains(loginDeptId)) {
            return loginDeptId;
        }
        return saleDeptIds.get(0);
    }

    private List<ErpSaleQuoteItemDO> validateSaleQuoteItems(List<ErpSaleQuoteSaveReqVO.Item> list, Long quoteDeptId) {
        validateSaleQuoteItemBasics(list);
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() ->
                productService.validProductList(convertSet(list, ErpSaleQuoteSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpSaleQuoteSaveReqVO.Item::getProductId, ErpSaleQuoteSaveReqVO.Item::getBatchNo);
        Set<Long> warehouseIds = convertSet(list, ErpSaleQuoteSaveReqVO.Item::getWarehouseId);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(warehouseService.validSaleWarehouseList(warehouseIds),
                ErpWarehouseDO::getId);
        if (quoteDeptId != null) {
            validateSaleQuoteWarehouseIdsAllowed(warehouseIds, quoteDeptId);
        }
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleQuoteItemDO.class, item -> {
            item.setId(null); // Clear stale id returned from frontend before insertBatch.
            ErpProductDO product = productMap.get(item.getProductId());
            item.setProductUnitId(product.getUnitId());
            fillProductWeightAndPackage(item, product);
            item.setDeptId(resolveQuoteItemDeptId(item.getProductId(), item.getWarehouseId(),
                    item.getDeptId(), warehouseMap));
            item.setConvertedCount(BigDecimal.ZERO);
            item.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            if (Boolean.TRUE.equals(item.getGiftFlag())) {
                item.setProductPrice(BigDecimal.ZERO);
            }
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            if (item.getTotalPrice() != null && item.getTaxPercent() != null) {
                item.setTaxPrice(MoneyUtils.priceMultiplyPercent(item.getTotalPrice(), item.getTaxPercent()));
            }
        }));
    }

    private List<ErpSaleQuoteSaveReqVO.Item> filterDraftItems(List<ErpSaleQuoteSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(item -> item != null && item.getProductId() != null
                        && item.getWarehouseId() != null && item.getCount() != null
                        && item.getCount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    private List<ErpSaleQuoteItemDO> validateSaleQuoteDraftItems(List<ErpSaleQuoteSaveReqVO.Item> list, Long quoteDeptId) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        validateDuplicateSaleQuoteItems(list);
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() ->
                productService.validProductList(convertSet(list, ErpSaleQuoteSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        productBatchNoValidator.validateBatchNoAllowed(list, productMap,
                ErpSaleQuoteSaveReqVO.Item::getProductId, ErpSaleQuoteSaveReqVO.Item::getBatchNo);
        Set<Long> warehouseIds = convertSet(list, ErpSaleQuoteSaveReqVO.Item::getWarehouseId);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(warehouseService.validSaleWarehouseList(warehouseIds),
                ErpWarehouseDO::getId);
        if (quoteDeptId != null) {
            validateSaleQuoteWarehouseIdsAllowed(warehouseIds, quoteDeptId);
        }
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleQuoteItemDO.class, item -> {
            item.setId(null); // Clear stale id returned from frontend before insertBatch.
            ErpProductDO product = productMap.get(item.getProductId());
            item.setProductUnitId(product.getUnitId());
            fillProductWeightAndPackage(item, product);
            item.setDeptId(resolveQuoteItemDeptId(item.getProductId(), item.getWarehouseId(),
                    item.getDeptId(), warehouseMap));
            item.setConvertedCount(BigDecimal.ZERO);
            item.setGiftFlag(Boolean.TRUE.equals(item.getGiftFlag()));
            if (Boolean.TRUE.equals(item.getGiftFlag())
                    || item.getProductPrice() == null
                    || item.getProductPrice().compareTo(BigDecimal.ZERO) < 0) {
                item.setProductPrice(BigDecimal.ZERO);
            }
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            if (item.getTotalPrice() != null && item.getTaxPercent() != null) {
                item.setTaxPrice(MoneyUtils.priceMultiplyPercent(item.getTotalPrice(), item.getTaxPercent()));
            }
        }));
    }

    private void validateSaleQuoteItemBasics(List<ErpSaleQuoteSaveReqVO.Item> list) {
        validateDuplicateSaleQuoteItems(list);
        validateSaleQuoteItemAmounts(list);
    }

    private void fillProductWeightAndPackage(ErpSaleQuoteItemDO item, ErpProductDO product) {
        if (product == null) {
            return;
        }
        if (item.getWeight() == null) {
            item.setWeight(product.getWeight());
        }
        if (item.getPackageQty() == null) {
            item.setPackageQty(product.getPackageQty());
        }
    }

    private void validateSaleQuoteItemWarehousesAllowed(Collection<ErpSaleQuoteItemDO> items, Long quoteDeptId) {
        validateSaleQuoteWarehouseIdsAllowed(convertSet(items, ErpSaleQuoteItemDO::getWarehouseId), quoteDeptId);
    }

    private void validateSaleQuoteWarehouseIdsAllowed(Collection<Long> warehouseIds, Long quoteDeptId) {
        if (quoteDeptId == null || CollUtil.isEmpty(warehouseIds)) {
            return;
        }
        warehouseIds.forEach(warehouseId -> warehouseService.validateWarehouseSaleSelectableForDept(warehouseId, quoteDeptId));
    }

    private Long resolveQuoteItemDeptId(Long productId, Long warehouseId, Long itemDeptId,
                                        Map<Long, ErpWarehouseDO> warehouseMap) {
        ErpStockDO stock = DataPermissionUtils.executeIgnore(() -> stockService.getStock(productId, warehouseId));
        if (stock != null && stock.getDeptId() != null) {
            return stock.getDeptId();
        }
        ErpWarehouseDO warehouse = warehouseMap.get(warehouseId);
        if (warehouse != null && warehouse.getDeptId() != null) {
            return warehouse.getDeptId();
        }
        return itemDeptId;
    }

    private void validateSaleQuoteItemAmounts(List<ErpSaleQuoteSaveReqVO.Item> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            ErpSaleQuoteSaveReqVO.Item item = list.get(i);
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(SALE_QUOTE_ITEM_COUNT_POSITIVE, i + 1);
            }
            if (!Boolean.TRUE.equals(item.getGiftFlag())
                    && (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) <= 0)) {
                throw exception(SALE_QUOTE_ITEM_PRODUCT_PRICE_POSITIVE, i + 1);
            }
        }
    }

    private void validateDuplicateSaleQuoteItems(List<ErpSaleQuoteSaveReqVO.Item> list) {
        Set<String> itemKeySet = new LinkedHashSet<>();
        if (CollUtil.isEmpty(list)) {
            return;
        }
        for (ErpSaleQuoteSaveReqVO.Item item : list) {
            String itemKey = item.getProductId() + "|" + item.getWarehouseId() + "|"
                    + (Boolean.TRUE.equals(item.getGiftFlag()) ? 1 : 0);
            if (!itemKeySet.add(itemKey)) {
                throw exception(SALE_QUOTE_ITEM_DUPLICATE, buildSaleQuoteItemDuplicateLabel(item));
            }
        }
    }

    private void validateBatchUpdateSaleQuoteNoDuplicate(List<ErpSaleQuoteItemDO> quoteItems,
                                                         List<Long> selectedItemIds,
                                                         ErpWarehouseDO targetWarehouse) {
        Set<Long> selectedItemIdSet = new LinkedHashSet<>(selectedItemIds);
        Set<String> itemKeySet = new LinkedHashSet<>();
        for (ErpSaleQuoteItemDO item : quoteItems) {
            boolean selected = selectedItemIdSet.contains(item.getId());
            Long finalWarehouseId = selected && targetWarehouse != null ? targetWarehouse.getId() : item.getWarehouseId();
            String itemKey = item.getProductId() + "|" + finalWarehouseId + "|"
                    + (Boolean.TRUE.equals(item.getGiftFlag()) ? 1 : 0);
            if (!itemKeySet.add(itemKey)) {
                throw exception(SALE_QUOTE_ITEM_DUPLICATE, "productId=" + item.getProductId()
                        + ", warehouseId=" + finalWarehouseId
                        + ", giftFlag=" + Boolean.TRUE.equals(item.getGiftFlag()));
            }
        }
    }

    private String buildSaleQuoteItemDuplicateLabel(ErpSaleQuoteSaveReqVO.Item item) {
        return "productId=" + item.getProductId()
                + ", warehouseId=" + item.getWarehouseId()
                + ", giftFlag=" + Boolean.TRUE.equals(item.getGiftFlag());
    }

    private void calculateTotalPrice(ErpSaleQuoteDO quote, List<ErpSaleQuoteItemDO> items) {
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
    public PageResult<ErpSaleQuoteItemDO> getSaleQuoteItemPage(ErpSaleQuoteItemPageReqVO pageReqVO) {
        validateSaleQuoteExists(pageReqVO.getQuoteId());
        return saleQuoteItemMapper.selectPageByQuoteId(pageReqVO);
    }

    @Override
    public List<ErpSaleQuoteItemDO> getSaleQuoteItemListByQuoteIds(Collection<Long> quoteIds) {
        if (CollUtil.isEmpty(quoteIds)) {
            return Collections.emptyList();
        }
        return saleQuoteItemMapper.selectListByQuoteIds(quoteIds);
    }

    @Override
    public List<DeptSimpleRespVO> getWarehouseAvailableDeptSimpleList(Long warehouseId) {
        return batchUpdateSupport.getWarehouseAvailableDeptSimpleList(warehouseId, FIELD_PERMISSION_MODULE);
    }

    @Override
    public ErpSaleQuoteImportRespVO parseImportData(List<ErpSaleQuoteImportExcelVO> list) {
        ErpSaleQuoteImportRespVO respVO = new ErpSaleQuoteImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        ErpImportProductResolver productResolver = ErpImportProductResolver.build(list,
                ErpSaleQuoteImportExcelVO::getProductCode, ErpSaleQuoteImportExcelVO::getProductName, ErpSaleQuoteImportExcelVO::getFactoryCode, productMapper);
        Map<Long, ErpProductRespVO> productVOMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(convertList(productResolver.getResolvedProducts(), ErpProductDO::getId)));
        list.forEach((row) -> {
            int rowNo = respVO.getSuccessCount() + respVO.getFailureCount() + 2;
            ErpImportProductResolver.ResolveResult productResult =
                    productResolver.resolve(row.getProductCode(), row.getProductName(), row.getFactoryCode());
            if (productResult.isFailure()) {
                respVO.getFailureDetails().add(new ErpSaleQuoteImportRespVO.FailureItem(
                        rowNo, productResult.getIdentifier(), productResult.getErrorMessage()));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                return;
            }
            ErpProductDO product = productResult.getProduct();
            if (product.getDefaultWarehouseId() == null) {
                respVO.getFailureDetails().add(new ErpSaleQuoteImportRespVO.FailureItem(
                        rowNo, productResult.getIdentifier(), "产品默认仓库不能为空"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                return;
            }
            BigDecimal count = row.getCount() == null ? BigDecimal.ZERO : row.getCount();
            if (count.compareTo(BigDecimal.ZERO) <= 0) {
                respVO.getFailureDetails().add(new ErpSaleQuoteImportRespVO.FailureItem(
                        rowNo, productResult.getIdentifier(), "数量必须大于 0"));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
                return;
            }
            ErpSaleQuoteRespVO.Item item = new ErpSaleQuoteRespVO.Item();
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductUnitId(product.getUnitId());
            item.setProductUnitName(productVOMap.get(product.getId()) == null ? null : productVOMap.get(product.getId()).getUnitName());
            item.setWarehouseId(product.getDefaultWarehouseId());
            item.setGiftFlag(Boolean.TRUE.equals(row.getGiftFlag()));
            item.setProductPrice(Boolean.TRUE.equals(item.getGiftFlag())
                    ? BigDecimal.ZERO : (row.getProductPrice() != null ? row.getProductPrice() : product.getSalePrice()));
            item.setCount(count);
            respVO.getItems().add(item);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        });
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpSaleImportResultRespVO importSaleQuoteOrderList(List<ErpSaleQuoteOrderImportExcelVO> list) {
        ErpSaleImportResultRespVO respVO = new ErpSaleImportResultRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }
        Map<String, ErpCustomerDO> customerMap = buildCustomerMap();
        ErpImportProductResolver productResolver = ErpImportProductResolver.build(list,
                ErpSaleQuoteOrderImportExcelVO::getProductCode, ErpSaleQuoteOrderImportExcelVO::getProductName,
                ErpSaleQuoteOrderImportExcelVO::getFactoryCode, productMapper);
        Map<String, cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO> warehouseMap =
                warehouseService.getCurrentUserVisibleSaleWarehouseList().stream()
                        .collect(Collectors.toMap(item -> normalizeKey(item.getName()), item -> item, (a, b) -> a));

        List<SaleQuoteOrderImportGroup> groups = new ArrayList<>();
        SaleQuoteOrderImportGroup currentGroup = null;
        for (int i = 0; i < list.size(); i++) {
            ErpSaleQuoteOrderImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            if (isBlankQuoteOrderRow(row)) {
                continue;
            }
            if (hasQuoteOrderMainFields(row)) {
                ErpCustomerDO customer = customerMap.get(normalizeKey(row.getCustomerName()));
                currentGroup = new SaleQuoteOrderImportGroup(rowNo, row, customer);
                groups.add(currentGroup);
                String orderNo = resolveQuoteImportNo(rowNo, row);
                if (trimToNull(row.getCustomerName()) == null) {
                    addImportFailure(respVO, rowNo, orderNo, null, "客户名称不能为空");
                } else if (customer == null) {
                    addImportFailure(respVO, rowNo, orderNo, null, "客户不存在：" + row.getCustomerName());
                } else if (CommonStatusEnum.isDisable(customer.getStatus())) {
                    addImportFailure(respVO, rowNo, orderNo, null, "客户未启用：" + customer.getName());
                }
            } else if (hasQuoteOrderDetailFields(row) && currentGroup == null) {
                addImportFailure(respVO, rowNo, null,
                        ErpImportProductResolver.getIdentifier(row.getProductCode(), row.getProductName(), row.getFactoryCode()),
                        "明细行前缺少报价订单主表信息");
                continue;
            }
            if (!hasQuoteOrderDetailFields(row)) {
                continue;
            }
            String orderNo = currentGroup == null ? null : resolveQuoteImportNo(currentGroup.getRowNo(), currentGroup.getMainRow());
            ErpImportProductResolver.ResolveResult productResult =
                    productResolver.resolve(row.getProductCode(), row.getProductName(), row.getFactoryCode());
            String productIdentifier = productResult.getIdentifier();
            ErpProductDO product = productResult.getProduct();
            cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO warehouse =
                    trimToNull(row.getWarehouseName()) == null ? null : warehouseMap.get(normalizeKey(row.getWarehouseName()));
            boolean valid = true;
            if (productResult.isFailure()) {
                addImportFailure(respVO, rowNo, orderNo, productIdentifier, productResult.getErrorMessage());
                valid = false;
            }
            if (row.getItemCount() == null || row.getItemCount().compareTo(BigDecimal.ZERO) <= 0) {
                addImportFailure(respVO, rowNo, orderNo, productIdentifier, "数量必须大于 0");
                valid = false;
            }
            if (row.getProductPrice() != null && row.getProductPrice().compareTo(BigDecimal.ZERO) < 0) {
                addImportFailure(respVO, rowNo, orderNo, productIdentifier, "单价不能小于 0");
                valid = false;
            }
            if (trimToNull(row.getWarehouseName()) != null && warehouse == null) {
                addImportFailure(respVO, rowNo, orderNo, productIdentifier, "仓库不存在：" + row.getWarehouseName());
                valid = false;
            }
            if (valid && currentGroup != null) {
                currentGroup.getRows().add(new SaleQuoteOrderImportRow(row, product, warehouse));
            }
        }
        for (SaleQuoteOrderImportGroup group : groups) {
            if (CollUtil.isEmpty(group.getRows())) {
                addImportFailure(respVO, group.getRowNo(), resolveQuoteImportNo(group.getRowNo(), group.getMainRow()), null,
                        "报价订单至少需要一条明细");
            }
        }
        if (respVO.getFailureCount() > 0) {
            return respVO;
        }
        for (SaleQuoteOrderImportGroup group : groups) {
            Long id = createSaleQuote(buildSaleQuoteSaveReq(group));
            respVO.getDocumentIds().add(id);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        return respVO;
    }

    private ErpSaleQuoteSaveReqVO buildSaleQuoteSaveReq(SaleQuoteOrderImportGroup group) {
        ErpSaleQuoteOrderImportExcelVO mainRow = group.getMainRow();
        ErpSaleQuoteSaveReqVO saveReqVO = new ErpSaleQuoteSaveReqVO();
        saveReqVO.setCustomerId(group.getCustomer().getId());
        saveReqVO.setQuoteTime(LocalDateTime.now());
        saveReqVO.setRemark(trimToNull(mainRow.getRemark()));
        saveReqVO.setDiscountPercent(BigDecimal.ZERO);
        saveReqVO.setOtherPrice(BigDecimal.ZERO);
        saveReqVO.setItems(convertList(group.getRows(), this::buildSaleQuoteImportItem));
        return saveReqVO;
    }

    private ErpSaleQuoteSaveReqVO.Item buildSaleQuoteImportItem(SaleQuoteOrderImportRow importRow) {
        ErpSaleQuoteOrderImportExcelVO row = importRow.getRow();
        ErpProductDO product = importRow.getProduct();
        ErpSaleQuoteSaveReqVO.Item item = new ErpSaleQuoteSaveReqVO.Item();
        item.setProductId(product.getId());
        item.setWarehouseId(importRow.getWarehouse() != null ? importRow.getWarehouse().getId() : product.getDefaultWarehouseId());
        item.setCount(row.getItemCount());
        item.setProductPrice(row.getProductPrice() != null ? row.getProductPrice() : product.getSalePrice());
        item.setGiftFlag(Boolean.TRUE.equals(row.getGiftFlag()));
        item.setTaxPercent(row.getTaxPercent());
        item.setRemark(trimToNull(row.getItemRemark()));
        return item;
    }

    private Map<String, ErpCustomerDO> buildCustomerMap() {
        Map<String, ErpCustomerDO> map = new LinkedHashMap<>();
        cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO pageReqVO =
                new cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO();
        pageReqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        customerService.getCustomerPage(pageReqVO).getList().forEach(customer -> {
            putIfNotBlank(map, customer.getCode(), customer);
            putIfNotBlank(map, customer.getName(), customer);
            putIfNotBlank(map, customer.getShortName(), customer);
        });
        return map;
    }

    private void putIfNotBlank(Map<String, ErpCustomerDO> map, String key, ErpCustomerDO customer) {
        String normalized = normalizeKey(key);
        if (normalized != null) {
            map.putIfAbsent(normalized, customer);
        }
    }

    private Set<String> extractQuoteOrderProductCodes(List<ErpSaleQuoteOrderImportExcelVO> list) {
        Set<String> codes = new LinkedHashSet<>();
        for (ErpSaleQuoteOrderImportExcelVO row : list) {
            String code = row == null ? null : trimToNull(row.getProductCode());
            if (code != null) {
                codes.add(code);
            }
        }
        return codes;
    }

    private boolean isBlankQuoteOrderRow(ErpSaleQuoteOrderImportExcelVO row) {
        return row == null || !hasQuoteOrderMainFields(row) && !hasQuoteOrderDetailFields(row);
    }

    private boolean hasQuoteOrderMainFields(ErpSaleQuoteOrderImportExcelVO row) {
        return trimToNull(row.getCustomerName()) != null
                || trimToNull(row.getRemark()) != null;
    }

    private boolean hasQuoteOrderDetailFields(ErpSaleQuoteOrderImportExcelVO row) {
        return trimToNull(row.getProductCode()) != null
                || trimToNull(row.getProductName()) != null
                || trimToNull(row.getFactoryCode()) != null
                || trimToNull(row.getWarehouseName()) != null
                || row.getItemCount() != null
                || row.getProductPrice() != null
                || row.getGiftFlag() != null
                || row.getTaxPercent() != null
                || trimToNull(row.getItemRemark()) != null;
    }

    private String resolveQuoteImportNo(Integer rowNo, ErpSaleQuoteOrderImportExcelVO row) {
        String customerName = trimToNull(row.getCustomerName());
        return customerName != null ? customerName : "row " + rowNo;
    }

    private boolean validateImportDate(ErpSaleImportResultRespVO respVO, Integer rowNo, String orderNo,
                                       String productCode, String label, String value) {
        if (trimToNull(value) == null) {
            return true;
        }
        try {
            parseImportDate(value, null);
            return true;
        } catch (IllegalArgumentException ignored) {
            addImportFailure(respVO, rowNo, orderNo, productCode,
                    label + "格式不正确，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
            return false;
        }
    }

    private LocalDateTime parseImportDate(String value, LocalDateTime defaultValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")}) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format.
            }
        }
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            throw new IllegalArgumentException("Invalid date format: " + value);
        }
    }

    private void addImportFailure(ErpSaleImportResultRespVO respVO, Integer rowNo, String orderNo,
                                  String productCode, String reason) {
        respVO.getFailureDetails().add(new ErpSaleImportResultRespVO.FailureItem(rowNo, orderNo, productCode, reason));
        respVO.setFailureCount(respVO.getFailureCount() + 1);
    }

    private String normalizeKey(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void clearHiddenFields(Object target) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.clearSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, target);
        }
    }

    private void clearHiddenItemFields(Object context, List<?> items) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.clearSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, context, items);
        }
    }

    private void preserveHiddenFields(Object target, Object existing) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.preserveSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, target, existing);
        }
    }

    private void preserveHiddenItemFields(Object context, List<?> items, List<?> existingItems) {
        if (fieldPermissionMasker != null) {
            fieldPermissionMasker.preserveSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, context, items, existingItems);
        }
    }

    private void recordCreate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordCreate(ERP_SALE_QUOTE_TYPE, id, no);
        }
    }

    private void recordUpdate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordUpdate(ERP_SALE_QUOTE_TYPE, id, no);
        }
    }

    private void recordDelete(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordDelete(ERP_SALE_QUOTE_TYPE, id, no);
        }
    }

    private void recordStatus(Long id, String no, boolean approve) {
        if (operateLogService != null) {
            operateLogService.recordStatus(ERP_SALE_QUOTE_TYPE, id, no, approve);
        }
    }

    private void record(Long id, String subType, String action, String no) {
        if (operateLogService != null) {
            operateLogService.record(ERP_SALE_QUOTE_TYPE, subType, id, action, no);
        }
    }

    private static class SaleQuoteOrderImportGroup {
        private final Integer rowNo;
        private final ErpSaleQuoteOrderImportExcelVO mainRow;
        private final ErpCustomerDO customer;
        private final List<SaleQuoteOrderImportRow> rows = new ArrayList<>();

        private SaleQuoteOrderImportGroup(Integer rowNo, ErpSaleQuoteOrderImportExcelVO mainRow, ErpCustomerDO customer) {
            this.rowNo = rowNo;
            this.mainRow = mainRow;
            this.customer = customer;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public ErpSaleQuoteOrderImportExcelVO getMainRow() {
            return mainRow;
        }

        public ErpCustomerDO getCustomer() {
            return customer;
        }

        public List<SaleQuoteOrderImportRow> getRows() {
            return rows;
        }
    }

    private static class SaleQuoteOrderImportRow {
        private final ErpSaleQuoteOrderImportExcelVO row;
        private final ErpProductDO product;
        private final cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO warehouse;

        private SaleQuoteOrderImportRow(ErpSaleQuoteOrderImportExcelVO row, ErpProductDO product,
                                        cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO warehouse) {
            this.row = row;
            this.product = product;
            this.warehouse = warehouse;
        }

        public ErpSaleQuoteOrderImportExcelVO getRow() {
            return row;
        }

        public ErpProductDO getProduct() {
            return product;
        }

        public cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO getWarehouse() {
            return warehouse;
        }
    }

}
