package cn.iocoder.yudao.module.erp.service.chain;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.chain.vo.ErpChainOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.chain.vo.ErpChainOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.chain.ErpChainOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.chain.ErpChainOrderItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 连锁开单 Service 接口
 *
 * @author 汽配ERP
 */
public interface ErpChainOrderService {

    /**
     * 创建连锁开单（分公司发起）
     */
    Long createChainOrder(@Valid ErpChainOrderSaveReqVO createReqVO);

    /**
     * 审核连锁开单（总公司审核，触发跨租户流转）
     */
    void approveChainOrder(Long id);

    /**
     * 取消连锁开单
     */
    void cancelChainOrder(Long id);

    /**
     * 获得连锁开单
     */
    ErpChainOrderDO getChainOrder(Long id);

    /**
     * 获得连锁开单分页
     */
    PageResult<ErpChainOrderDO> getChainOrderPage(ErpChainOrderPageReqVO pageReqVO);

    /**
     * 获得连锁开单项列表
     */
    List<ErpChainOrderItemDO> getChainOrderItemListByOrderId(Long chainOrderId);

    /**
     * 获得连锁开单项列表（批量）
     */
    List<ErpChainOrderItemDO> getChainOrderItemListByOrderIds(Collection<Long> chainOrderIds);

}
