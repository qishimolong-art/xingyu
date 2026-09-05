package cn.iocoder.yudao.module.erp.controller.admin.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpensePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableExpenseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPayableExpenseControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPayableExpenseController controller;

    @Mock
    private ErpPayableExpenseService payableExpenseService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @Test
    void createDraft_delegatesToDraftService() {
        ErpPayableExpenseDraftSaveReqVO reqVO =
                new ErpPayableExpenseDraftSaveReqVO().setRemark("未完成");
        when(payableExpenseService.createPayableExpenseDraft(reqVO)).thenReturn(9L);

        CommonResult<Long> result = controller.createDraft(reqVO);

        assertEquals(9L, result.getData());
        verify(payableExpenseService).createPayableExpenseDraft(reqVO);
    }

    @Test
    void page_allowsDraftWithoutAccountOrItems() {
        ErpPayableExpenseDO row = new ErpPayableExpenseDO()
                .setId(9L).setStatus(0)
                .setExpenseBizType("一般费用");
        when(payableExpenseService.getPayableExpensePage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));
        when(payableExpenseService.getPayableExpenseItemListByExpenseIds(any()))
                .thenReturn(Collections.emptyList());
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.emptyMap());
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpPayableExpenseRespVO>> result =
                controller.page(new ErpPayableExpensePageReqVO());

        assertNotNull(result.getData());
        assertEquals(0, result.getData().getList().get(0).getStatus());
        assertEquals("一般费用", result.getData().getList().get(0).getExpenseBizType());
    }

    @Test
    void itemPage_returnsPagedItemsAndAppliesMask() {
        ErpPayableExpenseItemDO item = new ErpPayableExpenseItemDO()
                .setId(11L).setExpenseId(7L).setItemName("停车费").setAmount(new BigDecimal("12.00"));
        when(payableExpenseService.getPayableExpenseItemPage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(item), 1L));

        ErpPayableExpenseItemPageReqVO reqVO = new ErpPayableExpenseItemPageReqVO()
                .setExpenseId(7L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        CommonResult<PageResult<ErpPayableExpenseRespVO.Item>> result = controller.itemPage(reqVO);

        assertEquals(1L, result.getData().getTotal());
        assertEquals(11L, result.getData().getList().get(0).getId());
        assertEquals("停车费", result.getData().getList().get(0).getItemName());
        verify(fieldPermissionMasker).clearHiddenItemFields(eq("erp_finance_payable_expense"), any());
    }

    @Test
    void importExcel_blankBizTimeDefaultsToToday() throws Exception {
        when(payableExpenseService.createPayableExpense(any())).thenReturn(1L);

        CommonResult<ErpFinanceImportRespVO> result = controller.importExcel(excelFile(
                new String[]{"结算方式", "结算账户ID", "费用类型", "申请人ID", "费用项目", "金额"},
                new Object[]{"现金", 3L, "日常费用", 8L, "停车费", new BigDecimal("12.00")}));

        ArgumentCaptor<ErpPayableExpenseSaveReqVO> captor =
                ArgumentCaptor.forClass(ErpPayableExpenseSaveReqVO.class);
        verify(payableExpenseService).createPayableExpense(captor.capture());
        assertEquals(1, result.getData().getSuccessCount());
        assertEquals(LocalDate.now(), captor.getValue().getBizTime());
        assertEquals(1, captor.getValue().getItems().size());
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
            return new MockMultipartFile("file", "payable-expense.xls", "application/vnd.ms-excel",
                    out.toByteArray());
        }
    }

}
