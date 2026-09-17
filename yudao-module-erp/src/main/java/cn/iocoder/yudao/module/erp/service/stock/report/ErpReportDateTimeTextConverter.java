package cn.iocoder.yudao.module.erp.service.stock.report;

import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 新库存实际起点使用微秒；局部文本导出避免默认日期转换器截去小数秒。 */
public class ErpReportDateTimeTextConverter implements Converter<LocalDateTime> {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    @Override
    public Class<?> supportJavaTypeKey() {
        return LocalDateTime.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public WriteCellData<String> convertToExcelData(LocalDateTime value, ExcelContentProperty property,
                                                   GlobalConfiguration configuration) {
        return new WriteCellData<>(value == null ? "" : FORMAT.format(value));
    }
}
