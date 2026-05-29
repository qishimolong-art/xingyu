package cn.iocoder.yudao.module.erp.service.finance.settlement;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.settlement.ErpSettlementOffsetDO;

public interface ErpSettlementOffsetService {

    PageResult<ErpSettlementOffsetDO> getSettlementOffsetPage(ErpSettlementOffsetPageReqVO reqVO);

    ErpSettlementOffsetDetailRespVO getSettlementOffsetDetail(ErpSettlementOffsetDetailReqVO reqVO);
}
