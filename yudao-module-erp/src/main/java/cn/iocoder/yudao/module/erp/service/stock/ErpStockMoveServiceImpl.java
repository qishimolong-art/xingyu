package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_STOCK_MOVE_TYPE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

// TODO 芋艿：记录操作日志

/**
 * ERP 库存调拨单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpStockMoveServiceImpl implements ErpStockMoveService {

    private static final String FIELD_PERMISSION_MODULE = "erp_stock_move";

    @Resource
    private ErpStockMoveMapper stockMoveMapper;
    @Resource
    private ErpStockMoveItemMapper stockMoveItemMapper;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockMove(ErpStockMoveSaveReqVO createReqVO) {
        return doCreateStockMove(createReqVO, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockMoveDraft(ErpStockMoveSaveReqVO createReqVO) {
        return doCreateStockMove(createReqVO, false);
    }

    private Long doCreateStockMove(ErpStockMoveSaveReqVO createReqVO, boolean requireWarehouses) {
        // 1.1 校验出库项的有效性
        List<ErpStockMoveItemDO> stockMoveItems = validateStockMoveItems(createReqVO.getItems(), requireWarehouses);
        // 1.2 生成调拨单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.STOCK_MOVE_NO_PREFIX);
        if (stockMoveMapper.selectByNo(no) != null) {
            throw exception(STOCK_MOVE_NO_EXISTS);
        }

        // 2.1 插入出库单
        ErpStockMoveDO stockMove = BeanUtils.toBean(createReqVO, ErpStockMoveDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setTotalCount(getSumValue(stockMoveItems, ErpStockMoveItemDO::getCount, BigDecimal::add))
                .setTotalPrice(getSumValue(stockMoveItems, ErpStockMoveItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO)));
        stockMoveMapper.insert(stockMove);
        // 2.2 插入出库单项
        stockMoveItems.forEach(o -> o.setMoveId(stockMove.getId()));
        stockMoveItemMapper.insertBatch(stockMoveItems);
        operateLogService.recordCreate(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo());
        return stockMove.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockMove(ErpStockMoveSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpStockMoveDO stockMove = validateStockMoveExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(stockMove.getStatus())) {
            throw exception(STOCK_MOVE_UPDATE_FAIL_APPROVE, stockMove.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, stockMove);
        fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(),
                stockMoveItemMapper.selectListByMoveId(updateReqVO.getId()));
        // 1.2 校验出库项的有效性
        List<ErpStockMoveItemDO> stockMoveItems = validateStockMoveItems(updateReqVO.getItems(), true);

        // 2.1 更新出库单
        ErpStockMoveDO updateObj = BeanUtils.toBean(updateReqVO, ErpStockMoveDO.class, in -> in
                .setTotalCount(getSumValue(stockMoveItems, ErpStockMoveItemDO::getCount, BigDecimal::add))
                .setTotalPrice(getSumValue(stockMoveItems, ErpStockMoveItemDO::getTotalPrice, BigDecimal::add)));
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(stockMove.getDeptId());
        }
        stockMoveMapper.updateById(updateObj);
        // 2.2 更新出库单项
        updateStockMoveItemList(updateReqVO.getId(), stockMoveItems);
        operateLogService.recordUpdate(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockMoveStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(STOCK_MOVE_PROCESS_FAIL);
        }
        // 1.1 校验存在
        ErpStockMoveDO stockMove = validateStockMoveExists(id);
        // 1.2 校验状态
        if (stockMove.getStatus().equals(status)) {
            throw exception(STOCK_MOVE_APPROVE_FAIL);
        }

        // 2. 更新状态
        int updateCount = stockMoveMapper.updateByIdAndStatus(id, stockMove.getStatus(),
                new ErpStockMoveDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(STOCK_MOVE_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo(), true);

        // 3. 变更库存
        List<ErpStockMoveItemDO> stockMoveItems = stockMoveItemMapper.selectListByMoveId(id);
        validateStockMoveItemsReadyForApprove(stockMoveItems);
        Set<Long> warehouseIds = new HashSet<>();
        stockMoveItems.forEach(item -> {
            warehouseIds.add(item.getFromWarehouseId());
            warehouseIds.add(item.getToWarehouseId());
        });
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);
        Map<Long, ErpProductDO> productMap = convertMap(productService.validProductList(
                convertSet(stockMoveItems, ErpStockMoveItemDO::getProductId)), ErpProductDO::getId);
        Integer fromBizType = ErpStockRecordBizTypeEnum.MOVE_OUT.getType();
        Integer toBizType = ErpStockRecordBizTypeEnum.MOVE_IN.getType();
        stockMoveItems.forEach(stockMoveItem -> {
            BigDecimal fromCount = stockMoveItem.getCount().negate();
            BigDecimal toCount = stockMoveItem.getCount();
            // 调拨入库单价：优先取 A 仓（fromWarehouse）当前成本均价；取不到则 fallback 到明细单价
            BigDecimal toUnitPrice = resolveStockMoveUnitPrice(stockMoveItem, warehouseMap, productMap);
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    stockMoveItem.getProductId(), stockMoveItem.getFromWarehouseId(), fromCount,
                    fromBizType, stockMoveItem.getMoveId(), stockMoveItem.getId(), stockMove.getNo(),
                    null, stockMove.getMoveTime()));
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    stockMoveItem.getProductId(), stockMoveItem.getToWarehouseId(), toCount,
                    toBizType, stockMoveItem.getMoveId(), stockMoveItem.getId(), stockMove.getNo(),
                    toUnitPrice, stockMove.getMoveTime()));
        });
    }

    private List<ErpStockMoveItemDO> validateStockMoveItems(List<ErpStockMoveSaveReqVO.Item> list,
                                                            boolean requireWarehouses) {
        validateDuplicateStockMoveItems(list);
        // 1.1 校验产品存在
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpStockMoveSaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 1.2 校验仓库存在
        validateStockMoveItemWarehouses(list, requireWarehouses);
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(collectStockMoveWarehouseIds(list));
        // 2. 转化为 ErpStockMoveItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpStockMoveItemDO.class, item -> item
                .setProductUnitId(productMap.get(item.getProductId()).getUnitId())
                .setProductPrice(resolveSubmittedProductPrice(item, warehouseMap, productMap))
                .setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()))));
    }

    private Set<Long> collectStockMoveWarehouseIds(List<ErpStockMoveSaveReqVO.Item> list) {
        Set<Long> warehouseIds = new HashSet<>();
        for (ErpStockMoveSaveReqVO.Item item : list) {
            if (item.getFromWarehouseId() != null) {
                warehouseIds.add(item.getFromWarehouseId());
            }
            if (item.getToWarehouseId() != null) {
                warehouseIds.add(item.getToWarehouseId());
            }
        }
        return warehouseIds;
    }

    private BigDecimal resolveSubmittedProductPrice(ErpStockMoveItemDO item,
                                                    Map<Long, ErpWarehouseDO> warehouseMap,
                                                    Map<Long, ErpProductDO> productMap) {
        if (!isCrossDeptMove(item.getFromWarehouseId(), item.getToWarehouseId(), warehouseMap)) {
            return item.getProductPrice();
        }
        return requireSharePrice(item, productMap);
    }

    private BigDecimal resolveStockMoveUnitPrice(ErpStockMoveItemDO item,
                                                 Map<Long, ErpWarehouseDO> warehouseMap,
                                                 Map<Long, ErpProductDO> productMap) {
        if (isCrossDeptMove(item.getFromWarehouseId(), item.getToWarehouseId(), warehouseMap)) {
            return requireSharePrice(item, productMap);
        }
        ErpStockDO fromStock = stockService.getStock(item.getProductId(), item.getFromWarehouseId());
        return (fromStock != null && fromStock.getCostPrice() != null)
                ? fromStock.getCostPrice() : item.getProductPrice();
    }

    private boolean isCrossDeptMove(Long fromWarehouseId, Long toWarehouseId, Map<Long, ErpWarehouseDO> warehouseMap) {
        ErpWarehouseDO fromWarehouse = warehouseMap.get(fromWarehouseId);
        ErpWarehouseDO toWarehouse = warehouseMap.get(toWarehouseId);
        return fromWarehouse != null && toWarehouse != null
                && fromWarehouse.getDeptId() != null && toWarehouse.getDeptId() != null
                && !fromWarehouse.getDeptId().equals(toWarehouse.getDeptId());
    }

    private BigDecimal requireSharePrice(ErpStockMoveItemDO item, Map<Long, ErpProductDO> productMap) {
        ErpProductDO product = productMap.get(item.getProductId());
        if (product == null || product.getSharePrice() == null) {
            throw exception(STOCK_MOVE_SHARE_PRICE_REQUIRED, item.getProductId());
        }
        return product.getSharePrice();
    }

    private void validateStockMoveItemWarehouses(List<ErpStockMoveSaveReqVO.Item> list, boolean requireWarehouses) {
        Set<Long> warehouseIds = new HashSet<>();
        for (ErpStockMoveSaveReqVO.Item item : list) {
            if (requireWarehouses && (item.getFromWarehouseId() == null || item.getToWarehouseId() == null)) {
                throw exception(STOCK_MOVE_WAREHOUSE_REQUIRED);
            }
            if (item.getFromWarehouseId() != null && item.getToWarehouseId() != null
                    && item.getFromWarehouseId().equals(item.getToWarehouseId())) {
                throw exception(STOCK_MOVE_WAREHOUSE_SAME);
            }
            if (item.getFromWarehouseId() != null) {
                warehouseIds.add(item.getFromWarehouseId());
            }
            if (item.getToWarehouseId() != null) {
                warehouseIds.add(item.getToWarehouseId());
            }
        }
        warehouseService.validWarehouseList(warehouseIds);
        warehouseService.validateCurrentUserWarehousePermission(warehouseIds);
    }

    private void validateStockMoveItemsReadyForApprove(List<ErpStockMoveItemDO> list) {
        Set<Long> warehouseIds = new HashSet<>();
        for (ErpStockMoveItemDO item : list) {
            if (item.getFromWarehouseId() == null || item.getToWarehouseId() == null) {
                throw exception(STOCK_MOVE_WAREHOUSE_REQUIRED);
            }
            if (item.getFromWarehouseId().equals(item.getToWarehouseId())) {
                throw exception(STOCK_MOVE_WAREHOUSE_SAME);
            }
            warehouseIds.add(item.getFromWarehouseId());
            warehouseIds.add(item.getToWarehouseId());
        }
        warehouseService.validWarehouseList(warehouseIds);
        warehouseService.validateCurrentUserWarehousePermission(warehouseIds);
    }

    private void validateDuplicateStockMoveItems(List<ErpStockMoveSaveReqVO.Item> list) {
        Set<String> keys = new HashSet<>();
        for (ErpStockMoveSaveReqVO.Item item : list) {
            String key = item.getProductId() + "-" + item.getFromWarehouseId() + "-" + item.getToWarehouseId();
            if (!keys.add(key)) {
                throw exception(STOCK_MOVE_ITEM_DUPLICATE, key);
            }
        }
    }

    private void updateStockMoveItemList(Long id, List<ErpStockMoveItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpStockMoveItemDO> oldList = stockMoveItemMapper.selectListByMoveId(id);
        List<List<ErpStockMoveItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setMoveId(id));
            stockMoveItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            stockMoveItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            stockMoveItemMapper.deleteByIds(convertList(diffList.get(2), ErpStockMoveItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStockMove(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpStockMoveDO> stockMoves = stockMoveMapper.selectByIds(ids);
        if (CollUtil.isEmpty(stockMoves)) {
            return;
        }
        stockMoves.forEach(stockMove -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(stockMove.getStatus())) {
                throw exception(STOCK_MOVE_DELETE_FAIL_APPROVE, stockMove.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        stockMoves.forEach(stockMove -> {
            // 2.1 删除出库单
            stockMoveMapper.deleteById(stockMove.getId());
            // 2.2 删除出库单项
            stockMoveItemMapper.deleteByMoveId(stockMove.getId());
            operateLogService.recordDelete(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo());
        });
    }

    private ErpStockMoveDO validateStockMoveExists(Long id) {
        ErpStockMoveDO stockMove = stockMoveMapper.selectById(id);
        if (stockMove == null) {
            throw exception(STOCK_MOVE_NOT_EXISTS);
        }
        return stockMove;
    }

    @Override
    public ErpStockMoveDO getStockMove(Long id) {
        return stockMoveMapper.selectById(id);
    }

    @Override
    public PageResult<ErpStockMoveDO> getStockMovePage(ErpStockMovePageReqVO pageReqVO) {
        return stockMoveMapper.selectPage(pageReqVO);
    }

    // ==================== 出库项 ====================

    @Override
    public List<ErpStockMoveItemDO> getStockMoveItemListByMoveId(Long moveId) {
        return stockMoveItemMapper.selectListByMoveId(moveId);
    }

    @Override
    public List<ErpStockMoveItemDO> getStockMoveItemListByMoveIds(Collection<Long> moveIds) {
        if (CollUtil.isEmpty(moveIds)) {
            return Collections.emptyList();
        }
        return stockMoveItemMapper.selectListByMoveIds(moveIds);
    }

}
