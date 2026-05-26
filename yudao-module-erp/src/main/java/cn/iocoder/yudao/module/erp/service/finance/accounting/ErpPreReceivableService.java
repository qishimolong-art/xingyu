package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 预收账款单 Service 接口
 */
public interface ErpPreReceivableService {

    /**
     * 创建预收账款单
     */
    Long createPreReceivable(@Valid ErpPreReceivableSaveReqVO createReqVO);

    /**
     * 更新预收账款单
     */
    void updatePreReceivable(@Valid ErpPreReceivableSaveReqVO updateReqVO);

    /**
     * 删除预收账款单
     */
    void deletePreReceivable(List<Long> ids);

    /**
     * 获得预收账款单
     */
    ErpPreReceivableDO getPreReceivable(Long id);

    /**
     * 获得预收账款单分页
     */
    PageResult<ErpPreReceivableDO> getPreReceivablePage(ErpPreReceivablePageReqVO pageReqVO);

    /**
     * 更新预收账款单状态（审核/反审核）
     */
    void updatePreReceivableStatus(Long id, Integer status);

    /**
     * 获得预收账款单明细列表
     */
    List<ErpPreReceivableItemDO> getPreReceivableItemListByPreReceivableId(Long preReceivableId);

    /**
     * 批量获得预收账款单明细列表
     */
    List<ErpPreReceivableItemDO> getPreReceivableItemListByPreReceivableIds(Collection<Long> preReceivableIds);

}
