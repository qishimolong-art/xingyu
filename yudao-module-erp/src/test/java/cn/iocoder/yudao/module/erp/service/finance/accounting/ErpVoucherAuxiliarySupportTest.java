package cn.iocoder.yudao.module.erp.service.finance.accounting;
import cn.iocoder.yudao.module.erp.service.finance.accounting.rule.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.*;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.*;
@ExtendWith(MockitoExtension.class) @MockitoSettings(strictness=Strictness.LENIENT)
class ErpVoucherAuxiliarySupportTest {
    @InjectMocks ErpVoucherAuxiliarySupport support;
    @Mock ErpSubjectAuxiliaryService auxiliaryService; @Mock ErpVoucherSourceReader reader; @Mock ErpVoucherRuleStore store;
    @Mock DeptApi deptApi;@Mock AdminUserApi userApi;@Mock PermissionApi permissionApi;
    ErpVoucherItemDO item;
    @BeforeEach void setup(){
        item=new ErpVoucherItemDO().setSubjectId(1L);
        when(permissionApi.getDeptDataPermission(any(),eq("erp_voucher"))).thenReturn(new DeptDataPermissionRespDTO().setAll(true));
        when(deptApi.getDeptListByStatus(0)).thenReturn(Collections.singletonList(new DeptRespDTO().setId(2L).setName("总部")));
        when(reader.list(eq("erpSupplierMapper"),any())).thenReturn(Collections.singletonList(ErpVoucherRuleCalculatorTest.map("id",1L,"name","供应商")));
        Config c=new Config();Project p=new Project();p.setId(3L);p.setName("项目");p.setDeptIds(Collections.singletonList(2L));c.getProjects().add(p);when(store.config()).thenReturn(c);
        when(auxiliaryService.getListBySubjectId(1L)).thenReturn(Arrays.asList(new ErpSubjectAuxiliaryDO().setAuxiliaryType("supplier"),new ErpSubjectAuxiliaryDO().setAuxiliaryType("dept"),new ErpSubjectAuxiliaryDO().setAuxiliaryType("project")));
    }
    @Test void multidimensionalValuesRoundTripAndKeepReadableLegacySummary(){
        item.setAuxiliaries(Arrays.asList(ErpVoucherAuxiliarySupport.value("supplier",1L,"伪造名称"),ErpVoucherAuxiliarySupport.value("dept",2L,null),ErpVoucherAuxiliarySupport.value("project",3L,null)));
        support.validate(item);assertEquals("供应商 / 总部 / 项目",item.getAuxiliaryName());
        ErpVoucherItemDO read=JsonUtils.parseObject(JsonUtils.toJsonString(item),ErpVoucherItemDO.class);
        assertEquals(3,read.getAuxiliaries().size());assertEquals("项目",read.getAuxiliaries().get(2).getName());
    }
    @Test void requiredDimensionCannotBeDropped(){item.setAuxiliaries(Collections.emptyList());assertThrows(RuntimeException.class,()->support.validate(item));}
    @Test void duplicateAndConflictingLegacyInputRejected(){
        item.setAuxiliaries(Arrays.asList(ErpVoucherAuxiliarySupport.value("supplier",1L,null),ErpVoucherAuxiliarySupport.value("supplier",1L,null)));
        assertThrows(RuntimeException.class,()->support.validate(item));
        item.setAuxiliaryType("supplier");item.setAuxiliaryId(99L);assertThrows(RuntimeException.class,()->support.validate(item));
    }
    @Test void unavailableProjectCannotBeSelected(){when(permissionApi.getDeptDataPermission(any(),eq("erp_voucher"))).thenReturn(new DeptDataPermissionRespDTO());assertThrows(RuntimeException.class,()->support.resolve(ErpVoucherAuxiliarySupport.value("project",3L,null)));}
    @Test void legacyNameWithoutIdIsPreserved(){when(auxiliaryService.getListBySubjectId(1L)).thenReturn(Collections.emptyList());item.setAuxiliaryName("历史名称");support.validate(item);assertEquals("历史名称",item.getAuxiliaryName());}
}
