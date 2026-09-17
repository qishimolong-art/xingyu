package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPickupReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
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
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
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
    private ErpProductService productService;
    @Resource
    private ErpStockItemSnapshotSupport snapshotSupport;
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
    public PageResult<ErpStockInBillItemDO> getStockInBillItemPage(ErpStockInBillItemPageReqVO pageReqVO) {
        validateStockInBillExists(pageReqVO.getBillId());
        return stockInBillItemMapper.selectPageByBillId(pageReqVO);
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
        throw new cn.iocoder.yudao.framework.common.exception.ServiceException(409,
                "采购领货流程已取消，审核直接入库，不再生成待领货凭证");
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
        Map<Long, ErpProductDO> productMap = convertMap(productService.validProductList(
                convertSet(purchaseInItems, ErpPurchaseInItemDO::getProductId)), ErpProductDO::getId);
        List<ErpStockInBillItemDO> billItems = purchaseInItems.stream()
                .map(item -> buildBillItem(bill, purchaseIn, item, productMap.get(item.getProductId())))
                .collect(Collectors.toList());
        stockInBillItemMapper.insertBatch(billItems);
    }

    private ErpStockInBillItemDO buildBillItem(ErpStockInBillDO bill, ErpPurchaseInDO purchaseIn,
                                               ErpPurchaseInItemDO item, ErpProductDO product) {
        BigDecimal weight = snapshotSupport.resolveWeight(null, product);
        return BeanUtils.toBean(item, ErpStockInBillItemDO.class)
                .setId(null)
                .setBillId(bill.getId())
                .setSourceId(purchaseIn.getId())
                .setSourceItemId(item.getId())
                .setSourceNo(purchaseIn.getNo())
                .setPickedCount(BigDecimal.ZERO)
                .setPackageQty(snapshotSupport.resolvePackageQty(item.getPackageQty(), product))
                .setWeight(weight)
                .setTotalWeight(snapshotSupport.calculateTotalWeight(weight, item.getCount()))
                .setStatus(STATUS_WAIT_PICKUP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickup(ErpStockInBillPickupReqVO reqVO) {
        throw new cn.iocoder.yudao.framework.common.exception.ServiceException(409,
                "采购领货流程已取消；历史入仓凭证仅供核对，不允许由领货入口补记库存");
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
