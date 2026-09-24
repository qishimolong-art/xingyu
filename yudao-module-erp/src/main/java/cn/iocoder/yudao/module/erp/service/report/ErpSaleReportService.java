package cn.iocoder.yudao.module.erp.service.report;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportDeptRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleReportTrendRespVO;

import java.util.List;

public interface ErpSaleReportService {

    ErpSaleReportSummaryRespVO getSaleReportSummary(ErpSaleReportPageReqVO reqVO);

    List<ErpSaleReportTrendRespVO> getSaleReportTrend(ErpSaleReportPageReqVO reqVO);

    PageResult<ErpSaleReportRespVO> getSaleReportPage(ErpSaleReportPageReqVO reqVO);

    PageResult<ErpSaleReportProductRespVO> getSaleReportProductPage(ErpSaleReportPageReqVO reqVO);

    PageResult<ErpSaleReportDeptRespVO> getSaleReportDeptPage(ErpSaleReportPageReqVO reqVO);

    PageResult<ErpSaleReportDetailRespVO> getSaleReportDetailPage(ErpSaleReportPageReqVO reqVO);

    List<ErpSaleReportDetailRespVO> getSaleReportDetailList(ErpSaleReportDetailReqVO reqVO);
}
