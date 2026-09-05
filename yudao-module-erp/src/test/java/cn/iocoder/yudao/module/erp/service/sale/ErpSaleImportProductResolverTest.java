package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.module.erp.service.common.ErpImportProductResolver;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

public class ErpSaleImportProductResolverTest extends BaseMockitoUnitTest {

    @Mock
    private ErpProductMapper productMapper;

    @Test
    public void resolve_onlyProductCode_success() {
        ImportRow row = new ImportRow("P0001", null, null);
        ErpProductDO product = new ErpProductDO().setId(1L).setCode("P0001").setName("机油滤芯");
        when(productMapper.selectListByCodes(anyCollection())).thenReturn(Collections.singletonList(product));

        ErpImportProductResolver resolver = build(row);
        ErpImportProductResolver.ResolveResult result = resolver.resolve(row.productCode, row.productName, row.factoryCode);

        assertFalse(result.isFailure());
        assertEquals(1L, result.getProduct().getId());
    }

    @Test
    public void resolve_onlyProductName_success() {
        ImportRow row = new ImportRow(null, "机油滤芯", null);
        ErpProductDO product = new ErpProductDO().setId(1L).setCode("P0001").setName("机油滤芯");
        when(productMapper.selectListByNames(anyCollection())).thenReturn(Collections.singletonList(product));

        ErpImportProductResolver resolver = build(row);
        ErpImportProductResolver.ResolveResult result = resolver.resolve(row.productCode, row.productName, row.factoryCode);

        assertFalse(result.isFailure());
        assertEquals("P0001", result.getProduct().getCode());
    }

    @Test
    public void resolve_onlyFactoryCode_success() {
        ImportRow row = new ImportRow(null, null, "F0001");
        ErpProductDO product = new ErpProductDO().setId(1L).setCode("P0001").setName("机油滤芯").setFactoryCode("F0001");
        when(productMapper.selectListByFactoryCodes(anyCollection())).thenReturn(Collections.singletonList(product));

        ErpImportProductResolver resolver = build(row);
        ErpImportProductResolver.ResolveResult result = resolver.resolve(row.productCode, row.productName, row.factoryCode);

        assertFalse(result.isFailure());
        assertEquals("P0001", result.getProduct().getCode());
    }

    @Test
    public void resolve_missingProductIdentity_failure() {
        ImportRow row = new ImportRow(null, null, null);

        ErpImportProductResolver resolver = build(row);
        ErpImportProductResolver.ResolveResult result = resolver.resolve(row.productCode, row.productName, row.factoryCode);

        assertTrue(result.isFailure());
        assertEquals("配件编码、配件名称和厂家编码为三选一字段，请至少填写其中一个", result.getErrorMessage());
    }

    @Test
    public void resolve_duplicateProductName_failure() {
        ImportRow row = new ImportRow(null, "机油滤芯", null);
        when(productMapper.selectListByNames(anyCollection())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(1L).setCode("P0001").setName("机油滤芯"),
                new ErpProductDO().setId(2L).setCode("P0002").setName("机油滤芯")));

        ErpImportProductResolver resolver = build(row);
        ErpImportProductResolver.ResolveResult result = resolver.resolve(row.productCode, row.productName, row.factoryCode);

        assertTrue(result.isFailure());
        assertEquals("配件名称存在重复，请填写配件编码：机油滤芯", result.getErrorMessage());
    }

    @Test
    public void resolve_duplicateFactoryCode_failure() {
        ImportRow row = new ImportRow(null, null, "F0001");
        when(productMapper.selectListByFactoryCodes(anyCollection())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(1L).setCode("P0001").setName("机油滤芯").setFactoryCode("F0001"),
                new ErpProductDO().setId(2L).setCode("P0002").setName("空气滤芯").setFactoryCode("F0001")));

        ErpImportProductResolver resolver = build(row);
        ErpImportProductResolver.ResolveResult result = resolver.resolve(row.productCode, row.productName, row.factoryCode);

        assertTrue(result.isFailure());
        assertEquals("厂家编码存在重复，请填写配件编码：F0001", result.getErrorMessage());
    }

    @Test
    public void resolve_duplicateProductNameWithProductCode_success() {
        ImportRow row = new ImportRow("P0001", "机油滤芯", null);
        ErpProductDO product = new ErpProductDO().setId(1L).setCode("P0001").setName("机油滤芯").setFactoryCode("F0001");
        when(productMapper.selectListByCodes(anyCollection())).thenReturn(Collections.singletonList(product));
        when(productMapper.selectListByNames(anyCollection())).thenReturn(Arrays.asList(
                product,
                new ErpProductDO().setId(2L).setCode("P0002").setName("机油滤芯")));

        ErpImportProductResolver resolver = build(row);
        ErpImportProductResolver.ResolveResult result = resolver.resolve(row.productCode, row.productName, row.factoryCode);

        assertFalse(result.isFailure());
        assertEquals(1L, result.getProduct().getId());
    }

    @Test
    public void resolve_productCodeAndNameMismatch_failure() {
        ImportRow row = new ImportRow("P0001", "空气滤芯", null);
        when(productMapper.selectListByCodes(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(1L).setCode("P0001").setName("机油滤芯")));
        when(productMapper.selectListByNames(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(2L).setCode("P0002").setName("空气滤芯")));

        ErpImportProductResolver resolver = build(row);
        ErpImportProductResolver.ResolveResult result = resolver.resolve(row.productCode, row.productName, row.factoryCode);

        assertTrue(result.isFailure());
        assertEquals("配件编码、配件名称和厂家编码不一致", result.getErrorMessage());
    }

    @Test
    public void resolve_productNameAndFactoryCodeMatchSameProduct_success() {
        ImportRow row = new ImportRow(null, "机油滤芯", "F0001");
        ErpProductDO product = new ErpProductDO().setId(1L).setCode("P0001").setName("机油滤芯").setFactoryCode("F0001");
        when(productMapper.selectListByNames(anyCollection())).thenReturn(Collections.singletonList(product));
        when(productMapper.selectListByFactoryCodes(anyCollection())).thenReturn(Collections.singletonList(product));

        ErpImportProductResolver resolver = build(row);
        ErpImportProductResolver.ResolveResult result = resolver.resolve(row.productCode, row.productName, row.factoryCode);

        assertFalse(result.isFailure());
        assertEquals(1L, result.getProduct().getId());
    }

    @Test
    public void resolve_productCodeAndFactoryCodeMismatch_failure() {
        ImportRow row = new ImportRow("P0001", null, "F0002");
        when(productMapper.selectListByCodes(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(1L).setCode("P0001").setName("机油滤芯").setFactoryCode("F0001")));
        when(productMapper.selectListByFactoryCodes(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(2L).setCode("P0002").setName("空气滤芯").setFactoryCode("F0002")));

        ErpImportProductResolver resolver = build(row);
        ErpImportProductResolver.ResolveResult result = resolver.resolve(row.productCode, row.productName, row.factoryCode);

        assertTrue(result.isFailure());
        assertEquals("配件编码、配件名称和厂家编码不一致", result.getErrorMessage());
    }

    private ErpImportProductResolver build(ImportRow... rows) {
        List<ImportRow> list = Arrays.asList(rows);
        return ErpImportProductResolver.build(list, row -> row.productCode, row -> row.productName,
                row -> row.factoryCode, productMapper);
    }

    private static class ImportRow {
        private final String productCode;
        private final String productName;
        private final String factoryCode;

        private ImportRow(String productCode, String productName, String factoryCode) {
            this.productCode = productCode;
            this.productName = productName;
            this.factoryCode = factoryCode;
        }
    }

}
