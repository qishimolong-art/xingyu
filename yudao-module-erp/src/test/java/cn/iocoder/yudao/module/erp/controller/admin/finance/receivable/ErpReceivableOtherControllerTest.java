package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
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

import javax.validation.ConstraintViolationException;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpReceivableOtherControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpReceivableOtherController controller;

    @Mock
    private ErpReceivableOtherService receivableOtherService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpCustomerDeptPermissionService customerDeptPermissionService;

    @Test
    void saveReqVO_rejectsZeroReceivableAmountButAllowsNegative() {
        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class,
                () -> ValidationUtils.validate(saveReqVO(BigDecimal.ZERO)));
        assertTrue(exception.getConstraintViolations().stream()
                .anyMatch(violation -> "应收金额不能为 0".equals(violation.getMessage())));

        assertDoesNotThrow(() -> ValidationUtils.validate(saveReqVO(new BigDecimal("-1.00"))));
    }

    @Test
    void createDraft_delegatesToDraftService() {
        ErpReceivableOtherDraftSaveReqVO reqVO =
                new ErpReceivableOtherDraftSaveReqVO().setRemark("未完成");
        when(receivableOtherService.createReceivableOtherDraft(reqVO)).thenReturn(9L);

        CommonResult<Long> result = controller.createDraft(reqVO);

        assertEquals(9L, result.getData());
        verify(receivableOtherService).createReceivableOtherDraft(reqVO);
    }

    @Test
    void page_allowsDraftWithoutCustomer() {
        ErpReceivableOtherDO row = new ErpReceivableOtherDO();
        row.setId(9L);
        row.setStatus(0);
        when(receivableOtherService.getReceivableOtherPage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.emptyMap());
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpReceivableOtherRespVO>> result =
                controller.page(new ErpReceivableOtherPageReqVO());

        assertNotNull(result.getData());
        assertEquals(0, result.getData().getList().get(0).getStatus());
    }

    @Test
    void getCustomerDeptSimpleList_delegatesToCustomerDeptPermissionService() {
        List<DeptSimpleRespVO> depts = Collections.singletonList(new DeptSimpleRespVO(6L, "财务部", 0L));
        when(customerDeptPermissionService.getAvailableDeptSimpleList(10L, "erp_receivable_other"))
                .thenReturn(depts);

        CommonResult<List<DeptSimpleRespVO>> result = controller.getCustomerDeptSimpleList(10L);

        assertEquals(depts, result.getData());
        verify(customerDeptPermissionService).getAvailableDeptSimpleList(10L, "erp_receivable_other");
    }

    @Test
    void page_fillsCustomerSalespersonAndAuditFields() {
        LocalDateTime auditTime = LocalDateTime.of(2026, 7, 20, 10, 30);
        ErpReceivableOtherDO row = new ErpReceivableOtherDO();
        row.setId(1L);
        row.setStatus(20);
        row.setCustomerId(10L);
        row.setHandlerId(5L);
        row.setDeptId(6L);
        row.setCreator("2");
        row.setUpdater("3");
        row.setUpdateTime(auditTime);
        when(receivableOtherService.getReceivableOtherPage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        ErpCustomerDO customer = ErpCustomerDO.builder()
                .id(10L).name("测试客户").saleUserId(4L).build();
        when(customerService.getCustomerMap(any()))
                .thenReturn(Collections.singletonMap(10L, customer));

        Map<Long, AdminUserRespDTO> userMap = new HashMap<>();
        userMap.put(2L, user("创建人"));
        userMap.put(3L, user("审核人"));
        userMap.put(4L, user("业务员"));
        userMap.put(5L, user("经手人"));
        when(adminUserApi.getUserMap(any())).thenReturn(userMap);

        DeptRespDTO dept = new DeptRespDTO();
        dept.setName("财务部");
        when(deptApi.getDeptMap(any())).thenReturn(Collections.singletonMap(6L, dept));

        CommonResult<PageResult<ErpReceivableOtherRespVO>> result =
                controller.page(new ErpReceivableOtherPageReqVO());

        assertNotNull(result.getData());
        ErpReceivableOtherRespVO vo = result.getData().getList().get(0);
        assertEquals("测试客户", vo.getCustomerName());
        assertEquals(4L, vo.getSaleUserId());
        assertEquals("业务员", vo.getSaleUserName());
        assertEquals("创建人", vo.getCreatorName());
        assertEquals("审核人", vo.getAuditorName());
        assertEquals(auditTime, vo.getAuditTime());
        assertEquals("经手人", vo.getHandlerName());
        assertEquals("财务部", vo.getDeptName());
    }

    @Test
    void exportColumns_alignsOtherReceivableListContract() {
        List<String> titles = Arrays.stream(ErpReceivableOtherExportRespVO.class.getDeclaredFields())
                .map(field -> field.getAnnotation(ExcelProperty.class))
                .filter(Objects::nonNull)
                .map(annotation -> annotation.value()[0])
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(
                "单号",
                "日期",
                "应收金额",
                "增加应收",
                "已结金额",
                "来源类型",
                "状态",
                "凭证号",
                "客户名称",
                "业务员",
                "创建人",
                "创建时间",
                "审核人",
                "审核时间",
                "经手人",
                "备注",
                "项目",
                "所属部门"
        ), titles);
    }

    @Test
    void importExcel_blankBizTimeDefaultsToToday() throws Exception {
        when(receivableOtherService.createReceivableOther(any())).thenReturn(1L);

        CommonResult<ErpFinanceImportRespVO> result = controller.importExcel(excelFile(
                new String[]{"客户ID", "应收金额", "调账原因备注"},
                new Object[]{10L, new BigDecimal("88.00"), "调账"}));

        ArgumentCaptor<ErpReceivableOtherSaveReqVO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherSaveReqVO.class);
        verify(receivableOtherService).createReceivableOther(captor.capture());
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
            return new MockMultipartFile("file", "receivable-other.xls", "application/vnd.ms-excel",
                    out.toByteArray());
        }
    }

    private static ErpReceivableOtherSaveReqVO saveReqVO(BigDecimal receivableAmount) {
        return new ErpReceivableOtherSaveReqVO()
                .setBizTime(LocalDate.of(2026, 9, 2))
                .setCustomerId(1L)
                .setDeptId(2L)
                .setReceivableAmount(receivableAmount);
    }

    private static AdminUserRespDTO user(String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setNickname(nickname);
        return user;
    }
}
