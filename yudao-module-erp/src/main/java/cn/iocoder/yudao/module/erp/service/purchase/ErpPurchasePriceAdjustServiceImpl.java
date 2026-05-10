package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 采购调价单 Service 实现类
 *
 * @author 汽配ERP
 */
@Service
@Validated
public class ErpPurchasePriceAdjustServiceImpl implements ErpPurchasePriceAdjustService {

    @Resource
    private ErpPurchasePriceAdjustMapper priceAdjustMapper;
    @Resource
    private ErpPurchasePriceAdjustItemMapper priceAdjustItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchasePriceAdjust(ErpPurchasePriceAdjustSaveReqVO createReqVO) {
        // 1. 生成调价单号
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_PRICE_ADJUST_NO_PREFIX);
        // 2. 插入调价单
        ErpPurchasePriceAdjustDO adjustDO = BeanUtils.toBean(createReqVO, ErpPurchasePriceAdjustDO.class);
        adjustDO.setNo(no);
        adjustDO.setStatus(ErpAuditStatus.PROCESS.getStatus());
        // 计算调价总金额
        BigDecimal totalAdjustPrice = BigDecimal.ZERO;
        List<ErpPurchasePriceAdjustItemDO> items = BeanUtils.toBean(createReqVO.getItems(), ErpPurchasePriceAdjustItemDO.class);
        for (ErpPurchasePriceAdjustItemDO item : items) {
            BigDecimal adjustPrice = calculateAdjustPrice(item);
            item.setAdjustPrice(adjustPrice);
            totalAdjustPrice = totalAdjustPrice.add(adjustPrice);
        }
        adjustDO.setTotalAdjustPrice(totalAdjustPrice);
        priceAdjustMapper.insert(adjustDO);
        // 3. 插入调价明细
        for (ErpPurchasePriceAdjustItemDO item : items) {
            item.setAdjustId(adjustDO.getId());
        }
        priceAdjustItemMapper.insertBatch(items);
        return adjustDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchasePriceAdjust(ErpPurchasePriceAdjustSaveReqVO updateReqVO) {
        // 1. 校验存在
        ErpPurchasePriceAdjustDO existDO = validatePurchasePriceAdjustExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(existDO.getStatus())) {
            throw exception(PURCHASE_PRICE_ADJUST_UPDATE_FAIL_APPROVE);
        }
        // 2. 更新调价单
        ErpPurchasePriceAdjustDO updateDO = BeanUtils.toBean(updateReqVO, ErpPurchasePriceAdjustDO.class);
        // 重新计算调价总金额
        BigDecimal totalAdjustPrice = BigDecimal.ZERO;
        List<ErpPurchasePriceAdjustItemDO> items = BeanUtils.toBean(updateReqVO.getItems(), ErpPurchasePriceAdjustItemDO.class);
        for (ErpPurchasePriceAdjustItemDO item : items) {
            BigDecimal adjustPrice = calculateAdjustPrice(item);
            item.setAdjustPrice(adjustPrice);
            totalAdjustPrice = totalAdjustPrice.add(adjustPrice);
        }
        updateDO.setTotalAdjustPrice(totalAdjustPrice);
        priceAdjustMapper.updateById(updateDO);
        // 3. 更新调价明细：先删后插
        priceAdjustItemMapper.deleteByAdjustId(updateReqVO.getId());
        for (ErpPurchasePriceAdjustItemDO item : items) {
            item.setAdjustId(updateReqVO.getId());
        }
        priceAdjustItemMapper.insertBatch(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchasePriceAdjustStatus(Long id, Integer status) {
        ErpPurchasePriceAdjustDO existDO = validatePurchasePriceAdjustExists(id);
        // 审核：只有未审核才能审核
        if (ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            if (!ErpAuditStatus.PROCESS.getStatus().equals(existDO.getStatus())) {
                throw exception(PURCHASE_PRICE_ADJUST_APPROVE_FAIL);
            }
        }
        // 反审核：只有已审核才能反审核
        if (ErpAuditStatus.PROCESS.getStatus().equals(status)) {
            if (!ErpAuditStatus.APPROVE.getStatus().equals(existDO.getStatus())) {
                throw exception(PURCHASE_PRICE_ADJUST_PROCESS_FAIL);
            }
        }
        // 更新状态
        ErpPurchasePriceAdjustDO updateDO = new ErpPurchasePriceAdjustDO();
        updateDO.setId(id);
        updateDO.setStatus(status);
        priceAdjustMapper.updateById(updateDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchasePriceAdjust(List<Long> ids) {
        for (Long id : ids) {
            ErpPurchasePriceAdjustDO existDO = validatePurchasePriceAdjustExists(id);
            if (ErpAuditStatus.APPROVE.getStatus().equals(existDO.getStatus())) {
                throw exception(PURCHASE_PRICE_ADJUST_DELETE_FAIL_APPROVE);
            }
            priceAdjustMapper.deleteById(id);
            priceAdjustItemMapper.deleteByAdjustId(id);
        }
    }

    @Override
    public ErpPurchasePriceAdjustDO getPurchasePriceAdjust(Long id) {
        return priceAdjustMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPurchasePriceAdjustDO> getPurchasePriceAdjustPage(ErpPurchasePriceAdjustPageReqVO pageReqVO) {
        return priceAdjustMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPurchasePriceAdjustItemDO> getPurchasePriceAdjustItemListByAdjustId(Long adjustId) {
        return priceAdjustItemMapper.selectListByAdjustId(adjustId);
    }

    @Override
    public List<ErpPurchasePriceAdjustItemDO> getPurchasePriceAdjustItemListByAdjustIds(Collection<Long> adjustIds) {
        return priceAdjustItemMapper.selectListByAdjustIds(adjustIds);
    }

    private ErpPurchasePriceAdjustDO validatePurchasePriceAdjustExists(Long id) {
        ErpPurchasePriceAdjustDO adjustDO = priceAdjustMapper.selectById(id);
        if (adjustDO == null) {
            throw exception(PURCHASE_PRICE_ADJUST_NOT_EXISTS);
        }
        return adjustDO;
    }

    /**
     * 计算调价金额 = (新价 - 旧价) * 入库数
     */
    private BigDecimal calculateAdjustPrice(ErpPurchasePriceAdjustItemDO item) {
        if (item.getNewPrice() == null || item.getOldPrice() == null || item.getInCount() == null) {
            return BigDecimal.ZERO;
        }
        return item.getNewPrice().subtract(item.getOldPrice()).multiply(item.getInCount());
    }

}
