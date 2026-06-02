package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUniversalMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class ErpProductServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpProductServiceImpl productService;

    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpProductUniversalMapper productUniversalMapper;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private ConfigApi configApi;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productService, "productMapper", productMapper);
        ReflectionTestUtils.setField(productService, "noRedisDAO", noRedisDAO);
    }

    @Test
    void isLowStockWarning_whenStockIsZero_thenWarns() {
        assertTrue(ErpProductServiceImpl.isLowStockWarning(BigDecimal.ZERO, null));
    }

    @Test
    void isLowStockWarning_whenStockIsPositiveAndBelowMin_thenWarns() {
        assertTrue(ErpProductServiceImpl.isLowStockWarning(BigDecimal.ONE, 5));
    }

    @Test
    void isLowStockWarning_whenStockIsPositiveAndMinMissing_thenDoesNotWarn() {
        assertFalse(ErpProductServiceImpl.isLowStockWarning(BigDecimal.ONE, null));
    }

    @Test
    void isLowStockWarning_whenStockIsAboveMin_thenDoesNotWarn() {
        assertFalse(ErpProductServiceImpl.isLowStockWarning(BigDecimal.valueOf(13), 10));
    }

    @Test
    void generateProductCode_whenRedisUnavailable_thenFallbackToDatabase() {
        when(productMapper.selectCodesByPrefix("P")).thenReturn(Arrays.asList("P000123", "PABC"));
        doThrow(new RuntimeException("redis down")).when(noRedisDAO).generatePlain("P");

        String code = ReflectionTestUtils.invokeMethod(productService, "generateProductCode", 0);

        assertEquals("P000124", code);
    }

}
