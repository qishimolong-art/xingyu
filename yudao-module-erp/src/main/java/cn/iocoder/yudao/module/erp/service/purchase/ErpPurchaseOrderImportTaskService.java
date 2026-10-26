package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderTaskStartRespVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ErpPurchaseOrderImportTaskService {
    ErpPurchaseOrderTaskStartRespVO startTask(MultipartFile file) throws IOException;
    ErpPurchaseOrderImportTaskRespVO getActiveTask();
    ErpPurchaseOrderImportTaskRespVO getTask(Long taskId);
}
