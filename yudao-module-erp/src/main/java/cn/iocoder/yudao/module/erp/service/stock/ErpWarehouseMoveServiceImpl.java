package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveMapper;
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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_WAREHOUSE_MOVE_TYPE;

@Service
@Validated
public class ErpWarehouseMoveServiceImpl implements ErpWarehouseMoveService {

    @Resource
    private ErpWarehouseMoveMapper warehouseMoveMapper;
    @Resource
    private ErpWarehouseMoveItemMapper warehouseMoveItemMapper;
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
    public Long createWarehouseMove(ErpWarehouseMoveSaveReqVO createReqVO) {
        List<ErpWarehouseMoveItemDO> items = validateWarehouseMoveItems(createReqVO);
        String no = noRedisDAO.generate(ErpNoRedisDAO.WAREHOUSE_MOVE_NO_PREFIX);
        if (warehouseMoveMapper.selectByNo(no) != null) {
            throw exception(WAREHOUSE_MOVE_NO_EXISTS);
        }
        ErpWarehouseMoveDO warehouseMove = BeanUtils.toBean(createReqVO, ErpWarehouseMoveDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setTotalCount(getSumValue(items, ErpWarehouseMoveItemDO::getCount, BigDecimal::add, BigDecimal.ZERO))
                .setTotalPrice(getSumValue(items, ErpWarehouseMoveItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO))
                .setTotalCostAmount(getSumValue(items, ErpWarehouseMoveItemDO::getCostAmount, BigDecimal::add, BigDecimal.ZERO)));
        warehouseMoveMapper.insert(warehouseMove);
        items.forEach(item -> item.setMoveId(warehouseMove.getId()));
        warehouseMoveItemMapper.insertBatch(items);
        operateLogService.recordCreate(ERP_WAREHOUSE_MOVE_TYPE, warehouseMove.getId(), warehouseMove.getNo());
        return warehouseMove.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseMove(ErpWarehouseMoveSaveReqVO updateReqVO) {
        ErpWarehouseMoveDO warehouseMove = validateWarehouseMoveExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(warehouseMove.getStatus())) {
            throw exception(WAREHOUSE_MOVE_UPDATE_FAIL_APPROVE, warehouseMove.getNo());
        }
        List<ErpWarehouseMoveItemDO> items = validateWarehouseMoveItems(updateReqVO);
        ErpWarehouseMoveDO updateObj = BeanUtils.toBean(updateReqVO, ErpWarehouseMoveDO.class, in -> in
                .setTotalCount(getSumValue(items, ErpWarehouseMoveItemDO::getCount, BigDecimal::add, BigDecimal.ZERO))
                .setTotalPrice(getSumValue(items, ErpWarehouseMoveItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO))
                .setTotalCostAmount(getSumValue(items, ErpWarehouseMoveItemDO::getCostAmount, BigDecimal::add, BigDecimal.ZERO)));
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(warehouseMove.getDeptId());
        }
        warehouseMoveMapper.updateById(updateObj);
        updateWarehouseMoveItemList(updateReqVO.getId(), items);
        operateLogService.recordUpdate(ERP_WAREHOUSE_MOVE_TYPE, warehouseMove.getId(), warehouseMove.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseMoveStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(WAREHOUSE_MOVE_PROCESS_FAIL);
        }
        ErpWarehouseMoveDO warehouseMove = validateWarehouseMoveExists(id);
        if (warehouseMove.getStatus().equals(status)) {
            throw exception(WAREHOUSE_MOVE_APPROVE_FAIL);
        }
        List<ErpWarehouseMoveItemDO> items = warehouseMoveItemMapper.selectListByMoveId(id);
        validateWarehouseMoveItemsReadyForApprove(warehouseMove, items);
        int updateCount = warehouseMoveMapper.updateByIdAndStatus(id, warehouseMove.getStatus(),
                new ErpWarehouseMoveDO().setStatus(status)
                        .setApproveUserId(getLoginUserId())
                        .setApproveTime(LocalDateTime.now()));
        if (updateCount == 0) {
            throw exception(WAREHOUSE_MOVE_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_WAREHOUSE_MOVE_TYPE, warehouseMove.getId(), warehouseMove.getNo(), true);

        items.forEach(item -> {
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    item.getProductId(), item.getFromWarehouseId(), item.getBatchNo(), item.getCount().negate(),
                    ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_OUT.getType(), item.getMoveId(), item.getId(),
                    warehouseMove.getNo(), null, warehouseMove.getMoveTime()));
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    item.getProductId(), item.getToWarehouseId(), item.getBatchNo(), item.getCount(),
                    ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_IN.getType(), item.getMoveId(), item.getId(),
                    warehouseMove.getNo(), resolveMoveInUnitPrice(item), warehouseMove.getMoveTime()));
        });
    }

    private List<ErpWarehouseMoveItemDO> validateWarehouseMoveItems(ErpWarehouseMoveSaveReqVO reqVO) {
        if (reqVO.getFromWarehouseId() == null || reqVO.getToWarehouseId() == null) {
            throw exception(WAREHOUSE_MOVE_WAREHOUSE_REQUIRED);
        }
        if (reqVO.getFromWarehouseId().equals(reqVO.getToWarehouseId())) {
            throw exception(WAREHOUSE_MOVE_WAREHOUSE_SAME);
        }
        validateDuplicateWarehouseMoveItems(reqVO);
        Set<Long> warehouseIds = new HashSet<>();
        warehouseIds.add(reqVO.getFromWarehouseId());
        warehouseIds.add(reqVO.getToWarehouseId());
        DataPermissionUtils.executeIgnore(() -> warehouseService.validWarehouseList(warehouseIds));
        warehouseService.validateCurrentUserStockMoveFromWarehousePermission(java.util.Collections.singleton(reqVO.getFromWarehouseId()));
        warehouseService.validateCurrentUserWarehousePermission(java.util.Collections.singleton(reqVO.getToWarehouseId()));
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() -> productService.validProductList(
                convertSet(reqVO.getItems(), ErpWarehouseMoveSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        return convertList(reqVO.getItems(), itemReq -> {
            if (itemReq.getCount() == null || itemReq.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(WAREHOUSE_MOVE_ITEM_COUNT_POSITIVE);
            }
            ErpProductDO product = productMap.get(itemReq.getProductId());
            BigDecimal costPrice = itemReq.getCostPrice();
            if (costPrice == null) {
                ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                        stockService.getStock(itemReq.getProductId(), reqVO.getFromWarehouseId()));
                costPrice = stock == null || stock.getCostPrice() == null ? BigDecimal.ZERO : stock.getCostPrice();
            }
            BigDecimal finalCostPrice = costPrice;
            BigDecimal productPrice = itemReq.getProductPrice() == null ? finalCostPrice : itemReq.getProductPrice();
            BigDecimal weight = itemReq.getWeight() == null && product != null ? product.getWeight() : itemReq.getWeight();
            return BeanUtils.toBean(itemReq, ErpWarehouseMoveItemDO.class, item -> item
                    .setFromWarehouseId(reqVO.getFromWarehouseId())
                    .setToWarehouseId(reqVO.getToWarehouseId())
                    .setProductUnitId(product == null ? null : product.getUnitId())
                    .setProductPrice(productPrice)
                    .setTotalPrice(MoneyUtils.priceMultiply(productPrice, itemReq.getCount()))
                    .setCostPrice(finalCostPrice)
                    .setCostAmount(MoneyUtils.priceMultiply(finalCostPrice, itemReq.getCount()))
                    .setWeight(weight)
                    .setTotalWeight(MoneyUtils.priceMultiply(weight, itemReq.getCount())));
        });
    }

    private void validateWarehouseMoveItemsReadyForApprove(ErpWarehouseMoveDO warehouseMove,
                                                           List<ErpWarehouseMoveItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            throw exception(WAREHOUSE_MOVE_NOT_EXISTS);
        }
        validateDuplicateWarehouseMoveItems(warehouseMove, items);
        Set<Long> warehouseIds = new HashSet<>();
        warehouseIds.add(warehouseMove.getFromWarehouseId());
        warehouseIds.add(warehouseMove.getToWarehouseId());
        DataPermissionUtils.executeIgnore(() -> warehouseService.validWarehouseList(warehouseIds));
        warehouseService.validateCurrentUserStockMoveFromWarehousePermission(java.util.Collections.singleton(warehouseMove.getFromWarehouseId()));
        warehouseService.validateCurrentUserWarehousePermission(java.util.Collections.singleton(warehouseMove.getToWarehouseId()));
        productService.validProductList(convertSet(items, ErpWarehouseMoveItemDO::getProductId));
        for (ErpWarehouseMoveItemDO item : items) {
            ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                    stockService.getStock(item.getProductId(), item.getFromWarehouseId()));
            BigDecimal available = stock == null || stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
            if (available.compareTo(item.getCount()) < 0) {
                throw exception(WAREHOUSE_MOVE_STOCK_NOT_ENOUGH, item.getProductId(), available, item.getCount());
            }
        }
    }

    private BigDecimal resolveMoveInUnitPrice(ErpWarehouseMoveItemDO item) {
        if (item.getCostPrice() != null) {
            return item.getCostPrice();
        }
        ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                stockService.getStock(item.getProductId(), item.getFromWarehouseId()));
        if (stock != null && stock.getCostPrice() != null) {
            return stock.getCostPrice();
        }
        return item.getProductPrice();
    }

    private void validateDuplicateWarehouseMoveItems(ErpWarehouseMoveSaveReqVO reqVO) {
        Set<String> keys = new HashSet<>();
        for (ErpWarehouseMoveSaveReqVO.Item item : reqVO.getItems()) {
            String key = item.getProductId() + "-" + reqVO.getFromWarehouseId() + "-"
                    + reqVO.getToWarehouseId() + "-" + (item.getFromShelf() == null ? "" : item.getFromShelf());
            if (!keys.add(key)) {
                throw exception(WAREHOUSE_MOVE_ITEM_DUPLICATE, key);
            }
        }
    }

    private void validateDuplicateWarehouseMoveItems(ErpWarehouseMoveDO warehouseMove,
                                                     List<ErpWarehouseMoveItemDO> items) {
        Set<String> keys = new HashSet<>();
        for (ErpWarehouseMoveItemDO item : items) {
            String key = item.getProductId() + "-" + warehouseMove.getFromWarehouseId() + "-"
                    + warehouseMove.getToWarehouseId() + "-" + (item.getFromShelf() == null ? "" : item.getFromShelf());
            if (!keys.add(key)) {
                throw exception(WAREHOUSE_MOVE_ITEM_DUPLICATE, key);
            }
        }
    }

    private void updateWarehouseMoveItemList(Long id, List<ErpWarehouseMoveItemDO> newList) {
        List<ErpWarehouseMoveItemDO> oldList = warehouseMoveItemMapper.selectListByMoveId(id);
        List<List<ErpWarehouseMoveItemDO>> diffList = diffList(oldList, newList,
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setMoveId(id));
            warehouseMoveItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            warehouseMoveItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            warehouseMoveItemMapper.deleteByIds(convertList(diffList.get(2), ErpWarehouseMoveItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouseMove(List<Long> ids) {
        List<ErpWarehouseMoveDO> moves = warehouseMoveMapper.selectByIds(ids);
        if (CollUtil.isEmpty(moves)) {
            return;
        }
        moves.forEach(move -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(move.getStatus())) {
                throw exception(WAREHOUSE_MOVE_DELETE_FAIL_APPROVE, move.getNo());
            }
        });
        moves.forEach(move -> {
            warehouseMoveMapper.deleteById(move.getId());
            warehouseMoveItemMapper.deleteByMoveId(move.getId());
            operateLogService.recordDelete(ERP_WAREHOUSE_MOVE_TYPE, move.getId(), move.getNo());
        });
    }

    private ErpWarehouseMoveDO validateWarehouseMoveExists(Long id) {
        ErpWarehouseMoveDO warehouseMove = warehouseMoveMapper.selectById(id);
        if (warehouseMove == null) {
            throw exception(WAREHOUSE_MOVE_NOT_EXISTS);
        }
        return warehouseMove;
    }

    @Override
    public ErpWarehouseMoveDO getWarehouseMove(Long id) {
        return warehouseMoveMapper.selectById(id);
    }

    @Override
    public PageResult<ErpWarehouseMoveDO> getWarehouseMovePage(ErpWarehouseMovePageReqVO pageReqVO) {
        return warehouseMoveMapper.selectPage(pageReqVO);
    }

    @Override
    public ErpWarehouseMoveSummaryRespVO getWarehouseMoveSummary(ErpWarehouseMovePageReqVO pageReqVO) {
        List<ErpWarehouseMoveDO> moves = warehouseMoveMapper.selectList(pageReqVO);
        List<ErpWarehouseMoveItemDO> items = getWarehouseMoveItemListByMoveIds(convertSet(moves, ErpWarehouseMoveDO::getId));
        ErpWarehouseMoveSummaryRespVO summary = new ErpWarehouseMoveSummaryRespVO();
        summary.setTotalRows((long) moves.size());
        summary.setTotalItems((long) items.size());
        summary.setTotalCount(getSumValue(moves, ErpWarehouseMoveDO::getTotalCount, BigDecimal::add, BigDecimal.ZERO));
        summary.setTotalPrice(getSumValue(moves, ErpWarehouseMoveDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        summary.setTotalCostAmount(getSumValue(moves, ErpWarehouseMoveDO::getTotalCostAmount, BigDecimal::add, BigDecimal.ZERO));
        return summary;
    }

    @Override
    public List<ErpWarehouseMoveItemDO> getWarehouseMoveItemListByMoveId(Long moveId) {
        return warehouseMoveItemMapper.selectListByMoveId(moveId);
    }

    @Override
    public List<ErpWarehouseMoveItemDO> getWarehouseMoveItemListByMoveIds(Collection<Long> moveIds) {
        return warehouseMoveItemMapper.selectListByMoveIds(moveIds);
    }

}
