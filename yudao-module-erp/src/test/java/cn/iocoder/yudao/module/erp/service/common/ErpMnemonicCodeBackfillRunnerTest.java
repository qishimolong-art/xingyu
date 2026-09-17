package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpMnemonicCodeBackfillRunnerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpMnemonicCodeBackfillRunner runner;

    @Mock
    private ErpCustomerMapper customerMapper;
    @Mock
    private ErpSupplierMapper supplierMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ApplicationArguments applicationArguments;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initTableInfo(configuration, ErpCustomerMapper.class, ErpCustomerDO.class);
        initTableInfo(configuration, ErpSupplierMapper.class, ErpSupplierDO.class);
        initTableInfo(configuration, ErpProductMapper.class, ErpProductDO.class);
    }

    private static void initTableInfo(MybatisConfiguration configuration, Class<?> mapperClass, Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(mapperClass.getName());
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void run_backfillsCustomerMnemonicCodesOnlyWhenBlank() throws Exception {
        ErpCustomerDO blankCustomer = new ErpCustomerDO()
                .setId(10L)
                .setName("测试客户");
        ErpCustomerDO existingCustomer = new ErpCustomerDO()
                .setId(11L)
                .setName("已有客户")
                .setPinyinCode("YYKH")
                .setWubiCode("DAN");
        when(customerMapper.selectList(any())).thenReturn(Arrays.asList(blankCustomer, existingCustomer));
        when(supplierMapper.selectList(any())).thenReturn(Collections.<ErpSupplierDO>emptyList());
        when(productMapper.selectList(any())).thenReturn(Collections.<ErpProductDO>emptyList());
        when(customerMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        runner.run(applicationArguments);

        verify(customerMapper).selectList(any());
        verify(customerMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(supplierMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(productMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void run_rebuildsOnlyLegacyProductPinyinCodeWhenEnabled() throws Exception {
        ErpProductDO legacyProduct = new ErpProductDO()
                .setId(20L)
                .setName("美孚自动变速箱油 ATF 8LV, 12X1L")
                .setPinyinCode("MZZDBSXYATF8LV12X1L")
                .setWubiCode("UETFYGTIATF8LV12X1L");
        ErpProductDO manualProduct = new ErpProductDO()
                .setId(21L)
                .setName("美孚1号经典表现欧系 0W-20 SQ 12X1L")
                .setPinyinCode("MANUAL")
                .setWubiCode("UE1KXMGGAT0W20SQ12X1L");
        ReflectionTestUtils.setField(runner, "rebuildProductPinyinCode", true);
        when(customerMapper.selectList(any())).thenReturn(Collections.<ErpCustomerDO>emptyList());
        when(supplierMapper.selectList(any())).thenReturn(Collections.<ErpSupplierDO>emptyList());
        when(productMapper.selectList(any())).thenReturn(Arrays.asList(legacyProduct, manualProduct));
        when(productMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        runner.run(applicationArguments);

        verify(productMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
    }

}
