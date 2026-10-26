package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveExportTaskRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive.ErpArchiveExportTaskStartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryListReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitPageReqVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ErpSimpleArchiveExportTaskService {

    ErpArchiveExportTaskStartRespVO startCategoryTask(ErpProductCategoryListReqVO reqVO);
    ErpArchiveExportTaskStartRespVO startBrandTask(ErpProductBrandPageReqVO reqVO);
    ErpArchiveExportTaskStartRespVO startUnitTask(ErpProductUnitPageReqVO reqVO);
    ErpArchiveExportTaskRespVO getActiveTask(ErpSimpleArchiveModule module);
    ErpArchiveExportTaskRespVO getTask(ErpSimpleArchiveModule module, Long taskId);
    PageResult<ErpArchiveExportTaskRespVO> getHistory(ErpSimpleArchiveModule module, int pageNo, int pageSize);
    void downloadTask(ErpSimpleArchiveModule module, Long taskId, HttpServletResponse response) throws IOException;

}
