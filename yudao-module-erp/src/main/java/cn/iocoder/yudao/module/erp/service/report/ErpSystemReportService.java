package cn.iocoder.yudao.module.erp.service.report;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportRankRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.system.ErpSystemReportTrendRespVO;

import java.util.List;
import java.util.Map;

public interface ErpSystemReportService {

    Map<String, Object> getOptions(String reportType, ErpSystemReportReqVO reqVO);

    ErpSystemReportSummaryRespVO getSummary(String reportType, ErpSystemReportReqVO reqVO);

    List<ErpSystemReportTrendRespVO> getTrend(String reportType, ErpSystemReportReqVO reqVO);

    List<ErpSystemReportRankRespVO> getRank(String reportType, ErpSystemReportReqVO reqVO);

    PageResult<Map<String, Object>> getPage(String reportType, ErpSystemReportReqVO reqVO);

    List<Map<String, Object>> getExportList(String reportType, ErpSystemReportReqVO reqVO);

}
