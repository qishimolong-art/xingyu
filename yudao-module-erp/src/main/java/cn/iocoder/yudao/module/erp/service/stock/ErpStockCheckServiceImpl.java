package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockAdjustReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockCheckTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockCheckStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_STOCK_CHECK_TYPE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

// TODO 鑺嬭壙锛氳褰曟搷浣滄棩蹇?
/**
 * ERP 搴撳瓨鐩樼偣鍗?Service 瀹炵幇绫? *
 * @author 鑺嬮亾婧愮爜
 */
@Service
@Validated
public class ErpStockCheckServiceImpl implements ErpStockCheckService {

    private static final String FIELD_PERMISSION_MODULE = "erp_stock_check";

    @Resource
    private ErpStockCheckMapper stockCheckMapper;
    @Resource
    private ErpStockCheckItemMapper stockCheckItemMapper;
    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockItemSnapshotSupport snapshotSupport;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpStockItemBatchUpdateSupport batchUpdateSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockCheck(ErpStockCheckSaveReqVO createReqVO) {
        Integer checkType = ErpStockCheckTypeEnum.defaultIfNull(createReqVO.getCheckType());
        createReqVO.setCheckType(checkType);
        List<ErpStockCheckItemDO> stockCheckItems = validateStockCheckItems(createReqVO.getItems(), checkType);
        String no = noRedisDAO.generate(ErpNoRedisDAO.STOCK_CHECK_NO_PREFIX);
        if (stockCheckMapper.selectByNo(no) != null) {
            throw exception(STOCK_CHECK_NO_EXISTS);
        }
        if (createReqVO.getDeptId() == null) {
            createReqVO.setDeptId(getLoginUserDeptId());
        }

        ErpStockCheckDO stockCheck = BeanUtils.toBean(createReqVO, ErpStockCheckDO.class, in -> in
                .setNo(no).setStatus(ErpStockCheckStatusEnum.PROCESS.getStatus())
                .setTotalCount(getSumValue(stockCheckItems, ErpStockCheckItemDO::getCount, BigDecimal::add))
                .setTotalPrice(getSumValue(stockCheckItems, ErpStockCheckItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO)));
        stockCheckMapper.insert(stockCheck);
        // 2.2 鎻掑叆鐩樼偣鍗曢」
        stockCheckItems.forEach(o -> o.setCheckId(stockCheck.getId()));
        stockCheckItemMapper.insertBatch(stockCheckItems);
        operateLogService.recordCreate(ERP_STOCK_CHECK_TYPE, stockCheck.getId(), stockCheck.getNo());
        return stockCheck.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockCheckDraft(ErpStockCheckDraftCreateReqVO createReqVO) {
        Integer checkType = ErpStockCheckTypeEnum.defaultIfNull(createReqVO.getCheckType());
        createReqVO.setCheckType(checkType);
        List<ErpStockCheckSaveReqVO.Item> itemReqs = filterDraftItems(createReqVO.getItems(), checkType);
        if (CollUtil.isEmpty(itemReqs)) {
            throw exception(STOCK_CHECK_DRAFT_ITEMS_REQUIRED);
        }
        List<ErpStockCheckItemDO> stockCheckItems = CollUtil.isEmpty(itemReqs)
                ? Collections.emptyList() : validateStockCheckItems(itemReqs, checkType);
        String no = noRedisDAO.generate(ErpNoRedisDAO.STOCK_CHECK_NO_PREFIX);
        if (stockCheckMapper.selectByNo(no) != null) {
            throw exception(STOCK_CHECK_NO_EXISTS);
        }
        if (createReqVO.getDeptId() == null) {
            createReqVO.setDeptId(getLoginUserDeptId());
        }
        ErpStockCheckDO stockCheck = BeanUtils.toBean(createReqVO, ErpStockCheckDO.class, target -> target
                .setNo(no)
                .setStatus(ErpStockCheckStatusEnum.DRAFT.getStatus())
                .setCheckTime(createReqVO.getCheckTime() != null ? createReqVO.getCheckTime() : LocalDateTime.now())
                .setTotalCount(getSumValue(stockCheckItems, ErpStockCheckItemDO::getCount,
                        BigDecimal::add, BigDecimal.ZERO))
                .setTotalPrice(getSumValue(stockCheckItems, ErpStockCheckItemDO::getTotalPrice,
                        BigDecimal::add, BigDecimal.ZERO)));
        stockCheckMapper.insert(stockCheck);
        replaceStockCheckItems(stockCheck.getId(), stockCheckItems);
        operateLogService.recordCreate(ERP_STOCK_CHECK_TYPE, stockCheck.getId(), stockCheck.getNo());
        return stockCheck.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal createAndApproveStockAdjustCheck(ErpStockAdjustReqVO reqVO) {
        ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                stockMapper.selectByProductIdAndWarehouseId(reqVO.getProductId(), reqVO.getWarehouseId()));
        BigDecimal stockCount = stock != null && stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
        BigDecimal actualCount = reqVO.getTargetCount();
        BigDecimal count = actualCount.subtract(stockCount);

        ErpProductDO product = DataPermissionUtils.executeIgnore(() -> productService.getProduct(reqVO.getProductId()));
        BigDecimal productPrice = getStockAdjustProductPrice(stock, product);
        String remark = buildStockAdjustRemark(reqVO);

        ErpStockCheckSaveReqVO.Item item = new ErpStockCheckSaveReqVO.Item();
        item.setWarehouseId(reqVO.getWarehouseId());
        item.setProductId(reqVO.getProductId());
        item.setProductPrice(productPrice);
        item.setStockCount(stockCount);
        item.setActualCount(actualCount);
        item.setCount(count);
        item.setRemark(remark);

        ErpStockCheckSaveReqVO createReqVO = new ErpStockCheckSaveReqVO();
        createReqVO.setCheckTime(LocalDateTime.now());
        createReqVO.setCheckType(ErpStockCheckTypeEnum.COUNT.getType());
        ErpWarehouseDO warehouse = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouse(reqVO.getWarehouseId()));
        createReqVO.setDeptId(warehouse != null ? warehouse.getDeptId() : null);
        createReqVO.setRemark(remark);
        createReqVO.setItems(new ArrayList<>(Collections.singletonList(item)));
        Long checkId = createStockCheck(createReqVO);
        updateStockCheckStatus(checkId, ErpStockCheckStatusEnum.APPROVE.getStatus());
        return actualCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockCheck(ErpStockCheckSaveReqVO updateReqVO) {
        // 1.1 鏍￠獙瀛樺湪
        ErpStockCheckDO stockCheck = validateStockCheckExists(updateReqVO.getId());
        if (ErpStockCheckStatusEnum.APPROVE.getStatus().equals(stockCheck.getStatus())) {
            throw exception(STOCK_CHECK_UPDATE_FAIL_APPROVE, stockCheck.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, stockCheck);
        fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(),
                stockCheckItemMapper.selectListByCheckId(updateReqVO.getId()));
        Integer checkType = ErpStockCheckTypeEnum.defaultIfNull(updateReqVO.getCheckType());
        updateReqVO.setCheckType(checkType);
        List<ErpStockCheckItemDO> stockCheckItems = validateStockCheckItems(updateReqVO.getItems(), checkType);

        ErpStockCheckDO updateObj = BeanUtils.toBean(updateReqVO, ErpStockCheckDO.class, in -> in
                .setTotalCount(getSumValue(stockCheckItems, ErpStockCheckItemDO::getCount, BigDecimal::add))
                .setTotalPrice(getSumValue(stockCheckItems, ErpStockCheckItemDO::getTotalPrice, BigDecimal::add)));
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(stockCheck.getDeptId());
        }
        stockCheckMapper.updateById(updateObj);
        // 2.2 鏇存柊鐩樼偣鍗曢」
        updateStockCheckItemList(updateReqVO.getId(), stockCheckItems);
        operateLogService.recordUpdate(ERP_STOCK_CHECK_TYPE, stockCheck.getId(), stockCheck.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStockCheckItems(ErpStockCheckItemBatchUpdateReqVO updateReqVO) {
        batchUpdateSupport.validateFieldPermission(FIELD_PERMISSION_MODULE, STOCK_CHECK_ITEM_BATCH_UPDATE_FIELD_REQUIRED);
        ErpStockCheckDO stockCheck = validateStockCheckExists(updateReqVO.getCheckId());
        if (ErpStockCheckStatusEnum.APPROVE.getStatus().equals(stockCheck.getStatus())) {
            throw exception(STOCK_CHECK_UPDATE_FAIL_APPROVE, stockCheck.getNo());
        }
        ErpWarehouseDO targetWarehouse = batchUpdateSupport.validateTargetWarehouse(updateReqVO.getWarehouseId());
        batchUpdateSupport.validateDocumentDeptAllowed(stockCheck.getDeptId(), targetWarehouse, FIELD_PERMISSION_MODULE,
                STOCK_CHECK_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);

        List<ErpStockCheckItemDO> stockCheckItems = stockCheckItemMapper.selectListByCheckId(updateReqVO.getCheckId());
        Set<Long> selectedItemIds = new LinkedHashSet<>(updateReqVO.getItemIds());
        List<ErpStockCheckItemDO> selectedItems = stockCheckItems.stream()
                .filter(item -> selectedItemIds.contains(item.getId()))
                .collect(java.util.stream.Collectors.toList());
        if (selectedItems.size() != selectedItemIds.size()) {
            throw exception(STOCK_CHECK_ITEM_BATCH_UPDATE_ITEM_NOT_EXISTS);
        }
        validateBatchUpdateStockCheckNoDuplicate(stockCheckItems, selectedItemIds, targetWarehouse.getId());

        for (ErpStockCheckItemDO item : selectedItems) {
            item.setWarehouseId(targetWarehouse.getId());
            item.setBatchNo(null);
            item.setStockCount(stockService.getStockCount(item.getProductId(), targetWarehouse.getId()));
        }
        Integer checkType = ErpStockCheckTypeEnum.defaultIfNull(stockCheck.getCheckType());
        List<ErpStockCheckItemDO> validatedItems = validateStockCheckItems(
                BeanUtils.toBean(stockCheckItems, ErpStockCheckSaveReqVO.Item.class), checkType);
        Map<Long, ErpStockCheckItemDO> validatedItemMap = convertMap(validatedItems, ErpStockCheckItemDO::getId);
        List<ErpStockCheckItemDO> updateItems = selectedItems.stream()
                .map(item -> validatedItemMap.get(item.getId()))
                .collect(java.util.stream.Collectors.toList());
        stockCheckMapper.updateById(new ErpStockCheckDO()
                .setId(stockCheck.getId())
                .setTotalCount(getSumValue(validatedItems, ErpStockCheckItemDO::getCount, BigDecimal::add, BigDecimal.ZERO))
                .setTotalPrice(getSumValue(validatedItems, ErpStockCheckItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO)));
        stockCheckItemMapper.updateBatch(updateItems);
        operateLogService.recordUpdate(ERP_STOCK_CHECK_TYPE, stockCheck.getId(), stockCheck.getNo());
    }

    @Override
    public List<DeptSimpleRespVO> getWarehouseDeptSimpleList(Long warehouseId) {
        return batchUpdateSupport.getWarehouseAvailableDeptSimpleList(warehouseId, FIELD_PERMISSION_MODULE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockCheckDraft(ErpStockCheckDraftUpdateReqVO updateReqVO) {
        ErpStockCheckDO stockCheck = validateStockCheckExists(updateReqVO.getId());
        if (!ErpStockCheckStatusEnum.DRAFT.getStatus().equals(stockCheck.getStatus())) {
            throw exception(STOCK_CHECK_UPDATE_FAIL_NOT_DRAFT, stockCheck.getNo());
        }
        List<ErpStockCheckItemDO> oldItems = stockCheckItemMapper.selectListByCheckId(updateReqVO.getId());
        if (updateReqVO.getItems() == null) {
            updateReqVO.setItems(Collections.emptyList());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, stockCheck);
        fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldItems);
        Integer checkType = ErpStockCheckTypeEnum.defaultIfNull(updateReqVO.getCheckType());
        updateReqVO.setCheckType(checkType);
        List<ErpStockCheckSaveReqVO.Item> itemReqs = filterDraftItems(updateReqVO.getItems(), checkType);
        List<ErpStockCheckItemDO> stockCheckItems = CollUtil.isEmpty(itemReqs)
                ? Collections.emptyList() : validateStockCheckItems(itemReqs, checkType);

        ErpStockCheckDO updateObj = BeanUtils.toBean(updateReqVO, ErpStockCheckDO.class, target -> target
                .setCheckTime(updateReqVO.getCheckTime() != null ? updateReqVO.getCheckTime() : stockCheck.getCheckTime())
                .setDeptId(updateReqVO.getDeptId() != null ? updateReqVO.getDeptId() : stockCheck.getDeptId())
                .setTotalCount(getSumValue(stockCheckItems, ErpStockCheckItemDO::getCount,
                        BigDecimal::add, BigDecimal.ZERO))
                .setTotalPrice(getSumValue(stockCheckItems, ErpStockCheckItemDO::getTotalPrice,
                        BigDecimal::add, BigDecimal.ZERO)));
        stockCheckMapper.updateById(updateObj);
        replaceStockCheckItems(updateReqVO.getId(), stockCheckItems);
        operateLogService.recordUpdate(ERP_STOCK_CHECK_TYPE, stockCheck.getId(), stockCheck.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitStockCheckDraft(ErpStockCheckSaveReqVO updateReqVO) {
        ErpStockCheckDO stockCheck = validateStockCheckExists(updateReqVO.getId());
        if (!ErpStockCheckStatusEnum.DRAFT.getStatus().equals(stockCheck.getStatus())) {
            throw exception(STOCK_CHECK_UPDATE_FAIL_NOT_DRAFT, stockCheck.getNo());
        }
        updateStockCheck(updateReqVO);
        submitStockCheck(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitStockCheck(Long id) {
        ErpStockCheckDO stockCheck = validateStockCheckExists(id);
        if (!ErpStockCheckStatusEnum.DRAFT.getStatus().equals(stockCheck.getStatus())) {
            throw exception(STOCK_CHECK_SUBMIT_FAIL);
        }
        if (stockCheck.getCheckTime() == null) {
            throw exception(STOCK_CHECK_SUBMIT_TIME_REQUIRED);
        }
        List<ErpStockCheckItemDO> items = stockCheckItemMapper.selectListByCheckId(id);
        if (CollUtil.isEmpty(items)) {
            throw exception(STOCK_CHECK_SUBMIT_ITEMS_REQUIRED);
        }
        Integer checkType = ErpStockCheckTypeEnum.defaultIfNull(stockCheck.getCheckType());
        validateStockCheckItems(BeanUtils.toBean(items, ErpStockCheckSaveReqVO.Item.class), checkType);
        int updateCount = stockCheckMapper.updateByIdAndStatus(id,
                ErpStockCheckStatusEnum.DRAFT.getStatus(),
                new ErpStockCheckDO().setStatus(ErpStockCheckStatusEnum.PROCESS.getStatus()));
        if (updateCount == 0) {
            throw exception(STOCK_CHECK_SUBMIT_FAIL);
        }
        operateLogService.recordUpdate(ERP_STOCK_CHECK_TYPE, stockCheck.getId(), stockCheck.getNo());
    }

    @Override
    public void updateStockCheckRemark(ErpStockUpdateRemarkReqVO updateReqVO) {
        ErpStockCheckDO stockCheck = validateStockCheckExists(updateReqVO.getId());
        stockCheckMapper.updateById(new ErpStockCheckDO()
                .setId(updateReqVO.getId()).setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_STOCK_CHECK_TYPE, stockCheck.getId(), stockCheck.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockCheckStatus(Long id, Integer status) {
        if (!ErpStockCheckStatusEnum.APPROVE.getStatus().equals(status)) {
            throw exception(STOCK_CHECK_PROCESS_FAIL);
        }
        // 1.1 鏍￠獙瀛樺湪
        ErpStockCheckDO stockCheck = validateStockCheckExists(id);
        if (stockCheck.getStatus().equals(status)) {
            throw exception(STOCK_CHECK_APPROVE_FAIL);
        }

        int updateCount = stockCheckMapper.updateByIdAndStatus(id, stockCheck.getStatus(),
                new ErpStockCheckDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(STOCK_CHECK_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_STOCK_CHECK_TYPE, stockCheck.getId(), stockCheck.getNo(), true);

        // 3. 鍙樻洿搴撳瓨
        List<ErpStockCheckItemDO> stockCheckItems = stockCheckItemMapper.selectListByCheckId(id);
        warehouseService.validateCurrentUserWarehousePermission(convertSet(stockCheckItems, ErpStockCheckItemDO::getWarehouseId));
        if (ErpStockCheckTypeEnum.isCount(stockCheck.getCheckType())) {
            stockCheckItems.forEach(stockCheckItem -> {
                if (stockCheckItem.getCount().compareTo(BigDecimal.ZERO) == 0) {
                    return;
                }
                BigDecimal count = stockCheckItem.getCount();
                Integer bizType = count.compareTo(BigDecimal.ZERO) > 0 ? ErpStockRecordBizTypeEnum.CHECK_MORE_IN.getType()
                        : ErpStockRecordBizTypeEnum.CHECK_LESS_OUT.getType();
                stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                        stockCheckItem.getProductId(), stockCheckItem.getWarehouseId(), stockCheckItem.getBatchNo(),
                        stockCheckItem.getProductUnitId(), stockCheckItem.getPackageQty(), stockCheckItem.getWeight(),
                        stockCheckItem.getTotalWeight(), count,
                        bizType, stockCheckItem.getCheckId(), stockCheckItem.getId(), stockCheck.getNo(),
                        count.compareTo(BigDecimal.ZERO) > 0 ? stockCheckItem.getProductPrice() : null,
                        stockCheck.getCheckTime()));
            });
        }
        stockCheckItems.forEach(stockCheckItem -> stockService.updateStockCostPrice(
                stockCheckItem.getProductId(), stockCheckItem.getWarehouseId(), stockCheckItem.getProductPrice()));
    }

    private List<ErpStockCheckItemDO> validateStockCheckItems(List<ErpStockCheckSaveReqVO.Item> list, Integer checkType) {
        validateDuplicateStockCheckItems(list);
        // 1.1 鏍￠獙浜у搧瀛樺湪
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() -> productService.validProductList(
                convertSet(list, ErpStockCheckSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 1.2 鏍￠獙浠撳簱瀛樺湪
        Set<Long> warehouseIds = convertSet(list, ErpStockCheckSaveReqVO.Item::getWarehouseId);
        DataPermissionUtils.executeIgnore(() -> warehouseService.validWarehouseList(warehouseIds));
        warehouseService.validateCurrentUserWarehousePermission(warehouseIds);
        // 2. 杞寲涓?ErpStockCheckItemDO 鍒楄〃
        return convertList(list, o -> BeanUtils.toBean(o, ErpStockCheckItemDO.class, item -> {
            if (item.getProductPrice() == null) {
                throw new IllegalArgumentException("库存盘点明细的价格不能为空");
            }
            if (item.getProductPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("库存盘点明细的价格不能为负");
            }
            BigDecimal stockCount = item.getStockCount() != null ? item.getStockCount() : BigDecimal.ZERO;
            BigDecimal actualCount;
            BigDecimal count;
            BigDecimal totalPrice;
            if (ErpStockCheckTypeEnum.isCost(checkType)) {
                actualCount = stockCount;
                count = BigDecimal.ZERO;
                if (item.getTotalPrice() == null) {
                    throw new IllegalArgumentException("盘成本时金额不能为空");
                }
                if (item.getTotalPrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("盘成本时金额不能为负");
                }
                totalPrice = item.getTotalPrice();
            } else {
                if (item.getCount() == null) {
                    throw new IllegalArgumentException("盘数量时调整数不能为空");
                }
                count = item.getCount();
                actualCount = stockCount.add(count);
                totalPrice = MoneyUtils.priceMultiply(item.getProductPrice(), count);
            }
            ErpProductDO product = productMap.get(item.getProductId());
            BigDecimal weight = snapshotSupport.resolveWeight(item.getWeight(), product);
            item.setProductUnitId(snapshotSupport.resolveProductUnitId(item.getProductUnitId(), product));
            item.setPackageQty(snapshotSupport.resolvePackageQty(item.getPackageQty(), product));
            item.setWeight(weight);
            item.setTotalWeight(snapshotSupport.calculateTotalWeight(weight, count));
            item.setActualCount(actualCount);
            item.setCount(count);
            item.setTotalPrice(totalPrice);
        }));
    }

    private List<ErpStockCheckSaveReqVO.Item> filterDraftItems(
            List<ErpStockCheckSaveReqVO.Item> items, Integer checkType) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        List<ErpStockCheckSaveReqVO.Item> result = new ArrayList<>();
        for (ErpStockCheckSaveReqVO.Item source : items) {
            if (source == null || source.getProductId() == null || source.getWarehouseId() == null) {
                continue;
            }
            ErpStockCheckSaveReqVO.Item item = BeanUtils.toBean(source, ErpStockCheckSaveReqVO.Item.class);
            if (item.getProductPrice() == null) {
                item.setProductPrice(BigDecimal.ZERO);
            }
            if (item.getStockCount() == null) {
                item.setStockCount(BigDecimal.ZERO);
            }
            if (ErpStockCheckTypeEnum.isCost(checkType)) {
                if (item.getTotalPrice() == null) {
                    continue;
                }
            } else if (item.getCount() == null) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    private void replaceStockCheckItems(Long id, List<ErpStockCheckItemDO> items) {
        stockCheckItemMapper.deleteByCheckId(id);
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> item.setId(null).setCheckId(id));
        stockCheckItemMapper.insertBatch(items);
    }

    private BigDecimal getStockAdjustProductPrice(ErpStockDO stock, ErpProductDO product) {
        if (stock != null && stock.getCostPrice() != null) {
            return stock.getCostPrice();
        }
        if (product != null && product.getLastPurchasePrice() != null) {
            return product.getLastPurchasePrice();
        }
        if (product != null && product.getPurchasePrice() != null) {
            return product.getPurchasePrice();
        }
        return BigDecimal.ZERO;
    }

    private String buildStockAdjustRemark(ErpStockAdjustReqVO reqVO) {
        String reason = reqVO.getReason() != null && !reqVO.getReason().isEmpty()
                ? reqVO.getReason() : "搴撳瓨娴忚璋冩暣";
        if (reqVO.getRemark() == null || reqVO.getRemark().isEmpty()) {
            return reason;
        }
        return reason + "：" + reqVO.getRemark();
    }

    private void validateDuplicateStockCheckItems(List<ErpStockCheckSaveReqVO.Item> list) {
        Set<String> keys = new HashSet<>();
        for (ErpStockCheckSaveReqVO.Item item : list) {
            String batchNo = item.getBatchNo() == null ? "" : item.getBatchNo().trim();
            String key = item.getProductId() + "-" + item.getWarehouseId() + "-" + batchNo;
            if (!keys.add(key)) {
                throw exception(STOCK_CHECK_ITEM_DUPLICATE, key);
            }
        }
    }

    private void validateBatchUpdateStockCheckNoDuplicate(List<ErpStockCheckItemDO> stockCheckItems,
                                                          Set<Long> selectedItemIds,
                                                          Long targetWarehouseId) {
        Set<String> keys = new HashSet<>();
        for (ErpStockCheckItemDO item : stockCheckItems) {
            Long warehouseId = selectedItemIds.contains(item.getId()) ? targetWarehouseId : item.getWarehouseId();
            String batchNo = selectedItemIds.contains(item.getId()) || item.getBatchNo() == null
                    ? "" : item.getBatchNo().trim();
            String key = item.getProductId() + "-" + warehouseId + "-" + batchNo;
            if (!keys.add(key)) {
                throw exception(STOCK_CHECK_ITEM_DUPLICATE, key);
            }
        }
    }

    private void updateStockCheckItemList(Long id, List<ErpStockCheckItemDO> newList) {
        // 绗竴姝ワ紝瀵规瘮鏂拌€佹暟鎹紝鑾峰緱娣诲姞銆佷慨鏀广€佸垹闄ょ殑鍒楄〃
        List<ErpStockCheckItemDO> oldList = stockCheckItemMapper.selectListByCheckId(id);
        List<List<ErpStockCheckItemDO>> diffList = diffList(oldList, newList, // id 涓嶅悓锛屽氨璁や负鏄笉鍚岀殑璁板綍
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 绗簩姝ワ紝鎵归噺娣诲姞銆佷慨鏀广€佸垹闄?
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setCheckId(id));
            stockCheckItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            stockCheckItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            stockCheckItemMapper.deleteByIds(convertList(diffList.get(2), ErpStockCheckItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStockCheck(List<Long> ids) {
        // 1. 鏍￠獙涓嶅浜庡凡瀹℃壒
        List<ErpStockCheckDO> stockChecks = stockCheckMapper.selectByIds(ids);
        if (CollUtil.isEmpty(stockChecks)) {
            return;
        }
        stockChecks.forEach(stockCheck -> {
            if (ErpStockCheckStatusEnum.APPROVE.getStatus().equals(stockCheck.getStatus())) {
                throw exception(STOCK_CHECK_DELETE_FAIL_APPROVE, stockCheck.getNo());
            }
        });

        // 2. 閬嶅巻鍒犻櫎锛屽苟璁板綍鎿嶄綔鏃ュ織
        stockChecks.forEach(stockCheck -> {
            // 2.1 鍒犻櫎鐩樼偣鍗?
            stockCheckMapper.deleteById(stockCheck.getId());
            // 2.2 鍒犻櫎鐩樼偣鍗曢」
            stockCheckItemMapper.deleteByCheckId(stockCheck.getId());
            operateLogService.recordDelete(ERP_STOCK_CHECK_TYPE, stockCheck.getId(), stockCheck.getNo());
        });
    }

    private ErpStockCheckDO validateStockCheckExists(Long id) {
        ErpStockCheckDO stockCheck = stockCheckMapper.selectById(id);
        if (stockCheck == null) {
            throw exception(STOCK_CHECK_NOT_EXISTS);
        }
        return stockCheck;
    }

    @Override
    public ErpStockCheckDO getStockCheck(Long id) {
        return stockCheckMapper.selectById(id);
    }

    @Override
    public PageResult<ErpStockCheckDO> getStockCheckPage(ErpStockCheckPageReqVO pageReqVO) {
        return stockCheckMapper.selectPage(pageReqVO);
    }

    // ==================== 鐩樼偣椤?====================

    @Override
    public List<ErpStockCheckItemDO> getStockCheckItemListByCheckId(Long checkId) {
        return stockCheckItemMapper.selectListByCheckId(checkId);
    }

    @Override
    public PageResult<ErpStockCheckItemDO> getStockCheckItemPage(ErpStockCheckItemPageReqVO pageReqVO) {
        validateStockCheckExists(pageReqVO.getCheckId());
        return stockCheckItemMapper.selectPageByCheckId(pageReqVO);
    }

    @Override
    public List<ErpStockCheckItemDO> getStockCheckItemListByCheckIds(Collection<Long> checkIds) {
        if (CollUtil.isEmpty(checkIds)) {
            return Collections.emptyList();
        }
        return stockCheckItemMapper.selectListByCheckIds(checkIds);
    }

}
