package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import cn.iocoder.yudao.module.erp.enums.config.ErpFieldConfigModuleEnum;
import cn.iocoder.yudao.module.erp.framework.excel.ErpImportTemplateRequiredFieldUtils;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErpProductImportExcelVOTest {

    @Test
    void writeImportTemplate_marksRequiredHeaders() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.writeImportTemplate(response, "产品导入模板.xls", "产品", ErpProductImportExcelVO.class,
                Collections.singletonList(new ErpProductImportExcelVO()));

        Set<String> headers = readFirstRowHeaders(response.getContentAsByteArray());
        assertTrue(headers.contains("* 配件编码"));
        assertTrue(headers.contains("* 产品名称"));
        assertTrue(headers.contains("产品条码"));
        assertFalse(headers.contains("* 产品条码"));
        assertTrue(headers.contains("* 配件分类编码"));
        assertTrue(headers.contains("* 单位"));
    }

    @Test
    void getRequiredFields_mapsConfiguredDefaultWarehouseToImportField() throws Exception {
        ErpFieldConfigService fieldConfigService = mock(ErpFieldConfigService.class);
        when(fieldConfigService.getFieldConfigListByModule("erp_product")).thenReturn(Arrays.asList(
                fieldConfig("defaultWarehouseId", true),
                fieldConfig("barCode", false)));

        Set<String> requiredFields = ErpImportTemplateRequiredFieldUtils.getRequiredFields(fieldConfigService,
                ErpFieldConfigModuleEnum.ERP_PRODUCT, ErpProductImportExcelVO.class,
                ErpImportTemplateRequiredFieldUtils.aliasMap(
                        "defaultWarehouseId", "defaultWarehouseName"));

        assertTrue(requiredFields.contains("defaultWarehouseName"));
        assertFalse(requiredFields.contains("barCode"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.writeImportTemplate(response, "产品导入模板.xls", "产品", ErpProductImportExcelVO.class,
                Collections.singletonList(new ErpProductImportExcelVO()), null, requiredFields);

        Set<String> headers = readFirstRowHeaders(response.getContentAsByteArray());
        assertTrue(headers.contains("* 默认仓库"));
        assertFalse(headers.contains("* 产品条码"));
    }

    private Set<String> readFirstRowHeaders(byte[] content) throws Exception {
        Set<String> headers = new HashSet<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }
        }
        return headers;
    }

    private ErpFieldConfigDO fieldConfig(String fieldName, boolean required) {
        return ErpFieldConfigDO.builder()
                .fieldName(fieldName)
                .required(required)
                .build();
    }

}
