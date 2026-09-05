package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPickReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillPickRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutBillItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutBillMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutBillPickRecordMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_OUT_BILL_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_OUT_BILL_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_OUT_BILL_PICK_COUNT_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_OUT_BILL_PICK_FAIL_COMPLETED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_OUT_BILL_PICK_ITEM_NOT_EXISTS;

@Service
@Validated
public class ErpStockOutBillServiceImpl implements ErpStockOutBillService {

    private static final int SOURCE_BIZ_TYPE_SALE_OUT = 20;
    public static final int STATUS_WAIT_PICK = 10;
    public static final int STATUS_PART_PICK = 20;
    public static final int STATUS_DONE = 30;

    @Resource
    private ErpStockOutBillMapper stockOutBillMapper;
    @Resource
    private ErpStockOutBillItemMapper stockOutBillItemMapper;
    @Resource
    private ErpStockOutBillPickRecordMapper pickRecordMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpStockItemSnapshotSupport snapshotSupport;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public PageResult<ErpStockOutBillDO> getStockOutBillPage(ErpStockOutBillPageReqVO pageReqVO) {
        return stockOutBillMapper.selectPage(pageReqVO);
    }

    @Override
    public ErpStockOutBillDO getStockOutBill(Long id) {
        return validateStockOutBillExists(id);
    }

    @Override
    public List<ErpStockOutBillDO> getStockOutBillList(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return stockOutBillMapper.selectBatchIds(ids);
    }

    @Override
    public List<ErpStockOutBillItemDO> getStockOutBillItemList(Long billId) {
        return stockOutBillItemMapper.selectListByBillId(billId);
    }

    @Override
    public PageResult<ErpStockOutBillItemDO> getStockOutBillItemPage(ErpStockOutBillItemPageReqVO pageReqVO) {
        validateStockOutBillExists(pageReqVO.getBillId());
        return stockOutBillItemMapper.selectPageByBillId(pageReqVO);
    }

    @Override
    public List<ErpStockOutBillItemDO> getStockOutBillItemListByBillIds(Collection<Long> billIds) {
        if (CollUtil.isEmpty(billIds)) {
            return Collections.emptyList();
        }
        return stockOutBillItemMapper.selectListByBillIds(billIds);
    }

    @Override
    public List<ErpStockOutBillDO> getStockOutBillListBySaleOutId(Long saleOutId) {
        return stockOutBillMapper.selectListBySource(SOURCE_BIZ_TYPE_SALE_OUT, saleOutId);
    }

    @Override
    public List<ErpStockOutBillDO> getStockOutBillListBySaleOutIds(Collection<Long> saleOutIds) {
        return stockOutBillMapper.selectListBySources(SOURCE_BIZ_TYPE_SALE_OUT, saleOutIds);
    }

    @Override
    public List<ErpStockOutBillItemDO> getSaleOutSourceItemList(Long saleOutId) {
        List<ErpStockOutBillDO> bills = getStockOutBillListBySaleOutId(saleOutId);
        if (CollUtil.isEmpty(bills)) {
            return Collections.emptyList();
        }
        return stockOutBillItemMapper.selectListByBillIds(convertList(bills, ErpStockOutBillDO::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFromSaleOut(ErpSaleOutDO saleOut, List<ErpSaleOutItemDO> saleOutItems) {
        if (saleOut == null || CollUtil.isEmpty(saleOutItems)) {
            return;
        }
        if (CollUtil.isNotEmpty(stockOutBillMapper.selectListBySource(SOURCE_BIZ_TYPE_SALE_OUT, saleOut.getId()))) {
            return;
        }
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(saleOutItems, ErpSaleOutItemDO::getWarehouseId));
        Map<Long, List<ErpSaleOutItemDO>> itemMap = saleOutItems.stream()
                .collect(Collectors.groupingBy(ErpSaleOutItemDO::getWarehouseId));
        itemMap.forEach((warehouseId, items) -> createFromSaleOutAndWarehouse(saleOut, warehouseMap.get(warehouseId), items));
    }

    private void createFromSaleOutAndWarehouse(ErpSaleOutDO saleOut, ErpWarehouseDO warehouse,
                                               List<ErpSaleOutItemDO> saleOutItems) {
        String no = noRedisDAO.generate(ErpNoRedisDAO.STOCK_OUT_BILL_NO_PREFIX);
        if (stockOutBillMapper.selectByNo(no) != null) {
            throw exception(STOCK_OUT_BILL_NO_EXISTS);
        }
        BigDecimal totalCount = sumCount(saleOutItems, ErpSaleOutItemDO::getCount);
        ErpStockOutBillDO bill = new ErpStockOutBillDO()
                .setNo(no)
                .setPickFlag(false)
                .setPick("待拣货")
                .setBillDate(saleOut.getOutTime())
                .setWarehouseId(saleOutItems.get(0).getWarehouseId())
                .setSourceUnitName(null)
                .setSourceNo(saleOut.getNo())
                .setSourceBizType(SOURCE_BIZ_TYPE_SALE_OUT)
                .setSourceId(saleOut.getId())
                .setStatus(STATUS_WAIT_PICK)
                .setAuditorName(saleOut.getUpdater())
                .setAuditTime(saleOut.getUpdateTime())
                .setPrintCount(0)
                .setTotalWeight(sumCount(saleOutItems, ErpSaleOutItemDO::getTotalWeight))
                .setRemark(saleOut.getRemark())
                .setTimeoutFlag(false)
                .setWholeQty(BigDecimal.ZERO)
                .setLooseQty(BigDecimal.ZERO)
                .setTotalCount(totalCount)
                .setPickedCount(BigDecimal.ZERO);
        if (warehouse != null) {
            bill.setWarehouseName(warehouse.getName());
        }
        stockOutBillMapper.insert(bill);
        List<ErpStockOutBillItemDO> billItems = saleOutItems.stream()
                .map(item -> buildBillItem(bill, saleOut, item))
                .collect(Collectors.toList());
        stockOutBillItemMapper.insertBatch(billItems);
    }

    private ErpStockOutBillItemDO buildBillItem(ErpStockOutBillDO bill, ErpSaleOutDO saleOut,
                                                ErpSaleOutItemDO item) {
        return BeanUtils.toBean(item, ErpStockOutBillItemDO.class)
                .setId(null)
                .setBillId(bill.getId())
                .setSourceId(saleOut.getId())
                .setSourceItemId(item.getId())
                .setSourceNo(saleOut.getNo())
                .setPickedCount(BigDecimal.ZERO)
                .setWeight(item.getUnitWeight())
                .setTotalWeight(item.getTotalWeight())
                .setStatus(STATUS_WAIT_PICK);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pick(ErpStockOutBillPickReqVO reqVO) {
        ErpStockOutBillDO bill = validateStockOutBillExists(reqVO.getId());
        if (Integer.valueOf(STATUS_DONE).equals(bill.getStatus())) {
            throw exception(STOCK_OUT_BILL_PICK_FAIL_COMPLETED, bill.getNo());
        }
        List<ErpStockOutBillItemDO> billItems = stockOutBillItemMapper.selectListByBillId(reqVO.getId());
        warehouseService.validateCurrentUserWarehousePermission(convertSet(billItems, ErpStockOutBillItemDO::getWarehouseId));
        Map<Long, ErpStockOutBillItemDO> billItemMap = convertMap(billItems, ErpStockOutBillItemDO::getId);
        LocalDateTime pickTime = reqVO.getPickTime() != null ? reqVO.getPickTime() : LocalDateTime.now();
        Long pickUserId = SecurityFrameworkUtils.getLoginUserId();
        String pickUserName = getUserName(pickUserId);

        List<ErpStockOutBillPickRecordDO> records = new ArrayList<>();
        for (ErpStockOutBillPickReqVO.Item reqItem : reqVO.getItems()) {
            ErpStockOutBillItemDO billItem = billItemMap.get(reqItem.getItemId());
            if (billItem == null) {
                throw exception(STOCK_OUT_BILL_PICK_ITEM_NOT_EXISTS, reqItem.getItemId());
            }
            BigDecimal oldPickedCount = nullToZero(billItem.getPickedCount());
            BigDecimal remainCount = nullToZero(billItem.getCount()).subtract(oldPickedCount);
            if (reqItem.getPickCount().compareTo(remainCount) > 0) {
                throw exception(STOCK_OUT_BILL_PICK_COUNT_EXCEED,
                        billItem.getId(), reqItem.getPickCount(), remainCount);
            }
            BigDecimal pickCount = reqItem.getPickCount().negate();
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    billItem.getProductId(), billItem.getWarehouseId(), billItem.getBatchNo(),
                    billItem.getProductUnitId(), billItem.getPackageQty(), billItem.getWeight(),
                    snapshotSupport.calculateTotalWeight(billItem.getWeight(), pickCount), pickCount,
                    ErpStockRecordBizTypeEnum.SALE_OUT.getType(), bill.getSourceId(), billItem.getSourceItemId(), bill.getSourceNo(),
                    billItem.getProductPrice(), pickTime));
            BigDecimal pickedCount = oldPickedCount.add(reqItem.getPickCount());
            int itemStatus = pickedCount.compareTo(nullToZero(billItem.getCount())) >= 0 ? STATUS_DONE : STATUS_PART_PICK;
            int updateCount = stockOutBillItemMapper.updatePickedCountByIdAndPickedCount(
                    billItem.getId(), oldPickedCount, pickedCount, itemStatus);
            if (updateCount == 0) {
                throw exception(STOCK_OUT_BILL_PICK_COUNT_EXCEED,
                        billItem.getId(), reqItem.getPickCount(), remainCount);
            }
            billItem.setPickedCount(pickedCount).setStatus(itemStatus);
            records.add(new ErpStockOutBillPickRecordDO()
                    .setBillId(bill.getId())
                    .setBillItemId(billItem.getId())
                    .setSourceId(billItem.getSourceId())
                    .setSourceItemId(billItem.getSourceItemId())
                    .setProductId(billItem.getProductId())
                    .setWarehouseId(billItem.getWarehouseId())
                    .setPickCount(reqItem.getPickCount())
                    .setPickUserId(pickUserId)
                    .setPickUserName(pickUserName)
                    .setPickTime(pickTime)
                    .setRemark(reqVO.getRemark()));
        }
        if (CollUtil.isNotEmpty(records)) {
            pickRecordMapper.insertBatch(records);
        }
        updateBillPickStatus(bill, pickUserName);
    }

    private void updateBillPickStatus(ErpStockOutBillDO bill, String pickUserName) {
        List<ErpStockOutBillItemDO> billItems = stockOutBillItemMapper.selectListByBillId(bill.getId());
        BigDecimal totalCount = sumCount(billItems, ErpStockOutBillItemDO::getCount);
        BigDecimal pickedCount = sumCount(billItems, ErpStockOutBillItemDO::getPickedCount);
        int status = pickedCount.compareTo(BigDecimal.ZERO) <= 0 ? STATUS_WAIT_PICK
                : (pickedCount.compareTo(totalCount) >= 0 ? STATUS_DONE : STATUS_PART_PICK);
        stockOutBillMapper.updateById(new ErpStockOutBillDO()
                .setId(bill.getId())
                .setStatus(status)
                .setPickFlag(status == STATUS_DONE)
                .setPick(status == STATUS_DONE ? "已完成" : (status == STATUS_WAIT_PICK ? "待拣货" : "部分拣货"))
                .setPickUserName(pickUserName)
                .setPickedCount(pickedCount)
                .setTotalCount(totalCount));
    }

    private ErpStockOutBillDO validateStockOutBillExists(Long id) {
        ErpStockOutBillDO bill = stockOutBillMapper.selectById(id);
        if (bill == null) {
            throw exception(STOCK_OUT_BILL_NOT_EXISTS);
        }
        return bill;
    }

    private String getUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user == null ? String.valueOf(userId) : user.getNickname();
    }

    private BigDecimal remainCount(ErpStockOutBillItemDO item) {
        return nullToZero(item.getCount()).subtract(nullToZero(item.getPickedCount()));
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static <T> BigDecimal sumCount(Collection<T> items, java.util.function.Function<T, BigDecimal> getter) {
        return items.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
