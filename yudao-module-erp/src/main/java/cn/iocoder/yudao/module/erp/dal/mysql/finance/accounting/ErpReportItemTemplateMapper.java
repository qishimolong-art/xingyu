package cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplatePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpReportItemTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 三大报表项目模板 Mapper
 *
 * @author Claude
 */
@Mapper
public interface ErpReportItemTemplateMapper extends BaseMapperX<ErpReportItemTemplateDO> {

    default List<ErpReportItemTemplateDO> selectListByReportType(Integer reportType) {
        return selectList(new LambdaQueryWrapperX<ErpReportItemTemplateDO>()
                .eq(ErpReportItemTemplateDO::getReportType, reportType)
                .orderByAsc(ErpReportItemTemplateDO::getSort)
                .orderByAsc(ErpReportItemTemplateDO::getRowNo));
    }

    default List<ErpReportItemTemplateDO> selectListByReportTypeAndSide(Integer reportType, Integer side) {
        return selectList(new LambdaQueryWrapperX<ErpReportItemTemplateDO>()
                .eq(ErpReportItemTemplateDO::getReportType, reportType)
                .eqIfPresent(ErpReportItemTemplateDO::getSide, side)
                .orderByAsc(ErpReportItemTemplateDO::getSort)
                .orderByAsc(ErpReportItemTemplateDO::getRowNo));
    }

    default PageResult<ErpReportItemTemplateDO> selectPage(ErpReportItemTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpReportItemTemplateDO>()
                .eq(ErpReportItemTemplateDO::getReportType, reqVO.getReportType())
                .eqIfPresent(ErpReportItemTemplateDO::getSide, reqVO.getSide())
                .likeIfPresent(ErpReportItemTemplateDO::getItemName, reqVO.getItemName())
                .eqIfPresent(ErpReportItemTemplateDO::getEnable, reqVO.getEnable())
                .orderByAsc(ErpReportItemTemplateDO::getSort)
                .orderByAsc(ErpReportItemTemplateDO::getRowNo));
    }

}
