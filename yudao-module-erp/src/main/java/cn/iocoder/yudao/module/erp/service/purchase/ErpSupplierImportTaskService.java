package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportTaskStartRespVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ErpSupplierImportTaskService {

    ErpSupplierImportTaskStartRespVO startTask(MultipartFile file) throws IOException;

    ErpSupplierImportTaskRespVO getActiveTask();

    ErpSupplierImportTaskRespVO getTask(Long taskId);

}
