package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;

import javax.validation.Valid;

public interface ErpPayableOtherService {

    Long createPayableOther(@Valid ErpPayableOtherSaveReqVO createReqVO);

    void updatePayableOther(@Valid ErpPayableOtherSaveReqVO updateReqVO);

    void updatePayableOtherStatus(Long id, Integer status);

    void deletePayableOther(Long id);

    ErpPayableOtherDO getPayableOther(Long id);

    PageResult<ErpPayableOtherDO> getPayableOtherPage(ErpPayableOtherPageReqVO pageReqVO);

}
