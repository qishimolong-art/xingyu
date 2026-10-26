package cn.iocoder.yudao.module.erp.service.common;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportExcelVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.*;
import cn.iocoder.yudao.module.erp.dal.mysql.common.*;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.ByteArrayInputStream;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ErpCustomerImportFailureDownloadTest extends BaseMockitoUnitTest {
 @InjectMocks ErpImportExportRecordServiceImpl service;
 @Mock ErpImportExportRecordMapper recordMapper;
 @Mock ErpImportExportRecordDetailMapper detailMapper;
 @Test void keepsCustomerTemplateOrderAndAddsOriginalRowAndReasonForOnlyFailedRows() throws Exception {
  ErpImportExportRecordDO record = ErpImportExportRecordDO.builder().id(1L).moduleKey("erp_customer").templateKey(ErpCustomerImportExcelVO.class.getName()).fileName("客户.xlsx").build();
  when(recordMapper.selectById(1L)).thenReturn(record);
  when(detailMapper.selectListByRecordId(1L)).thenReturn(Arrays.asList(
   ErpImportExportRecordDetailDO.builder().recordId(1L).rowNo(2).detailType("CONTEXT").rawData("{\"name\":\"成功行\"}").build(),
   ErpImportExportRecordDetailDO.builder().recordId(1L).rowNo(7).detailType("FAILURE").rawData("{\"name\":\"失败客户\",\"code\":\"0001\"}").failureReason("部门不存在").build()));
  MockHttpServletResponse response = new MockHttpServletResponse(); service.downloadFailureDetails(1L, response);
  try (org.apache.poi.ss.usermodel.Workbook book = org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
   org.apache.poi.ss.usermodel.Sheet sheet = book.getSheetAt(0); assertEquals(1, sheet.getLastRowNum());
   java.util.List<String> headers = new ArrayList<>();
   for (java.lang.reflect.Field field : ErpCustomerImportExcelVO.class.getDeclaredFields()) {
    cn.idev.excel.annotation.ExcelProperty property = field.getAnnotation(cn.idev.excel.annotation.ExcelProperty.class);
    if (property != null) headers.add(property.value()[0]);
   }
   for (int i=0;i<headers.size();i++) assertEquals(headers.get(i), sheet.getRow(0).getCell(i).getStringCellValue());
   int n=headers.size(); assertEquals("原始行号", sheet.getRow(0).getCell(n).getStringCellValue());
   assertEquals("7", new org.apache.poi.ss.usermodel.DataFormatter().formatCellValue(sheet.getRow(1).getCell(n)));
   assertEquals("部门不存在", sheet.getRow(1).getCell(n+1).getStringCellValue());
  }
 }
}
