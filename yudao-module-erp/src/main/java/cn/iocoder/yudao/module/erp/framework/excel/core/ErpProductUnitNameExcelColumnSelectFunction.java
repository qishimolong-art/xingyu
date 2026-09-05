package cn.iocoder.yudao.module.erp.framework.excel.core;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.excel.core.function.ExcelColumnSelectFunction;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductUnitService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 产品单位名称下拉框数据源。
 */
@Service
public class ErpProductUnitNameExcelColumnSelectFunction implements ExcelColumnSelectFunction {

    public static final String NAME = "getErpProductUnitNameList";

    @Resource
    private ErpProductUnitService productUnitService;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<String> getOptions() {
        return productUnitService.getProductUnitListByStatus(CommonStatusEnum.ENABLE.getStatus()).stream()
                .map(ErpProductUnitDO::getName)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

}
