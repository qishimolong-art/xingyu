package cn.iocoder.yudao.module.erp.controller.admin.base;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.excel.core.function.ExcelColumnSelectFunction;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportExcelVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ErpBaseDataImportTemplateTest {

    private static final Set<String> SYSTEM_TIME_HEADERS = new LinkedHashSet<>(Arrays.asList(
            "创建时间", "更新时间", "修改时间", "创建日期", "更新日期", "业务日期", "业务时间", "单据日期"));

    @Test
    void productTemplate_requiredHeadersAreYellowWithoutPrefixAndNoSystemTime() throws Exception {
        Template template = writeTemplate(ErpProductImportExcelVO.class);

        assertThat(template.headers).contains("配件编码", "产品名称", "配件分类编码", "单位", "产品条码");
        assertThat(template.headers).doesNotContain("* 配件编码", "* 产品名称", "* 配件分类编码", "* 单位");
        assertThat(template.headers).doesNotContainAnyElementsOf(SYSTEM_TIME_HEADERS);
        assertRequired(template, "配件编码");
        assertRequired(template, "产品名称");
        assertRequired(template, "配件分类编码");
        assertRequired(template, "单位");
        assertOptional(template, "产品条码");
    }

    @Test
    void productTemplate_dynamicRequiredHeaderIsYellowWithoutPrefix() throws Exception {
        Template template = writeTemplate(ErpProductImportExcelVO.class, Collections.singleton("defaultWarehouseName"));

        assertThat(template.headers).contains("默认仓库");
        assertThat(template.headers).doesNotContain("* 默认仓库");
        assertRequired(template, "默认仓库");
        assertOptional(template, "产品条码");
    }

    @Test
    void productCategoryTemplate_requiredHeadersAreYellowWithoutPrefixAndNoSystemTime() throws Exception {
        Template template = writeTemplate(ErpProductCategoryImportExcelVO.class);

        assertThat(template.headers).contains("分类名称", "分类编码", "上级分类编码");
        assertThat(template.headers).doesNotContain("* 分类名称", "* 分类编码");
        assertThat(template.headers).doesNotContainAnyElementsOf(SYSTEM_TIME_HEADERS);
        assertRequired(template, "分类名称");
        assertRequired(template, "分类编码");
        assertOptional(template, "上级分类编码");
    }

    @Test
    void productUnitTemplate_requiredHeadersAreYellowWithoutPrefixAndNoSystemTime() throws Exception {
        Template template = writeTemplate(ErpProductUnitImportExcelVO.class);

        assertThat(template.headers).contains("单位名称", "状态");
        assertThat(template.headers).doesNotContain("* 单位名称");
        assertThat(template.headers).doesNotContainAnyElementsOf(SYSTEM_TIME_HEADERS);
        assertRequired(template, "单位名称");
        assertOptional(template, "状态");
    }

    @Test
    void warehouseTemplate_requiredHeadersAreYellowWithoutPrefixAndNoSystemTime() throws Exception {
        Template template = writeTemplate(ErpWarehouseImportExcelVO.class);

        assertThat(template.headers).contains("仓库名称", "仓库编码", "所属部门");
        assertThat(template.headers).doesNotContain("* 仓库名称");
        assertThat(template.headers).doesNotContainAnyElementsOf(SYSTEM_TIME_HEADERS);
        assertRequired(template, "仓库名称");
        assertOptional(template, "仓库编码");
    }

    @Test
    void supplierTemplate_requiredHeadersAreYellowWithoutPrefixAndDynamicRequiredWorks() throws Exception {
        Template template = writeTemplate(ErpSupplierImportExcelVO.class, Collections.singleton("code"));

        assertThat(template.headers).contains("供应商编码", "供应商名称", "联系人");
        assertThat(template.headers).doesNotContain("* 供应商编码", "* 供应商名称");
        assertThat(template.headers).doesNotContainAnyElementsOf(SYSTEM_TIME_HEADERS);
        assertRequired(template, "供应商编码");
        assertOptional(template, "联系人");
    }

    @Test
    void customerTemplate_requiredHeadersAreYellowWithoutPrefixAndDynamicRequiredWorks() throws Exception {
        Template template = writeTemplate(ErpCustomerImportExcelVO.class, Collections.singleton("mobile"));

        assertThat(template.headers).contains("手机号", "客户名称", "客户编码");
        assertThat(template.headers).doesNotContain("* 手机号", "* 客户名称");
        assertThat(template.headers).doesNotContainAnyElementsOf(SYSTEM_TIME_HEADERS);
        assertRequired(template, "手机号");
        assertOptional(template, "客户编码");
    }

    @Test
    void accountTemplate_requiredHeadersAreYellowWithoutPrefixAndNoSystemTime() throws Exception {
        Template template = writeTemplate(ErpAccountImportExcelVO.class);

        assertThat(template.headers).contains("账户名称", "账户类型", "开户行");
        assertThat(template.headers).doesNotContain("* 账户名称", "* 账户类型");
        assertThat(template.headers).doesNotContainAnyElementsOf(SYSTEM_TIME_HEADERS);
        assertRequired(template, "账户名称");
        assertRequired(template, "账户类型");
        assertOptional(template, "开户行");
    }

    private <T> Template writeTemplate(Class<T> head) throws Exception {
        return writeTemplate(head, null);
    }

    private <T> Template writeTemplate(Class<T> head, Set<String> requiredFields) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            ApplicationContext applicationContext = mock(ApplicationContext.class);
            springUtil.when(SpringUtil::getApplicationContext).thenReturn(applicationContext);
            when(applicationContext.getBeansOfType(ExcelColumnSelectFunction.class))
                    .thenReturn(buildSelectFunctionMap());
            ExcelUtils.writeImportTemplate(response, "template.xls", "data", head,
                    Collections.singletonList(head.getDeclaredConstructor().newInstance()), null, requiredFields);
        }
        return readTemplate(response.getContentAsByteArray());
    }

    private Map<String, ExcelColumnSelectFunction> buildSelectFunctionMap() {
        Map<String, ExcelColumnSelectFunction> result = new LinkedHashMap<>();
        result.put("category", selectFunction("getErpProductCategoryCodeList", "001", "001 001"));
        result.put("unit", selectFunction("getErpProductUnitNameList", "个", "箱"));
        return result;
    }

    private ExcelColumnSelectFunction selectFunction(String name, String... options) {
        return new ExcelColumnSelectFunction() {

            @Override
            public String getName() {
                return name;
            }

            @Override
            public java.util.List<String> getOptions() {
                return Arrays.asList(options);
            }

        };
    }

    private Template readTemplate(byte[] content) throws Exception {
        Template template = new Template();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Row headerRow = workbook.getSheetAt(0).getRow(0);
            for (Cell cell : headerRow) {
                String header = cell.getStringCellValue();
                template.headers.add(header);
                if (isYellowRequiredHeader(cell)) {
                    template.requiredHeaders.add(header);
                }
            }
        }
        return template;
    }

    private boolean isYellowRequiredHeader(Cell cell) {
        return cell.getCellStyle().getFillPattern() == FillPatternType.SOLID_FOREGROUND
                && cell.getCellStyle().getFillForegroundColor() == IndexedColors.YELLOW.getIndex()
                && cell.getCellComment() != null;
    }

    private void assertRequired(Template template, String header) {
        assertThat(template.requiredHeaders.contains(header)).as(header).isTrue();
    }

    private void assertOptional(Template template, String header) {
        assertThat(template.headers).contains(header);
        assertThat(template.requiredHeaders.contains(header)).as(header).isFalse();
    }

    private static class Template {

        private final Set<String> headers = new LinkedHashSet<>();
        private final Set<String> requiredHeaders = new LinkedHashSet<>();

    }

}
