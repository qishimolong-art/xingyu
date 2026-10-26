package cn.iocoder.yudao.module.erp.service.sale;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import cn.iocoder.yudao.module.erp.framework.excel.ErpTradeAmountExcelConverter;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ErpTradeDecimalExcelTest {
    public static class Row {
        @ExcelProperty(value = "金额", converter = ErpTradeAmountExcelConverter.class)
        private String amount;
        @ExcelProperty("数量")
        @NumberFormat("0.######")
        private BigDecimal count;
        public Row(String amount, BigDecimal count) { this.amount = amount; this.count = count; }
        public String getAmount() { return amount; }
        public BigDecimal getCount() { return count; }
    }

    @Test
    void actualWorkbookPreservesNumericCellsSixDecimalsAndPermissionMask() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, Row.class).sheet("精度验证").doWrite(Arrays.asList(
                new Row("0.123456", new BigDecimal("0.5")), new Row("****", BigDecimal.ONE)));
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(out.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            var amount = sheet.getRow(1).getCell(0);
            assertEquals(CellType.NUMERIC, amount.getCellType());
            assertEquals(0.123456, amount.getNumericCellValue(), 0.000000000001);
            assertEquals("0.00####", amount.getCellStyle().getDataFormatString());
            var count = sheet.getRow(1).getCell(1);
            assertEquals(CellType.NUMERIC, count.getCellType());
            assertEquals(0.5, count.getNumericCellValue(), 0.000000000001);
            assertEquals("0.######", count.getCellStyle().getDataFormatString());
            assertEquals("****", sheet.getRow(2).getCell(0).getStringCellValue());
        }
    }
}
