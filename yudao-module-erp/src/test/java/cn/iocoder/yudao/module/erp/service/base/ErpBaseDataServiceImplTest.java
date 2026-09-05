package cn.iocoder.yudao.module.erp.service.base;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.dal.mysql.base.ErpBaseDataMapper;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ErpBaseDataServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpBaseDataServiceImpl service;

    @Mock
    private ErpBaseDataMapper baseDataMapper;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @Test
    void getBaseDataSimpleListByType_deduplicatesByTypeAndNameKeepingFirst() {
        ErpBaseDataDO firstCredit = ErpBaseDataDO.builder()
                .id(1L).type("settle_method").name("挂账").sort(10).build();
        ErpBaseDataDO duplicateCredit = ErpBaseDataDO.builder()
                .id(2L).type("settle_method").name("挂账").sort(10).build();
        ErpBaseDataDO cash = ErpBaseDataDO.builder()
                .id(3L).type("settle_method").name("现金").sort(40).build();
        when(baseDataMapper.selectListByTypeAndStatus("settle_method", CommonStatusEnum.ENABLE.getStatus()))
                .thenReturn(Arrays.asList(firstCredit, duplicateCredit, cash));

        List<ErpBaseDataDO> result = service.getBaseDataSimpleListByType("settle_method");

        assertThat(result).containsExactly(firstCredit, cash);
    }

}
