package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantEntityResolverTest {

    @Test void normalizesOnlyBusinessWrappersAndKeepsCodePunctuation() {
        assertEquals("145988",AssistantEntityResolver.normalizeKeyword(" 产品编码为：145988 "));
        assertEquals("WSC56208/WSC20856",AssistantEntityResolver.normalizeKeyword("“厂家编码 WSC56208/WSC20856”"));
        assertEquals("卫斯卡空调滤清器",AssistantEntityResolver.normalizeKeyword("卫斯卡空调滤清器"));
        assertNull(AssistantEntityResolver.normalizeKeyword("   "));
    }

    @Test void extractsExplicitEntityTypeAndIdentifierWithoutModelGuessing() {
        Map<String,Object> product=AssistantEntityResolver.explicitReference("产品编码为145988在各仓库的库存");
        assertEquals("PRODUCT",product.get("entityType"));assertEquals("145988",product.get("keyword"));
        Map<String,Object> customer=AssistantEntityResolver.explicitReference("查询编码KH001的客户本月收款");
        assertEquals("CUSTOMER",customer.get("entityType"));assertEquals("KH001",customer.get("keyword"));
        Map<String,Object> supplier=AssistantEntityResolver.explicitReference("供应商旧编码 GYS-01 的付款情况");
        assertEquals("SUPPLIER",supplier.get("entityType"));assertEquals("GYS-01",supplier.get("keyword"));
        assertNull(AssistantEntityResolver.explicitReference("蛟龙港仓当前库存"));
    }

    @Test void resolvesDepartmentFromAuthoritativeDepartmentApi() {
        DeptApi departments=mock(DeptApi.class);
        AssistantEntityResolver resolver=resolver(departments);
        DeptRespDTO ganzi=department(10L,"甘孜分公司");
        when(departments.getDeptListByName("甘孜分公司")).thenReturn(Collections.singletonList(ganzi));

        List<Map<String,Object>> rows=resolver.resolveDepartment(
                new HashMap<>(Collections.singletonMap("entityAll",true)),"甘孜分公司",null);

        assertEquals(1,rows.size());
        assertEquals(10L,rows.get(0).get("id"));
        assertEquals("DEPARTMENT",rows.get(0).get("entityType"));
        assertEquals("NAME",rows.get(0).get("matchType"));
        verify(departments,never()).getDeptSimplePage(any(),any(),any(),any());
    }

    @Test void departmentResolutionKeepsPermissionScopeForFuzzySearch() {
        DeptApi departments=mock(DeptApi.class);
        AssistantEntityResolver resolver=resolver(departments);
        when(departments.getDeptListByName("甘孜")).thenReturn(Collections.emptyList());
        when(departments.getDeptSimplePage(eq(CommonStatusEnum.ENABLE.getStatus()),eq("甘孜"),
                eq(Set.of(10L)),any())).thenReturn(new PageResult<>(Collections.singletonList(department(10L,"甘孜分公司")),1L));
        Map<String,Object> args=new HashMap<>();
        args.put("entityAll",false);args.put("entityDeptIds",Set.of(10L));

        List<Map<String,Object>> rows=resolver.resolveDepartment(args,"甘孜",null);

        assertEquals(1,rows.size());
        assertEquals("FUZZY_NAME",rows.get(0).get("matchType"));
        verify(departments).getDeptSimplePage(eq(CommonStatusEnum.ENABLE.getStatus()),eq("甘孜"),
                eq(Set.of(10L)),argThat(page -> page.getPageNo()==1 && page.getPageSize()==21));
    }

    private static AssistantEntityResolver resolver(DeptApi departments) {
        AssistantEntityResolver resolver=new AssistantEntityResolver();
        ReflectionTestUtils.setField(resolver,"departments",departments);
        ReflectionTestUtils.setField(resolver,"properties",new AssistantProperties());
        return resolver;
    }

    private static DeptRespDTO department(Long id,String name) {
        DeptRespDTO department=new DeptRespDTO();department.setId(id);department.setName(name);
        department.setStatus(CommonStatusEnum.ENABLE.getStatus());return department;
    }
}
