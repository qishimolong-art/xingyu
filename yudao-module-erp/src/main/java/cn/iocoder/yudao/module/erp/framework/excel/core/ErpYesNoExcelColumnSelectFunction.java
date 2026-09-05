package cn.iocoder.yudao.module.erp.framework.excel.core;

import cn.iocoder.yudao.framework.excel.core.function.ExcelColumnSelectFunction;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * ERP 是/否下拉框数据源。
 */
@Service
public class ErpYesNoExcelColumnSelectFunction implements ExcelColumnSelectFunction {

    public static final String NAME = "getErpYesNoList";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<String> getOptions() {
        return Arrays.asList("是", "否");
    }

}
