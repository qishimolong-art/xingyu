package cn.iocoder.yudao.module.erp.framework.web;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.service.common.ErpFinanceAmountUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpFinanceDecimalRules;
import cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferSaveReqVO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleCalculator;
import cn.idev.excel.FastExcelFactory;
import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FinanceDecimalPrecisionTest {
    @Test void normalizationPreservesActualSixPlaceValuesAndTinyBalances() {
        assertEquals("10.123456", ErpFinanceAmountUtils.normalize(new BigDecimal("10.123456000")).toPlainString());
        assertEquals("0.000001", ErpFinanceAmountUtils.normalize(new BigDecimal("0.000002").subtract(new BigDecimal("0.000001"))).toPlainString());
        assertEquals("-10.023456", ErpFinanceAmountUtils.normalize(new BigDecimal("-10.123456").add(new BigDecimal("0.1"))).toPlainString());
    }
    @Test void manualStorageChecksRejectOverflowAndAllowMeaninglessZeros() {
        ErpFinancePaymentSaveReqVO request = new ErpFinancePaymentSaveReqVO();
        request.setTotalPrice(new BigDecimal("10.123456000"));
        assertDoesNotThrow(() -> ErpFinanceDecimalRules.validate(request));
        request.setTotalPrice(new BigDecimal("10.1234567"));
        assertThrows(ServiceException.class, () -> ErpFinanceDecimalRules.validate(request));
        request.setTotalPrice(new BigDecimal("1000000000000000000"));
        assertThrows(ServiceException.class, () -> ErpFinanceDecimalRules.validate(request));
        ErpFinanceTransferSaveReqVO transfer = new ErpFinanceTransferSaveReqVO();
        transfer.setExchangeRate(new BigDecimal("1000000000000"));
        assertThrows(ServiceException.class, () -> ErpFinanceDecimalRules.validate(transfer));
    }
    @Test void actualPaymentWorkbookKeepsMinimumTwoAndAllSixDigits() throws Exception {
        ErpFinancePaymentExportRespVO row = new ErpFinancePaymentExportRespVO();
        row.setNo("000123"); row.setTotalPrice(new BigDecimal("0.1"));
        row.setPaymentPrice(new BigDecimal("10.123456"));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        FastExcelFactory.write(bytes, row.getClass()).sheet("付款").doWrite(List.of(row));
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes.toByteArray()))) {
            boolean foundShort=false,foundLong=false;
            for (org.apache.poi.ss.usermodel.Cell cell : book.getSheetAt(0).getRow(1)) {
                if (cell.getCellType()==org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                    if (cell.getNumericCellValue()==0.1) foundShort=true;
                    if (cell.getNumericCellValue()==10.123456) foundLong=true;
                    assertEquals("0.00##################",cell.getCellStyle().getDataFormatString());
                }
            }
            assertTrue(foundShort);assertTrue(foundLong);
            assertEquals("000123",book.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
        }
    }
    @Test void excelPreservesValuesAboveFifteenSignificantDigitsAsText() {
        FinanceDecimalExcelConverter converter = new FinanceDecimalExcelConverter();
        assertEquals("999999999999999999.00",converter.convertToExcelData(new BigDecimal("999999999999999999"),null,null).getStringValue());
        assertEquals("999999999999999999.123456",converter.convertToExcelData(new BigDecimal("999999999999999999.123456"),null,null).getStringValue());
    }
    @Test void accountingRuleStillRoundsToCentsWithoutChangingSource() {
        BigDecimal source = new BigDecimal("10.123456");
        assertEquals(new BigDecimal("10.12"),ErpVoucherRuleCalculator.money(source));
        assertEquals(new BigDecimal("0.00"),ErpVoucherRuleCalculator.money(new BigDecimal("0.000001")));
        assertEquals("10.123456",source.toPlainString());
    }
}
