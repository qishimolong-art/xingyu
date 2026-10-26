package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportTaskStartRespVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ErpCustomerImportTaskService {

    ErpCustomerImportTaskStartRespVO startTask(MultipartFile file) throws IOException;

    ErpCustomerImportTaskRespVO getActiveTask();

    ErpCustomerImportTaskRespVO getTask(Long taskId);

}
