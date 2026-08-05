package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerDetailPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerTotalRespVO;

import java.util.List;

public interface ErpStockTransferLedgerService {

    PageResult<ErpStockTransferLedgerSummaryRespVO> getSummaryPage(ErpStockTransferLedgerPageReqVO reqVO);

    ErpStockTransferLedgerTotalRespVO getTotal(ErpStockTransferLedgerPageReqVO reqVO);

    PageResult<ErpStockTransferLedgerDetailRespVO> getDetailPage(ErpStockTransferLedgerDetailPageReqVO reqVO);

    List<ErpStockTransferLedgerDetailRespVO> getExportList(ErpStockTransferLedgerPageReqVO reqVO);

}
