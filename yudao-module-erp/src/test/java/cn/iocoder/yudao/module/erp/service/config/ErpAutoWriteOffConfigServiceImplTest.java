package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpAutoWriteOffConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpAutoWriteOffConfigUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpAutoWriteOffDeptConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.config.ErpAutoWriteOffDeptConfigMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpAutoWriteOffConfigServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpAutoWriteOffConfigServiceImpl service;

    @Mock
    private ErpAutoWriteOffDeptConfigMapper autoWriteOffDeptConfigMapper;
    @Mock
    private DeptApi deptApi;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getConfig_expandsConfiguredDepartmentsWithChildren() {
        when(autoWriteOffDeptConfigMapper.selectActiveList())
                .thenReturn(Collections.singletonList(config(10L)));
        when(deptApi.getChildDeptList(10L)).thenReturn(Arrays.asList(dept(12L), dept(11L)));

        ErpAutoWriteOffConfigRespVO result = service.getConfig();

        assertThat(result.getDisabledDeptIds()).containsExactly(10L);
        assertThat(result.getEffectiveDisabledDeptIds()).containsExactly(10L, 11L, 12L);
    }

    @Test
    void isAutoWriteOffDisabled_nullDeptReturnsTrue() {
        assertThat(service.isAutoWriteOffDisabled(null)).isTrue();
    }

    @Test
    void isAutoWriteOffDisabled_matchesConfiguredChildDepartment() {
        when(autoWriteOffDeptConfigMapper.selectActiveList())
                .thenReturn(Collections.singletonList(config(10L)));
        when(deptApi.getChildDeptList(10L)).thenReturn(Collections.singletonList(dept(11L)));

        assertThat(service.isAutoWriteOffDisabled(11L)).isTrue();
        assertThat(service.isAutoWriteOffDisabled(12L)).isFalse();
    }

    @Test
    void updateConfig_validatesAndReplacesDistinctDepartmentIds() {
        TenantContextHolder.setTenantId(7L);
        ErpAutoWriteOffConfigUpdateReqVO reqVO = new ErpAutoWriteOffConfigUpdateReqVO();
        reqVO.setDisabledDeptIds(Arrays.asList(12L, 10L, 12L, null));

        service.updateConfig(reqVO);

        verify(deptApi).validateDeptList(eq(Arrays.asList(10L, 12L)));
        verify(autoWriteOffDeptConfigMapper).softDeleteAll("0", 7L);
        verify(autoWriteOffDeptConfigMapper).restoreOrInsert(10L, "0", 7L);
        verify(autoWriteOffDeptConfigMapper).restoreOrInsert(12L, "0", 7L);
    }

    private ErpAutoWriteOffDeptConfigDO config(Long deptId) {
        return new ErpAutoWriteOffDeptConfigDO().setDeptId(deptId);
    }

    private DeptRespDTO dept(Long id) {
        return new DeptRespDTO().setId(id);
    }

}
