package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO;

public interface ErpPayableMiscService {

    Long createPayableMisc(ErpPayableMiscSaveReqVO createReqVO);

    Long createPayableMiscDraft(ErpPayableMiscDraftSaveReqVO createReqVO);

    Long createAndSubmitPayableMisc(ErpPayableMiscSaveReqVO createReqVO);

    void updatePayableMisc(ErpPayableMiscSaveReqVO updateReqVO);

    void updatePayableMiscDraft(ErpPayableMiscDraftSaveReqVO updateReqVO);

    void updateAndSubmitPayableMisc(ErpPayableMiscSaveReqVO updateReqVO);

    void submitPayableMisc(Long id);

    void updatePayableMiscStatus(Long id, Integer status);

    void deletePayableMisc(Long id);

    ErpPayableMiscDO getPayableMisc(Long id);

    PageResult<ErpPayableMiscDO> getPayableMiscPage(ErpPayableMiscPageReqVO pageReqVO);

}
