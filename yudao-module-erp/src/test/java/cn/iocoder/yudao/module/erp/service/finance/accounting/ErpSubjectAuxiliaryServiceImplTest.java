package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpSubjectAuxiliaryDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpSubjectAuxiliaryMapper;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpAuxiliaryTypeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ErpSubjectAuxiliaryServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSubjectAuxiliaryServiceImpl subjectAuxiliaryService;

    @Mock
    private ErpSubjectAuxiliaryMapper subjectAuxiliaryMapper;

    // ==================== saveSubjectAuxiliary ====================

    @Test
    void testSaveSubjectAuxiliary_subjectIdNull() {
        // 调用：subjectId 为 null 直接返回，不调用 mapper
        subjectAuxiliaryService.saveSubjectAuxiliary(null, "1001",
                asList(ErpAuxiliaryTypeEnum.SUPPLIER.getType()));

        // 断言：完全没有任何 mapper 调用
        verify(subjectAuxiliaryMapper, never()).deleteBySubjectId(anyLong());
        verify(subjectAuxiliaryMapper, never()).insertBatch(anyList());
    }

    @Test
    void testSaveSubjectAuxiliary_emptyAuxiliaryTypes() {
        // 调用：auxiliaryTypes 为 null
        subjectAuxiliaryService.saveSubjectAuxiliary(100L, "1001", null);

        // 断言：只调 deleteBySubjectId，不调 insertBatch
        verify(subjectAuxiliaryMapper).deleteBySubjectId(100L);
        verify(subjectAuxiliaryMapper, never()).insertBatch(anyList());

        // 调用：auxiliaryTypes 为空集合
        reset(subjectAuxiliaryMapper);
        subjectAuxiliaryService.saveSubjectAuxiliary(101L, "1002", Collections.emptyList());

        verify(subjectAuxiliaryMapper).deleteBySubjectId(101L);
        verify(subjectAuxiliaryMapper, never()).insertBatch(anyList());
    }

    @Test
    void testSaveSubjectAuxiliary_allInvalid() {
        // 调用：全部是无效类型
        subjectAuxiliaryService.saveSubjectAuxiliary(100L, "1001",
                asList("invalid_type_1", "invalid_type_2", null));

        // 断言：deleteBySubjectId 调用了，但 insertBatch 没调
        verify(subjectAuxiliaryMapper).deleteBySubjectId(100L);
        verify(subjectAuxiliaryMapper, never()).insertBatch(anyList());
    }

    @Test
    void testSaveSubjectAuxiliary_validTypes() {
        // 准备：混合 valid + invalid + duplicate
        List<String> input = asList(
                ErpAuxiliaryTypeEnum.CUSTOMER.getType(),    // 有效
                ErpAuxiliaryTypeEnum.SUPPLIER.getType(),    // 有效
                "invalid_type",                              // 无效
                ErpAuxiliaryTypeEnum.CUSTOMER.getType(),    // 重复
                ErpAuxiliaryTypeEnum.PROJECT.getType(),     // 有效
                null                                          // null
        );

        // 调用
        subjectAuxiliaryService.saveSubjectAuxiliary(100L, "1001", input);

        // 断言：先 delete 再 insertBatch
        verify(subjectAuxiliaryMapper).deleteBySubjectId(100L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpSubjectAuxiliaryDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(subjectAuxiliaryMapper).insertBatch(captor.capture());
        List<ErpSubjectAuxiliaryDO> saved = captor.getValue();

        // 去重后只剩 3 个：customer, supplier, project（保持 LinkedHashSet 顺序）
        assertThat(saved).hasSize(3);
        assertThat(saved.get(0).getAuxiliaryType()).isEqualTo(ErpAuxiliaryTypeEnum.CUSTOMER.getType());
        assertThat(saved.get(1).getAuxiliaryType()).isEqualTo(ErpAuxiliaryTypeEnum.SUPPLIER.getType());
        assertThat(saved.get(2).getAuxiliaryType()).isEqualTo(ErpAuxiliaryTypeEnum.PROJECT.getType());

        // 断言：每条 subjectId / subjectCode / sort 设置正确
        assertThat(saved).allMatch(e -> e.getSubjectId().equals(100L));
        assertThat(saved).allMatch(e -> e.getSubjectCode().equals("1001"));
        assertThat(saved.get(0).getSort()).isEqualTo(0);
        assertThat(saved.get(1).getSort()).isEqualTo(1);
        assertThat(saved.get(2).getSort()).isEqualTo(2);
    }

    // ==================== deleteBySubjectId ====================

    @Test
    void testDeleteBySubjectId_null() {
        // 调用：subjectId 为 null 直接返回
        subjectAuxiliaryService.deleteBySubjectId(null);

        // 断言：mapper 没有被调用
        verify(subjectAuxiliaryMapper, never()).deleteBySubjectId(anyLong());
    }

    @Test
    void testDeleteBySubjectId() {
        // 调用
        subjectAuxiliaryService.deleteBySubjectId(100L);

        // 断言
        verify(subjectAuxiliaryMapper).deleteBySubjectId(100L);
    }

    // ==================== getListBySubjectId ====================

    @Test
    void testGetListBySubjectId() {
        // case 1: null subjectId 直接返回 emptyList，不调 mapper
        List<ErpSubjectAuxiliaryDO> nullResult = subjectAuxiliaryService.getListBySubjectId(null);
        assertThat(nullResult).isEmpty();
        verify(subjectAuxiliaryMapper, never()).selectListBySubjectId(anyLong());

        // case 2: 非 null subjectId 调 mapper
        ErpSubjectAuxiliaryDO entry1 = ErpSubjectAuxiliaryDO.builder()
                .id(1L).subjectId(100L).subjectCode("1001")
                .auxiliaryType(ErpAuxiliaryTypeEnum.CUSTOMER.getType()).sort(0).build();
        ErpSubjectAuxiliaryDO entry2 = ErpSubjectAuxiliaryDO.builder()
                .id(2L).subjectId(100L).subjectCode("1001")
                .auxiliaryType(ErpAuxiliaryTypeEnum.SUPPLIER.getType()).sort(1).build();
        when(subjectAuxiliaryMapper.selectListBySubjectId(100L)).thenReturn(asList(entry1, entry2));

        List<ErpSubjectAuxiliaryDO> result = subjectAuxiliaryService.getListBySubjectId(100L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAuxiliaryType()).isEqualTo(ErpAuxiliaryTypeEnum.CUSTOMER.getType());
        assertThat(result.get(1).getAuxiliaryType()).isEqualTo(ErpAuxiliaryTypeEnum.SUPPLIER.getType());
        verify(subjectAuxiliaryMapper).selectListBySubjectId(100L);
    }

    // ==================== getListBySubjectIds ====================

    @Test
    void testGetListBySubjectIds() {
        // case 1: null 集合 -> emptyList，不调 mapper
        List<ErpSubjectAuxiliaryDO> nullResult = subjectAuxiliaryService.getListBySubjectIds(null);
        assertThat(nullResult).isEmpty();

        // case 2: 空集合 -> emptyList，不调 mapper
        List<ErpSubjectAuxiliaryDO> emptyResult = subjectAuxiliaryService.getListBySubjectIds(Collections.emptyList());
        assertThat(emptyResult).isEmpty();

        verify(subjectAuxiliaryMapper, never()).selectListBySubjectIds(anyList());

        // case 3: 非空集合 -> 调 mapper
        ErpSubjectAuxiliaryDO entry1 = ErpSubjectAuxiliaryDO.builder()
                .id(1L).subjectId(100L).subjectCode("1001")
                .auxiliaryType(ErpAuxiliaryTypeEnum.CUSTOMER.getType()).sort(0).build();
        ErpSubjectAuxiliaryDO entry2 = ErpSubjectAuxiliaryDO.builder()
                .id(2L).subjectId(200L).subjectCode("1002")
                .auxiliaryType(ErpAuxiliaryTypeEnum.DEPT.getType()).sort(0).build();
        when(subjectAuxiliaryMapper.selectListBySubjectIds(anyList()))
                .thenReturn(Arrays.asList(entry1, entry2));

        List<ErpSubjectAuxiliaryDO> result = subjectAuxiliaryService.getListBySubjectIds(asList(100L, 200L));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSubjectId()).isEqualTo(100L);
        assertThat(result.get(1).getSubjectId()).isEqualTo(200L);
        verify(subjectAuxiliaryMapper).selectListBySubjectIds(anyList());
    }

}
