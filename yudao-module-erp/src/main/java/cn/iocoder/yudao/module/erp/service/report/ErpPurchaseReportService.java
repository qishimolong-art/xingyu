package cn.iocoder.yudao.module.erp.service.report;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportDetailReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportDeptRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseReportTrendRespVO;

import java.util.List;

public interface ErpPurchaseReportService {

    ErpPurchaseReportSummaryRespVO getPurchaseReportSummary(ErpPurchaseReportPageReqVO reqVO);

    List<ErpPurchaseReportTrendRespVO> getPurchaseReportTrend(ErpPurchaseReportPageReqVO reqVO);

    PageResult<ErpPurchaseReportRespVO> getPurchaseReportPage(ErpPurchaseReportPageReqVO reqVO);

    PageResult<ErpPurchaseReportProductRespVO> getPurchaseReportProductPage(ErpPurchaseReportPageReqVO reqVO);

    PageResult<ErpPurchaseReportDeptRespVO> getPurchaseReportDeptPage(ErpPurchaseReportPageReqVO reqVO);

    PageResult<ErpPurchaseReportDetailRespVO> getPurchaseReportDetailPage(ErpPurchaseReportPageReqVO reqVO);

    List<ErpPurchaseReportDetailRespVO> getPurchaseReportDetailList(ErpPurchaseReportDetailReqVO reqVO);
}
