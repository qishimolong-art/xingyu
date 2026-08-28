package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordDetailPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordFailureDetailExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpImportExportRecordRespVO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordFinishReqBO;

import java.util.List;

public interface ErpImportExportRecordService {

    Long createRecord(ErpImportExportRecordCreateReqBO reqBO);

    void finishRecord(Long recordId, ErpImportExportRecordFinishReqBO reqBO);

    ErpImportExportRecordRespVO getRecord(Long id);

    PageResult<ErpImportExportRecordRespVO> getRecordPage(ErpImportExportRecordPageReqVO reqVO);

    PageResult<ErpImportExportRecordDetailRespVO> getDetailPage(ErpImportExportRecordDetailPageReqVO reqVO);

    List<ErpImportExportRecordFailureDetailExportRespVO> getFailureDetailList(Long recordId);

}
