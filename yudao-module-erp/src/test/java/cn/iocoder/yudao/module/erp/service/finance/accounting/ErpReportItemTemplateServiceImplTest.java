package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplatePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplateSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpReportItemTemplateDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpReportItemTemplateMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.REPORT_TEMPLATE_NOT_EXISTS;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ErpReportItemTemplateServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpReportItemTemplateServiceImpl reportItemTemplateService;

    @Mock
    private ErpReportItemTemplateMapper reportItemTemplateMapper;

    // ==================== create ====================

    @Test
    void testCreateReportItemTemplate_success() {
        // 准备参数
        ErpReportItemTemplateSaveReqVO reqVO = new ErpReportItemTemplateSaveReqVO();
        reqVO.setReportType(1);
        reqVO.setSide(1);
        reqVO.setItemName("货币资金");
        reqVO.setRowNo(1);
        reqVO.setSort(1);
        reqVO.setEnable(true);

        // mock: insert 时设置 ID
        doAnswer(invocation -> {
            ErpReportItemTemplateDO arg = invocation.getArgument(0);
            arg.setId(200L);
            return null;
        }).when(reportItemTemplateMapper).insert(any(ErpReportItemTemplateDO.class));

        // 调用
        Long id = reportItemTemplateService.createReportItemTemplate(reqVO);

        // 断言
        assertThat(id).isEqualTo(200L);
        verify(reportItemTemplateMapper).insert(any(ErpReportItemTemplateDO.class));
    }

    // ==================== update ====================

    @Test
    void testUpdateReportItemTemplate_success() {
        // 准备参数
        ErpReportItemTemplateSaveReqVO reqVO = new ErpReportItemTemplateSaveReqVO();
        reqVO.setId(100L);
        reqVO.setReportType(1);
        reqVO.setItemName("应收账款");
        reqVO.setSort(2);
        reqVO.setEnable(true);

        // mock: selectById 返回已有记录
        ErpReportItemTemplateDO existing = new ErpReportItemTemplateDO();
        existing.setId(100L);
        existing.setItemName("货币资金");
        when(reportItemTemplateMapper.selectById(100L)).thenReturn(existing);

        // 调用
        reportItemTemplateService.updateReportItemTemplate(reqVO);

        // 断言
        verify(reportItemTemplateMapper).updateById(any(ErpReportItemTemplateDO.class));
    }

    @Test
    void testUpdateReportItemTemplate_notExists() {
        // 准备参数
        ErpReportItemTemplateSaveReqVO reqVO = new ErpReportItemTemplateSaveReqVO();
        reqVO.setId(999L);
        reqVO.setReportType(1);
        reqVO.setItemName("不存在的项目");
        reqVO.setSort(1);
        reqVO.setEnable(true);

        // mock: selectById 返回 null
        when(reportItemTemplateMapper.selectById(999L)).thenReturn(null);

        // 调用并断言异常
        assertServiceException(() -> reportItemTemplateService.updateReportItemTemplate(reqVO),
                REPORT_TEMPLATE_NOT_EXISTS);
    }

    // ==================== delete ====================

    @Test
    void testDeleteReportItemTemplate_success() {
        // mock: selectById 返回已有记录
        ErpReportItemTemplateDO existing = new ErpReportItemTemplateDO();
        existing.setId(100L);
        when(reportItemTemplateMapper.selectById(100L)).thenReturn(existing);

        // 调用
        reportItemTemplateService.deleteReportItemTemplate(100L);

        // 断言
        verify(reportItemTemplateMapper).deleteById(100L);
    }

    @Test
    void testDeleteReportItemTemplate_notExists() {
        // mock: selectById 返回 null
        when(reportItemTemplateMapper.selectById(999L)).thenReturn(null);

        // 调用并断言异常
        assertServiceException(() -> reportItemTemplateService.deleteReportItemTemplate(999L),
                REPORT_TEMPLATE_NOT_EXISTS);
    }

    // ==================== get ====================

    @Test
    void testGetReportItemTemplate() {
        // mock
        ErpReportItemTemplateDO template = new ErpReportItemTemplateDO();
        template.setId(100L);
        template.setReportType(1);
        template.setItemName("货币资金");
        when(reportItemTemplateMapper.selectById(100L)).thenReturn(template);

        // 调用
        ErpReportItemTemplateDO result = reportItemTemplateService.getReportItemTemplate(100L);

        // 断言
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getItemName()).isEqualTo("货币资金");
    }

    // ==================== list ====================

    @Test
    void testGetReportItemTemplateList() {
        // mock
        ErpReportItemTemplateDO t1 = new ErpReportItemTemplateDO();
        t1.setId(1L);
        t1.setReportType(1);
        t1.setSide(1);
        t1.setItemName("货币资金");
        ErpReportItemTemplateDO t2 = new ErpReportItemTemplateDO();
        t2.setId(2L);
        t2.setReportType(1);
        t2.setSide(1);
        t2.setItemName("应收账款");
        when(reportItemTemplateMapper.selectListByReportTypeAndSide(1, 1))
                .thenReturn(Arrays.asList(t1, t2));

        // 调用
        List<ErpReportItemTemplateDO> result = reportItemTemplateService.getReportItemTemplateList(1, 1);

        // 断言
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getItemName()).isEqualTo("货币资金");
        assertThat(result.get(1).getItemName()).isEqualTo("应收账款");
    }

    // ==================== page ====================

    @Test
    void testGetReportItemTemplatePage() {
        // 准备参数
        ErpReportItemTemplatePageReqVO pageReqVO = new ErpReportItemTemplatePageReqVO();
        pageReqVO.setReportType(2);
        pageReqVO.setSide(null);
        pageReqVO.setItemName("营业");

        // mock
        ErpReportItemTemplateDO t1 = new ErpReportItemTemplateDO();
        t1.setId(10L);
        t1.setReportType(2);
        t1.setItemName("营业收入");
        PageResult<ErpReportItemTemplateDO> expected = new PageResult<>(singletonList(t1), 1L);
        when(reportItemTemplateMapper.selectPage(pageReqVO)).thenReturn(expected);

        // 调用
        PageResult<ErpReportItemTemplateDO> result = reportItemTemplateService.getReportItemTemplatePage(pageReqVO);

        // 断言
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getItemName()).isEqualTo("营业收入");
    }

}
