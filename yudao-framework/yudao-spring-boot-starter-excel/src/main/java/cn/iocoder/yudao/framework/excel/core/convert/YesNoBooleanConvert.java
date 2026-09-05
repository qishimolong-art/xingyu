package cn.iocoder.yudao.framework.excel.core.convert;

import cn.hutool.core.util.StrUtil;
import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;

/**
 * 中文“是/否”布尔转换器。
 */
public class YesNoBooleanConvert implements Converter<Boolean> {

    @Override
    public Class<?> supportJavaTypeKey() {
        return Boolean.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public Boolean convertToJavaData(ReadCellData readCellData, ExcelContentProperty contentProperty,
                                     GlobalConfiguration globalConfiguration) {
        if (readCellData == null) {
            return null;
        }
        if (readCellData.getBooleanValue() != null) {
            return readCellData.getBooleanValue();
        }
        String value = normalize(readCellData.getStringValue());
        if (StrUtil.isBlank(value) && readCellData.getNumberValue() != null) {
            value = readCellData.getNumberValue().stripTrailingZeros().toPlainString();
        }
        if (StrUtil.isBlank(value)) {
            return null;
        }
        if (StrUtil.equalsAny(value, "是", "赠品", "1", "true", "yes", "y")) {
            return Boolean.TRUE;
        }
        if (StrUtil.equalsAny(value, "否", "0", "false", "no", "n")) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("只能填写是/否");
    }

    @Override
    public WriteCellData<String> convertToExcelData(Boolean value, ExcelContentProperty contentProperty,
                                                    GlobalConfiguration globalConfiguration) {
        if (value == null) {
            return new WriteCellData<>("");
        }
        return new WriteCellData<>(Boolean.TRUE.equals(value) ? "是" : "否");
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

}
