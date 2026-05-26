package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

public interface ErpOtherPayableService {

    Long createOtherPayable(@Valid ErpOtherPayableSaveReqVO createReqVO);

    void updateOtherPayable(@Valid ErpOtherPayableSaveReqVO updateReqVO);

    void updateOtherPayableStatus(Long id, Integer status);

    void deleteOtherPayable(Long id);

    ErpOtherPayableDO getOtherPayable(Long id);

    PageResult<ErpOtherPayableDO> getOtherPayablePage(ErpOtherPayablePageReqVO pageReqVO);

    List<ErpOtherPayableItemDO> getOtherPayableItemListByPayableId(Long payableId);

    List<ErpOtherPayableItemDO> getOtherPayableItemListByPayableIds(Collection<Long> payableIds);

}
