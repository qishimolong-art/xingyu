package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

public interface ErpOtherReceivableService {

    Long createOtherReceivable(@Valid ErpOtherReceivableSaveReqVO createReqVO);

    void updateOtherReceivable(@Valid ErpOtherReceivableSaveReqVO updateReqVO);

    void updateOtherReceivableStatus(Long id, Integer status);

    void deleteOtherReceivable(List<Long> ids);

    ErpOtherReceivableDO getOtherReceivable(Long id);

    PageResult<ErpOtherReceivableDO> getOtherReceivablePage(ErpOtherReceivablePageReqVO pageReqVO);

    List<ErpOtherReceivableItemDO> getOtherReceivableItemListByReceivableId(Long receivableId);

    List<ErpOtherReceivableItemDO> getOtherReceivableItemListByReceivableIds(Collection<Long> receivableIds);

}
