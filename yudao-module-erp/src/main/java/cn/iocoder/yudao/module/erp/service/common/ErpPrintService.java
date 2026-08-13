package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintRecordCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintTemplateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintTemplateSaveReqVO;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ErpPrintService {

    ErpPrintFieldRespVO getFields(String moduleKey);

    ErpPrintTemplateRespVO getDefaultTemplate(String moduleKey);

    List<ErpPrintTemplateRespVO> getTemplateList(String moduleKey);

    ErpPrintTemplateRespVO getTemplate(Long id);

    Long saveTemplate(@Valid ErpPrintTemplateSaveReqVO reqVO);

    Long saveAsTemplate(@Valid ErpPrintTemplateSaveReqVO reqVO);

    void recordPrint(@Valid ErpPrintRecordCreateReqVO reqVO);

    Map<Long, Long> getPrintCountMap(String moduleKey, Collection<Long> businessIds);

    Map<Long, LocalDateTime> getLastPrintTimeMap(String moduleKey, Collection<Long> businessIds);

}
