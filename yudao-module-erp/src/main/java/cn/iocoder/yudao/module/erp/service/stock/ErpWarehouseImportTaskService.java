package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportTaskStartRespVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ErpWarehouseImportTaskService {

    ErpWarehouseImportTaskStartRespVO startTask(MultipartFile file) throws IOException;

    ErpWarehouseImportTaskRespVO getActiveTask();

    ErpWarehouseImportTaskRespVO getTask(Long taskId);

}
