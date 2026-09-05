package cn.iocoder.yudao.module.erp.framework.excel.core;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.excel.core.function.ExcelColumnSelectFunction;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryListReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductCategoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 配件分类编码下拉框数据源。
 */
@Service
public class ErpProductCategoryCodeExcelColumnSelectFunction implements ExcelColumnSelectFunction {

    public static final String NAME = "getErpProductCategoryCodeList";

    @Resource
    private ErpProductCategoryService productCategoryService;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<String> getOptions() {
        return productCategoryService.getProductCategoryList(new ErpProductCategoryListReqVO()
                        .setStatus(CommonStatusEnum.ENABLE.getStatus())).stream()
                .map(ErpProductCategoryDO::getCode)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

}
