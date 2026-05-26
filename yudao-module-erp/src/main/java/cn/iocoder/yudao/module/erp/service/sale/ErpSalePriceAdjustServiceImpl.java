package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSaleOutItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 销售调价单 Service 实现类
 *
 * @author 汽配ERP
 */
@Service
@Validated
public class ErpSalePriceAdjustServiceImpl implements ErpSalePriceAdjustService {

    @Resource
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;
    @Resource
    private ErpSalePriceAdjustItemMapper salePriceAdjustItemMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpProductService productService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSalePriceAdjust(ErpSalePriceAdjustSaveReqVO createReqVO) {
        // 1. 生成调价单号
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_PRICE_ADJUST_NO_PREFIX);
        // 2. 插入调价单
        ErpSalePriceAdjustDO adjustDO = BeanUtils.toBean(createReqVO, ErpSalePriceAdjustDO.class);
        adjustDO.setNo(no);
        adjustDO.setStatus(ErpAuditStatus.PROCESS.getStatus());
        // 计算调价总金额
        BigDecimal totalAdjustPrice = BigDecimal.ZERO;
        List<ErpSalePriceAdjustItemDO> items = BeanUtils.toBean(createReqVO.getItems(), ErpSalePriceAdjustItemDO.class);
        for (ErpSalePriceAdjustItemDO item : items) {
            BigDecimal adjustPrice = calculateAdjustPrice(item);
            item.setAdjustPrice(adjustPrice);
            totalAdjustPrice = totalAdjustPrice.add(adjustPrice);
        }
        adjustDO.setTotalAdjustPrice(totalAdjustPrice);
        salePriceAdjustMapper.insert(adjustDO);
        // 3. 插入调价明细
        for (ErpSalePriceAdjustItemDO item : items) {
            item.setAdjustId(adjustDO.getId());
        }
        salePriceAdjustItemMapper.insertBatch(items);
        return adjustDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSalePriceAdjust(ErpSalePriceAdjustSaveReqVO updateReqVO) {
        // 1. 校验存在
        ErpSalePriceAdjustDO existDO = validateSalePriceAdjustExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(existDO.getStatus())) {
            throw exception(SALE_PRICE_ADJUST_UPDATE_FAIL_APPROVE);
        }
        // 2. 更新调价单
        ErpSalePriceAdjustDO updateDO = BeanUtils.toBean(updateReqVO, ErpSalePriceAdjustDO.class);
        BigDecimal totalAdjustPrice = BigDecimal.ZERO;
        List<ErpSalePriceAdjustItemDO> items = BeanUtils.toBean(updateReqVO.getItems(), ErpSalePriceAdjustItemDO.class);
        for (ErpSalePriceAdjustItemDO item : items) {
            BigDecimal adjustPrice = calculateAdjustPrice(item);
            item.setAdjustPrice(adjustPrice);
            totalAdjustPrice = totalAdjustPrice.add(adjustPrice);
        }
        updateDO.setTotalAdjustPrice(totalAdjustPrice);
        salePriceAdjustMapper.updateById(updateDO);
        // 3. 更新明细：先删后插
        salePriceAdjustItemMapper.deleteByAdjustId(updateReqVO.getId());
        for (ErpSalePriceAdjustItemDO item : items) {
            item.setId(null);
            item.setAdjustId(updateReqVO.getId());
        }
        salePriceAdjustItemMapper.insertBatch(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSalePriceAdjustStatus(Long id, Integer status) {
        ErpSalePriceAdjustDO existDO = validateSalePriceAdjustExists(id);
        if (ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            if (!ErpAuditStatus.PROCESS.getStatus().equals(existDO.getStatus())) {
                throw exception(SALE_PRICE_ADJUST_APPROVE_FAIL);
            }
            approveAndModifySaleOut(existDO);
            return;
        }
        if (ErpAuditStatus.PROCESS.getStatus().equals(status)) {
            if (!ErpAuditStatus.APPROVE.getStatus().equals(existDO.getStatus())) {
                throw exception(SALE_PRICE_ADJUST_PROCESS_FAIL);
            }
            // 反审批：恢复原销售单价格
            rejectAndRestoreSaleOut(existDO);
            salePriceAdjustMapper.updateById(new ErpSalePriceAdjustDO().setId(id)
                    .setStatus(ErpAuditStatus.PROCESS.getStatus()));
            return;
        }
        ErpSalePriceAdjustDO updateDO = new ErpSalePriceAdjustDO();
        updateDO.setId(id);
        updateDO.setStatus(status);
        salePriceAdjustMapper.updateById(updateDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSalePriceAdjust(List<Long> ids) {
        for (Long id : ids) {
            ErpSalePriceAdjustDO existDO = validateSalePriceAdjustExists(id);
            if (ErpAuditStatus.APPROVE.getStatus().equals(existDO.getStatus())) {
                throw exception(SALE_PRICE_ADJUST_DELETE_FAIL_APPROVE);
            }
            salePriceAdjustMapper.deleteById(id);
            salePriceAdjustItemMapper.deleteByAdjustId(id);
        }
    }

    @Override
    public ErpSalePriceAdjustDO getSalePriceAdjust(Long id) {
        return salePriceAdjustMapper.selectById(id);
    }

    @Override
    public PageResult<ErpSalePriceAdjustDO> getSalePriceAdjustPage(ErpSalePriceAdjustPageReqVO pageReqVO) {
        return salePriceAdjustMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpSalePriceAdjustItemDO> getSalePriceAdjustItemListByAdjustId(Long adjustId) {
        return salePriceAdjustItemMapper.selectListByAdjustId(adjustId);
    }

    @Override
    public List<ErpSalePriceAdjustItemDO> getSalePriceAdjustItemListByAdjustIds(Collection<Long> adjustIds) {
        return salePriceAdjustItemMapper.selectListByAdjustIds(adjustIds);
    }

    @Override
    public List<ErpSaleOutItemForAdjustRespVO> getAdjustableItemsByCustomerId(Long customerId, Long saleOutId) {
        // 1. 查询该客户所有已审批的销售单（如果指定了 saleOutId 则只查该单）
        List<ErpSaleOutDO> saleOuts = saleOutMapper.selectList(
                new LambdaQueryWrapper<ErpSaleOutDO>()
                        .eq(ErpSaleOutDO::getCustomerId, customerId)
                        .eq(saleOutId != null, ErpSaleOutDO::getId, saleOutId)
                        .eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                        .orderByDesc(ErpSaleOutDO::getOutTime));
        if (CollUtil.isEmpty(saleOuts)) {
            return Collections.emptyList();
        }
        // 2. 查询所有子表明细
        List<Long> outIds = saleOuts.stream().map(ErpSaleOutDO::getId).collect(Collectors.toList());
        List<ErpSaleOutItemDO> allItems = saleOutItemMapper.selectListByOutIds(outIds);
        // 3. 批量加载产品信息
        Set<Long> productIds = allItems.stream().map(ErpSaleOutItemDO::getProductId).collect(Collectors.toSet());
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(productIds);
        // 4. 拼装结果
        Map<Long, ErpSaleOutDO> outMap = convertMap(saleOuts, ErpSaleOutDO::getId);
        List<ErpSaleOutItemForAdjustRespVO> result = new ArrayList<>();
        for (ErpSaleOutItemDO item : allItems) {
            ErpSaleOutDO out = outMap.get(item.getOutId());
            if (out == null) continue;
            ErpSaleOutItemForAdjustRespVO vo = new ErpSaleOutItemForAdjustRespVO();
            vo.setSaleOutId(out.getId());
            vo.setSaleOutNo(out.getNo());
            vo.setOutTime(out.getOutTime());
            vo.setProductId(item.getProductId());
            vo.setSaleOutItemId(item.getId());
            vo.setCount(item.getCount());
            vo.setProductPrice(item.getProductPrice());
            vo.setAdjusted(item.getAdjusted());
            ErpProductRespVO product = productMap.get(item.getProductId());
            if (product != null) {
                vo.setProductCode(product.getCode());
                vo.setProductName(product.getName());
                vo.setVehicleModel(product.getVehicleModel());
                vo.setOriginPlace(product.getOriginPlace());
                vo.setBrand(product.getBrand());
                vo.setUnitName(product.getUnitName());
            }
            result.add(vo);
        }
        return result;
    }

    private void approveAndModifySaleOut(ErpSalePriceAdjustDO adjustDO) {
        List<ErpSalePriceAdjustItemDO> adjustItems = salePriceAdjustItemMapper.selectListByAdjustId(adjustDO.getId());
        if (CollUtil.isEmpty(adjustItems)) {
            throw exception(SALE_PRICE_ADJUST_APPROVE_FAIL);
        }
        // 按 saleOutNo 分组处理多张销售单
        Map<String, List<ErpSalePriceAdjustItemDO>> groupBySaleOutNo = adjustItems.stream()
                .collect(Collectors.groupingBy(ErpSalePriceAdjustItemDO::getSaleOutNo));

        for (Map.Entry<String, List<ErpSalePriceAdjustItemDO>> entry : groupBySaleOutNo.entrySet()) {
            String saleOutNo = entry.getKey();
            List<ErpSalePriceAdjustItemDO> groupItems = entry.getValue();

            ErpSaleOutDO originalOut = saleOutMapper.selectByNo(saleOutNo);
            if (originalOut == null || !ErpAuditStatus.APPROVE.getStatus().equals(originalOut.getStatus())) {
                throw exception(SALE_OUT_NOT_APPROVE);
            }

            List<ErpSaleOutItemDO> originalItems = saleOutItemMapper.selectListByOutId(originalOut.getId());
            Map<Long, ErpSalePriceAdjustItemDO> adjustItemMap = convertMap(groupItems, ErpSalePriceAdjustItemDO::getSaleOutItemId);

            for (ErpSaleOutItemDO item : originalItems) {
                ErpSalePriceAdjustItemDO adjustItem = adjustItemMap.get(item.getId());
                if (adjustItem != null) {
                    if (item.getOriginalProductPrice() == null) {
                        item.setOriginalProductPrice(item.getProductPrice());
                    }
                    item.setProductPrice(adjustItem.getNewPrice());
                    item.setAdjusted(true);
                    item.setAdjustId(adjustDO.getId());
                    recalculateSaleOutItem(item);
                    saleOutItemMapper.updateById(item);
                }
            }

            List<ErpSaleOutItemDO> updatedItems = saleOutItemMapper.selectListByOutId(originalOut.getId());
            recalculateSaleOut(originalOut, updatedItems);
            originalOut.setAdjusted(true);
            originalOut.setAdjustPriceAdjustId(adjustDO.getId());
            saleOutMapper.updateById(originalOut);
        }

        // 更新调价单状态
        salePriceAdjustMapper.updateById(new ErpSalePriceAdjustDO().setId(adjustDO.getId())
                .setStatus(ErpAuditStatus.APPROVE.getStatus()));
    }

    private void rejectAndRestoreSaleOut(ErpSalePriceAdjustDO adjustDO) {
        List<ErpSalePriceAdjustItemDO> adjustItems = salePriceAdjustItemMapper.selectListByAdjustId(adjustDO.getId());
        if (CollUtil.isEmpty(adjustItems)) return;

        // 按 saleOutNo 分组
        Map<String, List<ErpSalePriceAdjustItemDO>> groupBySaleOutNo = adjustItems.stream()
                .collect(Collectors.groupingBy(ErpSalePriceAdjustItemDO::getSaleOutNo));

        for (Map.Entry<String, List<ErpSalePriceAdjustItemDO>> entry : groupBySaleOutNo.entrySet()) {
            String saleOutNo = entry.getKey();
            ErpSaleOutDO originalOut = saleOutMapper.selectByNo(saleOutNo);
            if (originalOut == null) continue;

            List<ErpSaleOutItemDO> items = saleOutItemMapper.selectListByOutId(originalOut.getId());
            for (ErpSaleOutItemDO item : items) {
                if (adjustDO.getId().equals(item.getAdjustId())) {
                    item.setProductPrice(item.getOriginalProductPrice());
                    item.setOriginalProductPrice(null);
                    item.setAdjusted(false);
                    item.setAdjustId(null);
                    recalculateSaleOutItem(item);
                    saleOutItemMapper.updateById(item);
                }
            }

            List<ErpSaleOutItemDO> restoredItems = saleOutItemMapper.selectListByOutId(originalOut.getId());
            recalculateSaleOut(originalOut, restoredItems);
            originalOut.setAdjusted(false);
            originalOut.setAdjustPriceAdjustId(null);
            saleOutMapper.updateById(originalOut);
        }
    }

    private ErpSalePriceAdjustDO validateSalePriceAdjustExists(Long id) {
        ErpSalePriceAdjustDO adjustDO = salePriceAdjustMapper.selectById(id);
        if (adjustDO == null) {
            throw exception(SALE_PRICE_ADJUST_NOT_EXISTS);
        }
        return adjustDO;
    }

    private BigDecimal calculateAdjustPrice(ErpSalePriceAdjustItemDO item) {
        if (item.getNewPrice() == null || item.getOldPrice() == null || item.getOutCount() == null) {
            return BigDecimal.ZERO;
        }
        return item.getNewPrice().subtract(item.getOldPrice()).multiply(item.getOutCount());
    }

    private void recalculateSaleOutItem(ErpSaleOutItemDO item) {
        BigDecimal count = item.getCount() == null ? BigDecimal.ZERO : item.getCount();
        BigDecimal price = item.getProductPrice() == null ? BigDecimal.ZERO : item.getProductPrice();
        BigDecimal totalPrice = price.multiply(count);
        item.setTotalPrice(totalPrice);
        BigDecimal taxPercent = item.getTaxPercent() == null ? BigDecimal.ZERO : item.getTaxPercent();
        item.setTaxPrice(totalPrice.multiply(taxPercent).divide(new BigDecimal("100")));
    }

    private void recalculateSaleOut(ErpSaleOutDO saleOut, List<ErpSaleOutItemDO> items) {
        BigDecimal totalCount = BigDecimal.ZERO;
        BigDecimal totalProductPrice = BigDecimal.ZERO;
        BigDecimal totalTaxPrice = BigDecimal.ZERO;
        for (ErpSaleOutItemDO item : items) {
            totalCount = totalCount.add(item.getCount() == null ? BigDecimal.ZERO : item.getCount());
            totalProductPrice = totalProductPrice.add(item.getTotalPrice() == null ? BigDecimal.ZERO : item.getTotalPrice());
            totalTaxPrice = totalTaxPrice.add(item.getTaxPrice() == null ? BigDecimal.ZERO : item.getTaxPrice());
        }
        BigDecimal discountPercent = saleOut.getDiscountPercent() == null ? BigDecimal.ZERO : saleOut.getDiscountPercent();
        BigDecimal discountPrice = totalProductPrice.add(totalTaxPrice).multiply(discountPercent).divide(new BigDecimal("100"));
        BigDecimal otherPrice = saleOut.getOtherPrice() == null ? BigDecimal.ZERO : saleOut.getOtherPrice();
        saleOut.setTotalCount(totalCount);
        saleOut.setTotalProductPrice(totalProductPrice);
        saleOut.setTotalTaxPrice(totalTaxPrice);
        saleOut.setDiscountPrice(discountPrice);
        saleOut.setTotalPrice(totalProductPrice.add(totalTaxPrice).subtract(discountPrice).add(otherPrice));
    }

}
