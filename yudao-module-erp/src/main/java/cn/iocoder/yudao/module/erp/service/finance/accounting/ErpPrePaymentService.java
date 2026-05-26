package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentItemDO;

import javax.validation.Valid;
import java.util.List;

public interface ErpPrePaymentService {

    Long createPrePayment(@Valid ErpPrePaymentSaveReqVO createReqVO);

    void updatePrePayment(@Valid ErpPrePaymentSaveReqVO updateReqVO);

    void updatePrePaymentStatus(Long id, Integer status);

    void deletePrePayment(Long id);

    ErpPrePaymentDO getPrePayment(Long id);

    PageResult<ErpPrePaymentDO> getPrePaymentPage(ErpPrePaymentPageReqVO pageReqVO);

    List<ErpPrePaymentItemDO> getPrePaymentItemListByPrePaymentId(Long prePaymentId);

}
