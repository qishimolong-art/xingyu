package cn.iocoder.yudao.module.erp.framework.web;
import cn.iocoder.yudao.module.erp.service.stock.StockDecimalMath;
import cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInSaveReqVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import javax.validation.Validation;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class StockDecimalPrecisionTest {
 @Test void confirmedExampleAndNegativeHalfUp() {
  assertEquals(new BigDecimal("5.055556"),StockDecimalMath.multiply(new BigDecimal("0.5"),new BigDecimal("10.111111")));
  assertEquals(new BigDecimal("-5.055556"),StockDecimalMath.multiply(new BigDecimal("-0.5"),new BigDecimal("10.111111")));
  assertEquals(new BigDecimal("5.0556"),StockDecimalMath.multiply(new BigDecimal("0.5"),new BigDecimal("10.111111"),4));
 }
 @Test void sourceValidationIgnoresZerosButRejectsOverflow() {
  StockDecimalMath.require(new BigDecimal("1.123456000"),"数量",18,6);
  StockDecimalMath.require(new BigDecimal("1.123400"),"旧库存金额",16,4);
  StockDecimalMath.require(new BigDecimal("1.1200"),"仓库额度",10,2);
  assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->StockDecimalMath.require(new BigDecimal("1.12345"),"旧库存金额",16,4));
  assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->StockDecimalMath.require(new BigDecimal("1.123"),"仓库额度",10,2));
  assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->StockDecimalMath.require(new BigDecimal("1.1234567"),"数量",18,6));
  assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->StockDecimalMath.require(new BigDecimal("1000000000000000000"),"数量",18,6));
  ErpStockInSaveReqVO.Item item=new ErpStockInSaveReqVO.Item(); item.setCount(new BigDecimal("0.1234567"));
  try(javax.validation.ValidatorFactory factory=Validation.buildDefaultValidatorFactory()) {
   assertFalse(factory.getValidator().validate(item).isEmpty());
   item.setCount(new BigDecimal("0.123456000")); assertTrue(factory.getValidator().validate(item).isEmpty());
  }
 }
 @Test void decimalModeIsExplicitAndKeepsIdsMasksAndNulls() {
  ObjectMapper mapper=new ObjectMapper(); StockDecimalResponseAdvice advice=new StockDecimalResponseAdvice(mapper);
  Map<String,Object> body=new LinkedHashMap<>(); body.put("id",123L); body.put("price",new BigDecimal("999999999999999999.123456")); body.put("mask","****"); body.put("empty",null);
  MockHttpServletRequest request=new MockHttpServletRequest("GET","/admin-api/erp/stock-in/get");
  assertSame(body,advice.beforeBodyWrite(body,null,MediaType.APPLICATION_JSON,MappingJackson2HttpMessageConverter.class,new ServletServerHttpRequest(request),new ServletServerHttpResponse(new MockHttpServletResponse())));
  request.setQueryString("decimalMode=string");
  JsonNode output=(JsonNode)advice.beforeBodyWrite(body,null,MediaType.APPLICATION_JSON,MappingJackson2HttpMessageConverter.class,new ServletServerHttpRequest(request),new ServletServerHttpResponse(new MockHttpServletResponse()));
  assertEquals("999999999999999999.123456",output.get("price").asText());
  assertTrue(output.get("id").isIntegralNumber()); assertEquals("****",output.get("mask").asText()); assertTrue(output.get("empty").isNull());
 }
 @Test void excelUsesNumericCellsUnlessTextIsRequiredForFidelity() throws Exception {
  StockDecimalExcelConverter converter=new StockDecimalExcelConverter();
  assertEquals(cn.idev.excel.enums.CellDataTypeEnum.NUMBER,converter.convertToExcelData(new BigDecimal("5.055556"),null,null).getType());
  assertEquals("999999999999999999.123456",converter.convertToExcelData(new BigDecimal("999999999999999999.123456"),null,null).getStringValue());
 }
 public static class ExcelRow {
  @cn.idev.excel.annotation.ExcelProperty(value="金额",converter=StockDecimalExcelConverter.class)
  @cn.idev.excel.annotation.format.NumberFormat("0.####################")
  private BigDecimal amount;
  @cn.idev.excel.annotation.ExcelProperty(value="高精度值",converter=StockDecimalExcelConverter.class)
  private BigDecimal precise;
  public BigDecimal getAmount(){return amount;}
  public BigDecimal getPrecise(){return precise;}
 }
 @Test void realExcelCellsKeepSixDecimalsAndLargeValues() throws Exception {
  ExcelRow row=new ExcelRow(); row.amount=new BigDecimal("5.055556"); row.precise=new BigDecimal("999999999999999999.123456");
  java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
  cn.idev.excel.FastExcelFactory.write(bytes,ExcelRow.class).sheet("数据").doWrite(java.util.Collections.singletonList(row));
  try(org.apache.poi.xssf.usermodel.XSSFWorkbook book=new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
   org.apache.poi.ss.usermodel.Row result=book.getSheetAt(0).getRow(1);
   assertEquals(5.055556,result.getCell(0).getNumericCellValue(),0.0000000001);
   assertEquals("0.####################",result.getCell(0).getCellStyle().getDataFormatString());
   assertEquals("999999999999999999.123456",result.getCell(1).getStringCellValue());
  }
 }

 @Test void allStockExcelBindingsAreDecimals() throws Exception {
  String[] names={"cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckExportRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInExportRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillExportRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockTransferExportRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutExportRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillExportRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordApprovalTimeReportExportRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordReportExportRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordReportRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerDetailRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseExportRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveExportRespVO","cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveRespVO"};
  for(String name:names) verifyExcelBindings(Class.forName(name));
 }
 private void verifyExcelBindings(Class<?> type) {
  for(java.lang.reflect.Field field:type.getDeclaredFields()) {
   cn.idev.excel.annotation.ExcelProperty property=field.getAnnotation(cn.idev.excel.annotation.ExcelProperty.class);
   if(property!=null && property.converter()==StockDecimalExcelConverter.class)
    assertEquals(BigDecimal.class,field.getType(),type.getName()+"."+field.getName());
  }
  for(Class<?> nested:type.getDeclaredClasses()) verifyExcelBindings(nested);
 }
 @Test void realStockDocumentWorkbookPreservesCodesAndDecimalAmounts() throws Exception {
  cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckRespVO row=
   new cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckRespVO();
  row.setProductCodes("000123");row.setTotalPrice(new BigDecimal("5.055556"));
  java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
  cn.idev.excel.FastExcelFactory.write(bytes,row.getClass()).sheet("盘点").doWrite(java.util.List.of(row));
  try(org.apache.poi.xssf.usermodel.XSSFWorkbook book=new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
   org.apache.poi.ss.usermodel.Row head=book.getSheetAt(0).getRow(0),data=book.getSheetAt(0).getRow(1);
   boolean code=false,amount=false;
   for(org.apache.poi.ss.usermodel.Cell cell:head) {
    if("产品编码".equals(cell.getStringCellValue())) {assertEquals("000123",data.getCell(cell.getColumnIndex()).getStringCellValue());code=true;}
    if("合计金额".equals(cell.getStringCellValue())) {assertEquals(5.055556,data.getCell(cell.getColumnIndex()).getNumericCellValue(),1e-10);amount=true;}
   }
   assertTrue(code);assertTrue(amount);
  }
 }

 @Test void stockImportReadsDecimalCellsWithTheBuiltInReadConverter() throws Exception {
  cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO row=
   new cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO();
  row.setProductCode("000123");row.setCount(new BigDecimal("0.5"));row.setProductPrice(new BigDecimal("10.111111"));
  java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
  cn.idev.excel.FastExcelFactory.write(bytes,row.getClass()).sheet("导入").doWrite(java.util.List.of(row));
  java.util.List<cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO> result=
   cn.idev.excel.FastExcelFactory.read(new java.io.ByteArrayInputStream(bytes.toByteArray()))
    .head(cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO.class).sheet().doReadSync();
  assertEquals("000123",result.get(0).getProductCode());
  assertEquals(0,new BigDecimal("0.5").compareTo(result.get(0).getCount()));
  assertEquals(0,new BigDecimal("10.111111").compareTo(result.get(0).getProductPrice()));
 }

}
