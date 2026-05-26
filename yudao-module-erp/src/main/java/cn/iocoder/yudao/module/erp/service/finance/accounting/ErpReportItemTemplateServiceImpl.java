package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplatePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplateSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpReportItemTemplateDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpReportItemTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.REPORT_TEMPLATE_NOT_EXISTS;

/**
 * ERP 三大报表项目模板 Service 实现类
 *
 * @author Claude
 */
@Service
@Validated
public class ErpReportItemTemplateServiceImpl implements ErpReportItemTemplateService {

    @Resource
    private ErpReportItemTemplateMapper reportItemTemplateMapper;

    @Override
    public Long createReportItemTemplate(ErpReportItemTemplateSaveReqVO createReqVO) {
        ErpReportItemTemplateDO template = BeanUtils.toBean(createReqVO, ErpReportItemTemplateDO.class);
        reportItemTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    public void updateReportItemTemplate(ErpReportItemTemplateSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        ErpReportItemTemplateDO updateObj = BeanUtils.toBean(updateReqVO, ErpReportItemTemplateDO.class);
        reportItemTemplateMapper.updateById(updateObj);
    }

    @Override
    public void deleteReportItemTemplate(Long id) {
        validateExists(id);
        reportItemTemplateMapper.deleteById(id);
    }

    private void validateExists(Long id) {
        if (reportItemTemplateMapper.selectById(id) == null) {
            throw exception(REPORT_TEMPLATE_NOT_EXISTS);
        }
    }

    @Override
    public ErpReportItemTemplateDO getReportItemTemplate(Long id) {
        return reportItemTemplateMapper.selectById(id);
    }

    @Override
    public List<ErpReportItemTemplateDO> getReportItemTemplateList(Integer reportType, Integer side) {
        return reportItemTemplateMapper.selectListByReportTypeAndSide(reportType, side);
    }

    @Override
    public PageResult<ErpReportItemTemplateDO> getReportItemTemplatePage(ErpReportItemTemplatePageReqVO pageReqVO) {
        return reportItemTemplateMapper.selectPage(pageReqVO);
    }

}
