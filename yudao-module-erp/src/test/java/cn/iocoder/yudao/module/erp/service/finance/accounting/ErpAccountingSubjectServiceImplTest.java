package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpAccountingSubjectSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject.ErpOpeningBalanceUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpAccountingSubjectDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpAccountingSubjectMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNTING_SUBJECT_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNTING_SUBJECT_HAS_CHILDREN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNTING_SUBJECT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNTING_SUBJECT_USED_BY_VOUCHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpAccountingSubjectServiceImpl} 单元测试。
 *
 * 覆盖：
 *  - createSubject：父科目自动翻转 isLeaf / 编码重复 / 默认值兜底 / **S8 父科目不存在 静默允许**
 *  - updateSubject：编码冲突 / isLeaf 不可被覆盖 / auxiliaryTypes null vs empty 区分语义
 *  - deleteSubject：有子科目 / 被凭证引用 / 删后回填父 isLeaf
 *  - 各 getter / 简单列表 / 树形 / leafOnly 过滤
 *  - batchUpdateOpeningBalance：**S10 无存在性校验**
 *  - importOpeningBalance：科目不存在的反馈
 *  - importSubjects：DRY_RUN / OVERWRITE / SKIP 三种模式
 *  - parseSubjectCategory / parseBalanceDirection / parseVoucherType：**S9 接受任意整数绕过枚举校验**
 *
 * 重点 Bug 暴露：
 *  - S8：createSubject_parentCode_不存在_静默允许（应抛 ACCOUNTING_SUBJECT_NOT_EXISTS，当前未抛）
 *  - S9：枚举绕过 - parseSubjectCategory 接受 999 / -1
 *  - S10：batchUpdateOpeningBalance_id_不存在_静默通过（无存在性校验）
 */
public class ErpAccountingSubjectServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpAccountingSubjectServiceImpl subjectService;

    @Mock
    private ErpAccountingSubjectMapper subjectMapper;

    @Mock
    private ErpSubjectAuxiliaryService subjectAuxiliaryService;

    @Mock
    private ErpVoucherItemMapper voucherItemMapper;

    /** 公共：构造 SaveReqVO 模板（一级科目 1001 现金） */
    private ErpAccountingSubjectSaveReqVO buildBaseReq() {
        ErpAccountingSubjectSaveReqVO req = new ErpAccountingSubjectSaveReqVO();
        req.setSubjectCode("1001");
        req.setSubjectName("库存现金");
        req.setSubjectCategory(1);
        return req;
    }

    // ==================== createSubject ====================

    @Test
    @DisplayName("createSubject：正常 - 一级科目 + 默认 isLeaf=true / enable=true / sort=0 / openingBalance=0")
    public void testCreateSubject_normalCase() {
        ErpAccountingSubjectSaveReqVO req = buildBaseReq();
        when(subjectMapper.selectBySubjectCode(eq("1001"))).thenReturn(null);
        // 模拟 insert 回填 ID
        ArgumentCaptor<ErpAccountingSubjectDO> insertCaptor = ArgumentCaptor.forClass(ErpAccountingSubjectDO.class);
        when(subjectMapper.insert(any(ErpAccountingSubjectDO.class))).thenAnswer(inv -> {
            ((ErpAccountingSubjectDO) inv.getArgument(0)).setId(100L);
            return 1;
        });

        Long id = subjectService.createSubject(req);
        verify(subjectMapper).insert(insertCaptor.capture());

        assertEquals(100L, id);
        ErpAccountingSubjectDO captured = insertCaptor.getValue();
        assertEquals("1001", captured.getSubjectCode());
        assertEquals(Boolean.TRUE, captured.getIsLeaf());
        assertEquals(Boolean.TRUE, captured.getEnable());
        assertEquals(0, captured.getSort());
        assertEquals(0, captured.getOpeningBalance().compareTo(BigDecimal.ZERO));
        // subjectLevel：编码长度 4 → 1 级
        assertEquals(1, captured.getSubjectLevel());

        verify(subjectAuxiliaryService).saveSubjectAuxiliary(eq(100L), eq("1001"), any());
    }

    @Test
    @DisplayName("createSubject：subjectCode 重复 - 抛 ACCOUNTING_SUBJECT_CODE_DUPLICATE")
    public void testCreateSubject_codeDuplicate() {
        when(subjectMapper.selectBySubjectCode(eq("1001"))).thenReturn(
                new ErpAccountingSubjectDO().setId(99L).setSubjectCode("1001"));

        assertServiceException(() -> subjectService.createSubject(buildBaseReq()),
                ACCOUNTING_SUBJECT_CODE_DUPLICATE, "1001");
        verify(subjectMapper, never()).insert(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("createSubject：父科目存在 + 父科目原是 isLeaf=true - 翻转父科目为非末级")
    public void testCreateSubject_flipParentLeaf() {
        ErpAccountingSubjectSaveReqVO req = buildBaseReq();
        req.setSubjectCode("100101");
        req.setParentCode("1001");

        when(subjectMapper.selectBySubjectCode(eq("100101"))).thenReturn(null);
        when(subjectMapper.selectBySubjectCode(eq("1001"))).thenReturn(
                new ErpAccountingSubjectDO().setId(99L).setSubjectCode("1001").setIsLeaf(true).setSubjectLevel(1));
        when(subjectMapper.insert(any(ErpAccountingSubjectDO.class))).thenAnswer(inv -> {
            ((ErpAccountingSubjectDO) inv.getArgument(0)).setId(200L);
            return 1;
        });

        subjectService.createSubject(req);

        // 验证父科目被翻转为非末级
        ArgumentCaptor<ErpAccountingSubjectDO> updateCaptor = ArgumentCaptor.forClass(ErpAccountingSubjectDO.class);
        verify(subjectMapper).updateById(updateCaptor.capture());
        assertEquals(99L, updateCaptor.getValue().getId());
        assertEquals(Boolean.FALSE, updateCaptor.getValue().getIsLeaf());
    }

    @Test
    @DisplayName("createSubject：父科目存在 + 父科目原是 isLeaf=false - 不重复 update")
    public void testCreateSubject_parentAlreadyNonLeaf() {
        ErpAccountingSubjectSaveReqVO req = buildBaseReq();
        req.setSubjectCode("100101");
        req.setParentCode("1001");

        when(subjectMapper.selectBySubjectCode(eq("100101"))).thenReturn(null);
        when(subjectMapper.selectBySubjectCode(eq("1001"))).thenReturn(
                new ErpAccountingSubjectDO().setId(99L).setSubjectCode("1001").setIsLeaf(false).setSubjectLevel(1));
        when(subjectMapper.insert(any(ErpAccountingSubjectDO.class))).thenAnswer(inv -> {
            ((ErpAccountingSubjectDO) inv.getArgument(0)).setId(200L);
            return 1;
        });

        subjectService.createSubject(req);

        // 不调用 updateById（父已是 isLeaf=false）
        verify(subjectMapper, never()).updateById(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("S8 已修复：createSubject_parentCode_不存在 - 抛 ACCOUNTING_SUBJECT_NOT_EXISTS")
    public void testCreateSubject_bugS8_parentNotExists_silentlyAllowed() {
        ErpAccountingSubjectSaveReqVO req = buildBaseReq();
        req.setSubjectCode("100101");
        req.setParentCode("1001"); // parentCode 但 1001 查不到

        when(subjectMapper.selectBySubjectCode(eq("100101"))).thenReturn(null);
        when(subjectMapper.selectBySubjectCode(eq("1001"))).thenReturn(null); // 父查不到

        // S8 修复后：父科目不存在应抛 ACCOUNTING_SUBJECT_NOT_EXISTS，且不应 insert
        assertServiceException(() -> subjectService.createSubject(req),
                ACCOUNTING_SUBJECT_NOT_EXISTS);
        verify(subjectMapper, never()).insert(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("createSubject：编码长度=6 - subjectLevel 自动推断为 2 + parentCode 自动截前 4 位")
    public void testCreateSubject_inferLevelByCodeLength() {
        ErpAccountingSubjectSaveReqVO req = buildBaseReq();
        req.setSubjectCode("100201");
        req.setParentCode(null); // 不传 parentCode 走长度推断

        when(subjectMapper.selectBySubjectCode(eq("100201"))).thenReturn(null);
        when(subjectMapper.selectBySubjectCode(eq("1002"))).thenReturn(
                new ErpAccountingSubjectDO().setId(98L).setSubjectCode("1002").setIsLeaf(false).setSubjectLevel(1));
        ArgumentCaptor<ErpAccountingSubjectDO> insertCaptor = ArgumentCaptor.forClass(ErpAccountingSubjectDO.class);
        when(subjectMapper.insert(any(ErpAccountingSubjectDO.class))).thenAnswer(inv -> {
            ((ErpAccountingSubjectDO) inv.getArgument(0)).setId(300L);
            return 1;
        });

        subjectService.createSubject(req);
        verify(subjectMapper).insert(insertCaptor.capture());
        // 编码长 6 → level=2 + parentCode = "1002"
        assertEquals(2, insertCaptor.getValue().getSubjectLevel());
        assertEquals("1002", insertCaptor.getValue().getParentCode());
    }

    // ==================== updateSubject ====================

    @Test
    @DisplayName("updateSubject：subjectCode 不变 - 跳过唯一性校验，可正常更新")
    public void testUpdateSubject_codeNotChanged() {
        Long id = 100L;
        when(subjectMapper.selectById(eq(id))).thenReturn(
                new ErpAccountingSubjectDO().setId(id).setSubjectCode("1001").setIsLeaf(true));

        ErpAccountingSubjectSaveReqVO req = buildBaseReq();
        req.setId(id);
        req.setSubjectCode("1001"); // 同 old.subjectCode
        req.setSubjectName("库存现金（修改）");

        subjectService.updateSubject(req);

        verify(subjectMapper).updateById(any(ErpAccountingSubjectDO.class));
        // 不调用 selectBySubjectCode（编码没变）
        verify(subjectMapper, never()).selectBySubjectCode(anyString());
    }

    @Test
    @DisplayName("updateSubject：subjectCode 变化 - 检查唯一性，不重复则更新")
    public void testUpdateSubject_codeChangedNoConflict() {
        Long id = 100L;
        when(subjectMapper.selectById(eq(id))).thenReturn(
                new ErpAccountingSubjectDO().setId(id).setSubjectCode("1001").setIsLeaf(true));
        when(subjectMapper.selectBySubjectCode(eq("1002"))).thenReturn(null);

        ErpAccountingSubjectSaveReqVO req = buildBaseReq();
        req.setId(id);
        req.setSubjectCode("1002");

        subjectService.updateSubject(req);

        verify(subjectMapper).updateById(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("updateSubject：subjectCode 变化且重复 - 抛 ACCOUNTING_SUBJECT_CODE_DUPLICATE")
    public void testUpdateSubject_codeChangedConflict() {
        Long id = 100L;
        when(subjectMapper.selectById(eq(id))).thenReturn(
                new ErpAccountingSubjectDO().setId(id).setSubjectCode("1001").setIsLeaf(true));
        when(subjectMapper.selectBySubjectCode(eq("1002"))).thenReturn(
                new ErpAccountingSubjectDO().setId(200L).setSubjectCode("1002"));

        ErpAccountingSubjectSaveReqVO req = buildBaseReq();
        req.setId(id);
        req.setSubjectCode("1002");

        assertServiceException(() -> subjectService.updateSubject(req),
                ACCOUNTING_SUBJECT_CODE_DUPLICATE, "1002");
    }

    @Test
    @DisplayName("updateSubject：isLeaf 不可被 VO 覆盖（service 内强制 setIsLeaf(null)）")
    public void testUpdateSubject_isLeafNotOverridable() {
        Long id = 100L;
        when(subjectMapper.selectById(eq(id))).thenReturn(
                new ErpAccountingSubjectDO().setId(id).setSubjectCode("1001").setIsLeaf(true));

        ErpAccountingSubjectSaveReqVO req = buildBaseReq();
        req.setId(id);
        req.setSubjectCode("1001");

        ArgumentCaptor<ErpAccountingSubjectDO> updateCaptor = ArgumentCaptor.forClass(ErpAccountingSubjectDO.class);
        subjectService.updateSubject(req);
        verify(subjectMapper).updateById(updateCaptor.capture());
        assertNull(updateCaptor.getValue().getIsLeaf(), "update DO 的 isLeaf 必须置 null，由 create/delete 维护");
    }

    @Test
    @DisplayName("updateSubject：auxiliaryTypes=null - 不修改辅助核算（语义：不传不修改）")
    public void testUpdateSubject_auxiliaryTypesNullSkip() {
        Long id = 100L;
        when(subjectMapper.selectById(eq(id))).thenReturn(
                new ErpAccountingSubjectDO().setId(id).setSubjectCode("1001").setIsLeaf(true));

        ErpAccountingSubjectSaveReqVO req = buildBaseReq();
        req.setId(id);
        req.setSubjectCode("1001");
        req.setAuxiliaryTypes(null); // null = 不修改

        subjectService.updateSubject(req);

        verify(subjectAuxiliaryService, never()).saveSubjectAuxiliary(anyLong(), anyString(), anyCollection());
    }

    @Test
    @DisplayName("updateSubject：auxiliaryTypes=空集合 - 清空辅助核算（语义：传空清空）")
    public void testUpdateSubject_auxiliaryTypesEmptyClear() {
        Long id = 100L;
        when(subjectMapper.selectById(eq(id))).thenReturn(
                new ErpAccountingSubjectDO().setId(id).setSubjectCode("1001").setIsLeaf(true));

        ErpAccountingSubjectSaveReqVO req = buildBaseReq();
        req.setId(id);
        req.setSubjectCode("1001");
        req.setAuxiliaryTypes(Collections.emptyList()); // 空集合 = 清空

        subjectService.updateSubject(req);

        verify(subjectAuxiliaryService).saveSubjectAuxiliary(eq(id), eq("1001"), eq(Collections.emptyList()));
    }

    @Test
    @DisplayName("updateSubject：科目不存在 - 抛 ACCOUNTING_SUBJECT_NOT_EXISTS")
    public void testUpdateSubject_notExists() {
        when(subjectMapper.selectById(eq(999L))).thenReturn(null);

        ErpAccountingSubjectSaveReqVO req = buildBaseReq();
        req.setId(999L);

        assertServiceException(() -> subjectService.updateSubject(req), ACCOUNTING_SUBJECT_NOT_EXISTS);
    }

    // ==================== deleteSubject ====================

    @Test
    @DisplayName("deleteSubject：无子 + 未被引用 - 删除 + 清辅助 + 父无其他子时回填 isLeaf=true")
    public void testDeleteSubject_normalCase() {
        Long id = 100L;
        ErpAccountingSubjectDO sub = new ErpAccountingSubjectDO()
                .setId(id).setSubjectCode("100101").setParentCode("1001");
        when(subjectMapper.selectById(eq(id))).thenReturn(sub);
        when(subjectMapper.selectListByParentCode(eq("100101"))).thenReturn(Collections.emptyList());
        when(voucherItemMapper.selectCountBySubjectId(eq(id))).thenReturn(0L);
        // 兄弟节点已无（删自己后）
        when(subjectMapper.selectListByParentCode(eq("1001"))).thenReturn(Collections.emptyList());
        when(subjectMapper.selectBySubjectCode(eq("1001"))).thenReturn(
                new ErpAccountingSubjectDO().setId(99L).setSubjectCode("1001").setIsLeaf(false));

        subjectService.deleteSubject(id);

        verify(subjectMapper).deleteById(eq(id));
        verify(subjectAuxiliaryService).deleteBySubjectId(eq(id));
        // 父科目回填 isLeaf=true
        ArgumentCaptor<ErpAccountingSubjectDO> patchCaptor = ArgumentCaptor.forClass(ErpAccountingSubjectDO.class);
        verify(subjectMapper).updateById(patchCaptor.capture());
        assertEquals(99L, patchCaptor.getValue().getId());
        assertEquals(Boolean.TRUE, patchCaptor.getValue().getIsLeaf());
    }

    @Test
    @DisplayName("deleteSubject：父科目还有其他子 - 不回填 isLeaf")
    public void testDeleteSubject_parentStillHasOtherChildren() {
        Long id = 100L;
        ErpAccountingSubjectDO sub = new ErpAccountingSubjectDO()
                .setId(id).setSubjectCode("100101").setParentCode("1001");
        when(subjectMapper.selectById(eq(id))).thenReturn(sub);
        when(subjectMapper.selectListByParentCode(eq("100101"))).thenReturn(Collections.emptyList());
        when(voucherItemMapper.selectCountBySubjectId(eq(id))).thenReturn(0L);
        // 兄弟节点还在
        when(subjectMapper.selectListByParentCode(eq("1001"))).thenReturn(
                Collections.singletonList(new ErpAccountingSubjectDO().setId(101L).setSubjectCode("100102")));

        subjectService.deleteSubject(id);

        verify(subjectMapper).deleteById(eq(id));
        // 不调用 selectBySubjectCode("1001") 也不更新父
        verify(subjectMapper, never()).selectBySubjectCode(anyString());
        verify(subjectMapper, never()).updateById(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("deleteSubject：有子科目 - 抛 ACCOUNTING_SUBJECT_HAS_CHILDREN")
    public void testDeleteSubject_hasChildren() {
        Long id = 99L;
        ErpAccountingSubjectDO sub = new ErpAccountingSubjectDO().setId(id).setSubjectCode("1001");
        when(subjectMapper.selectById(eq(id))).thenReturn(sub);
        when(subjectMapper.selectListByParentCode(eq("1001"))).thenReturn(
                Collections.singletonList(new ErpAccountingSubjectDO().setId(100L).setSubjectCode("100101")));

        assertServiceException(() -> subjectService.deleteSubject(id),
                ACCOUNTING_SUBJECT_HAS_CHILDREN, "1001");
        verify(subjectMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteSubject：被凭证引用 - 抛 ACCOUNTING_SUBJECT_USED_BY_VOUCHER")
    public void testDeleteSubject_usedByVoucher() {
        Long id = 99L;
        ErpAccountingSubjectDO sub = new ErpAccountingSubjectDO().setId(id).setSubjectCode("1001");
        when(subjectMapper.selectById(eq(id))).thenReturn(sub);
        when(subjectMapper.selectListByParentCode(eq("1001"))).thenReturn(Collections.emptyList());
        when(voucherItemMapper.selectCountBySubjectId(eq(id))).thenReturn(5L);

        assertServiceException(() -> subjectService.deleteSubject(id),
                ACCOUNTING_SUBJECT_USED_BY_VOUCHER, "1001");
        verify(subjectMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteSubject：科目不存在 - 抛 ACCOUNTING_SUBJECT_NOT_EXISTS")
    public void testDeleteSubject_notExists() {
        when(subjectMapper.selectById(eq(999L))).thenReturn(null);

        assertServiceException(() -> subjectService.deleteSubject(999L), ACCOUNTING_SUBJECT_NOT_EXISTS);
    }

    // ==================== getter / 列表 ====================

    @Test
    @DisplayName("getSubject：透传 selectById")
    public void testGetSubject() {
        when(subjectMapper.selectById(eq(100L))).thenReturn(
                new ErpAccountingSubjectDO().setId(100L).setSubjectCode("1001"));

        ErpAccountingSubjectDO result = subjectService.getSubject(100L);

        assertNotNull(result);
        assertEquals("1001", result.getSubjectCode());
    }

    @Test
    @DisplayName("getSubjectByCode：空字符串 - 返回 null（不查 mapper）")
    public void testGetSubjectByCode_emptyString() {
        ErpAccountingSubjectDO result = subjectService.getSubjectByCode("");
        assertNull(result);

        ErpAccountingSubjectDO resultNull = subjectService.getSubjectByCode(null);
        assertNull(resultNull);

        verify(subjectMapper, never()).selectBySubjectCode(anyString());
    }

    @Test
    @DisplayName("getSubjectByCode：存在 - 返回 DO")
    public void testGetSubjectByCode_existing() {
        when(subjectMapper.selectBySubjectCode(eq("1001"))).thenReturn(
                new ErpAccountingSubjectDO().setId(100L).setSubjectCode("1001"));

        ErpAccountingSubjectDO result = subjectService.getSubjectByCode("1001");
        assertNotNull(result);
        assertEquals(100L, result.getId());
    }

    @Test
    @DisplayName("getSubjectList：空集合 - 返回空 List（不查 mapper，避免 'WHERE id IN ()' SQL 错误）")
    public void testGetSubjectList_emptyCollection() {
        List<ErpAccountingSubjectDO> result = subjectService.getSubjectList(Collections.emptyList());
        assertTrue(result.isEmpty());
        verify(subjectMapper, never()).selectByIds(any());

        // null 集合也走兜底
        List<ErpAccountingSubjectDO> resultNull = subjectService.getSubjectList(null);
        assertTrue(resultNull.isEmpty());
    }

    @Test
    @DisplayName("getSubjectList：有 ID - 透传 mapper.selectByIds")
    public void testGetSubjectList_normalCase() {
        Collection<Long> ids = Arrays.asList(100L, 101L);
        when(subjectMapper.selectByIds(eq(ids))).thenReturn(Arrays.asList(
                new ErpAccountingSubjectDO().setId(100L),
                new ErpAccountingSubjectDO().setId(101L)
        ));

        List<ErpAccountingSubjectDO> result = subjectService.getSubjectList(ids);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getSubjectMap：返回 id → DO 映射")
    public void testGetSubjectMap() {
        Collection<Long> ids = Arrays.asList(100L, 101L);
        when(subjectMapper.selectByIds(eq(ids))).thenReturn(Arrays.asList(
                new ErpAccountingSubjectDO().setId(100L).setSubjectCode("1001"),
                new ErpAccountingSubjectDO().setId(101L).setSubjectCode("1002")
        ));

        Map<Long, ErpAccountingSubjectDO> map = subjectService.getSubjectMap(ids);

        assertThat(map).hasSize(2);
        assertEquals("1001", map.get(100L).getSubjectCode());
        assertEquals("1002", map.get(101L).getSubjectCode());
    }

    @Test
    @DisplayName("getSubjectSimpleList：leafOnly=true + category=null - 调 selectListByIsLeaf(true)")
    public void testGetSubjectSimpleList_leafOnlyNoCategory() {
        when(subjectMapper.selectListByIsLeaf(eq(true))).thenReturn(
                Collections.singletonList(new ErpAccountingSubjectDO().setId(100L).setIsLeaf(true)));

        List<ErpAccountingSubjectDO> result = subjectService.getSubjectSimpleList(true, null);

        assertThat(result).hasSize(1);
        verify(subjectMapper).selectListByIsLeaf(eq(true));
    }

    @Test
    @DisplayName("getSubjectSimpleList：leafOnly=true + category=1 - 该大类下过滤末级")
    public void testGetSubjectSimpleList_leafOnlyAndCategory() {
        when(subjectMapper.selectListByCategory(eq(1))).thenReturn(Arrays.asList(
                new ErpAccountingSubjectDO().setId(100L).setIsLeaf(true),
                new ErpAccountingSubjectDO().setId(101L).setIsLeaf(false), // 非末级被过滤
                new ErpAccountingSubjectDO().setId(102L).setIsLeaf(true)
        ));

        List<ErpAccountingSubjectDO> result = subjectService.getSubjectSimpleList(true, 1);

        assertThat(result).hasSize(2);
        assertTrue(result.stream().allMatch(s -> Boolean.TRUE.equals(s.getIsLeaf())));
    }

    @Test
    @DisplayName("getSubjectSimpleList：leafOnly=null + category=null - 全量按编码升序")
    public void testGetSubjectSimpleList_noFilter() {
        when(subjectMapper.selectListAllOrderByCode()).thenReturn(Arrays.asList(
                new ErpAccountingSubjectDO().setId(100L),
                new ErpAccountingSubjectDO().setId(101L)
        ));

        List<ErpAccountingSubjectDO> result = subjectService.getSubjectSimpleList(null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getLeafSubjectList：转发 selectListByIsLeaf(true)")
    public void testGetLeafSubjectList() {
        when(subjectMapper.selectListByIsLeaf(eq(true))).thenReturn(
                Collections.singletonList(new ErpAccountingSubjectDO().setId(100L).setIsLeaf(true)));

        assertThat(subjectService.getLeafSubjectList()).hasSize(1);
    }

    @Test
    @DisplayName("getSubjectTreeList：null - 调 selectListAllOrderByCode + 按 parentCode 组装树")
    public void testGetSubjectTreeList_buildTree() {
        when(subjectMapper.selectListAllOrderByCode()).thenReturn(Arrays.asList(
                new ErpAccountingSubjectDO().setId(99L).setSubjectCode("1001").setParentCode(null),
                new ErpAccountingSubjectDO().setId(100L).setSubjectCode("100101").setParentCode("1001"),
                new ErpAccountingSubjectDO().setId(101L).setSubjectCode("100102").setParentCode("1001")
        ));
        when(subjectAuxiliaryService.getListBySubjectIds(any())).thenReturn(Collections.emptyList());

        List<ErpAccountingSubjectRespVO> result = subjectService.getSubjectTreeList(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren()).hasSize(2);
    }

    // ==================== batchUpdateOpeningBalance ====================

    @Test
    @DisplayName("batchUpdateOpeningBalance：正常 - 逐条 updateById")
    public void testBatchUpdateOpeningBalance_normalCase() {
        ErpOpeningBalanceUpdateReqVO req = new ErpOpeningBalanceUpdateReqVO();
        ErpOpeningBalanceUpdateReqVO.Item i1 = new ErpOpeningBalanceUpdateReqVO.Item();
        i1.setId(100L);
        i1.setOpeningBalance(new BigDecimal("1000.00"));
        ErpOpeningBalanceUpdateReqVO.Item i2 = new ErpOpeningBalanceUpdateReqVO.Item();
        i2.setId(101L);
        i2.setOpeningBalance(new BigDecimal("2000.00"));
        req.setItems(Arrays.asList(i1, i2));
        when(subjectMapper.selectByIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpAccountingSubjectDO().setId(100L),
                new ErpAccountingSubjectDO().setId(101L)));

        subjectService.batchUpdateOpeningBalance(req);

        verify(subjectMapper, times(2)).updateById(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("batchUpdateOpeningBalance：openingBalance=null - 默认置 0")
    public void testBatchUpdateOpeningBalance_nullOpeningBalanceTreatedAsZero() {
        ErpOpeningBalanceUpdateReqVO req = new ErpOpeningBalanceUpdateReqVO();
        ErpOpeningBalanceUpdateReqVO.Item i1 = new ErpOpeningBalanceUpdateReqVO.Item();
        i1.setId(100L);
        i1.setOpeningBalance(null);
        req.setItems(Collections.singletonList(i1));
        when(subjectMapper.selectByIds(anyCollection())).thenReturn(Collections.singletonList(
                new ErpAccountingSubjectDO().setId(100L)));

        ArgumentCaptor<ErpAccountingSubjectDO> captor = ArgumentCaptor.forClass(ErpAccountingSubjectDO.class);
        subjectService.batchUpdateOpeningBalance(req);
        verify(subjectMapper).updateById(captor.capture());

        assertEquals(0, captor.getValue().getOpeningBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("S10 已修复：batchUpdateOpeningBalance_id_不存在 - 抛 ACCOUNTING_SUBJECT_NOT_EXISTS")
    public void testBatchUpdateOpeningBalance_bugS10_idNotExistsSilent() {
        ErpOpeningBalanceUpdateReqVO req = new ErpOpeningBalanceUpdateReqVO();
        ErpOpeningBalanceUpdateReqVO.Item i1 = new ErpOpeningBalanceUpdateReqVO.Item();
        i1.setId(99999L); // 不存在的 id
        i1.setOpeningBalance(new BigDecimal("1000.00"));
        req.setItems(Collections.singletonList(i1));

        // S10 修复后：先做存在性校验，发现 id 缺失则抛 ACCOUNTING_SUBJECT_NOT_EXISTS
        when(subjectMapper.selectByIds(anyCollection())).thenReturn(Collections.emptyList());

        assertServiceException(() -> subjectService.batchUpdateOpeningBalance(req),
                ACCOUNTING_SUBJECT_NOT_EXISTS);
        verify(subjectMapper, never()).updateById(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("batchUpdateOpeningBalance：null reqVO - 直接返回不抛")
    public void testBatchUpdateOpeningBalance_nullReq() {
        subjectService.batchUpdateOpeningBalance(null);
        verify(subjectMapper, never()).updateById(any(ErpAccountingSubjectDO.class));
    }

    // ==================== importOpeningBalance ====================

    @Test
    @DisplayName("importOpeningBalance：科目存在 - 更新成功，map 值为 null（无错误）")
    public void testImportOpeningBalance_subjectExists() {
        ErpAccountingSubjectImportExcelVO row = new ErpAccountingSubjectImportExcelVO();
        row.setSubjectCode("1001");
        row.setOpeningBalance(new BigDecimal("1000.00"));
        when(subjectMapper.selectListBySubjectCodes(any())).thenReturn(
                Collections.singletonList(new ErpAccountingSubjectDO().setId(100L).setSubjectCode("1001")));

        Map<String, String> result = subjectService.importOpeningBalance(Collections.singletonList(row));

        assertThat(result).hasSize(1);
        assertNull(result.get("1001"));
        verify(subjectMapper).updateById(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("importOpeningBalance：科目不存在 - 反馈失败原因")
    public void testImportOpeningBalance_subjectNotExists() {
        ErpAccountingSubjectImportExcelVO row = new ErpAccountingSubjectImportExcelVO();
        row.setSubjectCode("9999");
        row.setOpeningBalance(new BigDecimal("1000.00"));
        when(subjectMapper.selectListBySubjectCodes(any())).thenReturn(Collections.emptyList());

        Map<String, String> result = subjectService.importOpeningBalance(Collections.singletonList(row));

        assertThat(result).hasSize(1);
        assertEquals("科目不存在，请先建立科目", result.get("9999"));
        verify(subjectMapper, never()).updateById(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("importOpeningBalance：空列表 - 返回空 Map")
    public void testImportOpeningBalance_emptyList() {
        Map<String, String> result = subjectService.importOpeningBalance(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    // ==================== importSubjects ====================

    @Test
    @DisplayName("importSubjects：DRY_RUN - 不写库，仅记录 existedCodes")
    public void testImportSubjects_dryRun() {
        ErpAccountingSubjectImportExcelVO row = new ErpAccountingSubjectImportExcelVO();
        row.setSubjectCode("1001");
        when(subjectMapper.selectListBySubjectCodes(any())).thenReturn(
                Collections.singletonList(new ErpAccountingSubjectDO().setId(100L).setSubjectCode("1001")));

        ErpAccountingSubjectImportRespVO resp = subjectService.importSubjects(
                Collections.singletonList(row), "DRY_RUN");

        assertEquals(Boolean.TRUE, resp.getDryRun());
        assertThat(resp.getExistedCodes()).contains("1001");
        verify(subjectMapper, never()).insert(any(ErpAccountingSubjectDO.class));
        verify(subjectMapper, never()).updateById(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("importSubjects：OVERWRITE - 已存在则覆盖")
    public void testImportSubjects_overwrite() {
        ErpAccountingSubjectImportExcelVO row = new ErpAccountingSubjectImportExcelVO();
        row.setSubjectCode("1001");
        row.setSubjectName("库存现金（修改）");
        row.setSubjectCategory("1");
        row.setOpeningBalance(new BigDecimal("100"));
        when(subjectMapper.selectListBySubjectCodes(any())).thenReturn(
                Collections.singletonList(new ErpAccountingSubjectDO().setId(100L).setSubjectCode("1001")));

        ErpAccountingSubjectImportRespVO resp = subjectService.importSubjects(
                Collections.singletonList(row), "OVERWRITE");

        assertFalse(resp.getDryRun());
        assertThat(resp.getUpdatedCodes()).contains("1001");
        verify(subjectMapper).updateById(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("importSubjects：SKIP - 已存在则跳过")
    public void testImportSubjects_skip() {
        ErpAccountingSubjectImportExcelVO row = new ErpAccountingSubjectImportExcelVO();
        row.setSubjectCode("1001");
        when(subjectMapper.selectListBySubjectCodes(any())).thenReturn(
                Collections.singletonList(new ErpAccountingSubjectDO().setId(100L).setSubjectCode("1001")));

        ErpAccountingSubjectImportRespVO resp = subjectService.importSubjects(
                Collections.singletonList(row), "SKIP");

        assertFalse(resp.getDryRun());
        assertThat(resp.getSkippedCodes()).contains("1001");
        verify(subjectMapper, never()).updateById(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("importSubjects：新增（已不存在） - 创建成功")
    public void testImportSubjects_newCreate() {
        ErpAccountingSubjectImportExcelVO row = new ErpAccountingSubjectImportExcelVO();
        row.setSubjectCode("1001");
        row.setSubjectName("库存现金");
        row.setSubjectCategory("1");
        row.setOpeningBalance(new BigDecimal("100"));
        lenient().when(subjectMapper.selectListBySubjectCodes(any())).thenReturn(Collections.emptyList());
        // inferParentAndLevel 内会查 parent
        lenient().when(subjectMapper.selectBySubjectCode(anyString())).thenReturn(null);

        ErpAccountingSubjectImportRespVO resp = subjectService.importSubjects(
                Collections.singletonList(row), "OVERWRITE");

        assertThat(resp.getCreatedCodes()).contains("1001");
        verify(subjectMapper).insert(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("importSubjects：新增 - 科目名称为空时降级到 failures")
    public void testImportSubjects_failureCaptured() {
        ErpAccountingSubjectImportExcelVO row = new ErpAccountingSubjectImportExcelVO();
        row.setSubjectCode("1001");
        // 不设置 subjectName 故意触发 IllegalArgumentException
        row.setSubjectCategory("1");
        when(subjectMapper.selectListBySubjectCodes(any())).thenReturn(Collections.emptyList());

        ErpAccountingSubjectImportRespVO resp = subjectService.importSubjects(
                Collections.singletonList(row), "OVERWRITE");

        assertThat(resp.getCreatedCodes()).isEmpty();
        assertThat(resp.getFailures()).hasSize(1);
        assertEquals("1001", resp.getFailures().get(0).getSubjectCode());
        assertThat(resp.getFailures().get(0).getReason()).contains("科目名称不能为空");
    }

    @Test
    @DisplayName("importSubjects：subjectCode 为空 - 跳过该行")
    public void testImportSubjects_blankCodeSkipped() {
        ErpAccountingSubjectImportExcelVO blank = new ErpAccountingSubjectImportExcelVO();
        blank.setSubjectCode(""); // 空
        ErpAccountingSubjectImportExcelVO whitespace = new ErpAccountingSubjectImportExcelVO();
        whitespace.setSubjectCode("   "); // 空白
        ErpAccountingSubjectImportExcelVO nullCode = new ErpAccountingSubjectImportExcelVO();
        nullCode.setSubjectCode(null);
        lenient().when(subjectMapper.selectListBySubjectCodes(any())).thenReturn(Collections.emptyList());

        ErpAccountingSubjectImportRespVO resp = subjectService.importSubjects(
                Arrays.asList(blank, whitespace, nullCode), "OVERWRITE");

        assertThat(resp.getCreatedCodes()).isEmpty();
        assertThat(resp.getUpdatedCodes()).isEmpty();
        verify(subjectMapper, never()).insert(any(ErpAccountingSubjectDO.class));
        verify(subjectMapper, never()).updateById(any(ErpAccountingSubjectDO.class));
    }

    @Test
    @DisplayName("S9 已修复：importSubjects subjectCategory='999' 非法整数 - 该行降级到 failures，不 insert")
    public void testImportSubjects_bugS9_categoryBypass() {
        ErpAccountingSubjectImportExcelVO row = new ErpAccountingSubjectImportExcelVO();
        row.setSubjectCode("9001");
        row.setSubjectName("非法分类测试");
        row.setSubjectCategory("999"); // 不在枚举范围（1-6）
        lenient().when(subjectMapper.selectListBySubjectCodes(any())).thenReturn(Collections.emptyList());
        lenient().when(subjectMapper.selectBySubjectCode(anyString())).thenReturn(null);

        ErpAccountingSubjectImportRespVO resp = subjectService.importSubjects(
                Collections.singletonList(row), "OVERWRITE");

        // S9 修复后：999 不在 1-6 范围内，parseSubjectCategory 返回 null，buildNewSubjectFromRow 抛异常 → failures
        verify(subjectMapper, never()).insert(any(ErpAccountingSubjectDO.class));
        assertThat(resp.getCreatedCodes()).doesNotContain("9001");
        assertThat(resp.getFailures()).anyMatch(f -> "9001".equals(f.getSubjectCode()));
    }

    @Test
    @DisplayName("S9 已修复：importSubjects subjectCategory='-1' 负数 - 该行降级到 failures")
    public void testImportSubjects_bugS9_negativeCategoryBypass() {
        ErpAccountingSubjectImportExcelVO row = new ErpAccountingSubjectImportExcelVO();
        row.setSubjectCode("9002");
        row.setSubjectName("负数测试");
        row.setSubjectCategory("-1");
        lenient().when(subjectMapper.selectListBySubjectCodes(any())).thenReturn(Collections.emptyList());
        lenient().when(subjectMapper.selectBySubjectCode(anyString())).thenReturn(null);

        ErpAccountingSubjectImportRespVO resp = subjectService.importSubjects(
                Collections.singletonList(row), "OVERWRITE");

        // S9 修复后：-1 不在 1-6 范围内 → failures
        verify(subjectMapper, never()).insert(any(ErpAccountingSubjectDO.class));
        assertThat(resp.getFailures()).anyMatch(f -> "9002".equals(f.getSubjectCode()));
    }

    @Test
    @DisplayName("importSubjects：subjectCategory 中文'资产' - 解析为 1")
    public void testImportSubjects_categoryChineseValid() {
        ErpAccountingSubjectImportExcelVO row = new ErpAccountingSubjectImportExcelVO();
        row.setSubjectCode("1001");
        row.setSubjectName("库存现金");
        row.setSubjectCategory("资产");
        lenient().when(subjectMapper.selectListBySubjectCodes(any())).thenReturn(Collections.emptyList());
        lenient().when(subjectMapper.selectBySubjectCode(anyString())).thenReturn(null);

        ArgumentCaptor<ErpAccountingSubjectDO> captor = ArgumentCaptor.forClass(ErpAccountingSubjectDO.class);
        subjectService.importSubjects(Collections.singletonList(row), "OVERWRITE");
        verify(subjectMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getSubjectCategory().intValue());
    }

}
