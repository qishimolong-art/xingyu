package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordSummaryVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ERP 产品库存明细 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpStockRecordServiceImpl implements ErpStockRecordService {

    @Resource
    private ErpStockRecordMapper stockRecordMapper;

    @Resource
    private ErpStockService stockService;

    @Resource
    private ErpProductMapper productMapper;

    @Override
    public ErpStockRecordDO getStockRecord(Long id) {
        return stockRecordMapper.selectById(id);
    }

    @Override
    public PageResult<ErpStockRecordDO> getStockRecordPage(ErpStockRecordPageReqVO pageReqVO) {
        Collection<Long> productIdFilter = resolveProductIdFilter(pageReqVO);
        return stockRecordMapper.selectPageWithProductFilter(pageReqVO, productIdFilter);
    }

    @Override
    public ErpStockRecordSummaryVO getStockRecordSummary(ErpStockRecordPageReqVO reqVO) {
        ErpStockRecordSummaryVO summary = new ErpStockRecordSummaryVO();
        Collection<Long> productIdFilter = resolveProductIdFilter(reqVO);
        // 若产品维度条件命中 0 条，直接返回空汇总
        if (productIdFilter != null && productIdFilter.isEmpty()) {
            return summary;
        }
        // 查全部满足条件的流水（临时把分页大小设为不限）
        Integer origPageSize = reqVO.getPageSize();
        Integer origPageNo = reqVO.getPageNo();
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        reqVO.setPageNo(1);
        try {
            PageResult<ErpStockRecordDO> page = stockRecordMapper.selectPageWithProductFilter(reqVO, productIdFilter);
            for (ErpStockRecordDO r : page.getList()) {
                BigDecimal count = r.getCount() != null ? r.getCount() : BigDecimal.ZERO;
                BigDecimal unitPrice = r.getUnitPrice() != null ? r.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal totalPrice = r.getTotalPrice() != null ? r.getTotalPrice() : BigDecimal.ZERO;
                int sign = count.compareTo(BigDecimal.ZERO);
                if (sign > 0) {
                    summary.setTotalInCount(summary.getTotalInCount().add(count));
                    BigDecimal inAmount = totalPrice.compareTo(BigDecimal.ZERO) != 0
                            ? totalPrice : count.multiply(unitPrice);
                    summary.setTotalInAmount(summary.getTotalInAmount().add(inAmount));
                } else if (sign < 0) {
                    BigDecimal abs = count.abs();
                    summary.setTotalOutCount(summary.getTotalOutCount().add(abs));
                    BigDecimal outAmount = totalPrice.compareTo(BigDecimal.ZERO) != 0
                            ? totalPrice.abs() : abs.multiply(unitPrice);
                    summary.setTotalOutAmount(summary.getTotalOutAmount().add(outAmount));
                }
            }
            summary.setRecordCount((long) page.getList().size());
        } finally {
            reqVO.setPageSize(origPageSize);
            reqVO.setPageNo(origPageNo);
        }
        return summary;
    }

    /**
     * 若 reqVO 有产品维度条件，预查 productIds；无条件返回 null 表示不过滤
     */
    private Collection<Long> resolveProductIdFilter(ErpStockRecordPageReqVO reqVO) {
        boolean hasCondition = (reqVO.getProductCode() != null && !reqVO.getProductCode().isEmpty())
                || (reqVO.getProductName() != null && !reqVO.getProductName().isEmpty())
                || (reqVO.getVehicleModel() != null && !reqVO.getVehicleModel().isEmpty())
                || (reqVO.getOriginPlace() != null && !reqVO.getOriginPlace().isEmpty());
        if (!hasCondition) {
            return null;
        }
        LambdaQueryWrapper<ErpProductDO> w = new LambdaQueryWrapper<>();
        w.select(ErpProductDO::getId);
        if (reqVO.getProductCode() != null && !reqVO.getProductCode().isEmpty()) {
            w.like(ErpProductDO::getCode, reqVO.getProductCode());
        }
        if (reqVO.getProductName() != null && !reqVO.getProductName().isEmpty()) {
            w.like(ErpProductDO::getName, reqVO.getProductName());
        }
        if (reqVO.getVehicleModel() != null && !reqVO.getVehicleModel().isEmpty()) {
            w.like(ErpProductDO::getVehicleModel, reqVO.getVehicleModel());
        }
        if (reqVO.getOriginPlace() != null && !reqVO.getOriginPlace().isEmpty()) {
            w.like(ErpProductDO::getOriginPlace, reqVO.getOriginPlace());
        }
        List<Map<String, Object>> rows = productMapper.selectMaps(w);
        return rows.stream().map(m -> (Long) m.get("id")).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createStockRecord(ErpStockRecordCreateReqBO createReqBO) {
        // 1. 决定业务发生日期：优先用 BO 的 bizDate，其次 LocalDateTime.now()
        LocalDateTime bizDate = createReqBO.getBizDate() != null ? createReqBO.getBizDate() : LocalDateTime.now();

        // 2. 决定本次业务单价：出库未传单价 → 取当前成本均价作为本次出库单价
        BigDecimal businessUnitPrice = createReqBO.getUnitPrice();
        if (createReqBO.getCount().compareTo(BigDecimal.ZERO) < 0 && businessUnitPrice == null) {
            ErpStockDO stock = stockService.getStock(createReqBO.getProductId(), createReqBO.getWarehouseId());
            businessUnitPrice = stock != null && stock.getCostPrice() != null ? stock.getCostPrice() : BigDecimal.ZERO;
        }

        // 3. 更新库存 + 成本均价（走移动加权平均算法）
        ErpStockService.StockUpdateResult result = stockService.updateStockCountAndCost(
                createReqBO.getProductId(), createReqBO.getWarehouseId(),
                createReqBO.getCount(), createReqBO.getUnitPrice());

        // 4. 计算本次业务金额
        BigDecimal totalPrice = businessUnitPrice == null ? null
                : MoneyUtils.priceMultiply(businessUnitPrice, createReqBO.getCount());

        // 5. 计算结存金额
        BigDecimal costAmount = result.getCostPrice() == null ? BigDecimal.ZERO
                : MoneyUtils.priceMultiply(result.getCostPrice(), result.getTotalCount());
        if (costAmount == null) {
            costAmount = BigDecimal.ZERO;
        }

        // 6. 落流水
        ErpStockRecordDO stockRecord = BeanUtils.toBean(createReqBO, ErpStockRecordDO.class)
                .setTotalCount(result.getTotalCount())
                .setUnitPrice(businessUnitPrice)
                .setTotalPrice(totalPrice)
                .setCostPrice(result.getCostPrice())
                .setCostAmount(costAmount)
                .setBizDate(bizDate);
        stockRecordMapper.insert(stockRecord);
    }

}
