package cn.iocoder.yudao.module.erp.controller.admin.product;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.*;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class ErpProductExportSupportTest {
    @Test void rejectsUnknownAndPermissionHiddenFields() {
        List<ErpExportFieldRespVO> allowed = ErpProductExportSupport.fields(new HashSet<>(Arrays.asList("purchasePrice","col_code","createDeptId","deptId")),Collections.emptyList());
        assertThrows(IllegalArgumentException.class,()->ErpProductExportSupport.select(allowed,"purchasePrice",Collections.emptySet()));
        assertThrows(IllegalArgumentException.class,()->ErpProductExportSupport.select(allowed,"productCode",Collections.emptySet()));
        assertThrows(IllegalArgumentException.class,()->ErpProductExportSupport.select(allowed,"madeUpField",Collections.emptySet()));
        assertThrows(IllegalArgumentException.class,()->ErpProductExportSupport.select(allowed,"createDeptName",Collections.emptySet()));
        assertThrows(IllegalArgumentException.class,()->ErpProductExportSupport.select(allowed,"deptName",Collections.emptySet()));
    }
    @Test void preservesDefaultFieldSetAndCustomFieldKeys() {
        ErpFieldConfigDO custom = ErpFieldConfigDO.builder().fieldName("orderPrice").fieldLabel("订货价").fieldSource("CUSTOM").fieldGroup("price_info").visible(true).build();
        List<ErpExportFieldRespVO> allowed = ErpProductExportSupport.fields(Collections.emptySet(), Collections.singletonList(custom));
        assertEquals("orderPrice",ErpProductExportSupport.select(allowed,"orderPrice",Collections.emptySet()).get(0).getField());
        assertEquals(2, ErpProductExportSupport.select(allowed,null,new LinkedHashSet<>(Arrays.asList("code","name"))).size());
    }
    @Test void writes501RowsInBoundedBatchesWithCorrectCellTypes() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse(); AtomicInteger calls = new AtomicInteger();
        List<ErpExportFieldRespVO> fields = ErpProductExportSupport.select(ErpProductExportSupport.fields(Collections.emptySet(),Collections.emptyList()),"code,salePrice,packageQty",Collections.emptySet());
        ErpProductExportSupport.write(response,fields,Collections.emptyList(),page->{
            calls.incrementAndGet(); List<ErpProductRespVO> rows = new ArrayList<>();
            for(int i=0;i<(page==1?500:1);i++) { ErpProductRespVO row = new ErpProductRespVO(); row.setCode("000123");row.setSalePrice(new BigDecimal("12.5"));row.setPackageQty(2);rows.add(row); } return rows;
        });
        assertEquals(2,calls.get()); assertTrue(response.getContentType().contains("spreadsheetml"));
        try (Workbook workbook=WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet=workbook.getSheetAt(0);assertEquals(501,sheet.getLastRowNum());
            assertEquals(CellType.STRING,sheet.getRow(1).getCell(0).getCellType()); assertEquals("000123",sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals(CellType.NUMERIC,sheet.getRow(1).getCell(1).getCellType());assertEquals("0.00",sheet.getRow(1).getCell(1).getCellStyle().getDataFormatString());
            assertEquals(CellType.NUMERIC,sheet.getRow(1).getCell(2).getCellType());
        }
    }
}
