package cn.iocoder.yudao.module.erp.service.stock.report;

import cn.idev.excel.converters.Converter;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import cn.idev.excel.enums.CellDataTypeEnum;
import java.math.BigDecimal;

/** Excel 数值单元格无法保留核算字段24位精度，报表按原始十进制文本输出。 */
public class ErpReportDecimalTextConverter implements Converter<BigDecimal> {
    @Override public Class<?> supportJavaTypeKey() { return BigDecimal.class; }
    @Override public CellDataTypeEnum supportExcelTypeKey() { return CellDataTypeEnum.STRING; }
    @Override public WriteCellData<String> convertToExcelData(BigDecimal value,ExcelContentProperty property,
            GlobalConfiguration configuration) { return new WriteCellData<>(value==null?"":value.toPlainString()); }
}
