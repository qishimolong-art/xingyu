package cn.iocoder.yudao.module.erp.service.finance.settlement;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account.ErpPayableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo.ErpSettlementOffsetPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.settlement.ErpSettlementOffsetDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.settlement.ErpSettlementOffsetMapper;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableAccountService;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableAccountService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

@Service
@Validated
public class ErpSettlementOffsetServiceImpl implements ErpSettlementOffsetService {

    @Resource
    private ErpSettlementOffsetMapper settlementOffsetMapper;
    @Resource
    private ErpReceivableAccountService receivableAccountService;
    @Resource
    private ErpPayableAccountService payableAccountService;

    @Override
    public PageResult<ErpSettlementOffsetDO> getSettlementOffsetPage(ErpSettlementOffsetPageReqVO reqVO) {
        return settlementOffsetMapper.selectPage(reqVO);
    }

    @Override
    public ErpSettlementOffsetDetailRespVO getSettlementOffsetDetail(ErpSettlementOffsetDetailReqVO reqVO) {
        ErpReceivableDetailReqVO receivableReqVO = new ErpReceivableDetailReqVO();
        receivableReqVO.setCustomerId(reqVO.getCustomerId());
        receivableReqVO.setBizTime(reqVO.getBizTime());

        ErpPayableDetailReqVO payableReqVO = new ErpPayableDetailReqVO();
        payableReqVO.setSupplierId(reqVO.getSupplierId());
        payableReqVO.setBizTime(reqVO.getBizTime());

        ErpSettlementOffsetDetailRespVO respVO = new ErpSettlementOffsetDetailRespVO();
        respVO.setReceivableDetails(receivableAccountService.getReceivableDetailList(receivableReqVO));
        respVO.setPayableDetails(payableAccountService.getPayableDetailList(payableReqVO));
        return respVO;
    }
}
