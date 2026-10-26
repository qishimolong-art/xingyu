package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

public interface ErpCustomerExportTaskService {

    ErpCustomerExportTaskStartRespVO startTask(ErpCustomerPageReqVO reqVO, Set<String> fields);

    ErpCustomerExportTaskRespVO getActiveTask();

    ErpCustomerExportTaskRespVO getTask(Long taskId);

    PageResult<ErpCustomerExportTaskRespVO> getHistory(int pageNo, int pageSize);

    void downloadTask(Long taskId, HttpServletResponse response) throws IOException;

}
