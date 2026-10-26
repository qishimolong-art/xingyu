package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

public interface ErpSupplierExportTaskService {

    ErpSupplierExportTaskStartRespVO startTask(ErpSupplierPageReqVO reqVO, Set<String> fields);

    ErpSupplierExportTaskRespVO getActiveTask();

    ErpSupplierExportTaskRespVO getTask(Long taskId);

    PageResult<ErpSupplierExportTaskRespVO> getHistory(int pageNo, int pageSize);

    void downloadTask(Long taskId, HttpServletResponse response) throws IOException;

}
