package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.report.ErpPayableReportRespVO;

import java.util.List;

public interface ErpPayableReportService {

    PageResult<ErpPayableReportRespVO> getPayableReportPage(ErpPayableReportPageReqVO reqVO);

    List<ErpPayableReportDetailRespVO> getPayableReportDetailList(ErpPayableReportDetailReqVO reqVO);

}
