package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableAccountDO;

import java.util.List;

public interface ErpPayableAccountService {

    PageResult<ErpPayableAccountDO> getPayableAccountPage(ErpPayableAccountPageReqVO reqVO);

    List<ErpPayableDetailRespVO> getPayableDetailList(ErpPayableDetailReqVO reqVO);
}
