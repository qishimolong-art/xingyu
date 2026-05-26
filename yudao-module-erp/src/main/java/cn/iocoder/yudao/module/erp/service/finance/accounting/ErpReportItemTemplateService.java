package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplatePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplateSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpReportItemTemplateDO;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 三大报表项目模板 Service 接口
 *
 * @author Claude
 */
public interface ErpReportItemTemplateService {

    /**
     * 创建报表项目
     */
    Long createReportItemTemplate(@Valid ErpReportItemTemplateSaveReqVO createReqVO);

    /**
     * 更新报表项目（含公式）
     */
    void updateReportItemTemplate(@Valid ErpReportItemTemplateSaveReqVO updateReqVO);

    /**
     * 删除报表项目
     */
    void deleteReportItemTemplate(Long id);

    /**
     * 获得报表项目
     */
    ErpReportItemTemplateDO getReportItemTemplate(Long id);

    /**
     * 按报表类型 + 区域获取列表（树形展示用）
     */
    List<ErpReportItemTemplateDO> getReportItemTemplateList(Integer reportType, Integer side);

    /**
     * 获得报表项目分页
     */
    PageResult<ErpReportItemTemplateDO> getReportItemTemplatePage(ErpReportItemTemplatePageReqVO pageReqVO);

}
