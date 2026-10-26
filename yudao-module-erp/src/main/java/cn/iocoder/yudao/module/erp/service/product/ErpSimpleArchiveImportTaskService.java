package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveImportTaskStartRespVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ErpSimpleArchiveImportTaskService {

    ErpArchiveImportTaskStartRespVO startTask(ErpSimpleArchiveModule module, MultipartFile file) throws IOException;

    ErpArchiveImportTaskRespVO getActiveTask(ErpSimpleArchiveModule module);

    ErpArchiveImportTaskRespVO getTask(ErpSimpleArchiveModule module, Long taskId);

    void downloadFailureDetails(ErpSimpleArchiveModule module, Long recordId,
                                HttpServletResponse response) throws IOException;

}
