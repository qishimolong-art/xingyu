package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDeptDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierDeptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpMnemonicCodeUtils;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpSupplierServiceImpl} 的单元测试类
 */
public class ErpSupplierServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSupplierServiceImpl supplierService;

    @Mock
    private ErpSupplierMapper supplierMapper;
    @Mock
    private ErpSupplierDeptMapper supplierDeptMapper;
    @Mock
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpPayableAccountMapper payableAccountMapper;
    @Mock
    private ErpPurchaseDocumentDefaultService purchaseDocumentDefaultService;
    @Mock
    private DeptApi deptApi;

    // ========== createSupplier ==========

    @Test
    public void testCreateSupplier_withoutMaxCode_generatesGYS000001() {
        when(supplierMapper.selectMaxCode()).thenReturn(null);
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setName("芋道源码");

        supplierService.createSupplier(reqVO);

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        ErpSupplierDO inserted = captor.getValue();
        assertEquals("芋道源码", inserted.getName());
        assertEquals("GYS000001", inserted.getCode());
        // 兜底默认值
        assertEquals(Integer.valueOf(0), inserted.getSort());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), inserted.getStatus());
    }

    @Test
    public void testCreateSupplier_keepsCreationDepartmentSeparateFromBusinessDepartment() {
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setName("部门测试供应商");
        reqVO.setDeptId(99L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(88L);

            supplierService.createSupplier(reqVO);
        }

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        assertEquals(99L, captor.getValue().getDeptId());
        assertEquals(88L, captor.getValue().getCreateDeptId());
    }

    @Test
    public void testCreateSupplier_withMaxCode_generatesNextSeq() {
        when(supplierMapper.selectMaxCode()).thenReturn("GYS000023");
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setName("芋道源码");
        reqVO.setSort(5);
        reqVO.setStatus(CommonStatusEnum.DISABLE.getStatus());

        supplierService.createSupplier(reqVO);

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        ErpSupplierDO inserted = captor.getValue();
        assertEquals("GYS000024", inserted.getCode());
        // 已提供 sort/status 时不覆盖
        assertEquals(Integer.valueOf(5), inserted.getSort());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), inserted.getStatus());
    }

    @Test
    public void testCreateSupplier_withInvalidMaxCode_fallbackToOne() {
        // 编码后缀非数字，应当兜底为 GYS000001
        when(supplierMapper.selectMaxCode()).thenReturn("GYSABCDEF");
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setName("芋道源码");

        supplierService.createSupplier(reqVO);

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        assertEquals("GYS000001", captor.getValue().getCode());
    }

    @Test
    public void testCreateSupplier_withManualCode_keepTrimmedCode() {
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setName("手动编码供应商");
        reqVO.setCode(" GYS-MANUAL-001 ");

        supplierService.createSupplier(reqVO);

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        assertEquals("GYS-MANUAL-001", captor.getValue().getCode());
        verify(supplierMapper, never()).selectMaxCode();
    }

    @Test
    public void testCreateSupplier_withBlankManualCode_generatesCode() {
        when(supplierMapper.selectMaxCode()).thenReturn("GYS000001");
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setName("空白编码供应商");
        reqVO.setCode("   ");

        supplierService.createSupplier(reqVO);

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        assertEquals("GYS000002", captor.getValue().getCode());
    }

    @Test
    public void testCreateSupplier_duplicateManualCode_throwException() {
        when(supplierMapper.selectByCodeExcludeId(eq("GYS-MANUAL-001"), eq(null)))
                .thenReturn(new ErpSupplierDO().setId(10L).setCode("GYS-MANUAL-001"));
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setName("重复编码供应商");
        reqVO.setCode(" GYS-MANUAL-001 ");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.createSupplier(reqVO));

        assertEquals(SUPPLIER_CODE_DUPLICATE.getCode(), ex.getCode());
        verify(supplierMapper, never()).insert(any(ErpSupplierDO.class));
    }

    @Test
    public void testCreateSupplier_duplicateName_throwException() {
        when(supplierMapper.selectByNameExcludeId(eq("重复供应商"), eq(null)))
                .thenReturn(new ErpSupplierDO().setId(10L).setName("重复供应商"));
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setCode("GYS-MANUAL-002");
        reqVO.setName(" 重复供应商 ");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.createSupplier(reqVO));

        assertEquals(SUPPLIER_NAME_DUPLICATE.getCode(), ex.getCode());
        verify(supplierMapper, never()).insert(any(ErpSupplierDO.class));
    }

    // ========== updateSupplier ==========

    @Test
    public void testUpdateSupplier_success() {
        when(supplierMapper.selectById(eq(10L)))
                .thenReturn(new ErpSupplierDO().setId(10L).setCode("GYS000010").setCreateDeptId(88L));
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setId(10L);
        reqVO.setName("芋道源码-更新");
        reqVO.setCode("SHOULD-NOT-CHANGE");

        supplierService.updateSupplier(reqVO);

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).updateById(captor.capture());
        assertEquals(Long.valueOf(10L), captor.getValue().getId());
        assertEquals("芋道源码-更新", captor.getValue().getName());
        assertEquals("GYS000010", captor.getValue().getCode());
        assertEquals(88L, captor.getValue().getCreateDeptId());
    }

    @Test
    public void testUpdateSupplier_duplicateName_throwException() {
        when(supplierMapper.selectById(eq(10L)))
                .thenReturn(new ErpSupplierDO().setId(10L).setCode("GYS000010").setCreateDeptId(88L));
        when(supplierMapper.selectByNameExcludeId(eq("重复供应商"), eq(10L)))
                .thenReturn(new ErpSupplierDO().setId(11L).setName("重复供应商"));
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setId(10L);
        reqVO.setName(" 重复供应商 ");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.updateSupplier(reqVO));

        assertEquals(SUPPLIER_NAME_DUPLICATE.getCode(), ex.getCode());
        verify(supplierMapper, never()).updateById(any(ErpSupplierDO.class));
    }

    // ========== importSupplierList ==========

    @Test
    public void testImportSupplierList_withExistingCode_updatesNonBlankFieldsOnly() {
        ErpSupplierDO existing = new ErpSupplierDO()
                .setId(10L)
                .setCode("GYS000010")
                .setName("旧供应商")
                .setMobile("13800000000")
                .setBankName("旧开户行")
                .setDeptId(88L)
                .setAllowMultiDept(true);
        when(supplierMapper.selectListByCodes(eq(Collections.singletonList("GYS000010"))))
                .thenReturn(Collections.singletonList(existing));
        when(supplierMapper.selectById(eq(10L))).thenReturn(existing);

        ErpSupplierImportExcelVO row = new ErpSupplierImportExcelVO();
        row.setCode(" GYS000010 ");
        row.setName("新供应商");
        row.setMobile("   ");
        row.setBankName("新开户行");
        row.setTaxPercent(new BigDecimal("13"));
        row.setStatus(CommonStatusEnum.DISABLE.getStatus());

        ErpSupplierImportRespVO result = supplierService.importSupplierList(Collections.singletonList(row));

        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        assertEquals(Integer.valueOf(0), result.getCreateCount());
        assertEquals(Integer.valueOf(1), result.getUpdateCount());
        assertEquals(Integer.valueOf(0), result.getFailureCount());
        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).updateById(captor.capture());
        ErpSupplierDO updated = captor.getValue();
        assertEquals(10L, updated.getId());
        assertEquals("GYS000010", updated.getCode());
        assertEquals("新供应商", updated.getName());
        assertNull(updated.getMobile());
        assertEquals("新开户行", updated.getBankName());
        assertEquals(new BigDecimal("13"), updated.getTaxPercent());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), updated.getStatus());
        assertEquals(ErpMnemonicCodeUtils.buildPinyinCode("新供应商"), updated.getPinyinCode());
        assertEquals(ErpMnemonicCodeUtils.buildWubiCode("新供应商"), updated.getWubiCode());
        verify(supplierMapper, never()).insert(any(ErpSupplierDO.class));
    }

    @Test
    public void testImportSupplierList_sameNewCode_updatesSecondRow() {
        when(supplierMapper.selectListByCodes(eq(Collections.singletonList("GYSNEW001"))))
                .thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            ErpSupplierDO supplier = invocation.getArgument(0);
            supplier.setId(99L);
            return 1;
        }).when(supplierMapper).insert(any(ErpSupplierDO.class));
        when(supplierMapper.selectById(eq(99L))).thenReturn(new ErpSupplierDO()
                .setId(99L)
                .setCode("GYSNEW001")
                .setName("第二行更新"));

        ErpSupplierImportExcelVO first = new ErpSupplierImportExcelVO();
        first.setCode("GYSNEW001");
        first.setName("第一行新增");
        ErpSupplierImportExcelVO second = new ErpSupplierImportExcelVO();
        second.setCode("GYSNEW001");
        second.setName("第二行更新");
        second.setContact("李四");

        ErpSupplierImportRespVO result = supplierService.importSupplierList(Arrays.asList(first, second));

        assertEquals(Integer.valueOf(2), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getCreateCount());
        assertEquals(Integer.valueOf(1), result.getUpdateCount());
        ArgumentCaptor<ErpSupplierDO> updateCaptor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).updateById(updateCaptor.capture());
        assertEquals(99L, updateCaptor.getValue().getId());
        assertEquals("第二行更新", updateCaptor.getValue().getName());
        assertEquals("李四", updateCaptor.getValue().getContact());
    }

    @Test
    public void testImportSupplierList_withDuplicateExistingCode_recordsFailure() {
        ErpSupplierDO supplier1 = new ErpSupplierDO().setId(10L).setCode("GYS000010");
        ErpSupplierDO supplier2 = new ErpSupplierDO().setId(11L).setCode("GYS000010");
        when(supplierMapper.selectListByCodes(eq(Collections.singletonList("GYS000010"))))
                .thenReturn(Arrays.asList(supplier1, supplier2));

        ErpSupplierImportExcelVO row = new ErpSupplierImportExcelVO();
        row.setCode("GYS000010");
        row.setName("重复编码供应商");

        ErpSupplierImportRespVO result = supplierService.importSupplierList(Collections.singletonList(row));

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(0), result.getCreateCount());
        assertEquals(Integer.valueOf(0), result.getUpdateCount());
        assertEquals(Integer.valueOf(1), result.getFailureCount());
        assertEquals(1, result.getFailureDetails().size());
        assertTrue(result.getFailureDetails().get(0).getReason().contains("存在多条供应商资料"));
        verify(supplierMapper, never()).updateById(any(ErpSupplierDO.class));
        verify(supplierMapper, never()).insert(any(ErpSupplierDO.class));
    }

    @Test
    public void testImportSupplierList_duplicateNameForCreate_recordsFailure() {
        when(supplierMapper.selectListByCodes(eq(Collections.singletonList("GYS-IMPORT-001"))))
                .thenReturn(Collections.emptyList());
        when(supplierMapper.selectByNameExcludeId(eq("重复导入供应商"), eq(null)))
                .thenReturn(new ErpSupplierDO().setId(10L).setName("重复导入供应商"));

        ErpSupplierImportExcelVO row = new ErpSupplierImportExcelVO();
        row.setCode("GYS-IMPORT-001");
        row.setName(" 重复导入供应商 ");

        ErpSupplierImportRespVO result = supplierService.importSupplierList(Collections.singletonList(row));

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(0), result.getCreateCount());
        assertEquals(Integer.valueOf(0), result.getUpdateCount());
        assertEquals(Integer.valueOf(1), result.getFailureCount());
        assertEquals(1, result.getFailureDetails().size());
        assertTrue(result.getFailureDetails().get(0).getReason().contains("供应商名称(重复导入供应商)已存在"));
        verify(supplierMapper, never()).insert(any(ErpSupplierDO.class));
    }

    @Test
    public void testImportSupplierList_withDeptName_createsPrimaryDept() {
        mockEnabledDepartments(dept(11L, "采购部", 0L));
        doAnswer(invocation -> {
            ErpSupplierDO supplier = invocation.getArgument(0);
            supplier.setId(101L);
            return 1;
        }).when(supplierMapper).insert(any(ErpSupplierDO.class));

        ErpSupplierImportExcelVO row = new ErpSupplierImportExcelVO();
        row.setCode("GYS-DEPT-001");
        row.setName("部门供应商");
        row.setDeptName("采购部");

        ErpSupplierImportRespVO result = supplierService.importSupplierList(Collections.singletonList(row));

        assertEquals(Integer.valueOf(1), result.getCreateCount());
        assertEquals(Integer.valueOf(0), result.getFailureCount());
        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        assertEquals(11L, captor.getValue().getDeptId());
        assertEquals(Boolean.FALSE, captor.getValue().getAllowMultiDept());
        assertEquals(ErpMnemonicCodeUtils.buildPinyinCode("部门供应商"), captor.getValue().getPinyinCode());
        assertEquals(ErpMnemonicCodeUtils.buildWubiCode("部门供应商"), captor.getValue().getWubiCode());
        verify(supplierDeptMapper).deleteBySupplierId(eq(101L));
        verify(supplierDeptMapper, never()).insertBatch(any(Collection.class));
    }

    @Test
    public void testImportSupplierList_withDeptNames_createsMultiDeptByFullName() {
        mockEnabledDepartments(
                dept(1L, "总公司", 0L),
                dept(12L, "采购一部", 1L),
                dept(13L, "采购二部", 1L));
        doAnswer(invocation -> {
            ErpSupplierDO supplier = invocation.getArgument(0);
            supplier.setId(102L);
            return 1;
        }).when(supplierMapper).insert(any(ErpSupplierDO.class));

        ErpSupplierImportExcelVO row = new ErpSupplierImportExcelVO();
        row.setCode("GYS-DEPT-002");
        row.setName("多部门供应商");
        row.setDeptNames("采购一部、总公司 / 采购二部");

        ErpSupplierImportRespVO result = supplierService.importSupplierList(Collections.singletonList(row));

        assertEquals(Integer.valueOf(1), result.getCreateCount());
        assertEquals(Integer.valueOf(0), result.getFailureCount());
        ArgumentCaptor<ErpSupplierDO> supplierCaptor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).insert(supplierCaptor.capture());
        assertEquals(12L, supplierCaptor.getValue().getDeptId());
        assertEquals(Boolean.TRUE, supplierCaptor.getValue().getAllowMultiDept());
        ArgumentCaptor<Collection<ErpSupplierDeptDO>> deptCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(supplierDeptMapper).insertBatch(deptCaptor.capture());
        assertEquals(Arrays.asList(12L, 13L), deptCaptor.getValue().stream()
                .map(ErpSupplierDeptDO::getDeptId)
                .collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testImportSupplierList_updateBlankDept_keepsExistingDeptDistribution() {
        ErpSupplierDO existing = new ErpSupplierDO()
                .setId(10L)
                .setCode("GYS000010")
                .setName("旧供应商")
                .setDeptId(88L)
                .setAllowMultiDept(true);
        when(supplierMapper.selectListByCodes(eq(Collections.singletonList("GYS000010"))))
                .thenReturn(Collections.singletonList(existing));
        when(supplierMapper.selectById(eq(10L))).thenReturn(existing);

        ErpSupplierImportExcelVO row = new ErpSupplierImportExcelVO();
        row.setCode("GYS000010");
        row.setName("新供应商");

        ErpSupplierImportRespVO result = supplierService.importSupplierList(Collections.singletonList(row));

        assertEquals(Integer.valueOf(1), result.getUpdateCount());
        assertEquals(Integer.valueOf(0), result.getFailureCount());
        verify(supplierDeptMapper, never()).selectListBySupplierId(any(Long.class));
        verify(supplierDeptMapper, never()).deleteBySupplierId(any(Long.class));
        verify(supplierDeptMapper, never()).insertBatch(any(Collection.class));
    }

    @Test
    public void testImportSupplierList_updateDeptNames_replacesDeptDistribution() {
        mockEnabledDepartments(
                dept(1L, "总公司", 0L),
                dept(13L, "采购二部", 1L));
        ErpSupplierDO existing = new ErpSupplierDO()
                .setId(10L)
                .setCode("GYS000010")
                .setName("旧供应商")
                .setDeptId(88L)
                .setAllowMultiDept(false);
        when(supplierMapper.selectListByCodes(eq(Collections.singletonList("GYS000010"))))
                .thenReturn(Collections.singletonList(existing));
        when(supplierMapper.selectById(eq(10L))).thenReturn(existing);

        ErpSupplierImportExcelVO row = new ErpSupplierImportExcelVO();
        row.setCode("GYS000010");
        row.setName("新供应商");
        row.setDeptNames("总公司 / 采购二部");
        row.setAllowMultiDept(false);

        ErpSupplierImportRespVO result = supplierService.importSupplierList(Collections.singletonList(row));

        assertEquals(Integer.valueOf(1), result.getUpdateCount());
        assertEquals(Integer.valueOf(0), result.getFailureCount());
        ArgumentCaptor<ErpSupplierDO> supplierCaptor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).updateById(supplierCaptor.capture());
        assertEquals(Boolean.TRUE, supplierCaptor.getValue().getAllowMultiDept());
        ArgumentCaptor<Collection<ErpSupplierDeptDO>> deptCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(supplierDeptMapper).insertBatch(deptCaptor.capture());
        assertEquals(Arrays.asList(88L, 13L), deptCaptor.getValue().stream()
                .map(ErpSupplierDeptDO::getDeptId)
                .collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void testImportSupplierList_unknownDept_recordsFailure() {
        when(deptApi.getDeptListByStatus(eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.emptyList());

        ErpSupplierImportExcelVO row = new ErpSupplierImportExcelVO();
        row.setCode("GYS-DEPT-404");
        row.setName("未知部门供应商");
        row.setDeptName("不存在部门");

        ErpSupplierImportRespVO result = supplierService.importSupplierList(Collections.singletonList(row));

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailureCount());
        assertTrue(result.getFailureDetails().get(0).getReason().contains("不存在或已停用"));
        verify(supplierMapper, never()).insert(any(ErpSupplierDO.class));
        verify(supplierMapper, never()).updateById(any(ErpSupplierDO.class));
    }

    @Test
    public void testImportSupplierList_duplicateDeptName_recordsFailure() {
        mockEnabledDepartments(
                dept(1L, "总公司", 0L),
                dept(2L, "分公司", 0L),
                dept(12L, "采购部", 1L),
                dept(22L, "采购部", 2L));

        ErpSupplierImportExcelVO row = new ErpSupplierImportExcelVO();
        row.setCode("GYS-DEPT-DUP");
        row.setName("重名部门供应商");
        row.setDeptName("采购部");

        ErpSupplierImportRespVO result = supplierService.importSupplierList(Collections.singletonList(row));

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailureCount());
        assertTrue(result.getFailureDetails().get(0).getReason().contains("存在多个"));
        verify(supplierMapper, never()).insert(any(ErpSupplierDO.class));
        verify(supplierMapper, never()).updateById(any(ErpSupplierDO.class));
    }

    @Test
    public void testUpdateSupplier_notExists_throwException() {
        when(supplierMapper.selectById(eq(10L))).thenReturn(null);
        ErpSupplierSaveReqVO reqVO = new ErpSupplierSaveReqVO();
        reqVO.setId(10L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.updateSupplier(reqVO));
        assertEquals(SUPPLIER_NOT_EXISTS.getCode(), ex.getCode());
        verify(supplierMapper, never()).updateById(any(ErpSupplierDO.class));
    }

    // ========== deleteSupplier ==========

    @Test
    public void testDeleteSupplier_success() {
        when(supplierMapper.selectById(eq(10L)))
                .thenReturn(new ErpSupplierDO().setId(10L));

        supplierService.deleteSupplier(10L);

        verify(supplierMapper).deleteById(eq(10L));
    }

    @Test
    public void testDeleteSupplier_notExists_throwException() {
        when(supplierMapper.selectById(eq(10L))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.deleteSupplier(10L));
        assertEquals(SUPPLIER_NOT_EXISTS.getCode(), ex.getCode());
        verify(supplierMapper, never()).deleteById(any(Long.class));
    }

    // ========== validateSupplier ==========

    @Test
    public void testValidateSupplier_success() {
        ErpSupplierDO supplier = new ErpSupplierDO().setId(10L).setName("芋道源码")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(supplierMapper.selectById(eq(10L))).thenReturn(supplier);

        ErpSupplierDO result = supplierService.validateSupplier(10L);

        assertSame(supplier, result);
    }

    @Test
    public void testValidateSupplier_notExists_throwException() {
        when(supplierMapper.selectById(eq(10L))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.validateSupplier(10L));
        assertEquals(SUPPLIER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testValidateSupplier_disabled_throwException() {
        when(supplierMapper.selectById(eq(10L))).thenReturn(new ErpSupplierDO()
                .setId(10L).setName("芋道源码")
                .setStatus(CommonStatusEnum.DISABLE.getStatus()));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.validateSupplier(10L));
        assertEquals(SUPPLIER_NOT_ENABLE.getCode(), ex.getCode());
    }

    // ========== getSupplier / getSupplierList / getSupplierPage ==========

    @Test
    public void testGetSupplier() {
        ErpSupplierDO supplier = new ErpSupplierDO().setId(10L);
        when(supplierMapper.selectById(eq(10L))).thenReturn(supplier);

        assertSame(supplier, supplierService.getSupplier(10L));
    }

    @Test
    public void testGetSupplier_notExists_returnsNull() {
        when(supplierMapper.selectById(eq(10L))).thenReturn(null);

        assertNull(supplierService.getSupplier(10L));
    }

    @Test
    public void testGetSupplierList() {
        List<Long> ids = Arrays.asList(1L, 2L);
        List<ErpSupplierDO> suppliers = Arrays.asList(
                new ErpSupplierDO().setId(1L), new ErpSupplierDO().setId(2L));
        when(supplierMapper.selectByIds(eq(ids))).thenReturn(suppliers);

        assertSame(suppliers, supplierService.getSupplierList(ids));
    }

    @Test
    public void testGetSupplierList_emptyIds_skipMapper() {
        assertTrue(supplierService.getSupplierList(null).isEmpty());
        assertTrue(supplierService.getSupplierList(Collections.emptyList()).isEmpty());

        verify(supplierMapper, never()).selectByIds(any());
    }

    @Test
    public void testGetSupplierPage() {
        ErpSupplierPageReqVO reqVO = new ErpSupplierPageReqVO();
        PageResult<ErpSupplierDO> page = new PageResult<>(
                Collections.singletonList(new ErpSupplierDO().setId(1L)), 1L);
        when(supplierMapper.selectPage(eq(reqVO))).thenReturn(page);

        assertSame(page, supplierService.getSupplierPage(reqVO));
    }

    // ========== updateSupplierStatus ==========

    @Test
    public void testUpdateSupplierStatus_success() {
        when(supplierMapper.selectById(eq(10L)))
                .thenReturn(new ErpSupplierDO().setId(10L)
                        .setStatus(CommonStatusEnum.ENABLE.getStatus()));

        supplierService.updateSupplierStatus(10L, CommonStatusEnum.DISABLE.getStatus());

        ArgumentCaptor<ErpSupplierDO> captor = ArgumentCaptor.forClass(ErpSupplierDO.class);
        verify(supplierMapper).updateById(captor.capture());
        ErpSupplierDO updated = captor.getValue();
        assertEquals(Long.valueOf(10L), updated.getId());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), updated.getStatus());
        // 仅更新 id + status，其他字段保持 null
        assertNull(updated.getName());
        assertNull(updated.getCode());
    }

    @Test
    public void testUpdateSupplierStatus_notExists_throwException() {
        when(supplierMapper.selectById(eq(10L))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.updateSupplierStatus(10L,
                        CommonStatusEnum.DISABLE.getStatus()));
        assertEquals(SUPPLIER_NOT_EXISTS.getCode(), ex.getCode());
        verify(supplierMapper, never()).updateById(any(ErpSupplierDO.class));
    }

    // ========== getSupplierListByStatus ==========

    @Test
    public void testGetSupplierListByStatus() {
        Integer status = CommonStatusEnum.ENABLE.getStatus();
        List<ErpSupplierDO> suppliers = Arrays.asList(
                new ErpSupplierDO().setId(1L).setStatus(status),
                new ErpSupplierDO().setId(2L).setStatus(status));
        when(supplierMapper.selectListByStatus(eq(status))).thenReturn(suppliers);

        List<ErpSupplierDO> result = supplierService.getSupplierListByStatus(status);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertSame(suppliers, result);
    }

    private void mockEnabledDepartments(DeptRespDTO... depts) {
        List<DeptRespDTO> deptList = Arrays.asList(depts);
        Map<Long, DeptRespDTO> deptMap = new HashMap<>();
        deptList.forEach(dept -> deptMap.put(dept.getId(), dept));
        when(deptApi.getDeptListByStatus(eq(CommonStatusEnum.ENABLE.getStatus()))).thenReturn(deptList);
        when(deptApi.getDeptMap(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            Map<Long, DeptRespDTO> result = new HashMap<>();
            ids.stream()
                    .map(deptMap::get)
                    .filter(java.util.Objects::nonNull)
                    .forEach(dept -> result.put(dept.getId(), dept));
            return result;
        });
    }

    private DeptRespDTO dept(Long id, String name, Long parentId) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setParentId(parentId);
        dept.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return dept;
    }

}
