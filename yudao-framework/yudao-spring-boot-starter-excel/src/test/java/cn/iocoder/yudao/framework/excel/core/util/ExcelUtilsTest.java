package cn.iocoder.yudao.framework.excel.core.util;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExcelUtilsTest {

    @Test
    public void testWrite_whenExcelBuildFails_thenResponseStreamNotTouched() {
        TrackingMockHttpServletResponse response = new TrackingMockHttpServletResponse();

        assertThrows(RuntimeException.class, () -> ExcelUtils.write(response, "test.xlsx", "data",
                BrokenExportVO.class, Collections.singletonList(new BrokenExportVO("broken"))));

        assertFalse(response.isOutputStreamAccessed());
        assertNull(response.getHeader("Content-Disposition"));
    }

    private static class BrokenExportVO {

        @ExcelProperty(value = "名称", converter = BrokenConverter.class)
        private final String name;

        private BrokenExportVO(String name) {
            this.name = name;
        }

        @SuppressWarnings("unused")
        public String getName() {
            return name;
        }

    }

    public static class BrokenConverter implements Converter<Object> {

        @Override
        public Class<?> supportJavaTypeKey() {
            return Object.class;
        }

        @Override
        public CellDataTypeEnum supportExcelTypeKey() {
            return CellDataTypeEnum.STRING;
        }

        @Override
        public Object convertToJavaData(ReadCellData readCellData, ExcelContentProperty contentProperty,
                                        GlobalConfiguration globalConfiguration) {
            return null;
        }

        @Override
        public WriteCellData<?> convertToExcelData(Object object, ExcelContentProperty contentProperty,
                                                   GlobalConfiguration globalConfiguration) {
            throw new RuntimeException("mock excel convert failure");
        }

    }

    private static class TrackingMockHttpServletResponse extends MockHttpServletResponse {

        private boolean outputStreamAccessed;

        @Override
        public javax.servlet.ServletOutputStream getOutputStream() {
            outputStreamAccessed = true;
            return super.getOutputStream();
        }

        private boolean isOutputStreamAccessed() {
            return outputStreamAccessed;
        }

    }

}
