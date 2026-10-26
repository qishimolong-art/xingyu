package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderDetailImportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderTaskStartRespVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ErpPurchaseOrderDetailImportTaskService {
    ErpPurchaseOrderTaskStartRespVO startTask(MultipartFile file, String formSessionId) throws IOException;
    ErpPurchaseOrderDetailImportTaskRespVO getActiveTask(String formSessionId);
    ErpPurchaseOrderDetailImportTaskRespVO getTask(Long taskId);
    PageResult<ErpPurchaseOrderSaveReqVO.Item> getResult(Long taskId, int pageNo, int pageSize) throws IOException;
}
