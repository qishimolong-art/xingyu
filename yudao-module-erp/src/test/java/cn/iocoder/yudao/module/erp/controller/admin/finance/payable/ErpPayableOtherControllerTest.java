package cn.iocoder.yudao.module.erp.controller.admin.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableOtherService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPayableOtherControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPayableOtherController controller;

    @Mock
    private ErpPayableOtherService payableOtherService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpSupplierDeptPermissionService supplierDeptPermissionService;

    @Test
    void createDraft_delegatesToDraftService() {
        ErpPayableOtherDraftSaveReqVO reqVO =
                new ErpPayableOtherDraftSaveReqVO().setRemark("未完成");
        when(payableOtherService.createPayableOtherDraft(reqVO)).thenReturn(9L);

        CommonResult<Long> result = controller.createDraft(reqVO);

        assertEquals(9L, result.getData());
        verify(payableOtherService).createPayableOtherDraft(reqVO);
    }

    @Test
    void page_allowsDraftWithoutSupplier() {
        ErpPayableOtherDO row = new ErpPayableOtherDO();
        row.setId(9L);
        row.setStatus(0);
        when(payableOtherService.getPayableOtherPage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));
        when(supplierService.getSupplierMap(any())).thenReturn(Collections.emptyMap());
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.emptyMap());
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpPayableOtherRespVO>> result =
                controller.page(new ErpPayableOtherPageReqVO());

        assertNotNull(result.getData());
        assertEquals(0, result.getData().getList().get(0).getStatus());
    }

    @Test
    void getSupplierDeptSimpleList_delegatesToSupplierDeptPermissionService() {
        List<DeptSimpleRespVO> depts = Collections.singletonList(new DeptSimpleRespVO(6L, "财务部", 0L));
        when(supplierDeptPermissionService.getAvailableDeptSimpleList(10L, "erp_payable_other"))
                .thenReturn(depts);

        CommonResult<List<DeptSimpleRespVO>> result = controller.getSupplierDeptSimpleList(10L);

        assertEquals(depts, result.getData());
        verify(supplierDeptPermissionService).getAvailableDeptSimpleList(10L, "erp_payable_other");
    }

    @Test
    void page_fillsSupplierAndAuditFields() {
        LocalDateTime auditTime = LocalDateTime.of(2026, 7, 20, 10, 30);
        ErpPayableOtherDO row = new ErpPayableOtherDO();
        row.setId(1L);
        row.setStatus(20);
        row.setSupplierId(10L);
        row.setHandlerId(5L);
        row.setDeptId(6L);
        row.setCreator("2");
        row.setUpdater("3");
        row.setUpdateTime(auditTime);
        when(payableOtherService.getPayableOtherPage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        ErpSupplierDO supplier = ErpSupplierDO.builder()
                .id(10L).name("测试供应商").build();
        when(supplierService.getSupplierMap(any()))
                .thenReturn(Collections.singletonMap(10L, supplier));

        Map<Long, AdminUserRespDTO> userMap = new HashMap<>();
        userMap.put(2L, user("创建人"));
        userMap.put(3L, user("审核人"));
        userMap.put(5L, user("经手人"));
        when(adminUserApi.getUserMap(any())).thenReturn(userMap);

        DeptRespDTO dept = new DeptRespDTO();
        dept.setName("财务部");
        when(deptApi.getDeptMap(any())).thenReturn(Collections.singletonMap(6L, dept));

        CommonResult<PageResult<ErpPayableOtherRespVO>> result =
                controller.page(new ErpPayableOtherPageReqVO());

        assertNotNull(result.getData());
        ErpPayableOtherRespVO vo = result.getData().getList().get(0);
        assertEquals("测试供应商", vo.getSupplierName());
        assertEquals("创建人", vo.getCreatorName());
        assertEquals("审核人", vo.getAuditorName());
        assertEquals(auditTime, vo.getAuditTime());
        assertEquals("经手人", vo.getHandlerName());
        assertEquals("财务部", vo.getDeptName());
    }

    @Test
    void importExcel_blankBizTimeDefaultsToToday() throws Exception {
        when(payableOtherService.createPayableOther(any())).thenReturn(1L);

        CommonResult<ErpFinanceImportRespVO> result = controller.importExcel(excelFile(
                new String[]{"供应商ID", "应付金额", "调账原因备注"},
                new Object[]{10L, new BigDecimal("88.00"), "调账"}));

        ArgumentCaptor<ErpPayableOtherSaveReqVO> captor =
                ArgumentCaptor.forClass(ErpPayableOtherSaveReqVO.class);
        verify(payableOtherService).createPayableOther(captor.capture());
        assertEquals(1, result.getData().getSuccessCount());
        assertEquals(LocalDate.now(), captor.getValue().getBizTime());
    }

    private MockMultipartFile excelFile(String[] headers, Object[] values) throws Exception {
        try (Workbook workbook = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("数据");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            Row dataRow = sheet.createRow(1);
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                if (value instanceof Number) {
                    dataRow.createCell(i).setCellValue(((Number) value).doubleValue());
                } else if (value != null) {
                    dataRow.createCell(i).setCellValue(value.toString());
                }
            }
            workbook.write(out);
            return new MockMultipartFile("file", "payable-other.xls", "application/vnd.ms-excel",
                    out.toByteArray());
        }
    }

    private static AdminUserRespDTO user(String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setNickname(nickname);
        return user;
    }
}
