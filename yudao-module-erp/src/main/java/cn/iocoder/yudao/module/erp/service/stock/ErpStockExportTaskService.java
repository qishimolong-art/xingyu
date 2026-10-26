package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ErpStockExportTaskService {

    ErpStockExportTaskStartRespVO startTask(ErpStockPageReqVO reqVO, ErpStockExportPageLoader pageLoader);

    ErpStockExportTaskRespVO getActiveTask();

    ErpStockExportTaskRespVO getTask(Long taskId);

    void downloadTask(Long taskId, HttpServletResponse response) throws IOException;

}
