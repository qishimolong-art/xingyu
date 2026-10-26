package cn.iocoder.yudao.module.erp.framework.excel;

import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.DataFormatData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;

import java.math.BigDecimal;

/** Excel has fifteen significant digits; larger decimals are preserved as text. */
public class StockDecimalExcelConverter implements Converter<BigDecimal> {

    @Override
    public Class<?> supportJavaTypeKey() {
        return BigDecimal.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.NUMBER;
    }

    @Override
    public WriteCellData<?> convertToExcelData(BigDecimal value, ExcelContentProperty property,
                                              GlobalConfiguration configuration) {
        if (value == null) {
            return new WriteCellData<String>("");
        }
        BigDecimal normalized = value.stripTrailingZeros();
        boolean text = normalized.precision() > 15;
        WriteCellData<?> cell = text ? new WriteCellData<String>(normalized.toPlainString())
                : new WriteCellData<BigDecimal>(value);
        DataFormatData format = new DataFormatData();
        format.setFormat(text ? "@" : "0.####################");
        cell.getOrCreateStyle().setDataFormatData(format);
        return cell;
    }
}
