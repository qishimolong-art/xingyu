package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;

import javax.validation.Valid;

public interface ErpReceivableOtherService {

    Long createReceivableOther(@Valid ErpReceivableOtherSaveReqVO createReqVO);

    void updateReceivableOther(@Valid ErpReceivableOtherSaveReqVO updateReqVO);

    void updateReceivableOtherStatus(Long id, Integer status);

    void deleteReceivableOther(Long id);

    ErpReceivableOtherDO getReceivableOther(Long id);

    PageResult<ErpReceivableOtherDO> getReceivableOtherPage(ErpReceivableOtherPageReqVO pageReqVO);
}
