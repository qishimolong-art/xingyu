package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeRespVO;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports.ErpFinanceImportRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeItemDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherIncomeService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
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
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpReceivableOtherIncomeControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpReceivableOtherIncomeController controller;

    @Mock
    private ErpReceivableOtherIncomeService otherIncomeService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;

    @Test
    void importExcel_blankBizTimeDefaultsToNow() throws Exception {
        when(otherIncomeService.createOtherIncome(any())).thenReturn(1L);
        LocalDateTime before = LocalDateTime.now();

        CommonResult<ErpFinanceImportRespVO> result = controller.importExcel(excelFile(
                new String[]{"结算方式", "结算账户ID", "收入类型", "经手人ID", "收入项目", "金额"},
                new Object[]{"现金", 3L, "其他收入", 8L, "利息", new BigDecimal("12.00")}));

        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<ErpReceivableOtherIncomeSaveReqVO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherIncomeSaveReqVO.class);
        verify(otherIncomeService).createOtherIncome(captor.capture());
        assertEquals(1, result.getData().getSuccessCount());
        assertFalse(captor.getValue().getBizTime().isBefore(before));
        assertFalse(captor.getValue().getBizTime().isAfter(after));
        assertEquals(1, captor.getValue().getItems().size());
    }

    @Test
    void importExcel_mapsItemCustomerId() throws Exception {
        when(otherIncomeService.createOtherIncome(any())).thenReturn(1L);

        controller.importExcel(excelFile(
                new String[]{"结算方式", "结算账户ID", "收入类型", "经手人ID", "收入项目", "金额", "明细客户ID"},
                new Object[]{"现金", 3L, "其他", 8L, "代垫运费", new BigDecimal("12.00"), 88L}));

        ArgumentCaptor<ErpReceivableOtherIncomeSaveReqVO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherIncomeSaveReqVO.class);
        verify(otherIncomeService).createOtherIncome(captor.capture());
        assertEquals(88L, captor.getValue().getItems().get(0).getCustomerId());
    }

    @Test
    void itemPage_returnsPagedItemsAndAppliesMask() {
        ErpReceivableOtherIncomeItemDO item = new ErpReceivableOtherIncomeItemDO()
                .setId(11L).setIncomeId(7L).setItemName("利息").setAmount(new BigDecimal("12.00"));
        when(otherIncomeService.getOtherIncomeItemPage(any()))
                .thenReturn(new PageResult<>(Collections.singletonList(item), 1L));

        ErpReceivableOtherIncomeItemPageReqVO reqVO = new ErpReceivableOtherIncomeItemPageReqVO()
                .setIncomeId(7L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        CommonResult<PageResult<ErpReceivableOtherIncomeRespVO.Item>> result = controller.itemPage(reqVO);

        assertEquals(1L, result.getData().getTotal());
        assertEquals(11L, result.getData().getList().get(0).getId());
        assertEquals("利息", result.getData().getList().get(0).getItemName());
        verify(fieldPermissionMasker).clearHiddenItemFields(eq("erp_finance_receivable_other_income"), any());
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
            return new MockMultipartFile("file", "receivable-other-income.xls", "application/vnd.ms-excel",
                    out.toByteArray());
        }
    }
}
