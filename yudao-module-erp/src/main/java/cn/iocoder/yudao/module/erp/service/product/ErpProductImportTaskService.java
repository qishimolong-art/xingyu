package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportTaskStartRespVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ErpProductImportTaskService {

    ErpProductImportTaskStartRespVO startTask(MultipartFile file) throws IOException;

    ErpProductImportTaskRespVO getActiveTask();

    ErpProductImportTaskRespVO getTask(Long taskId);

}
