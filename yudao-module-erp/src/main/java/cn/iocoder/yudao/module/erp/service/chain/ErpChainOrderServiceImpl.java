package cn.iocoder.yudao.module.erp.service.chain;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.chain.vo.ErpChainOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.chain.vo.ErpChainOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.chain.ErpChainOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.chain.ErpChainOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.chain.ErpChainOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.chain.ErpChainOrderMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.chain.ErpChainOrderStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 连锁开单 Service 实现类
 *
 * @author 汽配ERP
 */
@Service
@Validated
public class ErpChainOrderServiceImpl implements ErpChainOrderService {

    @Resource
    private ErpChainOrderMapper chainOrderMapper;
    @Resource
    private ErpChainOrderItemMapper chainOrderItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createChainOrder(ErpChainOrderSaveReqVO createReqVO) {
        // 1. 生成连锁开单号
        String no = noRedisDAO.generate(ErpNoRedisDAO.CHAIN_ORDER_NO_PREFIX);
        // 2. 插入连锁开单
        ErpChainOrderDO orderDO = BeanUtils.toBean(createReqVO, ErpChainOrderDO.class);
        orderDO.setNo(no);
        orderDO.setStatus(ErpChainOrderStatusEnum.PENDING.getStatus());
        if (orderDO.getOrderTime() == null) {
            orderDO.setOrderTime(LocalDateTime.now());
        }
        // 计算合计
        List<ErpChainOrderItemDO> items = BeanUtils.toBean(createReqVO.getItems(), ErpChainOrderItemDO.class);
        BigDecimal totalCount = BigDecimal.ZERO;
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (ErpChainOrderItemDO item : items) {
            totalCount = totalCount.add(item.getCount());
            BigDecimal itemTotal = item.getProductPrice() != null
                    ? item.getProductPrice().multiply(item.getCount()) : BigDecimal.ZERO;
            item.setTotalPrice(itemTotal);
            totalPrice = totalPrice.add(itemTotal);
        }
        orderDO.setTotalCount(totalCount);
        orderDO.setTotalPrice(totalPrice);
        chainOrderMapper.insert(orderDO);
        // 3. 插入明细
        for (ErpChainOrderItemDO item : items) {
            item.setChainOrderId(orderDO.getId());
        }
        chainOrderItemMapper.insertBatch(items);
        return orderDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveChainOrder(Long id) {
        // 1. 校验状态
        ErpChainOrderDO orderDO = validateChainOrderExists(id);
        if (!ErpChainOrderStatusEnum.PENDING.getStatus().equals(orderDO.getStatus())) {
            throw exception(CHAIN_ORDER_APPROVE_FAIL);
        }
        // 2. 更新状态为已审核
        ErpChainOrderDO updateDO = new ErpChainOrderDO();
        updateDO.setId(id);
        updateDO.setStatus(ErpChainOrderStatusEnum.APPROVED.getStatus());
        updateDO.setApproveTime(LocalDateTime.now());
        chainOrderMapper.updateById(updateDO);

        // 3. 跨租户流转逻辑
        // 获取明细
        List<ErpChainOrderItemDO> items = chainOrderItemMapper.selectListByChainOrderId(id);

        if (Integer.valueOf(1).equals(orderDO.getBranchType())) {
            // === 有仓分公司 ===
            // 3.1 在总公司租户下生成销售出库单
            // TenantUtils.execute(orderDO.getHqTenantId(), () -> {
            //     创建销售出库单 → 自动审核 → 库存扣减
            // });
            // 3.2 在分公司租户下生成采购入库草稿
            // TenantUtils.execute(orderDO.getBranchTenantId(), () -> {
            //     创建采购入库单(status=PROCESS)
            // });

            // TODO: 实际跨租户流转逻辑，需要注入 ErpSaleOutService 和 ErpPurchaseInService
            // 此处预留接口，待集成测试时完善

        } else {
            // === 无仓分公司 ===
            // 3.1 在总公司租户下生成销售出库单（客户=终端客户）
            // TenantUtils.execute(orderDO.getHqTenantId(), () -> {
            //     创建销售出库单(客户=终端客户) → 自动审核 → 库存扣减
            // });

            // TODO: 实际跨租户流转逻辑
        }

        // 4. 更新为已完成
        ErpChainOrderDO completeDO = new ErpChainOrderDO();
        completeDO.setId(id);
        completeDO.setStatus(ErpChainOrderStatusEnum.COMPLETED.getStatus());
        chainOrderMapper.updateById(completeDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelChainOrder(Long id) {
        // 1. 校验状态
        ErpChainOrderDO orderDO = validateChainOrderExists(id);
        if (!ErpChainOrderStatusEnum.PENDING.getStatus().equals(orderDO.getStatus())) {
            throw exception(CHAIN_ORDER_CANCEL_FAIL);
        }
        // 2. 更新状态为已取消
        ErpChainOrderDO updateDO = new ErpChainOrderDO();
        updateDO.setId(id);
        updateDO.setStatus(ErpChainOrderStatusEnum.CANCELLED.getStatus());
        chainOrderMapper.updateById(updateDO);
    }

    @Override
    public ErpChainOrderDO getChainOrder(Long id) {
        return chainOrderMapper.selectById(id);
    }

    @Override
    public PageResult<ErpChainOrderDO> getChainOrderPage(ErpChainOrderPageReqVO pageReqVO) {
        return chainOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpChainOrderItemDO> getChainOrderItemListByOrderId(Long chainOrderId) {
        return chainOrderItemMapper.selectListByChainOrderId(chainOrderId);
    }

    @Override
    public List<ErpChainOrderItemDO> getChainOrderItemListByOrderIds(Collection<Long> chainOrderIds) {
        return chainOrderItemMapper.selectListByChainOrderIds(chainOrderIds);
    }

    private ErpChainOrderDO validateChainOrderExists(Long id) {
        ErpChainOrderDO orderDO = chainOrderMapper.selectById(id);
        if (orderDO == null) {
            throw exception(CHAIN_ORDER_NOT_EXISTS);
        }
        return orderDO;
    }

}
