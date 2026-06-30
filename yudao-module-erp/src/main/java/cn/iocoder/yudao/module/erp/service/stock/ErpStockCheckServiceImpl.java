package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_STOCK_CHECK_TYPE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

// TODO 芋艿：记录操作日志

/**
 * ERP 库存盘点单 Service 实现类
 *
 * @author 芋道源码
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
    private ErpOperateLogService operateLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockCheck(ErpStockCheckSaveReqVO createReqVO) {
        // 1.1 校验盘点项的有效性
        List<ErpStockCheckItemDO> stockCheckItems = validateStockCheckItems(createReqVO.getItems());
        // 1.2 生成盘点单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.STOCK_CHECK_NO_PREFIX);
        if (stockCheckMapper.selectByNo(no) != null) {
            throw exception(STOCK_CHECK_NO_EXISTS);
        }
        if (createReqVO.getDeptId() == null) {
            createReqVO.setDeptId(getLoginUserDeptId());
        }

        // 2.1 插入盘点单
        ErpStockCheckDO stockCheck = BeanUtils.toBean(createReqVO, ErpStockCheckDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setTotalCount(getSumValue(stockCheckItems, ErpStockCheckItemDO::getCount, BigDecimal::add))
                .setTotalPrice(getSumValue(stockCheckItems, ErpStockCheckItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO)));
        stockCheckMapper.insert(stockCheck);
        // 2.2 插入盘点单项
        stockCheckItems.forEach(o -> o.setCheckId(stockCheck.getId()));
        stockCheckItemMapper.insertBatch(stockCheckItems);
        operateLogService.recordCreate(ERP_STOCK_CHECK_TYPE, stockCheck.getId(), stockCheck.getNo());
        return stockCheck.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal createAndApproveStockAdjustCheck(ErpStockAdjustReqVO reqVO) {
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(reqVO.getProductId(), reqVO.getWarehouseId());
        BigDecimal stockCount = stock != null && stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
        BigDecimal actualCount = reqVO.getTargetCount();
        BigDecimal count = actualCount.subtract(stockCount);

        ErpProductDO product = productService.getProduct(reqVO.getProductId());
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
        ErpWarehouseDO warehouse = warehouseService.getWarehouse(reqVO.getWarehouseId());
        createReqVO.setDeptId(warehouse != null ? warehouse.getDeptId() : null);
        createReqVO.setRemark(remark);
        createReqVO.setItems(new ArrayList<>(Collections.singletonList(item)));
        Long checkId = createStockCheck(createReqVO);
        updateStockCheckStatus(checkId, ErpAuditStatus.APPROVE.getStatus());
        return actualCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockCheck(ErpStockCheckSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpStockCheckDO stockCheck = validateStockCheckExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(stockCheck.getStatus())) {
            throw exception(STOCK_CHECK_UPDATE_FAIL_APPROVE, stockCheck.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(FIELD_PERMISSION_MODULE, updateReqVO, stockCheck);
        fieldPermissionMasker.preserveHiddenItemFields(FIELD_PERMISSION_MODULE, updateReqVO.getItems(),
                stockCheckItemMapper.selectListByCheckId(updateReqVO.getId()));
        // 1.2 校验盘点项的有效性
        List<ErpStockCheckItemDO> stockCheckItems = validateStockCheckItems(updateReqVO.getItems());

        // 2.1 更新盘点单
        ErpStockCheckDO updateObj = BeanUtils.toBean(updateReqVO, ErpStockCheckDO.class, in -> in
                .setTotalCount(getSumValue(stockCheckItems, ErpStockCheckItemDO::getCount, BigDecimal::add))
                .setTotalPrice(getSumValue(stockCheckItems, ErpStockCheckItemDO::getTotalPrice, BigDecimal::add)));
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(stockCheck.getDeptId());
        }
        stockCheckMapper.updateById(updateObj);
        // 2.2 更新盘点单项
        updateStockCheckItemList(updateReqVO.getId(), stockCheckItems);
        operateLogService.recordUpdate(ERP_STOCK_CHECK_TYPE, stockCheck.getId(), stockCheck.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockCheckStatus(Long id, Integer status) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(STOCK_CHECK_PROCESS_FAIL);
        }
        // 1.1 校验存在
        ErpStockCheckDO stockCheck = validateStockCheckExists(id);
        // 1.2 校验状态
        if (stockCheck.getStatus().equals(status)) {
            throw exception(STOCK_CHECK_APPROVE_FAIL);
        }

        // 2. 更新状态
        int updateCount = stockCheckMapper.updateByIdAndStatus(id, stockCheck.getStatus(),
                new ErpStockCheckDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(STOCK_CHECK_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_STOCK_CHECK_TYPE, stockCheck.getId(), stockCheck.getNo(), true);

        // 3. 变更库存
        List<ErpStockCheckItemDO> stockCheckItems = stockCheckItemMapper.selectListByCheckId(id);
        warehouseService.validateCurrentUserWarehousePermission(convertSet(stockCheckItems, ErpStockCheckItemDO::getWarehouseId));
        stockCheckItems.forEach(stockCheckItem -> {
            // 没有盈亏，不用出入库
            if (stockCheckItem.getCount().compareTo(BigDecimal.ZERO) == 0) {
                return;
            }
            BigDecimal count = stockCheckItem.getCount();
            Integer bizType = count.compareTo(BigDecimal.ZERO) > 0 ? ErpStockRecordBizTypeEnum.CHECK_MORE_IN.getType()
                    : ErpStockRecordBizTypeEnum.CHECK_LESS_OUT.getType();
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    stockCheckItem.getProductId(), stockCheckItem.getWarehouseId(), count,
                    bizType, stockCheckItem.getCheckId(), stockCheckItem.getId(), stockCheck.getNo(),
                    count.compareTo(BigDecimal.ZERO) > 0 ? stockCheckItem.getProductPrice() : null,
                    stockCheck.getCheckTime()));
        });
    }

    private List<ErpStockCheckItemDO> validateStockCheckItems(List<ErpStockCheckSaveReqVO.Item> list) {
        validateDuplicateStockCheckItems(list);
        // 1.1 校验产品存在
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpStockCheckSaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 1.2 校验仓库存在
        Set<Long> warehouseIds = convertSet(list, ErpStockCheckSaveReqVO.Item::getWarehouseId);
        warehouseService.validWarehouseList(warehouseIds);
        warehouseService.validateCurrentUserWarehousePermission(warehouseIds);
        // 2. 转化为 ErpStockCheckItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpStockCheckItemDO.class, item -> {
            BigDecimal stockCount = item.getStockCount() != null ? item.getStockCount() : BigDecimal.ZERO;
            BigDecimal actualCount = item.getActualCount() != null ? item.getActualCount() : BigDecimal.ZERO;
            BigDecimal count = actualCount.subtract(stockCount);
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            item.setCount(count);
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), count));
        }));
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
                ? reqVO.getReason() : "库存浏览调整";
        if (reqVO.getRemark() == null || reqVO.getRemark().isEmpty()) {
            return reason;
        }
        return reason + "：" + reqVO.getRemark();
    }

    private void validateDuplicateStockCheckItems(List<ErpStockCheckSaveReqVO.Item> list) {
        Set<String> keys = new HashSet<>();
        for (ErpStockCheckSaveReqVO.Item item : list) {
            String key = item.getProductId() + "-" + item.getWarehouseId();
            if (!keys.add(key)) {
                throw exception(STOCK_CHECK_ITEM_DUPLICATE, key);
            }
        }
    }

    private void updateStockCheckItemList(Long id, List<ErpStockCheckItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpStockCheckItemDO> oldList = stockCheckItemMapper.selectListByCheckId(id);
        List<List<ErpStockCheckItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
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
        // 1. 校验不处于已审批
        List<ErpStockCheckDO> stockChecks = stockCheckMapper.selectByIds(ids);
        if (CollUtil.isEmpty(stockChecks)) {
            return;
        }
        stockChecks.forEach(stockCheck -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(stockCheck.getStatus())) {
                throw exception(STOCK_CHECK_DELETE_FAIL_APPROVE, stockCheck.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        stockChecks.forEach(stockCheck -> {
            // 2.1 删除盘点单
            stockCheckMapper.deleteById(stockCheck.getId());
            // 2.2 删除盘点单项
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

    // ==================== 盘点项 ====================

    @Override
    public List<ErpStockCheckItemDO> getStockCheckItemListByCheckId(Long checkId) {
        return stockCheckItemMapper.selectListByCheckId(checkId);
    }

    @Override
    public List<ErpStockCheckItemDO> getStockCheckItemListByCheckIds(Collection<Long> checkIds) {
        if (CollUtil.isEmpty(checkIds)) {
            return Collections.emptyList();
        }
        return stockCheckItemMapper.selectListByCheckIds(checkIds);
    }

}
