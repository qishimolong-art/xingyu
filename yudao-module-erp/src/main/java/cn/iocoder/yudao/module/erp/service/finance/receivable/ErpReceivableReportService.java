package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report.ErpReceivableReportRespVO;

import java.util.List;

public interface ErpReceivableReportService {

    PageResult<ErpReceivableReportRespVO> getReceivableReportPage(ErpReceivableReportPageReqVO reqVO);

    List<ErpReceivableReportDetailRespVO> getReceivableReportDetailList(ErpReceivableReportDetailReqVO reqVO);

}
