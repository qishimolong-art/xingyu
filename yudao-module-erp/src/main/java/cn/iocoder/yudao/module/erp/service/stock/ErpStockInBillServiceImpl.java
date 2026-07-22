package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPickupReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillPickupRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInBillMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInBillItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInBillPickupRecordMapper;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_BILL_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_BILL_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_BILL_PICKUP_COUNT_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_BILL_PICKUP_FAIL_COMPLETED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_BILL_PICKUP_ITEM_NOT_EXISTS;

@Service
@Validated
public class ErpStockInBillServiceImpl implements ErpStockInBillService {

    private static final int SOURCE_BIZ_TYPE_PURCHASE_IN = 10;
    public static final int STATUS_WAIT_PICKUP = 10;
    public static final int STATUS_PART_PICKUP = 20;
    public static final int STATUS_DONE = 30;

    @Resource
    private ErpStockInBillMapper stockInBillMapper;
    @Resource
    private ErpStockInBillItemMapper stockInBillItemMapper;
    @Resource
    private ErpStockInBillPickupRecordMapper pickupRecordMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public PageResult<ErpStockInBillDO> getStockInBillPage(ErpStockInBillPageReqVO pageReqVO) {
        return stockInBillMapper.selectPage(pageReqVO);
    }

    @Override
    public ErpStockInBillDO getStockInBill(Long id) {
        return validateStockInBillExists(id);
    }

    @Override
    public List<ErpStockInBillDO> getStockInBillList(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return stockInBillMapper.selectBatchIds(ids);
    }

    @Override
    public List<ErpStockInBillItemDO> getStockInBillItemList(Long billId) {
        return stockInBillItemMapper.selectListByBillId(billId);
    }

    @Override
    public List<ErpStockInBillItemDO> getStockInBillItemListByBillIds(Collection<Long> billIds) {
        if (CollUtil.isEmpty(billIds)) {
            return Collections.emptyList();
        }
        return stockInBillItemMapper.selectListByBillIds(billIds);
    }

    @Override
    public List<ErpStockInBillDO> getStockInBillListByPurchaseInId(Long purchaseInId) {
        return stockInBillMapper.selectListBySource(SOURCE_BIZ_TYPE_PURCHASE_IN, purchaseInId);
    }

    @Override
    public List<ErpStockInBillItemDO> getPurchaseInSourceItemList(Long purchaseInId) {
        List<ErpStockInBillDO> bills = getStockInBillListByPurchaseInId(purchaseInId);
        if (CollUtil.isEmpty(bills)) {
            return Collections.emptyList();
        }
        return stockInBillItemMapper.selectListByBillIds(convertList(bills, ErpStockInBillDO::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFromPurchaseIn(ErpPurchaseInDO purchaseIn, List<ErpPurchaseInItemDO> purchaseInItems) {
        if (CollUtil.isEmpty(purchaseInItems)) {
            return;
        }
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(purchaseInItems, ErpPurchaseInItemDO::getWarehouseId));
        Map<Long, List<ErpPurchaseInItemDO>> itemMap = purchaseInItems.stream()
                .collect(Collectors.groupingBy(ErpPurchaseInItemDO::getWarehouseId));
        itemMap.forEach((warehouseId, items) -> createFromPurchaseInAndWarehouse(purchaseIn, warehouseMap.get(warehouseId), items));
    }

    private void createFromPurchaseInAndWarehouse(ErpPurchaseInDO purchaseIn, ErpWarehouseDO warehouse,
                                                  List<ErpPurchaseInItemDO> purchaseInItems) {
        String no = noRedisDAO.generate(ErpNoRedisDAO.STOCK_IN_BILL_NO_PREFIX);
        if (stockInBillMapper.selectByNo(no) != null) {
            throw exception(STOCK_IN_BILL_NO_EXISTS);
        }
        BigDecimal totalCount = sumCount(purchaseInItems, ErpPurchaseInItemDO::getCount);
        ErpStockInBillDO bill = new ErpStockInBillDO()
                .setNo(no)
                .setPickupFlag(false)
                .setPickup("待提货")
                .setBillDate(purchaseIn.getInTime())
                .setWarehouseId(purchaseInItems.get(0).getWarehouseId())
                .setSourceUnitName(null)
                .setSourceNo(purchaseIn.getNo())
                .setSourceBizType(SOURCE_BIZ_TYPE_PURCHASE_IN)
                .setSourceId(purchaseIn.getId())
                .setStatus(STATUS_WAIT_PICKUP)
                .setAuditorName(purchaseIn.getUpdater())
                .setAuditTime(purchaseIn.getUpdateTime())
                .setPrintCount(0)
                .setTotalWeight(BigDecimal.ZERO)
                .setRemark(purchaseIn.getRemark())
                .setTimeoutFlag(false)
                .setWholeQty(sumWholeQty(purchaseInItems))
                .setLooseQty(BigDecimal.ZERO)
                .setTotalCount(totalCount)
                .setPickedCount(BigDecimal.ZERO);
        if (warehouse != null) {
            bill.setRemark(purchaseIn.getRemark());
        }
        stockInBillMapper.insert(bill);
        List<ErpStockInBillItemDO> billItems = purchaseInItems.stream()
                .map(item -> buildBillItem(bill, purchaseIn, item))
                .collect(Collectors.toList());
        stockInBillItemMapper.insertBatch(billItems);
    }

    private ErpStockInBillItemDO buildBillItem(ErpStockInBillDO bill, ErpPurchaseInDO purchaseIn,
                                               ErpPurchaseInItemDO item) {
        return BeanUtils.toBean(item, ErpStockInBillItemDO.class)
                .setId(null)
                .setBillId(bill.getId())
                .setSourceId(purchaseIn.getId())
                .setSourceItemId(item.getId())
                .setSourceNo(purchaseIn.getNo())
                .setPickedCount(BigDecimal.ZERO)
                .setStatus(STATUS_WAIT_PICKUP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickup(ErpStockInBillPickupReqVO reqVO) {
        ErpStockInBillDO bill = validateStockInBillExists(reqVO.getId());
        if (Integer.valueOf(STATUS_DONE).equals(bill.getStatus())) {
            throw exception(STOCK_IN_BILL_PICKUP_FAIL_COMPLETED, bill.getNo());
        }
        List<ErpStockInBillItemDO> billItems = stockInBillItemMapper.selectListByBillId(reqVO.getId());
        Map<Long, ErpStockInBillItemDO> billItemMap = convertMap(billItems, ErpStockInBillItemDO::getId);
        LocalDateTime pickupTime = reqVO.getPickupTime() != null ? reqVO.getPickupTime() : LocalDateTime.now();
        Long pickupUserId = SecurityFrameworkUtils.getLoginUserId();
        String pickupUserName = getUserName(pickupUserId);

        List<ErpStockInBillPickupRecordDO> records = new ArrayList<>();
        for (ErpStockInBillPickupReqVO.Item reqItem : reqVO.getItems()) {
            ErpStockInBillItemDO billItem = billItemMap.get(reqItem.getItemId());
            if (billItem == null) {
                throw exception(STOCK_IN_BILL_PICKUP_ITEM_NOT_EXISTS, reqItem.getItemId());
            }
            BigDecimal remainCount = remainCount(billItem);
            if (reqItem.getPickupCount().compareTo(remainCount) > 0) {
                throw exception(STOCK_IN_BILL_PICKUP_COUNT_EXCEED,
                        billItem.getId(), reqItem.getPickupCount(), remainCount);
            }
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    billItem.getProductId(), billItem.getWarehouseId(), billItem.getBatchNo(), reqItem.getPickupCount(),
                    ErpStockRecordBizTypeEnum.PURCHASE_IN.getType(), bill.getSourceId(), billItem.getSourceItemId(), bill.getSourceNo(),
                    billItem.getProductPrice(), pickupTime));
            BigDecimal pickedCount = nullToZero(billItem.getPickedCount()).add(reqItem.getPickupCount());
            int itemStatus = pickedCount.compareTo(nullToZero(billItem.getCount())) >= 0 ? STATUS_DONE : STATUS_PART_PICKUP;
            stockInBillItemMapper.updateById(new ErpStockInBillItemDO()
                    .setId(billItem.getId())
                    .setPickedCount(pickedCount)
                    .setStatus(itemStatus));
            billItem.setPickedCount(pickedCount).setStatus(itemStatus);
            records.add(new ErpStockInBillPickupRecordDO()
                    .setBillId(bill.getId())
                    .setBillItemId(billItem.getId())
                    .setSourceId(billItem.getSourceId())
                    .setSourceItemId(billItem.getSourceItemId())
                    .setProductId(billItem.getProductId())
                    .setWarehouseId(billItem.getWarehouseId())
                    .setPickupCount(reqItem.getPickupCount())
                    .setPickupUserId(pickupUserId)
                    .setPickupUserName(pickupUserName)
                    .setPickupTime(pickupTime)
                    .setRemark(reqVO.getRemark()));
        }
        if (CollUtil.isNotEmpty(records)) {
            pickupRecordMapper.insertBatch(records);
        }
        updateBillPickupStatus(bill, billItems, pickupUserName, pickupTime);
    }

    private void updateBillPickupStatus(ErpStockInBillDO bill, List<ErpStockInBillItemDO> billItems,
                                        String pickupUserName, LocalDateTime pickupTime) {
        BigDecimal totalCount = sumCount(billItems, ErpStockInBillItemDO::getCount);
        BigDecimal pickedCount = sumCount(billItems, ErpStockInBillItemDO::getPickedCount);
        int status = pickedCount.compareTo(BigDecimal.ZERO) <= 0 ? STATUS_WAIT_PICKUP
                : (pickedCount.compareTo(totalCount) >= 0 ? STATUS_DONE : STATUS_PART_PICKUP);
        stockInBillMapper.updateById(new ErpStockInBillDO()
                .setId(bill.getId())
                .setStatus(status)
                .setPickupFlag(status == STATUS_DONE)
                .setPickup(status == STATUS_DONE ? "已提货" : "部分提货")
                .setPickupUserName(pickupUserName)
                .setPickedCount(pickedCount)
                .setTotalCount(totalCount));
    }

    private ErpStockInBillDO validateStockInBillExists(Long id) {
        ErpStockInBillDO bill = stockInBillMapper.selectById(id);
        if (bill == null) {
            throw exception(STOCK_IN_BILL_NOT_EXISTS);
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

    private BigDecimal remainCount(ErpStockInBillItemDO item) {
        return nullToZero(item.getCount()).subtract(nullToZero(item.getPickedCount()));
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static BigDecimal sumWholeQty(Collection<ErpPurchaseInItemDO> items) {
        return items.stream()
                .map(item -> item.getWholeQty() == null ? BigDecimal.ZERO : BigDecimal.valueOf(item.getWholeQty()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static <T> BigDecimal sumCount(Collection<T> items, java.util.function.Function<T, BigDecimal> getter) {
        return items.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
