package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

public interface ErpWarehouseExportTaskService {

    ErpWarehouseExportTaskStartRespVO startTask(ErpWarehousePageReqVO reqVO, Set<String> fields);

    ErpWarehouseExportTaskRespVO getActiveTask();

    ErpWarehouseExportTaskRespVO getTask(Long taskId);

    PageResult<ErpWarehouseExportTaskRespVO> getHistory(int pageNo, int pageSize);

    void downloadTask(Long taskId, HttpServletResponse response) throws IOException;

}
