package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSaleOutItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePriceAdjustStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpImportProductResolver;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_SALE_PRICE_ADJUST_TYPE;

/**
 * ERP 销售调价单 Service 实现类
 *
 * @author 汽配ERP
 */
@Service
@Validated
public class ErpSalePriceAdjustServiceImpl implements ErpSalePriceAdjustService {

    private static final String FIELD_PERMISSION_MODULE = "erp_sale_price_adjust";

    @Value("${erp.reporting.dual-cost-enabled:false}")
    private boolean dualCostEnabled;

    @Resource
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;
    @Resource
    private ErpSalePriceAdjustItemMapper salePriceAdjustItemMapper;
    @Resource
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpStockRecordMapper stockRecordMapper;
    @Resource
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleDocumentDefaultService saleDocumentDefaultService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private DeptApi deptApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSalePriceAdjust(ErpSalePriceAdjustSaveReqVO createReqVO) {
        fieldPermissionMasker.clearSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        fieldPermissionMasker.clearSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, createReqVO, createReqVO.getItems());
        // 1. 生成调价单号
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_PRICE_ADJUST_NO_PREFIX);
        // 2. 插入调价单
        ErpSalePriceAdjustDO adjustDO = BeanUtils.toBean(createReqVO, ErpSalePriceAdjustDO.class);
        adjustDO.setNo(no);
        adjustDO.setStatus(ErpAuditStatus.PROCESS.getStatus());
        adjustDO.setAdjustDate(LocalDateTime.now());
        // 计算调价总金额
        BigDecimal totalAdjustPrice = BigDecimal.ZERO;
        List<ErpSalePriceAdjustItemDO> items = BeanUtils.toBean(createReqVO.getItems(), ErpSalePriceAdjustItemDO.class);
        validateFormalSubmit(adjustDO, items);
        fillItemSnapshotsFromSaleOutItems(items);
        validateSalePriceAdjustItemsNotAdjusted(items, null);
        for (ErpSalePriceAdjustItemDO item : items) {
            BigDecimal adjustPrice = calculateAdjustPrice(item);
            item.setAdjustPrice(adjustPrice);
            totalAdjustPrice = totalAdjustPrice.add(adjustPrice);
        }
        adjustDO.setTotalAdjustPrice(totalAdjustPrice);
        saleDocumentDefaultService.fillCreateDefaults(adjustDO);
        salePriceAdjustMapper.insert(adjustDO);
        // 3. 插入调价明细
        for (ErpSalePriceAdjustItemDO item : items) {
            item.setAdjustId(adjustDO.getId());
        }
        salePriceAdjustItemMapper.insertBatch(items);
        recordCreate(adjustDO.getId(), adjustDO.getNo());
        return adjustDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSalePriceAdjustDraft(ErpSalePriceAdjustDraftSaveReqVO createReqVO) {
        fieldPermissionMasker.clearSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, createReqVO);
        fieldPermissionMasker.clearSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, createReqVO, createReqVO.getItems());
        ErpSalePriceAdjustDO adjustDO = BeanUtils.toBean(createReqVO, ErpSalePriceAdjustDO.class);
        adjustDO.setNo(noRedisDAO.generate(ErpNoRedisDAO.SALE_PRICE_ADJUST_NO_PREFIX));
        adjustDO.setStatus(ErpSalePriceAdjustStatusEnum.DRAFT.getStatus());
        adjustDO.setAdjustDate(LocalDateTime.now());
        List<ErpSalePriceAdjustItemDO> items = buildDraftItems(createReqVO.getItems());
        if (CollUtil.isEmpty(items)) {
            throw exception(SALE_PRICE_ADJUST_DRAFT_ITEMS_REQUIRED);
        }
        calculateDraftTotal(adjustDO, items);
        saleDocumentDefaultService.fillCreateDefaults(adjustDO);
        salePriceAdjustMapper.insert(adjustDO);
        insertDraftItems(adjustDO.getId(), items);
        recordCreate(adjustDO.getId(), adjustDO.getNo());
        return adjustDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSalePriceAdjust(ErpSalePriceAdjustSaveReqVO updateReqVO) {
        // 1. 校验存在
        ErpSalePriceAdjustDO existDO = validateSalePriceAdjustExists(updateReqVO.getId());
        if (ErpSalePriceAdjustStatusEnum.DRAFT.getStatus().equals(existDO.getStatus())) {
            throw exception(SALE_PRICE_ADJUST_DRAFT_UPDATE_FAIL, existDO.getNo());
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(existDO.getStatus())) {
            throw exception(SALE_PRICE_ADJUST_UPDATE_FAIL_APPROVE);
        }
        List<ErpSalePriceAdjustItemDO> oldItems = salePriceAdjustItemMapper.selectListByAdjustIdForUpdate(updateReqVO.getId());
        fieldPermissionMasker.preserveSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, existDO);
        fieldPermissionMasker.preserveSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO, updateReqVO.getItems(),
                oldItems);
        boolean incrementalItems = ErpSaleItemOperationHelper.useIncrementalItems(updateReqVO.getItems(),
                ErpSalePriceAdjustSaveReqVO.Item::getOperation, SALE_PRICE_ADJUST_ITEM_OPERATION_INVALID);
        ErpSaleItemOperationHelper.RequestChangeSet<ErpSalePriceAdjustSaveReqVO.Item> itemChangeSet = null;
        List<ErpSalePriceAdjustSaveReqVO.Item> itemReqs = updateReqVO.getItems();
        if (incrementalItems) {
            itemChangeSet = ErpSaleItemOperationHelper.buildRequestChangeSet(updateReqVO.getItems(), oldItems,
                    ErpSalePriceAdjustSaveReqVO.Item.class, ErpSalePriceAdjustSaveReqVO.Item::getId,
                    ErpSalePriceAdjustSaveReqVO.Item::setId, ErpSalePriceAdjustSaveReqVO.Item::getOperation,
                    ErpSalePriceAdjustItemDO::getId, SALE_PRICE_ADJUST_ITEM_OPERATION_INVALID,
                    SALE_PRICE_ADJUST_ITEM_UPDATE_NOT_EXISTS);
            itemReqs = itemChangeSet.getFinalItems();
        }
        // 2. 更新调价单
        ErpSalePriceAdjustDO updateDO = BeanUtils.toBean(updateReqVO, ErpSalePriceAdjustDO.class);
        updateDO.setAdjustDate(existDO.getAdjustDate());
        BigDecimal totalAdjustPrice = BigDecimal.ZERO;
        List<ErpSalePriceAdjustItemDO> items = BeanUtils.toBean(itemReqs, ErpSalePriceAdjustItemDO.class);
        validateFormalSubmit(updateDO, items);
        fillItemSnapshotsFromSaleOutItems(items);
        validateSalePriceAdjustItemsNotAdjusted(items, updateReqVO.getId());
        for (ErpSalePriceAdjustItemDO item : items) {
            BigDecimal adjustPrice = calculateAdjustPrice(item);
            item.setAdjustPrice(adjustPrice);
            totalAdjustPrice = totalAdjustPrice.add(adjustPrice);
        }
        updateDO.setTotalAdjustPrice(totalAdjustPrice);
        salePriceAdjustMapper.updateById(updateDO);
        // 3. 更新明细
        if (incrementalItems) {
            applySalePriceAdjustItemChangeSet(updateReqVO.getId(), itemChangeSet, items);
        } else {
            salePriceAdjustItemMapper.deleteByAdjustId(updateReqVO.getId());
            for (ErpSalePriceAdjustItemDO item : items) {
                item.setId(null);
                item.setAdjustId(updateReqVO.getId());
            }
            salePriceAdjustItemMapper.insertBatch(items);
        }
        recordUpdate(updateReqVO.getId(), existDO.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSalePriceAdjustDraft(ErpSalePriceAdjustDraftSaveReqVO updateReqVO) {
        ErpSalePriceAdjustDO existDO = validateSalePriceAdjustExists(updateReqVO.getId());
        if (!ErpSalePriceAdjustStatusEnum.DRAFT.getStatus().equals(existDO.getStatus())) {
            throw exception(SALE_PRICE_ADJUST_DRAFT_UPDATE_FAIL, existDO.getNo());
        }
        List<ErpSalePriceAdjustItemDO> oldItems = salePriceAdjustItemMapper.selectListByAdjustIdForUpdate(updateReqVO.getId());
        fieldPermissionMasker.preserveSaleDetailHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, existDO);
        fieldPermissionMasker.preserveSaleDetailHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO, updateReqVO.getItems(),
                oldItems);
        boolean incrementalItems = ErpSaleItemOperationHelper.useIncrementalItems(updateReqVO.getItems(),
                ErpSalePriceAdjustSaveReqVO.Item::getOperation, SALE_PRICE_ADJUST_ITEM_OPERATION_INVALID);
        ErpSaleItemOperationHelper.RequestChangeSet<ErpSalePriceAdjustSaveReqVO.Item> itemChangeSet = null;
        List<ErpSalePriceAdjustSaveReqVO.Item> itemReqs = updateReqVO.getItems();
        if (incrementalItems) {
            itemChangeSet = ErpSaleItemOperationHelper.buildRequestChangeSet(updateReqVO.getItems(), oldItems,
                    ErpSalePriceAdjustSaveReqVO.Item.class, ErpSalePriceAdjustSaveReqVO.Item::getId,
                    ErpSalePriceAdjustSaveReqVO.Item::setId, ErpSalePriceAdjustSaveReqVO.Item::getOperation,
                    ErpSalePriceAdjustItemDO::getId, SALE_PRICE_ADJUST_ITEM_OPERATION_INVALID,
                    SALE_PRICE_ADJUST_ITEM_UPDATE_NOT_EXISTS);
            itemReqs = itemChangeSet.getFinalItems();
        }
        List<ErpSalePriceAdjustItemDO> items = buildDraftItems(itemReqs);
        ErpSalePriceAdjustDO updateDO = BeanUtils.toBean(updateReqVO, ErpSalePriceAdjustDO.class);
        updateDO.setId(existDO.getId());
        updateDO.setNo(existDO.getNo());
        updateDO.setStatus(existDO.getStatus());
        updateDO.setAdjustDate(existDO.getAdjustDate());
        if (updateDO.getDeptId() == null) {
            updateDO.setDeptId(existDO.getDeptId());
        }
        calculateDraftTotal(updateDO, items);
        salePriceAdjustMapper.updateById(updateDO);
        if (incrementalItems) {
            applySalePriceAdjustItemChangeSet(existDO.getId(), itemChangeSet, items);
        } else {
            salePriceAdjustItemMapper.deleteByAdjustId(existDO.getId());
            insertDraftItems(existDO.getId(), items);
        }
        recordUpdate(existDO.getId(), existDO.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSalePriceAdjust(Long id) {
        ErpSalePriceAdjustDO adjustDO = validateSalePriceAdjustExists(id);
        if (!ErpSalePriceAdjustStatusEnum.DRAFT.getStatus().equals(adjustDO.getStatus())) {
            throw exception(SALE_PRICE_ADJUST_DRAFT_SUBMIT_FAIL);
        }
        List<ErpSalePriceAdjustItemDO> items = salePriceAdjustItemMapper.selectListByAdjustId(id);
        validateFormalSubmit(adjustDO, items);
        fillItemSnapshotsFromSaleOutItems(items);
        validateSalePriceAdjustItemsNotAdjusted(items, id);
        int updateCount = salePriceAdjustMapper.updateByIdAndStatus(id,
                ErpSalePriceAdjustStatusEnum.DRAFT.getStatus(),
                new ErpSalePriceAdjustDO().setStatus(ErpSalePriceAdjustStatusEnum.PROCESS.getStatus()));
        if (updateCount == 0) {
            throw exception(SALE_PRICE_ADJUST_DRAFT_SUBMIT_FAIL);
        }
        recordUpdate(id, adjustDO.getNo());
    }

    @Override
    public void updateSalePriceAdjustRemark(ErpSaleUpdateRemarkReqVO updateReqVO) {
        ErpSalePriceAdjustDO existDO = validateSalePriceAdjustExists(updateReqVO.getId());
        salePriceAdjustMapper.updateById(new ErpSalePriceAdjustDO()
                .setId(updateReqVO.getId())
                .setRemark(updateReqVO.getRemark()));
        recordUpdate(updateReqVO.getId(), existDO.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSalePriceAdjustStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(SALE_PRICE_ADJUST_PROCESS_FAIL);
        }
        ErpSalePriceAdjustDO existDO = validateSalePriceAdjustExists(id);
        if (!ErpAuditStatus.PROCESS.getStatus().equals(existDO.getStatus())) {
            throw exception(SALE_PRICE_ADJUST_APPROVE_FAIL);
        }
        // 旧调价直接追加零数量旧流水，尚未接入新账；必须在任何来源及流水写入之前拒绝。
        if (dualCostEnabled) {
            throw new ServiceException(409, "销售调价尚未接入新核算，暂不能审核；请保留草稿，待调价核算接入后处理");
        }
        approveAndModifySaleOut(existDO);
        recordStatus(id, existDO.getNo(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSalePriceAdjust(List<Long> ids) {
        for (Long id : ids) {
            ErpSalePriceAdjustDO existDO = validateSalePriceAdjustExists(id);
            if (ErpAuditStatus.APPROVE.getStatus().equals(existDO.getStatus())) {
                throw exception(SALE_PRICE_ADJUST_DELETE_FAIL_APPROVE);
            }
            salePriceAdjustMapper.deleteById(id);
            salePriceAdjustItemMapper.deleteByAdjustId(id);
            recordDelete(id, existDO.getNo());
        }
    }

    @Override
    public ErpSalePriceAdjustDO getSalePriceAdjust(Long id) {
        return salePriceAdjustMapper.selectById(id);
    }

    @Override
    public ErpSalePriceAdjustDO validateSalePriceAdjust(Long id) {
        ErpSalePriceAdjustDO adjust = validateSalePriceAdjustExists(id);
        if (!ErpAuditStatus.APPROVE.getStatus().equals(adjust.getStatus())) {
            throw exception(SALE_PRICE_ADJUST_APPROVE_FAIL);
        }
        return adjust;
    }

    @Override
    public void updateSalePriceAdjustReceiptPrice(Long id, BigDecimal receiptPrice) {
        // 销售调价单当前没有独立已收字段，收款单明细本身记录汇总结果。
    }

    @Override
    public PageResult<ErpSalePriceAdjustDO> getSalePriceAdjustPage(ErpSalePriceAdjustPageReqVO pageReqVO) {
        PageResult<ErpSalePriceAdjustDO> pageResult = salePriceAdjustMapper.selectPage(pageReqVO);
        Map<Long, BigDecimal> receiptPriceMap = financeReceiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(
                convertSet(pageResult.getList(), ErpSalePriceAdjustDO::getId),
                ErpBizTypeEnum.SALE_PRICE_ADJUST.getType());
        pageResult.getList().forEach(item -> item.setReceiptPrice(
                receiptPriceMap.getOrDefault(item.getId(), BigDecimal.ZERO)));
        return pageResult;
    }

    @Override
    public List<ErpSalePriceAdjustItemDO> getSalePriceAdjustItemListByAdjustId(Long adjustId) {
        return salePriceAdjustItemMapper.selectListByAdjustId(adjustId);
    }

    @Override
    public PageResult<ErpSalePriceAdjustItemDO> getSalePriceAdjustItemPage(ErpSalePriceAdjustItemPageReqVO pageReqVO) {
        validateSalePriceAdjustExists(pageReqVO.getAdjustId());
        return salePriceAdjustItemMapper.selectPageByAdjustId(pageReqVO);
    }

    @Override
    public List<ErpSalePriceAdjustItemDO> getSalePriceAdjustItemListByAdjustIds(Collection<Long> adjustIds) {
        return salePriceAdjustItemMapper.selectListByAdjustIds(adjustIds);
    }

    @Override
    public List<ErpSaleOutItemForAdjustRespVO> getAdjustableItemsByCustomerId(Long customerId, Long saleOutId,
                                                                              Boolean excludeAdjusted) {
        List<ErpSaleOutDO> saleOuts = saleOutMapper.selectList(
                new LambdaQueryWrapper<ErpSaleOutDO>()
                        .eq(ErpSaleOutDO::getCustomerId, customerId)
                        .eq(saleOutId != null, ErpSaleOutDO::getId, saleOutId)
                        .eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                        .orderByDesc(ErpSaleOutDO::getOutTime));
        if (CollUtil.isEmpty(saleOuts)) {
            return Collections.emptyList();
        }
        List<Long> outIds = saleOuts.stream().map(ErpSaleOutDO::getId).collect(Collectors.toList());
        List<ErpSaleOutItemDO> allItems = saleOutItemMapper.selectListByOutIds(outIds);
        if (Boolean.TRUE.equals(excludeAdjusted) || excludeAdjusted == null) {
            allItems = allItems.stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getAdjusted()))
                    .collect(Collectors.toList());
        }
        if (CollUtil.isEmpty(allItems)) {
            return Collections.emptyList();
        }
        return buildSaleOutItemForAdjustVOList(allItems, convertMap(saleOuts, ErpSaleOutDO::getId));
    }

    @Override
    public PageResult<ErpSaleOutItemForAdjustRespVO> getAdjustableItemPage(
            ErpSalePriceAdjustableItemPageReqVO pageReqVO) {
        if (pageReqVO.getCustomerId() == null) {
            return PageResult.empty();
        }
        if (pageReqVO.getExcludeAdjusted() == null) {
            pageReqVO.setExcludeAdjusted(true);
        }
        PageResult<ErpSaleOutItemDO> pageResult = saleOutItemMapper.selectAdjustableItemPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Set<Long> outIds = convertSet(pageResult.getList(), ErpSaleOutItemDO::getOutId);
        Map<Long, ErpSaleOutDO> outMap = convertMap(saleOutMapper.selectBatchIds(outIds), ErpSaleOutDO::getId);
        return new PageResult<>(buildSaleOutItemForAdjustVOList(pageResult.getList(), outMap), pageResult.getTotal());
    }

    private List<ErpSaleOutItemForAdjustRespVO> buildSaleOutItemForAdjustVOList(
            List<ErpSaleOutItemDO> items, Map<Long, ErpSaleOutDO> outMap) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        Set<Long> productIds = items.stream().map(ErpSaleOutItemDO::getProductId).collect(Collectors.toSet());
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductVOMap(productIds));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(items, ErpSaleOutItemDO::getWarehouseId)));
        Set<Long> warehouseDeptIds = convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId);
        warehouseDeptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(warehouseDeptIds)
                ? Collections.emptyMap() : deptApi.getDeptMap(warehouseDeptIds);
        List<ErpSaleOutItemForAdjustRespVO> result = new ArrayList<>();
        for (ErpSaleOutItemDO item : items) {
            ErpSaleOutDO out = outMap.get(item.getOutId());
            if (out == null) continue;
            ErpSaleOutItemForAdjustRespVO vo = new ErpSaleOutItemForAdjustRespVO();
            vo.setSaleOutId(out.getId());
            vo.setSaleOutNo(out.getNo());
            vo.setCustomerId(out.getCustomerId());
            vo.setOutTime(out.getOutTime());
            vo.setProductId(item.getProductId());
            vo.setSaleOutItemId(item.getId());
            vo.setCount(item.getCount());
            vo.setProductPrice(item.getProductPrice());
            vo.setAdjusted(item.getAdjusted());
            vo.setDeptId(item.getDeptId());
            vo.setWeight(item.getUnitWeight());
            vo.setPackageQty(item.getPackageQty());
            vo.setWarehouseId(item.getWarehouseId());
            vo.setBatchNo(item.getBatchNo());
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
                vo.setWarehouseDeptId(warehouse.getDeptId());
                MapUtils.findAndThen(deptMap, warehouse.getDeptId(),
                        dept -> vo.setWarehouseDeptName(dept.getName()));
            }
            ErpProductRespVO product = productMap.get(item.getProductId());
            if (product != null) {
                vo.setProductCode(product.getCode());
                vo.setProductName(product.getName());
                vo.setVehicleModel(product.getVehicleModel());
                vo.setOriginPlace(product.getOriginPlace());
                vo.setBrand(product.getBrand());
                vo.setUnitName(product.getUnitName());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public ErpSalePriceAdjustImportRespVO importSalePriceAdjustItems(List<ErpSalePriceAdjustImportExcelVO> list) {
        ErpSalePriceAdjustImportRespVO respVO = new ErpSalePriceAdjustImportRespVO();
        if (CollUtil.isEmpty(list)) {
            return respVO;
        }

        ErpImportProductResolver productResolver = ErpImportProductResolver.build(list,
                ErpSalePriceAdjustImportExcelVO::getProductCode, ErpSalePriceAdjustImportExcelVO::getProductName,
                ErpSalePriceAdjustImportExcelVO::getFactoryCode, productMapper);
        Map<Long, ErpProductRespVO> productVOMap = CollUtil.isEmpty(productResolver.getResolvedProducts())
                ? new HashMap<>()
                : DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(productResolver.getResolvedProducts().stream()
                .map(ErpProductDO::getId).collect(Collectors.toSet())));
        Map<String, ErpWarehouseDO> warehouseMap = warehouseService.getCurrentUserVisibleSaleWarehouseList().stream()
                .collect(Collectors.toMap(item -> normalizeKey(item.getName()), item -> item, (a, b) -> a));

        Long importCustomerId = null;
        Set<String> usedKeys = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            ErpSalePriceAdjustImportExcelVO row = list.get(i);
            int rowNo = i + 2;
            try {
                if (row == null || isEmptyImportRow(row)) {
                    continue;
                }
                String saleOutNo = trimToNull(row.getSaleOutNo());
                if (saleOutNo == null) {
                    throw new IllegalArgumentException("销售单号不能为空");
                }
                ErpImportProductResolver.ResolveResult productResult =
                        productResolver.resolve(row.getProductCode(), row.getProductName(), row.getFactoryCode());
                if (productResult.isFailure()) {
                    throw new IllegalArgumentException(productResult.getErrorMessage());
                }
                String warehouseName = trimToNull(row.getWarehouseName());
                if (warehouseName == null) {
                    throw new IllegalArgumentException("所属仓库不能为空");
                }
                ErpWarehouseDO warehouse = warehouseMap.get(normalizeKey(warehouseName));
                if (warehouse == null) {
                    throw new IllegalArgumentException("所属仓库不存在：" + row.getWarehouseName());
                }
                if (row.getNewPrice() == null || row.getNewPrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("调后价不能小于 0");
                }

                ErpSaleOutDO saleOut = saleOutMapper.selectByNo(saleOutNo);
                if (saleOut == null) {
                    throw new IllegalArgumentException("销售单不存在");
                }
                Long customerId = saleOut.getCustomerId();
                if (customerId == null) {
                    throw new IllegalArgumentException("销售单未关联客户");
                }
                if (importCustomerId == null) {
                    importCustomerId = customerId;
                    respVO.setCustomerId(customerId);
                } else if (!importCustomerId.equals(customerId)) {
                    throw new IllegalArgumentException("导入文件中销售单客户必须保持一致");
                }
                ErpProductDO product = productResult.getProduct();

                List<ErpSaleOutItemDO> matchedItems = saleOutItemMapper.selectListByOutId(saleOut.getId()).stream()
                        .filter(item -> product.getId().equals(item.getProductId()))
                        .filter(item -> warehouse.getId().equals(item.getWarehouseId()))
                        .collect(Collectors.toList());
                if (matchedItems.isEmpty()) {
                    throw new IllegalArgumentException("销售单中不存在该产品或所属仓库不匹配");
                }
                if (matchedItems.size() > 1) {
                    throw new IllegalArgumentException("销售单中该产品和所属仓库存在多条明细，暂不支持导入，请手动选择");
                }

                ErpSaleOutItemDO outItem = matchedItems.get(0);
                String uniqueKey = saleOut.getId() + "_" + outItem.getId();
                if (!usedKeys.add(uniqueKey)) {
                    throw new IllegalArgumentException("存在重复导入的销售明细");
                }
                validateSalePriceAdjustItemsNotAdjusted(Collections.singletonList(new ErpSalePriceAdjustItemDO()
                        .setSaleOutNo(saleOut.getNo())
                        .setSaleOutId(saleOut.getId())
                        .setSaleOutItemId(outItem.getId())
                        .setProductId(product.getId())
                        .setPartCode(product.getCode())
                        .setPartName(product.getName())), null);

                ErpProductRespVO productVO = productVOMap.get(product.getId());
                ErpSalePriceAdjustSaveReqVO.Item item = new ErpSalePriceAdjustSaveReqVO.Item();
                item.setSaleOutId(saleOut.getId());
                item.setSaleOutItemId(outItem.getId());
                item.setSaleOutNo(saleOut.getNo());
                item.setProductId(product.getId());
                item.setPartCode(product.getCode());
                item.setPartName(product.getName());
                item.setUnit(productVO != null ? productVO.getUnitName() : null);
                item.setBrand(outItem.getBrand());
                item.setVehicleModel(outItem.getVehicleModel());
                item.setOriginPlace(outItem.getOriginPlace());
                item.setWeight(outItem.getUnitWeight());
                item.setPackageQty(outItem.getPackageQty());
                item.setDeptId(outItem.getDeptId());
                item.setWarehouseId(outItem.getWarehouseId());
                item.setWarehouseName(warehouse.getName());
                item.setWarehouseDeptId(warehouse.getDeptId());
                item.setBatchNo(outItem.getBatchNo());
                item.setOutCount(outItem.getCount());
                item.setOldPrice(outItem.getProductPrice());
                item.setNewPrice(row.getNewPrice());
                item.setAdjustReason(trimToNull(row.getAdjustReason()));
                item.setItemRemark(trimToNull(row.getItemRemark()));
                respVO.getItems().add(item);
                respVO.setSuccessCount(respVO.getSuccessCount() + 1);
            } catch (Exception ex) {
                respVO.getFailureDetails().add(new ErpSalePriceAdjustImportRespVO.FailureItem(
                        rowNo,
                        row != null ? ErpImportProductResolver.getIdentifier(row.getProductCode(), row.getProductName(), row.getFactoryCode()) : null,
                        ex.getMessage()));
                respVO.setFailureCount(respVO.getFailureCount() + 1);
            }
        }
        return respVO;
    }

    private void approveAndModifySaleOut(ErpSalePriceAdjustDO adjustDO) {
        List<ErpSalePriceAdjustItemDO> adjustItems = salePriceAdjustItemMapper.selectListByAdjustId(adjustDO.getId());
        if (CollUtil.isEmpty(adjustItems)) {
            throw exception(SALE_PRICE_ADJUST_APPROVE_FAIL);
        }
        validateSalePriceAdjustItemsNotAdjusted(adjustItems, adjustDO.getId());
        LocalDateTime approveTime = LocalDateTime.now();
        // 按 saleOutNo 分组处理多张销售单
        Map<String, List<ErpSalePriceAdjustItemDO>> groupBySaleOutNo = adjustItems.stream()
                .collect(Collectors.groupingBy(ErpSalePriceAdjustItemDO::getSaleOutNo));

        // 与销售编辑/审核/退货共用父单锁，按真实单ID排序，不能按HashMap迭代顺序加锁。
        Map<Long,String> sourceNos = new TreeMap<>();
        for (String no : groupBySaleOutNo.keySet()) {
            ErpSaleOutDO source = saleOutMapper.selectByNo(no);
            if (source == null) throw exception(SALE_OUT_NOT_APPROVE);
            sourceNos.put(source.getId(), no);
        }
        Map<Long,ErpSaleOutDO> lockedSources = new LinkedHashMap<>();
        for (Long sourceId : sourceNos.keySet()) {
            ErpSaleOutDO source = saleOutMapper.selectByIdForUpdate(sourceId);
            if (source == null || !ErpAuditStatus.APPROVE.getStatus().equals(source.getStatus())) throw exception(SALE_OUT_NOT_APPROVE);
            lockedSources.put(sourceId, source);
        }
        validateSalePriceAdjustItemsNotAdjusted(adjustItems, adjustDO.getId());
        for (Map.Entry<Long,ErpSaleOutDO> entry : lockedSources.entrySet()) {
            List<ErpSalePriceAdjustItemDO> groupItems = groupBySaleOutNo.get(sourceNos.get(entry.getKey()));
            ErpSaleOutDO originalOut = entry.getValue();
            if (originalOut == null || !ErpAuditStatus.APPROVE.getStatus().equals(originalOut.getStatus())) {
                throw exception(SALE_OUT_NOT_APPROVE);
            }

            List<ErpSaleOutItemDO> originalItems = saleOutItemMapper.selectListByOutIdForUpdate(originalOut.getId());
            Map<Long, ErpSalePriceAdjustItemDO> adjustItemMap = convertMap(groupItems, ErpSalePriceAdjustItemDO::getSaleOutItemId);

            for (ErpSaleOutItemDO item : originalItems) {
                ErpSalePriceAdjustItemDO adjustItem = adjustItemMap.get(item.getId());
                if (adjustItem != null) {
                    if (item.getOriginalProductPrice() == null) {
                        item.setOriginalProductPrice(item.getProductPrice());
                    }
                    item.setProductPrice(adjustItem.getNewPrice());
                    item.setAdjusted(true);
                    item.setAdjustId(adjustDO.getId());
                    recalculateSaleOutItem(item);
                    saleOutItemMapper.updateById(item);
                    createSalePriceAdjustStockRecord(adjustDO, adjustItem, item, approveTime);
                }
            }

            List<ErpSaleOutItemDO> updatedItems = saleOutItemMapper.selectListByOutId(originalOut.getId());
            recalculateSaleOut(originalOut, updatedItems);
            originalOut.setAdjusted(true);
            originalOut.setAdjustPriceAdjustId(adjustDO.getId());
            saleOutMapper.updateById(originalOut);
        }

        // 更新调价单状态
        salePriceAdjustMapper.updateById(new ErpSalePriceAdjustDO().setId(adjustDO.getId())
                .setStatus(ErpAuditStatus.APPROVE.getStatus()));
    }

    private void validateSalePriceAdjustItemsNotAdjusted(List<ErpSalePriceAdjustItemDO> adjustItems, Long currentAdjustId) {
        if (CollUtil.isEmpty(adjustItems)) {
            return;
        }
        Set<Long> saleOutItemIds = new HashSet<>();
        for (ErpSalePriceAdjustItemDO item : adjustItems) {
            Long saleOutItemId = item.getSaleOutItemId();
            if (saleOutItemId == null) {
                continue;
            }
            if (!saleOutItemIds.add(saleOutItemId)) {
                throw exception(SALE_PRICE_ADJUST_ITEM_DUPLICATE, buildAdjustItemLabel(item));
            }
        }
        if (CollUtil.isEmpty(saleOutItemIds)) {
            return;
        }

        List<ErpSaleOutItemDO> saleOutItems = saleOutItemMapper.selectListByIds(saleOutItemIds);
        if (CollUtil.isNotEmpty(saleOutItems)) {
            for (ErpSaleOutItemDO saleOutItem : saleOutItems) {
                if (Boolean.TRUE.equals(saleOutItem.getAdjusted())
                        && (currentAdjustId == null || !currentAdjustId.equals(saleOutItem.getAdjustId()))) {
                    ErpSalePriceAdjustItemDO adjustItem = findAdjustItem(adjustItems, saleOutItem.getId());
                    throw exception(SALE_PRICE_ADJUST_ITEM_ADJUSTED, buildAdjustItemLabel(adjustItem));
                }
            }
        }

        List<ErpSalePriceAdjustItemDO> existedItems = salePriceAdjustItemMapper.selectListBySaleOutItemIds(saleOutItemIds);
        if (currentAdjustId != null && CollUtil.isNotEmpty(existedItems)) {
            existedItems = existedItems.stream()
                    .filter(item -> !currentAdjustId.equals(item.getAdjustId()))
                    .collect(Collectors.toList());
        }
        if (CollUtil.isEmpty(existedItems)) {
            return;
        }
        Set<Long> existedAdjustIds = existedItems.stream()
                .map(ErpSalePriceAdjustItemDO::getAdjustId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(existedAdjustIds)) {
            return;
        }
        List<ErpSalePriceAdjustDO> approvedAdjusts = salePriceAdjustMapper.selectList(
                new LambdaQueryWrapper<ErpSalePriceAdjustDO>()
                        .in(ErpSalePriceAdjustDO::getId, existedAdjustIds)
                        .eq(ErpSalePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus()));
        if (CollUtil.isEmpty(approvedAdjusts)) {
            return;
        }
        Set<Long> approvedAdjustIds = approvedAdjusts.stream()
                .map(ErpSalePriceAdjustDO::getId)
                .collect(Collectors.toSet());
        for (ErpSalePriceAdjustItemDO existedItem : existedItems) {
            if (approvedAdjustIds.contains(existedItem.getAdjustId())) {
                ErpSalePriceAdjustItemDO adjustItem = findAdjustItem(adjustItems, existedItem.getSaleOutItemId());
                throw exception(SALE_PRICE_ADJUST_ITEM_ADJUSTED, buildAdjustItemLabel(adjustItem));
            }
        }
    }

    private void fillItemSnapshotsFromSaleOutItems(List<ErpSalePriceAdjustItemDO> items) {
        Set<Long> saleOutItemIds = convertSet(items, ErpSalePriceAdjustItemDO::getSaleOutItemId);
        if (CollUtil.isEmpty(saleOutItemIds)) {
            return;
        }
        Map<Long, ErpSaleOutItemDO> saleOutItemMap = convertMap(
                saleOutItemMapper.selectListByIds(saleOutItemIds), ErpSaleOutItemDO::getId);
        for (ErpSalePriceAdjustItemDO item : items) {
            if (item.getDeptId() == null && item.getSaleOutItemId() != null) {
                ErpSaleOutItemDO saleOutItem = saleOutItemMap.get(item.getSaleOutItemId());
                if (saleOutItem != null) {
                    item.setDeptId(saleOutItem.getDeptId());
                }
            }
            ErpSaleOutItemDO saleOutItem = item.getSaleOutItemId() == null ? null : saleOutItemMap.get(item.getSaleOutItemId());
            if (saleOutItem == null) {
                continue;
            }
            if (item.getWeight() == null) {
                item.setWeight(saleOutItem.getUnitWeight());
            }
            if (item.getPackageQty() == null) {
                item.setPackageQty(saleOutItem.getPackageQty());
            }
        }
    }

    private ErpSalePriceAdjustItemDO findAdjustItem(List<ErpSalePriceAdjustItemDO> adjustItems, Long saleOutItemId) {
        if (saleOutItemId == null || CollUtil.isEmpty(adjustItems)) {
            return null;
        }
        for (ErpSalePriceAdjustItemDO item : adjustItems) {
            if (saleOutItemId.equals(item.getSaleOutItemId())) {
                return item;
            }
        }
        return null;
    }

    private List<ErpSalePriceAdjustItemDO> buildDraftItems(List<ErpSalePriceAdjustSaveReqVO.Item> itemReqs) {
        if (CollUtil.isEmpty(itemReqs)) {
            return Collections.emptyList();
        }
        List<ErpSalePriceAdjustItemDO> items = itemReqs.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getSaleOutItemId() != null
                        && item.getProductId() != null
                        && item.getOutCount() != null)
                .map(item -> BeanUtils.toBean(item, ErpSalePriceAdjustItemDO.class))
                .collect(Collectors.toList());
        fillItemSnapshotsFromSaleOutItems(items);
        return items;
    }

    private void calculateDraftTotal(ErpSalePriceAdjustDO adjustDO, List<ErpSalePriceAdjustItemDO> items) {
        BigDecimal totalAdjustPrice = BigDecimal.ZERO;
        for (ErpSalePriceAdjustItemDO item : items) {
            BigDecimal adjustPrice = calculateAdjustPrice(item);
            item.setAdjustPrice(adjustPrice);
            totalAdjustPrice = totalAdjustPrice.add(adjustPrice);
        }
        adjustDO.setTotalAdjustPrice(totalAdjustPrice);
    }

    private void insertDraftItems(Long adjustId, List<ErpSalePriceAdjustItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> {
            item.setId(null);
            item.setAdjustId(adjustId);
        });
        salePriceAdjustItemMapper.insertBatch(items);
    }

    private void applySalePriceAdjustItemChangeSet(Long adjustId,
            ErpSaleItemOperationHelper.RequestChangeSet<ErpSalePriceAdjustSaveReqVO.Item> changeSet,
            List<ErpSalePriceAdjustItemDO> finalItems) {
        if (CollUtil.isNotEmpty(changeSet.getDeleteIds())) {
            salePriceAdjustItemMapper.deleteByIds(changeSet.getDeleteIds());
        }
        List<ErpSalePriceAdjustItemDO> insertList = finalItems.stream()
                .filter(item -> item.getId() == null)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(insertList)) {
            insertList.forEach(item -> item.setId(null).setAdjustId(adjustId));
            salePriceAdjustItemMapper.insertBatch(insertList);
        }
        List<ErpSalePriceAdjustItemDO> updateList = finalItems.stream()
                .filter(item -> item.getId() != null && changeSet.getUpdateIds().contains(item.getId()))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(updateList)) {
            updateList.forEach(item -> item.setAdjustId(adjustId));
            salePriceAdjustItemMapper.updateBatch(updateList);
        }
    }

    private void validateFormalSubmit(ErpSalePriceAdjustDO adjustDO, List<ErpSalePriceAdjustItemDO> items) {
        if (adjustDO.getCustomerId() == null) {
            throw exception(SALE_PRICE_ADJUST_SUBMIT_CUSTOMER_REQUIRED);
        }
        if (adjustDO.getAdjustUserId() == null) {
            throw exception(SALE_PRICE_ADJUST_SUBMIT_USER_REQUIRED);
        }
        if (StrUtil.isBlank(adjustDO.getSettleMethod())) {
            throw exception(SALE_PRICE_ADJUST_SUBMIT_SETTLE_METHOD_REQUIRED);
        }
        if (StrUtil.isBlank(adjustDO.getDeliveryMethod())) {
            throw exception(SALE_PRICE_ADJUST_SUBMIT_DELIVERY_METHOD_REQUIRED);
        }
        if (CollUtil.isEmpty(items)) {
            throw exception(SALE_PRICE_ADJUST_SUBMIT_ITEMS_REQUIRED);
        }
        boolean invalidItem = items.stream().anyMatch(item -> item == null
                || item.getSaleOutId() == null
                || item.getSaleOutItemId() == null
                || item.getProductId() == null
                || item.getOutCount() == null
                || item.getOldPrice() == null
                || item.getNewPrice() == null);
        if (invalidItem) {
            throw exception(SALE_PRICE_ADJUST_SUBMIT_ITEMS_REQUIRED);
        }
    }

    private String buildAdjustItemLabel(ErpSalePriceAdjustItemDO item) {
        if (item == null) {
            return "-";
        }
        String productName = trimToNull(item.getPartName());
        String productCode = trimToNull(item.getPartCode());
        if (productName != null && productCode != null) {
            return productName + "/" + productCode + "#" + item.getSaleOutItemId();
        }
        if (productName != null) {
            return productName + "#" + item.getSaleOutItemId();
        }
        if (productCode != null) {
            return productCode + "#" + item.getSaleOutItemId();
        }
        return String.valueOf(item.getSaleOutItemId());
    }

    private ErpSalePriceAdjustDO validateSalePriceAdjustExists(Long id) {
        ErpSalePriceAdjustDO adjustDO = salePriceAdjustMapper.selectById(id);
        if (adjustDO == null) {
            throw exception(SALE_PRICE_ADJUST_NOT_EXISTS);
        }
        return adjustDO;
    }

    private BigDecimal calculateAdjustPrice(ErpSalePriceAdjustItemDO item) {
        if (item.getNewPrice() == null || item.getOldPrice() == null || item.getOutCount() == null) {
            return BigDecimal.ZERO;
        }
        return item.getNewPrice().subtract(item.getOldPrice()).multiply(item.getOutCount());
    }

    private void createSalePriceAdjustStockRecord(ErpSalePriceAdjustDO adjustDO,
                                                 ErpSalePriceAdjustItemDO adjustItem,
                                                 ErpSaleOutItemDO saleOutItem,
                                                 LocalDateTime approveTime) {
        BigDecimal adjustPrice = calculateAdjustPrice(adjustItem);
        if (adjustPrice.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                stockMapper.selectByProductIdAndWarehouseId(saleOutItem.getProductId(), saleOutItem.getWarehouseId()));
        BigDecimal totalCount = stock != null && stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
        BigDecimal costPrice = stock != null && stock.getCostPrice() != null ? stock.getCostPrice() : BigDecimal.ZERO;
        BigDecimal costAmount = stock != null && stock.getCostAmount() != null ? stock.getCostAmount() : BigDecimal.ZERO;

        ErpStockRecordDO record = new ErpStockRecordDO()
                .setProductId(saleOutItem.getProductId())
                .setWarehouseId(saleOutItem.getWarehouseId())
                .setDeptId(stock != null ? stock.getDeptId() : null)
                .setCount(BigDecimal.ZERO)
                .setTotalCount(totalCount)
                .setBizType(ErpStockRecordBizTypeEnum.SALE_PRICE_ADJUST.getType())
                .setBizId(adjustDO.getId())
                .setBizItemId(adjustItem.getId())
                .setBizNo(adjustDO.getNo())
                .setUnitPrice(adjustItem.getNewPrice())
                .setTotalPrice(adjustPrice)
                .setCostPrice(costPrice)
                .setCostAmount(costAmount)
                .setBizDate(approveTime != null ? approveTime : LocalDateTime.now());
        stockRecordMapper.insert(record);
    }

    private boolean isEmptyImportRow(ErpSalePriceAdjustImportExcelVO row) {
        return row == null
                || StrUtil.isAllBlank(row.getSaleOutNo(), row.getProductCode(), row.getProductName(),
                row.getFactoryCode(),
                row.getWarehouseName(), row.getAdjustReason(), row.getItemRemark())
                && row.getNewPrice() == null;
    }

    private String normalizeKey(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isEmpty(trimmed) ? null : trimmed;
    }

    private void recalculateSaleOutItem(ErpSaleOutItemDO item) {
        BigDecimal count = item.getCount() == null ? BigDecimal.ZERO : item.getCount();
        BigDecimal price = item.getProductPrice() == null ? BigDecimal.ZERO : item.getProductPrice();
        BigDecimal totalPrice = price.multiply(count);
        item.setTotalPrice(totalPrice);
        item.setTaxPercent(null); item.setTaxPrice(BigDecimal.ZERO);
    }

    private void recalculateSaleOut(ErpSaleOutDO saleOut, List<ErpSaleOutItemDO> items) {
        BigDecimal totalCount = BigDecimal.ZERO;
        BigDecimal totalProductPrice = BigDecimal.ZERO;
        BigDecimal totalTaxPrice = BigDecimal.ZERO;
        for (ErpSaleOutItemDO item : items) {
            totalCount = totalCount.add(item.getCount() == null ? BigDecimal.ZERO : item.getCount());
            totalProductPrice = totalProductPrice.add(item.getTotalPrice() == null ? BigDecimal.ZERO : item.getTotalPrice());
        }
        BigDecimal discountPercent = saleOut.getDiscountPercent() == null ? BigDecimal.ZERO : saleOut.getDiscountPercent();
        BigDecimal discountPrice = totalProductPrice.multiply(discountPercent).divide(new BigDecimal("100"));
        BigDecimal feeAmount = saleOut.getFeeAmount() != null ? saleOut.getFeeAmount()
                : (saleOut.getOtherPrice() != null ? saleOut.getOtherPrice() : BigDecimal.ZERO);
        saleOut.setTotalCount(totalCount);
        saleOut.setTotalProductPrice(totalProductPrice);
        saleOut.setTotalTaxPrice(totalTaxPrice);
        saleOut.setDiscountPrice(discountPrice);
        saleOut.setFeeAmount(feeAmount);
        saleOut.setOtherPrice(feeAmount);
        saleOut.setExtraFee(feeAmount);
        saleOut.setTotalPrice(totalProductPrice.subtract(discountPrice).add(feeAmount));
    }

    private void recordCreate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordCreate(ERP_SALE_PRICE_ADJUST_TYPE, id, no);
        }
    }

    private void recordUpdate(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordUpdate(ERP_SALE_PRICE_ADJUST_TYPE, id, no);
        }
    }

    private void recordDelete(Long id, String no) {
        if (operateLogService != null) {
            operateLogService.recordDelete(ERP_SALE_PRICE_ADJUST_TYPE, id, no);
        }
    }

    private void recordStatus(Long id, String no, boolean approve) {
        if (operateLogService != null) {
            operateLogService.recordStatus(ERP_SALE_PRICE_ADJUST_TYPE, id, no, approve);
        }
    }

}
