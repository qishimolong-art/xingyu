package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

public interface ErpPurchaseOrderExportTaskService {
    ErpPurchaseOrderTaskStartRespVO startTask(ErpPurchaseOrderPageReqVO reqVO, Set<String> fields);
    ErpPurchaseOrderExportTaskRespVO getActiveTask();
    ErpPurchaseOrderExportTaskRespVO getTask(Long taskId);
    PageResult<ErpPurchaseOrderExportTaskRespVO> getHistory(int pageNo, int pageSize);
    void downloadTask(Long taskId, HttpServletResponse response) throws IOException;
}
