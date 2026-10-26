package cn.iocoder.yudao.module.erp.framework.excel;

import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.DataFormatData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import java.math.BigDecimal;

/** Amount display: at least two places without hiding saved precision. */
public class FinanceDecimalExcelConverter extends StockDecimalExcelConverter {
    @Override
    public WriteCellData<?> convertToExcelData(BigDecimal value, ExcelContentProperty property,
                                               GlobalConfiguration configuration) {
        WriteCellData<?> cell = super.convertToExcelData(value, property, configuration);
        if (value != null && cell.getType() == CellDataTypeEnum.NUMBER) {
            DataFormatData format = new DataFormatData();
            format.setFormat("0.00##################");
            cell.getOrCreateStyle().setDataFormatData(format);
        }
        if (value != null && cell.getType() == CellDataTypeEnum.STRING) {
            BigDecimal normalized = value.stripTrailingZeros();
            cell.setStringValue(normalized.setScale(Math.max(2, normalized.scale())).toPlainString());
        }
        return cell;
    }
}
