package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableAccountDO;

import java.util.List;

public interface ErpReceivableAccountService {

    PageResult<ErpReceivableAccountDO> getReceivableAccountPage(ErpReceivableAccountPageReqVO reqVO);

    List<ErpReceivableDetailRespVO> getReceivableDetailList(ErpReceivableDetailReqVO reqVO);

}
