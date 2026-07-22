package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.deptpricefield.DeptPriceFieldConfigRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.deptpricefield.DeptPriceFieldUpdateReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.DeptPriceFieldDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.DeptPriceFieldMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FieldDefinitionMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.common.util.collection.SetUtils.asSet;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.DEPT_PRICE_FIELD_CONFIG_CHANGED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeptPriceFieldServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DeptPriceFieldServiceImpl service;

    @Mock
    private DeptPriceFieldMapper deptPriceFieldMapper;
    @Mock
    private FieldDefinitionMapper fieldDefinitionMapper;
    @Mock
    private DeptService deptService;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void getHiddenPriceFields_shouldUseUnionAndReturnFormAndColumnKeys() {
        when(fieldDefinitionMapper.selectListByModuleAndGroup("erp_product", "price_info"))
                .thenReturn(Arrays.asList(field("referencePrice", 1), field("retailPrice", 2)));
        when(deptPriceFieldMapper.selectListByDeptIds(eq(asSet(10L, 20L))))
                .thenReturn(Collections.singletonList(relation(20L, "retailPrice")));

        List<String> hiddenFields = service.getHiddenPriceFields(asSet(10L, 20L));

        assertEquals(Arrays.asList("referencePrice", "col_referencePrice"), hiddenFields);
    }

    @Test
    void getHiddenPriceFields_withoutDepartment_shouldHideAllDynamicPrices() {
        when(fieldDefinitionMapper.selectListByModuleAndGroup("erp_product", "price_info"))
                .thenReturn(Collections.singletonList(field("customDealerPrice", 1)));

        assertEquals(Arrays.asList("customDealerPrice", "col_customDealerPrice"),
                service.getHiddenPriceFields(Collections.emptySet()));
    }

    @Test
    void updateConfig_shouldRejectStaleVersion() {
        List<FieldDefinitionDO> definitions = Collections.singletonList(field("referencePrice", 1));
        when(fieldDefinitionMapper.selectListByModuleAndGroupForUpdate(1L, "erp_product", "price_info"))
                .thenReturn(definitions);
        when(deptService.getDeptList(any(DeptListReqVO.class))).thenReturn(Collections.singletonList(dept(10L)));
        when(deptPriceFieldMapper.selectListByFieldKeys(eq(Collections.singleton("referencePrice"))))
                .thenReturn(Collections.singletonList(relation(10L, "referencePrice")));
        DeptPriceFieldUpdateReqVO reqVO = updateReq("stale", "referencePrice", Collections.singletonList(10L));

        assertServiceException(() -> service.updateConfig(reqVO), DEPT_PRICE_FIELD_CONFIG_CHANGED);
    }

    @Test
    void updateConfig_shouldApplyOnlyRelationDifference() {
        List<FieldDefinitionDO> definitions = Collections.singletonList(field("referencePrice", 1));
        List<DeptDO> departments = Arrays.asList(dept(10L), dept(20L));
        List<DeptPriceFieldDO> relations = Collections.singletonList(relation(10L, "referencePrice"));
        when(fieldDefinitionMapper.selectListByModuleAndGroup("erp_product", "price_info"))
                .thenReturn(definitions);
        when(fieldDefinitionMapper.selectListByModuleAndGroupForUpdate(1L, "erp_product", "price_info"))
                .thenReturn(definitions);
        when(deptService.getDeptList(any(DeptListReqVO.class))).thenReturn(departments);
        when(deptService.getDeptList(eq(Collections.singleton(20L))))
                .thenReturn(Collections.singletonList(dept(20L)));
        when(deptPriceFieldMapper.selectListByFieldKeys(eq(Collections.singleton("referencePrice"))))
                .thenReturn(relations);
        DeptPriceFieldConfigRespVO config = service.getConfig();
        DeptPriceFieldUpdateReqVO reqVO = updateReq(
                config.getConfigVersion(), "referencePrice", Collections.singletonList(20L));

        service.updateConfig(reqVO);

        verify(deptPriceFieldMapper).restoreOrInsert(eq(20L), eq("referencePrice"), any(), eq(1L));
        verify(deptPriceFieldMapper).softDeleteByFieldKeyAndDeptIds(
                eq("referencePrice"), eq(Collections.singleton(10L)), any(), eq(1L));
    }

    @Test
    void updateConfig_shouldKeepVersionStableWhenLockedFieldsAreUnordered() {
        List<FieldDefinitionDO> sortedDefinitions = Arrays.asList(
                field("referencePrice", 1), field("retailPrice", 2));
        List<FieldDefinitionDO> reversedLockedDefinitions = Arrays.asList(
                field("retailPrice", 2), field("referencePrice", 1));
        when(fieldDefinitionMapper.selectListByModuleAndGroup("erp_product", "price_info"))
                .thenReturn(sortedDefinitions);
        when(fieldDefinitionMapper.selectListByModuleAndGroupForUpdate(1L, "erp_product", "price_info"))
                .thenReturn(reversedLockedDefinitions);
        when(deptService.getDeptList(any(DeptListReqVO.class))).thenReturn(Collections.singletonList(dept(10L)));
        when(deptPriceFieldMapper.selectListByFieldKeys(eq(asSet("referencePrice", "retailPrice"))))
                .thenReturn(Collections.emptyList());
        DeptPriceFieldConfigRespVO config = service.getConfig();
        DeptPriceFieldUpdateReqVO reqVO = updateReq(
                config.getConfigVersion(), "referencePrice", Collections.emptyList());

        assertDoesNotThrow(() -> service.updateConfig(reqVO));
    }

    private static FieldDefinitionDO field(String key, int sort) {
        FieldDefinitionDO field = new FieldDefinitionDO();
        field.setId((long) sort);
        field.setModule("erp_product");
        field.setFieldGroup("price_info");
        field.setFieldKey(key);
        field.setFieldLabel(key);
        field.setSort(sort);
        return field;
    }

    private static DeptDO dept(Long id) {
        DeptDO dept = new DeptDO();
        dept.setId(id);
        dept.setName("部门" + id);
        dept.setParentId(0L);
        dept.setSort(id.intValue());
        dept.setStatus(0);
        return dept;
    }

    private static DeptPriceFieldDO relation(Long deptId, String fieldKey) {
        DeptPriceFieldDO relation = new DeptPriceFieldDO();
        relation.setDeptId(deptId);
        relation.setFieldKey(fieldKey);
        return relation;
    }

    private static DeptPriceFieldUpdateReqVO updateReq(String version, String fieldKey, List<Long> deptIds) {
        DeptPriceFieldUpdateReqVO.Item item = new DeptPriceFieldUpdateReqVO.Item();
        item.setFieldKey(fieldKey);
        item.setDeptIds(deptIds);
        DeptPriceFieldUpdateReqVO reqVO = new DeptPriceFieldUpdateReqVO();
        reqVO.setConfigVersion(version);
        reqVO.setItems(Collections.singletonList(item));
        return reqVO;
    }

}
