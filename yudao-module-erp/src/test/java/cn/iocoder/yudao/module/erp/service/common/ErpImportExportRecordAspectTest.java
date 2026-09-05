package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.imports.ErpSaleImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportRespVO;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportRecordStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportFailureDetailBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.common.bo.ErpImportExportRecordFinishReqBO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ERP_IMPORT_ROW_LIMIT_EXCEEDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpImportExportRecordAspectTest extends BaseMockitoUnitTest {

    private final ErpImportExportRecordAspect aspect = new ErpImportExportRecordAspect();

    @Test
    void fillRecordIdIfPresent_setsRecordIdOnImportResponseData() {
        ErpPurchaseOrderImportResultRespVO data = new ErpPurchaseOrderImportResultRespVO();
        CommonResult<ErpPurchaseOrderImportResultRespVO> result = CommonResult.success(data);

        ReflectionTestUtils.invokeMethod(aspect, "fillRecordIdIfPresent", result, 100L);

        assertEquals(100L, data.getRecordId());
    }

    @Test
    void fillRecordIdIfPresent_setsRecordIdOnSharedPurchaseImportResponseData() {
        ErpPurchaseImportResultRespVO data = new ErpPurchaseImportResultRespVO();
        CommonResult<ErpPurchaseImportResultRespVO> result = CommonResult.success(data);

        ReflectionTestUtils.invokeMethod(aspect, "fillRecordIdIfPresent", result, 101L);

        assertEquals(101L, data.getRecordId());
    }

    @Test
    void fillRecordIdIfPresent_setsRecordIdOnSharedSaleImportResponseData() {
        ErpSaleImportResultRespVO data = new ErpSaleImportResultRespVO();
        CommonResult<ErpSaleImportResultRespVO> result = CommonResult.success(data);

        ReflectionTestUtils.invokeMethod(aspect, "fillRecordIdIfPresent", result, 102L);

        assertEquals(102L, data.getRecordId());
    }

    @Test
    void fillRecordIdIfPresent_setsRecordIdOnSaleOrderImportResponseData() {
        ErpSaleOrderImportRespVO data = new ErpSaleOrderImportRespVO();
        CommonResult<ErpSaleOrderImportRespVO> result = CommonResult.success(data);

        ReflectionTestUtils.invokeMethod(aspect, "fillRecordIdIfPresent", result, 103L);

        assertEquals(103L, data.getRecordId());
    }

    @Test
    void fillRecordIdIfPresent_setsRecordIdOnSharedStockImportResponseData() {
        ErpStockImportResultRespVO data = new ErpStockImportResultRespVO();
        CommonResult<ErpStockImportResultRespVO> result = CommonResult.success(data);

        ReflectionTestUtils.invokeMethod(aspect, "fillRecordIdIfPresent", result, 104L);

        assertEquals(104L, data.getRecordId());
    }

    @Test
    void fillRecordIdIfPresent_setsRecordIdOnWarehouseImportResponseData() {
        ErpWarehouseImportRespVO data = new ErpWarehouseImportRespVO();
        CommonResult<ErpWarehouseImportRespVO> result = CommonResult.success(data);

        ReflectionTestUtils.invokeMethod(aspect, "fillRecordIdIfPresent", result, 105L);

        assertEquals(105L, data.getRecordId());
    }

    @Test
    void fillRecordIdIfPresent_setsRecordIdOnSharedFinanceImportResponseData() {
        ErpFinanceImportRespVO data = new ErpFinanceImportRespVO();
        CommonResult<ErpFinanceImportRespVO> result = CommonResult.success(data);

        ReflectionTestUtils.invokeMethod(aspect, "fillRecordIdIfPresent", result, 106L);

        assertEquals(106L, data.getRecordId());
    }

    @Test
    void validateImportRowLimitIfNeeded_allowsBoundaryRows() throws Exception {
        ReflectionTestUtils.setField(aspect, "importMaxRows", 3);
        Method method = ImportController.class.getDeclaredMethod("importExcel", MultipartFile.class);

        ReflectionTestUtils.invokeMethod(aspect, "validateImportRowLimitIfNeeded", method,
                new Object[]{excelFileWithRows(4)});
    }

    @Test
    void validateImportRowLimitIfNeeded_rejectsBusinessImportWhenDataRowsExceedLimit() throws Exception {
        ReflectionTestUtils.setField(aspect, "importMaxRows", 3);
        Method method = ImportController.class.getDeclaredMethod("importExcel", MultipartFile.class);

        ServiceException ex = assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(aspect,
                "validateImportRowLimitIfNeeded", method, new Object[]{excelFileWithRows(5)}));

        assertEquals(ERP_IMPORT_ROW_LIMIT_EXCEEDED.getCode(), ex.getCode());
        assertEquals("导入数据超过最大行数限制，当前 4 行，最多允许 3 行", ex.getMessage());
    }

    @Test
    void validateImportRowLimitIfNeeded_rejectsParseImportWhenDataRowsExceedLimit() throws Exception {
        ReflectionTestUtils.setField(aspect, "importMaxRows", 3);
        Method method = ImportController.class.getDeclaredMethod("parseDetailImportExcel", MultipartFile.class);

        ServiceException ex = assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(aspect,
                "validateImportRowLimitIfNeeded", method, new Object[]{excelFileWithRows(2, 5)}));

        assertEquals(ERP_IMPORT_ROW_LIMIT_EXCEEDED.getCode(), ex.getCode());
        assertEquals("导入数据超过最大行数限制，当前 4 行，最多允许 3 行", ex.getMessage());
    }

    @Test
    void around_whenBusinessImportExceedsRowLimit_thenRecordsFailureAndSkipsController() throws Throwable {
        ReflectionTestUtils.setField(aspect, "importMaxRows", 3);
        ErpImportExportRecordService recordService = mock(ErpImportExportRecordService.class);
        ReflectionTestUtils.setField(aspect, "importExportRecordService", recordService);
        when(recordService.createRecord(any(ErpImportExportRecordCreateReqBO.class))).thenReturn(200L);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = ImportController.class.getDeclaredMethod("importExcel", MultipartFile.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{excelFileWithRows(5)});
        when(joinPoint.getTarget()).thenReturn(new ImportController());

        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.around(joinPoint));

        assertEquals(ERP_IMPORT_ROW_LIMIT_EXCEEDED.getCode(), ex.getCode());
        verify(joinPoint, never()).proceed();
        ArgumentCaptor<ErpImportExportRecordFinishReqBO> finishCaptor =
                ArgumentCaptor.forClass(ErpImportExportRecordFinishReqBO.class);
        verify(recordService).finishRecord(eq(200L), finishCaptor.capture());
        assertEquals(ErpImportExportRecordStatusEnum.FAILURE.getStatus(), finishCaptor.getValue().getStatus());
        assertEquals("导入数据超过最大行数限制，当前 4 行，最多允许 3 行",
                finishCaptor.getValue().getErrorMessage());
    }

    @Test
    void buildFailureDetails_singleRowTemplateOnlyKeepsFailedRow() {
        ErpProductUnitImportExcelVO first = new ErpProductUnitImportExcelVO();
        first.setName("个");
        ErpProductUnitImportExcelVO second = new ErpProductUnitImportExcelVO();
        second.setName("箱");
        ErpImportExportFailureDetailBO failure = failure(3, "箱", "单位已存在");
        ExcelUtils.ExcelOperationContext context = new ExcelUtils.ExcelOperationContext("READ",
                "产品单位.xls", 2, null, ErpProductUnitImportExcelVO.class.getName(), Arrays.asList(first, second));

        List<ErpImportExportFailureDetailBO> result = ReflectionTestUtils.invokeMethod(aspect,
                "buildFailureDetailsWithImportRows", Collections.singletonList(failure), context);

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getRowNo());
        assertEquals("ROW-3", result.get(0).getGroupKey());
        assertEquals("FAILURE", result.get(0).getDetailType());
        assertEquals("单位已存在", result.get(0).getFailureReason());
        assertEquals(second, result.get(0).getRawData());
    }

    @Test
    void buildFailureDetails_groupedTemplateKeepsFailedDocumentGroup() {
        ErpSaleQuoteOrderImportExcelVO first = quoteRow("客户A", "P001");
        ErpSaleQuoteOrderImportExcelVO second = quoteRow(null, "P002");
        ErpSaleQuoteOrderImportExcelVO third = quoteRow("客户B", "P003");
        ErpImportExportFailureDetailBO failure = failure(3, "P002", "产品不存在");
        ExcelUtils.ExcelOperationContext context = new ExcelUtils.ExcelOperationContext("READ",
                "报价订单.xls", 3, null, ErpSaleQuoteOrderImportExcelVO.class.getName(),
                Arrays.asList(first, second, third));

        List<ErpImportExportFailureDetailBO> result = ReflectionTestUtils.invokeMethod(aspect,
                "buildFailureDetailsWithImportRows", Collections.singletonList(failure), context);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getRowNo());
        assertEquals("GROUP-2", result.get(0).getGroupKey());
        assertEquals("CONTEXT", result.get(0).getDetailType());
        assertEquals("", result.get(0).getFailureReason());
        assertEquals(first, result.get(0).getRawData());
        assertEquals(3, result.get(1).getRowNo());
        assertEquals("GROUP-2", result.get(1).getGroupKey());
        assertEquals("FAILURE", result.get(1).getDetailType());
        assertEquals("产品不存在", result.get(1).getFailureReason());
        assertEquals(second, result.get(1).getRawData());
    }

    @Test
    void buildFailureDetails_purchaseOrderGroupedTemplateKeepsFailedOrderGroup() {
        ErpPurchaseOrderImportExcelVO first = purchaseOrderRow("示例供应商", "P001");
        ErpPurchaseOrderImportExcelVO second = purchaseOrderRow(null, "P002");
        ErpPurchaseOrderImportExcelVO third = purchaseOrderRow("另一供应商", "P003");
        ErpImportExportFailureDetailBO failure = failure(3, "P002", "产品不存在");
        ExcelUtils.ExcelOperationContext context = new ExcelUtils.ExcelOperationContext("READ",
                "采购订单.xls", 3, null, ErpPurchaseOrderImportExcelVO.class.getName(),
                Arrays.asList(first, second, third));

        List<ErpImportExportFailureDetailBO> result = ReflectionTestUtils.invokeMethod(aspect,
                "buildFailureDetailsWithImportRows", Collections.singletonList(failure), context);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getRowNo());
        assertEquals("GROUP-2", result.get(0).getGroupKey());
        assertEquals("CONTEXT", result.get(0).getDetailType());
        assertEquals(first, result.get(0).getRawData());
        assertEquals(3, result.get(1).getRowNo());
        assertEquals("GROUP-2", result.get(1).getGroupKey());
        assertEquals("FAILURE", result.get(1).getDetailType());
        assertEquals("产品不存在", result.get(1).getFailureReason());
        assertEquals(second, result.get(1).getRawData());
    }

    @Test
    void buildFailureDetails_stockGroupedTemplateKeepsFailedDocumentGroup() {
        ErpStockImportExcelVO first = stockRow("IN-001", "示例仓库", "P001");
        ErpStockImportExcelVO second = stockRow(null, null, "P002");
        ErpStockImportExcelVO third = stockRow("IN-002", "另一仓库", "P003");
        ErpImportExportFailureDetailBO failure = failure(3, "P002", "产品不存在");
        ExcelUtils.ExcelOperationContext context = new ExcelUtils.ExcelOperationContext("READ",
                "其它入库.xls", 3, null, ErpStockImportExcelVO.class.getName(),
                Arrays.asList(first, second, third));

        List<ErpImportExportFailureDetailBO> result = ReflectionTestUtils.invokeMethod(aspect,
                "buildFailureDetailsWithImportRows", Collections.singletonList(failure), context);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getRowNo());
        assertEquals("orderNo:IN-001", result.get(0).getGroupKey());
        assertEquals("CONTEXT", result.get(0).getDetailType());
        assertEquals(first, result.get(0).getRawData());
        assertEquals(3, result.get(1).getRowNo());
        assertEquals("orderNo:IN-001", result.get(1).getGroupKey());
        assertEquals("FAILURE", result.get(1).getDetailType());
        assertEquals("产品不存在", result.get(1).getFailureReason());
        assertEquals(second, result.get(1).getRawData());
    }

    @Test
    void buildFailureDetails_financeGroupedTemplateKeepsFailedAdjacentGroupRows() {
        ErpFinanceReceiptImportExcelVO first = receiptRow("客户A", "账户A", "SO-001");
        ErpFinanceReceiptImportExcelVO second = receiptRow(null, null, null);
        second.setTotalPrice(null);
        second.setReceiptPrice(null);
        ErpFinanceReceiptImportExcelVO third = receiptRow("客户B", "账户B", "SO-002");
        ErpImportExportFailureDetailBO failure = failure(3, "SO-001", "本次收款必须大于 0");
        ExcelUtils.ExcelOperationContext context = new ExcelUtils.ExcelOperationContext("READ",
                "收款单.xls", 3, null, ErpFinanceReceiptImportExcelVO.class.getName(),
                Arrays.asList(first, second, third));

        List<ErpImportExportFailureDetailBO> result = ReflectionTestUtils.invokeMethod(aspect,
                "buildFailureDetailsWithImportRows", Collections.singletonList(failure), context);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getRowNo());
        assertEquals("GROUP-2", result.get(0).getGroupKey());
        assertEquals("CONTEXT", result.get(0).getDetailType());
        assertEquals(first, result.get(0).getRawData());
        assertEquals(3, result.get(1).getRowNo());
        assertEquals("GROUP-2", result.get(1).getGroupKey());
        assertEquals("FAILURE", result.get(1).getDetailType());
        assertEquals("本次收款必须大于 0", result.get(1).getFailureReason());
        assertEquals(second, result.get(1).getRawData());
    }


    @Test
    void buildFailureDetails_mergesMultipleReasonsForSameRow() {
        ErpSaleQuoteOrderImportExcelVO row = quoteRow("客户A", "P001");
        ExcelUtils.ExcelOperationContext context = new ExcelUtils.ExcelOperationContext("READ",
                "报价订单.xls", 1, null, ErpSaleQuoteOrderImportExcelVO.class.getName(), Collections.singletonList(row));

        List<ErpImportExportFailureDetailBO> result = ReflectionTestUtils.invokeMethod(aspect,
                "buildFailureDetailsWithImportRows",
                Arrays.asList(failure(2, "P001", "产品不存在"), failure(2, "P001", "数量必须大于 0")),
                context);

        assertEquals(1, result.size());
        assertEquals("产品不存在；数量必须大于 0", result.get(0).getFailureReason());
    }

    private ErpSaleQuoteOrderImportExcelVO quoteRow(String customerName, String productCode) {
        ErpSaleQuoteOrderImportExcelVO row = new ErpSaleQuoteOrderImportExcelVO();
        row.setCustomerName(customerName);
        row.setProductCode(productCode);
        row.setItemCount(BigDecimal.ONE);
        row.setProductPrice(BigDecimal.TEN);
        return row;
    }

    private ErpPurchaseOrderImportExcelVO purchaseOrderRow(String supplierName, String productCode) {
        ErpPurchaseOrderImportExcelVO row = new ErpPurchaseOrderImportExcelVO();
        row.setSupplierName(supplierName);
        row.setProductCode(productCode);
        row.setItemCount(BigDecimal.ONE);
        row.setProductPrice(BigDecimal.TEN);
        return row;
    }

    private ErpStockImportExcelVO stockRow(String orderNo, String warehouseName, String productCode) {
        ErpStockImportExcelVO row = new ErpStockImportExcelVO();
        row.setOrderNo(orderNo);
        row.setWarehouseName(warehouseName);
        row.setProductCode(productCode);
        row.setCount(BigDecimal.ONE);
        row.setProductPrice(BigDecimal.TEN);
        return row;
    }

    private ErpFinanceReceiptImportExcelVO receiptRow(String customerName, String accountName, String bizNo) {
        ErpFinanceReceiptImportExcelVO row = new ErpFinanceReceiptImportExcelVO();
        row.setCustomerName(customerName);
        row.setAccountName(accountName);
        row.setBizNo(bizNo);
        row.setTotalPrice(BigDecimal.TEN);
        row.setReceiptPrice(BigDecimal.ONE);
        return row;
    }

    private ErpImportExportFailureDetailBO failure(Integer rowNo, String bizKey, String reason) {
        ErpImportExportFailureDetailBO failure = new ErpImportExportFailureDetailBO();
        failure.setRowNo(rowNo);
        failure.setBizKey(bizKey);
        failure.setFailureReason(reason);
        return failure;
    }

    private MockMultipartFile excelFileWithRows(int... nonBlankRowNumbers) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("data");
            sheet.createRow(0).createCell(0).setCellValue("名称");
            for (int rowNumber : nonBlankRowNumbers) {
                sheet.createRow(rowNumber - 1).createCell(0).setCellValue("row-" + rowNumber);
            }
            workbook.write(outputStream);
            return new MockMultipartFile("file", "rows.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    @RequestMapping("/erp/test-import")
    private static class ImportController {

        @PostMapping("/import")
        public void importExcel(MultipartFile file) {
        }

        @PostMapping("/parse-detail-import-excel")
        public void parseDetailImportExcel(MultipartFile file) {
        }
    }
}
