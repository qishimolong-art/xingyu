package cn.iocoder.yudao.module.erp.framework.excel.core;

import cn.iocoder.yudao.framework.excel.core.function.ExcelColumnSelectFunction;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockCheckTypeEnum;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * ERP 库存盘点类型下拉框数据源。
 */
@Service
public class ErpStockCheckTypeExcelColumnSelectFunction implements ExcelColumnSelectFunction {

    public static final String NAME = "getErpStockCheckTypeList";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<String> getOptions() {
        return Arrays.asList(ErpStockCheckTypeEnum.COUNT.getName(), ErpStockCheckTypeEnum.COST.getName());
    }

}
