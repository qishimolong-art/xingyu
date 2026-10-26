package cn.iocoder.yudao.module.erp.framework.excel;

import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.DataFormatData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;

import java.math.BigDecimal;

/** 采购／销售报表中的受权限遮罩金额：有效金额输出数值单元格，遮罩仍为文本。 */
public class ErpTradeAmountExcelConverter implements Converter<String> {
    @Override
    public Class<?> supportJavaTypeKey() { return String.class; }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() { return CellDataTypeEnum.STRING; }

    @Override
    public WriteCellData<?> convertToExcelData(String value, ExcelContentProperty property, GlobalConfiguration config) {
        if (value == null || value.isEmpty() || "****".equals(value) || "-".equals(value)) {
            return new WriteCellData<String>(value == null ? "" : value);
        }
        WriteCellData<BigDecimal> cell = new WriteCellData<>(new BigDecimal(value));
        DataFormatData format = new DataFormatData();
        format.setFormat("0.00####");
        cell.getOrCreateStyle().setDataFormatData(format);
        return cell;
    }
}
