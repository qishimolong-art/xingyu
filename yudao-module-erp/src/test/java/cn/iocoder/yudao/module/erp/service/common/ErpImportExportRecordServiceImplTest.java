package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpImportExportRecordDetailDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordDetailMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpImportExportRecordMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpImportExportOperationTypeEnum;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ErpImportExportRecordServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpImportExportRecordServiceImpl service;

    @Mock
    private ErpImportExportRecordMapper recordMapper;
    @Mock
    private ErpImportExportRecordDetailMapper detailMapper;

    @Test
    void downloadOwnImportFailureDetails_allowsCurrentUsersMatchingImportRecord() throws Exception {
        Long recordId = 20L;
        ErpImportExportRecordDO record = record(recordId);
        record.setModuleKey("erp_purchase_in");
        record.setOperationType(ErpImportExportOperationTypeEnum.IMPORT.getType());
        record.setOperatorId(104L);
        record.setTemplateKey(null);
        when(recordMapper.selectById(recordId)).thenReturn(record);
        when(detailMapper.selectFailureListByRecordId(recordId)).thenReturn(Collections.emptyList());
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            service.downloadOwnImportFailureDetails(recordId, "erp_purchase_in", response);
        }

        verify(detailMapper).selectFailureListByRecordId(recordId);
    }

    @Test
    void downloadOwnImportFailureDetails_rejectsOtherUsersRecord() throws Exception {
        Long recordId = 21L;
        ErpImportExportRecordDO record = record(recordId);
        record.setModuleKey("erp_purchase_in");
        record.setOperationType(ErpImportExportOperationTypeEnum.IMPORT.getType());
        record.setOperatorId(999L);
        when(recordMapper.selectById(recordId)).thenReturn(record);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            assertThrows(AccessDeniedException.class, () -> service.downloadOwnImportFailureDetails(recordId,
                    "erp_purchase_in", new MockHttpServletResponse()));
        }

        verify(detailMapper, never()).selectFailureListByRecordId(recordId);
    }

    @Test
    void downloadOwnImportFailureDetails_rejectsDifferentModuleRecord() throws Exception {
        Long recordId = 22L;
        ErpImportExportRecordDO record = record(recordId);
        record.setModuleKey("erp_purchase_return");
        record.setOperationType(ErpImportExportOperationTypeEnum.IMPORT.getType());
        record.setOperatorId(104L);
        when(recordMapper.selectById(recordId)).thenReturn(record);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            assertThrows(AccessDeniedException.class, () -> service.downloadOwnImportFailureDetails(recordId,
                    "erp_purchase_in", new MockHttpServletResponse()));
        }

        verify(detailMapper, never()).selectFailureListByRecordId(recordId);
    }

    @Test
    void downloadOwnImportFailureDetails_rejectsExportRecord() throws Exception {
        Long recordId = 23L;
        ErpImportExportRecordDO record = record(recordId);
        record.setModuleKey("erp_purchase_in");
        record.setOperationType(ErpImportExportOperationTypeEnum.EXPORT.getType());
        record.setOperatorId(104L);
        when(recordMapper.selectById(recordId)).thenReturn(record);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            assertThrows(AccessDeniedException.class, () -> service.downloadOwnImportFailureDetails(recordId,
                    "erp_purchase_in", new MockHttpServletResponse()));
        }

        verify(detailMapper, never()).selectFailureListByRecordId(recordId);
    }

    @Test
    void downloadFailureDetails_writesTemplateFormatAndErrorColumnWhenTemplateExists() throws Exception {
        Long recordId = 10L;
        ErpImportExportRecordDO record = record(recordId);
        ErpImportExportRecordDetailDO context = detail(recordId, 2, "GROUP-1", "CONTEXT",
                "客户A", "客户A", "", "{\"customerName\":\"客户A\",\"remark\":\"整单备注\"}");
        ErpImportExportRecordDetailDO failure = detail(recordId, 3, "GROUP-1", "FAILURE",
                "P002", "产品P002", "产品不存在",
                "{\"customerName\":\"客户A\",\"remark\":\"整单备注\",\"productCode\":\"P002\","
                        + "\"warehouseName\":\"主仓\",\"itemCount\":2,\"productPrice\":12.5,"
                        + "\"itemRemark\":\"明细备注\"}");
        when(recordMapper.selectById(recordId)).thenReturn(record);
        when(detailMapper.selectListByRecordId(recordId)).thenReturn(Arrays.asList(context, failure));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.downloadFailureDetails(recordId, response);

        try (Workbook workbook = workbook(response)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("失败明细", sheet.getSheetName());
            assertTemplateFailureHeaders(sheet);
            assertCell(sheet, 1, 0, "客户A");
            assertCell(sheet, 1, 1, "整单备注");
            assertCell(sheet, 1, 9, "");
            assertCell(sheet, 2, 0, "客户A");
            assertCell(sheet, 2, 2, "P002");
            assertCell(sheet, 2, 4, "主仓");
            assertCell(sheet, 2, 5, "2");
            assertCell(sheet, 2, 6, "12.5");
            assertCell(sheet, 2, 8, "明细备注");
            assertCell(sheet, 2, 9, "产品不存在");
            assertEquals("attachment;filename=" + HttpUtils.encodeUtf8("报价订单导入-错误数据.xls"),
                    response.getHeader("Content-Disposition"));
        }
        verify(detailMapper).selectListByRecordId(recordId);
        verify(detailMapper, never()).selectFailureListByRecordId(recordId);
    }

    @Test
    void downloadFailureDetails_writesFailureDetailFormatWhenTemplateMissing() throws Exception {
        Long recordId = 11L;
        ErpImportExportRecordDO record = record(recordId);
        record.setTemplateKey(null);
        ErpImportExportRecordDetailDO failure = detail(recordId, 2, null, null,
                "P001", null, "产品不存在", "{\"productCode\":\"P001\"}");
        when(recordMapper.selectById(recordId)).thenReturn(record);
        when(detailMapper.selectFailureListByRecordId(recordId)).thenReturn(Collections.singletonList(failure));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.downloadFailureDetails(recordId, response);

        Sheet sheet = firstSheet(response);
        assertEquals("失败明细", sheet.getSheetName());
        assertFailureDetailHeaders(sheet);
        assertEquals(2D, sheet.getRow(1).getCell(0).getNumericCellValue());
        assertEquals("P001", sheet.getRow(1).getCell(1).getStringCellValue());
        assertEquals("产品不存在", sheet.getRow(1).getCell(3).getStringCellValue());
        assertEquals("{\"productCode\":\"P001\"}", sheet.getRow(1).getCell(4).getStringCellValue());
        assertEquals("attachment;filename=" + HttpUtils.encodeUtf8("报价订单导入-错误数据.xls"),
                response.getHeader("Content-Disposition"));
        verify(detailMapper, never()).selectListByRecordId(recordId);
    }

    @Test
    void downloadFailureDetails_usesModuleNameForFilenameWhenFileNameMissing() throws Exception {
        Long recordId = 12L;
        ErpImportExportRecordDO record = record(recordId);
        record.setFileName(null);
        record.setModuleName("收款表");
        when(recordMapper.selectById(recordId)).thenReturn(record);
        when(detailMapper.selectListByRecordId(recordId)).thenReturn(Collections.emptyList());
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.downloadFailureDetails(recordId, response);

        Sheet sheet = firstSheet(response);
        assertEquals("失败明细", sheet.getSheetName());
        assertTemplateFailureHeaders(sheet);
        assertEquals("attachment;filename=" + HttpUtils.encodeUtf8("收款表-错误数据.xls"),
                response.getHeader("Content-Disposition"));
    }

    @Test
    void downloadFailureDetails_fallsBackToFailureDetailFormatWhenTemplateClassMissing() throws Exception {
        Long recordId = 13L;
        ErpImportExportRecordDO record = record(recordId);
        record.setTemplateKey("cn.iocoder.yudao.module.erp.NotExistsImportExcelVO");
        ErpImportExportRecordDetailDO failure = detail(recordId, 5, null, "FAILURE",
                "P003", null, "模板不存在时兜底", "{\"productCode\":\"P003\"}");
        when(recordMapper.selectById(recordId)).thenReturn(record);
        when(detailMapper.selectFailureListByRecordId(recordId)).thenReturn(Collections.singletonList(failure));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.downloadFailureDetails(recordId, response);

        Sheet sheet = firstSheet(response);
        assertEquals("失败明细", sheet.getSheetName());
        assertFailureDetailHeaders(sheet);
        assertEquals(5D, sheet.getRow(1).getCell(0).getNumericCellValue());
        assertEquals("P003", sheet.getRow(1).getCell(1).getStringCellValue());
        assertEquals("模板不存在时兜底", sheet.getRow(1).getCell(3).getStringCellValue());
        assertEquals("{\"productCode\":\"P003\"}", sheet.getRow(1).getCell(4).getStringCellValue());
        verify(detailMapper, never()).selectListByRecordId(recordId);
    }

    private ErpImportExportRecordDO record(Long recordId) {
        return ErpImportExportRecordDO.builder()
                .id(recordId)
                .moduleName("报价订单")
                .fileName("报价订单导入.xls")
                .templateKey(ErpSaleQuoteOrderImportExcelVO.class.getName())
                .build();
    }

    private ErpImportExportRecordDetailDO detail(Long recordId, Integer rowNo, String groupKey, String detailType,
                                                 String bizKey, String bizName, String failureReason, String rawData) {
        return ErpImportExportRecordDetailDO.builder()
                .recordId(recordId)
                .rowNo(rowNo)
                .groupKey(groupKey)
                .detailType(detailType)
                .bizKey(bizKey)
                .bizName(bizName)
                .failureReason(failureReason)
                .rawData(rawData)
                .build();
    }

    private void assertFailureDetailHeaders(Sheet sheet) {
        assertEquals("行号", sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("业务标识", sheet.getRow(0).getCell(1).getStringCellValue());
        assertEquals("业务名称", sheet.getRow(0).getCell(2).getStringCellValue());
        assertEquals("失败原因", sheet.getRow(0).getCell(3).getStringCellValue());
        assertEquals("原始数据", sheet.getRow(0).getCell(4).getStringCellValue());
        assertEquals("记录时间", sheet.getRow(0).getCell(5).getStringCellValue());
    }

    private void assertTemplateFailureHeaders(Sheet sheet) {
        assertEquals("客户名称", sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("备注", sheet.getRow(0).getCell(1).getStringCellValue());
        assertEquals("配件编码", sheet.getRow(0).getCell(2).getStringCellValue());
        assertEquals("配件名称", sheet.getRow(0).getCell(3).getStringCellValue());
        assertEquals("所属仓库", sheet.getRow(0).getCell(4).getStringCellValue());
        assertEquals("数量", sheet.getRow(0).getCell(5).getStringCellValue());
        assertEquals("单价", sheet.getRow(0).getCell(6).getStringCellValue());
        assertEquals("赠品", sheet.getRow(0).getCell(7).getStringCellValue());
        assertEquals("明细备注", sheet.getRow(0).getCell(8).getStringCellValue());
        assertEquals("错误信息", sheet.getRow(0).getCell(9).getStringCellValue());
    }

    private void assertCell(Sheet sheet, int rowIndex, int columnIndex, String value) {
        assertEquals(value, new DataFormatter().formatCellValue(sheet.getRow(rowIndex).getCell(columnIndex)));
    }

    private Sheet firstSheet(MockHttpServletResponse response) throws Exception {
        return workbook(response).getSheetAt(0);
    }

    private Workbook workbook(MockHttpServletResponse response) throws Exception {
        return WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()));
    }
}
