package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO;

public interface ErpReceivableMiscService {

    Long createReceivableMisc(ErpReceivableMiscSaveReqVO createReqVO);

    Long createReceivableMiscDraft(ErpReceivableMiscDraftSaveReqVO createReqVO);

    Long createAndSubmitReceivableMisc(ErpReceivableMiscSaveReqVO createReqVO);

    void updateReceivableMisc(ErpReceivableMiscSaveReqVO updateReqVO);

    void updateReceivableMiscDraft(ErpReceivableMiscDraftSaveReqVO updateReqVO);

    void updateAndSubmitReceivableMisc(ErpReceivableMiscSaveReqVO updateReqVO);

    void submitReceivableMisc(Long id);

    void updateReceivableMiscStatus(Long id, Integer status);

    void deleteReceivableMisc(Long id);

    ErpReceivableMiscDO getReceivableMisc(Long id);

    PageResult<ErpReceivableMiscDO> getReceivableMiscPage(ErpReceivableMiscPageReqVO pageReqVO);

}
