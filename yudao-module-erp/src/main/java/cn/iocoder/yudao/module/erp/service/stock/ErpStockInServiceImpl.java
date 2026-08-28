package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_STOCK_IN_TYPE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

// TODO 芋艿：记录操作日志
/**
 * ERP 其它入库单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpStockInServiceImpl implements ErpStockInService {

    private static final String FIELD_PERMISSION_MODULE = "erp_stock_in";

    @Resource
    private ErpStockInMapper stockInMapper;
    @Resource
    private ErpStockInItemMapper stockInItemMapper;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpStockItemBatchUpdateSupport batchUpdateSupport;
    @Resource
    private ErpStockItemSnapshotSupport snapshotSupport;

    @Resource
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Resource
    private ErpVoucherService voucherService;
    @Resource
    private ErpBookOpenService bookOpenService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockIn(ErpStockInSaveReqVO createReqVO) {
        // 1.1 校验入库项的有效性
        List<ErpStockInItemDO> stockInItems = validateStockInItems(createReqVO.getItems());
        // 1.2 校验供应商
        supplierService.validateSupplier(createReqVO.getSupplierId());
        // 1.3 生成入库单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.STOCK_IN_NO_PREFIX);
        if (stockInMapper.selectByNo(no) != null) {
            throw exception(STOCK_IN_NO_EXISTS);
        }

        // 2.1 插入入库单
        ErpStockInDO stockIn = BeanUtils.toBean(createReqVO, ErpStockInDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setTotalCount(getSumValue(stockInItems, ErpStockInItemDO::getCount, BigDecimal::add))
                .setTotalPrice(getSumValue(stockInItems, ErpStockInItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO)));
        stockInMapper.insert(stockIn);
        // 2.2 插入入库单项
        stockInItems.forEach(o -> o.setInId(stockIn.getId()));
        stockInItemMapper.insertBatch(stockInItems);
        operateLogService.recordCreate(ERP_STOCK_IN_TYPE, stockIn.getId(), stockIn.getNo());
        return stockIn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockIn(ErpStockInSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpStockInDO stockIn = validateStockInExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(stockIn.getStatus())) {
            throw exception(STOCK_IN_UPDATE_FAIL_APPROVE, stockIn.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, stockIn);
        fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(),
                stockInItemMapper.selectListByInId(updateReqVO.getId()));
        // 1.2 校验供应商
        supplierService.validateSupplier(updateReqVO.getSupplierId());
        // 1.3 校验入库项的有效性
        List<ErpStockInItemDO> stockInItems = validateStockInItems(updateReqVO.getItems());

        // 2.1 更新入库单
        ErpStockInDO updateObj = BeanUtils.toBean(updateReqVO, ErpStockInDO.class, in -> in
                .setTotalCount(getSumValue(stockInItems, ErpStockInItemDO::getCount, BigDecimal::add))
                .setTotalPrice(getSumValue(stockInItems, ErpStockInItemDO::getTotalPrice, BigDecimal::add)));
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(stockIn.getDeptId());
        }
        stockInMapper.updateById(updateObj);
        // 2.2 更新入库单项
        updateStockInItemList(updateReqVO.getId(), stockInItems);
        operateLogService.recordUpdate(ERP_STOCK_IN_TYPE, stockIn.getId(), stockIn.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStockInItems(ErpStockInItemBatchUpdateReqVO updateReqVO) {
        batchUpdateSupport.validateFieldPermission(FIELD_PERMISSION_MODULE, STOCK_IN_ITEM_BATCH_UPDATE_FIELD_REQUIRED);
        ErpStockInDO stockIn = validateStockInExists(updateReqVO.getInId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(stockIn.getStatus())) {
            throw exception(STOCK_IN_UPDATE_FAIL_APPROVE, stockIn.getNo());
        }
        ErpWarehouseDO targetWarehouse = batchUpdateSupport.validateTargetWarehouse(updateReqVO.getWarehouseId());
        batchUpdateSupport.validateDocumentDeptAllowed(stockIn.getDeptId(), targetWarehouse, FIELD_PERMISSION_MODULE,
                STOCK_IN_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);

        List<ErpStockInItemDO> stockInItems = stockInItemMapper.selectListByInId(updateReqVO.getInId());
        Set<Long> selectedItemIds = new LinkedHashSet<>(updateReqVO.getItemIds());
        List<ErpStockInItemDO> selectedItems = stockInItems.stream()
                .filter(item -> selectedItemIds.contains(item.getId()))
                .collect(java.util.stream.Collectors.toList());
        if (selectedItems.size() != selectedItemIds.size()) {
            throw exception(STOCK_IN_ITEM_BATCH_UPDATE_ITEM_NOT_EXISTS);
        }
        validateBatchUpdateStockInNoDuplicate(stockInItems, selectedItemIds, targetWarehouse.getId());

        for (ErpStockInItemDO item : selectedItems) {
            item.setWarehouseId(targetWarehouse.getId());
            stockService.ensureStockExists(item.getProductId(), targetWarehouse.getId());
        }
        stockInItemMapper.updateBatch(selectedItems);
        operateLogService.recordUpdate(ERP_STOCK_IN_TYPE, stockIn.getId(), stockIn.getNo());
    }

    @Override
    public List<DeptSimpleRespVO> getWarehouseDeptSimpleList(Long warehouseId) {
        return batchUpdateSupport.getWarehouseAvailableDeptSimpleList(warehouseId, FIELD_PERMISSION_MODULE);
    }

    @Override
    public void updateStockInRemark(ErpStockUpdateRemarkReqVO updateReqVO) {
        ErpStockInDO stockIn = validateStockInExists(updateReqVO.getId());
        stockInMapper.updateById(new ErpStockInDO()
                .setId(updateReqVO.getId()).setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_STOCK_IN_TYPE, stockIn.getId(), stockIn.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockInStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(STOCK_IN_PROCESS_FAIL);
        }
        // 1.1 校验存在
        ErpStockInDO stockIn = validateStockInExists(id);
        // 1.2 校验状态
        if (stockIn.getStatus().equals(status)) {
            throw exception(STOCK_IN_APPROVE_FAIL);
        }

        // 2. 更新状态
        int updateCount = stockInMapper.updateByIdAndStatus(id, stockIn.getStatus(),
                new ErpStockInDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(STOCK_IN_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_STOCK_IN_TYPE, stockIn.getId(), stockIn.getNo(), true);

        // 3. 变更库存
        List<ErpStockInItemDO> stockInItems = stockInItemMapper.selectListByInId(id);
        warehouseService.validateCurrentUserWarehousePermission(convertSet(stockInItems, ErpStockInItemDO::getWarehouseId));
        Integer bizType = ErpStockRecordBizTypeEnum.OTHER_IN.getType();
        stockInItems.forEach(stockInItem -> {
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    stockInItem.getProductId(), stockInItem.getWarehouseId(), stockInItem.getBatchNo(),
                    stockInItem.getProductUnitId(), stockInItem.getPackageQty(), stockInItem.getWeight(),
                    stockInItem.getTotalWeight(), stockInItem.getCount(),
                    bizType, stockInItem.getInId(), stockInItem.getId(), stockIn.getNo(),
                    stockInItem.getProductPrice(), stockIn.getInTime()));
        });

        // 4. 审批通过：自动生成其他入库凭证
        // 金额按单据 totalPrice（即所有明细 productPrice × count），与一期成本核算约定一致
        if (stockIn.getInTime() != null
                && bookOpenService.isVoucherTypeEnabled(stockIn.getInTime().toLocalDate(),
                ErpVoucherTypeEnum.OTHER_IN.getType())) {
            BigDecimal sumCost = stockIn.getTotalPrice() == null ? BigDecimal.ZERO : stockIn.getTotalPrice();
            List<ErpVoucherItemDO> voucherItems = autoVoucherBuilder.buildStockInItems(stockIn, sumCost);
            voucherService.createVoucherFromBiz(
                    ErpVoucherSourceBizTypeEnum.OTHER_IN.getType(),
                    stockIn.getId(),
                    stockIn.getNo(),
                    stockIn.getTotalPrice(),
                    stockIn.getInTime().toLocalDate(),
                    "其他入库 - " + stockIn.getNo(),
                    voucherItems);
        }
    }

    private List<ErpStockInItemDO> validateStockInItems(List<ErpStockInSaveReqVO.Item> list) {
        validateDuplicateStockInItems(list);
        // 1.1 校验产品存在
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() -> productService.validProductList(
                convertSet(list, ErpStockInSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 1.2 校验仓库存在
        Set<Long> warehouseIds = convertSet(list, ErpStockInSaveReqVO.Item::getWarehouseId);
        DataPermissionUtils.executeIgnore(() -> warehouseService.validWarehouseList(warehouseIds));
        warehouseService.validateCurrentUserWarehousePermission(warehouseIds);
        // 2. 转化为 ErpStockInItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpStockInItemDO.class, item -> {
            ErpProductDO product = productMap.get(item.getProductId());
            BigDecimal weight = snapshotSupport.resolveWeight(o.getWeight(), product);
            item.setBatchNo(normalizeBatchNo(item.getBatchNo()))
                    .setProductUnitId(snapshotSupport.resolveProductUnitId(item.getProductUnitId(), product))
                    .setPackageQty(snapshotSupport.resolvePackageQty(o.getPackageQty(), product))
                    .setWeight(weight)
                    .setTotalWeight(snapshotSupport.calculateTotalWeight(weight, item.getCount()))
                    .setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
        }));
    }

    private void validateDuplicateStockInItems(List<ErpStockInSaveReqVO.Item> list) {
        Set<String> keys = new HashSet<>();
        for (ErpStockInSaveReqVO.Item item : list) {
            String key = item.getProductId() + "-" + item.getWarehouseId() + "-" + normalizeBatchNo(item.getBatchNo());
            if (!keys.add(key)) {
                throw exception(STOCK_IN_ITEM_DUPLICATE, key);
            }
        }
    }

    private void validateBatchUpdateStockInNoDuplicate(List<ErpStockInItemDO> stockInItems,
                                                       Set<Long> selectedItemIds,
                                                       Long targetWarehouseId) {
        Set<String> keys = new HashSet<>();
        for (ErpStockInItemDO item : stockInItems) {
            Long warehouseId = selectedItemIds.contains(item.getId()) ? targetWarehouseId : item.getWarehouseId();
            String key = item.getProductId() + "-" + warehouseId + "-" + normalizeBatchNo(item.getBatchNo());
            if (!keys.add(key)) {
                throw exception(STOCK_IN_ITEM_DUPLICATE, key);
            }
        }
    }

    private String normalizeBatchNo(String batchNo) {
        return StringUtils.hasText(batchNo) ? batchNo.trim() : "";
    }

    private void updateStockInItemList(Long id, List<ErpStockInItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpStockInItemDO> oldList = stockInItemMapper.selectListByInId(id);
        List<List<ErpStockInItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setInId(id));
            stockInItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            stockInItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            stockInItemMapper.deleteByIds(convertList(diffList.get(2), ErpStockInItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStockIn(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpStockInDO> stockIns = stockInMapper.selectByIds(ids);
        if (CollUtil.isEmpty(stockIns)) {
            return;
        }
        stockIns.forEach(stockIn -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(stockIn.getStatus())) {
                throw exception(STOCK_IN_DELETE_FAIL_APPROVE, stockIn.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        stockIns.forEach(stockIn -> {
            // 2.1 删除入库单
            stockInMapper.deleteById(stockIn.getId());
            // 2.2 删除入库单项
            stockInItemMapper.deleteByInId(stockIn.getId());
            operateLogService.recordDelete(ERP_STOCK_IN_TYPE, stockIn.getId(), stockIn.getNo());
        });
    }

    private ErpStockInDO validateStockInExists(Long id) {
        ErpStockInDO stockIn = stockInMapper.selectById(id);
        if (stockIn == null) {
            throw exception(STOCK_IN_NOT_EXISTS);
        }
        return stockIn;
    }

    @Override
    public ErpStockInDO getStockIn(Long id) {
        return stockInMapper.selectById(id);
    }

    @Override
    public PageResult<ErpStockInDO> getStockInPage(ErpStockInPageReqVO pageReqVO) {
        return stockInMapper.selectPage(pageReqVO);
    }

    // ==================== 入库项 ====================

    @Override
    public List<ErpStockInItemDO> getStockInItemListByInId(Long inId) {
        return stockInItemMapper.selectListByInId(inId);
    }

    @Override
    public List<ErpStockInItemDO> getStockInItemListByInIds(Collection<Long> inIds) {
        if (CollUtil.isEmpty(inIds)) {
            return Collections.emptyList();
        }
        return stockInItemMapper.selectListByInIds(inIds);
    }

}
