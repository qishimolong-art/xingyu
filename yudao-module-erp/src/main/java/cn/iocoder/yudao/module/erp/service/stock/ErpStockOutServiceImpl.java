package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutMapper;
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
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_STOCK_OUT_TYPE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

// TODO 芋艿：记录操作日志

/**
 * ERP 其它出库单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpStockOutServiceImpl implements ErpStockOutService {

    private static final String FIELD_PERMISSION_MODULE = "erp_stock_out";

    @Resource
    private ErpStockOutMapper stockOutMapper;
    @Resource
    private ErpStockOutItemMapper stockOutItemMapper;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpStockItemBatchUpdateSupport batchUpdateSupport;

    @Resource
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Resource
    private ErpVoucherService voucherService;
    @Resource
    private ErpBookOpenService bookOpenService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockOut(ErpStockOutSaveReqVO createReqVO) {
        // 1.1 校验出库项的有效性
        List<ErpStockOutItemDO> stockOutItems = validateStockOutItems(createReqVO.getItems());
        // 1.2 校验客户
        customerService.validateCustomer(createReqVO.getCustomerId());
        // 1.3 生成出库单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.STOCK_OUT_NO_PREFIX);
        if (stockOutMapper.selectByNo(no) != null) {
            throw exception(STOCK_OUT_NO_EXISTS);
        }

        // 2.1 插入出库单
        ErpStockOutDO stockOut = BeanUtils.toBean(createReqVO, ErpStockOutDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setTotalCount(getSumValue(stockOutItems, ErpStockOutItemDO::getCount, BigDecimal::add))
                .setTotalPrice(getSumValue(stockOutItems, ErpStockOutItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO)));
        stockOutMapper.insert(stockOut);
        // 2.2 插入出库单项
        stockOutItems.forEach(o -> o.setOutId(stockOut.getId()));
        stockOutItemMapper.insertBatch(stockOutItems);
        operateLogService.recordCreate(ERP_STOCK_OUT_TYPE, stockOut.getId(), stockOut.getNo());
        return stockOut.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockOut(ErpStockOutSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpStockOutDO stockOut = validateStockOutExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(stockOut.getStatus())) {
            throw exception(STOCK_OUT_UPDATE_FAIL_APPROVE, stockOut.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, stockOut);
        fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(),
                stockOutItemMapper.selectListByOutId(updateReqVO.getId()));
        // 1.2 校验客户
        customerService.validateCustomer(updateReqVO.getCustomerId());
        // 1.3 校验出库项的有效性
        List<ErpStockOutItemDO> stockOutItems = validateStockOutItems(updateReqVO.getItems());

        // 2.1 更新出库单
        ErpStockOutDO updateObj = BeanUtils.toBean(updateReqVO, ErpStockOutDO.class, in -> in
                .setTotalCount(getSumValue(stockOutItems, ErpStockOutItemDO::getCount, BigDecimal::add))
                .setTotalPrice(getSumValue(stockOutItems, ErpStockOutItemDO::getTotalPrice, BigDecimal::add)));
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(stockOut.getDeptId());
        }
        stockOutMapper.updateById(updateObj);
        // 2.2 更新出库单项
        updateStockOutItemList(updateReqVO.getId(), stockOutItems);
        operateLogService.recordUpdate(ERP_STOCK_OUT_TYPE, stockOut.getId(), stockOut.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStockOutItems(ErpStockOutItemBatchUpdateReqVO updateReqVO) {
        batchUpdateSupport.validateFieldPermission(FIELD_PERMISSION_MODULE, STOCK_OUT_ITEM_BATCH_UPDATE_FIELD_REQUIRED);
        ErpStockOutDO stockOut = validateStockOutExists(updateReqVO.getOutId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(stockOut.getStatus())) {
            throw exception(STOCK_OUT_UPDATE_FAIL_APPROVE, stockOut.getNo());
        }
        ErpWarehouseDO targetWarehouse = batchUpdateSupport.validateTargetWarehouse(updateReqVO.getWarehouseId());
        batchUpdateSupport.validateDocumentDeptAllowed(stockOut.getDeptId(), targetWarehouse, FIELD_PERMISSION_MODULE,
                STOCK_OUT_ITEM_BATCH_UPDATE_WAREHOUSE_DEPT_NOT_ALLOWED);

        List<ErpStockOutItemDO> stockOutItems = stockOutItemMapper.selectListByOutId(updateReqVO.getOutId());
        Set<Long> selectedItemIds = new LinkedHashSet<>(updateReqVO.getItemIds());
        List<ErpStockOutItemDO> selectedItems = stockOutItems.stream()
                .filter(item -> selectedItemIds.contains(item.getId()))
                .collect(java.util.stream.Collectors.toList());
        if (selectedItems.size() != selectedItemIds.size()) {
            throw exception(STOCK_OUT_ITEM_BATCH_UPDATE_ITEM_NOT_EXISTS);
        }
        validateBatchUpdateStockOutNoDuplicate(stockOutItems, selectedItemIds, targetWarehouse.getId());

        selectedItems.forEach(item -> item.setWarehouseId(targetWarehouse.getId()));
        stockOutItemMapper.updateBatch(selectedItems);
        operateLogService.recordUpdate(ERP_STOCK_OUT_TYPE, stockOut.getId(), stockOut.getNo());
    }

    @Override
    public List<DeptSimpleRespVO> getWarehouseDeptSimpleList(Long warehouseId) {
        return batchUpdateSupport.getWarehouseAvailableDeptSimpleList(warehouseId, FIELD_PERMISSION_MODULE);
    }

    @Override
    public void updateStockOutRemark(ErpStockUpdateRemarkReqVO updateReqVO) {
        ErpStockOutDO stockOut = validateStockOutExists(updateReqVO.getId());
        stockOutMapper.updateById(new ErpStockOutDO()
                .setId(updateReqVO.getId()).setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_STOCK_OUT_TYPE, stockOut.getId(), stockOut.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockOutStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(STOCK_OUT_PROCESS_FAIL);
        }
        // 1.1 校验存在
        ErpStockOutDO stockOut = validateStockOutExists(id);
        // 1.2 校验状态
        if (stockOut.getStatus().equals(status)) {
            throw exception(STOCK_OUT_APPROVE_FAIL);
        }

        // 2. 更新状态
        int updateCount = stockOutMapper.updateByIdAndStatus(id, stockOut.getStatus(),
                new ErpStockOutDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(STOCK_OUT_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_STOCK_OUT_TYPE, stockOut.getId(), stockOut.getNo(), true);

        // 3. 取出库项
        List<ErpStockOutItemDO> stockOutItems = stockOutItemMapper.selectListByOutId(id);
        warehouseService.validateCurrentUserWarehousePermission(convertSet(stockOutItems, ErpStockOutItemDO::getWarehouseId));

        // 4. 审批通过：扣库存前先累加成本快照（与销售出库一致）
        boolean enableVoucher = stockOut.getOutTime() != null
                && bookOpenService.isVoucherTypeEnabled(stockOut.getOutTime().toLocalDate(),
                ErpVoucherTypeEnum.OTHER_OUT.getType());
        BigDecimal sumCost = BigDecimal.ZERO;
        if (enableVoucher) {
            for (ErpStockOutItemDO item : stockOutItems) {
                ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                        stockService.getStock(item.getProductId(), item.getWarehouseId()));
                BigDecimal cost = (stock != null && stock.getCostPrice() != null)
                        ? stock.getCostPrice() : BigDecimal.ZERO;
                sumCost = sumCost.add(cost.multiply(item.getCount()));
            }
        }

        // 5. 变更库存
        Integer bizType = ErpStockRecordBizTypeEnum.OTHER_OUT.getType();
        stockOutItems.forEach(stockOutItem -> {
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    stockOutItem.getProductId(), stockOutItem.getWarehouseId(), stockOutItem.getCount().negate(),
                    bizType, stockOutItem.getOutId(), stockOutItem.getId(), stockOut.getNo(),
                    null, stockOut.getOutTime()));
        });

        // 6. 审批通过：自动生成其他出库凭证
        if (enableVoucher) {
            List<ErpVoucherItemDO> voucherItems = autoVoucherBuilder.buildStockOutItems(stockOut, sumCost);
            voucherService.createVoucherFromBiz(
                    ErpVoucherSourceBizTypeEnum.OTHER_OUT.getType(),
                    stockOut.getId(),
                    stockOut.getNo(),
                    stockOut.getTotalPrice(),
                    stockOut.getOutTime().toLocalDate(),
                    "其他出库 - " + stockOut.getNo(),
                    voucherItems);
        }
    }

    private List<ErpStockOutItemDO> validateStockOutItems(List<ErpStockOutSaveReqVO.Item> list) {
        validateDuplicateStockOutItems(list);
        // 1.1 校验产品存在
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() -> productService.validProductList(
                convertSet(list, ErpStockOutSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 1.2 校验仓库存在
        Set<Long> warehouseIds = convertSet(list, ErpStockOutSaveReqVO.Item::getWarehouseId);
        DataPermissionUtils.executeIgnore(() -> warehouseService.validWarehouseList(warehouseIds));
        warehouseService.validateCurrentUserWarehousePermission(warehouseIds);
        // 2. 转化为 ErpStockOutItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpStockOutItemDO.class, item -> item
                .setProductUnitId(productMap.get(item.getProductId()).getUnitId())
                .setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()))));
    }

    private void validateDuplicateStockOutItems(List<ErpStockOutSaveReqVO.Item> list) {
        Set<String> keys = new HashSet<>();
        for (ErpStockOutSaveReqVO.Item item : list) {
            String key = item.getProductId() + "-" + item.getWarehouseId();
            if (!keys.add(key)) {
                throw exception(STOCK_OUT_ITEM_DUPLICATE, key);
            }
        }
    }

    private void validateBatchUpdateStockOutNoDuplicate(List<ErpStockOutItemDO> stockOutItems,
                                                        Set<Long> selectedItemIds,
                                                        Long targetWarehouseId) {
        Set<String> keys = new HashSet<>();
        for (ErpStockOutItemDO item : stockOutItems) {
            Long warehouseId = selectedItemIds.contains(item.getId()) ? targetWarehouseId : item.getWarehouseId();
            String key = item.getProductId() + "-" + warehouseId;
            if (!keys.add(key)) {
                throw exception(STOCK_OUT_ITEM_DUPLICATE, key);
            }
        }
    }

    private void updateStockOutItemList(Long id, List<ErpStockOutItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpStockOutItemDO> oldList = stockOutItemMapper.selectListByOutId(id);
        List<List<ErpStockOutItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setOutId(id));
            stockOutItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            stockOutItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            stockOutItemMapper.deleteByIds(convertList(diffList.get(2), ErpStockOutItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStockOut(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpStockOutDO> stockOuts = stockOutMapper.selectByIds(ids);
        if (CollUtil.isEmpty(stockOuts)) {
            return;
        }
        stockOuts.forEach(stockOut -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(stockOut.getStatus())) {
                throw exception(STOCK_OUT_DELETE_FAIL_APPROVE, stockOut.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        stockOuts.forEach(stockOut -> {
            // 2.1 删除出库单
            stockOutMapper.deleteById(stockOut.getId());
            // 2.2 删除出库单项
            stockOutItemMapper.deleteByOutId(stockOut.getId());
            operateLogService.recordDelete(ERP_STOCK_OUT_TYPE, stockOut.getId(), stockOut.getNo());
        });
    }

    private ErpStockOutDO validateStockOutExists(Long id) {
        ErpStockOutDO stockOut = stockOutMapper.selectById(id);
        if (stockOut == null) {
            throw exception(STOCK_OUT_NOT_EXISTS);
        }
        return stockOut;
    }

    @Override
    public ErpStockOutDO getStockOut(Long id) {
        return stockOutMapper.selectById(id);
    }

    @Override
    public PageResult<ErpStockOutDO> getStockOutPage(ErpStockOutPageReqVO pageReqVO) {
        return stockOutMapper.selectPage(pageReqVO);
    }

    // ==================== 出库项 ====================

    @Override
    public List<ErpStockOutItemDO> getStockOutItemListByOutId(Long outId) {
        return stockOutItemMapper.selectListByOutId(outId);
    }

    @Override
    public List<ErpStockOutItemDO> getStockOutItemListByOutIds(Collection<Long> outIds) {
        if (CollUtil.isEmpty(outIds)) {
            return Collections.emptyList();
        }
        return stockOutItemMapper.selectListByOutIds(outIds);
    }

}
