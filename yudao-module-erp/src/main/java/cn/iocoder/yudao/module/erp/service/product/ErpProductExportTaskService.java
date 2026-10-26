package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface ErpProductExportTaskService {

    ErpProductExportTaskStartRespVO startTask(ErpProductPageReqVO reqVO,
                                              List<ErpExportFieldRespVO> selectedFields,
                                              List<ErpFieldConfigDO> fieldConfigs);

    ErpProductExportTaskRespVO getActiveTask();

    ErpProductExportTaskRespVO getTask(Long taskId);

    PageResult<ErpProductExportTaskRespVO> getHistory(int pageNo, int pageSize);

    void downloadTask(Long taskId, HttpServletResponse response) throws IOException;

}
