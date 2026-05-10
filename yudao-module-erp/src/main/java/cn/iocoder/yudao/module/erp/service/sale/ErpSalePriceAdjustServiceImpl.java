package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
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
    private ErpNoRedisDAO noRedisDAO;

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
        }
        if (ErpAuditStatus.PROCESS.getStatus().equals(status)) {
            if (!ErpAuditStatus.APPROVE.getStatus().equals(existDO.getStatus())) {
                throw exception(SALE_PRICE_ADJUST_PROCESS_FAIL);
            }
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

}
